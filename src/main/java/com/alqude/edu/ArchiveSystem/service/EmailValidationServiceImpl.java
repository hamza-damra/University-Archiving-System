package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.exception.ValidationException;
import com.alqude.edu.ArchiveSystem.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of EmailValidationService for validating role-specific emails.
 * 
 * Email format by role:
 * - ADMIN: username@admin.alquds.edu
 * - DEANSHIP: username@dean.alquds.edu
 * - HOD: hod.department_shortcut@dean.alquds.edu
 * - PROFESSOR: username@staff.alquds.edu
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailValidationServiceImpl implements EmailValidationService {
    
    private final DepartmentRepository departmentRepository;
    
    // Error codes for email validation
    public static final String INVALID_ADMIN_EMAIL = "INVALID_ADMIN_EMAIL";
    public static final String INVALID_DEANSHIP_EMAIL = "INVALID_DEANSHIP_EMAIL";
    public static final String INVALID_PROFESSOR_EMAIL = "INVALID_PROFESSOR_EMAIL";
    public static final String INVALID_HOD_EMAIL_FORMAT = "INVALID_HOD_EMAIL_FORMAT";
    public static final String INVALID_DEPARTMENT_SHORTCUT = "INVALID_DEPARTMENT_SHORTCUT";
    
    // Email suffixes for each role
    private static final String ADMIN_EMAIL_SUFFIX = "@admin.alquds.edu";
    private static final String DEANSHIP_EMAIL_SUFFIX = "@dean.alquds.edu";
    private static final String PROFESSOR_EMAIL_SUFFIX = "@staff.alquds.edu";
    
    // HOD email pattern: hod.<shortcut>@dean.alquds.edu
    // Shortcut must be lowercase alphanumeric
    private static final Pattern HOD_EMAIL_PATTERN = Pattern.compile(
        "^hod\\.([a-z0-9]+)@dean\\.alquds\\.edu$",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public void validateAdminEmail(String email) {
        log.debug("Validating admin email: {}", email);
        
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException(
                INVALID_ADMIN_EMAIL,
                "Admin email must end with @admin.alquds.edu",
                Map.of("email", "Email is required")
            );
        }
        
        String normalizedEmail = email.trim().toLowerCase();
        
        if (!normalizedEmail.endsWith(ADMIN_EMAIL_SUFFIX)) {
            log.warn("Invalid admin email format: {}", email);
            throw new ValidationException(
                INVALID_ADMIN_EMAIL,
                "Admin email must end with @admin.alquds.edu",
                Map.of("email", "Admin email must end with @admin.alquds.edu")
            );
        }
        
        log.debug("Admin email validation passed: {}", email);
    }
    
    @Override
    public void validateDeanshipEmail(String email) {
        log.debug("Validating deanship email: {}", email);
        
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException(
                INVALID_DEANSHIP_EMAIL,
                "Deanship email must end with @dean.alquds.edu",
                Map.of("email", "Email is required")
            );
        }
        
        String normalizedEmail = email.trim().toLowerCase();
        
        // Deanship emails should end with @dean.alquds.edu but NOT start with "hod."
        if (!normalizedEmail.endsWith(DEANSHIP_EMAIL_SUFFIX)) {
            log.warn("Invalid deanship email format: {}", email);
            throw new ValidationException(
                INVALID_DEANSHIP_EMAIL,
                "Deanship email must end with @dean.alquds.edu",
                Map.of("email", "Deanship email must end with @dean.alquds.edu")
            );
        }
        
        // Make sure it's not an HOD email format
        if (normalizedEmail.startsWith("hod.")) {
            log.warn("Deanship email cannot start with 'hod.': {}", email);
            throw new ValidationException(
                INVALID_DEANSHIP_EMAIL,
                "Deanship email cannot start with 'hod.' - that format is reserved for HOD users",
                Map.of("email", "Deanship email cannot start with 'hod.'")
            );
        }
        
        log.debug("Deanship email validation passed: {}", email);
    }
    
    @Override
    public void validateProfessorEmail(String email) {
        log.debug("Validating professor email: {}", email);
        
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException(
                INVALID_PROFESSOR_EMAIL,
                "Professor email must end with @staff.alquds.edu",
                Map.of("email", "Email is required")
            );
        }
        
        String normalizedEmail = email.trim().toLowerCase();
        
        if (!normalizedEmail.endsWith(PROFESSOR_EMAIL_SUFFIX)) {
            log.warn("Invalid professor email format: {}", email);
            throw new ValidationException(
                INVALID_PROFESSOR_EMAIL,
                "Professor email must end with @staff.alquds.edu",
                Map.of("email", "Professor email must end with @staff.alquds.edu")
            );
        }
        
        log.debug("Professor email validation passed: {}", email);
    }
    
    @Override
    public void validateHodEmail(String email) {
        log.debug("Validating HOD email: {}", email);
        
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException(
                INVALID_HOD_EMAIL_FORMAT,
                "HOD email must be in the format hod.<department_shortcut>@dean.alquds.edu",
                Map.of("email", "Email is required")
            );
        }
        
        String normalizedEmail = email.trim().toLowerCase();
        
        // Check if email matches HOD pattern
        Matcher matcher = HOD_EMAIL_PATTERN.matcher(normalizedEmail);
        if (!matcher.matches()) {
            log.warn("Invalid HOD email format: {}", email);
            throw new ValidationException(
                INVALID_HOD_EMAIL_FORMAT,
                "HOD email must be in the format hod.<department_shortcut>@dean.alquds.edu",
                Map.of("email", "HOD email must be in the format hod.<department_shortcut>@dean.alquds.edu")
            );
        }
        
        // Extract and validate department shortcut
        String shortcut = matcher.group(1);
        
        if (!departmentRepository.existsByShortcut(shortcut)) {
            log.warn("Department shortcut '{}' does not exist for HOD email: {}", shortcut, email);
            throw new ValidationException(
                INVALID_DEPARTMENT_SHORTCUT,
                "Department shortcut '" + shortcut + "' does not exist",
                Map.of("email", "Department shortcut '" + shortcut + "' does not exist")
            );
        }
        
        log.debug("HOD email validation passed: {}", email);
    }
    
    @Override
    public String extractDepartmentShortcut(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        
        String normalizedEmail = email.trim().toLowerCase();
        Matcher matcher = HOD_EMAIL_PATTERN.matcher(normalizedEmail);
        
        if (matcher.matches()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    @Override
    public boolean isValidProfessorEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        return email.trim().toLowerCase().endsWith(PROFESSOR_EMAIL_SUFFIX);
    }
    
    @Override
    public boolean isValidAdminEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        return email.trim().toLowerCase().endsWith(ADMIN_EMAIL_SUFFIX);
    }
    
    @Override
    public boolean isValidDeanshipEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String normalizedEmail = email.trim().toLowerCase();
        return normalizedEmail.endsWith(DEANSHIP_EMAIL_SUFFIX) && !normalizedEmail.startsWith("hod.");
    }
    
    @Override
    public boolean isValidHodEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        return HOD_EMAIL_PATTERN.matcher(email.trim().toLowerCase()).matches();
    }
}
