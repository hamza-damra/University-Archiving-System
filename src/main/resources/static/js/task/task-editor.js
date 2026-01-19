/**
 * Task Editor Module
 * Modal interface for creating and editing tasks
 */

import { professor } from '../core/api.js';
import { showToast, formatDate } from '../core/ui.js';

let modalElement = null;
let currentTask = null;
let currentSemesterId = null;
let courses = [];
let weightSummary = null;
let onSaveCallback = null;

// Date picker state
let datePickerModal = null;
let selectedDate = null;
let currentMonth = new Date().getMonth();
let currentYear = new Date().getFullYear();

// Evidence file state
let filePickerModal = null;
let availableFiles = [];
let selectedEvidenceFiles = []; // Array of file objects with id, name, etc.

/**
 * Open the task editor modal
 * @param {number|null} taskId - Task ID for editing, null for creating
 * @param {number} semesterId - Current semester ID
 * @param {Function} onSave - Callback when task is saved
 */
export async function openTaskEditor(taskId = null, semesterId = null, onSave = null) {
    currentTask = null;
    currentSemesterId = semesterId;
    onSaveCallback = onSave;

    if (!semesterId) {
        showToast('Please select a semester first', 'warning');
        return;
    }

    // Load courses first
    try {
        courses = await professor.getMyCourses(semesterId);
        if (!courses || courses.length === 0) {
            showToast('No courses found for this semester', 'warning');
            return;
        }
    } catch (error) {
        console.error('Error loading courses:', error);
        showToast('Failed to load courses', 'error');
        return;
    }

    // If editing, load the task and its evidence
    if (taskId) {
        try {
            currentTask = await professor.getTask(taskId);
            // Load existing evidence files
            if (currentTask.evidenceFiles && currentTask.evidenceFiles.length > 0) {
                selectedEvidenceFiles = currentTask.evidenceFiles.map(e => ({
                    id: e.fileId,
                    name: e.fileName,
                    size: e.fileSize,
                    type: e.fileType,
                    evidenceId: e.id
                }));
            } else {
                selectedEvidenceFiles = [];
            }
        } catch (error) {
            console.error('Error loading task:', error);
            showToast('Failed to load task', 'error');
            return;
        }
    } else {
        selectedEvidenceFiles = [];
    }

    renderModal();

    // If editing and task has a course, load weight summary
    if (currentTask && currentTask.courseId) {
        await loadWeightSummary(currentTask.courseId);
    }
}

/**
 * Close the task editor modal
 */
export function closeTaskEditor() {
    // Close date picker if open
    if (datePickerModal) {
        datePickerModal.remove();
        datePickerModal = null;
    }
    
    // Close file picker if open
    if (filePickerModal) {
        filePickerModal.remove();
        filePickerModal = null;
    }
    
    if (modalElement) {
        modalElement.style.opacity = '0';
        setTimeout(() => {
            modalElement.remove();
            modalElement = null;
        }, 200);
    }
    currentTask = null;
    weightSummary = null;
    selectedDate = null;
    selectedEvidenceFiles = [];
    availableFiles = [];
}

/**
 * Render the modal HTML
 */
