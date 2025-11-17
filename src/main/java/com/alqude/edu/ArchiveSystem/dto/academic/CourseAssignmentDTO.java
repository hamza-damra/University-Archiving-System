package com.alqude.edu.ArchiveSystem.dto.academic;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseAssignmentDTO {
    
    @NotNull(message = "Semester ID is required")
    private Long semesterId;
    
    @NotNull(message = "Course ID is required")
    private Long courseId;
    
    @NotNull(message = "Professor ID is required")
    private Long professorId;
    
    private Boolean isActive;
}
