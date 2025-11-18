package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.academic.CourseAssignmentDTO;
import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.exception.BusinessException;
import com.alqude.edu.ArchiveSystem.exception.DuplicateEntityException;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseAssignmentRepository courseAssignmentRepository;

    @Mock
    private RequiredDocumentTypeRepository requiredDocumentTypeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentScopedFilterService departmentScopedFilterService;

    @InjectMocks
    private CourseServiceImpl courseService;

    private CourseAssignmentDTO courseAssignmentDTO;
    private Semester semester;
    private Course course;
    private User professor;
    private Department department;

    @BeforeEach
    void setUp() {
        // Setup department
        department = new Department();
        department.setId(1L);
        department.setName("Computer Science");

        // Setup semester
        semester = new Semester();
        semester.setId(1L);
        semester.setType(SemesterType.FIRST);

        // Setup course
        course = new Course();
        course.setId(1L);
        course.setCourseCode("CS101");
        course.setCourseName("Introduction to Programming");
        course.setDepartment(department);
        course.setIsActive(true);

        // Setup professor
        professor = new User();
        professor.setId(1L);
        professor.setEmail("professor@example.com");
        professor.setRole(Role.ROLE_PROFESSOR);
        professor.setIsActive(true);
        professor.setDepartment(department);

        // Setup DTO
        courseAssignmentDTO = new CourseAssignmentDTO();
        courseAssignmentDTO.setSemesterId(1L);
        courseAssignmentDTO.setCourseId(1L);
        courseAssignmentDTO.setProfessorId(1L);
        courseAssignmentDTO.setIsActive(true);
    }

    @Test
    void testAssignCourse_CreatesAssignment() {
        // Arrange
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        
        CourseAssignment savedAssignment = new CourseAssignment();
        savedAssignment.setId(1L);
        savedAssignment.setSemester(semester);
        savedAssignment.setCourse(course);
        savedAssignment.setProfessor(professor);
        savedAssignment.setIsActive(true);
        
        when(courseAssignmentRepository.save(any(CourseAssignment.class))).thenReturn(savedAssignment);

        // Act
        CourseAssignment result = courseService.assignCourse(courseAssignmentDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(semester, result.getSemester());
        assertEquals(course, result.getCourse());
        assertEquals(professor, result.getProfessor());
        assertTrue(result.getIsActive());
        
        verify(courseAssignmentRepository, times(1)).save(any(CourseAssignment.class));
    }

    @Test
    void testAssignCourse_ThrowsExceptionWhenSemesterNotFound() {
        // Arrange
        when(semesterRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            courseService.assignCourse(courseAssignmentDTO);
        });
        
        verify(courseAssignmentRepository, never()).save(any(CourseAssignment.class));
    }

    @Test
    void testAssignCourse_ThrowsExceptionWhenCourseNotFound() {
        // Arrange
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            courseService.assignCourse(courseAssignmentDTO);
        });
        
        verify(courseAssignmentRepository, never()).save(any(CourseAssignment.class));
    }

    @Test
    void testAssignCourse_ThrowsExceptionWhenCourseIsInactive() {
        // Arrange
        course.setIsActive(false);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            courseService.assignCourse(courseAssignmentDTO);
        });
        
        verify(courseAssignmentRepository, never()).save(any(CourseAssignment.class));
    }

    @Test
    void testAssignCourse_ThrowsExceptionWhenProfessorNotFound() {
        // Arrange
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            courseService.assignCourse(courseAssignmentDTO);
        });
        
        verify(courseAssignmentRepository, never()).save(any(CourseAssignment.class));
    }

    @Test
    void testAssignCourse_ThrowsExceptionWhenUserIsNotProfessor() {
        // Arrange
        professor.setRole(Role.ROLE_HOD);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            courseService.assignCourse(courseAssignmentDTO);
        });
        
        verify(courseAssignmentRepository, never()).save(any(CourseAssignment.class));
    }

    @Test
    void testAssignCourse_ThrowsExceptionWhenProfessorIsInactive() {
        // Arrange
        professor.setIsActive(false);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            courseService.assignCourse(courseAssignmentDTO);
        });
        
        verify(courseAssignmentRepository, never()).save(any(CourseAssignment.class));
    }

    @Test
    void testAssignCourse_ThrowsExceptionWhenAssignmentAlreadyExists() {
        // Arrange
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(courseAssignmentRepository.save(any(CourseAssignment.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        // Act & Assert
        assertThrows(DuplicateEntityException.class, () -> {
            courseService.assignCourse(courseAssignmentDTO);
        });
    }

    @Test
    void testGetAssignmentsByProfessor_FiltersCorrectly() {
        // Arrange
        Long professorId = 1L;
        Long semesterId = 1L;
        
        CourseAssignment assignment1 = new CourseAssignment();
        assignment1.setId(1L);
        assignment1.setProfessor(professor);
        assignment1.setSemester(semester);
        assignment1.setCourse(course);
        
        Course course2 = new Course();
        course2.setId(2L);
        course2.setCourseCode("CS102");
        course2.setCourseName("Data Structures");
        
        CourseAssignment assignment2 = new CourseAssignment();
        assignment2.setId(2L);
        assignment2.setProfessor(professor);
        assignment2.setSemester(semester);
        assignment2.setCourse(course2);
        
        List<CourseAssignment> expectedAssignments = List.of(assignment1, assignment2);
        
        when(userRepository.existsById(professorId)).thenReturn(true);
        when(semesterRepository.existsById(semesterId)).thenReturn(true);
        when(courseAssignmentRepository.findByProfessorIdAndSemesterId(professorId, semesterId))
            .thenReturn(expectedAssignments);

        // Act
        List<CourseAssignment> result = courseService.getAssignmentsByProfessor(professorId, semesterId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(assignment1, result.get(0));
        assertEquals(assignment2, result.get(1));
        
        // Verify that all assignments belong to the same professor
        assertTrue(result.stream().allMatch(a -> a.getProfessor().getId().equals(professorId)));
        
        // Verify that all assignments belong to the same semester
        assertTrue(result.stream().allMatch(a -> a.getSemester().getId().equals(semesterId)));
    }

    @Test
    void testGetAssignmentsByProfessor_ThrowsExceptionWhenProfessorNotFound() {
        // Arrange
        Long professorId = 999L;
        Long semesterId = 1L;
        
        when(userRepository.existsById(professorId)).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            courseService.getAssignmentsByProfessor(professorId, semesterId);
        });
        
        verify(courseAssignmentRepository, never()).findByProfessorIdAndSemesterId(anyLong(), anyLong());
    }

    @Test
    void testGetAssignmentsByProfessor_ThrowsExceptionWhenSemesterNotFound() {
        // Arrange
        Long professorId = 1L;
        Long semesterId = 999L;
        
        when(userRepository.existsById(professorId)).thenReturn(true);
        when(semesterRepository.existsById(semesterId)).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            courseService.getAssignmentsByProfessor(professorId, semesterId);
        });
        
        verify(courseAssignmentRepository, never()).findByProfessorIdAndSemesterId(anyLong(), anyLong());
    }

    @Test
    void testGetAssignmentsByProfessor_ReturnsEmptyListWhenNoCourses() {
        // Arrange
        Long professorId = 1L;
        Long semesterId = 1L;
        
        when(userRepository.existsById(professorId)).thenReturn(true);
        when(semesterRepository.existsById(semesterId)).thenReturn(true);
        when(courseAssignmentRepository.findByProfessorIdAndSemesterId(professorId, semesterId))
            .thenReturn(List.of());

        // Act
        List<CourseAssignment> result = courseService.getAssignmentsByProfessor(professorId, semesterId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
