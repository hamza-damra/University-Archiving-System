package com.alqude.edu.ArchiveSystem.controller;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FileUploadController.
 * Tests file upload functionality with authentication, authorization, and validation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"null", "unused"})
class FileUploadControllerIntegrationTest {

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
    private FolderRepository folderRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private User professorUser;
    private User otherProfessorUser;
    private User deanshipUser;
    private Department department;
    private AcademicYear academicYear;
    private Semester semester;
    private Course course;
    private CourseAssignment courseAssignment;
    private Folder folder;
    private String professorCookie;
    private String otherProfessorCookie;
    private String deanshipCookie;

    @BeforeEach
    void setUp() throws Exception {
        // Create department
        department = departmentRepository.findByName("FileUpload Test Dept")
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setName("FileUpload Test Dept");
                    dept.setDescription("Test department for file upload tests");
                    return departmentRepository.saveAndFlush(dept);
                });

        // Create users
        professorUser = createUser("prof.upload@alquds.edu", Role.ROLE_PROFESSOR, department, "PROF001");
        otherProfessorUser = createUser("prof.other@alquds.edu", Role.ROLE_PROFESSOR, department, "PROF002");
        deanshipUser = createUser("dean.upload@alquds.edu", Role.ROLE_DEANSHIP, department, null);

        // Create academic year and semester
        academicYear = academicYearRepository.findByYearCode("2024-2025")
                .orElseGet(() -> {
                    AcademicYear year = new AcademicYear();
                    year.setYearCode("2024-2025");
                    year.setStartYear(2024);
                    year.setEndYear(2025);
                    return academicYearRepository.saveAndFlush(year);
                });

        semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), SemesterType.FIRST)
                .orElseGet(() -> {
                    Semester sem = new Semester();
                    sem.setAcademicYear(academicYear);
                    sem.setType(SemesterType.FIRST);
                    sem.setStartDate(LocalDate.of(2024, 9, 1));
                    sem.setEndDate(LocalDate.of(2025, 1, 31));
                    return semesterRepository.saveAndFlush(sem);
                });

        // Create course and assignment
        course = createCourse("CS101", "Introduction to CS", department);
        courseAssignment = createCourseAssignment(semester, course, professorUser);

        // Create folder
        folder = createFolder("2024-2025/first/PROF001/CS101/Syllabus", "Syllabus", FolderType.SUBFOLDER, professorUser);

        // Login users
        professorCookie = loginUser("prof.upload@alquds.edu", "Test123!");
        otherProfessorCookie = loginUser("prof.other@alquds.edu", "Test123!");
        deanshipCookie = loginUser("dean.upload@alquds.edu", "Test123!");
    }

    @AfterEach
    void tearDown() {
        // Clean up uploaded files from disk
        uploadedFileRepository.findAll().forEach(file -> {
            try {
                Path filePath = Paths.get(uploadDir, file.getFolder().getPath(), file.getStoredFilename());
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        });

        // Clean up database
        uploadedFileRepository.deleteAll();
        folderRepository.deleteAll();
        courseAssignmentRepository.deleteAll();
        courseRepository.deleteAll();
        
        userRepository.findByEmail("prof.upload@alquds.edu").ifPresent(userRepository::delete);
        userRepository.findByEmail("prof.other@alquds.edu").ifPresent(userRepository::delete);
        userRepository.findByEmail("dean.upload@alquds.edu").ifPresent(userRepository::delete);
    }

    /**
     * Test: Upload single file with authenticated professor (success)
     * Requirement: 2.1, 2.2, 2.8
     */
    @Test
    @Order(1)
    @DisplayName("Professor should successfully upload single file to their own folder")
    void testUploadSingleFileSuccess() throws Exception {
        // Create test file
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "lecture-notes.pdf",
                "application/pdf",
                "Test lecture notes content".getBytes()
        );

        // Upload file
        MvcResult result = mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderId", folder.getId().toString())
                        .param("notes", "Week 1 lecture notes")
                        .with(user(professorUser.getEmail()).roles("PROFESSOR"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Successfully uploaded 1 file(s)"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].originalFilename").value("lecture-notes.pdf"))
                .andExpect(jsonPath("$.data[0].storedFilename").value("lecture-notes.pdf"))
                .andExpect(jsonPath("$.data[0].fileType").value("application/pdf"))
                .andExpect(jsonPath("$.data[0].notes").value("Week 1 lecture notes"))
                .andExpect(jsonPath("$.data[0].uploaderName").exists())
                .andReturn();

        // Verify file exists in database
        assertThat(uploadedFileRepository.count()).isEqualTo(1);

        // Verify physical file exists
        Path uploadedFilePath = Paths.get(uploadDir, folder.getPath(), "lecture-notes.pdf");
        assertThat(Files.exists(uploadedFilePath)).isTrue();
        assertThat(Files.size(uploadedFilePath)).isGreaterThan(0);
    }

    /**
     * Test: Upload multiple files with authenticated professor (success)
     * Requirement: 2.1, 2.2
     */
    @Test
    @Order(2)
    @DisplayName("Professor should successfully upload multiple files to their own folder")
    void testUploadMultipleFilesSuccess() throws Exception {
        // Create test files
        MockMultipartFile file1 = new MockMultipartFile(
                "files[]",
                "document1.pdf",
                "application/pdf",
                "Document 1 content".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "files[]",
                "document2.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "Document 2 content".getBytes()
        );

        MockMultipartFile file3 = new MockMultipartFile(
                "files[]",
                "presentation.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "Presentation content".getBytes()
        );

        // Upload files
        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file1)
                        .file(file2)
                        .file(file3)
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Successfully uploaded 3 file(s)"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].originalFilename").value("document1.pdf"))
                .andExpect(jsonPath("$.data[1].originalFilename").value("document2.docx"))
                .andExpect(jsonPath("$.data[2].originalFilename").value("presentation.pptx"));

        // Verify files exist in database
        assertThat(uploadedFileRepository.count()).isEqualTo(3);

        // Verify physical files exist
        assertThat(Files.exists(Paths.get(uploadDir, folder.getPath(), "document1.pdf"))).isTrue();
        assertThat(Files.exists(Paths.get(uploadDir, folder.getPath(), "document2.docx"))).isTrue();
        assertThat(Files.exists(Paths.get(uploadDir, folder.getPath(), "presentation.pptx"))).isTrue();
    }

    /**
     * Test: Upload file with notes (success)
     * Requirement: 2.2
     */
    @Test
    @Order(3)
    @DisplayName("Professor should successfully upload file with notes")
    void testUploadFileWithNotesSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "exam.pdf",
                "application/pdf",
                "Exam content".getBytes()
        );

        String notes = "Midterm exam - Fall 2024";

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderId", folder.getId().toString())
                        .param("notes", notes)
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].notes").value(notes));

        // Verify notes are saved
        var uploadedFile = uploadedFileRepository.findAll().get(0);
        assertThat(uploadedFile.getNotes()).isEqualTo(notes);
    }

    /**
     * Test: Upload without authentication (403 error due to CSRF)
     * Requirement: 3.1
     */
    @Test
    @Order(4)
    @DisplayName("Upload without authentication should return 403")
    void testUploadWithoutAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "test.pdf",
                "application/pdf",
                "Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderId", folder.getId().toString()))
                .andExpect(status().isForbidden());

        // Verify no file was uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(0);
    }

    /**
     * Test: Upload to another professor's folder (403 error)
     * Requirement: 3.2, 3.3
     */
    @Test
    @Order(5)
    @DisplayName("Professor should not be able to upload to another professor's folder")
    void testUploadToOtherProfessorFolder() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "unauthorized.pdf",
                "application/pdf",
                "Unauthorized content".getBytes()
        );

        // Try to upload as other professor
        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderId", folder.getId().toString())
                        .with(user(otherProfessorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        // Verify no file was uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(0);
    }

    /**
     * Test: Deanship can upload to any folder (success)
     * Requirement: 3.1, 3.4
     */
    @Test
    @Order(6)
    @DisplayName("Deanship should be able to upload to any professor's folder")
    void testDeanshipCanUploadToAnyFolder() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "deanship-document.pdf",
                "application/pdf",
                "Deanship document content".getBytes()
        );

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderId", folder.getId().toString())
                        .param("notes", "Uploaded by deanship")
                        .with(user(deanshipUser.getEmail()).roles("DEANSHIP")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].originalFilename").value("deanship-document.pdf"));

        // Verify file was uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(1);
        assertThat(Files.exists(Paths.get(uploadDir, folder.getPath(), "deanship-document.pdf"))).isTrue();
    }

    /**
     * Test: Upload to non-existent folder (404 error)
     * Requirement: 2.9, 8.1
     */
    @Test
    @Order(7)
    @DisplayName("Upload to non-existent folder should return 404")
    void testUploadToNonExistentFolder() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "test.pdf",
                "application/pdf",
                "Test content".getBytes()
        );

        Long nonExistentFolderId = 99999L;

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderId", nonExistentFolderId.toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        // Verify no file was uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(0);
    }

    /**
     * Test: Upload invalid file type (400 error)
     * Requirement: 9.1, 9.2
     */
    @Test
    @Order(8)
    @DisplayName("Upload invalid file type should return 400")
    void testUploadInvalidFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "malicious.exe",
                "application/x-msdownload",
                "Malicious content".getBytes()
        );

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        // Verify no file was uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(0);
    }

    /**
     * Test: Upload file too large (400 error)
     * Requirement: 9.1, 9.2
     */
    @Test
    @Order(9)
    @DisplayName("Upload file exceeding size limit should return 400")
    void testUploadFileTooLarge() throws Exception {
        // Create a file larger than 50MB (52428800 bytes)
        byte[] largeContent = new byte[52428801]; // 50MB + 1 byte
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "large-file.pdf",
                "application/pdf",
                largeContent
        );

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        // Verify no file was uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(0);
    }

    /**
     * Test: Upload empty file (400 error)
     * Requirement: 9.1
     */
    @Test
    @Order(10)
    @DisplayName("Upload empty file should return 400")
    void testUploadEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File is empty"));

        // Verify no file was uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(0);
    }

    /**
     * Test: Upload file with special characters in filename (sanitization)
     * Requirement: 7.1, 7.2, 7.3
     */
    @Test
    @Order(11)
    @DisplayName("Upload file with special characters should sanitize filename")
    void testUploadFileWithSpecialCharacters() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "test file!@#$%^&*().pdf",
                "application/pdf",
                "Test content".getBytes()
        );

        MvcResult result = mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].originalFilename").value("test file!@#$%^&*().pdf"))
                .andReturn();

        // Verify stored filename is sanitized
        var uploadedFile = uploadedFileRepository.findAll().get(0);
        assertThat(uploadedFile.getStoredFilename()).matches("test_file[_]+\\.pdf");
        assertThat(uploadedFile.getStoredFilename()).doesNotContain("!", "@", "#", "$", "%", "^", "&", "*", "(", ")");
    }

    /**
     * Test: Upload duplicate filename (rename with counter)
     * Requirement: 7.4, 7.5
     */
    @Test
    @Order(12)
    @DisplayName("Upload duplicate filename should rename with counter")
    void testUploadDuplicateFilename() throws Exception {
        // Upload first file
        MockMultipartFile file1 = new MockMultipartFile(
                "files[]",
                "document.pdf",
                "application/pdf",
                "First document".getBytes()
        );

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file1)
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isOk());

        // Upload second file with same name
        MockMultipartFile file2 = new MockMultipartFile(
                "files[]",
                "document.pdf",
                "application/pdf",
                "Second document".getBytes()
        );

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file2)
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].storedFilename").value("document(1).pdf"));

        // Verify both files exist
        assertThat(uploadedFileRepository.count()).isEqualTo(2);
        assertThat(Files.exists(Paths.get(uploadDir, folder.getPath(), "document.pdf"))).isTrue();
        assertThat(Files.exists(Paths.get(uploadDir, folder.getPath(), "document(1).pdf"))).isTrue();
    }

    /**
     * Test: Upload with various allowed file types
     * Requirement: 9.2
     */
    @Test
    @Order(13)
    @DisplayName("Upload should accept all allowed file types")
    void testUploadAllowedFileTypes() throws Exception {
        // Test PDF
        MockMultipartFile pdfFile = new MockMultipartFile("files[]", "test.pdf", "application/pdf", "PDF content".getBytes());
        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(pdfFile)
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isOk());

        // Test DOCX
        MockMultipartFile docxFile = new MockMultipartFile("files[]", "test.docx", 
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "DOCX content".getBytes());
        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(docxFile)
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isOk());

        // Test XLSX
        MockMultipartFile xlsxFile = new MockMultipartFile("files[]", "test.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "XLSX content".getBytes());
        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(xlsxFile)
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isOk());

        // Test JPG
        MockMultipartFile jpgFile = new MockMultipartFile("files[]", "test.jpg", "image/jpeg", "JPG content".getBytes());
        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(jpgFile)
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isOk());

        // Verify all files were uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(4);
    }

    // Helper methods

    private User createUser(String email, Role role, Department department, String professorId) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Test123!"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        user.setDepartment(department);
        if (professorId != null) {
            user.setProfessorId(professorId);
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

    private Folder createFolder(String path, String name, FolderType type, User owner) {
        Folder folder = Folder.builder()
                .path(path)
                .name(name)
                .type(type)
                .owner(owner)
                .build();
        return folderRepository.saveAndFlush(folder);
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
    
    /**
     * Test: Get files by folder ID - success case
     * Requirement: 6.1, 6.2
     */
    @Test
    @Order(14)
    @DisplayName("Get files should return all files in folder for authorized user")
    void testGetFilesByFolder_Success() throws Exception {
        // Upload some test files first
        MockMultipartFile file1 = new MockMultipartFile("files[]", "lecture1.pdf", "application/pdf", "Lecture 1 content".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files[]", "lecture2.pdf", "application/pdf", "Lecture 2 content".getBytes());
        
        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file1)
                        .file(file2)
                        .param("folderId", folder.getId().toString())
                        .param("notes", "Test lectures")
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isOk());
        
        // Now get the files
        MvcResult result = mockMvc.perform(get("/api/professor/files")
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();
        
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("lecture1.pdf");
        assertThat(responseBody).contains("lecture2.pdf");
    }

    /**
     * Test: Get files by folder ID - unauthorized access
     * Requirement: 3.1, 3.2
     */
    @Test
    @Order(15)
    @DisplayName("Get files should return 403 for unauthorized user")
    void testGetFilesByFolder_Unauthorized() throws Exception {
        // Try to get files from another professor's folder
        mockMvc.perform(get("/api/professor/files")
                        .param("folderId", folder.getId().toString())
                        .with(user(otherProfessorUser.getEmail()).roles("PROFESSOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You do not have permission to view files in this folder"));
    }

    /**
     * Test: Get files by folder ID - deanship can access any folder
     * Requirement: 3.1, 3.3
     */
    @Test
    @Order(16)
    @DisplayName("Get files should allow deanship to access any folder")
    void testGetFilesByFolder_DeanshipAccess() throws Exception {
        // Upload a file as professor
        MockMultipartFile file = new MockMultipartFile("files[]", "test.pdf", "application/pdf", "Test content".getBytes());
        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderId", folder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")).with(csrf()))
                .andExpect(status().isOk());
        
        // Deanship should be able to get files from professor's folder
        mockMvc.perform(get("/api/professor/files")
                        .param("folderId", folder.getId().toString())
                        .with(user(deanshipUser.getEmail()).roles("DEANSHIP")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * Test: Get files by folder ID - folder not found
     * Requirement: 9.3
     */
    @Test
    @Order(17)
    @DisplayName("Get files should return 404 for non-existent folder")
    void testGetFilesByFolder_FolderNotFound() throws Exception {
        mockMvc.perform(get("/api/professor/files")
                        .param("folderId", "99999")
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * Test: Get files by folder ID - empty folder
     * Requirement: 6.4
     */
    @Test
    @Order(18)
    @DisplayName("Get files should return empty array for folder with no files")
    void testGetFilesByFolder_EmptyFolder() throws Exception {
        // Create a new folder with no files
        Folder emptyFolder = new Folder();
        emptyFolder.setName("Empty Folder");
        emptyFolder.setPath(folder.getPath() + "/Empty");
        emptyFolder.setType(FolderType.SUBFOLDER);
        emptyFolder.setOwner(professorUser);
        emptyFolder.setParent(folder.getParent());
        emptyFolder = folderRepository.saveAndFlush(emptyFolder);
        
        mockMvc.perform(get("/api/professor/files")
                        .param("folderId", emptyFolder.getId().toString())
                        .with(user(professorUser.getEmail()).roles("PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("Retrieved 0 file(s) from folder"));
    }

    /**
     * Test: Upload to non-existing folder using folderPath (auto-creation)
     * Requirement: Task 34 - Folder auto-creation
     */
    @Test
    @Order(19)
    @DisplayName("Upload with folderPath should auto-create folder and upload file")
    void testUploadWithFolderPath_AutoCreation() throws Exception {
        // Create test file
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "assignment.pdf",
                "application/pdf",
                "Assignment content".getBytes()
        );

        // Construct folder path for a document type folder that doesn't exist
        String folderPath = String.format("/%s/%s/%s/%s/assignments",
                academicYear.getYearCode(),
                semester.getType().name().toLowerCase(),
                professorUser.getProfessorId(),
                course.getCourseCode()
        );

        // Verify folder doesn't exist yet
        assertThat(folderRepository.findByPath(folderPath)).isEmpty();

        // Upload file with folderPath
        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderPath", folderPath)
                        .param("notes", "Assignment 1")
                        .with(user(professorUser.getEmail()).roles("PROFESSOR"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Successfully uploaded 1 file(s)"))
                .andExpect(jsonPath("$.data[0].originalFilename").value("assignment.pdf"));

        // Verify folder was created
        var createdFolder = folderRepository.findByPath(folderPath);
        assertThat(createdFolder).isPresent();
        assertThat(createdFolder.get().getName()).isEqualTo("Assignments");
        assertThat(createdFolder.get().getOwner().getId()).isEqualTo(professorUser.getId());

        // Verify file was uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(1);
        var uploadedFile = uploadedFileRepository.findAll().get(0);
        assertThat(uploadedFile.getFolder().getId()).isEqualTo(createdFolder.get().getId());

        // Verify physical file exists
        Path uploadedFilePath = Paths.get(uploadDir, folderPath, "assignment.pdf");
        assertThat(Files.exists(uploadedFilePath)).isTrue();
    }

    /**
     * Test: Upload without folderId or folderPath (should fail)
     * Requirement: Task 34 - Validation
     */
    @Test
    @Order(20)
    @DisplayName("Upload without folderId or folderPath should return 400")
    void testUploadWithoutFolderIdOrPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "test.pdf",
                "application/pdf",
                "Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .with(user(professorUser.getEmail()).roles("PROFESSOR"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Either folderId or folderPath must be provided"));

        // Verify no file was uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(0);
    }

    /**
     * Test: Upload with invalid folderPath format (should fail)
     * Requirement: Task 34 - Path validation
     */
    @Test
    @Order(21)
    @DisplayName("Upload with invalid folderPath should return 400")
    void testUploadWithInvalidFolderPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "test.pdf",
                "application/pdf",
                "Test content".getBytes()
        );

        // Invalid path format (missing components)
        String invalidPath = "/invalid/path";

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderPath", invalidPath)
                        .with(user(professorUser.getEmail()).roles("PROFESSOR"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        // Verify no file was uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(0);
    }

    /**
     * Test: Upload with folderPath for non-existent academic year (should fail)
     * Requirement: Task 34 - Entity validation
     */
    @Test
    @Order(22)
    @DisplayName("Upload with folderPath for non-existent academic year should return 404")
    void testUploadWithNonExistentAcademicYear() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "test.pdf",
                "application/pdf",
                "Test content".getBytes()
        );

        // Path with non-existent academic year
        String invalidPath = "/2099-2100/first/PROF001/CS101/lecture_notes";

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderPath", invalidPath)
                        .with(user(professorUser.getEmail()).roles("PROFESSOR"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        // Verify no file was uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(0);
    }

    /**
     * Test: Upload with folderPath to another professor's course (should fail)
     * Requirement: Task 34 - Permission validation
     */
    @Test
    @Order(23)
    @DisplayName("Upload with folderPath to another professor's course should return 403")
    void testUploadWithFolderPathToOtherProfessorCourse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "test.pdf",
                "application/pdf",
                "Test content".getBytes()
        );

        // Path for PROF001's course, but uploading as PROF002
        String otherProfessorPath = String.format("/%s/%s/%s/%s/lecture_notes",
                academicYear.getYearCode(),
                semester.getType().name().toLowerCase(),
                professorUser.getProfessorId(),  // PROF001
                course.getCourseCode()
        );

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file)
                        .param("folderPath", otherProfessorPath)
                        .with(user(otherProfessorUser.getEmail()).roles("PROFESSOR"))  // PROF002
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        // Verify no file was uploaded
        assertThat(uploadedFileRepository.count()).isEqualTo(0);
    }

    /**
     * Test: Upload with folderPath - idempotent folder creation
     * Requirement: Task 34 - Idempotent operation
     */
    @Test
    @Order(24)
    @DisplayName("Upload with folderPath should reuse existing folder if already created")
    void testUploadWithFolderPath_IdempotentCreation() throws Exception {
        // Construct folder path
        String folderPath = String.format("/%s/%s/%s/%s/exams",
                academicYear.getYearCode(),
                semester.getType().name().toLowerCase(),
                professorUser.getProfessorId(),
                course.getCourseCode()
        );

        // First upload - creates folder
        MockMultipartFile file1 = new MockMultipartFile(
                "files[]",
                "exam1.pdf",
                "application/pdf",
                "Exam 1 content".getBytes()
        );

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file1)
                        .param("folderPath", folderPath)
                        .with(user(professorUser.getEmail()).roles("PROFESSOR"))
                        .with(csrf()))
                .andExpect(status().isOk());

        // Get folder ID after first upload
        var folder1 = folderRepository.findByPath(folderPath);
        assertThat(folder1).isPresent();
        Long folderId1 = folder1.get().getId();

        // Second upload - should reuse existing folder
        MockMultipartFile file2 = new MockMultipartFile(
                "files[]",
                "exam2.pdf",
                "application/pdf",
                "Exam 2 content".getBytes()
        );

        mockMvc.perform(multipart("/api/professor/files/upload")
                        .file(file2)
                        .param("folderPath", folderPath)
                        .with(user(professorUser.getEmail()).roles("PROFESSOR"))
                        .with(csrf()))
                .andExpect(status().isOk());

        // Verify same folder was used
        var folder2 = folderRepository.findByPath(folderPath);
        assertThat(folder2).isPresent();
        assertThat(folder2.get().getId()).isEqualTo(folderId1);

        // Verify both files are in the same folder
        assertThat(uploadedFileRepository.count()).isEqualTo(2);
        var files = uploadedFileRepository.findAll();
        assertThat(files.get(0).getFolder().getId()).isEqualTo(folderId1);
        assertThat(files.get(1).getFolder().getId()).isEqualTo(folderId1);
    }
}
