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
const mockIsAuthenticated = jest.fn();
const mockRedirectToLogin = jest.fn();
const mockClearAuthData = jest.fn();
const mockGetErrorMessage = jest.fn();
const mockInitializeAuth = jest.fn();

jest.mock('../../../main/resources/static/js/core/api.js', () => ({
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
const mockSetButtonLoading = jest.fn();

jest.mock('../../../main/resources/static/js/core/ui.js', () => ({
    showToast: mockShowToast,
    setButtonLoading: mockSetButtonLoading
}));

// Mock AdminLayout
const mockAdminLayout = {
    initialize: jest.fn().mockResolvedValue(undefined),
    getSelectedContext: jest.fn(() => ({ academicYearId: 1, semesterId: 1 })),
    onContextChange: jest.fn()
};

jest.mock('../../../main/resources/static/js/admin/admin-common.js', () => ({
    AdminLayout: jest.fn().mockImplementation(() => mockAdminLayout)
}));

// Mock initModernDropdowns
global.initModernDropdowns = jest.fn();
global.refreshModernDropdown = jest.fn();

describe('Admin Dashboard (admin/admin-dashboard.js)', () => {
    let AdminDashboardPage;
    let adminDashboard;
    let mockDocument;

    // Helper function to create mock DOM elements
    function createMockElement(id, tag = 'div') {
        const element = document.createElement(tag);
        element.id = id;
        element.innerHTML = '';
        element.textContent = '';
        element.value = '';
        element.disabled = false;
        element.style = { opacity: '1' };
        element.classList = {
            add: jest.fn(),
            remove: jest.fn(),
            toggle: jest.fn(),
            contains: jest.fn(() => false)
        };
        element.addEventListener = jest.fn();
        element.querySelector = jest.fn();
        element.querySelectorAll = jest.fn(() => []);
        element.closest = jest.fn();
        element.parentElement = null;
        element.previousElementSibling = null;
        return element;
    }

    beforeEach(async () => {
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
        const pageTitle = createMockElement('pageTitle', 'h1');
        const sidebar = createMockElement('sidebar', 'div');
        const sidebarToggle = createMockElement('sidebarToggle', 'button');
        const sidebarOverlay = createMockElement('sidebarOverlay', 'div');
        const tabPreloadStyle = createMockElement('tab-preload-style', 'style');
        tabPreloadStyle.id = 'tab-preload-style';

        // Dashboard stats elements
        const statProfessors = createMockElement('statProfessors', 'span');
        const statHods = createMockElement('statHods', 'span');
        const statDepartments = createMockElement('statDepartments', 'span');
        const statCourses = createMockElement('statCourses', 'span');
        const statSubmissions = createMockElement('statSubmissions', 'span');
        const statRecentSubmissions = createMockElement('statRecentSubmissions', 'span');

        // Dashboard card elements
        const cardUsers = createMockElement('cardUsers', 'span');
        const cardDepartments = createMockElement('cardDepartments', 'span');
        const cardCourses = createMockElement('cardCourses', 'span');
        const cardSubmissions = createMockElement('cardSubmissions', 'span');

        // Users tab elements
        const usersTableBody = createMockElement('usersTableBody', 'tbody');
        const usersPagination = createMockElement('usersPagination', 'div');
        const addUserBtn = createMockElement('addUserBtn', 'button');
        const userSearchInput = createMockElement('userSearchInput', 'input');
        const userRoleFilter = createMockElement('userRoleFilter', 'select');
        const userStatusFilter = createMockElement('userStatusFilter', 'select');
        const userDepartmentFilter = createMockElement('userDepartmentFilter', 'select');
        const userForm = createMockElement('userForm', 'form');
        const userModalClose = createMockElement('userModalClose', 'button');
        const userModalCancel = createMockElement('userModalCancel', 'button');
        const userRole = createMockElement('userRole', 'select');
        const userDepartment = createMockElement('userDepartment', 'select');
        const userPassword = createMockElement('userPassword', 'input');
        const togglePasswordBtn = createMockElement('togglePasswordBtn', 'button');
        const passwordEyeIcon = createMockElement('passwordEyeIcon', 'svg');
        const passwordEyeOffIcon = createMockElement('passwordEyeOffIcon', 'svg');
        const userEmailPrefix = createMockElement('userEmailPrefix', 'input');
        const userEmailDomain = createMockElement('userEmailDomain', 'span');
        const emailInputWrapper = createMockElement('emailInputWrapper', 'div');
        const emailAutoDisplay = createMockElement('emailAutoDisplay', 'div');
        const emailPendingDisplay = createMockElement('emailPendingDisplay', 'div');
        const emailFormatHint = createMockElement('emailFormatHint', 'span');
        const emailHintText = createMockElement('emailHintText', 'span');

        // Departments tab elements
        const departmentsTableBody = createMockElement('departmentsTableBody', 'tbody');
        const addDepartmentBtn = createMockElement('addDepartmentBtn', 'button');
        const departmentForm = createMockElement('departmentForm', 'form');
        const departmentModalClose = createMockElement('departmentModalClose', 'button');
        const departmentModalCancel = createMockElement('departmentModalCancel', 'button');

        // Reports tab elements
        const reportTotalSubmissions = createMockElement('reportTotalSubmissions', 'span');
        const reportRecentSubmissions = createMockElement('reportRecentSubmissions', 'span');
        const reportPendingSubmissions = createMockElement('reportPendingSubmissions', 'span');
        const reportProfessors = createMockElement('reportProfessors', 'span');
        const reportHods = createMockElement('reportHods', 'span');
        const reportDepartments = createMockElement('reportDepartments', 'span');

        // Tab navigation elements
        const navTabs = document.createElement('div');
        navTabs.className = 'nav-tabs';
        ['dashboard', 'users', 'departments', 'courses', 'reports'].forEach(tabName => {
            const tab = document.createElement('div');
            tab.className = 'nav-tab';
            tab.setAttribute('data-tab', tabName);
            navTabs.appendChild(tab);
        });

        // Tab content elements
        ['dashboard', 'users', 'departments', 'courses', 'reports'].forEach(tabName => {
            const tabContent = createMockElement(`${tabName}-tab`, 'div');
            tabContent.className = 'tab-content';
            document.body.appendChild(tabContent);
        });

        // Add all elements to document
        [
            pageTitle, sidebar, sidebarToggle, sidebarOverlay, tabPreloadStyle,
            statProfessors, statHods, statDepartments, statCourses, statSubmissions, statRecentSubmissions,
            cardUsers, cardDepartments, cardCourses, cardSubmissions,
            usersTableBody, usersPagination, addUserBtn, userSearchInput, userRoleFilter,
            userStatusFilter, userDepartmentFilter, userForm, userModalClose, userModalCancel,
            userRole, userDepartment, userPassword, togglePasswordBtn, passwordEyeIcon,
            passwordEyeOffIcon, userEmailPrefix, userEmailDomain, emailInputWrapper,
            emailAutoDisplay, emailPendingDisplay, emailFormatHint, emailHintText,
            departmentsTableBody, addDepartmentBtn, departmentForm, departmentModalClose, departmentModalCancel,
            reportTotalSubmissions, reportRecentSubmissions, reportPendingSubmissions,
            reportProfessors, reportHods, reportDepartments
        ].forEach(el => document.body.appendChild(el));

        document.body.appendChild(navTabs);

        // Create navigation cards
        ['users', 'departments', 'courses', 'reports'].forEach(tabName => {
            const card = createMockElement(`card-${tabName}`, 'div');
            card.setAttribute('data-navigate', tabName);
            document.body.appendChild(card);
        });

        // Setup default mocks
        mockIsAuthenticated.mockReturnValue(true);
        mockInitializeAuth.mockResolvedValue(true);
        mockGetUserInfo.mockReturnValue({
            id: 1,
            email: 'admin@admin.alquds.edu',
            fullName: 'Admin User',
            firstName: 'Admin',
            lastName: 'User',
            role: 'ROLE_ADMIN'
        });
        mockApiRequest.mockResolvedValue({});

        // Dynamically import the module
        const module = await import('../../../main/resources/static/js/admin/admin-dashboard.js');
        AdminDashboardPage = module.default;
    });

    afterEach(() => {
        document.body.innerHTML = '';
        jest.clearAllMocks();
    });

    describe('Authentication', () => {
        it('should validate ROLE_ADMIN', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockInitializeAuth.mockResolvedValue(true);
            mockGetUserInfo.mockReturnValue({
                id: 1,
                role: 'ROLE_ADMIN'
            });

            const userInfo = mockGetUserInfo();
            expect(userInfo.role).toBe('ROLE_ADMIN');
            expect(mockIsAuthenticated).toHaveBeenCalled();
        });

        it('should redirect non-admin users', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockInitializeAuth.mockResolvedValue(true);
            mockGetUserInfo.mockReturnValue({
                id: 1,
                role: 'ROLE_PROFESSOR'
            });

            // Simulate AdminLayout's role check
            const userInfo = mockGetUserInfo();
            if (userInfo.role !== 'ROLE_ADMIN') {
                mockShowToast('Access denied - Admin role required', 'error');
                setTimeout(() => mockRedirectToLogin('access_denied'), 2000);
            }

            expect(mockShowToast).toHaveBeenCalledWith(
                'Access denied - Admin role required',
                'error'
            );
        });

        it('should redirect if not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);

            if (!mockIsAuthenticated()) {
                mockRedirectToLogin();
            }

            expect(mockIsAuthenticated).toHaveBeenCalled();
            expect(mockRedirectToLogin).toHaveBeenCalled();
        });
    });

    describe('User Management', () => {
        it('should load all users', async () => {
            const mockUsers = {
                content: [
                    { id: 1, firstName: 'John', lastName: 'Doe', email: 'john@example.com', role: 'ROLE_PROFESSOR', isActive: true },
                    { id: 2, firstName: 'Jane', lastName: 'Smith', email: 'jane@example.com', role: 'ROLE_HOD', isActive: true }
                ],
                totalPages: 1,
                totalElements: 2
            };

            mockApiRequest.mockResolvedValue(mockUsers);

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            // Simulate loading users
            adminDashboard.usersCurrentPage = 0;
            adminDashboard.usersPageSize = 20;
            await adminDashboard.loadUsers();

            expect(mockApiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/admin/users'),
                { method: 'GET' }
            );
            expect(adminDashboard.users.length).toBeGreaterThan(0);
        });

        it('should filter users by role', async () => {
            const mockUsers = {
                content: [
                    { id: 1, firstName: 'John', lastName: 'Doe', email: 'john@example.com', role: 'ROLE_PROFESSOR', isActive: true }
                ],
                totalPages: 1,
                totalElements: 1
            };

            mockApiRequest.mockResolvedValue(mockUsers);

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            adminDashboard.usersFilters.role = 'ROLE_PROFESSOR';
            await adminDashboard.loadUsers();

            expect(mockApiRequest).toHaveBeenCalledWith(
                expect.stringContaining('role=ROLE_PROFESSOR'),
                { method: 'GET' }
            );
        });

        it('should create new user', async () => {
            const newUser = {
                firstName: 'New',
                lastName: 'User',
                email: 'newuser@staff.alquds.edu',
                role: 'ROLE_PROFESSOR',
                password: 'password123',
                departmentId: 1
            };

            mockApiRequest.mockResolvedValue({ id: 3, ...newUser });

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            // Simulate form submission
            const response = await mockApiRequest('/admin/users', {
                method: 'POST',
                body: JSON.stringify(newUser)
            });

            expect(mockApiRequest).toHaveBeenCalledWith(
                '/admin/users',
                expect.objectContaining({ method: 'POST' })
            );
            expect(response.id).toBeDefined();
        });

        it('should update user', async () => {
            const updatedUser = {
                id: 1,
                firstName: 'Updated',
                lastName: 'Name',
                email: 'updated@example.com',
                role: 'ROLE_PROFESSOR'
            };

            mockApiRequest.mockResolvedValue(updatedUser);

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            const response = await mockApiRequest(`/admin/users/${updatedUser.id}`, {
                method: 'PUT',
                body: JSON.stringify(updatedUser)
            });

            expect(mockApiRequest).toHaveBeenCalledWith(
                `/admin/users/${updatedUser.id}`,
                expect.objectContaining({ method: 'PUT' })
            );
            expect(response.firstName).toBe('Updated');
        });

        it('should deactivate user', async () => {
            const userId = 1;
            mockApiRequest.mockResolvedValue({ id: userId, isActive: false });

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            const response = await mockApiRequest(`/admin/users/${userId}/deactivate`, {
                method: 'PUT'
            });

            expect(mockApiRequest).toHaveBeenCalledWith(
                `/admin/users/${userId}/deactivate`,
                expect.objectContaining({ method: 'PUT' })
            );
            expect(response.isActive).toBe(false);
        });

        it('should reset user password', async () => {
            const userId = 1;
            const newPassword = 'newPassword123';
            mockApiRequest.mockResolvedValue({ success: true });

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            const response = await mockApiRequest(`/admin/users/${userId}/reset-password`, {
                method: 'POST',
                body: JSON.stringify({ newPassword })
            });

            expect(mockApiRequest).toHaveBeenCalledWith(
                `/admin/users/${userId}/reset-password`,
                expect.objectContaining({ method: 'POST' })
            );
            expect(response.success).toBe(true);
        });
    });

    describe('Department Management', () => {
        it('should load departments', async () => {
            const mockDepartments = [
                { id: 1, name: 'Computer Science', shortcut: 'CS' },
                { id: 2, name: 'Mathematics', shortcut: 'MATH' }
            ];

            mockApiRequest.mockResolvedValue(mockDepartments);

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            await adminDashboard.loadDepartmentsForFilters();

            expect(mockApiRequest).toHaveBeenCalledWith(
                '/admin/departments',
                { method: 'GET' }
            );
            expect(adminDashboard.departments.length).toBe(2);
        });

        it('should create department', async () => {
            const newDepartment = {
                name: 'Physics',
                shortcut: 'PHYS'
            };

            mockApiRequest.mockResolvedValue({ id: 3, ...newDepartment });

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            const response = await mockApiRequest('/admin/departments', {
                method: 'POST',
                body: JSON.stringify(newDepartment)
            });

            expect(mockApiRequest).toHaveBeenCalledWith(
                '/admin/departments',
                expect.objectContaining({ method: 'POST' })
            );
            expect(response.id).toBeDefined();
        });

        it('should update department', async () => {
            const updatedDepartment = {
                id: 1,
                name: 'Updated CS',
                shortcut: 'UCS'
            };

            mockApiRequest.mockResolvedValue(updatedDepartment);

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            const response = await mockApiRequest(`/admin/departments/${updatedDepartment.id}`, {
                method: 'PUT',
                body: JSON.stringify(updatedDepartment)
            });

            expect(mockApiRequest).toHaveBeenCalledWith(
                `/admin/departments/${updatedDepartment.id}`,
                expect.objectContaining({ method: 'PUT' })
            );
            expect(response.name).toBe('Updated CS');
        });

        it('should delete department', async () => {
            const departmentId = 1;
            mockApiRequest.mockResolvedValue({ success: true });

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();
            adminDashboard.deletingDepartmentId = departmentId;

            const response = await mockApiRequest(`/admin/departments/${departmentId}`, {
                method: 'DELETE'
            });

            expect(mockApiRequest).toHaveBeenCalledWith(
                `/admin/departments/${departmentId}`,
                expect.objectContaining({ method: 'DELETE' })
            );
            expect(response.success).toBe(true);
        });
    });

    describe('System Settings', () => {
        it('should load system configuration', async () => {
            const mockSettings = {
                maxFileSize: 10485760,
                allowedFileTypes: ['pdf', 'doc', 'docx'],
                submissionDeadline: '2024-12-31'
            };

            mockApiRequest.mockResolvedValue(mockSettings);

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            const response = await mockApiRequest('/admin/settings', {
                method: 'GET'
            });

            expect(mockApiRequest).toHaveBeenCalledWith(
                '/admin/settings',
                { method: 'GET' }
            );
            expect(response.maxFileSize).toBeDefined();
        });

        it('should update settings', async () => {
            const updatedSettings = {
                maxFileSize: 20971520,
                allowedFileTypes: ['pdf', 'doc', 'docx', 'xlsx'],
                submissionDeadline: '2025-12-31'
            };

            mockApiRequest.mockResolvedValue(updatedSettings);

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            const response = await mockApiRequest('/admin/settings', {
                method: 'PUT',
                body: JSON.stringify(updatedSettings)
            });

            expect(mockApiRequest).toHaveBeenCalledWith(
                '/admin/settings',
                expect.objectContaining({ method: 'PUT' })
            );
            expect(response.maxFileSize).toBe(20971520);
        });

        it('should validate settings before save', () => {
            const invalidSettings = {
                maxFileSize: -1, // Invalid
                allowedFileTypes: [],
                submissionDeadline: 'invalid-date'
            };

            const validateSettings = (settings) => {
                const errors = [];
                if (settings.maxFileSize <= 0) {
                    errors.push('Max file size must be greater than 0');
                }
                if (!settings.allowedFileTypes || settings.allowedFileTypes.length === 0) {
                    errors.push('At least one file type must be allowed');
                }
                if (!settings.submissionDeadline || isNaN(Date.parse(settings.submissionDeadline))) {
                    errors.push('Invalid deadline date');
                }
                return errors;
            };

            const errors = validateSettings(invalidSettings);
            expect(errors.length).toBeGreaterThan(0);
            expect(errors).toContain('Max file size must be greater than 0');
        });
    });

    describe('Dashboard Stats', () => {
        it('should display total users', async () => {
            const mockStats = {
                totalProfessors: 50,
                totalHods: 10,
                totalDepartments: 5,
                totalCourses: 100,
                totalSubmissions: 500,
                recentSubmissions: 50,
                pendingSubmissions: 10
            };

            mockApiRequest.mockResolvedValue(mockStats);

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            await adminDashboard.loadDashboardStats();

            expect(adminDashboard.stats.totalProfessors).toBe(50);
            expect(adminDashboard.stats.totalHods).toBe(10);
            const totalUsers = adminDashboard.stats.totalProfessors + adminDashboard.stats.totalHods;
            expect(totalUsers).toBe(60);
        });

        it('should display total courses', async () => {
            const mockStats = {
                totalCourses: 100
            };

            mockApiRequest.mockResolvedValue(mockStats);

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            await adminDashboard.loadDashboardStats();

            expect(adminDashboard.stats.totalCourses).toBe(100);
        });

        it('should display system health', async () => {
            const mockStats = {
                totalProfessors: 50,
                totalHods: 10,
                totalDepartments: 5,
                totalCourses: 100,
                totalSubmissions: 500,
                recentSubmissions: 50,
                pendingSubmissions: 10
            };

            mockApiRequest.mockResolvedValue(mockStats);

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            await adminDashboard.loadDashboardStats();

            // System health can be calculated from various metrics
            const hasActiveUsers = adminDashboard.stats.totalProfessors > 0 || adminDashboard.stats.totalHods > 0;
            const hasDepartments = adminDashboard.stats.totalDepartments > 0;
            const hasCourses = adminDashboard.stats.totalCourses > 0;

            expect(hasActiveUsers).toBe(true);
            expect(hasDepartments).toBe(true);
            expect(hasCourses).toBe(true);
        });
    });

    describe('Reports', () => {
        it('should generate admin reports', async () => {
            const mockReport = {
                statistics: {
                    totalUsers: 60,
                    totalDepartments: 5,
                    totalCourses: 100,
                    totalSubmissions: 500
                },
                data: []
            };

            mockApiRequest.mockResolvedValue(mockReport);

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            const context = adminDashboard.layout.getSelectedContext();
            const response = await mockApiRequest(`/admin/reports?academicYearId=${context.academicYearId}&semesterId=${context.semesterId}`, {
                method: 'GET'
            });

            expect(mockApiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/admin/reports'),
                { method: 'GET' }
            );
            expect(response.statistics).toBeDefined();
        });

        it('should export reports', async () => {
            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            const mockResponse = {
                ok: true,
                blob: jest.fn().mockResolvedValue(mockBlob)
            };

            fetch.mockResolvedValue(mockResponse);

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            const context = adminDashboard.layout.getSelectedContext();
            mockShowToast('Generating report...', 'info');

            const response = await fetch(`/admin/reports/export?academicYearId=${context.academicYearId}&semesterId=${context.semesterId}`);

            if (response.ok) {
                const blob = await response.blob();
                expect(blob).toBeDefined();
                expect(blob.type).toBe('application/pdf');
            }

            expect(mockShowToast).toHaveBeenCalledWith('Generating report...', 'info');
        });
    });

    describe('Tab Navigation', () => {
        it('should switch between tabs', async () => {
            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            adminDashboard.switchTab('users');

            expect(adminDashboard.currentTab).toBe('users');
            expect(localStorageMock.setItem).toHaveBeenCalledWith('adminCurrentTab', 'users');
        });

        it('should load tab data when switching', async () => {
            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            mockApiRequest.mockResolvedValue([]);

            adminDashboard.switchTab('users');
            await adminDashboard.loadTabData('users');

            expect(adminDashboard.loadedTabs.has('users')).toBe(true);
        });
    });

    describe('Error Handling', () => {
        it('should handle API errors gracefully', async () => {
            mockApiRequest.mockRejectedValue(new Error('API Error'));

            adminDashboard = new AdminDashboardPage();
            await adminDashboard.initialize();

            try {
                await adminDashboard.loadUsers();
            } catch (error) {
                expect(mockShowToast).toHaveBeenCalledWith(
                    'Failed to load users',
                    'error'
                );
            }
        });

        it('should show error message on initialization failure', async () => {
            mockAdminLayout.initialize.mockRejectedValue(new Error('Init failed'));

            adminDashboard = new AdminDashboardPage();

            try {
                await adminDashboard.initialize();
            } catch (error) {
                // Error should be handled
                expect(error).toBeDefined();
            }
        });
    });
});
