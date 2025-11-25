/**
 * Office Document Renderer
 * Renders Microsoft Office documents (.doc, .docx, .xls, .xlsx, .ppt, .pptx)
 * by requesting converted content from backend (HTML or PDF format)
 * 
 * Feature: file-preview-system
 * Requirements: 4.3, 10.2
 */

/**
 * OfficeRenderer class
 * Handles rendering of Office documents with conversion fallback
 * 
 * @example
 * const renderer = new OfficeRenderer();
 * await renderer.render(fileId, container);
 */
export class OfficeRenderer {
    /**
     * Create a new OfficeRenderer instance
     * @param {Object} options - Configuration options
     * @param {string} options.preferredFormat - Preferred conversion format ('html' or 'pdf', default: 'html')
     */
    constructor(options = {}) {
        this.options = {
            preferredFormat: options.preferredFormat || 'html'
        };
        
        this.currentFileId = null;
        this.currentFormat = null;
        this.conversionUrl = null;
    }

    /**
     * Render Office document content
     * @param {number} fileId - File ID
     * @param {HTMLElement} container - Container element to render into
     * @returns {Promise<void>}
     */
    async render(fileId, container) {
        if (!container) {
            throw new Error('Container element is required');
        }

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
                this.renderConversionError(container, error);
            } else {
                // Re-throw other errors to be handled by preview modal
                throw error;
            }
        }
    }

    /**
     * Fetch converted content from backend API
     * @private
     * @param {number} fileId - File ID
     * @returns {Promise<Object>} Converted content with format information
     */
    async fetchConvertedContent(fileId) {
        try {
            // Use the new office-preview endpoint that converts Office documents to HTML
            const response = await fetch(`/api/file-explorer/files/${fileId}/office-preview`);
            
            if (!response.ok) {
                const error = new Error();
                error.status = response.status;
                
                // Try to parse error message from response
                try {
                    const jsonResponse = await response.json();
                    error.message = jsonResponse.message || `HTTP ${response.status}`;
                } catch (e) {
                    // If not JSON, try text
                    try {
                        const errorText = await response.text();
                        error.message = errorText || `HTTP ${response.status}`;
                    } catch (e2) {
                        error.message = `HTTP ${response.status}`;
                    }
                }
                
                if (response.status === 404) {
                    error.message = 'File not found - it may have been deleted';
                } else if (response.status === 403) {
                    error.message = 'You don\'t have permission to view this file';
                } else if (response.status === 400) {
                    error.message = 'File is not an Office document';
                } else if (response.status === 500) {
                    // Check if this is a conversion error
                    if (error.message.includes('conversion') || error.message.includes('convert') || error.message.includes('corrupted')) {
                        error.message = 'Unable to convert document for preview. The file may be corrupted or in an unsupported format.';
                    } else {
                        error.message = 'Service unavailable - please try again later';
                    }
                }
                
                throw error;
            }
            
            // Get content type to determine format
            const contentType = response.headers.get('content-type');
            
            // Backend converts Office documents to HTML
            const blob = await response.blob();
            
            return {
                blob: blob,
                contentType: contentType,
                format: this.detectFormat(contentType)
            };
        } catch (error) {
            // Network errors (fetch failures)
            if (error instanceof TypeError && error.message.includes('fetch')) {
                const networkError = new Error('Network error - unable to connect to server');
                networkError.name = 'TypeError';
                throw networkError;
            }
            throw error;
        }
    }

    /**
     * Detect format from content type
     * @private
     * @param {string} contentType - Content type header
     * @returns {string} Format ('html', 'pdf', or 'binary')
     */
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

    /**
     * Render converted content based on format
     * @private
     * @param {HTMLElement} container - Container element
     * @param {Object} convertedContent - Converted content object
     */
    renderConvertedContent(container, convertedContent) {
        container.innerHTML = '';
        container.className = 'office-renderer-container h-full overflow-auto bg-white dark:bg-gray-900';
        
        const { blob, format } = convertedContent;
        this.currentFormat = format;
        
        if (format === 'html') {
            // Render HTML content
            this.renderHtmlContent(container, blob);
        } else if (format === 'pdf') {
            // Render PDF content
            this.renderPdfContent(container, blob);
        } else {
            // Binary format - show message that conversion is not available
            this.renderConversionNotAvailable(container);
        }
    }

    /**
     * Render HTML converted content
     * @private
     * @param {HTMLElement} container - Container element
     * @param {Blob} blob - HTML content blob
     */
    async renderHtmlContent(container, blob) {
        const htmlText = await blob.text();
        
        // Create iframe to safely render HTML
        const iframe = document.createElement('iframe');
        iframe.className = 'w-full h-full border-0';
        iframe.setAttribute('sandbox', 'allow-same-origin');
        iframe.setAttribute('title', 'Office Document Preview');
        
        container.appendChild(iframe);
        
        // Write HTML content to iframe
        const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
        iframeDoc.open();
        iframeDoc.write(htmlText);
        iframeDoc.close();
        
        // Add styling to iframe content
        const style = iframeDoc.createElement('style');
        style.textContent = `
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                padding: 2rem;
                max-width: 1200px;
                margin: 0 auto;
                background: white;
                color: #1f2937;
            }
            table {
                border-collapse: collapse;
                width: 100%;
                margin: 1rem 0;
            }
            table, th, td {
                border: 1px solid #d1d5db;
            }
            th, td {
                padding: 0.5rem;
                text-align: left;
            }
            th {
                background-color: #f3f4f6;
                font-weight: 600;
            }
        `;
        iframeDoc.head.appendChild(style);
    }

    /**
     * Render PDF converted content
     * @private
     * @param {HTMLElement} container - Container element
     * @param {Blob} blob - PDF content blob
     */
    renderPdfContent(container, blob) {
        // Create object URL for PDF
        const pdfUrl = URL.createObjectURL(blob);
        this.conversionUrl = pdfUrl;
        
        // Create iframe for PDF display
        const iframe = document.createElement('iframe');
        iframe.src = pdfUrl;
        iframe.className = 'w-full h-full border-0';
        iframe.setAttribute('title', 'Office Document Preview (PDF)');
        
        container.appendChild(iframe);
        
        // Add format badge
        const badge = document.createElement('div');
        badge.className = 'absolute top-2 right-2 px-3 py-1 bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300 text-xs rounded-full shadow-sm';
        badge.textContent = 'Converted to PDF';
        container.style.position = 'relative';
        container.appendChild(badge);
    }

    /**
     * Render message when conversion is not available
     * @private
     * @param {HTMLElement} container - Container element
     */
    renderConversionNotAvailable(container) {
        container.innerHTML = '';
        container.className = 'flex items-center justify-center h-full bg-gray-50 dark:bg-gray-900';
        
        const messageDiv = document.createElement('div');
        messageDiv.className = 'text-center p-8 max-w-md';
        
        messageDiv.innerHTML = `
            <div class="mb-4">
                <svg class="w-16 h-16 mx-auto text-gray-400 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                </svg>
            </div>
            <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">
                Preview Not Available
            </h3>
            <p class="text-gray-600 dark:text-gray-400 mb-6">
                Office document preview is currently not available. Please download the file to view its contents.
            </p>
            <button 
                class="office-download-btn px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors inline-flex items-center gap-2"
                aria-label="Download file">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                </svg>
                Download File
            </button>
        `;
        
        container.appendChild(messageDiv);
        
        // Add download button handler
        const downloadBtn = messageDiv.querySelector('.office-download-btn');
        downloadBtn.addEventListener('click', () => {
            this.downloadFile();
        });
    }

    /**
     * Render conversion error with download fallback
     * @private
     * @param {HTMLElement} container - Container element
     * @param {Error} error - Error object
     */
    renderConversionError(container, error) {
        container.innerHTML = '';
        container.className = 'flex items-center justify-center h-full bg-gray-50 dark:bg-gray-900';
        
        const errorDiv = document.createElement('div');
        errorDiv.className = 'text-center p-8 max-w-md';
        
        errorDiv.innerHTML = `
            <div class="mb-4">
                <svg class="w-16 h-16 mx-auto text-red-400 dark:text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                </svg>
            </div>
            <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">
                Conversion Failed
            </h3>
            <p class="text-gray-600 dark:text-gray-400 mb-2">
                Unable to convert document for preview.
            </p>
            <p class="text-sm text-gray-500 dark:text-gray-500 mb-6">
                ${this.escapeHtml(error.message)}
            </p>
            <button 
                class="office-download-btn px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors inline-flex items-center gap-2"
                aria-label="Download file">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                </svg>
                Download File Instead
            </button>
        `;
        
        container.appendChild(errorDiv);
        
        // Add download button handler
        const downloadBtn = errorDiv.querySelector('.office-download-btn');
        downloadBtn.addEventListener('click', () => {
            this.downloadFile();
        });
    }

    /**
     * Trigger file download
     * @private
     */
    async downloadFile() {
        if (!this.currentFileId) return;
        
        try {
            const response = await fetch(`/api/file-explorer/files/${this.currentFileId}/download`);
            
            if (!response.ok) {
                throw new Error('Failed to download file');
            }
            
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
            
            // Get filename from Content-Disposition header or use default
            const contentDisposition = response.headers.get('content-disposition');
            let filename = 'document';
            
            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                if (filenameMatch && filenameMatch[1]) {
                    filename = filenameMatch[1].replace(/['"]/g, '');
                }
            }
            
            // Create temporary link and trigger download
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            
            // Clean up
            URL.revokeObjectURL(url);
            
        } catch (error) {
            console.error('Error downloading file:', error);
            alert('Failed to download file. Please try again.');
        }
    }

    /**
     * Escape HTML to prevent XSS
     * @private
     * @param {string} text - Text to escape
     * @returns {string} Escaped text
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Check if file format is supported for Office rendering
     * @param {string} mimeType - MIME type of the file
     * @returns {boolean} True if supported
     */
    static supportsFormat(mimeType) {
        if (!mimeType) return false;
        
        const supportedTypes = [
            'application/msword', // .doc
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document', // .docx
            'application/vnd.ms-excel', // .xls
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', // .xlsx
            'application/vnd.ms-powerpoint', // .ppt
            'application/vnd.openxmlformats-officedocument.presentationml.presentation' // .pptx
        ];
        
        return supportedTypes.some(type => mimeType.includes(type));
    }

    /**
     * Get current format
     * @returns {string|null} Current format
     */
    getCurrentFormat() {
        return this.currentFormat;
    }

    /**
     * Clean up resources
     */
    destroy() {
        if (this.conversionUrl) {
            URL.revokeObjectURL(this.conversionUrl);
            this.conversionUrl = null;
        }
        
        this.currentFileId = null;
        this.currentFormat = null;
    }
}
