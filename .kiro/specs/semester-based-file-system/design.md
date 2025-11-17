# Design Document

## Overview

This design document outlines the refactoring of the Document Archiving System from a request-based model to a semester- and folder-based file exploration system. The refactoring introduces a hierarchical academic structure (Year → Semester → Professor → Course → Document Type → Files) and expands from two roles (HOD, Professor) to three roles (Deanship, HOD, Professor) with distinct permissions.

### Key Design Principles

1. **Hierarchical Organization**: All files organized in a consistent Year/Semester/Professor/Course/DocumentType structure
2. **Role-Based Access Control**: Three distinct roles with clearly defined permissions
3. **Academic Structure First**: Deanship manages the academic structure; HOD and Professor work within it
4. **Backward Compatibility**: Preserve existing data through migration; maintain Spring Boot + HTML/CSS/JS/Tailwind stack
5. **Separation of Concerns**: Clear boundaries between academic management, file storage, and user interfaces

### Technology Stack

- **Backend**: Spring Boot 3.x, Spring Security, JPA/Hibernate, PostgreSQL
- **Frontend**: HTML5, CSS3, JavaScript (ES6+), Tailwind CSS
- **File Storage**: Local filesystem with database metadata tracking
- **Authentication**: Session-based authentication with Spring Security

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Deanship   │  │     HOD      │  │  Professor   │      │
│  │  Dashboard   │  │  Dashboard   │  │  Dashboard   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│           │                │                 │               │
│           └────────────────┴─────────────────┘               │
│                            │                                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│                     Controller Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Deanship   │  │     HOD      │  │  Professor   │      │
│  │  Controller  │  │  Controller  │  │  Controller  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ FileExplorer │  │    Report    │  │     Auth     │      │
│  │  Controller  │  │  Controller  │  │  Controller  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│                      Service Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Academic    │  │    Course    │  │  Professor   │      │
│  │   Service    │  │   Service    │  │   Service    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │    File      │  │  Submission  │  │    Report    │      │
│  │   Service    │  │   Service    │  │   Service    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│                   Repository Layer (JPA)                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ AcademicYear │  │   Semester   │  │    Course    │      │
│  │  Repository  │  │  Repository  │  │  Repository  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │CourseAssign  │  │DocumentType  │  │    File      │      │
│  │  Repository  │  │  Repository  │  │  Repository  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│                      Data Layer                              │
│                    PostgreSQL Database                       │
└──────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

**Deanship Creates Academic Structure:**
1. Deanship user creates academic year via DeanshipController
2. AcademicService creates year and three semester records
3. Deanship assigns courses to professors for specific semesters
4. CourseService creates CourseAssignment records

**Professor Uploads Files:**
1. Professor selects semester and course in dashboard
2. Professor clicks upload for a document type
3. ProfessorController receives upload request
4. FileService validates files and stores to filesystem
5. SubmissionService creates/updates DocumentSubmission record
6. NotificationService notifies HOD of new submission

**HOD Views Submission Status:**
1. HOD selects semester in dashboard
2. HodController fetches submission status for department
3. ReportService aggregates data from CourseAssignment and DocumentSubmission
4. Dashboard displays professors, courses, and submission status

## Components and Interfaces

### 1. New Domain Entities

#### AcademicYear Entity
```java
@Entity
@Table(name = "academic_years")
public class AcademicYear {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String yearCode; // e.g., "2024-2025"
    
    @Column(nullable = false)
    private Integer startYear; // e.g., 2024
    
    @Column(nullable = false)
    private Integer endYear; // e.g., 2025
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL)
    private List<Semester> semesters;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

#### Semester Entity
```java
@Entity
@Table(name = "semesters")
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SemesterType type; // FIRST, SECOND, SUMMER
    
    @Column(nullable = false)
    private LocalDate startDate;
    
    @Column(nullable = false)
    private LocalDate endDate;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "semester", cascade = CascadeType.ALL)
    private List<CourseAssignment> courseAssignments;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

public enum SemesterType {
    FIRST,   // Fall
    SECOND,  // Spring
    SUMMER
}
```

#### Course Entity
```java
@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String courseCode; // e.g., "CS101"
    
    @Column(nullable = false)
    private String courseName; // e.g., "Database Systems"
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    
    @Column
    private String level; // e.g., "Undergraduate", "Graduate"
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<CourseAssignment> courseAssignments;
    
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<RequiredDocumentType> requiredDocumentTypes;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

