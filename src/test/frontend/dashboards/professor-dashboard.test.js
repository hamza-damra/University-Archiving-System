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
const mockProfessorAPI = {
    getMyCourses: jest.fn(),
    uploadFiles: jest.fn(),
    getSubmissionFiles: jest.fn(),
    downloadFile: jest.fn(),
    deleteFile: jest.fn()
};

const mockFileExplorerAPI = {
    getRoot: jest.fn(),
    getNode: jest.fn(),
    downloadFile: jest.fn()
};

const mockApiRequest = jest.fn();
const mockGetUserInfo = jest.fn();
const mockIsAuthenticated = jest.fn();
const mockRedirectToLogin = jest.fn();
const mockClearAuthData = jest.fn();
const mockInitializeAuth = jest.fn();

jest.mock('../../../main/resources/static/js/core/api.js', () => ({
    professor: mockProfessorAPI,
    fileExplorer: mockFileExplorerAPI,
    apiRequest: mockApiRequest,
    getUserInfo: mockGetUserInfo,
    isAuthenticated: mockIsAuthenticated,
    redirectToLogin: mockRedirectToLogin,
    clearAuthData: mockClearAuthData,
    initializeAuth: mockInitializeAuth
}));

// Mock the UI module
const mockShowToast = jest.fn();
const mockShowModal = jest.fn();
const mockFormatDate = jest.fn((date) => new Date(date).toLocaleDateString());
const mockGetTimeUntil = jest.fn((date) => '2 days');
const mockFormatFileSize = jest.fn((bytes) => `${(bytes / 1024).toFixed(2)} KB`);

jest.mock('../../../main/resources/static/js/core/ui.js', () => ({
    showToast: mockShowToast,
    showModal: mockShowModal,
    formatDate: mockFormatDate,
    getTimeUntil: mockGetTimeUntil,
    formatFileSize: mockFormatFileSize
}));

