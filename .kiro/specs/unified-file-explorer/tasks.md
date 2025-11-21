                                                                                                                                                                # Implementation Plan

This implementation plan breaks down the unified File Explorer feature into discrete, actionable coding tasks. Each task builds incrementally on previous tasks and references specific requirements from the requirements document.

## Task List

- [x] 1. Enhance FileExplorer class with role-specific configuration





  - Review the existing FileExplorer class in `src/main/resources/static/js/file-explorer.js`
  - Add role-specific configuration options to the constructor (role, showOwnershipLabels, showDepartmentContext, headerMessage, showProfessorLabels)
  - Implement logic to conditionally render role-specific labels based on configuration
  - Add support for header message display in the File Explorer container
  - Update the render() method to include role-specific UI elements
  - _Requirements: 1.1, 4.1, 4.2, 4.3, 4.4, 5.2, 10.4_

- [x] 2. Implement role-specific label rendering in FileExplorer





  - Create a method to generate "Your Folder" labels for Professor role when canWrite is true
  - Create a method to generate "Read Only" labels for folders without write permission
  - Create a method to generate professor name labels for Deanship role
  - Update renderFileList() to include role-specific labels on folder cards
  - Ensure labels use the same badge styling as Professor Dashboard (bg-blue-100, text-blue-800 for ownership, bg-gray-100, text-gray-600 for read-only)
  - _Requirements: 4.1, 4.2, 4.3, 4.5, 7.1, 7.2, 7.3_

- [x] 3. Add header message support for HOD dashboard





  - Modify the render() method to accept and display a header message
  - Add a header section above the breadcrumbs when headerMessage option is provided
  - Style the header message using text-sm text-gray-600 classes
  - Ensure the header message "Browse department files (Read-only)" displays for HOD role
  - _Requirements: 4.2, 4.5_

- [x] 4. Verify and document Professor Dashboard File Explorer as master reference





  - Review `src/main/resources/static/prof-dashboard.html` File Explorer tab structure
  - Review `src/main/resources/static/js/prof.js` File Explorer initialization code
  - Verify that Professor Dashboard uses consistent HTML structure and Tailwind classes
  - Add code comments identifying the Professor Dashboard File Explorer as the canonical layout
  - Document the Academic Year and Semester selector pattern used in Professor Dashboard
  - _Requirements: 1.1, 5.5, 8.1, 8.2, 8.3, 10.1, 10.2_
- [x] 5. Update Professor Dashboard to use enhanced FileExplorer configuration





























- [ ] 5. Update Professor Dashboard to use enhanced FileExplorer configuration

  - Modify `src/main/resources/static/js/prof.js` to explicitly set role configuration options
  - Set role: 'PROFESSOR', showOwnershipLabels: true, readOnly: false
  - Verify that "Your Folder" labels appear on professor's own course folders
  - Test breadcrumb navigation and file operations
  - Ensure all existing functionality continues to work
  - _Requirements: 1.1, 4.1, 5.1, 5.2, 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 6. Migrate HOD Dashboard to use unified FileExplorer component





  - Open `src/main/resources/static/hod-dashboard.html` and locate the File Explorer tab section
  - Update the HTML structure to match Professor Dashboard File Explorer layout (breadcrumbs, tree view, file list grid)
  - Open `src/main/resources/static/js/hod.js` and locate the File Explorer initialization code
  - Remove any custom File Explorer rendering logic
  - Instantiate FileExplorer class with HOD configuration: role: 'HOD', readOnly: true, showDepartmentContext: true, headerMessage: 'Browse department files (Read-only)'
  - Update the container ID to match the new FileExplorer instance
  - Ensure Academic Year and Semester selectors use the same styling as Professor Dashboard
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 3.2, 4.2, 5.1, 5.2, 5.3, 8.1, 8.2, 8.3, 8.4_

- [x] 7. Test HOD Dashboard File Explorer functionality





  - Verify that HOD can browse only their department's professors and courses
  - Verify that "Browse department files (Read-only)" message displays in the header
  - Verify that folder cards use the same blue card design as Professor Dashboard
  - Verify that breadcrumb navigation works correctly
  - Verify that file download functionality works
  - Verify that no upload buttons or write actions are available
  - Test Academic Year and Semester selector behavior
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 3.2, 4.2, 7.1, 7.4, 8.1, 8.2, 9.3_
- [ ] 8. Migrate Deanship Dashboard to use unified FileExplorer component













- [ ] 8. Migrate Deanship Dashboard to use unified FileExplorer component

  - Open `src/main/resources/static/deanship-dashboard.html` and locate the File Explorer tab section
  - Update the HTML structure to match Professor Dashboard File Explorer layout
  - Open `src/main/resources/static/js/deanship.js` and locate the File Explorer initialization code
  - Remove any custom File Explorer rendering logic
  - Instantiate FileExplorer class with Deanship configuration: role: 'DEANSHIP', readOnly: true, showAllDepartments: true, showProfessorLabels: true
  - Update the container ID to match the new FileExplorer instance
  - Ensure Academic Year and Semester selectors use the same styling as Professor Dashboard
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 3.1, 4.3, 5.1, 5.2, 5.3, 8.1, 8.2, 8.3, 8.4_




-

