package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alquds.edu.ArchiveSystem.dto.common.NotificationResponse;
import com.alquds.edu.ArchiveSystem.service.academic.AcademicService;
import com.alquds.edu.ArchiveSystem.service.auth.AuthService;
import com.alquds.edu.ArchiveSystem.service.file.FileExplorerService;
import com.alquds.edu.ArchiveSystem.service.file.FileService;
import com.alquds.edu.ArchiveSystem.service.user.NotificationService;
import com.alquds.edu.ArchiveSystem.service.academic.ProfessorService;
import com.alquds.edu.ArchiveSystem.service.submission.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Professor Controller providing semester-based endpoints for professors.
 * 
 * <h2>Authentication & Authorization</h2>
 * <p>
 * All endpoints in this controller require the user to be authenticated with
 * ROLE_PROFESSOR.
 * </p>
 * <p>
 * Session-based authentication is used with CSRF protection enabled.
 * </p>
 * 
 * <h2>Semester-Based Endpoints</h2>
 * <p>
 * The semester-based endpoints provide a modern interface for professors to:
 * </p>
 * <ul>
 * <li>View assigned courses for a specific semester</li>
 * <li>Upload and manage document submissions</li>
 * <li>Browse files through a hierarchical file explorer</li>
 * <li>View dashboard statistics and notifications</li>
 * </ul>
 * 
 * <h2>Error Responses</h2>
 * <p>
 * All endpoints return a standard ApiResponse wrapper with the following
 * structure:
 * </p>
 * 
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
 * <li>200 OK - Request successful</li>
 * <li>201 CREATED - Resource created successfully</li>
 * <li>400 BAD REQUEST - Invalid request parameters or validation failure</li>
 * <li>401 UNAUTHORIZED - User not authenticated or session expired</li>
 * <li>403 FORBIDDEN - User does not have required role or permissions</li>
 * <li>404 NOT FOUND - Requested resource not found</li>
 * <li>500 INTERNAL SERVER ERROR - Server error occurred</li>
 * </ul>
 * 
 * <h2>File Upload Requirements</h2>
 * <ul>
 * <li>Allowed file types: PDF, ZIP</li>
 * <li>Maximum file count and size limits are defined per document type</li>
 * <li>Files are stored in the pattern:
 * year/semester/professorId/courseCode/documentType</li>
 * </ul>
 * 
 * <h2>Permission Model</h2>
 * <ul>
 * <li>Professors can upload files to their own course folders (write
 * access)</li>
 * <li>Professors can view files from other professors in the same department
 * (read-only)</li>
 * <li>Professors cannot access files from other departments</li>
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
public class ProfessorController {

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
         * @return ResponseEntity containing ApiResponse with list of all academic years
         * @throws RuntimeException if database error occurs
         */
        @GetMapping("/academic-years")
        public ResponseEntity<ApiResponse<List<com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear>>> getAllAcademicYears() {
                log.info("Professor retrieving all academic years");

                try {
                        List<com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear> academicYears = academicService
                                        .getAllAcademicYears();
                        return ResponseEntity.ok(
                                        ApiResponse.success("Academic years retrieved successfully", academicYears));
                } catch (Exception e) {
                        log.error("Error retrieving academic years", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(ApiResponse
                                                        .error("Failed to retrieve academic years: " + e.getMessage()));
                }
        }

        /**
         * Get all semesters for a specific academic year
         * 
         * @param academicYearId The ID of the academic year
         * @return ResponseEntity containing ApiResponse with list of semesters
         * @throws RuntimeException if academic year not found or database error occurs
         */
        @GetMapping("/academic-years/{academicYearId}/semesters")
        public ResponseEntity<ApiResponse<List<com.alquds.edu.ArchiveSystem.entity.academic.Semester>>> getSemestersByYear(
                        @PathVariable Long academicYearId) {
                log.info("Professor retrieving semesters for academic year ID: {}", academicYearId);

                try {
                        List<com.alquds.edu.ArchiveSystem.entity.academic.Semester> semesters = academicService
                                        .getSemestersByYear(academicYearId);
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
         * @param semesterId The ID of the semester to retrieve courses for
         * @return ResponseEntity containing ApiResponse with list of courses and their
         *         submission statuses
         * @throws RuntimeException if semester not found or database error occurs
         */
        @GetMapping("/dashboard/courses")
        public ResponseEntity<ApiResponse<List<com.alquds.edu.ArchiveSystem.dto.professor.CourseAssignmentWithStatus>>> getMyCourses(
                        @RequestParam Long semesterId) {

                var currentUser = authService.getCurrentUser();
                log.info("Professor {} (ID: {}) fetching courses for semester ID: {}",
                                currentUser.getEmail(), currentUser.getId(), semesterId);

                try {
                        List<com.alquds.edu.ArchiveSystem.dto.professor.CourseAssignmentWithStatus> courses = professorService
                                        .getProfessorCoursesWithStatus(currentUser.getId(), semesterId);

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
         * @param semesterId The ID of the semester to retrieve overview for
         * @return ResponseEntity containing ApiResponse with dashboard overview
         *         statistics
         * @throws RuntimeException if semester not found or database error occurs
         */
        @GetMapping("/dashboard/overview")
        public ResponseEntity<ApiResponse<com.alquds.edu.ArchiveSystem.dto.professor.ProfessorDashboardOverview>> getDashboardOverview(
                        @RequestParam Long semesterId) {

                log.info("Professor fetching dashboard overview for semester ID: {}", semesterId);
                var currentUser = authService.getCurrentUser();

                com.alquds.edu.ArchiveSystem.dto.professor.ProfessorDashboardOverview overview = professorService
                                .getProfessorDashboardOverview(currentUser.getId(), semesterId);

                return ResponseEntity.ok(ApiResponse.success("Dashboard overview retrieved successfully", overview));
        }

        // ========== Semester-Based File Upload Endpoints ==========

        /**
         * Upload files for a course assignment and document type
         * 
         * @param courseAssignmentId The ID of the course assignment
         * @param documentType       The type of document being uploaded (SYLLABUS,
         *                           EXAM, ASSIGNMENT, etc.)
         * @param notes              Optional notes about the submission
         * @param files              List of files to upload (multipart/form-data)
         * @return ResponseEntity containing ApiResponse with the created
         *         DocumentSubmission
         * @throws IllegalArgumentException if validation fails
         * @throws IOException              if file storage fails
         */
        @PostMapping("/submissions/upload")
        public ResponseEntity<ApiResponse<com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission>> uploadFiles(
                        @RequestParam Long courseAssignmentId,
                        @RequestParam com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum documentType,
                        @RequestParam(required = false) String notes,
                        @RequestPart("files") List<MultipartFile> files) {

                var currentUser = authService.getCurrentUser();

                log.info("Professor {} (ID: {}) uploading {} files for course assignment ID: {}, document type: {}",
                                currentUser.getEmail(), currentUser.getId(), files.size(), courseAssignmentId,
                                documentType);

                // Log file details
                long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
                log.debug("Upload details - Total size: {} bytes, Files: {}", totalSize,
                                files.stream().map(MultipartFile::getOriginalFilename).toList());

                try {
                        // Call FileService to upload files (it handles validation and submission
                        // creation)
                        fileService.uploadFiles(courseAssignmentId, documentType, files, notes, currentUser.getId());

                        // Get the submission that was created/updated by FileService
                        com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission submission = submissionService
                                        .getSubmissionsByCourse(courseAssignmentId).stream()
                                        .filter(s -> s.getDocumentType() == documentType)
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalStateException(
                                                        "Submission not found after upload"));

                        log.info("Successfully uploaded {} files (total: {} bytes) for professor {} - course assignment ID: {}, submission ID: {}",
                                        files.size(), totalSize, currentUser.getId(), courseAssignmentId,
                                        submission.getId());

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
         * @param submissionId The ID of the submission to replace files for
         * @param notes        Optional notes about the replacement
         * @param files        List of new files to upload
         * @return ResponseEntity containing ApiResponse with the updated
         *         DocumentSubmission
         * @throws IllegalArgumentException if professor doesn't own submission or
         *                                  validation fails
         * @throws IOException              if file operations fail
         */
        @PutMapping("/submissions/{submissionId}/replace")
        @PreAuthorize("hasRole('PROFESSOR') and @securityExpressionService.ownsSubmission(#submissionId)")
        public ResponseEntity<ApiResponse<com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission>> replaceFiles(
                        @PathVariable Long submissionId,
                        @RequestParam(required = false) String notes,
                        @RequestPart("files") List<MultipartFile> files) {

                log.info("Professor replacing files for submission ID: {}", submissionId);

                var currentUser = authService.getCurrentUser();

                // Get the submission and validate professor owns it
                com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission submission = submissionService
                                .getSubmission(submissionId);

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
         * @param semesterId The ID of the semester to retrieve submissions for
         * @return ResponseEntity containing ApiResponse with list of submissions
         * @throws RuntimeException if semester not found or database error occurs
         */
        @GetMapping("/submissions")
        public ResponseEntity<ApiResponse<List<com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission>>> getMySubmissions(
                        @RequestParam Long semesterId) {

                log.info("Professor fetching submissions for semester ID: {}", semesterId);
                var currentUser = authService.getCurrentUser();

                List<com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission> submissions = submissionService
                                .getSubmissionsByProfessor(currentUser.getId(), semesterId);

                return ResponseEntity.ok(ApiResponse.success("Submissions retrieved successfully", submissions));
        }

        /**
         * Get a specific submission by ID
         * 
         * @param submissionId The ID of the submission to retrieve
         * @return ResponseEntity containing ApiResponse with submission details
         * @throws IllegalArgumentException if professor doesn't have access to
         *                                  submission
         */
        @GetMapping("/submissions/{submissionId}")
        @PreAuthorize("hasRole('PROFESSOR')")
        public ResponseEntity<ApiResponse<com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission>> getSubmission(
                        @PathVariable Long submissionId) {

                log.info("Professor fetching submission ID: {}", submissionId);
                var currentUser = authService.getCurrentUser();

                com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission submission = submissionService
                                .getSubmission(submissionId);

                // Validate professor owns the submission or has read access (same department)
                if (!submission.getProfessor().getId().equals(currentUser.getId()) &&
                                !submission.getProfessor().getDepartment().getId()
                                                .equals(currentUser.getDepartment().getId())) {
                        throw new IllegalArgumentException(
                                        "Professor does not have access to submission ID: " + submissionId);
                }

                return ResponseEntity.ok(ApiResponse.success("Submission retrieved successfully", submission));
        }

        // ========== Semester-Based File Explorer Endpoints ==========

        /**
         * Get file explorer root node for a semester
         * 
         * @param academicYearId The ID of the academic year
         * @param semesterId     The ID of the semester
         * @return ResponseEntity containing ApiResponse with the root FileExplorerNode
         * @throws RuntimeException if academic year or semester not found
         */
        @GetMapping("/file-explorer/root")
        public ResponseEntity<ApiResponse<com.alquds.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode>> getFileExplorerRoot(
                        @RequestParam Long academicYearId,
                        @RequestParam Long semesterId) {

                log.info("Professor fetching file explorer root for academic year ID: {}, semester ID: {}",
                                academicYearId, semesterId);
                var currentUser = authService.getCurrentUser();

                com.alquds.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode rootNode = fileExplorerService
                                .getRootNode(academicYearId, semesterId, currentUser);

                return ResponseEntity.ok(ApiResponse.success("File explorer root retrieved successfully", rootNode));
        }

        /**
         * Get a specific node in the file explorer hierarchy
         * 
         * @param path The path to the node (URL-encoded)
         * @return ResponseEntity containing ApiResponse with the FileExplorerNode
         * @throws IllegalArgumentException if professor doesn't have access to path
         */
        @GetMapping("/file-explorer/node")
        public ResponseEntity<ApiResponse<com.alquds.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode>> getFileExplorerNode(
                        @RequestParam String path) {

                log.info("Professor fetching file explorer node at path: {}", path);
                var currentUser = authService.getCurrentUser();

                com.alquds.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode node = fileExplorerService.getNode(path,
                                currentUser);

                return ResponseEntity.ok(ApiResponse.success("File explorer node retrieved successfully", node));
        }

        /**
         * Download a file by ID
         * 
         * @param fileId The ID of the file to download
         * @return ResponseEntity containing the file as a Resource
         * @throws IOException if file cannot be read
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
                                throw new com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException(
                                                "You do not have permission to access this file");
                        }

                        // Get the file
                        com.alquds.edu.ArchiveSystem.entity.file.UploadedFile uploadedFile = fileService.getFile(fileId);

                        log.debug("File details - ID: {}, Name: {}, Size: {} bytes, Type: {}",
                                        fileId, uploadedFile.getOriginalFilename(), uploadedFile.getFileSize(),
                                        uploadedFile.getFileType());

                        // Load file as resource
                        Resource resource = fileService.loadFileAsResource(uploadedFile.getFileUrl());

                        // Build Content-Disposition header with proper filename encoding
                        String originalFilename = uploadedFile.getOriginalFilename();
                        String encodedFilename = java.net.URLEncoder
                                        .encode(originalFilename, java.nio.charset.StandardCharsets.UTF_8)
                                        .replace("+", "%20");

                        String contentDisposition = String.format(
                                        "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                                        originalFilename,
                                        encodedFilename);

                        log.info("Successfully served file {} ({} bytes) to professor {}",
                                        uploadedFile.getOriginalFilename(), uploadedFile.getFileSize(),
                                        currentUser.getId());

                        return ResponseEntity.ok()
                                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_OCTET_STREAM))
                                        .body(resource);
                } catch (Exception e) {
                        log.error("Failed to download file {} for professor {}: {}",
                                        fileId, currentUser.getId(), e.getMessage(), e);
                        throw e;
                }
        }

        // ========== Notification Endpoints ==========

        /**
         * Get all notifications for the authenticated professor
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
         * @param notificationId The ID of the notification to mark as seen
         * @return ResponseEntity containing ApiResponse with success message
         */
        @PutMapping("/notifications/{notificationId}/seen")
        public ResponseEntity<ApiResponse<String>> markNotificationAsSeen(@PathVariable Long notificationId) {
                notificationService.markNotificationAsRead(notificationId);
                return ResponseEntity.ok(ApiResponse.success("Notification marked as read", "OK"));
        }
}
