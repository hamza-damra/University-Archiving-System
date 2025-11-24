/**
 * File Explorer Page Module
 * Deanship dashboard file explorer page with academic context integration
 */

import { DeanshipLayout } from './deanship-common.js';
import { FileExplorer } from './file-explorer.js';
import { fileExplorerState } from './file-explorer-state.js';
import { showToast } from './ui.js';

/**
 * FileExplorerPage class
 * Manages the file explorer page with academic context
 */
class FileExplorerPage {
    constructor() {
        this.layout = new DeanshipLayout();
        this.fileExplorer = null;
    }

    /**
     * Initialize the page
     */
    async initialize() {
        try {
            console.log('[FileExplorerPage] Starting initialization...');
            
            // Initialize shared layout
            await this.layout.initialize();
            console.log('[FileExplorerPage] Layout initialized');

            // Register callback for context changes
            this.layout.onContextChange((context) => {
                console.log('[FileExplorerPage] Context changed:', context);
                this.handleContextChange(context);
            });

            // Check if context is already selected and initialize file explorer
            const context = this.layout.getSelectedContext();
            console.log('[FileExplorerPage] Initial context:', context);
            this.handleContextChange(context);

            console.log('[FileExplorerPage] Initialized successfully');
        } catch (error) {
            console.error('[FileExplorerPage] Initialization error:', error);
            console.error('[FileExplorerPage] Error stack:', error.stack);
            this.handleError('Failed to initialize file explorer page. Please refresh the page.', error);
        }
    }

    /**
     * Handle academic context change
     * @param {object} context - The new context with academicYearId and semesterId
     */
    handleContextChange(context) {
        console.log('[FileExplorerPage] Handling context change:', context);
        
        const { academicYearId, semesterId } = context;

        // Show/hide context message
        const contextMessage = document.getElementById('contextMessage');
        const fileExplorerContainer = document.getElementById('fileExplorerContainer');

        if (!contextMessage) {
            console.error('[FileExplorerPage] Context message element not found!');
        }
        
        if (!fileExplorerContainer) {
            console.error('[FileExplorerPage] File explorer container element not found!');
        }

        if (!academicYearId || !semesterId) {
            // No context selected - show message
            console.log('[FileExplorerPage] No context, showing message');
            if (contextMessage) {
                contextMessage.style.display = 'flex';
            }
            if (fileExplorerContainer) {
                fileExplorerContainer.style.display = 'none';
            }

            // Clear file explorer if it exists
            if (this.fileExplorer) {
                this.fileExplorer = null;
            }

            return;
        }

        // Context selected - hide message and show file explorer
        console.log('[FileExplorerPage] Context selected, showing file explorer');
        if (contextMessage) {
            contextMessage.style.display = 'none';
        }
        if (fileExplorerContainer) {
            fileExplorerContainer.style.display = 'block';
        }

        // Initialize or reload file explorer
        this.initializeFileExplorer(academicYearId, semesterId);
    }