#### CourseAssignment Entity
```java
@Entity
@Table(name = "course_assignments")
public class CourseAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "courseAssignment", cascade = CascadeType.ALL)
    private List<DocumentSubmission> documentSubmissions;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

#### RequiredDocumentType Entity
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
    private Semester semester; // Optional: specific to semester
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentTypeEnum documentType;
    
    @Column
    private LocalDateTime deadline;
    
    @Column(nullable = false)
    private Boolean isRequired = true;
    
    @Column
    private Integer maxFileCount = 5;
    
    @Column
    private Integer maxTotalSizeMb = 50;
    
    @ElementCollection
    @CollectionTable(name = "allowed_file_extensions")
    private List<String> allowedFileExtensions; // e.g., ["pdf", "zip"]
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

public enum DocumentTypeEnum {
    SYLLABUS,
    EXAM,
    ASSIGNMENT,
    PROJECT_DOCS,
    LECTURE_NOTES,
    OTHER
}
```

#### DocumentSubmission Entity (Replaces SubmittedDocument)
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
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentTypeEnum documentType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;
    
    @OneToMany(mappedBy = "documentSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UploadedFile> uploadedFiles = new ArrayList<>();
    
    @Column(nullable = false)
    private LocalDateTime submittedAt;
    
    @Column(nullable = false)
    private Boolean isLateSubmission = false;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status; // NOT_UPLOADED, UPLOADED, OVERDUE
    
    @Column(length = 1000)
    private String notes;
    
    @Column
    private Integer fileCount = 0;
    
    @Column
    private Long totalFileSize = 0L;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

public enum SubmissionStatus {
    NOT_UPLOADED,
    UPLOADED,
    OVERDUE
}
```

#### UploadedFile Entity (Replaces FileAttachment)
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
    
    @Column(nullable = false)
    private String fileUrl; // Physical path or URL
    
    @Column(nullable = false)
    private String originalFilename;
    
    @Column
    private Long fileSize;
    
    @Column
    private String fileType; // MIME type
    
    @Column
    private Integer fileOrder;
    
    @Column(length = 500)
    private String description;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### 2. Modified Entities

#### User Entity (Updated)
```java
@Entity
@Table(name = "users")
public class User implements UserDetails {
    // ... existing fields ...
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // Updated to include ROLE_DEANSHIP
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    
    // New field for professor-specific data
    @Column(unique = true)
    private String professorId; // e.g., "prof_12345"
    
    @OneToMany(mappedBy = "professor", cascade = CascadeType.ALL)
    private List<CourseAssignment> courseAssignments;
    
    @OneToMany(mappedBy = "professor", cascade = CascadeType.ALL)
    private List<DocumentSubmission> documentSubmissions;
    
    // ... existing fields ...
}
```

#### Role Enum (Updated)
```java
public enum Role {
    ROLE_DEANSHIP,
    ROLE_HOD,
    ROLE_PROFESSOR
}
```

### 3. Service Layer Interfaces

#### AcademicService
```java
public interface AcademicService {
    // Academic Year Management
    AcademicYear createAcademicYear(String yearCode, Integer startYear, Integer endYear);
    AcademicYear updateAcademicYear(Long id, AcademicYearDTO dto);
    List<AcademicYear> getAllAcademicYears();
    AcademicYear getActiveAcademicYear();
    void setActiveAcademicYear(Long id);
    
    // Semester Management
    Semester getSemester(Long semesterId);
    List<Semester> getSemestersByYear(Long academicYearId);
    Semester updateSemester(Long id, SemesterDTO dto);
}
```

#### CourseService
```java
public interface CourseService {
    // Course Management
    Course createCourse(CourseDTO dto);
    Course updateCourse(Long id, CourseDTO dto);
    Course getCourse(Long id);
    List<Course> getCoursesByDepartment(Long departmentId);
    void deactivateCourse(Long id);
    
    // Course Assignment Management
    CourseAssignment assignCourse(Long semesterId, Long courseId, Long professorId);
    void unassignCourse(Long assignmentId);
    List<CourseAssignment> getAssignmentsBySemester(Long semesterId);
    List<CourseAssignment> getAssignmentsByProfessor(Long professorId, Long semesterId);
    
    // Required Document Types
    RequiredDocumentType addRequiredDocumentType(Long courseId, RequiredDocumentTypeDTO dto);
    void updateRequiredDocumentType(Long id, RequiredDocumentTypeDTO dto);
    List<RequiredDocumentType> getRequiredDocumentTypes(Long courseId, Long semesterId);
}
```

#### ProfessorService
```java
public interface ProfessorService {
    // Professor Management (Deanship only)
    User createProfessor(ProfessorDTO dto);
    User updateProfessor(Long id, ProfessorDTO dto);
    User getProfessor(Long id);
    List<User> getProfessorsByDepartment(Long departmentId);
    void deactivateProfessor(Long id);
    void activateProfessor(Long id);
    
    // Professor Information
    String generateProfessorId(User professor);
    List<CourseAssignment> getProfessorCourses(Long professorId, Long semesterId);
}
```

#### FileService
```java
public interface FileService {
    // File Upload
    List<UploadedFile> uploadFiles(Long courseAssignmentId, DocumentTypeEnum documentType, 
                                    List<MultipartFile> files, String notes, Long professorId);
    
    // File Replacement
    List<UploadedFile> replaceFiles(Long submissionId, List<MultipartFile> files, String notes);
    
    // File Retrieval
    UploadedFile getFile(Long fileId);
    List<UploadedFile> getFilesBySubmission(Long submissionId);
    Resource loadFileAsResource(String fileUrl);
    
    // File Deletion
    void deleteFile(Long fileId);
    
    // File Validation
    boolean validateFileType(MultipartFile file, List<String> allowedExtensions);
    boolean validateFileSize(List<MultipartFile> files, Integer maxTotalSizeMb);
    
    // File Path Generation
    String generateFilePath(Long academicYearId, Long semesterId, String professorId, 
                           String courseCode, DocumentTypeEnum documentType, String filename);
}
```

#### SubmissionService
```java
public interface SubmissionService {
    // Submission Management
    DocumentSubmission createSubmission(Long courseAssignmentId, DocumentTypeEnum documentType, 
                                       Long professorId, String notes);
    DocumentSubmission updateSubmission(Long submissionId, String notes);
    DocumentSubmission getSubmission(Long submissionId);
    
    // Submission Status
    List<DocumentSubmission> getSubmissionsByProfessor(Long professorId, Long semesterId);
    List<DocumentSubmission> getSubmissionsByCourse(Long courseAssignmentId);
    SubmissionStatus calculateSubmissionStatus(DocumentSubmission submission, LocalDateTime deadline);
    
    // Submission Statistics
    SubmissionStatistics getStatisticsBySemester(Long semesterId, Long departmentId);
    SubmissionStatistics getStatisticsByProfessor(Long professorId, Long semesterId);
}
```

#### FileExplorerService
```java
public interface FileExplorerService {
    // Hierarchical Navigation
    FileExplorerNode getRootNode(Long academicYearId, Long semesterId, User currentUser);
    FileExplorerNode getNode(String nodePath, User currentUser);
    List<FileExplorerNode> getChildren(String parentPath, User currentUser);
    
    // Permission Checking
    boolean canRead(String nodePath, User user);
    boolean canWrite(String nodePath, User user);
    boolean canDelete(String nodePath, User user);
    
    // Breadcrumb Generation
    List<BreadcrumbItem> generateBreadcrumbs(String nodePath);
}

// Supporting classes
public class FileExplorerNode {
    private String path;
    private String name;
    private NodeType type; // YEAR, SEMESTER, PROFESSOR, COURSE, DOCUMENT_TYPE, FILE
    private Long entityId;
    private Map<String, Object> metadata;
    private List<FileExplorerNode> children;
    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
}

public enum NodeType {
    YEAR, SEMESTER, PROFESSOR, COURSE, DOCUMENT_TYPE, FILE
}
```

#### ReportService
```java
public interface ReportService {
    // HOD Reports
    ProfessorSubmissionReport generateProfessorSubmissionReport(Long semesterId, Long departmentId);
    byte[] exportReportToPdf(ProfessorSubmissionReport report);
    
    // Deanship Reports
    SystemWideReport generateSystemWideReport(Long semesterId);
    DepartmentReport generateDepartmentReport(Long semesterId, Long departmentId);
    
    // Report Filtering
    ProfessorSubmissionReport filterReport(ProfessorSubmissionReport report, ReportFilter filter);
}

// Supporting classes
public class ProfessorSubmissionReport {
    private Long semesterId;
    private String semesterName;
    private Long departmentId;
    private String departmentName;
    private List<ProfessorSubmissionRow> rows;
    private SubmissionStatistics statistics;
}

public class ProfessorSubmissionRow {
    private Long professorId;
    private String professorName;
    private String courseCode;
    private String courseName;
    private Map<DocumentTypeEnum, SubmissionStatus> documentStatuses;
}

public class SubmissionStatistics {
    private Integer totalProfessors;
    private Integer totalCourses;
    private Integer totalRequiredDocuments;
    private Integer submittedDocuments;
    private Integer missingDocuments;
    private Integer overdueDocuments;
}
```

### 4. Controller Layer

#### DeanshipController
```java
@RestController
@RequestMapping("/api/deanship")
@PreAuthorize("hasRole('DEANSHIP')")
public class DeanshipController {
    
    // Academic Year Management
    @PostMapping("/academic-years")
    public ResponseEntity<AcademicYear> createAcademicYear(@RequestBody AcademicYearDTO dto);
    
    @PutMapping("/academic-years/{id}")
    public ResponseEntity<AcademicYear> updateAcademicYear(@PathVariable Long id, @RequestBody AcademicYearDTO dto);
    
    @GetMapping("/academic-years")
    public ResponseEntity<List<AcademicYear>> getAllAcademicYears();
    
    // Professor Management
    @PostMapping("/professors")
    public ResponseEntity<User> createProfessor(@RequestBody ProfessorDTO dto);
    
    @PutMapping("/professors/{id}")
    public ResponseEntity<User> updateProfessor(@PathVariable Long id, @RequestBody ProfessorDTO dto);
    
    @GetMapping("/professors")
    public ResponseEntity<List<User>> getAllProfessors(@RequestParam(required = false) Long departmentId);
    
    @PutMapping("/professors/{id}/deactivate")
    public ResponseEntity<Void> deactivateProfessor(@PathVariable Long id);
    
    // Course Management
    @PostMapping("/courses")
    public ResponseEntity<Course> createCourse(@RequestBody CourseDTO dto);
    
    @PutMapping("/courses/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody CourseDTO dto);
    
    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getAllCourses(@RequestParam(required = false) Long departmentId);
    
    // Course Assignment
    @PostMapping("/course-assignments")
    public ResponseEntity<CourseAssignment> assignCourse(@RequestBody CourseAssignmentDTO dto);
    
    @DeleteMapping("/course-assignments/{id}")
    public ResponseEntity<Void> unassignCourse(@PathVariable Long id);
    
    @GetMapping("/course-assignments")
    public ResponseEntity<List<CourseAssignment>> getAssignments(
        @RequestParam Long semesterId,
        @RequestParam(required = false) Long professorId);
    
    // Required Document Types
    @PostMapping("/courses/{courseId}/required-documents")
    public ResponseEntity<RequiredDocumentType> addRequiredDocumentType(
        @PathVariable Long courseId, @RequestBody RequiredDocumentTypeDTO dto);
    
    // Reports
    @GetMapping("/reports/system-wide")
    public ResponseEntity<SystemWideReport> getSystemWideReport(@RequestParam Long semesterId);
}
```

#### HodController (Updated)
```java
@RestController
@RequestMapping("/api/hod")
@PreAuthorize("hasRole('HOD')")
public class HodController {
    
    // Dashboard Overview
    @GetMapping("/dashboard/overview")
    public ResponseEntity<DashboardOverview> getDashboardOverview(
        @RequestParam Long semesterId);
    
    // Professor Submission Status
    @GetMapping("/submissions/status")
    public ResponseEntity<ProfessorSubmissionReport> getSubmissionStatus(
        @RequestParam Long semesterId,
        @RequestParam(required = false) String courseCode,
        @RequestParam(required = false) DocumentTypeEnum documentType,
        @RequestParam(required = false) SubmissionStatus status);
    
    // Reports
    @GetMapping("/reports/professor-submissions")
    public ResponseEntity<ProfessorSubmissionReport> getProfessorSubmissionReport(
        @RequestParam Long semesterId);
    
    @GetMapping("/reports/professor-submissions/pdf")
    public ResponseEntity<byte[]> exportReportToPdf(@RequestParam Long semesterId);
    
    // File Explorer (Read-only)
    @GetMapping("/file-explorer/root")
    public ResponseEntity<FileExplorerNode> getFileExplorerRoot(
        @RequestParam Long academicYearId, @RequestParam Long semesterId);
    
    @GetMapping("/file-explorer/node")
    public ResponseEntity<FileExplorerNode> getFileExplorerNode(@RequestParam String path);
    
    // File Download
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId);
}
```

#### ProfessorController (Updated)
```java
@RestController
@RequestMapping("/api/professor")
@PreAuthorize("hasRole('PROFESSOR')")
public class ProfessorController {
    
    // Dashboard
    @GetMapping("/dashboard/courses")
    public ResponseEntity<List<CourseAssignmentWithStatus>> getMyCourses(
        @RequestParam Long semesterId);
    
    @GetMapping("/dashboard/overview")
    public ResponseEntity<ProfessorDashboardOverview> getDashboardOverview(
        @RequestParam Long semesterId);
    
    // File Upload
    @PostMapping("/submissions/upload")
    public ResponseEntity<DocumentSubmission> uploadFiles(
        @RequestParam Long courseAssignmentId,
        @RequestParam DocumentTypeEnum documentType,
        @RequestParam(required = false) String notes,
        @RequestPart("files") List<MultipartFile> files);
    
    // File Replacement
    @PutMapping("/submissions/{submissionId}/replace")
    public ResponseEntity<DocumentSubmission> replaceFiles(
        @PathVariable Long submissionId,
        @RequestParam(required = false) String notes,
        @RequestPart("files") List<MultipartFile> files);
    
    // Submission Status
    @GetMapping("/submissions")
    public ResponseEntity<List<DocumentSubmission>> getMySubmissions(
        @RequestParam Long semesterId);
    
    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<DocumentSubmission> getSubmission(@PathVariable Long submissionId);
    
    // File Explorer
    @GetMapping("/file-explorer/root")
    public ResponseEntity<FileExplorerNode> getFileExplorerRoot(
        @RequestParam Long academicYearId, @RequestParam Long semesterId);
    
    @GetMapping("/file-explorer/node")
    public ResponseEntity<FileExplorerNode> getFileExplorerNode(@RequestParam String path);
    
    // File Download
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId);
}
```

#### FileExplorerController (Shared)
```java
@RestController
@RequestMapping("/api/file-explorer")
public class FileExplorerController {
    
