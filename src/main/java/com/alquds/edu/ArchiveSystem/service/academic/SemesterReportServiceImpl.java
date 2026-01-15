package com.alquds.edu.ArchiveSystem.service.academic;

import com.alquds.edu.ArchiveSystem.dto.report.ReportFilterOptions;
import com.alquds.edu.ArchiveSystem.dto.report.DepartmentReportSummary;
import com.alquds.edu.ArchiveSystem.service.report.PdfReportService;
import com.alquds.edu.ArchiveSystem.dto.report.ProfessorSubmissionReport;
import com.alquds.edu.ArchiveSystem.dto.report.ProfessorSubmissionRow;
import com.alquds.edu.ArchiveSystem.dto.report.SystemWideReport;
import com.alquds.edu.ArchiveSystem.service.core.DepartmentScopedFilterService;
import com.alquds.edu.ArchiveSystem.dto.report.SubmissionStatistics;
import com.alquds.edu.ArchiveSystem.dto.report.ReportFilter;
import com.alquds.edu.ArchiveSystem.dto.report.DocumentStatusInfo;

import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.repository.academic.DepartmentRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.repository.submission.RequiredDocumentTypeRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.entity.submission.RequiredDocumentType;
import com.alquds.edu.ArchiveSystem.repository.academic.SemesterRepository;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;

