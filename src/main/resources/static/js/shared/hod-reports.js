/**
 * HOD Reports Module
 * Handles report generation, filtering, and PDF export for HOD dashboard
 * 
 * Requirements:
 * - 6.1: Filter options for Course, Professor, Academic Year, Semester, Date Range (NO Department filter)
 * - 6.2: Report generation with paginated table display
 * - 6.3: Display results in paginated table
 * - 6.4: PDF export functionality
 */

import { hod, apiRequest } from '../core/api.js';
import { showToast, formatDate } from '../core/ui.js';

// Minimum loading time in milliseconds to prevent flickering shimmer effect
const MIN_LOADING_TIME = 800;

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
 * HOD Reports Manager Class
 */
export class HodReportsManager {
    constructor() {
        this.reportData = null;
        this.filterOptions = null;
        this.currentPage = 0;
        this.pageSize = 10;
        this.totalPages = 0;
        this.filteredRows = [];
        
        // DOM Elements
        this.elements = {
            // Filters
            filterCourse: document.getElementById('reportFilterCourse'),
            filterProfessor: document.getElementById('reportFilterProfessor'),
            filterDocType: document.getElementById('reportFilterDocType'),
            filterStatus: document.getElementById('reportFilterStatus'),
            filterStartDate: document.getElementById('reportFilterStartDate'),
            filterEndDate: document.getElementById('reportFilterEndDate'),
            
            // Buttons
            btnGenerateReport: document.getElementById('btnGenerateReport'),
            btnExportPdf: document.getElementById('btnExportPdf'),
            btnClearFilters: document.getElementById('btnClearReportFilters'),
            
            // Stats Summary
            statsSummary: document.getElementById('reportStatsSummary'),
            totalSubmissions: document.getElementById('reportTotalSubmissions'),
            completedSubmissions: document.getElementById('reportCompletedSubmissions'),
            pendingSubmissions: document.getElementById('reportPendingSubmissions'),
            completionRate: document.getElementById('reportCompletionRate'),
            
            // Results Table
            resultsSection: document.getElementById('reportResultsSection'),
            resultsTableBody: document.getElementById('reportResultsTableBody'),
            paginationInfo: document.getElementById('reportPaginationInfo'),
            pagination: document.getElementById('reportPagination'),
            pageSize: document.getElementById('reportPageSize'),
            prevPage: document.getElementById('reportPrevPage'),
            nextPage: document.getElementById('reportNextPage'),
            pageIndicator: document.getElementById('reportPageIndicator'),
        };
    }

    /**
     * Initialize the reports manager
     */
    initialize() {
        this.setupEventListeners();
        console.log('[HodReports] Initialized');
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // Generate Report button
        if (this.elements.btnGenerateReport) {
            this.elements.btnGenerateReport.addEventListener('click', () => this.generateReport());
        }

        // Export PDF button
        if (this.elements.btnExportPdf) {
            this.elements.btnExportPdf.addEventListener('click', () => this.exportPdf());
        }

        // Clear Filters button
        if (this.elements.btnClearFilters) {
            this.elements.btnClearFilters.addEventListener('click', () => this.clearFilters());
        }

        // Pagination controls
        if (this.elements.pageSize) {
            this.elements.pageSize.addEventListener('change', (e) => {
                this.pageSize = parseInt(e.target.value);
                this.currentPage = 0;
                this.renderTable();
            });
        }

        if (this.elements.prevPage) {
            this.elements.prevPage.addEventListener('click', () => {
                if (this.currentPage > 0) {
                    this.currentPage--;
                    this.renderTable();
                }
            });
        }

        if (this.elements.nextPage) {
            this.elements.nextPage.addEventListener('click', () => {
                if (this.currentPage < this.totalPages - 1) {
                    this.currentPage++;
                    this.renderTable();
                }
            });
        }
    }

