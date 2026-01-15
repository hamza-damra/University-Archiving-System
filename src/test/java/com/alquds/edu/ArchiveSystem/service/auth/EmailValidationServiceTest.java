package com.alquds.edu.ArchiveSystem.service.auth;

import com.alquds.edu.ArchiveSystem.exception.core.ValidationException;
import com.alquds.edu.ArchiveSystem.repository.academic.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailValidationService following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Unit tests with mocked dependencies
 * - Test email format validation for each role
 * - Follow AAA pattern
 * - Test edge cases and invalid formats
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailValidationService Unit Tests")
class EmailValidationServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;
    
    @InjectMocks
    private EmailValidationServiceImpl emailValidationService;
    
    // ==================== Admin Email Validation Tests ====================
    
    @Test
    @DisplayName("Should validate admin email successfully")
    void shouldValidateAdminEmailSuccessfully() {
        // Arrange
        String email = "admin@admin.alquds.edu";
        
        // Act & Assert - should not throw
        emailValidationService.validateAdminEmail(email);
    }
    
    @Test
    @DisplayName("Should validate admin email with uppercase")
    void shouldValidateAdminEmailWithUppercase() {
        // Arrange
        String email = "ADMIN@ADMIN.ALQUDS.EDU";
        
        // Act & Assert - should not throw (normalized to lowercase)
        emailValidationService.validateAdminEmail(email);
    }
    
    @Test
    @DisplayName("Should throw exception when admin email has wrong domain")
    void shouldThrowExceptionWhenAdminEmailHasWrongDomain() {
        // Arrange
        String email = "admin@staff.alquds.edu";
        
        // Act & Assert
        assertThatThrownBy(() -> emailValidationService.validateAdminEmail(email))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("@admin.alquds.edu");
    }
    
    @Test
    @DisplayName("Should throw exception when admin email is null")
    void shouldThrowExceptionWhenAdminEmailIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> emailValidationService.validateAdminEmail(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("@admin.alquds.edu");
    }
    
    @Test
    @DisplayName("Should throw exception when admin email is empty")
    void shouldThrowExceptionWhenAdminEmailIsEmpty() {
        // Arrange
        String email = "   ";
        
        // Act & Assert
        assertThatThrownBy(() -> emailValidationService.validateAdminEmail(email))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("@admin.alquds.edu");
    }
    
    // ==================== Professor Email Validation Tests ====================
    
    @Test
    @DisplayName("Should validate professor email successfully")
    void shouldValidateProfessorEmailSuccessfully() {
        // Arrange
        String email = "professor@staff.alquds.edu";
        
        // Act & Assert - should not throw
        emailValidationService.validateProfessorEmail(email);
    }
    
    @Test
    @DisplayName("Should throw exception when professor email has wrong domain")
    void shouldThrowExceptionWhenProfessorEmailHasWrongDomain() {
        // Arrange
        String email = "professor@admin.alquds.edu";
        
        // Act & Assert
        assertThatThrownBy(() -> emailValidationService.validateProfessorEmail(email))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("@staff.alquds.edu");
    }
    
    @Test
    @DisplayName("Should throw exception when professor email is null")
    void shouldThrowExceptionWhenProfessorEmailIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> emailValidationService.validateProfessorEmail(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("@staff.alquds.edu");
    }
    
    // ==================== HOD Email Validation Tests ====================
    
    @Test
    @DisplayName("Should validate HOD email successfully")
    void shouldValidateHodEmailSuccessfully() {
        // Arrange
        String email = "hod.cs@hod.alquds.edu";
        
        when(departmentRepository.existsByShortcut("cs")).thenReturn(true);
        
        // Act & Assert - should not throw
        emailValidationService.validateHodEmail(email);
        
        verify(departmentRepository).existsByShortcut("cs");
    }
    
    @Test
    @DisplayName("Should throw exception when HOD email has wrong format")
    void shouldThrowExceptionWhenHodEmailHasWrongFormat() {
        // Arrange
        String email = "hod@hod.alquds.edu"; // Missing department shortcut
        
        // Act & Assert
        assertThatThrownBy(() -> emailValidationService.validateHodEmail(email))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("hod.<department_shortcut>");
    }
    
    @Test
    @DisplayName("Should throw exception when HOD email department shortcut does not exist")
    void shouldThrowExceptionWhenHodEmailDepartmentShortcutDoesNotExist() {
        // Arrange
        String email = "hod.nonexistent@hod.alquds.edu";
        
        when(departmentRepository.existsByShortcut("nonexistent")).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> emailValidationService.validateHodEmail(email))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not exist");
        
        verify(departmentRepository).existsByShortcut("nonexistent");
    }
    
    @Test
    @DisplayName("Should throw exception when HOD email is null")
    void shouldThrowExceptionWhenHodEmailIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> emailValidationService.validateHodEmail(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("hod.<department_shortcut>");
    }
    
    // ==================== Deanship Email Validation Tests ====================
    
    @Test
    @DisplayName("Should validate deanship email successfully")
    void shouldValidateDeanshipEmailSuccessfully() {
        // Arrange
        String email = "dean@dean.alquds.edu";
        
        // Act & Assert - should not throw
        emailValidationService.validateDeanshipEmail(email);
    }
    
    @Test
    @DisplayName("Should throw exception when deanship email starts with hod")
    void shouldThrowExceptionWhenDeanshipEmailStartsWithHod() {
        // Arrange
        String email = "hod.cs@dean.alquds.edu";
        
        // Act & Assert
        assertThatThrownBy(() -> emailValidationService.validateDeanshipEmail(email))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot start with 'hod.'");
    }
    
    @Test
    @DisplayName("Should throw exception when deanship email has wrong domain")
    void shouldThrowExceptionWhenDeanshipEmailHasWrongDomain() {
        // Arrange
        String email = "dean@staff.alquds.edu";
        
        // Act & Assert
        assertThatThrownBy(() -> emailValidationService.validateDeanshipEmail(email))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("@dean.alquds.edu");
    }
    
    // ==================== Email Format Check Tests ====================
    
    @Test
    @DisplayName("Should return true for valid professor email format")
    void shouldReturnTrueForValidProfessorEmailFormat() {
        // Arrange
        String email = "professor@staff.alquds.edu";
        
        // Act
        boolean isValid = emailValidationService.isValidProfessorEmailFormat(email);
        
        // Assert
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("Should return false for invalid professor email format")
    void shouldReturnFalseForInvalidProfessorEmailFormat() {
        // Arrange
        String email = "professor@admin.alquds.edu";
        
        // Act
        boolean isValid = emailValidationService.isValidProfessorEmailFormat(email);
        
        // Assert
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should return true for valid admin email format")
    void shouldReturnTrueForValidAdminEmailFormat() {
        // Arrange
        String email = "admin@admin.alquds.edu";
        
        // Act
        boolean isValid = emailValidationService.isValidAdminEmailFormat(email);
        
        // Assert
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("Should return true for valid HOD email format")
    void shouldReturnTrueForValidHodEmailFormat() {
        // Arrange
        String email = "hod.cs@hod.alquds.edu";
        
        // Act
        boolean isValid = emailValidationService.isValidHodEmailFormat(email);
        
        // Assert
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("Should return false for invalid HOD email format")
    void shouldReturnFalseForInvalidHodEmailFormat() {
        // Arrange
        String email = "hod@hod.alquds.edu"; // Missing department shortcut
        
        // Act
        boolean isValid = emailValidationService.isValidHodEmailFormat(email);
        
        // Assert
        assertThat(isValid).isFalse();
    }
    
    // ==================== Extract Department Shortcut Tests ====================
    
    @Test
    @DisplayName("Should extract department shortcut from HOD email")
    void shouldExtractDepartmentShortcutFromHodEmail() {
        // Arrange
        String email = "hod.cs@hod.alquds.edu";
        
        // Act
        String shortcut = emailValidationService.extractDepartmentShortcut(email);
        
        // Assert
        assertThat(shortcut).isEqualTo("cs");
    }
    
    @Test
    @DisplayName("Should return null when email is not HOD format")
    void shouldReturnNullWhenEmailIsNotHodFormat() {
        // Arrange
        String email = "professor@staff.alquds.edu";
        
        // Act
        String shortcut = emailValidationService.extractDepartmentShortcut(email);
        
        // Assert
        assertThat(shortcut).isNull();
    }
    
    @Test
    @DisplayName("Should return null when email is null")
    void shouldReturnNullWhenEmailIsNull() {
        // Act
        String shortcut = emailValidationService.extractDepartmentShortcut(null);
        
        // Assert
        assertThat(shortcut).isNull();
    }
}