function renderModal() {
    // Remove existing modal if any
    if (modalElement) {
        modalElement.remove();
    }

    const isEditing = currentTask !== null;
    const title = isEditing ? 'Edit Task' : 'New Task';
    const subtitle = isEditing ? 'Update task details below' : 'Create a new assignment for your students';

    modalElement = document.createElement('div');
    modalElement.className = 'task-editor-container fixed inset-0 z-[11000] flex items-center justify-center p-4 sm:p-6 transition-opacity duration-300';
    modalElement.innerHTML = `
        <style>
            @keyframes modalScaleIn {
                from { opacity: 0; transform: scale(0.95) translateY(10px); }
                to { opacity: 1; transform: scale(1) translateY(0); }
            }
            .animate-scale-in {
                animation: modalScaleIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
            }
            .glass-blur {
                backdrop-filter: blur(8px);
                -webkit-backdrop-filter: blur(8px);
            }
            /* Custom form inputs */
            .custom-input {
                transition: all 0.2s ease;
            }
            .custom-input:focus {
                transform: translateY(-1px);
                box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
            }
        </style>

        <!-- Backdrop with Blur -->
        <div class="modal-backdrop absolute inset-0 bg-gray-900/80 glass-blur transition-opacity duration-300" data-close-modal></div>

        <!-- Modal Content -->
        <div class="task-editor-content relative w-full max-w-lg transform rounded-2xl bg-white dark:bg-[#1E1F20] shadow-[0_50px_100px_-20px_rgba(0,0,0,0.5)] ring-1 ring-white/10 transition-all animate-scale-in overflow-hidden flex flex-col max-h-[90vh] z-[11001]">
            
            <!-- Header -->
            <div class="px-6 py-5 border-b border-gray-100 dark:border-gray-800 bg-white/90 dark:bg-[#1E1F20]/90 backdrop-blur-md flex items-center justify-between sticky top-0 z-20">
                <div>
                    <h3 class="text-xl font-bold tracking-tight text-gray-900 dark:text-white flex items-center gap-3">
                        ${isEditing ?
            '<div class="p-2 rounded-xl bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400"><svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg></div>' :
            '<div class="p-2 rounded-xl bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400"><svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path></svg></div>'
        }
                        ${title}
                    </h3>
                    <div class="mt-1 flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400 pl-[3.25rem]">
                        <span>${subtitle}</span>
                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300 border border-blue-100 dark:border-blue-800 whitespace-nowrap">
                            <svg class="mr-1.5 h-2 w-2 text-blue-400" fill="currentColor" viewBox="0 0 8 8"><circle cx="4" cy="4" r="3" /></svg>
                            Current Semester
                        </span>
                    </div>
                </div>
                <button data-close-modal class="rounded-full p-2 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-500 dark:hover:text-gray-300 transition-colors">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>
            
            <!-- Scrollable Body -->
            <div class="overflow-y-auto custom-scrollbar">
                <form id="taskEditorForm" class="p-6 space-y-6">
                    
                    <!-- Title -->
                    <div class="group">
                        <label for="taskTitle" class="block text-sm font-semibold leading-6 text-gray-900 dark:text-gray-200 mb-1.5">
                            Task Title <span class="text-red-500">*</span>
                        </label>
                        <div class="relative">
                            <input 
                                type="text" 
                                id="taskTitle" 
                                name="title"
                                value="${escapeHtml(currentTask?.title || '')}"
                                placeholder="E.g., Midterm Project Submission"
                                required
                                minlength="3"
                                class="custom-input block w-full rounded-xl border-0 py-3 pl-4 pr-4 text-gray-900 dark:text-white shadow-sm ring-1 ring-inset ring-gray-300 dark:ring-gray-700 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 dark:focus:ring-blue-500 bg-gray-50 dark:bg-gray-800 sm:text-sm sm:leading-6"
                            />
                        </div>
                        <p id="titleError" class="mt-2 text-sm text-red-600 dark:text-red-400 flex items-center gap-1 hidden">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                            <span>Title is required</span>
                        </p>
                    </div>
                    
                    <!-- Description -->
                    <div class="group">
                        <label for="taskDescription" class="block text-sm font-semibold leading-6 text-gray-900 dark:text-gray-200 mb-1.5">
                            Description
                        </label>
                        <div class="relative">
                            <textarea 
                                id="taskDescription" 
                                name="description"
                                rows="3"
                                placeholder="Provide detailed instructions for this task..."
                                class="custom-input block w-full rounded-xl border-0 py-3 pl-4 pr-4 text-gray-900 dark:text-white shadow-sm ring-1 ring-inset ring-gray-300 dark:ring-gray-700 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 dark:focus:ring-blue-500 bg-gray-50 dark:bg-gray-800 sm:text-sm sm:leading-6 resize-none"
                            >${escapeHtml(currentTask?.description || '')}</textarea>
                        </div>
                    </div>
                    
                    <!-- Course Selection -->
                    <div class="group">
                        <label for="taskCourse" class="block text-sm font-semibold leading-6 text-gray-900 dark:text-gray-200 mb-1.5">
                            Course <span class="text-red-500">*</span>
                        </label>
                        <div class="relative">
                            <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                <svg class="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path>
                                </svg>
                            </div>
                            <select 
                                id="taskCourse" 
                                name="courseId"
                                required
                                ${isEditing ? 'disabled' : ''}
                                class="custom-input block w-full rounded-xl border-0 py-3 pl-10 pr-10 text-gray-900 dark:text-white shadow-sm ring-1 ring-inset ring-gray-300 dark:ring-gray-700 focus:ring-2 focus:ring-inset focus:ring-blue-600 dark:focus:ring-blue-500 bg-gray-50 dark:bg-gray-800 sm:text-sm sm:leading-6 appearance-none ${isEditing ? 'opacity-70 cursor-not-allowed' : ''}"
                            >
                                <option value="">Select a course...</option>
                                ${courses.map(course => `
                                    <option value="${course.courseId}" ${currentTask?.courseId === course.courseId ? 'selected' : ''}>
                                        ${escapeHtml(course.courseCode)} - ${escapeHtml(course.courseName)}
                                    </option>
                                `).join('')}
                            </select>
                            <div class="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                                <svg class="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 9l4-4 4 4m0 6l-4 4-4-4"></path>
                                </svg>
                            </div>
                        </div>
                        ${isEditing ? '<p class="mt-1.5 text-xs text-amber-600 dark:text-amber-500 flex items-center gap-1"><svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg> Course cannot be changed after creation</p>' : ''}
                    </div>
                    
                    <!-- Weight Summary Card -->
                    <div id="weightSummaryContainer" class="hidden rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-800/50 p-4 transition-all duration-300">
                        <div class="flex items-center justify-between text-sm mb-2">
                            <span class="font-medium text-gray-700 dark:text-gray-300 flex items-center gap-2">
                                <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 3.055A9.001 9.001 0 1020.945 13H11V3.055z"></path>
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z"></path>
                                </svg>
                                Course Weight
                            </span>
                            <span id="weightSummaryText" class="font-bold text-gray-900 dark:text-white"></span>
                        </div>
                        <div class="h-2 w-full bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                            <div id="weightProgressBar" class="h-full rounded-full transition-all duration-500 ease-out" style="width: 0%"></div>
                        </div>
                        <p id="weightRemainingText" class="mt-2 text-xs text-gray-500 dark:text-gray-400"></p>
                    </div>
                    
                    <!-- Weight & Deadline Flex Group -->
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <!-- Weight Percentage -->
                        <div>
                            <label for="taskWeight" class="block text-sm font-semibold leading-6 text-gray-900 dark:text-gray-200 mb-1.5">
                                Weight <span class="text-red-500">*</span>
                            </label>
                            <div class="relative">
                                <input 
                                    type="number" 
                                    id="taskWeight" 
                                    name="weightPercentage"
                                    value="${currentTask?.weightPercentage || ''}"
                                    placeholder="0"
                                    required
                                    min="0"
                                    max="100"
                                    class="custom-input block w-full rounded-xl border-0 py-3 pl-4 pr-10 text-gray-900 dark:text-white shadow-sm ring-1 ring-inset ring-gray-300 dark:ring-gray-700 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 dark:focus:ring-blue-500 bg-gray-50 dark:bg-gray-800 sm:text-sm sm:leading-6"
                                />
                                <div class="absolute inset-y-0 right-0 pr-4 flex items-center pointer-events-none">
                                    <span class="text-gray-500 dark:text-gray-400 font-medium">%</span>
                                </div>
                            </div>
                            <p id="weightError" class="mt-2 text-sm text-red-600 dark:text-red-400 hidden"></p>
                        </div>
                        
                        <!-- Deadline -->
                        <div>
                            <label for="taskDeadline" class="block text-sm font-semibold leading-6 text-gray-900 dark:text-gray-200 mb-1.5">
                                Deadline
                            </label>
                            <div class="relative">
                                <button 
                                    type="button"
                                    id="deadlinePickerBtn"
                                    class="custom-input flex items-center justify-between w-full rounded-xl border-0 py-3 pl-4 pr-4 text-gray-900 dark:text-white shadow-sm ring-1 ring-inset ring-gray-300 dark:ring-gray-700 focus:ring-2 focus:ring-inset focus:ring-blue-600 dark:focus:ring-blue-500 bg-gray-50 dark:bg-gray-800 sm:text-sm sm:leading-6 cursor-pointer text-left"
                                >
                                    <span id="deadlineDisplayText" class="text-gray-400">${currentTask?.deadline ? formatDateDisplay(currentTask.deadline) : 'Select a date...'}</span>
                                    <svg class="h-5 w-5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                                    </svg>
                                </button>
                                <input type="hidden" id="taskDeadline" name="deadline" value="${currentTask?.deadline || ''}" />
                            </div>
                            <p class="mt-1.5 text-xs text-gray-500 dark:text-gray-400">Optional</p>
                        </div>
                    </div>
                    
                    ${isEditing ? `
                    <!-- Progress (Edit only) -->
                    <div class="pt-4 border-t border-gray-100 dark:border-gray-800">
                        <label for="taskProgress" class="block text-sm font-semibold leading-6 text-gray-900 dark:text-gray-200 mb-1.5">
                            Progress & Status
                        </label>
                        <div class="flex items-center gap-4">
                            <div class="relative w-32">
                                <input 
                                    type="number" 
                                    id="taskProgress" 
                                    name="progressPercentage"
                                    value="${currentTask?.progressPercentage || 0}"
                                    min="0"
                                    max="100"
                                    class="custom-input block w-full rounded-xl border-0 py-3 pl-4 pr-10 text-gray-900 dark:text-white shadow-sm ring-1 ring-inset ring-gray-300 dark:ring-gray-700 focus:ring-2 focus:ring-inset focus:ring-blue-600 dark:focus:ring-blue-500 bg-gray-50 dark:bg-gray-800 sm:text-sm sm:leading-6"
                                />
                                <div class="absolute inset-y-0 right-0 pr-4 flex items-center pointer-events-none">
                                    <span class="text-gray-500 dark:text-gray-400 font-medium">%</span>
                                </div>
                            </div>
                            <div class="flex-1">
                                <div class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border ${currentTask?.status === 'COMPLETED' ? 'bg-green-50 text-green-700 border-green-200 dark:bg-green-900/20 dark:text-green-400 dark:border-green-800' :
                currentTask?.status === 'IN_PROGRESS' ? 'bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-900/20 dark:text-blue-400 dark:border-blue-800' :
                    'bg-yellow-50 text-yellow-700 border-yellow-200 dark:bg-yellow-900/20 dark:text-yellow-400 dark:border-yellow-800'
            }">
                                    ${currentTask?.status?.replace('_', ' ') || 'PENDING'}
                                </div>
                            </div>
                        </div>
                    </div>
                    ` : ''}
                    
                    <!-- Evidence Files Section -->
                    <div class="pt-4 border-t border-gray-100 dark:border-gray-800">
                        <div class="flex items-center justify-between mb-3">
                            <label class="block text-sm font-semibold leading-6 text-gray-900 dark:text-gray-200">
                                Evidence Files
                                <span class="font-normal text-gray-500 dark:text-gray-400">(Optional)</span>
                            </label>
                            <button 
                                type="button"
                                id="attachFilesBtn"
                                class="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/20 rounded-lg hover:bg-blue-100 dark:hover:bg-blue-900/40 transition-colors"
                            >
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"></path>
                                </svg>
                                Attach Files
                            </button>
                        </div>
                        <p class="text-xs text-gray-500 dark:text-gray-400 mb-3">
                            Attach files from your File Explorer as evidence of task completion. HOD can review these files.
                        </p>
                        
                        <!-- Selected Files List -->
                        <div id="selectedFilesContainer" class="space-y-2">
                            ${renderSelectedFiles()}
                        </div>
                    </div>
                    
                    <!-- Global Error Message -->
                    <div id="formError" class="hidden rounded-xl bg-red-50 dark:bg-red-900/20 p-4 border border-red-200 dark:border-red-800">
                        <div class="flex">
                            <div class="flex-shrink-0">
                                <svg class="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                                </svg>
                            </div>
                            <div class="ml-3">
                                <p id="formErrorMessage" class="text-sm font-medium text-red-800 dark:text-red-200"></p>
                            </div>
                        </div>
                    </div>
                    
                </form>
            </div>
            
            <!-- Footer -->
            <div class="px-6 py-4 bg-gray-50 dark:bg-[#252628] border-t border-gray-100 dark:border-gray-800 flex items-center justify-between">
                <button 
                    type="button"
                    data-close-modal
                    class="px-5 py-2.5 text-sm font-semibold text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700 transition-all focus:ring-2 focus:ring-gray-200 dark:focus:ring-gray-700"
                >
                    Cancel
                </button>
                <button 
                    type="button"
                    id="saveTaskBtn"
                    class="px-5 py-2.5 text-sm font-semibold text-white bg-blue-600 rounded-xl hover:bg-blue-700 hover:shadow-lg hover:shadow-blue-500/20 active:scale-95 transition-all focus:ring-2 focus:ring-offset-2 focus:ring-blue-600 dark:focus:ring-offset-gray-900 disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none"
                >
                    ${isEditing ? 'Update Task' : 'Create Task'}
                </button>
            </div>
        </div>
    `;

    document.body.appendChild(modalElement);

    // Add event listeners
    setupEventListeners();

    // Initialize Flatpickr
    initializeDatePicker();

    // Focus on title input
    setTimeout(() => {
        document.getElementById('taskTitle')?.focus();
    }, 100);
}