    @GetMapping("/root")
    public ResponseEntity<FileExplorerNode> getRoot(
        @RequestParam Long academicYearId,
        @RequestParam Long semesterId,
        Authentication authentication);
    
    @GetMapping("/node")
    public ResponseEntity<FileExplorerNode> getNode(
        @RequestParam String path,
        Authentication authentication);
    
    @GetMapping("/breadcrumbs")
    public ResponseEntity<List<BreadcrumbItem>> getBreadcrumbs(@RequestParam String path);
    
    @GetMapping("/files/{fileId}")
    public ResponseEntity<UploadedFile> getFileMetadata(@PathVariable Long fileId);
    
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
        @PathVariable Long fileId,
        Authentication authentication);
}
```

## Data Models

### Database Schema Changes

#### New Tables

1. **academic_years**
   - id (PK)
   - year_code (UNIQUE)
   - start_year
   - end_year
   - is_active
   - created_at
   - updated_at

2. **semesters**
   - id (PK)
   - academic_year_id (FK)
   - type (ENUM: FIRST, SECOND, SUMMER)
   - start_date
   - end_date
   - is_active
   - created_at
   - updated_at

3. **courses**
   - id (PK)
   - course_code (UNIQUE)
   - course_name
   - department_id (FK)
   - level
   - description
   - is_active
   - created_at
   - updated_at

4. **course_assignments**
   - id (PK)
   - semester_id (FK)
   - course_id (FK)
   - professor_id (FK)
   - is_active
   - created_at
   - updated_at
   - UNIQUE(semester_id, course_id, professor_id)

5. **required_document_types**
   - id (PK)
   - course_id (FK)
   - semester_id (FK, nullable)
   - document_type (ENUM)
   - deadline
   - is_required
   - max_file_count
   - max_total_size_mb
   - created_at
   - updated_at

6. **document_submissions**
   - id (PK)
   - course_assignment_id (FK)
   - document_type (ENUM)
   - professor_id (FK)
   - submitted_at
   - is_late_submission
   - status (ENUM)
   - notes
   - file_count
   - total_file_size
   - created_at
   - updated_at

7. **uploaded_files**
   - id (PK)
   - document_submission_id (FK)
   - file_url
   - original_filename
   - file_size
   - file_type
   - file_order
   - description
   - created_at
   - updated_at

8. **allowed_file_extensions**
   - id (PK)
   - required_document_type_id (FK)
   - extension

#### Modified Tables

1. **users**
   - Add: professor_id (UNIQUE, nullable)
   - Modify: role (add ROLE_DEANSHIP)

2. **roles** (if separate table)
   - Add: ROLE_DEANSHIP

#### Deprecated Tables (Keep for Migration)

1. **document_requests** - Data migrated to course_assignments + required_document_types
2. **submitted_documents** - Data migrated to document_submissions
3. **file_attachments** - Data migrated to uploaded_files

### File Storage Structure

Physical filesystem organization:
```
uploads/
├── 2024-2025/
│   ├── first/
│   │   ├── prof_12345/
│   │   │   ├── CS101/
│   │   │   │   ├── syllabus/
│   │   │   │   │   ├── file1.pdf
│   │   │   │   │   └── file2.pdf
│   │   │   │   ├── exam/
│   │   │   │   └── assignment/
│   │   │   └── CS102/
│   │   └── prof_67890/
│   ├── second/
│   └── summer/
└── 2025-2026/
```

File path generation pattern:
```
{uploads_base}/{year_code}/{semester_type}/{professor_id}/{course_code}/{document_type}/{unique_filename}
```

## Error Handling

### Exception Hierarchy

```java
public class ArchiveSystemException extends RuntimeException {
    private String errorCode;
    private HttpStatus httpStatus;
}

