/**
 * Professor Dashboard - Semester-based
 * 
 * MASTER DESIGN REFERENCE: Professor Dashboard File Explorer Implementation
 * 
 * This file contains the CANONICAL IMPLEMENTATION of the File Explorer for the Professor role.
 * The File Explorer rendering logic, folder card design, file table layout, and role-specific
 * labels defined here serve as the authoritative reference for all other dashboards.
 * 
 * Key Implementation Patterns:
 * 
 * 1. ACADEMIC YEAR AND SEMESTER SELECTOR PATTERN:
 *    - loadAcademicYears(): Loads all academic years and auto-selects active year
 *    - loadSemesters(academicYearId): Loads semesters for selected year
 *    - Event handlers on academicYearSelect and semesterSelect trigger data refresh
 *    - Semester selector is disabled until academic year is selected
 *    - Selection persists across tab switches
 * 
 * 2. FILE EXPLORER RENDERING PATTERN:
 *    - loadFileExplorer(path): Loads file explorer data for current path
 *    - renderFileExplorer(data): Renders folders and files with role-specific styling
 *    - renderBreadcrumbs(path): Renders breadcrumb navigation with home icon
 *    - Folder cards: Blue cards (bg-blue-50, border-blue-200) with hover effects
 *    - File items: White cards with metadata badges and download buttons
 * 
 * 3. ROLE-SPECIFIC LABELS (Professor):
 *    - "Your Folder" badge (bg-blue-100, text-blue-800) for folders with canWrite=true
 *    - "Read Only" badge (bg-gray-100, text-gray-600) for folders with canWrite=false
 *    - Labels include SVG icons for visual clarity
 * 
 * 4. FOLDER CARD DESIGN:
 *    - Container: flex items-center justify-between p-4 rounded-lg border
 *    - Background: bg-blue-50 for writable, bg-gray-50 for read-only
 *    - Border: border-blue-200 for writable, border-gray-200 for read-only
 *    - Hover: hover:bg-blue-100 or hover:bg-gray-100
 *    - Icon: w-7 h-7 folder icon in blue or gray
 *    - Arrow: w-5 h-5 with group-hover:translate-x-1 transition
 * 
 * 5. FILE TABLE DESIGN:
 *    - White cards with border border-gray-200 and hover:shadow-lg
 *    - File icon container: w-12 h-12 bg-gray-50 rounded-lg
 *    - Metadata badges: inline-flex px-2 py-0.5 rounded text-xs bg-gray-100 text-gray-700
 *    - Download button: bg-blue-600 hover:bg-blue-700 p-2.5 rounded-lg shadow-sm
 * 
 * All other dashboards (HOD, Deanship) should replicate these patterns with their
 * role-specific configurations while maintaining visual consistency.
 */

import { professor, fileExplorer, getUserInfo, isAuthenticated, redirectToLogin, clearAuthData, initializeAuth } from './api.js';
// FIX 1: Removed 'isOverdue' from imports to avoid conflict with local definition
import { showToast, showModal, formatDate, getTimeUntil, formatFileSize } from './ui.js';
import { FileExplorer } from './file-explorer.js';
import { fileExplorerState } from './file-explorer-state.js';

// State variables - defined before auth check
let academicYears = [];
let semesters = [];
let courses = [];
let notifications = [];
let selectedAcademicYearId = null;
let selectedSemesterId = null;
let fileExplorerInstance = null;

// Check authentication with token validation
(async function initAuth() {
    if (!isAuthenticated()) {
        // Update UI to show authentication required message
        const academicYearSelect = document.getElementById('academicYearSelect');
        const semesterSelect = document.getElementById('semesterSelect');
        if (academicYearSelect) {
            academicYearSelect.innerHTML = '<option value="">Authentication required</option>';
        }
        if (semesterSelect) {
            semesterSelect.innerHTML = '<option value="">Authentication required</option>';
        }
        redirectToLogin();
        return;
    }
    
    // Validate token with server
    const isValid = await initializeAuth();
    if (!isValid) {
        redirectToLogin('session_expired');
        return;
    }
    
    const userInfo = getUserInfo();
    if (userInfo.role !== 'ROLE_PROFESSOR') {
        showToast('Access denied - Professor privileges required', 'error');
        setTimeout(() => redirectToLogin('access_denied'), 2000);
        return;
    }
    
    // Continue with dashboard initialization
    initializeDashboard();
})();

const userInfo = getUserInfo();

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

// Initialize Modern Dropdowns
let academicYearDropdown = null;
let semesterDropdown = null;

function initializeModernDropdowns() {
    if (typeof initModernDropdowns === 'function') {
        const instances = initModernDropdowns('#academicYearSelect, #semesterSelect');
        if (instances.length >= 1) academicYearDropdown = instances[0];
        if (instances.length >= 2) semesterDropdown = instances[1];
    }
}

/**
 * Get current date/time in Palestine timezone (Asia/Jerusalem)
 * Palestine follows Israel Standard Time (IST) / Israel Daylight Time (IDT)
 * @returns {Date} Current date in Palestine timezone
 */
function getPalestineDate() {
    // Create a date string in Palestine timezone
    const palestineTimeString = new Date().toLocaleString('en-US', { 
        timeZone: 'Asia/Jerusalem' 
    });
    return new Date(palestineTimeString);
}

/**
 * Calculate the current academic year code based on Palestine timezone
 * Academic year starts in September and ends in August
 * e.g., September 2024 - August 2025 = "2024-2025"
 * @returns {string} Academic year code (e.g., "2024-2025")
 */
function getCurrentAcademicYearCode() {
    const palestineDate = getPalestineDate();
    const month = palestineDate.getMonth(); // 0-11 (0 = January, 8 = September)
    const year = palestineDate.getFullYear();
    
    // Academic year starts in September (month 8)
    // If we're in September or later, we're in the year-nextYear academic year
    // If we're before September, we're in the previousYear-year academic year
    if (month >= 8) { // September (8) or later
        return `${year}-${year + 1}`;
    } else {
        return `${year - 1}-${year}`;
    }
}

/**
 * Determine the current semester type based on Palestine timezone
 * FIRST semester (Fall): September - January
 * SECOND semester (Spring): February - June
 * SUMMER semester: July - August
 * @returns {string} Semester type: "FIRST", "SECOND", or "SUMMER"
 */
function getCurrentSemesterType() {
    const palestineDate = getPalestineDate();
    const month = palestineDate.getMonth(); // 0-11
    
    // September (8) through January (0) = FIRST semester
    // February (1) through June (5) = SECOND semester
    // July (6) through August (7) = SUMMER semester
    
    if (month >= 8 || month === 0) { // Sep, Oct, Nov, Dec, Jan
        return 'FIRST';
    } else if (month >= 1 && month <= 5) { // Feb, Mar, Apr, May, Jun
        return 'SECOND';
    } else { // Jul, Aug
        return 'SUMMER';
    }
}

function refreshDropdowns() {
    if (academicYearSelect._modernDropdown) {
        academicYearSelect._modernDropdown.refresh();
    }
    if (semesterSelect._modernDropdown) {
        semesterSelect._modernDropdown.refresh();
    }
}

function showNotificationsDropdown() {
    if (!notificationsDropdown) return;
    
    // Position the dropdown using fixed positioning to avoid stacking context issues
    const btnRect = notificationsBtn.getBoundingClientRect();
    notificationsDropdown.style.position = 'fixed';
    notificationsDropdown.style.top = (btnRect.bottom + 8) + 'px';
    notificationsDropdown.style.right = (window.innerWidth - btnRect.right) + 'px';
    notificationsDropdown.style.left = 'auto';
    
    notificationsDropdown.classList.remove('hidden');
    loadNotifications();
}

function hideNotificationsDropdown() {
    if (!notificationsDropdown) return;
    notificationsDropdown.classList.add('hidden');
}


// Initialize dashboard after auth validation
function initializeDashboard() {
    professorName.textContent = userInfo.fullName;
    loadAcademicYears();
    loadNotifications();

    // Initialize modern dropdowns after a short delay to ensure DOM is ready
    setTimeout(initializeModernDropdowns, 100);

    // Poll notifications every 30 seconds
    setInterval(loadNotifications, 30000);
}

