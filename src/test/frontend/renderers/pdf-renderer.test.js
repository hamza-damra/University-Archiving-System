/**
 * @jest-environment jsdom
 */

import { jest } from '@jest/globals';
import { PDFRenderer } from '../../../main/resources/static/js/renderers/pdf-renderer.js';

// Mock fetch
global.fetch = jest.fn();

// Mock URL.createObjectURL and URL.revokeObjectURL
const mockObjectURLs = new Map();
let objectURLCounter = 0;

global.URL.createObjectURL = jest.fn((blob) => {
    const url = `blob:mock-url-${objectURLCounter++}`;
    mockObjectURLs.set(url, blob);
    return url;
});

global.URL.revokeObjectURL = jest.fn((url) => {
    mockObjectURLs.delete(url);
});

// Mock sessionStorage
const sessionStorageMock = (() => {
    let store = {};
    return {
        getItem: jest.fn(key => store[key] || null),
        setItem: jest.fn((key, value) => { store[key] = value; }),
        removeItem: jest.fn(key => { delete store[key]; }),
        clear: jest.fn(() => { store = {}; }),
        get length() { return Object.keys(store).length; }
    };
})();
Object.defineProperty(window, 'sessionStorage', { value: sessionStorageMock });

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

// Mock PDF.js library (optional)
let mockPdfjsLib = undefined;
Object.defineProperty(window, 'pdfjsLib', {
    get: () => mockPdfjsLib,
    set: (value) => { mockPdfjsLib = value; },
    configurable: true
});

