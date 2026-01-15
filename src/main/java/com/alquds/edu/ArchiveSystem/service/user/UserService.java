package com.alquds.edu.ArchiveSystem.service.user;

import com.alquds.edu.ArchiveSystem.service.auth.EmailValidationService;

import com.alquds.edu.ArchiveSystem.repository.academic.DepartmentRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.repository.file.FolderRepository;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.exception.core.ValidationException;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.repository.auth.RefreshTokenRepository;
import com.alquds.edu.ArchiveSystem.repository.user.NotificationRepository;

import com.alquds.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alquds.edu.ArchiveSystem.dto.user.UserDeletionInfo;
import com.alquds.edu.ArchiveSystem.dto.user.UserResponse;
import com.alquds.edu.ArchiveSystem.dto.user.UserUpdateRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.alquds.edu.ArchiveSystem.exception.domain.UserException;
import com.alquds.edu.ArchiveSystem.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FolderRepository folderRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;
    private final NotificationRepository notificationRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailValidationService emailValidationService;
    
    @Value("${file.upload.directory:uploads/}")
    private String uploadDirectory;
    
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
    
    /**
     * Get information about what will be deleted when a user is removed.
     * This allows admins to preview the deletion impact before confirming.
     * 
     * @param userId User ID to check
     * @return UserDeletionInfo with counts and details
     */
    @Transactional(readOnly = true)
    public UserDeletionInfo getUserDeletionInfo(Long userId) {
        log.info("Getting deletion info for user with id: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.userNotFound(userId));
        
        // Count folders owned by user
        var folders = folderRepository.findByOwnerId(userId);
        int folderCount = folders.size();
        
        // Count files uploaded by user
        long fileCount = uploadedFileRepository.countByUploaderId(userId);
        
        // Get total file size
        Long totalFileSize = uploadedFileRepository.sumFileSizeByUploaderId(userId);
        double totalFileSizeMb = totalFileSize != null ? totalFileSize / (1024.0 * 1024.0) : 0.0;
        
        // Count document submissions
        var submissions = documentSubmissionRepository.findByProfessorId(userId);
        int submissionCount = submissions.size();
        
        // Count course assignments
        var courseAssignments = courseAssignmentRepository.findByProfessorId(userId);
        int courseAssignmentCount = courseAssignments.size();
        
        // Count notifications for user
        var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int notificationCount = notifications.size();
        
        // Determine if user can be deleted and build warning message
        boolean canDelete = true;
        String blockingReason = null;
        StringBuilder warningBuilder = new StringBuilder();
        
        // Check for self-deletion
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getId().equals(userId)) {
            canDelete = false;
            blockingReason = "You cannot delete your own account.";
        }
        
        // Build warning message for deletable items
        if (canDelete) {
            if (folderCount > 0 || fileCount > 0) {
                warningBuilder.append(String.format("This will delete %d folder(s) and %d file(s) (%.2f MB). ", 
                    folderCount, fileCount, totalFileSizeMb));
            }
            if (submissionCount > 0) {
                warningBuilder.append(String.format("%d document submission(s) will be removed. ", submissionCount));
            }
            if (courseAssignmentCount > 0) {
                warningBuilder.append(String.format("%d course assignment(s) will be removed. ", courseAssignmentCount));
            }
            if (warningBuilder.length() == 0) {
                warningBuilder.append("No associated data found. User can be safely deleted.");
            }
        }
        
        return UserDeletionInfo.builder()
                .userId(userId)
                .userName(user.getFirstName() + " " + user.getLastName())
                .userEmail(user.getEmail())
                .userRole(user.getRole().toString().replace("ROLE_", ""))
                .folderCount(folderCount)
                .fileCount((int) fileCount)
                .totalFileSizeMb(Math.round(totalFileSizeMb * 100.0) / 100.0)
                .submissionCount(submissionCount)
                .courseAssignmentCount(courseAssignmentCount)
                .notificationCount(notificationCount)
                .canDelete(canDelete)
                .blockingReason(blockingReason)
                .warningMessage(warningBuilder.toString().trim())
                .build();
    }
    
    /**
     * Delete a user with all associated data.
     * Default behavior deletes folders and files.
     * 
     * @param userId User ID to delete
     */
    public void deleteUser(Long userId) {
        deleteUser(userId, true);
    }
    
    /**
     * Delete a user with optional folder/file deletion.
     * 
     * @param userId User ID to delete
     * @param deleteAllData If true, deletes all associated folders, files, submissions, and assignments.
     *                      If false, only deletes user record (will fail if FK constraints exist).
     */
    public void deleteUser(Long userId, boolean deleteAllData) {
        log.info("Deleting user with id: {}, deleteAllData: {}", userId, deleteAllData);
        
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
        
        if (deleteAllData) {
            // Delete all associated data
            deleteUserAssociatedData(user);
        } else {
            // Only check for dependencies if not deleting all data
            checkUserDependencies(userId);
        }
        
        // Delete all refresh tokens for this user
        refreshTokenRepository.deleteAllByUserId(userId);
        log.debug("Deleted all refresh tokens for user id: {}", userId);
        
        // Delete all notifications for this user
        var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (!notifications.isEmpty()) {
            notificationRepository.deleteAll(notifications);
            log.debug("Deleted {} notifications for user id: {}", notifications.size(), userId);
        }
        
        // Delete folders owned by user (must be done before deleting user due to FK constraint)
        var folders = folderRepository.findByOwnerId(userId);
        if (!folders.isEmpty()) {
            // Collect unique root-level physical directories to delete
            Set<String> physicalPathsToDelete = new HashSet<>();
            for (var folder : folders) {
                if (folder.getPath() != null) {
                    physicalPathsToDelete.add(folder.getPath());
                }
            }
            
            // Sort by path length descending so children are deleted first from DB
            folders.sort((f1, f2) -> Integer.compare(f2.getPath().length(), f1.getPath().length()));
            folderRepository.deleteAll(folders);
            log.debug("Deleted {} folder records from database for user id: {}", folders.size(), userId);
            
            // Delete physical directories (sort by path length descending to delete children first)
            List<String> sortedPaths = physicalPathsToDelete.stream()
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .collect(Collectors.toList());
            
            for (String folderPath : sortedPaths) {
                try {
                    Path physicalPath = Path.of(uploadDirectory, folderPath);
                    if (Files.exists(physicalPath)) {
                        FileSystemUtils.deleteRecursively(physicalPath);
                        log.debug("Deleted physical directory: {}", physicalPath);
                    }
                } catch (IOException e) {
                    log.warn("Could not delete physical directory for path: {}", folderPath, e);
                }
            }
            log.info("Deleted {} physical directories for user id: {}", physicalPathsToDelete.size(), userId);
        }
        
        // Delete the user
        if (user != null) {
            userRepository.delete(user);
            log.info("User deleted successfully with id: {}", userId);
        }
    }
    
    /**
     * Delete all associated data for a user including files, submissions, and assignments.
     * 
     * @param user The user whose data should be deleted
     */
    private void deleteUserAssociatedData(User user) {
        Long userId = user.getId();
        log.info("Deleting all associated data for user id: {}", userId);
        
        try {
            // 1. Delete uploaded files (with physical files from filesystem)
            var uploadedFiles = uploadedFileRepository.findByUploaderId(userId);
            if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
                for (var file : uploadedFiles) {
                    if (file.getFileUrl() != null) {
                        try {
                            Path filePath = Path.of(file.getFileUrl());
                            Files.deleteIfExists(filePath);
                            log.debug("Deleted physical file: {}", file.getFileUrl());
                        } catch (IOException e) {
                            log.warn("Could not delete physical file: {}", file.getFileUrl(), e);
                        }
                    }
                }
                uploadedFileRepository.deleteAll(uploadedFiles);
                log.debug("Deleted {} uploaded files for user id: {}", uploadedFiles.size(), userId);
            }
            
            // 2. Delete document submissions
            var submissions = documentSubmissionRepository.findByProfessorId(userId);
            if (submissions != null && !submissions.isEmpty()) {
                // First delete files associated with these submissions
                for (var submission : submissions) {
                    var filesInSubmission = uploadedFileRepository.findByDocumentSubmissionId(submission.getId());
                    if (filesInSubmission != null && !filesInSubmission.isEmpty()) {
                        for (var file : filesInSubmission) {
                            if (file.getFileUrl() != null) {
                                try {
                                    Path filePath = Path.of(file.getFileUrl());
                                    Files.deleteIfExists(filePath);
                                } catch (IOException e) {
                                    log.warn("Could not delete physical file: {}", file.getFileUrl(), e);
                                }
                            }
                        }
                        uploadedFileRepository.deleteAll(filesInSubmission);
                    }
                }
                documentSubmissionRepository.deleteAll(submissions);
                log.debug("Deleted {} document submissions for user id: {}", submissions.size(), userId);
            }
            
            // 3. Delete course assignments and their related submissions
            var courseAssignments = courseAssignmentRepository.findByProfessorId(userId);
            if (courseAssignments != null && !courseAssignments.isEmpty()) {
                for (var assignment : courseAssignments) {
                    var assignmentSubmissions = documentSubmissionRepository.findByCourseAssignmentId(assignment.getId());
                    if (assignmentSubmissions != null && !assignmentSubmissions.isEmpty()) {
                        // Delete files for these submissions
                        for (var sub : assignmentSubmissions) {
                            var filesInSub = uploadedFileRepository.findByDocumentSubmissionId(sub.getId());
                            if (filesInSub != null && !filesInSub.isEmpty()) {
                                for (var file : filesInSub) {
                                    if (file.getFileUrl() != null) {
                                        try {
                                            Path filePath = Path.of(file.getFileUrl());
                                            Files.deleteIfExists(filePath);
                                        } catch (IOException e) {
                                            log.warn("Could not delete physical file: {}", file.getFileUrl(), e);
                                        }
                                    }
                                }
                                uploadedFileRepository.deleteAll(filesInSub);
                            }
                        }
                        documentSubmissionRepository.deleteAll(assignmentSubmissions);
                    }
                }
                courseAssignmentRepository.deleteAll(courseAssignments);
                log.debug("Deleted {} course assignments for user id: {}", courseAssignments.size(), userId);
            }
            
        } catch (Exception e) {
            log.error("Error deleting associated data for user id: {}", userId, e);
            throw new RuntimeException("Failed to delete user associated data: " + e.getMessage(), e);
        }
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
        // Check if user has course assignments
        var courseAssignments = courseAssignmentRepository.findByProfessorId(userId);
        if (!courseAssignments.isEmpty()) {
            throw UserException.userHasDependencies(userId, 
                courseAssignments.size() + " course assignment(s)");
        }
        
        // Check if user has document submissions
        var submissions = documentSubmissionRepository.findByProfessorId(userId);
        if (!submissions.isEmpty()) {
            throw UserException.userHasDependencies(userId, 
                submissions.size() + " document submission(s)");
        }
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
