package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.repository.academic.AcademicYearRepository;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.entity.academic.SemesterType;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.repository.academic.SemesterRepository;

import com.alquds.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.BreadcrumbItem;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.CreateFolderRequest;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.CreateFolderResponse;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.DeleteFolderRequest;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.DeleteFolderResponse;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode;


import com.alquds.edu.ArchiveSystem.service.auth.AuthService;
import com.alquds.edu.ArchiveSystem.service.file.FileExplorerService;
import com.alquds.edu.ArchiveSystem.service.file.FileService;
import com.alquds.edu.ArchiveSystem.service.file.FilesystemScanService;
import com.alquds.edu.ArchiveSystem.service.file.FolderFileUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * Shared controller for file explorer functionality across all roles.
 * Provides hierarchical navigation and file access with role-based permissions.
 * 
 * Note: This controller integrates with FilesystemScanService to invalidate
 * directory caches after write operations (upload, create folder, delete).
 */
@RestController
@RequestMapping("/api/file-explorer")
@RequiredArgsConstructor
@Slf4j
public class FileExplorerController {

    private final FileExplorerService fileExplorerService;
    private final FileService fileService;
    private final FilesystemScanService filesystemScanService;
    private final FolderFileUploadService folderFileUploadService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;

    /**
     * Get the root node of the file explorer for a specific academic year and
     * semester.
     * Applies role-based filtering:
     * - Deanship: all professors in semester
     * - HOD: professors in HOD's department only
     * - Professor: all professors in same department
     *
     * @param academicYearId the academic year ID
     * @param semesterId     the semester ID
     * @param authentication the authenticated user
     * @return root node with filtered children and metadata
     */
    @GetMapping("/root")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FileExplorerNode>> getRoot(
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId,
            Authentication authentication) {

        log.info("Fetching file explorer root for academicYearId: {}, semesterId: {}",
                academicYearId, semesterId);

        User currentUser = authService.getCurrentUser();
        FileExplorerNode rootNode = fileExplorerService.getRootNode(academicYearId, semesterId, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Root node retrieved successfully", rootNode));
    }

    /**
     * Get a specific node in the file explorer hierarchy by path.
     * Path format: /year/semester/professor/course/documentType
     * Returns empty children array for empty folders instead of error.
     *
     * @param path           the node path
     * @param authentication the authenticated user
     * @return the node with its children (empty array if no children)
     */
    @GetMapping("/node")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FileExplorerNode>> getNode(
            @RequestParam String path,
            Authentication authentication) {

        log.info("Fetching file explorer node for path: {}", path);

        try {
            User currentUser = authService.getCurrentUser();
            FileExplorerNode node = fileExplorerService.getNode(path, currentUser);

            return ResponseEntity.ok(ApiResponse.success("Node retrieved successfully", node));

        } catch (EntityNotFoundException e) {
            log.error("Node not found for path: {}", path);
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Node not found: " + e.getMessage()));
        }
    }

    /**
     * Generate breadcrumb navigation for a given path.
     *
     * @param path the node path
     * @return list of breadcrumb items
     */
    @GetMapping("/breadcrumbs")
    public ResponseEntity<ApiResponse<List<BreadcrumbItem>>> getBreadcrumbs(
            @RequestParam String path) {

        log.info("Generating breadcrumbs for path: {}", path);

        List<BreadcrumbItem> breadcrumbs = fileExplorerService.generateBreadcrumbs(path);

        return ResponseEntity.ok(ApiResponse.success("Breadcrumbs generated successfully", breadcrumbs));
    }

    /**
     * Create a new folder in the file explorer.
     * Only professors can create folders within their own namespace.
     * 
     * Request body:
     * {
     *     "path": "/2025-2026/first/John Doe/CS101/lecture_notes",
     *     "folderName": "Week 1"
     * }
     * 
     * Responses:
     * - 201 Created: Folder successfully created
     * - 400 Bad Request: Invalid folder name
     * - 403 Forbidden: User does not have permission
     * - 409 Conflict: Folder already exists
     *
     * @param request the folder creation request containing path and folder name
     * @param authentication the authenticated user
     * @return response containing the created folder details
     */
    @PostMapping("/folder")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<ApiResponse<CreateFolderResponse>> createFolder(
            @Valid @RequestBody CreateFolderRequest request,
            Authentication authentication) {

        log.info("Create folder request - path: {}, folderName: {}", 
                request.getPath(), request.getFolderName());

        User currentUser = authService.getCurrentUser();
        
        CreateFolderResponse response = fileExplorerService.createFolder(request, currentUser);

        log.info("Folder created successfully: {}", response.getFullPath());
        
        // Invalidate cache for the parent directory and the new folder
        filesystemScanService.invalidateCacheRecursive(request.getPath());
        filesystemScanService.invalidateCache(response.getFullPath());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Folder created successfully", response));
    }
    
    /**
     * Delete a folder and all its contents (files and subfolders).
     * Only professors can delete folders within their own namespace.
     * This operation also deletes all physical files and folders from the uploads directory.
     * 
     * Request body:
     * {
     *     "folderPath": "/2025-2026/first/John Doe/CS101/lecture_notes/Week 1"
     * }
     * 
     * Responses:
     * - 200 OK: Folder successfully deleted
     * - 400 Bad Request: Invalid folder path
     * - 403 Forbidden: User does not have permission
     * - 404 Not Found: Folder does not exist
     *
     * @param request the folder deletion request containing the folder path
     * @param authentication the authenticated user
     * @return response containing deletion statistics (number of files/folders deleted)
     */
    @DeleteMapping("/folder")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<ApiResponse<DeleteFolderResponse>> deleteFolder(
            @Valid @RequestBody DeleteFolderRequest request,
            Authentication authentication) {

        log.info("Delete folder request - path: {}", request.getFolderPath());

        User currentUser = authService.getCurrentUser();
        
        DeleteFolderResponse response = fileExplorerService.deleteFolder(request, currentUser);

        log.info("Folder deleted successfully: {} (deleted {} files and {} subfolders)", 
                response.getDeletedPath(), response.getFilesDeleted(), response.getSubfoldersDeleted());
        
        // Invalidate cache for the deleted folder and its parent
        filesystemScanService.invalidateCacheRecursive(request.getFolderPath());

        return ResponseEntity.ok(ApiResponse.success("Folder deleted successfully", response));
    }

    /**
     * Refresh the file explorer tree for a specific academic year and semester.
     * This endpoint forces a fresh fetch from the database and returns the updated
     * tree structure.
     * Useful after creating new professors or course assignments.
     *
     * @param academicYearId the academic year ID
     * @param semesterId     the semester ID
     * @param authentication the authenticated user
     * @return refreshed root node with updated children
     */
    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FileExplorerNode>> refresh(
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId,
            Authentication authentication) {

        log.info("Refreshing file explorer for academicYearId: {}, semesterId: {}",
                academicYearId, semesterId);

        User currentUser = authService.getCurrentUser();

        // Fetch fresh data from database
        FileExplorerNode rootNode = fileExplorerService.getRootNode(academicYearId, semesterId, currentUser);

        log.info("File explorer refreshed with {} professor nodes",
                rootNode.getChildren() != null ? rootNode.getChildren().size() : 0);

        return ResponseEntity.ok(ApiResponse.success("File explorer refreshed successfully", rootNode));
    }

    /**
     * Get metadata for a specific file.
     *
     * @param fileId the file ID
     * @return file metadata
     */
    @GetMapping("/files/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadedFile>> getFileMetadata(
            @PathVariable Long fileId) {

        log.info("Fetching file metadata for fileId: {}", fileId);

        UploadedFile file = fileService.getFile(fileId);

        return ResponseEntity.ok(ApiResponse.success("File metadata retrieved successfully", file));
    }

    /**
     * Upload files from file explorer based on path.
     * Path format: /yearCode/semesterType/professorId/courseCode/documentType
     * Only professors can upload to their own course folders.
     *
     * @param path           the folder path to upload to
     * @param files          the files to upload
     * @param notes          optional notes for the upload
     * @param authentication the authenticated user
     * @return updated node with uploaded files
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<ApiResponse<FileExplorerNode>> uploadFiles(
            @RequestParam String path,
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(required = false) String notes,
            Authentication authentication) {

        log.info("File explorer upload request for path: {}, {} files", path, files.size());

        User currentUser = authService.getCurrentUser();

        // Check write permission for this path
        if (!fileExplorerService.canWrite(path, currentUser)) {
            log.error("User {} does not have write permission for path: {}",
                    currentUser.getEmail(), path);
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("You do not have permission to upload to this folder"));
        }

        try {
            // Parse path to extract components
            PathInfo pathInfo = parsePath(path);

            // Validate path is at document type or custom folder level (5 parts)
            if (!path.matches(".+/.+/.+/.+/.+")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid path format. Must specify document type or custom folder."));
            }

            // Find professor - handle name format, professorId, and legacy "prof_<id>" format
            User professor = findProfessorByIdentifier(pathInfo.professorId)
                    .orElseThrow(() -> new EntityNotFoundException("Professor not found: " + pathInfo.professorId));

            // Find academic year and semester
            AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.yearCode)
                    .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.yearCode));

            SemesterType semesterType = SemesterType.valueOf(pathInfo.semesterType.toUpperCase());
            Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                    .orElseThrow(() -> new EntityNotFoundException("Semester not found"));

            List<UploadedFile> uploadedFiles;
            
            // Check if this is a known document type or a custom folder
            if (isKnownDocumentType(pathInfo.documentType)) {
                // Standard document type upload (Syllabus, Exams, etc.)
                // Find course assignment
                CourseAssignment assignment = courseAssignmentRepository
                        .findBySemesterIdAndCourseCodeAndProfessorId(semester.getId(), pathInfo.courseCode,
                                professor.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Course assignment not found"));

                // Parse document type
                DocumentTypeEnum documentType = DocumentTypeEnum.valueOf(pathInfo.documentType.toUpperCase());

                // Upload files using existing service
                uploadedFiles = fileService.uploadFiles(
                        assignment.getId(),
                        documentType,
                        files,
                        notes,
                        currentUser.getId());
            } else {
                // Custom folder upload - use FolderFileUploadService
                log.info("Uploading to custom folder: {}", pathInfo.documentType);
                
                // Normalize path for folder lookup
                String normalizedPath = path.replaceAll("^/+|/+$", "");
                
                // Convert List<MultipartFile> to array
                MultipartFile[] filesArray = files.toArray(new MultipartFile[0]);
                
                // Upload files to custom folder using folder path
                uploadedFiles = folderFileUploadService.uploadFiles(
                        filesArray,
                        null,  // folderId - will be resolved from path
                        normalizedPath,  // folderPath
                        notes,
                        currentUser.getId());
            }

            log.info("Successfully uploaded {} files to path: {}", uploadedFiles.size(), path);
            
            // Invalidate cache for the upload directory
            filesystemScanService.invalidateCacheRecursive(path);

            // Return updated node
            FileExplorerNode updatedNode = fileExplorerService.getNode(path, currentUser);

            return ResponseEntity.ok(ApiResponse.success(
                    "Successfully uploaded " + uploadedFiles.size() + " file(s)",
                    updatedNode));

        } catch (EntityNotFoundException e) {
            log.error("Entity not found during upload: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument during upload: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading files: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error uploading files: " + e.getMessage()));
        }
    }

    /**
     * Delete a file from the file explorer.
     * Only professors can delete their own files.
     *
     * @param fileId         the ID of the file to delete
     * @param authentication the authenticated user
     * @return success response or error
     */
    @DeleteMapping("/files/{fileId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable Long fileId,
            Authentication authentication) {

        log.info("File explorer delete request for file ID: {}", fileId);

        User currentUser = authService.getCurrentUser();

        try {
            // Get the file to check ownership
            UploadedFile file = fileService.getFile(fileId);
            
            // Store the file URL for cache invalidation before deleting
            String fileUrl = file.getFileUrl();
            
            // Check if user owns the file
            if (file.getUploader() == null || !file.getUploader().getId().equals(currentUser.getId())) {
                log.error("User {} does not own file {}", currentUser.getEmail(), fileId);
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("You can only delete your own files"));
            }

            // Delete the file
            fileService.deleteFile(fileId);
            
            // Invalidate cache for the parent directory
            if (fileUrl != null) {
                String parentPath = fileUrl.contains("/") 
                        ? fileUrl.substring(0, fileUrl.lastIndexOf('/'))
                        : "";
                filesystemScanService.invalidateCacheRecursive(parentPath);
            }

            log.info("Successfully deleted file ID: {} by user {}", fileId, currentUser.getEmail());
            return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));

        } catch (EntityNotFoundException e) {
            log.error("File not found: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error deleting file: " + e.getMessage()));
        }
    }

    /**
     * Replace a file with a new version.
     * Only professors can replace their own files.
     * The old file is deleted and replaced with the new one.
     *
     * @param fileId         the ID of the file to replace
     * @param file           the new file to upload
     * @param notes          optional notes for the new file
     * @param authentication the authenticated user
     * @return updated file information
     */
    @PostMapping("/files/{fileId}/replace")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<ApiResponse<UploadedFile>> replaceFile(
            @PathVariable Long fileId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String notes,
            Authentication authentication) {

        log.info("File explorer replace request for file ID: {} with new file: {}", fileId, file.getOriginalFilename());

        User currentUser = authService.getCurrentUser();

        try {
            // Get the existing file to check ownership
            UploadedFile existingFile = fileService.getFile(fileId);
            
            // Check if user owns the file
            if (existingFile.getUploader() == null || !existingFile.getUploader().getId().equals(currentUser.getId())) {
                log.error("User {} does not own file {}", currentUser.getEmail(), fileId);
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("You can only replace your own files"));
            }

            // Replace the file using the service
            UploadedFile newFile = fileService.replaceFile(fileId, file, notes, currentUser.getId());

            log.info("Successfully replaced file ID: {} with new file ID: {} by user {}", 
                    fileId, newFile.getId(), currentUser.getEmail());
            
            return ResponseEntity.ok(ApiResponse.success("File replaced successfully", newFile));

        } catch (EntityNotFoundException e) {
            log.error("File not found: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error replacing file: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error replacing file: " + e.getMessage()));
        }
    }

    /**
     * Parse path to extract components
     * Path format: /yearCode/semesterType/professorId/courseCode/documentType
     */
    private PathInfo parsePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        // Remove leading/trailing slashes
        path = path.replaceAll("^/+|/+$", "");

        String[] parts = path.split("/");

        PathInfo info = new PathInfo();
        if (parts.length >= 1)
            info.yearCode = parts[0];
        if (parts.length >= 2)
            info.semesterType = parts[1];
        if (parts.length >= 3)
            info.professorId = parts[2];
        if (parts.length >= 4)
            info.courseCode = parts[3];
        if (parts.length >= 5)
            info.documentType = parts[4];

        return info;
    }

    /**
     * Check if a string is a known document type (Syllabus, Exams, etc.)
     */
    private boolean isKnownDocumentType(String type) {
        if (type == null) return false;
        try {
            DocumentTypeEnum.valueOf(type.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Helper class to hold parsed path information
     */
    private static class PathInfo {
        String yearCode;
        String semesterType;
        String professorId;
        String courseCode;
        String documentType;
    }

    /**
     * Download a file with permission checking.
     * Permissions:
     * - Deanship: can download all files
     * - HOD: can download files in their department
     * - Professor: can download files in their department
     *
     * @param fileId         the file ID
     * @param authentication the authenticated user
     * @return file resource for download
     */
    @GetMapping("/files/{fileId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            Authentication authentication) {

        log.info("Downloading file with ID: {}", fileId);

        User currentUser = authService.getCurrentUser();
        UploadedFile file = fileService.getFile(fileId);

        // Check permission using multiple criteria
        boolean hasAccess = false;
        
        // Deanship can download all files
        if (currentUser.getRole() == Role.ROLE_DEANSHIP) {
            hasAccess = true;
            log.debug("Deanship user - granting download access");
        }
        
        // HOD can download files in their department
        if (!hasAccess && currentUser.getRole() == Role.ROLE_HOD) {
            hasAccess = true;
            log.debug("HOD user - granting download access");
        }
        
        // Check if user is the uploader
        if (!hasAccess && file.getUploader() != null && 
            file.getUploader().getId().equals(currentUser.getId())) {
            hasAccess = true;
            log.debug("User is file uploader - granting download access");
        }
        
        // Check via document submission
        if (!hasAccess && file.getDocumentSubmission() != null) {
            DocumentSubmission submission = file.getDocumentSubmission();
            if (submission.getProfessor() != null && 
                submission.getProfessor().getId().equals(currentUser.getId())) {
                hasAccess = true;
                log.debug("User is submission professor - granting download access");
            }
        }
        
        // Check if file path contains professor's identifier
        if (!hasAccess && currentUser.getRole() == Role.ROLE_PROFESSOR) {
            String fileUrl = file.getFileUrl();
            if (fileUrl != null) {
                // Check professor's name (new format)
                String professorFolderName = generateProfessorFolderName(currentUser);
                if (fileUrl.contains(professorFolderName)) {
                    hasAccess = true;
                    log.debug("File path contains professor name - granting download access");
                }
                // Check professorId (legacy)
                if (!hasAccess && currentUser.getProfessorId() != null && fileUrl.contains(currentUser.getProfessorId())) {
                    hasAccess = true;
                    log.debug("File path contains professorId - granting download access");
                }
                // Check fallback format (legacy)
                String fallbackId = "prof_" + currentUser.getId();
                if (!hasAccess && fileUrl.contains(fallbackId)) {
                    hasAccess = true;
                    log.debug("File path contains fallback ID - granting download access");
                }
            }
        }
        
        // Check via folder path
        if (!hasAccess && file.getFolder() != null && file.getFolder().getPath() != null) {
            String virtualPath = "/" + file.getFolder().getPath();
            hasAccess = fileExplorerService.canRead(virtualPath, currentUser);
            log.debug("Folder path check result: {}", hasAccess);
        }

        if (!hasAccess) {
            log.error("User {} does not have permission to download file: {}",
                    currentUser.getEmail(), fileId);
            return ResponseEntity.status(403).build();
        }

        // Load file as resource
        Resource resource = fileService.loadFileAsResource(file.getFileUrl());

        // Determine content type
        String contentType = file.getFileType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // Build Content-Disposition header with proper filename encoding
        // Use both filename and filename* (RFC 5987) for better browser compatibility
        String originalFilename = file.getOriginalFilename();
        String encodedFilename = java.net.URLEncoder.encode(originalFilename, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20"); // Replace + with %20 for spaces

        String contentDisposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                originalFilename,
                encodedFilename);

        log.info("Sending file download response - filename: {}, Content-Disposition: {}",
                originalFilename, contentDisposition);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    /**
     * Generate a safe folder name from professor's name.
     * Sanitizes the name to be filesystem-safe while remaining readable.
     */
    private String generateProfessorFolderName(User professor) {
        String firstName = professor.getFirstName() != null ? professor.getFirstName().trim() : "";
        String lastName = professor.getLastName() != null ? professor.getLastName().trim() : "";
        
        String fullName = (firstName + " " + lastName).trim();
        
        if (fullName.isEmpty()) {
            return "prof_" + professor.getId();
        }
        
        String sanitized = fullName.replaceAll("[\\\\/:*?\"<>|]", "_");
        sanitized = sanitized.replaceAll("\\s+", " ").replaceAll("_+", "_").trim();
        
        return sanitized;
    }

    /**
     * Find professor by identifier - supports multiple formats:
     * 1. Professor's full name (new format): "firstName lastName"
     * 2. Legacy fallback format: "prof_<id>"
     * 3. Legacy professorId field value
     * 
     * @param professorIdentifier The professor identifier from path
     * @return Optional containing the professor User entity, or empty if not found
     */
    private java.util.Optional<User> findProfessorByIdentifier(String professorIdentifier) {
        if (professorIdentifier == null || professorIdentifier.isEmpty() || "null".equals(professorIdentifier)) {
            log.warn("Invalid professor identifier: {}", professorIdentifier);
            return java.util.Optional.empty();
        }
        
        // 1. Check if this is a legacy fallback identifier (prof_<id>)
        if (professorIdentifier.startsWith("prof_")) {
            try {
                Long userId = Long.parseLong(professorIdentifier.substring(5));
                java.util.Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    return userOpt;
                }
            } catch (NumberFormatException e) {
                // Not a valid prof_<id> format, continue to other lookups
            }
        }
        
        // 2. Try looking up by professorId field
        java.util.Optional<User> byProfessorId = userRepository.findByProfessorId(professorIdentifier);
        if (byProfessorId.isPresent()) {
            return byProfessorId;
        }
        
        // 3. Try looking up by name (new format)
        java.util.List<User> allProfessors = userRepository.findByRole(Role.ROLE_PROFESSOR);
        for (User professor : allProfessors) {
            String folderName = generateProfessorFolderName(professor);
            if (folderName.equals(professorIdentifier)) {
                return java.util.Optional.of(professor);
            }
        }
        
        return java.util.Optional.empty();
    }

    /**
     * Download an orphaned file (file that exists on filesystem but not in database).
     * Only the folder owner (professor) can download orphaned files in their folders.
     *
     * @param path the relative path to the file
     * @param authentication the authenticated user
     * @return file resource for download
     */
    @GetMapping("/orphaned-file/download")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<Resource> downloadOrphanedFile(
            @RequestParam String path,
            Authentication authentication) {

        log.info("Downloading orphaned file at path: {}", path);

        User currentUser = authService.getCurrentUser();

        // Check permission - professor's folder name must be in the path
        if (!hasOrphanedFileAccess(path, currentUser)) {
            log.error("User {} does not have permission to download orphaned file: {}",
                    currentUser.getEmail(), path);
            return ResponseEntity.status(403).build();
        }

        try {
            // Load file as resource
            Resource resource = fileService.loadFileAsResource(path);
            
            // Get filename from path
            String filename = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;

            // Determine content type
            String contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading orphaned file: {}", e.getMessage(), e);
            return ResponseEntity.status(404).build();
        }
    }

    /**
     * Delete an orphaned file (file that exists on filesystem but not in database).
     * Only the folder owner (professor) can delete orphaned files in their folders.
     *
     * @param path the relative path to the file
     * @param authentication the authenticated user
     * @return success response
     */
    @DeleteMapping("/orphaned-file")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<ApiResponse<Void>> deleteOrphanedFile(
            @RequestParam String path,
            Authentication authentication) {

        log.info("Deleting orphaned file at path: {}", path);

        User currentUser = authService.getCurrentUser();

        // Check permission - professor's folder name must be in the path
        if (!hasOrphanedFileAccess(path, currentUser)) {
            log.error("User {} does not have permission to delete orphaned file: {}",
                    currentUser.getEmail(), path);
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("You do not have permission to delete this file"));
        }

        try {
            // Delete the physical file
            java.nio.file.Path filePath = java.nio.file.Paths.get("uploads", path);
            
            if (!java.nio.file.Files.exists(filePath)) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("File not found"));
            }
            
            java.nio.file.Files.delete(filePath);
            
            log.info("Successfully deleted orphaned file: {}", path);
            
            // Invalidate cache for the parent directory
            String parentPath = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
            filesystemScanService.invalidateCacheRecursive(parentPath);

            return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));

        } catch (Exception e) {
            log.error("Error deleting orphaned file: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error deleting file: " + e.getMessage()));
        }
    }
    
    /**
     * Check if a professor has access to an orphaned file.
     * Access is granted if the professor's folder name is in the path.
     * 
     * Path format: yearCode/semesterType/professorName/courseCode/...
     * 
     * @param path the file path
     * @param user the current user (professor)
     * @return true if user has access
     */
    private boolean hasOrphanedFileAccess(String path, User user) {
        if (user.getRole() != Role.ROLE_PROFESSOR) {
            return false;
        }
        
        // Generate professor's folder name
        String professorFolderName = generateProfessorFolderName(user);
        
        // Path should be: yearCode/semesterType/professorName/...
        String[] pathParts = path.split("/");
        if (pathParts.length < 3) {
            return false;
        }
        
        // Check if the 3rd part (index 2) matches the professor's folder name
        String pathProfessorName = pathParts[2];
        
        log.debug("Checking orphaned file access: pathProfessorName={}, expectedProfessorName={}", 
                pathProfessorName, professorFolderName);
        
        return pathProfessorName.equals(professorFolderName);
    }
}
