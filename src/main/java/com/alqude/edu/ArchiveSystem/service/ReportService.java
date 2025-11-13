package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestResponse;
import com.alqude.edu.ArchiveSystem.entity.SubmittedDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    
    private final DocumentRequestService documentRequestService;
    private final FileUploadService fileUploadService;
    
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
}
