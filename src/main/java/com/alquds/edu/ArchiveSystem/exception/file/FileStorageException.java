package com.alquds.edu.ArchiveSystem.exception.file;

import com.alquds.edu.ArchiveSystem.exception.core.ArchiveSystemException;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Exception thrown when file storage operations fail.
 * This includes errors during file system operations like creating directories,
 * writing files, or reading files from disk.
 */
public class FileStorageException extends ArchiveSystemException {
    
    public static final String DIRECTORY_CREATION_FAILED = "DIRECTORY_CREATION_FAILED";
    public static final String FILE_WRITE_FAILED = "FILE_WRITE_FAILED";
    public static final String FILE_READ_FAILED = "FILE_READ_FAILED";
    public static final String FILE_DELETE_FAILED = "FILE_DELETE_FAILED";
    public static final String STORAGE_ERROR = "STORAGE_ERROR";
    public static final String INSUFFICIENT_STORAGE = "INSUFFICIENT_STORAGE";
    
    public FileStorageException(String errorCode, String message, HttpStatus httpStatus) {
        super(errorCode, message, httpStatus);
    }
    
    public FileStorageException(String errorCode, String message, HttpStatus httpStatus, List<String> suggestions) {
        super(errorCode, message, httpStatus, suggestions);
    }
    
    public FileStorageException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
    }
    
    // Convenience factory methods
    
    public static FileStorageException directoryCreationFailed(String path, Throwable cause) {
        return new FileStorageException(
            DIRECTORY_CREATION_FAILED,
            "Failed to create directory: " + path,
            HttpStatus.INTERNAL_SERVER_ERROR,
            List.of("Check file system permissions", "Verify disk space availability", "Contact system administrator"),
            cause
        );
    }
    
    public static FileStorageException fileWriteFailed(String fileName, Throwable cause) {
        return new FileStorageException(
            FILE_WRITE_FAILED,
            "Failed to write file: " + fileName,
            HttpStatus.INTERNAL_SERVER_ERROR,
            List.of("Try uploading again", "Check disk space", "Contact system administrator"),
            cause
        );
    }
    
    public static FileStorageException fileReadFailed(String fileName, Throwable cause) {
        return new FileStorageException(
            FILE_READ_FAILED,
            "Failed to read file: " + fileName,
            HttpStatus.INTERNAL_SERVER_ERROR,
            List.of("Verify file exists", "Check file permissions", "Contact system administrator"),
            cause
        );
    }
    
    public static FileStorageException fileDeleteFailed(String fileName, Throwable cause) {
        return new FileStorageException(
            FILE_DELETE_FAILED,
            "Failed to delete file: " + fileName,
            HttpStatus.INTERNAL_SERVER_ERROR,
            List.of("Check file permissions", "Verify file is not in use", "Contact system administrator"),
            cause
        );
    }
    
    public static FileStorageException storageError(String details) {
        return new FileStorageException(
            STORAGE_ERROR,
            "Storage error: " + details,
            HttpStatus.INTERNAL_SERVER_ERROR,
            List.of("Try again later", "Contact system administrator", "Check storage availability")
        );
    }
    
    public static FileStorageException insufficientStorage() {
        return new FileStorageException(
            INSUFFICIENT_STORAGE,
            "Insufficient storage space available",
            HttpStatus.INSUFFICIENT_STORAGE,
            List.of("Contact system administrator", "Free up disk space", "Try uploading smaller files")
        );
    }
    
    // Constructor with cause for convenience
    private FileStorageException(String errorCode, String message, HttpStatus httpStatus, 
                                 List<String> suggestions, Throwable cause) {
        super(errorCode, message, httpStatus, suggestions);
        initCause(cause);
    }
}
