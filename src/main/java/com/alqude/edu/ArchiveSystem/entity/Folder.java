package com.alqude.edu.ArchiveSystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a folder in the file explorer system.
 * Folders are organized hierarchically and scoped by academic year and semester.
 */
@Entity
@Table(name = "folders", indexes = {
    @Index(name = "idx_folder_path", columnList = "path", unique = true),
    @Index(name = "idx_folder_parent", columnList = "parent_id"),
    @Index(name = "idx_folder_owner", columnList = "owner_id"),
    @Index(name = "idx_folder_context", columnList = "academic_year_id, semester_id, owner_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Folder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Full path of the folder following convention: {yearCode}/{semesterType}/{professorId}/{courseCode}/{subfolder}
     * Example: "2024-2025/first/PROF123/CS101 - Data Structures/Syllabus"
     */
    @Column(nullable = false, unique = true, length = 500)
    private String path;
    
    /**
     * Display name of the folder
     * Example: "CS101 - Data Structures" or "Syllabus"
     */
    @Column(nullable = false, length = 255)
    private String name;
    
    /**
     * Type of folder in the hierarchy
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FolderType type;
    
    /**
     * Parent folder (null for root folders)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;
    
    /**
     * Owner of the folder (typically the professor)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
    
    /**
     * Academic year this folder belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;
    
    /**
     * Semester this folder belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester;
    
    /**
     * Course this folder is associated with (null for non-course folders)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;
    
    /**
     * Timestamp when the folder was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Set creation timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
