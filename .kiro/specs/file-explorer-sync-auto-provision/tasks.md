# Implementation Plan

## Overview

This implementation plan breaks down the File Explorer synchronization and auto-provisioning feature into discrete, manageable coding tasks. Each task builds incrementally on previous work, ensuring the system remains functional throughout development.

All context documents (requirements.md, design.md) are available during implementation.

---

## Phase 1: Backend Foundation - Folder Service

### Task 1: Create FolderService Interface and Core Implementation
@task Task1

- [ ] 1.1 Create FolderService interface with method signatures
  - Define `createProfessorFolder()` method signature
  - Define `createCourseFolderStructure()` method signature
  - Define `professorFolderExists()` and `courseFolderExists()` check methods
  - Define `getFolderByPath()` and `createFolderIfNotExists()` utility methods
  - Add comprehensive JavaDoc comments for each method
  - _Requirements: 1.1, 1.3, 2.1, 2.3_

- [ ] 1.2 Implement FolderServiceImpl class with idempotent folder creation
  - Create `FolderServiceImpl` class with required repository dependencies
  - Implement `createProfessorFolder()` with idempotency check (return existing if found)
  - Implement path generation logic following convention: `{yearCode}/{semesterType}/{professorId}`
  - Implement physical file system directory creation using `Files.createDirectories()`
  - Implement database entity persistence with proper transaction handling
  - Add comprehensive logging for folder creation operations
  - _Requirements: 1.1, 1.3, 1.4_

- [ ] 1.3 Implement course folder structure creation
  - Implement `createCourseFolderStructure()` method
  - Generate course folder path: `{yearCode}/{semesterType}/{professorId}/{courseCode} - {courseName}`
  - Create standard subfolders: Syllabus, Exams, Course Notes, Assignments
  - Implement idempotency checks for each subfolder (skip if exists)
  - Return list of created/existing folders
  - Add logging for each folder creation step
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ] 1.4 Implement folder existence check methods
  - Implement `professorFolderExists()` using path-based query
  - Implement `courseFolderExists()` using path-based query
  - Implement `getFolderByPath()` with Optional return type
  - Add database indexes on folder path column for performance
  - _Requirements: 1.3, 2.3_

- [ ] 1.5 Implement utility method for folder creation
  - Implement `createFolderIfNotExists()` helper method
  - Handle parent folder relationships
  - Set folder type, owner, academic year, and semester
  - Create physical directory and database entity atomically
  - Handle errors gracefully with rollback support
  - _Requirements: 1.4, 8.2_

- [ ] 1.6 Write unit tests for FolderService
  - Test `createProfessorFolder()` success case
  - Test `createProfessorFolder()` idempotency (calling twice returns same folder)
  - Test `createCourseFolderStructure()` success case
  - Test `createCourseFolderStructure()` with partial existing folders
  - Test folder existence check methods
  - Mock repository and file system operations
  - _Requirements: 8.5_

---

## Phase 2: Backend Integration - Professor and Assignment Services

### Task 2: Integrate FolderService into ProfessorService
@task Task2

- [ ] 2.1 Add FolderService dependency to ProfessorServiceImpl
  - Inject `FolderService` via constructor
  - Add field declaration with `@RequiredArgsConstructor` support
  - _Requirements: 1.1, 8.2_

- [ ] 2.2 Implement auto-folder creation in createProfessor method
  - After professor creation, get current academic year and semester from context
  - Call `folderService.createProfessorFolder()` with professor ID, year ID, semester ID
  - Wrap folder creation in try-catch to prevent professor creation failure
  - Log success or failure of folder creation
  - Return professor entity regardless of folder creation outcome
  - _Requirements: 1.1, 1.2, 1.5_

- [ ] 2.3 Add endpoint to manually trigger professor folder creation
  - Create `POST /deanship/professors/{id}/create-folder` endpoint
  - Accept academicYearId and semesterId as query parameters
  - Call `folderService.createProfessorFolder()`
  - Return folder information in response
  - Add proper authorization checks (Deanship only)
  - _Requirements: 1.1, 1.2_

