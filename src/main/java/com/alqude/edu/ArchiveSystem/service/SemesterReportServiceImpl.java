package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.report.*;
import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.repository.*;
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
public class SemesterReportServiceImpl implements SemesterReportService {
    
    private final SemesterRepository semesterRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;
    private final UserRepository userRepository;
    private final PdfReportService pdfReportService;
    
    @Override
    @Transactional(readOnly = true)
    public ProfessorSubmissionReport generateProfessorSubmissionReport(Long semesterId, Long departmentId) {
        log.info("Generating professor submission report for semester {} and department {}", semesterId, departmentId);
        
        // Validate semester exists
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found with id: " + semesterId));
        
        // Validate department exists
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + departmentId));
        
        // Get current user for report metadata
        User currentUser = getCurrentUser();
        
        // Fetch all course assignments for this semester and department
        List<CourseAssignment> courseAssignments = courseAssignmentRepository.findBySemesterId(semesterId)
                .stream()
                .filter(ca -> ca.getProfessor().getDepartment() != null && 
                             ca.getProfessor().getDepartment().getId().equals(departmentId))
                .collect(Collectors.toList());
        
        log.debug("Found {} course assignments for semester {} and department {}", 
                courseAssignments.size(), semesterId, departmentId);
        
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
            Map<DocumentTypeEnum, SubmissionStatus> documentStatuses = new HashMap<>();
            
            for (RequiredDocumentType requiredDoc : requiredDocs) {
                totalRequiredDocuments++;
                
                // Find submission for this document type
                Optional<DocumentSubmission> submission = documentSubmissionRepository
                        .findByCourseAssignmentIdAndDocumentType(assignment.getId(), requiredDoc.getDocumentType());
                
                SubmissionStatus status;
                if (submission.isPresent()) {
                    status = submission.get().getStatus();
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
                
                documentStatuses.put(requiredDoc.getDocumentType(), status);
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
                .departmentId(departmentId)
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
        log.info("Generating system-wide report for semester {}", semesterId);
        
        // Validate semester exists
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found with id: " + semesterId));
        
        // Get current user for report metadata
        User currentUser = getCurrentUser();
        
        // Get all departments
        List<Department> departments = departmentRepository.findAll();
        
        // Generate summary for each department
        List<DepartmentReportSummary> departmentSummaries = new ArrayList<>();
        int totalProfessors = 0;
        int totalCourses = 0;
        int totalRequiredDocuments = 0;
        int totalSubmittedDocuments = 0;
        int totalMissingDocuments = 0;
        int totalOverdueDocuments = 0;
        
        for (Department department : departments) {
            // Get course assignments for this department
            List<CourseAssignment> courseAssignments = courseAssignmentRepository.findBySemesterId(semesterId)
                    .stream()
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
                SubmissionStatus status = row.getDocumentStatuses().get(filter.getDocumentType());
                if (status != filter.getStatus()) {
                    return false;
                }
            } else {
                // Check if any document has the specified status
                boolean hasStatus = row.getDocumentStatuses().values().stream()
                        .anyMatch(status -> status == filter.getStatus());
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
            
            Map<DocumentTypeEnum, SubmissionStatus> statuses = row.getDocumentStatuses();
            
            // If filtering by specific document type, only count that type
            if (specificDocType != null) {
                if (statuses.containsKey(specificDocType)) {
                    totalRequiredDocuments++;
                    SubmissionStatus status = statuses.get(specificDocType);
                    
                    if (status == SubmissionStatus.UPLOADED) {
                        submittedDocuments++;
                    } else {
                        missingDocuments++;
                        if (status == SubmissionStatus.OVERDUE) {
                            overdueDocuments++;
                        }
                    }
                }
            } else {
                // Count all document types
                for (Map.Entry<DocumentTypeEnum, SubmissionStatus> entry : statuses.entrySet()) {
                    totalRequiredDocuments++;
                    
                    if (entry.getValue() == SubmissionStatus.UPLOADED) {
                        submittedDocuments++;
                    } else {
                        missingDocuments++;
                        if (entry.getValue() == SubmissionStatus.OVERDUE) {
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
