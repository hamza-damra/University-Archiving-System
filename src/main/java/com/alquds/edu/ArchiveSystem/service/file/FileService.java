package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for file operations in the semester-based file system
 */
public interface FileService {
    
    // File Upload
    List<UploadedFile> uploadFiles(Long courseAssignmentId, DocumentTypeEnum documentType, 
                                    List<MultipartFile> files, String notes, Long professorId);
    
    // File Replacement
    void replaceFiles(Long submissionId, List<MultipartFile> files, String notes);
    
    /**
     * Replace a single file with a new version.
     * The old file is deleted and replaced with the new one.
     * 
     * @param fileId the ID of the file to replace
     * @param newFile the new file to upload
     * @param notes optional notes for the new file
     * @param userId the ID of the user performing the replacement
     * @return the newly uploaded file
     */
    UploadedFile replaceFile(Long fileId, MultipartFile newFile, String notes, Long userId);
    
    // File Retrieval
    UploadedFile getFile(Long fileId);
    
    List<UploadedFile> getFilesBySubmission(Long submissionId);
    
    Resource loadFileAsResource(String fileUrl);
    
    // File Deletion
    void deleteFile(Long fileId);
    
    // File Validation
    boolean validateFileType(MultipartFile file, List<String> allowedExtensions);
    
    boolean validateFileSize(List<MultipartFile> files, Integer maxTotalSizeMb);
    
    // File Path Generation
    String generateFilePath(String yearCode, String semesterType, String professorId, 
                           String courseCode, DocumentTypeEnum documentType, String filename);
    
    // Permission Checking (for use by controllers and other services)
    /**
     * Check if a user can read a specific file.
     * 
     * @param fileId the file ID
     * @param user the user to check
     * @return true if user has read permission
     */
    boolean canUserReadFile(Long fileId, com.alquds.edu.ArchiveSystem.entity.user.User user);
    
    /**
     * Check if a user can write/upload to a course assignment.
     * 
     * @param courseAssignmentId the course assignment ID
     * @param user the user to check
     * @return true if user has write permission
     */
    boolean canUserWriteToCourseAssignment(Long courseAssignmentId, com.alquds.edu.ArchiveSystem.entity.user.User user);
    
    /**
     * Check if a user can delete a specific file.
     * 
     * @param fileId the file ID
     * @param user the user to check
     * @return true if user has delete permission
     */
    boolean canUserDeleteFile(Long fileId, com.alquds.edu.ArchiveSystem.entity.user.User user);
}
