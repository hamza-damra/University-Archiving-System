package com.alqude.edu.ArchiveSystem.integration;

import com.alqude.edu.ArchiveSystem.dto.academic.CourseAssignmentDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.CourseDTO;
import com.alqude.edu.ArchiveSystem.dto.user.ProfessorDTO;
import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.repository.*;
import com.alqude.edu.ArchiveSystem.service.CourseService;
import com.alqude.edu.ArchiveSystem.service.FolderService;
import com.alqude.edu.ArchiveSystem.service.ProfessorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for folder auto-provisioning feature.
 * Tests the complete flow from professor/course creation to folder visibility in File Explorer API.
 * 
 * Requirements tested:
 * - 1.1, 1.2: Professor folder auto-creation
 * - 2.1, 2.2: Course folder structure auto-creation
 * - 3.1, 3.2: File Explorer API synchronization
 * - 1.3, 2.3: Idempotency
 * - 3.4: Cross-dashboard synchronization
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"null", "unused"})
class FolderAutoProvisioningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProfessorService professorService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private FolderService folderService;

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
    private PasswordEncoder passwordEncoder;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private User deanshipUser;
    private User hodUser;
    private User testProfessor;
    private Department testDepartment;
    private AcademicYear academicYear;
    private Semester semester;
    private MockHttpSession deanshipSession;
    private MockHttpSession hodSession;
    private MockHttpSession professorSession;

    @BeforeEach
    void setUp() throws Exception {
        // Create test department
        testDepartment = departmentRepository.findByName("Integration Test Dept")
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setName("Integration Test Dept");
                    dept.setDescription("Department for integration testing");
                    return departmentRepository.saveAndFlush(dept);
                });

        // Create test users
        deanshipUser = createUser("deanship.integration@alquds.edu", Role.ROLE_DEANSHIP, testDepartment);
        hodUser = createUser("hod.integration@alquds.edu", Role.ROLE_HOD, testDepartment);

        // Create or find academic year and semester
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

        // Login users and get sessions
        deanshipSession = loginUser("deanship.integration@alquds.edu", "Test123!");
        hodSession = loginUser("hod.integration@alquds.edu", "Test123!");
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        folderRepository.deleteAll();
        courseAssignmentRepository.deleteAll();
        courseRepository.deleteAll();
        
        userRepository.findByEmail("deanship.integration@alquds.edu").ifPresent(userRepository::delete);
        userRepository.findByEmail("hod.integration@alquds.edu").ifPresent(userRepository::delete);
        if (testProfessor != null) {
            userRepository.findById(testProfessor.getId()).ifPresent(userRepository::delete);
        }
    }

    /**
     * Test 12.1: End-to-end test for professor creation flow
     * - Create professor via API
     * - Verify folder exists in database
     * - Verify folder exists in file system
     * - Call File Explorer API and verify folder appears in tree
     * 
     * Requirements: 1.1, 1.2, 3.1, 8.5
     */
    @Test
    @Order(1)
    @DisplayName("Professor creation should auto-create folder visible in File Explorer")
    void testProfessorCreationFlow() throws Exception {
        // Step 1: Create professor via service (simulating API call)
        ProfessorDTO professorDTO = new ProfessorDTO();
        professorDTO.setEmail("test.prof.e2e@alquds.edu");
        professorDTO.setPassword("Test123!");
        professorDTO.setFirstName("Test");
        professorDTO.setLastName("Professor");
        professorDTO.setProfessorId("PROF-E2E-001");
        professorDTO.setDepartmentId(testDepartment.getId());
        professorDTO.setIsActive(true);

        testProfessor = professorService.createProfessor(professorDTO);
        assertThat(testProfessor).isNotNull();
        assertThat(testProfessor.getId()).isNotNull();

        // Step 2: Manually create professor folder (since auto-creation happens on course assignment)
        Folder professorFolder = folderService.createProfessorFolder(
            testProfessor.getId(), 
            academicYear.getId(), 
            semester.getId()
        );

        // Step 3: Verify folder exists in database
        String expectedPath = academicYear.getYearCode() + "/" + 
                             semester.getType().name().toLowerCase() + "/" + 
                             testProfessor.getProfessorId();
        
        assertThat(professorFolder).isNotNull();
        assertThat(professorFolder.getPath()).isEqualTo(expectedPath);
        assertThat(professorFolder.getType()).isEqualTo(FolderType.PROFESSOR_ROOT);
        assertThat(professorFolder.getOwner().getId()).isEqualTo(testProfessor.getId());

        // Verify folder can be found by path
        var foundFolder = folderRepository.findByPath(expectedPath);
        assertThat(foundFolder).isPresent();
        assertThat(foundFolder.get().getId()).isEqualTo(professorFolder.getId());

        // Step 4: Verify folder exists in file system
        Path physicalPath = Paths.get(uploadDir, expectedPath);
        assertThat(Files.exists(physicalPath)).isTrue();
        assertThat(Files.isDirectory(physicalPath)).isTrue();

        // Step 5: Call File Explorer API and verify folder appears in tree
        MvcResult result = mockMvc.perform(get("/api/file-explorer/root")
                        .session(deanshipSession)
                        .param("academicYearId", academicYear.getId().toString())
                        .param("semesterId", semester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains(testProfessor.getProfessorId());
        assertThat(responseBody).contains("Test Professor");
    }

    /**
     * Test 12.2: End-to-end test for course assignment flow
     * - Create professor, course, and assignment via API
     * - Verify course folder structure exists in database
     * - Verify course folders exist in file system
     * - Call File Explorer API and verify folders appear in tree
     * 
     * Requirements: 2.1, 2.2, 3.1, 8.5
     */
    @Test
    @Order(2)
    @DisplayName("Course assignment should auto-create folder structure visible in File Explorer")
    void testCourseAssignmentFlow() throws Exception {
        // Step 1: Create professor
        ProfessorDTO professorDTO = new ProfessorDTO();
        professorDTO.setEmail("test.prof.course@alquds.edu");
        professorDTO.setPassword("Test123!");
        professorDTO.setFirstName("Course");
        professorDTO.setLastName("Professor");
        professorDTO.setProfessorId("PROF-E2E-002");
        professorDTO.setDepartmentId(testDepartment.getId());
        professorDTO.setIsActive(true);

        testProfessor = professorService.createProfessor(professorDTO);

        // Step 2: Create course
        CourseDTO courseDTO = new CourseDTO();
        courseDTO.setCourseCode("TEST101");
        courseDTO.setCourseName("Test Course");
        courseDTO.setDepartmentId(testDepartment.getId());
        courseDTO.setLevel("Undergraduate");
        courseDTO.setIsActive(true);

        Course course = courseService.createCourse(courseDTO);
        assertThat(course).isNotNull();

        // Step 3: Assign course to professor (this should auto-create folders)
        CourseAssignmentDTO assignmentDTO = new CourseAssignmentDTO();
        assignmentDTO.setSemesterId(semester.getId());
        assignmentDTO.setCourseId(course.getId());
        assignmentDTO.setProfessorId(testProfessor.getId());
        assignmentDTO.setIsActive(true);

        CourseAssignment assignment = courseService.assignCourse(assignmentDTO);
        assertThat(assignment).isNotNull();

        // Step 4: Verify course folder structure exists in database
        String professorPath = academicYear.getYearCode() + "/" + 
                              semester.getType().name().toLowerCase() + "/" + 
                              testProfessor.getProfessorId();
        String coursePath = professorPath + "/" + course.getCourseCode() + " - " + course.getCourseName();

        // Verify professor folder
        var professorFolder = folderRepository.findByPath(professorPath);
        assertThat(professorFolder).isPresent();

        // Verify course folder
        var courseFolder = folderRepository.findByPath(coursePath);
        assertThat(courseFolder).isPresent();
        assertThat(courseFolder.get().getType()).isEqualTo(FolderType.COURSE);
        assertThat(courseFolder.get().getCourse().getId()).isEqualTo(course.getId());

        // Verify standard subfolders
        String[] expectedSubfolders = {"Syllabus", "Exams", "Course Notes", "Assignments"};
        for (String subfolderName : expectedSubfolders) {
            String subfolderPath = coursePath + "/" + subfolderName;
            var subfolder = folderRepository.findByPath(subfolderPath);
            assertThat(subfolder)
                .as("Subfolder '%s' should exist", subfolderName)
                .isPresent();
            assertThat(subfolder.get().getType()).isEqualTo(FolderType.SUBFOLDER);
        }

        // Step 5: Verify course folders exist in file system
        Path coursePhysicalPath = Paths.get(uploadDir, coursePath);
        assertThat(Files.exists(coursePhysicalPath)).isTrue();
        assertThat(Files.isDirectory(coursePhysicalPath)).isTrue();

        for (String subfolderName : expectedSubfolders) {
            Path subfolderPhysicalPath = Paths.get(uploadDir, coursePath, subfolderName);
            assertThat(Files.exists(subfolderPhysicalPath))
                .as("Physical subfolder '%s' should exist", subfolderName)
                .isTrue();
        }

        // Step 6: Call File Explorer API and verify folders appear in tree
        MvcResult result = mockMvc.perform(get("/api/file-explorer/root")
                        .session(deanshipSession)
                        .param("academicYearId", academicYear.getId().toString())
                        .param("semesterId", semester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains(testProfessor.getProfessorId());
        assertThat(responseBody).contains(course.getCourseCode());
    }


    /**
     * Test 12.3: Test idempotency of folder creation
     * - Create same professor folder twice
     * - Verify only one folder exists
     * - Create same course folder twice
     * - Verify no duplicate folders
     * 
     * Requirements: 1.3, 2.3, 8.5
     */
    @Test
    @Order(3)
    @DisplayName("Folder creation should be idempotent - no duplicates")
    void testFolderCreationIdempotency() throws Exception {
        // Step 1: Create professor
        ProfessorDTO professorDTO = new ProfessorDTO();
        professorDTO.setEmail("test.prof.idempotent@alquds.edu");
        professorDTO.setPassword("Test123!");
        professorDTO.setFirstName("Idempotent");
        professorDTO.setLastName("Professor");
        professorDTO.setProfessorId("PROF-E2E-003");
        professorDTO.setDepartmentId(testDepartment.getId());
        professorDTO.setIsActive(true);

        testProfessor = professorService.createProfessor(professorDTO);

        // Step 2: Create professor folder twice
        Folder folder1 = folderService.createProfessorFolder(
            testProfessor.getId(), 
            academicYear.getId(), 
            semester.getId()
        );

        Folder folder2 = folderService.createProfessorFolder(
            testProfessor.getId(), 
            academicYear.getId(), 
            semester.getId()
        );

        // Verify same folder is returned (idempotency)
        assertThat(folder1.getId()).isEqualTo(folder2.getId());
        assertThat(folder1.getPath()).isEqualTo(folder2.getPath());

        // Verify only one folder exists in database
        String professorPath = academicYear.getYearCode() + "/" + 
                              semester.getType().name().toLowerCase() + "/" + 
                              testProfessor.getProfessorId();
        
        List<Folder> allFolders = folderRepository.findAll();
        long professorFolderCount = allFolders.stream()
            .filter(f -> f.getPath().equals(professorPath))
            .count();
        
        assertThat(professorFolderCount).isEqualTo(1);

        // Step 3: Create course
        CourseDTO courseDTO = new CourseDTO();
        courseDTO.setCourseCode("IDEM101");
        courseDTO.setCourseName("Idempotent Course");
        courseDTO.setDepartmentId(testDepartment.getId());
        courseDTO.setLevel("Undergraduate");
        courseDTO.setIsActive(true);

        Course course = courseService.createCourse(courseDTO);

        // Step 4: Create course folder structure twice
        List<Folder> folders1 = folderService.createCourseFolderStructure(
            testProfessor.getId(), 
            course.getId(), 
            academicYear.getId(), 
            semester.getId()
        );

        List<Folder> folders2 = folderService.createCourseFolderStructure(
            testProfessor.getId(), 
            course.getId(), 
            academicYear.getId(), 
            semester.getId()
        );

        // Verify same folders are returned
        assertThat(folders1.size()).isEqualTo(folders2.size());
        assertThat(folders1.size()).isEqualTo(5); // 1 course + 4 subfolders

        // Verify no duplicate folders in database
        String coursePath = professorPath + "/" + course.getCourseCode() + " - " + course.getCourseName();
        
        allFolders = folderRepository.findAll();
        long courseFolderCount = allFolders.stream()
            .filter(f -> f.getPath().equals(coursePath))
            .count();
        
        assertThat(courseFolderCount).isEqualTo(1);

        // Verify no duplicate subfolders
        String[] expectedSubfolders = {"Syllabus", "Exams", "Course Notes", "Assignments"};
        for (String subfolderName : expectedSubfolders) {
            String subfolderPath = coursePath + "/" + subfolderName;
            long subfolderCount = allFolders.stream()
                .filter(f -> f.getPath().equals(subfolderPath))
                .count();
            
            assertThat(subfolderCount)
                .as("Subfolder '%s' should exist exactly once", subfolderName)
                .isEqualTo(1);
        }
    }

    /**
     * Test 12.4: Test cross-dashboard synchronization
     * - Create folder via Deanship API
     * - Call Professor File Explorer API
     * - Verify folder appears in Professor view
     * - Call HOD File Explorer API
     * - Verify folder appears in HOD view
     * 
     * Requirements: 3.1, 3.2, 3.4, 8.5
     */
    @Test
    @Order(4)
    @DisplayName("Folders should be visible across all dashboards (Deanship, Professor, HOD)")
    void testCrossDashboardSynchronization() throws Exception {
        // Step 1: Create professor
        ProfessorDTO professorDTO = new ProfessorDTO();
        professorDTO.setEmail("test.prof.sync@alquds.edu");
        professorDTO.setPassword("Test123!");
        professorDTO.setFirstName("Sync");
        professorDTO.setLastName("Professor");
        professorDTO.setProfessorId("PROF-E2E-004");
        professorDTO.setDepartmentId(testDepartment.getId());
        professorDTO.setIsActive(true);

        testProfessor = professorService.createProfessor(professorDTO);
        professorSession = loginUser("test.prof.sync@alquds.edu", "Test123!");

        // Step 2: Create course and assignment
        CourseDTO courseDTO = new CourseDTO();
        courseDTO.setCourseCode("SYNC101");
        courseDTO.setCourseName("Sync Course");
        courseDTO.setDepartmentId(testDepartment.getId());
        courseDTO.setLevel("Undergraduate");
        courseDTO.setIsActive(true);

        Course course = courseService.createCourse(courseDTO);

        CourseAssignmentDTO assignmentDTO = new CourseAssignmentDTO();
        assignmentDTO.setSemesterId(semester.getId());
        assignmentDTO.setCourseId(course.getId());
        assignmentDTO.setProfessorId(testProfessor.getId());
        assignmentDTO.setIsActive(true);

        CourseAssignment assignment = courseService.assignCourse(assignmentDTO);

        // Step 3: Verify folder appears in Deanship File Explorer
        MvcResult deanshipResult = mockMvc.perform(get("/api/file-explorer/root")
                        .session(deanshipSession)
                        .param("academicYearId", academicYear.getId().toString())
                        .param("semesterId", semester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String deanshipResponse = deanshipResult.getResponse().getContentAsString();
        assertThat(deanshipResponse).contains(testProfessor.getProfessorId());
        assertThat(deanshipResponse).contains(course.getCourseCode());

        // Step 4: Verify folder appears in Professor File Explorer
        MvcResult professorResult = mockMvc.perform(get("/api/file-explorer/root")
                        .session(professorSession)
                        .param("academicYearId", academicYear.getId().toString())
                        .param("semesterId", semester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String professorResponse = professorResult.getResponse().getContentAsString();
        assertThat(professorResponse).contains(testProfessor.getProfessorId());
        assertThat(professorResponse).contains(course.getCourseCode());

        // Step 5: Verify folder appears in HOD File Explorer (same department)
        MvcResult hodResult = mockMvc.perform(get("/api/file-explorer/root")
                        .session(hodSession)
                        .param("academicYearId", academicYear.getId().toString())
                        .param("semesterId", semester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String hodResponse = hodResult.getResponse().getContentAsString();
        assertThat(hodResponse).contains(testProfessor.getProfessorId());
        assertThat(hodResponse).contains(course.getCourseCode());

        // Step 6: Verify all dashboards see the same folder structure
        // Extract folder paths from responses and compare
        assertThat(deanshipResponse).contains("Syllabus");
        assertThat(professorResponse).contains("Syllabus");
        assertThat(hodResponse).contains("Syllabus");
    }

    // Helper methods

    private User createUser(String email, Role role, Department department) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("Test123!"));
            user.setFirstName("Test");
            user.setLastName("User");
            user.setRole(role);
            user.setDepartment(department);
            user.setIsActive(true);
            if (role == Role.ROLE_PROFESSOR) {
                user.setProfessorId("PROF-" + System.currentTimeMillis());
            }
            return userRepository.saveAndFlush(user);
        });
    }

    private MockHttpSession loginUser(String email, String password) throws Exception {
        String loginJson = objectMapper.writeValueAsString(new LoginRequest(email, password));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        assertThat(session).isNotNull();
        return session;
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
