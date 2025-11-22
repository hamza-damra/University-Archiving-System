/**
 * Dean Dashboard - Reports Module
 * Handles interactive reports dashboard and export functionality
 */

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
     */
    async loadData() {
        if (this.isLoading) return;
        
        // Check if semester is selected
        const semesterId = window.dashboardState?.getSelectedSemester()?.id;
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
            const endpoint = this.getEndpointForView();
            const response = await fetch(endpoint);
            
            if (!response.ok) {
                throw new Error(`Failed to load report data: ${response.statusText}`);
            }

            this.currentData = await response.json();
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
        const semesterId = window.dashboardState?.getSelectedSemester()?.id || '';
        
        if (!semesterId) {
            console.warn('No semester selected for reports');
        }
        
        // Use the actual backend endpoint that exists
        switch (this.currentView) {
            case 'department':
            case 'level':
            case 'semester':
            default:
                return `/api/deanship/reports/system-wide?semesterId=${semesterId}`;
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
                        <button class="view-btn px-4 py-2 rounded-lg font-medium transition-colors ${this.currentView === 'department' ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}" 
                                data-view="department">
                            By Department
                        </button>
                        <button class="view-btn px-4 py-2 rounded-lg font-medium transition-colors ${this.currentView === 'level' ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}" 
                                data-view="level">
                            By Course Level
                        </button>
                        <button class="view-btn px-4 py-2 rounded-lg font-medium transition-colors ${this.currentView === 'semester' ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}" 
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
                <div id="report-content" class="bg-white rounded-lg shadow-sm border border-gray-200">
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
                btn.className = 'view-btn px-4 py-2 rounded-lg font-medium transition-colors bg-gray-100 text-gray-700 hover:bg-gray-200';
            }
        });
    }

    /**
     * Show loading state
     */
    showLoadingState() {
        const content = document.getElementById('report-content');
        if (!content) return;

        content.innerHTML = `
            <div class="p-8">
                ${window.SkeletonLoader?.table(10, 5) || '<div class="text-center text-gray-500">Loading...</div>'}
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
                <div class="text-red-600 mb-4">
                    <svg class="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                </div>
                <h3 class="text-lg font-semibold text-gray-900 mb-2">Failed to Load Report</h3>
                <p class="text-gray-600 mb-4">${message}</p>
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
            <tr class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${dept.departmentName || 'N/A'}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${dept.totalCourses || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${dept.totalProfessors || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${dept.uploadedDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${dept.pendingDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${dept.overdueDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${this.renderProgressBar(dept.compliancePercentage || 0)}
                </td>
            </tr>
        `).join('');

        container.innerHTML = `
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Department</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Courses</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Professors</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Uploaded</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pending</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Overdue</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Compliance</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white divide-y divide-gray-200">
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
            <tr class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${level.courseLevel || 'N/A'}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${level.totalCourses || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${level.uploadedDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${level.pendingDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${level.overdueDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${this.renderProgressBar(level.compliancePercentage || 0)}
                </td>
            </tr>
        `).join('');

        container.innerHTML = `
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Course Level</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Courses</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Uploaded</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pending</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Overdue</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Compliance</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white divide-y divide-gray-200">
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
            <tr class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${sem.semesterName || 'N/A'}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${sem.academicYear || 'N/A'}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${sem.totalCourses || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${sem.totalProfessors || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${sem.uploadedDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${sem.pendingDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">${sem.overdueDocuments || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${this.renderProgressBar(sem.compliancePercentage || 0)}
                </td>
            </tr>
        `).join('');

        container.innerHTML = `
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Semester</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Academic Year</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Courses</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Professors</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Uploaded</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pending</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Overdue</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Compliance</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white divide-y divide-gray-200">
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
            <div class="w-full bg-gray-200 rounded-full h-6 relative">
                <div class="${colorClass} h-6 rounded-full transition-all duration-500" style="width: ${percent}%"></div>
                <span class="absolute inset-0 flex items-center justify-center text-xs font-semibold text-gray-700">
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
