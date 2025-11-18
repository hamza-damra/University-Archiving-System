/**
 * Professor Dashboard - Semester-based
 */

import { professor, getUserInfo, isAuthenticated, redirectToLogin, clearAuthData, deanship } from './api.js';
import { showToast, showModal, formatDate, getTimeUntil, isOverdue, formatFileSize } from './ui.js';

// Check authentication
if (!isAuthenticated()) {
    redirectToLogin();
}

const userInfo = getUserInfo();
if (userInfo.role !== 'ROLE_PROFESSOR') {
    showToast('Access denied - Professor privileges required', 'error');
    setTimeout(() => redirectToLogin(), 2000);
}

// State
let academicYears = [];
let semesters = [];
let courses = [];
let notifications = [];
let selectedAcademicYearId = null;
let selectedSemesterId = null;

// DOM Elements
const professorName = document.getElementById('professorName');
const logoutBtn = document.getElementById('logoutBtn');
const notificationsBtn = document.getElementById('notificationsBtn');
const notificationBadge = document.getElementById('notificationBadge');
const notificationsDropdown = document.getElementById('notificationsDropdown');
const closeNotificationsDropdown = document.getElementById('closeNotificationsDropdown');
const notificationsList = document.getElementById('notificationsList');
const academicYearSelect = document.getElementById('academicYearSelect');
const semesterSelect = document.getElementById('semesterSelect');
const coursesContainer = document.getElementById('coursesContainer');
const emptyState = document.getElementById('emptyState');
const coursesTabContent = document.getElementById('coursesTabContent');
const fileExplorerTabContent = document.getElementById('fileExplorerTabContent');
const fileExplorerContainer = document.getElementById('fileExplorerContainer');
const breadcrumbs = document.getElementById('breadcrumbs');

// Initialize
professorName.textContent = userInfo.fullName;
loadAcademicYears();
loadNotifications();

// Poll notifications every 30 seconds
setInterval(loadNotifications, 30000);

// Tab Switching
window.switchTab = function(tabName) {
    // Update tab buttons
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active', 'border-blue-600', 'text-blue-600');
        btn.classList.add('border-transparent', 'text-gray-500');
    });
    
    const activeBtn = document.getElementById(`${tabName}Tab`);
    if (activeBtn) {
        activeBtn.classList.add('active', 'border-blue-600', 'text-blue-600');
        activeBtn.classList.remove('border-transparent', 'text-gray-500');
    }
    
    // Update tab content
    document.getElementById('dashboardTabContent')?.classList.add('hidden');
    document.getElementById('coursesTabContent')?.classList.add('hidden');
    document.getElementById('fileExplorerTabContent')?.classList.add('hidden');
    
    const activeContent = document.getElementById(`${tabName}TabContent`);
    if (activeContent) {
        activeContent.classList.remove('hidden');
    }
    
    // Load data for specific tabs
    if (tabName === 'dashboard' && selectedSemesterId) {
        loadDashboardOverview();
    } else if (tabName === 'fileExplorer' && selectedSemesterId) {
        loadFileExplorer();
    }
};

// Logout
logoutBtn.addEventListener('click', () => {
    clearAuthData();
    showToast('Logged out successfully', 'success');
    redirectToLogin();
});

// Notifications dropdown toggle
notificationsBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    notificationsDropdown.classList.toggle('hidden');
    if (!notificationsDropdown.classList.contains('hidden')) {
        loadNotifications();
    }
});

closeNotificationsDropdown.addEventListener('click', () => {
    notificationsDropdown.classList.add('hidden');
});

// Close dropdown when clicking outside
document.addEventListener('click', (e) => {
    if (!notificationsDropdown.classList.contains('hidden') && 
        !notificationsDropdown.contains(e.target) && 
        !notificationsBtn.contains(e.target)) {
        notificationsDropdown.classList.add('hidden');
    }
});

// Academic year selection
academicYearSelect.addEventListener('change', async (e) => {
    selectedAcademicYearId = e.target.value ? parseInt(e.target.value) : null;
    selectedSemesterId = null;
    
    if (selectedAcademicYearId) {
        await loadSemesters(selectedAcademicYearId);
    } else {
        semesterSelect.innerHTML = '<option value="">Select academic year first</option>';
        courses = [];
        renderCourses();
    }
});

