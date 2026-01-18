/**
 * Report Export Service
 * Comprehensive PDF and Excel export functionality for University Archiving System
 * 
 * Features:
 * - Professional academic PDF formatting with jsPDF + autoTable
 * - University branding (logo, header, footer)
 * - Colored status badges and indicators
 * - Multi-page support with pagination
 * - Role-based report templates (HOD, Deanship, Admin)
 * - Filter context preservation
 * - Large dataset support (3000+ rows)
 * - Excel export with SheetJS
 * 
 * @requires jsPDF (loaded from CDN)
 * @requires jsPDF-AutoTable plugin
 * @requires SheetJS/xlsx (optional for Excel)
 */

// ============================================================================
// CONSTANTS & CONFIGURATION
// ============================================================================

const EXPORT_CONFIG = {
    // Brand Colors (matching dashboard theme from common.css)
    colors: {
        primary: [59, 130, 246],        // #3b82f6 - Blue
        primaryDark: [37, 99, 235],     // #2563eb
        purple: [147, 51, 234],         // #9333ea - Admin accent
        success: [34, 197, 94],         // #22c55e - Green (Uploaded)
        warning: [234, 179, 8],         // #eab308 - Yellow
        danger: [239, 68, 68],          // #ef4444 - Red (Overdue)
        gray: [107, 114, 128],          // #6b7280 - Gray (Not Uploaded)
        grayLight: [156, 163, 175],     // #9ca3af
        grayDark: [55, 65, 81],         // #374151
        white: [255, 255, 255],
        black: [17, 24, 39],            // #111827
        tableHeader: [59, 130, 246],    // Blue header
        tableAlt: [249, 250, 251],      // #f9fafb - Alternating rows
    },
    
    // Status Colors for badges
    statusColors: {
        'UPLOADED': { bg: [220, 252, 231], text: [22, 101, 52] },      // Green
        'NOT_UPLOADED': { bg: [243, 244, 246], text: [75, 85, 99] },   // Gray
        'OVERDUE': { bg: [254, 226, 226], text: [153, 27, 27] },       // Red
        'PENDING': { bg: [254, 243, 199], text: [146, 64, 14] },       // Yellow
    },
    
    // Typography
    fonts: {
        title: 16,
        subtitle: 12,
        heading: 11,
        body: 9,
        small: 8,
        tiny: 7,
    },
    
    // Page Layout
    layout: {
        margin: { top: 25, right: 10, bottom: 25, left: 10 },
        headerHeight: 35,
        footerHeight: 15,
    },
    
    // University Info
    university: {
        name: 'Al-Quds University',
        system: 'Academic Archiving System',
        logoPath: '/black-logo.png',
    },
    
    // Performance
    chunkSize: 500, // Process rows in chunks for large datasets
    maxRowsPerPage: 25, // Maximum rows per page for readability
};

// ============================================================================
// REPORT EXPORT SERVICE CLASS
// ============================================================================

class ReportExportService {
    
    constructor() {
        this.isProcessing = false;
        this.progressCallback = null;
        this.logoImageData = null;
    }
    
    // ========================================================================
    // PUBLIC API
    // ========================================================================
    
    /**
     * Export Professor Submission Report to PDF
     * Used by HOD to view department-specific submission status
     * 
     * @param {Object} reportData - ProfessorSubmissionReport JSON from API
     * @param {Object} options - Export options
     * @returns {Promise<string>} Generated filename
     */
    async exportProfessorSubmissionPDF(reportData, options = {}) {
        return this._generatePDF('professor', reportData, {
            title: 'Professor Submission Report',
            subtitle: reportData.departmentName || 'Department Report',
            ...options,
        });
    }
    
    /**
     * Export System-Wide Report to PDF
     * Used by Deanship/Admin for cross-department overview
     * 
     * @param {Object} reportData - SystemWideReport JSON from API
     * @param {Object} options - Export options
     * @returns {Promise<string>} Generated filename
     */
    async exportSystemWidePDF(reportData, options = {}) {
        return this._generatePDF('systemWide', reportData, {
            title: 'System-Wide Submission Report',
            subtitle: reportData.semesterName || 'All Departments',
            ...options,
        });
    }
    
