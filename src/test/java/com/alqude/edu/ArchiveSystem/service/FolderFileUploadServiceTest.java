package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.exception.*;
import com.alqude.edu.ArchiveSystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FolderFileUploadService.
 * Tests file upload, validation, sanitization, and authorization logic.
 */
@ExtendWith(MockitoExtension.class)
class FolderFileUploadServiceTest {

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FolderFileUploadServiceImpl fileUploadService;

    @TempDir
    Path tempDir;

    private User professor;
    private User dean;
    private User hod;
    private Folder folder;
    private Department department;

    @BeforeEach
    void setUp() {
        // Set configuration properties
        ReflectionTestUtils.setField(fileUploadService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileUploadService, "maxFileSize", 52428800L); // 50MB
        ReflectionTestUtils.setField(fileUploadService, "allowedTypes", "pdf,doc,docx,ppt,pptx,xls,xlsx,jpg,jpeg,png,gif");

        // Setup department
        department = new Department();
        department.setId(1L);
        department.setName("Computer Science");

        // Setup professor
        professor = new User();
        professor.setId(1L);
        professor.setProfessorId("PROF123");
        professor.setFirstName("John");
        professor.setLastName("Doe");
        professor.setEmail("john.doe@example.com");
        professor.setRole(Role.ROLE_PROFESSOR);
        professor.setDepartment(department);

        // Setup dean
        dean = new User();
        dean.setId(2L);
        dean.setFirstName("Jane");
        dean.setLastName("Smith");
        dean.setEmail("jane.smith@example.com");
        dean.setRole(Role.ROLE_DEANSHIP);
        dean.setDepartment(department);

        // Setup HOD
        hod = new User();
        hod.setId(3L);
        hod.setFirstName("Bob");
        hod.setLastName("Johnson");
        hod.setEmail("bob.johnson@example.com");
        hod.setRole(Role.ROLE_HOD);
        hod.setDepartment(department);

        // Setup folder
        folder = Folder.builder()
                .id(1L)
                .path("2024-2025/first/PROF123/CS101/Syllabus")
                .name("Syllabus")
                .type(FolderType.SUBFOLDER)
                .owner(professor)
                .build();
    }

    // ========== uploadFiles() Tests ==========

