# Requirements Document

## Introduction

This feature enhances the Deanship Archiving System's File Explorer module to provide automatic folder provisioning, global synchronization across dashboards, improved UI stability, and centralized state management. The system will automatically create appropriate folder structures when professors are created or assigned to courses, eliminate UI layout glitches during data loading, and ensure consistent behavior across Dean, Professor, and other dashboard views.

## Glossary

- **File Explorer**: The shared UI component used across multiple dashboards (Dean, Professor, HOD) to browse and manage folders and files within the archiving system
- **Academic Year**: A time period identifier (e.g., "2024-2025") used to scope file storage and organization
- **Semester**: A subdivision of an Academic Year (e.g., "First Semester", "Second Semester") used to further scope file storage
- **Professor Folder**: A root-level folder created for each professor within a specific Academic Year and Semester context
- **Course Folder**: A nested folder structure created under a Professor Folder when a course is assigned, containing standard subfolders (Syllabus, Exams, Course Notes, Assignments)
- **Folder Tree**: The hierarchical structure of folders displayed in the left panel of the File Explorer
- **Layout Shift**: An undesirable visual glitch where UI elements temporarily move out of position during data loading
- **FileExplorerState**: A centralized JavaScript state management object that holds current selections, data, and loading status for the File Explorer
- **Idempotent Operation**: An operation that produces the same result regardless of how many times it is executed
- **Dashboard Context**: The combination of selected Academic Year and Semester that determines which data is displayed

## Requirements

### Requirement 1: Automatic Professor Folder Creation

**User Story:** As a Dean, I want professor folders to be automatically created in the File Explorer when I create a new professor account, so that the folder structure is ready immediately without manual setup.

#### Acceptance Criteria

1. WHEN the Dean creates a new professor from the Professors Management page, THE System SHALL create a root folder for that professor in the currently selected Academic Year and Semester
2. WHEN a professor folder is created, THE System SHALL make the folder immediately visible in the File Explorer's Folder Structure tree without requiring a page reload
3. IF a professor folder already exists for the given professor in the current Academic Year and Semester, THEN THE System SHALL not create a duplicate folder
4. THE System SHALL persist the professor folder creation in the backend database and file system
5. WHEN the folder creation fails, THE System SHALL display an error message to the Dean and log the failure details

### Requirement 2: Automatic Course Folder Structure Creation

**User Story:** As a Dean, I want course folder structures to be automatically created when I assign a professor to a course, so that professors have organized spaces for their course materials immediately.

#### Acceptance Criteria

1. WHEN the Dean assigns a professor to a course from the Assignments section, THE System SHALL create a nested folder structure under the professor's folder in the currently selected Academic Year and Semester
2. THE System SHALL create the following standard subfolder hierarchy: `<Professor Name>/<CourseCode> - <CourseName>/Syllabus`, `Exams`, `Course Notes`, and `Assignments`
3. IF the course folder already exists for that professor in the current Academic Year and Semester, THEN THE System SHALL not create duplicate folders
4. IF any standard subfolder already exists, THEN THE System SHALL not recreate that specific subfolder
5. WHEN course folders are created, THE System SHALL refresh the File Explorer to display the new folder structure without breaking the layout

### Requirement 3: Global File Explorer Synchronization

**User Story:** As a system user (Dean, Professor, or HOD), I want the File Explorer to show consistent data across all dashboards, so that changes made in one view are reflected in all other views.

#### Acceptance Criteria

1. WHEN a folder is created in the Dean dashboard, THE System SHALL make that folder visible in the Professor dashboard and all other relevant dashboards upon reload
2. WHEN a file is uploaded in the Professor dashboard, THE System SHALL make that file visible in the Dean dashboard and all other relevant dashboards upon reload
3. WHEN a folder or file is renamed in any dashboard, THE System SHALL reflect the new name in all other dashboards upon reload
4. WHEN a folder or file is deleted in any dashboard, THE System SHALL remove it from all other dashboards upon reload
5. THE System SHALL use shared backend APIs for all folder and file operations to ensure consistency

### Requirement 4: Elimination of UI Layout Shifts During Loading

**User Story:** As a system user, I want the File Explorer to maintain stable layout during data loading, so that I don't experience disorienting visual glitches when the interface updates.

#### Acceptance Criteria

1. WHEN the File Explorer loads data, THE System SHALL maintain the container size and layout without removing DOM elements
2. WHILE the File Explorer is loading data, THE System SHALL display a loading indicator or skeleton placeholder within the File Explorer area
3. WHEN the Academic Year or Semester selection changes, THE System SHALL update only the File Explorer section without causing full page reloads
4. THE System SHALL apply stable loading behavior to all instances of the File Explorer across Dean, Professor, and HOD dashboards
5. WHEN folder or file operations complete, THE System SHALL update the display without causing visible element repositioning

### Requirement 5: Centralized State Management for File Explorer

**User Story:** As a developer, I want a centralized state management system for the File Explorer, so that UI behavior is consistent and predictable across all interactions.

#### Acceptance Criteria

1. THE System SHALL implement a FileExplorerState object that stores selected Academic Year, Semester, professor, course, folder tree data, file list, and loading/error flags
2. WHEN any File Explorer action occurs (select folder, load tree, create folder), THE System SHALL update the FileExplorerState through defined functions
3. WHEN the FileExplorerState changes, THE System SHALL update all relevant UI components to reflect the new state
4. THE System SHALL use the shared FileExplorerState in both Dean and Professor dashboards to ensure consistent behavior
5. WHEN users perform rapid interactions (e.g., quickly switching folders), THE System SHALL handle state updates without causing UI flickering or inconsistencies

### Requirement 6: Enhanced Folder Structure Visual Design

**User Story:** As a system user, I want larger and clearer folder items in the File Explorer's left panel, so that I can more easily navigate the folder structure.

#### Acceptance Criteria

1. THE System SHALL increase the row height and padding of folder items in the Folder Structure tree
2. THE System SHALL increase the font size of folder names to improve readability
3. THE System SHALL increase the folder icon size proportionally with the text
4. THE System SHALL maintain a clean, minimal design consistent with the existing dashboard aesthetic
5. THE System SHALL ensure the enhanced design remains responsive and functional on different screen sizes

### Requirement 7: Academic Year and Semester Context Enforcement

**User Story:** As a Dean, I want all File Explorer data to respect my selected Academic Year and Semester, so that I only see relevant files and folders for the current context.

#### Acceptance Criteria

1. THE System SHALL filter all File Explorer data by the selected Academic Year and Semester from the dashboard dropdowns
2. WHEN the Dean changes the Academic Year or Semester selection, THE System SHALL update the FileExplorerState and reload the folder tree and files for the new context
3. WHEN the Dean changes the Academic Year or Semester selection, THE System SHALL update the display without breaking the layout
4. THE System SHALL display breadcrumbs that accurately reflect the selected Academic Year, Semester, Professor, and Course
5. THE System SHALL ensure that folder and file operations (create, upload, delete) are scoped to the currently selected Academic Year and Semester

### Requirement 8: Code Quality and Maintainability

**User Story:** As a developer, I want well-structured and documented code for the File Explorer enhancements, so that the system is maintainable and extensible.

#### Acceptance Criteria

1. THE System SHALL preserve all existing working functionality during refactoring
2. THE System SHALL implement clean, modular Java code for backend services with clear separation of concerns
3. THE System SHALL implement modular JavaScript code for frontend components with reusable functions
4. THE System SHALL include comments explaining non-trivial logic, especially for folder auto-creation and state management
5. WHERE existing unit tests or integration tests exist, THE System SHALL extend them to cover new functionality
