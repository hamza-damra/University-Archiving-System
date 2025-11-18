# Mock Data API Testing Guide

This guide provides example API calls for testing the University Archive System using mock data accounts.

## Table of Contents

1. [Authentication](#authentication)
2. [HOD Endpoints](#hod-endpoints)
3. [Professor Endpoints](#professor-endpoints)
4. [Common Endpoints](#common-endpoints)
5. [Testing Workflows](#testing-workflows)

---

## Authentication

### Login

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "user@alquds.edu",
  "password": "password123"
}
```

**Example - Login as HOD:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "hod.cs@alquds.edu",
    "password": "password123"
  }'
```

**Example - Login as Professor:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "prof.ahmad.alnajjar@alquds.edu",
    "password": "password123"
  }'
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "email": "hod.cs@alquds.edu",
      "firstName": "Ahmad",
      "lastName": "Al-Rashid",
      "role": "ROLE_HOD",
      "department": {
        "id": 1,
        "name": "Computer Science"
      }
    }
  }
}
```

**Save the token for subsequent requests:**
```bash
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## HOD Endpoints

### 1. Get All Professors in Department

**Endpoint:** `GET /api/hod/professors`

**Description:** Retrieves all professors in the HOD's department

**Example:**
```bash
curl -X GET http://localhost:8080/api/hod/professors \
  -H "Authorization: Bearer $TOKEN"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Professors retrieved successfully",
  "data": [
    {
      "id": 6,
      "email": "prof.ahmad.alnajjar@alquds.edu",
      "firstName": "Ahmad",
      "lastName": "Al-Najjar",
      "professorId": "PCS001",
      "isActive": true,
      "department": {
        "id": 1,
        "name": "Computer Science"
      }
    },
    {
      "id": 7,
      "email": "prof.fatima.almasri@alquds.edu",
      "firstName": "Fatima",
      "lastName": "Al-Masri",
      "professorId": "PCS002",
      "isActive": true,
      "department": {
        "id": 1,
        "name": "Computer Science"
      }
    }
  ]
}
```

### 2. Get Professor Details

**Endpoint:** `GET /api/hod/professors/{professorId}`

**Description:** Retrieves detailed information about a specific professor

**Example:**
```bash
curl -X GET http://localhost:8080/api/hod/professors/6 \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Get Department Submission Report

**Endpoint:** `GET /api/hod/reports/department`

**Query Parameters:**
- `semesterId` (optional): Filter by semester
- `status` (optional): Filter by submission status (UPLOADED, NOT_UPLOADED, OVERDUE)

**Example:**
```bash
curl -X GET "http://localhost:8080/api/hod/reports/department?semesterId=1" \
  -H "Authorization: Bearer $TOKEN"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Department report generated successfully",
  "data": {
    "departmentName": "Computer Science",
    "semester": "Fall 2024-2025",
    "totalProfessors": 5,
    "totalAssignments": 15,
    "totalSubmissions": 45,
    "submissionStats": {
      "uploaded": 32,
      "notUploaded": 9,
      "overdue": 4
    },
    "complianceRate": 71.1,
    "professors": [
      {
        "professorId": "PCS001",
        "name": "Ahmad Al-Najjar",
        "totalAssignments": 3,
        "submittedCount": 2,
        "pendingCount": 1,
        "overdueCount": 0
      }
    ]
  }
}
```

### 4. Get Professor Submission History

**Endpoint:** `GET /api/hod/professors/{professorId}/submissions`

**Query Parameters:**
- `semesterId` (optional): Filter by semester
- `courseId` (optional): Filter by course

**Example:**
```bash
curl -X GET "http://localhost:8080/api/hod/professors/6/submissions?semesterId=1" \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Filter Professors by Status

**Endpoint:** `GET /api/hod/professors?status={active|inactive}`

**Example - Get only active professors:**
```bash
curl -X GET "http://localhost:8080/api/hod/professors?status=active" \
  -H "Authorization: Bearer $TOKEN"
```

**Example - Get only inactive professors:**
```bash
curl -X GET "http://localhost:8080/api/hod/professors?status=inactive" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Professor Endpoints

### 1. Get My Course Assignments

**Endpoint:** `GET /api/professor/assignments`

**Query Parameters:**
- `semesterId` (optional): Filter by semester
- `academicYearId` (optional): Filter by academic year

**Example:**
```bash
curl -X GET http://localhost:8080/api/professor/assignments \
  -H "Authorization: Bearer $TOKEN"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Course assignments retrieved successfully",
  "data": [
    {
      "id": 1,
      "course": {
        "id": 1,
        "courseCode": "CS101",
        "courseName": "Introduction to Programming",
        "level": "Undergraduate"
      },
      "semester": {
        "id": 1,
        "type": "FIRST",
        "academicYear": "2024-2025",
        "startDate": "2024-09-01",
        "endDate": "2025-01-15"
      },
      "requiredDocuments": [
        {
          "id": 1,
          "documentType": "SYLLABUS",
          "deadline": "2024-09-15T23:59:59",
          "isRequired": true,
          "submissionStatus": "UPLOADED"
        },
        {
          "id": 2,
          "documentType": "EXAM",
          "deadline": "2024-12-20T23:59:59",
          "isRequired": true,
          "submissionStatus": "NOT_UPLOADED"
        }
      ]
    }
  ]
}
```

### 2. Get Assignment Details

**Endpoint:** `GET /api/professor/assignments/{assignmentId}`

**Example:**
```bash
curl -X GET http://localhost:8080/api/professor/assignments/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Submit Document

