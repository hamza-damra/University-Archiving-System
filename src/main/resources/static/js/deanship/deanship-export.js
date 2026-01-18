/**
 * Dean Dashboard - Export Service
 * Handles PDF and Excel export functionality
 * 
 * This is a compatibility wrapper that uses the new ReportExportService
 * for enhanced PDF generation while maintaining backward compatibility.
 */

/**
 * Export Service for generating PDF and Excel reports
 * @deprecated Use window.reportExportService for new implementations
 */
class ExportService {
    /**
     * Export data to PDF format
     * Uses the new ReportExportService if available for enhanced formatting
     * @param {Array} data - Data to export
     * @param {Object} options - Export options
     */
    static async exportToPDF(data, options = {}) {
        const {
            title = 'Report',
            metadata = {},
            columns = [],
            view = 'department'
        } = options;

        // Try to use the new enhanced export service
        if (window.reportExportService && window.executeExportWithProgress) {
            try {
                // Transform data to system-wide report format
                const reportData = this.transformToReportFormat(data, view, metadata);
                
                const exportOptions = {
                    title: title,
                    filters: {
                        semester: metadata.semester || metadata.semesterName,
                        department: metadata.department,
                        generatedBy: metadata.generatedBy,
                    },
                };
                
                await window.executeExportWithProgress('pdf', 'systemWide', reportData, exportOptions);
                return this.generateFilename(title, 'pdf');
            } catch (error) {
                console.warn('[ExportService] Enhanced export failed, falling back to legacy:', error);
            }
        }

        // Fallback to legacy implementation
        return this._legacyExportToPDF(data, options);
    }

    /**
     * Transform view data to report format for new export service
     */
    static transformToReportFormat(data, view, metadata) {
        if (view === 'department') {
            return {
                semesterName: metadata.semester || 'Current Semester',
                generatedAt: new Date().toISOString(),
                generatedBy: metadata.generatedBy || 'Deanship',
                departmentSummaries: data.map(item => ({
                    departmentName: item.departmentName || 'N/A',
                    statistics: {
                        totalProfessors: item.totalProfessors || 0,
                        totalCourses: item.totalCourses || 0,
                        totalRequiredDocuments: (item.uploadedDocuments || 0) + (item.pendingDocuments || 0) + (item.overdueDocuments || 0),
                        submittedDocuments: item.uploadedDocuments || 0,
                        missingDocuments: item.pendingDocuments || 0,
                        overdueDocuments: item.overdueDocuments || 0,
                    }
                })),
                overallStatistics: this.calculateOverallStats(data),
            };
        }
        
        // For other views, return as-is with wrapper
        return {
            semesterName: metadata.semester || 'Current Semester',
            generatedAt: new Date().toISOString(),
            generatedBy: metadata.generatedBy || 'Deanship',
            rows: data,
        };
    }

    /**
     * Calculate overall statistics from department data
     */
    static calculateOverallStats(data) {
        const totals = data.reduce((acc, item) => ({
            totalProfessors: acc.totalProfessors + (item.totalProfessors || 0),
            totalCourses: acc.totalCourses + (item.totalCourses || 0),
            submittedDocuments: acc.submittedDocuments + (item.uploadedDocuments || 0),
            missingDocuments: acc.missingDocuments + (item.pendingDocuments || 0),
            overdueDocuments: acc.overdueDocuments + (item.overdueDocuments || 0),
        }), { totalProfessors: 0, totalCourses: 0, submittedDocuments: 0, missingDocuments: 0, overdueDocuments: 0 });
        
        totals.totalRequiredDocuments = totals.submittedDocuments + totals.missingDocuments + totals.overdueDocuments;
        return totals;
    }

    /**
     * Legacy PDF export implementation
     * @private
     */
    static async _legacyExportToPDF(data, options = {}) {
        // Check if jsPDF is available
        if (typeof jspdf === 'undefined' || !jspdf.jsPDF) {
            throw new Error('jsPDF library not loaded');
        }

        const {
            title = 'Report',
            metadata = {},
            columns = [],
            view = 'department'
        } = options;

        try {
            const { jsPDF } = jspdf;
            const doc = new jsPDF('l', 'mm', 'a4'); // Landscape orientation

            // Add university branding
            this.addPDFHeader(doc, title);

            // Add metadata
            this.addPDFMetadata(doc, metadata);

            // Prepare table data
            const tableData = this.prepareTableData(data, view);

            // Add table using autoTable
            if (typeof doc.autoTable === 'function') {
                doc.autoTable({
                    head: [columns],
                    body: tableData,
                    startY: 60,
                    theme: 'grid',
                    styles: {
                        fontSize: 9,
                        cellPadding: 3
                    },
                    headStyles: {
                        fillColor: [59, 130, 246], // Blue
                        textColor: 255,
                        fontStyle: 'bold'
                    },
                    alternateRowStyles: {
                        fillColor: [245, 247, 250]
                    },
                    margin: { top: 60, left: 10, right: 10 }
                });
            } else {
                // Fallback if autoTable is not available
                let yPos = 70;
                doc.setFontSize(10);
                tableData.forEach((row, index) => {
                    if (yPos > 180) {
                        doc.addPage();
                        yPos = 20;
                    }
                    doc.text(row.join(' | '), 10, yPos);
                    yPos += 7;
                });
            }

            // Add footer
            this.addPDFFooter(doc);

            // Generate filename and save
            const filename = this.generateFilename(title, 'pdf');
            doc.save(filename);

            return filename;
        } catch (error) {
            console.error('PDF export error:', error);
            throw new Error(`PDF export failed: ${error.message}`);
        }
    }

