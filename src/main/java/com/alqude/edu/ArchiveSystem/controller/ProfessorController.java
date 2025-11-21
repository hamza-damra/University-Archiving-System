package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.common.FileAttachmentResponse;
import com.alqude.edu.ArchiveSystem.dto.common.NotificationResponse;
import com.alqude.edu.ArchiveSystem.dto.common.SubmittedDocumentResponse;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestResponse;
import com.alqude.edu.ArchiveSystem.entity.SubmittedDocument;
import com.alqude.edu.ArchiveSystem.repository.FileAttachmentRepository;
import com.alqude.edu.ArchiveSystem.service.AcademicService;
import com.alqude.edu.ArchiveSystem.service.AuthService;
import com.alqude.edu.ArchiveSystem.service.DocumentRequestService;
import com.alqude.edu.ArchiveSystem.service.FileExplorerService;
import com.alqude.edu.ArchiveSystem.service.FileService;
import com.alqude.edu.ArchiveSystem.service.FileUploadService;
import com.alqude.edu.ArchiveSystem.service.MultiFileUploadService;
import com.alqude.edu.ArchiveSystem.service.NotificationService;
import com.alqude.edu.ArchiveSystem.service.ProfessorService;
import com.alqude.edu.ArchiveSystem.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Professor Controller providing both new semester-based endpoints and legacy request-based endpoints.
 * 
 * <h2>Authentication & Authorization</h2>
 * <p>All endpoints in this controller require the user to be authenticated with ROLE_PROFESSOR.</p>
 * <p>Session-based authentication is used with CSRF protection enabled.</p>
 * 
 * <h2>Semester-Based Endpoints (Recommended)</h2>
 * <p>The semester-based endpoints provide a modern interface for professors to:</p>
 * <ul>
 *   <li>View assigned courses for a specific semester</li>
 *   <li>Upload and manage document submissions</li>
 *   <li>Browse files through a hierarchical file explorer</li>
 *   <li>View dashboard statistics and notifications</li>
 * </ul>
 * 
 * <h2>Legacy Endpoints (Deprecated)</h2>
 * <p>Legacy endpoints use deprecated services (DocumentRequestService, MultiFileUploadService)
 * to maintain compatibility with existing clients during the transition period.</p>
 * <p>New development should use the semester-based endpoints.</p>
 * 
 * <h2>Error Responses</h2>
 * <p>All endpoints return a standard ApiResponse wrapper with the following structure:</p>
 * <pre>
 * {
 *   "success": true/false,
 *   "message": "Description of result",
 *   "data": {...},
 *   "timestamp": "2024-11-19T10:30:00Z"
 * }
 * </pre>
 * 
 * <h3>Common HTTP Status Codes:</h3>
 * <ul>
 *   <li>200 OK - Request successful</li>
 *   <li>201 CREATED - Resource created successfully</li>
 *   <li>400 BAD REQUEST - Invalid request parameters or validation failure</li>
 *   <li>401 UNAUTHORIZED - User not authenticated or session expired</li>
 *   <li>403 FORBIDDEN - User does not have required role or permissions</li>
 *   <li>404 NOT FOUND - Requested resource not found</li>
 *   <li>500 INTERNAL SERVER ERROR - Server error occurred</li>
 * </ul>
 * 
 * <h2>File Upload Requirements</h2>
 * <ul>
 *   <li>Allowed file types: PDF, ZIP</li>
 *   <li>Maximum file count and size limits are defined per document type</li>
 *   <li>Files are stored in the pattern: year/semester/professorId/courseCode/documentType</li>
 * </ul>
 * 
 * <h2>Permission Model</h2>
 * <ul>
 *   <li>Professors can upload files to their own course folders (write access)</li>
 *   <li>Professors can view files from other professors in the same department (read-only)</li>
 *   <li>Professors cannot access files from other departments</li>
 * </ul>
 * 
 * @author Archive System Team
 * @version 2.0
 * @since 2024-11-19
 */
@RestController
@RequestMapping("/api/professor")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('PROFESSOR')")
@SuppressWarnings("deprecation")
public class ProfessorController {
    
    private final DocumentRequestService documentRequestService;
    private final FileUploadService fileUploadService;
    private final MultiFileUploadService multiFileUploadService;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final ProfessorService professorService;
    private final FileService fileService;
    private final SubmissionService submissionService;
    private final FileExplorerService fileExplorerService;
    private final AcademicService academicService;
    
    // ========== Academic Year & Semester Endpoints ==========
    
