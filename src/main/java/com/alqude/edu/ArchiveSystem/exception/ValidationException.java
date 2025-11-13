package com.alqude.edu.ArchiveSystem.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {
    
    private final Map<String, String> validationErrors;
    
    public ValidationException(String message, Map<String, String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }
    
    public ValidationException(String message, Map<String, String> validationErrors, Throwable cause) {
        super(message, cause);
        this.validationErrors = validationErrors;
    }
}
