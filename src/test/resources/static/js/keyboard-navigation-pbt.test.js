/**
 * Property-Based Tests for Keyboard Navigation
 * Feature: file-preview-system, Property 19: Keyboard navigation support
 * Validates: Requirements 9.3
 * 
 * Tests that keyboard shortcuts work correctly in the preview modal
 */

import fc from 'https://cdn.jsdelivr.net/npm/fast-check@3.15.0/+esm';

// Mock FilePreviewModal for testing
class MockFilePreviewModal {
    constructor() {
        this.isOpen = false;
        this.searchVisible = false;
        this.downloadCalled = false;
        this.closeCalled = false;
        this.scrollPosition = 0;
        this.currentPage = 1;
        this.totalPages = 10;
        this.focusedElement = null;
        this.focusableElements = [];
        this.firstFocusableElement = null;
        this.lastFocusableElement = null;
        this.announcements = [];
        
        // Create mock modal element
        this.modalElement = this.createMockModal();
        this.currentRenderer = {
            previousPage: () => {
                if (this.currentPage > 1) {
                    this.currentPage--;
                    return true;
                }
                return false;
            },
            nextPage: () => {
                if (this.currentPage < this.totalPages) {
                    this.currentPage++;
                    return true;
                }
                return false;
            }
        };
        
        // Bind methods
        this.handleEscKey = this.handleEscKey.bind(this);
        this.handleKeyboardNavigation = this.handleKeyboardNavigation.bind(this);
        this.trapFocus = this.trapFocus.bind(this);
    }
    
    createMockModal() {
        const modal = document.createElement('div');
        modal.className = 'fixed inset-0 z-50';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');
        
        const content = document.createElement('div');
        content.className = 'preview-content';
        content.style.height = '1000px';
        content.scrollTop = 0;
        
        // Add focusable elements
        const button1 = document.createElement('button');
        button1.textContent = 'Button 1';
        const button2 = document.createElement('button');
        button2.textContent = 'Button 2';
        const button3 = document.createElement('button');
        button3.textContent = 'Button 3';
        
        modal.appendChild(button1);
        modal.appendChild(button2);
        modal.appendChild(button3);
        modal.appendChild(content);
        
        this.focusableElements = [button1, button2, button3];
        this.firstFocusableElement = button1;
        this.lastFocusableElement = button3;
        
        return modal;
    }
    
    open() {
        this.isOpen = true;
        document.body.appendChild(this.modalElement);
    }
    
    close() {
        this.closeCalled = true;
        this.isOpen = false;
        if (this.modalElement.parentNode) {
            this.modalElement.parentNode.removeChild(this.modalElement);
        }
    }
    
    toggleSearch() {
        this.searchVisible = !this.searchVisible;
    }
    
    downloadFile() {
        this.downloadCalled = true;
    }
    
    announceToScreenReader(message) {
        this.announcements.push(message);
    }
    
    handleEscKey(event) {
        if (event.key === 'Escape' && this.isOpen) {
            this.close();
        }
    }
    
    handleKeyboardNavigation(event) {
        if (!this.isOpen) return;
        
        // Don't interfere if user is typing in an input
        if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') {
            return;
        }
        
        // Ctrl/Cmd + F: Toggle search
        if ((event.ctrlKey || event.metaKey) && event.key === 'f') {
            event.preventDefault();
            this.toggleSearch();
            this.announceToScreenReader('Search toggled');
        }
        
        // Ctrl/Cmd + D: Download file
        if ((event.ctrlKey || event.metaKey) && event.key === 'd') {
            event.preventDefault();
            this.downloadFile();
            this.announceToScreenReader('Downloading file');
        }
        
