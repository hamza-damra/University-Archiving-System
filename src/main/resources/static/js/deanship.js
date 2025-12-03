/**
 * Deanship Dashboard
 * Manages academic years, professors, courses, assignments, reports, and file explorer
 */

import { apiRequest, getUserInfo, redirectToLogin, clearAuthData, getErrorMessage, initializeAuth } from './api.js';
import { showToast, showModal, showConfirm, formatDate } from './ui.js';
import { FileExplorer } from './file-explorer.js';
import { fileExplorerState } from './file-explorer-state.js';
import { SkeletonLoader, EmptyState, EnhancedToast, Tooltip, LoadingIndicator, withMinLoadingTime, MIN_LOADING_TIME } from './deanship-feedback.js';
import { dashboardNavigation } from './deanship-navigation.js';
import { dashboardAnalytics } from './deanship-analytics.js';
import { dashboardState } from './deanship-state.js';
import { ErrorBoundary, safeAsync } from './deanship-error-handler.js';

// Expose enhanced toast globally for backward compatibility
window.EnhancedToast = EnhancedToast;
window.Tooltip = Tooltip;
window.LoadingIndicator = LoadingIndicator;
window.EmptyState = EmptyState;
window.SkeletonLoader = SkeletonLoader;
window.withMinLoadingTime = withMinLoadingTime;
window.MIN_LOADING_TIME = MIN_LOADING_TIME;

// Expose UI functions globally for non-module scripts
window.showToast = showToast;
window.showModal = showModal;
window.showConfirm = showConfirm;

// State
let currentTab = 'dashboard'; // Default to dashboard
let selectedAcademicYear = null;
let selectedAcademicYearId = null;
let selectedSemesterId = null;
let semesters = [];
let academicYears = [];
let professors = [];
let courses = [];
let departments = [];
let fileExplorerInstance = null;
let reportsDashboardInstance = null;

/**
 * Get current date/time in Palestine timezone (Asia/Jerusalem)
 * @returns {Date} Current date in Palestine timezone
 */
function getPalestineDate() {
    const palestineTimeString = new Date().toLocaleString('en-US', { 
        timeZone: 'Asia/Jerusalem' 
    });
    return new Date(palestineTimeString);
}

/**
 * Calculate the current academic year code based on Palestine timezone
 * Academic year starts in September and ends in August
 * @returns {string} Academic year code (e.g., "2024-2025")
 */
function getCurrentAcademicYearCode() {
    const palestineDate = getPalestineDate();
    const month = palestineDate.getMonth();
    const year = palestineDate.getFullYear();
    
    if (month >= 8) { // September or later
        return `${year}-${year + 1}`;
    } else {
        return `${year - 1}-${year}`;
    }
}

/**
 * Determine the current semester type based on Palestine timezone
 * @returns {string} Semester type: "FIRST", "SECOND", or "SUMMER"
 */
function getCurrentSemesterType() {
    const palestineDate = getPalestineDate();
    const month = palestineDate.getMonth();
    
    if (month >= 8 || month === 0) { // Sep-Jan = FIRST
        return 'FIRST';
    } else if (month >= 1 && month <= 5) { // Feb-Jun = SECOND
        return 'SECOND';
    } else { // Jul-Aug = SUMMER
        return 'SUMMER';
    }
}

/**
 * Initialize modern dropdowns
 * Transforms native select elements into modern styled dropdowns
 */
function initializeModernDropdowns() {
    if (typeof window.initModernDropdowns === 'function') {
        window.initModernDropdowns();
    }
}

/**
 * Refresh all modern dropdowns
 * Call this after dynamically updating select options
 */
function refreshDropdowns() {
    if (typeof window.refreshModernDropdown === 'function') {
        const dropdownIds = [
            'academicYearSelect',
            'semesterSelect',
            'professorDepartmentFilter',
            'courseDepartmentFilter',
            'assignmentProfessorFilter',
            'assignmentCourseFilter'
        ];
        
        dropdownIds.forEach(id => {
            const select = document.getElementById(id);
            if (select) window.refreshModernDropdown(select);
        });
    }
}

// Initialize
document.addEventListener('DOMContentLoaded', async () => {
    console.log('=== Deanship Dashboard Initializing ===');
    console.log('Current tab on load:', currentTab);
    
    // Validate authentication before proceeding
    const isValid = await checkAuth();
    if (!isValid) return; // checkAuth handles redirect
    
    // Restore tab BEFORE any other initialization to prevent flicker
    restoreActiveTab();
    console.log('Current tab after restore:', currentTab);
    initializeEventListeners();
    initializeNavigation();
    initializeFileExplorer();
    initializeModernDropdowns(); // Initialize modern dropdowns
    // Load initial data WITHOUT triggering tab-specific loads yet
    loadInitialDataSilent();
    console.log('=== Initialization Complete ===');
});

/**
 * Check authentication and role
 * Verifies user is authenticated with valid token and has DEANSHIP role
 * Redirects to login if not authenticated or unauthorized
 * @returns {Promise<boolean>} True if authenticated, false otherwise
 */
async function checkAuth() {
    const userInfo = getUserInfo();
    if (!userInfo) {
        redirectToLogin();
        return false;
    }

    // Validate token with server (will auto-refresh if expired)
    const isAuthenticated = await initializeAuth();
    if (!isAuthenticated) {
        redirectToLogin('session_expired');
        return false;
    }

    if (userInfo.role !== 'ROLE_DEANSHIP') {
        showToast('Access denied - Deanship role required', 'error');
        setTimeout(() => redirectToLogin('access_denied'), 2000);
        return false;
    }

    // Display user name
    const userName = userInfo.fullName || userInfo.email;
    document.getElementById('deanshipName').textContent = userName;
    localStorage.setItem('deanship_user_name', userName);
    return true;
}

/**
 * Initialize event listeners
 * Sets up all DOM event listeners for the dashboard
 * @returns {void}
 */
function initializeEventListeners() {
    // Logout
    document.getElementById('logoutBtn').addEventListener('click', handleLogout);

    // Tab navigation
    document.querySelectorAll('.nav-tab').forEach(tab => {
        tab.addEventListener('click', () => switchTab(tab.dataset.tab));
    });

    // Academic year selector
    document.getElementById('academicYearSelect').addEventListener('change', async (e) => {
        selectedAcademicYearId = e.target.value ? parseInt(e.target.value) : null;
        localStorage.setItem('deanship_selected_academic_year', selectedAcademicYearId || '');
        selectedSemesterId = null;

        if (selectedAcademicYearId) {
            selectedAcademicYear = academicYears.find(y => y.id === selectedAcademicYearId);
            await loadSemesters(selectedAcademicYearId);
            onContextChange(); // Called AFTER loadSemesters completes
        } else {
            selectedAcademicYear = null;
            document.getElementById('semesterSelect').innerHTML = '<option value="">Select academic year first</option>';
            onContextChange(); // Also called for the empty state
        }
    });

    // Semester selector
    document.getElementById('semesterSelect').addEventListener('change', (e) => {
        selectedSemesterId = e.target.value ? parseInt(e.target.value) : null;
        localStorage.setItem('deanship_selected_semester', selectedSemesterId || '');
        onContextChange();
    });

    // Academic Years tab
    document.getElementById('addAcademicYearBtn').addEventListener('click', showAddAcademicYearModal);

    // Professors tab
    const addProfessorBtn = document.getElementById('addProfessorBtn');
    const professorSearch = document.getElementById('professorSearch');
    const professorDepartmentFilter = document.getElementById('professorDepartmentFilter');

    if (addProfessorBtn) addProfessorBtn.addEventListener('click', showAddProfessorModal);
    if (professorSearch) professorSearch.addEventListener('input', filterProfessors);
    if (professorDepartmentFilter) professorDepartmentFilter.addEventListener('change', filterProfessors);

    // Courses tab
    const addCourseBtn = document.getElementById('addCourseBtn');
    const courseSearch = document.getElementById('courseSearch');
    const courseDepartmentFilter = document.getElementById('courseDepartmentFilter');

    console.log('Setting up courses tab listeners, courseDepartmentFilter:', courseDepartmentFilter);

    if (addCourseBtn) addCourseBtn.addEventListener('click', showAddCourseModal);
    if (courseSearch) {
        let searchDebounceTimer;
        courseSearch.addEventListener('input', () => {
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(filterCoursesBySearch, 300);
        });
    }
    if (courseDepartmentFilter) {
        console.log('Adding change listener to courseDepartmentFilter');
        courseDepartmentFilter.addEventListener('change', (e) => {
            console.log('courseDepartmentFilter change event fired, value:', e.target.value);
            filterCoursesByDepartment();
        });
    }

    // Assignments tab
    const addAssignmentBtn = document.getElementById('addAssignmentBtn');
    const assignmentProfessorFilter = document.getElementById('assignmentProfessorFilter');
    const assignmentCourseFilter = document.getElementById('assignmentCourseFilter');

    if (addAssignmentBtn) addAssignmentBtn.addEventListener('click', showAddAssignmentModal);
    if (assignmentProfessorFilter) assignmentProfessorFilter.addEventListener('change', loadAssignments);
    if (assignmentCourseFilter) assignmentCourseFilter.addEventListener('change', loadAssignments);

    // Reports tab - now handled by ReportsDashboard component
}

/**
 * Initialize navigation components
 * Sets up sidebar, breadcrumbs, and navigation state
 * @returns {void}
 */
function initializeNavigation() {
    dashboardNavigation.initialize();
}

/**
 * Load initial data silently (without triggering tab-specific UI updates)
 * Loads academic years and departments, then loads active tab data
 * @returns {Promise<void>}
 */
async function loadInitialDataSilent() {
    try {
        console.log('Loading initial data silently...');
        await Promise.all([
            loadAcademicYearsData(),
            loadDepartments()
        ]);
        console.log('Initial data loaded (Academic Years & Departments).');
        
        // Load data for the current tab after initial data is ready
        // This ensures the tab content is populated on page refresh
        console.log('Loading data for current tab:', currentTab);
        loadTabData(currentTab);
    } catch (error) {
        console.error('Error loading initial data:', error);
        showToast('Failed to load initial data', 'error');
    }
}

/**
 * Load initial data (legacy method for backward compatibility)
 */
async function loadInitialData() {
    await loadInitialDataSilent();
}

/**
 * Restore active tab from localStorage
 * This runs FIRST to prevent any flicker or tab switching on page load
 */
function restoreActiveTab() {
    const savedTab = localStorage.getItem('deanship-active-tab');
    if (savedTab) {
        currentTab = savedTab;
    } else {
        // Default to dashboard if no saved tab
        currentTab = 'dashboard';
        localStorage.setItem('deanship-active-tab', currentTab);
    }

    // IMMEDIATELY update UI to prevent flicker
    // Update tab navigation buttons
    document.querySelectorAll('.nav-tab').forEach(tab => {
        if (tab.dataset.tab === currentTab) {
            tab.classList.add('active');
        } else {
            tab.classList.remove('active');
        }
    });

    // Show ONLY the current tab content, hide all others
    document.querySelectorAll('.tab-content').forEach(content => {
        const tabId = content.id.replace('-tab', '');
        if (tabId === currentTab) {
            content.classList.remove('hidden');
        } else {
            content.classList.add('hidden');
        }
    });

    // Toggle context bar visibility
    const contextBar = document.getElementById('contextBar');
    if (contextBar) {
        if (currentTab === 'professors') {
            contextBar.classList.add('hidden');
        } else {
            contextBar.classList.remove('hidden');
        }
    }

    // Update breadcrumbs for restored tab (will be called after navigation is initialized)
    setTimeout(() => {
        if (typeof updateBreadcrumbsForTab === 'function') {
            updateBreadcrumbsForTab(currentTab);
        }
    }, 0);
}

