package com.alqude.edu.ArchiveSystem.dto.professor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dashboard overview for Professor showing summary statistics for a semester
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorDashboardOverview {
    
    private Long professorId;
    private String professorName;
    private String professorEmail;
    private String departmentName;
    
    private Long semesterId;
    private String semesterName;
    private String academicYear;
    private LocalDateTime generatedAt;
    
    private Integer totalCourses;
    private Integer totalRequiredDocuments;
    private Integer submittedDocuments;
    private Integer missingDocuments;
    private Integer overdueDocuments;
    
    // Percentage calculations
    private Double completionPercentage;
}
