package com.alquds.edu.ArchiveSystem.mapper.submission;

import com.alquds.edu.ArchiveSystem.dto.common.SubmittedDocumentResponse;
import com.alquds.edu.ArchiveSystem.dto.common.FileAttachmentResponse;

import com.alquds.edu.ArchiveSystem.entity.submission.DocumentRequest;

import com.alquds.edu.ArchiveSystem.dto.request.DocumentRequestCreateRequest;
import com.alquds.edu.ArchiveSystem.dto.request.DocumentRequestResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * LEGACY MAPPER - ARCHIVED
 * 
 * This mapper is part of the old request-based document system.
 * 
 * DO NOT USE FOR NEW DEVELOPMENT
 * 
 * @deprecated Part of legacy request-based system
 */
@Deprecated(since = "2.0", forRemoval = false)
public interface DocumentRequestMapper {
    
    @Deprecated(since = "2.0")
    DocumentRequest toEntity(DocumentRequestCreateRequest request);
    
    @Deprecated(since = "2.0")
    DocumentRequestResponse toResponse(DocumentRequest documentRequest);
    
    @Deprecated(since = "2.0")
    default SubmittedDocumentResponse mapSubmittedDocument(DocumentRequest documentRequest) {
        if (documentRequest.getSubmittedDocument() == null) {
            return null;
        }
        var doc = documentRequest.getSubmittedDocument();
        
        // Map file attachments if any
        List<FileAttachmentResponse> attachments = 
            doc.getFileAttachments() != null ? 
            doc.getFileAttachments().stream()
                .map(attachment -> FileAttachmentResponse.builder()
                        .id(attachment.getId())
                        .originalFilename(attachment.getOriginalFilename())
                        .fileName(attachment.getOriginalFilename())
                        .fileUrl(attachment.getFileUrl())
                        .fileSize(attachment.getFileSize())
                        .fileType(attachment.getFileType())
                        .fileOrder(attachment.getFileOrder())
                        .description(attachment.getDescription())
                        .createdAt(attachment.getCreatedAt())
                        .updatedAt(attachment.getUpdatedAt())
                        .build())
                .collect(Collectors.toList()) : 
            null;
        
        return SubmittedDocumentResponse.builder()
                .id(doc.getId())
                .requestId(documentRequest.getId())
                .originalFilename(doc.getOriginalFilename())
                .fileName(doc.getOriginalFilename())
                .fileUrl(doc.getFileUrl())
                .fileSize(doc.getFileSize())
                .fileType(doc.getFileType())
                .fileCount(doc.getFileCount())
                .totalFileSize(doc.getTotalFileSize())
                .notes(doc.getNotes())
                .fileAttachments(attachments)
                .submittedAt(doc.getSubmittedAt())
                .isLateSubmission(doc.getIsLateSubmission())
                .submittedLate(doc.getIsLateSubmission())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
