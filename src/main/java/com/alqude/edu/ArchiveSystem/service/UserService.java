package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.user.UserResponse;
import com.alqude.edu.ArchiveSystem.dto.user.UserUpdateRequest;
import com.alqude.edu.ArchiveSystem.entity.Department;
import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.exception.UserException;
import com.alqude.edu.ArchiveSystem.exception.ValidationException;
import com.alqude.edu.ArchiveSystem.mapper.UserMapper;
import com.alqude.edu.ArchiveSystem.repository.DepartmentRepository;
import com.alqude.edu.ArchiveSystem.repository.DocumentRequestRepository;
import com.alqude.edu.ArchiveSystem.repository.RefreshTokenRepository;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@SuppressWarnings("deprecation")
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailValidationService emailValidationService;
    
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    // Password validation pattern (at least 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char)
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])[^\\s]{8,}$"
    );
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
    
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        
        // Comprehensive validation
        validateUserCreateRequest(request);
        
        // Check email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw UserException.emailAlreadyExists(request.getEmail());
        }
        
        // Validate and fetch department based on role
        Long departmentId = request.getDepartmentId();
        Department department = null;
        
        // Role-based department validation:
        // - ADMIN: Should NOT have a department (system-wide access)
        // - DEANSHIP (Dean): Should NOT have a department (faculty/college-wide access)
        // - HOD: MUST have a department (manages a specific department)
        // - PROFESSOR: MUST have a department (belongs to a specific department)
        switch (request.getRole()) {
            case ROLE_ADMIN:
                // Admin should not have a department - ignore if provided
                if (departmentId != null) {
                    log.info("Admin user should not have a department. Ignoring provided departmentId: {}", departmentId);
                }
                department = null;
                break;
                
            case ROLE_DEANSHIP:
                // Dean should not have a department - ignore if provided
                if (departmentId != null) {
                    log.info("Dean user should not have a department. Ignoring provided departmentId: {}", departmentId);
                }
                department = null;
                break;
                
            case ROLE_HOD:
                // HOD must have a department
                if (departmentId == null) {
                    throw new IllegalArgumentException("Department is required for Head of Department (HOD) role");
                }
                department = departmentRepository.findById(departmentId)
                        .orElseThrow(() -> UserException.departmentNotFound(departmentId));
                break;
                
            case ROLE_PROFESSOR:
                // Professor must have a department
                if (departmentId == null) {
                    throw new IllegalArgumentException("Department is required for Professor role");
                }
                department = departmentRepository.findById(departmentId)
                        .orElseThrow(() -> UserException.departmentNotFound(departmentId));
                break;
                
            default:
                throw new IllegalArgumentException("Unknown role: " + request.getRole());
        }
        
        // Validate role
        validateRole(request.getRole());
        
        // Validate role-specific email format
        validateRoleSpecificEmail(request.getEmail(), request.getRole());
        
        // Validate password strength
        validatePassword(request.getPassword());
        
        // Create user entity
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDepartment(department);
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {} and role: {}", savedUser.getId(), savedUser.getRole());
        
        return userMapper.toResponse(savedUser);
    }
    
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        log.info("Updating user with id: {}", userId);
        
        // Validate input
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        validateUserUpdateRequest(request);
        
        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.userNotFound(userId));
        
        // Check email uniqueness if email is being changed
        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw UserException.emailAlreadyExists(request.getEmail());
            }
            validateEmail(request.getEmail());
        }
        
        // Validate and update department based on user's role
        Long departmentId = request.getDepartmentId();
        Role userRole = user.getRole();
        
        // Role-based department validation for updates:
        // - ADMIN and DEANSHIP: Should NOT have a department (set to null if provided)
        // - HOD and PROFESSOR: Must have a department
        if (userRole == Role.ROLE_ADMIN || userRole == Role.ROLE_DEANSHIP) {
            // Admin and Dean should not have a department
            if (departmentId != null) {
                log.info("{} user should not have a department. Ignoring provided departmentId: {}", 
                    userRole == Role.ROLE_ADMIN ? "Admin" : "Dean", departmentId);
            }
            user.setDepartment(null);
        } else if (userRole == Role.ROLE_HOD || userRole == Role.ROLE_PROFESSOR) {
            // HOD and Professor must have a department
            if (departmentId != null) {
                Department department = departmentRepository.findById(departmentId)
                        .orElseThrow(() -> UserException.departmentNotFound(departmentId));
                user.setDepartment(department);
            } else if (user.getDepartment() == null) {
                // If no department is provided and user doesn't have one, throw error
                throw new IllegalArgumentException("Department is required for " + 
                    (userRole == Role.ROLE_HOD ? "HOD" : "Professor") + " role");
            }
            // If departmentId is null but user already has a department, keep the existing one
        }
        
        // Update user entity
        userMapper.updateEntity(request, user);
        User updatedUser = userRepository.save(user);
        
        log.info("User updated successfully with id: {}", updatedUser.getId());
        return userMapper.toResponse(updatedUser);
    }
    
    public void deleteUser(Long userId) {
        log.info("Deleting user with id: {}", userId);
        
        // Validate input
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.userNotFound(userId));
        
        // Prevent self-deletion
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getId().equals(userId)) {
            throw UserException.cannotDeleteSelf();
        }
        
        // Check for dependencies
        checkUserDependencies(userId);
        
        // Delete all refresh tokens for this user first
        refreshTokenRepository.deleteAllByUserId(userId);
        log.debug("Deleted all refresh tokens for user id: {}", userId);
        
        // Soft delete or hard delete based on business rules
        // For now, we'll do a hard delete after checking dependencies
        if (user == null) {
            throw new IllegalStateException("User entity cannot be null for deletion");
        }
        userRepository.delete(user);
        log.info("User deleted successfully with id: {}", userId);
    }
    
    /**
     * Update user password.
     * 
     * @param userId User ID
     * @param newPassword New password (plain text)
     */
    public void updatePassword(Long userId, String newPassword) {
        log.info("Updating password for user with id: {}", userId);
        
        // Validate input
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        
        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.userNotFound(userId));
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password updated successfully for user with id: {}", userId);
    }
    
    public UserResponse getUserById(Long userId) {
        log.debug("Retrieving user with id: {}", userId);
        
        // Validate input
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.userNotFound(userId));
        
        return userMapper.toResponse(user);
    }
    
    public List<UserResponse> getProfessorsByDepartment(Long departmentId) {
        log.debug("Retrieving professors for department id: {}", departmentId);
        
        // Validate input
        if (departmentId == null) {
            throw new IllegalArgumentException("Department ID cannot be null");
        }
        
        // Verify department exists
        if (!departmentRepository.existsById(departmentId)) {
            throw UserException.departmentNotFound(departmentId);
        }
        
        List<User> professors = userRepository.findByDepartmentIdAndRole(departmentId, Role.ROLE_PROFESSOR);
        return professors.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    public Page<UserResponse> getProfessorsByDepartment(Long departmentId, Pageable pageable) {
        Page<User> professors = userRepository.findByDepartmentIdAndRole(departmentId, Role.ROLE_PROFESSOR, pageable);
        return professors.map(userMapper::toResponse);
    }
    
    public List<UserResponse> getActiveProfessorsByDepartment(Long departmentId) {
        List<User> professors = userRepository.findActiveProfessorsByDepartment(departmentId, Role.ROLE_PROFESSOR);
        return professors.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    public Page<UserResponse> getAllProfessors(Pageable pageable) {
        Page<User> professors = userRepository.findActiveUsersByRole(Role.ROLE_PROFESSOR, pageable);
        return professors.map(userMapper::toResponse);
    }
    
    // ========== Validation Methods ==========
    
    private void validateUserCreateRequest(UserCreateRequest request) {
        Map<String, String> errors = new HashMap<>();
        
        if (!StringUtils.hasText(request.getEmail())) {
            errors.put("email", "Email is required");
        } else {
            validateEmail(request.getEmail(), errors);
        }
        
        if (!StringUtils.hasText(request.getPassword())) {
            errors.put("password", "Password is required");
        }
        
        // First name and last name are required for all roles
        if (!StringUtils.hasText(request.getFirstName())) {
            errors.put("firstName", "First name is required");
        } else if (request.getFirstName().length() > 50) {
            errors.put("firstName", "First name must not exceed 50 characters");
        }
        
        if (!StringUtils.hasText(request.getLastName())) {
            errors.put("lastName", "Last name is required");
        } else if (request.getLastName().length() > 50) {
            errors.put("lastName", "Last name must not exceed 50 characters");
        }
        
        if (request.getRole() == null) {
            errors.put("role", "Role is required");
        }
        
        // Department validation is role-based:
        // - HOD and Professor require a department
        // - Admin and Dean should NOT have a department
        if (request.getRole() != null) {
            if ((request.getRole() == Role.ROLE_HOD || request.getRole() == Role.ROLE_PROFESSOR) 
                && request.getDepartmentId() == null) {
                errors.put("departmentId", "Department is required for " + 
                    (request.getRole() == Role.ROLE_HOD ? "HOD" : "Professor") + " role");
            }
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("User creation validation failed", errors);
        }
    }
    
    private void validateUserUpdateRequest(UserUpdateRequest request) {
        Map<String, String> errors = new HashMap<>();
        
        if (request.getEmail() != null && !StringUtils.hasText(request.getEmail())) {
            errors.put("email", "Email cannot be empty");
        } else if (request.getEmail() != null) {
            validateEmail(request.getEmail(), errors);
        }
        
        if (request.getFirstName() != null && !StringUtils.hasText(request.getFirstName())) {
            errors.put("firstName", "First name cannot be empty");
        } else if (request.getFirstName() != null && request.getFirstName().length() > 50) {
            errors.put("firstName", "First name must not exceed 50 characters");
        }
        
        if (request.getLastName() != null && !StringUtils.hasText(request.getLastName())) {
            errors.put("lastName", "Last name cannot be empty");
        } else if (request.getLastName() != null && request.getLastName().length() > 50) {
            errors.put("lastName", "Last name must not exceed 50 characters");
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("User update validation failed", errors);
        }
    }
    
    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw UserException.invalidEmailFormat(email);
        }
    }
    
    private void validateEmail(String email, Map<String, String> errors) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.put("email", "Invalid email format");
        }
    }
    
    private void validatePassword(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw UserException.weakPassword();
        }
    }
    
    /**
     * Validates email format based on user role.
     * Email format by role:
     * - ADMIN: username@admin.alquds.edu
     * - DEANSHIP: username@dean.alquds.edu
     * - HOD: hod.department_shortcut@hod.alquds.edu
     * - PROFESSOR: username@staff.alquds.edu
     * 
     * @param email The email to validate
     * @param role The role of the user being created
     */
    private void validateRoleSpecificEmail(String email, Role role) {
        if (email == null || role == null) {
            return; // Basic validation is handled elsewhere
        }
        
        switch (role) {
            case ROLE_ADMIN:
                emailValidationService.validateAdminEmail(email);
                break;
            case ROLE_DEANSHIP:
                emailValidationService.validateDeanshipEmail(email);
                break;
            case ROLE_HOD:
                emailValidationService.validateHodEmail(email);
                break;
            case ROLE_PROFESSOR:
                emailValidationService.validateProfessorEmail(email);
                break;
        }
    }
    
    private void validateRole(Role role) {
        if (role == null) {
            throw UserException.invalidRole("null");
        }
        
        // Get current user to check if they have Admin privileges
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ROLE_ADMIN;
        
        // Admin can create users with any role
        if (isAdmin) {
            // All roles are valid for Admin
            return;
        }
        
        // Non-admin users (Dean) can only create PROFESSOR and HOD roles
        if (role != Role.ROLE_PROFESSOR && role != Role.ROLE_HOD) {
            throw UserException.invalidRole(role.toString());
        }
    }
    
    /**
     * Validates role for Admin user creation.
     * Only Admin users can create other Admin users.
     * 
     * @param role The role to validate
     * @param creatorRole The role of the user creating the new user
     */
    public void validateRoleForCreation(Role role, Role creatorRole) {
        if (role == null) {
            throw UserException.invalidRole("null");
        }
        
        // Admin can create users with any role
        if (creatorRole == Role.ROLE_ADMIN) {
            return;
        }
        
        // Non-admin users cannot create Admin users
        if (role == Role.ROLE_ADMIN) {
            throw UserException.invalidRole(role.toString() + " - Only Admin can create Admin users");
        }
        
        // Dean can create PROFESSOR, HOD, and DEANSHIP roles
        if (creatorRole == Role.ROLE_DEANSHIP) {
            if (role != Role.ROLE_PROFESSOR && role != Role.ROLE_HOD && role != Role.ROLE_DEANSHIP) {
                throw UserException.invalidRole(role.toString());
            }
            return;
        }
        
        // HOD can only create PROFESSOR roles
        if (creatorRole == Role.ROLE_HOD) {
            if (role != Role.ROLE_PROFESSOR) {
                throw UserException.invalidRole(role.toString() + " - HOD can only create Professor users");
            }
            return;
        }
        
        // Professors cannot create users
        throw UserException.invalidRole(role.toString() + " - Insufficient privileges to create users");
    }
    
    private void checkUserDependencies(Long userId) {
        // Check if user has created document requests
        long createdRequestsCount = documentRequestRepository.countByCreatedBy_Id(userId);
        if (createdRequestsCount > 0) {
            throw UserException.userHasDependencies(userId, 
                createdRequestsCount + " created document request(s)");
        }
        
        // Check if user is assigned as professor to document requests
        long assignedRequestsCount = documentRequestRepository.countByProfessor_Id(userId);
        if (assignedRequestsCount > 0) {
            throw UserException.userHasDependencies(userId, 
                assignedRequestsCount + " assigned document request(s)");
        }
        
        // Additional dependency checks can be added here
        // For example: submitted documents, notifications, etc.
    }
    
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            String email = authentication.getName();
            if (!StringUtils.hasText(email)) {
                return null;
            }
            
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            log.warn("Could not get current user: {}", e.getMessage());
            return null;
        }
    }
}
