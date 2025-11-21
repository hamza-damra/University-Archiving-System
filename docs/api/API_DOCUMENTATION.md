# API Documentation

## Overview

This document provides comprehensive documentation for all REST API endpoints in the Document Archiving System. The system supports three roles with distinct permissions:

- **Deanship**: Global management authority over academic structure, professors, and courses
- **HOD (Head of Department)**: Department-scoped read access to files and submission status
- **Professor**: Upload permissions for assigned courses and read access to department files

## Base URL

```
http://localhost:8080/api
```

## Authentication

All endpoints require authentication using session-based authentication with Spring Security. Include session cookies in all requests.

### Authentication Headers

```
Cookie: JSESSIONID=<session-id>
```

## Common Response Format

All API responses follow a consistent format:

### Success Response

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2025-11-18T10:30:00"
}
```

### Error Response

```json
{
  "success": false,
  "message": "Error description",
  "timestamp": "2025-11-18T10:30:00",
  "status": 400,
  "errorCode": "VALIDATION_ERROR",
  "path": "/api/endpoint"
}
```

## Table of Contents

1. [Deanship Endpoints](#deanship-endpoints)
2. [HOD Endpoints](#hod-endpoints)
3. [Professor Endpoints](#professor-endpoints)
4. [File Explorer Endpoints](#file-explorer-endpoints)
5. [Authentication Endpoints](#authentication-endpoints)

---


## Deanship Endpoints

All Deanship endpoints require `ROLE_DEANSHIP` authority.

### Academic Year Management

#### Create Academic Year

Creates a new academic year with three semesters (First, Second, Summer).

**Endpoint:** `POST /api/deanship/academic-years`

**Request Body:**
```json
{
  "yearCode": "2024-2025",
  "startYear": 2024,
  "endYear": 2025
}
```

**Response:**
```json
{
  "success": true,
  "message": "Academic year created successfully with three semesters",
  "data": {
    "id": 1,
    "yearCode": "2024-2025",
    "startYear": 2024,
    "endYear": 2025,
    "isActive": true,
    "semesters": [
      {
        "id": 1,
        "type": "FIRST",
        "startDate": "2024-09-01",
        "endDate": "2025-01-15",
        "isActive": true
      },
      {
        "id": 2,
        "type": "SECOND",
        "startDate": "2025-02-01",
        "endDate": "2025-06-15",
        "isActive": true
      },
      {
        "id": 3,
        "type": "SUMMER",
        "startDate": "2025-07-01",
        "endDate": "2025-08-31",
        "isActive": true
      }
    ],
    "createdAt": "2025-11-18T10:00:00",
    "updatedAt": "2025-11-18T10:00:00"
  }
}
```

#### Update Academic Year

**Endpoint:** `PUT /api/deanship/academic-years/{id}`

**Path Parameters:**
- `id` (Long): Academic year ID

**Request Body:**
```json
{
  "yearCode": "2024-2025",
  "startYear": 2024,
  "endYear": 2025
}
```

**Response:** Same as Create Academic Year

#### Get All Academic Years

**Endpoint:** `GET /api/deanship/academic-years`

**Response:**
```json
{
  "success": true,
  "message": "Academic years retrieved successfully",
  "data": [
    {
      "id": 1,
      "yearCode": "2024-2025",
      "startYear": 2024,
      "endYear": 2025,
      "isActive": true,
      "semesters": [ ... ]
    }
  ]
}
```

#### Activate Academic Year

Sets an academic year as the active year.

**Endpoint:** `PUT /api/deanship/academic-years/{id}/activate`

**Path Parameters:**
- `id` (Long): Academic year ID

**Response:**
```json
{
  "success": true,
  "message": "Academic year activated successfully",
  "data": "Academic year has been set as active"
}
```


### Professor Management

#### Create Professor

Creates a new professor with automatic professor_id generation.

**Endpoint:** `POST /api/deanship/professors`

**Request Body:**
```json
{
  "email": "professor@university.edu",
  "firstName": "John",
  "lastName": "Doe",
  "password": "SecurePassword123",
  "departmentId": 1
}
```

**Response:**
```json
{
  "success": true,
  "message": "Professor created successfully",
  "data": {
    "id": 10,
    "email": "professor@university.edu",
    "firstName": "John",
    "lastName": "Doe",
    "professorId": "prof_10",
    "role": "ROLE_PROFESSOR",
    "department": {
      "id": 1,
      "name": "Computer Science"
    },
    "isActive": true,
    "createdAt": "2025-11-18T10:00:00"
  }
}
```

#### Update Professor

**Endpoint:** `PUT /api/deanship/professors/{id}`

**Path Parameters:**
- `id` (Long): Professor ID

**Request Body:**
```json
{
  "email": "professor@university.edu",
  "firstName": "John",
  "lastName": "Doe",
  "departmentId": 1
}
```

**Response:** Same as Create Professor

#### Get All Professors

**Endpoint:** `GET /api/deanship/professors`

**Query Parameters:**
- `departmentId` (Long, optional): Filter by department

**Response:**
```json
{
  "success": true,
  "message": "Professors retrieved successfully",
  "data": [
    {
      "id": 10,
      "email": "professor@university.edu",
      "firstName": "John",
      "lastName": "Doe",
      "professorId": "prof_10",
      "role": "ROLE_PROFESSOR",
      "department": {
        "id": 1,
        "name": "Computer Science"
      },
      "isActive": true
    }
  ]
}
```

#### Deactivate Professor

**Endpoint:** `PUT /api/deanship/professors/{id}/deactivate`

**Path Parameters:**
- `id` (Long): Professor ID

**Response:**
```json
{
  "success": true,
  "message": "Professor deactivated successfully",
  "data": "Professor has been deactivated"
}
```

#### Activate Professor

**Endpoint:** `PUT /api/deanship/professors/{id}/activate`

**Path Parameters:**
- `id` (Long): Professor ID

**Response:**
```json
{
  "success": true,
  "message": "Professor activated successfully",
  "data": "Professor has been activated"
}
```


### Course Management

#### Create Course

**Endpoint:** `POST /api/deanship/courses`

**Request Body:**
```json
{
  "courseCode": "CS101",
  "courseName": "Introduction to Computer Science",
  "departmentId": 1,
  "level": "Undergraduate",
  "description": "Fundamentals of computer science"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Course created successfully",
  "data": {
    "id": 5,
    "courseCode": "CS101",
    "courseName": "Introduction to Computer Science",
    "department": {
      "id": 1,
      "name": "Computer Science"
    },
    "level": "Undergraduate",
    "description": "Fundamentals of computer science",
    "isActive": true,
    "createdAt": "2025-11-18T10:00:00"
  }
}
```

#### Update Course

**Endpoint:** `PUT /api/deanship/courses/{id}`

**Path Parameters:**
- `id` (Long): Course ID

**Request Body:** Same as Create Course

**Response:** Same as Create Course

#### Get All Courses

**Endpoint:** `GET /api/deanship/courses`

**Query Parameters:**
- `departmentId` (Long, optional): Filter by department

**Response:**
```json
{
  "success": true,
  "message": "Courses retrieved successfully",
  "data": [
    {
      "id": 5,
      "courseCode": "CS101",
      "courseName": "Introduction to Computer Science",
      "department": {
        "id": 1,
        "name": "Computer Science"
      },
      "level": "Undergraduate",
      "isActive": true
    }
  ]
}
```

#### Deactivate Course

**Endpoint:** `PUT /api/deanship/courses/{id}/deactivate`

**Path Parameters:**
- `id` (Long): Course ID

**Response:**
```json
{
  "success": true,
  "message": "Course deactivated successfully",
  "data": "Course has been deactivated"
}
```


### Course Assignment Management

#### Assign Course

Assigns a course to a professor for a specific semester.

**Endpoint:** `POST /api/deanship/course-assignments`

**Request Body:**
```json
{
  "semesterId": 1,
  "courseId": 5,
  "professorId": 10
}
```

**Response:**
```json
{
  "success": true,
  "message": "Course assigned successfully",
  "data": {
    "id": 15,
    "semester": {
      "id": 1,
      "type": "FIRST",
      "academicYear": {
        "yearCode": "2024-2025"
      }
    },
    "course": {
      "id": 5,
      "courseCode": "CS101",
      "courseName": "Introduction to Computer Science"
    },
    "professor": {
      "id": 10,
      "firstName": "John",
      "lastName": "Doe",
      "professorId": "prof_10"
    },
    "isActive": true,
    "createdAt": "2025-11-18T10:00:00"
  }
}
```

#### Unassign Course

**Endpoint:** `DELETE /api/deanship/course-assignments/{id}`

**Path Parameters:**
- `id` (Long): Course assignment ID

**Response:**
```json
{
  "success": true,
  "message": "Course unassigned successfully",
  "data": "Course assignment has been removed"
}
```

#### Get Course Assignments

**Endpoint:** `GET /api/deanship/course-assignments`

**Query Parameters:**
- `semesterId` (Long, required): Semester ID
- `professorId` (Long, optional): Filter by professor

**Response:**
```json
{
  "success": true,
  "message": "Course assignments retrieved successfully",
  "data": [
    {
      "id": 15,
      "semester": { ... },
      "course": { ... },
      "professor": { ... },
      "isActive": true
    }
  ]
}
```

### Required Document Type Management

#### Add Required Document Type

**Endpoint:** `POST /api/deanship/courses/{courseId}/required-documents`

**Path Parameters:**
- `courseId` (Long): Course ID

**Request Body:**
```json
{
  "documentType": "SYLLABUS",
  "semesterId": 1,
  "deadline": "2024-09-15T23:59:59",
  "isRequired": true,
  "maxFileCount": 5,
  "maxTotalSizeMb": 50,
  "allowedFileExtensions": ["pdf", "zip"]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Required document type added successfully",
  "data": {
    "id": 20,
    "course": {
      "id": 5,
      "courseCode": "CS101"
    },
    "semester": {
      "id": 1,
      "type": "FIRST"
    },
    "documentType": "SYLLABUS",
    "deadline": "2024-09-15T23:59:59",
    "isRequired": true,
    "maxFileCount": 5,
    "maxTotalSizeMb": 50,
    "allowedFileExtensions": ["pdf", "zip"]
  }
}
```

### Department Management

#### Get All Departments

**Endpoint:** `GET /api/deanship/departments`

**Response:**
```json
{
  "success": true,
  "message": "Departments retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Computer Science",
      "code": "CS"
    },
    {
      "id": 2,
      "name": "Mathematics",
      "code": "MATH"
    }
  ]
}
```

### Reports

#### Get System-Wide Report

Shows submission statistics across all departments for a semester.

**Endpoint:** `GET /api/deanship/reports/system-wide`

**Query Parameters:**
- `semesterId` (Long, required): Semester ID

**Response:**
```json
{
  "success": true,
  "message": "System-wide report generated successfully",
  "data": {
    "semesterId": 1,
    "semesterName": "First Semester 2024-2025",
    "generatedAt": "2025-11-18T10:00:00",
    "departmentReports": [
      {
        "departmentId": 1,
        "departmentName": "Computer Science",
        "totalProfessors": 10,
        "totalCourses": 15,
        "totalRequiredDocuments": 45,
        "submittedDocuments": 40,
        "missingDocuments": 3,
        "overdueDocuments": 2
      }
    ],
    "overallStatistics": {
      "totalProfessors": 25,
      "totalCourses": 40,
      "totalRequiredDocuments": 120,
      "submittedDocuments": 110,
      "missingDocuments": 7,
      "overdueDocuments": 3
    }
  }
}
```

---


## HOD Endpoints

All HOD endpoints require `ROLE_HOD` authority and are department-scoped.

### Dashboard

#### Get Dashboard Overview

Returns total professors, courses, and submission statistics for HOD's department.

**Endpoint:** `GET /api/hod/dashboard/overview`

**Query Parameters:**
- `semesterId` (Long, required): Semester ID

**Response:**
```json
{
  "success": true,
  "message": "Dashboard overview retrieved successfully",
  "data": {
    "semesterId": 1,
    "semesterName": "First Semester 2024-2025",
    "departmentId": 1,
    "departmentName": "Computer Science",
    "generatedAt": "2025-11-18T10:00:00",
    "totalProfessors": 10,
    "totalCourses": 15,
    "totalCourseAssignments": 20,
    "submissionStatistics": {
      "totalRequiredDocuments": 60,
      "submittedDocuments": 55,
      "missingDocuments": 3,
      "overdueDocuments": 2
    }
  }
}
```

### Submission Status

#### Get Submission Status

Returns professor submission status with optional filtering.

**Endpoint:** `GET /api/hod/submissions/status`

**Query Parameters:**
- `semesterId` (Long, required): Semester ID
- `courseCode` (String, optional): Filter by course code
- `documentType` (DocumentTypeEnum, optional): Filter by document type (SYLLABUS, EXAM, ASSIGNMENT, etc.)
- `status` (SubmissionStatus, optional): Filter by status (NOT_UPLOADED, UPLOADED, OVERDUE)

**Response:**
```json
{
  "success": true,
  "message": "Submission status retrieved successfully",
  "data": {
    "semesterId": 1,
    "semesterName": "First Semester 2024-2025",
    "departmentId": 1,
    "departmentName": "Computer Science",
    "generatedAt": "2025-11-18T10:00:00",
    "rows": [
      {
        "professorId": 10,
        "professorName": "John Doe",
        "courseCode": "CS101",
        "courseName": "Introduction to Computer Science",
        "documentStatuses": {
          "SYLLABUS": "UPLOADED",
          "EXAM": "NOT_UPLOADED",
          "ASSIGNMENT": "OVERDUE"
        }
      }
    ],
    "statistics": {
      "totalProfessors": 10,
      "totalCourses": 15,
      "totalRequiredDocuments": 60,
      "submittedDocuments": 55,
      "missingDocuments": 3,
      "overdueDocuments": 2
    }
  }
}
```

### Reports

#### Get Professor Submission Report

**Endpoint:** `GET /api/hod/reports/professor-submissions`

**Query Parameters:**
- `semesterId` (Long, required): Semester ID

**Response:** Same as Get Submission Status

#### Export Report to PDF

**Endpoint:** `GET /api/hod/reports/professor-submissions/pdf`

**Query Parameters:**
- `semesterId` (Long, required): Semester ID

**Response:** Binary PDF file

**Headers:**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="professor-submission-report-1-1731924000000.pdf"
```

