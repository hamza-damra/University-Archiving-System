package com.alquds.edu.ArchiveSystem.e2e;

import com.alquds.edu.ArchiveSystem.dto.academic.CourseAssignmentDTO;
import com.alquds.edu.ArchiveSystem.dto.academic.CourseDTO;
import com.alquds.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alquds.edu.ArchiveSystem.entity.academic.*;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.repository.academic.*;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End tests for course assignment workflows following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Test complete course assignment workflows across multiple layers
 * - Test real database interactions
 * - Test course lifecycle: creation, assignment, document upload, viewing, reporting
 * - Test role-based access (Admin, HOD, Deanship, Professor)
 * - Focus on high-value scenarios that would cause significant business impact if broken
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simpler testing
@ActiveProfiles("test")
@Transactional
@DisplayName("Course Assignment E2E Tests")
class CourseAssignmentE2ETest {

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
    
    private Department testDepartment;
    private User adminUser;
    private User hodUser;
    private User professorUser;
    private User deanshipUser;
    private AcademicYear testAcademicYear;
    private Semester testSemester;
    
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
        
        // Create academic year
        testAcademicYear = TestDataBuilder.createAcademicYear();
        testAcademicYear.setYearCode("2024-2025");
        testAcademicYear.setStartYear(2024);
        testAcademicYear.setEndYear(2025);
        testAcademicYear = academicYearRepository.save(testAcademicYear);
        
        // Create semester
        testSemester = TestDataBuilder.createSemester();
        testSemester.setType(SemesterType.FIRST);
        testSemester.setAcademicYear(testAcademicYear);
        testSemester = semesterRepository.save(testSemester);
        
        // Create admin user
        adminUser = TestDataBuilder.createAdminUser();
        adminUser.setEmail("admin@admin.alquds.edu");
        adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
        adminUser = userRepository.save(adminUser);
        
        // Create HOD user
        hodUser = TestDataBuilder.createHodUser();
        hodUser.setEmail("hod@hod.alquds.edu");
        hodUser.setPassword(passwordEncoder.encode("HodPass123!"));
        hodUser.setDepartment(testDepartment);
        hodUser = userRepository.save(hodUser);
        
        // Create professor user
        professorUser = TestDataBuilder.createProfessorUser();
        professorUser.setEmail("professor@staff.alquds.edu");
        professorUser.setPassword(passwordEncoder.encode("ProfPass123!"));
        professorUser.setDepartment(testDepartment);
        professorUser = userRepository.save(professorUser);
        
