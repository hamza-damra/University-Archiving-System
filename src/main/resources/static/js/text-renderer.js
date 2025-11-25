/**
 * Text File Renderer
 * Renders plain text files with preserved formatting and virtual scrolling
 * 
 * Feature: file-preview-system
 * Requirements: 4.1, 7.2
 */

/**
 * TextRenderer class
 * Handles rendering of plain text files (.txt, .md, .log, .csv)
 * 
 * @example
 * const renderer = new TextRenderer();
 * await renderer.render(fileId, container);
 */
export class TextRenderer {
    /**
     * Create a new TextRenderer instance
     * @param {Object} options - Configuration options
     * @param {number} options.virtualScrollThreshold - Line count threshold for virtual scrolling (default: 1000)
     * @param {number} options.chunkSize - Number of lines to render at once for virtual scrolling (default: 100)
     */
    constructor(options = {}) {
        this.options = {
            virtualScrollThreshold: options.virtualScrollThreshold || 1000,
            chunkSize: options.chunkSize || 100
        };
        
        this.currentContent = null;
        this.currentLines = null;
        this.searchMatches = [];
        this.currentMatchIndex = -1;
    }

    /**
     * Render text file content
     * @param {number} fileId - File ID
     * @param {HTMLElement} container - Container element to render into
     * @param {Object} options - Rendering options
     * @param {boolean} options.partial - Load only first N lines for large files
     * @returns {Promise<void>}
     */
    async render(fileId, container, options = {}) {
        if (!container) {
            throw new Error('Container element is required');
        }

        try {
            // Fetch file content from backend
            const content = await this.fetchContent(fileId, options.partial);
            
            this.currentContent = content;
            this.currentLines = content.split('\n');
            
            // Check if virtual scrolling is needed
            const useVirtualScrolling = this.currentLines.length > this.options.virtualScrollThreshold;
            
            if (useVirtualScrolling) {
                this.renderWithVirtualScrolling(container, options.partial);
            } else {
                this.renderStandard(container, options.partial);
            }
        } catch (error) {
            console.error('Error rendering text file:', error);
            throw error;
        }
    }

    /**
     * Render partial preview (first N lines)
     * @param {number} fileId - File ID
     * @param {HTMLElement} container - Container element to render into
     * @returns {Promise<void>}
     */
    async renderPartial(fileId, container) {
        return this.render(fileId, container, { partial: true });
    }

