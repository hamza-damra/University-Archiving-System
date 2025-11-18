package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
 * Integration tests for ProfessorController.
 * Tests file upload, permission enforcement, and department-scoped read access.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"null", "unused"})
class ProfessorControllerIntegrationTest {

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

    private static final String PROFESSOR_EMAIL = "professor.test@alquds.edu";
    private static final String PROFESSOR_PASSWORD = "ProfTest123!";
    
    private User professorUser;
    private User otherProfessor;
    private Department testDepartment;
    private String sessionCookie;
    private AcademicYear academicYear;
    private Semester semester;
    private Course course;
    private CourseAssignment courseAssignment;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up existing test data
        userRepository.findByEmail(PROFESSOR_EMAIL).ifPresent(user -> {
            userRepository.delete(user);
            userRepository.flush();
        });

        // Create test department
        testDepartment = departmentRepository.findByName("Professor Test Department")
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setName("Professor Test Department");
                    dept.setDescription("Test department for professor integration tests");
                    return departmentRepository.saveAndFlush(dept);
                });

        // Create professor user
        professorUser = new User();
        professorUser.setEmail(PROFESSOR_EMAIL);
        professorUser.setPassword(passwordEncoder.encode(PROFESSOR_PASSWORD));
        professorUser.setFirstName("Professor");
        professorUser.setLastName("Test");
        professorUser.setRole(Role.ROLE_PROFESSOR);
        professorUser.setDepartment(testDepartment);
        professorUser.setProfessorId("PROF-TEST-001");
        professorUser = userRepository.saveAndFlush(professorUser);

        // Create another professor in same department
        otherProfessor = new User();
        otherProfessor.setEmail("other.prof@alquds.edu");
        otherProfessor.setPassword(passwordEncoder.encode("Test123!"));
        otherProfessor.setFirstName("Other");
        otherProfessor.setLastName("Professor");
        otherProfessor.setRole(Role.ROLE_PROFESSOR);
        otherProfessor.setDepartment(testDepartment);
        otherProfessor.setProfessorId("PROF-TEST-002");
        otherProfessor = userRepository.saveAndFlush(otherProfessor);

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

        // Create course
        course = new Course();
        course.setCourseCode("CS301");
        course.setCourseName("Software Engineering");
        course.setDepartment(testDepartment);
        course.setLevel("Undergraduate");
        course = courseRepository.saveAndFlush(course);

        // Create course assignment for professor
        courseAssignment = new CourseAssignment();
        courseAssignment.setSemester(semester);
        courseAssignment.setCourse(course);
        courseAssignment.setProfessor(professorUser);
        courseAssignment = courseAssignmentRepository.saveAndFlush(courseAssignment);

        // Login to get session cookie
        String loginJson = objectMapper.writeValueAsString(new LoginRequest(PROFESSOR_EMAIL, PROFESSOR_PASSWORD));
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
        uploadedFileRepository.deleteAll();
        documentSubmissionRepository.deleteAll();
        courseAssignmentRepository.deleteAll();
        courseRepository.deleteAll();
        semesterRepository.deleteAll();
        academicYearRepository.deleteAll();
        
        userRepository.findByEmail(PROFESSOR_EMAIL).ifPresent(user -> {
            userRepository.delete(user);
            userRepository.flush();
        });
        userRepository.findByEmail("other.prof@alquds.edu").ifPresent(user -> {
            userRepository.delete(user);
            userRepository.flush();
        });
    }

    /**
     * Test: POST /api/professor/submissions/upload creates submission and files
     * Requirement: 8.3, 8.4
     */
    @Test
    @Order(1)
    @DisplayName("Uploading files should create submission and file records")
    void testUploadFilesCreatesSubmissionAndFiles() throws Exception {
        // Create mock file
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "syllabus.pdf",
                "application/pdf",
                "Test syllabus content".getBytes()
        );

        // Upload files
        MvcResult result = mockMvc.perform(multipart("/api/professor/submissions/upload")
                        .file(file)
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", sessionCookie))
                        .param("courseAssignmentId", courseAssignment.getId().toString())
                        .param("documentType", DocumentTypeEnum.SYLLABUS.name())
                        .param("notes", "Test syllabus upload"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentType").value(DocumentTypeEnum.SYLLABUS.name()))
                .andExpect(jsonPath("$.data.status").value(SubmissionStatus.UPLOADED.name()))
                .andReturn();

        // Verify submission was created
        String responseBody = result.getResponse().getContentAsString();
        Long submissionId = objectMapper.readTree(responseBody).get("data").get("id").asLong();
        
        DocumentSubmission submission = documentSubmissionRepository.findById(submissionId).orElse(null);
        assertThat(submission).isNotNull();
        assertThat(submission.getCourseAssignment().getId()).isEqualTo(courseAssignment.getId());
        assertThat(submission.getDocumentType()).isEqualTo(DocumentTypeEnum.SYLLABUS);
        assertThat(submission.getProfessor().getId()).isEqualTo(professorUser.getId());
        assertThat(submission.getFileCount()).isEqualTo(1);

        // Verify file was created
        var files = uploadedFileRepository.findByDocumentSubmissionId(submissionId);
        assertThat(files).hasSize(1);
        assertThat(files.get(0).getOriginalFilename()).isEqualTo("syllabus.pdf");
    }

    /**
     * Test: Professor cannot upload to unassigned course
     * Requirement: 8.3, 8.4
     */
    @Test
    @Order(2)
    @DisplayName("Professor should not upload to unassigned course")
    void testProfessorCannotUploadToUnassignedCourse() throws Exception {
        // Create another course not assigned to professor
        Course unassignedCourse = new Course();
        unassignedCourse.setCourseCode("CS401");
        unassignedCourse.setCourseName("Advanced Algorithms");
        unassignedCourse.setDepartment(testDepartment);
        unassignedCourse.setLevel("Graduate");
        unassignedCourse = courseRepository.saveAndFlush(unassignedCourse);

        // Create assignment for other professor
        CourseAssignment otherAssignment = new CourseAssignment();
        otherAssignment.setSemester(semester);
        otherAssignment.setCourse(unassignedCourse);
        otherAssignment.setProfessor(otherProfessor);
        otherAssignment = courseAssignmentRepository.saveAndFlush(otherAssignment);

        // Create mock file
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "exam.pdf",
                "application/pdf",
                "Test exam content".getBytes()
        );

        // Try to upload to unassigned course - should fail
        mockMvc.perform(multipart("/api/professor/submissions/upload")
                        .file(file)
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", sessionCookie))
                        .param("courseAssignmentId", otherAssignment.getId().toString())
                        .param("documentType", DocumentTypeEnum.EXAM.name()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: Professor can read other professors' files in same department
     * Requirement: 9.1, 9.2
     */
    @Test
    @Order(3)
    @DisplayName("Professor should read other professors' files in same department")
    void testProfessorCanReadDepartmentFiles() throws Exception {
        // Create course assignment for other professor
        Course otherCourse = new Course();
        otherCourse.setCourseCode("CS302");
        otherCourse.setCourseName("Database Systems");
        otherCourse.setDepartment(testDepartment);
        otherCourse.setLevel("Undergraduate");
        otherCourse = courseRepository.saveAndFlush(otherCourse);

        CourseAssignment otherAssignment = new CourseAssignment();
        otherAssignment.setSemester(semester);
        otherAssignment.setCourse(otherCourse);
        otherAssignment.setProfessor(otherProfessor);
        otherAssignment = courseAssignmentRepository.saveAndFlush(otherAssignment);

        // Create submission by other professor
        DocumentSubmission otherSubmission = new DocumentSubmission();
        otherSubmission.setCourseAssignment(otherAssignment);
        otherSubmission.setProfessor(otherProfessor);
        otherSubmission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        otherSubmission.setSubmittedAt(LocalDateTime.now());
        otherSubmission.setStatus(SubmissionStatus.UPLOADED);
        otherSubmission = documentSubmissionRepository.saveAndFlush(otherSubmission);

        // Create uploaded file
        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setDocumentSubmission(otherSubmission);
        uploadedFile.setFileUrl("/uploads/test-file.pdf");
        uploadedFile.setOriginalFilename("other-syllabus.pdf");
        uploadedFile.setFileSize(1024L);
        uploadedFile.setFileType("application/pdf");
        uploadedFile = uploadedFileRepository.saveAndFlush(uploadedFile);

        // Professor should be able to view the submission (read-only)
        mockMvc.perform(get("/api/professor/submissions/" + otherSubmission.getId())
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(otherSubmission.getId()))
                .andExpect(jsonPath("$.data.professor.id").value(otherProfessor.getId()));
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