// Semester selection
semesterSelect.addEventListener('change', async (e) => {
    selectedSemesterId = e.target.value ? parseInt(e.target.value) : null;
    
    if (selectedSemesterId) {
        await loadCourses(selectedSemesterId);
    } else {
        courses = [];
        renderCourses();
    }
});

// Load academic years
async function loadAcademicYears() {
    try {
        const response = await deanship.getAcademicYears();
        academicYears = response.data || [];
        
        academicYearSelect.innerHTML = '<option value="">Select academic year</option>';
        academicYears.forEach(year => {
            const option = document.createElement('option');
            option.value = year.id;
            option.textContent = year.yearCode;
            if (year.isActive) {
                option.selected = true;
                selectedAcademicYearId = year.id;
            }
            academicYearSelect.appendChild(option);
        });
        
        // Load semesters for active year
        if (selectedAcademicYearId) {
            await loadSemesters(selectedAcademicYearId);
        }
    } catch (error) {
        console.error('Error loading academic years:', error);
        showToast('Failed to load academic years', 'error');
        academicYearSelect.innerHTML = '<option value="">Error loading years</option>';
    }
}

// Load semesters for selected academic year
async function loadSemesters(academicYearId) {
    try {
        const response = await deanship.getSemesters(academicYearId);
        semesters = response.data || [];
        
        semesterSelect.innerHTML = '<option value="">Select semester</option>';
        semesters.forEach(semester => {
            const option = document.createElement('option');
            option.value = semester.id;
            option.textContent = `${semester.type} Semester`;
            semesterSelect.appendChild(option);
        });
        
        // Auto-select first semester if available
        if (semesters.length > 0) {
            selectedSemesterId = semesters[0].id;
            semesterSelect.value = selectedSemesterId;
            await loadCourses(selectedSemesterId);
        }
    } catch (error) {
        console.error('Error loading semesters:', error);
        showToast('Failed to load semesters', 'error');
        semesterSelect.innerHTML = '<option value="">Error loading semesters</option>';
    }
}

// Load courses for selected semester
async function loadCourses(semesterId) {
    try {
        coursesContainer.innerHTML = `
            <div class="animate-pulse space-y-4">
                <div class="h-32 bg-gray-200 rounded-lg"></div>
                <div class="h-32 bg-gray-200 rounded-lg"></div>
            </div>
        `;
        
        const response = await professor.getMyCourses(semesterId);
        courses = response.data || [];
        renderCourses();
    } catch (error) {
        console.error('Error loading courses:', error);
        showToast('Failed to load courses', 'error');
        coursesContainer.innerHTML = '<p class="text-red-600 text-sm text-center py-4">Error loading courses</p>';
    }
}

// Load dashboard overview
async function loadDashboardOverview() {
    if (!selectedSemesterId) {
        return;
    }
    
    try {
        const response = await professor.getDashboardOverview(selectedSemesterId);
        const overview = response.data || {};
        
        // Update overview cards
        document.getElementById('totalCoursesCount').textContent = overview.totalCourses || 0;
        document.getElementById('submittedDocsCount').textContent = overview.submittedDocuments || 0;
        document.getElementById('pendingDocsCount').textContent = overview.pendingDocuments || 0;
        document.getElementById('overdueDocsCount').textContent = overview.overdueDocuments || 0;
        
        // Update summary text
        const summaryEl = document.getElementById('dashboardSummary');
        if (overview.totalCourses === 0) {
            summaryEl.textContent = 'You have no courses assigned for this semester.';
        } else {
            const pendingText = overview.pendingDocuments > 0 ? `${overview.pendingDocuments} pending` : 'all documents submitted';
            const overdueText = overview.overdueDocuments > 0 ? ` (${overview.overdueDocuments} overdue)` : '';
            summaryEl.textContent = `You have ${overview.totalCourses} course(s) with ${pendingText}${overdueText}.`;
        }
    } catch (error) {
        console.error('Error loading dashboard overview:', error);
        // Set default values on error
        document.getElementById('totalCoursesCount').textContent = '0';
        document.getElementById('submittedDocsCount').textContent = '0';
        document.getElementById('pendingDocsCount').textContent = '0';
        document.getElementById('overdueDocsCount').textContent = '0';
        document.getElementById('dashboardSummary').textContent = 'Unable to load dashboard data.';
    }
}

