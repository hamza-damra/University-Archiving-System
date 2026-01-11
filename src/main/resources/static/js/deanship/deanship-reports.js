/**
 * Dean Dashboard - Reports Module
 * Handles interactive reports dashboard and export functionality
 */

// Dynamic API base URL - uses current host to support external access
const REPORTS_API_BASE_URL = `${window.location.origin}/api`;

// Minimum loading time in milliseconds to prevent flickering shimmer effect
const MIN_LOADING_TIME = 800;

/**
 * Get authentication token from localStorage
 */
function getAuthToken() {
    return localStorage.getItem('token');
}

/**
 * Execute an async operation with a minimum loading time
 * Prevents flickering when data loads too quickly
 * @param {Function|Promise} asyncOperation - Async function or promise to execute
 * @param {number} minTime - Minimum time in milliseconds
 * @returns {Promise} The result of the async operation
 */
async function withMinLoadingTime(asyncOperation, minTime = MIN_LOADING_TIME) {
    const startTime = Date.now();
    
    const promise = typeof asyncOperation === 'function' 
        ? asyncOperation() 
        : asyncOperation;
    
    const result = await promise;
    
    const elapsedTime = Date.now() - startTime;
    const remainingTime = minTime - elapsedTime;
    
    if (remainingTime > 0) {
        await new Promise(resolve => setTimeout(resolve, remainingTime));
    }
    
    return result;
}

/**
 * Make an authenticated API request
 * @param {string} endpoint - API endpoint (relative to base URL)
 * @param {object} options - Fetch options
 * @returns {Promise} Response data
 */
async function authenticatedFetch(endpoint, options = {}) {
    const url = `${REPORTS_API_BASE_URL}${endpoint}`;
    
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };
    
    const token = getAuthToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    
    const config = {
        ...options,
        headers,
    };
    
    return fetch(url, config);
}

/**
 * Reports Dashboard Component
 * Provides view toggle and interactive report generation
 */
