package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.academic.AcademicYearDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.CourseAssignmentDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.CourseDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.RequiredDocumentTypeDTO;
import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.report.SystemWideReport;
import com.alqude.edu.ArchiveSystem.dto.user.ProfessorDTO;
import com.alqude.edu.ArchiveSystem.entity.AcademicYear;
import com.alqude.edu.ArchiveSystem.entity.Course;
import com.alqude.edu.ArchiveSystem.entity.CourseAssignment;
import com.alqude.edu.ArchiveSystem.entity.RequiredDocumentType;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.service.AcademicService;
import com.alqude.edu.ArchiveSystem.service.CourseService;
import com.alqude.edu.ArchiveSystem.service.ProfessorService;
import com.alqude.edu.ArchiveSystem.service.SemesterReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Deanship role operations.
 * Provides endpoints for managing academic structure, professors, courses, and assignments.
 * All endpoints require ROLE_DEANSHIP authority.
 */
@RestController
@RequestMapping("/api/deanship")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('DEANSHIP')")
public class DeanshipController {
    
    private final AcademicService academicService;
    private final ProfessorService professorService;
    private final CourseService courseService;
    private final SemesterReportService semesterReportService;
    private final com.alqude.edu.ArchiveSystem.repository.DepartmentRepository departmentRepository;
    
    // ==================== Academic Year Management ====================
    