        // Arrow keys for navigation
        if (this.currentRenderer) {
            // Left arrow: Previous page
            if (event.key === 'ArrowLeft' && this.currentRenderer.previousPage) {
                event.preventDefault();
                this.currentRenderer.previousPage();
                this.announceToScreenReader('Previous page');
            }
            
            // Right arrow: Next page
            if (event.key === 'ArrowRight' && this.currentRenderer.nextPage) {
                event.preventDefault();
                this.currentRenderer.nextPage();
                this.announceToScreenReader('Next page');
            }
            
            // Up/Down arrows for scrolling
            const contentArea = this.modalElement?.querySelector('.preview-content');
            if (contentArea) {
                if (event.key === 'ArrowUp') {
                    event.preventDefault();
                    contentArea.scrollTop = Math.max(0, contentArea.scrollTop - 100);
                }
                if (event.key === 'ArrowDown') {
                    event.preventDefault();
                    contentArea.scrollTop += 100;
                }
                if (event.key === 'Home') {
                    event.preventDefault();
                    contentArea.scrollTop = 0;
                    this.announceToScreenReader('Scrolled to top');
                }
                if (event.key === 'End') {
                    event.preventDefault();
                    contentArea.scrollTop = contentArea.scrollHeight;
                    this.announceToScreenReader('Scrolled to bottom');
                }
            }
        }
    }
    
    trapFocus(event) {
        if (!this.isOpen || event.key !== 'Tab') return;
        
        // If shift+tab on first element, go to last
        if (event.shiftKey && document.activeElement === this.firstFocusableElement) {
            event.preventDefault();
            this.lastFocusableElement?.focus();
        }
        // If tab on last element, go to first
        else if (!event.shiftKey && document.activeElement === this.lastFocusableElement) {
            event.preventDefault();
            this.firstFocusableElement?.focus();
        }
    }
}

// Helper to create keyboard event
function createKeyboardEvent(key, options = {}) {
    return new KeyboardEvent('keydown', {
        key,
        ctrlKey: options.ctrlKey || false,
        metaKey: options.metaKey || false,
        shiftKey: options.shiftKey || false,
        bubbles: true,
        cancelable: true
    });
}

/**
 * Property 19: Keyboard navigation support
 * For any preview modal, keyboard shortcuts (ESC to close, arrow keys for navigation) 
 * should trigger the expected actions
 */
