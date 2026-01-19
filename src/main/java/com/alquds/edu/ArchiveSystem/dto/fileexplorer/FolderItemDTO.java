package com.alquds.edu.ArchiveSystem.dto.fileexplorer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing a folder item in a directory listing.
 * Contains metadata from both filesystem and database (if available).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderItemDTO {
    
    /**
     * Folder name (display name)
     */
    private String name;
    
    /**
     * Relative path from uploads root
     */
    private String path;
    
    /**
     * Last modified timestamp from filesystem
     */
    private LocalDateTime modifiedAt;
    
    /**
     * Database ID if folder is tracked in DB (nullable)
     */
    private Long id;
    
    /**
     * Type of folder (COURSE, DOCUMENT_TYPE, CUSTOM, etc.)
     */
    private String folderType;
    
    /**
     * Number of items in this folder (files + subfolders)
     * -1 indicates count not computed (for performance)
     */
    @Builder.Default
    private int itemCount = -1;
    
    /**
     * Whether the current user can write to this folder
     */
    private boolean canWrite;
    
    /**
     * Whether the current user can delete this folder
     */
    private boolean canDelete;
    
    /**
     * Whether this is a system folder (e.g., document type folders)
     * that cannot be deleted
     */
    private boolean isSystemFolder;
    
    /**
     * Additional metadata about the folder (owner info, department, etc.)
     */
    private FolderMetadata metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FolderMetadata {
        private Long professorId;
        private String professorName;
        private Long courseId;
        private String courseCode;
        private String courseName;
        private Long departmentId;
        private String departmentName;
        private Boolean isOwnFolder;
    }
}