### File Explorer

#### Get File Explorer Root

Returns the root node of the file explorer (department-scoped).

**Endpoint:** `GET /api/hod/file-explorer/root`

**Query Parameters:**
- `academicYearId` (Long, required): Academic year ID
- `semesterId` (Long, required): Semester ID

**Response:**
```json
{
  "success": true,
  "message": "File explorer root retrieved successfully",
  "data": {
    "path": "/2024-2025/first",
    "name": "First Semester",
    "type": "SEMESTER",
    "entityId": 1,
    "canRead": true,
    "canWrite": false,
    "canDelete": false,
    "children": [
      {
        "path": "/2024-2025/first/prof_10",
        "name": "John Doe",
        "type": "PROFESSOR",
        "entityId": 10,
        "canRead": true,
        "canWrite": false,
        "canDelete": false
      }
    ]
  }
}
```

#### Get File Explorer Node

**Endpoint:** `GET /api/hod/file-explorer/node`

**Query Parameters:**
- `path` (String, required): Node path (e.g., "/2024-2025/first/prof_10/CS101")

**Response:**
```json
{
  "success": true,
  "message": "File explorer node retrieved successfully",
  "data": {
    "path": "/2024-2025/first/prof_10/CS101",
    "name": "CS101",
    "type": "COURSE",
    "entityId": 5,
    "canRead": true,
    "canWrite": false,
    "canDelete": false,
    "children": [
      {
        "path": "/2024-2025/first/prof_10/CS101/SYLLABUS",
        "name": "Syllabus",
        "type": "DOCUMENT_TYPE",
        "canRead": true,
        "canWrite": false,
        "canDelete": false
      }
    ]
  }
}
```