public class ResourceNotFoundException extends ArchiveSystemException {
    // 404 errors
}

public class UnauthorizedAccessException extends ArchiveSystemException {
    // 403 errors
}

public class ValidationException extends ArchiveSystemException {
    // 400 errors
}

public class FileUploadException extends ArchiveSystemException {
    // File-specific errors
}
```

### Error Response Format

```json
{
  "timestamp": "2025-11-17T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "errorCode": "VALIDATION_ERROR",
  "message": "File size exceeds maximum allowed size",
  "path": "/api/professor/submissions/upload",
  "details": {
    "maxSizeMb": 50,
    "actualSizeMb": 75
  }
}
```

### Common Error Scenarios

1. **File Upload Errors**
   - File type not allowed
   - File size exceeds limit
   - Total upload size exceeds limit
   - File count exceeds limit
   - Deadline passed (if enforced)

2. **Permission Errors**
   - User attempting to access another department's data (HOD)
   - Professor attempting to upload to unassigned course
   - Professor attempting to modify another professor's files
   - HOD attempting to manage professors

3. **Validation Errors**
   - Invalid academic year format
   - Duplicate course assignment
   - Missing required fields
   - Invalid date ranges

4. **Resource Not Found**
   - Academic year not found
   - Semester not found
   - Course not found
   - Professor not found
   - File not found

## Testing Strategy

### Unit Testing

**Entity Tests**
- Test entity relationships and cascading
- Test validation constraints
- Test helper methods (e.g., file count calculation)

**Service Tests**
- Mock repository layer
- Test business logic in isolation
- Test permission checking logic
- Test file path generation
- Test submission status calculation

**Controller Tests**
- Mock service layer
- Test request/response mapping
- Test validation
- Test authorization annotations

### Integration Testing

**Repository Tests**
- Test JPA queries
- Test custom repository methods
- Test transaction management

**API Integration Tests**
- Test full request/response cycle
- Test authentication and authorization
- Test file upload/download
- Test error handling

**File System Tests**
- Test file storage and retrieval
- Test file deletion
- Test directory creation

### End-to-End Testing

**User Workflow Tests**
1. Deanship creates academic year and assigns courses
2. Professor uploads files for assigned courses
3. HOD views submission status and generates report
4. All roles navigate file explorer

**Migration Tests**
- Test data migration from old schema to new schema
- Verify data integrity after migration
- Test backward compatibility during transition

### Performance Testing

- Test file upload with large files
- Test file explorer with many files/folders
- Test report generation with large datasets
- Test concurrent file uploads

## Security Considerations

### Authentication

- Session-based authentication with Spring Security
- Secure password storage with BCrypt
- Session timeout configuration
- CSRF protection for state-changing operations

### Authorization

**Role-Based Access Control (RBAC)**

| Resource | Deanship | HOD | Professor |
|----------|----------|-----|-----------|
| Academic Years | CRUD | Read | Read |
| Semesters | CRUD | Read | Read |
| Professors | CRUD | Read | Read (department) |
| Courses | CRUD | Read | Read |
| Course Assignments | CRUD | Read | Read (own) |
| File Upload | No | No | Yes (own courses) |
| File Read | All | Department | Department |
| File Delete | No | No | Yes (own, before deadline) |
| Reports | All | Department | Own |

**Method-Level Security**
```java
@PreAuthorize("hasRole('DEANSHIP')")
public void createProfessor(ProfessorDTO dto) { ... }