- [ ] 2.4 Write unit tests for professor folder auto-creation
  - Test that `createProfessor()` calls `folderService.createProfessorFolder()`
  - Test that professor creation succeeds even if folder creation fails
  - Mock FolderService to verify method calls
  - _Requirements: 8.5_

### Task 3: Integrate FolderService into CourseAssignmentService
@task Task3

- [ ] 3.1 Add FolderService dependency to CourseAssignmentServiceImpl
  - Inject `FolderService` via constructor
  - Add field declaration
  - _Requirements: 2.1, 8.2_

- [ ] 3.2 Implement auto-folder creation in createAssignment method
  - After assignment creation, extract professor, course, academic year, and semester
  - Call `folderService.createCourseFolderStructure()` with required parameters
  - Wrap folder creation in try-catch to prevent assignment creation failure
  - Log success or failure of folder creation
  - Return assignment entity regardless of folder creation outcome
  - _Requirements: 2.1, 2.2, 2.5_

- [ ] 3.3 Add endpoint to manually trigger course folder creation
  - Create `POST /deanship/course-assignments/{id}/create-folders` endpoint
  - Extract assignment details and call `folderService.createCourseFolderStructure()`
  - Return folder information in response
  - Add proper authorization checks (Deanship only)
  - _Requirements: 2.1, 2.2_

- [ ] 3.4 Write unit tests for course folder auto-creation
  - Test that `createAssignment()` calls `folderService.createCourseFolderStructure()`
  - Test that assignment creation succeeds even if folder creation fails
  - Mock FolderService to verify method calls
  - _Requirements: 8.5_

---

## Phase 3: Backend API Enhancements

### Task 4: Enhance File Explorer API for Synchronization
@task Task4

- [ ] 4.1 Review and optimize File Explorer root endpoint
  - Review `/file-explorer/root` endpoint implementation
  - Ensure it returns fresh data from database (no caching issues)
  - Add query parameter support for filtering by academic year and semester
  - Optimize database queries with proper JOIN FETCH for related entities
  - _Requirements: 3.1, 3.2, 3.5_

- [ ] 4.2 Review and optimize File Explorer node endpoint
  - Review `/file-explorer/node` endpoint implementation
  - Ensure it returns fresh data for the specified path
  - Add proper error handling for non-existent paths
  - Return empty children array instead of error for empty folders
  - _Requirements: 3.1, 3.2, 3.5_

- [ ] 4.3 Add File Explorer refresh endpoint
  - Create `POST /file-explorer/refresh` endpoint
  - Accept academicYearId and semesterId as parameters
  - Trigger cache invalidation if caching is used
  - Return updated root tree structure
  - _Requirements: 3.1, 3.2_

- [ ] 4.4 Ensure consistent API responses across all dashboards
  - Verify Dean, Professor, and HOD endpoints return same data structure
  - Standardize error responses across all File Explorer endpoints
  - Add consistent metadata (timestamps, counts) to responses
  - _Requirements: 3.1, 3.2, 3.5_

- [ ] 4.5 Write integration tests for File Explorer API
  - Test root endpoint returns correct folder structure
  - Test node endpoint returns correct children
  - Test that newly created folders appear in API responses
  - Test cross-dashboard consistency (Dean creates, Professor sees)
  - _Requirements: 3.1, 3.2, 8.5_

---

## Phase 4: Frontend State Management

### Task 5: Create FileExplorerState Module
@task Task5

- [ ] 5.1 Create file-explorer-state.js module
  - Create new file `src/main/resources/static/js/file-explorer-state.js`
  - Define `FileExplorerState` class with state object structure
  - Implement state properties: academicYearId, semesterId, treeRoot, currentNode, currentPath, breadcrumbs, expandedNodes
  - Implement loading state properties: isLoading, isTreeLoading, isFileListLoading, error
  - _Requirements: 5.1, 5.2_

- [ ] 5.2 Implement state getter methods
  - Implement `getState()` method returning immutable copy of state
  - Implement `getContext()` method returning academic year/semester context
  - Implement specific getters for tree, node, path, breadcrumbs
  - _Requirements: 5.1, 5.2_