    /**
     * Export Department Report to PDF
     * Detailed department-level submission analysis
     * 
     * @param {Object} reportData - DepartmentSubmissionReport JSON from API
     * @param {Object} options - Export options
     * @returns {Promise<string>} Generated filename
     */
    async exportDepartmentPDF(reportData, options = {}) {
        return this._generatePDF('department', reportData, {
            title: 'Department Submission Report',
            subtitle: reportData.departmentName || 'Department Analysis',
            ...options,
        });
    }
    
    /**
     * Export data to Excel format
     * 
     * @param {Object} reportData - Report data
     * @param {Object} options - Export options
     * @returns {Promise<string>} Generated filename
     */
    async exportToExcel(reportData, options = {}) {
        return this._generateExcel(reportData, options);
    }
    
    /**
     * Set progress callback for long-running exports
     * @param {Function} callback - Progress callback (percent, message)
     */
    setProgressCallback(callback) {
        this.progressCallback = callback;
    }
    
    // ========================================================================
    // PDF GENERATION CORE
    // ========================================================================
    
    /**
     * Core PDF generation method
     * @private
     */
    async _generatePDF(reportType, reportData, options) {
        if (this.isProcessing) {
            throw new Error('Export already in progress');
        }
        
        this.isProcessing = true;
        this._reportProgress(0, 'Initializing PDF export...');
        
        try {
            // Validate jsPDF availability
            if (typeof jspdf === 'undefined' || !jspdf.jsPDF) {
                throw new Error('jsPDF library not loaded. Please include jsPDF and jsPDF-AutoTable.');
            }
            
            const { jsPDF } = jspdf;
            
            // Create PDF document (landscape for wider tables)
            const doc = new jsPDF({
                orientation: options.orientation || 'landscape',
                unit: 'mm',
                format: 'a4',
            });
            
            this._reportProgress(10, 'Loading assets...');
            
            // Load logo if available
            await this._loadLogo();
            
            this._reportProgress(20, 'Building document header...');
            
            // Add document header
            this._addDocumentHeader(doc, options);
            
            // Add metadata section
            this._addMetadataSection(doc, reportData, options);
            
            this._reportProgress(30, 'Generating statistics...');
            
            // Add statistics summary
            this._addStatisticsSummary(doc, reportData, reportType);
            
            this._reportProgress(40, 'Building data tables...');
            
            // Add main content based on report type
            await this._addReportContent(doc, reportData, reportType, options);
            
            this._reportProgress(80, 'Adding legend and footer...');
            
            // Add legend
            this._addLegend(doc);
            
            // Add footer to all pages
            this._addFooterToAllPages(doc, options);
            
            this._reportProgress(90, 'Finalizing document...');
            
            // Generate filename and save
            const filename = this._generateFilename(options.title || 'report', 'pdf');
            doc.save(filename);
            
            this._reportProgress(100, 'Export complete!');
            
            return filename;
            
        } catch (error) {
            console.error('[ReportExportService] PDF generation error:', error);
            throw new Error(`PDF export failed: ${error.message}`);
        } finally {
            this.isProcessing = false;
        }
    }
    
    // ========================================================================
    // PDF HEADER & BRANDING
    // ========================================================================
    
    /**
     * Add document header with university branding
     * @private
     */
    _addDocumentHeader(doc, options) {
        const pageWidth = doc.internal.pageSize.getWidth();
        const { colors, fonts, university } = EXPORT_CONFIG;
        
        // Add logo if available
        if (this.logoImageData) {
            try {
                doc.addImage(this.logoImageData, 'PNG', 10, 8, 20, 20);
            } catch (e) {
                console.warn('Could not add logo to PDF:', e);
            }
        }
        
        // University name
        doc.setFontSize(fonts.title);
        doc.setFont(undefined, 'bold');
        doc.setTextColor(...colors.black);
        doc.text(university.name, pageWidth / 2, 14, { align: 'center' });
        
        // System name
        doc.setFontSize(fonts.subtitle);
        doc.setFont(undefined, 'normal');
        doc.setTextColor(...colors.grayDark);
        doc.text(university.system, pageWidth / 2, 21, { align: 'center' });
        
        // Report title
        doc.setFontSize(14);
        doc.setFont(undefined, 'bold');
        doc.setTextColor(...colors.primary);
        doc.text(options.title || 'Report', pageWidth / 2, 30, { align: 'center' });
        
        // Subtitle (department/semester)
        if (options.subtitle) {
            doc.setFontSize(fonts.heading);
            doc.setFont(undefined, 'normal');
            doc.setTextColor(...colors.gray);
            doc.text(options.subtitle, pageWidth / 2, 36, { align: 'center' });
        }
        
        // Horizontal divider line
        doc.setDrawColor(...colors.primary);
        doc.setLineWidth(0.5);
        doc.line(10, 40, pageWidth - 10, 40);
    }
    
