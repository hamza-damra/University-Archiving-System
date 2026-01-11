package com.alquds.edu.ArchiveSystem.entity.file;

/**
 * Enum representing different types of folders in the file explorer hierarchy.
 */
public enum FolderType {
    /**
     * Academic year root folder (e.g., "2024-2025")
     */
    YEAR_ROOT,
    
    /**
     * Semester root folder (e.g., "first", "second", "summer")
     */
    SEMESTER_ROOT,
    
    /**
     * Professor root folder - top-level folder for a professor
     */
    PROFESSOR_ROOT,
    
    /**
     * Course folder - contains course materials
     */
    COURSE,
    
    /**
     * Standard subfolder within a course (e.g., Syllabus, Exams, Course Notes, Assignments)
     */
    SUBFOLDER
}
