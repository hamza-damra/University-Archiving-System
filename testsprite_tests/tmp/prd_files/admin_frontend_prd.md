# Admin Panel Frontend - Product Requirements Document

## Overview
Frontend web application for the Admin dashboard of the University Archiving System. Provides a comprehensive interface for managing users, departments, courses, and viewing statistics.

## Access URL
`/admin/dashboard.html`

## Authentication
- Requires valid JWT token in localStorage
- Role: ROLE_ADMIN
- Redirects to login if not authenticated

## Core Features

### 1. Dashboard Tab
**Description**: Overview page with statistics and charts
**Components**:
- Statistics cards (total users, departments, courses, files)
- Charts and visualizations
- Recent activity feed
- Quick action buttons

**Test Cases**:
1. Dashboard loads successfully
2. Statistics cards display correct data
3. Charts render properly
4. Data updates on filter changes

### 2. User Management Tab
**Description**: Manage system users (Admins, Deans, HODs, Professors)

**Features**:
- **User List Display**
  - Paginated table with user information
  - Columns: Name, Email, Role, Department, Status, Actions
  - Filter by role, department, active status
  - Search functionality

- **Create User Modal**
  - Form fields: First Name, Last Name, Email, Password, Role, Department
  - Field validation (email format, required fields)
  - Submit creates new user via API
  - Success/error messages

- **Edit User Modal**
  - Pre-filled form with existing user data
  - Update user information
  - Change password option
  - Validation on all fields

- **Delete User**
  - Confirmation dialog
  - Soft delete/deactivate user
  - Success feedback

**Test Cases**:
1. User list loads with data
2. Pagination works correctly
3. Filters apply properly
4. Create user modal opens
5. Form validation works (required fields, email format)
6. User creation succeeds with valid data
7. User creation fails with duplicate email
8. Edit modal pre-fills data correctly
9. User update succeeds
10. Delete confirmation appears
11. User deletion succeeds
12. Table updates after CRUD operations

### 3. Department Management Tab
**Description**: Manage academic departments

**Features**:
- **Department List Display**
  - Table with department information
  - Columns: Name, Shortcut, Description, Actions
  - Search functionality

- **Create Department Modal**
  - Form fields: Name, Shortcut, Description
  - Field validation
  - Submit creates new department

- **Edit Department Modal**
  - Pre-filled form
  - Update department information

- **Delete Department**
  - Confirmation dialog
  - Check for dependencies (users, courses)
  - Cannot delete if has dependencies

**Test Cases**:
1. Department list loads
2. Create department modal opens
3. Form validation works
4. Department creation succeeds
5. Duplicate department name fails
6. Edit modal pre-fills correctly
7. Department update succeeds
8. Delete fails if has dependencies
9. Delete succeeds if no dependencies
10. Table updates after operations

### 4. Course Management Tab
**Description**: Manage academic courses

**Features**:
- **Course List Display**
  - Table with course information
  - Columns: Name, Code, Department, Credits, Actions
  - Filter by department
  - Search functionality

- **Create Course Modal**
  - Form fields: Name, Code, Department, Credits
  - Field validation
  - Submit creates new course

- **Edit Course Modal**
  - Pre-filled form
  - Update course information

- **Delete Course**
  - Confirmation dialog
  - Check for active assignments

**Test Cases**:
1. Course list loads
2. Department filter works
3. Create course modal opens
4. Form validation works
5. Course creation succeeds
6. Duplicate course code fails
7. Edit modal pre-fills correctly
8. Course update succeeds
9. Delete fails if has assignments
10. Delete succeeds if no assignments
11. Table updates after operations

### 5. Reports Tab
**Description**: Generate and view reports

**Features**:
- Report filters (academic year, semester, department)
- Generate report button
- Display report data
- Export functionality

**Test Cases**:
1. Reports tab loads
2. Filters populate with data
3. Report generation works
4. Data displays correctly
5. Export functionality works

## UI/UX Requirements

### Navigation
- Tab-based navigation (Dashboard, Users, Departments, Courses, Reports)
- Active tab is highlighted
- Tab state persists in localStorage
- Page title updates based on active tab

### Theme Support
- Light mode (default)
- Dark mode toggle
- Theme preference saved in localStorage
- All components styled for both themes

### Modals
- Create/Edit forms in modal dialogs
- Modal backdrop dims page
- Close modal on backdrop click or X button
- Form data cleared on modal close
- Validation errors displayed inline

### Loading States
- Loading spinners during API calls
- Disabled buttons during submission
- Skeleton loaders for data tables

### Error Handling
- API error messages displayed to user
- Toast notifications for success/error
- Network error handling
- Authentication errors redirect to login

### Responsive Design
- Mobile-friendly layout
- Sidebar collapses on mobile
- Tables scroll horizontally on small screens
- Modals adapt to screen size

## Test Scenarios

### Navigation Testing
1. Tab switching works without page reload
2. Active tab persists after page refresh
3. Page title updates correctly
4. URL doesn't change (SPA behavior)

### Form Testing
1. All form fields validate properly
2. Required fields show errors when empty
3. Email validation works
4. Password strength requirements enforced
5. Dropdown selections work
6. Submit button disabled until valid

### API Integration Testing
1. GET requests fetch data correctly
2. POST requests create records
3. PUT requests update records
4. DELETE requests remove records
5. Error responses handled gracefully
6. Loading states shown during requests
7. Data refreshes after mutations

### Authentication Testing
1. Unauthenticated users redirected to login
2. Wrong role (non-admin) denied access
3. JWT token expiration handled
4. Logout clears authentication

### User Experience Testing
1. Success messages show after operations
2. Error messages are clear and helpful
3. Confirmations required for destructive actions
4. Data updates immediately in UI
5. No page flickers or jumps
6. Smooth transitions and animations

## Success Criteria
- All tabs load and function correctly
- CRUD operations work for all entities
- Form validation prevents bad data
- UI is responsive and accessible
- Dark mode works properly
- Loading and error states handled
- Authentication enforced
- User experience is smooth and intuitive
