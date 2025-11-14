package com.alqude.edu.ArchiveSystem.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorSubmissionSummary {
    
    private Long professorId;
    private String professorName;
    private String professorEmail;
    private String departmentName;
    
    private int totalRequests;
    private int submittedRequests;
    private int pendingRequests;
    private int overdueRequests;
    
    private int submittedOnTime;
    private int submittedLate;
    
    private Double completionRate; // Percentage of submitted requests
    private Double onTimeRate; // Percentage of on-time submissions
    
    private LocalDateTime lastSubmissionDate;
    private LocalDateTime earliestPendingDeadline;
}
