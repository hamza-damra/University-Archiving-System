package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.CourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, Long> {
    
    List<CourseAssignment> findBySemesterId(Long semesterId);
    
    @Query("SELECT ca FROM CourseAssignment ca " +
           "JOIN FETCH ca.course c " +
           "JOIN FETCH c.department " +
           "JOIN FETCH ca.semester s " +
           "JOIN FETCH s.academicYear " +
           "JOIN FETCH ca.professor " +
           "WHERE ca.professor.id = :professorId AND ca.semester.id = :semesterId")
    List<CourseAssignment> findByProfessorIdAndSemesterId(
            @Param("professorId") Long professorId, 
            @Param("semesterId") Long semesterId);
    
    Optional<CourseAssignment> findBySemesterIdAndCourseId(Long semesterId, Long courseId);
    
    @Query("SELECT ca FROM CourseAssignment ca " +
           "JOIN FETCH ca.course c " +
           "JOIN FETCH ca.professor " +
           "WHERE ca.semester.id = :semesterId AND c.courseCode = :courseCode AND ca.professor.id = :professorId")
    Optional<CourseAssignment> findBySemesterIdAndCourseCodeAndProfessorId(
            @Param("semesterId") Long semesterId, 
            @Param("courseCode") String courseCode, 
            @Param("professorId") Long professorId);
    
    Optional<CourseAssignment> findBySemesterIdAndCourseIdAndProfessorId(
            Long semesterId, Long courseId, Long professorId);
}