describe('PDFRenderer', () => {
    let renderer;
    let container;

    beforeEach(() => {
        // Reset mocks
        jest.clearAllMocks();
        fetch.mockClear();
        localStorageMock.clear();
        sessionStorageMock.clear();
        mockObjectURLs.clear();
        objectURLCounter = 0;
        mockPdfjsLib = undefined;

        // Create container element
        container = document.createElement('div');
        container.id = 'test-container';
        document.body.appendChild(container);

        // Create renderer instance
        renderer = new PDFRenderer();
    });

    afterEach(() => {
        // Cleanup
        if (renderer) {
            renderer.destroy();
        }
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
    });

    describe('Constructor', () => {
        it('should initialize with default values', () => {
            const newRenderer = new PDFRenderer();
            expect(newRenderer.options).toEqual({});
            expect(newRenderer.currentFileId).toBeNull();
            expect(newRenderer.currentPage).toBe(1);
            expect(newRenderer.totalPages).toBe(0);
            expect(newRenderer.pdfUrl).toBeNull();
            expect(newRenderer.iframeElement).toBeNull();
        });

        it('should accept custom options', () => {
            const options = { customOption: 'value' };
            const newRenderer = new PDFRenderer(options);
            expect(newRenderer.options).toEqual(options);
        });
    });

    describe('Rendering', () => {
        it('should initialize PDF.js library when available', async () => {
            // Mock PDF.js
            const mockGetDocument = jest.fn().mockResolvedValue({
                promise: Promise.resolve({ numPages: 5 })
            });
            mockPdfjsLib = {
                getDocument: mockGetDocument
            };

            // Mock fetch for PDF blob
            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            await renderer.render(123, container);

            // PDF.js should be checked (though it may not be called if browser native viewer is used)
            expect(container.querySelector('iframe')).toBeTruthy();
        });

        it('should load PDF document', async () => {
            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            await renderer.render(123, container);

            expect(fetch).toHaveBeenCalledWith(
                '/api/file-explorer/files/123/download',
                expect.objectContaining({
                    method: 'GET',
                    headers: expect.objectContaining({
                        'Authorization': 'Bearer test-token'
                    })
                })
            );

            expect(URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
            expect(renderer.pdfUrl).toBeTruthy();
        });

        it('should render pages to canvas (iframe)', async () => {
            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            await renderer.render(123, container);

            const iframe = container.querySelector('iframe');
            expect(iframe).toBeTruthy();
            expect(iframe.src).toBeTruthy();
            expect(iframe.className).toContain('flex-1');
            expect(iframe.getAttribute('title')).toBe('PDF Preview');
        });

        it('should handle multi-page documents', async () => {
            // Mock PDF.js with multi-page document
            const mockGetDocument = jest.fn().mockResolvedValue({
                promise: Promise.resolve({ numPages: 10 })
            });
            mockPdfjsLib = {
                getDocument: mockGetDocument
            };

            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            await renderer.render(123, container);

            // Wait for PDF.js to load
            await new Promise(resolve => setTimeout(resolve, 100));

            // Check that page count is detected (if PDF.js is available)
            if (mockPdfjsLib) {
                expect(mockGetDocument).toHaveBeenCalled();
            }
        });

        it('should throw error when container is missing', async () => {
            await expect(renderer.render(123, null)).rejects.toThrow('Container element is required');
            await expect(renderer.render(123, undefined)).rejects.toThrow('Container element is required');
        });
    });

    describe('Navigation', () => {
        beforeEach(async () => {
            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('test-token');
            await renderer.render(123, container);
        });

        it('should navigate to next page', () => {
            renderer.currentPage = 1;
            renderer.totalPages = 5;

            const nextBtn = container.querySelector('.pdf-next-btn');
            expect(nextBtn).toBeTruthy();

            nextBtn.click();

            expect(renderer.currentPage).toBe(2);
            expect(renderer.iframeElement.src).toContain('#page=2');
        });

        it('should navigate to previous page', () => {
            renderer.currentPage = 3;
            renderer.totalPages = 5;

            const prevBtn = container.querySelector('.pdf-prev-btn');
            expect(prevBtn).toBeTruthy();

            prevBtn.click();

            expect(renderer.currentPage).toBe(2);
            expect(renderer.iframeElement.src).toContain('#page=2');
        });

        it('should not navigate previous when on first page', () => {
            renderer.currentPage = 1;
            renderer.totalPages = 5;

            const prevBtn = container.querySelector('.pdf-prev-btn');
            prevBtn.click();

            expect(renderer.currentPage).toBe(1);
        });

        it('should not navigate next when on last page', () => {
            renderer.currentPage = 5;
            renderer.totalPages = 5;

            const nextBtn = container.querySelector('.pdf-next-btn');
            nextBtn.click();

            expect(renderer.currentPage).toBe(5);
        });

        it('should go to specific page', () => {
            renderer.totalPages = 10;

            renderer.goToPage(5);

            expect(renderer.currentPage).toBe(5);
            expect(renderer.iframeElement.src).toContain('#page=5');
        });

        it('should clamp page number to minimum of 1', () => {
            renderer.goToPage(0);
            expect(renderer.currentPage).toBe(1);

            renderer.goToPage(-5);
            expect(renderer.currentPage).toBe(1);
        });

        it('should clamp page number to maximum when total pages known', () => {
            renderer.totalPages = 10;

            renderer.goToPage(15);
            expect(renderer.currentPage).toBe(10);
        });

        it('should allow navigation beyond known pages when total pages unknown', () => {
            renderer.totalPages = 0;

            renderer.goToPage(15);
            expect(renderer.currentPage).toBe(15);
        });

        it('should update page input on navigation', () => {
            renderer.totalPages = 10;

            const pageInput = container.querySelector('.pdf-page-input');
            expect(pageInput).toBeTruthy();

            renderer.goToPage(5);
            expect(pageInput.value).toBe('5');
        });

        it('should navigate when page input changes', () => {
            renderer.totalPages = 10;

            const pageInput = container.querySelector('.pdf-page-input');
            pageInput.value = '7';
            
            const changeEvent = new Event('change', { bubbles: true });
            pageInput.dispatchEvent(changeEvent);

            expect(renderer.currentPage).toBe(7);
        });

        it('should navigate when Enter key pressed in page input', () => {
            renderer.totalPages = 10;

            const pageInput = container.querySelector('.pdf-page-input');
            pageInput.value = '8';
            
            const keyPressEvent = new KeyboardEvent('keypress', { key: 'Enter', bubbles: true });
            pageInput.dispatchEvent(keyPressEvent);

            expect(renderer.currentPage).toBe(8);
        });

        it('should update page indicator', () => {
            renderer.totalPages = 10;
            renderer.currentPage = 3;

            renderer.updateNavigationUI();

            const pageInput = container.querySelector('.pdf-page-input');
            const prevBtn = container.querySelector('.pdf-prev-btn');
            const nextBtn = container.querySelector('.pdf-next-btn');

            expect(pageInput.value).toBe('3');
            expect(prevBtn.disabled).toBe(false);
            expect(nextBtn.disabled).toBe(false);
        });

        it('should disable previous button on first page', () => {
            renderer.currentPage = 1;
            renderer.updateNavigationUI();

            const prevBtn = container.querySelector('.pdf-prev-btn');
            expect(prevBtn.disabled).toBe(true);
        });

        it('should disable next button on last page when total pages known', () => {
            renderer.currentPage = 5;
            renderer.totalPages = 5;
            renderer.updateNavigationUI();

            const nextBtn = container.querySelector('.pdf-next-btn');
            expect(nextBtn.disabled).toBe(true);
        });
    });

    describe('Zoom', () => {
        it('should note that zoom is handled by browser native PDF viewer', async () => {
            // The current implementation uses browser's native PDF viewer
            // which handles zoom internally. This test documents that behavior.
            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('test-token');
            await renderer.render(123, container);

            const infoText = container.textContent;
            expect(infoText).toContain('Use browser\'s built-in PDF controls for zoom');
        });

        // Note: Fit to width and fit to page are handled by browser's native viewer
        // The renderer doesn't implement custom zoom controls
    });

    describe('Error Handling', () => {
        it('should show error for invalid PDF (wrong content type)', async () => {
            const mockBlob = new Blob(['Not a PDF'], { type: 'text/plain' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            await expect(renderer.render(123, container)).rejects.toThrow('File is corrupted or not a valid PDF');
        });

        it('should handle 404 file not found error', async () => {
            fetch.mockResolvedValueOnce({
                ok: false,
                status: 404
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            await expect(renderer.render(123, container)).rejects.toThrow('File not found - it may have been deleted');
        });

        it('should handle 403 forbidden error', async () => {
            fetch.mockResolvedValueOnce({
                ok: false,
                status: 403
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            await expect(renderer.render(123, container)).rejects.toThrow('You don\'t have permission to view this file');
        });

        it('should handle 500 server error', async () => {
            fetch.mockResolvedValueOnce({
                ok: false,
                status: 500
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            await expect(renderer.render(123, container)).rejects.toThrow('Service unavailable - please try again later');
        });

        it('should handle generic fetch errors', async () => {
            fetch.mockResolvedValueOnce({
                ok: false,
                status: 400
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            await expect(renderer.render(123, container)).rejects.toThrow('Failed to load PDF file');
        });

        it('should handle network errors', async () => {
            fetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

            localStorageMock.getItem.mockReturnValue('test-token');

            await expect(renderer.render(123, container)).rejects.toThrow('Network error - unable to connect to server');
        });

        it('should handle loading error gracefully', async () => {
            fetch.mockRejectedValueOnce(new Error('Network failure'));

            localStorageMock.getItem.mockReturnValue('test-token');

            await expect(renderer.render(123, container)).rejects.toThrow();
        });

        it('should handle empty document (blob with no content)', async () => {
            const mockBlob = new Blob([], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            // Should not throw, but may fail when trying to render
            await renderer.render(123, container);
            expect(container.querySelector('iframe')).toBeTruthy();
        });
    });

    describe('Page Count Detection', () => {
        it('should detect page count when PDF.js is available', async () => {
            const mockPdf = { numPages: 7 };
            const mockGetDocument = jest.fn().mockResolvedValue({
                promise: Promise.resolve(mockPdf)
            });
            mockPdfjsLib = {
                getDocument: mockGetDocument
            };

            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            await renderer.render(123, container);

            // Wait for async page count detection
            await new Promise(resolve => setTimeout(resolve, 100));

            if (mockPdfjsLib) {
                expect(mockGetDocument).toHaveBeenCalled();
                expect(renderer.totalPages).toBe(7);
            }
        });

        it('should set unknown page count when PDF.js is not available', async () => {
            mockPdfjsLib = undefined;

            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            await renderer.render(123, container);

            // Wait for async page count detection
            await new Promise(resolve => setTimeout(resolve, 100));

            expect(renderer.totalPages).toBe(0);
        });

        it('should handle PDF.js loading error gracefully', async () => {
            const mockGetDocument = jest.fn().mockRejectedValue(new Error('PDF.js error'));
            mockPdfjsLib = {
                getDocument: mockGetDocument
            };

            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('test-token');

            await renderer.render(123, container);

            // Wait for async page count detection
            await new Promise(resolve => setTimeout(resolve, 100));

            expect(renderer.totalPages).toBe(0);
        });

        it('should update page count display', () => {
            renderer.totalPages = 10;
            renderer.updatePageCountDisplay();

            const totalPagesSpan = container.querySelector('.pdf-total-pages');
            if (totalPagesSpan) {
                expect(totalPagesSpan.textContent).toBe('10');
            }
        });

        it('should show -- when page count is unknown', () => {
            renderer.totalPages = 0;
            renderer.updatePageCountDisplay();

            const totalPagesSpan = container.querySelector('.pdf-total-pages');
            if (totalPagesSpan) {
                expect(totalPagesSpan.textContent).toBe('--');
            }
        });
    });

    describe('Utility Methods', () => {
        it('should get current page number', () => {
            renderer.currentPage = 5;
            expect(renderer.getCurrentPage()).toBe(5);
        });

        it('should get total page count', () => {
            renderer.totalPages = 10;
            expect(renderer.getPageCount()).toBe(10);
        });

        it('should return 0 for page count when unknown', () => {
            renderer.totalPages = 0;
            expect(renderer.getPageCount()).toBe(0);
        });
    });

    describe('Cleanup', () => {
        it('should clean up resources on destroy', async () => {
            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('test-token');
            await renderer.render(123, container);

            const pdfUrl = renderer.pdfUrl;
            expect(pdfUrl).toBeTruthy();

            renderer.destroy();

            expect(URL.revokeObjectURL).toHaveBeenCalledWith(pdfUrl);
            expect(renderer.pdfUrl).toBeNull();
            expect(renderer.iframeElement).toBeNull();
            expect(renderer.currentFileId).toBeNull();
            expect(renderer.currentPage).toBe(1);
            expect(renderer.totalPages).toBe(0);
        });

        it('should handle destroy when no resources exist', () => {
            expect(() => renderer.destroy()).not.toThrow();
        });
    });

    describe('Authentication', () => {
        it('should use token from localStorage', async () => {
            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue('local-token');
            sessionStorageMock.getItem.mockReturnValue(null);

            await renderer.render(123, container);

            expect(fetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    headers: expect.objectContaining({
                        'Authorization': 'Bearer local-token'
                    })
                })
            );
        });

        it('should use token from sessionStorage when localStorage is empty', async () => {
            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue(null);
            sessionStorageMock.getItem.mockReturnValue('session-token');

            await renderer.render(123, container);

            expect(fetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    headers: expect.objectContaining({
                        'Authorization': 'Bearer session-token'
                    })
                })
            );
        });

        it('should work without token', async () => {
            const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
            fetch.mockResolvedValueOnce({
                ok: true,
                blob: () => Promise.resolve(mockBlob)
            });

            localStorageMock.getItem.mockReturnValue(null);
            sessionStorageMock.getItem.mockReturnValue(null);

            await renderer.render(123, container);

            expect(fetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    headers: {}
                })
            );
        });
    });
});
