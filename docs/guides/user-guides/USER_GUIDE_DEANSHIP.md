# Deanship User Guide

## Overview

Welcome to the Document Archiving System Deanship Dashboard. As a Deanship user, you have global management authority over the academic structure, professors, courses, and course assignments across all departments.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Academic Year Management](#academic-year-management)
3. [Professor Management](#professor-management)
4. [Course Management](#course-management)
5. [Course Assignment Management](#course-assignment-management)
6. [Required Document Types](#required-document-types)
7. [System-Wide Reports](#system-wide-reports)
8. [File Explorer](#file-explorer)

---

## Getting Started

### Logging In

1. Navigate to the login page
2. Enter your Deanship credentials (email and password)
3. Click "Login"
4. You will be redirected to the Deanship Dashboard

### Dashboard Overview

The Deanship Dashboard provides access to:
- **Academic Years**: Manage academic years and semesters
- **Professors**: Create and manage professor accounts
- **Courses**: Create and manage courses
- **Assignments**: Assign professors to courses for specific semesters
- **Reports**: View system-wide submission statistics
- **File Explorer**: Browse all files across all departments

---

## Academic Year Management

### Creating an Academic Year

Academic years are the foundation of the system. Each academic year automatically creates three semesters: First (Fall), Second (Spring), and Summer.

**Steps:**

1. Navigate to **Academic Years** section
2. Click **"Create Academic Year"** button
3. Fill in the form:
   - **Year Code**: Format "YYYY-YYYY" (e.g., "2024-2025")
   - **Start Year**: The beginning year (e.g., 2024)
   - **End Year**: The ending year (e.g., 2025)
4. Click **"Create"**

**Result:** The system creates:
- One academic year record
- Three semester records (First, Second, Summer) with default date ranges

**Example:**
```
Year Code: 2024-2025
Start Year: 2024
End Year: 2025

Creates:
- First Semester: September 1, 2024 - January 15, 2025
- Second Semester: February 1, 2025 - June 15, 2025
- Summer Semester: July 1, 2025 - August 31, 2025
```

### Editing an Academic Year

1. In the Academic Years list, click **"Edit"** next to the year
2. Modify the year code or dates
3. Click **"Save Changes"**

### Activating an Academic Year

Only one academic year can be active at a time. The active year is used as the default in various parts of the system.

1. In the Academic Years list, click **"Activate"** next to the desired year
2. Confirm the activation
3. The previously active year will be automatically deactivated

### Editing Semester Dates

1. Navigate to the academic year
2. Click on a semester to expand its details
3. Click **"Edit Semester"**
4. Modify start and end dates
5. Click **"Save"**

---


## Professor Management

### Creating a Professor Account

**Steps:**

1. Navigate to **Professors** section
2. Click **"Create Professor"** button
3. Fill in the form:
   - **Email**: Professor's university email (must be unique)
   - **First Name**: Professor's first name
   - **Last Name**: Professor's last name
   - **Password**: Initial password (professor can change later)
   - **Department**: Select from dropdown
4. Click **"Create"**

**Result:** The system:
- Creates a new user account with ROLE_PROFESSOR
- Automatically generates a unique professor_id (e.g., "prof_10")
- Assigns the professor to the selected department

### Viewing Professors

**All Professors:**
1. Navigate to **Professors** section
2. View the complete list of all professors across all departments

**Filter by Department:**
1. Use the department dropdown filter
2. Select a specific department
3. View only professors in that department

### Editing a Professor

1. In the Professors list, click **"Edit"** next to the professor
2. Modify the information (email, name, department)
3. Click **"Save Changes"**

**Note:** You cannot change the professor_id once created.

### Deactivating a Professor

Deactivating a professor is a soft delete that preserves historical data.

1. In the Professors list, click **"Deactivate"** next to the professor
2. Confirm the deactivation

**Effects:**
- Professor cannot log in
- Historical course assignments and submissions are preserved
- Professor cannot be assigned to new courses
- Existing course assignments remain visible in reports

### Reactivating a Professor

1. In the Professors list, find the deactivated professor
2. Click **"Activate"**
3. Professor can now log in and be assigned to courses

---

## Course Management

### Creating a Course

**Steps:**

1. Navigate to **Courses** section
2. Click **"Create Course"** button
3. Fill in the form:
   - **Course Code**: Unique identifier (e.g., "CS101")
   - **Course Name**: Full course name (e.g., "Introduction to Computer Science")
   - **Department**: Select from dropdown
   - **Level**: Undergraduate, Graduate, or other
   - **Description**: Optional course description
4. Click **"Create"**

### Viewing Courses

**All Courses:**
1. Navigate to **Courses** section
2. View the complete list of all courses

**Filter by Department:**
1. Use the department dropdown filter
2. Select a specific department
3. View only courses in that department

### Editing a Course

1. In the Courses list, click **"Edit"** next to the course
2. Modify the information
3. Click **"Save Changes"**

### Deactivating a Course

1. In the Courses list, click **"Deactivate"** next to the course
2. Confirm the deactivation

**Effects:**
- Course cannot be assigned to professors for new semesters
- Historical course assignments are preserved
- Course remains visible in reports for past semesters

---

## Course Assignment Management

Course assignments link professors to courses for specific semesters. This is how you define who teaches what and when.

### Assigning a Course to a Professor

**Steps:**

1. Navigate to **Course Assignments** section
2. Click **"Assign Course"** button
3. Fill in the form:
   - **Semester**: Select academic year and semester
   - **Course**: Select from dropdown
   - **Professor**: Select from dropdown
4. Click **"Assign"**

**Validation:**
- The system prevents duplicate assignments (same professor, course, and semester)
- Only active professors and courses can be assigned
- Professor must belong to the same department as the course

### Viewing Course Assignments

**By Semester:**
1. Select an academic year and semester from the dropdowns
2. View all course assignments for that semester

**By Professor:**
1. Select a semester
2. Use the professor filter dropdown
3. View all courses assigned to that professor

### Unassigning a Course

1. In the Course Assignments list, click **"Unassign"** next to the assignment
2. Confirm the unassignment

**Warning:** Unassigning a course will affect:
- Professor's dashboard (course will no longer appear)
- Required document types for that assignment
- Submission tracking

**Best Practice:** Only unassign if the assignment was created in error. For completed semesters, leave assignments in place for historical records.

---


## Required Document Types

Required document types define what documents professors must submit for each course. You can set requirements at the course level with optional semester-specific overrides.

### Adding a Required Document Type

**Steps:**

1. Navigate to **Courses** section
2. Click on a course to view its details
3. Click **"Add Required Document"** button
4. Fill in the form:
   - **Document Type**: Select from dropdown (SYLLABUS, EXAM, ASSIGNMENT, PROJECT_DOCS, LECTURE_NOTES, OTHER)
   - **Semester**: Optional - leave blank for all semesters, or select specific semester
   - **Deadline**: Optional - set a submission deadline
   - **Is Required**: Check if mandatory
   - **Max File Count**: Maximum number of files allowed (default: 5)
   - **Max Total Size (MB)**: Maximum total size in megabytes (default: 50)
   - **Allowed File Extensions**: Select allowed types (PDF, ZIP)
5. Click **"Add"**

### Document Type Examples

**Syllabus:**
- Document Type: SYLLABUS
- Deadline: First week of semester
- Max File Count: 1
- Max Total Size: 10 MB
- Allowed Extensions: PDF

**Exams:**
- Document Type: EXAM
- Deadline: End of semester
- Max File Count: 5 (midterm, final, makeup exams)
- Max Total Size: 50 MB
- Allowed Extensions: PDF, ZIP

**Assignments:**
- Document Type: ASSIGNMENT
- Deadline: Throughout semester
- Max File Count: 10
- Max Total Size: 100 MB
- Allowed Extensions: PDF, ZIP

### Semester-Specific Requirements

You can create different requirements for the same document type in different semesters:

**Example:**
- CS101 - SYLLABUS - First Semester 2024-2025 - Deadline: Sept 15
- CS101 - SYLLABUS - Second Semester 2024-2025 - Deadline: Feb 15

### Editing Required Document Types

1. Navigate to the course
2. Find the required document type in the list
3. Click **"Edit"**
4. Modify the settings
5. Click **"Save"**

---

## System-Wide Reports

### Viewing System-Wide Submission Report

The system-wide report shows submission statistics across all departments for a selected semester.

**Steps:**

1. Navigate to **Reports** section
2. Select an academic year and semester
3. Click **"Generate System-Wide Report"**

**Report Contents:**

- **Overall Statistics:**
  - Total professors across all departments
  - Total courses
  - Total required documents
  - Submitted documents count
  - Missing documents count
  - Overdue documents count

- **Department Breakdown:**
  - For each department:
    - Department name
    - Total professors
    - Total courses
    - Submission statistics

**Use Cases:**
- Monitor compliance across the university
- Identify departments needing support
- Track submission trends over time
- Prepare reports for university administration

### Exporting Reports

Currently, system-wide reports are viewed in the dashboard. Future versions will support PDF export.

---

## File Explorer

The File Explorer allows you to browse all files in the system with a hierarchical view.

### Navigating the File Explorer

**Hierarchy:**
```
Academic Year
└── Semester
    └── Professor
        └── Course
            └── Document Type
                └── Files
```

**Steps:**

1. Navigate to **File Explorer** section
2. Select an academic year and semester
3. Browse the tree structure:
   - Click on a professor to expand their courses
   - Click on a course to expand document types
   - Click on a document type to view files

### Viewing Files

1. Navigate to a document type folder
2. View the file list with:
   - Filename
   - File size
   - Upload date and time
   - Uploaded by (professor name)
   - Notes (if any)

### Downloading Files

1. Navigate to a file
2. Click **"Download"** button
3. File will be downloaded to your computer

**Permissions:** As Deanship, you can download all files across all departments.

### Breadcrumb Navigation

Use the breadcrumb trail at the top of the File Explorer to quickly navigate back to parent folders:

```
Home > 2024-2025 > First Semester > John Doe > CS101 > Syllabus
```

Click any breadcrumb item to jump to that level.

---

## Best Practices

### Academic Year Setup

1. **Create academic years in advance**: Set up the next academic year before the current one ends
2. **Review semester dates**: Ensure semester dates align with university calendar
3. **Activate the correct year**: Keep only the current academic year active

### Professor Management

1. **Use university email addresses**: Ensures uniqueness and official communication
2. **Assign to correct department**: Double-check department assignments
3. **Deactivate instead of delete**: Preserve historical data by deactivating

### Course Management

1. **Use consistent course codes**: Follow university naming conventions
2. **Include full course names**: Make courses easily identifiable
3. **Keep descriptions updated**: Help professors understand course requirements

### Course Assignments

1. **Assign early**: Complete course assignments before semester starts
2. **Verify department alignment**: Ensure professor and course are in same department
3. **Review assignments**: Check for missing or duplicate assignments

### Required Document Types

1. **Set realistic deadlines**: Give professors adequate time to submit
2. **Configure appropriate file limits**: Balance between flexibility and storage
3. **Use consistent document types**: Standardize across similar courses

### Monitoring and Reports

1. **Review reports regularly**: Check submission status weekly
2. **Follow up on overdue items**: Contact HODs about missing submissions
3. **Track trends**: Monitor submission patterns across semesters

---

## Troubleshooting

### Cannot Create Academic Year

**Problem:** Error when creating academic year

**Solutions:**
- Ensure year code is unique (not already used)
- Check year code format (YYYY-YYYY)
- Verify start year is less than end year

### Cannot Assign Course

**Problem:** Error when assigning course to professor

**Solutions:**
- Verify professor and course are in the same department
- Check if assignment already exists for that semester
- Ensure professor and course are both active

### Cannot See Files in File Explorer

**Problem:** File Explorer shows no files

**Solutions:**
- Verify academic year and semester are selected
- Check if any professors have uploaded files for that semester
- Ensure you're logged in with Deanship role

### Report Shows No Data

**Problem:** System-wide report is empty

**Solutions:**
- Verify semester has course assignments
- Check if required document types are defined for courses
- Ensure professors have been assigned to courses

---

## Support

For technical support or questions about the Deanship Dashboard, contact:
- IT Support: support@university.edu
- System Administrator: admin@university.edu

**Last Updated:** November 18, 2025
