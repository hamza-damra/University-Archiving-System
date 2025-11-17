# Requirements Document

## Introduction

This document specifies the requirements for refactoring the existing Document Archiving System from a request-based model to a semester- and folder-based file exploration system. The system will support three distinct roles (Deanship, HOD, Professor) with a hierarchical academic structure (Year → Semester → Professor → Course → Document Type → Files). The refactoring maintains the existing Spring Boot backend and HTML/CSS/JavaScript/Tailwind frontend technology stack while introducing comprehensive academic year management, role-based permissions, and a file explorer interface.

## Glossary

- **System**: The Document Archiving System
- **Deanship**: The administrative role with global management authority over academic structure, professors, and courses
- **HOD**: Head of Department role with department-scoped read access to files and submission status
- **Professor**: Faculty member role with upload permissions for assigned courses and read access to department files
- **Academic Year**: A period spanning multiple semesters (e.g., 2024-2025)
- **Semester**: One of three fixed periods within an academic year (First/Fall, Second/Spring, Summer)
- **Course Assignment**: The association of a professor to a specific course within a semester
- **Document Type**: A categorized type of academic document (syllabus, exam, assignment, project_docs, etc.)
- **Required Document Type**: A document type that must be submitted for a specific course in a semester
- **File Explorer**: The hierarchical navigation interface for browsing the academic folder structure
- **Department**: An organizational unit within the university to which professors and courses belong

## Requirements

### Requirement 1: Role-Based Authentication and Authorization

**User Story:** As a system administrator, I want three distinct roles with specific permissions, so that users can only access and perform actions appropriate to their role.

#### Acceptance Criteria

1. THE System SHALL implement three distinct user roles: Deanship, HOD, and Professor
2. WHEN a user authenticates, THE System SHALL assign exactly one role to the authenticated session
3. THE System SHALL enforce role-based access control on all API endpoints and UI components
4. THE System SHALL prevent users from accessing features not authorized for their assigned role
5. WHEN a user attempts an unauthorized action, THE System SHALL return an access denied response

### Requirement 2: Academic Structure Management by Deanship

**User Story:** As a Deanship user, I want to manage the academic structure including years, semesters, and courses, so that the system reflects the current academic organization.

#### Acceptance Criteria

1. WHERE the user has Deanship role, THE System SHALL provide interfaces to create and edit academic years
2. WHEN a Deanship user creates an academic year, THE System SHALL automatically create three semester containers (First, Second, Summer) for that year
3. WHERE the user has Deanship role, THE System SHALL provide interfaces to create and edit courses with attributes including code, name, department, and level
4. WHEN a Deanship user assigns a course to a professor for a semester, THE System SHALL create a course assignment record linking the professor, course, and semester
5. WHERE the user has Deanship role, THE System SHALL provide interfaces to define required document types per course with optional deadlines

### Requirement 3: Professor Management by Deanship

**User Story:** As a Deanship user, I want to manage professor records centrally, so that professor information is consistent and HODs cannot create conflicting records.

#### Acceptance Criteria

1. WHERE the user has Deanship role, THE System SHALL provide interfaces to create, edit, and deactivate professor records
2. WHEN a Deanship user creates a professor record, THE System SHALL store core information including ID, name, email, and department
3. WHERE the user has HOD role, THE System SHALL prevent access to professor creation, editing, or deletion interfaces
4. THE System SHALL retain historical professor data when a professor is deactivated
5. WHEN a professor is deactivated, THE System SHALL prevent new course assignments to that professor while preserving existing assignment history

### Requirement 4: Hierarchical Folder Structure

**User Story:** As any system user, I want files organized in a consistent hierarchical structure, so that I can easily locate documents by year, semester, professor, course, and type.

#### Acceptance Criteria

1. THE System SHALL organize all files in a hierarchy following the pattern: Year → Semester → Professor → Course → Document Type → Files
2. WHEN a file is uploaded, THE System SHALL store metadata including year, semester, professor, course, document type, filename, size, upload timestamp, uploader, and notes
3. THE System SHALL create logical folder containers in the database corresponding to each level of the hierarchy
4. THE System SHALL generate unique identifiers for professor folders based on professor ID
5. THE System SHALL enforce that document types come from a controlled list including syllabus, exam, assignment, project_docs, lecture_notes, and other

