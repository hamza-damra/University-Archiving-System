package com.alqude.edu.ArchiveSystem.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    
    @NotBlank(message = "Course code is required")
    private String courseCode; // e.g., "CS101"
    
    @NotBlank(message = "Course name is required")
    private String courseName; // e.g., "Database Systems"
    
    @NotNull(message = "Department ID is required")
    private Long departmentId;
    
    private String level; // e.g., "Undergraduate", "Graduate"
    
    private String description;
    
    private Boolean isActive;
}