#### Download File

**Endpoint:** `GET /api/hod/files/{fileId}/download`

**Path Parameters:**
- `fileId` (Long): File ID

**Response:** Binary file

**Headers:**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="syllabus.pdf"
```

---


## Professor Endpoints

All Professor endpoints require `ROLE_PROFESSOR` authority.

### Dashboard

#### Get My Courses

Returns professor's courses with submission status for a semester.

**Endpoint:** `GET /api/professor/dashboard/courses`

**Query Parameters:**
- `semesterId` (Long, required): Semester ID

**Response:**
```json
{
  "success": true,
  "message": "Courses retrieved successfully",
  "data": [
    {
      "courseAssignmentId": 15,
      "courseCode": "CS101",
      "courseName": "Introduction to Computer Science",
      "semesterId": 1,
      "semesterName": "First Semester",
      "requiredDocuments": [
        {
          "documentType": "SYLLABUS",
          "deadline": "2024-09-15T23:59:59",
          "status": "UPLOADED",
          "submissionId": 25,
          "submittedAt": "2024-09-10T14:30:00",
          "isLateSubmission": false,
          "fileCount": 1
        },
        {
          "documentType": "EXAM",
          "deadline": "2024-12-01T23:59:59",
          "status": "NOT_UPLOADED",
          "submissionId": null,
          "submittedAt": null,
          "isLateSubmission": false,
          "fileCount": 0
        }
      ]
    }
  ]
}
```

#### Get Dashboard Overview

**Endpoint:** `GET /api/professor/dashboard/overview`

**Query Parameters:**
- `semesterId` (Long, required): Semester ID

**Response:**
```json
{
  "success": true,
  "message": "Dashboard overview retrieved successfully",
  "data": {
    "semesterId": 1,
    "semesterName": "First Semester 2024-2025",
    "totalCourses": 3,
    "totalRequiredDocuments": 9,
    "submittedDocuments": 7,
    "missingDocuments": 1,
    "overdueDocuments": 1,
    "upcomingDeadlines": [
      {
        "courseCode": "CS101",
        "documentType": "EXAM",
        "deadline": "2024-12-01T23:59:59",
        "daysRemaining": 5
      }
    ]
  }
}
```

### File Upload

#### Upload Files

Uploads files for a course assignment and document type.

**Endpoint:** `POST /api/professor/submissions/upload`

**Content-Type:** `multipart/form-data`

**Form Parameters:**
- `courseAssignmentId` (Long, required): Course assignment ID
- `documentType` (DocumentTypeEnum, required): Document type (SYLLABUS, EXAM, ASSIGNMENT, etc.)
- `notes` (String, optional): Additional notes
- `files` (MultipartFile[], required): Files to upload

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/professor/submissions/upload \
  -H "Cookie: JSESSIONID=..." \
  -F "courseAssignmentId=15" \
  -F "documentType=SYLLABUS" \
  -F "notes=Updated syllabus for Fall 2024" \
  -F "files=@syllabus.pdf" \
  -F "files=@course-outline.pdf"
```

