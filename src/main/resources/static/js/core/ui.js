/**
 * UI Helper Functions
 * Modals, toasts, and other UI components
 */

// ============================================================================
// LOADING UTILITIES
// ============================================================================

/**
 * Minimum loading time in milliseconds to prevent flickering shimmer effect
 * When data loads too quickly, the skeleton/shimmer effect appears as a flash
 * This ensures a smooth visual experience
 */
export const MIN_LOADING_TIME = 800;

/**
 * Execute an async operation with a minimum loading time
 * Prevents flickering when data loads too quickly
 * 
 * @param {Function|Promise} asyncOperation - Async function or promise to execute
 * @param {number} minTime - Minimum time in milliseconds (default: MIN_LOADING_TIME)
 * @returns {Promise} The result of the async operation
 * 
 * @example
 * // With async function
 * const data = await withMinLoadingTime(() => fetchData());
 * 
 * @example
 * // With promise
 * const data = await withMinLoadingTime(fetchData());
 * 
 * @example
 * // With custom minimum time
 * const data = await withMinLoadingTime(() => fetchData(), 1000);
 */
export async function withMinLoadingTime(asyncOperation, minTime = MIN_LOADING_TIME) {
    const startTime = Date.now();
    
    // Handle both functions and promises
    const promise = typeof asyncOperation === 'function' 
        ? asyncOperation() 
        : asyncOperation;
    
    const result = await promise;
    
    // Calculate remaining time to meet minimum loading duration
    const elapsedTime = Date.now() - startTime;
    const remainingTime = minTime - elapsedTime;
    
    // Wait for remaining time if data loaded too quickly
    if (remainingTime > 0) {
        await new Promise(resolve => setTimeout(resolve, remainingTime));
    }
    
    return result;
}

// ============================================================================
// BUTTON LOADING STATE UTILITIES
// ============================================================================

/**
 * Set a button to loading state with spinner animation
 * @param {HTMLButtonElement} button - The button element
 * @param {string} loadingText - Optional text to show while loading (default: keeps original text)
 * @returns {Object} Object with restore() method to restore original state
 */
export function setButtonLoading(button, loadingText = null) {
    if (!button) return { restore: () => {} };
    
    // Store original state
    const originalHTML = button.innerHTML;
    const originalDisabled = button.disabled;
    const originalWidth = button.offsetWidth;
    
    // Set minimum width to prevent button from shrinking
    button.style.minWidth = `${originalWidth}px`;
    
    // Create spinner HTML
    const spinnerHTML = `
        <svg class="btn-spinner animate-spin -ml-1 mr-2 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
    `;
    
    // Get text to display
    const displayText = loadingText || button.textContent?.trim() || 'Loading...';
    
    // Set loading state
    button.disabled = true;
    button.classList.add('btn-loading');
    button.innerHTML = `
        <span class="flex items-center justify-center">
            ${spinnerHTML}
            <span>${displayText}</span>
        </span>
    `;
    
    // Return restore function
    return {
        restore: () => {
            button.innerHTML = originalHTML;
            button.disabled = originalDisabled;
            button.style.minWidth = '';
            button.classList.remove('btn-loading');
        }
    };
}

/**
 * Execute an async function with button loading state
 * Automatically shows loading spinner on button and restores after completion
 * @param {HTMLButtonElement} button - The button element
 * @param {Function} asyncFn - Async function to execute
 * @param {string} loadingText - Optional text to show while loading
 * @returns {Promise} Result of the async function
 */
export async function withButtonLoading(button, asyncFn, loadingText = null) {
    const { restore } = setButtonLoading(button, loadingText);
    
    try {
        return await asyncFn();
    } finally {
        restore();
    }
}

/**
 * Create a submit handler with loading state for forms
 * @param {HTMLFormElement} form - The form element
 * @param {HTMLButtonElement} submitButton - The submit button
 * @param {Function} onSubmit - Async function to handle form submission
 * @param {Object} options - Options (loadingText, validateFn)
 */
export function createFormSubmitHandler(form, submitButton, onSubmit, options = {}) {
    const { loadingText = 'Submitting...', validateFn = null } = options;
    
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // Run validation if provided
        if (validateFn && !validateFn()) {
            return;
        }
        
        await withButtonLoading(submitButton, onSubmit, loadingText);
    });
}

// ============================================================================
// TOAST NOTIFICATIONS
// ============================================================================

/**
 * Parse raw JSON error messages to extract user-friendly message
 * @param {string} message - Raw message which might be JSON
 * @returns {string} User-friendly message
 */
