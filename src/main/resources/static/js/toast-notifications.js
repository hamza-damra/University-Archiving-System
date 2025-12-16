/**
 * Enhanced Toast Notification System
 * Professional toast messages for the entire application
 * 
 * Provides contextual error messages for common scenarios:
 * - File not found (404)
 * - Server errors (500)
 * - Permission errors (403)
 * - Network errors
 * - Validation errors
 */

class ToastNotificationSystem {
    constructor() {
        this.container = null;
        this.toasts = new Map();
        this.toastCounter = 0;
        this.defaultDuration = 5000;
        this.init();
    }

    init() {
        // Create container if it doesn't exist
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

    /**
     * Get icon SVG based on type
     */
    getIcon(type) {
        const icons = {
            success: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>`,
            error: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>`,
            warning: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
            </svg>`,
            info: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>`,
            loading: `<svg class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"/>
            </svg>`
        };
        return icons[type] || icons.info;
    }

    /**
     * Get color classes based on type
     */
    getColorClasses(type) {
        const classes = {
            success: {
                bg: 'bg-emerald-50 dark:bg-emerald-900/30',
                border: 'border-emerald-200 dark:border-emerald-800',
                icon: 'text-emerald-500 dark:text-emerald-400',
                title: 'text-emerald-800 dark:text-emerald-200',
                message: 'text-emerald-700 dark:text-emerald-300',
                progress: 'bg-emerald-500'
            },
            error: {
                bg: 'bg-red-50 dark:bg-red-900/30',
                border: 'border-red-200 dark:border-red-800',
                icon: 'text-red-500 dark:text-red-400',
                title: 'text-red-800 dark:text-red-200',
                message: 'text-red-700 dark:text-red-300',
                progress: 'bg-red-500'
            },
            warning: {
                bg: 'bg-amber-50 dark:bg-amber-900/30',
                border: 'border-amber-200 dark:border-amber-800',
                icon: 'text-amber-500 dark:text-amber-400',
                title: 'text-amber-800 dark:text-amber-200',
                message: 'text-amber-700 dark:text-amber-300',
                progress: 'bg-amber-500'
            },
            info: {
                bg: 'bg-blue-50 dark:bg-blue-900/30',
                border: 'border-blue-200 dark:border-blue-800',
                icon: 'text-blue-500 dark:text-blue-400',
                title: 'text-blue-800 dark:text-blue-200',
                message: 'text-blue-700 dark:text-blue-300',
                progress: 'bg-blue-500'
            },
            loading: {
                bg: 'bg-gray-50 dark:bg-gray-800',
                border: 'border-gray-200 dark:border-gray-700',
                icon: 'text-gray-500 dark:text-gray-400',
                title: 'text-gray-800 dark:text-gray-200',
                message: 'text-gray-600 dark:text-gray-400',
                progress: 'bg-gray-500'
            }
        };
        return classes[type] || classes.info;
    }

    /**
     * Show a toast notification
     * @param {Object} options - Toast options
     * @returns {string} Toast ID for programmatic control
     */
    show(options) {
        const {
            type = 'info',
            title = '',
            message = '',
            duration = this.defaultDuration,
            dismissible = true,
            action = null,
            actionLabel = '',
            onAction = null,
            persistent = false
        } = typeof options === 'string' ? { message: options } : options;

        const id = `toast-${++this.toastCounter}`;
        const colors = this.getColorClasses(type);

        const toast = document.createElement('div');
        toast.id = id;
        toast.className = `
            ${colors.bg} ${colors.border} border rounded-lg shadow-lg 
            transform transition-all duration-300 ease-out
            translate-x-full opacity-0
            overflow-hidden
        `.trim().replace(/\s+/g, ' ');
        toast.setAttribute('role', 'alert');

        const titleHtml = title ? `<div class="${colors.title} font-semibold text-sm">${this.escapeHtml(title)}</div>` : '';
        const messageHtml = message ? `<div class="${colors.message} text-sm mt-0.5">${this.escapeHtml(message)}</div>` : '';
        
        let actionHtml = '';
        if (action || (actionLabel && onAction)) {
            actionHtml = `
                <button class="mt-2 text-sm font-medium ${colors.title} hover:underline focus:outline-none" data-action="toast-action">
                    ${this.escapeHtml(actionLabel || action)}
                </button>
            `;
        }

        const dismissBtn = dismissible ? `
            <button class="flex-shrink-0 ml-3 ${colors.icon} hover:opacity-70 focus:outline-none" data-dismiss="toast" aria-label="Close">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
            </button>
        ` : '';

        toast.innerHTML = `
            <div class="flex items-start p-4">
                <div class="flex-shrink-0 ${colors.icon}">
                    ${this.getIcon(type)}
                </div>
                <div class="ml-3 flex-1 min-w-0">
                    ${titleHtml}
                    ${messageHtml}
                    ${actionHtml}
                </div>
                ${dismissBtn}
            </div>
            ${!persistent && duration > 0 ? `
                <div class="h-1 ${colors.progress} transition-all duration-100" data-progress style="width: 100%"></div>
            ` : ''}
        `;

        // Add to container
        this.container.appendChild(toast);

        // Store reference
        this.toasts.set(id, {
            element: toast,
            timeoutId: null,
            intervalId: null
        });

        // Animate in
        requestAnimationFrame(() => {
            toast.classList.remove('translate-x-full', 'opacity-0');
            toast.classList.add('translate-x-0', 'opacity-100');
        });

        // Setup dismiss button
        const dismissButton = toast.querySelector('[data-dismiss="toast"]');
        if (dismissButton) {
            dismissButton.addEventListener('click', () => this.dismiss(id));
        }

        // Setup action button
        const actionButton = toast.querySelector('[data-action="toast-action"]');
        if (actionButton && onAction) {
            actionButton.addEventListener('click', () => {
                onAction();
                this.dismiss(id);
            });
        }

        // Auto dismiss with progress bar
        if (!persistent && duration > 0) {
            const progressBar = toast.querySelector('[data-progress]');
            const startTime = Date.now();
            
            const updateProgress = () => {
                const elapsed = Date.now() - startTime;
                const remaining = Math.max(0, 100 - (elapsed / duration) * 100);
                if (progressBar) {
                    progressBar.style.width = `${remaining}%`;
                }
            };

            const intervalId = setInterval(updateProgress, 50);
            const timeoutId = setTimeout(() => {
                clearInterval(intervalId);
                this.dismiss(id);
            }, duration);

            this.toasts.get(id).timeoutId = timeoutId;
            this.toasts.get(id).intervalId = intervalId;
        }

        return id;
    }

    /**
     * Dismiss a toast
     */
    dismiss(id) {
        const toast = this.toasts.get(id);
        if (!toast) return;

        const { element, timeoutId, intervalId } = toast;

        // Clear timers
        if (timeoutId) clearTimeout(timeoutId);
        if (intervalId) clearInterval(intervalId);

        // Animate out
        element.classList.remove('translate-x-0', 'opacity-100');
        element.classList.add('translate-x-full', 'opacity-0');

        // Remove after animation
        setTimeout(() => {
            element.remove();
            this.toasts.delete(id);
        }, 300);
    }

    /**
     * Dismiss all toasts
     */
    dismissAll() {
        this.toasts.forEach((_, id) => this.dismiss(id));
    }

    /**
     * Update a toast (useful for loading -> success/error)
     */
    update(id, options) {
        const toast = this.toasts.get(id);
        if (!toast) return;

        this.dismiss(id);
        return this.show(options);
    }

    /**
     * Escape HTML
     */
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Convenience methods
    success(message, title = 'Success', options = {}) {
        return this.show({ type: 'success', title, message, ...options });
    }

    error(message, title = 'Error', options = {}) {
        return this.show({ type: 'error', title, message, duration: 7000, ...options });
    }

    warning(message, title = 'Warning', options = {}) {
        return this.show({ type: 'warning', title, message, ...options });
    }

    info(message, title = 'Info', options = {}) {
        return this.show({ type: 'info', title, message, ...options });
    }

    loading(message, title = 'Loading', options = {}) {
        return this.show({ type: 'loading', title, message, persistent: true, dismissible: false, ...options });
    }
}

// Create singleton instance
const Toast = new ToastNotificationSystem();

/**
 * Error Message Handler
 * Provides user-friendly messages for common error scenarios
 */
const ErrorMessages = {
    // HTTP Status Code Messages
    getHttpErrorMessage(status, context = '') {
        const messages = {
            400: {
                title: 'Invalid Request',
                message: 'The request could not be processed. Please check your input and try again.'
            },
            401: {
                title: 'Session Expired',
                message: 'Your session has expired. Please log in again to continue.'
            },
            403: {
                title: 'Access Denied',
                message: 'You don\'t have permission to access this resource.'
            },
            404: {
                title: 'Not Found',
                message: context ? `The requested ${context} could not be found. It may have been moved or deleted.` 
                                : 'The requested resource could not be found. It may have been moved or deleted.'
            },
            409: {
                title: 'Conflict',
                message: 'This operation conflicts with the current state. Please refresh and try again.'
            },
            413: {
                title: 'File Too Large',
                message: 'The file you\'re trying to upload exceeds the maximum allowed size.'
            },
            422: {
                title: 'Validation Error',
                message: 'The provided data is invalid. Please check your input and try again.'
            },
            429: {
                title: 'Too Many Requests',
                message: 'You\'ve made too many requests. Please wait a moment and try again.'
            },
            500: {
                title: 'Server Error',
                message: 'Something went wrong on our end. Our team has been notified. Please try again later.'
            },
            502: {
                title: 'Service Unavailable',
                message: 'The service is temporarily unavailable. Please try again in a few minutes.'
            },
            503: {
                title: 'Service Unavailable',
                message: 'The service is temporarily unavailable for maintenance. Please try again later.'
            },
            504: {
                title: 'Request Timeout',
                message: 'The request took too long to complete. Please try again.'
            }
        };

        return messages[status] || {
            title: 'Error',
            message: `An unexpected error occurred (${status}). Please try again.`
        };
    },

    // File-specific errors
    file: {
        notFound: {
            title: 'File Not Found',
            message: 'This file could not be found. It may have been moved or deleted from the server.'
        },
        previewFailed: {
            title: 'Preview Unavailable',
            message: 'Unable to preview this file. The file may be corrupted or in an unsupported format.'
        },
        downloadFailed: {
            title: 'Download Failed',
            message: 'Unable to download this file. Please check your connection and try again.'
        },
        uploadFailed: {
            title: 'Upload Failed',
            message: 'Failed to upload the file. Please check your connection and try again.'
        },
        unsupportedFormat: {
            title: 'Unsupported Format',
            message: 'This file format is not supported for preview. You can download it instead.'
        },
        corrupted: {
            title: 'File Error',
            message: 'The file appears to be corrupted or in an unsupported format.'
        }
    },

    // Folder-specific errors
    folder: {
        notFound: {
            title: 'Folder Not Found',
            message: 'This folder could not be found. It may have been moved or deleted.'
        },
        accessDenied: {
            title: 'Access Denied',
            message: 'You don\'t have permission to access this folder.'
        },
        createFailed: {
            title: 'Folder Creation Failed',
            message: 'Unable to create the folder. Please try again.'
        }
    },

    // Network errors
    network: {
        offline: {
            title: 'No Connection',
            message: 'You appear to be offline. Please check your internet connection.'
        },
        timeout: {
            title: 'Connection Timeout',
            message: 'The request timed out. Please check your connection and try again.'
        },
        failed: {
            title: 'Connection Failed',
            message: 'Unable to connect to the server. Please try again later.'
        }
    },

    // Generic errors
    generic: {
        unknown: {
            title: 'Something Went Wrong',
            message: 'An unexpected error occurred. Please try again.'
        },
        tryAgain: {
            title: 'Operation Failed',
            message: 'The operation could not be completed. Please try again.'
        }
    }
};

/**
 * Global error handler for fetch/API requests
 */
function handleApiError(error, context = '') {
    console.error('API Error:', error);

    // Network error
    if (error.name === 'TypeError' && (error.message === 'Failed to fetch' || error.message.includes('fetch'))) {
        if (!navigator.onLine) {
            Toast.error(ErrorMessages.network.offline.message, ErrorMessages.network.offline.title);
        } else {
            Toast.error(ErrorMessages.network.failed.message, ErrorMessages.network.failed.title);
        }
        return;
    }

    // Timeout error
    if (error.name === 'AbortError') {
        Toast.error(ErrorMessages.network.timeout.message, ErrorMessages.network.timeout.title);
        return;
    }

    // Parse the error to get clean message
    const parsed = parseApiError(error);

    // HTTP error with status
    if (parsed.status || error.status) {
        const status = parsed.status || error.status;
        const { title, message } = ErrorMessages.getHttpErrorMessage(status, context);
        Toast.error(message, title);
        return;
    }

    // Generic error with parsed message
    if (parsed.message && parsed.message !== 'An error occurred') {
        const friendlyMessage = getFriendlyErrorMessage(parsed.message, context);
        Toast.error(friendlyMessage, 'Error');
        return;
    }

    // Fallback
    Toast.error(ErrorMessages.generic.unknown.message, ErrorMessages.generic.unknown.title);
}

/**
 * Parse API error response and extract user-friendly message
 * @param {Error|Object|string} error - Error object or response
 * @returns {Object} Parsed error with message and status
 */
function parseApiError(error) {
    // If it's a string, try to parse as JSON
    if (typeof error === 'string') {
        try {
            error = JSON.parse(error);
        } catch (e) {
            return { message: error, status: null };
        }
    }

    // If error has a message that looks like JSON, parse it
    if (error.message && typeof error.message === 'string') {
        try {
            if (error.message.startsWith('{')) {
                const parsed = JSON.parse(error.message);
                return {
                    message: parsed.error?.message || parsed.message || 'An error occurred',
                    status: parsed.status || parsed.error?.status || error.status,
                    errorCode: parsed.error?.errorCode || parsed.errorCode
                };
            }
        } catch (e) {
            // Not JSON, use as-is
        }
    }

    return {
        message: error.error?.message || error.message || 'An error occurred',
        status: error.status || error.error?.status,
        errorCode: error.error?.errorCode || error.errorCode
    };
}

/**
 * Handle file-related errors with contextual messages
 */
function handleFileError(error, operation = 'access') {
    console.error('File Error:', error);

    // Parse the error to get clean message
    const parsed = parseApiError(error);
    const status = parsed.status || error.status || (error.response && error.response.status);
    const errorCode = parsed.errorCode || error.errorCode;

    // Handle by error code first (most specific)
    if (errorCode === 'FILE_NOT_FOUND') {
        Toast.error(ErrorMessages.file.notFound.message, ErrorMessages.file.notFound.title);
        return;
    }

    if (status === 404) {
        Toast.error(ErrorMessages.file.notFound.message, ErrorMessages.file.notFound.title);
        return;
    }

    if (status === 403) {
        Toast.error(ErrorMessages.folder.accessDenied.message, ErrorMessages.folder.accessDenied.title);
        return;
    }

    if (status === 500) {
        if (operation === 'preview') {
            Toast.error(ErrorMessages.file.previewFailed.message, ErrorMessages.file.previewFailed.title);
        } else if (operation === 'download') {
            Toast.error(ErrorMessages.file.downloadFailed.message, ErrorMessages.file.downloadFailed.title);
        } else if (operation === 'upload') {
            Toast.error(ErrorMessages.file.uploadFailed.message, ErrorMessages.file.uploadFailed.title);
        } else {
            const { title, message } = ErrorMessages.getHttpErrorMessage(500);
            Toast.error(message, title);
        }
        return;
    }

    // Check error message for specific issues
    const errorMsg = parsed.message || '';
    if (errorMsg.includes('corrupt') || errorMsg.includes('unsupported format')) {
        Toast.error(ErrorMessages.file.corrupted.message, ErrorMessages.file.corrupted.title);
        return;
    }

    // Fallback to generic error with friendly message
    const friendlyMessage = getFriendlyErrorMessage(errorMsg, operation);
    Toast.error(friendlyMessage, getErrorTitle(operation));
}

/**
 * Get a user-friendly error message
 */
function getFriendlyErrorMessage(rawMessage, operation) {
    // Remove technical details and return friendly message
    const lowerMsg = (rawMessage || '').toLowerCase();
    
    if (lowerMsg.includes('not found') || lowerMsg.includes('null')) {
        return 'This item could not be found. It may have been moved or deleted.';
    }
    if (lowerMsg.includes('permission') || lowerMsg.includes('denied') || lowerMsg.includes('unauthorized')) {
        return 'You don\'t have permission to perform this action.';
    }
    if (lowerMsg.includes('network') || lowerMsg.includes('fetch')) {
        return 'Unable to connect to the server. Please check your internet connection.';
    }
    if (lowerMsg.includes('timeout')) {
        return 'The request took too long. Please try again.';
    }
    
    // Default messages based on operation
    const defaultMessages = {
        'download': 'Unable to download this file. Please try again.',
        'preview': 'Unable to preview this file. You can try downloading it instead.',
        'upload': 'Unable to upload this file. Please try again.',
        'access': 'Unable to access this item. Please try again.'
    };
    
    return defaultMessages[operation] || 'Something went wrong. Please try again.';
}

/**
 * Get error title based on operation
 */
function getErrorTitle(operation) {
    const titles = {
        'download': 'Download Failed',
        'preview': 'Preview Unavailable',
        'upload': 'Upload Failed',
        'access': 'Access Error'
    };
    return titles[operation] || 'Error';
}

// Export for use in other modules
window.Toast = Toast;
window.ErrorMessages = ErrorMessages;
window.handleApiError = handleApiError;
window.handleFileError = handleFileError;
window.parseApiError = parseApiError;
window.getFriendlyErrorMessage = getFriendlyErrorMessage;

// Also export as EnhancedToast for compatibility with existing code
window.EnhancedToast = {
    show: (message, type, options = {}) => {
        return Toast.show({
            message,
            type,
            ...options
        });
    },
    success: (message, options) => Toast.success(message, 'Success', options),
    error: (message, options) => Toast.error(message, 'Error', options),
    warning: (message, options) => Toast.warning(message, 'Warning', options),
    info: (message, options) => Toast.info(message, 'Info', options)
};

export { Toast, ErrorMessages, handleApiError, handleFileError, parseApiError, getFriendlyErrorMessage };
export default Toast;

