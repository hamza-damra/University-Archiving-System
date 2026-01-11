/**
 * Admin Departments Page Module
 * Department management functionality for admin dashboard
 */

import { AdminLayout } from './admin-common.js';
import { apiRequest } from '../core/api.js';
import { showToast } from '../core/ui.js';

/**
 * Admin Departments Page Class
 */
class AdminDepartmentsPage {
    constructor() {
        this.layout = new AdminLayout();
        this.departments = [];
        this.editingDepartmentId = null;
        this.deletingDepartmentId = null;
    }

    /**
     * Initialize the page
     */
    async initialize() {
        try {
            // Initialize shared layout
            await this.layout.initialize();
            
            // Load departments
            await this.loadDepartments();
            
            // Set up event listeners
            this.setupEventListeners();
            
            console.log('[AdminDepartments] Initialized successfully');
        } catch (error) {
            console.error('[AdminDepartments] Initialization error:', error);
            showToast('Failed to initialize page', 'error');
        }
    }

    /**
     * Load departments from API
     */
    async loadDepartments() {
        try {
            this.showTableLoading(true);
            
            const response = await apiRequest('/admin/departments', { method: 'GET' });
            this.departments = response || [];
            
            this.renderDepartmentsTable();
            this.showTableLoading(false);
            
        } catch (error) {
            console.error('[AdminDepartments] Failed to load departments:', error);
            this.showTableLoading(false);
            showToast('Failed to load departments', 'error');
        }
    }