function parseErrorMessage(message) {
    if (!message || typeof message !== 'string') {
        return 'An error occurred. Please try again.';
    }
    
    // Try to parse JSON error responses
    if (message.startsWith('{')) {
        try {
            const parsed = JSON.parse(message);
            // Extract user-friendly message from API response structure
            const friendlyMessage = parsed.error?.message || parsed.message;
            if (friendlyMessage && typeof friendlyMessage === 'string') {
                return friendlyMessage;
            }
            return 'An error occurred. Please try again.';
        } catch (e) {
            // Not valid JSON, use as-is but truncate if too long
            return message.length > 200 ? 'An error occurred. Please try again.' : message;
        }
    }
    
    return message;
}

/**
 * Show a toast notification
 * @param {string} message - Toast message (supports multiline with \n)
 * @param {string} type - Toast type (success, error, info, warning)
 * @param {number} duration - Duration in milliseconds (default: 5000)
 * @param {Object} options - Additional options (action, actionLabel, onAction)
 */
export function showToast(message, type = 'info', duration = 5000, options = {}) {
    // Parse error messages to extract user-friendly text
    const cleanMessage = type === 'error' ? parseErrorMessage(message) : message;
    
    // Try to use enhanced toast if available
    if (typeof window.EnhancedToast !== 'undefined') {
        return window.EnhancedToast.show(cleanMessage, type, { duration, ...options });
    }
    
    // Fallback to basic toast
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) return;

    const toast = document.createElement('div');
    toast.className = `slide-in flex items-start p-4 rounded-lg shadow-lg max-w-md ${getToastClasses(type)}`;
    toast.setAttribute('role', 'alert');

    const icon = getToastIcon(type);
    
    // Convert newlines to HTML breaks for multiline messages
    const formattedMessage = cleanMessage.replace(/\n/g, '<br>');
    
    toast.innerHTML = `
        <div class="flex-shrink-0">${icon}</div>
        <div class="flex-1 ml-2" style="white-space: pre-wrap; word-break: break-word;">${formattedMessage}</div>
        <button class="ml-2 flex-shrink-0 text-current opacity-70 hover:opacity-100" onclick="this.parentElement.remove()">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
        </button>
    `;

    toastContainer.appendChild(toast);

    // Auto remove after duration (longer for error messages with multiple lines)
    const adjustedDuration = cleanMessage.includes('\n') ? Math.max(duration, 8000) : duration;
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, adjustedDuration);
}

function getToastClasses(type) {
    const classes = {
        success: 'bg-green-600 text-white',
        error: 'bg-red-600 text-white',
        warning: 'bg-yellow-500 text-white',
        info: 'bg-blue-600 text-white',
    };
    return classes[type] || classes.info;
}

function getToastIcon(type) {
    const icons = {
        success: `<svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
        </svg>`,
        error: `<svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
        </svg>`,
        warning: `<svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
        </svg>`,
        info: `<svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>`,
    };
    return icons[type] || icons.info;
}

/**
 * Create and show a modal
 * @param {string} title - Modal title
 * @param {string} content - Modal content (HTML)
 * @param {object} options - Modal options (buttons, size, etc.)
 * @returns {HTMLElement} Modal element
 */
export function showModal(title, content, options = {}) {
    const {
        buttons = [],
        size = 'md',
        closeButton = true,
        onClose = null,
    } = options;

    const modalsContainer = document.getElementById('modalsContainer');
    if (!modalsContainer) return null;

    const modal = document.createElement('div');
    modal.className = 'fixed inset-0 flex items-center justify-center p-4 fade-in';
    modal.style.zIndex = '9999';
    modal.setAttribute('role', 'dialog');
    modal.setAttribute('aria-modal', 'true');
    modal.setAttribute('aria-labelledby', 'modalTitle');

    const sizeClasses = {
        sm: 'max-w-sm',
        md: 'max-w-md',
        lg: 'max-w-lg',
        xl: 'max-w-xl',
        '2xl': 'max-w-2xl',
        full: 'max-w-full',
    };

    const closeModal = () => {
        modal.style.opacity = '0';
        setTimeout(() => {
            modal.remove();
            if (onClose) onClose();
        }, 200);
    };

    const buttonsHTML = buttons.map(btn => `
        <button 
            class="${btn.className || 'bg-gray-200 text-gray-800 hover:bg-gray-300'} px-4 py-2 rounded-md font-medium transition focus:outline-none focus:ring-2"
            data-action="${btn.action || ''}"
        >
            ${btn.text}
        </button>
    `).join('');

    modal.innerHTML = `
        <div class="modal-backdrop absolute inset-0" onclick="this.parentElement.querySelector('[data-close-modal]').click()"></div>
        <div class="bg-white rounded-lg shadow-xl ${sizeClasses[size] || sizeClasses.md} w-full relative z-10 max-h-[90vh] overflow-y-auto">
            <div class="flex justify-between items-center p-6 border-b border-gray-200">
                <h3 id="modalTitle" class="text-xl font-semibold text-gray-900">${title}</h3>
                ${closeButton ? `
                    <button data-close-modal class="text-gray-400 hover:text-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                        </svg>
                    </button>
                ` : ''}
            </div>
            <div class="p-6">
                ${content}
            </div>
            ${buttons.length > 0 ? `
                <div class="flex justify-end space-x-2 p-6 border-t border-gray-200">
                    ${buttonsHTML}
                </div>
            ` : ''}
        </div>
    `;

    modalsContainer.appendChild(modal);

    // Add event listeners
    const closeBtn = modal.querySelector('[data-close-modal]');
    if (closeBtn) {
        closeBtn.addEventListener('click', closeModal);
    }

    // Handle button clicks
    buttons.forEach(btn => {
        if (btn.action && btn.onClick) {
            const btnElement = modal.querySelector(`[data-action="${btn.action}"]`);
            if (btnElement) {
                btnElement.addEventListener('click', () => btn.onClick(closeModal));
            }
        }
    });

    // Trap focus within modal
    const focusableElements = modal.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    modal.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && closeButton) {
            closeModal();
        }

        if (e.key === 'Tab') {
            if (e.shiftKey && document.activeElement === firstElement) {
                e.preventDefault();
                lastElement.focus();
            } else if (!e.shiftKey && document.activeElement === lastElement) {
                e.preventDefault();
                firstElement.focus();
            }
        }
    });

    // Focus first element
    setTimeout(() => firstElement?.focus(), 100);

    return modal;
}

