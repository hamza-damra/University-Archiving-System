/**
 * Dashboard Page Module
 * Main landing page for deanship dashboard with overview cards and charts
 * Updated to use real data from DashboardWidgetService endpoints
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
            // New statistics from DashboardWidgetService
            totalHods: 0,
            totalDepartments: 0,
            totalSubmissions: 0,
            recentSubmissions: 0,
            pendingSubmissions: 0,
        };
        this.chartData = {
            submissions: [],
            departments: [],
        };
        this.charts = {};
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
            
            // Load dashboard statistics from real API
            await this.loadDashboardStats();
            
            // Render dashboard cards
            this.renderDashboardCards();
            
            // Load and render charts
            await this.loadChartData();
            this.renderCharts();
            
            // Register context change listener to reload stats
            this.layout.onContextChange(() => {
                this.loadDashboardStats();
                this.loadChartData();
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
     * Load dashboard statistics from DashboardWidgetService API
     */
    async loadDashboardStats() {
        try {
            // Show loading indicators on cards
            this.showCardsLoading(true);
            
            const context = this.layout.getSelectedContext();
            
            // Build query params for statistics endpoint
            const params = new URLSearchParams();
            if (context.academicYearId) {
                params.append('academicYearId', context.academicYearId);
            }
            if (context.semesterId) {
                params.append('semesterId', context.semesterId);
            }
            
            // Fetch real statistics from DashboardWidgetService
            const queryString = params.toString();
            const endpoint = `/deanship/dashboard/statistics${queryString ? '?' + queryString : ''}`;
            
            const response = await apiRequest(endpoint, { method: 'GET' });
            
            if (response && response.data) {
                const data = response.data;
                this.stats.totalProfessors = data.totalProfessors || 0;
                this.stats.totalHods = data.totalHods || 0;
                this.stats.totalDepartments = data.totalDepartments || 0;
                this.stats.totalCourses = data.totalCourses || 0;
                this.stats.totalSubmissions = data.totalSubmissions || 0;
                this.stats.recentSubmissions = data.recentSubmissions || 0;
                this.stats.pendingSubmissions = data.pendingSubmissions || 0;
            }
            
            // Also load academic years count (not in DashboardWidgetService)
            await this.loadAcademicYearsCount();
            
            // Load assignments count if semester is selected
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
            // Fallback to individual API calls if statistics endpoint fails
            await this.loadStatsFallback();
        }
    }

    /**
     * Fallback method to load stats from individual endpoints
     */
    async loadStatsFallback() {
        try {
            await this.loadAcademicYearsCount();
            await this.loadProfessorsCount();
            await this.loadCoursesCount();
            
            if (this.layout.hasContext()) {
                await this.loadAssignmentsCount();
                await this.loadSubmissionReport();
            }
            
            this.updateCardStats();
        } catch (error) {
            console.error('[Dashboard] Fallback stats loading failed:', error);
        }
    }

    /**
     * Load active academic years count
     */
    async loadAcademicYearsCount() {
        try {
            const response = await apiRequest('/deanship/academic-years', {
                method: 'GET',
            });
            
            // Handle ApiResponse wrapper
            const academicYears = response.data || response;
            
            // Count active academic years
            this.stats.activeAcademicYears = Array.isArray(academicYears) 
                ? academicYears.filter(year => year.active).length 
                : 0;
        } catch (error) {
            console.error('[Dashboard] Failed to load academic years:', error);
            this.handleApiError(error, 'academic years');
            this.stats.activeAcademicYears = 0;
        }
    }

    /**
     * Load professors count (fallback)
     */
    async loadProfessorsCount() {
        try {
            const response = await apiRequest('/deanship/professors', {
                method: 'GET',
            });
            
            const professors = response.data || response;
            
            this.stats.totalProfessors = Array.isArray(professors) ? professors.length : 0;
            this.stats.activeProfessors = Array.isArray(professors) 
                ? professors.filter(prof => prof.active).length 
                : 0;
        } catch (error) {
            console.error('[Dashboard] Failed to load professors:', error);
            this.handleApiError(error, 'professors');
            this.stats.totalProfessors = 0;
            this.stats.activeProfessors = 0;
        }
    }

    /**
     * Load courses count (fallback)
     */
    async loadCoursesCount() {
        try {
            const response = await apiRequest('/deanship/courses', {
                method: 'GET',
            });
            
            const courses = response.data || response;
            
            this.stats.totalCourses = Array.isArray(courses) ? courses.length : 0;
            this.stats.activeCourses = Array.isArray(courses) 
                ? courses.filter(course => course.active).length 
                : 0;
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
            
            const response = await apiRequest(`/deanship/course-assignments?semesterId=${context.semesterId}`, {
                method: 'GET',
            });
            
            const assignments = response.data || response;
            this.stats.totalAssignments = Array.isArray(assignments) ? assignments.length : 0;
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
            
            const response = await apiRequest(`/deanship/reports/system-wide?semesterId=${context.semesterId}`, {
                method: 'GET',
            });
            
            const report = response.data || response;
            
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
     * Load chart data from API endpoints
     */
    async loadChartData() {
        try {
            const context = this.layout.getSelectedContext();
            
            // Load submissions over time chart data
            await this.loadSubmissionsChartData();
            
            // Load department distribution chart data
            await this.loadDepartmentChartData(context.semesterId);
            
        } catch (error) {
            console.error('[Dashboard] Failed to load chart data:', error);
        }
    }

    /**
     * Load submissions over time chart data
     */
    async loadSubmissionsChartData() {
        try {
            // Default to last 6 months
            const endDate = new Date();
            const startDate = new Date();
            startDate.setMonth(startDate.getMonth() - 6);
            
            const params = new URLSearchParams({
                startDate: startDate.toISOString().split('T')[0],
                endDate: endDate.toISOString().split('T')[0],
                groupBy: 'MONTH'
            });
            
            const response = await apiRequest(`/deanship/dashboard/charts/submissions?${params}`, {
                method: 'GET',
            });
            
            this.chartData.submissions = response.data || response || [];
        } catch (error) {
            console.error('[Dashboard] Failed to load submissions chart data:', error);
            this.chartData.submissions = [];
        }
    }

    /**
     * Load department distribution chart data
     */
    async loadDepartmentChartData(semesterId) {
        try {
            const params = new URLSearchParams();
            if (semesterId) {
                params.append('semesterId', semesterId);
            }
            
            const queryString = params.toString();
            const endpoint = `/deanship/dashboard/charts/departments${queryString ? '?' + queryString : ''}`;
            
            const response = await apiRequest(endpoint, {
                method: 'GET',
            });
            
            this.chartData.departments = response.data || response || [];
        } catch (error) {
            console.error('[Dashboard] Failed to load department chart data:', error);
            this.chartData.departments = [];
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
        
        // Build cards HTML
        let cardsHTML = cards.map(card => this.createCardHTML(card)).join('');
        
        // Add charts section
        cardsHTML += this.createChartsSection();
        
        cardsContainer.innerHTML = cardsHTML;
        
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
     * Create charts section HTML
     */
    createChartsSection() {
        return `
            <div class="dashboard-charts-section" style="grid-column: 1 / -1; margin-top: var(--spacing-xl);">
                <h2 style="font-size: var(--font-size-xl); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin-bottom: var(--spacing-lg);">
                    Analytics Overview
                </h2>
                <div class="charts-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: var(--spacing-lg);">
                    <div class="chart-container" style="background-color: var(--color-bg-primary); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: var(--spacing-lg); box-shadow: var(--shadow-md);">
                        <h3 style="font-size: var(--font-size-lg); font-weight: var(--font-weight-medium); color: var(--color-text-primary); margin-bottom: var(--spacing-md);">
                            Submissions Over Time
                        </h3>
                        <div id="submissionsChart" style="height: 300px; display: flex; align-items: flex-end; gap: 8px; padding: var(--spacing-md) 0;">
                            <!-- Chart will be rendered here -->
                        </div>
                    </div>
                    <div class="chart-container" style="background-color: var(--color-bg-primary); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: var(--spacing-lg); box-shadow: var(--shadow-md);">
                        <h3 style="font-size: var(--font-size-lg); font-weight: var(--font-weight-medium); color: var(--color-text-primary); margin-bottom: var(--spacing-md);">
                            Department Distribution
                        </h3>
                        <div id="departmentChart" style="height: 300px; overflow-y: auto;">
                            <!-- Chart will be rendered here -->
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Render charts with real data
     */
    renderCharts() {
        this.renderSubmissionsChart();
        this.renderDepartmentChart();
    }

    /**
     * Render submissions over time bar chart
     */
    renderSubmissionsChart() {
        const container = document.getElementById('submissionsChart');
        if (!container) return;
        
        const data = this.chartData.submissions;
        
        if (!data || data.length === 0) {
            container.innerHTML = `
                <div style="display: flex; align-items: center; justify-content: center; height: 100%; color: var(--color-text-secondary);">
                    No submission data available
                </div>
            `;
            return;
        }
        
        // Find max value for scaling
        const maxValue = Math.max(...data.map(d => d.value || 0), 1);
        
        // Create bar chart
        const barsHTML = data.map(point => {
            const height = ((point.value || 0) / maxValue) * 100;
            const label = this.formatChartLabel(point.label);
            
            return `
                <div class="chart-bar-wrapper" style="flex: 1; display: flex; flex-direction: column; align-items: center; min-width: 40px;">
                    <div class="chart-bar-value" style="font-size: var(--font-size-sm); color: var(--color-text-secondary); margin-bottom: 4px;">
                        ${point.value || 0}
                    </div>
                    <div class="chart-bar" style="width: 100%; max-width: 50px; height: ${height}%; min-height: 4px; background-color: var(--color-primary); border-radius: var(--radius-sm) var(--radius-sm) 0 0; transition: height 0.3s ease;"></div>
                    <div class="chart-bar-label" style="font-size: var(--font-size-xs); color: var(--color-text-secondary); margin-top: 8px; text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 60px;" title="${point.label}">
                        ${label}
                    </div>
                </div>
            `;
        }).join('');
        
        container.innerHTML = barsHTML;
    }

    /**
     * Render department distribution chart
     */
    renderDepartmentChart() {
        const container = document.getElementById('departmentChart');
        if (!container) return;
        
        const data = this.chartData.departments;
        
        if (!data || data.length === 0) {
            container.innerHTML = `
                <div style="display: flex; align-items: center; justify-content: center; height: 100%; color: var(--color-text-secondary);">
                    No department data available
                </div>
            `;
            return;
        }
        
        // Find max submission count for scaling
        const maxSubmissions = Math.max(...data.map(d => d.submissionCount || 0), 1);
        
        // Create horizontal bar chart
        const barsHTML = data.map(dept => {
            const width = ((dept.submissionCount || 0) / maxSubmissions) * 100;
            const name = dept.departmentName || dept.departmentShortcut || 'Unknown';
            
            return `
                <div class="dept-bar-wrapper" style="margin-bottom: var(--spacing-md);">
                    <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
                        <span style="font-size: var(--font-size-sm); color: var(--color-text-primary); font-weight: var(--font-weight-medium);">
                            ${name}
                        </span>
                        <span style="font-size: var(--font-size-sm); color: var(--color-text-secondary);">
                            ${dept.submissionCount || 0} submissions
                        </span>
                    </div>
                    <div style="background-color: var(--color-bg-secondary); border-radius: var(--radius-sm); height: 24px; overflow: hidden;">
                        <div style="width: ${width}%; height: 100%; background-color: var(--color-primary); border-radius: var(--radius-sm); transition: width 0.3s ease; display: flex; align-items: center; padding-left: 8px;">
                            <span style="font-size: var(--font-size-xs); color: white; font-weight: var(--font-weight-medium);">
                                ${dept.professorCount || 0} professors, ${dept.courseCount || 0} courses
                            </span>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
        
        container.innerHTML = barsHTML;
    }

    /**
     * Format chart label for display
     */
    formatChartLabel(label) {
        if (!label) return '';
        
        // Handle YYYY-MM format
        if (/^\d{4}-\d{2}$/.test(label)) {
            const [year, month] = label.split('-');
            const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            return `${monthNames[parseInt(month) - 1]} ${year.slice(2)}`;
        }
        
        // Handle YYYY-Www format (week)
        if (/^\d{4}-W\d{2}$/.test(label)) {
            return label.replace(/^\d{4}-/, '');
        }
        
        // Handle YYYY-MM-DD format
        if (/^\d{4}-\d{2}-\d{2}$/.test(label)) {
            const date = new Date(label);
            return `${date.getMonth() + 1}/${date.getDate()}`;
        }
        
        return label;
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
        
        // Re-render charts when stats update
        this.renderCharts();
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