    /**
     * Add metadata section (filters, generation info)
     * @private
     */
    _addMetadataSection(doc, reportData, options) {
        const { fonts, colors } = EXPORT_CONFIG;
        const startY = 45;
        
        doc.setFontSize(fonts.small);
        doc.setFont(undefined, 'normal');
        doc.setTextColor(...colors.grayDark);
        
        const metadata = [];
        
        // Add generation info
        if (reportData.generatedBy) {
            metadata.push(`Generated By: ${reportData.generatedBy}`);
        }
        
        if (reportData.generatedAt) {
            const date = new Date(reportData.generatedAt);
            metadata.push(`Generated At: ${date.toLocaleString()}`);
        }
        
        // Add filter context
        if (options.filters) {
            if (options.filters.semester) metadata.push(`Semester: ${options.filters.semester}`);
            if (options.filters.academicYear) metadata.push(`Academic Year: ${options.filters.academicYear}`);
            if (options.filters.department) metadata.push(`Department: ${options.filters.department}`);
            if (options.filters.status) metadata.push(`Status Filter: ${options.filters.status}`);
            if (options.filters.courseCode) metadata.push(`Course: ${options.filters.courseCode}`);
        }
        
        // Render metadata in two columns
        const pageWidth = doc.internal.pageSize.getWidth();
        const leftCol = metadata.slice(0, Math.ceil(metadata.length / 2));
        const rightCol = metadata.slice(Math.ceil(metadata.length / 2));
        
        leftCol.forEach((text, i) => {
            doc.text(text, 12, startY + (i * 4));
        });
        
        rightCol.forEach((text, i) => {
            doc.text(text, pageWidth / 2, startY + (i * 4));
        });
    }
    
    // ========================================================================
    // STATISTICS SUMMARY
    // ========================================================================
    
    /**
     * Add statistics summary cards
     * @private
     */
    _addStatisticsSummary(doc, reportData, reportType) {
        const { colors, fonts } = EXPORT_CONFIG;
        const pageWidth = doc.internal.pageSize.getWidth();
        
        let stats;
        
        // Extract statistics based on report type
        if (reportType === 'systemWide') {
            stats = reportData.overallStatistics || {};
        } else if (reportType === 'department') {
            stats = {
                totalProfessors: reportData.totalProfessors,
                totalRequiredDocuments: reportData.totalRequests,
                submittedDocuments: reportData.totalSubmitted,
                missingDocuments: reportData.totalPending,
                overdueDocuments: reportData.totalOverdue,
            };
        } else {
            stats = reportData.statistics || {};
        }
        
        // Calculate completion rate
        const total = stats.totalRequiredDocuments || 0;
        const submitted = stats.submittedDocuments || 0;
        const completionRate = total > 0 ? ((submitted / total) * 100).toFixed(1) : 0;
        
        // Statistics to display
        const statsData = [
            { label: 'Total Documents', value: total, color: colors.primary },
            { label: 'Submitted', value: submitted, color: colors.success },
            { label: 'Missing', value: stats.missingDocuments || 0, color: colors.warning },
            { label: 'Overdue', value: stats.overdueDocuments || 0, color: colors.danger },
            { label: 'Completion Rate', value: `${completionRate}%`, color: colors.primary },
        ];
        
        // Draw stat boxes
        const boxWidth = (pageWidth - 30) / statsData.length;
        const boxHeight = 15;
        const startY = 58;
        
        statsData.forEach((stat, index) => {
            const x = 12 + (index * boxWidth);
            
            // Box background
            doc.setFillColor(249, 250, 251);
            doc.roundedRect(x, startY, boxWidth - 3, boxHeight, 2, 2, 'F');
            
            // Value
            doc.setFontSize(fonts.heading);
            doc.setFont(undefined, 'bold');
            doc.setTextColor(...stat.color);
            doc.text(String(stat.value), x + (boxWidth - 3) / 2, startY + 6, { align: 'center' });
            
            // Label
            doc.setFontSize(fonts.tiny);
            doc.setFont(undefined, 'normal');
            doc.setTextColor(...colors.gray);
            doc.text(stat.label, x + (boxWidth - 3) / 2, startY + 12, { align: 'center' });
        });
    }
    
