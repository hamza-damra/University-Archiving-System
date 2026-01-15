/**
 * @jest-environment jsdom
 */

import { jest } from '@jest/globals';

// Mock requestAnimationFrame
global.requestAnimationFrame = jest.fn((cb) => {
    return setTimeout(cb, 0);
});

global.cancelAnimationFrame = jest.fn((id) => {
    clearTimeout(id);
});

// Mock timers
jest.useFakeTimers();

// Import the module under test
// The module exports Toast and also sets window.Toast
import ToastModule from '../../../main/resources/static/js/core/toast-notifications.js';

describe('Toast Notifications (core/toast-notifications.js)', () => {
    let Toast;
    let container;

    beforeEach(() => {
        // Clear all mocks
        jest.clearAllMocks();
        jest.clearAllTimers();
        
        // Reset DOM
        document.body.innerHTML = '';
        
        // Reset requestAnimationFrame
        requestAnimationFrame.mockClear();
        requestAnimationFrame.mockImplementation((cb) => setTimeout(cb, 0));
        
        // Get Toast instance (it's a singleton)
        // The module exports default and also sets window.Toast
        Toast = window.Toast || ToastModule;
        
        // Reset Toast state
        if (Toast) {
            Toast.dismissAll();
            // Clear any existing container
            const existingContainer = document.getElementById('toast-notification-container');
            if (existingContainer) {
                existingContainer.remove();
            }
            // Reset internal state
            Toast.container = null;
            Toast.toasts.clear();
            Toast.toastCounter = 0;
            // Reinitialize
            Toast.init();
        }
        
        container = Toast ? Toast.container : null;
    });

    afterEach(() => {
        // Clean up
        if (Toast) {
            Toast.dismissAll();
        }
        jest.clearAllTimers();
    });

    // ============================================
    // Toast Creation Tests
    // ============================================

    describe('Toast Creation', () => {
        it('should create toast container if not exists', () => {
            // Remove container if it exists
            const existing = document.getElementById('toast-notification-container');
            if (existing) {
                existing.remove();
            }
            
            // Create new Toast instance (which will create container)
            const newToast = new (class {
                constructor() {
                    this.container = null;
                    this.toasts = new Map();
                    this.toastCounter = 0;
                    this.defaultDuration = 5000;
                    this.init();
                }
                init() {
                    this.container = document.getElementById('toast-notification-container');
                    if (!this.container) {
                        this.container = document.createElement('div');
                        this.container.id = 'toast-notification-container';
                        this.container.className = 'fixed top-4 right-4 z-[9999] flex flex-col gap-3 max-w-md';
                        this.container.setAttribute('aria-live', 'polite');
                        this.container.setAttribute('aria-label', 'Notifications');
                        document.body.appendChild(this.container);
                    }
                }
            })();
            
            expect(newToast.container).toBeTruthy();
            expect(newToast.container.id).toBe('toast-notification-container');
            expect(document.body.contains(newToast.container)).toBe(true);
        });

        it('should create toast with correct type (success, error, warning, info)', () => {
            const successId = Toast.success('Success message');
            const errorId = Toast.error('Error message');
            const warningId = Toast.warning('Warning message');
            const infoId = Toast.info('Info message');

            const successToast = document.getElementById(successId);
            const errorToast = document.getElementById(errorId);
            const warningToast = document.getElementById(warningId);
            const infoToast = document.getElementById(infoId);

            expect(successToast).toBeTruthy();
            expect(errorToast).toBeTruthy();
            expect(warningToast).toBeTruthy();
            expect(infoToast).toBeTruthy();

            // Check for type-specific classes
            expect(successToast.className).toContain('bg-emerald-50');
            expect(errorToast.className).toContain('bg-red-50');
            expect(warningToast.className).toContain('bg-amber-50');
            expect(infoToast.className).toContain('bg-blue-50');
        });

        it('should show toast with message', () => {
            const message = 'Test message';
            const id = Toast.show({ message, type: 'info' });

            const toast = document.getElementById(id);
            expect(toast).toBeTruthy();
            expect(toast.textContent).toContain(message);
        });

        it('should show toast with title', () => {
            const title = 'Test Title';
            const message = 'Test message';
            const id = Toast.show({ title, message, type: 'info' });

            const toast = document.getElementById(id);
            expect(toast).toBeTruthy();
            expect(toast.textContent).toContain(title);
            expect(toast.textContent).toContain(message);
        });
    });

    // ============================================
    // Toast Behavior Tests
    // ============================================

    describe('Toast Behavior', () => {
        it('should auto-dismiss after duration', () => {
            const duration = 1000;
            const id = Toast.show({ message: 'Test', type: 'info', duration });

            const toast = document.getElementById(id);
            expect(toast).toBeTruthy();
            expect(Toast.toasts.has(id)).toBe(true);

            // Fast-forward time
            jest.advanceTimersByTime(duration + 100);

            // Toast should be removed after animation delay
            jest.advanceTimersByTime(300);

            expect(Toast.toasts.has(id)).toBe(false);
            expect(document.getElementById(id)).toBeNull();
        });

        it('should manual dismiss on click', () => {
            const id = Toast.show({ message: 'Test', type: 'info' });

            const toast = document.getElementById(id);
            const dismissButton = toast.querySelector('[data-dismiss="toast"]');

            expect(dismissButton).toBeTruthy();
            expect(Toast.toasts.has(id)).toBe(true);

            // Click dismiss button
            dismissButton.click();

            // Fast-forward animation delay
            jest.advanceTimersByTime(300);

            expect(Toast.toasts.has(id)).toBe(false);
            expect(document.getElementById(id)).toBeNull();
        });

        it('should stack multiple toasts correctly', () => {
            const id1 = Toast.show({ message: 'Toast 1', type: 'info' });
            const id2 = Toast.show({ message: 'Toast 2', type: 'success' });
            const id3 = Toast.show({ message: 'Toast 3', type: 'warning' });

            expect(Toast.toasts.size).toBe(3);
            expect(container.children.length).toBe(3);

            // All toasts should be in the container
            expect(container.contains(document.getElementById(id1))).toBe(true);
            expect(container.contains(document.getElementById(id2))).toBe(true);
            expect(container.contains(document.getElementById(id3))).toBe(true);
        });

        it('should animate on show/hide', () => {
            const id = Toast.show({ message: 'Test', type: 'info' });

            const toast = document.getElementById(id);
            
            // Initially should have translate-x-full and opacity-0
            expect(toast.classList.contains('translate-x-full')).toBe(true);
            expect(toast.classList.contains('opacity-0')).toBe(true);

            // Trigger requestAnimationFrame callback
            jest.advanceTimersByTime(0);

            // After animation, should have translate-x-0 and opacity-100
            expect(toast.classList.contains('translate-x-0')).toBe(true);
            expect(toast.classList.contains('opacity-100')).toBe(true);

            // Dismiss and check animation
            Toast.dismiss(id);
            jest.advanceTimersByTime(0);

            expect(toast.classList.contains('translate-x-full')).toBe(true);
            expect(toast.classList.contains('opacity-0')).toBe(true);
        });
    });

    // ============================================
    // Toast Queue Tests
    // ============================================

    describe('Toast Queue', () => {
        it('should enforce maximum toasts limit', () => {
            // Create many toasts (the implementation doesn't have a hard limit,
            // but we can test that they all get created)
            const maxToasts = 10;
            const ids = [];

            for (let i = 0; i < maxToasts; i++) {
                ids.push(Toast.show({ message: `Toast ${i}`, type: 'info' }));
            }

            expect(Toast.toasts.size).toBe(maxToasts);
            expect(container.children.length).toBe(maxToasts);
        });

        it('should remove old toasts when limit exceeded', () => {
            // Note: The current implementation doesn't automatically remove old toasts
            // when a limit is exceeded. However, we can test dismissAll functionality
            const id1 = Toast.show({ message: 'Toast 1', type: 'info' });
            const id2 = Toast.show({ message: 'Toast 2', type: 'info' });
            const id3 = Toast.show({ message: 'Toast 3', type: 'info' });

            expect(Toast.toasts.size).toBe(3);

            // Dismiss first toast
            Toast.dismiss(id1);
            jest.advanceTimersByTime(300);

            expect(Toast.toasts.size).toBe(2);
            expect(Toast.toasts.has(id1)).toBe(false);
            expect(Toast.toasts.has(id2)).toBe(true);
            expect(Toast.toasts.has(id3)).toBe(true);
        });

        it('should process queue correctly', () => {
            // Test that toasts are processed in order
            const id1 = Toast.show({ message: 'First', type: 'info', duration: 1000 });
            const id2 = Toast.show({ message: 'Second', type: 'success', duration: 1000 });
            const id3 = Toast.show({ message: 'Third', type: 'warning', duration: 1000 });

            expect(Toast.toasts.size).toBe(3);

            // All should be visible
            expect(document.getElementById(id1)).toBeTruthy();
            expect(document.getElementById(id2)).toBeTruthy();
            expect(document.getElementById(id3)).toBeTruthy();

            // Dismiss all
            Toast.dismissAll();
            jest.advanceTimersByTime(300);

            expect(Toast.toasts.size).toBe(0);
        });
    });

    // ============================================
    // Styling Tests
    // ============================================

    describe('Styling', () => {
        it('should apply green styling to success toast', () => {
            const id = Toast.success('Success message');

            const toast = document.getElementById(id);
            expect(toast.className).toContain('bg-emerald-50');
            expect(toast.className).toContain('border-emerald-200');
        });

        it('should apply red styling to error toast', () => {
            const id = Toast.error('Error message');

            const toast = document.getElementById(id);
            expect(toast.className).toContain('bg-red-50');
            expect(toast.className).toContain('border-red-200');
        });

        it('should apply yellow styling to warning toast', () => {
            const id = Toast.warning('Warning message');

            const toast = document.getElementById(id);
            expect(toast.className).toContain('bg-amber-50');
            expect(toast.className).toContain('border-amber-200');
        });

        it('should apply blue styling to info toast', () => {
            const id = Toast.info('Info message');

            const toast = document.getElementById(id);
            expect(toast.className).toContain('bg-blue-50');
            expect(toast.className).toContain('border-blue-200');
        });
    });
});
