package com.alquds.edu.ArchiveSystem.service.dashboard;

import com.alquds.edu.ArchiveSystem.dto.dashboard.*;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.DepartmentRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DashboardWidgetService following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Mock all external dependencies (repositories)
 * - Test business logic in isolation
 * - Follow AAA pattern (Arrange, Act, Assert)
 * - Test edge cases and error conditions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardWidgetService Unit Tests")
class DashboardWidgetServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DepartmentRepository departmentRepository;
    
    @Mock
    private CourseRepository courseRepository;
    
    @Mock
    private DocumentSubmissionRepository documentSubmissionRepository;
    
    @InjectMocks
    private DashboardWidgetServiceImpl dashboardWidgetService;
    
    private Department testDepartment1;
    private Department testDepartment2;
    private User testProfessor;
    private Course testCourse;
    private CourseAssignment testCourseAssignment;
    private DocumentSubmission testSubmission1;
    
    @BeforeEach
    void setUp() {
        // Arrange: Set up test data
        testDepartment1 = TestDataBuilder.createDepartment();
        testDepartment1.setId(1L);
        testDepartment1.setName("Computer Science");
        testDepartment1.setShortcut("cs");
        
        testDepartment2 = TestDataBuilder.createDepartment();
        testDepartment2.setId(2L);
        testDepartment2.setName("Mathematics");
        testDepartment2.setShortcut("math");
        
        testProfessor = TestDataBuilder.createProfessorUser();
        testProfessor.setId(1L);
        testProfessor.setFirstName("John");
        testProfessor.setLastName("Doe");
        
        testCourse = TestDataBuilder.createCourse();
        testCourse.setId(1L);
        testCourse.setCourseCode("CS101");
        testCourse.setDepartment(testDepartment1);
        
        testCourseAssignment = TestDataBuilder.createCourseAssignment();
        testCourseAssignment.setId(1L);
        testCourseAssignment.setCourse(testCourse);
        
        testSubmission1 = createDocumentSubmission(1L, SubmissionStatus.UPLOADED, LocalDateTime.now().minusDays(5));
    }
    
    private DocumentSubmission createDocumentSubmission(Long id, SubmissionStatus status, LocalDateTime submittedAt) {
        DocumentSubmission submission = new DocumentSubmission();
        submission.setId(id);
        submission.setStatus(status);
        submission.setSubmittedAt(submittedAt);
        submission.setProfessor(testProfessor);
        submission.setCourseAssignment(testCourseAssignment);
        submission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        submission.setFileCount(1);
        submission.setTotalFileSize(1024L);
        return submission;
    }
    
    @Test
    @DisplayName("Should get statistics - all data (no filters)")
    void shouldGetStatisticsAllData() {
        // Arrange
        when(userRepository.countActiveProfessors()).thenReturn(10L);
        when(userRepository.countByRole(any())).thenReturn(5L);
        when(departmentRepository.count()).thenReturn(3L);
        when(courseRepository.countByIsActiveTrue()).thenReturn(20L);
        
        // Mock status distribution (all semesters)
        List<Object[]> statusCounts = new ArrayList<>();
        statusCounts.add(new Object[]{SubmissionStatus.NOT_UPLOADED, 15L});
        statusCounts.add(new Object[]{SubmissionStatus.UPLOADED, 30L});
        statusCounts.add(new Object[]{SubmissionStatus.OVERDUE, 5L});
        when(documentSubmissionRepository.countByStatus()).thenReturn(statusCounts);
        
        // Mock recent submissions
        List<DocumentSubmission> recentSubmissions = Arrays.asList(testSubmission1);
        when(documentSubmissionRepository.findRecentSubmissions(any(LocalDateTime.class)))
                .thenReturn(recentSubmissions);
        
        // Act
        DashboardStatistics result = dashboardWidgetService.getStatistics(null, null);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalProfessors()).isEqualTo(10L);
        assertThat(result.getTotalHods()).isEqualTo(5L);
        assertThat(result.getTotalDepartments()).isEqualTo(3L);
        assertThat(result.getTotalCourses()).isEqualTo(20L);
        assertThat(result.getTotalSubmissions()).isEqualTo(50L); // 15 + 30 + 5
        assertThat(result.getPendingSubmissions()).isEqualTo(15L);
        assertThat(result.getRecentSubmissions()).isEqualTo(1L);
        assertThat(result.getGeneratedAt()).isNotNull();
        
        verify(userRepository).countActiveProfessors();
        verify(userRepository).countByRole(any());
        verify(departmentRepository).count();
        verify(courseRepository).countByIsActiveTrue();
        verify(documentSubmissionRepository).countByStatus();
        verify(documentSubmissionRepository).findRecentSubmissions(any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should get statistics - filtered by academic year")
    void shouldGetStatisticsFilteredByAcademicYear() {
        // Arrange
        Long academicYearId = 1L;
        when(userRepository.countActiveProfessors()).thenReturn(10L);
        when(userRepository.countByRole(any())).thenReturn(5L);
        when(departmentRepository.count()).thenReturn(3L);
        when(courseRepository.countByIsActiveTrue()).thenReturn(20L);
        
        // Mock status distribution (all semesters, but filtered by academic year logic)
        List<Object[]> statusCounts = new ArrayList<>();
        statusCounts.add(new Object[]{SubmissionStatus.NOT_UPLOADED, 10L});
        statusCounts.add(new Object[]{SubmissionStatus.UPLOADED, 25L});
        statusCounts.add(new Object[]{SubmissionStatus.OVERDUE, 3L});
        when(documentSubmissionRepository.countByStatus()).thenReturn(statusCounts);
        
        List<DocumentSubmission> recentSubmissions = Arrays.asList(testSubmission1);
        when(documentSubmissionRepository.findRecentSubmissions(any(LocalDateTime.class)))
                .thenReturn(recentSubmissions);
        
        // Act
        DashboardStatistics result = dashboardWidgetService.getStatistics(academicYearId, null);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalSubmissions()).isEqualTo(38L); // 10 + 25 + 3
        assertThat(result.getPendingSubmissions()).isEqualTo(10L);
        
        verify(documentSubmissionRepository).countByStatus();
    }
    
    @Test
    @DisplayName("Should get statistics - filtered by semester")
    void shouldGetStatisticsFilteredBySemester() {
        // Arrange
        Long semesterId = 1L;
        when(userRepository.countActiveProfessors()).thenReturn(10L);
        when(userRepository.countByRole(any())).thenReturn(5L);
        when(departmentRepository.count()).thenReturn(3L);
        when(courseRepository.countByIsActiveTrue()).thenReturn(20L);
        
        // Mock status distribution filtered by semester
        List<Object[]> statusCounts = new ArrayList<>();
        statusCounts.add(new Object[]{SubmissionStatus.NOT_UPLOADED, 8L});
        statusCounts.add(new Object[]{SubmissionStatus.UPLOADED, 20L});
        statusCounts.add(new Object[]{SubmissionStatus.OVERDUE, 2L});
        when(documentSubmissionRepository.countByStatusAndSemesterId(semesterId)).thenReturn(statusCounts);
        
        List<DocumentSubmission> recentSubmissions = Arrays.asList(testSubmission1);
        when(documentSubmissionRepository.findRecentSubmissions(any(LocalDateTime.class)))
                .thenReturn(recentSubmissions);
        
        // Act
        DashboardStatistics result = dashboardWidgetService.getStatistics(null, semesterId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalSubmissions()).isEqualTo(30L); // 8 + 20 + 2
        assertThat(result.getPendingSubmissions()).isEqualTo(8L);
        
        verify(documentSubmissionRepository).countByStatusAndSemesterId(semesterId);
        verify(documentSubmissionRepository, never()).countByStatus();
    }
    
    @Test
    @DisplayName("Should get submissions over time - grouped by day")
    void shouldGetSubmissionsOverTimeGroupedByDay() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        TimeGrouping groupBy = TimeGrouping.DAY;
        
        List<DocumentSubmission> submissions = Arrays.asList(
            createDocumentSubmission(1L, SubmissionStatus.UPLOADED, startDate.atStartOfDay().plusDays(1)),
            createDocumentSubmission(2L, SubmissionStatus.UPLOADED, startDate.atStartOfDay().plusDays(3)),
            createDocumentSubmission(3L, SubmissionStatus.UPLOADED, startDate.atStartOfDay().plusDays(3))
        );
        
        when(documentSubmissionRepository.findBySubmittedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(submissions);
        
        // Act
        List<ChartDataPoint> result = dashboardWidgetService.getSubmissionsOverTime(startDate, endDate, groupBy);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        
        // Verify all days in range are included (even with 0 submissions)
        assertThat(result.size()).isGreaterThanOrEqualTo(7);
        
        // Verify submissions are counted correctly
        long totalCount = result.stream()
                .mapToLong(ChartDataPoint::getValue)
                .sum();
        assertThat(totalCount).isEqualTo(3L);
        
        verify(documentSubmissionRepository).findBySubmittedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should get submissions over time - grouped by week")
    void shouldGetSubmissionsOverTimeGroupedByWeek() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusWeeks(4);
        LocalDate endDate = LocalDate.now();
        TimeGrouping groupBy = TimeGrouping.WEEK;
        
        List<DocumentSubmission> submissions = Arrays.asList(
            createDocumentSubmission(1L, SubmissionStatus.UPLOADED, startDate.atStartOfDay().plusWeeks(1)),
            createDocumentSubmission(2L, SubmissionStatus.UPLOADED, startDate.atStartOfDay().plusWeeks(2)),
            createDocumentSubmission(3L, SubmissionStatus.UPLOADED, startDate.atStartOfDay().plusWeeks(2))
        );
        
        when(documentSubmissionRepository.findBySubmittedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(submissions);
        
        // Act
        List<ChartDataPoint> result = dashboardWidgetService.getSubmissionsOverTime(startDate, endDate, groupBy);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        
        // Verify submissions are grouped by week
        long totalCount = result.stream()
                .mapToLong(ChartDataPoint::getValue)
                .sum();
        assertThat(totalCount).isEqualTo(3L);
        
        verify(documentSubmissionRepository).findBySubmittedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should get submissions over time - grouped by month")
    void shouldGetSubmissionsOverTimeGroupedByMonth() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusMonths(6);
        LocalDate endDate = LocalDate.now();
        TimeGrouping groupBy = TimeGrouping.MONTH;
        
        List<DocumentSubmission> submissions = Arrays.asList(
            createDocumentSubmission(1L, SubmissionStatus.UPLOADED, startDate.atStartOfDay().plusMonths(1)),
            createDocumentSubmission(2L, SubmissionStatus.UPLOADED, startDate.atStartOfDay().plusMonths(2)),
            createDocumentSubmission(3L, SubmissionStatus.UPLOADED, startDate.atStartOfDay().plusMonths(2))
        );
        
        when(documentSubmissionRepository.findBySubmittedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(submissions);
        
        // Act
        List<ChartDataPoint> result = dashboardWidgetService.getSubmissionsOverTime(startDate, endDate, groupBy);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        
        // Verify submissions are grouped by month
        long totalCount = result.stream()
                .mapToLong(ChartDataPoint::getValue)
                .sum();
        assertThat(totalCount).isEqualTo(3L);
        
        verify(documentSubmissionRepository).findBySubmittedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should get department distribution - all semesters")
    void shouldGetDepartmentDistributionAllSemesters() {
        // Arrange
        List<Department> departments = Arrays.asList(testDepartment1, testDepartment2);
        when(departmentRepository.findAll()).thenReturn(departments);
        
        // Mock submission counts by department (all semesters)
        List<Object[]> submissionCounts = new ArrayList<>();
        submissionCounts.add(new Object[]{testDepartment1.getId(), "CS", 25L});
        submissionCounts.add(new Object[]{testDepartment2.getId(), "MATH", 15L});
        when(documentSubmissionRepository.countByDepartment()).thenReturn(submissionCounts);
        
        // Mock professor counts by department
        List<Object[]> professorCounts = Arrays.asList(
            new Object[]{testDepartment1.getId(), "CS", 8L},
            new Object[]{testDepartment2.getId(), "MATH", 5L}
        );
        when(userRepository.countActiveProfessorsByDepartment()).thenReturn(professorCounts);
        
        // Mock course counts by department
        List<Object[]> courseCounts = Arrays.asList(
            new Object[]{testDepartment1.getId(), "CS", 12L},
            new Object[]{testDepartment2.getId(), "MATH", 8L}
        );
        when(courseRepository.countActiveCoursesByDepartment()).thenReturn(courseCounts);
        
        // Act
        List<DepartmentChartData> result = dashboardWidgetService.getDepartmentDistribution(null);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        
        DepartmentChartData dept1Data = result.stream()
                .filter(d -> d.getDepartmentId().equals(testDepartment1.getId()))
                .findFirst()
                .orElse(null);
        assertThat(dept1Data).isNotNull();
        assertThat(dept1Data.getSubmissionCount()).isEqualTo(25L);
        assertThat(dept1Data.getProfessorCount()).isEqualTo(8L);
        assertThat(dept1Data.getCourseCount()).isEqualTo(12L);
        
        DepartmentChartData dept2Data = result.stream()
                .filter(d -> d.getDepartmentId().equals(testDepartment2.getId()))
                .findFirst()
                .orElse(null);
        assertThat(dept2Data).isNotNull();
        assertThat(dept2Data.getSubmissionCount()).isEqualTo(15L);
        assertThat(dept2Data.getProfessorCount()).isEqualTo(5L);
        assertThat(dept2Data.getCourseCount()).isEqualTo(8L);
        
        verify(departmentRepository).findAll();
        verify(documentSubmissionRepository).countByDepartment();
        verify(userRepository).countActiveProfessorsByDepartment();
        verify(courseRepository).countActiveCoursesByDepartment();
    }
    
    @Test
    @DisplayName("Should get department distribution - specific semester")
    void shouldGetDepartmentDistributionSpecificSemester() {
        // Arrange
        Long semesterId = 1L;
        List<Department> departments = Arrays.asList(testDepartment1, testDepartment2);
        when(departmentRepository.findAll()).thenReturn(departments);
        
        // Mock submission counts by department filtered by semester
        List<Object[]> submissionCounts = new ArrayList<>();
        submissionCounts.add(new Object[]{testDepartment1.getId(), "CS", 15L});
        submissionCounts.add(new Object[]{testDepartment2.getId(), "MATH", 10L});
        when(documentSubmissionRepository.countByDepartmentAndSemesterId(semesterId)).thenReturn(submissionCounts);
        
        // Mock professor counts by department
        List<Object[]> professorCounts = new ArrayList<>();
        professorCounts.add(new Object[]{testDepartment1.getId(), "CS", 8L});
        professorCounts.add(new Object[]{testDepartment2.getId(), "MATH", 5L});
        when(userRepository.countActiveProfessorsByDepartment()).thenReturn(professorCounts);
        
        // Mock course counts by department
        List<Object[]> courseCounts = new ArrayList<>();
        courseCounts.add(new Object[]{testDepartment1.getId(), "CS", 12L});
        courseCounts.add(new Object[]{testDepartment2.getId(), "MATH", 8L});
        when(courseRepository.countActiveCoursesByDepartment()).thenReturn(courseCounts);
        
        // Act
        List<DepartmentChartData> result = dashboardWidgetService.getDepartmentDistribution(semesterId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        
        DepartmentChartData dept1Data = result.stream()
                .filter(d -> d.getDepartmentId().equals(testDepartment1.getId()))
                .findFirst()
                .orElse(null);
        assertThat(dept1Data).isNotNull();
        assertThat(dept1Data.getSubmissionCount()).isEqualTo(15L);
        
        verify(documentSubmissionRepository).countByDepartmentAndSemesterId(semesterId);
        verify(documentSubmissionRepository, never()).countByDepartment();
    }
    
    @Test
    @DisplayName("Should get status distribution - all semesters")
    void shouldGetStatusDistributionAllSemesters() {
        // Arrange
        List<Object[]> statusCounts = new ArrayList<>();
        statusCounts.add(new Object[]{SubmissionStatus.NOT_UPLOADED, 20L});
        statusCounts.add(new Object[]{SubmissionStatus.UPLOADED, 50L});
        statusCounts.add(new Object[]{SubmissionStatus.OVERDUE, 10L});
        when(documentSubmissionRepository.countByStatus()).thenReturn(statusCounts);
        
        // Act
        StatusDistribution result = dashboardWidgetService.getStatusDistribution(null);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPending()).isEqualTo(20L);
        assertThat(result.getUploaded()).isEqualTo(50L);
        assertThat(result.getOverdue()).isEqualTo(10L);
        assertThat(result.getTotal()).isEqualTo(80L); // 20 + 50 + 10
        
        verify(documentSubmissionRepository).countByStatus();
        verify(documentSubmissionRepository, never()).countByStatusAndSemesterId(anyLong());
    }
    
    @Test
    @DisplayName("Should get status distribution - specific semester")
    void shouldGetStatusDistributionSpecificSemester() {
        // Arrange
        Long semesterId = 1L;
        List<Object[]> statusCounts = new ArrayList<>();
        statusCounts.add(new Object[]{SubmissionStatus.NOT_UPLOADED, 10L});
        statusCounts.add(new Object[]{SubmissionStatus.UPLOADED, 30L});
        statusCounts.add(new Object[]{SubmissionStatus.OVERDUE, 5L});
        when(documentSubmissionRepository.countByStatusAndSemesterId(semesterId)).thenReturn(statusCounts);
        
        // Act
        StatusDistribution result = dashboardWidgetService.getStatusDistribution(semesterId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPending()).isEqualTo(10L);
        assertThat(result.getUploaded()).isEqualTo(30L);
        assertThat(result.getOverdue()).isEqualTo(5L);
        assertThat(result.getTotal()).isEqualTo(45L); // 10 + 30 + 5
        
        verify(documentSubmissionRepository).countByStatusAndSemesterId(semesterId);
        verify(documentSubmissionRepository, never()).countByStatus();
    }
    
    @Test
    @DisplayName("Should get recent activity - with limit")
    void shouldGetRecentActivityWithLimit() {
        // Arrange
        int limit = 5;
        
        List<DocumentSubmission> recentSubmissions = Arrays.asList(
            createDocumentSubmission(1L, SubmissionStatus.UPLOADED, LocalDateTime.now().minusDays(1)),
            createDocumentSubmission(2L, SubmissionStatus.UPLOADED, LocalDateTime.now().minusDays(2)),
            createDocumentSubmission(3L, SubmissionStatus.UPLOADED, LocalDateTime.now().minusDays(3)),
            createDocumentSubmission(4L, SubmissionStatus.UPLOADED, LocalDateTime.now().minusDays(4)),
            createDocumentSubmission(5L, SubmissionStatus.UPLOADED, LocalDateTime.now().minusDays(5)),
            createDocumentSubmission(6L, SubmissionStatus.UPLOADED, LocalDateTime.now().minusDays(6))
        );
        
        when(documentSubmissionRepository.findRecentSubmissions(any(LocalDateTime.class)))
                .thenReturn(recentSubmissions);
        
        // Act
        List<RecentActivity> result = dashboardWidgetService.getRecentActivity(limit);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.size()).isLessThanOrEqualTo(limit);
        assertThat(result).isNotEmpty();
        
        // Verify activities are sorted by timestamp descending
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getTimestamp())
                    .isAfterOrEqualTo(result.get(i + 1).getTimestamp());
        }
        
        // Verify activity structure
        result.forEach(activity -> {
            assertThat(activity.getType()).isEqualTo("SUBMISSION");
            assertThat(activity.getIcon()).isEqualTo("upload");
            assertThat(activity.getMessage()).isNotNull();
            assertThat(activity.getTimestamp()).isNotNull();
            assertThat(activity.getTimeAgo()).isNotNull();
            assertThat(activity.getEntityId()).isNotNull();
            assertThat(activity.getEntityType()).isEqualTo("submission");
        });
        
        verify(documentSubmissionRepository).findRecentSubmissions(any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should get recent activity - empty result")
    void shouldGetRecentActivityEmptyResult() {
        // Arrange
        int limit = 10;
        when(documentSubmissionRepository.findRecentSubmissions(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        
        // Act
        List<RecentActivity> result = dashboardWidgetService.getRecentActivity(limit);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        
        verify(documentSubmissionRepository).findRecentSubmissions(any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should handle null dates in getSubmissionsOverTime")
    void shouldHandleNullDatesInGetSubmissionsOverTime() {
        // Arrange
        List<DocumentSubmission> submissions = Arrays.asList(testSubmission1);
        when(documentSubmissionRepository.findBySubmittedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(submissions);
        
        // Act
        List<ChartDataPoint> result = dashboardWidgetService.getSubmissionsOverTime(null, null, TimeGrouping.DAY);
        
        // Assert
        assertThat(result).isNotNull();
        // Should default to last 30 days
        verify(documentSubmissionRepository).findBySubmittedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should handle null TimeGrouping in getSubmissionsOverTime")
    void shouldHandleNullTimeGroupingInGetSubmissionsOverTime() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        List<DocumentSubmission> submissions = Arrays.asList(testSubmission1);
        when(documentSubmissionRepository.findBySubmittedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(submissions);
        
        // Act
        List<ChartDataPoint> result = dashboardWidgetService.getSubmissionsOverTime(startDate, endDate, null);
        
        // Assert
        assertThat(result).isNotNull();
        // Should default to DAY grouping
        verify(documentSubmissionRepository).findBySubmittedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should handle department with no submissions in getDepartmentDistribution")
    void shouldHandleDepartmentWithNoSubmissions() {
        // Arrange
        Department emptyDept = TestDataBuilder.createDepartment();
        emptyDept.setId(3L);
        emptyDept.setName("Physics");
        emptyDept.setShortcut("physics");
        
        List<Department> departments = Arrays.asList(testDepartment1, emptyDept);
        when(departmentRepository.findAll()).thenReturn(departments);
        
        // Mock submission counts - emptyDept has no submissions
        List<Object[]> submissionCounts = new ArrayList<>();
        submissionCounts.add(new Object[]{testDepartment1.getId(), "CS", 25L});
        when(documentSubmissionRepository.countByDepartment()).thenReturn(submissionCounts);
        
        // Mock professor counts
        List<Object[]> professorCounts = new ArrayList<>();
        professorCounts.add(new Object[]{testDepartment1.getId(), "CS", 8L});
        professorCounts.add(new Object[]{emptyDept.getId(), "PHYSICS", 3L});
        when(userRepository.countActiveProfessorsByDepartment()).thenReturn(professorCounts);
        
        // Mock course counts
        List<Object[]> courseCounts = new ArrayList<>();
        courseCounts.add(new Object[]{testDepartment1.getId(), "CS", 12L});
        courseCounts.add(new Object[]{emptyDept.getId(), "PHYSICS", 5L});
        when(courseRepository.countActiveCoursesByDepartment()).thenReturn(courseCounts);
        
        // Act
        List<DepartmentChartData> result = dashboardWidgetService.getDepartmentDistribution(null);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        
        DepartmentChartData emptyDeptData = result.stream()
                .filter(d -> d.getDepartmentId().equals(emptyDept.getId()))
                .findFirst()
                .orElse(null);
        assertThat(emptyDeptData).isNotNull();
        assertThat(emptyDeptData.getSubmissionCount()).isEqualTo(0L); // Should default to 0
        assertThat(emptyDeptData.getProfessorCount()).isEqualTo(3L);
        assertThat(emptyDeptData.getCourseCount()).isEqualTo(5L);
    }
}
