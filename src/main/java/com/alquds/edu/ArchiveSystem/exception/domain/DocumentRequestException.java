package com.alquds.edu.ArchiveSystem.exception.domain;

import com.alquds.edu.ArchiveSystem.exception.core.BusinessException;

import java.util.List;

/**
 * LEGACY EXCEPTION - ARCHIVED
 * 
 * This exception is part of the old request-based document system.
 * 
 * DO NOT USE FOR NEW DEVELOPMENT
 * 
 * @deprecated Part of legacy request-based system
 */
@Deprecated(since = "2.0", forRemoval = false)
public class DocumentRequestException extends BusinessException {

    @Deprecated
    public static final String INVALID_DEADLINE = "INVALID_DEADLINE";
    @Deprecated
    public static final String PROFESSOR_NOT_FOUND = "PROFESSOR_NOT_FOUND";
    @Deprecated
    public static final String INVALID_FILE_EXTENSIONS = "INVALID_FILE_EXTENSIONS";
    @Deprecated
    public static final String REQUEST_NOT_FOUND = "REQUEST_NOT_FOUND";
    @Deprecated
    public static final String REQUEST_ALREADY_SUBMITTED = "REQUEST_ALREADY_SUBMITTED";
    @Deprecated
    public static final String DEADLINE_PASSED = "DEADLINE_PASSED";
    @Deprecated
    public static final String UNAUTHORIZED_ACCESS = "UNAUTHORIZED_ACCESS";

    @Deprecated
    public DocumentRequestException(String errorCode, String message) {
        super(errorCode, message);
    }

    @Deprecated
    public DocumentRequestException(String errorCode, String message, List<String> suggestions) {
        super(errorCode, message, suggestions);
    }

    @Deprecated
    public DocumentRequestException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    @Deprecated
    public static DocumentRequestException invalidDeadline(String details) {
        return new DocumentRequestException(
                INVALID_DEADLINE,
                "Invalid deadline: " + details,
                List.of("Deadline must be in the future", "Use format: yyyy-MM-dd HH:mm:ss"));
    }

    @Deprecated
    public static DocumentRequestException professorNotFound(Long professorId) {
        return new DocumentRequestException(
                PROFESSOR_NOT_FOUND,
                "Professor not found with ID: " + professorId,
                List.of("Verify the professor ID", "Ensure the professor exists in the system"));
    }

    @Deprecated
    public static DocumentRequestException requestNotFound(Long requestId) {
        return new DocumentRequestException(
                REQUEST_NOT_FOUND,
                "Document request not found with ID: " + requestId,
                List.of("Verify the request ID", "Check if the request was deleted"));
    }

    @Deprecated
    public static DocumentRequestException unauthorizedAccess(String details) {
        return new DocumentRequestException(
                UNAUTHORIZED_ACCESS,
                "Unauthorized access to document request: " + details,
                List.of("Ensure you have proper permissions", "Contact system administrator if needed"));
    }

    @Deprecated
    public static DocumentRequestException deadlinePassed(String courseName) {
        return new DocumentRequestException(
                DEADLINE_PASSED,
                "Deadline has passed for course: " + courseName,
                List.of("Contact the department head for extension", "Submit request for new deadline"));
    }
}
