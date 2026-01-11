/**
 * File Explorer Sync Service
 * 
 * Handles synchronization of file explorer state with server,
 * detecting deleted files/folders and updating UI accordingly.
 * 
 * Features:
 * - Periodic sync to detect deleted content
 * - Graceful 404 handling with UI cleanup
 * - User notifications for deleted content
 * - Auto-navigation when current folder is deleted
 */

import { fileExplorer } from '../core/api.js';
import { fileExplorerState } from './file-explorer-state.js';

class FileExplorerSyncService {
    constructor() {
        this.syncInterval = null;
        this.syncIntervalMs = 30000; // 30 seconds
        this.isEnabled = false;
        this.lastSyncTime = null;
        this.pendingValidations = new Map();
        this.deletedItems = new Set();
        this.listeners = [];
        
        // Bind methods
        this.performSync = this.performSync.bind(this);
        this.handleVisibilityChange = this.handleVisibilityChange.bind(this);
    }

    /**
     * Start the sync service
     * @param {number} intervalMs - Sync interval in milliseconds (default: 30000)
     */
    start(intervalMs = 30000) {
        if (this.isEnabled) return;
        
        this.syncIntervalMs = intervalMs;
        this.isEnabled = true;
        this.startSyncInterval();
        
        // Listen for visibility changes to pause/resume sync
        document.addEventListener('visibilitychange', this.handleVisibilityChange);
        
        console.log('[FileExplorerSync] Sync service started');
    }

    /**
     * Stop the sync service
     */
    stop() {
        this.isEnabled = false;
        this.stopSyncInterval();
        document.removeEventListener('visibilitychange', this.handleVisibilityChange);
        console.log('[FileExplorerSync] Sync service stopped');
    }

    /**
     * Start the sync interval
     */
    startSyncInterval() {
        if (this.syncInterval) return;
        
        this.syncInterval = setInterval(this.performSync, this.syncIntervalMs);
    }

    /**
     * Stop the sync interval
     */
    stopSyncInterval() {
        if (this.syncInterval) {
            clearInterval(this.syncInterval);
            this.syncInterval = null;
        }
    }

    /**
     * Handle page visibility changes
     */
    handleVisibilityChange() {
        if (document.hidden) {
            // Page is hidden, stop syncing to save resources
            this.stopSyncInterval();
        } else {
            // Page is visible again, resume syncing and perform immediate sync
            if (this.isEnabled) {
                this.startSyncInterval();
                this.performSync();
            }
        }
    }

    /**
     * Add a listener for sync events
     * @param {Function} callback - Callback function
     */
    addListener(callback) {
        if (typeof callback === 'function' && !this.listeners.includes(callback)) {
            this.listeners.push(callback);
        }
    }

    /**
     * Remove a listener
     * @param {Function} callback - Callback function to remove
     */
    removeListener(callback) {
        const index = this.listeners.indexOf(callback);
        if (index > -1) {
            this.listeners.splice(index, 1);
        }
    }

    /**
     * Notify all listeners of an event
     * @param {string} event - Event type
     * @param {Object} data - Event data
     */
    notify(event, data) {
        this.listeners.forEach(callback => {
            try {
                callback(event, data);
            } catch (e) {
                console.error('[FileExplorerSync] Listener error:', e);
            }
        });
    }

    /**
     * Perform a sync operation
     */
    async performSync() {
        if (!this.isEnabled) return;
        
        const currentPath = fileExplorerState.getCurrentPath();
        const currentNode = fileExplorerState.getCurrentNode();
        
        if (!currentPath && !currentNode) {
            // No active navigation, skip sync
            return;
        }

        try {
            // Validate current path exists
            if (currentPath) {
                const exists = await this.validatePath(currentPath);
                if (!exists) {
                    this.handleDeletedPath(currentPath, 'folder');
                }
            }
            
            // Validate files in current view
            if (currentNode && currentNode.files && currentNode.files.length > 0) {
                await this.validateFiles(currentNode.files);
            }

            this.lastSyncTime = Date.now();
        } catch (error) {
            console.error('[FileExplorerSync] Sync error:', error);
        }
    }

