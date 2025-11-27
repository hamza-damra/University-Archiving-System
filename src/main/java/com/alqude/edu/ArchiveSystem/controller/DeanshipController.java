package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.academic.AcademicYearDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.CourseAssignmentDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.CourseDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.RequiredDocumentTypeDTO;
import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.common.NotificationResponse;
import com.alqude.edu.ArchiveSystem.dto.dashboard.ChartDataPoint;
import com.alqude.edu.ArchiveSystem.dto.dashboard.DashboardStatistics;
import com.alqude.edu.ArchiveSystem.dto.dashboard.DepartmentChartData;
import com.alqude.edu.ArchiveSystem.dto.dashboard.RecentActivity;
import com.alqude.edu.ArchiveSystem.dto.dashboard.StatusDistribution;
import com.alqude.edu.ArchiveSystem.dto.dashboard.TimeGrouping;
import com.alqude.edu.ArchiveSystem.dto.report.ReportFilterOptions;
import com.alqude.edu.ArchiveSystem.dto.report.SystemWideReport;
import com.alqude.edu.ArchiveSystem.dto.user.ProfessorDTO;
import com.alqude.edu.ArchiveSystem.entity.AcademicYear;
import com.alqude.edu.ArchiveSystem.entity.Course;
import com.alqude.edu.ArchiveSystem.entity.CourseAssignment;
import com.alqude.edu.ArchiveSystem.entity.RequiredDocumentType;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import com.alqude.edu.ArchiveSystem.service.AcademicService;
import com.alqude.edu.ArchiveSystem.service.CourseService;
import com.alqude.edu.ArchiveSystem.service.DashboardWidgetService;
import com.alqude.edu.ArchiveSystem.service.NotificationService;
import com.alqude.edu.ArchiveSystem.service.ProfessorService;
import com.alqude.edu.ArchiveSystem.service.SemesterReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
@PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
public class DeanshipController {
    
    private final AcademicService academicService;
    private final ProfessorService professorService;
    private final CourseService courseService;
    private final SemesterReportService semesterReportService;
    private final DashboardWidgetService dashboardWidgetService;
    private final NotificationService notificationService;
    private final com.alqude.edu.ArchiveSystem.repository.DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    
    // ==================== Academic Year Management ====================
    
