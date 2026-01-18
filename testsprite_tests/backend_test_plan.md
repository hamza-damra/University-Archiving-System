# University Archiving System - Admin Panel Backend Test Plan

## Test Environment Setup
- **Base URL**: http://localhost:8080
- **API Prefix**: /api/admin
- **Authentication**: JWT Bearer Token (ROLE_ADMIN required)
- **Database**: MySQL (archive_system)

## Prerequisites
1. Application running on http://localhost:8080
2. Database seeded with test data
3. Admin user credentials available
4. Valid JWT token for authentication

---

## Test Suite 1: Authentication & Authorization

### Test Case 1.1: Health Check (No Auth Required)
**Test ID**: ADMIN-AUTH-001
**Priority**: High
**Endpoint**: GET /api/admin/health
**Description**: Verify health check endpoint is accessible
**Steps**:
1. Send GET request to /api/admin/health
2. Verify response status is 200
3. Verify response body contains "Admin API is operational"

**Expected Result**: 
- Status: 200 OK
- Body: "Admin API is operational"

### Test Case 1.2: Access Without Token
**Test ID**: ADMIN-AUTH-002
**Priority**: High
**Endpoint**: GET /api/admin/users
**Description**: Verify endpoints are protected
**Steps**:
1. Send GET request to /api/admin/users without Authorization header
2. Verify response status is 401 or 403

**Expected Result**: 
- Status: 401 Unauthorized or 403 Forbidden

### Test Case 1.3: Access With Invalid Token
**Test ID**: ADMIN-AUTH-003
**Priority**: High
**Endpoint**: GET /api/admin/users
**Description**: Verify invalid tokens are rejected
**Steps**:
1. Send GET request with Authorization: Bearer invalid_token
2. Verify response status is 401 or 403

**Expected Result**: 
- Status: 401 Unauthorized or 403 Forbidden

### Test Case 1.4: Access With Valid Admin Token
**Test ID**: ADMIN-AUTH-004
**Priority**: High
**Endpoint**: GET /api/admin/users
**Description**: Verify valid admin token grants access
**Steps**:
1. Login as admin to get JWT token
2. Send GET request with Authorization: Bearer {valid_token}
3. Verify response status is 200

**Expected Result**: 
- Status: 200 OK
- Response contains user data

---

## Test Suite 2: User Management

### Test Case 2.1: Create User - Success
**Test ID**: ADMIN-USER-001
**Priority**: High
**Endpoint**: POST /api/admin/users
**Description**: Create a new user with valid data
**Request Body**:
```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "test.user@alquds.edu",
  "password": "SecurePass123!",
  "role": "ROLE_PROFESSOR",
  "departmentId": 1
}
```
**Expected Result**:
- Status: 201 Created
- Response contains created user with ID
- Password is not returned in response

### Test Case 2.2: Create User - Duplicate Email
**Test ID**: ADMIN-USER-002
**Priority**: High
**Endpoint**: POST /api/admin/users
**Description**: Attempt to create user with existing email
**Steps**:
1. Create user with email test@alquds.edu
2. Attempt to create another user with same email
3. Verify error response

**Expected Result**:
- Status: 400 Bad Request
- Error message indicates email already exists

### Test Case 2.3: Create User - Invalid Email
**Test ID**: ADMIN-USER-003
**Priority**: Medium
**Endpoint**: POST /api/admin/users
**Description**: Attempt to create user with invalid email format
**Request Body**:
```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "invalid-email",
  "password": "SecurePass123!",
  "role": "ROLE_PROFESSOR"
}
```
**Expected Result**:
- Status: 400 Bad Request
- Validation error for email field

### Test Case 2.4: Create User - Missing Required Fields
**Test ID**: ADMIN-USER-004
**Priority**: High
**Endpoint**: POST /api/admin/users
**Description**: Attempt to create user without required fields
**Request Body**:
```json
{
  "email": "test@alquds.edu"
}
```
**Expected Result**:
- Status: 400 Bad Request
- Validation errors for missing fields

### Test Case 2.5: Get All Users - No Filters
**Test ID**: ADMIN-USER-005
**Priority**: High
**Endpoint**: GET /api/admin/users
**Description**: Retrieve all users without filters
**Expected Result**:
- Status: 200 OK
- Paginated response with users
- Default pagination (page=0, size=20)

### Test Case 2.6: Get All Users - With Role Filter
**Test ID**: ADMIN-USER-006
**Priority**: Medium
**Endpoint**: GET /api/admin/users?role=ROLE_PROFESSOR
**Description**: Filter users by role
**Expected Result**:
- Status: 200 OK
- All returned users have ROLE_PROFESSOR

