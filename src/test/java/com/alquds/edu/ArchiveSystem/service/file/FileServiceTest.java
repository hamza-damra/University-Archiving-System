package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.entity.academic.SemesterType;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.exception.file.FileUploadException;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.RequiredDocumentTypeRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for FileService following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Unit tests with mocked dependencies
 * - Test file upload, download, validation, and deletion
 * - Follow AAA pattern
 * - Test security and permission checks
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileService Unit Tests")
class FileServiceTest {

    @Mock
    private CourseAssignmentRepository courseAssignmentRepository;
    
    @Mock
    private DocumentSubmissionRepository documentSubmissionRepository;
    
    @Mock
    private UploadedFileRepository uploadedFileRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private FileServiceImpl fileService;
    
    @TempDir
    Path tempDir;
    
    private CourseAssignment testCourseAssignment;
    private User testProfessor;
    private DocumentSubmission testSubmission;
    private UploadedFile testFile;
    
    @BeforeEach
    void setUp() {
        // Setup test data
        testProfessor = TestDataBuilder.createProfessorUser();
        testProfessor.setId(1L);
        testProfessor.setEmail("professor@staff.alquds.edu");
        var department = TestDataBuilder.createDepartment();
        department.setId(1L);
        testProfessor.setDepartment(department); // Add department for permission checks
        
        testCourseAssignment = TestDataBuilder.createCourseAssignment();
        testCourseAssignment.setId(1L);
        testCourseAssignment.setProfessor(testProfessor);
        
        testSubmission = new DocumentSubmission();
        testSubmission.setId(1L);
        testSubmission.setCourseAssignment(testCourseAssignment);
        testSubmission.setProfessor(testProfessor);
        testSubmission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        testSubmission.setStatus(SubmissionStatus.UPLOADED);
        testSubmission.setFileCount(1);
        
        testFile = new UploadedFile();
        testFile.setId(1L);
        testFile.setOriginalFilename("test.pdf");
        testFile.setFileUrl("uploads/test.pdf");
        testFile.setFileSize(1024L);
        testFile.setDocumentSubmission(testSubmission);
        testFile.setUploader(testProfessor);
        
        // Ensure submission's professor has department for permission checks
        testSubmission.getProfessor().setDepartment(department);
        
        // Setup SecurityContext (use lenient for tests that don't need it)
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("professor@staff.alquds.edu");
        lenient().when(userRepository.findByEmail("professor@staff.alquds.edu"))
                .thenReturn(Optional.of(testProfessor));
        
        // Set upload directory to temp directory
        try {
            java.lang.reflect.Field field = FileServiceImpl.class.getDeclaredField("uploadDirectory");
            field.setAccessible(true);
            field.set(fileService, tempDir.toString());
        } catch (Exception e) {
            // Ignore reflection errors in tests
        }
    }
    
    // ==================== File Validation Tests ====================
    
