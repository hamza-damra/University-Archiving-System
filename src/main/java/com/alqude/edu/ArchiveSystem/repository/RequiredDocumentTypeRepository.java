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
    
    /**
     * Batch fetch required document types for multiple courses in a single query.
     * This optimizes performance by avoiding N+1 query problems.
     * 
     * @param courseIds List of course IDs
     * @param semesterId Semester ID
     * @return List of required document types for all specified courses
     */
    List<RequiredDocumentType> findByCourseIdInAndSemesterId(List<Long> courseIds, Long semesterId);
}
