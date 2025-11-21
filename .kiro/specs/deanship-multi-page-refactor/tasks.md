# Implementation Plan

- [x] 1. Create backend view controller and route mappings





  - Create `DeanshipViewController.java` with route handlers for all seven pages
  - Add `@PreAuthorize("hasRole('DEANSHIP')")` security annotation to controller
  - Implement GET endpoints that return view names for dashboard, academic-years, professors, courses, course-assignments, reports, and file-explorer
  - Configure Spring MVC to resolve view names to HTML templates in static/deanship/ directory
  - Add exception handler for `AccessDeniedException` to redirect to login
  - _Requirements: 1.2, 1.3, 1.4, 11.1, 11.3, 11.4_

- [x] 2. Create shared layout module and CSS





  - Create `deanship-layout.css` with typography scale, spacing scale, and component styles
  - Implement CSS variables for font sizes (base 16px, headers 18-24px), spacing (4-48px), and colors
  - Create `deanship-common.js` module with `DeanshipLayout` class
  - Implement authentication check in `DeanshipLayout.initialize()`
  - Implement academic year and semester loading and selection restoration from localStorage
  - Add event listeners for academic year and semester dropdowns with localStorage persistence
  - Implement navigation link highlighting based on current page URL
  - Implement logout functionality that clears auth data and redirects to login
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9, 12.10_
- [x] 3. Create main dashboard page




- [ ] 3. Create main dashboard page

  - Create `dashboard.html` with shared layout structure (header, nav, filters, content area)
  - Create `dashboard.js` module that initializes `DeanshipLayout`
  - Implement six dashboard cards with icons, titles, descriptions, and "Open" buttons
  - Add click handlers to navigate to corresponding pages
  - Implement `loadDashboardStats()` function to fetch summary metrics from APIs
  - Display active academic years count, total professors count, total courses count, total assignments count
  - Display submission completion percentage from system-wide report API
  - Style cards with 24px padding, 8px border radius, and box shadow
  - Make dashboard responsive for 1366x768 and above resolutions
  - _Requirements: 1.1, 10.1, 10.2, 10.3, 10.4, 10.5_
- [x] 4. Create academic years management page




- [ ] 4. Create academic years management page

  - Create `academic-years.html` with shared layout and page-specific content area
  - Create `academic-years.js` module that initializes `DeanshipLayout`
  - Implement page title "Academic Years Management" and "Add Academic Year" button
  - Implement `loadAcademicYears()` function to fetch data from `/api/deanship/academic-years`
  - Implement `renderAcademicYearsTable()` to display table with columns: Year Code, Start Year, End Year, Status, Actions
  - Implement "Add Academic Year" modal with form for start year and end year
  - Implement "Edit" modal to update existing academic year
  - Implement "Activate" button handler to activate academic year via PUT request
  - Display empty state message when no academic years exist
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
-

- [x] 5. Create professors management page




  - Create `professors.html` with shared layout and page-specific content area
  - Create `professors.js` module that initializes `DeanshipLayout`
  - Implement page title "Professors Management" and "Add Professor" button
  - Add search input field and department filter dropdown
  - Implement `loadProfessors()` function to fetch data from `/api/deanship/professors` with optional departmentId parameter
  - Implement `renderProfessorsTable()` to display table with columns: Professor ID, Name, Email, Department, Status, Actions
  - Implement search filter that filters by name, email, or professor ID on input
  - Implement department filter that reloads data when changed
  - Implement "Add Professor" modal with form for name, email, password, and department
  - Implement "Edit" modal to update existing professor
  - Implement "Activate" and "Deactivate" button handlers
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_
-

- [x] 6. Create courses management page




  - Create `courses.html` with shared layout and page-specific content area
  - Create `courses.js` module that initializes `DeanshipLayout`
  - Implement page title "Courses Management" and "Add Course" button
  - Add search input field and department filter dropdown
  - Implement `loadCourses()` function to fetch data from `/api/deanship/courses` with optional departmentId parameter
  - Implement `renderCoursesTable()` to display table with columns: Course Code, Course Name, Department, Credits, Status, Actions
  - Implement search filter that filters by course code or course name on input
  - Implement department filter that reloads data when changed
  - Implement "Add Course" modal with form for course code, name, department, and credits
  - Implement "Edit" modal to update existing course
  - Implement "Deactivate" button handler
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_
-

- [x] 7. Create course assignments page




  - Create `course-assignments.html` with shared layout and page-specific content area
  - Create `course-assignments.js` module that initializes `DeanshipLayout`
  - Implement page title "Course Assignments" and "Assign Course" button
  - Add professor filter dropdown and course filter dropdown
  - Implement context check to display message when no academic year is selected
  - Implement `loadAssignments()` function to fetch data from `/api/deanship/course-assignments` with semesterId parameter
  - Implement `renderAssignmentsTable()` to display assignments with professor name, course name, semester, and actions
  - Register callback with `DeanshipLayout` to reload assignments when academic year or semester changes
  - Implement "Assign Course" modal with form for professor, course, and semester selection
  - Implement "Unassign" button handler with confirmation dialog
  - Apply professor and course filters to reload assignments when changed
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 3.5_
-

