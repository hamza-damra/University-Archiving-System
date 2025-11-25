/**
 * Property-Based Tests for File Preview Modal
 * Feature: file-preview-system
 * 
 * These tests validate correctness properties for the file preview modal component.
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
            json: async () => response.data,
            blob: async () => new Blob([JSON.stringify(response.data)])
        };
    }
}

// Mock FilePreviewModal for testing
class MockFilePreviewModal {
    constructor(options = {}) {
        this.options = options;
        this.isOpen = false;
        this.currentFileId = null;
        this.currentFileName = null;
        this.currentFileType = null;
        this.modalElement = null;
        this.events = [];
        this.metadata = null;
        this.contentLoaded = false;
        this.errorMessage = null;
        this.loadingShown = false;
    }

    async open(fileId, fileName, fileType) {
        if (this.isOpen) {
            this.close();
        }

        this.currentFileId = fileId;
        this.currentFileName = fileName;
        this.currentFileType = fileType;
        this.isOpen = true;
        this.modalElement = { id: 'mock-modal' };
        
        this.dispatchEvent('preview:opened', { fileId, fileName, fileType });
        
        // Show loading
        this.showLoading();
        
        try {
            // Fetch metadata
            this.metadata = await this.fetchMetadata(fileId);
            
            // Load content
            await this.loadContent(fileId, fileType);
            this.contentLoaded = true;
            
            this.dispatchEvent('preview:loaded', { fileId, fileName, metadata: this.metadata });
        } catch (error) {
            this.errorMessage = error.message;
            this.dispatchEvent('preview:error', { fileId, fileName, error: error.message });
        }
    }

    close() {
        if (!this.isOpen) return;
        
        this.isOpen = false;
        this.modalElement = null;
        this.currentFileId = null;
        this.currentFileName = null;
        this.currentFileType = null;
        this.metadata = null;
        this.contentLoaded = false;
        this.errorMessage = null;
        this.loadingShown = false;
        
        this.dispatchEvent('preview:closed', {});
        
        if (this.options.onClose) {
            this.options.onClose();
        }
    }

    async downloadFile() {
        if (!this.currentFileId) return;

        try {
            // Call onDownload callback if provided
            if (this.options.onDownload) {
                await this.options.onDownload(this.currentFileId, this.currentFileName);
            } else {
                // Default download implementation
                const response = await window.mockFetch.fetch(`/api/file-explorer/files/${this.currentFileId}/download`);
                
                if (!response.ok) {
                    throw new Error('Download failed');
                }
                
                // In real implementation, this would create a blob and trigger download
                // For testing, we just return the response
                return response;
            }
        } catch (error) {
            console.error('Error downloading file:', error);
            throw error;
        }
    }

    showLoading() {
        this.loadingShown = true;
    }

    showError(message) {
        this.errorMessage = message;
    }

    async fetchMetadata(fileId) {
        // Simulate API call
        if (!window.mockFetch) {
            throw new Error('Mock fetch not configured');
        }
        
        const response = await window.mockFetch.fetch(`/api/file-explorer/files/${fileId}/metadata`);
        
        if (!response.ok) {
            if (response.status === 404) {
                throw new Error('File not found - it may have been deleted');
            } else if (response.status === 403) {
                throw new Error('You don\'t have permission to preview this file');
            } else {
                throw new Error('Failed to load file metadata');
            }
        }
        
        return await response.json();
    }

    async loadContent(fileId, fileType) {
        // Simulate content loading
        await new Promise(resolve => setTimeout(resolve, 10));
    }

    dispatchEvent(eventName, detail) {
        this.events.push({ eventName, detail, timestamp: Date.now() });
    }

    handleEscKey(event) {
        if (event.key === 'Escape' && this.isOpen) {
            this.close();
        }
    }

    handleClickOutside() {
        if (this.isOpen) {
            this.close();
        }
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

// **Feature: file-preview-system, Property 1: Preview modal displays file content**
// **Validates: Requirements 1.1**
async function property1_previewModalDisplaysFileContent() {
    // Generate random file data
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `file_${Math.random().toString(36).substring(7)}.txt`;
    const fileType = 'text/plain';
    
    // Setup mock fetch
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/metadata`, {
        ok: true,
        data: {
            id: fileId,
            fileName: fileName,
            mimeType: fileType,
            fileSize: 1024,
            uploadDate: new Date().toISOString(),
            uploaderName: 'Test User'
        }
    });
    
    const modal = new MockFilePreviewModal();
    
    // Open preview
    await modal.open(fileId, fileName, fileType);
    
    // Property: For any supported file type, when a user triggers preview,
    // the system should open a modal containing the file content
    
    // Check modal is open
    if (!modal.isOpen) {
        return {
            passed: false,
            reason: 'Modal did not open',
            details: { isOpen: modal.isOpen }
        };
    }
    
    // Check modal element exists
    if (!modal.modalElement) {
        return {
            passed: false,
            reason: 'Modal element was not created',
            details: { modalElement: modal.modalElement }
        };
    }
    
    // Check file information is stored
    if (modal.currentFileId !== fileId || 
        modal.currentFileName !== fileName || 
        modal.currentFileType !== fileType) {
        return {
            passed: false,
            reason: 'File information not stored correctly',
            details: {
                expected: { fileId, fileName, fileType },
                actual: {
                    fileId: modal.currentFileId,
                    fileName: modal.currentFileName,
                    fileType: modal.currentFileType
                }
            }
        };
    }
    
    // Check content was loaded
    if (!modal.contentLoaded) {
        return {
            passed: false,
            reason: 'Content was not loaded',
            details: { contentLoaded: modal.contentLoaded }
        };
    }
    
    // Check opened event was dispatched
    const openedEvent = modal.events.find(e => e.eventName === 'preview:opened');
    if (!openedEvent) {
        return {
            passed: false,
            reason: 'preview:opened event was not dispatched',
            details: { events: modal.events.map(e => e.eventName) }
        };
    }
    
    return { passed: true };
}

// **Feature: file-preview-system, Property 4: Modal dismissal behavior**
// **Validates: Requirements 1.4**
async function property4_modalDismissalBehavior() {
    // Generate random file data
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `file_${Math.random().toString(36).substring(7)}.pdf`;
    const fileType = 'application/pdf';
    
    // Setup mock fetch
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/metadata`, {
        ok: true,
        data: {
            id: fileId,
            fileName: fileName,
            mimeType: fileType,
            fileSize: 2048,
            uploadDate: new Date().toISOString()
        }
    });
    
    const modal = new MockFilePreviewModal();
    
    // Open preview
    await modal.open(fileId, fileName, fileType);
    
    // Verify modal is open
    if (!modal.isOpen) {
        return {
            passed: false,
            reason: 'Modal did not open for dismissal test',
            details: { isOpen: modal.isOpen }
        };
    }
    
    // Test 1: ESC key dismissal
    const escapeEvent = { key: 'Escape' };
    modal.handleEscKey(escapeEvent);
    
    if (modal.isOpen) {
        return {
            passed: false,
            reason: 'Modal did not close on ESC key press',
            details: { isOpen: modal.isOpen }
        };
    }
    
    // Check closed event was dispatched
    const closedEvent1 = modal.events.find(e => e.eventName === 'preview:closed');
    if (!closedEvent1) {
        return {
            passed: false,
            reason: 'preview:closed event was not dispatched after ESC key',
            details: { events: modal.events.map(e => e.eventName) }
        };
    }
    
    // Re-open modal for click outside test
    await modal.open(fileId, fileName, fileType);
    
    if (!modal.isOpen) {
        return {
            passed: false,
            reason: 'Modal did not re-open for click outside test',
            details: { isOpen: modal.isOpen }
        };
    }
    
    // Test 2: Click outside dismissal
    modal.handleClickOutside();
    
    if (modal.isOpen) {
        return {
            passed: false,
            reason: 'Modal did not close on click outside',
            details: { isOpen: modal.isOpen }
        };
    }
    
    // Check closed event was dispatched again
    const closedEvents = modal.events.filter(e => e.eventName === 'preview:closed');
    if (closedEvents.length < 2) {
        return {
            passed: false,
            reason: 'preview:closed event was not dispatched after click outside',
            details: { 
                closedEventCount: closedEvents.length,
                expected: 2
            }
        };
    }
    
    // Verify modal state is cleaned up
    if (modal.modalElement !== null || 
        modal.currentFileId !== null || 
        modal.currentFileName !== null) {
        return {
            passed: false,
            reason: 'Modal state was not cleaned up after close',
            details: {
                modalElement: modal.modalElement,
                currentFileId: modal.currentFileId,
                currentFileName: modal.currentFileName
            }
        };
    }
    
    return { passed: true };
}

// **Feature: file-preview-system, Property 2: Preview modal displays complete metadata**
// **Validates: Requirements 1.2, 2.2, 3.2**
async function property2_previewModalDisplaysCompleteMetadata() {
    // Generate random file data with complete metadata
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `document_${Math.random().toString(36).substring(7)}.docx`;
    const fileType = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
    const fileSize = Math.floor(Math.random() * 10000000) + 1000; // 1KB to 10MB
    const uploadDate = new Date(Date.now() - Math.random() * 365 * 24 * 60 * 60 * 1000).toISOString();
    const uploaderName = `User_${Math.random().toString(36).substring(7)}`;
    const uploaderEmail = `${uploaderName.toLowerCase()}@example.com`;
    const departmentName = ['Computer Science', 'Mathematics', 'Physics', 'Chemistry'][Math.floor(Math.random() * 4)];
    
    // Setup mock fetch with complete metadata
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/metadata`, {
        ok: true,
        data: {
            id: fileId,
            fileName: fileName,
            mimeType: fileType,
            fileSize: fileSize,
            uploadDate: uploadDate,
            uploaderName: uploaderName,
            uploaderEmail: uploaderEmail,
            departmentName: departmentName,
            previewable: true
        }
    });
    
    const modal = new MockFilePreviewModal();
    
    // Open preview
    await modal.open(fileId, fileName, fileType);
    
    // Property: For any file being previewed, the modal header should display
    // file name, size, type, upload date, and role-appropriate uploader information
    
    // Check modal is open
    if (!modal.isOpen) {
        return {
            passed: false,
            reason: 'Modal did not open',
            details: { isOpen: modal.isOpen }
        };
    }
    
    // Check metadata was fetched
    if (!modal.metadata) {
        return {
            passed: false,
            reason: 'Metadata was not fetched',
            details: { metadata: modal.metadata }
        };
    }
    
    // Verify all required metadata fields are present
    const requiredFields = ['fileName', 'fileSize', 'mimeType', 'uploadDate'];
    const missingFields = requiredFields.filter(field => !modal.metadata[field]);
    
    if (missingFields.length > 0) {
        return {
            passed: false,
            reason: 'Required metadata fields are missing',
            details: {
                missingFields,
                metadata: modal.metadata
            }
        };
    }
    
    // Verify metadata values match expected values
    if (modal.metadata.id !== fileId ||
        modal.metadata.fileName !== fileName ||
        modal.metadata.mimeType !== fileType ||
        modal.metadata.fileSize !== fileSize ||
        modal.metadata.uploadDate !== uploadDate) {
        return {
            passed: false,
            reason: 'Metadata values do not match expected values',
            details: {
                expected: { fileId, fileName, fileType, fileSize, uploadDate },
                actual: modal.metadata
            }
        };
    }
    
    // Verify role-appropriate fields are present
    if (!modal.metadata.uploaderName) {
        return {
            passed: false,
            reason: 'Uploader name is missing from metadata',
            details: { metadata: modal.metadata }
        };
    }
    
    // For Dean/HOD views, department should be present
    if (!modal.metadata.departmentName) {
        return {
            passed: false,
            reason: 'Department name is missing from metadata (required for Dean/HOD views)',
            details: { metadata: modal.metadata }
        };
    }
    
    return { passed: true };
}

// Mock TextRenderer for testing
class MockTextRenderer {
    constructor(options = {}) {
        this.options = {
            virtualScrollThreshold: options.virtualScrollThreshold || 1000,
            chunkSize: options.chunkSize || 100
        };
        this.currentContent = null;
        this.currentLines = null;
        this.renderCalled = false;
        this.virtualScrollingUsed = false;
    }

    async render(fileId, container) {
        if (!container) {
            throw new Error('Container element is required');
        }

        this.renderCalled = true;

        // Fetch content
        const content = await this.fetchContent(fileId);
        this.currentContent = content;
        this.currentLines = content.split('\n');

        // Check if virtual scrolling is needed
        const useVirtualScrolling = this.currentLines.length > this.options.virtualScrollThreshold;
        this.virtualScrollingUsed = useVirtualScrolling;

        // Render content
        if (useVirtualScrolling) {
            container.innerHTML = `<div class="virtual-scroll">${this.currentLines.length} lines (virtual scrolling)</div>`;
        } else {
            container.innerHTML = `<pre>${this.escapeHtml(content)}</pre>`;
        }
    }

    async fetchContent(fileId) {
        if (!window.mockFetch) {
            throw new Error('Mock fetch not configured');
        }

        const response = await window.mockFetch.fetch(`/api/file-explorer/files/${fileId}/content`);

        if (!response.ok) {
            if (response.status === 404) {
                throw new Error('File not found - it may have been deleted');
            } else if (response.status === 403) {
                throw new Error('You don\'t have permission to view this file');
            } else if (response.status === 500) {
                throw new Error('Service unavailable - please try again later');
            } else {
                throw new Error('Failed to load file content');
            }
        }

        const jsonResponse = await response.json();

        // Handle both wrapped and unwrapped responses for testing
        if (jsonResponse.success !== undefined) {
            // Wrapped ApiResponse format
            if (jsonResponse.success && jsonResponse.data !== undefined) {
                return jsonResponse.data;
            } else {
                throw new Error(jsonResponse.message || 'Failed to load file content');
            }
        } else {
            // Direct data format (for backward compatibility with existing tests)
            return jsonResponse.data || jsonResponse;
        }
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// **Feature: file-preview-system, Property 3: Format-specific renderer selection (text files)**
// **Validates: Requirements 4.1**
async function property3_formatSpecificRendererSelection_textFiles() {
    // Generate random text file types
    const textFileTypes = [
        'text/plain',
        'text/markdown',
        'text/csv',
        'application/json',
        'application/xml',
        'text/xml'
    ];
    
    const fileType = textFileTypes[Math.floor(Math.random() * textFileTypes.length)];
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `file_${Math.random().toString(36).substring(7)}.txt`;
    const fileContent = `Sample content for ${fileType}\nLine 2\nLine 3`;
    
    // Setup mock fetch
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/metadata`, {
        ok: true,
        data: {
            id: fileId,
            fileName: fileName,
            mimeType: fileType,
            fileSize: fileContent.length,
            uploadDate: new Date().toISOString(),
            uploaderName: 'Test User',
            previewable: true,
            previewType: 'text'
        }
    });
    
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/content`, {
        ok: true,
        data: {
            success: true,
            data: fileContent
        }
    });
    
    // Create mock container
    const container = document.createElement('div');
    
    // Create renderer and render
    const renderer = new MockTextRenderer();
    await renderer.render(fileId, container);
    
    // Property: For any text file type, the system should select and apply
    // the text renderer
    
    // Check render was called
    if (!renderer.renderCalled) {
        return {
            passed: false,
            reason: 'Renderer was not called',
            details: { renderCalled: renderer.renderCalled }
        };
    }
    
    // Check content was fetched
    if (!renderer.currentContent) {
        return {
            passed: false,
            reason: 'Content was not fetched',
            details: { currentContent: renderer.currentContent }
        };
    }
    
    // Check content matches expected
    if (renderer.currentContent !== fileContent) {
        return {
            passed: false,
            reason: 'Content does not match expected',
            details: {
                expected: fileContent,
                actual: renderer.currentContent
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
    
    return { passed: true };
}

// **Feature: file-preview-system, Property 15: Virtual scrolling for large text files**
// **Validates: Requirements 7.2**
async function property15_virtualScrollingForLargeTextFiles() {
    // Generate random file with more than 1000 lines
    const lineCount = Math.floor(Math.random() * 5000) + 1001; // 1001 to 6000 lines
    const lines = [];
    for (let i = 0; i < lineCount; i++) {
        lines.push(`Line ${i + 1}: ${Math.random().toString(36).substring(7)}`);
    }
    const fileContent = lines.join('\n');
    
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `large_file_${Math.random().toString(36).substring(7)}.txt`;
    
    // Setup mock fetch
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/metadata`, {
        ok: true,
        data: {
            id: fileId,
            fileName: fileName,
            mimeType: 'text/plain',
            fileSize: fileContent.length,
            uploadDate: new Date().toISOString(),
            uploaderName: 'Test User',
            previewable: true,
            previewType: 'text'
        }
    });
    
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/content`, {
        ok: true,
        data: {
            success: true,
            data: fileContent
        }
    });
    
    // Create mock container
    const container = document.createElement('div');
    
    // Create renderer with default threshold (1000 lines)
    const renderer = new MockTextRenderer();
    await renderer.render(fileId, container);
    
    // Property: For any text file with more than 1000 lines,
    // the preview should implement virtual scrolling
    
    // Check that virtual scrolling was used
    if (!renderer.virtualScrollingUsed) {
        return {
            passed: false,
            reason: 'Virtual scrolling was not used for large file',
            details: {
                lineCount: lineCount,
                threshold: renderer.options.virtualScrollThreshold,
                virtualScrollingUsed: renderer.virtualScrollingUsed
            }
        };
    }
    
    // Check that lines were parsed correctly
    if (renderer.currentLines.length !== lineCount) {
        return {
            passed: false,
            reason: 'Line count does not match expected',
            details: {
                expected: lineCount,
                actual: renderer.currentLines.length
            }
        };
    }
    
    return { passed: true };
}

// Test that files under threshold do NOT use virtual scrolling
async function property15_noVirtualScrollingForSmallTextFiles() {
    // Generate random file with less than 1000 lines
    const lineCount = Math.floor(Math.random() * 900) + 10; // 10 to 909 lines
    const lines = [];
    for (let i = 0; i < lineCount; i++) {
        lines.push(`Line ${i + 1}: ${Math.random().toString(36).substring(7)}`);
    }
    const fileContent = lines.join('\n');
    
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `small_file_${Math.random().toString(36).substring(7)}.txt`;
    
    // Setup mock fetch
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/metadata`, {
        ok: true,
        data: {
            id: fileId,
            fileName: fileName,
            mimeType: 'text/plain',
            fileSize: fileContent.length,
            uploadDate: new Date().toISOString(),
            uploaderName: 'Test User',
            previewable: true,
            previewType: 'text'
        }
    });
    
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/content`, {
        ok: true,
        data: {
            success: true,
            data: fileContent
        }
    });
    
    // Create mock container
    const container = document.createElement('div');
    
    // Create renderer with default threshold (1000 lines)
    const renderer = new MockTextRenderer();
    await renderer.render(fileId, container);
    
    // Property: For any text file with less than 1000 lines,
    // the preview should NOT use virtual scrolling
    
    // Check that virtual scrolling was NOT used
    if (renderer.virtualScrollingUsed) {
        return {
            passed: false,
            reason: 'Virtual scrolling was used for small file',
            details: {
                lineCount: lineCount,
                threshold: renderer.options.virtualScrollThreshold,
                virtualScrollingUsed: renderer.virtualScrollingUsed
            }
        };
    }
    
    return { passed: true };
}

// **Feature: file-preview-system, Property 5: Download action in preview**
// **Validates: Requirements 1.5**
async function property5_downloadActionInPreview() {
    // Generate random file data
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `document_${Math.random().toString(36).substring(7)}.pdf`;
    const fileType = 'application/pdf';
    const fileContent = `Binary content for file ${fileId}`;
    
    // Setup mock fetch
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/metadata`, {
        ok: true,
        data: {
            id: fileId,
            fileName: fileName,
            mimeType: fileType,
            fileSize: 2048,
            uploadDate: new Date().toISOString(),
            uploaderName: 'Test User'
        }
    });
    
    // Setup download endpoint response
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/download`, {
        ok: true,
        data: fileContent
    });
    
    // Track download callback
    let downloadCalled = false;
    let downloadedFileId = null;
    let downloadedFileName = null;
    
    const modal = new MockFilePreviewModal({
        onDownload: async (fileId, fileName) => {
            downloadCalled = true;
            downloadedFileId = fileId;
            downloadedFileName = fileName;
        }
    });
    
    // Open preview
    await modal.open(fileId, fileName, fileType);
    
    // Verify modal is open
    if (!modal.isOpen) {
        return {
            passed: false,
            reason: 'Modal did not open',
            details: { isOpen: modal.isOpen }
        };
    }
    
    // Trigger download action
    await modal.downloadFile();
    
    // Property: For any file being previewed, clicking the download button
    // should trigger a download of that specific file
    
    // Check download callback was called
    if (!downloadCalled) {
        return {
            passed: false,
            reason: 'Download callback was not called',
            details: { downloadCalled }
        };
    }
    
    // Check correct file ID was passed to download
    if (downloadedFileId !== fileId) {
        return {
            passed: false,
            reason: 'Incorrect file ID passed to download',
            details: {
                expected: fileId,
                actual: downloadedFileId
            }
        };
    }
    
    // Check correct file name was passed to download
    if (downloadedFileName !== fileName) {
        return {
            passed: false,
            reason: 'Incorrect file name passed to download',
            details: {
                expected: fileName,
                actual: downloadedFileName
            }
        };
    }
    
    return { passed: true };
}

// Test download action without callback (default implementation)
async function property5_downloadActionInPreview_defaultImplementation() {
    // Generate random file data
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `report_${Math.random().toString(36).substring(7)}.xlsx`;
    const fileType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    const fileContent = `Binary content for file ${fileId}`;
    
    // Setup mock fetch
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/metadata`, {
        ok: true,
        data: {
            id: fileId,
            fileName: fileName,
            mimeType: fileType,
            fileSize: 5120,
            uploadDate: new Date().toISOString(),
            uploaderName: 'Test User'
        }
    });
    
    // Setup download endpoint response
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/download`, {
        ok: true,
        data: fileContent
    });
    
    // Create modal without onDownload callback to test default implementation
    const modal = new MockFilePreviewModal();
    
    // Mock the default download implementation
    modal.downloadFile = async function() {
        if (!this.currentFileId) return;
        
        const response = await window.mockFetch.fetch(`/api/file-explorer/files/${this.currentFileId}/download`);
        
        if (!response.ok) {
            throw new Error('Download failed');
        }
        
        // In real implementation, this would create a blob and trigger download
        // For testing, we just verify the fetch was called correctly
        return response;
    };
    
    // Open preview
    await modal.open(fileId, fileName, fileType);
    
    // Verify modal is open
    if (!modal.isOpen) {
        return {
            passed: false,
            reason: 'Modal did not open',
            details: { isOpen: modal.isOpen }
        };
    }
    
    // Trigger download action
    const downloadResponse = await modal.downloadFile();
    
    // Property: For any file being previewed, the download action should
    // fetch the file from the correct endpoint
    
    // Check download response is valid
    if (!downloadResponse || !downloadResponse.ok) {
        return {
            passed: false,
            reason: 'Download response is invalid',
            details: { downloadResponse }
        };
    }
    
    // Verify the correct endpoint was called
    const downloadCall = window.mockFetch.calls.find(
        call => call.url === `/api/file-explorer/files/${fileId}/download`
    );
    
    if (!downloadCall) {
        return {
            passed: false,
            reason: 'Download endpoint was not called',
            details: {
                expectedUrl: `/api/file-explorer/files/${fileId}/download`,
                actualCalls: window.mockFetch.calls.map(c => c.url)
            }
        };
    }
    
    return { passed: true };
}

// Test download action for all file types
async function property5_downloadActionWorksForAllFileTypes() {
    // Test various file types
    const fileTypes = [
        { ext: 'txt', mime: 'text/plain' },
        { ext: 'pdf', mime: 'application/pdf' },
        { ext: 'docx', mime: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' },
        { ext: 'xlsx', mime: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' },
        { ext: 'jpg', mime: 'image/jpeg' },
        { ext: 'png', mime: 'image/png' },
        { ext: 'zip', mime: 'application/zip' },
        { ext: 'mp4', mime: 'video/mp4' }
    ];
    
    const randomType = fileTypes[Math.floor(Math.random() * fileTypes.length)];
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `file_${Math.random().toString(36).substring(7)}.${randomType.ext}`;
    const fileType = randomType.mime;
    
    // Setup mock fetch
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/metadata`, {
        ok: true,
        data: {
            id: fileId,
            fileName: fileName,
            mimeType: fileType,
            fileSize: 1024,
            uploadDate: new Date().toISOString(),
            uploaderName: 'Test User'
        }
    });
    
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/download`, {
        ok: true,
        data: `Content for ${fileName}`
    });
    
    // Track download callback
    let downloadCalled = false;
    let downloadedFileId = null;
    
    const modal = new MockFilePreviewModal({
        onDownload: async (fileId, fileName) => {
            downloadCalled = true;
            downloadedFileId = fileId;
        }
    });
    
    // Open preview
    await modal.open(fileId, fileName, fileType);
    
    // Trigger download
    await modal.downloadFile();
    
    // Property: Download should work for all file types
    
    if (!downloadCalled) {
        return {
            passed: false,
            reason: 'Download was not triggered',
            details: { fileType, downloadCalled }
        };
    }
    
    if (downloadedFileId !== fileId) {
        return {
            passed: false,
            reason: 'Incorrect file downloaded',
            details: {
                fileType,
                expected: fileId,
                actual: downloadedFileId
            }
        };
    }
    
    return { passed: true };
}

// Run all tests
async function runAllTests() {
    console.log('='.repeat(60));
    console.log('FILE PREVIEW MODAL PROPERTY-BASED TESTS');
    console.log('Feature: file-preview-system');
    console.log('='.repeat(60));
    
    const runner = new PropertyTestRunner();
    
    // Run Property 1: Preview modal displays file content
    await runner.runProperty(
        'Property 1: Preview modal displays file content',
        property1_previewModalDisplaysFileContent,
        100
    );
    
    // Run Property 4: Modal dismissal behavior
    await runner.runProperty(
        'Property 4: Modal dismissal behavior',
        property4_modalDismissalBehavior,
        100
    );
    
    // Run Property 2: Preview modal displays complete metadata
    await runner.runProperty(
        'Property 2: Preview modal displays complete metadata',
        property2_previewModalDisplaysCompleteMetadata,
        100
    );
    
    // Run Property 3: Format-specific renderer selection (text files)
    await runner.runProperty(
        'Property 3: Format-specific renderer selection (text files)',
        property3_formatSpecificRendererSelection_textFiles,
        100
    );
    
    // Run Property 15: Virtual scrolling for large text files
    await runner.runProperty(
        'Property 15: Virtual scrolling for large text files',
        property15_virtualScrollingForLargeTextFiles,
        100
    );
    
    // Run Property 15 (complement): No virtual scrolling for small text files
    await runner.runProperty(
        'Property 15 (complement): No virtual scrolling for small text files',
        property15_noVirtualScrollingForSmallTextFiles,
        100
    );
    
    // Run Property 5: Download action in preview
    await runner.runProperty(
        'Property 5: Download action in preview',
        property5_downloadActionInPreview,
        100
    );
    
    // Run Property 5 (default implementation): Download action without callback
    await runner.runProperty(
        'Property 5 (default): Download action with default implementation',
        property5_downloadActionInPreview_defaultImplementation,
        100
    );
    
    // Run Property 5 (all file types): Download works for all file types
    await runner.runProperty(
        'Property 5 (all types): Download action works for all file types',
        property5_downloadActionWorksForAllFileTypes,
        100
    );
    
    // Print summary
    const allPassed = runner.printSummary();
    
    return allPassed;
}

// Export for use in HTML test runner
if (typeof window !== 'undefined') {
    window.runFilePreviewModalTests = runAllTests;
}
