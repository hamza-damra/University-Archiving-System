package com.alquds.edu.ArchiveSystem.dto.fileexplorer;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for deleting a folder in the file explorer.
 * Used by professors to delete custom folders within their own namespace.
 * When a folder is deleted, all its contents (files and subfolders) are also deleted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteFolderRequest {
    
    /**
     * The full path of the folder to delete.
     * Must be within the professor's own namespace.
     * Example: "/2025-2026/first/John Doe/CS101/lecture_notes"
     */
    @NotBlank(message = "Folder path cannot be empty")
    private String folderPath;
}
