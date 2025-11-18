package com.alqude.edu.ArchiveSystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "uploaded_files",
       indexes = {
           @Index(name = "idx_uploaded_files_submission", columnList = "document_submission_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFile implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Document submission is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_submission_id", nullable = false)
    @lombok.ToString.Exclude
    private DocumentSubmission documentSubmission;
    
    @NotBlank(message = "File URL is required")
    @Column(name = "file_url", nullable = false)
    private String fileUrl; // Physical path or URL
    
    @NotBlank(message = "Original filename is required")
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_type")
    private String fileType; // MIME type
    
    @Column(name = "file_order")
    private Integer fileOrder;
    
    @Column(length = 500)
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