- [ ] 5.3 Implement state setter methods
  - Implement `setContext()` for academic year/semester changes
  - Implement `setTreeRoot()` for updating folder tree
  - Implement `setCurrentNode()` for navigation
  - Implement `setBreadcrumbs()` for breadcrumb updates
  - Implement `toggleNodeExpansion()` for tree expand/collapse
  - Implement loading state setters: `setLoading()`, `setTreeLoading()`, `setFileListLoading()`
  - Implement error state setters: `setError()`, `clearError()`
  - _Requirements: 5.2, 5.3_

- [ ] 5.4 Implement observer pattern for state changes
  - Implement `subscribe()` method to register listeners
  - Implement `notify()` method to call all listeners on state change
  - Return unsubscribe function from `subscribe()`
  - Ensure all setter methods call `notify()` after state update
  - _Requirements: 5.2, 5.3, 5.4_

- [ ] 5.5 Implement state reset and persistence
  - Implement `reset()` method to clear all state
  - Add `lastUpdated` timestamp to track state freshness
  - Export singleton instance `fileExplorerState`
  - _Requirements: 5.1, 5.5_

---

## Phase 5: Frontend File Explorer Component Enhancement

### Task 6: Integrate FileExplorerState into FileExplorer Component
@task Task6

- [ ] 6.1 Import and subscribe to FileExplorerState
  - Import `fileExplorerState` in `file-explorer.js`
  - Subscribe to state changes in constructor
  - Store unsubscribe function for cleanup
  - _Requirements: 5.2, 5.3_

- [ ] 6.2 Implement onStateChange handler
  - Create `onStateChange(state)` method
  - Update local component properties from state
  - Trigger UI re-renders based on state changes
  - Handle loading states appropriately
  - Handle error states appropriately
  - _Requirements: 5.3, 5.4_

- [ ] 6.3 Refactor loadRoot to use FileExplorerState
  - Update `loadRoot()` to call `fileExplorerState.setLoading(true)`
  - Update state with tree root using `fileExplorerState.setTreeRoot()`
  - Update state with current node using `fileExplorerState.setCurrentNode()`
  - Call `fileExplorerState.setLoading(false)` on completion
  - Handle errors by calling `fileExplorerState.setError()`
  - _Requirements: 4.1, 5.2, 5.3_

- [ ] 6.4 Refactor loadNode to use FileExplorerState
  - Update `loadNode()` to call `fileExplorerState.setFileListLoading(true)`
  - Update state with new current node
  - Update expanded nodes in state
  - Call `fileExplorerState.setFileListLoading(false)` on completion
  - _Requirements: 4.1, 5.2, 5.3_

- [ ] 6.5 Refactor tree expansion to use FileExplorerState
  - Update `toggleNode()` to call `fileExplorerState.toggleNodeExpansion()`
  - Remove local expandedNodes tracking
  - Use state's expandedNodes set for rendering
  - _Requirements: 5.2, 5.3, 5.5_

- [ ] 6.6 Add cleanup method
  - Implement `destroy()` method
  - Call unsubscribe function to remove state listener
  - Clean up any other resources
  - _Requirements: 5.4, 8.2_

---

## Phase 6: Frontend Loading State Improvements

### Task 7: Implement Stable Loading States
@task Task7

- [ ] 7.1 Create loading skeleton rendering methods
  - Implement `renderLoadingSkeleton(type)` method
  - Create tree skeleton: animated placeholder rows with folder icons
  - Create file list skeleton: animated placeholder cards
  - Use Tailwind's `animate-pulse` for loading animation
  - Maintain same dimensions as actual content to prevent layout shift
  - _Requirements: 4.1, 4.2, 4.4_

- [ ] 7.2 Implement showLoading method
  - Update `showLoading()` to render skeletons in both tree and file list
  - Do not remove or hide containers
  - Maintain container dimensions during loading
  - _Requirements: 4.1, 4.2, 4.4_

- [ ] 7.3 Implement showTreeLoading method
  - Create `showTreeLoading()` for tree-only loading
  - Render skeleton only in tree container
  - Keep file list visible
  - _Requirements: 4.1, 4.4_

