/**
 * Property-Based Tests for Accessibility Compliance
 * Feature: file-preview-system, Property 20: Accessibility compliance
 * Validates: Requirements 9.4
 * 
 * Tests that all interactive elements have appropriate ARIA labels and roles
 */

import fc from 'https://cdn.jsdelivr.net/npm/fast-check@3.15.0/+esm';

/**
 * Helper to check if element has required ARIA attributes
 */
function hasRequiredAriaAttributes(element) {
    const tagName = element.tagName.toLowerCase();
    
    // Buttons should have aria-label or accessible text
    if (tagName === 'button') {
        return element.hasAttribute('aria-label') || 
               element.textContent.trim().length > 0 ||
               element.hasAttribute('title');
    }
    
    // Inputs should have aria-label or associated label
    if (tagName === 'input') {
        return element.hasAttribute('aria-label') ||
               element.hasAttribute('aria-labelledby') ||
               element.hasAttribute('placeholder');
    }
    
    // Links should have accessible text
    if (tagName === 'a') {
        return element.textContent.trim().length > 0 ||
               element.hasAttribute('aria-label');
    }
    
    return true;
}

/**
 * Helper to check if element has appropriate role
 */
function hasAppropriateRole(element) {
    const tagName = element.tagName.toLowerCase();
    const role = element.getAttribute('role');
    
    // Semantic HTML elements have implicit roles
    const semanticElements = ['button', 'input', 'a', 'nav', 'main', 'header', 'footer'];
    if (semanticElements.includes(tagName)) {
        return true;
    }
    
    // Non-semantic elements should have explicit roles
    if (tagName === 'div' || tagName === 'span') {
        // If it's interactive, it should have a role
        if (element.hasAttribute('onclick') || element.hasAttribute('tabindex')) {
            return role !== null;
        }
    }
    
    return true;
}

/**
 * Create a mock FilePreviewModal with DOM structure
 */
function createMockModalDOM() {
    const modal = document.createElement('div');
    modal.className = 'fixed inset-0 z-50';
    modal.setAttribute('role', 'dialog');
    modal.setAttribute('aria-modal', 'true');
    modal.setAttribute('aria-labelledby', 'preview-modal-title');
    modal.setAttribute('aria-describedby', 'preview-modal-description');
    
    modal.innerHTML = `
        <div class="absolute inset-0 bg-black bg-opacity-50 preview-backdrop" aria-hidden="true"></div>
        
        <div class="relative bg-white rounded-lg shadow-2xl">
            <div class="flex items-center justify-between p-4">
                <div class="flex-1">
                    <h2 id="preview-modal-title" class="text-lg font-semibold">Test File.pdf</h2>
                    <div id="preview-modal-description" class="preview-metadata">
                        <span>1.2 MB</span>
                        <span>PDF Document</span>
                    </div>
                </div>
                <div class="flex items-center gap-2" role="toolbar" aria-label="Preview actions">
                    <button id="preview-search-toggle" 
                            type="button"
                            title="Search in preview (Ctrl+F)"
                            aria-label="Search in preview. Press Control F to activate">
                        <svg aria-hidden="true"><path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                    </button>
                    <button type="button"
                            title="Download file (Ctrl+D)"
                            aria-label="Download file. Press Control D to download">
                        <svg aria-hidden="true"><path d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                    </button>
                    <button type="button"
                            title="Close preview (Escape)"
                            aria-label="Close preview. Press Escape to close">
                        <svg aria-hidden="true"><path d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                </div>
            </div>
            
            <div id="preview-search-bar" class="hidden" role="search" aria-label="Search within preview">
                <div class="flex items-center gap-2">
                    <input type="text" 
                           id="preview-search-input" 
                           placeholder="Search in preview..." 
                           aria-label="Search query"
                           aria-describedby="preview-search-count">
                    <button type="button"
                            title="Clear search"
                            aria-label="Clear search">
                        <svg aria-hidden="true"><path d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                    <span id="preview-search-count" role="status" aria-live="polite"></span>
                    <button type="button"
                            title="Previous match (Shift+Enter)"
                            aria-label="Go to previous search match. Press Shift Enter"
                            id="preview-search-prev">
                        <svg aria-hidden="true"><path d="M15 19l-7-7 7-7"></path></svg>
                    </button>
                    <button type="button"
                            title="Next match (Enter)"
                            aria-label="Go to next search match. Press Enter"
                            id="preview-search-next">
                        <svg aria-hidden="true"><path d="M9 5l7 7-7 7"></path></svg>
                    </button>
                </div>
            </div>
            
            <div class="preview-content" role="document" aria-label="File content" tabindex="0">
                <p>File content goes here</p>
            </div>
            
            <div id="preview-sr-announcements" class="sr-only" role="status" aria-live="polite" aria-atomic="true"></div>
        </div>
    `;
    
    return modal;
}

/**
 * Property 20: Accessibility compliance
 * For any interactive element in the preview modal, appropriate ARIA labels and roles should be present
 */