    // ========================================================================
    // REPORT CONTENT GENERATION
    // ========================================================================
    
    /**
     * Add main report content based on type
     * @private
     */
    async _addReportContent(doc, reportData, reportType, options) {
        switch (reportType) {
            case 'professor':
                await this._addProfessorSubmissionTable(doc, reportData, options);
                break;
            case 'systemWide':
                await this._addSystemWideTable(doc, reportData, options);
                break;
            case 'department':
                await this._addDepartmentTable(doc, reportData, options);
                break;
            default:
                throw new Error(`Unknown report type: ${reportType}`);
        }
    }
    
    /**
     * Add Professor Submission Table
     * Shows submission status for each professor/course/document combination
     * @private
     */
    async _addProfessorSubmissionTable(doc, reportData, options) {
        const rows = reportData.rows || [];
        
        if (rows.length === 0) {
            this._addEmptyState(doc, 'No submission data available');
            return;
        }
        
        // Flatten rows: one row per professor/course/docType combination
        const tableRows = [];
        
        for (const row of rows) {
            const docStatuses = row.documentStatuses || {};
            
            for (const [docType, statusInfo] of Object.entries(docStatuses)) {
                tableRows.push([
                    row.professorName || 'N/A',
                    row.courseCode || 'N/A',
                    row.courseName || 'N/A',
                    this._formatDocumentType(docType),
                    { content: this._formatStatus(statusInfo.status), styles: this._getStatusStyles(statusInfo.status) },
                    this._formatDate(statusInfo.deadline),
                    this._formatDate(statusInfo.submittedAt),
                ]);
            }
        }
        
        // Process in chunks for large datasets
        await this._addLargeTable(doc, tableRows, {
            head: [['Professor', 'Course Code', 'Course Name', 'Document Type', 'Status', 'Deadline', 'Submitted']],
            startY: 78,
            headStyles: {
                fillColor: EXPORT_CONFIG.colors.tableHeader,
                textColor: EXPORT_CONFIG.colors.white,
                fontStyle: 'bold',
                fontSize: 8,
                cellPadding: 2,
            },
            columnStyles: {
                0: { cellWidth: 35 },  // Professor
                1: { cellWidth: 22 },  // Course Code
                2: { cellWidth: 45 },  // Course Name
                3: { cellWidth: 28 },  // Document Type
                4: { cellWidth: 25, halign: 'center' }, // Status
                5: { cellWidth: 25 },  // Deadline
                6: { cellWidth: 25 },  // Submitted
            },
            ...options,
        });
    }
    
    /**
     * Add System-Wide Report Table
     * Shows department summaries across the system
     * @private
     */
    async _addSystemWideTable(doc, reportData, options) {
        const departments = reportData.departmentSummaries || [];
        
        if (departments.length === 0) {
            this._addEmptyState(doc, 'No department data available');
            return;
        }
        
        const tableRows = departments.map(dept => {
            const stats = dept.statistics || {};
            const total = stats.totalRequiredDocuments || 0;
            const submitted = stats.submittedDocuments || 0;
            const rate = total > 0 ? ((submitted / total) * 100).toFixed(1) : 0;
            
            return [
                dept.departmentName || 'N/A',
                stats.totalProfessors || 0,
                stats.totalCourses || 0,
                submitted,
                stats.missingDocuments || 0,
                stats.overdueDocuments || 0,
                { content: `${rate}%`, styles: this._getComplianceStyles(parseFloat(rate)) },
            ];
        });
        
        await this._addLargeTable(doc, tableRows, {
            head: [['Department', 'Professors', 'Courses', 'Submitted', 'Missing', 'Overdue', 'Compliance']],
            startY: 78,
            headStyles: {
                fillColor: EXPORT_CONFIG.colors.tableHeader,
                textColor: EXPORT_CONFIG.colors.white,
                fontStyle: 'bold',
                fontSize: 9,
                cellPadding: 3,
            },
            columnStyles: {
                0: { cellWidth: 60 },  // Department
                1: { cellWidth: 25, halign: 'center' },  // Professors
                2: { cellWidth: 25, halign: 'center' },  // Courses
                3: { cellWidth: 25, halign: 'center' },  // Submitted
                4: { cellWidth: 25, halign: 'center' },  // Missing
                5: { cellWidth: 25, halign: 'center' },  // Overdue
                6: { cellWidth: 30, halign: 'center' },  // Compliance
            },
            ...options,
        });
    }
    
