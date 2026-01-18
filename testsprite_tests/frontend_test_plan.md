# University Archiving System - Admin Panel Frontend Test Plan

## Test Environment Setup
- **URL**: http://localhost:8080/admin/dashboard.html
- **Browser**: Chrome, Firefox, Safari, Edge
- **Authentication**: JWT token stored in localStorage
- **Required Role**: ROLE_ADMIN
- **Test Data**: Seeded database with users, departments, and courses

## Prerequisites
1. Application running on http://localhost:8080
2. Valid admin account credentials
3. Test data populated in database
4. Modern browser with JavaScript enabled
5. Network connectivity to backend API

---

## Test Suite 1: Authentication & Access Control

### Test Case 1.1: Unauthenticated Access
**Test ID**: ADMIN-FE-AUTH-001
**Priority**: Critical
**Description**: Verify unauthenticated users cannot access admin dashboard
**Steps**:
1. Clear localStorage (remove auth token)
2. Navigate to /admin/dashboard.html
3. Observe redirect behavior

**Expected Result**:
- User is redirected to login page
- Dashboard does not load
- JWT guard prevents access

### Test Case 1.2: Wrong Role Access
**Test ID**: ADMIN-FE-AUTH-002
**Priority**: Critical
**Description**: Verify non-admin users cannot access admin dashboard
**Steps**:
1. Login as PROFESSOR or HOD
2. Attempt to navigate to /admin/dashboard.html
3. Observe response

**Expected Result**:
- Access denied or redirect to appropriate dashboard
- Error message displayed

### Test Case 1.3: Valid Admin Login
**Test ID**: ADMIN-FE-AUTH-003
**Priority**: Critical
**Description**: Verify admin user can access dashboard
**Steps**:
1. Navigate to login page
2. Enter admin credentials
3. Submit login form
4. Verify redirect to admin dashboard

**Expected Result**:
- Login successful
- JWT token stored in localStorage
- Redirected to /admin/dashboard.html
- Dashboard loads completely

### Test Case 1.4: Session Expiry
**Test ID**: ADMIN-FE-AUTH-004
**Priority**: High
**Description**: Verify behavior when JWT token expires
**Steps**:
1. Login as admin
2. Wait for token expiration (or manually expire)
3. Attempt API call (e.g., load users)
4. Observe response

**Expected Result**:
- 401 error received
- User redirected to login
- Error message displayed

### Test Case 1.5: Logout Functionality
**Test ID**: ADMIN-FE-AUTH-005
**Priority**: High
**Description**: Verify logout clears authentication
**Steps**:
1. Login as admin
2. Click logout button
3. Attempt to access dashboard

**Expected Result**:
- Token removed from localStorage
- Redirected to login page
- Cannot access dashboard

---

## Test Suite 2: Page Load & Initial Rendering

### Test Case 2.1: Dashboard Initial Load
**Test ID**: ADMIN-FE-LOAD-001
**Priority**: Critical
**Description**: Verify dashboard loads successfully
**Steps**:
1. Login as admin
2. Observe dashboard loading

**Expected Result**:
- Page loads without errors
- All assets (CSS, JS) load
- No console errors
- Statistics cards visible
- Navigation sidebar visible
- Dark mode toggle visible

### Test Case 2.2: Saved Tab State Restoration
**Test ID**: ADMIN-FE-LOAD-002
**Priority**: Medium
**Description**: Verify last active tab is restored on page refresh
**Steps**:
1. Navigate to Users tab
2. Refresh page (F5)
3. Observe active tab

**Expected Result**:
- Users tab is active after refresh
- Page title shows "User Management"
- No flash of wrong tab

### Test Case 2.3: Dark Mode Persistence
**Test ID**: ADMIN-FE-LOAD-003
**Priority**: Low
**Description**: Verify dark mode preference persists
**Steps**:
1. Enable dark mode
2. Refresh page
3. Observe theme

**Expected Result**:
- Dark mode remains active
- All components render in dark theme
- Toggle shows correct state

### Test Case 2.4: Responsive Sidebar
**Test ID**: ADMIN-FE-LOAD-004
**Priority**: Medium
**Description**: Verify sidebar behavior on different screen sizes
**Steps**:
1. Load dashboard on desktop (>1024px)
2. Resize to tablet (768-1024px)
3. Resize to mobile (<768px)

