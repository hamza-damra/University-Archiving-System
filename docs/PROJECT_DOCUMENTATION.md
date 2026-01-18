# Al-Quds University Document Archiving System

## Complete Project Documentation

---

## Overview

The **Al-Quds University Document Archiving System** is a comprehensive digital platform designed to streamline and modernize the academic document management and archiving process. The system provides a secure, hierarchical, and semester-based structure for storing, managing, and retrieving essential academic materials across all university departments.

This system enables faculty members to securely upload, organize, and access important academic materials including course files, research documents, syllabi, exams, and administrative records.

---

## Key Objectives

| Objective | Description |
|-----------|-------------|
| **Centralized Archiving** | Provide a single source of truth for all academic documents |
| **Hierarchical Access** | Implement a robust role-based access control (RBAC) system |
| **Academic Context** | Organize documents by Academic Year, Semester, and Course |
| **Multi-File Support** | Handle large and diverse sets of document types |
| **Reporting & Analysis** | Provide insights into submission progress and academic activity |

---

## User Roles

The system implements four distinct user roles, each with specific permissions and access levels:

### 1. Administrator (ROLE_ADMIN)

The **Admin** is the highest level of authority within the system with unrestricted access to all features.

**Primary Responsibilities:**
- Create, update, and deactivate accounts for all roles (Admin, Deanship, HOD, Professor)
- Reset passwords and manage user permissions
- Filter users by department, role, or activity status
- Oversee the overall academic structure
- Manage global settings and system-wide parameters
- Execute and monitor database migrations
- View high-level dashboard statistics across the entire university
- Monitor system health
- Access and review security audit reports
- Manage the catalog of Departments and Courses

**Dashboard Sections:**
- Dashboard (Overview Statistics)
- User Management
- Departments
- Courses
- Reports

---

### 2. Deanship (ROLE_DEANSHIP)

The **Deanship** role focuses on academic governance and university-wide coordination of the archiving process.

**Primary Responsibilities:**
- Create and manage Academic Years (e.g., 2024-2025)
- Activate and configure Semesters (First, Second, Summer)
- Assign Professors to Courses for specific semesters
- Manage the association between courses and document requirements
- Monitor submission progress across all university departments
- View recent activity and status distributions
- Generate and analyze system-wide reports on academic document compliance
- Access comprehensive dashboards showing performance metrics by department

**Dashboard Sections:**
- Dashboard (Overview)
- Academic Years Management
- Professors Management
- Courses Management
- Course Assignments
- Reports
- File Explorer

---

### 3. Head of Department - HOD (ROLE_HOD)

The **Head of Department** is responsible for the archiving integrity of their specific academic department.

**Primary Responsibilities:**
- Manage and support Professors within the department
- Request specific documents from professors when needed
- Oversight of document submissions for all courses belonging to the department
- Ensure professors are meeting archiving deadlines and requirements
- Generate reports specific to the department's performance
- View departmental dashboards to track submission status (Pending, Approved, Rejected)
- Browse the departmental file hierarchy to retrieve or review documents

**Dashboard Sections:**
- Dashboard (Department Overview)
- Submission Status Tracking
- Reports
- File Explorer

**Scope Limitation:** HODs cannot see or manage data from other departments. Their view is strictly limited to their own department.

---

### 4. Professor (ROLE_PROFESSOR)

The **Professor** is the primary content producer in the system, responsible for uploading academic materials.

**Primary Responsibilities:**
- Upload required academic documents (Syllabus, Final Exam, Assignments, etc.)
- Support for multiple file uploads for complex course requirements
- Manage documents based on current course assignments for each semester
- Track the status of their own submissions
- Browse their own uploaded documents using the integrated File Explorer
- Access and preview previously uploaded files
- Receive and respond to document requests from HOD or Deanship

**Dashboard Sections:**
- Dashboard (Personal Overview)
- My Courses
- File Explorer
- Notifications

**Scope Limitation:** Professors can only see their own assignments and uploaded files. They do not have access to other professors' data.

---

## System Pages

### Public Pages

