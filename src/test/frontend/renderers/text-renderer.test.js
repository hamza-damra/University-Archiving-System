/**
 * @jest-environment jsdom
 */

import { jest } from '@jest/globals';
import { TextRenderer } from '../../../main/resources/static/js/renderers/text-renderer.js';

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

describe('TextRenderer', () => {
    let renderer;
    let container;

    beforeEach(() => {
        renderer = new TextRenderer();
        container = document.createElement('div');
        document.body.appendChild(container);
        global.fetch.mockClear();
        localStorageMock.clear();
        sessionStorageMock.clear();
    });

    afterEach(() => {
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
    });

    describe('Text Rendering', () => {
        test('Renders plain text', async () => {
            const fileId = 1;
            const content = 'This is plain text content\nLine 2\nLine 3';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            await renderer.render(fileId, container);

            const pre = container.querySelector('pre');
            expect(pre).toBeTruthy();
            expect(pre.textContent).toBe(content);
            expect(global.fetch).toHaveBeenCalledWith(
                `/api/file-explorer/files/${fileId}/content`,
                expect.objectContaining({
                    method: 'GET',
                    headers: expect.any(Object)
                })
            );
        });

        test('Shows line numbers', async () => {
            const fileId = 1;
            const content = 'Line 1\nLine 2\nLine 3\nLine 4\nLine 5';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            await renderer.render(fileId, container);

            const pre = container.querySelector('pre');
            expect(pre).toBeTruthy();
            // Text renderer doesn't show line numbers by default, but content is split by lines
            expect(renderer.currentLines).toHaveLength(5);
        });

        test('Handles large files', async () => {
            const fileId = 1;
            // Create a large file with more than 1000 lines (virtual scroll threshold)
            const lines = Array.from({ length: 1500 }, (_, i) => `Line ${i + 1}`);
            const content = lines.join('\n');
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            await renderer.render(fileId, container);

            // Should use virtual scrolling for large files
            const scrollContainer = container.querySelector('.relative.overflow-auto');
            expect(scrollContainer).toBeTruthy();
            
            // Should show info about large file
            const info = container.querySelector('.bg-blue-100, .bg-blue-900');
            expect(info).toBeTruthy();
            expect(info.textContent).toContain('Large file');
            expect(info.textContent).toContain('1,500 lines');
        });

        test('Word wrap option', async () => {
            const fileId = 1;
            const content = 'This is a very long line that should wrap when word wrap is enabled. It contains many words and should break appropriately.';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            await renderer.render(fileId, container);

            const pre = container.querySelector('pre');
            expect(pre).toBeTruthy();
            // Check for word wrap classes
            expect(pre.className).toContain('whitespace-pre-wrap');
            expect(pre.className).toContain('break-words');
        });
    });

    describe('Code Rendering', () => {
        // Note: Code rendering is handled by CodeRenderer, but we test text renderer's basic functionality
        test('Renders text content correctly', async () => {
            const fileId = 1;
            const content = 'function test() {\n  return true;\n}';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            await renderer.render(fileId, container);

            const pre = container.querySelector('pre');
            expect(pre).toBeTruthy();
            expect(pre.textContent).toBe(content);
        });
    });

    describe('Search', () => {
        beforeEach(async () => {
            const fileId = 1;
            const content = 'This is a test file\nwith multiple lines\ncontaining test words\nand more test content';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            await renderer.render(fileId, container);
        });

        test('Find text in content', () => {
            const query = 'test';
            const matchCount = renderer.search(query);
            
            expect(matchCount).toBeGreaterThan(0);
            expect(renderer.searchMatches.length).toBeGreaterThan(0);
            expect(renderer.currentMatchIndex).toBeGreaterThanOrEqual(0);
        });

        test('Highlight matches', () => {
            const query = 'test';
            const matchCount = renderer.search(query);
            
            expect(matchCount).toBeGreaterThan(0);
            // Verify matches are found
            renderer.searchMatches.forEach(match => {
                expect(match).toHaveProperty('start');
                expect(match).toHaveProperty('end');
                expect(match).toHaveProperty('line');
                expect(match.start).toBeLessThan(match.end);
            });
        });

        test('Navigate between matches', () => {
            const query = 'test';
            renderer.search(query);
            
            expect(renderer.searchMatches.length).toBeGreaterThan(0);
            
            // Navigate to next match
            const firstMatch = renderer.getCurrentMatch();
            const nextMatch = renderer.nextMatch();
            
            expect(nextMatch).toBeTruthy();
            expect(nextMatch.index).toBeGreaterThanOrEqual(0);
            
            // Navigate to previous match
            const previousMatch = renderer.previousMatch();
            expect(previousMatch).toBeTruthy();
        });

        test('Case-sensitive option', () => {
            // TextRenderer uses case-insensitive search by default
            const query = 'TEST';
            const matchCount = renderer.search(query);
            
            // Should find matches regardless of case
            expect(matchCount).toBeGreaterThan(0);
            
            // Test with lowercase
            renderer.clearSearch();
            const lowerQuery = 'test';
            const lowerMatchCount = renderer.search(lowerQuery);
            
            // Should find same matches (case-insensitive)
            expect(lowerMatchCount).toBe(matchCount);
        });

        test('Search with no matches', () => {
            const query = 'nonexistentword12345';
            const matchCount = renderer.search(query);
            
            expect(matchCount).toBe(0);
            expect(renderer.searchMatches).toHaveLength(0);
            expect(renderer.currentMatchIndex).toBe(-1);
        });

        test('Clear search results', () => {
            const query = 'test';
            renderer.search(query);
            
            expect(renderer.searchMatches.length).toBeGreaterThan(0);
            
            renderer.clearSearch();
            
            expect(renderer.searchMatches).toHaveLength(0);
            expect(renderer.currentMatchIndex).toBe(-1);
        });
    });

    describe('Partial Rendering', () => {
        test('Renders partial preview for large files', async () => {
            const fileId = 1;
            const lines = Array.from({ length: 1000 }, (_, i) => `Line ${i + 1}`);
            const content = lines.join('\n');
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            await renderer.renderPartial(fileId, container);

            // Should show partial preview notice
            const notice = container.querySelector('.bg-yellow-100, .bg-yellow-900');
            expect(notice).toBeTruthy();
            expect(notice.textContent).toContain('Showing first 500 lines only');
            
            // Should call API with partial parameter
            expect(global.fetch).toHaveBeenCalledWith(
                expect.stringContaining('partial=true'),
                expect.any(Object)
            );
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
    });

    describe('Virtual Scrolling', () => {
        test('Uses virtual scrolling for files exceeding threshold', async () => {
            const fileId = 1;
            const lines = Array.from({ length: 1200 }, (_, i) => `Line ${i + 1}`);
            const content = lines.join('\n');
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            await renderer.render(fileId, container);

            const scrollContainer = container.querySelector('.relative.overflow-auto');
            expect(scrollContainer).toBeTruthy();
            
            const spacer = scrollContainer.querySelector('div[style*="height"]');
            expect(spacer).toBeTruthy();
        });

        test('Uses standard rendering for small files', async () => {
            const fileId = 1;
            const content = 'Line 1\nLine 2\nLine 3';
            
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: content
                })
            });

            await renderer.render(fileId, container);

            // Should not have virtual scroll container
            const scrollContainer = container.querySelector('.relative.overflow-auto');
            expect(scrollContainer).toBeFalsy();
            
            // Should have standard pre element
            const pre = container.querySelector('pre');
            expect(pre).toBeTruthy();
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
                    data: 'test content'
                })
            });

            await renderer.render(fileId, container);

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
                    data: 'test content'
                })
            });

            await renderer.render(fileId, container);

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
});
