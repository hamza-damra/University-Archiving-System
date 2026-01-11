/**
 * File Preview Modal Component
 * Displays file content in a modal overlay with metadata and actions
 * 
 * Feature: file-preview-system
 * Requirements: 1.1, 1.2, 1.4, 6.1
 */

import { showToast } from '../core/ui.js';
import { TextRenderer } from '../renderers/text-renderer.js';
import { PDFRenderer } from '../renderers/pdf-renderer.js';
import { CodeRenderer } from '../renderers/code-renderer.js';
import { OfficeRenderer } from '../renderers/office-renderer.js';

/**
 * FilePreviewModal class
 * Manages the file preview modal UI and lifecycle
 * 
 * @example
 * const previewModal = new FilePreviewModal();
 * await previewModal.open(fileId, fileName, fileType);
 */
export class FilePreviewModal {
    /**
     * Create a new FilePreviewModal instance
     * @param {Object} options - Configuration options
     * @param {Function} options.onDownload - Callback for download action
     * @param {Function} options.onClose - Callback when modal closes
     */
    constructor(options = {}) {
        this.options = {
            onDownload: options.onDownload || null,
            onClose: options.onClose || null
        };
        
        this.modalElement = null;
        this.currentFileId = null;
        this.currentFileName = null;
        this.currentFileType = null;
        this.isOpen = false;
        this.currentRenderer = null;
        this.searchVisible = false;
        this.focusableElements = [];
        this.firstFocusableElement = null;
        this.lastFocusableElement = null;
        this.previouslyFocusedElement = null;
        
        // Drag state
        this.isDragging = false;
        this.dragStartX = 0;
        this.dragStartY = 0;
        this.modalStartX = 0;
        this.modalStartY = 0;
        
        // Bind methods to maintain context
        this.handleEscKey = this.handleEscKey.bind(this);
        this.handleClickOutside = this.handleClickOutside.bind(this);
        this.handleKeyboardNavigation = this.handleKeyboardNavigation.bind(this);
        this.trapFocus = this.trapFocus.bind(this);
        this.handleDragStart = this.handleDragStart.bind(this);
        this.handleDragMove = this.handleDragMove.bind(this);
        this.handleDragEnd = this.handleDragEnd.bind(this);
    }

    /**
     * Open preview modal for a file
     * @param {number} fileId - File ID
     * @param {string} fileName - File name
     * @param {string} fileType - File MIME type
     * @returns {Promise<void>}
     */
    async open(fileId, fileName, fileType) {
        if (this.isOpen) {
            this.close();
        }

        this.currentFileId = fileId;
        this.currentFileName = fileName;
        this.currentFileType = fileType;
        this.isOpen = true;

        // Store currently focused element to restore later
        this.previouslyFocusedElement = document.activeElement;

        // Create modal element
        this.createModal();
        
        // Show loading state
        this.showLoading();
        
        // Add event listeners
        this.addEventListeners();
        
        // Setup focus trap
        this.setupFocusTrap();
        
        // Announce modal opening to screen readers
        this.announceToScreenReader('File preview opened. Press Escape to close.');
        
        // Dispatch opened event
        this.dispatchEvent('preview:opened', {
            fileId,
            fileName,
            fileType
        });

        try {
            // Fetch file metadata
            const metadata = await this.fetchMetadata(fileId);
            
            // Update modal with metadata
            this.displayMetadata(metadata);
            
            // Check file size before loading (5MB threshold)
            const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB in bytes
            if (metadata.fileSize && metadata.fileSize > MAX_FILE_SIZE) {
                // Show large file warning
                this.showLargeFileWarning(metadata.fileSize);
                return;
            }
            
            // Fetch and display content (placeholder for now)
            await this.loadContent(fileId, fileType);
            
            // Dispatch loaded event
            this.dispatchEvent('preview:loaded', {
                fileId,
                fileName,
                metadata
            });
            
            // Announce successful load to screen readers
            this.announceToScreenReader(`File preview loaded successfully. ${this.currentFileName}`);
        } catch (error) {
            console.error('Error loading preview:', error);
            this.handleError(error);
        }
    }

    /**
     * Close the preview modal
     */
    close() {
        if (!this.isOpen) return;

        // Announce modal closing to screen readers
        this.announceToScreenReader('File preview closed.');

        // Remove event listeners
        this.removeEventListeners();
        
        // Remove modal from DOM
        if (this.modalElement && this.modalElement.parentNode) {
            this.modalElement.parentNode.removeChild(this.modalElement);
        }
        
        // Restore focus to previously focused element
        if (this.previouslyFocusedElement && this.previouslyFocusedElement.focus) {
            this.previouslyFocusedElement.focus();
        }
        
        this.modalElement = null;
        this.currentFileId = null;
        this.currentFileName = null;
        this.currentFileType = null;
        this.currentRenderer = null;
        this.isDragging = false;
        this.searchVisible = false;
        this.isOpen = false;
        this.focusableElements = [];
        this.firstFocusableElement = null;
        this.lastFocusableElement = null;
        this.previouslyFocusedElement = null;
        
        // Dispatch closed event
        this.dispatchEvent('preview:closed', {});
        
        // Call onClose callback
        if (this.options.onClose) {
            this.options.onClose();
        }
    }

