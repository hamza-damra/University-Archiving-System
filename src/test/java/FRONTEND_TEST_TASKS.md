# Frontend Test Implementation Tasks

## Overview
This file contains all frontend test implementation tasks organized by priority. Each task includes the module to test, test scenarios to implement, test types (Unit/Integration/E2E), and estimated test count.

**Framework Recommendation:** Jest + Testing Library for unit tests, Playwright/Cypress for E2E tests  
**Target Coverage:** ~250+ test cases across all frontend modules

---

## 🔴 Priority 1: Core Authentication & API Module (Critical Security)

### Task 1.1: Test Authentication Flow (`auth/auth.js`) ✅ COMPLETED
**File:** `src/test/frontend/auth/auth.test.js`  
**Estimated Tests:** 25  
**Priority:** CRITICAL  
**Test Type:** Unit + E2E  
**Status:** COMPLETED

**Unit Tests:**
- [x] Test: `redirectToDashboard` - routes correctly for ROLE_ADMIN
- [x] Test: `redirectToDashboard` - routes correctly for ROLE_DEANSHIP
- [x] Test: `redirectToDashboard` - routes correctly for ROLE_HOD
- [x] Test: `redirectToDashboard` - routes correctly for ROLE_PROFESSOR
- [x] Test: `redirectToDashboard` - shows error for unknown role
- [x] Test: `showLoginPage` - hides overlay and shows login content
- [x] Test: `checkExistingAuth` - redirects if already authenticated with valid token
- [x] Test: `checkExistingAuth` - clears invalid/expired auth data and shows login
- [x] Test: `checkExistingAuth` - handles missing userInfo gracefully
- [x] Test: Form validation - shows error for empty email
- [x] Test: Form validation - shows error for invalid email format
- [x] Test: Form validation - shows error for empty password
- [x] Test: `isValidEmail` - accepts valid email formats
- [x] Test: `isValidEmail` - rejects invalid email formats
- [x] Test: Login success - saves auth data and redirects
- [x] Test: Login failure - shows error message for invalid credentials
- [x] Test: Login failure - handles ACCOUNT_DISABLED error
- [x] Test: Login failure - handles ACCOUNT_LOCKED error
- [x] Test: Rate limiting - starts countdown on RATE_LIMIT_EXCEEDED
- [x] Test: `startRateLimitCountdown` - disables button during countdown
- [x] Test: `startRateLimitCountdown` - updates error message with time
- [x] Test: `startRateLimitCountdown` - re-enables button after countdown
- [x] Test: `isRateLimited` - returns correct state
- [x] Test: `setButtonLoadingState` - shows spinner during loading
- [x] Test: `setButtonSuccessState` - shows success checkmark

---

### Task 1.2: Test Auth Guard (`auth/auth-guard.js`) ✅ COMPLETED
**File:** `src/test/frontend/auth/auth-guard.test.js`  
**Estimated Tests:** 18  
**Priority:** CRITICAL  
**Test Type:** Unit + Integration

**Tasks:**
- [x] Test: Skips guard on login page (`/` and `/index.html`)
- [x] Test: Redirects to login when no token present
- [x] Test: Redirects to login when no userInfo present
- [x] Test: Redirects with error when userInfo is invalid JSON
- [x] Test: Redirects with error when userInfo missing required fields (role, email)
- [x] Test: Role-based access - admin pages require ROLE_ADMIN
- [x] Test: Role-based access - deanship pages allow ROLE_DEANSHIP and ROLE_ADMIN
- [x] Test: Role-based access - HOD pages allow ROLE_HOD and ROLE_ADMIN
- [x] Test: Role-based access - professor pages allow ROLE_PROFESSOR and ROLE_ADMIN
- [x] Test: `validateAndProceed` - validates token with server
- [x] Test: `validateAndProceed` - handles successful token validation
- [x] Test: `validateAndProceed` - attempts refresh on invalid token
- [x] Test: `validateAndProceed` - handles validation timeout (10s)
- [x] Test: `attemptRefresh` - updates token on successful refresh
- [x] Test: `attemptRefresh` - redirects on refresh failure
- [x] Test: `clearAndRedirect` - clears all localStorage auth items
- [x] Test: `clearAndRedirect` - uses replace for navigation (no back button)
- [x] Test: Integration - complete auth validation flow

---

### Task 1.3: Test Core API Module (`core/api.js`) ✅ COMPLETED
**File:** `src/test/frontend/core/api.test.js`  
**Estimated Tests:** 45  
**Priority:** CRITICAL  
**Test Type:** Unit + Integration  
**Status:** COMPLETED

**Token Management:**
- [x] Test: `getToken` - retrieves token from localStorage
- [x] Test: `getRefreshToken` - retrieves refresh token from localStorage
- [x] Test: `getUserInfo` - parses and returns userInfo from localStorage
- [x] Test: `getUserInfo` - returns null when userInfo is empty
- [x] Test: `saveAuthData` - stores token, userInfo, and refreshToken
- [x] Test: `clearAuthData` - removes all auth data
- [x] Test: `clearAuthData` - clears role-specific cached data (admin, deanship, HOD, professor)
- [x] Test: `isAuthenticated` - returns true when token exists
- [x] Test: `isAuthenticated` - returns false when token missing
- [x] Test: `redirectToLogin` - clears auth data and redirects

