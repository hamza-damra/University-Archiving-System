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
@Table(name = "academic_years")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYear implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Year code is required")
    @Column(name = "year_code", nullable = false, unique = true)
    private String yearCode; // e.g., "2024-2025"
    
    @NotNull(message = "Start year is required")
    @Column(name = "start_year", nullable = false)
    private Integer startYear; // e.g., 2024
    
    @NotNull(message = "End year is required")
    @Column(name = "end_year", nullable = false)
    private Integer endYear; // e.g., 2025
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @lombok.ToString.Exclude
    private List<Semester> semesters = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