**Expected Result**:
- Desktop: Sidebar always visible
- Tablet/Mobile: Sidebar collapses
- Hamburger menu appears on mobile
- Content adjusts appropriately

---

## Test Suite 3: Dashboard Tab

### Test Case 3.1: Statistics Cards Display
**Test ID**: ADMIN-FE-DASH-001
**Priority**: High
**Description**: Verify statistics cards show correct data
**Steps**:
1. Navigate to Dashboard tab
2. Observe statistics cards
3. Verify numbers against database

**Expected Result**:
- Total Users count displayed
- Total Departments count displayed
- Total Courses count displayed
- Total Files count displayed
- Numbers match database

### Test Case 3.2: Charts Rendering
**Test ID**: ADMIN-FE-DASH-002
**Priority**: Medium
**Description**: Verify charts render properly
**Steps**:
1. Navigate to Dashboard tab
2. Observe chart area
3. Check for chart library initialization

**Expected Result**:
- Charts load without errors
- Data visualized correctly
- Legends and labels visible
- Interactive elements work

### Test Case 3.3: Dashboard Filters
**Test ID**: ADMIN-FE-DASH-003
**Priority**: Medium
**Description**: Verify dashboard filters work
**Steps**:
1. Navigate to Dashboard tab
2. Select academic year filter
3. Select semester filter
4. Observe data update

**Expected Result**:
- Filters populate from API
- Statistics update based on filters
- Charts update accordingly
- Loading indicator shown during update

### Test Case 3.4: Quick Actions
**Test ID**: ADMIN-FE-DASH-004
**Priority**: Low
**Description**: Verify quick action buttons work
**Steps**:
1. Navigate to Dashboard tab
2. Click quick action buttons
3. Verify navigation or modal opens

**Expected Result**:
- Buttons clickable
- Navigate to appropriate tab or open modal
- Actions execute correctly

---

## Test Suite 4: User Management Tab

### Test Case 4.1: User List Display
**Test ID**: ADMIN-FE-USER-001
**Priority**: Critical
**Description**: Verify user list loads and displays
**Steps**:
1. Navigate to Users tab
2. Observe user table

**Expected Result**:
- Table loads with users
- Columns: Name, Email, Role, Department, Status, Actions
- Data matches API response
- Loading indicator shown while fetching

### Test Case 4.2: User Pagination
**Test ID**: ADMIN-FE-USER-002
**Priority**: High
**Description**: Verify pagination controls work
**Steps**:
1. Navigate to Users tab
2. Click next page button
3. Click previous page button
4. Select different page size

**Expected Result**:
- Page changes correctly
- Data updates
- Current page highlighted
- Page size dropdown works
- Total pages calculated correctly

### Test Case 4.3: User Role Filter
**Test ID**: ADMIN-FE-USER-003
**Priority**: High
**Description**: Verify role filter works
**Steps**:
1. Navigate to Users tab
2. Select "PROFESSOR" from role filter
3. Observe filtered results

**Expected Result**:
- Dropdown shows all roles
- Only professors displayed
- Count updates
- Pagination resets to page 1

### Test Case 4.4: User Department Filter
**Test ID**: ADMIN-FE-USER-004
**Priority**: High
**Description**: Verify department filter works
**Steps**:
1. Navigate to Users tab
2. Select a department from filter
3. Observe filtered results

**Expected Result**:
- Dropdown shows all departments
- Only users from selected department
- Filter indicator visible
- Clear filter option available

### Test Case 4.5: User Active Status Filter
**Test ID**: ADMIN-FE-USER-005
**Priority**: Medium
**Description**: Verify active status filter works
**Steps**:
1. Navigate to Users tab
2. Toggle "Active Only" filter
3. Observe filtered results

**Expected Result**:
- Only active users shown when toggled
- All users shown when untoggled
- Filter state clearly indicated

### Test Case 4.6: Create User Modal - Open
**Test ID**: ADMIN-FE-USER-006
**Priority**: Critical
**Description**: Verify create user modal opens
**Steps**:
1. Navigate to Users tab
2. Click "Add User" or "Create User" button
3. Observe modal

