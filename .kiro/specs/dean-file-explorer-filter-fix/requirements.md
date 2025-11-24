# Requirements Document

## Introduction

The Dean Dashboard File Explorer currently has two major issues:

1. **Filter Update Issue**: The File Explorer fails to update its displayed content when the Academic Year or Semester filters are changed. Users must manually refresh the entire page (F5) to see updated folders and files after changing filter selections.

2. **UI Layout Issue**: The File Explorer uses a tree view layout (left panel with tree structure, right panel with files) which is unnecessary for the Dean role and creates visual complexity. The Professor Dashboard uses a simpler, more intuitive layout without the tree view.

This creates a poor user experience and is inconsistent with the working behavior observed in the Professor Dashboard, where filter changes immediately update the File Explorer content and the layout is cleaner without the tree panel.

## Glossary

- **File Explorer**: A hierarchical file navigation component that displays folders and files
- **Tree View**: A left-side panel showing the folder hierarchy in an expandable/collapsible tree structure
- **Card View**: A layout where folders are displayed as clickable cards in the main content area
- **Academic Context**: The combination of a selected Academic Year and Semester that determines which data is displayed
- **Filter Change**: The action of selecting a different Academic Year or Semester from the dropdown filters
- **State Management**: The system for tracking and updating the current state of the File Explorer, including current path and displayed files
- **UI Reactivity**: The ability of the user interface to automatically update in response to data or state changes without requiring manual page refresh
- **Breadcrumb Navigation**: A navigation element showing the current path from root to current folder

## Requirements

### Requirement 1

**User Story:** As a Dean user, I want the File Explorer to automatically refresh when I change the Academic Year or Semester filters, so that I can view the correct data without manually refreshing the page.

#### Acceptance Criteria

1. WHEN a Dean user changes the Academic Year filter THEN the File Explorer SHALL clear all displayed content and reset to an empty state
2. WHEN a Dean user changes the Semester filter THEN the File Explorer SHALL load and display the folder structure for the newly selected semester
3. WHEN the File Explorer loads new data after a filter change THEN the system SHALL display folders and files corresponding to the selected Academic Year and Semester
4. WHEN a filter change occurs THEN the File Explorer SHALL reset the current path to root and clear breadcrumbs
5. WHEN both Academic Year and Semester are selected THEN the File Explorer SHALL display the root-level folder structure immediately without requiring additional user action

### Requirement 2

**User Story:** As a Dean user, I want the File Explorer state to be properly managed when filters change, so that I don't see stale or incorrect data from previous filter selections.

#### Acceptance Criteria

1. WHEN a filter change occurs THEN the system SHALL reset the File Explorer state including current node, current path, and breadcrumbs
2. WHEN the File Explorer state is reset THEN the system SHALL clear all navigation history to prevent showing incorrect states
3. WHEN new data is loaded after a filter change THEN the system SHALL update the File Explorer state with the new folder structure and current node
4. WHEN the File Explorer is reinitialized THEN the system SHALL preserve the File Explorer instance rather than creating a new instance for each filter change
5. WHEN state is reset THEN the system SHALL ensure no residual data from the previous context remains visible in the UI

### Requirement 3

**User Story:** As a Dean user, I want the File Explorer behavior to match the Professor Dashboard behavior, so that I have a consistent experience across different dashboard roles.

#### Acceptance Criteria

1. WHEN a Dean user changes filters THEN the File Explorer SHALL update its content using the same state management pattern as the Professor Dashboard
2. WHEN the File Explorer updates THEN the system SHALL call the state reset method before loading new data, matching the Professor Dashboard implementation
3. WHEN displaying the File Explorer THEN the system SHALL use the same FileExplorer component with role-specific configuration options
4. WHEN handling filter changes THEN the system SHALL follow the same sequence of operations: reset state, update context, load new data, render UI
5. WHEN errors occur during filter changes THEN the system SHALL display appropriate error messages consistent with the Professor Dashboard error handling

### Requirement 4

**User Story:** As a Dean user, I want clear visual feedback during filter changes, so that I understand when the File Explorer is loading new data.

#### Acceptance Criteria

1. WHEN a filter change is initiated THEN the system SHALL display a loading indicator in the File Explorer container
2. WHEN data is being loaded THEN the system SHALL disable user interactions with the File Explorer to prevent conflicting operations
3. WHEN data loading completes successfully THEN the system SHALL remove the loading indicator and enable user interactions
4. WHEN data loading fails THEN the system SHALL display an error message and remove the loading indicator
5. WHEN no Academic Year or Semester is selected THEN the system SHALL display a message prompting the user to select filters rather than showing stale data

### Requirement 5

**User Story:** As a Dean user, I want a simplified File Explorer layout without the tree view panel, so that I can navigate folders more easily and have a cleaner interface similar to the Professor Dashboard.

#### Acceptance Criteria

1. WHEN the Dean File Explorer is displayed THEN the system SHALL show folders as clickable cards in the main content area without a separate tree panel
2. WHEN a Dean user clicks on a folder card THEN the system SHALL navigate into that folder and display its contents
3. WHEN navigating through folders THEN the system SHALL display breadcrumb navigation at the top showing the current path
4. WHEN a Dean user clicks on a breadcrumb segment THEN the system SHALL navigate to that folder level
5. WHEN displaying the File Explorer THEN the system SHALL use a single-column layout matching the Professor Dashboard design without the tree view sidebar
