/**
 * Professor Task Management Module
 * Handles task creation, editing, progress tracking, and file linking
 */

import { professor, apiRequest } from '../core/api.js';
import { showToast, formatDate } from '../core/ui.js';
import { openTaskEditor } from './task-editor.js';
import { getStatusColor, initializeDropdowns } from './professor-tasks-helpers.js';


let currentSemesterId = null;
let currentCourseId = null;
let tasks = [];

/**
 * Initialize task management for professor
 */
export function initializeProfessorTasks() {
    const tasksTab = document.getElementById('tasksTab');
    const tasksTabContent = document.getElementById('tasksTabContent');
    const createTaskBtn = document.getElementById('createTaskBtn');

    if (!tasksTab || !tasksTabContent) return;

    // Get semester from context (shared with other tabs)
    const semesterSelect = document.getElementById('semesterSelect');
    if (semesterSelect) {
        // Initialize currentSemesterId from current value
        if (semesterSelect.value) {
            currentSemesterId = parseInt(semesterSelect.value);
            console.log('[ProfessorTasks] Initial semester ID:', currentSemesterId);
        }

        // Listen for changes
        semesterSelect.addEventListener('change', () => {
            currentSemesterId = semesterSelect.value ? parseInt(semesterSelect.value) : null;
            console.log('[ProfessorTasks] Semester changed to:', currentSemesterId);
            loadTasks();
        });
    }

    // Listen for tab switch
    tasksTab.addEventListener('click', () => {
        // Re-sync semester ID in case it was changed by another module
        if (semesterSelect && semesterSelect.value) {
            currentSemesterId = parseInt(semesterSelect.value);
        }
        loadTasks();
    });

    // Listen for create task button
    if (createTaskBtn) {
        createTaskBtn.addEventListener('click', () => {
            // Re-sync semester ID before opening modal
            if (semesterSelect && semesterSelect.value) {
                currentSemesterId = parseInt(semesterSelect.value);
            }
            openTaskEditorModal();
        });
    }
}

/**
 * Load tasks for current professor
 */
async function loadTasks() {
    const container = document.getElementById('tasksContainer');
    const emptyState = document.getElementById('tasksEmptyState');

    if (!container) return;

    if (!currentSemesterId) {
        container.innerHTML = '<p class="text-gray-500 text-center py-8">Please select a semester to view tasks.</p>';
        return;
    }

    try {
        container.innerHTML = '<div class="animate-pulse space-y-4"><div class="h-32 bg-gray-200 rounded-lg"></div></div>';

        // API returns tasks array directly (unwrapped by apiRequest)
        tasks = await professor.getTasks(null, currentSemesterId);

        // Handle case where tasks might be null/undefined
        if (!Array.isArray(tasks)) {
            tasks = [];
        }

        renderTasks(tasks);
        renderProgressWidget(tasks);

        if (tasks.length === 0) {
            container.classList.add('hidden');
            if (emptyState) emptyState.classList.remove('hidden');
        } else {
            container.classList.remove('hidden');
            if (emptyState) emptyState.classList.add('hidden');
        }
    } catch (error) {
        console.error('Error loading tasks:', error);
        showToast('Failed to load tasks', 'error');
        container.innerHTML = '<p class="text-red-500 text-center py-8">Error loading tasks. Please try again.</p>';
    }
}

/**
 * Render tasks list
 */