### Test Case 2.7: Get All Users - With Department Filter
**Test ID**: ADMIN-USER-007
**Priority**: Medium
**Endpoint**: GET /api/admin/users?departmentId=1
**Description**: Filter users by department
**Expected Result**:
- Status: 200 OK
- All returned users belong to department 1

### Test Case 2.8: Get All Users - With Active Status Filter
**Test ID**: ADMIN-USER-008
**Priority**: Medium
**Endpoint**: GET /api/admin/users?isActive=true
**Description**: Filter users by active status
**Expected Result**:
- Status: 200 OK
- All returned users have isActive=true

### Test Case 2.9: Get All Users - With Pagination
**Test ID**: ADMIN-USER-009
**Priority**: Medium
**Endpoint**: GET /api/admin/users?page=1&size=10
**Description**: Test pagination
**Expected Result**:
- Status: 200 OK
- Response contains 10 users (or less if fewer available)
- Page number is 1

### Test Case 2.10: Get User By ID - Success
**Test ID**: ADMIN-USER-010
**Priority**: High
**Endpoint**: GET /api/admin/users/{id}
**Description**: Retrieve specific user by ID
**Expected Result**:
- Status: 200 OK
- Response contains user details
- Includes department information if applicable

### Test Case 2.11: Get User By ID - Not Found
**Test ID**: ADMIN-USER-011
**Priority**: Medium
**Endpoint**: GET /api/admin/users/99999
**Description**: Attempt to get non-existent user
**Expected Result**:
- Status: 404 Not Found
- Error message indicates user not found

### Test Case 2.12: Update User - Success
**Test ID**: ADMIN-USER-012
**Priority**: High
**Endpoint**: PUT /api/admin/users/{id}
**Description**: Update user information
**Request Body**:
```json
{
  "firstName": "Updated",
  "lastName": "Name",
  "email": "updated@alquds.edu",
  "role": "ROLE_HOD",
  "departmentId": 2
}
```
**Expected Result**:
- Status: 200 OK
- Response contains updated user data

### Test Case 2.13: Update User - Invalid ID
**Test ID**: ADMIN-USER-013
**Priority**: Medium
**Endpoint**: PUT /api/admin/users/99999
**Description**: Attempt to update non-existent user
**Expected Result**:
- Status: 404 Not Found

### Test Case 2.14: Update User Password - Success
**Test ID**: ADMIN-USER-014
**Priority**: High
**Endpoint**: PUT /api/admin/users/{id}/password
**Description**: Update user password
**Request Body**:
```json
{
  "newPassword": "NewSecurePass123!"
}
```
**Expected Result**:
- Status: 200 OK
- Success message returned

### Test Case 2.15: Delete User - Success
**Test ID**: ADMIN-USER-015
**Priority**: High
**Endpoint**: DELETE /api/admin/users/{id}
**Description**: Delete/deactivate user
**Expected Result**:
- Status: 200 OK
- Success message returned
- User is deactivated (not physically deleted)

### Test Case 2.16: Delete User - Not Found
**Test ID**: ADMIN-USER-016
**Priority**: Medium
**Endpoint**: DELETE /api/admin/users/99999
**Description**: Attempt to delete non-existent user
**Expected Result**:
- Status: 404 Not Found

---

## Test Suite 3: Department Management

### Test Case 3.1: Create Department - Success
**Test ID**: ADMIN-DEPT-001
**Priority**: High
**Endpoint**: POST /api/admin/departments
**Description**: Create new department
**Request Body**:
```json
{
  "name": "Computer Science",
  "shortcut": "CS",
  "description": "Department of Computer Science"
}
```
**Expected Result**:
- Status: 201 Created
- Response contains created department with ID

### Test Case 3.2: Create Department - Duplicate Name
**Test ID**: ADMIN-DEPT-002
**Priority**: High
**Endpoint**: POST /api/admin/departments
**Description**: Attempt to create department with existing name
**Expected Result**:
- Status: 400 Bad Request
- Error indicates department already exists

### Test Case 3.3: Get All Departments
**Test ID**: ADMIN-DEPT-003
**Priority**: High
**Endpoint**: GET /api/admin/departments
**Description**: Retrieve all departments
**Expected Result**:
- Status: 200 OK
- Array of departments returned

### Test Case 3.4: Get Department By ID - Success
**Test ID**: ADMIN-DEPT-004
**Priority**: High
**Endpoint**: GET /api/admin/departments/{id}
**Description**: Retrieve specific department
**Expected Result**:
- Status: 200 OK
- Department details returned

