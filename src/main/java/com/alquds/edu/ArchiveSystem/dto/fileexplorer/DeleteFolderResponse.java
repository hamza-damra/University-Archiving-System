package com.alquds.edu.ArchiveSystem.dto.fileexplorer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for folder deletion operations.
 * Returns the details of the deleted folder.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteFolderResponse {
    
    /**
     * Status of the operation (e.g., "success")
     */
    private String status;
    
    /**
     * The full path of the deleted folder
     */
    private String deletedPath;
    
    /**
     * The name of the deleted folder
     */
    private String folderName;
    
    /**
     * Number of files deleted
     */
    private int filesDeleted;
    
    /**
     * Number of subfolders deleted
     */
    private int subfoldersDeleted;
    
    /**
     * Timestamp when the folder was deleted
     */
    private LocalDateTime deletedAt;
    
    /**
     * Factory method to create a success response
     */
    public static DeleteFolderResponse success(String deletedPath, String folderName, int filesDeleted, int subfoldersDeleted) {
        return DeleteFolderResponse.builder()
                .status("success")
                .deletedPath(deletedPath)
                .folderName(folderName)
                .filesDeleted(filesDeleted)
                .subfoldersDeleted(subfoldersDeleted)
                .deletedAt(LocalDateTime.now())
                .build();
    }
}
