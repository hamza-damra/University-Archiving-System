/**
 * Course Assignments Page Module
 * Manages course assignments to professors for specific semesters
 */

import { DeanshipLayout } from './deanship-common.js';
import { apiRequest } from './api.js';
import { showToast, showModal } from './ui.js';

/**
 * Course Assignments Page Class
 */
class CourseAssignmentsPage {
    constructor() {
        this.layout = new DeanshipLayout();
        this.assignments = [];
        this.allAssignments = []; // Store unfiltered list for filtering
        this.professors = [];
        this.courses = [];
        this.selectedProfessorId = null;
        this.selectedCourseId = null;
    }

    /**
     * Initialize the course assignments page
     */
    async initialize() {
        try {
            // Initialize shared layout
            await this.layout.initialize();
            
            // Load professors and courses for filters
            await this.loadProfessors();
            await this.loadCourses();
            
            // Set up event listeners
            this.setupEventListeners();
            
            // Register callback for context changes
            this.layout.onContextChange((context) => {
                this.handleContextChange(context);
            });
            
            // Check if context is available and load assignments
            if (this.layout.hasContext()) {
                await this.loadAssignments();
                this.showContent(true);
            } else {
                this.showContextMessage(true);
            }
            
            console.log('[CourseAssignments] Initialized successfully');
        } catch (error) {
            console.error('[CourseAssignments] Initialization error:', error);
            showToast('Failed to initialize course assignments page', 'error');
        }
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // Assign course button
        const assignBtn = document.getElementById('assignCourseBtn');
        if (assignBtn) {
            assignBtn.addEventListener('click', () => this.showAssignCourseModal());
        }
        
        // Professor filter
        const professorFilter = document.getElementById('professorFilter');
        if (professorFilter) {
            professorFilter.addEventListener('change', (e) => {
                this.selectedProfessorId = e.target.value ? parseInt(e.target.value) : null;
                this.applyFilters();
            });
        }
        
        // Course filter
        const courseFilter = document.getElementById('courseFilter');
        if (courseFilter) {
            courseFilter.addEventListener('change', (e) => {
                this.selectedCourseId = e.target.value ? parseInt(e.target.value) : null;
                this.applyFilters();
            });
        }
    }

    /**
     * Handle context change (academic year or semester)
     */
    async handleContextChange(context) {
        if (context.semesterId) {
            // Context is available, load assignments
            this.showContextMessage(false);
            await this.loadAssignments();
            this.showContent(true);
        } else {
            // No context, show message
            this.showContextMessage(true);
            this.showContent(false);
        }
    }

    /**
     * Load professors from API
     */
    async loadProfessors() {
        try {
            const response = await apiRequest('/deanship/professors', {
                method: 'GET',
            });
            
            this.professors = response.filter(prof => prof.active);
            this.renderProfessorFilter();
            
        } catch (error) {
            console.error('[CourseAssignments] Failed to load professors:', error);
            this.handleApiError(error, 'load professors');
        }
    }

    /**
     * Load courses from API
     */
    async loadCourses() {
        try {
            const response = await apiRequest('/deanship/courses', {
                method: 'GET',
            });
            
            this.courses = response.filter(course => course.active);
            this.renderCourseFilter();
            
        } catch (error) {
            console.error('[CourseAssignments] Failed to load courses:', error);
            this.handleApiError(error, 'load courses');
        }
    }

    /**
     * Render professor filter dropdown
     */
    renderProfessorFilter() {
        const select = document.getElementById('professorFilter');
        if (!select) return;
        
        // Keep "All Professors" option
        select.innerHTML = '<option value="">All Professors</option>';
        
        this.professors.forEach(prof => {
            const option = document.createElement('option');
            option.value = prof.id;
            option.textContent = prof.name;
            select.appendChild(option);
        });
    }

    /**
     * Render course filter dropdown
     */
    renderCourseFilter() {
        const select = document.getElementById('courseFilter');
        if (!select) return;
        
        // Keep "All Courses" option
        select.innerHTML = '<option value="">All Courses</option>';
        
        this.courses.forEach(course => {
            const option = document.createElement('option');
            option.value = course.id;
            option.textContent = `${course.courseCode} - ${course.courseName}`;
            select.appendChild(option);
        });
    }

    /**
     * Load assignments from API
     */
    async loadAssignments() {
        const context = this.layout.getSelectedContext();
        
        if (!context.semesterId) {
            console.log('[CourseAssignments] No semester selected, skipping load');
            return;
        }
        
        try {
            // Show loading state
            this.showLoading(true);
            
            // Build URL with semester parameter
            const url = `/deanship/course-assignments?semesterId=${context.semesterId}`;
            
            // Fetch assignments
            this.assignments = await apiRequest(url, {
                method: 'GET',
            });
            
            // Store unfiltered list
            this.allAssignments = [...this.assignments];
            
            // Hide loading state
            this.showLoading(false);
            
            // Apply any active filters
            this.applyFilters();
            
        } catch (error) {
            console.error('[CourseAssignments] Failed to load assignments:', error);
            this.showLoading(false);
            this.handleApiError(error, 'load course assignments');
            this.showEmptyState(true);
        }
    }

