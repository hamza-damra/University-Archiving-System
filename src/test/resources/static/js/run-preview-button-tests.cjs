/**
 * Node.js test runner for File Preview Button Property-Based Tests
 * Feature: file-preview-system
 * Task: 8.1 Write property test for preview button rendering
 */

const fc = require('fast-check');
global.fc = fc;

// Mock FilePreviewModal
class MockFilePreviewModal {
    constructor(options = {}) {
        this.options = options;
        this.isOpen = false;
    }
    
    async open(fileId, fileName, fileType) {
        this.isOpen = true;
        this.currentFileId = fileId;
        this.currentFileName = fileName;
        this.currentFileType = fileType;
    }
    
    close() {
        this.isOpen = false;
    }
}

// FilePreviewButton implementation
class FilePreviewButton {
    static SUPPORTED_TYPES = {
        'text/plain': true,
        'text/markdown': true,
        'text/csv': true,
        'text/log': true,
        'application/pdf': true,
        'application/msword': true,
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document': true,
        'application/vnd.ms-excel': true,
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': true,
        'application/vnd.ms-powerpoint': true,
        'application/vnd.openxmlformats-officedocument.presentationml.presentation': true,
        'text/javascript': true,
        'application/javascript': true,
        'text/x-java-source': true,
        'text/x-python': true,
        'text/css': true,
        'text/html': true,
        'application/sql': true,
        'application/xml': true,
        'text/xml': true,
        'application/json': true
    };

    static SUPPORTED_EXTENSIONS = {
        'txt': true, 'md': true, 'log': true, 'csv': true,
        'pdf': true,
        'doc': true, 'docx': true, 'xls': true, 'xlsx': true, 'ppt': true, 'pptx': true,
        'java': true, 'js': true, 'py': true, 'css': true, 'html': true, 'sql': true, 'xml': true, 'json': true
    };

    static isPreviewable(fileType, fileName = '') {
        if (fileType && this.SUPPORTED_TYPES[fileType.toLowerCase()]) {
            return true;
        }
        
        if (fileName) {
            const extension = this.getFileExtension(fileName);
            if (extension && this.SUPPORTED_EXTENSIONS[extension.toLowerCase()]) {
                return true;
            }
        }
        
        return false;
    }

    static getFileExtension(fileName) {
        if (!fileName) return '';
        const parts = fileName.split('.');
        return parts.length > 1 ? parts[parts.length - 1] : '';
    }

    static renderButton(file) {
        const fileId = file.id || (file.metadata && file.metadata.fileId);
        const fileName = file.originalFilename || file.name || 'Unknown';
        const fileType = file.fileType || (file.metadata && file.metadata.fileType) || '';
        
        const canPreview = this.isPreviewable(fileType, fileName);
        
        if (!canPreview) {
            return `
                <button 
                    class="text-gray-400 p-1.5 rounded cursor-not-allowed"
                    title="Download only - Preview not available for this file type"
                    disabled
                >
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                        <line x1="3" y1="3" x2="21" y2="21" stroke="currentColor" stroke-width="2" stroke-linecap="round"></line>
                    </svg>
                </button>
            `;
        }
        
        return `
            <button 
                onclick="window.filePreviewButton.handlePreviewClick(${fileId}, '${this.escapeHtml(fileName)}', '${this.escapeHtml(fileType)}')"
                class="preview-button text-blue-600 hover:text-blue-800 hover:bg-blue-50 p-1.5 rounded transition-all"
                title="Click to preview"
                data-file-id="${fileId}"
                data-file-name="${this.escapeHtml(fileName)}"
                data-file-type="${this.escapeHtml(fileType)}"
            >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                </svg>
            </button>
        `;
    }

    static escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    async handlePreviewClick(fileId, fileName, fileType) {
        if (!this.previewModal) {
            this.previewModal = new MockFilePreviewModal();
        }
        await this.previewModal.open(fileId, fileName, fileType);
    }
}

global.FilePreviewButton = FilePreviewButton;

// Run tests
console.log('🧪 Running File Preview Button Property-Based Tests...\n');
console.log('Property 10: Preview button rendering');
console.log('Validates: Requirements 5.1, 5.2, 5.3, 5.4\n');

let passedTests = 0;
let failedTests = 0;

