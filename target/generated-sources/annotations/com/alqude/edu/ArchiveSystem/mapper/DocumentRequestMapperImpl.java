package com.alqude.edu.ArchiveSystem.mapper;

import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestResponse;
import com.alqude.edu.ArchiveSystem.entity.DocumentRequest;
import com.alqude.edu.ArchiveSystem.entity.SubmittedDocument;
import com.alqude.edu.ArchiveSystem.entity.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-13T03:02:16+0200",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251023-0518, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class DocumentRequestMapperImpl implements DocumentRequestMapper {

    @Override
    public DocumentRequest toEntity(DocumentRequestCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        DocumentRequest documentRequest = new DocumentRequest();

        documentRequest.setCourseName( request.getCourseName() );
        documentRequest.setDeadline( request.getDeadline() );
        documentRequest.setDescription( request.getDescription() );
        documentRequest.setDocumentType( request.getDocumentType() );
        List<String> list = request.getRequiredFileExtensions();
        if ( list != null ) {
            documentRequest.setRequiredFileExtensions( new ArrayList<String>( list ) );
        }

        return documentRequest;
    }

    @Override
    public DocumentRequestResponse toResponse(DocumentRequest documentRequest) {
        if ( documentRequest == null ) {
            return null;
        }

        DocumentRequestResponse documentRequestResponse = new DocumentRequestResponse();

        documentRequestResponse.setProfessorEmail( documentRequestProfessorEmail( documentRequest ) );
        documentRequestResponse.setIsLateSubmission( documentRequestSubmittedDocumentIsLateSubmission( documentRequest ) );
        documentRequestResponse.setSubmittedAt( documentRequestSubmittedDocumentSubmittedAt( documentRequest ) );
        documentRequestResponse.setCourseName( documentRequest.getCourseName() );
        documentRequestResponse.setCreatedAt( documentRequest.getCreatedAt() );
        documentRequestResponse.setDeadline( documentRequest.getDeadline() );
        documentRequestResponse.setDescription( documentRequest.getDescription() );
        documentRequestResponse.setDocumentType( documentRequest.getDocumentType() );
        documentRequestResponse.setId( documentRequest.getId() );
        List<String> list = documentRequest.getRequiredFileExtensions();
        if ( list != null ) {
            documentRequestResponse.setRequiredFileExtensions( new ArrayList<String>( list ) );
        }
        documentRequestResponse.setUpdatedAt( documentRequest.getUpdatedAt() );

        documentRequestResponse.setProfessorName( documentRequest.getProfessor().getFirstName() + " " + documentRequest.getProfessor().getLastName() );
        documentRequestResponse.setCreatedByName( documentRequest.getCreatedBy().getFirstName() + " " + documentRequest.getCreatedBy().getLastName() );
        documentRequestResponse.setIsSubmitted( documentRequest.getSubmittedDocument() != null );

        return documentRequestResponse;
    }

    private String documentRequestProfessorEmail(DocumentRequest documentRequest) {
        if ( documentRequest == null ) {
            return null;
        }
        User professor = documentRequest.getProfessor();
        if ( professor == null ) {
            return null;
        }
        String email = professor.getEmail();
        if ( email == null ) {
            return null;
        }
        return email;
    }

    private Boolean documentRequestSubmittedDocumentIsLateSubmission(DocumentRequest documentRequest) {
        if ( documentRequest == null ) {
            return null;
        }
        SubmittedDocument submittedDocument = documentRequest.getSubmittedDocument();
        if ( submittedDocument == null ) {
            return null;
        }
        Boolean isLateSubmission = submittedDocument.getIsLateSubmission();
        if ( isLateSubmission == null ) {
            return null;
        }
        return isLateSubmission;
    }

    private LocalDateTime documentRequestSubmittedDocumentSubmittedAt(DocumentRequest documentRequest) {
        if ( documentRequest == null ) {
            return null;
        }
        SubmittedDocument submittedDocument = documentRequest.getSubmittedDocument();
        if ( submittedDocument == null ) {
            return null;
        }
        LocalDateTime submittedAt = submittedDocument.getSubmittedAt();
        if ( submittedAt == null ) {
            return null;
        }
        return submittedAt;
    }
}