function renderTasks(tasksList) {
    const container = document.getElementById('tasksContainer');
    if (!container) return;

    if (tasksList.length === 0) {
        container.innerHTML = '<p class="text-gray-500 text-center py-8">No tasks found.</p>';
        return;
    }

    container.innerHTML = `<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        ${tasksList.map(task => {
        const statusColor = getStatusColor(task.status);
        const statusLabel = task.status.replace(/_/g, ' ');
        const formattedDate = task.deadline ? formatDate(task.deadline) : 'No deadline';

        return `
            <div class="task-card group relative overflow-hidden bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm hover:shadow-lg transition-all duration-300">
                <!-- Status Stripe -->
                <div class="absolute top-0 left-0 w-1.5 h-full bg-${statusColor}-500 transition-colors"></div>
                
                <div class="p-5 pl-7 flex flex-col h-full">
                    <!-- Header -->
                    <div class="flex justify-between items-start mb-3">
                        <div>
                            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-${statusColor}-50 text-${statusColor}-700 dark:bg-${statusColor}-900/30 dark:text-${statusColor}-300 mb-2">
                                ${statusLabel}
                            </span>
                            <h3 class="text-lg font-bold text-gray-900 dark:text-white leading-tight group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors line-clamp-1" title="${escapeHtml(task.title)}">
                                ${escapeHtml(task.title)}
                            </h3>
                        </div>
                        <div class="flex items-center gap-1">
                             <div class="relative">
                                <button class="p-1.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors" data-dropdown-trigger="task-menu-${task.id}">
                                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z"></path>
                                    </svg>
                                </button>
                                <!-- Dropdown Menu -->
                                <div id="task-menu-${task.id}" class="hidden absolute right-0 top-full mt-1 w-36 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 z-10 py-1">
                                    <button onclick="editTask(${task.id})" class="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-750 flex items-center gap-2">
                                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path></svg>
                                        Edit
                                    </button>
                                    <button onclick="deleteTask(${task.id})" class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 flex items-center gap-2">
                                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                                        Delete
                                    </button>
                                </div>
                             </div>
                        </div>
                    </div>

                    <!-- Description -->
                    <p class="text-sm text-gray-600 dark:text-gray-400 mb-4 line-clamp-2 min-h-[2.5rem]">
                        ${task.description ? escapeHtml(task.description) : '<span class="italic text-gray-400">No description provided</span>'}
                    </p>

                    <!-- Meta Grid -->
                    <div class="grid grid-cols-2 gap-3 mb-4">
                        <div class="bg-gray-50 dark:bg-gray-700/50 p-2 rounded-lg">
                            <p class="text-xs text-gray-500 dark:text-gray-400 mb-0.5">Deadline</p>
                            <div class="flex items-center gap-1.5 text-sm font-medium text-gray-800 dark:text-gray-200">
                                <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                                </svg>
                                ${formattedDate}
                            </div>
                        </div>
                         <div class="bg-gray-50 dark:bg-gray-700/50 p-2 rounded-lg">
                            <p class="text-xs text-gray-500 dark:text-gray-400 mb-0.5">Course</p>
                            <div class="flex items-center gap-1.5 text-sm font-medium text-gray-800 dark:text-gray-200">
                                <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path>
                                </svg>
                                ${escapeHtml(task.courseCode)}
                            </div>
                        </div>
                    </div>

                    <!-- Progress & Actions -->
                    <div class="mt-auto pt-4 border-t border-gray-100 dark:border-gray-700">
                         <div class="flex justify-between items-center mb-1.5">
                            <span class="text-xs font-semibold text-gray-700 dark:text-gray-300">Progress (${task.progressPercentage}%)</span>
                            <span class="text-xs text-gray-500">Weight: ${task.weightPercentage}%</span>
                        </div>
                        <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2 mb-3">
                            <div class="bg-${statusColor}-500 h-2 rounded-full transition-all duration-500" style="width: ${task.progressPercentage}%"></div>
                        </div>

                        ${task.evidenceCount > 0 ? `
                            <div class="flex items-center gap-2 mb-3 p-2 bg-emerald-50 dark:bg-emerald-900/20 rounded-lg border border-emerald-200 dark:border-emerald-800">
                                <svg class="w-4 h-4 text-emerald-600 dark:text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"></path>
                                </svg>
                                <span class="text-xs font-medium text-emerald-700 dark:text-emerald-300">${task.evidenceCount} evidence file${task.evidenceCount > 1 ? 's' : ''} attached</span>
                            </div>
                        ` : ''}

                        ${task.fileReferenceId ? `
                            <button onclick="downloadTaskFile(${task.fileReferenceId})" 
                                    class="w-full flex items-center justify-center gap-2 px-3 py-2 text-sm font-medium text-blue-600 bg-blue-50 dark:bg-blue-900/20 dark:text-blue-400 rounded-lg hover:bg-blue-100 dark:hover:bg-blue-900/40 transition-colors">
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10"></path></svg>
                                Download Attachment
                            </button>
                        ` : ''}
                    </div>
                </div>
            </div>
            `;
    }).join('')}
    </div>`;

    // Initialize dropdowns
    initializeDropdowns();
}

