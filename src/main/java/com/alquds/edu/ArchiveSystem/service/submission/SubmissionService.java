package com.alquds.edu.ArchiveSystem.service.submission;

import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;

import com.alquds.edu.ArchiveSystem.dto.report.SubmissionStatistics;

import java.time.LocalDateTime;
import java.util.List;

public interface SubmissionService {
    
    // Submission Management
    DocumentSubmission createSubmission(Long courseAssignmentId, DocumentTypeEnum documentType, 
                                       Long professorId, String notes);
    
    DocumentSubmission updateSubmission(Long submissionId, String notes);
    
    DocumentSubmission getSubmission(Long submissionId);
    
    // Submission Status
    List<DocumentSubmission> getSubmissionsByProfessor(Long professorId, Long semesterId);
    
    List<DocumentSubmission> getSubmissionsByCourse(Long courseAssignmentId);
    
    SubmissionStatus calculateSubmissionStatus(DocumentSubmission submission, LocalDateTime deadline);
    
    // Submission Statistics
    SubmissionStatistics getStatisticsBySemester(Long semesterId, Long departmentId);
    
    SubmissionStatistics getStatisticsByProfessor(Long professorId, Long semesterId);
}
