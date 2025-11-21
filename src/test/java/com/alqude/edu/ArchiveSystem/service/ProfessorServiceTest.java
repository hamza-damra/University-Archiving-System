package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.user.ProfessorDTO;
import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.exception.BusinessException;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProfessorService.
 * Tests professor folder auto-creation integration.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
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

    @InjectMocks
    private ProfessorServiceImpl professorService;

    private Department department;
    private User professor;
    private AcademicYear academicYear;
    private Semester semester;
    private Folder professorFolder;

    @BeforeEach
    void setUp() {
        // Setup department
        department = new Department();
        department.setId(1L);
        department.setName("Computer Science");

        // Setup professor
        professor = new User();
        professor.setId(1L);
        professor.setEmail("prof@example.com");
        professor.setFirstName("John");
        professor.setLastName("Doe");
        professor.setRole(Role.ROLE_PROFESSOR);
        professor.setDepartment(department);
        professor.setProfessorId("PROF1");
        professor.setIsActive(true);

        // Setup academic year
        academicYear = new AcademicYear();
        academicYear.setId(1L);
        academicYear.setYearCode("2024-2025");
        academicYear.setStartYear(2024);
        academicYear.setEndYear(2025);
        academicYear.setIsActive(true);

        // Setup semester
        semester = new Semester();
        semester.setId(1L);
        semester.setAcademicYear(academicYear);
        semester.setType(SemesterType.FIRST);
        semester.setIsActive(true);

        // Setup professor folder
        professorFolder = new Folder();
        professorFolder.setId(1L);
        professorFolder.setPath("2024-2025/first/PROF1");
        professorFolder.setName("PROF1");
        professorFolder.setType(FolderType.PROFESSOR_ROOT);
        professorFolder.setOwner(professor);
        professorFolder.setAcademicYear(academicYear);
        professorFolder.setSemester(semester);
    }

    /**
     * Test 2.4.1: Verify that createProfessorFolder method calls FolderService
     */
    @Test
    void testCreateProfessorFolder_CallsFolderService() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(folderService.createProfessorFolder(1L, 1L, 1L)).thenReturn(professorFolder);

        // Act
        Folder result = professorService.createProfessorFolder(1L, 1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("2024-2025/first/PROF1", result.getPath());
        assertEquals(FolderType.PROFESSOR_ROOT, result.getType());
        verify(folderService, times(1)).createProfessorFolder(1L, 1L, 1L);
    }

    /**
     * Test 2.4.2: Verify that createProfessorFolder validates professor exists
     */
    @Test
    void testCreateProfessorFolder_ProfessorNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            professorService.createProfessorFolder(999L, 1L, 1L);
        });

        verify(folderService, never()).createProfessorFolder(anyLong(), anyLong(), anyLong());
    }

    /**
     * Test 2.4.3: Verify that createProfessorFolder validates user is a professor
     */
    @Test
    void testCreateProfessorFolder_UserNotProfessor() {
        // Arrange
        User nonProfessor = new User();
        nonProfessor.setId(2L);
        nonProfessor.setRole(Role.ROLE_DEANSHIP);
        when(userRepository.findById(2L)).thenReturn(Optional.of(nonProfessor));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            professorService.createProfessorFolder(2L, 1L, 1L);
        });

        verify(folderService, never()).createProfessorFolder(anyLong(), anyLong(), anyLong());
    }

    /**
     * Test 2.4.4: Verify that createProfessorFolder handles FolderService exceptions
     */
    @Test
    void testCreateProfessorFolder_FolderServiceThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(folderService.createProfessorFolder(1L, 1L, 1L))
                .thenThrow(new RuntimeException("File system error"));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            professorService.createProfessorFolder(1L, 1L, 1L);
        });

        verify(folderService, times(1)).createProfessorFolder(1L, 1L, 1L);
    }

    /**
     * Test 2.4.5: Verify that createProfessorFolder is idempotent
     */
    @Test
    void testCreateProfessorFolder_Idempotent() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(folderService.createProfessorFolder(1L, 1L, 1L)).thenReturn(professorFolder);

        // Act - Call twice
        Folder result1 = professorService.createProfessorFolder(1L, 1L, 1L);
        Folder result2 = professorService.createProfessorFolder(1L, 1L, 1L);

        // Assert - Both calls should succeed and return same folder
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getPath(), result2.getPath());
        verify(folderService, times(2)).createProfessorFolder(1L, 1L, 1L);
    }
}
