package com.alquds.edu.ArchiveSystem.service.academic;

import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.repository.file.FolderRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.DepartmentRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.repository.auth.RefreshTokenRepository;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final FolderRepository folderRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;
    
    public DepartmentServiceImpl(
            DepartmentRepository departmentRepository, 
            UserRepository userRepository,
            CourseRepository courseRepository,
            FolderRepository folderRepository,
            UploadedFileRepository uploadedFileRepository,
            RefreshTokenRepository refreshTokenRepository,
            CourseAssignmentRepository courseAssignmentRepository,
            DocumentSubmissionRepository documentSubmissionRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.folderRepository = folderRepository;
        this.uploadedFileRepository = uploadedFileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.courseAssignmentRepository = courseAssignmentRepository;
        this.documentSubmissionRepository = documentSubmissionRepository;
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
        logger.info("Deleting department ID: {} with all related data", id);
        
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Department not found with ID: " + id));
        
        // Get all users in this department
        List<User> usersInDepartment = userRepository.findByDepartmentId(id);
        logger.info("Found {} users in department ID: {}", usersInDepartment.size(), id);
        
        // Delete all data related to each user
        for (User user : usersInDepartment) {
            deleteUserData(user);
        }
        
        // Delete all courses in this department
        List<Course> coursesInDepartment = courseRepository.findByDepartmentId(id);
        logger.info("Deleting {} courses in department ID: {}", coursesInDepartment.size(), id);
        for (Course course : coursesInDepartment) {
            // Get all course assignments for this course
            var assignments = courseAssignmentRepository.findByCourseId(course.getId());
            for (var assignment : assignments) {
                // Delete submissions and their files for this assignment
                var submissions = documentSubmissionRepository.findByCourseAssignmentId(assignment.getId());
                for (var submission : submissions) {
                    var files = uploadedFileRepository.findByDocumentSubmissionId(submission.getId());
                    for (var file : files) {
                        if (file.getFileUrl() != null) {
                            try {
                                Path filePath = Path.of(file.getFileUrl());
                                Files.deleteIfExists(filePath);
                            } catch (IOException e) {
                                logger.warn("Could not delete physical file: {}", file.getFileUrl(), e);
                            }
                        }
                    }
                    uploadedFileRepository.deleteAll(files);
                }
                documentSubmissionRepository.deleteAll(submissions);
            }
            // Delete course assignments for this course
            courseAssignmentRepository.deleteAll(assignments);
        }
        courseRepository.deleteAll(coursesInDepartment);
        
        // Finally delete the department
        departmentRepository.delete(department);
        logger.info("Successfully deleted department ID: {} with all related data", id);
    }
    
    /**
     * Delete all data related to a user including files, folders, tokens, etc.
     */
    private void deleteUserData(User user) {
        Long userId = user.getId();
        logger.info("Deleting all data for user ID: {} ({})", userId, user.getEmail());
        
        try {
            // 1. First, get all document submissions for this user
            var submissions = documentSubmissionRepository.findByProfessorId(userId);
            logger.debug("Found {} document submissions for user ID: {}", submissions.size(), userId);
            
            // 2. Delete uploaded files for each submission (and from filesystem)
            for (var submission : submissions) {
                var filesInSubmission = uploadedFileRepository.findByDocumentSubmissionId(submission.getId());
                for (var file : filesInSubmission) {
                    // Delete physical file
                    if (file.getFileUrl() != null) {
                        try {
                            Path filePath = Path.of(file.getFileUrl());
                            Files.deleteIfExists(filePath);
                            logger.debug("Deleted physical file: {}", file.getFileUrl());
                        } catch (IOException e) {
                            logger.warn("Could not delete physical file: {}", file.getFileUrl(), e);
                        }
                    }
                }
                uploadedFileRepository.deleteAll(filesInSubmission);
            }
            
            // 3. Also delete any uploaded files by this user (not in submissions)
            var uploadedFiles = uploadedFileRepository.findByUploaderId(userId);
            for (var file : uploadedFiles) {
                if (file.getFileUrl() != null) {
                    try {
                        Path filePath = Path.of(file.getFileUrl());
                        Files.deleteIfExists(filePath);
                        logger.debug("Deleted physical file: {}", file.getFileUrl());
                    } catch (IOException e) {
                        logger.warn("Could not delete physical file: {}", file.getFileUrl(), e);
                    }
                }
            }
            uploadedFileRepository.deleteAll(uploadedFiles);
            logger.debug("Deleted uploaded files for user ID: {}", userId);
            
            // 4. Now delete document submissions for this user
            documentSubmissionRepository.deleteAll(submissions);
            logger.debug("Deleted document submissions for user ID: {}", userId);
            
            // 5. Get all course assignments for this user
            var courseAssignments = courseAssignmentRepository.findByProfessorId(userId);
            logger.debug("Found {} course assignments for user ID: {}", courseAssignments.size(), userId);
            
            // 6. For each course assignment, delete related document submissions (from other users too)
            for (var assignment : courseAssignments) {
                var assignmentSubmissions = documentSubmissionRepository.findByCourseAssignmentId(assignment.getId());
                // Delete uploaded files for these submissions
                for (var sub : assignmentSubmissions) {
                    var filesInSub = uploadedFileRepository.findByDocumentSubmissionId(sub.getId());
                    for (var file : filesInSub) {
                        if (file.getFileUrl() != null) {
                            try {
                                Path filePath = Path.of(file.getFileUrl());
                                Files.deleteIfExists(filePath);
                            } catch (IOException e) {
                                logger.warn("Could not delete physical file: {}", file.getFileUrl(), e);
                            }
                        }
                    }
                    uploadedFileRepository.deleteAll(filesInSub);
                }
                documentSubmissionRepository.deleteAll(assignmentSubmissions);
            }
            
            // 7. Now delete course assignments
            courseAssignmentRepository.deleteAll(courseAssignments);
            logger.debug("Deleted course assignments for user ID: {}", userId);
            
            // 8. Delete folders owned by user
            var folders = folderRepository.findByOwnerId(userId);
            // Delete folders in reverse order (children first)
            folders.sort((f1, f2) -> {
                // Sort by path length descending so children are deleted first
                return Integer.compare(f2.getPath().length(), f1.getPath().length());
            });
            folderRepository.deleteAll(folders);
            logger.debug("Deleted {} folders for user ID: {}", folders.size(), userId);
            
            // 9. Delete refresh tokens
            refreshTokenRepository.deleteAllByUserId(userId);
            logger.debug("Deleted refresh tokens for user ID: {}", userId);
            
            // 10. Finally delete the user
            userRepository.delete(user);
            logger.info("Successfully deleted user ID: {} ({})", userId, user.getEmail());
            
        } catch (Exception e) {
            logger.error("Error deleting data for user ID: {}", userId, e);
            throw new RuntimeException("Failed to delete user data for user ID: " + userId, e);
        }
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
