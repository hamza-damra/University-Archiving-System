package com.alqude.edu.ArchiveSystem.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Department create/update operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO {
    
    @NotBlank(message = "Department name is required")
    @Size(max = 255, message = "Department name must not exceed 255 characters")
    private String name;
    
    @NotBlank(message = "Department shortcut is required")
    @Size(max = 20, message = "Shortcut must not exceed 20 characters")
    @Pattern(regexp = "^[a-z0-9]+$", message = "Shortcut must contain only lowercase letters and numbers")
    private String shortcut;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
