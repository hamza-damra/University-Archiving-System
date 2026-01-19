/**
 * Filesystem-Based File Browser Module
 * 
 * This module provides filesystem-synchronized browsing functionality.
 * It treats the physical filesystem as the source of truth and uses
 * ETag-based caching for efficient updates.
 * 
 * Features:
 * - Direct filesystem-based listing (no phantom folders)
 * - ETag caching with 304 Not Modified support
 * - Pagination for large directories
 * - Lazy loading for tree view
 * - Automatic refresh after write operations
 * - Loading states and error handling
 */

import { fileExplorer } from '../core/api.js';
import { showToast } from '../core/ui.js';

/**
 * FilesystemBrowser class
 * Manages filesystem-based directory browsing with caching
 */
export class FilesystemBrowser {
    constructor(options = {}) {
        this.options = {
            pageSize: options.pageSize || 50,
            sortBy: options.sortBy || 'name',
            sortOrder: options.sortOrder || 'asc',
            onFolderClick: options.onFolderClick || null,
            onFileClick: options.onFileClick || null,
            onLoadStart: options.onLoadStart || null,
            onLoadEnd: options.onLoadEnd || null,
            onError: options.onError || null,
            ...options
        };
        
        // State management
        this.currentPath = '';
        this.currentListing = null;
        this.currentPage = 1;
        this.etagCache = new Map();
        this.isLoading = false;
        
        // Tree state
        this.treeEtagCache = new Map();
        this.expandedNodes = new Set();
    }
    
    /**
     * Load directory contents from filesystem
     * 
     * @param {string} path - Relative path from uploads root
     * @param {Object} options - Loading options
     * @param {number} [options.page] - Page number
     * @param {boolean} [options.forceRefresh] - Skip ETag check
     * @returns {Promise<Object>} Directory listing
     */
    async loadDirectory(path = '', options = {}) {
        const {
            page = 1,
            forceRefresh = false
        } = options;
        
        this.isLoading = true;
        this.currentPath = path;
        this.currentPage = page;
        
        // Notify load start
        if (this.options.onLoadStart) {
            this.options.onLoadStart(path);
        }
        
        try {
            // Get cached ETag unless force refresh
            const cachedEtag = forceRefresh ? null : this.etagCache.get(path);
            
            const result = await fileExplorer.listDirectory(path, {
                page: page,
                pageSize: this.options.pageSize,
                sortBy: this.options.sortBy,
                sortOrder: this.options.sortOrder,
                etag: cachedEtag
            });
            
            // Handle 304 Not Modified
            if (result.notModified) {
                console.log('[FilesystemBrowser] Directory unchanged (304):', path);
                // Return cached listing if available
                if (this.currentListing && this.currentListing.path === path) {
                    this.options.onLoadEnd?.(this.currentListing);
                    return this.currentListing;
                }
            }
            
            // Update cache
            if (result.etag) {
                this.etagCache.set(path, result.etag);
            }
            
            this.currentListing = result.data;
            
            // Notify load end
            if (this.options.onLoadEnd) {
                this.options.onLoadEnd(result.data);
            }
            
            return result.data;
            
        } catch (error) {
            console.error('[FilesystemBrowser] Error loading directory:', error);
            
            if (this.options.onError) {
                this.options.onError(error, path);
            }
            
            throw error;
        } finally {
            this.isLoading = false;
        }
    }
    
    /**
     * Load tree children for a node (lazy loading)
     * 
     * @param {string} path - Path of the node
     * @param {number} [depth=1] - How many levels to load
     * @returns {Promise<Object>} Tree node with children
     */
    async loadTreeChildren(path = '', depth = 1) {
        try {
            const cachedEtag = this.treeEtagCache.get(path);
            
            const result = await fileExplorer.getDirectoryTree(path, depth, cachedEtag);
            
            if (result.notModified) {
                console.log('[FilesystemBrowser] Tree unchanged (304):', path);
                return null; // Use cached UI
            }
            
            if (result.etag) {
                this.treeEtagCache.set(path, result.etag);
            }
            
            // Track expanded state
            if (depth > 0) {
                this.expandedNodes.add(path);
            }
            
            return result.data;
            
        } catch (error) {
            console.error('[FilesystemBrowser] Error loading tree:', error);
            throw error;
        }
    }
    