@PreAuthorize("hasRole('PROFESSOR') and #professorId == authentication.principal.id")
public void uploadFiles(Long professorId, ...) { ... }

@PreAuthorize("hasAnyRole('HOD', 'DEANSHIP')")
public ProfessorSubmissionReport getReport(Long semesterId) { ... }
```

### File Security

- Validate file types using MIME type checking
- Scan uploaded files for malware (optional integration)
- Store files outside web root
- Generate unique filenames to prevent overwrites
- Implement file size limits
- Sanitize filenames to prevent path traversal

### Data Security

- Encrypt sensitive data at rest (if required)
- Use HTTPS for all communications
- Implement audit logging for sensitive operations
- Prevent SQL injection through parameterized queries
- Validate and sanitize all user inputs

## Frontend Design

### Page Structure

#### 1. Deanship Dashboard (`deanship-dashboard.html`)

**Sections:**
- Navigation: Academic Years, Professors, Courses, Assignments, Reports, File Explorer
- Academic Year Management: Create/edit years, view semesters
- Professor Management: Create/edit/deactivate professors
- Course Management: Create/edit courses
- Course Assignment: Assign professors to courses for semesters
- System-Wide Reports: View submission statistics

**Key Components:**
- Academic year selector (dropdown)
- Semester tabs (First, Second, Summer)
- Professor table with CRUD actions
- Course table with CRUD actions
- Assignment form with professor/course/semester selectors
- File explorer tree view

#### 2. HOD Dashboard (`hod-dashboard.html`)

**Sections:**
- Navigation: Dashboard, Submission Status, Reports, File Explorer
- Overview Widgets: Total professors, courses, submission statistics
- Professor Submission Status Table: Filter by course, document type, status
- Reports: Generate and export PDF reports
- File Explorer: Browse department files (read-only)

**Key Components:**
- Semester selector
- Overview cards (professors, courses, submitted, missing, overdue)
- Filterable submission status table
- Export to PDF button
- File explorer tree view with breadcrumbs

#### 3. Professor Dashboard (`prof-dashboard.html`)

**Sections:**
- Navigation: Dashboard, My Courses, File Explorer, Notifications
- Semester Selector: Choose academic year and semester
- Course Cards: Display assigned courses with document requirements
- Upload Modal: Multi-file upload with notes
- File Explorer: Browse department files

**Key Components:**
- Semester selector (year + semester dropdown)
- Course cards with document type status indicators
- Upload/Replace buttons per document type
- Multi-file upload modal
- File list with download links
- File explorer tree view

### UI Components

#### File Explorer Component

**Structure:**
```html
<div class="file-explorer">
  <!-- Breadcrumbs -->
  <nav class="breadcrumbs">
    <a href="#" data-path="/">Home</a> /
    <a href="#" data-path="/2024-2025">2024-2025</a> /
    <a href="#" data-path="/2024-2025/first">First Semester</a>
  </nav>
  
  <!-- Tree View (Left Panel) -->
  <div class="tree-view">
    <ul class="tree-root">
      <li class="tree-node" data-type="year">
        <span class="node-icon">📅</span>
        <span class="node-name">2024-2025</span>
        <ul class="tree-children">
          <li class="tree-node" data-type="semester">
            <span class="node-icon">📚</span>
            <span class="node-name">First Semester</span>
          </li>
        </ul>
      </li>
    </ul>
  </div>
  
  <!-- File List (Right Panel) -->
  <div class="file-list">
    <table class="file-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Size</th>
          <th>Uploaded</th>
          <th>Uploaded By</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>syllabus.pdf</td>
          <td>2.5 MB</td>
          <td>2025-11-15 10:30</td>
          <td>Prof. Smith</td>
          <td>
            <button class="btn-view">View</button>
            <button class="btn-download">Download</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</div>
