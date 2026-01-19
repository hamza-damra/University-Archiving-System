package com.alquds.edu.ArchiveSystem.entity.task;

import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing evidence files attached to a task.
 * Allows professors to attach multiple files as evidence of task completion.
 * Evidence files are immutable references to files in the file explorer.
 */
@Entity
@Table(name = "task_evidence",
       indexes = {
           @Index(name = "idx_task_evidence_task_id", columnList = "task_id"),
           @Index(name = "idx_task_evidence_file_id", columnList = "file_id")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_task_evidence_task_file", columnNames = {"task_id", "file_id"})
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEvidence implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Task is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @lombok.ToString.Exclude
    @JsonIgnoreProperties({"evidenceFiles", "professor", "course", "semester"})
    private Task task;
    
    @NotNull(message = "File is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    @lombok.ToString.Exclude
    @JsonIgnoreProperties({"documentSubmission", "folder", "uploader"})
    private UploadedFile file;
    
    /**
     * Display order for the evidence file (0-indexed)
     */
    @Column(name = "display_order")
    private Integer displayOrder;
    
    /**
     * Optional note about this specific evidence file
     */
    @Column(name = "note", length = 500)
    private String note;
    
    /**
     * Snapshot of the original filename at the time of attachment.
     * Used to maintain reference even if file is renamed.
     */
    @Column(name = "original_filename_snapshot", length = 255)
    private String originalFilenameSnapshot;
    
    /**
     * Snapshot of the file URL at the time of attachment.
     * Used for audit trail and to detect if file was moved.
     */
    @Column(name = "file_url_snapshot", length = 1000)
    private String fileUrlSnapshot;
    
    @CreationTimestamp
    @Column(name = "attached_at", nullable = false, updatable = false)
    private LocalDateTime attachedAt;
}
