/**
 * Admin Users Page Module
 * User management functionality for admin dashboard
 */

import { AdminLayout } from './admin-common.js';
import { apiRequest } from '../core/api.js';
import { showToast } from '../core/ui.js';

/**
 * Admin Users Page Class
 */
class AdminUsersPage {
    constructor() {
        this.layout = new AdminLayout();
        this.users = [];
        this.departments = [];
        this.currentPage = 0;
        this.pageSize = 20;
        this.totalPages = 0;
        this.totalElements = 0;
        this.filters = {
            search: '',
            role: '',
            isActive: '',
            departmentId: '',
        };
        this.editingUserId = null;
        this.deletingUserId = null;
    }

    /**
     * Initialize the page
     */
    async initialize() {
        try {
            // Initialize shared layout
            await this.layout.initialize();
            
            // Load departments for filters and form
            await this.loadDepartments();
            
            // Load users
            await this.loadUsers();
            
            // Set up event listeners
            this.setupEventListeners();
            
            console.log('[AdminUsers] Initialized successfully');
        } catch (error) {
            console.error('[AdminUsers] Initialization error:', error);
            showToast('Failed to initialize page', 'error');
        }
    }

    /**
     * Load departments for filters and form
     */
    async loadDepartments() {
        try {
            const response = await apiRequest('/admin/departments', { method: 'GET' });
            this.departments = response.data || [];
            this.renderDepartmentOptions();
        } catch (error) {
            console.error('[AdminUsers] Failed to load departments:', error);
        }
    }

    /**
     * Render department options in filters and form
     */
    renderDepartmentOptions() {
        const filterSelect = document.getElementById('departmentFilter');
        const formSelect = document.getElementById('department');
        
        const options = this.departments.map(dept => 
            `<option value="${dept.id}">${dept.name}</option>`
        ).join('');
        
        if (filterSelect) {
            filterSelect.innerHTML = '<option value="">All Departments</option>' + options;
        }
        
        if (formSelect) {
            formSelect.innerHTML = '<option value="">No Department</option>' + options;
        }
    }

    /**
     * Load users from API
     */
    async loadUsers() {
        try {
            this.showTableLoading(true);
            
            // Build query params
            const params = new URLSearchParams();
            params.append('page', this.currentPage);
            params.append('size', this.pageSize);
            
            if (this.filters.role) {
                params.append('role', this.filters.role);
            }
            if (this.filters.departmentId) {
                params.append('departmentId', this.filters.departmentId);
            }
            if (this.filters.isActive !== '') {
                params.append('isActive', this.filters.isActive);
            }
            
            const response = await apiRequest(`/admin/users?${params.toString()}`, { method: 'GET' });
            
            if (response && response.data) {
                this.users = response.data.content || [];
                this.totalPages = response.data.totalPages || 0;
                this.totalElements = response.data.totalElements || 0;
            } else {
                this.users = [];
            }
            
            // Apply client-side search filter
            if (this.filters.search) {
                const searchLower = this.filters.search.toLowerCase();
                this.users = this.users.filter(user => 
                    (user.firstName && user.firstName.toLowerCase().includes(searchLower)) ||
                    (user.lastName && user.lastName.toLowerCase().includes(searchLower)) ||
                    (user.email && user.email.toLowerCase().includes(searchLower))
                );
            }
            
            this.renderUsersTable();
            this.renderPagination();
            this.showTableLoading(false);
            
        } catch (error) {
            console.error('[AdminUsers] Failed to load users:', error);
            this.showTableLoading(false);
            showToast('Failed to load users', 'error');
        }
    }

