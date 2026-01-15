package com.alquds.edu.ArchiveSystem.service.user;

import com.alquds.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alquds.edu.ArchiveSystem.dto.user.UserResponse;
import com.alquds.edu.ArchiveSystem.dto.user.UserUpdateRequest;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.exception.domain.UserException;
import com.alquds.edu.ArchiveSystem.mapper.user.UserMapper;
import com.alquds.edu.ArchiveSystem.repository.academic.DepartmentRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.auth.RefreshTokenRepository;
import com.alquds.edu.ArchiveSystem.repository.file.FolderRepository;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentRequestRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.repository.user.NotificationRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.service.auth.EmailValidationService;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for UserService following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - 70-80% unit tests (this file)
 * - Mock all external dependencies (repositories, services)
 * - Test business logic in isolation
 * - Follow AAA pattern (Arrange, Act, Assert)
 * - Test edge cases and error conditions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DepartmentRepository departmentRepository;
    
    @Mock
    private DocumentRequestRepository documentRequestRepository;
    
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private FolderRepository folderRepository;
    
    @Mock
    private UploadedFileRepository uploadedFileRepository;
    
    @Mock
    private CourseAssignmentRepository courseAssignmentRepository;
    
    @Mock
    private DocumentSubmissionRepository documentSubmissionRepository;
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private EmailValidationService emailValidationService;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    private Department testDepartment;
    private UserCreateRequest userCreateRequest;
    
    @BeforeEach
    void setUp() {
        // Arrange: Set up test data
        testDepartment = TestDataBuilder.createDepartment();
        testDepartment.setId(1L);
        
        testUser = TestDataBuilder.createProfessorUser();
        testUser.setId(1L);
        testUser.setDepartment(testDepartment);
        
        userCreateRequest = TestDataBuilder.createUserCreateRequest();
        userCreateRequest.setDepartmentId(1L);
        
        // Mock security context (lenient to avoid unnecessary stubbing errors)
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("admin@admin.alquds.edu");
    }
    
    // ==================== Create User Tests ====================
    
    @Test
    @DisplayName("Should create user successfully when all validations pass")
    void shouldCreateUserSuccessfully() {
        // Arrange
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setDepartmentId(1L);
        
        User savedUser = TestDataBuilder.createProfessorUser();
        savedUser.setId(1L);
        savedUser.setDepartment(testDepartment);
        
        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(1L);
        expectedResponse.setEmail(request.getEmail());
        
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(userMapper.toEntity(request)).thenReturn(savedUser);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);
        doNothing().when(emailValidationService).validateProfessorEmail(anyString());
        
        // Act
        UserResponse result = userService.createUser(request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo(request.getEmail());
        
        verify(userRepository).existsByEmail(request.getEmail());
        verify(departmentRepository).findById(1L);
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
        verify(userMapper).toResponse(savedUser);
    }
    
    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Arrange
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setDepartmentId(1L);
        
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("already exists");
        
        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when department not found for HOD role")
    void shouldThrowExceptionWhenDepartmentNotFoundForHod() {
        // Arrange
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setRole(Role.ROLE_HOD);
        request.setDepartmentId(999L);
        
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Department not found");
        
        verify(departmentRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when department is null for Professor role")
    void shouldThrowExceptionWhenDepartmentNullForProfessor() {
        // Arrange
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setRole(Role.ROLE_PROFESSOR);
        request.setDepartmentId(null);
        
        // No need to mock existsByEmail since validation happens before that check
        
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(Exception.class); // Can be ValidationException or IllegalArgumentException
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should ignore department for Admin role")
    void shouldIgnoreDepartmentForAdminRole() {
        // Arrange
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setEmail("admin@admin.alquds.edu");
        request.setRole(Role.ROLE_ADMIN);
        request.setDepartmentId(1L); // Provided but should be ignored
        
        User savedUser = TestDataBuilder.createAdminUser();
        savedUser.setId(1L);
        savedUser.setDepartment(null);
        
        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(1L);
        
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(savedUser);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);
        doNothing().when(emailValidationService).validateAdminEmail(anyString());
        // Mock current user as admin for role validation
        User adminUser = TestDataBuilder.createAdminUser();
        adminUser.setId(999L);
        when(userRepository.findByEmail("admin@admin.alquds.edu")).thenReturn(Optional.of(adminUser));
        
        // Act
        UserResponse result = userService.createUser(request);
        
        // Assert
        assertThat(result).isNotNull();
        verify(departmentRepository, never()).findById(any());
        verify(userRepository).save(argThat(user -> user.getDepartment() == null));
    }
    
    @Test
    @DisplayName("Should throw exception when password is weak")
    void shouldThrowExceptionWhenPasswordIsWeak() {
        // Arrange
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setPassword("weak");
        request.setDepartmentId(1L);
        
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Password");
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    // ==================== Update User Tests ====================
    
    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUserSuccessfully() {
        // Arrange
        Long userId = 1L;
        UserUpdateRequest updateRequest = TestDataBuilder.createUserUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        
        User existingUser = TestDataBuilder.createProfessorUser();
        existingUser.setId(userId);
        existingUser.setDepartment(testDepartment);
        
        User updatedUser = TestDataBuilder.createProfessorUser();
        updatedUser.setId(userId);
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("Name");
        updatedUser.setDepartment(testDepartment);
        
        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(userId);
        expectedResponse.setFirstName("Updated");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toResponse(updatedUser)).thenReturn(expectedResponse);
        
        // Act
        UserResponse result = userService.updateUser(userId, updateRequest);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        verify(userRepository).findById(userId);
        verify(userMapper).updateEntity(updateRequest, existingUser);
        verify(userRepository).save(existingUser);
    }
    
    @Test
    @DisplayName("Should throw exception when user not found for update")
    void shouldThrowExceptionWhenUserNotFoundForUpdate() {
        // Arrange
        Long userId = 999L;
        UserUpdateRequest updateRequest = TestDataBuilder.createUserUpdateRequest();
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(userId, updateRequest))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("not found");
        
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when email already exists during update")
    void shouldThrowExceptionWhenEmailExistsDuringUpdate() {
        // Arrange
        Long userId = 1L;
        User existingUser = TestDataBuilder.createProfessorUser();
        existingUser.setId(userId);
        existingUser.setEmail("old@staff.alquds.edu");
        
        UserUpdateRequest updateRequest = TestDataBuilder.createUserUpdateRequest();
        updateRequest.setEmail("new@staff.alquds.edu");
        
        User otherUser = TestDataBuilder.createProfessorUser();
        otherUser.setId(2L);
        otherUser.setEmail("new@staff.alquds.edu");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("new@staff.alquds.edu")).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(userId, updateRequest))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("already exists");
        
        verify(userRepository).existsByEmail("new@staff.alquds.edu");
        verify(userRepository, never()).save(any(User.class));
    }
    
    // ==================== Delete User Tests ====================
    
    @Test
    @DisplayName("Should delete user successfully when no dependencies exist")
    void shouldDeleteUserSuccessfully() {
        // Arrange
        Long userId = 1L;
        User userToDelete = TestDataBuilder.createProfessorUser();
        userToDelete.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(userToDelete));
        when(documentRequestRepository.countByCreatedBy_Id(userId)).thenReturn(0L);
        when(documentRequestRepository.countByProfessor_Id(userId)).thenReturn(0L);
        User admin = TestDataBuilder.createAdminUser();
        admin.setId(999L); // Different ID to avoid self-deletion check
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        doNothing().when(refreshTokenRepository).deleteAllByUserId(userId);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Collections.emptyList());
        when(folderRepository.findByOwnerId(userId)).thenReturn(Collections.emptyList());
        
        // Act
        userService.deleteUser(userId);
        
        // Assert
        verify(userRepository).findById(userId);
        verify(documentRequestRepository).countByCreatedBy_Id(userId);
        verify(documentRequestRepository).countByProfessor_Id(userId);
        verify(refreshTokenRepository).deleteAllByUserId(userId);
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(userId);
        verify(folderRepository).findByOwnerId(userId);
        verify(userRepository).delete(userToDelete);
    }
    
    @Test
    @DisplayName("Should throw exception when user not found for deletion")
    void shouldThrowExceptionWhenUserNotFoundForDeletion() {
        // Arrange
        Long userId = 999L;
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("not found");
        
        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when user tries to delete themselves")
    void shouldThrowExceptionWhenUserDeletesSelf() {
        // Arrange
        Long userId = 1L;
        User userToDelete = TestDataBuilder.createProfessorUser();
        userToDelete.setId(userId);
        userToDelete.setEmail("test@staff.alquds.edu");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(userToDelete));
        // Mock authentication to return the same user trying to delete themselves
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@staff.alquds.edu");
        when(userRepository.findByEmail("test@staff.alquds.edu")).thenReturn(Optional.of(userToDelete));
        
        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("delete your own account");
        
        verify(userRepository, never()).delete(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when user has dependencies")
    void shouldThrowExceptionWhenUserHasDependencies() {
        // Arrange
        Long userId = 1L;
        User userToDelete = TestDataBuilder.createProfessorUser();
        userToDelete.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(userToDelete));
        when(documentRequestRepository.countByCreatedBy_Id(userId)).thenReturn(5L);
        User admin = TestDataBuilder.createAdminUser();
        admin.setId(999L);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        
        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("existing");
        
        verify(userRepository, never()).delete(any(User.class));
    }
    
    // ==================== Get User Tests ====================
    
    @Test
    @DisplayName("Should retrieve user by ID successfully")
    void shouldRetrieveUserByIdSuccessfully() {
        // Arrange
        Long userId = 1L;
        User user = TestDataBuilder.createProfessorUser();
        user.setId(userId);
        
        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);
        
        // Act
        UserResponse result = userService.getUserById(userId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        verify(userRepository).findById(userId);
        verify(userMapper).toResponse(user);
    }
    
    @Test
    @DisplayName("Should throw exception when user ID is null")
    void shouldThrowExceptionWhenUserIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    @DisplayName("Should handle null email in create request")
    void shouldHandleNullEmail() {
        // Arrange
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setEmail(null);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(Exception.class);
    }
    
    @Test
    @DisplayName("Should handle empty first name in create request")
    void shouldHandleEmptyFirstName() {
        // Arrange
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setFirstName("");
        request.setDepartmentId(1L);
        
        lenient().when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(Exception.class);
    }
    
    @Test
    @DisplayName("Should validate email format for Professor role")
    void shouldValidateEmailFormatForProfessor() {
        // Arrange
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setEmail("invalid-email");
        request.setDepartmentId(1L);
        
        lenient().when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(Exception.class);
    }
}
