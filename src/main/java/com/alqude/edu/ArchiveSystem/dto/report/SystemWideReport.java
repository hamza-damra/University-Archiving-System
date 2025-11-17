package com.alqude.edu.ArchiveSystem.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * System-wide report for Deanship
 * Aggregates submission data across all departments
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemWideReport {
    
    private Long semesterId;
    private String semesterName;
    private LocalDateTime generatedAt;
    private String generatedBy;
    
    private List<DepartmentReportSummary> departmentSummaries;
    private SubmissionStatistics overallStatistics;
}