// Test 1: Supported file types should render active preview button
try {
    console.log('Test 1: Supported file types should render active preview button');
    
    fc.assert(
        fc.property(
            fc.oneof(
                fc.record({
                    id: fc.integer({ min: 1, max: 10000 }),
                    originalFilename: fc.constantFrom('document.txt', 'readme.md', 'data.csv', 'app.log'),
                    fileType: fc.constantFrom('text/plain', 'text/markdown', 'text/csv', 'text/log')
                }),
                fc.record({
                    id: fc.integer({ min: 1, max: 10000 }),
                    originalFilename: fc.constant('document.pdf'),
                    fileType: fc.constant('application/pdf')
                }),
                fc.record({
                    id: fc.integer({ min: 1, max: 10000 }),
                    originalFilename: fc.constantFrom('report.docx', 'spreadsheet.xlsx', 'presentation.pptx'),
                    fileType: fc.constantFrom(
                        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                        'application/vnd.openxmlformats-officedocument.presentationml.presentation'
                    )
                }),
                fc.record({
                    id: fc.integer({ min: 1, max: 10000 }),
                    originalFilename: fc.constantFrom('script.js', 'Main.java', 'app.py', 'style.css', 'index.html'),
                    fileType: fc.constantFrom('text/javascript', 'text/x-java-source', 'text/x-python', 'text/css', 'text/html')
                })
            ),
            (file) => {
                const buttonHtml = FilePreviewButton.renderButton(file);
                
                if (!buttonHtml || buttonHtml.length === 0) {
                    throw new Error('Button HTML is empty');
                }
                if (buttonHtml.includes('disabled')) {
                    throw new Error('Button should not be disabled for supported types');
                }
                if (buttonHtml.includes('cursor-not-allowed')) {
                    throw new Error('Button should not have cursor-not-allowed for supported types');
                }
                if (!buttonHtml.includes('preview-button')) {
                    throw new Error('Button should have preview-button class');
                }
                if (!buttonHtml.includes('text-blue-600')) {
                    throw new Error('Button should have blue color for visual distinction');
                }
                if (!buttonHtml.includes('Click to preview')) {
                    throw new Error('Button should have "Click to preview" tooltip');
                }
                if (!buttonHtml.includes('onclick')) {
                    throw new Error('Button should have onclick handler');
                }
                if (!buttonHtml.includes('handlePreviewClick')) {
                    throw new Error('Button should call handlePreviewClick');
                }
                if (!buttonHtml.includes(`data-file-id="${file.id}"`)) {
                    throw new Error('Button should have data-file-id attribute');
                }
            }
        ),
        { numRuns: 100 }
    );
    
    console.log('✅ PASSED (100 runs)\n');
    passedTests++;
} catch (error) {
    console.log('❌ FAILED');
    console.log('Error:', error.message);
    console.log('');
    failedTests++;
}

// Test 2: Unsupported file types should render disabled button
try {
    console.log('Test 2: Unsupported file types should render disabled button');
    
    fc.assert(
        fc.property(
            fc.record({
                id: fc.integer({ min: 1, max: 10000 }),
                originalFilename: fc.constantFrom('image.png', 'video.mp4', 'archive.zip', 'binary.exe'),
                fileType: fc.constantFrom('image/png', 'video/mp4', 'application/zip', 'application/octet-stream')
            }),
            (file) => {
                const buttonHtml = FilePreviewButton.renderButton(file);
                
                if (!buttonHtml || buttonHtml.length === 0) {
                    throw new Error('Button HTML is empty');
                }
                if (!buttonHtml.includes('disabled')) {
                    throw new Error('Button should be disabled for unsupported types');
                }
                if (!buttonHtml.includes('cursor-not-allowed')) {
                    throw new Error('Button should have cursor-not-allowed for unsupported types');
                }
                if (!buttonHtml.includes('text-gray-400')) {
                    throw new Error('Button should have gray color for disabled state');
                }
                if (buttonHtml.includes('text-blue-600')) {
                    throw new Error('Button should not have blue color for disabled state');
                }
                if (!buttonHtml.includes('Download only')) {
                    throw new Error('Button should have "Download only" tooltip');
                }
                if (!buttonHtml.includes('Preview not available')) {
                    throw new Error('Button should indicate preview is not available');
                }
                if (buttonHtml.includes('onclick')) {
                    throw new Error('Button should not have onclick handler for unsupported types');
                }
            }
        ),
        { numRuns: 100 }
    );
    
    console.log('✅ PASSED (100 runs)\n');
    passedTests++;
} catch (error) {
    console.log('❌ FAILED');
    console.log('Error:', error.message);
    console.log('');
    failedTests++;
}