    /**
     * Load filter options from API
     * @param {number} semesterId - Selected semester ID
     */
    async loadFilterOptions(semesterId) {
        if (!semesterId) return;

        try {
            // Load filter options from the API
            this.filterOptions = await hod.getReportFilterOptions();

            this.populateFilterDropdowns();
        } catch (error) {
            console.error('[HodReports] Error loading filter options:', error);
            // Fallback: populate from report data if available
            if (this.reportData) {
                this.populateFiltersFromReport();
            }
        }
    }

    /**
     * Populate filter dropdowns with options
     */
    populateFilterDropdowns() {
        if (!this.filterOptions) return;

        // Populate courses
        if (this.elements.filterCourse && this.filterOptions.courses) {
            const courseOptions = this.filterOptions.courses.map(course =>
                `<option value="${course.courseCode}">${course.courseCode} - ${course.courseName}</option>`
            ).join('');
            this.elements.filterCourse.innerHTML = '<option value="">All Courses</option>' + courseOptions;
            // Refresh modern dropdown if available
            if (this.elements.filterCourse._modernDropdown) {
                this.elements.filterCourse._modernDropdown.refresh();
            }
        }

        // Populate professors
        if (this.elements.filterProfessor && this.filterOptions.professors) {
            const professorOptions = this.filterOptions.professors.map(prof =>
                `<option value="${prof.id}">${prof.name}</option>`
            ).join('');
            this.elements.filterProfessor.innerHTML = '<option value="">All Professors</option>' + professorOptions;
            // Refresh modern dropdown if available
            if (this.elements.filterProfessor._modernDropdown) {
                this.elements.filterProfessor._modernDropdown.refresh();
            }
        }
    }

    /**
     * Populate filters from report data (fallback)
     */
    populateFiltersFromReport() {
        if (!this.reportData || !this.reportData.rows) return;

        // Extract unique courses
        const courses = new Map();
        const professors = new Map();

        this.reportData.rows.forEach(row => {
            if (row.courseCode && row.courseName) {
                courses.set(row.courseCode, row.courseName);
            }
            if (row.professorId && row.professorName) {
                professors.set(row.professorId, row.professorName);
            }
        });

        // Populate course filter
        if (this.elements.filterCourse) {
            const courseOptions = Array.from(courses.entries()).map(([code, name]) =>
                `<option value="${code}">${code} - ${name}</option>`
            ).join('');
            this.elements.filterCourse.innerHTML = '<option value="">All Courses</option>' + courseOptions;
            // Refresh modern dropdown if available
            if (this.elements.filterCourse._modernDropdown) {
                this.elements.filterCourse._modernDropdown.refresh();
            }
        }

        // Populate professor filter
        if (this.elements.filterProfessor) {
            const professorOptions = Array.from(professors.entries()).map(([id, name]) =>
                `<option value="${id}">${name}</option>`
            ).join('');
            this.elements.filterProfessor.innerHTML = '<option value="">All Professors</option>' + professorOptions;
            // Refresh modern dropdown if available
            if (this.elements.filterProfessor._modernDropdown) {
                this.elements.filterProfessor._modernDropdown.refresh();
            }
        }
    }

    /**
     * Generate report based on selected filters
     * @param {number} semesterId - Selected semester ID
     */
    async generateReport(semesterId = null) {
        // Get semester from global state if not provided
        if (!semesterId) {
            semesterId = window.selectedSemester;
        }

        if (!semesterId) {
            showToast('Please select a semester first', 'warning');
            return;
        }

        try {
            // Show loading state
            this.showLoadingState();

            // Build filter parameters
            const filters = this.getFilterValues();

            // Fetch report data with minimum loading time to prevent flickering
            this.reportData = await withMinLoadingTime(() => 
                hod.getSubmissionStatus(semesterId, filters)
            );

            // Load filter options if not already loaded
            if (!this.filterOptions) {
                await this.loadFilterOptions(semesterId);
            } else {
                this.populateFiltersFromReport();
            }

            // Process and display report
            this.processReportData();
            this.updateStatsSummary();
            this.renderTable();

            // Enable PDF export
            if (this.elements.btnExportPdf) {
                this.elements.btnExportPdf.disabled = false;
            }

            showToast('Report generated successfully', 'success');
        } catch (error) {
            console.error('[HodReports] Error generating report:', error);
            showToast('Failed to generate report: ' + (error.message || 'Unknown error'), 'error');
            this.showEmptyState('Error loading report data');
        }
    }

