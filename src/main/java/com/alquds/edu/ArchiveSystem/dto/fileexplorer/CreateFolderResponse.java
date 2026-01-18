package com.alquds.edu.ArchiveSystem.dto.fileexplorer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for folder creation operations.
 * Returns the details of the newly created folder.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFolderResponse {
    
    /**
     * Status of the operation (e.g., "success")
     */
    private String status;
    
    /**
     * The full path of the created folder
     * Example: "/2025-2026/first/John Doe/CS101/lecture_notes/Assignments"
     */
    private String fullPath;
    
    /**
     * The name of the created folder
     */
    private String folderName;
    
    /**
     * The ID of the created folder entity (if stored in database)
     */
    private Long folderId;
    
    /**
     * Timestamp when the folder was created
     */
    private LocalDateTime createdAt;
    
    /**
     * Factory method to create a success response
     */
    public static CreateFolderResponse success(String fullPath, String folderName, Long folderId) {
        return CreateFolderResponse.builder()
                .status("success")
                .fullPath(fullPath)
                .folderName(folderName)
                .folderId(folderId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
