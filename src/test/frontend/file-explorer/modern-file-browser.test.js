/**
 * @jest-environment jsdom
 */

import { jest } from '@jest/globals';

// Mock dependencies before importing the module under test
global.fetch = jest.fn();

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
jest.mock('../../../main/resources/static/js/core/api.js', () => ({
    fileExplorer: {
        getRoot: jest.fn(),
        getNode: jest.fn(),
        getBreadcrumbs: jest.fn(),
        downloadFile: jest.fn()
    }
}));

// Mock the UI module
jest.mock('../../../main/resources/static/js/core/ui.js', () => ({
    showToast: jest.fn(),
    showModal: jest.fn(),
    formatDate: jest.fn((date) => {
        if (!date) return '-';
        return new Date(date).toLocaleDateString();
    })
}));

// Mock FilePreviewButton
jest.mock('../../../main/resources/static/js/file-explorer/file-preview-button.js', () => ({
    FilePreviewButton: {
        isPreviewable: jest.fn((fileType, fileName) => {
            const previewableTypes = ['application/pdf', 'text/plain', 'text/html', 'application/javascript'];
            return previewableTypes.includes(fileType) || fileName.endsWith('.pdf') || fileName.endsWith('.txt');
        })
    }
}));

// Mock fileExplorerState
jest.mock('../../../main/resources/static/js/file-explorer/file-explorer-state.js', () => ({
    fileExplorerState: {
        state: {
            academicYearId: null,
            semesterId: null,
            yearCode: null,
            semesterType: null
        },
        setContext: jest.fn()
    }
}));

// Import mocked modules
import { fileExplorer } from '../../../main/resources/static/js/core/api.js';
import { showToast, formatDate } from '../../../main/resources/static/js/core/ui.js';
import { FilePreviewButton } from '../../../main/resources/static/js/file-explorer/file-preview-button.js';
import { fileExplorerState } from '../../../main/resources/static/js/file-explorer/file-explorer-state.js';

// Import the module under test
import { ModernFileBrowser } from '../../../main/resources/static/js/file-explorer/modern-file-browser.js';

