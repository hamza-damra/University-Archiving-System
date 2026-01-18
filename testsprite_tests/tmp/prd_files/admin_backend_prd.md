# Admin Panel Backend API - Product Requirements Document

## Overview
Backend REST API endpoints for the Admin panel of the University Archiving System. All endpoints require ROLE_ADMIN authentication.

## Base URL
`/api/admin`

## Authentication
- Type: JWT Bearer Token
- Required Role: ROLE_ADMIN
- Storage: Authorization header

## API Endpoints

### 1. User Management

#### 1.1 Create User
- **Endpoint**: `POST /api/admin/users`
- **Description**: Create a new user with any role (Admin, Dean, HOD, Professor)
- **Request Body**:
  ```json
  {
    "firstName": "string",
    "lastName": "string",
    "email": "string",
    "password": "string",
    "role": "ROLE_ADMIN|ROLE_DEANSHIP|ROLE_HOD|ROLE_PROFESSOR",
    "departmentId": "number (optional)"
  }
  ```
- **Expected Response**: 201 Created with user details
- **Error Cases**: 400 if email exists, validation errors

#### 1.2 Get All Users
- **Endpoint**: `GET /api/admin/users`
- **Description**: Get paginated list of users with optional filters
- **Query Parameters**:
  - `role`: Filter by role (optional)
  - `departmentId`: Filter by department (optional)
  - `isActive`: Filter by active status (optional)
  - `page`: Page number (default: 0)
  - `size`: Page size (default: 20)
- **Expected Response**: 200 OK with paginated user list
- **Error Cases**: 500 for server errors

#### 1.3 Get User by ID
- **Endpoint**: `GET /api/admin/users/{id}`
- **Description**: Get specific user details
- **Expected Response**: 200 OK with user details
- **Error Cases**: 404 if user not found

#### 1.4 Update User
- **Endpoint**: `PUT /api/admin/users/{id}`
- **Description**: Update user information
- **Request Body**: User update fields
- **Expected Response**: 200 OK with updated user
- **Error Cases**: 404 if user not found, 400 for validation errors

#### 1.5 Update User Password
- **Endpoint**: `PUT /api/admin/users/{id}/password`
- **Description**: Update user password
- **Request Body**: `{ "newPassword": "string" }`
- **Expected Response**: 200 OK
- **Error Cases**: 404 if user not found

#### 1.6 Delete User
- **Endpoint**: `DELETE /api/admin/users/{id}`
- **Description**: Delete/deactivate user
- **Expected Response**: 200 OK
- **Error Cases**: 404 if user not found

### 2. Department Management

#### 2.1 Create Department
- **Endpoint**: `POST /api/admin/departments`
- **Description**: Create a new department
- **Request Body**:
  ```json
  {
    "name": "string",
    "shortcut": "string",
    "description": "string (optional)"
  }
  ```
- **Expected Response**: 201 Created
- **Error Cases**: 400 if department exists

#### 2.2 Get All Departments
- **Endpoint**: `GET /api/admin/departments`
- **Description**: Get all departments
- **Expected Response**: 200 OK with department list
- **Error Cases**: 500 for server errors

#### 2.3 Get Department by ID
- **Endpoint**: `GET /api/admin/departments/{id}`
- **Description**: Get specific department
- **Expected Response**: 200 OK with department details
- **Error Cases**: 404 if not found

#### 2.4 Update Department
- **Endpoint**: `PUT /api/admin/departments/{id}`
- **Description**: Update department information
- **Expected Response**: 200 OK with updated department
- **Error Cases**: 404 if not found

#### 2.5 Delete Department
- **Endpoint**: `DELETE /api/admin/departments/{id}`
- **Description**: Delete department
- **Expected Response**: 200 OK
- **Error Cases**: 400 if department has dependencies

### 3. Course Management

#### 3.1 Create Course
- **Endpoint**: `POST /api/admin/courses`
- **Description**: Create a new course
- **Request Body**:
  ```json
  {
    "name": "string",
    "code": "string",
    "departmentId": "number",
    "credits": "number (optional)"
  }
  ```
- **Expected Response**: 201 Created
- **Error Cases**: 400 if course code exists

#### 3.2 Get All Courses
- **Endpoint**: `GET /api/admin/courses`
- **Description**: Get all courses with optional department filter
- **Query Parameters**: `departmentId` (optional)
- **Expected Response**: 200 OK with course list
- **Error Cases**: 500 for server errors

#### 3.3 Get Course by ID
- **Endpoint**: `GET /api/admin/courses/{id}`
- **Description**: Get specific course
- **Expected Response**: 200 OK with course details
- **Error Cases**: 404 if not found

#### 3.4 Update Course
- **Endpoint**: `PUT /api/admin/courses/{id}`
- **Description**: Update course information
- **Expected Response**: 200 OK with updated course
- **Error Cases**: 404 if not found

#### 3.5 Delete Course
- **Endpoint**: `DELETE /api/admin/courses/{id}`
- **Description**: Delete/deactivate course
- **Expected Response**: 200 OK
- **Error Cases**: 400 if course has active assignments

### 4. Course Assignments

#### 4.1 Assign Course
- **Endpoint**: `POST /api/admin/course-assignments`
- **Description**: Assign course to professor for specific semester
- **Request Body**:
  ```json
  {
    "courseId": "number",
    "professorId": "number",
    "semesterId": "number"
  }
  ```
- **Expected Response**: 201 Created
- **Error Cases**: 400 if assignment exists

#### 4.2 Get Assignments
- **Endpoint**: `GET /api/admin/course-assignments`
- **Description**: Get course assignments with filters
- **Query Parameters**:
  - `semesterId`: Required
  - `professorId`: Optional
- **Expected Response**: 200 OK with assignment list
- **Error Cases**: 400 if semesterId missing

#### 4.3 Unassign Course
- **Endpoint**: `DELETE /api/admin/course-assignments/{id}`
- **Description**: Remove course assignment
- **Expected Response**: 200 OK
- **Error Cases**: 404 if assignment not found

### 5. Dashboard Statistics

#### 5.1 Get Statistics
- **Endpoint**: `GET /api/admin/dashboard/statistics`
- **Description**: Get dashboard statistics
- **Query Parameters**:
  - `academicYearId`: Optional
  - `semesterId`: Optional
- **Expected Response**: 200 OK with statistics data
- **Error Cases**: 500 for server errors

### 6. Reports

#### 6.1 Get Filter Options
- **Endpoint**: `GET /api/admin/reports/filter-options`
- **Description**: Get available filter options for reports
- **Expected Response**: 200 OK with filter options
- **Error Cases**: 500 for server errors

### 7. Health Check

#### 7.1 Health Check
- **Endpoint**: `GET /api/admin/health`
- **Description**: Check API health status
- **Expected Response**: 200 OK with "Admin API is operational"
- **Error Cases**: None expected

## Testing Requirements

### Security Testing
1. Verify all endpoints require ROLE_ADMIN authorization
2. Test unauthorized access returns 401/403
3. Test invalid JWT tokens are rejected

### Functional Testing
1. CRUD operations for Users, Departments, Courses
2. Course assignment workflows
3. Pagination and filtering
4. Data validation
5. Error handling

### Edge Cases
1. Duplicate entries (email, course code, etc.)
2. Missing required fields
3. Invalid references (foreign keys)
4. Cascading deletes
5. Large datasets (pagination)

## Success Criteria
- All API endpoints respond with correct status codes
- Data validation works as expected
- Error messages are clear and helpful
- Authorization is properly enforced
- CRUD operations maintain data integrity