// Render courses
function renderCourses() {
    if (courses.length === 0) {
        coursesContainer.innerHTML = '';
        emptyState.classList.remove('hidden');
        return;
    }

    emptyState.classList.add('hidden');
    coursesContainer.innerHTML = courses.map(course => createCourseCard(course)).join('');
}

// Create course card
function createCourseCard(course) {
    const documentTypes = Object.entries(course.documentStatuses || {});
    
    return `
        <div class="border border-gray-200 rounded-lg p-6 mb-4">
            <div class="flex justify-between items-start mb-4">
                <div>
                    <h3 class="text-lg font-semibold text-gray-900">${course.courseCode} - ${course.courseName}</h3>
                    <p class="text-sm text-gray-600">${course.departmentName} • ${course.courseLevel || 'N/A'}</p>
                </div>
            </div>

            ${documentTypes.length === 0 ? `
                <p class="text-sm text-gray-500 italic">No required documents for this course</p>
            ` : `
                <div class="space-y-3">
                    ${documentTypes.map(([docType, status]) => createDocumentTypeRow(course, docType, status)).join('')}
                </div>
            `}
        </div>
    `;
}

// Create document type row
function createDocumentTypeRow(course, docType, status) {
    const isSubmitted = status.status === 'UPLOADED';
    const isOverdue = status.status === 'OVERDUE';
    const isNotUploaded = status.status === 'NOT_UPLOADED';
    
    let statusBadge = '';
    let statusClass = '';
    
    if (isSubmitted) {
        statusClass = status.isLateSubmission ? 'badge-warning' : 'badge-success';
        statusBadge = status.isLateSubmission ? 'Submitted (Late)' : 'Submitted';
    } else if (isOverdue) {
        statusClass = 'badge-danger';
        statusBadge = 'Overdue';
    } else {
        statusClass = 'badge-gray';
        statusBadge = 'Not Uploaded';
    }
    
    const deadline = status.deadline ? new Date(status.deadline) : null;
    const timeUntil = deadline ? getTimeUntil(deadline) : 'No deadline';
    const isPastDue = deadline ? isOverdue(deadline) : false;
    
    return `
        <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg border border-gray-200">
            <div class="flex-1">
                <div class="flex items-center gap-3 mb-2">
                    <h4 class="font-medium text-gray-900">${formatDocumentType(docType)}</h4>
                    <span class="badge ${statusClass}">${statusBadge}</span>
                </div>
                
                <div class="flex flex-wrap gap-4 text-sm text-gray-600">
                    ${deadline ? `
                        <div class="flex items-center">
                            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                            </svg>
                            <span class="${isPastDue ? 'text-red-600 font-medium' : ''}">${formatDate(deadline)}</span>
                        </div>
                        <div class="flex items-center ${isPastDue ? 'text-red-600' : 'text-blue-600'}">
                            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                            </svg>
                            ${timeUntil}
                        </div>
                    ` : '<span class="text-gray-500">No deadline</span>'}
                    
                    ${isSubmitted ? `
                        <div class="flex items-center text-green-600">
                            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                            </svg>
                            ${status.fileCount} file(s) uploaded
                        </div>
                    ` : ''}
                </div>
                
                ${status.maxFileCount || status.maxTotalSizeMb ? `
                    <div class="mt-2 text-xs text-gray-500">
                        Max: ${status.maxFileCount || 'unlimited'} files, ${status.maxTotalSizeMb || 'unlimited'} MB total
                    </div>
                ` : ''}
            </div>
            
            <div class="flex gap-2 ml-4">
                <button 
                    class="px-4 py-2 ${isSubmitted ? 'bg-gray-600' : 'bg-blue-600'} text-white rounded-md hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-blue-500 transition text-sm font-medium"
                    onclick="window.openUploadModal(${course.courseAssignmentId}, '${docType}', ${status.submissionId || 'null'}, ${isSubmitted}, ${JSON.stringify(status).replace(/"/g, '&quot;')})"
                >
                    ${isSubmitted ? 'Replace Files' : 'Upload Files'}
                </button>
                ${isSubmitted && status.fileCount > 0 ? `
                    <button 
                        class="px-3 py-2 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-400 transition text-sm"
                        onclick="window.viewSubmissionFiles(${status.submissionId})"
                        title="View files"
                    >
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                        </svg>
                    </button>
                ` : ''}
            </div>
        </div>
    `;
}

