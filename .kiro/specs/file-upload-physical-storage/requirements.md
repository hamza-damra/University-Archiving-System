# Requirements Document

## Introduction

This feature fixes the broken file upload functionality in the Deanship Archiving System and establishes a robust physical file storage system. Currently, when professors click the "Upload" button in the "Upload Lecture Notes" modal, nothing happens—no files are saved and the UI does not refresh. This feature will implement a complete end-to-end upload flow that stores files both physically on disk and in the database, ensuring the folder structure visible in the File Explorer corresponds exactly to the physical directory structure on the server.

## Glossary

- **Upload Modal**: The UI dialog that appears when professors click "Upload Lecture Notes" or similar upload buttons in their dashboard
- **Physical Storage**: The actual file system directories and files on the server disk
- **Base Storage Directory**: The root directory on the server where all archived files are stored (e.g., `/data/archive`)
- **Canonical Storage Structure**: The standardized directory hierarchy: `{yearCode}/{semesterType}/{professorId}/{courseCode} - {courseName}/{category}/`
- **Category Folder**: One of the standard course subfolders: Course Notes, Exams, Syllabus, or Assignments
- **File Metadata**: Database record containing information about an uploaded file (name, size, upload date, uploader, notes)
- **Multipart Request**: HTTP request format that allows file uploads along with form data
- **FileExplorerState**: The centralized JavaScript state management object for the File Explorer component
- **Idempotent Upload**: Upload operation that handles duplicate file names consistently (overwrite, version, or rename)

## Requirements

### Requirement 1: Establish Canonical Physical Storage Structure

**User Story:** As a system administrator, I want all uploaded files to be stored in a consistent physical directory structure on the server, so that files are organized, discoverable, and maintainable.

#### Acceptance Criteria

1. THE System SHALL define a single base storage directory for all archived files
2. THE System SHALL enforce the following directory hierarchy under the base path: `{yearCode}/{semesterType}/{professorId}/{courseCode} - {courseName}/{category}/`
3. WHEN a Folder entity is created in the database, THE System SHALL create the corresponding physical directory using `Files.createDirectories()`
4. THE System SHALL store the physical path in the Folder entity's `path` field
5. THE System SHALL ensure every Folder database record corresponds to an actual directory on disk

### Requirement 2: Implement Robust File Upload Endpoint

**User Story:** As a professor, I want to upload files to my course folders through a reliable backend endpoint, so that my files are safely stored and accessible.

#### Acceptance Criteria

1. THE System SHALL provide a file upload endpoint that accepts `multipart/form-data` requests
2. THE System SHALL accept the following upload parameters: `files[]` (one or multiple files), `notes` (optional text), and `folderId` (target folder identifier)
3. WHEN an upload request is received, THE System SHALL resolve the target Folder from the database using the provided `folderId`
4. THE System SHALL compute the physical directory path from the Folder's path field
5. THE System SHALL ensure the physical directory exists by calling `Files.createDirectories()` if needed
6. THE System SHALL save each uploaded file to the physical directory with a safe file name
7. THE System SHALL persist a File entity in the database for each uploaded file with metadata: file name, size, upload timestamp, uploader ID, related Folder, and notes
8. THE System SHALL return a JSON response with success status and metadata for newly uploaded files
9. IF the upload fails, THEN THE System SHALL return an error response with details

### Requirement 3: Enforce Upload Security and Permissions

**User Story:** As a system administrator, I want file uploads to be restricted by proper authorization rules, so that professors can only upload to their own folders and unauthorized access is prevented.

#### Acceptance Criteria

1. THE System SHALL verify that the authenticated user is a professor
2. THE System SHALL verify that the target folder belongs to the authenticated professor
3. THE System SHALL verify that the professor is assigned to the course associated with the folder
4. IF the user is a Dean or Admin, THEN THE System SHALL allow broader upload access
5. IF authorization fails, THEN THE System SHALL return a 403 Forbidden response

### Requirement 4: Wire Frontend Upload Modal to Backend

**User Story:** As a professor, I want the Upload button in the upload modal to actually upload my selected files, so that I can share course materials with students.

#### Acceptance Criteria