    /**
     * Validate if a path exists on the server
     * @param {string} path - Path to validate
     * @returns {Promise<boolean>} True if path exists
     */
    async validatePath(path) {
        if (!path) return true;
        
        try {
            const context = fileExplorerState.getContext();
            if (!context.semesterId) return true;
            
            await fileExplorer.getNode(path, context.semesterId);
            return true;
        } catch (error) {
            if (error.status === 404 || (error.message && error.message.includes('not found'))) {
                return false;
            }
            // Other errors - assume path exists to avoid false positives
            return true;
        }
    }

    /**
     * Validate if files exist in the DATABASE (not just on disk).
     * 
     * This checks the metadata endpoint which queries the database.
     * A 404 from this endpoint means the file RECORD is deleted (by admin),
     * not just that the physical file is missing on disk.
     * 
     * IMPORTANT: Files are only removed from UI when the DATABASE record
     * is deleted, NOT when the physical file is missing. This ensures:
     * - Files stay visible if the record exists (even if disk file is missing)
     * - Files only disappear when admin actually deletes them from the system
     * - On refresh, files that exist in database will still be shown
     * 
     * @param {Array} files - Array of file objects
     */
    async validateFiles(files) {
        const deletedFiles = [];
        
        for (const file of files) {
            if (!file.id) continue;
            
            try {
                // Check if file RECORD exists in database via metadata endpoint
                // This is different from checking if the physical file exists!
                const response = await fetch(`/api/file-explorer/files/${file.id}/metadata`, {
                    method: 'GET',
                    headers: this.getAuthHeaders()
                });
                
                if (response.status === 404) {
                    // Database record is deleted - file was removed by admin
                    console.log('[FileExplorerSync] File record deleted from database:', file.id, file.name);
                    deletedFiles.push(file);
                }
                // Note: 200 with "file not found on disk" is NOT a deletion
                // The record exists, just the physical file is missing
            } catch (error) {
                // Network error - skip validation, assume file exists
                console.warn('[FileExplorerSync] File validation error:', error);
            }
        }
        
        if (deletedFiles.length > 0) {
            this.handleDeletedFiles(deletedFiles);
        }
    }

    /**
     * Get authorization headers
     * @returns {Object} Headers object
     */
    getAuthHeaders() {
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        const headers = { 'Content-Type': 'application/json' };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    }

    /**
     * Handle a deleted path (folder)
     * @param {string} path - Deleted path
     * @param {string} type - Type of item (folder/file)
     */
    handleDeletedPath(path, type = 'folder') {
        console.log('[FileExplorerSync] Detected deleted path:', path);
        
        // Add to deleted items set
        this.deletedItems.add(path);
        
        // Notify listeners
        this.notify('pathDeleted', { path, type });
        
        // Show user notification
        this.showDeletedNotification(type, this.getPathName(path));
        
        // Navigate to parent or root
        this.navigateToSafeLocation(path);
    }

    /**
     * Handle files that were deleted from the DATABASE by an admin.
     * These files will be removed from the UI since they no longer exist in the system.
     * 
     * @param {Array} files - Array of deleted file objects
     */
    handleDeletedFiles(files) {
        console.log('[FileExplorerSync] Files deleted from database by admin:', files.length);
        
        // Add to deleted items set
        files.forEach(file => {
            this.deletedItems.add(`file_${file.id}`);
        });
        
        // Notify listeners
        this.notify('filesDeleted', { files });
        
        // Show notification - make it clear this was an admin action
        if (files.length === 1) {
            this.showDeletedNotification('file', files[0].name || 'File');
        } else {
            this.showDeletedNotification('files', `${files.length} files`);
        }
        
        // Remove files from UI since they no longer exist in the database
        this.removeFilesFromUI(files);
    }