- [ ] 7.4 Implement showFileListLoading method
  - Create `showFileListLoading()` for file list-only loading
  - Render skeleton only in file list container
  - Keep tree visible
  - _Requirements: 4.1, 4.4_

- [ ] 7.5 Update render methods to prevent layout shift
  - Ensure `renderTree()` maintains container height
  - Ensure `renderFileList()` maintains container height
  - Add smooth transitions between loading and loaded states
  - Test rapid state changes to verify no flickering
  - _Requirements: 4.4, 4.5_

---

## Phase 7: Frontend Visual Enhancements

### Task 8: Enhance Folder Structure Visual Design
@task Task8

- [ ] 8.1 Increase folder tree item sizes
  - Update tree node row height from `py-1.5` to `py-2.5`
  - Update tree node padding from `px-2` to `px-3`
  - Increase folder icon size from `w-4 h-4` to `w-5 h-5`
  - Increase folder name font size from `text-sm` to `text-base`
  - _Requirements: 6.1, 6.2, 6.3_

- [ ] 8.2 Improve tree node visual hierarchy
  - Increase indentation per level from `16px` to `20px`
  - Make selected node more prominent with darker blue background
  - Add subtle hover effect with smooth transition
  - Increase expand/collapse button size for easier clicking
  - _Requirements: 6.1, 6.2, 6.4_

- [ ] 8.3 Enhance folder and file card designs
  - Increase folder card padding from `p-4` to `p-5`
  - Increase folder icon size in cards from `w-7 h-7` to `w-8 h-8`
  - Increase file icon container from `w-12 h-12` to `w-14 h-14`
  - Improve spacing between card elements
  - _Requirements: 6.1, 6.2, 6.3_

- [ ] 8.4 Test responsive design
  - Test folder tree on mobile devices (320px width)
  - Test folder tree on tablets (768px width)
  - Test folder tree on desktop (1024px+ width)
  - Ensure text doesn't overflow or wrap awkwardly
  - Verify touch targets are large enough on mobile (min 44px)
  - _Requirements: 6.1, 6.5_

---

## Phase 8: Dashboard Integration

### Task 9: Integrate State Management into Deanship Dashboard
@task Task9

- [ ] 9.1 Import FileExplorerState in deanship.js
  - Import `fileExplorerState` from `./file-explorer-state.js`
  - Remove any local File Explorer state variables
  - _Requirements: 5.4, 7.1_

- [ ] 9.2 Update context change handler
  - Modify `onContextChange()` to call `fileExplorerState.setContext()`
  - Pass academicYearId, semesterId, yearCode, and semesterType
  - Trigger File Explorer reload if on file-explorer tab
  - _Requirements: 5.2, 7.1, 7.2_

- [ ] 9.3 Add File Explorer refresh after professor creation
  - In `handleCreateProfessor()`, after successful creation, check if on file-explorer tab
  - If on file-explorer tab and context is set, call `fileExplorerInstance.loadRoot()` with `isBackground=true`
  - Show toast notification: "Professor folder created"
  - _Requirements: 1.1, 1.2, 3.1, 3.2_

- [ ] 9.4 Add File Explorer refresh after course assignment
  - In `handleCreateAssignment()`, after successful creation, check if on file-explorer tab
  - If on file-explorer tab and context is set, call `fileExplorerInstance.loadRoot()` with `isBackground=true`
  - Show toast notification: "Course folders created"
  - _Requirements: 2.1, 2.2, 3.1, 3.2_

- [ ] 9.5 Ensure File Explorer respects year/semester selection
  - Verify File Explorer only loads when both academicYearId and semesterId are selected
  - Show "Select academic year and semester" message when context is incomplete
  - Update breadcrumbs to show current year and semester
  - _Requirements: 7.1, 7.2, 7.4_

### Task 10: Integrate State Management into Professor Dashboard
@task Task10

- [ ] 10.1 Import FileExplorerState in prof.js
  - Import `fileExplorerState` from `./file-explorer-state.js`
  - Remove any local File Explorer state variables
  - _Requirements: 5.4, 7.1_

