# Professor Dashboard API Documentation

## Overview

This document provides comprehensive documentation for the Professor Dashboard REST API endpoints. The API enables professors to view assigned courses, upload documents, browse files, and manage notifications within the semester-based Document Archiving System.

## Table of Contents

1. [Authentication & Authorization](#authentication--authorization)
2. [API Response Format](#api-response-format)
3. [Error Handling](#error-handling)
4. [Academic Year & Semester Endpoints](#academic-year--semester-endpoints)
5. [Dashboard Endpoints](#dashboard-endpoints)
6. [File Upload & Submission Endpoints](#file-upload--submission-endpoints)
7. [File Explorer Endpoints](#file-explorer-endpoints)
8. [Notification Endpoints](#notification-endpoints)
9. [Legacy Endpoints (Deprecated)](#legacy-endpoints-deprecated)

---

## Authentication & Authorization

### Authentication Method
- **Type**: Session-based authentication
- **Required Role**: `ROLE_PROFESSOR`
- **Session Timeout**: 30 minutes of inactivity

### Authorization Rules
All endpoints in `/api/professor/**` require:
- User must be authenticated
- User must have `ROLE_PROFESSOR` role
- Some endpoints have additional permission checks (e.g., ownership of submissions)

### Session Management
- Login endpoint: `/api/auth/login`
- Logout endpoint: `/api/auth/logout`
- Session expired: Returns 401 UNAUTHORIZED, frontend redirects to login

---

## API Response Format

All endpoints return a standard `ApiResponse` wrapper:

```json
{
  "success": true,
  "message": "Operation description",
  "data": { ... },
  "timestamp": "2024-11-19T10:30:00Z"
}
```

### Fields
- `success` (boolean): Indicates if the operation was successful
- `message` (string): Human-readable description of the result
- `data` (object/array): The actual response data (null on error)
- `timestamp` (string): ISO 8601 timestamp of the response

---

## Error Handling

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Request successful |
| 201 | CREATED | Resource created successfully |
| 400 | BAD REQUEST | Invalid request parameters or validation failure |
| 401 | UNAUTHORIZED | User not authenticated or session expired |
| 403 | FORBIDDEN | User does not have required role or permissions |
| 404 | NOT FOUND | Requested resource not found |
| 500 | INTERNAL SERVER ERROR | Server error occurred |

### Error Response Format

```json
{
  "success": false,
  "message": "Error description with details",
  "data": null,
  "timestamp": "2024-11-19T10:30:00Z"
}
```

### Common Error Scenarios

#### File Upload Validation Errors (400)
```json
{
  "success": false,
  "message": "Invalid file type. Only PDF and ZIP files are allowed.",
  "data": null
}
```

#### Authorization Errors (403)
```json
{
  "success": false,
  "message": "You do not have permission to access this file",
  "data": null
}
```

#### Resource Not Found (404)
```json
{
  "success": false,
  "message": "Course assignment not found with ID: 123",
  "data": null
}
```

---

## Academic Year & Semester Endpoints

### Get All Academic Years

Retrieves all academic years in the system.

**Endpoint**: `GET /api/professor/academic-years`

**Authentication**: Required (ROLE_PROFESSOR)

**Response**:
```json
{
  "success": true,
  "message": "Academic years retrieved successfully",
  "data": [
    {
      "id": 1,
      "yearCode": "2024-2025",
      "startDate": "2024-09-01",
      "endDate": "2025-08-31",
      "isActive": true,
      "createdAt": "2024-08-01T10:00:00",
      "updatedAt": "2024-08-01T10:00:00"
    }
  ]
}
```

---

### Get Semesters by Academic Year

Retrieves all semesters for a specific academic year.

**Endpoint**: `GET /api/professor/academic-years/{academicYearId}/semesters`

**Authentication**: Required (ROLE_PROFESSOR)

**Path Parameters**:
- `academicYearId` (Long): The ID of the academic year

**Response**:
```json
{
  "success": true,
  "message": "Semesters retrieved successfully",
  "data": [
    {
      "id": 1,
      "type": "FIRST",
      "startDate": "2024-09-01",
      "endDate": "2025-01-15",
      "isActive": true,
      "academicYearId": 1,
      "createdAt": "2024-08-01T10:00:00",
      "updatedAt": "2024-08-01T10:00:00"
    },
    {
      "id": 2,
      "type": "SECOND",
      "startDate": "2025-02-01",
      "endDate": "2025-06-15",
      "isActive": false,
      "academicYearId": 1
    }
  ]
}
```

**Semester Types**:
- `FIRST` - First semester (Fall)
- `SECOND` - Second semester (Spring)
- `SUMMER` - Summer semester

---

## Dashboard Endpoints

### Get My Courses

Retrieves all courses assigned to the professor for a specific semester, including submission status for each required document type.

**Endpoint**: `GET /api/professor/dashboard/courses`

**Authentication**: Required (ROLE_PROFESSOR)

**Query Parameters**:
- `semesterId` (Long, required): The ID of the semester

**Example Request**:
```
GET /api/professor/dashboard/courses?semesterId=1
```

**Response**:
```json
{
  "success": true,
  "message": "Courses retrieved successfully",
  "data": [
    {
      "courseAssignmentId": 1,
      "courseCode": "CS101",
      "courseName": "Introduction to Computer Science",
      "departmentName": "Computer Science",
      "courseLevel": "UNDERGRADUATE",
      "semesterType": "FIRST",
      "academicYearCode": "2024-2025",
      "documentStatuses": {
        "SYLLABUS": {
          "documentType": "SYLLABUS",
          "status": "UPLOADED",
          "submissionId": 10,
          "fileCount": 1,
          "totalFileSize": 524288,
          "submittedAt": "2024-11-15T10:30:00",
          "isLateSubmission": false,
          "deadline": "2024-11-20T23:59:59",
          "maxFileCount": 1,
          "maxTotalSizeMb": 10,
          "allowedFileExtensions": ["pdf"]
        },
        "EXAM": {
          "documentType": "EXAM",
          "status": "NOT_UPLOADED",
          "submissionId": null,
          "fileCount": 0,
          "totalFileSize": 0,
          "submittedAt": null,
          "isLateSubmission": false,
          "deadline": "2024-12-01T23:59:59",
          "maxFileCount": 5,
          "maxTotalSizeMb": 50,
          "allowedFileExtensions": ["pdf", "zip"]
        },
        "ASSIGNMENT": {
          "documentType": "ASSIGNMENT",
          "status": "OVERDUE",
          "submissionId": null,
          "fileCount": 0,
          "totalFileSize": 0,
          "submittedAt": null,
          "isLateSubmission": false,
          "deadline": "2024-11-10T23:59:59",
          "maxFileCount": 3,
          "maxTotalSizeMb": 30,
          "allowedFileExtensions": ["pdf", "zip"]
        }
      }
    }
  ]
}
```

**Document Status Values**:
- `NOT_UPLOADED` - No files have been submitted
- `UPLOADED` - Files have been successfully submitted
- `OVERDUE` - Deadline has passed and no files were submitted

**Document Types**:
- `SYLLABUS` - Course syllabus
- `EXAM` - Exam materials
- `ASSIGNMENT` - Assignment materials
- `PROJECT_DOCS` - Project documentation
- `LECTURE_NOTES` - Lecture notes
- `OTHER` - Other documents

---

### Get Dashboard Overview

Retrieves summary statistics for the professor's dashboard.

**Endpoint**: `GET /api/professor/dashboard/overview`

**Authentication**: Required (ROLE_PROFESSOR)

**Query Parameters**:
- `semesterId` (Long, required): The ID of the semester

**Example Request**:
```
GET /api/professor/dashboard/overview?semesterId=1
```

**Response**:
```json
{
  "success": true,
  "message": "Dashboard overview retrieved successfully",
  "data": {
    "totalCourses": 3,
    "submittedDocuments": 5,
    "pendingDocuments": 7,
    "overdueDocuments": 2,
    "upcomingDeadlines": [
      {
        "courseCode": "CS101",
        "courseName": "Introduction to Computer Science",
        "documentType": "EXAM",
        "deadline": "2024-12-01T23:59:59",
        "hoursRemaining": 72
      },
      {
        "courseCode": "CS102",
        "courseName": "Data Structures",
        "documentType": "SYLLABUS",
        "deadline": "2024-11-25T23:59:59",
        "hoursRemaining": 144
      }
    ]
  }
}
```

---

## File Upload & Submission Endpoints

### Upload Files

Uploads one or more files for a specific course assignment and document type.

**Endpoint**: `POST /api/professor/submissions/upload`

**Authentication**: Required (ROLE_PROFESSOR)

**Content-Type**: `multipart/form-data`

**Query Parameters**:
- `courseAssignmentId` (Long, required): The ID of the course assignment
- `documentType` (String, required): The type of document (SYLLABUS, EXAM, etc.)
- `notes` (String, optional): Optional notes about the submission

**Form Data**:
- `files` (MultipartFile[], required): Array of files to upload

**File Validation Rules**:
- Allowed file types: PDF, ZIP only
- Maximum file count: Defined per document type (typically 1-5 files)
- Maximum total size: Defined per document type (typically 10-50 MB)
- Filenames are sanitized to prevent security issues

**File Storage Pattern**:
```
{uploadDir}/{year}/{semester}/{professorId}/{courseCode}/{documentType}/{filename}
```

**Example Request**:
```
POST /api/professor/submissions/upload?courseAssignmentId=1&documentType=SYLLABUS&notes=Updated syllabus
Content-Type: multipart/form-data

files: [syllabus_v2.pdf]
```

**Success Response (201 CREATED)**:
```json
{
  "success": true,
  "message": "Files uploaded successfully",
  "data": {
    "id": 10,
    "courseAssignmentId": 1,
    "documentType": "SYLLABUS",
    "fileCount": 1,
    "totalFileSize": 524288,
    "submittedAt": "2024-11-19T10:30:00",
    "isLateSubmission": false,
    "notes": "Updated syllabus",
    "professorId": 5,
    "uploadedFiles": [
      {
        "id": 100,
        "originalFilename": "syllabus_v2.pdf",
        "fileSize": 524288,
        "fileType": "application/pdf",
        "fileUrl": "/uploads/2024-2025/FIRST/5/CS101/SYLLABUS/syllabus_v2.pdf",
        "uploadedAt": "2024-11-19T10:30:00"
      }
    ]
  }
}
```

**Error Responses**:

Invalid file type (400):
```json
{
  "success": false,
  "message": "Invalid file type. Only PDF and ZIP files are allowed.",
  "data": null
}
```

File count exceeds limit (400):
```json
{
  "success": false,
  "message": "Maximum 1 file(s) allowed for SYLLABUS. You uploaded 3 files.",
  "data": null
}
```

File size exceeds limit (400):
```json
{
  "success": false,
  "message": "Total file size (52428800 bytes) exceeds maximum allowed (10485760 bytes).",
  "data": null
}
```

Professor not assigned to course (403):
```json
{
  "success": false,
  "message": "Professor is not assigned to this course",
  "data": null
}
```

---

### Replace Files

Replaces previously uploaded files for an existing submission.

**Endpoint**: `PUT /api/professor/submissions/{submissionId}/replace`

**Authentication**: Required (ROLE_PROFESSOR)

**Authorization**: Professor must own the submission

**Content-Type**: `multipart/form-data`

**Path Parameters**:
- `submissionId` (Long, required): The ID of the submission to replace files for

**Query Parameters**:
- `notes` (String, optional): Optional notes about the replacement

**Form Data**:
- `files` (MultipartFile[], required): Array of new files to upload

**Behavior**:
- Old files are deleted from storage
- New files are uploaded
- Submission timestamp is updated
- Late submission status is recalculated based on deadline

**Example Request**:
```
PUT /api/professor/submissions/10/replace?notes=Corrected version
Content-Type: multipart/form-data

files: [syllabus_corrected.pdf]
```

**Success Response (200 OK)**:
```json
{
  "success": true,
  "message": "Files replaced successfully",
  "data": {
    "id": 10,
    "courseAssignmentId": 1,
    "documentType": "SYLLABUS",
    "fileCount": 1,
    "totalFileSize": 524288,
    "submittedAt": "2024-11-19T14:30:00",
    "isLateSubmission": false,
    "notes": "Corrected version",
    "uploadedFiles": [
      {
        "id": 101,
        "originalFilename": "syllabus_corrected.pdf",
        "fileSize": 524288,
        "fileType": "application/pdf"
      }
    ]
  }
}
```

**Error Responses**:

Professor doesn't own submission (403):
```json
{
  "success": false,
  "message": "Professor does not own submission ID: 10",
  "data": null
}
```

---

### Get My Submissions

Retrieves all document submissions created by the professor for a specific semester.

**Endpoint**: `GET /api/professor/submissions`

**Authentication**: Required (ROLE_PROFESSOR)

**Query Parameters**:
- `semesterId` (Long, required): The ID of the semester

**Example Request**:
```
GET /api/professor/submissions?semesterId=1
```

**Response**:
```json
{
  "success": true,
  "message": "Submissions retrieved successfully",
  "data": [
    {
      "id": 10,
      "courseAssignmentId": 1,
      "documentType": "SYLLABUS",
      "fileCount": 1,
      "totalFileSize": 524288,
      "submittedAt": "2024-11-15T10:30:00",
      "isLateSubmission": false,
      "notes": "Initial submission"
    },
    {
      "id": 11,
      "courseAssignmentId": 1,
      "documentType": "EXAM",
      "fileCount": 3,
      "totalFileSize": 2097152,
      "submittedAt": "2024-11-18T14:00:00",
      "isLateSubmission": false,
      "notes": "Midterm exam materials"
    }
  ]
}
```

---

### Get Submission by ID

Retrieves detailed information about a specific document submission.

**Endpoint**: `GET /api/professor/submissions/{submissionId}`

**Authentication**: Required (ROLE_PROFESSOR)

**Authorization**: Professor must own the submission OR be in the same department

**Path Parameters**:
- `submissionId` (Long, required): The ID of the submission

**Example Request**:
```
GET /api/professor/submissions/10
```

**Response**:
```json
{
  "success": true,
  "message": "Submission retrieved successfully",
  "data": {
    "id": 10,
    "courseAssignmentId": 1,
    "documentType": "SYLLABUS",
    "fileCount": 1,
    "totalFileSize": 524288,
    "submittedAt": "2024-11-15T10:30:00",
    "isLateSubmission": false,
    "notes": "Initial submission",
    "uploadedFiles": [
      {
        "id": 100,
        "originalFilename": "syllabus.pdf",
        "fileSize": 524288,
        "fileType": "application/pdf",
        "fileUrl": "/uploads/2024-2025/FIRST/5/CS101/SYLLABUS/syllabus.pdf",
        "uploadedAt": "2024-11-15T10:30:00"
      }
    ]
  }
}
```

---

## File Explorer Endpoints

### Get File Explorer Root

Retrieves the root node of the file explorer hierarchy for a specific academic year and semester.

**Endpoint**: `GET /api/professor/file-explorer/root`

**Authentication**: Required (ROLE_PROFESSOR)

**Query Parameters**:
- `academicYearId` (Long, required): The ID of the academic year
- `semesterId` (Long, required): The ID of the semester

**Permission Model**:
- Professors can see their own folders with write access (`canWrite: true`)
- Professors can see other professors' folders in the same department (read-only)
- Professors cannot see folders from other departments

**Folder Hierarchy**:
```
Root
└── Academic Year (2024-2025)
    └── Semester (FIRST)
        └── Professor (John Doe)
            └── Course (CS101)
                └── Document Type (SYLLABUS)
                    └── Files
```

**Example Request**:
```
GET /api/professor/file-explorer/root?academicYearId=1&semesterId=1
```

**Response**:
```json
{
  "success": true,
  "message": "File explorer root retrieved successfully",
  "data": {
    "path": "/2024-2025/FIRST",
    "name": "FIRST Semester",
    "type": "SEMESTER",
    "entityId": 1,
    "metadata": {},
    "canRead": true,
    "canWrite": false,
    "canDelete": false,
    "children": [
      {
        "path": "/2024-2025/FIRST/5",
        "name": "John Doe (You)",
        "type": "PROFESSOR",
        "entityId": 5,
        "canRead": true,
        "canWrite": true,
        "canDelete": false,
        "children": []
      },
      {
        "path": "/2024-2025/FIRST/7",
        "name": "Jane Smith",
        "type": "PROFESSOR",
        "entityId": 7,
        "canRead": true,
        "canWrite": false,
        "canDelete": false,
        "children": []
      }
    ]
  }
}
```

**Node Types**:
- `YEAR` - Academic year node
- `SEMESTER` - Semester node
- `PROFESSOR` - Professor folder node
- `COURSE` - Course folder node
- `DOCUMENT_TYPE` - Document type folder node
- `FILE` - File node

---

### Get File Explorer Node

Retrieves a specific node (folder or file) in the file explorer by its path.

**Endpoint**: `GET /api/professor/file-explorer/node`

**Authentication**: Required (ROLE_PROFESSOR)

**Query Parameters**:
- `path` (String, required): The path to the node (URL-encoded)

**Path Format**:
```
/{year}/{semester}/{professorId}/{courseCode}/{documentType}
```

**Example**: `/2024-2025/FIRST/5/CS101/SYLLABUS`

**Example Request**:
```
GET /api/professor/file-explorer/node?path=%2F2024-2025%2FFIRST%2F5%2FCS101
```

**Response**:
```json
{
  "success": true,
  "message": "File explorer node retrieved successfully",
  "data": {
    "path": "/2024-2025/FIRST/5/CS101",
    "name": "CS101 - Introduction to Computer Science",
    "type": "COURSE",
    "entityId": 1,
    "canRead": true,
    "canWrite": true,
    "canDelete": false,
    "children": [
      {
        "path": "/2024-2025/FIRST/5/CS101/SYLLABUS",
        "name": "SYLLABUS",
        "type": "DOCUMENT_TYPE",
        "canRead": true,
        "canWrite": true,
        "canDelete": false,
        "children": [
          {
            "path": "/2024-2025/FIRST/5/CS101/SYLLABUS/syllabus.pdf",
            "name": "syllabus.pdf",
            "type": "FILE",
            "entityId": 100,
            "metadata": {
              "fileSize": 524288,
              "fileType": "application/pdf",
              "uploadedAt": "2024-11-15T10:30:00"
            },
            "canRead": true,
            "canWrite": false,
            "canDelete": true,
            "children": []
          }
        ]
      }
    ]
  }
}
```

---

### Download File

Downloads a file from the system.

**Endpoint**: `GET /api/professor/files/{fileId}/download`

**Authentication**: Required (ROLE_PROFESSOR)

**Authorization**: Professor must be in the same department as the file owner

**Path Parameters**:
- `fileId` (Long, required): The ID of the file to download

**Example Request**:
```
GET /api/professor/files/100/download
```

**Success Response (200 OK)**:
```
HTTP/1.1 200 OK
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="syllabus.pdf"
Content-Length: 524288

[binary file data]
```

**Error Responses**:

Permission denied (403):
```json
{
  "success": false,
  "message": "You do not have permission to access this file",
  "data": null
}
```

File not found (404):
```json
{
  "success": false,
  "message": "File not found with ID: 100",
  "data": null
}
```

---

## Notification Endpoints

### Get My Notifications

Retrieves all notifications for the authenticated professor.

**Endpoint**: `GET /api/professor/notifications`

**Authentication**: Required (ROLE_PROFESSOR)

**Response**:
```json
{
  "success": true,
  "message": "Notifications retrieved successfully",
  "data": [
    {
      "id": 1,
      "message": "New document request for CS101 - SYLLABUS",
      "type": "DOCUMENT_REQUEST",
      "isSeen": false,
      "createdAt": "2024-11-19T10:00:00",
      "relatedEntityId": 1,
      "relatedEntityType": "DOCUMENT_REQUEST"
    },
    {
      "id": 2,
      "message": "Deadline approaching for CS102 - EXAM (Due: 2024-11-25)",
      "type": "DEADLINE_REMINDER",
      "isSeen": true,
      "createdAt": "2024-11-18T09:00:00",
      "relatedEntityId": 2,
      "relatedEntityType": "DOCUMENT_REQUEST"
    }
  ]
}
```

**Notification Types**:
- `DOCUMENT_REQUEST` - New document request
- `DEADLINE_REMINDER` - Deadline reminder
- `SYSTEM_ANNOUNCEMENT` - System announcement

---

### Mark Notification as Seen

Marks a specific notification as seen/read.

**Endpoint**: `PUT /api/professor/notifications/{notificationId}/seen`

**Authentication**: Required (ROLE_PROFESSOR)

**Path Parameters**:
- `notificationId` (Long, required): The ID of the notification

**Example Request**:
```
PUT /api/professor/notifications/1/seen
```

**Success Response (200 OK)**:
```json
{
  "success": true,
  "message": "Notification marked as read",
  "data": "OK"
}
```

---

## Legacy Endpoints (Deprecated)

The following endpoints are deprecated and maintained for backward compatibility only. New development should use the semester-based endpoints documented above.

### Legacy Document Request Endpoints

- `GET /api/professor/document-requests` - Get paginated document requests
- `GET /api/professor/document-requests/all` - Get all document requests
- `GET /api/professor/document-requests/{requestId}` - Get specific request
- `GET /api/professor/document-requests/pending-count` - Get pending count
- `POST /api/professor/document-requests/{requestId}/upload` - Upload single file
- `POST /api/professor/document-requests/{requestId}/submit` - Submit document
- `PUT /api/professor/document-requests/{requestId}/replace` - Replace document

### Legacy Multi-File Upload Endpoints

- `POST /api/professor/document-requests/{requestId}/upload-multiple` - Upload multiple files
- `POST /api/professor/document-requests/{requestId}/add-files` - Add files to submission
- `GET /api/professor/document-requests/{requestId}/file-attachments` - Get file attachments
- `DELETE /api/professor/file-attachments/{attachmentId}` - Delete file attachment
- `GET /api/professor/file-attachments/{attachmentId}/download` - Download file attachment
- `PUT /api/professor/submitted-documents/{submittedDocumentId}/reorder-files` - Reorder files

### Legacy Submitted Document Endpoints

- `GET /api/professor/document-requests/{requestId}/submitted-document` - Get submitted document
- `GET /api/professor/submitted-documents` - Get all submitted documents
- `GET /api/professor/submitted-documents/{documentId}/download` - Download document
- `DELETE /api/professor/submitted-documents/{documentId}` - Delete submitted document

**Note**: These endpoints will be removed in a future version. Please migrate to the semester-based endpoints.

---

## Best Practices

### File Upload Best Practices

1. **Validate files on client-side** before uploading to provide immediate feedback
2. **Show progress indicators** during upload for better UX
3. **Handle large files** by implementing chunked uploads if needed
4. **Retry failed uploads** with exponential backoff
5. **Sanitize filenames** to prevent security issues

### Error Handling Best Practices

1. **Display user-friendly error messages** from the API response
2. **Log errors to console** for debugging purposes
3. **Implement retry logic** for network errors
4. **Redirect to login** on 401 errors
5. **Show validation errors inline** in forms

### Performance Best Practices

1. **Cache academic year and semester data** (rarely changes)
2. **Implement pagination** for large lists
3. **Use lazy loading** for file explorer navigation
4. **Debounce search inputs** to reduce API calls
5. **Compress API responses** with gzip

### Security Best Practices

1. **Never expose sensitive data** in client-side code
2. **Validate all inputs** on both client and server
3. **Use HTTPS** for all API calls
4. **Implement CSRF protection** for state-changing operations
5. **Set secure session cookies** (HttpOnly, Secure flags)

---

## Support

For questions or issues with the API, please contact:
- **Email**: support@archivesystem.edu
- **Documentation**: https://docs.archivesystem.edu
- **Issue Tracker**: https://github.com/archivesystem/issues

---

**Last Updated**: November 19, 2024  
**API Version**: 2.0  
**Document Version**: 1.0
