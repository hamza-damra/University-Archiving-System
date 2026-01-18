package com.alquds.edu.ArchiveSystem.entity.task;

/**
 * Enum representing task status in the Academic Task Tracking System.
 * Status workflow: PENDING -> IN_PROGRESS -> COMPLETED -> APPROVED/REJECTED
 * OVERDUE status is set automatically when deadline passes.
 */
public enum TaskStatus {
    /**
     * Task has been created but not yet started.
     */
    PENDING,
    
    /**
     * Task is currently being worked on.
     */
    IN_PROGRESS,
    
    /**
     * Task has been completed by professor, awaiting HOD approval.
     */
    COMPLETED,
    
    /**
     * Task deadline has passed and task is not completed or approved.
     * Set automatically by scheduled job.
     */
    OVERDUE,
    
    /**
     * Task has been approved by HOD.
     */
    APPROVED,
    
    /**
     * Task has been rejected by HOD.
     */
    REJECTED
}
