/**
 * HOD Task Review Module
 * Handles task review, approval, and rejection for HOD
 */

import { hod, apiRequest } from '../core/api.js';
import { showToast, formatDate } from '../core/ui.js';

let currentSemesterId = null;
let tasks = [];

/**
 * Initialize task review for HOD
 */
export function initializeHodTasks() {
    console.log('[HodTasks] Initializing task review module');
    
    const taskReviewTab = document.querySelector('[data-tab="task-review"]');
    const filterBtn = document.getElementById('taskFilterBtn');
    
    if (!taskReviewTab) {
        console.warn('[HodTasks] Task review tab element not found');
        return;
    }
    
    // Load filter options (courses and professors) on initialization
    loadFilterOptions();
    
    // Listen for tab switch
    taskReviewTab.addEventListener('click', () => {
        console.log('[HodTasks] Task review tab clicked, loading tasks');
        loadFilterOptions(); // Reload options when tab is clicked
        loadTasks();
    });
    
    // Listen for filter button
    if (filterBtn) {
        filterBtn.addEventListener('click', () => {
            console.log('[HodTasks] Filter button clicked, loading tasks');
            loadTasks();
        });
    }
    
    // Get semester from context
    const semesterSelect = document.getElementById('semesterSelect');
    if (semesterSelect) {
        semesterSelect.addEventListener('change', () => {
            currentSemesterId = semesterSelect.value ? parseInt(semesterSelect.value) : null;
            console.log('[HodTasks] Semester changed to:', currentSemesterId);
            loadFilterOptions(); // Reload options when semester changes
            loadTasks();
        });
        
        // Get initial semester value
        if (semesterSelect.value) {
            currentSemesterId = parseInt(semesterSelect.value);
            console.log('[HodTasks] Initial semester:', currentSemesterId);
        }
    }
    
    // Load tasks if Task Review tab is already active
    const taskReviewContent = document.getElementById('task-review-tab');
    if (taskReviewContent && !taskReviewContent.classList.contains('hidden')) {
        console.log('[HodTasks] Task review tab already visible, loading tasks');
        loadTasks();
    }
    
    console.log('[HodTasks] Task review module initialized');
}

/**
 * Load filter options (courses and professors) and populate dropdowns
 */
async function loadFilterOptions() {
    console.log('[HodTasks] Loading filter options');
    
    try {
        // apiRequest already unwraps the response data, so we receive the options directly
        const options = await hod.getReportFilterOptions();
        console.log('[HodTasks] Filter options:', options);
        
        if (options) {
            // Populate courses dropdown
            const courseSelect = document.getElementById('taskFilterCourse');
            if (courseSelect && options.courses) {
                // Keep the "All Courses" option
                const allCoursesOption = courseSelect.querySelector('option[value=""]');
                courseSelect.innerHTML = '';
                if (allCoursesOption) {
                    courseSelect.appendChild(allCoursesOption);
                } else {
                    const defaultOption = document.createElement('option');
                    defaultOption.value = '';
                    defaultOption.textContent = 'All Courses';
                    courseSelect.appendChild(defaultOption);
                }
                
                // Add course options
                options.courses.forEach(course => {
                    const option = document.createElement('option');
                    option.value = course.id;
                    option.textContent = `${course.courseCode} - ${course.courseName}`;
                    courseSelect.appendChild(option);
                });
                
                console.log('[HodTasks] Populated courses dropdown with', options.courses.length, 'courses');
            }
            
            // Populate professors dropdown
            const professorSelect = document.getElementById('taskFilterProfessor');
            if (professorSelect && options.professors) {
                // Keep the "All Professors" option
                const allProfessorsOption = professorSelect.querySelector('option[value=""]');
                professorSelect.innerHTML = '';
                if (allProfessorsOption) {
                    professorSelect.appendChild(allProfessorsOption);
                } else {
                    const defaultOption = document.createElement('option');
                    defaultOption.value = '';
                    defaultOption.textContent = 'All Professors';
                    professorSelect.appendChild(defaultOption);
                }
                
                // Add professor options
                options.professors.forEach(professor => {
                    const option = document.createElement('option');
                    option.value = professor.id;
                    option.textContent = professor.name;
                    professorSelect.appendChild(option);
                });
                
                console.log('[HodTasks] Populated professors dropdown with', options.professors.length, 'professors');
            }
        } else {
            console.warn('[HodTasks] No filter options received');
        }
    } catch (error) {
        console.error('[HodTasks] Error loading filter options:', error);
        showToast('Failed to load filter options', 'error');
    }
}

/**
 * Load tasks for HOD review
 */
