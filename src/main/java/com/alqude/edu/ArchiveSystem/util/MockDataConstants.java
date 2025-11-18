package com.alqude.edu.ArchiveSystem.util;

import com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Constants used for mock data generation.
 * Contains configuration values, department information, course details, and file type mappings.
 */
public class MockDataConstants {
    
    // ==================== Quantity Configuration ====================
    
    public static final int ACADEMIC_YEARS_COUNT = 3;
    public static final int SEMESTERS_PER_YEAR = 3;
    public static final int DEPARTMENTS_COUNT = 5;
    public static final int COURSES_PER_DEPARTMENT = 3;
    public static final int HODS_COUNT = 5; // One per department
    public static final int PROFESSORS_PER_DEPARTMENT = 5;
    public static final int ASSIGNMENTS_PER_COURSE_PER_SEMESTER = 2;
    public static final int DOCUMENT_TYPES_PER_COURSE = 6;
    public static final int MIN_SUBMISSIONS_PER_PROFESSOR = 3;
    public static final int MAX_SUBMISSIONS_PER_PROFESSOR = 6;
    public static final int MIN_FILES_PER_SUBMISSION = 1;
    public static final int MAX_FILES_PER_SUBMISSION = 3;
    public static final int NOTIFICATIONS_PER_PROFESSOR = 3;
    
    // ==================== User Configuration ====================
    
    public static final String DEFAULT_PASSWORD = "password123";
    public static final int PROFESSOR_ACTIVE_PERCENTAGE = 80; // 80% active, 20% inactive
    
    // ==================== Department Information ====================
    
    public static final Map<String, String> DEPARTMENT_NAMES = new HashMap<String, String>() {{
        put("CS", "Computer Science");
        put("MATH", "Mathematics");
        put("PHYS", "Physics");
        put("ENG", "Engineering");
        put("BUS", "Business Administration");
    }};
    
    public static final Map<String, String> DEPARTMENT_DESCRIPTIONS = new HashMap<String, String>() {{
        put("CS", "Department of Computer Science - Specializing in software engineering, artificial intelligence, and data science");
        put("MATH", "Department of Mathematics - Covering pure and applied mathematics, statistics, and mathematical modeling");
        put("PHYS", "Department of Physics - Focusing on theoretical physics, experimental physics, and applied physics");
        put("ENG", "Department of Engineering - Offering programs in civil, mechanical, and electrical engineering");
        put("BUS", "Department of Business Administration - Providing education in management, finance, and entrepreneurship");
    }};
    
    // ==================== Course Information ====================
    
    public static final Map<String, List<CourseInfo>> DEPARTMENT_COURSES = new HashMap<String, List<CourseInfo>>() {{
        put("CS", Arrays.asList(
            new CourseInfo("CS101", "Introduction to Programming", "Undergraduate", "Fundamentals of programming using Java"),
            new CourseInfo("CS201", "Data Structures and Algorithms", "Undergraduate", "Core data structures and algorithmic techniques"),
            new CourseInfo("CS301", "Advanced Software Engineering", "Graduate", "Software design patterns and architecture")
        ));
        put("MATH", Arrays.asList(
            new CourseInfo("MATH101", "Calculus I", "Undergraduate", "Differential and integral calculus"),
            new CourseInfo("MATH201", "Linear Algebra", "Undergraduate", "Vector spaces and matrix theory"),
            new CourseInfo("MATH301", "Advanced Mathematical Analysis", "Graduate", "Real and complex analysis")
        ));
        put("PHYS", Arrays.asList(
            new CourseInfo("PHYS101", "General Physics I", "Undergraduate", "Mechanics and thermodynamics"),
            new CourseInfo("PHYS201", "Electromagnetism", "Undergraduate", "Electric and magnetic fields"),
            new CourseInfo("PHYS301", "Quantum Mechanics", "Graduate", "Quantum theory and applications")
        ));
        put("ENG", Arrays.asList(
            new CourseInfo("ENG101", "Engineering Fundamentals", "Undergraduate", "Basic engineering principles"),
            new CourseInfo("ENG201", "Circuit Analysis", "Undergraduate", "Electrical circuit theory and analysis"),
            new CourseInfo("ENG301", "Advanced Control Systems", "Graduate", "Modern control theory and applications")
        ));
        put("BUS", Arrays.asList(
            new CourseInfo("BUS101", "Introduction to Business", "Undergraduate", "Business fundamentals and practices"),
            new CourseInfo("BUS201", "Financial Management", "Undergraduate", "Corporate finance and investment"),
            new CourseInfo("BUS301", "Strategic Management", "Graduate", "Business strategy and competitive analysis")
        ));
    }};
    
