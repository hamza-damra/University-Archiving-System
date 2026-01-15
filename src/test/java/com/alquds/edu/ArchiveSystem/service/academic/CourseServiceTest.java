package com.alquds.edu.ArchiveSystem.service.academic;

import com.alquds.edu.ArchiveSystem.dto.academic.CourseAssignmentDTO;
import com.alquds.edu.ArchiveSystem.dto.academic.CourseDTO;
import com.alquds.edu.ArchiveSystem.entity.academic.*;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.exception.core.BusinessException;
import com.alquds.edu.ArchiveSystem.exception.core.DuplicateEntityException;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.repository.academic.*;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.service.core.DepartmentScopedFilterService;
import com.alquds.edu.ArchiveSystem.service.file.FolderService;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CourseService following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Unit tests with mocked dependencies
 * - Test business logic in isolation
 * - Follow AAA pattern
 * - Test edge cases and validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CourseService Unit Tests")
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    
    @Mock
    private CourseAssignmentRepository courseAssignmentRepository;
    
    @Mock
    private com.alquds.edu.ArchiveSystem.repository.submission.RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    
    @Mock
    private DepartmentRepository departmentRepository;
    
    @Mock
    private SemesterRepository semesterRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DepartmentScopedFilterService departmentScopedFilterService;
    
    @Mock
    private FolderService folderService;
    
    @InjectMocks
    private CourseServiceImpl courseService;
    
    private Department testDepartment;
    private Course testCourse;
    private AcademicYear testAcademicYear;
    private Semester testSemester;
    private User testProfessor;
    
    @BeforeEach
    void setUp() {
        testDepartment = TestDataBuilder.createDepartment();
        testDepartment.setId(1L);
        
        testCourse = TestDataBuilder.createCourse();
        testCourse.setId(1L);
        testCourse.setDepartment(testDepartment);
        
        testAcademicYear = TestDataBuilder.createAcademicYear();
        testAcademicYear.setId(1L);
        
        testSemester = TestDataBuilder.createSemester();
        testSemester.setId(1L);
        testSemester.setAcademicYear(testAcademicYear);
        
        testProfessor = TestDataBuilder.createProfessorUser();
        testProfessor.setId(1L);
        testProfessor.setDepartment(testDepartment);
    }
    
    // ==================== Create Course Tests ====================
    
    @Test
    @DisplayName("Should create course successfully when all validations pass")
    void shouldCreateCourseSuccessfully() {
        // Arrange
        CourseDTO courseDTO = TestDataBuilder.createCourseDTO();
        courseDTO.setDepartmentId(1L);
        
        Course savedCourse = TestDataBuilder.createCourse();
        savedCourse.setId(1L);
        savedCourse.setDepartment(testDepartment);
        
        when(courseRepository.findByCourseCode(courseDTO.getCourseCode())).thenReturn(Optional.empty());
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);
        
        // Act
        Course result = courseService.createCourse(courseDTO);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCourseCode()).isEqualTo(courseDTO.getCourseCode());
        
        verify(courseRepository).findByCourseCode(courseDTO.getCourseCode());
        verify(departmentRepository).findById(1L);
        verify(courseRepository).save(any(Course.class));
    }
    
    @Test
    @DisplayName("Should throw exception when course code already exists")
    void shouldThrowExceptionWhenCourseCodeExists() {
        // Arrange
        CourseDTO courseDTO = TestDataBuilder.createCourseDTO();
        courseDTO.setCourseCode("CS101");
        courseDTO.setDepartmentId(1L);
        
        Course existingCourse = TestDataBuilder.createCourse();
        existingCourse.setCourseCode("CS101");
        
        when(courseRepository.findByCourseCode("CS101")).thenReturn(Optional.of(existingCourse));
        
        // Act & Assert
        assertThatThrownBy(() -> courseService.createCourse(courseDTO))
                .isInstanceOf(DuplicateEntityException.class)
                .hasMessageContaining("already exists");
        
        verify(courseRepository).findByCourseCode("CS101");
        verify(courseRepository, never()).save(any(Course.class));
    }
    
    @Test
    @DisplayName("Should throw exception when department not found")
    void shouldThrowExceptionWhenDepartmentNotFound() {
        // Arrange
        CourseDTO courseDTO = TestDataBuilder.createCourseDTO();
        courseDTO.setDepartmentId(999L);
        
        when(courseRepository.findByCourseCode(anyString())).thenReturn(Optional.empty());
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> courseService.createCourse(courseDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Department not found");
        
        verify(departmentRepository).findById(999L);
        verify(courseRepository, never()).save(any(Course.class));
    }
    
    // ==================== Update Course Tests ====================
    
    @Test
    @DisplayName("Should update course successfully")
    void shouldUpdateCourseSuccessfully() {
        // Arrange
        Long courseId = 1L;
        CourseDTO courseDTO = TestDataBuilder.createCourseDTO();
        courseDTO.setCourseCode("CS101");
        courseDTO.setCourseName("Updated Course Name");
        courseDTO.setDepartmentId(1L);
        
        Course existingCourse = TestDataBuilder.createCourse();
        existingCourse.setId(courseId);
        existingCourse.setCourseCode("CS101");
        existingCourse.setDepartment(testDepartment);
        
        Course updatedCourse = TestDataBuilder.createCourse();
        updatedCourse.setId(courseId);
        updatedCourse.setCourseCode("CS101");
        updatedCourse.setCourseName("Updated Course Name");
        updatedCourse.setDepartment(testDepartment);
        
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);
        
        // Act
        Course result = courseService.updateCourse(courseId, courseDTO);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCourseName()).isEqualTo("Updated Course Name");
        verify(courseRepository).findById(courseId);
        verify(courseRepository).save(existingCourse);
    }
    
    @Test
    @DisplayName("Should throw exception when course not found for update")
    void shouldThrowExceptionWhenCourseNotFoundForUpdate() {
        // Arrange
        Long courseId = 999L;
        CourseDTO courseDTO = TestDataBuilder.createCourseDTO();
        
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> courseService.updateCourse(courseId, courseDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
        
        verify(courseRepository).findById(courseId);
        verify(courseRepository, never()).save(any(Course.class));
    }
    
    // ==================== Get Course Tests ====================
    
    @Test
    @DisplayName("Should retrieve course by ID successfully")
    void shouldRetrieveCourseByIdSuccessfully() {
        // Arrange
        Long courseId = 1L;
        
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        
        // Act
        Course result = courseService.getCourse(courseId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(courseId);
        verify(courseRepository).findById(courseId);
    }
    
    @Test
    @DisplayName("Should throw exception when course not found")
    void shouldThrowExceptionWhenCourseNotFound() {
        // Arrange
        Long courseId = 999L;
        
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> courseService.getCourse(courseId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }
    
    // ==================== Course Assignment Tests ====================
    
    @Test
    @DisplayName("Should assign course to professor successfully")
    void shouldAssignCourseToProfessorSuccessfully() {
        // Arrange
        CourseAssignmentDTO assignmentDTO = TestDataBuilder.createCourseAssignmentDTO();
        assignmentDTO.setCourseId(1L);
        assignmentDTO.setProfessorId(1L);
        assignmentDTO.setSemesterId(1L);
        
        CourseAssignment savedAssignment = TestDataBuilder.createCourseAssignment();
        savedAssignment.setId(1L);
        savedAssignment.setCourse(testCourse);
        savedAssignment.setProfessor(testProfessor);
        savedAssignment.setSemester(testSemester);
        
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(testSemester));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testProfessor));
        when(courseAssignmentRepository.save(any(CourseAssignment.class))).thenReturn(savedAssignment);
        when(folderService.createCourseFolderStructure(anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(java.util.Collections.emptyList());
        
        // Act
        CourseAssignment result = courseService.assignCourse(assignmentDTO);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(semesterRepository).findById(1L);
        verify(courseRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(courseAssignmentRepository).save(any(CourseAssignment.class));
        verify(folderService).createCourseFolderStructure(anyLong(), anyLong(), anyLong(), anyLong());
    }
    
    @Test
    @DisplayName("Should throw exception when assigning inactive course")
    void shouldThrowExceptionWhenAssigningInactiveCourse() {
        // Arrange
        CourseAssignmentDTO assignmentDTO = TestDataBuilder.createCourseAssignmentDTO();
        assignmentDTO.setCourseId(1L);
        assignmentDTO.setProfessorId(1L);
        assignmentDTO.setSemesterId(1L);
        
        Course inactiveCourse = TestDataBuilder.createCourse();
        inactiveCourse.setId(1L);
        inactiveCourse.setIsActive(false);
        
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(testSemester));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(inactiveCourse));
        
        // Act & Assert
        assertThatThrownBy(() -> courseService.assignCourse(assignmentDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
        
        verify(courseRepository).findById(1L);
        verify(courseAssignmentRepository, never()).save(any(CourseAssignment.class));
    }
    
    @Test
    @DisplayName("Should throw exception when user is not a professor")
    void shouldThrowExceptionWhenUserIsNotProfessor() {
        // Arrange
        CourseAssignmentDTO assignmentDTO = TestDataBuilder.createCourseAssignmentDTO();
        assignmentDTO.setCourseId(1L);
        assignmentDTO.setProfessorId(1L);
        assignmentDTO.setSemesterId(1L);
        
        User adminUser = TestDataBuilder.createAdminUser();
        adminUser.setId(1L);
        adminUser.setRole(Role.ROLE_ADMIN);
        
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(testSemester));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        
        // Act & Assert
        assertThatThrownBy(() -> courseService.assignCourse(assignmentDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not a professor");
        
        verify(userRepository).findById(1L);
        verify(courseAssignmentRepository, never()).save(any(CourseAssignment.class));
    }
    
    @Test
    @DisplayName("Should throw exception when professor is inactive")
    void shouldThrowExceptionWhenProfessorIsInactive() {
        // Arrange
        CourseAssignmentDTO assignmentDTO = TestDataBuilder.createCourseAssignmentDTO();
        assignmentDTO.setCourseId(1L);
        assignmentDTO.setProfessorId(1L);
        assignmentDTO.setSemesterId(1L);
        
        User inactiveProfessor = TestDataBuilder.createProfessorUser();
        inactiveProfessor.setId(1L);
        inactiveProfessor.setIsActive(false);
        
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(testSemester));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(inactiveProfessor));
        
        // Act & Assert
        assertThatThrownBy(() -> courseService.assignCourse(assignmentDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
        
        verify(userRepository).findById(1L);
        verify(courseAssignmentRepository, never()).save(any(CourseAssignment.class));
    }
    
    // ==================== Deactivate Course Tests ====================
    
    @Test
    @DisplayName("Should deactivate course successfully")
    void shouldDeactivateCourseSuccessfully() {
        // Arrange
        Long courseId = 1L;
        Course activeCourse = TestDataBuilder.createCourse();
        activeCourse.setId(courseId);
        activeCourse.setIsActive(true);
        
        Course deactivatedCourse = TestDataBuilder.createCourse();
        deactivatedCourse.setId(courseId);
        deactivatedCourse.setIsActive(false);
        
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(activeCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(deactivatedCourse);
        
        // Act
        courseService.deactivateCourse(courseId);
        
        // Assert
        verify(courseRepository).findById(courseId);
        verify(courseRepository).save(argThat(course -> !course.getIsActive()));
    }
    
    @Test
    @DisplayName("Should throw exception when course not found for deactivation")
    void shouldThrowExceptionWhenCourseNotFoundForDeactivation() {
        // Arrange
        Long courseId = 999L;
        
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> courseService.deactivateCourse(courseId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
        
        verify(courseRepository, never()).save(any(Course.class));
    }
}
