/**
 * @jest-environment jsdom
 */

const fs = require('fs');
const path = require('path');

// Read the actual auth-guard.js file
const authGuardCode = fs.readFileSync(
    path.join(__dirname, '../../../main/resources/static/js/auth/auth-guard.js'),
    'utf-8'
);

describe('Auth Guard', () => {
    let originalLocation;
    let originalFetch;
    let originalLocalStorage;
    let originalSetTimeout;
    let originalClearTimeout;
    let localStorageStore;
    let mockLocation;
    let mockFetch;
    let timeoutCallbacks;
    let executedTimeouts;

    beforeEach(() => {
        // Save originals
        originalLocation = window.location;
        originalFetch = global.fetch;
        originalLocalStorage = global.localStorage;
        originalSetTimeout = global.setTimeout;
        originalClearTimeout = global.clearTimeout;

        // Mock localStorage
        localStorageStore = {};
        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: jest.fn((key) => localStorageStore[key] || null),
                setItem: jest.fn((key, value) => { localStorageStore[key] = value; }),
                removeItem: jest.fn((key) => { delete localStorageStore[key]; }),
                clear: jest.fn(() => { localStorageStore = {}; })
            },
            writable: true,
            configurable: true
        });

        // Mock location
        mockLocation = {
            pathname: '/hod-dashboard.html',
            origin: 'http://localhost:8080',
            replace: jest.fn((url) => {
                const urlObj = new URL(url, mockLocation.origin);
                mockLocation.pathname = urlObj.pathname;
                mockLocation.search = urlObj.search;
            }),
            search: '',
            href: 'http://localhost:8080/hod-dashboard.html'
        };
        delete window.location;
        Object.defineProperty(window, 'location', {
            value: mockLocation,
            writable: true,
            configurable: true
        });

        // Mock fetch
        mockFetch = jest.fn();
        global.fetch = mockFetch;

        // Mock setTimeout/clearTimeout with ability to execute
        timeoutCallbacks = new Map();
        executedTimeouts = new Set();
        let timeoutIdCounter = 0;
        
        global.setTimeout = jest.fn((callback, delay) => {
            const id = ++timeoutIdCounter;
            timeoutCallbacks.set(id, { callback, delay, id });
            return id;
        });
        
        global.clearTimeout = jest.fn((id) => {
            timeoutCallbacks.delete(id);
            executedTimeouts.delete(id);
        });

        // Clear all mocks
        jest.clearAllMocks();
    });

    afterEach(() => {
        // Restore originals
        if (originalLocation) {
            Object.defineProperty(window, 'location', {
                value: originalLocation,
                writable: true,
                configurable: true
            });
        }
        global.fetch = originalFetch;
        global.localStorage = originalLocalStorage;
        global.setTimeout = originalSetTimeout;
        global.clearTimeout = originalClearTimeout;
    });

    /**
     * Helper to execute the auth guard code
     */
    function executeAuthGuard() {
        // Execute the IIFE code
        eval(authGuardCode);
    }

    /**
     * Helper to flush pending promises
     */
    async function flushPromises() {
        return new Promise(resolve => setImmediate(resolve));
    }

    /**
     * Helper to execute a specific timeout
     */
    function executeTimeout(id) {
        const timeout = timeoutCallbacks.get(id);
        if (timeout && !executedTimeouts.has(id)) {
            executedTimeouts.add(id);
            timeout.callback();
        }
    }

    describe('Login Page Skip', () => {
        it('should skip guard on login page (/)', () => {
            mockLocation.pathname = '/';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_ADMIN', email: 'admin@test.com' });

            executeAuthGuard();

            expect(mockLocation.replace).not.toHaveBeenCalled();
            expect(mockFetch).not.toHaveBeenCalled();
        });

        it('should skip guard on login page (/index.html)', () => {
            mockLocation.pathname = '/index.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_ADMIN', email: 'admin@test.com' });

            executeAuthGuard();

            expect(mockLocation.replace).not.toHaveBeenCalled();
            expect(mockFetch).not.toHaveBeenCalled();
        });
    });

    describe('Missing Authentication Data', () => {
        it('should redirect to login when no token present', () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_HOD', email: 'hod@test.com' });

            executeAuthGuard();

            expect(window.localStorage.removeItem).toHaveBeenCalledWith('token');
            expect(window.localStorage.removeItem).toHaveBeenCalledWith('refreshToken');
            expect(window.localStorage.removeItem).toHaveBeenCalledWith('userInfo');
            expect(mockLocation.replace).toHaveBeenCalledWith('/index.html?error=unauthorized');
        });

        it('should redirect to login when no userInfo present', () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'test-token';

            executeAuthGuard();

            expect(window.localStorage.removeItem).toHaveBeenCalledWith('token');
            expect(window.localStorage.removeItem).toHaveBeenCalledWith('refreshToken');
            expect(window.localStorage.removeItem).toHaveBeenCalledWith('userInfo');
            expect(mockLocation.replace).toHaveBeenCalledWith('/index.html?error=unauthorized');
        });

        it('should redirect with error when userInfo is invalid JSON', () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = 'invalid-json{';

            executeAuthGuard();

            expect(mockLocation.replace).toHaveBeenCalledWith('/index.html?error=session_expired');
        });

        it('should redirect with error when userInfo missing required fields (role, email)', () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ email: 'test@test.com' }); // missing role

            executeAuthGuard();

            expect(mockLocation.replace).toHaveBeenCalledWith('/index.html?error=session_expired');
        });
    });

    describe('Role-Based Access Control', () => {
        it('should allow admin pages for ROLE_ADMIN', async () => {
            mockLocation.pathname = '/admin/dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_ADMIN', email: 'admin@test.com' });

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ success: true, data: { valid: true } })
            });

            executeAuthGuard();
            await flushPromises();

            expect(mockLocation.replace).not.toHaveBeenCalled();
            expect(mockFetch).toHaveBeenCalledWith(
                'http://localhost:8080/api/auth/validate',
                expect.objectContaining({
                    method: 'GET',
                    headers: expect.objectContaining({
                        'Authorization': 'Bearer test-token'
                    })
                })
            );
        });

        it('should redirect non-admin users from admin pages', () => {
            mockLocation.pathname = '/admin/dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_HOD', email: 'hod@test.com' });

            executeAuthGuard();

            expect(mockLocation.replace).toHaveBeenCalledWith('/index.html?error=access_denied');
        });

        it('should allow deanship pages for ROLE_DEANSHIP', async () => {
            mockLocation.pathname = '/deanship/dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_DEANSHIP', email: 'dean@test.com' });

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ success: true, data: { valid: true } })
            });

            executeAuthGuard();
            await flushPromises();

            expect(mockLocation.replace).not.toHaveBeenCalled();
        });

        it('should allow deanship pages for ROLE_ADMIN', async () => {
            mockLocation.pathname = '/deanship/dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_ADMIN', email: 'admin@test.com' });

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ success: true, data: { valid: true } })
            });

            executeAuthGuard();
            await flushPromises();

            expect(mockLocation.replace).not.toHaveBeenCalled();
        });

        it('should redirect non-deanship users from deanship pages', () => {
            mockLocation.pathname = '/deanship/dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_PROFESSOR', email: 'prof@test.com' });

            executeAuthGuard();

            expect(mockLocation.replace).toHaveBeenCalledWith('/index.html?error=access_denied');
        });

        it('should allow HOD pages for ROLE_HOD', async () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_HOD', email: 'hod@test.com' });

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ success: true, data: { valid: true } })
            });

            executeAuthGuard();
            await flushPromises();

            expect(mockLocation.replace).not.toHaveBeenCalled();
        });

        it('should allow HOD pages for ROLE_ADMIN', async () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_ADMIN', email: 'admin@test.com' });

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ success: true, data: { valid: true } })
            });

            executeAuthGuard();
            await flushPromises();

            expect(mockLocation.replace).not.toHaveBeenCalled();
        });

        it('should redirect non-HOD users from HOD pages', () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_PROFESSOR', email: 'prof@test.com' });

            executeAuthGuard();

            expect(mockLocation.replace).toHaveBeenCalledWith('/index.html?error=access_denied');
        });

        it('should allow professor pages for ROLE_PROFESSOR', async () => {
            mockLocation.pathname = '/prof-dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_PROFESSOR', email: 'prof@test.com' });

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ success: true, data: { valid: true } })
            });

            executeAuthGuard();
            await flushPromises();

            expect(mockLocation.replace).not.toHaveBeenCalled();
        });

        it('should allow professor pages for ROLE_ADMIN', async () => {
            mockLocation.pathname = '/prof-dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_ADMIN', email: 'admin@test.com' });

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ success: true, data: { valid: true } })
            });

            executeAuthGuard();
            await flushPromises();

            expect(mockLocation.replace).not.toHaveBeenCalled();
        });

        it('should redirect non-professor users from professor pages', () => {
            mockLocation.pathname = '/prof-dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_HOD', email: 'hod@test.com' });

            executeAuthGuard();

            expect(mockLocation.replace).toHaveBeenCalledWith('/index.html?error=access_denied');
        });
    });

    describe('Token Validation', () => {
        it('should validate token with server', async () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_HOD', email: 'hod@test.com' });

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ success: true, data: { valid: true } })
            });

            executeAuthGuard();
            await flushPromises();

            expect(mockFetch).toHaveBeenCalledWith(
                'http://localhost:8080/api/auth/validate',
                expect.objectContaining({
                    method: 'GET',
                    headers: expect.objectContaining({
                        'Authorization': 'Bearer test-token'
                    })
                })
            );
        });

        it('should handle successful token validation', async () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_HOD', email: 'hod@test.com' });

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ success: true, data: { valid: true } })
            });

            executeAuthGuard();
            await flushPromises();

            expect(mockLocation.replace).not.toHaveBeenCalled();
            expect(global.clearTimeout).toHaveBeenCalled();
        });

        it('should attempt refresh on invalid token', async () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'invalid-token';
            localStorageStore.refreshToken = 'refresh-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_HOD', email: 'hod@test.com' });

            // First call: validation fails
            mockFetch.mockResolvedValueOnce({
                ok: false,
                status: 401
            });

            // Second call: refresh succeeds
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: { accessToken: 'new-token' }
                })
            });

            executeAuthGuard();
            await flushPromises();

            expect(mockFetch).toHaveBeenCalledTimes(2);
            expect(mockFetch).toHaveBeenNthCalledWith(
                2,
                'http://localhost:8080/api/auth/refresh-token',
                expect.objectContaining({
                    method: 'POST',
                    headers: expect.objectContaining({
                        'Content-Type': 'application/json'
                    }),
                    body: JSON.stringify({ refreshToken: 'refresh-token' })
                })
            );
        });

        it('should handle validation timeout (10s)', async () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_HOD', email: 'hod@test.com' });

            // Mock fetch to never resolve
            mockFetch.mockImplementation(() => new Promise(() => {}));

            executeAuthGuard();
            
            // Verify timeout was set
            expect(global.setTimeout).toHaveBeenCalledWith(expect.any(Function), 10000);

            // Get the timeout ID and execute it
            const timeoutCalls = global.setTimeout.mock.calls;
            const timeoutCall = timeoutCalls.find(call => call[1] === 10000);
            if (timeoutCall) {
                const timeoutId = global.setTimeout.mock.results[timeoutCalls.indexOf(timeoutCall)].value;
                executeTimeout(timeoutId);
            }

            await flushPromises();

            expect(mockLocation.replace).toHaveBeenCalledWith('/index.html?error=session_expired');
        });
    });

    describe('Token Refresh', () => {
        it('should update token on successful refresh', async () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'old-token';
            localStorageStore.refreshToken = 'refresh-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_HOD', email: 'hod@test.com' });

            // Validation fails
            mockFetch.mockResolvedValueOnce({
                ok: false,
                status: 401
            });

            // Refresh succeeds
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: { accessToken: 'new-access-token' }
                })
            });

            executeAuthGuard();
            await flushPromises();

            expect(window.localStorage.setItem).toHaveBeenCalledWith('token', 'new-access-token');
            expect(mockLocation.replace).not.toHaveBeenCalled();
        });

        it('should redirect on refresh failure', async () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'old-token';
            localStorageStore.refreshToken = 'invalid-refresh-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_HOD', email: 'hod@test.com' });

            // Validation fails
            mockFetch.mockResolvedValueOnce({
                ok: false,
                status: 401
            });

            // Refresh fails
            mockFetch.mockResolvedValueOnce({
                ok: false,
                status: 401
            });

            executeAuthGuard();
            await flushPromises();

            expect(mockLocation.replace).toHaveBeenCalledWith('/index.html?error=session_expired');
        });
    });

    describe('clearAndRedirect', () => {
        it('should clear all localStorage auth items', () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'test-token';
            localStorageStore.refreshToken = 'refresh-token';
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_HOD', email: 'hod@test.com' });

            // Remove token to trigger clearAndRedirect
            delete localStorageStore.token;

            executeAuthGuard();

            expect(window.localStorage.removeItem).toHaveBeenCalledWith('token');
            expect(window.localStorage.removeItem).toHaveBeenCalledWith('refreshToken');
            expect(window.localStorage.removeItem).toHaveBeenCalledWith('userInfo');
        });

        it('should use replace for navigation (no back button)', () => {
            mockLocation.pathname = '/hod-dashboard.html';
            // No token, should redirect
            localStorageStore.userInfo = JSON.stringify({ role: 'ROLE_HOD', email: 'hod@test.com' });

            executeAuthGuard();

            expect(mockLocation.replace).toHaveBeenCalledWith('/index.html?error=unauthorized');
            // Verify it's using replace, not assign
            expect(mockLocation.replace).toHaveBeenCalled();
        });
    });

    describe('Integration - Complete Auth Validation Flow', () => {
        it('should complete full auth validation flow successfully', async () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'valid-token';
            localStorageStore.refreshToken = 'refresh-token';
            localStorageStore.userInfo = JSON.stringify({ 
                role: 'ROLE_HOD', 
                email: 'hod@test.com' 
            });

            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ success: true, data: { valid: true } })
            });

            executeAuthGuard();
            await flushPromises();

            // Should validate token
            expect(mockFetch).toHaveBeenCalledWith(
                'http://localhost:8080/api/auth/validate',
                expect.any(Object)
            );

            // Should not redirect
            expect(mockLocation.replace).not.toHaveBeenCalled();

            // Should clear timeout
            expect(global.clearTimeout).toHaveBeenCalled();
        });

        it('should handle complete flow with token refresh', async () => {
            mockLocation.pathname = '/hod-dashboard.html';
            localStorageStore.token = 'expired-token';
            localStorageStore.refreshToken = 'valid-refresh-token';
            localStorageStore.userInfo = JSON.stringify({ 
                role: 'ROLE_HOD', 
                email: 'hod@test.com' 
            });

            // Validation fails
            mockFetch.mockResolvedValueOnce({
                ok: false,
                status: 401
            });

            // Refresh succeeds
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: { accessToken: 'new-valid-token' }
                })
            });

            executeAuthGuard();
            await flushPromises();

            // Should attempt validation first
            expect(mockFetch).toHaveBeenNthCalledWith(
                1,
                'http://localhost:8080/api/auth/validate',
                expect.any(Object)
            );

            // Should then refresh
            expect(mockFetch).toHaveBeenNthCalledWith(
                2,
                'http://localhost:8080/api/auth/refresh-token',
                expect.any(Object)
            );

            // Should update token
            expect(window.localStorage.setItem).toHaveBeenCalledWith('token', 'new-valid-token');

            // Should not redirect
            expect(mockLocation.replace).not.toHaveBeenCalled();
        });
    });
});
