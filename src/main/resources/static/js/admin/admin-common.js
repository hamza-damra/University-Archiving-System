/**
 * Admin Common Module
 * Shared layout and functionality for multi-page admin dashboard
 * 
 * This module provides shared layout management for admin pages, including:
 * - Authentication verification (ROLE_ADMIN required)
 * - User information display
 * - Academic year/semester selection and persistence
 * - Navigation highlighting
 * - Context change notifications
 * - Logout functionality
 * 
 * @module admin-common
 */

import { apiRequest, getUserInfo, clearAuthData, isAuthenticated, redirectToLogin, initializeAuth } from '../core/api.js';
import { showToast } from '../core/ui.js';

/**
 * AdminLayout class
 * Manages shared layout, navigation, and academic context across all admin pages
 */
export class AdminLayout {
    constructor() {
        this.selectedAcademicYearId = null;
        this.selectedSemesterId = null;
        this.academicYears = [];
        this.semesters = [];
        this.contextChangeCallbacks = [];
        this.userInfo = null;
    }

    /**
     * Initialize the layout
     */
    async initialize() {
        try {
            // Check authentication
            if (!isAuthenticated()) {
                redirectToLogin();
                return;
            }

            // Validate token with server (will auto-refresh if expired)
            const isValid = await initializeAuth();
            if (!isValid) {
                redirectToLogin('session_expired');
                return;
            }

            // Get user info
            this.userInfo = getUserInfo();
            if (!this.userInfo) {
                redirectToLogin();
                return;
            }

            // Verify user has ADMIN role
            if (!this.userInfo.role || this.userInfo.role !== 'ROLE_ADMIN') {
                showToast('Access denied - Admin role required', 'error');
                setTimeout(() => redirectToLogin('access_denied'), 2000);
                return;
            }

            // Display user info
            this.displayUserInfo();

            // Initialize modern dropdowns
            if (typeof window.initModernDropdowns === 'function') {
                window.initModernDropdowns();
            }

            // Load academic years
            await this.loadAcademicYears();

            // Restore selections from localStorage
            this.restoreSelections();

            // Set up event listeners
            this.setupEventListeners();

            // Highlight active navigation link
            this.highlightActiveNavLink();

            // Track current page for navigation
            this.saveCurrentPage();

            console.log('[AdminLayout] Initialized successfully');
        } catch (error) {
            console.error('[AdminLayout] Initialization error:', error);
            showToast('Failed to initialize dashboard', 'error');
        }
    }

    /**
     * Display user information in header
     */
    displayUserInfo() {
        const userNameElement = document.getElementById('adminName');
        if (userNameElement && this.userInfo) {
            userNameElement.textContent = this.userInfo.name || 'Admin User';
        }
    }

    /**
     * Load academic years from API
     */
    async loadAcademicYears() {
        try {
            // Use deanship endpoint for academic years (admin has access)
            this.academicYears = await apiRequest('/deanship/academic-years', {
                method: 'GET',
            });

            this.renderAcademicYearSelect();
        } catch (error) {
            console.error('[AdminLayout] Failed to load academic years:', error);
            showToast('Failed to load academic years', 'error');
        }
    }

    /**
     * Render academic year dropdown
     */
    renderAcademicYearSelect() {
        const select = document.getElementById('academicYearSelect');
        if (!select) return;

        select.innerHTML = '<option value="">Select Academic Year</option>';
        
        this.academicYears.forEach(year => {
            const option = document.createElement('option');
            option.value = year.id;
            option.textContent = year.yearCode;
            if (year.active) {
                option.textContent += ' (Active)';
            }
            select.appendChild(option);
        });
        
        // Refresh modern dropdown
        if (typeof window.refreshModernDropdown === 'function') {
            window.refreshModernDropdown(select);
        }
    }

    /**
     * Load semesters for selected academic year
     */
    async loadSemesters(academicYearId) {
        try {
            this.semesters = await apiRequest(`/deanship/academic-years/${academicYearId}/semesters`, {
                method: 'GET',
            });

            this.renderSemesterSelect();
        } catch (error) {
            console.error('[AdminLayout] Failed to load semesters:', error);
            showToast('Failed to load semesters', 'error');
            this.semesters = [];
            this.renderSemesterSelect();
        }
    }

    /**
     * Render semester dropdown
     */
    renderSemesterSelect() {
        const select = document.getElementById('semesterSelect');
        if (!select) return;

        select.innerHTML = '<option value="">Select Semester</option>';
        
        this.semesters.forEach(semester => {
            const option = document.createElement('option');
            option.value = semester.id;
            option.textContent = `${semester.name} (${semester.startDate} - ${semester.endDate})`;
            select.appendChild(option);
        });
        
        // Refresh modern dropdown
        if (typeof window.refreshModernDropdown === 'function') {
            window.refreshModernDropdown(select);
        }
    }

