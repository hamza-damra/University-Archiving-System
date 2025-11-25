package com.alqude.edu.ArchiveSystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing an uploaded file in the file explorer system.
 * Files are stored physically on disk and associated with folders.
 */
@Entity
@Table(name = "uploaded_files", indexes = {
        @Index(name = "idx_uploaded_files_folder", columnList = "folder_id"),
        @Index(name = "idx_uploaded_files_uploader", columnList = "uploader_id"),
        @Index(name = "idx_uploaded_files_submission", columnList = "document_submission_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Folder where this file is stored
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    @lombok.ToString.Exclude
    private Folder folder;

    /**
     * Original filename as uploaded by user
     */
    @NotBlank(message = "Original filename is required")
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    /**
     * Sanitized filename stored on disk
     */
    @Column(name = "stored_filename")
    private String storedFilename;

    /**
     * Full file URL/path: {folderPath}/{storedFilename}
     */
    @NotBlank(message = "File URL is required")
    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    /**
     * File size in bytes
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * MIME type
     */
    @Column(name = "file_type")
    private String fileType;

    /**
     * User who uploaded the file
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    @lombok.ToString.Exclude
    private User uploader;

    /**
     * Optional notes about the file
     */
    @Column(length = 1000)
    private String notes;

    /**
     * Document submission (optional, for backward compatibility)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_submission_id")
    @lombok.ToString.Exclude
    private DocumentSubmission documentSubmission;

    /**
     * File order (optional, for backward compatibility)
     */
    @Column(name = "file_order")
    private Integer fileOrder;

    /**
     * Description (optional, for backward compatibility)
     */
    @Column(length = 500)
    private String description;

    /**
     * Upload timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
