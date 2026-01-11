package com.alquds.edu.ArchiveSystem.entity.file;

import com.alquds.edu.ArchiveSystem.entity.submission.SubmittedDocument;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * LEGACY ENTITY - ARCHIVED
 * 
 * Entity representing an individual file attachment for a submitted document
 * Supports multiple files per document request
 * 
 * This entity is part of the old request-based document system and has been replaced
 * by the new semester-based UploadedFile entity.
 * 
 * Replacement entity:
 * - UploadedFile: Stores file metadata within the semester-based structure
 * 
 * This entity is kept for:
 * 1. Historical data access
 * 2. Rollback capability
 * 3. Data migration reference
 * 
 * DO NOT USE FOR NEW DEVELOPMENT
 * 
 * @deprecated Replaced by UploadedFile in semester-based system
 * @see com.alquds.edu.ArchiveSystem.entity.file.UploadedFile
 */
@Deprecated(since = "2.0", forRemoval = false)
@Entity
@Table(name = "file_attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_document_id", nullable = false)
    private SubmittedDocument submittedDocument;
    
    @Column(name = "file_url", nullable = false)
    private String fileUrl;
    
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_type")
    private String fileType;
    
    @Column(name = "file_order")
    private Integer fileOrder;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
