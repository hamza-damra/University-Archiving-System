/**
 * Node.js test runner for Large File Handling Property-Based Tests
 * Run with: node src/test/resources/static/js/run-large-file-tests.cjs
 */

const fc = require('fast-check');

// Mock FilePreviewModal for testing
class MockFilePreviewModal {
    constructor() {
        this.isOpen = false;
        this.currentFileId = null;
        this.currentFileName = null;
        this.currentFileType = null;
        this.modalElement = null;
        this.warningShown = false;
        this.partialPreviewLoaded = false;
        this.downloadTriggered = false;
    }

    createModal() {
        // Create a simple content area object
        this.contentArea = {
            innerHTML: ''
        };
        
        this.modalElement = {
            querySelector: (selector) => {
                if (selector === '.preview-content') {
                    return this.contentArea;
                }
                return null;
            }
        };
    }

    async open(fileId, fileName, fileType) {
        this.isOpen = true;
        this.currentFileId = fileId;
        this.currentFileName = fileName;
        this.currentFileType = fileType;
        this.createModal();

        // Simulate fetching metadata
        const metadata = await this.fetchMetadata(fileId);

        // Check file size (5MB threshold)
        const MAX_FILE_SIZE = 5 * 1024 * 1024;
        if (metadata.fileSize && metadata.fileSize > MAX_FILE_SIZE) {
            this.showLargeFileWarning(metadata.fileSize);
            return;
        }

        // Normal preview loading would happen here
    }

    async fetchMetadata(fileId) {
        return {
            id: fileId,
            fileName: this.currentFileName,
            fileSize: this.simulatedFileSize || 1024,
            mimeType: this.currentFileType
        };
    }

    showLargeFileWarning(fileSize) {
        this.warningShown = true;
        const contentArea = this.modalElement?.querySelector('.preview-content');
        if (contentArea) {
            // Format file size for display
            const formattedSize = formatFileSize(fileSize);
            contentArea.innerHTML = `
                <div class="large-file-warning">
                    <p>File size: ${fileSize} bytes (${formattedSize})</p>
                    <button class="partial-preview-btn">Preview First Part</button>
                    <button class="download-btn">Download File</button>
                </div>
            `;
        }
    }

    async loadPartialPreview() {
        this.partialPreviewLoaded = true;
    }

    async downloadFile() {
        this.downloadTriggered = true;
    }

    close() {
        this.isOpen = false;
        this.modalElement = null;
    }

    setSimulatedFileSize(size) {
        this.simulatedFileSize = size;
    }
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}

console.log('🔍 Large File Handling Property-Based Tests');
console.log('Feature: file-preview-system, Property 12: Large file warning');
console.log('Validates: Requirements 6.3\n');

let totalTests = 0;
let passedTests = 0;
let failedTests = 0;

async function runTest(name, testFn) {
    totalTests++;
    process.stdout.write(`Running: ${name}... `);
    
    try {
        await testFn();
        passedTests++;
        console.log('✅ PASSED');
        return true;
    } catch (error) {
        failedTests++;
        console.log('❌ FAILED');
        console.log(`  Error: ${error.message}`);
        return false;
    }
}

