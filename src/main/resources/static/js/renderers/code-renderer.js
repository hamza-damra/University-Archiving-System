/**
 * Code File Renderer
 * Renders code files with syntax highlighting using Highlight.js
 * 
 * Feature: file-preview-system
 * Requirements: 4.4
 */

/**
 * CodeRenderer class
 * Handles rendering of code files (.java, .js, .py, .css, .html, .sql, .xml, .json)
 * with syntax highlighting and line numbers
 * 
 * @example
 * const renderer = new CodeRenderer();
 * await renderer.render(fileId, container, 'javascript');
 */
export class CodeRenderer {
    /**
     * Create a new CodeRenderer instance
     * @param {Object} options - Configuration options
     * @param {string} options.theme - Highlight.js theme name (default: 'github')
     * @param {boolean} options.showLineNumbers - Show line numbers (default: true)
     */
    constructor(options = {}) {
        this.options = {
            theme: options.theme || 'github',
            showLineNumbers: options.showLineNumbers !== false
        };
        
        this.currentContent = null;
        this.currentLanguage = null;
        this.highlightJsLoaded = false;
    }

    /**
     * Render code file content with syntax highlighting
     * @param {number} fileId - File ID
     * @param {HTMLElement} container - Container element to render into
     * @param {string} language - Programming language (optional, will be detected from file extension)
     * @returns {Promise<void>}
     */
    async render(fileId, container, language = null) {
        if (!container) {
            throw new Error('Container element is required');
        }

        try {
            // Fetch file content from backend
            const content = await this.fetchContent(fileId);
            
            this.currentContent = content;
            this.currentLanguage = language;
            
            // Ensure Highlight.js is loaded
            await this.ensureHighlightJsLoaded();
            
            // Render code with syntax highlighting
            this.renderCode(container);
            
        } catch (error) {
            console.error('Error rendering code file:', error);
            throw error;
        }
    }

