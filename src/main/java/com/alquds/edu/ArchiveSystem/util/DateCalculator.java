package com.alquds.edu.ArchiveSystem.util;

import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.academic.SemesterType;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for calculating semester dates and document deadlines.
 * Provides methods to determine semester start/end dates and calculate realistic deadlines.
 */
public class DateCalculator {
    
    /**
     * Calculates the start date for a semester based on academic year and semester type.
     * 
     * @param startYear the starting year of the academic year (e.g., 2024 for 2024-2025)
     * @param semesterType the type of semester (FIRST, SECOND, SUMMER)
     * @return the start date of the semester
     */
    public static LocalDate calculateSemesterStartDate(int startYear, SemesterType semesterType) {
        switch (semesterType) {
            case FIRST:
                // Fall semester starts September 1
                return LocalDate.of(startYear, 9, 1);
            case SECOND:
                // Spring semester starts February 1 of the next year
                return LocalDate.of(startYear + 1, 2, 1);
            case SUMMER:
                // Summer semester starts July 1 of the next year
                return LocalDate.of(startYear + 1, 7, 1);
            default:
                throw new IllegalArgumentException("Unknown semester type: " + semesterType);
        }
    }
    
    /**
     * Calculates the end date for a semester based on academic year and semester type.
     * 
     * @param startYear the starting year of the academic year (e.g., 2024 for 2024-2025)
     * @param semesterType the type of semester (FIRST, SECOND, SUMMER)
     * @return the end date of the semester
     */
    public static LocalDate calculateSemesterEndDate(int startYear, SemesterType semesterType) {
        switch (semesterType) {
            case FIRST:
                // Fall semester ends January 15 of the next year
                return LocalDate.of(startYear + 1, 1, 15);
            case SECOND:
                // Spring semester ends June 15 of the next year
                return LocalDate.of(startYear + 1, 6, 15);
            case SUMMER:
                // Summer semester ends August 31 of the next year
                return LocalDate.of(startYear + 1, 8, 31);
            default:
                throw new IllegalArgumentException("Unknown semester type: " + semesterType);
        }
    }
    
    /**
     * Calculates a realistic deadline for a document type based on semester dates.
     * 
     * @param semesterStartDate the start date of the semester
     * @param semesterEndDate the end date of the semester
     * @param documentType the type of document
     * @return the deadline date for the document
     */
    public static LocalDate calculateDocumentDeadline(
            LocalDate semesterStartDate, 
            LocalDate semesterEndDate, 
            DocumentTypeEnum documentType) {
        
        long semesterDurationDays = ChronoUnit.DAYS.between(semesterStartDate, semesterEndDate);
        
        switch (documentType) {
            case SYLLABUS:
                // Syllabus due 2 weeks after semester start
                return semesterStartDate.plusWeeks(2);
                
            case EXAM:
                // Exams due at mid-semester (for midterm) or end (for final)
                // Default to mid-semester
                return semesterStartDate.plusDays(semesterDurationDays / 2);
                
            case ASSIGNMENT:
                // Assignments due throughout semester, default to 1/3 through
                return semesterStartDate.plusDays(semesterDurationDays / 3);
                
            case PROJECT_DOCS:
                // Project documents due near end of semester (2 weeks before end)
                return semesterEndDate.minusWeeks(2);
                
            case LECTURE_NOTES:
                // Lecture notes due weekly, default to 1 month into semester
                return semesterStartDate.plusMonths(1);
                
            case OTHER:
                // Other documents due mid-semester
                return semesterStartDate.plusDays(semesterDurationDays / 2);
                
            default:
                throw new IllegalArgumentException("Unknown document type: " + documentType);
        }
    }
    
    /**
     * Calculates a submission date based on deadline and whether it should be late.
     * 
     * @param deadline the deadline date
     * @param isLate true if the submission should be late
     * @return the submission date
     */
    public static LocalDate calculateSubmissionDate(LocalDate deadline, boolean isLate) {
        if (isLate) {
            // Late submission: 1-7 days after deadline
            int daysLate = 1 + (int) (Math.random() * 7);
            return deadline.plusDays(daysLate);
        } else {
            // On-time submission: 0-3 days before deadline
            int daysEarly = (int) (Math.random() * 4);
            return deadline.minusDays(daysEarly);
        }
    }
    
    /**
     * Calculates a notification date relative to a deadline.
     * 
     * @param deadline the deadline date
     * @param daysBefore number of days before the deadline
     * @return the notification date
     */
    public static LocalDate calculateNotificationDate(LocalDate deadline, int daysBefore) {
        return deadline.minusDays(daysBefore);
    }
    
    /**
     * Checks if a submission date is after the deadline (late submission).
     * 
     * @param submissionDate the submission date
     * @param deadline the deadline date
     * @return true if the submission is late, false otherwise
     */
    public static boolean isLateSubmission(LocalDate submissionDate, LocalDate deadline) {
        return submissionDate.isAfter(deadline);
    }
    
    /**
     * Gets the current semester type based on the current date.
     * 
     * @param currentDate the current date
     * @return the current semester type
     */
    public static SemesterType getCurrentSemesterType(LocalDate currentDate) {
        int month = currentDate.getMonthValue();
        
        if (month >= 9 || month <= 1) {
            return SemesterType.FIRST;
        } else if (month >= 2 && month <= 6) {
            return SemesterType.SECOND;
        } else {
            return SemesterType.SUMMER;
        }
    }
}
