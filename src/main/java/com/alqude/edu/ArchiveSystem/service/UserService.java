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
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    
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
        
        // Validate and fetch department
        Long departmentId = request.getDepartmentId();
        if (departmentId == null) {
            throw new IllegalArgumentException("Department ID cannot be null");
        }
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> UserException.departmentNotFound(departmentId));
        
        // Validate role
        validateRole(request.getRole());
        
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
        
        // Validate and update department if provided
        Long departmentId = request.getDepartmentId();
        if (departmentId != null) {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> UserException.departmentNotFound(departmentId));
            user.setDepartment(department);
        }
        
        // Update user entity
        userMapper.updateEntity(request, user);
        if (user == null) {
            throw new IllegalStateException("User entity cannot be null after update");
        }
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
        
        // Soft delete or hard delete based on business rules
        // For now, we'll do a hard delete after checking dependencies
        if (user == null) {
            throw new IllegalStateException("User entity cannot be null for deletion");
        }
        userRepository.delete(user);
        log.info("User deleted successfully with id: {}", userId);
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
        
        if (request.getDepartmentId() == null) {
            errors.put("departmentId", "Department is required");
        }
        
        if (request.getRole() == null) {
            errors.put("role", "Role is required");
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
    
    private void validateRole(Role role) {
        if (role == null) {
            throw UserException.invalidRole("null");
        }
        
        // Validate that role is one of the allowed values
        if (role != Role.ROLE_PROFESSOR && role != Role.ROLE_HOD) {
            throw UserException.invalidRole(role.toString());
        }
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
