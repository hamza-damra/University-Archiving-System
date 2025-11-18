package com.alqude.edu.ArchiveSystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document_submissions",
       indexes = {
           @Index(name = "idx_document_submissions_course_assignment", columnList = "course_assignment_id"),
           @Index(name = "idx_document_submissions_professor", columnList = "professor_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSubmission implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Course assignment is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_assignment_id", nullable = false)
    @lombok.ToString.Exclude
    private CourseAssignment courseAssignment;
    
    @NotNull(message = "Document type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentTypeEnum documentType;
    
    @NotNull(message = "Professor is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    @lombok.ToString.Exclude
    private User professor;
    
    @OneToMany(mappedBy = "documentSubmission", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @lombok.ToString.Exclude
    private List<UploadedFile> uploadedFiles = new ArrayList<>();
    
    @NotNull(message = "Submitted at is required")
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    
    @Column(name = "is_late_submission", nullable = false)
    private Boolean isLateSubmission = false;
    
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status; // NOT_UPLOADED, UPLOADED, OVERDUE
    
    @Column(length = 1000)
    private String notes;
    
    @Column(name = "file_count", nullable = false)
    private Integer fileCount = 0;
    
    @Column(name = "total_file_size", nullable = false)
    private Long totalFileSize = 0L;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
