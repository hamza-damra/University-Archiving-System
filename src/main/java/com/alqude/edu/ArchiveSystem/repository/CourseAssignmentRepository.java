package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.CourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, Long> {

        @Query("SELECT ca FROM CourseAssignment ca " +
                        "WHERE ca.semester.id = :semesterId AND ca.isActive = true")
        List<CourseAssignment> findBySemesterId(@Param("semesterId") Long semesterId);
        
        /**
         * Optimized query for report generation with eager loading.
         * Fetches course assignments with all related entities in a single query
         * to prevent N+1 query issues.
         * 
         * @param semesterId The semester ID
         * @return List of course assignments with eagerly loaded relationships
         */
        @Query("SELECT DISTINCT ca FROM CourseAssignment ca " +
                        "JOIN FETCH ca.course c " +
                        "JOIN FETCH ca.professor p " +
                        "LEFT JOIN FETCH p.department " +
                        "JOIN FETCH ca.semester s " +
                        "JOIN FETCH s.academicYear " +
                        "WHERE ca.semester.id = :semesterId AND ca.isActive = true")
        List<CourseAssignment> findBySemesterIdWithEagerLoading(@Param("semesterId") Long semesterId);

        @Query("SELECT ca FROM CourseAssignment ca " +
                        "JOIN FETCH ca.course c " +
                        "JOIN FETCH c.department " +
                        "JOIN FETCH ca.semester s " +
                        "JOIN FETCH s.academicYear " +
                        "JOIN FETCH ca.professor " +
                        "WHERE ca.professor.id = :professorId AND ca.semester.id = :semesterId AND ca.isActive = true")
        List<CourseAssignment> findByProfessorIdAndSemesterId(
                        @Param("professorId") Long professorId,
                        @Param("semesterId") Long semesterId);

        Optional<CourseAssignment> findBySemesterIdAndCourseId(Long semesterId, Long courseId);

        @Query("SELECT ca FROM CourseAssignment ca " +
                        "JOIN FETCH ca.course c " +
                        "JOIN FETCH ca.professor " +
                        "WHERE ca.semester.id = :semesterId AND c.courseCode = :courseCode AND ca.professor.id = :professorId AND ca.isActive = true")
        Optional<CourseAssignment> findBySemesterIdAndCourseCodeAndProfessorId(
                        @Param("semesterId") Long semesterId,
                        @Param("courseCode") String courseCode,
                        @Param("professorId") Long professorId);

        @Query("SELECT ca FROM CourseAssignment ca " +
                        "WHERE ca.semester.id = :semesterId AND ca.course.id = :courseId AND ca.professor.id = :professorId AND ca.isActive = true")
        Optional<CourseAssignment> findBySemesterIdAndCourseIdAndProfessorId(
                        @Param("semesterId") Long semesterId,
                        @Param("courseId") Long courseId,
                        @Param("professorId") Long professorId);
        
        /**
         * Find all course assignments for a professor
         */
        @Query("SELECT ca FROM CourseAssignment ca WHERE ca.professor.id = :professorId")
        List<CourseAssignment> findByProfessorId(@Param("professorId") Long professorId);
        
        /**
         * Find all course assignments for a course
         */
        @Query("SELECT ca FROM CourseAssignment ca WHERE ca.course.id = :courseId")
        List<CourseAssignment> findByCourseId(@Param("courseId") Long courseId);
        
        /**
         * Delete all course assignments for a professor
         */
        @Modifying
        @Transactional
        @Query("DELETE FROM CourseAssignment ca WHERE ca.professor.id = :professorId")
        void deleteByProfessorId(@Param("professorId") Long professorId);
        
        /**
         * Delete all course assignments for a course
         */
        @Modifying
        @Transactional
        @Query("DELETE FROM CourseAssignment ca WHERE ca.course.id = :courseId")
        void deleteByCourseId(@Param("courseId") Long courseId);
}
