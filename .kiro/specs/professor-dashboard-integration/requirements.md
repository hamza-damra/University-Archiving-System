# Requirements Document

## Introduction

This document specifies the requirements for verifying and completing the Professor Dashboard integration with the semester-based Document Archiving System. The system must ensure that professors can view their assigned courses, upload documents, and browse files through a file explorer interface that is consistent with the Deanship's course assignments and file storage model. The verification will confirm that all backend APIs are properly connected to the frontend and that the data flow from Deanship course assignments to Professor dashboard is working correctly.

## Glossary

- **System**: The Document Archiving System
- **Professor Dashboard**: The web interface used by professors to view courses, upload documents, and browse files
- **Deanship Dashboard**: The administrative interface used to assign courses to professors
- **Course Assignment**: The association of a professor to a specific course within a semester, created by the Deanship
- **Document Type**: A categorized type of academic document (syllabus, exam, assignment, project_docs, lecture_notes, other)
- **Document Submission**: A record of files uploaded by a professor for a specific course and document type
- **File Explorer**: The hierarchical navigation interface for browsing the academic folder structure
- **Academic Year**: A period spanning multiple semesters (e.g., 2024-2025)
- **Semester**: One of three fixed periods within an academic year (FIRST, SECOND, SUMMER)
- **Authenticated User**: The currently logged-in professor user
- **Professor ID**: The unique identifier for a professor in the system

## Requirements

### Requirement 1: Professor Authentication and User Mapping

**User Story:** As a professor, I want to log in to the system and have my user account correctly mapped to my professor assignments, so that I can see my assigned courses.

#### Acceptance Criteria

1. WHEN a professor authenticates, THE System SHALL retrieve the user's professor ID from the User entity
2. THE System SHALL use the professor ID to query course assignments from the CourseAssignment table
3. WHERE the authenticated user does not have a professor ID, THE System SHALL return an empty course list
4. THE System SHALL maintain the professor's authentication session throughout their interaction with the dashboard
5. WHEN a professor's session expires, THE System SHALL redirect them to the login page

### Requirement 2: Course Assignment Retrieval

**User Story:** As a professor, I want to see all courses assigned to me for a selected semester, so that I know which courses I need to submit documents for.

#### Acceptance Criteria

1. WHEN a professor selects an academic year and semester, THE System SHALL query the CourseAssignment table for active assignments matching the professor ID, academic year, and semester
2. THE System SHALL return a list of courses with course code, course name, department, and semester information
3. WHERE no course assignments exist for the selected semester, THE System SHALL return an empty list
4. THE System SHALL display an empty state message when no courses are assigned
5. THE System SHALL include document submission status for each course in the response

### Requirement 3: Document Submission Status Display

**User Story:** As a professor, I want to see the submission status for each required document type in my courses, so that I know what documents I still need to upload.

#### Acceptance Criteria

1. FOR each assigned course, THE System SHALL retrieve all required document types from the RequiredDocumentType table
2. FOR each required document type, THE System SHALL determine the submission status (NOT_UPLOADED, UPLOADED, OVERDUE)
3. WHERE a document has been uploaded, THE System SHALL display the file count and upload timestamp
4. WHERE a document has a deadline, THE System SHALL display the deadline and time remaining
5. WHERE a deadline has passed and no document is uploaded, THE System SHALL mark the status as OVERDUE

### Requirement 4: File Upload Functionality

**User Story:** As a professor, I want to upload multiple files for a specific course and document type, so that I can fulfill my submission requirements.

#### Acceptance Criteria

1. WHEN a professor clicks the upload button for a document type, THE System SHALL open an upload modal with file selection
2. THE System SHALL validate that uploaded files are PDF or ZIP format only
3. THE System SHALL enforce maximum file count and total size limits defined in RequiredDocumentType
4. WHEN files pass validation, THE System SHALL store files in the physical location following the pattern: year/semester/professorId/courseCode/documentType
5. WHEN files are stored, THE System SHALL create or update a DocumentSubmission record with file metadata

### Requirement 5: File Replacement Functionality

**User Story:** As a professor, I want to replace previously uploaded files for a document type, so that I can correct mistakes or update submissions.

#### Acceptance Criteria

1. WHERE a professor has previously uploaded files for a document type, THE System SHALL display a "Replace Files" button
2. WHEN a professor clicks the replace button, THE System SHALL open an upload modal pre-populated with current submission information
3. WHEN replacement files are uploaded, THE System SHALL delete the old files from storage
4. WHEN replacement files are uploaded, THE System SHALL update the DocumentSubmission record with new file metadata
5. THE System SHALL record whether the replacement was submitted after the deadline

### Requirement 6: Professor File Explorer Integration

**User Story:** As a professor, I want to browse files through a file explorer interface, so that I can navigate the folder structure and download files.

#### Acceptance Criteria

1. WHEN a professor selects the File Explorer tab, THE System SHALL display the root node for the selected academic year and semester
2. THE System SHALL filter the file explorer to show only folders and files the professor has read access to
3. WHERE the professor has write access to a folder, THE System SHALL indicate this visually in the interface
4. WHEN a professor navigates through folders, THE System SHALL update breadcrumbs to show the current path
5. WHERE a professor has read access to a file, THE System SHALL provide a download button

