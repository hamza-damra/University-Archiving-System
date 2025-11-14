package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.SubmittedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmittedDocumentRepository extends JpaRepository<SubmittedDocument, Long> {
    
    Optional<SubmittedDocument> findByDocumentRequestId(Long documentRequestId);
    
    List<SubmittedDocument> findByProfessorId(Long professorId);
    
    @Query("SELECT sd FROM SubmittedDocument sd WHERE sd.professor.department.id = :departmentId")
    List<SubmittedDocument> findByDepartmentId(@Param("departmentId") Long departmentId);
    
    @Query("SELECT sd FROM SubmittedDocument sd WHERE sd.documentRequest.id IN :requestIds")
    List<SubmittedDocument> findByDocumentRequestIds(@Param("requestIds") List<Long> requestIds);
    
    @Query("SELECT COUNT(sd) FROM SubmittedDocument sd WHERE sd.professor.id = :professorId AND sd.isLateSubmission = true")
    long countLateSubmissionsByProfessor(@Param("professorId") Long professorId);
    
    @Query("SELECT COUNT(sd) FROM SubmittedDocument sd WHERE sd.professor.department.id = :departmentId AND sd.isLateSubmission = true")
    long countLateSubmissionsByDepartment(@Param("departmentId") Long departmentId);
}
