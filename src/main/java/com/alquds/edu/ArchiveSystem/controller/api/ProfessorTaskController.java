package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alquds.edu.ArchiveSystem.dto.task.*;
import com.alquds.edu.ArchiveSystem.service.auth.AuthService;
import com.alquds.edu.ArchiveSystem.service.task.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Professor task management endpoints.
 * All endpoints require ROLE_PROFESSOR authentication.
 */
@RestController
@RequestMapping("/api/professor/tasks")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('PROFESSOR')")
public class ProfessorTaskController {
    
    private final TaskService taskService;
    private final AuthService authService;
    
    /**
     * Create a new task.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TaskDTO>> createTask(@Valid @RequestBody TaskCreateRequest request) {
        log.info("Professor creating new task: {}", request.getTitle());
        
        var currentUser = authService.getCurrentUser();
        TaskDTO task = taskService.createTask(request, currentUser.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task created successfully", task));
    }
    
    /**
     * Update an existing task.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDTO>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request) {
        log.info("Professor updating task: {}", id);
        
        var currentUser = authService.getCurrentUser();
        TaskDTO task = taskService.updateTask(id, request, currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", task));
    }
    
    /**
     * Delete a task (only if status is PENDING).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTask(@PathVariable Long id) {
        log.info("Professor deleting task: {}", id);
        
        var currentUser = authService.getCurrentUser();
        taskService.deleteTask(id, currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", "OK"));
    }
    
    /**
     * Get a task by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDTO>> getTask(@PathVariable Long id) {
        log.info("Professor retrieving task: {}", id);
        
        var currentUser = authService.getCurrentUser();
        TaskDTO task = taskService.getTaskById(id, currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("Task retrieved successfully", task));
    }
    
    /**
     * Get all tasks for the current professor with optional filters.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasks(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String status) {
        log.info("Professor retrieving tasks - courseId: {}, semesterId: {}, status: {}", 
                courseId, semesterId, status);
        
        var currentUser = authService.getCurrentUser();
        
        TaskFilterRequest filters = TaskFilterRequest.builder()
                .courseId(courseId)
                .semesterId(semesterId)
                .build();
        
        List<TaskDTO> tasks = taskService.getTasksForProfessor(currentUser.getId(), filters);
        
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", tasks));
    }
    
    /**
     * Get weight summary for a course in a semester.
     */
    @GetMapping("/weight-summary")
    public ResponseEntity<ApiResponse<WeightSummaryDTO>> getWeightSummary(
            @RequestParam Long courseId,
            @RequestParam Long semesterId) {
        log.info("Professor retrieving weight summary - courseId: {}, semesterId: {}", 
                courseId, semesterId);
        
        var currentUser = authService.getCurrentUser();
        WeightSummaryDTO summary = taskService.getWeightSummary(
                currentUser.getId(), courseId, semesterId);
        
        return ResponseEntity.ok(ApiResponse.success("Weight summary retrieved successfully", summary));
    }
}