| Page | Description |
|------|-------------|
| **Login Page** | Secure authentication portal with email and password. Supports dark/light theme toggle. Includes "About This System" modal with project information. |

---

### Admin Dashboard Pages

| Page | Description |
|------|-------------|
| **Dashboard** | Full overview of system-wide submission counts, user activity, growth trends, and key statistics |
| **User Management** | Interface for creating, editing, and managing all university users across all roles |
| **Departments** | Manage academic departments - create, edit, delete departments |
| **Courses** | Manage the university course catalog - add courses, assign to departments |
| **Reports** | Generate system-wide reports with filtering options, export to PDF |

---

### Deanship Dashboard Pages

| Page | Description |
|------|-------------|
| **Dashboard** | Overview of university-wide metrics and submission statistics |
| **Academic Years** | Create and manage academic year timelines (e.g., 2024-2025, 2025-2026) |
| **Professors** | View and manage professor information across all departments |
| **Courses** | Manage courses and their document requirements |
| **Course Assignments** | Assign professors to courses for specific semesters |
| **Reports** | Generate comprehensive reports by department, course level, or semester. Export to PDF/Excel |
| **File Explorer** | Browse and access all archived documents across the university |

---

### HOD Dashboard Pages

| Page | Description |
|------|-------------|
| **Dashboard** | Real-time view of submission metrics for the department with statistics cards |
| **Submission Status** | Track document submissions from professors with status filters (Pending, Submitted, Overdue) |
| **Reports** | Generate departmental reports on professor submissions. Export to PDF |
| **File Explorer** | Direct access to the archived files within the department |

---

### Professor Dashboard Pages

| Page | Description |
|------|-------------|
| **Dashboard** | Personal overview of assigned courses and submission progress for the current semester |
| **My Courses** | View all assigned courses and their required document submissions |
| **File Explorer** | Personal view of all uploaded files with preview capabilities |

---

## Core Features

### 1. Authentication & Security

- **JWT-based Authentication** - Secure token-based authentication
- **Session Management** - Spring Session JDBC for persistent sessions
- **Role-Based Access Control (RBAC)** - Granular permissions per role
- **Department Scoping** - Automatic data filtering based on user's department
- **Secure Password Hashing** - BCrypt encryption for all passwords
- **Auth Guard** - Client-side authentication verification before page load

---

### 2. Academic Structure Management

- **Academic Years** - Create and manage academic year cycles
- **Semesters** - Three semester types: First, Second, Summer
- **Departments** - University department organization
- **Courses** - Course catalog with department associations
- **Course Assignments** - Link professors to courses per semester

---

### 3. Document Management

#### Document Types Supported:
| Type | Description |
|------|-------------|
| Syllabus | Course syllabus and outline |
| Final Exam | Final examination papers |
| Midterm Exam | Midterm examination papers |
| Assignments | Course assignments and homework |
| Projects | Project documentation |
| Other | Miscellaneous academic documents |

#### Submission Statuses:
| Status | Description | Visual Indicator |
|--------|-------------|------------------|
| UPLOADED | Document successfully submitted | Green |
| NOT_UPLOADED | Document not yet submitted | Gray |
| OVERDUE | Submission deadline passed | Red |

---

### 4. File Explorer

A sophisticated file browser system with:

- **Hierarchical Navigation** - Browse files by Academic Year > Semester > Department > Course > Professor
- **File Preview** - In-browser preview for multiple file types:
  - PDF documents
  - Images (JPEG, PNG, GIF, WebP)
  - Code files with syntax highlighting
  - Text files
  - Office documents (Word, Excel, PowerPoint)
- **Multi-File Upload** - Drag-and-drop interface for uploading multiple files
- **File Operations** - Download, preview, delete files
- **Grid/List View** - Toggle between visual layouts

---

### 5. Reporting System

#### Report Types:

**1. Professor Submission Report (HOD)**
- Track individual professor submissions
- Filter by course, document type, status
- View completion rates

**2. Department Submission Report**
- Summary statistics for the department
- Professor performance overview
- Completion and on-time rates

