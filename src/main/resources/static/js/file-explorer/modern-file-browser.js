/**
 * Modern File Browser Module
 * 
 * Implements the "Modern" file browser mode featuring:
 * - Desktop-class UI (sidebar, toolbar, status bar)
 * - Grid and List views
 * - Advanced navigation (history, breadcrumbs)
 * - File preview integration
 * 
 * Styles are defined in: modern-file-browser.css
 */

import { fileExplorer } from '../core/api.js';
import { showToast, showModal, formatDate } from '../core/ui.js';
import { FilePreviewButton } from './file-preview-button.js'; // Reusing existing component
import { fileExplorerState } from './file-explorer-state.js';

export class ModernFileBrowser {
    /**
     * @param {string} containerId - DOM ID of the container
     * @param {Object} options - Configuration options
     */
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.container = document.getElementById(containerId);
        
        this.options = {
            role: 'PROFESSOR',
            startPath: '',
            viewMode: localStorage.getItem('mfb-view-mode') || 'grid', // 'grid' | 'list'
            showSidebar: localStorage.getItem('mfb-show-sidebar') !== 'false',
            ...options
        };
        
        // Navigation State
        this.currentPath = this.options.startPath || '';
        this.history = [this.currentPath];
        this.historyIndex = 0;
        
        // Data State
        this.files = [];
        this.folders = [];
        this.breadcrumbs = [];
        this.isLoading = false;
        
        // Bind methods
        this.handleFileClick = this.handleFileClick.bind(this);
        this.handleFolderClick = this.handleFolderClick.bind(this);
        this.handleBack = this.handleBack.bind(this);
        this.handleForward = this.handleForward.bind(this);
        this.handleUp = this.handleUp.bind(this);
        this.toggleViewMode = this.toggleViewMode.bind(this);
        this.handleFileDownload = this.handleFileDownload.bind(this);
        
        // Expose instance for FilePreviewButton compatibility
        window.fileExplorerInstance = this;

