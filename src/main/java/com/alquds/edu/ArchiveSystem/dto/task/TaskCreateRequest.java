package com.alquds.edu.ArchiveSystem.dto.task;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for creating a new task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {
    
    @NotBlank(message = "Task title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Weight percentage is required")
    @Min(value = 0, message = "Weight percentage must be >= 0")
    @Max(value = 100, message = "Weight percentage must be <= 100")
    private Integer weightPercentage;
    
    @NotNull(message = "Course ID is required")
    private Long courseId;
    
    @NotNull(message = "Semester ID is required")
    private Long semesterId;
    
    private LocalDate deadline;
    
    private Long fileReferenceId; // Optional file link (legacy)
    
    /**
     * List of file IDs to attach as evidence.
     * Files must be owned by or accessible to the professor.
     */
    @Builder.Default
    private List<Long> evidenceFileIds = new ArrayList<>();
}