    /**
     * Add Department Report Table
     * Shows professor summaries within a department
     * @private
     */
    async _addDepartmentTable(doc, reportData, options) {
        const professors = reportData.professorSummaries || [];
        
        if (professors.length === 0) {
            this._addEmptyState(doc, 'No professor data available');
            return;
        }
        
        const tableRows = professors.map(prof => {
            const total = prof.totalDocuments || 0;
            const submitted = prof.submittedDocuments || 0;
            const rate = total > 0 ? ((submitted / total) * 100).toFixed(1) : 0;
            
            return [
                prof.professorName || 'N/A',
                prof.email || 'N/A',
                prof.assignedCourses || 0,
                submitted,
                prof.pendingDocuments || 0,
                prof.overdueDocuments || 0,
                { content: `${rate}%`, styles: this._getComplianceStyles(parseFloat(rate)) },
            ];
        });
        
        await this._addLargeTable(doc, tableRows, {
            head: [['Professor', 'Email', 'Courses', 'Submitted', 'Pending', 'Overdue', 'Compliance']],
            startY: 78,
            headStyles: {
                fillColor: EXPORT_CONFIG.colors.tableHeader,
                textColor: EXPORT_CONFIG.colors.white,
                fontStyle: 'bold',
                fontSize: 9,
                cellPadding: 3,
            },
            columnStyles: {
                0: { cellWidth: 45 },  // Professor
                1: { cellWidth: 55 },  // Email
                2: { cellWidth: 22, halign: 'center' },  // Courses
                3: { cellWidth: 25, halign: 'center' },  // Submitted
                4: { cellWidth: 25, halign: 'center' },  // Pending
                5: { cellWidth: 25, halign: 'center' },  // Overdue
                6: { cellWidth: 30, halign: 'center' },  // Compliance
            },
            ...options,
        });
    }
    
    // ========================================================================
    // TABLE GENERATION WITH CHUNKING
    // ========================================================================
    
    /**
     * Add large table with chunking for performance
     * @private
     */
    async _addLargeTable(doc, rows, options) {
        const { chunkSize } = EXPORT_CONFIG;
        
        // For smaller datasets, use standard approach
        if (rows.length <= chunkSize) {
            this._addTable(doc, rows, options);
            return;
        }
        
        // Process in chunks for large datasets
        const totalChunks = Math.ceil(rows.length / chunkSize);
        let currentStartY = options.startY;
        
        for (let i = 0; i < totalChunks; i++) {
            const start = i * chunkSize;
            const end = Math.min(start + chunkSize, rows.length);
            const chunkRows = rows.slice(start, end);
            
            // Update progress
            const progress = 40 + Math.round((i / totalChunks) * 35);
            this._reportProgress(progress, `Processing rows ${start + 1}-${end} of ${rows.length}...`);
            
            // Add table chunk
            const tableOptions = {
                ...options,
                startY: currentStartY,
                // Only show header on first chunk or new pages
                showHead: i === 0 ? 'everyPage' : 'everyPage',
            };
            
            this._addTable(doc, chunkRows, tableOptions);
            
            // Get final Y position after table
            currentStartY = doc.lastAutoTable?.finalY || currentStartY + 50;
            
            // Allow UI to update (prevent freezing)
            await new Promise(resolve => setTimeout(resolve, 0));
        }
    }
    
    /**
     * Add table using autoTable
     * @private
     */
    _addTable(doc, rows, options) {
        const { colors, fonts } = EXPORT_CONFIG;
        
        if (typeof doc.autoTable !== 'function') {
            console.error('autoTable plugin not available');
            return;
        }
        
        doc.autoTable({
            head: options.head,
            body: rows,
            startY: options.startY || 80,
            theme: 'grid',
            styles: {
                fontSize: fonts.small,
                cellPadding: 2,
                overflow: 'linebreak',
                lineColor: [229, 231, 235],
                lineWidth: 0.1,
            },
            headStyles: options.headStyles || {
                fillColor: colors.tableHeader,
                textColor: colors.white,
                fontStyle: 'bold',
                fontSize: fonts.small,
            },
            bodyStyles: {
                textColor: colors.grayDark,
            },
            alternateRowStyles: {
                fillColor: colors.tableAlt,
            },
            columnStyles: options.columnStyles || {},
            margin: EXPORT_CONFIG.layout.margin,
            showHead: options.showHead || 'everyPage',
            tableWidth: 'auto',
            didDrawPage: (data) => {
                // Re-add header on new pages
                if (data.pageNumber > 1) {
                    this._addPageHeader(doc, data.pageNumber);
                }
            },
        });
    }
    
