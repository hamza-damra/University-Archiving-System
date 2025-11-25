/**
 * Property-Based Tests for Responsive Layout
 * Feature: file-preview-system, Property 18: Responsive layout adaptation
 * Validates: Requirements 9.2
 * 
 * Tests that the preview modal adapts its layout appropriately for different viewport sizes
 */

import fc from 'https://cdn.jsdelivr.net/npm/fast-check@3.15.0/+esm';

// Mock FilePreviewModal for testing
class MockFilePreviewModal {
    constructor() {
        this.modalElement = null;
        this.isOpen = false;
    }

    createModal(fileName, fileType) {
        const modal = document.createElement('div');
        modal.className = 'file-preview-modal fade-in';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');

        modal.innerHTML = `
            <div class="preview-backdrop" aria-hidden="true"></div>
            <div class="preview-modal-content">
                <div class="preview-modal-header">
                    <div class="flex-1 min-w-0">
                        <h2 class="preview-modal-title">${fileName}</h2>
                        <div class="preview-metadata">
                            <span class="px-2 py-1 bg-gray-200 dark:bg-gray-700 rounded">2.5 MB</span>
                            <span class="px-2 py-1 bg-gray-200 dark:bg-gray-700 rounded">${fileType}</span>
                            <span class="px-2 py-1 bg-gray-200 dark:bg-gray-700 rounded">Uploaded: Nov 26, 2025</span>
                        </div>
                    </div>
                    <div class="preview-actions" role="toolbar">
                        <button class="preview-action-btn" title="Search">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                            </svg>
                        </button>
                        <button class="preview-action-btn" title="Download">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                            </svg>
                        </button>
                        <button class="preview-action-btn" title="Close">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path d="M6 18L18 6M6 6l12 12"></path>
                            </svg>
                        </button>
                    </div>
                </div>
                <div class="preview-search-bar hidden">
                    <div class="flex items-center gap-2">
                        <div class="flex-1 relative">
                            <input type="text" placeholder="Search..." class="preview-search-input">
                        </div>
                    </div>
                </div>
                <div class="preview-content">
                    <p>Sample content</p>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        this.modalElement = modal;
        this.isOpen = true;
        return modal;
    }

    close() {
        if (this.modalElement && this.modalElement.parentNode) {
            this.modalElement.parentNode.removeChild(this.modalElement);
        }
        this.modalElement = null;
        this.isOpen = false;
    }
}

// Helper function to set viewport size
function setViewportSize(width, height) {
    // Create or update viewport meta tag
    let viewportMeta = document.querySelector('meta[name="viewport"]');
    if (!viewportMeta) {
        viewportMeta = document.createElement('meta');
        viewportMeta.name = 'viewport';
        document.head.appendChild(viewportMeta);
    }
    viewportMeta.content = `width=${width}, initial-scale=1.0`;

    // Simulate window resize
    Object.defineProperty(window, 'innerWidth', {
        writable: true,
        configurable: true,
        value: width
    });
    Object.defineProperty(window, 'innerHeight', {
        writable: true,
        configurable: true,
        value: height
    });

    // Trigger resize event
    window.dispatchEvent(new Event('resize'));
}

// Helper function to get computed styles
function getComputedStyleValue(element, property) {
    return window.getComputedStyle(element).getPropertyValue(property);
}

// Helper function to check if element has minimum touch-friendly size
function isTouchFriendly(element) {
    const rect = element.getBoundingClientRect();
    const minSize = window.innerWidth <= 480 ? 48 : 44; // 48px on mobile, 44px on tablet
    return rect.width >= minSize && rect.height >= minSize;
}

// Generators
const viewportWidthArb = fc.integer({ min: 320, max: 1920 });
const viewportHeightArb = fc.integer({ min: 480, max: 1080 });
const fileNameArb = fc.string({ minLength: 5, maxLength: 50 }).map(s => s + '.txt');
const fileTypeArb = fc.constantFrom('text/plain', 'application/pdf', 'text/javascript');

// Test Suite
const tests = [];

/**
 * Property 18: Responsive layout adaptation
 * For any viewport size below tablet breakpoint, the preview modal should adapt its layout for smaller screens
 */
tests.push({
    name: 'Property 18.1: Modal adapts to tablet breakpoint (≤768px)',
    property: fc.property(
        fc.integer({ min: 481, max: 768 }),
        fc.integer({ min: 600, max: 1024 }),
        fileNameArb,
        fileTypeArb,
        (width, height, fileName, fileType) => {
            // Set viewport to tablet size
            setViewportSize(width, height);

            // Create modal
            const modal = new MockFilePreviewModal();
            const modalElement = modal.createModal(fileName, fileType);

            try {
                // Check that modal exists
                if (!modalElement) {
                    return false;
                }

                const modalContent = modalElement.querySelector('.preview-modal-content');
                const header = modalElement.querySelector('.preview-modal-header');
                const buttons = modalElement.querySelectorAll('.preview-action-btn');

                // Verify modal content exists
                if (!modalContent || !header || buttons.length === 0) {
                    return false;
                }

                // Check that buttons are touch-friendly (minimum 44px)
                const allButtonsTouchFriendly = Array.from(buttons).every(btn => {
                    const rect = btn.getBoundingClientRect();
                    return rect.width >= 44 && rect.height >= 44;
                });

                if (!allButtonsTouchFriendly) {
                    console.log('Tablet: Buttons not touch-friendly at', width, 'x', height);
                    return false;
                }

                // Check that modal has appropriate max-width
                const modalWidth = modalContent.getBoundingClientRect().width;
                const viewportWidth = window.innerWidth;
                
                // Modal should not exceed viewport width minus padding
                if (modalWidth > viewportWidth) {
                    console.log('Tablet: Modal too wide at', width, 'x', height);
                    return false;
                }

                return true;
            } finally {
                modal.close();
            }
        }
    ),
    options: { numRuns: 100 }
});

tests.push({
    name: 'Property 18.2: Modal goes full-screen on mobile (≤480px)',
    property: fc.property(
        fc.integer({ min: 320, max: 480 }),
        fc.integer({ min: 480, max: 896 }),
        fileNameArb,
        fileTypeArb,
        (width, height, fileName, fileType) => {
            // Set viewport to mobile size
            setViewportSize(width, height);

            // Create modal
            const modal = new MockFilePreviewModal();
            const modalElement = modal.createModal(fileName, fileType);

            try {
                // Check that modal exists
                if (!modalElement) {
                    return false;
                }

                const modalContent = modalElement.querySelector('.preview-modal-content');
                const buttons = modalElement.querySelectorAll('.preview-action-btn');

                // Verify modal content exists
                if (!modalContent || buttons.length === 0) {
                    return false;
                }

                // Check that buttons are touch-friendly (minimum 48px on mobile)
                const allButtonsTouchFriendly = Array.from(buttons).every(btn => {
                    const rect = btn.getBoundingClientRect();
                    return rect.width >= 48 && rect.height >= 48;
                });

                if (!allButtonsTouchFriendly) {
                    console.log('Mobile: Buttons not touch-friendly at', width, 'x', height);
                    return false;
                }

                // On mobile, modal should be full-screen (or very close to it)
                const modalWidth = modalContent.getBoundingClientRect().width;
                const modalHeight = modalContent.getBoundingClientRect().height;
                const viewportWidth = window.innerWidth;
                const viewportHeight = window.innerHeight;

                // Allow small margin for rounding
                const widthMatch = Math.abs(modalWidth - viewportWidth) <= 2;
                const heightMatch = Math.abs(modalHeight - viewportHeight) <= 2;

                if (!widthMatch || !heightMatch) {
                    console.log('Mobile: Modal not full-screen at', width, 'x', height);
                    console.log('Modal:', modalWidth, 'x', modalHeight, 'Viewport:', viewportWidth, 'x', viewportHeight);
                    return false;
                }

                return true;
            } finally {
                modal.close();
            }
        }
    ),
    options: { numRuns: 100 }
});

tests.push({
    name: 'Property 18.3: Search input prevents zoom on mobile (font-size ≥ 16px)',
    property: fc.property(
        fc.integer({ min: 320, max: 480 }),
        fc.integer({ min: 480, max: 896 }),
        fileNameArb,
        fileTypeArb,
        (width, height, fileName, fileType) => {
            // Set viewport to mobile size
            setViewportSize(width, height);

            // Create modal
            const modal = new MockFilePreviewModal();
            const modalElement = modal.createModal(fileName, fileType);

            try {
                const searchInput = modalElement.querySelector('.preview-search-input');

                // Verify search input exists
                if (!searchInput) {
                    return false;
                }

                // Get computed font size
                const fontSize = getComputedStyleValue(searchInput, 'font-size');
                const fontSizeValue = parseFloat(fontSize);

                // On mobile, font size should be at least 16px to prevent zoom
                if (fontSizeValue < 16) {
                    console.log('Mobile: Search input font too small:', fontSizeValue, 'at', width, 'x', height);
                    return false;
                }

                return true;
            } finally {
                modal.close();
            }
        }
    ),
    options: { numRuns: 100 }
});

tests.push({
    name: 'Property 18.4: Modal maintains readability across all viewport sizes',
    property: fc.property(
        viewportWidthArb,
        viewportHeightArb,
        fileNameArb,
        fileTypeArb,
        (width, height, fileName, fileType) => {
            // Set viewport size
            setViewportSize(width, height);

            // Create modal
            const modal = new MockFilePreviewModal();
            const modalElement = modal.createModal(fileName, fileType);

            try {
                const title = modalElement.querySelector('.preview-modal-title');
                const metadata = modalElement.querySelector('.preview-metadata');
                const content = modalElement.querySelector('.preview-content');

                // Verify elements exist
                if (!title || !metadata || !content) {
                    return false;
                }

                // Check that title font size is readable (at least 13px)
                const titleFontSize = parseFloat(getComputedStyleValue(title, 'font-size'));
                if (titleFontSize < 13) {
                    console.log('Title font too small:', titleFontSize, 'at', width, 'x', height);
                    return false;
                }

                // Check that metadata font size is readable (at least 10px)
                const metadataFontSize = parseFloat(getComputedStyleValue(metadata, 'font-size'));
                if (metadataFontSize < 10) {
                    console.log('Metadata font too small:', metadataFontSize, 'at', width, 'x', height);
                    return false;
                }

                // Check that content has appropriate padding
                const contentPadding = parseFloat(getComputedStyleValue(content, 'padding'));
                if (contentPadding < 8) { // At least 8px padding
                    console.log('Content padding too small:', contentPadding, 'at', width, 'x', height);
                    return false;
                }

                return true;
            } finally {
                modal.close();
            }
        }
    ),
    options: { numRuns: 100 }
});

tests.push({
    name: 'Property 18.5: Modal content is scrollable at all viewport sizes',
    property: fc.property(
        viewportWidthArb,
        viewportHeightArb,
        fileNameArb,
        fileTypeArb,
        (width, height, fileName, fileType) => {
            // Set viewport size
            setViewportSize(width, height);

            // Create modal
            const modal = new MockFilePreviewModal();
            const modalElement = modal.createModal(fileName, fileType);

            try {
                const content = modalElement.querySelector('.preview-content');

                // Verify content exists
                if (!content) {
                    return false;
                }

                // Check that content has overflow property set
                const overflow = getComputedStyleValue(content, 'overflow');
                const overflowY = getComputedStyleValue(content, 'overflow-y');

                // Content should be scrollable
                const isScrollable = overflow === 'auto' || overflow === 'scroll' || 
                                   overflowY === 'auto' || overflowY === 'scroll';

                if (!isScrollable) {
                    console.log('Content not scrollable at', width, 'x', height);
                    return false;
                }

                return true;
            } finally {
                modal.close();
            }
        }
    ),
    options: { numRuns: 100 }
});

tests.push({
    name: 'Property 18.6: Action buttons maintain touch-friendly size across breakpoints',
    property: fc.property(
        viewportWidthArb,
        viewportHeightArb,
        fileNameArb,
        fileTypeArb,
        (width, height, fileName, fileType) => {
            // Set viewport size
            setViewportSize(width, height);

            // Create modal
            const modal = new MockFilePreviewModal();
            const modalElement = modal.createModal(fileName, fileType);

            try {
                const buttons = modalElement.querySelectorAll('.preview-action-btn');

                // Verify buttons exist
                if (buttons.length === 0) {
                    return false;
                }

                // Determine minimum size based on viewport
                let minSize = 44; // Default for desktop and tablet
                if (width <= 480) {
                    minSize = 48; // Mobile requires larger touch targets
                }

                // Check all buttons meet minimum size
                const allButtonsValid = Array.from(buttons).every(btn => {
                    const rect = btn.getBoundingClientRect();
                    return rect.width >= minSize && rect.height >= minSize;
                });

                if (!allButtonsValid) {
                    console.log('Buttons not touch-friendly at', width, 'x', height, 'min:', minSize);
                    return false;
                }

                return true;
            } finally {
                modal.close();
            }
        }
    ),
    options: { numRuns: 100 }
});

// Run all tests
async function runTests() {
    console.log('Running Responsive Layout Property-Based Tests...\n');
    
    let passedCount = 0;
    let failedCount = 0;
    const results = [];

    for (const test of tests) {
        console.log(`Running: ${test.name}`);
        try {
            await fc.assert(test.property, test.options);
            console.log(`✓ PASSED: ${test.name}\n`);
            passedCount++;
            results.push({ name: test.name, status: 'PASSED' });
        } catch (error) {
            console.error(`✗ FAILED: ${test.name}`);
            console.error(`Error: ${error.message}\n`);
            failedCount++;
            results.push({ name: test.name, status: 'FAILED', error: error.message });
        }
    }

    console.log('\n' + '='.repeat(60));
    console.log('Test Summary:');
    console.log(`Total: ${tests.length}`);
    console.log(`Passed: ${passedCount}`);
    console.log(`Failed: ${failedCount}`);
    console.log('='.repeat(60));

    return { passedCount, failedCount, results };
}

// Export for use in test runner
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { runTests, tests };
}

// Auto-run if loaded directly in browser
if (typeof window !== 'undefined' && !window.testRunner) {
    runTests().then(results => {
        console.log('\nAll tests completed!');
        if (results.failedCount === 0) {
            console.log('🎉 All property tests passed!');
        } else {
            console.log(`⚠️  ${results.failedCount} test(s) failed.`);
        }
    });
}