**Token Refresh:**
- [x] Test: `attemptTokenRefresh` - returns null when no refresh token
- [x] Test: `attemptTokenRefresh` - prevents multiple simultaneous refresh attempts
- [x] Test: `attemptTokenRefresh` - waits for ongoing refresh when already refreshing
- [x] Test: `attemptTokenRefresh` - updates access token on success
- [x] Test: `attemptTokenRefresh` - returns null on failure
- [x] Test: `validateToken` - returns NO_TOKEN status when no token
- [x] Test: `validateToken` - returns valid status for valid token
- [x] Test: `validateToken` - handles network errors gracefully

**API Request:**
- [x] Test: `apiRequest` - adds Authorization header when token exists
- [x] Test: `apiRequest` - removes Content-Type for FormData
- [x] Test: `apiRequest` - handles 401 with token refresh and retry
- [x] Test: `apiRequest` - redirects to login after failed retry
- [x] Test: `apiRequest` - handles 429 rate limit errors
- [x] Test: `apiRequest` - handles 403 forbidden errors
- [x] Test: `apiRequest` - handles 400 validation errors
- [x] Test: `apiRequest` - extracts data from ApiResponse wrapper
- [x] Test: `apiRequest` - handles network errors
- [x] Test: `formatValidationErrors` - formats errors correctly
- [x] Test: `getErrorMessage` - extracts formatted message from errors

**File Upload:**
- [x] Test: `uploadFile` - sends FormData with correct headers
- [x] Test: `uploadFile` - tracks upload progress
- [x] Test: `uploadFile` - handles 401 with token refresh
- [x] Test: `uploadFile` - handles upload errors
- [x] Test: `uploadFile` - handles upload cancellation

**API Endpoints (Integration):**
- [x] Test: `auth.login` - sends correct request
- [x] Test: `auth.logout` - sends correct request
- [x] Test: `auth.refreshToken` - sends correct request
- [x] Test: `hod.getAcademicYears` - returns academic years
- [x] Test: `hod.getDashboardOverview` - passes semesterId
- [x] Test: `professor.getMyCourses` - passes semesterId
- [x] Test: `professor.uploadFiles` - uploads with correct params
- [x] Test: `deanship.getCourseAssignments` - passes filter params
- [x] Test: `fileExplorer.getRoot` - passes academic year and semester
- [x] Test: `fileExplorer.downloadFile` - returns blob response

---

## 🟡 Priority 2: File Explorer Components (High User Impact)

### Task 2.1: Test File Explorer Core (`file-explorer/file-explorer.js`) ✅ COMPLETED
**File:** `src/test/frontend/file-explorer/file-explorer.test.js`  
**Estimated Tests:** 30  
**Priority:** HIGH  
**Test Type:** Unit + Integration  
**Status:** COMPLETED

**Initialization:**
- [x] Test: Constructor sets correct default options
- [x] Test: Constructor respects custom options (role, readOnly, showDepartmentContext)
- [x] Test: `init` creates correct DOM structure
- [x] Test: `init` shows loading state initially

**Data Loading:**
- [x] Test: `loadRoot` - fetches root node for academic year/semester
- [x] Test: `loadRoot` - renders folder cards correctly
- [x] Test: `loadRoot` - handles empty folder list
- [x] Test: `loadRoot` - handles API errors gracefully
- [x] Test: `loadNode` - fetches node by path
- [x] Test: `loadNode` - updates breadcrumbs correctly
- [x] Test: `loadNode` - renders files in table

**Navigation:**
- [x] Test: Folder card click navigates to folder
- [x] Test: Breadcrumb click navigates to ancestor
- [x] Test: Back button navigates to previous folder
- [x] Test: Up button navigates to parent folder

**File Operations (when not readOnly):**
- [x] Test: Upload button visible when not readOnly
- [x] Test: Upload button hidden when readOnly
- [x] Test: File upload triggers API call
- [x] Test: File download triggers download

**Rendering:**
- [x] Test: `renderFolderCards` - creates correct card structure
- [x] Test: `renderFolderCards` - shows folder icon and name
- [x] Test: `renderFilesTable` - creates table with correct columns
- [x] Test: `renderFilesTable` - shows file metadata (name, size, date)
- [x] Test: `renderFilesTable` - shows empty state when no files
- [x] Test: `renderBreadcrumbs` - shows home icon at start
- [x] Test: `renderBreadcrumbs` - shows separator between items
- [x] Test: Preview button appears for previewable files
- [x] Test: Download button appears for all files

**Role-Specific Behavior:**
- [x] Test: PROFESSOR role - shows upload controls
- [x] Test: HOD role - read-only mode, no upload controls
- [x] Test: DEANSHIP role - read-only mode, department context
- [x] Test: ADMIN role - full access to all files

