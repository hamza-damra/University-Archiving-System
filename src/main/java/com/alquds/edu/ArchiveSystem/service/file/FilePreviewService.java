package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.entity.user.User;

import com.alquds.edu.ArchiveSystem.dto.fileexplorer.FileMetadataDTO;

/**
 * Service interface for file preview operations.
 * Handles file metadata retrieval, content access, and permission validation.
 */
public interface FilePreviewService {
    
    /**
     * Get file metadata for preview.
     * 
     * @param fileId the file ID
     * @param currentUser the current authenticated user
     * @return file metadata DTO
     */
    FileMetadataDTO getFileMetadata(Long fileId, User currentUser);
    
    /**
     * Get file content as string (for text-based files).
     * 
     * @param fileId the file ID
     * @param currentUser the current authenticated user
     * @return file content as string
     */
    String getFileContent(Long fileId, User currentUser);
    
    /**
     * Get partial file content (first N lines) for large files.
     * 
     * @param fileId the file ID
     * @param currentUser the current authenticated user
     * @param maxLines maximum number of lines to return
     * @return partial file content as string
     */
    String getPartialFileContent(Long fileId, User currentUser, int maxLines);
    
    /**
     * Get file preview (for binary files or converted formats).
     * 
     * @param fileId the file ID
     * @param currentUser the current authenticated user
     * @return file content as byte array
     */
    byte[] getFilePreview(Long fileId, User currentUser);
    
    /**
     * Check if a file type is previewable.
     * 
     * @param mimeType the MIME type to check
     * @return true if the file type can be previewed
     */
    boolean isPreviewable(String mimeType);
    
    /**
     * Validate that a user has permission to preview a file.
     * 
     * @param fileId the file ID
     * @param user the user to check
     * @return true if user can preview the file
     */
    boolean canUserPreviewFile(Long fileId, User user);
    
    /**
     * Detect the MIME type of a file.
     * 
     * @param filePath the file path
     * @return detected MIME type
     */
    String detectMimeType(String filePath);
    
    /**
     * Determine the preview type for a given MIME type.
     * 
     * @param mimeType the MIME type
     * @return preview type ("pdf", "office", "code", "text", "image", "unsupported")
     */
    String getPreviewType(String mimeType);
    
    /**
     * Convert an Office document to HTML format.
     * 
     * @param fileId the file ID
     * @param currentUser the current authenticated user
     * @return HTML content as byte array
     */
    byte[] convertOfficeDocumentToHtml(Long fileId, User currentUser);
}
