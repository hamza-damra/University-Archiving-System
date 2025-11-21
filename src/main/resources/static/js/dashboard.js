/**
 * Dashboard Page Module
 * Main landing page for deanship dashboard with overview cards
 */

import { DeanshipLayout } from './deanship-common.js';
import { apiRequest } from './api.js';
import { showToast } from './ui.js';

/**
 * Dashboard Page Class
 */
class DashboardPage {
    constructor() {
        this.layout = new DeanshipLayout();
        this.stats = {
            activeAcademicYears: 0,
            totalProfessors: 0,
            activeProfessors: 0,
            totalCourses: 0,
            activeCourses: 0,
            totalAssignments: 0,
            submissionCompletionRate: 0,
        };
    }

    /**
     * Initialize the dashboard page
     */
    async initialize() {
        try {
            // Show loading state
            this.showPageLoading(true);
            
            // Initialize shared layout
            await this.layout.initialize();
            
            // Load dashboard statistics
            await this.loadDashboardStats();
            
            // Render dashboard cards
            this.renderDashboardCards();
            
            // Register context change listener to reload stats
            this.layout.onContextChange(() => {
                this.loadDashboardStats();
            });
            
            // Hide loading state
            this.showPageLoading(false);
            
            console.log('[Dashboard] Initialized successfully');
        } catch (error) {
            console.error('[Dashboard] Initialization error:', error);
            this.showPageLoading(false);
            this.handleError('Failed to initialize dashboard. Please refresh the page.', error);
        }
    }

    /**
     * Load dashboard statistics from APIs
     */
    async loadDashboardStats() {
        try {
            // Show loading indicators on cards
            this.showCardsLoading(true);
            
            // Load academic years count
            await this.loadAcademicYearsCount();
            
            // Load professors count
            await this.loadProfessorsCount();
            
            // Load courses count
            await this.loadCoursesCount();
            
            // Load assignments count (if semester is selected)
            if (this.layout.hasContext()) {
                await this.loadAssignmentsCount();
                await this.loadSubmissionReport();
            }
            
            // Update card displays
            this.updateCardStats();
            
            // Hide loading indicators
            this.showCardsLoading(false);
            
        } catch (error) {
            console.error('[Dashboard] Failed to load stats:', error);
            this.showCardsLoading(false);
            // Don't show toast for stats loading errors - cards will show default values
        }
    }

    /**
     * Load active academic years count
     */
    async loadAcademicYearsCount() {
        try {
            const academicYears = await apiRequest('/deanship/academic-years', {
                method: 'GET',
            });
            
            // Count active academic years
            this.stats.activeAcademicYears = academicYears.filter(year => year.active).length;
        } catch (error) {
            console.error('[Dashboard] Failed to load academic years:', error);
            this.handleApiError(error, 'academic years');
            this.stats.activeAcademicYears = 0;
        }
    }

    /**
     * Load professors count
     */
    async loadProfessorsCount() {
        try {
            const professors = await apiRequest('/deanship/professors', {
                method: 'GET',
            });
            
            this.stats.totalProfessors = professors.length;
            this.stats.activeProfessors = professors.filter(prof => prof.active).length;
        } catch (error) {
            console.error('[Dashboard] Failed to load professors:', error);
            this.handleApiError(error, 'professors');
            this.stats.totalProfessors = 0;
            this.stats.activeProfessors = 0;
        }
    }

    /**
     * Load courses count
     */
    async loadCoursesCount() {
        try {
            const courses = await apiRequest('/deanship/courses', {
                method: 'GET',
            });
            
            this.stats.totalCourses = courses.length;
            this.stats.activeCourses = courses.filter(course => course.active).length;
        } catch (error) {
            console.error('[Dashboard] Failed to load courses:', error);
            this.handleApiError(error, 'courses');
            this.stats.totalCourses = 0;
            this.stats.activeCourses = 0;
        }
    }

    /**
     * Load assignments count for selected semester
     */
    async loadAssignmentsCount() {
        try {
            const context = this.layout.getSelectedContext();
            if (!context.semesterId) {
                this.stats.totalAssignments = 0;
                return;
            }
            
            const assignments = await apiRequest(`/deanship/course-assignments?semesterId=${context.semesterId}`, {
                method: 'GET',
            });
            
            this.stats.totalAssignments = assignments.length;
        } catch (error) {
            console.error('[Dashboard] Failed to load assignments:', error);
            this.handleApiError(error, 'assignments');
            this.stats.totalAssignments = 0;
        }
    }

    /**
     * Load submission report for selected semester
     */
    async loadSubmissionReport() {
        try {
            const context = this.layout.getSelectedContext();
            if (!context.semesterId) {
                this.stats.submissionCompletionRate = 0;
                return;
            }
            
            const report = await apiRequest(`/deanship/reports/system-wide?semesterId=${context.semesterId}`, {
                method: 'GET',
            });
            
            // Calculate completion percentage
            if (report && report.totalSubmissions > 0) {
                this.stats.submissionCompletionRate = Math.round(
                    (report.completedSubmissions / report.totalSubmissions) * 100
                );
            } else {
                this.stats.submissionCompletionRate = 0;
            }
        } catch (error) {
            console.error('[Dashboard] Failed to load submission report:', error);
            this.handleApiError(error, 'submission report');
            this.stats.submissionCompletionRate = 0;
        }
    }