/**
 * Initialize the custom date picker
 */
function initializeDatePicker() {
    const deadlineBtn = document.getElementById('deadlinePickerBtn');
    
    if (deadlineBtn) {
        deadlineBtn.addEventListener('click', openDatePicker);
    }
    
    // Set initial selected date if editing
    if (currentTask?.deadline) {
        const date = new Date(currentTask.deadline);
        selectedDate = currentTask.deadline;
        currentMonth = date.getMonth();
        currentYear = date.getFullYear();
        updateDeadlineDisplay();
    }
}

/**
 * Open the date picker modal
 */
function openDatePicker() {
    // Create the date picker modal
    datePickerModal = document.createElement('div');
    datePickerModal.className = 'fixed inset-0 z-[12000] flex items-center justify-center p-4';
    datePickerModal.innerHTML = `
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" data-close-datepicker></div>
        
        <!-- Calendar Card -->
        <div class="relative w-full max-w-[360px] bg-[#1E1F20] rounded-2xl shadow-2xl border border-gray-700/50 overflow-hidden animate-scale-in">
            <!-- Header -->
            <div class="px-6 py-4 border-b border-gray-700/50 flex items-center justify-between">
                <h3 class="text-lg font-semibold text-white flex items-center gap-2">
                    <svg class="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                    </svg>
                    Select Deadline
                </h3>
                <button data-close-datepicker class="p-1.5 rounded-lg text-gray-400 hover:text-white hover:bg-gray-700/50 transition-colors">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>
            
            <!-- Month Navigation -->
            <div class="px-6 py-3 flex items-center justify-between">
                <button type="button" id="prevMonthBtn" class="p-2 rounded-lg text-gray-400 hover:text-white hover:bg-gray-700/50 transition-colors">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                    </svg>
                </button>
                <h4 id="currentMonthYear" class="text-base font-semibold text-white"></h4>
                <button type="button" id="nextMonthBtn" class="p-2 rounded-lg text-gray-400 hover:text-white hover:bg-gray-700/50 transition-colors">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                    </svg>
                </button>
            </div>
            
            <!-- Weekday Headers -->
            <div class="px-4 grid grid-cols-7 gap-1 mb-1">
                <div class="text-center text-xs font-semibold text-gray-500 uppercase py-2">Sun</div>
                <div class="text-center text-xs font-semibold text-gray-500 uppercase py-2">Mon</div>
                <div class="text-center text-xs font-semibold text-gray-500 uppercase py-2">Tue</div>
                <div class="text-center text-xs font-semibold text-gray-500 uppercase py-2">Wed</div>
                <div class="text-center text-xs font-semibold text-gray-500 uppercase py-2">Thu</div>
                <div class="text-center text-xs font-semibold text-gray-500 uppercase py-2">Fri</div>
                <div class="text-center text-xs font-semibold text-gray-500 uppercase py-2">Sat</div>
            </div>
            
            <!-- Calendar Grid -->
            <div id="calendarGrid" class="px-4 pb-4 grid grid-cols-7 gap-1">
                <!-- Days will be rendered here -->
            </div>
            
            <!-- Footer -->
            <div class="px-6 py-4 bg-[#252628] border-t border-gray-700/50 flex items-center justify-between">
                <button type="button" id="clearDateBtn" class="px-4 py-2 text-sm font-medium text-gray-400 hover:text-white transition-colors">
                    Clear
                </button>
                <div class="flex gap-2">
                    <button type="button" data-close-datepicker class="px-4 py-2 text-sm font-medium text-gray-300 bg-gray-700/50 rounded-lg hover:bg-gray-700 transition-colors">
                        Cancel
                    </button>
                    <button type="button" id="confirmDateBtn" class="px-4 py-2 text-sm font-semibold text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors">
                        Confirm
                    </button>
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(datePickerModal);
    
    // Render the calendar
    renderCalendar();
    
    // Add event listeners
    setupDatePickerEvents();
}

/**
 * Close the date picker modal
 */
function closeDatePicker() {
    if (datePickerModal) {
        datePickerModal.style.opacity = '0';
        setTimeout(() => {
            datePickerModal.remove();
            datePickerModal = null;
        }, 150);
    }
}

/**
 * Setup date picker event listeners
 */
function setupDatePickerEvents() {
    // Close buttons
    datePickerModal.querySelectorAll('[data-close-datepicker]').forEach(el => {
        el.addEventListener('click', closeDatePicker);
    });
    
    // Month navigation
    document.getElementById('prevMonthBtn')?.addEventListener('click', () => {
        currentMonth--;
        if (currentMonth < 0) {
            currentMonth = 11;
            currentYear--;
        }
        renderCalendar();
    });
    
    document.getElementById('nextMonthBtn')?.addEventListener('click', () => {
        currentMonth++;
        if (currentMonth > 11) {
            currentMonth = 0;
            currentYear++;
        }
        renderCalendar();
    });
    
    // Clear button
    document.getElementById('clearDateBtn')?.addEventListener('click', () => {
        selectedDate = null;
        renderCalendar();
    });
    
    // Confirm button
    document.getElementById('confirmDateBtn')?.addEventListener('click', () => {
        const deadlineInput = document.getElementById('taskDeadline');
        if (deadlineInput) {
            deadlineInput.value = selectedDate || '';
        }
        updateDeadlineDisplay();
        closeDatePicker();
    });
}

/**
 * Render the calendar grid
 */
function renderCalendar() {
    const grid = document.getElementById('calendarGrid');
    const monthYearDisplay = document.getElementById('currentMonthYear');
    
    if (!grid || !monthYearDisplay) return;
    
    const months = ['January', 'February', 'March', 'April', 'May', 'June',
                    'July', 'August', 'September', 'October', 'November', 'December'];
    
    monthYearDisplay.textContent = `${months[currentMonth]} ${currentYear}`;
    
    // Get first day of month and total days
    const firstDay = new Date(currentYear, currentMonth, 1).getDay();
    const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
    const daysInPrevMonth = new Date(currentYear, currentMonth, 0).getDate();
    
    // Get today's date for comparison
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    let html = '';
    
    // Previous month's trailing days
    for (let i = firstDay - 1; i >= 0; i--) {
        const day = daysInPrevMonth - i;
        html += `<div class="text-center py-2.5 text-sm text-gray-600 cursor-not-allowed">${day}</div>`;
    }
    
    // Current month's days
    for (let day = 1; day <= daysInMonth; day++) {
        const dateStr = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        const date = new Date(currentYear, currentMonth, day);
        date.setHours(0, 0, 0, 0);
        
        const isToday = date.getTime() === today.getTime();
        const isPast = date < today;
        const isSelected = selectedDate === dateStr;
        
        let classes = 'text-center py-2.5 text-sm rounded-lg transition-all duration-150 ';
        
        if (isPast) {
            classes += 'text-gray-600 cursor-not-allowed';
        } else if (isSelected) {
            classes += 'bg-blue-600 text-white font-semibold shadow-lg shadow-blue-600/30 cursor-pointer';
        } else if (isToday) {
            classes += 'text-blue-400 font-semibold ring-2 ring-blue-500 cursor-pointer hover:bg-blue-600/20';
        } else {
            classes += 'text-gray-200 cursor-pointer hover:bg-gray-700/50';
        }
        
        if (isPast) {
            html += `<div class="${classes}">${day}</div>`;
        } else {
            html += `<div class="${classes}" data-date="${dateStr}">${day}</div>`;
        }
    }
    
    // Next month's leading days
    const totalCells = firstDay + daysInMonth;
    const remainingCells = totalCells % 7 === 0 ? 0 : 7 - (totalCells % 7);
    for (let i = 1; i <= remainingCells; i++) {
        html += `<div class="text-center py-2.5 text-sm text-gray-600 cursor-not-allowed">${i}</div>`;
    }
    
    grid.innerHTML = html;
    
    // Add click listeners to selectable days
    grid.querySelectorAll('[data-date]').forEach(el => {
        el.addEventListener('click', () => {
            selectedDate = el.dataset.date;
            renderCalendar();
        });
    });
}

/**
 * Update the deadline display text
 */
function updateDeadlineDisplay() {
    const displayText = document.getElementById('deadlineDisplayText');
    if (displayText) {
        if (selectedDate) {
            displayText.textContent = formatDateDisplay(selectedDate);
            displayText.classList.remove('text-gray-400');
            displayText.classList.add('text-white');
        } else {
            displayText.textContent = 'Select a date...';
            displayText.classList.add('text-gray-400');
            displayText.classList.remove('text-white');
        }
    }
}

/**
 * Format date for display
 */
function formatDateDisplay(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr + 'T00:00:00');
    const months = ['January', 'February', 'March', 'April', 'May', 'June',
                    'July', 'August', 'September', 'October', 'November', 'December'];
    return `${months[date.getMonth()]} ${date.getDate()}, ${date.getFullYear()}`;
}

/**
 * Setup event listeners for the modal
 */
function setupEventListeners() {
    // Close modal on backdrop click or close button
    modalElement.querySelectorAll('[data-close-modal]').forEach(el => {
        el.addEventListener('click', closeTaskEditor);
    });

    // Close on Escape key
    document.addEventListener('keydown', handleEscapeKey);

    // Course change - load weight summary
    const courseSelect = document.getElementById('taskCourse');
    if (courseSelect && !courseSelect.disabled) {
        courseSelect.addEventListener('change', async (e) => {
            const courseId = e.target.value;
            if (courseId) {
                await loadWeightSummary(parseInt(courseId));
            } else {
                hideWeightSummary();
            }
        });
    }

    // Weight input validation
    const weightInput = document.getElementById('taskWeight');
    if (weightInput) {
        weightInput.addEventListener('input', validateWeight);
    }

    // Save button
    const saveBtn = document.getElementById('saveTaskBtn');
    if (saveBtn) {
        saveBtn.addEventListener('click', handleSave);
    }

    // Form submission (Enter key)
    const form = document.getElementById('taskEditorForm');
    if (form) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            handleSave();
        });
    }
    
    // Attach files button
    const attachFilesBtn = document.getElementById('attachFilesBtn');
    if (attachFilesBtn) {
        attachFilesBtn.addEventListener('click', openFilePicker);
    }
}

/**
 * Handle Escape key to close modal
 */
function handleEscapeKey(e) {
    if (e.key === 'Escape') {
        closeTaskEditor();
        document.removeEventListener('keydown', handleEscapeKey);
    }
}

/**
 * Load weight summary for a course
 */
async function loadWeightSummary(courseId) {
    try {
        weightSummary = await professor.getWeightSummary(courseId, currentSemesterId);
        displayWeightSummary();
    } catch (error) {
        console.error('Error loading weight summary:', error);
        hideWeightSummary();
    }
}

/**
 * Display weight summary in the UI
 */
function displayWeightSummary() {
    const container = document.getElementById('weightSummaryContainer');
    const summaryText = document.getElementById('weightSummaryText');
    const progressBar = document.getElementById('weightProgressBar');
    const remainingText = document.getElementById('weightRemainingText');

    if (!container || !weightSummary) return;

    // Calculate used weight (exclude current task if editing)
    let usedWeight = weightSummary.totalWeightPercentage || 0;
    if (currentTask && weightSummary.taskWeights) {
        const currentTaskWeight = weightSummary.taskWeights[currentTask.id] || 0;
        usedWeight -= currentTaskWeight;
    }

    const remaining = 100 - usedWeight;

    summaryText.textContent = `${usedWeight}% used`;
    progressBar.style.width = `${usedWeight}%`;

    // Color the progress bar based on usage
    progressBar.classList.remove('bg-blue-500', 'bg-yellow-500', 'bg-red-500');
    if (usedWeight >= 100) {
        progressBar.classList.add('bg-red-500');
    } else if (usedWeight >= 80) {
        progressBar.classList.add('bg-yellow-500');
    } else {
        progressBar.classList.add('bg-blue-500');
    }

    remainingText.textContent = `${remaining}% remaining for new tasks`;
    container.classList.remove('hidden');

    // Validate current weight input
    validateWeight();
}

/**
 * Hide weight summary
 */
function hideWeightSummary() {
    const container = document.getElementById('weightSummaryContainer');
    if (container) {
        container.classList.add('hidden');
    }
    weightSummary = null;
}

// ==================== Evidence File Management ====================

/**
 * Render the selected evidence files list
 */
function renderSelectedFiles() {
    if (!selectedEvidenceFiles || selectedEvidenceFiles.length === 0) {
        return `
            <div class="flex items-center justify-center p-4 bg-gray-50 dark:bg-gray-800/50 rounded-xl border-2 border-dashed border-gray-200 dark:border-gray-700">
                <div class="text-center">
                    <svg class="mx-auto h-8 w-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                    </svg>
                    <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">No files attached</p>
                </div>
            </div>
        `;
    }
    
    return selectedEvidenceFiles.map((file, index) => `
        <div class="flex items-center gap-3 p-3 bg-gray-50 dark:bg-gray-800/50 rounded-lg border border-gray-200 dark:border-gray-700 group">
            <div class="flex-shrink-0">
                ${getFileIcon(file.type)}
            </div>
            <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-gray-900 dark:text-white truncate" title="${escapeHtml(file.name)}">
                    ${escapeHtml(file.name)}
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-400">
                    ${formatFileSize(file.size)}
                </p>
            </div>
            <button 
                type="button"
                onclick="removeEvidenceFile(${index})"
                class="flex-shrink-0 p-1.5 text-gray-400 hover:text-red-500 dark:hover:text-red-400 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors opacity-0 group-hover:opacity-100"
                title="Remove file"
            >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
            </button>
        </div>
    `).join('');
}

/**
 * Update the selected files display
 */
function updateSelectedFilesDisplay() {
    const container = document.getElementById('selectedFilesContainer');
    if (container) {
        container.innerHTML = renderSelectedFiles();
    }
}

/**
 * Remove an evidence file from selection
 */
window.removeEvidenceFile = function(index) {
    selectedEvidenceFiles.splice(index, 1);
    updateSelectedFilesDisplay();
};

/**
 * Open the file picker modal
 */
async function openFilePicker() {
    try {
        // Load available files
        availableFiles = await professor.getAvailableFilesForEvidence(currentSemesterId);
        
        if (!availableFiles || availableFiles.length === 0) {
            showToast('No files available. Please upload files in the File Explorer first.', 'warning');
            return;
        }
        
        renderFilePicker();
    } catch (error) {
        console.error('Error loading available files:', error);
        showToast('Failed to load available files', 'error');
    }
}

/**
 * Render the file picker modal
 */
function renderFilePicker() {
    // Get IDs of already selected files
    const selectedIds = new Set(selectedEvidenceFiles.map(f => f.id));
    
    filePickerModal = document.createElement('div');
    filePickerModal.className = 'fixed inset-0 z-[12000] flex items-center justify-center p-4';
    filePickerModal.innerHTML = `
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" data-close-filepicker></div>
        
        <!-- Modal -->
        <div class="relative w-full max-w-xl bg-white dark:bg-[#1E1F20] rounded-2xl shadow-2xl border border-gray-200 dark:border-gray-700 overflow-hidden animate-scale-in max-h-[80vh] flex flex-col">
            <!-- Header -->
            <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
                <div>
                    <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                        <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"></path>
                        </svg>
                        Select Evidence Files
                    </h3>
                    <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
                        Choose files to attach as evidence
                    </p>
                </div>
                <button data-close-filepicker class="p-2 text-gray-400 hover:text-gray-500 dark:hover:text-gray-300 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>
            
            <!-- File List -->
            <div class="overflow-y-auto flex-1 p-4">
                <div class="space-y-2" id="filePickerList">
                    ${availableFiles.map(file => {
                        const isSelected = selectedIds.has(file.id);
                        return `
                            <label class="flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-all ${
                                isSelected 
                                    ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-300 dark:border-blue-700' 
                                    : 'bg-gray-50 dark:bg-gray-800/50 border-gray-200 dark:border-gray-700 hover:border-blue-300 dark:hover:border-blue-700'
                            }">
                                <input 
                                    type="checkbox" 
                                    class="file-checkbox w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500 dark:border-gray-600 dark:focus:ring-blue-600 dark:ring-offset-gray-800"
                                    data-file-id="${file.id}"
                                    data-file-name="${escapeHtml(file.originalFilename)}"
                                    data-file-size="${file.fileSize || 0}"
                                    data-file-type="${file.fileType || ''}"
                                    ${isSelected ? 'checked' : ''}
                                />
                                <div class="flex-shrink-0">
                                    ${getFileIcon(file.fileType)}
                                </div>
                                <div class="flex-1 min-w-0">
                                    <p class="text-sm font-medium text-gray-900 dark:text-white truncate">
                                        ${escapeHtml(file.originalFilename)}
                                    </p>
                                    <p class="text-xs text-gray-500 dark:text-gray-400">
                                        ${formatFileSize(file.fileSize)} • ${formatUploadDate(file.uploadedAt)}
                                    </p>
                                </div>
                            </label>
                        `;
                    }).join('')}
                </div>
                
                ${availableFiles.length === 0 ? `
                    <div class="text-center py-8">
                        <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                        </svg>
                        <p class="mt-2 text-sm text-gray-500 dark:text-gray-400">No files available</p>
                        <p class="text-xs text-gray-400 dark:text-gray-500">Upload files in the File Explorer first</p>
                    </div>
                ` : ''}
            </div>
            
            <!-- Footer -->
            <div class="px-6 py-4 bg-gray-50 dark:bg-[#252628] border-t border-gray-200 dark:border-gray-700 flex items-center justify-between">
                <span id="selectedCount" class="text-sm text-gray-600 dark:text-gray-400">
                    ${selectedIds.size} file(s) selected
                </span>
                <div class="flex gap-2">
                    <button 
                        type="button"
                        data-close-filepicker
                        class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors"
                    >
                        Cancel
                    </button>
                    <button 
                        type="button"
                        id="confirmFileSelection"
                        class="px-4 py-2 text-sm font-semibold text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
                    >
                        Attach Selected
                    </button>
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(filePickerModal);
    setupFilePickerEvents();
}

