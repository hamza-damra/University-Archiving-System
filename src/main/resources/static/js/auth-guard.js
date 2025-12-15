/**
 * Auth Guard - Immediate Authentication Check
 * This script MUST be loaded as early as possible in protected pages.
 * It checks for the presence of authentication token and validates it with the server.
 * If token is expired, it attempts to refresh. If refresh fails, redirects to login.
 * This prevents any page content from being displayed to unauthenticated users.
 */
(function() {
    'use strict';
    
    // Check if we're on the login page (index.html) - don't redirect from login
    const currentPath = window.location.pathname;
    if (currentPath === '/' || currentPath === '/index.html') {
        return;
    }
    
    // Check for authentication token
    const token = localStorage.getItem('token');
    const refreshToken = localStorage.getItem('refreshToken');
    const userInfo = localStorage.getItem('userInfo');
    
    // If no token or userInfo, immediately redirect to login
    if (!token || !userInfo) {
        clearAndRedirect('unauthorized');
        return;
    }
    
    // Additional check: verify userInfo is valid JSON with required fields
    let user;
    try {
        user = JSON.parse(userInfo);
        if (!user.role || !user.email) {
            throw new Error('Invalid user info');
        }
    } catch (e) {
        clearAndRedirect('session_expired');
        return;
    }
    
    // Check role-based access for specific pages
    const path = window.location.pathname.toLowerCase();
    
    if (path.includes('/admin/') && user.role !== 'ROLE_ADMIN') {
        clearAndRedirect('access_denied');
        return;
    }
    
    if (path.includes('deanship') && user.role !== 'ROLE_DEANSHIP' && user.role !== 'ROLE_ADMIN') {
        clearAndRedirect('access_denied');
        return;
    }
    
    if (path.includes('hod-dashboard') && user.role !== 'ROLE_HOD' && user.role !== 'ROLE_ADMIN') {
        clearAndRedirect('access_denied');
        return;
    }
    
    if (path.includes('prof-dashboard') && user.role !== 'ROLE_PROFESSOR' && user.role !== 'ROLE_ADMIN') {
        clearAndRedirect('access_denied');
        return;
    }
    
    // Now validate the token with the server (async but blocks page render)
    validateAndProceed(token, refreshToken);
    
    /**
     * Clear all auth data and redirect to login
     */
    function clearAndRedirect(error) {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userInfo');
        // Use replace to prevent back button returning to protected page
        window.location.replace('/index.html?error=' + error);
    }
    
    /**
     * Validate token with server and handle refresh if needed
     */
    function validateAndProceed(token, refreshToken) {
        // Show loading state while validating
        showLoadingOverlay();
        
        // Set a timeout to prevent infinite loading
        const timeoutId = setTimeout(function() {
            console.error('Auth validation timeout');
            clearAndRedirect('session_expired');
        }, 10000); // 10 second timeout
        
        // Validate current token
        fetch(window.location.origin + '/api/auth/validate', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + token
            }
        })
        .then(function(response) {
            // Check if response is ok (2xx status)
            if (!response.ok) {
                // Token invalid, try refresh
                if (refreshToken) {
                    return attemptRefresh(refreshToken, timeoutId);
                } else {
                    clearTimeout(timeoutId);
                    clearAndRedirect('session_expired');
                    return;
                }
            }
            return response.json();
        })
        .then(function(data) {
            if (!data) return; // Already handled
            
            console.log('Token validation response:', data);
            
            // Check if token is valid - handle both response formats
            var isValid = false;
            if (data.success && data.data) {
                isValid = data.data.valid === true;
            } else if (data.valid !== undefined) {
                isValid = data.valid === true;
            }
            
            console.log('Token is valid:', isValid);
            
            if (isValid) {
                // Token is valid, hide loading and allow page to render
                clearTimeout(timeoutId);
                hideLoadingOverlay();
                return;
            }
            
            // Token is invalid/expired, try to refresh
            if (refreshToken) {
                return attemptRefresh(refreshToken, timeoutId);
            } else {
                clearTimeout(timeoutId);
                clearAndRedirect('session_expired');
            }
        })
        .catch(function(error) {
            console.error('Token validation error:', error);
            clearTimeout(timeoutId);
            // On any error, try refresh token first
            if (refreshToken) {
                attemptRefresh(refreshToken, null);
            } else {
                clearAndRedirect('session_expired');
            }
        });
    }
    
    /**
     * Attempt to refresh the access token
     */
    function attemptRefresh(refreshToken, timeoutId) {
        return fetch(window.location.origin + '/api/auth/refresh-token', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ refreshToken: refreshToken })
        })
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Refresh failed');
            }
            return response.json();
        })
        .then(function(data) {
            if (timeoutId) clearTimeout(timeoutId);
            
            if (data.success && data.data && data.data.accessToken) {
                // Update token and allow page to render
                localStorage.setItem('token', data.data.accessToken);
                hideLoadingOverlay();
                return;
            }
            // Refresh failed
            clearAndRedirect('session_expired');
        })
        .catch(function(error) {
            if (timeoutId) clearTimeout(timeoutId);
            console.error('Token refresh error:', error);
            clearAndRedirect('session_expired');
        });
    }
    
    /**
     * Show a loading overlay while validating authentication
     */
    function showLoadingOverlay() {
        // Avoid duplicates
        if (document.getElementById('auth-loading-overlay')) {
            return;
        }

        // Create loading overlay
        var overlay = document.createElement('div');
        overlay.id = 'auth-loading-overlay';
        overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:#1a1a2e;display:flex;flex-direction:column;justify-content:center;align-items:center;z-index:999999;';
        overlay.innerHTML = '<div style="width:40px;height:40px;border:3px solid #3b82f6;border-top-color:transparent;border-radius:50%;animation:spin 1s linear infinite;"></div>' +
            '<p style="color:#9ca3af;margin-top:16px;font-family:Inter,sans-serif;">Verifying authentication...</p>' +
            '<style>@keyframes spin{to{transform:rotate(360deg);}}</style>';

        // Insert immediately even if <body> is not ready yet.
        // This prevents a race where validation finishes before the overlay is attached,
        // then the overlay gets attached later and never removed.
        if (document.body) {
            document.body.insertBefore(overlay, document.body.firstChild);
        } else if (document.documentElement) {
            document.documentElement.appendChild(overlay);
        }
    }
    
    /**
     * Hide the loading overlay
     */
    function hideLoadingOverlay() {
        var overlay = document.getElementById('auth-loading-overlay');
        if (overlay) {
            overlay.remove();
        }
    }
})();