/**
 * Switch tab
 * Changes active tab and loads corresponding data
 * @param {string} tabName - Name of the tab to switch to
 * @returns {void}
 */
function switchTab(tabName) {
    try {
        console.log(`Switching to tab: ${tabName}`);
        currentTab = tabName;

        // Save active tab to localStorage
        localStorage.setItem('deanship-active-tab', tabName);

        // Update tab buttons
        document.querySelectorAll('.nav-tab').forEach(tab => {
            if (tab.dataset.tab === tabName) {
                tab.classList.add('active');
            } else {
                tab.classList.remove('active');
            }
        });

        // Show/hide tab content
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.add('hidden');
        });
        
        const tabContent = document.getElementById(`${tabName}-tab`);
        if (tabContent) {
            tabContent.classList.remove('hidden');
        } else {
            console.error(`Tab content element not found: ${tabName}-tab`);
            showToast(`Error: Tab content for ${tabName} not found`, 'error');
            return;
        }

        // Toggle context bar visibility
        const contextBar = document.getElementById('contextBar');
        if (contextBar) {
            if (tabName === 'professors' || tabName === 'academic-years' || tabName === 'courses') {
                contextBar.classList.add('hidden');
            } else {
                contextBar.classList.remove('hidden');
            }
        }

        // Update breadcrumbs
        updateBreadcrumbsForTab(tabName);

        // Load tab data
        loadTabData(tabName);
    } catch (error) {
        console.error(`Error switching to tab ${tabName}:`, error);
        showToast('Error switching tabs', 'error');
    }
}

/**
 * Update breadcrumbs based on current tab
 * Updates navigation breadcrumbs and page title
 * @param {string} tabName - Name of the current tab
 * @returns {void}
 */
function updateBreadcrumbsForTab(tabName) {
    const breadcrumbMap = {
        'dashboard': [
            { label: 'Home', path: '#' }
        ],
        'academic-years': [
            { label: 'Home', path: '#' },
            { label: 'Academic Years' }
        ],
        'professors': [
            { label: 'Home', path: '#' },
            { label: 'Professors' }
        ],
        'courses': [
            { label: 'Home', path: '#' },
            { label: 'Courses' }
        ],
        'assignments': [
            { label: 'Home', path: '#' },
            { label: 'Course Assignments' }
        ],
        'reports': [
            { label: 'Home', path: '#' },
            { label: 'Reports' }
        ],
        'file-explorer': [
            { label: 'Home', path: '#' },
            { label: 'File Explorer' }
        ]
    };

    const breadcrumbs = breadcrumbMap[tabName] || [{ label: 'Home' }];
    dashboardNavigation.updateBreadcrumbs(breadcrumbs);

    // Update page title
    const pageTitle = document.getElementById('pageTitle');
    if (pageTitle && breadcrumbs.length > 0) {
        pageTitle.textContent = breadcrumbs[breadcrumbs.length - 1].label;
    }
}



/**
 * Handle context change (academic year or semester)
 * Updates file explorer state and reloads relevant tab data
 * @returns {void}
 */
function onContextChange() {
    // Update FileExplorerState with new context
    if (selectedAcademicYearId && selectedSemesterId) {
        const semester = semesters.find(s => s.id === selectedSemesterId);
        fileExplorerState.setContext(
            selectedAcademicYearId,
            selectedSemesterId,
            selectedAcademicYear?.yearCode || '',
            semester?.type || ''
        );
    }

    // Update dashboard state
    if (selectedAcademicYearId) {
        dashboardState.setSelectedAcademicYear(selectedAcademicYearId);
    }
    if (selectedSemesterId) {
        dashboardState.setSelectedSemester(selectedSemesterId);
    }

    // Refresh analytics charts if on dashboard tab
    if (currentTab === 'dashboard' && dashboardAnalytics.initialized) {
        dashboardAnalytics.refreshAllCharts();
    }

    // Reload tab data if needed
    if (currentTab === 'assignments' || currentTab === 'reports' || currentTab === 'file-explorer' || currentTab === 'dashboard') {
        loadTabData(currentTab);
    }
}

/**
 * Load tab data
 * Loads data for the specified tab
 * @param {string} tabName - Name of the tab to load data for
 * @returns {void}
 */
function loadTabData(tabName) {
    console.log(`Loading data for tab: ${tabName}`);
    try {
        switch (tabName) {
            case 'dashboard':
                loadDashboardData();
                break;
            case 'academic-years':
                loadAcademicYears();
                break;
            case 'professors':
                loadProfessors();
                break;
            case 'courses':
                loadCourses();
                break;
            case 'assignments':
                // Load professors and courses first if not already loaded
                Promise.all([
                    professors.length === 0 ? loadProfessors() : Promise.resolve(),
                    courses.length === 0 ? loadCourses() : Promise.resolve()
                ]).then(() => {
                    loadAssignments();
                }).catch(err => {
                    console.error('Error loading dependencies for assignments:', err);
                    loadAssignments(); // Try loading anyway
                });
                break;
            case 'reports':
                if (typeof initializeReportsDashboard === 'function') {
                    initializeReportsDashboard();
                } else {
                    console.warn('initializeReportsDashboard function not found');
                    showToast('Reports module not loaded', 'warning');
                }
                break;
            case 'file-explorer':
                if (!selectedAcademicYearId || !selectedSemesterId) {
                    // Initialize file explorer even without context to show empty state
                    if (!fileExplorerInstance) {
                        initializeFileExplorer();
                    }
                }
                loadFileExplorer();
                break;
        }
    } catch (error) {
        console.error(`Error loading data for tab ${tabName}:`, error);
        showToast(`Failed to load ${tabName} data`, 'error');
    }
}

/**
 * Handle logout
 * Logs out user and redirects to login page
 * @returns {Promise<void>}
 */
async function handleLogout() {
    try {
        await apiRequest('/auth/logout', { method: 'POST' });
    } catch (error) {
        console.error('Logout error:', error);
    } finally {
        clearAuthData();
        redirectToLogin();
    }
}


// ============================================================================
// DASHBOARD
// ============================================================================

/**
 * Load dashboard statistics
 * Loads professors, courses, and initializes analytics
 * @returns {Promise<void>}
 */
async function loadDashboardData() {
    try {
        // Ensure professors and courses are loaded
        await Promise.all([
            professors.length === 0 ? loadProfessorsData() : Promise.resolve(),
            courses.length === 0 ? loadCoursesData() : Promise.resolve()
        ]);

        // Update dashboard cards
        updateDashboardStats();

        // Initialize analytics components
        await dashboardAnalytics.initialize();
    } catch (error) {
        console.error('Error loading dashboard data:', error);
    }
}

/**
 * Load professors data only (without UI update)
 * Fetches professors from API and updates state
 * @returns {Promise<void>}
 */
async function loadProfessorsData() {
    try {
        const response = await apiRequest('/deanship/professors', { method: 'GET' });
        professors = Array.isArray(response) ? response : (response?.content || []);
        // Update state for analytics
        if (typeof dashboardState !== 'undefined') {
            dashboardState.setProfessors(professors);
        }
    } catch (error) {
        console.error('Error loading professors data:', error);
        professors = [];
    }
}

/**
 * Load courses data only (without UI update)
 * Fetches courses from API and updates state
 * @returns {Promise<void>}
 */
async function loadCoursesData() {
    try {
        const response = await apiRequest('/deanship/courses', { method: 'GET' });
        courses = Array.isArray(response) ? response : (response?.content || []);
        // Update state for analytics
        if (typeof dashboardState !== 'undefined') {
            dashboardState.setCourses(courses);
        }
    } catch (error) {
        console.error('Error loading courses data:', error);
        courses = [];
    }
}

/**
 * Update dashboard statistics cards
 * Updates stat cards with current professor and course counts
 * @returns {void}
 */
function updateDashboardStats() {
    // Get all stat card values
    const statCards = document.querySelectorAll('#dashboard-tab .card span.text-2xl');

    if (statCards.length >= 3) {
        // Total Professors
        statCards[0].textContent = professors.length;

        // Active Courses  
        statCards[1].textContent = courses.length;

        // Pending Reports - placeholder for now
        statCards[2].textContent = '0';
    }
}

// ============================================================================
// ACADEMIC YEARS MANAGEMENT
// ============================================================================

/**
 * Load academic years data only (without UI update)
 * Fetches academic years from API and updates selector
 * @returns {Promise<void>}
 */
async function loadAcademicYearsData() {
    try {
        const response = await apiRequest('/deanship/academic-years', { method: 'GET' });
        academicYears = Array.isArray(response) ? response : (response?.content || []);
        updateAcademicYearSelector();
    } catch (error) {
        console.error('Error loading academic years:', error);
        showToast('Failed to load academic years', 'error');
        academicYears = []; // Ensure it's an array
    }
}

/**
 * Load academic years (full load with UI update)
 * Fetches academic years and renders table with skeleton loader
 * Uses minimum loading time to prevent flickering shimmer effect
 * @returns {Promise<void>}
 */
async function loadAcademicYears() {
    const tbody = document.getElementById('academicYearsTableBody');

    // Show skeleton loader
    if (tbody) tbody.innerHTML = SkeletonLoader.table(5, 5);

    try {
        // Use minimum loading time to prevent flickering shimmer effect
        const response = await withMinLoadingTime(() => 
            apiRequest('/deanship/academic-years', { method: 'GET' })
        );
        academicYears = Array.isArray(response) ? response : (response?.content || []);
        renderAcademicYearsTable();
        updateAcademicYearSelector();
    } catch (error) {
        console.error('Error loading academic years:', error);
        showToast('Failed to load academic years', 'error');
        academicYears = [];
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="px-6 py-8 text-center text-red-500">
                        Failed to load academic years. Please try again.
                    </td>
                </tr>
            `;
        }
    }
}

/**
 * Render academic years table
 * Renders academic years in table or shows empty state
 * @returns {void}
 */
function renderAcademicYearsTable() {
    const tbody = document.getElementById('academicYearsTableBody');

    if (academicYears.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="px-6 py-4">
                    <div id="academicYearsEmpty"></div>
                </td>
            </tr>
        `;
        EmptyState.render('academicYearsEmpty', {
            title: 'No Academic Years',
            message: 'Get started by creating your first academic year to organize semesters and courses.',
            illustration: 'no-academic-years',
            actionLabel: 'Add Academic Year',
            actionId: 'addAcademicYearBtnEmpty',
            actionCallback: showAddAcademicYearModal
        });
        return;
    }

    tbody.innerHTML = academicYears.map(year => `
        <tr>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${year.yearCode}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${year.startYear}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${year.endYear}</td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${year.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}">
                    ${year.isActive ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                <button 
                    onclick="window.deanship.editAcademicYear(${year.id})"
                    class="text-blue-600 hover:text-blue-900"
                >
                    Edit
                </button>
                ${!year.isActive ? `
                    <button 
                        onclick="window.deanship.activateAcademicYear(${year.id})"
                        class="text-green-600 hover:text-green-900"
                    >
                        Activate
                    </button>
                ` : ''}
            </td>
        </tr>
    `).join('');
}

