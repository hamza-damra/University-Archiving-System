package com.alquds.edu.ArchiveSystem.service.academic;

import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.user.User;

import com.alquds.edu.ArchiveSystem.dto.professor.CourseAssignmentWithStatus;
import com.alquds.edu.ArchiveSystem.dto.professor.ProfessorDashboardOverview;
import com.alquds.edu.ArchiveSystem.dto.user.ProfessorDTO;

import java.util.List;

/**
 * Service interface for professor management operations.
 * Provides CRUD operations for professors (Deanship only) and course assignment retrieval.
 */
public interface ProfessorService {
    
    /**
     * Create a new professor with automatic professor_id generation.
     * Only accessible by Deanship role.
     * 
     * @param dto Professor data transfer object
     * @return Created professor user
     */
    User createProfessor(ProfessorDTO dto);
    
    /**
     * Update an existing professor's information.
     * Only accessible by Deanship role.
     * 
     * @param id Professor user ID
     * @param dto Updated professor data
     * @return Updated professor user
     */
    User updateProfessor(Long id, ProfessorDTO dto);
    
    /**
     * Get a professor by ID.
     * 
     * @param id Professor user ID
     * @return Professor user
     */
    User getProfessor(Long id);
    
    /**
     * Get all professors in a specific department.
     * 
     * @param departmentId Department ID
     * @return List of professors in the department
     */
    List<User> getProfessorsByDepartment(Long departmentId);
    
    /**
     * Get all professors across all departments.
     * For Deanship role only.
     * 
     * @return List of all professors
     */
    List<User> getAllProfessors();
    
    /**
     * Deactivate a professor (soft delete).
     * Only accessible by Deanship role.
     * 
     * @param id Professor user ID
     */
    void deactivateProfessor(Long id);
    
    /**
     * Activate a previously deactivated professor.
     * Only accessible by Deanship role.
     * 
     * @param id Professor user ID
     */
    void activateProfessor(Long id);
    
    /**
     * Generate a unique professor ID for a professor.
     * Format: "PROF{id}" (e.g., "PROF12345")
     * 
     * @param professor Professor user
     * @return Generated professor ID
     */
    String generateProfessorId(User professor);
    
    /**
     * Get all course assignments for a professor in a specific semester.
     * 
     * @param professorId Professor user ID
     * @param semesterId Semester ID
     * @return List of course assignments
     */
    List<CourseAssignment> getProfessorCourses(Long professorId, Long semesterId);
    
    /**
     * Get course assignments with submission status for professor dashboard.
     * 
     * @param professorId Professor user ID
     * @param semesterId Semester ID
     * @return List of course assignments with document submission status
     */
    List<CourseAssignmentWithStatus> getProfessorCoursesWithStatus(Long professorId, Long semesterId);
    
    /**
     * Get dashboard overview for a professor in a specific semester.
     * 
     * @param professorId Professor user ID
     * @param semesterId Semester ID
     * @return Dashboard overview with statistics
     */
    ProfessorDashboardOverview getProfessorDashboardOverview(Long professorId, Long semesterId);
    
    /**
     * Manually create professor folder for a specific academic year and semester.
     * This is idempotent - returns existing folder if already created.
     * Only accessible by Deanship role.
     * 
     * @param professorId Professor user ID
     * @param academicYearId Academic year ID
     * @param semesterId Semester ID
     * @return Created or existing folder
     */
    com.alquds.edu.ArchiveSystem.entity.file.Folder createProfessorFolder(Long professorId, Long academicYearId, Long semesterId);
}
