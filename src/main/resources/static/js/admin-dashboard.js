/**
 * Admin Dashboard Page Module
 * Main landing page for admin dashboard with overview cards and statistics
 */

import { AdminLayout } from './admin-common.js';
import { apiRequest } from './api.js';
import { showToast } from './ui.js';

/**
 * Admin Dashboard Page Class
 */
class AdminDashboardPage {
    constructor() {
        this.layout = new AdminLayout();
        this.stats = {
            totalProfessors: 0,
            totalHods: 0,
            totalDepartments: 0,
            totalCourses: 0,
            totalSubmissions: 0,
            recentSubmissions: 0,
            pendingSubmissions: 0,
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
            
            // Load dashboard statistics first
            await this.loadDashboardStats();
            
            // Render dashboard cards after stats are loaded
            this.renderDashboardCards();
            
            // Register context change listener to reload stats and re-render cards
            this.layout.onContextChange(async () => {
                await this.loadDashboardStats();
                this.renderDashboardCards();
            });
            
            // Hide loading state
            this.showPageLoading(false);
            
            console.log('[AdminDashboard] Initialized successfully');
        } catch (error) {
            console.error('[AdminDashboard] Initialization error:', error);
            this.showPageLoading(false);
            this.handleError('Failed to initialize dashboard. Please refresh the page.', error);
        }
    }

    /**
     * Load dashboard statistics from API
     */
    async loadDashboardStats() {
        try {
            // Show loading indicators on stats
            this.showStatsLoading(true);
            
            const context = this.layout.getSelectedContext();
            
            // Build query params
            const params = new URLSearchParams();
            if (context.academicYearId) {
                params.append('academicYearId', context.academicYearId);
            }
            if (context.semesterId) {
                params.append('semesterId', context.semesterId);
            }
            
            // Fetch dashboard statistics from admin API
            const queryString = params.toString();
            const url = `/admin/dashboard/statistics${queryString ? '?' + queryString : ''}`;
            
            console.log('[AdminDashboard] Fetching stats from:', url);
            const response = await apiRequest(url, { method: 'GET' });
            console.log('[AdminDashboard] Stats response:', response);
            
            // apiRequest already extracts the data from ApiResponse wrapper
            if (response) {
                this.stats = {
                    totalProfessors: response.totalProfessors || 0,
                    totalHods: response.totalHods || 0,
                    totalDepartments: response.totalDepartments || 0,
                    totalCourses: response.totalCourses || 0,
                    totalSubmissions: response.totalSubmissions || 0,
                    recentSubmissions: response.recentSubmissions || 0,
                    pendingSubmissions: response.pendingSubmissions || 0,
                };
                console.log('[AdminDashboard] Stats updated:', this.stats);
            } else {
                console.warn('[AdminDashboard] No data in response');
            }
            
            // Update stat displays
            this.updateStatDisplays();
            
            // Hide loading indicators
            this.showStatsLoading(false);
            
        } catch (error) {
            console.error('[AdminDashboard] Failed to load stats:', error);
            this.showStatsLoading(false);
            // Don't show toast for stats loading errors - stats will show default values
        }
    }

    /**
     * Update stat card displays
     */
    updateStatDisplays() {
        const statElements = {
            'statProfessors': this.stats.totalProfessors,
            'statHods': this.stats.totalHods,
            'statDepartments': this.stats.totalDepartments,
            'statCourses': this.stats.totalCourses,
            'statSubmissions': this.stats.totalSubmissions,
            'statRecentSubmissions': this.stats.recentSubmissions,
        };
        
        Object.entries(statElements).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value.toLocaleString();
            }
        });
    }

    /**
     * Render dashboard cards
     */
    renderDashboardCards() {
        const cardsContainer = document.getElementById('dashboardCards');
        if (!cardsContainer) return;
        
        const cards = [
            {
                id: 'users',
                icon: '👥',
                title: 'User Management',
                description: 'Create, update, and manage all users including Admins, Deans, HODs, and Professors',
                statLabel: 'Total Users',
                statValue: this.stats.totalProfessors + this.stats.totalHods,
                link: '/admin/users',
            },
            {
                id: 'departments',
                icon: '🏛️',
                title: 'Departments',
                description: 'Manage academic departments and their shortcuts for HOD email validation',
                statLabel: 'Total Departments',
                statValue: this.stats.totalDepartments,
                link: '/admin/departments',
            },
            {
                id: 'courses',
                icon: '📚',
                title: 'Courses',
                description: 'Manage course catalog and course assignments across all departments',
                statLabel: 'Total Courses',
                statValue: this.stats.totalCourses,
                link: '/admin/courses',
            },
            {
                id: 'reports',
                icon: '📊',
                title: 'Reports',
                description: 'Generate system-wide reports and analytics across all departments',
                statLabel: 'Total Submissions',
                statValue: this.stats.totalSubmissions,
                link: '/admin/reports',
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
        // Always show stat value, even if 0
        const statHTML = `<div class="dashboard-card-stat" id="stat-${card.id}">${(card.statValue || 0).toLocaleString()}</div>`;
        
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
     * Show/hide page loading state
     */
    showPageLoading(show) {
        const pageContent = document.getElementById('page-content');
        if (pageContent) {
            pageContent.style.opacity = show ? '0.5' : '1';
            pageContent.style.pointerEvents = show ? 'none' : 'auto';
        }
    }

    /**
     * Show/hide loading state on stats
     */
    showStatsLoading(show) {
        const statCards = document.querySelectorAll('.stat-value');
        statCards.forEach(card => {
            if (show) {
                card.style.opacity = '0.5';
            } else {
                card.style.opacity = '1';
            }
        });
    }

    /**
     * Handle general errors with user feedback
     */
    handleError(message, error) {
        console.error('[AdminDashboard] Error:', error);
        showToast(message, 'error');
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const page = new AdminDashboardPage();
    page.initialize();
});

export default AdminDashboardPage;
