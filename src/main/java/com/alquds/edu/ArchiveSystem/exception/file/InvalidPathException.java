package com.alquds.edu.ArchiveSystem.exception.file;

/**
 * Exception thrown when a file path is invalid or contains dangerous patterns.
 * Used by SafePathResolver to indicate path validation failures.
 */
public class InvalidPathException extends RuntimeException {
    
    public InvalidPathException(String message) {
        super(message);
    }
    
    public InvalidPathException(String message, Throwable cause) {
        super(message, cause);
    }
}