    /**
     * Get current filter values
     * @returns {object} Filter values
     */
    getFilterValues() {
        const filters = {};

        if (this.elements.filterCourse?.value) {
            filters.courseCode = this.elements.filterCourse.value;
        }
        if (this.elements.filterProfessor?.value) {
            filters.professorId = this.elements.filterProfessor.value;
        }
        if (this.elements.filterDocType?.value) {
            filters.documentType = this.elements.filterDocType.value;
        }
        if (this.elements.filterStatus?.value) {
            filters.status = this.elements.filterStatus.value;
        }
        if (this.elements.filterStartDate?.value) {
            filters.startDate = this.elements.filterStartDate.value;
        }
        if (this.elements.filterEndDate?.value) {
            filters.endDate = this.elements.filterEndDate.value;
        }

        return filters;
    }

    /**
     * Process report data into flat rows for table display
     */
    processReportData() {
        if (!this.reportData || !this.reportData.rows) {
            this.filteredRows = [];
            return;
        }

        const rows = [];
        const filters = this.getFilterValues();

        this.reportData.rows.forEach(row => {
            // Create a row for each document type
            Object.entries(row.documentStatuses || {}).forEach(([docType, status]) => {
                // Apply client-side filters for date range
                let includeRow = true;

                if (filters.startDate && status.submittedAt) {
                    const submittedDate = new Date(status.submittedAt);
                    const startDate = new Date(filters.startDate);
                    if (submittedDate < startDate) {
                        includeRow = false;
                    }
                }

                if (filters.endDate && status.submittedAt) {
                    const submittedDate = new Date(status.submittedAt);
                    const endDate = new Date(filters.endDate);
                    endDate.setHours(23, 59, 59, 999);
                    if (submittedDate > endDate) {
                        includeRow = false;
                    }
                }

                if (includeRow) {
                    rows.push({
                        professorId: row.professorId,
                        professorName: row.professorName,
                        courseCode: row.courseCode,
                        courseName: row.courseName,
                        documentType: docType,
                        status: status.status,
                        deadline: status.deadline,
                        submittedAt: status.submittedAt,
                        fileCount: status.fileCount || 0,
                    });
                }
            });
        });

        this.filteredRows = rows;
        this.totalPages = Math.ceil(rows.length / this.pageSize);
        this.currentPage = 0;
    }

    /**
     * Update statistics summary
     */
    updateStatsSummary() {
        if (!this.reportData || !this.reportData.statistics) {
            if (this.elements.statsSummary) {
                this.elements.statsSummary.classList.add('hidden');
            }
            return;
        }

        const stats = this.reportData.statistics;
        const totalExpected = stats.totalExpectedDocuments || this.filteredRows.length;
        const submitted = stats.submittedDocuments || this.filteredRows.filter(r => r.status === 'UPLOADED').length;
        const pending = stats.missingDocuments || this.filteredRows.filter(r => r.status !== 'UPLOADED').length;
        const rate = totalExpected > 0 ? Math.round((submitted / totalExpected) * 100) : 0;

        if (this.elements.totalSubmissions) {
            this.elements.totalSubmissions.textContent = totalExpected;
        }
        if (this.elements.completedSubmissions) {
            this.elements.completedSubmissions.textContent = submitted;
        }
        if (this.elements.pendingSubmissions) {
            this.elements.pendingSubmissions.textContent = pending;
        }
        if (this.elements.completionRate) {
            this.elements.completionRate.textContent = `${rate}%`;
        }

        if (this.elements.statsSummary) {
            this.elements.statsSummary.classList.remove('hidden');
        }
    }

