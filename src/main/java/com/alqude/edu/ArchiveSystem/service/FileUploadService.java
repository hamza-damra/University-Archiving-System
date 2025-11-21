package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.entity.DocumentRequest;
import com.alqude.edu.ArchiveSystem.entity.SubmittedDocument;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.repository.DocumentRequestRepository;
import com.alqude.edu.ArchiveSystem.repository.SubmittedDocumentRepository;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * File upload service for legacy request-based document system.
 * 
 * NOTE: This service uses legacy entities (DocumentRequest, SubmittedDocument)
 * for backward compatibility with the old request-based system.
 * Deprecation warnings are suppressed as this service maintains compatibility
 * during the transition period.
 * 
 * @deprecated This service will be replaced by FileService in the new
 *             semester-based system
 */
@Deprecated(since = "2.0", forRemoval = false)
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileUploadService {

    private final DocumentRequestRepository documentRequestRepository;
    private final SubmittedDocumentRepository submittedDocumentRepository;
    private final UserRepository userRepository;

    @Value("${file.upload.directory}")
    private String uploadDirectory;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "txt", "zip", "rar", "jpg", "jpeg", "png", "gif");

    @Deprecated
    public SubmittedDocument uploadDocument(Long requestId, MultipartFile file) throws IOException {
        log.info("Uploading document for request id: {}", requestId);

        // Validate request
        if (requestId == null) {
            throw new IllegalArgumentException("Request ID cannot be null");
        }
        DocumentRequest documentRequest = documentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Document request not found with id: " + requestId));

        User currentUser = getCurrentUser();

        // Check if current user is the assigned professor
        if (!documentRequest.getProfessor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not authorized to upload document for this request");
        }

        // Validate file
        validateFile(file, documentRequest);

        // Check if document already exists (for replacement)
        SubmittedDocument existingDocument = submittedDocumentRepository
                .findByDocumentRequestId(requestId).orElse(null);

        // Save file
        String fileName = saveFile(file, requestId);

        // Create or update submitted document
        SubmittedDocument submittedDocument;
        if (existingDocument != null) {
            // Delete old file
            deleteFile(existingDocument.getFileUrl());
            // Update existing document
            submittedDocument = existingDocument;
            submittedDocument.setFileUrl(fileName);
            submittedDocument.setOriginalFilename(file.getOriginalFilename());
            submittedDocument.setFileSize(file.getSize());
            submittedDocument.setFileType(file.getContentType());
            submittedDocument.setSubmittedAt(LocalDateTime.now());
            submittedDocument.setIsLateSubmission(LocalDateTime.now().isAfter(documentRequest.getDeadline()));
        } else {
            // Create new document
            submittedDocument = new SubmittedDocument();
            submittedDocument.setDocumentRequest(documentRequest);
            submittedDocument.setProfessor(currentUser);
            submittedDocument.setFileUrl(fileName);
            submittedDocument.setOriginalFilename(file.getOriginalFilename());
            submittedDocument.setFileSize(file.getSize());
            submittedDocument.setFileType(file.getContentType());
            submittedDocument.setSubmittedAt(LocalDateTime.now());
            submittedDocument.setIsLateSubmission(LocalDateTime.now().isAfter(documentRequest.getDeadline()));
        }

        SubmittedDocument savedDocument = submittedDocumentRepository.save(submittedDocument);
        log.info("Document uploaded successfully with id: {}", savedDocument.getId());

        return savedDocument;
    }

    @Deprecated
    public SubmittedDocument getSubmittedDocument(Long requestId) {
        return submittedDocumentRepository.findByDocumentRequestId(requestId)
                .orElseThrow(() -> new RuntimeException("No submitted document found for request id: " + requestId));
    }

    @Deprecated
    public List<SubmittedDocument> getSubmittedDocumentsByProfessor(Long professorId) {
        return submittedDocumentRepository.findByProfessorId(professorId);
    }

    @Deprecated
    public List<SubmittedDocument> getSubmittedDocumentsByDepartment(Long departmentId) {
        return submittedDocumentRepository.findByDepartmentId(departmentId);
    }

    @Deprecated
    public byte[] downloadDocument(Long documentId) throws IOException {
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        SubmittedDocument document = submittedDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        Path filePath = Paths.get(uploadDirectory, document.getFileUrl());

        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + document.getFileUrl());
        }

        return Files.readAllBytes(filePath);
    }

    @Deprecated
    public void deleteSubmittedDocument(Long documentId) throws IOException {
        log.info("Deleting submitted document with id: {}", documentId);

        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        SubmittedDocument document = submittedDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        // Delete file from filesystem
        deleteFile(document.getFileUrl());

        // Delete from database
        submittedDocumentRepository.delete(document);

        log.info("Submitted document deleted successfully with id: {}", documentId);
    }

    private void validateFile(MultipartFile file, DocumentRequest documentRequest) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Check file size (50MB limit)
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds 50MB limit");
        }

        // Get file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new RuntimeException("File must have an extension");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        // Check if extension is allowed globally
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("File type not allowed: " + extension);
        }

        List<String> allowedRequestExtensions = getNormalizedRequestExtensions(documentRequest);
        if (!allowedRequestExtensions.isEmpty() && !allowedRequestExtensions.contains(extension)) {
            throw new RuntimeException("File extension " + extension + " is not allowed for this request. Required: " +
                    String.join(", ", allowedRequestExtensions));
        }
    }

    private List<String> getNormalizedRequestExtensions(DocumentRequest documentRequest) {
        List<String> extensions = documentRequest.getRequiredFileExtensions();
        if (extensions == null) {
            return List.of();
        }
        return extensions.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    private String saveFile(MultipartFile file, Long requestId) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File must have a valid filename");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = "req_" + requestId + "_" + UUID.randomUUID().toString() + extension;

        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFilename;
    }

    private void deleteFile(String fileName) throws IOException {
        Path filePath = Paths.get(uploadDirectory, fileName);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("File deleted: {}", fileName);
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }
}
