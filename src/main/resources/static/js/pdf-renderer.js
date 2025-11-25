/**
 * PDF File Renderer
 * Renders PDF files using browser's native PDF viewer with navigation controls
 * 
 * Feature: file-preview-system
 * Requirements: 4.2, 7.1, 7.3
 */

/**
 * PDFRenderer class
 * Handles rendering of PDF files with page navigation
 * 
 * @example
 * const renderer = new PDFRenderer();
 * await renderer.render(fileId, container);
 */
export class PDFRenderer {
    /**
     * Create a new PDFRenderer instance
     * @param {Object} options - Configuration options
     */
    constructor(options = {}) {
        this.options = options;
        this.currentFileId = null;
        this.currentPage = 1;
        this.totalPages = 0;
        this.pdfUrl = null;
        this.iframeElement = null;
    }

    /**
     * Render PDF file content
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
            // Fetch PDF file as blob
            const blob = await this.fetchPdfBlob(fileId);
            
            // Create object URL for the PDF
            this.pdfUrl = URL.createObjectURL(blob);
            
            // Render PDF viewer UI
            this.renderPdfViewer(container);
            
        } catch (error) {
            console.error('Error rendering PDF file:', error);
            throw error;
        }
    }

    /**
     * Fetch PDF file as blob from backend API
     * @private
     * @param {number} fileId - File ID
     * @returns {Promise<Blob>} PDF file blob
     */
    async fetchPdfBlob(fileId) {
        try {
            const response = await fetch(`/api/file-explorer/files/${fileId}/download`);
            
            if (!response.ok) {
                const error = new Error();
                error.status = response.status;
                
                if (response.status === 404) {
                    error.message = 'File not found - it may have been deleted';
                } else if (response.status === 403) {
                    error.message = 'You don\'t have permission to view this file';
                } else if (response.status === 500) {
                    error.message = 'Service unavailable - please try again later';
                } else {
                    error.message = 'Failed to load PDF file';
                }
                
                throw error;
            }
            
            const blob = await response.blob();
            
            // Validate that we received a PDF
            if (blob.type && !blob.type.includes('pdf')) {
                const error = new Error('File is corrupted or not a valid PDF');
                error.status = 400;
                throw error;
            }
            
            return blob;
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
     * Render PDF viewer with iframe and navigation controls
     * @private
     * @param {HTMLElement} container - Container element
     */
    renderPdfViewer(container) {
        container.innerHTML = '';
        container.className = 'flex flex-col h-full';
        
        // Create navigation controls
        const controls = this.createNavigationControls();
        container.appendChild(controls);
        
        // Create iframe for PDF display
        const iframe = document.createElement('iframe');
        iframe.src = this.pdfUrl;
        iframe.className = 'flex-1 w-full border-0 bg-gray-100 dark:bg-gray-800';
        iframe.style.minHeight = '500px';
        iframe.setAttribute('title', 'PDF Preview');
        
        this.iframeElement = iframe;
        container.appendChild(iframe);
        
        // Try to detect page count (this is browser-dependent and may not work)
        this.detectPageCount();
    }

    /**
     * Create navigation controls for PDF
     * @private
     * @returns {HTMLElement} Controls container
     */
    createNavigationControls() {
        const controls = document.createElement('div');
        controls.className = 'flex items-center justify-between px-4 py-3 bg-gray-100 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700';
        
        controls.innerHTML = `
            <div class="flex items-center gap-2">
                <button 
                    class="pdf-prev-btn px-3 py-1.5 text-sm bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    title="Previous page"
                    aria-label="Previous page">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                    </svg>
                </button>
                
                <button 
                    class="pdf-next-btn px-3 py-1.5 text-sm bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    title="Next page"
                    aria-label="Next page">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                    </svg>
                </button>
                
                <div class="flex items-center gap-2 ml-2">
                    <span class="text-sm text-gray-600 dark:text-gray-400">Page</span>
                    <input 
                        type="number" 
                        class="pdf-page-input w-16 px-2 py-1 text-sm text-center border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                        value="1"
                        min="1"
                        aria-label="Current page number">
                    <span class="text-sm text-gray-600 dark:text-gray-400">
                        of <span class="pdf-total-pages">--</span>
                    </span>
                </div>
            </div>
            
            <div class="text-xs text-gray-500 dark:text-gray-400">
                Use browser's built-in PDF controls for zoom and additional features
            </div>
        `;
        
        // Add event listeners
        const prevBtn = controls.querySelector('.pdf-prev-btn');
        const nextBtn = controls.querySelector('.pdf-next-btn');
        const pageInput = controls.querySelector('.pdf-page-input');
        
        prevBtn.addEventListener('click', () => this.previousPage());
        nextBtn.addEventListener('click', () => this.nextPage());
        pageInput.addEventListener('change', (e) => this.goToPage(parseInt(e.target.value)));
        pageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.goToPage(parseInt(e.target.value));
            }
        });
        
