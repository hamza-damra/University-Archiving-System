/**
 * Reports Page Module
 * Displays submission status reports and analytics for deanship
 */

import { DeanshipLayout } from './deanship-common.js';
import { apiRequest } from './api.js';
import { showToast } from './ui.js';

/**
 * Reports Page Class
 */
class ReportsPage {
    constructor() {
        this.layout = new DeanshipLayout();
        this.reportData = null;
        this.isReportVisible = false;
    }

    /**
     * Initialize the reports page
     */
    async initialize() {
        try {
            console.log('[Reports] Starting initialization...');
            
            // Initialize shared layout
            await this.layout.initialize();
            console.log('[Reports] Layout initialized');
            
            // Check if context is available
            this.updateContextMessage();
            console.log('[Reports] Context message updated');
            
            // Set up event listeners
            this.setupEventListeners();
            console.log('[Reports] Event listeners set up');
            
            // Register context change listener
            this.layout.onContextChange((context) => {
                console.log('[Reports] Context changed:', context);
                this.handleContextChange();
            });
            
            // Load report if context is available
            const hasContext = this.layout.hasContext();
            console.log('[Reports] Has context:', hasContext);
            
            if (hasContext) {
                await this.loadSubmissionReport();
            }
            
            console.log('[Reports] Initialized successfully');
        } catch (error) {
            console.error('[Reports] Initialization error:', error);
            console.error('[Reports] Error stack:', error.stack);
            showToast('Failed to initialize reports page: ' + error.message, 'error');
        }
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        const viewReportBtn = document.getElementById('viewReportBtn');
        if (viewReportBtn) {
            viewReportBtn.addEventListener('click', () => {
                this.toggleReportVisibility();
            });
        }
    }

    /**
     * Handle context change (academic year or semester selection)
     */
    handleContextChange() {
        this.updateContextMessage();
        
        // Hide report content when context changes
        this.hideReport();
        
        // Load report if context is available
        if (this.layout.hasContext()) {
            this.loadSubmissionReport();
        } else {
            this.clearReportData();
        }
    }

    /**
     * Update context message visibility
     */
    updateContextMessage() {
        const contextMessage = document.getElementById('contextMessage');
        const viewReportBtn = document.getElementById('viewReportBtn');
        
        if (!contextMessage || !viewReportBtn) return;
        
        if (this.layout.hasContext()) {
            contextMessage.classList.add('hidden');
            viewReportBtn.disabled = false;
        } else {
            contextMessage.classList.remove('hidden');
            viewReportBtn.disabled = true;
        }
    }

    /**
     * Load submission report from API
     */
    async loadSubmissionReport() {
        try {
            const context = this.layout.getSelectedContext();
            if (!context.semesterId) {
                console.warn('[Reports] No semester selected');
                return;
            }
            
            console.log('[Reports] Loading submission report for semester:', context.semesterId);
            
            // Show loading state
            this.showLoadingState();
            
            // Fetch report data
            this.reportData = await apiRequest(
                `/deanship/reports/system-wide?semesterId=${context.semesterId}`,
                { method: 'GET' }
            );
            
            console.log('[Reports] Report data loaded:', this.reportData);
            
            // If report is visible, update the display
            if (this.isReportVisible) {
                this.displayReport();
            }
            
        } catch (error) {
            console.error('[Reports] Failed to load submission report:', error);
            this.handleApiError(error, 'load submission report');
            this.clearReportData();
        }
    }

    /**
     * Show loading state in button
     */
    showLoadingState() {
        const viewReportBtn = document.getElementById('viewReportBtn');
        if (viewReportBtn) {
            viewReportBtn.innerHTML = '<span class="loading-spinner"></span> Loading...';
            viewReportBtn.disabled = true;
        }
    }

    /**
     * Toggle report visibility
     */
    toggleReportVisibility() {
        if (this.isReportVisible) {
            this.hideReport();
        } else {
            this.showReport();
        }
    }

    /**
     * Show report content
     */
    showReport() {
        const reportContent = document.getElementById('reportContent');
        const viewReportBtn = document.getElementById('viewReportBtn');
        
        if (!reportContent || !viewReportBtn) return;
        
        if (this.reportData) {
            reportContent.classList.remove('hidden');
            viewReportBtn.textContent = 'Hide Report';
            this.isReportVisible = true;
            this.displayReport();
        } else {
            showToast('No report data available', 'warning');
        }
    }

