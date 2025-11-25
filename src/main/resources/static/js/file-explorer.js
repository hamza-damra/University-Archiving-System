/**
 * File Explorer Component
 * Reusable component for hierarchical file navigation
 * 
 * ============================================================================
 * MASTER DESIGN REFERENCE: Professor Dashboard File Explorer
 * ============================================================================
 * 
 * This component implements the CANONICAL FILE EXPLORER LAYOUT based on the
 * Professor Dashboard File Explorer design (prof-dashboard.html + prof.js).
 * 
 * All dashboards (Professor, HOD, Deanship) use this component with role-specific
 * configuration options while maintaining visual consistency.
 * 
 * DESIGN AUTHORITY:
 * - HTML Structure: Defined in prof-dashboard.html File Explorer tab
 * - Rendering Logic: Defined in prof.js renderFileExplorer() function
 * - Tailwind Classes: Specified in Professor Dashboard implementation
 * - Role Labels: Defined in prof.js with "Your Folder" and "Read Only" badges
 * 
 * KEY DESIGN ELEMENTS FROM PROFESSOR DASHBOARD:
 * 
 * 1. FOLDER CARDS:
 *    - Wide blue cards: bg-blue-50, border-blue-200, hover:bg-blue-100
 *    - Folder icon: w-7 h-7 text-blue-600
 *    - Name: text-sm font-semibold text-gray-900
 *    - Arrow: w-5 h-5 with group-hover:translate-x-1 transition
 *    - Padding: p-4
 *    - Border radius: rounded-lg
 * 
 * 2. FILE ITEMS:
 *    - White cards: bg-white border border-gray-200 hover:shadow-lg
 *    - Icon container: w-12 h-12 bg-gray-50 rounded-lg
 *    - Metadata badges: bg-gray-100 text-gray-700 px-2 py-0.5 rounded text-xs
 *    - Download button: bg-blue-600 hover:bg-blue-700 p-2.5 rounded-lg shadow-sm
 * 
 * 3. ROLE-SPECIFIC LABELS:
 *    - "Your Folder": bg-blue-100 text-blue-800 (Professor ownership)
 *    - "Read Only": bg-gray-100 text-gray-600 (No write permission)
 *    - Professor name: bg-purple-100 text-purple-700 (Deanship view)
 * 
 * 4. BREADCRUMB NAVIGATION:
 *    - Home icon: w-4 h-4 (house icon)
 *    - Separators: w-5 h-5 text-gray-400 (chevron right)
 *    - Links: text-blue-600 hover:text-blue-800 hover:underline
 *    - Current: text-gray-700 font-medium
 * 
 * 5. EMPTY STATES:
 *    - Centered: text-center py-8
 *    - Icon: w-12 h-12 text-gray-300
 *    - Text: text-sm text-gray-500
 * 
 * CONFIGURATION OPTIONS:
 * - role: 'PROFESSOR' | 'HOD' | 'DEANSHIP'
 * - readOnly: boolean
 * - showOwnershipLabels: boolean (Professor)
 * - showDepartmentContext: boolean (HOD)
 * - headerMessage: string (HOD)
 * - showProfessorLabels: boolean (Deanship)
 * - showAllDepartments: boolean (Deanship)
 * 
 * USAGE EXAMPLES: See constructor JSDoc below
 * 
 * ============================================================================
 */

import { fileExplorer } from './api.js';
import { showToast, showModal, formatDate } from './ui.js';
import { fileExplorerState } from './file-explorer-state.js';

/**
 * FileExplorer class
 * Manages the file explorer UI and interactions
 * 
 * @example Professor Dashboard Configuration
 * new FileExplorer('container', {
 *   role: 'PROFESSOR',
 *   showOwnershipLabels: true,
 *   readOnly: false
 * });
 * 
 * @example HOD Dashboard Configuration
 * new FileExplorer('container', {
 *   role: 'HOD',
 *   showDepartmentContext: true,
 *   headerMessage: 'Browse department files (Read-only)',
 *   readOnly: true
 * });
 * 
 * @example Deanship Dashboard Configuration
 * new FileExplorer('container', {
 *   role: 'DEANSHIP',
 *   showAllDepartments: true,
 *   showProfessorLabels: true,
 *   readOnly: true
 * });
 */
export class FileExplorer {
    /**
     * Create a new FileExplorer instance
     * 
     * @param {string} containerId - The ID of the DOM element to render the File Explorer into
     * @param {Object} options - Configuration options for role-specific behavior
     * 
     * @param {boolean} [options.readOnly=false] - Whether the explorer is in read-only mode (no upload/edit actions)
     * @param {Function} [options.onFileClick=null] - Callback function when a file is clicked
     * @param {Function} [options.onNodeExpand=null] - Callback function when a tree node is expanded
     * 
     * @param {string} [options.role='PROFESSOR'] - User role: 'PROFESSOR', 'HOD', or 'DEANSHIP'
     * @param {boolean} [options.showOwnershipLabels=false] - Show "Your Folder" labels for professors (PROFESSOR role)
     * @param {boolean} [options.showDepartmentContext=false] - Show department context information (HOD role)
     * @param {string} [options.headerMessage=null] - Header message to display above breadcrumbs (e.g., "Browse department files (Read-only)")
     * @param {boolean} [options.showProfessorLabels=false] - Show professor name labels on folders (DEANSHIP role)
     * @param {boolean} [options.showAllDepartments=false] - Allow viewing all departments (DEANSHIP role)
     * @param {boolean} [options.hideTree=false] - Hide the tree view panel and use single-column layout (DEANSHIP role)
     *                                             When true: Uses grid-cols-1 layout with only folder cards and file list
     *                                             When false: Uses grid-cols-3 layout with tree panel (1/3) and file list (2/3)
     *                                             This simplifies the UI for roles that don't need hierarchical tree navigation
     * 
     * @throws {Error} If the container element with the specified ID is not found
     * 
     * @example
     * // Professor Dashboard - Full access with ownership labels
     * const profExplorer = new FileExplorer('fileExplorerContainer', {
     *   role: 'PROFESSOR',
     *   showOwnershipLabels: true,
     *   readOnly: false
     * });
     * 
     * @example
     * // HOD Dashboard - Read-only with department context
     * const hodExplorer = new FileExplorer('hodFileExplorer', {
     *   role: 'HOD',
     *   showDepartmentContext: true,
     *   headerMessage: 'Browse department files (Read-only)',
     *   readOnly: true
     * });
     * 
     * @example
     * // Deanship Dashboard - Read-only with professor labels and no tree view
     * const deanshipExplorer = new FileExplorer('deanshipFileExplorer', {
     *   role: 'DEANSHIP',
     *   showAllDepartments: true,
     *   showProfessorLabels: true,
     *   readOnly: true,
     *   hideTree: true
     * });
     */
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        if (!this.container) {
            throw new Error(`Container element with id "${containerId}" not found`);
        }

        // Role-specific configuration options
        this.options = {
            // Core options
            readOnly: options.readOnly || false,
            onFileClick: options.onFileClick || null,
            onNodeExpand: options.onNodeExpand || null,

            // Role-specific options
            role: options.role || 'PROFESSOR', // 'PROFESSOR', 'HOD', 'DEANSHIP'
            showOwnershipLabels: options.showOwnershipLabels || false, // Show "Your Folder" labels for professors
            showDepartmentContext: options.showDepartmentContext || false, // Show department context for HOD
            headerMessage: options.headerMessage || null, // Header message (e.g., "Browse department files (Read-only)")
            showProfessorLabels: options.showProfessorLabels || false, // Show professor names on folders for Deanship
            showAllDepartments: options.showAllDepartments || false, // Allow viewing all departments (Deanship)
            
            // TREE VIEW HIDING OPTION (Requirements: 5.1, 5.5)
            // When true, hides the left-side tree panel and uses a single-column layout
            // This simplifies the UI for the Dean role, matching the Professor Dashboard design
            // Benefits:
            // - Cleaner, less cluttered interface
            // - More space for folder cards and file list
            // - Simpler navigation using only breadcrumbs and folder cards
            // - Consistent with Professor Dashboard UX
            hideTree: options.hideTree || false,

            ...options
        };

        this.currentPath = '';
        this.currentNode = null;
        this.treeRoot = null;
        this.breadcrumbs = [];

        // Subscribe to FileExplorerState changes
        this.unsubscribe = fileExplorerState.subscribe((state) => {
            this.onStateChange(state);
        });