**Expected Result**:
- Modal opens with form
- All fields visible (First Name, Last Name, Email, Password, Role, Department)
- Role dropdown populated
- Department dropdown populated
- Submit and Cancel buttons visible

### Test Case 4.7: Create User - Form Validation
**Test ID**: ADMIN-FE-USER-007
**Priority**: High
**Description**: Verify form validation on create user
**Steps**:
1. Open create user modal
2. Leave fields empty and click submit
3. Observe validation errors
4. Enter invalid email format
5. Observe validation

**Expected Result**:
- Required field errors shown
- Email format validated
- Password requirements shown
- Submit disabled until valid
- Error messages clear and helpful

### Test Case 4.8: Create User - Success
**Test ID**: ADMIN-FE-USER-008
**Priority**: Critical
**Description**: Verify user creation succeeds
**Steps**:
1. Open create user modal
2. Fill all fields with valid data
3. Click submit
4. Observe result

**Expected Result**:
- Loading indicator on submit button
- Success message/toast appears
- Modal closes
- User list refreshes
- New user appears in table

### Test Case 4.9: Create User - Duplicate Email
**Test ID**: ADMIN-FE-USER-009
**Priority**: High
**Description**: Verify duplicate email error handling
**Steps**:
1. Open create user modal
2. Enter email that already exists
3. Fill other fields
4. Submit form

**Expected Result**:
- API error received
- Error message displayed
- Modal remains open
- Form data preserved
- User can correct and retry

### Test Case 4.10: Create User - Cancel
**Test ID**: ADMIN-FE-USER-010
**Priority**: Medium
**Description**: Verify cancel button works
**Steps**:
1. Open create user modal
2. Enter some data
3. Click Cancel

**Expected Result**:
- Modal closes
- Form data cleared
- No user created
- User list unchanged

### Test Case 4.11: Edit User Modal - Open
**Test ID**: ADMIN-FE-USER-011
**Priority**: Critical
**Description**: Verify edit user modal opens with data
**Steps**:
1. Navigate to Users tab
2. Click edit icon on a user
3. Observe modal

**Expected Result**:
- Modal opens
- Form fields pre-filled with user data
- All current values displayed
- Password field empty (not showing current password)
- Save and Cancel buttons visible

### Test Case 4.12: Edit User - Update Success
**Test ID**: ADMIN-FE-USER-012
**Priority**: Critical
**Description**: Verify user update succeeds
**Steps**:
1. Open edit user modal
2. Modify first name
3. Click Save

**Expected Result**:
- Loading indicator shown
- Success message appears
- Modal closes
- User list refreshes
- Updated data visible in table

### Test Case 4.13: Edit User - Password Update
**Test ID**: ADMIN-FE-USER-013
**Priority**: High
**Description**: Verify password can be updated separately
**Steps**:
1. Open edit user modal
2. Click "Change Password" option
3. Enter new password
4. Submit

**Expected Result**:
- Password field becomes editable
- New password validated
- Update succeeds
- Success message shown

### Test Case 4.14: Delete User - Confirmation
**Test ID**: ADMIN-FE-USER-014
**Priority**: Critical
**Description**: Verify delete confirmation dialog
**Steps**:
1. Navigate to Users tab
2. Click delete icon on a user
3. Observe confirmation dialog

**Expected Result**:
- Confirmation dialog appears
- User name/email shown
- Warning message displayed
- Confirm and Cancel buttons
- No deletion until confirmed

### Test Case 4.15: Delete User - Success
**Test ID**: ADMIN-FE-USER-015
**Priority**: Critical
**Description**: Verify user deletion succeeds
**Steps**:
1. Click delete on a user
2. Confirm deletion
3. Observe result

**Expected Result**:
- Loading indicator shown
- Success message appears
- User removed from table
- Count decremented
- Table refreshes

### Test Case 4.16: Delete User - Cancel
**Test ID**: ADMIN-FE-USER-016
**Priority**: Medium
**Description**: Verify cancel on delete confirmation
**Steps**:
1. Click delete on a user
2. Click Cancel on confirmation

