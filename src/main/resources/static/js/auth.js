/**
 * Authentication Page (Login)
 */

import { auth, saveAuthData, isAuthenticated, initializeAuth } from './api.js';
import { showToast, isValidEmail } from './ui.js';

// Helper function to redirect based on role - defined early to avoid reference errors
function redirectToDashboard(role) {
    if (role === 'ROLE_ADMIN') {
        window.location.href = '/admin/dashboard.html';
    } else if (role === 'ROLE_DEANSHIP') {
        window.location.href = '/deanship/dashboard';
    } else if (role === 'ROLE_HOD') {
        window.location.href = '/hod-dashboard.html';
    } else if (role === 'ROLE_PROFESSOR') {
        window.location.href = '/prof-dashboard.html';
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

// Check if already logged in with valid token
(async function checkExistingAuth() {
    if (isAuthenticated()) {
        // Validate the existing token
        const isValid = await initializeAuth();
        if (isValid) {
            const userInfo = JSON.parse(localStorage.getItem('userInfo'));
            redirectToDashboard(userInfo.role);
            return;
        }
        // Token was invalid, user will see login form
    }
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
const loadingOverlay = document.getElementById('loadingOverlay');

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
    
    // Show loading
    submitBtn.disabled = true;
    submitBtn.textContent = 'Signing in...';
    loadingOverlay.classList.remove('hidden');
    
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
            
            // Show success message
            showToast('Login successful! Redirecting...', 'success');
            
            // Redirect based on role
            setTimeout(() => {
                redirectToDashboard(role);
            }, 500);
        } else {
            throw new Error('Invalid response from server');
        }
    } catch (error) {
        console.error('Login error:', error);
        showGeneralError(error.message || 'Invalid email or password. Please try again.');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Sign In';
        loadingOverlay.classList.add('hidden');
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



// Handle Enter key
emailInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') passwordInput.focus();
});