/**
 * Setup file picker event listeners
 */
function setupFilePickerEvents() {
    // Close buttons
    filePickerModal.querySelectorAll('[data-close-filepicker]').forEach(el => {
        el.addEventListener('click', closeFilePicker);
    });
    
    // Checkbox change - update count
    const checkboxes = filePickerModal.querySelectorAll('.file-checkbox');
    checkboxes.forEach(cb => {
        cb.addEventListener('change', updateSelectedCount);
    });
    
    // Confirm selection
    const confirmBtn = filePickerModal.querySelector('#confirmFileSelection');
    if (confirmBtn) {
        confirmBtn.addEventListener('click', confirmFileSelection);
    }
    
    // Close on Escape
    const handleEscape = (e) => {
        if (e.key === 'Escape') {
            closeFilePicker();
            document.removeEventListener('keydown', handleEscape);
        }
    };
    document.addEventListener('keydown', handleEscape);
}

/**
 * Update the selected count display in file picker
 */
function updateSelectedCount() {
    const checkboxes = filePickerModal.querySelectorAll('.file-checkbox:checked');
    const countEl = filePickerModal.querySelector('#selectedCount');
    if (countEl) {
        countEl.textContent = `${checkboxes.length} file(s) selected`;
    }
}

/**
 * Confirm file selection and update the main form
 */