    /**
     * Initialize file explorer with academic context
     * 
     * This method implements the critical fix for the filter change issue.
     * It follows a specific sequence to ensure proper state management:
     * 
     * 1. Reset State: Clear all previous data to prevent stale content
     * 2. Update Context: Set new academic year/semester in state
     * 3. Create/Reuse Instance: Preserve FileExplorer instance across filter changes
     * 4. Load Data: Fetch and display new content
     * 
     * STATE RESET LOGIC:
     * The resetData() call is CRITICAL and must occur before loadRoot().
     * This matches the Professor Dashboard pattern and ensures:
     * - No residual folders/files from previous context are displayed
     * - Breadcrumbs are cleared
     * - Navigation history is reset
     * - Current path returns to root
     * 
     * INSTANCE PRESERVATION:
     * The FileExplorer instance is created once and reused for all filter changes.
     * This prevents memory leaks and maintains consistent event handlers.
     * 
     * @param {number} academicYearId - The academic year ID
     * @param {number} semesterId - The semester ID
     * @returns {Promise<void>}
     * 
     * @see Requirements 1.1, 2.1, 3.2 - State reset on filter change
     * @see Requirements 2.3, 3.4 - Context update sequence
     * @see Requirements 2.4 - Instance preservation
     * @see Requirements 5.1, 5.5 - Tree view hiding for Dean role
     */
    async initializeFileExplorer(academicYearId, semesterId) {
        try {
            // Show loading state
            this.showLoading(true);
            
            // STEP 1: Reset state before loading new data (Requirements: 1.1, 2.1, 3.2)
            // This is the KEY FIX for the filter change issue.
            // Clears: currentNode, currentPath, breadcrumbs, treeRoot, expandedNodes
            // Ensures no stale data from previous context remains visible
            fileExplorerState.resetData();
            
            // STEP 2: Update context in state (Requirements: 2.3, 3.4)
            // Sets the new academic year and semester context
            // This must happen AFTER reset and BEFORE load
            const context = this.layout.getSelectedContext();
            if (context.academicYear && context.semester) {
                fileExplorerState.setContext(
                    academicYearId,
                    semesterId,
                    context.academicYear.yearCode,
                    context.semester.name
                );
            }
            
            // STEP 3: Create FileExplorer instance if it doesn't exist (Requirements: 2.4)
            // Instance is preserved across filter changes to prevent memory leaks
            // and maintain consistent event handlers
            if (!this.fileExplorer) {
                this.fileExplorer = new FileExplorer('fileExplorerContainer', {
                    role: 'DEANSHIP',
                    showAllDepartments: true,
                    showProfessorLabels: true,
                    readOnly: true,
                    hideTree: true  // Requirements: 5.1, 5.5 - Hide tree view for simplified Dean UI
                });

                // Make it globally accessible for event handlers
                window.fileExplorerInstance = this.fileExplorer;
            }

            // STEP 4: Load root for the selected academic year and semester
            // This fetches and displays the new content
            await this.fileExplorer.loadRoot(academicYearId, semesterId);

            // Hide loading state
            this.showLoading(false);

            console.log('[FileExplorerPage] File explorer loaded for academic year:', academicYearId, 'semester:', semesterId);
        } catch (error) {
            console.error('[FileExplorerPage] Error initializing file explorer:', error);
            this.showLoading(false);
            this.handleApiError(error, 'load file explorer');
        }
    }

    /**
     * Show/hide loading state
     */
    showLoading(show) {
        const fileExplorerContainer = document.getElementById('fileExplorerContainer');
        if (fileExplorerContainer) {
            if (show) {
                fileExplorerContainer.style.opacity = '0.5';
                fileExplorerContainer.style.pointerEvents = 'none';
            } else {
                fileExplorerContainer.style.opacity = '1';
                fileExplorerContainer.style.pointerEvents = 'auto';
            }
        }
    }

    /**
     * Handle API errors with appropriate user feedback
     */
    handleApiError(error, action) {
        let message = `Failed to ${action}`;
        
        if (error.status === 401) {
            console.error(`[FileExplorerPage] Unauthorized: ${action}`);
            message = 'Your session has expired. Please log in again.';
            showToast(message, 'error');
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        } else if (error.status === 403) {
            console.error(`[FileExplorerPage] Forbidden: ${action}`);
            message = 'You do not have permission to access the file explorer.';
            showToast(message, 'error');
        } else if (error.status === 500) {
            console.error(`[FileExplorerPage] Server error: ${action}`, error);
            message = `Server error. Please try again later.`;
            showToast(message, 'error');
        } else if (error.message && error.message.includes('NetworkError')) {
            console.error(`[FileExplorerPage] Network error: ${action}`, error);
            message = 'Network error. Please check your connection and try again.';
            showToast(message, 'error');
        } else {
            console.error(`[FileExplorerPage] Error: ${action}`, error);
            message = error.message || message;
            showToast(message, 'error');
        }
    }

    /**
     * Handle general errors with user feedback
     */
    handleError(message, error) {
        console.error('[FileExplorerPage] Error:', error);
        showToast(message, 'error');
    }
}

// Initialize page on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const page = new FileExplorerPage();
    page.initialize();
});

export default FileExplorerPage;