    /**
     * Export data to Excel format
     * Uses the new ReportExportService if available for enhanced formatting
     * @param {Array} data - Data to export
     * @param {Object} options - Export options
     */
    static async exportToExcel(data, options = {}) {
        const {
            title = 'Report',
            metadata = {},
            columns = [],
            view = 'department'
        } = options;

        // Try to use the new enhanced export service
        if (window.reportExportService && window.executeExportWithProgress) {
            try {
                const reportData = this.transformToReportFormat(data, view, metadata);
                
                const exportOptions = {
                    title: title,
                    filters: {
                        semester: metadata.semester || metadata.semesterName,
                        department: metadata.department,
                    },
                };
                
                await window.executeExportWithProgress('excel', 'systemWide', reportData, exportOptions);
                return this.generateFilename(title, 'xlsx');
            } catch (error) {
                console.warn('[ExportService] Enhanced Excel export failed, falling back to legacy:', error);
            }
        }

        // Fallback to legacy implementation
        return this._legacyExportToExcel(data, options);
    }

    /**
     * Legacy Excel export implementation
     * @private
     */
    static async _legacyExportToExcel(data, options = {}) {
        // Check if XLSX is available
        if (typeof XLSX === 'undefined') {
            throw new Error('XLSX library not loaded');
        }

        const {
            title = 'Report',
            metadata = {},
            columns = [],
            view = 'department'
        } = options;

        try {
            // Prepare worksheet data
            const worksheetData = [];

            // Add title
            worksheetData.push([title]);
            worksheetData.push([]);

            // Add metadata
            Object.entries(metadata).forEach(([key, value]) => {
                worksheetData.push([this.formatMetadataKey(key), value]);
            });
            worksheetData.push([]);

            // Add headers
            worksheetData.push(columns);

            // Add data rows
            const tableData = this.prepareTableData(data, view);
            worksheetData.push(...tableData);

            // Create workbook and worksheet
            const wb = XLSX.utils.book_new();
            const ws = XLSX.utils.aoa_to_sheet(worksheetData);

            // Set column widths
            const colWidths = columns.map(() => ({ wch: 15 }));
            ws['!cols'] = colWidths;

            // Add worksheet to workbook
            XLSX.utils.book_append_sheet(wb, ws, 'Report');

            // Generate filename and save
            const filename = this.generateFilename(title, 'xlsx');
            XLSX.writeFile(wb, filename);

            return filename;
        } catch (error) {
            console.error('Excel export error:', error);
            throw new Error(`Excel export failed: ${error.message}`);
        }
    }

    /**
     * Export table data to PDF
     * @param {Array} data - Table data
     * @param {Array} columns - Column headers
     * @param {string} title - Export title
     */
    static async exportTableToPDF(data, columns, title = 'Table Export') {
        if (typeof jspdf === 'undefined' || !jspdf.jsPDF) {
            throw new Error('jsPDF library not loaded');
        }

        try {
            const { jsPDF } = jspdf;
            const doc = new jsPDF('l', 'mm', 'a4');

            // Add header
            this.addPDFHeader(doc, title);

            // Add metadata
            this.addPDFMetadata(doc, {
                generatedBy: window.DashboardState?.getCurrentUser()?.name || 'Dean',
                generatedAt: new Date().toLocaleString(),
                recordCount: data.length
            });

            // Add table
            if (typeof doc.autoTable === 'function') {
                doc.autoTable({
                    head: [columns],
                    body: data,
                    startY: 60,
                    theme: 'grid',
                    styles: {
                        fontSize: 9,
                        cellPadding: 3
                    },
                    headStyles: {
                        fillColor: [59, 130, 246],
                        textColor: 255,
                        fontStyle: 'bold'
                    },
                    alternateRowStyles: {
                        fillColor: [245, 247, 250]
                    },
                    margin: { top: 60, left: 10, right: 10 }
                });
            }

            // Add footer
            this.addPDFFooter(doc);

            // Save
            const filename = this.generateFilename(title, 'pdf');
            doc.save(filename);

            return filename;
        } catch (error) {
            console.error('Table PDF export error:', error);
            throw new Error(`Table PDF export failed: ${error.message}`);
        }
    }

