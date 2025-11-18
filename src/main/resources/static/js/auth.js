/**
 * Authentication Page (Login)
 */

import { auth, saveAuthData, isAuthenticated } from './api.js';
import { showToast, isValidEmail } from './ui.js';

// Check if already logged in
if (isAuthenticated()) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo'));
    redirectToDashboard(userInfo.role);
}

// DOM Elements
const loginForm = document.getElementById('loginForm');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const emailError = document.getElementById('emailError');
const passwordError = document.getElementById('passwordError');
const generalError = document.getElementById('generalError');
const submitBtn = document.getElementById('submitBtn');
const loadingOverlay = document.getElementById('loadingOverlay');

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
            const { token, role, firstName, lastName, email: userEmail, id, departmentId, departmentName } = response;
            
            // Save auth data
            saveAuthData(token, {
                id,
                email: userEmail,
                firstName,
                lastName,
                role,
                departmentId,
                departmentName,
                fullName: `${firstName} ${lastName}`,
            });
            
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
    generalError.textContent = message;
    generalError.classList.remove('hidden');
}

function clearErrors() {
    clearFieldError(emailError);
    clearFieldError(passwordError);
    generalError.classList.add('hidden');
}

function redirectToDashboard(role) {
    if (role === 'ROLE_DEANSHIP') {
        window.location.href = '/deanship-dashboard.html';
    } else if (role === 'ROLE_HOD') {
        window.location.href = '/hod-dashboard.html';
    } else if (role === 'ROLE_PROFESSOR') {
        window.location.href = '/prof-dashboard.html';
    } else {
        showGeneralError('Unknown user role. Please contact administrator.');
    }
}

// Handle Enter key
emailInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') passwordInput.focus();
});