class ReportsDashboard {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.currentView = 'department'; // 'department' | 'level' | 'semester'
        this.currentData = null;
        this.filters = {};
        this.isLoading = false;
    }

    /**
     * Initialize the reports dashboard
     */
    async init() {
        this.render();
        await this.loadData();
    }

    /**
     * Set the current view type
     * @param {string} viewType - 'department' | 'level' | 'semester'
     */
    async setView(viewType) {
        if (this.currentView === viewType) return;
        
        this.currentView = viewType;
        this.updateViewButtons();
        await this.loadData();
    }

    /**
     * Load data for the current view
     * Uses minimum loading time to prevent flickering shimmer effect
     */
    async loadData() {
        if (this.isLoading) return;
        
        // Check if semester is selected - use getSelectedSemesterId() for direct ID access
        const semesterId = window.dashboardState?.getSelectedSemesterId?.() || document.getElementById('semesterSelect')?.value;
        if (!semesterId) {
            const content = document.getElementById('report-content');
            if (content) {
                content.innerHTML = window.EmptyState?.render({
                    title: 'No Semester Selected',
                    message: 'Please select an academic year and semester to view reports.',
                    illustration: 'empty'
                }) || '<div class="p-8 text-center text-gray-500">Please select an academic year and semester</div>';
            }
            return;
        }

        this.isLoading = true;
        this.showLoadingState();

        try {
            // Use minimum loading time to prevent flickering shimmer effect
            const endpoint = this.getEndpointForView();
            const response = await withMinLoadingTime(async () => {
                const res = await authenticatedFetch(endpoint);
                if (!res.ok) {
                    throw new Error(`Failed to load report data: ${res.statusText}`);
                }
                return res.json();
            });

            this.currentData = response;
            this.renderReport();
        } catch (error) {
            console.error('Error loading report data:', error);
            this.showErrorState(error.message);
        } finally {
            this.isLoading = false;
        }
    }

    /**
     * Get API endpoint based on current view
     */
    getEndpointForView() {
        // Use getSelectedSemesterId() for direct ID access, fallback to DOM select value
        const semesterId = window.dashboardState?.getSelectedSemesterId?.() || document.getElementById('semesterSelect')?.value || '';
        
        if (!semesterId) {
            console.warn('No semester selected for reports');
        }
        
        // Use the actual backend endpoint that exists (without /api prefix as authenticatedFetch adds it)
        switch (this.currentView) {
            case 'department':
            case 'level':
            case 'semester':
            default:
                return `/deanship/reports/system-wide?semesterId=${semesterId}`;
        }
    }

    /**
     * Render the reports dashboard structure
     */
    render() {
        this.container.innerHTML = `
            <div class="reports-dashboard">
                <!-- Header with view toggle and export buttons -->
                <div class="flex justify-between items-center mb-6">
                    <div class="view-toggle flex gap-2">
                        <button class="view-btn px-4 py-2 rounded-lg font-medium transition-colors ${this.currentView === 'department' ? 'bg-blue-600 text-white' : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-200 hover:bg-gray-200 dark:hover:bg-gray-600'}" 
                                data-view="department">
                            By Department
                        </button>
                        <button class="view-btn px-4 py-2 rounded-lg font-medium transition-colors ${this.currentView === 'level' ? 'bg-blue-600 text-white' : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-200 hover:bg-gray-200 dark:hover:bg-gray-600'}" 
                                data-view="level">
                            By Course Level
                        </button>
                        <button class="view-btn px-4 py-2 rounded-lg font-medium transition-colors ${this.currentView === 'semester' ? 'bg-blue-600 text-white' : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-200 hover:bg-gray-200 dark:hover:bg-gray-600'}" 
                                data-view="semester">
                            By Semester
                        </button>
                    </div>
                    
                    <div class="export-buttons flex gap-2">
                        <button id="export-pdf-btn" class="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors flex items-center gap-2"
                                title="Export to PDF">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                            </svg>
                            Export PDF
                        </button>
                        <button id="export-excel-btn" class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors flex items-center gap-2"
                                title="Export to Excel">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                            Export Excel
                        </button>
                    </div>
                </div>

                <!-- Report content area -->
                <div id="report-content" class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                    <!-- Content will be rendered here -->
                </div>
            </div>
        `;

        this.attachEventListeners();
    }

    /**
     * Attach event listeners to buttons
     */
    attachEventListeners() {
        // View toggle buttons
        this.container.querySelectorAll('.view-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const view = e.currentTarget.dataset.view;
                this.setView(view);
            });
        });

        // Export buttons
        document.getElementById('export-pdf-btn')?.addEventListener('click', () => {
            this.exportReport('pdf');
        });

        document.getElementById('export-excel-btn')?.addEventListener('click', () => {
            this.exportReport('excel');
        });
    }

    /**
     * Update view button states
     */
    updateViewButtons() {
        this.container.querySelectorAll('.view-btn').forEach(btn => {
            const view = btn.dataset.view;
            if (view === this.currentView) {
                btn.className = 'view-btn px-4 py-2 rounded-lg font-medium transition-colors bg-blue-600 text-white';
            } else {
                btn.className = 'view-btn px-4 py-2 rounded-lg font-medium transition-colors bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-200 hover:bg-gray-200 dark:hover:bg-gray-600';
            }
        });
    }

    /**
     * Show loading state with professional skeleton
     */
    showLoadingState() {
        const content = document.getElementById('report-content');
        if (!content) return;

        // Generate random bar widths for visual variety
        const barWidths = [85, 72, 95, 60, 78, 88, 65, 92, 70, 55];
        const progressWidths = [75, 88, 62, 95, 70, 82, 58, 90, 68, 85];
        
        content.innerHTML = `
            <div class="reports-skeleton-container">
                <!-- Stats Summary Cards -->
                <div class="skeleton-stats-grid">
                    <div class="skeleton-stat-card">
                        <div class="skeleton-stat-icon"></div>
                        <div class="skeleton-stat-value"></div>
                        <div class="skeleton-stat-label"></div>
                    </div>
                    <div class="skeleton-stat-card">
                        <div class="skeleton-stat-icon"></div>
                        <div class="skeleton-stat-value"></div>
                        <div class="skeleton-stat-label"></div>
                    </div>
                    <div class="skeleton-stat-card">
                        <div class="skeleton-stat-icon"></div>
                        <div class="skeleton-stat-value"></div>
                        <div class="skeleton-stat-label"></div>
                    </div>
                    <div class="skeleton-stat-card">
                        <div class="skeleton-stat-icon"></div>
                        <div class="skeleton-stat-value"></div>
                        <div class="skeleton-stat-label"></div>
                    </div>
                </div>

                <!-- Professional Bar Chart Skeleton -->
                <div class="skeleton-chart-container mb-8">
                    ${barWidths.map(width => `
                        <div class="skeleton-bar-row">
                            <div class="skeleton-bar-label"></div>
                            <div class="skeleton-bar-track">
                                <div class="skeleton-bar-fill" style="width: ${width}%"></div>
                            </div>
                        </div>
                    `).join('')}
                </div>

                <!-- Table Skeleton -->
                <div class="skeleton-table-container">
                    <div class="skeleton-table-header">
                        <div class="skeleton-table-header-cell" style="width: 70%"></div>
                        <div class="skeleton-table-header-cell" style="width: 60%"></div>
                        <div class="skeleton-table-header-cell" style="width: 60%"></div>
                        <div class="skeleton-table-header-cell" style="width: 60%"></div>
                        <div class="skeleton-table-header-cell" style="width: 60%"></div>
                        <div class="skeleton-table-header-cell" style="width: 80%"></div>
                    </div>
                    ${[0,1,2,3,4,5,6,7].map((_, i) => `
                        <div class="skeleton-table-row">
                            <div class="skeleton-table-cell" style="width: ${70 + Math.random() * 20}%"></div>
                            <div class="skeleton-table-cell" style="width: ${50 + Math.random() * 30}%"></div>
                            <div class="skeleton-table-cell" style="width: ${40 + Math.random() * 40}%"></div>
                            <div class="skeleton-table-cell" style="width: ${45 + Math.random() * 35}%"></div>
                            <div class="skeleton-table-cell" style="width: ${50 + Math.random() * 30}%"></div>
                            <div class="skeleton-progress-cell">
                                <div class="skeleton-progress-bar">
                                    <div class="skeleton-progress-fill" style="width: ${progressWidths[i] || 70}%"></div>
                                </div>
                                <div class="skeleton-progress-text"></div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    }

    /**
     * Show error state
     */
    showErrorState(message) {
        const content = document.getElementById('report-content');
        if (!content) return;

        content.innerHTML = `
            <div class="p-8 text-center">
                <div class="text-red-600 dark:text-red-400 mb-4">
                    <svg class="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                </div>
                <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">Failed to Load Report</h3>
                <p class="text-gray-600 dark:text-gray-400 mb-4">${message}</p>
                <button onclick="window.reportsDashboard?.loadData()" class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                    Retry
                </button>
            </div>
        `;
    }

    /**
     * Render the report based on current data
     */
    renderReport() {
        const content = document.getElementById('report-content');
        if (!content || !this.currentData) return;

        switch (this.currentView) {
            case 'department':
                this.renderDepartmentReport(content);
                break;
            case 'level':
                this.renderLevelReport(content);
                break;
            case 'semester':
                this.renderSemesterReport(content);
                break;
        }
    }

    /**
     * Render department-based report
     */
    renderDepartmentReport(container) {
        const data = Array.isArray(this.currentData) ? this.currentData : [];
        
        if (data.length === 0) {
            container.innerHTML = window.EmptyState?.render({
                title: 'No Department Data',
                message: 'No department report data available for the selected semester.',
                illustration: 'no-data'
            }) || '<div class="p-8 text-center text-gray-500">No data available</div>';
            return;
        }

        const tableRows = data.map(dept => `
            <tr class="hover:bg-gray-50 dark:hover:bg-gray-700">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-gray-100">${dept.departmentName || 'N/A'}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${dept.totalCourses || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${dept.totalProfessors || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${dept.uploadedDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${dept.pendingDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${dept.overdueDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${this.renderProgressBar(dept.compliancePercentage || 0)}
                </td>
            </tr>
        `).join('');

        container.innerHTML = `
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                    <thead class="bg-gray-50 dark:bg-gray-800">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Department</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Courses</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Professors</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Uploaded</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Pending</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Overdue</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Compliance</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
                        ${tableRows}
                    </tbody>
                </table>
            </div>
        `;
    }

    /**
     * Render level-based report
     */
    renderLevelReport(container) {
        const data = Array.isArray(this.currentData) ? this.currentData : [];
        
        if (data.length === 0) {
            container.innerHTML = window.EmptyState?.render({
                title: 'No Level Data',
                message: 'No course level report data available for the selected semester.',
                illustration: 'no-data'
            }) || '<div class="p-8 text-center text-gray-500">No data available</div>';
            return;
        }

        const tableRows = data.map(level => `
            <tr class="hover:bg-gray-50 dark:hover:bg-gray-700">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-gray-100">${level.courseLevel || 'N/A'}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${level.totalCourses || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${level.uploadedDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${level.pendingDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${level.overdueDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${this.renderProgressBar(level.compliancePercentage || 0)}
                </td>
            </tr>
        `).join('');

        container.innerHTML = `
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                    <thead class="bg-gray-50 dark:bg-gray-800">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Course Level</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Courses</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Uploaded</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Pending</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Overdue</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Compliance</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
                        ${tableRows}
                    </tbody>
                </table>
            </div>
        `;
    }

    /**
     * Render semester-based report
     */
    renderSemesterReport(container) {
        const data = Array.isArray(this.currentData) ? this.currentData : [];
        
        if (data.length === 0) {
            container.innerHTML = window.EmptyState?.render({
                title: 'No Semester Data',
                message: 'No semester report data available.',
                illustration: 'no-data'
            }) || '<div class="p-8 text-center text-gray-500">No data available</div>';
            return;
        }

        const tableRows = data.map(sem => `
            <tr class="hover:bg-gray-50 dark:hover:bg-gray-700">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-gray-100">${sem.semesterName || 'N/A'}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${sem.academicYear || 'N/A'}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${sem.totalCourses || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${sem.totalProfessors || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${sem.uploadedDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${sem.pendingDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-300">${sem.overdueDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${this.renderProgressBar(sem.compliancePercentage || 0)}
                </td>
            </tr>
        `).join('');

        container.innerHTML = `
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                    <thead class="bg-gray-50 dark:bg-gray-800">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Semester</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Academic Year</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Courses</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Professors</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Uploaded</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Pending</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Overdue</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Compliance</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
                        ${tableRows}
                    </tbody>
                </table>
            </div>
        `;
    }

    /**
     * Render progress bar
     */
    renderProgressBar(percentage) {
        const percent = Math.round(percentage);
        let colorClass = 'bg-red-500';
        
        if (percent >= 80) {
            colorClass = 'bg-green-500';
        } else if (percent >= 50) {
            colorClass = 'bg-yellow-500';
        }

        return `
            <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-6 relative">
                <div class="${colorClass} h-6 rounded-full transition-all duration-500" style="width: ${percent}%"></div>
                <span class="absolute inset-0 flex items-center justify-center text-xs font-semibold text-gray-700 dark:text-gray-200">
                    ${percent}%
                </span>
            </div>
        `;
    }

    /**
     * Export report in specified format
     */
    async exportReport(format) {
        if (!this.currentData || this.currentData.length === 0) {
            window.showToast?.('No data to export', 'warning');
            return;
        }

        try {
            const exportService = window.ExportService;
            if (!exportService) {
                throw new Error('Export service not available');
            }

            const metadata = {
                generatedBy: window.DashboardState?.getCurrentUser()?.name || 'Dean',
                generatedAt: new Date().toLocaleString(),
                view: this.currentView,
                semester: window.DashboardState?.getCurrentSemester()?.name || 'All Semesters'
            };

            if (format === 'pdf') {
                await exportService.exportToPDF(this.currentData, {
                    title: `${this.getViewTitle()} Report`,
                    metadata,
                    columns: this.getColumnsForView(),
                    view: this.currentView
                });
            } else if (format === 'excel') {
                await exportService.exportToExcel(this.currentData, {
                    title: `${this.getViewTitle()} Report`,
                    metadata,
                    columns: this.getColumnsForView(),
                    view: this.currentView
                });
            }

            window.showToast?.(`Report exported successfully as ${format.toUpperCase()}`, 'success');
        } catch (error) {
            console.error('Export failed:', error);
            window.showToast?.(`Failed to export report: ${error.message}`, 'error');
        }
    }

    /**
     * Get view title
     */
    getViewTitle() {
        switch (this.currentView) {
            case 'department':
                return 'Department';
            case 'level':
                return 'Course Level';
            case 'semester':
                return 'Semester';
            default:
                return 'Report';
        }
    }

    /**
     * Get columns for current view
     */
    getColumnsForView() {
        switch (this.currentView) {
            case 'department':
                return ['Department', 'Courses', 'Professors', 'Uploaded', 'Pending', 'Overdue', 'Compliance %'];
            case 'level':
                return ['Course Level', 'Courses', 'Uploaded', 'Pending', 'Overdue', 'Compliance %'];
            case 'semester':
                return ['Semester', 'Academic Year', 'Courses', 'Professors', 'Uploaded', 'Pending', 'Overdue', 'Compliance %'];
            default:
                return [];
        }
    }
}

// Make ReportsDashboard available globally
window.ReportsDashboard = ReportsDashboard;