    /**
     * Navigate to a folder
     * 
     * @param {string} path - Folder path
     */
    async navigateToFolder(path) {
        this.currentPath = path;
        this.currentPage = 1;
        return this.loadDirectory(path);
    }
    
    /**
     * Navigate to parent directory
     */
    async navigateUp() {
        if (!this.currentPath) {
            return this.currentListing;
        }
        
        const parts = this.currentPath.split('/');
        parts.pop();
        const parentPath = parts.join('/');
        
        return this.navigateToFolder(parentPath);
    }
    
    /**
     * Load next page
     */
    async loadNextPage() {
        if (this.currentListing && this.currentListing.hasMore) {
            return this.loadDirectory(this.currentPath, {
                page: this.currentPage + 1
            });
        }
        return this.currentListing;
    }
    
    /**
     * Load previous page
     */
    async loadPreviousPage() {
        if (this.currentPage > 1) {
            return this.loadDirectory(this.currentPath, {
                page: this.currentPage - 1
            });
        }
        return this.currentListing;
    }
    
    /**
     * Change sort settings and reload
     * 
     * @param {string} sortBy - Field to sort by
     * @param {string} [sortOrder] - Sort order (toggles if not specified)
     */
    async changeSort(sortBy, sortOrder = null) {
        if (sortOrder === null) {
            // Toggle order if same field, otherwise default to asc
            sortOrder = (this.options.sortBy === sortBy && this.options.sortOrder === 'asc')
                ? 'desc' : 'asc';
        }
        
        this.options.sortBy = sortBy;
        this.options.sortOrder = sortOrder;
        
        // Invalidate cache for current path
        this.etagCache.delete(this.currentPath);
        
        return this.loadDirectory(this.currentPath, { forceRefresh: true });
    }
    
    /**
     * Refresh current directory
     * 
     * @param {boolean} [invalidateCache=true] - Also invalidate server cache
     */
    async refresh(invalidateCache = true) {
        // Clear local cache
        this.etagCache.delete(this.currentPath);
        
        // Optionally invalidate server cache
        if (invalidateCache) {
            try {
                await fileExplorer.refreshCache(this.currentPath);
            } catch (error) {
                console.warn('[FilesystemBrowser] Could not refresh server cache:', error);
            }
        }
        
        return this.loadDirectory(this.currentPath, { forceRefresh: true });
    }
    
    /**
     * Refresh after a write operation (upload, create, delete)
     * This invalidates both local and server caches
     * 
     * @param {string} [path] - Path that was modified (defaults to current)
     */
    async refreshAfterWrite(path = null) {
        const targetPath = path || this.currentPath;
        
        // Clear local caches for path and parents
        this.invalidateLocalCache(targetPath);
        
        // Invalidate server cache
        try {
            await fileExplorer.refreshCache(targetPath, true);
        } catch (error) {
            console.warn('[FilesystemBrowser] Server cache refresh error:', error);
        }
        
        // Reload current directory if affected
        if (this.currentPath === targetPath || this.currentPath.startsWith(targetPath + '/')) {
            return this.loadDirectory(this.currentPath, { forceRefresh: true });
        } else if (targetPath.startsWith(this.currentPath + '/') || this.currentPath === '') {
            // Child was modified, still refresh to update counts
            return this.loadDirectory(this.currentPath, { forceRefresh: true });
        }
    }
    
    /**
     * Invalidate local cache for a path and its parents
     * 
     * @param {string} path - Path to invalidate
     */
    invalidateLocalCache(path) {
        // Invalidate exact path
        this.etagCache.delete(path);
        this.treeEtagCache.delete(path);
        
        // Invalidate parent paths
        const parts = path.split('/');
        while (parts.length > 0) {
            parts.pop();
            const parentPath = parts.join('/');
            this.etagCache.delete(parentPath);
            this.treeEtagCache.delete(parentPath);
        }
        
        // Invalidate root
        this.etagCache.delete('');
        this.treeEtagCache.delete('');
    }
    
