package com.alqude.edu.ArchiveSystem.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for SubmittedDocument responses
 * Prevents Jackson serialization issues with Hibernate proxies
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmittedDocumentResponse {
    
    private Long id;
    private Long requestId;
    
    // Legacy single file fields (for backward compatibility)
    private String originalFilename;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    
    // Multi-file support
    private java.util.List<FileAttachmentResponse> fileAttachments;
    private Integer fileCount;
    private Long totalFileSize;
    private String notes;
    
    private Long professorId;
    private String professorName;
    private String professorEmail;
    private LocalDateTime submittedAt;
    private Boolean isLateSubmission;
    private Boolean submittedLate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