/**
 * Update academic year selector
 * Updates dropdown with academic years and restores selection
 * @returns {void}
 */
function updateAcademicYearSelector() {
    const select = document.getElementById('academicYearSelect');

    if (academicYears.length === 0) {
        const html = '<option value="">No academic years available</option>';
        select.innerHTML = html;
        localStorage.setItem('deanship_academic_years_options', html);
        refreshDropdowns(); // Refresh modern dropdown
        return;
    }

    const html = '<option value="">Select academic year</option>' +
        academicYears.map(year =>
            `<option value="${year.id}">${year.yearCode}</option>`
        ).join('');
    select.innerHTML = html;
    localStorage.setItem('deanship_academic_years_options', html);

    // Try to restore selection from localStorage first
    const storedYearId = localStorage.getItem('deanship_selected_academic_year');
    let yearToSelect = null;

    if (storedYearId) {
        yearToSelect = academicYears.find(y => y.id.toString() === storedYearId);
    }

    // Fallback to current academic year based on Palestine timezone, then active, then first
    if (!yearToSelect) {
        const currentYearCode = getCurrentAcademicYearCode();
        yearToSelect = academicYears.find(y => y.yearCode === currentYearCode);
    }
    if (!yearToSelect) {
        yearToSelect = academicYears.find(y => y.isActive) || academicYears[0];
    }

    if (yearToSelect) {
        select.value = yearToSelect.id;
        selectedAcademicYearId = yearToSelect.id;
        selectedAcademicYear = yearToSelect;
        localStorage.setItem('deanship_selected_academic_year', yearToSelect.id);
        refreshDropdowns(); // Refresh modern dropdown after populating options
        loadSemesters(yearToSelect.id);
    } else {
        refreshDropdowns(); // Refresh even if no year selected
    }
}

/**
 * Load semesters for selected academic year
 * Loads semesters and updates selector dropdown
 * @param {number} academicYearId - ID of the academic year
 * @returns {Promise<void>}
 */
async function loadSemesters(academicYearId) {
    try {
        // Find the academic year and get its semesters
        const year = academicYears.find(y => y.id === academicYearId);
        if (!year || !year.semesters) {
            document.getElementById('semesterSelect').innerHTML = '<option value="">No semesters available</option>';
            refreshDropdowns(); // Refresh modern dropdown
            return;
        }

        semesters = year.semesters;
        const semesterSelect = document.getElementById('semesterSelect');

        if (semesters.length === 0) {
            const html = '<option value="">No semesters available for this year</option>';
            semesterSelect.innerHTML = html;
            localStorage.setItem('deanship_semesters_options', html);
            refreshDropdowns(); // Refresh modern dropdown
            return;
        }

        const html = '<option value="">Select semester</option>' +
            semesters.map(semester =>
                `<option value="${semester.id}">${semester.type} Semester</option>`
            ).join('');
        semesterSelect.innerHTML = html;
        localStorage.setItem('deanship_semesters_options', html);

        // Try to restore selection from localStorage first
        const storedSemesterId = localStorage.getItem('deanship_selected_semester');
        let semesterToSelect = null;

        if (storedSemesterId) {
            semesterToSelect = semesters.find(s => s.id.toString() === storedSemesterId);
        }

        // Fallback to current semester based on Palestine timezone, then first semester
        if (!semesterToSelect) {
            const currentSemesterType = getCurrentSemesterType();
            semesterToSelect = semesters.find(s => s.type === currentSemesterType);
        }
        if (!semesterToSelect && semesters.length > 0) {
            semesterToSelect = semesters[0];
        }

        if (semesterToSelect) {
            selectedSemesterId = semesterToSelect.id;
            semesterSelect.value = selectedSemesterId;
            localStorage.setItem('deanship_selected_semester', selectedSemesterId);
            refreshDropdowns(); // Refresh modern dropdown after populating options
            onContextChange();
        } else {
            refreshDropdowns(); // Refresh even if no semester selected
        }
    } catch (error) {
        console.error('Error loading semesters:', error);
        showToast('Failed to load semesters', 'error');
    }
}

/**
 * Show add academic year modal
 * Displays modal for creating new academic year
 * @returns {void}
 */
function showAddAcademicYearModal() {
    const content = `
        <form id="academicYearForm" class="space-y-4">
            <div>
                <label for="startYear" class="block text-sm font-medium text-gray-700 mb-1">Start Year *</label>
                <input 
                    type="number" 
                    id="startYear" 
                    name="startYear"
                    required
                    min="2000"
                    max="2100"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., 2024"
                >
            </div>
            <div>
                <label for="endYear" class="block text-sm font-medium text-gray-700 mb-1">End Year *</label>
                <input 
                    type="number" 
                    id="endYear" 
                    name="endYear"
                    required
                    min="2000"
                    max="2100"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., 2025"
                >
            </div>
            <p class="text-sm text-gray-600">
                The system will automatically create three semesters (First, Second, Summer) for this academic year.
            </p>
        </form>
    `;

    showModal('Add Academic Year', content, {
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
                onClick: (close) => handleCreateAcademicYear(close)
            }
        ]
    });
}

/**
 * Handle create academic year
 * Validates form and creates new academic year via API
 * @param {Function} closeModal - Function to close the modal
 * @returns {Promise<void>}
 */
async function handleCreateAcademicYear(closeModal) {
    const form = document.getElementById('academicYearForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const startYear = parseInt(document.getElementById('startYear').value);
    const endYear = parseInt(document.getElementById('endYear').value);

    if (endYear <= startYear) {
        showToast('End year must be greater than start year', 'error');
        return;
    }

    try {
        // Generate yearCode in the format "YYYY-YYYY"
        const yearCode = `${startYear}-${endYear}`;

        await apiRequest('/deanship/academic-years', {
            method: 'POST',
            body: JSON.stringify({
                yearCode,
                startYear,
                endYear
            })
        });

        showToast('Academic year created successfully', 'success');
        closeModal();
        loadAcademicYears();
    } catch (error) {
        console.error('Error creating academic year:', error);
        showToast(getErrorMessage(error), 'error');
    }
}

/**
 * Edit academic year
 */
window.deanship = window.deanship || {};
window.deanship.editAcademicYear = function (yearId) {
    const year = academicYears.find(y => y.id === yearId);
    if (!year) return;

    const content = `
        <form id="editAcademicYearForm" class="space-y-4">
            <div>
                <label for="editStartYear" class="block text-sm font-medium text-gray-700 mb-1">Start Year *</label>
                <input 
                    type="number" 
                    id="editStartYear" 
                    name="startYear"
                    required
                    min="2000"
                    max="2100"
                    value="${year.startYear}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
            </div>
            <div>
                <label for="editEndYear" class="block text-sm font-medium text-gray-700 mb-1">End Year *</label>
                <input 
                    type="number" 
                    id="editEndYear" 
                    name="endYear"
                    required
                    min="2000"
                    max="2100"
                    value="${year.endYear}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
            </div>
        </form>
    `;

    showModal('Edit Academic Year', content, {
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
                onClick: (close) => handleUpdateAcademicYear(yearId, close)
            }
        ]
    });
};

/**
 * Handle update academic year
 * Validates form and updates academic year via API
 * @param {number} yearId - ID of the academic year to update
 * @param {Function} closeModal - Function to close the modal
 * @returns {Promise<void>}
 */
