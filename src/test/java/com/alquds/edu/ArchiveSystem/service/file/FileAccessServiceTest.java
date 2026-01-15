package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileAccessService Unit Tests")
class FileAccessServiceTest {

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @InjectMocks
    private FileAccessServiceImpl fileAccessService;

    private User professor1;
    private User professor2;
    private User hodUser;
    private User deanshipUser;
    private User adminUser;
    private Department department1;
    private Department department2;
    private UploadedFile file1;
    private UploadedFile file2;

    @BeforeEach
    void setUp() {
        // Setup departments
        department1 = TestDataBuilder.createDepartment();
        department1.setId(1L);
        department1.setName("Computer Science");

        department2 = TestDataBuilder.createDepartment();
        department2.setId(2L);
        department2.setName("Mathematics");

        // Setup professor1 (same department as HOD)
        professor1 = TestDataBuilder.createProfessorUser();
        professor1.setId(1L);
        professor1.setEmail("prof1@staff.alquds.edu");
        professor1.setFirstName("John");
        professor1.setLastName("Doe");
        professor1.setDepartment(department1);

        // Setup professor2 (different department)
        professor2 = TestDataBuilder.createProfessorUser();
        professor2.setId(2L);
        professor2.setEmail("prof2@staff.alquds.edu");
        professor2.setFirstName("Jane");
        professor2.setLastName("Smith");
        professor2.setDepartment(department2);

        // Setup HOD user (department1)
        hodUser = TestDataBuilder.createHodUser();
        hodUser.setId(3L);
        hodUser.setEmail("hod@hod.alquds.edu");
        hodUser.setFirstName("HOD");
        hodUser.setLastName("User");
        hodUser.setDepartment(department1);

        // Setup deanship user
        deanshipUser = TestDataBuilder.createUser();
        deanshipUser.setId(4L);
        deanshipUser.setEmail("dean@deanship.alquds.edu");
        deanshipUser.setRole(Role.ROLE_DEANSHIP);
        deanshipUser.setDepartment(null); // Deanship doesn't have a department

        // Setup admin user
        adminUser = TestDataBuilder.createAdminUser();
        adminUser.setId(5L);
        adminUser.setEmail("admin@admin.alquds.edu");
        adminUser.setDepartment(null); // Admin doesn't have a department

        // Setup files
        file1 = UploadedFile.builder()
                .id(1L)
                .originalFilename("syllabus1.pdf")
                .storedFilename("syllabus1_123.pdf")
                .fileUrl("/uploads/file1.pdf")
                .fileSize(1024L)
                .fileType("application/pdf")
                .uploader(professor1)
                .createdAt(LocalDateTime.now())
                .build();

        file2 = UploadedFile.builder()
                .id(2L)
                .originalFilename("syllabus2.pdf")
                .storedFilename("syllabus2_456.pdf")
                .fileUrl("/uploads/file2.pdf")
                .fileSize(2048L)
                .fileType("application/pdf")
                .uploader(professor2)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== canAccessFile(User, Long) Tests ====================

    @Test
    @DisplayName("Should return true when HOD can access file from same department")
    void shouldReturnTrueWhenHodCanAccessFileFromSameDepartment() {
        // Arrange
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(1L))
                .thenReturn(Optional.of(file1));

        // Act
        boolean result = fileAccessService.canAccessFile(hodUser, 1L);

        // Assert
        assertThat(result).isTrue();
        verify(uploadedFileRepository).findByIdWithUploaderAndFolder(1L);
    }

    @Test
    @DisplayName("Should return false when HOD cannot access file from different department")
    void shouldReturnFalseWhenHodCannotAccessFileFromDifferentDepartment() {
        // Arrange
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(2L))
                .thenReturn(Optional.of(file2));

        // Act
        boolean result = fileAccessService.canAccessFile(hodUser, 2L);

