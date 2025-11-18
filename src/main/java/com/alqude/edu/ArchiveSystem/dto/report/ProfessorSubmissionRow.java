package com.alqude.edu.ArchiveSystem.dto.report;

import com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Single row in professor submission report
 * Shows one professor's submission status for their courses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorSubmissionRow {
    
    private Long professorId;
    private String professorName;
    private String professorEmail;
    private Long courseAssignmentId;
    private String courseCode;
    private String courseName;
    
    // Map of document type to submission status info (includes status, deadline, etc.)
    private Map<DocumentTypeEnum, DocumentStatusInfo> documentStatuses;
}
