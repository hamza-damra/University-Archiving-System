package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.fileexplorer.FileMetadataDTO;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.service.AuthService;
import com.alqude.edu.ArchiveSystem.service.FilePreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for file preview operations.
 * Provides endpoints for retrieving file metadata, content, and preview data.
 */
@RestController
@RequestMapping("/api/file-explorer/files")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FilePreviewController {
    
    private final FilePreviewService filePreviewService;
    private final AuthService authService;
    
    /**
     * Get file metadata for preview.
     * Returns comprehensive file information including preview capabilities.
     * 
     * @param fileId the file ID
     * @return file metadata DTO
     */
    @GetMapping("/{fileId}/metadata")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FileMetadataDTO>> getFileMetadata(@PathVariable Long fileId) {
        log.info("Getting file metadata for fileId: {}", fileId);
        
        try {
            User currentUser = authService.getCurrentUser();
            FileMetadataDTO metadata = filePreviewService.getFileMetadata(fileId, currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("File metadata retrieved successfully", metadata));
            
        } catch (EntityNotFoundException e) {
            log.error("File not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("File not found: " + e.getMessage()));
                
        } catch (AccessDeniedException e) {
            log.error("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You do not have permission to preview this file"));
                
        } catch (Exception e) {
            log.error("Error getting file metadata: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An error occurred while retrieving file metadata"));
        }
    }
    
    /**
     * Get file content as text (for text-based files).
     * Returns the raw file content as a string.
     * Supports partial loading for large files.
     * 
     * @param fileId the file ID
     * @param partial whether to load only partial content (first N lines)
     * @param lines number of lines to load for partial preview (default: 500)
     * @return file content as string
     */
    @GetMapping("/{fileId}/content")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> getFileContent(
            @PathVariable Long fileId,
            @RequestParam(required = false, defaultValue = "false") boolean partial,
            @RequestParam(required = false, defaultValue = "500") int lines) {
        log.info("Getting file content for fileId: {}, partial: {}, lines: {}", fileId, partial, lines);
        
        try {
            User currentUser = authService.getCurrentUser();
            String content;
            
            if (partial) {
                content = filePreviewService.getPartialFileContent(fileId, currentUser, lines);
            } else {
                content = filePreviewService.getFileContent(fileId, currentUser);
            }
            
            return ResponseEntity.ok(ApiResponse.success("File content retrieved successfully", content));
            
        } catch (EntityNotFoundException e) {
            log.error("File not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("File not found: " + e.getMessage()));
                
        } catch (AccessDeniedException e) {
            log.error("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You do not have permission to preview this file"));
                
        } catch (Exception e) {
            log.error("Error getting file content: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An error occurred while retrieving file content"));
        }
    }
    
    /**
     * Get file preview (for binary files or converted formats).
     * Returns the file content as a byte array.
     * 
     * @param fileId the file ID
     * @return file content as byte array
     */
    @GetMapping("/{fileId}/preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFilePreview(@PathVariable Long fileId) {
        log.info("Getting file preview for fileId: {}", fileId);
        
        try {
            User currentUser = authService.getCurrentUser();
            
            // Get metadata first to determine content type
            FileMetadataDTO metadata = filePreviewService.getFileMetadata(fileId, currentUser);
            
            // Get preview content
            byte[] content = filePreviewService.getFilePreview(fileId, currentUser);
            
            // Determine media type
            MediaType mediaType = MediaType.parseMediaType(
                metadata.getMimeType() != null ? metadata.getMimeType() : "application/octet-stream"
            );
            
            return ResponseEntity.ok()
                .contentType(mediaType)
                .body(content);
                
        } catch (EntityNotFoundException e) {
            log.error("File not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("File not found: " + e.getMessage()));
                
        } catch (AccessDeniedException e) {
            log.error("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You do not have permission to preview this file"));
                
        } catch (Exception e) {
            log.error("Error getting file preview: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An error occurred while retrieving file preview"));
        }
    }
    
    /**
     * Check if a file is previewable.
     * Returns a boolean indicating whether the file type is supported for preview.
     * 
     * @param fileId the file ID
     * @return true if file is previewable, false otherwise
     */
    @GetMapping("/{fileId}/previewable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> isPreviewable(@PathVariable Long fileId) {
        log.info("Checking if file is previewable: {}", fileId);
        
        try {
            User currentUser = authService.getCurrentUser();
            FileMetadataDTO metadata = filePreviewService.getFileMetadata(fileId, currentUser);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Preview availability checked", 
                metadata.isPreviewable()
            ));
            
        } catch (EntityNotFoundException e) {
            log.error("File not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("File not found: " + e.getMessage()));
                
        } catch (AccessDeniedException e) {
            log.error("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You do not have permission to access this file"));
                
        } catch (Exception e) {
            log.error("Error checking preview availability: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An error occurred while checking preview availability"));
        }
    }
    
    /**
     * Convert an Office document to HTML for preview.
     * Returns the converted HTML content.
     * 
     * @param fileId the file ID
     * @return HTML content
     */
    @GetMapping("/{fileId}/office-preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOfficePreview(@PathVariable Long fileId) {
        log.info("Getting Office document preview for fileId: {}", fileId);
        
        try {
            User currentUser = authService.getCurrentUser();
            
            // Convert Office document to HTML
            byte[] htmlContent = filePreviewService.convertOfficeDocumentToHtml(fileId, currentUser);
            
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlContent);
                
        } catch (EntityNotFoundException e) {
            log.error("File not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("File not found: " + e.getMessage()));
                
        } catch (AccessDeniedException e) {
            log.error("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You do not have permission to preview this file"));
                
        } catch (IllegalArgumentException e) {
            log.error("Invalid file type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("File is not an Office document: " + e.getMessage()));
                
        } catch (Exception e) {
            log.error("Error converting Office document: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to convert Office document. The file may be corrupted or in an unsupported format."));
        }
    }
}