    // ==================== Document Type Configuration ====================
    
    public static final List<DocumentTypeEnum> ALL_DOCUMENT_TYPES = Arrays.asList(
        DocumentTypeEnum.SYLLABUS,
        DocumentTypeEnum.EXAM,
        DocumentTypeEnum.ASSIGNMENT,
        DocumentTypeEnum.PROJECT_DOCS,
        DocumentTypeEnum.LECTURE_NOTES,
        DocumentTypeEnum.OTHER
    );
    
    // ==================== File Configuration ====================
    
    public static final Map<DocumentTypeEnum, List<String>> ALLOWED_FILE_EXTENSIONS = new HashMap<DocumentTypeEnum, List<String>>() {{
        put(DocumentTypeEnum.SYLLABUS, Arrays.asList("pdf", "docx"));
        put(DocumentTypeEnum.EXAM, Arrays.asList("pdf"));
        put(DocumentTypeEnum.ASSIGNMENT, Arrays.asList("pdf", "zip"));
        put(DocumentTypeEnum.PROJECT_DOCS, Arrays.asList("zip", "pdf"));
        put(DocumentTypeEnum.LECTURE_NOTES, Arrays.asList("pdf", "pptx"));
        put(DocumentTypeEnum.OTHER, Arrays.asList("pdf", "docx", "zip"));
    }};
    
    public static final Map<String, String> FILE_MIME_TYPES = new HashMap<String, String>() {{
        put("pdf", "application/pdf");
        put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        put("zip", "application/zip");
        put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    }};
    
    public static final int MAX_FILE_COUNT = 5;
    public static final int MAX_FILE_SIZE_MB = 50;
    public static final long MIN_FILE_SIZE_BYTES = 100 * 1024; // 100 KB
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    
    // ==================== Submission Status Distribution ====================
    
    public static final int UPLOADED_PERCENTAGE = 70;
    public static final int NOT_UPLOADED_PERCENTAGE = 20;
    public static final int OVERDUE_PERCENTAGE = 10;
    public static final int LATE_SUBMISSION_PERCENTAGE = 15;
    
    // ==================== Notification Configuration ====================
    
    public static final int NOTIFICATION_READ_PERCENTAGE = 60;
    public static final int NOTIFICATION_DAYS_BACK = 30;
    
    public static final Map<String, Integer> NOTIFICATION_TYPE_DISTRIBUTION = new HashMap<String, Integer>() {{
        put("NEW_REQUEST", 30);
        put("REQUEST_REMINDER", 20);
        put("DEADLINE_APPROACHING", 25);
        put("DOCUMENT_SUBMITTED", 15);
        put("DOCUMENT_OVERDUE", 10);
    }};
    
    // ==================== Academic Year Configuration ====================
    
    public static final int CURRENT_ACADEMIC_YEAR_START = 2024; // 2024-2025 is current
    
    // ==================== Helper Class ====================
    
    /**
     * Helper class to store course information.
     */
    public static class CourseInfo {
        public final String code;
        public final String name;
        public final String level;
        public final String description;
        
        public CourseInfo(String code, String name, String level, String description) {
            this.code = code;
            this.name = name;
            this.level = level;
            this.description = description;
        }
    }
}
