/**
 * Error Handling Module for Deanship Dashboard
 * Provides error boundaries, centralized error handling, and fallback UI
 * 
 * @module deanship-error-handler
 * @version 1.0
 * @since 2024-11-22
 */

import { showToast } from './ui.js';

/**
 * ErrorBoundary class
 * Wraps components and provides fallback UI when errors occur
 * 
 * @class ErrorBoundary
 * @example
 * const boundary = new ErrorBoundary('analytics-container');
 * boundary.wrap(async () => {
 *   await loadAnalytics();
 * });
 */
export class ErrorBoundary {
    /**
     * Create an error boundary
     * @param {string} containerId - ID of the container element
     * @param {Object} options - Configuration options
     * @param {Function} options.onError - Custom error handler
     * @param {boolean} options.showRetry - Show retry button (default: true)
     * @param {string} options.fallbackMessage - Custom fallback message
     */
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.container = document.getElementById(containerId);
        this.options = {
            onError: options.onError || this.defaultErrorHandler.bind(this),
            showRetry: options.showRetry !== false,
            fallbackMessage: options.fallbackMessage || 'Something went wrong. Please try again.'
        };
        this.originalContent = null;
        this.wrappedFunction = null;
    }

    /**
     * Wrap a function with error boundary
     * @param {Function} fn - Function to wrap
     * @param {Object} context - Context for function execution
     * @returns {Promise<any>} Result of function execution
     */
    async wrap(fn, context = null) {
        this.wrappedFunction = fn;
        
        try {
            // Store original content before execution
            if (this.container && !this.originalContent) {
                this.originalContent = this.container.innerHTML;
            }
            
            // Execute the wrapped function
            const result = await (context ? fn.call(context) : fn());
            return result;
        } catch (error) {
            // Handle the error
            this.handleError(error);
            throw error; // Re-throw for upstream handling
        }
    }

    /**
     * Handle error and display fallback UI
     * @param {Error} error - The error that occurred
     */
    handleError(error) {
        console.error(`[ErrorBoundary:${this.containerId}]`, error);
        
        // Call custom error handler
        this.options.onError(error);
        
        // Display fallback UI
        this.showFallbackUI(error);
    }

    /**
     * Default error handler
     * @param {Error} error - The error that occurred
     */
    defaultErrorHandler(error) {
        const errorMessage = this.getErrorMessage(error);
        showToast(errorMessage, 'error');
    }

    /**
     * Show fallback UI in the container
     * @param {Error} error - The error that occurred
     */
    showFallbackUI(error) {
        if (!this.container) return;

        const errorMessage = this.getErrorMessage(error);
        const retryButton = this.options.showRetry ? `
            <button 
                onclick="window.errorBoundaries['${this.containerId}'].retry()"
                class="mt-4 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
            >
                <svg class="w-4 h-4 inline-block mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                </svg>
                Retry
            </button>
        ` : '';

        this.container.innerHTML = `
            <div class="flex flex-col items-center justify-center py-12 px-4">
                <svg class="w-16 h-16 text-red-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                </svg>
                <h3 class="text-lg font-semibold text-gray-900 mb-2">Oops! Something went wrong</h3>
                <p class="text-gray-600 text-center mb-2">${this.options.fallbackMessage}</p>
                <p class="text-sm text-gray-500 text-center">${errorMessage}</p>
                ${retryButton}
            </div>
        `;

        // Register this boundary globally for retry functionality
        if (!window.errorBoundaries) {
            window.errorBoundaries = {};
        }
        window.errorBoundaries[this.containerId] = this;
    }

    /**
     * Retry the wrapped function
     * @returns {Promise<any>} Result of function execution
     */
    async retry() {
        if (!this.wrappedFunction) {
            console.error('No function to retry');
            return;
        }

        // Restore original content or show loading
        if (this.container) {
            this.container.innerHTML = `
                <div class="flex items-center justify-center py-12">
                    <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
                </div>
            `;
        }

        try {
            const result = await this.wrappedFunction();
            return result;
        } catch (error) {
            this.handleError(error);
        }
    }

    /**
     * Extract user-friendly error message
     * @param {Error} error - The error object
     * @returns {string} User-friendly error message
     */
    getErrorMessage(error) {
        if (error.message) {
            return error.message;
        }
        if (typeof error === 'string') {
            return error;
        }
        return 'An unexpected error occurred';
    }

    /**
     * Reset the error boundary
     */
    reset() {
        if (this.container && this.originalContent) {
            this.container.innerHTML = this.originalContent;
        }
    }
}

