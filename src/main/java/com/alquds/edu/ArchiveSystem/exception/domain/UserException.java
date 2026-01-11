package com.alquds.edu.ArchiveSystem.exception.domain;

import com.alquds.edu.ArchiveSystem.exception.core.BusinessException;

import java.util.List;

public class UserException extends BusinessException {
    
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String INVALID_EMAIL_FORMAT = "INVALID_EMAIL_FORMAT";
    public static final String WEAK_PASSWORD = "WEAK_PASSWORD";
    public static final String DEPARTMENT_NOT_FOUND = "DEPARTMENT_NOT_FOUND";
    public static final String INVALID_ROLE = "INVALID_ROLE";
    public static final String USER_HAS_DEPENDENCIES = "USER_HAS_DEPENDENCIES";
    public static final String CANNOT_DELETE_SELF = "CANNOT_DELETE_SELF";
    public static final String INVALID_USER_DATA = "INVALID_USER_DATA";
    
    public UserException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public UserException(String errorCode, String message, List<String> suggestions) {
        super(errorCode, message, suggestions);
    }
    
    public UserException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public static UserException userNotFound(Long userId) {
        return new UserException(
            USER_NOT_FOUND,
            "User not found with ID: " + userId,
            List.of("Verify the user ID", "Check if the user was deleted", "Contact administrator")
        );
    }
    
    public static UserException emailAlreadyExists(String email) {
        return new UserException(
            EMAIL_ALREADY_EXISTS,
            "Email already exists: " + email,
            List.of("Use a different email address", "Check if you already have an account", "Contact administrator if this is an error")
        );
    }
    
    public static UserException departmentNotFound(Long departmentId) {
        return new UserException(
            DEPARTMENT_NOT_FOUND,
            "Department not found with ID: " + departmentId,
            List.of("Verify the department ID", "Check available departments", "Contact administrator")
        );
    }
    
    public static UserException invalidEmailFormat(String email) {
        return new UserException(
            INVALID_EMAIL_FORMAT,
            "Invalid email format: " + email,
            List.of("Use a valid email format (e.g., user@example.com)", "Check for typos")
        );
    }
    
    public static UserException weakPassword() {
        return new UserException(
            WEAK_PASSWORD,
            "Password does not meet security requirements",
            List.of(
                "Use at least 8 characters",
                "Include uppercase and lowercase letters",
                "Include at least one number",
                "Include at least one special character"
            )
        );
    }
    
    public static UserException invalidRole(String role) {
        return new UserException(
            INVALID_ROLE,
            "Invalid role: " + role,
            List.of("Use valid roles: ROLE_PROFESSOR, ROLE_HOD", "Contact administrator")
        );
    }
    
    public static UserException userHasDependencies(Long userId, String dependencies) {
        return new UserException(
            USER_HAS_DEPENDENCIES,
            "Cannot delete user with ID " + userId + ". User has existing " + dependencies,
            List.of(
                "Remove or reassign " + dependencies + " first",
                "Archive the user instead of deleting",
                "Contact administrator for assistance"
            )
        );
    }
    
    public static UserException cannotDeleteSelf() {
        return new UserException(
            CANNOT_DELETE_SELF,
            "Cannot delete your own account",
            List.of("Ask another administrator to delete your account", "Deactivate your account instead")
        );
    }
}
