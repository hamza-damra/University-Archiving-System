package com.alquds.edu.ArchiveSystem.exception.file;

import com.alquds.edu.ArchiveSystem.exception.core.ArchiveSystemException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Exception thrown when a folder name is invalid.
 * Returns HTTP 400 Bad Request status.
 */
public class InvalidFolderNameException extends ArchiveSystemException {
    
    public static final String INVALID_FOLDER_NAME = "INVALID_FOLDER_NAME";
    
    public InvalidFolderNameException(String folderName, String reason) {
        super(
            INVALID_FOLDER_NAME,
            String.format("Invalid folder name '%s': %s", folderName, reason),
            HttpStatus.BAD_REQUEST,
            List.of(
                "Folder name cannot be empty",
                "Folder name cannot contain: / \\ ? * : < > | \"",
                "Folder name must not exceed 128 characters",
                "Avoid leading or trailing spaces"
            )
        );
    }
    
    public InvalidFolderNameException(String message) {
        super(INVALID_FOLDER_NAME, message, HttpStatus.BAD_REQUEST);
    }
    
    public static InvalidFolderNameException empty() {
        return new InvalidFolderNameException("", "Folder name cannot be empty");
    }
    
    public static InvalidFolderNameException tooLong(String folderName) {
        return new InvalidFolderNameException(folderName, "Folder name exceeds maximum length of 128 characters");
    }
    
    public static InvalidFolderNameException invalidCharacters(String folderName) {
        return new InvalidFolderNameException(folderName, "Contains invalid characters (/ \\ ? * : < > | \")");
    }
}
