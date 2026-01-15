package com.alquds.edu.ArchiveSystem.service.submission;

import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.SemesterRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.RequiredDocumentTypeRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.service.core.DepartmentScopedFilterService;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SubmissionService following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Unit tests with mocked dependencies
 * - Test business logic in isolation
 * - Follow AAA pattern
 * - Test submission creation, updates, status calculation, and statistics
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionService Unit Tests")
class SubmissionServiceTest {

    @Mock
    private DocumentSubmissionRepository documentSubmissionRepository;
    
    @Mock
    private CourseAssignmentRepository courseAssignmentRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    
    @Mock
    private SemesterRepository semesterRepository;
    
    @Mock
    private DepartmentScopedFilterService departmentScopedFilterService;
    
    @InjectMocks
    private SubmissionServiceImpl submissionService;
    
    private CourseAssignment testCourseAssignment;
    private User testProfessor;
    private Semester testSemester;
    private DocumentSubmission testSubmission;
    
    @BeforeEach
    void setUp() {
        testSemester = TestDataBuilder.createSemester();
        testSemester.setId(1L);
        
        testProfessor = TestDataBuilder.createProfessorUser();
        testProfessor.setId(1L);
        
        testCourseAssignment = TestDataBuilder.createCourseAssignment();
        testCourseAssignment.setId(1L);
        testCourseAssignment.setProfessor(testProfessor);
        testCourseAssignment.setSemester(testSemester);
        
        testSubmission = new DocumentSubmission();
        testSubmission.setId(1L);
        testSubmission.setCourseAssignment(testCourseAssignment);
        testSubmission.setProfessor(testProfessor);
        testSubmission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        testSubmission.setStatus(SubmissionStatus.NOT_UPLOADED);
        testSubmission.setFileCount(0);
    }
    
    // ==================== Create Submission Tests ====================
    