    /**
     * Fetch file content from backend API
     * @private
     * @param {number} fileId - File ID
     * @param {boolean} partial - Fetch only partial content (first 500 lines)
     * @returns {Promise<string>} File content
     */
    async fetchContent(fileId, partial = false) {
        try {
            // Add partial parameter to URL if requested
            const url = partial 
                ? `/api/file-explorer/files/${fileId}/content?partial=true&lines=500`
                : `/api/file-explorer/files/${fileId}/content`;
            
            const response = await fetch(url);
            
            if (!response.ok) {
                const error = new Error();
                error.status = response.status;
                
                // Try to parse error message from response
                try {
                    const jsonResponse = await response.json();
                    error.message = jsonResponse.message || `HTTP ${response.status}`;
                } catch (e) {
                    error.message = `HTTP ${response.status}`;
                }
                
                if (response.status === 404) {
                    error.message = 'File not found - it may have been deleted';
                } else if (response.status === 403) {
                    error.message = 'You don\'t have permission to view this file';
                } else if (response.status === 500) {
                    error.message = 'Service unavailable - please try again later';
                }
                
                throw error;
            }
            
            // Parse JSON response (API returns wrapped response)
            const jsonResponse = await response.json();
            
            // Extract content from ApiResponse wrapper
            if (jsonResponse.success && jsonResponse.data) {
                return jsonResponse.data;
            } else {
                const error = new Error(jsonResponse.message || 'Failed to load file content');
                error.status = response.status;
                throw error;
            }
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
     * Render content with standard scrolling
     * @private
     * @param {HTMLElement} container - Container element
     * @param {boolean} isPartial - Whether this is a partial preview
     */
    renderStandard(container, isPartial = false) {
        container.innerHTML = '';
        
        // Create pre element for text content
        const pre = document.createElement('pre');
        pre.className = 'text-sm font-mono text-gray-900 dark:text-gray-100 whitespace-pre-wrap break-words m-0 p-4';
        pre.textContent = this.currentContent;
        
        container.appendChild(pre);
        
        // Add partial preview notice if applicable
        if (isPartial) {
            const notice = document.createElement('div');
            notice.className = 'absolute bottom-4 left-1/2 transform -translate-x-1/2 px-4 py-2 bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200 text-sm rounded-lg shadow-lg';
            notice.innerHTML = `
                <div class="flex items-center gap-2">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                    <span>Showing first 500 lines only. Download file to view complete content.</span>
                </div>
            `;
            container.style.position = 'relative';
            container.appendChild(notice);
        }
    }

    /**
     * Render content with virtual scrolling for large files
     * @private
     * @param {HTMLElement} container - Container element
     * @param {boolean} isPartial - Whether this is a partial preview
     */
    renderWithVirtualScrolling(container, isPartial = false) {
        container.innerHTML = '';
        
        // Create virtual scroll container
        const scrollContainer = document.createElement('div');
        scrollContainer.className = 'relative overflow-auto h-full';
        
        // Create spacer to maintain scroll height
        const spacer = document.createElement('div');
        const lineHeight = 20; // Approximate line height in pixels
        spacer.style.height = `${this.currentLines.length * lineHeight}px`;
        scrollContainer.appendChild(spacer);
        
        // Create visible content container
        const contentContainer = document.createElement('pre');
        contentContainer.className = 'absolute top-0 left-0 right-0 text-sm font-mono text-gray-900 dark:text-gray-100 whitespace-pre-wrap break-words m-0 p-4';
        scrollContainer.appendChild(contentContainer);
        
        // Initial render
        this.updateVirtualScrollContent(contentContainer, 0);
        
        // Add scroll listener for virtual scrolling
        let scrollTimeout;
        scrollContainer.addEventListener('scroll', () => {
            clearTimeout(scrollTimeout);
            scrollTimeout = setTimeout(() => {
                const scrollTop = scrollContainer.scrollTop;
                const startLine = Math.floor(scrollTop / lineHeight);
                this.updateVirtualScrollContent(contentContainer, startLine);
            }, 50);
        });
        
        container.appendChild(scrollContainer);
        
        // Add info message about virtual scrolling
        const info = document.createElement('div');
        info.className = 'absolute top-2 right-2 px-3 py-1 bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 text-xs rounded-full shadow-sm';
        const partialText = isPartial ? ' (partial preview - first 500 lines)' : '';
        info.textContent = `Large file: ${this.currentLines.length.toLocaleString()} lines (virtual scrolling enabled)${partialText}`;
        container.style.position = 'relative';
        container.appendChild(info);
        
        // Add partial preview notice if applicable
        if (isPartial) {
            const notice = document.createElement('div');
            notice.className = 'absolute bottom-4 left-1/2 transform -translate-x-1/2 px-4 py-2 bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200 text-sm rounded-lg shadow-lg z-10';
            notice.innerHTML = `
                <div class="flex items-center gap-2">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                    <span>Showing first 500 lines only. Download file to view complete content.</span>
                </div>
            `;
            container.appendChild(notice);
        }
    }

    /**
     * Update visible content for virtual scrolling
     * @private
     * @param {HTMLElement} contentContainer - Content container element
     * @param {number} startLine - Starting line number
     */
    updateVirtualScrollContent(contentContainer, startLine) {
        const endLine = Math.min(
            startLine + this.options.chunkSize * 3, // Render 3 chunks for smooth scrolling
            this.currentLines.length
        );
        
        const visibleLines = this.currentLines.slice(startLine, endLine);
        contentContainer.textContent = visibleLines.join('\n');
        
        // Update position
        const lineHeight = 20;
        contentContainer.style.top = `${startLine * lineHeight}px`;
    }

    /**
     * Search for text within the content
     * @param {string} query - Search query
     * @returns {number} Number of matches found
     */
    search(query) {
        if (!this.currentContent || !query) {
            this.searchMatches = [];
            this.currentMatchIndex = -1;
            return 0;
        }

        // Find all matches
        this.searchMatches = [];
        const lowerContent = this.currentContent.toLowerCase();
        const lowerQuery = query.toLowerCase();
        
        let index = 0;
        while ((index = lowerContent.indexOf(lowerQuery, index)) !== -1) {
            this.searchMatches.push({
                start: index,
                end: index + query.length,
                line: this.getLineNumber(index)
            });
            index += query.length;
        }
        
        this.currentMatchIndex = this.searchMatches.length > 0 ? 0 : -1;
        
        return this.searchMatches.length;
    }

    /**
     * Navigate to next search match
     * @returns {Object|null} Match information or null if no matches
     */
    nextMatch() {
        if (this.searchMatches.length === 0) return null;
        
        this.currentMatchIndex = (this.currentMatchIndex + 1) % this.searchMatches.length;
        return this.searchMatches[this.currentMatchIndex];
    }

    /**
     * Navigate to previous search match
     * @returns {Object|null} Match information or null if no matches
     */
    previousMatch() {
        if (this.searchMatches.length === 0) return null;
        
        this.currentMatchIndex = this.currentMatchIndex - 1;
        if (this.currentMatchIndex < 0) {
            this.currentMatchIndex = this.searchMatches.length - 1;
        }
        
        return this.searchMatches[this.currentMatchIndex];
    }

    /**
     * Get line number for a character index
     * @private
     * @param {number} charIndex - Character index in content
     * @returns {number} Line number (0-based)
     */
    getLineNumber(charIndex) {
        if (!this.currentContent) return 0;
        
        const beforeMatch = this.currentContent.substring(0, charIndex);
        return beforeMatch.split('\n').length - 1;
    }

    /**
     * Get current match information
     * @returns {Object|null} Current match or null
     */
    getCurrentMatch() {
        if (this.currentMatchIndex < 0 || this.currentMatchIndex >= this.searchMatches.length) {
            return null;
        }
        
        return {
            ...this.searchMatches[this.currentMatchIndex],
            index: this.currentMatchIndex,
            total: this.searchMatches.length
        };
    }

    /**
     * Clear search results
     */
    clearSearch() {
        this.searchMatches = [];
        this.currentMatchIndex = -1;
    }
}
