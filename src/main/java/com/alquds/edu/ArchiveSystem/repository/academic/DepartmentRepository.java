package com.alquds.edu.ArchiveSystem.repository.academic;

import com.alquds.edu.ArchiveSystem.entity.academic.Department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    Optional<Department> findByName(String name);
    
    boolean existsByName(String name);
    
    /**
     * Find a department by its unique shortcut identifier.
     * Used for HOD email validation to verify department existence.
     * 
     * @param shortcut The department shortcut (e.g., "cs", "math")
     * @return Optional containing the department if found
     */
    Optional<Department> findByShortcut(String shortcut);
    
    /**
     * Check if a department with the given shortcut exists.
     * Used for uniqueness validation during department creation/update.
     * 
     * @param shortcut The department shortcut to check
     * @return true if a department with this shortcut exists
     */
    boolean existsByShortcut(String shortcut);
    
    /**
     * Check if a department with the given shortcut exists, excluding a specific department.
     * Used for uniqueness validation during department update.
     * 
     * @param shortcut The department shortcut to check
     * @param id The department ID to exclude from the check
     * @return true if another department with this shortcut exists
     */
    boolean existsByShortcutAndIdNot(String shortcut, Long id);
}