```

**JavaScript Behavior:**
- Lazy load tree nodes on expand
- Update breadcrumbs on navigation
- Fetch file list when folder selected
- Handle file preview/download
- Show/hide actions based on permissions

#### Upload Modal Component

**Structure:**
```html
<div class="modal" id="uploadModal">
  <div class="modal-content">
    <h2>Upload Files</h2>
    <p>Course: <strong id="uploadCourseName"></strong></p>
    <p>Document Type: <strong id="uploadDocType"></strong></p>
    
    <div class="file-drop-zone">
      <input type="file" id="fileInput" multiple accept=".pdf,.zip">
      <p>Drag files here or click to browse</p>
      <p class="file-requirements">
        Allowed: PDF, ZIP | Max 5 files | Max 50 MB total
      </p>
    </div>
    
    <div class="file-preview-list">
      <!-- Dynamic file list -->
    </div>
    
    <div class="form-group">
      <label for="uploadNotes">Notes (optional)</label>
      <textarea id="uploadNotes" rows="3"></textarea>
    </div>
    
    <div class="modal-actions">
      <button class="btn-cancel">Cancel</button>
      <button class="btn-upload">Upload</button>
    </div>
  </div>
</div>
```

### API Integration

**JavaScript Service Layer:**
```javascript
// api-service.js
class ApiService {
  async getAcademicYears() {
    return fetch('/api/deanship/academic-years').then(r => r.json());
  }
  
