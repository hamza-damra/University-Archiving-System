package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum;
import com.alqude.edu.ArchiveSystem.entity.UploadedFile;
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
    List<UploadedFile> replaceFiles(Long submissionId, List<MultipartFile> files, String notes);
    
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
    boolean canUserReadFile(Long fileId, com.alqude.edu.ArchiveSystem.entity.User user);
    
    /**
     * Check if a user can write/upload to a course assignment.
     * 
     * @param courseAssignmentId the course assignment ID
     * @param user the user to check
     * @return true if user has write permission
     */
    boolean canUserWriteToCourseAssignment(Long courseAssignmentId, com.alqude.edu.ArchiveSystem.entity.User user);
    
    /**
     * Check if a user can delete a specific file.
     * 
     * @param fileId the file ID
     * @param user the user to check
     * @return true if user has delete permission
     */
    boolean canUserDeleteFile(Long fileId, com.alqude.edu.ArchiveSystem.entity.User user);
}