/**
 * Open task editor modal
 * @param {number|null} taskId - Task ID for editing, null for creating
 */
function openTaskEditorModal(taskId = null) {
    openTaskEditor(taskId, currentSemesterId, () => {
        // Callback to reload tasks after save
        loadTasks();
    });
}

/**
 * Edit task
 */
window.editTask = async function (taskId) {
    openTaskEditorModal(taskId);
};

/**
 * Delete task with styled confirmation modal
 */
window.deleteTask = async function (taskId) {
    // Find the task to get its details for the confirmation message
    const task = tasks.find(t => t.id === taskId);
    const taskTitle = task ? task.title : 'this task';
    const taskProgress = task ? task.progressPercentage : 0;
    
    // Show styled confirmation modal
    const confirmed = await showDeleteConfirmationModal(taskTitle, taskProgress);
    if (!confirmed) return;

    try {
        await professor.deleteTask(taskId);
        showToast('Task deleted successfully', 'success');
        loadTasks();
    } catch (error) {
        console.error('Error deleting task:', error);
        showToast('Failed to delete task', 'error');
    }
};

/**
 * Show a styled confirmation modal for task deletion
 * @param {string} taskTitle - The title of the task being deleted
 * @param {number} taskProgress - The progress percentage of the task
 * @returns {Promise<boolean>} - True if user confirmed, false otherwise
 */
function showDeleteConfirmationModal(taskTitle, taskProgress) {
    return new Promise((resolve) => {
        // Create modal backdrop
        const backdrop = document.createElement('div');
        backdrop.id = 'deleteTaskModal';
        backdrop.className = 'fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4';
        backdrop.style.animation = 'fadeIn 0.2s ease-out';
        
        // Build warning message based on progress
        let warningMessage = '';
        if (taskProgress > 0) {
            warningMessage = `
                <div class="flex items-start gap-3 p-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg mb-4">
                    <svg class="w-5 h-5 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                    </svg>
                    <div>
                        <p class="text-sm font-medium text-amber-800 dark:text-amber-300">This task has ${taskProgress}% progress</p>
                        <p class="text-xs text-amber-700 dark:text-amber-400 mt-1">Deleting it will reduce your overall semester progress.</p>
                    </div>
                </div>
            `;
        }
        
        backdrop.innerHTML = `
            <div class="bg-white dark:bg-gray-800 rounded-xl shadow-2xl max-w-md w-full overflow-hidden transform transition-all" style="animation: slideUp 0.3s ease-out">
                <!-- Header -->
                <div class="flex items-center gap-4 p-6 border-b border-gray-200 dark:border-gray-700">
                    <div class="flex-shrink-0 w-12 h-12 bg-red-100 dark:bg-red-900/30 rounded-full flex items-center justify-center">
                        <svg class="w-6 h-6 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                        </svg>
                    </div>
                    <div>
                        <h3 class="text-lg font-semibold text-gray-900 dark:text-white">Delete Task</h3>
                        <p class="text-sm text-gray-500 dark:text-gray-400">This action cannot be undone</p>
                    </div>
                </div>
                
                <!-- Body -->
                <div class="p-6">
                    <p class="text-gray-700 dark:text-gray-300 mb-4">
                        Are you sure you want to delete <span class="font-semibold text-gray-900 dark:text-white">"${escapeHtml(taskTitle)}"</span>?
                    </p>
                    ${warningMessage}
                </div>
                
                <!-- Footer -->
                <div class="flex items-center justify-end gap-3 p-6 bg-gray-50 dark:bg-gray-900/50 border-t border-gray-200 dark:border-gray-700">
                    <button id="cancelDeleteBtn" class="px-4 py-2.5 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800">
                        Cancel
                    </button>
                    <button id="confirmDeleteBtn" class="px-4 py-2.5 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 transition-colors focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800">
                        Delete Task
                    </button>
                </div>
            </div>
        `;
        
        // Add modal to page
        document.body.appendChild(backdrop);
        
        // Add animation styles if not already present
        if (!document.getElementById('deleteModalStyles')) {
            const styles = document.createElement('style');
            styles.id = 'deleteModalStyles';
            styles.textContent = `
                @keyframes fadeIn {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }
                @keyframes slideUp {
                    from { opacity: 0; transform: translateY(20px) scale(0.95); }
                    to { opacity: 1; transform: translateY(0) scale(1); }
                }
            `;
            document.head.appendChild(styles);
        }
        
        // Handle button clicks
        const confirmBtn = backdrop.querySelector('#confirmDeleteBtn');
        const cancelBtn = backdrop.querySelector('#cancelDeleteBtn');
        
        const cleanup = () => {
            backdrop.remove();
        };
        
        confirmBtn.addEventListener('click', () => {
            cleanup();
            resolve(true);
        });
        
        cancelBtn.addEventListener('click', () => {
            cleanup();
            resolve(false);
        });
        
        // Close on backdrop click
        backdrop.addEventListener('click', (e) => {
            if (e.target === backdrop) {
                cleanup();
                resolve(false);
            }
        });
        
        // Close on Escape key
        const handleKeyDown = (e) => {
            if (e.key === 'Escape') {
                cleanup();
                resolve(false);
                document.removeEventListener('keydown', handleKeyDown);
            }
        };
        document.addEventListener('keydown', handleKeyDown);
        
        // Focus the cancel button by default (safer option)
        cancelBtn.focus();
    });
}