    @Test
    void testUploadFiles_Success_SingleFile() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());
        MultipartFile[] files = {file};

        when(folderRepository.findById(1L)).thenReturn(Optional.of(folder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(uploadedFileRepository.save(any(UploadedFile.class))).thenAnswer(invocation -> {
            UploadedFile uf = invocation.getArgument(0);
            uf.setId(1L);
            return uf;
        });

        // Act
        List<UploadedFile> result = fileUploadService.uploadFiles(files, 1L, null, "Test notes", 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test.pdf", result.get(0).getOriginalFilename());
        assertEquals("test.pdf", result.get(0).getStoredFilename());
        assertEquals("Test notes", result.get(0).getNotes());
        assertEquals(professor, result.get(0).getUploader());
        assertEquals(folder, result.get(0).getFolder());

        verify(folderRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(uploadedFileRepository).save(any(UploadedFile.class));

        // Verify physical file was created
        Path uploadedFile = tempDir.resolve(folder.getPath()).resolve("test.pdf");
        assertTrue(Files.exists(uploadedFile));
    }

    @Test
    void testUploadFiles_Success_MultipleFiles() throws IOException {
        // Arrange
        MockMultipartFile file1 = new MockMultipartFile(
                "file1", "document1.pdf", "application/pdf", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file2", "document2.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 
                "content2".getBytes());
        MultipartFile[] files = {file1, file2};

        when(folderRepository.findById(1L)).thenReturn(Optional.of(folder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(uploadedFileRepository.save(any(UploadedFile.class))).thenAnswer(invocation -> {
            UploadedFile uf = invocation.getArgument(0);
            uf.setId(System.currentTimeMillis());
            return uf;
        });

        // Act
        List<UploadedFile> result = fileUploadService.uploadFiles(files, 1L, null, null, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("document1.pdf", result.get(0).getOriginalFilename());
        assertEquals("document2.docx", result.get(1).getOriginalFilename());

        verify(uploadedFileRepository, times(2)).save(any(UploadedFile.class));

        // Verify physical files were created
        assertTrue(Files.exists(tempDir.resolve(folder.getPath()).resolve("document1.pdf")));
        assertTrue(Files.exists(tempDir.resolve(folder.getPath()).resolve("document2.docx")));
    }

    @Test
    void testUploadFiles_Error_NoFilesProvided() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            fileUploadService.uploadFiles(null, 1L, null, null, 1L);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            fileUploadService.uploadFiles(new MultipartFile[0], 1L, null, null, 1L);
        });

        verify(folderRepository, never()).findById(anyLong());
        verify(uploadedFileRepository, never()).save(any(UploadedFile.class));
    }

    @Test
    void testUploadFiles_Error_FolderNotFound() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());
        MultipartFile[] files = {file};

        when(folderRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FolderNotFoundException.class, () -> {
            fileUploadService.uploadFiles(files, 1L, null, null, 1L);
        });

        verify(folderRepository).findById(1L);
        verify(userRepository, never()).findById(anyLong());
        verify(uploadedFileRepository, never()).save(any(UploadedFile.class));
    }

    @Test
    void testUploadFiles_Error_UserNotAuthorized() {
        // Arrange
        User otherProfessor = new User();
        otherProfessor.setId(99L);
        otherProfessor.setRole(Role.ROLE_PROFESSOR);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());
        MultipartFile[] files = {file};

        when(folderRepository.findById(1L)).thenReturn(Optional.of(folder));
        when(userRepository.findById(99L)).thenReturn(Optional.of(otherProfessor));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            fileUploadService.uploadFiles(files, 1L, null, null, 99L);
        });

        verify(folderRepository).findById(1L);
        verify(userRepository).findById(99L);
        verify(uploadedFileRepository, never()).save(any(UploadedFile.class));
    }

    // ========== validateFile() Tests ==========

    @Test
    void testValidateFile_Success() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> fileUploadService.validateFile(file));
    }

    @Test
    void testValidateFile_Error_EmptyFile() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[0]);

        // Act & Assert
        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            fileUploadService.validateFile(file);
        });

        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void testValidateFile_Error_FileTooLarge() {
        // Arrange
        byte[] largeContent = new byte[52428801]; // 50MB + 1 byte
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent);

        // Act & Assert
        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            fileUploadService.validateFile(file);
        });

        assertTrue(exception.getMessage().contains("exceeds maximum allowed size"));
    }

    @Test
    void testValidateFile_Error_InvalidFileType() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.exe", "application/x-msdownload", "test content".getBytes());

        // Act & Assert
        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            fileUploadService.validateFile(file);
        });

        assertTrue(exception.getMessage().contains("not allowed"));
        assertTrue(exception.getMessage().contains("exe"));
    }

    @Test
    void testValidateFile_Error_EmptyFilename() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "", "application/pdf", "test content".getBytes());

        // Act & Assert
        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            fileUploadService.validateFile(file);
        });

        assertTrue(exception.getMessage().contains("Filename is empty or invalid"));
    }

    @Test
    void testValidateFile_Error_NullFilename() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "application/pdf", "test content".getBytes());

        // Act & Assert
        FileValidationException exception = assertThrows(FileValidationException.class, () -> {
            fileUploadService.validateFile(file);
        });

        assertTrue(exception.getMessage().contains("Filename is empty or invalid"));
    }

    // ========== generateSafeFilename() Tests ==========

    @Test
    void testGenerateSafeFilename_Sanitization() throws IOException {
        // Arrange
        Path targetPath = tempDir.resolve("test");
        Files.createDirectories(targetPath);

        // Act
        String result = fileUploadService.generateSafeFilename("test file!@#$%^&*().pdf", targetPath);

        // Assert
        // Verify special characters are replaced with underscores
        assertTrue(result.startsWith("test_file"));
        assertTrue(result.endsWith(".pdf"));
        assertFalse(result.contains("!"));
        assertFalse(result.contains("@"));
        assertFalse(result.contains("#"));
    }

    @Test
    void testGenerateSafeFilename_DuplicateHandling() throws IOException {
        // Arrange
        Path targetPath = tempDir.resolve("test");
        Files.createDirectories(targetPath);
        
        // Create existing file
        Files.createFile(targetPath.resolve("document.pdf"));

        // Act
        String result = fileUploadService.generateSafeFilename("document.pdf", targetPath);

        // Assert
        assertEquals("document(1).pdf", result);
    }

    @Test
    void testGenerateSafeFilename_MultipleDuplicates() throws IOException {
        // Arrange
        Path targetPath = tempDir.resolve("test");
        Files.createDirectories(targetPath);
        
        // Create existing files
        Files.createFile(targetPath.resolve("document.pdf"));
        Files.createFile(targetPath.resolve("document(1).pdf"));
        Files.createFile(targetPath.resolve("document(2).pdf"));

        // Act
        String result = fileUploadService.generateSafeFilename("document.pdf", targetPath);

        // Assert
        assertEquals("document(3).pdf", result);
    }

    @Test
    void testGenerateSafeFilename_NoExtension() throws IOException {
        // Arrange
        Path targetPath = tempDir.resolve("test");
        Files.createDirectories(targetPath);

        // Act
        String result = fileUploadService.generateSafeFilename("README", targetPath);

        // Assert
        assertEquals("README", result);
    }

    // ========== canUploadToFolder() Tests ==========

    @Test
    void testCanUploadToFolder_Professor_OwnFolder() {
        // Act
        boolean result = fileUploadService.canUploadToFolder(folder, professor);

        // Assert
        assertTrue(result);
    }

    @Test
    void testCanUploadToFolder_Professor_OtherFolder() {
        // Arrange
        User otherProfessor = new User();
        otherProfessor.setId(99L);
        otherProfessor.setRole(Role.ROLE_PROFESSOR);

        // Act
        boolean result = fileUploadService.canUploadToFolder(folder, otherProfessor);

        // Assert
        assertFalse(result);
    }

    @Test
    void testCanUploadToFolder_Dean() {
        // Act
        boolean result = fileUploadService.canUploadToFolder(folder, dean);

        // Assert
        assertTrue(result);
    }

    @Test
    void testCanUploadToFolder_HOD_OwnFolder() {
        // Arrange
        Folder hodFolder = Folder.builder()
                .id(2L)
                .path("2024-2025/first/HOD001/CS101/Syllabus")
                .name("Syllabus")
                .type(FolderType.SUBFOLDER)
                .owner(hod)
                .build();

        // Act
        boolean result = fileUploadService.canUploadToFolder(hodFolder, hod);

        // Assert
        assertTrue(result);
    }

    @Test
    void testCanUploadToFolder_HOD_OtherFolder() {
        // Act
        boolean result = fileUploadService.canUploadToFolder(folder, hod);

        // Assert
        assertFalse(result);
    }

    @Test
    void testCanUploadToFolder_FolderWithoutOwner() {
        // Arrange
        Folder folderWithoutOwner = Folder.builder()
                .id(3L)
                .path("shared/folder")
                .name("Shared")
                .type(FolderType.SUBFOLDER)
                .owner(null)
                .build();

        // Act
        boolean professorResult = fileUploadService.canUploadToFolder(folderWithoutOwner, professor);
        boolean deanResult = fileUploadService.canUploadToFolder(folderWithoutOwner, dean);

        // Assert
        assertFalse(professorResult);
        assertTrue(deanResult); // Dean can still upload
    }
    
    @Test
    void testUploadFiles_WithFolderId() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());
        MultipartFile[] files = {file};
        
        when(folderRepository.findById(1L)).thenReturn(Optional.of(folder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(uploadedFileRepository.save(any(UploadedFile.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        List<UploadedFile> result = fileUploadService.uploadFiles(files, 1L, null, "Test notes", 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(folderRepository).findById(1L);
        verify(uploadedFileRepository).save(any(UploadedFile.class));
    }
    
    @Test
    void testUploadFiles_WithFolderPath() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());
        MultipartFile[] files = {file};
        String folderPath = "/2024-2025/first/PROF123/CS101/lecture_notes";
        
        // Mock FolderService dependency
        FolderService folderService = mock(FolderService.class);
        ReflectionTestUtils.setField(fileUploadService, "folderService", folderService);
        
        when(folderService.getOrCreateFolderByPath(folderPath, 1L)).thenReturn(folder);
        when(folderRepository.findById(1L)).thenReturn(Optional.of(folder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(uploadedFileRepository.save(any(UploadedFile.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        List<UploadedFile> result = fileUploadService.uploadFiles(files, null, folderPath, "Test notes", 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(folderService).getOrCreateFolderByPath(folderPath, 1L);
        verify(folderRepository).findById(1L);
        verify(uploadedFileRepository).save(any(UploadedFile.class));
    }
    
    @Test
    void testUploadFiles_WithoutFolderIdOrPath() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());
        MultipartFile[] files = {file};

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> fileUploadService.uploadFiles(files, null, null, "Test notes", 1L));
    }
}