    /**
     * Check if an item has been marked as deleted
     * @param {string} itemId - Item identifier (path or file_id)
     * @returns {boolean} True if item is deleted
     */
    isDeleted(itemId) {
        return this.deletedItems.has(itemId);
    }

    /**
     * Clear deleted items cache
     */
    clearDeletedCache() {
        this.deletedItems.clear();
    }

    /**
     * Show notification for content deleted by admin from the database.
     * @param {string} type - Type of deleted item
     * @param {string} name - Name of deleted item
     */
    showDeletedNotification(type, name) {
        const messages = {
            file: {
                title: 'File Deleted by Administrator',
                message: `"${name}" has been deleted from the system and is no longer available.`,
                icon: 'file'
            },
            files: {
                title: 'Files Deleted by Administrator',
                message: `${name} have been deleted from the system and are no longer available.`,
                icon: 'files'
            },
            folder: {
                title: 'Folder Deleted by Administrator',
                message: `"${name}" has been deleted from the system. Redirecting to available folder.`,
                icon: 'folder'
            }
        };

        const config = messages[type] || messages.folder;

        // Use Toast if available
        if (typeof window.Toast !== 'undefined') {
            window.Toast.warning(config.message, config.title);
        } else if (typeof window.showToast === 'function') {
            window.showToast(config.message, 'warning');
        }
    }

    /**
     * Get display name from path
     * @param {string} path - Full path
     * @returns {string} Display name
     */
    getPathName(path) {
        if (!path) return 'Unknown';
        const parts = path.split('/').filter(Boolean);
        return parts[parts.length - 1] || 'Folder';
    }

    /**
     * Navigate to a safe location after current location is deleted
     * @param {string} deletedPath - The deleted path
     */
    navigateToSafeLocation(deletedPath) {
        // Get parent path
        const pathParts = deletedPath.split('/').filter(Boolean);
        pathParts.pop(); // Remove deleted folder
        const parentPath = pathParts.join('/');

        // Dispatch event for FileExplorer to handle navigation
        window.dispatchEvent(new CustomEvent('fileExplorerNavigate', {
            detail: {
                path: parentPath || '',
                reason: 'deleted',
                deletedPath: deletedPath
            }
        }));
    }

    /**
     * Remove files from UI
     * @param {Array} files - Files to remove
     */
    removeFilesFromUI(files) {
        files.forEach(file => {
            const fileElement = document.querySelector(`[data-file-id="${file.id}"]`);
            if (fileElement) {
                // Add fade-out animation
                fileElement.style.transition = 'opacity 0.3s, transform 0.3s';
                fileElement.style.opacity = '0';
                fileElement.style.transform = 'translateX(-20px)';
                
                // Remove after animation
                setTimeout(() => {
                    fileElement.remove();
                }, 300);
            }
        });

        // Notify listeners to update state
        this.notify('filesRemoved', { files });
    }

    /**
     * Manually trigger validation for a specific file
     * @param {number} fileId - File ID to validate
     * @returns {Promise<boolean>} True if file exists
     */
    async validateFile(fileId) {
        try {
            const response = await fetch(`/api/file-explorer/files/${fileId}/metadata`, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });
            
            if (response.status === 404) {
                this.handleDeletedFiles([{ id: fileId, name: 'File' }]);
                return false;
            }
            
            return response.ok;
        } catch (error) {
            console.error('[FileExplorerSync] File validation error:', error);
            return true; // Assume exists on network error
        }
    }

    /**
     * Manually trigger validation for current view
     */
    async validateCurrentView() {
        await this.performSync();
    }
}

// Create singleton instance
const fileExplorerSync = new FileExplorerSyncService();

// Export
export { fileExplorerSync, FileExplorerSyncService };
export default fileExplorerSync;

