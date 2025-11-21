# Requirements Document

## Introduction

This document specifies the requirements for refactoring the Deanship Dashboard from a single-page tabbed interface into a multi-page application with dedicated routes for each functional area. The refactor aims to improve navigation, maintainability, and user experience by separating concerns into distinct pages while preserving all existing functionality.

## Glossary

- **Deanship Dashboard**: The administrative interface for Al-Quds University deans to manage academic data
- **System**: The Al-Quds University Archiving System
- **Tab**: A section of the current single-page interface (Academic Years, Professors, Courses, etc.)
- **Route**: A URL path that maps to a specific page in the application
- **Shared Layout**: Common UI elements (header, navigation, filters) that appear across all pages
- **Academic Context**: The selected academic year and semester that filter data across the application
- **Navigation Bar**: The horizontal menu that allows users to switch between different sections
- **Main Dashboard**: The new landing page that provides overview and quick access to all sections

## Requirements

### Requirement 1: Multi-Page Architecture

**User Story:** As a dean, I want each functional area to have its own dedicated page, so that I can navigate directly to specific sections and bookmark them.

#### Acceptance Criteria

1. WHEN the System loads, THE System SHALL render a main dashboard page at the route `/deanship/dashboard`
2. THE System SHALL provide dedicated routes for each functional area:
   - `/deanship/academic-years` for Academic Years Management
   - `/deanship/professors` for Professors Management
   - `/deanship/courses` for Courses Management
   - `/deanship/course-assignments` for Course Assignments
   - `/deanship/reports` for Reports
   - `/deanship/file-explorer` for File Explorer
3. WHEN a user navigates to any deanship route, THE System SHALL verify the user has the ROLE_DEANSHIP role
4. IF the user lacks the ROLE_DEANSHIP role, THEN THE System SHALL redirect the user to the login page with an access denied message
5. WHEN a user navigates between pages using browser back/forward buttons, THE System SHALL load the correct page content

### Requirement 2: Shared Navigation and Layout

**User Story:** As a dean, I want consistent navigation across all pages, so that I can easily switch between different sections without confusion.

#### Acceptance Criteria

1. THE System SHALL display a shared navigation bar on all deanship pages containing:
   - Application header "Al-Quds University / Deanship Dashboard"
   - Navigation links to all six functional areas
   - User information display showing the logged-in dean's name
   - Logout button
2. WHEN a user is viewing a specific page, THE System SHALL highlight the corresponding navigation link as active
3. WHEN a user clicks a navigation link, THE System SHALL navigate to the corresponding page
4. THE System SHALL maintain consistent visual styling across all navigation elements on all pages
5. THE System SHALL preserve the current color scheme and branding across all pages

### Requirement 3: Global Academic Context Filters

**User Story:** As a dean, I want to select an academic year and semester once and have that selection apply across all relevant pages, so that I don't have to re-select filters when navigating.

#### Acceptance Criteria

1. THE System SHALL display academic year and semester dropdown selectors in the shared layout on all pages
2. WHEN a user selects an academic year, THE System SHALL:
   - Store the selection in browser local storage
   - Load the corresponding semesters for that academic year
   - Trigger data reload on the current page if applicable
3. WHEN a user selects a semester, THE System SHALL:
   - Store the selection in browser local storage
   - Trigger data reload on the current page if applicable
4. WHEN a user navigates to a different page, THE System SHALL preserve the selected academic year and semester
5. THE System SHALL apply academic context filters to Course Assignments, Reports, and File Explorer pages
6. THE System SHALL NOT apply academic context filters to Academic Years, Professors, and Courses pages

### Requirement 4: Academic Years Management Page

**User Story:** As a dean, I want a dedicated page for managing academic years, so that I can focus on this specific administrative task.

#### Acceptance Criteria

1. WHEN a user navigates to `/deanship/academic-years`, THE System SHALL display a page containing:
   - Page title "Academic Years Management"
   - "Add Academic Year" button
   - Table showing all academic years with columns: Year Code, Start Year, End Year, Status, Actions
2. WHEN a user clicks "Add Academic Year", THE System SHALL display a modal form to create a new academic year
3. WHEN a user clicks "Edit" on an academic year, THE System SHALL display a modal form to update that academic year
4. WHEN a user clicks "Activate" on an inactive academic year, THE System SHALL activate that year and deactivate all other years
5. THE System SHALL load academic years data via GET request to `/api/academic-years`
6. THE System SHALL display appropriate empty state message when no academic years exist

### Requirement 5: Professors Management Page