### Requirement 5: File Explorer Interface for All Roles

**User Story:** As any system user, I want a file explorer interface to navigate the folder hierarchy, so that I can browse and access files according to my permissions.

#### Acceptance Criteria

1. THE System SHALL provide a file explorer interface with year and semester selection controls
2. WHEN a user selects a year and semester, THE System SHALL display the hierarchical structure of professors, courses, and document types for that selection
3. THE System SHALL display file listings with columns for filename, size, upload date/time, uploader, and notes
4. THE System SHALL provide breadcrumb navigation showing the current location in the hierarchy
5. WHERE a user has read permission for a file, THE System SHALL provide view and download actions for that file

### Requirement 6: Deanship File Explorer and Reporting

**User Story:** As a Deanship user, I want to browse all files and view system-wide reports, so that I can monitor submission status across all departments and semesters.

#### Acceptance Criteria

1. WHERE the user has Deanship role, THE System SHALL grant read access to all folders and files in the hierarchy
2. WHERE the user has Deanship role, THE System SHALL provide reports summarizing submission status by semester and department
3. WHEN a Deanship user views a semester report, THE System SHALL display which courses exist, which professors are assigned, and which required documents are submitted or missing
4. WHERE the user has Deanship role, THE System SHALL provide file preview functionality for supported file types
5. WHERE the user has Deanship role, THE System SHALL provide file download functionality for all files

### Requirement 7: HOD Department-Scoped Access

**User Story:** As an HOD user, I want to view submission status and files for my department only, so that I can monitor my department's compliance without managing professors directly.

#### Acceptance Criteria

1. WHERE the user has HOD role, THE System SHALL restrict file explorer access to professors and courses belonging to the HOD's department
2. WHERE the user has HOD role, THE System SHALL provide read-only access to all files within the department scope
3. WHEN an HOD user views the dashboard, THE System SHALL display overview widgets showing total professors, total courses, and submission status for the selected semester
4. WHERE the user has HOD role, THE System SHALL provide a professor submission status list showing professor, course, required document types, and status (Submitted/Missing/Overdue)
5. WHERE the user has HOD role, THE System SHALL prevent file deletion, editing, or upload actions on any files

### Requirement 8: Professor File Upload and Management

**User Story:** As a Professor user, I want to upload required documents to my assigned courses, so that I can fulfill my submission obligations.

#### Acceptance Criteria

1. WHERE the user has Professor role, THE System SHALL display all courses assigned to that professor for the selected semester
2. WHEN a Professor user views an assigned course, THE System SHALL display all required document types with their submission status and deadlines
3. WHERE a document type has not been uploaded, THE System SHALL provide an upload button for that document type
4. WHEN a Professor user uploads files, THE System SHALL accept multiple PDF and ZIP files with optional notes
5. WHERE a Professor user has previously uploaded files for a document type, THE System SHALL provide a replace files button allowing file replacement according to deadline rules

### Requirement 9: Professor Read Access to Department Files

**User Story:** As a Professor user, I want to view files uploaded by other professors in my department, so that I can reference examples and maintain consistency.

#### Acceptance Criteria

1. WHERE the user has Professor role, THE System SHALL grant read access to files of other professors in the same department and semester
2. WHERE the user has Professor role, THE System SHALL prevent upload, replace, or delete actions on files uploaded by other professors
3. WHEN a Professor user navigates to another professor's folder, THE System SHALL display file listings with view and download actions only
4. WHERE the user has Professor role, THE System SHALL grant full upload and replace permissions only for folders corresponding to courses assigned to that professor
5. THE System SHALL clearly indicate in the UI which folders are owned by the current professor versus other professors

### Requirement 10: Semester-Based Professor Dashboard

**User Story:** As a Professor user, I want a dashboard organized by semester and course, so that I can quickly see my submission obligations and status.

#### Acceptance Criteria