    /**
     * Render users table
     */
    renderUsersTable() {
        const tbody = document.getElementById('usersTableBody');
        if (!tbody) return;
        
        if (this.users.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-state">
                        <div class="empty-state-icon">👥</div>
                        <div class="empty-state-title">No users found</div>
                        <div class="empty-state-message">Try adjusting your filters or create a new user</div>
                    </td>
                </tr>
            `;
            return;
        }
        
        tbody.innerHTML = this.users.map(user => this.createUserRowHTML(user)).join('');
        
        // Add event listeners to action buttons
        this.users.forEach(user => {
            const editBtn = document.getElementById(`edit-${user.id}`);
            const deleteBtn = document.getElementById(`delete-${user.id}`);
            
            if (editBtn) {
                editBtn.addEventListener('click', () => this.openEditModal(user));
            }
            if (deleteBtn) {
                deleteBtn.addEventListener('click', () => this.openDeleteModal(user.id));
            }
        });
    }

    /**
     * Create HTML for a user row
     */
    createUserRowHTML(user) {
        const roleBadgeClass = this.getRoleBadgeClass(user.role);
        const roleLabel = this.getRoleLabel(user.role);
        const statusBadgeClass = user.isActive ? 'status-active' : 'status-inactive';
        const statusLabel = user.isActive ? 'Active' : 'Inactive';
        
        return `
            <tr>
                <td>${this.escapeHtml(user.firstName || '')} ${this.escapeHtml(user.lastName || '')}</td>
                <td>${this.escapeHtml(user.email || '')}</td>
                <td><span class="role-badge ${roleBadgeClass}">${roleLabel}</span></td>
                <td>${this.escapeHtml(user.departmentName || '-')}</td>
                <td><span class="status-badge ${statusBadgeClass}">${statusLabel}</span></td>
                <td>
                    <div class="action-buttons">
                        <button class="btn-icon" id="edit-${user.id}" title="Edit user">✏️</button>
                        <button class="btn-icon danger" id="delete-${user.id}" title="Delete user">🗑️</button>
                    </div>
                </td>
            </tr>
        `;
    }

    /**
     * Get role badge CSS class
     */
    getRoleBadgeClass(role) {
        const classes = {
            'ROLE_ADMIN': 'role-admin',
            'ROLE_DEANSHIP': 'role-deanship',
            'ROLE_HOD': 'role-hod',
            'ROLE_PROFESSOR': 'role-professor',
        };
        return classes[role] || '';
    }

    /**
     * Get role display label
     */
    getRoleLabel(role) {
        const labels = {
            'ROLE_ADMIN': 'Admin',
            'ROLE_DEANSHIP': 'Dean',
            'ROLE_HOD': 'HOD',
            'ROLE_PROFESSOR': 'Professor',
        };
        return labels[role] || role;
    }

    /**
     * Render pagination
     */
    renderPagination() {
        const pagination = document.getElementById('pagination');
        if (!pagination) return;
        
        if (this.totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }
        
        let html = `
            <button class="pagination-btn" ${this.currentPage === 0 ? 'disabled' : ''} id="prevPage">
                Previous
            </button>
            <span class="pagination-info">
                Page ${this.currentPage + 1} of ${this.totalPages} (${this.totalElements} users)
            </span>
            <button class="pagination-btn" ${this.currentPage >= this.totalPages - 1 ? 'disabled' : ''} id="nextPage">
                Next
            </button>
        `;
        
        pagination.innerHTML = html;
        
        // Add event listeners
        const prevBtn = document.getElementById('prevPage');
        const nextBtn = document.getElementById('nextPage');
        
        if (prevBtn) {
            prevBtn.addEventListener('click', () => {
                if (this.currentPage > 0) {
                    this.currentPage--;
                    this.loadUsers();
                }
            });
        }
        
        if (nextBtn) {
            nextBtn.addEventListener('click', () => {
                if (this.currentPage < this.totalPages - 1) {
                    this.currentPage++;
                    this.loadUsers();
                }
            });
        }
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // Create user button
        const createBtn = document.getElementById('btnCreateUser');
        if (createBtn) {
            createBtn.addEventListener('click', () => this.openCreateModal());
        }
        
        // Search input
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            let debounceTimer;
            searchInput.addEventListener('input', (e) => {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(() => {
                    this.filters.search = e.target.value;
                    this.currentPage = 0;
                    this.loadUsers();
                }, 300);
            });
        }
        
        // Role filter
        const roleFilter = document.getElementById('roleFilter');
        if (roleFilter) {
            roleFilter.addEventListener('change', (e) => {
                this.filters.role = e.target.value;
                this.currentPage = 0;
                this.loadUsers();
            });
        }
        
        // Status filter
        const statusFilter = document.getElementById('statusFilter');
        if (statusFilter) {
            statusFilter.addEventListener('change', (e) => {
                this.filters.isActive = e.target.value;
                this.currentPage = 0;
                this.loadUsers();
            });
        }
        
        // Department filter
        const departmentFilter = document.getElementById('departmentFilter');
        if (departmentFilter) {
            departmentFilter.addEventListener('change', (e) => {
                this.filters.departmentId = e.target.value;
                this.currentPage = 0;
                this.loadUsers();
            });
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
        if (btnConfirmDelete) btnConfirmDelete.addEventListener('click', () => this.deleteUser());
        
        // Form submission
        const userForm = document.getElementById('userForm');
        if (userForm) {
            userForm.addEventListener('submit', (e) => this.handleFormSubmit(e));
        }
        
        // Role change - update email validation hint
        const roleSelect = document.getElementById('role');
        if (roleSelect) {
            roleSelect.addEventListener('change', (e) => this.onRoleChange(e.target.value));
        }
        
        // Email validation on blur
        const emailInput = document.getElementById('email');
        if (emailInput) {
            emailInput.addEventListener('blur', () => this.validateEmail());
        }
    }

    /**
     * Handle role change - show email format hints
     */
    onRoleChange(role) {
        const emailError = document.getElementById('emailError');
        if (!emailError) return;
        
        if (role === 'ROLE_PROFESSOR') {
            emailError.textContent = 'Professor email must end with @stuff.alquds.edu';
            emailError.classList.add('visible');
            emailError.style.color = 'var(--color-info)';
        } else if (role === 'ROLE_HOD') {
            emailError.textContent = 'HOD email must be in format: hod.<department_shortcut>@hod.alquds.edu';
            emailError.classList.add('visible');
            emailError.style.color = 'var(--color-info)';
        } else {
            emailError.classList.remove('visible');
        }
    }

    /**
     * Validate email based on selected role
     */
    validateEmail() {
        const email = document.getElementById('email').value;
        const role = document.getElementById('role').value;
        const emailError = document.getElementById('emailError');
        
        if (!email || !role) return true;
        
        let isValid = true;
        let errorMessage = '';
        
        if (role === 'ROLE_PROFESSOR') {
            if (!email.endsWith('@stuff.alquds.edu')) {
                isValid = false;
                errorMessage = 'Professor email must end with @stuff.alquds.edu';
            }
        } else if (role === 'ROLE_HOD') {
            const hodPattern = /^hod\.[a-z0-9]+@hod\.alquds\.edu$/;
            if (!hodPattern.test(email)) {
                isValid = false;
                errorMessage = 'HOD email must be in format: hod.<department_shortcut>@hod.alquds.edu';
            }
        }
        
        if (emailError) {
            if (!isValid) {
                emailError.textContent = errorMessage;
                emailError.classList.add('visible');
                emailError.style.color = 'var(--color-danger)';
            } else {
                emailError.classList.remove('visible');
            }
        }
        
        return isValid;
    }

    /**
     * Open create user modal
     */
    openCreateModal() {
        this.editingUserId = null;
        document.getElementById('modalTitle').textContent = 'Create User';
        const btnSubmitText = document.getElementById('btnSubmitText');
        if (btnSubmitText) btnSubmitText.textContent = 'Create User';
        document.getElementById('passwordRequired').style.display = 'inline';
        document.getElementById('password').required = true;
        
        // Reset form and clear validation errors
        document.getElementById('userForm').reset();
        document.getElementById('isActive').checked = true;
        document.getElementById('emailError').classList.remove('visible');
        this.clearValidationErrors();
        
        // Show modal
        document.getElementById('userModal').classList.add('active');
    }

    /**
     * Open edit user modal
     */
    openEditModal(user) {
        this.editingUserId = user.id;
        document.getElementById('modalTitle').textContent = 'Edit User';
        const btnSubmitText = document.getElementById('btnSubmitText');
        if (btnSubmitText) btnSubmitText.textContent = 'Update User';
        document.getElementById('passwordRequired').style.display = 'none';
        document.getElementById('password').required = false;
        
        // Clear any previous validation errors
        this.clearValidationErrors();
        
        // Fill form with user data
        document.getElementById('userId').value = user.id;
        document.getElementById('firstName').value = user.firstName || '';
        document.getElementById('lastName').value = user.lastName || '';
        document.getElementById('email').value = user.email || '';
        document.getElementById('password').value = '';
        document.getElementById('role').value = user.role || '';
        document.getElementById('department').value = user.departmentId || '';
        document.getElementById('isActive').checked = user.isActive !== false;
        document.getElementById('emailError').classList.remove('visible');
        
        // Show modal
        document.getElementById('userModal').classList.add('active');
    }

    /**
     * Close user modal
     */
    closeModal() {
        document.getElementById('userModal').classList.remove('active');
        this.editingUserId = null;
    }

    /**
     * Open delete confirmation modal
     */
    openDeleteModal(userId) {
        this.deletingUserId = userId;
        document.getElementById('deleteModal').classList.add('active');
    }

    /**
     * Close delete modal
     */
    closeDeleteModal() {
        document.getElementById('deleteModal').classList.remove('active');
        this.deletingUserId = null;
    }

    /**
     * Handle form submission
     */
    async handleFormSubmit(e) {
        e.preventDefault();
        
        // Validate email
        if (!this.validateEmail()) {
            return;
        }
        
        const formData = {
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value,
            email: document.getElementById('email').value,
            role: document.getElementById('role').value,
            departmentId: document.getElementById('department').value || null,
            isActive: document.getElementById('isActive').checked,
        };
        
        const password = document.getElementById('password').value;
        if (password) {
            formData.password = password;
        }
        
        try {
            if (this.editingUserId) {
                // Update user
                await apiRequest(`/admin/users/${this.editingUserId}`, {
                    method: 'PUT',
                    body: JSON.stringify(formData),
                });
                showToast('User updated successfully', 'success');
            } else {
                // Create user
                if (!password) {
                    showToast('Password is required for new users', 'error');
                    return;
                }
                await apiRequest('/admin/users', {
                    method: 'POST',
                    body: JSON.stringify(formData),
                });
                showToast('User created successfully', 'success');
            }
            
            this.closeModal();
            this.loadUsers();
            
        } catch (error) {
            console.error('[AdminUsers] Form submission error:', error);
            
            // Handle validation errors - show inline on form fields
            if (error.validationErrors) {
                this.showValidationErrors(error.validationErrors);
                showToast('Please fix the validation errors', 'error');
            } else {
                const message = error.message || 'Failed to save user';
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
            'firstName': 'firstName',
            'lastName': 'lastName',
            'email': 'email',
            'password': 'password',
            'role': 'role',
            'departmentId': 'department',
            'isActive': 'isActive'
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
     * Delete user
     */
    async deleteUser() {
        if (!this.deletingUserId) return;
        
        try {
            await apiRequest(`/admin/users/${this.deletingUserId}`, {
                method: 'DELETE',
            });
            
            showToast('User deleted successfully', 'success');
            this.closeDeleteModal();
            this.loadUsers();
            
        } catch (error) {
            console.error('[AdminUsers] Delete error:', error);
            showToast('Failed to delete user', 'error');
        }
    }

    /**
     * Show/hide table loading state
     */
    showTableLoading(show) {
        const tbody = document.getElementById('usersTableBody');
        if (tbody && show) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="loading-container">
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
    const page = new AdminUsersPage();
    page.initialize();
});

export default AdminUsersPage;
