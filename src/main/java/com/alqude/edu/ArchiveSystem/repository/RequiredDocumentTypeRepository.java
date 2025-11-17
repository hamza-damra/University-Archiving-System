package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.RequiredDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequiredDocumentTypeRepository extends JpaRepository<RequiredDocumentType, Long> {
    
    List<RequiredDocumentType> findByCourseId(Long courseId);
    
    List<RequiredDocumentType> findByCourseIdAndSemesterId(Long courseId, Long semesterId);
    
    List<RequiredDocumentType> findByCourseIdAndDocumentType(
            Long courseId, 
            com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum documentType);
}