**User Story:** As a dean, I want a dedicated page for managing professors, so that I can efficiently handle faculty administration.

#### Acceptance Criteria

1. WHEN a user navigates to `/deanship/professors`, THE System SHALL display a page containing:
   - Page title "Professors Management"
   - "Add Professor" button
   - Search input field for filtering professors
   - Department filter dropdown
   - Table showing professors with columns: Professor ID, Name, Email, Department, Status, Actions
2. WHEN a user types in the search field, THE System SHALL filter the professors table by name, email, or professor ID
3. WHEN a user selects a department filter, THE System SHALL reload professors data filtered by that department
4. WHEN a user clicks "Add Professor", THE System SHALL display a modal form to create a new professor
5. WHEN a user clicks "Edit" on a professor, THE System SHALL display a modal form to update that professor
6. THE System SHALL load professors data via GET request to `/api/professors` with optional departmentId parameter

### Requirement 6: Courses Management Page

**User Story:** As a dean, I want a dedicated page for managing courses, so that I can maintain the course catalog effectively.

#### Acceptance Criteria

1. WHEN a user navigates to `/deanship/courses`, THE System SHALL display a page containing:
   - Page title "Courses Management"
   - "Add Course" button
   - Search input field for filtering courses
   - Department filter dropdown
   - Table showing courses with columns: Course Code, Course Name, Department, Credits, Status, Actions
2. WHEN a user types in the search field, THE System SHALL filter the courses table by course code or course name
3. WHEN a user selects a department filter, THE System SHALL reload courses data filtered by that department
4. WHEN a user clicks "Add Course", THE System SHALL display a modal form to create a new course
5. WHEN a user clicks "Edit" on a course, THE System SHALL display a modal form to update that course
6. THE System SHALL load courses data via GET request to `/api/courses` with optional departmentId parameter

### Requirement 7: Course Assignments Page

**User Story:** As a dean, I want a dedicated page for managing course assignments, so that I can assign professors to courses for specific semesters.

#### Acceptance Criteria

1. WHEN a user navigates to `/deanship/course-assignments`, THE System SHALL display a page containing:
   - Page title "Course Assignments"
   - "Assign Course" button
   - Professor filter dropdown
   - Course filter dropdown
   - List or table showing current assignments with professor name, course name, semester, and actions
2. WHEN no academic year is selected, THE System SHALL display a message prompting the user to select an academic year
3. WHEN an academic year and semester are selected, THE System SHALL load assignments filtered by that context
4. WHEN a user clicks "Assign Course", THE System SHALL display a modal form to create a new assignment
5. WHEN a user clicks "Unassign" on an assignment, THE System SHALL remove that assignment after confirmation
6. THE System SHALL load assignments data via GET request to `/api/course-assignments` with academicYearId and semesterId parameters

### Requirement 8: Reports Page

**User Story:** As a dean, I want a dedicated page for viewing reports, so that I can analyze submission status and other metrics.

#### Acceptance Criteria

1. WHEN a user navigates to `/deanship/reports`, THE System SHALL display a page containing:
   - Page title "Reports"
   - Card for "Submission Status Report" with description and "View Report" button
2. WHEN a user clicks "View Report", THE System SHALL load and display the submission status report
3. WHEN no academic year is selected, THE System SHALL display a message prompting the user to select an academic year
4. WHEN an academic year and semester are selected, THE System SHALL load report data filtered by that context
5. THE System SHALL load report data via GET request to `/api/reports/submission-status` with academicYearId and semesterId parameters

### Requirement 9: File Explorer Page

**User Story:** As a dean, I want a dedicated page for browsing the file system, so that I can access archived documents efficiently.

#### Acceptance Criteria

1. WHEN a user navigates to `/deanship/file-explorer`, THE System SHALL display a page containing:
   - Page title "File Explorer"
   - Folder tree navigation on the left side
   - File list/table on the right side
   - Breadcrumb navigation showing current path
2. WHEN no academic year is selected, THE System SHALL display a message prompting the user to select an academic year
3. WHEN an academic year and semester are selected, THE System SHALL load the file explorer with the appropriate folder structure
4. THE System SHALL preserve all existing file explorer functionality including folder navigation, file viewing, and file operations
5. THE System SHALL use the existing FileExplorer component without modification

### Requirement 10: Main Dashboard Landing Page

**User Story:** As a dean, I want a main dashboard page that provides an overview and quick access to all sections, so that I can efficiently navigate to the area I need.

#### Acceptance Criteria

