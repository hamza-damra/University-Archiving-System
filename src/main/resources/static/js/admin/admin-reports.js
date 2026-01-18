/**
 * Admin Reports Module
 * Professional reporting system with interactive charts and data tables
 */

import { apiRequest } from '../core/api.js';
import { showToast, setButtonLoading } from '../core/ui.js';

/**
 * Admin Reports Page Class
 */
class AdminReportsPage {
    constructor() {
        this.currentFilters = {
            semesterId: null,
            departmentId: null,
            startDate: null,
            endDate: null
        };
        this.reportData = null;
        this.charts = {};
        this.departments = [];
        this.academicYears = [];
        this.semesters = [];
    }

    /**
     * Initialize the reports page
     */
    async initialize() {
        try {
            console.log('[AdminReports] Initializing...');
            
            // Load filter options
            await this.loadFilterOptions();
            
            // Set up event listeners
            this.setupEventListeners();
            
            // Initialize Chart.js if available
            if (typeof Chart !== 'undefined') {
                this.initializeCharts();
            } else {
                console.warn('[AdminReports] Chart.js not loaded');
            }
            
            console.log('[AdminReports] Initialized successfully');
        } catch (error) {
            console.error('[AdminReports] Initialization error:', error);
            this.handleError('Failed to initialize reports page', error);
        }
    }

    /**
     * Load filter options (academic years, departments, semesters)
     *
     * NOTE:
     * - apiRequest() already unwraps our standard ApiResponse and returns the `data` field directly.
     * - That means the returned value is usually an array or object, NOT { success, data }.
     */
    async loadFilterOptions() {
        try {
            // Load departments (Admin API is under /api/admin, apiRequest already prefixes /api)
            const deptResponse = await apiRequest('/admin/departments', { method: 'GET' });
            this.departments = Array.isArray(deptResponse) ? deptResponse : [];
            this.populateDepartmentFilter();

            // Load academic years (Deanship API)
            const yearResponse = await apiRequest('/deanship/academic-years', { method: 'GET' });
            this.academicYears = Array.isArray(yearResponse) ? yearResponse : [];
            this.populateAcademicYearFilter();

            // Load additional filter options from report service (currently optional)
            await apiRequest('/admin/reports/filter-options', { method: 'GET' });
        } catch (error) {
            console.error('[AdminReports] Error loading filter options:', error);
            showToast('Failed to load filter options', 'error');
        }
    }

    /**
     * Populate department filter dropdown
     */
    populateDepartmentFilter() {
        const select = document.getElementById('reportDepartmentFilter');
        if (!select) return;

        select.innerHTML = '<option value="">All Departments</option>';
        this.departments.forEach(dept => {
            const option = document.createElement('option');
            option.value = dept.id;
            option.textContent = dept.name;
            select.appendChild(option);
        });
        
        // Refresh modern dropdown to show new options
        if (typeof window.refreshModernDropdown === 'function') {
            window.refreshModernDropdown(select);
        }
    }

    /**
     * Populate academic year filter dropdown
     */
    populateAcademicYearFilter() {
        const select = document.getElementById('reportAcademicYearFilter');
        if (!select) return;

        select.innerHTML = '<option value="">Select Academic Year</option>';
        this.academicYears.forEach(year => {
            const option = document.createElement('option');
            option.value = year.id;
            option.textContent = year.yearCode;
            select.appendChild(option);
        });
        
        // Refresh modern dropdown to show new options
        if (typeof window.refreshModernDropdown === 'function') {
            window.refreshModernDropdown(select);
        }
    }

    /**
     * Handle academic year selection change
     */
    async onAcademicYearChange(yearId) {
        if (!yearId) {
            this.semesters = [];
            this.populateSemesterFilter();
            return;
        }

        try {
            // apiRequest already prefixes /api, so we only pass the controller path
            const response = await apiRequest(`/deanship/academic-years/${yearId}/semesters`, { method: 'GET' });
            this.semesters = Array.isArray(response) ? response : [];
            this.populateSemesterFilter();
        } catch (error) {
            console.error('[AdminReports] Error loading semesters:', error);
            showToast('Failed to load semesters', 'error');
        }
    }