- [x] 8. Create reports page




  - Create `reports.html` with shared layout and page-specific content area
  - Create `reports.js` module that initializes `DeanshipLayout`
  - Implement page title "Reports"
  - Create card for "Submission Status Report" with description and "View Report" button
  - Implement context check to display message when no academic year is selected
  - Implement `loadSubmissionReport()` function to fetch data from `/api/deanship/reports/system-wide` with semesterId parameter
  - Register callback with `DeanshipLayout` to reload report when academic year or semester changes
  - Implement report display with submission statistics and completion percentage
  - Style report card with appropriate spacing and typography
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 3.5_
- [x] 9. Create file explorer page




- [ ] 9. Create file explorer page

  - Create `file-explorer.html` with shared layout and page-specific content area
  - Create `file-explorer-page.js` module that initializes `DeanshipLayout`
  - Implement page title "File Explorer"
  - Add container for folder tree navigation on left side
  - Add container for file list/table on right side
  - Add breadcrumb navigation container
  - Implement context check to display message when no academic year is selected
  - Initialize existing `FileExplorer` component with academic year and semester context
  - Register callback with `DeanshipLayout` to reinitialize file explorer when academic year or semester changes
  - Preserve all existing file explorer functionality (folder navigation, file viewing, file operations)
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 3.5_

- [x] 10. Implement state preservation and navigation





  - Update `DeanshipLayout` to persist selected academic year to localStorage with key `deanship_selected_academic_year`
  - Update `DeanshipLayout` to persist selected semester to localStorage with key `deanship_selected_semester`
  - Implement restoration of academic year and semester selections on page load
  - Add navigation tracking to persist last visited page with key `deanship_last_page`
  - Test browser back and forward navigation to ensure correct page loads
  - Test page refresh to ensure state is preserved
  - Verify that academic context persists when navigating between pages
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 3.4, 1.5_

- [x] 11. Implement error handling and loading states





  - Add loading indicators to all data fetching functions in page modules
  - Implement try-catch blocks around all API calls with user-friendly error messages
  - Display toast notifications for errors using existing `showToast()` function
  - Log detailed error information to console for debugging
  - Implement empty state messages for pages requiring academic context when none is selected
  - Add network error handling that displays helpful messages without breaking page functionality
  - Test error scenarios: network failure, 401 unauthorized, 403 forbidden, 500 server error
  - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_
- [ ] 12. Update authentication flow and entry points













- [ ] 12. Update authentication flow and entry points

  - Update login success redirect to point to `/deanship/dashboard` instead of old single-page dashboard
  - Verify that unauthorized access to any `/deanship/*` route redirects to login page
  - Test that non-deanship users receive access denied message and redirect
  - Verify that logout works correctly from all pages and clears authentication state
  - Test session timeout behavior across all pages
  - _Requirements: 1.3, 1.4, 2.1_

- [x] 13. Apply UI and typography improvements




  - Update base font size to 16px in `deanship-layout.css`
  - Style table headers with font-size 18px and font-weight 600
  - Style section titles with font-size 24px and font-weight 700
  - Style card titles with font-size 20px and font-weight 600
  - Verify text contrast ratios meet WCAG AA standards (4.5:1 minimum)
  - Add vertical spacing between sections with margin-bottom 24px
  - Increase table row height to minimum 56px
  - Increase card padding to 24px
  - Style action buttons with minimum height 40px and prominent colors
  - Test horizontal scrolling behavior on 1366x768 resolution
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9, 12.10_
-

- [x] 14. Integration testing and verification




  - Test navigation between all pages using navigation links
  - Verify active navigation link highlights correctly on each page
  - Test academic year and semester filter persistence across page navigation
  - Test browser back and forward buttons work correctly
  - Test page refresh preserves state on all pages
  - Verify all CRUD operations work on academic years, professors, courses, and assignments pages
  - Test search and filter functionality on professors and courses pages
  - Verify file explorer functionality works correctly with academic context
  - Test dashboard cards navigate to correct pages
  - Test logout functionality from all pages
  - Verify responsive behavior on 1366x768 and 1920x1080 resolutions
  - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.7_


- [x] 15. Documentation and deployment preparation




  - Update README or create deployment guide documenting new route structure
  - Document where each page's code lives (HTML, JS, CSS files)
  - Document how shared layout and filters work
  - Create rollback plan documenting how to revert to old single-page dashboard
  - Update any existing documentation that references the old dashboard structure
  - Add code comments explaining key architectural decisions
  - _Requirements: 11.5_
