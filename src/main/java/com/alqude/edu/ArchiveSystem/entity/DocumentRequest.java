package com.alqude.edu.ArchiveSystem.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LEGACY ENTITY - ARCHIVED
 * 
 * This entity is part of the old request-based document system and has been replaced
 * by the new semester-based structure using CourseAssignment and RequiredDocumentType entities.
 * 
 * Replacement entities:
 * - CourseAssignment: Links professors to courses within semesters
 * - RequiredDocumentType: Defines required document types per course
 * 
 * This entity is kept for:
 * 1. Historical data access
 * 2. Rollback capability
 * 3. Data migration reference
 * 
 * DO NOT USE FOR NEW DEVELOPMENT
 * 
 * @deprecated Replaced by CourseAssignment and RequiredDocumentType in semester-based system
 * @see com.alqude.edu.ArchiveSystem.entity.CourseAssignment
 * @see com.alqude.edu.ArchiveSystem.entity.RequiredDocumentType
 */
@Deprecated(since = "2.0", forRemoval = false)
@Entity
@Table(name = "document_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Course name is required")
    @Size(max = 100, message = "Course name must not exceed 100 characters")
    @Column(name = "course_name", nullable = false)
    private String courseName;
    
    @NotBlank(message = "Document type is required")
    @Size(max = 50, message = "Document type must not exceed 50 characters")
    @Column(name = "document_type", nullable = false)
    private String documentType;
    
    @Size(max = 10, message = "Maximum 10 file extensions allowed")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "required_file_extensions", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "extension")
    private List<String> requiredFileExtensions;
    
    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    @Column(nullable = false)
    private LocalDateTime deadline;
    
    @NotNull(message = "Professor is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;
    
    @NotNull(message = "Created by user is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Column(length = 1000)
    private String description;
    
    @JsonManagedReference
    @OneToOne(mappedBy = "documentRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SubmittedDocument submittedDocument;
    
    @Column(name = "max_file_count")
    private Integer maxFileCount = 5; // Default maximum 5 files
    
    @Column(name = "max_total_size_mb")
    private Integer maxTotalSizeMb = 50; // Default max 50MB total
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