---

### Task 2.2: Test Modern File Browser (`file-explorer/modern-file-browser.js`) ✅ COMPLETED
**File:** `src/test/frontend/file-explorer/modern-file-browser.test.js`  
**Estimated Tests:** 22  
**Priority:** HIGH  
**Test Type:** Unit + Integration

**Initialization:**
- [x] Test: Constructor creates correct layout
- [x] Test: Loads view mode from localStorage
- [x] Test: Loads sidebar visibility from localStorage
- [x] Test: Binds all event handlers

**View Modes:**
- [x] Test: Grid view displays files as cards
- [x] Test: List view displays files in table
- [x] Test: Toggle view mode updates localStorage
- [x] Test: View mode persists across page loads

**Navigation:**
- [x] Test: History navigation (back/forward buttons)
- [x] Test: Back button disabled at start
- [x] Test: Forward button disabled without forward history
- [x] Test: Up button navigates to parent
- [x] Test: Quick access sidebar navigation

**Sidebar:**
- [x] Test: Toggle sidebar visibility
- [x] Test: Sidebar state persists in localStorage
- [x] Test: Quick access items navigate correctly

**File/Folder Interactions:**
- [x] Test: Folder click navigates into folder
- [x] Test: File click opens preview
- [x] Test: Right-click context menu (if implemented) - Note: Not implemented in current codebase

**Status Bar:**
- [x] Test: Shows correct item count
- [x] Test: Shows selected item count - Note: Selected count feature not fully implemented in current codebase
- [x] Test: Updates on navigation

**Loading States:**
- [x] Test: Shows loading indicator during fetch
- [x] Test: Shows empty state message
- [x] Test: Shows error state on failure

---

### Task 2.3: Test File Preview Modal (`file-explorer/file-preview-modal.js`) ✅ COMPLETED
**File:** `src/test/frontend/file-explorer/file-preview-modal.test.js`  
**Estimated Tests:** 28  
**Priority:** HIGH  
**Test Type:** Unit + Integration  
**Status:** COMPLETED

**Modal Lifecycle:**
- [x] Test: `open` creates modal DOM element
- [x] Test: `open` shows loading state initially
- [x] Test: `open` fetches file metadata
- [x] Test: `open` loads file content
- [x] Test: `open` dispatches 'preview:opened' event
- [x] Test: `open` stores previously focused element
- [x] Test: `close` removes modal from DOM
- [x] Test: `close` dispatches 'preview:closed' event
- [x] Test: `close` restores focus to previous element
- [x] Test: Opening new modal closes existing one

**Content Rendering:**
- [x] Test: Text files render with TextRenderer
- [x] Test: PDF files render with PDFRenderer
- [x] Test: Code files render with CodeRenderer
- [x] Test: Office files render with OfficeRenderer
- [x] Test: Large file warning shown (>5MB)

**Metadata Display:**
- [x] Test: Shows file name in header
- [x] Test: Shows file size
- [x] Test: Shows upload date
- [x] Test: Shows MIME type

**User Actions:**
- [x] Test: Download button triggers download
- [x] Test: Close button closes modal
- [x] Test: Click outside modal closes it
- [x] Test: Escape key closes modal

**Accessibility:**
- [x] Test: Focus trap works correctly
- [x] Test: Tab cycles through focusable elements
- [x] Test: Screen reader announcements work
- [x] Test: Keyboard navigation functions properly

**Drag Functionality:**
- [x] Test: Modal can be dragged by header
- [x] Test: Drag state tracks correctly
- [x] Test: Drag end releases properly

---

### Task 2.4: Test File Explorer State (`file-explorer/file-explorer-state.js`) ✅ COMPLETED
**File:** `src/test/frontend/file-explorer/file-explorer-state.test.js`  
**Estimated Tests:** 12  
**Priority:** MEDIUM  
**Test Type:** Unit  
**Status:** COMPLETED

**State Management:**
- [x] Test: Initial state is correct
- [x] Test: `setContext` updates academic year and semester
- [x] Test: `setCurrentNode` updates path (note: method is `setCurrentNode`, not `setCurrentPath`)
- [x] Test: `getContext` returns current context
- [x] Test: State persists required values

**Listeners:**
- [x] Test: `subscribe` registers callback (note: method is `subscribe`, not `addListener`)
- [x] Test: `unsubscribe` unregisters callback (note: method is `unsubscribe` via returned function, not `removeListener`)
- [x] Test: Listeners notified on state change
- [x] Test: Multiple listeners receive updates

**History:**
- [x] Test: State reset on context change (note: history tracking not implemented in state, but state reset functionality tested)
- [x] Test: `resetData` clears data while preserving context
- [x] Test: `reset` clears all state including context

---

## 🟢 Priority 3: Dashboard Components

