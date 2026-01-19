package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.dto.fileexplorer.DirectoryListingDTO;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.DirectoryTreeDTO;
import com.alquds.edu.ArchiveSystem.entity.user.User;

/**
 * Service interface for filesystem-based directory scanning and listing.
 * Provides direct filesystem operations for the file explorer,
 * treating the filesystem as the source of truth.
 */
public interface FilesystemScanService {

    /**
     * List the contents of a directory from the filesystem.
     * Combines filesystem data with database metadata where available.
     * 
     * @param relativePath The relative path from uploads root (empty string for root)
     * @param currentUser The authenticated user (for permission checking)
     * @param page Page number (1-indexed)
     * @param pageSize Number of items per page
     * @param sortBy Field to sort by ("name", "modifiedAt", "size")
     * @param sortOrder Sort order ("asc" or "desc")
     * @return DirectoryListingDTO with folders and files
     */
    DirectoryListingDTO listDirectory(String relativePath, User currentUser, 
            int page, int pageSize, String sortBy, String sortOrder);
    
    /**
     * Get a lazy-loaded directory tree starting from a path.
     * 
     * @param relativePath The relative path from uploads root
     * @param currentUser The authenticated user
     * @param depth How many levels to load (1 = immediate children only)
     * @return DirectoryTreeDTO with children up to specified depth
     */
    DirectoryTreeDTO getDirectoryTree(String relativePath, User currentUser, int depth);
    
    /**
     * Check if a directory has changed since a specific ETag.
     * Used for 304 Not Modified responses.
     * 
     * @param relativePath The relative path
     * @param etag The ETag to compare against
     * @return true if directory has changed (ETag doesn't match)
     */
    boolean hasDirectoryChanged(String relativePath, String etag);
    
    /**
     * Compute an ETag for a directory based on its contents.
     * ETag changes when files/folders are added, removed, or modified.
     * 
     * @param relativePath The relative path
     * @return The computed ETag string
     */
    String computeDirectoryETag(String relativePath);
    
    /**
     * Invalidate cache for a specific directory path.
     * Called after write operations.
     * 
     * @param relativePath The relative path to invalidate
     */
    void invalidateCache(String relativePath);
    
    /**
     * Invalidate cache for a directory and all its parent directories.
     * 
     * @param relativePath The relative path
     */
    void invalidateCacheRecursive(String relativePath);
    
    /**
     * Check if a path exists on the filesystem.
     * 
     * @param relativePath The relative path
     * @return true if the path exists
     */
    boolean pathExists(String relativePath);
    
    /**
     * Check if a path is a directory.
     * 
     * @param relativePath The relative path
     * @return true if the path exists and is a directory
     */
    boolean isDirectory(String relativePath);
}
