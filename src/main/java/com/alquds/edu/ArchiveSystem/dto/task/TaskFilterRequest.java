package com.alquds.edu.ArchiveSystem.dto.task;

import com.alquds.edu.ArchiveSystem.entity.task.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for filtering tasks in queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskFilterRequest {
    
    private Long courseId;
    private Long semesterId;
    private Long professorId;
    private Long departmentId; // For HOD filtering
    private List<TaskStatus> statuses;
    private Boolean overdueOnly; // Filter only overdue tasks
    private Boolean hasDeadline; // Filter tasks with/without deadlines
}
