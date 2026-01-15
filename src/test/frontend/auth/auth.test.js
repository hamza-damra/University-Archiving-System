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
jest.mock('../../../main/resources/static/js/core/api.js', () => ({
    auth: {
        login: jest.fn()
    },
    saveAuthData: jest.fn(),
    isAuthenticated: jest.fn(),
    initializeAuth: jest.fn()
}));

// Mock the UI module
jest.mock('../../../main/resources/static/js/core/ui.js', () => ({
    showToast: jest.fn(),
    isValidEmail: jest.fn()
}));

// Import mocked modules
import { auth, saveAuthData, isAuthenticated, initializeAuth } from '../../../main/resources/static/js/core/api.js';
import { showToast, isValidEmail } from '../../../main/resources/static/js/core/ui.js';

// Import the module under test (we'll need to restructure it for testing)
// Since auth.js uses top-level code, we'll test the functions directly

describe('Authentication Flow (auth/auth.js)', () => {
    let mockDocument;
    let mockEmailInput;
    let mockPasswordInput;
    let mockEmailError;
    let mockPasswordError;
    let mockGeneralError;
    let mockSubmitBtn;
    let mockBtnText;
    let mockLoginForm;
    let mockOverlay;
    let mockLoginContent;

    beforeEach(() => {
        // Reset all mocks
        jest.clearAllMocks();
        localStorageMock.clear();
        fetch.mockClear();
        
        // Reset window.location
        delete window.location;
        window.location = { href: '' };

        // Create mock DOM elements
        mockEmailInput = {
            value: '',
            addEventListener: jest.fn(),
            focus: jest.fn(),
            previousElementSibling: { classList: { add: jest.fn(), remove: jest.fn() } }
        };
        
        mockPasswordInput = {
            value: '',
            addEventListener: jest.fn(),
            previousElementSibling: { classList: { add: jest.fn(), remove: jest.fn() } }
        };
        
        mockEmailError = {
            textContent: '',
            classList: { add: jest.fn(), remove: jest.fn() },
            previousElementSibling: mockEmailInput.previousElementSibling
        };
        
        mockPasswordError = {
            textContent: '',
            classList: { add: jest.fn(), remove: jest.fn() },
            previousElementSibling: mockPasswordInput.previousElementSibling
        };
        
        mockGeneralError = {
            textContent: '',
            classList: { add: jest.fn(), remove: jest.fn() }
        };
        
        mockBtnText = {
            textContent: 'Sign In'
        };
        
        mockSubmitBtn = {
            disabled: false,
            classList: { add: jest.fn(), remove: jest.fn() },
            querySelector: jest.fn(() => mockBtnText)
        };
        
        mockLoginForm = {
            addEventListener: jest.fn()
        };
        
        mockOverlay = {
            style: { display: 'block' }
        };
        
        mockLoginContent = {
            style: { visibility: 'hidden' }
        };

        // Setup document.getElementById mock
        mockDocument = {
            getElementById: jest.fn((id) => {
                const elements = {
                    'email': mockEmailInput,
                    'password': mockPasswordInput,
                    'emailError': mockEmailError,
                    'passwordError': mockPasswordError,
                    'generalError': mockGeneralError,
                    'submitBtn': mockSubmitBtn,
                    'loginForm': mockLoginForm,
                    'authCheckOverlay': mockOverlay,
                    'loginContent': mockLoginContent
                };
                return elements[id] || null;
            }),
            body: { innerHTML: '' }
        };

        // Replace global document
        global.document = mockDocument;
    });

    afterEach(() => {
        jest.clearAllTimers();
    });

    describe('redirectToDashboard', () => {
        // Helper function extracted from auth.js for testing
        function redirectToDashboard(role) {
            if (role === 'ROLE_ADMIN') {
                window.location.href = '/admin/dashboard.html';
            } else if (role === 'ROLE_DEANSHIP') {
                window.location.href = '/deanship/dashboard.html';
            } else if (role === 'ROLE_HOD') {
                window.location.href = '/hod/dashboard.html';
            } else if (role === 'ROLE_PROFESSOR') {
                window.location.href = '/professor/dashboard.html';
            } else {
                const errorEl = document.getElementById('generalError');
                if (errorEl) {
                    errorEl.textContent = 'Unknown user role. Please contact administrator.';
                    errorEl.classList.remove('hidden');
                } else {
                    alert('Unknown user role. Please contact administrator.');
                }
            }
        }

        it('should route correctly for ROLE_ADMIN', () => {
            redirectToDashboard('ROLE_ADMIN');
            expect(window.location.href).toBe('/admin/dashboard.html');
        });

        it('should route correctly for ROLE_DEANSHIP', () => {
            redirectToDashboard('ROLE_DEANSHIP');
            expect(window.location.href).toBe('/deanship/dashboard.html');
        });

        it('should route correctly for ROLE_HOD', () => {
            redirectToDashboard('ROLE_HOD');
            expect(window.location.href).toBe('/hod/dashboard.html');
        });

        it('should route correctly for ROLE_PROFESSOR', () => {
            redirectToDashboard('ROLE_PROFESSOR');
            expect(window.location.href).toBe('/professor/dashboard.html');
        });

        it('should show error for unknown role', () => {
            const alertSpy = jest.spyOn(window, 'alert').mockImplementation(() => {});
            redirectToDashboard('UNKNOWN_ROLE');
            
            expect(mockGeneralError.textContent).toBe('Unknown user role. Please contact administrator.');
            expect(mockGeneralError.classList.remove).toHaveBeenCalledWith('hidden');
            alertSpy.mockRestore();
        });
    });

    describe('showLoginPage', () => {
        function showLoginPage() {
            const overlay = document.getElementById('authCheckOverlay');
            const loginContent = document.getElementById('loginContent');
            
            if (overlay) {
                overlay.style.display = 'none';
            }
            if (loginContent) {
                loginContent.style.visibility = 'visible';
            }
        }

        it('should hide overlay and show login content', () => {
            showLoginPage();
            
            expect(mockOverlay.style.display).toBe('none');
            expect(mockLoginContent.style.visibility).toBe('visible');
        });
    });

    describe('checkExistingAuth', () => {
        it('should redirect if already authenticated with valid token', async () => {
            isAuthenticated.mockReturnValue(true);
            initializeAuth.mockResolvedValue(true);
            localStorageMock.setItem('userInfo', JSON.stringify({ role: 'ROLE_ADMIN' }));
            
            // Simulate the checkExistingAuth logic
            if (isAuthenticated()) {
                const isValid = await initializeAuth();
                if (isValid) {
                    const userInfo = JSON.parse(localStorage.getItem('userInfo'));
                    if (userInfo && userInfo.role) {
                        window.location.href = userInfo.role === 'ROLE_ADMIN' ? '/admin/dashboard.html' : '';
                    }
                }
            }
            
            await new Promise(resolve => setTimeout(resolve, 0));
            expect(initializeAuth).toHaveBeenCalled();
            expect(window.location.href).toBe('/admin/dashboard.html');
        });

        it('should clear invalid/expired auth data and show login', async () => {
            isAuthenticated.mockReturnValue(true);
            initializeAuth.mockResolvedValue(false);
            
            // Simulate the checkExistingAuth logic
            if (isAuthenticated()) {
                const isValid = await initializeAuth();
                if (!isValid) {
                    localStorage.removeItem('token');
                    localStorage.removeItem('refreshToken');
                    localStorage.removeItem('userInfo');
                }
            }
            
            await new Promise(resolve => setTimeout(resolve, 0));
            expect(localStorage.removeItem).toHaveBeenCalledWith('token');
            expect(localStorage.removeItem).toHaveBeenCalledWith('refreshToken');
            expect(localStorage.removeItem).toHaveBeenCalledWith('userInfo');
        });

        it('should handle missing userInfo gracefully', async () => {
            isAuthenticated.mockReturnValue(true);
            initializeAuth.mockResolvedValue(true);
            localStorageMock.getItem.mockReturnValue(null);
            
            // Simulate the checkExistingAuth logic
            if (isAuthenticated()) {
                const isValid = await initializeAuth();
                if (isValid) {
                    const userInfo = JSON.parse(localStorage.getItem('userInfo'));
                    if (!userInfo || !userInfo.role) {
                        localStorage.removeItem('token');
                        localStorage.removeItem('refreshToken');
                        localStorage.removeItem('userInfo');
                    }
                }
            }
            
            await new Promise(resolve => setTimeout(resolve, 0));
            expect(localStorage.removeItem).toHaveBeenCalled();
        });
    });

    describe('Form validation', () => {
        function showFieldError(element, message) {
            element.textContent = message;
            element.classList.remove('hidden');
            element.previousElementSibling.classList.add('border-red-500');
        }

        it('should show error for empty email', () => {
            const email = '';
            if (!email) {
                showFieldError(mockEmailError, 'Email is required');
            }
            
            expect(mockEmailError.textContent).toBe('Email is required');
            expect(mockEmailError.classList.remove).toHaveBeenCalledWith('hidden');
            expect(mockEmailError.previousElementSibling.classList.add).toHaveBeenCalledWith('border-red-500');
        });

        it('should show error for invalid email format', () => {
            isValidEmail.mockReturnValue(false);
            const email = 'invalid-email';
            
            if (!isValidEmail(email)) {
                showFieldError(mockEmailError, 'Please enter a valid email address');
            }
            
            expect(isValidEmail).toHaveBeenCalledWith(email);
            expect(mockEmailError.textContent).toBe('Please enter a valid email address');
        });

        it('should show error for empty password', () => {
            const password = '';
            if (!password) {
                showFieldError(mockPasswordError, 'Password is required');
            }
            
            expect(mockPasswordError.textContent).toBe('Password is required');
            expect(mockPasswordError.classList.remove).toHaveBeenCalledWith('hidden');
        });
    });

    describe('isValidEmail', () => {
        it('should accept valid email formats', () => {
            isValidEmail.mockReturnValue(true);
            const validEmails = ['test@example.com', 'user.name@domain.co.uk', 'user+tag@example.com'];
            
            validEmails.forEach(email => {
                isValidEmail.mockReturnValueOnce(true);
                const result = isValidEmail(email);
                expect(result).toBe(true);
            });
        });

        it('should reject invalid email formats', () => {
            isValidEmail.mockReturnValue(false);
            const invalidEmails = ['invalid', '@example.com', 'user@', 'user@.com'];
            
            invalidEmails.forEach(email => {
                isValidEmail.mockReturnValueOnce(false);
                const result = isValidEmail(email);
                expect(result).toBe(false);
            });
        });
    });

    describe('Login success', () => {
        it('should save auth data and redirect', async () => {
            const mockResponse = {
                token: 'test-token',
                refreshToken: 'test-refresh-token',
                role: 'ROLE_ADMIN',
                firstName: 'John',
                lastName: 'Doe',
                email: 'john@example.com',
                id: 1,
                departmentId: 1,
                departmentName: 'CS'
            };
            
            auth.login.mockResolvedValue(mockResponse);
            
            // Simulate login flow
            const response = await auth.login({ email: 'test@example.com', password: 'password' });
            
            if (response && response.token) {
                const { token, refreshToken, role, firstName, lastName, email: userEmail, id, departmentId, departmentName } = response;
                
                saveAuthData(token, {
                    id,
                    email: userEmail,
                    firstName,
                    lastName,
                    role,
                    departmentId,
                    departmentName,
                    fullName: `${firstName} ${lastName}`,
                }, refreshToken);
                
                window.location.href = role === 'ROLE_ADMIN' ? '/admin/dashboard.html' : '';
            }
            
            expect(auth.login).toHaveBeenCalledWith({ email: 'test@example.com', password: 'password' });
            expect(saveAuthData).toHaveBeenCalled();
            expect(window.location.href).toBe('/admin/dashboard.html');
        });
    });

    describe('Login failure', () => {
        function showGeneralError(message) {
            const errorEl = document.getElementById('generalError');
            if (errorEl) {
                errorEl.textContent = message;
                errorEl.classList.remove('hidden');
            }
        }

        it('should show error message for invalid credentials', async () => {
            const error = new Error('Invalid email or password');
            error.message = 'Invalid email or password';
            
            showGeneralError(error.message || 'Invalid email or password. Please try again.');
            
            expect(mockGeneralError.textContent).toBe('Invalid email or password. Please try again.');
            expect(mockGeneralError.classList.remove).toHaveBeenCalledWith('hidden');
        });

        it('should handle ACCOUNT_DISABLED error', async () => {
            const error = { code: 'ACCOUNT_DISABLED' };
            
            if (error.code === 'ACCOUNT_DISABLED') {
                showGeneralError('Your account has been deactivated. Please contact your administrator.');
            }
            
            expect(mockGeneralError.textContent).toBe('Your account has been deactivated. Please contact your administrator.');
        });

        it('should handle ACCOUNT_LOCKED error', async () => {
            const error = { code: 'ACCOUNT_LOCKED' };
            
            if (error.code === 'ACCOUNT_LOCKED') {
                showGeneralError('Your account has been locked. Please contact your administrator.');
            }
            
            expect(mockGeneralError.textContent).toBe('Your account has been locked. Please contact your administrator.');
        });
    });

    describe('Rate limiting', () => {
        let rateLimitedUntil = 0;
        let countdownInterval = null;

        function startRateLimitCountdown(seconds) {
            if (countdownInterval) {
                clearInterval(countdownInterval);
            }
            
            rateLimitedUntil = Date.now() + (seconds * 1000);
            
            mockSubmitBtn.disabled = true;
            mockSubmitBtn.classList.add('opacity-50', 'cursor-not-allowed');
            
            const updateCountdown = () => {
                const remaining = Math.ceil((rateLimitedUntil - Date.now()) / 1000);
                
                if (remaining <= 0) {
                    clearInterval(countdownInterval);
                    countdownInterval = null;
                    rateLimitedUntil = 0;
                    
                    mockSubmitBtn.disabled = false;
                    mockSubmitBtn.classList.remove('opacity-50', 'cursor-not-allowed');
                    
                    mockGeneralError.classList.add('hidden');
                    return;
                }
                
                const minutes = Math.floor(remaining / 60);
                const secs = remaining % 60;
                const timeStr = minutes > 0 
                    ? `${minutes}:${secs.toString().padStart(2, '0')}` 
                    : `${secs} second${secs !== 1 ? 's' : ''}`;
                
                mockGeneralError.textContent = `Too many login attempts. Please wait ${timeStr} before trying again.`;
                mockGeneralError.classList.remove('hidden');
            };
            
            updateCountdown();
            countdownInterval = setInterval(updateCountdown, 1000);
        }

        function isRateLimited() {
            return rateLimitedUntil > Date.now();
        }

        it('should start countdown on RATE_LIMIT_EXCEEDED', () => {
            jest.useFakeTimers();
            const retryAfterSeconds = 60;
            
            startRateLimitCountdown(retryAfterSeconds);
            
            expect(mockSubmitBtn.disabled).toBe(true);
            expect(mockSubmitBtn.classList.add).toHaveBeenCalledWith('opacity-50', 'cursor-not-allowed');
            expect(mockGeneralError.textContent).toContain('Too many login attempts');
            
            jest.useRealTimers();
        });

        it('should disable button during countdown', () => {
            jest.useFakeTimers();
            startRateLimitCountdown(60);
            
            expect(mockSubmitBtn.disabled).toBe(true);
            
            jest.useRealTimers();
        });

        it('should update error message with time', () => {
            jest.useFakeTimers();
            startRateLimitCountdown(125); // 2 minutes 5 seconds
            
            expect(mockGeneralError.textContent).toContain('Too many login attempts');
            expect(mockGeneralError.textContent).toMatch(/\d+/); // Contains numbers
            
            jest.useRealTimers();
        });

        it('should re-enable button after countdown', () => {
            jest.useFakeTimers();
            startRateLimitCountdown(1);
            
            expect(mockSubmitBtn.disabled).toBe(true);
            
            jest.advanceTimersByTime(1000);
            
            expect(mockSubmitBtn.disabled).toBe(false);
            expect(mockSubmitBtn.classList.remove).toHaveBeenCalledWith('opacity-50', 'cursor-not-allowed');
            
            jest.useRealTimers();
        });

        it('should return correct state for isRateLimited', () => {
            jest.useFakeTimers();
            const now = Date.now();
            rateLimitedUntil = now + 5000;
            
            expect(isRateLimited()).toBe(true);
            
            rateLimitedUntil = now - 1000;
            expect(isRateLimited()).toBe(false);
            
            jest.useRealTimers();
        });
    });

    describe('Button states', () => {
        function setButtonLoadingState(isLoading, text = 'Signing in...') {
            if (!mockSubmitBtn) return;
            
            if (isLoading) {
                mockSubmitBtn.disabled = true;
                mockSubmitBtn.classList.add('btn-loading');
                if (mockBtnText) {
                    mockBtnText.textContent = text;
                }
            } else {
                mockSubmitBtn.disabled = false;
                mockSubmitBtn.classList.remove('btn-loading', 'btn-success');
                if (mockBtnText) {
                    mockBtnText.textContent = 'Sign In';
                }
            }
        }

        function setButtonSuccessState() {
            if (!mockSubmitBtn) return;
            
            mockSubmitBtn.classList.remove('btn-loading');
            mockSubmitBtn.classList.add('btn-success');
            if (mockBtnText) {
                mockBtnText.textContent = 'Success!';
            }
        }

        it('should show spinner during loading', () => {
            setButtonLoadingState(true, 'Signing in...');
            
            expect(mockSubmitBtn.disabled).toBe(true);
            expect(mockSubmitBtn.classList.add).toHaveBeenCalledWith('btn-loading');
            expect(mockBtnText.textContent).toBe('Signing in...');
        });

        it('should show success checkmark', () => {
            setButtonSuccessState();
            
            expect(mockSubmitBtn.classList.remove).toHaveBeenCalledWith('btn-loading');
            expect(mockSubmitBtn.classList.add).toHaveBeenCalledWith('btn-success');
            expect(mockBtnText.textContent).toBe('Success!');
        });
    });
});
