/**
 * @jest-environment jsdom
 */

import { jest } from '@jest/globals';
import {
    withMinLoadingTime,
    MIN_LOADING_TIME,
    setButtonLoading,
    withButtonLoading,
    createFormSubmitHandler,
    showToast,
    showModal,
    showConfirm,
    formatDate,
} from '../../../main/resources/static/js/core/ui.js';

describe('UI Helper Functions', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
        jest.clearAllTimers();
        jest.useFakeTimers();
        
        // Create required containers
        const toastContainer = document.createElement('div');
        toastContainer.id = 'toastContainer';
        document.body.appendChild(toastContainer);
        
        const modalsContainer = document.createElement('div');
        modalsContainer.id = 'modalsContainer';
        document.body.appendChild(modalsContainer);
        
        // Clear EnhancedToast if it exists
        delete window.EnhancedToast;
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.clearAllMocks();
    });

    // ============================================================================
    // LOADING UTILITIES
    // ============================================================================

    describe('withMinLoadingTime', () => {
        it('should wait minimum time when operation completes quickly', async () => {
            const quickOperation = Promise.resolve('result');
            
            const startTime = Date.now();
            const promise = withMinLoadingTime(quickOperation, 100);
            
            // Advance timers to simulate the wait
            jest.advanceTimersByTime(100);
            const result = await promise;
            const elapsedTime = Date.now() - startTime;
            
            expect(result).toBe('result');
            // Should have waited at least 100ms (with some tolerance for test execution)
            expect(elapsedTime).toBeGreaterThanOrEqual(90);
        });

        it('should not wait if operation takes longer than minimum time', async () => {
            const slowOperation = new Promise(resolve => {
                setTimeout(() => resolve('result'), 200);
            });
            
            const promise = withMinLoadingTime(slowOperation, 100);
            
            // Advance timers to complete the slow operation
            jest.advanceTimersByTime(200);
            
            const result = await promise;
            
            expect(result).toBe('result');
            // Should not wait additional time beyond the operation duration
        });

        it('should handle promises', async () => {
            const promise = Promise.resolve('promise result');
            
            const resultPromise = withMinLoadingTime(promise, 100);
            jest.advanceTimersByTime(100);
            
            const result = await resultPromise;
            expect(result).toBe('promise result');
        });

        it('should handle async functions', async () => {
            const asyncFn = async () => {
                await new Promise(resolve => setTimeout(resolve, 50));
                return 'async result';
            };
            
            const resultPromise = withMinLoadingTime(asyncFn, 100);
            
            // Advance timers for both the async function and the minimum wait
            jest.advanceTimersByTime(50); // Complete async function
            jest.advanceTimersByTime(50); // Complete minimum wait
            
            const result = await resultPromise;
            expect(result).toBe('async result');
        });
    });

    // ============================================================================
    // BUTTON LOADING
    // ============================================================================

    describe('setButtonLoading', () => {
        it('should disable button', () => {
            const button = document.createElement('button');
            button.textContent = 'Submit';
            button.disabled = false;
            document.body.appendChild(button);
            
            setButtonLoading(button);
            
            expect(button.disabled).toBe(true);
        });

        it('should show spinner HTML', () => {
            const button = document.createElement('button');
            button.textContent = 'Submit';
            document.body.appendChild(button);
            
            setButtonLoading(button);
            
            expect(button.innerHTML).toContain('btn-spinner');
            expect(button.innerHTML).toContain('animate-spin');
            expect(button.classList.contains('btn-loading')).toBe(true);
        });

        it('should store original state', () => {
            const button = document.createElement('button');
            button.innerHTML = '<span>Original HTML</span>';
            button.disabled = false;
            button.style.width = '100px';
            Object.defineProperty(button, 'offsetWidth', {
                value: 100,
                writable: true
            });
            document.body.appendChild(button);
            
            const { restore } = setButtonLoading(button);
            
            // Verify original state is stored
            restore();
            
            expect(button.innerHTML).toBe('<span>Original HTML</span>');
            expect(button.disabled).toBe(false);
            expect(button.style.minWidth).toBe('');
            expect(button.classList.contains('btn-loading')).toBe(false);
        });

        it('should restore button to original state', () => {
            const button = document.createElement('button');
            button.textContent = 'Click Me';
            button.disabled = false;
            Object.defineProperty(button, 'offsetWidth', {
                value: 120,
                writable: true
            });
            document.body.appendChild(button);
            
            const originalHTML = button.innerHTML;
            const { restore } = setButtonLoading(button);
            
            // Verify loading state
            expect(button.disabled).toBe(true);
            expect(button.innerHTML).toContain('btn-spinner');
            
            // Restore
            restore();
            
            expect(button.innerHTML).toBe(originalHTML);
            expect(button.disabled).toBe(false);
            expect(button.style.minWidth).toBe('');
        });

        it('should handle null button gracefully', () => {
            const { restore } = setButtonLoading(null);
            
            // Should not throw
            expect(() => restore()).not.toThrow();
        });

        it('should use custom loading text', () => {
            const button = document.createElement('button');
            button.textContent = 'Submit';
            document.body.appendChild(button);
            
            setButtonLoading(button, 'Processing...');
            
            expect(button.innerHTML).toContain('Processing...');
        });
    });

    describe('withButtonLoading', () => {
        it('should wrap async function with loading state', async () => {
            const button = document.createElement('button');
            button.textContent = 'Submit';
            document.body.appendChild(button);
            
            const asyncFn = jest.fn(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
                return 'success';
            });
            
            const promise = withButtonLoading(button, asyncFn);
            
            // Check loading state is set
            expect(button.disabled).toBe(true);
            expect(button.innerHTML).toContain('btn-spinner');
            
            // Advance timers to complete the async operation
            jest.advanceTimersByTime(100);
            
            const result = await promise;
            
            expect(result).toBe('success');
            expect(asyncFn).toHaveBeenCalled();
            expect(button.disabled).toBe(false); // Should be restored
            expect(button.innerHTML).not.toContain('btn-spinner');
        });

        it('should restore button even if async function throws', async () => {
            const button = document.createElement('button');
            button.textContent = 'Submit';
            document.body.appendChild(button);
            
            const asyncFn = jest.fn(async () => {
                throw new Error('Test error');
            });
            
            const promise = withButtonLoading(button, asyncFn);
            
            await expect(promise).rejects.toThrow('Test error');
            expect(button.disabled).toBe(false); // Should be restored even on error
        });
    });

    // ============================================================================
    // FORM SUBMIT HANDLER
    // ============================================================================

    describe('createFormSubmitHandler', () => {
        it('should prevent default form submission', async () => {
            const form = document.createElement('form');
            const submitButton = document.createElement('button');
            submitButton.type = 'submit';
            form.appendChild(submitButton);
            document.body.appendChild(form);
            
            const onSubmit = jest.fn(async () => {});
            
            createFormSubmitHandler(form, submitButton, onSubmit);
            
            const event = new Event('submit', { bubbles: true, cancelable: true });
            const preventDefaultSpy = jest.spyOn(event, 'preventDefault');
            
            form.dispatchEvent(event);
            
            // Wait for async operations
            await Promise.resolve();
            
            expect(preventDefaultSpy).toHaveBeenCalled();
        });

        it('should run validation if provided', async () => {
            const form = document.createElement('form');
            const submitButton = document.createElement('button');
            submitButton.type = 'submit';
            form.appendChild(submitButton);
            document.body.appendChild(form);
            
            const onSubmit = jest.fn(async () => {});
            const validateFn = jest.fn(() => false); // Validation fails
            
            createFormSubmitHandler(form, submitButton, onSubmit, { validateFn });
            
            const event = new Event('submit', { bubbles: true, cancelable: true });
            form.dispatchEvent(event);
            
            // Wait for async operations
            await Promise.resolve();
            
            expect(validateFn).toHaveBeenCalled();
            expect(onSubmit).not.toHaveBeenCalled();
        });

        it('should show loading state during submission', async () => {
            const form = document.createElement('form');
            const submitButton = document.createElement('button');
            submitButton.type = 'submit';
            form.appendChild(submitButton);
            document.body.appendChild(form);
            
            const onSubmit = jest.fn(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
            });
            
            createFormSubmitHandler(form, submitButton, onSubmit);
            
            const event = new Event('submit', { bubbles: true, cancelable: true });
            form.dispatchEvent(event);
            
            // Check loading state is set immediately
            expect(submitButton.disabled).toBe(true);
            expect(submitButton.innerHTML).toContain('btn-spinner');
            
            // Advance timers to complete the async operation
            await jest.advanceTimersByTimeAsync(100);
            
            // Wait for all promises to resolve
            await Promise.resolve();
            
            // Should be restored after completion
            expect(submitButton.disabled).toBe(false);
        });

        it('should call onSubmit when validation passes', async () => {
            const form = document.createElement('form');
            const submitButton = document.createElement('button');
            submitButton.type = 'submit';
            form.appendChild(submitButton);
            document.body.appendChild(form);
            
            const onSubmit = jest.fn(async () => 'result');
            const validateFn = jest.fn(() => true); // Validation passes
            
            createFormSubmitHandler(form, submitButton, onSubmit, { validateFn });
            
            const event = new Event('submit', { bubbles: true, cancelable: true });
            form.dispatchEvent(event);
            
            // Wait for async operations
            await Promise.resolve();
            
            expect(validateFn).toHaveBeenCalled();
            expect(onSubmit).toHaveBeenCalled();
        });
    });

    // ============================================================================
    // MODAL FUNCTIONS
    // ============================================================================

    describe('showModal', () => {
        it('should create modal DOM element', () => {
            const modal = showModal('Test Title', 'Test Content');
            
            expect(modal).not.toBeNull();
            expect(modal.tagName).toBe('DIV');
            expect(modal.getAttribute('role')).toBe('dialog');
            expect(modal.getAttribute('aria-modal')).toBe('true');
        });

        it('should set title and content', () => {
            const modal = showModal('My Title', '<p>My Content</p>');
            
            const title = modal.querySelector('#modalTitle');
            const content = modal.querySelectorAll('.p-6')[1]; // 0=header, 1=content, 2=footer (if present)
            
            expect(title).not.toBeNull();
            expect(title.textContent).toBe('My Title');
            expect(content.innerHTML).toContain('<p>My Content</p>');
        });

        it('should handle close action', () => {
            const onClose = jest.fn();
            const modal = showModal('Test', 'Content', { onClose });
            
            const closeBtn = modal.querySelector('[data-close-modal]');
            expect(closeBtn).not.toBeNull();
            
            closeBtn.click();
            jest.advanceTimersByTime(200);
            
            expect(onClose).toHaveBeenCalled();
            expect(document.body.contains(modal)).toBe(false);
        });

        it('should close on backdrop click', () => {
            const modal = showModal('Test', 'Content');
            
            const backdrop = modal.querySelector('.modal-backdrop');
            const closeBtn = modal.querySelector('[data-close-modal]');
            
            backdrop.click();
            jest.advanceTimersByTime(200);
            
            expect(document.body.contains(modal)).toBe(false);
        });

        it('should close on Escape key', () => {
            const modal = showModal('Test', 'Content');
            
            const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
            modal.dispatchEvent(escapeEvent);
            
            jest.advanceTimersByTime(200);
            
            expect(document.body.contains(modal)).toBe(false);
        });

        it('should handle custom buttons', () => {
            const buttonClick = jest.fn();
            const modal = showModal('Test', 'Content', {
                buttons: [
                    {
                        text: 'Custom Button',
                        action: 'custom',
                        onClick: buttonClick
                    }
                ]
            });
            
            const customBtn = modal.querySelector('[data-action="custom"]');
            expect(customBtn).not.toBeNull();
            expect(customBtn.textContent.trim()).toBe('Custom Button');
            
            customBtn.click();
            expect(buttonClick).toHaveBeenCalled();
        });

        it('should return null if modalsContainer does not exist', () => {
            document.getElementById('modalsContainer')?.remove();
            
            const modal = showModal('Test', 'Content');
            
            expect(modal).toBeNull();
        });
    });

    describe('showConfirm', () => {
        it('should show confirm/cancel buttons', () => {
            const modal = showConfirm('Confirm Action', 'Are you sure?', () => {});
            
            const buttons = modal.querySelectorAll('button');
            expect(buttons.length).toBeGreaterThanOrEqual(2);
            
            const buttonTexts = Array.from(buttons).map(btn => btn.textContent.trim());
            expect(buttonTexts).toContain('Confirm');
            expect(buttonTexts).toContain('Cancel');
        });

        it('should call onConfirm when confirm button is clicked', () => {
            const onConfirm = jest.fn();
            const modal = showConfirm('Confirm', 'Are you sure?', onConfirm);
            
            const confirmBtn = modal.querySelector('[data-action="confirm"]');
            expect(confirmBtn).not.toBeNull();
            
            confirmBtn.click();
            jest.advanceTimersByTime(200);
            
            expect(onConfirm).toHaveBeenCalled();
            expect(document.body.contains(modal)).toBe(false);
        });

        it('should close modal when cancel button is clicked', () => {
            const onConfirm = jest.fn();
            const modal = showConfirm('Confirm', 'Are you sure?', onConfirm);
            
            const cancelBtn = modal.querySelector('[data-action="cancel"]');
            expect(cancelBtn).not.toBeNull();
            
            cancelBtn.click();
            jest.advanceTimersByTime(200);
            
            expect(onConfirm).not.toHaveBeenCalled();
            expect(document.body.contains(modal)).toBe(false);
        });

        it('should support custom button text', () => {
            const modal = showConfirm('Confirm', 'Message', () => {}, {
                confirmText: 'Yes, Delete',
                cancelText: 'No, Keep'
            });
            
            const buttons = Array.from(modal.querySelectorAll('button'))
                .map(btn => btn.textContent.trim());
            
            expect(buttons).toContain('Yes, Delete');
            expect(buttons).toContain('No, Keep');
        });
    });

    // ============================================================================
    // TOAST NOTIFICATIONS
    // ============================================================================

    describe('showToast', () => {
        it('should create toast element', () => {
            showToast('Test message', 'info');
            
            const toast = document.querySelector('#toastContainer > div');
            expect(toast).not.toBeNull();
            expect(toast.getAttribute('role')).toBe('alert');
        });

        it('should apply correct type styling', () => {
            const types = ['success', 'error', 'warning', 'info'];
            
            types.forEach(type => {
                document.getElementById('toastContainer').innerHTML = '';
                showToast('Message', type);
                
                const toast = document.querySelector('#toastContainer > div');
                expect(toast.className).toContain(getToastClassForType(type));
            });
        });

        it('should auto-remove after timeout', () => {
            showToast('Test message', 'info', 1000);
            
            let toast = document.querySelector('#toastContainer > div');
            expect(toast).not.toBeNull();
            
            jest.advanceTimersByTime(1000);
            
            // Toast should start fading
            expect(toast.style.opacity).toBe('0');
            
            jest.advanceTimersByTime(300);
            
            toast = document.querySelector('#toastContainer > div');
            expect(toast).toBeNull();
        });

        it('should handle multiline messages with longer duration', () => {
            showToast('Line 1\nLine 2', 'info', 5000);
            
            const toast = document.querySelector('#toastContainer > div');
            expect(toast).not.toBeNull();
            
            // Should use adjusted duration (at least 8000ms for multiline)
            jest.advanceTimersByTime(5000);
            expect(toast.style.opacity).not.toBe('0'); // Still visible
            
            jest.advanceTimersByTime(3000); // Total 8000ms
            expect(toast.style.opacity).toBe('0');
        });

        it('should parse error messages from JSON', () => {
            const jsonError = JSON.stringify({ error: { message: 'User-friendly error' } });
            showToast(jsonError, 'error');
            
            const toast = document.querySelector('#toastContainer > div');
            expect(toast.textContent).toContain('User-friendly error');
        });

        it('should use EnhancedToast if available', () => {
            const enhancedToastShow = jest.fn();
            window.EnhancedToast = {
                show: enhancedToastShow
            };
            
            showToast('Message', 'success', 5000, { action: 'test' });
            
            expect(enhancedToastShow).toHaveBeenCalledWith(
                'Message',
                'success',
                expect.objectContaining({ duration: 5000, action: 'test' })
            );
            
            // Should not create fallback toast
            const toast = document.querySelector('#toastContainer > div');
            expect(toast).toBeNull();
        });

        it('should return early if toastContainer does not exist', () => {
            document.getElementById('toastContainer')?.remove();
            
            expect(() => showToast('Message')).not.toThrow();
        });

        it('should allow manual dismissal', () => {
            showToast('Test message', 'info');
            
            const toast = document.querySelector('#toastContainer > div');
            const closeBtn = toast.querySelector('button');
            
            closeBtn.click();
            
            expect(document.querySelector('#toastContainer > div')).toBeNull();
        });
    });

    // ============================================================================
    // UTILITY FUNCTIONS
    // ============================================================================

    describe('formatDate', () => {
        it('should format dates correctly', () => {
            const date = new Date('2024-01-15T10:30:00');
            const formatted = formatDate(date);
            
            // Should contain date components
            expect(formatted).toContain('2024');
            expect(formatted).toContain('Jan');
            expect(formatted).toContain('15');
        });

        it('should handle date strings', () => {
            const formatted = formatDate('2024-01-15T10:30:00');
            
            expect(formatted).toContain('2024');
            expect(formatted).toContain('Jan');
        });

        it('should include time information', () => {
            const date = new Date('2024-01-15T14:30:00');
            const formatted = formatDate(date);
            
            // Should include time (format may vary by locale, but should be present)
            expect(formatted.length).toBeGreaterThan(10);
        });
    });
});

// Helper function to get expected toast class
function getToastClassForType(type) {
    const classes = {
        success: 'bg-green-600',
        error: 'bg-red-600',
        warning: 'bg-yellow-500',
        info: 'bg-blue-600',
    };
    return classes[type] || classes.info;
}