    /**
     * Restore selections from localStorage
     */
    restoreSelections() {
        const savedAcademicYearId = localStorage.getItem('admin_selected_academic_year');
        const savedSemesterId = localStorage.getItem('admin_selected_semester');

        if (savedAcademicYearId) {
            const select = document.getElementById('academicYearSelect');
            if (select) {
                select.value = savedAcademicYearId;
                this.selectedAcademicYearId = parseInt(savedAcademicYearId);
                
                // Load semesters for this academic year
                this.loadSemesters(savedAcademicYearId).then(() => {
                    if (savedSemesterId) {
                        const semesterSelect = document.getElementById('semesterSelect');
                        if (semesterSelect) {
                            semesterSelect.value = savedSemesterId;
                            this.selectedSemesterId = parseInt(savedSemesterId);
                        }
                    }
                });
            }
        }
    }

    /**
     * Set up event listeners for dropdowns and logout button
     */
    setupEventListeners() {
        // Academic year dropdown
        const academicYearSelect = document.getElementById('academicYearSelect');
        if (academicYearSelect) {
            academicYearSelect.addEventListener('change', async (e) => {
                const academicYearId = e.target.value;
                
                if (academicYearId) {
                    this.selectedAcademicYearId = parseInt(academicYearId);
                    localStorage.setItem('admin_selected_academic_year', academicYearId);
                    
                    // Load semesters for selected academic year
                    await this.loadSemesters(academicYearId);
                    
                    // Clear semester selection
                    this.selectedSemesterId = null;
                    localStorage.removeItem('admin_selected_semester');
                    
                    // Notify listeners
                    this.notifyContextChange();
                } else {
                    this.selectedAcademicYearId = null;
                    this.selectedSemesterId = null;
                    localStorage.removeItem('admin_selected_academic_year');
                    localStorage.removeItem('admin_selected_semester');
                    
                    // Clear semester dropdown
                    this.semesters = [];
                    this.renderSemesterSelect();
                    
                    // Notify listeners
                    this.notifyContextChange();
                }
            });
        }

        // Semester dropdown
        const semesterSelect = document.getElementById('semesterSelect');
        if (semesterSelect) {
            semesterSelect.addEventListener('change', (e) => {
                const semesterId = e.target.value;
                
                if (semesterId) {
                    this.selectedSemesterId = parseInt(semesterId);
                    localStorage.setItem('admin_selected_semester', semesterId);
                } else {
                    this.selectedSemesterId = null;
                    localStorage.removeItem('admin_selected_semester');
                }
                
                // Notify listeners
                this.notifyContextChange();
            });
        }

        // Logout button
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => {
                this.logout();
            });
        }
    }

    /**
     * Highlight active navigation link based on current URL
     */
    highlightActiveNavLink() {
        const currentPath = window.location.pathname;
        const navLinks = document.querySelectorAll('.nav-link');
        
        navLinks.forEach(link => {
            const linkPath = new URL(link.href).pathname;
            if (linkPath === currentPath) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    }

    /**
     * Register a callback to be notified when academic context changes
     * @param {function} callback - Function to call when context changes
     */
    onContextChange(callback) {
        if (typeof callback === 'function') {
            this.contextChangeCallbacks.push(callback);
        }
    }

    /**
     * Notify all registered callbacks of context change
     */
    notifyContextChange() {
        const context = this.getSelectedContext();
        this.contextChangeCallbacks.forEach(callback => {
            try {
                callback(context);
            } catch (error) {
                console.error('[AdminLayout] Error in context change callback:', error);
            }
        });
    }

    /**
     * Get currently selected academic context
     * @returns {object} Context object with academicYearId, semesterId, academicYear, semester
     */
    getSelectedContext() {
        const academicYear = this.academicYears.find(y => y.id === this.selectedAcademicYearId);
        const semester = this.semesters.find(s => s.id === this.selectedSemesterId);
        
        return {
            academicYearId: this.selectedAcademicYearId,
            semesterId: this.selectedSemesterId,
            academicYear: academicYear || null,
            semester: semester || null,
        };
    }

    /**
     * Check if academic context is selected
     * @returns {boolean} True if both academic year and semester are selected
     */
    hasContext() {
        return this.selectedAcademicYearId !== null && this.selectedSemesterId !== null;
    }

    /**
     * Check if academic year is selected
     * @returns {boolean} True if academic year is selected
     */
    hasAcademicYear() {
        return this.selectedAcademicYearId !== null;
    }

    /**
     * Logout user
     */
    logout() {
        try {
            // Clear authentication data
            clearAuthData();
            
            // Clear admin-specific localStorage
            localStorage.removeItem('admin_selected_academic_year');
            localStorage.removeItem('admin_selected_semester');
            localStorage.removeItem('admin_last_page');
            
            // Show message
            showToast('Logged out successfully', 'success');
            
            // Redirect to login
            setTimeout(() => {
                window.location.href = '/index.html';
            }, 500);
        } catch (error) {
            console.error('[AdminLayout] Logout error:', error);
            // Force redirect even if error occurs
            window.location.href = '/index.html';
        }
    }

    /**
     * Save current page to localStorage
     */
    saveCurrentPage() {
        const currentPath = window.location.pathname;
        localStorage.setItem('admin_last_page', currentPath);
    }

    /**
     * Get last visited page from localStorage
     * @returns {string|null} Last page path or null
     */
    getLastPage() {
        return localStorage.getItem('admin_last_page');
    }
}

export default AdminLayout;