**Endpoint:** `POST /api/professor/submissions/{assignmentId}`

**Description:** Submit documents for a course assignment

**Example:**
```bash
curl -X POST http://localhost:8080/api/professor/submissions/1 \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@syllabus.pdf" \
  -F "files=@course_outline.pdf" \
  -F "documentType=SYLLABUS" \
  -F "notes=Course syllabus for Fall 2024"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Documents submitted successfully",
  "data": {
    "submissionId": 1,
    "courseAssignmentId": 1,
    "documentType": "SYLLABUS",
    "fileCount": 2,
    "totalFileSize": 2457600,
    "submittedAt": "2024-09-10T14:30:00",
    "isLateSubmission": false,
    "status": "UPLOADED"
  }
}
```

### 4. Get My Submissions

**Endpoint:** `GET /api/professor/submissions`

**Query Parameters:**
- `semesterId` (optional): Filter by semester
- `status` (optional): Filter by status

**Example:**
```bash
curl -X GET "http://localhost:8080/api/professor/submissions?status=UPLOADED" \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Get Submission Details

**Endpoint:** `GET /api/professor/submissions/{submissionId}`

**Example:**
```bash
curl -X GET http://localhost:8080/api/professor/submissions/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Submission details retrieved successfully",
  "data": {
    "id": 1,
    "courseAssignment": {
      "course": {
        "courseCode": "CS101",
        "courseName": "Introduction to Programming"
      },
      "semester": {
        "type": "FIRST",
        "academicYear": "2024-2025"
      }
    },
    "documentType": "SYLLABUS",
    "submittedAt": "2024-09-10T14:30:00",
    "isLateSubmission": false,
    "status": "UPLOADED",
    "notes": "Course syllabus for Fall 2024",
    "files": [
      {
        "id": 1,
        "originalFilename": "syllabus.pdf",
        "fileSize": 1228800,
        "fileType": "application/pdf",
        "fileUrl": "uploads/1_abc123_1694356200000.pdf",
        "uploadedAt": "2024-09-10T14:30:00"
      }
    ]
  }
}
```

### 6. Update Submission

**Endpoint:** `PUT /api/professor/submissions/{submissionId}`

**Description:** Update an existing submission with new files

**Example:**
```bash
curl -X PUT http://localhost:8080/api/professor/submissions/1 \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@updated_syllabus.pdf" \
  -F "notes=Updated syllabus with corrections"
```

### 7. Delete Submission

**Endpoint:** `DELETE /api/professor/submissions/{submissionId}`

**Example:**
```bash
curl -X DELETE http://localhost:8080/api/professor/submissions/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

## Common Endpoints

### 1. Get My Notifications

**Endpoint:** `GET /api/notifications`

**Query Parameters:**
- `unreadOnly` (optional): true/false

**Example:**
```bash
curl -X GET "http://localhost:8080/api/notifications?unreadOnly=true" \
  -H "Authorization: Bearer $TOKEN"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Notifications retrieved successfully",
  "data": [
    {
      "id": 1,
      "title": "New Course Assignment",
      "message": "You have been assigned to teach CS101 for Fall 2024-2025",
      "type": "NEW_REQUEST",
      "isRead": false,
      "createdAt": "2024-08-25T10:00:00",
      "relatedEntityType": "CourseAssignment",
      "relatedEntityId": 1
    },
    {
      "id": 2,
      "title": "Deadline Approaching",
      "message": "Syllabus for CS101 is due in 3 days",
      "type": "DEADLINE_APPROACHING",
      "isRead": false,
      "createdAt": "2024-09-12T09:00:00",
      "relatedEntityType": "CourseAssignment",
      "relatedEntityId": 1
    }
  ]
}
```

### 2. Mark Notification as Read

**Endpoint:** `PUT /api/notifications/{notificationId}/read`

**Example:**
```bash
curl -X PUT http://localhost:8080/api/notifications/1/read \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Get Academic Years

**Endpoint:** `GET /api/academic-years`

**Example:**
```bash
curl -X GET http://localhost:8080/api/academic-years \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Get Semesters

**Endpoint:** `GET /api/semesters`