    /**
     * Render the results table
     */
    renderTable() {
        if (!this.elements.resultsTableBody) return;

        if (this.filteredRows.length === 0) {
            this.showEmptyState('No data found for the selected filters');
            return;
        }

        // Calculate pagination
        const startIndex = this.currentPage * this.pageSize;
        const endIndex = Math.min(startIndex + this.pageSize, this.filteredRows.length);
        const pageRows = this.filteredRows.slice(startIndex, endIndex);

        // Render rows
        this.elements.resultsTableBody.innerHTML = pageRows.map(row => `
            <tr class="hover:bg-gray-50">
                <td class="px-4 py-3 text-sm text-gray-900">${this.escapeHtml(row.professorName)}</td>
                <td class="px-4 py-3 text-sm text-gray-600">
                    <div>${this.escapeHtml(row.courseCode)}</div>
                    <div class="text-xs text-gray-500">${this.escapeHtml(row.courseName)}</div>
                </td>
                <td class="px-4 py-3 text-sm text-gray-600">${this.formatDocumentType(row.documentType)}</td>
                <td class="px-4 py-3">${this.getStatusBadge(row.status)}</td>
                <td class="px-4 py-3 text-sm text-gray-600">${row.deadline ? formatDate(row.deadline) : '-'}</td>
                <td class="px-4 py-3 text-sm text-gray-600">${row.submittedAt ? formatDate(row.submittedAt) : '-'}</td>
            </tr>
        `).join('');

        // Update pagination
        this.updatePagination(startIndex, endIndex);
    }

    /**
     * Update pagination controls
     */
    updatePagination(startIndex, endIndex) {
        if (this.elements.paginationInfo) {
            this.elements.paginationInfo.textContent = 
                `Showing ${startIndex + 1}-${endIndex} of ${this.filteredRows.length} results`;
        }

        if (this.elements.pagination) {
            this.elements.pagination.classList.remove('hidden');
        }

        if (this.elements.pageIndicator) {
            this.elements.pageIndicator.textContent = `Page ${this.currentPage + 1} of ${this.totalPages || 1}`;
        }

        if (this.elements.prevPage) {
            this.elements.prevPage.disabled = this.currentPage === 0;
        }

        if (this.elements.nextPage) {
            this.elements.nextPage.disabled = this.currentPage >= this.totalPages - 1;
        }
    }

    /**
     * Show loading state
     */
    showLoadingState() {
        if (this.elements.resultsTableBody) {
            this.elements.resultsTableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="px-4 py-8 text-center text-gray-500">
                        <div class="flex flex-col items-center">
                            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mb-3"></div>
                            <p>Generating report...</p>
                        </div>
                    </td>
                </tr>
            `;
        }

        if (this.elements.btnExportPdf) {
            this.elements.btnExportPdf.disabled = true;
        }
    }

    /**
     * Show empty state
     * @param {string} message - Message to display
     */
    showEmptyState(message) {
        if (this.elements.resultsTableBody) {
            this.elements.resultsTableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="px-4 py-8 text-center text-gray-500">
                        <div class="flex flex-col items-center">
                            <svg class="w-12 h-12 text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z">
                                </path>
                            </svg>
                            <p>${message}</p>
                        </div>
                    </td>
                </tr>
            `;
        }

        if (this.elements.pagination) {
            this.elements.pagination.classList.add('hidden');
        }

        if (this.elements.paginationInfo) {
            this.elements.paginationInfo.textContent = '';
        }
    }

