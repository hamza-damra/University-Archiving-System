/**
 * Academic Years Management Page Module
 * Manages academic years and their semesters
 */

import { DeanshipLayout } from './deanship-common.js';
import { apiRequest } from './api.js';
import { showToast, showModal } from './ui.js';

/**
 * Academic Years Page Class
 */
class AcademicYearsPage {
    constructor() {
        this.layout = new DeanshipLayout();
        this.academicYears = [];
    }

    /**
     * Initialize the academic years page
     */
    async initialize() {
        try {
            // Initialize shared layout
            await this.layout.initialize();
            
            // Load academic years
            await this.loadAcademicYears();
            
            // Set up event listeners
            this.setupEventListeners();
            
            console.log('[AcademicYears] Initialized successfully');
        } catch (error) {
            console.error('[AcademicYears] Initialization error:', error);
            this.showLoading(false);
            this.handleError('Failed to initialize academic years page. Please refresh the page.', error);
        }
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // Add academic year button
        const addBtn = document.getElementById('addAcademicYearBtn');
        if (addBtn) {
            addBtn.addEventListener('click', () => this.showAddAcademicYearModal());
        }
    }

    /**
     * Load academic years from API
     */
    async loadAcademicYears() {
        try {
            // Show loading state
            this.showLoading(true);
            
            // Fetch academic years
            this.academicYears = await apiRequest('/deanship/academic-years', {
                method: 'GET',
            });
            
            // Hide loading state
            this.showLoading(false);
            
            // Render table
            this.renderAcademicYearsTable();
            
        } catch (error) {
            console.error('[AcademicYears] Failed to load academic years:', error);
            this.showLoading(false);
            this.handleApiError(error, 'load academic years');
            this.showEmptyState(true);
        }
    }

    /**
     * Render academic years table
     */
    renderAcademicYearsTable() {
        const tbody = document.getElementById('academicYearsTableBody');
        const tableContainer = document.getElementById('tableContainer');
        const emptyState = document.getElementById('emptyState');
        
        if (!tbody) return;
        
        // Check if there are any academic years
        if (this.academicYears.length === 0) {
            tableContainer.style.display = 'none';
            emptyState.style.display = 'block';
            return;
        }
        
        // Show table, hide empty state
        tableContainer.style.display = 'block';
        emptyState.style.display = 'none';
        
        // Render table rows
        tbody.innerHTML = this.academicYears.map(year => this.createTableRow(year)).join('');
        
        // Add event listeners to action buttons
        this.attachRowEventListeners();
    }

    /**
     * Create HTML for a table row
     */
    createTableRow(year) {
        const statusClass = year.isActive ? 'status-active' : 'status-inactive';
        const statusText = year.isActive ? 'Active' : 'Inactive';
        
        const activateButton = !year.isActive 
            ? `<a class="action-link success" data-action="activate" data-id="${year.id}">Activate</a>`
            : '';
        
        return `
            <tr>
                <td>${this.escapeHtml(year.yearCode)}</td>
                <td>${year.startYear}</td>
                <td>${year.endYear}</td>
                <td>
                    <span class="status-badge ${statusClass}">${statusText}</span>
                </td>
                <td>
                    <div class="actions-cell">
                        <a class="action-link" data-action="edit" data-id="${year.id}">Edit</a>
                        ${activateButton}
                    </div>
                </td>
            </tr>
        `;
    }

