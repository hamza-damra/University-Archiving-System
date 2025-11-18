package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for HodController.
 * Tests department-scoped data access and permission enforcement.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"null", "unused"})
class HodControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String HOD_EMAIL = "hod.test@alquds.edu";
    private static final String HOD_PASSWORD = "HodTest123!";
    
    private User hodUser;
    private Department hodDepartment;
    private Department otherDepartment;
    private String sessionCookie;
    private AcademicYear academicYear;
    private Semester semester;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up existing test data
        userRepository.findByEmail(HOD_EMAIL).ifPresent(user -> {
            userRepository.delete(user);
            userRepository.flush();
        });

        // Create HOD's department
        hodDepartment = departmentRepository.findByName("HOD Test Department")
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setName("HOD Test Department");
                    dept.setDescription("Test department for HOD integration tests");
                    return departmentRepository.saveAndFlush(dept);
                });

        // Create another department
        otherDepartment = departmentRepository.findByName("Other Test Department")
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setName("Other Test Department");
                    dept.setDescription("Another department for testing access control");
                    return departmentRepository.saveAndFlush(dept);
                });

        // Create HOD user
        hodUser = new User();
        hodUser.setEmail(HOD_EMAIL);
        hodUser.setPassword(passwordEncoder.encode(HOD_PASSWORD));
        hodUser.setFirstName("HOD");
        hodUser.setLastName("Test");
        hodUser.setRole(Role.ROLE_HOD);
        hodUser.setDepartment(hodDepartment);
        hodUser = userRepository.saveAndFlush(hodUser);

        // Create academic year and semester
        academicYear = new AcademicYear();
        academicYear.setYearCode("2024-2025");
        academicYear.setStartYear(2024);
        academicYear.setEndYear(2025);
        academicYear = academicYearRepository.saveAndFlush(academicYear);

        semester = new Semester();
        semester.setAcademicYear(academicYear);
        semester.setType(SemesterType.FIRST);
        semester.setStartDate(LocalDate.of(2024, 9, 1));
        semester.setEndDate(LocalDate.of(2025, 1, 31));
        semester = semesterRepository.saveAndFlush(semester);

        // Login to get session cookie
        String loginJson = objectMapper.writeValueAsString(new LoginRequest(HOD_EMAIL, HOD_PASSWORD));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        var cookie = loginResult.getResponse().getCookie("ARCHIVESESSION");
        assertThat(cookie).isNotNull();
        sessionCookie = cookie.getValue();
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        documentSubmissionRepository.deleteAll();
        courseAssignmentRepository.deleteAll();
        courseRepository.deleteAll();
        semesterRepository.deleteAll();
        academicYearRepository.deleteAll();
        
        userRepository.findByEmail(HOD_EMAIL).ifPresent(user -> {
            userRepository.delete(user);
            userRepository.flush();
        });
    }

    /**
     * Test: GET /api/hod/dashboard/overview returns department-scoped data
     * Requirement: 7.1, 7.3
     */
    @Test
    @Order(1)
    @DisplayName("Dashboard overview should return only HOD's department data")
    void testDashboardOverviewDepartmentScoped() throws Exception {
        // Create professor in HOD's department
        User professorInDept = createProfessor("prof.in.dept@alquds.edu", hodDepartment);
        
        // Create professor in other department
        User professorInOther = createProfessor("prof.in.other@alquds.edu", otherDepartment);

        // Create courses in both departments
        Course courseInDept = createCourse("CS101", "Computer Science 101", hodDepartment);
        Course courseInOther = createCourse("MATH101", "Mathematics 101", otherDepartment);

        // Create course assignments
        CourseAssignment assignmentInDept = createCourseAssignment(semester, courseInDept, professorInDept);
        CourseAssignment assignmentInOther = createCourseAssignment(semester, courseInOther, professorInOther);

        // Get dashboard overview
        mockMvc.perform(get("/api/hod/dashboard/overview")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", sessionCookie))
                        .param("semesterId", semester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.departmentId").value(hodDepartment.getId()))
                .andExpect(jsonPath("$.data.departmentName").value(hodDepartment.getName()))
                .andExpect(jsonPath("$.data.totalProfessors").value(1)) // Only professor in HOD's dept
                .andExpect(jsonPath("$.data.totalCourses").value(1)); // Only course in HOD's dept

        // Clean up
        userRepository.delete(professorInDept);
        userRepository.delete(professorInOther);
    }

    /**
     * Test: GET /api/hod/submissions/status filters by department
     * Requirement: 7.4
     */
    @Test
    @Order(2)
    @DisplayName("Submission status should filter by HOD's department")
    void testSubmissionStatusFiltersByDepartment() throws Exception {
        // Create professors in both departments
        User professorInDept = createProfessor("prof.dept@alquds.edu", hodDepartment);
        User professorInOther = createProfessor("prof.other@alquds.edu", otherDepartment);

        // Create courses
        Course courseInDept = createCourse("CS201", "Data Structures", hodDepartment);
        Course courseInOther = createCourse("MATH201", "Calculus", otherDepartment);

        // Create course assignments
        CourseAssignment assignmentInDept = createCourseAssignment(semester, courseInDept, professorInDept);
        CourseAssignment assignmentInOther = createCourseAssignment(semester, courseInOther, professorInOther);

        // Create submissions
        createDocumentSubmission(assignmentInDept, professorInDept, DocumentTypeEnum.SYLLABUS);
        createDocumentSubmission(assignmentInOther, professorInOther, DocumentTypeEnum.SYLLABUS);

        // Get submission status
        MvcResult result = mockMvc.perform(get("/api/hod/submissions/status")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", sessionCookie))
                        .param("semesterId", semester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.departmentId").value(hodDepartment.getId()))
                .andReturn();

        // Verify only HOD's department data is returned
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains(professorInDept.getEmail());
        assertThat(responseBody).doesNotContain(professorInOther.getEmail());

        // Clean up
        userRepository.delete(professorInDept);
        userRepository.delete(professorInOther);
    }

    /**
     * Test: HOD cannot access other departments' data
     * Requirement: 7.1
     */
    @Test
    @Order(3)
    @DisplayName("HOD should not access other departments' data")
    void testHodCannotAccessOtherDepartments() throws Exception {
        // Create professor in other department
        User professorInOther = createProfessor("prof.restricted@alquds.edu", otherDepartment);
        Course courseInOther = createCourse("PHYS101", "Physics 101", otherDepartment);
        CourseAssignment assignmentInOther = createCourseAssignment(semester, courseInOther, professorInOther);

        // Try to get submission status - should only return HOD's department data
        MvcResult result = mockMvc.perform(get("/api/hod/submissions/status")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", sessionCookie))
                        .param("semesterId", semester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.departmentId").value(hodDepartment.getId()))
                .andReturn();

        // Verify other department's data is not included
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain(professorInOther.getEmail());
        assertThat(responseBody).doesNotContain("PHYS101");

        // Clean up
        userRepository.delete(professorInOther);
    }

    // Helper methods

    private User createProfessor(String email, Department department) {
        User professor = new User();
        professor.setEmail(email);
        professor.setPassword(passwordEncoder.encode("Test123!"));
        professor.setFirstName("Test");
        professor.setLastName("Professor");
        professor.setRole(Role.ROLE_PROFESSOR);
        professor.setDepartment(department);
        professor.setProfessorId("PROF-" + System.currentTimeMillis());
        return userRepository.saveAndFlush(professor);
    }

    private Course createCourse(String code, String name, Department department) {
        Course course = new Course();
        course.setCourseCode(code);
        course.setCourseName(name);
        course.setDepartment(department);
        course.setLevel("Undergraduate");
        return courseRepository.saveAndFlush(course);
    }

    private CourseAssignment createCourseAssignment(Semester semester, Course course, User professor) {
        CourseAssignment assignment = new CourseAssignment();
        assignment.setSemester(semester);
        assignment.setCourse(course);
        assignment.setProfessor(professor);
        return courseAssignmentRepository.saveAndFlush(assignment);
    }

    private DocumentSubmission createDocumentSubmission(CourseAssignment assignment, User professor, DocumentTypeEnum docType) {
        DocumentSubmission submission = new DocumentSubmission();
        submission.setCourseAssignment(assignment);
        submission.setProfessor(professor);
        submission.setDocumentType(docType);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.UPLOADED);
        return documentSubmissionRepository.saveAndFlush(submission);
    }

    // Helper class for login request
    private static class LoginRequest {
        private String email;
        private String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
