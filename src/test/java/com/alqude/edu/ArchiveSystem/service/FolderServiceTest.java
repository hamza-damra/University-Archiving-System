package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FolderService.
 * Tests folder creation, idempotency, and folder existence checks.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class FolderServiceTest {

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private FolderServiceImpl folderService;

    private User professor;
    private AcademicYear academicYear;
    private Semester semester;
    private Course course;
    private Department department;

    @BeforeEach
    void setUp() {
        // Set upload directory for testing
        ReflectionTestUtils.setField(folderService, "uploadDir", "test-uploads");

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
        professor.setIsActive(true);
        professor.setDepartment(department);

        // Setup academic year
        academicYear = new AcademicYear();
        academicYear.setId(1L);
        academicYear.setYearCode("2024-2025");

        // Setup semester
        semester = new Semester();
        semester.setId(1L);
        semester.setType(SemesterType.FIRST);
        semester.setAcademicYear(academicYear);

        // Setup course
        course = new Course();
        course.setId(1L);
        course.setCourseCode("CS101");
        course.setCourseName("Data Structures");
        course.setDepartment(department);
        course.setIsActive(true);
    }

    @Test
    void testCreateProfessorFolder_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.empty());
        
        Folder expectedFolder = Folder.builder()
                .id(1L)
                .path("2024-2025/first/PROF123")
                .name("John Doe")
                .type(FolderType.PROFESSOR_ROOT)
                .owner(professor)
                .academicYear(academicYear)
                .semester(semester)
                .build();
        
        when(folderRepository.save(any(Folder.class))).thenReturn(expectedFolder);

        // Act
        Folder result = folderService.createProfessorFolder(1L, 1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("2024-2025/first/PROF123", result.getPath());
        assertEquals("John Doe", result.getName());
        assertEquals(FolderType.PROFESSOR_ROOT, result.getType());
        assertEquals(professor, result.getOwner());
        
        verify(userRepository).findById(1L);
        verify(academicYearRepository).findById(1L);
        verify(semesterRepository).findById(1L);
        verify(folderRepository).findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT);
        verify(folderRepository).save(any(Folder.class));
    }

    @Test
    void testCreateProfessorFolder_Idempotent() {
        // Arrange
        Folder existingFolder = Folder.builder()
                .id(1L)
                .path("2024-2025/first/PROF123")
                .name("John Doe")
                .type(FolderType.PROFESSOR_ROOT)
                .owner(professor)
                .academicYear(academicYear)
                .semester(semester)
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.of(existingFolder));

        // Act
        Folder result = folderService.createProfessorFolder(1L, 1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(existingFolder, result);
        assertEquals("2024-2025/first/PROF123", result.getPath());
        
        // Verify that save was NOT called (idempotency)
        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    void testCreateProfessorFolder_ProfessorNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            folderService.createProfessorFolder(1L, 1L, 1L);
        });
        
        verify(userRepository).findById(1L);
        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    void testCreateProfessorFolder_AcademicYearNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            folderService.createProfessorFolder(1L, 1L, 1L);
        });
        
        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    void testCreateProfessorFolder_SemesterNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            folderService.createProfessorFolder(1L, 1L, 1L);
        });
        
        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    void testCreateProfessorFolder_UserNotProfessor() {
        // Arrange
        User nonProfessor = new User();
        nonProfessor.setId(1L);
        nonProfessor.setRole(Role.ROLE_DEANSHIP);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(nonProfessor));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            folderService.createProfessorFolder(1L, 1L, 1L);
        });
        
        verify(folderRepository, never()).save(any(Folder.class));
    }


    @Test
    void testCreateCourseFolderStructure_Success() {
        // Arrange
        Folder professorFolder = Folder.builder()
                .id(1L)
                .path("2024-2025/first/PROF123")
                .name("John Doe")
                .type(FolderType.PROFESSOR_ROOT)
                .owner(professor)
                .academicYear(academicYear)
                .semester(semester)
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.of(professorFolder));
        when(folderRepository.findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE))
                .thenReturn(Optional.empty());
        when(folderRepository.findByPath(anyString())).thenReturn(Optional.empty());
        
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(System.currentTimeMillis()); // Simulate ID generation
            return folder;
        });

        // Act
        List<Folder> result = folderService.createCourseFolderStructure(1L, 1L, 1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size()); // 1 course folder + 4 subfolders
        
        // Verify course folder
        Folder courseFolder = result.get(0);
        assertEquals("2024-2025/first/PROF123/CS101 - Data Structures", courseFolder.getPath());
        assertEquals(FolderType.COURSE, courseFolder.getType());
        
        // Verify subfolders were created
        verify(folderRepository, times(5)).save(any(Folder.class)); // 1 course + 4 subfolders
    }

    @Test
    void testCreateCourseFolderStructure_WithPartialExistingFolders() {
        // Arrange
        Folder professorFolder = Folder.builder()
                .id(1L)
                .path("2024-2025/first/PROF123")
                .name("John Doe")
                .type(FolderType.PROFESSOR_ROOT)
                .owner(professor)
                .academicYear(academicYear)
                .semester(semester)
                .build();
        
        Folder existingCourseFolder = Folder.builder()
                .id(2L)
                .path("2024-2025/first/PROF123/CS101 - Data Structures")
                .name("CS101 - Data Structures")
                .type(FolderType.COURSE)
                .parent(professorFolder)
                .owner(professor)
                .academicYear(academicYear)
                .semester(semester)
                .course(course)
                .build();
        
        Folder existingSyllabusFolder = Folder.builder()
                .id(3L)
                .path("2024-2025/first/PROF123/CS101 - Data Structures/Syllabus")
                .name("Syllabus")
                .type(FolderType.SUBFOLDER)
                .parent(existingCourseFolder)
                .owner(professor)
                .academicYear(academicYear)
                .semester(semester)
                .course(course)
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.of(professorFolder));
        when(folderRepository.findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE))
                .thenReturn(Optional.of(existingCourseFolder));
        
        // Mock findByPath to return existing Syllabus folder, but not others
        when(folderRepository.findByPath("2024-2025/first/PROF123/CS101 - Data Structures/Syllabus"))
                .thenReturn(Optional.of(existingSyllabusFolder));
        when(folderRepository.findByPath(argThat(path -> 
                !path.equals("2024-2025/first/PROF123/CS101 - Data Structures/Syllabus"))))
                .thenReturn(Optional.empty());
        
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(System.currentTimeMillis());
            return folder;
        });

        // Act
        List<Folder> result = folderService.createCourseFolderStructure(1L, 1L, 1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size()); // 1 existing course + 1 existing subfolder + 3 new subfolders
        
        // Verify only 3 new subfolders were saved (Exams, Course Notes, Assignments)
        verify(folderRepository, times(3)).save(any(Folder.class));
    }

    @Test
    void testCreateCourseFolderStructure_CourseNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            folderService.createCourseFolderStructure(1L, 1L, 1L, 1L);
        });
        
        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    void testProfessorFolderExists_ReturnsTrue() {
        // Arrange
        Folder professorFolder = Folder.builder()
                .id(1L)
                .path("2024-2025/first/PROF123")
                .build();
        
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.of(professorFolder));

        // Act
        boolean result = folderService.professorFolderExists(1L, 1L, 1L);

        // Assert
        assertTrue(result);
        verify(folderRepository).findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT);
    }

    @Test
    void testProfessorFolderExists_ReturnsFalse() {
        // Arrange
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.empty());

        // Act
        boolean result = folderService.professorFolderExists(1L, 1L, 1L);

        // Assert
        assertFalse(result);
        verify(folderRepository).findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT);
    }

    @Test
    void testCourseFolderExists_ReturnsTrue() {
        // Arrange
        Folder courseFolder = Folder.builder()
                .id(2L)
                .path("2024-2025/first/PROF123/CS101 - Data Structures")
                .build();
        
        when(folderRepository.findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE))
                .thenReturn(Optional.of(courseFolder));

        // Act
        boolean result = folderService.courseFolderExists(1L, 1L, 1L, 1L);

        // Assert
        assertTrue(result);
        verify(folderRepository).findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE);
    }

    @Test
    void testCourseFolderExists_ReturnsFalse() {
        // Arrange
        when(folderRepository.findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE))
                .thenReturn(Optional.empty());

        // Act
        boolean result = folderService.courseFolderExists(1L, 1L, 1L, 1L);

        // Assert
        assertFalse(result);
        verify(folderRepository).findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE);
    }

    @Test
    void testGetFolderByPath_Found() {
        // Arrange
        Folder folder = Folder.builder()
                .id(1L)
                .path("2024-2025/first/PROF123")
                .name("John Doe")
                .build();
        
        when(folderRepository.findByPath("2024-2025/first/PROF123"))
                .thenReturn(Optional.of(folder));

        // Act
        Optional<Folder> result = folderService.getFolderByPath("2024-2025/first/PROF123");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("2024-2025/first/PROF123", result.get().getPath());
        verify(folderRepository).findByPath("2024-2025/first/PROF123");
    }

    @Test
    void testGetFolderByPath_NotFound() {
        // Arrange
        when(folderRepository.findByPath("nonexistent/path"))
                .thenReturn(Optional.empty());

        // Act
        Optional<Folder> result = folderService.getFolderByPath("nonexistent/path");

        // Assert
        assertFalse(result.isPresent());
        verify(folderRepository).findByPath("nonexistent/path");
    }

    @Test
    void testCreateFolderIfNotExists_CreatesNewFolder() {
        // Arrange
        when(folderRepository.findByPath("test/path")).thenReturn(Optional.empty());
        
        Folder savedFolder = Folder.builder()
                .id(1L)
                .path("test/path")
                .name("Test Folder")
                .type(FolderType.SUBFOLDER)
                .owner(professor)
                .build();
        
        when(folderRepository.save(any(Folder.class))).thenReturn(savedFolder);

        // Act
        Folder result = folderService.createFolderIfNotExists(
                "test/path", "Test Folder", null, FolderType.SUBFOLDER, professor);

        // Assert
        assertNotNull(result);
        assertEquals("test/path", result.getPath());
        assertEquals("Test Folder", result.getName());
        verify(folderRepository).findByPath("test/path");
        verify(folderRepository).save(any(Folder.class));
    }

    @Test
    void testCreateFolderIfNotExists_ReturnsExistingFolder() {
        // Arrange
        Folder existingFolder = Folder.builder()
                .id(1L)
                .path("test/path")
                .name("Test Folder")
                .type(FolderType.SUBFOLDER)
                .owner(professor)
                .build();
        
        when(folderRepository.findByPath("test/path")).thenReturn(Optional.of(existingFolder));

        // Act
        Folder result = folderService.createFolderIfNotExists(
                "test/path", "Test Folder", null, FolderType.SUBFOLDER, professor);

        // Assert
        assertNotNull(result);
        assertEquals(existingFolder, result);
        verify(folderRepository).findByPath("test/path");
        verify(folderRepository, never()).save(any(Folder.class));
    }
    
    @Test
    void testGetOrCreateFolderByPath_FolderExists() {
        // Arrange
        String path = "/2024-2025/first/PROF123/CS101/lecture_notes";
        Folder existingFolder = Folder.builder()
                .id(1L)
                .path("2024-2025/first/PROF123/CS101/lecture_notes")
                .name("Lecture Notes")
                .type(FolderType.SUBFOLDER)
                .owner(professor)
                .build();
        
        when(folderRepository.findByPath(anyString())).thenReturn(Optional.of(existingFolder));

        // Act
        Folder result = folderService.getOrCreateFolderByPath(path, professor.getId());

        // Assert
        assertNotNull(result);
        assertEquals(existingFolder, result);
        verify(folderRepository).findByPath(anyString());
        verify(academicYearRepository, never()).findByYearCode(anyString());
    }
    
    @Test
    void testGetOrCreateFolderByPath_CreateNewFolder() {
        // Arrange
        String path = "/2024-2025/first/PROF123/CS101/lecture_notes";
        
        when(folderRepository.findByPath(anyString())).thenReturn(Optional.empty());
        when(academicYearRepository.findByYearCode("2024-2025")).thenReturn(Optional.of(academicYear));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findByAcademicYearIdAndType(1L, SemesterType.FIRST))
                .thenReturn(Optional.of(semester));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(userRepository.findByProfessorId("PROF123")).thenReturn(Optional.of(professor));
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findByCourseCode("CS101")).thenReturn(Optional.of(course));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        
        // Mock professor folder creation
        Folder professorFolder = Folder.builder()
                .id(2L)
                .path("2024-2025/first/PROF123")
                .name("John Doe")
                .type(FolderType.PROFESSOR_ROOT)
                .owner(professor)
                .build();
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.of(professorFolder));
        
        // Mock course folder creation
        Folder courseFolder = Folder.builder()
                .id(3L)
                .path("2024-2025/first/PROF123/CS101 - Data Structures")
                .name("CS101 - Data Structures")
                .type(FolderType.COURSE)
                .owner(professor)
                .build();
        when(folderRepository.findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE))
                .thenReturn(Optional.of(courseFolder));
        
        // Mock new folder save
        Folder newFolder = Folder.builder()
                .id(4L)
                .path("2024-2025/first/PROF123/CS101/lecture_notes")
                .name("Lecture notes")
                .type(FolderType.SUBFOLDER)
                .owner(professor)
                .build();
        when(folderRepository.save(any(Folder.class))).thenReturn(newFolder);

        // Act
        Folder result = folderService.getOrCreateFolderByPath(path, professor.getId());

        // Assert
        assertNotNull(result);
        assertEquals(4L, result.getId());
        verify(folderRepository, atLeastOnce()).findByPath(anyString());
        verify(academicYearRepository).findByYearCode("2024-2025");
        verify(semesterRepository).findByAcademicYearIdAndType(1L, SemesterType.FIRST);
        verify(userRepository).findByProfessorId("PROF123");
        verify(courseRepository).findByCourseCode("CS101");
        verify(folderRepository, atLeastOnce()).save(any(Folder.class));
    }
    
    @Test
    void testGetOrCreateFolderByPath_InvalidPath() {
        // Arrange
        String path = "/invalid/path";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> folderService.getOrCreateFolderByPath(path, professor.getId()));
    }
    
    @Test
    void testGetOrCreateFolderByPath_AcademicYearNotFound() {
        // Arrange
        String path = "/2024-2025/first/PROF123/CS101/lecture_notes";
        
        when(folderRepository.findByPath(anyString())).thenReturn(Optional.empty());
        when(academicYearRepository.findByYearCode("2024-2025")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, 
                () -> folderService.getOrCreateFolderByPath(path, professor.getId()));
    }
    
    @Test
    void testGetOrCreateFolderByPath_ProfessorNotFound() {
        // Arrange
        String path = "/2024-2025/first/PROF123/CS101/lecture_notes";
        
        when(folderRepository.findByPath(anyString())).thenReturn(Optional.empty());
        when(academicYearRepository.findByYearCode("2024-2025")).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findByAcademicYearIdAndType(1L, SemesterType.FIRST))
                .thenReturn(Optional.of(semester));
        when(userRepository.findByProfessorId("PROF123")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, 
                () -> folderService.getOrCreateFolderByPath(path, professor.getId()));
    }
    
    @Test
    void testGetOrCreateFolderByPath_UnauthorizedUser() {
        // Arrange
        String path = "/2024-2025/first/PROF123/CS101/lecture_notes";
        Long unauthorizedUserId = 999L;
        
        when(folderRepository.findByPath(anyString())).thenReturn(Optional.empty());
        when(academicYearRepository.findByYearCode("2024-2025")).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findByAcademicYearIdAndType(1L, SemesterType.FIRST))
                .thenReturn(Optional.of(semester));
        when(userRepository.findByProfessorId("PROF123")).thenReturn(Optional.of(professor));

        // Act & Assert
        assertThrows(SecurityException.class, 
                () -> folderService.getOrCreateFolderByPath(path, unauthorizedUserId));
    }
}