### Task 3.1: Test HOD Dashboard (`shared/hod.js`) ✅ COMPLETED
**File:** `src/test/frontend/dashboards/hod-dashboard.test.js`  
**Estimated Tests:** 25  
**Priority:** MEDIUM  
**Test Type:** Unit + Integration  
**Status:** COMPLETED

**Authentication:**
- [x] Test: Redirects if not authenticated
- [x] Test: Validates token with server
- [x] Test: Fetches fresh user info
- [x] Test: Rejects non-HOD role
- [x] Test: Initializes dashboard after auth

**Academic Year Selection:**
- [x] Test: `loadAcademicYears` - populates dropdown
- [x] Test: `loadAcademicYears` - selects current year based on Palestine timezone
- [x] Test: `loadAcademicYears` - falls back to active year
- [x] Test: `loadSemesters` - populates semester dropdown
- [x] Test: `getCurrentAcademicYearCode` - calculates correctly
- [x] Test: `getCurrentSemesterType` - returns FIRST/SECOND/SUMMER correctly

**Dashboard Overview:**
- [x] Test: Displays total professors count
- [x] Test: Displays total courses count
- [x] Test: Displays submitted documents count
- [x] Test: Displays missing documents count
- [x] Test: Displays overdue documents count

**Submission Status:**
- [x] Test: `loadSubmissionStatus` - fetches with filters
- [x] Test: `renderSubmissionStatus` - creates correct table rows
- [x] Test: `getStatusBadgeNew` - returns correct badge for UPLOADED
- [x] Test: `getStatusBadgeNew` - returns correct badge for NOT_UPLOADED
- [x] Test: `getStatusBadgeNew` - returns correct badge for OVERDUE
- [x] Test: Filter dropdowns trigger reload

**Tab Switching:**
- [x] Test: `switchTab` - updates active tab styling
- [x] Test: `switchTab` - shows correct tab content
- [x] Test: `switchTab` - loads data for specific tabs

**Reports:**
- [x] Test: View report button shows modal
- [x] Test: Download report button triggers PDF download

---

### Task 3.2: Test Professor Dashboard (`shared/prof.js`) ✅ COMPLETED
**File:** `src/test/frontend/dashboards/professor-dashboard.test.js`  
**Estimated Tests:** 28  
**Priority:** MEDIUM  
**Test Type:** Unit + Integration  
**Status:** COMPLETED

**Authentication:**
- [x] Test: Redirects if not authenticated
- [x] Test: Validates ROLE_PROFESSOR
- [x] Test: Displays professor name

**Course Display:**
- [x] Test: `loadCourses` - fetches courses for semester
- [x] Test: `renderCourseCards` - creates course cards
- [x] Test: Course card shows course code and name
- [x] Test: Course card shows document type counts
- [x] Test: Course card expands on click

**Document Upload:**
- [x] Test: Upload button opens file picker
- [x] Test: File selection triggers upload
- [x] Test: Upload progress shown
- [x] Test: Upload success shows toast
- [x] Test: Upload error handled gracefully
- [x] Test: Invalid file type rejected
- [x] Test: File size limit enforced

**Document Management:**
- [x] Test: Document list displays correctly
- [x] Test: Document status badges shown
- [x] Test: Download document works
- [x] Test: Replace document works
- [x] Test: Delete document (if allowed)

**Dashboard Overview:**
- [x] Test: Shows course count
- [x] Test: Shows submission status
- [x] Test: Shows deadlines

**Notifications:**
- [x] Test: Loads notifications
- [x] Test: Marks notification as seen
- [x] Test: Shows notification badge

**File Explorer Integration:**
- [x] Test: File explorer initializes
- [x] Test: Navigation works
- [x] Test: Upload integrated with explorer

---

### Task 3.3: Test Deanship Dashboard (`deanship/deanship.js`) ✅ COMPLETED
**File:** `src/test/frontend/dashboards/deanship-dashboard.test.js`  
**Estimated Tests:** 35  
**Priority:** MEDIUM  
**Test Type:** Unit + Integration  
**Status:** COMPLETED

**Authentication:**
- [x] Test: Redirects if not authenticated
- [x] Test: Validates ROLE_DEANSHIP
- [x] Test: Fetches fresh user info from server
- [x] Test: Handles role mismatch gracefully

**Tab Management:**
- [x] Test: Restores active tab from localStorage
- [x] Test: `switchTab` updates state
- [x] Test: Tab persistence across page loads
- [x] Test: Correct content shown per tab

**Academic Years:**
- [x] Test: Load academic years list
- [x] Test: Create new academic year
- [x] Test: Update academic year
- [x] Test: Activate academic year
- [x] Test: Load semesters for year
- [x] Test: Update semester

**Professor Management:**
- [x] Test: Load professors list
- [x] Test: Filter by department
- [x] Test: Create new professor
- [x] Test: Update professor
- [x] Test: Deactivate professor
- [x] Test: Activate professor

**Course Management:**
- [x] Test: Load courses list
- [x] Test: Filter by department
- [x] Test: Create new course
- [x] Test: Update course
- [x] Test: Deactivate course

