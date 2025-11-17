package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.report.SubmissionStatistics;
import com.alqude.edu.ArchiveSystem.entity.DocumentSubmission;
import com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum;
import com.alqude.edu.ArchiveSystem.entity.SubmissionStatus;

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