**Expected Result**:
- Dialog closes
- User not deleted
- Table unchanged

### Test Case 4.17: User Search
**Test ID**: ADMIN-FE-USER-017
**Priority**: Medium
**Description**: Verify user search functionality
**Steps**:
1. Navigate to Users tab
2. Enter text in search box
3. Observe filtered results

**Expected Result**:
- Results filter as you type
- Search across name and email
- No results message if none found
- Clear search button appears

---

## Test Suite 5: Department Management Tab

### Test Case 5.1: Department List Display
**Test ID**: ADMIN-FE-DEPT-001
**Priority**: Critical
**Description**: Verify department list loads
**Steps**:
1. Navigate to Departments tab
2. Observe department table

**Expected Result**:
- Table loads with departments
- Columns: Name, Shortcut, Description, Actions
- Data matches API response

### Test Case 5.2: Create Department Modal
**Test ID**: ADMIN-FE-DEPT-002
**Priority**: Critical
**Description**: Verify create department modal
**Steps**:
1. Click "Add Department" button
2. Fill form (Name, Shortcut, Description)
3. Submit

**Expected Result**:
- Modal opens
- All fields visible
- Form validation works
- Success creates department
- Table updates

### Test Case 5.3: Edit Department
**Test ID**: ADMIN-FE-DEPT-003
**Priority**: High
**Description**: Verify department editing
**Steps**:
1. Click edit on a department
2. Modify fields
3. Save

**Expected Result**:
- Modal pre-fills data
- Update succeeds
- Changes reflected in table

### Test Case 5.4: Delete Department - No Dependencies
**Test ID**: ADMIN-FE-DEPT-004
**Priority**: High
**Description**: Verify department deletion when no users/courses
**Steps**:
1. Click delete on department with no users
2. Confirm deletion

**Expected Result**:
- Confirmation dialog appears
- Deletion succeeds
- Department removed from table
- Success message shown

### Test Case 5.5: Delete Department - With Dependencies
**Test ID**: ADMIN-FE-DEPT-005
**Priority**: High
**Description**: Verify cannot delete department with dependencies
**Steps**:
1. Click delete on department with users or courses
2. Confirm deletion

**Expected Result**:
- Error message from API
- Error displayed to user
- Department not deleted
- Message explains why (has dependencies)

### Test Case 5.6: Department Search
**Test ID**: ADMIN-FE-DEPT-006
**Priority**: Medium
**Description**: Verify department search
**Steps**:
1. Enter text in search box
2. Observe results

**Expected Result**:
- Departments filtered
- Search by name and shortcut
- Real-time filtering

---

## Test Suite 6: Course Management Tab

### Test Case 6.1: Course List Display
**Test ID**: ADMIN-FE-COURSE-001
**Priority**: Critical
**Description**: Verify course list loads
**Steps**:
1. Navigate to Courses tab
2. Observe course table

**Expected Result**:
- Table loads with courses
- Columns: Name, Code, Department, Credits, Actions
- Data correct

### Test Case 6.2: Department Filter for Courses
**Test ID**: ADMIN-FE-COURSE-002
**Priority**: High
**Description**: Verify courses can be filtered by department
**Steps**:
1. Select department from filter
2. Observe results

**Expected Result**:
- Only courses from selected department
- Filter clearly indicated
- Can clear filter

### Test Case 6.3: Create Course Modal
**Test ID**: ADMIN-FE-COURSE-003
**Priority**: Critical
**Description**: Verify create course functionality
**Steps**:
1. Click "Add Course" button
2. Fill form (Name, Code, Department, Credits)
3. Submit

**Expected Result**:
- Modal opens
- Department dropdown populated
- Form validates
- Success creates course
- Table updates

### Test Case 6.4: Edit Course
**Test ID**: ADMIN-FE-COURSE-004
**Priority**: High
**Description**: Verify course editing
**Steps**:
1. Click edit on a course
2. Modify fields
3. Save

**Expected Result**:
- Modal pre-fills data
- Update succeeds
- Changes visible in table

### Test Case 6.5: Delete Course - No Assignments
**Test ID**: ADMIN-FE-COURSE-005
**Priority**: High
**Description**: Verify course deletion without assignments
**Steps**:
1. Click delete on unassigned course
2. Confirm

