package com.alquds.edu.ArchiveSystem.exception.core;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base exception class for all Archive System exceptions.
 * Provides consistent error handling with error codes, HTTP status, and suggestions.
 */
@Getter
public class ArchiveSystemException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final List<String> suggestions;
    
    public ArchiveSystemException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.suggestions = null;
    }
    
    public ArchiveSystemException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.suggestions = null;
    }
    
    public ArchiveSystemException(String errorCode, String message, HttpStatus httpStatus, List<String> suggestions) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.suggestions = suggestions;
    }
    
    public ArchiveSystemException(String errorCode, String message, HttpStatus httpStatus, Throwable cause, List<String> suggestions) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.suggestions = suggestions;
    }
}