/**
 * Download task file
 */
window.downloadTaskFile = async function (fileId) {
    try {
        const response = await professor.downloadSubmissionFile(fileId);
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
        }
    } catch (error) {
        console.error('Error downloading file:', error);
        showToast('Failed to download file', 'error');
    }
};

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Render the progress widget professionally
 */
function renderProgressWidget(tasksList) {
    const widget = document.getElementById('tasksProgressWidget');
    if (!widget) return;

    if (!tasksList || tasksList.length === 0) {
        widget.classList.add('hidden');
        return;
    }

    widget.classList.remove('hidden');

    // Calculate stats
    const totalTasks = tasksList.length;
    const completedTasks = tasksList.filter(t => t.status === 'COMPLETED').length;
    const inProgressTasks = tasksList.filter(t => t.status === 'IN_PROGRESS').length;
    const pendingTasks = tasksList.filter(t => t.status === 'PENDING').length;
    const overdueTasks = tasksList.filter(t => t.status === 'OVERDUE').length; // Assuming OVERDUE is a status

    // Calculate weighted progress
    let totalWeight = 0;
    let weightedProgressSum = 0;

    tasksList.forEach(task => {
        const weight = task.weightPercentage || 0;
        const progress = task.progressPercentage || 0;
        totalWeight += weight;
        weightedProgressSum += (progress * weight);
    });

    // If total weight is 0 (or very small), use simple average
    let overallProgress = 0;
    if (totalWeight > 0) {
        overallProgress = Math.round(weightedProgressSum / Math.max(totalWeight, 100)); // Normalize to 100% max weight
    } else {
        // Fallback to simple average of progress
        const totalProgressSum = tasksList.reduce((acc, t) => acc + (t.progressPercentage || 0), 0);
        overallProgress = Math.round(totalProgressSum / totalTasks);
    }
    
    // Cap at 100
    overallProgress = Math.min(100, overallProgress);

    // Color for the circular progress based on value
    let progressColor = 'blue';
    if (overallProgress >= 100) progressColor = 'emerald';
    else if (overallProgress >= 75) progressColor = 'blue';
    else if (overallProgress >= 40) progressColor = 'indigo';
    else progressColor = 'amber';

    const circumference = 2 * Math.PI * 36; // r=36
    const dashOffset = circumference - (overallProgress / 100) * circumference;

    widget.innerHTML = `
        <div class="flex flex-col md:flex-row items-center justify-between gap-8">
            <!-- Left: Circular Progress -->
            <div class="flex items-center gap-6 md:border-r md:border-gray-200 dark:md:border-gray-700 md:pr-8 md:mr-4 min-w-[280px]">
                <div class="relative w-24 h-24 flex-shrink-0">
                    <svg class="w-full h-full transform -rotate-90" viewBox="0 0 80 80">
                        <!-- Background Circle -->
                        <circle cx="40" cy="40" r="36" fill="transparent" stroke="currentColor" stroke-width="8" class="text-gray-100 dark:text-gray-700"></circle>
                        <!-- Progress Circle -->
                        <circle cx="40" cy="40" r="36" fill="transparent" stroke="currentColor" stroke-width="8" 
                            stroke-dasharray="${circumference}" stroke-dashoffset="${dashOffset}" 
                            stroke-linecap="round"
                            class="text-${progressColor}-600 transition-all duration-1000 ease-out"></circle>
                    </svg>
                    <div class="absolute inset-0 flex items-center justify-center flex-col">
                        <span class="text-xl font-bold text-gray-900 dark:text-white">${overallProgress}%</span>
                    </div>
                </div>
                <div>
                    <h3 class="text-lg font-bold text-gray-900 dark:text-white">Semester Progress</h3>
                    <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">Based on task weights</p>
                    <div class="mt-2 flex items-center gap-2">
                        <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300">
                            ${totalTasks} Tasks Total
                        </span>
                    </div>
                </div>
            </div>

            <!-- Right: Detailed Stats Grid -->
            <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 w-full">
                <div class="bg-gray-50 dark:bg-gray-700/30 p-4 rounded-lg border border-gray-100 dark:border-gray-700 hover:border-blue-200 dark:hover:border-blue-800 transition-colors">
                    <div class="flex items-center gap-3 mb-2">
                        <div class="p-2 bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 rounded-lg">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
                        </div>
                        <span class="text-sm font-medium text-gray-600 dark:text-gray-400">In Progress</span>
                    </div>
                    <div class="text-2xl font-bold text-gray-900 dark:text-white pl-1">${inProgressTasks}</div>
                </div>

                <div class="bg-emerald-50 dark:bg-emerald-900/10 p-4 rounded-lg border border-emerald-100 dark:border-emerald-800/30 hover:border-emerald-200 dark:hover:border-emerald-700 transition-colors">
                    <div class="flex items-center gap-3 mb-2">
                        <div class="p-2 bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 rounded-lg">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        </div>
                        <span class="text-sm font-medium text-gray-600 dark:text-gray-400">Completed</span>
                    </div>
                    <div class="text-2xl font-bold text-gray-900 dark:text-white pl-1">${completedTasks}</div>
                </div>

                <div class="bg-gray-50 dark:bg-gray-700/30 p-4 rounded-lg border border-gray-100 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600 transition-colors">
                    <div class="flex items-center gap-3 mb-2">
                         <div class="p-2 bg-gray-200 dark:bg-gray-600 text-gray-600 dark:text-gray-300 rounded-lg">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        </div>
                        <span class="text-sm font-medium text-gray-600 dark:text-gray-400">Pending</span>
                    </div>
                    <div class="text-2xl font-bold text-gray-900 dark:text-white pl-1">${pendingTasks}</div>
                </div>

                <div class="bg-red-50 dark:bg-red-900/10 p-4 rounded-lg border border-red-100 dark:border-red-800/30 hover:border-red-200 dark:hover:border-red-700 transition-colors">
                    <div class="flex items-center gap-3 mb-2">
                         <div class="p-2 bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 rounded-lg">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
                        </div>
                        <span class="text-sm font-medium text-gray-600 dark:text-gray-400">Overdue</span>
                    </div>
                    <div class="text-2xl font-bold text-gray-900 dark:text-white pl-1">${overdueTasks}</div>
                </div>
            </div>
        </div>
    `;
}