function confirmFileSelection() {
    const checkboxes = filePickerModal.querySelectorAll('.file-checkbox:checked');
    
    selectedEvidenceFiles = Array.from(checkboxes).map(cb => ({
        id: parseInt(cb.dataset.fileId),
        name: cb.dataset.fileName,
        size: parseInt(cb.dataset.fileSize) || 0,
        type: cb.dataset.fileType || ''
    }));
    
    updateSelectedFilesDisplay();
    closeFilePicker();
}

/**
 * Close the file picker modal
 */
function closeFilePicker() {
    if (filePickerModal) {
        filePickerModal.style.opacity = '0';
        setTimeout(() => {
            if (filePickerModal) {
                filePickerModal.remove();
                filePickerModal = null;
            }
        }, 150);
    }
}

/**
 * Get file icon based on type
 */
function getFileIcon(fileType) {
    const type = fileType?.toLowerCase() || '';
    
    if (type.includes('pdf')) {
        return `<div class="w-10 h-10 bg-red-100 dark:bg-red-900/30 rounded-lg flex items-center justify-center">
            <svg class="w-5 h-5 text-red-600 dark:text-red-400" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/>
            </svg>
        </div>`;
    }
    if (type.includes('image') || type.includes('png') || type.includes('jpg') || type.includes('jpeg')) {
        return `<div class="w-10 h-10 bg-purple-100 dark:bg-purple-900/30 rounded-lg flex items-center justify-center">
            <svg class="w-5 h-5 text-purple-600 dark:text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
            </svg>
        </div>`;
    }
    if (type.includes('word') || type.includes('doc')) {
        return `<div class="w-10 h-10 bg-blue-100 dark:bg-blue-900/30 rounded-lg flex items-center justify-center">
            <svg class="w-5 h-5 text-blue-600 dark:text-blue-400" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/>
            </svg>
        </div>`;
    }
    if (type.includes('excel') || type.includes('spreadsheet') || type.includes('xls')) {
        return `<div class="w-10 h-10 bg-green-100 dark:bg-green-900/30 rounded-lg flex items-center justify-center">
            <svg class="w-5 h-5 text-green-600 dark:text-green-400" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/>
            </svg>
        </div>`;
    }
    
    // Default file icon
    return `<div class="w-10 h-10 bg-gray-100 dark:bg-gray-700 rounded-lg flex items-center justify-center">
        <svg class="w-5 h-5 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
        </svg>
    </div>`;
}

