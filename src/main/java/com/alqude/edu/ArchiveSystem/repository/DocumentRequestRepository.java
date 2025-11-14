package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.DocumentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long> {
    
    @Query("SELECT dr FROM DocumentRequest dr " +
           "LEFT JOIN FETCH dr.professor " +
           "LEFT JOIN FETCH dr.createdBy " +
           "LEFT JOIN FETCH dr.submittedDocument " +
           "WHERE dr.professor.id = :professorId")
    List<DocumentRequest> findByProfessorId(@Param("professorId") Long professorId);
    
    @Query("SELECT dr FROM DocumentRequest dr " +
           "LEFT JOIN FETCH dr.professor " +
           "LEFT JOIN FETCH dr.createdBy " +
           "LEFT JOIN FETCH dr.submittedDocument " +
           "WHERE dr.professor.id = :professorId")
    Page<DocumentRequest> findByProfessorId(@Param("professorId") Long professorId, Pageable pageable);
    
    @Query("SELECT dr FROM DocumentRequest dr " +
           "LEFT JOIN FETCH dr.professor " +
           "LEFT JOIN FETCH dr.createdBy " +
           "LEFT JOIN FETCH dr.submittedDocument " +
           "WHERE dr.createdBy.id = :createdById")
    List<DocumentRequest> findByCreatedById(@Param("createdById") Long createdById);
    
    @Query("SELECT dr FROM DocumentRequest dr " +
           "LEFT JOIN FETCH dr.professor " +
           "LEFT JOIN FETCH dr.createdBy " +
           "LEFT JOIN FETCH dr.submittedDocument " +
           "WHERE dr.createdBy.id = :createdById")
    Page<DocumentRequest> findByCreatedById(@Param("createdById") Long createdById, Pageable pageable);
    
    @Query("SELECT dr FROM DocumentRequest dr " +
           "LEFT JOIN FETCH dr.professor " +
           "LEFT JOIN FETCH dr.createdBy " +
           "LEFT JOIN FETCH dr.submittedDocument " +
           "WHERE dr.professor.department.id = :departmentId")
    List<DocumentRequest> findByDepartmentId(@Param("departmentId") Long departmentId);
    
    @Query("SELECT dr FROM DocumentRequest dr " +
           "LEFT JOIN FETCH dr.professor " +
           "LEFT JOIN FETCH dr.createdBy " +
           "LEFT JOIN FETCH dr.submittedDocument " +
           "WHERE dr.professor.department.id = :departmentId")
    Page<DocumentRequest> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);
    
    @Query("SELECT dr FROM DocumentRequest dr " +
           "LEFT JOIN FETCH dr.professor " +
           "LEFT JOIN FETCH dr.createdBy " +
           "WHERE dr.deadline < :now AND dr.submittedDocument IS NULL")
    List<DocumentRequest> findOverdueRequests(@Param("now") LocalDateTime now);
    
    @Query("SELECT dr FROM DocumentRequest dr " +
           "LEFT JOIN FETCH dr.professor " +
           "LEFT JOIN FETCH dr.createdBy " +
           "WHERE dr.deadline BETWEEN :start AND :end AND dr.submittedDocument IS NULL")
    List<DocumentRequest> findRequestsWithUpcomingDeadline(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(dr) FROM DocumentRequest dr WHERE dr.professor.id = :professorId AND dr.submittedDocument IS NULL")
    long countPendingRequestsByProfessor(@Param("professorId") Long professorId);
    
    // Count methods for dependency checking
    long countByCreatedBy_Id(Long userId);
    
    long countByProfessor_Id(Long userId);
}
