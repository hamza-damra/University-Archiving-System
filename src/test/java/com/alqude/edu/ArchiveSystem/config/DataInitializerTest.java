package com.alqude.edu.ArchiveSystem.config;

import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.repository.*;
import com.alqude.edu.ArchiveSystem.util.MockDataConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DataInitializer.
 * Tests mock data generation with clean database, idempotency, data relationships, and entity counts.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "mock.data.enabled=true",
    "mock.data.skip-if-exists=false"
})
@Transactional
class DataInitializerTest {

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseAssignmentRepository courseAssignmentRepository;

    @Autowired
    private RequiredDocumentTypeRepository requiredDocumentTypeRepository;

    @Autowired
    private DocumentSubmissionRepository documentSubmissionRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        clearAllData();
    }

    /**
     * Test mock data generation with clean database.
     * Verifies that all entities are created with expected counts.
     */
    @Test
    void testMockDataGeneration() throws Exception {
        // When: Run data initializer
        dataInitializer.run();

        // Then: Verify all entities created with expected counts
        assertThat(academicYearRepository.count())
            .as("Academic years should be created")
            .isEqualTo(MockDataConstants.ACADEMIC_YEARS_COUNT);

        assertThat(semesterRepository.count())
            .as("Semesters should be created (3 per academic year)")
            .isEqualTo(MockDataConstants.ACADEMIC_YEARS_COUNT * 3);

        assertThat(departmentRepository.count())
            .as("Departments should be created")
            .isEqualTo(MockDataConstants.DEPARTMENT_NAMES.size());

        assertThat(courseRepository.count())
            .as("Courses should be created")
            .isGreaterThanOrEqualTo(MockDataConstants.DEPARTMENT_NAMES.size() * MockDataConstants.COURSES_PER_DEPARTMENT);

        long expectedUserCount = MockDataConstants.DEPARTMENT_NAMES.size() + 
                                (MockDataConstants.DEPARTMENT_NAMES.size() * MockDataConstants.PROFESSORS_PER_DEPARTMENT);
        assertThat(userRepository.count())
            .as("Users should be created (HODs + Professors)")
            .isEqualTo(expectedUserCount);

        assertThat(courseAssignmentRepository.count())
            .as("Course assignments should be created")
            .isGreaterThan(0);

        assertThat(requiredDocumentTypeRepository.count())
            .as("Required document types should be created")
            .isGreaterThan(0);

        assertThat(documentSubmissionRepository.count())
            .as("Document submissions should be created")
            .isGreaterThan(0);

        assertThat(uploadedFileRepository.count())
            .as("Uploaded files should be created")
            .isGreaterThan(0);

        assertThat(notificationRepository.count())
            .as("Notifications should be created")
            .isGreaterThan(0);
    }

    /**
     * Test idempotency - running twice doesn't duplicate data.
     * Verifies that running the initializer multiple times doesn't create duplicate entities.
     */
    @Test
    void testIdempotency() throws Exception {
        // Given: Run data initializer first time
        dataInitializer.run();
        long initialAcademicYearCount = academicYearRepository.count();
        long initialDepartmentCount = departmentRepository.count();
        long initialUserCount = userRepository.count();
        long initialCourseCount = courseRepository.count();

        // When: Run again
        dataInitializer.run();

        // Then: No duplicate data created
        assertThat(academicYearRepository.count())
            .as("Academic year count should not increase")
            .isEqualTo(initialAcademicYearCount);

        assertThat(departmentRepository.count())
            .as("Department count should not increase")
            .isEqualTo(initialDepartmentCount);

        assertThat(userRepository.count())
            .as("User count should not increase")
            .isEqualTo(initialUserCount);

        assertThat(courseRepository.count())
            .as("Course count should not increase")
            .isEqualTo(initialCourseCount);
    }

    /**
     * Test data relationships are valid.
     * Verifies that all entity relationships are properly established and valid.
     */
    @Test
    void testDataRelationships() throws Exception {
        // Given: Generated data
        dataInitializer.run();

        // When: Query relationships
        List<CourseAssignment> assignments = courseAssignmentRepository.findAll();
        List<DocumentSubmission> submissions = documentSubmissionRepository.findAll();
        List<UploadedFile> files = uploadedFileRepository.findAll();
        List<Notification> notifications = notificationRepository.findAll();

        // Then: All relationships valid
        assertThat(assignments).isNotEmpty();
        for (CourseAssignment assignment : assignments) {
            assertThat(assignment.getSemester())
                .as("Course assignment should have semester")
                .isNotNull();
            assertThat(assignment.getCourse())
                .as("Course assignment should have course")
                .isNotNull();
            assertThat(assignment.getProfessor())
                .as("Course assignment should have professor")
                .isNotNull();
            assertThat(assignment.getProfessor().getRole())
                .as("Assigned user should be professor")
                .isEqualTo(Role.ROLE_PROFESSOR);
            assertThat(assignment.getCourse().getDepartment())
                .as("Professor should be from same department as course")
                .isEqualTo(assignment.getProfessor().getDepartment());
        }

        // Verify document submission relationships
        assertThat(submissions).isNotEmpty();
        for (DocumentSubmission submission : submissions) {
            assertThat(submission.getCourseAssignment())
                .as("Document submission should have course assignment")
                .isNotNull();
            assertThat(submission.getProfessor())
                .as("Document submission should have professor")
                .isNotNull();
            assertThat(submission.getProfessor().getRole())
                .as("Submitter should be professor")
                .isEqualTo(Role.ROLE_PROFESSOR);
        }

        // Verify uploaded file relationships
        assertThat(files).isNotEmpty();
        for (UploadedFile file : files) {
            assertThat(file.getDocumentSubmission())
                .as("Uploaded file should have document submission")
                .isNotNull();
            assertThat(file.getFileUrl())
                .as("Uploaded file should have file URL")
                .isNotBlank();
            assertThat(file.getOriginalFilename())
                .as("Uploaded file should have original filename")
                .isNotBlank();
        }

        // Verify notification relationships
        assertThat(notifications).isNotEmpty();
        for (Notification notification : notifications) {
            assertThat(notification.getUser())
                .as("Notification should have user")
                .isNotNull();
            assertThat(notification.getTitle())
                .as("Notification should have title")
                .isNotBlank();
            assertThat(notification.getMessage())
                .as("Notification should have message")
                .isNotBlank();
        }
    }

    /**
     * Test entity counts match expectations.
     * Verifies that the number of entities created matches the expected counts from requirements.
     */
    @Test
    void testEntityCountsMatchExpectations() throws Exception {
        // When: Run data initializer
        dataInitializer.run();

        // Then: Verify entity counts match expectations
        
        // Academic structure
        long academicYearCount = academicYearRepository.count();
        assertThat(academicYearCount)
            .as("Should create exactly 3 academic years")
            .isEqualTo(3);

        long semesterCount = semesterRepository.count();
        assertThat(semesterCount)
            .as("Should create 9 semesters (3 per academic year)")
            .isEqualTo(9);

        long departmentCount = departmentRepository.count();
        assertThat(departmentCount)
            .as("Should create 5 departments")
            .isEqualTo(5);

        long courseCount = courseRepository.count();
        assertThat(courseCount)
            .as("Should create 15 courses (3 per department)")
            .isEqualTo(15);

        // Users
        long hodCount = userRepository.countByRole(Role.ROLE_HOD);
        assertThat(hodCount)
            .as("Should create 5 HOD users (1 per department)")
            .isEqualTo(5);

        long professorCount = userRepository.countByRole(Role.ROLE_PROFESSOR);
        assertThat(professorCount)
            .as("Should create 25 professor users (5 per department)")
            .isEqualTo(25);

        // Course assignments
        long assignmentCount = courseAssignmentRepository.count();
        assertThat(assignmentCount)
            .as("Should create at least 30 course assignments")
            .isGreaterThanOrEqualTo(30);

        // Document system
        long submissionCount = documentSubmissionRepository.count();
        assertThat(submissionCount)
            .as("Should create at least 50 document submissions")
            .isGreaterThanOrEqualTo(50);

        long fileCount = uploadedFileRepository.count();
        assertThat(fileCount)
            .as("Should create at least 50 uploaded files")
            .isGreaterThanOrEqualTo(50);

        // Notifications
        long notificationCount = notificationRepository.count();
        assertThat(notificationCount)
            .as("Should create at least 30 notifications")
            .isGreaterThanOrEqualTo(30);
    }

    /**
     * Helper method to clear all data from database.
     */
    private void clearAllData() {
        // Delete in reverse dependency order
        notificationRepository.deleteAll();
        uploadedFileRepository.deleteAll();
        documentSubmissionRepository.deleteAll();
        requiredDocumentTypeRepository.deleteAll();
        courseAssignmentRepository.deleteAll();
        courseRepository.deleteAll();
        semesterRepository.deleteAll();
        academicYearRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }
}
