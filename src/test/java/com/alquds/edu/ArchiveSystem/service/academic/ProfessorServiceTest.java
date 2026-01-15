package com.alquds.edu.ArchiveSystem.service.academic;

import com.alquds.edu.ArchiveSystem.dto.professor.CourseAssignmentWithStatus;
import com.alquds.edu.ArchiveSystem.dto.professor.ProfessorDashboardOverview;
import com.alquds.edu.ArchiveSystem.dto.user.ProfessorDTO;
import com.alquds.edu.ArchiveSystem.entity.academic.*;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.submission.RequiredDocumentType;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.exception.core.BusinessException;
import com.alquds.edu.ArchiveSystem.exception.core.DuplicateEntityException;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.DepartmentRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.RequiredDocumentTypeRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.service.auth.EmailValidationService;
import com.alquds.edu.ArchiveSystem.service.file.FolderService;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProfessorService following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Mock all external dependencies (repositories, services)
 * - Test business logic in isolation
 * - Follow AAA pattern (Arrange, Act, Assert)
 * - Test edge cases and error conditions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfessorService Unit Tests")
class ProfessorServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DepartmentRepository departmentRepository;
    
    @Mock
    private CourseAssignmentRepository courseAssignmentRepository;
    
    @Mock
    private RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    
    @Mock
    private DocumentSubmissionRepository documentSubmissionRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private FolderService folderService;
    
    @Mock
    private EmailValidationService emailValidationService;
    
    @InjectMocks
    private ProfessorServiceImpl professorService;
    
    private User testProfessor;
    private Department testDepartment;
    private ProfessorDTO professorDTO;
    private AcademicYear testAcademicYear;
    private Semester testSemester;
    private Course testCourse;
    private CourseAssignment testCourseAssignment;
    
    @BeforeEach
    void setUp() {
        // Arrange: Set up test data
        testDepartment = TestDataBuilder.createDepartment();
        testDepartment.setId(1L);
        
        testProfessor = TestDataBuilder.createProfessorUser();
        testProfessor.setId(1L);
        testProfessor.setDepartment(testDepartment);
        testProfessor.setProfessorId("PROF1");
        
        professorDTO = TestDataBuilder.createProfessorDTO();
        professorDTO.setDepartmentId(1L);
        
        testAcademicYear = TestDataBuilder.createAcademicYear();
        testAcademicYear.setId(1L);
        
        testSemester = TestDataBuilder.createSemester();
        testSemester.setId(1L);
        testSemester.setAcademicYear(testAcademicYear);
        
        testCourse = TestDataBuilder.createCourse();
        testCourse.setId(1L);
        testCourse.setDepartment(testDepartment);
        
        testCourseAssignment = TestDataBuilder.createCourseAssignment();
        testCourseAssignment.setId(1L);
        testCourseAssignment.setProfessor(testProfessor);
        testCourseAssignment.setCourse(testCourse);
        testCourseAssignment.setSemester(testSemester);
    }
    
    // ==================== Create Professor Tests ====================
    
    @Test
    @DisplayName("Should create professor successfully when all validations pass")
    void shouldCreateProfessorSuccessfully() {
        // Arrange
        User savedProfessor = TestDataBuilder.createProfessorUser();
        savedProfessor.setId(1L);
        savedProfessor.setDepartment(testDepartment);
        
        doNothing().when(emailValidationService).validateProfessorEmail(professorDTO.getEmail());
        when(userRepository.existsByEmail(professorDTO.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(passwordEncoder.encode(professorDTO.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedProfessor);
        
        // Act
        User result = professorService.createProfessor(professorDTO);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRole()).isEqualTo(Role.ROLE_PROFESSOR);
        assertThat(result.getDepartment()).isEqualTo(testDepartment);
        assertThat(result.getIsActive()).isTrue();
        
        verify(emailValidationService).validateProfessorEmail(professorDTO.getEmail());
        verify(userRepository).existsByEmail(professorDTO.getEmail());
        verify(departmentRepository).findById(1L);
        verify(passwordEncoder).encode(professorDTO.getPassword());
        verify(userRepository, times(2)).save(any(User.class)); // Once to get ID, once to set professorId
    }
    
    @Test
    @DisplayName("Should call EmailValidationService when creating professor")
    void shouldCallEmailValidationServiceWhenCreatingProfessor() {
        // Arrange
        doNothing().when(emailValidationService).validateProfessorEmail(professorDTO.getEmail());
        when(userRepository.existsByEmail(professorDTO.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(passwordEncoder.encode(professorDTO.getPassword())).thenReturn("encodedPassword");
        
        User savedProfessor = TestDataBuilder.createProfessorUser();
        savedProfessor.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedProfessor);
        
        // Act
        professorService.createProfessor(professorDTO);
        
        // Assert
        verify(emailValidationService).validateProfessorEmail(professorDTO.getEmail());
    }
    
    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Arrange
        doNothing().when(emailValidationService).validateProfessorEmail(professorDTO.getEmail());
        when(userRepository.existsByEmail(professorDTO.getEmail())).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> professorService.createProfessor(professorDTO))
                .isInstanceOf(DuplicateEntityException.class)
                .hasMessageContaining("already exists");
        
        verify(userRepository).existsByEmail(professorDTO.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when department not found")
    void shouldThrowExceptionWhenDepartmentNotFound() {
        // Arrange
        doNothing().when(emailValidationService).validateProfessorEmail(professorDTO.getEmail());
        when(userRepository.existsByEmail(professorDTO.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> professorService.createProfessor(professorDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Department not found");
        
        verify(departmentRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when password is not provided")
    void shouldThrowExceptionWhenPasswordNotProvided() {
        // Arrange
        professorDTO.setPassword(null);
        
        doNothing().when(emailValidationService).validateProfessorEmail(professorDTO.getEmail());
        when(userRepository.existsByEmail(professorDTO.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        
        // Act & Assert
        assertThatThrownBy(() -> professorService.createProfessor(professorDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Password is required");
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when password is empty")
    void shouldThrowExceptionWhenPasswordIsEmpty() {
        // Arrange
        professorDTO.setPassword("   ");
        
        doNothing().when(emailValidationService).validateProfessorEmail(professorDTO.getEmail());
        when(userRepository.existsByEmail(professorDTO.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        
        // Act & Assert
        assertThatThrownBy(() -> professorService.createProfessor(professorDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Password is required");
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should generate professor ID in PROF{id} format during creation")
    void shouldGenerateProfessorIdInCorrectFormatDuringCreation() {
        // Arrange
        User savedProfessor = TestDataBuilder.createProfessorUser();
        savedProfessor.setId(12345L);
        savedProfessor.setDepartment(testDepartment);
        
        doNothing().when(emailValidationService).validateProfessorEmail(professorDTO.getEmail());
        when(userRepository.existsByEmail(professorDTO.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(passwordEncoder.encode(professorDTO.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedProfessor);
        
        // Act
        professorService.createProfessor(professorDTO);
        
        // Assert
        verify(userRepository, times(2)).save(argThat(user -> {
            if (user.getProfessorId() != null) {
                return user.getProfessorId().equals("PROF12345");
            }
            return true; // First save doesn't have professorId yet
        }));
    }
    
    // ==================== Update Professor Tests ====================
    
    @Test
    @DisplayName("Should update professor successfully")
    void shouldUpdateProfessorSuccessfully() {
        // Arrange
        Long professorId = 1L;
        ProfessorDTO updateDTO = TestDataBuilder.createProfessorDTO();
        updateDTO.setFirstName("Updated");
        updateDTO.setLastName("Name");
        updateDTO.setPassword(null); // Password not being updated
        
        User existingProfessor = TestDataBuilder.createProfessorUser();
        existingProfessor.setId(professorId);
        existingProfessor.setDepartment(testDepartment);
        
        when(userRepository.findById(professorId)).thenReturn(Optional.of(existingProfessor));
        when(userRepository.save(any(User.class))).thenReturn(existingProfessor);
        
        // Act
        User result = professorService.updateProfessor(professorId, updateDTO);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(professorId);
        verify(userRepository).findById(professorId);
        verify(userRepository).save(existingProfessor);
        verify(passwordEncoder, never()).encode(anyString());
    }
    
    @Test
    @DisplayName("Should throw exception when professor not found for update")
    void shouldThrowExceptionWhenProfessorNotFoundForUpdate() {
        // Arrange
        Long professorId = 999L;
        ProfessorDTO updateDTO = TestDataBuilder.createProfessorDTO();
        
        when(userRepository.findById(professorId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> professorService.updateProfessor(professorId, updateDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Professor not found");
        
        verify(userRepository).findById(professorId);
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when department not found during update")
    void shouldThrowExceptionWhenDepartmentNotFoundDuringUpdate() {
        // Arrange
        Long professorId = 1L;
        ProfessorDTO updateDTO = TestDataBuilder.createProfessorDTO();
        updateDTO.setDepartmentId(999L);
        
        User existingProfessor = TestDataBuilder.createProfessorUser();
        existingProfessor.setId(professorId);
        existingProfessor.setDepartment(testDepartment);
        
        when(userRepository.findById(professorId)).thenReturn(Optional.of(existingProfessor));
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> professorService.updateProfessor(professorId, updateDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Department not found");
        
        verify(departmentRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should update password when provided in update")
    void shouldUpdatePasswordWhenProvided() {
        // Arrange
        Long professorId = 1L;
        ProfessorDTO updateDTO = TestDataBuilder.createProfessorDTO();
        updateDTO.setPassword("NewPassword123!");
        
        User existingProfessor = TestDataBuilder.createProfessorUser();
        existingProfessor.setId(professorId);
        existingProfessor.setDepartment(testDepartment);
        
        when(userRepository.findById(professorId)).thenReturn(Optional.of(existingProfessor));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingProfessor);
        
        // Act
        professorService.updateProfessor(professorId, updateDTO);
        
        // Assert
        verify(passwordEncoder).encode("NewPassword123!");
        verify(userRepository).save(argThat(user -> 
            "encodedNewPassword".equals(user.getPassword())
        ));
    }
    
    // ==================== Get Professor Tests ====================
    
    @Test
    @DisplayName("Should retrieve professor by ID successfully")
    void shouldRetrieveProfessorByIdSuccessfully() {
        // Arrange
        Long professorId = 1L;
        
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));
        
        // Act
        User result = professorService.getProfessor(professorId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(professorId);
        assertThat(result.getRole()).isEqualTo(Role.ROLE_PROFESSOR);
        verify(userRepository).findById(professorId);
    }
    
    @Test
    @DisplayName("Should throw exception when professor not found")
    void shouldThrowExceptionWhenProfessorNotFound() {
        // Arrange
        Long professorId = 999L;
        
        when(userRepository.findById(professorId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> professorService.getProfessor(professorId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Professor not found");
        
        verify(userRepository).findById(professorId);
    }
    
    @Test
    @DisplayName("Should throw exception when user is not a professor")
    void shouldThrowExceptionWhenUserIsNotProfessor() {
        // Arrange
        Long userId = 1L;
        User adminUser = TestDataBuilder.createAdminUser();
        adminUser.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));
        
        // Act & Assert
        assertThatThrownBy(() -> professorService.getProfessor(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("is not a professor");
    }
    
    // ==================== Get Professors By Department Tests ====================
    
    @Test
    @DisplayName("Should return list of professors by department")
    void shouldReturnListOfProfessorsByDepartment() {
        // Arrange
        Long departmentId = 1L;
        List<User> professors = List.of(testProfessor);
        
        when(departmentRepository.existsById(departmentId)).thenReturn(true);
        when(userRepository.findByDepartmentIdAndRole(departmentId, Role.ROLE_PROFESSOR))
                .thenReturn(professors);
        
        // Act
        List<User> result = professorService.getProfessorsByDepartment(departmentId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testProfessor);
        verify(departmentRepository).existsById(departmentId);
        verify(userRepository).findByDepartmentIdAndRole(departmentId, Role.ROLE_PROFESSOR);
    }
    
    @Test
    @DisplayName("Should throw exception when department not found")
    void shouldThrowExceptionWhenDepartmentNotFoundForGetProfessors() {
        // Arrange
        Long departmentId = 999L;
        
        when(departmentRepository.existsById(departmentId)).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> professorService.getProfessorsByDepartment(departmentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Department not found");
        
        verify(departmentRepository).existsById(departmentId);
        verify(userRepository, never()).findByDepartmentIdAndRole(anyLong(), any());
    }
    
    // ==================== Get All Professors Tests ====================
    
    @Test
    @DisplayName("Should return all professors")
    void shouldReturnAllProfessors() {
        // Arrange
        User professor1 = TestDataBuilder.createProfessorUser();
        professor1.setId(1L);
        User professor2 = TestDataBuilder.createProfessorUser();
        professor2.setId(2L);
        professor2.setEmail("professor2@staff.alquds.edu");
        User adminUser = TestDataBuilder.createAdminUser();
        adminUser.setId(3L);
        
        List<User> allUsers = List.of(professor1, professor2, adminUser);
        
        when(userRepository.findAll()).thenReturn(allUsers);
        
        // Act
        List<User> result = professorService.getAllProfessors();
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2); // Only professors, not admin
        assertThat(result).allMatch(user -> user.getRole() == Role.ROLE_PROFESSOR);
        verify(userRepository).findAll();
    }
    
    // ==================== Deactivate/Activate Professor Tests ====================
    
    @Test
    @DisplayName("Should deactivate professor successfully")
    void shouldDeactivateProfessorSuccessfully() {
        // Arrange
        Long professorId = 1L;
        testProfessor.setIsActive(true);
        
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));
        when(userRepository.save(any(User.class))).thenReturn(testProfessor);
        
        // Act
        professorService.deactivateProfessor(professorId);
        
        // Assert
        verify(userRepository).findById(professorId);
        verify(userRepository).save(argThat(user -> !user.getIsActive()));
    }
    
    @Test
    @DisplayName("Should activate professor successfully")
    void shouldActivateProfessorSuccessfully() {
        // Arrange
        Long professorId = 1L;
        testProfessor.setIsActive(false);
        
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));
        when(userRepository.save(any(User.class))).thenReturn(testProfessor);
        
        // Act
        professorService.activateProfessor(professorId);
        
        // Assert
        verify(userRepository).findById(professorId);
        verify(userRepository).save(argThat(User::getIsActive));
    }
    
    // ==================== Generate Professor ID Tests ====================
    
    @Test
    @DisplayName("Should generate professor ID in correct format")
    void shouldGenerateProfessorIdInCorrectFormat() {
        // Arrange
        User professor = TestDataBuilder.createProfessorUser();
        professor.setId(12345L);
        
        // Act
        String professorId = professorService.generateProfessorId(professor);
        
        // Assert
        assertThat(professorId).isEqualTo("PROF12345");
    }
    
    @Test
    @DisplayName("Should throw exception when professor ID is null")
    void shouldThrowExceptionWhenProfessorIdIsNull() {
        // Arrange
        User professor = TestDataBuilder.createProfessorUser();
        professor.setId(null);
        
        // Act & Assert
        assertThatThrownBy(() -> professorService.generateProfessorId(professor))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be saved before generating professor ID");
    }
    
    // ==================== Get Professor Courses Tests ====================
    
    @Test
    @DisplayName("Should return professor courses by semester")
    void shouldReturnProfessorCoursesBySemester() {
        // Arrange
        Long professorId = 1L;
        Long semesterId = 1L;
        List<CourseAssignment> assignments = List.of(testCourseAssignment);
        
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));
        when(courseAssignmentRepository.findByProfessorIdAndSemesterId(professorId, semesterId))
                .thenReturn(assignments);
        
        // Act
        List<CourseAssignment> result = professorService.getProfessorCourses(professorId, semesterId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testCourseAssignment);
        verify(userRepository).findById(professorId);
        verify(courseAssignmentRepository).findByProfessorIdAndSemesterId(professorId, semesterId);
    }
    
    // ==================== Get Professor Courses With Status Tests ====================
    
    @Test
    @DisplayName("Should return professor courses with submission status")
    void shouldReturnProfessorCoursesWithStatus() {
        // Arrange
        Long professorId = 1L;
        Long semesterId = 1L;
        List<CourseAssignment> assignments = List.of(testCourseAssignment);
        
        RequiredDocumentType requiredDoc = new RequiredDocumentType();
        requiredDoc.setId(1L);
        requiredDoc.setCourse(testCourse);
        requiredDoc.setDocumentType(DocumentTypeEnum.SYLLABUS);
        requiredDoc.setIsRequired(true);
        requiredDoc.setDeadline(LocalDateTime.now().plusDays(30));
        requiredDoc.setMaxFileCount(1);
        requiredDoc.setMaxTotalSizeMb(10);
        
        DocumentSubmission submission = new DocumentSubmission();
        submission.setId(1L);
        submission.setCourseAssignment(testCourseAssignment);
        submission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        submission.setStatus(SubmissionStatus.UPLOADED);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setIsLateSubmission(false);
        submission.setFileCount(1);
        submission.setTotalFileSize(5000000L);
        
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));
        when(courseAssignmentRepository.findByProfessorIdAndSemesterId(professorId, semesterId))
                .thenReturn(assignments);
        when(requiredDocumentTypeRepository.findByCourseIdInAndSemesterId(anyList(), eq(semesterId)))
                .thenReturn(List.of(requiredDoc));
        when(documentSubmissionRepository.findByCourseAssignmentIdIn(anyList()))
                .thenReturn(List.of(submission));
        
        // Act
        List<CourseAssignmentWithStatus> result = professorService.getProfessorCoursesWithStatus(professorId, semesterId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourseId()).isEqualTo(testCourse.getId());
        assertThat(result.get(0).getDocumentStatuses()).isNotNull();
        assertThat(result.get(0).getDocumentStatuses()).containsKey(DocumentTypeEnum.SYLLABUS);
        
        CourseAssignmentWithStatus.DocumentTypeStatus status = 
                result.get(0).getDocumentStatuses().get(DocumentTypeEnum.SYLLABUS);
        assertThat(status.getStatus()).isEqualTo(SubmissionStatus.UPLOADED);
        assertThat(status.getSubmissionId()).isEqualTo(1L);
        
        verify(userRepository).findById(professorId);
        verify(courseAssignmentRepository).findByProfessorIdAndSemesterId(professorId, semesterId);
    }
    
    @Test
    @DisplayName("Should return empty list when no courses assigned")
    void shouldReturnEmptyListWhenNoCoursesAssigned() {
        // Arrange
        Long professorId = 1L;
        Long semesterId = 1L;
        
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));
        when(courseAssignmentRepository.findByProfessorIdAndSemesterId(professorId, semesterId))
                .thenReturn(List.of());
        
        // Act
        List<CourseAssignmentWithStatus> result = professorService.getProfessorCoursesWithStatus(professorId, semesterId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
    
    // ==================== Get Professor Dashboard Overview Tests ====================
    
    @Test
    @DisplayName("Should return professor dashboard overview with statistics")
    void shouldReturnProfessorDashboardOverview() {
        // Arrange
        Long professorId = 1L;
        Long semesterId = 1L;
        List<CourseAssignment> assignments = List.of(testCourseAssignment);
        
        RequiredDocumentType requiredDoc = new RequiredDocumentType();
        requiredDoc.setId(1L);
        requiredDoc.setCourse(testCourse);
        requiredDoc.setDocumentType(DocumentTypeEnum.SYLLABUS);
        requiredDoc.setIsRequired(true);
        requiredDoc.setDeadline(LocalDateTime.now().plusDays(30));
        requiredDoc.setMaxFileCount(1);
        requiredDoc.setMaxTotalSizeMb(10);
        
        DocumentSubmission submission = new DocumentSubmission();
        submission.setId(1L);
        submission.setCourseAssignment(testCourseAssignment);
        submission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        submission.setStatus(SubmissionStatus.UPLOADED);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setIsLateSubmission(false);
        submission.setFileCount(1);
        submission.setTotalFileSize(5000000L);
        
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));
        when(courseAssignmentRepository.findByProfessorIdAndSemesterId(professorId, semesterId))
                .thenReturn(assignments);
        when(requiredDocumentTypeRepository.findByCourseIdInAndSemesterId(anyList(), eq(semesterId)))
                .thenReturn(List.of(requiredDoc));
        when(documentSubmissionRepository.findByCourseAssignmentIdIn(anyList()))
                .thenReturn(List.of(submission));
        when(courseAssignmentRepository.findById(anyLong()))
                .thenReturn(Optional.of(testCourseAssignment));
        
        // Act
        ProfessorDashboardOverview result = professorService.getProfessorDashboardOverview(professorId, semesterId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProfessorId()).isEqualTo(professorId);
        assertThat(result.getTotalCourses()).isEqualTo(1);
        assertThat(result.getTotalRequiredDocuments()).isEqualTo(1);
        assertThat(result.getSubmittedDocuments()).isEqualTo(1);
        assertThat(result.getMissingDocuments()).isEqualTo(0);
        assertThat(result.getOverdueDocuments()).isEqualTo(0);
        assertThat(result.getCompletionPercentage()).isGreaterThan(0.0);
    }
    
    // ==================== Create Professor Folder Tests ====================
    
    @Test
    @DisplayName("Should call FolderService to create professor folder")
    void shouldCallFolderServiceToCreateProfessorFolder() {
        // Arrange
        Long professorId = 1L;
        Long academicYearId = 1L;
        Long semesterId = 1L;
        
        Folder expectedFolder = TestDataBuilder.createFolder();
        expectedFolder.setId(1L);
        
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));
        when(folderService.createProfessorFolder(professorId, academicYearId, semesterId))
                .thenReturn(expectedFolder);
        
        // Act
        Folder result = professorService.createProfessorFolder(professorId, academicYearId, semesterId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedFolder);
        verify(userRepository).findById(professorId);
        verify(folderService).createProfessorFolder(professorId, academicYearId, semesterId);
    }
    
    @Test
    @DisplayName("Should throw exception when professor not found for folder creation")
    void shouldThrowExceptionWhenProfessorNotFoundForFolderCreation() {
        // Arrange
        Long professorId = 999L;
        Long academicYearId = 1L;
        Long semesterId = 1L;
        
        when(userRepository.findById(professorId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> professorService.createProfessorFolder(professorId, academicYearId, semesterId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Professor not found");
        
        verify(userRepository).findById(professorId);
        verify(folderService, never()).createProfessorFolder(anyLong(), anyLong(), anyLong());
    }
    
    @Test
    @DisplayName("Should throw exception when user is not a professor for folder creation")
    void shouldThrowExceptionWhenUserIsNotProfessorForFolderCreation() {
        // Arrange
        Long userId = 1L;
        Long academicYearId = 1L;
        Long semesterId = 1L;
        User adminUser = TestDataBuilder.createAdminUser();
        adminUser.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));
        
        // Act & Assert
        assertThatThrownBy(() -> professorService.createProfessorFolder(userId, academicYearId, semesterId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("is not a professor");
        
        verify(userRepository).findById(userId);
        verify(folderService, never()).createProfessorFolder(anyLong(), anyLong(), anyLong());
    }
}
