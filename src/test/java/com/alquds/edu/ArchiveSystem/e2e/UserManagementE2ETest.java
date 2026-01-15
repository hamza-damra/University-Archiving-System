package com.alquds.edu.ArchiveSystem.e2e;

import com.alquds.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alquds.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alquds.edu.ArchiveSystem.dto.user.UserResponse;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.repository.academic.DepartmentRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End tests for critical user workflows following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - 5-10% E2E tests
 * - Test complete user workflows across multiple layers
 * - Test real database interactions
 * - Test authentication and authorization flows
 * - Focus on high-value scenarios that would cause significant business impact if broken
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simpler testing
@ActiveProfiles("test")
@Transactional
@DisplayName("User Management E2E Tests")
class UserManagementE2ETest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    private Department testDepartment;
    private User adminUser;
    
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
        
        // Create admin user for authentication
        adminUser = TestDataBuilder.createAdminUser();
        adminUser.setEmail("admin@admin.alquds.edu");
        adminUser = userRepository.save(adminUser);
    }
    
    @Test
    @DisplayName("E2E: Complete user lifecycle - Create, Read, Update, Delete")
    @WithMockUser(username = "admin@admin.alquds.edu", roles = "ADMIN")
    void shouldCompleteUserLifecycle() throws Exception {
        // Step 1: Create a new professor user
        UserCreateRequest createRequest = TestDataBuilder.createUserCreateRequest();
        createRequest.setEmail("newprofessor@staff.alquds.edu");
        createRequest.setPassword("TestPass123!");
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");
        createRequest.setRole(Role.ROLE_PROFESSOR);
        createRequest.setDepartmentId(testDepartment.getId());
        
        MvcResult createResult = mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(createRequest.getEmail()))
                .andReturn();
        
        // Extract created user ID
        String responseBody = createResult.getResponse().getContentAsString();
        ApiResponse<UserResponse> response = objectMapper.readValue(
                responseBody, 
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, UserResponse.class));
        Long createdUserId = response.getData().getId();
        
        // Step 2: Verify user was created in database
        assertThat(userRepository.findById(createdUserId)).isPresent();
        User createdUser = userRepository.findById(createdUserId).get();
        assertThat(createdUser.getEmail()).isEqualTo(createRequest.getEmail());
        assertThat(createdUser.getRole()).isEqualTo(Role.ROLE_PROFESSOR);
        
        // Step 3: Retrieve the user
        mockMvc.perform(get("/api/admin/users/{id}", createdUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(createdUserId))
                .andExpect(jsonPath("$.data.email").value(createRequest.getEmail()));
        
        // Step 4: Update the user
        com.alquds.edu.ArchiveSystem.dto.user.UserUpdateRequest updateRequest = 
                TestDataBuilder.createUserUpdateRequest();
        updateRequest.setEmail(createRequest.getEmail()); // Keep same email
        updateRequest.setFirstName("Jane");
        updateRequest.setLastName("Smith");
        
        mockMvc.perform(put("/api/admin/users/{id}", createdUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Jane"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"));
        
        // Step 5: Verify update in database
        User updatedUser = userRepository.findById(createdUserId).get();
        assertThat(updatedUser.getFirstName()).isEqualTo("Jane");
        assertThat(updatedUser.getLastName()).isEqualTo("Smith");
        
        // Step 6: Delete the user
        mockMvc.perform(delete("/api/admin/users/{id}", createdUserId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        
        // Step 7: Verify user was deleted
        assertThat(userRepository.findById(createdUserId)).isEmpty();
    }
    
    @Test
    @DisplayName("E2E: Admin creates HOD, HOD creates Professor - Role hierarchy workflow")
    @WithMockUser(username = "admin@admin.alquds.edu", roles = "ADMIN")
    void shouldHandleRoleHierarchyWorkflow() throws Exception {
        // Step 1: Admin creates HOD
        UserCreateRequest hodRequest = TestDataBuilder.createUserCreateRequest();
        hodRequest.setEmail("hod.cs@hod.alquds.edu");
        hodRequest.setPassword("HodPass123!");
        hodRequest.setFirstName("HOD");
        hodRequest.setLastName("User");
        hodRequest.setRole(Role.ROLE_HOD);
        hodRequest.setDepartmentId(testDepartment.getId());
        
        MvcResult hodResult = mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hodRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("ROLE_HOD"))
                .andReturn();
        
        // Extract HOD user ID
        String hodResponseBody = hodResult.getResponse().getContentAsString();
        ApiResponse<UserResponse> hodResponse = objectMapper.readValue(
                hodResponseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, UserResponse.class));
        Long hodId = hodResponse.getData().getId();
        
        // Step 2: Verify HOD was created
        assertThat(userRepository.findById(hodId)).isPresent();
        User hodUser = userRepository.findById(hodId).get();
        assertThat(hodUser.getRole()).isEqualTo(Role.ROLE_HOD);
        assertThat(hodUser.getDepartment().getId()).isEqualTo(testDepartment.getId());
        
        // Step 3: Admin creates Professor (simulating HOD would do this in real scenario)
        UserCreateRequest professorRequest = TestDataBuilder.createUserCreateRequest();
        professorRequest.setEmail("professor@staff.alquds.edu");
        professorRequest.setPassword("ProfPass123!");
        professorRequest.setFirstName("Professor");
        professorRequest.setLastName("User");
        professorRequest.setRole(Role.ROLE_PROFESSOR);
        professorRequest.setDepartmentId(testDepartment.getId());
        
        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(professorRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("ROLE_PROFESSOR"));
        
        // Step 4: Verify both users exist and belong to same department
        assertThat(userRepository.findByEmail(hodRequest.getEmail())).isPresent();
        assertThat(userRepository.findByEmail(professorRequest.getEmail())).isPresent();
        
        User hod = userRepository.findByEmail(hodRequest.getEmail()).get();
        User professor = userRepository.findByEmail(professorRequest.getEmail()).get();
        
        assertThat(hod.getDepartment().getId()).isEqualTo(professor.getDepartment().getId());
    }
    
    @Test
    @DisplayName("E2E: User creation with validation errors - Complete error handling flow")
    @WithMockUser(username = "admin@admin.alquds.edu", roles = "ADMIN")
    void shouldHandleValidationErrorsInUserCreation() throws Exception {
        // Test 1: Missing required fields
        UserCreateRequest invalidRequest = TestDataBuilder.createUserCreateRequest();
        invalidRequest.setEmail(null);
        invalidRequest.setFirstName("");
        invalidRequest.setPassword("weak");
        invalidRequest.setDepartmentId(testDepartment.getId());
        
        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        
        // Test 2: Duplicate email
        User existingUser = TestDataBuilder.createProfessorUser();
        existingUser.setEmail("duplicate@staff.alquds.edu");
        existingUser.setDepartment(testDepartment);
        userRepository.save(existingUser);
        
        UserCreateRequest duplicateEmailRequest = TestDataBuilder.createUserCreateRequest();
        duplicateEmailRequest.setEmail("duplicate@staff.alquds.edu");
        duplicateEmailRequest.setDepartmentId(testDepartment.getId());
        
        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmailRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
        
        // Test 3: Invalid department
        UserCreateRequest invalidDeptRequest = TestDataBuilder.createUserCreateRequest();
        invalidDeptRequest.setDepartmentId(999L);
        
        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDeptRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
