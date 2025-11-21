package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.entity.Folder;
import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.UploadedFile;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.exception.FileStorageException;
import com.alqude.edu.ArchiveSystem.exception.FileValidationException;
import com.alqude.edu.ArchiveSystem.exception.FolderNotFoundException;
import com.alqude.edu.ArchiveSystem.exception.UnauthorizedException;
import com.alqude.edu.ArchiveSystem.repository.FolderRepository;
import com.alqude.edu.ArchiveSystem.repository.UploadedFileRepository;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of FolderFileUploadService for handling file uploads to folder-based storage.
 * 
 * This service provides complete file upload functionality including:
 * - File validation (size, type)
 * - Filename sanitization and duplicate handling
 * - Authorization checks
 * - Physical file storage
 * - Database persistence
 * 
 * @since 2.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FolderFileUploadServiceImpl implements FolderFileUploadService {
    
    private final FolderRepository folderRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final UserRepository userRepository;
    private final FolderService folderService;
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    @Value("${file.max-size:52428800}") // 50MB default
    private long maxFileSize;
    
    @Value("${file.allowed-types:pdf,doc,docx,ppt,pptx,xls,xlsx,jpg,jpeg,png,gif}")
    private String allowedTypes;
    
    @Override
    @Transactional
    public List<UploadedFile> uploadFiles(MultipartFile[] files, Long folderId, 
                                         String folderPath, String notes, Long uploaderId) {
        log.info("=== SERVICE: UPLOAD FILES CALLED ===");
        log.info("File count: {}", files != null ? files.length : 0);
        log.info("Folder ID: {}", folderId);
        log.info("Folder Path: {}", folderPath);
        log.info("Uploader ID: {}", uploaderId);
        log.info("Notes: {}", notes != null ? notes.substring(0, Math.min(notes.length(), 50)) : "null");
        
        // 1. Validate inputs
        if (files == null || files.length == 0) {
            log.error("Validation failed: No files provided");
            throw new IllegalArgumentException("No files provided");
        }
        log.info("✓ Files provided: {}", files.length);
        
        // 2. Resolve folder ID from path if needed
        final Long resolvedFolderId;
        if (folderId == null && folderPath != null) {
            log.info("Folder ID not provided, creating/retrieving from path: {}", folderPath);
            Folder folderFromPath = folderService.getOrCreateFolderByPath(folderPath, uploaderId);
            resolvedFolderId = folderFromPath.getId();
            log.info("✓ Folder resolved with ID: {}", resolvedFolderId);
        } else if (folderId != null) {
            resolvedFolderId = folderId;
        } else {
            log.error("Validation failed: Neither folderId nor folderPath provided");
            throw new IllegalArgumentException("Either folderId or folderPath must be provided");
        }
        
        // 3. Get folder and validate existence
        Folder folder = folderRepository.findById(resolvedFolderId)
            .orElseThrow(() -> FolderNotFoundException.byId(resolvedFolderId));
        
        log.debug("Found folder: {} (path: {})", folder.getName(), folder.getPath());
        
        // 4. Get uploader and validate authorization
        User uploader = userRepository.findById(uploaderId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + uploaderId));
        
        if (!canUploadToFolder(folder, uploader)) {
            log.warn("Unauthorized upload attempt by user {} to folder {}", uploaderId, resolvedFolderId);
            throw UnauthorizedException.uploadNotAuthorized(uploaderId, resolvedFolderId);
        }
        
        log.debug("User {} authorized to upload to folder {}", uploader.getEmail(), folder.getName());
        
        // 5. Validate all files first
        for (MultipartFile file : files) {
            validateFile(file);
        }
        
        log.debug("All files validated successfully");
        
        // 5. Prepare physical directory
        Path targetDir = Paths.get(uploadDir, folder.getPath());
        try {
            Files.createDirectories(targetDir);
            log.debug("Created/verified directory: {}", targetDir);
        } catch (IOException e) {
            log.error("Failed to create directory: {}", targetDir, e);
            throw FileStorageException.directoryCreationFailed(targetDir.toString(), e);
        }
        
        // 6. Upload files and create entities
        List<UploadedFile> uploadedFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                // Generate safe filename
                String safeFilename = generateSafeFilename(file.getOriginalFilename(), targetDir);
                Path targetPath = targetDir.resolve(safeFilename);
                
                log.debug("Uploading file: {} -> {}", file.getOriginalFilename(), safeFilename);
                
                // Save file to disk
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Create database entity
                UploadedFile uploadedFile = UploadedFile.builder()
                    .folder(folder)
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(safeFilename)
                    .fileUrl(folder.getPath() + "/" + safeFilename)
                    .fileSize(file.getSize())
                    .fileType(file.getContentType())
                    .uploader(uploader)
                    .notes(notes)
                    .build();
                
                uploadedFile = uploadedFileRepository.save(uploadedFile);
                uploadedFiles.add(uploadedFile);
                
                log.info("Successfully uploaded file: {} to {} (ID: {})", 
                         safeFilename, folder.getPath(), uploadedFile.getId());
                
            } catch (IOException e) {
                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                throw FileStorageException.fileWriteFailed(file.getOriginalFilename(), e);
            }
        }
        
        log.info("✓ Upload complete: {} files uploaded to folder {}", uploadedFiles.size(), folderId);
        log.info("=== SERVICE: UPLOAD FILES COMPLETE ===");
        return uploadedFiles;
    }
    
    @Override
    public void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw FileValidationException.fileEmpty();
        }
        
        // Check file size
        if (file.getSize() > maxFileSize) {
            throw FileValidationException.fileTooLarge(
                file.getOriginalFilename(), file.getSize(), maxFileSize);
        }
        
        // Check file type
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw FileValidationException.invalidFilename(filename);
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        List<String> allowedExtensions = Arrays.asList(allowedTypes.split(","));
        
        if (!allowedExtensions.contains(extension)) {
            throw FileValidationException.invalidFileType(filename, extension, allowedTypes);
        }
    }
    
    @Override
    public String generateSafeFilename(String originalFilename, Path targetPath) {
        // Sanitize filename - replace special characters with underscore
        String sanitized = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Handle duplicates by appending number
        Path targetFile = targetPath.resolve(sanitized);
        if (!Files.exists(targetFile)) {
            return sanitized;
        }
        
        // File exists, append number
        String nameWithoutExt = getFilenameWithoutExtension(sanitized);
        String extension = getFileExtension(sanitized);
        
        int counter = 1;
        String newFilename;
        do {
            newFilename = nameWithoutExt + "(" + counter + ")." + extension;
            counter++;
        } while (Files.exists(targetPath.resolve(newFilename)));
        
        log.debug("Generated safe filename: {} -> {}", originalFilename, newFilename);
        return newFilename;
    }
    
    @Override
    public boolean canUploadToFolder(Folder folder, User user) {
        // Admins and Deans can upload anywhere
        if (user.getRole() == Role.ROLE_DEANSHIP) {
            return true;
        }
        
        // Professors can only upload to their own folders
        if (user.getRole() == Role.ROLE_PROFESSOR) {
            return folder.getOwner() != null && folder.getOwner().getId().equals(user.getId());
        }
        
        // HOD role - treat similar to professors (can only upload to own folders)
        if (user.getRole() == Role.ROLE_HOD) {
            return folder.getOwner() != null && folder.getOwner().getId().equals(user.getId());
        }
        
        return false;
    }
    
    /**
     * Extract file extension from filename.
     * 
     * @param filename the filename
     * @return the file extension (without dot), or empty string if no extension
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
    
    /**
     * Get filename without extension.
     * 
     * @param filename the filename
     * @return the filename without extension
     */
    private String getFilenameWithoutExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }
}
