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
import com.alqude.edu.ArchiveSystem.service.FileUploadService;
import com.alqude.edu.ArchiveSystem.service.MultiFileUploadService;
import com.alqude.edu.ArchiveSystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
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

@RestController
@RequestMapping("/api/professor")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('PROFESSOR')")
public class ProfessorController {
    
    private final DocumentRequestService documentRequestService;
    private final FileUploadService fileUploadService;
    private final MultiFileUploadService multiFileUploadService;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    
    // Document Request Management
    
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
