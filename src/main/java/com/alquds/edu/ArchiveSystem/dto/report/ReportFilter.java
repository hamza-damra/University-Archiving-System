package com.alquds.edu.ArchiveSystem.dto.report;

import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filter criteria for report filtering
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilter {
    
    private String courseCode;
    private DocumentTypeEnum documentType;
    private SubmissionStatus status;
}