// Format document type enum to readable text
function formatDocumentType(docType) {
    const typeMap = {
        'SYLLABUS': 'Syllabus',
        'EXAM': 'Exam',
        'ASSIGNMENT': 'Assignment',
        'PROJECT_DOCS': 'Project Documents',
        'LECTURE_NOTES': 'Lecture Notes',
        'OTHER': 'Other'
    };
    return typeMap[docType] || docType;
}

// Load notifications
async function loadNotifications() {
    try {
        const response = await professor.getNotifications();
        notifications = (response.data || []).sort((a, b) => 
            new Date(b.createdAt) - new Date(a.createdAt)
        );
        
        const unseenCount = notifications.filter(n => !n.seen).length;
        if (unseenCount > 0) {
            notificationBadge.classList.remove('hidden');
        } else {
            notificationBadge.classList.add('hidden');
        }

        renderNotifications();
    } catch (error) {
        console.error('Error loading notifications:', error);
    }
}

// Render notifications
function renderNotifications() {
    if (notifications.length === 0) {
        notificationsList.innerHTML = '<div class="p-8 text-center"><p class="text-gray-500 text-sm">No notifications</p></div>';
        return;
    }

    notificationsList.innerHTML = notifications.map(notif => `
        <div class="p-4 border-b border-gray-100 ${notif.seen ? 'bg-white' : 'bg-blue-50'} cursor-pointer hover:bg-gray-50 transition"
            onclick="window.markNotificationSeen(${notif.id})">
            <div class="flex items-start gap-3">
                <div class="flex-shrink-0 mt-1">
                    <div class="w-2 h-2 ${notif.seen ? 'bg-gray-300' : 'bg-blue-600'} rounded-full"></div>
                </div>
                <div class="flex-1 min-w-0">
                    <p class="text-sm ${notif.seen ? 'text-gray-700' : 'text-gray-900 font-medium'}">${notif.message}</p>
                    <p class="text-xs text-gray-500 mt-1">${formatDate(notif.createdAt)}</p>
                </div>
            </div>
        </div>
    `).join('');
}

// Mark notification as seen
window.markNotificationSeen = async (notificationId) => {
    try {
        await professor.markNotificationSeen(notificationId);
        loadNotifications();
    } catch (error) {
        console.error('Error marking notification as seen:', error);
    }
};

// Open upload modal
window.openUploadModal = (courseAssignmentId, documentType, submissionId, isReplacement, statusData) => {
    const status = typeof statusData === 'string' ? JSON.parse(statusData) : statusData;
    const maxFileCount = status.maxFileCount || 5;
    const maxTotalSizeMb = status.maxTotalSizeMb || 50;
    
    const content = `
        <form id="uploadForm" class="space-y-4">
            <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
                <h4 class="font-medium text-blue-900 mb-2">${formatDocumentType(documentType)}</h4>
                <div class="text-sm text-blue-800 space-y-1">
                    <p><strong>Max files:</strong> ${maxFileCount}</p>
                    <p><strong>Max total size:</strong> ${maxTotalSizeMb} MB</p>
                    <p><strong>Allowed types:</strong> PDF, ZIP</p>
                </div>
            </div>
            
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                    Select Files
                </label>
                <div class="file-upload-area" id="fileUploadArea">
                    <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                    </svg>
                    <p class="mt-2 text-sm text-gray-600">Click to upload or drag and drop</p>
                    <p class="text-xs text-gray-500 mt-1">PDF and ZIP files only</p>
                    <input 
                        type="file" 
                        id="fileInput" 
                        name="files" 
                        multiple
                        class="hidden"
                        accept=".pdf,.zip"
                    >
                </div>
                <p id="fileError" class="text-red-600 text-sm mt-1 hidden"></p>
            </div>
            
            <div id="filePreviewList" class="hidden space-y-2">
                <label class="block text-sm font-medium text-gray-700 mb-2">
                    Selected Files
                </label>
                <div id="filePreviewContainer" class="space-y-2 max-h-48 overflow-y-auto">
                    <!-- File previews will be inserted here -->
                </div>
            </div>
            
            <div>
                <label for="notesInput" class="block text-sm font-medium text-gray-700 mb-2">
                    Notes (Optional)
                </label>
                <textarea 
                    id="notesInput" 
                    rows="3" 
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Add any notes about this submission..."
                ></textarea>
            </div>
            
            <div id="uploadProgress" class="hidden">
                <div class="progress-bar">
                    <div id="progressFill" class="progress-bar-fill" style="width: 0%"></div>
                </div>
                <p id="progressText" class="text-sm text-gray-600 mt-1 text-center">Uploading...</p>
            </div>
        </form>
    `;

    const modal = showModal(
        isReplacement ? 'Replace Files' : 'Upload Files', 
        content, 
        {
            size: 'lg',
            buttons: [
                {
                    text: 'Cancel',
                    className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                    action: 'cancel',
                    onClick: (close) => close(),
                },
                {
                    text: isReplacement ? 'Replace' : 'Upload',
                    className: 'bg-blue-600 text-white hover:bg-blue-700',
                    action: 'upload',
                    onClick: async (close) => {
                        await handleFileUpload(
                            courseAssignmentId, 
                            documentType, 
                            submissionId, 
                            isReplacement, 
                            maxFileCount, 
                            maxTotalSizeMb,
                            close
                        );
                    },
                },
            ],
        }
    );

    setupFileUploadHandlers(maxFileCount, maxTotalSizeMb);
};