### Test Case 3.5: Get Department By ID - Not Found
**Test ID**: ADMIN-DEPT-005
**Priority**: Medium
**Endpoint**: GET /api/admin/departments/99999
**Description**: Attempt to get non-existent department
**Expected Result**:
- Status: 404 Not Found

### Test Case 3.6: Update Department - Success
**Test ID**: ADMIN-DEPT-006
**Priority**: High
**Endpoint**: PUT /api/admin/departments/{id}
**Description**: Update department information
**Request Body**:
```json
{
  "name": "Updated Computer Science",
  "shortcut": "UCS",
  "description": "Updated description"
}
```
**Expected Result**:
- Status: 200 OK
- Updated department returned

### Test Case 3.7: Delete Department - Success (No Dependencies)
**Test ID**: ADMIN-DEPT-007
**Priority**: High
**Endpoint**: DELETE /api/admin/departments/{id}
**Description**: Delete department with no users or courses
**Expected Result**:
- Status: 200 OK
- Success message returned

### Test Case 3.8: Delete Department - With Dependencies
**Test ID**: ADMIN-DEPT-008
**Priority**: High
**Endpoint**: DELETE /api/admin/departments/{id}
**Description**: Attempt to delete department with users/courses
**Expected Result**:
- Status: 400 Bad Request
- Error indicates department has dependencies

---

## Test Suite 4: Course Management

### Test Case 4.1: Create Course - Success
**Test ID**: ADMIN-COURSE-001
**Priority**: High
**Endpoint**: POST /api/admin/courses
**Description**: Create new course
**Request Body**:
```json
{
  "name": "Data Structures",
  "code": "CS201",
  "departmentId": 1,
  "credits": 3
}
```
**Expected Result**:
- Status: 201 Created
- Response contains created course

### Test Case 4.2: Create Course - Duplicate Code
**Test ID**: ADMIN-COURSE-002
**Priority**: High
**Endpoint**: POST /api/admin/courses
**Description**: Attempt to create course with existing code
**Expected Result**:
- Status: 400 Bad Request
- Error indicates course code exists

### Test Case 4.3: Get All Courses - No Filter
**Test ID**: ADMIN-COURSE-003
**Priority**: High
**Endpoint**: GET /api/admin/courses
**Description**: Retrieve all courses
**Expected Result**:
- Status: 200 OK
- Array of courses returned

### Test Case 4.4: Get All Courses - Department Filter
**Test ID**: ADMIN-COURSE-004
**Priority**: Medium
**Endpoint**: GET /api/admin/courses?departmentId=1
**Description**: Filter courses by department
**Expected Result**:
- Status: 200 OK
- Only courses from department 1 returned

### Test Case 4.5: Get Course By ID - Success
**Test ID**: ADMIN-COURSE-005
**Priority**: High
**Endpoint**: GET /api/admin/courses/{id}
**Description**: Retrieve specific course
**Expected Result**:
- Status: 200 OK
- Course details returned

### Test Case 4.6: Update Course - Success
**Test ID**: ADMIN-COURSE-006
**Priority**: High
**Endpoint**: PUT /api/admin/courses/{id}
**Description**: Update course information
**Request Body**:
```json
{
  "name": "Advanced Data Structures",
  "code": "CS301",
  "departmentId": 1,
  "credits": 4
}
```
**Expected Result**:
- Status: 200 OK
- Updated course returned

### Test Case 4.7: Delete Course - Success
**Test ID**: ADMIN-COURSE-007
**Priority**: High
**Endpoint**: DELETE /api/admin/courses/{id}
**Description**: Delete course with no assignments
**Expected Result**:
- Status: 200 OK
- Success message returned

### Test Case 4.8: Delete Course - With Active Assignments
**Test ID**: ADMIN-COURSE-008
**Priority**: High
**Endpoint**: DELETE /api/admin/courses/{id}
**Description**: Attempt to delete course with assignments
**Expected Result**:
- Status: 400 Bad Request
- Error indicates active assignments exist

---

## Test Suite 5: Course Assignments

### Test Case 5.1: Assign Course - Success
**Test ID**: ADMIN-ASSIGN-001
**Priority**: High
**Endpoint**: POST /api/admin/course-assignments
**Description**: Assign course to professor
**Request Body**:
```json
{
  "courseId": 1,
  "professorId": 2,
  "semesterId": 1
}
```
**Expected Result**:
- Status: 201 Created
- Assignment details returned

