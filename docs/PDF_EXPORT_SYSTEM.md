# PDF Export System Documentation

## Overview

The University Archiving System features a comprehensive client-side PDF and Excel export system built with JavaScript. This system provides professional-grade report generation with:

- **Professional Academic Formatting**: University branding, proper typography, and academic styling
- **Dynamic Table Generation**: Auto-pagination, colored status badges, and proper column alignment
- **Multi-page Support**: Automatic page breaks with headers/footers on each page
- **Role-based Templates**: Different report layouts for HOD, Deanship, and Admin
- **Large Dataset Support**: Handles 3000+ rows efficiently with chunked processing
- **Excel Export**: Full spreadsheet export with formatting using SheetJS

---

## Architecture

### Core Components

```
src/main/resources/static/js/shared/
├── report-export-service.js  # Main PDF/Excel generation engine
├── export-ui.js              # UI components (modal, progress, toasts)
└── hod-reports.js            # HOD-specific report integration

src/main/resources/static/css/
└── report-export.css         # Export UI styling
```

### Dependencies (CDN)

- **jsPDF** (v2.5.1): Core PDF generation
- **jsPDF-AutoTable** (v3.8.0): Table rendering plugin
- **SheetJS/xlsx** (v0.18.5): Excel export

---

## Report Types

### 1. Professor Submission Report (`exportProfessorSubmissionPDF`)

Used by HOD to view submission status for professors in their department.

**Data Structure:**
```javascript
{
    semesterId: 1,
    semesterName: "First Semester 2024-2025",
    departmentId: 5,
    departmentName: "Computer Science",
    generatedAt: "2025-01-18T10:30:00",
    generatedBy: "Dr. Ahmad Hassan",
    rows: [
        {
            professorId: 10,
            professorName: "Dr. Sarah Ahmed",
            professorEmail: "sarah@alquds.edu",
            courseCode: "CS101",
            courseName: "Introduction to Programming",
            documentStatuses: {
                "SYLLABUS": { status: "UPLOADED", deadline: "...", submittedAt: "..." },
                "EXAM": { status: "OVERDUE", deadline: "...", submittedAt: null },
                // ...
            }
        }
    ],
    statistics: {
        totalProfessors: 15,
        totalCourses: 30,
        totalRequiredDocuments: 150,
        submittedDocuments: 120,
        missingDocuments: 20,
        overdueDocuments: 10
    }
}
```

### 2. System-Wide Report (`exportSystemWidePDF`)

Used by Deanship/Admin for cross-department overview.

**Data Structure:**
```javascript
{
    semesterId: 1,
    semesterName: "First Semester 2024-2025",
    generatedAt: "2025-01-18T10:30:00",
    generatedBy: "Dean Office",
    departmentSummaries: [
        {
            departmentId: 1,
            departmentName: "Computer Science",
            statistics: {
                totalProfessors: 20,
                totalCourses: 45,
                totalRequiredDocuments: 225,
                submittedDocuments: 180,
                missingDocuments: 30,
                overdueDocuments: 15
            }
        }
    ],
    overallStatistics: {
        totalProfessors: 100,
        totalCourses: 200,
        // ...
    }
}
```

### 3. Department Report (`exportDepartmentPDF`)

Detailed department-level analysis with professor summaries.

---

## Usage Examples

### Basic PDF Export

```javascript
// Export Professor Submission Report
const reportData = await api.getReportData(semesterId);

await window.reportExportService.exportProfessorSubmissionPDF(reportData, {
    filters: {
        semester: 'First Semester 2024-2025',
        department: 'Computer Science',
        status: 'All'
    }
});
```

### With Progress Modal

```javascript
// Using the convenience function with modal
await window.executeExportWithProgress('pdf', 'professor', reportData, {
    title: 'Professor Submission Report',
    filters: {
        semester: reportData.semesterName,
        department: reportData.departmentName
    }
});
```

### Excel Export

```javascript
// Export to Excel
await window.executeExportWithProgress('excel', 'systemWide', reportData, {
    title: 'System-Wide Report'
});
```

### Direct Export Functions (Global)

```javascript
// Quick export functions available globally
await window.exportProfessorReportPDF(reportData, options);
await window.exportSystemReportPDF(reportData, options);
await window.exportDepartmentReportPDF(reportData, options);
await window.exportReportExcel(reportData, options);
```

---

## Status Colors

