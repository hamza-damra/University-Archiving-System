# Professor Dashboard Developer Guide

## Overview

This guide provides comprehensive technical documentation for developers working on the Professor Dashboard feature of the Document Archiving System. It covers architecture, implementation details, permission rules, file path structure, and best practices.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Backend Components](#backend-components)
3. [Frontend Components](#frontend-components)
4. [Permission Model](#permission-model)
5. [File Path Structure](#file-path-structure)
6. [Database Schema](#database-schema)
7. [API Integration](#api-integration)
8. [Security Considerations](#security-considerations)
9. [Testing Strategy](#testing-strategy)
10. [Deployment Guide](#deployment-guide)
11. [Troubleshooting](#troubleshooting)
12. [Code Examples](#code-examples)

---

## Architecture Overview

### System Architecture

The Professor Dashboard follows a three-tier architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                  Presentation Layer (Frontend)               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Dashboard   │  │  My Courses  │  │     File     │      │
│  │     Tab      │  │     Tab      │  │  Explorer    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────────┬─────────────────────────────────┘
                             │ REST API (JSON)
┌────────────────────────────┼─────────────────────────────────┐
│                  Business Logic Layer (Backend)              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Professor   │  │     File     │  │FileExplorer  │      │
│  │   Service    │  │   Service    │  │   Service    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────────┼─────────────────────────────────┘
                             │ JPA/Hibernate
┌────────────────────────────┼─────────────────────────────────┐
│                  Data Access Layer (Database)                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │CourseAssign  │  │DocumentSub   │  │UploadedFile  │      │
│  │    ments     │  │  missions    │  │      s       │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└──────────────────────────────────────────────────────────────┘
```

### Technology Stack

**Backend**:
- Spring Boot 3.x
- Spring Security (Session-based authentication)
- Spring Data JPA / Hibernate
- PostgreSQL database
- Java 17+

**Frontend**:
- HTML5, CSS3 (Tailwind CSS)
- Vanilla JavaScript (ES6+)
- Fetch API for HTTP requests
- No build tools (static files)

**File Storage**:
- Local filesystem
- Database metadata tracking



---

## Backend Components

### Controller Layer

#### ProfessorController

**Location**: `src/main/java/com/alqude/edu/ArchiveSystem/controller/ProfessorController.java`

**Purpose**: REST API endpoints for professor operations

**Key Annotations**:
```java
@RestController
@RequestMapping("/api/professor")
@PreAuthorize("hasRole('PROFESSOR')")
```

**Endpoint Categories**:
1. Academic Year & Semester endpoints
2. Dashboard endpoints
3. File upload & submission endpoints
4. File explorer endpoints
5. Notification endpoints
6. Legacy endpoints (deprecated)

**Example Endpoint**:
```java
@GetMapping("/dashboard/courses")
public ResponseEntity<ApiResponse<List<CourseAssignmentWithStatus>>> getMyCourses(
        @RequestParam Long semesterId) {
    var currentUser = authService.getCurrentUser();
    List<CourseAssignmentWithStatus> courses = 
            professorService.getProfessorCoursesWithStatus(currentUser.getId(), semesterId);
    return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", courses));
}
```

**Security Notes**:
- All endpoints require `ROLE_PROFESSOR`
- Some endpoints have additional method-level security (e.g., `@securityExpressionService.ownsSubmission`)
- Professor ID is extracted from authenticated user session

### Service Layer

#### ProfessorService

**Location**: `src/main/java/com/alqude/edu/ArchiveSystem/service/ProfessorServiceImpl.java`

**Purpose**: Business logic for professor operations

**Key Methods**:

```java
// Get courses with submission status
List<CourseAssignmentWithStatus> getProfessorCoursesWithStatus(
    Long professorId, Long semesterId);

// Get dashboard overview statistics
ProfessorDashboardOverview getProfessorDashboardOverview(
    Long professorId, Long semesterId);
```

**Implementation Notes**:
- Uses JOIN FETCH to avoid N+1 query problems
- Calculates document status based on deadline and submission timestamp
- Filters by professor ID and semester ID
- Returns DTOs to prevent lazy loading issues

**Status Calculation Logic**:
```java
private SubmissionStatus calculateStatus(
        DocumentSubmission submission, 
        LocalDateTime deadline) {
    if (submission != null && submission.getSubmittedAt() != null) {
        return SubmissionStatus.UPLOADED;
    }
    if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
        return SubmissionStatus.OVERDUE;
    }
    return SubmissionStatus.NOT_UPLOADED;
}
```

#### FileService

**Location**: `src/main/java/com/alqude/edu/ArchiveSystem/service/FileService.java`

**Purpose**: File upload, validation, and storage operations

**Key Methods**:

```java
// Upload files for a course assignment
List<UploadedFile> uploadFiles(
    Long courseAssignmentId,
    DocumentTypeEnum documentType,
    List<MultipartFile> files,
    String notes,
    Long professorId);

// Replace files for existing submission
List<UploadedFile> replaceFiles(
    Long submissionId,
    List<MultipartFile> files,
    String notes);

// Validate file type and size
boolean validateFileType(MultipartFile file, List<String> allowedExtensions);
boolean validateFileSize(List<MultipartFile> files, Integer maxTotalSizeMb);
```

**File Validation Rules**:
- Allowed extensions: PDF, ZIP
- Maximum file count: Defined per document type
- Maximum total size: Defined per document type
- Filename sanitization to prevent path traversal

**File Storage Process**:
1. Validate files (type, count, size)
2. Generate file path: `{uploadDir}/{year}/{semester}/{professorId}/{courseCode}/{docType}/{filename}`
3. Create directories if they don't exist
4. Store files to filesystem
5. Create/update DocumentSubmission record
6. Create UploadedFile records for each file
7. Handle transaction rollback on failure

#### FileExplorerService

**Location**: `src/main/java/com/alqude/edu/ArchiveSystem/service/FileExplorerService.java`

**Purpose**: Hierarchical file navigation with permission checks

**Key Methods**:

```java
// Get root node for academic year and semester
FileExplorerNode getRootNode(
    Long academicYearId,
    Long semesterId,
    User currentUser);

// Get specific node by path
FileExplorerNode getNode(String nodePath, User currentUser);

// Check permissions
boolean canRead(String nodePath, User user);
boolean canWrite(String nodePath, User user);
```

**Node Types**:
- `YEAR` - Academic year node
- `SEMESTER` - Semester node
- `PROFESSOR` - Professor folder node
- `COURSE` - Course folder node
- `DOCUMENT_TYPE` - Document type folder node
- `FILE` - File node

**Permission Logic**:
```java
// Professors can write to their own folders
boolean canWrite = nodePath.contains("/" + currentUser.getId() + "/");

// Professors can read department folders
boolean canRead = currentUser.getDepartment().getId().equals(departmentId);
```



### Repository Layer

#### CourseAssignmentRepository

**Location**: `src/main/java/com/alqude/edu/ArchiveSystem/repository/CourseAssignmentRepository.java`

**Purpose**: Data access for course assignments

**Key Queries**:

```java
@Query("SELECT ca FROM CourseAssignment ca " +
       "JOIN FETCH ca.course c " +
       "JOIN FETCH c.department d " +
       "JOIN FETCH ca.semester s " +
       "JOIN FETCH s.academicYear ay " +
       "WHERE ca.professor.id = :professorId " +
       "AND ca.semester.id = :semesterId " +
       "AND ca.isActive = true")
List<CourseAssignment> findByProfessorAndSemesterWithDetails(
    @Param("professorId") Long professorId,
    @Param("semesterId") Long semesterId);
```

**Performance Notes**:
- Uses JOIN FETCH to avoid N+1 queries
- Filters by professor ID and semester ID
- Only returns active assignments

#### DocumentSubmissionRepository

**Location**: `src/main/java/com/alqude/edu/ArchiveSystem/repository/DocumentSubmissionRepository.java`

**Purpose**: Data access for document submissions

**Key Queries**:

```java
@Query("SELECT ds FROM DocumentSubmission ds " +
       "WHERE ds.courseAssignment.id = :courseAssignmentId " +
       "AND ds.documentType = :documentType " +
       "ORDER BY ds.submittedAt DESC")
Optional<DocumentSubmission> findLatestByCourseAndType(
    @Param("courseAssignmentId") Long courseAssignmentId,
    @Param("documentType") DocumentTypeEnum documentType);
```

---

## Frontend Components

### HTML Structure

#### prof-dashboard.html

**Location**: `src/main/resources/static/prof-dashboard.html`

**Key Sections**:

```html
<!-- Header with user info and notifications -->
<header class="bg-white shadow">
  <div class="flex justify-between items-center">
    <h1>Professor Dashboard</h1>
    <div class="flex items-center gap-4">
      <button id="notificationBtn">
        <span id="notificationBadge" class="badge"></span>
      </button>
      <span id="userEmail"></span>
      <button id="logoutBtn">Logout</button>
    </div>
  </div>
</header>

<!-- Semester selector -->
<div class="semester-selector">
  <select id="academicYearSelect"></select>
  <select id="semesterSelect"></select>
</div>

<!-- Tab navigation -->
<div class="tabs">
  <button data-tab="dashboard">Dashboard</button>
  <button data-tab="courses">My Courses</button>
  <button data-tab="explorer">File Explorer</button>
</div>

<!-- Tab content areas -->
<div id="dashboardTab" class="tab-content"></div>
<div id="coursesTab" class="tab-content"></div>
<div id="explorerTab" class="tab-content"></div>

<!-- Modals container -->
<div id="modalsContainer"></div>

<!-- Toast container -->
<div id="toastContainer"></div>
```

### JavaScript Modules

#### prof.js

**Location**: `src/main/resources/static/js/prof.js`

**Purpose**: Main logic for professor dashboard

**Key Functions**:

```javascript
// Initialization
async function loadAcademicYears() {
    const response = await api.professor.getAcademicYears();
    // Populate dropdown
}

async function loadSemesters(academicYearId) {
    const response = await api.professor.getSemesters(academicYearId);
    // Populate dropdown
}

async function loadCourses(semesterId) {
    const response = await api.professor.getMyCourses(semesterId);
    renderCourses(response.data);
}

// UI Rendering
function renderCourses(courses) {
    const container = document.getElementById('coursesContainer');
    container.innerHTML = courses.map(course => createCourseCard(course)).join('');
}

function createCourseCard(course) {
    return `
        <div class="course-card">
            <h3>${course.courseCode} - ${course.courseName}</h3>
            <div class="document-types">
                ${Object.entries(course.documentStatuses)
                    .map(([type, status]) => createDocumentTypeRow(course, type, status))
                    .join('')}
            </div>
        </div>
    `;
}

// File Upload
async function openUploadModal(courseAssignmentId, documentType, submissionId, isReplacement, statusData) {
    // Create modal HTML
    // Attach event listeners
    // Handle file selection and validation
}

async function handleFileUpload(courseAssignmentId, documentType, files, notes, isReplacement, submissionId) {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    
    if (isReplacement) {
        await api.professor.replaceFiles(submissionId, formData, onProgress);
    } else {
        await api.professor.uploadFiles(courseAssignmentId, documentType, formData, onProgress);
    }
    
    // Refresh courses
    await loadCourses(currentSemesterId);
}

// File Explorer
async function loadFileExplorer(path = null) {
    let response;
    if (path) {
        response = await api.professor.getFileExplorerNode(path);
    } else {
        response = await api.professor.getFileExplorerRoot(currentAcademicYearId, currentSemesterId);
    }
    renderFileExplorer(response.data);
}

// Notifications
async function loadNotifications() {
    const response = await api.professor.getNotifications();
    renderNotifications(response.data);
    updateNotificationBadge(response.data);
}

// Polling for notifications every 30 seconds
setInterval(loadNotifications, 30000);
```

#### api.js

**Location**: `src/main/resources/static/js/api.js`

**Purpose**: Centralized API service

**Professor API Module**:

```javascript
export const professor = {
    // Academic structure
    getAcademicYears: () => GET('/api/professor/academic-years'),
    getSemesters: (yearId) => GET(`/api/professor/academic-years/${yearId}/semesters`),
    
    // Dashboard
    getMyCourses: (semesterId) => GET(`/api/professor/dashboard/courses?semesterId=${semesterId}`),
    getDashboardOverview: (semesterId) => GET(`/api/professor/dashboard/overview?semesterId=${semesterId}`),
    
    // File operations
    uploadFiles: (courseAssignmentId, documentType, formData, onProgress) => 
        POST(`/api/professor/submissions/upload?courseAssignmentId=${courseAssignmentId}&documentType=${documentType}`, formData, onProgress),
    replaceFiles: (submissionId, formData, onProgress) => 
        PUT(`/api/professor/submissions/${submissionId}/replace`, formData, onProgress),
    
    // File explorer
    getFileExplorerRoot: (academicYearId, semesterId) => 
        GET(`/api/professor/file-explorer/root?academicYearId=${academicYearId}&semesterId=${semesterId}`),
    getFileExplorerNode: (path) => GET(`/api/professor/file-explorer/node?path=${encodeURIComponent(path)}`),
    
    // File download
    downloadFile: (fileId) => GET(`/api/professor/files/${fileId}/download`, { responseType: 'blob' }),
    
    // Notifications
    getNotifications: () => GET('/api/professor/notifications'),
    markNotificationSeen: (notificationId) => PUT(`/api/professor/notifications/${notificationId}/seen`)
};
```

**HTTP Helper Functions**:

```javascript
async function GET(url, options = {}) {
    return apiRequest(url, { method: 'GET', ...options });
}

async function POST(url, body, onProgress) {
    return apiRequest(url, { method: 'POST', body }, onProgress);
}

async function PUT(url, body, onProgress) {
    return apiRequest(url, { method: 'PUT', body }, onProgress);
}

async function apiRequest(url, options, onProgress) {
    const response = await fetch(API_BASE_URL + url, {
        ...options,
        credentials: 'include', // Include session cookie
        headers: {
            ...options.headers,
            // Don't set Content-Type for FormData (browser sets it with boundary)
        }
    });
    
    if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    return response.json();
}
```



---

## Permission Model

### Overview

The Professor Dashboard implements a department-scoped permission model that controls access to files and folders.

### Permission Rules

#### Professor Permissions

**Own Folders**:
- ✅ **Read**: Can view own files
- ✅ **Write**: Can upload and replace files
- ✅ **Delete**: Can delete own files
- Path pattern: `/{year}/{semester}/{professorId}/{courseCode}/{docType}`

**Department Colleagues' Folders**:
- ✅ **Read**: Can view files from same department
- ❌ **Write**: Cannot upload or modify
- ❌ **Delete**: Cannot delete
- Path pattern: `/{year}/{semester}/{otherProfessorId}/{courseCode}/{docType}`

**Other Departments' Folders**:
- ❌ **Not Visible**: Cannot see folders from other departments

### Implementation

#### Backend Permission Checks

**FileExplorerService**:

```java
public boolean canRead(String nodePath, User user) {
    // Extract professor ID from path
    Long pathProfessorId = extractProfessorIdFromPath(nodePath);
    
    if (pathProfessorId == null) {
        return true; // Root nodes are readable
    }
    
    // Get professor from path
    User pathProfessor = userRepository.findById(pathProfessorId)
            .orElseThrow(() -> new IllegalArgumentException("Professor not found"));
    
    // Check if same department
    return user.getDepartment().getId().equals(pathProfessor.getDepartment().getId());
}

public boolean canWrite(String nodePath, User user) {
    // Extract professor ID from path
    Long pathProfessorId = extractProfessorIdFromPath(nodePath);
    
    // Can only write to own folders
    return pathProfessorId != null && pathProfessorId.equals(user.getId());
}
```

**FileService**:

```java
public boolean canUserReadFile(Long fileId, User user) {
    UploadedFile file = uploadedFileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    
    DocumentSubmission submission = file.getDocumentSubmission();
    User fileOwner = submission.getProfessor();
    
    // Can read if same department
    return user.getDepartment().getId().equals(fileOwner.getDepartment().getId());
}
```

#### Frontend Permission Indicators

**File Explorer UI**:

```javascript
function renderFileExplorerNode(node) {
    const isOwnFolder = node.canWrite;
    const icon = isOwnFolder ? '📝' : '🔒';
    const label = isOwnFolder ? '(You)' : '(Read-only)';
    
    return `
        <div class="file-node">
            <span class="icon">${icon}</span>
            <span class="name">${node.name} ${label}</span>
            ${node.canWrite ? '<button class="upload-btn">Upload</button>' : ''}
            ${node.canRead ? '<button class="download-btn">Download</button>' : ''}
        </div>
    `;
}
```

### Security Considerations

1. **Always validate permissions on the backend** - Never trust frontend checks
2. **Use method-level security** - `@PreAuthorize` annotations
3. **Check ownership** - Verify professor owns submission before allowing modifications
4. **Department filtering** - Always filter queries by department
5. **Path validation** - Sanitize and validate file paths to prevent traversal attacks

---

## File Path Structure

### Overview

Files are organized in a hierarchical structure that mirrors the academic organization.

### Path Pattern

```
{uploadDir}/{academicYear}/{semester}/{professorId}/{courseCode}/{documentType}/{filename}
```

### Example Paths

```
/uploads/2024-2025/FIRST/5/CS101/SYLLABUS/syllabus_v1.pdf
/uploads/2024-2025/FIRST/5/CS101/EXAM/midterm_exam.pdf
/uploads/2024-2025/FIRST/5/CS102/ASSIGNMENT/assignment1.zip
/uploads/2024-2025/SECOND/7/MATH201/LECTURE_NOTES/chapter1.pdf
```

### Path Components

| Component | Description | Example |
|-----------|-------------|---------|
| uploadDir | Base upload directory | `/uploads` or `/var/app/uploads` |
| academicYear | Academic year code | `2024-2025` |
| semester | Semester type | `FIRST`, `SECOND`, `SUMMER` |
| professorId | Professor's user ID | `5` |
| courseCode | Course code | `CS101` |
| documentType | Document type enum | `SYLLABUS`, `EXAM`, `ASSIGNMENT` |
| filename | Sanitized filename | `syllabus_v1.pdf` |

### Path Generation

**Backend (FileService)**:

```java
private String generateFilePath(
        CourseAssignment courseAssignment,
        DocumentTypeEnum documentType,
        String originalFilename) {
    
    Semester semester = courseAssignment.getSemester();
    AcademicYear academicYear = semester.getAcademicYear();
    Course course = courseAssignment.getCourse();
    User professor = courseAssignment.getProfessor();
    
    // Sanitize filename
    String sanitizedFilename = sanitizeFilename(originalFilename);
    
    // Build path
    return String.format("%s/%s/%s/%d/%s/%s/%s",
            uploadDir,
            academicYear.getYearCode(),
            semester.getType().name(),
            professor.getId(),
            course.getCourseCode(),
            documentType.name(),
            sanitizedFilename);
}

private String sanitizeFilename(String filename) {
    // Remove path traversal attempts
    filename = filename.replaceAll("\\.\\./", "");
    filename = filename.replaceAll("/", "_");
    filename = filename.replaceAll("\\\\", "_");
    
    // Remove special characters
    filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    
    return filename;
}
```

### Directory Creation

**Automatic Directory Creation**:

```java
private void ensureDirectoryExists(String filePath) {
    Path path = Paths.get(filePath).getParent();
    try {
        Files.createDirectories(path);
    } catch (IOException e) {
        throw new RuntimeException("Failed to create directory: " + path, e);
    }
}
```

### Path Consistency

**Important**: The file path structure must be consistent between:
1. Professor file uploads
2. Deanship file explorer
3. HOD file access
4. File download URLs

**Verification**:
- Test file uploads from professor dashboard
- Verify files appear in deanship file explorer
- Confirm paths match expected pattern
- Check file downloads work correctly



---

## Database Schema

### Entity Relationships

```
AcademicYear (1) ──< (N) Semester
Semester (1) ──< (N) CourseAssignment
User/Professor (1) ──< (N) CourseAssignment
Course (1) ──< (N) CourseAssignment
CourseAssignment (1) ──< (N) DocumentSubmission
DocumentSubmission (1) ──< (N) UploadedFile
Course (1) ──< (N) RequiredDocumentType
Department (1) ──< (N) User
Department (1) ──< (N) Course
```

### Key Entities

#### CourseAssignment

```java
@Entity
@Table(name = "course_assignments")
public class CourseAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @OneToMany(mappedBy = "courseAssignment", cascade = CascadeType.ALL)
    private List<DocumentSubmission> submissions = new ArrayList<>();
}
```

#### DocumentSubmission

```java
@Entity
@Table(name = "document_submissions")
public class DocumentSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_assignment_id", nullable = false)
    private CourseAssignment courseAssignment;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentTypeEnum documentType;
    
    @Column(name = "file_count")
    private Integer fileCount;
    
    @Column(name = "total_file_size")
    private Long totalFileSize;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "is_late_submission")
    private Boolean isLateSubmission = false;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @OneToMany(mappedBy = "documentSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UploadedFile> uploadedFiles = new ArrayList<>();
}
```

#### UploadedFile

```java
@Entity
@Table(name = "uploaded_files")
public class UploadedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_submission_id", nullable = false)
    private DocumentSubmission documentSubmission;
    
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    
    @Column(name = "file_url", nullable = false)
    private String fileUrl;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_type")
    private String fileType;
    
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}
```

#### RequiredDocumentType

```java
@Entity
@Table(name = "required_document_types")
public class RequiredDocumentType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester; // Optional: semester-specific requirements
    
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentTypeEnum documentType;
    
    @Column(name = "deadline")
    private LocalDateTime deadline;
    
    @Column(name = "is_required")
    private Boolean isRequired = true;
    
    @Column(name = "max_file_count")
    private Integer maxFileCount = 1;
    
    @Column(name = "max_total_size_mb")
    private Integer maxTotalSizeMb = 10;
    
    @ElementCollection
    @CollectionTable(name = "allowed_file_extensions")
    private List<String> allowedFileExtensions = Arrays.asList("pdf", "zip");
}
```

### Database Indexes

**Recommended Indexes**:

```sql
-- Course assignments
CREATE INDEX idx_course_assignments_professor ON course_assignments(professor_id);
CREATE INDEX idx_course_assignments_semester ON course_assignments(semester_id);
CREATE INDEX idx_course_assignments_course ON course_assignments(course_id);
CREATE INDEX idx_course_assignments_active ON course_assignments(is_active);

-- Document submissions
CREATE INDEX idx_document_submissions_course_assignment ON document_submissions(course_assignment_id);
CREATE INDEX idx_document_submissions_professor ON document_submissions(professor_id);
CREATE INDEX idx_document_submissions_type ON document_submissions(document_type);
CREATE INDEX idx_document_submissions_submitted_at ON document_submissions(submitted_at);

-- Uploaded files
CREATE INDEX idx_uploaded_files_submission ON uploaded_files(document_submission_id);
CREATE INDEX idx_uploaded_files_uploaded_at ON uploaded_files(uploaded_at);

-- Required document types
CREATE INDEX idx_required_doc_types_course ON required_document_types(course_id);
CREATE INDEX idx_required_doc_types_semester ON required_document_types(semester_id);
CREATE INDEX idx_required_doc_types_type ON required_document_types(document_type);
```

---

## API Integration

### Request/Response Flow

#### Example: Upload Files

**1. Frontend Request**:

```javascript
const formData = new FormData();
files.forEach(file => formData.append('files', file));

const response = await fetch(
    '/api/professor/submissions/upload?courseAssignmentId=1&documentType=SYLLABUS&notes=Initial submission',
    {
        method: 'POST',
        body: formData,
        credentials: 'include'
    }
);

const result = await response.json();
```

**2. Backend Processing**:

```java
@PostMapping("/submissions/upload")
public ResponseEntity<ApiResponse<DocumentSubmission>> uploadFiles(
        @RequestParam Long courseAssignmentId,
        @RequestParam DocumentTypeEnum documentType,
        @RequestParam(required = false) String notes,
        @RequestPart("files") List<MultipartFile> files) {
    
    var currentUser = authService.getCurrentUser();
    
    // Validate and upload files
    fileService.uploadFiles(courseAssignmentId, documentType, files, notes, currentUser.getId());
    
    // Get created submission
    DocumentSubmission submission = submissionService.getLatestSubmission(courseAssignmentId, documentType);
    
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Files uploaded successfully", submission));
}
```

**3. FileService Processing**:

```java
@Transactional
public List<UploadedFile> uploadFiles(
        Long courseAssignmentId,
        DocumentTypeEnum documentType,
        List<MultipartFile> files,
        String notes,
        Long professorId) {
    
    // 1. Get course assignment
    CourseAssignment courseAssignment = courseAssignmentRepository.findById(courseAssignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Course assignment not found"));
    
    // 2. Validate professor owns assignment
    if (!courseAssignment.getProfessor().getId().equals(professorId)) {
        throw new IllegalArgumentException("Professor not assigned to this course");
    }
    
    // 3. Get required document type for validation
    RequiredDocumentType requiredDocType = requiredDocTypeRepository
            .findByCourseAndType(courseAssignment.getCourse().getId(), documentType)
            .orElseThrow(() -> new IllegalArgumentException("Document type not required for this course"));
    
    // 4. Validate files
    validateFiles(files, requiredDocType);
    
    // 5. Create or update submission
    DocumentSubmission submission = getOrCreateSubmission(courseAssignment, documentType, professorId);
    submission.setNotes(notes);
    submission.setSubmittedAt(LocalDateTime.now());
    submission.setFileCount(files.size());
    submission.setTotalFileSize(files.stream().mapToLong(MultipartFile::getSize).sum());
    
    // Check if late
    if (requiredDocType.getDeadline() != null && 
        LocalDateTime.now().isAfter(requiredDocType.getDeadline())) {
        submission.setIsLateSubmission(true);
    }
    
    submission = documentSubmissionRepository.save(submission);
    
    // 6. Store files
    List<UploadedFile> uploadedFiles = new ArrayList<>();
    for (MultipartFile file : files) {
        String filePath = generateFilePath(courseAssignment, documentType, file.getOriginalFilename());
        storeFile(file, filePath);
        
        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setDocumentSubmission(submission);
        uploadedFile.setOriginalFilename(file.getOriginalFilename());
        uploadedFile.setFileUrl(filePath);
        uploadedFile.setFileSize(file.getSize());
        uploadedFile.setFileType(file.getContentType());
        uploadedFile.setUploadedAt(LocalDateTime.now());
        
        uploadedFiles.add(uploadedFileRepository.save(uploadedFile));
    }
    
    return uploadedFiles;
}
```

**4. Frontend Response Handling**:

```javascript
if (result.success) {
    showToast('Files uploaded successfully', 'success');
    closeModal();
    await loadCourses(currentSemesterId); // Refresh courses
} else {
    showToast(result.message, 'error');
}
```

### Error Handling

**Backend Exception Handling**:

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage()));
}

@ExceptionHandler(UnauthorizedOperationException.class)
public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedOperationException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getMessage()));
}

@ExceptionHandler(Exception.class)
public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
    log.error("Unexpected error", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An unexpected error occurred"));
}
```

**Frontend Error Handling**:

```javascript
async function apiRequest(url, options) {
    try {
        const response = await fetch(API_BASE_URL + url, {
            ...options,
            credentials: 'include'
        });
        
        if (response.status === 401) {
            // Session expired
            window.location.href = '/index.html';
            return;
        }
        
        const result = await response.json();
        
        if (!result.success) {
            throw new Error(result.message);
        }
        
        return result;
    } catch (error) {
        console.error('API Error:', error);
        showToast(error.message || 'Network error', 'error');
        throw error;
    }
}
```



---

## Security Considerations

### Authentication

**Session-Based Authentication**:
- Spring Security manages sessions
- Session cookies are HttpOnly and Secure
- Session timeout: 30 minutes of inactivity
- CSRF protection enabled

**Configuration**:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/professor/**").hasRole("PROFESSOR")
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            );
        
        return http.build();
    }
}
```

### Authorization

**Role-Based Access Control**:

```java
// Controller-level
@PreAuthorize("hasRole('PROFESSOR')")
public class ProfessorController { }

// Method-level
@PreAuthorize("hasRole('PROFESSOR') and @securityExpressionService.ownsSubmission(#submissionId)")
public ResponseEntity<ApiResponse<DocumentSubmission>> replaceFiles(@PathVariable Long submissionId) { }
```

**Custom Security Expressions**:

```java
@Service("securityExpressionService")
public class SecurityExpressionService {
    
    public boolean ownsSubmission(Long submissionId) {
        User currentUser = authService.getCurrentUser();
        DocumentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found"));
        
        return submission.getProfessor().getId().equals(currentUser.getId());
    }
    
    public boolean canAccessFile(Long fileId) {
        User currentUser = authService.getCurrentUser();
        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        
        User fileOwner = file.getDocumentSubmission().getProfessor();
        
        // Can access if same department
        return currentUser.getDepartment().getId().equals(fileOwner.getDepartment().getId());
    }
}
```

### Input Validation

**File Upload Validation**:

```java
private void validateFiles(List<MultipartFile> files, RequiredDocumentType requiredDocType) {
    // Check file count
    if (files.size() > requiredDocType.getMaxFileCount()) {
        throw new IllegalArgumentException(
            String.format("Maximum %d file(s) allowed. You uploaded %d files.",
                requiredDocType.getMaxFileCount(), files.size()));
    }
    
    // Check total size
    long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
    long maxSizeBytes = requiredDocType.getMaxTotalSizeMb() * 1024 * 1024;
    if (totalSize > maxSizeBytes) {
        throw new IllegalArgumentException(
            String.format("Total file size (%d bytes) exceeds maximum allowed (%d bytes).",
                totalSize, maxSizeBytes));
    }
    
    // Check file types
    List<String> allowedExtensions = requiredDocType.getAllowedFileExtensions();
    for (MultipartFile file : files) {
        String filename = file.getOriginalFilename();
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException(
                String.format("Invalid file type '%s'. Allowed types: %s",
                    extension, String.join(", ", allowedExtensions)));
        }
    }
}
```

**Path Traversal Prevention**:

```java
private String sanitizeFilename(String filename) {
    // Remove path traversal attempts
    filename = filename.replaceAll("\\.\\./", "");
    filename = filename.replaceAll("/", "_");
    filename = filename.replaceAll("\\\\", "_");
    
    // Remove special characters
    filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    
    // Limit length
    if (filename.length() > 255) {
        filename = filename.substring(0, 255);
    }
    
    return filename;
}
```

### SQL Injection Prevention

**Use Parameterized Queries**:

```java
// Good - Parameterized query
@Query("SELECT ca FROM CourseAssignment ca WHERE ca.professor.id = :professorId")
List<CourseAssignment> findByProfessor(@Param("professorId") Long professorId);

// Bad - String concatenation (DON'T DO THIS)
// String query = "SELECT * FROM course_assignments WHERE professor_id = " + professorId;
```

### XSS Prevention

**Frontend**:
- Use `textContent` instead of `innerHTML` when displaying user input
- Sanitize HTML if innerHTML is necessary
- Escape special characters

```javascript
// Good
element.textContent = userInput;

// Bad
// element.innerHTML = userInput;
```

**Backend**:
- Spring Boot automatically escapes JSON responses
- Use `@JsonProperty` annotations for proper serialization

### CSRF Protection

**Backend**:
- CSRF tokens in cookies
- Validated on state-changing operations (POST, PUT, DELETE)

**Frontend**:
- Include CSRF token in requests
- Fetch API automatically includes cookies

```javascript
fetch(url, {
    method: 'POST',
    credentials: 'include', // Includes session cookie and CSRF token
    body: formData
});
```

---

## Testing Strategy

### Unit Tests

**Service Layer Tests**:

```java
@ExtendWith(MockitoExtension.class)
class ProfessorServiceTest {
    
    @Mock
    private CourseAssignmentRepository courseAssignmentRepository;
    
    @Mock
    private DocumentSubmissionRepository documentSubmissionRepository;
    
    @InjectMocks
    private ProfessorServiceImpl professorService;
    
    @Test
    void testGetProfessorCoursesWithStatus_Success() {
        // Arrange
        Long professorId = 1L;
        Long semesterId = 1L;
        
        CourseAssignment assignment = createMockCourseAssignment();
        when(courseAssignmentRepository.findByProfessorAndSemester(professorId, semesterId))
                .thenReturn(List.of(assignment));
        
        // Act
        List<CourseAssignmentWithStatus> result = 
                professorService.getProfessorCoursesWithStatus(professorId, semesterId);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CS101", result.get(0).getCourseCode());
    }
    
    @Test
    void testCalculateStatus_Uploaded() {
        // Arrange
        DocumentSubmission submission = new DocumentSubmission();
        submission.setSubmittedAt(LocalDateTime.now());
        LocalDateTime deadline = LocalDateTime.now().plusDays(7);
        
        // Act
        SubmissionStatus status = professorService.calculateStatus(submission, deadline);
        
        // Assert
        assertEquals(SubmissionStatus.UPLOADED, status);
    }
    
    @Test
    void testCalculateStatus_Overdue() {
        // Arrange
        DocumentSubmission submission = null; // No submission
        LocalDateTime deadline = LocalDateTime.now().minusDays(1); // Past deadline
        
        // Act
        SubmissionStatus status = professorService.calculateStatus(submission, deadline);
        
        // Assert
        assertEquals(SubmissionStatus.OVERDUE, status);
    }
}
```

### Integration Tests

**Controller Tests**:

```java
@SpringBootTest
@AutoConfigureMockMvc
class ProfessorControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(roles = "PROFESSOR")
    void testGetMyCourses_Success() throws Exception {
        // Arrange
        Long semesterId = 1L;
        
        // Act & Assert
        mockMvc.perform(get("/api/professor/dashboard/courses")
                .param("semesterId", semesterId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    @WithMockUser(roles = "PROFESSOR")
    void testUploadFiles_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.pdf",
                "application/pdf",
                "test content".getBytes());
        
        // Act & Assert
        mockMvc.perform(multipart("/api/professor/submissions/upload")
                .file(file)
                .param("courseAssignmentId", "1")
                .param("documentType", "SYLLABUS")
                .param("notes", "Test upload"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    void testGetMyCourses_Unauthorized() throws Exception {
        // Act & Assert - No authentication
        mockMvc.perform(get("/api/professor/dashboard/courses")
                .param("semesterId", "1"))
                .andExpect(status().isUnauthorized());
    }
}
```

### Frontend Testing

**Manual Testing Checklist**:

```markdown
## Authentication
- [ ] Professor can log in successfully
- [ ] Non-professor users are denied access
- [ ] Session expires after 30 minutes
- [ ] Logout clears session

## Course Viewing
- [ ] Courses load for selected semester
- [ ] Empty state shows when no courses
- [ ] Course cards display correct information
- [ ] Document statuses are accurate

## File Upload
- [ ] Upload modal opens correctly
- [ ] File validation works (type, count, size)
- [ ] Progress bar updates during upload
- [ ] Success message shows on completion
- [ ] Course list refreshes after upload

## File Replacement
- [ ] Replace button shows for uploaded docs
- [ ] Old files are deleted on replacement
- [ ] New files are stored correctly
- [ ] Late submission flag set if past deadline

## File Explorer
- [ ] Root node loads correctly
- [ ] Navigation through folders works
- [ ] Own folders show write indicator
- [ ] Department folders show as read-only
- [ ] Download functionality works

## Notifications
- [ ] Notification badge shows unseen count
- [ ] Dropdown opens on click
- [ ] Mark as seen works
- [ ] Polling updates every 30 seconds
```

---

## Deployment Guide

### Prerequisites

1. **Java 17+** installed
2. **PostgreSQL** database running
3. **Maven 3.6+** installed
4. **File upload directory** with write permissions

### Configuration

**application.properties**:

```properties
# Server
server.port=8080
server.servlet.session.timeout=30m

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/archive_system
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# File Upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=100MB
app.upload.dir=/var/app/uploads

# Logging
logging.level.com.alqude.edu.ArchiveSystem=INFO
logging.file.name=logs/archive-system.log
```

### Build and Deploy

**1. Build the application**:

```bash
mvn clean package -DskipTests
```

**2. Create upload directory**:

```bash
mkdir -p /var/app/uploads
chmod 755 /var/app/uploads
```

**3. Run the application**:

```bash
java -jar target/archive-system-1.0.0.jar
```

**4. Verify deployment**:

```bash
curl http://localhost:8080/api/professor/academic-years
```

### Docker Deployment

**Dockerfile**:

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/archive-system-1.0.0.jar app.jar
RUN mkdir -p /var/app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml**:

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/archive_system
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=password
      - APP_UPLOAD_DIR=/var/app/uploads
    volumes:
      - ./uploads:/var/app/uploads
    depends_on:
      - db
  
  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=archive_system
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=password
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

**Deploy with Docker**:

```bash
docker-compose up -d
```

### Production Considerations

1. **Use environment variables** for sensitive configuration
2. **Enable HTTPS** with SSL certificates
3. **Set up reverse proxy** (Nginx, Apache)
4. **Configure backup** for database and uploaded files
5. **Set up monitoring** (Prometheus, Grafana)
6. **Configure logging** (ELK stack, Splunk)
7. **Implement rate limiting** to prevent abuse
8. **Set up CDN** for static assets

---

## Troubleshooting

### Common Issues

#### Issue: Files not uploading

**Symptoms**:
- Upload fails with 500 error
- "Failed to store file" message

**Possible Causes**:
1. Upload directory doesn't exist
2. No write permissions on upload directory
3. Disk space full

**Solutions**:
```bash
# Create directory
mkdir -p /var/app/uploads

# Set permissions
chmod 755 /var/app/uploads
chown app-user:app-group /var/app/uploads

# Check disk space
df -h
```

#### Issue: N+1 Query Problem

**Symptoms**:
- Slow API responses
- Many SQL queries in logs

**Solution**:
Use JOIN FETCH in queries:

```java
@Query("SELECT ca FROM CourseAssignment ca " +
       "JOIN FETCH ca.course c " +
       "JOIN FETCH ca.semester s " +
       "WHERE ca.professor.id = :professorId")
List<CourseAssignment> findByProfessorWithDetails(@Param("professorId") Long professorId);
```

#### Issue: Session expires too quickly

**Symptoms**:
- Users logged out frequently
- "Session expired" messages

**Solution**:
Increase session timeout in application.properties:

```properties
server.servlet.session.timeout=60m
```

#### Issue: CORS errors

**Symptoms**:
- "CORS policy" errors in browser console
- API calls fail from frontend

**Solution**:
Configure CORS in Spring Security:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

## Code Examples

### Complete File Upload Flow

**Frontend**:

```javascript
async function uploadFiles(courseAssignmentId, documentType, files, notes) {
    try {
        // Show loading state
        showLoadingSpinner();
        
        // Create FormData
        const formData = new FormData();
        files.forEach(file => formData.append('files', file));
        
        // Upload with progress tracking
        const response = await api.professor.uploadFiles(
            courseAssignmentId,
            documentType,
            formData,
            (progress) => updateProgressBar(progress)
        );
        
        // Handle success
        showToast('Files uploaded successfully', 'success');
        await loadCourses(currentSemesterId);
        closeModal();
        
    } catch (error) {
        // Handle error
        showToast(error.message, 'error');
    } finally {
        hideLoadingSpinner();
    }
}
```

**Backend**:

```java
@PostMapping("/submissions/upload")
@Transactional
public ResponseEntity<ApiResponse<DocumentSubmission>> uploadFiles(
        @RequestParam Long courseAssignmentId,
        @RequestParam DocumentTypeEnum documentType,
        @RequestParam(required = false) String notes,
        @RequestPart("files") List<MultipartFile> files) {
    
    var currentUser = authService.getCurrentUser();
    
    log.info("Professor {} uploading {} files for course assignment {}", 
            currentUser.getId(), files.size(), courseAssignmentId);
    
    try {
        // Upload files
        fileService.uploadFiles(courseAssignmentId, documentType, files, notes, currentUser.getId());
        
        // Get created submission
        DocumentSubmission submission = submissionService.getLatestSubmission(
                courseAssignmentId, documentType);
        
        log.info("Successfully uploaded {} files for submission {}", 
                files.size(), submission.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Files uploaded successfully", submission));
                
    } catch (IllegalArgumentException e) {
        log.error("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
        log.error("Upload failed", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to upload files"));
    }
}
```

---

## Additional Resources

- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **Spring Security Documentation**: https://spring.io/projects/spring-security
- **Tailwind CSS Documentation**: https://tailwindcss.com/docs
- **PostgreSQL Documentation**: https://www.postgresql.org/docs/

---

**Document Version**: 1.0  
**Last Updated**: November 19, 2024  
**Maintained By**: Archive System Development Team
