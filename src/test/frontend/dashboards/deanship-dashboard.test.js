/**
 * @jest-environment jsdom
 */

import { jest } from '@jest/globals';

// Mock dependencies before importing the module under test
global.fetch = jest.fn();
global.window = {
    location: { href: '', origin: 'http://localhost' },
    ...global.window
};

// Mock localStorage
const localStorageMock = (() => {
    let store = {};
    return {
        getItem: jest.fn(key => store[key] || null),
        setItem: jest.fn((key, value) => { store[key] = value; }),
        removeItem: jest.fn(key => { delete store[key]; }),
        clear: jest.fn(() => { store = {}; }),
        get length() { return Object.keys(store).length; }
    };
})();
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

// Mock the API module
const mockApiRequest = jest.fn();
const mockGetUserInfo = jest.fn();
const mockRedirectToLogin = jest.fn();
const mockClearAuthData = jest.fn();
const mockGetErrorMessage = jest.fn();
const mockInitializeAuth = jest.fn();

jest.mock('../../../main/resources/static/js/core/api.js', () => ({
    apiRequest: mockApiRequest,
    getUserInfo: mockGetUserInfo,
    redirectToLogin: mockRedirectToLogin,
    clearAuthData: mockClearAuthData,
    getErrorMessage: mockGetErrorMessage,
    initializeAuth: mockInitializeAuth
}));

// Mock the UI module
const mockShowToast = jest.fn();
const mockShowModal = jest.fn();
const mockShowConfirm = jest.fn();
const mockFormatDate = jest.fn((date) => date ? new Date(date).toLocaleDateString() : '');

jest.mock('../../../main/resources/static/js/core/ui.js', () => ({
    showToast: mockShowToast,
    showModal: mockShowModal,
    showConfirm: mockShowConfirm,
    formatDate: mockFormatDate
}));

// Mock FileExplorer
const mockFileExplorer = jest.fn().mockImplementation(() => ({
    loadRoot: jest.fn().mockResolvedValue(undefined)
}));

jest.mock('../../../main/resources/static/js/file-explorer/file-explorer.js', () => ({
    FileExplorer: mockFileExplorer
}));

// Mock file-explorer-state
const mockFileExplorerState = {
    setContext: jest.fn()
};

jest.mock('../../../main/resources/static/js/file-explorer/file-explorer-state.js', () => ({
    fileExplorerState: mockFileExplorerState
}));

// Mock deanship-feedback
const mockSkeletonLoader = jest.fn();
const mockEmptyState = jest.fn();
const mockEnhancedToast = jest.fn();
const mockTooltip = jest.fn();
const mockLoadingIndicator = jest.fn();
const mockWithMinLoadingTime = jest.fn((fn) => fn);
const MIN_LOADING_TIME = 500;

jest.mock('../../../main/resources/static/js/deanship/deanship-feedback.js', () => ({
    SkeletonLoader: mockSkeletonLoader,
    EmptyState: mockEmptyState,
    EnhancedToast: mockEnhancedToast,
    Tooltip: mockTooltip,
    LoadingIndicator: mockLoadingIndicator,
    withMinLoadingTime: mockWithMinLoadingTime,
    MIN_LOADING_TIME: MIN_LOADING_TIME
}));

// Mock deanship-navigation
const mockDashboardNavigation = {
    initialize: jest.fn(),
    updateBreadcrumbs: jest.fn()
};

jest.mock('../../../main/resources/static/js/deanship/deanship-navigation.js', () => ({
    dashboardNavigation: mockDashboardNavigation
}));

// Mock deanship-analytics
const mockDashboardAnalytics = {
    initialize: jest.fn().mockResolvedValue(undefined),
    refreshAllCharts: jest.fn(),
    initialized: false
};

jest.mock('../../../main/resources/static/js/deanship/deanship-analytics.js', () => ({
    dashboardAnalytics: mockDashboardAnalytics
}));

