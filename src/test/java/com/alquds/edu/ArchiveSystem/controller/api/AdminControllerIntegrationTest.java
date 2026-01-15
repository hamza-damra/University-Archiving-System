package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.entity.academic.SemesterType;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.submission.RequiredDocumentType;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.repository.academic.AcademicYearRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.DepartmentRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.SemesterRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.RequiredDocumentTypeRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminController following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - 15-20% integration tests
 * - Test controller + service + repository layers together
 * - Use @SpringBootTest with MockMvc
 * - Test real database interactions (with @Transactional rollback)
 * - Test security and authorization
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminController Integration Tests")
@SuppressWarnings("null")
class AdminControllerIntegrationTest {

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
    
    private Department testDepartment;
    
    @BeforeEach
    void setUp() {
        // Clean up test data
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        
        // Create test department with valid shortcut (lowercase required)
        testDepartment = TestDataBuilder.createDepartment();
        testDepartment.setName("Computer Science");
        testDepartment.setShortcut("cs"); // Must be lowercase
        testDepartment = departmentRepository.save(testDepartment);
    }
    
    // ==================== Create User Tests ====================
    
    @Test
    @DisplayName("Should create user successfully via API")
    @WithMockUser(roles = "ADMIN")
    void shouldCreateUserSuccessfullyViaApi() throws Exception {
        // Arrange
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setEmail("newprofessor@staff.alquds.edu");
        request.setPassword("TestPass123!");
        request.setFirstName("New");
        request.setLastName("Professor");
        request.setRole(Role.ROLE_PROFESSOR);
        request.setDepartmentId(testDepartment.getId());
        
        // Act & Assert
        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.email").value(request.getEmail()))
                .andExpect(jsonPath("$.data.firstName").value(request.getFirstName()))
                .andExpect(jsonPath("$.data.role").value("ROLE_PROFESSOR"));
        
        // Verify user was saved in database
        assertThat(userRepository.findByEmail(request.getEmail())).isPresent();
    }
    
