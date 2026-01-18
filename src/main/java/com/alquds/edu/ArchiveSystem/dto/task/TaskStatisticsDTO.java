package com.alquds.edu.ArchiveSystem.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for task statistics and analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatisticsDTO {
    
    // Overall statistics
    private Long totalTasks;
    private Long pendingTasks;
    private Long inProgressTasks;
    private Long completedTasks;
    private Long overdueTasks;
    private Long approvedTasks;
    private Long rejectedTasks;
    
    // Course-level completion (sum of completed task weights per course)
    private Map<Long, CourseCompletionStats> courseCompletion;
    
    // Department-level completion (aggregated per professor)
    private Map<Long, ProfessorCompletionStats> professorCompletion;
    
    // Weight distribution summary
    private Integer totalWeightPercentage; // Should be 100% per course+semester+professor
    private Map<Long, Integer> courseWeightTotals; // Total weight per course
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseCompletionStats {
        private Long courseId;
        private String courseCode;
        private String courseName;
        private Integer completionPercentage; // Sum of completed task weights
        private Long totalTasks;
        private Long completedTasks;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfessorCompletionStats {
        private Long professorId;
        private String professorName;
        private Integer completionPercentage; // Average across all courses
        private Long totalTasks;
        private Long completedTasks;
        private Long approvedTasks;
    }
}