**Response:**
```json
{
  "success": true,
  "message": "Files uploaded successfully",
  "data": {
    "id": 25,
    "courseAssignment": {
      "id": 15,
      "course": {
        "courseCode": "CS101"
      }
    },
    "documentType": "SYLLABUS",
    "professor": {
      "id": 10,
      "firstName": "John",
      "lastName": "Doe"
    },
    "uploadedFiles": [
      {
        "id": 30,
        "originalFilename": "syllabus.pdf",
        "fileSize": 2500000,
        "fileType": "application/pdf",
        "fileOrder": 0
      },
      {
        "id": 31,
        "originalFilename": "course-outline.pdf",
        "fileSize": 1800000,
        "fileType": "application/pdf",
        "fileOrder": 1
      }
    ],
    "submittedAt": "2025-11-18T10:00:00",
    "isLateSubmission": false,
    "status": "UPLOADED",
    "notes": "Updated syllabus for Fall 2024",
    "fileCount": 2,
    "totalFileSize": 4300000
  }
}
```

#### Replace Files

Replaces files for an existing submission.

**Endpoint:** `PUT /api/professor/submissions/{submissionId}/replace`

**Path Parameters:**
- `submissionId` (Long): Submission ID

**Content-Type:** `multipart/form-data`

**Form Parameters:**
- `notes` (String, optional): Additional notes
- `files` (MultipartFile[], required): New files to upload

