package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.academic.CourseAssignmentDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.CourseDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.DepartmentDTO;
import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.dashboard.ChartDataPoint;
import com.alqude.edu.ArchiveSystem.dto.dashboard.DashboardStatistics;
import com.alqude.edu.ArchiveSystem.dto.dashboard.DepartmentChartData;
import com.alqude.edu.ArchiveSystem.dto.dashboard.TimeGrouping;
import com.alqude.edu.ArchiveSystem.dto.report.ReportFilterOptions;
import com.alqude.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.user.UserResponse;
import com.alqude.edu.ArchiveSystem.dto.user.UserUpdateRequest;
import com.alqude.edu.ArchiveSystem.entity.Course;
import com.alqude.edu.ArchiveSystem.entity.CourseAssignment;
import com.alqude.edu.ArchiveSystem.entity.Department;
import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import com.alqude.edu.ArchiveSystem.service.CourseService;
import com.alqude.edu.ArchiveSystem.service.DashboardWidgetService;
import com.alqude.edu.ArchiveSystem.service.DataMigrationService;
import com.alqude.edu.ArchiveSystem.service.DataMigrationService.MigrationAnalysis;
import com.alqude.edu.ArchiveSystem.service.DataMigrationService.MigrationResult;
import com.alqude.edu.ArchiveSystem.service.DepartmentService;
import com.alqude.edu.ArchiveSystem.service.SemesterReportService;
import com.alqude.edu.ArchiveSystem.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for administrative operations.
 * Provides endpoints for user management, department management, course management,
 * dashboard statistics, and data migration.
 * All endpoints require ROLE_ADMIN authority.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DataMigrationService dataMigrationService;
    private final UserService userService;
    private final DepartmentService departmentService;
    private final CourseService courseService;
    private final DashboardWidgetService dashboardWidgetService;
    private final UserRepository userRepository;
    private final SemesterReportService semesterReportService;


    // ==================== User Management ====================

    /**
     * Create a new user with any role (Admin, Dean, HOD, Professor).
     * POST /api/admin/users
     * 
     * @param request User creation data
     * @return Created user response
     */
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("Admin creating user with email: {} and role: {}", request.getEmail(), request.getRole());
        
        try {
            UserResponse user = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User created successfully", user));
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get all users with optional filters.
     * GET /api/admin/users
     * 
     * @param role Optional role filter
     * @param departmentId Optional department filter
     * @param isActive Optional active status filter
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paginated list of users
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Admin retrieving users with filters - role: {}, departmentId: {}, isActive: {}", 
                role, departmentId, isActive);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<UserResponse> users;
            
            if (role != null && departmentId != null) {
                // Filter by both role and department
                users = userService.getProfessorsByDepartment(departmentId, pageable);
            } else if (role != null) {
                // Filter by role only
                users = userService.getAllProfessors(pageable);
            } else {
                // Get all users - we need to add this method or use repository directly
                users = userRepository.findAll(pageable)
                        .map(user -> {
                            UserResponse response = new UserResponse();
                            response.setId(user.getId());
                            response.setEmail(user.getEmail());
                            response.setFirstName(user.getFirstName());
                            response.setLastName(user.getLastName());
                            response.setRole(user.getRole());
                            response.setIsActive(user.getIsActive());
                            response.setCreatedAt(user.getCreatedAt());
                            response.setUpdatedAt(user.getUpdatedAt());
                            if (user.getDepartment() != null) {
                                response.setDepartmentId(user.getDepartment().getId());
                                response.setDepartmentName(user.getDepartment().getName());
                            }
                            return response;
                        });
            }
            
            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
        } catch (Exception e) {
            log.error("Error retrieving users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve users: " + e.getMessage()));
        }
    }

    /**
     * Get a user by ID.
     * GET /api/admin/users/{id}
     * 
     * @param id User ID
     * @return User response
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        log.info("Admin retrieving user with id: {}", id);
        
        try {
            UserResponse user = userService.getUserById(id);
            return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
        } catch (Exception e) {
            log.error("Error retrieving user with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update a user.
     * PUT /api/admin/users/{id}
     * 
     * @param id User ID
     * @param request User update data
     * @return Updated user response
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        
        log.info("Admin updating user with id: {}", id);
        
        try {
            UserResponse user = userService.updateUser(id, request);
            return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
        } catch (Exception e) {
            log.error("Error updating user with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete a user.
     * DELETE /api/admin/users/{id}
     * 
     * @param id User ID
     * @return Success response
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        log.info("Admin deleting user with id: {}", id);
        
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", "User has been deleted"));
        } catch (Exception e) {
            log.error("Error deleting user with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }


    // ==================== Department Management ====================

    /**
     * Create a new department.
     * POST /api/admin/departments
     * 
     * @param dto Department data
     * @return Created department
     */
    @PostMapping("/departments")
    public ResponseEntity<ApiResponse<Department>> createDepartment(@Valid @RequestBody DepartmentDTO dto) {
        log.info("Admin creating department with name: {} and shortcut: {}", dto.getName(), dto.getShortcut());
        
        try {
            Department department = new Department();
            department.setName(dto.getName());
            department.setShortcut(dto.getShortcut());
            department.setDescription(dto.getDescription());
            
            Department created = departmentService.createDepartment(department);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Department created successfully", created));
        } catch (Exception e) {
            log.error("Error creating department", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get all departments.
     * GET /api/admin/departments
     * 
     * @return List of all departments
     */
    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<Department>>> getAllDepartments() {
        log.info("Admin retrieving all departments");
        
        try {
            List<Department> departments = departmentService.findAll();
            return ResponseEntity.ok(ApiResponse.success("Departments retrieved successfully", departments));
        } catch (Exception e) {
            log.error("Error retrieving departments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve departments: " + e.getMessage()));
        }
    }

    /**
     * Get a department by ID.
     * GET /api/admin/departments/{id}
     * 
     * @param id Department ID
     * @return Department
     */
    @GetMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<Department>> getDepartmentById(@PathVariable Long id) {
        log.info("Admin retrieving department with id: {}", id);
        
        try {
            Department department = departmentService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
            return ResponseEntity.ok(ApiResponse.success("Department retrieved successfully", department));
        } catch (Exception e) {
            log.error("Error retrieving department with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update a department.
     * PUT /api/admin/departments/{id}
     * 
     * @param id Department ID
     * @param dto Updated department data
     * @return Updated department
     */
    @PutMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<Department>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentDTO dto) {
        
        log.info("Admin updating department with id: {}", id);
        
        try {
            Department department = new Department();
            department.setName(dto.getName());
            department.setShortcut(dto.getShortcut());
            department.setDescription(dto.getDescription());
            
            Department updated = departmentService.updateDepartment(id, department);
            return ResponseEntity.ok(ApiResponse.success("Department updated successfully", updated));
        } catch (Exception e) {
            log.error("Error updating department with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete a department.
     * DELETE /api/admin/departments/{id}
     * 
     * @param id Department ID
     * @return Success response
     */
    @DeleteMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<String>> deleteDepartment(@PathVariable Long id) {
        log.info("Admin deleting department with id: {}", id);
        
        try {
            departmentService.deleteDepartment(id);
            return ResponseEntity.ok(ApiResponse.success("Department deleted successfully", "Department has been deleted"));
        } catch (Exception e) {
            log.error("Error deleting department with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }


    // ==================== Course Management ====================

    /**
     * Create a new course.
     * POST /api/admin/courses
     * 
     * @param dto Course data
     * @return Created course
     */
    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<Course>> createCourse(@Valid @RequestBody CourseDTO dto) {
        log.info("Admin creating course with code: {}", dto.getCourseCode());
        
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
     * Get all courses with optional department filter.
     * GET /api/admin/courses
     * 
     * @param departmentId Optional department filter
     * @return List of courses
     */
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses(
            @RequestParam(required = false) Long departmentId) {
        
        log.info("Admin retrieving courses" + (departmentId != null ? " for department: " + departmentId : ""));
        
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
     * Get a course by ID.
     * GET /api/admin/courses/{id}
     * 
     * @param id Course ID
     * @return Course
     */
    @GetMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<Course>> getCourseById(@PathVariable Long id) {
        log.info("Admin retrieving course with id: {}", id);
        
        try {
            Course course = courseService.getCourse(id);
            return ResponseEntity.ok(ApiResponse.success("Course retrieved successfully", course));
        } catch (Exception e) {
            log.error("Error retrieving course with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update a course.
     * PUT /api/admin/courses/{id}
     * 
     * @param id Course ID
     * @param dto Updated course data
     * @return Updated course
     */
    @PutMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<Course>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseDTO dto) {
        
        log.info("Admin updating course with id: {}", id);
        
        try {
            Course course = courseService.updateCourse(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Course updated successfully", course));
        } catch (Exception e) {
            log.error("Error updating course with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete (deactivate) a course.
     * DELETE /api/admin/courses/{id}
     * 
     * @param id Course ID
     * @return Success response
     */
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCourse(@PathVariable Long id) {
        log.info("Admin deactivating course with id: {}", id);
        
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
     * POST /api/admin/course-assignments
     * 
     * @param dto Course assignment data
     * @return Created course assignment
     */
    @PostMapping("/course-assignments")
    public ResponseEntity<ApiResponse<CourseAssignment>> assignCourse(@Valid @RequestBody CourseAssignmentDTO dto) {
        log.info("Admin assigning course {} to professor {} for semester {}", 
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
     * Get course assignments with optional filters.
     * GET /api/admin/course-assignments
     * 
     * @param semesterId Semester ID (required)
     * @param professorId Optional professor filter
     * @return List of course assignments
     */
    @GetMapping("/course-assignments")
    public ResponseEntity<ApiResponse<List<CourseAssignment>>> getAssignments(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long professorId) {
        
        log.info("Admin retrieving course assignments for semester: {}" + 
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
     * Unassign a course from a professor.
     * DELETE /api/admin/course-assignments/{id}
     * 
     * @param id Course assignment ID
     * @return Success response
     */
    @DeleteMapping("/course-assignments/{id}")
    public ResponseEntity<ApiResponse<String>> unassignCourse(@PathVariable Long id) {
        log.info("Admin unassigning course assignment with id: {}", id);
        
        try {
            courseService.unassignCourse(id);
            return ResponseEntity.ok(ApiResponse.success("Course unassigned successfully", "Course assignment has been removed"));
        } catch (Exception e) {
            log.error("Error unassigning course with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }


    // ==================== Dashboard Statistics ====================

    /**
     * Get dashboard statistics.
     * GET /api/admin/dashboard/statistics
     * 
     * @param academicYearId Optional academic year filter
     * @param semesterId Optional semester filter
     * @return Dashboard statistics
     */
    @GetMapping("/dashboard/statistics")
    public ResponseEntity<ApiResponse<DashboardStatistics>> getDashboardStatistics(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long semesterId) {
        
        log.info("Admin retrieving dashboard statistics - academicYearId: {}, semesterId: {}", 
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
     * GET /api/admin/dashboard/charts/submissions
     * 
     * @param startDate Start date for the period
     * @param endDate End date for the period
     * @param groupBy Time grouping (DAY, WEEK, MONTH)
     * @return Chart data points
     */
    @GetMapping("/dashboard/charts/submissions")
    public ResponseEntity<ApiResponse<List<ChartDataPoint>>> getSubmissionsOverTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "MONTH") TimeGrouping groupBy) {
        
        log.info("Admin retrieving submissions chart data - startDate: {}, endDate: {}, groupBy: {}", 
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
     * GET /api/admin/dashboard/charts/departments
     * 
     * @param semesterId Optional semester filter
     * @return Department chart data
     */
    @GetMapping("/dashboard/charts/departments")
    public ResponseEntity<ApiResponse<List<DepartmentChartData>>> getDepartmentDistribution(
            @RequestParam(required = false) Long semesterId) {
        
        log.info("Admin retrieving department distribution chart data - semesterId: {}", semesterId);
        
        try {
            List<DepartmentChartData> chartData = dashboardWidgetService.getDepartmentDistribution(semesterId);
            return ResponseEntity.ok(ApiResponse.success("Department distribution chart data retrieved successfully", chartData));
        } catch (Exception e) {
            log.error("Error retrieving department distribution chart data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve department distribution chart data: " + e.getMessage()));
        }
    }

    // ==================== Report Filter Options ====================

    /**
     * Get available filter options for report generation.
     * Admin users can filter by all departments.
     * GET /api/admin/reports/filter-options
     * 
     * @return Available filter options
     */
    @GetMapping("/reports/filter-options")
    public ResponseEntity<ApiResponse<ReportFilterOptions>> getReportFilterOptions() {
        log.info("Admin retrieving report filter options");
        
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

    // ==================== Migration Operations ====================

    /**
     * Analyzes existing data to determine migration scope.
     * GET /api/admin/migration/analyze
     */
    @GetMapping("/migration/analyze")
    public ResponseEntity<MigrationAnalysis> analyzeMigrationData() {
        log.info("Analyzing migration data...");
        MigrationAnalysis analysis = dataMigrationService.analyzeExistingData();
        return ResponseEntity.ok(analysis);
    }

    /**
     * Executes the full data migration from old schema to new schema.
     * POST /api/admin/migration/execute
     */
    @PostMapping("/migration/execute")
    public ResponseEntity<MigrationResult> executeMigration() {
        log.info("Starting data migration...");
        MigrationResult result = dataMigrationService.executeFullMigration();
        
        if (result.isSuccess()) {
            log.info("Migration completed successfully");
            return ResponseEntity.ok(result);
        } else {
            log.error("Migration failed: {}", result.getErrorMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Health check endpoint for admin operations.
     * GET /api/admin/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Admin API is operational");
    }
}
