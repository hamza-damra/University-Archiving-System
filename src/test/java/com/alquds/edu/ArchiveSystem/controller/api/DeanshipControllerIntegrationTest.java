package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.entity.academic.*;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
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
 * Integration tests for DeanshipController following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Test controller + service + repository layers together
 * - Use @SpringBootTest with MockMvc
 * - Test real database interactions (with @Transactional rollback)
 * - Test security and authorization
 * - Test system-wide access (Deanship can access all departments)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("DeanshipController Integration Tests")
class DeanshipControllerIntegrationTest {

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
    
    private Department testDepartment1;
    private Department testDepartment2;
    private User testDeanship;
    private User testProfessor1;
    private User testProfessor2;
    private AcademicYear testAcademicYear;
    private Semester testSemester;
    
    @BeforeEach
    void setUp() {
        // Clean up test data
        courseRepository.deleteAll();
        semesterRepository.deleteAll();
        academicYearRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        
        // Create test departments
        Department dept1 = TestDataBuilder.createDepartment();
        dept1.setName("Computer Science");
        dept1.setShortcut("cs");
        testDepartment1 = departmentRepository.save(dept1);
        
        Department dept2 = TestDataBuilder.createDepartment();
        dept2.setName("Mathematics");
        dept2.setShortcut("math");
        testDepartment2 = departmentRepository.save(dept2);
        
        // Create test deanship user
        testDeanship = TestDataBuilder.createUser();
        testDeanship.setEmail("dean@deanship.alquds.edu");
        testDeanship.setFirstName("Dean");
        testDeanship.setLastName("User");
        testDeanship.setRole(Role.ROLE_DEANSHIP);
        testDeanship.setDepartment(null); // Deanship doesn't belong to a department
        testDeanship.setIsActive(true);
        testDeanship = userRepository.save(testDeanship);
        
        // Create test professors in different departments
        testProfessor1 = TestDataBuilder.createProfessorUser();
        testProfessor1.setEmail("prof1.cs@staff.alquds.edu");
        testProfessor1.setFirstName("Professor");
        testProfessor1.setLastName("One");
        testProfessor1.setDepartment(testDepartment1);
        testProfessor1.setIsActive(true);
        testProfessor1 = userRepository.save(testProfessor1);
        
        testProfessor2 = TestDataBuilder.createProfessorUser();
        testProfessor2.setEmail("prof2.math@staff.alquds.edu");
        testProfessor2.setFirstName("Professor");
        testProfessor2.setLastName("Two");
        testProfessor2.setDepartment(testDepartment2);
        testProfessor2.setIsActive(true);
        testProfessor2 = userRepository.save(testProfessor2);
        
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
        
        // Create test courses in different departments
        Course course1 = TestDataBuilder.createCourse();
        course1.setCourseCode("CS101");
        course1.setCourseName("Introduction to Computer Science");
        course1.setDepartment(testDepartment1);
        course1.setIsActive(true);
        courseRepository.save(course1);
        
        Course course2 = TestDataBuilder.createCourse();
        course2.setCourseCode("MATH101");
        course2.setCourseName("Introduction to Mathematics");
        course2.setDepartment(testDepartment2);
        course2.setIsActive(true);
        courseRepository.save(course2);
    }
    
    // ==================== Get Departments Tests ====================
    