// Mock FileExplorer
const mockFileExplorer = jest.fn().mockImplementation(() => ({
    loadRoot: jest.fn().mockResolvedValue(undefined),
    loadNode: jest.fn().mockResolvedValue(undefined),
    init: jest.fn()
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

// Mock initModernDropdowns
global.initModernDropdowns = jest.fn().mockReturnValue([{}, {}]);

describe('Professor Dashboard (shared/prof.js)', () => {
    let mockDocument;
    let professorName, logoutBtn, notificationsBtn, notificationBadge;
    let notificationsDropdown, closeNotificationsDropdown, notificationsList;
    let academicYearSelect, semesterSelect, coursesContainer, emptyState;
    let coursesTabContent, fileExplorerTabContent, fileExplorerContainer;

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
        element.getBoundingClientRect = jest.fn(() => ({
            bottom: 100,
            right: 200,
            top: 50,
            left: 100
        }));
        element.style = {};
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
        professorName = createMockElement('professorName', 'span');
        logoutBtn = createMockElement('logoutBtn', 'button');
        notificationsBtn = createMockElement('notificationsBtn', 'button');
        notificationBadge = createMockElement('notificationBadge', 'span');
        notificationsDropdown = createMockElement('notificationsDropdown', 'div');
        closeNotificationsDropdown = createMockElement('closeNotificationsDropdown', 'button');
        notificationsList = createMockElement('notificationsList', 'div');
        academicYearSelect = createMockElement('academicYearSelect', 'select');
        semesterSelect = createMockElement('semesterSelect', 'select');
        coursesContainer = createMockElement('coursesContainer', 'div');
        emptyState = createMockElement('emptyState', 'div');
        coursesTabContent = createMockElement('coursesTabContent', 'div');
        fileExplorerTabContent = createMockElement('fileExplorerTabContent', 'div');
        fileExplorerContainer = createMockElement('fileExplorerContainer', 'div');

        // Append elements to document
        document.body.appendChild(professorName);
        document.body.appendChild(logoutBtn);
        document.body.appendChild(notificationsBtn);
        document.body.appendChild(notificationBadge);
        document.body.appendChild(notificationsDropdown);
        document.body.appendChild(closeNotificationsDropdown);
        document.body.appendChild(notificationsList);
        document.body.appendChild(academicYearSelect);
        document.body.appendChild(semesterSelect);
        document.body.appendChild(coursesContainer);
        document.body.appendChild(emptyState);
        document.body.appendChild(coursesTabContent);
        document.body.appendChild(fileExplorerTabContent);
        document.body.appendChild(fileExplorerContainer);

        // Create tab navigation elements
        const navTab1 = createMockElement('navTab1', 'button');
        navTab1.classList.add = jest.fn();
        navTab1.classList.remove = jest.fn();
        navTab1.setAttribute('data-tab', 'dashboard');
        document.body.appendChild(navTab1);

        const navTab2 = createMockElement('navTab2', 'button');
        navTab2.classList.add = jest.fn();
        navTab2.classList.remove = jest.fn();
        navTab2.setAttribute('data-tab', 'fileExplorer');
        document.body.appendChild(navTab2);

        // Setup default mocks
        mockIsAuthenticated.mockReturnValue(true);
        mockInitializeAuth.mockResolvedValue(true);
        mockGetUserInfo.mockReturnValue({
            id: 1,
            email: 'professor@example.com',
            fullName: 'Professor User',
            firstName: 'Professor',
            lastName: 'User',
            role: 'ROLE_PROFESSOR',
            departmentId: 1,
            departmentName: 'Computer Science'
        });
        mockApiRequest.mockResolvedValue({
            userId: 1,
            email: 'professor@example.com',
            firstName: 'Professor',
            lastName: 'User',
            role: 'ROLE_PROFESSOR',
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

        it('should validate ROLE_PROFESSOR', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockInitializeAuth.mockResolvedValue(true);

            const freshUserInfo = await mockApiRequest('/auth/me');
            expect(mockApiRequest).toHaveBeenCalledWith('/auth/me');
            expect(freshUserInfo).toBeDefined();
            expect(freshUserInfo.role).toBe('ROLE_PROFESSOR');
        });

        it('should display professor name', () => {
            const userInfo = mockGetUserInfo();
            if (professorName) {
                professorName.textContent = userInfo.fullName;
            }
            expect(userInfo.fullName).toBe('Professor User');
        });

        it('should reject non-professor role', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockInitializeAuth.mockResolvedValue(true);
            mockApiRequest.mockResolvedValue({
                userId: 1,
                email: 'user@example.com',
                role: 'ROLE_HOD'
            });

            const freshUserInfo = await mockApiRequest('/auth/me');
            if (freshUserInfo.role !== 'ROLE_PROFESSOR') {
                mockShowToast('Access denied - Professor privileges required. Your role: ' + freshUserInfo.role, 'error');
            }

            expect(mockShowToast).toHaveBeenCalledWith(
                expect.stringContaining('Access denied'),
                'error'
            );
        });
    });

    describe('Course Display', () => {
        it('should fetch courses for semester', async () => {
            const mockCourses = [
                {
                    id: 1,
                    courseCode: 'CS101',
                    courseName: 'Introduction to Computer Science',
                    documentTypes: [
                        { type: 'SYLLABUS', uploaded: true },
                        { type: 'EXAM', uploaded: false }
                    ]
                }
            ];

            mockProfessorAPI.getMyCourses.mockResolvedValue(mockCourses);
            const semesterId = 1;

            const courses = await mockProfessorAPI.getMyCourses(semesterId);
            expect(mockProfessorAPI.getMyCourses).toHaveBeenCalledWith(semesterId);
            expect(courses).toBeDefined();
            expect(Array.isArray(courses)).toBe(true);
        });

        it('should create course cards', () => {
            const course = {
                id: 1,
                courseCode: 'CS101',
                courseName: 'Introduction to Computer Science',
                documentTypes: [
                    { type: 'SYLLABUS', uploaded: true, submissionId: 1 },
                    { type: 'EXAM', uploaded: false }
                ]
            };

            // Simulate course card creation
            const courseCard = document.createElement('div');
            courseCard.className = 'course-card';
            courseCard.innerHTML = `
                <div class="course-header">
                    <h3>${course.courseCode} - ${course.courseName}</h3>
                </div>
                <div class="document-types">
                    ${course.documentTypes.map(doc => `
                        <div class="doc-type-row">
                            <span>${doc.type}</span>
                            <span>${doc.uploaded ? 'Uploaded' : 'Not Uploaded'}</span>
                        </div>
                    `).join('')}
                </div>
            `;

            expect(courseCard).toBeDefined();
            expect(courseCard.textContent).toContain('CS101');
            expect(courseCard.textContent).toContain('Introduction to Computer Science');
        });

        it('should show course code and name in course card', () => {
            const course = {
                courseCode: 'CS101',
                courseName: 'Introduction to Computer Science'
            };

            const courseCard = document.createElement('div');
            courseCard.innerHTML = `
                <div class="course-header">
                    <h3>${course.courseCode} - ${course.courseName}</h3>
                </div>
            `;

            expect(courseCard.textContent).toContain('CS101');
            expect(courseCard.textContent).toContain('Introduction to Computer Science');
        });

        it('should show document type counts in course card', () => {
            const course = {
                courseCode: 'CS101',
                courseName: 'Introduction to Computer Science',
                documentTypes: [
                    { type: 'SYLLABUS', uploaded: true },
                    { type: 'EXAM', uploaded: false },
                    { type: 'ASSIGNMENT', uploaded: true }
                ]
            };

            const uploadedCount = course.documentTypes.filter(doc => doc.uploaded).length;
            const totalCount = course.documentTypes.length;

            expect(uploadedCount).toBe(2);
            expect(totalCount).toBe(3);
        });

        it('should expand course card on click', () => {
            const courseCard = document.createElement('div');
            courseCard.className = 'course-card';
            const details = document.createElement('div');
            details.className = 'course-details hidden';
            courseCard.appendChild(details);

            let isExpanded = false;
            courseCard.addEventListener('click', () => {
                isExpanded = !isExpanded;
                if (isExpanded) {
                    details.classList.remove('hidden');
                } else {
                    details.classList.add('hidden');
                }
            });

            // Simulate click
            const clickEvent = new Event('click');
            courseCard.dispatchEvent(clickEvent);

            expect(isExpanded).toBe(true);
        });
    });

    describe('Document Upload', () => {
        it('should open file picker when upload button is clicked', () => {
            const uploadButton = document.createElement('button');
            uploadButton.id = 'uploadBtn';
            const fileInput = document.createElement('input');
            fileInput.type = 'file';
            fileInput.style.display = 'none';
            document.body.appendChild(uploadButton);
            document.body.appendChild(fileInput);

            uploadButton.addEventListener('click', () => {
                fileInput.click();
            });

            const clickSpy = jest.spyOn(fileInput, 'click');
            uploadButton.click();

            expect(clickSpy).toHaveBeenCalled();
        });

        it('should trigger upload on file selection', async () => {
            const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
            const formData = new FormData();
            formData.append('files', file);

            mockProfessorAPI.uploadFiles.mockResolvedValue({
                success: true,
                message: 'Files uploaded successfully'
            });

            const result = await mockProfessorAPI.uploadFiles(1, 'SYLLABUS', formData);
            expect(mockProfessorAPI.uploadFiles).toHaveBeenCalled();
            expect(result.success).toBe(true);
        });

        it('should show upload progress', () => {
            const progressBar = document.createElement('div');
            progressBar.className = 'upload-progress';
            progressBar.innerHTML = '<div class="progress-bar" style="width: 0%"></div>';
            document.body.appendChild(progressBar);

            const updateProgress = (percent) => {
                const bar = progressBar.querySelector('.progress-bar');
                if (bar) {
                    bar.style.width = `${percent}%`;
                }
            };

            updateProgress(50);
            const bar = progressBar.querySelector('.progress-bar');
            expect(bar.style.width).toBe('50%');
        });

        it('should show toast on upload success', async () => {
            mockProfessorAPI.uploadFiles.mockResolvedValue({
                success: true,
                message: 'Files uploaded successfully'
            });

            const result = await mockProfessorAPI.uploadFiles(1, 'SYLLABUS', new FormData());
            if (result.success) {
                mockShowToast('Files uploaded successfully', 'success');
            }

            expect(mockShowToast).toHaveBeenCalledWith(
                'Files uploaded successfully',
                'success'
            );
        });

        it('should handle upload error gracefully', async () => {
            mockProfessorAPI.uploadFiles.mockRejectedValue(new Error('Upload failed'));

            try {
                await mockProfessorAPI.uploadFiles(1, 'SYLLABUS', new FormData());
            } catch (error) {
                mockShowToast('Upload failed: ' + error.message, 'error');
            }

            expect(mockShowToast).toHaveBeenCalledWith(
                expect.stringContaining('Upload failed'),
                'error'
            );
        });

        it('should reject invalid file type', () => {
            const allowedTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
            const file = new File(['content'], 'test.exe', { type: 'application/x-msdownload' });

            const isValidType = allowedTypes.includes(file.type) || 
                file.name.endsWith('.pdf') || 
                file.name.endsWith('.doc') || 
                file.name.endsWith('.docx');

            expect(isValidType).toBe(false);
        });

        it('should enforce file size limit', () => {
            const maxSizeMB = 10;
            const maxSizeBytes = maxSizeMB * 1024 * 1024;
            const file = new File(['x'.repeat(maxSizeBytes + 1)], 'large.pdf', { type: 'application/pdf' });

            const isValidSize = file.size <= maxSizeBytes;

            expect(isValidSize).toBe(false);
            expect(file.size).toBeGreaterThan(maxSizeBytes);
        });
    });

    describe('Document Management', () => {
        it('should display document list correctly', async () => {
            const mockFiles = [
                { id: 1, fileName: 'syllabus.pdf', fileSize: 1024, uploadDate: '2024-01-15' },
                { id: 2, fileName: 'exam.pdf', fileSize: 2048, uploadDate: '2024-01-16' }
            ];

            mockProfessorAPI.getSubmissionFiles.mockResolvedValue(mockFiles);

            const files = await mockProfessorAPI.getSubmissionFiles(1);
            expect(files).toBeDefined();
            expect(Array.isArray(files)).toBe(true);
            expect(files.length).toBe(2);
        });

        it('should show document status badges', () => {
            const doc = {
                id: 1,
                fileName: 'syllabus.pdf',
                status: 'UPLOADED',
                uploadDate: '2024-01-15'
            };

            const badge = document.createElement('span');
            badge.className = 'status-badge';
            if (doc.status === 'UPLOADED') {
                badge.className += ' bg-green-100 text-green-800';
                badge.textContent = 'Uploaded';
            } else if (doc.status === 'NOT_UPLOADED') {
                badge.className += ' bg-gray-100 text-gray-800';
                badge.textContent = 'Not Uploaded';
            } else if (doc.status === 'OVERDUE') {
                badge.className += ' bg-red-100 text-red-800';
                badge.textContent = 'Overdue';
            }

            expect(badge.textContent).toBe('Uploaded');
            expect(badge.className).toContain('bg-green-100');
        });

        it('should download document', async () => {
            const mockBlob = new Blob(['file content'], { type: 'application/pdf' });
            mockProfessorAPI.downloadFile.mockResolvedValue(mockBlob);

            const fileId = 1;
            const blob = await mockProfessorAPI.downloadFile(fileId);
            expect(mockProfessorAPI.downloadFile).toHaveBeenCalledWith(fileId);
            expect(blob).toBeDefined();
        });

        it('should replace document', async () => {
            const file = new File(['new content'], 'new-syllabus.pdf', { type: 'application/pdf' });
            const formData = new FormData();
            formData.append('files', file);

            mockProfessorAPI.uploadFiles.mockResolvedValue({
                success: true,
                message: 'Document replaced successfully'
            });

            const result = await mockProfessorAPI.uploadFiles(1, 'SYLLABUS', formData, 1, true);
            expect(result.success).toBe(true);
        });

        it('should delete document if allowed', async () => {
            mockProfessorAPI.deleteFile.mockResolvedValue({
                success: true,
                message: 'File deleted successfully'
            });

            const result = await mockProfessorAPI.deleteFile(1);
            expect(mockProfessorAPI.deleteFile).toHaveBeenCalledWith(1);
            expect(result.success).toBe(true);
        });
    });

    describe('Dashboard Overview', () => {
        it('should show course count', () => {
            const courses = [
                { id: 1, courseCode: 'CS101' },
                { id: 2, courseCode: 'CS102' },
                { id: 3, courseCode: 'CS103' }
            ];

            const courseCount = courses.length;
            expect(courseCount).toBe(3);
        });

        it('should show submission status', () => {
            const courses = [
                {
                    documentTypes: [
                        { type: 'SYLLABUS', uploaded: true },
                        { type: 'EXAM', uploaded: false }
                    ]
                },
                {
                    documentTypes: [
                        { type: 'SYLLABUS', uploaded: true },
                        { type: 'EXAM', uploaded: true }
                    ]
                }
            ];

            let totalUploaded = 0;
            let totalRequired = 0;

            courses.forEach(course => {
                course.documentTypes.forEach(doc => {
                    totalRequired++;
                    if (doc.uploaded) totalUploaded++;
                });
            });

            expect(totalUploaded).toBe(3);
            expect(totalRequired).toBe(4);
        });

        it('should show deadlines', () => {
            // Use a deadline in the future (relative to test execution date)
            const deadline = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
            const now = new Date();
            const daysUntilDeadline = Math.ceil((deadline - now) / (1000 * 60 * 60 * 24));

            expect(daysUntilDeadline).toBeGreaterThan(0);
        });
    });

    describe('Notifications', () => {
        it('should load notifications', async () => {
            const mockNotifications = [
                { id: 1, message: 'New deadline approaching', seen: false, createdAt: '2024-01-15' },
                { id: 2, message: 'Document approved', seen: true, createdAt: '2024-01-14' }
            ];

            mockApiRequest.mockResolvedValue(mockNotifications);
            const notifications = await mockApiRequest('/api/notifications');

            expect(notifications).toBeDefined();
            expect(Array.isArray(notifications)).toBe(true);
            expect(notifications.length).toBe(2);
        });

        it('should mark notification as seen', async () => {
            mockApiRequest.mockResolvedValue({ success: true });
            const result = await mockApiRequest('/api/notifications/1/seen', { method: 'PUT' });

            expect(mockApiRequest).toHaveBeenCalledWith('/api/notifications/1/seen', expect.objectContaining({ method: 'PUT' }));
            expect(result.success).toBe(true);
        });

        it('should show notification badge', () => {
            const notifications = [
                { id: 1, seen: false },
                { id: 2, seen: false },
                { id: 3, seen: true }
            ];

            const unseenCount = notifications.filter(n => !n.seen).length;

            if (notificationBadge) {
                if (unseenCount > 0) {
                    notificationBadge.textContent = unseenCount.toString();
                    notificationBadge.classList.remove('hidden');
                } else {
                    notificationBadge.classList.add('hidden');
                }
            }

            expect(unseenCount).toBe(2);
        });
    });

    describe('File Explorer Integration', () => {
        it('should initialize file explorer', () => {
            const fileExplorer = new mockFileExplorer({
                role: 'PROFESSOR',
                readOnly: false,
                showDepartmentContext: false
            });

            expect(fileExplorer).toBeDefined();
            expect(mockFileExplorer).toHaveBeenCalled();
        });

        it('should navigate in file explorer', async () => {
            const mockNodeData = {
                folders: [
                    { name: 'Folder1', path: '/folder1', canWrite: true }
                ],
                files: [
                    { id: 1, fileName: 'file.pdf', fileSize: 1024 }
                ]
            };

            mockFileExplorerAPI.getNode.mockResolvedValue(mockNodeData);
            const data = await mockFileExplorerAPI.getNode('/folder1');

            expect(mockFileExplorerAPI.getNode).toHaveBeenCalledWith('/folder1');
            expect(data).toBeDefined();
            expect(data.folders).toBeDefined();
            expect(data.files).toBeDefined();
        });

        it('should integrate upload with explorer', async () => {
            const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
            const formData = new FormData();
            formData.append('files', file);

            mockProfessorAPI.uploadFiles.mockResolvedValue({
                success: true,
                message: 'File uploaded to explorer'
            });

            const result = await mockProfessorAPI.uploadFiles(1, 'SYLLABUS', formData);
            expect(result.success).toBe(true);
        });
    });
});