async function handleUpdateAcademicYear(yearId, closeModal) {
    const form = document.getElementById('editAcademicYearForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const startYear = parseInt(document.getElementById('editStartYear').value);
    const endYear = parseInt(document.getElementById('editEndYear').value);

    if (endYear <= startYear) {
        showToast('End year must be greater than start year', 'error');
        return;
    }

    try {
        // Generate yearCode in the format "YYYY-YYYY"
        const yearCode = `${startYear}-${endYear}`;

        await apiRequest(`/deanship/academic-years/${yearId}`, {
            method: 'PUT',
            body: JSON.stringify({
                yearCode,
                startYear,
                endYear
            })
        });

        showToast('Academic year updated successfully', 'success');
        closeModal();
        loadAcademicYears();
    } catch (error) {
        console.error('Error updating academic year:', error);
        showToast(getErrorMessage(error), 'error');
    }
}

/**
 * Activate academic year
 */
window.deanship.activateAcademicYear = async function (yearId) {
    try {
        await apiRequest(`/deanship/academic-years/${yearId}/activate`, {
            method: 'PUT'
        });

        showToast('Academic year activated successfully', 'success');
        loadAcademicYears();
    } catch (error) {
        console.error('Error activating academic year:', error);
        showToast(getErrorMessage(error), 'error');
    }
};

// ============================================================================
// COURSES MANAGEMENT
// ============================================================================

/**
 * Load departments
 * Fetches departments from API and updates filters
 * @returns {Promise<void>}
 */
async function loadDepartments() {
    try {
        departments = await apiRequest('/deanship/departments', { method: 'GET' });
        updateDepartmentFilters();
    } catch (error) {
        console.error('Error loading departments:', error);
        // If endpoint doesn't exist, use mock data
        departments = [
            { id: 1, name: 'Computer Science' },
            { id: 2, name: 'Mathematics' },
            { id: 3, name: 'Physics' },
            { id: 4, name: 'Chemistry' }
        ];
        updateDepartmentFilters();
    }
}

/**
 * Update department filters
 * Updates department filter dropdowns across tabs
 * @returns {void}
 */
function updateDepartmentFilters() {
    console.log('updateDepartmentFilters called, departments:', departments);
    
    const filters = [
        document.getElementById('professorDepartmentFilter'),
        document.getElementById('courseDepartmentFilter')
    ];

    filters.forEach(filter => {
        if (filter) {
            const currentValue = filter.value;
            console.log(`Updating filter ${filter.id} with ${departments.length} departments`);
            filter.innerHTML = '<option value="">All Departments</option>' +
                departments.map(dept =>
                    `<option value="${dept.id}">${dept.name}</option>`
                ).join('');
            filter.value = currentValue;
            
            // Refresh modern dropdown if it exists
            if (typeof window.refreshModernDropdown === 'function') {
                window.refreshModernDropdown(filter);
            }
        }
    });
}

/**
 * Load professors
 * Fetches professors with optional department filter and renders table
 * Uses minimum loading time to prevent flickering shimmer effect
 * @returns {Promise<void>}
 */
async function loadProfessors() {
    const tbody = document.getElementById('professorsTableBody');

    // Show skeleton loader
    if (tbody) {
        tbody.innerHTML = SkeletonLoader.table(5, 6);
    }

    try {
        const departmentId = document.getElementById('professorDepartmentFilter')?.value;
        const params = departmentId ? `?departmentId=${departmentId}` : '';

        // Use minimum loading time to prevent flickering shimmer effect
        const response = await withMinLoadingTime(() => 
            apiRequest(`/deanship/professors${params}`, { method: 'GET' })
        );
        professors = Array.isArray(response) ? response : (response?.content || []);

        // Use enhanced table if available, otherwise fallback to basic rendering
        if (typeof tableEnhancementManager !== 'undefined') {
            tableEnhancementManager.enhanceProfessorsTable();
        } else {
            renderProfessorsTable();
        }

        // Add export buttons to professors table
        if (window.TableExportHelper) {
            setTimeout(() => {
                window.TableExportHelper.addToProfessorsTable();
            }, 100);
        }
    } catch (error) {
        console.error('Error loading professors:', error);
        showToast('Failed to load professors', 'error');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="px-6 py-8 text-center text-red-500">
                        Failed to load professors. Please try again.
                    </td>
                </tr>
            `;
        }
    }
}

/**
 * Filter professors
 * Applies client-side filtering based on search and department filter
 * @returns {void}
 */
function filterProfessors() {
    // Use enhanced table if available, otherwise fallback to basic rendering
    if (typeof tableEnhancementManager !== 'undefined' && tableEnhancementManager) {
        tableEnhancementManager.renderEnhancedProfessorsTable();
    } else {
        renderProfessorsTable();
    }
}

/**
 * Render professors table
 * Renders professors in table with search filtering and empty states
 * @returns {void}
 */
function renderProfessorsTable() {
    const tbody = document.getElementById('professorsTableBody');
    if (!tbody) return;

    const searchInput = document.getElementById('professorSearch');
    const searchTerm = searchInput ? searchInput.value.toLowerCase().trim() : '';
    const departmentFilter = document.getElementById('professorDepartmentFilter');
    const selectedDeptId = departmentFilter && departmentFilter.value ? parseInt(departmentFilter.value) : null;

    // Ensure professors is an array
    if (!Array.isArray(professors)) {
        console.error('Professors data is not an array:', professors);
        professors = [];
    }

    let filteredProfessors = [...professors];
    
    // Apply department filter
    if (selectedDeptId) {
        filteredProfessors = filteredProfessors.filter(prof => prof.department?.id === selectedDeptId);
    }
    
    // Apply search filter
    if (searchTerm) {
        filteredProfessors = filteredProfessors.filter(prof =>
            (prof.name && prof.name.toLowerCase().includes(searchTerm)) ||
            (prof.email && prof.email.toLowerCase().includes(searchTerm)) ||
            (prof.professorId && prof.professorId.toLowerCase().includes(searchTerm))
        );
    }

    if (filteredProfessors.length === 0) {
        const isFiltering = searchTerm.length > 0 || selectedDeptId !== null;
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-4">
                    <div id="professorsEmpty"></div>
                </td>
            </tr>
        `;
        EmptyState.render('professorsEmpty', {
            title: isFiltering ? 'No Professors Found' : 'No Professors',
            message: isFiltering
                ? 'No professors match your search criteria. Try adjusting your filters.'
                : 'Get started by adding professors to manage their courses and documents.',
            illustration: isFiltering ? 'no-search-results' : 'no-professors',
            actionLabel: isFiltering ? null : 'Add Professor',
            actionId: 'addProfessorBtnEmpty',
            actionCallback: isFiltering ? null : showAddProfessorModal
        });
        return;
    }

    tbody.innerHTML = filteredProfessors.map(prof => `
        <tr>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${prof.professorId || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${prof.name}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${prof.email}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${prof.department?.name || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${prof.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}">
                    ${prof.isActive ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                <button 
                    onclick="window.deanship.editProfessor(${prof.id})"
                    class="text-blue-600 hover:text-blue-900"
                >
                    Edit
                </button>
                ${prof.isActive ? `
                    <button 
                        onclick="window.deanship.deactivateProfessor(${prof.id})"
                        class="text-red-600 hover:text-red-900"
                    >
                        Deactivate
                    </button>
                ` : `
                    <button 
                        onclick="window.deanship.activateProfessor(${prof.id})"
                        class="text-green-600 hover:text-green-900"
                    >
                        Activate
                    </button>
                `}
            </td>
        </tr>
    `).join('');
}

/**
 * Show add professor modal
 * Displays modal for creating new professor
 * @returns {void}
 */
function showAddProfessorModal() {
    const content = `
        <form id="professorForm" class="space-y-4">
            <div>
                <label for="profName" class="block text-sm font-medium text-gray-700 mb-1">Full Name *</label>
                <input 
                    type="text" 
                    id="profName" 
                    name="name"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., Dr. John Smith"
                >
            </div>
            <div>
                <label for="profEmail" class="block text-sm font-medium text-gray-700 mb-1">Email *</label>
                <input 
                    type="email" 
                    id="profEmail" 
                    name="email"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., john.smith@alquds.edu"
                >
            </div>
            <div>
                <label for="profPassword" class="block text-sm font-medium text-gray-700 mb-1">Password *</label>
                <input 
                    type="password" 
                    id="profPassword" 
                    name="password"
                    required
                    minlength="6"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="Minimum 6 characters"
                >
            </div>
            <div>
                <label for="profDepartment" class="block text-sm font-medium text-gray-700 mb-1">Department *</label>
                <select 
                    id="profDepartment" 
                    name="departmentId"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    <option value="">Select department...</option>
                    ${departments.map(dept => `<option value="${dept.id}">${dept.name}</option>`).join('')}
                </select>
            </div>
            <p class="text-sm text-gray-600">
                A unique professor ID will be automatically generated.
            </p>
        </form>
    `;

    showModal('Add Professor', content, {
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
                onClick: (close) => handleCreateProfessor(close)
            }
        ]
    });
}

/**
 * Handle create professor
 * Validates form and creates new professor via API
 * @param {Function} closeModal - Function to close the modal
 * @returns {Promise<void>}
 */
async function handleCreateProfessor(closeModal) {
    const form = document.getElementById('professorForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    // Get the full name and split it
    const fullName = document.getElementById('profName').value.trim();
    const nameParts = fullName.split(' ');
    const firstName = nameParts[0] || '';
    const lastName = nameParts.slice(1).join(' ') || '';

    // Validate name parts
    if (!firstName || !lastName) {
        showToast('Please enter both first name and last name separated by a space', 'error');
        return;
    }

    const data = {
        firstName: firstName,
        lastName: lastName,
        email: document.getElementById('profEmail').value,
        password: document.getElementById('profPassword').value,
        departmentId: parseInt(document.getElementById('profDepartment').value)
    };

    try {
        await apiRequest('/deanship/professors', {
            method: 'POST',
            body: JSON.stringify(data)
        });

        showToast('Professor created successfully', 'success');
        closeModal();
        loadProfessors();

        // Refresh File Explorer if on file-explorer tab and context is set
        if (currentTab === 'file-explorer' && fileExplorerState.hasContext() && fileExplorerInstance) {
            await fileExplorerInstance.loadRoot(selectedAcademicYearId, selectedSemesterId, true);
            showToast('Professor folder created', 'info');
        }
    } catch (error) {
        console.error('Error creating professor:', error);
        const errorMessage = getErrorMessage(error);
        showToast(errorMessage, 'error');
    }
}

/**
 * Edit professor
 */
window.deanship.editProfessor = function (profId) {
    const prof = professors.find(p => p.id == profId);
    if (!prof) return;

    const content = `
        <form id="editProfessorForm" class="space-y-4">
            <div>
                <label for="editProfName" class="block text-sm font-medium text-gray-700 mb-1">Full Name *</label>
                <input 
                    type="text" 
                    id="editProfName" 
                    name="name"
                    required
                    value="${prof.name}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
            </div>
            <div>
                <label for="editProfEmail" class="block text-sm font-medium text-gray-700 mb-1">Email *</label>
                <input 
                    type="email" 
                    id="editProfEmail" 
                    name="email"
                    required
                    value="${prof.email}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
            </div>
            <div>
                <label for="editProfDepartment" class="block text-sm font-medium text-gray-700 mb-1">Department *</label>
                <select 
                    id="editProfDepartment" 
                    name="departmentId"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    ${departments.map(dept =>
        `<option value="${dept.id}" ${prof.department?.id === dept.id ? 'selected' : ''}>${dept.name}</option>`
    ).join('')}
                </select>
            </div>
            <div>
                <label for="editProfPassword" class="block text-sm font-medium text-gray-700 mb-1">New Password</label>
                <input 
                    type="password" 
                    id="editProfPassword" 
                    name="password"
                    minlength="6"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="Leave blank to keep current password"
                >
            </div>
        </form>
    `;

    showModal('Edit Professor', content, {
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
                onClick: (close) => handleUpdateProfessor(profId, close)
            }
        ]
    });
};

/**
 * Handle update professor
 * Validates form and updates professor via API
 * @param {number} profId - ID of the professor to update
 * @param {Function} closeModal - Function to close the modal
 * @returns {Promise<void>}
 */
