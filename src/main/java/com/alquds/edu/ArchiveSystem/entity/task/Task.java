package com.alquds.edu.ArchiveSystem.entity.task;

import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing an academic task with weighted distribution and progress tracking.
 * Tasks are scoped to a specific professor, course, and semester combination.
 */
@Entity
@Table(name = "tasks",
       indexes = {
           @Index(name = "idx_tasks_professor_id", columnList = "professor_id"),
           @Index(name = "idx_tasks_course_id", columnList = "course_id"),
           @Index(name = "idx_tasks_semester_id", columnList = "semester_id"),
           @Index(name = "idx_tasks_status", columnList = "status"),
           @Index(name = "idx_tasks_professor_course_semester", columnList = "professor_id, course_id, semester_id"),
           @Index(name = "idx_tasks_deadline", columnList = "deadline")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Task title is required")
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(length = 2000)
    private String description;
    
    @NotNull(message = "Weight percentage is required")
    @Min(value = 0, message = "Weight percentage must be >= 0")
    @Max(value = 100, message = "Weight percentage must be <= 100")
    @Column(name = "weight_percentage", nullable = false)
    private Integer weightPercentage;
    
    @NotNull(message = "Progress percentage is required")
    @Min(value = 0, message = "Progress percentage must be >= 0")
    @Max(value = 100, message = "Progress percentage must be <= 100")
    @Column(name = "progress_percentage", nullable = false)
    private Integer progressPercentage = 0;
    
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;
    
    @Column(name = "deadline")
    private LocalDate deadline;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_reference_id")
    @lombok.ToString.Exclude
    @JsonIgnoreProperties({"documentSubmission", "folder", "uploader"})
    private UploadedFile fileReference;
    
    @NotNull(message = "Professor is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    @lombok.ToString.Exclude
    @JsonIgnoreProperties({"password", "courseAssignments", "documentSubmissions", "notifications", "refreshTokens"})
    private User professor;
    
    @NotNull(message = "Course is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @lombok.ToString.Exclude
    @JsonIgnoreProperties({"courseAssignments", "requiredDocumentTypes", "department"})
    private Course course;
    
    @NotNull(message = "Semester is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    @lombok.ToString.Exclude
    @JsonIgnoreProperties({"courseAssignments", "academicYear"})
    private Semester semester;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Check if task is overdue based on deadline and current status.
     */
    @Transient
    public boolean isOverdue() {
        if (deadline == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return today.isAfter(deadline) && 
               status != TaskStatus.COMPLETED && 
               status != TaskStatus.APPROVED && 
               status != TaskStatus.OVERDUE;
    }
    
    /**
     * Check if task can be deleted (only PENDING tasks can be deleted).
     */
    @Transient
    public boolean canBeDeleted() {
        return status == TaskStatus.PENDING;
    }
}
