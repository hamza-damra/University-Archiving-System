package com.alqude.edu.ArchiveSystem.service;

/**
 * Service interface for email validation operations.
 * Provides validation for Professor and HOD email formats according to university standards.
 */
public interface EmailValidationService {
    
    /**
     * Validates professor email format.
     * Professor emails must end with @stuff.alquds.edu
     * 
     * @param email The email to validate
     * @throws com.alqude.edu.ArchiveSystem.exception.ValidationException if email doesn't end with @stuff.alquds.edu
     */
    void validateProfessorEmail(String email);
    
    /**
     * Validates HOD email format and department shortcut.
     * HOD emails must follow the pattern hod.<department_shortcut>@dean.alquds.edu
     * 
     * @param email The email to validate
     * @throws com.alqude.edu.ArchiveSystem.exception.ValidationException if email doesn't match pattern or shortcut doesn't exist
     */
    void validateHodEmail(String email);
    
    /**
     * Extracts department shortcut from HOD email.
     * 
     * @param email The HOD email
     * @return The extracted shortcut or null if invalid format
     */
    String extractDepartmentShortcut(String email);
    
    /**
     * Checks if an email is a valid professor email format.
     * 
     * @param email The email to check
     * @return true if email ends with @stuff.alquds.edu
     */
    boolean isValidProfessorEmailFormat(String email);
    
    /**
     * Checks if an email is a valid HOD email format.
     * 
     * @param email The email to check
     * @return true if email matches pattern hod.<shortcut>@dean.alquds.edu
     */
    boolean isValidHodEmailFormat(String email);
}