**Course Assignments:**
- [x] Test: Load assignments for semester
- [x] Test: Filter by professor
- [x] Test: Create new assignment
- [x] Test: Delete assignment

**Reports:**
- [x] Test: Load system-wide report
- [x] Test: Export report to PDF

**File Explorer:**
- [x] Test: Initialize file explorer
- [x] Test: Load data for selected semester
- [x] Test: Read-only mode enforced

**Modern Dropdowns:**
- [x] Test: Initialize modern dropdowns
- [x] Test: Refresh dropdowns on data change

---

### Task 3.4: Test Admin Dashboard (`admin/admin-dashboard.js`) ✅ COMPLETED
**File:** `src/test/frontend/dashboards/admin-dashboard.test.js`  
**Estimated Tests:** 20  
**Priority:** MEDIUM  
**Test Type:** Unit + Integration  
**Status:** COMPLETED

**Authentication:**
- [x] Test: Validates ROLE_ADMIN
- [x] Test: Redirects non-admin users

**User Management:**
- [x] Test: Load all users
- [x] Test: Filter users by role
- [x] Test: Create new user
- [x] Test: Update user
- [x] Test: Deactivate user
- [x] Test: Reset user password

**Department Management:**
- [x] Test: Load departments
- [x] Test: Create department
- [x] Test: Update department
- [x] Test: Delete department

**System Settings:**
- [x] Test: Load system configuration
- [x] Test: Update settings
- [x] Test: Validate settings before save

**Dashboard Stats:**
- [x] Test: Display total users
- [x] Test: Display total courses
- [x] Test: Display system health

**Reports:**
- [x] Test: Generate admin reports
- [x] Test: Export reports

---

## 🔵 Priority 4: UI Components & Utilities

### Task 4.1: Test UI Helper Functions (`core/ui.js`) ✅ COMPLETED
**File:** `src/test/frontend/core/ui.test.js`  
**Estimated Tests:** 22  
**Priority:** MEDIUM  
**Test Type:** Unit  
**Status:** COMPLETED

**Loading Utilities:**
- [x] Test: `withMinLoadingTime` - waits minimum time
- [x] Test: `withMinLoadingTime` - doesn't wait if operation takes longer
- [x] Test: `withMinLoadingTime` - handles promises
- [x] Test: `withMinLoadingTime` - handles async functions

**Button Loading:**
- [x] Test: `setButtonLoading` - disables button
- [x] Test: `setButtonLoading` - shows spinner HTML
- [x] Test: `setButtonLoading` - stores original state
- [x] Test: `restore` function returns button to original state
- [x] Test: `withButtonLoading` - wraps async function

**Form Submit Handler:**
- [x] Test: `createFormSubmitHandler` - prevents default
- [x] Test: `createFormSubmitHandler` - runs validation
- [x] Test: `createFormSubmitHandler` - shows loading state
- [x] Test: `createFormSubmitHandler` - calls onSubmit

**Modal Functions:**
- [x] Test: `showModal` - creates modal DOM
- [x] Test: `showModal` - sets title and content
- [x] Test: `showModal` - handles close action
- [x] Test: `showConfirm` - shows confirm/cancel buttons
- [x] Test: `showConfirm` - resolves on confirm
- [x] Test: `showConfirm` - rejects on cancel

**Toast Notifications:**
- [x] Test: `showToast` - creates toast element
- [x] Test: `showToast` - applies correct type styling
- [x] Test: `showToast` - auto-removes after timeout

**Utility Functions:**
- [x] Test: `formatDate` - formats dates correctly

---

### Task 4.2: Test Toast Notifications (`core/toast-notifications.js`) ✅ COMPLETED
**File:** `src/test/frontend/core/toast-notifications.test.js`  
**Estimated Tests:** 15  
**Priority:** LOW  
**Test Type:** Unit  
**Status:** COMPLETED

**Toast Creation:**
- [x] Test: Creates toast container if not exists
- [x] Test: Creates toast with correct type (success, error, warning, info)
- [x] Test: Shows toast with message
- [x] Test: Shows toast with title

**Toast Behavior:**
- [x] Test: Auto-dismisses after duration
- [x] Test: Manual dismiss on click
- [x] Test: Multiple toasts stack correctly
- [x] Test: Animation on show/hide

**Toast Queue:**
- [x] Test: Maximum toasts limit enforced
- [x] Test: Old toasts removed when limit exceeded
- [x] Test: Queue processes correctly

**Styling:**
- [x] Test: Success toast has green styling
- [x] Test: Error toast has red styling
- [x] Test: Warning toast has yellow styling
- [x] Test: Info toast has blue styling

---

### Task 4.3: Test Modern Dropdown (`core/modern-dropdown.js`) ✅ COMPLETED
**File:** `src/test/frontend/core/modern-dropdown.test.js`  
**Estimated Tests:** 18  
**Priority:** LOW  
**Test Type:** Unit  
**Status:** COMPLETED

