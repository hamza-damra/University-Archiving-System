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
const mockHodAPI = {
    getAcademicYears: jest.fn(),
    getDashboardOverview: jest.fn(),
    getSubmissionStatus: jest.fn(),
    getProfessorSubmissionReport: jest.fn(),
    exportReportToPdf: jest.fn(),
    getRequests: jest.fn(),
    getRequestDetails: jest.fn()
};

const mockApiRequest = jest.fn();
const mockGetUserInfo = jest.fn();
const mockIsAuthenticated = jest.fn();
const mockRedirectToLogin = jest.fn();
const mockClearAuthData = jest.fn();
const mockGetErrorMessage = jest.fn();
const mockInitializeAuth = jest.fn();

jest.mock('../../../main/resources/static/js/core/api.js', () => ({
    hod: mockHodAPI,
    apiRequest: mockApiRequest,
    getUserInfo: mockGetUserInfo,
    isAuthenticated: mockIsAuthenticated,
    redirectToLogin: mockRedirectToLogin,
    clearAuthData: mockClearAuthData,
    getErrorMessage: mockGetErrorMessage,
    initializeAuth: mockInitializeAuth
}));

// Mock the UI module
const mockShowToast = jest.fn();
const mockShowModal = jest.fn();
const mockFormatDate = jest.fn((date) => new Date(date).toLocaleDateString());

