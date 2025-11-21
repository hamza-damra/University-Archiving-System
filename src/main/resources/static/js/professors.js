/**
 * Professors Management Page Module
 * Manages professors and their department assignments
 */

import { DeanshipLayout } from './deanship-common.js';
import { apiRequest } from './api.js';
import { showToast, showModal } from './ui.js';

/**
 * Professors Page Class
 */
class ProfessorsPage {
    constructor() {
        this.layout = new DeanshipLayout();
        this.professors = [];
        this.allProfessors = []; // Store unfiltered list for search
        this.departments = [];
        this.selectedDepartmentId = null;
    }

    /**
     * Initialize the professors page
     */
    async initialize() {
        try {
            // Initialize shared layout
            await this.layout.initialize();
            
            // Load departments
            await this.loadDepartments();
            
            // Load professors
            await this.loadProfessors();
            
            // Set up event listeners
            this.setupEventListeners();
            
            console.log('[Professors] Initialized successfully');
        } catch (error) {
            console.error('[Professors] Initialization error:', error);
            showToast('Failed to initialize professors page', 'error');
        }
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // Add professor button
        const addBtn = document.getElementById('addProfessorBtn');
        if (addBtn) {
            addBtn.addEventListener('click', () => this.showAddProfessorModal());
        }
        
        // Search input
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => this.handleSearch(e.target.value));
        }
        
        // Department filter
        const departmentFilter = document.getElementById('departmentFilter');
        if (departmentFilter) {
            departmentFilter.addEventListener('change', (e) => {
                this.selectedDepartmentId = e.target.value ? parseInt(e.target.value) : null;
                this.loadProfessors();
            });
        }
    }

    /**
     * Load departments from API
     */
    async loadDepartments() {
        try {
            const response = await apiRequest('/deanship/departments', {
                method: 'GET',
            });
            
            this.departments = response;
            this.renderDepartmentFilter();
            
        } catch (error) {
            console.error('[Professors] Failed to load departments:', error);
            this.handleApiError(error, 'load departments');
        }
    }

    /**
     * Render department filter dropdown
     */
    renderDepartmentFilter() {
        const select = document.getElementById('departmentFilter');
        if (!select) return;
        
        // Keep "All Departments" option
        select.innerHTML = '<option value="">All Departments</option>';
        
        this.departments.forEach(dept => {
            const option = document.createElement('option');
            option.value = dept.id;
            option.textContent = dept.name;
            select.appendChild(option);
        });
    }

    /**
     * Load professors from API
     */
    async loadProfessors() {
        try {
            // Show loading state
            this.showLoading(true);
            
            // Build URL with optional department filter
            let url = '/deanship/professors';
            if (this.selectedDepartmentId) {
                url += `?departmentId=${this.selectedDepartmentId}`;
            }
            
            // Fetch professors
            this.professors = await apiRequest(url, {
                method: 'GET',
            });
            
            // Store unfiltered list for search
            this.allProfessors = [...this.professors];
            
            // Hide loading state
            this.showLoading(false);
            
            // Render table
            this.renderProfessorsTable();
            
        } catch (error) {
            console.error('[Professors] Failed to load professors:', error);
            this.showLoading(false);
            this.handleApiError(error, 'load professors');
            this.showEmptyState(true);
        }
    }

    /**
     * Handle search input
     */
    handleSearch(searchTerm) {
        if (!searchTerm.trim()) {
            // No search term, show all professors
            this.professors = [...this.allProfessors];
        } else {
            // Filter professors by name, email, or professor ID
            const term = searchTerm.toLowerCase();
            this.professors = this.allProfessors.filter(prof => {
                const name = (prof.name || '').toLowerCase();
                const email = (prof.email || '').toLowerCase();
                const professorId = (prof.professorId || '').toLowerCase();
                
                return name.includes(term) || 
                       email.includes(term) || 
                       professorId.includes(term);
            });
        }
        
        // Re-render table with filtered results
        this.renderProfessorsTable();
    }

    /**
     * Render professors table
     */
    renderProfessorsTable() {
        const tbody = document.getElementById('professorsTableBody');
        const tableContainer = document.getElementById('tableContainer');
        const emptyState = document.getElementById('emptyState');
        
        if (!tbody) return;
        
        // Check if there are any professors
        if (this.professors.length === 0) {
            tableContainer.style.display = 'none';
            emptyState.style.display = 'block';
            return;
        }
        
        // Show table, hide empty state
        tableContainer.style.display = 'block';
        emptyState.style.display = 'none';
        
        // Render table rows
        tbody.innerHTML = this.professors.map(prof => this.createTableRow(prof)).join('');
        
        // Add event listeners to action buttons
        this.attachRowEventListeners();
    }

    /**
     * Create HTML for a table row
     */
    createTableRow(prof) {
        const statusClass = prof.active ? 'status-active' : 'status-inactive';
        const statusText = prof.active ? 'Active' : 'Inactive';
        
        const departmentName = prof.department ? this.escapeHtml(prof.department.name) : 'N/A';
        
        // Show activate or deactivate button based on status
        const statusButton = prof.active
            ? `<a class="action-link danger" data-action="deactivate" data-id="${prof.id}">Deactivate</a>`
            : `<a class="action-link success" data-action="activate" data-id="${prof.id}">Activate</a>`;
        
        return `
            <tr>
                <td>${this.escapeHtml(prof.professorId || 'N/A')}</td>
                <td>${this.escapeHtml(prof.name)}</td>
                <td>${this.escapeHtml(prof.email)}</td>
                <td>${departmentName}</td>
                <td>
                    <span class="status-badge ${statusClass}">${statusText}</span>
                </td>
                <td>
                    <div class="actions-cell">
                        <a class="action-link" data-action="edit" data-id="${prof.id}">Edit</a>
                        ${statusButton}
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
                const profId = parseInt(btn.dataset.id);
                this.showEditProfessorModal(profId);
            });
        });
        
        // Activate buttons
        document.querySelectorAll('[data-action="activate"]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const profId = parseInt(btn.dataset.id);
                this.activateProfessor(profId);
            });
        });
        
        // Deactivate buttons
        document.querySelectorAll('[data-action="deactivate"]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const profId = parseInt(btn.dataset.id);
                this.deactivateProfessor(profId);
            });
        });
    }

    /**
     * Show add professor modal
     */
    showAddProfessorModal() {
        const departmentOptions = this.departments.map(dept => 
            `<option value="${dept.id}">${this.escapeHtml(dept.name)}</option>`
        ).join('');
        
        const content = `
            <form id="professorForm" class="space-y-4">
                <div class="form-group">
                    <label for="professorName" class="form-label">Name *</label>
                    <input 
                        type="text" 
                        id="professorName" 
                        name="name"
                        required
                        class="form-input"
                        placeholder="Enter professor name"
                    >
                </div>
                <div class="form-group">
                    <label for="professorEmail" class="form-label">Email *</label>
                    <input 
                        type="email" 
                        id="professorEmail" 
                        name="email"
                        required
                        class="form-input"
                        placeholder="professor@alquds.edu"
                    >
                </div>
                <div class="form-group">
                    <label for="professorPassword" class="form-label">Password *</label>
                    <input 
                        type="password" 
                        id="professorPassword" 
                        name="password"
                        required
                        minlength="6"
                        class="form-input"
                        placeholder="Enter password (min 6 characters)"
                    >
                </div>
                <div class="form-group">
                    <label for="professorDepartment" class="form-label">Department *</label>
                    <select 
                        id="professorDepartment" 
                        name="departmentId"
                        required
                        class="form-input"
                    >
                        <option value="">Select Department</option>
                        ${departmentOptions}
                    </select>
                </div>
                <p style="font-size: var(--font-size-sm); color: var(--color-text-secondary);">
                    A unique professor ID will be automatically generated.
                </p>
            </form>
        `;

        showModal('Add Professor', content, {
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
                    onClick: (close) => this.handleCreateProfessor(close)
                }
            ]
        });
    }

    /**
     * Handle create professor
     */
    async handleCreateProfessor(closeModal) {
        const form = document.getElementById('professorForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const name = document.getElementById('professorName').value.trim();
        const email = document.getElementById('professorEmail').value.trim();
        const password = document.getElementById('professorPassword').value;
        const departmentId = parseInt(document.getElementById('professorDepartment').value);

        try {
            await apiRequest('/deanship/professors', {
                method: 'POST',
                body: JSON.stringify({ name, email, password, departmentId })
            });

            showToast('Professor created successfully', 'success');
            closeModal();
            
            // Reload professors
            await this.loadProfessors();
            
        } catch (error) {
            console.error('[Professors] Error creating professor:', error);
            this.handleApiError(error, 'create professor');
        }
    }

    /**
     * Show edit professor modal
     */
    showEditProfessorModal(profId) {
        const prof = this.allProfessors.find(p => p.id === profId);
        if (!prof) {
            showToast('Professor not found', 'error');
            return;
        }

        const departmentOptions = this.departments.map(dept => 
            `<option value="${dept.id}" ${prof.department && prof.department.id === dept.id ? 'selected' : ''}>
                ${this.escapeHtml(dept.name)}
            </option>`
        ).join('');
        
        const content = `
            <form id="editProfessorForm" class="space-y-4">
                <div class="form-group">
                    <label for="editProfessorName" class="form-label">Name *</label>
                    <input 
                        type="text" 
                        id="editProfessorName" 
                        name="name"
                        required
                        value="${this.escapeHtml(prof.name)}"
                        class="form-input"
                    >
                </div>
                <div class="form-group">
                    <label for="editProfessorEmail" class="form-label">Email *</label>
                    <input 
                        type="email" 
                        id="editProfessorEmail" 
                        name="email"
                        required
                        value="${this.escapeHtml(prof.email)}"
                        class="form-input"
                    >
                </div>
                <div class="form-group">
                    <label for="editProfessorPassword" class="form-label">Password</label>
                    <input 
                        type="password" 
                        id="editProfessorPassword" 
                        name="password"
                        minlength="6"
                        class="form-input"
                        placeholder="Leave blank to keep current password"
                    >
                </div>
                <div class="form-group">
                    <label for="editProfessorDepartment" class="form-label">Department *</label>
                    <select 
                        id="editProfessorDepartment" 
                        name="departmentId"
                        required
                        class="form-input"
                    >
                        <option value="">Select Department</option>
                        ${departmentOptions}
                    </select>
                </div>
            </form>
        `;

        showModal('Edit Professor', content, {
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
                    onClick: (close) => this.handleUpdateProfessor(profId, close)
                }
            ]
        });
    }

    /**
     * Handle update professor
     */
    async handleUpdateProfessor(profId, closeModal) {
        const form = document.getElementById('editProfessorForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const name = document.getElementById('editProfessorName').value.trim();
        const email = document.getElementById('editProfessorEmail').value.trim();
        const password = document.getElementById('editProfessorPassword').value;
        const departmentId = parseInt(document.getElementById('editProfessorDepartment').value);

        const updateData = { name, email, departmentId };
        
        // Only include password if it was provided
        if (password) {
            updateData.password = password;
        }

        try {
            await apiRequest(`/deanship/professors/${profId}`, {
                method: 'PUT',
                body: JSON.stringify(updateData)
            });

            showToast('Professor updated successfully', 'success');
            closeModal();
            
            // Reload professors
            await this.loadProfessors();
            
        } catch (error) {
            console.error('[Professors] Error updating professor:', error);
            this.handleApiError(error, 'update professor');
        }
    }

    /**
     * Activate professor
     */
    async activateProfessor(profId) {
        try {
            await apiRequest(`/deanship/professors/${profId}/activate`, {
                method: 'PUT'
            });

            showToast('Professor activated successfully', 'success');
            
            // Reload professors
            await this.loadProfessors();
            
        } catch (error) {
            console.error('[Professors] Error activating professor:', error);
            this.handleApiError(error, 'activate professor');
        }
    }

    /**
     * Deactivate professor
     */
    async deactivateProfessor(profId) {
        try {
            await apiRequest(`/deanship/professors/${profId}/deactivate`, {
                method: 'PUT'
            });

            showToast('Professor deactivated successfully', 'success');
            
            // Reload professors
            await this.loadProfessors();
            
        } catch (error) {
            console.error('[Professors] Error deactivating professor:', error);
            this.handleApiError(error, 'deactivate professor');
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
            console.error(`[Professors] Unauthorized: ${action}`);
            message = 'Your session has expired. Please log in again.';
            showToast(message, 'error');
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        } else if (error.status === 403) {
            console.error(`[Professors] Forbidden: ${action}`);
            message = 'You do not have permission to perform this action.';
            showToast(message, 'error');
        } else if (error.status === 500) {
            console.error(`[Professors] Server error: ${action}`, error);
            message = `Server error. Please try again later.`;
            showToast(message, 'error');
        } else if (error.message && error.message.includes('NetworkError')) {
            console.error(`[Professors] Network error: ${action}`, error);
            message = 'Network error. Please check your connection and try again.';
            showToast(message, 'error');
        } else {
            console.error(`[Professors] Error: ${action}`, error);
            message = error.message || message;
            showToast(message, 'error');
        }
    }

    /**
     * Handle general errors with user feedback
     */
    handleError(message, error) {
        console.error('[Professors] Error:', error);
        showToast(message, 'error');
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const page = new ProfessorsPage();
    page.initialize();
});

export default ProfessorsPage;