/**
 * Format file size for display
 */
function formatFileSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

/**
 * Format upload date for display
 */
function formatUploadDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

/**
 * Validate weight input against remaining weight
 */
function validateWeight() {
    const weightInput = document.getElementById('taskWeight');
    const weightError = document.getElementById('weightError');

    if (!weightInput || !weightError) return true;

    const weight = parseInt(weightInput.value) || 0;

    // Basic validation
    if (weight < 0 || weight > 100) {
        weightError.textContent = 'Weight must be between 0 and 100';
        weightError.classList.remove('hidden');
        return false;
    }

    // Check against remaining weight
    if (weightSummary) {
        let usedWeight = weightSummary.totalWeightPercentage || 0;
        if (currentTask && weightSummary.taskWeights) {
            const currentTaskWeight = weightSummary.taskWeights[currentTask.id] || 0;
            usedWeight -= currentTaskWeight;
        }

        const remaining = 100 - usedWeight;

        if (weight > remaining) {
            weightError.textContent = `Weight exceeds remaining capacity. Maximum available: ${remaining}%`;
            weightError.classList.remove('hidden');
            return false;
        }
    }

    weightError.classList.add('hidden');
    return true;
}

/**
 * Validate the entire form
 */
function validateForm() {
    const form = document.getElementById('taskEditorForm');
    const titleInput = document.getElementById('taskTitle');
    const courseSelect = document.getElementById('taskCourse');
    const weightInput = document.getElementById('taskWeight');
    const deadlineInput = document.getElementById('taskDeadline');

    let isValid = true;

    // Title validation
    const titleError = document.getElementById('titleError');
    const title = titleInput?.value?.trim() || '';
    if (!title) {
        titleError.textContent = 'Title is required';
        titleError.classList.remove('hidden');
        isValid = false;
    } else if (title.length < 3) {
        titleError.textContent = 'Title must be at least 3 characters';
        titleError.classList.remove('hidden');
        isValid = false;
    } else {
        titleError.classList.add('hidden');
    }

    // Course validation (only for new tasks)
    if (!currentTask) {
        const courseId = courseSelect?.value;
        if (!courseId) {
            showFormError('Please select a course');
            isValid = false;
        }
    }

    // Weight validation
    if (!validateWeight()) {
        isValid = false;
    }

    const weight = parseInt(weightInput?.value);
    if (isNaN(weight) || weight < 0 || weight > 100) {
        const weightError = document.getElementById('weightError');
        weightError.textContent = 'Weight percentage is required (0-100)';
        weightError.classList.remove('hidden');
        isValid = false;
    }

    // Deadline validation (if set, must be today or future)
    const deadline = deadlineInput?.value;
    if (deadline) {
        const deadlineDate = new Date(deadline);
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        if (deadlineDate < today) {
            showFormError('Deadline must be today or a future date');
            isValid = false;
        }
    }

    if (isValid) {
        hideFormError();
    }

    return isValid;
}

