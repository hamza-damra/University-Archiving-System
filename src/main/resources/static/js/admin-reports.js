/**
 * Admin Reports Page Module
 * Report generation and analytics for admin dashboard
 */

import { AdminLayout } from './admin-common.js';
import { apiRequest } from './api.js';
import { showToast } from './ui.js';

/**
 * Admin Reports Page Class
 */
class AdminReportsPage {
    constructor() {
        this.layout = new AdminLayout();
        this.departments = [];
        this.statistics = null;
        this.chartData = {
            submissions: [],
            departments: [],
        };
    }

    /**
     * Initialize the page
     */
    async initialize() {
        try {
            // Initialize shared layout
            await this.layout.initialize();
            
            // Load departments for filter
            await this.loadDepartments();
            
            // Load initial statistics
            await this.loadStatistics();
            
            // Set up event listeners
            this.setupEventListeners();
            
            // Register context change listener
            this.layout.onContextChange(() => {
                this.loadStatistics();
                this.loadChartData();
            });
            
            console.log('[AdminReports] Initialized successfully');
        } catch (error) {
            console.error('[AdminReports] Initialization error:', error);
            showToast('Failed to initialize page', 'error');
        }
    }

    /**
     * Load departments for filter
     */
    async loadDepartments() {
        try {
            const response = await apiRequest('/admin/departments', { method: 'GET' });
            this.departments = response || [];
            this.renderDepartmentOptions();
        } catch (error) {
            console.error('[AdminReports] Failed to load departments:', error);
        }
    }

    /**
     * Render department options in filter
     */
    renderDepartmentOptions() {
        const select = document.getElementById('reportDepartment');
        if (!select) return;
        
        const options = this.departments.map(dept => 
            `<option value="${dept.id}">${dept.name}</option>`
        ).join('');
        
        select.innerHTML = '<option value="">All Departments</option>' + options;
    }

    /**
     * Load dashboard statistics
     */
    async loadStatistics() {
        try {
            const context = this.layout.getSelectedContext();
            
            // Build query params
            const params = new URLSearchParams();
            if (context.academicYearId) {
                params.append('academicYearId', context.academicYearId);
            }
            if (context.semesterId) {
                params.append('semesterId', context.semesterId);
            }
            
            const queryString = params.toString();
            const url = `/admin/dashboard/statistics${queryString ? '?' + queryString : ''}`;
            
            const response = await apiRequest(url, { method: 'GET' });
            this.statistics = response || {};
            
            this.updateStatisticsDisplay();
            
            // Load chart data if context is selected
            if (context.semesterId) {
                await this.loadChartData();
            }
            
        } catch (error) {
            console.error('[AdminReports] Failed to load statistics:', error);
        }
    }

    /**
     * Update statistics display
     */
    updateStatisticsDisplay() {
        const stats = this.statistics || {};
        
        const totalSubmissions = stats.totalSubmissions || 0;
        const pendingSubmissions = stats.pendingSubmissions || 0;
        const completedSubmissions = totalSubmissions - pendingSubmissions;
        const completionRate = totalSubmissions > 0 
            ? Math.round((completedSubmissions / totalSubmissions) * 100) 
            : 0;
        
        document.getElementById('statTotalSubmissions').textContent = totalSubmissions.toLocaleString();
        document.getElementById('statPendingSubmissions').textContent = pendingSubmissions.toLocaleString();
        document.getElementById('statCompletedSubmissions').textContent = completedSubmissions.toLocaleString();
        document.getElementById('statCompletionRate').textContent = `${completionRate}%`;
    }

    /**
     * Load chart data
     */
    async loadChartData() {
        const context = this.layout.getSelectedContext();
        
        if (!context.semesterId) {
            this.renderChartPlaceholder('submissionsChart', 'Select an academic year and semester to view chart data');
            this.renderChartPlaceholder('departmentsChart', 'Select an academic year and semester to view chart data');
            return;
        }
        
        try {
            // Load submissions over time
            const semester = context.semester;
            if (semester && semester.startDate && semester.endDate) {
                const submissionsResponse = await apiRequest(
                    `/admin/dashboard/charts/submissions?startDate=${semester.startDate}&endDate=${semester.endDate}&groupBy=WEEK`,
                    { method: 'GET' }
                );
                this.chartData.submissions = submissionsResponse || [];
                this.renderSubmissionsChart();
            }
            
            // Load department distribution
            const departmentsResponse = await apiRequest(
                `/admin/dashboard/charts/departments?semesterId=${context.semesterId}`,
                { method: 'GET' }
            );
            this.chartData.departments = departmentsResponse || [];
            this.renderDepartmentsChart();
            
        } catch (error) {
            console.error('[AdminReports] Failed to load chart data:', error);
        }
    }

