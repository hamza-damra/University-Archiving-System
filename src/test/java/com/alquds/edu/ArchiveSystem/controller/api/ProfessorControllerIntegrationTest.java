package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.entity.academic.*;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.repository.academic.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ProfessorController following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Test controller + service + repository layers together
 * - Use @SpringBootTest with MockMvc
 * - Test real database interactions (with @Transactional rollback)
 * - Test security and authorization
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ProfessorController Integration Tests")
class ProfessorControllerIntegrationTest {

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
    
    private Department testDepartment;
    private User testProfessor;
    private AcademicYear testAcademicYear;
    private Semester testSemester;
    private Course testCourse;
    private CourseAssignment testCourseAssignment;
    
    @BeforeEach
    void setUp() {
        // Clean up test data
        courseAssignmentRepository.deleteAll();
        courseRepository.deleteAll();
        semesterRepository.deleteAll();
        academicYearRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        
        // Create test department
        testDepartment = TestDataBuilder.createDepartment();
        testDepartment.setName("Computer Science");
        testDepartment.setShortcut("cs");
        testDepartment = departmentRepository.save(testDepartment);
        
        // Create test professor
        testProfessor = TestDataBuilder.createProfessorUser();
        testProfessor.setEmail("test.professor@staff.alquds.edu");
        testProfessor.setFirstName("Test");
        testProfessor.setLastName("Professor");
        testProfessor.setDepartment(testDepartment);
        testProfessor.setIsActive(true);
        testProfessor = userRepository.save(testProfessor);
        
        // Create test academic year
        testAcademicYear = TestDataBuilder.createAcademicYear();
        testAcademicYear.setYearCode("2024-2025");
        testAcademicYear.setIsActive(true);
        testAcademicYear = academicYearRepository.save(testAcademicYear);
        
        // Create test semester
        testSemester = TestDataBuilder.createSemester();
        testSemester.setType(SemesterType.FIRST);
        testSemester.setAcademicYear(testAcademicYear);
        testSemester.setIsActive(true);
        testSemester = semesterRepository.save(testSemester);
        
        // Create test course
        testCourse = TestDataBuilder.createCourse();
        testCourse.setCourseCode("CS101");
        testCourse.setCourseName("Introduction to Computer Science");
        testCourse.setDepartment(testDepartment);
        testCourse.setIsActive(true);
        testCourse = courseRepository.save(testCourse);
        
        // Create test course assignment
        testCourseAssignment = TestDataBuilder.createCourseAssignment();
        testCourseAssignment.setProfessor(testProfessor);
        testCourseAssignment.setCourse(testCourse);
        testCourseAssignment.setSemester(testSemester);
        testCourseAssignment.setIsActive(true);
        testCourseAssignment = courseAssignmentRepository.save(testCourseAssignment);
    }
    
    // ==================== Get Courses Tests ====================
    