/**
 * Show form error message
 */
function showFormError(message) {
    const errorDiv = document.getElementById('formError');
    const errorMessage = document.getElementById('formErrorMessage');

    if (errorDiv && errorMessage) {
        errorMessage.textContent = message;
        errorDiv.classList.remove('hidden');
    }
}

/**
 * Hide form error message
 */
function hideFormError() {
    const errorDiv = document.getElementById('formError');
    if (errorDiv) {
        errorDiv.classList.add('hidden');
    }
}

/**
 * Handle save button click
 */
async function handleSave() {
    if (!validateForm()) {
        return;
    }

    const saveBtn = document.getElementById('saveTaskBtn');
    const originalText = saveBtn.textContent;

    try {
        // Show loading state
        saveBtn.disabled = true;
        saveBtn.innerHTML = `
            <svg class="animate-spin -ml-1 mr-2 h-4 w-4 inline" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Saving...
        `;

        // Gather form data
        const titleInput = document.getElementById('taskTitle');
        const descriptionInput = document.getElementById('taskDescription');
        const courseSelect = document.getElementById('taskCourse');
        const weightInput = document.getElementById('taskWeight');
        const progressInput = document.getElementById('taskProgress');
        const deadlineInput = document.getElementById('taskDeadline');

        // Get evidence file IDs
        const evidenceFileIds = selectedEvidenceFiles.map(f => f.id);

        if (currentTask) {
            // Update existing task
            const updateData = {
                title: titleInput.value.trim(),
                description: descriptionInput.value.trim() || null,
                weightPercentage: parseInt(weightInput.value),
                progressPercentage: progressInput ? parseInt(progressInput.value) : undefined,
                deadline: deadlineInput.value || null,
                evidenceFileIds: evidenceFileIds,
            };

            await professor.updateTask(currentTask.id, updateData);
            showToast('Task updated successfully', 'success');
        } else {
            // Create new task
            const createData = {
                title: titleInput.value.trim(),
                description: descriptionInput.value.trim() || null,
                weightPercentage: parseInt(weightInput.value),
                courseId: parseInt(courseSelect.value),
                semesterId: currentSemesterId,
                deadline: deadlineInput.value || null,
                evidenceFileIds: evidenceFileIds,
            };

            await professor.createTask(createData);
            showToast('Task created successfully', 'success');
        }

        // Close modal and trigger callback
        closeTaskEditor();
        if (onSaveCallback) {
            onSaveCallback();
        }

    } catch (error) {
        console.error('Error saving task:', error);
        showFormError(error.message || 'Failed to save task. Please try again.');

        // Restore button state
        saveBtn.disabled = false;
        saveBtn.textContent = originalText;
    }
}

/**
 * Get today's date in YYYY-MM-DD format
 */
function getTodayDate() {
    const today = new Date();
    return today.toISOString().split('T')[0];
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

export default {
    openTaskEditor,
    closeTaskEditor,
};
