package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.DocumentSubmission;
import com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum;
import com.alqude.edu.ArchiveSystem.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentSubmissionRepository extends JpaRepository<DocumentSubmission, Long> {
    
    @Query("SELECT ds FROM DocumentSubmission ds WHERE ds.professor.id = :professorId AND ds.courseAssignment.semester.id = :semesterId")
    List<DocumentSubmission> findByProfessorIdAndCourseAssignment_SemesterId(
            @Param("professorId") Long professorId, 
            @Param("semesterId") Long semesterId);
    
    List<DocumentSubmission> findByCourseAssignmentId(Long courseAssignmentId);
    
    @Query("SELECT ds FROM DocumentSubmission ds WHERE ds.status = :status AND ds.courseAssignment.semester.id = :semesterId")
    List<DocumentSubmission> findByStatusAndCourseAssignment_Semester_Id(
            @Param("status") SubmissionStatus status, 
            @Param("semesterId") Long semesterId);
    
    Optional<DocumentSubmission> findByCourseAssignmentIdAndDocumentType(
            Long courseAssignmentId, 
            DocumentTypeEnum documentType);
    
    List<DocumentSubmission> findAllByCourseAssignmentIdAndDocumentType(
            Long courseAssignmentId, 
            DocumentTypeEnum documentType);
}