    /**
     * Apply professor and course filters
     */
    applyFilters() {
        // Start with all assignments
        this.assignments = [...this.allAssignments];
        
        // Apply professor filter
        if (this.selectedProfessorId) {
            this.assignments = this.assignments.filter(a => 
                a.professor && a.professor.id === this.selectedProfessorId
            );
        }
        
        // Apply course filter
        if (this.selectedCourseId) {
            this.assignments = this.assignments.filter(a => 
                a.course && a.course.id === this.selectedCourseId
            );
        }
        
        // Re-render table with filtered results
        this.renderAssignmentsTable();
    }

    /**
     * Render assignments table
     */
    renderAssignmentsTable() {
        const tbody = document.getElementById('assignmentsTableBody');
        const tableContainer = document.getElementById('tableContainer');
        const emptyState = document.getElementById('emptyState');
        
        if (!tbody) return;
        
        // Check if there are any assignments
        if (this.assignments.length === 0) {
            tableContainer.style.display = 'none';
            emptyState.style.display = 'block';
            return;
        }
        
        // Show table, hide empty state
        tableContainer.style.display = 'block';
        emptyState.style.display = 'none';
        
        // Render table rows
        tbody.innerHTML = this.assignments.map(assignment => this.createTableRow(assignment)).join('');
        
        // Add event listeners to action buttons
        this.attachRowEventListeners();
    }

    /**
     * Create HTML for a table row
     */
    createTableRow(assignment) {
        const professorName = assignment.professor ? this.escapeHtml(assignment.professor.name) : 'N/A';
        const courseCode = assignment.course ? this.escapeHtml(assignment.course.courseCode) : 'N/A';
        const courseName = assignment.course ? this.escapeHtml(assignment.course.courseName) : 'N/A';
        const departmentName = assignment.course && assignment.course.department 
            ? this.escapeHtml(assignment.course.department.name) 
            : 'N/A';
        const semesterName = assignment.semester ? this.escapeHtml(assignment.semester.name) : 'N/A';
        
        return `
            <tr>
                <td>${professorName}</td>
                <td>${courseCode}</td>
                <td>${courseName}</td>
                <td>${departmentName}</td>
                <td>${semesterName}</td>
                <td>
                    <div class="actions-cell">
                        <a class="action-link danger" data-action="unassign" data-id="${assignment.id}">Unassign</a>
                    </div>
                </td>
            </tr>
        `;
    }