/**
 * Global error handler for uncaught errors
 * @param {ErrorEvent} event - The error event
 */
export function setupGlobalErrorHandler() {
    window.addEventListener('error', (event) => {
        console.error('[Global Error]', event.error);
        
        // Log to console with context
        const errorInfo = {
            message: event.message,
            filename: event.filename,
            lineno: event.lineno,
            colno: event.colno,
            error: event.error
        };
        
        console.error('Error details:', errorInfo);
        
        // Show user-friendly toast
        showToast('An unexpected error occurred. Please refresh the page.', 'error');
    });

    window.addEventListener('unhandledrejection', (event) => {
        console.error('[Unhandled Promise Rejection]', event.reason);
        
        // Show user-friendly toast
        showToast('An operation failed. Please try again.', 'error');
    });
}

/**
 * Async operation wrapper with error handling
 * @param {Function} fn - Async function to execute
 * @param {Object} options - Configuration options
 * @param {Function} options.onError - Custom error handler
 * @param {Function} options.onSuccess - Success callback
 * @param {string} options.errorMessage - Custom error message
 * @returns {Promise<any>} Result of function execution
 */
export async function safeAsync(fn, options = {}) {
    try {
        const result = await fn();
        
        if (options.onSuccess) {
            options.onSuccess(result);
        }
        
        return result;
    } catch (error) {
        console.error('[SafeAsync Error]', error);
        
        if (options.onError) {
            options.onError(error);
        } else {
            const message = options.errorMessage || 'Operation failed';
            showToast(message, 'error');
        }
        
        throw error;
    }
}

/**
 * Retry an async operation with exponential backoff
 * @param {Function} fn - Async function to retry
 * @param {Object} options - Configuration options
 * @param {number} options.maxRetries - Maximum number of retries (default: 3)
 * @param {number} options.initialDelay - Initial delay in ms (default: 1000)
 * @param {number} options.maxDelay - Maximum delay in ms (default: 10000)
 * @param {Function} options.onRetry - Callback on each retry
 * @returns {Promise<any>} Result of function execution
 */
export async function retryAsync(fn, options = {}) {
    const {
        maxRetries = 3,
        initialDelay = 1000,
        maxDelay = 10000,
        onRetry = null
    } = options;

    let lastError;
    let delay = initialDelay;

    for (let attempt = 0; attempt <= maxRetries; attempt++) {
        try {
            return await fn();
        } catch (error) {
            lastError = error;
            
            if (attempt < maxRetries) {
                if (onRetry) {
                    onRetry(attempt + 1, maxRetries, error);
                }
                
                console.log(`Retry attempt ${attempt + 1}/${maxRetries} after ${delay}ms`);
                await new Promise(resolve => setTimeout(resolve, delay));
                
                // Exponential backoff
                delay = Math.min(delay * 2, maxDelay);
            }
        }
    }

    throw lastError;
}

/**
 * Create a debounced error handler
 * @param {Function} handler - Error handler function
 * @param {number} delay - Debounce delay in ms (default: 300)
 * @returns {Function} Debounced error handler
 */
export function debounceErrorHandler(handler, delay = 300) {
    let timeoutId;
    const errors = [];

    return function(error) {
        errors.push(error);
        
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => {
            handler(errors);
            errors.length = 0;
        }, delay);
    };
}

// Initialize global error handler
setupGlobalErrorHandler();