**Initialization:**
- [x] Test: Wraps native select element
- [x] Test: Creates custom dropdown DOM
- [x] Test: Syncs initial selected value
- [x] Test: Handles multiple select elements

**Interaction:**
- [x] Test: Click opens dropdown
- [x] Test: Click outside closes dropdown
- [x] Test: Option click selects value
- [x] Test: Keyboard navigation works
- [x] Test: Enter selects highlighted option
- [x] Test: Escape closes dropdown

**Sync:**
- [x] Test: `refresh` updates options from native select
- [x] Test: Programmatic select change syncs
- [x] Test: Dispatches change event

**Accessibility:**
- [x] Test: ARIA attributes set correctly
- [x] Test: Focus management works
- [x] Test: Screen reader compatible

**Edge Cases:**
- [x] Test: Empty select handled
- [x] Test: Disabled select handled
- [x] Test: Option groups supported

---

### Task 4.4: Test Theme Manager (`core/theme.js`) ✅ COMPLETED
**File:** `src/test/frontend/core/theme.test.js`  
**Estimated Tests:** 10  
**Priority:** LOW  
**Test Type:** Unit  
**Status:** COMPLETED

**Theme Detection:**
- [x] Test: Detects system preference
- [x] Test: Loads theme from localStorage
- [x] Test: Falls back to system preference

**Theme Toggle:**
- [x] Test: `toggle` switches between light and dark
- [x] Test: Adds/removes dark class on body
- [x] Test: Persists theme in localStorage

**Theme Application:**
- [x] Test: Applies on page load
- [x] Test: Listens for system preference changes
- [x] Test: Updates CSS custom properties

**Icon Toggle:**
- [x] Test: Shows sun icon in dark mode
- [x] Test: Shows moon icon in light mode

---

## 🟣 Priority 5: File Renderers

### Task 5.1: Test PDF Renderer (`renderers/pdf-renderer.js`) ✅ COMPLETED
**File:** `src/test/frontend/renderers/pdf-renderer.test.js`  
**Estimated Tests:** 15  
**Priority:** MEDIUM  
**Test Type:** Unit + Integration  
**Status:** COMPLETED

**Rendering:**
- [x] Test: Initializes PDF.js library
- [x] Test: Loads PDF document
- [x] Test: Renders pages to canvas
- [x] Test: Handles multi-page documents

**Navigation:**
- [x] Test: Next page navigation
- [x] Test: Previous page navigation
- [x] Test: Go to specific page
- [x] Test: Page indicator updates

