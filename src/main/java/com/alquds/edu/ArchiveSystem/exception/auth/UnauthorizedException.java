package com.alquds.edu.ArchiveSystem.exception.auth;

import com.alquds.edu.ArchiveSystem.exception.core.ArchiveSystemException;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Exception thrown when a user is not authorized to perform an operation.
 * This is specifically for file upload authorization checks.
 */
public class UnauthorizedException extends ArchiveSystemException {
    
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String UPLOAD_NOT_AUTHORIZED = "UPLOAD_NOT_AUTHORIZED";
    public static final String FOLDER_ACCESS_DENIED = "FOLDER_ACCESS_DENIED";
    public static final String INSUFFICIENT_ROLE = "INSUFFICIENT_ROLE";
    
    public UnauthorizedException(String errorCode, String message, HttpStatus httpStatus) {
        super(errorCode, message, httpStatus);
    }
    
    public UnauthorizedException(String errorCode, String message, HttpStatus httpStatus, List<String> suggestions) {
        super(errorCode, message, httpStatus, suggestions);
    }
    
    public UnauthorizedException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
    }
    
    // Convenience factory methods
    
    public static UnauthorizedException uploadNotAuthorized(Long userId, Long folderId) {
        return new UnauthorizedException(
            UPLOAD_NOT_AUTHORIZED,
            String.format("User %d is not authorized to upload files to folder %d", userId, folderId),
            HttpStatus.FORBIDDEN,
            List.of("You can only upload to your own course folders", "Contact administrator for access", "Verify folder ownership")
        );
    }
    
    public static UnauthorizedException folderAccessDenied(String folderName) {
        return new UnauthorizedException(
            FOLDER_ACCESS_DENIED,
            "Access denied to folder: " + folderName,
            HttpStatus.FORBIDDEN,
            List.of("Check folder permissions", "Verify you are assigned to this course", "Contact administrator")
        );
    }
    
    public static UnauthorizedException insufficientRole(String requiredRole, String currentRole) {
        return new UnauthorizedException(
            INSUFFICIENT_ROLE,
            String.format("Insufficient role. Required: %s, Current: %s", requiredRole, currentRole),
            HttpStatus.FORBIDDEN,
            List.of("Login with appropriate role", "Contact administrator for role assignment")
        );
    }
    
    public static UnauthorizedException unauthorized(String message) {
        return new UnauthorizedException(
            UNAUTHORIZED,
            message,
            HttpStatus.FORBIDDEN,
            List.of("Check your permissions", "Contact administrator", "Verify your access rights")
        );
    }
}
