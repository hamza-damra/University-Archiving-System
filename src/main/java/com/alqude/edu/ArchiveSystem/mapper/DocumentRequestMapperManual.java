package com.alqude.edu.ArchiveSystem.mapper;

import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestResponse;
import com.alqude.edu.ArchiveSystem.entity.DocumentRequest;
import org.springframework.stereotype.Component;

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
@Component("documentRequestMapper")
public class DocumentRequestMapperManual implements DocumentRequestMapper {
    
    @Override
    public DocumentRequest toEntity(DocumentRequestCreateRequest request) {
        if (request == null) {
            return null;
        }
        
        DocumentRequest documentRequest = new DocumentRequest();
        documentRequest.setCourseName(request.getCourseName());
        documentRequest.setDocumentType(request.getDocumentType());
        documentRequest.setRequiredFileExtensions(request.getRequiredFileExtensions());
        documentRequest.setDeadline(request.getDeadline());
        documentRequest.setDescription(request.getDescription());
        
        return documentRequest;
    }
    
    @Override
    public DocumentRequestResponse toResponse(DocumentRequest documentRequest) {
        if (documentRequest == null) {
            return null;
        }
        
        DocumentRequestResponse response = new DocumentRequestResponse();
        response.setId(documentRequest.getId());
        response.setCourseName(documentRequest.getCourseName());
        response.setDocumentType(documentRequest.getDocumentType());
        response.setRequiredFileExtensions(documentRequest.getRequiredFileExtensions());
        response.setDeadline(documentRequest.getDeadline());
        response.setDescription(documentRequest.getDescription());
        response.setMaxFileCount(documentRequest.getMaxFileCount());
        response.setMaxTotalSizeMb(documentRequest.getMaxTotalSizeMb());
        response.setCreatedAt(documentRequest.getCreatedAt());
        response.setUpdatedAt(documentRequest.getUpdatedAt());
        
        // Map professor name
        if (documentRequest.getProfessor() != null) {
            response.setProfessorName(documentRequest.getProfessor().getFirstName() + " " + 
                                     documentRequest.getProfessor().getLastName());
            response.setProfessorEmail(documentRequest.getProfessor().getEmail());
        }
        
        // Map created by name
        if (documentRequest.getCreatedBy() != null) {
            response.setCreatedByName(documentRequest.getCreatedBy().getFirstName() + " " + 
                                     documentRequest.getCreatedBy().getLastName());
        }
        
        // Map submitted document info
        if (documentRequest.getSubmittedDocument() != null) {
            response.setIsSubmitted(true);
            response.setIsLateSubmission(documentRequest.getSubmittedDocument().getIsLateSubmission());
            response.setSubmittedAt(documentRequest.getSubmittedDocument().getSubmittedAt());
            response.setSubmittedDocument(mapSubmittedDocument(documentRequest));
        } else {
            response.setIsSubmitted(false);
        }
        
        return response;
    }
    
    @Override
    public com.alqude.edu.ArchiveSystem.dto.common.SubmittedDocumentResponse mapSubmittedDocument(com.alqude.edu.ArchiveSystem.entity.DocumentRequest documentRequest) {
        if (documentRequest == null || documentRequest.getSubmittedDocument() == null) {
            return null;
        }
        
        var doc = documentRequest.getSubmittedDocument();
        
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
                .fileAttachments(null) // Simplified for now
                .submittedAt(doc.getSubmittedAt())
                .isLateSubmission(doc.getIsLateSubmission())
                .submittedLate(doc.getIsLateSubmission())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
