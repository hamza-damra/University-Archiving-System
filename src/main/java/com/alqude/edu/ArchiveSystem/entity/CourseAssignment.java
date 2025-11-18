package com.alqude.edu.ArchiveSystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@Table(name = "course_assignments", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"semester_id", "course_id", "professor_id"}),
       indexes = {
           @Index(name = "idx_course_assignments_semester", columnList = "semester_id"),
           @Index(name = "idx_course_assignments_professor", columnList = "professor_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseAssignment implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Semester is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    @lombok.ToString.Exclude
    @JsonIgnoreProperties({"academicYear", "courseAssignments"})
    private Semester semester;
    
    @NotNull(message = "Course is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @lombok.ToString.Exclude
    @JsonIgnoreProperties({"courseAssignments", "requiredDocumentTypes"})
    private Course course;
    
    @NotNull(message = "Professor is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    @lombok.ToString.Exclude
    @JsonIgnoreProperties({"courseAssignments", "password", "documentRequests"})
    private User professor;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "courseAssignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @lombok.ToString.Exclude
    @JsonManagedReference("assignment-submissions")
    private List<DocumentSubmission> documentSubmissions = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
