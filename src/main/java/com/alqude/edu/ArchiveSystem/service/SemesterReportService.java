package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.report.ProfessorSubmissionReport;
import com.alqude.edu.ArchiveSystem.dto.report.ReportFilter;
import com.alqude.edu.ArchiveSystem.dto.report.SystemWideReport;

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
}