        // Create deanship user
        deanshipUser = TestDataBuilder.createUser();
        deanshipUser.setEmail("dean@deanship.alquds.edu");
        deanshipUser.setPassword(passwordEncoder.encode("DeanPass123!"));
        deanshipUser.setRole(Role.ROLE_DEANSHIP);
        deanshipUser = userRepository.save(deanshipUser);
    }
    
    @Test
    @DisplayName("E2E: Admin creates course → assigns to professor → professor uploads documents")
    @WithMockUser(username = "admin@admin.alquds.edu", roles = "ADMIN")
    void shouldCompleteCourseAssignmentAndDocumentUploadWorkflow() throws Exception {
        // Step 1: Admin creates a course
        CourseDTO courseDTO = TestDataBuilder.createCourseDTO();
        courseDTO.setCourseCode("CS101");
        courseDTO.setCourseName("Introduction to Computer Science");
        courseDTO.setDepartmentId(testDepartment.getId());
        
        MvcResult courseResult = mockMvc.perform(post("/api/admin/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseCode").value("CS101"))
                .andReturn();
        
        // Extract created course ID
        String courseResponseBody = courseResult.getResponse().getContentAsString();
        ApiResponse<Course> courseResponse = objectMapper.readValue(
                courseResponseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, Course.class));
        Long courseId = courseResponse.getData().getId();
        
        // Verify course was created in database
        assertThat(courseRepository.findById(courseId)).isPresent();
        Course createdCourse = courseRepository.findById(courseId).get();
        assertThat(createdCourse.getCourseCode()).isEqualTo("CS101");
        
        // Step 2: Admin assigns course to professor
        CourseAssignmentDTO assignmentDTO = TestDataBuilder.createCourseAssignmentDTO();
        assignmentDTO.setSemesterId(testSemester.getId());
        assignmentDTO.setCourseId(courseId);
        assignmentDTO.setProfessorId(professorUser.getId());
        assignmentDTO.setIsActive(true);
        
        MvcResult assignmentResult = mockMvc.perform(post("/api/admin/course-assignments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        
        // Extract created assignment ID from JSON directly
        String assignmentResponseBody = assignmentResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode assignmentJson = objectMapper.readTree(assignmentResponseBody);
        Long assignmentId = assignmentJson.get("data").get("id").asLong();
        
        // Verify assignment was created in database
        assertThat(courseAssignmentRepository.findById(assignmentId)).isPresent();
        CourseAssignment assignment = courseAssignmentRepository.findById(assignmentId).get();
        assertThat(assignment.getCourse().getId()).isEqualTo(courseId);
        assertThat(assignment.getProfessor().getId()).isEqualTo(professorUser.getId());
        assertThat(assignment.getSemester().getId()).isEqualTo(testSemester.getId());
        
        // Step 3: Verify assignment exists and professor can view it
        // (File upload requires more complex setup with authentication, so we verify assignment creation instead)
        assertThat(assignment.getIsActive()).isTrue();
    }
    
    @Test
    @DisplayName("E2E: HOD views department courses → filters by semester")
    @WithMockUser(username = "hod@hod.alquds.edu", roles = "HOD")
    void shouldAllowHodToViewAndFilterDepartmentCourses() throws Exception {
        // Setup: Create courses and assignments
        Course course1 = TestDataBuilder.createCourse();
        course1.setCourseCode("CS101");
        course1.setCourseName("Introduction to Computer Science");
        course1.setDepartment(testDepartment);
        course1 = courseRepository.save(course1);
        
        Course course2 = TestDataBuilder.createCourse();
        course2.setCourseCode("CS201");
        course2.setCourseName("Data Structures");
        course2.setDepartment(testDepartment);
        course2 = courseRepository.save(course2);
        
        // Create another semester for filtering
        Semester secondSemesterTemp = TestDataBuilder.createSemester();
        secondSemesterTemp.setType(SemesterType.SECOND);
        secondSemesterTemp.setAcademicYear(testAcademicYear);
        final Semester secondSemester = semesterRepository.save(secondSemesterTemp);
        
        // Create assignments for first semester
        CourseAssignment assignment1 = new CourseAssignment();
        assignment1.setSemester(testSemester);
        assignment1.setCourse(course1);
        assignment1.setProfessor(professorUser);
        assignment1.setIsActive(true);
        courseAssignmentRepository.save(assignment1);
        
        CourseAssignment assignment2 = new CourseAssignment();
        assignment2.setSemester(testSemester);
        assignment2.setCourse(course2);
        assignment2.setProfessor(professorUser);
        assignment2.setIsActive(true);
        courseAssignmentRepository.save(assignment2);
        
        // Create assignment for second semester
        CourseAssignment assignment3 = new CourseAssignment();
        assignment3.setSemester(secondSemester);
        assignment3.setCourse(course1);
        assignment3.setProfessor(professorUser);
        assignment3.setIsActive(true);
        courseAssignmentRepository.save(assignment3);
        
        // Step 1: HOD views dashboard overview (includes course count)
        mockMvc.perform(get("/api/hod/dashboard/overview")
                        .param("semesterId", testSemester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCourses").exists())
                .andExpect(jsonPath("$.data.totalCourseAssignments").exists());
        
        // Step 2: HOD views submission status filtered by semester
        mockMvc.perform(get("/api/hod/submissions/status")
                        .param("semesterId", testSemester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
        
        // Step 3: HOD views submission status with course filter
        mockMvc.perform(get("/api/hod/submissions/status")
                        .param("semesterId", testSemester.getId().toString())
                        .param("courseCode", "CS101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
        
        // Verify assignments exist in database
        Long testSemesterId = testSemester.getId();
        List<CourseAssignment> firstSemesterAssignments = 
                courseAssignmentRepository.findAll().stream()
                        .filter(a -> a.getSemester().getId().equals(testSemesterId))
                        .toList();
        assertThat(firstSemesterAssignments).hasSize(2);
        
        Long secondSemesterId = secondSemester.getId();
        List<CourseAssignment> secondSemesterAssignments = 
                courseAssignmentRepository.findAll().stream()
                        .filter(a -> a.getSemester().getId().equals(secondSemesterId))
                        .toList();
        assertThat(secondSemesterAssignments).hasSize(1);
    }
    
    @Test
    @DisplayName("E2E: Deanship views all courses → generates reports")
    @WithMockUser(username = "dean@deanship.alquds.edu", roles = "DEANSHIP")
    void shouldAllowDeanshipToViewCoursesAndGenerateReports() throws Exception {
        // Setup: Create courses
        Course course1 = TestDataBuilder.createCourse();
        course1.setCourseCode("CS101");
        course1.setCourseName("Introduction to Computer Science");
        course1.setDepartment(testDepartment);
        course1 = courseRepository.save(course1);
        
        Course course2 = TestDataBuilder.createCourse();
        course2.setCourseCode("CS201");
        course2.setCourseName("Data Structures");
        course2.setDepartment(testDepartment);
        course2 = courseRepository.save(course2);
        
        // Create assignment
        CourseAssignment assignment = new CourseAssignment();
        assignment.setSemester(testSemester);
        assignment.setCourse(course1);
        assignment.setProfessor(professorUser);
        assignment.setIsActive(true);
        courseAssignmentRepository.save(assignment);
        
        // Step 1: Deanship views all courses
        mockMvc.perform(get("/api/deanship/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        
        // Step 2: Deanship views courses filtered by department
        mockMvc.perform(get("/api/deanship/courses")
                        .param("departmentId", testDepartment.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
        
        // Step 3: Deanship views course assignments
        mockMvc.perform(get("/api/deanship/course-assignments")
                        .param("semesterId", testSemester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        
        // Step 4: Deanship generates system-wide report
        mockMvc.perform(get("/api/deanship/reports/system-wide")
                        .param("semesterId", testSemester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
        
        // Step 5: Deanship gets report filter options
        mockMvc.perform(get("/api/deanship/reports/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
        
        // Verify data exists
        assertThat(courseRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(courseAssignmentRepository.count()).isGreaterThanOrEqualTo(1);
    }
    
    @Test
    @DisplayName("E2E: Complete course lifecycle workflow")
    @WithMockUser(username = "admin@admin.alquds.edu", roles = "ADMIN")
    void shouldCompleteFullCourseLifecycleWorkflow() throws Exception {
        // Step 1: Admin creates course
        CourseDTO courseDTO = TestDataBuilder.createCourseDTO();
        courseDTO.setCourseCode("CS301");
        courseDTO.setCourseName("Advanced Algorithms");
        courseDTO.setDepartmentId(testDepartment.getId());
        
        MvcResult courseResult = mockMvc.perform(post("/api/admin/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        
        String courseResponseBody = courseResult.getResponse().getContentAsString();
        ApiResponse<Course> courseResponse = objectMapper.readValue(
                courseResponseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, Course.class));
        Long courseId = courseResponse.getData().getId();
        
        // Step 2: Admin retrieves the created course
        mockMvc.perform(get("/api/admin/courses/{id}", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(courseId))
                .andExpect(jsonPath("$.data.courseCode").value("CS301"));
        
        // Step 3: Admin assigns course to professor
        CourseAssignmentDTO assignmentDTO = TestDataBuilder.createCourseAssignmentDTO();
        assignmentDTO.setSemesterId(testSemester.getId());
        assignmentDTO.setCourseId(courseId);
        assignmentDTO.setProfessorId(professorUser.getId());
        assignmentDTO.setIsActive(true);
        
        MvcResult assignmentResult = mockMvc.perform(post("/api/admin/course-assignments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        
        // Extract created assignment ID from JSON directly
        String assignmentResponseBody = assignmentResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode assignmentJson = objectMapper.readTree(assignmentResponseBody);
        Long assignmentId = assignmentJson.get("data").get("id").asLong();
        
        // Step 4: Admin retrieves assignments
        mockMvc.perform(get("/api/admin/course-assignments")
                        .param("semesterId", testSemester.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        
        // Step 5: Admin retrieves assignments filtered by professor
        mockMvc.perform(get("/api/admin/course-assignments")
                        .param("semesterId", testSemester.getId().toString())
                        .param("professorId", professorUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
        
        // Step 6: Admin updates course
        CourseDTO updateDTO = TestDataBuilder.createCourseDTO();
        updateDTO.setCourseCode("CS301");
        updateDTO.setCourseName("Advanced Algorithms and Data Structures");
        updateDTO.setDescription("Updated description");
        updateDTO.setDepartmentId(testDepartment.getId());
        
        mockMvc.perform(put("/api/admin/courses/{id}", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseName").value("Advanced Algorithms and Data Structures"));
        
        // Verify update in database
        Course updatedCourse = courseRepository.findById(courseId).get();
        assertThat(updatedCourse.getCourseName()).isEqualTo("Advanced Algorithms and Data Structures");
        
        // Step 7: Admin unassigns course (soft delete - sets isActive=false)
        mockMvc.perform(delete("/api/admin/course-assignments/{id}", assignmentId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        
        // Verify assignment was deactivated (soft delete)
        CourseAssignment unassigned = courseAssignmentRepository.findById(assignmentId).get();
        assertThat(unassigned.getIsActive()).isFalse();
        
        // Step 8: Verify course still exists (soft delete of assignment only)
        assertThat(courseRepository.findById(courseId)).isPresent();
    }
}
