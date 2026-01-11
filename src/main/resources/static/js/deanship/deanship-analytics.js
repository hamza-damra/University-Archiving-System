/**
 * Analytics Components Module
 * Handles dashboard analytics, charts, and statistics
 */

import { dashboardState } from './deanship-state.js';
import { apiRequest } from '../core/api.js';
import { showToast } from '../core/ui.js';
import { withMinLoadingTime, withMinLoadingTimeAll } from './deanship-utils.js';

// Chart.js configuration and theme
const CHART_THEME = {
    colors: {
        primary: '#3B82F6',
        success: '#10B981',
        warning: '#F59E0B',
        danger: '#EF4444',
        purple: '#8B5CF6',
        gray: '#6B7280'
    },
    fonts: {
        family: "'Inter', 'Cairo', sans-serif",
        size: 12
    }
};

const BASE_CHART_OPTIONS = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
        legend: {
            display: true,
            position: 'bottom',
            labels: {
                font: {
                    family: CHART_THEME.fonts.family,
                    size: CHART_THEME.fonts.size
                },
                padding: 15,
                usePointStyle: true
            }
        },
        tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 12,
            titleFont: {
                family: CHART_THEME.fonts.family,
                size: 13,
                weight: 'bold'
            },
            bodyFont: {
                family: CHART_THEME.fonts.family,
                size: 12
            },
            cornerRadius: 8
        }
    }
};

/**
 * Load Chart.js library dynamically (Task 12.1: Lazy Loading)
 * Only loads when analytics tab is activated
 */
async function loadChartJS() {
    if (window.Chart) {
        return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = '/js/lib/chart.umd.min.js';
        script.onload = () => resolve();
        script.onerror = () => {
            console.error('Failed to load Chart.js library');
            reject(new Error('Failed to load Chart.js'));
        };
        document.head.appendChild(script);
    });
}

/**
 * Analytics module for dashboard
 */
export class DashboardAnalytics {
    constructor() {
        this.charts = {};
        this.refreshInterval = null;
        this.chartJSLoaded = false;
        this.dataCache = new Map();
        this.CACHE_TTL = 5 * 60 * 1000; // 5 minutes (Task 12.2: Data Caching)
        this.initialized = false; // Track initialization state
    }
    
    /**
     * Initialize analytics components (Task 12.1: Lazy Loading)
     * Only initializes when dashboard tab is activated
     * Uses minimum loading time to prevent flickering shimmer effect
     */
    async initialize() {
        try {
            // Clear cache to always fetch fresh data
            this.clearCache();
            
            // Lazy load Chart.js library only when needed
            if (!this.chartJSLoaded) {
                await loadChartJS();
                this.chartJSLoaded = true;
            }
            
            console.log('Initializing dashboard analytics...');
            
            // Initialize all analytics components with minimum loading time
            // This prevents the shimmer/skeleton effect from appearing as a flash
            await withMinLoadingTimeAll([
                () => this.loadDashboardStats(),
                () => this.initializeCharts()
            ]);
            
            // Set up auto-refresh for activity feed (only once)
            if (!this.initialized) {
                this.startAutoRefresh();
            }
            
            this.initialized = true;
            console.log('Dashboard analytics initialized successfully');
        } catch (error) {
            console.error('Error initializing analytics:', error);
            showToast('Failed to initialize analytics', 'error');
        }
    }

    /**
     * Refresh all charts and statistics
     */
    async refreshAllCharts() {
        if (!this.initialized) {
            return;
        }
        
        try {
            // Clear cache to ensure fresh data
            this.clearCache();
            
            // Reload stats and charts with minimum loading time
            await withMinLoadingTimeAll([
                () => this.loadDashboardStats(),
                () => this.initializeCharts()
            ]);
            
        } catch (error) {
            console.error('Error refreshing analytics:', error);
            showToast('Failed to refresh analytics', 'error');
        }
    }
    