    /**
     * Create a new academic year with three semesters (First, Second, Summer).
     * 
     * @param dto Academic year data
     * @return Created academic year with semesters
     */
    @PostMapping("/academic-years")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    
    /**
     * Get semesters for a specific academic year.
     * 
     * @param id Academic year ID
     * @return List of semesters for the academic year
     */
    @GetMapping("/academic-years/{id}/semesters")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
    public ResponseEntity<ApiResponse<List<com.alqude.edu.ArchiveSystem.entity.Semester>>> getSemestersByAcademicYear(@PathVariable Long id) {
        log.info("Deanship retrieving semesters for academic year: {}", id);
        
        try {
            List<com.alqude.edu.ArchiveSystem.entity.Semester> semesters = academicService.getSemestersByYear(id);
            return ResponseEntity.ok(ApiResponse.success("Semesters retrieved successfully", semesters));
        } catch (Exception e) {
            log.error("Error retrieving semesters for academic year: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve semesters: " + e.getMessage()));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    
    /**
     * Manually create professor folder for a specific academic year and semester.
     * This endpoint allows Deanship to create professor folders on demand.
     * The operation is idempotent - returns existing folder if already created.
     * 
     * @param id Professor ID
     * @param academicYearId Academic year ID (query parameter)
     * @param semesterId Semester ID (query parameter)
     * @return Created or existing folder information
     */
    @PostMapping("/professors/{id}/create-folder")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
    public ResponseEntity<ApiResponse<com.alqude.edu.ArchiveSystem.entity.Folder>> createProfessorFolder(
            @PathVariable Long id,
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId) {
        
        log.info("Deanship creating folder for professor ID: {}, academic year ID: {}, semester ID: {}", 
                id, academicYearId, semesterId);
        
        try {
            com.alqude.edu.ArchiveSystem.entity.Folder folder = professorService.createProfessorFolder(
                    id, academicYearId, semesterId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Professor folder created successfully", folder));
        } catch (Exception e) {
            log.error("Error creating professor folder for professor ID: {}", id, e);
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    
    /**
     * Manually create course folder structure for an existing assignment.
     * This endpoint allows Deanship to create course folders on demand.
     * The operation is idempotent - returns existing folders if already created.
     * 
     * @param id Course assignment ID
     * @return List of created or existing folders
     */
    @PostMapping("/course-assignments/{id}/create-folders")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
    public ResponseEntity<ApiResponse<List<com.alqude.edu.ArchiveSystem.entity.Folder>>> createCourseFolders(
            @PathVariable Long id) {
        
        log.info("Deanship creating course folders for assignment ID: {}", id);
        
        try {
            List<com.alqude.edu.ArchiveSystem.entity.Folder> folders = courseService.createCourseFoldersForAssignment(id);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Course folders created successfully", folders));
        } catch (Exception e) {
            log.error("Error creating course folders for assignment ID: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
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
    
    /**
     * Get available filter options for report generation.
     * Dean users can filter by all departments.
     * GET /api/deanship/reports/filter-options
     * 
     * @return Available filter options
     */
    @GetMapping("/reports/filter-options")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
    public ResponseEntity<ApiResponse<ReportFilterOptions>> getReportFilterOptions() {
        log.info("Deanship retrieving report filter options");
        
        try {
            User currentUser = getCurrentUser();
            ReportFilterOptions options = semesterReportService.getFilterOptions(currentUser);
            return ResponseEntity.ok(ApiResponse.success("Report filter options retrieved successfully", options));
        } catch (Exception e) {
            log.error("Error retrieving report filter options", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve report filter options: " + e.getMessage()));
        }
    }
    
    // ==================== Dashboard Statistics ====================
    
    /**
     * Get dashboard statistics.
     * GET /api/deanship/dashboard/statistics
     * 
     * @param academicYearId Optional academic year filter
     * @param semesterId Optional semester filter
     * @return Dashboard statistics
     */
    @GetMapping("/dashboard/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
    public ResponseEntity<ApiResponse<DashboardStatistics>> getDashboardStatistics(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long semesterId) {
        
        log.info("Deanship retrieving dashboard statistics - academicYearId: {}, semesterId: {}", 
                academicYearId, semesterId);
        
        try {
            DashboardStatistics statistics = dashboardWidgetService.getStatistics(academicYearId, semesterId);
            return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved successfully", statistics));
        } catch (Exception e) {
            log.error("Error retrieving dashboard statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve dashboard statistics: " + e.getMessage()));
        }
    }
    
    // ==================== Chart Data ====================
    
    /**
     * Get submissions over time chart data.
     * GET /api/deanship/dashboard/charts/submissions
     * 
     * @param startDate Start date for the period (optional, defaults to 30 days ago)
     * @param endDate End date for the period (optional, defaults to today)
     * @param groupBy Time grouping (DAY, WEEK, MONTH)
     * @return Chart data points
     */
    @GetMapping("/dashboard/charts/submissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
    public ResponseEntity<ApiResponse<List<ChartDataPoint>>> getSubmissionsOverTime(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DAY") TimeGrouping groupBy) {
        
        // Set defaults if not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }
        
        log.info("Deanship retrieving submissions chart data - startDate: {}, endDate: {}, groupBy: {}", 
                startDate, endDate, groupBy);
        
        try {
            List<ChartDataPoint> chartData = dashboardWidgetService.getSubmissionsOverTime(startDate, endDate, groupBy);
            return ResponseEntity.ok(ApiResponse.success("Submissions chart data retrieved successfully", chartData));
        } catch (Exception e) {
            log.error("Error retrieving submissions chart data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve submissions chart data: " + e.getMessage()));
        }
    }
    
    /**
     * Get department distribution chart data.
     * GET /api/deanship/dashboard/charts/departments
     * 
     * @param semesterId Optional semester filter
     * @return Department chart data
     */
    @GetMapping("/dashboard/charts/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
    public ResponseEntity<ApiResponse<List<DepartmentChartData>>> getDepartmentDistribution(
            @RequestParam(required = false) Long semesterId) {
        
        log.info("Deanship retrieving department distribution chart data - semesterId: {}", semesterId);
        
        try {
            List<DepartmentChartData> chartData = dashboardWidgetService.getDepartmentDistribution(semesterId);
            return ResponseEntity.ok(ApiResponse.success("Department distribution chart data retrieved successfully", chartData));
        } catch (Exception e) {
            log.error("Error retrieving department distribution chart data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve department distribution chart data: " + e.getMessage()));
        }
    }
    
    /**
     * Get status distribution chart data.
     * GET /api/deanship/dashboard/charts/status-distribution
     * 
     * @param semesterId Optional semester filter
     * @return Status distribution data (pending, uploaded, overdue, total)
     */
    @GetMapping("/dashboard/charts/status-distribution")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
    public ResponseEntity<ApiResponse<StatusDistribution>> getStatusDistribution(
            @RequestParam(required = false) Long semesterId) {
        
        log.info("Deanship retrieving status distribution chart data - semesterId: {}", semesterId);
        
        try {
            StatusDistribution statusData = dashboardWidgetService.getStatusDistribution(semesterId);
            return ResponseEntity.ok(ApiResponse.success("Status distribution chart data retrieved successfully", statusData));
        } catch (Exception e) {
            log.error("Error retrieving status distribution chart data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve status distribution chart data: " + e.getMessage()));
        }
    }
    
    /**
     * Get recent activity feed.
     * GET /api/deanship/dashboard/activity
     * 
     * @param limit Maximum number of activity items to return (default: 10)
     * @return List of recent activities
     */
    @GetMapping("/dashboard/activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
    public ResponseEntity<ApiResponse<List<RecentActivity>>> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Deanship retrieving recent activity - limit: {}", limit);
        
        try {
            List<RecentActivity> activities = dashboardWidgetService.getRecentActivity(limit);
            return ResponseEntity.ok(ApiResponse.success("Recent activity retrieved successfully", activities));
        } catch (Exception e) {
            log.error("Error retrieving recent activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve recent activity: " + e.getMessage()));
        }
    }
    
    // ==================== Notification Management ====================
    
    /**
     * Get notifications for the current Dean user.
     * Dean users see notifications from all departments.
     * GET /api/deanship/notifications
     * 
     * @return List of notifications
     */
    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications() {
        log.info("Deanship retrieving notifications");
        
        try {
            List<NotificationResponse> notifications = notificationService.getCurrentUserNotifications();
            return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
        } catch (Exception e) {
            log.error("Error retrieving notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve notifications: " + e.getMessage()));
        }
    }
    
    /**
     * Get unread notification count for the current Dean user.
     * GET /api/deanship/notifications/unread-count
     * 
     * @return Unread notification count
     */
    @GetMapping("/notifications/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
    public ResponseEntity<ApiResponse<Long>> getUnreadNotificationCount() {
        log.info("Deanship retrieving unread notification count");
        
        try {
            User currentUser = getCurrentUser();
            long unreadCount = notificationService.getUnreadCount(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Unread count retrieved successfully", unreadCount));
        } catch (Exception e) {
            log.error("Error retrieving unread notification count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve unread count: " + e.getMessage()));
        }
    }
    
    /**
     * Mark a notification as read.
     * PUT /api/deanship/notifications/{id}/read
     * 
     * @param id Notification ID
     * @return Success response
     */
    @PutMapping("/notifications/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANSHIP')")
    public ResponseEntity<ApiResponse<String>> markNotificationAsRead(@PathVariable Long id) {
        log.info("Deanship marking notification {} as read", id);
        
        try {
            notificationService.markNotificationAsRead(id);
            return ResponseEntity.ok(ApiResponse.success("Notification marked as read", "Success"));
        } catch (Exception e) {
            log.error("Error marking notification {} as read", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Current user not found with email: " + email));
    }
}
