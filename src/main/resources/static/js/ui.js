/**
 * UI Helper Functions
 * Modals, toasts, and other UI components
 */

/**
 * Show a toast notification
 * @param {string} message - Toast message (supports multiline with \n)
 * @param {string} type - Toast type (success, error, info, warning)
 * @param {number} duration - Duration in milliseconds (default: 5000)
 * @param {Object} options - Additional options (action, actionLabel, onAction)
 */
export function showToast(message, type = 'info', duration = 5000, options = {}) {
    // Try to use enhanced toast if available
    if (typeof window.EnhancedToast !== 'undefined') {
        return window.EnhancedToast.show(message, type, { duration, ...options });
    }
    
    // Fallback to basic toast
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) return;

    const toast = document.createElement('div');
    toast.className = `slide-in flex items-start p-4 rounded-lg shadow-lg max-w-md ${getToastClasses(type)}`;
    toast.setAttribute('role', 'alert');

    const icon = getToastIcon(type);
    
    // Convert newlines to HTML breaks for multiline messages
    const formattedMessage = message.replace(/\n/g, '<br>');
    
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
    const adjustedDuration = message.includes('\n') ? Math.max(duration, 8000) : duration;
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
    modal.className = 'fixed inset-0 z-50 flex items-center justify-center p-4 fade-in';
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
};
