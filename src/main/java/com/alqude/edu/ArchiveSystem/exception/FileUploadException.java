package com.alqude.edu.ArchiveSystem.exception;

import java.util.List;

public class FileUploadException extends BusinessException {
    
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String INVALID_FILE_TYPE = "INVALID_FILE_TYPE";
    public static final String FILE_UPLOAD_FAILED = "FILE_UPLOAD_FAILED";
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    public static final String FILE_PROCESSING_ERROR = "FILE_PROCESSING_ERROR";
    public static final String STORAGE_ERROR = "STORAGE_ERROR";
    
    public FileUploadException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public FileUploadException(String errorCode, String message, List<String> suggestions) {
        super(errorCode, message, suggestions);
    }
    
    public FileUploadException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public static FileUploadException fileTooLarge(long maxSize) {
        return new FileUploadException(
            FILE_TOO_LARGE, 
            "File size exceeds maximum allowed size of " + maxSize + " bytes",
            List.of("Compress the file", "Split large files into smaller parts", "Contact administrator for size limit increase")
        );
    }
    
    public static FileUploadException invalidFileType(String fileName, List<String> allowedTypes) {
        return new FileUploadException(
            INVALID_FILE_TYPE, 
            "File type not allowed for file: " + fileName + ". Allowed types: " + String.join(", ", allowedTypes),
            List.of("Convert file to allowed format", "Check file extension", "Verify file requirements")
        );
    }
    
    public static FileUploadException uploadFailed(String fileName, String reason) {
        return new FileUploadException(
            FILE_UPLOAD_FAILED, 
            "Failed to upload file: " + fileName + ". Reason: " + reason,
            List.of("Try uploading again", "Check internet connection", "Verify file is not corrupted")
        );
    }
    
    public static FileUploadException fileNotFound(Long fileId) {
        return new FileUploadException(
            FILE_NOT_FOUND, 
            "File not found with ID: " + fileId,
            List.of("Verify the file ID", "Check if file was deleted", "Contact support if file should exist")
        );
    }
    
    public static FileUploadException storageError(String details) {
        return new FileUploadException(
            STORAGE_ERROR, 
            "Storage error: " + details,
            List.of("Try again later", "Contact system administrator", "Check storage availability")
        );
    }
}
