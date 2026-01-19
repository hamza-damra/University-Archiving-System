package com.alquds.edu.ArchiveSystem.dto.fileexplorer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for uploaded files in the file explorer system.
 * Used for API responses when returning file information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFileDTO {
    
    /**
     * Unique identifier of the uploaded file
     */
    private Long id;
    
    /**
     * Original filename as uploaded by the user
     */
    private String originalFilename;
    
    /**
     * Sanitized filename stored on disk
     */
    private String storedFilename;
    
    /**
     * File size in bytes
     */
    private Long fileSize;
    
    /**
     * MIME type of the file (e.g., "application/pdf", "image/jpeg")
     */
    private String fileType;
    
    /**
     * Timestamp when the file was uploaded
     */
    private LocalDateTime uploadedAt;
    
    /**
     * Optional notes about the file
     */
    private String notes;
    
    /**
     * Full file URL/path for downloading
     */
    private String fileUrl;
    
    /**
     * Name of the user who uploaded the file
     */
    private String uploaderName;
    
    /**
     * ID of the user who uploaded the file
     */
    private Long uploaderId;
    
    /**
     * Whether the current user can delete this file
     */
    private Boolean canDelete;
    
    /**
     * Whether the current user can replace this file
     */
    private Boolean canReplace;
    
    /**
     * Timestamp when the file was created
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the file was last updated
     */
    private LocalDateTime updatedAt;
    
    /**
     * Whether this file exists only on filesystem without a database record
     * (e.g., added directly by admin)
     */
    private Boolean orphaned;
}