1. WHEN a Professor user accesses the dashboard, THE System SHALL provide academic year and semester selection controls
2. WHEN a Professor user selects a semester, THE System SHALL display all courses assigned to that professor for that semester
3. FOR each assigned course, THE System SHALL display all required document types with status indicators (Not uploaded/Uploaded/Overdue)
4. FOR each required document type, THE System SHALL display the deadline if defined
5. FOR each document type, THE System SHALL provide a clear upload or replace files button that opens the upload modal

### Requirement 11: File Upload Validation and Storage

**User Story:** As a Professor user, I want the system to validate my file uploads and store them correctly, so that my submissions are properly recorded.

#### Acceptance Criteria

1. WHEN a Professor user uploads files, THE System SHALL validate that file types are limited to PDF and ZIP formats
2. WHEN a Professor user uploads files, THE System SHALL enforce maximum file size limits per file and per upload batch
3. WHEN files pass validation, THE System SHALL store files in the physical location corresponding to the logical folder path (year/semester/professorId/course/documentType)
4. WHEN files are stored, THE System SHALL create database records with complete metadata including year, semester, course, professor, document type, filename, size, upload timestamp, uploader, and notes
5. WHEN file upload completes successfully, THE System SHALL update the submission status for that document type to "Uploaded"

### Requirement 12: Submission Status Tracking and Deadlines

**User Story:** As any system user, I want to see accurate submission status including deadline compliance, so that I can identify missing or overdue submissions.

#### Acceptance Criteria

1. WHERE a required document type has a defined deadline, THE System SHALL calculate submission status as Submitted on time, Submitted late, or Overdue based on upload timestamp and deadline
2. WHERE a required document type has no defined deadline, THE System SHALL calculate submission status as Submitted or Missing based on file existence
3. WHEN the current date/time exceeds a deadline and no files are uploaded, THE System SHALL mark that document type as Overdue
4. THE System SHALL display submission status consistently across all interfaces including dashboards, file explorers, and reports
5. WHEN a Professor user uploads files after a deadline, THE System SHALL record the submission as late but still accept the files

### Requirement 13: HOD Reporting Based on Academic Structure

**User Story:** As an HOD user, I want reports based on the semester structure showing submission compliance, so that I can identify professors who need follow-up.

#### Acceptance Criteria

1. WHERE the user has HOD role, THE System SHALL provide a professor submission report filtered by academic year and semester
2. WHEN an HOD user generates a report, THE System SHALL include columns for professor, course, required document types, and status for each document type
3. WHERE the user has HOD role, THE System SHALL provide filtering options by semester, course, document type, and status
4. WHERE the user has HOD role, THE System SHALL provide export functionality to generate PDF reports
5. WHEN a report is exported, THE System SHALL include summary statistics showing total required submissions, submitted count, missing count, and overdue count

### Requirement 14: Navigation and UI Updates for Three Roles

**User Story:** As any system user, I want navigation appropriate to my role, so that I can access my authorized features efficiently.

#### Acceptance Criteria

1. WHEN a Deanship user logs in, THE System SHALL display navigation to dashboard, professor management, academic structure management, course assignments, and file explorer
2. WHEN an HOD user logs in, THE System SHALL display navigation to dashboard, submission status, reports, and file explorer
3. WHEN a Professor user logs in, THE System SHALL display navigation to dashboard, my courses, file explorer, and notifications
4. THE System SHALL hide navigation items for features not authorized for the current user's role
5. THE System SHALL display the current user's role and name in the navigation header

### Requirement 15: Data Migration from Request-Based to Semester-Based Model

**User Story:** As a system administrator, I want existing data migrated to the new structure, so that historical submissions are preserved and accessible.

#### Acceptance Criteria

1. THE System SHALL provide a migration process that converts existing document request records to the new academic structure model
2. WHEN migration executes, THE System SHALL create academic year and semester records corresponding to existing request dates
3. WHEN migration executes, THE System SHALL create course assignment records linking professors to courses based on existing requests
4. WHEN migration executes, THE System SHALL convert uploaded files to the new folder structure and update database metadata accordingly
5. WHEN migration completes, THE System SHALL preserve all historical file uploads with their original upload timestamps and metadata
