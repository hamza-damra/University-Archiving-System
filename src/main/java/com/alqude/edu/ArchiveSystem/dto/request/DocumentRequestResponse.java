package com.alqude.edu.ArchiveSystem.dto.request;

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
    private Boolean isSubmitted;
    private Boolean isLateSubmission;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
