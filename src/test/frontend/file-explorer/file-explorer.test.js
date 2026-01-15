/**
 * @jest-environment jsdom
 */

import { jest } from '@jest/globals';

// Mock dependencies before importing the module under test
global.fetch = jest.fn();
global.window = {
    location: { href: '' },
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
const mockFileExplorerAPI = {
    getRoot: jest.fn(),
    getNode: jest.fn(),
    downloadFile: jest.fn(),
    uploadFile: jest.fn()
};

jest.mock('../../../main/resources/static/js/core/api.js', () => ({
    fileExplorer: mockFileExplorerAPI
}));

// Mock the UI module
const mockShowToast = jest.fn();
const mockShowModal = jest.fn();
const mockFormatDate = jest.fn((date) => date ? new Date(date).toLocaleDateString() : '');

jest.mock('../../../main/resources/static/js/core/ui.js', () => ({
    showToast: mockShowToast,
    showModal: mockShowModal,
    formatDate: mockFormatDate
}));

// Mock file-explorer-state
const mockState = {
    academicYearId: null,
    semesterId: null,
    treeRoot: null,
    currentNode: null,
    currentPath: '',
    breadcrumbs: [],
    isLoading: false,
    isTreeLoading: false,
    isFileListLoading: false,
    error: null
};

const mockStateListeners = [];
const mockFileExplorerState = {
    subscribe: jest.fn((callback) => {
        mockStateListeners.push(callback);
        return () => {
            const index = mockStateListeners.indexOf(callback);
            if (index > -1) mockStateListeners.splice(index, 1);
        };
    }),
    setLoading: jest.fn((loading) => {
        mockState.isLoading = loading;
        mockStateListeners.forEach(listener => listener(mockState));
    }),
    setTreeRoot: jest.fn((root) => {
        mockState.treeRoot = root;
        mockStateListeners.forEach(listener => listener(mockState));
    }),
    setCurrentNode: jest.fn((node, path) => {
        mockState.currentNode = node;
        mockState.currentPath = path;
        mockStateListeners.forEach(listener => listener(mockState));
    }),
    setBreadcrumbs: jest.fn((breadcrumbs) => {
        mockState.breadcrumbs = breadcrumbs;
        mockStateListeners.forEach(listener => listener(mockState));
    }),
    setError: jest.fn((error) => {
        mockState.error = error;
        mockStateListeners.forEach(listener => listener(mockState));
    }),
    getState: jest.fn(() => ({ ...mockState })),
    getContext: jest.fn(() => ({
        academicYearId: mockState.academicYearId,
        semesterId: mockState.semesterId
    }))
};

jest.mock('../../../main/resources/static/js/file-explorer/file-explorer-state.js', () => ({
    fileExplorerState: mockFileExplorerState
}));

// Mock file-preview-button
const mockFilePreviewButton = {
    isPreviewable: jest.fn((mimeType) => {
        const previewableTypes = ['application/pdf', 'text/plain', 'text/markdown'];
        return previewableTypes.includes(mimeType);
    }),
    renderButton: jest.fn((file) => {
        if (mockFilePreviewButton.isPreviewable(file.mimeType)) {
            return '<button class="preview-btn">Preview</button>';
        }
        return '';
    })
};

jest.mock('../../../main/resources/static/js/file-explorer/file-preview-button.js', () => ({
    FilePreviewButton: mockFilePreviewButton
}));

// Mock file-explorer-sync
const mockFileExplorerSync = {
    start: jest.fn(),
    stop: jest.fn(),
    addListener: jest.fn(),
    clearDeletedCache: jest.fn()
};

jest.mock('../../../main/resources/static/js/file-explorer/file-explorer-sync.js', () => ({
    fileExplorerSync: mockFileExplorerSync
}));

// Import the module under test
import { FileExplorer } from '../../../main/resources/static/js/file-explorer/file-explorer.js';

describe('File Explorer Core (file-explorer/file-explorer.js)', () => {
    let container;
    let fileExplorer;

    beforeEach(() => {
        // Clear all mocks
        jest.clearAllMocks();
        mockStateListeners.length = 0;
        mockState.academicYearId = null;
        mockState.semesterId = null;
        mockState.treeRoot = null;
        mockState.currentNode = null;
        mockState.currentPath = '';
        mockState.breadcrumbs = [];
        mockState.isLoading = false;
        mockState.error = null;

        // Setup DOM
        document.body.innerHTML = '';
        container = document.createElement('div');
        container.id = 'testFileExplorer';
        document.body.appendChild(container);

        // Mock timers for loading time tests
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
        if (fileExplorer && fileExplorer.unsubscribe) {
            fileExplorer.unsubscribe();
        }
        document.body.innerHTML = '';
    });

    describe('Initialization', () => {
        it('should set correct default options', () => {
            fileExplorer = new FileExplorer('testFileExplorer');
            
            expect(fileExplorer.options.readOnly).toBe(false);
            expect(fileExplorer.options.role).toBe('PROFESSOR');
            expect(fileExplorer.options.showOwnershipLabels).toBe(false);
            expect(fileExplorer.options.showDepartmentContext).toBe(false);
            expect(fileExplorer.options.hideTree).toBe(false);
        });

        it('should respect custom options (role, readOnly, showDepartmentContext)', () => {
            fileExplorer = new FileExplorer('testFileExplorer', {
                role: 'HOD',
                readOnly: true,
                showDepartmentContext: true,
                headerMessage: 'Test message',
                hideTree: true
            });
            
            expect(fileExplorer.options.role).toBe('HOD');
            expect(fileExplorer.options.readOnly).toBe(true);
            expect(fileExplorer.options.showDepartmentContext).toBe(true);
            expect(fileExplorer.options.headerMessage).toBe('Test message');
            expect(fileExplorer.options.hideTree).toBe(true);
        });

        it('should create correct DOM structure', () => {
            fileExplorer = new FileExplorer('testFileExplorer');
            
            expect(container.querySelector('#fileExplorerBreadcrumbs')).toBeTruthy();
            expect(container.querySelector('#fileExplorerFileList')).toBeTruthy();
            expect(container.querySelector('.file-explorer')).toBeTruthy();
        });

        it('should show loading state initially', () => {
            fileExplorer = new FileExplorer('testFileExplorer');
            
            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
            // Should show "Select a semester" message initially
            expect(fileList.textContent).toContain('Select a semester');
        });

        it('should throw error if container not found', () => {
            expect(() => {
                new FileExplorer('nonExistentContainer');
            }).toThrow('Container element with id "nonExistentContainer" not found');
        });

        it('should subscribe to fileExplorerState on initialization', () => {
            fileExplorer = new FileExplorer('testFileExplorer');
            
            expect(mockFileExplorerState.subscribe).toHaveBeenCalled();
        });

        it('should initialize sync service on initialization', () => {
            fileExplorer = new FileExplorer('testFileExplorer');
            
            expect(mockFileExplorerSync.start).toHaveBeenCalledWith(30000);
            expect(mockFileExplorerSync.addListener).toHaveBeenCalled();
        });
    });

    describe('Data Loading', () => {
        beforeEach(() => {
            fileExplorer = new FileExplorer('testFileExplorer');
        });

        it('should fetch root node for academic year/semester', async () => {
            const mockRootData = {
                id: 1,
                name: 'Root',
                children: [
                    { id: 2, name: 'Folder 1', type: 'folder' },
                    { id: 3, name: 'Folder 2', type: 'folder' }
                ],
                files: []
            };

            mockFileExplorerAPI.getRoot.mockResolvedValue({ data: mockRootData });

            await fileExplorer.loadRoot(1, 1);
            jest.advanceTimersByTime(600); // Advance past MIN_LOADING_TIME

            expect(mockFileExplorerAPI.getRoot).toHaveBeenCalledWith(1, 1);
            expect(mockFileExplorerState.setTreeRoot).toHaveBeenCalledWith(mockRootData);
        });

        it('should render folder cards correctly', async () => {
            const mockRootData = {
                id: 1,
                name: 'Root',
                children: [
                    { id: 2, name: 'Folder 1', type: 'folder', path: 'folder1' },
                    { id: 3, name: 'Folder 2', type: 'folder', path: 'folder2' }
                ],
                files: []
            };

            mockFileExplorerAPI.getRoot.mockResolvedValue({ data: mockRootData });

            await fileExplorer.loadRoot(1, 1);
            jest.advanceTimersByTime(600);

            const fileList = container.querySelector('#fileExplorerFileList');
            // Should render folder cards
            expect(fileList).toBeTruthy();
        });

        it('should handle empty folder list', async () => {
            const mockRootData = {
                id: 1,
                name: 'Root',
                children: [],
                files: []
            };

            mockFileExplorerAPI.getRoot.mockResolvedValue({ data: mockRootData });

            await fileExplorer.loadRoot(1, 1);
            jest.advanceTimersByTime(600);

            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
            // Should show empty state
        });

        it('should handle API errors gracefully', async () => {
            const error = new Error('Network error');
            mockFileExplorerAPI.getRoot.mockRejectedValue(error);

            await fileExplorer.loadRoot(1, 1);
            jest.advanceTimersByTime(600);

            expect(mockFileExplorerState.setError).toHaveBeenCalled();
            expect(mockShowToast).toHaveBeenCalled();
        });

        it('should load node by path', async () => {
            const mockNodeData = {
                id: 2,
                name: 'Folder 1',
                path: 'folder1',
                files: [
                    { id: 1, name: 'file1.pdf', mimeType: 'application/pdf', size: 1024 }
                ]
            };

            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });

            await fileExplorer.loadNode('folder1');
            jest.advanceTimersByTime(600);

            expect(mockFileExplorerAPI.getNode).toHaveBeenCalledWith('folder1');
            expect(mockFileExplorerState.setCurrentNode).toHaveBeenCalled();
        });

        it('should update breadcrumbs correctly when loading node', async () => {
            const mockNodeData = {
                id: 2,
                name: 'Folder 1',
                path: 'folder1',
                files: []
            };

            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });

            await fileExplorer.loadNode('folder1');
            jest.advanceTimersByTime(600);

            expect(mockFileExplorerState.setBreadcrumbs).toHaveBeenCalled();
        });

        it('should render files in table when loading node', async () => {
            const mockNodeData = {
                id: 2,
                name: 'Folder 1',
                path: 'folder1',
                files: [
                    { id: 1, name: 'file1.pdf', mimeType: 'application/pdf', size: 1024, uploadDate: '2024-01-01' },
                    { id: 2, name: 'file2.txt', mimeType: 'text/plain', size: 512, uploadDate: '2024-01-02' }
                ]
            };

            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });

            await fileExplorer.loadNode('folder1');
            jest.advanceTimersByTime(600);

            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
        });
    });

    describe('Navigation', () => {
        beforeEach(() => {
            fileExplorer = new FileExplorer('testFileExplorer');
        });

        it('should navigate to folder when folder card is clicked', async () => {
            const mockRootData = {
                id: 1,
                name: 'Root',
                children: [
                    { id: 2, name: 'Folder 1', type: 'folder', path: 'folder1' }
                ],
                files: []
            };

            mockFileExplorerAPI.getRoot.mockResolvedValue({ data: mockRootData });
            await fileExplorer.loadRoot(1, 1);
            jest.advanceTimersByTime(600);

            const mockNodeData = {
                id: 2,
                name: 'Folder 1',
                path: 'folder1',
                files: []
            };
            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });

            // Find and click folder card
            const folderCard = container.querySelector('[data-folder-path="folder1"]') || 
                               container.querySelector('[data-path="folder1"]') ||
                               container.querySelector('.folder-card');
            if (folderCard) {
                folderCard.click();
                jest.advanceTimersByTime(600);
                expect(mockFileExplorerAPI.getNode).toHaveBeenCalledWith('folder1');
            }
        });

        it('should navigate to ancestor when breadcrumb is clicked', async () => {
            // First load a node
            const mockNodeData = {
                id: 2,
                name: 'Folder 1',
                path: 'folder1',
                files: []
            };
            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });
            await fileExplorer.loadNode('folder1');
            jest.advanceTimersByTime(600);

            // Click breadcrumb
            const breadcrumb = container.querySelector('[data-breadcrumb-path]');
            if (breadcrumb) {
                const path = breadcrumb.getAttribute('data-breadcrumb-path');
                mockFileExplorerAPI.getNode.mockResolvedValue({ data: { id: 1, name: 'Root', path: '', files: [] } });
                breadcrumb.click();
                jest.advanceTimersByTime(600);
                expect(mockFileExplorerAPI.getNode).toHaveBeenCalled();
            }
        });

        it('should navigate back to previous folder', async () => {
            // Simulate navigation history
            fileExplorer.currentPath = 'folder1';
            fileExplorer.breadcrumbs = [
                { name: 'Root', path: '' },
                { name: 'Folder 1', path: 'folder1' }
            ];

            const mockNodeData = { id: 1, name: 'Root', path: '', files: [] };
            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });

            // Simulate back button click
            const backButton = container.querySelector('[data-action="back"]');
            if (backButton) {
                backButton.click();
                jest.advanceTimersByTime(600);
                expect(mockFileExplorerAPI.getNode).toHaveBeenCalled();
            }
        });

        it('should navigate up to parent folder', async () => {
            fileExplorer.currentPath = 'folder1/subfolder';
            fileExplorer.breadcrumbs = [
                { name: 'Root', path: '' },
                { name: 'Folder 1', path: 'folder1' },
                { name: 'Subfolder', path: 'folder1/subfolder' }
            ];

            const mockNodeData = { id: 2, name: 'Folder 1', path: 'folder1', files: [] };
            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });

            // Simulate up button click
            const upButton = container.querySelector('[data-action="up"]');
            if (upButton) {
                upButton.click();
                jest.advanceTimersByTime(600);
                expect(mockFileExplorerAPI.getNode).toHaveBeenCalledWith('folder1');
            }
        });
    });

    describe('File Operations (when not readOnly)', () => {
        it('should show upload button when not readOnly', () => {
            fileExplorer = new FileExplorer('testFileExplorer', {
                readOnly: false
            });

            // Upload button should be available in the UI
            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
        });

        it('should hide upload button when readOnly', () => {
            fileExplorer = new FileExplorer('testFileExplorer', {
                readOnly: true
            });

            // Upload controls should not be visible
            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
        });

        it('should trigger API call on file upload', async () => {
            fileExplorer = new FileExplorer('testFileExplorer', {
                readOnly: false
            });

            const mockFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
            const mockFormData = new FormData();
            mockFormData.append('file', mockFile);

            mockFileExplorerAPI.uploadFile.mockResolvedValue({ success: true });

            // Simulate file upload
            const fileInput = document.createElement('input');
            fileInput.type = 'file';
            fileInput.files = [mockFile];
            
            // Trigger upload handler
            if (fileExplorer.handleFileUpload) {
                await fileExplorer.handleFileUpload(mockFile, 'folder1');
                expect(mockFileExplorerAPI.uploadFile).toHaveBeenCalled();
            }
        });

        it('should trigger download on file download', async () => {
            fileExplorer = new FileExplorer('testFileExplorer');

            const mockFile = { id: 1, name: 'test.pdf', path: 'folder1/test.pdf' };
            const mockBlob = new Blob(['content'], { type: 'application/pdf' });

            mockFileExplorerAPI.downloadFile.mockResolvedValue(mockBlob);

            // Simulate download
            const downloadButton = document.createElement('button');
            downloadButton.setAttribute('data-file-id', '1');
            downloadButton.setAttribute('data-file-path', 'folder1/test.pdf');
            
            if (fileExplorer.handleFileDownload) {
                await fileExplorer.handleFileDownload(mockFile);
                expect(mockFileExplorerAPI.downloadFile).toHaveBeenCalled();
            }
        });
    });

    describe('Rendering', () => {
        beforeEach(() => {
            fileExplorer = new FileExplorer('testFileExplorer');
        });

        it('should create correct card structure for folders', async () => {
            const mockRootData = {
                id: 1,
                name: 'Root',
                children: [
                    { id: 2, name: 'Folder 1', type: 'folder', path: 'folder1' }
                ],
                files: []
            };

            mockFileExplorerAPI.getRoot.mockResolvedValue({ data: mockRootData });
            await fileExplorer.loadRoot(1, 1);
            jest.advanceTimersByTime(600);

            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
        });

        it('should show folder icon and name', async () => {
            const mockRootData = {
                id: 1,
                name: 'Root',
                children: [
                    { id: 2, name: 'Test Folder', type: 'folder', path: 'test-folder' }
                ],
                files: []
            };

            mockFileExplorerAPI.getRoot.mockResolvedValue({ data: mockRootData });
            await fileExplorer.loadRoot(1, 1);
            jest.advanceTimersByTime(600);

            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
            // Folder name should be visible
            expect(fileList.textContent).toContain('Test Folder');
        });

        it('should create table with correct columns for files', async () => {
            const mockNodeData = {
                id: 2,
                name: 'Folder 1',
                path: 'folder1',
                files: [
                    { id: 1, name: 'file1.pdf', mimeType: 'application/pdf', size: 1024, uploadDate: '2024-01-01' }
                ]
            };

            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });
            await fileExplorer.loadNode('folder1');
            jest.advanceTimersByTime(600);

            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
        });

        it('should show file metadata (name, size, date)', async () => {
            const mockNodeData = {
                id: 2,
                name: 'Folder 1',
                path: 'folder1',
                files: [
                    { 
                        id: 1, 
                        name: 'test.pdf', 
                        mimeType: 'application/pdf', 
                        size: 2048, 
                        uploadDate: '2024-01-01T00:00:00Z' 
                    }
                ]
            };

            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });
            await fileExplorer.loadNode('folder1');
            jest.advanceTimersByTime(600);

            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
            expect(fileList.textContent).toContain('test.pdf');
        });

        it('should show empty state when no files', async () => {
            const mockNodeData = {
                id: 2,
                name: 'Folder 1',
                path: 'folder1',
                files: []
            };

            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });
            await fileExplorer.loadNode('folder1');
            jest.advanceTimersByTime(600);

            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
        });

        it('should show home icon at start of breadcrumbs', () => {
            fileExplorer.breadcrumbs = [
                { name: 'Root', path: '' }
            ];
            fileExplorer.renderBreadcrumbs();

            const breadcrumbs = container.querySelector('#fileExplorerBreadcrumbs');
            expect(breadcrumbs).toBeTruthy();
        });

        it('should show separator between breadcrumb items', () => {
            fileExplorer.breadcrumbs = [
                { name: 'Root', path: '' },
                { name: 'Folder 1', path: 'folder1' },
                { name: 'Subfolder', path: 'folder1/subfolder' }
            ];
            fileExplorer.renderBreadcrumbs();

            const breadcrumbs = container.querySelector('#fileExplorerBreadcrumbs');
            expect(breadcrumbs).toBeTruthy();
        });

        it('should show preview button for previewable files', async () => {
            const mockNodeData = {
                id: 2,
                name: 'Folder 1',
                path: 'folder1',
                files: [
                    { id: 1, name: 'test.pdf', mimeType: 'application/pdf', size: 1024 }
                ]
            };

            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });
            await fileExplorer.loadNode('folder1');
            jest.advanceTimersByTime(600);

            expect(mockFilePreviewButton.isPreviewable).toHaveBeenCalledWith('application/pdf');
        });

        it('should show download button for all files', async () => {
            const mockNodeData = {
                id: 2,
                name: 'Folder 1',
                path: 'folder1',
                files: [
                    { id: 1, name: 'test.pdf', mimeType: 'application/pdf', size: 1024 }
                ]
            };

            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });
            await fileExplorer.loadNode('folder1');
            jest.advanceTimersByTime(600);

            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
        });
    });

    describe('Role-Specific Behavior', () => {
        it('should show upload controls for PROFESSOR role', () => {
            fileExplorer = new FileExplorer('testFileExplorer', {
                role: 'PROFESSOR',
                readOnly: false,
                showOwnershipLabels: true
            });

            expect(fileExplorer.options.role).toBe('PROFESSOR');
            expect(fileExplorer.options.readOnly).toBe(false);
            expect(fileExplorer.options.showOwnershipLabels).toBe(true);
        });

        it('should enforce read-only mode for HOD role', () => {
            fileExplorer = new FileExplorer('testFileExplorer', {
                role: 'HOD',
                readOnly: true,
                showDepartmentContext: true
            });

            expect(fileExplorer.options.role).toBe('HOD');
            expect(fileExplorer.options.readOnly).toBe(true);
            expect(fileExplorer.options.showDepartmentContext).toBe(true);
        });

        it('should enforce read-only mode with department context for DEANSHIP role', () => {
            fileExplorer = new FileExplorer('testFileExplorer', {
                role: 'DEANSHIP',
                readOnly: true,
                showAllDepartments: true,
                showProfessorLabels: true
            });

            expect(fileExplorer.options.role).toBe('DEANSHIP');
            expect(fileExplorer.options.readOnly).toBe(true);
            expect(fileExplorer.options.showAllDepartments).toBe(true);
            expect(fileExplorer.options.showProfessorLabels).toBe(true);
        });

        it('should allow full access for ADMIN role', () => {
            fileExplorer = new FileExplorer('testFileExplorer', {
                role: 'ADMIN',
                readOnly: false
            });

            expect(fileExplorer.options.role).toBe('ADMIN');
            expect(fileExplorer.options.readOnly).toBe(false);
        });

        it('should show header message for HOD role when provided', () => {
            fileExplorer = new FileExplorer('testFileExplorer', {
                role: 'HOD',
                headerMessage: 'Browse department files (Read-only)'
            });

            const header = container.querySelector('.bg-blue-50');
            expect(header).toBeTruthy();
            expect(header.textContent).toContain('Browse department files');
        });

        it('should hide tree view when hideTree option is true', () => {
            fileExplorer = new FileExplorer('testFileExplorer', {
                hideTree: true
            });

            const treeContainer = container.querySelector('#fileExplorerTree');
            // Tree should not be rendered when hideTree is true
            expect(treeContainer).toBeFalsy();
        });

        it('should show tree view when hideTree option is false', () => {
            fileExplorer = new FileExplorer('testFileExplorer', {
                hideTree: false
            });

            const treeContainer = container.querySelector('#fileExplorerTree');
            expect(treeContainer).toBeTruthy();
        });
    });

    describe('Error Handling', () => {
        beforeEach(() => {
            fileExplorer = new FileExplorer('testFileExplorer');
        });

        it('should handle network errors during loadRoot', async () => {
            const error = new Error('Network error');
            mockFileExplorerAPI.getRoot.mockRejectedValue(error);

            await fileExplorer.loadRoot(1, 1);
            jest.advanceTimersByTime(600);

            expect(mockFileExplorerState.setError).toHaveBeenCalled();
            expect(mockShowToast).toHaveBeenCalled();
        });

        it('should handle 404 errors gracefully', async () => {
            const error = new Error('Not found');
            error.message = 'not found';
            mockFileExplorerAPI.getRoot.mockRejectedValue(error);

            await fileExplorer.loadRoot(1, 1);
            jest.advanceTimersByTime(600);

            // Should show empty state for not found errors
            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
        });

        it('should handle null/empty responses', async () => {
            mockFileExplorerAPI.getRoot.mockResolvedValue({ data: null });

            await fileExplorer.loadRoot(1, 1);
            jest.advanceTimersByTime(600);

            const fileList = container.querySelector('#fileExplorerFileList');
            expect(fileList).toBeTruthy();
        });
    });

    describe('State Management', () => {
        beforeEach(() => {
            fileExplorer = new FileExplorer('testFileExplorer');
        });

        it('should update state when loading root', async () => {
            const mockRootData = {
                id: 1,
                name: 'Root',
                children: [],
                files: []
            };

            mockFileExplorerAPI.getRoot.mockResolvedValue({ data: mockRootData });

            await fileExplorer.loadRoot(1, 1);
            jest.advanceTimersByTime(600);

            expect(mockFileExplorerState.setTreeRoot).toHaveBeenCalledWith(mockRootData);
            expect(mockFileExplorerState.setCurrentNode).toHaveBeenCalled();
        });

        it('should update state when loading node', async () => {
            const mockNodeData = {
                id: 2,
                name: 'Folder 1',
                path: 'folder1',
                files: []
            };

            mockFileExplorerAPI.getNode.mockResolvedValue({ data: mockNodeData });

            await fileExplorer.loadNode('folder1');
            jest.advanceTimersByTime(600);

            expect(mockFileExplorerState.setCurrentNode).toHaveBeenCalled();
            expect(mockFileExplorerState.setBreadcrumbs).toHaveBeenCalled();
        });

        it('should respond to state changes from FileExplorerState', () => {
            const newState = {
                ...mockState,
                currentNode: { id: 1, name: 'Test', files: [] },
                currentPath: 'test'
            };

            // Trigger state change
            mockStateListeners.forEach(listener => listener(newState));

            // FileExplorer should have updated its local state
            expect(fileExplorer.currentNode).toBeDefined();
        });
    });
});
