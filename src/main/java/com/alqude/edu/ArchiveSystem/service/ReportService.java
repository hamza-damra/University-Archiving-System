package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.report.DepartmentSubmissionReport;
import com.alqude.edu.ArchiveSystem.dto.report.ProfessorSubmissionSummary;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestResponse;
import com.alqude.edu.ArchiveSystem.dto.user.UserResponse;
import com.alqude.edu.ArchiveSystem.entity.SubmittedDocument;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Report service for generating reports from legacy and new systems.
 * 
 * NOTE: This service uses legacy services (DocumentRequestService, FileUploadService)
 * to generate reports from historical data during the transition period.
 * Deprecation warnings are suppressed as this is intentional for backward compatibility.
 * 
 * @deprecated This service will be replaced by new reporting services in the semester-based system
 */
@Deprecated(since = "2.0", forRemoval = false)
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    
    private final DocumentRequestService documentRequestService;
    private final FileUploadService fileUploadService;
    private final UserService userService;
    private final UserRepository userRepository;
    
    public Map<String, Object> generateDepartmentReport(Long departmentId) {
        log.info("Generating report for department id: {}", departmentId);
        
        List<DocumentRequestResponse> allRequests = documentRequestService.getDocumentRequestsByDepartment(departmentId);
        List<SubmittedDocument> submittedDocuments = fileUploadService.getSubmittedDocumentsByDepartment(departmentId);
        
        Map<String, Object> report = new HashMap<>();
        
        // Basic statistics
        report.put("totalRequests", allRequests.size());
        report.put("submittedDocuments", submittedDocuments.size());
        report.put("pendingRequests", allRequests.size() - submittedDocuments.size());
        
        // Submission statistics
        long onTimeSubmissions = submittedDocuments.stream()
                .mapToLong(doc -> doc.getIsLateSubmission() ? 0 : 1)
                .sum();
        long lateSubmissions = submittedDocuments.size() - onTimeSubmissions;
        
        report.put("onTimeSubmissions", onTimeSubmissions);
        report.put("lateSubmissions", lateSubmissions);
        
        // Completion rate
        double completionRate = allRequests.isEmpty() ? 0.0 : 
                (double) submittedDocuments.size() / allRequests.size() * 100;
        report.put("completionRate", Math.round(completionRate * 100.0) / 100.0);
        
        // On-time rate
        double onTimeRate = submittedDocuments.isEmpty() ? 0.0 : 
                (double) onTimeSubmissions / submittedDocuments.size() * 100;
        report.put("onTimeRate", Math.round(onTimeRate * 100.0) / 100.0);
        
        // Detailed requests
        report.put("requests", allRequests);
        
        log.info("Report generated for department id: {} with {} requests", departmentId, allRequests.size());
        return report;
    }
    
    public Map<String, Object> generateProfessorReport(Long professorId) {
        log.info("Generating report for professor id: {}", professorId);
        
        List<DocumentRequestResponse> professorRequests = documentRequestService.getDocumentRequestsByProfessor(professorId);
        List<SubmittedDocument> professorSubmissions = fileUploadService.getSubmittedDocumentsByProfessor(professorId);
        
        Map<String, Object> report = new HashMap<>();
        
        // Basic statistics
        report.put("totalRequests", professorRequests.size());
        report.put("submittedDocuments", professorSubmissions.size());
        report.put("pendingRequests", professorRequests.size() - professorSubmissions.size());
        
        // Submission statistics
        long onTimeSubmissions = professorSubmissions.stream()
                .mapToLong(doc -> doc.getIsLateSubmission() ? 0 : 1)
                .sum();
        long lateSubmissions = professorSubmissions.size() - onTimeSubmissions;
        
        report.put("onTimeSubmissions", onTimeSubmissions);
        report.put("lateSubmissions", lateSubmissions);
        
        // Completion rate
        double completionRate = professorRequests.isEmpty() ? 0.0 : 
                (double) professorSubmissions.size() / professorRequests.size() * 100;
        report.put("completionRate", Math.round(completionRate * 100.0) / 100.0);
        
        // On-time rate
        double onTimeRate = professorSubmissions.isEmpty() ? 0.0 : 
                (double) onTimeSubmissions / professorSubmissions.size() * 100;
        report.put("onTimeRate", Math.round(onTimeRate * 100.0) / 100.0);
        
        // Detailed requests
        report.put("requests", professorRequests);
        report.put("submissions", professorSubmissions);
        
        log.info("Report generated for professor id: {} with {} requests", professorId, professorRequests.size());
        return report;
    }
    
    public Map<String, Object> generateOverallSystemReport() {
        log.info("Generating overall system report");
        
        List<DocumentRequestResponse> overdueRequests = documentRequestService.getOverdueRequests();
        List<DocumentRequestResponse> upcomingDeadlines = documentRequestService.getRequestsWithUpcomingDeadline(24);
        
        Map<String, Object> report = new HashMap<>();
        
        report.put("overdueRequests", overdueRequests.size());
        report.put("upcomingDeadlines", upcomingDeadlines.size());
        report.put("overdueRequestsList", overdueRequests);
        report.put("upcomingDeadlinesList", upcomingDeadlines);
        
        log.info("Overall system report generated with {} overdue and {} upcoming deadlines", 
                overdueRequests.size(), upcomingDeadlines.size());
        return report;
    }
    
    /**
     * Generate a comprehensive submission report for the HOD's department
     * showing which professors have submitted required documents
     */
    public DepartmentSubmissionReport generateDepartmentSubmissionReport() {
        log.info("Generating department submission report for current HOD");
        
        User currentUser = getCurrentUser();
        if (currentUser.getDepartment() == null) {
            throw new IllegalStateException("HOD must be assigned to a department");
        }
        
        Long departmentId = currentUser.getDepartment().getId();
        String departmentName = currentUser.getDepartment().getName();
        
        // Get all professors in the department
        List<UserResponse> professors = userService.getProfessorsByDepartment(departmentId);
        
        // Generate summary for each professor
        List<ProfessorSubmissionSummary> professorSummaries = professors.stream()
                .map(prof -> generateProfessorSummary(prof, departmentId))
                .sorted(Comparator.comparing(ProfessorSubmissionSummary::getCompletionRate).reversed())
                .collect(Collectors.toList());
        
        // Calculate overall statistics
        int totalRequests = professorSummaries.stream()
                .mapToInt(ProfessorSubmissionSummary::getTotalRequests)
                .sum();
        int totalSubmitted = professorSummaries.stream()
                .mapToInt(ProfessorSubmissionSummary::getSubmittedRequests)
                .sum();
        int totalPending = professorSummaries.stream()
                .mapToInt(ProfessorSubmissionSummary::getPendingRequests)
                .sum();
        int totalOverdue = professorSummaries.stream()
                .mapToInt(ProfessorSubmissionSummary::getOverdueRequests)
                .sum();
        
        double overallCompletionRate = totalRequests > 0 ? 
                (double) totalSubmitted / totalRequests * 100 : 0.0;
        
        int totalOnTime = professorSummaries.stream()
                .mapToInt(ProfessorSubmissionSummary::getSubmittedOnTime)
                .sum();
        double overallOnTimeRate = totalSubmitted > 0 ? 
                (double) totalOnTime / totalSubmitted * 100 : 0.0;
        
        return DepartmentSubmissionReport.builder()
                .departmentName(departmentName)
                .generatedAt(LocalDateTime.now())
                .generatedBy(currentUser.getFirstName() + " " + currentUser.getLastName())
                .totalProfessors(professors.size())
                .totalRequests(totalRequests)
                .totalSubmitted(totalSubmitted)
                .totalPending(totalPending)
                .totalOverdue(totalOverdue)
                .overallCompletionRate(Math.round(overallCompletionRate * 100.0) / 100.0)
                .overallOnTimeRate(Math.round(overallOnTimeRate * 100.0) / 100.0)
                .professorSummaries(professorSummaries)
                .build();
    }
    
    /**
     * Generate submission summary for a specific professor
     */
    private ProfessorSubmissionSummary generateProfessorSummary(UserResponse professor, Long departmentId) {
        Long professorId = professor.getId();
        
        // Get all requests for this professor
        List<DocumentRequestResponse> allRequests = documentRequestService.getDocumentRequestsByProfessor(professorId);
        
        // Count submitted and pending
        long submitted = allRequests.stream()
                .filter(req -> req.getSubmittedDocument() != null)
                .count();
        
        long pending = allRequests.size() - submitted;
        
        // Count overdue (pending requests past deadline)
        LocalDateTime now = LocalDateTime.now();
        long overdue = allRequests.stream()
                .filter(req -> req.getSubmittedDocument() == null && req.getDeadline().isBefore(now))
                .count();
        
        // Count on-time vs late submissions
        long onTime = allRequests.stream()
                .filter(req -> req.getSubmittedDocument() != null && 
                        (req.getSubmittedDocument().getSubmittedLate() == null || 
                         !req.getSubmittedDocument().getSubmittedLate()))
                .count();
        
        long late = submitted - onTime;
        
        // Calculate rates
        double completionRate = allRequests.isEmpty() ? 0.0 : 
                (double) submitted / allRequests.size() * 100;
        double onTimeRate = submitted > 0 ? 
                (double) onTime / submitted * 100 : 0.0;
        
        // Find last submission date
        LocalDateTime lastSubmission = allRequests.stream()
                .filter(req -> req.getSubmittedDocument() != null)
                .map(req -> req.getSubmittedDocument().getSubmittedAt())
                .max(LocalDateTime::compareTo)
                .orElse(null);
        
        // Find earliest pending deadline
        LocalDateTime earliestDeadline = allRequests.stream()
                .filter(req -> req.getSubmittedDocument() == null)
                .map(DocumentRequestResponse::getDeadline)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        
        return ProfessorSubmissionSummary.builder()
                .professorId(professorId)
                .professorName(professor.getFirstName() + " " + professor.getLastName())
                .professorEmail(professor.getEmail())
                .departmentName(professor.getDepartmentName() != null ? professor.getDepartmentName() : "N/A")
                .totalRequests(allRequests.size())
                .submittedRequests((int) submitted)
                .pendingRequests((int) pending)
                .overdueRequests((int) overdue)
                .submittedOnTime((int) onTime)
                .submittedLate((int) late)
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .onTimeRate(Math.round(onTimeRate * 100.0) / 100.0)
                .lastSubmissionDate(lastSubmission)
                .earliestPendingDeadline(earliestDeadline)
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