    @Test
    @DisplayName("Should retrieve all departments successfully")
    @WithMockUser(username = "dean@deanship.alquds.edu", roles = "DEANSHIP")
    void shouldRetrieveAllDepartmentsSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/deanship/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Departments retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.name == 'Computer Science')]").exists())
                .andExpect(jsonPath("$.data[?(@.name == 'Mathematics')]").exists());
    }
    
    // ==================== Get Professors Tests ====================
    
    @Test
    @DisplayName("Should retrieve all professors successfully")
    @WithMockUser(username = "dean@deanship.alquds.edu", roles = "DEANSHIP")
    void shouldRetrieveAllProfessorsSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/deanship/professors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Professors retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.email == 'prof1.cs@staff.alquds.edu')]").exists())
                .andExpect(jsonPath("$.data[?(@.email == 'prof2.math@staff.alquds.edu')]").exists());
    }
    
    @Test
    @DisplayName("Should filter professors by department successfully")
    @WithMockUser(username = "dean@deanship.alquds.edu", roles = "DEANSHIP")
    void shouldFilterProfessorsByDepartmentSuccessfully() throws Exception {
        // Act & Assert: Filter by department1
        mockMvc.perform(get("/api/deanship/professors")
                        .param("departmentId", String.valueOf(testDepartment1.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].email").value("prof1.cs@staff.alquds.edu"));
    }
    
    // ==================== Get Courses Tests ====================
    
    @Test
    @DisplayName("Should retrieve all courses successfully")
    @WithMockUser(username = "dean@deanship.alquds.edu", roles = "DEANSHIP")
    void shouldRetrieveAllCoursesSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/deanship/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Courses retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.courseCode == 'CS101')]").exists())
                .andExpect(jsonPath("$.data[?(@.courseCode == 'MATH101')]").exists());
    }
    
    @Test
    @DisplayName("Should filter courses by department successfully")
    @WithMockUser(username = "dean@deanship.alquds.edu", roles = "DEANSHIP")
    void shouldFilterCoursesByDepartmentSuccessfully() throws Exception {
        // Act & Assert: Filter by department1
        mockMvc.perform(get("/api/deanship/courses")
                        .param("departmentId", String.valueOf(testDepartment1.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].courseCode").value("CS101"));
    }
    
    // ==================== Dashboard Statistics Tests ====================
    
    @Test
    @DisplayName("Should retrieve dashboard statistics successfully")
    @WithMockUser(username = "dean@deanship.alquds.edu", roles = "DEANSHIP")
    void shouldRetrieveDashboardStatisticsSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/deanship/dashboard/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Dashboard statistics retrieved successfully"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.totalSubmissions").exists())
                .andExpect(jsonPath("$.data.totalCourses").exists())
                .andExpect(jsonPath("$.data.totalProfessors").exists());
    }
    
    @Test
    @DisplayName("Should filter dashboard statistics by semester successfully")
    @WithMockUser(username = "dean@deanship.alquds.edu", roles = "DEANSHIP")
    void shouldFilterDashboardStatisticsBySemesterSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/deanship/dashboard/statistics")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }
    
    // ==================== Dashboard Chart Data Tests ====================
    
    @Test
    @DisplayName("Should retrieve department distribution chart data successfully")
    @WithMockUser(username = "dean@deanship.alquds.edu", roles = "DEANSHIP")
    void shouldRetrieveDepartmentDistributionChartDataSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/deanship/dashboard/charts/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Department distribution chart data retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    @DisplayName("Should filter department distribution by semester successfully")
    @WithMockUser(username = "dean@deanship.alquds.edu", roles = "DEANSHIP")
    void shouldFilterDepartmentDistributionBySemesterSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/deanship/dashboard/charts/departments")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    @DisplayName("Should retrieve status distribution chart data successfully")
    @WithMockUser(username = "dean@deanship.alquds.edu", roles = "DEANSHIP")
    void shouldRetrieveStatusDistributionChartDataSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/deanship/dashboard/charts/status-distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Status distribution chart data retrieved successfully"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.pending").exists())
                .andExpect(jsonPath("$.data.uploaded").exists())
                .andExpect(jsonPath("$.data.overdue").exists())
                .andExpect(jsonPath("$.data.total").exists());
    }
    
    @Test
    @DisplayName("Should filter status distribution by semester successfully")
    @WithMockUser(username = "dean@deanship.alquds.edu", roles = "DEANSHIP")
    void shouldFilterStatusDistributionBySemesterSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/deanship/dashboard/charts/status-distribution")
                        .param("semesterId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }
    
    // ==================== Security Tests ====================
    
    @Test
    @DisplayName("Should return 403 when unauthorized user tries to access deanship endpoints")
    @WithMockUser(username = "prof1.cs@staff.alquds.edu", roles = "PROFESSOR")
    void shouldReturn403WhenUnauthorizedUserAccessesDeanshipEndpoints() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/deanship/departments"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("Should return 403 when unauthenticated user tries to access deanship endpoints")
    void shouldReturn403WhenUnauthenticatedUserAccessesDeanshipEndpoints() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/deanship/departments"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("Should allow ADMIN role to access deanship endpoints")
    @WithMockUser(username = "admin@admin.alquds.edu", roles = "ADMIN")
    void shouldAllowAdminRoleToAccessDeanshipEndpoints() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/deanship/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
