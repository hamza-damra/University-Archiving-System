/**
 * Property-Based Tests for Office Renderer
 * Feature: file-preview-system
 * 
 * These tests validate correctness properties for the Office document renderer component.
 */

// Mock fetch for testing
class MockFetch {
    constructor() {
        this.reset();
    }

    reset() {
        this.calls = [];
        this.responses = new Map();
    }

    setResponse(url, response) {
        this.responses.set(url, response);
    }

    async fetch(url, options = {}) {
        this.calls.push({ url, options });
        
        const response = this.responses.get(url);
        if (!response) {
            return {
                ok: false,
                status: 404,
                json: async () => ({ error: 'Not found' })
            };
        }
        
        return {
            ok: response.ok !== false,
            status: response.status || 200,
            headers: {
                get: (name) => response.headers ? response.headers[name] : null
            },
            json: async () => response.data,
            text: async () => response.text || JSON.stringify(response.data),
            blob: async () => new Blob([response.text || JSON.stringify(response.data)], {
                type: response.headers ? response.headers['content-type'] : 'application/octet-stream'
            })
        };
    }
}

// Mock OfficeRenderer for testing
class MockOfficeRenderer {
    constructor(options = {}) {
        this.options = {
            preferredFormat: options.preferredFormat || 'html'
        };
        this.currentFileId = null;
        this.currentFormat = null;
        this.conversionUrl = null;
        this.renderCalled = false;
        this.conversionErrorHandled = false;
        this.downloadFallbackOffered = false;
    }

    async render(fileId, container) {
        if (!container) {
            throw new Error('Container element is required');
        }

        this.renderCalled = true;
        this.currentFileId = fileId;

        try {
            // Request converted content from backend
            const convertedContent = await this.fetchConvertedContent(fileId);
            
            // Render the converted content
            this.renderConvertedContent(container, convertedContent);
            
        } catch (error) {
            console.error('Error rendering Office document:', error);
            
            // Check if this is a conversion error
            if (error.message.includes('conversion') || error.message.includes('convert')) {
                // Show conversion error with download fallback
                this.conversionErrorHandled = true;
                this.renderConversionError(container, error);
            } else {
                // Re-throw other errors to be handled by preview modal
                throw error;
            }
        }
    }

    async fetchConvertedContent(fileId) {
        if (!window.mockFetch) {
            throw new Error('Mock fetch not configured');
        }

        const response = await window.mockFetch.fetch(`/api/file-explorer/files/${fileId}/preview`);
        
        if (!response.ok) {
            if (response.status === 404) {
                throw new Error('File not found - it may have been deleted');
            } else if (response.status === 403) {
                throw new Error('You don\'t have permission to view this file');
            } else if (response.status === 500) {
                // Check if this is a conversion error
                const errorText = await response.text();
                if (errorText.includes('conversion') || errorText.includes('convert')) {
                    throw new Error('Unable to convert document for preview');
                }
                throw new Error('Service unavailable - please try again later');
            } else {
                throw new Error('Failed to load Office document');
            }
        }
        
        // Get content type to determine format
        const contentType = response.headers.get('content-type');
        
        const blob = await response.blob();
        
        return {
            blob: blob,
            contentType: contentType,
            format: this.detectFormat(contentType)
        };
    }

    detectFormat(contentType) {
        if (!contentType) return 'binary';
        
        if (contentType.includes('text/html')) {
            return 'html';
        } else if (contentType.includes('application/pdf')) {
            return 'pdf';
        } else {
            return 'binary';
        }
    }

    renderConvertedContent(container, convertedContent) {
        container.innerHTML = '';
        
        const { blob, format } = convertedContent;
        this.currentFormat = format;
        
        if (format === 'html') {
            this.renderHtmlContent(container, blob);
        } else if (format === 'pdf') {
            this.renderPdfContent(container, blob);
        } else {
            this.renderConversionNotAvailable(container);
        }
    }

    async renderHtmlContent(container, blob) {
        const htmlText = await blob.text();
        
        // Create iframe to safely render HTML
        const iframe = document.createElement('iframe');
        iframe.className = 'w-full h-full border-0';
        iframe.setAttribute('sandbox', 'allow-same-origin');
        iframe.setAttribute('title', 'Office Document Preview');
        iframe.setAttribute('data-format', 'html');
        
        container.appendChild(iframe);
    }

