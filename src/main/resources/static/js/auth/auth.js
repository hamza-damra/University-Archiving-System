/**
 * Authentication Page (Login)
 */

import { auth, saveAuthData, isAuthenticated, initializeAuth } from '../core/api.js';
import { showToast, isValidEmail } from '../core/ui.js';

// Rate limiting state
let rateLimitedUntil = 0;
let countdownInterval = null;

// Helper function to redirect based on role - defined early to avoid reference errors
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
        // DOM might not be ready, use alert as fallback
        const errorEl = document.getElementById('generalError');
        if (errorEl) {
            errorEl.textContent = 'Unknown user role. Please contact administrator.';
            errorEl.classList.remove('hidden');
        } else {
            alert('Unknown user role. Please contact administrator.');
        }
    }
}

// Show login page content (called when auth check completes and user is not authenticated)
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

// Check if already logged in with valid token
(async function checkExistingAuth() {
    if (isAuthenticated()) {
        // Validate the existing token with the server
        // This prevents stale localStorage data from causing auto-redirect
        const isValid = await initializeAuth();
        if (isValid) {
            const userInfo = JSON.parse(localStorage.getItem('userInfo'));
            if (userInfo && userInfo.role) {
                // User is authenticated - redirect immediately
                // Keep the loading overlay visible during redirect for smooth UX
                redirectToDashboard(userInfo.role);
                return;
            }
        }
        // Token was invalid or user info missing - explicitly clear all auth data
        // to prevent any future auto-redirect attempts with stale data
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userInfo');
        console.log('Cleared invalid/expired auth data');
    }
    
    // Not authenticated or auth failed - show the login page
    showLoginPage();
})();

// Check for error parameters in URL
const urlParams = new URLSearchParams(window.location.search);
const errorParam = urlParams.get('error');

// DOM Elements
const loginForm = document.getElementById('loginForm');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const emailError = document.getElementById('emailError');
const passwordError = document.getElementById('passwordError');
const generalError = document.getElementById('generalError');
const submitBtn = document.getElementById('submitBtn');
const btnText = submitBtn?.querySelector('.btn-text');

// Display error message if present in URL
if (errorParam) {
    if (errorParam === 'access_denied') {
        showGeneralError('Access denied. You do not have permission to access that page. Please log in with appropriate credentials.');
    } else if (errorParam === 'session_expired') {
        showGeneralError('Your session has expired. Please log in again.');
    } else if (errorParam === 'unauthorized') {
        showGeneralError('Unauthorized access. Please log in to continue.');
    }
}

// Form submission
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    // Check if currently rate limited
    if (isRateLimited()) {
        return;
    }
    
    // Clear previous errors
    clearErrors();
    
    // Get form values
    const email = emailInput.value.trim();
    const password = passwordInput.value;
    
    // Validate
    let isValid = true;
    
    if (!email) {
        showFieldError(emailError, 'Email is required');
        isValid = false;
    } else if (!isValidEmail(email)) {
        showFieldError(emailError, 'Please enter a valid email address');
        isValid = false;
    }
    
    if (!password) {
        showFieldError(passwordError, 'Password is required');
        isValid = false;
    }
    
    if (!isValid) return;
    
    // Show loading state on button with smooth animation
    setButtonLoadingState(true, 'Signing in...');
    
    try {
        // Call login API
        const response = await auth.login({ email, password });
        
        // Check if response has required fields
        // Note: apiRequest now extracts data from ApiResponse wrapper automatically
        if (response && response.token) {
            const { token, refreshToken, role, firstName, lastName, email: userEmail, id, departmentId, departmentName } = response;
            
            // Save auth data including refresh token
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
            
            // Show success state on button
            setButtonSuccessState();
            
            // Show success message
            showToast('Login successful! Redirecting...', 'success');
            
            // Redirect based on role
            setTimeout(() => {
                redirectToDashboard(role);
            }, 800);
        } else {
            throw new Error('Invalid response from server');
        }
    } catch (error) {
        console.error('Login error:', error);
        console.log('Error details:', { code: error.code, message: error.message, retryAfterSeconds: error.retryAfterSeconds });
        
        // Check if it's a rate limit error
        if (error.code === 'RATE_LIMIT_EXCEEDED' || error.message?.includes('Too many')) {
            const retryAfterSeconds = error.retryAfterSeconds || 60;
            console.log('Starting rate limit countdown for', retryAfterSeconds, 'seconds');
            startRateLimitCountdown(retryAfterSeconds);
        } else if (error.code === 'ACCOUNT_DISABLED') {
            showGeneralError('Your account has been deactivated. Please contact your administrator.');
        } else if (error.code === 'ACCOUNT_LOCKED') {
            showGeneralError('Your account has been locked. Please contact your administrator.');
        } else {
            showGeneralError(error.message || 'Invalid email or password. Please try again.');
        }
        // Restore button state on error
        setButtonLoadingState(false);
    }
});