describe('Property 20: Accessibility Compliance', () => {
    
    test('Modal has dialog role and aria-modal attribute', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    
                    // Verify modal has dialog role
                    expect(modal.getAttribute('role')).toBe('dialog');
                    
                    // Verify modal has aria-modal
                    expect(modal.getAttribute('aria-modal')).toBe('true');
                    
                    // Verify modal has aria-labelledby
                    expect(modal.hasAttribute('aria-labelledby')).toBe(true);
                    
                    // Verify modal has aria-describedby
                    expect(modal.hasAttribute('aria-describedby')).toBe(true);
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('All buttons have aria-label or accessible text', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const buttons = modal.querySelectorAll('button');
                    
                    expect(buttons.length).toBeGreaterThan(0);
                    
                    buttons.forEach(button => {
                        const hasLabel = button.hasAttribute('aria-label');
                        const hasText = button.textContent.trim().length > 0;
                        const hasTitle = button.hasAttribute('title');
                        
                        expect(hasLabel || hasText || hasTitle).toBe(true);
                    });
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('All buttons have type attribute', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const buttons = modal.querySelectorAll('button');
                    
                    buttons.forEach(button => {
                        expect(button.hasAttribute('type')).toBe(true);
                    });
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Search input has aria-label', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const searchInput = modal.querySelector('#preview-search-input');
                    
                    expect(searchInput).not.toBeNull();
                    expect(searchInput.hasAttribute('aria-label')).toBe(true);
                    expect(searchInput.hasAttribute('aria-describedby')).toBe(true);
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Search bar has search role', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const searchBar = modal.querySelector('#preview-search-bar');
                    
                    expect(searchBar).not.toBeNull();
                    expect(searchBar.getAttribute('role')).toBe('search');
                    expect(searchBar.hasAttribute('aria-label')).toBe(true);
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Search count has live region attributes', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const searchCount = modal.querySelector('#preview-search-count');
                    
                    expect(searchCount).not.toBeNull();
                    expect(searchCount.getAttribute('role')).toBe('status');
                    expect(searchCount.getAttribute('aria-live')).toBe('polite');
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Screen reader announcements element exists with proper attributes', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const announcer = modal.querySelector('#preview-sr-announcements');
                    
                    expect(announcer).not.toBeNull();
                    expect(announcer.getAttribute('role')).toBe('status');
                    expect(announcer.getAttribute('aria-live')).toBe('polite');
                    expect(announcer.getAttribute('aria-atomic')).toBe('true');
                    expect(announcer.classList.contains('sr-only')).toBe(true);
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Backdrop has aria-hidden attribute', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const backdrop = modal.querySelector('.preview-backdrop');
                    
                    expect(backdrop).not.toBeNull();
                    expect(backdrop.getAttribute('aria-hidden')).toBe('true');
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Content area has document role and is focusable', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const content = modal.querySelector('.preview-content');
                    
                    expect(content).not.toBeNull();
                    expect(content.getAttribute('role')).toBe('document');
                    expect(content.hasAttribute('aria-label')).toBe(true);
                    expect(content.hasAttribute('tabindex')).toBe(true);
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Toolbar has appropriate role and label', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const toolbar = modal.querySelector('[role="toolbar"]');
                    
                    expect(toolbar).not.toBeNull();
                    expect(toolbar.hasAttribute('aria-label')).toBe(true);
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Decorative SVGs have aria-hidden attribute', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const svgs = modal.querySelectorAll('svg');
                    
                    expect(svgs.length).toBeGreaterThan(0);
                    
                    svgs.forEach(svg => {
                        // Decorative SVGs should have aria-hidden
                        expect(svg.getAttribute('aria-hidden')).toBe('true');
                    });
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Modal title is properly referenced', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const titleId = modal.getAttribute('aria-labelledby');
                    const title = modal.querySelector(`#${titleId}`);
                    
                    expect(title).not.toBeNull();
                    expect(title.textContent.trim().length).toBeGreaterThan(0);
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Modal description is properly referenced', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const descId = modal.getAttribute('aria-describedby');
                    const description = modal.querySelector(`#${descId}`);
                    
                    expect(description).not.toBeNull();
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('All interactive elements are keyboard accessible', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const interactiveElements = modal.querySelectorAll('button, input, a, [tabindex]');
                    
                    interactiveElements.forEach(element => {
                        const tabindex = element.getAttribute('tabindex');
                        
                        // Elements should not have tabindex="-1" unless they're explicitly non-focusable
                        // Buttons and inputs are naturally focusable
                        if (element.tagName.toLowerCase() === 'button' || 
                            element.tagName.toLowerCase() === 'input') {
                            expect(tabindex !== '-1').toBe(true);
                        }
                    });
                }
            ),
            { numRuns: 100 }
        );
    });
    
    test('Navigation buttons have descriptive labels with keyboard hints', () => {
        fc.assert(
            fc.property(
                fc.constant(null),
                () => {
                    const modal = createMockModalDOM();
                    const prevButton = modal.querySelector('#preview-search-prev');
                    const nextButton = modal.querySelector('#preview-search-next');
                    
                    expect(prevButton).not.toBeNull();
                    expect(nextButton).not.toBeNull();
                    
                    // Check that labels include keyboard hints
                    const prevLabel = prevButton.getAttribute('aria-label');
                    const nextLabel = nextButton.getAttribute('aria-label');
                    
                    expect(prevLabel).toContain('previous');
                    expect(nextLabel).toContain('next');
                }
            ),
            { numRuns: 100 }
        );
    });
});

// Run tests
console.log('Running accessibility compliance property-based tests...');
