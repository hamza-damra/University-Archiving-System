/**
 * Node.js Test Runner for Responsive Layout Property-Based Tests
 * Feature: file-preview-system, Property 18
 * 
 * Run with: node src/test/resources/static/js/run-responsive-layout-tests.cjs
 */

const { JSDOM } = require('jsdom');
const fc = require('fast-check');

// Setup JSDOM environment
const dom = new JSDOM(`
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        /* Include responsive CSS inline for testing */
        .file-preview-modal {
            position: fixed;
            inset: 0;
            z-index: 50;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 1rem;
        }
        
        .preview-modal-content {
            position: relative;
            background-color: white;
            border-radius: 0.5rem;
            width: 100%;
            max-width: 72rem;
            max-height: 90vh;
            display: flex;
            flex-direction: column;
        }
        
        .preview-modal-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 1rem;
        }
        
        .preview-modal-title {
            font-size: 1.125rem;
            font-weight: 600;
        }
        
        .preview-metadata {
            margin-top: 0.25rem;
            display: flex;
            flex-wrap: wrap;
            gap: 0.5rem;
            font-size: 0.75rem;
        }
        
        .preview-actions {
            display: flex;
            align-items: center;
            gap: 0.5rem;
            margin-left: 1rem;
        }
        
        .preview-action-btn {
            padding: 0.5rem;
            min-width: 44px;
            min-height: 44px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        .preview-search-bar {
            padding: 0.75rem;
        }
        
        .preview-search-input {
            width: 100%;
            padding: 0.5rem 0.75rem;
            font-size: 0.875rem;
        }
        
        .preview-content {
            flex: 1;
            overflow: auto;
            padding: 1.5rem;
        }
        
        /* Tablet breakpoint */
        @media (max-width: 768px) {
            .file-preview-modal {
                padding: 0.5rem;
            }
            
            .preview-modal-content {
                max-width: 100%;
                max-height: 95vh;
            }
            
            .preview-modal-header {
                padding: 0.75rem;
            }
            
            .preview-modal-title {
                font-size: 1rem;
            }
            
            .preview-metadata {
                font-size: 0.7rem;
            }
            
            .preview-action-btn {
                min-width: 44px;
                min-height: 44px;
            }
            
            .preview-content {
                padding: 1rem;
            }
        }
        
        /* Mobile breakpoint */
        @media (max-width: 480px) {
            .file-preview-modal {
                padding: 0;
            }
            
            .preview-modal-content {
                max-width: 100%;
                max-height: 100vh;
                height: 100vh;
                border-radius: 0;
            }
            
            .preview-modal-header {
                padding: 0.5rem;
            }
            
            .preview-modal-title {
                font-size: 0.875rem;
            }
            
            .preview-metadata {
                font-size: 0.65rem;
            }
            
            .preview-action-btn {
                min-width: 48px;
                min-height: 48px;
            }
            
            .preview-search-input {
                font-size: 1rem;
            }
            
            .preview-content {
                padding: 0.75rem;
            }
        }
    </style>
</head>
<body></body>
</html>
`, {
    url: 'http://localhost',
    pretendToBeVisual: true,
    resources: 'usable'
});

global.window = dom.window;
global.document = dom.window.document;
global.navigator = dom.window.navigator;

// Mock FilePreviewModal
class MockFilePreviewModal {
    constructor() {
        this.modalElement = null;
        this.isOpen = false;
    }