    /**
     * Download the current file
     * @returns {Promise<void>}
     */
    async downloadFile() {
        if (!this.currentFileId) return;

        try {
            // Call onDownload callback if provided
            if (this.options.onDownload) {
                await this.options.onDownload(this.currentFileId, this.currentFileName);
            } else {
                // Default download implementation
                const token = localStorage.getItem('token') || sessionStorage.getItem('token');
                const headers = {};
                if (token) {
                    headers['Authorization'] = `Bearer ${token}`;
                }
                
                const response = await fetch(`/api/file-explorer/files/${this.currentFileId}/download`, {
                    method: 'GET',
                    headers: headers
                });
                
                if (!response.ok) {
                    throw new Error('Download failed');
                }
                
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = this.currentFileName;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);
                
                showToast('File downloaded successfully', 'success');
            }
        } catch (error) {
            console.error('Error downloading file:', error);
            showToast('Failed to download file', 'error');
        }
    }

    /**
     * Show large file warning with options
     * @param {number} fileSize - File size in bytes
     */
    showLargeFileWarning(fileSize) {
        const contentArea = this.modalElement?.querySelector('.preview-content');
        if (!contentArea) return;

        const formattedSize = this.formatFileSize(fileSize);
        const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

        contentArea.innerHTML = `
            <div class="flex flex-col items-center justify-center h-full py-12">
                <svg class="w-16 h-16 text-yellow-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                          d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                </svg>
                <p class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">Large File Warning</p>
                <p class="text-sm text-gray-600 dark:text-gray-400 mb-2 text-center max-w-md">
                    This file is <strong>${formattedSize}</strong>, which exceeds the recommended preview size of 5MB.
                </p>
                <p class="text-sm text-gray-600 dark:text-gray-400 mb-6 text-center max-w-md">
                    Loading large files may take longer and could affect browser performance.
                </p>
                <div class="flex flex-col sm:flex-row gap-3">
                    <button onclick="window.filePreviewModal.loadPartialPreview()" 
                            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                        <svg class="w-4 h-4 inline-block mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                  d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                        </svg>
                        Preview First Part
                    </button>
                    <button onclick="window.filePreviewModal.downloadFile()" 
                            class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors">
                        <svg class="w-4 h-4 inline-block mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                  d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                        </svg>
                        Download File
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Load partial preview for large files
     * Shows first N lines for text files or first N pages for documents
     */
    async loadPartialPreview() {
        if (!this.currentFileId || !this.currentFileType) {
            return;
        }

        // Show loading state
        this.showLoading();

        try {
            // Load content with partial flag
            await this.loadContent(this.currentFileId, this.currentFileType, { partial: true });
            
            // Dispatch loaded event
            this.dispatchEvent('preview:loaded', {
                fileId: this.currentFileId,
                fileName: this.currentFileName,
                partial: true
            });
        } catch (error) {
            console.error('Error loading partial preview:', error);
            this.handleError(error);
        }
    }

    /**
     * Show error state in modal
     * @param {string} message - Error message
     * @param {Object} options - Error display options
     * @param {string} options.type - Error type (network, permission, notfound, unsupported, service, corrupted)
     * @param {boolean} options.showRetry - Show retry button
     * @param {boolean} options.showDownload - Show download button
     */
    showError(message, options = {}) {
        const contentArea = this.modalElement?.querySelector('.preview-content');
        if (!contentArea) return;

        const errorType = options.type || 'generic';
        const showRetry = options.showRetry !== undefined ? options.showRetry : false;
        const showDownload = options.showDownload !== undefined ? options.showDownload : false;

        // Select appropriate icon and color based on error type
        let iconSvg = '';
        let iconColor = 'text-red-500';
        let errorTitle = 'Preview Error';

        switch (errorType) {
            case 'network':
                iconColor = 'text-orange-500';
                errorTitle = 'Network Error';
                iconSvg = `
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                          d="M18.364 5.636a9 9 0 010 12.728m0 0l-2.829-2.829m2.829 2.829L21 21M15.536 8.464a5 5 0 010 7.072m0 0l-2.829-2.829m-4.243 2.829a4.978 4.978 0 01-1.414-2.83m-1.414 5.658a9 9 0 01-2.167-9.238m7.824 2.167a1 1 0 111.414 1.414m-1.414-1.414L3 3m8.293 8.293l1.414 1.414"></path>
                `;
                break;
            case 'permission':
                iconColor = 'text-red-500';
                errorTitle = 'Access Denied';
                iconSvg = `
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                          d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                `;
                break;
            case 'notfound':
                iconColor = 'text-gray-500';
                errorTitle = 'File Not Found';
                iconSvg = `
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                          d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                `;
                break;
            case 'unsupported':
                iconColor = 'text-yellow-500';
                errorTitle = 'Preview Not Available';
                iconSvg = `
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                          d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                `;
                break;
            case 'service':
                iconColor = 'text-red-500';
                errorTitle = 'Service Unavailable';
                iconSvg = `
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                          d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                `;
                break;
            case 'corrupted':
                iconColor = 'text-red-500';
                errorTitle = 'Corrupted File';
                iconSvg = `
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                          d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                `;
                break;
            default:
                iconSvg = `
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                          d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                `;
        }

        // Build action buttons
        let actionButtons = '';
        
        if (showRetry) {
            actionButtons += `
                <button onclick="window.filePreviewModal.retryPreview()" 
                        class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                    <svg class="w-4 h-4 inline-block mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                    </svg>
                    Retry
                </button>
            `;
        }
        
        if (showDownload) {
            actionButtons += `
                <button onclick="window.filePreviewModal.downloadFile()" 
                        class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors ml-2">
                    <svg class="w-4 h-4 inline-block mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                    </svg>
                    Download File
                </button>
            `;
        }
        
        if (!showRetry && !showDownload) {
            actionButtons = `
                <button onclick="window.filePreviewModal.close()" 
                        class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                    Close
                </button>
            `;
        }

        contentArea.innerHTML = `
            <div class="flex flex-col items-center justify-center h-full py-12">
                <svg class="w-16 h-16 ${iconColor} mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    ${iconSvg}
                </svg>
                <p class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">${errorTitle}</p>
                <p class="text-sm text-gray-600 dark:text-gray-400 mb-6 text-center max-w-md">${message}</p>
                <div class="flex gap-2">
                    ${actionButtons}
                </div>
            </div>
        `;
    }

    /**
     * Retry loading the preview
     */
    async retryPreview() {
        if (!this.currentFileId || !this.currentFileName || !this.currentFileType) {
            return;
        }
        
        // Show loading state
        this.showLoading();
        
        try {
            // Fetch file metadata
            const metadata = await this.fetchMetadata(this.currentFileId);
            
            // Update modal with metadata
            this.displayMetadata(metadata);
            
            // Fetch and display content
            await this.loadContent(this.currentFileId, this.currentFileType);
            
            // Dispatch loaded event
            this.dispatchEvent('preview:loaded', {
                fileId: this.currentFileId,
                fileName: this.currentFileName,
                metadata
            });
        } catch (error) {
            console.error('Error retrying preview:', error);
            this.handleError(error);
        }
    }

    /**
     * Handle errors with appropriate error type detection
     * @private
     * @param {Error} error - Error object
     */
    handleError(error) {
        let errorType = 'generic';
        let showRetry = false;
        let showDownload = false;

        // Parse error message - handle JSON error responses
        let message = error.message || 'Failed to load preview';
        try {
            if (typeof message === 'string' && message.startsWith('{')) {
                const parsed = JSON.parse(message);
                message = parsed.error?.message || parsed.message || 'Failed to load preview';
            }
        } catch (e) {
            // Not JSON, use as-is
        }
        
        // Use parseApiError if available for consistent error parsing
        if (typeof window.parseApiError === 'function') {
            const parsed = window.parseApiError(error);
            message = parsed.message || message;
        }

        // Detect error type from error message or properties
        if (error.name === 'TypeError' && error.message && error.message.includes('fetch')) {
            // Network error
            errorType = 'network';
            message = 'Unable to connect to the server. Please check your internet connection and try again.';
            showRetry = true;
        } else if (error.status === 403 || message.includes('permission')) {
            // Permission error
            errorType = 'permission';
            message = 'You don\'t have permission to preview this file. Please contact your administrator if you believe this is an error.';
        } else if (error.status === 404 || message.includes('not found') || message.includes('deleted')) {
            // File not found - physical file may be missing but record still exists
            errorType = 'notfound';
            message = 'This file could not be found on the server. It may have been moved or deleted.';
            showDownload = false; // Can't download if file is missing
            
            // NOTE: Do NOT dispatch fileDeleted event here!
            // The file record may still exist in database (physical file missing).
            // Files are only removed from UI when the sync service detects 
            // the database record was actually deleted by an admin.
        } else if (error.status === 500 || message.includes('service') || message.includes('server')) {
            // Service error
            errorType = 'service';
            message = 'The preview service is temporarily unavailable. Please try again later.';
            showRetry = true;
        } else if (message.includes('corrupted') || message.includes('invalid') || message.includes('malformed')) {
            // Corrupted file
            errorType = 'corrupted';
            message = 'This file appears to be corrupted and cannot be previewed. You may try downloading it to verify.';
            showDownload = true;
        } else if (message.includes('unsupported') || message.includes('not available')) {
            // Unsupported format
            errorType = 'unsupported';
            message = 'This file type cannot be previewed in the browser. Please download the file to view its contents.';
            showDownload = true;
        } else if (message.includes('convert') || message.includes('conversion')) {
            // Conversion failure
            errorType = 'corrupted';
            message = 'Unable to convert this document for preview. The file may be corrupted or in an unsupported format.';
            showDownload = true;
        }

        this.showError(message, { type: errorType, showRetry, showDownload });
        
        // Show toast notification for user feedback
        if (typeof window.Toast !== 'undefined') {
            const toastTitle = {
                'network': 'Connection Error',
                'permission': 'Access Denied',
                'notfound': 'File Not Found',
                'service': 'Service Unavailable',
                'corrupted': 'File Error',
                'unsupported': 'Unsupported Format',
                'generic': 'Preview Error'
            }[errorType] || 'Error';
            
            window.Toast.error(message, toastTitle);
        }
        
        // Announce error to screen readers
        this.announceToScreenReader(`Error loading preview: ${message}`);
        
        // Dispatch error event
        this.dispatchEvent('preview:error', {
            fileId: this.currentFileId,
            fileName: this.currentFileName,
            error: message,
            errorType
        });
    }

    /**
     * Show loading state in modal
     * Ensures loading indicator appears within 100ms with smooth fade-in animation
     * Requirements: 6.1
     */
    showLoading() {
        const contentArea = this.modalElement?.querySelector('.preview-content');
        if (!contentArea) return;

        // Use requestAnimationFrame to ensure immediate rendering
        requestAnimationFrame(() => {
            contentArea.innerHTML = `
                <div class="flex flex-col items-center justify-center h-full py-12 loading-fade-in">
                    <div class="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mb-4" role="status" aria-label="Loading"></div>
                    <p class="text-sm text-gray-600 dark:text-gray-400">Loading preview...</p>
                </div>
            `;
        });
        
        // Announce loading state to screen readers
        this.announceToScreenReader('Loading file preview. Please wait.');
    }

    /**
     * Create modal DOM structure
     * @private
     */
    createModal() {
        // Create modal container
        const modal = document.createElement('div');
        modal.className = 'file-preview-modal fade-in';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');
        modal.setAttribute('aria-labelledby', 'preview-modal-title');
        modal.setAttribute('aria-describedby', 'preview-modal-description');

        modal.innerHTML = `
            <!-- Backdrop -->
            <div class="preview-backdrop" aria-hidden="true"></div>
            
            <!-- Modal Content -->
            <div class="preview-modal-content">
                <!-- Header - Draggable -->
                <div class="preview-modal-header" id="preview-modal-drag-handle">
                    <div class="flex-1 min-w-0">
                        <h2 id="preview-modal-title" class="preview-modal-title">
                            ${this.escapeHtml(this.currentFileName)}
                        </h2>
                        <div id="preview-modal-description" class="preview-metadata">
                            <!-- Metadata hidden for cleaner UI -->
                        </div>
                    </div>
                    <div class="preview-actions" role="toolbar" aria-label="Preview actions">
                        <!-- Search Button (for text-based content) -->
                        <button id="preview-search-toggle" 
                                onclick="window.filePreviewModal.toggleSearch()" 
                                class="preview-action-btn hidden"
                                type="button"
                                title="Search in preview (Ctrl+F)"
                                aria-label="Search in preview. Press Control F to activate">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                      d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                            </svg>
                        </button>
                        <!-- Download Button -->
                        <button onclick="window.filePreviewModal.downloadFile()" 
                                class="preview-action-btn"
                                type="button"
                                title="Download file (Ctrl+D)"
                                aria-label="Download file. Press Control D to download">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                      d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                            </svg>
                        </button>
                        <!-- Close Button -->
                        <button onclick="window.filePreviewModal.close()" 
                                class="preview-action-btn"
                                type="button"
                                title="Close preview (Escape)"
                                aria-label="Close preview. Press Escape to close">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                      d="M6 18L18 6M6 6l12 12"></path>
                            </svg>
                        </button>
                    </div>
                </div>
                
                <!-- Search Bar (hidden by default) -->
                <div id="preview-search-bar" class="preview-search-bar hidden" role="search" aria-label="Search within preview">
                    <div class="flex items-center gap-2">
                        <div class="flex-1 relative">
                            <input type="text" 
                                   id="preview-search-input" 
                                   placeholder="Search in preview..." 
                                   class="preview-search-input"
                                   aria-label="Search query"
                                   aria-describedby="preview-search-count">
                            <button onclick="window.filePreviewModal.clearSearch()" 
                                    class="absolute right-2 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                                    type="button"
                                    title="Clear search"
                                    aria-label="Clear search">
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                                </svg>
                            </button>
                        </div>
                        <div class="flex items-center gap-1">
                            <span id="preview-search-count" class="text-sm text-gray-600 dark:text-gray-400 min-w-[80px] text-center" role="status" aria-live="polite">
                                <!-- Match count will appear here -->
                            </span>
                            <button onclick="window.filePreviewModal.previousSearchMatch()" 
                                    class="p-2 text-gray-600 dark:text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                    type="button"
                                    title="Previous match (Shift+Enter)"
                                    aria-label="Go to previous search match. Press Shift Enter"
                                    id="preview-search-prev">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                                </svg>
                            </button>
                            <button onclick="window.filePreviewModal.nextSearchMatch()" 
                                    class="p-2 text-gray-600 dark:text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                    type="button"
                                    title="Next match (Enter)"
                                    aria-label="Go to next search match. Press Enter"
                                    id="preview-search-next">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                                </svg>
                            </button>
                        </div>
                    </div>
                </div>
                
                <!-- Content Area -->
                <div class="preview-content" role="document" aria-label="File content" tabindex="0">
                    <!-- Content will be loaded here -->
                </div>
                
                <!-- Resize Handle -->
                <div class="preview-resize-handle" aria-hidden="true"></div>
                
                <!-- Screen reader announcements -->
                <div id="preview-sr-announcements" class="sr-only" role="status" aria-live="polite" aria-atomic="true"></div>
            </div>
        `;

        // Add to modals container or body
        const modalsContainer = document.getElementById('modalsContainer') || document.body;
        modalsContainer.appendChild(modal);
        
        this.modalElement = modal;
        
        // Make modal accessible globally for onclick handlers
        window.filePreviewModal = this;
        
        // Setup drag functionality
        this.setupDragFunctionality();
    }

    /**
     * Add event listeners for modal interactions
     * @private
     */
    addEventListeners() {
        // ESC key and keyboard navigation handler
        document.addEventListener('keydown', this.handleEscKey);
        document.addEventListener('keydown', this.handleKeyboardNavigation);
        
        // Focus trap handler
        document.addEventListener('keydown', this.trapFocus);
        
        // Click outside handler
        const backdrop = this.modalElement?.querySelector('.preview-backdrop');
        if (backdrop) {
            backdrop.addEventListener('click', this.handleClickOutside);
        }
        
        // Search input handler
        const searchInput = this.modalElement?.querySelector('#preview-search-input');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                const query = e.target.value;
                if (query) {
                    this.performSearch(query);
                } else {
                    this.clearSearch();
                }
            });
            
            // Handle Enter key to navigate to next match
            searchInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    if (e.shiftKey) {
                        this.previousSearchMatch();
                    } else {
                        this.nextSearchMatch();
                    }
                }
            });
        }
    }

    /**
     * Remove event listeners
     * @private
     */
    removeEventListeners() {
        document.removeEventListener('keydown', this.handleEscKey);
        document.removeEventListener('keydown', this.handleKeyboardNavigation);
        document.removeEventListener('keydown', this.trapFocus);
        
        // Clean up drag listeners
        document.removeEventListener('mousemove', this.handleDragMove);
        document.removeEventListener('mouseup', this.handleDragEnd);
        document.removeEventListener('touchmove', this.handleDragMove);
        document.removeEventListener('touchend', this.handleDragEnd);
    }

    /**
     * Handle ESC key press
     * @private
     */
    handleEscKey(event) {
        if (event.key === 'Escape' && this.isOpen) {
            this.close();
        }
    }

    /**
     * Handle click outside modal
     * @private
     */
    handleClickOutside(event) {
        if (event.target.classList.contains('preview-backdrop')) {
            this.close();
        }
    }

    /**
     * Setup drag functionality for the modal
     * @private
     */
    setupDragFunctionality() {
        const dragHandle = this.modalElement?.querySelector('#preview-modal-drag-handle');
        const modalContent = this.modalElement?.querySelector('.preview-modal-content');
        
        if (!dragHandle || !modalContent) return;
        
        // Set initial position to center
        modalContent.style.position = 'relative';
        modalContent.style.left = '0';
        modalContent.style.top = '0';
        
        dragHandle.addEventListener('mousedown', this.handleDragStart);
        dragHandle.addEventListener('touchstart', this.handleDragStart, { passive: false });
    }

    /**
     * Handle drag start
     * @private
     */
    handleDragStart(event) {
        // Don't drag if clicking on buttons
        if (event.target.closest('button')) return;
        
        const modalContent = this.modalElement?.querySelector('.preview-modal-content');
        if (!modalContent) return;
        
        this.isDragging = true;
        modalContent.classList.add('dragging');
        
        // Get starting positions
        const clientX = event.type === 'touchstart' ? event.touches[0].clientX : event.clientX;
        const clientY = event.type === 'touchstart' ? event.touches[0].clientY : event.clientY;
        
        this.dragStartX = clientX;
        this.dragStartY = clientY;
        this.modalStartX = parseInt(modalContent.style.left || '0', 10);
        this.modalStartY = parseInt(modalContent.style.top || '0', 10);
        
        // Add move and end listeners
        document.addEventListener('mousemove', this.handleDragMove);
        document.addEventListener('mouseup', this.handleDragEnd);
        document.addEventListener('touchmove', this.handleDragMove, { passive: false });
        document.addEventListener('touchend', this.handleDragEnd);
        
        event.preventDefault();
    }

    /**
     * Handle drag move
     * @private
     */
    handleDragMove(event) {
        if (!this.isDragging) return;
        
        const modalContent = this.modalElement?.querySelector('.preview-modal-content');
        if (!modalContent) return;
        
        const clientX = event.type === 'touchmove' ? event.touches[0].clientX : event.clientX;
        const clientY = event.type === 'touchmove' ? event.touches[0].clientY : event.clientY;
        
        const deltaX = clientX - this.dragStartX;
        const deltaY = clientY - this.dragStartY;
        
        // Calculate new position
        let newX = this.modalStartX + deltaX;
        let newY = this.modalStartY + deltaY;
        
        // Get viewport bounds
        const rect = modalContent.getBoundingClientRect();
        const maxX = window.innerWidth - rect.width / 2;
        const maxY = window.innerHeight - 50; // Keep at least 50px visible
        const minX = -rect.width / 2;
        const minY = -rect.height + 50;
        
        // Constrain to viewport
        newX = Math.max(minX, Math.min(maxX, newX));
        newY = Math.max(minY, Math.min(maxY, newY));
        
        modalContent.style.left = `${newX}px`;
        modalContent.style.top = `${newY}px`;
        
        event.preventDefault();
    }

    /**
     * Handle drag end
     * @private
     */
    handleDragEnd() {
        if (!this.isDragging) return;
        
        this.isDragging = false;
        
        const modalContent = this.modalElement?.querySelector('.preview-modal-content');
        if (modalContent) {
            modalContent.classList.remove('dragging');
        }
        
        // Remove move and end listeners
        document.removeEventListener('mousemove', this.handleDragMove);
        document.removeEventListener('mouseup', this.handleDragEnd);
        document.removeEventListener('touchmove', this.handleDragMove);
        document.removeEventListener('touchend', this.handleDragEnd);
    }

    /**
     * Fetch file metadata from API
     * @private
     * @param {number} fileId - File ID
     * @returns {Promise<Object>} File metadata
     */
    async fetchMetadata(fileId) {
        try {
            // Get auth token from localStorage or sessionStorage
            const token = localStorage.getItem('token') || sessionStorage.getItem('token');
            const headers = {
                'Content-Type': 'application/json'
            };
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }
            
            const response = await fetch(`/api/file-explorer/files/${fileId}/metadata`, {
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
                    error.message = 'You don\'t have permission to preview this file';
                } else if (response.status === 500) {
                    error.message = 'Server error occurred while loading file metadata';
                }
                
                throw error;
            }
            
            // Parse JSON response (API returns wrapped response)
            const jsonResponse = await response.json();
            
            // Extract metadata from ApiResponse wrapper
            if (jsonResponse.success && jsonResponse.data) {
                return jsonResponse.data;
            } else {
                const error = new Error(jsonResponse.message || 'Failed to load file metadata');
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
     * Display file metadata in modal header
     * @private
     * @param {Object} metadata - File metadata
     */
    displayMetadata(metadata) {
        // Metadata display is disabled for cleaner UI
        // The metadata container is hidden via CSS
        // This function is kept for potential future use or API compatibility
        const metadataContainer = this.modalElement?.querySelector('.preview-metadata');
        if (!metadataContainer) return;
        
        // Clear any existing content - keep header clean
        metadataContainer.innerHTML = '';
    }

    /**
     * Load and display file content
     * @private
     * @param {number} fileId - File ID
     * @param {string} fileType - File MIME type
     * @param {Object} options - Loading options
     * @param {boolean} options.partial - Load partial content for large files
     * @returns {Promise<void>}
     */
    async loadContent(fileId, fileType, options = {}) {
        const contentArea = this.modalElement?.querySelector('.preview-content');
        if (!contentArea) return;

        // Determine renderer based on file type
        const renderer = this.selectRenderer(fileType);
        
        if (!renderer) {
            // Unsupported format
            contentArea.innerHTML = `
                <div class="flex flex-col items-center justify-center h-full py-12">
                    <svg class="w-16 h-16 text-yellow-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                    </svg>
                    <p class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">Preview Not Available</p>
                    <p class="text-sm text-gray-600 dark:text-gray-400 mb-6 text-center max-w-md">
                        This file type cannot be previewed in the browser.<br>
                        Please download the file to view its contents.
                    </p>
                    <button onclick="window.filePreviewModal.downloadFile()" 
                            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                        Download File
                    </button>
                </div>
            `;
            return;
        }
        
        // Store renderer for search functionality
        this.currentRenderer = renderer;
        
        // Show/hide search button based on renderer type
        if (renderer && (renderer instanceof TextRenderer || renderer.constructor.name === 'TextRenderer')) {
            this.showSearchButton();
        } else {
            this.hideSearchButton();
        }
        
        // Render content using appropriate renderer
        try {
            // Pass partial flag to renderer if supported
            if (options.partial && renderer.renderPartial) {
                await renderer.renderPartial(fileId, contentArea);
            } else if (options.partial) {
                // Fallback: render with partial flag in options
                await renderer.render(fileId, contentArea, { partial: true });
            } else {
                await renderer.render(fileId, contentArea);
            }
        } catch (error) {
            throw error; // Re-throw to be caught by open() method
        }
    }

    /**
     * Select appropriate renderer based on file type
     * @private
     * @param {string} fileType - File MIME type
     * @returns {Object|null} Renderer instance or null if unsupported
     */
    selectRenderer(fileType) {
        // PDF files
        if (fileType === 'application/pdf') {
            return new PDFRenderer();
        }
        
        // Code file types
        const codeTypes = [
            'text/javascript',
            'application/javascript',
            'text/x-java-source',
            'text/x-python',
            'text/x-c',
            'text/x-c++',
            'text/x-csharp',
            'text/x-php',
            'text/x-ruby',
            'text/x-go',
            'text/x-rust',
            'text/x-swift',
            'text/x-kotlin',
            'text/x-scala',
            'text/css',
            'text/html',
            'application/xhtml+xml',
            'text/x-sql'
        ];
        
        if (codeTypes.includes(fileType)) {
            const language = CodeRenderer.detectLanguage(this.currentFileName);
            return { 
                render: async (fileId, container) => {
                    const codeRenderer = new CodeRenderer();
                    return await codeRenderer.render(fileId, container, language);
                }
            };
        }
        
        // Text file types (plain text, markdown, CSV, JSON, XML)
        const textTypes = [
            'text/plain',
            'text/markdown',
            'text/csv',
            'text/log',
            'application/json',
            'application/xml',
            'text/xml'
        ];
        
        if (textTypes.includes(fileType) || fileType.startsWith('text/')) {
            return new TextRenderer();
        }
        
        // Office documents (doc, docx, xls, xlsx, ppt, pptx)
        if (OfficeRenderer.supportsFormat(fileType)) {
            return new OfficeRenderer();
        }
        
        return null; // Unsupported format
    }

    /**
     * Toggle search bar visibility
     */
    toggleSearch() {
        const searchBar = this.modalElement?.querySelector('#preview-search-bar');
        const searchToggle = this.modalElement?.querySelector('#preview-search-toggle');
        
        if (!searchBar || !searchToggle) return;
        
        this.searchVisible = !this.searchVisible;
        
        if (this.searchVisible) {
            searchBar.classList.remove('hidden');
            searchToggle.classList.add('text-blue-600', 'dark:text-blue-400');
            searchToggle.setAttribute('aria-pressed', 'true');
            
            // Focus search input
            const searchInput = this.modalElement?.querySelector('#preview-search-input');
            if (searchInput) {
                setTimeout(() => {
                    searchInput.focus();
                    this.announceToScreenReader('Search bar opened. Enter search query.');
                }, 100);
            }
        } else {
            searchBar.classList.add('hidden');
            searchToggle.classList.remove('text-blue-600', 'dark:text-blue-400');
            searchToggle.setAttribute('aria-pressed', 'false');
            this.clearSearch();
            this.announceToScreenReader('Search bar closed.');
        }
    }

    /**
     * Perform search in preview content
     * @param {string} query - Search query
     */
    performSearch(query) {
        if (!this.currentRenderer || !this.currentRenderer.search) {
            return;
        }
        
        const matchCount = this.currentRenderer.search(query);
        this.updateSearchUI(matchCount);
        
        if (matchCount > 0) {
            // Highlight and scroll to first match
            this.highlightCurrentMatch();
            this.announceToScreenReader(`Found ${matchCount} match${matchCount === 1 ? '' : 'es'} for "${query}"`);
        } else {
            this.announceToScreenReader(`No matches found for "${query}"`);
        }
    }

    /**
     * Navigate to next search match
     */
    nextSearchMatch() {
        if (!this.currentRenderer || !this.currentRenderer.nextMatch) {
            return;
        }
        
        const match = this.currentRenderer.nextMatch();
        if (match) {
            this.highlightCurrentMatch();
            this.updateSearchUI();
            
            // Get current match info for announcement
            const currentMatch = this.currentRenderer.getCurrentMatch();
            if (currentMatch) {
                this.announceToScreenReader(`Match ${currentMatch.index + 1} of ${currentMatch.total}`);
            }
        }
    }

    /**
     * Navigate to previous search match
     */
    previousSearchMatch() {
        if (!this.currentRenderer || !this.currentRenderer.previousMatch) {
            return;
        }
        
        const match = this.currentRenderer.previousMatch();
        if (match) {
            this.highlightCurrentMatch();
            this.updateSearchUI();
            
            // Get current match info for announcement
            const currentMatch = this.currentRenderer.getCurrentMatch();
            if (currentMatch) {
                this.announceToScreenReader(`Match ${currentMatch.index + 1} of ${currentMatch.total}`);
            }
        }
    }

    /**
     * Clear search results
     */
    clearSearch() {
        const searchInput = this.modalElement?.querySelector('#preview-search-input');
        if (searchInput) {
            searchInput.value = '';
        }
        
        if (this.currentRenderer && this.currentRenderer.clearSearch) {
            this.currentRenderer.clearSearch();
        }
        
        this.updateSearchUI(0);
        this.removeHighlights();
    }

    /**
     * Update search UI with match count
     * @private
     * @param {number} matchCount - Number of matches (optional, will get from renderer if not provided)
     */
    updateSearchUI(matchCount) {
        const countElement = this.modalElement?.querySelector('#preview-search-count');
        const prevButton = this.modalElement?.querySelector('#preview-search-prev');
        const nextButton = this.modalElement?.querySelector('#preview-search-next');
        
        if (!countElement) return;
        
        // Get current match info from renderer
        let currentMatch = null;
        if (this.currentRenderer && this.currentRenderer.getCurrentMatch) {
            currentMatch = this.currentRenderer.getCurrentMatch();
        }
        
        if (matchCount === undefined && currentMatch) {
            matchCount = currentMatch.total;
        }
        
        if (matchCount > 0 && currentMatch) {
            countElement.textContent = `${currentMatch.index + 1} of ${currentMatch.total}`;
            if (prevButton) prevButton.disabled = false;
            if (nextButton) nextButton.disabled = false;
        } else if (matchCount === 0) {
            countElement.textContent = 'No matches';
            if (prevButton) prevButton.disabled = true;
            if (nextButton) nextButton.disabled = true;
        } else {
            countElement.textContent = '';
            if (prevButton) prevButton.disabled = true;
            if (nextButton) nextButton.disabled = true;
        }
    }

    /**
     * Highlight current search match
     * @private
     */
    highlightCurrentMatch() {
        if (!this.currentRenderer || !this.currentRenderer.getCurrentMatch) {
            return;
        }
        
        const match = this.currentRenderer.getCurrentMatch();
        if (!match) return;
        
        const contentArea = this.modalElement?.querySelector('.preview-content');
        if (!contentArea) return;
        
        // Remove previous highlights
        this.removeHighlights();
        
        // Find the pre element containing the text
        const preElement = contentArea.querySelector('pre');
        if (!preElement) return;
        
        // Get the text content
        const textContent = preElement.textContent;
        if (!textContent) return;
        
        // Get search query from input
        const searchInput = this.modalElement?.querySelector('#preview-search-input');
        const query = searchInput?.value || '';
        if (!query) return;
        
        // Create highlighted HTML
        const beforeMatch = textContent.substring(0, match.start);
        const matchText = textContent.substring(match.start, match.end);
        const afterMatch = textContent.substring(match.end);
        
        // Escape HTML and create highlighted version
        const escapedBefore = this.escapeHtml(beforeMatch);
        const escapedMatch = this.escapeHtml(matchText);
        const escapedAfter = this.escapeHtml(afterMatch);
        
        // Replace all matches with highlighted versions
        let highlightedHTML = escapedBefore;
        
        // Highlight all matches
        const lowerContent = textContent.toLowerCase();
        const lowerQuery = query.toLowerCase();
        let searchIndex = 0;
        let htmlIndex = escapedBefore.length;
        
        while ((searchIndex = lowerContent.indexOf(lowerQuery, searchIndex)) !== -1) {
            const isCurrentMatch = searchIndex === match.start;
            const matchTextPart = textContent.substring(searchIndex, searchIndex + query.length);
            const escapedMatchPart = this.escapeHtml(matchTextPart);
            
            if (isCurrentMatch) {
                highlightedHTML += `<mark class="bg-yellow-300 dark:bg-yellow-600 text-gray-900 dark:text-gray-100 font-semibold" id="current-search-match">${escapedMatchPart}</mark>`;
            } else {
                highlightedHTML += `<mark class="bg-yellow-100 dark:bg-yellow-800 text-gray-900 dark:text-gray-100">${escapedMatchPart}</mark>`;
            }
            
            searchIndex += query.length;
            
            // Add text between matches
            const nextMatchIndex = lowerContent.indexOf(lowerQuery, searchIndex);
            if (nextMatchIndex !== -1) {
                const betweenText = textContent.substring(searchIndex, nextMatchIndex);
                highlightedHTML += this.escapeHtml(betweenText);
                searchIndex = nextMatchIndex;
            } else {
                // Add remaining text
                highlightedHTML += this.escapeHtml(textContent.substring(searchIndex));
                break;
            }
        }
        
        // Update pre element with highlighted HTML
        preElement.innerHTML = highlightedHTML;
        
        // Scroll to current match
        const currentMatchElement = preElement.querySelector('#current-search-match');
        if (currentMatchElement) {
            currentMatchElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }

    /**
     * Remove search highlights
     * @private
     */
    removeHighlights() {
        const contentArea = this.modalElement?.querySelector('.preview-content');
        if (!contentArea) return;
        
        const preElement = contentArea.querySelector('pre');
        if (!preElement) return;
        
        // If there are mark elements, restore original text
        const marks = preElement.querySelectorAll('mark');
        if (marks.length > 0) {
            // Get original text from renderer
            if (this.currentRenderer && this.currentRenderer.currentContent) {
                preElement.textContent = this.currentRenderer.currentContent;
            }
        }
    }

    /**
     * Show search button for text-based content
     * @private
     */
    showSearchButton() {
        const searchToggle = this.modalElement?.querySelector('#preview-search-toggle');
        if (searchToggle) {
            searchToggle.classList.remove('hidden');
        }
    }

    /**
     * Hide search button for non-text content
     * @private
     */
    hideSearchButton() {
        const searchToggle = this.modalElement?.querySelector('#preview-search-toggle');
        if (searchToggle) {
            searchToggle.classList.add('hidden');
        }
        
        // Also hide search bar if visible
        if (this.searchVisible) {
            this.toggleSearch();
        }
    }

    /**
     * Dispatch custom event
     * @private
     * @param {string} eventName - Event name
     * @param {Object} detail - Event detail data
     */
    dispatchEvent(eventName, detail) {
        const event = new CustomEvent(eventName, {
            detail,
            bubbles: true,
            cancelable: true
        });
        
        if (this.modalElement) {
            this.modalElement.dispatchEvent(event);
        } else {
            document.dispatchEvent(event);
        }
    }

    /**
     * Format file size for display
     * @private
     * @param {number} bytes - File size in bytes
     * @returns {string} Formatted file size
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
    }

    /**
     * Format date for display
     * @private
     * @param {string} dateString - ISO date string
     * @returns {string} Formatted date
     */
    formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
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
     * Setup focus trap for modal
     * Identifies all focusable elements and sets up focus management
     * @private
     */
    setupFocusTrap() {
        if (!this.modalElement) return;

        // Find all focusable elements within the modal
        const focusableSelectors = [
            'button:not([disabled])',
            'input:not([disabled])',
            'textarea:not([disabled])',
            'select:not([disabled])',
            'a[href]',
            '[tabindex]:not([tabindex="-1"])'
        ].join(', ');

        const modalContent = this.modalElement.querySelector('.relative');
        if (!modalContent) return;

        this.focusableElements = Array.from(modalContent.querySelectorAll(focusableSelectors));
        this.firstFocusableElement = this.focusableElements[0];
        this.lastFocusableElement = this.focusableElements[this.focusableElements.length - 1];

        // Focus the first focusable element (close button)
        if (this.firstFocusableElement) {
            setTimeout(() => {
                this.firstFocusableElement.focus();
            }, 100);
        }
    }

    /**
     * Trap focus within modal
     * Prevents Tab from moving focus outside the modal
     * @private
     * @param {KeyboardEvent} event - Keyboard event
     */
    trapFocus(event) {
        if (!this.isOpen || event.key !== 'Tab') return;

        // Update focusable elements list (in case search bar was toggled)
        const focusableSelectors = [
            'button:not([disabled])',
            'input:not([disabled])',
            'textarea:not([disabled])',
            'select:not([disabled])',
            'a[href]',
            '[tabindex]:not([tabindex="-1"])'
        ].join(', ');

        const modalContent = this.modalElement?.querySelector('.relative');
        if (!modalContent) return;

        this.focusableElements = Array.from(modalContent.querySelectorAll(focusableSelectors));
        this.firstFocusableElement = this.focusableElements[0];
        this.lastFocusableElement = this.focusableElements[this.focusableElements.length - 1];

        // If shift+tab on first element, go to last
        if (event.shiftKey && document.activeElement === this.firstFocusableElement) {
            event.preventDefault();
            this.lastFocusableElement?.focus();
        }
        // If tab on last element, go to first
        else if (!event.shiftKey && document.activeElement === this.lastFocusableElement) {
            event.preventDefault();
            this.firstFocusableElement?.focus();
        }
    }

    /**
     * Handle keyboard navigation shortcuts
     * @private
     * @param {KeyboardEvent} event - Keyboard event
     */
    handleKeyboardNavigation(event) {
        if (!this.isOpen) return;

        // Don't interfere if user is typing in an input
        if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') {
            return;
        }

        // Ctrl/Cmd + F: Toggle search
        if ((event.ctrlKey || event.metaKey) && event.key === 'f') {
            event.preventDefault();
            this.toggleSearch();
            this.announceToScreenReader('Search toggled');
        }

        // Ctrl/Cmd + D: Download file
        if ((event.ctrlKey || event.metaKey) && event.key === 'd') {
            event.preventDefault();
            this.downloadFile();
            this.announceToScreenReader('Downloading file');
        }

        // Arrow keys for navigation (if renderer supports it)
        if (this.currentRenderer) {
            // Left arrow: Previous page/section
            if (event.key === 'ArrowLeft' && this.currentRenderer.previousPage) {
                event.preventDefault();
                this.currentRenderer.previousPage();
                this.announceToScreenReader('Previous page');
            }

            // Right arrow: Next page/section
            if (event.key === 'ArrowRight' && this.currentRenderer.nextPage) {
                event.preventDefault();
                this.currentRenderer.nextPage();
                this.announceToScreenReader('Next page');
            }

            // Up arrow: Scroll up
            if (event.key === 'ArrowUp') {
                const contentArea = this.modalElement?.querySelector('.preview-content');
                if (contentArea) {
                    event.preventDefault();
                    contentArea.scrollBy({ top: -100, behavior: 'smooth' });
                }
            }

            // Down arrow: Scroll down
            if (event.key === 'ArrowDown') {
                const contentArea = this.modalElement?.querySelector('.preview-content');
                if (contentArea) {
                    event.preventDefault();
                    contentArea.scrollBy({ top: 100, behavior: 'smooth' });
                }
            }

            // Page Up: Scroll up one page
            if (event.key === 'PageUp') {
                const contentArea = this.modalElement?.querySelector('.preview-content');
                if (contentArea) {
                    event.preventDefault();
                    contentArea.scrollBy({ top: -contentArea.clientHeight, behavior: 'smooth' });
                }
            }

            // Page Down: Scroll down one page
            if (event.key === 'PageDown') {
                const contentArea = this.modalElement?.querySelector('.preview-content');
                if (contentArea) {
                    event.preventDefault();
                    contentArea.scrollBy({ top: contentArea.clientHeight, behavior: 'smooth' });
                }
            }

            // Home: Scroll to top
            if (event.key === 'Home') {
                const contentArea = this.modalElement?.querySelector('.preview-content');
                if (contentArea) {
                    event.preventDefault();
                    contentArea.scrollTo({ top: 0, behavior: 'smooth' });
                    this.announceToScreenReader('Scrolled to top');
                }
            }

            // End: Scroll to bottom
            if (event.key === 'End') {
                const contentArea = this.modalElement?.querySelector('.preview-content');
                if (contentArea) {
                    event.preventDefault();
                    contentArea.scrollTo({ top: contentArea.scrollHeight, behavior: 'smooth' });
                    this.announceToScreenReader('Scrolled to bottom');
                }
            }
        }
    }

    /**
     * Announce message to screen readers
     * @private
     * @param {string} message - Message to announce
     */
    announceToScreenReader(message) {
        const announcer = this.modalElement?.querySelector('#preview-sr-announcements') || 
                         document.getElementById('preview-sr-announcements');
        
        if (!announcer) {
            // Create announcer if it doesn't exist
            const newAnnouncer = document.createElement('div');
            newAnnouncer.id = 'preview-sr-announcements';
            newAnnouncer.className = 'sr-only';
            newAnnouncer.setAttribute('role', 'status');
            newAnnouncer.setAttribute('aria-live', 'polite');
            newAnnouncer.setAttribute('aria-atomic', 'true');
            document.body.appendChild(newAnnouncer);
            
            // Set message after a brief delay to ensure screen reader picks it up
            setTimeout(() => {
                newAnnouncer.textContent = message;
            }, 100);
            return;
        }

        // Clear previous message
        announcer.textContent = '';
        
        // Set new message after a brief delay to ensure screen reader picks it up
        setTimeout(() => {
            announcer.textContent = message;
        }, 100);
    }
}
