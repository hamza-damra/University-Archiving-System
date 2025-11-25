/**
 * File Preview Button Component
 * Renders preview buttons for supported file types in the file explorer
 * 
 * Feature: file-preview-system
 * Task: 8. Implement preview button component in file explorer
 * Requirements: 5.1, 5.2, 5.3, 5.4
 */

import { FilePreviewModal } from './file-preview-modal.js';

/**
 * FilePreviewButton class
 * Manages preview button rendering and file type detection
 * 
 * Supported file types:
 * - Text files: .txt, .md, .log, .csv
 * - PDF files: .pdf
 * - Code files: .java, .js, .py, .css, .html, .sql, .xml, .json
 * - Office documents: .doc, .docx, .xls, .xlsx, .ppt, .pptx
 * 
 * @example
 * // Check if file is previewable
 * const canPreview = FilePreviewButton.isPreviewable('application/pdf');
 * 
 * // Render preview button
 * const buttonHtml = FilePreviewButton.renderButton(file);
 */
export class FilePreviewButton {
    /**
     * Supported MIME types for preview
     * Based on design document requirements
     */
    static SUPPORTED_TYPES = {
        // Text files (Requirement 4.1)
        'text/plain': true,
        'text/markdown': true,
        'text/csv': true,
        'text/log': true,
        
        // PDF files (Requirement 4.2)
        'application/pdf': true,
        
        // Office documents (Requirement 4.3)
        'application/msword': true,
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document': true,
        'application/vnd.ms-excel': true,
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': true,
        'application/vnd.ms-powerpoint': true,
        'application/vnd.openxmlformats-officedocument.presentationml.presentation': true,
        
        // Code files (Requirement 4.4)
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

    /**
     * Supported file extensions for preview
     * Used as fallback when MIME type is not available
     */
    static SUPPORTED_EXTENSIONS = {
        // Text files
        'txt': true,
        'md': true,
        'log': true,
        'csv': true,
        
        // PDF files
        'pdf': true,
        
        // Office documents
        'doc': true,
        'docx': true,
        'xls': true,
        'xlsx': true,
        'ppt': true,
        'pptx': true,
        
        // Code files
        'java': true,
        'js': true,
        'py': true,
        'css': true,
        'html': true,
        'sql': true,
        'xml': true,
        'json': true
    };

    /**
     * Check if a file type is previewable
     * 
     * @param {string} fileType - MIME type or file extension
     * @param {string} fileName - Optional file name for extension detection
     * @returns {boolean} True if file can be previewed
     * 
     * @example
     * FilePreviewButton.isPreviewable('application/pdf'); // true
     * FilePreviewButton.isPreviewable('image/png'); // false
     * FilePreviewButton.isPreviewable('', 'document.pdf'); // true (fallback to extension)
     */
    static isPreviewable(fileType, fileName = '') {
        // Check MIME type first
        if (fileType && this.SUPPORTED_TYPES[fileType.toLowerCase()]) {
            return true;
        }
        
        // Fallback to file extension
        if (fileName) {
            const extension = this.getFileExtension(fileName);
            if (extension && this.SUPPORTED_EXTENSIONS[extension.toLowerCase()]) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Get file extension from filename
     * 
     * @param {string} fileName - File name
     * @returns {string} File extension without dot
     * 
     * @example
     * FilePreviewButton.getFileExtension('document.pdf'); // 'pdf'
     * FilePreviewButton.getFileExtension('script.min.js'); // 'js'
     */
    static getFileExtension(fileName) {
        if (!fileName) return '';
        const parts = fileName.split('.');
        return parts.length > 1 ? parts[parts.length - 1] : '';
    }

    /**
     * Render preview button HTML for a file
     * 
     * Requirements:
     * - 5.1: Show preview icon/button next to supported files
     * - 5.2: Display tooltip indicating preview is available
     * - 5.3: Display tooltip for non-previewable files
     * - 5.4: Visually distinguish previewable files
     * 
     * @param {Object} file - File object with id, name, and type
     * @param {number} file.id - File ID
     * @param {string} file.originalFilename - File name
     * @param {string} file.fileType - MIME type
     * @returns {string} HTML string for preview button
     * 
     * @example
     * const file = {
     *   id: 123,
     *   originalFilename: 'document.pdf',
     *   fileType: 'application/pdf'
     * };
     * const buttonHtml = FilePreviewButton.renderButton(file);
     */
    static renderButton(file) {
        const fileId = file.id || (file.metadata && file.metadata.fileId);
        const fileName = file.originalFilename || file.name || 'Unknown';
        const fileType = file.fileType || (file.metadata && file.metadata.fileType) || '';
        
        // Check if file is previewable
        const canPreview = this.isPreviewable(fileType, fileName);
        
        if (!canPreview) {
            // Non-previewable file - show disabled state with tooltip
            // Requirement 5.3: Tooltip indicating only download is available
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
        
        // Previewable file - show active preview button
        // Requirements 5.1, 5.2, 5.4: Preview button with tooltip and visual distinction
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

    /**
     * Escape HTML to prevent XSS
     * 
     * @param {string} str - String to escape
     * @returns {string} Escaped string
     */
    static escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    /**
     * Handle preview button click
     * Opens the FilePreviewModal for the selected file
     * 
     * @param {number} fileId - File ID
     * @param {string} fileName - File name
     * @param {string} fileType - MIME type
     * @returns {Promise<void>}
     */
    async handlePreviewClick(fileId, fileName, fileType) {
        try {
            // Create modal instance if not exists
            if (!this.previewModal) {
                this.previewModal = new FilePreviewModal({
                    onDownload: async (id) => {
                        // Delegate to file explorer's download handler
                        if (window.fileExplorerInstance && window.fileExplorerInstance.handleFileDownload) {
                            await window.fileExplorerInstance.handleFileDownload(id);
                        }
                    }
                });
            }
            
            // Open preview modal
            await this.previewModal.open(fileId, fileName, fileType);
        } catch (error) {
            console.error('Error opening preview:', error);
            // Show error toast if available
            if (window.showToast) {
                window.showToast('Failed to open preview', 'error');
            }
        }
    }

    /**
     * Get appropriate icon for file type
     * Returns SVG path data for different file types
     * 
     * @param {string} fileType - MIME type
     * @param {string} fileName - File name
     * @returns {string} Icon identifier
     */
    static getFileIcon(fileType, fileName = '') {
        const type = (fileType || '').toLowerCase();
        const ext = this.getFileExtension(fileName).toLowerCase();
        
        if (type.includes('pdf') || ext === 'pdf') {
            return 'pdf';
        }
        if (type.includes('word') || ext === 'doc' || ext === 'docx') {
            return 'word';
        }
        if (type.includes('excel') || ext === 'xls' || ext === 'xlsx') {
            return 'excel';
        }
        if (type.includes('powerpoint') || ext === 'ppt' || ext === 'pptx') {
            return 'powerpoint';
        }
        if (type.includes('javascript') || ext === 'js') {
            return 'code';
        }
        if (type.includes('java') || ext === 'java') {
            return 'code';
        }
        if (type.includes('python') || ext === 'py') {
            return 'code';
        }
        if (type.includes('text') || ext === 'txt' || ext === 'md') {
            return 'text';
        }
        
        return 'file';
    }
}

// Create global instance for event handlers
window.filePreviewButton = new FilePreviewButton();

// Export for module usage
export default FilePreviewButton;
