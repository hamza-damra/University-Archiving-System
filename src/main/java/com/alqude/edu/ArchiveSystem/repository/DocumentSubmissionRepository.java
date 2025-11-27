package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.DocumentSubmission;
import com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum;
import com.alqude.edu.ArchiveSystem.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    
    /**
     * Batch fetch document submissions for multiple course assignments in a single query.
     * This optimizes performance by avoiding N+1 query problems.
     * 
     * @param courseAssignmentIds List of course assignment IDs
     * @return List of document submissions for all specified assignments
     */
    @Query("SELECT ds FROM DocumentSubmission ds " +
           "LEFT JOIN FETCH ds.uploadedFiles " +
           "WHERE ds.courseAssignment.id IN :courseAssignmentIds")
    List<DocumentSubmission> findByCourseAssignmentIdIn(@Param("courseAssignmentIds") List<Long> courseAssignmentIds);
    
    // ==================== Dashboard Analytics Queries ====================
    
    /**
     * Count submissions by status for a specific semester
     */
    @Query("SELECT ds.status, COUNT(ds) FROM DocumentSubmission ds " +
           "WHERE ds.courseAssignment.semester.id = :semesterId " +
           "GROUP BY ds.status")
    List<Object[]> countByStatusAndSemesterId(@Param("semesterId") Long semesterId);
    
    /**
     * Count all submissions by status (no filter)
     */
    @Query("SELECT ds.status, COUNT(ds) FROM DocumentSubmission ds GROUP BY ds.status")
    List<Object[]> countByStatus();
    
    /**
     * Count submissions by department for a specific semester
     */
    @Query("SELECT ds.professor.department.id, ds.professor.department.name, COUNT(ds) " +
           "FROM DocumentSubmission ds " +
           "WHERE ds.courseAssignment.semester.id = :semesterId " +
           "GROUP BY ds.professor.department.id, ds.professor.department.name")
    List<Object[]> countByDepartmentAndSemesterId(@Param("semesterId") Long semesterId);
    
    /**
     * Count all submissions by department (no filter)
     */
    @Query("SELECT ds.professor.department.id, ds.professor.department.name, COUNT(ds) " +
           "FROM DocumentSubmission ds " +
           "GROUP BY ds.professor.department.id, ds.professor.department.name")
    List<Object[]> countByDepartment();
    
    /**
     * Count submissions per day within a date range
     */
    @Query("SELECT FUNCTION('DATE', ds.submittedAt), COUNT(ds) " +
           "FROM DocumentSubmission ds " +
           "WHERE ds.submittedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('DATE', ds.submittedAt) " +
           "ORDER BY FUNCTION('DATE', ds.submittedAt)")
    List<Object[]> countSubmissionsPerDay(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find submissions within a date range
     */
    @Query("SELECT ds FROM DocumentSubmission ds " +
           "WHERE ds.submittedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ds.submittedAt DESC")
    List<DocumentSubmission> findBySubmittedAtBetween(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count submissions by academic year and semester
     */
    @Query("SELECT COUNT(ds) FROM DocumentSubmission ds " +
           "WHERE ds.courseAssignment.semester.academicYear.id = :academicYearId " +
           "AND ds.courseAssignment.semester.id = :semesterId")
    long countByAcademicYearAndSemester(
            @Param("academicYearId") Long academicYearId, 
            @Param("semesterId") Long semesterId);
    
    /**
     * Get recent submissions (last N days) ordered by date
     */
    @Query("SELECT ds FROM DocumentSubmission ds " +
           "WHERE ds.submittedAt >= :since " +
           "ORDER BY ds.submittedAt DESC")
    List<DocumentSubmission> findRecentSubmissions(@Param("since") LocalDateTime since);
}