    /**
     * Render departments table
     */
    renderDepartmentsTable() {
        const tbody = document.getElementById('departmentsTableBody');
        if (!tbody) return;
        
        if (this.departments.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="empty-state">
                        <div class="empty-state-icon">🏛️</div>
                        <div class="empty-state-title">No departments found</div>
                        <div class="empty-state-message">Create a new department to get started</div>
                    </td>
                </tr>
            `;
            return;
        }
        
        tbody.innerHTML = this.departments.map(dept => this.createDepartmentRowHTML(dept)).join('');
        
        // Add event listeners to action buttons
        this.departments.forEach(dept => {
            const editBtn = document.getElementById(`edit-${dept.id}`);
            const deleteBtn = document.getElementById(`delete-${dept.id}`);
            
            if (editBtn) {
                editBtn.addEventListener('click', () => this.openEditModal(dept));
            }
            if (deleteBtn) {
                deleteBtn.addEventListener('click', () => this.openDeleteModal(dept.id));
            }
        });
    }

    /**
     * Create HTML for a department row
     */
    createDepartmentRowHTML(dept) {
        return `
            <tr>
                <td>${this.escapeHtml(dept.name || '')}</td>
                <td><span class="shortcut-badge">${this.escapeHtml(dept.shortcut || '')}</span></td>
                <td>${this.escapeHtml(dept.description || '-')}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn-icon" id="edit-${dept.id}" title="Edit department">✏️</button>
                        <button class="btn-icon danger" id="delete-${dept.id}" title="Delete department">🗑️</button>
                    </div>
                </td>
            </tr>
        `;
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // Create department button
        const createBtn = document.getElementById('btnCreateDepartment');
        if (createBtn) {
            createBtn.addEventListener('click', () => this.openCreateModal());
        }
        
        // Modal close buttons
        const modalClose = document.getElementById('modalClose');
        const btnCancel = document.getElementById('btnCancel');
        if (modalClose) modalClose.addEventListener('click', () => this.closeModal());
        if (btnCancel) btnCancel.addEventListener('click', () => this.closeModal());
        
        // Delete modal
        const deleteModalClose = document.getElementById('deleteModalClose');
        const btnCancelDelete = document.getElementById('btnCancelDelete');
        const btnConfirmDelete = document.getElementById('btnConfirmDelete');
        if (deleteModalClose) deleteModalClose.addEventListener('click', () => this.closeDeleteModal());
        if (btnCancelDelete) btnCancelDelete.addEventListener('click', () => this.closeDeleteModal());
        if (btnConfirmDelete) btnConfirmDelete.addEventListener('click', () => this.deleteDepartment());
        
        // Form submission
        const departmentForm = document.getElementById('departmentForm');
        if (departmentForm) {
            departmentForm.addEventListener('submit', (e) => this.handleFormSubmit(e));
        }
        
        // Shortcut input - enforce lowercase
        const shortcutInput = document.getElementById('shortcut');
        if (shortcutInput) {
            shortcutInput.addEventListener('input', (e) => {
                e.target.value = e.target.value.toLowerCase().replace(/[^a-z0-9]/g, '');
            });
        }
    }

    /**
     * Open create department modal
     */
    openCreateModal() {
        this.editingDepartmentId = null;
        document.getElementById('modalTitle').textContent = 'Create Department';
        const btnSubmitText = document.getElementById('btnSubmitText');
        if (btnSubmitText) btnSubmitText.textContent = 'Create Department';
        
        // Reset form and clear validation errors
        document.getElementById('departmentForm').reset();
        this.clearValidationErrors();
        
        // Show modal
        document.getElementById('departmentModal').classList.add('active');
    }

    /**
     * Open edit department modal
     */
    openEditModal(dept) {
        this.editingDepartmentId = dept.id;
        document.getElementById('modalTitle').textContent = 'Edit Department';
        const btnSubmitText = document.getElementById('btnSubmitText');
        if (btnSubmitText) btnSubmitText.textContent = 'Update Department';
        
        // Clear any previous validation errors
        this.clearValidationErrors();
        
        // Fill form with department data
        document.getElementById('departmentId').value = dept.id;
        document.getElementById('name').value = dept.name || '';
        document.getElementById('shortcut').value = dept.shortcut || '';
        document.getElementById('description').value = dept.description || '';
        
        // Show modal
        document.getElementById('departmentModal').classList.add('active');
    }

    /**
     * Close department modal
     */
    closeModal() {
        document.getElementById('departmentModal').classList.remove('active');
        this.editingDepartmentId = null;
    }

    /**
     * Open delete confirmation modal
     */
    openDeleteModal(departmentId) {
        this.deletingDepartmentId = departmentId;
        document.getElementById('deleteModal').classList.add('active');
    }

    /**
     * Close delete modal
     */
    closeDeleteModal() {
        document.getElementById('deleteModal').classList.remove('active');
        this.deletingDepartmentId = null;
    }

    /**
     * Handle form submission
     */
    async handleFormSubmit(e) {
        e.preventDefault();
        
        const formData = {
            name: document.getElementById('name').value,
            shortcut: document.getElementById('shortcut').value,
            description: document.getElementById('description').value || null,
        };
        
        try {
            if (this.editingDepartmentId) {
                // Update department
                await apiRequest(`/admin/departments/${this.editingDepartmentId}`, {
                    method: 'PUT',
                    body: JSON.stringify(formData),
                });
                showToast('Department updated successfully', 'success');
            } else {
                // Create department
                await apiRequest('/admin/departments', {
                    method: 'POST',
                    body: JSON.stringify(formData),
                });
                showToast('Department created successfully', 'success');
            }
            
            this.closeModal();
            this.loadDepartments();
            
        } catch (error) {
            console.error('[AdminDepartments] Form submission error:', error);
            
            // Handle validation errors - show inline on form fields
            if (error.validationErrors) {
                this.showValidationErrors(error.validationErrors);
                showToast('Please fix the validation errors', 'error');
            } else {
                const message = error.message || 'Failed to save department';
                showToast(message, 'error');
            }
        }
    }

    /**
     * Show validation errors inline on form fields
     */
    showValidationErrors(errors) {
        // Clear previous errors
        this.clearValidationErrors();
        
        // Field mapping: backend field names to form field IDs
        const fieldMapping = {
            'name': 'name',
            'shortcut': 'shortcut',
            'description': 'description'
        };
        
        Object.entries(errors).forEach(([field, message]) => {
            const fieldId = fieldMapping[field] || field;
            const input = document.getElementById(fieldId);
            const errorElement = document.getElementById(`${fieldId}Error`);
            
            if (input) {
                input.classList.add('admin-form-input-error');
                input.setAttribute('aria-invalid', 'true');
            }
            
            if (errorElement) {
                errorElement.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg><span>${this.escapeHtml(message)}</span>`;
                errorElement.classList.add('show');
            } else {
                // If no specific error element, show in toast
                showToast(`${field}: ${message}`, 'error');
            }
        });
    }

    /**
     * Clear all validation errors from form
     */
    clearValidationErrors() {
        // Remove error styling from all inputs
        document.querySelectorAll('.admin-form-input-error').forEach(input => {
            input.classList.remove('admin-form-input-error');
            input.removeAttribute('aria-invalid');
        });
        
        // Hide all error messages
        document.querySelectorAll('.admin-form-error.show').forEach(error => {
            error.classList.remove('show');
        });
    }

    /**
     * Delete department
     */
    async deleteDepartment() {
        if (!this.deletingDepartmentId) return;
        
        try {
            await apiRequest(`/admin/departments/${this.deletingDepartmentId}`, {
                method: 'DELETE',
            });
            
            showToast('Department deleted successfully', 'success');
            this.closeDeleteModal();
            this.loadDepartments();
            
        } catch (error) {
            console.error('[AdminDepartments] Delete error:', error);
            showToast(error.message || 'Failed to delete department', 'error');
        }
    }

    /**
     * Show/hide table loading state
     */
    showTableLoading(show) {
        const tbody = document.getElementById('departmentsTableBody');
        if (tbody && show) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="loading-container">
                        <div class="loading-spinner"></div>
                    </td>
                </tr>
            `;
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
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const page = new AdminDepartmentsPage();
    page.initialize();
});

export default AdminDepartmentsPage;
