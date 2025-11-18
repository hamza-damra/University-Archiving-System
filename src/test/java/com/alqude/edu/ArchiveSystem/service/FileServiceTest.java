package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.entity.*;

import com.alqude.edu.ArchiveSystem.exception.FileUploadException;
import com.alqude.edu.ArchiveSystem.repository.CourseAssignmentRepository;
import com.alqude.edu.ArchiveSystem.repository.DocumentSubmissionRepository;
import com.alqude.edu.ArchiveSystem.repository.UploadedFileRepository;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class FileServiceTest {

    @Mock
    private CourseAssignmentRepository courseAssignmentRepository;

    @Mock
    private DocumentSubmissionRepository documentSubmissionRepository;

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FileServiceImpl fileService;

    private CourseAssignment courseAssignment;
    private User professor;
    private Department department;
    private Course course;
    private Semester semester;
    private AcademicYear academicYear;

    @BeforeEach
    void setUp() {
        // Set upload directory for testing
        ReflectionTestUtils.setField(fileService, "uploadDirectory", "test-uploads/", String.class);

        // Setup department
        department = new Department();
        department.setId(1L);
        department.setName("Computer Science");

        // Setup academic year
        academicYear = new AcademicYear();
        academicYear.setId(1L);
        academicYear.setYearCode("2024-2025");
        academicYear.setStartYear(2024);
        academicYear.setEndYear(2025);

        // Setup semester
        semester = new Semester();
        semester.setId(1L);
        semester.setType(SemesterType.FIRST);
        semester.setAcademicYear(academicYear);

        // Setup course
        course = new Course();
        course.setId(1L);
        course.setCourseCode("CS101");
        course.setCourseName("Introduction to Programming");
        course.setDepartment(department);

        // Setup professor
        professor = new User();
        professor.setId(1L);
        professor.setEmail("professor@example.com");
        professor.setRole(Role.ROLE_PROFESSOR);
        professor.setDepartment(department);
        professor.setProfessorId("prof_001");

        // Setup course assignment
        courseAssignment = new CourseAssignment();
        courseAssignment.setId(1L);
        courseAssignment.setSemester(semester);
        courseAssignment.setCourse(course);
        courseAssignment.setProfessor(professor);
    }

    @Test
    void testGenerateFilePath_CreatesCorrectHierarchicalPath() {
        // Arrange
        String yearCode = "2024-2025";
        String semesterType = "FIRST";
        String professorId = "prof_001";
        String courseCode = "CS101";
        DocumentTypeEnum documentType = DocumentTypeEnum.SYLLABUS;
        String filename = "syllabus.pdf";

        // Act
        String result = fileService.generateFilePath(yearCode, semesterType, professorId, 
                                                     courseCode, documentType, filename);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(yearCode));
        assertTrue(result.contains(semesterType.toLowerCase()));
        assertTrue(result.contains(professorId));
        assertTrue(result.contains(courseCode));
        assertTrue(result.contains(documentType.name().toLowerCase()));
        assertTrue(result.contains("syllabus"));
        assertTrue(result.endsWith(".pdf"));
        
        // Verify hierarchical structure
        String[] pathParts = result.split("/");
        assertTrue(pathParts.length >= 6, "Path should have at least 6 parts");
    }

    @Test
    void testValidateFileType_AcceptsPdfFiles() {
        // Arrange
        MockMultipartFile pdfFile = new MockMultipartFile(
            "file", 
            "document.pdf", 
            "application/pdf", 
            "test content".getBytes()
        );
        List<String> allowedExtensions = Arrays.asList("pdf", "zip");

        // Act
        boolean result = fileService.validateFileType(pdfFile, allowedExtensions);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateFileType_AcceptsZipFiles() {
        // Arrange
        MockMultipartFile zipFile = new MockMultipartFile(
            "file", 
            "archive.zip", 
            "application/zip", 
            "test content".getBytes()
        );
        List<String> allowedExtensions = Arrays.asList("pdf", "zip");

        // Act
        boolean result = fileService.validateFileType(zipFile, allowedExtensions);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateFileType_RejectsInvalidExtension() {
        // Arrange
        MockMultipartFile docFile = new MockMultipartFile(
            "file", 
            "document.doc", 
            "application/msword", 
            "test content".getBytes()
        );
        List<String> allowedExtensions = Arrays.asList("pdf", "zip");

        // Act
        boolean result = fileService.validateFileType(docFile, allowedExtensions);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateFileType_RejectsNullFile() {
        // Arrange
        List<String> allowedExtensions = Arrays.asList("pdf", "zip");

        // Act
        boolean result = fileService.validateFileType(null, allowedExtensions);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateFileType_RejectsFileWithoutExtension() {
        // Arrange
        MockMultipartFile fileWithoutExt = new MockMultipartFile(
            "file", 
            "document", 
            "application/octet-stream", 
            "test content".getBytes()
        );
        List<String> allowedExtensions = Arrays.asList("pdf", "zip");

        // Act
        boolean result = fileService.validateFileType(fileWithoutExt, allowedExtensions);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateFileSize_AcceptsFilesWithinLimit() {
        // Arrange
        MockMultipartFile file1 = new MockMultipartFile(
            "file1", 
            "doc1.pdf", 
            "application/pdf", 
            new byte[5 * 1024 * 1024] // 5MB
        );
        MockMultipartFile file2 = new MockMultipartFile(
            "file2", 
            "doc2.pdf", 
            "application/pdf", 
            new byte[3 * 1024 * 1024] // 3MB
        );
        List<MultipartFile> files = Arrays.asList(file1, file2);
        Integer maxTotalSizeMb = 10; // 10MB limit

        // Act
        boolean result = fileService.validateFileSize(files, maxTotalSizeMb);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateFileSize_RejectsFilesExceedingLimit() {
        // Arrange
        MockMultipartFile file1 = new MockMultipartFile(
            "file1", 
            "doc1.pdf", 
            "application/pdf", 
            new byte[6 * 1024 * 1024] // 6MB
        );
        MockMultipartFile file2 = new MockMultipartFile(
            "file2", 
            "doc2.pdf", 
            "application/pdf", 
            new byte[5 * 1024 * 1024] // 5MB
        );
        List<MultipartFile> files = Arrays.asList(file1, file2);
        Integer maxTotalSizeMb = 10; // 10MB limit, but files total 11MB

        // Act
        boolean result = fileService.validateFileSize(files, maxTotalSizeMb);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateFileSize_RejectsNullFiles() {
        // Arrange
        Integer maxTotalSizeMb = 10;

        // Act
        boolean result = fileService.validateFileSize(null, maxTotalSizeMb);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateFileSize_RejectsEmptyFileList() {
        // Arrange
        List<MultipartFile> files = Arrays.asList();
        Integer maxTotalSizeMb = 10;

        // Act
        boolean result = fileService.validateFileSize(files, maxTotalSizeMb);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateFileSize_UsesDefaultLimitWhenNull() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "doc.pdf", 
            "application/pdf", 
            new byte[40 * 1024 * 1024] // 40MB
        );
        List<MultipartFile> files = Arrays.asList(file);

        // Act - should use default 50MB limit
        boolean result = fileService.validateFileSize(files, null);

        // Assert
        assertTrue(result); // 40MB is within default 50MB limit
    }

    @Test
    void testGetFile_ThrowsExceptionWhenFileNotFound() {
        // Arrange
        Long fileId = 999L;
        when(uploadedFileRepository.findById(fileId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FileUploadException.class, () -> {
            fileService.getFile(fileId);
        });
    }

    @Test
    void testValidateFileType_CaseInsensitiveExtensionCheck() {
        // Arrange
        MockMultipartFile pdfFile = new MockMultipartFile(
            "file", 
            "document.PDF", // Uppercase extension
            "application/pdf", 
            "test content".getBytes()
        );
        List<String> allowedExtensions = Arrays.asList("pdf", "zip");

        // Act
        boolean result = fileService.validateFileType(pdfFile, allowedExtensions);

        // Assert
        assertTrue(result);
    }
}