        // Check if container already has structure (e.g. restored from storage)
        // If so, skip initial render to prevent flash
        if (!this.container.querySelector('#fileExplorerBreadcrumbs')) {
            this.render();
        }
    }

    /**
     * Handle state changes from FileExplorerState
     * 
     * Called whenever the centralized state changes.
     * Updates local component properties and triggers UI re-renders.
     * 
     * CRITICAL FIX: When state is reset (currentNode becomes null), this method
     * now properly clears the UI to prevent showing stale content from previous
     * filter selections.
     * 
     * @param {Object} state - The new state from FileExplorerState
     * @returns {void}
     */
    onStateChange(state) {
        // Update local properties from state
        const hasChanges =
            this.currentPath !== state.currentPath ||
            this.treeRoot !== state.treeRoot ||
            this.currentNode !== state.currentNode;

        if (hasChanges) {
            this.currentPath = state.currentPath;
            this.treeRoot = state.treeRoot;
            this.currentNode = state.currentNode;
            this.breadcrumbs = state.breadcrumbs;
            this.expandedNodes = state.expandedNodes;

            // Handle loading states
            if (state.isLoading) {
                this.showLoading();
            } else if (state.isTreeLoading) {
                this.showTreeLoading();
            } else if (state.isFileListLoading) {
                this.showFileListLoading();
            } else {
                // CRITICAL FIX: Handle null state (after reset) by clearing UI
                // This prevents showing stale content when filters change
                if (this.treeRoot === null && this.currentNode === null) {
                    // State was reset - clear both panels
                    const treeContainer = document.getElementById('fileExplorerTree');
                    const fileListContainer = document.getElementById('fileExplorerFileList');
                    
                    if (treeContainer) {
                        treeContainer.innerHTML = this.renderEmptyState('Loading...', 'info');
                    }
                    if (fileListContainer) {
                        fileListContainer.innerHTML = this.renderEmptyState('Loading...', 'info');
                    }
                } else {
                    // Render updated content
                    if (this.treeRoot) {
                        this.renderTree(this.treeRoot);
                    }
                    // CRITICAL FIX: Don't auto-render file list at root level
                    // Check if we're at root by comparing currentNode with treeRoot
                    // This prevents overriding the empty state set by loadRoot
                    const isAtRoot = this.currentNode === this.treeRoot;
                    if (this.currentNode && !isAtRoot) {
                        this.renderFileList(this.currentNode);
                    }
                    // Always render breadcrumbs (even if empty array) to show proper state
                    this.renderBreadcrumbs();
                }
            }

            // Handle error state
            if (state.error) {
                this.renderError(state.error);
            }
        }
    }

    /**
     * Render the file explorer UI with role-specific elements
     * 
     * Creates the complete File Explorer HTML structure including:
     * - Optional header message (for HOD role)
     * - Breadcrumb navigation area
     * - Tree view panel (left side) - optional, can be hidden with hideTree option
     * - File list panel (right side)
     * 
     * The layout follows the Professor Dashboard master design with a 1/3 - 2/3 grid split,
     * or a single-column layout when hideTree is true.
     * 
     * @returns {void}
     */
    render() {
        // Build header message HTML if provided
        const headerMessageHtml = this.options.headerMessage ? `
            <div class="bg-blue-50 border-b border-blue-200 px-4 py-2">
                <p class="text-sm text-gray-600">${this.escapeHtml(this.options.headerMessage)}</p>
            </div>
        ` : '';

        // LAYOUT CONFIGURATION BASED ON hideTree OPTION
        // When hideTree is true (Dean role):
        //   - Uses single-column layout (grid-cols-1)
        //   - File list takes full width
        //   - Navigation via breadcrumbs and folder cards only
        // When hideTree is false (Professor/HOD roles):
        //   - Uses two-column layout (grid-cols-3: 1/3 tree, 2/3 file list)
        //   - Tree panel shows hierarchical structure
        //   - File list spans 2 columns (md:col-span-2)
        const layoutClass = this.options.hideTree ? 'grid-cols-1' : 'grid-cols-1 md:grid-cols-3';
        const fileListColSpan = this.options.hideTree ? '' : 'md:col-span-2';
        
        // Conditionally render tree view panel
        // Empty string when hideTree is true, full tree HTML when false
        const treeViewHtml = this.options.hideTree ? '' : `
            <!-- Tree View -->
            <div class="md:col-span-1 bg-white border border-gray-200 rounded-lg p-4">
                <h3 class="text-sm font-semibold text-gray-700 mb-3">Folder Structure</h3>
                <div id="fileExplorerTree" class="space-y-1">
                    ${this.renderNoSemesterSelected()}
                </div>
            </div>
        `;

        this.container.innerHTML = `
            <div class="file-explorer">
                ${headerMessageHtml}
                
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
                <div class="grid ${layoutClass} gap-4 p-4">
                    ${treeViewHtml}

                    <!-- File List -->
                    <div class="${fileListColSpan} bg-white border border-gray-200 rounded-lg p-4">
                        <h3 class="text-sm font-semibold text-gray-700 mb-3">Files</h3>
                        <div id="fileExplorerFileList" class="overflow-x-auto">
                            ${this.renderNoSemesterSelected()}
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Load root node for a semester
     * 
     * Fetches the root-level folder structure for a specific academic year and semester.
     * This is typically called when the user selects a semester from the dropdown.
     * 
     * Shows loading state while fetching, then renders both tree view and file list.
     * On error, displays error state with user-friendly message.
     * 
     * CRITICAL FIX: Clears both folder structure AND files section immediately to prevent
     * showing stale content from previous filter selections.
     * 
     * @param {number} academicYearId - The ID of the academic year to load
     * @param {number} semesterId - The ID of the semester to load
     * @param {boolean} [isBackground=false] - If true, suppresses the loading skeleton (useful for background updates)
     * @returns {Promise<void>}
     * 
     * @example
     * // Load root for academic year 2024-2025, first semester
     * await fileExplorer.loadRoot(1, 1);
     */
    async loadRoot(academicYearId, semesterId, isBackground = false) {
        // CRITICAL FIX: Clear BOTH folder structure and files section immediately
        // This prevents showing stale content from previous filter selections
        const treeContainer = document.getElementById('fileExplorerTree');
        const fileListContainer = document.getElementById('fileExplorerFileList');
        
        if (treeContainer) {
            treeContainer.innerHTML = this.renderEmptyState('Loading folders...', 'info');
        }
        if (fileListContainer) {
            fileListContainer.innerHTML = this.renderEmptyState('Loading files...', 'info');
        }

        // CRITICAL FIX: Clear breadcrumbs immediately to prevent showing stale path
        this.breadcrumbs = [];
        fileExplorerState.setBreadcrumbs([]);
        this.renderBreadcrumbs();

        // Show loading state only if not a background update
        if (!isBackground) {
            fileExplorerState.setLoading(true);
        }

        try {
            const response = await fileExplorer.getRoot(academicYearId, semesterId);
            this.treeRoot = response.data || response;

            // Handle empty or null response
            if (!this.treeRoot) {
                this.renderEmptyDataState();
                this.breadcrumbs = [];
                fileExplorerState.setBreadcrumbs([]);
                this.renderBreadcrumbs();
                fileExplorerState.setLoading(false);
                return;
            }

            this.currentNode = this.treeRoot;
            // CRITICAL FIX: Reset currentPath to empty string at root level
            // This ensures we're truly at root, not preserving old navigation state
            this.currentPath = '';

            // Update state with loaded data
            fileExplorerState.setTreeRoot(this.treeRoot);
            fileExplorerState.setCurrentNode(this.currentNode, this.currentPath);

            // CRITICAL FIX: Don't load breadcrumbs at root level
            // Breadcrumbs should only be loaded when navigating into folders
            // At root, we want to show "Select a folder to navigate"
            // await this.loadBreadcrumbs(this.currentPath);

            // Check if we have an empty response (no children)
            const hasData = this.treeRoot && this.treeRoot.children && this.treeRoot.children.length > 0;
            
            // CRITICAL FIX: Also check for files in the root node
            const hasFiles = this.treeRoot && this.treeRoot.files && this.treeRoot.files.length > 0;

            if (!hasData && !hasFiles) {
                // Show friendly empty state instead of error
                this.renderEmptyDataState();
            } else {
                // Render tree view (if not hidden)
                this.renderTree(this.treeRoot);
                
                // CRITICAL FIX: Show empty state in files section after filter change
                // User must click on a folder to view its contents
                // This prevents confusion and makes it clear that filters have been applied
                const fileListContainer = document.getElementById('fileExplorerFileList');
                if (fileListContainer) {
                    fileListContainer.innerHTML = this.renderEmptyState(
                        'Select a folder to view its contents',
                        'folder'
                    );
                }
            }

            // Clear loading state
            fileExplorerState.setLoading(false);
        } catch (error) {
            console.error('Error loading file explorer root:', error);

            // Distinguish between different error types
            const isNotFoundError = error.message && error.message.toLowerCase().includes('not found');

            if (isNotFoundError) {
                // This might be an empty database scenario
                if (!isBackground) {
                    this.renderEmptyDataState();
                }
            } else {
                // Actual error occurred
                fileExplorerState.setError(error.message || 'Failed to load file explorer');
                if (!isBackground) {
                    showToast('Failed to load file explorer', 'error');
                    this.renderError('Failed to load file explorer', 'Please try again or select a different semester');
                }
            }

            // Clear loading state
            fileExplorerState.setLoading(false);
        }
    }

    /**
     * Load a specific node by path
     * 
     * Fetches the contents of a specific folder in the hierarchy.
     * Called when user clicks on a folder card or tree node.
     * 
     * Shows loading state in file list only (tree remains visible).
     * Updates breadcrumbs and renders the folder contents.
     * 
     * @param {string} path - The full path to the node (e.g., "2024-2025/first/PBUS001")
     * @returns {Promise<void>}
     * 
     * @example
     * // Navigate to a specific course folder
     * await fileExplorer.loadNode("2024-2025/first/PBUS001");
     */
    async loadNode(path) {
        // Show loading state in file list only (tree stays visible)
        fileExplorerState.setFileListLoading(true);

        try {
            const response = await fileExplorer.getNode(path);
            const newNode = response.data || response;
            this.currentNode = newNode;
            this.currentPath = path;

            // Update state with new current node
            fileExplorerState.setCurrentNode(this.currentNode, this.currentPath);

            // Update treeRoot with new data
            if (this.treeRoot) {
                const treeNode = this.findNodeByPath(this.treeRoot, path);
                if (treeNode) {
                    treeNode.children = newNode.children;
                }
            }

            // Auto-expand in tree if it has sub-folders
            const hasFolderChildren = newNode.children && newNode.children.some(c => c.type !== 'FILE');
            if (hasFolderChildren) {
                fileExplorerState.expandNode(path);
            }

            await this.loadBreadcrumbs(path);
            this.renderFileList(this.currentNode);
            this.renderTree(this.treeRoot);

            // Clear loading state
            fileExplorerState.setFileListLoading(false);
        } catch (error) {
            // Check if this is a "not found" error (404)
            const isNotFoundError = error.message && (
                error.message.toLowerCase().includes('not found') ||
                error.message.toLowerCase().includes('professor not found')
            );

            if (isNotFoundError) {
                // This is likely an empty folder or missing data, not a real error
                console.warn('Path not found (empty data):', path);

                // Show friendly empty state instead of error
                const container = document.getElementById('fileExplorerFileList');
                if (container) {
                    container.innerHTML = this.renderEmptyState(
                        'This folder has no content yet',
                        'folder'
                    );
                }
            } else {
                // Actual error occurred
                console.error('Error loading node:', error);
                fileExplorerState.setError(error.message || 'Failed to load folder');
                showToast('Failed to load folder', 'error');

                // Show error in file list using shared error state rendering
                const container = document.getElementById('fileExplorerFileList');
                if (container) {
                    container.innerHTML = this.renderErrorState('Failed to load folder', 'Please try again');
                }
            }

            // Clear loading state
            fileExplorerState.setFileListLoading(false);
        }
    }

    /**
     * Load breadcrumbs for current path
     * 
     * Fetches the breadcrumb trail from the API and renders it.
     * Breadcrumbs show the navigation path from root to current location.
     * 
     * If path is empty or null, clears breadcrumbs.
     * On error, silently clears breadcrumbs (non-critical failure).
     * 
     * @param {string} path - The current path to generate breadcrumbs for
     * @returns {Promise<void>}
     */
    async loadBreadcrumbs(path) {
        try {
            if (!path) {
                this.breadcrumbs = [];
                fileExplorerState.setBreadcrumbs([]);
                this.renderBreadcrumbs();
                return;
            }

            const response = await fileExplorer.getBreadcrumbs(path);
            this.breadcrumbs = response.data || response || [];
            fileExplorerState.setBreadcrumbs(this.breadcrumbs);
            this.renderBreadcrumbs();
        } catch (error) {
            console.error('Error loading breadcrumbs:', error);
            this.breadcrumbs = [];
            fileExplorerState.setBreadcrumbs([]);
            this.renderBreadcrumbs();
        }
    }

    /**
     * Render breadcrumbs with improved navigation
     * 
     * Displays the breadcrumb trail showing current location in the hierarchy.
     * 
     * Features:
     * - Home icon for the first breadcrumb
     * - Chevron separators between segments
     * - Clickable links for navigation (except current location)
     * - Current location shown in gray (non-clickable)
     * - Horizontal scrolling for long paths
     * 
     * Design follows Professor Dashboard: text-blue-600 for links, text-gray-700 for current.
     * 
     * @returns {void}
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

        // Back button logic
        let backButtonHtml = '';
        if (this.breadcrumbs.length > 1) {
            const parentPath = this.breadcrumbs[this.breadcrumbs.length - 2].path;
            backButtonHtml = `
                <button onclick="window.fileExplorerInstance.handleBreadcrumbClick(event, '${this.escapeHtml(parentPath)}')" 
                        class="mr-2 p-1 rounded-full hover:bg-gray-200 text-gray-500 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500"
                        title="Go back">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path>
                    </svg>
                </button>
            `;
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
            <div class="flex items-center w-full">
                ${backButtonHtml}
                <nav class="flex items-center overflow-x-auto" aria-label="Breadcrumb">
                    <ol class="inline-flex items-center space-x-1 whitespace-nowrap">
                        ${breadcrumbHtml}
                    </ol>
                </nav>
            </div>
        `;
    }

    /**
     * Handle breadcrumb click - navigate to breadcrumb path
     * 
     * Called when user clicks on a breadcrumb segment.
     * Navigates to that level in the hierarchy and expands the path in tree view.
     * 
     * @param {Event} event - The click event
     * @param {string} path - The path to navigate to
     * @returns {void}
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
     * 
     * Ensures all parent folders in the tree are expanded to show the current path.
     * Used when navigating via breadcrumbs to keep tree view in sync.
     * 
     * @param {string} path - The path to expand in the tree
     * @returns {void}
     */
    expandPathInTree(path) {
        // Find all parent paths and expand them
        this.breadcrumbs.forEach(crumb => {
            if (path.startsWith(crumb.path) || crumb.path === path) {
                fileExplorerState.expandNode(crumb.path);
            }
        });

        // Re-render tree to show expanded state
        this.renderTree(this.treeRoot);
    }

    /**
     * Render tree view with hierarchical structure
     * 
     * Displays the folder hierarchy in the left panel.
     * Only shows folders (files are excluded from tree view).
     * 
     * Features:
     * - Expandable/collapsible nodes
     * - Visual indentation for hierarchy levels
     * - Selected node highlighting (blue background)
     * - Folder icons (open/closed states)
     * 
     * TREE VIEW HIDING:
     * When hideTree option is true, this method returns immediately without rendering.
     * This prevents unnecessary DOM manipulation and improves performance for roles
     * that don't use the tree view (e.g., Dean role).
     * 
     * @param {Object} node - The root node to render from
     * @returns {void}
     */
    renderTree(node) {
        // Skip rendering if tree is hidden (Requirements: 5.1)
        // Early return prevents DOM manipulation when tree panel is not displayed
        if (this.options.hideTree) {
            return;
        }

        const container = document.getElementById('fileExplorerTree');
        if (!container) return;

        // Clear the tree completely if no node
        if (!node) {
            container.innerHTML = this.renderEmptyState('No data available', 'folder');
            return;
        }

        if (!node.children || node.children.length === 0) {
            container.innerHTML = this.renderEmptyState('No folders available', 'folder');
            return;
        }

        // Get expanded nodes from state
        this.expandedNodes = fileExplorerState.getExpandedNodes();

        // Render the tree starting from root
        const treeHtml = this.renderTreeNodes(node.children, 0);
        container.innerHTML = treeHtml;
    }

    /**
     * Render tree nodes recursively with lazy loading support
     * 
     * Recursively renders tree nodes with proper indentation and expand/collapse controls.
     * Filters out files (only folders shown in tree).
     * 
     * @param {Array<Object>} nodes - Array of child nodes to render
     * @param {number} level - Current depth level (0 = root, used for indentation)
     * @returns {string} HTML string for the tree nodes
     */
    renderTreeNodes(nodes, level) {
        if (!nodes || nodes.length === 0) return '';

        let html = '';

        // Filter out files, only show folders in tree
        const folders = nodes.filter(node => node.type !== 'FILE');

        folders.forEach(node => {
            const hasChildren = node.children && node.children.length > 0;
            const isExpanded = this.expandedNodes.has(node.path);
            const indent = level * 20;
            const isSelected = this.currentPath === node.path;

            html += `
                <div class="tree-node" data-path="${this.escapeHtml(node.path)}">
                    <div class="flex items-center py-2.5 px-3 hover:bg-gray-100 dark:hover:bg-gray-700 rounded cursor-pointer transition-colors ${isSelected ? 'bg-blue-100 dark:bg-blue-900 border-l-4 border-blue-600 dark:border-blue-400' : ''}"
                         style="padding-left: ${indent + 12}px;">
                        ${hasChildren ? `
                            <button 
                                class="expand-toggle w-5 h-5 mr-2 flex items-center justify-center focus:outline-none hover:bg-gray-200 dark:hover:bg-gray-600 rounded transition-colors"
                                onclick="event.stopPropagation(); window.fileExplorerInstance.toggleNode('${this.escapeHtml(node.path)}')">
                                <svg class="w-5 h-5 text-gray-600 dark:text-gray-400 transition-transform ${isExpanded ? 'transform rotate-90' : ''}" 
                                     fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                                </svg>
                            </button>
                        ` : `
                            <span class="w-5 h-5 mr-2"></span>
                        `}
                        <div class="flex items-center flex-1" onclick="window.fileExplorerInstance.handleNodeClick('${this.escapeHtml(node.path)}')">
                            <svg class="w-5 h-5 ${isExpanded ? 'text-yellow-600' : 'text-yellow-500'} mr-2" fill="currentColor" viewBox="0 0 20 20">
                                ${isExpanded ? `
                                    <path fill-rule="evenodd" d="M2 6a2 2 0 012-2h4l2 2h4a2 2 0 012 2v1H8a2 2 0 00-2 2v5a2 2 0 01-2 2H2V6z" clip-rule="evenodd"></path>
                                ` : `
                                    <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"></path>
                                `}
                            </svg>
                            <span class="text-base ${isSelected ? 'text-blue-800 dark:text-blue-100 font-semibold' : 'text-gray-800 dark:text-gray-200'}">${this.escapeHtml(node.name)}</span>
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
     * 
     * Expands or collapses a tree node. If expanding and children haven't been loaded,
     * fetches them from the API (lazy loading).
     * 
     * @param {string} path - The path of the node to toggle
     * @returns {Promise<void>}
     */
    async toggleNode(path) {
        const isExpanded = fileExplorerState.isNodeExpanded(path);

        if (isExpanded) {
            // Collapse node
            fileExplorerState.collapseNode(path);
        } else {
            // Expand node - lazy load if needed
            fileExplorerState.expandNode(path);

            // Check if we need to load children
            const node = this.findNodeByPath(this.treeRoot, path);
            if (node && (!node.children || node.children.length === 0)) {
                // Show loading indicator in tree while fetching
                this.renderTree(this.treeRoot);

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
                    fileExplorerState.collapseNode(path);
                }
            }
        }

        // Re-render the tree
        this.renderTree(this.treeRoot);
    }

    /**
     * Find a node by path in the tree
     * 
     * Recursively searches the tree structure to find a node with the given path.
     * Used for lazy loading and tree manipulation.
     * 
     * @param {Object} node - The node to start searching from
     * @param {string} path - The path to search for
     * @returns {Object|null} The found node or null if not found
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
     * 
     * Called when user clicks on a folder name in the tree view.
     * Loads the node contents and ensures it's expanded in the tree.
     * 
     * @param {string} path - The path of the clicked node
     * @returns {void}
     */
    handleNodeClick(path) {
        this.loadNode(path);
    }

    /**
     * Render file list with table format
     * 
     * Displays the contents of the current folder in the right panel.
     * 
     * Layout:
     * - Folders shown first as blue cards (wide format)
     * - Files shown below in a table format
     * - Role-specific labels added to folders based on configuration
     * 
     * Follows Professor Dashboard design:
     * - Folder cards: bg-blue-50, border-blue-200, hover:bg-blue-100
     * - File table: columns for Name, Size, Uploaded, Uploader, Actions
     * - Action buttons: View (gray), Download (blue)
     * 
     * @param {Object} node - The node whose contents to display
     * @returns {void}
     */
    renderFileList(node) {
        const container = document.getElementById('fileExplorerFileList');
        if (!container) return;

        // Clear the file list completely if no node
        if (!node) {
            container.innerHTML = this.renderEmptyState('No data available', 'info');
            return;
        }

        // Show folders and files
        const items = node.children || [];
        const folders = items.filter(item => item.type !== 'FILE');
        const filesFromChildren = items.filter(item => item.type === 'FILE');

        // Check for uploaded files in the files array (from Task 26 backend enhancement)
        const uploadedFiles = node.files || [];

        // Debug: Log file data structure
        if (uploadedFiles.length > 0) {
            console.log('📁 Files found:', uploadedFiles.length);
            console.log('📄 First file structure:', uploadedFiles[0]);
        }

        // Combine files from both sources
        const allFiles = [...filesFromChildren, ...uploadedFiles];

        if (items.length === 0 && allFiles.length === 0) {
            // Check if this is a professor folder (node.type === 'PROFESSOR')
            if (node.type === 'PROFESSOR') {
                container.innerHTML = `
                    <div class="text-center py-12">
                        <svg class="mx-auto h-16 w-16 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path>
                        </svg>
                        <h3 class="text-lg font-medium text-gray-900 mb-2">No Course Assignments</h3>
                        <p class="text-sm text-gray-500">This professor has no course assignments in the selected semester.</p>
                        ${this.options.role === 'DEANSHIP' ? `
                            <div class="mt-4 p-3 bg-blue-50 border border-blue-100 rounded-lg inline-block">
                                <p class="text-xs text-blue-700">
                                    <svg class="inline-block w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                    </svg>
                                    Use the Assignments tab to assign courses to this professor
                                </p>
                            </div>
                        ` : ''}
                    </div>
                `;
            } else if (node.canWrite && node.type === 'DOCUMENT_TYPE') {
                container.innerHTML = this.renderWritableEmptyState(node);
                this.initializeDragDrop(node);
            } else {
                container.innerHTML = this.renderEmptyState('This folder is empty', 'folder');
            }
            return;
        }

        let html = '';

        // Add upload button for writable document type folders (appears above file list)
        if (node.canWrite && node.type === 'DOCUMENT_TYPE') {
            html += this.renderUploadButton(node);
            this.initializeDragDrop(node);
        }

        // Render folders first (card view)
        if (folders.length > 0) {
            html += '<div class="mb-4"><h4 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase mb-2">Folders</h4>';
            html += '<div class="grid grid-cols-1 gap-2">';

            folders.forEach(folder => {
                // Generate role-specific labels
                const roleLabels = this.generateRoleSpecificLabels(folder);

                html += `
                    <div class="flex items-center justify-between p-5 bg-blue-50 dark:bg-gray-800 rounded-lg border border-blue-200 dark:border-gray-700 hover:bg-blue-100 dark:hover:bg-gray-700 cursor-pointer transition-all group"
                         onclick="window.fileExplorerInstance.handleNodeClick('${this.escapeHtml(folder.path)}')">
                        <div class="flex items-center space-x-4 flex-1">
                            <svg class="w-8 h-8 text-blue-600 dark:text-blue-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                            </svg>
                            <div class="flex-1">
                                <div class="flex items-center flex-wrap gap-2">
                                    <p class="text-base font-semibold text-gray-900 dark:text-gray-100">${this.escapeHtml(folder.name)}</p>
                                    ${roleLabels}
                                </div>
                                ${folder.metadata && folder.metadata.description ? `
                                    <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">${this.escapeHtml(folder.metadata.description)}</p>
                                ` : ''}
                            </div>
                        </div>
                        <svg class="w-6 h-6 text-gray-400 dark:text-gray-500 group-hover:text-gray-700 dark:group-hover:text-gray-300 group-hover:translate-x-1 transition-all flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                        </svg>
                    </div>
                `;
            });

            html += '</div></div>';
        }

        // Render files (table view)
        if (allFiles.length > 0) {
            html += '<div><h4 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase mb-2">Files</h4>';
            html += `
                <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700 border border-gray-200 dark:border-gray-700 rounded-lg">
                        <thead class="bg-gray-50 dark:bg-gray-800">
                            <tr>
                                <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                                    Name
                                </th>
                                <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                                    Size
                                </th>
                                <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                                    Uploaded
                                </th>
                                <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                                    Uploader
                                </th>
                                <th scope="col" class="px-4 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                                    Actions
                                </th>
                            </tr>
                        </thead>
                        <tbody class="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
            `;

            allFiles.forEach(file => {
                html += this.renderFileCard(file);
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
     * Render a single file card/row for display in the file list
     * 
     * Creates HTML for a file table row with icon, metadata, and action buttons.
     * Includes file name, size, upload date, uploader, and download/view buttons.
     * 
     * @param {Object} file - The file object to render
     * @returns {string} HTML string for the file table row
     * 
     * @example
     * const fileHtml = renderFileCard({
     *   name: 'lecture1.pdf',
     *   metadata: {
     *     fileId: 123,
     *     fileType: 'application/pdf',
     *     fileSize: 2621440,
     *     uploadedAt: '2024-01-15T10:30:00',
     *     uploaderName: 'Dr. Smith',
     *     notes: 'Chapter 1 lecture notes'
     *   },
     *   canRead: true
     * });
     */
    renderFileCard(file) {
        // Support both old format (file.metadata) and new format (direct properties from UploadedFileDTO)
        const fileId = file.id || (file.metadata && file.metadata.fileId);
        const fileName = file.originalFilename || file.name || 'Unknown';
        const fileSize = file.fileSize || (file.metadata && file.metadata.fileSize);
        const uploadedAt = file.uploadedAt || (file.metadata && file.metadata.uploadedAt);
        const uploaderName = file.uploaderName || (file.metadata && file.metadata.uploaderName);
        const notes = file.notes || (file.metadata && file.metadata.notes);
        const fileType = file.fileType || (file.metadata && file.metadata.fileType) || '';

        // Debug: Log extracted values
        console.log('🔍 Rendering file card:', {
            fileId,
            fileName,
            fileSize,
            uploadedAt,
            uploaderName,
            rawFile: file
        });

        const canDownload = file.canRead !== false;
        const canView = file.canRead !== false;
        const fileIconClass = this.getFileIconClass(fileType);

        return `
            <tr class="hover:bg-gray-50 dark:hover:bg-gray-700 transition-all group">
                <td class="px-4 py-3 whitespace-nowrap">
                    <div class="flex items-center">
                        <div class="file-icon-container w-14 h-14 flex items-center justify-center bg-gray-50 dark:bg-gray-700 rounded-lg mr-3 flex-shrink-0">
                            <svg class="w-7 h-7 ${fileIconClass}" fill="currentColor" viewBox="0 0 20 20">
                                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"></path>
                            </svg>
                        </div>
                        <div class="flex flex-col">
                            <span class="text-sm font-medium text-gray-900 dark:text-gray-100 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">${this.escapeHtml(fileName)}</span>
                            ${notes ? `
                                <span class="text-xs text-gray-500 dark:text-gray-400 mt-1" title="${this.escapeHtml(notes)}">
                                    ${this.escapeHtml(notes.length > 50 ? notes.substring(0, 50) + '...' : notes)}
                                </span>
                            ` : ''}
                        </div>
                    </div>
                </td>
                <td class="px-4 py-3 whitespace-nowrap">
                    <span class="file-metadata-badge inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300">
                        ${fileSize ? this.formatFileSize(fileSize) : '-'}
                    </span>
                </td>
                <td class="px-4 py-3 whitespace-nowrap">
                    <span class="file-metadata-badge inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300">
                        ${uploadedAt ? formatDate(uploadedAt) : '-'}
                    </span>
                </td>
                <td class="px-4 py-3 whitespace-nowrap">
                    <span class="file-metadata-badge inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300">
                        ${uploaderName ? this.escapeHtml(uploaderName) : '-'}
                    </span>
                </td>
                <td class="px-4 py-3 whitespace-nowrap text-right text-sm font-medium">
                    <div class="flex items-center justify-end space-x-2">
                        ${canView && fileId ? `
                            <button 
                                onclick="window.fileExplorerInstance.handleFileView(${fileId})"
                                class="text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100 p-1.5 rounded hover:bg-gray-100 dark:hover:bg-gray-700 transition-all"
                                title="View file details"
                            >
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                                </svg>
                            </button>
                        ` : ''}
                        ${canDownload && fileId ? `
                            <button 
                                onclick="window.fileExplorerInstance.handleFileDownload(${fileId})"
                                class="download-button text-white bg-blue-600 hover:bg-blue-700 p-1.5 rounded shadow-sm hover:shadow-md transition-all"
                                title="Download file"
                            >
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                                </svg>
                            </button>
                        ` : ''}
                        ${!fileId ? `
                            <span class="text-gray-400 dark:text-gray-500 text-xs px-2 py-1 bg-gray-100 dark:bg-gray-700 rounded">No access</span>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    }

    /**
     * Handle file view - show file details in modal
     * 
     * Fetches file metadata and displays it in a modal dialog.
     * Shows file name, size, type, upload date, uploader, and optional notes.
     * Provides Download and Close buttons.
     * 
     * @param {number} fileId - The ID of the file to view
     * @returns {Promise<void>}
     */
    async handleFileView(fileId) {
        try {
            const response = await fileExplorer.getFileMetadata(fileId);
            const file = response.data || response;

            // Extract uploader name from nested uploader object
            const uploaderName = file.uploader ?
                ((file.uploader.firstName && file.uploader.lastName) ?
                    `${file.uploader.firstName} ${file.uploader.lastName}` :
                    (file.uploader.email || 'Unknown')) :
                'Unknown';

            const content = `
                <div class="space-y-3">
                    <div>
                        <label class="text-sm font-medium text-gray-700">File Name:</label>
                        <p class="text-sm text-gray-900 mt-1">${this.escapeHtml(file.originalFilename || 'Unknown')}</p>
                    </div>
                    <div>
                        <label class="text-sm font-medium text-gray-700">Size:</label>
                        <p class="text-sm text-gray-900 mt-1">${file.fileSize ? this.formatFileSize(file.fileSize) : 'Unknown'}</p>
                    </div>
                    <div>
                        <label class="text-sm font-medium text-gray-700">Type:</label>
                        <p class="text-sm text-gray-900 mt-1">${file.fileType || 'Unknown'}</p>
                    </div>
                    <div>
                        <label class="text-sm font-medium text-gray-700">Uploaded:</label>
                        <p class="text-sm text-gray-900 mt-1">${file.createdAt ? formatDate(file.createdAt) : 'Unknown'}</p>
                    </div>
                    <div>
                        <label class="text-sm font-medium text-gray-700">Uploaded By:</label>
                        <p class="text-sm text-gray-900 mt-1">${this.escapeHtml(uploaderName)}</p>
                    </div>
                    ${file.notes ? `
                        <div>
                            <label class="text-sm font-medium text-gray-700">Notes:</label>
                            <p class="text-sm text-gray-900 mt-1">${this.escapeHtml(file.notes)}</p>
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
     * 
     * Downloads a file from the server and triggers browser download.
     * 
     * Process:
     * 1. Shows "Preparing download..." toast
     * 2. Fetches file from API
     * 3. Extracts filename from Content-Disposition header
     * 4. Creates blob and triggers download
     * 5. Shows success or error toast
     * 
     * Handles permission errors (403) and not found errors (404) with specific messages.
     * 
     * @param {number} fileId - The ID of the file to download
     * @returns {Promise<void>}
     */
    async handleFileDownload(fileId) {
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
                console.log('Content-Disposition header:', contentDisposition);

                // Try multiple patterns to extract filename from Content-Disposition header
                // Pattern 1: filename="example.pdf"
                let filenameMatch = /filename="([^"]+)"/.exec(contentDisposition);

                // Pattern 2: filename=example.pdf (without quotes)
                if (!filenameMatch) {
                    filenameMatch = /filename=([^;]+)/.exec(contentDisposition);
                }

                // Pattern 3: filename*=UTF-8''example.pdf (RFC 5987)
                if (!filenameMatch) {
                    filenameMatch = /filename\*=UTF-8''([^;]+)/.exec(contentDisposition);
                }

                if (filenameMatch && filenameMatch[1]) {
                    filename = filenameMatch[1].trim().replace(/['"]/g, '');
                    console.log('Extracted filename:', filename);

                    // Decode URI component if needed (for RFC 5987 format)
                    try {
                        filename = decodeURIComponent(filename);
                        console.log('Decoded filename:', filename);
                    } catch (e) {
                        // Keep original filename if decode fails
                        console.warn('Failed to decode filename:', e);
                    }
                } else {
                    console.warn('Could not extract filename from Content-Disposition header');
                }
            } else {
                console.warn('No Content-Disposition header found');
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

            // Show success message
            showToast(`File downloaded successfully`, 'success');

        } catch (error) {
            console.error('Error downloading file:', error);

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
     * Render error state with consistent styling
     * Based on Professor Dashboard design: error icon (text-red-400), text-center py-8
     * 
     * @param {string} message - Primary error message to display
     * @param {string} secondaryMessage - Optional secondary message with additional context
     * @returns {string} HTML string for error state
     */
    renderErrorState(message, secondaryMessage = null) {
        return `
            <div class="text-center py-8">
                <svg class="mx-auto h-12 w-12 text-red-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
                <p class="text-red-600 text-sm font-medium">${this.escapeHtml(message)}</p>
                ${secondaryMessage ? `
                    <p class="text-gray-500 text-xs mt-2">${this.escapeHtml(secondaryMessage)}</p>
                ` : ''}
            </div>
        `;
    }

    /**
     * Render error state in tree and file list containers
     * Convenience method to show error in both areas
     * 
     * @param {string} message - Primary error message to display
     * @param {string} secondaryMessage - Optional secondary message with additional context
     */
    renderError(message, secondaryMessage = null) {
        const treeContainer = document.getElementById('fileExplorerTree');
        const fileListContainer = document.getElementById('fileExplorerFileList');

        const errorHtml = this.renderErrorState(message, secondaryMessage);

        if (treeContainer) {
            treeContainer.innerHTML = errorHtml;
        }

        if (fileListContainer) {
            fileListContainer.innerHTML = errorHtml;
        }
    }

    /**
     * Format node type for display
     * 
     * Converts internal node type codes to human-readable labels.
     * 
     * @param {string} type - The node type code (e.g., 'YEAR', 'SEMESTER', 'COURSE')
     * @returns {string} Human-readable type label
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
     * Format file size for display
     * 
     * Converts bytes to human-readable format (Bytes, KB, MB, GB).
     * 
     * @param {number} bytes - File size in bytes
     * @returns {string} Formatted file size (e.g., "2.5 MB")
     * 
     * @example
     * formatFileSize(1024) // "1 KB"
     * formatFileSize(2621440) // "2.5 MB"
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    }

    /**
     * Get file icon CSS class based on file type
     * 
     * Returns appropriate Tailwind color class for file icon based on MIME type.
     * 
     * Color mapping:
     * - PDF: text-red-600
     * - ZIP/Archives: text-amber-600
     * - Word/Documents: text-blue-600
     * - Images: text-green-600
     * - Default: text-gray-500
     * 
     * @param {string} mimeType - The MIME type of the file
     * @returns {string} Tailwind CSS class for icon color
     */
    getFileIconClass(mimeType) {
        if (!mimeType) return 'text-gray-500';
        const type = mimeType.toLowerCase();
        if (type.includes('pdf')) return 'text-red-600';
        if (type.includes('zip') || type.includes('compressed') || type.includes('rar') || type.includes('7z')) return 'text-amber-600';
        if (type.includes('word') || type.includes('document') || type.includes('doc')) return 'text-blue-600';
        if (type.includes('image') || type.includes('png') || type.includes('jpg') || type.includes('jpeg') || type.includes('gif')) return 'text-green-600';
        return 'text-gray-500';
    }

    /**
     * Generate role-specific labels for folders
     * 
     * Creates HTML badges that appear next to folder names based on user role and permissions.
     * 
     * Label types:
     * - PROFESSOR role with showOwnershipLabels:
     *   - "Your Folder" (blue badge) when canWrite is true
     *   - "Read Only" (gray badge) when canRead is true but canWrite is false
     * 
     * - HOD role with showDepartmentContext:
     *   - "Read Only" (gray badge) for all folders
     * 
     * - DEANSHIP role with showProfessorLabels:
     *   - Professor name (purple badge) on professor folders
     * 
     * Badge styling follows Professor Dashboard design:
     * - Ownership: bg-blue-100 text-blue-800
     * - Read-only: bg-gray-100 text-gray-600
     * - Professor: bg-purple-100 text-purple-700
     * 
     * @param {Object} folder - The folder node to generate labels for
     * @returns {string} HTML string with role-specific badges
     */
    generateRoleSpecificLabels(folder) {
        let labels = '';

        // Professor role: Show "Your Folder" label for owned folders
        if (this.options.role === 'PROFESSOR' && this.options.showOwnershipLabels) {
            if (folder.canWrite === true) {
                labels += `
                    <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-blue-100 text-blue-800 ml-2">
                        <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path>
                        </svg>
                        Your Folder
                    </span>
                `;
            } else if (folder.canRead === true && folder.canWrite === false) {
                labels += `
                    <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600 ml-2">
                        <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                        </svg>
                        Read Only
                    </span>
                `;
            }
        }

        // HOD role: Show read-only label if configured
        if (this.options.role === 'HOD' && this.options.showDepartmentContext) {
            if (folder.canRead === true && folder.canWrite === false) {
                labels += `
                    <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600 ml-2">
                        <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                        </svg>
                        Read Only
                    </span>
                `;
            }
        }

        // Deanship role: Show professor name labels on professor folders
        if (this.options.role === 'DEANSHIP' && this.options.showProfessorLabels) {
            if (folder.type === 'PROFESSOR' && folder.metadata && folder.metadata.professorName) {
                labels += `
                    <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-700 ml-2">
                        <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
                        </svg>
                        ${this.escapeHtml(folder.metadata.professorName)}
                    </span>
                `;
            }
        }

        return labels;
    }

    /**
     * Render empty state with consistent styling
     * Based on Professor Dashboard design: folder icon, text-sm text-gray-500, py-8 text-center
     * 
     * @param {string} message - The message to display
     * @param {string} iconType - Type of icon to show ('folder', 'file', 'info')
     * @returns {string} HTML string for empty state
     */
    renderEmptyState(message, iconType = 'folder') {
        const icons = {
            folder: `
                <svg class="w-12 h-12 mx-auto text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                </svg>
            `,
            file: `
                <svg class="w-12 h-12 mx-auto text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
                </svg>
            `,
            info: `
                <svg class="w-12 h-12 mx-auto text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
            `
        };

        const icon = icons[iconType] || icons.folder;

        return `
            <div class="text-sm text-gray-500 py-8 text-center">
                ${icon}
                <p>${this.escapeHtml(message)}</p>
            </div>
        `;
    }

    /**
     * Render "no semester selected" state
     * Used when the user hasn't selected a semester yet
     * 
     * @returns {string} HTML string for no semester selected state
     */
    renderNoSemesterSelected() {
        return this.renderEmptyState('Select a semester to browse files', 'info');
    }

    /**
     * Render empty data state (when database has no data) 
     * Shows a friendly message explaining that no data exists yet
     * 
     * @returns {void}
     */
    renderEmptyDataState() {
        const treeContainer = document.getElementById('fileExplorerTree');
        const fileListContainer = document.getElementById('fileExplorerFileList');

        const emptyMessage = this.getEmptyDataMessage();
        const emptyHtml = `
            <div class="text-center py-12">
                <svg class="mx-auto h-16 w-16 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"></path>
                </svg>
                <h3 class="text-sm font-medium text-gray-900 mb-2">${emptyMessage.title}</h3>
                <p class="text-sm text-gray-500 mb-4">${emptyMessage.description}</p>
                ${emptyMessage.action ? `
                    <div class="mt-4 p-3 bg-blue-50 border border-blue-100 rounded-lg inline-block">
                        <p class="text-xs text-blue-700">
                            <svg class="inline-block w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                            </svg>
                            ${emptyMessage.action}
                        </p>
                    </div>
                ` : ''}
            </div>
        `;

        if (treeContainer) {
            treeContainer.innerHTML = emptyHtml;
        }

        if (fileListContainer) {
            fileListContainer.innerHTML = emptyHtml;
        }
    }

    /**
     * Get appropriate empty data message based on user role
     * 
     * @returns {Object} Object with title, description, and action properties
     */
    getEmptyDataMessage() {
        const role = this.options.role || 'PROFESSOR';

        switch (role) {
            case 'PROFESSOR':
                return {
                    title: 'No Data Available',
                    description: 'No courses or documents have been assigned to you for this semester yet.',
                    action: 'Contact your department head if you believe this is incorrect.'
                };

            case 'HOD':
                return {
                    title: 'No Data Available',
                    description: 'No professors or course assignments exist for this semester yet.',
                    action: 'Use the Professors tab to add faculty members and assign courses.'
                };

            case 'DEANSHIP':
                return {
                    title: 'No Data Available',
                    description: 'The system has no data for this semester yet.',
                    action: 'Ensure professors, courses, and assignments have been created in the system.'
                };

            default:
                return {
                    title: 'No Data Available',
                    description: 'There is no data available for this semester.',
                    action: null
                };
        }
    }

    /**
     * Render loading skeleton with stable dimensions
     * Task 7.1: Create loading skeleton rendering methods
     * 
     * Creates animated skeleton loaders that maintain the same dimensions as actual content
     * to prevent layout shift. Uses Tailwind's animate-pulse for loading animation.
     * 
     * @param {string} type - Type of loading state ('folders', 'files', 'tree', 'mixed', 'default')
     * @param {number} count - Number of skeleton items to render (default: 3)
     * @returns {string} HTML string for loading skeleton
     */
    renderLoadingSkeleton(type = 'default', count = 3) {
        let html = '';

        switch (type) {
            case 'folders':
                // Skeleton loaders for folder cards (wide blue cards)
                // Maintains same dimensions as actual folder cards (p-5, h-auto)
                html = '<div class="space-y-2">';
                for (let i = 0; i < count; i++) {
                    html += `
                        <div class="flex items-center justify-between p-5 bg-gray-50 rounded-lg border border-gray-200 animate-pulse">
                            <div class="flex items-center space-x-4 flex-1">
                                <div class="w-8 h-8 bg-gray-300 rounded"></div>
                                <div class="flex-1">
                                    <div class="h-5 bg-gray-300 rounded" style="width: 60%;"></div>
                                </div>
                            </div>
                            <div class="w-6 h-6 bg-gray-300 rounded"></div>
                        </div>
                    `;
                }
                html += '</div>';
                break;

            case 'files':
                // Skeleton loaders for file table rows
                // Maintains same table structure and dimensions
                html = `
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

                for (let i = 0; i < count; i++) {
                    html += `
                        <tr class="animate-pulse">
                            <td class="px-4 py-3 whitespace-nowrap">
                                <div class="flex items-center">
                                    <div class="w-14 h-14 bg-gray-300 rounded-lg mr-3"></div>
                                    <div class="h-4 bg-gray-300 rounded" style="width: 50%;"></div>
                                </div>
                            </td>
                            <td class="px-4 py-3 whitespace-nowrap">
                                <div class="h-6 bg-gray-300 rounded" style="width: 4rem;"></div>
                            </td>
                            <td class="px-4 py-3 whitespace-nowrap">
                                <div class="h-6 bg-gray-300 rounded" style="width: 6rem;"></div>
                            </td>
                            <td class="px-4 py-3 whitespace-nowrap">
                                <div class="h-6 bg-gray-300 rounded" style="width: 5rem;"></div>
                            </td>
                            <td class="px-4 py-3 whitespace-nowrap text-right">
                                <div class="flex items-center justify-end space-x-2">
                                    <div class="w-8 h-8 bg-gray-300 rounded"></div>
                                    <div class="w-8 h-8 bg-gray-300 rounded"></div>
                                </div>
                            </td>
                        </tr>
                    `;
                }

                html += `
                            </tbody>
                        </table>
                    </div>
                `;
                break;

            case 'tree':
                // Skeleton loaders for tree view
                // Maintains same row height (py-2.5) and structure as tree nodes
                html = '<div class="space-y-1">';
                for (let i = 0; i < count; i++) {
                    const indent = (i % 3) * 20; // Alternate indentation for visual hierarchy
                    html += `
                        <div class="flex items-center py-2.5 px-3 animate-pulse" style="padding-left: ${indent + 12}px;">
                            <div class="w-5 h-5 bg-gray-300 rounded mr-2"></div>
                            <div class="h-5 bg-gray-300 rounded flex-1" style="max-width: 70%;"></div>
                        </div>
                    `;
                }
                html += '</div>';
                break;

            case 'mixed':
                // Mixed loading state for folders and files together
                // Maintains same spacing and structure as actual content
                html = '<div class="space-y-4">';

                // Folder skeletons
                html += '<div><h4 class="text-xs font-semibold text-gray-500 uppercase mb-2">Folders</h4>';
                html += '<div class="space-y-2">';
                for (let i = 0; i < Math.min(count, 2); i++) {
                    html += `
                        <div class="flex items-center justify-between p-5 bg-gray-50 rounded-lg border border-gray-200 animate-pulse">
                            <div class="flex items-center space-x-4 flex-1">
                                <div class="w-8 h-8 bg-gray-300 rounded"></div>
                                <div class="flex-1">
                                    <div class="h-5 bg-gray-300 rounded" style="width: 60%;"></div>
                                </div>
                            </div>
                            <div class="w-6 h-6 bg-gray-300 rounded"></div>
                        </div>
                    `;
                }
                html += '</div></div>';

                // File skeletons
                html += '<div><h4 class="text-xs font-semibold text-gray-500 uppercase mb-2">Files</h4>';
                html += this.renderLoadingSkeleton('files', Math.min(count, 2));
                html += '</div>';

                html += '</div>';
                break;

            case 'default':
            default:
                // Default loading state - simple skeleton cards
                // Maintains same padding and height as actual items
                html = '<div class="space-y-3">';
                for (let i = 0; i < count; i++) {
                    html += `
                        <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200 animate-pulse">
                            <div class="flex items-center space-x-3 flex-1">
                                <div class="w-6 h-6 bg-gray-300 rounded"></div>
                                <div class="flex-1">
                                    <div class="h-4 bg-gray-300 rounded" style="width: 50%;"></div>
                                </div>
                            </div>
                            <div class="w-4 h-4 bg-gray-300 rounded"></div>
                        </div>
                    `;
                }
                html += '</div>';
                break;
        }

        return html;
    }

    /**
     * Task 7.2: Implement showLoading method
     * Show loading state in both tree and file list
     * 
     * Renders skeleton loaders in both panels without removing or hiding containers.
     * Maintains container dimensions during loading to prevent layout shift.
     * 
     * @returns {void}
     */
    showLoading() {
        this.showTreeLoading();
        this.showFileListLoading();
    }

    /**
     * Task 7.3: Implement showTreeLoading method
     * Show loading state in tree view only
     * 
     * Displays skeleton loaders in the tree panel while keeping file list visible.
     * Uses animated skeleton elements that match the tree node structure.
     * Maintains same dimensions as actual tree nodes to prevent layout shift.
     * 
     * @returns {void}
     */
    showTreeLoading() {
        const container = document.getElementById('fileExplorerTree');
        if (container) {
            container.innerHTML = this.renderLoadingSkeleton('tree', 5);
        }
    }

    /**
     * Task 7.4: Implement showFileListLoading method
     * Show loading state in file list only
     * 
     * Renders skeleton loaders only in file list container while keeping tree visible.
     * Maintains container dimensions during loading to prevent layout shift.
     * 
     * @param {string} type - Type of content being loaded ('folders', 'files', 'mixed')
     * @returns {void}
     */
    showFileListLoading(type = 'mixed') {
        const container = document.getElementById('fileExplorerFileList');
        if (container) {
            container.innerHTML = this.renderLoadingSkeleton(type, 3);
        }
    }

    /**
     * Render upload button for writable folders
     * Shows a prominent upload button when user has write permission
     * 
     * @param {Object} node - The current folder node
     * @returns {string} HTML for upload button
     */
    renderUploadButton(node) {
        if (!node || !node.canWrite) {
            return '';
        }

        const documentType = node.metadata?.documentType || '';
        const formattedType = this.formatDocumentTypeName(documentType);

        return `
            <div class="mb-4">
                <button 
                    onclick="window.fileExplorerInstance.handleUploadClick('${this.escapeHtml(node.path)}', '${documentType}')"
                    class="inline-flex items-center px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg shadow-sm hover:shadow-md transition-all">
                    <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                    </svg>
                    Upload ${formattedType}
                </button>
            </div>
        `;
    }

    /**
     * Render writable empty state with upload CTA
     * Shows an encouraging empty state with large upload button for writable folders
     * 
     * @param {Object} node - The current folder node  
     * @returns {string} HTML for writable empty state
     */
    renderWritableEmptyState(node) {
        const documentType = node.metadata?.documentType || 'files';
        const formattedType = this.formatDocumentTypeName(documentType);

        return `
            <div class="text-center py-12 upload-drop-zone" id="uploadDropZone">
                <div class="inline-flex items-center justify-center w-16 h-16 bg-blue-100 rounded-full mb-4">
                    <svg class="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                    </svg>
                </div>
                <h3 class="text-lg font-medium text-gray-900 mb-2">No ${formattedType} uploaded yet</h3>
                <p class="text-sm text-gray-500 mb-6">Upload your first ${formattedType.toLowerCase()} to get started</p>
                <button 
                    onclick="window.fileExplorerInstance.handleUploadClick('${this.escapeHtml(node.path)}', '${documentType}')"
                    class="inline-flex items-center px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg shadow-sm hover:shadow-md transition-all">
                    <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                    </svg>
                    Upload ${formattedType}
                </button>
                <p class="text-xs text-gray-400 mt-4">Drag and drop files here or click to upload<br>Supported formats: PDF, ZIP • Max 10 files per upload</p>
            </div>
        `;
    }

    /**
     * Handle upload button click
     * Opens upload modal for the specified path by dispatching custom event
     * 
     * @param {string} path - The folder path to upload to
     * @param {string} documentType - The document type
     */
    handleUploadClick(path, documentType) {
        // Dispatch custom event that prof.js will listen to
        const event = new CustomEvent('fileExplorerUpload', {
            detail: { path, documentType }
        });
        window.dispatchEvent(event);
    }

    /**
     * Initialize drag and drop for writable folders
     * Sets up event listeners for drag-drop file upload
     * 
     * @param {Object} node - The current folder node
     */
    initializeDragDrop(node) {
        if (!node || !node.canWrite) return;

        const container = document.getElementById('fileExplorerFileList');
        if (!container) return;

        // Remove existing listeners if any
        if (this.dragDropInitialized) return;
        this.dragDropInitialized = true;

        // Prevent default drag behaviors
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            container.addEventListener(eventName, (e) => {
                e.preventDefault();
                e.stopPropagation();
            }, false);
        });

        // Highlight drop zone
        ['dragenter', 'dragover'].forEach(eventName => {
            container.addEventListener(eventName, () => {
                const dropZone = document.getElementById('uploadDropZone');
                if (dropZone) {
                    dropZone.classList.add('border-4', 'border-dashed', 'border-blue-500', 'bg-blue-50');
                }
            }, false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            container.addEventListener(eventName, () => {
                const dropZone = document.getElementById('uploadDropZone');
                if (dropZone) {
                    dropZone.classList.remove('border-4', 'border-dashed', 'border-blue-500', 'bg-blue-50');
                }
            }, false);
        });

        // Handle drop
        container.addEventListener('drop', (e) => {
            const dt = e.dataTransfer;
            const files = dt.files;

            if (files.length > 0) {
                this.handleFileDrop(node.path, node.metadata?.documentType, files);
            }
        }, false);
    }

    /**
     * Handle file drop
     * Triggers upload modal with pre-selected files
     * 
     * @param {string} path - The folder path
     * @param {string} documentType - The document type
     * @param {FileList} files - The dropped files
     */
    handleFileDrop(path, documentType, files) {
        // Trigger same upload event as button click, but with files
        const event = new CustomEvent('fileExplorerUpload', {
            detail: { path, documentType, files }
        });
        window.dispatchEvent(event);
    }

    /**
     * Format document type name for display
     * Converts enum values to human-readable names
     * 
     * @param {string} documentType - The document type enum value
     * @returns {string} Formatted document type name
     */
    /**
     * Format document type name for display
     * Converts enum values to human-readable names
     * 
     * @param {string} documentType - The document type enum value
     * @returns {string} Formatted document type name
     */
    formatDocumentTypeName(documentType) {
        if (!documentType) return 'Files';

        const typeMap = {
            'SYLLABUS': 'Syllabus',
            'EXAM': 'Exams',
            'ASSIGNMENT': 'Assignments',
            'PROJECT_DOCS': 'Project Documents',
            'LECTURE_NOTES': 'Lecture Notes',
            'OTHER': 'Other Documents',
            'syllabus': 'Syllabus',
            'exam': 'Exams',
            'assignment': 'Assignments',
            'project_docs': 'Project Documents',
            'lecture_notes': 'Lecture Notes',
            'other': 'Other Documents'
        };

        return typeMap[documentType] || documentType;
    }

    /**
     * Show loading state in both tree and file list
     * 
     * Convenience method to show loading indicators in all File Explorer areas.
     * Called when loading root node or refreshing entire explorer.
     * 
     * @returns {void}
     */
    showLoading() {
        this.showTreeLoading();
        this.showFileListLoading();
    }

    /**
     * Render upload button for writable folders
     * 
     * @param {Object} node - The current folder node
     * @returns {string} HTML for the upload button area
     */
    renderUploadButton(node) {
        return `
            <div class="flex justify-end mb-4">
                <button 
                    onclick="window.dispatchEvent(new CustomEvent('fileExplorerUpload', { detail: { path: '${this.escapeHtml(node.path)}', documentType: '${node.name}', files: null } }))"
                    class="flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
                >
                    <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                    </svg>
                    Upload Files
                </button>
            </div>
        `;
    }

    /**
     * Render writable empty state
     * Shows a call-to-action to upload files when a folder is empty but writable
     * 
     * @param {Object} node - The current folder node
     * @returns {string} HTML for the empty state
     */
    renderWritableEmptyState(node) {
        return `
            <div class="text-center py-12 border-2 border-dashed border-gray-300 rounded-lg hover:border-blue-400 transition-colors cursor-pointer"
                 onclick="window.dispatchEvent(new CustomEvent('fileExplorerUpload', { detail: { path: '${this.escapeHtml(node.path)}', documentType: '${node.name}', files: null } }))">
                <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                </svg>
                <h3 class="mt-2 text-sm font-medium text-gray-900">No files yet</h3>
                <p class="mt-1 text-sm text-gray-500">Upload a file or drag and drop</p>
                <div class="mt-6">
                    <button type="button" class="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
                        <svg class="-ml-1 mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                        </svg>
                        Upload Files
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Handle upload button click
     * Opens upload modal for the specified path by dispatching custom event
     * 
     * @param {string} path - The folder path to upload to
     * @param {string} documentType - The document type
     */
    handleUploadClick(path, documentType) {
        // Dispatch custom event that prof.js will listen to
        const event = new CustomEvent('fileExplorerUpload', {
            detail: { path, documentType }
        });
        window.dispatchEvent(event);
    }

    /**
     * Initialize drag and drop functionality
     * 
     * @param {Object} node - The current folder node
     */
    initializeDragDrop(node) {
        const container = document.getElementById('fileExplorerFileList');
        if (!container) return;

        // Prevent default drag behaviors
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            container.addEventListener(eventName, preventDefaults, false);
        });

        function preventDefaults(e) {
            e.preventDefault();
            e.stopPropagation();
        }

        // Highlight drop zone
        ['dragenter', 'dragover'].forEach(eventName => {
            container.addEventListener(eventName, highlight, false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            container.addEventListener(eventName, unhighlight, false);
        });

        function highlight(e) {
            container.classList.add('dragover');
        }

        function unhighlight(e) {
            container.classList.remove('dragover');
        }

        // Handle dropped files
        container.addEventListener('drop', (e) => {
            const dt = e.dataTransfer;
            const files = dt.files;

            if (files && files.length > 0) {
                window.dispatchEvent(new CustomEvent('fileExplorerUpload', {
                    detail: {
                        path: node.path,
                        documentType: node.name,
                        files: files
                    }
                }));
            }
        }, false);
    }

    /**
     * Handle file drop
     * Triggers upload modal with pre-selected files
     * 
     * @param {string} path - The folder path
     * @param {string} documentType - The document type
     * @param {FileList} files - The dropped files
     */
    handleFileDrop(path, documentType, files) {
        // Trigger same upload event as button click, but with files
        const event = new CustomEvent('fileExplorerUpload', {
            detail: { path, documentType, files }
        });
        window.dispatchEvent(event);
    }

    /**
     * Format file size for display
     * Converts bytes to human-readable format (B, KB, MB, GB)
     * 
     * @param {number} bytes - File size in bytes
     * @returns {string} Formatted file size with 1 decimal place
     * 
     * @example
     * formatFileSize(1024) // "1.0 KB"
     * formatFileSize(2621440) // "2.5 MB"
     */
    formatFileSize(bytes) {
        if (bytes === 0 || bytes === null || bytes === undefined) return '0 B';

        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        const size = bytes / Math.pow(k, i);

        return size.toFixed(1) + ' ' + sizes[i];
    }

    /**
     * Get file icon class based on file type (MIME type)
     * Returns appropriate icon identifier for different file types
     * 
     * @param {string} mimeType - The MIME type of the file
     * @returns {string} Icon identifier for the file type
     * 
     * @example
     * getFileIcon('application/pdf') // 'pdf'
     * getFileIcon('image/png') // 'image'
     */
    getFileIcon(mimeType) {
        if (!mimeType) return 'file';

        const type = mimeType.toLowerCase();

        // PDF files
        if (type.includes('pdf')) return 'pdf';

        // Word documents
        if (type.includes('word') || type.includes('msword') ||
            type.includes('officedocument.wordprocessingml')) return 'word';

        // PowerPoint presentations
        if (type.includes('powerpoint') || type.includes('presentation') ||
            type.includes('officedocument.presentationml')) return 'powerpoint';

        // Excel spreadsheets
        if (type.includes('excel') || type.includes('spreadsheet') ||
            type.includes('officedocument.spreadsheetml')) return 'excel';

        // Images
        if (type.includes('image')) return 'image';

        // Archives
        if (type.includes('zip') || type.includes('rar') || type.includes('7z') ||
            type.includes('tar') || type.includes('gz')) return 'archive';

        // Text files
        if (type.includes('text')) return 'text';

        // Default
        return 'file';
    }

    /**
     * Escape HTML to prevent XSS
     * 
     * @param {string} str - String to escape
     * @returns {string} Escaped string
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

    /**
     * Cleanup method - unsubscribe from state changes
     * 
     * Call this method when destroying the FileExplorer instance
     * to prevent memory leaks from the state subscription.
     * 
     * @returns {void}
     */
    destroy() {
        if (this.unsubscribe) {
            this.unsubscribe();
            this.unsubscribe = null;
        }
    }
}

// Export for use in other modules
export default FileExplorer;
