package com.alquds.edu.ArchiveSystem.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for dashboard statistics containing counts for various entities.
 * Used by Admin and Dean dashboards to display summary widgets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatistics {
    
    /**
     * Total count of professors in the system
     */
    private Long totalProfessors;
    
    /**
     * Total count of HODs in the system
     */
    private Long totalHods;
    
    /**
     * Total count of departments in the system
     */
    private Long totalDepartments;
    
    /**
     * Total count of courses in the system
     */
    private Long totalCourses;
    
    /**
     * Total count of all submissions in the system
     */
    private Long totalSubmissions;
    
    /**
     * Count of submissions within the current academic period
     */
    private Long recentSubmissions;
    
    /**
     * Count of pending/not-uploaded submissions
     */
    private Long pendingSubmissions;
    
    /**
     * Timestamp when these statistics were generated
     */
    private LocalDateTime generatedAt;
}
