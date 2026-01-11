package com.alquds.edu.ArchiveSystem.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for department-specific chart data.
 * Used for department distribution charts in dashboards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentChartData {
    
    /**
     * Department ID
     */
    private Long departmentId;
    
    /**
     * Department name for display
     */
    private String departmentName;
    
    /**
     * Department shortcut identifier
     */
    private String departmentShortcut;
    
    /**
     * Count of submissions for this department
     */
    private Long submissionCount;
    
    /**
     * Count of professors in this department
     */
    private Long professorCount;
    
    /**
     * Count of courses in this department
     */
    private Long courseCount;
}
