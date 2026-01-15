/**
 * @jest-environment jsdom
 */

import { jest } from '@jest/globals';
import { CodeRenderer } from '../../../main/resources/static/js/renderers/code-renderer.js';

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
        clear: jest.fn(() => { store = {}; })
    };
})();
Object.defineProperty(window, 'sessionStorage', { value: sessionStorageMock });

// Mock Highlight.js
const mockHljs = {
    highlight: jest.fn((code, options) => ({
        value: `<span class="hljs-keyword">function</span> <span class="hljs-title">test</span>() { return <span class="hljs-literal">true</span>; }`,
        language: options.language
    })),
    highlightAuto: jest.fn((code) => ({
        value: `<span class="hljs-keyword">function</span> <span class="hljs-title">test</span>() { return <span class="hljs-literal">true</span>; }`,
        language: 'javascript'
    }))
};

describe('CodeRenderer', () => {
    let renderer;
    let container;

    beforeEach(() => {
        renderer = new CodeRenderer();
        container = document.createElement('div');
        document.body.appendChild(container);
        global.fetch.mockClear();
        localStorageMock.clear();
        sessionStorageMock.clear();
        
        // Reset Highlight.js mock
        mockHljs.highlight.mockClear();
        mockHljs.highlightAuto.mockClear();
        
        // Mock Highlight.js library
        global.hljs = mockHljs;
        
        // Mock document.createElement for link and script elements
        const originalCreateElement = document.createElement.bind(document);
        document.createElement = jest.fn((tagName) => {
            const element = originalCreateElement(tagName);
            if (tagName === 'link') {
                element.onload = null;
            } else if (tagName === 'script') {
                element.onload = null;
                element.onerror = null;
            }
            return element;
        });
    });

    afterEach(() => {
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
        delete global.hljs;
    });

    describe('Code Rendering', () => {
        test('Syntax highlighting applied', async () => {
            const fileId = 1;
            const content = 'function test() { return true; }';
            const language = 'javascript';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            // Mock Highlight.js as loaded
            renderer.highlightJsLoaded = true;

            await renderer.render(fileId, container, language);

            expect(mockHljs.highlight).toHaveBeenCalledWith(
                content,
                expect.objectContaining({
                    language: language,
                    ignoreIllegals: true
                })
            );

            const code = container.querySelector('code');
            expect(code).toBeTruthy();
            expect(code.innerHTML).toContain('hljs-keyword');
        });

        test('Detects language from extension', () => {
            expect(CodeRenderer.detectLanguage('test.js')).toBe('javascript');
            expect(CodeRenderer.detectLanguage('test.java')).toBe('java');
            expect(CodeRenderer.detectLanguage('test.py')).toBe('python');
            expect(CodeRenderer.detectLanguage('test.css')).toBe('css');
            expect(CodeRenderer.detectLanguage('test.html')).toBe('html');
            expect(CodeRenderer.detectLanguage('test.sql')).toBe('sql');
            expect(CodeRenderer.detectLanguage('test.xml')).toBe('xml');
            expect(CodeRenderer.detectLanguage('test.json')).toBe('json');
            expect(CodeRenderer.detectLanguage('test.ts')).toBe('typescript');
            expect(CodeRenderer.detectLanguage('test.cpp')).toBe('cpp');
            expect(CodeRenderer.detectLanguage('test.go')).toBe('go');
            expect(CodeRenderer.detectLanguage('test.rs')).toBe('rust');
        });

        test('Returns null for unknown extensions', () => {
            expect(CodeRenderer.detectLanguage('test.unknown')).toBeNull();
            expect(CodeRenderer.detectLanguage('test')).toBeNull();
            expect(CodeRenderer.detectLanguage('')).toBeNull();
            expect(CodeRenderer.detectLanguage(null)).toBeNull();
        });

        test('Line numbers shown', async () => {
            const fileId = 1;
            const content = 'Line 1\nLine 2\nLine 3\nLine 4\nLine 5';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            renderer.highlightJsLoaded = true;

            await renderer.render(fileId, container, 'javascript');

            const lineNumbers = container.querySelector('.line-numbers');
            expect(lineNumbers).toBeTruthy();
            
            // Check that line numbers are created
            const lineNumberElements = lineNumbers.querySelectorAll('div');
            expect(lineNumberElements.length).toBe(5);
        });

        test('Line numbers can be disabled', async () => {
            const fileId = 1;
            const content = 'function test() { return true; }';
            
            const rendererNoLineNumbers = new CodeRenderer({ showLineNumbers: false });
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            rendererNoLineNumbers.highlightJsLoaded = true;

            await rendererNoLineNumbers.render(fileId, container, 'javascript');

            const lineNumbers = container.querySelector('.line-numbers');
            expect(lineNumbers).toBeFalsy();
        });

        test('Theme applied (light/dark)', async () => {
            const fileId = 1;
            const content = 'function test() { return true; }';
            
            // Test with default theme (github)
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            renderer.highlightJsLoaded = true;
            await renderer.render(fileId, container, 'javascript');

            // Check container has theme classes
            expect(container.className).toContain('bg-gray-50');
            expect(container.className).toContain('dark:bg-gray-900');

            // Test with custom theme
            const darkRenderer = new CodeRenderer({ theme: 'monokai' });
            const darkContainer = document.createElement('div');
            document.body.appendChild(darkContainer);

            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            darkRenderer.highlightJsLoaded = true;
            await darkRenderer.render(fileId, darkContainer, 'javascript');

            expect(darkRenderer.options.theme).toBe('monokai');
            
            if (darkContainer.parentNode) {
                darkContainer.parentNode.removeChild(darkContainer);
            }
        });

        test('Auto-detects language when not specified', async () => {
            const fileId = 1;
            const content = 'function test() { return true; }';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            renderer.highlightJsLoaded = true;

            await renderer.render(fileId, container);

            expect(mockHljs.highlightAuto).toHaveBeenCalledWith(content);
            expect(renderer.currentLanguage).toBe('javascript');
        });

        test('Shows language badge', async () => {
            const fileId = 1;
            const content = 'function test() { return true; }';
            const language = 'javascript';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            renderer.highlightJsLoaded = true;

            await renderer.render(fileId, container, language);

            const badge = container.querySelector('.bg-blue-100, .bg-blue-900');
            expect(badge).toBeTruthy();
            expect(badge.textContent).toBe('JAVASCRIPT');
        });
    });

    describe('Highlight.js Loading', () => {
        test('Uses existing Highlight.js if already loaded', async () => {
            const fileId = 1;
            const content = 'function test() { return true; }';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            // Highlight.js is already defined in beforeEach
            await renderer.render(fileId, container, 'javascript');

            expect(renderer.highlightJsLoaded).toBe(true);
        });

        test('Loads Highlight.js from CDN if not available', async () => {
            const fileId = 1;
            const content = 'function test() { return true; }';
            
            // Remove hljs temporarily
            delete global.hljs;
            renderer.highlightJsLoaded = false;
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            // Mock script and link loading
            const mockScript = {
                onload: null,
                onerror: null,
                src: ''
            };
            const mockLink = {
                rel: '',
                href: ''
            };

            document.createElement = jest.fn((tagName) => {
                if (tagName === 'script') {
                    return mockScript;
                } else if (tagName === 'link') {
                    return mockLink;
                }
                return document.createElement.call(document, tagName);
            });

            // Simulate script load
            const renderPromise = renderer.render(fileId, container, 'javascript');
            
            // Simulate script loading
            setTimeout(() => {
                global.hljs = mockHljs;
                if (mockScript.onload) {
                    mockScript.onload();
                }
            }, 10);

            await renderPromise;

            expect(document.createElement).toHaveBeenCalledWith('script');
            expect(document.createElement).toHaveBeenCalledWith('link');
        });

        test('Handles Highlight.js loading failure gracefully', async () => {
            const fileId = 1;
            const content = 'function test() { return true; }';
            
            delete global.hljs;
            renderer.highlightJsLoaded = false;
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            const mockScript = {
                onload: null,
                onerror: null,
                src: ''
            };
            const mockLink = {
                rel: '',
                href: ''
            };

            document.createElement = jest.fn((tagName) => {
                if (tagName === 'script') {
                    return mockScript;
                } else if (tagName === 'link') {
                    return mockLink;
                }
                return document.createElement.call(document, tagName);
            });

            const renderPromise = renderer.render(fileId, container, 'javascript');
            
            // Simulate script loading error
            setTimeout(() => {
                if (mockScript.onerror) {
                    mockScript.onerror();
                }
            }, 10);

            await expect(renderPromise).rejects.toThrow('Failed to load Highlight.js library');
        });
    });

    describe('Error Handling', () => {
        test('Handles missing container', async () => {
            const fileId = 1;
            
            await expect(renderer.render(fileId, null)).rejects.toThrow('Container element is required');
        });

        test('Handles 404 error', async () => {
            const fileId = 999;
            
            global.fetch.mockResolvedValueOnce({
                ok: false,
                status: 404,
                json: async () => ({
                    success: false,
                    message: 'File not found'
                })
            });

            await expect(renderer.render(fileId, container)).rejects.toThrow('File not found');
        });

        test('Handles 403 error', async () => {
            const fileId = 1;
            
            global.fetch.mockResolvedValueOnce({
                ok: false,
                status: 403,
                json: async () => ({
                    success: false,
                    message: 'Access denied'
                })
            });

            await expect(renderer.render(fileId, container)).rejects.toThrow('You don\'t have permission');
        });

        test('Handles network error', async () => {
            const fileId = 1;
            
            global.fetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

            await expect(renderer.render(fileId, container)).rejects.toThrow('Network error');
        });

        test('Falls back to plain text on highlighting error', async () => {
            const fileId = 1;
            const content = 'function test() { return true; }';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            renderer.highlightJsLoaded = true;
            
            // Mock highlighting to throw error
            mockHljs.highlight.mockImplementation(() => {
                throw new Error('Highlighting failed');
            });

            const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();

            await renderer.render(fileId, container, 'javascript');

            const code = container.querySelector('code');
            expect(code).toBeTruthy();
            expect(code.textContent).toBe(content);
            expect(consoleSpy).toHaveBeenCalledWith(
                'Syntax highlighting failed, showing plain text:',
                expect.any(Error)
            );

            consoleSpy.mockRestore();
        });
    });

    describe('Content Management', () => {
        test('Stores current content', async () => {
            const fileId = 1;
            const content = 'function test() { return true; }';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            renderer.highlightJsLoaded = true;

            await renderer.render(fileId, container, 'javascript');

            expect(renderer.getCurrentContent()).toBe(content);
        });

        test('Stores current language', async () => {
            const fileId = 1;
            const content = 'function test() { return true; }';
            const language = 'javascript';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            renderer.highlightJsLoaded = true;

            await renderer.render(fileId, container, language);

            expect(renderer.getCurrentLanguage()).toBe(language);
        });
    });

    describe('Authentication', () => {
        test('Includes auth token in request headers', async () => {
            const fileId = 1;
            const token = 'test-token-123';
            localStorageMock.setItem('token', token);
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: 'function test() { return true; }'
                })
            });

            renderer.highlightJsLoaded = true;

            await renderer.render(fileId, container, 'javascript');

            expect(global.fetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    headers: expect.objectContaining({
                        'Authorization': `Bearer ${token}`
                    })
                })
            );
        });

        test('Falls back to sessionStorage token', async () => {
            const fileId = 1;
            const token = 'session-token-456';
            sessionStorageMock.setItem('token', token);
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: 'function test() { return true; }'
                })
            });

            renderer.highlightJsLoaded = true;

            await renderer.render(fileId, container, 'javascript');

            expect(global.fetch).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    headers: expect.objectContaining({
                        'Authorization': `Bearer ${token}`
                    })
                })
            );
        });
    });

    describe('Language Detection Edge Cases', () => {
        test('Handles case-insensitive file extensions', () => {
            expect(CodeRenderer.detectLanguage('test.JS')).toBe('javascript');
            expect(CodeRenderer.detectLanguage('test.JAVA')).toBe('java');
            expect(CodeRenderer.detectLanguage('test.PY')).toBe('python');
        });

        test('Handles files with multiple dots', () => {
            expect(CodeRenderer.detectLanguage('test.min.js')).toBe('javascript');
            expect(CodeRenderer.detectLanguage('file.test.java')).toBe('java');
        });

        test('Handles TypeScript variants', () => {
            expect(CodeRenderer.detectLanguage('test.ts')).toBe('typescript');
            expect(CodeRenderer.detectLanguage('test.tsx')).toBe('typescript');
        });

        test('Handles C++ variants', () => {
            expect(CodeRenderer.detectLanguage('test.cpp')).toBe('cpp');
            expect(CodeRenderer.detectLanguage('test.cc')).toBe('cpp');
            expect(CodeRenderer.detectLanguage('test.cxx')).toBe('cpp');
            expect(CodeRenderer.detectLanguage('test.hpp')).toBe('cpp');
        });
    });
});