### Test Case 5.2: Assign Course - Duplicate Assignment
**Test ID**: ADMIN-ASSIGN-002
**Priority**: High
**Endpoint**: POST /api/admin/course-assignments
**Description**: Attempt duplicate assignment
**Expected Result**:
- Status: 400 Bad Request
- Error indicates assignment exists

### Test Case 5.3: Get Assignments - With Semester Filter
**Test ID**: ADMIN-ASSIGN-003
**Priority**: High
**Endpoint**: GET /api/admin/course-assignments?semesterId=1
**Description**: Get all assignments for semester
**Expected Result**:
- Status: 200 OK
- Array of assignments returned

### Test Case 5.4: Get Assignments - Missing Semester
**Test ID**: ADMIN-ASSIGN-004
**Priority**: High
**Endpoint**: GET /api/admin/course-assignments
**Description**: Attempt to get assignments without semester
**Expected Result**:
- Status: 400 Bad Request
- Error indicates semesterId required

### Test Case 5.5: Get Assignments - With Professor Filter
**Test ID**: ADMIN-ASSIGN-005
**Priority**: Medium
**Endpoint**: GET /api/admin/course-assignments?semesterId=1&professorId=2
**Description**: Filter assignments by professor
**Expected Result**:
- Status: 200 OK
- Only assignments for specified professor

### Test Case 5.6: Unassign Course - Success
**Test ID**: ADMIN-ASSIGN-006
**Priority**: High
**Endpoint**: DELETE /api/admin/course-assignments/{id}
**Description**: Remove course assignment
**Expected Result**:
- Status: 200 OK
- Success message returned

### Test Case 5.7: Unassign Course - Not Found
**Test ID**: ADMIN-ASSIGN-007
**Priority**: Medium
**Endpoint**: DELETE /api/admin/course-assignments/99999
**Description**: Attempt to unassign non-existent assignment
**Expected Result**:
- Status: 404 Not Found

---

## Test Suite 6: Dashboard Statistics

### Test Case 6.1: Get Statistics - No Filters
**Test ID**: ADMIN-STATS-001
**Priority**: High
**Endpoint**: GET /api/admin/dashboard/statistics
**Description**: Get overall dashboard statistics
**Expected Result**:
- Status: 200 OK
- Statistics object with counts (users, departments, courses, files)
- Chart data included

### Test Case 6.2: Get Statistics - Academic Year Filter
**Test ID**: ADMIN-STATS-002
**Priority**: Medium
**Endpoint**: GET /api/admin/dashboard/statistics?academicYearId=1
**Description**: Filter statistics by academic year
**Expected Result**:
- Status: 200 OK
- Statistics for specified academic year

### Test Case 6.3: Get Statistics - Semester Filter
**Test ID**: ADMIN-STATS-003
**Priority**: Medium
**Endpoint**: GET /api/admin/dashboard/statistics?semesterId=1
**Description**: Filter statistics by semester
**Expected Result**:
- Status: 200 OK
- Statistics for specified semester

---

## Test Suite 7: Reports

### Test Case 7.1: Get Filter Options
**Test ID**: ADMIN-REPORT-001
**Priority**: Medium
**Endpoint**: GET /api/admin/reports/filter-options
**Description**: Get available report filter options
**Expected Result**:
- Status: 200 OK
- Filter options (academic years, semesters, departments)

---

## Test Execution Summary

### Total Test Cases: 56

**Priority Breakdown**:
- High Priority: 41 tests
- Medium Priority: 15 tests

**Category Breakdown**:
- Authentication & Authorization: 4 tests
- User Management: 16 tests
- Department Management: 8 tests
- Course Management: 8 tests
- Course Assignments: 7 tests
- Dashboard Statistics: 3 tests
- Reports: 1 test

### Test Data Requirements
1. Admin user account with valid credentials
2. At least 2 departments in database
3. At least 5 users with different roles
4. At least 5 courses across departments
5. At least 2 course assignments
6. Active academic year and semester

### Environment Requirements
1. MySQL database running
2. Spring Boot application running on port 8080
3. Database seeded with initial data
4. Test isolation between runs

### Success Criteria
- All HIGH priority tests pass: 100%
- Overall pass rate: >= 95%
- All CRUD operations functional
- Security tests pass (authentication/authorization)
- Data validation working correctly
- Error handling appropriate

### Notes
- Tests should be run in order within each suite
- Some tests depend on data created by previous tests
- Clean up test data after execution
- Each test should be idempotent where possible
