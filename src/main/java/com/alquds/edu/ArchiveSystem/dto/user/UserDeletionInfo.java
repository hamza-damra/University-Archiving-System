package com.alquds.edu.ArchiveSystem.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO containing information about what will be deleted when a user is removed.
 * Used to show admin a preview before confirming deletion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeletionInfo {
    
    /**
     * User ID being checked for deletion
     */
    private Long userId;
    
    /**
     * User's full name
     */
    private String userName;
    
    /**
     * User's email
     */
    private String userEmail;
    
    /**
     * User's role
     */
    private String userRole;
    
    /**
     * Number of folders owned by the user
     */
    private int folderCount;
    
    /**
     * Number of files uploaded by the user
     */
    private int fileCount;
    
    /**
     * Total size of files in MB
     */
    private double totalFileSizeMb;
    
    /**
     * Number of document submissions by the user
     */
    private int submissionCount;
    
    /**
     * Number of course assignments for the user
     */
    private int courseAssignmentCount;
    
    /**
     * Number of notifications for the user
     */
    private int notificationCount;
    
    /**
     * Whether the user can be deleted (no blocking dependencies)
     */
    private boolean canDelete;
    
    /**
     * Message explaining why deletion is blocked (if canDelete is false)
     */
    private String blockingReason;
    
    /**
     * Warning message for admin (if there are items that will be deleted)
     */
    private String warningMessage;
}