    /**
     * Populate semester filter dropdown
     */
    populateSemesterFilter() {
        const select = document.getElementById('reportSemesterFilter');
        if (!select) return;

        select.innerHTML = '<option value="">Select Semester</option>';
        this.semesters.forEach(semester => {
            const option = document.createElement('option');
            option.value = semester.id;
            // Backend returns Semester entity with `type` field (FIRST, SECOND, SUMMER)
            // Use that as the display text, optionally including the year code if present
            const typeLabel = semester.type || semester.name || 'UNKNOWN';
            option.textContent = typeLabel;
            select.appendChild(option);
        });
        
        // Refresh modern dropdown to show new options
        if (typeof window.refreshModernDropdown === 'function') {
            window.refreshModernDropdown(select);
        }
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // Filter change listeners
        const academicYearFilter = document.getElementById('reportAcademicYearFilter');
        if (academicYearFilter) {
            academicYearFilter.addEventListener('change', (e) => {
                this.onAcademicYearChange(e.target.value);
            });
        }

        const semesterFilter = document.getElementById('reportSemesterFilter');
        if (semesterFilter) {
            semesterFilter.addEventListener('change', (e) => {
                this.currentFilters.semesterId = e.target.value || null;
                if (e.target.value) {
                    this.loadReport();
                }
            });
        }

        const departmentFilter = document.getElementById('reportDepartmentFilter');
        if (departmentFilter) {
            departmentFilter.addEventListener('change', (e) => {
                this.currentFilters.departmentId = e.target.value || null;
                if (this.currentFilters.semesterId) {
                    this.loadReport();
                }
            });
        }

        // Export buttons
        const exportPdfBtn = document.getElementById('exportPdfBtn');
        if (exportPdfBtn) {
            exportPdfBtn.addEventListener('click', () => this.exportToPdf());
        }

        const exportCsvBtn = document.getElementById('exportCsvBtn');
        if (exportCsvBtn) {
            exportCsvBtn.addEventListener('click', () => this.exportToCsv());
        }

        const exportExcelBtn = document.getElementById('exportExcelBtn');
        if (exportExcelBtn) {
            exportExcelBtn.addEventListener('click', () => this.exportToExcel());
        }

        // Refresh button
        const refreshBtn = document.getElementById('refreshReportBtn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => this.loadReport());
        }
    }

    /**
     * Load report data
     */
    async loadReport() {
        if (!this.currentFilters.semesterId) {
            showToast('Please select a semester', 'warning');
            return;
        }

        try {
            this.showLoadingState(true);

            // Build query parameters
            let queryParams = `semesterId=${this.currentFilters.semesterId}`;
            if (this.currentFilters.departmentId) {
                queryParams += `&departmentId=${this.currentFilters.departmentId}`;
            }

            // Load overview report
            // Note: apiRequest already unwraps ApiResponse and returns the data field directly
            const response = await apiRequest(
                `/admin/reports/overview?${queryParams}`,
                { method: 'GET' }
            );

            // apiRequest returns the data directly, not wrapped in { success, data }
            if (response) {
                this.reportData = response;
                this.updateStatistics();
                this.updateCharts();
                this.updateTables();
            } else {
                throw new Error('Failed to load report - no data returned');
            }
        } catch (error) {
            console.error('[AdminReports] Error loading report:', error);
            this.handleError('Failed to load report data', error);
        } finally {
            this.showLoadingState(false);
        }
    }

    /**
     * Load chart data
     */
    async loadChartData() {
        if (!this.currentFilters.semesterId) return;

        try {
            // Load submissions over time
            const endDate = new Date();
            const startDate = new Date();
            startDate.setDate(startDate.getDate() - 30);

            const submissionsResponse = await apiRequest(
                `/admin/dashboard/charts/submissions?startDate=${startDate.toISOString().split('T')[0]}&endDate=${endDate.toISOString().split('T')[0]}&groupBy=DAY`,
                { method: 'GET' }
            );

            // apiRequest returns data directly, not wrapped in { success, data }
            if (submissionsResponse) {
                this.updateSubmissionsChart(submissionsResponse);
            }

            // Load department distribution
            const deptResponse = await apiRequest(
                `/admin/dashboard/charts/departments?semesterId=${this.currentFilters.semesterId}`,
                { method: 'GET' }
            );

            if (deptResponse) {
                this.updateDepartmentChart(deptResponse);
            }

            // Load status distribution
            const statusResponse = await apiRequest(
                `/admin/dashboard/charts/status-distribution?semesterId=${this.currentFilters.semesterId}`,
                { method: 'GET' }
            );

            if (statusResponse) {
                this.updateStatusChart(statusResponse);
            }
        } catch (error) {
            console.error('[AdminReports] Error loading chart data:', error);
        }
    }