    // ========================================================================
    // LEGEND & FOOTER
    // ========================================================================
    
    /**
     * Add status legend
     * @private
     */
    _addLegend(doc) {
        const { colors, fonts, statusColors } = EXPORT_CONFIG;
        const pageWidth = doc.internal.pageSize.getWidth();
        const pageHeight = doc.internal.pageSize.getHeight();
        
        // Position legend above footer
        const legendY = pageHeight - 30;
        
        doc.setFontSize(fonts.small);
        doc.setFont(undefined, 'bold');
        doc.setTextColor(...colors.grayDark);
        doc.text('Status Legend:', 12, legendY);
        
        const statuses = [
            { key: 'UPLOADED', label: 'Uploaded', color: colors.success },
            { key: 'NOT_UPLOADED', label: 'Not Uploaded', color: colors.gray },
            { key: 'OVERDUE', label: 'Overdue', color: colors.danger },
        ];
        
        let xPos = 45;
        
        statuses.forEach((status) => {
            // Color indicator
            doc.setFillColor(...status.color);
            doc.circle(xPos, legendY - 1, 2, 'F');
            
            // Label
            doc.setFontSize(fonts.tiny);
            doc.setFont(undefined, 'normal');
            doc.setTextColor(...colors.grayDark);
            doc.text(status.label, xPos + 4, legendY);
            
            xPos += 35;
        });
    }
    
    /**
     * Add footer to all pages
     * @private
     */
    _addFooterToAllPages(doc, options) {
        const { colors, fonts, university } = EXPORT_CONFIG;
        const pageCount = doc.internal.getNumberOfPages();
        const pageWidth = doc.internal.pageSize.getWidth();
        const pageHeight = doc.internal.pageSize.getHeight();
        
        for (let i = 1; i <= pageCount; i++) {
            doc.setPage(i);
            
            // Footer line
            doc.setDrawColor(...colors.grayLight);
            doc.setLineWidth(0.3);
            doc.line(10, pageHeight - 15, pageWidth - 10, pageHeight - 15);
            
            // Page number
            doc.setFontSize(fonts.tiny);
            doc.setFont(undefined, 'normal');
            doc.setTextColor(...colors.gray);
            doc.text(
                `Page ${i} of ${pageCount}`,
                pageWidth / 2,
                pageHeight - 10,
                { align: 'center' }
            );
            
            // Timestamp
            doc.text(
                `Generated: ${new Date().toLocaleString()}`,
                12,
                pageHeight - 10
            );
            
            // System name
            doc.text(
                university.system,
                pageWidth - 12,
                pageHeight - 10,
                { align: 'right' }
            );
        }
    }
    
    /**
     * Add header for continuation pages
     * @private
     */
    _addPageHeader(doc, pageNumber) {
        const { colors, fonts, university } = EXPORT_CONFIG;
        const pageWidth = doc.internal.pageSize.getWidth();
        
        doc.setFontSize(fonts.small);
        doc.setFont(undefined, 'normal');
        doc.setTextColor(...colors.grayDark);
        doc.text(`${university.name} - ${university.system}`, pageWidth / 2, 10, { align: 'center' });
        
        doc.setDrawColor(...colors.grayLight);
        doc.setLineWidth(0.2);
        doc.line(10, 13, pageWidth - 10, 13);
    }
    
    // ========================================================================
    // EXCEL EXPORT
    // ========================================================================
    