/**
 * Show a confirmation dialog
 * @param {string} title - Dialog title
 * @param {string} message - Confirmation message
 * @param {function} onConfirm - Callback when confirmed
 * @param {object} options - Additional options
 */
export function showConfirm(title, message, onConfirm, options = {}) {
    const {
        confirmText = 'Confirm',
        cancelText = 'Cancel',
        confirmClass = 'bg-red-600 text-white hover:bg-red-700',
        danger = false,
    } = options;

    return showModal(title, `<p class="text-gray-700">${message}</p>`, {
        buttons: [
            {
                text: cancelText,
                className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                action: 'cancel',
                onClick: (close) => close(),
            },
            {
                text: confirmText,
                className: danger ? 'bg-red-600 text-white hover:bg-red-700' : confirmClass,
                action: 'confirm',
                onClick: (close) => {
                    onConfirm();
                    close();
                },
            },
        ],
    });
}

/**
 * Format date to human-readable string
 * @param {string|Date} date - Date to format
 * @returns {string} Formatted date
 */
export function formatDate(date) {
    const d = new Date(date);
    return d.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
}

/**
 * Get time difference in human-readable format
 * @param {string|Date} date - Target date
 * @returns {string} Relative time string
 */
export function getTimeUntil(date) {
    const now = new Date();
    const target = new Date(date);
    const diff = target - now;
    
    if (diff < 0) {
        return 'Overdue';
    }

    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    
    if (days > 0) {
        return `Due in ${days} day${days !== 1 ? 's' : ''}`;
    } else if (hours > 0) {
        return `Due in ${hours} hour${hours !== 1 ? 's' : ''}`;
    } else {
        return 'Due soon';
    }
}

/**
 * Check if deadline is passed
 * @param {string|Date} deadline - Deadline date
 * @returns {boolean} True if deadline passed
 */
export function isOverdue(deadline) {
    return new Date(deadline) < new Date();
}

/**
 * Validate email format
 * @param {string} email - Email to validate
 * @returns {boolean} True if valid
 */
export function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

/**
 * Validate file extension
 * @param {string} filename - File name
 * @param {string} allowedExtensions - Comma-separated allowed extensions
 * @returns {boolean} True if valid
 */
export function isValidFileExtension(filename, allowedExtensions) {
    if (!allowedExtensions) return true;
    
    const fileExt = filename.split('.').pop().toLowerCase();
    const allowed = allowedExtensions.split(',').map(ext => ext.trim().toLowerCase());
    
    return allowed.includes(fileExt);
}

/**
 * Format file size
 * @param {number} bytes - File size in bytes
 * @returns {string} Formatted file size
 */
export function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

/**
 * Debounce function
 * @param {function} func - Function to debounce
 * @param {number} wait - Wait time in milliseconds
 * @returns {function} Debounced function
 */
export function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

export default {
    showToast,
    showModal,
    showConfirm,
    formatDate,
    getTimeUntil,
    isOverdue,
    isValidEmail,
    isValidFileExtension,
    formatFileSize,
    debounce,
    setButtonLoading,
    withButtonLoading,
    createFormSubmitHandler,
};