// Setup file upload handlers
function setupFileUploadHandlers(maxFileCount, maxTotalSizeMb) {
    const fileInput = document.getElementById('fileInput');
    const fileUploadArea = document.getElementById('fileUploadArea');
    const filePreviewList = document.getElementById('filePreviewList');
    const filePreviewContainer = document.getElementById('filePreviewContainer');
    const fileError = document.getElementById('fileError');

    fileUploadArea.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', (e) => {
        handleFileSelection(e.target.files, maxFileCount, maxTotalSizeMb);
    });

    // Drag and drop
    fileUploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        fileUploadArea.classList.add('dragover');
    });

    fileUploadArea.addEventListener('dragleave', () => {
        fileUploadArea.classList.remove('dragover');
    });

    fileUploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        fileUploadArea.classList.remove('dragover');
        handleFileSelection(e.dataTransfer.files, maxFileCount, maxTotalSizeMb);
    });
}

// Handle file selection
function handleFileSelection(files, maxFileCount, maxTotalSizeMb) {
    const fileInput = document.getElementById('fileInput');
    const filePreviewList = document.getElementById('filePreviewList');
    const filePreviewContainer = document.getElementById('filePreviewContainer');
    const fileError = document.getElementById('fileError');
    
    fileError.classList.add('hidden');
    
    if (files.length === 0) return;
    
    // Validate file count
    if (files.length > maxFileCount) {
        fileError.textContent = `Maximum ${maxFileCount} files allowed`;
        fileError.classList.remove('hidden');
        return;
    }
    
    // Validate file types and calculate total size
    let totalSize = 0;
    const validFiles = [];
    
    for (let file of files) {
        const ext = file.name.split('.').pop().toLowerCase();
        if (ext !== 'pdf' && ext !== 'zip') {
            fileError.textContent = `Invalid file type: ${file.name}. Only PDF and ZIP files are allowed.`;
            fileError.classList.remove('hidden');
            return;
        }
        totalSize += file.size;
        validFiles.push(file);
    }
    
    // Validate total size
    const totalSizeMb = totalSize / (1024 * 1024);
    if (totalSizeMb > maxTotalSizeMb) {
        fileError.textContent = `Total file size (${totalSizeMb.toFixed(2)} MB) exceeds maximum of ${maxTotalSizeMb} MB`;
        fileError.classList.remove('hidden');
        return;
    }
    
    // Update file input
    const dataTransfer = new DataTransfer();
    validFiles.forEach(file => dataTransfer.items.add(file));
    fileInput.files = dataTransfer.files;
    
    // Show file previews
    filePreviewList.classList.remove('hidden');
    filePreviewContainer.innerHTML = validFiles.map((file, index) => `
        <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200">
            <div class="flex items-center space-x-3 flex-1">
                <span class="text-2xl">${getFileIcon(file.type)}</span>
                <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-gray-900 truncate">${file.name}</p>
                    <p class="text-xs text-gray-500">${formatFileSize(file.size)}</p>
                </div>
            </div>
            <button 
                type="button"
                onclick="removeFileFromSelection(${index})"
                class="text-red-600 hover:text-red-700 p-1 rounded hover:bg-red-50"
                title="Remove"
            >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
            </button>
        </div>
    `).join('');
}

