package com.alqude.edu.ArchiveSystem.service;

/**
 * Service interface for email validation operations.
 * Provides validation for all role-specific email formats according to university standards.
 * 
 * Email format by role:
 * - ADMIN: username@admin.alquds.edu
 * - DEANSHIP: username@dean.alquds.edu
 * - HOD: hod.department_shortcut@dean.alquds.edu
 * - PROFESSOR: username@staff.alquds.edu
 */
public interface EmailValidationService {
    
    /**
     * Validates admin email format.
     * Admin emails must end with @admin.alquds.edu
     * 
     * @param email The email to validate
     * @throws com.alqude.edu.ArchiveSystem.exception.ValidationException if email doesn't end with @admin.alquds.edu
     */
    void validateAdminEmail(String email);
    
    /**
     * Validates deanship email format.
     * Deanship emails must end with @dean.alquds.edu
     * 
     * @param email The email to validate
     * @throws com.alqude.edu.ArchiveSystem.exception.ValidationException if email doesn't end with @dean.alquds.edu
     */
    void validateDeanshipEmail(String email);
    
    /**
     * Validates professor email format.
     * Professor emails must end with @staff.alquds.edu
     * 
     * @param email The email to validate
     * @throws com.alqude.edu.ArchiveSystem.exception.ValidationException if email doesn't end with @staff.alquds.edu
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
     * Checks if an email is a valid admin email format.
     * 
     * @param email The email to check
     * @return true if email ends with @admin.alquds.edu
     */
    boolean isValidAdminEmailFormat(String email);
    
    /**
     * Checks if an email is a valid deanship email format.
     * 
     * @param email The email to check
     * @return true if email ends with @dean.alquds.edu
     */
    boolean isValidDeanshipEmailFormat(String email);
    
    /**
     * Checks if an email is a valid professor email format.
     * 
     * @param email The email to check
     * @return true if email ends with @staff.alquds.edu
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