    @Test
    @DisplayName("Should create submission successfully when all validations pass")
    void shouldCreateSubmissionSuccessfully() {
        // Arrange
        Long courseAssignmentId = 1L;
        DocumentTypeEnum documentType = DocumentTypeEnum.SYLLABUS;
        Long professorId = 1L;
        String notes = "Test notes";
        
        DocumentSubmission savedSubmission = new DocumentSubmission();
        savedSubmission.setId(1L);
        savedSubmission.setCourseAssignment(testCourseAssignment);
        savedSubmission.setProfessor(testProfessor);
        savedSubmission.setDocumentType(documentType);
        savedSubmission.setNotes(notes);
        savedSubmission.setStatus(SubmissionStatus.NOT_UPLOADED);
        savedSubmission.setFileCount(0);
        
        when(courseAssignmentRepository.findById(courseAssignmentId)).thenReturn(Optional.of(testCourseAssignment));
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));
        when(documentSubmissionRepository.findByCourseAssignmentIdAndDocumentType(courseAssignmentId, documentType))
                .thenReturn(Optional.empty());
        when(documentSubmissionRepository.save(any(DocumentSubmission.class))).thenReturn(savedSubmission);
        
        // Act
        DocumentSubmission result = submissionService.createSubmission(
                courseAssignmentId, documentType, professorId, notes);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDocumentType()).isEqualTo(documentType);
        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.NOT_UPLOADED);
        assertThat(result.getFileCount()).isEqualTo(0);
        
        verify(courseAssignmentRepository).findById(courseAssignmentId);
        verify(userRepository).findById(professorId);
        verify(documentSubmissionRepository).save(any(DocumentSubmission.class));
    }
    
    @Test
    @DisplayName("Should throw exception when course assignment not found")
    void shouldThrowExceptionWhenCourseAssignmentNotFound() {
        // Arrange
        Long courseAssignmentId = 999L;
        DocumentTypeEnum documentType = DocumentTypeEnum.SYLLABUS;
        Long professorId = 1L;
        
        when(courseAssignmentRepository.findById(courseAssignmentId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> submissionService.createSubmission(
                courseAssignmentId, documentType, professorId, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Course assignment not found");
        
        verify(documentSubmissionRepository, never()).save(any(DocumentSubmission.class));
    }
    
    @Test
    @DisplayName("Should throw exception when professor not found")
    void shouldThrowExceptionWhenProfessorNotFound() {
        // Arrange
        Long courseAssignmentId = 1L;
        DocumentTypeEnum documentType = DocumentTypeEnum.SYLLABUS;
        Long professorId = 999L;
        
        when(courseAssignmentRepository.findById(courseAssignmentId)).thenReturn(Optional.of(testCourseAssignment));
        when(userRepository.findById(professorId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> submissionService.createSubmission(
                courseAssignmentId, documentType, professorId, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Professor not found");
        
        verify(documentSubmissionRepository, never()).save(any(DocumentSubmission.class));
    }
    
    @Test
    @DisplayName("Should throw exception when submission already exists")
    void shouldThrowExceptionWhenSubmissionAlreadyExists() {
        // Arrange
        Long courseAssignmentId = 1L;
        DocumentTypeEnum documentType = DocumentTypeEnum.SYLLABUS;
        Long professorId = 1L;
        
        DocumentSubmission existingSubmission = new DocumentSubmission();
        existingSubmission.setId(1L);
        
        when(courseAssignmentRepository.findById(courseAssignmentId)).thenReturn(Optional.of(testCourseAssignment));
        when(userRepository.findById(professorId)).thenReturn(Optional.of(testProfessor));
        when(documentSubmissionRepository.findByCourseAssignmentIdAndDocumentType(courseAssignmentId, documentType))
                .thenReturn(Optional.of(existingSubmission));
        
        // Act & Assert
        assertThatThrownBy(() -> submissionService.createSubmission(
                courseAssignmentId, documentType, professorId, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
        
        verify(documentSubmissionRepository, never()).save(any(DocumentSubmission.class));
    }
    
    // ==================== Get Submission Tests ====================
    
    @Test
    @DisplayName("Should retrieve submission by ID successfully")
    void shouldRetrieveSubmissionByIdSuccessfully() {
        // Arrange
        Long submissionId = 1L;
        
        when(documentSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(testSubmission));
        
        // Act
        DocumentSubmission result = submissionService.getSubmission(submissionId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(submissionId);
        verify(documentSubmissionRepository).findById(submissionId);
    }
    
    @Test
    @DisplayName("Should throw exception when submission not found")
    void shouldThrowExceptionWhenSubmissionNotFound() {
        // Arrange
        Long submissionId = 999L;
        
        when(documentSubmissionRepository.findById(submissionId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> submissionService.getSubmission(submissionId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }
    
    // ==================== Calculate Status Tests ====================
    
    @Test
    @DisplayName("Should calculate status as NOT_UPLOADED when no files")
    void shouldCalculateStatusAsNotUploadedWhenNoFiles() {
        // Arrange
        DocumentSubmission submission = new DocumentSubmission();
        submission.setFileCount(0);
        submission.setStatus(SubmissionStatus.NOT_UPLOADED);
        
        // Act
        SubmissionStatus result = submissionService.calculateSubmissionStatus(submission, null);
        
        // Assert
        assertThat(result).isEqualTo(SubmissionStatus.NOT_UPLOADED);
        assertThat(submission.getIsLateSubmission()).isFalse();
    }
    
    @Test
    @DisplayName("Should calculate status as OVERDUE when no files and deadline passed")
    void shouldCalculateStatusAsOverdueWhenNoFilesAndDeadlinePassed() {
        // Arrange
        DocumentSubmission submission = new DocumentSubmission();
        submission.setFileCount(0);
        LocalDateTime deadline = LocalDateTime.now().minusDays(1); // Yesterday
        
        // Act
        SubmissionStatus result = submissionService.calculateSubmissionStatus(submission, deadline);
        
        // Assert
        assertThat(result).isEqualTo(SubmissionStatus.OVERDUE);
        assertThat(submission.getIsLateSubmission()).isFalse();
    }
    
    @Test
    @DisplayName("Should calculate status as UPLOADED when files uploaded and no deadline")
    void shouldCalculateStatusAsUploadedWhenFilesUploadedAndNoDeadline() {
        // Arrange
        DocumentSubmission submission = new DocumentSubmission();
        submission.setFileCount(5);
        submission.setSubmittedAt(LocalDateTime.now());
        
        // Act
        SubmissionStatus result = submissionService.calculateSubmissionStatus(submission, null);
        
        // Assert
        assertThat(result).isEqualTo(SubmissionStatus.UPLOADED);
        assertThat(submission.getIsLateSubmission()).isFalse();
    }
    
    @Test
    @DisplayName("Should calculate status as UPLOADED with late flag when submitted after deadline")
    void shouldCalculateStatusAsUploadedLateWhenSubmittedAfterDeadline() {
        // Arrange
        DocumentSubmission submission = new DocumentSubmission();
        submission.setFileCount(5);
        submission.setSubmittedAt(LocalDateTime.now());
        LocalDateTime deadline = LocalDateTime.now().minusDays(1); // Yesterday
        
        // Act
        SubmissionStatus result = submissionService.calculateSubmissionStatus(submission, deadline);
        
        // Assert
        assertThat(result).isEqualTo(SubmissionStatus.UPLOADED);
        assertThat(submission.getIsLateSubmission()).isTrue();
    }
    
    @Test
    @DisplayName("Should calculate status as UPLOADED on time when submitted before deadline")
    void shouldCalculateStatusAsUploadedOnTimeWhenSubmittedBeforeDeadline() {
        // Arrange
        DocumentSubmission submission = new DocumentSubmission();
        submission.setFileCount(5);
        submission.setSubmittedAt(LocalDateTime.now().minusDays(2)); // 2 days ago
        LocalDateTime deadline = LocalDateTime.now().plusDays(1); // Tomorrow
        
        // Act
        SubmissionStatus result = submissionService.calculateSubmissionStatus(submission, deadline);
        
        // Assert
        assertThat(result).isEqualTo(SubmissionStatus.UPLOADED);
        assertThat(submission.getIsLateSubmission()).isFalse();
    }
    
    // ==================== Get Submissions by Professor Tests ====================
    
    @Test
    @DisplayName("Should retrieve submissions by professor and semester successfully")
    void shouldRetrieveSubmissionsByProfessorAndSemesterSuccessfully() {
        // Arrange
        Long professorId = 1L;
        Long semesterId = 1L;
        
        DocumentSubmission submission1 = new DocumentSubmission();
        submission1.setId(1L);
        DocumentSubmission submission2 = new DocumentSubmission();
        submission2.setId(2L);
        
        when(userRepository.existsById(professorId)).thenReturn(true);
        when(semesterRepository.existsById(semesterId)).thenReturn(true);
        when(documentSubmissionRepository.findByProfessorIdAndCourseAssignment_SemesterId(professorId, semesterId))
                .thenReturn(List.of(submission1, submission2));
        
        // Act
        List<DocumentSubmission> result = submissionService.getSubmissionsByProfessor(professorId, semesterId);
        
        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).contains(submission1, submission2);
        verify(userRepository).existsById(professorId);
        verify(semesterRepository).existsById(semesterId);
    }
    
    @Test
    @DisplayName("Should throw exception when professor not found")
    void shouldThrowExceptionWhenProfessorNotFoundForSubmissions() {
        // Arrange
        Long professorId = 999L;
        Long semesterId = 1L;
        
        when(userRepository.existsById(professorId)).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> submissionService.getSubmissionsByProfessor(professorId, semesterId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Professor not found");
    }
    
    @Test
    @DisplayName("Should throw exception when semester not found")
    void shouldThrowExceptionWhenSemesterNotFoundForSubmissions() {
        // Arrange
        Long professorId = 1L;
        Long semesterId = 999L;
        
        when(userRepository.existsById(professorId)).thenReturn(true);
        when(semesterRepository.existsById(semesterId)).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> submissionService.getSubmissionsByProfessor(professorId, semesterId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Semester not found");
    }
    
    // ==================== Get Submissions by Course Tests ====================
    
    @Test
    @DisplayName("Should retrieve submissions by course assignment successfully")
    void shouldRetrieveSubmissionsByCourseAssignmentSuccessfully() {
        // Arrange
        Long courseAssignmentId = 1L;
        
        DocumentSubmission submission1 = new DocumentSubmission();
        submission1.setId(1L);
        DocumentSubmission submission2 = new DocumentSubmission();
        submission2.setId(2L);
        
        when(courseAssignmentRepository.existsById(courseAssignmentId)).thenReturn(true);
        when(documentSubmissionRepository.findByCourseAssignmentId(courseAssignmentId))
                .thenReturn(List.of(submission1, submission2));
        
        // Act
        List<DocumentSubmission> result = submissionService.getSubmissionsByCourse(courseAssignmentId);
        
        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).contains(submission1, submission2);
        verify(courseAssignmentRepository).existsById(courseAssignmentId);
    }
    
    @Test
    @DisplayName("Should throw exception when course assignment not found")
    void shouldThrowExceptionWhenCourseAssignmentNotFoundForSubmissions() {
        // Arrange
        Long courseAssignmentId = 999L;
        
        when(courseAssignmentRepository.existsById(courseAssignmentId)).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> submissionService.getSubmissionsByCourse(courseAssignmentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Course assignment not found");
    }
}
