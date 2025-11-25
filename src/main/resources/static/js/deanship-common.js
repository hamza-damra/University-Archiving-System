/**
 * Deanship Common Module
 * Shared layout and functionality for multi-page deanship dashboard
 * 
 * ARCHITECTURAL DECISION: Shared Layout via JavaScript Class
 * ============================================================
 * This module implements a shared layout pattern using a JavaScript class rather than
 * server-side templates. This approach was chosen for the following reasons:
 * 
 * 1. Frontend Consistency: All layout logic lives in JavaScript, making it easier to
 *    maintain and test without mixing server-side and client-side rendering.
 * 
 * 2. Dynamic Behavior: The layout needs to respond to user interactions (filter changes,
 *    navigation) which is more natural to implement in JavaScript.
 * 
 * 3. State Management: Academic year/semester selections need to persist across pages
 *    and be accessible to page-specific logic. A JavaScript class provides a clean
 *    interface for this.
 * 
 * 4. Callback Pattern: Pages can register callbacks to be notified when academic context
 *    changes, enabling reactive data loading without tight coupling.
 * 
 * 5. Simplicity: No need for complex template inheritance or server-side rendering.
 *    Each page is a simple HTML file that imports this module.
 * 
 * TRADE-OFFS:
 * - Some HTML duplication across pages (header, nav, filters structure)
 * - Requires JavaScript to be enabled (acceptable for admin dashboard)
 * - Initial page load includes layout initialization overhead
 * 
 * USAGE PATTERN:
 * Each page should:
 * 1. Import this module: import { DeanshipLayout } from './deanship-common.js';
 * 2. Create instance: const layout = new DeanshipLayout();
 * 3. Initialize: await layout.initialize();
 * 4. Register callbacks if needed: layout.onContextChange(() => loadData());
 * 
 * @module deanship-common
 * @version 1.0
 * @since 2024-11-20
 */

import { apiRequest, getUserInfo, clearAuthData, isAuthenticated, redirectToLogin } from './api.js';
import { showToast } from './ui.js';

/**
 * DeanshipLayout class
 * Manages shared layout, navigation, and academic context across all deanship pages
 * 
 * RESPONSIBILITIES:
 * - Authentication verification
 * - User information display
 * - Academic year/semester selection and persistence
 * - Navigation highlighting
 * - Context change notifications
 * - Logout functionality
 */
export class DeanshipLayout {
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
     * - Check authentication
     * - Load user info
     * - Load academic years
     * - Restore selections from localStorage
     * - Set up event listeners
     * - Highlight active navigation link
     */
    async initialize() {
        try {
            // Check authentication
            if (!isAuthenticated()) {
                redirectToLogin();
                return;
            }

            // Get user info
            this.userInfo = getUserInfo();
            if (!this.userInfo) {
                redirectToLogin();
                return;
            }

            // Verify user has DEANSHIP role
            if (!this.userInfo.role || this.userInfo.role !== 'ROLE_DEANSHIP') {
                showToast('Access denied - Deanship role required', 'error');
                setTimeout(() => redirectToLogin(), 2000);
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

            console.log('[DeanshipLayout] Initialized successfully');
        } catch (error) {
            console.error('[DeanshipLayout] Initialization error:', error);
            showToast('Failed to initialize dashboard', 'error');
        }
    }

    /**
     * Display user information in header
     */
    displayUserInfo() {
        const userNameElement = document.getElementById('deanshipName');
        if (userNameElement && this.userInfo) {
            userNameElement.textContent = this.userInfo.name || 'Dean User';
        }
    }

    /**
     * Load academic years from API
     */
    async loadAcademicYears() {
        try {
            this.academicYears = await apiRequest('/deanship/academic-years', {
                method: 'GET',
            });

            this.renderAcademicYearSelect();
        } catch (error) {
            console.error('[DeanshipLayout] Failed to load academic years:', error);
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
            console.error('[DeanshipLayout] Failed to load semesters:', error);
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
     * 
     * ARCHITECTURAL DECISION: LocalStorage for State Persistence
     * ===========================================================
     * Academic year and semester selections are persisted to browser localStorage
     * rather than server-side session storage. This approach was chosen because:
     * 
     * 1. Fast Access: No server round-trip needed to restore selections
     * 2. Cross-Page Persistence: Selections survive page navigation and refresh
     * 3. Simple Implementation: No server-side session management required
     * 4. User-Specific: Each browser maintains its own context
     * 
     * TRADE-OFFS:
     * - Not synchronized across devices/browsers
     * - Can be cleared by user (acceptable - they can reselect)
     * - Limited to 5-10MB storage (more than sufficient for our needs)
     * 
     * STORAGE KEYS:
     * - deanship_selected_academic_year: Selected academic year ID
     * - deanship_selected_semester: Selected semester ID
     * - deanship_last_page: Last visited page (for future navigation restoration)
     */
    restoreSelections() {
        const savedAcademicYearId = localStorage.getItem('deanship_selected_academic_year');
        const savedSemesterId = localStorage.getItem('deanship_selected_semester');

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
                    localStorage.setItem('deanship_selected_academic_year', academicYearId);
                    
                    // Load semesters for selected academic year
                    await this.loadSemesters(academicYearId);
                    
                    // Clear semester selection
                    this.selectedSemesterId = null;
                    localStorage.removeItem('deanship_selected_semester');
                    
                    // Notify listeners
                    this.notifyContextChange();
                } else {
                    this.selectedAcademicYearId = null;
                    this.selectedSemesterId = null;
                    localStorage.removeItem('deanship_selected_academic_year');
                    localStorage.removeItem('deanship_selected_semester');
                    
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
                    localStorage.setItem('deanship_selected_semester', semesterId);
                } else {
                    this.selectedSemesterId = null;
                    localStorage.removeItem('deanship_selected_semester');
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
                console.error('[DeanshipLayout] Error in context change callback:', error);
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
            
            // Clear deanship-specific localStorage
            localStorage.removeItem('deanship_selected_academic_year');
            localStorage.removeItem('deanship_selected_semester');
            localStorage.removeItem('deanship_last_page');
            
            // Show message
            showToast('Logged out successfully', 'success');
            
            // Redirect to login
            setTimeout(() => {
                window.location.href = '/index.html';
            }, 500);
        } catch (error) {
            console.error('[DeanshipLayout] Logout error:', error);
            // Force redirect even if error occurs
            window.location.href = '/index.html';
        }
    }

    /**
     * Save current page to localStorage
     */
    saveCurrentPage() {
        const currentPath = window.location.pathname;
        localStorage.setItem('deanship_last_page', currentPath);
    }

    /**
     * Get last visited page from localStorage
     * @returns {string|null} Last page path or null
     */
    getLastPage() {
        return localStorage.getItem('deanship_last_page');
    }
}

export default DeanshipLayout;
