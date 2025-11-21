/**
 * File Explorer Page Module
 * Deanship dashboard file explorer page with academic context integration
 */

import { DeanshipLayout } from './deanship-common.js';
import { FileExplorer } from './file-explorer.js';
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
            // Initialize shared layout
            await this.layout.initialize();

            // Register callback for context changes
            this.layout.onContextChange((context) => {
                this.handleContextChange(context);
            });

            // Check if context is already selected and initialize file explorer
            const context = this.layout.getSelectedContext();
            this.handleContextChange(context);

            console.log('[FileExplorerPage] Initialized successfully');
        } catch (error) {
            console.error('[FileExplorerPage] Initialization error:', error);
            this.handleError('Failed to initialize file explorer page. Please refresh the page.', error);
        }
    }

    /**
     * Handle academic context change
     * @param {object} context - The new context with academicYearId and semesterId
     */
    handleContextChange(context) {
        const { academicYearId, semesterId } = context;

        // Show/hide context message
        const contextMessage = document.getElementById('contextMessage');
        const fileExplorerContainer = document.getElementById('fileExplorerContainer');

        if (!academicYearId || !semesterId) {
            // No context selected - show message
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
     * @param {number} academicYearId - The academic year ID
     * @param {number} semesterId - The semester ID
     */
    async initializeFileExplorer(academicYearId, semesterId) {
        try {
            // Show loading state
            this.showLoading(true);
            
            // If file explorer doesn't exist, create it
            if (!this.fileExplorer) {
                this.fileExplorer = new FileExplorer('fileExplorerContainer', {
                    role: 'DEANSHIP',
                    showAllDepartments: true,
                    showProfessorLabels: true,
                    readOnly: true
                });

                // Make it globally accessible for event handlers
                window.fileExplorerInstance = this.fileExplorer;
            }

            // Load root for the selected academic year and semester
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