**Expected Result**:
- Deletion succeeds
- Course removed
- Success message

### Test Case 6.6: Delete Course - With Assignments
**Test ID**: ADMIN-FE-COURSE-006
**Priority**: High
**Description**: Verify cannot delete course with assignments
**Steps**:
1. Click delete on assigned course
2. Confirm

**Expected Result**:
- Error from API
- Error displayed
- Course not deleted
- Explanation shown

---

## Test Suite 7: Reports Tab

### Test Case 7.1: Reports Tab Load
**Test ID**: ADMIN-FE-REPORT-001
**Priority**: Medium
**Description**: Verify reports tab loads
**Steps**:
1. Navigate to Reports tab
2. Observe interface

**Expected Result**:
- Tab loads successfully
- Filter options displayed
- Generate button visible

### Test Case 7.2: Report Filters
**Test ID**: ADMIN-FE-REPORT-002
**Priority**: Medium
**Description**: Verify report filters populate
**Steps**:
1. Navigate to Reports tab
2. Check filter dropdowns

**Expected Result**:
- Academic years populated from API
- Semesters populated
- Departments populated
- All can be selected

### Test Case 7.3: Generate Report
**Test ID**: ADMIN-FE-REPORT-003
**Priority**: Medium
**Description**: Verify report generation
**Steps**:
1. Select filters
2. Click Generate Report
3. Observe results

**Expected Result**:
- Loading indicator shown
- Report data loads
- Data displayed in table/chart
- Matches selected filters

---

## Test Suite 8: UI/UX Testing

### Test Case 8.1: Tab Navigation
**Test ID**: ADMIN-FE-UX-001
**Priority**: High
**Description**: Verify smooth tab switching
**Steps**:
1. Click through all tabs (Dashboard, Users, Departments, Courses, Reports)
2. Observe transitions

**Expected Result**:
- Tabs switch without page reload
- Content loads correctly for each tab
- Active tab highlighted
- Page title updates
- No flashing or layout shifts

### Test Case 8.2: Loading States
**Test ID**: ADMIN-FE-UX-002
**Priority**: Medium
**Description**: Verify loading indicators work
**Steps**:
1. Trigger various API calls
2. Observe loading states

**Expected Result**:
- Loading spinners/skeletons shown
- Buttons disabled during submission
- "Loading..." text where appropriate
- No double-submissions possible

### Test Case 8.3: Error Messages
**Test ID**: ADMIN-FE-UX-003
**Priority**: High
**Description**: Verify error messages are user-friendly
**Steps**:
1. Trigger various errors (network, validation, API)
2. Observe error display

**Expected Result**:
- Errors displayed clearly
- Toast/notification system works
- Error messages helpful
- User can dismiss errors
- Form errors inline

### Test Case 8.4: Success Messages
**Test ID**: ADMIN-FE-UX-004
**Priority**: Medium
**Description**: Verify success feedback
**Steps**:
1. Complete various actions (create, update, delete)
2. Observe success messages

**Expected Result**:
- Success toast/notification appears
- Auto-dismisses after few seconds
- Can be manually dismissed
- Message specific to action

### Test Case 8.5: Modal Backdrop Click
**Test ID**: ADMIN-FE-UX-005
**Priority**: Low
**Description**: Verify modals close on backdrop click
**Steps**:
1. Open any modal
2. Click outside modal (on backdrop)

**Expected Result**:
- Modal closes
- Form data cleared (or confirmation if data entered)
- Background unfreezes

### Test Case 8.6: Keyboard Navigation
**Test ID**: ADMIN-FE-UX-006
**Priority**: Low
**Description**: Verify keyboard accessibility
**Steps**:
1. Tab through form fields
2. Use Enter to submit
3. Use Escape to close modals

**Expected Result**:
- Tab order logical
- Focus indicators visible
- Enter submits forms
- Escape closes modals
- Keyboard accessible

### Test Case 8.7: Dark Mode Toggle
**Test ID**: ADMIN-FE-UX-007
**Priority**: Medium
**Description**: Verify dark mode works completely
**Steps**:
1. Toggle dark mode on
2. Navigate through all tabs
3. Open modals
4. Check all components