        this.init();
    }
    
    init() {
        this.renderLayout();
        // Don't auto-load content here - wait for loadRoot() to be called
        // This ensures academic year/semester context is set first
        this.renderEmptyState('Select a semester to view files');
        
        // Listen for global state changes if needed
        // fileExplorerState.addListener(...)
    }
    
    /**
     * Render initial empty state with message
     * @param {string} message - Message to display
     */
    renderEmptyState(message = 'This folder is empty') {
        const viewContainer = this.container.querySelector('#mfb-main-view');
        if (viewContainer) {
            viewContainer.innerHTML = `
                <div class="mfb-empty-state">
                    <i class="fas fa-folder-open"></i>
                    <p>${message}</p>
                </div>
            `;
        }
    }
    
    renderLayout() {
        if (!this.container) return;
        
        this.container.innerHTML = `
            <div class="modern-file-browser">
                <div class="mfb-toolbar">
                    <div class="mfb-nav-controls">
                        <button class="mfb-btn-icon" id="mfb-back" title="Back" disabled>
                            <i class="fas fa-arrow-left"></i>
                        </button>
                        <button class="mfb-btn-icon" id="mfb-forward" title="Forward" disabled>
                            <i class="fas fa-arrow-right"></i>
                        </button>
                        <button class="mfb-btn-icon" id="mfb-up" title="Up level">
                            <i class="fas fa-arrow-up"></i>
                        </button>
                        <button class="mfb-btn-icon" id="mfb-refresh" title="Refresh">
                            <i class="fas fa-sync-alt"></i>
                        </button>
                    </div>
                    
                    <div class="mfb-path-bar">
                        <div class="mfb-breadcrumbs" id="mfb-breadcrumbs">
                            <!-- Breadcrumbs injected here -->
                        </div>
                    </div>
                    
                    <div class="mfb-view-controls">
                        <div class="mfb-btn-group">
                            <button class="mfb-btn-icon ${this.options.viewMode === 'grid' ? 'active' : ''}" id="mfb-view-grid" title="Grid View">
                                <i class="fas fa-th-large"></i>
                            </button>
                            <button class="mfb-btn-icon ${this.options.viewMode === 'list' ? 'active' : ''}" id="mfb-view-list" title="List View">
                                <i class="fas fa-list"></i>
                            </button>
                        </div>
                        <button class="mfb-btn-icon" id="mfb-toggle-sidebar" title="Toggle Sidebar">
                            <i class="fas fa-columns"></i>
                        </button>
                    </div>
                </div>
                
                <div class="mfb-workspace">
                    <div class="mfb-sidebar ${this.options.showSidebar ? '' : 'hidden'}" id="mfb-sidebar">
                        <div class="mfb-sidebar-section">
                            <div class="mfb-sidebar-header">
                                <span>Quick Access</span>
                            </div>
                            <div class="mfb-sidebar-list" id="mfb-quick-access">
                                <div class="mfb-sidebar-item" data-path="">
                                    <i class="fas fa-home text-blue-500"></i>
                                    <span>Home</span>
                                </div>
                                <!-- More items could be added here -->
                            </div>
                        </div>
                    </div>
                    
                    <div class="mfb-main-view" id="mfb-main-view">
                        <!-- File content goes here -->
                    </div>
                </div>
                
                <div class="mfb-statusbar">
                    <span id="mfb-status-count">0 items</span>
                    <span id="mfb-status-selected"></span>
                </div>
            </div>
        `;
        
        // Attach Event Listeners
        this.bindEvents();
    }
    
    bindEvents() {
        this.container.querySelector('#mfb-back').addEventListener('click', this.handleBack);
        this.container.querySelector('#mfb-forward').addEventListener('click', this.handleForward);
        this.container.querySelector('#mfb-up').addEventListener('click', this.handleUp);
        this.container.querySelector('#mfb-refresh').addEventListener('click', () => this.loadCurrentPath());
        
        this.container.querySelector('#mfb-view-grid').addEventListener('click', () => this.toggleViewMode('grid'));
        this.container.querySelector('#mfb-view-list').addEventListener('click', () => this.toggleViewMode('list'));
        
        const sidebarToggle = this.container.querySelector('#mfb-toggle-sidebar');
        const sidebar = this.container.querySelector('#mfb-sidebar');
        sidebarToggle.addEventListener('click', () => {
            const isHidden = sidebar.classList.toggle('hidden');
            this.options.showSidebar = !isHidden;
            localStorage.setItem('mfb-show-sidebar', this.options.showSidebar);
        });

        // Quick Access
        const homeLink = this.container.querySelector('.mfb-sidebar-item[data-path=""]');
        if (homeLink) {
            homeLink.addEventListener('click', () => this.navigateTo(''));
        }
    }
    
    async loadCurrentPath() {
        this.setLoading(true);
        try {
            // Fetch data
            // Note: If path is empty, we might need getRoot, but getNode('') might work if handled by backend
            // Or we check fileExplorerState for academic params if at root
            
            let data;
            if (this.currentPath === '' && fileExplorerState.state.academicYearId) {
                // Initial load might use getRoot
                const { academicYearId, semesterId } = fileExplorerState.state;
                data = await fileExplorer.getRoot(academicYearId, semesterId);
            } else {
                data = await fileExplorer.getNode(this.currentPath);
            }
            
            if (data) {
                this.folders = data.folders || [];
                this.files = data.files || [];
                
                // Get breadcrumbs
                const breadcrumbsFn = fileExplorer.getBreadcrumbs(this.currentPath);
                // Handle if it's a promise or data
                this.breadcrumbs = await breadcrumbsFn;
                
                this.renderContent();
                this.renderBreadcrumbs();
                this.updateStatusBar();
                this.updateNavControls();
            }
        } catch (error) {
            console.error('Failed to load path:', error);
            showToast('Failed to load files', 'error');
            this.renderError(error);
        } finally {
            this.setLoading(false);
        }
    }
    
    renderContent() {
        const viewContainer = this.container.querySelector('#mfb-main-view');
        
        if (this.folders.length === 0 && this.files.length === 0) {
            viewContainer.innerHTML = `
                <div class="mfb-empty-state">
                    <i class="fas fa-folder-open"></i>
                    <p>This folder is empty</p>
                </div>
            `;
            return;
        }

        let html = '';
        const isGrid = this.options.viewMode === 'grid';
        
        html += `<div class="mfb-file-container ${isGrid ? 'mfb-view-grid' : 'mfb-view-list'}">`;
        
        // Folders first
        this.folders.forEach(folder => {
            html += this.renderFolderItem(folder, isGrid);
        });
        
        // Files
        this.files.forEach(file => {
            html += this.renderFileItem(file, isGrid);
        });
        
        html += '</div>';
        viewContainer.innerHTML = html;
        
        // Add listeners to new elements
        viewContainer.querySelectorAll('.mfb-item[data-type="folder"]').forEach(el => {
            el.addEventListener('click', (e) => this.handleFolderClick(e, el.dataset.path));
        });
        
        viewContainer.querySelectorAll('.mfb-item[data-type="file"]').forEach(el => {
             // File click handling (preview/download)
            el.addEventListener('click', (e) => this.handleFileClick(e, el.dataset.id));
        });

        // Initialize preview buttons if needed
        // Assuming FilePreviewButton can be attached or rewritten for new UI
    }

    renderFolderItem(folder, isGrid) {
        const icon = 'fa-folder';
        const colorClass = 'text-yellow-500'; // Or use CSS variable style
        
        if (isGrid) {
            return `
                <div class="mfb-item" data-type="folder" data-path="${folder.path}" title="${folder.name}">
                    <div class="mfb-item-icon ${colorClass}">
                        <i class="fas ${icon}"></i>
                    </div>
                    <div class="mfb-item-name">${folder.name}</div>
                    <div class="mfb-item-details">${folder.itemsCount || 0} items</div>
                </div>
            `;
        } else {
            return `
                <div class="mfb-item mfb-list-item" data-type="folder" data-path="${folder.path}">
                    <div class="mfb-col-icon ${colorClass}"><i class="fas ${icon}"></i></div>
                    <div class="mfb-col-name">${folder.name}</div>
                    <div class="mfb-col-date">-</div>
                    <div class="mfb-col-size">-</div>
                    <div class="mfb-col-type">Folder</div>
                </div>
            `;
        }
    }

    renderFileItem(file, isGrid) {
        const iconClass = this.getFileIcon(file.extension || file.fileType);
        
        if (isGrid) {
            return `
                <div class="mfb-item" data-type="file" data-id="${file.id}" title="${file.name}">
                    <div class="mfb-item-icon text-gray-500">
                        <i class="${iconClass}"></i>
                    </div>
                    <div class="mfb-item-name">${file.name}</div>
                    <div class="mfb-item-details">${this.formatSize(file.size)}</div>
                </div>
            `;
        } else {
            return `
                <div class="mfb-item mfb-list-item" data-type="file" data-id="${file.id}">
                    <div class="mfb-col-icon text-gray-500"><i class="${iconClass}"></i></div>
                    <div class="mfb-col-name">${file.name}</div>
                    <div class="mfb-col-date">${formatDate(file.uploadDate)}</div>
                    <div class="mfb-col-size">${this.formatSize(file.size)}</div>
                    <div class="mfb-col-type">${file.extension}</div>
                </div>
            `;
        }
    }
    
    renderBreadcrumbs() {
        const container = this.container.querySelector('#mfb-breadcrumbs');
        if (!container) return;
        
        let html = '';
        // Always start with Home/Root
        html += `<span class="mfb-breadcrumb-item ${!this.currentPath ? 'active' : ''}" data-path="">Home</span>`;
        
        this.breadcrumbs.forEach((crumb, index) => {
            html += `<span class="mfb-breadcrumb-separator"><i class="fas fa-chevron-right"></i></span>`;
            const isLast = index === this.breadcrumbs.length - 1;
            html += `<span class="mfb-breadcrumb-item ${isLast ? 'active' : ''}" data-path="${crumb.path}">${crumb.name}</span>`;
        });
        
        container.innerHTML = html;
        
        // Add click events
        container.querySelectorAll('.mfb-breadcrumb-item:not(.active)').forEach(el => {
            el.addEventListener('click', () => this.navigateTo(el.dataset.path));
        });
    }

    updateNavControls() {
        const backBtn = this.container.querySelector('#mfb-back');
        const fwdBtn = this.container.querySelector('#mfb-forward');
        const upBtn = this.container.querySelector('#mfb-up');
        
        backBtn.disabled = this.historyIndex <= 0;
        fwdBtn.disabled = this.historyIndex >= this.history.length - 1;
        upBtn.disabled = !this.currentPath;
    }
    
    updateStatusBar() {
        const countEl = this.container.querySelector('#mfb-status-count');
        const total = this.folders.length + this.files.length;
        countEl.textContent = `${total} item${total !== 1 ? 's' : ''}`;
    }

    setLoading(isLoading) {
        this.isLoading = isLoading;
        const view = this.container.querySelector('#mfb-main-view');
        if (isLoading) {
            view.classList.add('loading');
            view.innerHTML = '<div class="mfb-spinner"><i class="fas fa-circle-notch fa-spin"></i> Loading...</div>';
        } else {
            view.classList.remove('loading');
        }
    }
    
    renderError(error) {
         const viewContainer = this.container.querySelector('#mfb-main-view');
         viewContainer.innerHTML = `
            <div class="mfb-error-state">
                <i class="fas fa-exclamation-triangle"></i>
                <p>Error loading files</p>
                <button class="btn btn-sm btn-primary mt-2" onclick="this.loadCurrentPath()">Retry</button>
            </div>
         `;
    }

    // --- Actions ---

    navigateTo(path) {
        if (this.currentPath === path) return;
        
        // Add to history
        this.history = this.history.slice(0, this.historyIndex + 1);
        this.history.push(path);
        this.historyIndex++;
        
        this.currentPath = path;
        this.loadCurrentPath();
    }

    handleBack() {
        if (this.historyIndex > 0) {
            this.historyIndex--;
            this.currentPath = this.history[this.historyIndex];
            this.loadCurrentPath();
        }
    }

    handleForward() {
        if (this.historyIndex < this.history.length - 1) {
            this.historyIndex++;
            this.currentPath = this.history[this.historyIndex];
            this.loadCurrentPath();
        }
    }

    handleUp() {
        if (!this.currentPath) return;
        
        // Simple string manipulation for parent path
        // Assuming path separators are handled by backend or normalized
        // Should rely on breadcrumbs usually, but let's try popping last segment
        if (this.breadcrumbs.length > 1) {
             const parentPath = this.breadcrumbs[this.breadcrumbs.length - 2].path;
             this.navigateTo(parentPath);
        } else {
             this.navigateTo('');
        }
    }
    
    handleFolderClick(e, path) {
        e.preventDefault();
        this.navigateTo(path);
    }
    
    handleFileClick(e, fileId) {
        e.preventDefault();
        
        const file = this.files.find(f => String(f.id) === String(fileId));
        if (!file) return;

        const fileName = file.name || file.originalFilename || '';
        const fileType = file.extension ? this.getExtensionMimeType(file.extension) : (file.fileType || '');

        // Use FilePreviewButton logic for preview
        if (FilePreviewButton.isPreviewable(fileType, fileName)) {
            // Using the global instance from file-preview-button.js
            if (window.filePreviewButton) {
                window.filePreviewButton.handlePreviewClick(fileId, fileName, fileType);
            }
        } else {
            // Download if not previewable
            this.handleFileDownload(fileId);
        }
    }

    async handleFileDownload(fileId) {
        try {
            const response = await fileExplorer.downloadFile(fileId);
            if (response.ok) {
                 // Trigger download from blob
                 const blob = await response.blob();
                 const url = window.URL.createObjectURL(blob);
                 const a = document.createElement('a');
                 a.href = url;
                 // Try to get filename from headers or state
                 // For now, we rely on browser content-disposition
                 const disposition = response.headers.get('content-disposition');
                 let filename = 'download';
                 if (disposition && disposition.indexOf('attachment') !== -1) {
                     const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                     const matches = filenameRegex.exec(disposition);
                     if (matches != null && matches[1]) { 
                         filename = matches[1].replace(/['"]/g, '');
                     }
                 }
                 a.download = filename;
                 document.body.appendChild(a);
                 a.click();
                 window.URL.revokeObjectURL(url);
                 document.body.removeChild(a);
            } else {
                showToast('Download failed', 'error');
            }
        } catch (error) {
            console.error('Download error:', error);
            showToast('Download error', 'error');
        }
    }

    getExtensionMimeType(extension) {
        // Simple mapping to help preview button
        const map = {
            'pdf': 'application/pdf',
            'txt': 'text/plain',
            'md': 'text/markdown',
            'js': 'application/javascript',
            'css': 'text/css',
            'html': 'text/html',
            'java': 'text/x-java-source',
            'py': 'text/x-python',
            // Images
            'jpg': 'image/jpeg',
            'jpeg': 'image/jpeg',
            'png': 'image/png'
            // Add more as needed
        };
        return map[extension?.toLowerCase()] || '';
    }

    toggleViewMode(mode) {
        if (this.options.viewMode === mode) return;
        this.options.viewMode = mode;
        localStorage.setItem('mfb-view-mode', mode);
        
        // Update buttons
        this.container.querySelectorAll('.mfb-view-controls button').forEach(btn => btn.classList.remove('active'));
        this.container.querySelector(`#mfb-view-${mode}`).classList.add('active');
        
        this.renderContent();
    }
    
    // --- Public API (Interface compatibility with FileExplorer) ---
    
    /**
     * Load root directory for given academic year and semester
     * This method provides compatibility with FileExplorer interface
     * @param {number} academicYearId - Academic year ID
     * @param {number} semesterId - Semester ID
     */
    async loadRoot(academicYearId, semesterId) {
        // Store academic context in state using the correct API
        fileExplorerState.setContext(academicYearId, semesterId, null, null);
        
        // Reset to root path
        this.currentPath = '';
        this.history = [''];
        this.historyIndex = 0;
        
        // Load the root content
        await this.loadCurrentPath();
    }
    
    /**
     * Load a specific node/path in the file explorer
     * This method provides compatibility with FileExplorer interface
     * @param {string} path - The path to load
     */
    async loadNode(path) {
        this.navigateTo(path || '');
    }
    
    // --- Utilities ---
    
    formatSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
    
    getFileIcon(extension) {
        const icons = {
            'pdf': 'fas fa-file-pdf text-red-500',
            'doc': 'fas fa-file-word text-blue-500',
            'docx': 'fas fa-file-word text-blue-500',
            'xls': 'fas fa-file-excel text-green-500',
            'xlsx': 'fas fa-file-excel text-green-500',
            'ppt': 'fas fa-file-powerpoint text-orange-500',
            'pptx': 'fas fa-file-powerpoint text-orange-500',
            'jpg': 'fas fa-file-image text-purple-500',
            'jpeg': 'fas fa-file-image text-purple-500',
            'png': 'fas fa-file-image text-purple-500',
            'zip': 'fas fa-file-archive text-yellow-600',
            'rar': 'fas fa-file-archive text-yellow-600',
            'txt': 'fas fa-file-alt text-gray-400'
        };
        return icons[extension?.toLowerCase()] || 'fas fa-file text-gray-400';

    }
}

export default ModernFileBrowser;
