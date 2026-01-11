package com.alquds.edu.ArchiveSystem.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for recent activity feed items.
 * Represents a recent action in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivity {
    
    /**
     * Type of activity: SUBMISSION, PROFESSOR_ADDED, COURSE_UPDATED, etc.
     */
    private String type;
    
    /**
     * Icon type for UI display
     */
    private String icon;
    
    /**
     * Main message describing the activity
     */
    private String message;
    
    /**
     * Additional details (course code, file name, etc.)
     */
    private String details;
    
    /**
     * When the activity occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * Human-readable time ago string
     */
    private String timeAgo;
    
    /**
     * Related entity ID (professor ID, course ID, etc.)
     */
    private Long entityId;
    
    /**
     * Entity type for linking
     */
    private String entityType;
}
