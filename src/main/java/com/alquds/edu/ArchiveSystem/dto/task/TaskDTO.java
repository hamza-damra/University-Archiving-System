package com.alquds.edu.ArchiveSystem.dto.task;

import com.alquds.edu.ArchiveSystem.entity.task.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO representing a task with all details for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    
    private Long id;
    private String title;
    private String description;
    private Integer weightPercentage;
    private Integer progressPercentage;
    private TaskStatus status;
    private LocalDate deadline;
    
    // File reference
    private Long fileReferenceId;
    private String fileReferenceName;
    private String fileReferenceUrl;
    
    // Professor information
    private Long professorId;
    private String professorName;
    private String professorEmail;
    
    // Course information
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Long departmentId;
    private String departmentName;
    
    // Semester information
    private Long semesterId;
    private String semesterType;
    private String academicYear;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private Boolean isOverdue;
    private Boolean canBeDeleted;
    private Integer daysUntilDeadline; // null if no deadline or overdue
    private Integer daysOverdue; // null if not overdue
}