// Tab Switching
window.switchTab = function (tabName) {
    // Update sidebar tabs
    document.querySelectorAll('.nav-tab').forEach(tab => {
        tab.classList.remove('active');
    });

    const activeTab = document.querySelector(`.nav-tab[data-tab="${tabName}"]`);
    if (activeTab) {
        activeTab.classList.add('active');
    }

    // Update tab content
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.add('hidden');
    });

    const activeContent = document.getElementById(`${tabName}TabContent`);
    if (activeContent) {
        activeContent.classList.remove('hidden');
    }

    // Load data for specific tabs
    if (tabName === 'dashboard' && selectedSemesterId) {
        loadDashboardOverview();
    } else if (tabName === 'fileExplorer') {
        if (selectedAcademicYearId && selectedSemesterId) {
            if (!fileExplorerInstance) {
                initializeFileExplorer();
            }
            loadFileExplorer();
        } else {
            const container = document.getElementById('fileExplorerContainer');
            if (container) container.innerHTML = '<p class="text-gray-500 text-center py-8">Please select a semester to view your files</p>';
        }
    }
};

// Logout
logoutBtn.addEventListener('click', () => {
    clearAuthData();
    showToast('Logged out successfully', 'success');
    redirectToLogin();
});

// Show loading state for dashboard overview cards immediately
function showDashboardLoadingState() {
    const spinnerHtml = '<div class="flex items-center h-9"><div class="spinner spinner-sm"></div></div>';
    document.getElementById('totalCoursesCount').innerHTML = spinnerHtml;
    document.getElementById('submittedDocsCount').innerHTML = spinnerHtml;
    document.getElementById('pendingDocsCount').innerHTML = spinnerHtml;
    document.getElementById('overdueDocsCount').innerHTML = spinnerHtml;
    document.getElementById('dashboardSummary').textContent = 'Loading dashboard data...';
}

// Notifications dropdown toggle
notificationsBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    if (notificationsDropdown.classList.contains('hidden')) {
        showNotificationsDropdown();
    } else {
        hideNotificationsDropdown();
    }
});

closeNotificationsDropdown.addEventListener('click', () => {
    hideNotificationsDropdown();
});

// Close dropdown when clicking outside
document.addEventListener('click', (e) => {
    if (!notificationsDropdown.classList.contains('hidden') &&
        !notificationsDropdown.contains(e.target) &&
        !notificationsBtn.contains(e.target)) {
        hideNotificationsDropdown();
    }
});

// Handle semester change logic
async function handleSemesterChange(semesterId) {
    selectedSemesterId = semesterId;

    if (selectedSemesterId) {
        // Immediately show loading spinners for dashboard overview
        showDashboardLoadingState();
        
        // Update FileExplorerState with new context
        const selectedYear = academicYears.find(y => y.id === selectedAcademicYearId);
        const selectedSemester = semesters.find(s => s.id === selectedSemesterId);
        
        if (selectedYear && selectedSemester) {
            fileExplorerState.setContext(
                selectedAcademicYearId,
                selectedSemesterId,
                selectedYear.yearCode,
                selectedSemester.type
            );
        }
        
        await loadCourses(selectedSemesterId);
        
        // Trigger File Explorer reload if on file-explorer tab
        const fileExplorerTab = document.getElementById('fileExplorerTabContent');
        if (fileExplorerTab && !fileExplorerTab.classList.contains('hidden')) {
            // Clear the file explorer state before loading new data
            fileExplorerState.resetData();
            loadFileExplorer();
        }
    } else {
        // Clear the file explorer when no semester is selected
        fileExplorerState.resetData();
        courses = [];
        renderCourses();
    }
}

// Academic year selection
academicYearSelect.addEventListener('change', async (e) => {
    selectedAcademicYearId = e.target.value ? parseInt(e.target.value) : null;
    selectedSemesterId = null;

    if (selectedAcademicYearId) {
        // Show loading state immediately
        showDashboardLoadingState();
        await loadSemesters(selectedAcademicYearId);
    } else {
        semesterSelect.innerHTML = '<option value="">Select academic year first</option>';
        refreshDropdowns();
        courses = [];
        renderCourses();
    }
});

// Semester selection
semesterSelect.addEventListener('change', async (e) => {
    const semesterId = e.target.value ? parseInt(e.target.value) : null;
    await handleSemesterChange(semesterId);
});

// Load academic years
async function loadAcademicYears() {
    try {
        // Track start time to ensure minimum loading duration
        const startTime = Date.now();
        
        academicYears = await professor.getAcademicYears();

        // Calculate remaining time to meet minimum loading duration
        const elapsedTime = Date.now() - startTime;
        const remainingTime = MIN_LOADING_TIME - elapsedTime;
        
        // Wait for remaining time if data loaded too quickly
        if (remainingTime > 0) {
            await new Promise(resolve => setTimeout(resolve, remainingTime));
        }

        if (academicYears.length === 0) {
            academicYearSelect.innerHTML = '<option value="">No academic years available</option>';
            refreshDropdowns();
            showToast('No academic years found. Please contact the administrator.', 'warning');
            return;
        }

        // Determine the current academic year based on Palestine timezone
        const currentYearCode = getCurrentAcademicYearCode();
        
        academicYearSelect.innerHTML = '<option value="">Select academic year</option>';
        academicYears.forEach(year => {
            const option = document.createElement('option');
            option.value = year.id;
            option.textContent = year.yearCode;
            // Select the year that matches the current academic year in Palestine
            // Fall back to active year if current year is not found
            if (year.yearCode === currentYearCode) {
                option.selected = true;
                selectedAcademicYearId = year.id;
            }
            academicYearSelect.appendChild(option);
        });
        
        // If no year matched the current academic year, fall back to the active year
        if (!selectedAcademicYearId) {
            const activeYear = academicYears.find(year => year.isActive);
            if (activeYear) {
                academicYearSelect.value = activeYear.id;
                selectedAcademicYearId = activeYear.id;
            } else if (academicYears.length > 0) {
                // If no active year, select the first one
                academicYearSelect.value = academicYears[0].id;
                selectedAcademicYearId = academicYears[0].id;
            }
        }

        // Refresh modern dropdown after populating options
        refreshDropdowns();

        // Load semesters for active year
        if (selectedAcademicYearId) {
            await loadSemesters(selectedAcademicYearId);
        }
    } catch (error) {
        console.error('Error loading academic years:', error);
        // Check if it's an authentication error
        if (error.message && error.message.includes('Unauthorized')) {
            // User will be redirected to login by the API layer
            return;
        }
        showToast('Unable to load academic years. Please refresh the page or contact support if the problem persists.', 'error');
        academicYearSelect.innerHTML = '<option value="">Error loading years</option>';
        refreshDropdowns();
    }
}

// Load semesters for selected academic year
async function loadSemesters(academicYearId) {
    try {
        semesters = await professor.getSemesters(academicYearId);

        if (semesters.length === 0) {
            semesterSelect.innerHTML = '<option value="">No semesters available for this year</option>';
            refreshDropdowns();
            showToast('No semesters found for the selected academic year.', 'warning');
            courses = [];
            renderCourses();
            return;
        }

        // Determine the current semester type based on Palestine timezone
        const currentSemesterType = getCurrentSemesterType();
        
        semesterSelect.innerHTML = '<option value="">Select semester</option>';
        semesters.forEach(semester => {
            const option = document.createElement('option');
            option.value = semester.id;
            option.textContent = `${semester.type} Semester`;
            semesterSelect.appendChild(option);
        });

        // Refresh modern dropdown after populating options
        refreshDropdowns();

        // Auto-select semester: prioritize current semester type, then active, then first
        let semesterToSelect = null;
        
        // First try to find a semester matching the current semester type
        semesterToSelect = semesters.find(s => s.type === currentSemesterType);
        
        // If no match for current semester type, try to find an active semester
        if (!semesterToSelect) {
            semesterToSelect = semesters.find(s => s.isActive);
        }
        
        // If still no match, fall back to the first semester
        if (!semesterToSelect && semesters.length > 0) {
            semesterToSelect = semesters[0];
        }
        
        if (semesterToSelect) {
            semesterSelect.value = semesterToSelect.id;
            refreshDropdowns();
            await handleSemesterChange(semesterToSelect.id);
        }
    } catch (error) {
        console.error('Error loading semesters:', error);
        showToast('Unable to load semesters. Please try selecting a different academic year or refresh the page.', 'error');
        semesterSelect.innerHTML = '<option value="">Error loading semesters</option>';
        refreshDropdowns();
    }
}

// Minimum loading time in milliseconds to prevent flickering shimmer effect
const MIN_LOADING_TIME = 1000;