**Expected Result**:
- All components render in dark theme
- Text readable (good contrast)
- Colors appropriate
- No broken styling
- Toggle state clear

### Test Case 8.8: Mobile Responsive - Tables
**Test ID**: ADMIN-FE-UX-008
**Priority**: Medium
**Description**: Verify tables responsive on mobile
**Steps**:
1. Resize browser to mobile width
2. View tables in Users, Departments, Courses tabs

**Expected Result**:
- Tables scroll horizontally
- Or cards layout on mobile
- All data accessible
- Actions still available
- No horizontal page scroll

### Test Case 8.9: Mobile Responsive - Modals
**Test ID**: ADMIN-FE-UX-009
**Priority**: Medium
**Description**: Verify modals work on mobile
**Steps**:
1. Resize to mobile
2. Open create/edit modals

**Expected Result**:
- Modals fit screen
- Full-screen on mobile or appropriate sizing
- Forms usable
- All fields accessible
- Can scroll if needed

---

## Test Suite 9: Error Handling & Edge Cases

### Test Case 9.1: Network Error
**Test ID**: ADMIN-FE-ERROR-001
**Priority**: High
**Description**: Verify handling of network errors
**Steps**:
1. Disconnect from network
2. Attempt API call (e.g., load users)

**Expected Result**:
- Error message displayed
- User informed of network issue
- Retry option available
- Application doesn't crash

### Test Case 9.2: API Error 500
**Test ID**: ADMIN-FE-ERROR-002
**Priority**: High
**Description**: Verify handling of server errors
**Steps**:
1. Trigger condition causing 500 error
2. Observe response

**Expected Result**:
- Error message displayed
- User-friendly message (not technical)
- Application remains stable
- User can continue using app

### Test Case 9.3: Empty State - No Data
**Test ID**: ADMIN-FE-ERROR-003
**Priority**: Medium
**Description**: Verify empty state displays
**Steps**:
1. View tab with no data (e.g., no departments)
2. Observe display

**Expected Result**:
- Empty state message shown
- Helpful text (e.g., "No departments yet. Create one to get started")
- Create button available
- No broken tables

### Test Case 9.4: Large Dataset Performance
**Test ID**: ADMIN-FE-ERROR-004
**Priority**: Low
**Description**: Verify performance with many records
**Steps**:
1. Load table with 100+ records
2. Observe performance

**Expected Result**:
- Pagination limits displayed records
- Table renders smoothly
- No lag or freezing
- Scrolling smooth

---

## Test Execution Summary

### Total Test Cases: 69

**Priority Breakdown**:
- Critical: 19 tests
- High: 32 tests
- Medium: 16 tests
- Low: 2 tests

**Category Breakdown**:
- Authentication & Access: 5 tests
- Page Load & Rendering: 4 tests
- Dashboard Tab: 4 tests
- User Management Tab: 17 tests
- Department Management: 6 tests
- Course Management: 6 tests
- Reports Tab: 3 tests
- UI/UX Testing: 9 tests
- Error Handling: 4 tests

### Browser Compatibility Testing
Test on:
- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)
- Mobile browsers (Chrome Mobile, Safari iOS)

### Responsive Testing
- Desktop: 1920x1080, 1366x768
- Tablet: 768x1024
- Mobile: 375x667, 414x896

### Performance Targets
- Initial load: < 3 seconds
- Tab switch: < 500ms
- API calls: < 2 seconds
- No memory leaks

### Accessibility Requirements
- WCAG 2.1 Level AA compliance
- Screen reader compatible
- Keyboard navigable
- Sufficient color contrast

### Success Criteria
- All CRITICAL tests pass: 100%
- All HIGH priority tests pass: >= 95%
- Overall pass rate: >= 90%
- No blocking bugs
- Performance targets met
- Works on all target browsers

### Test Data Requirements
1. Admin user account
2. Multiple user accounts (different roles)
3. Several departments (some with users, some without)
4. Multiple courses (some assigned, some not)
5. Test data for all entities

### Notes
- Tests should be repeatable
- Each test should be independent
- Use test data that can be reset
- Document any bugs found with screenshots
- Re-test after bug fixes
