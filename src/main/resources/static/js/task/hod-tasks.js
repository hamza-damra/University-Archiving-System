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
            
            // Refresh modern dropdowns if available
            if (window.refreshModernDropdown) {
                if (courseSelect) window.refreshModernDropdown(courseSelect);
                if (professorSelect) window.refreshModernDropdown(professorSelect);
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
        tableBody.innerHTML = '<tr><td colspan="8" class="px-6 py-12 text-center text-gray-500">No tasks found matching your criteria</td></tr>';
        return;
    }
    
    tableBody.innerHTML = tasksList.map(task => `
        <tr class="hover:bg-gray-50 transition-colors">
            <td class="px-6 py-4 text-sm font-medium text-gray-900">${escapeHtml(task.title)}</td>
            <td class="px-6 py-4 text-sm text-gray-600">${escapeHtml(task.professorName)}</td>
            <td class="px-6 py-4 text-sm text-gray-600">${escapeHtml(task.courseCode)}</td>
            <td class="px-6 py-4 text-sm text-gray-600 text-center">${task.weightPercentage}%</td>
            <td class="px-6 py-4 align-middle">
                <div class="w-full min-w-[120px]">
                    <div class="flex items-center justify-between mb-1">
                        <span class="text-xs font-medium text-gray-700">${task.progressPercentage}%</span>
                    </div>
                    <div class="w-full bg-gray-200 rounded-full h-1.5">
                        <div class="bg-blue-600 h-1.5 rounded-full transition-all duration-300" style="width: ${task.progressPercentage}%"></div>
                    </div>
                </div>
            </td>
            <td class="px-6 py-4">
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-${getStatusColor(task.status)}-100 text-${getStatusColor(task.status)}-800 border border-${getStatusColor(task.status)}-200">
                    ${formatStatus(task.status)}
                </span>
            </td>
            <td class="px-6 py-4 text-sm text-gray-600 whitespace-nowrap">${task.deadline ? formatDate(task.deadline) : '-'}</td>
            <td class="px-6 py-4 text-sm font-medium whitespace-nowrap">
                <div class="flex items-center space-x-2">
                    ${renderActions(task)}
                </div>
            </td>
        </tr>
    `).join('');
}

function getStatusColor(status) {
    const statusMap = {
        'PENDING': 'gray',
        'IN_PROGRESS': 'blue',
        'COMPLETED': 'green',
        'OVERDUE': 'red',
        'APPROVED': 'emerald',
        'REJECTED': 'rose'
    };
    return statusMap[status] || 'gray';
}

function formatStatus(status) {
    if (!status) return '';
    const word = status.replace(/_/g, ' ');
    return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
}

function renderActions(task) {
    let actions = '';
    
    // Evidence files button (shows count if available)
    if (task.evidenceCount > 0) {
        actions += `
            <button onclick="viewTaskEvidence(${task.id}, '${escapeHtml(task.title)}')" 
                    class="inline-flex items-center px-2.5 py-1.5 border border-transparent text-xs font-medium rounded text-emerald-700 bg-emerald-50 hover:bg-emerald-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 transition-colors"
                    title="View Evidence Files">
                <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"></path>
                </svg>
                Evidence (${task.evidenceCount})
            </button>
        `;
    }
    
    if (task.fileReferenceId) {
        actions += `
            <button onclick="viewTaskFile(${task.fileReferenceId})" 
                    class="inline-flex items-center px-2.5 py-1.5 border border-transparent text-xs font-medium rounded text-blue-700 bg-blue-50 hover:bg-blue-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                    title="View File">
                <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                View
            </button>
        `;
    }
    
    if (task.status === 'COMPLETED') {
        actions += `
            <button onclick="approveTask(${task.id})" 
                    class="inline-flex items-center px-2.5 py-1.5 border border-transparent text-xs font-medium rounded text-white bg-emerald-600 hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 transition-colors"
                    title="Approve Task">
                <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                Approve
            </button>
            <button onclick="rejectTask(${task.id})" 
                    class="inline-flex items-center px-2.5 py-1.5 border border-transparent text-xs font-medium rounded text-white bg-rose-600 hover:bg-rose-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-rose-500 transition-colors"
                    title="Reject Task">
                <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
                Reject
            </button>
        `;
    }
    
    return actions;
}

/**
 * Approve task
 */
window.approveTask = function(taskId) {
    showConfirmModal({
        title: 'Approve Task',
        message: 'Are you sure you want to approve this task? This action cannot be undone.',
        confirmText: 'Approve Task',
        confirmColor: 'emerald',
        onConfirm: async () => {
            try {
                await hod.approveTask(taskId);
                showToast('Task approved successfully', 'success');
                loadTasks();
            } catch (error) {
                console.error('Error approving task:', error);
                showToast('Failed to approve task', 'error');
            }
        }
    });
};

