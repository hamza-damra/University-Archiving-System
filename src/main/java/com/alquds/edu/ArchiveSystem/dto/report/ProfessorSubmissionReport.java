package com.alquds.edu.ArchiveSystem.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Report showing professor submission status for a semester
 * Used by HOD to monitor department submission compliance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorSubmissionReport {
    
    private Long semesterId;
    private String semesterName;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime generatedAt;
    private String generatedBy;
    
    private List<ProfessorSubmissionRow> rows;
    private SubmissionStatistics statistics;
}
