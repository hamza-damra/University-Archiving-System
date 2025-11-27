package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.entity.Department;
import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.UploadedFile;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of FileAccessService providing centralized file access control.
 * 
 * Role-based access rules:
 * - ROLE_ADMIN: Full access to all files across all departments
 * - ROLE_DEANSHIP: Full access to all files across all departments
 * - ROLE_HOD: Access only to files from professors in their department
 * - ROLE_PROFESSOR: Access only to their own files
 * 
 * Requirements: 10.1, 11.1, 12.1, 13.1, 13.4
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FileAccessServiceImpl implements FileAccessService {
    
    private final UploadedFileRepository uploadedFileRepository;
    
    @Override
    public boolean canAccessFile(User user, Long fileId) {
        if (user == null || fileId == null) {
            log.warn("canAccessFile called with null user or fileId");
            return false;
        }
        
        // Admin and Dean have full access
        if (hasAdminLevelAccess(user)) {
            log.debug("User {} has admin-level access to file {}", user.getEmail(), fileId);
            return true;
        }
        
        // Fetch file with uploader and department info
        Optional<UploadedFile> fileOpt = uploadedFileRepository.findByIdWithUploaderAndFolder(fileId);
        if (fileOpt.isEmpty()) {
            log.warn("File not found: {}", fileId);
            return false;
        }
        
        return canAccessFile(user, fileOpt.get());
    }
    
    @Override
    public boolean canAccessFile(User user, UploadedFile file) {
        if (user == null || file == null) {
            log.warn("canAccessFile called with null user or file");
            return false;
        }

        
        // Admin and Dean have full access
        if (hasAdminLevelAccess(user)) {
            log.debug("User {} has admin-level access to file {}", user.getEmail(), file.getId());
            return true;
        }
        
        User uploader = file.getUploader();
        if (uploader == null) {
            log.warn("File {} has no uploader information", file.getId());
            // If no uploader info, deny access for non-admin users
            return false;
        }
        
        switch (user.getRole()) {
            case ROLE_HOD:
                // HOD can access files from professors in their department
                return canHodAccessFile(user, uploader);
                
            case ROLE_PROFESSOR:
                // Professor can only access their own files
                return canProfessorAccessFile(user, uploader);
                
            default:
                log.warn("Unknown role {} for user {}", user.getRole(), user.getEmail());
                return false;
        }
    }
    
    /**
     * Check if HOD can access a file based on department.
     * HOD can access files uploaded by professors in their department.
     */
    private boolean canHodAccessFile(User hod, User uploader) {
        Department hodDepartment = hod.getDepartment();
        Department uploaderDepartment = uploader.getDepartment();
        
        if (hodDepartment == null) {
            log.warn("HOD {} has no department assigned", hod.getEmail());
            return false;
        }
        
        if (uploaderDepartment == null) {
            log.warn("Uploader {} has no department assigned", uploader.getEmail());
            return false;
        }
        
        boolean canAccess = hodDepartment.getId().equals(uploaderDepartment.getId());
        log.debug("HOD {} {} access file from {} (department match: {})", 
            hod.getEmail(), 
            canAccess ? "can" : "cannot", 
            uploader.getEmail(),
            canAccess);
        
        return canAccess;
    }
    
    /**
     * Check if Professor can access a file.
     * Professor can only access their own files.
     */
    private boolean canProfessorAccessFile(User professor, User uploader) {
        boolean canAccess = professor.getId().equals(uploader.getId());
        log.debug("Professor {} {} access file from {} (own file: {})", 
            professor.getEmail(), 
            canAccess ? "can" : "cannot", 
            uploader.getEmail(),
            canAccess);
        
        return canAccess;
    }
    
    @Override
    public List<UploadedFile> getAccessibleFiles(User user) {
        if (user == null) {
            log.warn("getAccessibleFiles called with null user");
            return new ArrayList<>();
        }
        
        // Admin and Dean can access all files
        if (hasAdminLevelAccess(user)) {
            log.debug("Returning all files for admin-level user {}", user.getEmail());
            return uploadedFileRepository.findAll();
        }
        
        switch (user.getRole()) {
            case ROLE_HOD:
                return getFilesForHod(user);
                
            case ROLE_PROFESSOR:
                return getFilesForProfessor(user);
                
            default:
                log.warn("Unknown role {} for user {}", user.getRole(), user.getEmail());
                return new ArrayList<>();
        }
    }
    
    /**
     * Get files accessible to HOD (files from professors in their department).
     */
    private List<UploadedFile> getFilesForHod(User hod) {
        Department department = hod.getDepartment();
        if (department == null) {
            log.warn("HOD {} has no department assigned", hod.getEmail());
            return new ArrayList<>();
        }
        
        // Get all files and filter by department
        // This could be optimized with a custom query if performance is a concern
        return uploadedFileRepository.findAll().stream()
            .filter(file -> {
                User uploader = file.getUploader();
                if (uploader == null || uploader.getDepartment() == null) {
                    return false;
                }
                return department.getId().equals(uploader.getDepartment().getId());
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get files accessible to Professor (their own files).
     */
    private List<UploadedFile> getFilesForProfessor(User professor) {
        return uploadedFileRepository.findByUploaderId(professor.getId());
    }
    
    @Override
    public List<UploadedFile> getAccessibleFilesByDepartment(User user, Long departmentId) {
        if (user == null) {
            log.warn("getAccessibleFilesByDepartment called with null user");
            return new ArrayList<>();
        }
        
        // Only Admin and Dean can filter by department
        if (!hasAdminLevelAccess(user)) {
            log.warn("User {} does not have permission to filter by department", user.getEmail());
            return new ArrayList<>();
        }
        
        if (departmentId == null) {
            // Return all files if no department filter
            return uploadedFileRepository.findAll();
        }
        
        // Filter files by department
        return uploadedFileRepository.findAll().stream()
            .filter(file -> {
                User uploader = file.getUploader();
                if (uploader == null || uploader.getDepartment() == null) {
                    return false;
                }
                return departmentId.equals(uploader.getDepartment().getId());
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public void logAccessDenial(User user, Long fileId, String reason) {
        String userId = user != null ? String.valueOf(user.getId()) : "null";
        String userEmail = user != null ? user.getEmail() : "unknown";
        String userRole = user != null && user.getRole() != null ? user.getRole().name() : "unknown";
        
        log.warn("FILE ACCESS DENIED - User: {} (ID: {}, Role: {}), File: {}, Reason: {}, Timestamp: {}",
            userEmail, userId, userRole, fileId, reason, LocalDateTime.now());
    }
    
    @Override
    public boolean hasAdminLevelAccess(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_DEANSHIP;
    }
    
    @Override
    public boolean canAccessDepartmentFiles(User user, Long departmentId) {
        if (user == null || departmentId == null) {
            return false;
        }
        
        // Admin and Dean can access all departments
        if (hasAdminLevelAccess(user)) {
            return true;
        }
        
        // HOD can only access their own department
        if (user.getRole() == Role.ROLE_HOD) {
            Department userDepartment = user.getDepartment();
            if (userDepartment == null) {
                return false;
            }
            return departmentId.equals(userDepartment.getId());
        }
        
        // Professor can only access their own department
        if (user.getRole() == Role.ROLE_PROFESSOR) {
            Department userDepartment = user.getDepartment();
            if (userDepartment == null) {
                return false;
            }
            return departmentId.equals(userDepartment.getId());
        }
        
        return false;
    }
}
