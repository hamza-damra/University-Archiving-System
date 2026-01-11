package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;


import java.util.List;

/**
 * Service for centralized file access control based on user roles.
 * Implements role-based access rules:
 * - Admin/Dean: Full access to all files across all departments
 * - HOD: Access only to files from professors in their department
 * - Professor: Access only to their own files
 * 
 * Requirements: 10.1, 11.1, 12.1, 13.1, 13.4
 */
public interface FileAccessService {
    
    /**
     * Checks if a user can access a specific file.
     * Access rules:
     * - Admin/Dean: Can access all files
     * - HOD: Can access files from professors in their department
     * - Professor: Can access their own files
     *
     * @param user The user requesting access
     * @param fileId The ID of the file to access
     * @return true if access is allowed, false otherwise
     */
    boolean canAccessFile(User user, Long fileId);
    
    /**
     * Checks if a user can access a specific file entity.
     * Same rules as canAccessFile(User, Long) but accepts the file entity directly.
     *
     * @param user The user requesting access
     * @param file The file to access
     * @return true if access is allowed, false otherwise
     */
    boolean canAccessFile(User user, UploadedFile file);
    
    /**
     * Gets all files accessible to a user based on their role and department.
     * - Admin/Dean: All files
     * - HOD: Files from professors in their department
     * - Professor: Their own files
     *
     * @param user The user
     * @return List of accessible files
     */
    List<UploadedFile> getAccessibleFiles(User user);
    
    /**
     * Gets files accessible to a user filtered by department.
     * Only applicable for Admin/Dean users who can filter by department.
     *
     * @param user The user
     * @param departmentId The department ID to filter by (null for all)
     * @return List of accessible files
     */
    List<UploadedFile> getAccessibleFilesByDepartment(User user, Long departmentId);
    
    /**
     * Logs an access denial for audit purposes.
     * Records user ID, file ID, timestamp, and denial reason.
     *
     * @param user The user who was denied access
     * @param fileId The file ID that was denied
     * @param reason The reason for denial
     */
    void logAccessDenial(User user, Long fileId, String reason);
    
    /**
     * Checks if a user has admin-level file access (Admin or Dean role).
     *
     * @param user The user to check
     * @return true if user has admin-level access
     */
    boolean hasAdminLevelAccess(User user);
    
    /**
     * Checks if a user can access files from a specific department.
     *
     * @param user The user
     * @param departmentId The department ID
     * @return true if user can access files from the department
     */
    boolean canAccessDepartmentFiles(User user, Long departmentId);
}