| Status | Background | Text | PDF Color |
|--------|------------|------|-----------|
| UPLOADED | Green (#d1fae5) | Dark Green (#065f46) | [34, 197, 94] |
| NOT_UPLOADED | Gray (#f3f4f6) | Dark Gray (#4b5563) | [107, 114, 128] |
| OVERDUE | Red (#fee2e2) | Dark Red (#991b1b) | [239, 68, 68] |
| PENDING | Yellow (#fef3c7) | Dark Yellow (#92400e) | [234, 179, 8] |

---

## PDF Layout Specifications

### Page Setup
- **Format**: A4 Landscape (297mm × 210mm)
- **Margins**: 10mm (left, right), 25mm (top, bottom)
- **Header Height**: 35mm
- **Footer Height**: 15mm

### Typography
- **Title**: 16pt Bold
- **Subtitle**: 12pt Normal
- **Heading**: 11pt Bold
- **Body Text**: 9pt Normal
- **Small Text**: 8pt Normal
- **Tiny (Footer)**: 7pt Normal

### Brand Colors
- **Primary**: #3b82f6 (Blue)
- **Success**: #22c55e (Green)
- **Warning**: #eab308 (Yellow)
- **Danger**: #ef4444 (Red)
- **Gray**: #6b7280

---

## Performance Considerations

### Large Datasets (3000+ rows)

The export service uses chunked processing for large datasets:

```javascript
// Configuration in report-export-service.js
const EXPORT_CONFIG = {
    chunkSize: 500,           // Process 500 rows at a time
    maxRowsPerPage: 25,       // Rows per page for readability
};
```

### Progress Tracking

Set up progress callback for long-running exports:

```javascript
window.reportExportService.setProgressCallback((percent, message) => {
    console.log(`${percent}%: ${message}`);
    // Update UI progress indicator
});
```

---

## UI Components

### Export Modal

```javascript
// Show export progress modal
window.exportModal.open('Generating PDF Report');

// Update progress
window.exportModal.updateProgress(50, 'Processing data...');

// Show success
window.exportModal.showSuccess('Export completed!');

// Show error
window.exportModal.showError('Failed to generate PDF');

// Close modal
window.exportModal.close();
```

### Toast Notifications

```javascript
window.exportToast.success('Export Complete', 'Your PDF has been downloaded.');
window.exportToast.error('Export Failed', 'Please try again.');
window.exportToast.info('Processing', 'Generating your report...');
```

### Export Dropdown

```javascript
const dropdown = new ExportDropdown(document.getElementById('exportContainer'), {
    onPdfExport: () => exportToPDF(data),
    onExcelExport: () => exportToExcel(data),
    buttonText: 'Export Report'
});
```

---

## Integration with Existing Pages

### HOD Dashboard

```javascript
// In hod-reports.js
async exportPdf() {
    if (window.reportExportService) {
        await window.executeExportWithProgress('pdf', 'professor', this.reportData, {
            filters: {
                semester: this.reportData.semesterName,
                department: this.reportData.departmentName,
            }
        });
    }
}
```

### Deanship Dashboard

```javascript
// In deanship-export.js
// Automatically uses enhanced export when reportExportService is available
await ExportService.exportToPDF(data, {
    title: 'Department Report',
    view: 'department',
    metadata: { generatedBy: 'Dean Office' }
});
```

### Admin Dashboard

```javascript
// In admin-reports.js
async exportToPdf() {
    await window.executeExportWithProgress('pdf', 'systemWide', this.reportData, {
        title: 'System-Wide Submission Report',
        filters: {
            semester: this.reportData.semesterName,
            department: 'All Departments'
        }
    });
}
```

---

## Adding New Report Types

To add a new report type:

1. **Define the export method in `ReportExportService`:**

```javascript
async exportNewReportPDF(reportData, options = {}) {
    return this._generatePDF('newReport', reportData, {
        title: 'New Report Type',
        subtitle: reportData.subtitle || '',
        ...options,
    });
}
```

2. **Add content generation method:**

```javascript
async _addNewReportTable(doc, reportData, options) {
    const tableRows = reportData.items.map(item => [
        item.field1,
        item.field2,
        // ...
    ]);
    
    await this._addLargeTable(doc, tableRows, {
        head: [['Field 1', 'Field 2', /* ... */]],
        startY: 78,
        // ... column styles
    });
}
```

3. **Update the content router in `_addReportContent`:**

```javascript
case 'newReport':
    await this._addNewReportTable(doc, reportData, options);
    break;
```

---

## Alternative Libraries

If jsPDF doesn't meet specific needs, consider:

| Library | Pros | Cons |
|---------|------|------|
| **PDF-LIB** | Pure JavaScript, good for editing existing PDFs | More complex API |
| **pdfmake** | Declarative syntax, good table support | Larger bundle size |
| **Puppeteer** | Full HTML/CSS support, pixel-perfect output | Server-side only |
| **html2pdf.js** | Simple HTML to PDF conversion | Less control over output |

---

## Troubleshooting

### Common Issues

**1. "jsPDF library not loaded"**
- Ensure CDN scripts are loaded before export service
- Check network connectivity for CDN access

**2. "autoTable plugin not available"**
- Verify jspdf-autotable script is loaded after jspdf

**3. Large file export freezes browser**
- Enable chunked processing (default)
- Consider using Web Workers for very large datasets (4000+ rows)

**4. Logo not appearing in PDF**
- Check logo path (`/black-logo.png`)
- Ensure logo is served with correct CORS headers

---

## Browser Support

| Browser | Support |
|---------|---------|
| Chrome 80+ | ✅ Full |
| Firefox 75+ | ✅ Full |
| Safari 14+ | ✅ Full |
| Edge 80+ | ✅ Full |
| IE 11 | ❌ Not Supported |

---

## Version History

- **v1.0.0** (2026-01-18): Initial release with PDF and Excel support
  - Three report types: Professor, System-Wide, Department
  - Progress modal with async rendering
  - Large dataset support (3000+ rows)
  - University branding integration