    /**
     * Initialize all charts
     */
    async initializeCharts() {
        // Initialize charts in parallel
        await Promise.all([
            this.initializeSubmissionTrendsChart(),
            this.initializeDepartmentComplianceChart(),
            this.initializeStatusDistributionChart(),
            this.initializeRecentActivityFeed(),
            this.initializeQuickActions()
        ]);
    }
    
    /**
     * Initialize submission trends chart
     */
    async initializeSubmissionTrendsChart() {
        const container = document.getElementById('submissionTrendsChart');
        if (!container) {
            console.warn('Submission trends chart container not found');
            return;
        }
        
        try {
            const data = await this.fetchSubmissionTrends();
            this.renderSubmissionTrendsChart(container, data);
        } catch (error) {
            console.error('Error initializing submission trends chart:', error);
            container.innerHTML = '<p class="text-red-500 text-sm">Failed to load chart</p>';
        }
    }
    
    /**
     * Initialize department compliance chart
     */
    async initializeDepartmentComplianceChart() {
        const container = document.getElementById('departmentComplianceChart');
        if (!container) {
            console.warn('Department compliance chart container not found');
            return;
        }
        
        try {
            const data = await this.fetchDepartmentCompliance();
            this.renderDepartmentComplianceChart(container, data);
        } catch (error) {
            console.error('Error initializing department compliance chart:', error);
            container.innerHTML = '<p class="text-red-500 text-sm">Failed to load chart</p>';
        }
    }
    
    /**
     * Initialize status distribution chart
     */
    async initializeStatusDistributionChart() {
        const container = document.getElementById('statusDistributionChart');
        if (!container) {
            console.warn('Status distribution chart container not found');
            return;
        }
        
        try {
            const data = await this.fetchStatusDistribution();
            this.renderStatusDistributionChart(container, data);
        } catch (error) {
            console.error('Error initializing status distribution chart:', error);
            container.innerHTML = '<p class="text-red-500 text-sm">Failed to load chart</p>';
        }
    }
    
    /**
     * Fetch submission trends data from API
     */
    async fetchSubmissionTrends(days = 30) {
        const cacheKey = `submission-trends-${days}`;
        const cached = this.getCachedData(cacheKey);
        if (cached) return cached;
        
        try {
            // Calculate date range
            const endDate = new Date();
            const startDate = new Date();
            startDate.setDate(endDate.getDate() - days);
            
            const startDateStr = startDate.toISOString().split('T')[0];
            const endDateStr = endDate.toISOString().split('T')[0];
            
            console.log('Fetching submission trends:', { startDateStr, endDateStr });
            
            const response = await apiRequest(
                `/deanship/dashboard/charts/submissions?startDate=${startDateStr}&endDate=${endDateStr}&groupBy=DAY`, 
                { method: 'GET' }
            );
            
            console.log('Submission trends response:', response);
            
            // API helper already unwraps the data, so response IS the array
            if (response && Array.isArray(response) && response.length > 0) {
                // Transform API response to chart format
                const data = {
                    labels: response.map(point => point.label),
                    values: response.map(point => point.value)
                };
                console.log('Transformed submission trends data:', data);
                this.setCachedData(cacheKey, data);
                return data;
            }
            
            console.warn('No submission trends data from API, using mock data');
            // Fallback to mock data
            return this.generateMockSubmissionTrends(days);
        } catch (error) {
            console.error('Error fetching submission trends:', error);
            // Return mock data as fallback
            return this.generateMockSubmissionTrends(days);
        }
    }
    