**Query Parameters:**
- `academicYearId` (optional): Filter by academic year

**Example:**
```bash
curl -X GET "http://localhost:8080/api/semesters?academicYearId=1" \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Get Departments

**Endpoint:** `GET /api/departments`

**Example:**
```bash
curl -X GET http://localhost:8080/api/departments \
  -H "Authorization: Bearer $TOKEN"
```

### 6. Get Courses

**Endpoint:** `GET /api/courses`

**Query Parameters:**
- `departmentId` (optional): Filter by department

**Example:**
```bash
curl -X GET "http://localhost:8080/api/courses?departmentId=1" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Testing Workflows

### Workflow 1: HOD Reviews Department Submissions

```bash
# Step 1: Login as HOD
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"hod.cs@alquds.edu","password":"password123"}' \
  | jq -r '.data.token')

# Step 2: Get all professors in department
curl -X GET http://localhost:8080/api/hod/professors \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 3: Get department submission report
curl -X GET http://localhost:8080/api/hod/reports/department \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 4: View specific professor's submissions
curl -X GET http://localhost:8080/api/hod/professors/6/submissions \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Workflow 2: Professor Submits Documents

```bash
# Step 1: Login as Professor
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"prof.ahmad.alnajjar@alquds.edu","password":"password123"}' \
  | jq -r '.data.token')

# Step 2: Get my course assignments
curl -X GET http://localhost:8080/api/professor/assignments \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 3: Submit document for an assignment
curl -X POST http://localhost:8080/api/professor/submissions/1 \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@test-document.pdf" \
  -F "documentType=SYLLABUS" \
  -F "notes=Course syllabus" | jq

# Step 4: View my submissions
curl -X GET http://localhost:8080/api/professor/submissions \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Workflow 3: Check Notifications

```bash
# Step 1: Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"prof.ahmad.alnajjar@alquds.edu","password":"password123"}' \
  | jq -r '.data.token')

# Step 2: Get unread notifications
curl -X GET "http://localhost:8080/api/notifications?unreadOnly=true" \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 3: Mark notification as read
curl -X PUT http://localhost:8080/api/notifications/1/read \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 4: Get all notifications
curl -X GET http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Workflow 4: Filter and Search

```bash
# Login as HOD
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"hod.cs@alquds.edu","password":"password123"}' \
  | jq -r '.data.token')

# Get only active professors
curl -X GET "http://localhost:8080/api/hod/professors?status=active" \
  -H "Authorization: Bearer $TOKEN" | jq

# Get submissions for specific semester
curl -X GET "http://localhost:8080/api/hod/reports/department?semesterId=1" \
  -H "Authorization: Bearer $TOKEN" | jq

# Filter by submission status
curl -X GET "http://localhost:8080/api/hod/reports/department?status=OVERDUE" \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Tips for Testing

### Using jq for JSON Parsing

Install `jq` for better JSON output formatting:
```bash
# On Ubuntu/Debian
sudo apt-get install jq

# On macOS
brew install jq

# On Windows (using Chocolatey)
choco install jq
```

### Saving Tokens

Save tokens to environment variables for easier testing:
```bash
# Save HOD token
export HOD_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"hod.cs@alquds.edu","password":"password123"}' \
  | jq -r '.data.token')

# Save Professor token
export PROF_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"prof.ahmad.alnajjar@alquds.edu","password":"password123"}' \
  | jq -r '.data.token')

# Use in requests
curl -X GET http://localhost:8080/api/hod/professors \
  -H "Authorization: Bearer $HOD_TOKEN"
```

### Testing File Uploads

Create test files for upload testing:
```bash
# Create a test PDF
echo "Test document content" > test-document.txt
# Convert to PDF or use actual PDF files

# Upload multiple files
curl -X POST http://localhost:8080/api/professor/submissions/1 \
  -H "Authorization: Bearer $PROF_TOKEN" \
  -F "files=@file1.pdf" \
  -F "files=@file2.pdf" \
  -F "files=@file3.pdf" \
  -F "documentType=ASSIGNMENT"
```

### Error Handling

Common error responses:

**401 Unauthorized:**
```json
{
  "success": false,
  "message": "Invalid credentials",
  "data": null
}
```

**403 Forbidden:**
```json
{
  "success": false,
  "message": "Access denied",
  "data": null
}
```

**404 Not Found:**
```json
{
  "success": false,
  "message": "Resource not found",
  "data": null
}
```

**400 Bad Request:**
```json
{
  "success": false,
  "message": "Invalid request parameters",
  "data": {
    "errors": ["Field 'email' is required"]
  }
}
```

---

## Additional Resources

- See `MOCK_ACCOUNTS.md` for complete list of test accounts
- See `mock_data_guide.md` for mock data structure and relationships
- Check application logs for detailed request/response information
- Use browser developer tools for testing web interface alongside API calls
