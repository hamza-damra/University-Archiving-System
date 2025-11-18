/**
 * File Explorer Component
 * Reusable component for hierarchical file navigation
 */

import { fileExplorer } from './api.js';
import { showToast, showModal, formatDate } from './ui.js';

/**
 * FileExplorer class
 * Manages the file explorer UI and interactions
 */
export class FileExplorer {
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        if (!this.container) {
            throw new Error(`Container element with id "${containerId}" not found`);
        }

        this.options = {
            readOnly: options.readOnly || false,
            onFileClick: options.onFileClick || null,
            onNodeExpand: options.onNodeExpand || null,
            ...options
        };

        this.currentPath = '';
        this.currentNode = null;
        this.breadcrumbs = [];
        
        this.render();
    }

    /**
     * Render the file explorer UI
     */
    render() {
        this.container.innerHTML = `
            <div class="file-explorer">
                <!-- Breadcrumbs -->
                <div id="fileExplorerBreadcrumbs" class="bg-gray-50 px-4 py-3 border-b border-gray-200">
                    <nav class="flex" aria-label="Breadcrumb">
                        <ol class="inline-flex items-center space-x-1 md:space-x-3">
                            <li class="inline-flex items-center">
                                <span class="text-sm text-gray-500">Select a semester to browse files</span>
                            </li>
                        </ol>
                    </nav>
                </div>

                <!-- File Explorer Content -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-4 p-4">
                    <!-- Tree View -->
                    <div class="md:col-span-1 bg-white border border-gray-200 rounded-lg p-4">
                        <h3 class="text-sm font-semibold text-gray-700 mb-3">Folder Structure</h3>
                        <div id="fileExplorerTree" class="space-y-1">
                            <div class="text-sm text-gray-500 py-4 text-center">
                                No data loaded
                            </div>
                        </div>
                    </div>

                    <!-- File List -->
                    <div class="md:col-span-2 bg-white border border-gray-200 rounded-lg p-4">
                        <h3 class="text-sm font-semibold text-gray-700 mb-3">Files</h3>
                        <div id="fileExplorerFileList" class="overflow-x-auto">
                            <div class="text-sm text-gray-500 py-8 text-center">
                                Select a folder to view files
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Load root node for a semester
     */
    async loadRoot(academicYearId, semesterId) {
        try {
            const response = await fileExplorer.getRoot(academicYearId, semesterId);
            this.currentNode = response.data || response;
            this.currentPath = this.currentNode.path || '';
            
            await this.loadBreadcrumbs(this.currentPath);
            this.renderTree(this.currentNode);
            this.renderFileList(this.currentNode);
        } catch (error) {
            console.error('Error loading file explorer root:', error);
            showToast('Failed to load file explorer', 'error');
            this.renderError('Failed to load file explorer');
        }
    }

    /**
     * Load a specific node by path
     */
    async loadNode(path) {
        try {
            const response = await fileExplorer.getNode(path);
            this.currentNode = response.data || response;
            this.currentPath = path;
            
            await this.loadBreadcrumbs(path);
            this.renderFileList(this.currentNode);
        } catch (error) {
            console.error('Error loading node:', error);
            showToast('Failed to load folder', 'error');
        }
    }

    /**
     * Load breadcrumbs for current path
     */
    async loadBreadcrumbs(path) {
        try {
            if (!path) {
                this.breadcrumbs = [];
                this.renderBreadcrumbs();
                return;
            }

            const response = await fileExplorer.getBreadcrumbs(path);
            this.breadcrumbs = response.data || response || [];
            this.renderBreadcrumbs();
        } catch (error) {
            console.error('Error loading breadcrumbs:', error);
            this.breadcrumbs = [];
            this.renderBreadcrumbs();
        }
    }

    /**
     * Render breadcrumbs with improved navigation
     */
    renderBreadcrumbs() {
        const container = document.getElementById('fileExplorerBreadcrumbs');
        if (!container) return;

        if (this.breadcrumbs.length === 0) {
            container.innerHTML = `
                <nav class="flex items-center" aria-label="Breadcrumb">
                    <ol class="inline-flex items-center space-x-1">
                        <li class="inline-flex items-center">
                            <svg class="w-4 h-4 text-gray-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
                                <path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"></path>
                            </svg>
                            <span class="text-sm text-gray-500">Select a folder to navigate</span>
                        </li>
                    </ol>
                </nav>
            `;
            return;
        }

        // Add home icon for first breadcrumb
        const breadcrumbHtml = this.breadcrumbs.map((crumb, index) => {
            const isLast = index === this.breadcrumbs.length - 1;
            const isFirst = index === 0;
            
            return `
                <li class="inline-flex items-center">
                    ${index > 0 ? `
                        <svg class="w-5 h-5 text-gray-400 mx-1" fill="currentColor" viewBox="0 0 20 20">
                            <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"></path>
                        </svg>
                    ` : ''}
                    ${isFirst ? `
                        <svg class="w-4 h-4 text-gray-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"></path>
                        </svg>
                    ` : ''}
                    ${isLast ? `
                        <span class="text-sm font-medium text-gray-700">${this.escapeHtml(crumb.name)}</span>
                    ` : `
                        <a href="#" 
                           class="text-sm font-medium text-blue-600 hover:text-blue-800 hover:underline transition-colors"
                           data-path="${this.escapeHtml(crumb.path)}"
                           onclick="window.fileExplorerInstance.handleBreadcrumbClick(event, '${this.escapeHtml(crumb.path)}')">
                            ${this.escapeHtml(crumb.name)}
                        </a>
                    `}
                </li>
            `;
        }).join('');

        container.innerHTML = `
            <nav class="flex items-center overflow-x-auto" aria-label="Breadcrumb">
                <ol class="inline-flex items-center space-x-1 whitespace-nowrap">
                    ${breadcrumbHtml}
                </ol>
            </nav>
        `;
    }

    /**
     * Handle breadcrumb click - navigate to breadcrumb path
     */
    handleBreadcrumbClick(event, path) {
        event.preventDefault();
        
        // Load the node at the breadcrumb path
        this.loadNode(path);
        
        // Ensure parent nodes are expanded in tree
        this.expandPathInTree(path);
    }

    /**
     * Expand all parent nodes in tree for a given path
     */
    expandPathInTree(path) {
        if (!this.expandedNodes) {
            this.expandedNodes = new Set();
        }

        // Find all parent paths and expand them
        this.breadcrumbs.forEach(crumb => {
            if (path.startsWith(crumb.path) || crumb.path === path) {
                this.expandedNodes.add(crumb.path);
            }
        });

        // Re-render tree to show expanded state
        this.renderTree(this.currentNode);
    }

    /**
     * Render tree view with hierarchical structure
     */
    renderTree(node) {
        const container = document.getElementById('fileExplorerTree');
        if (!container) return;

        if (!node || !node.children || node.children.length === 0) {
            container.innerHTML = `
                <div class="text-sm text-gray-500 py-4 text-center">
                    No folders available
                </div>
            `;
            return;
        }

        // Initialize expanded nodes set if not exists
        if (!this.expandedNodes) {
            this.expandedNodes = new Set();
        }

        // Render the tree starting from root
        const treeHtml = this.renderTreeNodes(node.children, 0);
        container.innerHTML = treeHtml;
    }

    /**
     * Render tree nodes recursively with lazy loading support
     */
    renderTreeNodes(nodes, level) {
        if (!nodes || nodes.length === 0) return '';

        let html = '';
        
        // Filter out files, only show folders in tree
        const folders = nodes.filter(node => node.type !== 'FILE');

        folders.forEach(node => {
            const hasChildren = node.children && node.children.length > 0;
            const isExpanded = this.expandedNodes.has(node.path);
            const indent = level * 16;
            const isSelected = this.currentPath === node.path;

            html += `
                <div class="tree-node" data-path="${this.escapeHtml(node.path)}">
                    <div class="flex items-center py-1.5 px-2 hover:bg-gray-100 rounded cursor-pointer ${isSelected ? 'bg-blue-50 border-l-2 border-blue-500' : ''}"
                         style="padding-left: ${indent + 8}px;">
                        ${hasChildren ? `
                            <button 
                                class="expand-toggle w-4 h-4 mr-1 flex items-center justify-center focus:outline-none"
                                onclick="event.stopPropagation(); window.fileExplorerInstance.toggleNode('${this.escapeHtml(node.path)}')">
                                <svg class="w-4 h-4 text-gray-500 transition-transform ${isExpanded ? 'transform rotate-90' : ''}" 
                                     fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                                </svg>
                            </button>
                        ` : `
                            <span class="w-4 h-4 mr-1"></span>
                        `}
                        <div class="flex items-center flex-1" onclick="window.fileExplorerInstance.handleNodeClick('${this.escapeHtml(node.path)}')">
                            <svg class="w-4 h-4 ${isExpanded ? 'text-yellow-600' : 'text-yellow-500'} mr-2" fill="currentColor" viewBox="0 0 20 20">
                                ${isExpanded ? `
                                    <path fill-rule="evenodd" d="M2 6a2 2 0 012-2h4l2 2h4a2 2 0 012 2v1H8a2 2 0 00-2 2v5a2 2 0 01-2 2H2V6z" clip-rule="evenodd"></path>
                                ` : `
                                    <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"></path>
                                `}
                            </svg>
                            <span class="text-sm ${isSelected ? 'text-blue-700 font-medium' : 'text-gray-700'}">${this.escapeHtml(node.name)}</span>
                        </div>
                    </div>
                    ${isExpanded && hasChildren ? `
                        <div class="tree-children">
                            ${this.renderTreeNodes(node.children, level + 1)}
                        </div>
                    ` : ''}
                </div>
            `;
        });

        return html;
    }

    /**
     * Toggle node expansion (lazy loading)
     */
    async toggleNode(path) {
        if (this.expandedNodes.has(path)) {
            // Collapse node
            this.expandedNodes.delete(path);
        } else {
            // Expand node - lazy load if needed
            this.expandedNodes.add(path);
            
            // Check if we need to load children
            const node = this.findNodeByPath(this.currentNode, path);
            if (node && (!node.children || node.children.length === 0)) {
                try {
                    const response = await fileExplorer.getNode(path);
                    const loadedNode = response.data || response;
                    
                    // Update the node with loaded children
                    if (node && loadedNode.children) {
                        node.children = loadedNode.children;
                    }
                } catch (error) {
                    console.error('Error loading node children:', error);
                    showToast('Failed to load folder contents', 'error');
                    this.expandedNodes.delete(path);
                }
            }
        }
        
        // Re-render the tree
        this.renderTree(this.currentNode);
    }

    /**
     * Find a node by path in the tree
     */
    findNodeByPath(node, path) {
        if (!node) return null;
        if (node.path === path) return node;
        
        if (node.children) {
            for (const child of node.children) {
                const found = this.findNodeByPath(child, path);
                if (found) return found;
            }
        }
        
        return null;
    }

    /**
     * Handle tree node click - navigate to node
     */
    handleNodeClick(path) {
        this.loadNode(path);
        
        // Ensure the node is expanded
        if (!this.expandedNodes.has(path)) {
            this.expandedNodes.add(path);
            this.renderTree(this.currentNode);
        }
    }

    /**
     * Render file list with table format
     */
    renderFileList(node) {
        const container = document.getElementById('fileExplorerFileList');
        if (!container) return;

        if (!node) {
            container.innerHTML = `
                <div class="text-sm text-gray-500 py-8 text-center">
                    No data available
                </div>
            `;
            return;
        }

        // Show folders and files
        const items = node.children || [];
        const folders = items.filter(item => item.type !== 'FILE');
        const files = items.filter(item => item.type === 'FILE');

        if (items.length === 0) {
            container.innerHTML = `
                <div class="text-sm text-gray-500 py-8 text-center">
                    <svg class="w-12 h-12 mx-auto text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                    </svg>
                    <p>This folder is empty</p>
                </div>
            `;
            return;
        }

        let html = '';

        // Render folders first (card view)
        if (folders.length > 0) {
            html += '<div class="mb-4"><h4 class="text-xs font-semibold text-gray-500 uppercase mb-2">Folders</h4>';
            html += '<div class="grid grid-cols-1 gap-2">';
            
            folders.forEach(folder => {
                html += `
                    <div class="flex items-center justify-between p-3 hover:bg-gray-50 rounded-lg border border-gray-200 cursor-pointer transition-colors"
                         onclick="window.fileExplorerInstance.handleNodeClick('${this.escapeHtml(folder.path)}')">
                        <div class="flex items-center">
                            <svg class="w-5 h-5 text-yellow-500 mr-3 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"></path>
                            </svg>
                            <div>
                                <div class="text-sm font-medium text-gray-900">${this.escapeHtml(folder.name)}</div>
                                <div class="text-xs text-gray-500">${this.formatNodeType(folder.type)}</div>
                            </div>
                        </div>
                        <svg class="w-5 h-5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                        </svg>
                    </div>
                `;
            });
            
            html += '</div></div>';
        }

        // Render files (table view)
        if (files.length > 0) {
            html += '<div><h4 class="text-xs font-semibold text-gray-500 uppercase mb-2">Files</h4>';
            html += `
                <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-gray-200 border border-gray-200 rounded-lg">
                        <thead class="bg-gray-50">
                            <tr>
                                <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Name
                                </th>
                                <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Size
                                </th>
                                <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Uploaded
                                </th>
                                <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Uploader
                                </th>
                                <th scope="col" class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Actions
                                </th>
                            </tr>
                        </thead>
                        <tbody class="bg-white divide-y divide-gray-200">
            `;
            
            files.forEach(file => {
                const metadata = file.metadata || {};
                const canDownload = file.canRead !== false;
                const canView = file.canRead !== false;
                
                html += `
                    <tr class="hover:bg-gray-50 transition-colors">
                        <td class="px-4 py-3 whitespace-nowrap">
                            <div class="flex items-center">
                                <svg class="w-5 h-5 text-blue-500 mr-2 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                    <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"></path>
                                </svg>
                                <span class="text-sm font-medium text-gray-900">${this.escapeHtml(file.name)}</span>
                            </div>
                        </td>
                        <td class="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                            ${metadata.fileSize ? this.formatFileSize(metadata.fileSize) : '-'}
                        </td>
                        <td class="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                            ${metadata.uploadedAt ? formatDate(metadata.uploadedAt) : '-'}
                        </td>
                        <td class="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                            ${metadata.uploaderName ? this.escapeHtml(metadata.uploaderName) : '-'}
                        </td>
                        <td class="px-4 py-3 whitespace-nowrap text-right text-sm font-medium">
                            <div class="flex items-center justify-end space-x-2">
                                ${canView ? `
                                    <button 
                                        onclick="window.fileExplorerInstance.handleFileView(${metadata.fileId})"
                                        class="text-gray-600 hover:text-gray-900 transition-colors"
                                        title="View file details"
                                    >
                                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                                        </svg>
                                    </button>
                                ` : ''}
                                ${canDownload ? `
                                    <button 
                                        onclick="window.fileExplorerInstance.handleFileDownload(${metadata.fileId})"
                                        class="text-blue-600 hover:text-blue-800 transition-colors"
                                        title="Download file"
                                    >
                                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                                        </svg>
                                    </button>
                                ` : `
                                    <span class="text-gray-400 text-xs">No access</span>
                                `}
                            </div>
                        </td>
                    </tr>
                `;
            });
            
            html += `
                        </tbody>
                    </table>
                </div>
            </div>
            `;
        }

        container.innerHTML = html;
    }

    /**
     * Handle file view - show file details
     */
    async handleFileView(fileId) {
        try {
            const response = await fileExplorer.getFileMetadata(fileId);
            const file = response.data || response;
            
            const metadata = file.metadata || {};
            const content = `
                <div class="space-y-3">
                    <div>
                        <label class="text-sm font-medium text-gray-700">File Name:</label>
                        <p class="text-sm text-gray-900 mt-1">${this.escapeHtml(file.name || metadata.originalFilename || 'Unknown')}</p>
                    </div>
                    <div>
                        <label class="text-sm font-medium text-gray-700">Size:</label>
                        <p class="text-sm text-gray-900 mt-1">${metadata.fileSize ? this.formatFileSize(metadata.fileSize) : 'Unknown'}</p>
                    </div>
                    <div>
                        <label class="text-sm font-medium text-gray-700">Type:</label>
                        <p class="text-sm text-gray-900 mt-1">${metadata.fileType || 'Unknown'}</p>
                    </div>
                    <div>
                        <label class="text-sm font-medium text-gray-700">Uploaded:</label>
                        <p class="text-sm text-gray-900 mt-1">${metadata.uploadedAt ? formatDate(metadata.uploadedAt) : 'Unknown'}</p>
                    </div>
                    <div>
                        <label class="text-sm font-medium text-gray-700">Uploaded By:</label>
                        <p class="text-sm text-gray-900 mt-1">${metadata.uploaderName || 'Unknown'}</p>
                    </div>
                    ${metadata.notes ? `
                        <div>
                            <label class="text-sm font-medium text-gray-700">Notes:</label>
                            <p class="text-sm text-gray-900 mt-1">${this.escapeHtml(metadata.notes)}</p>
                        </div>
                    ` : ''}
                </div>
            `;
            
            showModal('File Details', content, {
                size: 'md',
                buttons: [
                    {
                        text: 'Download',
                        className: 'bg-blue-600 text-white hover:bg-blue-700',
                        action: 'download',
                        onClick: (close) => {
                            this.handleFileDownload(fileId);
                            close();
                        }
                    },
                    {
                        text: 'Close',
                        className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                        action: 'close',
                        onClick: (close) => close()
                    }
                ]
            });
        } catch (error) {
            console.error('Error loading file details:', error);
            showToast('Failed to load file details', 'error');
        }
    }

    /**
     * Handle file download with progress feedback
     */
    async handleFileDownload(fileId) {
        // Show loading toast
        const loadingToast = showToast('Preparing download...', 'info', 30000);
        
        try {
            const response = await fileExplorer.downloadFile(fileId);
            
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || 'Failed to download file');
            }

            // Get filename from Content-Disposition header
            const contentDisposition = response.headers.get('Content-Disposition');
            let filename = 'download';
            
            if (contentDisposition) {
                // Try to extract filename from Content-Disposition header
                const filenameMatch = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(contentDisposition);
                if (filenameMatch != null && filenameMatch[1]) {
                    filename = filenameMatch[1].replace(/['"]/g, '');
                    // Decode URI component if needed
                    try {
                        filename = decodeURIComponent(filename);
                    } catch (e) {
                        // Keep original filename if decode fails
                    }
                }
            }

            // Get the blob
            const blob = await response.blob();
            
            // Create download link
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            a.download = filename;
            
            // Trigger download
            document.body.appendChild(a);
            a.click();
            
            // Cleanup
            setTimeout(() => {
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            }, 100);

            // Remove loading toast and show success
            if (loadingToast && loadingToast.remove) {
                loadingToast.remove();
            }
            showToast(`File "${filename}" downloaded successfully`, 'success');
            
        } catch (error) {
            console.error('Error downloading file:', error);
            
            // Remove loading toast
            if (loadingToast && loadingToast.remove) {
                loadingToast.remove();
            }
            
            // Show error message
            const errorMessage = error.message || 'Failed to download file';
            showToast(errorMessage, 'error');
            
            // If it's a permission error, show more details
            if (error.message && error.message.includes('403')) {
                showToast('You do not have permission to download this file', 'error');
            } else if (error.message && error.message.includes('404')) {
                showToast('File not found', 'error');
            }
        }
    }

    /**
     * Render error state
     */
    renderError(message) {
        const treeContainer = document.getElementById('fileExplorerTree');
        const fileListContainer = document.getElementById('fileExplorerFileList');

        if (treeContainer) {
            treeContainer.innerHTML = `
                <div class="text-sm text-red-600 py-4 text-center">
                    ${this.escapeHtml(message)}
                </div>
            `;
        }

        if (fileListContainer) {
            fileListContainer.innerHTML = `
                <div class="text-sm text-red-600 py-8 text-center">
                    ${this.escapeHtml(message)}
                </div>
            `;
        }
    }

    /**
     * Format node type for display
     */
    formatNodeType(type) {
        const types = {
            'YEAR': 'Academic Year',
            'SEMESTER': 'Semester',
            'PROFESSOR': 'Professor',
            'COURSE': 'Course',
            'DOCUMENT_TYPE': 'Document Type',
            'FILE': 'File'
        };
        return types[type] || type;
    }

    /**
     * Format file size
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    }

    /**
     * Escape HTML to prevent XSS
     */
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Export for use in other modules
export default FileExplorer;