// Remove file from selection
window.removeFileFromSelection = (index) => {
    const fileInput = document.getElementById('fileInput');
    const files = Array.from(fileInput.files);
    files.splice(index, 1);
    
    const dataTransfer = new DataTransfer();
    files.forEach(file => dataTransfer.items.add(file));
    fileInput.files = dataTransfer.files;
    
    // Re-render previews
    const filePreviewContainer = document.getElementById('filePreviewContainer');
    const filePreviewList = document.getElementById('filePreviewList');
    
    if (files.length === 0) {
        filePreviewList.classList.add('hidden');
    } else {
        filePreviewContainer.innerHTML = files.map((file, idx) => `
            <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200">
                <div class="flex items-center space-x-3 flex-1">
                    <span class="text-2xl">${getFileIcon(file.type)}</span>
                    <div class="flex-1 min-w-0">
                        <p class="text-sm font-medium text-gray-900 truncate">${file.name}</p>
                        <p class="text-xs text-gray-500">${formatFileSize(file.size)}</p>
                    </div>
                </div>
                <button 
                    type="button"
                    onclick="removeFileFromSelection(${idx})"
                    class="text-red-600 hover:text-red-700 p-1 rounded hover:bg-red-50"
                    title="Remove"
                >
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>
        `).join('');
    }
};

// Handle file upload
async function handleFileUpload(courseAssignmentId, documentType, submissionId, isReplacement, maxFileCount, maxTotalSizeMb, closeModal) {
    const fileInput = document.getElementById('fileInput');
    const notesInput = document.getElementById('notesInput');
    const fileError = document.getElementById('fileError');
    const uploadProgress = document.getElementById('uploadProgress');
    const progressFill = document.getElementById('progressFill');
    const progressText = document.getElementById('progressText');
    
    const files = fileInput.files;
    const notes = notesInput.value.trim();
    
    if (files.length === 0) {
        fileError.textContent = 'Please select at least one file';
        fileError.classList.remove('hidden');
        return;
    }
    
    // Create FormData
    const formData = new FormData();
    for (let file of files) {
        formData.append('files', file);
    }
    if (notes) {
        formData.append('notes', notes);
    }
    
    // Show progress
    uploadProgress.classList.remove('hidden');
    
    // Disable upload button
    const uploadBtn = document.querySelector('[data-action="upload"]');
    if (uploadBtn) {
        uploadBtn.disabled = true;
        uploadBtn.textContent = 'Uploading...';
    }
    
    try {
        if (isReplacement && submissionId) {
            await professor.replaceFiles(
                submissionId,
                formData,
                (percent) => {
                    progressFill.style.width = `${percent}%`;
                    progressText.textContent = `Uploading... ${Math.round(percent)}%`;
                }
            );
            showToast('Files replaced successfully', 'success');
        } else {
            await professor.uploadFiles(
                courseAssignmentId,
                documentType,
                formData,
                (percent) => {
                    progressFill.style.width = `${percent}%`;
                    progressText.textContent = `Uploading... ${Math.round(percent)}%`;
                }
            );
            showToast('Files uploaded successfully', 'success');
        }
        
        // Reload courses
        if (selectedSemesterId) {
            await loadCourses(selectedSemesterId);
        }
        closeModal();
    } catch (error) {
        console.error('Upload error:', error);
        fileError.textContent = error.message || 'Upload failed';
        fileError.classList.remove('hidden');
        
        if (uploadBtn) {
            uploadBtn.disabled = false;
            uploadBtn.textContent = isReplacement ? 'Replace' : 'Upload';
        }
        uploadProgress.classList.add('hidden');
    }
}

// View submission files
window.viewSubmissionFiles = async (submissionId) => {
    try {
        const response = await professor.getSubmissionFiles(submissionId);
        const files = response.data || [];
        
        if (files.length === 0) {
            showToast('No files found', 'info');
            return;
        }
        
        const content = `
            <div class="space-y-3">
                <p class="text-sm text-gray-600 mb-4">
                    ${files.length} file(s) submitted
                </p>
                ${files.map((file, index) => `
                    <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200 hover:bg-gray-100 transition-colors">
                        <div class="flex items-center space-x-3 flex-1">
                            <span class="text-2xl">${getFileIcon(file.fileType)}</span>
                            <div class="flex-1 min-w-0">
                                <p class="text-sm font-medium text-gray-900 truncate">${file.originalFilename}</p>
                                <p class="text-xs text-gray-500">${formatFileSize(file.fileSize)}</p>
                            </div>
                        </div>
                        <div class="flex items-center space-x-2">
                            <button 
                                onclick="downloadSubmissionFile(${file.id}, '${file.originalFilename}')"
                                class="text-blue-600 hover:text-blue-700 p-2 rounded hover:bg-blue-50"
                                title="Download"
                            >
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                                </svg>
                            </button>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
        
        showModal('Submitted Files', content, {
            size: 'md',
            buttons: [
                {
                    text: 'Close',
                    className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                    action: 'close',
                    onClick: (close) => close(),
                },
            ],
        });
    } catch (error) {
        console.error('Error loading files:', error);
        showToast('Failed to load files', 'error');
    }
};

