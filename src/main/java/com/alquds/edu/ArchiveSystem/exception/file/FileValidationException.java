package com.alquds.edu.ArchiveSystem.exception.file;

import com.alquds.edu.ArchiveSystem.exception.core.ArchiveSystemException;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Exception thrown when file validation fails during upload.
 * This includes validation for file size, type, name, and other file properties.
 */
public class FileValidationException extends ArchiveSystemException {
    
    public static final String FILE_EMPTY = "FILE_EMPTY";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String INVALID_FILE_TYPE = "INVALID_FILE_TYPE";
    public static final String INVALID_FILENAME = "INVALID_FILENAME";
    public static final String FILE_VALIDATION_FAILED = "FILE_VALIDATION_FAILED";
    
    public FileValidationException(String errorCode, String message, HttpStatus httpStatus) {
        super(errorCode, message, httpStatus);
    }
    
    public FileValidationException(String errorCode, String message, HttpStatus httpStatus, List<String> suggestions) {
        super(errorCode, message, httpStatus, suggestions);
    }
    
    public FileValidationException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
    }
    
    // Convenience factory methods
    
    public static FileValidationException fileEmpty() {
        return new FileValidationException(
            FILE_EMPTY,
            "File is empty",
            HttpStatus.BAD_REQUEST,
            List.of("Select a valid file", "Ensure the file contains data")
        );
    }
    
    public static FileValidationException fileTooLarge(String fileName, long fileSize, long maxSize) {
        return new FileValidationException(
            FILE_TOO_LARGE,
            String.format("File '%s' size (%d bytes) exceeds maximum allowed size of %d bytes", 
                fileName, fileSize, maxSize),
            HttpStatus.BAD_REQUEST,
            List.of("Compress the file", "Split large files into smaller parts", "Contact administrator for size limit increase")
        );
    }
    
    public static FileValidationException invalidFileType(String fileName, String extension, String allowedTypes) {
        return new FileValidationException(
            INVALID_FILE_TYPE,
            String.format("File type '%s' is not allowed for file '%s'. Allowed types: %s", 
                extension, fileName, allowedTypes),
            HttpStatus.BAD_REQUEST,
            List.of("Convert file to allowed format", "Check file extension", "Verify file requirements")
        );
    }
    
    public static FileValidationException invalidFilename(String fileName) {
        return new FileValidationException(
            INVALID_FILENAME,
            "Filename is empty or invalid: " + fileName,
            HttpStatus.BAD_REQUEST,
            List.of("Provide a valid filename", "Check filename format")
        );
    }
    
    public static FileValidationException validationFailed(String message) {
        return new FileValidationException(
            FILE_VALIDATION_FAILED,
            message,
            HttpStatus.BAD_REQUEST,
            List.of("Check file requirements", "Verify file properties", "Try again with a valid file")
        );
    }
}