        return controls;
    }

    /**
     * Navigate to previous page
     */
    previousPage() {
        if (this.currentPage > 1) {
            this.goToPage(this.currentPage - 1);
        }
    }

    /**
     * Navigate to next page
     */
    nextPage() {
        if (this.totalPages === 0 || this.currentPage < this.totalPages) {
            this.goToPage(this.currentPage + 1);
        }
    }

    /**
     * Navigate to specific page
     * @param {number} pageNumber - Page number to navigate to
     */
    goToPage(pageNumber) {
        if (pageNumber < 1) {
            pageNumber = 1;
        }
        
        if (this.totalPages > 0 && pageNumber > this.totalPages) {
            pageNumber = this.totalPages;
        }
        
        this.currentPage = pageNumber;
        
        // Update iframe src with page parameter
        // Note: This uses the PDF Open Parameters standard
        if (this.pdfUrl && this.iframeElement) {
            const urlWithPage = `${this.pdfUrl}#page=${pageNumber}`;
            this.iframeElement.src = urlWithPage;
        }
        
        // Update UI
        this.updateNavigationUI();
    }

    /**
     * Update navigation UI elements
     * @private
     */
    updateNavigationUI() {
        if (!this.iframeElement || !this.iframeElement.parentElement) return;
        
        const container = this.iframeElement.parentElement;
        const pageInput = container.querySelector('.pdf-page-input');
        const prevBtn = container.querySelector('.pdf-prev-btn');
        const nextBtn = container.querySelector('.pdf-next-btn');
        
        if (pageInput) {
            pageInput.value = this.currentPage;
        }
        
        if (prevBtn) {
            prevBtn.disabled = this.currentPage <= 1;
        }
        
        if (nextBtn) {
            nextBtn.disabled = this.totalPages > 0 && this.currentPage >= this.totalPages;
        }
    }

    /**
     * Attempt to detect total page count
     * Note: This is limited by browser security and may not always work
     * @private
     */
    async detectPageCount() {
        // For now, we'll set a placeholder
        // In a full implementation with PDF.js library, we could get accurate page count
        // The browser's native viewer doesn't expose this information via JavaScript
        
        // Try to use PDF.js if available
        if (typeof pdfjsLib !== 'undefined') {
            try {
                const pdf = await pdfjsLib.getDocument(this.pdfUrl).promise;
                this.totalPages = pdf.numPages;
                this.updatePageCountDisplay();
            } catch (error) {
                console.warn('Could not load PDF with PDF.js:', error);
                // Fall back to unknown page count
                this.setUnknownPageCount();
            }
        } else {
            // PDF.js not available, use browser native viewer
            this.setUnknownPageCount();
        }
    }

    /**
     * Set unknown page count in UI
     * @private
     */
    setUnknownPageCount() {
        this.totalPages = 0;
        this.updatePageCountDisplay();
    }

    /**
     * Update page count display in UI
     * @private
     */
    updatePageCountDisplay() {
        if (!this.iframeElement || !this.iframeElement.parentElement) return;
        
        const container = this.iframeElement.parentElement;
        const totalPagesSpan = container.querySelector('.pdf-total-pages');
        const pageInput = container.querySelector('.pdf-page-input');
        
        if (totalPagesSpan) {
            totalPagesSpan.textContent = this.totalPages > 0 ? this.totalPages : '--';
        }
        
        if (pageInput && this.totalPages > 0) {
            pageInput.max = this.totalPages;
        }
    }

    /**
     * Get current page number
     * @returns {number} Current page number
     */
    getCurrentPage() {
        return this.currentPage;
    }

    /**
     * Get total page count
     * @returns {number} Total page count (0 if unknown)
     */
    getPageCount() {
        return this.totalPages;
    }

    /**
     * Clean up resources
     */
    destroy() {
        if (this.pdfUrl) {
            URL.revokeObjectURL(this.pdfUrl);
            this.pdfUrl = null;
        }
        
        this.iframeElement = null;
        this.currentFileId = null;
        this.currentPage = 1;
        this.totalPages = 0;
    }
}