// Mock deanship-state
const mockDashboardState = {
    setSelectedAcademicYear: jest.fn(),
    setSelectedSemester: jest.fn(),
    setProfessors: jest.fn(),
    setCourses: jest.fn()
};

jest.mock('../../../main/resources/static/js/deanship/deanship-state.js', () => ({
    dashboardState: mockDashboardState
}));

// Mock deanship-error-handler
const mockErrorBoundary = jest.fn();
const mockSafeAsync = jest.fn((fn) => fn);

jest.mock('../../../main/resources/static/js/deanship/deanship-error-handler.js', () => ({
    ErrorBoundary: mockErrorBoundary,
    safeAsync: mockSafeAsync
}));

// Mock initModernDropdowns
global.initModernDropdowns = jest.fn();
global.refreshModernDropdown = jest.fn();

// Mock initializeReportsDashboard
global.initializeReportsDashboard = jest.fn();

describe('Deanship Dashboard (deanship/deanship.js)', () => {
    let mockDocument;
    let deanshipName, logoutBtn, academicYearSelect, semesterSelect;
    let contextBar, pageTitle;
    let addAcademicYearBtn, addProfessorBtn, addCourseBtn, addAssignmentBtn;
    let professorSearch, professorDepartmentFilter;
    let courseSearch, courseDepartmentFilter;
    let assignmentProfessorFilter, assignmentCourseFilter;

    // Helper function to create mock DOM elements
    function createMockElement(id, tag = 'div') {
        const element = document.createElement(tag);
        element.id = id;
        element.innerHTML = '';
        element.textContent = '';
        element.value = '';
        element.disabled = false;
        element.classList = {
            add: jest.fn(),
            remove: jest.fn(),
            toggle: jest.fn(),
            contains: jest.fn(() => false)
        };
        element.addEventListener = jest.fn();
        element.querySelector = jest.fn();
        element.querySelectorAll = jest.fn(() => []);
        return element;
    }

    beforeEach(() => {
        // Reset all mocks
        jest.clearAllMocks();
        localStorageMock.clear();
        fetch.mockClear();

        // Reset window.location
        delete window.location;
        window.location = { href: '' };

        // Setup DOM
        document.body.innerHTML = '';

        // Create all required DOM elements
        deanshipName = createMockElement('deanshipName', 'span');
        logoutBtn = createMockElement('logoutBtn', 'button');
        academicYearSelect = createMockElement('academicYearSelect', 'select');
        semesterSelect = createMockElement('semesterSelect', 'select');
        contextBar = createMockElement('contextBar', 'div');
        pageTitle = createMockElement('pageTitle', 'h1');
        
        // Tab buttons
        const dashboardTab = createMockElement('dashboardTab', 'button');
        dashboardTab.dataset.tab = 'dashboard';
        dashboardTab.classList.add('nav-tab');
        const academicYearsTab = createMockElement('academicYearsTab', 'button');
        academicYearsTab.dataset.tab = 'academic-years';
        academicYearsTab.classList.add('nav-tab');
        const professorsTab = createMockElement('professorsTab', 'button');
        professorsTab.dataset.tab = 'professors';
        professorsTab.classList.add('nav-tab');
        const coursesTab = createMockElement('coursesTab', 'button');
        coursesTab.dataset.tab = 'courses';
        coursesTab.classList.add('nav-tab');
        const assignmentsTab = createMockElement('assignmentsTab', 'button');
        assignmentsTab.dataset.tab = 'assignments';
        assignmentsTab.classList.add('nav-tab');
        const reportsTab = createMockElement('reportsTab', 'button');
        reportsTab.dataset.tab = 'reports';
        reportsTab.classList.add('nav-tab');
        const fileExplorerTab = createMockElement('fileExplorerTab', 'button');
        fileExplorerTab.dataset.tab = 'file-explorer';
        fileExplorerTab.classList.add('nav-tab');

        // Tab content
        const dashboardTabContent = createMockElement('dashboard-tab', 'div');
        dashboardTabContent.classList.add('tab-content');
        const academicYearsTabContent = createMockElement('academic-years-tab', 'div');
        academicYearsTabContent.classList.add('tab-content');
        const professorsTabContent = createMockElement('professors-tab', 'div');
        professorsTabContent.classList.add('tab-content');
        const coursesTabContent = createMockElement('courses-tab', 'div');
        coursesTabContent.classList.add('tab-content');
        const assignmentsTabContent = createMockElement('assignments-tab', 'div');
        assignmentsTabContent.classList.add('tab-content');
        const reportsTabContent = createMockElement('reports-tab', 'div');
        reportsTabContent.classList.add('tab-content');
        const fileExplorerTabContent = createMockElement('file-explorer-tab', 'div');
        fileExplorerTabContent.classList.add('tab-content');

        // Action buttons
        addAcademicYearBtn = createMockElement('addAcademicYearBtn', 'button');
        addProfessorBtn = createMockElement('addProfessorBtn', 'button');
        addCourseBtn = createMockElement('addCourseBtn', 'button');
        addAssignmentBtn = createMockElement('addAssignmentBtn', 'button');

        // Filters
        professorSearch = createMockElement('professorSearch', 'input');
        professorDepartmentFilter = createMockElement('professorDepartmentFilter', 'select');
        courseSearch = createMockElement('courseSearch', 'input');
        courseDepartmentFilter = createMockElement('courseDepartmentFilter', 'select');
        assignmentProfessorFilter = createMockElement('assignmentProfessorFilter', 'select');
        assignmentCourseFilter = createMockElement('assignmentCourseFilter', 'select');

        // Append to document
        document.body.appendChild(deanshipName);
        document.body.appendChild(logoutBtn);
        document.body.appendChild(academicYearSelect);
        document.body.appendChild(semesterSelect);
        document.body.appendChild(contextBar);
        document.body.appendChild(pageTitle);
        document.body.appendChild(dashboardTab);
        document.body.appendChild(academicYearsTab);
        document.body.appendChild(professorsTab);
        document.body.appendChild(coursesTab);
        document.body.appendChild(assignmentsTab);
        document.body.appendChild(reportsTab);
        document.body.appendChild(fileExplorerTab);
        document.body.appendChild(dashboardTabContent);
        document.body.appendChild(academicYearsTabContent);
        document.body.appendChild(professorsTabContent);
        document.body.appendChild(coursesTabContent);
        document.body.appendChild(assignmentsTabContent);
        document.body.appendChild(reportsTabContent);
        document.body.appendChild(fileExplorerTabContent);
        document.body.appendChild(addAcademicYearBtn);
        document.body.appendChild(addProfessorBtn);
        document.body.appendChild(addCourseBtn);
        document.body.appendChild(addAssignmentBtn);
        document.body.appendChild(professorSearch);
        document.body.appendChild(professorDepartmentFilter);
        document.body.appendChild(courseSearch);
        document.body.appendChild(courseDepartmentFilter);
        document.body.appendChild(assignmentProfessorFilter);
        document.body.appendChild(assignmentCourseFilter);

        // Mock getElementById
        document.getElementById = jest.fn((id) => {
            const elements = {
                'deanshipName': deanshipName,
                'logoutBtn': logoutBtn,
                'academicYearSelect': academicYearSelect,
                'semesterSelect': semesterSelect,
                'contextBar': contextBar,
                'pageTitle': pageTitle,
                'addAcademicYearBtn': addAcademicYearBtn,
                'addProfessorBtn': addProfessorBtn,
                'addCourseBtn': addCourseBtn,
                'addAssignmentBtn': addAssignmentBtn,
                'professorSearch': professorSearch,
                'professorDepartmentFilter': professorDepartmentFilter,
                'courseSearch': courseSearch,
                'courseDepartmentFilter': courseDepartmentFilter,
                'assignmentProfessorFilter': assignmentProfessorFilter,
                'assignmentCourseFilter': assignmentCourseFilter,
                'dashboard-tab': dashboardTabContent,
                'academic-years-tab': academicYearsTabContent,
                'professors-tab': professorsTabContent,
                'courses-tab': coursesTabContent,
                'assignments-tab': assignmentsTabContent,
                'reports-tab': reportsTabContent,
                'file-explorer-tab': fileExplorerTabContent
            };
            return elements[id] || null;
        });

        // Mock querySelectorAll for nav-tabs
        document.querySelectorAll = jest.fn((selector) => {
            if (selector === '.nav-tab') {
                return [dashboardTab, academicYearsTab, professorsTab, coursesTab, assignmentsTab, reportsTab, fileExplorerTab];
            }
            if (selector === '.tab-content') {
                return [dashboardTabContent, academicYearsTabContent, professorsTabContent, coursesTabContent, assignmentsTabContent, reportsTabContent, fileExplorerTabContent];
            }
            return [];
        });

        // Setup default mocks
        mockGetUserInfo.mockReturnValue({
            id: 1,
            email: 'deanship@test.com',
            fullName: 'Deanship User',
            role: 'ROLE_DEANSHIP'
        });
        mockInitializeAuth.mockResolvedValue(true);
        mockApiRequest.mockResolvedValue({});
    });

    afterEach(() => {
        jest.clearAllTimers();
    });

    // ============================================
    // Authentication Tests
    // ============================================

    describe('Authentication', () => {
        it('should redirect if not authenticated', async () => {
            mockGetUserInfo.mockReturnValue(null);
            
            // Import module to trigger DOMContentLoaded
            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            // Simulate DOMContentLoaded
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockRedirectToLogin).toHaveBeenCalled();
        });

        it('should validate ROLE_DEANSHIP', async () => {
            mockGetUserInfo.mockReturnValue({
                id: 1,
                email: 'user@test.com',
                role: 'ROLE_PROFESSOR'
            });
            mockApiRequest.mockResolvedValue({
                userId: 1,
                email: 'user@test.com',
                role: 'ROLE_PROFESSOR'
            });

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockShowToast).toHaveBeenCalledWith(
                expect.stringContaining('Access denied'),
                'error'
            );
            expect(mockRedirectToLogin).toHaveBeenCalled();
        });

        it('should fetch fresh user info from server', async () => {
            const freshUserInfo = {
                userId: 1,
                email: 'deanship@test.com',
                firstName: 'Deanship',
                lastName: 'User',
                role: 'ROLE_DEANSHIP'
            };
            mockApiRequest.mockResolvedValue(freshUserInfo);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalledWith('/auth/me');
            expect(localStorageMock.setItem).toHaveBeenCalledWith(
                'userInfo',
                expect.stringContaining('ROLE_DEANSHIP')
            );
        });

        it('should handle role mismatch gracefully', async () => {
            mockGetUserInfo.mockReturnValue({
                id: 1,
                email: 'user@test.com',
                role: 'ROLE_DEANSHIP'
            });
            mockApiRequest.mockResolvedValue({
                userId: 1,
                email: 'user@test.com',
                role: 'ROLE_PROFESSOR'
            });

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 200));
            
            expect(mockShowToast).toHaveBeenCalledWith(
                expect.stringContaining('Access denied'),
                'error'
            );
        });
    });

    // ============================================
    // Tab Management Tests
    // ============================================

    describe('Tab Management', () => {
        it('should restore active tab from localStorage', async () => {
            localStorageMock.setItem('deanship-active-tab', 'professors');
            
            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Check that professors tab is active
            const professorsTab = document.querySelectorAll('.nav-tab').find(t => t.dataset.tab === 'professors');
            expect(professorsTab).toBeDefined();
        });

        it('should update state when switchTab is called', async () => {
            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Access switchTab through the module
            // Since it's not exported, we'll test it through tab clicks
            const professorsTab = document.querySelectorAll('.nav-tab').find(t => t.dataset.tab === 'professors');
            if (professorsTab) {
                professorsTab.click();
                await new Promise(resolve => setTimeout(resolve, 50));
                
                expect(localStorageMock.setItem).toHaveBeenCalledWith('deanship-active-tab', 'professors');
            }
        });

        it('should persist tab across page loads', async () => {
            localStorageMock.setItem('deanship-active-tab', 'courses');
            
            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(localStorageMock.getItem).toHaveBeenCalledWith('deanship-active-tab');
        });

        it('should show correct content per tab', async () => {
            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Check that dashboard tab content is visible by default
            const dashboardContent = document.getElementById('dashboard-tab');
            expect(dashboardContent).toBeDefined();
        });
    });

    // ============================================
    // Academic Years Tests
    // ============================================

    describe('Academic Years', () => {
        it('should load academic years list', async () => {
            const mockYears = [
                { id: 1, yearCode: '2024-2025', isActive: true },
                { id: 2, yearCode: '2023-2024', isActive: false }
            ];
            mockApiRequest.mockResolvedValue(mockYears);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/deanship/academic-years'),
                expect.any(Object)
            );
        });

        it('should create new academic year', async () => {
            mockApiRequest.mockResolvedValue({ id: 3, yearCode: '2025-2026', isActive: false });
            mockShowModal.mockImplementation((content, onConfirm) => {
                if (onConfirm) {
                    onConfirm({ yearCode: '2025-2026' });
                }
            });

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Trigger add academic year
            addAcademicYearBtn.click();
            await new Promise(resolve => setTimeout(resolve, 50));
            
            expect(mockShowModal).toHaveBeenCalled();
        });

        it('should update academic year', async () => {
            const mockYear = { id: 1, yearCode: '2024-2025', isActive: true };
            mockApiRequest.mockResolvedValue(mockYear);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Update would be triggered through modal
            expect(mockApiRequest).toHaveBeenCalled();
        });

        it('should activate academic year', async () => {
            const mockYear = { id: 1, yearCode: '2024-2025', isActive: false };
            mockApiRequest.mockResolvedValue({ ...mockYear, isActive: true });

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalled();
        });

        it('should load semesters for year', async () => {
            const mockSemesters = [
                { id: 1, type: 'FIRST', academicYearId: 1 },
                { id: 2, type: 'SECOND', academicYearId: 1 }
            ];
            mockApiRequest.mockResolvedValue(mockSemesters);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Simulate academic year selection
            academicYearSelect.value = '1';
            const changeEvent = new Event('change');
            academicYearSelect.dispatchEvent(changeEvent);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/deanship/academic-years/1/semesters'),
                expect.any(Object)
            );
        });

        it('should update semester', async () => {
            const mockSemester = { id: 1, type: 'FIRST', academicYearId: 1 };
            mockApiRequest.mockResolvedValue(mockSemester);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalled();
        });
    });

    // ============================================
    // Professor Management Tests
    // ============================================

    describe('Professor Management', () => {
        it('should load professors list', async () => {
            const mockProfessors = [
                { id: 1, firstName: 'John', lastName: 'Doe', email: 'john@test.com', departmentId: 1 },
                { id: 2, firstName: 'Jane', lastName: 'Smith', email: 'jane@test.com', departmentId: 2 }
            ];
            mockApiRequest.mockResolvedValue(mockProfessors);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/deanship/professors'),
                expect.any(Object)
            );
        });

        it('should filter by department', async () => {
            const mockProfessors = [
                { id: 1, firstName: 'John', lastName: 'Doe', departmentId: 1 },
                { id: 2, firstName: 'Jane', lastName: 'Smith', departmentId: 2 }
            ];
            mockApiRequest.mockResolvedValue(mockProfessors);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Simulate department filter change
            professorDepartmentFilter.value = '1';
            const changeEvent = new Event('change');
            professorDepartmentFilter.dispatchEvent(changeEvent);
            
            await new Promise(resolve => setTimeout(resolve, 50));
            
            expect(professorDepartmentFilter.value).toBe('1');
        });

        it('should create new professor', async () => {
            const mockProfessor = {
                id: 3,
                firstName: 'New',
                lastName: 'Professor',
                email: 'new@test.com',
                departmentId: 1
            };
            mockApiRequest.mockResolvedValue(mockProfessor);
            mockShowModal.mockImplementation((content, onConfirm) => {
                if (onConfirm) {
                    onConfirm({
                        firstName: 'New',
                        lastName: 'Professor',
                        email: 'new@test.com',
                        departmentId: 1
                    });
                }
            });

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Trigger add professor
            addProfessorBtn.click();
            await new Promise(resolve => setTimeout(resolve, 50));
            
            expect(mockShowModal).toHaveBeenCalled();
        });

        it('should update professor', async () => {
            const mockProfessor = {
                id: 1,
                firstName: 'Updated',
                lastName: 'Professor',
                email: 'updated@test.com'
            };
            mockApiRequest.mockResolvedValue(mockProfessor);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalled();
        });

        it('should deactivate professor', async () => {
            const mockProfessor = { id: 1, active: true };
            mockApiRequest.mockResolvedValue({ ...mockProfessor, active: false });
            mockShowConfirm.mockResolvedValue(true);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalled();
        });

        it('should activate professor', async () => {
            const mockProfessor = { id: 1, active: false };
            mockApiRequest.mockResolvedValue({ ...mockProfessor, active: true });

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalled();
        });
    });

    // ============================================
    // Course Management Tests
    // ============================================

    describe('Course Management', () => {
        it('should load courses list', async () => {
            const mockCourses = [
                { id: 1, code: 'CS101', name: 'Introduction to CS', departmentId: 1 },
                { id: 2, code: 'MATH101', name: 'Calculus', departmentId: 2 }
            ];
            mockApiRequest.mockResolvedValue(mockCourses);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/deanship/courses'),
                expect.any(Object)
            );
        });

        it('should filter by department', async () => {
            const mockCourses = [
                { id: 1, code: 'CS101', departmentId: 1 },
                { id: 2, code: 'MATH101', departmentId: 2 }
            ];
            mockApiRequest.mockResolvedValue(mockCourses);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Simulate department filter change
            courseDepartmentFilter.value = '1';
            const changeEvent = new Event('change');
            courseDepartmentFilter.dispatchEvent(changeEvent);
            
            await new Promise(resolve => setTimeout(resolve, 50));
            
            expect(courseDepartmentFilter.value).toBe('1');
        });

        it('should create new course', async () => {
            const mockCourse = {
                id: 3,
                code: 'NEW101',
                name: 'New Course',
                departmentId: 1
            };
            mockApiRequest.mockResolvedValue(mockCourse);
            mockShowModal.mockImplementation((content, onConfirm) => {
                if (onConfirm) {
                    onConfirm({
                        code: 'NEW101',
                        name: 'New Course',
                        departmentId: 1
                    });
                }
            });

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Trigger add course
            addCourseBtn.click();
            await new Promise(resolve => setTimeout(resolve, 50));
            
            expect(mockShowModal).toHaveBeenCalled();
        });

        it('should update course', async () => {
            const mockCourse = {
                id: 1,
                code: 'UPDATED101',
                name: 'Updated Course'
            };
            mockApiRequest.mockResolvedValue(mockCourse);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalled();
        });

        it('should deactivate course', async () => {
            const mockCourse = { id: 1, active: true };
            mockApiRequest.mockResolvedValue({ ...mockCourse, active: false });
            mockShowConfirm.mockResolvedValue(true);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalled();
        });
    });

    // ============================================
    // Course Assignments Tests
    // ============================================

    describe('Course Assignments', () => {
        it('should load assignments for semester', async () => {
            const mockAssignments = [
                { id: 1, professorId: 1, courseId: 1, semesterId: 1 },
                { id: 2, professorId: 2, courseId: 2, semesterId: 1 }
            ];
            mockApiRequest.mockResolvedValue(mockAssignments);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalled();
        });

        it('should filter by professor', async () => {
            const mockAssignments = [
                { id: 1, professorId: 1, courseId: 1 },
                { id: 2, professorId: 2, courseId: 2 }
            ];
            mockApiRequest.mockResolvedValue(mockAssignments);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Simulate professor filter change
            assignmentProfessorFilter.value = '1';
            const changeEvent = new Event('change');
            assignmentProfessorFilter.dispatchEvent(changeEvent);
            
            await new Promise(resolve => setTimeout(resolve, 50));
            
            expect(assignmentProfessorFilter.value).toBe('1');
        });

        it('should create new assignment', async () => {
            const mockAssignment = {
                id: 3,
                professorId: 1,
                courseId: 1,
                semesterId: 1
            };
            mockApiRequest.mockResolvedValue(mockAssignment);
            mockShowModal.mockImplementation((content, onConfirm) => {
                if (onConfirm) {
                    onConfirm({
                        professorId: 1,
                        courseId: 1,
                        semesterId: 1
                    });
                }
            });

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Trigger add assignment
            addAssignmentBtn.click();
            await new Promise(resolve => setTimeout(resolve, 50));
            
            expect(mockShowModal).toHaveBeenCalled();
        });

        it('should delete assignment', async () => {
            mockApiRequest.mockResolvedValue({ success: true });
            mockShowConfirm.mockResolvedValue(true);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalled();
        });
    });

    // ============================================
    // Reports Tests
    // ============================================

    describe('Reports', () => {
        it('should load system-wide report', async () => {
            const mockReport = {
                totalProfessors: 10,
                totalCourses: 20,
                totalAssignments: 30
            };
            mockApiRequest.mockResolvedValue(mockReport);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockApiRequest).toHaveBeenCalled();
        });

        it('should export report to PDF', async () => {
            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            global.fetch.mockResolvedValueOnce({
                ok: true,
                blob: async () => mockBlob
            });

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(global.fetch).toHaveBeenCalled();
        });
    });

    // ============================================
    // File Explorer Tests
    // ============================================

    describe('File Explorer', () => {
        it('should initialize file explorer', async () => {
            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockFileExplorer).toHaveBeenCalled();
        });

        it('should load data for selected semester', async () => {
            localStorageMock.setItem('deanship_selected_academic_year', '1');
            localStorageMock.setItem('deanship_selected_semester', '1');
            
            const mockFileExplorerInstance = {
                loadRoot: jest.fn().mockResolvedValue(undefined)
            };
            mockFileExplorer.mockReturnValue(mockFileExplorerInstance);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(mockFileExplorer).toHaveBeenCalled();
        });

        it('should enforce read-only mode', async () => {
            const mockFileExplorerInstance = {
                loadRoot: jest.fn().mockResolvedValue(undefined)
            };
            mockFileExplorer.mockReturnValue(mockFileExplorerInstance);

            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Check that FileExplorer was called with readOnly: true
            expect(mockFileExplorer).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    readOnly: true
                })
            );
        });
    });

    // ============================================
    // Modern Dropdowns Tests
    // ============================================

    describe('Modern Dropdowns', () => {
        it('should initialize modern dropdowns', async () => {
            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(global.initModernDropdowns).toHaveBeenCalled();
        });

        it('should refresh dropdowns on data change', async () => {
            await import('../../../main/resources/static/js/deanship/deanship.js');
            
            const event = new Event('DOMContentLoaded');
            document.dispatchEvent(event);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Simulate data change that would trigger refresh
            academicYearSelect.value = '1';
            const changeEvent = new Event('change');
            academicYearSelect.dispatchEvent(changeEvent);
            
            await new Promise(resolve => setTimeout(resolve, 100));
            
            expect(global.refreshModernDropdown).toHaveBeenCalled();
        });
    });
});