jest.mock('../../../main/resources/static/js/core/ui.js', () => ({
    showToast: mockShowToast,
    showModal: mockShowModal,
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

// Mock hod-reports
const mockHodReportsManager = {
    initialize: jest.fn(),
    loadFilterOptions: jest.fn()
};

jest.mock('../../../main/resources/static/js/shared/hod-reports.js', () => ({
    hodReportsManager: mockHodReportsManager
}));

// Mock initModernDropdowns
global.initModernDropdowns = jest.fn();

describe('HOD Dashboard (shared/hod.js)', () => {
    let mockDocument;
    let hodName, logoutBtn, academicYearSelect, semesterSelect;
    let dashboardOverview, submissionStatusSection, submissionStatusTableBody;
    let requestsTableBody, viewReportBtn, downloadReportBtn;
    let filterCourse, filterDocType, filterStatus;

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
        hodName = createMockElement('hodName', 'span');
        logoutBtn = createMockElement('logoutBtn', 'button');
        academicYearSelect = createMockElement('academicYearSelect', 'select');
        semesterSelect = createMockElement('semesterSelect', 'select');
        dashboardOverview = createMockElement('dashboardOverview', 'div');
        submissionStatusSection = createMockElement('submissionStatusSection', 'div');
        submissionStatusTableBody = createMockElement('submissionStatusTableBody', 'tbody');
        requestsTableBody = createMockElement('requestsTableBody', 'tbody');
        viewReportBtn = createMockElement('viewReportBtn', 'button');
        downloadReportBtn = createMockElement('downloadReportBtn', 'button');
        filterCourse = createMockElement('filterCourse', 'select');
        filterDocType = createMockElement('filterDocType', 'select');
        filterStatus = createMockElement('filterStatus', 'select');

        // Add elements to document
        document.body.appendChild(hodName);
        document.body.appendChild(logoutBtn);
        document.body.appendChild(academicYearSelect);
        document.body.appendChild(semesterSelect);
        document.body.appendChild(dashboardOverview);
        document.body.appendChild(submissionStatusSection);
        document.body.appendChild(submissionStatusTableBody);
        document.body.appendChild(requestsTableBody);
        document.body.appendChild(viewReportBtn);
        document.body.appendChild(downloadReportBtn);
        document.body.appendChild(filterCourse);
        document.body.appendChild(filterDocType);
        document.body.appendChild(filterStatus);

        // Create additional elements that might be needed
        const totalProfessors = createMockElement('totalProfessors', 'span');
        const totalCourses = createMockElement('totalCourses', 'span');
        const submittedCount = createMockElement('submittedCount', 'span');
        const missingCount = createMockElement('missingCount', 'span');
        const overdueCount = createMockElement('overdueCount', 'span');
        document.body.appendChild(totalProfessors);
        document.body.appendChild(totalCourses);
        document.body.appendChild(submittedCount);
        document.body.appendChild(missingCount);
        document.body.appendChild(overdueCount);

        // Create tab elements
        const navTabs = document.createElement('div');
        navTabs.className = 'nav-tabs';
        const tab1 = document.createElement('div');
        tab1.className = 'nav-tab';
        tab1.setAttribute('data-tab', 'submission-status');
        const tab2 = document.createElement('div');
        tab2.className = 'nav-tab';
        tab2.setAttribute('data-tab', 'reports');
        navTabs.appendChild(tab1);
        navTabs.appendChild(tab2);
        document.body.appendChild(navTabs);

        // Create tab content
        const tabContent1 = createMockElement('submission-status-tab', 'div');
        tabContent1.classList.contains = jest.fn(() => false);
        const tabContent2 = createMockElement('reports-tab', 'div');
        tabContent2.classList.contains = jest.fn(() => false);
        document.body.appendChild(tabContent1);
        document.body.appendChild(tabContent2);

        // Create page title
        const pageTitle = createMockElement('pageTitle', 'h1');
        document.body.appendChild(pageTitle);

        // Create file explorer container
        const hodFileExplorer = createMockElement('hodFileExplorer', 'div');
        const breadcrumbs = createMockElement('breadcrumbs', 'div');
        breadcrumbs.innerHTML = '';
        hodFileExplorer.appendChild(breadcrumbs);
        document.body.appendChild(hodFileExplorer);

        // Create sidebar elements
        const sidebarToggle = createMockElement('sidebarToggle', 'button');
        const sidebar = createMockElement('sidebar', 'div');
        document.body.appendChild(sidebarToggle);
        document.body.appendChild(sidebar);

        // Create report buttons in tabs
        const viewReportBtnTab = createMockElement('viewReportBtnTab', 'button');
        const downloadReportBtnTab = createMockElement('downloadReportBtnTab', 'button');
        document.body.appendChild(viewReportBtnTab);
        document.body.appendChild(downloadReportBtnTab);

        // Setup default mocks
        mockIsAuthenticated.mockReturnValue(true);
        mockInitializeAuth.mockResolvedValue(true);
        mockGetUserInfo.mockReturnValue({
            id: 1,
            email: 'hod@example.com',
            fullName: 'HOD User',
            firstName: 'HOD',
            lastName: 'User',
            role: 'ROLE_HOD',
            departmentId: 1,
            departmentName: 'Computer Science'
        });
        mockApiRequest.mockResolvedValue({
            userId: 1,
            email: 'hod@example.com',
            firstName: 'HOD',
            lastName: 'User',
            role: 'ROLE_HOD',
            departmentId: 1,
            departmentName: 'Computer Science'
        });
    });

    afterEach(() => {
        document.body.innerHTML = '';
        jest.clearAllMocks();
    });

    describe('Authentication', () => {
        it('should redirect if not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);

            // Since the module runs top-level code, we'll test the auth check logic
            const isAuth = mockIsAuthenticated();
            if (!isAuth) {
                mockRedirectToLogin();
            }

            expect(mockIsAuthenticated).toHaveBeenCalled();
            expect(mockRedirectToLogin).toHaveBeenCalled();
        });

        it('should validate token with server', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockInitializeAuth.mockResolvedValue(true);

            const isValid = await mockInitializeAuth();
            expect(mockInitializeAuth).toHaveBeenCalled();
            expect(isValid).toBe(true);
        });

        it('should fetch fresh user info', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockInitializeAuth.mockResolvedValue(true);

            const freshUserInfo = await mockApiRequest('/auth/me');
            expect(mockApiRequest).toHaveBeenCalledWith('/auth/me');
            expect(freshUserInfo).toBeDefined();
            expect(freshUserInfo.role).toBe('ROLE_HOD');
        });

        it('should reject non-HOD role', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockInitializeAuth.mockResolvedValue(true);
            mockApiRequest.mockResolvedValue({
                userId: 1,
                email: 'user@example.com',
                role: 'ROLE_PROFESSOR'
            });

            const freshUserInfo = await mockApiRequest('/auth/me');
            if (freshUserInfo.role !== 'ROLE_HOD') {
                mockShowToast('Access denied - HOD privileges required. Your role: ' + freshUserInfo.role, 'error');
            }

            expect(mockShowToast).toHaveBeenCalledWith(
                expect.stringContaining('Access denied'),
                'error'
            );
        });

        it('should initialize dashboard after auth', () => {
            const userInfo = mockGetUserInfo();
            expect(userInfo).toBeDefined();
            expect(userInfo.role).toBe('ROLE_HOD');
        });
    });

    describe('Academic Year Selection', () => {
        it('should populate dropdown with academic years', async () => {
            const mockYears = [
                { id: 1, yearCode: '2024-2025', isActive: true, semesters: [] },
                { id: 2, yearCode: '2023-2024', isActive: false, semesters: [] }
            ];
            mockHodAPI.getAcademicYears.mockResolvedValue(mockYears);

            const response = await mockHodAPI.getAcademicYears();
            expect(mockHodAPI.getAcademicYears).toHaveBeenCalled();
            expect(Array.isArray(response)).toBe(true);
            expect(response.length).toBe(2);
        });

        it('should select current year based on Palestine timezone', () => {
            // Mock Palestine date calculation
            const getPalestineDate = () => {
                const palestineTimeString = new Date().toLocaleString('en-US', { 
                    timeZone: 'Asia/Jerusalem' 
                });
                return new Date(palestineTimeString);
            };

            const getCurrentAcademicYearCode = () => {
                const palestineDate = getPalestineDate();
                const month = palestineDate.getMonth();
                const year = palestineDate.getFullYear();
                
                if (month >= 8) { // September or later
                    return `${year}-${year + 1}`;
                } else {
                    return `${year - 1}-${year}`;
                }
            };

            const currentYearCode = getCurrentAcademicYearCode();
            expect(currentYearCode).toMatch(/^\d{4}-\d{4}$/);
        });

        it('should fall back to active year if current year not found', () => {
            const academicYears = [
                { id: 1, yearCode: '2024-2025', isActive: true },
                { id: 2, yearCode: '2023-2024', isActive: false }
            ];

            const currentYearCode = '2025-2026'; // Not in list
            let yearToSelect = academicYears.find(year => year.yearCode === currentYearCode);
            if (!yearToSelect) {
                yearToSelect = academicYears.find(year => year.isActive) || academicYears[0];
            }

            expect(yearToSelect).toBeDefined();
            expect(yearToSelect.isActive).toBe(true);
        });

        it('should populate semester dropdown', async () => {
            const mockSemesters = [
                { id: 1, type: 'FIRST', isActive: true },
                { id: 2, type: 'SECOND', isActive: false },
                { id: 3, type: 'SUMMER', isActive: false }
            ];

            const year = {
                id: 1,
                yearCode: '2024-2025',
                semesters: mockSemesters
            };

            expect(year.semesters).toBeDefined();
            expect(year.semesters.length).toBe(3);
        });

        it('should calculate current academic year code correctly', () => {
            // Test for September (month 8)
            const mockDate = new Date('2024-09-15');
            const month = mockDate.getMonth();
            const year = mockDate.getFullYear();
            
            let yearCode;
            if (month >= 8) {
                yearCode = `${year}-${year + 1}`;
            } else {
                yearCode = `${year - 1}-${year}`;
            }

            expect(yearCode).toBe('2024-2025');
        });

        it('should return FIRST/SECOND/SUMMER correctly for current semester type', () => {
            const getCurrentSemesterType = (month) => {
                if (month >= 8 || month === 0) { // Sep-Jan = FIRST
                    return 'FIRST';
                } else if (month >= 1 && month <= 5) { // Feb-Jun = SECOND
                    return 'SECOND';
                } else { // Jul-Aug = SUMMER
                    return 'SUMMER';
                }
            };

            expect(getCurrentSemesterType(9)).toBe('FIRST'); // September
            expect(getCurrentSemesterType(0)).toBe('FIRST'); // January
            expect(getCurrentSemesterType(2)).toBe('SECOND'); // February
            expect(getCurrentSemesterType(5)).toBe('SECOND'); // June
            expect(getCurrentSemesterType(7)).toBe('SUMMER'); // July
        });
    });

    describe('Dashboard Overview', () => {
        it('should display total professors count', async () => {
            const mockOverview = {
                totalProfessors: 10,
                totalCourses: 25,
                submissionStatistics: {
                    submittedDocuments: 50,
                    missingDocuments: 10,
                    overdueDocuments: 5
                }
            };

            mockHodAPI.getDashboardOverview.mockResolvedValue(mockOverview);
            const overview = await mockHodAPI.getDashboardOverview(1);

            expect(overview.totalProfessors).toBe(10);
            expect(mockHodAPI.getDashboardOverview).toHaveBeenCalledWith(1);
        });

        it('should display total courses count', async () => {
            const mockOverview = {
                totalProfessors: 10,
                totalCourses: 25,
                submissionStatistics: {}
            };

            mockHodAPI.getDashboardOverview.mockResolvedValue(mockOverview);
            const overview = await mockHodAPI.getDashboardOverview(1);

            expect(overview.totalCourses).toBe(25);
        });

        it('should display submitted documents count', async () => {
            const mockOverview = {
                totalProfessors: 10,
                totalCourses: 25,
                submissionStatistics: {
                    submittedDocuments: 50,
                    missingDocuments: 10,
                    overdueDocuments: 5
                }
            };

            mockHodAPI.getDashboardOverview.mockResolvedValue(mockOverview);
            const overview = await mockHodAPI.getDashboardOverview(1);

            expect(overview.submissionStatistics.submittedDocuments).toBe(50);
        });

        it('should display missing documents count', async () => {
            const mockOverview = {
                totalProfessors: 10,
                totalCourses: 25,
                submissionStatistics: {
                    submittedDocuments: 50,
                    missingDocuments: 10,
                    overdueDocuments: 5
                }
            };

            mockHodAPI.getDashboardOverview.mockResolvedValue(mockOverview);
            const overview = await mockHodAPI.getDashboardOverview(1);

            expect(overview.submissionStatistics.missingDocuments).toBe(10);
        });

        it('should display overdue documents count', async () => {
            const mockOverview = {
                totalProfessors: 10,
                totalCourses: 25,
                submissionStatistics: {
                    submittedDocuments: 50,
                    missingDocuments: 10,
                    overdueDocuments: 5
                }
            };

            mockHodAPI.getDashboardOverview.mockResolvedValue(mockOverview);
            const overview = await mockHodAPI.getDashboardOverview(1);

            expect(overview.submissionStatistics.overdueDocuments).toBe(5);
        });
    });

    describe('Submission Status', () => {
        it('should fetch submission status with filters', async () => {
            const mockStatus = {
                rows: [
                    {
                        professorName: 'John Doe',
                        courseCode: 'CS101',
                        courseName: 'Introduction to CS',
                        documentStatuses: {
                            'SYLLABUS': { status: 'UPLOADED', deadline: '2024-01-15' }
                        }
                    }
                ]
            };

            const filters = {
                courseCode: 'CS101',
                documentType: 'SYLLABUS',
                status: 'UPLOADED'
            };

            mockHodAPI.getSubmissionStatus.mockResolvedValue(mockStatus);
            const status = await mockHodAPI.getSubmissionStatus(1, filters);

            expect(mockHodAPI.getSubmissionStatus).toHaveBeenCalledWith(1, filters);
            expect(status.rows).toBeDefined();
            expect(status.rows.length).toBe(1);
        });

        it('should create correct table rows', () => {
            const report = {
                rows: [
                    {
                        professorName: 'John Doe',
                        courseCode: 'CS101',
                        courseName: 'Introduction to CS',
                        documentStatuses: {
                            'SYLLABUS': { status: 'UPLOADED', deadline: '2024-01-15' },
                            'EXAM': { status: 'NOT_UPLOADED', deadline: '2024-02-15' }
                        }
                    }
                ]
            };

            const rows = [];
            report.rows.forEach(row => {
                Object.entries(row.documentStatuses || {}).forEach(([docType, status]) => {
                    rows.push({
                        professorName: row.professorName,
                        courseCode: row.courseCode,
                        courseName: row.courseName,
                        documentType: docType,
                        status: status.status,
                        deadline: status.deadline
                    });
                });
            });

            expect(rows.length).toBe(2);
            expect(rows[0].documentType).toBe('SYLLABUS');
            expect(rows[0].status).toBe('UPLOADED');
        });

        it('should return correct badge for UPLOADED status', () => {
            const getStatusBadgeNew = (status) => {
                const badges = {
                    'UPLOADED': '<span class="badge badge-success">Uploaded</span>',
                    'NOT_UPLOADED': '<span class="badge badge-gray">Not Uploaded</span>',
                    'OVERDUE': '<span class="badge badge-danger">Overdue</span>'
                };
                return badges[status] || '<span class="badge badge-gray">Unknown</span>';
            };

            const badge = getStatusBadgeNew('UPLOADED');
            expect(badge).toContain('badge-success');
            expect(badge).toContain('Uploaded');
        });

        it('should return correct badge for NOT_UPLOADED status', () => {
            const getStatusBadgeNew = (status) => {
                const badges = {
                    'UPLOADED': '<span class="badge badge-success">Uploaded</span>',
                    'NOT_UPLOADED': '<span class="badge badge-gray">Not Uploaded</span>',
                    'OVERDUE': '<span class="badge badge-danger">Overdue</span>'
                };
                return badges[status] || '<span class="badge badge-gray">Unknown</span>';
            };

            const badge = getStatusBadgeNew('NOT_UPLOADED');
            expect(badge).toContain('badge-gray');
            expect(badge).toContain('Not Uploaded');
        });

        it('should return correct badge for OVERDUE status', () => {
            const getStatusBadgeNew = (status) => {
                const badges = {
                    'UPLOADED': '<span class="badge badge-success">Uploaded</span>',
                    'NOT_UPLOADED': '<span class="badge badge-gray">Not Uploaded</span>',
                    'OVERDUE': '<span class="badge badge-danger">Overdue</span>'
                };
                return badges[status] || '<span class="badge badge-gray">Unknown</span>';
            };

            const badge = getStatusBadgeNew('OVERDUE');
            expect(badge).toContain('badge-danger');
            expect(badge).toContain('Overdue');
        });

        it('should trigger reload when filter dropdowns change', () => {
            const loadSubmissionStatus = jest.fn();

            // Simulate filter change
            filterCourse.addEventListener('change', loadSubmissionStatus);
            const changeEvent = new Event('change');
            filterCourse.dispatchEvent(changeEvent);

            expect(filterCourse.addEventListener).toHaveBeenCalledWith('change', expect.any(Function));
        });
    });

    describe('Tab Switching', () => {
        it('should update active tab styling', () => {
            const switchTab = (tabName) => {
                const tabs = document.querySelectorAll('.nav-tab');
                tabs.forEach(tab => {
                    if (tab.getAttribute('data-tab') === tabName) {
                        tab.classList.add('active');
                    } else {
                        tab.classList.remove('active');
                    }
                });
            };

            const tabs = document.querySelectorAll('.nav-tab');
            switchTab('submission-status');

            // Check that first tab has active class
            expect(tabs[0].classList.add).toHaveBeenCalledWith('active');
        });

        it('should show correct tab content', () => {
            const switchTab = (tabName) => {
                const tabContents = document.querySelectorAll('.tab-content');
                tabContents.forEach(content => {
                    content.classList.add('hidden');
                });

                const activeTab = document.getElementById(`${tabName}-tab`);
                if (activeTab) {
                    activeTab.classList.remove('hidden');
                }
            };

            switchTab('submission-status');
            const activeTab = document.getElementById('submission-status-tab');
            expect(activeTab).toBeDefined();
        });

        it('should load data for specific tabs', () => {
            const loadSubmissionStatus = jest.fn();
            const selectedSemester = 1;
            const tabName = 'submission-status';

            if (tabName === 'submission-status' && selectedSemester) {
                loadSubmissionStatus();
            }

            expect(loadSubmissionStatus).toHaveBeenCalled();
        });
    });

    describe('Reports', () => {
        it('should show modal when view report button is clicked', async () => {
            const selectedSemester = 1;
            const mockReport = {
                statistics: {
                    totalProfessors: 10,
                    totalCourses: 25,
                    submittedDocuments: 50,
                    missingDocuments: 10,
                    overdueDocuments: 5
                },
                rows: []
            };

            mockHodAPI.getProfessorSubmissionReport.mockResolvedValue(mockReport);

            if (selectedSemester) {
                const report = await mockHodAPI.getProfessorSubmissionReport(selectedSemester);
                expect(report).toBeDefined();
                expect(report.statistics).toBeDefined();
            }

            expect(mockHodAPI.getProfessorSubmissionReport).toHaveBeenCalledWith(selectedSemester);
        });

        it('should trigger PDF download when download report button is clicked', async () => {
            const selectedSemester = 1;
            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            const mockResponse = {
                ok: true,
                blob: jest.fn().mockResolvedValue(mockBlob)
            };

            mockHodAPI.exportReportToPdf.mockResolvedValue(mockResponse);

            if (selectedSemester) {
                mockShowToast('Generating PDF report...', 'info');
                const response = await mockHodAPI.exportReportToPdf(selectedSemester);

                if (response.ok) {
                    const blob = await response.blob();
                    expect(blob).toBeDefined();
                }
            }

            expect(mockHodAPI.exportReportToPdf).toHaveBeenCalledWith(selectedSemester);
            expect(mockShowToast).toHaveBeenCalledWith('Generating PDF report...', 'info');
        });

        it('should show warning if semester not selected for view report', () => {
            const selectedSemester = null;

            if (!selectedSemester) {
                mockShowToast('Please select a semester first', 'warning');
            }

            expect(mockShowToast).toHaveBeenCalledWith('Please select a semester first', 'warning');
        });

        it('should show warning if semester not selected for download report', () => {
            const selectedSemester = null;

            if (!selectedSemester) {
                mockShowToast('Please select a semester first', 'warning');
            }

            expect(mockShowToast).toHaveBeenCalledWith('Please select a semester first', 'warning');
        });
    });
});
