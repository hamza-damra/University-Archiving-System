package com.alqude.edu.ArchiveSystem.security;

import com.alqude.edu.ArchiveSystem.entity.DocumentSubmission;
import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.repository.DocumentSubmissionRepository;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for custom security expressions used in @PreAuthorize annotations.
 * Provides methods to check ownership and permissions for various entities.
 */
@Service("securityExpressionService")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class SecurityExpressionService {
    
    private final UserRepository userRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;
    
    /**
     * Check if the current user owns the specified submission.
     * 
     * @param submissionId the submission ID
     * @return true if the current user is the professor who created the submission
     */
    public boolean ownsSubmission(Long submissionId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            log.warn("No authenticated user found");
            return false;
        }
        
        DocumentSubmission submission = documentSubmissionRepository.findById(submissionId)
                .orElse(null);
        
        if (submission == null) {
            log.warn("Submission not found with ID: {}", submissionId);
            return false;
        }
        
        boolean owns = submission.getProfessor().getId().equals(currentUser.getId());
        log.debug("User {} owns submission {}: {}", currentUser.getEmail(), submissionId, owns);
        return owns;
    }
    
    /**
     * Check if the current user is in the same department as the specified user.
     * 
     * @param userId the user ID to check
     * @return true if both users are in the same department
     */
    public boolean isSameDepartment(Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getDepartment() == null) {
            log.warn("Current user or department not found");
            return false;
        }
        
        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null || targetUser.getDepartment() == null) {
            log.warn("Target user or department not found for user ID: {}", userId);
            return false;
        }
        
        boolean sameDept = currentUser.getDepartment().getId().equals(targetUser.getDepartment().getId());
        log.debug("User {} same department as user {}: {}", 
                currentUser.getEmail(), userId, sameDept);
        return sameDept;
    }
    
    /**
     * Check if the current user has HOD role and is in the same department as the target user.
     * 
     * @param userId the user ID to check
     * @return true if current user is HOD and in same department
     */
    public boolean isHodOfDepartment(Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getRole() != Role.ROLE_HOD) {
            return false;
        }
        
        return isSameDepartment(userId);
    }
    
    /**
     * Check if the current user is the specified user or has Deanship role.
     * 
     * @param userId the user ID to check
     * @return true if current user is the target user or has Deanship role
     */
    public boolean isSelfOrDeanship(Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        
        return currentUser.getId().equals(userId) || 
               currentUser.getRole() == Role.ROLE_DEANSHIP;
    }
    
    /**
     * Check if the current user has access to the specified department.
     * - Deanship: access to all departments
     * - HOD: access only to their own department
     * - Professor: access only to their own department
     * 
     * @param departmentId the department ID to check
     * @return true if user has access to the department
     */
    public boolean hasAccessToDepartment(Long departmentId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        
        // Deanship has access to all departments
        if (currentUser.getRole() == Role.ROLE_DEANSHIP) {
            return true;
        }
        
        // HOD and Professor only have access to their own department
        if (currentUser.getDepartment() == null) {
            log.warn("User {} has no department assigned", currentUser.getEmail());
            return false;
        }
        
        return currentUser.getDepartment().getId().equals(departmentId);
    }
    
    /**
     * Get the current authenticated user.
     * 
     * @return the current user or null if not authenticated
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email).orElse(null);
    }
}
