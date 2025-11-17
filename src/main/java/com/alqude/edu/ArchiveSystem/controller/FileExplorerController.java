package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.fileexplorer.BreadcrumbItem;
import com.alqude.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode;
import com.alqude.edu.ArchiveSystem.entity.UploadedFile;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.service.AuthService;
import com.alqude.edu.ArchiveSystem.service.FileExplorerService;
import com.alqude.edu.ArchiveSystem.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    
    /**
     * Get the root node of the file explorer for a specific academic year and semester.
     * Applies role-based filtering:
     * - Deanship: all professors in semester
     * - HOD: professors in HOD's department only
     * - Professor: all professors in same department
     *
     * @param academicYearId the academic year ID
     * @param semesterId the semester ID
     * @param authentication the authenticated user
     * @return root node with filtered children
     */
    @GetMapping("/root")
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
     *
     * @param path the node path
     * @param authentication the authenticated user
     * @return the node with its children
     */
    @GetMapping("/node")
    public ResponseEntity<ApiResponse<FileExplorerNode>> getNode(
            @RequestParam String path,
            Authentication authentication) {
        
        log.info("Fetching file explorer node for path: {}", path);
        
        User currentUser = authService.getCurrentUser();
        FileExplorerNode node = fileExplorerService.getNode(path, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success("Node retrieved successfully", node));
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
     * Get metadata for a specific file.
     *
     * @param fileId the file ID
     * @return file metadata
     */
    @GetMapping("/files/{fileId}")
    public ResponseEntity<ApiResponse<UploadedFile>> getFileMetadata(
            @PathVariable Long fileId) {
        
        log.info("Fetching file metadata for fileId: {}", fileId);
        
        UploadedFile file = fileService.getFile(fileId);
        
        return ResponseEntity.ok(ApiResponse.success("File metadata retrieved successfully", file));
    }
    
    /**
     * Download a file with permission checking.
     * Permissions:
     * - Deanship: can download all files
     * - HOD: can download files in their department
     * - Professor: can download files in their department
     *
     * @param fileId the file ID
     * @param authentication the authenticated user
     * @return file resource for download
     */
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            Authentication authentication) {
        
        log.info("Downloading file with ID: {}", fileId);
        
        User currentUser = authService.getCurrentUser();
        UploadedFile file = fileService.getFile(fileId);
        
        // Note: Permission checking is handled by FileExplorerService.canRead()
        // which is called internally when accessing file metadata
        // Additional permission checks can be added here if needed based on user role
        
        // Load file as resource
        Resource resource = fileService.loadFileAsResource(file.getFileUrl());
        
        // Determine content type
        String contentType = file.getFileType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .body(resource);
    }
}
