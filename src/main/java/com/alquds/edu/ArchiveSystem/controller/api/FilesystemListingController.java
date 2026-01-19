package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.DirectoryListingDTO;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.DirectoryTreeDTO;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.exception.file.InvalidPathException;
import com.alquds.edu.ArchiveSystem.service.auth.AuthService;
import com.alquds.edu.ArchiveSystem.service.file.FilesystemScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for filesystem-based directory listing endpoints.
 * These endpoints provide direct filesystem scanning for the file explorer,
 * treating the filesystem as the source of truth.
 * 
 * Endpoints:
 * - GET /api/file-explorer/list - List directory contents with pagination
 * - GET /api/file-explorer/tree - Get lazy-loaded directory tree
 * 
 * Features:
 * - ETag support for caching (304 Not Modified)
 * - Pagination for large directories
 * - Role-based access control
 * - Path traversal protection
 */
@RestController
@RequestMapping("/api/file-explorer")
@RequiredArgsConstructor
@Slf4j
public class FilesystemListingController {

    private final FilesystemScanService filesystemScanService;
    private final AuthService authService;

    /**
     * List the contents of a directory from the filesystem.
     * Returns folders and files with metadata, supporting pagination and sorting.
     * 
     * Path format: relative path from uploads root
     * Examples:
     *   - "" or "/" -> uploads root
     *   - "2025-2026/first" -> specific semester folder
     *   - "2025-2026/first/John Doe/CS101" -> course folder
     * 
     * @param path Relative path from uploads root (empty string for root)
     * @param page Page number (1-indexed, default: 1)
     * @param pageSize Items per page (default: 50, max: 100)
     * @param sortBy Field to sort by: "name", "modifiedAt", "size" (default: "name")
     * @param sortOrder Sort order: "asc" or "desc" (default: "asc")
     * @param ifNoneMatch ETag from previous request for conditional GET
     * @return DirectoryListingDTO with folders, files, pagination info, and ETag
     */
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DirectoryListingDTO>> listDirectory(
            @RequestParam(defaultValue = "") String path,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {

        log.info("List directory request: path={}, page={}, pageSize={}, sort={} {}",
                path, page, pageSize, sortBy, sortOrder);

        try {
            User currentUser = authService.getCurrentUser();
            
            // Validate pagination parameters
            page = Math.max(1, page);
            pageSize = Math.min(100, Math.max(1, pageSize));
            
            // Check if directory has changed (ETag validation)
            if (ifNoneMatch != null && !ifNoneMatch.isEmpty()) {
                if (!filesystemScanService.hasDirectoryChanged(path, ifNoneMatch)) {
                    log.debug("Directory unchanged, returning 304 for path: {}", path);
                    return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
                }
            }
            
            // Get directory listing
            DirectoryListingDTO listing = filesystemScanService.listDirectory(
                    path, currentUser, page, pageSize, sortBy, sortOrder);
            
            // Build response with ETag header
            return ResponseEntity.ok()
                    .header(HttpHeaders.ETAG, listing.getEtag())
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=5")
                    .body(ApiResponse.success("Directory listing retrieved successfully", listing));
            
        } catch (InvalidPathException e) {
            log.warn("Invalid path requested: {}", path, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error listing directory: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to list directory: " + e.getMessage()));
        }
    }

    /**
     * Get a lazy-loaded directory tree structure.
     * Returns tree nodes with optional depth-limited children loading.
     * 
     * @param path Relative path from uploads root
     * @param depth How many levels of children to load (default: 1, max: 3)
     * @param ifNoneMatch ETag from previous request
     * @return DirectoryTreeDTO with children up to specified depth
     */
    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DirectoryTreeDTO>> getDirectoryTree(
            @RequestParam(defaultValue = "") String path,
            @RequestParam(defaultValue = "1") int depth,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {

        log.info("Directory tree request: path={}, depth={}", path, depth);

        try {
            User currentUser = authService.getCurrentUser();
            
            // Limit depth for performance
            depth = Math.min(3, Math.max(0, depth));
            
            // Check ETag
            if (ifNoneMatch != null && !ifNoneMatch.isEmpty()) {
                if (!filesystemScanService.hasDirectoryChanged(path, ifNoneMatch)) {
                    log.debug("Directory tree unchanged, returning 304 for path: {}", path);
                    return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
                }
            }
            
            // Get tree
            DirectoryTreeDTO tree = filesystemScanService.getDirectoryTree(path, currentUser, depth);
            
            if (tree == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.ETAG, tree.getEtag())
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=5")
                    .body(ApiResponse.success("Directory tree retrieved successfully", tree));
            
        } catch (InvalidPathException e) {
            log.warn("Invalid path for tree: {}", path, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting directory tree: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get directory tree: " + e.getMessage()));
        }
    }

    /**
     * Check if a path exists on the filesystem.
     * Useful for validating paths before operations.
     * 
     * @param path Relative path to check
     * @return Boolean indicating if path exists
     */
    @GetMapping("/exists")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> checkPathExists(
            @RequestParam String path) {

        log.debug("Check path exists: {}", path);

        try {
            boolean exists = filesystemScanService.pathExists(path);
            boolean isDirectory = exists && filesystemScanService.isDirectory(path);
            
            return ResponseEntity.ok(ApiResponse.success(
                    exists ? (isDirectory ? "Path exists and is a directory" : "Path exists and is a file")
                           : "Path does not exist",
                    exists));
            
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Force refresh a directory by invalidating its cache.
     * Call this after write operations to ensure fresh data.
     * 
     * @param path Relative path to refresh
     * @param recursive Whether to invalidate parent caches too
     * @return Success message
     */
    @PostMapping("/refresh-cache")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> refreshCache(
            @RequestParam(defaultValue = "") String path,
            @RequestParam(defaultValue = "true") boolean recursive) {

        log.info("Refresh cache request: path={}, recursive={}", path, recursive);

        try {
            if (recursive) {
                filesystemScanService.invalidateCacheRecursive(path);
            } else {
                filesystemScanService.invalidateCache(path);
            }
            
            return ResponseEntity.ok(ApiResponse.success("Cache invalidated successfully", null));
            
        } catch (Exception e) {
            log.error("Error refreshing cache: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to refresh cache: " + e.getMessage()));
        }
    }

    /**
     * Get the current ETag for a directory.
     * Useful for checking if a refresh is needed.
     * 
     * @param path Relative path
     * @return Current ETag
     */
    @GetMapping("/etag")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> getDirectoryETag(
            @RequestParam(defaultValue = "") String path) {

        try {
            String etag = filesystemScanService.computeDirectoryETag(path);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.ETAG, etag)
                    .body(ApiResponse.success("ETag computed", etag));
            
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