// Download submission file
window.downloadSubmissionFile = async (fileId, filename) => {
    try {
        const response = await professor.downloadSubmissionFile(fileId);
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        showToast('File downloaded successfully', 'success');
    } catch (error) {
        console.error('Download error:', error);
        showToast('Failed to download file', 'error');
    }
};

function getFileIcon(mimeType) {
    if (!mimeType) return '📎';
    if (mimeType.includes('pdf')) return '📄';
    if (mimeType.includes('word') || mimeType.includes('document')) return '📝';
    if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return '📊';
    if (mimeType.includes('powerpoint') || mimeType.includes('presentation')) return '📽️';
    if (mimeType.includes('image')) return '🖼️';
    if (mimeType.includes('video')) return '🎥';
    if (mimeType.includes('audio')) return '🎵';
    if (mimeType.includes('zip') || mimeType.includes('compressed')) return '📦';
    return '📎';
}


// Tab switching
let currentTab = 'courses';
let currentPath = '';
let fileExplorerData = null;

window.switchTab = (tab) => {
    currentTab = tab;
    
    // Update tab buttons
    document.getElementById('coursesTab').classList.remove('active', 'border-blue-600', 'text-blue-600');
    document.getElementById('coursesTab').classList.add('border-transparent', 'text-gray-500');
    document.getElementById('fileExplorerTab').classList.remove('active', 'border-blue-600', 'text-blue-600');
    document.getElementById('fileExplorerTab').classList.add('border-transparent', 'text-gray-500');
    
    if (tab === 'courses') {
        document.getElementById('coursesTab').classList.add('active', 'border-blue-600', 'text-blue-600');
        document.getElementById('coursesTab').classList.remove('border-transparent', 'text-gray-500');
        coursesTabContent.classList.remove('hidden');
        fileExplorerTabContent.classList.add('hidden');
    } else if (tab === 'fileExplorer') {
        document.getElementById('fileExplorerTab').classList.add('active', 'border-blue-600', 'text-blue-600');
        document.getElementById('fileExplorerTab').classList.remove('border-transparent', 'text-gray-500');
        coursesTabContent.classList.add('hidden');
        fileExplorerTabContent.classList.remove('hidden');
        
        // Load file explorer if semester is selected
        if (selectedAcademicYearId && selectedSemesterId) {
            loadFileExplorer();
        } else {
            fileExplorerContainer.innerHTML = '<p class="text-gray-500 text-center py-8">Please select an academic year and semester first</p>';
        }
    }
};

// Load file explorer
async function loadFileExplorer(path = '') {
    try {
        fileExplorerContainer.innerHTML = `
            <div class="animate-pulse space-y-4">
                <div class="h-16 bg-gray-200 rounded-lg"></div>
                <div class="h-16 bg-gray-200 rounded-lg"></div>
            </div>
        `;
        
        let response;
        if (path) {
            response = await professor.getFileExplorerNode(path);
        } else {
            response = await professor.getFileExplorerRoot(selectedAcademicYearId, selectedSemesterId);
        }
        
        fileExplorerData = response.data;
        currentPath = path;
        renderFileExplorer(fileExplorerData);
        renderBreadcrumbs(path);
    } catch (error) {
        console.error('Error loading file explorer:', error);
        showToast('Failed to load file explorer', 'error');
        fileExplorerContainer.innerHTML = '<p class="text-red-600 text-center py-8">Error loading file explorer</p>';
    }
}

