package com.alqude.edu.ArchiveSystem.mapper;

import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestResponse;
import com.alqude.edu.ArchiveSystem.entity.DocumentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

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
@SuppressWarnings({"deprecation", "all"})
@Mapper(componentModel = "default", 
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        suppressTimestampInGenerated = true)
public interface DocumentRequestMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "professor", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "submittedDocument", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DocumentRequest toEntity(DocumentRequestCreateRequest request);
    
    @Mapping(target = "professorName", 
             expression = "java(documentRequest.getProfessor() != null ? documentRequest.getProfessor().getFirstName() + \" \" + documentRequest.getProfessor().getLastName() : null)")
    @Mapping(source = "professor.email", target = "professorEmail")
    @Mapping(target = "createdByName", 
             expression = "java(documentRequest.getCreatedBy() != null ? documentRequest.getCreatedBy().getFirstName() + \" \" + documentRequest.getCreatedBy().getLastName() : null)")
    @Mapping(target = "isSubmitted", 
             expression = "java(documentRequest.getSubmittedDocument() != null)")
    @Mapping(source = "submittedDocument.isLateSubmission", target = "isLateSubmission")
    @Mapping(source = "submittedDocument.submittedAt", target = "submittedAt")
    @Mapping(target = "submittedDocument", expression = "java(mapSubmittedDocument(documentRequest))")
    DocumentRequestResponse toResponse(DocumentRequest documentRequest);
    
    default com.alqude.edu.ArchiveSystem.dto.common.SubmittedDocumentResponse mapSubmittedDocument(com.alqude.edu.ArchiveSystem.entity.DocumentRequest documentRequest) {
        if (documentRequest.getSubmittedDocument() == null) {
            return null;
        }
        var doc = documentRequest.getSubmittedDocument();
        
        // Map file attachments if any
        java.util.List<com.alqude.edu.ArchiveSystem.dto.common.FileAttachmentResponse> attachments = 
            doc.getFileAttachments() != null ? 
            doc.getFileAttachments().stream()
                .map(attachment -> com.alqude.edu.ArchiveSystem.dto.common.FileAttachmentResponse.builder()
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
                .collect(java.util.stream.Collectors.toList()) : 
            null;
        
        return com.alqude.edu.ArchiveSystem.dto.common.SubmittedDocumentResponse.builder()
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