    /**
     * Generate Excel file using SheetJS
     * @private
     */
    async _generateExcel(reportData, options) {
        if (typeof XLSX === 'undefined') {
            throw new Error('SheetJS (XLSX) library not loaded');
        }
        
        this._reportProgress(10, 'Preparing Excel data...');
        
        const workbook = XLSX.utils.book_new();
        
        // Add title sheet with metadata
        const metaData = [
            [EXPORT_CONFIG.university.name],
            [EXPORT_CONFIG.university.system],
            [],
            [options.title || 'Report'],
            [],
            ['Generated By:', reportData.generatedBy || 'System'],
            ['Generated At:', new Date().toLocaleString()],
            [],
        ];
        
        // Add filter info if present
        if (options.filters) {
            Object.entries(options.filters).forEach(([key, value]) => {
                if (value) {
                    metaData.push([this._formatLabel(key) + ':', value]);
                }
            });
            metaData.push([]);
        }
        
        this._reportProgress(30, 'Building Excel sheets...');
        
        // Add data based on report structure
        if (reportData.rows) {
            // Professor submission report
            const headers = ['Professor', 'Course Code', 'Course Name', 'Document Type', 'Status', 'Deadline', 'Submitted'];
            const rows = [];
            
            reportData.rows.forEach(row => {
                Object.entries(row.documentStatuses || {}).forEach(([docType, statusInfo]) => {
                    rows.push([
                        row.professorName,
                        row.courseCode,
                        row.courseName,
                        this._formatDocumentType(docType),
                        this._formatStatus(statusInfo.status),
                        this._formatDate(statusInfo.deadline),
                        this._formatDate(statusInfo.submittedAt),
                    ]);
                });
            });
            
            const ws = XLSX.utils.aoa_to_sheet([...metaData, headers, ...rows]);
            ws['!cols'] = [{ wch: 25 }, { wch: 15 }, { wch: 30 }, { wch: 20 }, { wch: 15 }, { wch: 15 }, { wch: 15 }];
            XLSX.utils.book_append_sheet(workbook, ws, 'Submission Data');
            
        } else if (reportData.departmentSummaries) {
            // System-wide report
            const headers = ['Department', 'Professors', 'Courses', 'Submitted', 'Missing', 'Overdue', 'Completion %'];
            const rows = reportData.departmentSummaries.map(dept => {
                const stats = dept.statistics || {};
                const total = stats.totalRequiredDocuments || 0;
                const submitted = stats.submittedDocuments || 0;
                const rate = total > 0 ? ((submitted / total) * 100).toFixed(1) : 0;
                
                return [
                    dept.departmentName,
                    stats.totalProfessors,
                    stats.totalCourses,
                    submitted,
                    stats.missingDocuments,
                    stats.overdueDocuments,
                    rate + '%',
                ];
            });
            
            const ws = XLSX.utils.aoa_to_sheet([...metaData, headers, ...rows]);
            ws['!cols'] = [{ wch: 30 }, { wch: 12 }, { wch: 12 }, { wch: 12 }, { wch: 12 }, { wch: 12 }, { wch: 15 }];
            XLSX.utils.book_append_sheet(workbook, ws, 'Department Summary');
        }
        
        // Add statistics summary sheet
        this._addStatisticsSheet(workbook, reportData);
        
        this._reportProgress(80, 'Saving Excel file...');
        
        // Generate and save file
        const filename = this._generateFilename(options.title || 'report', 'xlsx');
        XLSX.writeFile(workbook, filename);
        
        this._reportProgress(100, 'Excel export complete!');
        
        return filename;
    }
    
    /**
     * Add statistics summary sheet to workbook
     * @private
     */
    _addStatisticsSheet(workbook, reportData) {
        const stats = reportData.statistics || reportData.overallStatistics || {};
        
        const total = stats.totalRequiredDocuments || 0;
        const submitted = stats.submittedDocuments || 0;
        const rate = total > 0 ? ((submitted / total) * 100).toFixed(1) : 0;
        
        const statsData = [
            ['Statistics Summary'],
            [],
            ['Metric', 'Value'],
            ['Total Required Documents', total],
            ['Submitted Documents', submitted],
            ['Missing Documents', stats.missingDocuments || 0],
            ['Overdue Documents', stats.overdueDocuments || 0],
            ['Total Professors', stats.totalProfessors || 0],
            ['Total Courses', stats.totalCourses || 0],
            ['Completion Rate', rate + '%'],
        ];
        
        const ws = XLSX.utils.aoa_to_sheet(statsData);
        ws['!cols'] = [{ wch: 25 }, { wch: 15 }];
        XLSX.utils.book_append_sheet(workbook, ws, 'Statistics');
    }
    
    // ========================================================================
    // UTILITY METHODS
    // ========================================================================
    