**Response:** Same as Upload Files

### Submissions

#### Get My Submissions

**Endpoint:** `GET /api/professor/submissions`

**Query Parameters:**
- `semesterId` (Long, required): Semester ID

**Response:**
```json
{
  "success": true,
  "message": "Submissions retrieved successfully",
  "data": [
    {
      "id": 25,
      "courseAssignment": {
        "id": 15,
        "course": {
          "courseCode": "CS101",
          "courseName": "Introduction to Computer Science"
        }
      },
      "documentType": "SYLLABUS",
      "submittedAt": "2025-11-18T10:00:00",
      "isLateSubmission": false,
      "status": "UPLOADED",
      "fileCount": 2,
      "totalFileSize": 4300000
    }
  ]
}
```

#### Get Submission

**Endpoint:** `GET /api/professor/submissions/{submissionId}`

**Path Parameters:**
- `submissionId` (Long): Submission ID

**Response:**
```json
{
  "success": true,
  "message": "Submission retrieved successfully",
  "data": {
    "id": 25,
    "courseAssignment": { ... },
    "documentType": "SYLLABUS",
    "uploadedFiles": [ ... ],
    "submittedAt": "2025-11-18T10:00:00",
    "isLateSubmission": false,
    "status": "UPLOADED",
    "notes": "Updated syllabus for Fall 2024",
    "fileCount": 2,
    "totalFileSize": 4300000
  }
}
```

