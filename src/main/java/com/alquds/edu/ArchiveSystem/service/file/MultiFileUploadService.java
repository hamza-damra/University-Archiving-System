package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.service.auth.AuthService;

import com.alquds.edu.ArchiveSystem.entity.submission.SubmittedDocument;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.file.FileAttachment;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentRequest;
import com.alquds.edu.ArchiveSystem.repository.submission.SubmittedDocumentRepository;
import com.alquds.edu.ArchiveSystem.repository.file.FileAttachmentRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentRequestRepository;

import com.alquds.edu.ArchiveSystem.dto.common.FileAttachmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Enhanced service for handling multi-file uploads professionally
 * 
 * NOTE: This service uses legacy entities (DocumentRequest, SubmittedDocument,
 * FileAttachment)
 * for backward compatibility and migration support. Deprecation warnings are
 * suppressed
 * as this is intentional usage of archived code.
 * 
 * @deprecated This service will be replaced by FileService in the new
 *             semester-based system
 */
@Deprecated(since = "2.0", forRemoval = false)
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MultiFileUploadService {

    private final DocumentRequestRepository documentRequestRepository;
    private final SubmittedDocumentRepository submittedDocumentRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final AuthService authService;

    @Value("${file.upload.directory:uploads}")
    private String uploadDirectory;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "txt", "zip", "rar", "jpg", "jpeg", "png", "gif", "csv");

    // Legacy multi-file upload max size per file (100MB) to match new system limits
    private static final long MAX_SINGLE_FILE_SIZE = 100 * 1024 * 1024; // 100MB per file

    /**
     * Upload multiple files for a document request
     */
    @Deprecated
    public synchronized SubmittedDocument uploadMultipleDocuments(@NonNull Long requestId, List<MultipartFile> files,
            String notes) throws IOException {
        log.info("Uploading {} files for request id: {}", files.size(), requestId);

        // Validate request
        DocumentRequest documentRequest = documentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Document request not found with id: " + requestId));

        User currentUser = authService.getCurrentUser();

        // Validate authorization
        if (!documentRequest.getProfessor().getId().equals(currentUser.getId())) {
            throw new SecurityException("You are not authorized to upload documents for this request");
        }

        // Validate file count and total size
        validateMultipleFiles(files, documentRequest);

        // Check if submission already exists (with retry to handle race conditions)
        SubmittedDocument submittedDocument = submittedDocumentRepository
                .findByDocumentRequestId(requestId)
                .orElse(null);

        boolean isNewSubmission = submittedDocument == null;

        if (isNewSubmission) {
            // Create new submission
            try {
                submittedDocument = new SubmittedDocument();
                submittedDocument.setDocumentRequest(documentRequest);
                submittedDocument.setProfessor(currentUser);
                submittedDocument.setSubmittedAt(LocalDateTime.now());
                submittedDocument.setIsLateSubmission(LocalDateTime.now().isAfter(documentRequest.getDeadline()));
                submittedDocument.setNotes(notes);
                // Set temporary placeholder for file_url (will be updated after file upload)
                submittedDocument.setFileUrl("pending");
                submittedDocument.setOriginalFilename("pending");
                submittedDocument.setFileSize(0L);
                submittedDocument.setFileType("pending");
                submittedDocument = submittedDocumentRepository.save(submittedDocument);
                log.debug("Created new submission for request id: {}", requestId);
            } catch (Exception e) {
                // Handle race condition: another request might have created the submission
                log.warn("Failed to create new submission, checking if it was created by concurrent request: {}",
                        e.getMessage());
                submittedDocument = submittedDocumentRepository.findByDocumentRequestId(requestId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Failed to create or retrieve submission for request id: " + requestId));
                isNewSubmission = false;
                log.info("Found existing submission created by concurrent request, will update it instead");
            }
        }

        // Ensure submittedDocument is not null (should never happen due to logic above)
        if (submittedDocument == null) {
            throw new IllegalStateException("Failed to create or retrieve submission for request id: " + requestId);
        }

        if (!isNewSubmission) {
            // Update existing submission
            submittedDocument.setNotes(notes);
            submittedDocument.setSubmittedAt(LocalDateTime.now());
            submittedDocument.setIsLateSubmission(LocalDateTime.now().isAfter(documentRequest.getDeadline()));
            // Delete old file attachments
            deleteAllFileAttachments(submittedDocument.getId());
            log.debug("Updated existing submission for request id: {}", requestId);
        }

        // Save all files
        List<FileAttachment> attachments = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            FileAttachment attachment = saveFileAttachment(file, submittedDocument, i);
            attachments.add(attachment);
        }

        // Update submission metadata
        submittedDocument.setFileCount(attachments.size());
        submittedDocument.setTotalFileSize(
                attachments.stream().mapToLong(a -> a.getFileSize() != null ? a.getFileSize() : 0L).sum());

        // Set first file as primary for backward compatibility
        if (!attachments.isEmpty()) {
            FileAttachment firstFile = attachments.get(0);
            submittedDocument.setFileUrl(firstFile.getFileUrl());
            submittedDocument.setOriginalFilename(firstFile.getOriginalFilename());
            submittedDocument.setFileSize(firstFile.getFileSize());
            submittedDocument.setFileType(firstFile.getFileType());
        }

        submittedDocument = submittedDocumentRepository.save(submittedDocument);
        log.info("Successfully uploaded {} files for submission id: {}", files.size(), submittedDocument.getId());

        return submittedDocument;
    }

    /**
     * Add additional files to existing submission
     */
    @Deprecated
    public SubmittedDocument addFilesToSubmission(Long requestId, List<MultipartFile> files) throws IOException {
        SubmittedDocument submittedDocument = submittedDocumentRepository.findByDocumentRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("No submission found for request id: " + requestId));

        DocumentRequest documentRequest = submittedDocument.getDocumentRequest();

        // Get existing file count
        long existingFileCount = fileAttachmentRepository.countBySubmittedDocumentId(submittedDocument.getId());

        // Validate total file count
        if (existingFileCount + files.size() > documentRequest.getMaxFileCount()) {
            throw new IllegalArgumentException(
                    String.format("Total file count would exceed maximum allowed (%d). Current: %d, Adding: %d",
                            documentRequest.getMaxFileCount(), existingFileCount, files.size()));
        }

        // Validate files
        for (MultipartFile file : files) {
            validateFile(file, documentRequest);
        }

        // Get next file order
        int nextOrder = (int) existingFileCount;

        // Save new files
        for (MultipartFile file : files) {
            saveFileAttachment(file, submittedDocument, nextOrder++);
        }

        // Update metadata
        List<FileAttachment> allAttachments = fileAttachmentRepository
                .findBySubmittedDocumentId(submittedDocument.getId());
        submittedDocument.setFileCount(allAttachments.size());
        submittedDocument.setTotalFileSize(
                allAttachments.stream().mapToLong(a -> a.getFileSize() != null ? a.getFileSize() : 0L).sum());

        return submittedDocumentRepository.save(submittedDocument);
    }

    /**
     * Delete a specific file attachment
     */
    @Deprecated
    public void deleteFileAttachment(@NonNull Long attachmentId) throws IOException {
        FileAttachment attachment = fileAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("File attachment not found with id: " + attachmentId));

        // Delete physical file
        deletePhysicalFile(attachment.getFileUrl());

        // Delete database record
        fileAttachmentRepository.delete(attachment);

        // Update submission metadata
        SubmittedDocument submission = attachment.getSubmittedDocument();
        List<FileAttachment> remainingAttachments = fileAttachmentRepository
                .findBySubmittedDocumentId(submission.getId());

        submission.setFileCount(remainingAttachments.size());
        submission.setTotalFileSize(
                remainingAttachments.stream().mapToLong(a -> a.getFileSize() != null ? a.getFileSize() : 0L).sum());

        submittedDocumentRepository.save(submission);

        log.info("Deleted file attachment id: {} from submission id: {}", attachmentId, submission.getId());
    }

    /**
     * Reorder file attachments
     */
    @Deprecated
    @Transactional
    public void reorderFileAttachments(Long submittedDocumentId, List<Long> attachmentIdsInOrder) {
        List<FileAttachment> attachments = fileAttachmentRepository.findBySubmittedDocumentId(submittedDocumentId);

        Map<Long, FileAttachment> attachmentMap = attachments.stream()
                .collect(Collectors.toMap(FileAttachment::getId, a -> a));

        for (int i = 0; i < attachmentIdsInOrder.size(); i++) {
            Long attachmentId = attachmentIdsInOrder.get(i);
            FileAttachment attachment = attachmentMap.get(attachmentId);
            if (attachment != null) {
                attachment.setFileOrder(i);
            }
        }

        fileAttachmentRepository.saveAll(attachments);
        log.info("Reordered {} file attachments for submission id: {}", attachmentIdsInOrder.size(),
                submittedDocumentId);
    }

    /**
     * Get all file attachments for a submission
     */
    @Deprecated
    public List<FileAttachmentResponse> getFileAttachments(Long requestId) {
        List<FileAttachment> attachments = fileAttachmentRepository.findByDocumentRequestId(requestId);
        return attachments.stream()
                .map(this::mapToFileAttachmentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Download a specific file attachment
     */
    @Deprecated
    public byte[] downloadFileAttachment(@NonNull Long attachmentId) throws IOException {
        FileAttachment attachment = fileAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("File attachment not found with id: " + attachmentId));

        Path filePath = Paths.get(uploadDirectory, attachment.getFileUrl());

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + attachment.getOriginalFilename());
        }

        return Files.readAllBytes(filePath);
    }

    // ========== Private Helper Methods ==========

    private void validateMultipleFiles(List<MultipartFile> files, DocumentRequest documentRequest) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }

        if (files.size() > documentRequest.getMaxFileCount()) {
            throw new IllegalArgumentException(
                    String.format("Too many files. Maximum allowed: %d, Provided: %d",
                            documentRequest.getMaxFileCount(), files.size()));
        }

        long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        long maxTotalSize = documentRequest.getMaxTotalSizeMb() * 1024L * 1024L;

        if (totalSize > maxTotalSize) {
            throw new IllegalArgumentException(
                    String.format("Total file size exceeds maximum allowed (%d MB). Total size: %.2f MB",
                            documentRequest.getMaxTotalSizeMb(), totalSize / (1024.0 * 1024.0)));
        }

        for (MultipartFile file : files) {
            validateFile(file, documentRequest);
        }
    }

    private void validateFile(MultipartFile file, DocumentRequest documentRequest) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty: " + file.getOriginalFilename());
        }

        if (file.getSize() > MAX_SINGLE_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum allowed (10 MB): %s (%.2f MB)",
                            file.getOriginalFilename(), file.getSize() / (1024.0 * 1024.0)));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name is empty");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        List<String> requiredExtensions = documentRequest.getRequiredFileExtensions();

        if (requiredExtensions != null && !requiredExtensions.isEmpty()) {
            if (!requiredExtensions.stream().anyMatch(ext -> ext.equalsIgnoreCase(extension))) {
                throw new IllegalArgumentException(
                        String.format("Invalid file type: %s. Allowed: %s",
                                extension, String.join(", ", requiredExtensions)));
            }
        } else if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    String.format("Unsupported file type: %s", extension));
        }
    }

    private FileAttachment saveFileAttachment(MultipartFile file, SubmittedDocument submittedDocument, int order)
            throws IOException {
        String savedFileName = savePhysicalFile(file, submittedDocument.getDocumentRequest().getId());

        FileAttachment attachment = FileAttachment.builder()
                .submittedDocument(submittedDocument)
                .fileUrl(savedFileName)
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .fileOrder(order)
                .build();

        return fileAttachmentRepository.save(Objects.requireNonNull(attachment));
    }

    private String savePhysicalFile(MultipartFile file, Long requestId) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFileName = String.format("%d_%s_%s.%s",
                requestId,
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                extension);

        // Save file
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.debug("Saved file: {} as {}", originalFilename, uniqueFileName);
        return uniqueFileName;
    }

    private void deletePhysicalFile(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }

        Path filePath = Paths.get(uploadDirectory, fileName);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.debug("Deleted file: {}", fileName);
        }
    }

    private void deleteAllFileAttachments(Long submittedDocumentId) throws IOException {
        List<FileAttachment> attachments = fileAttachmentRepository.findBySubmittedDocumentId(submittedDocumentId);

        for (FileAttachment attachment : attachments) {
            deletePhysicalFile(attachment.getFileUrl());
        }

        fileAttachmentRepository.deleteBySubmittedDocumentId(submittedDocumentId);
        log.info("Deleted {} file attachments for submission id: {}", attachments.size(), submittedDocumentId);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private FileAttachmentResponse mapToFileAttachmentResponse(FileAttachment attachment) {
        return FileAttachmentResponse.builder()
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
                .build();
    }
}
