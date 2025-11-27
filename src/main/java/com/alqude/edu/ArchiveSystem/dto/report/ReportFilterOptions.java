package com.alqude.edu.ArchiveSystem.dto.report;

import com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum;
import com.alqude.edu.ArchiveSystem.entity.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Available filter options for report generation based on user role.
 * For Dean/Admin: includes all departments.
 * For HOD: excludes department list (restricted to own department).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterOptions {
    
    /**
     * List of available departments for filtering.
     * Empty for HOD users (they can only see their own department).
     */
    private List<DepartmentOption> departments;
    
    /**
     * List of available courses for filtering.
     */
    private List<CourseOption> courses;
    
    /**
     * List of available professors for filtering.
     */
    private List<ProfessorOption> professors;
    
    /**
     * List of available academic years for filtering.
     */
    private List<AcademicYearOption> academicYears;
    
    /**
     * List of available semesters for filtering.
     */
    private List<SemesterOption> semesters;
    
    /**
     * List of available document types for filtering.
     */
    private List<DocumentTypeEnum> documentTypes;
    
    /**
     * List of available submission statuses for filtering.
     */
    private List<SubmissionStatus> submissionStatuses;
    
    /**
     * Whether the user can filter by department.
     * False for HOD users (restricted to own department).
     */
    private boolean canFilterByDepartment;
    
    /**
     * The user's department ID (for HOD users).
     * Null for Dean/Admin users.
     */
    private Long userDepartmentId;
    
    /**
     * The user's department name (for HOD users).
     * Null for Dean/Admin users.
     */
    private String userDepartmentName;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentOption {
        private Long id;
        private String name;
        private String shortcut;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseOption {
        private Long id;
        private String courseCode;
        private String courseName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfessorOption {
        private Long id;
        private String name;
        private String email;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcademicYearOption {
        private Long id;
        private String yearCode;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterOption {
        private Long id;
        private String name;
        private Long academicYearId;
    }
}
