package com.alqude.edu.ArchiveSystem.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dashboard overview for HOD showing summary statistics for a semester
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverview {
    
    private Long semesterId;
    private String semesterName;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime generatedAt;
    
    private Integer totalProfessors;
    private Integer totalCourses;
    private Integer totalCourseAssignments;
    
    private SubmissionStatistics submissionStatistics;
}
