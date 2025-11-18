package com.alqude.edu.ArchiveSystem.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Exception thrown when validation fails (400 errors).
 */
@Getter
public class ValidationException extends ArchiveSystemException {
    
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_DATE_RANGE = "INVALID_DATE_RANGE";
    public static final String INVALID_ACADEMIC_YEAR_FORMAT = "INVALID_ACADEMIC_YEAR_FORMAT";
    public static final String DUPLICATE_ASSIGNMENT = "DUPLICATE_ASSIGNMENT";
    public static final String MISSING_REQUIRED_FIELD = "MISSING_REQUIRED_FIELD";
    
    private final Map<String, String> validationErrors;
    
    public ValidationException(String errorCode, String message, Map<String, String> validationErrors) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
        this.validationErrors = validationErrors;
    }
    
    public ValidationException(String errorCode, String message, Map<String, String> validationErrors, Throwable cause) {
        super(errorCode, message, HttpStatus.BAD_REQUEST, cause);
        this.validationErrors = validationErrors;
    }
    
    // Convenience constructors for backward compatibility
    public ValidationException(String message, Map<String, String> validationErrors) {
        this(VALIDATION_ERROR, message, validationErrors);
    }
    
    public ValidationException(String message, Map<String, String> validationErrors, Throwable cause) {
        this(VALIDATION_ERROR, message, validationErrors, cause);
    }
    
    // Convenience factory methods
    
    public static ValidationException invalidDateRange(String startField, String endField) {
        return new ValidationException(
            INVALID_DATE_RANGE,
            "Invalid date range",
            Map.of(
                startField, "Start date must be before end date",
                endField, "End date must be after start date"
            )
        );
    }
    
    public static ValidationException invalidAcademicYearFormat(String yearCode) {
        return new ValidationException(
            INVALID_ACADEMIC_YEAR_FORMAT,
            "Invalid academic year format: " + yearCode,
            Map.of("yearCode", "Academic year must be in format YYYY-YYYY (e.g., 2024-2025)")
        );
    }
    
    public static ValidationException duplicateAssignment(Long professorId, Long courseId, Long semesterId) {
        return new ValidationException(
            DUPLICATE_ASSIGNMENT,
            "Course assignment already exists",
            Map.of(
                "professorId", String.valueOf(professorId),
                "courseId", String.valueOf(courseId),
                "semesterId", String.valueOf(semesterId),
                "error", "This professor is already assigned to this course for this semester"
            )
        );
    }
    
    public static ValidationException missingRequiredField(String fieldName) {
        return new ValidationException(
            MISSING_REQUIRED_FIELD,
            "Missing required field",
            Map.of(fieldName, "This field is required")
        );
    }
}