    /**
     * Render submissions chart (simple bar representation)
     */
    renderSubmissionsChart() {
        const container = document.getElementById('submissionsChart');
        if (!container) return;
        
        const data = this.chartData.submissions;
        
        if (!data || data.length === 0) {
            this.renderChartPlaceholder('submissionsChart', 'No submission data available for this period');
            return;
        }
        
        // Find max value for scaling
        const maxValue = Math.max(...data.map(d => d.value || 0), 1);
        
        // Create simple bar chart
        const bars = data.map(item => {
            const height = ((item.value || 0) / maxValue) * 100;
            return `
                <div style="display: flex; flex-direction: column; align-items: center; flex: 1; min-width: 40px;">
                    <div style="height: 200px; display: flex; align-items: flex-end; width: 100%;">
                        <div style="background-color: var(--color-primary); width: 80%; height: ${height}%; margin: 0 auto; border-radius: 4px 4px 0 0; min-height: 4px;" title="${item.value || 0} submissions"></div>
                    </div>
                    <div style="font-size: 10px; color: var(--color-text-secondary); margin-top: 4px; text-align: center; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%;">${item.label || ''}</div>
                </div>
            `;
        }).join('');
        
        container.innerHTML = `
            <div style="display: flex; align-items: flex-end; gap: 4px; width: 100%; height: 100%; padding: 16px;">
                ${bars}
            </div>
        `;
    }

    /**
     * Render departments chart (simple horizontal bars)
     */
    renderDepartmentsChart() {
        const container = document.getElementById('departmentsChart');
        if (!container) return;
        
        const data = this.chartData.departments;
        
        if (!data || data.length === 0) {
            this.renderChartPlaceholder('departmentsChart', 'No department data available for this period');
            return;
        }
        
        // Find max value for scaling
        const maxValue = Math.max(...data.map(d => d.submissionCount || 0), 1);
        
        // Create horizontal bar chart
        const bars = data.map(item => {
            const width = ((item.submissionCount || 0) / maxValue) * 100;
            return `
                <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
                    <div style="width: 120px; font-size: 12px; color: var(--color-text-secondary); text-align: right; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${item.departmentName || 'Unknown'}</div>
                    <div style="flex: 1; height: 24px; background-color: var(--color-bg-tertiary); border-radius: 4px; overflow: hidden;">
                        <div style="height: 100%; width: ${width}%; background-color: var(--color-primary); border-radius: 4px; display: flex; align-items: center; justify-content: flex-end; padding-right: 8px; min-width: 30px;">
                            <span style="font-size: 11px; color: white; font-weight: 600;">${item.submissionCount || 0}</span>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
        
        container.innerHTML = `
            <div style="width: 100%; padding: 16px;">
                ${bars}
            </div>
        `;
    }

    /**
     * Render chart placeholder
     */
    renderChartPlaceholder(containerId, message) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        const icon = containerId === 'submissionsChart' ? '📈' : '📊';
        container.innerHTML = `
            <div class="report-placeholder">
                <div class="report-placeholder-icon">${icon}</div>
                <p>${message}</p>
            </div>
        `;
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // Generate report button
        const generateBtn = document.getElementById('btnGenerateReport');
        if (generateBtn) {
            generateBtn.addEventListener('click', () => this.generateReport());
        }
        
        // Clear filters button
        const clearBtn = document.getElementById('btnClearFilters');
        if (clearBtn) {
            clearBtn.addEventListener('click', () => this.clearFilters());
        }
    }

    /**
     * Generate report based on filters
     */
    async generateReport() {
        const context = this.layout.getSelectedContext();
        
        if (!context.semesterId) {
            showToast('Please select an academic year and semester', 'warning');
            return;
        }
        
        const departmentId = document.getElementById('reportDepartment').value;
        const startDate = document.getElementById('reportStartDate').value;
        const endDate = document.getElementById('reportEndDate').value;
        
        try {
            // For now, show a placeholder message
            // In a full implementation, this would call a report generation API
            const resultsContainer = document.getElementById('reportResults');
            
            resultsContainer.innerHTML = `
                <div class="table-wrapper">
                    <div style="padding: var(--spacing-lg); text-align: center; color: var(--color-text-secondary);">
                        <p>Report generation is configured with:</p>
                        <ul style="list-style: none; padding: 0; margin-top: var(--spacing-md);">
                            <li>Semester: ${context.semester?.name || 'Selected'}</li>
                            ${departmentId ? `<li>Department: ${this.departments.find(d => d.id == departmentId)?.name || departmentId}</li>` : '<li>Department: All</li>'}
                            ${startDate ? `<li>Start Date: ${startDate}</li>` : ''}
                            ${endDate ? `<li>End Date: ${endDate}</li>` : ''}
                        </ul>
                        <p style="margin-top: var(--spacing-lg); font-style: italic;">Full report generation will be available in a future update.</p>
                    </div>
                </div>
            `;
            
            showToast('Report filters applied', 'info');
            
        } catch (error) {
            console.error('[AdminReports] Failed to generate report:', error);
            showToast('Failed to generate report', 'error');
        }
    }

    /**
     * Clear all filters
     */
    clearFilters() {
        document.getElementById('reportDepartment').value = '';
        document.getElementById('reportStartDate').value = '';
        document.getElementById('reportEndDate').value = '';
        
        document.getElementById('reportResults').innerHTML = `
            <div class="report-placeholder">
                <div class="report-placeholder-icon">📋</div>
                <p>Configure filters and click "Generate Report" to view results</p>
            </div>
        `;
        
        showToast('Filters cleared', 'info');
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const page = new AdminReportsPage();
    page.initialize();
});

export default AdminReportsPage;