    /**
     * Fetch department compliance data from API
     */
    async fetchDepartmentCompliance() {
        const semesterId = dashboardState.getSelectedSemesterId();
        
        const cacheKey = `department-compliance-${semesterId || 'all'}`;
        const cached = this.getCachedData(cacheKey);
        if (cached) return cached;
        
        try {
            const url = semesterId 
                ? `/deanship/dashboard/charts/departments?semesterId=${semesterId}`
                : '/deanship/dashboard/charts/departments';
            
            console.log('Fetching department compliance:', { url, semesterId });
                
            const response = await apiRequest(url, { method: 'GET' });
            
            console.log('Department compliance response:', response);
            
            // API helper already unwraps the data, so response IS the array
            if (response && Array.isArray(response) && response.length > 0) {
                // Transform API response to chart format
                const data = {
                    labels: response.map(dept => dept.departmentShortcut || dept.departmentName),
                    values: response.map(dept => {
                        // For pie chart, use professor count or submission count
                        return dept.professorCount || dept.submissionCount || 1;
                    }),
                    departmentData: response // Keep full data for tooltips
                };
                console.log('Transformed department data:', data);
                this.setCachedData(cacheKey, data);
                return data;
            }
            
            console.warn('No department data from API, using mock data');
            // Fallback to mock data if no departments
            return this.generateMockDepartmentCompliance();
        } catch (error) {
            console.error('Error fetching department compliance:', error);
            return this.generateMockDepartmentCompliance();
        }
    }
    
    /**
     * Fetch status distribution data from API
     */
    async fetchStatusDistribution() {
        const semesterId = dashboardState.getSelectedSemesterId();
        
        const cacheKey = `status-distribution-${semesterId || 'all'}`;
        const cached = this.getCachedData(cacheKey);
        if (cached) return cached;
        
        try {
            const url = semesterId 
                ? `/deanship/dashboard/charts/status-distribution?semesterId=${semesterId}`
                : '/deanship/dashboard/charts/status-distribution';
                
            const response = await apiRequest(url, { method: 'GET' });
            
            console.log('Status distribution response:', response);
            
            // API helper already unwraps the data, so response IS the object
            if (response && typeof response === 'object') {
                const data = {
                    pending: response.pending || 0,
                    uploaded: response.uploaded || 0,
                    overdue: response.overdue || 0,
                    total: response.total || 0
                };
                this.setCachedData(cacheKey, data);
                return data;
            }
            
            // Fallback to mock data
            return this.generateMockStatusDistribution();
        } catch (error) {
            console.error('Error fetching status distribution:', error);
            return this.generateMockStatusDistribution();
        }
    }
    