    /**
     * Fetch file content from backend API
     * @private
     * @param {number} fileId - File ID
     * @returns {Promise<string>} File content
     */
    async fetchContent(fileId) {
        try {
            const token = localStorage.getItem('token') || sessionStorage.getItem('token');
            const headers = {};
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }
            
            const response = await fetch(`/api/file-explorer/files/${fileId}/content`, {
                method: 'GET',
                headers: headers
            });
            
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
     * Ensure Highlight.js library is loaded
     * @private
     * @returns {Promise<void>}
     */
    async ensureHighlightJsLoaded() {
        // Check if Highlight.js is already loaded
        if (typeof hljs !== 'undefined') {
            this.highlightJsLoaded = true;
            return;
        }

        // Load Highlight.js from CDN
        return new Promise((resolve, reject) => {
            // Load CSS
            const cssLink = document.createElement('link');
            cssLink.rel = 'stylesheet';
            cssLink.href = `https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/${this.options.theme}.min.css`;
            document.head.appendChild(cssLink);

            // Load JS
            const script = document.createElement('script');
            script.src = 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js';
            script.onload = () => {
                this.highlightJsLoaded = true;
                resolve();
            };
            script.onerror = () => {
                reject(new Error('Failed to load Highlight.js library'));
            };
            document.head.appendChild(script);
        });
    }

    /**
     * Render code with syntax highlighting
     * @private
     * @param {HTMLElement} container - Container element
     */
    renderCode(container) {
        container.innerHTML = '';
        container.className = 'code-renderer-container h-full overflow-auto bg-gray-50 dark:bg-gray-900';
        
        // Create wrapper for code
        const wrapper = document.createElement('div');
        wrapper.className = 'relative';
        
        // Create pre and code elements
        const pre = document.createElement('pre');
        pre.className = 'text-sm m-0 p-0';
        
        const code = document.createElement('code');
        
        // Set language class if specified
        if (this.currentLanguage) {
            code.className = `language-${this.currentLanguage}`;
        }
        
        code.textContent = this.currentContent;
        
        // Apply syntax highlighting if Highlight.js is loaded
        if (this.highlightJsLoaded && typeof hljs !== 'undefined') {
            try {
                if (this.currentLanguage) {
                    // Highlight with specified language
                    const highlighted = hljs.highlight(this.currentContent, { 
                        language: this.currentLanguage,
                        ignoreIllegals: true 
                    });
                    code.innerHTML = highlighted.value;
                } else {
                    // Auto-detect language
                    const highlighted = hljs.highlightAuto(this.currentContent);
                    code.innerHTML = highlighted.value;
                    this.currentLanguage = highlighted.language;
                }
            } catch (error) {
                console.warn('Syntax highlighting failed, showing plain text:', error);
                // Fall back to plain text
                code.textContent = this.currentContent;
            }
        }
        
        pre.appendChild(code);
        
        // Add line numbers if enabled
        if (this.options.showLineNumbers) {
            const lineNumbersContainer = this.createLineNumbers();
            const codeContainer = document.createElement('div');
            codeContainer.className = 'flex';
            codeContainer.appendChild(lineNumbersContainer);
            codeContainer.appendChild(pre);
            wrapper.appendChild(codeContainer);
        } else {
            pre.className += ' p-4';
            wrapper.appendChild(pre);
        }
        
        // Add language badge
        if (this.currentLanguage) {
            const badge = document.createElement('div');
            badge.className = 'absolute top-2 right-2 px-3 py-1 bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 text-xs rounded-full shadow-sm';
            badge.textContent = this.currentLanguage.toUpperCase();
            wrapper.appendChild(badge);
        }
        
        container.appendChild(wrapper);
    }

    /**
     * Create line numbers element
     * @private
     * @returns {HTMLElement} Line numbers container
     */
    createLineNumbers() {
        const lines = this.currentContent.split('\n');
        const lineCount = lines.length;
        
        const lineNumbersDiv = document.createElement('div');
        lineNumbersDiv.className = 'line-numbers bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400 text-sm font-mono text-right select-none border-r border-gray-300 dark:border-gray-700 py-4 px-3 sticky left-0';
        
        const lineNumbers = [];
        for (let i = 1; i <= lineCount; i++) {
            lineNumbers.push(`<div class="leading-6">${i}</div>`);
        }
        
        lineNumbersDiv.innerHTML = lineNumbers.join('');
        
        return lineNumbersDiv;
    }

    /**
     * Detect programming language from file extension
     * @param {string} fileName - File name with extension
     * @returns {string|null} Detected language or null
     */
    static detectLanguage(fileName) {
        if (!fileName) return null;
        
        const extension = fileName.split('.').pop().toLowerCase();
        
        const languageMap = {
            'js': 'javascript',
            'jsx': 'javascript',
            'ts': 'typescript',
            'tsx': 'typescript',
            'java': 'java',
            'py': 'python',
            'rb': 'ruby',
            'php': 'php',
            'c': 'c',
            'cpp': 'cpp',
            'cc': 'cpp',
            'cxx': 'cpp',
            'h': 'c',
            'hpp': 'cpp',
            'cs': 'csharp',
            'go': 'go',
            'rs': 'rust',
            'swift': 'swift',
            'kt': 'kotlin',
            'scala': 'scala',
            'css': 'css',
            'scss': 'scss',
            'sass': 'sass',
            'less': 'less',
            'html': 'html',
            'xml': 'xml',
            'json': 'json',
            'yaml': 'yaml',
            'yml': 'yaml',
            'sql': 'sql',
            'sh': 'bash',
            'bash': 'bash',
            'zsh': 'bash',
            'ps1': 'powershell',
            'r': 'r',
            'matlab': 'matlab',
            'm': 'matlab',
            'md': 'markdown',
            'markdown': 'markdown'
        };
        
        return languageMap[extension] || null;
    }

    /**
     * Get current language
     * @returns {string|null} Current language
     */
    getCurrentLanguage() {
        return this.currentLanguage;
    }

    /**
     * Get current content
     * @returns {string|null} Current content
     */
    getCurrentContent() {
        return this.currentContent;
    }
}