    /**
     * Export table data to Excel
     * @param {Array} data - Table data
     * @param {Array} columns - Column headers
     * @param {string} title - Export title
     */
    static async exportTableToExcel(data, columns, title = 'Table Export') {
        if (typeof XLSX === 'undefined') {
            throw new Error('XLSX library not loaded');
        }

        try {
            const worksheetData = [];

            // Add title
            worksheetData.push([title]);
            worksheetData.push([]);

            // Add metadata
            worksheetData.push(['Generated By', window.DashboardState?.getCurrentUser()?.name || 'Dean']);
            worksheetData.push(['Generated At', new Date().toLocaleString()]);
            worksheetData.push(['Record Count', data.length]);
            worksheetData.push([]);

            // Add headers and data
            worksheetData.push(columns);
            worksheetData.push(...data);

            // Create workbook
            const wb = XLSX.utils.book_new();
            const ws = XLSX.utils.aoa_to_sheet(worksheetData);

            // Set column widths
            ws['!cols'] = columns.map(() => ({ wch: 15 }));

            // Add worksheet
            XLSX.utils.book_append_sheet(wb, ws, 'Data');

            // Save
            const filename = this.generateFilename(title, 'xlsx');
            XLSX.writeFile(wb, filename);

            return filename;
        } catch (error) {
            console.error('Table Excel export error:', error);
            throw new Error(`Table Excel export failed: ${error.message}`);
        }
    }

    /**
     * Add PDF header with branding
     */
    static addPDFHeader(doc, title) {
        // University name
        doc.setFontSize(16);
        doc.setFont(undefined, 'bold');
        doc.text('Al-Quds University', 148, 15, { align: 'center' });

        // Archiving System
        doc.setFontSize(12);
        doc.setFont(undefined, 'normal');
        doc.text('Academic Archiving System', 148, 22, { align: 'center' });

        // Report title
        doc.setFontSize(14);
        doc.setFont(undefined, 'bold');
        doc.text(title, 148, 32, { align: 'center' });

        // Horizontal line
        doc.setLineWidth(0.5);
        doc.line(10, 38, 287, 38);
    }

    /**
     * Add PDF metadata section
     */
    static addPDFMetadata(doc, metadata) {
        doc.setFontSize(9);
        doc.setFont(undefined, 'normal');

        let yPos = 45;
        Object.entries(metadata).forEach(([key, value]) => {
            const label = this.formatMetadataKey(key);
            doc.text(`${label}: ${value}`, 10, yPos);
            yPos += 5;
        });
    }

    /**
     * Add PDF footer
     */
    static addPDFFooter(doc) {
        const pageCount = doc.internal.getNumberOfPages();
        
        for (let i = 1; i <= pageCount; i++) {
            doc.setPage(i);
            doc.setFontSize(8);
            doc.setFont(undefined, 'normal');
            doc.text(
                `Page ${i} of ${pageCount}`,
                148,
                200,
                { align: 'center' }
            );
        }
    }

    /**
     * Prepare table data based on view type
     */
    static prepareTableData(data, view) {
        if (!Array.isArray(data)) return [];

        switch (view) {
            case 'department':
                return data.map(item => [
                    item.departmentName || 'N/A',
                    item.totalCourses || 0,
                    item.totalProfessors || 0,
                    item.uploadedDocuments || 0,
                    item.pendingDocuments || 0,
                    item.overdueDocuments || 0,
                    `${Math.round(item.compliancePercentage || 0)}%`
                ]);

            case 'level':
                return data.map(item => [
                    item.courseLevel || 'N/A',
                    item.totalCourses || 0,
                    item.uploadedDocuments || 0,
                    item.pendingDocuments || 0,
                    item.overdueDocuments || 0,
                    `${Math.round(item.compliancePercentage || 0)}%`
                ]);

            case 'semester':
                return data.map(item => [
                    item.semesterName || 'N/A',
                    item.academicYear || 'N/A',
                    item.totalCourses || 0,
                    item.totalProfessors || 0,
                    item.uploadedDocuments || 0,
                    item.pendingDocuments || 0,
                    item.overdueDocuments || 0,
                    `${Math.round(item.compliancePercentage || 0)}%`
                ]);

            default:
                return data;
        }
    }

    /**
     * Format metadata key for display
     */
    static formatMetadataKey(key) {
        return key
            .replace(/([A-Z])/g, ' $1')
            .replace(/^./, str => str.toUpperCase())
            .trim();
    }

    /**
     * Generate timestamped filename
     */
    static generateFilename(prefix, format) {
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
        const sanitizedPrefix = prefix.replace(/[^a-z0-9]/gi, '_').toLowerCase();
        return `${sanitizedPrefix}_${timestamp}.${format}`;
    }
}

// Make ExportService available globally
window.ExportService = ExportService;
