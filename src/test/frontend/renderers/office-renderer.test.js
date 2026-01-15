/**
 * @jest-environment jsdom
 */

import { jest } from '@jest/globals';
import { OfficeRenderer } from '../../../main/resources/static/js/renderers/office-renderer.js';

// Mock fetch
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

// Mock window.handleFileError
window.handleFileError = jest.fn();

// Mock URL.createObjectURL and URL.revokeObjectURL
global.URL.createObjectURL = jest.fn(() => 'blob:mock-url');
global.URL.revokeObjectURL = jest.fn();

describe('OfficeRenderer', () => {
    let renderer;
    let container;

    beforeEach(() => {
        // Reset all mocks
        jest.clearAllMocks();
        fetch.mockClear();
        localStorageMock.clear();
        sessionStorageMock.clear();
        window.handleFileError.mockClear();
        global.URL.createObjectURL.mockClear();
        global.URL.revokeObjectURL.mockClear();

        // Create container element
        container = document.createElement('div');
        container.id = 'test-container';
        document.body.appendChild(container);

        // Create renderer instance
        renderer = new OfficeRenderer();
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
        it('should create instance with default options', () => {
            const defaultRenderer = new OfficeRenderer();
            expect(defaultRenderer.options.preferredFormat).toBe('html');
            expect(defaultRenderer.currentFileId).toBeNull();
            expect(defaultRenderer.currentFormat).toBeNull();
            expect(defaultRenderer.conversionUrl).toBeNull();
        });

        it('should create instance with custom options', () => {
            const customRenderer = new OfficeRenderer({ preferredFormat: 'pdf' });
            expect(customRenderer.options.preferredFormat).toBe('pdf');
        });
    });

    describe('Document Rendering', () => {
        it('should render Word documents (.docx) as HTML', async () => {
            const htmlContent = '<html><body><h1>Test Document</h1></body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(1, container);

            expect(fetch).toHaveBeenCalledWith(
                '/api/file-explorer/files/1/office-preview',
                expect.objectContaining({
                    method: 'GET',
                    headers: expect.any(Object)
                })
            );

            // Check that iframe was created
            const iframe = container.querySelector('iframe');
            expect(iframe).toBeTruthy();
            expect(iframe.className).toContain('w-full');
            expect(iframe.className).toContain('h-full');
            expect(iframe.getAttribute('sandbox')).toBe('allow-same-origin');
        });

        it('should render Excel spreadsheets (.xlsx) as HTML', async () => {
            const htmlContent = '<html><body><table><tr><td>Cell 1</td></tr></table></body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(2, container);

            expect(fetch).toHaveBeenCalledWith(
                '/api/file-explorer/files/2/office-preview',
                expect.any(Object)
            );

            const iframe = container.querySelector('iframe');
            expect(iframe).toBeTruthy();
        });

        it('should render PowerPoint presentations (.pptx) as HTML', async () => {
            const htmlContent = '<html><body><div>Slide Content</div></body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(3, container);

            expect(fetch).toHaveBeenCalledWith(
                '/api/file-explorer/files/3/office-preview',
                expect.any(Object)
            );

            const iframe = container.querySelector('iframe');
            expect(iframe).toBeTruthy();
        });

        it('should handle legacy Word format (.doc)', async () => {
            const htmlContent = '<html><body><p>Legacy Document</p></body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(4, container);

            expect(fetch).toHaveBeenCalled();
            const iframe = container.querySelector('iframe');
            expect(iframe).toBeTruthy();
        });

        it('should handle legacy Excel format (.xls)', async () => {
            const htmlContent = '<html><body><table><tr><td>Legacy Spreadsheet</td></tr></table></body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(5, container);

            expect(fetch).toHaveBeenCalled();
            const iframe = container.querySelector('iframe');
            expect(iframe).toBeTruthy();
        });

        it('should handle legacy PowerPoint format (.ppt)', async () => {
            const htmlContent = '<html><body><div>Legacy Presentation</div></body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(6, container);

            expect(fetch).toHaveBeenCalled();
            const iframe = container.querySelector('iframe');
            expect(iframe).toBeTruthy();
        });

        it('should render PDF converted content', async () => {
            const pdfBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('application/pdf')
                },
                blob: jest.fn().mockResolvedValue(pdfBlob)
            });

            await renderer.render(7, container);

            expect(global.URL.createObjectURL).toHaveBeenCalledWith(pdfBlob);
            
            const iframe = container.querySelector('iframe');
            expect(iframe).toBeTruthy();
            expect(iframe.src).toBe('blob:mock-url');
            
            // Check for PDF badge
            const badge = container.querySelector('.bg-green-100');
            expect(badge).toBeTruthy();
            expect(badge.textContent).toContain('Converted to PDF');
        });

        it('should apply correct styling to HTML content', async () => {
            const htmlContent = '<html><body><h1>Styled Document</h1></body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(8, container);

            expect(container.className).toContain('office-renderer-container');
            expect(container.className).toContain('h-full');
            expect(container.className).toContain('overflow-auto');
            expect(container.className).toContain('bg-white');
            expect(container.className).toContain('dark:bg-gray-900');
        });
    });

    describe('Error Handling', () => {
        it('should show error for corrupt file', async () => {
            const errorResponse = {
                ok: false,
                status: 500,
                json: jest.fn().mockResolvedValue({
                    message: 'Unable to convert document. File may be corrupted.'
                })
            };

            fetch.mockResolvedValueOnce(errorResponse);

            await renderer.render(9, container);

            expect(window.handleFileError).toHaveBeenCalled();
            
            // Check for error message
            const errorDiv = container.querySelector('.text-center');
            expect(errorDiv).toBeTruthy();
            expect(errorDiv.textContent).toContain('Preview Unavailable');
            
            // Check for download button
            const downloadBtn = container.querySelector('.office-download-btn');
            expect(downloadBtn).toBeTruthy();
        });

        it('should show unsupported format message', async () => {
            const errorResponse = {
                ok: false,
                status: 400,
                json: jest.fn().mockResolvedValue({
                    message: 'File is not an Office document'
                })
            };

            fetch.mockResolvedValueOnce(errorResponse);

            await renderer.render(10, container);

            expect(window.handleFileError).toHaveBeenCalled();
            
            const errorDiv = container.querySelector('.text-center');
            expect(errorDiv).toBeTruthy();
        });

        it('should handle loading timeout', async () => {
            // Simulate network timeout
            fetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

            await renderer.render(11, container);

            expect(window.handleFileError).toHaveBeenCalled();
        });

        it('should show file not found error', async () => {
            const errorResponse = {
                ok: false,
                status: 404,
                json: jest.fn().mockResolvedValue({
                    message: 'File not found'
                })
            };

            fetch.mockResolvedValueOnce(errorResponse);

            await renderer.render(12, container);

            expect(window.handleFileError).toHaveBeenCalled();
            
            const errorDiv = container.querySelector('.text-center');
            expect(errorDiv).toBeTruthy();
            expect(errorDiv.textContent).toContain('File Not Found');
        });

        it('should handle permission denied error', async () => {
            const errorResponse = {
                ok: false,
                status: 403,
                json: jest.fn().mockResolvedValue({
                    message: 'You don\'t have permission to view this file'
                })
            };

            fetch.mockResolvedValueOnce(errorResponse);

            await renderer.render(13, container);

            expect(window.handleFileError).toHaveBeenCalled();
        });

        it('should throw error when container is missing', async () => {
            await expect(renderer.render(14, null)).rejects.toThrow('Container element is required');
        });
    });

    describe('Display', () => {
        it('should apply correct styling', async () => {
            const htmlContent = '<html><body>Content</body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(15, container);

            expect(container.className).toContain('office-renderer-container');
            expect(container.className).toContain('h-full');
            expect(container.className).toContain('overflow-auto');
        });

        it('should have responsive layout', async () => {
            const htmlContent = '<html><body>Content</body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(16, container);

            const iframe = container.querySelector('iframe');
            expect(iframe).toBeTruthy();
            expect(iframe.className).toContain('w-full');
            expect(iframe.className).toContain('h-full');
        });

        it('should be print-friendly with proper styling in iframe', async () => {
            const htmlContent = '<html><body><table><tr><td>Test</td></tr></table></body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(17, container);

            const iframe = container.querySelector('iframe');
            expect(iframe).toBeTruthy();
            
            // Wait for iframe content to load
            await new Promise(resolve => setTimeout(resolve, 100));
            
            const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
            const style = iframeDoc.querySelector('style');
            expect(style).toBeTruthy();
            expect(style.textContent).toContain('font-family');
            expect(style.textContent).toContain('max-width');
        });

        it('should handle binary format with conversion not available message', async () => {
            const binaryBlob = new Blob(['binary data'], { type: 'application/octet-stream' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('application/octet-stream')
                },
                blob: jest.fn().mockResolvedValue(binaryBlob)
            });

            await renderer.render(18, container);

            const messageDiv = container.querySelector('.text-center');
            expect(messageDiv).toBeTruthy();
            expect(messageDiv.textContent).toContain('Preview Not Available');
            
            const downloadBtn = container.querySelector('.office-download-btn');
            expect(downloadBtn).toBeTruthy();
        });
    });

    describe('Authentication', () => {
        it('should include auth token from localStorage', async () => {
            localStorageMock.getItem.mockReturnValue('test-token');
            const htmlContent = '<html><body>Content</body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(19, container);

            expect(fetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    headers: expect.objectContaining({
                        'Authorization': 'Bearer test-token'
                    })
                })
            );
        });

        it('should include auth token from sessionStorage if localStorage is empty', async () => {
            localStorageMock.getItem.mockReturnValue(null);
            sessionStorageMock.getItem.mockReturnValue('session-token');
            const htmlContent = '<html><body>Content</body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(20, container);

            expect(fetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    headers: expect.objectContaining({
                        'Authorization': 'Bearer session-token'
                    })
                })
            );
        });
    });

    describe('Download Functionality', () => {
        it('should trigger download when download button is clicked', async () => {
            const errorResponse = {
                ok: false,
                status: 500,
                json: jest.fn().mockResolvedValue({
                    message: 'Conversion failed'
                })
            };

            fetch.mockResolvedValueOnce(errorResponse);

            // Mock successful download
            const downloadBlob = new Blob(['file content'], { type: 'application/octet-stream' });
            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('attachment; filename="test.docx"')
                },
                blob: jest.fn().mockResolvedValue(downloadBlob)
            });

            // Mock createElement and click
            const mockLink = {
                href: '',
                download: '',
                click: jest.fn(),
                remove: jest.fn()
            };
            const originalCreateElement = document.createElement.bind(document);
            const createElementSpy = jest.spyOn(document, 'createElement').mockImplementation((tag) => {
                if (tag === 'a') {
                    return mockLink;
                }
                return originalCreateElement(tag);
            });
            const appendChildSpy = jest.spyOn(document.body, 'appendChild').mockImplementation(() => mockLink);
            const removeChildSpy = jest.spyOn(document.body, 'removeChild').mockImplementation(() => mockLink);

            await renderer.render(21, container);

            const downloadBtn = container.querySelector('.office-download-btn');
            expect(downloadBtn).toBeTruthy();

            downloadBtn.click();

            // Wait for async download
            await new Promise(resolve => setTimeout(resolve, 100));

            expect(fetch).toHaveBeenCalledWith(
                '/api/file-explorer/files/21/download',
                expect.any(Object)
            );

            createElementSpy.mockRestore();
            appendChildSpy.mockRestore();
            removeChildSpy.mockRestore();
        });
    });

    describe('Static Methods', () => {
        it('should support Word document MIME types', () => {
            expect(OfficeRenderer.supportsFormat('application/msword')).toBe(true);
            expect(OfficeRenderer.supportsFormat('application/vnd.openxmlformats-officedocument.wordprocessingml.document')).toBe(true);
        });

        it('should support Excel MIME types', () => {
            expect(OfficeRenderer.supportsFormat('application/vnd.ms-excel')).toBe(true);
            expect(OfficeRenderer.supportsFormat('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')).toBe(true);
        });

        it('should support PowerPoint MIME types', () => {
            expect(OfficeRenderer.supportsFormat('application/vnd.ms-powerpoint')).toBe(true);
            expect(OfficeRenderer.supportsFormat('application/vnd.openxmlformats-officedocument.presentationml.presentation')).toBe(true);
        });

        it('should not support non-Office MIME types', () => {
            expect(OfficeRenderer.supportsFormat('application/pdf')).toBe(false);
            expect(OfficeRenderer.supportsFormat('text/plain')).toBe(false);
            expect(OfficeRenderer.supportsFormat('image/png')).toBe(false);
        });

        it('should return false for null or undefined MIME types', () => {
            expect(OfficeRenderer.supportsFormat(null)).toBe(false);
            expect(OfficeRenderer.supportsFormat(undefined)).toBe(false);
            expect(OfficeRenderer.supportsFormat('')).toBe(false);
        });
    });

    describe('Instance Methods', () => {
        it('should return current format after rendering', async () => {
            const htmlContent = '<html><body>Content</body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            await renderer.render(22, container);

            expect(renderer.getCurrentFormat()).toBe('html');
        });

        it('should clean up resources on destroy', async () => {
            const pdfBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('application/pdf')
                },
                blob: jest.fn().mockResolvedValue(pdfBlob)
            });

            await renderer.render(23, container);

            expect(renderer.conversionUrl).toBeTruthy();

            renderer.destroy();

            expect(global.URL.revokeObjectURL).toHaveBeenCalled();
            expect(renderer.conversionUrl).toBeNull();
            expect(renderer.currentFileId).toBeNull();
            expect(renderer.currentFormat).toBeNull();
        });
    });

    describe('Format Detection', () => {
        it('should detect HTML format correctly', () => {
            const htmlContent = '<html><body>Content</body></html>';
            const blob = new Blob([htmlContent], { type: 'text/html' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('text/html; charset=utf-8')
                },
                blob: jest.fn().mockResolvedValue(blob)
            });

            return renderer.render(24, container).then(() => {
                expect(renderer.getCurrentFormat()).toBe('html');
            });
        });

        it('should detect PDF format correctly', () => {
            const pdfBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('application/pdf')
                },
                blob: jest.fn().mockResolvedValue(pdfBlob)
            });

            return renderer.render(25, container).then(() => {
                expect(renderer.getCurrentFormat()).toBe('pdf');
            });
        });

        it('should default to binary format for unknown content types', () => {
            const binaryBlob = new Blob(['data'], { type: 'application/octet-stream' });

            fetch.mockResolvedValueOnce({
                ok: true,
                headers: {
                    get: jest.fn().mockReturnValue('application/octet-stream')
                },
                blob: jest.fn().mockResolvedValue(binaryBlob)
            });

            return renderer.render(26, container).then(() => {
                expect(renderer.getCurrentFormat()).toBe('binary');
            });
        });
    });
});
