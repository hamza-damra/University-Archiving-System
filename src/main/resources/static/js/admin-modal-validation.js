/**
 * Admin Modal Form Validation Utilities
 * Provides consistent form validation UI for admin modals
 */

/**
 * Show validation error on a form field
 * @param {string} fieldId - The ID of the input field
 * @param {string} message - The error message to display
 */
export function showFieldError(fieldId, message) {
    const field = document.getElementById(fieldId);
    const errorEl = document.getElementById(`${fieldId}Error`);
    
    if (field) {
        field.classList.add('error', 'shake');
        // Remove shake animation after it completes
        setTimeout(() => field.classList.remove('shake'), 400);
    }
    
    if (errorEl) {
        const messageSpan = errorEl.querySelector('span');
        if (messageSpan && message) {
            messageSpan.textContent = message;
        }
        errorEl.classList.add('visible');
    }
}

/**
 * Clear validation error on a form field
 * @param {string} fieldId - The ID of the input field
 */
export function clearFieldError(fieldId) {
    const field = document.getElementById(fieldId);
    const errorEl = document.getElementById(`${fieldId}Error`);
    
    if (field) {
        field.classList.remove('error', 'shake');
    }
    
    if (errorEl) {
        errorEl.classList.remove('visible');
    }
}

/**
 * Clear all validation errors in a form
 * @param {string} formId - The ID of the form
 */
export function clearFormErrors(formId) {
    const form = document.getElementById(formId);
    if (!form) return;
    
    // Clear error classes from inputs
    form.querySelectorAll('.admin-form-input, .admin-form-select, .admin-form-textarea').forEach(el => {
        el.classList.remove('error', 'shake');
    });
    
    // Hide all error messages
    form.querySelectorAll('.admin-form-error').forEach(el => {
        el.classList.remove('visible');
    });
}

/**
 * Validate a required field
 * @param {string} fieldId - The ID of the input field
 * @param {string} errorMessage - The error message to show if invalid
 * @returns {boolean} - True if valid, false otherwise
 */
export function validateRequired(fieldId, errorMessage = 'This field is required') {
    const field = document.getElementById(fieldId);
    if (!field) return true;
    
    const value = field.value.trim();
    
    if (!value) {
        showFieldError(fieldId, errorMessage);
        return false;
    }
    
    clearFieldError(fieldId);
    return true;
}

/**
 * Validate an email field
 * @param {string} fieldId - The ID of the email input field
 * @param {string} errorMessage - The error message to show if invalid
 * @returns {boolean} - True if valid, false otherwise
 */
export function validateEmail(fieldId, errorMessage = 'Please enter a valid email address') {
    const field = document.getElementById(fieldId);
    if (!field) return true;
    
    const value = field.value.trim();
    if (!value) return true; // Let required validation handle empty
    
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    
    if (!emailPattern.test(value)) {
        showFieldError(fieldId, errorMessage);
        return false;
    }
    
    clearFieldError(fieldId);
    return true;
}

/**
 * Validate a pattern
 * @param {string} fieldId - The ID of the input field
 * @param {RegExp} pattern - The regex pattern to match
 * @param {string} errorMessage - The error message to show if invalid
 * @returns {boolean} - True if valid, false otherwise
 */
export function validatePattern(fieldId, pattern, errorMessage = 'Invalid format') {
    const field = document.getElementById(fieldId);
    if (!field) return true;
    
    const value = field.value.trim();
    if (!value) return true; // Let required validation handle empty
    
    if (!pattern.test(value)) {
        showFieldError(fieldId, errorMessage);
        return false;
    }
    
    clearFieldError(fieldId);
    return true;
}

/**
 * Validate number range
 * @param {string} fieldId - The ID of the number input field
 * @param {number} min - Minimum value
 * @param {number} max - Maximum value
 * @param {string} errorMessage - The error message to show if invalid
 * @returns {boolean} - True if valid, false otherwise
 */
export function validateNumberRange(fieldId, min, max, errorMessage) {
    const field = document.getElementById(fieldId);
    if (!field) return true;
    
    const value = field.value.trim();
    if (!value) return true; // Let required validation handle empty
    
    const numValue = parseFloat(value);
    
    if (isNaN(numValue) || numValue < min || numValue > max) {
        showFieldError(fieldId, errorMessage || `Value must be between ${min} and ${max}`);
        return false;
    }
    
    clearFieldError(fieldId);
    return true;
}

/**
 * Set up real-time validation on input fields
 * Clears error state when user starts typing
 * @param {string} formId - The ID of the form
 */
export function setupRealTimeValidation(formId) {
    const form = document.getElementById(formId);
    if (!form) return;
    
    form.querySelectorAll('.admin-form-input, .admin-form-select, .admin-form-textarea').forEach(field => {
        field.addEventListener('input', () => {
            clearFieldError(field.id);
        });
        
        field.addEventListener('change', () => {
            clearFieldError(field.id);
        });
    });
}

/**
 * Set loading state on submit button
 * @param {string} buttonId - The ID of the submit button
 * @param {boolean} isLoading - Whether to show loading state
 */
export function setButtonLoading(buttonId, isLoading) {
    const button = document.getElementById(buttonId);
    if (!button) return;
    
    if (isLoading) {
        button.classList.add('loading');
        button.disabled = true;
    } else {
        button.classList.remove('loading');
        button.disabled = false;
    }
}