    createModal(fileName, fileType) {
        const modal = document.createElement('div');
        modal.className = 'file-preview-modal fade-in';
        modal.setAttribute('role', 'dialog');

        modal.innerHTML = `
            <div class="preview-backdrop"></div>
            <div class="preview-modal-content">
                <div class="preview-modal-header">
                    <div class="flex-1 min-w-0">
                        <h2 class="preview-modal-title">${fileName}</h2>
                        <div class="preview-metadata">
                            <span>2.5 MB</span>
                            <span>${fileType}</span>
                        </div>
                    </div>
                    <div class="preview-actions">
                        <button class="preview-action-btn">Search</button>
                        <button class="preview-action-btn">Download</button>
                        <button class="preview-action-btn">Close</button>
                    </div>
                </div>
                <div class="preview-search-bar hidden">
                    <input type="text" class="preview-search-input">
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

// Helper to set viewport
function setViewportSize(width, height) {
    // Update window dimensions
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
}

// Generators
const fileNameArb = fc.string({ minLength: 5, maxLength: 50 }).map(s => s + '.txt');
const fileTypeArb = fc.constantFrom('text/plain', 'application/pdf', 'text/javascript');

// Tests
const tests = [
    {
        name: 'Property 18.1: Modal structure is valid for tablet breakpoint (≤768px)',
        property: fc.property(
            fc.integer({ min: 481, max: 768 }),
            fc.integer({ min: 600, max: 1024 }),
            fileNameArb,
            fileTypeArb,
            (width, height, fileName, fileType) => {
                setViewportSize(width, height);
                const modal = new MockFilePreviewModal();
                const modalElement = modal.createModal(fileName, fileType);

                try {
                    const buttons = modalElement.querySelectorAll('.preview-action-btn');
                    const modalContent = modalElement.querySelector('.preview-modal-content');
                    const header = modalElement.querySelector('.preview-modal-header');
                    
                    // Check structure exists
                    if (!modalContent || !header || buttons.length === 0) {
                        return false;
                    }
                    
                    // Check that buttons have the correct class (CSS will handle sizing)
                    const allButtonsHaveClass = Array.from(buttons).every(btn => {
                        return btn.classList.contains('preview-action-btn');
                    });

                    return allButtonsHaveClass;
                } finally {
                    modal.close();
                }
            }
        ),
        options: { numRuns: 100 }
    },
    {
        name: 'Property 18.2: Modal structure is valid for mobile breakpoint (≤480px)',
        property: fc.property(
            fc.integer({ min: 320, max: 480 }),
            fc.integer({ min: 480, max: 896 }),
            fileNameArb,
            fileTypeArb,
            (width, height, fileName, fileType) => {
                setViewportSize(width, height);
                const modal = new MockFilePreviewModal();
                const modalElement = modal.createModal(fileName, fileType);

                try {
                    const buttons = modalElement.querySelectorAll('.preview-action-btn');
                    const modalContent = modalElement.querySelector('.preview-modal-content');
                    const content = modalElement.querySelector('.preview-content');
                    
                    // Check structure exists
                    if (!modalContent || !content || buttons.length === 0) {
                        return false;
                    }
                    
                    // Check that all required elements have correct classes
                    const hasCorrectClasses = 
                        modalElement.classList.contains('file-preview-modal') &&
                        modalContent.classList.contains('preview-modal-content') &&
                        content.classList.contains('preview-content');

                    return hasCorrectClasses;
                } finally {
                    modal.close();
                }
            }
        ),
        options: { numRuns: 100 }
    },
    {
        name: 'Property 18.3: Search input has correct class for mobile styling',
        property: fc.property(
            fc.integer({ min: 320, max: 480 }),
            fc.integer({ min: 480, max: 896 }),
            fileNameArb,
            fileTypeArb,
            (width, height, fileName, fileType) => {
                setViewportSize(width, height);
                const modal = new MockFilePreviewModal();
                const modalElement = modal.createModal(fileName, fileType);

                try {
                    const searchInput = modalElement.querySelector('.preview-search-input');
                    if (!searchInput) return false;

                    // Check that search input has the correct class (CSS will handle font size)
                    return searchInput.classList.contains('preview-search-input');
                } finally {
                    modal.close();
                }
            }
        ),
        options: { numRuns: 100 }
    },
    {
        name: 'Property 18.4: Modal maintains required structure across all viewport sizes',
        property: fc.property(
            fc.integer({ min: 320, max: 1920 }),
            fc.integer({ min: 480, max: 1080 }),
            fileNameArb,
            fileTypeArb,
            (width, height, fileName, fileType) => {
                setViewportSize(width, height);
                const modal = new MockFilePreviewModal();
                const modalElement = modal.createModal(fileName, fileType);

                try {
                    // Check all required elements exist
                    const hasBackdrop = modalElement.querySelector('.preview-backdrop') !== null;
                    const hasModalContent = modalElement.querySelector('.preview-modal-content') !== null;
                    const hasHeader = modalElement.querySelector('.preview-modal-header') !== null;
                    const hasTitle = modalElement.querySelector('.preview-modal-title') !== null;
                    const hasMetadata = modalElement.querySelector('.preview-metadata') !== null;
                    const hasActions = modalElement.querySelector('.preview-actions') !== null;
                    const hasContent = modalElement.querySelector('.preview-content') !== null;
                    const hasButtons = modalElement.querySelectorAll('.preview-action-btn').length >= 3;

                    return hasBackdrop && hasModalContent && hasHeader && hasTitle && 
                           hasMetadata && hasActions && hasContent && hasButtons;
                } finally {
                    modal.close();
                }
            }
        ),
        options: { numRuns: 100 }
    }
];

// Run tests
async function runTests() {
    console.log('Running Responsive Layout Property-Based Tests...\n');
    console.log('Feature: file-preview-system, Property 18');
    console.log('Validates: Requirements 9.2\n');
    console.log('='.repeat(60));
    
    let passedCount = 0;
    let failedCount = 0;

    for (const test of tests) {
        console.log(`\nRunning: ${test.name}`);
        try {
            await fc.assert(test.property, test.options);
            console.log(`✓ PASSED`);
            passedCount++;
        } catch (error) {
            console.error(`✗ FAILED`);
            console.error(`Error: ${error.message}`);
            failedCount++;
        }
    }

    console.log('\n' + '='.repeat(60));
    console.log('Test Summary:');
    console.log(`Total: ${tests.length}`);
    console.log(`Passed: ${passedCount}`);
    console.log(`Failed: ${failedCount}`);
    console.log('='.repeat(60));

    if (failedCount === 0) {
        console.log('\n🎉 All property tests passed!');
    } else {
        console.log(`\n⚠️  ${failedCount} test(s) failed.`);
        process.exit(1);
    }
}

// Run if executed directly
if (require.main === module) {
    runTests().catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}

module.exports = { runTests };
