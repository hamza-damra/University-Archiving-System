package com.alquds.edu.ArchiveSystem.dto.fileexplorer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new folder in the file explorer.
 * Used by professors to create custom folders within their own namespace.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFolderRequest {
    
    /**
     * The parent path where the folder will be created.
     * Must be within the professor's own namespace.
     * Example: "/2025-2026/first/John Doe/CS101/lecture_notes"
     */
    @NotBlank(message = "Path cannot be empty")
    private String path;
    
    /**
     * The name of the folder to create.
     * Cannot contain invalid filesystem characters: / \ ? * : < > |
     */
    @NotBlank(message = "Folder name cannot be empty")
    @Size(max = 128, message = "Folder name must not exceed 128 characters")
    @Pattern(
        regexp = "^[^/\\\\?*:<>|\"]+$",
        message = "Folder name cannot contain invalid characters: / \\ ? * : < > | \""
    )
    private String folderName;
}
