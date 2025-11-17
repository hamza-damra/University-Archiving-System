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
    
    List<CourseAssignment> findByProfessorIdAndSemesterId(Long professorId, Long semesterId);
    
    Optional<CourseAssignment> findBySemesterIdAndCourseId(Long semesterId, Long courseId);
    
    @Query("SELECT ca FROM CourseAssignment ca WHERE ca.semester.id = :semesterId AND ca.course.courseCode = :courseCode AND ca.professor.id = :professorId")
    Optional<CourseAssignment> findBySemesterIdAndCourseCodeAndProfessorId(
            @Param("semesterId") Long semesterId, 
            @Param("courseCode") String courseCode, 
            @Param("professorId") Long professorId);
}