1. WHEN a user navigates to `/deanship/dashboard`, THE System SHALL display a landing page containing:
   - Welcome header "Deanship Dashboard"
   - Subtitle describing the dashboard purpose
   - Six cards representing each functional area
2. WHEN the System renders each card, THE System SHALL display:
   - Section icon or visual indicator
   - Section title
   - Brief description of the section
   - "Open" or "Go to" button
3. WHEN a user clicks a card's button, THE System SHALL navigate to the corresponding section page
4. WHERE summary metrics are available, THE System SHALL display them on the dashboard cards:
   - Number of active academic years
   - Number of active professors
   - Number of active courses
   - Submission completion percentage
5. THE System SHALL make the dashboard responsive for common laptop resolutions (1366x768 and above)

### Requirement 11: Backend Route Handling

**User Story:** As a developer, I want the backend to serve separate HTML pages for each route, so that the multi-page architecture functions correctly.

#### Acceptance Criteria

1. THE System SHALL provide controller endpoints that return HTML templates for each route:
   - `/deanship/dashboard` returns dashboard.html
   - `/deanship/academic-years` returns academic-years.html
   - `/deanship/professors` returns professors.html
   - `/deanship/courses` returns courses.html
   - `/deanship/course-assignments` returns course-assignments.html
   - `/deanship/reports` returns reports.html
   - `/deanship/file-explorer` returns file-explorer.html
2. THE System SHALL maintain all existing REST API endpoints without breaking changes
3. THE System SHALL apply authentication and authorization checks to all deanship routes
4. THE System SHALL return appropriate HTTP status codes for unauthorized access attempts
5. WHERE a shared base controller exists, THE System SHALL use it to reduce code duplication

### Requirement 12: UI and Typography Improvements

**User Story:** As a dean, I want larger, more readable text throughout the interface, so that I can work comfortably without straining my eyes.

#### Acceptance Criteria

1. THE System SHALL increase the base font size from 14px to 16px across all deanship pages
2. THE System SHALL render table headers with font-weight bold and font-size 18px
3. THE System SHALL render section titles with font-size 24px and font-weight bold
4. THE System SHALL render card titles with font-size 20px and font-weight semibold
5. THE System SHALL ensure text contrast ratios meet WCAG AA standards (minimum 4.5:1 for normal text)
6. THE System SHALL increase vertical spacing between sections by adding margin-bottom of 24px
7. THE System SHALL increase table row height to minimum 48px for better readability
8. THE System SHALL increase padding inside cards to 24px
9. THE System SHALL make action buttons (Add, Edit, etc.) visually prominent with minimum height of 40px
10. THE System SHALL ensure all pages are horizontally scrollable on screens narrower than content width

### Requirement 13: Data Loading and Error Handling

**User Story:** As a dean, I want clear feedback when data is loading or when errors occur, so that I understand the system state.

#### Acceptance Criteria

1. WHEN a page loads data from the backend, THE System SHALL display a loading indicator
2. IF a data request fails, THEN THE System SHALL display a user-friendly error message via toast notification
3. THE System SHALL log detailed error information to the browser console for debugging
4. WHEN a page requires academic context but none is selected, THE System SHALL display a helpful message guiding the user to select filters
5. THE System SHALL handle network errors gracefully without breaking the page functionality

### Requirement 14: State Preservation and Navigation

**User Story:** As a dean, I want my selections and scroll position preserved when navigating between pages, so that I can maintain my workflow context.

#### Acceptance Criteria

1. THE System SHALL persist the selected academic year in browser local storage with key `deanship_selected_academic_year`
2. THE System SHALL persist the selected semester in browser local storage with key `deanship_selected_semester`
3. THE System SHALL persist the last visited page in browser local storage with key `deanship_last_page`
4. WHEN a user returns to the deanship dashboard, THE System SHALL restore the previously selected academic year and semester
5. THE System SHALL support browser back and forward navigation without losing application state

### Requirement 15: Backward Compatibility

**User Story:** As a system administrator, I want the refactor to maintain all existing functionality, so that no features are lost during the transition.

#### Acceptance Criteria

1. THE System SHALL preserve all existing CRUD operations for academic years, professors, courses, and assignments
2. THE System SHALL preserve all existing modal forms and their validation logic
3. THE System SHALL preserve all existing API endpoints and their request/response formats
4. THE System SHALL preserve all existing authentication and authorization mechanisms
5. THE System SHALL preserve all existing file explorer functionality including upload, download, and folder navigation
6. THE System SHALL preserve all existing status badges and their color coding
7. THE System SHALL preserve all existing filter and search functionality