1. WHEN the professor clicks the Upload button, THE System SHALL read the currently selected folder from FileExplorerState
2. THE System SHALL construct a FormData object containing: selected files, notes text, and folderId
3. THE System SHALL send a POST request to the upload endpoint with `multipart/form-data` content type
4. WHILE uploading, THE System SHALL disable the Upload button and display "Uploading…" text
5. WHEN the upload succeeds, THE System SHALL close the modal, clear the file input and notes textarea, and refresh the current folder's file list
6. WHEN the upload succeeds, THE System SHALL display a success message: "Files uploaded successfully"
7. IF the upload fails, THEN THE System SHALL keep the modal open, display the error message, and re-enable the Upload button

### Requirement 5: Support Uploads Across All Course Categories

**User Story:** As a professor, I want to upload files to any of my course folders (Course Notes, Exams, Syllabus, Assignments), so that I can organize materials appropriately.

#### Acceptance Criteria

1. THE System SHALL support file uploads to all standard course category folders: Course Notes, Exams, Syllabus, and Assignments
2. THE System SHALL detect the selected folder category from the FileExplorerState
3. THE System SHALL display the appropriate modal title based on the category (e.g., "Upload Lecture Notes", "Upload Exams")
4. THE System SHALL send uploads to the same generic endpoint with the correct folderId for the selected category
5. THE System SHALL store uploaded files in the physical directory matching the selected category

### Requirement 6: Maintain Backend-Frontend Structure Synchronization

**User Story:** As a system user, I want the folder structure I see in the File Explorer to exactly match the physical directories on the server, so that the system is consistent and reliable.

#### Acceptance Criteria

1. WHEN a Dean assigns a course to a professor, THE System SHALL create the physical folder structure: `{base}/{yearCode}/{semesterType}/{professorId}/{courseCode} - {courseName}/{Syllabus, Exams, Course Notes, Assignments}`
2. WHEN a Dean assigns a course, THE System SHALL make the folder structure visible in both Dean and Professor File Explorer trees
3. WHEN a professor uploads files to a category folder, THE System SHALL store files physically in the corresponding directory
4. WHEN a professor uploads files, THE System SHALL create File database records linked to the correct Folder
5. WHEN a professor uploads files, THE System SHALL refresh the frontend file list to display the new files
6. WHEN a Dean views the File Explorer, THE System SHALL display the same uploaded files under the course's category folder

### Requirement 7: Handle Duplicate File Names Consistently

**User Story:** As a professor, I want the system to handle duplicate file names predictably when I upload files, so that I don't accidentally lose data or create confusion.

#### Acceptance Criteria

1. THE System SHALL define a policy for handling duplicate file names: overwrite, versioning, or rename
2. WHEN a file with the same name already exists in the target folder, THE System SHALL apply the defined policy
3. IF the policy is "overwrite", THEN THE System SHALL replace the existing file and update the database record
4. IF the policy is "rename", THEN THE System SHALL append a unique suffix to the new file name (e.g., `file(1).pdf`)
5. THE System SHALL log all file name collision resolutions

### Requirement 8: Provide Upload Progress and Feedback

**User Story:** As a professor, I want clear feedback during file uploads, so that I know the upload is progressing and when it completes.

#### Acceptance Criteria

1. WHILE files are uploading, THE System SHALL display a loading state on the Upload button
2. WHILE files are uploading, THE System SHALL disable the Upload button to prevent duplicate submissions
3. WHEN the upload completes successfully, THE System SHALL display a success toast notification
4. IF the upload fails, THEN THE System SHALL display an error message with details
5. THE System SHALL not cause layout shifts or visual glitches during upload operations

### Requirement 9: Validate Uploaded Files

**User Story:** As a system administrator, I want uploaded files to be validated for safety and appropriateness, so that malicious or inappropriate files are rejected.

#### Acceptance Criteria

1. THE System SHALL validate file size does not exceed a maximum limit (e.g., 50MB per file)
2. THE System SHALL validate file types against an allowed list (e.g., PDF, DOCX, PPTX, XLSX, images)
3. THE System SHALL reject files with disallowed extensions
4. THE System SHALL sanitize file names to remove special characters that could cause file system issues
5. IF validation fails, THEN THE System SHALL return a clear error message indicating the reason

### Requirement 10: Code Quality and Testing

**User Story:** As a developer, I want well-tested and maintainable upload code, so that the feature is reliable and easy to extend.

#### Acceptance Criteria

1. THE System SHALL implement clean, modular Java code for the upload endpoint with proper separation of concerns
2. THE System SHALL implement modular JavaScript code for the upload modal with reusable functions
3. THE System SHALL include unit tests for file upload service methods
4. THE System SHALL include integration tests for the upload endpoint
5. THE System SHALL include comments explaining non-trivial logic, especially for file storage and validation
