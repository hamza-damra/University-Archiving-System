package com.alquds.edu.ArchiveSystem.repository.academic;

import com.alquds.edu.ArchiveSystem.entity.academic.Course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    List<Course> findByDepartmentId(Long departmentId);
    
    Optional<Course> findByCourseCode(String courseCode);
    
    List<Course> findByIsActiveTrue();
    
    // ==================== Dashboard Analytics Queries ====================
    
    /**
     * Count active courses
     */
    long countByIsActiveTrue();
    
    /**
     * Count courses by department
     */
    @Query("SELECT c.department.id, c.department.name, COUNT(c) " +
           "FROM Course c " +
           "WHERE c.isActive = true " +
           "GROUP BY c.department.id, c.department.name")
    List<Object[]> countActiveCoursesByDepartment();
}
