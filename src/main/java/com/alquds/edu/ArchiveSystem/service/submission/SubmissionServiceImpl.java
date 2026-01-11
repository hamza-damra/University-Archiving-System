package com.alquds.edu.ArchiveSystem.service.submission;

import com.alquds.edu.ArchiveSystem.service.core.DepartmentScopedFilterService;

import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.repository.submission.RequiredDocumentTypeRepository;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.entity.submission.RequiredDocumentType;
import com.alquds.edu.ArchiveSystem.repository.academic.SemesterRepository;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;

import com.alquds.edu.ArchiveSystem.dto.report.SubmissionStatistics;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class SubmissionServiceImpl implements SubmissionService {
    
    private final DocumentSubmissionRepository documentSubmissionRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final UserRepository userRepository;
    private final RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    private final SemesterRepository semesterRepository;
    private final DepartmentScopedFilterService departmentScopedFilterService;
    
    /**
     * Create a new DocumentSubmission record for a course assignment and document type.
     * Initial status is set to NOT_UPLOADED.
     * 
     * @param courseAssignmentId The ID of the course assignment
     * @param documentType The type of document being submitted
     * @param professorId The ID of the professor making the submission
     * @param notes Optional notes for the submission
     * @return The created DocumentSubmission
     */
    @Override
    @Transactional
    public DocumentSubmission createSubmission(Long courseAssignmentId, DocumentTypeEnum documentType, 
                                              Long professorId, String notes) {
        log.info("Creating submission for courseAssignmentId: {}, documentType: {}, professorId: {}", 
                courseAssignmentId, documentType, professorId);
        
        // Validate course assignment exists
        CourseAssignment courseAssignment = courseAssignmentRepository.findById(courseAssignmentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Course assignment not found with id: " + courseAssignmentId));
        
        // Validate professor exists
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Professor not found with id: " + professorId));
        
        // Check if submission already exists for this course assignment and document type
        documentSubmissionRepository.findByCourseAssignmentIdAndDocumentType(
                courseAssignmentId, documentType)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Submission already exists for this course assignment and document type");
                });
        
        // Create new submission
        DocumentSubmission submission = new DocumentSubmission();
        submission.setCourseAssignment(courseAssignment);
        submission.setDocumentType(documentType);
        submission.setProfessor(professor);
        submission.setNotes(notes);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.NOT_UPLOADED);
        submission.setIsLateSubmission(false);
        submission.setFileCount(0);
        submission.setTotalFileSize(0L);
        
        DocumentSubmission savedSubmission = documentSubmissionRepository.save(submission);
        log.info("Created submission with id: {}", savedSubmission.getId());
        
        return savedSubmission;
    }
    
    /**
     * Update submission notes and recalculate status if needed.
     * 
     * @param submissionId The ID of the submission to update
     * @param notes The new notes
     * @return The updated DocumentSubmission
     */
    @Override
    @Transactional
    public DocumentSubmission updateSubmission(Long submissionId, String notes) {
        log.info("Updating submission with id: {}", submissionId);
        
        DocumentSubmission submission = documentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Submission not found with id: " + submissionId));
        
        submission.setNotes(notes);
        
        // Recalculate status based on current state and deadline
        LocalDateTime deadline = getDeadlineForSubmission(submission);
        calculateSubmissionStatus(submission, deadline);
        
        DocumentSubmission updatedSubmission = documentSubmissionRepository.save(submission);
        log.info("Updated submission with id: {}", updatedSubmission.getId());
        
        return updatedSubmission;
    }
    
    /**
     * Helper method to get the deadline for a submission by looking up the required document type.
     * 
     * @param submission The document submission
     * @return The deadline, or null if not found or not set
     */
    private LocalDateTime getDeadlineForSubmission(DocumentSubmission submission) {
        CourseAssignment courseAssignment = submission.getCourseAssignment();
        DocumentTypeEnum documentType = submission.getDocumentType();
        
        // Try to find required document type for this course and semester
        return requiredDocumentTypeRepository
                .findByCourseIdAndSemesterId(
                        courseAssignment.getCourse().getId(), 
                        courseAssignment.getSemester().getId())
                .stream()
                .filter(rdt -> rdt.getDocumentType() == documentType)
                .findFirst()
                .map(RequiredDocumentType::getDeadline)
                .orElse(null);
    }
    
    /**
     * Get a single submission by ID.
     * 
     * @param submissionId The ID of the submission
     * @return The DocumentSubmission
     * @throws EntityNotFoundException if submission not found
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentSubmission getSubmission(Long submissionId) {
        log.debug("Getting submission with id: {}", submissionId);
        
        return documentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Submission not found with id: " + submissionId));
    }
    
    /**
     * Get all submissions for a professor in a specific semester.
     * 
     * @param professorId The ID of the professor
     * @param semesterId The ID of the semester
     * @return List of DocumentSubmissions
     */
    @Override
    @Transactional(readOnly = true)
    public List<DocumentSubmission> getSubmissionsByProfessor(Long professorId, Long semesterId) {
        log.debug("Getting submissions for professorId: {}, semesterId: {}", professorId, semesterId);
        
        // Validate professor exists
        if (!userRepository.existsById(professorId)) {
            throw new EntityNotFoundException("Professor not found with id: " + professorId);
        }
        
        // Validate semester exists
        if (!semesterRepository.existsById(semesterId)) {
            throw new EntityNotFoundException("Semester not found with id: " + semesterId);
        }
        
        return documentSubmissionRepository.findByProfessorIdAndCourseAssignment_SemesterId(
                professorId, semesterId);
    }
    
    /**
     * Get all submissions for a semester with department-scoped filtering.
     * For HOD and Professor: only returns submissions in their department.
     * For Deanship: returns all submissions.
     * 
     * @param semesterId The ID of the semester
     * @param currentUser The current authenticated user
     * @return Filtered list of DocumentSubmissions
     */
    @Transactional(readOnly = true)
    public List<DocumentSubmission> getSubmissionsBySemester(Long semesterId, User currentUser) {
        log.debug("Getting submissions for semesterId: {} with department filtering for user: {}", 
                semesterId, currentUser.getEmail());
        
        // Validate semester exists
        if (!semesterRepository.existsById(semesterId)) {
            throw new EntityNotFoundException("Semester not found with id: " + semesterId);
        }
        
        // Get all course assignments for the semester
        List<CourseAssignment> assignments = courseAssignmentRepository.findBySemesterId(semesterId);
        
        // Apply department-scoped filtering to assignments
        List<CourseAssignment> filteredAssignments = 
                departmentScopedFilterService.filterCourseAssignments(assignments, currentUser);
        
        // Get submissions for filtered assignments
        List<DocumentSubmission> submissions = filteredAssignments.stream()
                .flatMap(assignment -> documentSubmissionRepository
                        .findByCourseAssignmentId(assignment.getId()).stream())
                .collect(java.util.stream.Collectors.toList());
        
        log.debug("Found {} submissions after department filtering", submissions.size());
        return submissions;
    }
    
    /**
     * Get all submissions for a specific course assignment.
     * 
     * @param courseAssignmentId The ID of the course assignment
     * @return List of DocumentSubmissions
     */
    @Override
    @Transactional(readOnly = true)
    public List<DocumentSubmission> getSubmissionsByCourse(Long courseAssignmentId) {
        log.debug("Getting submissions for courseAssignmentId: {}", courseAssignmentId);
        
        // Validate course assignment exists
        if (!courseAssignmentRepository.existsById(courseAssignmentId)) {
            throw new EntityNotFoundException(
                    "Course assignment not found with id: " + courseAssignmentId);
        }
        
        return documentSubmissionRepository.findByCourseAssignmentId(courseAssignmentId);
    }
    
    /**
     * Calculate the submission status based on submitted_at timestamp and deadline.
     * Sets status to UPLOADED, OVERDUE, or NOT_UPLOADED.
     * Sets is_late_submission flag if submitted after deadline.
     * 
     * @param submission The document submission to evaluate
     * @param deadline The deadline for the submission (can be null)
     * @return The calculated SubmissionStatus
     */
    @Override
    public SubmissionStatus calculateSubmissionStatus(DocumentSubmission submission, LocalDateTime deadline) {
        log.debug("Calculating submission status for submission id: {}, deadline: {}", 
                submission.getId(), deadline);
        
        // If no files uploaded, status is NOT_UPLOADED
        if (submission.getFileCount() == null || submission.getFileCount() == 0) {
            // Check if deadline has passed
            if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
                submission.setStatus(SubmissionStatus.OVERDUE);
                submission.setIsLateSubmission(false); // No submission made
                return SubmissionStatus.OVERDUE;
            }
            submission.setStatus(SubmissionStatus.NOT_UPLOADED);
            submission.setIsLateSubmission(false);
            return SubmissionStatus.NOT_UPLOADED;
        }
        
        // Files are uploaded
        LocalDateTime submittedAt = submission.getSubmittedAt();
        
        // If no deadline specified, status is simply UPLOADED
        if (deadline == null) {
            submission.setStatus(SubmissionStatus.UPLOADED);
            submission.setIsLateSubmission(false);
            return SubmissionStatus.UPLOADED;
        }
        
        // Check if submitted after deadline
        if (submittedAt.isAfter(deadline)) {
            submission.setStatus(SubmissionStatus.UPLOADED);
            submission.setIsLateSubmission(true);
            log.info("Submission {} was submitted late. Deadline: {}, Submitted: {}", 
                    submission.getId(), deadline, submittedAt);
            return SubmissionStatus.UPLOADED;
        }
        
        // Submitted on time
        submission.setStatus(SubmissionStatus.UPLOADED);
        submission.setIsLateSubmission(false);
        return SubmissionStatus.UPLOADED;
    }
    
    /**
     * Get submission statistics for a semester, optionally filtered by department.
     * Calculates total required documents, submitted, missing, and overdue counts.
     * 
     * @param semesterId The ID of the semester
     * @param departmentId The ID of the department (null for all departments)
     * @return SubmissionStatistics with aggregated counts
     */
    @Override
    @Transactional(readOnly = true)
    public SubmissionStatistics getStatisticsBySemester(Long semesterId, Long departmentId) {
        log.info("Getting statistics for semesterId: {}, departmentId: {}", semesterId, departmentId);
        
        // Validate semester exists
        if (!semesterRepository.existsById(semesterId)) {
            throw new EntityNotFoundException("Semester not found with id: " + semesterId);
        }
        
        // Get all course assignments for the semester
        List<CourseAssignment> courseAssignments = courseAssignmentRepository.findBySemesterId(semesterId);
        
        // Filter by department if specified
        if (departmentId != null) {
            courseAssignments = courseAssignments.stream()
                    .filter(ca -> ca.getCourse().getDepartment() != null && 
                                 ca.getCourse().getDepartment().getId().equals(departmentId))
                    .toList();
        }
        
        // Count unique professors and courses
        long totalProfessors = courseAssignments.stream()
                .map(ca -> ca.getProfessor().getId())
                .distinct()
                .count();
        
        long totalCourses = courseAssignments.stream()
                .map(ca -> ca.getCourse().getId())
                .distinct()
                .count();
        
        // Calculate required documents count using arrays for mutability in lambdas
        final int[] counts = new int[4]; // [totalRequired, submitted, missing, overdue]
        
        for (CourseAssignment assignment : courseAssignments) {
            // Get required document types for this course and semester
            List<RequiredDocumentType> requiredTypes = requiredDocumentTypeRepository
                    .findByCourseIdAndSemesterId(
                            assignment.getCourse().getId(), 
                            assignment.getSemester().getId());
            
            // If no semester-specific requirements, get course-level requirements
            if (requiredTypes.isEmpty()) {
                requiredTypes = requiredDocumentTypeRepository
                        .findByCourseId(assignment.getCourse().getId())
                        .stream()
                        .filter(rdt -> rdt.getSemester() == null)
                        .toList();
            }
            
            counts[0] += requiredTypes.size(); // totalRequiredDocuments
            
            // Check submission status for each required document type
            for (RequiredDocumentType requiredType : requiredTypes) {
                DocumentTypeEnum docType = requiredType.getDocumentType();
                LocalDateTime deadline = requiredType.getDeadline();
                
                // Find submission for this document type
                documentSubmissionRepository
                        .findByCourseAssignmentIdAndDocumentType(assignment.getId(), docType)
                        .ifPresentOrElse(
                                submission -> {
                                    // Recalculate status based on current state
                                    SubmissionStatus status = calculateSubmissionStatus(submission, deadline);
                                    
                                    if (status == SubmissionStatus.UPLOADED) {
                                        counts[1]++; // submittedDocuments
                                    } else if (status == SubmissionStatus.OVERDUE) {
                                        counts[3]++; // overdueDocuments
                                    } else {
                                        counts[2]++; // missingDocuments
                                    }
                                },
                                () -> {
                                    // No submission found
                                    if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
                                        counts[3]++; // overdueDocuments
                                    } else {
                                        counts[2]++; // missingDocuments
                                    }
                                }
                        );
            }
        }
        
        return SubmissionStatistics.builder()
                .totalProfessors((int) totalProfessors)
                .totalCourses((int) totalCourses)
                .totalRequiredDocuments(counts[0])
                .submittedDocuments(counts[1])
                .missingDocuments(counts[2])
                .overdueDocuments(counts[3])
                .build();
    }
    
    /**
     * Get submission statistics for a semester with automatic department-scoped filtering.
     * For HOD and Professor: only includes statistics for their department.
     * For Deanship: includes all departments.
     * 
     * @param semesterId The ID of the semester
     * @param currentUser The current authenticated user
     * @return SubmissionStatistics with aggregated counts
     */
    @Transactional(readOnly = true)
    public SubmissionStatistics getStatisticsBySemester(Long semesterId, User currentUser) {
        log.info("Getting statistics for semesterId: {} with department filtering for user: {}", 
                semesterId, currentUser.getEmail());
        
        // Get department ID for filtering based on user role
        Long departmentId = departmentScopedFilterService.getDepartmentIdForFiltering(currentUser);
        
        // Use existing method with department filtering
        return getStatisticsBySemester(semesterId, departmentId);
    }
    
    /**
     * Get submission statistics for a specific professor in a semester.
     * 
     * @param professorId The ID of the professor
     * @param semesterId The ID of the semester
     * @return SubmissionStatistics for the professor
     */
    @Override
    @Transactional(readOnly = true)
    public SubmissionStatistics getStatisticsByProfessor(Long professorId, Long semesterId) {
        log.info("Getting statistics for professorId: {}, semesterId: {}", professorId, semesterId);
        
        // Validate professor exists
        if (!userRepository.existsById(professorId)) {
            throw new EntityNotFoundException("Professor not found with id: " + professorId);
        }
        
        // Validate semester exists
        if (!semesterRepository.existsById(semesterId)) {
            throw new EntityNotFoundException("Semester not found with id: " + semesterId);
        }
        
        // Get all course assignments for this professor in the semester
        List<CourseAssignment> courseAssignments = courseAssignmentRepository
                .findByProfessorIdAndSemesterId(professorId, semesterId);
        
        int totalCourses = courseAssignments.size();
        
        // Calculate required documents count using arrays for mutability in lambdas
        final int[] counts = new int[4]; // [totalRequired, submitted, missing, overdue]
        
        for (CourseAssignment assignment : courseAssignments) {
            // Get required document types for this course and semester
            List<RequiredDocumentType> requiredTypes = requiredDocumentTypeRepository
                    .findByCourseIdAndSemesterId(
                            assignment.getCourse().getId(), 
                            assignment.getSemester().getId());
            
            // If no semester-specific requirements, get course-level requirements
            if (requiredTypes.isEmpty()) {
                requiredTypes = requiredDocumentTypeRepository
                        .findByCourseId(assignment.getCourse().getId())
                        .stream()
                        .filter(rdt -> rdt.getSemester() == null)
                        .toList();
            }
            
            counts[0] += requiredTypes.size(); // totalRequiredDocuments
            
            // Check submission status for each required document type
            for (RequiredDocumentType requiredType : requiredTypes) {
                DocumentTypeEnum docType = requiredType.getDocumentType();
                LocalDateTime deadline = requiredType.getDeadline();
                
                // Find submission for this document type
                documentSubmissionRepository
                        .findByCourseAssignmentIdAndDocumentType(assignment.getId(), docType)
                        .ifPresentOrElse(
                                submission -> {
                                    // Recalculate status based on current state
                                    SubmissionStatus status = calculateSubmissionStatus(submission, deadline);
                                    
                                    if (status == SubmissionStatus.UPLOADED) {
                                        counts[1]++; // submittedDocuments
                                    } else if (status == SubmissionStatus.OVERDUE) {
                                        counts[3]++; // overdueDocuments
                                    } else {
                                        counts[2]++; // missingDocuments
                                    }
                                },
                                () -> {
                                    // No submission found
                                    if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
                                        counts[3]++; // overdueDocuments
                                    } else {
                                        counts[2]++; // missingDocuments
                                    }
                                }
                        );
            }
        }
        
        return SubmissionStatistics.builder()
                .totalProfessors(1) // Single professor
                .totalCourses(totalCourses)
                .totalRequiredDocuments(counts[0])
                .submittedDocuments(counts[1])
                .missingDocuments(counts[2])
                .overdueDocuments(counts[3])
                .build();
    }
}
