package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.report.SubmissionStatistics;
import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    private DocumentSubmission submission;
    private CourseAssignment courseAssignment;
    private User professor;
    private Department department;
    private Course course;
    private Semester semester;
    private AcademicYear academicYear;

    @BeforeEach
    void setUp() {
        // Setup department
        department = new Department();
        department.setId(1L);
        department.setName("Computer Science");

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
        course.setCourseName("Introduction to Programming");
        course.setDepartment(department);

        // Setup professor
        professor = new User();
        professor.setId(1L);
        professor.setEmail("professor@example.com");
        professor.setRole(Role.ROLE_PROFESSOR);
        professor.setDepartment(department);

        // Setup course assignment
        courseAssignment = new CourseAssignment();
        courseAssignment.setId(1L);
        courseAssignment.setSemester(semester);
        courseAssignment.setCourse(course);
        courseAssignment.setProfessor(professor);

        // Setup submission
        submission = new DocumentSubmission();
        submission.setId(1L);
        submission.setCourseAssignment(courseAssignment);
        submission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        submission.setProfessor(professor);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setFileCount(0);
        submission.setTotalFileSize(0L);
    }

    @Test
    void testCalculateSubmissionStatus_NotUploadedBeforeDeadline() {
        // Arrange
        submission.setFileCount(0);
        LocalDateTime deadline = LocalDateTime.now().plusDays(7); // Deadline in future

        // Act
        SubmissionStatus result = submissionService.calculateSubmissionStatus(submission, deadline);

        // Assert
        assertEquals(SubmissionStatus.NOT_UPLOADED, result);
        assertEquals(SubmissionStatus.NOT_UPLOADED, submission.getStatus());
        assertFalse(submission.getIsLateSubmission());
    }

    @Test
    void testCalculateSubmissionStatus_OverdueWhenNoFilesAndDeadlinePassed() {
        // Arrange
        submission.setFileCount(0);
        LocalDateTime deadline = LocalDateTime.now().minusDays(7); // Deadline in past

        // Act
        SubmissionStatus result = submissionService.calculateSubmissionStatus(submission, deadline);

        // Assert
        assertEquals(SubmissionStatus.OVERDUE, result);
        assertEquals(SubmissionStatus.OVERDUE, submission.getStatus());
        assertFalse(submission.getIsLateSubmission()); // No submission was made
    }

    @Test
    void testCalculateSubmissionStatus_UploadedOnTime() {
        // Arrange
        submission.setFileCount(2);
        LocalDateTime deadline = LocalDateTime.now().plusDays(7); // Deadline in future
        submission.setSubmittedAt(LocalDateTime.now().minusDays(1)); // Submitted before deadline

        // Act
        SubmissionStatus result = submissionService.calculateSubmissionStatus(submission, deadline);

        // Assert
        assertEquals(SubmissionStatus.UPLOADED, result);
        assertEquals(SubmissionStatus.UPLOADED, submission.getStatus());
        assertFalse(submission.getIsLateSubmission());
    }

    @Test
    void testCalculateSubmissionStatus_UploadedLate() {
        // Arrange
        submission.setFileCount(2);
        LocalDateTime deadline = LocalDateTime.now().minusDays(7); // Deadline in past
        submission.setSubmittedAt(LocalDateTime.now().minusDays(1)); // Submitted after deadline

        // Act
        SubmissionStatus result = submissionService.calculateSubmissionStatus(submission, deadline);

        // Assert
        assertEquals(SubmissionStatus.UPLOADED, result);
        assertEquals(SubmissionStatus.UPLOADED, submission.getStatus());
        assertTrue(submission.getIsLateSubmission());
    }

    @Test
    void testCalculateSubmissionStatus_UploadedWithNoDeadline() {
        // Arrange
        submission.setFileCount(2);
        LocalDateTime deadline = null; // No deadline specified

        // Act
        SubmissionStatus result = submissionService.calculateSubmissionStatus(submission, deadline);

        // Assert
        assertEquals(SubmissionStatus.UPLOADED, result);
        assertEquals(SubmissionStatus.UPLOADED, submission.getStatus());
        assertFalse(submission.getIsLateSubmission());
    }

    @Test
    void testCalculateSubmissionStatus_NotUploadedWithNoDeadline() {
        // Arrange
        submission.setFileCount(0);
        LocalDateTime deadline = null; // No deadline specified

        // Act
        SubmissionStatus result = submissionService.calculateSubmissionStatus(submission, deadline);

        // Assert
        assertEquals(SubmissionStatus.NOT_UPLOADED, result);
        assertEquals(SubmissionStatus.NOT_UPLOADED, submission.getStatus());
        assertFalse(submission.getIsLateSubmission());
    }

    @Test
    void testGetStatisticsBySemester_AggregatesCorrectly() {
        // Arrange
        Long semesterId = 1L;
        Long departmentId = 1L;

        // Setup course assignments
        CourseAssignment assignment1 = new CourseAssignment();
        assignment1.setId(1L);
        assignment1.setSemester(semester);
        assignment1.setCourse(course);
        assignment1.setProfessor(professor);

        User professor2 = new User();
        professor2.setId(2L);
        professor2.setEmail("professor2@example.com");
        professor2.setDepartment(department);

        Course course2 = new Course();
        course2.setId(2L);
        course2.setCourseCode("CS102");
        course2.setDepartment(department);

        CourseAssignment assignment2 = new CourseAssignment();
        assignment2.setId(2L);
        assignment2.setSemester(semester);
        assignment2.setCourse(course2);
        assignment2.setProfessor(professor2);

        List<CourseAssignment> assignments = Arrays.asList(assignment1, assignment2);

        // Setup required document types
        RequiredDocumentType reqDoc1 = new RequiredDocumentType();
        reqDoc1.setId(1L);
        reqDoc1.setCourse(course);
        reqDoc1.setSemester(semester);
        reqDoc1.setDocumentType(DocumentTypeEnum.SYLLABUS);
        reqDoc1.setDeadline(LocalDateTime.now().plusDays(7));

        RequiredDocumentType reqDoc2 = new RequiredDocumentType();
        reqDoc2.setId(2L);
        reqDoc2.setCourse(course);
        reqDoc2.setSemester(semester);
        reqDoc2.setDocumentType(DocumentTypeEnum.EXAM);
        reqDoc2.setDeadline(LocalDateTime.now().minusDays(7)); // Past deadline

        RequiredDocumentType reqDoc3 = new RequiredDocumentType();
        reqDoc3.setId(3L);
        reqDoc3.setCourse(course2);
        reqDoc3.setSemester(semester);
        reqDoc3.setDocumentType(DocumentTypeEnum.SYLLABUS);
        reqDoc3.setDeadline(LocalDateTime.now().plusDays(7));

        // Setup submissions
        DocumentSubmission sub1 = new DocumentSubmission();
        sub1.setId(1L);
        sub1.setCourseAssignment(assignment1);
        sub1.setDocumentType(DocumentTypeEnum.SYLLABUS);
        sub1.setFileCount(2);
        sub1.setSubmittedAt(LocalDateTime.now().minusDays(1));

        when(semesterRepository.existsById(semesterId)).thenReturn(true);
        when(courseAssignmentRepository.findBySemesterId(semesterId)).thenReturn(assignments);
        
        when(requiredDocumentTypeRepository.findByCourseIdAndSemesterId(1L, 1L))
            .thenReturn(Arrays.asList(reqDoc1, reqDoc2));
        when(requiredDocumentTypeRepository.findByCourseIdAndSemesterId(2L, 1L))
            .thenReturn(Arrays.asList(reqDoc3));
        
        when(documentSubmissionRepository.findByCourseAssignmentIdAndDocumentType(1L, DocumentTypeEnum.SYLLABUS))
            .thenReturn(Optional.of(sub1));
        when(documentSubmissionRepository.findByCourseAssignmentIdAndDocumentType(1L, DocumentTypeEnum.EXAM))
            .thenReturn(Optional.empty());
        when(documentSubmissionRepository.findByCourseAssignmentIdAndDocumentType(2L, DocumentTypeEnum.SYLLABUS))
            .thenReturn(Optional.empty());

        // Act
        SubmissionStatistics result = submissionService.getStatisticsBySemester(semesterId, departmentId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalProfessors()); // 2 unique professors
        assertEquals(2, result.getTotalCourses()); // 2 unique courses
        assertEquals(3, result.getTotalRequiredDocuments()); // 3 required document types
        assertEquals(1, result.getSubmittedDocuments()); // 1 submitted (syllabus for assignment1)
        assertEquals(1, result.getMissingDocuments()); // 1 missing (syllabus for assignment2)
        assertEquals(1, result.getOverdueDocuments()); // 1 overdue (exam for assignment1)
    }

    @Test
    void testGetStatisticsBySemester_ThrowsExceptionWhenSemesterNotFound() {
        // Arrange
        Long semesterId = 999L;
        Long departmentId = 1L;
        
        when(semesterRepository.existsById(semesterId)).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            submissionService.getStatisticsBySemester(semesterId, departmentId);
        });
    }

    @Test
    void testGetSubmissionsByProfessor_ThrowsExceptionWhenProfessorNotFound() {
        // Arrange
        Long professorId = 999L;
        Long semesterId = 1L;
        
        when(userRepository.existsById(professorId)).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            submissionService.getSubmissionsByProfessor(professorId, semesterId);
        });
    }

    @Test
    void testGetSubmissionsByProfessor_ThrowsExceptionWhenSemesterNotFound() {
        // Arrange
        Long professorId = 1L;
        Long semesterId = 999L;
        
        when(userRepository.existsById(professorId)).thenReturn(true);
        when(semesterRepository.existsById(semesterId)).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            submissionService.getSubmissionsByProfessor(professorId, semesterId);
        });
    }

    @Test
    void testGetSubmission_ThrowsExceptionWhenNotFound() {
        // Arrange
        Long submissionId = 999L;
        when(documentSubmissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            submissionService.getSubmission(submissionId);
        });
    }
}