- [ ] 10.2 Update semester selection handler
  - Modify semester change handler to call `fileExplorerState.setContext()`
  - Pass academicYearId, semesterId, yearCode, and semesterType
  - Trigger File Explorer reload if on file-explorer tab
  - _Requirements: 5.2, 7.1, 7.2_

- [ ] 10.3 Ensure File Explorer respects semester selection
  - Verify File Explorer only loads when semester is selected
  - Show "Select a semester" message when no semester selected
  - Update breadcrumbs to show current semester
  - _Requirements: 7.1, 7.2, 7.4_

### Task 11: Integrate State Management into HOD Dashboard
@task Task11

- [ ] 11.1 Import FileExplorerState in hod.js
  - Import `fileExplorerState` from `./file-explorer-state.js`
  - Remove any local File Explorer state variables
  - _Requirements: 5.4, 7.1_

- [ ] 11.2 Update semester selection handler
  - Modify semester change handler to call `fileExplorerState.setContext()`
  - Pass academicYearId, semesterId, yearCode, and semesterType
  - Trigger File Explorer reload if on file-explorer tab
  - _Requirements: 5.2, 7.1, 7.2_

- [ ] 11.3 Ensure File Explorer respects semester selection
  - Verify File Explorer only loads when semester is selected
  - Show "Select a semester" message when no semester selected
  - Update breadcrumbs to show current semester
  - _Requirements: 7.1, 7.2, 7.4_

---

## Phase 9: Testing and Validation

### Task 12: Backend Integration Testing
@task Task12

- [ ] 12.1 Write end-to-end test for professor creation flow
  - Create test that creates professor via API
  - Verify folder exists in database
  - Verify folder exists in file system
  - Call File Explorer API and verify folder appears in tree
  - _Requirements: 1.1, 1.2, 3.1, 8.5_

- [ ] 12.2 Write end-to-end test for course assignment flow
  - Create test that creates professor, course, and assignment via API
  - Verify course folder structure exists in database
  - Verify course folders exist in file system
  - Call File Explorer API and verify folders appear in tree
  - _Requirements: 2.1, 2.2, 3.1, 8.5_

- [ ] 12.3 Write test for idempotency
  - Create test that creates same professor folder twice
  - Verify only one folder exists
  - Create test that creates same course folder twice
  - Verify no duplicate folders
  - _Requirements: 1.3, 2.3, 8.5_

- [ ] 12.4 Write test for cross-dashboard synchronization
  - Create folder via Deanship API
  - Call Professor File Explorer API
  - Verify folder appears in Professor view
  - Call HOD File Explorer API
  - Verify folder appears in HOD view
  - _Requirements: 3.1, 3.2, 3.4, 8.5_

### Task 13: Frontend Manual Testing
@task Task13

- [ ] 13.1 Test professor creation and folder visibility
  - Open Deanship dashboard
  - Select academic year and semester
  - Navigate to Professors tab
  - Create new professor
  - Navigate to File Explorer tab
  - Verify professor folder appears in tree
  - Open Professor dashboard (login as new professor)
  - Verify professor folder appears in their File Explorer
  - _Requirements: 1.1, 1.2, 3.1, 3.2_

- [ ] 13.2 Test course assignment and folder visibility
  - Open Deanship dashboard
  - Select academic year and semester
  - Navigate to Assignments tab
  - Assign course to professor
  - Navigate to File Explorer tab
  - Verify course folder structure appears under professor folder
  - Verify all standard subfolders exist (Syllabus, Exams, Course Notes, Assignments)
  - Open Professor dashboard
  - Verify course folders appear in their File Explorer
  - _Requirements: 2.1, 2.2, 3.1, 3.2_

- [ ] 13.3 Test loading states and layout stability
  - Open Deanship dashboard
  - Select academic year and semester
  - Navigate to File Explorer tab
  - Observe loading skeleton appears
  - Verify no layout shift when data loads
  - Change semester selection
  - Verify smooth transition with skeleton
  - Rapidly switch between folders
  - Verify no flickering or layout jumps
  - _Requirements: 4.1, 4.2, 4.4, 4.5_