    /**
     * Render submission trends chart
     */
    renderSubmissionTrendsChart(container, data) {
        const canvas = document.createElement('canvas');
        container.innerHTML = '';
        container.appendChild(canvas);
        
        const ctx = canvas.getContext('2d');
        
        if (this.charts.submissionTrends) {
            this.charts.submissionTrends.destroy();
        }
        
        this.charts.submissionTrends = new Chart(ctx, {
            type: 'line',
            data: {
                labels: data.labels,
                datasets: [{
                    label: 'Submissions',
                    data: data.values,
                    borderColor: CHART_THEME.colors.primary,
                    backgroundColor: `${CHART_THEME.colors.primary}20`,
                    borderWidth: 2,
                    fill: true,
                    tension: 0.4,
                    pointRadius: 3,
                    pointHoverRadius: 5
                }]
            },
            options: {
                ...BASE_CHART_OPTIONS,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            font: {
                                family: CHART_THEME.fonts.family,
                                size: CHART_THEME.fonts.size
                            }
                        },
                        grid: {
                            color: 'rgba(0, 0, 0, 0.05)'
                        }
                    },
                    x: {
                        ticks: {
                            font: {
                                family: CHART_THEME.fonts.family,
                                size: CHART_THEME.fonts.size
                            }
                        },
                        grid: {
                            display: false
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Render department compliance chart
     */
    renderDepartmentComplianceChart(container, data) {
        const canvas = document.createElement('canvas');
        container.innerHTML = '';
        container.appendChild(canvas);
        
        const ctx = canvas.getContext('2d');
        
        if (this.charts.departmentCompliance) {
            this.charts.departmentCompliance.destroy();
        }
        
        const colors = [
            CHART_THEME.colors.primary,
            CHART_THEME.colors.success,
            CHART_THEME.colors.warning,
            CHART_THEME.colors.purple,
            CHART_THEME.colors.danger
        ];
        
        this.charts.departmentCompliance = new Chart(ctx, {
            type: 'pie',
            data: {
                labels: data.labels,
                datasets: [{
                    data: data.values,
                    backgroundColor: colors.slice(0, data.labels.length),
                    borderWidth: 2,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                ...BASE_CHART_OPTIONS,
                plugins: {
                    ...BASE_CHART_OPTIONS.plugins,
                    tooltip: {
                        ...BASE_CHART_OPTIONS.plugins.tooltip,
                        callbacks: {
                            label: function(context) {
                                const label = context.label || '';
                                const value = context.parsed || 0;
                                return `${label}: ${value}%`;
                            }
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Render status distribution chart
     */
    renderStatusDistributionChart(container, data) {
        const canvas = document.createElement('canvas');
        container.innerHTML = '';
        container.appendChild(canvas);
        
        const ctx = canvas.getContext('2d');
        
        if (this.charts.statusDistribution) {
            this.charts.statusDistribution.destroy();
        }
        
        this.charts.statusDistribution = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Pending', 'Uploaded', 'Overdue'],
                datasets: [{
                    label: 'Count',
                    data: [data.pending, data.uploaded, data.overdue],
                    backgroundColor: [
                        CHART_THEME.colors.warning,
                        CHART_THEME.colors.success,
                        CHART_THEME.colors.danger
                    ],
                    borderWidth: 0,
                    borderRadius: 6
                }]
            },
            options: {
                ...BASE_CHART_OPTIONS,
                plugins: {
                    ...BASE_CHART_OPTIONS.plugins,
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            font: {
                                family: CHART_THEME.fonts.family,
                                size: CHART_THEME.fonts.size
                            },
                            stepSize: 1
                        },
                        grid: {
                            color: 'rgba(0, 0, 0, 0.05)'
                        }
                    },
                    x: {
                        ticks: {
                            font: {
                                family: CHART_THEME.fonts.family,
                                size: CHART_THEME.fonts.size
                            }
                        },
                        grid: {
                            display: false
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Load dashboard statistics from API
     */
    async loadDashboardStats() {
        try {
            // Fetch real statistics from API
            const semesterId = dashboardState.getSelectedSemesterId();
            const academicYearId = dashboardState.getSelectedAcademicYearId?.() || null;
            
            let url = '/deanship/dashboard/statistics';
            const params = [];
            if (academicYearId) params.push(`academicYearId=${academicYearId}`);
            if (semesterId) params.push(`semesterId=${semesterId}`);
            if (params.length > 0) url += '?' + params.join('&');
            
            const response = await apiRequest(url, { method: 'GET' });
            
            console.log('Dashboard stats response:', response);
            
            // API helper already unwraps the data, so response IS the stats object
            if (response && typeof response === 'object') {
                this.updateStatsCards({
                    totalProfessors: response.totalProfessors || 0,
                    activeCourses: response.totalCourses || 0,
                    pendingSubmissions: response.pendingSubmissions || 0,
                    totalSubmissions: response.totalSubmissions || 0,
                    recentSubmissions: response.recentSubmissions || 0,
                    totalDepartments: response.totalDepartments || 0
                });
            } else {
                // Fallback to cached state data
                const professors = dashboardState.getProfessors();
                const courses = dashboardState.getCourses();
                
                this.updateStatsCards({
                    totalProfessors: professors.length,
                    activeCourses: courses.length,
                    pendingSubmissions: 0
                });
            }
        } catch (error) {
            console.error('Error loading dashboard stats:', error);
            // Fallback to cached state data
            const professors = dashboardState.getProfessors();
            const courses = dashboardState.getCourses();
            
            this.updateStatsCards({
                totalProfessors: professors.length,
                activeCourses: courses.length,
                pendingSubmissions: 0
            });
        }
    }
    
    /**
     * Update statistics cards with real data
     */
    updateStatsCards(stats) {
        // Find dashboard stat cards
        const dashboardTab = document.getElementById('dashboard-tab');
        if (!dashboardTab) return;
        
        // Update specific stat cards by their position or data attribute
        const statCards = dashboardTab.querySelectorAll('.card span.text-2xl, .card h3.text-2xl');
        
        if (statCards.length >= 3) {
            // First card: Total Professors
            statCards[0].textContent = stats.totalProfessors || 0;
            
            // Second card: Active Courses
            statCards[1].textContent = stats.activeCourses || 0;
            
            // Third card: Pending Submissions (use real data)
            statCards[2].textContent = stats.pendingSubmissions || 0;
        }
        
        // Also update any stat cards with data attributes
        const professorCard = dashboardTab.querySelector('[data-stat="professors"]');
        if (professorCard) {
            const valueEl = professorCard.querySelector('.text-2xl, h3');
            if (valueEl) valueEl.textContent = stats.totalProfessors || 0;
        }
        
        const coursesCard = dashboardTab.querySelector('[data-stat="courses"]');
        if (coursesCard) {
            const valueEl = coursesCard.querySelector('.text-2xl, h3');
            if (valueEl) valueEl.textContent = stats.activeCourses || 0;
        }
        
        const pendingCard = dashboardTab.querySelector('[data-stat="pending"]');
        if (pendingCard) {
            const valueEl = pendingCard.querySelector('.text-2xl, h3');
            if (valueEl) valueEl.textContent = stats.pendingSubmissions || 0;
        }
    }
    
    /**
     * Initialize recent activity feed
     */
    async initializeRecentActivityFeed() {
        const container = document.getElementById('recentActivityFeed');
        if (!container) {
            console.warn('Recent activity feed container not found');
            return;
        }
        
        try {
            const activities = await this.fetchRecentActivities();
            this.renderRecentActivityFeed(container, activities);
        } catch (error) {
            console.error('Error initializing recent activity feed:', error);
            container.innerHTML = '<p class="text-red-500 text-sm">Failed to load activities</p>';
        }
    }
    
    /**
     * Initialize quick actions
     */
    initializeQuickActions() {
        const container = document.getElementById('quickActionsCard');
        if (!container) {
            console.warn('Quick actions container not found');
            return;
        }
        
        this.renderQuickActions(container);
    }
    
    /**
     * Fetch recent activities from API
     */
    async fetchRecentActivities(limit = 10) {
        const cacheKey = `recent-activities-${limit}`;
        const cached = this.getCachedData(cacheKey);
        if (cached) return cached;
        
        try {
            const response = await apiRequest(`/deanship/dashboard/activity?limit=${limit}`, { method: 'GET' });
            
            console.log('Recent activities response:', response);
            
            // API helper already unwraps the data, so response IS the array
            if (response && Array.isArray(response) && response.length > 0) {
                // Transform API response - it already matches our format
                const activities = response.map(activity => ({
                    id: activity.entityId,
                    type: activity.type === 'SUBMISSION' ? 'UPLOAD' : activity.type,
                    message: activity.message,
                    timestamp: activity.timestamp,
                    timeAgo: activity.timeAgo
                }));
                this.setCachedData(cacheKey, activities);
                return activities;
            }
            
            // Fallback to mock data
            return this.generateMockActivities(limit);
        } catch (error) {
            console.error('Error fetching recent activities:', error);
            return this.generateMockActivities(limit);
        }
    }
    
    /**
     * Render recent activity feed
     */
    renderRecentActivityFeed(container, activities) {
        if (activities.length === 0) {
            container.innerHTML = `
                <div class="flex flex-col items-center justify-center h-full text-gray-400">
                    <svg class="w-12 h-12 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                            d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <p class="text-sm">No recent activity</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = `
            <div class="space-y-3">
                ${activities.map(activity => `
                    <div class="flex items-start space-x-3 p-3 rounded-lg hover:bg-gray-50 transition-colors">
                        <div class="flex-shrink-0">
                            ${this.getActivityIcon(activity.type)}
                        </div>
                        <div class="flex-1 min-w-0">
                            <p class="text-sm text-gray-900">${activity.message}</p>
                            <p class="text-xs text-gray-500 mt-1">${this.getRelativeTime(activity.timestamp)}</p>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }
    
    /**
     * Render quick actions
     */
    renderQuickActions(container) {
        const actions = [
            {
                id: 'add-professor',
                label: 'Add Professor',
                icon: `<svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                </svg>`,
                onClick: () => {
                    const addProfessorBtn = document.getElementById('addProfessorBtn');
                    if (addProfessorBtn) addProfessorBtn.click();
                }
            },
            {
                id: 'add-course',
                label: 'Add Course',
                icon: `<svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                </svg>`,
                onClick: () => {
                    const addCourseBtn = document.getElementById('addCourseBtn');
                    if (addCourseBtn) addCourseBtn.click();
                }
            },
            {
                id: 'view-reports',
                label: 'View Reports',
                icon: `<svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>`,
                onClick: () => {
                    const reportsTab = document.querySelector('.nav-tab[data-tab="reports"]');
                    if (reportsTab) reportsTab.click();
                }
            }
        ];
        
        container.innerHTML = actions.map(action => `
            <button 
                id="quickAction-${action.id}"
                class="flex items-center space-x-3 p-4 bg-gray-50 hover:bg-gray-100 rounded-lg transition-colors w-full text-left"
            >
                <div class="flex-shrink-0 text-blue-600">
                    ${action.icon}
                </div>
                <span class="text-sm font-medium text-gray-900">${action.label}</span>
            </button>
        `).join('');
        
        // Attach event listeners
        actions.forEach(action => {
            const button = document.getElementById(`quickAction-${action.id}`);
            if (button) {
                button.addEventListener('click', action.onClick);
            }
        });
    }
    
    /**
     * Get activity icon based on type
     */
    getActivityIcon(type) {
        const icons = {
            UPLOAD: `<div class="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center text-blue-600">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                </svg>
            </div>`,
            SUBMISSION: `<div class="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center text-blue-600">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                </svg>
            </div>`,
            CREATE: `<div class="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center text-green-600">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M12 4v16m8-8H4" />
                </svg>
            </div>`,
            UPDATE: `<div class="w-8 h-8 bg-yellow-100 rounded-full flex items-center justify-center text-yellow-600">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
            </div>`,
            DELETE: `<div class="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center text-red-600">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
            </div>`
        };
        
        return icons[type] || icons.UPDATE;
    }
    
    /**
     * Get relative time string
     */
    getRelativeTime(timestamp) {
        const now = new Date();
        const date = new Date(timestamp);
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);
        
        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
        if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
        if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
        
        return date.toLocaleDateString();
    }
    
    /**
     * Start auto-refresh for activity feed
     */
    startAutoRefresh() {
        // Refresh activity feed every 30 seconds
        this.refreshInterval = setInterval(() => {
            this.refreshActivityFeed();
        }, 30000);
    }
    
    /**
     * Refresh activity feed
     */
    async refreshActivityFeed() {
        const container = document.getElementById('recentActivityFeed');
        if (!container) return;
        
        try {
            const activities = await this.fetchRecentActivities();
            this.renderRecentActivityFeed(container, activities);
        } catch (error) {
            console.error('Error refreshing activity feed:', error);
        }
    }
    
    /**
     * Get cached data
     */
    getCachedData(key) {
        const cached = this.dataCache.get(key);
        if (!cached) return null;
        
        const now = Date.now();
        if (now - cached.timestamp > this.CACHE_TTL) {
            this.dataCache.delete(key);
            return null;
        }
        
        return cached.data;
    }
    
    /**
     * Set cached data
     */
    setCachedData(key, data) {
        this.dataCache.set(key, {
            data,
            timestamp: Date.now()
        });
    }
    
    /**
     * Clear cache
     */
    clearCache() {
        this.dataCache.clear();
    }
    
    /**
     * Generate mock submission trends data
     */
    generateMockSubmissionTrends(days) {
        const labels = [];
        const values = [];
        const today = new Date();
        
        for (let i = days - 1; i >= 0; i--) {
            const date = new Date(today);
            date.setDate(date.getDate() - i);
            labels.push(date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));
            values.push(Math.floor(Math.random() * 20) + 5);
        }
        
        return { labels, values };
    }
    
    /**
     * Generate mock department compliance data
     */
    generateMockDepartmentCompliance() {
        return {
            labels: ['Computer Science', 'Mathematics', 'Physics', 'Chemistry', 'Biology'],
            values: [85, 92, 78, 88, 95]
        };
    }
    
    /**
     * Generate mock status distribution data
     */
    generateMockStatusDistribution() {
        return {
            pending: 45,
            uploaded: 120,
            overdue: 12
        };
    }
    
    /**
     * Generate mock activities
     */
    generateMockActivities(limit) {
        const activities = [
            {
                id: 1,
                type: 'UPLOAD',
                message: 'Prof. Ahmed Hassan uploaded Syllabus for CS101',
                timestamp: new Date(Date.now() - 2 * 60000).toISOString()
            },
            {
                id: 2,
                type: 'CREATE',
                message: 'New professor Dr. Sarah Ali added to Computer Science',
                timestamp: new Date(Date.now() - 15 * 60000).toISOString()
            },
            {
                id: 3,
                type: 'UPDATE',
                message: 'Course MATH201 details updated',
                timestamp: new Date(Date.now() - 45 * 60000).toISOString()
            },
            {
                id: 4,
                type: 'UPLOAD',
                message: 'Prof. Mohammed Khalil uploaded Exam Schedule for PHY301',
                timestamp: new Date(Date.now() - 2 * 3600000).toISOString()
            },
            {
                id: 5,
                type: 'CREATE',
                message: 'New course BIO102 created in Biology department',
                timestamp: new Date(Date.now() - 5 * 3600000).toISOString()
            },
            {
                id: 6,
                type: 'UPLOAD',
                message: 'Prof. Layla Ahmad uploaded Course Materials for CHEM201',
                timestamp: new Date(Date.now() - 24 * 3600000).toISOString()
            },
            {
                id: 7,
                type: 'UPDATE',
                message: 'Academic year 2024-2025 activated',
                timestamp: new Date(Date.now() - 2 * 24 * 3600000).toISOString()
            },
            {
                id: 8,
                type: 'CREATE',
                message: 'Course assignment created for CS202',
                timestamp: new Date(Date.now() - 3 * 24 * 3600000).toISOString()
            },
            {
                id: 9,
                type: 'UPLOAD',
                message: 'Prof. Omar Nasser uploaded Lecture Notes for MATH301',
                timestamp: new Date(Date.now() - 4 * 24 * 3600000).toISOString()
            },
            {
                id: 10,
                type: 'UPDATE',
                message: 'Professor status updated for Dr. Fatima Yousef',
                timestamp: new Date(Date.now() - 5 * 24 * 3600000).toISOString()
            }
        ];
        
        return activities.slice(0, limit);
    }
    
    /**
     * Cleanup analytics resources
     */
    destroy() {
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
        }
        
        // Destroy charts
        Object.values(this.charts).forEach(chart => {
            if (chart && typeof chart.destroy === 'function') {
                chart.destroy();
            }
        });
        
        this.charts = {};
        this.clearCache();
    }
}

// Create singleton instance
const dashboardAnalytics = new DashboardAnalytics();

export { dashboardAnalytics };
