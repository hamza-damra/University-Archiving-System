package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.entity.academic.*;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.repository.academic.*;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for HodController following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Test controller + service + repository layers together
 * - Use @SpringBootTest with MockMvc
 * - Test real database interactions (with @Transactional rollback)
 * - Test security and authorization
 * - Test department-scoped access control
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("HodController Integration Tests")
class HodControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private AcademicYearRepository academicYearRepository;
    
    @Autowired
    private SemesterRepository semesterRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private CourseAssignmentRepository courseAssignmentRepository;
    
    @Autowired
    private DocumentSubmissionRepository documentSubmissionRepository;
    
    private Department testDepartment;
    private Department otherDepartment;
    private User testHod;
    private User otherHod;
    private User testProfessor;
    private User otherProfessor;
    private AcademicYear testAcademicYear;
    private Semester testSemester;
    private Course testCourse;
    private CourseAssignment testCourseAssignment;
    private DocumentSubmission testSubmission;
    
    @BeforeEach
    void setUp() {
        // Clean up test data
        documentSubmissionRepository.deleteAll();
        courseAssignmentRepository.deleteAll();
        courseRepository.deleteAll();
        semesterRepository.deleteAll();
        academicYearRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        
        // Create test department
        Department dept1 = TestDataBuilder.createDepartment();
        dept1.setName("Computer Science");
        dept1.setShortcut("cs");
        testDepartment = departmentRepository.save(dept1);
        
        // Create other department
        Department dept2 = TestDataBuilder.createDepartment();
        dept2.setName("Mathematics");
        dept2.setShortcut("math");
        otherDepartment = departmentRepository.save(dept2);
        
        // Create test HOD
        testHod = TestDataBuilder.createHodUser();
        testHod.setEmail("hod.cs@hod.alquds.edu");
        testHod.setFirstName("HOD");
        testHod.setLastName("CS");
        testHod.setDepartment(testDepartment);
        testHod.setIsActive(true);
        testHod = userRepository.save(testHod);
        
        // Create other HOD (different department)
        otherHod = TestDataBuilder.createHodUser();
        otherHod.setEmail("hod.math@hod.alquds.edu");
        otherHod.setFirstName("HOD");
        otherHod.setLastName("Math");
        otherHod.setDepartment(otherDepartment);
        otherHod.setIsActive(true);
        otherHod = userRepository.save(otherHod);
        
        // Create test professor in test department
        testProfessor = TestDataBuilder.createProfessorUser();
        testProfessor.setEmail("prof.cs@staff.alquds.edu");
        testProfessor.setFirstName("Test");
        testProfessor.setLastName("Professor");
        testProfessor.setDepartment(testDepartment);
        testProfessor.setIsActive(true);
        testProfessor = userRepository.save(testProfessor);
        
        // Create other professor in other department
        otherProfessor = TestDataBuilder.createProfessorUser();
        otherProfessor.setEmail("prof.math@staff.alquds.edu");
        otherProfessor.setFirstName("Other");
        otherProfessor.setLastName("Professor");
        otherProfessor.setDepartment(otherDepartment);
        otherProfessor.setIsActive(true);
        otherProfessor = userRepository.save(otherProfessor);
        
        // Create test academic year
        AcademicYear year = TestDataBuilder.createAcademicYear();
        year.setYearCode("2024-2025");
        year.setIsActive(true);
        testAcademicYear = academicYearRepository.save(year);
        
        // Create test semester
        testSemester = TestDataBuilder.createSemester();
        testSemester.setType(SemesterType.FIRST);
        testSemester.setAcademicYear(testAcademicYear);
        testSemester.setIsActive(true);
        testSemester = semesterRepository.save(testSemester);
        
        // Create test course in test department
        Course course = TestDataBuilder.createCourse();
        course.setCourseCode("CS101");
        course.setCourseName("Introduction to Computer Science");
        course.setDepartment(testDepartment);
        course.setIsActive(true);
        testCourse = courseRepository.save(course);
        
        // Create test course assignment
        CourseAssignment assignment = TestDataBuilder.createCourseAssignment();
        assignment.setProfessor(testProfessor);
        assignment.setCourse(testCourse);
        assignment.setSemester(testSemester);
        assignment.setIsActive(true);
        testCourseAssignment = courseAssignmentRepository.save(assignment);
        
        // Create test document submission
        testSubmission = new DocumentSubmission();
        testSubmission.setCourseAssignment(testCourseAssignment);
        testSubmission.setProfessor(testProfessor);
        testSubmission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        testSubmission.setStatus(SubmissionStatus.UPLOADED);
        testSubmission.setSubmittedAt(LocalDateTime.now());
        testSubmission.setIsLateSubmission(false);
        testSubmission.setFileCount(1);
        testSubmission.setTotalFileSize(1024L);
        testSubmission = documentSubmissionRepository.save(testSubmission);
    }
    
    // ==================== Dashboard Overview Tests ====================
    
    @Test
    @DisplayName("Should retrieve dashboard overview successfully for authenticated HOD")
    @WithMockUser(username = "hod.cs@hod.alquds.edu", roles = "HOD")
    void shouldRetrieveDashboardOverviewSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/hod/dashboard/overview")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Dashboard overview retrieved successfully"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.semesterId").value(testSemester.getId()))
                .andExpect(jsonPath("$.data.departmentId").value(testDepartment.getId()))
                .andExpect(jsonPath("$.data.totalProfessors").exists())
                .andExpect(jsonPath("$.data.totalCourses").exists())
                .andExpect(jsonPath("$.data.submissionStatistics").exists());
    }
    
    @Test
    @DisplayName("Should return 400 when HOD has no department assigned")
    @WithMockUser(username = "hod.nodep@hod.alquds.edu", roles = "HOD")
    void shouldReturn400WhenHodHasNoDepartment() throws Exception {
        // Arrange: Create HOD without department
        User hodNoDep = TestDataBuilder.createHodUser();
        hodNoDep.setEmail("hod.nodep@hod.alquds.edu");
        hodNoDep.setDepartment(null);
        hodNoDep.setIsActive(true);
        userRepository.save(hodNoDep);
        
        // Act & Assert
        mockMvc.perform(get("/api/hod/dashboard/overview")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("HOD must be assigned to a department"));
    }
    
    // ==================== Get Professors Tests ====================
    
    @Test
    @DisplayName("Should retrieve department professors successfully")
    @WithMockUser(username = "hod.cs@hod.alquds.edu", roles = "HOD")
    void shouldRetrieveDepartmentProfessorsSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/hod/professors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Professors retrieved successfully"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].email").value("prof.cs@staff.alquds.edu"));
    }
    
    @Test
    @DisplayName("Should only return professors from HOD's department")
    @WithMockUser(username = "hod.cs@hod.alquds.edu", roles = "HOD")
    void shouldOnlyReturnProfessorsFromHodDepartment() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/hod/professors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[?(@.email == 'prof.cs@staff.alquds.edu')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.email == 'prof.math@staff.alquds.edu')]").doesNotExist());
    }
    
    // ==================== Get Submissions Tests ====================
    
    @Test
    @DisplayName("Should retrieve department submissions successfully")
    @WithMockUser(username = "hod.cs@hod.alquds.edu", roles = "HOD")
    void shouldRetrieveDepartmentSubmissionsSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/hod/submissions/status")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Submission status retrieved successfully"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.semesterId").value(testSemester.getId()))
                .andExpect(jsonPath("$.data.departmentId").value(testDepartment.getId()));
    }
    
    @Test
    @DisplayName("Should filter submissions by semester successfully")
    @WithMockUser(username = "hod.cs@hod.alquds.edu", roles = "HOD")
    void shouldFilterSubmissionsBySemesterSuccessfully() throws Exception {
        // Arrange: Create another semester and course assignment
        Semester secondSemester = TestDataBuilder.createSemester();
        secondSemester.setType(SemesterType.SECOND);
        secondSemester.setAcademicYear(testAcademicYear);
        secondSemester.setIsActive(true);
        secondSemester = semesterRepository.save(secondSemester);
        
        CourseAssignment secondAssignment = TestDataBuilder.createCourseAssignment();
        secondAssignment.setProfessor(testProfessor);
        secondAssignment.setCourse(testCourse);
        secondAssignment.setSemester(secondSemester);
        secondAssignment.setIsActive(true);
        courseAssignmentRepository.save(secondAssignment);
        
        // Act & Assert: Request submissions for first semester only
        mockMvc.perform(get("/api/hod/submissions/status")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.semesterId").value(testSemester.getId()));
    }
    
    // ==================== Security Tests ====================
    
    @Test
    @DisplayName("Should ensure HOD can only access own department data")
    @WithMockUser(username = "hod.cs@hod.alquds.edu", roles = "HOD")
    void shouldEnsureHodCanOnlyAccessOwnDepartmentData() throws Exception {
        // Act & Assert: HOD from CS department should only see CS professors
        mockMvc.perform(get("/api/hod/professors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[?(@.email == 'prof.cs@staff.alquds.edu')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.email == 'prof.math@staff.alquds.edu')]").doesNotExist());
    }
    
    @Test
    @DisplayName("Should return 403 when non-HOD tries to access HOD endpoints")
    @WithMockUser(username = "prof.cs@staff.alquds.edu", roles = "PROFESSOR")
    void shouldReturn403WhenNonHodAccessesHodEndpoints() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/hod/professors"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("Should return 403 when unauthenticated user tries to access HOD endpoints")
    void shouldReturn403WhenUnauthenticatedAccessesHodEndpoints() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/hod/professors"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("Should return 403 when HOD from different department tries to access other department")
    @WithMockUser(username = "hod.math@hod.alquds.edu", roles = "HOD")
    void shouldReturn403WhenHodFromDifferentDepartmentAccessesOtherDepartment() throws Exception {
        // Act & Assert: Math HOD should not see CS department data
        mockMvc.perform(get("/api/hod/professors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[?(@.email == 'prof.cs@staff.alquds.edu')]").doesNotExist())
                .andExpect(jsonPath("$.data.content[?(@.email == 'prof.math@staff.alquds.edu')]").exists());
    }
}