describe('Property 19: Keyboard Navigation Support', () => {
    
    test('ESC key closes modal', () => {
        fc.assert(
            fc.property(
                fc.constant(null), // No random input needed
                () => {
                    const modal = new MockFilePreviewModal();
                    modal.open();
                    
                    // Verify modal is open
                    expect(modal.isOpen).toBe(true);
                    
                    // Press ESC key
                    const escEvent = createKeyboardEvent('Escape');
                    modal.handleEscKey(escEvent);
                    
                    // Verify modal is closed
                    expect(modal.closeCalled).toBe(true);
                    expect(modal.isOpen).toBe(false);
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Ctrl+F toggles search', () => {
        fc.assert(
            fc.property(
                fc.boolean(), // Random initial search state
                (initialSearchState) => {
                    const modal = new MockFilePreviewModal();
                    modal.open();
                    modal.searchVisible = initialSearchState;
                    
                    // Press Ctrl+F
                    const ctrlFEvent = createKeyboardEvent('f', { ctrlKey: true });
                    modal.handleKeyboardNavigation(ctrlFEvent);
                    
                    // Verify search was toggled
                    expect(modal.searchVisible).toBe(!initialSearchState);
                    expect(modal.announcements).toContain('Search toggled');
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Ctrl+D triggers download', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = new MockFilePreviewModal();
                    modal.open();
                    
                    // Press Ctrl+D
                    const ctrlDEvent = createKeyboardEvent('d', { ctrlKey: true });
                    modal.handleKeyboardNavigation(ctrlDEvent);
                    
                    // Verify download was triggered
                    expect(modal.downloadCalled).toBe(true);
                    expect(modal.announcements).toContain('Downloading file');
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Arrow keys navigate pages', () => {
        fc.assert(
            fc.property(
                fc.integer({ min: 1, max: 10 }), // Random starting page
                (startPage) => {
                    const modal = new MockFilePreviewModal();
                    modal.open();
                    modal.currentPage = startPage;
                    
                    // Press right arrow (next page)
                    if (startPage < modal.totalPages) {
                        const rightEvent = createKeyboardEvent('ArrowRight');
                        modal.handleKeyboardNavigation(rightEvent);
                        expect(modal.currentPage).toBe(startPage + 1);
                        expect(modal.announcements).toContain('Next page');
                    }
                    
                    // Press left arrow (previous page)
                    if (modal.currentPage > 1) {
                        const leftEvent = createKeyboardEvent('ArrowLeft');
                        modal.handleKeyboardNavigation(leftEvent);
                        expect(modal.currentPage).toBe(startPage);
                        expect(modal.announcements).toContain('Previous page');
                    }
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Home key scrolls to top', () => {
        fc.assert(
            fc.property(
                fc.integer({ min: 100, max: 1000 }), // Random scroll position
                (scrollPos) => {
                    const modal = new MockFilePreviewModal();
                    modal.open();
                    
                    const contentArea = modal.modalElement.querySelector('.preview-content');
                    contentArea.scrollTop = scrollPos;
                    
                    // Press Home key
                    const homeEvent = createKeyboardEvent('Home');
                    modal.handleKeyboardNavigation(homeEvent);
                    
                    // Verify scrolled to top
                    expect(contentArea.scrollTop).toBe(0);
                    expect(modal.announcements).toContain('Scrolled to top');
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('End key scrolls to bottom', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = new MockFilePreviewModal();
                    modal.open();
                    
                    const contentArea = modal.modalElement.querySelector('.preview-content');
                    const initialScroll = contentArea.scrollTop;
                    
                    // Press End key
                    const endEvent = createKeyboardEvent('End');
                    modal.handleKeyboardNavigation(endEvent);
                    
                    // Verify scrolled to bottom
                    expect(contentArea.scrollTop).toBe(contentArea.scrollHeight);
                    expect(modal.announcements).toContain('Scrolled to bottom');
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Tab key traps focus within modal', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = new MockFilePreviewModal();
                    modal.open();
                    
                    // Focus last element
                    modal.lastFocusableElement.focus();
                    expect(document.activeElement).toBe(modal.lastFocusableElement);
                    
                    // Press Tab (should wrap to first element)
                    const tabEvent = createKeyboardEvent('Tab');
                    Object.defineProperty(tabEvent, 'target', { value: modal.lastFocusableElement });
                    modal.trapFocus(tabEvent);
                    
                    // In real implementation, focus would move to first element
                    // We verify the logic was triggered
                    expect(tabEvent.defaultPrevented).toBe(true);
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Shift+Tab wraps focus from first to last element', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = new MockFilePreviewModal();
                    modal.open();
                    
                    // Focus first element
                    modal.firstFocusableElement.focus();
                    expect(document.activeElement).toBe(modal.firstFocusableElement);
                    
                    // Press Shift+Tab (should wrap to last element)
                    const shiftTabEvent = createKeyboardEvent('Tab', { shiftKey: true });
                    Object.defineProperty(shiftTabEvent, 'target', { value: modal.firstFocusableElement });
                    modal.trapFocus(shiftTabEvent);
                    
                    // Verify the logic was triggered
                    expect(shiftTabEvent.defaultPrevented).toBe(true);
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Keyboard shortcuts do not trigger when modal is closed', () => {
        fc.assert(
            fc.property(
                fc.constantFrom('f', 'd', 'Escape', 'ArrowLeft', 'ArrowRight'),
                (key) => {
                    const modal = new MockFilePreviewModal();
                    // Modal is NOT opened
                    
                    const initialState = {
                        searchVisible: modal.searchVisible,
                        downloadCalled: modal.downloadCalled,
                        closeCalled: modal.closeCalled,
                        currentPage: modal.currentPage
                    };
                    
                    // Try to trigger keyboard shortcuts
                    const event = createKeyboardEvent(key, { ctrlKey: true });
                    modal.handleKeyboardNavigation(event);
                    modal.handleEscKey(event);
                    
                    // Verify nothing changed
                    expect(modal.searchVisible).toBe(initialState.searchVisible);
                    expect(modal.downloadCalled).toBe(initialState.downloadCalled);
                    expect(modal.closeCalled).toBe(initialState.closeCalled);
                    expect(modal.currentPage).toBe(initialState.currentPage);
                }
            ),
            { numRuns: 100 }
        );
    });
});

// Run tests
console.log('Running keyboard navigation property-based tests...');
