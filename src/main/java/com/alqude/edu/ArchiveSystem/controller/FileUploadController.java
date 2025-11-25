package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.fileexplorer.UploadedFileDTO;
import com.alqude.edu.ArchiveSystem.entity.Folder;
import com.alqude.edu.ArchiveSystem.entity.UploadedFile;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.exception.FileStorageException;
import com.alqude.edu.ArchiveSystem.exception.FileValidationException;
import com.alqude.edu.ArchiveSystem.exception.FolderNotFoundException;
import com.alqude.edu.ArchiveSystem.exception.UnauthorizedException;
import com.alqude.edu.ArchiveSystem.service.FolderFileUploadService;
import com.alqude.edu.ArchiveSystem.repository.FolderRepository;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import com.alqude.edu.ArchiveSystem.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * File Upload Controller for handling file uploads to folders.
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
 * <h2>File Upload Endpoints</h2>
 * <p>
 * This controller provides endpoints for:
 * </p>
 * <ul>
 * <li>Uploading files to specific folders</li>
 * <li>Validating file types and sizes</li>
 * <li>Managing file storage on the physical file system</li>
 * </ul>
 * 
 * <h2>Permission Model</h2>
 * <ul>
 * <li>Professors can upload files to their own folders (write access)</li>
 * <li>Deanship users can upload files to any folder</li>
 * <li>HOD users can upload files to their own folders</li>
 * </ul>
 * 
 * @author Archive System Team
 * @version 1.0
 * @since 2024-11-21
 */
@RestController
@RequestMapping("/api/professor/files")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('PROFESSOR', 'DEANSHIP', 'HOD')")
public class FileUploadController {

        private final FolderFileUploadService folderFileUploadService;
        private final UserRepository userRepository;
        private final UploadedFileRepository uploadedFileRepository;
        private final FolderRepository folderRepository;

        /**
         * Upload files to a specific folder.
         * 
         * <p>
         * This endpoint accepts multiple files and uploads them to the specified
         * folder.
         * The current authenticated user must have permission to upload to the target
         * folder.
         * </p>
         * 
         * <h3>Request Parameters:</h3>
         * <ul>
         * <li><b>files[]</b> - Array of files to upload (required,
         * multipart/form-data)</li>
         * <li><b>folderId</b> - Target folder ID (required)</li>
         * <li><b>notes</b> - Optional notes about the upload</li>
         * </ul>
         * 
         * <h3>Success Response:</h3>
         * 
         * <pre>
         * {
         *   "success": true,
         *   "message": "Successfully uploaded 3 file(s)",
         *   "data": [
         *     {
         *       "id": 1,
         *       "originalFilename": "lecture.pdf",
         *       "storedFilename": "lecture.pdf",
         *       "fileSize": 1024000,
         *       "fileType": "application/pdf",
         *       "uploadedAt": "2024-11-21T10:30:00",
         *       "notes": "Week 1 lecture notes"
         *     }
         *   ]
         * }
         * </pre>
         * 
         * <h3>Error Responses:</h3>
         * <ul>
         * <li><b>400 Bad Request</b> - File validation failed (invalid type, too large,
         * etc.)</li>
         * <li><b>403 Forbidden</b> - User not authorized to upload to this folder</li>
         * <li><b>404 Not Found</b> - Folder not found</li>
         * <li><b>500 Internal Server Error</b> - File storage error</li>
         * </ul>
         * 
         * @param files       Array of files to upload
         * @param folderId    Target folder ID
         * @param notes       Optional notes about the upload
         * @param userDetails Current authenticated user
         * @return ResponseEntity with ApiResponse containing list of uploaded file DTOs
         */
        @PostMapping("/upload")
        public ResponseEntity<ApiResponse<List<UploadedFileDTO>>> uploadFiles(
                        @RequestParam("files[]") MultipartFile[] files,
                        @RequestParam(value = "folderId", required = false) Long folderId,
                        @RequestParam(value = "folderPath", required = false) String folderPath,
                        @RequestParam(value = "notes", required = false) String notes,
                        @AuthenticationPrincipal UserDetails userDetails) {

                log.info("=== UPLOAD REQUEST RECEIVED ===");
                log.info("User: {}", userDetails != null ? userDetails.getUsername() : "null");
                log.info("Folder ID: {}", folderId);
                log.info("Folder Path: {}", folderPath);
                log.info("File count: {}", files != null ? files.length : 0);
                log.info("Notes: {}", notes != null ? notes.substring(0, Math.min(notes.length(), 50)) : "null");

                if (files != null) {
                        for (int i = 0; i < files.length; i++) {
                                MultipartFile file = files[i];
                                log.info("  File {}: {} ({} bytes, type: {})",
                                                i + 1,
                                                file.getOriginalFilename(),
                                                file.getSize(),
                                                file.getContentType());
                        }
                }

                // Validate at least one is provided
                if (folderId == null && folderPath == null) {
                        log.error("Validation failed: Neither folderId nor folderPath provided");
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error("Either folderId or folderPath must be provided"));
                }

