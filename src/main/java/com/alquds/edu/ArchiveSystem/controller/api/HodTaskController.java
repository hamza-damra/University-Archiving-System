package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alquds.edu.ArchiveSystem.dto.task.*;
import com.alquds.edu.ArchiveSystem.entity.task.TaskStatus;
import com.alquds.edu.ArchiveSystem.service.auth.AuthService;
import com.alquds.edu.ArchiveSystem.service.task.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for HOD task review and approval endpoints.
 * All endpoints require ROLE_HOD authentication.
 */
@RestController
@RequestMapping("/api/hod/tasks")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('HOD')")
public class HodTaskController {
    
    private final TaskService taskService;
    private final AuthService authService;
    
    /**
     * Get all tasks in the HOD's department with optional filters.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasks(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long professorId,
            @RequestParam(required = false) String status) {
        log.info("HOD retrieving tasks - courseId: {}, semesterId: {}, professorId: {}, status: {}", 
                courseId, semesterId, professorId, status);
        
        var currentUser = authService.getCurrentUser();
        if (currentUser.getDepartment() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("HOD must be assigned to a department"));
        }
        
        TaskFilterRequest filters = TaskFilterRequest.builder()
                .departmentId(currentUser.getDepartment().getId())
                .courseId(courseId)
                .semesterId(semesterId)
                .professorId(professorId)
                .statuses(status != null ? List.of(TaskStatus.valueOf(status)) : null)
                .build();
        
        List<TaskDTO> tasks = taskService.getTasksForDepartment(
                currentUser.getDepartment().getId(), filters);
        
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", tasks));
    }
    
    /**
     * Get a task by ID for review.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDTO>> getTask(@PathVariable Long id) {
        log.info("HOD retrieving task: {}", id);
        
        var currentUser = authService.getCurrentUser();
        if (currentUser.getDepartment() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("HOD must be assigned to a department"));
        }
        
        TaskDTO task = taskService.getTaskByIdForHod(id, currentUser.getDepartment().getId());
        
        return ResponseEntity.ok(ApiResponse.success("Task retrieved successfully", task));
    }
    
    /**
     * Approve a completed task.
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<TaskDTO>> approveTask(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) TaskApprovalRequest request) {
        log.info("HOD approving task: {}", id);
        
        var currentUser = authService.getCurrentUser();
        if (currentUser.getDepartment() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("HOD must be assigned to a department"));
        }
        
        if (request == null) {
            request = TaskApprovalRequest.builder().build();
        }
        
        TaskDTO task = taskService.approveTask(id, request, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success("Task approved successfully", task));
    }
    
    /**
     * Reject a completed task.
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<TaskDTO>> rejectTask(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) TaskApprovalRequest request) {
        log.info("HOD rejecting task: {}", id);
        
        var currentUser = authService.getCurrentUser();
        if (currentUser.getDepartment() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("HOD must be assigned to a department"));
        }
        
        if (request == null) {
            request = TaskApprovalRequest.builder().build();
        }
        
        TaskDTO task = taskService.rejectTask(id, request, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success("Task rejected successfully", task));
    }
    
    /**
     * Get task statistics for the department.
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<TaskStatisticsDTO>> getStatistics(
            @RequestParam(required = false) Long semesterId) {
        log.info("HOD retrieving task statistics - semesterId: {}", semesterId);
        
        var currentUser = authService.getCurrentUser();
        if (currentUser.getDepartment() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("HOD must be assigned to a department"));
        }
        
        TaskStatisticsDTO statistics = taskService.getTaskStatistics(
                currentUser.getDepartment().getId(), semesterId);
        
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", statistics));
    }
    
    /**
     * Get tasks for a specific professor in the department.
     */
    @GetMapping("/professor/{professorId}")
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasksForProfessor(
            @PathVariable Long professorId,
            @RequestParam(required = false) Long semesterId) {
        log.info("HOD retrieving tasks for professor: {}, semesterId: {}", professorId, semesterId);
        
        var currentUser = authService.getCurrentUser();
        if (currentUser.getDepartment() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("HOD must be assigned to a department"));
        }
        
        List<TaskDTO> tasks = taskService.getTasksForProfessorInDepartment(
                currentUser.getDepartment().getId(), professorId, semesterId);
        
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", tasks));
    }
    
    /**
     * Get evidence files for a task (for HOD review).
     */
    @GetMapping("/{id}/evidence")
    public ResponseEntity<ApiResponse<List<TaskEvidenceDTO>>> getTaskEvidence(@PathVariable Long id) {
        log.info("HOD retrieving evidence for task: {}", id);
        
        var currentUser = authService.getCurrentUser();
        if (currentUser.getDepartment() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("HOD must be assigned to a department"));
        }
        
        List<TaskEvidenceDTO> evidence = taskService.getTaskEvidence(id, currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("Evidence retrieved successfully", evidence));
    }
}