async function handleUpdateProfessor(profId, closeModal) {
    const form = document.getElementById('editProfessorForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    // Get the full name and split it
    const fullName = document.getElementById('editProfName').value.trim();
    const nameParts = fullName.split(' ');
    const firstName = nameParts[0] || '';
    const lastName = nameParts.slice(1).join(' ') || '';

    // Validate name parts
    if (!firstName || !lastName) {
        showToast('Please enter both first name and last name separated by a space', 'error');
        return;
    }

    const data = {
        firstName: firstName,
        lastName: lastName,
        email: document.getElementById('editProfEmail').value,
        departmentId: parseInt(document.getElementById('editProfDepartment').value)
    };

    const password = document.getElementById('editProfPassword').value;
    if (password) {
        data.password = password;
    }

    try {
        await apiRequest(`/deanship/professors/${profId}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });

        showToast('Professor updated successfully', 'success');
        closeModal();
        loadProfessors();
    } catch (error) {
        console.error('Error updating professor:', error);
        const errorMessage = getErrorMessage(error);
        showToast(errorMessage, 'error');
    }
}

/**
 * Deactivate professor
 */
window.deanship.deactivateProfessor = function (profId, btnElement) {
    const prof = professors.find(p => p.id == profId);
    if (!prof) return;

    showConfirm(
        'Deactivate Professor',
        `Are you sure you want to deactivate ${prof.name}? They will no longer be able to log in.`,
        async () => {
            if (btnElement) {
                btnElement.disabled = true;
                btnElement.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
            }
            try {
                await apiRequest(`/deanship/professors/${profId}/deactivate`, {
                    method: 'PUT'
                });

                showToast('Professor deactivated successfully', 'success');
                loadProfessors();
            } catch (error) {
                console.error('Error deactivating professor:', error);
                showToast(getErrorMessage(error), 'error');
                if (btnElement) {
                    btnElement.disabled = false;
                    btnElement.innerHTML = '<i class="fas fa-ban"></i>';
                }
            }
        },
        { danger: true }
    );
};

/**
 * Activate professor
 */
window.deanship.activateProfessor = async function (profId, btnElement) {
    if (btnElement) {
        btnElement.disabled = true;
        btnElement.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    }
    try {
        await apiRequest(`/deanship/professors/${profId}/activate`, {
            method: 'PUT'
        });

        showToast('Professor activated successfully', 'success');
        loadProfessors();
    } catch (error) {
        console.error('Error activating professor:', error);
        showToast(getErrorMessage(error), 'error');
        if (btnElement) {
            btnElement.disabled = false;
            btnElement.innerHTML = '<i class="fas fa-check"></i>';
        }
    }
};

/**
 * Delete professor
 */
window.deanship.deleteProfessor = function (profId, btnElement) {
    const prof = professors.find(p => p.id == profId);
    if (!prof) return;

    showConfirm(
        'Delete Professor',
        `Are you sure you want to delete ${prof.name}? This action cannot be undone.`,
        async () => {
            if (btnElement) {
                btnElement.disabled = true;
                btnElement.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
            }
            try {
                await apiRequest(`/deanship/professors/${profId}`, {
                    method: 'DELETE'
                });

                showToast('Professor deleted successfully', 'success');
                loadProfessors();
            } catch (error) {
                console.error('Error deleting professor:', error);
                showToast(getErrorMessage(error), 'error');
                if (btnElement) {
                    btnElement.disabled = false;
                    btnElement.innerHTML = '<i class="fas fa-trash"></i>';
                }
            }
        },
        { danger: true }
    );
};

// ============================================================================
// COURSES MANAGEMENT
// ============================================================================

/**
 * Load courses
 * Fetches courses with optional department filter and renders table
 * Uses minimum loading time to prevent flickering shimmer effect
 * @returns {Promise<void>}
 */
async function loadCourses() {
    console.log('loadCourses: Starting...');
    const tbody = document.getElementById('coursesTableBody');

    // Show skeleton loader
    if (tbody) {
        tbody.innerHTML = SkeletonLoader.table(5, 6);
    } else {
        console.error('loadCourses: coursesTableBody not found!');
        return;
    }

    try {
        const departmentId = document.getElementById('courseDepartmentFilter')?.value;
        const params = departmentId ? `?departmentId=${departmentId}` : '';

        console.log(`loadCourses: Fetching from /deanship/courses${params}`);
        // Use minimum loading time to prevent flickering shimmer effect
        const response = await withMinLoadingTime(() => 
            apiRequest(`/deanship/courses${params}`, { method: 'GET' })
        );
        console.log('loadCourses: Response received', response);
        
        courses = Array.isArray(response) ? response : (response?.content || []);
        console.log(`loadCourses: Parsed courses array. Length: ${courses.length}`);

        // Extract unique departments from courses if not already set
        if (departments.length === 0) {
            departments = [...new Set(courses.map(c => c.department).filter(Boolean))];
        }

        // Use enhanced table if available, otherwise fallback to basic rendering
        if (typeof tableEnhancementManager !== 'undefined') {
            console.log('loadCourses: Using tableEnhancementManager');
            tableEnhancementManager.enhanceCoursesTable();
        } else {
            console.log('loadCourses: Using renderCoursesTable');
            renderCoursesTable();
        }

        // Add export buttons to courses table
        if (window.TableExportHelper) {
            setTimeout(() => {
                window.TableExportHelper.addToCoursesTable();
            }, 100);
        }
    } catch (error) {
        console.error('Error loading courses:', error);
        showToast('Failed to load courses', 'error');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="px-6 py-8 text-center text-red-500">
                        Failed to load courses. Please try again.
                    </td>
                </tr>
            `;
        }
    }
}

/**
 * Filter courses by search term (client-side filtering)
 * Re-renders the courses table with the current search filter
 * @returns {void}
 */
function filterCoursesBySearch() {
    // Use enhanced table if available, otherwise fallback to basic rendering
    if (typeof tableEnhancementManager !== 'undefined') {
        tableEnhancementManager.enhanceCoursesTable();
    } else {
        renderCoursesTable();
    }
}

/**
 * Filter courses by department (server-side filtering)
 * Reloads courses from API with the selected department filter
 * @returns {void}
 */
function filterCoursesByDepartment() {
    const filter = document.getElementById('courseDepartmentFilter');
    console.log('filterCoursesByDepartment called, value:', filter?.value);
    loadCourses();
}

/**
 * Render courses table
 * Renders courses in table with search filtering and empty states
 * @returns {void}
 */
function renderCoursesTable() {
    console.log('renderCoursesTable: Starting...');
    const tbody = document.getElementById('coursesTableBody');
    if (!tbody) {
        console.error('coursesTableBody element not found');
        return;
    }
    
    const searchInput = document.getElementById('courseSearch');
    const searchTerm = searchInput ? searchInput.value.toLowerCase() : '';

    // Ensure courses is an array
    if (!Array.isArray(courses)) {
        console.error('Courses data is not an array:', courses);
        courses = [];
    }

    let filteredCourses = courses;
    if (searchTerm) {
        filteredCourses = courses.filter(course =>
            (course.courseCode && course.courseCode.toLowerCase().includes(searchTerm)) ||
            (course.courseName && course.courseName.toLowerCase().includes(searchTerm))
        );
    }
    
    console.log(`renderCoursesTable: Rendering ${filteredCourses.length} courses (Total: ${courses.length})`);

    if (filteredCourses.length === 0) {
        console.log('renderCoursesTable: Rendering empty state');
        const isSearching = searchTerm.length > 0;
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-4">
                    <div id="coursesEmpty"></div>
                </td>
            </tr>
        `;
        EmptyState.render('coursesEmpty', {
            title: isSearching ? 'No Courses Found' : 'No Courses',
            message: isSearching
                ? 'No courses match your search criteria. Try adjusting your filters.'
                : 'Get started by adding courses that professors can teach.',
            illustration: isSearching ? 'no-search-results' : 'no-courses',
            actionLabel: isSearching ? null : 'Add Course',
            actionId: 'addCourseBtnEmpty',
            actionCallback: isSearching ? null : showAddCourseModal
        });
        return;
    }

    tbody.innerHTML = filteredCourses.map(course => `
        <tr>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${course.courseCode || ''}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${course.courseName || ''}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${course.department?.name || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${course.level || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${course.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}">
                    ${course.isActive ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                <button 
                    onclick="window.deanship.editCourse(${course.id})"
                    class="text-blue-600 hover:text-blue-900"
                >
                    Edit
                </button>
                ${course.isActive ? `
                    <button 
                        onclick="window.deanship.deactivateCourse(${course.id})"
                        class="text-red-600 hover:text-red-900"
                    >
                        Deactivate
                    </button>
                ` : ''}
            </td>
        </tr>
    `).join('');
    console.log('renderCoursesTable: Render complete');
}

/**
 * Show add course modal
 * Displays modal for creating new course
 * @returns {void}
 */
function showAddCourseModal() {
    const content = `
        <form id="courseForm" class="space-y-4">
            <div>
                <label for="courseCode" class="block text-sm font-medium text-gray-700 mb-1">Course Code *</label>
                <input 
                    type="text" 
                    id="courseCode" 
                    name="courseCode"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., CS101"
                >
            </div>
            <div>
                <label for="courseName" class="block text-sm font-medium text-gray-700 mb-1">Course Name *</label>
                <input 
                    type="text" 
                    id="courseName" 
                    name="courseName"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., Introduction to Computer Science"
                >
            </div>
            <div>
                <label for="courseDepartment" class="block text-sm font-medium text-gray-700 mb-1">Department *</label>
                <select 
                    id="courseDepartment" 
                    name="departmentId"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    <option value="">Select department...</option>
                    ${departments.map(dept => `<option value="${dept.id}">${dept.name}</option>`).join('')}
                </select>
            </div>
            <div>
                <label for="courseLevel" class="block text-sm font-medium text-gray-700 mb-1">Level</label>
                <select 
                    id="courseLevel" 
                    name="level"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    <option value="">Select level...</option>
                    <option value="Undergraduate">Undergraduate</option>
                    <option value="Graduate">Graduate</option>
                    <option value="Doctoral">Doctoral</option>
                </select>
            </div>
            <div>
                <label for="courseDescription" class="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea 
                    id="courseDescription" 
                    name="description"
                    rows="3"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="Course description..."
                ></textarea>
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
                onClick: (close) => handleCreateCourse(close)
            }
        ]
    });
}

/**
 * Handle create course
 * Validates form and creates new course via API
 * @param {Function} closeModal - Function to close the modal
 * @returns {Promise<void>}
 */
async function handleCreateCourse(closeModal) {
    const form = document.getElementById('courseForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const data = {
        courseCode: document.getElementById('courseCode').value,
        courseName: document.getElementById('courseName').value,
        departmentId: parseInt(document.getElementById('courseDepartment').value),
        level: document.getElementById('courseLevel').value || null,
        description: document.getElementById('courseDescription').value || null
    };

    try {
        await apiRequest('/deanship/courses', {
            method: 'POST',
            body: JSON.stringify(data)
        });

        showToast('Course created successfully', 'success');
        closeModal();
        loadCourses();
    } catch (error) {
        console.error('Error creating course:', error);
        showToast(getErrorMessage(error), 'error');
    }
}

/**
 * Edit course
 */
window.deanship.editCourse = function (courseId) {
    const course = courses.find(c => c.id === courseId);
    if (!course) return;

    const content = `
        <form id="editCourseForm" class="space-y-4">
            <div>
                <label for="editCourseCode" class="block text-sm font-medium text-gray-700 mb-1">Course Code *</label>
                <input 
                    type="text" 
                    id="editCourseCode" 
                    name="courseCode"
                    required
                    value="${course.courseCode}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
            </div>
            <div>
                <label for="editCourseName" class="block text-sm font-medium text-gray-700 mb-1">Course Name *</label>
                <input 
                    type="text" 
                    id="editCourseName" 
                    name="courseName"
                    required
                    value="${course.courseName}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
            </div>
            <div>
                <label for="editCourseDepartment" class="block text-sm font-medium text-gray-700 mb-1">Department *</label>
                <select 
                    id="editCourseDepartment" 
                    name="departmentId"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    ${departments.map(dept =>
        `<option value="${dept.id}" ${course.department?.id === dept.id ? 'selected' : ''}>${dept.name}</option>`
    ).join('')}
                </select>
            </div>
            <div>
                <label for="editCourseLevel" class="block text-sm font-medium text-gray-700 mb-1">Level</label>
                <select 
                    id="editCourseLevel" 
                    name="level"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    <option value="">Select level...</option>
                    <option value="Undergraduate" ${course.level === 'Undergraduate' ? 'selected' : ''}>Undergraduate</option>
                    <option value="Graduate" ${course.level === 'Graduate' ? 'selected' : ''}>Graduate</option>
                    <option value="Doctoral" ${course.level === 'Doctoral' ? 'selected' : ''}>Doctoral</option>
                </select>
            </div>
            <div>
                <label for="editCourseDescription" class="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea 
                    id="editCourseDescription" 
                    name="description"
                    rows="3"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >${course.description || ''}</textarea>
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
                onClick: (close) => handleUpdateCourse(courseId, close)
            }
        ]
    });
};

/**
 * Handle update course
 * Validates form and updates course via API
 * @param {number} courseId - ID of the course to update
 * @param {Function} closeModal - Function to close the modal
 * @returns {Promise<void>}
 */
async function handleUpdateCourse(courseId, closeModal) {
    const form = document.getElementById('editCourseForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const data = {
        courseCode: document.getElementById('editCourseCode').value,
        courseName: document.getElementById('editCourseName').value,
        departmentId: parseInt(document.getElementById('editCourseDepartment').value),
        level: document.getElementById('editCourseLevel').value || null,
        description: document.getElementById('editCourseDescription').value || null
    };

    try {
        await apiRequest(`/deanship/courses/${courseId}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });

        showToast('Course updated successfully', 'success');
        closeModal();
        loadCourses();
    } catch (error) {
        console.error('Error updating course:', error);
        showToast(getErrorMessage(error), 'error');
    }
}

