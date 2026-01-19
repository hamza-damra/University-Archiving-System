package com.alquds.edu.ArchiveSystem.dto.fileexplorer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing a file item in a directory listing.
 * Combines filesystem data with database metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileItemDTO {
    
    /**
     * Original filename
     */
    private String name;
    
    /**
     * Stored filename on disk (may be sanitized/renamed)
     */
    private String storedName;
    
    /**
     * Relative path from uploads root
     */
    private String path;
    
    /**
     * File size in bytes
     */
    private Long size;
    
    /**
     * Human-readable file size (e.g., "2.5 MB")
     */
    private String sizeFormatted;
    
    /**
     * MIME type of the file
     */
    private String mimeType;
    
    /**
     * File extension (without dot)
     */
    private String extension;
    
    /**
     * Last modified timestamp from filesystem
     */
    private LocalDateTime modifiedAt;
    
    /**
     * Upload timestamp from database (if tracked)
     */
    private LocalDateTime uploadedAt;
    
    /**
     * Database ID if file is tracked in DB (nullable)
     */
    private Long id;
    
    /**
     * Name of the user who uploaded this file (from DB)
     */
    private String uploaderName;
    
    /**
     * ID of the user who uploaded this file (from DB)
     */
    private Long uploaderId;
    
    /**
     * Notes associated with the file (from DB)
     */
    private String notes;
    
    /**
     * Whether this file exists only on filesystem (not in DB)
     * Indicates it may need reconciliation
     */
    private boolean orphaned;
    
    /**
     * Whether the current user can delete this file
     */
    private boolean canDelete;
    
    /**
     * Whether the current user can replace this file
     */
    private boolean canReplace;
    
    /**
     * Whether this file can be previewed in browser
     */
    private boolean previewable;
    
    /**
     * Download URL for this file
     */
    private String downloadUrl;
    
    /**
     * Preview URL for this file (if previewable)
     */
    private String previewUrl;
}
