/**
 * Courses Management Page Module
 * Manages courses and their department assignments
 */

import { DeanshipLayout } from './deanship-common.js';
import { apiRequest } from './api.js';
import { showToast, showModal } from './ui.js';

/**
 * Courses Page Class
 */
class CoursesPage {
    constructor() {
        this.layout = new DeanshipLayout();
        this.courses = [];
        this.allCourses = []; // Store unfiltered list for search
        this.departments = [];
        this.selectedDepartmentId = null;
    }

    /**
     * Initialize the courses page
     */
    async initialize() {
        try {
            console.log('[Courses] Starting initialization...');
            
            // Initialize shared layout
            await this.layout.initialize();
            console.log('[Courses] Layout initialized');
            
            // Load departments
            await this.loadDepartments();
            console.log('[Courses] Departments loaded:', this.departments.length);
            
            // Load courses
            await this.loadCourses();
            console.log('[Courses] Courses loaded:', this.courses.length);
            
            // Set up event listeners
            this.setupEventListeners();
            console.log('[Courses] Event listeners set up');
            
            console.log('[Courses] Initialized successfully');
        } catch (error) {
            console.error('[Courses] Initialization error:', error);
            console.error('[Courses] Error stack:', error.stack);
            showToast('Failed to initialize courses page: ' + error.message, 'error');
            
            // Show error state
            this.showLoading(false);
            this.showEmptyState(true);
        }
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // Add course button
        const addBtn = document.getElementById('addCourseBtn');
        if (addBtn) {
            addBtn.addEventListener('click', () => this.showAddCourseModal());
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
                this.loadCourses();
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
            console.error('[Courses] Failed to load departments:', error);
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
     * Load courses from API
     */
    async loadCourses() {
        try {
            console.log('[Courses] Loading courses...');
            
            // Show loading state
            this.showLoading(true);
            
            // Build URL with optional department filter
            let url = '/deanship/courses';
            if (this.selectedDepartmentId) {
                url += `?departmentId=${this.selectedDepartmentId}`;
            }
            
            console.log('[Courses] Fetching from URL:', url);
            
            // Fetch courses
            this.courses = await apiRequest(url, {
                method: 'GET',
            });
            
            console.log('[Courses] Received courses:', this.courses);
            
            // Store unfiltered list for search
            this.allCourses = [...this.courses];
            
            // Hide loading state
            this.showLoading(false);
            
            // Render table
            this.renderCoursesTable();
            console.log('[Courses] Table rendered');
            
        } catch (error) {
            console.error('[Courses] Failed to load courses:', error);
            console.error('[Courses] Error details:', error.message, error.stack);
            this.showLoading(false);
            this.handleApiError(error, 'load courses');
            this.showEmptyState(true);
        }
    }

    /**
     * Handle search input
     */
    handleSearch(searchTerm) {
        if (!searchTerm.trim()) {
            // No search term, show all courses
            this.courses = [...this.allCourses];
        } else {
            // Filter courses by course code or course name
            const term = searchTerm.toLowerCase();
            this.courses = this.allCourses.filter(course => {
                const courseCode = (course.courseCode || '').toLowerCase();
                const courseName = (course.courseName || '').toLowerCase();
                
                return courseCode.includes(term) || courseName.includes(term);
            });
        }
        
        // Re-render table with filtered results
        this.renderCoursesTable();
    }

    /**
     * Render courses table
     */
    renderCoursesTable() {
        console.log('[Courses] Rendering table with', this.courses.length, 'courses');
        
        const tbody = document.getElementById('coursesTableBody');
        const tableContainer = document.getElementById('tableContainer');
        const emptyState = document.getElementById('emptyState');
        
        if (!tbody) {
            console.error('[Courses] Table body element not found!');
            return;
        }
        
        if (!tableContainer) {
            console.error('[Courses] Table container element not found!');
            return;
        }
        
        if (!emptyState) {
            console.error('[Courses] Empty state element not found!');
            return;
        }
        
        // Check if there are any courses
        if (this.courses.length === 0) {
            console.log('[Courses] No courses to display, showing empty state');
            tableContainer.style.display = 'none';
            emptyState.style.display = 'block';
            return;
        }
        
        // Show table, hide empty state
        console.log('[Courses] Showing table with courses');
        tableContainer.style.display = 'block';
        emptyState.style.display = 'none';
        
        // Render table rows
        tbody.innerHTML = this.courses.map(course => this.createTableRow(course)).join('');
        console.log('[Courses] Table rows rendered');
        
        // Add event listeners to action buttons
        this.attachRowEventListeners();
    }

    /**
     * Create HTML for a table row
     */
    createTableRow(course) {
        const statusClass = course.active ? 'status-active' : 'status-inactive';
        const statusText = course.active ? 'Active' : 'Inactive';
        
        const departmentName = course.department ? this.escapeHtml(course.department.name) : 'N/A';
        const credits = course.credits || 'N/A';
        
        // Show deactivate button only for active courses
        const deactivateButton = course.active
            ? `<a class="action-link danger" data-action="deactivate" data-id="${course.id}">Deactivate</a>`
            : '';
        
        return `
            <tr>
                <td>${this.escapeHtml(course.courseCode || 'N/A')}</td>
                <td>${this.escapeHtml(course.courseName)}</td>
                <td>${departmentName}</td>
                <td>${credits}</td>
                <td>
                    <span class="status-badge ${statusClass}">${statusText}</span>
                </td>
                <td>
                    <div class="actions-cell">
                        <a class="action-link" data-action="edit" data-id="${course.id}">Edit</a>
                        ${deactivateButton}
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
                const courseId = parseInt(btn.dataset.id);
                this.showEditCourseModal(courseId);
            });
        });
        
        // Deactivate buttons
        document.querySelectorAll('[data-action="deactivate"]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const courseId = parseInt(btn.dataset.id);
                this.deactivateCourse(courseId);
            });
        });
    }

    /**
     * Show add course modal
     */
    showAddCourseModal() {
        const departmentOptions = this.departments.map(dept => 
            `<option value="${dept.id}">${this.escapeHtml(dept.name)}</option>`
        ).join('');
        
        const content = `
            <form id="courseForm" class="space-y-4">
                <div class="form-group">
                    <label for="courseCode" class="form-label">Course Code *</label>
                    <input 
                        type="text" 
                        id="courseCode" 
                        name="courseCode"
                        required
                        class="form-input"
                        placeholder="e.g., CS101"
                    >
                </div>
                <div class="form-group">
                    <label for="courseName" class="form-label">Course Name *</label>
                    <input 
                        type="text" 
                        id="courseName" 
                        name="courseName"
                        required
                        class="form-input"
                        placeholder="Enter course name"
                    >
                </div>
                <div class="form-group">
                    <label for="courseDepartment" class="form-label">Department *</label>
                    <select 
                        id="courseDepartment" 
                        name="departmentId"
                        required
                        class="form-input"
                    >
                        <option value="">Select Department</option>
                        ${departmentOptions}
                    </select>
                </div>
                <div class="form-group">
                    <label for="courseCredits" class="form-label">Credits *</label>
                    <input 
                        type="number" 
                        id="courseCredits" 
                        name="credits"
                        required
                        min="1"
                        max="6"
                        class="form-input"
                        placeholder="e.g., 3"
                    >
                </div>
            </form>
        `;

        showModal('Add Course', content, {
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
                    onClick: (close) => this.handleCreateCourse(close)
                }
            ]
        });
    }

    /**
     * Handle create course
     */
    async handleCreateCourse(closeModal) {
        const form = document.getElementById('courseForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const courseCode = document.getElementById('courseCode').value.trim();
        const courseName = document.getElementById('courseName').value.trim();
        const departmentId = parseInt(document.getElementById('courseDepartment').value);
        const credits = parseInt(document.getElementById('courseCredits').value);

        try {
            await apiRequest('/deanship/courses', {
                method: 'POST',
                body: JSON.stringify({ courseCode, courseName, departmentId, credits })
            });

            showToast('Course created successfully', 'success');
            closeModal();
            
            // Reload courses
            await this.loadCourses();
            
        } catch (error) {
            console.error('[Courses] Error creating course:', error);
            this.handleApiError(error, 'create course');
        }
    }

    /**
     * Show edit course modal
     */
    showEditCourseModal(courseId) {
        const course = this.allCourses.find(c => c.id === courseId);
        if (!course) {
            showToast('Course not found', 'error');
            return;
        }

        const departmentOptions = this.departments.map(dept => 
            `<option value="${dept.id}" ${course.department && course.department.id === dept.id ? 'selected' : ''}>
                ${this.escapeHtml(dept.name)}
            </option>`
        ).join('');
        
        const content = `
            <form id="editCourseForm" class="space-y-4">
                <div class="form-group">
                    <label for="editCourseCode" class="form-label">Course Code *</label>
                    <input 
                        type="text" 
                        id="editCourseCode" 
                        name="courseCode"
                        required
                        value="${this.escapeHtml(course.courseCode)}"
                        class="form-input"
                    >
                </div>
                <div class="form-group">
                    <label for="editCourseName" class="form-label">Course Name *</label>
                    <input 
                        type="text" 
                        id="editCourseName" 
                        name="courseName"
                        required
                        value="${this.escapeHtml(course.courseName)}"
                        class="form-input"
                    >
                </div>
                <div class="form-group">
                    <label for="editCourseDepartment" class="form-label">Department *</label>
                    <select 
                        id="editCourseDepartment" 
                        name="departmentId"
                        required
                        class="form-input"
                    >
                        <option value="">Select Department</option>
                        ${departmentOptions}
                    </select>
                </div>
                <div class="form-group">
                    <label for="editCourseCredits" class="form-label">Credits *</label>
                    <input 
                        type="number" 
                        id="editCourseCredits" 
                        name="credits"
                        required
                        min="1"
                        max="6"
                        value="${course.credits || 3}"
                        class="form-input"
                    >
                </div>
            </form>
        `;

        showModal('Edit Course', content, {
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
                    onClick: (close) => this.handleUpdateCourse(courseId, close)
                }
            ]
        });
    }

    /**
     * Handle update course
     */
    async handleUpdateCourse(courseId, closeModal) {
        const form = document.getElementById('editCourseForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const courseCode = document.getElementById('editCourseCode').value.trim();
        const courseName = document.getElementById('editCourseName').value.trim();
        const departmentId = parseInt(document.getElementById('editCourseDepartment').value);
        const credits = parseInt(document.getElementById('editCourseCredits').value);

        try {
            await apiRequest(`/deanship/courses/${courseId}`, {
                method: 'PUT',
                body: JSON.stringify({ courseCode, courseName, departmentId, credits })
            });

            showToast('Course updated successfully', 'success');
            closeModal();
            
            // Reload courses
            await this.loadCourses();
            
        } catch (error) {
            console.error('[Courses] Error updating course:', error);
            this.handleApiError(error, 'update course');
        }
    }

    /**
     * Deactivate course
     */
    async deactivateCourse(courseId) {
        try {
            await apiRequest(`/deanship/courses/${courseId}/deactivate`, {
                method: 'PUT'
            });

            showToast('Course deactivated successfully', 'success');
            
            // Reload courses
            await this.loadCourses();
            
        } catch (error) {
            console.error('[Courses] Error deactivating course:', error);
            this.handleApiError(error, 'deactivate course');
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
            console.error(`[Courses] Unauthorized: ${action}`);
            message = 'Your session has expired. Please log in again.';
            showToast(message, 'error');
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        } else if (error.status === 403) {
            console.error(`[Courses] Forbidden: ${action}`);
            message = 'You do not have permission to perform this action.';
            showToast(message, 'error');
        } else if (error.status === 500) {
            console.error(`[Courses] Server error: ${action}`, error);
            message = `Server error. Please try again later.`;
            showToast(message, 'error');
        } else if (error.message && error.message.includes('NetworkError')) {
            console.error(`[Courses] Network error: ${action}`, error);
            message = 'Network error. Please check your connection and try again.';
            showToast(message, 'error');
        } else {
            console.error(`[Courses] Error: ${action}`, error);
            message = error.message || message;
            showToast(message, 'error');
        }
    }

    /**
     * Handle general errors with user feedback
     */
    handleError(message, error) {
        console.error('[Courses] Error:', error);
        showToast(message, 'error');
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const page = new CoursesPage();
    page.initialize();
});

export default CoursesPage;