// Load courses for selected semester
async function loadCourses(semesterId) {
    try {
        // Show skeleton loader
        coursesContainer.innerHTML = `
            <div class="space-y-4">
                ${createCourseSkeletonLoader()}
                ${createCourseSkeletonLoader()}
                ${createCourseSkeletonLoader()}
            </div>
        `;

        // Track start time to ensure minimum loading duration
        const startTime = Date.now();
        
        courses = await professor.getMyCourses(semesterId);
        
        // Calculate remaining time to meet minimum loading duration
        const elapsedTime = Date.now() - startTime;
        const remainingTime = MIN_LOADING_TIME - elapsedTime;
        
        // Wait for remaining time if data loaded too quickly
        if (remainingTime > 0) {
            await new Promise(resolve => setTimeout(resolve, remainingTime));
        }
        
        renderCourses();

        // Also load dashboard overview if the dashboard tab is currently active
        const dashboardTab = document.getElementById('dashboardTabContent');
        if (dashboardTab && !dashboardTab.classList.contains('hidden')) {
            await loadDashboardOverview();
        }
    } catch (error) {
        console.error('Error loading courses:', error);
        const errorMessage = error.message || 'An unexpected error occurred';
        showToast(`Unable to load courses: ${errorMessage}. Please refresh the page or contact support if the issue persists.`, 'error');
        coursesContainer.innerHTML = `
            <div class="text-center py-8">
                <svg class="mx-auto h-12 w-12 text-red-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
                <p class="text-red-600 text-sm font-medium">Failed to load courses</p>
                <p class="text-gray-500 text-xs mt-2">Please try refreshing the page</p>
            </div>
        `;
    }
}

// Create course skeleton loader
function createCourseSkeletonLoader() {
    return `
        <div class="skeleton-card">
            <div class="flex justify-between items-start mb-4">
                <div class="flex-1">
                    <div class="skeleton-line h-6 w-3-4 mb-2"></div>
                    <div class="skeleton-line h-4 w-1-2"></div>
                </div>
            </div>
            <div class="space-y-3">
                <div class="p-4 skeleton-inner-box rounded-lg">
                    <div class="skeleton-line h-4 w-1-4 mb-2"></div>
                    <div class="skeleton-line h-4 w-full"></div>
                </div>
                <div class="p-4 skeleton-inner-box rounded-lg">
                    <div class="skeleton-line h-4 w-1-4 mb-2"></div>
                    <div class="skeleton-line h-4 w-full"></div>
                </div>
            </div>
        </div>
    `;
}