async function loadTasks() {
    console.log('[HodTasks] loadTasks called');
    
    const tableBody = document.getElementById('taskReviewTableBody');
    const emptyState = document.getElementById('taskReviewEmptyState');
    
    if (!tableBody) {
        console.warn('[HodTasks] Task table body not found');
        return;
    }
    
    try {
        tableBody.innerHTML = '<tr><td colspan="8" class="px-4 py-8 text-center"><div class="animate-pulse">Loading tasks...</div></td></tr>';
        
        const courseId = document.getElementById('taskFilterCourse')?.value || null;
        const professorId = document.getElementById('taskFilterProfessor')?.value || null;
        const status = document.getElementById('taskFilterStatus')?.value || null;
        
        console.log('[HodTasks] Fetching tasks with filters:', { courseId, semesterId: currentSemesterId, professorId, status });
        
        const response = await hod.getTasks(
            courseId ? parseInt(courseId) : null,
            currentSemesterId,
            professorId ? parseInt(professorId) : null,
            status
        );
        
        console.log('[HodTasks] API response:', response);
        
        // Handle both wrapped response { success, data } and direct array response
        const taskData = response.success !== undefined ? response.data : response;
        
        if (Array.isArray(taskData)) {
            tasks = taskData;
            console.log('[HodTasks] Tasks loaded:', tasks.length, 'tasks');
            renderTasks(tasks);
            
            if (tasks.length === 0) {
                tableBody.parentElement.parentElement.classList.add('hidden');
                if (emptyState) emptyState.classList.remove('hidden');
            } else {
                tableBody.parentElement.parentElement.classList.remove('hidden');
                if (emptyState) emptyState.classList.add('hidden');
            }
        } else {
            console.warn('[HodTasks] API returned unexpected response:', response);
            tableBody.innerHTML = '<tr><td colspan="8" class="px-4 py-8 text-center text-gray-500">No tasks found</td></tr>';
        }
    } catch (error) {
        console.error('[HodTasks] Error loading tasks:', error);
        showToast('Failed to load tasks', 'error');
        tableBody.innerHTML = '<tr><td colspan="8" class="px-4 py-8 text-center text-red-500">Error loading tasks</td></tr>';
    }
}

/**
 * Render tasks table
 */
function renderTasks(tasksList) {
    const tableBody = document.getElementById('taskReviewTableBody');
    if (!tableBody) return;
    
    if (tasksList.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="8" class="px-4 py-8 text-center text-gray-500">No tasks found</td></tr>';
        return;
    }
    
    tableBody.innerHTML = tasksList.map(task => `
        <tr>
            <td class="font-medium">${escapeHtml(task.title)}</td>
            <td>${escapeHtml(task.professorName)}</td>
            <td>${escapeHtml(task.courseCode)}</td>
            <td>${task.weightPercentage}%</td>
            <td>
                <div class="flex items-center gap-2">
                    <div class="w-16 progress-container">
                        <div class="progress-bar progress-bar-${task.status.toLowerCase().replace('_', '-')}" 
                             style="width: ${task.progressPercentage}%"></div>
                    </div>
                    <span class="text-xs">${task.progressPercentage}%</span>
                </div>
            </td>
            <td>
                <span class="status-badge status-badge-${task.status.toLowerCase().replace('_', '-')}">
                    ${task.status.replace('_', ' ')}
                </span>
            </td>
            <td>${task.deadline ? formatDate(task.deadline) : '-'}</td>
            <td>
                <div class="flex items-center gap-2">
                    ${task.fileReferenceId ? `
                        <button onclick="viewTaskFile(${task.fileReferenceId})" 
                                class="px-2 py-1 text-xs text-blue-600 bg-blue-50 rounded hover:bg-blue-100">
                            View File
                        </button>
                    ` : ''}
                    ${task.status === 'COMPLETED' ? `
                        <button onclick="approveTask(${task.id})" 
                                class="px-2 py-1 text-xs text-white bg-emerald-600 rounded hover:bg-emerald-700">
                            Approve
                        </button>
                        <button onclick="rejectTask(${task.id})" 
                                class="px-2 py-1 text-xs text-white bg-rose-600 rounded hover:bg-rose-700">
                            Reject
                        </button>
                    ` : ''}
                </div>
            </td>
        </tr>
    `).join('');
}

/**
 * Approve task
 */
window.approveTask = async function(taskId) {
    if (!confirm('Are you sure you want to approve this task?')) return;
    
    try {
        await hod.approveTask(taskId);
        showToast('Task approved successfully', 'success');
        loadTasks();
    } catch (error) {
        console.error('Error approving task:', error);
        showToast('Failed to approve task', 'error');
    }
};

/**
 * Reject task
 */
window.rejectTask = async function(taskId) {
    const feedback = prompt('Please provide feedback for rejection (optional):');
    
    try {
        await hod.rejectTask(taskId, feedback);
        showToast('Task rejected successfully', 'success');
        loadTasks();
    } catch (error) {
        console.error('Error rejecting task:', error);
        showToast('Failed to reject task', 'error');
    }
};

/**
 * View task file
 */
window.viewTaskFile = async function(fileId) {
    try {
        const response = await hod.downloadFile(fileId);
        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            window.open(url, '_blank');
        }
    } catch (error) {
        console.error('Error viewing file:', error);
        showToast('Failed to view file', 'error');
    }
};

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