### File Explorer

#### Get File Explorer Root

**Endpoint:** `GET /api/professor/file-explorer/root`

**Query Parameters:**
- `academicYearId` (Long, required): Academic year ID
- `semesterId` (Long, required): Semester ID

**Response:** Same structure as HOD File Explorer Root

#### Get File Explorer Node

**Endpoint:** `GET /api/professor/file-explorer/node`

**Query Parameters:**
- `path` (String, required): Node path

**Response:** Same structure as HOD File Explorer Node

#### Download File

**Endpoint:** `GET /api/professor/files/{fileId}/download`

**Path Parameters:**
- `fileId` (Long): File ID

**Response:** Binary file

---


## File Explorer Endpoints

Shared endpoints for all roles with role-based permissions.

### Get Root Node

**Endpoint:** `GET /api/file-explorer/root`

**Query Parameters:**
- `academicYearId` (Long, required): Academic year ID
- `semesterId` (Long, required): Semester ID

**Permissions:**
- **Deanship**: All professors in semester
- **HOD**: Professors in HOD's department only
- **Professor**: All professors in same department

**Response:**
```json
{
  "success": true,
  "message": "Root node retrieved successfully",
  "data": {
    "path": "/2024-2025/first",
    "name": "First Semester",
    "type": "SEMESTER",
    "entityId": 1,
    "canRead": true,
    "canWrite": false,
    "canDelete": false,
    "children": [
      {
        "path": "/2024-2025/first/prof_10",
        "name": "John Doe",
        "type": "PROFESSOR",
        "entityId": 10,
        "canRead": true,
        "canWrite": true,
        "canDelete": false
      }
    ]
  }
}
```

