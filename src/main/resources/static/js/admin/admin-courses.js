/**
 * Admin Courses Page Module
 * Course management functionality for admin dashboard
 */

import { AdminLayout } from './admin-common.js';
import { apiRequest } from '../core/api.js';
import { showToast } from '../core/ui.js';

/**
 * Admin Courses Page Class
 */
class AdminCoursesPage {
    constructor() {
        this.layout = new AdminLayout();
        this.courses = [];
        this.departments = [];
        this.filters = {
            search: '',
            departmentId: '',
            isActive: '',
        };
        this.editingCourseId = null;
        this.deletingCourseId = null;
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
            
            // Load courses
            await this.loadCourses();
            
            // Set up event listeners
            this.setupEventListeners();
            
            console.log('[AdminCourses] Initialized successfully');
        } catch (error) {
            console.error('[AdminCourses] Initialization error:', error);
            showToast('Failed to initialize page', 'error');
        }
    }

    /**
     * Load departments for filters and form
     */
    async loadDepartments() {
        try {
            const response = await apiRequest('/admin/departments', { method: 'GET' });
            this.departments = response || [];
            this.renderDepartmentOptions();
        } catch (error) {
            console.error('[AdminCourses] Failed to load departments:', error);
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
            formSelect.innerHTML = '<option value="">Select Department</option>' + options;
        }
    }

    /**
     * Load courses from API
     */
    async loadCourses() {
        try {
            this.showTableLoading(true);
            
            // Build query params
            const params = new URLSearchParams();
            if (this.filters.departmentId) {
                params.append('departmentId', this.filters.departmentId);
            }
            
            const queryString = params.toString();
            const url = `/admin/courses${queryString ? '?' + queryString : ''}`;
            
            const response = await apiRequest(url, { method: 'GET' });
            this.courses = response || [];
            
            // Apply client-side filters
            if (this.filters.search) {
                const searchLower = this.filters.search.toLowerCase();
                this.courses = this.courses.filter(course => 
                    (course.courseCode && course.courseCode.toLowerCase().includes(searchLower)) ||
                    (course.name && course.name.toLowerCase().includes(searchLower))
                );
            }
            
            if (this.filters.isActive !== '') {
                const isActive = this.filters.isActive === 'true';
                this.courses = this.courses.filter(course => course.active === isActive);
            }
            
            this.renderCoursesTable();
            this.showTableLoading(false);
            
        } catch (error) {
            console.error('[AdminCourses] Failed to load courses:', error);
            this.showTableLoading(false);
            showToast('Failed to load courses', 'error');
        }
    }

    /**
     * Render courses table
     */
    renderCoursesTable() {
        const tbody = document.getElementById('coursesTableBody');
        if (!tbody) return;
        
        if (this.courses.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-state">
                        <div class="empty-state-icon">📚</div>
                        <div class="empty-state-title">No courses found</div>
                        <div class="empty-state-message">Try adjusting your filters or create a new course</div>
                    </td>
                </tr>
            `;
            return;
        }
        
        tbody.innerHTML = this.courses.map(course => this.createCourseRowHTML(course)).join('');
        
        // Add event listeners to action buttons
        this.courses.forEach(course => {
            const editBtn = document.getElementById(`edit-${course.id}`);
            const deleteBtn = document.getElementById(`delete-${course.id}`);
            
            if (editBtn) {
                editBtn.addEventListener('click', () => this.openEditModal(course));
            }
            if (deleteBtn) {
                deleteBtn.addEventListener('click', () => this.openDeleteModal(course.id));
            }
        });
    }

    /**
     * Create HTML for a course row
     */
    createCourseRowHTML(course) {
        const statusBadgeClass = course.active ? 'status-active' : 'status-inactive';
        const statusLabel = course.active ? 'Active' : 'Inactive';
        const departmentName = course.department ? course.department.name : '-';
        
        return `
            <tr>
                <td><span class="course-code">${this.escapeHtml(course.courseCode || '')}</span></td>
                <td>${this.escapeHtml(course.name || '')}</td>
                <td>${this.escapeHtml(departmentName)}</td>
                <td>${course.creditHours || '-'}</td>
                <td><span class="status-badge ${statusBadgeClass}">${statusLabel}</span></td>
                <td>
                    <div class="action-buttons">
                        <button class="btn-icon" id="edit-${course.id}" title="Edit course">✏️</button>
                        <button class="btn-icon danger" id="delete-${course.id}" title="Deactivate course">🗑️</button>
                    </div>
                </td>
            </tr>
        `;
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // Create course button
        const createBtn = document.getElementById('btnCreateCourse');
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
                    this.loadCourses();
                }, 300);
            });
        }
        
        // Department filter
        const departmentFilter = document.getElementById('departmentFilter');
        if (departmentFilter) {
            departmentFilter.addEventListener('change', (e) => {
                this.filters.departmentId = e.target.value;
                this.loadCourses();
            });
        }
        
        // Status filter
        const statusFilter = document.getElementById('statusFilter');
        if (statusFilter) {
            statusFilter.addEventListener('change', (e) => {
                this.filters.isActive = e.target.value;
                this.loadCourses();
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
        if (btnConfirmDelete) btnConfirmDelete.addEventListener('click', () => this.deactivateCourse());
        
        // Form submission
        const courseForm = document.getElementById('courseForm');
        if (courseForm) {
            courseForm.addEventListener('submit', (e) => this.handleFormSubmit(e));
        }
        
        // Close modals on overlay click
        const courseModal = document.getElementById('courseModal');
        const deleteModal = document.getElementById('deleteModal');
        
        if (courseModal) {
            courseModal.addEventListener('click', (e) => {
                if (e.target === courseModal) this.closeModal();
            });
        }
        
        if (deleteModal) {
            deleteModal.addEventListener('click', (e) => {
                if (e.target === deleteModal) this.closeDeleteModal();
            });
        }
        
        // Close modals on Escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                if (courseModal?.classList.contains('active')) {
                    this.closeModal();
                }
                if (deleteModal?.classList.contains('active')) {
                    this.closeDeleteModal();
                }
            }
        });
    }

    /**
     * Open create course modal
     */
    openCreateModal() {
        this.editingCourseId = null;
        document.getElementById('modalTitle').textContent = 'Create Course';
        const btnSubmitText = document.getElementById('btnSubmitText');
        if (btnSubmitText) btnSubmitText.textContent = 'Create Course';
        
        // Reset form and clear validation errors
        document.getElementById('courseForm').reset();
        document.getElementById('isActive').checked = true;
        this.clearValidationErrors();
        
        // Show modal
        document.getElementById('courseModal').classList.add('active');
        
        // Focus first input
        setTimeout(() => document.getElementById('courseCode').focus(), 100);
    }

    /**
     * Open edit course modal
     */
    openEditModal(course) {
        this.editingCourseId = course.id;
        document.getElementById('modalTitle').textContent = 'Edit Course';
        const btnSubmitText = document.getElementById('btnSubmitText');
        if (btnSubmitText) btnSubmitText.textContent = 'Update Course';
        
        // Clear any previous validation errors
        this.clearValidationErrors();
        
        // Fill form with course data
        document.getElementById('courseId').value = course.id;
        document.getElementById('courseCode').value = course.courseCode || '';
        document.getElementById('courseName').value = course.name || '';
        document.getElementById('creditHours').value = course.creditHours || '';
        document.getElementById('department').value = course.department ? course.department.id : '';
        document.getElementById('description').value = course.description || '';
        document.getElementById('isActive').checked = course.active !== false;
        
        // Show modal
        document.getElementById('courseModal').classList.add('active');
        
        // Focus first input
        setTimeout(() => document.getElementById('courseCode').focus(), 100);
    }

    /**
     * Close course modal
     */
    closeModal() {
        document.getElementById('courseModal').classList.remove('active');
        this.editingCourseId = null;
        // Reset form to clear any validation states
        document.getElementById('courseForm').reset();
    }

    /**
     * Open delete confirmation modal
     */
    openDeleteModal(courseId) {
        this.deletingCourseId = courseId;
        document.getElementById('deleteModal').classList.add('active');
    }

    /**
     * Close delete modal
     */
    closeDeleteModal() {
        document.getElementById('deleteModal').classList.remove('active');
        this.deletingCourseId = null;
    }

    /**
     * Handle form submission
     */
    async handleFormSubmit(e) {
        e.preventDefault();
        
        // Build form data matching CourseDTO structure
        const formData = {
            courseCode: document.getElementById('courseCode').value,
            courseName: document.getElementById('courseName').value,
            departmentId: parseInt(document.getElementById('department').value),
            description: document.getElementById('description').value || null,
            isActive: document.getElementById('isActive').checked,
        };
        
        // Add level/creditHours if the field exists (store as level string)
        const creditHoursInput = document.getElementById('creditHours');
        if (creditHoursInput && creditHoursInput.value) {
            formData.level = creditHoursInput.value + ' Credit Hours';
        }
        
        try {
            if (this.editingCourseId) {
                // Update course
                await apiRequest(`/admin/courses/${this.editingCourseId}`, {
                    method: 'PUT',
                    body: JSON.stringify(formData),
                });
                showToast('Course updated successfully', 'success');
            } else {
                // Create course
                await apiRequest('/admin/courses', {
                    method: 'POST',
                    body: JSON.stringify(formData),
                });
                showToast('Course created successfully', 'success');
            }
            
            this.closeModal();
            this.loadCourses();
            
        } catch (error) {
            console.error('[AdminCourses] Form submission error:', error);
            
            // Handle validation errors - show inline on form fields
            if (error.validationErrors) {
                this.showValidationErrors(error.validationErrors);
                showToast('Please fix the validation errors', 'error');
            } else {
                const message = error.message || 'Failed to save course';
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
        // Backend DTO fields -> HTML form element IDs
        const fieldMapping = {
            'courseCode': 'courseCode',
            'courseName': 'courseName',
            'departmentId': 'department',
            'creditHours': 'creditHours',
            'description': 'description',
            'isActive': 'isActive',
            'level': 'level'
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
     * Deactivate course
     */
    async deactivateCourse() {
        if (!this.deletingCourseId) return;
        
        try {
            await apiRequest(`/admin/courses/${this.deletingCourseId}`, {
                method: 'DELETE',
            });
            
            showToast('Course deactivated successfully', 'success');
            this.closeDeleteModal();
            this.loadCourses();
            
        } catch (error) {
            console.error('[AdminCourses] Deactivate error:', error);
            showToast(error.message || 'Failed to deactivate course', 'error');
        }
    }

    /**
     * Show/hide table loading state
     */
    showTableLoading(show) {
        const tbody = document.getElementById('coursesTableBody');
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
    const page = new AdminCoursesPage();
    page.initialize();
});

export default AdminCoursesPage;