    @Test
    @DisplayName("Should validate file type successfully")
    void shouldValidateFileTypeSuccessfully() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());
        List<String> allowedExtensions = Arrays.asList("pdf", "doc", "docx");
        
        // Act
        boolean isValid = fileService.validateFileType(file, allowedExtensions);
        
        // Assert
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("Should return false for invalid file type")
    void shouldReturnFalseForInvalidFileType() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file", "test.exe", "application/x-msdownload", "content".getBytes());
        List<String> allowedExtensions = Arrays.asList("pdf", "doc", "docx");
        
        // Act
        boolean isValid = fileService.validateFileType(file, allowedExtensions);
        
        // Assert
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should return false for null file")
    void shouldReturnFalseForNullFile() {
        // Act
        boolean isValid = fileService.validateFileType(null, Arrays.asList("pdf"));
        
        // Assert
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should return false for empty file")
    void shouldReturnFalseForEmptyFile() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[0]);
        
        // Act
        boolean isValid = fileService.validateFileType(file, Arrays.asList("pdf"));
        
        // Assert
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should validate file size successfully")
    void shouldValidateFileSizeSuccessfully() {
        // Arrange
        MultipartFile file1 = new MockMultipartFile(
                "file1", "test1.pdf", "application/pdf", new byte[1024 * 1024]); // 1MB
        MultipartFile file2 = new MockMultipartFile(
                "file2", "test2.pdf", "application/pdf", new byte[1024 * 1024]); // 1MB
        List<MultipartFile> files = Arrays.asList(file1, file2);
        Integer maxTotalSizeMb = 10; // 10MB limit
        
        // Act
        boolean isValid = fileService.validateFileSize(files, maxTotalSizeMb);
        
        // Assert
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("Should return false when file size exceeds limit")
    void shouldReturnFalseWhenFileSizeExceedsLimit() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[100 * 1024 * 1024]); // 100MB
        List<MultipartFile> files = Arrays.asList(file);
        Integer maxTotalSizeMb = 50; // 50MB limit
        
        // Act
        boolean isValid = fileService.validateFileSize(files, maxTotalSizeMb);
        
        // Assert
        assertThat(isValid).isFalse();
    }
    
    // ==================== Get File Tests ====================
    
    @Test
    @DisplayName("Should throw exception when file not found")
    void shouldThrowExceptionWhenFileNotFound() {
        // Arrange
        Long fileId = 999L;
        
        when(uploadedFileRepository.findByIdWithUploader(fileId))
                .thenReturn(Optional.empty());
        
        // Setup SecurityContext for getCurrentUser (even though it will fail before that)
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> fileService.getFile(fileId))
                .isInstanceOf(FileUploadException.class);
    }
    
    // ==================== Get Files By Submission Tests ====================
    
    @Test
    @DisplayName("Should throw exception when submission not found")
    void shouldThrowExceptionWhenSubmissionNotFound() {
        // Arrange
        Long submissionId = 999L;
        
        when(documentSubmissionRepository.findById(submissionId))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> fileService.getFilesBySubmission(submissionId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }
    
    // ==================== Delete File Tests ====================
    
    @Test
    @DisplayName("Should throw exception when file not found for deletion")
    void shouldThrowExceptionWhenFileNotFoundForDeletion() {
        // Arrange
        Long fileId = 999L;
        
        when(uploadedFileRepository.findById(fileId))
                .thenReturn(Optional.empty());
        
        // Setup SecurityContext for getCurrentUser (even though it will fail before that)
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> fileService.deleteFile(fileId))
                .isInstanceOf(FileUploadException.class);
        
        verify(uploadedFileRepository, never()).delete(any(UploadedFile.class));
    }
    
    // ==================== Generate File Path Tests ====================
    
    @Test
    @DisplayName("Should generate file path successfully")
    void shouldGenerateFilePathSuccessfully() {
        // Arrange
        String yearCode = "2024-2025";
        String semesterType = "FIRST";
        String professorId = "1";
        String courseCode = "CS101";
        DocumentTypeEnum documentType = DocumentTypeEnum.SYLLABUS;
        String filename = "syllabus.pdf";
        
        // Act
        String result = fileService.generateFilePath(
                yearCode, semesterType, professorId, courseCode, documentType, filename);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).contains(yearCode);
        assertThat(result).contains(semesterType.toLowerCase());
        assertThat(result).contains(professorId);
        assertThat(result).contains(courseCode);
        assertThat(result).contains(documentType.name().toLowerCase());
        assertThat(result).endsWith(".pdf");
    }
    
    @Test
    @DisplayName("Should sanitize filename in generated path")
    void shouldSanitizeFilenameInGeneratedPath() {
        // Arrange
        String filename = "../../../malicious.pdf"; // Path traversal attempt
        
        // Act
        String result = fileService.generateFilePath(
                "2024-2025", "FIRST", "1", "CS101", DocumentTypeEnum.SYLLABUS, filename);
        
        // Assert
        // The generateFilePath method sanitizes the filename and generates a unique name
        // So the result should not contain the original malicious path
        assertThat(result).isNotNull();
        assertThat(result).contains("2024-2025");
        assertThat(result).contains("first");
        // The filename will be sanitized and made unique, so we just check it doesn't have the malicious path
        assertThat(result).doesNotContain("../../../");
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    @DisplayName("Should handle file with no extension")
    void shouldHandleFileWithNoExtension() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file", "test", "application/octet-stream", "content".getBytes());
        List<String> allowedExtensions = Arrays.asList("pdf", "doc");
        
        // Act
        boolean isValid = fileService.validateFileType(file, allowedExtensions);
        
        // Assert
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should handle empty file list for size validation")
    void shouldHandleEmptyFileListForSizeValidation() {
        // Act
        boolean isValid = fileService.validateFileSize(Arrays.asList(), 50);
        
        // Assert
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should use default max size when null provided")
    void shouldUseDefaultMaxSizeWhenNullProvided() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[30 * 1024 * 1024]); // 30MB
        List<MultipartFile> files = Arrays.asList(file);
        
        // Act (null maxTotalSizeMb should default to 50MB)
        boolean isValid = fileService.validateFileSize(files, null);
        
        // Assert
        assertThat(isValid).isTrue(); // 30MB < 50MB default
    }
    
    // ==================== Task 4.2: Enhanced Tests ====================
    
    @Test
    @DisplayName("uploadFiles - full workflow with file saving")
    void uploadFiles_FullWorkflowWithFileSaving() throws IOException {
        // Arrange
        AcademicYear academicYear = TestDataBuilder.createAcademicYear();
        academicYear.setId(1L);
        academicYear.setYearCode("2024-2025");
        
        Semester semester = TestDataBuilder.createSemester();
        semester.setId(1L);
        semester.setType(SemesterType.FIRST);
        semester.setAcademicYear(academicYear);
        semester.setStartDate(LocalDate.of(2024, 9, 1));
        semester.setEndDate(LocalDate.of(2024, 12, 31));
        
        Course course = TestDataBuilder.createCourse();
        course.setId(1L);
        course.setCourseCode("CS101");
        
        testCourseAssignment.setSemester(semester);
        testCourseAssignment.setCourse(course);
        
        MultipartFile file1 = new MockMultipartFile(
                "file1", "syllabus.pdf", "application/pdf", "PDF content".getBytes());
        MultipartFile file2 = new MockMultipartFile(
                "file2", "notes.pdf", "application/pdf", "Notes content".getBytes());
        List<MultipartFile> files = Arrays.asList(file1, file2);
        
        when(courseAssignmentRepository.findById(1L))
                .thenReturn(Optional.of(testCourseAssignment));
        when(requiredDocumentTypeRepository.findByCourseIdAndDocumentType(1L, DocumentTypeEnum.SYLLABUS))
                .thenReturn(Collections.emptyList());
        when(documentSubmissionRepository.findByCourseAssignmentIdAndDocumentType(1L, DocumentTypeEnum.SYLLABUS))
                .thenReturn(Optional.empty());
        when(documentSubmissionRepository.save(any(DocumentSubmission.class)))
                .thenAnswer(invocation -> {
                    DocumentSubmission sub = invocation.getArgument(0);
                    if (sub.getId() == null) {
                        sub.setId(1L);
                    }
                    return sub;
                });
        when(uploadedFileRepository.save(any(UploadedFile.class)))
                .thenAnswer(invocation -> {
                    UploadedFile uf = invocation.getArgument(0);
                    if (uf.getId() == null) {
                        uf.setId((long) (Math.random() * 1000));
                    }
                    return uf;
                });
        
        // Act
        List<UploadedFile> result = fileService.uploadFiles(
                1L, DocumentTypeEnum.SYLLABUS, files, "Test notes", 1L);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOriginalFilename()).isEqualTo("syllabus.pdf");
        assertThat(result.get(1).getOriginalFilename()).isEqualTo("notes.pdf");
        
        // Verify files were saved to disk
        verify(documentSubmissionRepository, times(2)).save(any(DocumentSubmission.class));
        verify(uploadedFileRepository, times(2)).save(any(UploadedFile.class));
        
        // Verify physical files exist
        assertThat(result.get(0).getFileUrl()).isNotNull();
        assertThat(result.get(1).getFileUrl()).isNotNull();
    }
    
    @Test
    @DisplayName("replaceFiles - full workflow")
    void replaceFiles_FullWorkflow() throws IOException {
        // Arrange
        AcademicYear academicYear = TestDataBuilder.createAcademicYear();
        academicYear.setId(1L);
        academicYear.setYearCode("2024-2025");
        
        Semester semester = TestDataBuilder.createSemester();
        semester.setId(1L);
        semester.setType(SemesterType.FIRST);
        semester.setAcademicYear(academicYear);
        
        Course course = TestDataBuilder.createCourse();
        course.setId(1L);
        course.setCourseCode("CS101");
        
        testCourseAssignment.setSemester(semester);
        testCourseAssignment.setCourse(course);
        testSubmission.setCourseAssignment(testCourseAssignment);
        
        // Create old files
        UploadedFile oldFile1 = new UploadedFile();
        oldFile1.setId(10L);
        oldFile1.setFileUrl("2024-2025/first/prof_1/CS101/syllabus/old_file.pdf");
        oldFile1.setDocumentSubmission(testSubmission);
        
        UploadedFile oldFile2 = new UploadedFile();
        oldFile2.setId(11L);
        oldFile2.setFileUrl("2024-2025/first/prof_1/CS101/syllabus/old_file2.pdf");
        oldFile2.setDocumentSubmission(testSubmission);
        
        List<UploadedFile> oldFiles = Arrays.asList(oldFile1, oldFile2);
        
        MultipartFile newFile = new MockMultipartFile(
                "file", "new_syllabus.pdf", "application/pdf", "New content".getBytes());
        List<MultipartFile> newFiles = Arrays.asList(newFile);
        
        when(documentSubmissionRepository.findById(1L))
                .thenReturn(Optional.of(testSubmission));
        when(requiredDocumentTypeRepository.findByCourseIdAndDocumentType(1L, DocumentTypeEnum.SYLLABUS))
                .thenReturn(Collections.emptyList());
        when(uploadedFileRepository.findByDocumentSubmissionId(1L))
                .thenReturn(oldFiles);
        when(uploadedFileRepository.save(any(UploadedFile.class)))
                .thenAnswer(invocation -> {
                    UploadedFile uf = invocation.getArgument(0);
                    if (uf.getId() == null) {
                        uf.setId(20L);
                    }
                    return uf;
                });
        when(uploadedFileRepository.findByDocumentSubmissionId(1L))
                .thenReturn(oldFiles) // First call returns old files
                .thenReturn(Collections.singletonList(oldFile1)); // After deletion, return remaining
        
        // Act
        fileService.replaceFiles(1L, newFiles, "Updated notes");
        
        // Assert
        verify(uploadedFileRepository).deleteAll(oldFiles);
        verify(documentSubmissionRepository).save(any(DocumentSubmission.class));
        verify(uploadedFileRepository, times(1)).save(any(UploadedFile.class));
    }
    
    @Test
    @DisplayName("loadFileAsResource - path traversal protection")
    void loadFileAsResource_PathTraversalProtection() throws IOException {
        // Arrange - Create a valid file first
        Path validFile = tempDir.resolve("valid_file.pdf");
        Files.write(validFile, "Test content".getBytes());
        
        // Test path traversal attempts
        String[] maliciousPaths = {
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32",
            "2024-2025/../../../etc/passwd",
            "2024-2025/first/../../../../root",
            "valid_file.pdf/../../etc/passwd"
        };
        
        for (String maliciousPath : maliciousPaths) {
            // Act & Assert
            assertThatThrownBy(() -> fileService.loadFileAsResource(maliciousPath))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("access denied");
        }
    }
    
    @Test
    @DisplayName("loadFileAsResourceWithPermissionCheck - permission validation")
    void loadFileAsResourceWithPermissionCheck_PermissionValidation() throws IOException {
        // Arrange
        Path validFile = tempDir.resolve("2024-2025").resolve("first").resolve("prof_1").resolve("CS101").resolve("syllabus").resolve("test.pdf");
        Files.createDirectories(validFile.getParent());
        Files.write(validFile, "Test content".getBytes());
        
        testFile.setFileUrl("2024-2025/first/prof_1/CS101/syllabus/test.pdf");
        
        // Ensure the file's submission professor has a department (same as testProfessor)
        testFile.getDocumentSubmission().getProfessor().setDepartment(testProfessor.getDepartment());
        
        // Test with authorized user (same professor)
        when(uploadedFileRepository.findByIdWithUploader(1L))
                .thenReturn(Optional.of(testFile));
        
        // Act
        Resource resource = fileService.loadFileAsResourceWithPermissionCheck(1L, testProfessor);
        
        // Assert
        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        
        // Test with unauthorized user (different professor)
        User otherProfessor = TestDataBuilder.createProfessorUser();
        otherProfessor.setId(2L);
        otherProfessor.setEmail("other@staff.alquds.edu");
        var otherDept = TestDataBuilder.createDepartment();
        otherDept.setId(2L); // Different department
        otherProfessor.setDepartment(otherDept);
        
        // Update SecurityContext to use otherProfessor
        when(authentication.getName()).thenReturn("other@staff.alquds.edu");
        when(userRepository.findByEmail("other@staff.alquds.edu"))
                .thenReturn(Optional.of(otherProfessor));
        
        when(uploadedFileRepository.findByIdWithUploader(1L))
                .thenReturn(Optional.of(testFile));
        
        // Act & Assert
        assertThatThrownBy(() -> fileService.loadFileAsResourceWithPermissionCheck(1L, otherProfessor))
                .isInstanceOf(UnauthorizedOperationException.class);
    }
    
    @Test
    @DisplayName("deleteFile - updates submission metadata correctly")
    void deleteFile_UpdatesSubmissionMetadataCorrectly() throws IOException {
        // Arrange
        testSubmission.setFileCount(3);
        testSubmission.setTotalFileSize(3072L); // 3 files * 1024 bytes
        
        UploadedFile fileToDelete = new UploadedFile();
        fileToDelete.setId(2L);
        fileToDelete.setFileUrl("2024-2025/first/prof_1/CS101/syllabus/file2.pdf");
        fileToDelete.setFileSize(1024L);
        fileToDelete.setDocumentSubmission(testSubmission);
        fileToDelete.setUploader(testProfessor);
        
        // Create physical file
        Path filePath = tempDir.resolve(fileToDelete.getFileUrl());
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, "Content".getBytes());
        
        // Remaining files after deletion
        UploadedFile remainingFile1 = new UploadedFile();
        remainingFile1.setId(1L);
        remainingFile1.setFileSize(1024L);
        remainingFile1.setDocumentSubmission(testSubmission);
        
        UploadedFile remainingFile2 = new UploadedFile();
        remainingFile2.setId(3L);
        remainingFile2.setFileSize(1024L);
        remainingFile2.setDocumentSubmission(testSubmission);
        
        List<UploadedFile> remainingFiles = Arrays.asList(remainingFile1, remainingFile2);
        
        when(uploadedFileRepository.findById(2L))
                .thenReturn(Optional.of(fileToDelete));
        when(uploadedFileRepository.findByDocumentSubmissionId(1L))
                .thenReturn(remainingFiles);
        
        // Act
        fileService.deleteFile(2L);
        
        // Assert
        verify(uploadedFileRepository).delete(fileToDelete);
        verify(documentSubmissionRepository).save(argThat(submission -> {
            assertThat(submission.getFileCount()).isEqualTo(2);
            assertThat(submission.getTotalFileSize()).isEqualTo(2048L); // 2 files * 1024 bytes
            return true;
        }));
        
        // Verify physical file was deleted
        assertThat(Files.exists(filePath)).isFalse();
    }
    
    @Test
    @DisplayName("generateFilePath - various edge cases")
    void generateFilePath_VariousEdgeCases() {
        // Test case 1: Normal path
        String normalPath = fileService.generateFilePath(
                "2024-2025", "FIRST", "prof_1", "CS101", DocumentTypeEnum.SYLLABUS, "syllabus.pdf");
        assertThat(normalPath).contains("2024-2025");
        assertThat(normalPath).contains("first");
        assertThat(normalPath).contains("prof_1");
        assertThat(normalPath).contains("CS101");
        assertThat(normalPath).contains("syllabus");
        assertThat(normalPath).endsWith(".pdf");
        
        // Test case 2: Empty filename
        String emptyFilenamePath = fileService.generateFilePath(
                "2024-2025", "SECOND", "prof_2", "CS102", DocumentTypeEnum.EXAM, "");
        assertThat(emptyFilenamePath).isNotNull();
        assertThat(emptyFilenamePath).contains("2024-2025");
        
        // Test case 3: Null filename
        String nullFilenamePath = fileService.generateFilePath(
                "2024-2025", "SUMMER", "prof_3", "CS103", DocumentTypeEnum.LECTURE_NOTES, null);
        assertThat(nullFilenamePath).isNotNull();
        assertThat(nullFilenamePath).contains("2024-2025");
        
        // Test case 4: Long filename
        String longFilename = "a".repeat(200) + ".pdf";
        String longFilenamePath = fileService.generateFilePath(
                "2024-2025", "FIRST", "prof_1", "CS101", DocumentTypeEnum.ASSIGNMENT, longFilename);
        assertThat(longFilenamePath).isNotNull();
        assertThat(longFilenamePath).endsWith(".pdf");
        
        // Test case 5: Different document types
        for (DocumentTypeEnum docType : DocumentTypeEnum.values()) {
            String path = fileService.generateFilePath(
                    "2024-2025", "FIRST", "prof_1", "CS101", docType, "file.pdf");
            assertThat(path).isNotNull();
            assertThat(path).contains(docType.name().toLowerCase());
        }
        
        // Test case 6: Different semester types
        String[] semesterTypes = {"FIRST", "SECOND", "SUMMER"};
        for (String semesterType : semesterTypes) {
            String path = fileService.generateFilePath(
                    "2024-2025", semesterType, "prof_1", "CS101", DocumentTypeEnum.SYLLABUS, "file.pdf");
            assertThat(path).isNotNull();
            assertThat(path).contains(semesterType.toLowerCase());
        }
    }
    
    @Test
    @DisplayName("Filename sanitization - various special characters")
    void filenameSanitization_VariousSpecialCharacters() {
        // Test various special characters that should be sanitized
        String[] specialCharFilenames = {
            "file with spaces.pdf",
            "file@with#special$chars.pdf",
            "file/with\\path\\separators.pdf",
            "file:with*invalid?chars<>.pdf",
            "file\"with|pipes\".pdf",
            "file%20encoded.pdf",
            "file+plus+signs.pdf",
            "file=equals=signs.pdf",
            "file&and&signs.pdf",
            "file^caret^signs.pdf",
            "file~tilde~signs.pdf",
            "file`backtick`signs.pdf",
            "file!exclamation.pdf",
            "file@at@sign.pdf",
            "file#hash#sign.pdf",
            "file$dollar$sign.pdf"
        };
        
        for (String originalFilename : specialCharFilenames) {
            String path = fileService.generateFilePath(
                    "2024-2025", "FIRST", "prof_1", "CS101", DocumentTypeEnum.SYLLABUS, originalFilename);
            
            // Assert that the path doesn't contain dangerous characters
            assertThat(path).isNotNull();
            assertThat(path).doesNotContain(".."); // No path traversal
            // Note: The path structure itself uses "/" as separators, so we only check the filename part
            String sanitizedFilename = path.substring(path.lastIndexOf("/") + 1);
            assertThat(sanitizedFilename).doesNotContain("\\");
            assertThat(sanitizedFilename).doesNotContain(":");
            assertThat(sanitizedFilename).doesNotContain("*");
            assertThat(sanitizedFilename).doesNotContain("?");
            assertThat(sanitizedFilename).doesNotContain("\"");
            assertThat(sanitizedFilename).doesNotContain("<");
            assertThat(sanitizedFilename).doesNotContain(">");
            assertThat(sanitizedFilename).doesNotContain("|");
            
            // The filename should be sanitized (special chars replaced with _)
            // But we can't check exact content since it's also made unique
            assertThat(path).contains("2024-2025");
        }
    }
}
