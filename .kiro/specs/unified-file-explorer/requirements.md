# Requirements Document

## Introduction

This specification defines the requirements for unifying the File Explorer UI across all three dashboards (Deanship, HOD, and Professor) in the Al-Quds University Document Archiving System. Currently, each dashboard implements its own File Explorer with different layouts, styles, and UI patterns. The goal is to standardize the File Explorer component using the Professor Dashboard's File Explorer as the master design template, while maintaining role-specific functionality and permissions.

## Glossary

- **File Explorer**: A hierarchical file navigation component that allows users to browse academic years, semesters, professors, courses, document types, and files
- **Professor Dashboard**: The dashboard interface used by professors to manage their course documents and submissions
- **HOD Dashboard**: The dashboard interface used by Heads of Department to view department-wide submission status (read-only access)
- **Deanship Dashboard**: The dashboard interface used by Deanship staff to manage the entire archiving system across all departments
- **Master Design**: The Professor Dashboard File Explorer UI that serves as the canonical reference for visual design and layout
- **Breadcrumb Navigation**: A navigation pattern showing the current location in the file hierarchy (e.g., Home / 2024-2025 / first / PBUS001)
- **Folder Card**: A visual card component displaying a folder with an icon, title, and metadata
- **Academic Year Selector**: A dropdown control for selecting the academic year to browse
- **Semester Selector**: A dropdown control for selecting the semester within an academic year
- **Role-Specific Label**: Small text or badge indicating context specific to the user's role (e.g., "Your Folder", "Read-only")

## Requirements

### Requirement 1: Unified Visual Design

**User Story:** As a system user, I want all File Explorer interfaces to have the same visual appearance, so that I have a consistent experience regardless of which dashboard I'm using.

#### Acceptance Criteria

1. WHEN a user views the File Explorer in any dashboard, THE System SHALL display the same base HTML structure and Tailwind CSS classes as the Professor Dashboard File Explorer
2. WHEN a user views folder cards in any dashboard, THE System SHALL render them using the same blue card design with folder icon and title as the Professor Dashboard
3. WHEN a user views file lists in any dashboard, THE System SHALL display them using the same table layout with columns for name, size, uploaded date, and actions as the Professor Dashboard
4. WHEN a user views the breadcrumb navigation in any dashboard, THE System SHALL display it using the same visual style and typography as the Professor Dashboard
5. WHEN a user views the Academic Year and Semester selectors in any dashboard, THE System SHALL display them using the same Tailwind classes and positioning as the Professor Dashboard

### Requirement 2: Consistent Breadcrumb Behavior

**User Story:** As a system user, I want breadcrumb navigation to work the same way in all dashboards, so that I can easily understand my current location and navigate back through the hierarchy.

#### Acceptance Criteria

1. WHEN a user navigates through folders, THE System SHALL update the breadcrumb path to show the current location in the format "Home → academic year → semester → professor → course → document type"
2. WHEN a user clicks on a breadcrumb segment, THE System SHALL navigate to that level in the hierarchy
3. WHEN the breadcrumb path exceeds the available width, THE System SHALL provide horizontal scrolling while maintaining visual consistency
4. WHEN a user is at the root level, THE System SHALL display a home icon in the breadcrumb
5. WHEN the current location changes, THE System SHALL highlight the current breadcrumb segment using the same styling as the Professor Dashboard

### Requirement 3: Role-Specific Behavior Preservation

**User Story:** As a system administrator, I want each dashboard to maintain its existing permissions and data filtering, so that users only see the data they are authorized to access.

#### Acceptance Criteria

1. WHEN a Deanship user views the File Explorer, THE System SHALL display all academic years, semesters, professors, courses, and document types across all departments
2. WHEN an HOD user views the File Explorer, THE System SHALL display only professors and courses within their department with read-only access
3. WHEN a Professor user views the File Explorer, THE System SHALL display only their own courses and folders with read-write access
4. WHEN a user attempts to access a file, THE System SHALL enforce the existing permission checks without modification
5. WHEN a user views folders, THE System SHALL apply the existing data filtering logic based on their role

### Requirement 4: Role-Specific Visual Indicators

**User Story:** As a system user, I want to see small visual indicators that help me understand my access level and context, so that I know what actions I can perform.

#### Acceptance Criteria

1. WHEN a Professor views their own course folder, THE System SHALL display a "Your Folder" label on the folder card
2. WHEN an HOD views the File Explorer, THE System SHALL display a "Browse department files (Read-only)" message in the header area
3. WHEN a Deanship user views a professor's folder, THE System SHALL display the professor's name as a subtitle on the folder card
4. WHEN a user has read-only access to a folder, THE System SHALL display action buttons using the same visual style but with appropriate disabled states or hidden upload buttons
5. WHERE a role-specific label is displayed, THE System SHALL use the same badge and text styling as the Professor Dashboard

