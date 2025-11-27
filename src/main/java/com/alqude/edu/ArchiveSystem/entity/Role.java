package com.alqude.edu.ArchiveSystem.entity;

/**
 * Enum representing user roles in the Archive System.
 * Roles are ordered by hierarchy with ADMIN having the highest privileges.
 */
public enum Role {
    /**
     * Administrator role with full system access.
     * Can manage all users, departments, courses, and system-wide settings.
     */
    ROLE_ADMIN,
    
    /**
     * Deanship role with access to all departments.
     * Can view and manage data across all departments.
     */
    ROLE_DEANSHIP,
    
    /**
     * Head of Department role with access to own department only.
     * Can manage professors and view submissions within their department.
     */
    ROLE_HOD,
    
    /**
     * Professor role with access to own data only.
     * Can upload documents and manage their own submissions.
     */
    ROLE_PROFESSOR
}
