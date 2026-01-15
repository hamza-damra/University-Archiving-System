package com.alquds.edu.ArchiveSystem.service.academic;

import com.alquds.edu.ArchiveSystem.entity.user.User;

import com.alquds.edu.ArchiveSystem.dto.report.ProfessorSubmissionReport;
import com.alquds.edu.ArchiveSystem.dto.report.ReportFilter;
import com.alquds.edu.ArchiveSystem.dto.report.ReportFilterOptions;
import com.alquds.edu.ArchiveSystem.dto.report.SystemWideReport;

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
     * Generate system-wide report with role-based filtering and optional department filter.
     * For HOD users, returns report limited to their department only (departmentId is ignored).
     * For Dean/Admin users, if departmentId is provided, returns report for that department only.
     * If departmentId is null, returns full system-wide report.
     * 
     * @param semesterId The semester to report on
     * @param currentUser The current authenticated user
     * @param departmentId Optional department ID to filter by (for Admin/Dean only)
     * @return System-wide report with department summaries (filtered by role and department)
     */
    SystemWideReport generateSystemWideReportWithRoleFilter(Long semesterId, User currentUser, Long departmentId);
    
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
