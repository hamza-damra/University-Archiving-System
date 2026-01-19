package com.alquds.edu.ArchiveSystem.dto.fileexplorer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for filesystem-based directory listing response.
 * Represents the contents of a directory as scanned from the filesystem.
 * This is the primary response format for the /api/file-explorer/list endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryListingDTO {
    
    /**
     * The relative path of the directory being listed
     * Example: "2025-2026/first/Nour Salem/MATH201 - Linear Algebra"
     */
    private String path;
    
    /**
     * Display name for the current directory
     */
    private String name;
    
    /**
     * List of folders in this directory
     */
    @Builder.Default
    private List<FolderItemDTO> folders = new ArrayList<>();
    
    /**
     * List of files in this directory
     */
    @Builder.Default
    private List<FileItemDTO> files = new ArrayList<>();
    
    /**
     * Total count of items (folders + files)
     */
    private int totalItems;
    
    /**
     * Current page number (1-indexed) for pagination
     */
    private int page;
    
    /**
     * Number of items per page
     */
    private int pageSize;
    
    /**
     * Total pages available
     */
    private int totalPages;
    
    /**
     * Whether there are more pages available
     */
    private boolean hasMore;
    
    /**
     * ETag for caching - based on directory lastModified + content hash
     * Format: W/"timestamp-itemcount-contentshash"
     */
    private String etag;
    
    /**
     * Whether the current user can write to this directory
     */
    private boolean canWrite;
    
    /**
     * Whether the current user can delete from this directory
     */
    private boolean canDelete;
    
    /**
     * Whether the current user can create folders in this directory
     */
    private boolean canCreateFolder;
    
    /**
     * Parent directory path (null if at root)
     */
    private String parentPath;
}
