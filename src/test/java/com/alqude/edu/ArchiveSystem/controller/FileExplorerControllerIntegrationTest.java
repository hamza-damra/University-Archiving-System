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

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FileExplorerController.
 * Tests role-based file explorer access and file download permissions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"null", "unused"})
class FileExplorerControllerIntegrationTest {

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
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User deanshipUser;
    private User hodUser;
    private User professorUser;
    private Department department1;
    private Department department2;
    private AcademicYear academicYear;
    private Semester semester;
    private String deanshipCookie;
    private String hodCookie;
    private String professorCookie;

    @BeforeEach
    void setUp() throws Exception {
        // Create departments
        department1 = departmentRepository.findByName("FileExplorer Dept 1")
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setName("FileExplorer Dept 1");
                    dept.setDescription("Test department 1 for file explorer tests");
                    return departmentRepository.saveAndFlush(dept);
                });

        department2 = departmentRepository.findByName("FileExplorer Dept 2")
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setName("FileExplorer Dept 2");
                    dept.setDescription("Test department 2 for file explorer tests");
                    return departmentRepository.saveAndFlush(dept);
                });

        // Create users
        deanshipUser = createUser("deanship.fe@alquds.edu", Role.ROLE_DEANSHIP, department1);
        hodUser = createUser("hod.fe@alquds.edu", Role.ROLE_HOD, department1);
        professorUser = createUser("prof.fe@alquds.edu", Role.ROLE_PROFESSOR, department1);

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

        // Login users
        deanshipCookie = loginUser("deanship.fe@alquds.edu", "Test123!");
        hodCookie = loginUser("hod.fe@alquds.edu", "Test123!");
        professorCookie = loginUser("prof.fe@alquds.edu", "Test123!");
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        uploadedFileRepository.deleteAll();
        documentSubmissionRepository.deleteAll();
        courseAssignmentRepository.deleteAll();
        courseRepository.deleteAll();
        semesterRepository.deleteAll();
        academicYearRepository.deleteAll();
        
        userRepository.findByEmail("deanship.fe@alquds.edu").ifPresent(userRepository::delete);
        userRepository.findByEmail("hod.fe@alquds.edu").ifPresent(userRepository::delete);
        userRepository.findByEmail("prof.fe@alquds.edu").ifPresent(userRepository::delete);
    }

    /**
     * Test: File explorer returns correct nodes based on role (Deanship sees all)
     * Requirement: 5.1
     */
    @Test
    @Order(1)
    @DisplayName("Deanship should see all departments in file explorer")
    void testDeanshipSeesAllDepartments() throws Exception {
        // Create professors in both departments
        User prof1 = createUser("prof1.fe@alquds.edu", Role.ROLE_PROFESSOR, department1);
        User prof2 = createUser("prof2.fe@alquds.edu", Role.ROLE_PROFESSOR, department2);

        // Create courses and assignments
        Course course1 = createCourse("CS101", "Course 1", department1);
        Course course2 = createCourse("MATH101", "Course 2", department2);
        
        createCourseAssignment(semester, course1, prof1);
        createCourseAssignment(semester, course2, prof2);

        // Get root node as Deanship
        MvcResult result = mockMvc.perform(get("/api/file-explorer/root")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", deanshipCookie))
                        .param("academicYearId", academicYear.getId().toString())
                        .param("semesterId", semester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // Verify both professors are visible
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains(prof1.getProfessorId());
        assertThat(responseBody).contains(prof2.getProfessorId());

        // Clean up
        userRepository.delete(prof1);
        userRepository.delete(prof2);
    }

    /**
     * Test: File explorer returns correct nodes based on role (HOD sees only department)
     * Requirement: 5.1
     */
    @Test
    @Order(2)
    @DisplayName("HOD should see only their department in file explorer")
    void testHodSeesOnlyDepartment() throws Exception {
        // Create professors in both departments
        User prof1 = createUser("prof3.fe@alquds.edu", Role.ROLE_PROFESSOR, department1);
        User prof2 = createUser("prof4.fe@alquds.edu", Role.ROLE_PROFESSOR, department2);

        // Create courses and assignments
        Course course1 = createCourse("CS201", "Course 3", department1);
        Course course2 = createCourse("MATH201", "Course 4", department2);
        
        createCourseAssignment(semester, course1, prof1);
        createCourseAssignment(semester, course2, prof2);

        // Get root node as HOD (department1)
        MvcResult result = mockMvc.perform(get("/api/file-explorer/root")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", hodCookie))
                        .param("academicYearId", academicYear.getId().toString())
                        .param("semesterId", semester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // Verify only department1 professor is visible
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains(prof1.getProfessorId());
        assertThat(responseBody).doesNotContain(prof2.getProfessorId());

        // Clean up
        userRepository.delete(prof1);
        userRepository.delete(prof2);
    }

    /**
     * Test: File explorer returns correct nodes based on role (Professor sees department)
     * Requirement: 5.1
     */
    @Test
    @Order(3)
    @DisplayName("Professor should see their department in file explorer")
    void testProfessorSeesDepartment() throws Exception {
        // Create another professor in same department
        User otherProf = createUser("prof5.fe@alquds.edu", Role.ROLE_PROFESSOR, department1);

        // Create courses and assignments
        Course course1 = createCourse("CS301", "Course 5", department1);
        Course course2 = createCourse("CS302", "Course 6", department1);
        
        createCourseAssignment(semester, course1, professorUser);
        createCourseAssignment(semester, course2, otherProf);

        // Get root node as Professor
        MvcResult result = mockMvc.perform(get("/api/file-explorer/root")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorCookie))
                        .param("academicYearId", academicYear.getId().toString())
                        .param("semesterId", semester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // Verify both professors in same department are visible
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains(professorUser.getProfessorId());
        assertThat(responseBody).contains(otherProf.getProfessorId());

        // Clean up
        userRepository.delete(otherProf);
    }

    /**
     * Test: File download enforces permissions
     * Requirement: 5.5
     */
    @Test
    @Order(4)
    @DisplayName("File download should enforce role-based permissions")
    void testFileDownloadEnforcesPermissions() throws Exception {
        // Create professor in department2
        User prof2 = createUser("prof6.fe@alquds.edu", Role.ROLE_PROFESSOR, department2);

        // Create course and assignment in department2
        Course course = createCourse("PHYS101", "Physics", department2);
        CourseAssignment assignment = createCourseAssignment(semester, course, prof2);

        // Create submission and file
        DocumentSubmission submission = new DocumentSubmission();
        submission.setCourseAssignment(assignment);
        submission.setProfessor(prof2);
        submission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.UPLOADED);
        submission = documentSubmissionRepository.saveAndFlush(submission);

        // Create a temporary test file
        File tempFile = File.createTempFile("test-file", ".pdf");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("Test file content".getBytes());
        }

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setDocumentSubmission(submission);
        uploadedFile.setFileUrl(tempFile.getAbsolutePath());
        uploadedFile.setOriginalFilename("test-syllabus.pdf");
        uploadedFile.setFileSize(1024L);
        uploadedFile.setFileType("application/pdf");
        uploadedFile = uploadedFileRepository.saveAndFlush(uploadedFile);

        // Deanship should be able to download (has access to all)
        mockMvc.perform(get("/api/file-explorer/files/" + uploadedFile.getId() + "/download")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", deanshipCookie)))
                .andExpect(status().isOk());

        // HOD from department1 should NOT be able to download (different department)
        mockMvc.perform(get("/api/file-explorer/files/" + uploadedFile.getId() + "/download")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", hodCookie)))
                .andExpect(status().isForbidden());

        // Professor from department1 should NOT be able to download (different department)
        mockMvc.perform(get("/api/file-explorer/files/" + uploadedFile.getId() + "/download")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorCookie)))
                .andExpect(status().isForbidden());

        // Clean up
        tempFile.delete();
        userRepository.delete(prof2);
    }

    // Helper methods

    private User createUser(String email, Role role, Department department) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Test123!"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        user.setDepartment(department);
        if (role == Role.ROLE_PROFESSOR) {
            user.setProfessorId("PROF-" + System.currentTimeMillis());
        }
        return userRepository.saveAndFlush(user);
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

    private String loginUser(String email, String password) throws Exception {
        String loginJson = objectMapper.writeValueAsString(new LoginRequest(email, password));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        var cookie = loginResult.getResponse().getCookie("ARCHIVESESSION");
        assertThat(cookie).isNotNull();
        return cookie.getValue();
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
