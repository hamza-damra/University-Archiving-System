package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.user.ProfessorDTO;
import com.alqude.edu.ArchiveSystem.entity.CourseAssignment;
import com.alqude.edu.ArchiveSystem.entity.Department;
import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.exception.BusinessException;
import com.alqude.edu.ArchiveSystem.exception.DuplicateEntityException;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.repository.CourseAssignmentRepository;
import com.alqude.edu.ArchiveSystem.repository.DepartmentRepository;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of ProfessorService for managing professor operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProfessorServiceImpl implements ProfessorService {
    
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public User createProfessor(ProfessorDTO dto) {
        log.info("Creating new professor with email: {}", dto.getEmail());
        
        // Validate email uniqueness
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateEntityException("Professor with email " + dto.getEmail() + " already exists");
        }
        
        // Validate department exists
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found with ID: " + dto.getDepartmentId()));
        
        // Validate password is provided for new professor
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "Password is required for new professor");
        }
        
        // Create new professor user
        User professor = new User();
        professor.setEmail(dto.getEmail());
        professor.setPassword(passwordEncoder.encode(dto.getPassword()));
        professor.setFirstName(dto.getFirstName());
        professor.setLastName(dto.getLastName());
        professor.setRole(Role.ROLE_PROFESSOR);
        professor.setDepartment(department);
        professor.setIsActive(true);
        
        // Save to get ID
        professor = userRepository.save(professor);
        
        // Generate and set professor ID
        String professorId = generateProfessorId(professor);
        professor.setProfessorId(professorId);
        
        // Save again with professor ID
        professor = userRepository.save(professor);
        
        log.info("Successfully created professor with ID: {} and professorId: {}", professor.getId(), professorId);
        return professor;
    }
    
    @Override
    public User updateProfessor(Long id, ProfessorDTO dto) {
        log.info("Updating professor with ID: {}", id);
        
        User professor = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found with ID: " + id));
        
        // Validate role is professor
        if (professor.getRole() != Role.ROLE_PROFESSOR) {
            throw new BusinessException("INVALID_ROLE", "User with ID " + id + " is not a professor");
        }
        
        // Check email uniqueness if email is being changed
        if (!professor.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateEntityException("Professor with email " + dto.getEmail() + " already exists");
        }
        
        // Validate department exists if being changed
        if (!professor.getDepartment().getId().equals(dto.getDepartmentId())) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new EntityNotFoundException("Department not found with ID: " + dto.getDepartmentId()));
            professor.setDepartment(department);
        }
        
        // Update fields
        professor.setEmail(dto.getEmail());
        professor.setFirstName(dto.getFirstName());
        professor.setLastName(dto.getLastName());
        
        // Update password only if provided
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            professor.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        
        professor = userRepository.save(professor);
        
        log.info("Successfully updated professor with ID: {}", id);
        return professor;
    }
    
    @Override
    @Transactional(readOnly = true)
    public User getProfessor(Long id) {
        log.debug("Fetching professor with ID: {}", id);
        
        User professor = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found with ID: " + id));
        
        // Validate role is professor
        if (professor.getRole() != Role.ROLE_PROFESSOR) {
            throw new BusinessException("INVALID_ROLE", "User with ID " + id + " is not a professor");
        }
        
        return professor;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<User> getProfessorsByDepartment(Long departmentId) {
        log.debug("Fetching professors for department ID: {}", departmentId);
        
        // Validate department exists
        if (!departmentRepository.existsById(departmentId)) {
            throw new EntityNotFoundException("Department not found with ID: " + departmentId);
        }
        
        return userRepository.findByDepartmentIdAndRole(departmentId, Role.ROLE_PROFESSOR);
    }
    
    @Override
    public void deactivateProfessor(Long id) {
        log.info("Deactivating professor with ID: {}", id);
        
        User professor = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found with ID: " + id));
        
        // Validate role is professor
        if (professor.getRole() != Role.ROLE_PROFESSOR) {
            throw new BusinessException("INVALID_ROLE", "User with ID " + id + " is not a professor");
        }
        
        professor.setIsActive(false);
        userRepository.save(professor);
        
        log.info("Successfully deactivated professor with ID: {}", id);
    }
    
    @Override
    public void activateProfessor(Long id) {
        log.info("Activating professor with ID: {}", id);
        
        User professor = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found with ID: " + id));
        
        // Validate role is professor
        if (professor.getRole() != Role.ROLE_PROFESSOR) {
            throw new BusinessException("INVALID_ROLE", "User with ID " + id + " is not a professor");
        }
        
        professor.setIsActive(true);
        userRepository.save(professor);
        
        log.info("Successfully activated professor with ID: {}", id);
    }
    
    @Override
    public String generateProfessorId(User professor) {
        if (professor.getId() == null) {
            throw new BusinessException("VALIDATION_ERROR", "Professor must be saved before generating professor ID");
        }
        
        // Generate professor ID in format "PROF{id}" (e.g., "PROF12345")
        String professorId = String.format("PROF%d", professor.getId());
        
        log.debug("Generated professor ID: {} for user ID: {}", professorId, professor.getId());
        return professorId;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CourseAssignment> getProfessorCourses(Long professorId, Long semesterId) {
        log.debug("Fetching course assignments for professor ID: {} in semester ID: {}", professorId, semesterId);
        
        // Validate professor exists
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found with ID: " + professorId));
        
        // Validate role is professor
        if (professor.getRole() != Role.ROLE_PROFESSOR) {
            throw new BusinessException("INVALID_ROLE", "User with ID " + professorId + " is not a professor");
        }
        
        // Fetch course assignments
        return courseAssignmentRepository.findByProfessorIdAndSemesterId(professorId, semesterId);
    }
}