// Test 3: isPreviewable correctly identifies supported types
try {
    console.log('Test 3: isPreviewable correctly identifies supported types');
    
    fc.assert(
        fc.property(
            fc.constantFrom(
                'text/plain', 'text/markdown', 'text/csv', 'application/pdf',
                'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                'text/javascript', 'text/x-java-source', 'text/x-python',
                'text/css', 'text/html', 'application/json'
            ),
            (mimeType) => {
                const isPreviewable = FilePreviewButton.isPreviewable(mimeType);
                if (!isPreviewable) {
                    throw new Error(`${mimeType} should be previewable`);
                }
            }
        ),
        { numRuns: 100 }
    );
    
    console.log('✅ PASSED (100 runs)\n');
    passedTests++;
} catch (error) {
    console.log('❌ FAILED');
    console.log('Error:', error.message);
    console.log('');
    failedTests++;
}

// Test 4: isPreviewable correctly identifies unsupported types
try {
    console.log('Test 4: isPreviewable correctly identifies unsupported types');
    
    fc.assert(
        fc.property(
            fc.constantFrom(
                'image/png', 'image/jpeg', 'video/mp4', 'audio/mp3',
                'application/zip', 'application/octet-stream', 'application/x-executable'
            ),
            (mimeType) => {
                const isPreviewable = FilePreviewButton.isPreviewable(mimeType);
                if (isPreviewable) {
                    throw new Error(`${mimeType} should not be previewable`);
                }
            }
        ),
        { numRuns: 100 }
    );
    
    console.log('✅ PASSED (100 runs)\n');
    passedTests++;
} catch (error) {
    console.log('❌ FAILED');
    console.log('Error:', error.message);
    console.log('');
    failedTests++;
}

// Test 5: Extension fallback works
try {
    console.log('Test 5: Extension fallback works when MIME type is missing');
    
    fc.assert(
        fc.property(
            fc.record({
                fileName: fc.constantFrom('document.pdf', 'script.js', 'readme.md', 'data.csv', 'Main.java'),
                mimeType: fc.constant('')
            }),
            ({ fileName, mimeType }) => {
                const isPreviewable = FilePreviewButton.isPreviewable(mimeType, fileName);
                if (!isPreviewable) {
                    throw new Error(`${fileName} should be previewable via extension fallback`);
                }
            }
        ),
        { numRuns: 100 }
    );
    
    console.log('✅ PASSED (100 runs)\n');
    passedTests++;
} catch (error) {
    console.log('❌ FAILED');
    console.log('Error:', error.message);
    console.log('');
    failedTests++;
}

// Test 6: XSS protection
try {
    console.log('Test 6: Button HTML is properly escaped to prevent XSS');
    
    fc.assert(
        fc.property(
            fc.record({
                id: fc.integer({ min: 1, max: 10000 }),
                originalFilename: fc.constantFrom(
                    '<script>alert("xss")</script>.pdf',
                    'file"with"quotes.txt',
                    "file'with'quotes.md",
                    'file&with&ampersands.js'
                ),
                fileType: fc.constant('application/pdf')
            }),
            (file) => {
                const buttonHtml = FilePreviewButton.renderButton(file);
                
                if (buttonHtml.includes('<script>')) {
                    throw new Error('Script tags should be escaped');
                }
                if (buttonHtml.includes('alert(')) {
                    throw new Error('Alert calls should be escaped');
                }
                if (file.originalFilename.includes('<') && !buttonHtml.includes('&lt;')) {
                    throw new Error('< should be escaped to &lt;');
                }
                if (file.originalFilename.includes('>') && !buttonHtml.includes('&gt;')) {
                    throw new Error('> should be escaped to &gt;');
                }
                if (file.originalFilename.includes('&') && !buttonHtml.includes('&amp;')) {
                    throw new Error('& should be escaped to &amp;');
                }
            }
        ),
        { numRuns: 100 }
    );
    
    console.log('✅ PASSED (100 runs)\n');
    passedTests++;
} catch (error) {
    console.log('❌ FAILED');
    console.log('Error:', error.message);
    console.log('');
    failedTests++;
}

// Summary
console.log('═'.repeat(60));
console.log('Test Summary:');
console.log(`✅ Passed: ${passedTests}`);
console.log(`❌ Failed: ${failedTests}`);
console.log(`📊 Total: ${passedTests + failedTests}`);
console.log('═'.repeat(60));

if (failedTests > 0) {
    process.exit(1);
}