- [ ] 13.4 Test state management across tabs
  - Open Deanship dashboard
  - Select academic year and semester
  - Navigate to File Explorer tab
  - Expand several folders in tree
  - Navigate to different folder
  - Switch to Professors tab
  - Switch back to File Explorer tab
  - Verify expanded folders remain expanded
  - Verify current folder is still selected
  - _Requirements: 5.1, 5.2, 5.5_

- [ ] 13.5 Test visual enhancements
  - Open File Explorer in Deanship dashboard
  - Verify folder tree items are larger and easier to read
  - Verify folder icons are appropriately sized
  - Verify hover effects work smoothly
  - Verify selected folder is clearly highlighted
  - Test on mobile device (or browser dev tools mobile view)
  - Verify responsive design works correctly
  - _Requirements: 6.1, 6.2, 6.3, 6.5_

### Task 14: Browser Compatibility Testing
@task Task14

- [ ] 14.1 Test in Chrome
  - Test all functionality in latest Chrome
  - Verify loading states render correctly
  - Verify animations are smooth
  - Check console for errors
  - _Requirements: 8.5_

- [ ] 14.2 Test in Firefox
  - Test all functionality in latest Firefox
  - Verify loading states render correctly
  - Verify animations are smooth
  - Check console for errors
  - _Requirements: 8.5_

- [ ] 14.3 Test in Edge
  - Test all functionality in latest Edge
  - Verify loading states render correctly
  - Verify animations are smooth
  - Check console for errors
  - _Requirements: 8.5_

- [ ] 14.4 Test in Safari
  - Test all functionality in latest Safari (if available)
  - Verify loading states render correctly
  - Verify animations are smooth
  - Check console for errors
  - _Requirements: 8.5_

---

## Phase 10: Documentation and Cleanup

### Task 15: Update Documentation
@task Task15

- [ ] 15.1 Update API documentation
  - Document new FolderService methods
  - Document professor folder auto-creation behavior
  - Document course folder auto-creation behavior
  - Document File Explorer refresh endpoints
  - _Requirements: 8.4_

- [ ] 15.2 Update developer guide
  - Document FileExplorerState usage
  - Document how to integrate File Explorer in new dashboards
  - Document folder path conventions
  - Document standard subfolder structure
  - _Requirements: 8.4_

- [ ] 15.3 Create implementation summary
  - List all modified backend files with brief description of changes
  - List all modified frontend files with brief description of changes
  - List all new files created
  - Document any breaking changes or migration steps
  - _Requirements: 8.4_

### Task 16: Code Review and Cleanup
@task Task16

- [ ] 16.1 Review backend code quality
  - Verify all methods have JavaDoc comments
  - Verify proper exception handling
  - Verify transaction boundaries are correct
  - Remove any debug logging or commented code
  - _Requirements: 8.1, 8.2, 8.4_

- [ ] 16.2 Review frontend code quality
  - Verify all functions have JSDoc comments
  - Verify consistent code style
  - Remove any console.log statements (except intentional logging)
  - Remove any commented code
  - _Requirements: 8.1, 8.2, 8.4_

- [ ] 16.3 Verify no regressions
  - Test existing File Explorer functionality still works
  - Test file upload still works
  - Test file download still works
  - Test folder navigation still works
  - Test breadcrumb navigation still works
  - _Requirements: 8.1_

---

## Summary

This implementation plan consists of **16 major tasks** with **71 sub-tasks** (all required for comprehensive implementation).

**Estimated Timeline**:
- Phase 1-3 (Backend): 3-4 days
- Phase 4-7 (Frontend Core): 3-4 days
- Phase 8 (Integration): 2-3 days
- Phase 9-10 (Testing & Docs): 2-3 days

**Total Estimated Time**: 10-14 days

**Key Deliverables**:
1. FolderService with auto-provisioning logic
2. Enhanced ProfessorService and CourseAssignmentService
3. FileExplorerState module for centralized state management
4. Enhanced FileExplorer component with stable loading states
5. Integrated dashboards (Dean, Professor, HOD)
6. Comprehensive tests and documentation
