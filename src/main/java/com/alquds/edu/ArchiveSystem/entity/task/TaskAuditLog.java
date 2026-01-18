package com.alquds.edu.ArchiveSystem.entity.task;

import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing audit log entries for task status changes.
 * Tracks all status transitions and approvals for compliance and accountability.
 */
@Entity
@Table(name = "task_audit_log",
       indexes = {
           @Index(name = "idx_task_audit_log_task_id", columnList = "task_id"),
           @Index(name = "idx_task_audit_log_changed_by", columnList = "changed_by_id"),
           @Index(name = "idx_task_audit_log_created_at", columnList = "created_at")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskAuditLog implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Task is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @lombok.ToString.Exclude
    @JsonIgnoreProperties({"professor", "course", "semester", "fileReference"})
    private Task task;
    
    @Column(name = "old_status", length = 20)
    @Enumerated(EnumType.STRING)
    private TaskStatus oldStatus;
    
    @NotNull(message = "New status is required")
    @Column(name = "new_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TaskStatus newStatus;
    
    @NotNull(message = "Changed by user is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", nullable = false)
    @lombok.ToString.Exclude
    @JsonIgnoreProperties({"password", "courseAssignments", "documentSubmissions", "notifications", "refreshTokens"})
    private User changedBy;
    
    @Column(name = "change_reason", length = 500)
    private String changeReason;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
