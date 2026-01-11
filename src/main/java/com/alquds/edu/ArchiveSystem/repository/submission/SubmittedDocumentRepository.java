package com.alquds.edu.ArchiveSystem.repository.submission;

import com.alquds.edu.ArchiveSystem.entity.submission.SubmittedDocument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * LEGACY REPOSITORY - ARCHIVED
 * 
 * This repository is part of the old request-based document system.
 * 
 * Replacement repository:
 * - DocumentSubmissionRepository: For document submissions in semester-based
 * system
 * 
 * This repository is kept for:
 * 1. Historical data access
 * 2. Data migration operations
 * 3. Rollback capability
 * 
 * DO NOT USE FOR NEW DEVELOPMENT
 * 
 * @deprecated Replaced by DocumentSubmissionRepository
 * @see com.alquds.edu.ArchiveSystem.repository.DocumentSubmissionRepository
 */
@Deprecated(since = "2.0", forRemoval = false)
@Repository
public interface SubmittedDocumentRepository extends JpaRepository<SubmittedDocument, Long> {

    @Deprecated
    Optional<SubmittedDocument> findByDocumentRequestId(Long documentRequestId);

    @Deprecated
    List<SubmittedDocument> findByProfessorId(Long professorId);

    @Deprecated
    @Query("SELECT sd FROM SubmittedDocument sd WHERE sd.professor.department.id = :departmentId")
    List<SubmittedDocument> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Deprecated
    @Query("SELECT sd FROM SubmittedDocument sd WHERE sd.documentRequest.id IN :requestIds")
    List<SubmittedDocument> findByDocumentRequestIds(@Param("requestIds") List<Long> requestIds);

    @Deprecated
    @Query("SELECT COUNT(sd) FROM SubmittedDocument sd WHERE sd.professor.id = :professorId AND sd.isLateSubmission = true")
    long countLateSubmissionsByProfessor(@Param("professorId") Long professorId);

    @Deprecated
    @Query("SELECT COUNT(sd) FROM SubmittedDocument sd WHERE sd.professor.department.id = :departmentId AND sd.isLateSubmission = true")
    long countLateSubmissionsByDepartment(@Param("departmentId") Long departmentId);
}
