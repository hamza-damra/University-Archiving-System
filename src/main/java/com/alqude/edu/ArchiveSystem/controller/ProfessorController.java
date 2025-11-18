package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.common.FileAttachmentResponse;
import com.alqude.edu.ArchiveSystem.dto.common.NotificationResponse;
import com.alqude.edu.ArchiveSystem.dto.common.SubmittedDocumentResponse;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestResponse;
import com.alqude.edu.ArchiveSystem.entity.SubmittedDocument;
import com.alqude.edu.ArchiveSystem.repository.FileAttachmentRepository;
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
 * NOTE: This controller includes legacy endpoints for backward compatibility.
 * Legacy endpoints use deprecated services (DocumentRequestService, MultiFileUploadService)
 * to maintain compatibility with existing clients during the transition period.
 * 
 * Deprecation warnings are suppressed for legacy endpoint methods.
 * New development should use the semester-based endpoints.
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
    
    // ========== Semester-Based Dashboard Endpoints ==========
    
    /**
     * Get professor's courses with submission status for a semester
     */
    @GetMapping("/dashboard/courses")
    public ResponseEntity<ApiResponse<List<com.alqude.edu.ArchiveSystem.dto.professor.CourseAssignmentWithStatus>>> getMyCourses(
            @RequestParam Long semesterId) {
        
        log.info("Professor fetching courses for semester ID: {}", semesterId);
        var currentUser = authService.getCurrentUser();
        
        List<com.alqude.edu.ArchiveSystem.dto.professor.CourseAssignmentWithStatus> courses = 
                professorService.getProfessorCoursesWithStatus(currentUser.getId(), semesterId);
        
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", courses));
    }
    
    /**
     * Get professor's dashboard overview for a semester
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
     */
    @PostMapping("/submissions/upload")
    public ResponseEntity<ApiResponse<com.alqude.edu.ArchiveSystem.entity.DocumentSubmission>> uploadFiles(
            @RequestParam Long courseAssignmentId,
            @RequestParam com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum documentType,
            @RequestParam(required = false) String notes,
            @RequestPart("files") List<MultipartFile> files) {
        
        log.info("Professor uploading {} files for course assignment ID: {}, document type: {}", 
                files.size(), courseAssignmentId, documentType);
        
        var currentUser = authService.getCurrentUser();
        
        // Call FileService to upload files (it handles validation and submission creation)
        fileService.uploadFiles(courseAssignmentId, documentType, files, notes, currentUser.getId());
        
        // Get the submission that was created/updated by FileService
        com.alqude.edu.ArchiveSystem.entity.DocumentSubmission submission = 
                submissionService.getSubmissionsByCourse(courseAssignmentId).stream()
                        .filter(s -> s.getDocumentType() == documentType)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Submission not found after upload"));
        
        log.info("Successfully uploaded {} files for course assignment ID: {}", files.size(), courseAssignmentId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Files uploaded successfully", submission));
    }
    
    /**
     * Replace files for an existing submission
     * Security: Professor must own the submission
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
     * Security: Professor must own the submission or be in the same department
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
     * Security: Professor must be in the same department as the file owner
     */
    @GetMapping("/files/{fileId}/download")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) throws IOException {
        
        log.info("Professor downloading file ID: {}", fileId);
        var currentUser = authService.getCurrentUser();
        
        // Get the file
        com.alqude.edu.ArchiveSystem.entity.UploadedFile uploadedFile = fileService.getFile(fileId);
        
        // Check if professor has read access (same department)
        com.alqude.edu.ArchiveSystem.entity.DocumentSubmission submission = uploadedFile.getDocumentSubmission();
        if (!submission.getProfessor().getDepartment().getId().equals(currentUser.getDepartment().getId())) {
            throw new IllegalArgumentException("Professor does not have access to file ID: " + fileId);
        }
        
        // Load file as resource
        Resource resource = fileService.loadFileAsResource(uploadedFile.getFileUrl());
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + uploadedFile.getOriginalFilename() + "\"")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_OCTET_STREAM))
                .body(resource);
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

    // Notifications

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications() {
        List<NotificationResponse> notifications = notificationService.getCurrentUserNotifications();
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }

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