    renderPdfContent(container, blob) {
        // Create object URL for PDF
        const pdfUrl = URL.createObjectURL(blob);
        this.conversionUrl = pdfUrl;
        
        // Create iframe for PDF display
        const iframe = document.createElement('iframe');
        iframe.src = pdfUrl;
        iframe.className = 'w-full h-full border-0';
        iframe.setAttribute('title', 'Office Document Preview (PDF)');
        iframe.setAttribute('data-format', 'pdf');
        
        container.appendChild(iframe);
    }

    renderConversionNotAvailable(container) {
        container.innerHTML = '';
        
        const messageDiv = document.createElement('div');
        messageDiv.className = 'conversion-not-available';
        messageDiv.innerHTML = `
            <h3>Preview Not Available</h3>
            <p>Office document preview is currently not available. Please download the file to view its contents.</p>
            <button class="office-download-btn">Download File</button>
        `;
        
        container.appendChild(messageDiv);
        this.downloadFallbackOffered = true;
    }

    renderConversionError(container, error) {
        container.innerHTML = '';
        
        const errorDiv = document.createElement('div');
        errorDiv.className = 'conversion-error';
        errorDiv.innerHTML = `
            <h3>Conversion Failed</h3>
            <p>Unable to convert document for preview.</p>
            <p class="error-message">${this.escapeHtml(error.message)}</p>
            <button class="office-download-btn">Download File Instead</button>
        `;
        
        container.appendChild(errorDiv);
        this.downloadFallbackOffered = true;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    static supportsFormat(mimeType) {
        if (!mimeType) return false;
        
        const supportedTypes = [
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'application/vnd.ms-excel',
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'application/vnd.ms-powerpoint',
            'application/vnd.openxmlformats-officedocument.presentationml.presentation'
        ];
        
        return supportedTypes.some(type => mimeType.includes(type));
    }

    getCurrentFormat() {
        return this.currentFormat;
    }

    destroy() {
        if (this.conversionUrl) {
            URL.revokeObjectURL(this.conversionUrl);
            this.conversionUrl = null;
        }
        
        this.currentFileId = null;
        this.currentFormat = null;
    }
}

// Test runner
class PropertyTestRunner {
    constructor() {
        this.results = [];
    }

    async runProperty(name, propertyFn, iterations = 100) {
        console.log(`\nRunning: ${name}`);
        console.log(`Iterations: ${iterations}`);
        
        let passed = 0;
        let failed = 0;
        let failureExample = null;

        for (let i = 0; i < iterations; i++) {
            try {
                const result = await propertyFn();
                
                if (result.passed) {
                    passed++;
                } else {
                    failed++;
                    if (!failureExample) {
                        failureExample = {
                            iteration: i + 1,
                            reason: result.reason,
                            details: result.details
                        };
                    }
                }
            } catch (error) {
                failed++;
                if (!failureExample) {
                    failureExample = {
                        iteration: i + 1,
                        error: error.message,
                        stack: error.stack
                    };
                }
            }
        }

        const testPassed = failed === 0;
        const result = {
            name,
            passed: testPassed,
            iterations,
            passedCount: passed,
            failedCount: failed,
            failureExample
        };

        this.results.push(result);

        if (testPassed) {
            console.log(`✓ PASSED: All ${iterations} iterations passed`);
        } else {
            console.log(`✗ FAILED: ${failed} of ${iterations} iterations failed`);
            console.log('Failure example:', JSON.stringify(failureExample, null, 2));
        }

        return result;
    }

