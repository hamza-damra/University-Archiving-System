# HOD (Head of Department) User Guide

## Overview

Welcome to the Document Archiving System HOD Dashboard. As an HOD user, you have department-scoped read access to view submission status, generate reports, and browse files for professors in your department.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Dashboard Overview](#dashboard-overview)
3. [Viewing Submission Status](#viewing-submission-status)
4. [Generating Reports](#generating-reports)
5. [File Explorer](#file-explorer)
6. [Understanding Submission Statuses](#understanding-submission-statuses)

---

## Getting Started

### Logging In

1. Navigate to the login page
2. Enter your HOD credentials (email and password)
3. Click "Login"
4. You will be redirected to the HOD Dashboard

### Dashboard Layout

The HOD Dashboard provides access to:
- **Dashboard**: Overview of submission statistics for your department
- **Submission Status**: Detailed view of professor submissions
- **Reports**: Generate and export submission reports
- **File Explorer**: Browse files uploaded by professors in your department

**Important:** All data you see is scoped to your department only. You cannot view or access data from other departments.

---

## Dashboard Overview

### Selecting a Semester

All dashboard views are semester-based. Start by selecting the semester you want to view:

1. Use the **Academic Year** dropdown to select the year
2. Use the **Semester** dropdown to select the semester (First, Second, or Summer)
3. The dashboard will automatically update with data for that semester

### Overview Widgets

The dashboard displays key statistics for your department:

**Total Professors**
- Number of professors in your department who have course assignments in the selected semester

**Total Courses**
- Number of unique courses taught by your department in the selected semester

**Total Course Assignments**
- Number of professor-course pairings in the selected semester

**Submission Statistics**
- **Total Required Documents**: Total number of documents that should be submitted
- **Submitted Documents**: Number of documents successfully uploaded
- **Missing Documents**: Number of documents not yet uploaded (deadline not passed)
- **Overdue Documents**: Number of documents past their deadline and not uploaded

### Interpreting the Statistics

**Example:**
```
Total Professors: 10
Total Courses: 15
Total Course Assignments: 20
(Some professors teach multiple courses)

Submission Statistics:
- Total Required: 60 (20 assignments × 3 document types average)
- Submitted: 55 (92% compliance)
- Missing: 3 (5%)
- Overdue: 2 (3%)
```

**Action Items:**
- If "Overdue" is high, follow up with specific professors
- If "Missing" is increasing as deadlines approach, send reminders
- Track trends across semesters to identify patterns

---


## Viewing Submission Status

The Submission Status view provides a detailed breakdown of which professors have submitted which documents.

### Accessing Submission Status

1. Navigate to **Submission Status** section
2. Select an academic year and semester
3. View the professor submission table

### Submission Status Table

The table displays:

| Column | Description |
|--------|-------------|
| Professor | Professor name |
| Course Code | Course identifier (e.g., CS101) |
| Course Name | Full course name |
| Document Types | Status for each required document type |

**Document Type Status Indicators:**
- ✅ **UPLOADED**: Document submitted on time
- ⏳ **NOT_UPLOADED**: Document not yet submitted (deadline not passed)
- ⚠️ **OVERDUE**: Deadline passed, document not submitted

### Filtering Submission Status

Use filters to narrow down the view:

**Filter by Course:**
1. Use the **Course Code** dropdown
2. Select a specific course
3. View submissions for that course only

**Filter by Document Type:**
1. Use the **Document Type** dropdown
2. Select a type (SYLLABUS, EXAM, ASSIGNMENT, etc.)
3. View submissions for that document type only

**Filter by Status:**
1. Use the **Status** dropdown
2. Select a status (NOT_UPLOADED, UPLOADED, OVERDUE)
3. View only submissions with that status

**Combining Filters:**
You can combine multiple filters. For example:
- Course: CS101
- Document Type: SYLLABUS
- Status: OVERDUE

This shows all overdue syllabi for CS101.

### Example Submission Status View

```
Professor: John Doe
Course: CS101 - Introduction to Computer Science

Document Statuses:
- SYLLABUS: ✅ UPLOADED (Submitted: Sept 10, 2024)
- EXAM: ⏳ NOT_UPLOADED (Deadline: Dec 1, 2024)
- ASSIGNMENT: ⚠️ OVERDUE (Deadline: Nov 1, 2024)

Professor: Jane Smith
Course: CS102 - Data Structures

Document Statuses:
- SYLLABUS: ✅ UPLOADED (Submitted: Sept 8, 2024)
- EXAM: ✅ UPLOADED (Submitted: Nov 15, 2024)
- ASSIGNMENT: ✅ UPLOADED (Submitted: Oct 20, 2024)
```

### Taking Action on Submission Status

**For Missing Documents:**
1. Identify professors with NOT_UPLOADED status
2. Check the deadline
3. Send reminder emails if deadline is approaching

**For Overdue Documents:**
1. Identify professors with OVERDUE status
2. Contact them directly to request submission
3. Escalate to Deanship if necessary

---

## Generating Reports

### Professor Submission Report

This report provides a comprehensive view of submission status for your department.

**Steps to Generate:**

1. Navigate to **Reports** section
2. Select an academic year and semester
3. Click **"Generate Professor Submission Report"**

**Report Contents:**

- **Header Information:**
  - Semester name
  - Department name
  - Generation date and time

- **Professor Submission Rows:**
  - Each row shows a professor-course assignment
  - Document type statuses
  - Submission dates (if uploaded)

- **Summary Statistics:**
  - Total professors
  - Total courses
  - Total required documents
  - Submitted count
  - Missing count
  - Overdue count

### Exporting Reports to PDF

**Steps:**

1. Generate the report (as above)
2. Click **"Export to PDF"** button
3. PDF file will be downloaded to your computer

**PDF Report Features:**
- Professional formatting
- University branding (if configured)
- Complete submission data
- Summary statistics
- Generation timestamp

**Use Cases for PDF Reports:**
- Share with university administration
- Archive for record-keeping
- Present in department meetings
- Include in annual reports

### Report Filtering

Before exporting, you can apply filters to customize the report:

1. Use the filter options (course, document type, status)
2. Click **"Generate Report"** with filters applied
3. Export the filtered report to PDF

**Example:** Export a report showing only overdue submissions to focus on action items.

---


## File Explorer

The File Explorer allows you to browse and download files uploaded by professors in your department.

### Accessing the File Explorer

1. Navigate to **File Explorer** section
2. Select an academic year and semester
3. Browse the hierarchical file structure

### File Explorer Hierarchy

```
Semester
└── Professor (Your Department Only)
    └── Course
        └── Document Type
            └── Files
```

**Note:** You can only see professors from your department. Professors from other departments are not visible.

### Navigating the File Explorer

**Tree View (Left Panel):**
- Shows the folder structure
- Click on folders to expand/collapse
- Click on a folder to view its contents in the right panel

**File List (Right Panel):**
- Shows files in the selected folder
- Displays file details:
  - Filename
  - File size
  - Upload date and time
  - Uploaded by (professor name)
  - Notes (if any)

### Viewing File Details

1. Navigate to a document type folder
2. View the list of uploaded files
3. Click on a file to see detailed information:
   - Original filename
   - File size
   - File type (PDF, ZIP)
   - Upload timestamp
   - Professor who uploaded it
   - Any notes provided by the professor

### Downloading Files

**To Download a Single File:**

1. Navigate to the file in the File Explorer
2. Click the **"Download"** button next to the file
3. File will be downloaded to your computer

**Permissions:** You have read-only access. You can view and download files but cannot upload, modify, or delete them.

### Breadcrumb Navigation

Use the breadcrumb trail at the top to navigate:

```
Home > 2024-2025 > First Semester > John Doe > CS101 > Syllabus
```

Click any breadcrumb to jump to that level.

### Use Cases for File Explorer

**Quality Assurance:**
- Review syllabi for consistency across courses
- Check exam formats and standards
- Verify assignment requirements

**Reference:**
- Download files for department records
- Share examples with new professors
- Archive important documents

**Verification:**
- Confirm files were uploaded correctly
- Check file contents match requirements
- Verify submission completeness

---

## Understanding Submission Statuses

### Status Definitions

**NOT_UPLOADED**
- Document has not been uploaded yet
- Deadline has not passed
- No action required if deadline is far away
- Send reminder if deadline is approaching

**UPLOADED**
- Document has been successfully uploaded
- Uploaded before or on the deadline
- Submission is complete and on time

**OVERDUE**
- Deadline has passed
- Document has not been uploaded
- Requires immediate follow-up with professor

### Status Transitions

```
NOT_UPLOADED → UPLOADED (when professor uploads before deadline)
NOT_UPLOADED → OVERDUE (when deadline passes without upload)
OVERDUE → UPLOADED (when professor uploads after deadline, marked as late)
```

### Late Submissions

If a professor uploads a document after the deadline:
- Status changes to UPLOADED
- Submission is marked as "Late Submission"
- Late flag is visible in reports and file details

**Note:** The system accepts late submissions. It's up to department policy whether to accept or reject them.

---

## Best Practices

### Regular Monitoring

1. **Check dashboard weekly**: Monitor submission progress throughout the semester
2. **Review upcoming deadlines**: Identify documents due soon
3. **Track overdue items**: Follow up promptly on overdue submissions

### Communication with Professors

1. **Send early reminders**: Notify professors 1-2 weeks before deadlines
2. **Follow up on overdue items**: Contact professors within 1-2 days of deadline passing
3. **Provide support**: Help professors who have technical difficulties

### Report Generation

1. **Generate reports regularly**: Weekly or bi-weekly during active periods
2. **Archive PDF reports**: Keep records for each semester
3. **Share with administration**: Provide updates to university leadership

### File Review

1. **Spot-check submissions**: Randomly review files for quality
2. **Verify completeness**: Ensure all required documents are present
3. **Check file quality**: Confirm files are readable and complete

---

## Troubleshooting

### Cannot See Any Professors

**Problem:** File Explorer or Submission Status shows no professors

**Solutions:**
- Verify you're assigned to a department
- Check if any professors in your department have course assignments for the selected semester
- Ensure the correct semester is selected

### Report Shows No Data

**Problem:** Generated report is empty

**Solutions:**
- Verify semester has course assignments for your department
- Check if required document types are defined for courses
- Ensure professors have been assigned to courses by Deanship

### Cannot Download File

**Problem:** Download button doesn't work or file not found

**Solutions:**
- Verify file still exists in the system
- Check your browser's download settings
- Try a different browser
- Contact IT support if problem persists

### Statistics Don't Match

**Problem:** Dashboard statistics seem incorrect

**Solutions:**
- Refresh the page to reload data
- Verify you're viewing the correct semester
- Check if recent submissions haven't been processed yet
- Contact system administrator if discrepancy persists

---

## Frequently Asked Questions

**Q: Can I upload files on behalf of professors?**
A: No, HOD role has read-only access. Only professors can upload files for their courses.

**Q: Can I see files from other departments?**
A: No, your access is limited to your department only.

**Q: Can I delete or modify uploaded files?**
A: No, you have read-only access. Contact Deanship if files need to be removed.

**Q: How often is the dashboard updated?**
A: The dashboard shows real-time data. Refresh the page to see the latest updates.

**Q: Can I export reports in formats other than PDF?**
A: Currently, only PDF export is supported. Future versions may include Excel export.

**Q: What happens if a professor uploads the wrong file?**
A: The professor can replace the file using the "Replace Files" feature in their dashboard.

**Q: Can I set deadlines for document submissions?**
A: No, deadlines are set by Deanship when defining required document types.

**Q: How do I know if a submission is late?**
A: Late submissions are marked with a "Late Submission" flag in the file details and reports.

---

## Support

For technical support or questions about the HOD Dashboard, contact:
- IT Support: support@university.edu
- System Administrator: admin@university.edu
- Deanship Office: deanship@university.edu

**Last Updated:** November 18, 2025
