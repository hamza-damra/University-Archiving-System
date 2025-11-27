package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.report.ProfessorSubmissionReport;
import com.alqude.edu.ArchiveSystem.dto.report.ReportFilter;
import com.alqude.edu.ArchiveSystem.dto.report.ReportFilterOptions;
import com.alqude.edu.ArchiveSystem.dto.report.SystemWideReport;
import com.alqude.edu.ArchiveSystem.entity.User;

/**
 * Service for generating semester-based submission reports
 */
public interface SemesterReportService {
    
    /**
     * Generate professor submission report for HOD
     * Shows submission status for all professors in the department for a semester
     * 
     * @param semesterId The semester to report on
     * @param departmentId The department to report on
     * @return Professor submission report with rows and statistics
     */
    ProfessorSubmissionReport generateProfessorSubmissionReport(Long semesterId, Long departmentId);
    
    /**
     * Generate professor submission report with role-based filtering.
     * For HOD users, automatically applies department filter.
     * For Dean/Admin users, uses the provided departmentId (or all departments if null).
     * 
     * @param semesterId The semester to report on
     * @param departmentId The department to report on (optional for Dean/Admin)
     * @param currentUser The current authenticated user
     * @return Professor submission report with rows and statistics
     */
    ProfessorSubmissionReport generateProfessorSubmissionReportWithRoleFilter(Long semesterId, Long departmentId, User currentUser);
    
    /**
     * Filter an existing report by course, document type, or status
     * 
     * @param report The report to filter
     * @param filter Filter criteria
     * @return Filtered report
     */
    ProfessorSubmissionReport filterReport(ProfessorSubmissionReport report, ReportFilter filter);
    
    /**
     * Export report to PDF format
     * 
     * @param report The report to export
     * @return PDF bytes
     */
    byte[] exportReportToPdf(ProfessorSubmissionReport report);
    
    /**
     * Generate system-wide report for Deanship
     * Aggregates data across all departments for a semester
     * 
     * @param semesterId The semester to report on
     * @return System-wide report with department summaries
     */
    SystemWideReport generateSystemWideReport(Long semesterId);
    
    /**
     * Generate system-wide report with role-based filtering.
     * For HOD users, returns report limited to their department only.
     * For Dean/Admin users, returns full system-wide report.
     * 
     * @param semesterId The semester to report on
     * @param currentUser The current authenticated user
     * @return System-wide report with department summaries (filtered by role)
     */
    SystemWideReport generateSystemWideReportWithRoleFilter(Long semesterId, User currentUser);
    
    /**
     * Get available filter options based on user role.
     * For Dean/Admin: includes all departments.
     * For HOD: excludes department filter (restricted to own department).
     * 
     * @param currentUser The current authenticated user
     * @return Available filter options for the user
     */
    ReportFilterOptions getFilterOptions(User currentUser);
}