        // Assert
        assertThat(result).isFalse();
        verify(uploadedFileRepository).findByIdWithUploaderAndFolder(2L);
    }

    @Test
    @DisplayName("Should return true when deanship user can access any file")
    void shouldReturnTrueWhenDeanshipUserCanAccessAnyFile() {
        // Act
        boolean result = fileAccessService.canAccessFile(deanshipUser, 1L);

        // Assert
        assertThat(result).isTrue();
        // Should return early without checking file details
        verify(uploadedFileRepository, never()).findByIdWithUploaderAndFolder(anyLong());
    }

    @Test
    @DisplayName("Should return true when admin user can access any file")
    void shouldReturnTrueWhenAdminUserCanAccessAnyFile() {
        // Act
        boolean result = fileAccessService.canAccessFile(adminUser, 1L);

        // Assert
        assertThat(result).isTrue();
        // Should return early without checking file details
        verify(uploadedFileRepository, never()).findByIdWithUploaderAndFolder(anyLong());
    }

    @Test
    @DisplayName("Should return true when professor can access own file")
    void shouldReturnTrueWhenProfessorCanAccessOwnFile() {
        // Arrange
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(1L))
                .thenReturn(Optional.of(file1));

        // Act
        boolean result = fileAccessService.canAccessFile(professor1, 1L);

        // Assert
        assertThat(result).isTrue();
        verify(uploadedFileRepository).findByIdWithUploaderAndFolder(1L);
    }

    @Test
    @DisplayName("Should return false when professor cannot access other professor's file")
    void shouldReturnFalseWhenProfessorCannotAccessOtherProfessorsFile() {
        // Arrange
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(2L))
                .thenReturn(Optional.of(file2));

        // Act
        boolean result = fileAccessService.canAccessFile(professor1, 2L);

        // Assert
        assertThat(result).isFalse();
        verify(uploadedFileRepository).findByIdWithUploaderAndFolder(2L);
    }

    @Test
    @DisplayName("Should return false when file not found")
    void shouldReturnFalseWhenFileNotFound() {
        // Arrange
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(999L))
                .thenReturn(Optional.empty());

        // Act
        boolean result = fileAccessService.canAccessFile(professor1, 999L);

        // Assert
        assertThat(result).isFalse();
        verify(uploadedFileRepository).findByIdWithUploaderAndFolder(999L);
    }

    @Test
    @DisplayName("Should return false when user is null")
    void shouldReturnFalseWhenUserIsNull() {
        // Act
        boolean result = fileAccessService.canAccessFile(null, 1L);

        // Assert
        assertThat(result).isFalse();
        verify(uploadedFileRepository, never()).findByIdWithUploaderAndFolder(anyLong());
    }

    @Test
    @DisplayName("Should return false when fileId is null")
    void shouldReturnFalseWhenFileIdIsNull() {
        // Act
        Long nullFileId = null;
        boolean result = fileAccessService.canAccessFile(professor1, nullFileId);

        // Assert
        assertThat(result).isFalse();
        verify(uploadedFileRepository, never()).findByIdWithUploaderAndFolder(anyLong());
    }

    // ==================== canAccessFile(User, UploadedFile) Tests ====================

    @Test
    @DisplayName("Should return true when HOD can access file from same department (file entity)")
    void shouldReturnTrueWhenHodCanAccessFileFromSameDepartmentFileEntity() {
        // Act
        boolean result = fileAccessService.canAccessFile(hodUser, file1);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when HOD cannot access file from different department (file entity)")
    void shouldReturnFalseWhenHodCannotAccessFileFromDifferentDepartmentFileEntity() {
        // Act
        boolean result = fileAccessService.canAccessFile(hodUser, file2);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when deanship user can access any file (file entity)")
    void shouldReturnTrueWhenDeanshipUserCanAccessAnyFileEntity() {
        // Act
        boolean result = fileAccessService.canAccessFile(deanshipUser, file1);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true when professor can access own file (file entity)")
    void shouldReturnTrueWhenProfessorCanAccessOwnFileEntity() {
        // Act
        boolean result = fileAccessService.canAccessFile(professor1, file1);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when professor cannot access other professor's file (file entity)")
    void shouldReturnFalseWhenProfessorCannotAccessOtherProfessorsFileEntity() {
        // Act
        boolean result = fileAccessService.canAccessFile(professor1, file2);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when file has no uploader")
    void shouldReturnFalseWhenFileHasNoUploader() {
        // Arrange
        UploadedFile fileWithoutUploader = UploadedFile.builder()
                .id(3L)
                .originalFilename("test.pdf")
                .uploader(null)
                .build();

        // Act
        boolean result = fileAccessService.canAccessFile(professor1, fileWithoutUploader);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when user is null (file entity)")
    void shouldReturnFalseWhenUserIsNullFileEntity() {
        // Act
        boolean result = fileAccessService.canAccessFile(null, file1);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when file is null")
    void shouldReturnFalseWhenFileIsNull() {
        // Act
        UploadedFile nullFile = null;
        boolean result = fileAccessService.canAccessFile(professor1, nullFile);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when HOD has no department")
    void shouldReturnFalseWhenHodHasNoDepartment() {
        // Arrange
        User hodWithoutDept = TestDataBuilder.createHodUser();
        hodWithoutDept.setId(6L);
        hodWithoutDept.setDepartment(null);

        // Act
        boolean result = fileAccessService.canAccessFile(hodWithoutDept, file1);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when uploader has no department")
    void shouldReturnFalseWhenUploaderHasNoDepartment() {
        // Arrange
        User uploaderWithoutDept = TestDataBuilder.createProfessorUser();
        uploaderWithoutDept.setId(7L);
        uploaderWithoutDept.setDepartment(null);

        UploadedFile fileWithUploaderNoDept = UploadedFile.builder()
                .id(4L)
                .originalFilename("test.pdf")
                .uploader(uploaderWithoutDept)
                .build();

        // Act
        boolean result = fileAccessService.canAccessFile(hodUser, fileWithUploaderNoDept);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== getAccessibleFiles Tests ====================

    @Test
    @DisplayName("Should return all files when admin user requests accessible files")
    void shouldReturnAllFilesWhenAdminUserRequestsAccessibleFiles() {
        // Arrange
        List<UploadedFile> allFiles = new ArrayList<>();
        allFiles.add(file1);
        allFiles.add(file2);
        when(uploadedFileRepository.findAll()).thenReturn(allFiles);

        // Act
        List<UploadedFile> result = fileAccessService.getAccessibleFiles(adminUser);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(file1, file2);
        verify(uploadedFileRepository).findAll();
    }

    @Test
    @DisplayName("Should return all files when deanship user requests accessible files")
    void shouldReturnAllFilesWhenDeanshipUserRequestsAccessibleFiles() {
        // Arrange
        List<UploadedFile> allFiles = new ArrayList<>();
        allFiles.add(file1);
        allFiles.add(file2);
        when(uploadedFileRepository.findAll()).thenReturn(allFiles);

        // Act
        List<UploadedFile> result = fileAccessService.getAccessibleFiles(deanshipUser);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(file1, file2);
        verify(uploadedFileRepository).findAll();
    }

    @Test
    @DisplayName("Should return department files when HOD requests accessible files")
    void shouldReturnDepartmentFilesWhenHodRequestsAccessibleFiles() {
        // Arrange
        List<UploadedFile> allFiles = new ArrayList<>();
        allFiles.add(file1); // Same department
        allFiles.add(file2); // Different department
        when(uploadedFileRepository.findAll()).thenReturn(allFiles);

        // Act
        List<UploadedFile> result = fileAccessService.getAccessibleFiles(hodUser);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(file1);
        verify(uploadedFileRepository).findAll();
    }

    @Test
    @DisplayName("Should return own files when professor requests accessible files")
    void shouldReturnOwnFilesWhenProfessorRequestsAccessibleFiles() {
        // Arrange
        List<UploadedFile> professorFiles = new ArrayList<>();
        professorFiles.add(file1);
        when(uploadedFileRepository.findByUploaderId(professor1.getId()))
                .thenReturn(professorFiles);

        // Act
        List<UploadedFile> result = fileAccessService.getAccessibleFiles(professor1);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(file1);
        verify(uploadedFileRepository).findByUploaderId(professor1.getId());
    }

    @Test
    @DisplayName("Should return empty list when HOD has no department")
    void shouldReturnEmptyListWhenHodHasNoDepartment() {
        // Arrange
        User hodWithoutDept = TestDataBuilder.createHodUser();
        hodWithoutDept.setId(6L);
        hodWithoutDept.setDepartment(null);

        // Act
        List<UploadedFile> result = fileAccessService.getAccessibleFiles(hodWithoutDept);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when user is null")
    void shouldReturnEmptyListWhenUserIsNull() {
        // Act
        List<UploadedFile> result = fileAccessService.getAccessibleFiles(null);

        // Assert
        assertThat(result).isEmpty();
        verify(uploadedFileRepository, never()).findAll();
        verify(uploadedFileRepository, never()).findByUploaderId(anyLong());
    }

    // ==================== getAccessibleFilesByDepartment Tests ====================

    @Test
    @DisplayName("Should return all files when admin filters by null department")
    void shouldReturnAllFilesWhenAdminFiltersByNullDepartment() {
        // Arrange
        List<UploadedFile> allFiles = new ArrayList<>();
        allFiles.add(file1);
        allFiles.add(file2);
        when(uploadedFileRepository.findAll()).thenReturn(allFiles);

        // Act
        List<UploadedFile> result = fileAccessService.getAccessibleFilesByDepartment(adminUser, null);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(file1, file2);
        verify(uploadedFileRepository).findAll();
    }

    @Test
    @DisplayName("Should return filtered files when admin filters by department")
    void shouldReturnFilteredFilesWhenAdminFiltersByDepartment() {
        // Arrange
        List<UploadedFile> allFiles = new ArrayList<>();
        allFiles.add(file1); // Department 1
        allFiles.add(file2); // Department 2
        when(uploadedFileRepository.findAll()).thenReturn(allFiles);

        // Act
        List<UploadedFile> result = fileAccessService.getAccessibleFilesByDepartment(adminUser, 1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(file1);
        verify(uploadedFileRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when non-admin filters by department")
    void shouldReturnEmptyListWhenNonAdminFiltersByDepartment() {
        // Act
        List<UploadedFile> result = fileAccessService.getAccessibleFilesByDepartment(professor1, 1L);

        // Assert
        assertThat(result).isEmpty();
        verify(uploadedFileRepository, never()).findAll();
    }

    @Test
    @DisplayName("Should return empty list when user is null")
    void shouldReturnEmptyListWhenUserIsNullForDepartmentFilter() {
        // Act
        List<UploadedFile> result = fileAccessService.getAccessibleFilesByDepartment(null, 1L);

        // Assert
        assertThat(result).isEmpty();
    }

    // ==================== hasAdminLevelAccess Tests ====================

    @Test
    @DisplayName("Should return true when user is admin")
    void shouldReturnTrueWhenUserIsAdmin() {
        // Act
        boolean result = fileAccessService.hasAdminLevelAccess(adminUser);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true when user is deanship")
    void shouldReturnTrueWhenUserIsDeanship() {
        // Act
        boolean result = fileAccessService.hasAdminLevelAccess(deanshipUser);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when user is professor")
    void shouldReturnFalseWhenUserIsProfessor() {
        // Act
        boolean result = fileAccessService.hasAdminLevelAccess(professor1);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when user is HOD")
    void shouldReturnFalseWhenUserIsHod() {
        // Act
        boolean result = fileAccessService.hasAdminLevelAccess(hodUser);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when user is null")
    void shouldReturnFalseWhenUserIsNullForAdminLevelAccess() {
        // Act
        boolean result = fileAccessService.hasAdminLevelAccess(null);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when user has null role")
    void shouldReturnFalseWhenUserHasNullRole() {
        // Arrange
        User userWithNullRole = TestDataBuilder.createUser();
        userWithNullRole.setRole(null);

        // Act
        boolean result = fileAccessService.hasAdminLevelAccess(userWithNullRole);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== canAccessDepartmentFiles Tests ====================

    @Test
    @DisplayName("Should return true when admin can access any department")
    void shouldReturnTrueWhenAdminCanAccessAnyDepartment() {
        // Act
        boolean result = fileAccessService.canAccessDepartmentFiles(adminUser, 1L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true when deanship can access any department")
    void shouldReturnTrueWhenDeanshipCanAccessAnyDepartment() {
        // Act
        boolean result = fileAccessService.canAccessDepartmentFiles(deanshipUser, 1L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true when HOD can access own department")
    void shouldReturnTrueWhenHodCanAccessOwnDepartment() {
        // Act
        boolean result = fileAccessService.canAccessDepartmentFiles(hodUser, 1L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when HOD cannot access other department")
    void shouldReturnFalseWhenHodCannotAccessOtherDepartment() {
        // Act
        boolean result = fileAccessService.canAccessDepartmentFiles(hodUser, 2L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when professor can access own department")
    void shouldReturnTrueWhenProfessorCanAccessOwnDepartment() {
        // Act
        boolean result = fileAccessService.canAccessDepartmentFiles(professor1, 1L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when professor cannot access other department")
    void shouldReturnFalseWhenProfessorCannotAccessOtherDepartment() {
        // Act
        boolean result = fileAccessService.canAccessDepartmentFiles(professor1, 2L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when user is null")
    void shouldReturnFalseWhenUserIsNullForDepartmentAccess() {
        // Act
        boolean result = fileAccessService.canAccessDepartmentFiles(null, 1L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when departmentId is null")
    void shouldReturnFalseWhenDepartmentIdIsNull() {
        // Act
        boolean result = fileAccessService.canAccessDepartmentFiles(professor1, null);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when HOD has no department")
    void shouldReturnFalseWhenHodHasNoDepartmentForDepartmentAccess() {
        // Arrange
        User hodWithoutDept = TestDataBuilder.createHodUser();
        hodWithoutDept.setId(6L);
        hodWithoutDept.setDepartment(null);

        // Act
        boolean result = fileAccessService.canAccessDepartmentFiles(hodWithoutDept, 1L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when professor has no department")
    void shouldReturnFalseWhenProfessorHasNoDepartmentForDepartmentAccess() {
        // Arrange
        User profWithoutDept = TestDataBuilder.createProfessorUser();
        profWithoutDept.setId(7L);
        profWithoutDept.setDepartment(null);

        // Act
        boolean result = fileAccessService.canAccessDepartmentFiles(profWithoutDept, 1L);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== logAccessDenial Tests ====================

    @Test
    @DisplayName("Should log access denial successfully")
    void shouldLogAccessDenialSuccessfully() {
        // Act - This method logs, so we just verify it doesn't throw
        fileAccessService.logAccessDenial(professor1, 1L, "Unauthorized access attempt");

        // Assert - No exception thrown means success
        // In a real scenario, you might want to verify logging was called
    }

    @Test
    @DisplayName("Should log access denial with null user")
    void shouldLogAccessDenialWithNullUser() {
        // Act - Should handle null user gracefully
        fileAccessService.logAccessDenial(null, 1L, "User is null");

        // Assert - No exception thrown means success
    }
}
