package com.alquds.edu.ArchiveSystem.entity.submission;

import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;

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
@Table(name = "required_document_types",
       indexes = {
           @Index(name = "idx_required_doc_types_course", columnList = "course_id"),
           @Index(name = "idx_required_doc_types_semester", columnList = "semester_id"),
           @Index(name = "idx_required_doc_types_course_sem", columnList = "course_id, semester_id"),
           @Index(name = "idx_required_doc_types_doc_type", columnList = "document_type")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocumentType implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Course is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @lombok.ToString.Exclude
    private Course course;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    @lombok.ToString.Exclude
    private Semester semester; // Optional: specific to semester
    
    @NotNull(message = "Document type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentTypeEnum documentType;
    
    @Column
    private LocalDateTime deadline;
    
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = true;
    
    @Column(name = "max_file_count")
    private Integer maxFileCount = 5;
    
    @Column(name = "max_total_size_mb")
    private Integer maxTotalSizeMb = 50;
    
    @ElementCollection
    @CollectionTable(name = "allowed_file_extensions", 
                     joinColumns = @JoinColumn(name = "required_document_type_id"))
    @Column(name = "extension")
    private List<String> allowedFileExtensions = new ArrayList<>(); // e.g., ["pdf", "zip"]
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
