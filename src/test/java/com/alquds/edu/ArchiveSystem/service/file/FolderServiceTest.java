package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.entity.academic.SemesterType;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;
import com.alquds.edu.ArchiveSystem.entity.file.FolderType;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.repository.academic.AcademicYearRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.SemesterRepository;
import com.alquds.edu.ArchiveSystem.repository.file.FolderRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FolderService Unit Tests")
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

    @TempDir
    Path tempDir;

    private User professor;
    private AcademicYear academicYear;
    private Semester semester;
    private Course course;
    private Folder existingProfessorFolder;
    private Folder existingCourseFolder;

    @BeforeEach
    void setUp() {
        // Set upload directory to temp directory
        ReflectionTestUtils.setField(folderService, "uploadDir", tempDir.toString());

        // Setup test data
        professor = TestDataBuilder.createProfessorUser();
        professor.setId(1L);
        professor.setFirstName("John");
        professor.setLastName("Doe");
        professor.setProfessorId("PROF1");

        academicYear = TestDataBuilder.createAcademicYear();
        academicYear.setId(1L);

        semester = TestDataBuilder.createSemester();
        semester.setId(1L);
        semester.setAcademicYear(academicYear);
        semester.setType(SemesterType.FIRST);

        course = TestDataBuilder.createCourse();
        course.setId(1L);

        existingProfessorFolder = Folder.builder()
                .id(1L)
                .path("2024-2025/first/John Doe")
                .name("John Doe")
                .type(FolderType.PROFESSOR_ROOT)
                .parent(null)
                .owner(professor)
                .academicYear(academicYear)
                .semester(semester)
                .course(null)
                .build();

        existingCourseFolder = Folder.builder()
                .id(2L)
                .path("2024-2025/first/John Doe/CS101 - Introduction to Computer Science")
                .name("CS101 - Introduction to Computer Science")
                .type(FolderType.COURSE)
                .parent(existingProfessorFolder)
                .owner(professor)
                .academicYear(academicYear)
                .semester(semester)
                .course(course)
                .build();
    }

    // ==================== createProfessorFolder Tests ====================

    @Test
    @DisplayName("Should create professor folder successfully")
    void shouldCreateProfessorFolderSuccessfully() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.empty());
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(1L);
            return folder;
        });

        // Act
        Folder result = folderService.createProfessorFolder(1L, 1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPath()).isEqualTo("2024-2025/first/John Doe");
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getType()).isEqualTo(FolderType.PROFESSOR_ROOT);
        assertThat(result.getOwner()).isEqualTo(professor);
        assertThat(result.getAcademicYear()).isEqualTo(academicYear);
        assertThat(result.getSemester()).isEqualTo(semester);

        verify(folderRepository).save(any(Folder.class));
        verify(folderRepository).findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT);
    }

    @Test
    @DisplayName("Should return existing professor folder (idempotent)")
    void shouldReturnExistingProfessorFolder() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.of(existingProfessorFolder));

        // Act
        Folder result = folderService.createProfessorFolder(1L, 1L, 1L);

        // Assert
        assertThat(result).isEqualTo(existingProfessorFolder);
        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should throw exception when professor not found")
    void shouldThrowExceptionWhenProfessorNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> folderService.createProfessorFolder(1L, 1L, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Professor not found");

        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should throw exception when academic year not found")
    void shouldThrowExceptionWhenAcademicYearNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> folderService.createProfessorFolder(1L, 1L, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Academic year not found");

        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should throw exception when semester not found")
    void shouldThrowExceptionWhenSemesterNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> folderService.createProfessorFolder(1L, 1L, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Semester not found");

        verify(folderRepository, never()).save(any(Folder.class));
    }

    // ==================== createCourseFolderStructure Tests ====================

    @Test
    @DisplayName("Should create course folder structure successfully")
    void shouldCreateCourseFolderStructureSuccessfully() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        
        // Mock professor folder creation
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.of(existingProfessorFolder));
        
        // Mock course folder doesn't exist
        when(folderRepository.findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE))
                .thenReturn(Optional.empty());
        
        // Mock subfolder checks - none exist
        when(folderRepository.findByPath(anyString())).thenReturn(Optional.empty());
        
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            if (folder.getId() == null) {
                folder.setId((long) (Math.random() * 1000));
            }
            return folder;
        });

        // Act
        List<Folder> result = folderService.createCourseFolderStructure(1L, 1L, 1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(5); // 1 course folder + 4 subfolders
        assertThat(result.get(0).getType()).isEqualTo(FolderType.COURSE);
        assertThat(result.get(0).getPath()).contains("CS101 - Introduction to Computer Science");
        
        // Verify subfolders were created
        long subfolderCount = result.stream()
                .filter(f -> f.getType() == FolderType.SUBFOLDER)
                .count();
        assertThat(subfolderCount).isEqualTo(4);

        verify(folderRepository, atLeastOnce()).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should return existing course folder structure (idempotent)")
    void shouldReturnExistingCourseFolderStructure() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.of(existingProfessorFolder));
        when(folderRepository.findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE))
                .thenReturn(Optional.of(existingCourseFolder));
        
        // Mock all subfolders exist
        List<Folder> existingSubfolders = new ArrayList<>();
        existingSubfolders.add(existingCourseFolder);
        String[] subfolderNames = {"Syllabus", "Exams", "Course Notes", "Assignments"};
        for (String name : subfolderNames) {
            Folder subfolder = Folder.builder()
                    .id((long) (existingSubfolders.size() + 1))
                    .path(existingCourseFolder.getPath() + "/" + name)
                    .name(name)
                    .type(FolderType.SUBFOLDER)
                    .parent(existingCourseFolder)
                    .owner(professor)
                    .academicYear(academicYear)
                    .semester(semester)
                    .course(course)
                    .build();
            existingSubfolders.add(subfolder);
            when(folderRepository.findByPath(existingCourseFolder.getPath() + "/" + name))
                    .thenReturn(Optional.of(subfolder));
        }

        // Act
        List<Folder> result = folderService.createCourseFolderStructure(1L, 1L, 1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(5);
        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should create standard subfolders (Syllabus, Exams, Course Notes, Assignments)")
    void shouldCreateStandardSubfolders() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.of(existingProfessorFolder));
        when(folderRepository.findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE))
                .thenReturn(Optional.empty());
        when(folderRepository.findByPath(anyString())).thenReturn(Optional.empty());
        
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            if (folder.getId() == null) {
                folder.setId((long) (Math.random() * 1000));
            }
            return folder;
        });

        // Act
        List<Folder> result = folderService.createCourseFolderStructure(1L, 1L, 1L, 1L);

        // Assert
        assertThat(result).hasSize(5);
        
        List<String> subfolderNames = result.stream()
                .filter(f -> f.getType() == FolderType.SUBFOLDER)
                .map(Folder::getName)
                .toList();
        
        assertThat(subfolderNames).containsExactlyInAnyOrder(
                "Syllabus", "Exams", "Course Notes", "Assignments");
    }

    @Test
    @DisplayName("Should throw exception when course not found")
    void shouldThrowExceptionWhenCourseNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> folderService.createCourseFolderStructure(1L, 1L, 1L, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Course not found");

        verify(folderRepository, never()).save(any(Folder.class));
    }

    // ==================== professorFolderExists Tests ====================

    @Test
    @DisplayName("Should return true when professor folder exists")
    void shouldReturnTrueWhenProfessorFolderExists() {
        // Arrange
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.of(existingProfessorFolder));

        // Act
        boolean result = folderService.professorFolderExists(1L, 1L, 1L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when professor folder does not exist")
    void shouldReturnFalseWhenProfessorFolderNotExists() {
        // Arrange
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.empty());

        // Act
        boolean result = folderService.professorFolderExists(1L, 1L, 1L);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== courseFolderExists Tests ====================

    @Test
    @DisplayName("Should return true when course folder exists")
    void shouldReturnTrueWhenCourseFolderExists() {
        // Arrange
        when(folderRepository.findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE))
                .thenReturn(Optional.of(existingCourseFolder));

        // Act
        boolean result = folderService.courseFolderExists(1L, 1L, 1L, 1L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when course folder does not exist")
    void shouldReturnFalseWhenCourseFolderNotExists() {
        // Arrange
        when(folderRepository.findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE))
                .thenReturn(Optional.empty());

        // Act
        boolean result = folderService.courseFolderExists(1L, 1L, 1L, 1L);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== getFolderByPath Tests ====================

    @Test
    @DisplayName("Should return folder when found by path")
    void shouldReturnFolderWhenFoundByPath() {
        // Arrange
        String path = "2024-2025/first/John Doe";
        when(folderRepository.findByPath(path)).thenReturn(Optional.of(existingProfessorFolder));

        // Act
        Optional<Folder> result = folderService.getFolderByPath(path);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(existingProfessorFolder);
    }

    @Test
    @DisplayName("Should return empty when folder not found by path")
    void shouldReturnEmptyWhenFolderNotFoundByPath() {
        // Arrange
        String path = "2024-2025/first/NonExistent";
        when(folderRepository.findByPath(path)).thenReturn(Optional.empty());

        // Act
        Optional<Folder> result = folderService.getFolderByPath(path);

        // Assert
        assertThat(result).isEmpty();
    }

    // ==================== createFolderIfNotExists Tests ====================

    @Test
    @DisplayName("Should create new folder when it does not exist")
    void shouldCreateNewFolderWhenNotExists() {
        // Arrange
        String path = "2024-2025/first/John Doe/NewFolder";
        String name = "NewFolder";
        FolderType type = FolderType.SUBFOLDER;
        
        when(folderRepository.findByPath(path)).thenReturn(Optional.empty());
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(10L);
            return folder;
        });

        // Act
        Folder result = folderService.createFolderIfNotExists(path, name, existingProfessorFolder, type, professor);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPath()).isEqualTo(path);
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getType()).isEqualTo(type);
        assertThat(result.getParent()).isEqualTo(existingProfessorFolder);
        assertThat(result.getOwner()).isEqualTo(professor);
        
        verify(folderRepository).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should return existing folder when it already exists (idempotent)")
    void shouldReturnExistingFolderWhenAlreadyExists() {
        // Arrange
        String path = "2024-2025/first/John Doe/ExistingFolder";
        Folder existingFolder = Folder.builder()
                .id(5L)
                .path(path)
                .name("ExistingFolder")
                .type(FolderType.SUBFOLDER)
                .build();
        
        when(folderRepository.findByPath(path)).thenReturn(Optional.of(existingFolder));

        // Act
        Folder result = folderService.createFolderIfNotExists(path, "ExistingFolder", 
                existingProfessorFolder, FolderType.SUBFOLDER, professor);

        // Assert
        assertThat(result).isEqualTo(existingFolder);
        verify(folderRepository, never()).save(any(Folder.class));
    }

    // ==================== getOrCreateFolderByPath Tests ====================

    @Test
    @DisplayName("Should create folder hierarchy when path is valid")
    void shouldCreateFolderHierarchyWhenPathIsValid() {
        // Arrange
        String path = "2024-2025/first/John Doe/CS101/lecture_notes";
        
        when(folderRepository.findByPath(path)).thenReturn(Optional.empty());
        when(academicYearRepository.findByYearCode("2024-2025")).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findByAcademicYearIdAndType(1L, SemesterType.FIRST))
                .thenReturn(Optional.of(semester));
        when(userRepository.findByProfessorId("John Doe")).thenReturn(Optional.of(professor));
        when(courseRepository.findByCourseCode("CS101")).thenReturn(Optional.of(course));
        
        // Mock professor folder creation - need to mock findById for createProfessorFolder call
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.of(existingProfessorFolder));
        
        // Mock course folder creation - need to mock courseRepository.findById for createCourseFolderStructure
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(folderRepository.findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE))
                .thenReturn(Optional.of(existingCourseFolder));
        
        // Mock subfolders exist (for createCourseFolderStructure)
        String[] subfolderNames = {"Syllabus", "Exams", "Course Notes", "Assignments"};
        for (String name : subfolderNames) {
            when(folderRepository.findByPath(existingCourseFolder.getPath() + "/" + name))
                    .thenReturn(Optional.of(Folder.builder()
                            .id((long) (Math.random() * 1000))
                            .path(existingCourseFolder.getPath() + "/" + name)
                            .name(name)
                            .type(FolderType.SUBFOLDER)
                            .build()));
        }
        
        // Mock the document type folder doesn't exist
        when(folderRepository.findByPath(path)).thenReturn(Optional.empty());
        
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            if (folder.getId() == null) {
                folder.setId((long) (Math.random() * 1000));
            }
            return folder;
        });

        // Act
        Folder result = folderService.getOrCreateFolderByPath(path, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPath()).isEqualTo(path);
        // PathParser.formatDocumentTypeName converts "lecture_notes" to "Lecture notes"
        assertThat(result.getName()).isEqualTo("Lecture notes");
        assertThat(result.getType()).isEqualTo(FolderType.SUBFOLDER);
        
        verify(folderRepository).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should validate path components")
    void shouldValidatePathComponents() {
        // Arrange
        String invalidPath = "invalid/path";
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.getOrCreateFolderByPath(invalidPath, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid folder path format");
    }

    // ==================== generateProfessorFolderName Tests ====================

    @Test
    @DisplayName("Should generate folder name from normal professor name")
    void shouldGenerateFolderNameFromNormalProfessorName() {
        // Arrange
        User prof = TestDataBuilder.createProfessorUser();
        prof.setFirstName("John");
        prof.setLastName("Doe");
        prof.setId(1L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(prof));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(folderRepository.findProfessorRootFolder(1L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.empty());
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(1L);
            return folder;
        });

        // Act
        Folder result = folderService.createProfessorFolder(1L, 1L, 1L);

        // Assert
        assertThat(result.getPath()).contains("John Doe");
        assertThat(result.getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should sanitize special characters in professor name")
    void shouldSanitizeSpecialCharactersInProfessorName() {
        // Arrange
        User prof = TestDataBuilder.createProfessorUser();
        prof.setFirstName("John/Test");
        prof.setLastName("Doe*Name");
        prof.setId(2L);
        
        when(userRepository.findById(2L)).thenReturn(Optional.of(prof));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(folderRepository.findProfessorRootFolder(2L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.empty());
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(1L);
            return folder;
        });

        // Act
        Folder result = folderService.createProfessorFolder(2L, 1L, 1L);

        // Assert
        // Check that the professor folder name part doesn't contain special characters
        // The path format is: {yearCode}/{semesterType}/{professorFolderName}
        String[] pathParts = result.getPath().split("/");
        String professorFolderName = pathParts[pathParts.length - 1];
        assertThat(professorFolderName).doesNotContain("/");
        assertThat(professorFolderName).doesNotContain("*");
        assertThat(professorFolderName).contains("_");
    }

    @Test
    @DisplayName("Should use fallback format when professor name is empty")
    void shouldUseFallbackFormatWhenProfessorNameIsEmpty() {
        // Arrange
        User prof = TestDataBuilder.createProfessorUser();
        prof.setFirstName("");
        prof.setLastName("");
        prof.setId(3L);
        
        when(userRepository.findById(3L)).thenReturn(Optional.of(prof));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(folderRepository.findProfessorRootFolder(3L, 1L, 1L, FolderType.PROFESSOR_ROOT))
                .thenReturn(Optional.empty());
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(1L);
            return folder;
        });

        // Act
        Folder result = folderService.createProfessorFolder(3L, 1L, 1L);

        // Assert
        assertThat(result.getPath()).contains("prof_3");
    }
}
