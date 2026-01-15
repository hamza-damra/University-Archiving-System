package com.alquds.edu.ArchiveSystem.service.academic;

import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.repository.academic.*;
import com.alquds.edu.ArchiveSystem.repository.auth.RefreshTokenRepository;
import com.alquds.edu.ArchiveSystem.repository.file.FolderRepository;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DepartmentService following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Unit tests with mocked dependencies
 * - Test business logic in isolation
 * - Follow AAA pattern
 * - Test validation, CRUD operations, and edge cases
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentService Unit Tests")
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private CourseRepository courseRepository;
    
    @Mock
    private FolderRepository folderRepository;
    
    @Mock
    private UploadedFileRepository uploadedFileRepository;
    
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private CourseAssignmentRepository courseAssignmentRepository;
    
    @Mock
    private DocumentSubmissionRepository documentSubmissionRepository;
    
    @InjectMocks
    private DepartmentServiceImpl departmentService;
    
    private Department testDepartment;
    
    @BeforeEach
    void setUp() {
        testDepartment = TestDataBuilder.createDepartment();
        testDepartment.setId(1L);
    }
    
    // ==================== Create Department Tests ====================
    
    @Test
    @DisplayName("Should create department successfully when all validations pass")
    void shouldCreateDepartmentSuccessfully() {
        // Arrange
        Department department = TestDataBuilder.createDepartment();
        department.setShortcut("cs");
        
        Department savedDepartment = TestDataBuilder.createDepartment();
        savedDepartment.setId(1L);
        savedDepartment.setShortcut("cs");
        
        when(departmentRepository.existsByShortcut("cs")).thenReturn(false);
        when(departmentRepository.existsByName(department.getName())).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(savedDepartment);
        
        // Act
        Department result = departmentService.createDepartment(department);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getShortcut()).isEqualTo("cs");
        
        verify(departmentRepository).existsByShortcut("cs");
        verify(departmentRepository).existsByName(department.getName());
        verify(departmentRepository).save(department);
    }
    
    @Test
    @DisplayName("Should throw exception when shortcut contains uppercase letters")
    void shouldThrowExceptionWhenShortcutHasUppercase() {
        // Arrange
        Department department = TestDataBuilder.createDepartment();
        department.setShortcut("CS"); // Invalid: uppercase
        
        // Act & Assert
        assertThatThrownBy(() -> departmentService.createDepartment(department))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase letters");
        
        verify(departmentRepository, never()).save(any(Department.class));
    }
    
    @Test
    @DisplayName("Should throw exception when shortcut contains special characters")
    void shouldThrowExceptionWhenShortcutHasSpecialCharacters() {
        // Arrange
        Department department = TestDataBuilder.createDepartment();
        department.setShortcut("cs-101"); // Invalid: contains dash
        
        // Act & Assert
        assertThatThrownBy(() -> departmentService.createDepartment(department))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase letters");
        
        verify(departmentRepository, never()).save(any(Department.class));
    }
    
    @Test
    @DisplayName("Should throw exception when shortcut is null")
    void shouldThrowExceptionWhenShortcutIsNull() {
        // Arrange
        Department department = TestDataBuilder.createDepartment();
        department.setShortcut(null);
        
        // Act & Assert
        assertThatThrownBy(() -> departmentService.createDepartment(department))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
        
        verify(departmentRepository, never()).save(any(Department.class));
    }
    
    @Test
    @DisplayName("Should throw exception when shortcut is empty")
    void shouldThrowExceptionWhenShortcutIsEmpty() {
        // Arrange
        Department department = TestDataBuilder.createDepartment();
        department.setShortcut("");
        
        // Act & Assert
        assertThatThrownBy(() -> departmentService.createDepartment(department))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
        
        verify(departmentRepository, never()).save(any(Department.class));
    }
    
    @Test
    @DisplayName("Should throw exception when shortcut exceeds 20 characters")
    void shouldThrowExceptionWhenShortcutTooLong() {
        // Arrange
        Department department = TestDataBuilder.createDepartment();
        department.setShortcut("a".repeat(21)); // 21 characters
        
        // Act & Assert
        assertThatThrownBy(() -> departmentService.createDepartment(department))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20 characters");
        
        verify(departmentRepository, never()).save(any(Department.class));
    }
    
    @Test
    @DisplayName("Should throw exception when shortcut already exists")
    void shouldThrowExceptionWhenShortcutExists() {
        // Arrange
        Department department = TestDataBuilder.createDepartment();
        department.setShortcut("cs");
        
        when(departmentRepository.existsByShortcut("cs")).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> departmentService.createDepartment(department))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        
        verify(departmentRepository).existsByShortcut("cs");
        verify(departmentRepository, never()).save(any(Department.class));
    }
    
    @Test
    @DisplayName("Should throw exception when department name already exists")
    void shouldThrowExceptionWhenNameExists() {
        // Arrange
        Department department = TestDataBuilder.createDepartment();
        department.setShortcut("cs");
        
        when(departmentRepository.existsByShortcut("cs")).thenReturn(false);
        when(departmentRepository.existsByName(department.getName())).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> departmentService.createDepartment(department))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        
        verify(departmentRepository).existsByName(department.getName());
        verify(departmentRepository, never()).save(any(Department.class));
    }
    
    // ==================== Update Department Tests ====================
    
    @Test
    @DisplayName("Should update department successfully")
    void shouldUpdateDepartmentSuccessfully() {
        // Arrange
        Long departmentId = 1L;
        Department updateData = TestDataBuilder.createDepartment();
        updateData.setName("Updated Computer Science");
        updateData.setShortcut("cs");
        
        Department existingDepartment = TestDataBuilder.createDepartment();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Computer Science");
        existingDepartment.setShortcut("cs");
        
        Department updatedDepartment = TestDataBuilder.createDepartment();
        updatedDepartment.setId(departmentId);
        updatedDepartment.setName("Updated Computer Science");
        updatedDepartment.setShortcut("cs");
        
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.existsByShortcutAndIdNot("cs", departmentId)).thenReturn(false);
        when(departmentRepository.existsByName("Updated Computer Science")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);
        
        // Act
        Department result = departmentService.updateDepartment(departmentId, updateData);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Computer Science");
        verify(departmentRepository).findById(departmentId);
        verify(departmentRepository).save(existingDepartment);
    }
    
    @Test
    @DisplayName("Should throw exception when department not found for update")
    void shouldThrowExceptionWhenDepartmentNotFoundForUpdate() {
        // Arrange
        Long departmentId = 999L;
        Department updateData = TestDataBuilder.createDepartment();
        
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> departmentService.updateDepartment(departmentId, updateData))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
        
        verify(departmentRepository).findById(departmentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }
    
    @Test
    @DisplayName("Should throw exception when shortcut already exists for another department")
    void shouldThrowExceptionWhenShortcutExistsForAnotherDepartment() {
        // Arrange
        Long departmentId = 1L;
        Department updateData = TestDataBuilder.createDepartment();
        updateData.setShortcut("math"); // Different shortcut
        
        Department existingDepartment = TestDataBuilder.createDepartment();
        existingDepartment.setId(departmentId);
        existingDepartment.setShortcut("cs");
        
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.existsByShortcutAndIdNot("math", departmentId)).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> departmentService.updateDepartment(departmentId, updateData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        
        verify(departmentRepository, never()).save(any(Department.class));
    }
    
    // ==================== Find Department Tests ====================
    
    @Test
    @DisplayName("Should find department by ID successfully")
    void shouldFindDepartmentByIdSuccessfully() {
        // Arrange
        Long departmentId = 1L;
        
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(testDepartment));
        
        // Act
        Optional<Department> result = departmentService.findById(departmentId);
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(departmentId);
        verify(departmentRepository).findById(departmentId);
    }
    
    @Test
    @DisplayName("Should return empty when department not found by ID")
    void shouldReturnEmptyWhenDepartmentNotFoundById() {
        // Arrange
        Long departmentId = 999L;
        
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());
        
        // Act
        Optional<Department> result = departmentService.findById(departmentId);
        
        // Assert
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Should find department by shortcut successfully")
    void shouldFindDepartmentByShortcutSuccessfully() {
        // Arrange
        String shortcut = "cs";
        testDepartment.setShortcut("CS"); // Repository may return uppercase
        
        when(departmentRepository.findByShortcut(shortcut)).thenReturn(Optional.of(testDepartment));
        
        // Act
        Optional<Department> result = departmentService.findByShortcut(shortcut);
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getShortcut()).isEqualTo("CS"); // Match actual returned value
        verify(departmentRepository).findByShortcut(shortcut);
    }
    
    @Test
    @DisplayName("Should return all departments")
    void shouldReturnAllDepartments() {
        // Arrange
        Department dept1 = TestDataBuilder.createDepartment();
        dept1.setId(1L);
        Department dept2 = TestDataBuilder.createDepartment();
        dept2.setId(2L);
        dept2.setName("Mathematics");
        dept2.setShortcut("math");
        
        when(departmentRepository.findAll()).thenReturn(List.of(dept1, dept2));
        
        // Act
        List<Department> result = departmentService.findAll();
        
        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).contains(dept1, dept2);
        verify(departmentRepository).findAll();
    }
    
    // ==================== Shortcut Validation Tests ====================
    
    @Test
    @DisplayName("Should validate shortcut format successfully")
    void shouldValidateShortcutFormatSuccessfully() {
        // Arrange
        String validShortcut = "cs123";
        
        // Act & Assert - should not throw
        departmentService.validateShortcutFormat(validShortcut);
        
        // No exception means validation passed
    }
    
    @Test
    @DisplayName("Should validate shortcut with numbers")
    void shouldValidateShortcutWithNumbers() {
        // Arrange
        String shortcut = "cs101";
        
        // Act & Assert - should not throw
        departmentService.validateShortcutFormat(shortcut);
    }
    
    @Test
    @DisplayName("Should check if shortcut exists")
    void shouldCheckIfShortcutExists() {
        // Arrange
        String shortcut = "cs";
        
        when(departmentRepository.existsByShortcut(shortcut)).thenReturn(true);
        
        // Act
        boolean exists = departmentService.shortcutExists(shortcut);
        
        // Assert
        assertThat(exists).isTrue();
        verify(departmentRepository).existsByShortcut(shortcut);
    }
    
    @Test
    @DisplayName("Should return false when shortcut does not exist")
    void shouldReturnFalseWhenShortcutDoesNotExist() {
        // Arrange
        String shortcut = "nonexistent";
        
        when(departmentRepository.existsByShortcut(shortcut)).thenReturn(false);
        
        // Act
        boolean exists = departmentService.shortcutExists(shortcut);
        
        // Assert
        assertThat(exists).isFalse();
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    @DisplayName("Should accept valid shortcut with only lowercase letters")
    void shouldAcceptValidShortcutWithOnlyLowercaseLetters() {
        // Arrange
        Department department = TestDataBuilder.createDepartment();
        department.setShortcut("computerscience");
        
        Department savedDepartment = TestDataBuilder.createDepartment();
        savedDepartment.setId(1L);
        savedDepartment.setShortcut("computerscience");
        
        when(departmentRepository.existsByShortcut("computerscience")).thenReturn(false);
        when(departmentRepository.existsByName(department.getName())).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(savedDepartment);
        
        // Act
        Department result = departmentService.createDepartment(department);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getShortcut()).isEqualTo("computerscience");
    }
    
    @Test
    @DisplayName("Should accept valid shortcut with only numbers")
    void shouldAcceptValidShortcutWithOnlyNumbers() {
        // Arrange
        Department department = TestDataBuilder.createDepartment();
        department.setShortcut("123");
        
        Department savedDepartment = TestDataBuilder.createDepartment();
        savedDepartment.setId(1L);
        savedDepartment.setShortcut("123");
        
        when(departmentRepository.existsByShortcut("123")).thenReturn(false);
        when(departmentRepository.existsByName(department.getName())).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(savedDepartment);
        
        // Act
        Department result = departmentService.createDepartment(department);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getShortcut()).isEqualTo("123");
    }
}