    /**
     * Hide report content
     */
    hideReport() {
        const reportContent = document.getElementById('reportContent');
        const viewReportBtn = document.getElementById('viewReportBtn');
        
        if (!reportContent || !viewReportBtn) return;
        
        reportContent.classList.add('hidden');
        viewReportBtn.textContent = 'View Report';
        this.isReportVisible = false;
    }

    /**
     * Display report data
     */
    displayReport() {
        if (!this.reportData) {
            console.warn('[Reports] No report data to display');
            return;
        }
        
        // Calculate statistics
        const totalSubmissions = this.reportData.totalSubmissions || 0;
        const completedSubmissions = this.reportData.completedSubmissions || 0;
        const pendingSubmissions = totalSubmissions - completedSubmissions;
        const completionRate = totalSubmissions > 0
            ? Math.round((completedSubmissions / totalSubmissions) * 100)
            : 0;
        
        // Update statistics
        this.updateStatistic('totalSubmissions', totalSubmissions);
        this.updateStatistic('completedSubmissions', completedSubmissions);
        this.updateStatistic('pendingSubmissions', pendingSubmissions);
        this.updateStatistic('completionRate', `${completionRate}%`);
        
        // Update completion bar
        this.updateCompletionBar(completionRate);
        
        // Display additional details if available
        this.displayReportDetails();
    }

    /**
     * Update a statistic value
     */
    updateStatistic(elementId, value) {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = value;
        }
    }

    /**
     * Update completion bar
     */
    updateCompletionBar(percentage) {
        const completionBarFill = document.getElementById('completionBarFill');
        const completionBarText = document.getElementById('completionBarText');
        
        if (completionBarFill) {
            completionBarFill.style.width = `${percentage}%`;
        }
        
        if (completionBarText) {
            completionBarText.textContent = `${percentage}%`;
        }
    }

    /**
     * Display additional report details
     */
    displayReportDetails() {
        const reportDetails = document.getElementById('reportDetails');
        if (!reportDetails) return;
        
        // Check if report data has department breakdown or other details
        if (this.reportData.departmentBreakdown && this.reportData.departmentBreakdown.length > 0) {
            reportDetails.classList.remove('hidden');
            
            const html = `
                <div class="report-details-title">Department Breakdown</div>
                <table class="report-table">
                    <thead>
                        <tr>
                            <th>Department</th>
                            <th>Total Submissions</th>
                            <th>Completed</th>
                            <th>Pending</th>
                            <th>Completion Rate</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${this.reportData.departmentBreakdown.map(dept => {
                            const deptCompletionRate = dept.totalSubmissions > 0
                                ? Math.round((dept.completedSubmissions / dept.totalSubmissions) * 100)
                                : 0;
                            return `
                                <tr>
                                    <td>${dept.departmentName}</td>
                                    <td>${dept.totalSubmissions}</td>
                                    <td>${dept.completedSubmissions}</td>
                                    <td>${dept.totalSubmissions - dept.completedSubmissions}</td>
                                    <td>${deptCompletionRate}%</td>
                                </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            `;
            
            reportDetails.innerHTML = html;
        } else {
            reportDetails.classList.add('hidden');
        }
    }

    /**
     * Clear report data
     */
    clearReportData() {
        this.reportData = null;
        this.hideReport();
        
        const viewReportBtn = document.getElementById('viewReportBtn');
        if (viewReportBtn) {
            viewReportBtn.textContent = 'View Report';
            viewReportBtn.disabled = !this.layout.hasContext();
        }
    }

    /**
     * Handle API errors with appropriate user feedback
     */
    handleApiError(error, action) {
        let message = `Failed to ${action}`;
        
        if (error.status === 401) {
            console.error(`[Reports] Unauthorized: ${action}`);
            message = 'Your session has expired. Please log in again.';
            showToast(message, 'error');
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        } else if (error.status === 403) {
            console.error(`[Reports] Forbidden: ${action}`);
            message = 'You do not have permission to access this report.';
            showToast(message, 'error');
        } else if (error.status === 500) {
            console.error(`[Reports] Server error: ${action}`, error);
            message = `Server error. Please try again later.`;
            showToast(message, 'error');
        } else if (error.message && error.message.includes('NetworkError')) {
            console.error(`[Reports] Network error: ${action}`, error);
            message = 'Network error. Please check your connection and try again.';
            showToast(message, 'error');
        } else {
            console.error(`[Reports] Error: ${action}`, error);
            message = error.message || message;
            showToast(message, 'error');
        }
    }

    /**
     * Handle general errors with user feedback
     */
    handleError(message, error) {
        console.error('[Reports] Error:', error);
        showToast(message, 'error');
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const page = new ReportsPage();
    page.initialize();
});

export default ReportsPage;
