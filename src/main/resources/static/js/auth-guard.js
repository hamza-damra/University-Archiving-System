/**
 * Auth Guard - Immediate Authentication Check
 * This script MUST be loaded as early as possible in protected pages.
 * It checks for the presence of authentication token and redirects to login if not found.
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
    const userInfo = localStorage.getItem('userInfo');
    
    // If no token or userInfo, immediately redirect to login
    if (!token || !userInfo) {
        // Clear any stale data
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userInfo');
        
        // Redirect to login with unauthorized error
        window.location.replace('/index.html?error=unauthorized');
        
        // Stop script execution and prevent page from rendering
        // This works by throwing an error that stops the page load
        document.documentElement.innerHTML = '';
        throw new Error('Unauthorized - Redirecting to login');
    }
    
    // Additional check: verify userInfo is valid JSON with required fields
    try {
        const user = JSON.parse(userInfo);
        if (!user.role || !user.email) {
            throw new Error('Invalid user info');
        }
        
        // Optional: Check role-based access for specific pages
        const path = window.location.pathname.toLowerCase();
        
        if (path.includes('/admin/') && user.role !== 'ROLE_ADMIN') {
            window.location.replace('/index.html?error=access_denied');
            document.documentElement.innerHTML = '';
            throw new Error('Access denied - Redirecting to login');
        }
        
        if (path.includes('deanship') && user.role !== 'ROLE_DEANSHIP' && user.role !== 'ROLE_ADMIN') {
            window.location.replace('/index.html?error=access_denied');
            document.documentElement.innerHTML = '';
            throw new Error('Access denied - Redirecting to login');
        }
        
        if (path.includes('hod-dashboard') && user.role !== 'ROLE_HOD' && user.role !== 'ROLE_ADMIN') {
            window.location.replace('/index.html?error=access_denied');
            document.documentElement.innerHTML = '';
            throw new Error('Access denied - Redirecting to login');
        }
        
        if (path.includes('prof-dashboard') && user.role !== 'ROLE_PROFESSOR' && user.role !== 'ROLE_ADMIN') {
            window.location.replace('/index.html?error=access_denied');
            document.documentElement.innerHTML = '';
            throw new Error('Access denied - Redirecting to login');
        }
        
    } catch (e) {
        // Invalid userInfo JSON or missing required fields
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userInfo');
        window.location.replace('/index.html?error=session_expired');
        document.documentElement.innerHTML = '';
        throw new Error('Invalid session - Redirecting to login');
    }
})();
