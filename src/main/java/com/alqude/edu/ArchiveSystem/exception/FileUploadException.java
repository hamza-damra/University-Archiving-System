package com.alqude.edu.ArchiveSystem.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Exception thrown for file upload and storage related errors.
 */
public class FileUploadException extends ArchiveSystemException {
    
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String INVALID_FILE_TYPE = "INVALID_FILE_TYPE";
    public static final String FILE_UPLOAD_FAILED = "FILE_UPLOAD_FAILED";
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    public static final String FILE_PROCESSING_ERROR = "FILE_PROCESSING_ERROR";
    public static final String STORAGE_ERROR = "STORAGE_ERROR";
    public static final String FILE_COUNT_EXCEEDED = "FILE_COUNT_EXCEEDED";
    public static final String TOTAL_SIZE_EXCEEDED = "TOTAL_SIZE_EXCEEDED";
    
    public FileUploadException(String errorCode, String message, HttpStatus httpStatus) {
        super(errorCode, message, httpStatus);
    }
    
    public FileUploadException(String errorCode, String message, HttpStatus httpStatus, List<String> suggestions) {
        super(errorCode, message, httpStatus, suggestions);
    }
    
    public FileUploadException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
    }
    
    // Convenience factory methods
    
    public static FileUploadException fileTooLarge(long maxSize) {
        return new FileUploadException(
            FILE_TOO_LARGE, 
            "File size exceeds maximum allowed size of " + maxSize + " bytes",
            HttpStatus.PAYLOAD_TOO_LARGE,
            List.of("Compress the file", "Split large files into smaller parts", "Contact administrator for size limit increase")
        );
    }
    
    public static FileUploadException fileTooLarge(String fileName, long fileSize, long maxSize) {
        return new FileUploadException(
            FILE_TOO_LARGE, 
            String.format("File '%s' size (%d bytes) exceeds maximum allowed size of %d bytes", fileName, fileSize, maxSize),
            HttpStatus.PAYLOAD_TOO_LARGE,
            List.of("Compress the file", "Split large files into smaller parts", "Contact administrator for size limit increase")
        );
    }
    
    public static FileUploadException totalSizeExceeded(long totalSize, long maxTotalSize) {
        return new FileUploadException(
            TOTAL_SIZE_EXCEEDED, 
            String.format("Total upload size (%d MB) exceeds maximum allowed size of %d MB", totalSize / (1024 * 1024), maxTotalSize),
            HttpStatus.PAYLOAD_TOO_LARGE,
            List.of("Reduce number of files", "Compress files", "Upload in multiple batches")
        );
    }
    
    public static FileUploadException fileCountExceeded(int fileCount, int maxFileCount) {
        return new FileUploadException(
            FILE_COUNT_EXCEEDED, 
            String.format("Number of files (%d) exceeds maximum allowed count of %d", fileCount, maxFileCount),
            HttpStatus.BAD_REQUEST,
            List.of("Reduce number of files", "Upload in multiple batches", "Contact administrator")
        );
    }
    
    public static FileUploadException invalidFileType(String fileName, List<String> allowedTypes) {
        return new FileUploadException(
            INVALID_FILE_TYPE, 
            "File type not allowed for file: " + fileName + ". Allowed types: " + String.join(", ", allowedTypes),
            HttpStatus.BAD_REQUEST,
            List.of("Convert file to allowed format", "Check file extension", "Verify file requirements")
        );
    }
    
    public static FileUploadException uploadFailed(String fileName, String reason) {
        return new FileUploadException(
            FILE_UPLOAD_FAILED, 
            "Failed to upload file: " + fileName + ". Reason: " + reason,
            HttpStatus.INTERNAL_SERVER_ERROR,
            List.of("Try uploading again", "Check internet connection", "Verify file is not corrupted")
        );
    }
    
    public static FileUploadException fileNotFound(Long fileId) {
        return new FileUploadException(
            FILE_NOT_FOUND, 
            "File not found with ID: " + fileId,
            HttpStatus.NOT_FOUND,
            List.of("Verify the file ID", "Check if file was deleted", "Contact support if file should exist")
        );
    }
    
    public static FileUploadException storageError(String details) {
        return new FileUploadException(
            STORAGE_ERROR, 
            "Storage error: " + details,
            HttpStatus.INSUFFICIENT_STORAGE,
            List.of("Try again later", "Contact system administrator", "Check storage availability")
        );
    }
    
    public static FileUploadException physicalFileNotFound(String fileUrl) {
        return new FileUploadException(
            FILE_NOT_FOUND, 
            "The file could not be found on the server. It may have been moved or deleted from storage.",
            HttpStatus.NOT_FOUND,
            List.of("The file record exists but the physical file is missing", "Contact administrator for assistance")
        );
    }
    
    public static FileUploadException processingError(String fileName, String details) {
        return new FileUploadException(
            FILE_PROCESSING_ERROR, 
            String.format("Error processing file '%s': %s", fileName, details),
            HttpStatus.INTERNAL_SERVER_ERROR,
            List.of("Verify file is not corrupted", "Try uploading again", "Contact support")
        );
    }
}
