package com.alqude.edu.ArchiveSystem.dto.report;

import com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum;
import com.alqude.edu.ArchiveSystem.entity.SubmissionStatus;
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