    /**
     * Attach event listeners to row action buttons
     */
    attachRowEventListeners() {
        // Unassign buttons
        document.querySelectorAll('[data-action="unassign"]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const assignmentId = parseInt(btn.dataset.id);
                this.confirmUnassign(assignmentId);
            });
        });
    }

    /**
     * Show assign course modal
     */
    showAssignCourseModal() {
        const context = this.layout.getSelectedContext();
        
        if (!context.semesterId) {
            showToast('Please select an academic year and semester first', 'warning');
            return;
        }
        
        const professorOptions = this.professors.map(prof => 
            `<option value="${prof.id}">${this.escapeHtml(prof.name)}</option>`
        ).join('');
        
        const courseOptions = this.courses.map(course => 
            `<option value="${course.id}">${this.escapeHtml(course.courseCode)} - ${this.escapeHtml(course.courseName)}</option>`
        ).join('');
        
        const content = `
            <form id="assignCourseForm" class="space-y-4">
                <div class="form-group">
                    <label for="assignProfessor" class="form-label">Professor *</label>
                    <select 
                        id="assignProfessor" 
                        name="professorId"
                        required
                        class="form-input"
                    >
                        <option value="">Select Professor</option>
                        ${professorOptions}
                    </select>
                </div>
                <div class="form-group">
                    <label for="assignCourse" class="form-label">Course *</label>
                    <select 
                        id="assignCourse" 
                        name="courseId"
                        required
                        class="form-input"
                    >
                        <option value="">Select Course</option>
                        ${courseOptions}
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Semester</label>
                    <input 
                        type="text" 
                        value="${this.escapeHtml(context.semester.name)}"
                        class="form-input"
                        disabled
                    >
                    <p style="font-size: var(--font-size-sm); color: var(--color-text-secondary); margin-top: 4px;">
                        The course will be assigned for the currently selected semester.
                    </p>
                </div>
            </form>
        `;

        showModal('Assign Course', content, {
            buttons: [
                {
                    text: 'Cancel',
                    className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                    action: 'cancel',
                    onClick: (close) => close()
                },
                {
                    text: 'Assign',
                    className: 'bg-blue-600 text-white hover:bg-blue-700',
                    action: 'assign',
                    onClick: (close) => this.handleAssignCourse(close)
                }
            ]
        });
    }

    /**
     * Handle assign course
     */
    async handleAssignCourse(closeModal) {
        const form = document.getElementById('assignCourseForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const professorId = parseInt(document.getElementById('assignProfessor').value);
        const courseId = parseInt(document.getElementById('assignCourse').value);
        const context = this.layout.getSelectedContext();
        const semesterId = context.semesterId;

        try {
            await apiRequest('/deanship/course-assignments', {
                method: 'POST',
                body: JSON.stringify({ professorId, courseId, semesterId })
            });

            showToast('Course assigned successfully', 'success');
            closeModal();
            
            // Reload assignments
            await this.loadAssignments();
            
        } catch (error) {
            console.error('[CourseAssignments] Error assigning course:', error);
            this.handleApiError(error, 'assign course');
        }
    }

    /**
     * Confirm unassign with dialog
     */
    confirmUnassign(assignmentId) {
        const assignment = this.allAssignments.find(a => a.id === assignmentId);
        if (!assignment) {
            showToast('Assignment not found', 'error');
            return;
        }

        const professorName = assignment.professor ? assignment.professor.name : 'Unknown';
        const courseName = assignment.course ? `${assignment.course.courseCode} - ${assignment.course.courseName}` : 'Unknown';
        
        const content = `
            <div class="space-y-4">
                <p style="font-size: var(--font-size-md);">
                    Are you sure you want to unassign this course?
                </p>
                <div style="background-color: #f3f4f6; padding: var(--spacing-md); border-radius: var(--radius-md);">
                    <p style="margin-bottom: var(--spacing-xs);"><strong>Professor:</strong> ${this.escapeHtml(professorName)}</p>
                    <p><strong>Course:</strong> ${this.escapeHtml(courseName)}</p>
                </div>
                <p style="font-size: var(--font-size-sm); color: var(--color-text-secondary);">
                    This action cannot be undone.
                </p>
            </div>
        `;

        showModal('Confirm Unassign', content, {
            buttons: [
                {
                    text: 'Cancel',
                    className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                    action: 'cancel',
                    onClick: (close) => close()
                },
                {
                    text: 'Unassign',
                    className: 'bg-red-600 text-white hover:bg-red-700',
                    action: 'unassign',
                    onClick: (close) => this.handleUnassign(assignmentId, close)
                }
            ]
        });
    }

    /**
     * Handle unassign course
     */
    async handleUnassign(assignmentId, closeModal) {
        try {
            await apiRequest(`/deanship/course-assignments/${assignmentId}`, {
                method: 'DELETE'
            });

            showToast('Course unassigned successfully', 'success');
            closeModal();
            
            // Reload assignments
            await this.loadAssignments();
            
        } catch (error) {
            console.error('[CourseAssignments] Error unassigning course:', error);
            this.handleApiError(error, 'unassign course');
        }
    }

    /**
     * Show/hide context message
     */
    showContextMessage(show) {
        const contextMessage = document.getElementById('contextMessage');
        if (contextMessage) {
            contextMessage.style.display = show ? 'block' : 'none';
        }
    }

    /**
     * Show/hide content (filters and table)
     */
    showContent(show) {
        const filtersRow = document.getElementById('filtersRow');
        const tableContainer = document.getElementById('tableContainer');
        const emptyState = document.getElementById('emptyState');
        
        if (show) {
            if (filtersRow) filtersRow.style.display = 'flex';
        } else {
            if (filtersRow) filtersRow.style.display = 'none';
            if (tableContainer) tableContainer.style.display = 'none';
            if (emptyState) emptyState.style.display = 'none';
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
            console.error(`[CourseAssignments] Unauthorized: ${action}`);
            message = 'Your session has expired. Please log in again.';
            showToast(message, 'error');
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        } else if (error.status === 403) {
            console.error(`[CourseAssignments] Forbidden: ${action}`);
            message = 'You do not have permission to perform this action.';
            showToast(message, 'error');
        } else if (error.status === 500) {
            console.error(`[CourseAssignments] Server error: ${action}`, error);
            message = `Server error. Please try again later.`;
            showToast(message, 'error');
        } else if (error.message && error.message.includes('NetworkError')) {
            console.error(`[CourseAssignments] Network error: ${action}`, error);
            message = 'Network error. Please check your connection and try again.';
            showToast(message, 'error');
        } else {
            console.error(`[CourseAssignments] Error: ${action}`, error);
            message = error.message || message;
            showToast(message, 'error');
        }
    }

    /**
     * Handle general errors with user feedback
     */
    handleError(message, error) {
        console.error('[CourseAssignments] Error:', error);
        showToast(message, 'error');
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const page = new CourseAssignmentsPage();
    page.initialize();
});

export default CourseAssignmentsPage;
