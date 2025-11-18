package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.exception.UnauthorizedOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for applying department-scoped filtering based on user role.
 * 
 * Rules:
 * - Deanship: No filtering, can access all departments
 * - HOD: Filter all queries by their department
 * - Professor: Filter by department for read operations, by assignment for write operations
 */
@Service
@Slf4j
public class DepartmentScopedFilterService {
    
    /**
     * Filter course assignments based on user role and department.
     * 
     * @param assignments List of course assignments to filter
     * @param currentUser The current authenticated user
     * @return Filtered list of course assignments
     */
    public List<CourseAssignment> filterCourseAssignments(List<CourseAssignment> assignments, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user cannot be null");
        }
        
        switch (currentUser.getRole()) {
            case ROLE_DEANSHIP:
                // Deanship can see all assignments
                log.debug("Deanship user - no filtering applied to {} assignments", assignments.size());
                return assignments;
                
            case ROLE_HOD:
            case ROLE_PROFESSOR:
                // HOD and Professor can only see assignments in their department
                if (currentUser.getDepartment() == null) {
                    log.warn("User {} has no department assigned", currentUser.getEmail());
                    return List.of();
                }
                
                List<CourseAssignment> filtered = assignments.stream()
                        .filter(assignment -> {
                            // Check if professor's department matches
                            User professor = assignment.getProfessor();
                            return professor.getDepartment() != null && 
                                   professor.getDepartment().getId().equals(currentUser.getDepartment().getId());
                        })
                        .collect(Collectors.toList());
                
                log.debug("{} user - filtered {} assignments to {} in department {}", 
                        currentUser.getRole(), assignments.size(), filtered.size(), 
                        currentUser.getDepartment().getName());
                return filtered;
                
            default:
                log.warn("Unknown role: {}", currentUser.getRole());
                return List.of();
        }
    }
    
    /**
     * Filter professors based on user role and department.
     * 
     * @param professors List of professors to filter
     * @param currentUser The current authenticated user
     * @return Filtered list of professors
     */
    public List<User> filterProfessors(List<User> professors, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user cannot be null");
        }
        
        switch (currentUser.getRole()) {
            case ROLE_DEANSHIP:
                // Deanship can see all professors
                log.debug("Deanship user - no filtering applied to {} professors", professors.size());
                return professors;
                
            case ROLE_HOD:
            case ROLE_PROFESSOR:
                // HOD and Professor can only see professors in their department
                if (currentUser.getDepartment() == null) {
                    log.warn("User {} has no department assigned", currentUser.getEmail());
                    return List.of();
                }
                
                List<User> filtered = professors.stream()
                        .filter(professor -> professor.getDepartment() != null && 
                                           professor.getDepartment().getId().equals(currentUser.getDepartment().getId()))
                        .collect(Collectors.toList());
                
                log.debug("{} user - filtered {} professors to {} in department {}", 
                        currentUser.getRole(), professors.size(), filtered.size(), 
                        currentUser.getDepartment().getName());
                return filtered;
                
            default:
                log.warn("Unknown role: {}", currentUser.getRole());
                return List.of();
        }
    }
    
    /**
     * Filter courses based on user role and department.
     * 
     * @param courses List of courses to filter
     * @param currentUser The current authenticated user
     * @return Filtered list of courses
     */
    public List<Course> filterCourses(List<Course> courses, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user cannot be null");
        }
        
        switch (currentUser.getRole()) {
            case ROLE_DEANSHIP:
                // Deanship can see all courses
                log.debug("Deanship user - no filtering applied to {} courses", courses.size());
                return courses;
                
            case ROLE_HOD:
            case ROLE_PROFESSOR:
                // HOD and Professor can only see courses in their department
                if (currentUser.getDepartment() == null) {
                    log.warn("User {} has no department assigned", currentUser.getEmail());
                    return List.of();
                }
                
                List<Course> filtered = courses.stream()
                        .filter(course -> course.getDepartment() != null && 
                                        course.getDepartment().getId().equals(currentUser.getDepartment().getId()))
                        .collect(Collectors.toList());
                
                log.debug("{} user - filtered {} courses to {} in department {}", 
                        currentUser.getRole(), courses.size(), filtered.size(), 
                        currentUser.getDepartment().getName());
                return filtered;
                
            default:
                log.warn("Unknown role: {}", currentUser.getRole());
                return List.of();
        }
    }
    
    /**
     * Filter document submissions based on user role and department.
     * 
     * @param submissions List of document submissions to filter
     * @param currentUser The current authenticated user
     * @return Filtered list of document submissions
     */
    public List<DocumentSubmission> filterDocumentSubmissions(List<DocumentSubmission> submissions, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user cannot be null");
        }
        
        switch (currentUser.getRole()) {
            case ROLE_DEANSHIP:
                // Deanship can see all submissions
                log.debug("Deanship user - no filtering applied to {} submissions", submissions.size());
                return submissions;
                
            case ROLE_HOD:
                // HOD can only see submissions in their department
                if (currentUser.getDepartment() == null) {
                    log.warn("HOD user {} has no department assigned", currentUser.getEmail());
                    return List.of();
                }
                
                List<DocumentSubmission> hodFiltered = submissions.stream()
                        .filter(submission -> {
                            User professor = submission.getProfessor();
                            return professor.getDepartment() != null && 
                                   professor.getDepartment().getId().equals(currentUser.getDepartment().getId());
                        })
                        .collect(Collectors.toList());
                
                log.debug("HOD user - filtered {} submissions to {} in department {}", 
                        submissions.size(), hodFiltered.size(), currentUser.getDepartment().getName());
                return hodFiltered;
                
            case ROLE_PROFESSOR:
                // Professor can see submissions in their department (read access)
                // For write access, additional checks are done at the controller level
                if (currentUser.getDepartment() == null) {
                    log.warn("Professor user {} has no department assigned", currentUser.getEmail());
                    return List.of();
                }
                
                List<DocumentSubmission> profFiltered = submissions.stream()
                        .filter(submission -> {
                            User professor = submission.getProfessor();
                            return professor.getDepartment() != null && 
                                   professor.getDepartment().getId().equals(currentUser.getDepartment().getId());
                        })
                        .collect(Collectors.toList());
                
                log.debug("Professor user - filtered {} submissions to {} in department {}", 
                        submissions.size(), profFiltered.size(), currentUser.getDepartment().getName());
                return profFiltered;
                
            default:
                log.warn("Unknown role: {}", currentUser.getRole());
                return List.of();
        }
    }
    
    /**
     * Validate that a user has access to a specific department.
     * Throws UnauthorizedOperationException if access is denied.
     * 
     * @param departmentId The department ID to check access for
     * @param currentUser The current authenticated user
     * @throws UnauthorizedOperationException if user doesn't have access to the department
     */
    public void validateDepartmentAccess(Long departmentId, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user cannot be null");
        }
        
        if (departmentId == null) {
            return; // No specific department to validate
        }
        
        switch (currentUser.getRole()) {
            case ROLE_DEANSHIP:
                // Deanship has access to all departments
                log.debug("Deanship user - access granted to department {}", departmentId);
                return;
                
            case ROLE_HOD:
            case ROLE_PROFESSOR:
                // HOD and Professor can only access their own department
                if (currentUser.getDepartment() == null) {
                    log.warn("User {} has no department assigned", currentUser.getEmail());
                    throw new UnauthorizedOperationException(
                            "User has no department assigned and cannot access department data");
                }
                
                if (!currentUser.getDepartment().getId().equals(departmentId)) {
                    log.warn("{} user {} attempted to access department {} but belongs to department {}", 
                            currentUser.getRole(), currentUser.getEmail(), 
                            departmentId, currentUser.getDepartment().getId());
                    throw new UnauthorizedOperationException(
                            "User does not have access to the requested department");
                }
                
                log.debug("{} user - access granted to own department {}", 
                        currentUser.getRole(), departmentId);
                return;
                
            default:
                log.warn("Unknown role: {}", currentUser.getRole());
                throw new UnauthorizedOperationException("Invalid user role");
        }
    }
    
    /**
     * Validate that a professor has write access to a specific course assignment.
     * Throws UnauthorizedOperationException if access is denied.
     * 
     * @param assignment The course assignment to check write access for
     * @param currentUser The current authenticated user
     * @throws UnauthorizedOperationException if user doesn't have write access
     */
    public void validateWriteAccess(CourseAssignment assignment, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user cannot be null");
        }
        
        if (assignment == null) {
            throw new IllegalArgumentException("Course assignment cannot be null");
        }
        
        // Only professors can have write access
        if (currentUser.getRole() != Role.ROLE_PROFESSOR) {
            log.warn("Non-professor user {} attempted write operation", currentUser.getEmail());
            throw new UnauthorizedOperationException(
                    "Only professors can perform write operations on course assignments");
        }
        
        // Professor must be assigned to the course
        if (!assignment.getProfessor().getId().equals(currentUser.getId())) {
            log.warn("Professor {} attempted to write to course assignment {} owned by professor {}", 
                    currentUser.getEmail(), assignment.getId(), assignment.getProfessor().getEmail());
            throw new UnauthorizedOperationException(
                    "Professor can only modify their own course assignments");
        }
        
        log.debug("Professor user - write access granted to own course assignment {}", assignment.getId());
    }
    
    /**
     * Get the department ID for filtering based on user role.
     * Returns null for Deanship (no filtering), user's department ID for HOD and Professor.
     * 
     * @param currentUser The current authenticated user
     * @return Department ID for filtering, or null if no filtering needed
     */
    public Long getDepartmentIdForFiltering(User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user cannot be null");
        }
        
        switch (currentUser.getRole()) {
            case ROLE_DEANSHIP:
                // No filtering for Deanship
                return null;
                
            case ROLE_HOD:
            case ROLE_PROFESSOR:
                // Filter by user's department
                if (currentUser.getDepartment() == null) {
                    log.warn("User {} has no department assigned", currentUser.getEmail());
                    return null;
                }
                return currentUser.getDepartment().getId();
                
            default:
                log.warn("Unknown role: {}", currentUser.getRole());
                return null;
        }
    }
}
