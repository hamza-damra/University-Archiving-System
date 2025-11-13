package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.common.NotificationResponse;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestResponse;
import com.alqude.edu.ArchiveSystem.entity.SubmittedDocument;
import com.alqude.edu.ArchiveSystem.service.AuthService;
import com.alqude.edu.ArchiveSystem.service.DocumentRequestService;
import com.alqude.edu.ArchiveSystem.service.FileUploadService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/professor")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('PROFESSOR')")
public class ProfessorController {
    
    private final DocumentRequestService documentRequestService;
    private final FileUploadService fileUploadService;
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
    public ResponseEntity<ApiResponse<SubmittedDocument>> uploadDocument(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Professor uploading document for request id: {}", requestId);
        SubmittedDocument submittedDocument = fileUploadService.uploadDocument(requestId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", submittedDocument));
    }

    @PostMapping("/document-requests/{requestId}/submit")
    public ResponseEntity<ApiResponse<SubmittedDocument>> submitDocument(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Professor submitting document for request id: {} via legacy endpoint", requestId);
        return uploadDocument(requestId, file);
    }
    
    @PutMapping("/document-requests/{requestId}/replace")
    public ResponseEntity<ApiResponse<SubmittedDocument>> replaceDocument(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        log.info("Professor replacing document for request id: {}", requestId);
        
        // The upload service handles both new uploads and replacements
        SubmittedDocument submittedDocument = fileUploadService.uploadDocument(requestId, file);
        return ResponseEntity.ok(ApiResponse.success("Document replaced successfully", submittedDocument));
    }
    
    @GetMapping("/document-requests/{requestId}/submitted-document")
    public ResponseEntity<ApiResponse<SubmittedDocument>> getSubmittedDocument(@PathVariable Long requestId) {
        SubmittedDocument submittedDocument = fileUploadService.getSubmittedDocument(requestId);
        return ResponseEntity.ok(ApiResponse.success("Submitted document retrieved successfully", submittedDocument));
    }
    
    @GetMapping("/submitted-documents")
    public ResponseEntity<ApiResponse<List<SubmittedDocument>>> getMySubmittedDocuments() {
        var currentUser = authService.getCurrentUser();
        List<SubmittedDocument> submittedDocuments = fileUploadService.getSubmittedDocumentsByProfessor(currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("Submitted documents retrieved successfully", submittedDocuments));
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
}
