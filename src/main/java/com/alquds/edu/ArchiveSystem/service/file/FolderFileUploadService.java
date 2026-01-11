package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Service interface for handling file uploads to folder-based storage system.
 * 
 * This service provides functionality for:
 * - Uploading multiple files to specific folders
 * - Validating file size and type
 * - Sanitizing filenames and handling duplicates
 * - Checking user authorization for folder uploads
 * - Storing files physically on disk matching folder structure
 * 
 * @since 2.0
 */
public interface FolderFileUploadService {
    
    /**
     * Upload multiple files to a specific folder.
     * 
     * This method performs the following operations:
     * 1. Validates that files array is not empty
     * 2. Resolves folder ID from folderId or folderPath (creates folder if needed)
     * 3. Retrieves and validates the target folder exists
     * 4. Retrieves and validates the uploader user exists
     * 5. Checks authorization using canUploadToFolder()
     * 6. Validates all files before uploading any
     * 7. Creates physical directory if needed
     * 8. Saves each file to disk with sanitized filename
     * 9. Creates UploadedFile entity for each file
     * 10. Saves entities to database
     * 11. Returns list of uploaded files
     * 
     * @param files Array of uploaded files (must not be null or empty)
     * @param folderId Target folder ID where files will be uploaded (optional if folderPath provided)
     * @param folderPath Target folder path for auto-creation (optional if folderId provided)
     * @param notes Optional notes about the upload (can be null)
     * @param uploaderId User ID of the person uploading the files
     * @return List of created UploadedFile entities with metadata
     * @throws com.alquds.edu.ArchiveSystem.exception.FolderNotFoundException if folder doesn't exist
     * @throws com.alquds.edu.ArchiveSystem.exception.UnauthorizedException if user doesn't have permission
     * @throws com.alquds.edu.ArchiveSystem.exception.FileValidationException if files fail validation
     * @throws com.alquds.edu.ArchiveSystem.exception.FileStorageException if file storage fails
     * @throws IllegalArgumentException if both folderId and folderPath are null
     */
    List<UploadedFile> uploadFiles(MultipartFile[] files, Long folderId, String folderPath, String notes, Long uploaderId);
    
    /**
     * Validate a single file against system constraints.
     * 
     * Validation checks include:
     * - File is not empty
     * - File size doesn't exceed maximum allowed size (configured via file.max-size)
     * - Filename is not null or empty
     * - File extension is in the allowed types list (configured via file.allowed-types)
     * 
     * @param file File to validate
     * @throws com.alquds.edu.ArchiveSystem.exception.FileValidationException if validation fails with specific error message
     */
    void validateFile(MultipartFile file);
    
    /**
     * Generate a safe filename by sanitizing and handling duplicates.
     * 
     * Sanitization process:
     * 1. Replace special characters (except alphanumeric, dot, underscore, hyphen) with underscore
     * 2. Keep only safe characters: a-z, A-Z, 0-9, dot (.), underscore (_), hyphen (-)
     * 3. Check if file already exists at target path
     * 4. If exists, append counter: filename(1).ext, filename(2).ext, etc.
     * 
     * Examples:
     * - "lecture notes.pdf" ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ "lecture_notes.pdf"
     * - "file@#$.doc" ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ "file___.doc"
     * - "report.pdf" (if exists) ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ "report(1).pdf"
     * - "report(1).pdf" (if exists) ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ "report(2).pdf"
     * 
     * @param originalFilename Original filename from uploaded file
     * @param targetPath Target directory path where file will be stored
     * @return Safe filename that doesn't conflict with existing files
     */
    String generateSafeFilename(String originalFilename, Path targetPath);
    
    /**
     * Check if user has permission to upload files to the specified folder.
     * 
     * Authorization rules:
     * - ADMIN role: Can upload to any folder
     * - DEANSHIP role: Can upload to any folder
     * - PROFESSOR role: Can only upload to their own folders (folder.owner.id == user.id)
     * - Other roles: Cannot upload
     * 
     * @param folder Target folder for upload
     * @param user User attempting to upload
     * @return true if user is authorized to upload to the folder, false otherwise
     */
    boolean canUploadToFolder(Folder folder, User user);
}
