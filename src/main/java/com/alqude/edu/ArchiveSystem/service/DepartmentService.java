package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.entity.Department;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Department operations.
 * Handles CRUD operations with shortcut validation.
 */
public interface DepartmentService {
    
    /**
     * Create a new department with shortcut validation.
     * 
     * @param department The department to create
     * @return The created department
     * @throws IllegalArgumentException if shortcut format is invalid or not unique
     */
    Department createDepartment(Department department);
    
    /**
     * Update an existing department with shortcut validation.
     * 
     * @param id The department ID
     * @param department The updated department data
     * @return The updated department
     * @throws IllegalArgumentException if shortcut format is invalid or not unique
     * @throws jakarta.persistence.EntityNotFoundException if department not found
     */
    Department updateDepartment(Long id, Department department);
    
    /**
     * Delete a department.
     * 
     * @param id The department ID
     * @throws IllegalStateException if HOD accounts reference this department's shortcut
     * @throws jakarta.persistence.EntityNotFoundException if department not found
     */
    void deleteDepartment(Long id);
    
    /**
     * Find a department by ID.
     * 
     * @param id The department ID
     * @return Optional containing the department if found
     */
    Optional<Department> findById(Long id);
    
    /**
     * Find a department by shortcut.
     * 
     * @param shortcut The department shortcut
     * @return Optional containing the department if found
     */
    Optional<Department> findByShortcut(String shortcut);
    
    /**
     * Get all departments.
     * 
     * @return List of all departments
     */
    List<Department> findAll();
    
    /**
     * Validate shortcut format (lowercase alphanumeric only).
     * 
     * @param shortcut The shortcut to validate
     * @throws IllegalArgumentException if format is invalid
     */
    void validateShortcutFormat(String shortcut);
    
    /**
     * Check if a shortcut exists in the system.
     * 
     * @param shortcut The shortcut to check
     * @return true if shortcut exists
     */
    boolean shortcutExists(String shortcut);
}
