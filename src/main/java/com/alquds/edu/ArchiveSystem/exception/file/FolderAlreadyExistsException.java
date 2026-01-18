package com.alquds.edu.ArchiveSystem.exception.file;

import com.alquds.edu.ArchiveSystem.exception.core.ArchiveSystemException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Exception thrown when a folder already exists at the specified path.
 * Returns HTTP 409 Conflict status.
 */
public class FolderAlreadyExistsException extends ArchiveSystemException {
    
    public static final String FOLDER_ALREADY_EXISTS = "FOLDER_ALREADY_EXISTS";
    
    public FolderAlreadyExistsException(String folderName, String parentPath) {
        super(
            FOLDER_ALREADY_EXISTS,
            String.format("A folder named '%s' already exists at '%s'", folderName, parentPath),
            HttpStatus.CONFLICT,
            List.of(
                "Choose a different folder name",
                "Navigate to the existing folder instead"
            )
        );
    }
    
    public FolderAlreadyExistsException(String message) {
        super(FOLDER_ALREADY_EXISTS, message, HttpStatus.CONFLICT);
    }
}