/**
 * Reject task
 */
window.rejectTask = function(taskId) {
    showConfirmModal({
        title: 'Reject Task',
        message: 'Are you sure you want to reject this task? Please provide a reason to help the professor understand what needs to be improved.',
        confirmText: 'Reject Task',
        confirmColor: 'rose',
        type: 'input',
        onConfirm: async (feedback) => {
            try {
                await hod.rejectTask(taskId, feedback);
                showToast('Task rejected successfully', 'success');
                loadTasks();
            } catch (error) {
                console.error('Error rejecting task:', error);
                showToast('Failed to reject task', 'error');
            }
        }
    });
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
 * View task evidence files
 */
window.viewTaskEvidence = async function(taskId, taskTitle) {
    try {
        const evidence = await hod.getTaskEvidence(taskId);
        
        if (!evidence || evidence.length === 0) {
            showToast('No evidence files found', 'info');
            return;
        }
        
        showEvidenceModal(taskTitle, evidence);
    } catch (error) {
        console.error('Error loading evidence:', error);
        showToast('Failed to load evidence files', 'error');
    }
};

/**
 * Show evidence files modal
 */
function showEvidenceModal(taskTitle, evidenceFiles) {
    // Remove existing modal if any
    const existingModal = document.getElementById('evidence-modal');
    if (existingModal) existingModal.remove();
    
    const modalHtml = `
        <div id="evidence-modal" class="fixed inset-0 z-[100] overflow-y-auto" aria-labelledby="modal-title" role="dialog" aria-modal="true">
            <div class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
                <div class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity backdrop-blur-sm" aria-hidden="true" onclick="document.getElementById('evidence-modal').remove()"></div>
                <span class="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>
                <div class="inline-block align-bottom bg-white rounded-xl text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-xl sm:w-full animate-[fadeIn_0.2s_ease-out]">
                    <!-- Header -->
                    <div class="px-6 py-4 border-b border-gray-200 bg-gray-50">
                        <div class="flex items-center justify-between">
                            <div>
                                <h3 class="text-lg font-semibold text-gray-900 flex items-center gap-2">
                                    <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"></path>
                                    </svg>
                                    Evidence Files
                                </h3>
                                <p class="text-sm text-gray-500 mt-1">Task: ${escapeHtml(taskTitle)}</p>
                            </div>
                            <button onclick="document.getElementById('evidence-modal').remove()" class="p-2 text-gray-400 hover:text-gray-500 rounded-lg hover:bg-gray-100 transition-colors">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                                </svg>
                            </button>
                        </div>
                    </div>
                    
                    <!-- File List -->
                    <div class="px-6 py-4 max-h-96 overflow-y-auto">
                        <div class="space-y-3">
                            ${evidenceFiles.map(file => `
                                <div class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg border border-gray-200 hover:border-gray-300 transition-colors">
                                    <div class="flex-shrink-0">
                                        ${getFileIcon(file.fileType)}
                                    </div>
                                    <div class="flex-1 min-w-0">
                                        <p class="text-sm font-medium text-gray-900 truncate" title="${escapeHtml(file.fileName)}">
                                            ${escapeHtml(file.fileName)}
                                        </p>
                                        <p class="text-xs text-gray-500">
                                            ${formatFileSize(file.fileSize)} ${file.attachedAt ? '• Attached ' + formatDateTime(file.attachedAt) : ''}
                                        </p>
                                        ${!file.fileExists ? '<p class="text-xs text-amber-600 mt-0.5">File may have been moved or deleted</p>' : ''}
                                    </div>
                                    <div class="flex-shrink-0 flex gap-2">
                                        ${file.fileExists && file.fileId ? `
                                            <button onclick="downloadEvidenceFile(${file.fileId})" 
                                                    class="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                                    title="Download">
                                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                                                </svg>
                                            </button>
                                            <button onclick="previewEvidenceFile(${file.fileId})" 
                                                    class="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                                                    title="Preview">
                                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                                                </svg>
                                            </button>
                                        ` : ''}
                                    </div>
                                </div>
                            `).join('')}
                        </div>
                    </div>
                    
                    <!-- Footer -->
                    <div class="px-6 py-4 bg-gray-50 border-t border-gray-200 flex justify-end">
                        <button onclick="document.getElementById('evidence-modal').remove()" 
                                class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors">
                            Close
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
}

/**
 * Download evidence file
 */
window.downloadEvidenceFile = async function(fileId) {
    try {
        const response = await hod.downloadFile(fileId);
        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = '';
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
        } else {
            showToast('Failed to download file', 'error');
        }
    } catch (error) {
        console.error('Error downloading file:', error);
        showToast('Failed to download file', 'error');
    }
};

/**
 * Preview evidence file
 */
window.previewEvidenceFile = async function(fileId) {
    try {
        const response = await hod.downloadFile(fileId);
        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            window.open(url, '_blank');
        } else {
            showToast('Failed to preview file', 'error');
        }
    } catch (error) {
        console.error('Error previewing file:', error);
        showToast('Failed to preview file', 'error');
    }
};

/**
 * Get file icon based on type
 */
function getFileIcon(fileType) {
    const type = (fileType || '').toLowerCase();
    
    if (type.includes('pdf')) {
        return `<div class="w-10 h-10 bg-red-100 rounded-lg flex items-center justify-center">
            <svg class="w-5 h-5 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/>
            </svg>
        </div>`;
    }
    if (type.includes('image') || type.includes('png') || type.includes('jpg') || type.includes('jpeg')) {
        return `<div class="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
            <svg class="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
            </svg>
        </div>`;
    }
    if (type.includes('word') || type.includes('doc')) {
        return `<div class="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
            <svg class="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/>
            </svg>
        </div>`;
    }
    if (type.includes('excel') || type.includes('spreadsheet') || type.includes('xls')) {
        return `<div class="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
            <svg class="w-5 h-5 text-green-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/>
            </svg>
        </div>`;
    }
    
    // Default file icon
    return `<div class="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center">
        <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
        </svg>
    </div>`;
}

/**
 * Format file size
 */
function formatFileSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

/**
 * Format date time
 */
function formatDateTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Show a custom confirmation modal
 */
function showConfirmModal({ title, message, type = 'confirm', confirmText = 'Confirm', cancelText = 'Cancel', confirmColor = 'blue', onConfirm }) {
    // Remove existing modal if any
    const existingModal = document.getElementById('custom-modal');
    if (existingModal) existingModal.remove();

    const isInput = type === 'input';
    
    // Icon selection based on color/intent
    let iconPath = 'M5 13l4 4L19 7'; // Checkmark default
    if (confirmColor === 'rose' || confirmColor === 'red') {
        iconPath = 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z'; // Warning triangle
    }

    const modalHtml = `
        <div id="custom-modal" class="fixed inset-0 z-[100] overflow-y-auto" aria-labelledby="modal-title" role="dialog" aria-modal="true">
            <div class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
                <div class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" aria-hidden="true" onclick="document.getElementById('custom-modal').remove()"></div>
                <span class="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>
                <div class="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full animate-[fadeIn_0.2s_ease-out]">
                    <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                        <div class="sm:flex sm:items-start">
                            <div class="mx-auto flex-shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-${confirmColor}-100 sm:mx-0 sm:h-10 sm:w-10">
                                <svg class="h-6 w-6 text-${confirmColor}-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="${iconPath}" />
                                </svg>
                            </div>
                            <div class="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left w-full">
                                <h3 class="text-lg leading-6 font-medium text-gray-900" id="modal-title">${title}</h3>
                                <div class="mt-2">
                                    <p class="text-sm text-gray-500">${message}</p>
                                    ${isInput ? `
                                        <div class="mt-3">
                                            <textarea id="modal-input" rows="3" class="shadow-sm focus:ring-${confirmColor}-500 focus:border-${confirmColor}-500 block w-full sm:text-sm border border-gray-300 rounded-md p-2" placeholder="Enter reason (optional)..."></textarea>
                                        </div>
                                    ` : ''}
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse gap-2">
                        <button type="button" id="modal-confirm-btn" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-${confirmColor}-600 text-base font-medium text-white hover:bg-${confirmColor}-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-${confirmColor}-500 sm:w-auto sm:text-sm">
                            ${confirmText}
                        </button>
                        <button type="button" id="modal-cancel-btn" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:w-auto sm:text-sm">
                            ${cancelText}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHtml);

    const modal = document.getElementById('custom-modal');
    const confirmBtn = document.getElementById('modal-confirm-btn');
    const cancelBtn = document.getElementById('modal-cancel-btn');
    const input = document.getElementById('modal-input');
    
    // Focus handles
    if (input) {
        setTimeout(() => input.focus(), 50);
    } else {
        setTimeout(() => confirmBtn.focus(), 50);
    }

    const close = () => {
        modal.remove();
        document.removeEventListener('keydown', keyHandler);
    };

    const confirmHandler = () => {
        const value = isInput ? (input.value || '') : true;
        onConfirm(value);
        close();
    };

    const keyHandler = (e) => {
        if (e.key === 'Escape') {
            close();
        }
    };

    confirmBtn.addEventListener('click', confirmHandler);
    cancelBtn.addEventListener('click', close);
    document.addEventListener('keydown', keyHandler);
}