/**
 * Deactivate course
 */
window.deanship.deactivateCourse = function (courseId) {
    const course = courses.find(c => c.id === courseId);
    if (!course) return;

    showConfirm(
        'Deactivate Course',
        `Are you sure you want to deactivate ${course.courseCode} - ${course.courseName}?`,
        async () => {
            try {
                await apiRequest(`/deanship/courses/${courseId}/deactivate`, {
                    method: 'PUT'
                });

                showToast('Course deactivated successfully', 'success');
                loadCourses();
            } catch (error) {
                console.error('Error deactivating course:', error);
                showToast(getErrorMessage(error), 'error');
            }
        },
        { danger: true }
    );
};

/**
 * Activate course
 */
window.deanship.activateCourse = async function (courseId) {
    try {
        await apiRequest(`/deanship/courses/${courseId}/activate`, {
            method: 'PUT'
        });

        showToast('Course activated successfully', 'success');
        loadCourses();
    } catch (error) {
        console.error('Error activating course:', error);
        showToast(getErrorMessage(error), 'error');
    }
};

// ============================================================================
// COURSE ASSIGNMENTS MANAGEMENT
// ============================================================================

let assignments = [];

/**
 * Load assignments
 * Fetches course assignments for selected semester and renders table
 * @returns {Promise<void>}
 */
async function loadAssignments() {
    console.log('loadAssignments: Starting...');
    const tbody = document.getElementById('assignmentsTableBody');

    if (!selectedSemesterId) {
        console.log('loadAssignments: No semester selected');
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-4">
                    <div id="assignmentsNoContext"></div>
                </td>
            </tr>
        `;
        EmptyState.render('assignmentsNoContext', {
            title: 'Select Academic Context',
            message: 'Please select an academic year and semester to view course assignments.',
            illustration: 'no-assignments'
        });
        return;
    }

    // Show skeleton loader
    tbody.innerHTML = SkeletonLoader.table(5, 6);

    try {
        const professorId = document.getElementById('assignmentProfessorFilter').value;
        const params = new URLSearchParams({ semesterId: selectedSemesterId });
        if (professorId) params.append('professorId', professorId);

        console.log(`loadAssignments: Fetching from /deanship/course-assignments?${params}`);
        // Use minimum loading time to prevent flickering shimmer effect
        const response = await withMinLoadingTime(() => 
            apiRequest(`/deanship/course-assignments?${params}`, { method: 'GET' })
        );
        console.log('loadAssignments: Response received', response);
        
        assignments = Array.isArray(response) ? response : (response?.content || []);
        console.log(`loadAssignments: Parsed assignments array. Length: ${assignments.length}`);
        
        renderAssignmentsTable();
        updateAssignmentFilters();

        // Add export buttons to assignments table
        if (window.TableExportHelper) {
            setTimeout(() => {
                window.TableExportHelper.addToAssignmentsTable();
            }, 100);
        }
    } catch (error) {
        console.error('Error loading assignments:', error);
        showToast('Failed to load assignments', 'error');
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-8 text-center text-red-500">
                    Failed to load assignments. Please try again.
                </td>
            </tr>
        `;
    }
}

/**
 * Update assignment filters
 * Updates professor and course filter dropdowns
 * @returns {void}
 */
function updateAssignmentFilters() {
    // Update professor filter
    const profFilter = document.getElementById('assignmentProfessorFilter');
    const currentProfValue = profFilter.value;
    profFilter.innerHTML = '<option value="">All Professors</option>' +
        professors.map(prof => `<option value="${prof.id}">${prof.name}</option>`).join('');
    profFilter.value = currentProfValue;
    
    // Refresh modern dropdown for professor filter
    if (typeof window.refreshModernDropdown === 'function') {
        window.refreshModernDropdown(profFilter);
    }

    // Update course filter
    const courseFilter = document.getElementById('assignmentCourseFilter');
    const currentCourseValue = courseFilter.value;
    courseFilter.innerHTML = '<option value="">All Courses</option>' +
        courses.map(course => `<option value="${course.id}">${course.courseCode} - ${course.courseName}</option>`).join('');
    courseFilter.value = currentCourseValue;
    
    // Refresh modern dropdown for course filter
    if (typeof window.refreshModernDropdown === 'function') {
        window.refreshModernDropdown(courseFilter);
    }
}

/**
 * Render assignments table
 * Renders course assignments with filtering and empty states
 * @returns {void}
 */
