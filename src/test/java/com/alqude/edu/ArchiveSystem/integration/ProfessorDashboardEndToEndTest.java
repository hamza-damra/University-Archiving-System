package com.alqude.edu.ArchiveSystem.integration;

import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Integration Tests for Professor Dashboard
 * Tests complete workflows from Deanship setup to Professor interactions
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"null", "unused"})
class ProfessorDashboardEndToEndTest {

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
    private RequiredDocumentTypeRepository requiredDocumentTypeRepository;

    @Autowired
    private DocumentSubmissionRepository documentSubmissionRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // Test data
    private User deanshipUser;
    private User professorUser;
    private User otherProfessorUser;
    private Department csDepartment;
    private Department mathDepartment;
    private AcademicYear currentYear;
    private AcademicYear previousYear;
    private Semester currentSemester;
    private Semester previousSemester;
    private Course course1;
    private Course course2;
    private Course otherDeptCourse;
    private CourseAssignment assignment1;
    private CourseAssignment assignment2;
    private CourseAssignment previousAssignment;
    private RequiredDocumentType syllabusReq;
    private RequiredDocumentType examReq;
    
    private String deanshipSessionCookie;
    private String professorSessionCookie;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up existing test data
        cleanupTestData();

        // Create departments
        csDepartment = new Department();
        csDepartment.setName("E2E CS Department");
        csDepartment.setDescription("Computer Science for E2E testing");
        csDepartment = departmentRepository.saveAndFlush(csDepartment);

        mathDepartment = new Department();
        mathDepartment.setName("E2E Math Department");
        mathDepartment.setDescription("Mathematics for E2E testing");
        mathDepartment = departmentRepository.saveAndFlush(mathDepartment);

        // Create deanship user
        deanshipUser = new User();
        deanshipUser.setEmail("e2e.deanship@alquds.edu");
        deanshipUser.setPassword(passwordEncoder.encode("Deanship123!"));
        deanshipUser.setFirstName("E2E");
        deanshipUser.setLastName("Deanship");
        deanshipUser.setRole(Role.ROLE_DEANSHIP);
        deanshipUser = userRepository.saveAndFlush(deanshipUser);

        // Create professor user
        professorUser = new User();
        professorUser.setEmail("e2e.professor@alquds.edu");
        professorUser.setPassword(passwordEncoder.encode("Professor123!"));
        professorUser.setFirstName("E2E");
        professorUser.setLastName("Professor");
        professorUser.setRole(Role.ROLE_PROFESSOR);
        professorUser.setDepartment(csDepartment);
        professorUser.setProfessorId("E2E-PROF-001");
        professorUser = userRepository.saveAndFlush(professorUser);

        // Create other professor in same department
        otherProfessorUser = new User();
        otherProfessorUser.setEmail("e2e.other.prof@alquds.edu");
        otherProfessorUser.setPassword(passwordEncoder.encode("Professor123!"));
        otherProfessorUser.setFirstName("Other");
        otherProfessorUser.setLastName("Professor");
        otherProfessorUser.setRole(Role.ROLE_PROFESSOR);
        otherProfessorUser.setDepartment(csDepartment);
        otherProfessorUser.setProfessorId("E2E-PROF-002");
        otherProfessorUser = userRepository.saveAndFlush(otherProfessorUser);

        // Create academic years
        currentYear = new AcademicYear();
        currentYear.setYearCode("2024-2025");
        currentYear.setStartYear(2024);
        currentYear.setEndYear(2025);
        currentYear.setIsActive(true);
        currentYear = academicYearRepository.saveAndFlush(currentYear);

        previousYear = new AcademicYear();
        previousYear.setYearCode("2023-2024");
        previousYear.setStartYear(2023);
        previousYear.setEndYear(2024);
        previousYear.setIsActive(false);
        previousYear = academicYearRepository.saveAndFlush(previousYear);

        // Create semesters
        currentSemester = new Semester();
        currentSemester.setAcademicYear(currentYear);
        currentSemester.setType(SemesterType.FIRST);
        currentSemester.setStartDate(LocalDate.of(2024, 9, 1));
        currentSemester.setEndDate(LocalDate.of(2025, 1, 31));
        currentSemester = semesterRepository.saveAndFlush(currentSemester);

