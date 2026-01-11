package com.alquds.edu.ArchiveSystem.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSubmissionReport {
    
    private String departmentName;
    private LocalDateTime generatedAt;
    private String generatedBy;
    
    private int totalProfessors;
    private int totalRequests;
    private int totalSubmitted;
    private int totalPending;
    private int totalOverdue;
    
    private Double overallCompletionRate;
    private Double overallOnTimeRate;
    
    private List<ProfessorSubmissionSummary> professorSummaries;
}