    /**
     * Create a new academic year with three semesters (First, Second, Summer).
     * 
     * @param dto Academic year data
     * @return Created academic year with semesters
     */
    @PostMapping("/academic-years")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<AcademicYear>> createAcademicYear(@Valid @RequestBody AcademicYearDTO dto) {
        log.info("Deanship creating academic year: {}", dto.getYearCode());
        
        try {
            AcademicYear academicYear = academicService.createAcademicYear(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Academic year created successfully with three semesters", academicYear));
        } catch (Exception e) {
            log.error("Error creating academic year", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Update an existing academic year.
     * 
     * @param id Academic year ID
     * @param dto Updated academic year data
     * @return Updated academic year
     */
    @PutMapping("/academic-years/{id}")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<AcademicYear>> updateAcademicYear(
            @PathVariable Long id,
            @Valid @RequestBody AcademicYearDTO dto) {
        
        log.info("Deanship updating academic year with id: {}", id);
        
        try {
            AcademicYear updatedYear = academicService.updateAcademicYear(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Academic year updated successfully", updatedYear));
        } catch (Exception e) {
            log.error("Error updating academic year with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Get all academic years.
     * 
     * @return List of all academic years
     */
    @GetMapping("/academic-years")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<List<AcademicYear>>> getAllAcademicYears() {
        log.info("Deanship retrieving all academic years");
        
        try {
            List<AcademicYear> academicYears = academicService.getAllAcademicYears();
            return ResponseEntity.ok(ApiResponse.success("Academic years retrieved successfully", academicYears));
        } catch (Exception e) {
            log.error("Error retrieving academic years", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve academic years: " + e.getMessage()));
        }
    }
    
    /**
     * Activate an academic year (set as the active year).
     * 
     * @param id Academic year ID
     * @return Success response
     */
    @PutMapping("/academic-years/{id}/activate")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<String>> activateAcademicYear(@PathVariable Long id) {
        log.info("Deanship activating academic year with id: {}", id);
        
        try {
            academicService.setActiveAcademicYear(id);
            return ResponseEntity.ok(ApiResponse.success("Academic year activated successfully", "Academic year has been set as active"));
        } catch (Exception e) {
            log.error("Error activating academic year with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // ==================== Professor Management ====================
    
    /**
     * Create a new professor with automatic professor_id generation.
     * 
     * @param dto Professor data
     * @return Created professor
     */
    @PostMapping("/professors")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<User>> createProfessor(@Valid @RequestBody ProfessorDTO dto) {
        log.info("Deanship creating professor with email: {}", dto.getEmail());
        
        try {
            User professor = professorService.createProfessor(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Professor created successfully", professor));
        } catch (Exception e) {
            log.error("Error creating professor", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Update an existing professor's information.
     * 
     * @param id Professor ID
     * @param dto Updated professor data
     * @return Updated professor
     */
    @PutMapping("/professors/{id}")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<User>> updateProfessor(
            @PathVariable Long id,
            @Valid @RequestBody ProfessorDTO dto) {
        
        log.info("Deanship updating professor with id: {}", id);
        
        try {
            User updatedProfessor = professorService.updateProfessor(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Professor updated successfully", updatedProfessor));
        } catch (Exception e) {
            log.error("Error updating professor with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Get all professors with optional department filter.
     * 
     * @param departmentId Optional department ID to filter by
     * @return List of professors
     */
    @GetMapping("/professors")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<List<User>>> getAllProfessors(
            @RequestParam(required = false) Long departmentId) {
        
        log.info("Deanship retrieving professors" + (departmentId != null ? " for department: " + departmentId : ""));
        
        try {
            List<User> professors;
            if (departmentId != null) {
                professors = professorService.getProfessorsByDepartment(departmentId);
            } else {
                // Get all professors across all departments
                professors = professorService.getAllProfessors();
            }
            return ResponseEntity.ok(ApiResponse.success("Professors retrieved successfully", professors));
        } catch (Exception e) {
            log.error("Error retrieving professors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve professors: " + e.getMessage()));
        }
    }
    
    /**
     * Deactivate a professor (soft delete).
     * 
     * @param id Professor ID
     * @return Success response
     */
    @PutMapping("/professors/{id}/deactivate")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<String>> deactivateProfessor(@PathVariable Long id) {
        log.info("Deanship deactivating professor with id: {}", id);
        
        try {
            professorService.deactivateProfessor(id);
            return ResponseEntity.ok(ApiResponse.success("Professor deactivated successfully", "Professor has been deactivated"));
        } catch (Exception e) {
            log.error("Error deactivating professor with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Activate a professor.
     * 
     * @param id Professor ID
     * @return Success response
     */
    @PutMapping("/professors/{id}/activate")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<String>> activateProfessor(@PathVariable Long id) {
        log.info("Deanship activating professor with id: {}", id);
        
        try {
            professorService.activateProfessor(id);
            return ResponseEntity.ok(ApiResponse.success("Professor activated successfully", "Professor has been activated"));
        } catch (Exception e) {
            log.error("Error activating professor with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // ==================== Course Management ====================
    
    /**
     * Create a new course.
     * 
     * @param dto Course data
     * @return Created course
     */
    @PostMapping("/courses")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<Course>> createCourse(@Valid @RequestBody CourseDTO dto) {
        log.info("Deanship creating course: {}", dto.getCourseCode());
        
        try {
            Course course = courseService.createCourse(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Course created successfully", course));
        } catch (Exception e) {
            log.error("Error creating course", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Update an existing course.
     * 
     * @param id Course ID
     * @param dto Updated course data
     * @return Updated course
     */
    @PutMapping("/courses/{id}")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<Course>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseDTO dto) {
        
        log.info("Deanship updating course with id: {}", id);
        
        try {
            Course updatedCourse = courseService.updateCourse(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Course updated successfully", updatedCourse));
        } catch (Exception e) {
            log.error("Error updating course with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Get all courses with optional department filter.
     * 
     * @param departmentId Optional department ID to filter by
     * @return List of courses
     */
    @GetMapping("/courses")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses(
            @RequestParam(required = false) Long departmentId) {
        
        log.info("Deanship retrieving courses" + (departmentId != null ? " for department: " + departmentId : ""));
        
        try {
            List<Course> courses;
            if (departmentId != null) {
                courses = courseService.getCoursesByDepartment(departmentId);
            } else {
                courses = courseService.getAllCourses();
            }
            return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", courses));
        } catch (Exception e) {
            log.error("Error retrieving courses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve courses: " + e.getMessage()));
        }
    }
    
    /**
     * Deactivate a course (soft delete).
     * 
     * @param id Course ID
     * @return Success response
     */
    @PutMapping("/courses/{id}/deactivate")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<String>> deactivateCourse(@PathVariable Long id) {
        log.info("Deanship deactivating course with id: {}", id);
        
        try {
            courseService.deactivateCourse(id);
            return ResponseEntity.ok(ApiResponse.success("Course deactivated successfully", "Course has been deactivated"));
        } catch (Exception e) {
            log.error("Error deactivating course with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // ==================== Course Assignment Management ====================
    
    /**
     * Assign a course to a professor for a specific semester.
     * 
     * @param dto Course assignment data
     * @return Created course assignment
     */
    @PostMapping("/course-assignments")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<CourseAssignment>> assignCourse(@Valid @RequestBody CourseAssignmentDTO dto) {
        log.info("Deanship assigning course {} to professor {} for semester {}", 
                dto.getCourseId(), dto.getProfessorId(), dto.getSemesterId());
        
        try {
            CourseAssignment assignment = courseService.assignCourse(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Course assigned successfully", assignment));
        } catch (Exception e) {
            log.error("Error assigning course", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Unassign a course from a professor.
     * 
     * @param id Course assignment ID
     * @return Success response
     */
    @DeleteMapping("/course-assignments/{id}")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<String>> unassignCourse(@PathVariable Long id) {
        log.info("Deanship unassigning course assignment with id: {}", id);
        
        try {
            courseService.unassignCourse(id);
            return ResponseEntity.ok(ApiResponse.success("Course unassigned successfully", "Course assignment has been removed"));
        } catch (Exception e) {
            log.error("Error unassigning course with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Get course assignments with optional filters.
     * 
     * @param semesterId Semester ID (required)
     * @param professorId Optional professor ID to filter by
     * @return List of course assignments
     */
    @GetMapping("/course-assignments")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<List<CourseAssignment>>> getAssignments(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long professorId) {
        
        log.info("Deanship retrieving course assignments for semester: {}" + 
                (professorId != null ? " and professor: " + professorId : ""), semesterId);
        
        try {
            List<CourseAssignment> assignments;
            if (professorId != null) {
                assignments = courseService.getAssignmentsByProfessor(professorId, semesterId);
            } else {
                assignments = courseService.getAssignmentsBySemester(semesterId);
            }
            return ResponseEntity.ok(ApiResponse.success("Course assignments retrieved successfully", assignments));
        } catch (Exception e) {
            log.error("Error retrieving course assignments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve course assignments: " + e.getMessage()));
        }
    }
    
    // ==================== Required Document Type Management ====================
    
    /**
     * Add a required document type for a course.
     * 
     * @param courseId Course ID
     * @param dto Required document type data
     * @return Created required document type
     */
    @PostMapping("/courses/{courseId}/required-documents")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<RequiredDocumentType>> addRequiredDocumentType(
            @PathVariable Long courseId,
            @Valid @RequestBody RequiredDocumentTypeDTO dto) {
        
        log.info("Deanship adding required document type {} for course: {}", dto.getDocumentType(), courseId);
        
        try {
            RequiredDocumentType requiredDocType = courseService.addRequiredDocumentType(courseId, dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Required document type added successfully", requiredDocType));
        } catch (Exception e) {
            log.error("Error adding required document type for course: {}", courseId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // ==================== Department Management ====================
    
    /**
     * Get all departments.
     * 
     * @return List of all departments
     */
    @GetMapping("/departments")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<List<com.alqude.edu.ArchiveSystem.entity.Department>>> getAllDepartments() {
        log.info("Deanship retrieving all departments");
        
        try {
            List<com.alqude.edu.ArchiveSystem.entity.Department> departments = departmentRepository.findAll();
            return ResponseEntity.ok(ApiResponse.success("Departments retrieved successfully", departments));
        } catch (Exception e) {
            log.error("Error retrieving departments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve departments: " + e.getMessage()));
        }
    }
    
    // ==================== Reports ====================
    
    /**
     * Get system-wide submission report for a semester.
     * Shows submission statistics across all departments.
     * 
     * @param semesterId Semester ID
     * @return System-wide report
     */
    @GetMapping("/reports/system-wide")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<ApiResponse<SystemWideReport>> getSystemWideReport(@RequestParam Long semesterId) {
        log.info("Deanship requesting system-wide report for semester: {}", semesterId);
        
        try {
            SystemWideReport report = semesterReportService.generateSystemWideReport(semesterId);
            return ResponseEntity.ok(ApiResponse.success("System-wide report generated successfully", report));
        } catch (Exception e) {
            log.error("Error generating system-wide report for semester: {}", semesterId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate system-wide report: " + e.getMessage()));
        }
    }
}