    /**
     * Check if a path exists on the filesystem
     * 
     * @param {string} path - Path to check
     * @returns {Promise<boolean>}
     */
    async checkPathExists(path) {
        try {
            return await fileExplorer.checkPathExists(path);
        } catch (error) {
            return false;
        }
    }
    
    /**
     * Handle folder not found error
     * Navigates to nearest existing parent
     * 
     * @param {string} path - Path that was not found
     */
    async handleFolderNotFound(path) {
        console.log('[FilesystemBrowser] Folder not found:', path);
        
        // Try parent paths until one exists
        const parts = path.split('/');
        while (parts.length > 0) {
            parts.pop();
            const parentPath = parts.join('/');
            
            const exists = await this.checkPathExists(parentPath);
            if (exists || parentPath === '') {
                showToast('Folder no longer exists, navigating to parent', 'warning');
                return this.navigateToFolder(parentPath);
            }
        }
        
        // Navigate to root
        return this.navigateToFolder('');
    }
    
    // ==================== Getters ====================
    
    get path() {
        return this.currentPath;
    }
    
    get listing() {
        return this.currentListing;
    }
    
    get page() {
        return this.currentPage;
    }
    
    get loading() {
        return this.isLoading;
    }
    
    get hasMore() {
        return this.currentListing?.hasMore || false;
    }
    
    get totalPages() {
        return this.currentListing?.totalPages || 1;
    }
}

/**
 * Render functions for filesystem browser
 */