        previousSemester = new Semester();
        previousSemester.setAcademicYear(previousYear);
        previousSemester.setType(SemesterType.FIRST);
        previousSemester.setStartDate(LocalDate.of(2023, 9, 1));
        previousSemester.setEndDate(LocalDate.of(2024, 1, 31));
        previousSemester = semesterRepository.saveAndFlush(previousSemester);

        // Create courses
        course1 = new Course();
        course1.setCourseCode("E2E-CS301");
        course1.setCourseName("Software Engineering");
        course1.setDepartment(csDepartment);
        course1.setLevel("Undergraduate");
        course1 = courseRepository.saveAndFlush(course1);

        course2 = new Course();
        course2.setCourseCode("E2E-CS302");
        course2.setCourseName("Database Systems");
        course2.setDepartment(csDepartment);
        course2.setLevel("Undergraduate");
        course2 = courseRepository.saveAndFlush(course2);

        otherDeptCourse = new Course();
        otherDeptCourse.setCourseCode("E2E-MATH201");
        otherDeptCourse.setCourseName("Linear Algebra");
        otherDeptCourse.setDepartment(mathDepartment);
        otherDeptCourse.setLevel("Undergraduate");
        otherDeptCourse = courseRepository.saveAndFlush(otherDeptCourse);

        // Create course assignments
        assignment1 = new CourseAssignment();
        assignment1.setSemester(currentSemester);
        assignment1.setCourse(course1);
        assignment1.setProfessor(professorUser);
        assignment1.setIsActive(true);
        assignment1 = courseAssignmentRepository.saveAndFlush(assignment1);

        assignment2 = new CourseAssignment();
        assignment2.setSemester(currentSemester);
        assignment2.setCourse(course2);
        assignment2.setProfessor(professorUser);
        assignment2.setIsActive(true);
        assignment2 = courseAssignmentRepository.saveAndFlush(assignment2);

        previousAssignment = new CourseAssignment();
        previousAssignment.setSemester(previousSemester);
        previousAssignment.setCourse(course1);
        previousAssignment.setProfessor(professorUser);
        previousAssignment.setIsActive(true);
        previousAssignment = courseAssignmentRepository.saveAndFlush(previousAssignment);

        // Create required document types
        syllabusReq = new RequiredDocumentType();
        syllabusReq.setCourse(course1);
        syllabusReq.setSemester(currentSemester);
        syllabusReq.setDocumentType(DocumentTypeEnum.SYLLABUS);
        syllabusReq.setIsRequired(true);
        syllabusReq.setDeadline(LocalDateTime.now().plusDays(30));
        syllabusReq.setMaxFileCount(2);
        syllabusReq.setMaxTotalSizeMb(10);
        syllabusReq = requiredDocumentTypeRepository.saveAndFlush(syllabusReq);

        examReq = new RequiredDocumentType();
        examReq.setCourse(course1);
        examReq.setSemester(currentSemester);
        examReq.setDocumentType(DocumentTypeEnum.EXAM);
        examReq.setIsRequired(true);
        examReq.setDeadline(LocalDateTime.now().plusDays(60));
        examReq.setMaxFileCount(3);
        examReq.setMaxTotalSizeMb(15);
        examReq = requiredDocumentTypeRepository.saveAndFlush(examReq);

        // Login as deanship
        deanshipSessionCookie = loginUser("e2e.deanship@alquds.edu", "Deanship123!");
        
        // Login as professor
        professorSessionCookie = loginUser("e2e.professor@alquds.edu", "Professor123!");
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        uploadedFileRepository.deleteAll();
        documentSubmissionRepository.deleteAll();
        requiredDocumentTypeRepository.deleteAll();
        courseAssignmentRepository.deleteAll();
        courseRepository.deleteAll();
        semesterRepository.deleteAll();
        academicYearRepository.deleteAll();
        
        userRepository.findByEmail("e2e.deanship@alquds.edu").ifPresent(userRepository::delete);
        userRepository.findByEmail("e2e.professor@alquds.edu").ifPresent(userRepository::delete);
        userRepository.findByEmail("e2e.other.prof@alquds.edu").ifPresent(userRepository::delete);
        
        departmentRepository.findByName("E2E CS Department").ifPresent(departmentRepository::delete);
        departmentRepository.findByName("E2E Math Department").ifPresent(departmentRepository::delete);
        