**3. System-Wide Report (Deanship/Admin)**
- University-wide statistics
- Department-by-department breakdown
- Trend analysis

#### Report Features:
- **PDF Export** - Professional PDF generation with:
  - University logo and header
  - Statistics tables with color coding
  - Detailed data tables
  - Status indicators
  - Legend and footer
- **Excel Export** - Data export for further analysis
- **Filtering** - Filter by semester, department, course, professor, document type, status
- **Visual Charts** - Interactive charts and graphs

---

### 6. Notification System

- **In-App Notifications** - Real-time notification dropdown
- **Document Requests** - HOD can request specific documents from professors
- **Status Updates** - Notifications for submission status changes
- **Badge Indicators** - Unread notification count display

---

### 7. Dashboard Widgets

Each role has customized dashboard widgets:

**Statistics Cards:**
- Total Documents
- Submitted Count
- Pending Count
- Overdue Count
- Completion Rate
- On-Time Rate

**Visual Elements:**
- Progress bars
- Pie charts
- Bar charts
- Trend indicators

---

### 8. Theme System

- **Light Mode** - Default bright theme
- **Dark Mode** - Eye-friendly dark theme
- **Persistent Preference** - Theme choice saved in localStorage
- **System Detection** - Auto-detect system theme preference
- **Smooth Transitions** - Animated theme switching

---

### 9. Responsive Design

- **Mobile-Friendly** - Responsive layouts for all screen sizes
- **Collapsible Sidebar** - Hidden on mobile, slide-out menu
- **Touch-Friendly** - Optimized for touch interactions
- **Adaptive Tables** - Scrollable tables on small screens

---

## Document Submission Workflow

```
1. Academic Year Created (Deanship)
        ↓
2. Semesters Configured (Deanship)
        ↓
3. Courses Assigned to Professors (Deanship)
        ↓
4. Professors Upload Documents (Professor)
        ↓
5. HOD Monitors Submissions (HOD)
        ↓
6. Reports Generated (All Roles)
        ↓
7. Documents Archived & Accessible (All Roles)
```

---

## File Organization Structure

```
uploads/
└── [Academic Year]/
    └── [Semester]/
        └── [Department]/
            └── [Course Code]/
                └── [Professor Name]/
                    └── [Document Type]/
                        └── files...
```

---

## Supported File Formats

| Category | Formats |
|----------|---------|
| Documents | PDF, DOC, DOCX |
| Spreadsheets | XLS, XLSX |
| Presentations | PPT, PPTX |
| Images | JPEG, JPG, PNG, GIF, WebP |
| Text | TXT, RTF |
| Code | JS, TS, PY, JAVA, HTML, CSS, JSON, XML |
| Archives | ZIP, RAR |

---

## Key Statistics Displayed

| Metric | Description | Roles |
|--------|-------------|-------|
| Total Professors | Number of registered professors | Admin, Deanship |
| Total Courses | Number of courses in the system | Admin, Deanship |
| Total Departments | Number of academic departments | Admin |
| Required Documents | Total documents expected | All |
| Submitted Documents | Documents successfully uploaded | All |
| Missing Documents | Documents not yet submitted | All |
| Overdue Documents | Past-due submissions | All |
| Completion Rate | Percentage of submitted documents | All |
| On-Time Rate | Percentage submitted before deadline | All |

---

## Project Information

**Institution:** Al-Quds University

**System Name:** Document Archiving System

**Developer:** Eng. Hamza Damra

**Supervisor:** Dr. Rushdi Hamamreh

**Year:** 2025-2026

---

## Technical Stack Summary

| Component | Technology |
|-----------|------------|
| Backend | Java 17, Spring Boot 3.x |
| Security | Spring Security, JWT |
| Database | MySQL 8.0 |
| Migrations | Flyway |
| Frontend | HTML5, Tailwind CSS, Vanilla JavaScript (ES6+) |
| Session Management | Spring Session JDBC |
| PDF Generation | iTextPDF 7.x (Backend), jsPDF (Frontend) |
| Charts | Chart.js |

---

*Last Updated: January 2026*