function renderAssignmentsTable() {
    console.log('renderAssignmentsTable: Starting...');
    const tbody = document.getElementById('assignmentsTableBody');
    if (!tbody) {
        console.error('assignmentsTableBody element not found');
        return;
    }

    const courseFilterEl = document.getElementById('assignmentCourseFilter');
    const courseFilter = courseFilterEl ? courseFilterEl.value : '';

    // Ensure assignments is an array
    if (!Array.isArray(assignments)) {
        console.error('Assignments data is not an array:', assignments);
        assignments = [];
    }

    let filteredAssignments = assignments;
    if (courseFilter) {
        filteredAssignments = assignments.filter(a => a.course && a.course.id === parseInt(courseFilter));
    }
    
    console.log(`renderAssignmentsTable: Rendering ${filteredAssignments.length} assignments (Total: ${assignments.length})`);

    if (filteredAssignments.length === 0) {
        console.log('renderAssignmentsTable: Rendering empty state');
        const isFiltering = courseFilter.length > 0;
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-4">
                    <div id="assignmentsEmpty"></div>
                </td>
            </tr>
        `;
        EmptyState.render('assignmentsEmpty', {
            title: isFiltering ? 'No Assignments Found' : 'No Course Assignments',
            message: isFiltering
                ? 'No assignments match your filter criteria. Try adjusting your filters.'
                : 'Assign courses to professors for the selected semester to get started.',
            illustration: isFiltering ? 'no-search-results' : 'no-assignments',
            actionLabel: isFiltering ? null : 'Assign Course',
            actionId: 'addAssignmentBtnEmpty',
            actionCallback: isFiltering ? null : showAddAssignmentModal
        });
        return;
    }

    tbody.innerHTML = filteredAssignments.map(assignment => `
        <tr>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${assignment.professor?.name || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${assignment.course?.courseCode} - ${assignment.course?.courseName}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${assignment.course?.department?.name || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${assignment.semester?.type || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${assignment.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}">
                    ${assignment.isActive ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <button 
                    onclick="window.deanship.unassignCourse(${assignment.id})"
                    class="text-red-600 hover:text-red-900"
                >
                    Unassign
                </button>
            </td>
        </tr>
    `).join('');
    console.log('renderAssignmentsTable: Render complete');
}

/**
 * Show add assignment modal
 * Displays modal for creating new course assignment
 * @returns {void}
 */
function showAddAssignmentModal() {
    if (!selectedSemesterId) {
        showToast('Please select an academic year and semester first', 'warning');
        return;
    }

    const semester = semesters.find(s => s.id === selectedSemesterId);
    if (!semester) {
        showToast('Semester not found', 'error');
        return;
    }

    const content = `
        <form id="assignmentForm" class="space-y-4">
            <div>
                <label for="assignProfessor" class="block text-sm font-medium text-gray-700 mb-1">Professor *</label>
                <select 
                    id="assignProfessor" 
                    name="professorId"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    <option value="">Select professor...</option>
                    ${professors.filter(p => p.isActive).map(prof =>
        `<option value="${prof.id}">${prof.name} (${prof.department?.name || 'N/A'})</option>`
    ).join('')}
                </select>
            </div>
            <div>
                <label for="assignCourse" class="block text-sm font-medium text-gray-700 mb-1">Course *</label>
                <select 
                    id="assignCourse" 
                    name="courseId"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    <option value="">Select course...</option>
                    ${courses.filter(c => c.isActive).map(course =>
        `<option value="${course.id}">${course.courseCode} - ${course.courseName}</option>`
    ).join('')}
                </select>
            </div>
            <div class="bg-gray-50 p-3 rounded-md">
                <p class="text-sm text-gray-700">
                    <strong>Academic Year:</strong> ${selectedAcademicYear?.yearCode || 'N/A'}<br>
                    <strong>Semester:</strong> ${semester.type}
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
                onClick: (close) => handleCreateAssignment(selectedSemesterId, close)
            }
        ]
    });
}

/**
 * Handle create assignment
 */
async function handleCreateAssignment(semesterId, closeModal) {
    const form = document.getElementById('assignmentForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const data = {
        semesterId: semesterId,
        courseId: parseInt(document.getElementById('assignCourse').value),
        professorId: parseInt(document.getElementById('assignProfessor').value)
    };

    try {
        await apiRequest('/deanship/course-assignments', {
            method: 'POST',
            body: JSON.stringify(data)
        });

        showToast('Course assigned successfully', 'success');
        closeModal();
        loadAssignments();

        // Refresh File Explorer if on file-explorer tab and context is set
        if (currentTab === 'file-explorer' && fileExplorerState.hasContext() && fileExplorerInstance) {
            await fileExplorerInstance.loadRoot(selectedAcademicYearId, selectedSemesterId, true);
            showToast('Course folders created', 'info');
        }
    } catch (error) {
        console.error('Error creating assignment:', error);
        showToast(getErrorMessage(error), 'error');
    }
}

/**
 * Unassign course
 */
window.deanship.unassignCourse = function (assignmentId) {
    const assignment = assignments.find(a => a.id === assignmentId);
    if (!assignment) return;

    showConfirm(
        'Unassign Course',
        `Are you sure you want to unassign ${assignment.course?.courseCode} from ${assignment.professor?.name}?`,
        async () => {
            try {
                await apiRequest(`/deanship/course-assignments/${assignmentId}`, {
                    method: 'DELETE'
                });

                showToast('Course unassigned successfully', 'success');
                loadAssignments();
            } catch (error) {
                console.error('Error unassigning course:', error);
                showToast(getErrorMessage(error), 'error');
            }
        },
        { danger: true }
    );
};

// ============================================================================
// REPORTS
// ============================================================================

/**
 * Initialize interactive reports dashboard
 */
function initializeReportsDashboard() {
    try {
        if (typeof window.ReportsDashboard === 'undefined') {
            console.warn('ReportsDashboard class not found. Reports module may not be loaded.');
            showToast('Reports module unavailable', 'warning');
            return;
        }

        if (!reportsDashboardInstance) {
            reportsDashboardInstance = new window.ReportsDashboard('reports-dashboard-container');
        }
        reportsDashboardInstance.init();
    } catch (error) {
        console.error('Error initializing reports dashboard:', error);
        showToast('Failed to initialize reports', 'error');
    }
}

/**
 * Load system report (legacy - kept for backward compatibility)
 */
async function loadSystemReport() {
    if (!selectedSemesterId) {
        showToast('Please select an academic year and semester first', 'warning');
        return;
    }

    try {
        const report = await apiRequest(`/deanship/reports/system-wide?semesterId=${selectedSemesterId}`, {
            method: 'GET'
        });

        renderSystemReport(report);
    } catch (error) {
        console.error('Error loading system report:', error);
        showToast('Failed to load system report', 'error');
    }
}

/**
 * Render system report
 */
function renderSystemReport(report) {
    const reportDisplay = document.getElementById('reportDisplay');
    const reportContent = document.getElementById('reportContent');

    if (!report || !report.departmentReports || report.departmentReports.length === 0) {
        reportContent.innerHTML = `
            <div class="text-center py-8 text-gray-500">
                No data available for this semester
            </div>
        `;
        reportDisplay.classList.remove('hidden');
        return;
    }

    let html = `
        <div class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div class="bg-blue-50 p-4 rounded-lg">
                    <p class="text-sm text-gray-600">Total Professors</p>
                    <p class="text-2xl font-bold text-blue-600">${report.totalProfessors || 0}</p>
                </div>
                <div class="bg-green-50 p-4 rounded-lg">
                    <p class="text-sm text-gray-600">Submitted</p>
                    <p class="text-2xl font-bold text-green-600">${report.totalSubmitted || 0}</p>
                </div>
                <div class="bg-yellow-50 p-4 rounded-lg">
                    <p class="text-sm text-gray-600">Missing</p>
                    <p class="text-2xl font-bold text-yellow-600">${report.totalMissing || 0}</p>
                </div>
                <div class="bg-red-50 p-4 rounded-lg">
                    <p class="text-sm text-gray-600">Overdue</p>
                    <p class="text-2xl font-bold text-red-600">${report.totalOverdue || 0}</p>
                </div>
            </div>
    `;

    // Department breakdown
    html += `
        <div>
            <h4 class="text-lg font-medium text-gray-900 mb-3">Department Breakdown</h4>
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Department</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Professors</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Submitted</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Missing</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Overdue</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white divide-y divide-gray-200">
    `;

    report.departmentReports.forEach(dept => {
        html += `
            <tr>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${dept.departmentName}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${dept.professorCount || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-green-600">${dept.submittedCount || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-yellow-600">${dept.missingCount || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-red-600">${dept.overdueCount || 0}</td>
            </tr>
        `;
    });

    html += `
                    </tbody>
                </table>
            </div>
        </div>
    </div>
    `;

    reportContent.innerHTML = html;
    reportDisplay.classList.remove('hidden');
}

// ============================================================================
// FILE EXPLORER
// ============================================================================

/**
 * Initialize file explorer component
 * Uses the unified FileExplorer class with Deanship-specific configuration
 */
function initializeFileExplorer() {
    try {
        // Restore saved state to prevent flash
        const savedHtml = localStorage.getItem('deanship_file_explorer_html');
        const container = document.getElementById('fileExplorerContainer');
        if (savedHtml && container) {
            container.innerHTML = savedHtml;
        }

        fileExplorerInstance = new FileExplorer('fileExplorerContainer', {
            role: 'DEANSHIP',
            readOnly: true,
            showAllDepartments: true,
            showProfessorLabels: true
        });

        // Make it globally accessible for event handlers
        window.fileExplorerInstance = fileExplorerInstance;
    } catch (error) {
        console.error('Error initializing file explorer:', error);
        showToast('Failed to initialize file explorer', 'error');
    }
}

/**
 * Load file explorer for selected semester
 */
async function loadFileExplorer() {
    const container = document.getElementById('fileExplorerContainer');

    if (!selectedAcademicYearId || !selectedSemesterId || !fileExplorerInstance) {
        if (container) {
            // Only show empty state if container is truly empty (no restored content)
            if (!container.innerHTML.trim()) {
                container.innerHTML = `
                    <div class="flex flex-col items-center justify-center py-16 px-4">
                        <div class="text-center max-w-md">
                            <svg class="mx-auto h-20 w-20 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                            </svg>
                            <h3 class="text-lg font-semibold text-gray-700 mb-2">No Academic Context Selected</h3>
                            <p class="text-sm text-gray-600 mb-4">To explore files and folders, please select an <strong>Academic Year</strong> and <strong>Semester</strong> from the dropdown filters at the top of the page.</p>
                            <div class="inline-flex items-center text-xs text-gray-500 bg-gray-50 px-3 py-2 rounded-lg">
                                <svg class="w-4 h-4 mr-2 text-blue-500" fill="currentColor" viewBox="0 0 20 20">
                                    <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"></path>
                                </svg>
                                <span>You have read-only access to all departments</span>
                            </div>
                        </div>
                    </div>
                `;
            }
        }
        return;
    }

    try {
        // Ensure the file explorer structure exists
        if (container && !container.querySelector('#fileExplorerTree')) {
            console.log('Restoring file explorer structure...');
            fileExplorerInstance.render();
        }

        // Update FileExplorerState context
        const semester = semesters.find(s => s.id === selectedSemesterId);
        fileExplorerState.setContext(
            selectedAcademicYearId,
            selectedSemesterId,
            selectedAcademicYear?.yearCode || '',
            semester?.type || ''
        );

        // Check if we have existing content to decide on background update
        const hasContent = container && container.querySelector('#fileExplorerTree') &&
            container.querySelector('#fileExplorerTree').children.length > 0;

        // Use background update if we already have content (e.g. restored from storage)
        await fileExplorerInstance.loadRoot(selectedAcademicYearId, selectedSemesterId, hasContent);

        // Save state after successful load
        if (container) {
            localStorage.setItem('deanship_file_explorer_html', container.innerHTML);
        }
    } catch (error) {
        console.error('Error loading file explorer:', error);

        // Determine error type and show appropriate message
        let errorTitle = 'Unable to Load Files';
        let errorMessage = 'An error occurred while loading the file explorer.';
        let actionMessage = 'Please try selecting a different semester or refresh the page.';

        if (error.message && error.message.includes('not found')) {
            errorTitle = 'No Data Available';
            errorMessage = 'No files or folders exist for this semester yet.';
            actionMessage = 'Files will appear here once professors upload their documents.';
        } else if (error.message && error.message.includes('network')) {
            errorTitle = 'Connection Error';
            errorMessage = 'Unable to connect to the server.';
            actionMessage = 'Please check your internet connection and try again.';
        } else if (error.message && error.message.includes('permission')) {
            errorTitle = 'Access Denied';
            errorMessage = 'You don\'t have permission to access this content.';
            actionMessage = 'Please contact your system administrator if you believe this is an error.';
        }

        // Show user-friendly error message
        showToast(errorMessage, 'error');

        // Render error state in container
        if (container) {
            container.innerHTML = `
                <div class="flex flex-col items-center justify-center py-16 px-4">
                    <div class="text-center max-w-md">
                        <svg class="mx-auto h-20 w-20 text-red-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                        </svg>
                        <h3 class="text-lg font-semibold text-gray-800 mb-2">${errorTitle}</h3>
                        <p class="text-sm text-gray-600 mb-3">${errorMessage}</p>
                        <p class="text-xs text-gray-500 mb-4">${actionMessage}</p>
                        <button onclick="window.location.reload()" class="inline-flex items-center px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg transition-colors">
                            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                            </svg>
                            Refresh Page
                        </button>
                    </div>
                </div>
            `;
        }
    }
}


// ===================================
// Enhanced Table Features Integration
// ===================================

/**
 * Table Enhancement Manager
 * Manages advanced table features like filtering, bulk actions, and visual enhancements
 */
class TableEnhancementManager {
    constructor() {
        this.selectedRows = new Map(); // Map of table -> Set of selected IDs
        this.filters = new Map(); // Map of table -> filter values
        this.bulkToolbars = new Map(); // Map of table -> BulkActionsToolbar instance
    }

    /**
     * Enhance professors table with advanced features
     */
    enhanceProfessorsTable() {
        // Check for required dependencies
        if (typeof MultiSelectFilter === 'undefined' || typeof UserAvatar === 'undefined') {
            console.warn('Enhanced table components (MultiSelectFilter, UserAvatar) not found. Falling back to standard table.');
            renderProfessorsTable();
            return;
        }

        const tableContainer = document.getElementById('professorsTableContainer');
        if (!tableContainer) return;

        try {
            // Only create the table wrapper, keep the existing search/filter controls
            const filtersHtml = `
                <div class="enhanced-table-container">
                    <div id="professorsTableWrapper"></div>
                </div>
            `;

            tableContainer.innerHTML = filtersHtml;

            // Re-render table with enhancements
            this.renderEnhancedProfessorsTable();
        } catch (error) {
            console.error('Error enhancing professors table:', error);
            // Fallback
            renderProfessorsTable();
        }
    }

    /**
     * Render enhanced professors table with selection and avatars
     */
    renderEnhancedProfessorsTable() {
        const wrapper = document.getElementById('professorsTableWrapper');
        if (!wrapper) return;

        const filteredProfessors = this.getFilteredProfessors();

        if (filteredProfessors.length === 0) {
            wrapper.innerHTML = EmptyState.generate(
                'No professors found',
                'Try adjusting your filters or add a new professor',
                'Add Professor',
                () => showAddProfessorModal()
            );
            return;
        }

        const tableHtml = `
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Professor</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Department</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white divide-y divide-gray-200">
                        ${filteredProfessors.map(prof => this.renderProfessorRow(prof)).join('')}
                    </tbody>
                </table>
            </div>
        `;

        wrapper.innerHTML = tableHtml;
    }

    /**
     * Render a single professor row with avatar and selection
     */
    renderProfessorRow(professor) {
        const avatar = UserAvatar.generate(professor.name, 'md');
        const statusClass = professor.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800';
        const statusText = professor.isActive ? 'Active' : 'Inactive';

        return `
            <tr data-id="${professor.id}">
                <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center gap-3">
                        ${avatar}
                        <div>
                            <div class="text-sm font-medium text-gray-900">${professor.name}</div>
                        </div>
                    </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${professor.email}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${professor.department?.name || 'N/A'}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${statusClass}">
                        ${statusText}
                    </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                    <button class="text-blue-600 hover:text-blue-900" onclick="window.deanship.editProfessor('${professor.id}')" title="Edit professor">
                        <i class="fas fa-edit"></i>
                    </button>
                    ${professor.isActive ? `
                        <button class="text-red-600 hover:text-red-900" onclick="window.deanship.deactivateProfessor('${professor.id}', this)" title="Deactivate professor">
                            <i class="fas fa-ban"></i>
                        </button>
                    ` : `
                        <button class="text-green-600 hover:text-green-900" onclick="window.deanship.activateProfessor('${professor.id}', this)" title="Activate professor">
                            <i class="fas fa-check"></i>
                        </button>
                    `}
                    <button class="text-red-600 hover:text-red-900" onclick="window.deanship.deleteProfessor('${professor.id}', this)" title="Delete professor">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    }



    /**
     * Enhance courses table with progress bars and bulk actions
     */
    enhanceCoursesTable() {
        // Check for required dependencies
        if (typeof MultiSelectFilter === 'undefined' || typeof BulkActionsToolbar === 'undefined' || typeof TableProgressBar === 'undefined') {
            console.warn('Enhanced table components not found. Falling back to standard table.');
            renderCoursesTable();
            return;
        }

        const tableContainer = document.getElementById('coursesTableContainer');
        if (!tableContainer) return;

        try {
            // Only replace the table content, not the filters
            const filtersHtml = `
                <div class="enhanced-table-container">
                    <div id="courseBulkToolbar"></div>
                    <div id="coursesTableWrapper"></div>
                </div>
            `;

            tableContainer.innerHTML = filtersHtml;

            // Initialize bulk actions toolbar
            const bulkToolbar = new BulkActionsToolbar();
            bulkToolbar.render(
                document.getElementById('courseBulkToolbar'),
                (action) => this.handleCourseBulkAction(action)
            );
            this.bulkToolbars.set('courses', bulkToolbar);

            this.renderEnhancedCoursesTable();
        } catch (error) {
            console.error('Error enhancing courses table:', error);
            renderCoursesTable();
        }
    }

    /**
     * Render enhanced courses table with progress bars
     */
    renderEnhancedCoursesTable() {
        const wrapper = document.getElementById('coursesTableWrapper');
        if (!wrapper) return;

        const filteredCourses = this.getFilteredCourses();
        const searchInput = document.getElementById('courseSearch');
        const searchTerm = searchInput ? searchInput.value.trim() : '';
        const isSearching = searchTerm.length > 0;

        if (filteredCourses.length === 0) {
            wrapper.innerHTML = EmptyState.generate(
                isSearching ? 'No Courses Found' : 'No Courses',
                isSearching 
                    ? 'No courses match your search criteria. Try adjusting your filters.'
                    : 'Get started by adding courses that professors can teach.'
            );
            return;
        }

        const tableHtml = `
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left">
                                <input type="checkbox" id="selectAllCourses" class="table-row-checkbox" />
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Course</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Professor</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Department</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Progress</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white divide-y divide-gray-200">
                        ${filteredCourses.map(course => this.renderCourseRow(course)).join('')}
                    </tbody>
                </table>
            </div>
        `;

        wrapper.innerHTML = tableHtml;
        this.attachCourseTableListeners();
        this.animateProgressBars();
    }

    /**
     * Render a single course row with progress bar
     */
    renderCourseRow(course) {
        const isSelected = this.isRowSelected('courses', course.id);
        const progress = this.calculateCourseProgress(course);
        const progressBar = TableProgressBar.generate(
            progress,
            `${progress}% of documents submitted`
        );

        return `
            <tr class="table-row-selectable ${isSelected ? 'selected' : ''}" data-id="${course.id}">
                <td class="px-6 py-4">
                    <input type="checkbox" class="row-checkbox table-row-checkbox" 
                           data-id="${course.id}" ${isSelected ? 'checked' : ''} />
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                    <div class="text-sm font-medium text-gray-900">${course.courseName}</div>
                    <div class="text-sm text-gray-500">${course.courseCode}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${course.professor?.name || course.professorName || 'N/A'}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${course.department?.name || 'N/A'}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${progressBar}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <button class="text-blue-600 hover:text-blue-900 mr-3" onclick="editCourse(${course.id})" title="Edit course">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="text-red-600 hover:text-red-900" onclick="deleteCourse(${course.id})" title="Delete course">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    }

    /**
     * Attach event listeners to course table
     */
    attachCourseTableListeners() {
        const selectAll = document.getElementById('selectAllCourses');
        if (selectAll) {
            selectAll.addEventListener('change', (e) => {
                const checkboxes = document.querySelectorAll('.row-checkbox');
                checkboxes.forEach(cb => {
                    cb.checked = e.target.checked;
                    const id = parseInt(cb.dataset.id);
                    if (e.target.checked) {
                        this.selectRow('courses', id);
                    } else {
                        this.deselectRow('courses', id);
                    }
                });
                this.updateBulkToolbar('courses');
            });
        }

        document.querySelectorAll('.row-checkbox').forEach(cb => {
            cb.addEventListener('change', (e) => {
                const id = parseInt(e.target.dataset.id);
                if (e.target.checked) {
                    this.selectRow('courses', id);
                } else {
                    this.deselectRow('courses', id);
                }
                this.updateBulkToolbar('courses');
            });
        });

        document.querySelectorAll('.table-row-selectable').forEach(row => {
            row.addEventListener('click', (e) => {
                if (e.target.type === 'checkbox' || e.target.closest('button')) return;
                const checkbox = row.querySelector('.row-checkbox');
                checkbox.checked = !checkbox.checked;
                checkbox.dispatchEvent(new Event('change'));
            });
        });
    }

    /**
     * Enhance assignments table with progress indicators
     */
    enhanceAssignmentsTable() {
        // This will be called when rendering assignments
        // Add progress bars to assignment rows
        const assignmentRows = document.querySelectorAll('#assignmentsTable tbody tr');
        assignmentRows.forEach(row => {
            const progressCell = row.querySelector('.assignment-progress');
            if (progressCell) {
                const percentage = parseInt(progressCell.dataset.progress || 0);
                const progressBar = TableProgressBar.generate(
                    percentage,
                    `${percentage}% completed`
                );
                progressCell.innerHTML = progressBar;
            }
        });
        this.animateProgressBars();
    }

    /**
     * Animate all progress bars in the current view
     */
    animateProgressBars() {
        setTimeout(() => {
            document.querySelectorAll('.table-progress-bar').forEach(bar => {
                TableProgressBar.animate(bar);
            });
        }, 100);
    }

    /**
     * Calculate course progress based on document submissions
     */
    calculateCourseProgress(course) {
        // Mock calculation - replace with actual logic
        if (course.documentsSubmitted && course.totalDocuments) {
            return Math.round((course.documentsSubmitted / course.totalDocuments) * 100);
        }
        // Random for demo purposes
        return Math.floor(Math.random() * 100);
    }

    /**
     * Get filtered professors based on active filters
     */
    getFilteredProfessors() {
        let filtered = [...professors];

        // Apply department filter from the original dropdown
        const deptFilter = document.getElementById('professorDepartmentFilter');
        if (deptFilter && deptFilter.value) {
            const selectedDeptId = parseInt(deptFilter.value);
            filtered = filtered.filter(p => p.department?.id === selectedDeptId);
        }

        // Also check MultiSelectFilter if available
        const multiDeptFilter = this.filters.get('professors-department');
        if (multiDeptFilter) {
            const selectedDepts = multiDeptFilter.getSelectedValues();
            if (selectedDepts.length > 0) {
                filtered = filtered.filter(p => selectedDepts.includes(p.department?.name));
            }
        }

        // Apply search filter
        const searchInput = document.getElementById('professorSearch');
        if (searchInput && searchInput.value.trim()) {
            const searchTerm = searchInput.value.toLowerCase().trim();
            filtered = filtered.filter(prof =>
                (prof.name && prof.name.toLowerCase().includes(searchTerm)) ||
                (prof.email && prof.email.toLowerCase().includes(searchTerm)) ||
                (prof.professorId && prof.professorId.toLowerCase().includes(searchTerm))
            );
        }

        return filtered;
    }

    /**
     * Get filtered courses based on active filters and search term
     */
    getFilteredCourses() {
        let filtered = [...courses];

        // Apply search filter from the search input
        const searchInput = document.getElementById('courseSearch');
        const searchTerm = searchInput ? searchInput.value.toLowerCase().trim() : '';
        if (searchTerm) {
            filtered = filtered.filter(course =>
                (course.courseCode && course.courseCode.toLowerCase().includes(searchTerm)) ||
                (course.courseName && course.courseName.toLowerCase().includes(searchTerm)) ||
                (course.department?.name && course.department.name.toLowerCase().includes(searchTerm))
            );
        }

        // Department filter is now handled server-side via loadCourses()
        // No need to apply client-side department filtering here

        return filtered;
    }

    /**
     * Apply professor filters and re-render table
     */
    applyProfessorFilters() {
        this.renderEnhancedProfessorsTable();
    }

    /**
     * Apply course filters and re-render table
     */
    applyCoursesFilters() {
        this.renderEnhancedCoursesTable();
    }



    /**
     * Handle bulk actions for courses
     */
    async handleCourseBulkAction(action) {
        const selectedIds = this.getSelectedRows('courses');

        if (action === 'clear') {
            this.clearSelection('courses');
            this.renderEnhancedCoursesTable();
            return;
        }

        if (selectedIds.length === 0) {
            showToast('No courses selected', 'warning');
            return;
        }

        try {
            switch (action) {
                case 'delete':
                    await this.bulkDeleteCourses(selectedIds);
                    showToast(`${selectedIds.length} course(s) deleted`, 'success');
                    break;
            }

            this.clearSelection('courses');
            await loadCourses();
            this.renderEnhancedCoursesTable();
        } catch (error) {
            showToast(`Bulk action failed: ${getErrorMessage(error)}`, 'error');
        }
    }



    /**
     * Bulk delete courses
     */
    async bulkDeleteCourses(ids) {
        const promises = ids.map(id =>
            apiRequest(`/api/deanship/courses/${id}`, { method: 'DELETE' })
        );
        await Promise.all(promises);
    }

    // Selection management methods
    selectRow(table, id) {
        if (!this.selectedRows.has(table)) {
            this.selectedRows.set(table, new Set());
        }
        this.selectedRows.get(table).add(id);
    }

    deselectRow(table, id) {
        if (this.selectedRows.has(table)) {
            this.selectedRows.get(table).delete(id);
        }
    }

    isRowSelected(table, id) {
        return this.selectedRows.has(table) && this.selectedRows.get(table).has(id);
    }

    getSelectedRows(table) {
        return this.selectedRows.has(table) ? Array.from(this.selectedRows.get(table)) : [];
    }

    clearSelection(table) {
        if (this.selectedRows.has(table)) {
            this.selectedRows.get(table).clear();
        }
    }

    updateBulkToolbar(table) {
        const toolbar = this.bulkToolbars.get(table);
        if (toolbar) {
            const count = this.getSelectedRows(table).length;
            toolbar.show(count);
        }
    }
}

// Initialize table enhancement manager
const tableEnhancementManager = new TableEnhancementManager();

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { tableEnhancementManager };
}
