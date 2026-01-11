package com.alquds.edu.ArchiveSystem.exception.core;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Exception thrown when a requested resource is not found (404 errors).
 */
public class ResourceNotFoundException extends ArchiveSystemException {
    
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String ACADEMIC_YEAR_NOT_FOUND = "ACADEMIC_YEAR_NOT_FOUND";
    public static final String SEMESTER_NOT_FOUND = "SEMESTER_NOT_FOUND";
    public static final String COURSE_NOT_FOUND = "COURSE_NOT_FOUND";
    public static final String COURSE_ASSIGNMENT_NOT_FOUND = "COURSE_ASSIGNMENT_NOT_FOUND";
    public static final String PROFESSOR_NOT_FOUND = "PROFESSOR_NOT_FOUND";
    public static final String DOCUMENT_SUBMISSION_NOT_FOUND = "DOCUMENT_SUBMISSION_NOT_FOUND";
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    public static final String DEPARTMENT_NOT_FOUND = "DEPARTMENT_NOT_FOUND";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    
    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
    
    public ResourceNotFoundException(String errorCode, String message, List<String> suggestions) {
        super(errorCode, message, HttpStatus.NOT_FOUND, suggestions);
    }
    
    public ResourceNotFoundException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.NOT_FOUND, cause);
    }
    
    // Convenience factory methods
    
    public static ResourceNotFoundException academicYear(Long id) {
        return new ResourceNotFoundException(
            ACADEMIC_YEAR_NOT_FOUND,
            "Academic year not found with ID: " + id,
            List.of("Verify the academic year ID", "Check if the academic year exists", "Contact administrator")
        );
    }
    
    public static ResourceNotFoundException academicYear(String yearCode) {
        return new ResourceNotFoundException(
            ACADEMIC_YEAR_NOT_FOUND,
            "Academic year not found with code: " + yearCode,
            List.of("Verify the year code format (e.g., 2024-2025)", "Check available academic years")
        );
    }
    
    public static ResourceNotFoundException semester(Long id) {
        return new ResourceNotFoundException(
            SEMESTER_NOT_FOUND,
            "Semester not found with ID: " + id,
            List.of("Verify the semester ID", "Check if the semester exists for the academic year")
        );
    }
    
    public static ResourceNotFoundException course(Long id) {
        return new ResourceNotFoundException(
            COURSE_NOT_FOUND,
            "Course not found with ID: " + id,
            List.of("Verify the course ID", "Check if the course is active")
        );
    }
    
    public static ResourceNotFoundException course(String courseCode) {
        return new ResourceNotFoundException(
            COURSE_NOT_FOUND,
            "Course not found with code: " + courseCode,
            List.of("Verify the course code", "Check if the course exists in the system")
        );
    }
    
    public static ResourceNotFoundException courseAssignment(Long id) {
        return new ResourceNotFoundException(
            COURSE_ASSIGNMENT_NOT_FOUND,
            "Course assignment not found with ID: " + id,
            List.of("Verify the assignment ID", "Check if the course is assigned to the professor for this semester")
        );
    }
    
    public static ResourceNotFoundException professor(Long id) {
        return new ResourceNotFoundException(
            PROFESSOR_NOT_FOUND,
            "Professor not found with ID: " + id,
            List.of("Verify the professor ID", "Check if the professor is active")
        );
    }
    
    public static ResourceNotFoundException professor(String professorId) {
        return new ResourceNotFoundException(
            PROFESSOR_NOT_FOUND,
            "Professor not found with professor ID: " + professorId,
            List.of("Verify the professor ID", "Check if the professor exists in the system")
        );
    }
    
    public static ResourceNotFoundException documentSubmission(Long id) {
        return new ResourceNotFoundException(
            DOCUMENT_SUBMISSION_NOT_FOUND,
            "Document submission not found with ID: " + id,
            List.of("Verify the submission ID", "Check if the submission exists")
        );
    }
    
    public static ResourceNotFoundException file(Long id) {
        return new ResourceNotFoundException(
            FILE_NOT_FOUND,
            "File not found with ID: " + id,
            List.of("Verify the file ID", "Check if the file was deleted", "Contact support if file should exist")
        );
    }
    
    public static ResourceNotFoundException department(Long id) {
        return new ResourceNotFoundException(
            DEPARTMENT_NOT_FOUND,
            "Department not found with ID: " + id,
            List.of("Verify the department ID", "Check available departments")
        );
    }
    
    public static ResourceNotFoundException user(Long id) {
        return new ResourceNotFoundException(
            USER_NOT_FOUND,
            "User not found with ID: " + id,
            List.of("Verify the user ID", "Check if the user is active")
        );
    }
    
    public static ResourceNotFoundException user(String email) {
        return new ResourceNotFoundException(
            USER_NOT_FOUND,
            "User not found with email: " + email,
            List.of("Verify the email address", "Check if the user is registered")
        );
    }
}
