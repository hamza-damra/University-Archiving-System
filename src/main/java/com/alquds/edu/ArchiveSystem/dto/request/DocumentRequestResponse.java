package com.alquds.edu.ArchiveSystem.dto.request;

import com.alquds.edu.ArchiveSystem.dto.common.SubmittedDocumentResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequestResponse {
    
    private Long id;
    private String courseName;
    private String documentType;
    private List<String> requiredFileExtensions;
    private LocalDateTime deadline;
    private String professorName;
    private String professorEmail;
    private String createdByName;
    private String description;
    
    // Multi-file upload settings
    private Integer maxFileCount;
    private Integer maxTotalSizeMb;
    
    private Boolean isSubmitted;
    private Boolean isLateSubmission;
    private LocalDateTime submittedAt;
    private SubmittedDocumentResponse submittedDocument;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
