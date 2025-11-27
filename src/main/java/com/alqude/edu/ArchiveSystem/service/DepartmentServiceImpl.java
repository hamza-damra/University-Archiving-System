package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.entity.Department;
import com.alqude.edu.ArchiveSystem.repository.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Implementation of DepartmentService.
 * Handles department CRUD operations with shortcut validation.
 */
@Service
@Transactional
@SuppressWarnings("null")
public class DepartmentServiceImpl implements DepartmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(DepartmentServiceImpl.class);
    
    /**
     * Pattern for valid shortcuts: lowercase letters and numbers only.
     * Requirements 14.1: shortcut must contain only lowercase letters and numbers.
     */
    private static final Pattern VALID_SHORTCUT_PATTERN = Pattern.compile("^[a-z0-9]+$");
    
    private final DepartmentRepository departmentRepository;
    
    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }
    
    @Override
    public Department createDepartment(Department department) {
        logger.info("Creating department with name: {} and shortcut: {}", 
                department.getName(), department.getShortcut());
        
        // Validate shortcut format (Requirements 14.1)
        validateShortcutFormat(department.getShortcut());
        
        // Validate shortcut uniqueness (Requirements 14.2)
        validateShortcutUniqueness(department.getShortcut(), null);
        
        // Validate name uniqueness
        if (departmentRepository.existsByName(department.getName())) {
            throw new IllegalArgumentException(
                    "Department with name '" + department.getName() + "' already exists");
        }
        
        Department saved = departmentRepository.save(department);
        logger.info("Created department with ID: {}", saved.getId());
        return saved;
    }
    
    @Override
    public Department updateDepartment(Long id, Department department) {
        logger.info("Updating department ID: {} with shortcut: {}", id, department.getShortcut());
        
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Department not found with ID: " + id));
        
        // Validate shortcut format (Requirements 14.1)
        validateShortcutFormat(department.getShortcut());
        
        // Validate shortcut uniqueness, excluding current department (Requirements 14.2)
        validateShortcutUniqueness(department.getShortcut(), id);
        
        // Validate name uniqueness if changed
        if (!existing.getName().equals(department.getName()) && 
                departmentRepository.existsByName(department.getName())) {
            throw new IllegalArgumentException(
                    "Department with name '" + department.getName() + "' already exists");
        }
        
        // Update fields
        existing.setName(department.getName());
        existing.setShortcut(department.getShortcut());
        existing.setDescription(department.getDescription());
        
        Department saved = departmentRepository.save(existing);
        logger.info("Updated department ID: {}", saved.getId());
        return saved;
    }
    
    @Override
    public void deleteDepartment(Long id) {
        logger.info("Deleting department ID: {}", id);
        
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Department not found with ID: " + id));
        
        // Note: HOD email reference check will be implemented in task 14.1
        // For now, just delete the department
        
        departmentRepository.delete(department);
        logger.info("Deleted department ID: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Department> findById(Long id) {
        return departmentRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Department> findByShortcut(String shortcut) {
        return departmentRepository.findByShortcut(shortcut);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }
    
    @Override
    public void validateShortcutFormat(String shortcut) {
        if (shortcut == null || shortcut.isEmpty()) {
            throw new IllegalArgumentException("Department shortcut cannot be null or empty");
        }
        
        if (!VALID_SHORTCUT_PATTERN.matcher(shortcut).matches()) {
            throw new IllegalArgumentException(
                    "Shortcut must contain only lowercase letters and numbers. " +
                    "Invalid shortcut: '" + shortcut + "'");
        }
        
        if (shortcut.length() > 20) {
            throw new IllegalArgumentException(
                    "Shortcut must be at most 20 characters. " +
                    "Invalid shortcut length: " + shortcut.length());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean shortcutExists(String shortcut) {
        return departmentRepository.existsByShortcut(shortcut);
    }
    
    /**
     * Validate that the shortcut is unique.
     * 
     * @param shortcut The shortcut to validate
     * @param excludeId Department ID to exclude from check (for updates), or null for creates
     * @throws IllegalArgumentException if shortcut is not unique
     */
    private void validateShortcutUniqueness(String shortcut, Long excludeId) {
        boolean exists;
        if (excludeId != null) {
            exists = departmentRepository.existsByShortcutAndIdNot(shortcut, excludeId);
        } else {
            exists = departmentRepository.existsByShortcut(shortcut);
        }
        
        if (exists) {
            throw new IllegalArgumentException(
                    "Department shortcut '" + shortcut + "' already exists");
        }
    }
}
