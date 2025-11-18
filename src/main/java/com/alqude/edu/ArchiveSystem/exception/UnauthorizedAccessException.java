package com.alqude.edu.ArchiveSystem.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Exception thrown when a user attempts an unauthorized operation (403 errors).
 */
public class UnauthorizedAccessException extends ArchiveSystemException {
    
    public static final String UNAUTHORIZED_ACCESS = "UNAUTHORIZED_ACCESS";
    public static final String DEPARTMENT_ACCESS_DENIED = "DEPARTMENT_ACCESS_DENIED";
    public static final String COURSE_ACCESS_DENIED = "COURSE_ACCESS_DENIED";
    public static final String FILE_ACCESS_DENIED = "FILE_ACCESS_DENIED";
    public static final String SUBMISSION_ACCESS_DENIED = "SUBMISSION_ACCESS_DENIED";
    public static final String OPERATION_NOT_ALLOWED = "OPERATION_NOT_ALLOWED";
    public static final String INSUFFICIENT_PERMISSIONS = "INSUFFICIENT_PERMISSIONS";
    
    public UnauthorizedAccessException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.FORBIDDEN);
    }
    
    public UnauthorizedAccessException(String errorCode, String message, List<String> suggestions) {
        super(errorCode, message, HttpStatus.FORBIDDEN, suggestions);
    }
    
    public UnauthorizedAccessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.FORBIDDEN, cause);
    }
    
    // Convenience factory methods
    
    public static UnauthorizedAccessException departmentAccess(String userRole, Long departmentId) {
        return new UnauthorizedAccessException(
            DEPARTMENT_ACCESS_DENIED,
            String.format("User with role %s cannot access department with ID: %d", userRole, departmentId),
            List.of("Check your department assignment", "Contact administrator for access", "Login with appropriate role")
        );
    }
    
    public static UnauthorizedAccessException courseAccess(Long userId, Long courseId) {
        return new UnauthorizedAccessException(
            COURSE_ACCESS_DENIED,
            String.format("User %d is not authorized to access course %d", userId, courseId),
            List.of("Verify course assignment", "Check if you are assigned to this course", "Contact administrator")
        );
    }
    
    public static UnauthorizedAccessException fileAccess(Long userId, Long fileId, String operation) {
        return new UnauthorizedAccessException(
            FILE_ACCESS_DENIED,
            String.format("User %d is not authorized to %s file %d", userId, operation, fileId),
            List.of("Check file permissions", "Verify you own this file", "Contact administrator")
        );
    }
    
    public static UnauthorizedAccessException submissionAccess(Long userId, Long submissionId) {
        return new UnauthorizedAccessException(
            SUBMISSION_ACCESS_DENIED,
            String.format("User %d is not authorized to access submission %d", userId, submissionId),
            List.of("Verify submission ownership", "Check if you are assigned to this course", "Contact administrator")
        );
    }
    
    public static UnauthorizedAccessException operationNotAllowed(String operation, String reason) {
        return new UnauthorizedAccessException(
            OPERATION_NOT_ALLOWED,
            String.format("Operation '%s' is not allowed: %s", operation, reason),
            List.of("Check operation requirements", "Verify your permissions", "Contact administrator")
        );
    }
    
    public static UnauthorizedAccessException insufficientPermissions(String requiredRole, String userRole) {
        return new UnauthorizedAccessException(
            INSUFFICIENT_PERMISSIONS,
            String.format("Insufficient permissions. Required role: %s, Current role: %s", requiredRole, userRole),
            List.of("Login with appropriate role", "Contact administrator for role assignment")
        );
    }
    
    public static UnauthorizedAccessException professorCannotModifyOthersFiles(Long professorId) {
        return new UnauthorizedAccessException(
            FILE_ACCESS_DENIED,
            "Professors can only modify their own files",
            List.of("You can only upload/replace files for your assigned courses", "Contact the file owner for changes")
        );
    }
    
    public static UnauthorizedAccessException hodCannotManageProfessors() {
        return new UnauthorizedAccessException(
            OPERATION_NOT_ALLOWED,
            "HOD users cannot create, edit, or delete professor records",
            List.of("Contact Deanship for professor management", "HOD role has read-only access to professor data")
        );
    }
    
    public static UnauthorizedAccessException deadlinePassed(String documentType) {
        return new UnauthorizedAccessException(
            OPERATION_NOT_ALLOWED,
            String.format("Cannot upload %s - deadline has passed", documentType),
            List.of("Contact HOD for deadline extension", "Check submission deadlines")
        );
    }
}
