package com.alquds.edu.ArchiveSystem.service.academic;

import com.alquds.edu.ArchiveSystem.service.auth.EmailValidationService;

import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.repository.academic.DepartmentRepository;
import com.alquds.edu.ArchiveSystem.exception.core.DuplicateEntityException;
import com.alquds.edu.ArchiveSystem.service.file.FolderService;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.exception.core.BusinessException;
import com.alquds.edu.ArchiveSystem.repository.submission.RequiredDocumentTypeRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.entity.submission.RequiredDocumentType;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;

import com.alquds.edu.ArchiveSystem.dto.professor.CourseAssignmentWithStatus;
import com.alquds.edu.ArchiveSystem.dto.professor.ProfessorDashboardOverview;
import com.alquds.edu.ArchiveSystem.dto.user.ProfessorDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of ProfessorService for managing professor operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@SuppressWarnings("null")
public class ProfessorServiceImpl implements ProfessorService {
    
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final FolderService folderService;
    private final EmailValidationService emailValidationService;
    
    @Override
    public User createProfessor(ProfessorDTO dto) {
        log.info("Creating new professor with email: {}", dto.getEmail());
        
        // Validate professor email format (must end with @staff.alquds.edu)
        emailValidationService.validateProfessorEmail(dto.getEmail());
        
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
        
        // Note: Auto-folder creation is not performed here because academic year/semester context
        // is not available at professor creation time. Folders will be created when:
        // 1. Course assignments are made (which have semester context)
        // 2. Manual folder creation endpoint is called
        // This prevents creating folders for all possible year/semester combinations
        
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
        
        // Validate professor email format if email is being changed
        if (!professor.getEmail().equals(dto.getEmail())) {
            emailValidationService.validateProfessorEmail(dto.getEmail());
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
        
        List<User> professors = userRepository.findByDepartmentIdAndRole(departmentId, Role.ROLE_PROFESSOR);
        
        // Eagerly load department to avoid lazy loading issues in JSON serialization
        professors.forEach(prof -> {
            if (prof.getDepartment() != null) {
                prof.getDepartment().getName(); // Force load
            }
        });
        
        return professors;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<User> getAllProfessors() {
        log.debug("Fetching all professors across all departments");
        
        // Get all users and filter by professor role
        List<User> professors = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ROLE_PROFESSOR)
                .collect(java.util.stream.Collectors.toList());
        
        // Eagerly load department to avoid lazy loading issues in JSON serialization
        professors.forEach(prof -> {
            if (prof.getDepartment() != null) {
                prof.getDepartment().getName(); // Force load
            }
        });
        
        return professors;
    }
    
    /**
     * Get all professors with department-scoped filtering.
     * For HOD and Professor: only returns professors in their department.
     * For Deanship: returns all professors.
     * 
     * @param currentUser The current authenticated user
     * @return Filtered list of professors
     */
    @Transactional(readOnly = true)
    public List<User> getAllProfessors(User currentUser) {
        log.debug("Fetching all professors with department filtering for user: {}", currentUser.getEmail());
        
        // For Deanship, get all professors from all departments
        if (currentUser.getRole() == Role.ROLE_DEANSHIP) {
            // Get all professors by querying all departments
            List<User> allProfessors = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.ROLE_PROFESSOR)
                    .collect(java.util.stream.Collectors.toList());
            log.debug("Deanship user - returning all {} professors", allProfessors.size());
            return allProfessors;
        }
        
        // For HOD and Professor, get only professors in their department
        if (currentUser.getDepartment() == null) {
            log.warn("User {} has no department assigned", currentUser.getEmail());
            return List.of();
        }
        
        List<User> departmentProfessors = userRepository.findByDepartmentIdAndRole(
                currentUser.getDepartment().getId(), Role.ROLE_PROFESSOR);
        
        log.debug("{} user - returning {} professors in department {}", 
                currentUser.getRole(), departmentProfessors.size(), 
                currentUser.getDepartment().getName());
        
        return departmentProfessors;
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
    
    @Override
    @Transactional(readOnly = true)
    public List<CourseAssignmentWithStatus> getProfessorCoursesWithStatus(Long professorId, Long semesterId) {
        log.debug("Fetching course assignments with status for professor ID: {} in semester ID: {}", professorId, semesterId);
        
        // Validate professor exists
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found with ID: " + professorId));
        
        // Validate role is professor
        if (professor.getRole() != Role.ROLE_PROFESSOR) {
            throw new BusinessException("INVALID_ROLE", "User with ID " + professorId + " is not a professor");
        }
        
        // Fetch course assignments with JOIN FETCH (already optimized in repository)
        List<CourseAssignment> assignments = courseAssignmentRepository.findByProfessorIdAndSemesterId(professorId, semesterId);
        
        if (assignments.isEmpty()) {
            log.debug("No course assignments found for professor ID: {} in semester ID: {}", professorId, semesterId);
            return List.of();
        }
        
        // Batch fetch all required document types for all courses in one query
        List<Long> courseIds = assignments.stream()
                .map(a -> a.getCourse().getId())
                .distinct()
                .collect(Collectors.toList());
        
        List<RequiredDocumentType> allRequiredDocs = requiredDocumentTypeRepository
                .findByCourseIdInAndSemesterId(courseIds, semesterId);
        
        // Group required docs by course ID for efficient lookup
        Map<Long, List<RequiredDocumentType>> requiredDocsByCourse = allRequiredDocs.stream()
                .collect(Collectors.groupingBy(rd -> rd.getCourse().getId()));
        
        // Batch fetch all submissions for all assignments in one query
        List<Long> assignmentIds = assignments.stream()
                .map(CourseAssignment::getId)
                .collect(Collectors.toList());
        
        List<DocumentSubmission> allSubmissions = documentSubmissionRepository
                .findByCourseAssignmentIdIn(assignmentIds);
        
        // Group submissions by assignment ID for efficient lookup
        Map<Long, List<DocumentSubmission>> submissionsByAssignment = allSubmissions.stream()
                .collect(Collectors.groupingBy(s -> s.getCourseAssignment().getId()));
        
        return assignments.stream().map(assignment -> {
            Course course = assignment.getCourse();
            Semester semester = assignment.getSemester();
            AcademicYear academicYear = semester.getAcademicYear();
            
            // Get required document types for this course from pre-fetched map
            List<RequiredDocumentType> requiredDocs = requiredDocsByCourse.getOrDefault(course.getId(), List.of());
            
            // Get submissions for this assignment from pre-fetched map
            List<DocumentSubmission> submissions = submissionsByAssignment.getOrDefault(assignment.getId(), List.of());
            
            // Build document status map
            Map<DocumentTypeEnum, CourseAssignmentWithStatus.DocumentTypeStatus> documentStatuses = new HashMap<>();
            
            for (RequiredDocumentType requiredDoc : requiredDocs) {
                DocumentTypeEnum docType = requiredDoc.getDocumentType();
                
                // Find submission for this document type
                DocumentSubmission submission = submissions.stream()
                        .filter(s -> s.getDocumentType() == docType)
                        .findFirst()
                        .orElse(null);
                
                CourseAssignmentWithStatus.DocumentTypeStatus status = CourseAssignmentWithStatus.DocumentTypeStatus.builder()
                        .documentType(docType)
                        .deadline(requiredDoc.getDeadline())
                        .isRequired(requiredDoc.getIsRequired())
                        .maxFileCount(requiredDoc.getMaxFileCount())
                        .maxTotalSizeMb(requiredDoc.getMaxTotalSizeMb())
                        .build();
                
                if (submission != null) {
                    // Submission exists - determine current status
                    // Check if deadline has passed and submission is late
                    if (requiredDoc.getDeadline() != null && 
                        submission.getSubmittedAt() != null &&
                        submission.getSubmittedAt().isAfter(requiredDoc.getDeadline())) {
                        // Submitted after deadline - mark as late
                        status.setStatus(SubmissionStatus.UPLOADED);
                        status.setIsLateSubmission(true);
                    } else if (submission.getStatus() == SubmissionStatus.UPLOADED) {
                        // Submitted on time or no deadline
                        status.setStatus(SubmissionStatus.UPLOADED);
                        status.setIsLateSubmission(submission.getIsLateSubmission());
                    } else {
                        // Use submission's current status
                        status.setStatus(submission.getStatus());
                        status.setIsLateSubmission(submission.getIsLateSubmission());
                    }
                    
                    status.setSubmissionId(submission.getId());
                    status.setSubmittedAt(submission.getSubmittedAt());
                    status.setFileCount(submission.getFileCount());
                    status.setTotalFileSize(submission.getTotalFileSize());
                    status.setNotes(submission.getNotes());
                } else {
                    // No submission yet - determine status based on deadline
                    if (requiredDoc.getDeadline() != null && LocalDateTime.now().isAfter(requiredDoc.getDeadline())) {
                        status.setStatus(SubmissionStatus.OVERDUE);
                        status.setIsLateSubmission(false);
                    } else {
                        status.setStatus(SubmissionStatus.NOT_UPLOADED);
                        status.setIsLateSubmission(false);
                    }
                }
                
                documentStatuses.put(docType, status);
            }
            
            return CourseAssignmentWithStatus.builder()
                    .courseAssignmentId(assignment.getId())
                    .courseId(course.getId())
                    .courseCode(course.getCourseCode())
                    .courseName(course.getCourseName())
                    .courseLevel(course.getLevel())
                    .departmentName(course.getDepartment().getName())
                    .semesterId(semester.getId())
                    .semesterType(semester.getType().name())
                    .academicYear(academicYear.getYearCode())
                    .documentStatuses(documentStatuses)
                    .build();
        }).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProfessorDashboardOverview getProfessorDashboardOverview(Long professorId, Long semesterId) {
        log.debug("Generating dashboard overview for professor ID: {} in semester ID: {}", professorId, semesterId);
        
        // Validate professor exists
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found with ID: " + professorId));
        
        // Validate role is professor
        if (professor.getRole() != Role.ROLE_PROFESSOR) {
            throw new BusinessException("INVALID_ROLE", "User with ID " + professorId + " is not a professor");
        }
        
        // Get course assignments with status
        List<CourseAssignmentWithStatus> coursesWithStatus = getProfessorCoursesWithStatus(professorId, semesterId);
        
        // Calculate statistics
        int totalCourses = coursesWithStatus.size();
        int totalRequiredDocuments = 0;
        int submittedDocuments = 0;
        int missingDocuments = 0;
        int overdueDocuments = 0;
        
        for (CourseAssignmentWithStatus course : coursesWithStatus) {
            for (CourseAssignmentWithStatus.DocumentTypeStatus docStatus : course.getDocumentStatuses().values()) {
                if (docStatus.getIsRequired()) {
                    totalRequiredDocuments++;
                    
                    switch (docStatus.getStatus()) {
                        case UPLOADED:
                            submittedDocuments++;
                            break;
                        case OVERDUE:
                            overdueDocuments++;
                            missingDocuments++;
                            break;
                        case NOT_UPLOADED:
                            missingDocuments++;
                            break;
                    }
                }
            }
        }
        
        // Calculate completion percentage
        double completionPercentage = totalRequiredDocuments > 0 
                ? (double) submittedDocuments / totalRequiredDocuments * 100.0 
                : 0.0;
        
        // Get semester info
        Semester semester = coursesWithStatus.isEmpty() ? null : 
                courseAssignmentRepository.findById(coursesWithStatus.get(0).getCourseAssignmentId())
                        .map(CourseAssignment::getSemester)
                        .orElse(null);
        
        String semesterName = semester != null ? 
                semester.getType().name() + " " + semester.getAcademicYear().getYearCode() : "Unknown";
        String academicYear = semester != null ? semester.getAcademicYear().getYearCode() : "Unknown";
        
        return ProfessorDashboardOverview.builder()
                .professorId(professor.getId())
                .professorName(professor.getFirstName() + " " + professor.getLastName())
                .professorEmail(professor.getEmail())
                .departmentName(professor.getDepartment().getName())
                .semesterId(semesterId)
                .semesterName(semesterName)
                .academicYear(academicYear)
                .generatedAt(LocalDateTime.now())
                .totalCourses(totalCourses)
                .totalRequiredDocuments(totalRequiredDocuments)
                .submittedDocuments(submittedDocuments)
                .missingDocuments(missingDocuments)
                .overdueDocuments(overdueDocuments)
                .completionPercentage(completionPercentage)
                .build();
    }
    
    @Override
    public com.alquds.edu.ArchiveSystem.entity.file.Folder createProfessorFolder(Long professorId, Long academicYearId, Long semesterId) {
        log.info("Creating professor folder for professor ID: {}, academic year ID: {}, semester ID: {}", 
                professorId, academicYearId, semesterId);
        
        // Validate professor exists and is a professor
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found with ID: " + professorId));
        
        if (professor.getRole() != Role.ROLE_PROFESSOR) {
            throw new BusinessException("INVALID_ROLE", "User with ID " + professorId + " is not a professor");
        }
        
        try {
            // Call FolderService to create professor folder
            com.alquds.edu.ArchiveSystem.entity.file.Folder folder = folderService.createProfessorFolder(
                    professorId, academicYearId, semesterId);
            
            log.info("Successfully created/retrieved professor folder with path: {}", folder.getPath());
            return folder;
        } catch (Exception e) {
            log.error("Error creating professor folder for professor ID: {}", professorId, e);
            throw new BusinessException("FOLDER_CREATION_ERROR", 
                    "Failed to create professor folder: " + e.getMessage());
        }
    }
}
