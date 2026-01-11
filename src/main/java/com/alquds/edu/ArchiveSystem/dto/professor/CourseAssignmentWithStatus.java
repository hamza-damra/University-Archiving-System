package com.alquds.edu.ArchiveSystem.dto.professor;

import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO representing a course assignment with submission status for professor dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAssignmentWithStatus {
    
    private Long courseAssignmentId;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String courseLevel;
    private String departmentName;
    
    private Long semesterId;
    private String semesterType;
    private String academicYear;
    
    // Map of document type to its submission status and details
    private Map<DocumentTypeEnum, DocumentTypeStatus> documentStatuses;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentTypeStatus {
        private DocumentTypeEnum documentType;
        private SubmissionStatus status;
        private LocalDateTime deadline;
        private Boolean isRequired;
        private Integer maxFileCount;
        private Integer maxTotalSizeMb;
        
        // Submission details if exists
        private Long submissionId;
        private LocalDateTime submittedAt;
        private Boolean isLateSubmission;
        private Integer fileCount;
        private Long totalFileSize;
        private String notes;
    }
}