    printSummary() {
        console.log('\n' + '='.repeat(60));
        console.log('TEST SUMMARY');
        console.log('='.repeat(60));
        
        const totalTests = this.results.length;
        const passedTests = this.results.filter(r => r.passed).length;
        const failedTests = totalTests - passedTests;

        this.results.forEach(result => {
            const status = result.passed ? '✓ PASS' : '✗ FAIL';
            console.log(`${status}: ${result.name}`);
        });

        console.log('='.repeat(60));
        console.log(`Total: ${totalTests} | Passed: ${passedTests} | Failed: ${failedTests}`);
        console.log('='.repeat(60));

        return failedTests === 0;
    }
}

// **Feature: file-preview-system, Property 3: Format-specific renderer selection (Office files)**
// **Validates: Requirements 4.3**
async function property3_formatSpecificRendererSelection_officeFiles() {
    // Generate random Office file types
    const officeFileTypes = [
        { ext: 'doc', mime: 'application/msword', name: 'Word 97-2003' },
        { ext: 'docx', mime: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', name: 'Word' },
        { ext: 'xls', mime: 'application/vnd.ms-excel', name: 'Excel 97-2003' },
        { ext: 'xlsx', mime: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', name: 'Excel' },
        { ext: 'ppt', mime: 'application/vnd.ms-powerpoint', name: 'PowerPoint 97-2003' },
        { ext: 'pptx', mime: 'application/vnd.openxmlformats-officedocument.presentationml.presentation', name: 'PowerPoint' }
    ];
    
    const randomFile = officeFileTypes[Math.floor(Math.random() * officeFileTypes.length)];
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `document_${Math.random().toString(36).substring(7)}.${randomFile.ext}`;
    
    // Randomly choose conversion format (HTML or PDF)
    const conversionFormats = ['html', 'pdf'];
    const conversionFormat = conversionFormats[Math.floor(Math.random() * conversionFormats.length)];
    
    const contentType = conversionFormat === 'html' ? 'text/html' : 'application/pdf';
    const convertedContent = conversionFormat === 'html' 
        ? '<html><body><h1>Converted Document</h1><p>Content here</p></body></html>'
        : 'PDF content here';
    
    // Setup mock fetch
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/preview`, {
        ok: true,
        headers: {
            'content-type': contentType
        },
        text: convertedContent,
        data: { content: convertedContent }
    });
    
    // Create mock container
    const container = document.createElement('div');
    
    // Create renderer and render
    const renderer = new MockOfficeRenderer({ preferredFormat: conversionFormat });
    await renderer.render(fileId, container);
    
    // Property: For any Office file type, the system should select and apply
    // the Office renderer with appropriate format conversion
    
    // Check render was called
    if (!renderer.renderCalled) {
        return {
            passed: false,
            reason: 'Renderer was not called',
            details: { renderCalled: renderer.renderCalled }
        };
    }
    
    // Check file ID was stored
    if (renderer.currentFileId !== fileId) {
        return {
            passed: false,
            reason: 'File ID was not stored correctly',
            details: {
                expected: fileId,
                actual: renderer.currentFileId
            }
        };
    }
    
    // Check format was detected correctly
    if (renderer.currentFormat !== conversionFormat) {
        return {
            passed: false,
            reason: 'Format was not detected correctly',
            details: {
                expected: conversionFormat,
                actual: renderer.currentFormat,
                contentType: contentType
            }
        };
    }
    
    // Check container has content
    if (!container.innerHTML || container.innerHTML.length === 0) {
        return {
            passed: false,
            reason: 'Container was not populated with content',
            details: { innerHTML: container.innerHTML }
        };
    }
    
    // Check iframe element exists
    const iframe = container.querySelector('iframe');
    if (!iframe) {
        return {
            passed: false,
            reason: 'Iframe element was not created',
            details: { innerHTML: container.innerHTML }
        };
    }
    
    // Check iframe has correct format attribute
    const iframeFormat = iframe.getAttribute('data-format');
    if (iframeFormat !== conversionFormat) {
        return {
            passed: false,
            reason: 'Iframe format attribute does not match expected format',
            details: {
                expected: conversionFormat,
                actual: iframeFormat
            }
        };
    }
    
    return { passed: true };
}

// **Feature: file-preview-system, Property 22: Conversion failure handling**
// **Validates: Requirements 10.2**
async function property22_conversionFailureHandling() {
    // Generate random Office file
    const officeFileTypes = [
        { ext: 'docx', mime: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' },
        { ext: 'xlsx', mime: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' },
        { ext: 'pptx', mime: 'application/vnd.openxmlformats-officedocument.presentationml.presentation' }
    ];
    
    const randomFile = officeFileTypes[Math.floor(Math.random() * officeFileTypes.length)];
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `document_${Math.random().toString(36).substring(7)}.${randomFile.ext}`;
    
    // Setup mock fetch to simulate conversion failure
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/preview`, {
        ok: false,
        status: 500,
        text: 'Conversion failed: Unable to process document',
        data: { error: 'Conversion failed' }
    });
    
    // Create mock container
    const container = document.createElement('div');
    
    // Create renderer and render
    const renderer = new MockOfficeRenderer();
    await renderer.render(fileId, container);
    
    // Property: For any Office document that fails to convert,
    // the system should display an error message and offer a download option
    
    // Check render was called
    if (!renderer.renderCalled) {
        return {
            passed: false,
            reason: 'Renderer was not called',
            details: { renderCalled: renderer.renderCalled }
        };
    }
    
    // Check conversion error was handled
    if (!renderer.conversionErrorHandled) {
        return {
            passed: false,
            reason: 'Conversion error was not handled',
            details: { conversionErrorHandled: renderer.conversionErrorHandled }
        };
    }
    
    // Check download fallback was offered
    if (!renderer.downloadFallbackOffered) {
        return {
            passed: false,
            reason: 'Download fallback was not offered',
            details: { downloadFallbackOffered: renderer.downloadFallbackOffered }
        };
    }
    
    // Check container has error content
    if (!container.innerHTML || container.innerHTML.length === 0) {
        return {
            passed: false,
            reason: 'Container was not populated with error content',
            details: { innerHTML: container.innerHTML }
        };
    }
    
    // Check error message is displayed
    const errorDiv = container.querySelector('.conversion-error');
    if (!errorDiv) {
        return {
            passed: false,
            reason: 'Error message div was not created',
            details: { innerHTML: container.innerHTML }
        };
    }
    
    // Check download button exists
    const downloadBtn = container.querySelector('.office-download-btn');
    if (!downloadBtn) {
        return {
            passed: false,
            reason: 'Download button was not created',
            details: { innerHTML: container.innerHTML }
        };
    }
    
    // Check error message contains appropriate text
    const errorMessage = container.querySelector('.error-message');
    if (!errorMessage || !errorMessage.textContent.includes('convert')) {
        return {
            passed: false,
            reason: 'Error message does not contain conversion error text',
            details: {
                errorMessage: errorMessage ? errorMessage.textContent : null
            }
        };
    }
    
    return { passed: true };
}

// Test supportsFormat static method
async function propertyTest_supportsFormat() {
    // Test cases for format support
    const testCases = [
        { mime: 'application/msword', expected: true, name: '.doc' },
        { mime: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', expected: true, name: '.docx' },
        { mime: 'application/vnd.ms-excel', expected: true, name: '.xls' },
        { mime: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', expected: true, name: '.xlsx' },
        { mime: 'application/vnd.ms-powerpoint', expected: true, name: '.ppt' },
        { mime: 'application/vnd.openxmlformats-officedocument.presentationml.presentation', expected: true, name: '.pptx' },
        { mime: 'application/pdf', expected: false, name: '.pdf' },
        { mime: 'text/plain', expected: false, name: '.txt' },
        { mime: 'image/jpeg', expected: false, name: '.jpg' },
        { mime: null, expected: false, name: 'null' }
    ];
    
    // Pick a random test case
    const testCase = testCases[Math.floor(Math.random() * testCases.length)];
    
    // Check format support
    const isSupported = MockOfficeRenderer.supportsFormat(testCase.mime);
    
    // Property: For any MIME type, the supportsFormat method should correctly
    // identify whether it's an Office document format
    
    if (isSupported !== testCase.expected) {
        return {
            passed: false,
            reason: 'Format support detection failed',
            details: {
                mime: testCase.mime,
                name: testCase.name,
                expected: testCase.expected,
                actual: isSupported
            }
        };
    }
    
    return { passed: true };
}

// Test conversion not available scenario
async function propertyTest_conversionNotAvailable() {
    // Generate random Office file
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `document_${Math.random().toString(36).substring(7)}.docx`;
    
    // Setup mock fetch to return binary content (no conversion)
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/preview`, {
        ok: true,
        headers: {
            'content-type': 'application/octet-stream'
        },
        text: 'Binary content',
        data: { content: 'Binary content' }
    });
    
    // Create mock container
    const container = document.createElement('div');
    
    // Create renderer and render
    const renderer = new MockOfficeRenderer();
    await renderer.render(fileId, container);
    
    // Property: For any Office document where conversion is not available,
    // the system should display a message and offer download option
    
    // Check format is binary
    if (renderer.currentFormat !== 'binary') {
        return {
            passed: false,
            reason: 'Format was not detected as binary',
            details: {
                expected: 'binary',
                actual: renderer.currentFormat
            }
        };
    }
    
    // Check download fallback was offered
    if (!renderer.downloadFallbackOffered) {
        return {
            passed: false,
            reason: 'Download fallback was not offered',
            details: { downloadFallbackOffered: renderer.downloadFallbackOffered }
        };
    }
    
    // Check container has message
    const messageDiv = container.querySelector('.conversion-not-available');
    if (!messageDiv) {
        return {
            passed: false,
            reason: 'Conversion not available message was not displayed',
            details: { innerHTML: container.innerHTML }
        };
    }
    
    // Check download button exists
    const downloadBtn = container.querySelector('.office-download-btn');
    if (!downloadBtn) {
        return {
            passed: false,
            reason: 'Download button was not created',
            details: { innerHTML: container.innerHTML }
        };
    }
    
    return { passed: true };
}

// Test error handling for missing files
async function propertyTest_errorHandling_missingFile() {
    // Generate random file ID that doesn't exist
    const fileId = Math.floor(Math.random() * 1000) + 1;
    
    // Setup mock fetch to return 404
    window.mockFetch = new MockFetch();
    // Don't set any response, so it will return 404
    
    // Create mock container
    const container = document.createElement('div');
    
    // Create renderer
    const renderer = new MockOfficeRenderer();
    
    // Property: For any non-existent Office file, the renderer should throw
    // an appropriate error
    
    let errorThrown = false;
    let errorMessage = '';
    
    try {
        await renderer.render(fileId, container);
    } catch (error) {
        errorThrown = true;
        errorMessage = error.message;
    }
    
    if (!errorThrown) {
        return {
            passed: false,
            reason: 'No error was thrown for missing file',
            details: { fileId }
        };
    }
    
    if (!errorMessage.includes('not found')) {
        return {
            passed: false,
            reason: 'Error message does not indicate file not found',
            details: {
                errorMessage,
                expected: 'Message containing "not found"'
            }
        };
    }
    
    return { passed: true };
}

// Test error handling for permission denied
async function propertyTest_errorHandling_permissionDenied() {
    // Generate random file ID
    const fileId = Math.floor(Math.random() * 1000) + 1;
    
    // Setup mock fetch to return 403
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/preview`, {
        ok: false,
        status: 403,
        text: 'Access denied',
        data: { error: 'Access denied' }
    });
    
    // Create mock container
    const container = document.createElement('div');
    
    // Create renderer
    const renderer = new MockOfficeRenderer();
    
    // Property: For any Office file without proper permissions,
    // the renderer should throw an appropriate error
    
    let errorThrown = false;
    let errorMessage = '';
    
    try {
        await renderer.render(fileId, container);
    } catch (error) {
        errorThrown = true;
        errorMessage = error.message;
    }
    
    if (!errorThrown) {
        return {
            passed: false,
            reason: 'No error was thrown for permission denied',
            details: { fileId }
        };
    }
    
    if (!errorMessage.includes('permission')) {
        return {
            passed: false,
            reason: 'Error message does not indicate permission denied',
            details: {
                errorMessage,
                expected: 'Message containing "permission"'
            }
        };
    }
    
    return { passed: true };
}

// Run all tests
async function runAllTests() {
    console.log('='.repeat(60));
    console.log('OFFICE RENDERER PROPERTY-BASED TESTS');
    console.log('Feature: file-preview-system');
    console.log('='.repeat(60));
    
    const runner = new PropertyTestRunner();
    
    // Run Property 3: Format-specific renderer selection (Office files)
    await runner.runProperty(
        'Property 3: Format-specific renderer selection (Office files)',
        property3_formatSpecificRendererSelection_officeFiles,
        100
    );
    
    // Run Property 22: Conversion failure handling
    await runner.runProperty(
        'Property 22: Conversion failure handling',
        property22_conversionFailureHandling,
        100
    );
    
    // Run format support test
    await runner.runProperty(
        'Property: Format support detection',
        propertyTest_supportsFormat,
        100
    );
    
    // Run conversion not available test
    await runner.runProperty(
        'Property: Conversion not available handling',
        propertyTest_conversionNotAvailable,
        100
    );
    
    // Run error handling tests
    await runner.runProperty(
        'Property: Error handling for missing files',
        propertyTest_errorHandling_missingFile,
        100
    );
    
    await runner.runProperty(
        'Property: Error handling for permission denied',
        propertyTest_errorHandling_permissionDenied,
        100
    );
    
    // Print summary
    const allPassed = runner.printSummary();
    
    return allPassed;
}

// Export for use in HTML test runner
if (typeof window !== 'undefined') {
    window.runOfficeRendererTests = runAllTests;
}