// Load dashboard overview
async function loadDashboardOverview() {
    if (!selectedSemesterId) {
        return;
    }

    try {
        // Show loading spinners - wrapped to maintain consistent height with the number (text-3xl = ~36px)
        const spinnerHtml = '<div class="flex items-center h-9"><div class="spinner spinner-sm"></div></div>';
        document.getElementById('totalCoursesCount').innerHTML = spinnerHtml;
        document.getElementById('submittedDocsCount').innerHTML = spinnerHtml;
        document.getElementById('pendingDocsCount').innerHTML = spinnerHtml;
        document.getElementById('overdueDocsCount').innerHTML = spinnerHtml;
        document.getElementById('dashboardSummary').textContent = 'Loading dashboard data...';

        // Track start time to ensure minimum loading duration
        const startTime = Date.now();
        
        const overview = await professor.getDashboardOverview(selectedSemesterId);

        // Calculate remaining time to meet minimum loading duration
        const elapsedTime = Date.now() - startTime;
        const remainingTime = MIN_LOADING_TIME - elapsedTime;
        
        // Wait for remaining time if data loaded too quickly
        if (remainingTime > 0) {
            await new Promise(resolve => setTimeout(resolve, remainingTime));
        }

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
    const isStatusOverdue = status.status === 'OVERDUE';
    const isNotUploaded = status.status === 'NOT_UPLOADED';

    let statusBadge = '';
    let statusClass = '';
    let rowBorderClass = 'border-gray-200';
    let rowBgClass = 'bg-gray-50';

    if (isSubmitted) {
        statusClass = status.isLateSubmission ? 'badge-warning' : 'badge-success';
        statusBadge = status.isLateSubmission ? 'Submitted (Late)' : 'Submitted';
        rowBgClass = status.isLateSubmission ? 'bg-yellow-50' : 'bg-green-50';
        rowBorderClass = status.isLateSubmission ? 'border-yellow-200' : 'border-green-200';
    } else if (isStatusOverdue) {
        statusClass = 'badge-danger';
        statusBadge = 'Overdue';
        rowBgClass = 'bg-red-50';
        rowBorderClass = 'border-red-300';
    } else {
        statusClass = 'badge-gray';
        statusBadge = 'Not Uploaded';
    }

    const deadline = status.deadline ? new Date(status.deadline) : null;
    const timeUntil = deadline ? getTimeUntil(deadline) : 'No deadline';

    // FIX 2: Now using the local function defined at bottom of file
    const isPastDue = deadline ? isOverdue(deadline) : false;

    // Calculate urgency for upcoming deadlines
    let urgencyIndicator = '';
    if (deadline && !isSubmitted && !isPastDue) {
        const now = new Date();
        const hoursUntilDeadline = (deadline - now) / (1000 * 60 * 60);

        if (hoursUntilDeadline <= 24) {
            urgencyIndicator = '<span class="inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-800 ml-2 animate-pulse"><svg class="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"></path></svg>Due in ${Math.round(hoursUntilDeadline)} hours!</span>';
            rowBgClass = 'bg-red-50';
            rowBorderClass = 'border-red-300';
        } else if (hoursUntilDeadline <= 72) {
            urgencyIndicator = '<span class="inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold bg-orange-100 text-orange-800 ml-2"><svg class="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"></path></svg>Due soon</span>';
            rowBgClass = 'bg-orange-50';
            rowBorderClass = 'border-orange-200';
        }
    }

    return `
        <div class="flex items-center justify-between p-4 ${rowBgClass} rounded-lg border ${rowBorderClass} transition-all">
            <div class="flex-1">
                <div class="flex items-center gap-3 mb-2 flex-wrap">
                    <h4 class="font-medium text-gray-900">${formatDocumentType(docType)}</h4>
                    <span class="badge ${statusClass}">${statusBadge}</span>
                    ${urgencyIndicator}
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

// FIX 3: Added local helper function to replace the missing import
function isOverdue(dateStringOrDate) {
    if (!dateStringOrDate) return false;
    return new Date(dateStringOrDate) < new Date();
}

// ==================== File Upload Modal Functions ====================

/**
 * Open upload modal for file explorer
 * Opens the upload modal for uploading files to a folder in the file explorer
 */
window.openFileExplorerUploadModal = function() {
    console.log('=== OPEN FILE EXPLORER UPLOAD MODAL CALLED ===');
    console.log('Timestamp:', new Date().toISOString());
    
    // Get current node from FileExplorerState
    console.log('Getting current node from FileExplorerState...');
    const currentNode = fileExplorerState.getCurrentNode();
    console.log('Current node:', currentNode);
    
    // Validate current node exists
    if (!currentNode) {
        console.error('Validation failed: No current node');
        showToast('Please select a folder first', 'error');
        return;
    }
    console.log('✓ Current node exists');
    
    // Validate current node has an id or entityId
    const folderId = currentNode.id || currentNode.entityId;
    if (!folderId) {
        console.error('Validation failed: Current node has no ID or entityId');
        console.error('Current node:', currentNode);
        showToast('Invalid folder selected', 'error');
        return;
    }
    console.log('✓ Current node has ID:', folderId);
    
    // Validate current node type is 'DOCUMENT_TYPE' or 'SUBFOLDER' (category folder)
    const validTypes = ['DOCUMENT_TYPE', 'SUBFOLDER'];
    if (!validTypes.includes(currentNode.type)) {
        console.warn('Validation failed: Current node type is not valid for upload:', currentNode.type);
        showToast('You can only upload files to category folders (Course Notes, Exams, Syllabus, Assignments)', 'warning');
        return;
    }
    console.log('✓ Current node type is valid for upload:', currentNode.type);
    
    // Get modal elements
    console.log('Getting modal elements...');
    const modal = document.getElementById('uploadModal');
    const modalTitle = document.getElementById('uploadModalTitle');
    const filesInput = document.getElementById('uploadFiles');
    const notesInput = document.getElementById('uploadNotes');
    const errorDiv = document.getElementById('uploadError');
    
    console.log('Modal elements found:', {
        modal: !!modal,
        modalTitle: !!modalTitle,
        filesInput: !!filesInput,
        notesInput: !!notesInput,
        errorDiv: !!errorDiv
    });
    
    if (!modal) {
        console.error('Upload modal not found in DOM');
        return;
    }
    
    // Set modal title based on folder name
    if (modalTitle) {
        const title = `Upload Files to ${currentNode.name || 'Folder'}`;
        console.log('Setting modal title:', title);
        modalTitle.textContent = title;
    }
    
    // Clear previous file input
    if (filesInput) {
        console.log('Clearing previous file input');
        filesInput.value = '';
    }
    
    // Clear notes textarea
    if (notesInput) {
        console.log('Clearing notes textarea');
        notesInput.value = '';
    }
    
    // Hide error message div
    if (errorDiv) {
        console.log('Hiding error div');
        errorDiv.classList.add('hidden');
    }
    
    // Show modal by removing 'hidden' class
    console.log('Showing modal...');
    modal.classList.remove('hidden');
    console.log('✓ Modal opened successfully');
};

/**
 * Close upload modal
 */
window.closeUploadModal = function() {
    console.log('=== CLOSE UPLOAD MODAL CALLED ===');
    
    const modal = document.getElementById('uploadModal');
    const filesInput = document.getElementById('uploadFiles');
    const notesInput = document.getElementById('uploadNotes');
    const errorDiv = document.getElementById('uploadError');
    
    if (!modal) {
        console.error('Modal not found');
        return;
    }
    
    // Hide modal by adding 'hidden' class
    console.log('Hiding modal...');
    modal.classList.add('hidden');
    
    // Clear file input
    if (filesInput) {
        console.log('Clearing file input');
        filesInput.value = '';
    }
    
    // Clear notes textarea
    if (notesInput) {
        console.log('Clearing notes textarea');
        notesInput.value = '';
    }
    
    // Hide error message div
    if (errorDiv) {
        console.log('Hiding error div');
        errorDiv.classList.add('hidden');
    }
    
    console.log('✓ Modal closed successfully');
};

/**
 * Handle file upload - Validation
 * Validates files and prepares for upload
 */
window.handleUpload = async function() {
    console.log('=== UPLOAD BUTTON CLICKED ===');
    console.log('Timestamp:', new Date().toISOString());
    
    // Get references to DOM elements
    const uploadBtn = document.getElementById('uploadBtn');
    const errorDiv = document.getElementById('uploadError');
    const errorMessage = document.getElementById('uploadErrorMessage');
    const filesInput = document.getElementById('uploadFiles');
    const notesInput = document.getElementById('uploadNotes');
    
    console.log('DOM Elements found:', {
        uploadBtn: !!uploadBtn,
        errorDiv: !!errorDiv,
        errorMessage: !!errorMessage,
        filesInput: !!filesInput,
        notesInput: !!notesInput
    });
    
    // Helper function to show error in modal
    const showError = (message) => {
        console.error('Upload Error:', message);
        if (errorDiv && errorMessage) {
            errorMessage.textContent = message;
            errorDiv.classList.remove('hidden');
        }
    };
    
    // Helper function to hide error in modal
    const hideError = () => {
        if (errorDiv) {
            errorDiv.classList.add('hidden');
        }
    };
    
    // Get current node from FileExplorerState
    console.log('Getting current node from FileExplorerState...');
    const currentNode = fileExplorerState.getCurrentNode();
    console.log('Current node:', currentNode);
    
    // Validate current node exists
    if (!currentNode) {
        console.error('Validation failed: No current node');
        showError('No folder selected. Please select a folder first.');
        return;
    }
    console.log('✓ Current node exists');
    
    // Get folder ID or path from current node
    const folderId = currentNode.id || currentNode.entityId;
    const folderPath = currentNode.path;
    
    // Validate we have either folderId or folderPath
    if (!folderId && !folderPath) {
        console.error('Validation failed: Current node has no ID, entityId, or path');
        showError('Invalid folder selected. Please try again.');
        return;
    }
    
    if (folderId) {
        console.log('✓ Current node has ID:', folderId);
    } else {
        console.log('✓ Current node has path (will auto-create folder):', folderPath);
    }
    
    // Get selected files from file input
    const files = filesInput ? filesInput.files : null;
    console.log('Files from input:', files ? files.length : 0, 'file(s)');
    
    // Validate at least one file selected
    if (!files || files.length === 0) {
        console.error('Validation failed: No files selected');
        showError('Please select at least one file to upload.');
        return;
    }
    console.log('✓ Files selected:', files.length);
    
    // Validate each file size doesn't exceed 50MB
    const maxFileSize = 50 * 1024 * 1024; // 50MB in bytes
    const invalidFiles = [];
    
    console.log('Validating file sizes (max 50MB)...');
    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const fileSizeMB = (file.size / (1024 * 1024)).toFixed(2);
        console.log(`  File ${i + 1}: ${file.name} - ${fileSizeMB}MB`);
        if (file.size > maxFileSize) {
            invalidFiles.push(file.name);
        }
    }
    
    // Show error if any files exceed size limit
    if (invalidFiles.length > 0) {
        console.error('Validation failed: Files exceed size limit:', invalidFiles);
        const fileList = invalidFiles.length > 3 
            ? `${invalidFiles.slice(0, 3).join(', ')} and ${invalidFiles.length - 3} more` 
            : invalidFiles.join(', ');
        showError(`The following file(s) exceed the 50MB size limit: ${fileList}`);
        return;
    }
    console.log('✓ All files within size limit');
    
    // If all validations pass, hide any previous errors
    hideError();
    console.log('✓ All validations passed');
    
    // Task 19: Implement upload logic
    console.log('--- Starting upload process ---');
    
    // Construct FormData object
    const formData = new FormData();
    console.log('Creating FormData...');
    
    // Append each file with key 'files[]'
    for (let i = 0; i < files.length; i++) {
        console.log(`Appending file ${i + 1}: ${files[i].name}`);
        formData.append('files[]', files[i]);
    }
    
    // Append folderId or folderPath (backend supports both)
    if (folderId) {
        console.log('Appending folderId:', folderId);
        formData.append('folderId', folderId);
    } else {
        console.log('Appending folderPath:', folderPath);
        formData.append('folderPath', folderPath);
    }
    
    // Append notes if provided
    const notes = notesInput ? notesInput.value.trim() : '';
    if (notes) {
        console.log('Appending notes:', notes.substring(0, 50) + (notes.length > 50 ? '...' : ''));
        formData.append('notes', notes);
    } else {
        console.log('No notes provided');
    }
    
    // Set loading state: disable button, change text to "Uploading...", hide error
    console.log('Setting button to loading state...');
    if (uploadBtn) {
        uploadBtn.disabled = true;
        uploadBtn.textContent = 'Uploading...';
    }
    
    try {
        // Send POST request to /api/professor/files/upload with FormData
        console.log('Sending POST request to /api/professor/files/upload...');
        const response = await fetch('/api/professor/files/upload', {
            method: 'POST',
            body: formData
            // Note: Don't set Content-Type header - browser will set it automatically with boundary
        });
        
        console.log('Response received:', {
            status: response.status,
            statusText: response.statusText,
            ok: response.ok
        });
        
        // Parse JSON response
        console.log('Parsing JSON response...');
        const result = await response.json();
        console.log('Response data:', result);
        
        // Task 20: Handle response
        // Check if response is ok and result.success is true
        if (response.ok && result.success) {
            console.log('✓ Upload successful!');
            // On success:
            // Show success toast with file count
            const fileCount = files.length;
            const fileWord = fileCount === 1 ? 'file' : 'files';
            console.log(`Showing success toast for ${fileCount} ${fileWord}`);
            showToast(`Successfully uploaded ${fileCount} ${fileWord}!`, 'success');
            
            // Close modal
            console.log('Closing upload modal...');
            closeUploadModal();
            
            // Call refreshCurrentFolderFiles() to update file list
            console.log('Refreshing current folder files...');
            await refreshCurrentFolderFiles();
            console.log('✓ Upload process complete');
        } else {
            // On error:
            console.error('Upload failed:', result);
            // Show error message in modal error div
            const errorMsg = result.message || 'Upload failed. Please try again.';
            showError(errorMsg);
            // Keep modal open (don't call closeUploadModal)
        }
        
    } catch (error) {
        // On network error (catch block):
        console.error('Upload error (exception):', error);
        console.error('Error stack:', error.stack);
        // Show network error message
        showError('Network error occurred. Please check your connection and try again.');
        // Keep modal open (don't call closeUploadModal)
    } finally {
        // Always reset button state in finally block
        console.log('Resetting button state...');
        if (uploadBtn) {
            uploadBtn.disabled = false;
            uploadBtn.textContent = 'Upload';
        }
        console.log('=== UPLOAD FUNCTION COMPLETE ===');
    }
};

// Confirm handleUpload is registered
console.log('✓ window.handleUpload registered:', typeof window.handleUpload);

/**
 * Refresh current folder files
 * Task 21: Refreshes the file list for the current folder after upload
 */
async function refreshCurrentFolderFiles() {
    console.log('=== REFRESH CURRENT FOLDER FILES ===');
    
    // Get current node from FileExplorerState
    const currentNode = fileExplorerState.getCurrentNode();
    console.log('Current node:', currentNode);
    
    // Return early if no current node
    if (!currentNode) {
        console.warn('No current node, skipping refresh');
        return;
    }
    
    try {
        // Set file list loading state to true (if needed)
        // This could be handled by the FileExplorer component
        
        // Fetch updated node data from /api/file-explorer/node?path={path}
        const path = currentNode.path || '';
        console.log('Fetching updated node data for path:', path);
        const response = await fetch(`/api/file-explorer/node?path=${encodeURIComponent(path)}`);
        
        console.log('Refresh response:', {
            status: response.status,
            statusText: response.statusText,
            ok: response.ok
        });
        
        if (!response.ok) {
            throw new Error('Failed to fetch updated node data');
        }
        
        // Parse JSON response
        console.log('Parsing updated node data...');
        const updatedNodeData = await response.json();
        console.log('Updated node data:', updatedNodeData);
        
        // Update FileExplorerState with new current node data
        console.log('Updating FileExplorerState...');
        fileExplorerState.setCurrentNode(updatedNodeData);
        
        // Reload the file explorer to show updated files
        if (fileExplorerInstance) {
            console.log('Reloading file explorer...');
            await fileExplorerInstance.loadNode(path);
            console.log('✓ File explorer reloaded');
        } else {
            console.warn('No fileExplorerInstance found');
        }
        
        // Set file list loading state to false (if needed)
        console.log('✓ Refresh complete');
        
    } catch (error) {
        // Handle errors gracefully (log but don't show to user)
        console.error('Error refreshing folder files:', error);
        console.error('Error stack:', error.stack);
        // Don't show error toast to user as this is a background refresh
    }
}

// Load notifications
async function loadNotifications() {
    try {
        notifications = (await professor.getNotifications()).sort((a, b) =>
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
                <div class="mb-2">
                    <div class="flex justify-between items-center mb-1">
                        <span class="text-sm font-medium text-gray-700">Uploading files...</span>
                        <span id="progressPercentage" class="text-sm font-semibold text-blue-600">0%</span>
                    </div>
                    <div class="progress-bar">
                        <div id="progressFill" class="progress-bar-fill" style="width: 0%"></div>
                    </div>
                </div>
                <p id="progressText" class="text-xs text-gray-500 text-center">Please wait while your files are being uploaded...</p>
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
        fileError.textContent = `You can only upload up to ${maxFileCount} file${maxFileCount > 1 ? 's' : ''}. Please select fewer files and try again.`;
        fileError.classList.remove('hidden');
        return;
    }

    // Validate file types and calculate total size
    let totalSize = 0;
    const validFiles = [];
    const invalidFiles = [];

    for (let file of files) {
        const ext = file.name.split('.').pop().toLowerCase();
        if (ext !== 'pdf' && ext !== 'zip') {
            invalidFiles.push(file.name);
        } else {
            totalSize += file.size;
            validFiles.push(file);
        }
    }

    if (invalidFiles.length > 0) {
        fileError.textContent = `Invalid file type${invalidFiles.length > 1 ? 's' : ''}: ${invalidFiles.join(', ')}. Only PDF and ZIP files are accepted. Please remove the invalid file${invalidFiles.length > 1 ? 's' : ''} and try again.`;
        fileError.classList.remove('hidden');
        return;
    }

    // Validate total size
    const totalSizeMb = totalSize / (1024 * 1024);
    if (totalSizeMb > maxTotalSizeMb) {
        fileError.textContent = `Total file size (${totalSizeMb.toFixed(2)} MB) exceeds the maximum allowed size of ${maxTotalSizeMb} MB. Please reduce the file size or select fewer files.`;
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
    const progressPercentage = document.getElementById('progressPercentage');

    // Disable upload button and cancel button
    const uploadBtn = document.querySelector('[data-action="upload"]');
    const cancelBtn = document.querySelector('[data-action="cancel"]');
    if (uploadBtn) {
        uploadBtn.disabled = true;
        uploadBtn.classList.add('opacity-50', 'cursor-not-allowed');
        uploadBtn.innerHTML = `
            <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white inline" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Uploading...
        `;
    }
    if (cancelBtn) {
        cancelBtn.disabled = true;
        cancelBtn.classList.add('opacity-50', 'cursor-not-allowed');
    }

    try {
        if (isReplacement && submissionId) {
            await professor.replaceFiles(
                submissionId,
                formData,
                (percent) => {
                    const roundedPercent = Math.round(percent);
                    progressFill.style.width = `${percent}%`;
                    progressPercentage.textContent = `${roundedPercent}%`;
                    if (roundedPercent < 100) {
                        progressText.textContent = `Please wait while your files are being uploaded... (${roundedPercent}%)`;
                    } else {
                        progressText.textContent = 'Processing upload... Almost done!';
                    }
                }
            );
            showToast('Files replaced successfully! Your submission has been updated.', 'success');
        } else {
            await professor.uploadFiles(
                courseAssignmentId,
                documentType,
                formData,
                (percent) => {
                    const roundedPercent = Math.round(percent);
                    progressFill.style.width = `${percent}%`;
                    progressPercentage.textContent = `${roundedPercent}%`;
                    if (roundedPercent < 100) {
                        progressText.textContent = `Please wait while your files are being uploaded... (${roundedPercent}%)`;
                    } else {
                        progressText.textContent = 'Processing upload... Almost done!';
                    }
                }
            );
            showToast('Files uploaded successfully! Your submission has been recorded.', 'success');
        }

        // Reload courses
        if (selectedSemesterId) {
            await loadCourses(selectedSemesterId);
        }
        closeModal();
    } catch (error) {
        console.error('Upload error:', error);
        const errorMessage = error.message || 'Upload failed due to an unexpected error';
        fileError.textContent = `${errorMessage}. Please check your files and try again. If the problem persists, contact support.`;
        fileError.classList.remove('hidden');
        showToast(`Upload failed: ${errorMessage}`, 'error');

        if (uploadBtn) {
            uploadBtn.disabled = false;
            uploadBtn.classList.remove('opacity-50', 'cursor-not-allowed');
            uploadBtn.textContent = isReplacement ? 'Replace' : 'Upload';
        }
        if (cancelBtn) {
            cancelBtn.disabled = false;
            cancelBtn.classList.remove('opacity-50', 'cursor-not-allowed');
        }
        uploadProgress.classList.add('hidden');
        progressFill.style.width = '0%';
        if (progressPercentage) progressPercentage.textContent = '0%';
    }
}

// View submission files
window.viewSubmissionFiles = async (submissionId) => {
    try {
        const submission = await professor.getSubmission(submissionId);
        const files = submission.uploadedFiles || [];

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

// Helper to extract filename from Content-Disposition header
function extractFilenameFromContentDisposition(contentDisposition, fallbackName = 'download') {
    let filename = fallbackName;

    if (!contentDisposition) {
        return filename;
    }

    // Pattern 1: filename="example.pdf"
    let match = /filename="([^"]+)"/i.exec(contentDisposition);

    // Pattern 2: filename=example.pdf (no quotes)
    if (!match) {
        match = /filename=([^;]+)/i.exec(contentDisposition);
    }

    // Pattern 3: filename*=UTF-8''example.pdf (RFC 5987)
    if (!match) {
        match = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
    }

    if (match && match[1]) {
        filename = match[1].trim().replace(/['"]/g, '');
        try {
            filename = decodeURIComponent(filename);
        } catch (e) {
            console.warn('Failed to decode filename from header:', e);
        }
    }

    return filename;
}

// Download submission file
window.downloadSubmissionFile = async (fileId, fallbackName) => {
    try {
        showToast('Downloading file...', 'info');
        const response = await professor.downloadSubmissionFile(fileId);

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Failed to download file');
        }

        const contentDisposition = response.headers.get('Content-Disposition');
        const filename = extractFilenameFromContentDisposition(contentDisposition, fallbackName || 'download');

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        showToast(`File "${filename}" downloaded successfully`, 'success');
    } catch (error) {
        console.error('Download error:', error);
        const errorMessage = error.message || 'An unexpected error occurred';
        showToast(`Failed to download file: ${errorMessage}. Please try again or contact support.`, 'error');
    }
};

function getFileIcon(mimeType) {
    if (!mimeType) return '📎';
    const type = mimeType.toLowerCase();
    if (type.includes('pdf')) return '📄';
    if (type.includes('word') || type.includes('document') || type.includes('doc')) return '📝';
    if (type.includes('excel') || type.includes('spreadsheet') || type.includes('xls')) return '📊';
    if (type.includes('powerpoint') || type.includes('presentation') || type.includes('ppt')) return '📽️';
    if (type.includes('image') || type.includes('png') || type.includes('jpg') || type.includes('jpeg') || type.includes('gif')) return '🖼️';
    if (type.includes('video') || type.includes('mp4') || type.includes('avi') || type.includes('mov')) return '🎥';
    if (type.includes('audio') || type.includes('mp3') || type.includes('wav')) return '🎵';
    if (type.includes('zip') || type.includes('compressed') || type.includes('rar') || type.includes('7z')) return '📦';
    if (type.includes('text') || type.includes('txt')) return '📃';
    return '📎';
}

function getFileIconClass(mimeType) {
    if (!mimeType) return 'file-icon-default';
    const type = mimeType.toLowerCase();
    if (type.includes('pdf')) return 'file-icon-pdf';
    if (type.includes('zip') || type.includes('compressed') || type.includes('rar') || type.includes('7z')) return 'file-icon-zip';
    if (type.includes('word') || type.includes('document') || type.includes('doc')) return 'file-icon-doc';
    if (type.includes('image') || type.includes('png') || type.includes('jpg') || type.includes('jpeg') || type.includes('gif')) return 'file-icon-image';
    return 'file-icon-default';
}


// Tab switching
let currentTab = 'courses';
let currentPath = '';
let fileExplorerData = null;

/**
 * Initialize File Explorer with Professor role configuration
 * 
 * TASK 5 IMPLEMENTATION: Enhanced FileExplorer Configuration
 * 
 * This function creates a FileExplorer instance with explicit role-specific configuration
 * for the Professor Dashboard. The configuration enables:
 * - role: 'PROFESSOR' - Identifies this as a professor user
 * - showOwnershipLabels: true - Displays "Your Folder" labels on writable folders
 * - readOnly: false - Allows file upload and modification operations
 * 
 * The FileExplorer class handles all rendering, navigation, and role-specific UI elements
 * based on these configuration options.
 */
function initializeFileExplorer() {
    try {
        // Create FileExplorer instance with explicit Professor role configuration
        fileExplorerInstance = new FileExplorer('fileExplorerContainer', {
            role: 'PROFESSOR',
            showOwnershipLabels: true,
            readOnly: false,
            onFileClick: (file) => {
                // Handle file click if needed
                console.log('File clicked:', file);
            },
            onNodeExpand: (node) => {
                // Handle node expansion if needed
                console.log('Node expanded:', node);
            }
        });

        // Make instance available globally for event handlers
        window.fileExplorerInstance = fileExplorerInstance;

        console.log('FileExplorer initialized with Professor configuration:', {
            role: 'PROFESSOR',
            showOwnershipLabels: true,
            readOnly: false
        });
    } catch (error) {
        console.error('Error initializing FileExplorer:', error);
        showToast('Failed to initialize file explorer. Please refresh the page.', 'error');
    }
}

/**
 * Load file explorer using the FileExplorer class
 * 
 * This function uses the enhanced FileExplorer component with Professor role configuration.
 * The FileExplorer class handles all rendering, navigation, and role-specific UI elements.
 * 
 * Configuration applied:
 * - role: 'PROFESSOR' - Enables professor-specific features
 * - showOwnershipLabels: true - Shows "Your Folder" labels on writable folders
 * - readOnly: false - Allows file upload and modification
 */
async function loadFileExplorer(path = '') {
    try {
        if (!selectedAcademicYearId || !selectedSemesterId) {
            console.warn('Cannot load file explorer: No semester selected');
            return;
        }

        if (!fileExplorerInstance) {
            initializeFileExplorer();
        }

        if (path) {
            await fileExplorerInstance.loadNode(path);
        } else {
            await fileExplorerInstance.loadRoot(selectedAcademicYearId, selectedSemesterId);
        }
        currentPath = path;
    } catch (error) {
        console.error('Error loading file explorer:', error);
        const errorMessage = error.message || 'An unexpected error occurred';
        showToast(`Unable to load file explorer: ${errorMessage}. Please try again or contact support.`, 'error');
    }
}



// Create file explorer skeleton loader
function createFileExplorerSkeletonLoader() {
    return `
        <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200">
            <div class="flex items-center space-x-3 flex-1">
                <div class="skeleton-line skeleton-circle h-6" style="width: 1.5rem;"></div>
                <div class="flex-1">
                    <div class="skeleton-line h-4 w-1-2"></div>
                </div>
            </div>
            <div class="skeleton-line h-4" style="width: 1.25rem;"></div>
        </div>
    `;
}

/**
 * Render file explorer
 * 
 * MASTER IMPLEMENTATION: File Explorer Rendering Pattern
 * 
 * This function defines the CANONICAL VISUAL DESIGN for folder cards and file items
 * that must be replicated across all dashboards.
 * 
 * FOLDER CARD DESIGN SPECIFICATIONS:
 * 
 * Structure:
 * - Container: flex items-center justify-between p-4 rounded-lg border cursor-pointer group
 * - Inner flex: flex items-center space-x-3 flex-1
 * - Icon: w-7 h-7 folder icon (SVG)
 * - Content: flex-1 with name and optional description
 * - Arrow: w-5 h-5 with group-hover:translate-x-1 transition-all
 * 
 * Color Scheme (Professor Role):
 * - Writable folders (canWrite=true):
 *   - Background: bg-blue-50 border-blue-200
 *   - Hover: hover:bg-blue-100
 *   - Icon: text-blue-600
 *   - Label: "Your Folder" badge (bg-blue-100 text-blue-800)
 * 
 * - Read-only folders (canWrite=false):
 *   - Background: bg-gray-50 border-gray-200
 *   - Hover: hover:bg-gray-100
 *   - Icon: text-gray-600
 *   - Label: "Read Only" badge (bg-gray-100 text-gray-600)
 * 
 * Role-Specific Labels:
 * - "Your Folder": inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold
 *   - Includes edit icon (w-3 h-3 mr-1)
 *   - Applied when canWrite=true for Professor role
 * 
 * - "Read Only": inline-flex items-center px-2 py-0.5 rounded text-xs font-medium
 *   - Includes eye icon (w-3 h-3 mr-1)
 *   - Applied when canWrite=false
 * 
 * Typography:
 * - Folder name: text-sm font-semibold text-gray-900
 * - Description: text-xs text-gray-500 mt-1
 * - Section headers: text-sm font-medium text-gray-700 mb-3
 * 
 * Empty State:
 * - Centered text with py-8 padding
 * - text-gray-500 text-center
 * - Descriptive message
 */
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
        html += '<div class="mb-6"><h3 class="text-sm font-medium text-gray-700 mb-3 flex items-center"><svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg>Folders</h3><div class="space-y-2">';
        folders.forEach(folder => {
            const canWrite = folder.canWrite;
            const folderColor = canWrite ? 'text-blue-600' : 'text-gray-600';
            const bgColor = canWrite ? 'bg-blue-50 border-blue-200' : 'bg-gray-50 border-gray-200';
            const hoverColor = canWrite ? 'hover:bg-blue-100' : 'hover:bg-gray-100';
            const writeIndicator = canWrite ? '<span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-blue-100 text-blue-800 ml-2 own-folder-indicator"><svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path></svg>Your Folder</span>' : '<span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600 ml-2"><svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>Read Only</span>';

            html += `
                <div class="file-explorer-folder flex items-center justify-between p-4 ${bgColor} rounded-lg border ${hoverColor} cursor-pointer group"
                    onclick="navigateToPath('${folder.path}')">
                    <div class="flex items-center space-x-3 flex-1">
                        <svg class="folder-icon w-7 h-7 ${folderColor}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                        </svg>
                        <div class="flex-1">
                            <div class="flex items-center flex-wrap">
                                <p class="text-sm font-semibold text-gray-900">${folder.name}</p>
                                ${writeIndicator}
                            </div>
                            ${folder.metadata && folder.metadata.description ? `<p class="text-xs text-gray-500 mt-1">${folder.metadata.description}</p>` : ''}
                        </div>
                    </div>
                    <svg class="w-5 h-5 text-gray-400 group-hover:text-gray-700 group-hover:translate-x-1 transition-all" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                    </svg>
                </div>
            `;
        });
        html += '</div></div>';
    }

    /**
     * Render files section
     * 
     * MASTER IMPLEMENTATION: File Item Design Pattern
     * 
     * This section defines the CANONICAL FILE ITEM DESIGN for all dashboards.
     * 
     * File Item Structure:
     * - Container: flex items-center justify-between p-4 bg-white rounded-lg border border-gray-200
     * - Hover effect: hover:shadow-lg group
     * - Inner flex: flex items-center space-x-3 flex-1 min-w-0
     * 
     * File Icon Container:
     * - Size: w-12 h-12
     * - Background: bg-gray-50 rounded-lg
     * - Icon: text-3xl with role-specific color class
     * - Flex: flex-shrink-0 to prevent icon from shrinking
     * 
     * File Name:
     * - Typography: text-sm font-semibold text-gray-900
     * - Truncate: truncate to prevent overflow
     * - Hover: group-hover:text-blue-600 transition-colors
     * - Title attribute for full name on hover
     * 
     * Metadata Badges:
     * - Container: flex items-center flex-wrap gap-x-3 gap-y-1 mt-1.5
     * - Badge: inline-flex items-center px-2 py-0.5 rounded text-xs font-medium
     * - Colors: bg-gray-100 text-gray-700
     * - Icon: w-3 h-3 mr-1 (SVG icon for each metadata type)
     * - Metadata types: File size, Upload date, Uploader name
     * 
     * Download Button:
     * - Container: flex items-center space-x-2 flex-shrink-0 ml-4
     * - Button: text-white bg-blue-600 hover:bg-blue-700 p-2.5 rounded-lg
     * - Shadow: shadow-sm hover:shadow-md transition-all
     * - Icon: w-5 h-5 download arrow icon
     * - Disabled state: text-gray-400 p-2.5 bg-gray-100 rounded-lg with lock icon
     * 
     * This design ensures consistent file display across all dashboards.
     */
    // Render files
    if (files.length > 0) {
        html += '<div><h3 class="text-sm font-medium text-gray-700 mb-3 flex items-center"><svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path></svg>Files</h3><div class="space-y-2">';
        files.forEach(file => {
            const metadata = file.metadata || {};
            const fileSize = metadata.fileSize ? formatFileSize(metadata.fileSize) : 'Unknown size';
            const uploadDate = metadata.uploadedAt ? formatDate(metadata.uploadedAt) : 'Unknown date';
            const uploaderName = metadata.uploaderName || 'Unknown';
            const fileType = metadata.fileType || '';
            const fileIcon = getFileIcon(fileType);
            const fileIconClass = getFileIconClass(fileType);

            html += `
                <div class="file-explorer-item flex items-center justify-between p-4 bg-white rounded-lg border border-gray-200 hover:shadow-lg group">
                    <div class="flex items-center space-x-3 flex-1 min-w-0">
                        <div class="file-icon-container flex-shrink-0 w-12 h-12 flex items-center justify-center bg-gray-50 rounded-lg">
                            <span class="text-3xl ${fileIconClass}">${fileIcon}</span>
                        </div>
                        <div class="flex-1 min-w-0">
                            <p class="text-sm font-semibold text-gray-900 truncate group-hover:text-blue-600 transition-colors" title="${file.name}">${file.name}</p>
                            <div class="flex items-center flex-wrap gap-x-3 gap-y-1 mt-1.5">
                                <span class="file-metadata-badge inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-700">
                                    <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4"></path>
                                    </svg>
                                    ${fileSize}
                                </span>
                                <span class="file-metadata-badge inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-700">
                                    <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                                    </svg>
                                    ${uploadDate}
                                </span>
                                <span class="file-metadata-badge inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-700">
                                    <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
                                    </svg>
                                    ${uploaderName}
                                </span>
                            </div>
                        </div>
                    </div>
                    <div class="flex items-center space-x-2 flex-shrink-0 ml-4">
                        ${file.canRead ? `
                            <button 
                                onclick="downloadFileFromExplorer(${file.entityId}, '${file.name}')"
                                class="download-button text-white bg-blue-600 hover:bg-blue-700 p-2.5 rounded-lg shadow-sm hover:shadow-md transition-all"
                                title="Download ${file.name}"
                            >
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                                </svg>
                            </button>
                        ` : `
                            <span class="text-gray-400 p-2.5 bg-gray-100 rounded-lg" title="No download permission">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                                </svg>
                            </span>
                        `}
                    </div>
                </div>
            `;
        });
        html += '</div></div>';
    }

    fileExplorerContainer.innerHTML = html;
}

// Navigate to path - used by FileExplorer class
window.navigateToPath = (path) => {
    loadFileExplorer(path);
};

// Note: renderFileExplorer() and renderBreadcrumbs() functions have been removed
// as they are now handled by the FileExplorer class instance.
// The FileExplorer class provides all rendering, navigation, and role-specific
// label functionality based on the configuration set in initializeFileExplorer().

// Download file from explorer (Professor dashboard)
window.downloadFileFromExplorer = async (fileId, fallbackName) => {
    try {
        showToast('Downloading file...', 'info');
        // For professor file explorer, files are served by /api/file-explorer/... endpoint
        const response = await fileExplorer.downloadFile(fileId);

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Failed to download file');
        }

        const contentDisposition = response.headers.get('Content-Disposition');
        const filename = extractFilenameFromContentDisposition(contentDisposition, fallbackName || 'download');

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        showToast(`File "${filename}" downloaded successfully`, 'success');
    } catch (error) {
        console.error('Download error:', error);
        const errorMessage = error.message || 'An unexpected error occurred';
        showToast(`Failed to download file: ${errorMessage}. Please check your permissions and try again.`, 'error');
    }
}

/**
 * Listen for file explorer upload events
 * This event is dispatched by file-explorer.js when user clicks upload button
  * or drops files on a writable folder
 */
window.addEventListener('fileExplorerUpload', (e) => {
    const { path, documentType, files } = e.detail;
    showFileExplorerUploadModal(path, documentType, files);
});

/**
 * Show upload modal for file explorer
 * Displays a modal where users can select files to upload to a specific path
 * 
 * @param {string} path - The folder path to upload to
 * @param {string} documentType - The document type
 * @param {FileList} preselectedFiles - Optional files from drag-drop
 */
function showFileExplorerUploadModal(path, documentType, preselectedFiles = null) {
    const formattedType = formatDocumentTypeName(documentType);
    
    // Store selected files in closure scope
    let selectedFiles = null;

    const modalHtml = `
        <div>
            <div class="mb-4">
                <label class="block text-sm font-medium text-gray-700 mb-2">
                    Upload ${formattedType}
                    <span class="text-gray-500 font-normal">(PDF or ZIP, max 10 files)</span>
                </label>
                <input type="file" id="uploadFilesInput" multiple accept=".pdf,.zip"
                       class="block w-full text-sm text-gray-900 border border-gray-300 rounded-lg cursor-pointer bg-gray-50 focus:outline-none hover:bg-gray-100 file:mr-4 file:py-2 file:px-4 file:rounded-l-lg file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100">
                <p class="mt-1 text-xs text-gray-500">Maximum file size: 10MB per file</p>
            </div>
            
            <div class="mb-4">
                <label class="block text-sm font-medium text-gray-700 mb-2">
                    Notes (Optional)
                </label>
                <textarea id="uploadNotes" rows="3" 
                          placeholder="Add any notes about these files..."
                          class="block w-full text-sm text-gray-900 border border-gray-300 rounded-lg p-2.5 focus:ring-blue-500 focus:border-blue-500"></textarea>
            </div>
            
            <div id="uploadProgress" class="hidden">
                <div class="mb-2">
                    <div class="flex justify-between text-xs text-gray-600 mb-1">
                        <span id="uploadProgressText">Uploading...</span>
                        <span id="uploadProgressPercent">0%</span>
                    </div>
                    <div class="w-full bg-gray-200 rounded-full h-2.5">
                        <div id="uploadProgressBar" class="bg-blue-600 h-2.5 rounded-full transition-all duration-300" style="width: 0%"></div>
                    </div>
                </div>
            </div>
            
            <div id="fileList" class="mt-4"></div>
        </div>
    `;

    showModal(`Upload ${formattedType}`, modalHtml, {
        size: 'md',
        buttons: [
            {
                text: 'Upload',
                className: 'bg-blue-600 text-white hover:bg-blue-700',
                action: 'upload',
                onClick: async (close) => {
                    console.log('Upload button clicked');
                    
                    // Use the preserved files from closure
                    const files = selectedFiles;
                    
                    console.log('selectedFiles from closure:', selectedFiles);
                    console.log('Files array:', files);
                    
                    const notesInput = document.getElementById('uploadNotes');
                    const notes = notesInput ? notesInput.value.trim() : '';

                    if (!files || files.length === 0) {
                        showToast('Please select at least one file', 'error');
                        return;
                    }
                    
                    console.log('Files to upload:', files.map(f => f.name));

                    // Validate file count
                    if (files.length > 10) {
                        showToast('Maximum 10 files allowed per upload', 'error');
                        return;
                    }

                    // Validate file sizes
                    const maxSize = 10 * 1024 * 1024; // 10MB
                    for (const file of files) {
                        if (file.size > maxSize) {
                            showToast(`File "${file.name}" exceeds 10MB limit`, 'error');
                            return;
                        }
                    }

                    // Show progress
                    const progressDiv = document.getElementById('uploadProgress');
                    if (progressDiv) progressDiv.classList.remove('hidden');

                    const formData = new FormData();
                    for (const file of files) {
                        formData.append('files', file);
                    }
                    if (notes) {
                        formData.append('notes', notes);
                    }

                    try {
                        // Use the fileExplorer API method
                        console.log('Sending upload request to backend for path:', path);
                        await fileExplorer.uploadFiles(path, formData, (progress) => {
                            const progressBar = document.getElementById('uploadProgressBar');
                            const progressPercent = document.getElementById('uploadProgressPercent');
                            const progressText = document.getElementById('uploadProgressText');

                            if (progressBar) progressBar.style.width = progress + '%';
                            if (progressPercent) progressPercent.textContent = Math.round(progress) + '%';
                            if (progressText) {
                                if (progress < 100) {
                                    progressText.textContent = 'Uploading...';
                                } else {
                                    progressText.textContent = 'Processing...';
                                }
                            }
                        });

                        showToast(`${files.length} file(s) uploaded successfully`, 'success');
                        close();

                        // Refresh file explorer to show new files
                        if (window.fileExplorerInstance) {
                            await window.fileExplorerInstance.loadNode(path);
                        }

                        // Also refresh dashboard if on My Courses tab
                        if (typeof refreshDashboard === 'function') {
                            refreshDashboard();
                        }
                    } catch (error) {
                        console.error('Upload error:', error);
                        showToast(error.message || 'Upload failed. Please try again.', 'error');
                        if (progressDiv) progressDiv.classList.add('hidden');
                    }
                }
            },
            {
                text: 'Cancel',
                className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                action: 'cancel',
                onClick: (close) => close()
            }
        ]
    });

    // IMMEDIATELY attach handler after modal creation
    // Use requestAnimationFrame to ensure DOM is painted
    requestAnimationFrame(() => {
        const filesInput = document.getElementById('uploadFilesInput');
        if (!filesInput) {
            console.error('Could not find uploadFilesInput element after modal creation');
            return;
        }
        
        console.log('File input found, attaching change listener and polling');

        // Attach change listener to capture files immediately
        filesInput.addEventListener('change', (e) => {
            console.log('CHANGE EVENT FIRED!');
            selectedFiles = e.target.files;
            console.log('Files selected via change event:', selectedFiles ? selectedFiles.length : 0);
            
            // CRITICAL: Store files in an array to preserve them
            if (selectedFiles && selectedFiles.length > 0) {
                const fileArray = Array.from(selectedFiles);
                selectedFiles = fileArray;
                console.log('Files preserved in array:', fileArray.map(f => f.name));
                updateFileList(selectedFiles);
            }
        });

        // FALLBACK: Poll the input every 200ms to detect file selection
        // This works even if the change event doesn't fire
        let lastFileCount = 0;
        let pollCount = 0;
        const pollInterval = setInterval(() => {
            const input = document.getElementById('uploadFilesInput');
            if (!input || !document.body.contains(input)) {
                console.log('Poll stopped - input removed from DOM');
                clearInterval(pollInterval);
                return;
            }
            
            pollCount++;
            if (pollCount % 10 === 0) {
                console.log(`Polling... count=${pollCount}, input.files.length=${input.files ? input.files.length : 'null'}, input.value="${input.value}"`);
            }
            
            const currentFileCount = input.files ? input.files.length : 0;
            if (currentFileCount > 0 && currentFileCount !== lastFileCount) {
                console.log('Files detected via polling:', currentFileCount);
                selectedFiles = Array.from(input.files);
                console.log('Files:', selectedFiles.map(f => f.name));
                updateFileList(selectedFiles);
                lastFileCount = currentFileCount;
            }
        }, 200);

        console.log('Change listener and polling attached successfully');

        // Pre-fill files if dropped
        if (preselectedFiles && preselectedFiles.length > 0) {
            const dt = new DataTransfer();
            for (let i = 0; i < preselectedFiles.length; i++) {
                dt.items.add(preselectedFiles[i]);
            }
            filesInput.files = dt.files;
            selectedFiles = Array.from(filesInput.files);
            updateFileList(selectedFiles);
        }
    });
}

/**
 * Update file list display in upload modal
 * Shows list of selected files with names and sizes
 * 
 * @param {FileList} files - The selected files
 */
function updateFileList(files) {
    const fileListContainer = document.getElementById('fileList');
    if (!fileListContainer || files.length === 0) return;

    let html = '<div class="border border-gray-200 rounded-lg p-3 max-h-48 overflow-y-auto">';
    html += '<p class="text-xs font-semibold text-gray-700 mb-2">Selected Files:</p>';
    html += '<ul class="space-y-1">';

    for (const file of files) {
        const sizeKB = (file.size / 1024).toFixed(1);
        html += `
            <li class="flex items-center justify-between text-xs">
                <span class="text-gray-700 truncate flex-1">${escapeHtml(file.name)}</span>
                <span class="text-gray-500 ml-2">${sizeKB} KB</span>
            </li>
        `;
    }

    html += '</ul></div>';
    fileListContainer.innerHTML = html;
}

/**
 * Format document type name for display
 * Converts enum values to human-readable names
 * 
 * @param {string} documentType - The document type enum value
 * @returns {string} Formatted document type name
 */
function formatDocumentTypeName(documentType) {
    if (!documentType) return 'Files';

    const typeMap = {
        'SYLLABUS': 'Syllabus',
        'EXAM': 'Exams',
        'ASSIGNMENT': 'Assignments',
        'PROJECT_DOCS': 'Project Documents',
        'LECTURE_NOTES': 'Lecture Notes',
        'OTHER': 'Other Documents',
        'syllabus': 'Syllabus',
        'exam': 'Exams',
        'assignment': 'Assignments',
        'project_docs': 'Project Documents',
        'lecture_notes': 'Lecture Notes',
        'other': 'Other Documents'
    };

    return typeMap[documentType] || documentType;
}

/**
 * Escape HTML to prevent XSS
 * @param {string} text - Text to escape
 * @returns {string} Escaped text
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}


// ==================== DEBUG: Verify Window Functions ====================
console.log('=== PROF.JS: Verifying Window Functions ===');
console.log('window.openFileExplorerUploadModal:', typeof window.openFileExplorerUploadModal);
console.log('window.openUploadModal:', typeof window.openUploadModal);
console.log('window.closeUploadModal:', typeof window.closeUploadModal);
console.log('window.handleUpload:', typeof window.handleUpload);
console.log('window.switchTab:', typeof window.switchTab);
console.log('window.markNotificationSeen:', typeof window.markNotificationSeen);
console.log('window.removeFileFromSelection:', typeof window.removeFileFromSelection);
console.log('window.downloadSubmissionFile:', typeof window.downloadSubmissionFile);
console.log('window.viewSubmissionFiles:', typeof window.viewSubmissionFiles);
console.log('=== END Window Functions Verification ===');
