package com.alquds.edu.ArchiveSystem.dto.fileexplorer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for file metadata in the preview system.
 * Contains comprehensive file information including preview capabilities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataDTO {
    
    /**
     * Unique identifier of the file
     */
    private Long id;
    
    /**
     * File name as displayed to users
     */
    private String fileName;
    
    /**
     * Original filename as uploaded
     */
    private String originalFilename;
    
    /**
     * MIME type of the file (e.g., "application/pdf", "text/plain")
     */
    private String mimeType;
    
    /**
     * File size in bytes
     */
    private Long fileSize;
    
    /**
     * Timestamp when the file was uploaded
     */
    private LocalDateTime uploadDate;
    
    /**
     * Name of the user who uploaded the file
     */
    private String uploaderName;
    
    /**
     * Email of the user who uploaded the file
     */
    private String uploaderEmail;
    
    /**
     * Department name (for Dean/HOD views)
     */
    private String departmentName;
    
    /**
     * Whether the file can be previewed
     */
    private boolean previewable;
    
    /**
     * Type of preview renderer to use
     * Values: "pdf", "office", "code", "text", "image", "unsupported"
     */
    private String previewType;
}