// Render file explorer
function renderFileExplorer(data) {
    if (!data) {
        fileExplorerContainer.innerHTML = '<p class="text-gray-500 text-center py-8">No data available</p>';
        return;
    }
    
    const children = data.children || [];
    
    if (children.length === 0) {
        fileExplorerContainer.innerHTML = '<p class="text-gray-500 text-center py-8">No items found</p>';
        return;
    }
    
    // Separate folders and files
    const folders = children.filter(item => item.type !== 'FILE');
    const files = children.filter(item => item.type === 'FILE');
    
    let html = '';
    
    // Render folders
    if (folders.length > 0) {
        html += '<div class="mb-6"><h3 class="text-sm font-medium text-gray-700 mb-3">Folders</h3><div class="space-y-2">';
        folders.forEach(folder => {
            const canWrite = folder.canWrite ? 'text-blue-600' : 'text-gray-600';
            const writeIndicator = folder.canWrite ? '<span class="text-xs text-blue-600 ml-2">(Your folder)</span>' : '';
            
            html += `
                <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200 hover:bg-gray-100 transition-colors cursor-pointer"
                    onclick="navigateToPath('${folder.path}')">
                    <div class="flex items-center space-x-3 flex-1">
                        <svg class="w-6 h-6 ${canWrite}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                        </svg>
                        <div class="flex-1">
                            <p class="text-sm font-medium text-gray-900">${folder.name}${writeIndicator}</p>
                            ${folder.metadata && folder.metadata.description ? `<p class="text-xs text-gray-500">${folder.metadata.description}</p>` : ''}
                        </div>
                    </div>
                    <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                    </svg>
                </div>
            `;
        });
        html += '</div></div>';
    }
    
    // Render files
    if (files.length > 0) {
        html += '<div><h3 class="text-sm font-medium text-gray-700 mb-3">Files</h3><div class="space-y-2">';
        files.forEach(file => {
            const metadata = file.metadata || {};
            const fileSize = metadata.fileSize ? formatFileSize(metadata.fileSize) : 'Unknown size';
            const uploadDate = metadata.uploadedAt ? formatDate(metadata.uploadedAt) : 'Unknown date';
            
            html += `
                <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200 hover:bg-gray-100 transition-colors">
                    <div class="flex items-center space-x-3 flex-1">
                        <span class="text-2xl">${getFileIcon(metadata.fileType)}</span>
                        <div class="flex-1 min-w-0">
                            <p class="text-sm font-medium text-gray-900 truncate">${file.name}</p>
                            <p class="text-xs text-gray-500">${fileSize} • ${uploadDate}</p>
                        </div>
                    </div>
                    <div class="flex items-center space-x-2">
                        ${file.canRead ? `
                            <button 
                                onclick="downloadFileFromExplorer(${file.entityId}, '${file.name}')"
                                class="text-blue-600 hover:text-blue-700 p-2 rounded hover:bg-blue-50"
                                title="Download"
                            >
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                                </svg>
                            </button>
                        ` : ''}
                    </div>
                </div>
            `;
        });
        html += '</div></div>';
    }
    
    fileExplorerContainer.innerHTML = html;
}

// Render breadcrumbs
function renderBreadcrumbs(path) {
    if (!path) {
        breadcrumbs.innerHTML = `
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path>
            </svg>
            <button onclick="loadFileExplorer('')" class="text-blue-600 hover:underline">Home</button>
        `;
        return;
    }
    
    const parts = path.split('/').filter(p => p);
    let html = `
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path>
        </svg>
        <button onclick="loadFileExplorer('')" class="text-blue-600 hover:underline">Home</button>
    `;
    
    let currentPath = '';
    parts.forEach((part, index) => {
        currentPath += (currentPath ? '/' : '') + part;
        const isLast = index === parts.length - 1;
        
        html += `
            <svg class="w-4 h-4 mx-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
            </svg>
        `;
        
        if (isLast) {
            html += `<span class="text-gray-700 font-medium">${part}</span>`;
        } else {
            html += `<button onclick="loadFileExplorer('${currentPath}')" class="text-blue-600 hover:underline">${part}</button>`;
        }
    });
    
    breadcrumbs.innerHTML = html;
}

// Navigate to path
window.navigateToPath = (path) => {
    loadFileExplorer(path);
};

// Download file from explorer
window.downloadFileFromExplorer = async (fileId, filename) => {
    try {
        const response = await professor.downloadSubmissionFile(fileId);
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        showToast('File downloaded successfully', 'success');
    } catch (error) {
        console.error('Download error:', error);
        showToast('Failed to download file', 'error');
    }
};
