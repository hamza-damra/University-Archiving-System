package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.fileexplorer.FileMetadataDTO;
import com.alqude.edu.ArchiveSystem.entity.DocumentSubmission;
import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.UploadedFile;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of FilePreviewService.
 * Handles file preview operations with permission validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FilePreviewServiceImpl implements FilePreviewService {
    
    private final UploadedFileRepository uploadedFileRepository;
    private final FileExplorerService fileExplorerService;
    private final OfficeDocumentConverter officeDocumentConverter;
    
    // Supported MIME types for preview
    private static final Set<String> TEXT_TYPES = Set.of(
        "text/plain",
        "text/markdown",
        "text/csv",
        "application/json",
        "application/xml",
        "text/xml"
    );
    
    private static final Set<String> CODE_TYPES = Set.of(
        "text/x-java-source",
        "text/javascript",
        "application/javascript",
        "text/x-python",
        "text/x-c",
        "text/x-c++",
        "text/css",
        "text/html",
        "application/x-sql"
    );
    
    private static final Set<String> PDF_TYPES = Set.of(
        "application/pdf"
    );
    
    private static final Set<String> OFFICE_TYPES = Set.of(
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );
    
    private static final Set<String> IMAGE_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp",
        "image/svg+xml"
    );
    
    // File extension to MIME type mapping
    private static final Map<String, String> EXTENSION_TO_MIME = new HashMap<>();
    static {
        // Text files
        EXTENSION_TO_MIME.put("txt", "text/plain");
        EXTENSION_TO_MIME.put("md", "text/markdown");
        EXTENSION_TO_MIME.put("csv", "text/csv");
        EXTENSION_TO_MIME.put("log", "text/plain");
        
        // Code files
        EXTENSION_TO_MIME.put("java", "text/x-java-source");
        EXTENSION_TO_MIME.put("js", "text/javascript");
        EXTENSION_TO_MIME.put("py", "text/x-python");
        EXTENSION_TO_MIME.put("c", "text/x-c");
        EXTENSION_TO_MIME.put("cpp", "text/x-c++");
        EXTENSION_TO_MIME.put("css", "text/css");
        EXTENSION_TO_MIME.put("html", "text/html");
        EXTENSION_TO_MIME.put("sql", "application/x-sql");
        EXTENSION_TO_MIME.put("json", "application/json");
        EXTENSION_TO_MIME.put("xml", "application/xml");
        
        // PDF
        EXTENSION_TO_MIME.put("pdf", "application/pdf");
        
        // Office documents
        EXTENSION_TO_MIME.put("doc", "application/msword");
        EXTENSION_TO_MIME.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        EXTENSION_TO_MIME.put("xls", "application/vnd.ms-excel");
        EXTENSION_TO_MIME.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        EXTENSION_TO_MIME.put("ppt", "application/vnd.ms-powerpoint");
        EXTENSION_TO_MIME.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        
        // Images
        EXTENSION_TO_MIME.put("jpg", "image/jpeg");
        EXTENSION_TO_MIME.put("jpeg", "image/jpeg");
        EXTENSION_TO_MIME.put("png", "image/png");
        EXTENSION_TO_MIME.put("gif", "image/gif");
        EXTENSION_TO_MIME.put("webp", "image/webp");
        EXTENSION_TO_MIME.put("svg", "image/svg+xml");
    }
    
    @Override
    @Transactional(readOnly = true)
    public FileMetadataDTO getFileMetadata(Long fileId, User currentUser) {
        log.info("Getting file metadata for fileId: {}, user: {}", fileId, currentUser.getEmail());
        
        // Get file from database
        UploadedFile file = uploadedFileRepository.findById(fileId)
            .orElseThrow(() -> new EntityNotFoundException("File not found with id: " + fileId));
        
        // Check permission
        if (!canUserPreviewFile(fileId, currentUser)) {
            log.warn("User {} does not have permission to preview file {}", 
                currentUser.getEmail(), fileId);
            throw new AccessDeniedException("You do not have permission to preview this file");
        }
        
        // Detect MIME type if not set
        String mimeType = file.getFileType();
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = detectMimeType(file.getFileUrl());
        }
        
        // Build metadata DTO
        return FileMetadataDTO.builder()
            .id(file.getId())
            .fileName(file.getOriginalFilename())
            .originalFilename(file.getOriginalFilename())
            .mimeType(mimeType)
            .fileSize(file.getFileSize())
            .uploadDate(file.getCreatedAt())
            .uploaderName(file.getUploader() != null ? file.getUploader().getName() : "Unknown")
            .uploaderEmail(file.getUploader() != null ? file.getUploader().getEmail() : null)
            .departmentName(file.getUploader() != null && file.getUploader().getDepartment() != null 
                ? file.getUploader().getDepartment().getName() : null)
            .previewable(isPreviewable(mimeType))
            .previewType(getPreviewType(mimeType))
            .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getFileContent(Long fileId, User currentUser) {
        log.info("Getting file content for fileId: {}, user: {}", fileId, currentUser.getEmail());
        
        // Get file from database
        UploadedFile file = uploadedFileRepository.findById(fileId)
            .orElseThrow(() -> new EntityNotFoundException("File not found with id: " + fileId));
        
        // Check permission
        if (!canUserPreviewFile(fileId, currentUser)) {
            log.warn("User {} does not have permission to preview file {}", 
                currentUser.getEmail(), fileId);
            throw new AccessDeniedException("You do not have permission to preview this file");
        }
        
        // Read file content
        try {
            Path filePath = Paths.get(file.getFileUrl());
            return Files.readString(filePath);
        } catch (IOException e) {
            log.error("Error reading file content: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read file content: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getPartialFileContent(Long fileId, User currentUser, int maxLines) {
        log.info("Getting partial file content for fileId: {}, user: {}, maxLines: {}", 
            fileId, currentUser.getEmail(), maxLines);
        
        // Get file from database
        UploadedFile file = uploadedFileRepository.findById(fileId)
            .orElseThrow(() -> new EntityNotFoundException("File not found with id: " + fileId));
        
        // Check permission
        if (!canUserPreviewFile(fileId, currentUser)) {
            log.warn("User {} does not have permission to preview file {}", 
                currentUser.getEmail(), fileId);
            throw new AccessDeniedException("You do not have permission to preview this file");
        }
        
        // Read first N lines of file
        try {
            Path filePath = Paths.get(file.getFileUrl());
            
            // Read lines with limit
            java.util.List<String> lines = Files.lines(filePath)
                .limit(maxLines)
                .toList();
            
            return String.join("\n", lines);
        } catch (IOException e) {
            log.error("Error reading partial file content: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read partial file content: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public byte[] getFilePreview(Long fileId, User currentUser) {
        log.info("Getting file preview for fileId: {}, user: {}", fileId, currentUser.getEmail());
        
        // Get file from database
        UploadedFile file = uploadedFileRepository.findById(fileId)
            .orElseThrow(() -> new EntityNotFoundException("File not found with id: " + fileId));
        
        // Check permission
        if (!canUserPreviewFile(fileId, currentUser)) {
            log.warn("User {} does not have permission to preview file {}", 
                currentUser.getEmail(), fileId);
            throw new AccessDeniedException("You do not have permission to preview this file");
        }
        
        // Read file as bytes
        try {
            Path filePath = Paths.get(file.getFileUrl());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Error reading file preview: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read file preview: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isPreviewable(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return false;
        }
        
        return TEXT_TYPES.contains(mimeType) ||
               CODE_TYPES.contains(mimeType) ||
               PDF_TYPES.contains(mimeType) ||
               OFFICE_TYPES.contains(mimeType) ||
               IMAGE_TYPES.contains(mimeType);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean canUserPreviewFile(Long fileId, User user) {
        log.info("Checking preview permission for fileId: {}, user: {} (role: {})", 
            fileId, user.getEmail(), user.getRole());
        
        // Get file from database with eager fetch of relationships for permission checking
        UploadedFile file = uploadedFileRepository.findByIdWithUploaderAndFolder(fileId)
            .orElseThrow(() -> new EntityNotFoundException("File not found with id: " + fileId));
        
        // Deanship can read all files
        if (user.getRole() == Role.ROLE_DEANSHIP) {
            log.debug("Deanship user - granting access");
            return true;
        }
        
        // Check if user is the uploader (professors can always access their own files)
        if (file.getUploader() != null) {
            log.info("File uploader ID: {}, current user ID: {}", 
                file.getUploader().getId(), user.getId());
            if (file.getUploader().getId().equals(user.getId())) {
                log.info("User is the uploader - granting access");
                return true;
            }
        } else {
            log.info("File has no uploader set");
        }
        
        // Get the folder to check permissions
        if (file.getFolder() != null && file.getFolder().getPath() != null) {
            // Use the folder's virtual path for permission check
            String virtualPath = "/" + file.getFolder().getPath();
            log.debug("Checking folder path permission: {}", virtualPath);
            return fileExplorerService.canRead(virtualPath, user);
        }
        
        // If file has an uploader, check department-based access
        if (file.getUploader() != null) {
            User uploader = file.getUploader();
            
            // HOD and Professor can access files from same department
            if (user.getDepartment() != null && uploader.getDepartment() != null) {
                boolean sameDept = user.getDepartment().getId().equals(uploader.getDepartment().getId());
                log.debug("Department check - user dept: {}, uploader dept: {}, same: {}", 
                    user.getDepartment().getId(), uploader.getDepartment().getId(), sameDept);
                return sameDept;
            }
        }
        
        // Fallback: Check if the file's physical path contains the professor's ID
        // This handles legacy files that may not have proper folder/uploader associations
        if (user.getRole() == Role.ROLE_PROFESSOR) {
            String fileUrl = file.getFileUrl();
            if (fileUrl != null) {
                // Check for professorId if set
                if (user.getProfessorId() != null && fileUrl.contains(user.getProfessorId())) {
                    log.info("File path contains professor ID {} - granting access", user.getProfessorId());
                    return true;
                }
                // Check for fallback format "prof_<userId>"
                String fallbackId = "prof_" + user.getId();
                if (fileUrl.contains(fallbackId)) {
                    log.info("File path contains fallback ID {} - granting access", fallbackId);
                    return true;
                }
            }
        }
        
        // Also check via document submission if the file is linked to a course assignment
        // where the current professor is assigned
        if (file.getDocumentSubmission() != null && user.getRole() == Role.ROLE_PROFESSOR) {
            DocumentSubmission submission = file.getDocumentSubmission();
            if (submission.getProfessor() != null && 
                submission.getProfessor().getId().equals(user.getId())) {
                log.info("User is the professor on the document submission - granting access");
                return true;
            }
        }
        
        // HOD can access all files in their department
        if (user.getRole() == Role.ROLE_HOD) {
            log.info("HOD user - granting access to department files");
            return true;
        }
        
        log.warn("No permission criteria matched for fileId: {}, user: {}", fileId, user.getEmail());
        // Default: deny access for files without folder or uploader info
        return false;
    }
    
    @Override
    public String detectMimeType(String filePath) {
        // Extract file extension
        String extension = "";
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            extension = filePath.substring(lastDotIndex + 1).toLowerCase();
        }
        
        // Look up MIME type from extension
        String mimeType = EXTENSION_TO_MIME.get(extension);
        if (mimeType != null) {
            return mimeType;
        }
        
        // Try to detect using Files.probeContentType
        try {
            Path path = Paths.get(filePath);
            String detectedType = Files.probeContentType(path);
            if (detectedType != null) {
                return detectedType;
            }
        } catch (IOException e) {
            log.warn("Failed to probe content type for file: {}", filePath);
        }
        
        // Default to octet-stream
        return "application/octet-stream";
    }
    
    @Override
    public String getPreviewType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return "unsupported";
        }
        
        if (PDF_TYPES.contains(mimeType)) {
            return "pdf";
        } else if (OFFICE_TYPES.contains(mimeType)) {
            return "office";
        } else if (CODE_TYPES.contains(mimeType)) {
            return "code";
        } else if (TEXT_TYPES.contains(mimeType)) {
            return "text";
        } else if (IMAGE_TYPES.contains(mimeType)) {
            return "image";
        } else {
            return "unsupported";
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public byte[] convertOfficeDocumentToHtml(Long fileId, User currentUser) {
        log.info("Converting Office document to HTML for fileId: {}, user: {}", fileId, currentUser.getEmail());
        
        // Get file from database
        UploadedFile file = uploadedFileRepository.findById(fileId)
            .orElseThrow(() -> new EntityNotFoundException("File not found with id: " + fileId));
        
        // Check permission
        if (!canUserPreviewFile(fileId, currentUser)) {
            log.warn("User {} does not have permission to preview file {}", 
                currentUser.getEmail(), fileId);
            throw new AccessDeniedException("You do not have permission to preview this file");
        }
        
        // Detect MIME type if not set
        String mimeType = file.getFileType();
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = detectMimeType(file.getFileUrl());
        }
        
        // Verify it's an Office document
        if (!OFFICE_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("File is not an Office document: " + mimeType);
        }
        
        // Convert to HTML
        try {
            return officeDocumentConverter.convertToHtml(file.getFileUrl(), mimeType);
        } catch (IOException e) {
            log.error("Error converting Office document to HTML: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert Office document: " + e.getMessage(), e);
        }
    }
}