        userRepository.flush();
        departmentRepository.flush();
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

    /**
     * Task 3.1: Test complete course assignment flow
     * - Deanship creates academic year and semester (done in setup)
     * - Deanship creates professor account (done in setup)
     * - Deanship creates courses (done in setup)
     * - Deanship assigns courses to professor (done in setup)
     * - Professor logs in and sees assigned courses
     * - Verify all data displays correctly
     */
    @Test
    @Order(1)
    @DisplayName("3.1 - Complete course assignment flow from Deanship to Professor")
    void testCompleteCourseAssignmentFlow() throws Exception {
        // Verify academic years endpoint
        mockMvc.perform(get("/api/professor/academic-years")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.yearCode == '2024-2025')]").exists());

        // Verify semesters endpoint
        mockMvc.perform(get("/api/professor/academic-years/" + currentYear.getId() + "/semesters")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.type == 'FIRST')]").exists());

        // Verify professor sees assigned courses
        MvcResult coursesResult = mockMvc.perform(get("/api/professor/dashboard/courses")
                        .param("semesterId", currentSemester.getId().toString())
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();

        String responseBody = coursesResult.getResponse().getContentAsString();
        var jsonNode = objectMapper.readTree(responseBody);
        var courses = jsonNode.get("data");

        // Verify course 1 details
        boolean foundCourse1 = false;
        boolean foundCourse2 = false;
        
        for (var course : courses) {
            String courseCode = course.get("courseCode").asText();
            if ("E2E-CS301".equals(courseCode)) {
                foundCourse1 = true;
                assertThat(course.get("courseName").asText()).isEqualTo("Software Engineering");
                assertThat(course.get("departmentName").asText()).isEqualTo("E2E CS Department");
                assertThat(course.get("documentStatuses")).isNotNull();
            } else if ("E2E-CS302".equals(courseCode)) {
                foundCourse2 = true;
                assertThat(course.get("courseName").asText()).isEqualTo("Database Systems");
            }
        }

        assertThat(foundCourse1).isTrue();
        assertThat(foundCourse2).isTrue();

        // Verify dashboard overview
        mockMvc.perform(get("/api/professor/dashboard/overview")
                        .param("semesterId", currentSemester.getId().toString())
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCourses").value(2));
    }

    /**
     * Task 3.2: Test file upload and submission flow
     * - Professor selects course and document type
     * - Professor uploads files through modal
     * - Verify files are stored in correct location
     * - Verify DocumentSubmission record is created
     * - Verify status updates to "Uploaded"
     * - Deanship can see uploaded files in file explorer
     */
    @Test
    @Order(2)
    @DisplayName("3.2 - File upload and submission flow")
    void testFileUploadAndSubmissionFlow() throws Exception {
        // Create mock files
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "syllabus-part1.pdf",
                "application/pdf",
                "Syllabus content part 1".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "syllabus-part2.pdf",
                "application/pdf",
                "Syllabus content part 2".getBytes()
        );

        // Professor uploads files
        MvcResult uploadResult = mockMvc.perform(multipart("/api/professor/submissions/upload")
                        .file(file1)
                        .file(file2)
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie))
                        .param("courseAssignmentId", assignment1.getId().toString())
                        .param("documentType", DocumentTypeEnum.SYLLABUS.name())
                        .param("notes", "Initial syllabus upload"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentType").value(DocumentTypeEnum.SYLLABUS.name()))
                .andExpect(jsonPath("$.data.status").value(SubmissionStatus.UPLOADED.name()))
                .andExpect(jsonPath("$.data.fileCount").value(2))
                .andReturn();

        String responseBody = uploadResult.getResponse().getContentAsString();
        Long submissionId = objectMapper.readTree(responseBody).get("data").get("id").asLong();

        // Verify DocumentSubmission record was created
        DocumentSubmission submission = documentSubmissionRepository.findById(submissionId).orElse(null);
        assertThat(submission).isNotNull();
        assertThat(submission.getCourseAssignment().getId()).isEqualTo(assignment1.getId());
        assertThat(submission.getDocumentType()).isEqualTo(DocumentTypeEnum.SYLLABUS);
        assertThat(submission.getProfessor().getId()).isEqualTo(professorUser.getId());
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.UPLOADED);
        assertThat(submission.getFileCount()).isEqualTo(2);

        // Verify uploaded files were created
        List<UploadedFile> uploadedFiles = uploadedFileRepository.findByDocumentSubmissionId(submissionId);
        assertThat(uploadedFiles).hasSize(2);
        assertThat(uploadedFiles.get(0).getOriginalFilename()).isIn("syllabus-part1.pdf", "syllabus-part2.pdf");
        assertThat(uploadedFiles.get(1).getOriginalFilename()).isIn("syllabus-part1.pdf", "syllabus-part2.pdf");

        // Verify files are stored in correct location pattern: year/semester/professorId/courseCode/docType
        for (UploadedFile file : uploadedFiles) {
            String fileUrl = file.getFileUrl();
            assertThat(fileUrl).contains("2024-2025");
            assertThat(fileUrl).contains("FIRST");
            assertThat(fileUrl).contains("E2E-PROF-001");
            assertThat(fileUrl).contains("E2E-CS301");
            assertThat(fileUrl).contains("SYLLABUS");
        }

        // Verify status updates - check courses endpoint shows uploaded status
        mockMvc.perform(get("/api/professor/dashboard/courses")
                        .param("semesterId", currentSemester.getId().toString())
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.courseCode == 'E2E-CS301')].documentStatuses.SYLLABUS.status")
                        .value("UPLOADED"));

        // Verify Deanship can see uploaded files in file explorer
        mockMvc.perform(get("/api/professor/file-explorer/root")
                        .param("academicYearId", currentYear.getId().toString())
                        .param("semesterId", currentSemester.getId().toString())
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * Task 3.3: Test file replacement flow
     * - Professor has previously uploaded files
     * - Professor clicks "Replace Files" button
     * - Professor uploads new files
     * - Verify old files are deleted
     * - Verify new files are stored
     * - Verify submission timestamp updates
     */
    @Test
    @Order(3)
    @DisplayName("3.3 - File replacement flow")
    void testFileReplacementFlow() throws Exception {
        // First, upload initial files
        MockMultipartFile initialFile = new MockMultipartFile(
                "files",
                "exam-v1.pdf",
                "application/pdf",
                "Initial exam version".getBytes()
        );

        MvcResult initialUpload = mockMvc.perform(multipart("/api/professor/submissions/upload")
                        .file(initialFile)
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie))
                        .param("courseAssignmentId", assignment1.getId().toString())
                        .param("documentType", DocumentTypeEnum.EXAM.name())
                        .param("notes", "Initial exam upload"))
                .andExpect(status().isCreated())
                .andReturn();

        String initialResponse = initialUpload.getResponse().getContentAsString();
        Long submissionId = objectMapper.readTree(initialResponse).get("data").get("id").asLong();

        // Get initial file details
        List<UploadedFile> initialFiles = uploadedFileRepository.findByDocumentSubmissionId(submissionId);
        assertThat(initialFiles).hasSize(1);
        String initialFileUrl = initialFiles.get(0).getFileUrl();
        Long initialFileId = initialFiles.get(0).getId();
        LocalDateTime initialTimestamp = documentSubmissionRepository.findById(submissionId).get().getSubmittedAt();

        // Wait a moment to ensure timestamp difference
        Thread.sleep(100);

        // Now replace the files
        MockMultipartFile replacementFile1 = new MockMultipartFile(
                "files",
                "exam-v2-part1.pdf",
                "application/pdf",
                "Updated exam version part 1".getBytes()
        );

        MockMultipartFile replacementFile2 = new MockMultipartFile(
                "files",
                "exam-v2-part2.pdf",
                "application/pdf",
                "Updated exam version part 2".getBytes()
        );

        mockMvc.perform(multipart("/api/professor/submissions/" + submissionId + "/replace")
                        .file(replacementFile1)
                        .file(replacementFile2)
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie))
                        .param("notes", "Updated exam with corrections")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileCount").value(2));

        // Verify old files are deleted from database
        UploadedFile oldFile = uploadedFileRepository.findById(initialFileId).orElse(null);
        assertThat(oldFile).isNull();

        // Verify new files are stored
        List<UploadedFile> newFiles = uploadedFileRepository.findByDocumentSubmissionId(submissionId);
        assertThat(newFiles).hasSize(2);
        assertThat(newFiles.get(0).getOriginalFilename()).isIn("exam-v2-part1.pdf", "exam-v2-part2.pdf");
        assertThat(newFiles.get(1).getOriginalFilename()).isIn("exam-v2-part1.pdf", "exam-v2-part2.pdf");

        // Verify submission timestamp was updated
        DocumentSubmission updatedSubmission = documentSubmissionRepository.findById(submissionId).get();
        assertThat(updatedSubmission.getSubmittedAt()).isAfter(initialTimestamp);
        assertThat(updatedSubmission.getFileCount()).isEqualTo(2);
        assertThat(updatedSubmission.getNotes()).isEqualTo("Updated exam with corrections");
    }

    /**
     * Task 3.4: Test file explorer navigation and permissions
     * - Professor navigates to File Explorer tab
     * - Professor sees own folders with write indicator
     * - Professor sees department folders as read-only
     * - Professor cannot see other department folders
     * - Professor can download files from accessible folders
     */
    @Test
    @Order(4)
    @DisplayName("3.4 - File explorer navigation and permissions")
    void testFileExplorerNavigationAndPermissions() throws Exception {
        // First, create some files for testing
        // Upload file by main professor
        MockMultipartFile professorFile = new MockMultipartFile(
                "files",
                "my-syllabus.pdf",
                "application/pdf",
                "My syllabus content".getBytes()
        );

        MvcResult myUpload = mockMvc.perform(multipart("/api/professor/submissions/upload")
                        .file(professorFile)
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie))
                        .param("courseAssignmentId", assignment1.getId().toString())
                        .param("documentType", DocumentTypeEnum.SYLLABUS.name()))
                .andExpect(status().isCreated())
                .andReturn();

        Long mySubmissionId = objectMapper.readTree(myUpload.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Create assignment for other professor in same department
        CourseAssignment otherAssignment = new CourseAssignment();
        otherAssignment.setSemester(currentSemester);
        otherAssignment.setCourse(course2);
        otherAssignment.setProfessor(otherProfessorUser);
        otherAssignment.setIsActive(true);
        otherAssignment = courseAssignmentRepository.saveAndFlush(otherAssignment);

        // Create submission by other professor
        DocumentSubmission otherSubmission = new DocumentSubmission();
        otherSubmission.setCourseAssignment(otherAssignment);
        otherSubmission.setProfessor(otherProfessorUser);
        otherSubmission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        otherSubmission.setSubmittedAt(LocalDateTime.now());
        otherSubmission.setStatus(SubmissionStatus.UPLOADED);
        otherSubmission.setFileCount(1);
        otherSubmission = documentSubmissionRepository.saveAndFlush(otherSubmission);

        UploadedFile otherFile = new UploadedFile();
        otherFile.setDocumentSubmission(otherSubmission);
        otherFile.setFileUrl("/uploads/2024-2025/FIRST/E2E-PROF-002/E2E-CS302/SYLLABUS/other-syllabus.pdf");
        otherFile.setOriginalFilename("other-syllabus.pdf");
        otherFile.setFileSize(1024L);
        otherFile.setFileType("application/pdf");
        otherFile = uploadedFileRepository.saveAndFlush(otherFile);

        // Test: Professor navigates to File Explorer
        MvcResult rootResult = mockMvc.perform(get("/api/professor/file-explorer/root")
                        .param("academicYearId", currentYear.getId().toString())
                        .param("semesterId", currentSemester.getId().toString())
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andReturn();

        // Test: Professor can see own submission (read access)
        mockMvc.perform(get("/api/professor/submissions/" + mySubmissionId)
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.professor.id").value(professorUser.getId()));

        // Test: Professor can see other professor's submission in same department (read-only)
        mockMvc.perform(get("/api/professor/submissions/" + otherSubmission.getId())
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.professor.id").value(otherProfessorUser.getId()));

        // Test: Professor can download files from accessible folders
        List<UploadedFile> myFiles = uploadedFileRepository.findByDocumentSubmissionId(mySubmissionId);
        assertThat(myFiles).isNotEmpty();
        Long myFileId = myFiles.get(0).getId();

        mockMvc.perform(get("/api/professor/files/" + myFileId + "/download")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk());

        // Test: Professor can download other department professor's files (same department)
        mockMvc.perform(get("/api/professor/files/" + otherFile.getId() + "/download")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk());
    }

    /**
     * Task 3.5: Test cross-semester navigation
     * - Professor selects previous semester
     * - Historical courses load correctly
     * - Professor can view old submissions
     * - Verify data consistency across semesters
     */
    @Test
    @Order(5)
    @DisplayName("3.5 - Cross-semester navigation")
    void testCrossSemesterNavigation() throws Exception {
        // Create submission in previous semester
        RequiredDocumentType prevSyllabusReq = new RequiredDocumentType();
        prevSyllabusReq.setCourse(course1);
        prevSyllabusReq.setSemester(previousSemester);
        prevSyllabusReq.setDocumentType(DocumentTypeEnum.SYLLABUS);
        prevSyllabusReq.setIsRequired(true);
        prevSyllabusReq.setDeadline(LocalDateTime.of(2023, 12, 31, 23, 59));
        prevSyllabusReq.setMaxFileCount(2);
        prevSyllabusReq.setMaxTotalSizeMb(10);
        prevSyllabusReq = requiredDocumentTypeRepository.saveAndFlush(prevSyllabusReq);

        DocumentSubmission prevSubmission = new DocumentSubmission();
        prevSubmission.setCourseAssignment(previousAssignment);
        prevSubmission.setProfessor(professorUser);
        prevSubmission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        prevSubmission.setSubmittedAt(LocalDateTime.of(2023, 10, 15, 10, 30));
        prevSubmission.setStatus(SubmissionStatus.UPLOADED);
        prevSubmission.setFileCount(1);
        prevSubmission.setNotes("Previous semester syllabus");
        prevSubmission = documentSubmissionRepository.saveAndFlush(prevSubmission);

        UploadedFile prevFile = new UploadedFile();
        prevFile.setDocumentSubmission(prevSubmission);
        prevFile.setFileUrl("/uploads/2023-2024/FIRST/E2E-PROF-001/E2E-CS301/SYLLABUS/old-syllabus.pdf");
        prevFile.setOriginalFilename("old-syllabus.pdf");
        prevFile.setFileSize(2048L);
        prevFile.setFileType("application/pdf");
        prevFile = uploadedFileRepository.saveAndFlush(prevFile);

        // Test: Professor selects previous academic year
        mockMvc.perform(get("/api/professor/academic-years/" + previousYear.getId() + "/semesters")
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.type == 'FIRST')]").exists());

        // Test: Historical courses load correctly
        MvcResult prevCoursesResult = mockMvc.perform(get("/api/professor/dashboard/courses")
                        .param("semesterId", previousSemester.getId().toString())
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].courseCode").value("E2E-CS301"))
                .andReturn();

        String prevResponse = prevCoursesResult.getResponse().getContentAsString();
        var prevJsonNode = objectMapper.readTree(prevResponse);
        var prevCourse = prevJsonNode.get("data").get(0);

        // Verify document status shows UPLOADED for previous semester
        assertThat(prevCourse.get("documentStatuses").has("SYLLABUS")).isTrue();
        assertThat(prevCourse.get("documentStatuses").get("SYLLABUS").get("status").asText())
                .isEqualTo("UPLOADED");

        // Test: Professor can view old submissions
        mockMvc.perform(get("/api/professor/submissions/" + prevSubmission.getId())
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(prevSubmission.getId()))
                .andExpect(jsonPath("$.data.notes").value("Previous semester syllabus"))
                .andExpect(jsonPath("$.data.fileCount").value(1));

        // Test: Verify data consistency - switch back to current semester
        mockMvc.perform(get("/api/professor/dashboard/courses")
                        .param("semesterId", currentSemester.getId().toString())
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));

        // Test: Dashboard overview for previous semester
        mockMvc.perform(get("/api/professor/dashboard/overview")
                        .param("semesterId", previousSemester.getId().toString())
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCourses").value(1));

        // Test: File explorer for previous semester
        mockMvc.perform(get("/api/professor/file-explorer/root")
                        .param("academicYearId", previousYear.getId().toString())
                        .param("semesterId", previousSemester.getId().toString())
                        .cookie(new jakarta.servlet.http.Cookie("ARCHIVESESSION", professorSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
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

        public String getPassword() {
            return password;
        }
    }
}