    /**
     * Attach event listeners to row action buttons
     */
    attachRowEventListeners() {
        // Edit buttons
        document.querySelectorAll('[data-action="edit"]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const yearId = parseInt(btn.dataset.id);
                this.showEditAcademicYearModal(yearId);
            });
        });
        
        // Activate buttons
        document.querySelectorAll('[data-action="activate"]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const yearId = parseInt(btn.dataset.id);
                this.activateAcademicYear(yearId);
            });
        });
    }

    /**
     * Show add academic year modal
     */
    showAddAcademicYearModal() {
        const content = `
            <form id="academicYearForm" class="space-y-4">
                <div class="form-group">
                    <label for="startYear" class="form-label">Start Year *</label>
                    <input 
                        type="number" 
                        id="startYear" 
                        name="startYear"
                        required
                        min="2000"
                        max="2100"
                        class="form-input"
                        placeholder="e.g., 2024"
                    >
                </div>
                <div class="form-group">
                    <label for="endYear" class="form-label">End Year *</label>
                    <input 
                        type="number" 
                        id="endYear" 
                        name="endYear"
                        required
                        min="2000"
                        max="2100"
                        class="form-input"
                        placeholder="e.g., 2025"
                    >
                </div>
                <p style="font-size: var(--font-size-sm); color: var(--color-text-secondary);">
                    The system will automatically create three semesters (First, Second, Summer) for this academic year.
                </p>
            </form>
        `;

        showModal('Add Academic Year', content, {
            buttons: [
                {
                    text: 'Cancel',
                    className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                    action: 'cancel',
                    onClick: (close) => close()
                },
                {
                    text: 'Create',
                    className: 'bg-blue-600 text-white hover:bg-blue-700',
                    action: 'create',
                    onClick: (close) => this.handleCreateAcademicYear(close)
                }
            ]
        });
    }

    /**
     * Handle create academic year
     */
    async handleCreateAcademicYear(closeModal) {
        const form = document.getElementById('academicYearForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const startYear = parseInt(document.getElementById('startYear').value);
        const endYear = parseInt(document.getElementById('endYear').value);

        // Validate years
        if (endYear <= startYear) {
            showToast('End year must be greater than start year', 'error');
            return;
        }

        try {
            await apiRequest('/deanship/academic-years', {
                method: 'POST',
                body: JSON.stringify({ startYear, endYear })
            });

            showToast('Academic year created successfully', 'success');
            closeModal();
            
            // Reload academic years
            await this.loadAcademicYears();
            
            // Reload layout academic years (for dropdown)
            await this.layout.loadAcademicYears();
            
        } catch (error) {
            console.error('[AcademicYears] Error creating academic year:', error);
            this.handleApiError(error, 'create academic year');
        }
    }

    /**
     * Show edit academic year modal
     */
    showEditAcademicYearModal(yearId) {
        const year = this.academicYears.find(y => y.id === yearId);
        if (!year) {
            showToast('Academic year not found', 'error');
            return;
        }

        const content = `
            <form id="editAcademicYearForm" class="space-y-4">
                <div class="form-group">
                    <label for="editStartYear" class="form-label">Start Year *</label>
                    <input 
                        type="number" 
                        id="editStartYear" 
                        name="startYear"
                        required
                        min="2000"
                        max="2100"
                        value="${year.startYear}"
                        class="form-input"
                    >
                </div>
                <div class="form-group">
                    <label for="editEndYear" class="form-label">End Year *</label>
                    <input 
                        type="number" 
                        id="editEndYear" 
                        name="endYear"
                        required
                        min="2000"
                        max="2100"
                        value="${year.endYear}"
                        class="form-input"
                    >
                </div>
            </form>
        `;

        showModal('Edit Academic Year', content, {
            buttons: [
                {
                    text: 'Cancel',
                    className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                    action: 'cancel',
                    onClick: (close) => close()
                },
                {
                    text: 'Save',
                    className: 'bg-blue-600 text-white hover:bg-blue-700',
                    action: 'save',
                    onClick: (close) => this.handleUpdateAcademicYear(yearId, close)
                }
            ]
        });
    }

    /**
     * Handle update academic year
     */
    async handleUpdateAcademicYear(yearId, closeModal) {
        const form = document.getElementById('editAcademicYearForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const startYear = parseInt(document.getElementById('editStartYear').value);
        const endYear = parseInt(document.getElementById('editEndYear').value);

        // Validate years
        if (endYear <= startYear) {
            showToast('End year must be greater than start year', 'error');
            return;
        }

        try {
            await apiRequest(`/deanship/academic-years/${yearId}`, {
                method: 'PUT',
                body: JSON.stringify({ startYear, endYear })
            });

            showToast('Academic year updated successfully', 'success');
            closeModal();
            
            // Reload academic years
            await this.loadAcademicYears();
            
            // Reload layout academic years (for dropdown)
            await this.layout.loadAcademicYears();
            
        } catch (error) {
            console.error('[AcademicYears] Error updating academic year:', error);
            this.handleApiError(error, 'update academic year');
        }
    }

    /**
     * Activate academic year
     */
    async activateAcademicYear(yearId) {
        try {
            await apiRequest(`/deanship/academic-years/${yearId}/activate`, {
                method: 'PUT'
            });

            showToast('Academic year activated successfully', 'success');
            
            // Reload academic years
            await this.loadAcademicYears();
            
            // Reload layout academic years (for dropdown)
            await this.layout.loadAcademicYears();
            
        } catch (error) {
            console.error('[AcademicYears] Error activating academic year:', error);
            this.handleApiError(error, 'activate academic year');
        }
    }

    /**
     * Show/hide loading state
     */
    showLoading(show) {
        const loadingState = document.getElementById('loadingState');
        const tableContainer = document.getElementById('tableContainer');
        const emptyState = document.getElementById('emptyState');
        
        if (loadingState) {
            loadingState.style.display = show ? 'flex' : 'none';
        }
        
        if (show) {
            if (tableContainer) tableContainer.style.display = 'none';
            if (emptyState) emptyState.style.display = 'none';
        }
    }

    /**
     * Show/hide empty state
     */
    showEmptyState(show) {
        const emptyState = document.getElementById('emptyState');
        const tableContainer = document.getElementById('tableContainer');
        
        if (emptyState) {
            emptyState.style.display = show ? 'block' : 'none';
        }
        
        if (tableContainer) {
            tableContainer.style.display = show ? 'none' : 'block';
        }
    }

    /**
     * Escape HTML to prevent XSS
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Handle API errors with appropriate user feedback
     */
    handleApiError(error, action) {
        let message = `Failed to ${action}`;
        
        if (error.status === 401) {
            console.error(`[AcademicYears] Unauthorized: ${action}`);
            message = 'Your session has expired. Please log in again.';
            showToast(message, 'error');
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        } else if (error.status === 403) {
            console.error(`[AcademicYears] Forbidden: ${action}`);
            message = 'You do not have permission to perform this action.';
            showToast(message, 'error');
        } else if (error.status === 500) {
            console.error(`[AcademicYears] Server error: ${action}`, error);
            message = `Server error. Please try again later.`;
            showToast(message, 'error');
        } else if (error.message && error.message.includes('NetworkError')) {
            console.error(`[AcademicYears] Network error: ${action}`, error);
            message = 'Network error. Please check your connection and try again.';
            showToast(message, 'error');
        } else {
            console.error(`[AcademicYears] Error: ${action}`, error);
            message = error.message || message;
            showToast(message, 'error');
        }
    }

    /**
     * Handle general errors with user feedback
     */
    handleError(message, error) {
        console.error('[AcademicYears] Error:', error);
        showToast(message, 'error');
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const page = new AcademicYearsPage();
    page.initialize();
});

export default AcademicYearsPage;
