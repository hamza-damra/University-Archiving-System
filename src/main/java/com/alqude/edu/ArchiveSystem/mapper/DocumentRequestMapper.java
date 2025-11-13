package com.alqude.edu.ArchiveSystem.mapper;

import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestResponse;
import com.alqude.edu.ArchiveSystem.entity.DocumentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DocumentRequestMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "professor", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "submittedDocument", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DocumentRequest toEntity(DocumentRequestCreateRequest request);
    
    @Mapping(target = "professorName", 
             expression = "java(documentRequest.getProfessor().getFirstName() + \" \" + documentRequest.getProfessor().getLastName())")
    @Mapping(source = "professor.email", target = "professorEmail")
    @Mapping(target = "createdByName", 
             expression = "java(documentRequest.getCreatedBy().getFirstName() + \" \" + documentRequest.getCreatedBy().getLastName())")
    @Mapping(target = "isSubmitted", 
             expression = "java(documentRequest.getSubmittedDocument() != null)")
    @Mapping(source = "submittedDocument.isLateSubmission", target = "isLateSubmission")
    @Mapping(source = "submittedDocument.submittedAt", target = "submittedAt")
    DocumentRequestResponse toResponse(DocumentRequest documentRequest);
}
