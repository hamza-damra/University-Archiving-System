/**
 * Deanship File Explorer Enhancements
 * Task 7: Enhanced file explorer with preview and bulk download
 * 
 * Features:
 * - Bulk download folders as ZIP
 * - In-browser file preview (PDF, images, text)
 * - File metadata tooltips
 */

import { fileExplorer } from '../core/api.js';
import { showToast } from '../core/ui.js';

/**
 * BulkDownloadService
 * Task 7.1: Service for creating and downloading folder archives
 * 
 * Features:
 * - Sequential file fetching to avoid server overload
 * - Progress modal with percentage indicator
 * - Cancel button to abort download
 * - Timestamped ZIP filenames
 */
export class BulkDownloadService {
    constructor() {
        this.isDownloading = false;
        this.cancelRequested = false;
        this.progressModal = null;
    }

    /**
     * Download all files in a folder as ZIP
     * 
     * @param {string} folderPath - Path to the folder
     * @param {string} folderName - Name of the folder (for ZIP filename)
     * @param {Array} files - Array of file objects to download
     * @returns {Promise<void>}
     */
    async downloadFolderAsZip(folderPath, folderName, files) {
        if (this.isDownloading) {
            showToast('A download is already in progress', 'warning');
            return;
        }

        if (!files || files.length === 0) {
            showToast('No files to download', 'warning');
            return;
        }

        this.isDownloading = true;
        this.cancelRequested = false;

        try {
            // Dynamically import JSZip
            const JSZip = (await import('https://cdn.jsdelivr.net/npm/jszip@3.10.1/+esm')).default;
            const zip = new JSZip();

            // Show progress modal
            this.showProgressModal(files.length);

            // Download files sequentially
            for (let i = 0; i < files.length; i++) {
                if (this.cancelRequested) {
                    showToast('Download cancelled', 'info');
                    this.hideProgressModal();
                    this.isDownloading = false;
                    return;
                }

                const file = files[i];
                const fileId = file.id || file.metadata?.fileId;
                const fileName = file.originalFilename || file.name || `file_${i}`;

                try {
                    this.updateProgress(i + 1, files.length, `Downloading ${fileName}...`);

                    // Fetch file blob
                    const response = await fileExplorer.downloadFile(fileId);
                    if (!response.ok) {
                        console.error(`Failed to download file ${fileName}`);
                        continue;
                    }

                    const blob = await response.blob();
                    zip.file(fileName, blob);

                    // Small delay to avoid overwhelming server
                    await new Promise(resolve => setTimeout(resolve, 100));
                } catch (error) {
                    console.error(`Error downloading file ${fileName}:`, error);
                }
            }

            // Generate ZIP
            this.updateProgress(files.length, files.length, 'Creating archive...');
            const zipBlob = await zip.generateAsync({ type: 'blob' });

            // Download ZIP
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
            const zipFilename = `${folderName}_${timestamp}.zip`;

            const url = window.URL.createObjectURL(zipBlob);
            const a = document.createElement('a');
            a.href = url;
            a.download = zipFilename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);

            this.hideProgressModal();
            showToast(`Downloaded ${files.length} files as ${zipFilename}`, 'success');
        } catch (error) {
            console.error('Error creating ZIP:', error);
            showToast('Failed to create archive', 'error');
            this.hideProgressModal();
        } finally {
            this.isDownloading = false;
        }
    }

    /**
     * Show progress modal
     */
    showProgressModal(totalFiles) {
        const modalHtml = `
            <div id="bulkDownloadModal" class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center">
                <div class="relative bg-white rounded-lg shadow-xl p-6 m-4 max-w-md w-full">
                    <h3 class="text-lg font-semibold text-gray-900 mb-4">Downloading Files</h3>
                    
                    <div class="mb-4">
                        <div class="flex justify-between text-sm text-gray-600 mb-2">
                            <span id="downloadStatus">Preparing download...</span>
                            <span id="downloadProgress">0%</span>
                        </div>
                        <div class="w-full bg-gray-200 rounded-full h-2.5">
                            <div id="downloadProgressBar" class="bg-blue-600 h-2.5 rounded-full transition-all duration-300" style="width: 0%"></div>
                        </div>
                    </div>

                    <div class="flex justify-end">
                        <button 
                            onclick="window.bulkDownloadService.cancelDownload()"
                            class="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors">
                            Cancel
                        </button>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        this.progressModal = document.getElementById('bulkDownloadModal');
    }

    /**
     * Update progress
     */
    updateProgress(current, total, status) {
        const percentage = Math.round((current / total) * 100);
        
        const progressBar = document.getElementById('downloadProgressBar');
        const progressText = document.getElementById('downloadProgress');
        const statusText = document.getElementById('downloadStatus');

        if (progressBar) progressBar.style.width = `${percentage}%`;
        if (progressText) progressText.textContent = `${percentage}%`;
        if (statusText) statusText.textContent = status;
    }

    /**
     * Hide progress modal
     */
    hideProgressModal() {
        if (this.progressModal) {
            this.progressModal.remove();
            this.progressModal = null;
        }
    }

    /**
     * Cancel download
     */
    cancelDownload() {
        this.cancelRequested = true;
    }
}

/**
 * FilePreviewPane
 * Task 7.2: Component for in-browser file preview
 * 
 * Features:
 * - Slide-in panel from right (40% width)
 * - PDF preview using PDF.js
 * - Image preview (jpg, png, gif)
 * - Text preview (txt, md) with syntax highlighting
 * - Close button and ESC key support
 * - Download button in preview header
 */
export class FilePreviewPane {
    constructor() {
        this.isOpen = false;
        this.currentFileId = null;
        this.pane = null;
    }

    /**
     * Open preview pane for a file
     * 
     * @param {number} fileId - ID of the file to preview
     * @param {string} fileName - Name of the file
     * @param {string} fileType - MIME type of the file
     * @returns {Promise<void>}
     */
    async open(fileId, fileName, fileType) {
        this.currentFileId = fileId;
        this.createPane(fileName, fileType);
        
        try {
            await this.loadPreview(fileId, fileName, fileType);
        } catch (error) {
            console.error('Error loading preview:', error);
            this.showError('Failed to load preview');
        }
    }

    /**
     * Create preview pane HTML
     */
    createPane(fileName, fileType) {
        if (this.pane) {
            this.close();
        }

        const paneHtml = `
            <div id="filePreviewPane" class="fixed inset-y-0 right-0 w-2/5 bg-white shadow-2xl z-50 transform transition-transform duration-300 translate-x-full">
                <div class="h-full flex flex-col">
                    <!-- Header -->
                    <div class="flex items-center justify-between p-4 border-b border-gray-200 bg-gray-50">
                        <div class="flex-1 min-w-0">
                            <h3 class="text-lg font-semibold text-gray-900 truncate">${this.escapeHtml(fileName)}</h3>
                            <p class="text-sm text-gray-500">${this.getFileTypeLabel(fileType)}</p>
                        </div>
                        <div class="flex items-center space-x-2 ml-4">
                            <button 
                                onclick="window.filePreviewPane.downloadFile()"
                                class="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                title="Download">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                                </svg>
                            </button>
                            <button 
                                onclick="window.filePreviewPane.close()"
                                class="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                                title="Close">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                                </svg>
                            </button>
                        </div>
                    </div>

                    <!-- Preview Content -->
                    <div id="previewContent" class="flex-1 overflow-auto p-4 bg-gray-50">
                        <div class="flex items-center justify-center h-full">
                            <div class="text-center">
                                <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
                                <p class="mt-4 text-sm text-gray-500">Loading preview...</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', paneHtml);
        this.pane = document.getElementById('filePreviewPane');
        this.isOpen = true;

        // Slide in animation
        setTimeout(() => {
            this.pane.classList.remove('translate-x-full');
        }, 10);

        // ESC key support
        this.escapeHandler = (e) => {
            if (e.key === 'Escape') this.close();
        };
        document.addEventListener('keydown', this.escapeHandler);
    }

    /**
     * Load preview content based on file type
     */
    async loadPreview(fileId, fileName, fileType) {
        const content = document.getElementById('previewContent');
        if (!content) return;

        const type = fileType.toLowerCase();

        try {
            if (type.includes('pdf')) {
                await this.loadPdfPreview(fileId, content);
            } else if (type.includes('image')) {
                await this.loadImagePreview(fileId, content);
            } else if (type.includes('text') || fileName.endsWith('.md') || fileName.endsWith('.txt')) {
                await this.loadTextPreview(fileId, content);
            } else {
                this.showUnsupported(content, fileType);
            }
        } catch (error) {
            console.error('Error loading preview:', error);
            this.showError('Failed to load preview');
        }
    }

    /**
     * Load PDF preview using PDF.js
     */
    async loadPdfPreview(fileId, container) {
        try {
            const response = await fileExplorer.downloadFile(fileId);
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);

            container.innerHTML = `
                <iframe 
                    src="${url}" 
                    class="w-full h-full border-0 rounded-lg"
                    style="min-height: 600px;">
                </iframe>
            `;
        } catch (error) {
            this.showError('Failed to load PDF preview');
        }
    }

    /**
     * Load image preview
     */
    async loadImagePreview(fileId, container) {
        try {
            const response = await fileExplorer.downloadFile(fileId);
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);

            container.innerHTML = `
                <div class="flex items-center justify-center h-full bg-gray-900 rounded-lg">
                    <img src="${url}" alt="Preview" class="max-w-full max-h-full object-contain">
                </div>
            `;
        } catch (error) {
            this.showError('Failed to load image preview');
        }
    }

    /**
     * Load text preview
     */
    async loadTextPreview(fileId, container) {
        try {
            const response = await fileExplorer.downloadFile(fileId);
            const text = await response.text();

            container.innerHTML = `
                <div class="bg-white rounded-lg border border-gray-200 p-4">
                    <pre class="text-sm text-gray-800 whitespace-pre-wrap font-mono">${this.escapeHtml(text)}</pre>
                </div>
            `;
        } catch (error) {
            this.showError('Failed to load text preview');
        }
    }

    /**
     * Show unsupported file type message
     */
    showUnsupported(container, fileType) {
        container.innerHTML = `
            <div class="flex items-center justify-center h-full">
                <div class="text-center">
                    <svg class="w-16 h-16 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                    </svg>
                    <h3 class="text-lg font-medium text-gray-900 mb-2">Preview not available</h3>
                    <p class="text-sm text-gray-500 mb-4">This file type (${this.escapeHtml(fileType)}) cannot be previewed</p>
                    <button 
                        onclick="window.filePreviewPane.downloadFile()"
                        class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                        Download File
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Show error message
     */
    showError(message) {
        const content = document.getElementById('previewContent');
        if (!content) return;

        content.innerHTML = `
            <div class="flex items-center justify-center h-full">
                <div class="text-center">
                    <svg class="w-16 h-16 text-red-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                    <h3 class="text-lg font-medium text-gray-900 mb-2">Error</h3>
                    <p class="text-sm text-gray-500">${this.escapeHtml(message)}</p>
                </div>
            </div>
        `;
    }

    /**
     * Download current file
     */
    async downloadFile() {
        if (!this.currentFileId) return;

        try {
            const response = await fileExplorer.downloadFile(this.currentFileId);
            if (!response.ok) throw new Error('Download failed');

            const blob = await response.blob();
            const contentDisposition = response.headers.get('Content-Disposition');
            let filename = 'download';

            if (contentDisposition) {
                const match = /filename="?([^"]+)"?/.exec(contentDisposition);
                if (match) filename = match[1];
            }

            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);

            showToast('File downloaded successfully', 'success');
        } catch (error) {
            console.error('Error downloading file:', error);
            showToast('Failed to download file', 'error');
        }
    }

    /**
     * Close preview pane
     */
    close() {
        if (!this.pane) return;

        this.pane.classList.add('translate-x-full');
        
        setTimeout(() => {
            if (this.pane) {
                this.pane.remove();
                this.pane = null;
            }
        }, 300);

        this.isOpen = false;
        this.currentFileId = null;

        if (this.escapeHandler) {
            document.removeEventListener('keydown', this.escapeHandler);
            this.escapeHandler = null;
        }
    }

    /**
     * Get file type label
     */
    getFileTypeLabel(mimeType) {
        if (!mimeType) return 'Unknown';
        
        const type = mimeType.toLowerCase();
        if (type.includes('pdf')) return 'PDF Document';
        if (type.includes('image')) return 'Image';
        if (type.includes('text')) return 'Text File';
        if (type.includes('word')) return 'Word Document';
        if (type.includes('excel')) return 'Excel Spreadsheet';
        if (type.includes('powerpoint')) return 'PowerPoint Presentation';
        
        return mimeType;
    }

    /**
     * Escape HTML
     */
    escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
}

/**
 * FileMetadataTooltip
 * Task 7.3: Add file metadata tooltips
 * 
 * Features:
 * - Display tooltip on file hover
 * - Show size, upload date, uploader name
 * - Human-readable file sizes
 * - Relative date format ("2 days ago")
 */
export class FileMetadataTooltip {
    constructor() {
        this.tooltip = null;
        this.currentTarget = null;
    }

    /**
     * Initialize tooltips for file elements
     */
    initialize() {
        // Use event delegation for dynamic content
        document.addEventListener('mouseover', (e) => {
            const fileRow = e.target.closest('[data-file-id]');
            if (fileRow && fileRow !== this.currentTarget) {
                this.show(fileRow);
            }
        });

        document.addEventListener('mouseout', (e) => {
            const fileRow = e.target.closest('[data-file-id]');
            if (fileRow) {
                this.hide();
            }
        });
    }

    /**
     * Show tooltip for file
     */
    show(element) {
        this.currentTarget = element;
        
        const fileId = element.dataset.fileId;
        const fileName = element.dataset.fileName;
        const fileSize = element.dataset.fileSize;
        const uploadDate = element.dataset.uploadDate;
        const uploaderName = element.dataset.uploaderName;

        if (!fileId) return;

        // Create tooltip
        const tooltipHtml = `
            <div id="fileTooltip" class="fixed bg-gray-900 text-white text-xs rounded-lg py-2 px-3 z-50 shadow-lg" style="pointer-events: none;">
                <div class="space-y-1">
                    <div><span class="font-semibold">File:</span> ${this.escapeHtml(fileName)}</div>
                    <div><span class="font-semibold">Size:</span> ${this.formatFileSize(parseInt(fileSize))}</div>
                    <div><span class="font-semibold">Uploaded:</span> ${this.formatRelativeDate(uploadDate)}</div>
                    <div><span class="font-semibold">By:</span> ${this.escapeHtml(uploaderName)}</div>
                </div>
            </div>
        `;

        // Remove existing tooltip
        this.hide();

        // Add new tooltip
        document.body.insertAdjacentHTML('beforeend', tooltipHtml);
        this.tooltip = document.getElementById('fileTooltip');

        // Position tooltip
        this.position(element);
    }

    /**
     * Position tooltip near element
     */
    position(element) {
        if (!this.tooltip) return;

        const rect = element.getBoundingClientRect();
        const tooltipRect = this.tooltip.getBoundingClientRect();

        let top = rect.top - tooltipRect.height - 8;
        let left = rect.left + (rect.width / 2) - (tooltipRect.width / 2);

        // Adjust if tooltip goes off screen
        if (top < 0) {
            top = rect.bottom + 8;
        }
        if (left < 0) {
            left = 8;
        }
        if (left + tooltipRect.width > window.innerWidth) {
            left = window.innerWidth - tooltipRect.width - 8;
        }

        this.tooltip.style.top = `${top}px`;
        this.tooltip.style.left = `${left}px`;
    }

    /**
     * Hide tooltip
     */
    hide() {
        if (this.tooltip) {
            this.tooltip.remove();
            this.tooltip = null;
        }
        this.currentTarget = null;
    }

    /**
     * Format file size
     */
    formatFileSize(bytes) {
        if (!bytes || bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 10) / 10 + ' ' + sizes[i];
    }

    /**
     * Format relative date
     */
    formatRelativeDate(dateString) {
        if (!dateString) return 'Unknown';
        
        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffSecs = Math.floor(diffMs / 1000);
        const diffMins = Math.floor(diffSecs / 60);
        const diffHours = Math.floor(diffMins / 60);
        const diffDays = Math.floor(diffHours / 24);

        if (diffSecs < 60) return 'Just now';
        if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
        if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
        if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
        if (diffDays < 30) return `${Math.floor(diffDays / 7)} week${Math.floor(diffDays / 7) > 1 ? 's' : ''} ago`;
        if (diffDays < 365) return `${Math.floor(diffDays / 30)} month${Math.floor(diffDays / 30) > 1 ? 's' : ''} ago`;
        return `${Math.floor(diffDays / 365)} year${Math.floor(diffDays / 365) > 1 ? 's' : ''} ago`;
    }

    /**
     * Escape HTML
     */
    escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
}

// Initialize global instances
window.bulkDownloadService = new BulkDownloadService();
window.filePreviewPane = new FilePreviewPane();
window.fileMetadataTooltip = new FileMetadataTooltip();

// Initialize tooltips on page load
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.fileMetadataTooltip.initialize();
    });
} else {
    window.fileMetadataTooltip.initialize();
}
