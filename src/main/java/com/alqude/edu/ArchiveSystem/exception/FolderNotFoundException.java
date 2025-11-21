package com.alqude.edu.ArchiveSystem.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Exception thrown when a requested folder cannot be found.
 */
public class FolderNotFoundException extends ArchiveSystemException {
    
    public static final String FOLDER_NOT_FOUND = "FOLDER_NOT_FOUND";
    public static final String FOLDER_NOT_FOUND_BY_ID = "FOLDER_NOT_FOUND_BY_ID";
    public static final String FOLDER_NOT_FOUND_BY_PATH = "FOLDER_NOT_FOUND_BY_PATH";
    
    public FolderNotFoundException(String errorCode, String message, HttpStatus httpStatus) {
        super(errorCode, message, httpStatus);
    }
    
    public FolderNotFoundException(String errorCode, String message, HttpStatus httpStatus, List<String> suggestions) {
        super(errorCode, message, httpStatus, suggestions);
    }
    
    public FolderNotFoundException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
    }
    
    // Convenience factory methods
    
    public static FolderNotFoundException byId(Long folderId) {
        return new FolderNotFoundException(
            FOLDER_NOT_FOUND_BY_ID,
            "Folder not found with ID: " + folderId,
            HttpStatus.NOT_FOUND,
            List.of("Verify the folder ID", "Check if folder was deleted", "Refresh the folder list")
        );
    }
    
    public static FolderNotFoundException byPath(String path) {
        return new FolderNotFoundException(
            FOLDER_NOT_FOUND_BY_PATH,
            "Folder not found at path: " + path,
            HttpStatus.NOT_FOUND,
            List.of("Verify the folder path", "Check if folder exists", "Refresh the folder tree")
        );
    }
    
    public static FolderNotFoundException notFound(String message) {
        return new FolderNotFoundException(
            FOLDER_NOT_FOUND,
            message,
            HttpStatus.NOT_FOUND,
            List.of("Verify the folder exists", "Check folder permissions", "Contact administrator")
        );
    }
}