    /**
     * Load university logo
     * @private
     */
    async _loadLogo() {
        if (this.logoImageData) return;
        
        try {
            const response = await fetch(EXPORT_CONFIG.university.logoPath);
            if (response.ok) {
                const blob = await response.blob();
                const reader = new FileReader();
                
                return new Promise((resolve) => {
                    reader.onloadend = () => {
                        this.logoImageData = reader.result;
                        resolve();
                    };
                    reader.onerror = () => resolve();
                    reader.readAsDataURL(blob);
                });
            }
        } catch (e) {
            console.warn('Could not load logo:', e);
        }
    }
    
    /**
     * Format document type for display
     * @private
     */
    _formatDocumentType(type) {
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
     * Format status for display
     * @private
     */
    _formatStatus(status) {
        const statuses = {
            'UPLOADED': 'Uploaded',
            'NOT_UPLOADED': 'Not Uploaded',
            'OVERDUE': 'Overdue',
            'PENDING': 'Pending',
        };
        return statuses[status] || status;
    }
    
    /**
     * Get status-specific cell styles
     * @private
     */
    _getStatusStyles(status) {
        const colors = EXPORT_CONFIG.statusColors[status] || EXPORT_CONFIG.statusColors['NOT_UPLOADED'];
        return {
            fillColor: colors.bg,
            textColor: colors.text,
            fontStyle: 'bold',
            halign: 'center',
        };
    }
    
    /**
     * Get compliance rate cell styles
     * @private
     */
    _getComplianceStyles(rate) {
        let color;
        if (rate >= 80) {
            color = EXPORT_CONFIG.colors.success;
        } else if (rate >= 50) {
            color = EXPORT_CONFIG.colors.warning;
        } else {
            color = EXPORT_CONFIG.colors.danger;
        }
        
        return {
            textColor: color,
            fontStyle: 'bold',
            halign: 'center',
        };
    }
    
    /**
     * Format date for display
     * @private
     */
    _formatDate(dateStr) {
        if (!dateStr) return '-';
        
        try {
            const date = new Date(dateStr);
            return date.toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
            });
        } catch {
            return dateStr;
        }
    }
    
    /**
     * Format label for display
     * @private
     */
    _formatLabel(key) {
        return key
            .replace(/([A-Z])/g, ' $1')
            .replace(/^./, str => str.toUpperCase())
            .trim();
    }
    
    /**
     * Generate timestamped filename
     * @private
     */
    _generateFilename(prefix, extension) {
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
        const sanitizedPrefix = prefix.replace(/[^a-z0-9]/gi, '_').toLowerCase();
        return `${sanitizedPrefix}_${timestamp}.${extension}`;
    }
    
    /**
     * Add empty state message
     * @private
     */
    _addEmptyState(doc, message) {
        const pageWidth = doc.internal.pageSize.getWidth();
        const { colors, fonts } = EXPORT_CONFIG;
        
        doc.setFontSize(fonts.heading);
        doc.setTextColor(...colors.gray);
        doc.text(message, pageWidth / 2, 100, { align: 'center' });
    }
    
    /**
     * Report progress to callback
     * @private
     */
    _reportProgress(percent, message) {
        if (this.progressCallback) {
            this.progressCallback(percent, message);
        }
    }
}

// ============================================================================
// GLOBAL EXPORT SERVICE INSTANCE
// ============================================================================

// Create singleton instance
const reportExportService = new ReportExportService();

// Export for ES modules
export { ReportExportService, reportExportService, EXPORT_CONFIG };

// Make available globally for non-module scripts
window.ReportExportService = ReportExportService;
window.reportExportService = reportExportService;

// ============================================================================
// CONVENIENCE FUNCTIONS
// ============================================================================

/**
 * Quick export function for Professor Submission Report
 */
window.exportProfessorReportPDF = async (reportData, options) => {
    return reportExportService.exportProfessorSubmissionPDF(reportData, options);
};

/**
 * Quick export function for System-Wide Report
 */
window.exportSystemReportPDF = async (reportData, options) => {
    return reportExportService.exportSystemWidePDF(reportData, options);
};

/**
 * Quick export function for Department Report
 */
window.exportDepartmentReportPDF = async (reportData, options) => {
    return reportExportService.exportDepartmentPDF(reportData, options);
};

/**
 * Quick export function for Excel
 */
window.exportReportExcel = async (reportData, options) => {
    return reportExportService.exportToExcel(reportData, options);
};

console.log('[ReportExportService] Loaded successfully');