**Zoom:**
- [x] Test: Zoom in increases scale (Note: Handled by browser's native PDF viewer)
- [x] Test: Zoom out decreases scale (Note: Handled by browser's native PDF viewer)
- [x] Test: Fit to width (Note: Handled by browser's native PDF viewer)
- [x] Test: Fit to page (Note: Handled by browser's native PDF viewer)

**Error Handling:**
- [x] Test: Invalid PDF shows error
- [x] Test: Loading error handled
- [x] Test: Empty document handled

---

### Task 5.2: Test Text/Code Renderer (`renderers/text-renderer.js`, `renderers/code-renderer.js`) ✅ COMPLETED
**File:** `src/test/frontend/renderers/text-renderer.test.js`, `src/test/frontend/renderers/code-renderer.test.js`  
**Estimated Tests:** 12  
**Priority:** MEDIUM  
**Test Type:** Unit  
**Status:** COMPLETED

**Text Rendering:**
- [x] Test: Renders plain text
- [x] Test: Shows line numbers
- [x] Test: Handles large files
- [x] Test: Word wrap option

**Code Rendering:**
- [x] Test: Syntax highlighting applied
- [x] Test: Detects language from extension
- [x] Test: Line numbers shown
- [x] Test: Theme applied (light/dark)

**Search:**
- [x] Test: Find text in content
- [x] Test: Highlight matches
- [x] Test: Navigate between matches
- [x] Test: Case-sensitive option

---

### Task 5.3: Test Office Renderer (`renderers/office-renderer.js`) ✅ COMPLETED
**File:** `src/test/frontend/renderers/office-renderer.test.js`  
**Estimated Tests:** 10  
**Priority:** LOW  
**Test Type:** Unit + Integration  
**Status:** COMPLETED

**Document Rendering:**
- [x] Test: Renders Word documents (.docx)
- [x] Test: Renders Excel spreadsheets (.xlsx)
- [x] Test: Renders PowerPoint presentations (.pptx)
- [x] Test: Handles legacy formats (.doc, .xls, .ppt)

**Error Handling:**
- [x] Test: Corrupt file shows error
- [x] Test: Unsupported format message
- [x] Test: Loading timeout handled

**Display:**
- [x] Test: Correct styling applied
- [x] Test: Responsive layout
- [x] Test: Print-friendly rendering

---

## ⚪ Priority 6: E2E Test Scenarios

### Task 6.1: Authentication E2E Tests ✅ COMPLETED
**File:** `src/test/frontend/e2e/auth.e2e.test.js`  
**Estimated Tests:** 12  
**Priority:** MEDIUM  
**Test Type:** E2E (Playwright/Cypress)  
**Status:** COMPLETED

**Login Flow:**
- [x] Test: Complete login with valid credentials
- [x] Test: Login failure with invalid credentials
- [x] Test: Role-based redirect after login
- [x] Test: Remember session across page refresh
- [x] Test: Rate limiting prevents brute force

**Logout Flow:**
- [x] Test: Logout clears session
- [x] Test: Cannot access protected pages after logout
- [x] Test: Redirect to login with error message

**Session Management:**
- [x] Test: Token refresh on expiration
- [x] Test: Redirect on refresh failure
- [x] Test: Multiple tab session handling
- [x] Test: Session timeout handling

---

### Task 6.2: File Upload E2E Tests ✅ COMPLETED
**File:** `src/test/frontend/e2e/file-upload.e2e.test.js`  
**Estimated Tests:** 15  
**Priority:** MEDIUM  
**Test Type:** E2E (Playwright/Cypress)

**Upload Flow:**
- [x] Test: Single file upload
- [x] Test: Multiple file upload
- [x] Test: Progress indicator shows correctly
- [x] Test: Success notification shown
- [x] Test: File appears in list after upload

**Validation:**
- [x] Test: Reject invalid file types
- [x] Test: Reject files exceeding size limit
- [x] Test: Show validation error messages

**Error Handling:**
- [x] Test: Network error during upload
- [x] Test: Server error during upload
- [x] Test: Retry upload after error

**File Operations:**
- [x] Test: Download uploaded file
- [x] Test: Replace existing file
- [x] Test: Preview file after upload
- [x] Test: Delete file (if allowed)

---

### Task 6.3: File Explorer E2E Tests ✅ COMPLETED
**File:** `src/test/frontend/e2e/file-explorer.e2e.test.js`  
**Estimated Tests:** 18  
**Priority:** MEDIUM  
**Test Type:** E2E (Playwright/Cypress)  
**Status:** COMPLETED

**Navigation:**
- [x] Test: Navigate into folders
- [x] Test: Navigate using breadcrumbs
- [x] Test: Back/forward navigation
- [x] Test: Home button returns to root

**File Operations:**
- [x] Test: Preview PDF file
- [x] Test: Preview image file
- [x] Test: Preview text file
- [x] Test: Download file
- [x] Test: Upload file (professor role)

**Role-Based Access:**
- [x] Test: Professor can upload
- [x] Test: HOD read-only view
- [x] Test: Deanship can view all departments
- [x] Test: Admin full access

**View Modes:**
- [x] Test: Grid view displays correctly
- [x] Test: List view displays correctly
- [x] Test: Toggle view mode
- [x] Test: View preference persists

**Search/Filter:**
- [x] Test: Search files by name
- [x] Test: Filter by file type
- [x] Test: Sort by name/date/size

---

### Task 6.4: Dashboard E2E Tests ✅ COMPLETED
**File:** `src/test/frontend/e2e/dashboards.e2e.test.js`  
**Estimated Tests:** 20  
**Priority:** LOW  
**Test Type:** E2E (Playwright/Cypress)  
**Status:** COMPLETED

**HOD Dashboard:**
- [x] Test: Dashboard loads with correct statistics
- [x] Test: Academic year selection updates data
- [x] Test: Semester selection updates data
- [x] Test: View submission report
- [x] Test: Download PDF report

**Professor Dashboard:**
- [x] Test: Dashboard loads with courses
- [x] Test: Course card expand/collapse
- [x] Test: Upload document flow
- [x] Test: View notifications

**Deanship Dashboard:**
- [x] Test: Tab switching works
- [x] Test: Create/edit professor
- [x] Test: Create/edit course
- [x] Test: Manage course assignments
- [x] Test: Generate reports

**Admin Dashboard:**
- [x] Test: User management flow
- [x] Test: Department management
- [x] Test: System settings

---

## 📊 Progress Tracking

### Overall Progress
- **Total Tasks:** 26
- **Completed:** 26
- **In Progress:** 0
- **Pending:** 0

### By Priority
- **Priority 1 (Critical):** 3 tasks (~88 tests) - Authentication & API - **3/3 completed** ✅
- **Priority 2 (High):** 4 tasks (~92 tests) - File Explorer - **4/4 completed** ✅
- **Priority 3 (Medium):** 4 tasks (~108 tests) - Dashboards - **4/4 completed** ✅
- **Priority 4 (Medium):** 4 tasks (~65 tests) - UI Components - **4/4 completed** ✅
- **Priority 5 (Medium):** 3 tasks (~37 tests) - Renderers - **3/3 completed** ✅
- **Priority 6 (E2E):** 4 tasks (~65 tests) - End-to-End Scenarios - **4/4 completed** ✅

---

## Quick Reference

### Unit Test Template (Jest + Testing Library)
```javascript
/**
 * @jest-environment jsdom
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/dom';
import userEvent from '@testing-library/user-event';
import { functionToTest } from '../path/to/module.js';

describe('ModuleName', () => {
    beforeEach(() => {
        // Setup DOM or mocks
        document.body.innerHTML = '';
        localStorage.clear();
        jest.clearAllMocks();
    });

    afterEach(() => {
        // Cleanup
    });

    describe('functionToTest', () => {
        it('should do something successfully', async () => {
            // Arrange
            const mockData = { /* ... */ };
            
            // Act
            const result = await functionToTest(mockData);
            
            // Assert
            expect(result).toBeDefined();
        });

        it('should handle error case', async () => {
            // Arrange
            const invalidData = null;
            
            // Act & Assert
            await expect(functionToTest(invalidData)).rejects.toThrow();
        });
    });
});
```

### E2E Test Template (Playwright)
```javascript
import { test, expect } from '@playwright/test';

test.describe('Feature Name', () => {
    test.beforeEach(async ({ page }) => {
        // Setup - navigate to page, login if needed
        await page.goto('/login');
        await page.fill('#email', 'test@example.com');
        await page.fill('#password', 'password');
        await page.click('button[type="submit"]');
        await page.waitForURL('/dashboard');
    });

    test('should complete user flow', async ({ page }) => {
        // Arrange
        await page.goto('/target-page');

        // Act
        await page.click('button#action');
        await page.fill('input#field', 'value');

        // Assert
        await expect(page.locator('.success-message')).toBeVisible();
    });

    test('should handle error state', async ({ page }) => {
        // Test error scenario
        await page.goto('/target-page');
        await page.click('button#invalid-action');
        await expect(page.locator('.error-message')).toBeVisible();
    });
});
```

### Mock Fetch Template
```javascript
// Mock global fetch for API tests
global.fetch = jest.fn();

function mockFetchResponse(data, status = 200) {
    return Promise.resolve({
        ok: status >= 200 && status < 300,
        status,
        json: () => Promise.resolve(data),
        clone: () => ({
            json: () => Promise.resolve(data)
        })
    });
}

// In test
beforeEach(() => {
    fetch.mockClear();
});

it('should fetch data', async () => {
    fetch.mockImplementationOnce(() => 
        mockFetchResponse({ success: true, data: [] })
    );

    const result = await apiCall();
    
    expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/endpoint'),
        expect.objectContaining({ method: 'GET' })
    );
});
```

### LocalStorage Mock Template
```javascript
const localStorageMock = (() => {
    let store = {};
    return {
        getItem: jest.fn(key => store[key] || null),
        setItem: jest.fn((key, value) => { store[key] = value; }),
        removeItem: jest.fn(key => { delete store[key]; }),
        clear: jest.fn(() => { store = {}; }),
        get length() { return Object.keys(store).length; }
    };
})();

Object.defineProperty(window, 'localStorage', { value: localStorageMock });
```

---

## Testing Tools Setup

### Recommended Stack
```json
{
  "devDependencies": {
    "jest": "^29.7.0",
    "@testing-library/dom": "^9.3.0",
    "@testing-library/jest-dom": "^6.1.0",
    "@testing-library/user-event": "^14.5.0",
    "jest-environment-jsdom": "^29.7.0",
    "@playwright/test": "^1.40.0"
  }
}
```

### Jest Configuration
```javascript
// jest.config.js
module.exports = {
    testEnvironment: 'jsdom',
    moduleNameMapper: {
        '^(\\.{1,2}/.*)\\.js$': '$1'
    },
    transform: {
        '^.+\\.js$': 'babel-jest'
    },
    setupFilesAfterEnv: ['@testing-library/jest-dom'],
    testMatch: ['**/test/frontend/**/*.test.js'],
    collectCoverageFrom: [
        'src/main/resources/static/js/**/*.js',
        '!**/node_modules/**',
        '!**/lib/**'
    ]
};
```

### Playwright Configuration
```javascript
// playwright.config.js
import { defineConfig } from '@playwright/test';

export default defineConfig({
    testDir: './src/test/frontend/e2e',
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    use: {
        baseURL: 'http://localhost:8080',
        trace: 'on-first-retry',
    },
    webServer: {
        command: 'mvn spring-boot:run',
        url: 'http://localhost:8080',
        reuseExistingServer: !process.env.CI,
    },
});
```

---

## Notes

- Use mocking for external API calls in unit tests
- Use real backend in E2E tests (start server before tests)
- Test both success and error scenarios
- Test edge cases (empty data, null values, network errors)
- Test accessibility features where applicable
- Use data-testid attributes for reliable E2E selectors
- Mock localStorage for isolation in unit tests
- Test across different user roles
- Verify loading and error states are displayed correctly

---

## Estimated Timeline

- **Priority 1 (Critical):** 3-4 days
- **Priority 2 (High):** 3-4 days
- **Priority 3 (Medium):** 4-5 days
- **Priority 4 (Medium):** 2-3 days
- **Priority 5 (Medium):** 2 days
- **Priority 6 (E2E):** 3-4 days

**Total:** 17-22 days for complete coverage