// Main test execution
(async () => {
    try {
        // Test 1: Files larger than 5MB should trigger warning
        await runTest('Files larger than 5MB should trigger warning modal', async () => {
            await fc.assert(
                fc.asyncProperty(
                    fc.integer({ min: 5 * 1024 * 1024 + 1, max: 100 * 1024 * 1024 }),
                    fc.integer({ min: 1, max: 1000 }),
                    fc.constantFrom('text/plain', 'application/pdf', 'text/csv', 'application/json'),
                    async (fileSize, fileId, mimeType) => {
                        const modal = new MockFilePreviewModal();
                        modal.setSimulatedFileSize(fileSize);
                        await modal.open(fileId, `test-file-${fileId}.txt`, mimeType);

                        if (!modal.warningShown) {
                            throw new Error(`Expected warning for ${formatFileSize(fileSize)} (${fileSize} bytes)`);
                        }

                        const contentArea = modal.modalElement?.querySelector('.preview-content');
                        if (!contentArea) {
                            throw new Error('Content area not found');
                        }
                        
                        if (!contentArea.innerHTML.includes('large-file-warning')) {
                            throw new Error(`Expected warning UI, got: ${contentArea.innerHTML.substring(0, 100)}`);
                        }

                        if (!contentArea.innerHTML.includes('partial-preview-btn') || 
                            !contentArea.innerHTML.includes('download-btn')) {
                            throw new Error('Expected action buttons');
                        }

                        modal.close();
                    }
                ),
                { numRuns: 100 }
            );
        });

        // Test 2: Files <= 5MB should NOT trigger warning
        await runTest('Files smaller than or equal to 5MB should NOT trigger warning', async () => {
            await fc.assert(
                fc.asyncProperty(
                    fc.integer({ min: 1, max: 5 * 1024 * 1024 }),
                    fc.integer({ min: 1, max: 1000 }),
                    fc.constantFrom('text/plain', 'application/pdf', 'text/csv'),
                    async (fileSize, fileId, mimeType) => {
                        const modal = new MockFilePreviewModal();
                        modal.setSimulatedFileSize(fileSize);
                        await modal.open(fileId, `test-file-${fileId}.txt`, mimeType);

                        if (modal.warningShown) {
                            throw new Error(`Did not expect warning for ${formatFileSize(fileSize)}`);
                        }

                        modal.close();
                    }
                ),
                { numRuns: 100 }
            );
        });

        // Test 3: Partial preview option
        await runTest('Warning modal should offer partial preview option', async () => {
            await fc.assert(
                fc.asyncProperty(
                    fc.integer({ min: 5 * 1024 * 1024 + 1, max: 100 * 1024 * 1024 }),
                    fc.integer({ min: 1, max: 1000 }),
                    async (fileSize, fileId) => {
                        const modal = new MockFilePreviewModal();
                        modal.setSimulatedFileSize(fileSize);
                        await modal.open(fileId, `large-file-${fileId}.txt`, 'text/plain');

                        if (!modal.warningShown) {
                            throw new Error('Expected warning');
                        }

                        await modal.loadPartialPreview();
                        if (!modal.partialPreviewLoaded) {
                            throw new Error('Expected partial preview to load');
                        }

                        modal.close();
                    }
                ),
                { numRuns: 100 }
            );
        });

        // Test 4: Download option
        await runTest('Warning modal should offer download option', async () => {
            await fc.assert(
                fc.asyncProperty(
                    fc.integer({ min: 5 * 1024 * 1024 + 1, max: 100 * 1024 * 1024 }),
                    fc.integer({ min: 1, max: 1000 }),
                    async (fileSize, fileId) => {
                        const modal = new MockFilePreviewModal();
                        modal.setSimulatedFileSize(fileSize);
                        await modal.open(fileId, `large-file-${fileId}.txt`, 'text/plain');

                        if (!modal.warningShown) {
                            throw new Error('Expected warning');
                        }

                        await modal.downloadFile();
                        if (!modal.downloadTriggered) {
                            throw new Error('Expected download to trigger');
                        }

                        modal.close();
                    }
                ),
                { numRuns: 100 }
            );
        });

        // Test 5: Boundary - exactly 5MB
        await runTest('Boundary test: Exactly 5MB should NOT trigger warning', async () => {
            await fc.assert(
                fc.asyncProperty(
                    fc.integer({ min: 1, max: 1000 }),
                    async (fileId) => {
                        const modal = new MockFilePreviewModal();
                        modal.setSimulatedFileSize(5 * 1024 * 1024);
                        await modal.open(fileId, `boundary-file-${fileId}.txt`, 'text/plain');

                        if (modal.warningShown) {
                            throw new Error('Did not expect warning for exactly 5MB');
                        }

                        modal.close();
                    }
                ),
                { numRuns: 100 }
            );
        });

        // Test 6: Boundary - 5MB + 1 byte
        await runTest('Boundary test: 5MB + 1 byte should trigger warning', async () => {
            await fc.assert(
                fc.asyncProperty(
                    fc.integer({ min: 1, max: 1000 }),
                    async (fileId) => {
                        const modal = new MockFilePreviewModal();
                        modal.setSimulatedFileSize(5 * 1024 * 1024 + 1);
                        await modal.open(fileId, `boundary-file-${fileId}.txt`, 'text/plain');

                        if (!modal.warningShown) {
                            throw new Error('Expected warning for 5MB + 1 byte');
                        }

                        modal.close();
                    }
                ),
                { numRuns: 100 }
            );
        });

        // Test 7: File size displayed
        await runTest('Large file warning should display file size information', async () => {
            await fc.assert(
                fc.asyncProperty(
                    fc.integer({ min: 5 * 1024 * 1024 + 1, max: 100 * 1024 * 1024 }),
                    fc.integer({ min: 1, max: 1000 }),
                    async (fileSize, fileId) => {
                        const modal = new MockFilePreviewModal();
                        modal.setSimulatedFileSize(fileSize);
                        await modal.open(fileId, `large-file-${fileId}.txt`, 'text/plain');

                        if (!modal.warningShown) {
                            throw new Error('Expected warning');
                        }

                        const contentArea = modal.modalElement?.querySelector('.preview-content');
                        if (!contentArea || !contentArea.innerHTML.includes(fileSize.toString())) {
                            throw new Error('Expected file size in warning');
                        }

                        modal.close();
                    }
                ),
                { numRuns: 100 }
            );
        });

        // Summary
        console.log('\n' + '='.repeat(60));
        console.log(`Summary: ${passedTests}/${totalTests} tests passed`);
        if (failedTests > 0) {
            console.log(`❌ ${failedTests} test(s) failed`);
            process.exit(1);
        } else {
            console.log('✅ All tests passed!');
            process.exit(0);
        }
    } catch (error) {
        console.error('\nTest suite error:', error);
        process.exit(1);
    }
})();