### Requirement 5: Shared Component Architecture

**User Story:** As a developer, I want the File Explorer to be implemented as a reusable component, so that changes to the layout can be made in one place and automatically apply to all dashboards.

#### Acceptance Criteria

1. THE System SHALL implement the File Explorer using the existing FileExplorer class in file-explorer.js
2. WHEN the FileExplorer class is instantiated, THE System SHALL accept configuration options for role-specific behavior (readOnly, permissions, etc.)
3. WHEN rendering the File Explorer, THE System SHALL use the same HTML template structure across all dashboard implementations
4. WHEN the File Explorer component is updated, THE System SHALL automatically reflect changes in all three dashboards without requiring separate modifications
5. THE System SHALL document in code comments which component serves as the canonical File Explorer layout

### Requirement 6: Consistent Empty States and Loading States

**User Story:** As a system user, I want to see consistent feedback when folders are empty or data is loading, so that I understand the system state.

#### Acceptance Criteria

1. WHEN a folder contains no items, THE System SHALL display an empty state message with an icon using the same design as the Professor Dashboard
2. WHEN data is being loaded, THE System SHALL display a loading indicator using the same animation and styling as the Professor Dashboard
3. WHEN an error occurs, THE System SHALL display an error message using the same visual treatment as the Professor Dashboard
4. WHEN the user has not yet selected a semester, THE System SHALL display a "Select a semester to browse files" message in the same style across all dashboards
5. THE System SHALL use the same icon library and color scheme for all state indicators across all dashboards

### Requirement 7: Consistent Folder and File Row Design

**User Story:** As a system user, I want folders and files to be displayed in the same way across all dashboards, so that I can quickly recognize and interact with them.

#### Acceptance Criteria

1. WHEN displaying course folders, THE System SHALL use wide blue cards with a folder icon, course code, course name, and hover effects matching the Professor Dashboard
2. WHEN displaying document type folders, THE System SHALL use the same card design with appropriate icons and labels
3. WHEN displaying professor folders in HOD or Deanship views, THE System SHALL use the same card design with professor name and department information
4. WHEN displaying files in a table, THE System SHALL use the same column layout, typography, and row hover effects as the Professor Dashboard
5. WHEN displaying action buttons (Download, View, Upload), THE System SHALL use the same button styles, icons, and positioning as the Professor Dashboard

### Requirement 8: Synchronized Selector Behavior

**User Story:** As a system user, I want the Academic Year and Semester selectors to work consistently across all dashboards, so that I can easily switch between different time periods.

#### Acceptance Criteria

1. WHEN a user selects an academic year, THE System SHALL load the available semesters for that year using the same interaction pattern across all dashboards
2. WHEN a user selects a semester, THE System SHALL update the File Explorer content to show folders for that semester
3. WHEN the selectors are displayed, THE System SHALL use the same label positioning, dropdown styling, and spacing as the Professor Dashboard
4. WHEN no academic year is selected, THE System SHALL disable the semester selector with the same visual treatment across all dashboards
5. WHEN the active academic year is loaded, THE System SHALL auto-select it in the dropdown using the same logic across all dashboards

### Requirement 9: Backward Compatibility

**User Story:** As a system administrator, I want the unified File Explorer to maintain all existing functionality, so that no features are lost during the migration.

#### Acceptance Criteria

1. WHEN the unified File Explorer is deployed, THE System SHALL maintain all existing API endpoints without modification
2. WHEN users interact with the File Explorer, THE System SHALL use the existing backend routing and permission logic
3. WHEN files are downloaded, THE System SHALL use the existing download mechanism without changes
4. WHEN the File Explorer is loaded, THE System SHALL use the existing data fetching methods from the api.js module
5. THE System SHALL preserve all existing event handlers and callback functions for file operations

### Requirement 10: Code Organization and Documentation

**User Story:** As a developer, I want the File Explorer code to be well-organized and documented, so that future maintenance and enhancements are straightforward.

#### Acceptance Criteria

1. THE System SHALL include code comments identifying the FileExplorer class as the canonical File Explorer layout
2. WHEN the FileExplorer class is modified, THE System SHALL update documentation to reflect changes
3. THE System SHALL organize File Explorer-related code in the file-explorer.js module with clear separation of concerns
4. THE System SHALL document role-specific configuration options in the FileExplorer constructor
5. THE System SHALL include inline comments explaining how role-specific variations are implemented without breaking the unified appearance
