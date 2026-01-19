package com.alquds.edu.ArchiveSystem.dto.task;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for updating an existing task.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {
    
    private String title;
    
    private String description;
    
    @Min(value = 0, message = "Weight percentage must be >= 0")
    @Max(value = 100, message = "Weight percentage must be <= 100")
    private Integer weightPercentage;
    
    @Min(value = 0, message = "Progress percentage must be >= 0")
    @Max(value = 100, message = "Progress percentage must be <= 100")
    private Integer progressPercentage;
    
    private LocalDate deadline;
    
    private Long fileReferenceId; // File link (null to remove link, 0 to clear)
    
    /**
     * List of file IDs to attach as evidence.
     * If provided, replaces all existing evidence files.
     * Empty list removes all evidence.
     * Null means no change to existing evidence.
     */
    private List<Long> evidenceFileIds;
}
