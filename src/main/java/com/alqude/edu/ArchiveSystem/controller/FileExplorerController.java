package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.fileexplorer.BreadcrumbItem;
import com.alqude.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode;
import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.repository.*;
import com.alqude.edu.ArchiveSystem.service.AuthService;
import com.alqude.edu.ArchiveSystem.service.FileExplorerService;
import com.alqude.edu.ArchiveSystem.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

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
 */
@RestController
@RequestMapping("/api/file-explorer")
@RequiredArgsConstructor
@Slf4j
public class FileExplorerController {

    private final FileExplorerService fileExplorerService;
    private final FileService fileService;
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

            // Validate path is at document type level
            if (!path.matches(".+/.+/.+/.+/.+")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid path format. Must specify document type folder."));
            }

            // Find professor - handle both professorId and fallback format "prof_<id>"
            User professor = findProfessorByIdentifier(pathInfo.professorId)
                    .orElseThrow(() -> new EntityNotFoundException("Professor not found: " + pathInfo.professorId));

            // Find academic year and semester
            AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.yearCode)
                    .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.yearCode));

            SemesterType semesterType = SemesterType.valueOf(pathInfo.semesterType.toUpperCase());
            Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                    .orElseThrow(() -> new EntityNotFoundException("Semester not found"));

            // Find course assignment
            CourseAssignment assignment = courseAssignmentRepository
                    .findBySemesterIdAndCourseCodeAndProfessorId(semester.getId(), pathInfo.courseCode,
                            professor.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Course assignment not found"));

            // Parse document type
            DocumentTypeEnum documentType = DocumentTypeEnum.valueOf(pathInfo.documentType.toUpperCase());

            // Upload files using existing service
            List<UploadedFile> uploadedFiles = fileService.uploadFiles(
                    assignment.getId(),
                    documentType,
                    files,
                    notes,
                    currentUser.getId());

            log.info("Successfully uploaded {} files to path: {}", uploadedFiles.size(), path);

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
                // Check professorId
                if (currentUser.getProfessorId() != null && fileUrl.contains(currentUser.getProfessorId())) {
                    hasAccess = true;
                    log.debug("File path contains professorId - granting download access");
                }
                // Check fallback format
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
     * Find professor by identifier (either professorId or fallback format "prof_<id>")
     * @param professorIdentifier The professor identifier from path
     * @return Optional containing the professor User entity, or empty if not found
     */
    private java.util.Optional<User> findProfessorByIdentifier(String professorIdentifier) {
        // Check if this is a fallback identifier (prof_<id>)
        if (professorIdentifier != null && professorIdentifier.startsWith("prof_")) {
            try {
                Long userId = Long.parseLong(professorIdentifier.substring(5));
                return userRepository.findById(userId);
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }
        
        // Otherwise, look up by professorId
        return userRepository.findByProfessorId(professorIdentifier);
    }
}