export const FilesystemBrowserRenderer = {
    
    /**
     * Render folder cards
     * 
     * @param {Array} folders - Array of FolderItemDTO
     * @param {Object} options - Render options
     * @returns {string} HTML string
     */
    renderFolderCards(folders, options = {}) {
        if (!folders || folders.length === 0) {
            return '';
        }
        
        return folders.map(folder => this.renderFolderCard(folder, options)).join('');
    },
    
    /**
     * Render a single folder card
     * 
     * @param {Object} folder - FolderItemDTO
     * @param {Object} options - Render options
     * @returns {string} HTML string
     */
    renderFolderCard(folder, options = {}) {
        const {
            showOwnershipBadge = false,
            showItemCount = true,
            currentUserId = null
        } = options;
        
        const isOwnFolder = folder.metadata?.isOwnFolder || false;
        const ownershipBadge = showOwnershipBadge && isOwnFolder
            ? '<span class="text-xs bg-blue-100 text-blue-800 px-2 py-0.5 rounded ml-2">Your Folder</span>'
            : '';
        
        const itemCountBadge = showItemCount && folder.itemCount >= 0
            ? `<span class="text-xs text-gray-500">${folder.itemCount} item${folder.itemCount !== 1 ? 's' : ''}</span>`
            : '';
        
        const modifiedDate = folder.modifiedAt 
            ? new Date(folder.modifiedAt).toLocaleDateString()
            : '';
        
        return `
            <div class="folder-card group cursor-pointer bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700 rounded-lg p-4 hover:bg-blue-100 dark:hover:bg-blue-900/30 hover:shadow-md transition-all duration-200"
                 data-path="${this.escapeHtml(folder.path)}"
                 data-folder-type="${folder.folderType || 'CUSTOM'}"
                 onclick="window.filesystemBrowser?.navigateToFolder('${this.escapeJs(folder.path)}')">
                <div class="flex items-center justify-between">
                    <div class="flex items-center space-x-3">
                        <div class="w-10 h-10 bg-blue-100 dark:bg-blue-800 rounded-lg flex items-center justify-center">
                            <svg class="w-6 h-6 text-blue-600 dark:text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                            </svg>
                        </div>
                        <div>
                            <div class="flex items-center">
                                <span class="font-medium text-gray-900 dark:text-gray-100">${this.escapeHtml(folder.name)}</span>
                                ${ownershipBadge}
                            </div>
                            <div class="flex items-center space-x-2 mt-1">
                                ${itemCountBadge}
                                ${modifiedDate ? `<span class="text-xs text-gray-400">${modifiedDate}</span>` : ''}
                            </div>
                        </div>
                    </div>
                    <svg class="w-5 h-5 text-gray-400 group-hover:text-blue-600 group-hover:translate-x-1 transition-all" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                    </svg>
                </div>
            </div>
        `;
    },
    
    /**
     * Render file list
     * 
     * @param {Array} files - Array of FileItemDTO
     * @param {Object} options - Render options
     * @returns {string} HTML string
     */
    renderFileList(files, options = {}) {
        if (!files || files.length === 0) {
            return this.renderEmptyState('No files in this folder');
        }
        
        const fileRows = files.map(file => this.renderFileRow(file, options)).join('');
        
        return `
            <div class="space-y-2">
                ${fileRows}
            </div>
        `;
    },
    
    /**
     * Render a file row
     * 
     * @param {Object} file - FileItemDTO
     * @param {Object} options - Render options
     * @returns {string} HTML string
     */
    renderFileRow(file, options = {}) {
        const {
            showActions = true,
            canDelete = false,
            canReplace = false
        } = options;
        
        const iconClass = this.getFileIconClass(file.extension);
        const orphanedBadge = file.orphaned 
            ? '<span class="text-xs bg-yellow-100 text-yellow-800 px-2 py-0.5 rounded ml-2">Untracked</span>' 
            : '';
        
        const actionButtons = showActions ? `
            <div class="flex items-center space-x-2">
                ${file.previewable && file.previewUrl ? `
                    <button class="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="Preview" onclick="event.stopPropagation(); window.filesystemBrowser?.previewFile(${file.id})">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                        </svg>
                    </button>
                ` : ''}
                ${file.downloadUrl ? `
                    <a href="${file.downloadUrl}" class="p-2 text-gray-500 hover:text-green-600 hover:bg-green-50 rounded-lg transition-colors"
                       title="Download" onclick="event.stopPropagation()">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                        </svg>
                    </a>
                ` : ''}
                ${(canDelete || file.canDelete) ? `
                    <button class="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title="Delete" onclick="event.stopPropagation(); window.filesystemBrowser?.deleteFile(${file.id}, '${this.escapeJs(file.name)}')">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                        </svg>
                    </button>
                ` : ''}
            </div>
        ` : '';
        
        return `
            <div class="file-row flex items-center justify-between p-3 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg hover:shadow-md transition-shadow"
                 data-file-id="${file.id || ''}"
                 data-path="${this.escapeHtml(file.path)}">
                <div class="flex items-center space-x-3 flex-1 min-w-0">
                    <div class="w-10 h-10 bg-gray-100 dark:bg-gray-700 rounded-lg flex items-center justify-center flex-shrink-0">
                        <i class="${iconClass} text-lg"></i>
                    </div>
                    <div class="flex-1 min-w-0">
                        <div class="flex items-center">
                            <span class="font-medium text-gray-900 dark:text-gray-100 truncate">${this.escapeHtml(file.name)}</span>
                            ${orphanedBadge}
                        </div>
                        <div class="flex items-center space-x-3 mt-1 text-sm text-gray-500">
                            <span>${file.sizeFormatted || this.formatFileSize(file.size)}</span>
                            <span>•</span>
                            <span>${file.extension?.toUpperCase() || 'FILE'}</span>
                            ${file.uploaderName ? `<span>• ${this.escapeHtml(file.uploaderName)}</span>` : ''}
                        </div>
                    </div>
                </div>
                ${actionButtons}
            </div>
        `;
    },
    
    /**
     * Render empty state
     * 
     * @param {string} message - Message to display
     * @param {string} [icon='folder'] - Icon type
     * @returns {string} HTML string
     */
    renderEmptyState(message, icon = 'folder') {
        const iconSvg = icon === 'folder' 
            ? '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>'
            : '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>';
        
        return `
            <div class="text-center py-12">
                <svg class="w-16 h-16 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    ${iconSvg}
                </svg>
                <p class="text-gray-500">${this.escapeHtml(message)}</p>
            </div>
        `;
    },
    
    /**
     * Render loading skeleton
     * 
     * @param {number} [count=5] - Number of skeleton items
     * @returns {string} HTML string
     */
    renderLoadingSkeleton(count = 5) {
        const items = [];
        for (let i = 0; i < count; i++) {
            items.push(`
                <div class="animate-pulse flex items-center space-x-3 p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                    <div class="w-10 h-10 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
                    <div class="flex-1">
                        <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4 mb-2"></div>
                        <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-1/2"></div>
                    </div>
                </div>
            `);
        }
        return items.join('');
    },
    
    /**
     * Render breadcrumbs
     * 
     * @param {string} path - Current path
     * @returns {string} HTML string
     */
    renderBreadcrumbs(path) {
        const parts = path ? path.split('/').filter(Boolean) : [];
        
        let html = `
            <nav class="flex" aria-label="Breadcrumb">
                <ol class="inline-flex items-center space-x-1">
                    <li class="inline-flex items-center">
                        <a href="#" class="text-blue-600 hover:text-blue-800 hover:underline flex items-center"
                           onclick="event.preventDefault(); window.filesystemBrowser?.navigateToFolder('')">
                            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path>
                            </svg>
                            Uploads
                        </a>
                    </li>
        `;
        
        let currentPath = '';
        for (let i = 0; i < parts.length; i++) {
            currentPath += (currentPath ? '/' : '') + parts[i];
            const isLast = i === parts.length - 1;
            
            html += `
                <li>
                    <div class="flex items-center">
                        <svg class="w-5 h-5 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                            <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"></path>
                        </svg>
                        ${isLast 
                            ? `<span class="ml-1 text-gray-700 dark:text-gray-300 font-medium">${this.escapeHtml(parts[i])}</span>`
                            : `<a href="#" class="ml-1 text-blue-600 hover:text-blue-800 hover:underline"
                                  onclick="event.preventDefault(); window.filesystemBrowser?.navigateToFolder('${this.escapeJs(currentPath)}')">${this.escapeHtml(parts[i])}</a>`
                        }
                    </div>
                </li>
            `;
        }
        
        html += `</ol></nav>`;
        return html;
    },
    
    /**
     * Render pagination controls
     * 
     * @param {Object} listing - DirectoryListingDTO
     * @returns {string} HTML string
     */
    renderPagination(listing) {
        if (!listing || listing.totalPages <= 1) {
            return '';
        }
        
        return `
            <div class="flex items-center justify-between mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
                <div class="text-sm text-gray-500">
                    Page ${listing.page} of ${listing.totalPages} (${listing.totalItems} items)
                </div>
                <div class="flex space-x-2">
                    <button class="px-3 py-1 bg-gray-100 dark:bg-gray-700 rounded hover:bg-gray-200 dark:hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
                            onclick="window.filesystemBrowser?.loadPreviousPage()"
                            ${listing.page <= 1 ? 'disabled' : ''}>
                        Previous
                    </button>
                    <button class="px-3 py-1 bg-gray-100 dark:bg-gray-700 rounded hover:bg-gray-200 dark:hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
                            onclick="window.filesystemBrowser?.loadNextPage()"
                            ${!listing.hasMore ? 'disabled' : ''}>
                        Next
                    </button>
                </div>
            </div>
        `;
    },
    
    // ==================== Utilities ====================
    
    getFileIconClass(extension) {
        const iconMap = {
            pdf: 'fas fa-file-pdf text-red-500',
            doc: 'fas fa-file-word text-blue-500',
            docx: 'fas fa-file-word text-blue-500',
            xls: 'fas fa-file-excel text-green-500',
            xlsx: 'fas fa-file-excel text-green-500',
            ppt: 'fas fa-file-powerpoint text-orange-500',
            pptx: 'fas fa-file-powerpoint text-orange-500',
            jpg: 'fas fa-file-image text-purple-500',
            jpeg: 'fas fa-file-image text-purple-500',
            png: 'fas fa-file-image text-purple-500',
            gif: 'fas fa-file-image text-purple-500',
            zip: 'fas fa-file-archive text-yellow-500',
            rar: 'fas fa-file-archive text-yellow-500',
            txt: 'fas fa-file-alt text-gray-500',
            csv: 'fas fa-file-csv text-green-600',
        };
        return iconMap[extension?.toLowerCase()] || 'fas fa-file text-gray-400';
    },
    
    formatFileSize(bytes) {
        if (!bytes || bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    },
    
    escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    },
    
    escapeJs(str) {
        if (!str) return '';
        return str.replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '\\"');
    }
};

// Export for use in other modules
export default { FilesystemBrowser, FilesystemBrowserRenderer };
