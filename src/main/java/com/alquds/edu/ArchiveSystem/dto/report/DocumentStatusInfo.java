package com.alquds.edu.ArchiveSystem.dto.report;

import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Information about a document submission status
 * Includes the status and deadline for a specific document type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatusInfo {
    
    private SubmissionStatus status;
    private LocalDateTime deadline;
    private LocalDateTime submittedAt;
    private Boolean isLateSubmission;
}
