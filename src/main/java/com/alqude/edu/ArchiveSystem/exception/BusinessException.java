package com.alqude.edu.ArchiveSystem.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    private final List<String> suggestions;
    
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.suggestions = null;
    }
    
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.suggestions = null;
    }
    
    public BusinessException(String errorCode, String message, List<String> suggestions) {
        super(message);
        this.errorCode = errorCode;
        this.suggestions = suggestions;
    }
    
    public BusinessException(String errorCode, String message, Throwable cause, List<String> suggestions) {
        super(message, cause);
        this.errorCode = errorCode;
        this.suggestions = suggestions;
    }
}