  async getMyCourses(semesterId) {
    return fetch(`/api/professor/dashboard/courses?semesterId=${semesterId}`)
      .then(r => r.json());
  }
  
  async uploadFiles(courseAssignmentId, documentType, files, notes) {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    formData.append('courseAssignmentId', courseAssignmentId);
    formData.append('documentType', documentType);
    if (notes) formData.append('notes', notes);
    
    return fetch('/api/professor/submissions/upload', {
      method: 'POST',
      body: formData
    }).then(r => r.json());
  }
  
  async getFileExplorerNode(path) {
    return fetch(`/api/file-explorer/node?path=${encodeURIComponent(path)}`)
      .then(r => r.json());
  }
}
```

## Migration Strategy

### Phase 1: Database Schema Migration

1. Create new tables (academic_years, semesters, courses, etc.)
2. Add new columns to users table (professor_id, updated role enum)
3. Keep old tables (document_requests, submitted_documents, file_attachments)

### Phase 2: Data Migration

**Migration Script Steps:**

1. **Create Default Academic Year**
   - Analyze existing document_requests to determine year range
   - Create academic year records (e.g., 2024-2025)
   - Create three semesters per year

2. **Migrate Professors**
   - Generate professor_id for existing ROLE_PROFESSOR users
   - Update user records

3. **Extract and Create Courses**
   - Extract unique course names from document_requests
   - Create course records with generated course codes
   - Link to departments

4. **Create Course Assignments**
   - For each document_request:
     - Determine semester based on deadline date
     - Create course_assignment linking professor, course, semester

5. **Migrate Document Submissions**
   - For each submitted_document:
     - Find corresponding course_assignment
     - Create document_submission record
     - Link to course_assignment and document type

6. **Migrate Files**
   - For each file_attachment:
     - Create uploaded_file record
     - Move physical file to new folder structure
     - Update file_url in database

7. **Create Required Document Types**
   - Extract document types from document_requests
   - Create required_document_type records per course

### Phase 3: Code Deployment

1. Deploy new backend code with both old and new endpoints
2. Deploy new frontend pages (deanship-dashboard.html, updated hod/prof dashboards)
3. Update navigation to include new pages
4. Keep old pages accessible during transition

### Phase 4: Testing and Validation

1. Verify all migrated data is accessible
2. Test new workflows (Deanship creates year, assigns courses, etc.)
3. Test file upload/download with new structure
4. Verify reports show correct data

### Phase 5: Cleanup

1. Remove old endpoints after validation period
2. Remove old frontend pages
3. Archive old tables (don't drop immediately)
4. Update documentation

## Deployment Considerations

### Environment Configuration

**application.properties additions:**
```properties
# File Upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# File Storage
app.upload.base-path=./uploads
app.upload.allowed-extensions=pdf,zip
app.upload.max-file-count=5
app.upload.max-total-size-mb=50