    /**
     * Update statistics cards
     */
    updateStatistics() {
        if (!this.reportData || !this.reportData.overallStatistics) return;

        const stats = this.reportData.overallStatistics;
        const totalRequired = stats.totalRequiredDocuments || 0;
        const submitted = stats.submittedDocuments || 0;
        const missing = stats.missingDocuments || 0;
        const overdue = stats.overdueDocuments || 0;
        const completionRate = totalRequired > 0 ? (submitted / totalRequired * 100) : 0;

        // Update KPI cards
        this.updateStatCard('statTotalSubmissions', submitted, 'blue');
        this.updateStatCard('statCompletionRate', `${completionRate.toFixed(1)}%`, 'green');
        this.updateStatCard('statPendingDocuments', missing, 'orange');
        this.updateStatCard('statOverdueDocuments', overdue, 'red');
        this.updateStatCard('statTotalProfessors', stats.totalProfessors || 0, 'purple');
        this.updateStatCard('statTotalCourses', stats.totalCourses || 0, 'teal');
    }

    /**
     * Update a statistics card
     */
    updateStatCard(elementId, value, color) {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = value;
            // Keep existing theme-aware classes from HTML, just update the text
        }
    }

    /**
     * Initialize Chart.js charts
     */
    initializeCharts() {
        // Submissions timeline chart
        const submissionsCtx = document.getElementById('submissionsChart');
        if (submissionsCtx) {
            this.charts.submissions = new Chart(submissionsCtx, {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'Submissions',
                        data: [],
                        borderColor: 'rgb(59, 130, 246)',
                        backgroundColor: 'rgba(59, 130, 246, 0.1)',
                        tension: 0.4,
                        fill: true
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            mode: 'index',
                            intersect: false
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true
                        }
                    }
                }
            });
        }

        // Department distribution chart
        const deptCtx = document.getElementById('departmentChart');
        if (deptCtx) {
            this.charts.department = new Chart(deptCtx, {
                type: 'bar',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'Completion Rate (%)',
                        data: [],
                        backgroundColor: 'rgba(59, 130, 246, 0.8)'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    indexAxis: 'y',
                    plugins: {
                        legend: {
                            display: false
                        }
                    },
                    scales: {
                        x: {
                            beginAtZero: true,
                            max: 100
                        }
                    }
                }
            });
        }

        // Status distribution chart
        const statusCtx = document.getElementById('statusChart');
        if (statusCtx) {
            this.charts.status = new Chart(statusCtx, {
                type: 'doughnut',
                data: {
                    labels: ['Submitted', 'Pending', 'Overdue'],
                    datasets: [{
                        data: [0, 0, 0],
                        backgroundColor: [
                            'rgba(34, 197, 94, 0.8)',
                            'rgba(234, 179, 8, 0.8)',
                            'rgba(239, 68, 68, 0.8)'
                        ]
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom'
                        }
                    }
                }
            });
        }
    }

    /**
     * Update submissions chart
     */
    updateSubmissionsChart(data) {
        if (!this.charts.submissions || !data) return;

        const labels = data.map(point => {
            const date = new Date(point.label);
            return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
        });
        const values = data.map(point => point.value);

        this.charts.submissions.data.labels = labels;
        this.charts.submissions.data.datasets[0].data = values;
        this.charts.submissions.update();
    }

    /**
     * Update department chart
     */
    updateDepartmentChart(data) {
        if (!this.charts.department || !data) return;

        const labels = data.map(item => item.departmentName);
        const values = data.map(item => {
            const total = item.totalSubmissions || 0;
            const submitted = item.submittedSubmissions || 0;
            return total > 0 ? (submitted / total * 100) : 0;
        });

        this.charts.department.data.labels = labels;
        this.charts.department.data.datasets[0].data = values;
        this.charts.department.update();
    }

    /**
     * Update status chart
     */
    updateStatusChart(data) {
        if (!this.charts.status || !data) return;

        this.charts.status.data.datasets[0].data = [
            data.submitted || 0,
            data.pending || 0,
            data.overdue || 0
        ];
        this.charts.status.update();
    }

    /**
     * Update all charts
     */
    async updateCharts() {
        await this.loadChartData();
    }

    /**
     * Update data tables
     */
    updateTables() {
        if (!this.reportData || !this.reportData.departmentSummaries) return;

        const tbody = document.getElementById('departmentTableBody');
        if (!tbody) return;

        tbody.innerHTML = '';

        this.reportData.departmentSummaries.forEach(dept => {
            const stats = dept.statistics;
            const totalRequired = stats.totalRequiredDocuments || 0;
            const submitted = stats.submittedDocuments || 0;
            const completionRate = totalRequired > 0 ? (submitted / totalRequired * 100) : 0;

            const row = document.createElement('tr');
            row.className = 'hover:bg-gray-50 report-table-row';
            row.innerHTML = `
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 report-text-primary">
                    ${dept.departmentName}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 report-text-secondary">
                    ${stats.totalProfessors || 0}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 report-text-secondary">
                    ${stats.totalCourses || 0}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-green-600 report-text-green font-semibold">
                    ${submitted}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-orange-600 report-text-orange font-semibold">
                    ${stats.missingDocuments || 0}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-red-600 report-text-red font-semibold">
                    ${stats.overdueDocuments || 0}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 report-text-secondary">
                    <div class="flex items-center">
                        <div class="w-full bg-gray-200 report-progress-bg rounded-full h-2 mr-2">
                            <div class="bg-blue-600 report-progress-fill h-2 rounded-full" style="width: ${completionRate}%"></div>
                        </div>
                        <span class="font-semibold">${completionRate.toFixed(1)}%</span>
                    </div>
                </td>
            `;
            tbody.appendChild(row);
        });
    }

    /**
     * Export report to PDF
     * Uses client-side PDF generation for professional formatting
     */
    async exportToPdf() {
        if (!this.currentFilters.semesterId) {
            showToast('Please select a semester', 'warning');
            return;
        }

        if (!this.reportData) {
            showToast('Please generate a report first', 'warning');
            return;
        }

        try {
            // Use the new client-side PDF generation if available
            if (window.reportExportService && window.executeExportWithProgress) {
                const exportOptions = {
                    title: 'System-Wide Submission Report',
                    filters: {
                        semester: this.reportData.semesterName,
                        department: this.currentFilters.departmentId 
                            ? this.departments.find(d => d.id == this.currentFilters.departmentId)?.name 
                            : 'All Departments',
                    },
                };
                
                await window.executeExportWithProgress('pdf', 'systemWide', this.reportData, exportOptions);
            } else {
                // Fallback to backend PDF export
                const url = `${window.location.origin}/api/admin/reports/export/pdf?semesterId=${this.currentFilters.semesterId}`;
                window.open(url, '_blank');
                showToast('PDF export started', 'success');
            }
        } catch (error) {
            console.error('[AdminReports] Error exporting to PDF:', error);
            this.handleError('Failed to export PDF', error);
        }
    }

    /**
     * Export report to Excel
     * Uses client-side Excel generation for enhanced formatting
     */
    async exportToExcel() {
        if (!this.currentFilters.semesterId) {
            showToast('Please select a semester', 'warning');
            return;
        }

        if (!this.reportData) {
            showToast('Please generate a report first', 'warning');
            return;
        }

        try {
            if (window.reportExportService && window.executeExportWithProgress) {
                const exportOptions = {
                    title: 'System-Wide Submission Report',
                    filters: {
                        semester: this.reportData.semesterName,
                        department: this.currentFilters.departmentId 
                            ? this.departments.find(d => d.id == this.currentFilters.departmentId)?.name 
                            : 'All Departments',
                    },
                };
                
                await window.executeExportWithProgress('excel', 'systemWide', this.reportData, exportOptions);
            } else {
                // Fallback to CSV export
                await this.exportToCsv();
            }
        } catch (error) {
            console.error('[AdminReports] Error exporting to Excel:', error);
            this.handleError('Failed to export Excel', error);
        }
    }

    /**
     * Export report to CSV
     */
    async exportToCsv() {
        if (!this.currentFilters.semesterId) {
            showToast('Please select a semester', 'warning');
            return;
        }

        try {
            const response = await fetch(`${window.location.origin}/api/admin/reports/export/csv?semesterId=${this.currentFilters.semesterId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
                }
            });

            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `system-report-${this.currentFilters.semesterId}.csv`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
                showToast('CSV export completed', 'success');
            } else {
                throw new Error('Failed to export CSV');
            }
        } catch (error) {
            console.error('[AdminReports] Error exporting to CSV:', error);
            this.handleError('Failed to export CSV', error);
        }
    }

    /**
     * Show/hide loading state
     */
    showLoadingState(loading) {
        const container = document.getElementById('reportsContainer');
        const loadingIndicator = document.getElementById('reportsLoading');
        
        if (loadingIndicator) {
            loadingIndicator.style.display = loading ? 'block' : 'none';
        }
        
        if (container) {
            container.style.opacity = loading ? '0.5' : '1';
            container.style.pointerEvents = loading ? 'none' : 'auto';
        }
    }

    /**
     * Handle errors
     */
    handleError(message, error) {
        console.error(`[AdminReports] ${message}:`, error);
        showToast(message, 'error');
    }
}

// Export for use in admin-dashboard.js
export default AdminReportsPage;