### Requirement 7: Department-Scoped File Access

**User Story:** As a professor, I want to view files uploaded by other professors in my department, so that I can reference examples and maintain consistency.

#### Acceptance Criteria

1. THE System SHALL grant professors read access to files uploaded by other professors in the same department
2. THE System SHALL restrict professors from viewing files from other departments
3. WHERE a professor views another professor's folder, THE System SHALL display files in read-only mode
4. THE System SHALL prevent professors from uploading, replacing, or deleting files in other professors' folders
5. THE System SHALL clearly indicate in the UI which folders belong to the current professor versus other professors

### Requirement 8: Dashboard Overview Statistics

**User Story:** As a professor, I want to see an overview of my submission statistics for a semester, so that I can quickly assess my progress.

#### Acceptance Criteria

1. WHEN a professor views the Dashboard tab, THE System SHALL display total courses count for the selected semester
2. THE System SHALL display count of submitted documents across all courses
3. THE System SHALL display count of pending (not uploaded) documents
4. THE System SHALL display count of overdue documents
5. THE System SHALL provide a summary text describing the professor's overall status

### Requirement 9: Notification System Integration

**User Story:** As a professor, I want to receive notifications about document requests and deadlines, so that I stay informed about my obligations.

#### Acceptance Criteria

1. THE System SHALL display a notification badge when the professor has unseen notifications
2. WHEN a professor clicks the notification button, THE System SHALL display a dropdown with recent notifications
3. WHEN a professor clicks a notification, THE System SHALL mark it as seen
4. THE System SHALL poll for new notifications every 30 seconds
5. THE System SHALL sort notifications by creation date with newest first

### Requirement 10: Academic Year and Semester Selection

**User Story:** As a professor, I want to select different academic years and semesters, so that I can view historical data and plan for future semesters.

#### Acceptance Criteria

1. WHEN the professor dashboard loads, THE System SHALL populate the academic year dropdown with all available academic years
2. THE System SHALL auto-select the active academic year by default
3. WHEN a professor selects an academic year, THE System SHALL load the semesters for that year
4. WHEN a professor selects a semester, THE System SHALL reload all dashboard data for that semester
5. THE System SHALL persist the selected academic year and semester across tab switches

### Requirement 11: Empty State Handling

**User Story:** As a professor, I want to see clear messages when no data is available, so that I understand why the interface is empty.

#### Acceptance Criteria

1. WHERE no courses are assigned for the selected semester, THE System SHALL display "No courses assigned" message
2. WHERE no files exist in a file explorer folder, THE System SHALL display "No items found" message
3. WHERE no notifications exist, THE System SHALL display "No notifications" message
4. THE System SHALL distinguish between loading states and empty states with appropriate visual indicators
5. THE System SHALL provide helpful context in empty state messages

### Requirement 12: Error Handling and User Feedback

**User Story:** As a professor, I want to see clear error messages when operations fail, so that I can understand what went wrong and how to fix it.

#### Acceptance Criteria

1. WHEN an API call fails, THE System SHALL display a toast notification with an error message
2. WHEN file upload fails validation, THE System SHALL display specific validation errors in the upload modal
3. WHEN file upload fails due to server error, THE System SHALL display the error message and allow retry
4. THE System SHALL log errors to the browser console for debugging purposes
5. THE System SHALL handle network errors gracefully and inform the user to check their connection

### Requirement 13: File Download Functionality

**User Story:** As a professor, I want to download files from my submissions and the file explorer, so that I can review what has been uploaded.

#### Acceptance Criteria

1. WHEN a professor clicks a download button, THE System SHALL retrieve the file from storage
2. THE System SHALL verify the professor has read access to the file before allowing download
3. THE System SHALL serve the file with the correct filename and content type
4. WHEN download completes, THE System SHALL display a success toast notification
5. WHERE download fails, THE System SHALL display an error message

### Requirement 14: Responsive UI and Loading States

**User Story:** As a professor, I want the interface to provide visual feedback during data loading, so that I know the system is working.

#### Acceptance Criteria

1. WHEN data is being loaded, THE System SHALL display skeleton loading animations
2. WHEN file upload is in progress, THE System SHALL display a progress bar with percentage
3. THE System SHALL disable action buttons during operations to prevent duplicate submissions
4. WHEN operations complete, THE System SHALL re-enable buttons and update the UI
5. THE System SHALL provide smooth transitions between loading and loaded states

### Requirement 15: Data Consistency Between Deanship and Professor Views

**User Story:** As a system administrator, I want to ensure that course assignments created by the Deanship are immediately visible to professors, so that data is consistent across roles.

#### Acceptance Criteria

1. WHEN the Deanship creates a course assignment, THE System SHALL store it in the CourseAssignment table
2. WHEN a professor refreshes their dashboard, THE System SHALL query the latest course assignments from the database
3. THE System SHALL use the same CourseAssignment entity for both Deanship and Professor views
4. THE System SHALL ensure file paths generated by professors match the structure expected by the Deanship file explorer
5. WHERE a course assignment is deactivated by the Deanship, THE System SHALL hide it from the professor's course list