    @Test
    @DisplayName("Should retrieve courses successfully for authenticated professor")
    @WithMockUser(username = "test.professor@staff.alquds.edu", roles = "PROFESSOR")
    void shouldRetrieveCoursesSuccessfullyForAuthenticatedProfessor() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/professor/dashboard/courses")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Courses retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].courseAssignmentId").value(testCourseAssignment.getId()))
                .andExpect(jsonPath("$.data[0].courseCode").value("CS101"))
                .andExpect(jsonPath("$.data[0].courseName").value("Introduction to Computer Science"));
    }
    
    @Test
    @DisplayName("Should return 403 when non-professor tries to access courses")
    @WithMockUser(username = "admin@admin.alquds.edu", roles = "ADMIN")
    void shouldReturn403WhenNonProfessorAccessesCourses() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/professor/dashboard/courses")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("Should return 403 when unauthenticated user tries to access courses")
    void shouldReturn403WhenUnauthenticatedAccessesCourses() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/professor/dashboard/courses")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("Should filter courses by semester successfully")
    @WithMockUser(username = "test.professor@staff.alquds.edu", roles = "PROFESSOR")
    void shouldFilterCoursesBySemesterSuccessfully() throws Exception {
        // Arrange: Create another semester and course assignment
        Semester secondSemester = TestDataBuilder.createSemester();
        secondSemester.setType(SemesterType.SECOND);
        secondSemester.setAcademicYear(testAcademicYear);
        secondSemester.setIsActive(true);
        secondSemester = semesterRepository.save(secondSemester);
        
        Course secondCourse = TestDataBuilder.createCourse();
        secondCourse.setCourseCode("CS102");
        secondCourse.setCourseName("Advanced Computer Science");
        secondCourse.setDepartment(testDepartment);
        secondCourse.setIsActive(true);
        secondCourse = courseRepository.save(secondCourse);
        
        CourseAssignment secondAssignment = TestDataBuilder.createCourseAssignment();
        secondAssignment.setProfessor(testProfessor);
        secondAssignment.setCourse(secondCourse);
        secondAssignment.setSemester(secondSemester);
        secondAssignment.setIsActive(true);
        courseAssignmentRepository.save(secondAssignment);
        
        // Act & Assert: Request courses for first semester only
        mockMvc.perform(get("/api/professor/dashboard/courses")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].courseCode").value("CS101"))
                .andExpect(jsonPath("$.data[0].semesterId").value(testSemester.getId()));
    }
    
    // ==================== Get Dashboard Overview Tests ====================
    
    @Test
    @DisplayName("Should retrieve dashboard overview successfully")
    @WithMockUser(username = "test.professor@staff.alquds.edu", roles = "PROFESSOR")
    void shouldRetrieveDashboardOverviewSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/professor/dashboard/overview")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Dashboard overview retrieved successfully"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.totalCourses").exists())
                .andExpect(jsonPath("$.data.submittedDocuments").exists())
                .andExpect(jsonPath("$.data.missingDocuments").exists())
                .andExpect(jsonPath("$.data.overdueDocuments").exists());
    }
    
    @Test
    @DisplayName("Should return 403 when non-professor tries to access dashboard overview")
    @WithMockUser(username = "hod@hod.alquds.edu", roles = "HOD")
    void shouldReturn403WhenNonProfessorAccessesDashboardOverview() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/professor/dashboard/overview")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isForbidden());
    }
    
    // ==================== Get Academic Years Tests ====================
    
    @Test
    @DisplayName("Should retrieve all academic years successfully")
    @WithMockUser(username = "test.professor@staff.alquds.edu", roles = "PROFESSOR")
    void shouldRetrieveAllAcademicYearsSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/professor/academic-years"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Academic years retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(testAcademicYear.getId()))
                .andExpect(jsonPath("$.data[0].yearCode").value("2024-2025"));
    }
    
    // ==================== Get Semesters by Year Tests ====================
    
    @Test
    @DisplayName("Should retrieve semesters by academic year successfully")
    @WithMockUser(username = "test.professor@staff.alquds.edu", roles = "PROFESSOR")
    void shouldRetrieveSemestersByAcademicYearSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/professor/academic-years/{academicYearId}/semesters", testAcademicYear.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Semesters retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(testSemester.getId()))
                .andExpect(jsonPath("$.data[0].type").value("FIRST"));
    }
    
    // ==================== Security Tests ====================
    
    @Test
    @DisplayName("Should ensure professor can only access own data")
    @WithMockUser(username = "test.professor@staff.alquds.edu", roles = "PROFESSOR")
    void shouldEnsureProfessorCanOnlyAccessOwnData() throws Exception {
        // Arrange: Create another professor with different courses
        User otherProfessor = TestDataBuilder.createProfessorUser();
        otherProfessor.setEmail("other.professor@staff.alquds.edu");
        otherProfessor.setFirstName("Other");
        otherProfessor.setLastName("Professor");
        otherProfessor.setDepartment(testDepartment);
        otherProfessor.setIsActive(true);
        otherProfessor = userRepository.save(otherProfessor);
        
        Course otherCourse = TestDataBuilder.createCourse();
        otherCourse.setCourseCode("CS201");
        otherCourse.setCourseName("Other Course");
        otherCourse.setDepartment(testDepartment);
        otherCourse.setIsActive(true);
        otherCourse = courseRepository.save(otherCourse);
        
        CourseAssignment otherAssignment = TestDataBuilder.createCourseAssignment();
        otherAssignment.setProfessor(otherProfessor);
        otherAssignment.setCourse(otherCourse);
        otherAssignment.setSemester(testSemester);
        otherAssignment.setIsActive(true);
        courseAssignmentRepository.save(otherAssignment);
        
        // Act & Assert: Current professor should only see their own courses
        mockMvc.perform(get("/api/professor/dashboard/courses")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].courseCode").value("CS101"));
    }
}
