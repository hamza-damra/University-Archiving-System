package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.academic.CourseAssignmentDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.CourseDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.RequiredDocumentTypeDTO;
import com.alqude.edu.ArchiveSystem.entity.Course;
import com.alqude.edu.ArchiveSystem.entity.CourseAssignment;
import com.alqude.edu.ArchiveSystem.entity.Folder;
import com.alqude.edu.ArchiveSystem.entity.RequiredDocumentType;

import java.util.List;

public interface CourseService {
    
    // Course Management
    Course createCourse(CourseDTO dto);
    
    Course updateCourse(Long id, CourseDTO dto);
    
    Course getCourse(Long id);
    
    List<Course> getAllCourses();
    
    List<Course> getCoursesByDepartment(Long departmentId);
    
    void deactivateCourse(Long id);
    
    // Course Assignment Management
    CourseAssignment assignCourse(CourseAssignmentDTO dto);
    
    void unassignCourse(Long assignmentId);
    
    List<CourseAssignment> getAssignmentsBySemester(Long semesterId);
    
    List<CourseAssignment> getAssignmentsByProfessor(Long professorId, Long semesterId);
    
    /**
     * Manually trigger course folder creation for an existing assignment.
     * This is useful for creating folders for assignments that were created before
     * the auto-provisioning feature was implemented.
     *
     * @param assignmentId the course assignment ID
     * @return list of created or existing folders
     */
    List<Folder> createCourseFoldersForAssignment(Long assignmentId);
    
    // Required Document Types
    RequiredDocumentType addRequiredDocumentType(Long courseId, RequiredDocumentTypeDTO dto);
    
    RequiredDocumentType updateRequiredDocumentType(Long id, RequiredDocumentTypeDTO dto);
    
    List<RequiredDocumentType> getRequiredDocumentTypes(Long courseId, Long semesterId);
}
