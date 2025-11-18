package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.academic.AcademicYearDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.CourseAssignmentDTO;
import com.alqude.edu.ArchiveSystem.dto.user.ProfessorDTO;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DeanshipController.
 * Tests academic year creation, professor management, and course assignments.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"null", "unused"})
class DeanshipControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private static final String DEANSHIP_EMAIL = "deanship.test@alquds.edu";
    private static final String DEANSHIP_PASSWORD = "DeanshipTest123!";
    
    private User deanshipUser;
    private Department testDepartment;
    private String sessionCookie;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up existing test data
        userRepository.findByEmail(DEANSHIP_EMAIL).ifPresent(user -> {
            userRepository.delete(user);
            userRepository.flush();
        });

        // Create test department
        testDepartment = departmentRepository.findByName("Test Department Deanship")
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setName("Test Department Deanship");
                    dept.setDescription("Test department for deanship integration tests");
                    return departmentRepository.saveAndFlush(dept);
                });

        // Create deanship user
        deanshipUser = new User();
        deanshipUser.setEmail(DEANSHIP_EMAIL);
        deanshipUser.setPassword(passwordEncoder.encode(DEANSHIP_PASSWORD));
        deanshipUser.setFirstName("Deanship");
        deanshipUser.setLastName("Test");
        deanshipUser.setRole(Role.ROLE_DEANSHIP);
        deanshipUser.setDepartment(testDepartment);
        deanshipUser = userRepository.saveAndFlush(deanshipUser);

        // Login to get session cookie
        String loginJson = objectMapper.writeValueAsString(new LoginRequest(DEANSHIP_EMAIL, DEANSHIP_PASSWORD));
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
        courseAssignmentRepository.deleteAll();
        courseRepository.deleteAll();
        semesterRepository.deleteAll();
        academicYearRepository.deleteAll();
        
        userRepository.findByEmail(DEANSHIP_EMAIL).ifPresent(user -> {
            userRepository.delete(user);
            userRepository.flush();
        });
    }

    /**
     * Test: POST /api/deanship/academic-years creates year and three semesters
     * Requirement: 2.2
     */
    @Test
    @Order(1)
    @DisplayName("Creating academic year should auto-create three semesters")
    void testCreateAcademicYearWithSemesters() throws Exception {
        // Prepare academic year DTO
        AcademicYearDTO dto = new AcademicYearDTO();
        dto.setYearCode("2024-2025");
        dto.setStartYear(2024);
        dto.setEndYear(2025);

        // Create academic year
        MvcResult result = mockMvc.perform(post("/api/deanship/academic-years")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", sessionCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.yearCode").value("2024-2025"))
                .andExpect(jsonPath("$.data.startYear").value(2024))
                .andExpect(jsonPath("$.data.endYear").value(2025))
                .andReturn();

        // Extract academic year ID from response
        String responseBody = result.getResponse().getContentAsString();
        Long academicYearId = objectMapper.readTree(responseBody).get("data").get("id").asLong();

        // Verify three semesters were created
        List<Semester> semesters = semesterRepository.findByAcademicYearId(academicYearId);
        assertThat(semesters).hasSize(3);
        
        // Verify semester types
        assertThat(semesters).extracting(Semester::getType)
                .containsExactlyInAnyOrder(SemesterType.FIRST, SemesterType.SECOND, SemesterType.SUMMER);
    }

    /**
     * Test: POST /api/deanship/professors creates professor with professor_id
     * Requirement: 3.2
     */
    @Test
    @Order(2)
    @DisplayName("Creating professor should generate unique professor_id")
    void testCreateProfessorWithProfessorId() throws Exception {
        // Prepare professor DTO
        ProfessorDTO dto = new ProfessorDTO();
        dto.setEmail("professor.test@alquds.edu");
        dto.setPassword("ProfTest123!");
        dto.setFirstName("Test");
        dto.setLastName("Professor");
        dto.setDepartmentId(testDepartment.getId());

        // Create professor
        MvcResult result = mockMvc.perform(post("/api/deanship/professors")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", sessionCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("professor.test@alquds.edu"))
                .andExpect(jsonPath("$.data.role").value("ROLE_PROFESSOR"))
                .andExpect(jsonPath("$.data.professorId").exists())
                .andReturn();

        // Extract professor ID and verify professor_id is not null
        String responseBody = result.getResponse().getContentAsString();
        String professorId = objectMapper.readTree(responseBody).get("data").get("professorId").asText();
        
        assertThat(professorId).isNotNull().isNotEmpty();
        assertThat(professorId).startsWith("PROF-");

        // Clean up
        userRepository.findByEmail("professor.test@alquds.edu").ifPresent(userRepository::delete);
    }

    /**
     * Test: POST /api/deanship/course-assignments creates assignment
     * Requirement: 2.4
     */
    @Test
    @Order(3)
    @DisplayName("Creating course assignment should link professor, course, and semester")
    void testCreateCourseAssignment() throws Exception {
        // Create academic year and semester
        AcademicYear academicYear = new AcademicYear();
        academicYear.setYearCode("2024-2025");
        academicYear.setStartYear(2024);
        academicYear.setEndYear(2025);
        academicYear = academicYearRepository.saveAndFlush(academicYear);

        Semester semester = new Semester();
        semester.setAcademicYear(academicYear);
        semester.setType(SemesterType.FIRST);
        semester.setStartDate(java.time.LocalDate.of(2024, 9, 1));
        semester.setEndDate(java.time.LocalDate.of(2025, 1, 31));
        semester = semesterRepository.saveAndFlush(semester);

        // Create professor
        User professor = new User();
        professor.setEmail("prof.assignment@alquds.edu");
        professor.setPassword(passwordEncoder.encode("Test123!"));
        professor.setFirstName("Assignment");
        professor.setLastName("Professor");
        professor.setRole(Role.ROLE_PROFESSOR);
        professor.setDepartment(testDepartment);
        professor.setProfessorId("PROF-TEST-001");
        professor = userRepository.saveAndFlush(professor);

        // Create course
        Course course = new Course();
        course.setCourseCode("CS101");
        course.setCourseName("Introduction to Computer Science");
        course.setDepartment(testDepartment);
        course.setLevel("Undergraduate");
        course = courseRepository.saveAndFlush(course);

        // Prepare course assignment DTO
        CourseAssignmentDTO dto = new CourseAssignmentDTO();
        dto.setSemesterId(semester.getId());
        dto.setCourseId(course.getId());
        dto.setProfessorId(professor.getId());

        // Create course assignment
        MvcResult result = mockMvc.perform(post("/api/deanship/course-assignments")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", sessionCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.semester.id").value(semester.getId()))
                .andExpect(jsonPath("$.data.course.id").value(course.getId()))
                .andExpect(jsonPath("$.data.professor.id").value(professor.getId()))
                .andReturn();

        // Verify course assignment was created in database
        String responseBody = result.getResponse().getContentAsString();
        Long assignmentId = objectMapper.readTree(responseBody).get("data").get("id").asLong();
        
        CourseAssignment assignment = courseAssignmentRepository.findById(assignmentId).orElse(null);
        assertThat(assignment).isNotNull();
        assertThat(assignment.getSemester().getId()).isEqualTo(semester.getId());
        assertThat(assignment.getCourse().getId()).isEqualTo(course.getId());
        assertThat(assignment.getProfessor().getId()).isEqualTo(professor.getId());

        // Clean up
        userRepository.findByEmail("prof.assignment@alquds.edu").ifPresent(userRepository::delete);
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