# Academic Year
app.academic.default-year=2024-2025
app.academic.auto-create-semesters=true
```

### Database Indexes

```sql
-- Performance indexes
CREATE INDEX idx_course_assignments_semester ON course_assignments(semester_id);
CREATE INDEX idx_course_assignments_professor ON course_assignments(professor_id);
CREATE INDEX idx_document_submissions_course_assignment ON document_submissions(course_assignment_id);
CREATE INDEX idx_document_submissions_professor ON document_submissions(professor_id);
CREATE INDEX idx_uploaded_files_submission ON uploaded_files(document_submission_id);
CREATE INDEX idx_users_department ON users(department_id);
CREATE INDEX idx_users_role ON users(role);
```

### Monitoring and Logging

**Key Metrics to Monitor:**
- File upload success/failure rate
- Average file upload time
- Storage usage by semester/department
- API response times
- Authentication failures
- Permission denial events

**Logging Strategy:**
- Log all file uploads/downloads with user and timestamp
- Log all permission denials
- Log all data modifications (audit trail)
- Log migration progress and errors

## Future Enhancements

1. **Advanced File Preview**: In-browser PDF viewer, ZIP file browser
2. **Version Control**: Track file versions when replaced
3. **Bulk Operations**: Bulk upload, bulk download as ZIP
4. **Email Notifications**: Notify professors of approaching deadlines
5. **Dashboard Analytics**: Charts and graphs for submission trends
6. **Mobile Responsive**: Optimize UI for mobile devices
7. **Search Functionality**: Search files by name, course, professor
8. **Comments/Feedback**: HOD can leave comments on submissions
9. **Approval Workflow**: HOD approval required for certain document types
10. **Integration**: Export data to external systems (LMS, SIS)
