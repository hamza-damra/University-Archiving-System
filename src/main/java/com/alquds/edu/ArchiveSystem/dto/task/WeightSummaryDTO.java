package com.alquds.edu.ArchiveSystem.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for weight distribution summary per course.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeightSummaryDTO {
    
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Long semesterId;
    
    // Map of task ID to weight percentage
    private Map<Long, Integer> taskWeights;
    
    // Total weight percentage (should be 100)
    private Integer totalWeightPercentage;
    
    // Validation status
    private Boolean isValid; // true if totalWeightPercentage == 100
    private String validationMessage;
}