                try {
                        // Get current user from database
                        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "User not found: " + userDetails.getUsername()));

                        log.debug("Uploading files for user ID: {}, role: {}", currentUser.getId(),
                                        currentUser.getRole());

                        // Call service to upload files
                        List<UploadedFile> uploadedFiles = folderFileUploadService.uploadFiles(
                                        files, folderId, folderPath, notes, currentUser.getId());

                        // Convert entities to DTOs
                        List<UploadedFileDTO> fileDTOs = uploadedFiles.stream()
                                        .map(this::convertToDTO)
                                        .toList();

                        log.info("✓ Successfully uploaded {} file(s) to folder ID: {}", fileDTOs.size(), folderId);
                        log.info("=== UPLOAD REQUEST COMPLETE ===");

                        return ResponseEntity.ok(
                                        ApiResponse.success(
                                                        String.format("Successfully uploaded %d file(s)",
                                                                        fileDTOs.size()),
                                                        fileDTOs));

                } catch (FolderNotFoundException e) {
                        // Handle folder not found - return 404
                        log.warn("Folder not found: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.error(e.getMessage()));

                } catch (UnauthorizedException e) {
                        // Handle unauthorized access - return 403
                        log.warn("Unauthorized upload attempt: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(ApiResponse.error(e.getMessage()));

                } catch (FileValidationException e) {
                        // Handle file validation errors - return 400
                        log.warn("File validation failed: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(ApiResponse.error(e.getMessage()));

                } catch (FileStorageException e) {
                        // Handle file storage errors - return 500
                        log.error("File storage error: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(ApiResponse.error("Failed to store file: " + e.getMessage()));

                } catch (Exception e) {
                        // Handle any other unexpected errors - return 500
                        log.error("Unexpected error uploading files to folder {}: {}", folderId, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(ApiResponse.error(
                                                        "An unexpected error occurred while uploading files. Please try again."));
                }
        }

        /**
         * Get list of files in a specific folder.
         * 
         * <p>
         * This endpoint retrieves all files uploaded to the specified folder.
         * The current authenticated user must have permission to view the folder.
         * </p>
         * 
         * <h3>Request Parameters:</h3>
         * <ul>
         * <li><b>folderId</b> - Target folder ID (required)</li>
         * </ul>
         * 
         * <h3>Success Response:</h3>
         * 
         * <pre>
         * {
         *   "success": true,
         *   "message": "Retrieved 5 file(s) from folder",
         *   "data": [
         *     {
         *       "id": 1,
         *       "originalFilename": "lecture.pdf",
         *       "storedFilename": "lecture.pdf",
         *       "fileSize": 1024000,
         *       "fileType": "application/pdf",
         *       "uploadedAt": "2024-11-21T10:30:00",
         *       "notes": "Week 1 lecture notes",
         *       "uploaderName": "Dr. John Smith"
         *     }
         *   ]
         * }
         * </pre>
         * 
         * <h3>Error Responses:</h3>
         * <ul>
         * <li><b>403 Forbidden</b> - User not authorized to view this folder</li>
         * <li><b>404 Not Found</b> - Folder not found</li>
         * <li><b>500 Internal Server Error</b> - Unexpected error</li>
         * </ul>
         * 
         * @param folderId    Target folder ID
         * @param userDetails Current authenticated user
         * @return ResponseEntity with ApiResponse containing list of file DTOs
         */
        @GetMapping
        public ResponseEntity<ApiResponse<List<UploadedFileDTO>>> getFilesByFolder(
                        @RequestParam("folderId") Long folderId,
                        @AuthenticationPrincipal UserDetails userDetails) {

                log.info("Get files request - User: {}, Folder ID: {}", userDetails.getUsername(), folderId);

                try {
                        // Get current user from database
                        if (userDetails == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(ApiResponse.error("User not authenticated"));
                        }
                        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "User not found: " + userDetails.getUsername()));

                        // Get folder and validate it exists
                        Folder folder = folderRepository.findById(java.util.Objects.requireNonNull(folderId))
                                        .orElseThrow(() -> FolderNotFoundException.byId(folderId));

                        // Check authorization - user must have permission to view this folder
                        if (!canViewFolder(folder, currentUser)) {
                                log.warn("Unauthorized folder access attempt by user {} for folder {}",
                                                currentUser.getId(), folderId);
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(ApiResponse.error(
                                                                "You do not have permission to view files in this folder"));
                        }

                        // Get files from repository with uploader data
                        List<UploadedFile> files = uploadedFileRepository.findByFolderIdWithUploader(folderId);

                        // Convert to DTOs
                        List<UploadedFileDTO> fileDTOs = files.stream()
                                        .map(this::convertToDTO)
                                        .toList();

                        log.info("Retrieved {} file(s) from folder ID: {}", fileDTOs.size(), folderId);

                        return ResponseEntity.ok(
                                        ApiResponse.success(
                                                        String.format("Retrieved %d file(s) from folder",
                                                                        fileDTOs.size()),
                                                        fileDTOs));

                } catch (FolderNotFoundException e) {
                        // Handle folder not found - return 404
                        log.warn("Folder not found: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.error(e.getMessage()));

                } catch (Exception e) {
                        // Handle any other unexpected errors - return 500
                        log.error("Unexpected error retrieving files from folder {}: {}", folderId, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(ApiResponse.error(
                                                        "An unexpected error occurred while retrieving files. Please try again."));
                }
        }

        /**
         * Check if user has permission to view files in the specified folder.
         * 
         * Authorization rules:
         * - ADMIN role: Can view any folder
         * - DEANSHIP role: Can view any folder
         * - PROFESSOR role: Can only view their own folders (folder.owner.id ==
         * user.id)
         * - HOD role: Can view their own folders
         * - Other roles: Cannot view
         * 
         * @param folder Target folder
         * @param user   User attempting to view
         * @return true if user is authorized to view the folder, false otherwise
         */
        private boolean canViewFolder(Folder folder, User user) {
                // Admins and Deanship can view any folder
                if (user.getRole().name().equals("ROLE_ADMIN") || user.getRole().name().equals("ROLE_DEANSHIP")) {
                        return true;
                }

                // Professors and HODs can only view their own folders
                if (user.getRole().name().equals("ROLE_PROFESSOR") || user.getRole().name().equals("ROLE_HOD")) {
                        return folder.getOwner() != null && folder.getOwner().getId().equals(user.getId());
                }

                return false;
        }

        /**
         * Convert UploadedFile entity to UploadedFileDTO.
         * 
         * @param file UploadedFile entity
         * @return UploadedFileDTO
         */
        private UploadedFileDTO convertToDTO(UploadedFile file) {
                return UploadedFileDTO.builder()
                                .id(file.getId())
                                .originalFilename(file.getOriginalFilename())
                                .storedFilename(file.getStoredFilename())
                                .fileSize(file.getFileSize())
                                .fileType(file.getFileType())
                                .uploadedAt(file.getCreatedAt())
                                .notes(file.getNotes())
                                .uploaderName(file.getUploader() != null ? file.getUploader().getName() : null)
                                .build();
        }
}