describe('ModernFileBrowser', () => {
    let container;
    let browser;

    beforeEach(() => {
        // Reset all mocks
        jest.clearAllMocks();
        localStorageMock.clear();
        fetch.mockClear();

        // Create container element
        container = document.createElement('div');
        container.id = 'test-container';
        document.body.appendChild(container);

        // Reset window.filePreviewButton
        window.filePreviewButton = {
            handlePreviewClick: jest.fn()
        };
    });

    afterEach(() => {
        // Cleanup
        if (browser && browser.container) {
            browser.container.innerHTML = '';
        }
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
        delete window.fileExplorerInstance;
        delete window.filePreviewButton;
    });

    describe('Initialization', () => {
        it('should create correct layout', () => {
            browser = new ModernFileBrowser('test-container');
            
            expect(container.querySelector('.modern-file-browser')).toBeTruthy();
            expect(container.querySelector('.mfb-toolbar')).toBeTruthy();
            expect(container.querySelector('.mfb-workspace')).toBeTruthy();
            expect(container.querySelector('.mfb-statusbar')).toBeTruthy();
            expect(container.querySelector('#mfb-main-view')).toBeTruthy();
        });

        it('should load view mode from localStorage', () => {
            localStorageMock.getItem.mockReturnValueOnce('list');
            
            browser = new ModernFileBrowser('test-container');
            
            expect(localStorage.getItem).toHaveBeenCalledWith('mfb-view-mode');
            expect(browser.options.viewMode).toBe('list');
        });

        it('should load sidebar visibility from localStorage', () => {
            localStorageMock.getItem.mockImplementation((key) => {
                if (key === 'mfb-view-mode') return 'grid';
                if (key === 'mfb-show-sidebar') return 'false';
                return null;
            });
            
            browser = new ModernFileBrowser('test-container');
            
            expect(browser.options.showSidebar).toBe(false);
            expect(container.querySelector('#mfb-sidebar').classList.contains('hidden')).toBe(true);
        });

        it('should bind all event handlers', () => {
            browser = new ModernFileBrowser('test-container');
            
            const backBtn = container.querySelector('#mfb-back');
            const forwardBtn = container.querySelector('#mfb-forward');
            const upBtn = container.querySelector('#mfb-up');
            const refreshBtn = container.querySelector('#mfb-refresh');
            const gridBtn = container.querySelector('#mfb-view-grid');
            const listBtn = container.querySelector('#mfb-view-list');
            const sidebarToggle = container.querySelector('#mfb-toggle-sidebar');
            
            expect(backBtn).toBeTruthy();
            expect(forwardBtn).toBeTruthy();
            expect(upBtn).toBeTruthy();
            expect(refreshBtn).toBeTruthy();
            expect(gridBtn).toBeTruthy();
            expect(listBtn).toBeTruthy();
            expect(sidebarToggle).toBeTruthy();
        });
    });

    describe('View Modes', () => {
        beforeEach(() => {
            browser = new ModernFileBrowser('test-container');
            browser.folders = [
                { path: '/folder1', name: 'Folder 1', itemsCount: 5 }
            ];
            browser.files = [
                { id: 1, name: 'file1.pdf', size: 1024, extension: 'pdf', uploadDate: new Date() }
            ];
        });

        it('should display files as cards in grid view', () => {
            browser.options.viewMode = 'grid';
            browser.renderContent();
            
            const viewContainer = container.querySelector('#mfb-main-view');
            expect(viewContainer.querySelector('.mfb-view-grid')).toBeTruthy();
            expect(viewContainer.querySelector('.mfb-item[data-type="folder"]')).toBeTruthy();
            expect(viewContainer.querySelector('.mfb-item[data-type="file"]')).toBeTruthy();
        });

        it('should display files in table in list view', () => {
            browser.options.viewMode = 'list';
            browser.renderContent();
            
            const viewContainer = container.querySelector('#mfb-main-view');
            expect(viewContainer.querySelector('.mfb-view-list')).toBeTruthy();
            expect(viewContainer.querySelector('.mfb-list-item[data-type="folder"]')).toBeTruthy();
            expect(viewContainer.querySelector('.mfb-list-item[data-type="file"]')).toBeTruthy();
        });

        it('should update localStorage when toggling view mode', () => {
            browser.options.viewMode = 'grid';
            browser.toggleViewMode('list');
            
            expect(localStorage.setItem).toHaveBeenCalledWith('mfb-view-mode', 'list');
            expect(browser.options.viewMode).toBe('list');
        });

        it('should persist view mode across page loads', () => {
            localStorageMock.getItem.mockReturnValueOnce('list');
            
            browser = new ModernFileBrowser('test-container');
            
            expect(browser.options.viewMode).toBe('list');
            
            // Simulate page reload
            const newBrowser = new ModernFileBrowser('test-container');
            expect(newBrowser.options.viewMode).toBe('list');
        });
    });

    describe('Navigation', () => {
        beforeEach(() => {
            browser = new ModernFileBrowser('test-container');
            fileExplorer.getNode.mockResolvedValue({
                folders: [],
                files: []
            });
            fileExplorer.getBreadcrumbs.mockResolvedValue([]);
        });

        it('should navigate back in history', async () => {
            browser.history = ['', '/folder1', '/folder1/subfolder'];
            browser.historyIndex = 2;
            browser.currentPath = '/folder1/subfolder';
            
            await browser.handleBack();
            
            expect(browser.historyIndex).toBe(1);
            expect(browser.currentPath).toBe('/folder1');
            expect(fileExplorer.getNode).toHaveBeenCalledWith('/folder1');
        });

        it('should disable back button at start', () => {
            browser.history = [''];
            browser.historyIndex = 0;
            browser.updateNavControls();
            
            const backBtn = container.querySelector('#mfb-back');
            expect(backBtn.disabled).toBe(true);
        });

        it('should disable forward button without forward history', () => {
            browser.history = ['', '/folder1'];
            browser.historyIndex = 1;
            browser.updateNavControls();
            
            const forwardBtn = container.querySelector('#mfb-forward');
            expect(forwardBtn.disabled).toBe(true);
        });

        it('should navigate to parent using up button', async () => {
            browser.breadcrumbs = [
                { path: '', name: 'Home' },
                { path: '/folder1', name: 'Folder 1' },
                { path: '/folder1/subfolder', name: 'Subfolder' }
            ];
            browser.currentPath = '/folder1/subfolder';
            
            await browser.handleUp();
            
            expect(browser.currentPath).toBe('/folder1');
            expect(fileExplorer.getNode).toHaveBeenCalled();
        });

        it('should navigate via quick access sidebar', async () => {
            const homeLink = container.querySelector('.mfb-sidebar-item[data-path=""]');
            
            await new Promise(resolve => {
                homeLink.click();
                setTimeout(resolve, 0);
            });
            
            expect(browser.currentPath).toBe('');
        });
    });

    describe('Sidebar', () => {
        beforeEach(() => {
            browser = new ModernFileBrowser('test-container');
        });

        it('should toggle sidebar visibility', () => {
            const sidebar = container.querySelector('#mfb-sidebar');
            const toggleBtn = container.querySelector('#mfb-toggle-sidebar');
            
            expect(sidebar.classList.contains('hidden')).toBe(false);
            
            toggleBtn.click();
            
            expect(sidebar.classList.contains('hidden')).toBe(true);
            expect(browser.options.showSidebar).toBe(false);
        });

        it('should persist sidebar state in localStorage', () => {
            const toggleBtn = container.querySelector('#mfb-toggle-sidebar');
            
            toggleBtn.click();
            
            expect(localStorage.setItem).toHaveBeenCalledWith('mfb-show-sidebar', 'false');
        });

        it('should navigate correctly from quick access items', async () => {
            const homeLink = container.querySelector('.mfb-sidebar-item[data-path=""]');
            
            fileExplorer.getRoot.mockResolvedValue({ folders: [], files: [] });
            fileExplorerState.state.academicYearId = 1;
            fileExplorerState.state.semesterId = 1;
            
            await new Promise(resolve => {
                homeLink.click();
                setTimeout(resolve, 0);
            });
            
            expect(browser.currentPath).toBe('');
        });
    });

    describe('File/Folder Interactions', () => {
        beforeEach(() => {
            browser = new ModernFileBrowser('test-container');
            fileExplorer.getNode.mockResolvedValue({
                folders: [],
                files: []
            });
            fileExplorer.getBreadcrumbs.mockResolvedValue([]);
        });

        it('should navigate into folder on click', async () => {
            browser.folders = [
                { path: '/folder1', name: 'Folder 1', itemsCount: 5 }
            ];
            browser.renderContent();
            
            const folderItem = container.querySelector('.mfb-item[data-type="folder"]');
            const clickEvent = new MouseEvent('click', { bubbles: true });
            
            await new Promise(resolve => {
                folderItem.dispatchEvent(clickEvent);
                setTimeout(resolve, 0);
            });
            
            expect(browser.currentPath).toBe('/folder1');
        });

        it('should open preview for previewable files', () => {
            browser.files = [
                { id: 1, name: 'document.pdf', extension: 'pdf', fileType: 'application/pdf' }
            ];
            FilePreviewButton.isPreviewable.mockReturnValue(true);
            
            browser.handleFileClick(new MouseEvent('click'), '1');
            
            expect(window.filePreviewButton.handlePreviewClick).toHaveBeenCalledWith(
                '1',
                'document.pdf',
                'application/pdf'
            );
        });

        it('should download non-previewable files', async () => {
            browser.files = [
                { id: 2, name: 'archive.zip', extension: 'zip', fileType: 'application/zip' }
            ];
            FilePreviewButton.isPreviewable.mockReturnValue(false);
            
            const mockBlob = new Blob(['test'], { type: 'application/zip' });
            const mockResponse = {
                ok: true,
                blob: jest.fn().mockResolvedValue(mockBlob),
                headers: {
                    get: jest.fn().mockReturnValue('attachment; filename="archive.zip"')
                }
            };
            fileExplorer.downloadFile.mockResolvedValue(mockResponse);
            
            // Mock URL.createObjectURL and document.createElement
            global.URL.createObjectURL = jest.fn(() => 'blob:test-url');
            global.URL.revokeObjectURL = jest.fn();
            
            const createElementSpy = jest.spyOn(document, 'createElement');
            const mockAnchor = {
                href: '',
                download: '',
                click: jest.fn(),
                remove: jest.fn()
            };
            createElementSpy.mockReturnValue(mockAnchor);
            document.body.appendChild = jest.fn();
            document.body.removeChild = jest.fn();
            
            await browser.handleFileDownload('2');
            
            expect(fileExplorer.downloadFile).toHaveBeenCalledWith('2');
            expect(mockAnchor.click).toHaveBeenCalled();
        });
    });

    describe('Status Bar', () => {
        beforeEach(() => {
            browser = new ModernFileBrowser('test-container');
        });

        it('should show correct item count', () => {
            browser.folders = [
                { path: '/folder1', name: 'Folder 1' }
            ];
            browser.files = [
                { id: 1, name: 'file1.pdf' },
                { id: 2, name: 'file2.txt' }
            ];
            
            browser.updateStatusBar();
            
            const countEl = container.querySelector('#mfb-status-count');
            expect(countEl.textContent).toBe('3 items');
        });

        it('should show singular form for single item', () => {
            browser.files = [
                { id: 1, name: 'file1.pdf' }
            ];
            
            browser.updateStatusBar();
            
            const countEl = container.querySelector('#mfb-status-count');
            expect(countEl.textContent).toBe('1 item');
        });

        it('should update on navigation', async () => {
            fileExplorer.getNode.mockResolvedValue({
                folders: [{ path: '/subfolder', name: 'Subfolder' }],
                files: [{ id: 1, name: 'file.pdf' }]
            });
            fileExplorer.getBreadcrumbs.mockResolvedValue([]);
            
            await browser.navigateTo('/folder1');
            
            const countEl = container.querySelector('#mfb-status-count');
            expect(countEl.textContent).toBe('2 items');
        });
    });

    describe('Loading States', () => {
        beforeEach(() => {
            browser = new ModernFileBrowser('test-container');
        });

        it('should show loading indicator during fetch', async () => {
            fileExplorer.getNode.mockImplementation(() => 
                new Promise(resolve => setTimeout(() => resolve({ folders: [], files: [] }), 100))
            );
            fileExplorer.getBreadcrumbs.mockResolvedValue([]);
            
            const loadPromise = browser.loadCurrentPath();
            
            // Check loading state immediately
            const viewContainer = container.querySelector('#mfb-main-view');
            expect(viewContainer.classList.contains('loading')).toBe(true);
            expect(viewContainer.innerHTML).toContain('Loading');
            
            await loadPromise;
        });

        it('should show empty state message', () => {
            browser.folders = [];
            browser.files = [];
            browser.renderContent();
            
            const viewContainer = container.querySelector('#mfb-main-view');
            const emptyState = viewContainer.querySelector('.mfb-empty-state');
            expect(emptyState).toBeTruthy();
            expect(emptyState.textContent).toContain('This folder is empty');
        });

        it('should show error state on failure', async () => {
            const error = new Error('Network error');
            fileExplorer.getNode.mockRejectedValue(error);
            
            await browser.loadCurrentPath();
            
            const viewContainer = container.querySelector('#mfb-main-view');
            const errorState = viewContainer.querySelector('.mfb-error-state');
            expect(errorState).toBeTruthy();
            expect(showToast).toHaveBeenCalledWith('Failed to load files', 'error');
        });
    });

    describe('Integration Tests', () => {
        beforeEach(() => {
            browser = new ModernFileBrowser('test-container', {
                role: 'PROFESSOR',
                viewMode: 'grid'
            });
        });

        it('should complete full navigation flow', async () => {
            // Mock initial root load
            fileExplorer.getRoot.mockResolvedValue({
                folders: [
                    { path: '/folder1', name: 'Folder 1', itemsCount: 2 }
                ],
                files: []
            });
            fileExplorer.getBreadcrumbs.mockResolvedValue([]);
            fileExplorerState.state.academicYearId = 1;
            fileExplorerState.state.semesterId = 1;
            
            // Load root
            await browser.loadRoot(1, 1);
            
            expect(browser.currentPath).toBe('');
            expect(browser.folders.length).toBe(1);
            
            // Navigate into folder
            fileExplorer.getNode.mockResolvedValue({
                folders: [],
                files: [
                    { id: 1, name: 'file.pdf', size: 1024, extension: 'pdf', uploadDate: new Date() }
                ]
            });
            fileExplorer.getBreadcrumbs.mockResolvedValue([
                { path: '/folder1', name: 'Folder 1' }
            ]);
            
            await browser.navigateTo('/folder1');
            
            expect(browser.currentPath).toBe('/folder1');
            expect(browser.files.length).toBe(1);
            expect(browser.history.length).toBe(2);
        });

        it('should handle view mode toggle with content re-render', () => {
            browser.folders = [
                { path: '/folder1', name: 'Folder 1', itemsCount: 5 }
            ];
            browser.files = [
                { id: 1, name: 'file1.pdf', size: 1024, extension: 'pdf', uploadDate: new Date() }
            ];
            
            browser.options.viewMode = 'grid';
            browser.renderContent();
            
            const viewContainer = container.querySelector('#mfb-main-view');
            expect(viewContainer.querySelector('.mfb-view-grid')).toBeTruthy();
            
            browser.toggleViewMode('list');
            
            expect(viewContainer.querySelector('.mfb-view-list')).toBeTruthy();
            expect(viewContainer.querySelector('.mfb-view-grid')).toBeFalsy();
        });
    });
});