    /**
     * Render dashboard cards
     */
    renderDashboardCards() {
        const cardsContainer = document.getElementById('dashboardCards');
        if (!cardsContainer) return;
        
        const cards = [
            {
                id: 'academic-years',
                icon: '📅',
                title: 'Academic Years',
                description: 'Manage academic years and semesters',
                statLabel: 'Active Years',
                statValue: this.stats.activeAcademicYears,
                link: '/deanship/academic-years',
            },
            {
                id: 'professors',
                icon: '👨‍🏫',
                title: 'Professors',
                description: 'Manage faculty members and their information',
                statLabel: 'Total Professors',
                statValue: this.stats.totalProfessors,
                link: '/deanship/professors',
            },
            {
                id: 'courses',
                icon: '📚',
                title: 'Courses',
                description: 'Manage course catalog and course information',
                statLabel: 'Total Courses',
                statValue: this.stats.totalCourses,
                link: '/deanship/courses',
            },
            {
                id: 'assignments',
                icon: '🎓',
                title: 'Course Assignments',
                description: 'Assign professors to courses for each semester',
                statLabel: 'Total Assignments',
                statValue: this.stats.totalAssignments,
                link: '/deanship/course-assignments',
            },
            {
                id: 'reports',
                icon: '📊',
                title: 'Reports',
                description: 'View submission status and analytics',
                statLabel: 'Completion Rate',
                statValue: `${this.stats.submissionCompletionRate}%`,
                link: '/deanship/reports',
            },
            {
                id: 'file-explorer',
                icon: '📁',
                title: 'File Explorer',
                description: 'Browse and manage archived documents',
                statLabel: '',
                statValue: '',
                link: '/deanship/file-explorer',
            },
        ];
        
        cardsContainer.innerHTML = cards.map(card => this.createCardHTML(card)).join('');
        
        // Add click handlers to buttons
        cards.forEach(card => {
            const button = document.getElementById(`btn-${card.id}`);
            if (button) {
                button.addEventListener('click', () => {
                    window.location.href = card.link;
                });
            }
        });
    }

    /**
     * Create HTML for a dashboard card
     */
    createCardHTML(card) {
        const statHTML = card.statValue !== '' && card.statValue !== 0
            ? `<div class="dashboard-card-stat" id="stat-${card.id}">${card.statValue}</div>`
            : '';
        
        return `
            <div class="dashboard-card">
                <div class="dashboard-card-icon">${card.icon}</div>
                <div class="dashboard-card-title">${card.title}</div>
                <div class="dashboard-card-description">${card.description}</div>
                ${statHTML}
                <button class="dashboard-card-button" id="btn-${card.id}">
                    Open ${card.title}
                </button>
            </div>
        `;
    }

    /**
     * Update card statistics after loading data
     */
    updateCardStats() {
        // Update academic years stat
        const academicYearsStat = document.getElementById('stat-academic-years');
        if (academicYearsStat) {
            academicYearsStat.textContent = this.stats.activeAcademicYears;
        }
        
        // Update professors stat
        const professorsStat = document.getElementById('stat-professors');
        if (professorsStat) {
            professorsStat.textContent = this.stats.totalProfessors;
        }
        
        // Update courses stat
        const coursesStat = document.getElementById('stat-courses');
        if (coursesStat) {
            coursesStat.textContent = this.stats.totalCourses;
        }
        
        // Update assignments stat
        const assignmentsStat = document.getElementById('stat-assignments');
        if (assignmentsStat) {
            assignmentsStat.textContent = this.stats.totalAssignments;
        }
        
        // Update reports stat
        const reportsStat = document.getElementById('stat-reports');
        if (reportsStat) {
            reportsStat.textContent = `${this.stats.submissionCompletionRate}%`;
        }
    }

    /**
     * Show/hide page loading state
     */
    showPageLoading(show) {
        const pageContent = document.getElementById('dashboardCards');
        if (pageContent) {
            pageContent.style.opacity = show ? '0.5' : '1';
            pageContent.style.pointerEvents = show ? 'none' : 'auto';
        }
    }

    /**
     * Show/hide loading state on cards
     */
    showCardsLoading(show) {
        // Visual feedback that stats are loading
        const cards = document.querySelectorAll('.dashboard-card-stat');
        cards.forEach(card => {
            if (show) {
                card.style.opacity = '0.5';
            } else {
                card.style.opacity = '1';
            }
        });
    }

    /**
     * Handle API errors with appropriate user feedback
     */
    handleApiError(error, resourceName) {
        // Check for specific error types
        if (error.status === 401) {
            console.error(`[Dashboard] Unauthorized access to ${resourceName}`);
            showToast('Your session has expired. Please log in again.', 'error');
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        } else if (error.status === 403) {
            console.error(`[Dashboard] Forbidden access to ${resourceName}`);
            showToast('You do not have permission to access this resource.', 'error');
        } else if (error.status === 500) {
            console.error(`[Dashboard] Server error loading ${resourceName}:`, error);
            showToast(`Server error loading ${resourceName}. Please try again later.`, 'error');
        } else if (error.message && error.message.includes('NetworkError')) {
            console.error(`[Dashboard] Network error loading ${resourceName}:`, error);
            showToast('Network error. Please check your connection and try again.', 'error');
        } else {
            console.error(`[Dashboard] Error loading ${resourceName}:`, error);
            // Silent failure for dashboard stats - don't overwhelm user with errors
        }
    }

    /**
     * Handle general errors with user feedback
     */
    handleError(message, error) {
        console.error('[Dashboard] Error:', error);
        showToast(message, 'error');
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const page = new DashboardPage();
    page.initialize();
});

export default DashboardPage;