### Get Node

**Endpoint:** `GET /api/file-explorer/node`

**Query Parameters:**
- `path` (String, required): Node path (format: /year/semester/professor/course/documentType)

**Response:**
```json
{
  "success": true,
  "message": "Node retrieved successfully",
  "data": {
    "path": "/2024-2025/first/prof_10/CS101/SYLLABUS",
    "name": "Syllabus",
    "type": "DOCUMENT_TYPE",
    "canRead": true,
    "canWrite": true,
    "canDelete": true,
    "children": [
      {
        "path": "/2024-2025/first/prof_10/CS101/SYLLABUS/30",
        "name": "syllabus.pdf",
        "type": "FILE",
        "entityId": 30,
        "metadata": {
          "fileSize": 2500000,
          "fileType": "application/pdf",
          "uploadedAt": "2025-11-18T10:00:00",
          "uploadedBy": "John Doe"
        },
        "canRead": true,
        "canWrite": false,
        "canDelete": true
      }
    ]
  }
}
```

### Get Breadcrumbs

**Endpoint:** `GET /api/file-explorer/breadcrumbs`

**Query Parameters:**
- `path` (String, required): Node path

**Response:**
```json
{
  "success": true,
  "message": "Breadcrumbs generated successfully",
  "data": [
    {
      "name": "Home",
      "path": "/"
    },
    {
      "name": "2024-2025",
      "path": "/2024-2025"
    },
    {
      "name": "First Semester",
      "path": "/2024-2025/first"
    },
    {
      "name": "John Doe",
      "path": "/2024-2025/first/prof_10"
    },
    {
      "name": "CS101",
      "path": "/2024-2025/first/prof_10/CS101"
    }
  ]
}
```

### Get File Metadata

**Endpoint:** `GET /api/file-explorer/files/{fileId}`

**Path Parameters:**
- `fileId` (Long): File ID

**Response:**
```json
{
  "success": true,
  "message": "File metadata retrieved successfully",
  "data": {
    "id": 30,
    "originalFilename": "syllabus.pdf",
    "fileUrl": "uploads/2024-2025/first/prof_10/CS101/SYLLABUS/syllabus_uuid.pdf",
    "fileSize": 2500000,
    "fileType": "application/pdf",
    "fileOrder": 0,
    "description": null,
    "createdAt": "2025-11-18T10:00:00",
    "documentSubmission": {
      "id": 25,
      "professor": {
        "firstName": "John",
        "lastName": "Doe"
      }
    }
  }
}
```

### Download File

**Endpoint:** `GET /api/file-explorer/files/{fileId}/download`

**Path Parameters:**
- `fileId` (Long): File ID

**Permissions:**
- **Deanship**: Can download all files
- **HOD**: Can download files in their department
- **Professor**: Can download files in their department

**Response:** Binary file

**Headers:**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="syllabus.pdf"
```

---


## Authentication Endpoints

### Login

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "user@university.edu",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "id": 10,
    "email": "user@university.edu",
    "firstName": "John",
    "lastName": "Doe",
    "role": "ROLE_PROFESSOR",
    "department": {
      "id": 1,
      "name": "Computer Science"
    }
  }
}
```

**Headers:**
```
Set-Cookie: JSESSIONID=<session-id>; Path=/; HttpOnly
```

### Logout

**Endpoint:** `POST /api/auth/logout`

**Response:**
```json
{
  "success": true,
  "message": "Logout successful"
}
```

### Get Current User

**Endpoint:** `GET /api/auth/me`

**Response:**
```json
{
  "success": true,
  "message": "Current user retrieved successfully",
  "data": {
    "id": 10,
    "email": "user@university.edu",
    "firstName": "John",
    "lastName": "Doe",
    "role": "ROLE_PROFESSOR",
    "professorId": "prof_10",
    "department": {
      "id": 1,
      "name": "Computer Science"
    }
  }
}
```

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `RESOURCE_NOT_FOUND` | 404 | Requested resource not found |
| `UNAUTHORIZED_ACCESS` | 403 | User does not have permission |
| `FILE_UPLOAD_ERROR` | 400 | File upload validation failed |
| `AUTHENTICATION_ERROR` | 401 | Authentication required or failed |
| `DUPLICATE_RESOURCE` | 409 | Resource already exists |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected server error |