// Clear errors when typing
emailInput.addEventListener('input', () => clearFieldError(emailError));
passwordInput.addEventListener('input', () => clearFieldError(passwordError));

// Helper functions
function showFieldError(element, message) {
    element.textContent = message;
    element.classList.remove('hidden');
    element.previousElementSibling.classList.add('border-red-500');
}

function clearFieldError(element) {
    element.textContent = '';
    element.classList.add('hidden');
    element.previousElementSibling.classList.remove('border-red-500');
}

function showGeneralError(message) {
    const errorEl = document.getElementById('generalError');
    if (errorEl) {
        errorEl.textContent = message;
        errorEl.classList.remove('hidden');
    }
}

function clearErrors() {
    clearFieldError(emailError);
    clearFieldError(passwordError);
    generalError.classList.add('hidden');
}

/**
 * Start a countdown timer when rate limited
 * @param {number} seconds - Seconds until rate limit expires
 */
function startRateLimitCountdown(seconds) {
    // Clear any existing countdown
    if (countdownInterval) {
        clearInterval(countdownInterval);
    }
    
    rateLimitedUntil = Date.now() + (seconds * 1000);
    
    // Restore button first, then apply rate limit styling
    setButtonLoadingState(false);
    submitBtn.disabled = true;
    submitBtn.classList.add('opacity-50', 'cursor-not-allowed');
    
    const updateCountdown = () => {
        const remaining = Math.ceil((rateLimitedUntil - Date.now()) / 1000);
        
        if (remaining <= 0) {
            // Rate limit expired
            clearInterval(countdownInterval);
            countdownInterval = null;
            rateLimitedUntil = 0;
            
            // Re-enable the submit button
            submitBtn.disabled = false;
            submitBtn.classList.remove('opacity-50', 'cursor-not-allowed');
            
            // Clear the error message
            generalError.classList.add('hidden');
            return;
        }
        
        // Format the time
        const minutes = Math.floor(remaining / 60);
        const secs = remaining % 60;
        const timeStr = minutes > 0 
            ? `${minutes}:${secs.toString().padStart(2, '0')}` 
            : `${secs} second${secs !== 1 ? 's' : ''}`;
        
        // Show countdown in error message
        showGeneralError(`Too many login attempts. Please wait ${timeStr} before trying again.`);
    };
    
    // Update immediately and then every second
    updateCountdown();
    countdownInterval = setInterval(updateCountdown, 1000);
}

/**
 * Check if currently rate limited
 * @returns {boolean} True if rate limited
 */
function isRateLimited() {
    return rateLimitedUntil > Date.now();
}



// Handle Enter key
emailInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') passwordInput.focus();
});

/**
 * Set button loading state with smooth animation
 * @param {boolean} isLoading - Whether to show loading state
 * @param {string} text - Text to display while loading
 */
function setButtonLoadingState(isLoading, text = 'Signing in...') {
    if (!submitBtn) return;
    
    if (isLoading) {
        submitBtn.disabled = true;
        submitBtn.classList.add('btn-loading');
        if (btnText) {
            btnText.textContent = text;
        }
    } else {
        submitBtn.disabled = false;
        submitBtn.classList.remove('btn-loading', 'btn-success');
        if (btnText) {
            btnText.textContent = 'Sign In';
        }
    }
}

/**
 * Set button to success state with checkmark animation
 */
function setButtonSuccessState() {
    if (!submitBtn) return;
    
    submitBtn.classList.remove('btn-loading');
    submitBtn.classList.add('btn-success');
    if (btnText) {
        btnText.textContent = 'Success!';
    }
}