import com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class SemesterReportServiceImpl implements SemesterReportService {
    
    private final SemesterRepository semesterRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;
    private final UserRepository userRepository;
    private final PdfReportService pdfReportService;
    private final DepartmentScopedFilterService departmentScopedFilterService;
    
    @Override
    @Transactional(readOnly = true)
    public ProfessorSubmissionReport generateProfessorSubmissionReport(Long semesterId, Long departmentId) {
        User currentUser = getCurrentUser();
        return generateProfessorSubmissionReportWithRoleFilter(semesterId, departmentId, currentUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProfessorSubmissionReport generateProfessorSubmissionReportWithRoleFilter(Long semesterId, Long departmentId, User currentUser) {
        log.info("Generating professor submission report for semester {} and department {} with role-based filtering", semesterId, departmentId);
        
        // Validate semester exists
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found with id: " + semesterId));
        
        // Apply role-based department filtering
        Long effectiveDepartmentId = getEffectiveDepartmentId(departmentId, currentUser);
        
        // Validate department exists
        Department department = departmentRepository.findById(effectiveDepartmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + effectiveDepartmentId));
        
        // Validate department access for HOD and Professor roles
        departmentScopedFilterService.validateDepartmentAccess(effectiveDepartmentId, currentUser);
        
        // Fetch all course assignments for this semester and department using optimized query with eager loading
        // This prevents N+1 query issues by loading all related entities in a single query
        List<CourseAssignment> courseAssignments = courseAssignmentRepository.findBySemesterIdWithEagerLoading(semesterId)
                .stream()
                .filter(ca -> ca.getProfessor().getDepartment() != null && 
                             ca.getProfessor().getDepartment().getId().equals(effectiveDepartmentId))
                .collect(Collectors.toList());
        
        log.debug("Found {} course assignments for semester {} and department {} (using optimized query)", 
                courseAssignments.size(), semesterId, effectiveDepartmentId);
        
        // Build report rows
        List<ProfessorSubmissionRow> rows = new ArrayList<>();
        Set<Long> uniqueProfessors = new HashSet<>();
        Set<Long> uniqueCourses = new HashSet<>();
        int totalRequiredDocuments = 0;
        int submittedDocuments = 0;
        int missingDocuments = 0;
        int overdueDocuments = 0;
        
        for (CourseAssignment assignment : courseAssignments) {
            uniqueProfessors.add(assignment.getProfessor().getId());
            uniqueCourses.add(assignment.getCourse().getId());
            
            // Get required document types for this course
            List<RequiredDocumentType> requiredDocs = requiredDocumentTypeRepository
                    .findByCourseIdAndSemesterId(assignment.getCourse().getId(), semesterId);
            
            // If no semester-specific requirements, get course-level requirements
            if (requiredDocs.isEmpty()) {
                requiredDocs = requiredDocumentTypeRepository.findByCourseId(assignment.getCourse().getId())
                        .stream()
                        .filter(rdt -> rdt.getSemester() == null)
                        .collect(Collectors.toList());
            }
            
            // Build document status map
            Map<DocumentTypeEnum, DocumentStatusInfo> documentStatuses = new HashMap<>();
            
            for (RequiredDocumentType requiredDoc : requiredDocs) {
                totalRequiredDocuments++;
                
                // Find submission for this document type
                Optional<DocumentSubmission> submission = documentSubmissionRepository
                        .findByCourseAssignmentIdAndDocumentType(assignment.getId(), requiredDoc.getDocumentType());
                
                SubmissionStatus status;
                LocalDateTime submittedAt = null;
                Boolean isLateSubmission = false;
                
                if (submission.isPresent()) {
                    DocumentSubmission sub = submission.get();
                    status = sub.getStatus();
                    submittedAt = sub.getSubmittedAt();
                    isLateSubmission = sub.getIsLateSubmission();
                    
                    if (status == SubmissionStatus.UPLOADED) {
                        submittedDocuments++;
                    } else if (status == SubmissionStatus.OVERDUE) {
                        overdueDocuments++;
                        missingDocuments++;
                    }
                } else {
                    // Check if deadline has passed
                    if (requiredDoc.getDeadline() != null && 
                        requiredDoc.getDeadline().isBefore(LocalDateTime.now())) {
                        status = SubmissionStatus.OVERDUE;
                        overdueDocuments++;
                    } else {
                        status = SubmissionStatus.NOT_UPLOADED;
                    }
                    missingDocuments++;
                }
                
                // Create DocumentStatusInfo with all relevant information
                DocumentStatusInfo statusInfo = DocumentStatusInfo.builder()
                        .status(status)
                        .deadline(requiredDoc.getDeadline())
                        .submittedAt(submittedAt)
                        .isLateSubmission(isLateSubmission)
                        .build();
                
                documentStatuses.put(requiredDoc.getDocumentType(), statusInfo);
            }
            
            // Create row if there are required documents
            if (!documentStatuses.isEmpty()) {
                ProfessorSubmissionRow row = ProfessorSubmissionRow.builder()
                        .professorId(assignment.getProfessor().getId())
                        .professorName(assignment.getProfessor().getFirstName() + " " + 
                                     assignment.getProfessor().getLastName())
                        .professorEmail(assignment.getProfessor().getEmail())
                        .courseAssignmentId(assignment.getId())
                        .courseCode(assignment.getCourse().getCourseCode())
                        .courseName(assignment.getCourse().getCourseName())
                        .documentStatuses(documentStatuses)
                        .build();
                
                rows.add(row);
            }
        }
        
        // Build statistics
        SubmissionStatistics statistics = SubmissionStatistics.builder()
                .totalProfessors(uniqueProfessors.size())
                .totalCourses(uniqueCourses.size())
                .totalRequiredDocuments(totalRequiredDocuments)
                .submittedDocuments(submittedDocuments)
                .missingDocuments(missingDocuments)
                .overdueDocuments(overdueDocuments)
                .build();
        
        // Build semester name
        String semesterName = semester.getAcademicYear().getYearCode() + " - " + 
                            semester.getType().toString();
        
        ProfessorSubmissionReport report = ProfessorSubmissionReport.builder()
                .semesterId(semesterId)
                .semesterName(semesterName)
                .departmentId(effectiveDepartmentId)
                .departmentName(department.getName())
                .generatedAt(LocalDateTime.now())
                .generatedBy(currentUser.getFirstName() + " " + currentUser.getLastName())
                .rows(rows)
                .statistics(statistics)
                .build();
        
        log.info("Generated report with {} rows and {} total required documents", 
                rows.size(), totalRequiredDocuments);
        
        return report;
    }
    
    /**
     * Get the effective department ID based on user role.
     * For HOD users, always returns their own department ID.
     * For Dean/Admin users, returns the provided departmentId.
     * 
     * @param requestedDepartmentId The requested department ID
     * @param currentUser The current authenticated user
     * @return The effective department ID to use for filtering
     */
    private Long getEffectiveDepartmentId(Long requestedDepartmentId, User currentUser) {
        switch (currentUser.getRole()) {
            case ROLE_ADMIN:
            case ROLE_DEANSHIP:
                // Admin and Dean can access any department
                return requestedDepartmentId;
                
            case ROLE_HOD:
            case ROLE_PROFESSOR:
                // HOD and Professor are restricted to their own department
                if (currentUser.getDepartment() == null) {
                    throw new UnauthorizedOperationException("User has no department assigned");
                }
                Long userDepartmentId = currentUser.getDepartment().getId();
                
                // If a different department was requested, log a warning and use user's department
                if (requestedDepartmentId != null && !requestedDepartmentId.equals(userDepartmentId)) {
                    log.warn("{} user {} attempted to access department {} but is restricted to department {}", 
                            currentUser.getRole(), currentUser.getEmail(), requestedDepartmentId, userDepartmentId);
                }
                return userDepartmentId;
                
            default:
                throw new UnauthorizedOperationException("Invalid user role");
        }
    }

    @Override
    public ProfessorSubmissionReport filterReport(ProfessorSubmissionReport report, ReportFilter filter) {
        log.info("Filtering report with filter: {}", filter);
        
        if (filter == null) {
            return report;
        }
        
        List<ProfessorSubmissionRow> filteredRows = report.getRows().stream()
                .filter(row -> matchesFilter(row, filter))
                .collect(Collectors.toList());
        
        // Recalculate statistics for filtered rows
        SubmissionStatistics filteredStats = calculateStatistics(filteredRows, filter.getDocumentType());
        
        return ProfessorSubmissionReport.builder()
                .semesterId(report.getSemesterId())
                .semesterName(report.getSemesterName())
                .departmentId(report.getDepartmentId())
                .departmentName(report.getDepartmentName())
                .generatedAt(report.getGeneratedAt())
                .generatedBy(report.getGeneratedBy())
                .rows(filteredRows)
                .statistics(filteredStats)
                .build();
    }
    
    @Override
    public byte[] exportReportToPdf(ProfessorSubmissionReport report) {
        log.info("Exporting report to PDF for semester {} and department {}", 
                report.getSemesterId(), report.getDepartmentId());
        
        return pdfReportService.generateProfessorSubmissionReportPdf(report);
    }
    
    @Override
    @Transactional(readOnly = true)
    public SystemWideReport generateSystemWideReport(Long semesterId) {
        User currentUser = getCurrentUser();
        return generateSystemWideReportWithRoleFilter(semesterId, currentUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    public SystemWideReport generateSystemWideReportWithRoleFilter(Long semesterId, User currentUser) {
        return generateSystemWideReportWithRoleFilter(semesterId, currentUser, null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public SystemWideReport generateSystemWideReportWithRoleFilter(Long semesterId, User currentUser, Long departmentId) {
        log.info("Generating system-wide report for semester {} with role-based filtering, departmentId: {}", 
                semesterId, departmentId);
        
        // Validate semester exists
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found with id: " + semesterId));
        
        // Get departments based on user role and optional department filter
        List<Department> departments = getDepartmentsForUser(currentUser, departmentId);
        
        // Generate summary for each department
        List<DepartmentReportSummary> departmentSummaries = new ArrayList<>();
        int totalProfessors = 0;
        int totalCourses = 0;
        int totalRequiredDocuments = 0;
        int totalSubmittedDocuments = 0;
        int totalMissingDocuments = 0;
        int totalOverdueDocuments = 0;
        
        // Fetch all course assignments once using optimized query with eager loading
        // This prevents N+1 query issues by loading all related entities in a single query
        List<CourseAssignment> allCourseAssignments = courseAssignmentRepository.findBySemesterIdWithEagerLoading(semesterId);
        
        for (Department department : departments) {
            // Filter course assignments for this department from the pre-fetched list
            List<CourseAssignment> courseAssignments = allCourseAssignments.stream()
                    .filter(ca -> ca.getProfessor().getDepartment() != null && 
                                 ca.getProfessor().getDepartment().getId().equals(department.getId()))
                    .collect(Collectors.toList());
            
            if (courseAssignments.isEmpty()) {
                continue; // Skip departments with no assignments
            }
            
            Set<Long> uniqueProfessors = new HashSet<>();
            Set<Long> uniqueCourses = new HashSet<>();
            int deptRequiredDocuments = 0;
            int deptSubmittedDocuments = 0;
            int deptMissingDocuments = 0;
            int deptOverdueDocuments = 0;
            
            for (CourseAssignment assignment : courseAssignments) {
                uniqueProfessors.add(assignment.getProfessor().getId());
                uniqueCourses.add(assignment.getCourse().getId());
                
                // Get required document types for this course
                List<RequiredDocumentType> requiredDocs = requiredDocumentTypeRepository
                        .findByCourseIdAndSemesterId(assignment.getCourse().getId(), semesterId);
                
                if (requiredDocs.isEmpty()) {
                    requiredDocs = requiredDocumentTypeRepository.findByCourseId(assignment.getCourse().getId())
                            .stream()
                            .filter(rdt -> rdt.getSemester() == null)
                            .collect(Collectors.toList());
                }
                
                for (RequiredDocumentType requiredDoc : requiredDocs) {
                    deptRequiredDocuments++;
                    
                    Optional<DocumentSubmission> submission = documentSubmissionRepository
                            .findByCourseAssignmentIdAndDocumentType(assignment.getId(), requiredDoc.getDocumentType());
                    
                    if (submission.isPresent() && submission.get().getStatus() == SubmissionStatus.UPLOADED) {
                        deptSubmittedDocuments++;
                    } else {
                        deptMissingDocuments++;
                        
                        if (requiredDoc.getDeadline() != null && 
                            requiredDoc.getDeadline().isBefore(LocalDateTime.now())) {
                            deptOverdueDocuments++;
                        }
                    }
                }
            }
            
            SubmissionStatistics deptStats = SubmissionStatistics.builder()
                    .totalProfessors(uniqueProfessors.size())
                    .totalCourses(uniqueCourses.size())
                    .totalRequiredDocuments(deptRequiredDocuments)
                    .submittedDocuments(deptSubmittedDocuments)
                    .missingDocuments(deptMissingDocuments)
                    .overdueDocuments(deptOverdueDocuments)
                    .build();
            
            DepartmentReportSummary summary = DepartmentReportSummary.builder()
                    .departmentId(department.getId())
                    .departmentName(department.getName())
                    .statistics(deptStats)
                    .build();
            
            departmentSummaries.add(summary);
            
            // Aggregate totals
            totalProfessors += uniqueProfessors.size();
            totalCourses += uniqueCourses.size();
            totalRequiredDocuments += deptRequiredDocuments;
            totalSubmittedDocuments += deptSubmittedDocuments;
            totalMissingDocuments += deptMissingDocuments;
            totalOverdueDocuments += deptOverdueDocuments;
        }
        
        // Build overall statistics
        SubmissionStatistics overallStats = SubmissionStatistics.builder()
                .totalProfessors(totalProfessors)
                .totalCourses(totalCourses)
                .totalRequiredDocuments(totalRequiredDocuments)
                .submittedDocuments(totalSubmittedDocuments)
                .missingDocuments(totalMissingDocuments)
                .overdueDocuments(totalOverdueDocuments)
                .build();
        
        String semesterName = semester.getAcademicYear().getYearCode() + " - " + 
                            semester.getType().toString();
        
        SystemWideReport report = SystemWideReport.builder()
                .semesterId(semesterId)
                .semesterName(semesterName)
                .generatedAt(LocalDateTime.now())
                .generatedBy(currentUser.getFirstName() + " " + currentUser.getLastName())
                .departmentSummaries(departmentSummaries)
                .overallStatistics(overallStats)
                .build();
        
        log.info("Generated system-wide report with {} departments", departmentSummaries.size());
        
        return report;
    }
    
    /**
     * Check if a row matches the filter criteria
     */
    private boolean matchesFilter(ProfessorSubmissionRow row, ReportFilter filter) {
        // Filter by course code
        if (filter.getCourseCode() != null && !filter.getCourseCode().isEmpty()) {
            if (!row.getCourseCode().equalsIgnoreCase(filter.getCourseCode())) {
                return false;
            }
        }
        
        // Filter by document type
        if (filter.getDocumentType() != null) {
            if (!row.getDocumentStatuses().containsKey(filter.getDocumentType())) {
                return false;
            }
        }
        
        // Filter by status
        if (filter.getStatus() != null) {
            if (filter.getDocumentType() != null) {
                // Check specific document type status
                DocumentStatusInfo statusInfo = row.getDocumentStatuses().get(filter.getDocumentType());
                if (statusInfo == null || statusInfo.getStatus() != filter.getStatus()) {
                    return false;
                }
            } else {
                // Check if any document has the specified status
                boolean hasStatus = row.getDocumentStatuses().values().stream()
                        .anyMatch(statusInfo -> statusInfo.getStatus() == filter.getStatus());
                if (!hasStatus) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Calculate statistics for filtered rows
     */
    private SubmissionStatistics calculateStatistics(List<ProfessorSubmissionRow> rows, 
                                                     DocumentTypeEnum specificDocType) {
        Set<Long> uniqueProfessors = new HashSet<>();
        Set<String> uniqueCourses = new HashSet<>();
        int totalRequiredDocuments = 0;
        int submittedDocuments = 0;
        int missingDocuments = 0;
        int overdueDocuments = 0;
        
        for (ProfessorSubmissionRow row : rows) {
            uniqueProfessors.add(row.getProfessorId());
            uniqueCourses.add(row.getCourseCode());
            
            Map<DocumentTypeEnum, DocumentStatusInfo> statuses = row.getDocumentStatuses();
            
            // If filtering by specific document type, only count that type
            if (specificDocType != null) {
                if (statuses.containsKey(specificDocType)) {
                    totalRequiredDocuments++;
                    DocumentStatusInfo statusInfo = statuses.get(specificDocType);
                    
                    if (statusInfo.getStatus() == SubmissionStatus.UPLOADED) {
                        submittedDocuments++;
                    } else {
                        missingDocuments++;
                        if (statusInfo.getStatus() == SubmissionStatus.OVERDUE) {
                            overdueDocuments++;
                        }
                    }
                }
            } else {
                // Count all document types
                for (Map.Entry<DocumentTypeEnum, DocumentStatusInfo> entry : statuses.entrySet()) {
                    totalRequiredDocuments++;
                    
                    if (entry.getValue().getStatus() == SubmissionStatus.UPLOADED) {
                        submittedDocuments++;
                    } else {
                        missingDocuments++;
                        if (entry.getValue().getStatus() == SubmissionStatus.OVERDUE) {
                            overdueDocuments++;
                        }
                    }
                }
            }
        }
        
        return SubmissionStatistics.builder()
                .totalProfessors(uniqueProfessors.size())
                .totalCourses(uniqueCourses.size())
                .totalRequiredDocuments(totalRequiredDocuments)
                .submittedDocuments(submittedDocuments)
                .missingDocuments(missingDocuments)
                .overdueDocuments(overdueDocuments)
                .build();
    }
    
    /**
     * Get departments accessible to the current user based on their role and optional department filter.
     * Admin and Dean: if departmentId is provided, returns only that department; otherwise all departments.
     * HOD and Professor: always returns their own department (departmentId is ignored).
     */
    private List<Department> getDepartmentsForUser(User currentUser, Long departmentId) {
        switch (currentUser.getRole()) {
            case ROLE_ADMIN:
            case ROLE_DEANSHIP:
                // Admin and Dean can see all departments, or filter by specific department
                if (departmentId != null) {
                    return departmentRepository.findById(departmentId)
                            .map(List::of)
                            .orElse(List.of());
                }
                return departmentRepository.findAll();
                
            case ROLE_HOD:
            case ROLE_PROFESSOR:
                // HOD and Professor can only see their own department (departmentId is ignored)
                if (currentUser.getDepartment() == null) {
                    log.warn("User {} has no department assigned", currentUser.getEmail());
                    return List.of();
                }
                return List.of(currentUser.getDepartment());
                
            default:
                log.warn("Unknown role: {}", currentUser.getRole());
                return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public ReportFilterOptions getFilterOptions(User currentUser) {
        log.info("Getting filter options for user {} with role {}", currentUser.getEmail(), currentUser.getRole());
        
        boolean canFilterByDepartment = currentUser.getRole() == Role.ROLE_ADMIN || 
                                        currentUser.getRole() == Role.ROLE_DEANSHIP;
        
        // Get departments based on role
        List<ReportFilterOptions.DepartmentOption> departmentOptions;
        Long userDepartmentId = null;
        String userDepartmentName = null;
        
        if (canFilterByDepartment) {
            // Admin/Dean can see all departments
            departmentOptions = departmentRepository.findAll().stream()
                    .map(dept -> ReportFilterOptions.DepartmentOption.builder()
                            .id(dept.getId())
                            .name(dept.getName())
                            .shortcut(dept.getShortcut())
                            .build())
                    .collect(Collectors.toList());
        } else {
            // HOD/Professor can only see their own department
            departmentOptions = List.of();
            if (currentUser.getDepartment() != null) {
                userDepartmentId = currentUser.getDepartment().getId();
                userDepartmentName = currentUser.getDepartment().getName();
            }
        }
        
        // Get courses based on role
        List<Course> accessibleCourses;
        if (canFilterByDepartment) {
            accessibleCourses = courseAssignmentRepository.findAll().stream()
                    .map(CourseAssignment::getCourse)
                    .distinct()
                    .collect(Collectors.toList());
        } else if (currentUser.getDepartment() != null) {
            accessibleCourses = courseAssignmentRepository.findAll().stream()
                    .filter(ca -> ca.getProfessor().getDepartment() != null &&
                                 ca.getProfessor().getDepartment().getId().equals(currentUser.getDepartment().getId()))
                    .map(CourseAssignment::getCourse)
                    .distinct()
                    .collect(Collectors.toList());
        } else {
            accessibleCourses = List.of();
        }
        
        List<ReportFilterOptions.CourseOption> courseOptions = accessibleCourses.stream()
                .map(course -> ReportFilterOptions.CourseOption.builder()
                        .id(course.getId())
                        .courseCode(course.getCourseCode())
                        .courseName(course.getCourseName())
                        .build())
                .collect(Collectors.toList());
        
        // Get professors based on role
        List<User> accessibleProfessors;
        if (canFilterByDepartment) {
            accessibleProfessors = userRepository.findByRole(Role.ROLE_PROFESSOR);
        } else if (currentUser.getDepartment() != null) {
            accessibleProfessors = userRepository.findByRole(Role.ROLE_PROFESSOR).stream()
                    .filter(prof -> prof.getDepartment() != null &&
                                   prof.getDepartment().getId().equals(currentUser.getDepartment().getId()))
                    .collect(Collectors.toList());
        } else {
            accessibleProfessors = List.of();
        }
        
        List<ReportFilterOptions.ProfessorOption> professorOptions = accessibleProfessors.stream()
                .map(prof -> ReportFilterOptions.ProfessorOption.builder()
                        .id(prof.getId())
                        .name(prof.getFirstName() + " " + prof.getLastName())
                        .email(prof.getEmail())
                        .build())
                .collect(Collectors.toList());
        
        // Get academic years
        List<ReportFilterOptions.AcademicYearOption> academicYearOptions = semesterRepository.findAll().stream()
                .map(Semester::getAcademicYear)
                .distinct()
                .map(ay -> ReportFilterOptions.AcademicYearOption.builder()
                        .id(ay.getId())
                        .yearCode(ay.getYearCode())
                        .build())
                .collect(Collectors.toList());
        
        // Get semesters
        List<ReportFilterOptions.SemesterOption> semesterOptions = semesterRepository.findAll().stream()
                .map(sem -> ReportFilterOptions.SemesterOption.builder()
                        .id(sem.getId())
                        .name(sem.getAcademicYear().getYearCode() + " - " + sem.getType().toString())
                        .academicYearId(sem.getAcademicYear().getId())
                        .build())
                .collect(Collectors.toList());
        
        // Get document types and submission statuses
        List<DocumentTypeEnum> documentTypes = Arrays.asList(DocumentTypeEnum.values());
        List<SubmissionStatus> submissionStatuses = Arrays.asList(SubmissionStatus.values());
        
        return ReportFilterOptions.builder()
                .departments(departmentOptions)
                .courses(courseOptions)
                .professors(professorOptions)
                .academicYears(academicYearOptions)
                .semesters(semesterOptions)
                .documentTypes(documentTypes)
                .submissionStatuses(submissionStatuses)
                .canFilterByDepartment(canFilterByDepartment)
                .userDepartmentId(userDepartmentId)
                .userDepartmentName(userDepartmentName)
                .build();
    }
    
    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Current user not found with email: " + email));
    }
}