- [x] 9. Test Deanship Dashboard File Explorer functionality





  - Verify that Deanship can browse all academic years, semesters, professors, and courses across all departments
  - Verify that professor name labels display on professor folders
  - Verify that folder cards use the same blue card design as Professor Dashboard
  - Verify that breadcrumb navigation works correctly
  - Verify that file download functionality works
  - Verify that no upload buttons or write actions are available
  - Test Academic Year and Semester selector behavior
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 3.1, 4.3, 7.1, 7.3, 7.4, 8.1, 8.2, 9.3_

- [x] 10. Implement consistent empty state rendering across all dashboards











  - Create a shared method in FileExplorer class for rendering empty states
  - Use the same icon (folder icon), text styling (text-sm text-gray-500), and layout (py-8 text-center) as Professor Dashboard
  - Ensure empty state displays when a folder has no items
  - Ensure "Select a semester to browse files" message displays when no semester is selected
  - Apply empty state rendering to all three dashboards
  - _Requirements: 1.1, 1.2, 6.1, 6.4_
-

- [x] 11. Implement consistent loading state rendering across all dashboards




  - Create a shared method in FileExplorer class for rendering loading states
  - Use skeleton loaders with the same animation and styling as Professor Dashboard
  - Ensure loading state displays while data is being fetched
  - Apply loading state rendering to all three dashboards
  - _Requirements: 1.1, 1.2, 6.2_

- [x] 12. Implement consistent error state rendering across all dashboards





  - Create a shared method in FileExplorer class for rendering error states
  - Use the same error icon, text styling, and layout as Professor Dashboard
  - Ensure error state displays when API calls fail
  - Apply error state rendering to all three dashboards
  - _Requirements: 1.1, 1.2, 6.3_

- [x] 13. Standardize breadcrumb navigation behavior across all dashboards





  - Verify that breadcrumb path updates correctly when navigating through folders
  - Verify that clicking on a breadcrumb segment navigates to that level
  - Verify that home icon displays for the root level
  - Verify that current location is highlighted in the breadcrumb
  - Verify that horizontal scrolling works when breadcrumb path is long
  - Test breadcrumb behavior in all three dashboards
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 14. Standardize Academic Year and Semester selector styling and behavior





  - Verify that Academic Year selector uses the same label, dropdown styling, and positioning across all dashboards
  - Verify that Semester selector uses the same label, dropdown styling, and positioning across all dashboards
  - Verify that selecting an academic year loads semesters and enables the semester selector
  - Verify that selecting a semester updates the File Explorer content
  - Verify that the active academic year is auto-selected on page load
  - Test selector behavior in all three dashboards
  - _Requirements: 1.5, 8.1, 8.2, 8.3, 8.4, 8.5_


- [x] 15. Verify consistent folder card design across all dashboards




  - Verify that course folders use wide blue cards (bg-blue-50, border-blue-200) with folder icon
  - Verify that document type folders use the same card design
  - Verify that professor folders (in HOD/Deanship views) use the same card design
  - Verify that hover effects (hover:bg-blue-100) work consistently
  - Verify that arrow icon animates on hover (group-hover:translate-x-1)
  - Test folder card rendering in all three dashboards
  - _Requirements: 1.2, 7.1, 7.2, 7.3_

- [x] 16. Verify consistent file table design across all dashboards




  - Verify that file tables use the same column layout (Name, Size, Uploaded, Uploader, Actions)
  - Verify that file rows use the same typography and spacing
  - Verify that file icons use the same color coding (red for PDF, amber for ZIP, etc.)
  - Verify that metadata badges use the same styling (bg-gray-100, text-gray-700)
  - Verify that action buttons (Download, View) use the same styling and positioning
  - Test file table rendering in all three dashboards
  - _Requirements: 1.3, 7.4, 7.5_

- [x] 17. Add comprehensive code documentation




  - Add JSDoc comments to all FileExplorer class methods
  - Document role-specific configuration options with examples
  - Add inline comments explaining role-specific rendering logic
  - Document the master design reference in file-explorer.js
  - Update README or developer guide with File Explorer usage instructions
  - _Requirements: 5.5, 10.1, 10.2, 10.3, 10.4, 10.5_
-

- [x] 18. Perform cross-dashboard visual consistency verification




  - Open all three dashboards side-by-side
  - Compare File Explorer visual appearance (colors, spacing, typography, borders)
  - Verify that folder cards look identical across dashboards
  - Verify that file tables look identical across dashboards
  - Verify that breadcrumbs look identical across dashboards
  - Verify that empty, loading, and error states look identical across dashboards
  - Document any visual discrepancies and fix them
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 19. Perform end-to-end functional testing





  - Test complete user workflows in Professor Dashboard (browse, upload, download, replace files)
  - Test complete user workflows in HOD Dashboard (browse, download, verify read-only)
  - Test complete user workflows in Deanship Dashboard (browse, download, verify all departments visible)
  - Verify that all existing API endpoints work correctly
  - Verify that permission checks are enforced
  - Verify that file downloads work correctly
  - Test on multiple browsers (Chrome, Firefox, Safari, Edge)
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 20. Create rollback plan and deployment documentation




  - Document the changes made to each dashboard file
  - Create backup copies of original dashboard files
  - Document rollback procedure in case of critical issues
  - Create deployment checklist
  - Document any configuration changes needed for production
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_