## Common Error Scenarios

### File Upload Errors

**File Type Not Allowed:**
```json
{
  "success": false,
  "message": "File type not allowed. Allowed types: pdf, zip",
  "errorCode": "FILE_UPLOAD_ERROR",
  "status": 400,
  "details": {
    "filename": "document.docx",
    "allowedTypes": ["pdf", "zip"]
  }
}
```

**File Size Exceeds Limit:**
```json
{
  "success": false,
  "message": "Total file size exceeds maximum allowed size",
  "errorCode": "FILE_UPLOAD_ERROR",
  "status": 400,
  "details": {
    "maxSizeMb": 50,
    "actualSizeMb": 75
  }
}
```

### Permission Errors

**Unauthorized Access:**
```json
{
  "success": false,
  "message": "You do not have permission to access this resource",
  "errorCode": "UNAUTHORIZED_ACCESS",
  "status": 403,
  "path": "/api/hod/files/123/download"
}
```

### Resource Not Found

```json
{
  "success": false,
  "message": "Course assignment not found with ID: 999",
  "errorCode": "RESOURCE_NOT_FOUND",
  "status": 404,
  "path": "/api/professor/submissions/upload"
}
```

---

## Data Models

### DocumentTypeEnum

```
SYLLABUS
EXAM
ASSIGNMENT
PROJECT_DOCS
LECTURE_NOTES
OTHER
```

### SubmissionStatus

```
NOT_UPLOADED - Document type has not been uploaded yet
UPLOADED - Document has been uploaded on time
OVERDUE - Deadline passed and document not uploaded
```

### SemesterType

```
FIRST - Fall semester
SECOND - Spring semester
SUMMER - Summer semester
```

### NodeType

```
YEAR - Academic year node
SEMESTER - Semester node
PROFESSOR - Professor folder node
COURSE - Course folder node
DOCUMENT_TYPE - Document type folder node
FILE - Individual file node
```

---

## Rate Limiting

Currently, no rate limiting is implemented. Future versions may include rate limiting for file upload endpoints.

## Pagination

Some endpoints support pagination using the following query parameters:

- `page` (int, default: 0): Page number (zero-indexed)
- `size` (int, default: 10): Number of items per page
- `sortBy` (string, default: "createdAt"): Field to sort by
- `sortDir` (string, default: "desc"): Sort direction ("asc" or "desc")

**Example:**
```
GET /api/hod/professors?page=0&size=20&sortBy=lastName&sortDir=asc
```

---

## File Storage Structure

Files are stored in a hierarchical structure on the filesystem:

```
uploads/
├── 2024-2025/
│   ├── first/
│   │   ├── prof_10/
│   │   │   ├── CS101/
│   │   │   │   ├── SYLLABUS/
│   │   │   │   │   ├── syllabus_uuid.pdf
│   │   │   │   ├── EXAM/
│   │   │   │   └── ASSIGNMENT/
│   │   │   └── CS102/
│   │   └── prof_11/
│   ├── second/
│   └── summer/
└── 2025-2026/
```

Path pattern: `{year}/{semester}/{professorId}/{courseCode}/{documentType}/{filename}`

---

## Security Considerations

1. **Authentication**: All endpoints require authentication except login
2. **Authorization**: Role-based access control enforced at method level
3. **Department Scoping**: HOD and Professor access is scoped to their department
4. **File Access**: File download permissions checked before serving files
5. **CSRF Protection**: Enabled for state-changing operations
6. **Session Management**: Session-based authentication with secure cookies

---

## Support

For API support or questions, contact the development team or refer to the system documentation.

**Last Updated:** November 18, 2025  
**API Version:** 1.0.0
