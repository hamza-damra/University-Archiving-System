package com.alquds.edu.ArchiveSystem.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary of submission statistics for a single department
 * Used in system-wide reports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentReportSummary {
    
    private Long departmentId;
    private String departmentName;
    private SubmissionStatistics statistics;
}