    /**
     * Clear all filters
     */
    clearFilters() {
        if (this.elements.filterCourse) this.elements.filterCourse.value = '';
        if (this.elements.filterProfessor) this.elements.filterProfessor.value = '';
        if (this.elements.filterDocType) this.elements.filterDocType.value = '';
        if (this.elements.filterStatus) this.elements.filterStatus.value = '';
        if (this.elements.filterStartDate) this.elements.filterStartDate.value = '';
        if (this.elements.filterEndDate) this.elements.filterEndDate.value = '';

        showToast('Filters cleared', 'info');
    }

    /**
     * Export report to PDF using client-side generation
     * Uses the new ReportExportService for professional PDF output
     */
    async exportPdf() {
        const semesterId = window.selectedSemester;

        if (!semesterId) {
            showToast('Please select a semester first', 'warning');
            return;
        }

        if (!this.reportData) {
            showToast('Please generate a report first', 'warning');
            return;
        }

        try {
            // Check if ReportExportService is available
            if (window.reportExportService && window.executeExportWithProgress) {
                // Use the new client-side PDF generation
                const exportOptions = {
                    filters: {
                        semester: this.reportData.semesterName,
                        department: this.reportData.departmentName,
                        courseCode: this.getFilterValues().courseCode,
                        status: this.getFilterValues().status,
                    },
                };
                
                await window.executeExportWithProgress('pdf', 'professor', this.reportData, exportOptions);
            } else {
                // Fallback to backend PDF export
                showToast('Generating PDF...', 'info');

                const response = await hod.exportReportToPdf(semesterId);

                if (!response.ok) {
                    throw new Error('Failed to generate PDF');
                }

                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;

                const timestamp = new Date().toISOString().slice(0, 10);
                a.download = `hod-submission-report-${timestamp}.pdf`;

                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);

                showToast('PDF downloaded successfully', 'success');
            }
        } catch (error) {
            console.error('[HodReports] Error exporting PDF:', error);
            showToast('Failed to export PDF: ' + (error.message || 'Unknown error'), 'error');
        }
    }

    /**
     * Export report to Excel using client-side generation
     */
    async exportExcel() {
        const semesterId = window.selectedSemester;

        if (!semesterId) {
            showToast('Please select a semester first', 'warning');
            return;
        }

        if (!this.reportData) {
            showToast('Please generate a report first', 'warning');
            return;
        }

        try {
            if (window.reportExportService && window.executeExportWithProgress) {
                const exportOptions = {
                    title: 'Professor Submission Report',
                    filters: {
                        semester: this.reportData.semesterName,
                        department: this.reportData.departmentName,
                    },
                };
                
                await window.executeExportWithProgress('excel', 'professor', this.reportData, exportOptions);
            } else {
                showToast('Excel export requires additional libraries', 'warning');
            }
        } catch (error) {
            console.error('[HodReports] Error exporting Excel:', error);
            showToast('Failed to export Excel: ' + (error.message || 'Unknown error'), 'error');
        }
    }

    /**
     * Format document type for display
     * @param {string} type - Document type enum value
     * @returns {string} Formatted type name
     */
    formatDocumentType(type) {
        const types = {
            'SYLLABUS': 'Syllabus',
            'EXAM': 'Exam',
            'ASSIGNMENT': 'Assignment',
            'PROJECT_DOCS': 'Project Docs',
            'LECTURE_NOTES': 'Lecture Notes',
            'OTHER': 'Other',
        };
        return types[type] || type;
    }

    /**
     * Get status badge HTML
     * @param {string} status - Status value
     * @returns {string} Badge HTML
     */
    getStatusBadge(status) {
        const badges = {
            'UPLOADED': '<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">Uploaded</span>',
            'NOT_UPLOADED': '<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">Not Uploaded</span>',
            'OVERDUE': '<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">Overdue</span>',
        };
        return badges[status] || `<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">${status}</span>`;
    }

    /**
     * Escape HTML to prevent XSS
     * @param {string} str - String to escape
     * @returns {string} Escaped string
     */
    escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }
}

// Export singleton instance
export const hodReportsManager = new HodReportsManager();

export default HodReportsManager;
