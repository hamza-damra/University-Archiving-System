package com.alquds.edu.ArchiveSystem.dto.fileexplorer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for lazy-loaded directory tree structure.
 * Used by the /api/file-explorer/tree endpoint.
 * Supports depth-limited loading for performance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryTreeDTO {
    
    /**
     * Folder name (display name)
     */
    private String name;
    
    /**
     * Relative path from uploads root
     */
    private String path;
    
    /**
     * Type of node (YEAR, SEMESTER, PROFESSOR, COURSE, DOCUMENT_TYPE, CUSTOM)
     */
    private String type;
    
    /**
     * Database entity ID if available
     */
    private Long entityId;
    
    /**
     * Last modified timestamp
     */
    private LocalDateTime modifiedAt;
    
    /**
     * Whether this node has children (used for lazy loading indicator)
     */
    private boolean hasChildren;
    
    /**
     * Child nodes (populated based on requested depth)
     * Empty if depth limit reached - UI should lazy load
     */
    @Builder.Default
    private List<DirectoryTreeDTO> children = new ArrayList<>();
    
    /**
     * Whether children have been loaded for this node
     * If false and hasChildren is true, UI should request children
     */
    private boolean childrenLoaded;
    
    /**
     * Whether the current user can write to this node
     */
    private boolean canWrite;
    
    /**
     * Whether the current user can delete this node
     */
    private boolean canDelete;
    
    /**
     * ETag for this tree branch (for caching)
     */
    private String etag;
    
    /**
     * Number of files directly in this folder (not recursive)
     */
    @Builder.Default
    private int fileCount = 0;
    
    /**
     * Number of subfolders directly in this folder
     */
    @Builder.Default  
    private int folderCount = 0;
    
    /**
     * Additional metadata depending on node type
     */
    private TreeNodeMetadata metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TreeNodeMetadata {
        // Academic context
        private Long academicYearId;
        private String academicYearCode;
        private Long semesterId;
        private String semesterType;
        
        // Professor context
        private Long professorId;
        private String professorName;
        private String professorEmail;
        
        // Course context
        private Long courseId;
        private String courseCode;
        private String courseName;
        
        // Department context
        private Long departmentId;
        private String departmentName;
        
        // Ownership flags
        private Boolean isOwnFolder;
        private Boolean isOwnCourse;
    }
}
