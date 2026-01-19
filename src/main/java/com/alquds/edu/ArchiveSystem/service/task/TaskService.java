package com.alquds.edu.ArchiveSystem.service.task;

import com.alquds.edu.ArchiveSystem.dto.task.*;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.UploadedFileDTO;
import com.alquds.edu.ArchiveSystem.entity.user.User;

import java.util.List;

/**
 * Service interface for managing academic tasks.
 */
public interface TaskService {
    
    /**
     * Create a new task for a professor.
     * Validates weight percentage sum (must equal 100% per course+semester+professor).
     */
    TaskDTO createTask(TaskCreateRequest request, Long professorId);
    
    /**
     * Update an existing task.
     * Validates weight percentage if updated.
     */
    TaskDTO updateTask(Long taskId, TaskUpdateRequest request, Long professorId);
    
    /**
     * Delete a task owned by the professor.
     * Also deletes associated audit logs and evidence.
     */
    void deleteTask(Long taskId, Long professorId);
    
    /**
     * Get a task by ID.
     */
    TaskDTO getTaskById(Long taskId, Long professorId);
    
    /**
     * Get all tasks for a professor with optional filters.
     */
    List<TaskDTO> getTasksForProfessor(Long professorId, TaskFilterRequest filters);
    
    /**
     * Get weight summary for a professor's course in a semester.
     */
    WeightSummaryDTO getWeightSummary(Long professorId, Long courseId, Long semesterId);
    
    /**
     * Get all tasks in a department (for HOD view).
     */
    List<TaskDTO> getTasksForDepartment(Long departmentId, TaskFilterRequest filters);
    
    /**
     * Get a task by ID for HOD review (includes evidence files).
     */
    TaskDTO getTaskByIdForHod(Long taskId, Long departmentId);
    
    /**
     * Approve a completed task (HOD only).
     */
    TaskDTO approveTask(Long taskId, TaskApprovalRequest request, User hodUser);
    
    /**
     * Reject a completed task (HOD only).
     */
    TaskDTO rejectTask(Long taskId, TaskApprovalRequest request, User hodUser);
    
    /**
     * Get task statistics for a department.
     */
    TaskStatisticsDTO getTaskStatistics(Long departmentId, Long semesterId);
    
    /**
     * Get tasks for a specific professor in a department (HOD view).
     */
    List<TaskDTO> getTasksForProfessorInDepartment(Long departmentId, Long professorId, Long semesterId);
    
    /**
     * Check and update overdue tasks (called by scheduled job).
     */
    int checkAndUpdateOverdueTasks();
    
    // ==================== Evidence Management ====================
    
    /**
     * Get evidence files for a task.
     * Professor can view own tasks; HOD can view department tasks.
     */
    List<TaskEvidenceDTO> getTaskEvidence(Long taskId, Long userId);
    
    /**
     * Add evidence files to a task.
     * Only the task owner (professor) can add evidence.
     */
    List<TaskEvidenceDTO> addEvidence(Long taskId, List<Long> fileIds, Long professorId);
    
    /**
     * Remove a specific evidence file from a task.
     * Only the task owner (professor) can remove evidence.
     */
    void removeEvidence(Long taskId, Long evidenceId, Long professorId);
    
    /**
     * Get files available for the professor to attach as evidence.
     * Returns files the professor owns or has access to.
     */
    List<UploadedFileDTO> getAvailableFilesForEvidence(Long professorId, Long semesterId);
}
