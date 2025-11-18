package com.alqude.edu.ArchiveSystem.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * LEGACY ENTITY - ARCHIVED
 * 
 * This entity is part of the old request-based document system and has been replaced
 * by the new semester-based DocumentSubmission entity.
 * 
 * Replacement entity:
 * - DocumentSubmission: Tracks document submissions within the semester-based structure
 * 
 * This entity is kept for:
 * 1. Historical data access
 * 2. Rollback capability
 * 3. Data migration reference
 * 
 * DO NOT USE FOR NEW DEVELOPMENT
 * 
 * @deprecated Replaced by DocumentSubmission in semester-based system
 * @see com.alqude.edu.ArchiveSystem.entity.DocumentSubmission
 */
@Deprecated(since = "2.0", forRemoval = false)
@Entity
@Table(name = "submitted_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmittedDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonBackReference
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private DocumentRequest documentRequest;
    
        @ManyToOne(fetch = FetchType.LAZY)
        @JsonIgnoreProperties({
            "documentRequests",
            "submittedDocuments",
            "notifications",
            "department",
            "password",
            "authorities",
            "username",
            "enabled",
            "accountNonExpired",
            "accountNonLocked",
            "credentialsNonExpired"
        })
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;
    
    // Legacy single file fields (kept for backward compatibility)
    @Column(name = "file_url")
    private String fileUrl;
    
    @Column(name = "original_filename")
    private String originalFilename;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_type")
    private String fileType;
    
    // New multi-file support
    @JsonManagedReference
    @OneToMany(mappedBy = "submittedDocument", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("fileOrder ASC")
    private List<FileAttachment> fileAttachments = new ArrayList<>();
    
    @Column(name = "total_file_size")
    private Long totalFileSize;
    
    @Column(name = "file_count")
    private Integer fileCount = 0;
    
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    
    @Column(name = "is_late_submission", nullable = false)
    private Boolean isLateSubmission = false;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper methods
    public void addFileAttachment(FileAttachment attachment) {
        fileAttachments.add(attachment);
        attachment.setSubmittedDocument(this);
        updateFileCount();
    }
    
    public void removeFileAttachment(FileAttachment attachment) {
        fileAttachments.remove(attachment);
        attachment.setSubmittedDocument(null);
        updateFileCount();
    }
    
    private void updateFileCount() {
        this.fileCount = fileAttachments.size();
        this.totalFileSize = fileAttachments.stream()
                .mapToLong(fa -> fa.getFileSize() != null ? fa.getFileSize() : 0L)
                .sum();
    }
}
