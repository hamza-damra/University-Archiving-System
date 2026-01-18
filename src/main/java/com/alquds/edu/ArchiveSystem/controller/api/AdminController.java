package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;

import com.alquds.edu.ArchiveSystem.dto.academic.CourseAssignmentDTO;
import com.alquds.edu.ArchiveSystem.dto.academic.CourseDTO;
import com.alquds.edu.ArchiveSystem.dto.academic.DepartmentDTO;
import com.alquds.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alquds.edu.ArchiveSystem.dto.dashboard.ChartDataPoint;
import com.alquds.edu.ArchiveSystem.dto.dashboard.DashboardStatistics;
import com.alquds.edu.ArchiveSystem.dto.dashboard.DepartmentChartData;
import com.alquds.edu.ArchiveSystem.dto.dashboard.TimeGrouping;
import com.alquds.edu.ArchiveSystem.dto.report.ReportFilterOptions;
import com.alquds.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alquds.edu.ArchiveSystem.dto.user.UserResponse;
import com.alquds.edu.ArchiveSystem.dto.user.UserUpdateRequest;
import com.alquds.edu.ArchiveSystem.service.academic.CourseService;
import com.alquds.edu.ArchiveSystem.service.dashboard.DashboardWidgetService;
import com.alquds.edu.ArchiveSystem.service.academic.DepartmentService;
import com.alquds.edu.ArchiveSystem.service.academic.SemesterReportService;
import com.alquds.edu.ArchiveSystem.service.user.UserService;
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
            Page<User> usersPage;
            
            // Apply filters based on what's provided
            if (role != null && departmentId != null && isActive != null) {
                // All three filters
                usersPage = userRepository.findByRoleAndDepartmentIdAndIsActiveWithDepartment(role, departmentId, isActive, pageable);
            } else if (role != null && departmentId != null) {
                // Role + Department
                usersPage = userRepository.findByRoleAndDepartmentIdWithDepartment(role, departmentId, pageable);
            } else if (role != null && isActive != null) {
                // Role + Active status
                usersPage = userRepository.findByRoleAndIsActiveWithDepartment(role, isActive, pageable);
            } else if (departmentId != null && isActive != null) {
                // Department + Active status
                usersPage = userRepository.findByDepartmentIdAndIsActiveWithDepartment(departmentId, isActive, pageable);
            } else if (role != null) {
                // Role only
                usersPage = userRepository.findByRoleWithDepartment(role, pageable);
            } else if (departmentId != null) {
                // Department only
                usersPage = userRepository.findByDepartmentIdWithDepartment(departmentId, pageable);
            } else if (isActive != null) {
                // Active status only
                usersPage = userRepository.findByIsActiveWithDepartment(isActive, pageable);
            } else {
                // No filters - get all users
                usersPage = userRepository.findAllWithDepartment(pageable);
            }
            
            // Map to UserResponse
            Page<UserResponse> users = usersPage.map(user -> {
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
     * Update user password only (for admin self-password change).
     * PUT /api/admin/users/{id}/password
     * 
     * @param id User ID
     * @param request Password update request containing new password
     * @return Success response
     */
    @PutMapping("/users/{id}/password")
    public ResponseEntity<ApiResponse<String>> updateUserPassword(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> request) {
        
        log.info("Admin updating password for user with id: {}", id);
        
        try {
            // Verify the current admin is updating their own password
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = auth.getName();
            User currentUser = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            if (!currentUser.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You can only change your own password through this endpoint"));
            }
            
            String newPassword = request.get("password");
            if (newPassword == null || newPassword.length() < 8) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Password must be at least 8 characters"));
            }
            
            userService.updatePassword(id, newPassword);
            return ResponseEntity.ok(ApiResponse.success("Password updated successfully", "Password has been changed"));
        } catch (Exception e) {
            log.error("Error updating password for user with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get information about what will be deleted when a user is removed.
     * GET /api/admin/users/{id}/deletion-info
     * 
     * @param id User ID
     * @return UserDeletionInfo with counts and details
     */
    @GetMapping("/users/{id}/deletion-info")
    public ResponseEntity<ApiResponse<com.alquds.edu.ArchiveSystem.dto.user.UserDeletionInfo>> getUserDeletionInfo(@PathVariable Long id) {
        log.info("Admin requesting deletion info for user with id: {}", id);
        
        try {
            var deletionInfo = userService.getUserDeletionInfo(id);
            return ResponseEntity.ok(ApiResponse.success("User deletion info retrieved", deletionInfo));
        } catch (Exception e) {
            log.error("Error getting deletion info for user with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Delete a user with all associated data.
     * DELETE /api/admin/users/{id}
     * 
     * @param id User ID
     * @param deleteAllData If true (default), deletes all folders, files, and associated data.
     *                      If false, only deletes user record (may fail if FK constraints exist).
     * @return Success response
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean deleteAllData) {
        log.info("Admin deleting user with id: {}, deleteAllData: {}", id, deleteAllData);
        
        try {
            userService.deleteUser(id, deleteAllData);
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", "User and all associated data have been deleted"));
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

    /**
     * Get status distribution chart data.
     * GET /api/admin/dashboard/charts/status-distribution
     * 
     * @param semesterId Optional semester filter
     * @return Status distribution data (pending, uploaded, overdue, total)
     */
    @GetMapping("/dashboard/charts/status-distribution")
    public ResponseEntity<ApiResponse<com.alquds.edu.ArchiveSystem.dto.dashboard.StatusDistribution>> getStatusDistribution(
            @RequestParam(required = false) Long semesterId) {
        
        log.info("Admin retrieving status distribution chart data - semesterId: {}", semesterId);
        
        try {
            com.alquds.edu.ArchiveSystem.dto.dashboard.StatusDistribution statusData = dashboardWidgetService.getStatusDistribution(semesterId);
            return ResponseEntity.ok(ApiResponse.success("Status distribution chart data retrieved successfully", statusData));
        } catch (Exception e) {
            log.error("Error retrieving status distribution chart data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve status distribution chart data: " + e.getMessage()));
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

    // ==================== Comprehensive Reports ====================

    /**
     * Get comprehensive system-wide report overview.
     * Includes statistics, department summaries, and aggregated data.
     * GET /api/admin/reports/overview
     * 
     * @param semesterId Optional semester ID filter
     * @return System-wide report with comprehensive statistics
     */
    @GetMapping("/reports/overview")
    public ResponseEntity<ApiResponse<com.alquds.edu.ArchiveSystem.dto.report.SystemWideReport>> getReportOverview(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long departmentId) {
        log.info("Admin retrieving report overview - semesterId: {}, departmentId: {}", semesterId, departmentId);
        
        try {
            User currentUser = getCurrentUser();
            com.alquds.edu.ArchiveSystem.dto.report.SystemWideReport report;
            
            if (semesterId != null) {
                report = semesterReportService.generateSystemWideReportWithRoleFilter(semesterId, currentUser, departmentId);
            } else {
                // If no semester specified, get the most recent active semester
                // For now, return error - frontend should always provide semester
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Semester ID is required"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("Report overview retrieved successfully", report));
        } catch (Exception e) {
            log.error("Error retrieving report overview", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve report overview: " + e.getMessage()));
        }
    }

    /**
     * Get department-specific report.
     * GET /api/admin/reports/department/{departmentId}
     * 
     * @param departmentId Department ID
     * @param semesterId Semester ID (required)
     * @return Department-specific report
     */
    @GetMapping("/reports/department/{departmentId}")
    public ResponseEntity<ApiResponse<com.alquds.edu.ArchiveSystem.dto.report.ProfessorSubmissionReport>> getDepartmentReport(
            @PathVariable Long departmentId,
            @RequestParam Long semesterId) {
        log.info("Admin retrieving department report - departmentId: {}, semesterId: {}", departmentId, semesterId);
        
        try {
            User currentUser = getCurrentUser();
            com.alquds.edu.ArchiveSystem.dto.report.ProfessorSubmissionReport report = 
                    semesterReportService.generateProfessorSubmissionReportWithRoleFilter(semesterId, departmentId, currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("Department report retrieved successfully", report));
        } catch (Exception e) {
            log.error("Error retrieving department report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve department report: " + e.getMessage()));
        }
    }

    /**
     * Export system-wide report as PDF.
     * GET /api/admin/reports/export/pdf
     * 
     * @param semesterId Semester ID (required)
     * @return PDF file download
     */
    @GetMapping("/reports/export/pdf")
    public ResponseEntity<byte[]> exportReportToPdf(@RequestParam Long semesterId) {
        log.info("Admin exporting report to PDF - semesterId: {}", semesterId);
        
        try {
            User currentUser = getCurrentUser();
            
            // Generate professor submission report for PDF export
            com.alquds.edu.ArchiveSystem.dto.report.ProfessorSubmissionReport report = 
                    semesterReportService.generateProfessorSubmissionReportWithRoleFilter(semesterId, null, currentUser);
            
            // Use the existing export service
            byte[] pdfBytes = semesterReportService.exportReportToPdf(report);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"system-report-" + semesterId + ".pdf\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error exporting report to PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export system-wide report as CSV.
     * GET /api/admin/reports/export/csv
     * 
     * @param semesterId Semester ID (required)
     * @return CSV file download
     */
    @GetMapping("/reports/export/csv")
    public ResponseEntity<String> exportReportToCsv(@RequestParam Long semesterId) {
        log.info("Admin exporting report to CSV - semesterId: {}", semesterId);
        
        try {
            User currentUser = getCurrentUser();
            com.alquds.edu.ArchiveSystem.dto.report.SystemWideReport report = 
                    semesterReportService.generateSystemWideReportWithRoleFilter(semesterId, currentUser);
            
            // Generate CSV content
            StringBuilder csv = new StringBuilder();
            csv.append("Department,Professors,Courses,Required Documents,Submitted,Missing,Overdue,Completion Rate\n");
            
            if (report.getDepartmentSummaries() != null) {
                for (com.alquds.edu.ArchiveSystem.dto.report.DepartmentReportSummary deptSummary : report.getDepartmentSummaries()) {
                    com.alquds.edu.ArchiveSystem.dto.report.SubmissionStatistics stats = deptSummary.getStatistics();
                    int totalRequired = stats.getTotalRequiredDocuments();
                    int submitted = stats.getSubmittedDocuments();
                    double completionRate = totalRequired > 0 ? (double) submitted / totalRequired * 100 : 0.0;
                    
                    csv.append(String.format("\"%s\",%d,%d,%d,%d,%d,%d,%.2f%%\n",
                            deptSummary.getDepartmentName(),
                            stats.getTotalProfessors(),
                            stats.getTotalCourses(),
                            totalRequired,
                            submitted,
                            stats.getMissingDocuments(),
                            stats.getOverdueDocuments(),
                            completionRate));
                }
            }
            
            // Add overall statistics
            if (report.getOverallStatistics() != null) {
                com.alquds.edu.ArchiveSystem.dto.report.SubmissionStatistics overall = report.getOverallStatistics();
                int totalRequired = overall.getTotalRequiredDocuments();
                int submitted = overall.getSubmittedDocuments();
                double completionRate = totalRequired > 0 ? (double) submitted / totalRequired * 100 : 0.0;
                
                csv.append(String.format("\"TOTAL\",%d,%d,%d,%d,%d,%d,%.2f%%\n",
                        overall.getTotalProfessors(),
                        overall.getTotalCourses(),
                        totalRequired,
                        submitted,
                        overall.getMissingDocuments(),
                        overall.getOverdueDocuments(),
                        completionRate));
            }
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=\"system-report-" + semesterId + ".csv\"")
                    .body(csv.toString());
        } catch (Exception e) {
            log.error("Error exporting report to CSV", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating CSV: " + e.getMessage());
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

    /**
     * Health check endpoint for admin operations.
     * GET /api/admin/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Admin API is operational");
    }
}