    /**
     * Get all academic years (read-only for professors)
     * 
     * <p>Retrieves a list of all academic years in the system. Professors use this to select
     * which academic year they want to view data for. The active academic year is typically
     * auto-selected by the frontend.</p>
     * 
     * <h3>Example Response:</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Academic years retrieved successfully",
     *   "data": [
     *     {
     *       "id": 1,
     *       "yearCode": "2024-2025",
     *       "startDate": "2024-09-01",
     *       "endDate": "2025-08-31",
     *       "isActive": true
     *     }
     *   ]
     * }
     * </pre>
     * 
     * @return ResponseEntity containing ApiResponse with list of all academic years
     * @throws RuntimeException if database error occurs
     */
    @GetMapping("/academic-years")
    public ResponseEntity<ApiResponse<List<com.alqude.edu.ArchiveSystem.entity.AcademicYear>>> getAllAcademicYears() {
        log.info("Professor retrieving all academic years");
        
        try {
            List<com.alqude.edu.ArchiveSystem.entity.AcademicYear> academicYears = academicService.getAllAcademicYears();
            return ResponseEntity.ok(ApiResponse.success("Academic years retrieved successfully", academicYears));
        } catch (Exception e) {
            log.error("Error retrieving academic years", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve academic years: " + e.getMessage()));
        }
    }
    
    /**
     * Get all semesters for a specific academic year
     * 
     * <p>Retrieves all semesters (FIRST, SECOND, SUMMER) for a given academic year.
     * Professors use this to select which semester they want to view courses and submissions for.</p>
     * 
     * <h3>Example Request:</h3>
     * <pre>GET /api/professor/academic-years/1/semesters</pre>
     * 
     * <h3>Example Response:</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Semesters retrieved successfully",
     *   "data": [
     *     {
     *       "id": 1,
     *       "type": "FIRST",
     *       "startDate": "2024-09-01",
     *       "endDate": "2025-01-15",
     *       "isActive": true,
     *       "academicYearId": 1
     *     }
     *   ]
     * }
     * </pre>
     * 
     * @param academicYearId The ID of the academic year
     * @return ResponseEntity containing ApiResponse with list of semesters
     * @throws RuntimeException if academic year not found or database error occurs
     */
    @GetMapping("/academic-years/{academicYearId}/semesters")
    public ResponseEntity<ApiResponse<List<com.alqude.edu.ArchiveSystem.entity.Semester>>> getSemestersByYear(
            @PathVariable Long academicYearId) {
        log.info("Professor retrieving semesters for academic year ID: {}", academicYearId);
        
        try {
            List<com.alqude.edu.ArchiveSystem.entity.Semester> semesters = academicService.getSemestersByYear(academicYearId);
            return ResponseEntity.ok(ApiResponse.success("Semesters retrieved successfully", semesters));
        } catch (Exception e) {
            log.error("Error retrieving semesters for academic year ID: {}", academicYearId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve semesters: " + e.getMessage()));
        }
    }
    
    // ========== Semester-Based Dashboard Endpoints ==========
    
    /**
     * Get professor's courses with submission status for a semester
     * 
     * <p>Retrieves all courses assigned to the authenticated professor for a specific semester,
     * along with the submission status for each required document type. This is the primary
     * endpoint for populating the "My Courses" tab in the professor dashboard.</p>
     * 
     * <h3>Authentication:</h3>
     * <p>Requires ROLE_PROFESSOR. The professor ID is automatically extracted from the
     * authenticated user's session.</p>
     * 
     * <h3>Document Status Values:</h3>
     * <ul>
     *   <li>NOT_UPLOADED - No files have been submitted for this document type</li>
     *   <li>UPLOADED - Files have been successfully submitted</li>
     *   <li>OVERDUE - Deadline has passed and no files were submitted</li>
     * </ul>
     * 
     * <h3>Example Request:</h3>
     * <pre>GET /api/professor/dashboard/courses?semesterId=1</pre>
     * 
     * <h3>Example Response:</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Courses retrieved successfully",
     *   "data": [
     *     {
     *       "courseAssignmentId": 1,
     *       "courseCode": "CS101",
     *       "courseName": "Introduction to Computer Science",
     *       "departmentName": "Computer Science",
     *       "courseLevel": "UNDERGRADUATE",
     *       "semesterType": "FIRST",
     *       "academicYearCode": "2024-2025",
     *       "documentStatuses": {
     *         "SYLLABUS": {
     *           "documentType": "SYLLABUS",
     *           "status": "UPLOADED",
     *           "submissionId": 10,
     *           "fileCount": 1,
     *           "totalFileSize": 524288,
     *           "submittedAt": "2024-11-15T10:30:00",
     *           "isLateSubmission": false,
     *           "deadline": "2024-11-20T23:59:59",
     *           "maxFileCount": 1,
     *           "maxTotalSizeMb": 10,
     *           "allowedFileExtensions": ["pdf"]
     *         },
     *         "EXAM": {
     *           "documentType": "EXAM",
     *           "status": "NOT_UPLOADED",
     *           "deadline": "2024-12-01T23:59:59",
     *           "maxFileCount": 5,
     *           "maxTotalSizeMb": 50,
     *           "allowedFileExtensions": ["pdf", "zip"]
     *         }
     *       }
     *     }
     *   ]
     * }
     * </pre>
     * 
     * @param semesterId The ID of the semester to retrieve courses for
     * @return ResponseEntity containing ApiResponse with list of courses and their submission statuses
     * @throws RuntimeException if semester not found or database error occurs
     */
    @GetMapping("/dashboard/courses")
    public ResponseEntity<ApiResponse<List<com.alqude.edu.ArchiveSystem.dto.professor.CourseAssignmentWithStatus>>> getMyCourses(
            @RequestParam Long semesterId) {
        
        var currentUser = authService.getCurrentUser();
        log.info("Professor {} (ID: {}) fetching courses for semester ID: {}", 
                currentUser.getEmail(), currentUser.getId(), semesterId);
        
        try {
            List<com.alqude.edu.ArchiveSystem.dto.professor.CourseAssignmentWithStatus> courses = 
                    professorService.getProfessorCoursesWithStatus(currentUser.getId(), semesterId);
            
            log.debug("Successfully retrieved {} courses for professor {} in semester {}", 
                    courses.size(), currentUser.getId(), semesterId);
            
            return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", courses));
        } catch (Exception e) {
            log.error("Error fetching courses for professor {} in semester {}: {}", 
                    currentUser.getId(), semesterId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get professor's dashboard overview for a semester
     * 
     * <p>Retrieves summary statistics for the professor's dashboard, including total courses,
     * submitted documents, pending documents, and overdue documents for a specific semester.
     * This endpoint populates the "Dashboard" tab overview section.</p>
     * 
     * <h3>Example Request:</h3>
     * <pre>GET /api/professor/dashboard/overview?semesterId=1</pre>
     * 
     * <h3>Example Response:</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Dashboard overview retrieved successfully",
     *   "data": {
     *     "totalCourses": 3,
     *     "submittedDocuments": 5,
     *     "pendingDocuments": 7,
     *     "overdueDocuments": 2,
     *     "upcomingDeadlines": [
     *       {
     *         "courseCode": "CS101",
     *         "courseName": "Introduction to Computer Science",
     *         "documentType": "EXAM",
     *         "deadline": "2024-12-01T23:59:59",
     *         "hoursRemaining": 72
     *       }
     *     ]
     *   }
     * }
     * </pre>
     * 
     * @param semesterId The ID of the semester to retrieve overview for
     * @return ResponseEntity containing ApiResponse with dashboard overview statistics
     * @throws RuntimeException if semester not found or database error occurs
     */
    @GetMapping("/dashboard/overview")
    public ResponseEntity<ApiResponse<com.alqude.edu.ArchiveSystem.dto.professor.ProfessorDashboardOverview>> getDashboardOverview(
            @RequestParam Long semesterId) {
        
        log.info("Professor fetching dashboard overview for semester ID: {}", semesterId);
        var currentUser = authService.getCurrentUser();
        
        com.alqude.edu.ArchiveSystem.dto.professor.ProfessorDashboardOverview overview = 
                professorService.getProfessorDashboardOverview(currentUser.getId(), semesterId);
        
        return ResponseEntity.ok(ApiResponse.success("Dashboard overview retrieved successfully", overview));
    }
    
    // ========== Semester-Based File Upload Endpoints ==========
    
    /**
     * Upload files for a course assignment and document type
     * 
     * <p>Uploads one or more files for a specific course assignment and document type.
     * This endpoint handles file validation, storage, and creates a DocumentSubmission record.</p>
     * 
     * <h3>Authentication & Authorization:</h3>
     * <p>Requires ROLE_PROFESSOR. The professor must be assigned to the specified course.</p>
     * 
     * <h3>File Validation:</h3>
     * <ul>
     *   <li>Allowed file types: PDF, ZIP only</li>
     *   <li>Maximum file count: Defined per document type (typically 1-5 files)</li>
     *   <li>Maximum total size: Defined per document type (typically 10-50 MB)</li>
     *   <li>Filenames are sanitized to prevent path traversal attacks</li>
     * </ul>
     * 
     * <h3>File Storage:</h3>
     * <p>Files are stored in the pattern: {uploadDir}/{year}/{semester}/{professorId}/{courseCode}/{documentType}/</p>
     * 
     * <h3>Example Request:</h3>
     * <pre>
     * POST /api/professor/submissions/upload?courseAssignmentId=1&documentType=SYLLABUS&notes=Updated syllabus
     * Content-Type: multipart/form-data
     * 
     * files: [file1.pdf, file2.pdf]
     * </pre>
     * 
     * <h3>Example Response (Success):</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Files uploaded successfully",
     *   "data": {
     *     "id": 10,
     *     "courseAssignmentId": 1,
     *     "documentType": "SYLLABUS",
     *     "fileCount": 2,
     *     "totalFileSize": 1048576,
     *     "submittedAt": "2024-11-19T10:30:00",
     *     "isLateSubmission": false,
     *     "notes": "Updated syllabus",
     *     "professorId": 5
     *   }
     * }
     * </pre>
     * 
     * <h3>Error Responses:</h3>
     * <ul>
     *   <li>400 BAD REQUEST - Invalid file type, file count exceeds limit, or file size exceeds limit</li>
     *   <li>403 FORBIDDEN - Professor not assigned to this course</li>
     *   <li>404 NOT FOUND - Course assignment not found</li>
     *   <li>500 INTERNAL SERVER ERROR - File storage error</li>
     * </ul>
     * 
     * @param courseAssignmentId The ID of the course assignment
     * @param documentType The type of document being uploaded (SYLLABUS, EXAM, ASSIGNMENT, etc.)
     * @param notes Optional notes about the submission
     * @param files List of files to upload (multipart/form-data)
     * @return ResponseEntity containing ApiResponse with the created DocumentSubmission
     * @throws IllegalArgumentException if validation fails
     * @throws IOException if file storage fails
     */
    @PostMapping("/submissions/upload")
    public ResponseEntity<ApiResponse<com.alqude.edu.ArchiveSystem.entity.DocumentSubmission>> uploadFiles(
            @RequestParam Long courseAssignmentId,
            @RequestParam com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum documentType,
            @RequestParam(required = false) String notes,
            @RequestPart("files") List<MultipartFile> files) {
        
        var currentUser = authService.getCurrentUser();
        
        log.info("Professor {} (ID: {}) uploading {} files for course assignment ID: {}, document type: {}", 
                currentUser.getEmail(), currentUser.getId(), files.size(), courseAssignmentId, documentType);
        
        // Log file details
        long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        log.debug("Upload details - Total size: {} bytes, Files: {}", totalSize, 
                files.stream().map(MultipartFile::getOriginalFilename).toList());
        
        try {
            // Call FileService to upload files (it handles validation and submission creation)
            fileService.uploadFiles(courseAssignmentId, documentType, files, notes, currentUser.getId());
            
            // Get the submission that was created/updated by FileService
            com.alqude.edu.ArchiveSystem.entity.DocumentSubmission submission = 
                    submissionService.getSubmissionsByCourse(courseAssignmentId).stream()
                            .filter(s -> s.getDocumentType() == documentType)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Submission not found after upload"));
            
            log.info("Successfully uploaded {} files (total: {} bytes) for professor {} - course assignment ID: {}, submission ID: {}", 
                    files.size(), totalSize, currentUser.getId(), courseAssignmentId, submission.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Files uploaded successfully", submission));
        } catch (Exception e) {
            log.error("Failed to upload files for professor {} - course assignment ID: {}, document type: {}: {}", 
                    currentUser.getId(), courseAssignmentId, documentType, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Replace files for an existing submission
     * 
     * <p>Replaces previously uploaded files for a document submission. The old files are
     * deleted from storage and new files are uploaded. The submission timestamp is updated
     * and late submission status is recalculated based on the deadline.</p>
     * 
     * <h3>Authentication & Authorization:</h3>
     * <p>Requires ROLE_PROFESSOR. The professor must own the submission (created by them).</p>
     * <p>Security check: @securityExpressionService.ownsSubmission(#submissionId)</p>
     * 
     * <h3>File Validation:</h3>
     * <p>Same validation rules as upload endpoint apply (file type, count, size limits).</p>
     * 
     * <h3>Example Request:</h3>
     * <pre>
     * PUT /api/professor/submissions/10/replace?notes=Corrected version
     * Content-Type: multipart/form-data
     * 
     * files: [corrected_file.pdf]
     * </pre>
     * 
     * <h3>Example Response:</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Files replaced successfully",
     *   "data": {
     *     "id": 10,
     *     "courseAssignmentId": 1,
     *     "documentType": "SYLLABUS",
     *     "fileCount": 1,
     *     "totalFileSize": 524288,
     *     "submittedAt": "2024-11-19T14:30:00",
     *     "isLateSubmission": false,
     *     "notes": "Corrected version"
     *   }
     * }
     * </pre>
     * 
     * <h3>Error Responses:</h3>
     * <ul>
     *   <li>400 BAD REQUEST - Invalid file type or size</li>
     *   <li>403 FORBIDDEN - Professor does not own this submission</li>
     *   <li>404 NOT FOUND - Submission not found</li>
     *   <li>500 INTERNAL SERVER ERROR - File deletion or storage error</li>
     * </ul>
     * 
     * @param submissionId The ID of the submission to replace files for
     * @param notes Optional notes about the replacement
     * @param files List of new files to upload
     * @return ResponseEntity containing ApiResponse with the updated DocumentSubmission
     * @throws IllegalArgumentException if professor doesn't own submission or validation fails
     * @throws IOException if file operations fail
     */
    @PutMapping("/submissions/{submissionId}/replace")
    @PreAuthorize("hasRole('PROFESSOR') and @securityExpressionService.ownsSubmission(#submissionId)")
    public ResponseEntity<ApiResponse<com.alqude.edu.ArchiveSystem.entity.DocumentSubmission>> replaceFiles(
            @PathVariable Long submissionId,
            @RequestParam(required = false) String notes,
            @RequestPart("files") List<MultipartFile> files) {
        
        log.info("Professor replacing files for submission ID: {}", submissionId);
        
        var currentUser = authService.getCurrentUser();
        
        // Get the submission and validate professor owns it
        com.alqude.edu.ArchiveSystem.entity.DocumentSubmission submission = 
                submissionService.getSubmission(submissionId);
        
        if (!submission.getProfessor().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Professor does not own submission ID: " + submissionId);
        }
        
        // Call FileService to replace files
        fileService.replaceFiles(submissionId, files, notes);
        
        // Get updated submission
        submission = submissionService.getSubmission(submissionId);
        
        log.info("Successfully replaced files for submission ID: {}", submissionId);
        
        return ResponseEntity.ok(ApiResponse.success("Files replaced successfully", submission));
    }
    
    /**
     * Get all submissions for the professor in a semester
     * 
     * <p>Retrieves all document submissions created by the authenticated professor
     * for a specific semester. This includes submissions for all courses and document types.</p>
     * 
     * <h3>Example Request:</h3>
     * <pre>GET /api/professor/submissions?semesterId=1</pre>
     * 
     * <h3>Example Response:</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Submissions retrieved successfully",
     *   "data": [
     *     {
     *       "id": 10,
     *       "courseAssignmentId": 1,
     *       "documentType": "SYLLABUS",
     *       "fileCount": 1,
     *       "totalFileSize": 524288,
     *       "submittedAt": "2024-11-15T10:30:00",
     *       "isLateSubmission": false,
     *       "notes": "Initial submission"
     *     }
     *   ]
     * }
     * </pre>
     * 
     * @param semesterId The ID of the semester to retrieve submissions for
     * @return ResponseEntity containing ApiResponse with list of submissions
     * @throws RuntimeException if semester not found or database error occurs
     */
    @GetMapping("/submissions")
    public ResponseEntity<ApiResponse<List<com.alqude.edu.ArchiveSystem.entity.DocumentSubmission>>> getMySubmissions(
            @RequestParam Long semesterId) {
        
        log.info("Professor fetching submissions for semester ID: {}", semesterId);
        var currentUser = authService.getCurrentUser();
        
        List<com.alqude.edu.ArchiveSystem.entity.DocumentSubmission> submissions = 
                submissionService.getSubmissionsByProfessor(currentUser.getId(), semesterId);
        
        return ResponseEntity.ok(ApiResponse.success("Submissions retrieved successfully", submissions));
    }
    
    /**
     * Get a specific submission by ID
     * 
     * <p>Retrieves detailed information about a specific document submission.
     * Professors can view their own submissions or submissions from other professors
     * in the same department (read-only access).</p>
     * 
     * <h3>Authorization:</h3>
     * <p>Professor must either own the submission OR be in the same department as the submission owner.</p>
     * 
     * <h3>Example Request:</h3>
     * <pre>GET /api/professor/submissions/10</pre>
     * 
     * <h3>Example Response:</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Submission retrieved successfully",
     *   "data": {
     *     "id": 10,
     *     "courseAssignmentId": 1,
     *     "documentType": "SYLLABUS",
     *     "fileCount": 1,
     *     "totalFileSize": 524288,
     *     "submittedAt": "2024-11-15T10:30:00",
     *     "isLateSubmission": false,
     *     "notes": "Initial submission",
     *     "uploadedFiles": [
     *       {
     *         "id": 100,
     *         "originalFilename": "syllabus.pdf",
     *         "fileSize": 524288,
     *         "fileType": "application/pdf"
     *       }
     *     ]
     *   }
     * }
     * </pre>
     * 
     * <h3>Error Responses:</h3>
     * <ul>
     *   <li>403 FORBIDDEN - Professor not in same department as submission owner</li>
     *   <li>404 NOT FOUND - Submission not found</li>
     * </ul>
     * 
     * @param submissionId The ID of the submission to retrieve
     * @return ResponseEntity containing ApiResponse with submission details
     * @throws IllegalArgumentException if professor doesn't have access to submission
     */
    @GetMapping("/submissions/{submissionId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<ApiResponse<com.alqude.edu.ArchiveSystem.entity.DocumentSubmission>> getSubmission(
            @PathVariable Long submissionId) {
        
        log.info("Professor fetching submission ID: {}", submissionId);
        var currentUser = authService.getCurrentUser();
        
        com.alqude.edu.ArchiveSystem.entity.DocumentSubmission submission = 
                submissionService.getSubmission(submissionId);
        
        // Validate professor owns the submission or has read access (same department)
        if (!submission.getProfessor().getId().equals(currentUser.getId()) &&
            !submission.getProfessor().getDepartment().getId().equals(currentUser.getDepartment().getId())) {
            throw new IllegalArgumentException("Professor does not have access to submission ID: " + submissionId);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Submission retrieved successfully", submission));
    }
    
    // ========== Semester-Based File Explorer Endpoints ==========
    
    /**
     * Get file explorer root node for a semester
     * 
     * <p>Retrieves the root node of the file explorer hierarchy for a specific academic year
     * and semester. The file explorer provides a hierarchical view of the folder structure
     * with department-scoped permissions.</p>
     * 
     * <h3>Permission Model:</h3>
     * <ul>
     *   <li>Professors can see their own folders with write access (canWrite: true)</li>
     *   <li>Professors can see other professors' folders in the same department (read-only)</li>
     *   <li>Professors cannot see folders from other departments</li>
     * </ul>
     * 
     * <h3>Folder Hierarchy:</h3>
     * <pre>
     * Root
     * └── Academic Year (2024-2025)
     *     └── Semester (FIRST)
     *         └── Professor (John Doe)
     *             └── Course (CS101)
     *                 └── Document Type (SYLLABUS)
     *                     └── Files
     * </pre>
     * 
     * <h3>Example Request:</h3>
     * <pre>GET /api/professor/file-explorer/root?academicYearId=1&semesterId=1</pre>
     * 
     * <h3>Example Response:</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "File explorer root retrieved successfully",
     *   "data": {
     *     "path": "/2024-2025/FIRST",
     *     "name": "FIRST Semester",
     *     "type": "SEMESTER",
     *     "canRead": true,
     *     "canWrite": false,
     *     "canDelete": false,
     *     "children": [
     *       {
     *         "path": "/2024-2025/FIRST/5",
     *         "name": "John Doe (You)",
     *         "type": "PROFESSOR",
     *         "canRead": true,
     *         "canWrite": true,
     *         "canDelete": false,
     *         "children": []
     *       }
     *     ]
     *   }
     * }
     * </pre>
     * 
     * @param academicYearId The ID of the academic year
     * @param semesterId The ID of the semester
     * @return ResponseEntity containing ApiResponse with the root FileExplorerNode
     * @throws RuntimeException if academic year or semester not found
     */
    @GetMapping("/file-explorer/root")
    public ResponseEntity<ApiResponse<com.alqude.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode>> getFileExplorerRoot(
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId) {
        
        log.info("Professor fetching file explorer root for academic year ID: {}, semester ID: {}", 
                academicYearId, semesterId);
        var currentUser = authService.getCurrentUser();
        
        com.alqude.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode rootNode = 
                fileExplorerService.getRootNode(academicYearId, semesterId, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success("File explorer root retrieved successfully", rootNode));
    }
    
    /**
     * Get a specific node in the file explorer hierarchy
     * 
     * <p>Retrieves a specific node (folder or file) in the file explorer by its path.
     * This endpoint is used for navigation through the folder hierarchy.</p>
     * 
     * <h3>Path Format:</h3>
     * <p>Paths follow the pattern: /{year}/{semester}/{professorId}/{courseCode}/{documentType}</p>
     * <p>Example: /2024-2025/FIRST/5/CS101/SYLLABUS</p>
     * 
     * <h3>Example Request:</h3>
     * <pre>GET /api/professor/file-explorer/node?path=/2024-2025/FIRST/5/CS101</pre>
     * 
     * <h3>Example Response:</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "File explorer node retrieved successfully",
     *   "data": {
     *     "path": "/2024-2025/FIRST/5/CS101",
     *     "name": "CS101 - Introduction to Computer Science",
     *     "type": "COURSE",
     *     "canRead": true,
     *     "canWrite": true,
     *     "canDelete": false,
     *     "children": [
     *       {
     *         "path": "/2024-2025/FIRST/5/CS101/SYLLABUS",
     *         "name": "SYLLABUS",
     *         "type": "DOCUMENT_TYPE",
     *         "canRead": true,
     *         "canWrite": true,
     *         "children": [
     *           {
     *             "path": "/2024-2025/FIRST/5/CS101/SYLLABUS/syllabus.pdf",
     *             "name": "syllabus.pdf",
     *             "type": "FILE",
     *             "entityId": 100,
     *             "metadata": {
     *               "fileSize": 524288,
     *               "fileType": "application/pdf",
     *               "uploadedAt": "2024-11-15T10:30:00"
     *             },
     *             "canRead": true,
     *             "canWrite": false,
     *             "canDelete": true
     *           }
     *         ]
     *       }
     *     ]
     *   }
     * }
     * </pre>
     * 
     * <h3>Error Responses:</h3>
     * <ul>
     *   <li>403 FORBIDDEN - Professor doesn't have access to this path</li>
     *   <li>404 NOT FOUND - Path not found</li>
     * </ul>
     * 
     * @param path The path to the node (URL-encoded)
     * @return ResponseEntity containing ApiResponse with the FileExplorerNode
     * @throws IllegalArgumentException if professor doesn't have access to path
     */
    @GetMapping("/file-explorer/node")
    public ResponseEntity<ApiResponse<com.alqude.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode>> getFileExplorerNode(
            @RequestParam String path) {
        
        log.info("Professor fetching file explorer node at path: {}", path);
        var currentUser = authService.getCurrentUser();
        
        com.alqude.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode node = 
                fileExplorerService.getNode(path, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success("File explorer node retrieved successfully", node));
    }
    
    /**
     * Download a file by ID
     * 
     * <p>Downloads a file from the system. Professors can download their own files or
     * files from other professors in the same department (read-only access).</p>
     * 
     * <h3>Authorization:</h3>
     * <p>Professor must be in the same department as the file owner. The FileService
     * performs permission checks before allowing download.</p>
     * 
     * <h3>Example Request:</h3>
     * <pre>GET /api/professor/files/100/download</pre>
     * 
     * <h3>Response:</h3>
     * <p>Returns the file as an octet-stream with Content-Disposition header set to
     * "attachment" to trigger browser download.</p>
     * <pre>
     * HTTP/1.1 200 OK
     * Content-Type: application/octet-stream
     * Content-Disposition: attachment; filename="syllabus.pdf"
     * Content-Length: 524288
     * 
     * [binary file data]
     * </pre>
     * 
     * <h3>Error Responses:</h3>
     * <ul>
     *   <li>403 FORBIDDEN - Professor doesn't have permission to access this file</li>
     *   <li>404 NOT FOUND - File not found</li>
     *   <li>500 INTERNAL SERVER ERROR - File read error</li>
     * </ul>
     * 
     * @param fileId The ID of the file to download
     * @return ResponseEntity containing the file as a Resource
     * @throws IOException if file cannot be read
     * @throws com.alqude.edu.ArchiveSystem.exception.UnauthorizedOperationException if professor doesn't have access
     */
    @GetMapping("/files/{fileId}/download")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) throws IOException {
        
        var currentUser = authService.getCurrentUser();
        log.info("Professor {} (ID: {}) attempting to download file ID: {}", 
                currentUser.getEmail(), currentUser.getId(), fileId);
        
        try {
            // Check if professor has read access using FileService permission check
            if (!fileService.canUserReadFile(fileId, currentUser)) {
                log.warn("AUTHORIZATION DENIED: Professor {} (ID: {}) attempted to access file {} without permission", 
                        currentUser.getEmail(), currentUser.getId(), fileId);
                throw new com.alqude.edu.ArchiveSystem.exception.UnauthorizedOperationException(
                        "You do not have permission to access this file");
            }
            
            // Get the file
            com.alqude.edu.ArchiveSystem.entity.UploadedFile uploadedFile = fileService.getFile(fileId);
            
            log.debug("File details - ID: {}, Name: {}, Size: {} bytes, Type: {}", 
                    fileId, uploadedFile.getOriginalFilename(), uploadedFile.getFileSize(), uploadedFile.getFileType());
            
            // Load file as resource
            Resource resource = fileService.loadFileAsResource(uploadedFile.getFileUrl());
            
            log.info("Successfully served file {} ({} bytes) to professor {}", 
                    uploadedFile.getOriginalFilename(), uploadedFile.getFileSize(), currentUser.getId());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + uploadedFile.getOriginalFilename() + "\"")
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_OCTET_STREAM))
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to download file {} for professor {}: {}", 
                    fileId, currentUser.getId(), e.getMessage(), e);
            throw e;
        }
    }
    
    // ========== Legacy Document Request Management ==========
    
    @GetMapping("/document-requests")
    public ResponseEntity<ApiResponse<Page<DocumentRequestResponse>>> getMyDocumentRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        var currentUser = authService.getCurrentUser();
        Page<DocumentRequestResponse> requests = documentRequestService.getDocumentRequestsByProfessor(currentUser.getId(), pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Document requests retrieved successfully", requests));
    }
    
    @GetMapping("/document-requests/all")
    public ResponseEntity<ApiResponse<List<DocumentRequestResponse>>> getAllMyDocumentRequests() {
        var currentUser = authService.getCurrentUser();
        List<DocumentRequestResponse> requests = documentRequestService.getDocumentRequestsByProfessor(currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("All document requests retrieved successfully", requests));
    }
    
    @GetMapping("/document-requests/{requestId}")
    public ResponseEntity<ApiResponse<DocumentRequestResponse>> getDocumentRequestById(@PathVariable Long requestId) {
        DocumentRequestResponse request = documentRequestService.getDocumentRequestById(requestId);
        return ResponseEntity.ok(ApiResponse.success("Document request retrieved successfully", request));
    }
    
    @GetMapping("/document-requests/pending-count")
    public ResponseEntity<ApiResponse<Long>> getPendingRequestsCount() {
        var currentUser = authService.getCurrentUser();
        long count = documentRequestService.getPendingRequestsCountByProfessor(currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("Pending requests count retrieved successfully", count));
    }

    // ========== Notification Endpoints ==========
    
    /**
     * Get all notifications for the authenticated professor
     * 
     * <p>Retrieves all notifications for the current professor, including both seen and unseen
     * notifications. Notifications are sorted by creation date (newest first).</p>
     * 
     * <h3>Notification Types:</h3>
     * <ul>
     *   <li>Document request notifications</li>
     *   <li>Deadline reminders</li>
     *   <li>System announcements</li>
     * </ul>
     * 
     * <h3>Example Request:</h3>
     * <pre>GET /api/professor/notifications</pre>
     * 
     * <h3>Example Response:</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Notifications retrieved successfully",
     *   "data": [
     *     {
     *       "id": 1,
     *       "message": "New document request for CS101 - SYLLABUS",
     *       "type": "DOCUMENT_REQUEST",
     *       "isSeen": false,
     *       "createdAt": "2024-11-19T10:00:00",
     *       "relatedEntityId": 1,
     *       "relatedEntityType": "DOCUMENT_REQUEST"
     *     }
     *   ]
     * }
     * </pre>
     * 
     * @return ResponseEntity containing ApiResponse with list of notifications
     */
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications() {
        List<NotificationResponse> notifications = notificationService.getCurrentUserNotifications();
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }

    /**
     * Mark a notification as seen
     * 
     * <p>Marks a specific notification as seen/read. This updates the notification's
     * isSeen flag and removes it from the unseen count in the UI badge.</p>
     * 
     * <h3>Example Request:</h3>
     * <pre>PUT /api/professor/notifications/1/seen</pre>
     * 
     * <h3>Example Response:</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Notification marked as read",
     *   "data": "OK"
     * }
     * </pre>
     * 
     * <h3>Error Responses:</h3>
     * <ul>
     *   <li>404 NOT FOUND - Notification not found</li>
     * </ul>
     * 
     * @param notificationId The ID of the notification to mark as seen
     * @return ResponseEntity containing ApiResponse with success message
     */
    @PutMapping("/notifications/{notificationId}/seen")
    public ResponseEntity<ApiResponse<String>> markNotificationAsSeen(@PathVariable Long notificationId) {
        notificationService.markNotificationAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", "OK"));
    }
    
    // File Upload Management
    
    @PostMapping("/document-requests/{requestId}/upload")
    public ResponseEntity<ApiResponse<SubmittedDocumentResponse>> uploadDocument(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Professor uploading document for request id: {}", requestId);
        SubmittedDocument submittedDocument = fileUploadService.uploadDocument(requestId, file);
        SubmittedDocumentResponse response = mapToDto(submittedDocument);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", response));
    }

    @PostMapping("/document-requests/{requestId}/submit")
    public ResponseEntity<ApiResponse<SubmittedDocumentResponse>> submitDocument(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Professor submitting document for request id: {} via legacy endpoint", requestId);
        return uploadDocument(requestId, file);
    }
    
    @PutMapping("/document-requests/{requestId}/replace")
    public ResponseEntity<ApiResponse<SubmittedDocumentResponse>> replaceDocument(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        log.info("Professor replacing document for request id: {}", requestId);
        
        // The upload service handles both new uploads and replacements
        SubmittedDocument submittedDocument = fileUploadService.uploadDocument(requestId, file);
        SubmittedDocumentResponse response = mapToDto(submittedDocument);
        return ResponseEntity.ok(ApiResponse.success("Document replaced successfully", response));
    }
    
    // ========== Multi-File Upload Endpoints ==========
    
    /**
     * Upload multiple files for a document request
     */
    @PostMapping("/document-requests/{requestId}/upload-multiple")
    public ResponseEntity<ApiResponse<SubmittedDocumentResponse>> uploadMultipleDocuments(
            @PathVariable @NonNull Long requestId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "notes", required = false) String notes) throws IOException {
        
        log.info("Professor uploading {} files for request id: {}", files.length, requestId);
        
        List<MultipartFile> fileList = Arrays.asList(files);
        SubmittedDocument submittedDocument = multiFileUploadService.uploadMultipleDocuments(requestId, fileList, notes);
        SubmittedDocumentResponse response = mapToDtoWithAttachments(submittedDocument);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        String.format("Successfully uploaded %d file(s)", files.length), 
                        response
                ));
    }
    
    /**
     * Add additional files to existing submission
     */
    @PostMapping("/document-requests/{requestId}/add-files")
    public ResponseEntity<ApiResponse<SubmittedDocumentResponse>> addFilesToSubmission(
            @PathVariable Long requestId,
            @RequestParam("files") MultipartFile[] files) throws IOException {
        
        log.info("Professor adding {} files to request id: {}", files.length, requestId);
        
        List<MultipartFile> fileList = Arrays.asList(files);
        SubmittedDocument submittedDocument = multiFileUploadService.addFilesToSubmission(requestId, fileList);
        SubmittedDocumentResponse response = mapToDtoWithAttachments(submittedDocument);
        
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Successfully added %d file(s)", files.length),
                response
        ));
    }
    
    /**
     * Get all file attachments for a request
     */
    @GetMapping("/document-requests/{requestId}/file-attachments")
    public ResponseEntity<ApiResponse<List<FileAttachmentResponse>>> getFileAttachments(
            @PathVariable Long requestId) {
        
        log.info("Getting file attachments for request id: {}", requestId);
        List<FileAttachmentResponse> attachments = multiFileUploadService.getFileAttachments(requestId);
        
        return ResponseEntity.ok(ApiResponse.success("File attachments retrieved successfully", attachments));
    }
    
    /**
     * Delete a specific file attachment
     */
    @DeleteMapping("/file-attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<String>> deleteFileAttachment(@PathVariable @NonNull Long attachmentId) throws IOException {
        
        log.info("Deleting file attachment id: {}", attachmentId);
        multiFileUploadService.deleteFileAttachment(attachmentId);
        
        return ResponseEntity.ok(ApiResponse.success("File attachment deleted successfully", "Deleted"));
    }
    
    /**
     * Download a specific file attachment
     */
    @GetMapping("/file-attachments/{attachmentId}/download")
    public ResponseEntity<ByteArrayResource> downloadFileAttachment(@PathVariable @NonNull Long attachmentId) throws IOException {
        
        log.info("Downloading file attachment id: {}", attachmentId);
        byte[] fileData = multiFileUploadService.downloadFileAttachment(attachmentId);
        
        if (fileData == null) {
            throw new IOException("File data not found for attachment: " + attachmentId);
        }
        
        com.alqude.edu.ArchiveSystem.entity.FileAttachment attachment = fileAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("File attachment not found"));
        
        ByteArrayResource resource = new ByteArrayResource(fileData);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + attachment.getOriginalFilename() + "\"")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_OCTET_STREAM))
                .contentLength(fileData.length)
                .body(resource);
    }
    
    /**
     * Reorder file attachments
     */
    @PutMapping("/submitted-documents/{submittedDocumentId}/reorder-files")
    public ResponseEntity<ApiResponse<String>> reorderFileAttachments(
            @PathVariable Long submittedDocumentId,
            @RequestBody List<Long> attachmentIdsInOrder) {
        
        log.info("Reordering files for submission id: {}", submittedDocumentId);
        multiFileUploadService.reorderFileAttachments(submittedDocumentId, attachmentIdsInOrder);
        
        return ResponseEntity.ok(ApiResponse.success("Files reordered successfully", "OK"));
    }
    
    @GetMapping("/document-requests/{requestId}/submitted-document")
    public ResponseEntity<ApiResponse<SubmittedDocumentResponse>> getSubmittedDocument(@PathVariable Long requestId) {
        SubmittedDocument submittedDocument = fileUploadService.getSubmittedDocument(requestId);
        SubmittedDocumentResponse response = mapToDto(submittedDocument);
        return ResponseEntity.ok(ApiResponse.success("Submitted document retrieved successfully", response));
    }
    
    @GetMapping("/submitted-documents")
    public ResponseEntity<ApiResponse<List<SubmittedDocumentResponse>>> getMySubmittedDocuments() {
        var currentUser = authService.getCurrentUser();
        List<SubmittedDocument> submittedDocuments = fileUploadService.getSubmittedDocumentsByProfessor(currentUser.getId());
        List<SubmittedDocumentResponse> responses = submittedDocuments.stream()
                .map(this::mapToDto)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Submitted documents retrieved successfully", responses));
    }
    
    @GetMapping("/submitted-documents/{documentId}/download")
    public ResponseEntity<ByteArrayResource> downloadDocument(@PathVariable Long documentId) throws IOException {
        byte[] fileData = fileUploadService.downloadDocument(documentId);
        
        if (fileData == null) {
            throw new IllegalStateException("File data is null for document id: " + documentId);
        }
        
        // Get document info for proper filename and content type
        // For now, we'll use a generic approach
        ByteArrayResource resource = new ByteArrayResource(fileData);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document_" + documentId + "\"")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_OCTET_STREAM))
                .contentLength(fileData.length)
                .body(resource);
    }
    
    @DeleteMapping("/submitted-documents/{documentId}")
    public ResponseEntity<ApiResponse<String>> deleteSubmittedDocument(@PathVariable Long documentId) throws IOException {
        log.info("Professor deleting submitted document with id: {}", documentId);
        
        fileUploadService.deleteSubmittedDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", "Document removed"));
    }
    
    /**
     * Helper method to map SubmittedDocument entity to DTO
     * This prevents Jackson serialization errors with Hibernate lazy-loaded proxies
     * by only exposing necessary fields without triggering proxy initialization
     */
    private SubmittedDocumentResponse mapToDto(SubmittedDocument document) {
        return SubmittedDocumentResponse.builder()
                .id(document.getId())
                .requestId(document.getDocumentRequest().getId())
                .originalFilename(document.getOriginalFilename())
                .fileName(document.getOriginalFilename())
                .fileUrl(document.getFileUrl())
                .fileSize(document.getFileSize())
                .fileType(document.getFileType())
                .fileCount(document.getFileCount())
                .totalFileSize(document.getTotalFileSize())
                .notes(document.getNotes())
                .professorId(document.getProfessor().getId())
                .professorName(document.getProfessor().getFirstName() + " " + document.getProfessor().getLastName())
                .professorEmail(document.getProfessor().getEmail())
                .submittedAt(document.getSubmittedAt())
                .isLateSubmission(document.getIsLateSubmission())
                .submittedLate(document.getIsLateSubmission())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
    
    /**
     * Helper method to map SubmittedDocument with file attachments
     */
    private SubmittedDocumentResponse mapToDtoWithAttachments(SubmittedDocument document) {
        List<FileAttachmentResponse> attachments = fileAttachmentRepository
                .findBySubmittedDocumentIdOrderByFileOrderAsc(document.getId())
                .stream()
                .map(attachment -> FileAttachmentResponse.builder()
                        .id(attachment.getId())
                        .originalFilename(attachment.getOriginalFilename())
                        .fileName(attachment.getOriginalFilename())
                        .fileUrl(attachment.getFileUrl())
                        .fileSize(attachment.getFileSize())
                        .fileType(attachment.getFileType())
                        .fileOrder(attachment.getFileOrder())
                        .description(attachment.getDescription())
                        .createdAt(attachment.getCreatedAt())
                        .updatedAt(attachment.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
        
        return SubmittedDocumentResponse.builder()
                .id(document.getId())
                .requestId(document.getDocumentRequest().getId())
                .originalFilename(document.getOriginalFilename())
                .fileName(document.getOriginalFilename())
                .fileUrl(document.getFileUrl())
                .fileSize(document.getFileSize())
                .fileType(document.getFileType())
                .fileCount(document.getFileCount())
                .totalFileSize(document.getTotalFileSize())
                .notes(document.getNotes())
                .fileAttachments(attachments)
                .professorId(document.getProfessor().getId())
                .professorName(document.getProfessor().getFirstName() + " " + document.getProfessor().getLastName())
                .professorEmail(document.getProfessor().getEmail())
                .submittedAt(document.getSubmittedAt())
                .isLateSubmission(document.getIsLateSubmission())
                .submittedLate(document.getIsLateSubmission())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
