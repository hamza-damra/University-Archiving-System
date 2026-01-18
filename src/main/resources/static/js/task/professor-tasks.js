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
                                    ${task.canBeDeleted ? `
                                    <button onclick="deleteTask(${task.id})" class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 flex items-center gap-2">
                                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                                        Delete
                                    </button>
                                    ` : ''}
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
                        <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2 mb-4">
                            <div class="bg-${statusColor}-500 h-2 rounded-full transition-all duration-500" style="width: ${task.progressPercentage}%"></div>
                        </div>

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
 * Delete task
 */
window.deleteTask = async function (taskId) {
    if (!confirm('Are you sure you want to delete this task?')) return;

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