    @Test
    @DisplayName("Should return 400 when creating user with duplicate email")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400WhenEmailExists() throws Exception {
        // Arrange
        User existingUser = TestDataBuilder.createProfessorUser();
        existingUser.setEmail("existing@staff.alquds.edu");
        existingUser.setDepartment(testDepartment);
        userRepository.save(existingUser);
        
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setEmail("existing@staff.alquds.edu");
        request.setDepartmentId(testDepartment.getId());
        
        // Act & Assert
        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    @DisplayName("Should return 403 when non-admin tries to create user")
    @WithMockUser(roles = "PROFESSOR")
    void shouldReturn403WhenNonAdminCreatesUser() throws Exception {
        // Arrange
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setDepartmentId(testDepartment.getId());
        
        // Act & Assert
        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("Should return 403 when unauthenticated user tries to create user")
    void shouldReturn403WhenUnauthenticated() throws Exception {
        // Arrange
        UserCreateRequest request = TestDataBuilder.createUserCreateRequest();
        request.setDepartmentId(testDepartment.getId());
        
        // Act & Assert
        // Spring Security returns 403 (Forbidden) for unauthenticated requests to protected endpoints
        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
    
    // ==================== Get Users Tests ====================
    
    @Test
    @DisplayName("Should retrieve all users successfully")
    @WithMockUser(roles = "ADMIN")
    void shouldRetrieveAllUsersSuccessfully() throws Exception {
        // Arrange
        User user1 = TestDataBuilder.createProfessorUser();
        user1.setEmail("user1@staff.alquds.edu");
        user1.setDepartment(testDepartment);
        User user2 = TestDataBuilder.createProfessorUser();
        user2.setEmail("user2@staff.alquds.edu");
        user2.setDepartment(testDepartment);
        userRepository.save(user1);
        userRepository.save(user2);
        
        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }
    
    @Test
    @DisplayName("Should filter users by role")
    @WithMockUser(roles = "ADMIN")
    void shouldFilterUsersByRole() throws Exception {
        // Arrange
        User professor = TestDataBuilder.createProfessorUser();
        professor.setEmail("prof@staff.alquds.edu");
        professor.setDepartment(testDepartment);
        User hod = TestDataBuilder.createHodUser();
        hod.setEmail("hod@hod.alquds.edu");
        hod.setDepartment(testDepartment);
        userRepository.save(professor);
        userRepository.save(hod);
        
        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .param("role", "ROLE_PROFESSOR")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].role").value("ROLE_PROFESSOR"));
    }
    
    // ==================== Get User By ID Tests ====================
    
    @Test
    @DisplayName("Should retrieve user by ID successfully")
    @WithMockUser(roles = "ADMIN")
    void shouldRetrieveUserByIdSuccessfully() throws Exception {
        // Arrange
        User user = TestDataBuilder.createProfessorUser();
        user.setEmail("test@staff.alquds.edu");
        user.setDepartment(testDepartment);
        user = userRepository.save(user);
        
        // Act & Assert
        mockMvc.perform(get("/api/admin/users/{id}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.email").value(user.getEmail()));
    }
    
    @Test
    @DisplayName("Should return 404 when user not found")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenUserNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== Reports Tests ====================

    @Test
    @DisplayName("Should return system-wide report overview for a semester")
    @WithMockUser(username = "admin@admin.alquds.edu", roles = "ADMIN")
    void shouldReturnSystemWideReportOverviewForSemester() throws Exception {
        // Arrange: authenticated admin user must exist for getCurrentUser()
        User admin = TestDataBuilder.createAdminUser();
        admin.setEmail("admin@admin.alquds.edu");
        userRepository.save(admin);

        Department mathDept = TestDataBuilder.createDepartment();
        mathDept.setName("Mathematics");
        mathDept.setShortcut("math");
        mathDept = departmentRepository.save(mathDept);

        AcademicYear year = TestDataBuilder.createAcademicYear("2024-2025");
        year = academicYearRepository.save(year);

        Semester semester = TestDataBuilder.createSemester(year, SemesterType.FIRST);
        semester = semesterRepository.save(semester);

        User csProfessor = TestDataBuilder.createProfessorUser();
        csProfessor.setEmail("prof.cs@staff.alquds.edu");
        csProfessor.setDepartment(testDepartment);
        csProfessor = userRepository.save(csProfessor);

        User mathProfessor = TestDataBuilder.createProfessorUser();
        mathProfessor.setEmail("prof.math@staff.alquds.edu");
        mathProfessor.setDepartment(mathDept);
        mathProfessor = userRepository.save(mathProfessor);

        Course csCourse = TestDataBuilder.createCourse();
        csCourse.setCourseCode("CS101");
        csCourse.setDepartment(testDepartment);
        csCourse = courseRepository.save(csCourse);

        Course mathCourse = TestDataBuilder.createCourse();
        mathCourse.setCourseCode("MATH101");
        mathCourse.setCourseName("Calculus I");
        mathCourse.setDepartment(mathDept);
        mathCourse = courseRepository.save(mathCourse);

        CourseAssignment csAssignment = TestDataBuilder.createCourseAssignment();
        csAssignment.setSemester(semester);
        csAssignment.setCourse(csCourse);
        csAssignment.setProfessor(csProfessor);
        csAssignment = courseAssignmentRepository.save(csAssignment);

        CourseAssignment mathAssignment = TestDataBuilder.createCourseAssignment();
        mathAssignment.setSemester(semester);
        mathAssignment.setCourse(mathCourse);
        mathAssignment.setProfessor(mathProfessor);
        mathAssignment = courseAssignmentRepository.save(mathAssignment);

        RequiredDocumentType csSyllabus = new RequiredDocumentType();
        csSyllabus.setCourse(csCourse);
        csSyllabus.setSemester(semester);
        csSyllabus.setDocumentType(DocumentTypeEnum.SYLLABUS);
        csSyllabus.setDeadline(LocalDateTime.now().minusDays(1)); // overdue if missing
        csSyllabus.setAllowedFileExtensions(List.of("pdf"));
        requiredDocumentTypeRepository.save(csSyllabus);

        RequiredDocumentType mathSyllabus = new RequiredDocumentType();
        mathSyllabus.setCourse(mathCourse);
        mathSyllabus.setSemester(semester);
        mathSyllabus.setDocumentType(DocumentTypeEnum.SYLLABUS);
        mathSyllabus.setDeadline(LocalDateTime.now().minusDays(1)); // overdue if missing
        mathSyllabus.setAllowedFileExtensions(List.of("pdf"));
        requiredDocumentTypeRepository.save(mathSyllabus);

        // Create one uploaded submission for CS syllabus so report has both submitted + missing
        DocumentSubmission submission = new DocumentSubmission();
        submission.setCourseAssignment(csAssignment);
        submission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        submission.setProfessor(csProfessor);
        submission.setSubmittedAt(LocalDateTime.now().minusDays(2));
        submission.setStatus(SubmissionStatus.UPLOADED);
        submission.setFileCount(1);
        submission.setTotalFileSize(1024L);
        documentSubmissionRepository.save(submission);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reports/overview")
                        .param("semesterId", String.valueOf(semester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.semesterId").value(semester.getId()))
                .andExpect(jsonPath("$.data.overallStatistics").exists())
                .andExpect(jsonPath("$.data.departmentSummaries").isArray())
                .andExpect(jsonPath("$.data.departmentSummaries.length()").value(2));
    }

    @Test
    @DisplayName("Should filter report overview by departmentId for admin")
    @WithMockUser(username = "admin@admin.alquds.edu", roles = "ADMIN")
    void shouldFilterReportOverviewByDepartmentId() throws Exception {
        // Arrange: authenticated admin user must exist for getCurrentUser()
        User admin = TestDataBuilder.createAdminUser();
        admin.setEmail("admin@admin.alquds.edu");
        userRepository.save(admin);

        Department mathDept = TestDataBuilder.createDepartment();
        mathDept.setName("Mathematics");
        mathDept.setShortcut("math");
        mathDept = departmentRepository.save(mathDept);

        AcademicYear year = TestDataBuilder.createAcademicYear("2024-2025");
        year = academicYearRepository.save(year);

        Semester semester = TestDataBuilder.createSemester(year, SemesterType.FIRST);
        semester = semesterRepository.save(semester);

        User mathProfessor = TestDataBuilder.createProfessorUser();
        mathProfessor.setEmail("prof.math@staff.alquds.edu");
        mathProfessor.setDepartment(mathDept);
        mathProfessor = userRepository.save(mathProfessor);

        Course mathCourse = TestDataBuilder.createCourse();
        mathCourse.setCourseCode("MATH101");
        mathCourse.setCourseName("Calculus I");
        mathCourse.setDepartment(mathDept);
        mathCourse = courseRepository.save(mathCourse);

        CourseAssignment mathAssignment = TestDataBuilder.createCourseAssignment();
        mathAssignment.setSemester(semester);
        mathAssignment.setCourse(mathCourse);
        mathAssignment.setProfessor(mathProfessor);
        mathAssignment = courseAssignmentRepository.save(mathAssignment);

        RequiredDocumentType mathSyllabus = new RequiredDocumentType();
        mathSyllabus.setCourse(mathCourse);
        mathSyllabus.setSemester(semester);
        mathSyllabus.setDocumentType(DocumentTypeEnum.SYLLABUS);
        mathSyllabus.setDeadline(LocalDateTime.now().minusDays(1));
        mathSyllabus.setAllowedFileExtensions(List.of("pdf"));
        requiredDocumentTypeRepository.save(mathSyllabus);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reports/overview")
                        .param("semesterId", String.valueOf(semester.getId()))
                        .param("departmentId", String.valueOf(mathDept.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.departmentSummaries").isArray())
                .andExpect(jsonPath("$.data.departmentSummaries.length()").value(1))
                .andExpect(jsonPath("$.data.departmentSummaries[0].departmentId").value(mathDept.getId()))
                .andExpect(jsonPath("$.data.departmentSummaries[0].departmentName").value("Mathematics"));
    }
}
