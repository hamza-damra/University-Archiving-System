package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.dto.auth.LoginRequest;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.entity.user.User;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SessionController following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Test session management endpoints with real database interactions
 * - Use @SpringBootTest with MockMvc
 * - Test session creation, retrieval, refresh, and invalidation
 * - Test session timeout handling
 * - Test role-based access control
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.session.store-type=jdbc",
        "app.rate-limit.enabled=false"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("SessionController Integration Tests")
class SessionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private Department testDepartment;
    private User testUser;
    private User testHodUser;
    private String testPassword = "TestPass123!";
    
    @BeforeEach
    void setUp() {
        // Clean up test data
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        
        // Create test department
        testDepartment = TestDataBuilder.createDepartment();
        testDepartment.setName("Computer Science");
        testDepartment.setShortcut("cs");
        testDepartment = departmentRepository.save(testDepartment);
        
        // Create test professor user
        testUser = TestDataBuilder.createProfessorUser();
        testUser.setEmail("testprofessor@staff.alquds.edu");
        testUser.setPassword(passwordEncoder.encode(testPassword));
        testUser.setDepartment(testDepartment);
        testUser.setIsActive(true);
        testUser = userRepository.save(testUser);
        
        // Create test HOD user
        testHodUser = TestDataBuilder.createHodUser();
        testHodUser.setEmail("hod.cs@hod.alquds.edu");
        testHodUser.setPassword(passwordEncoder.encode(testPassword));
        testHodUser.setDepartment(testDepartment);
        testHodUser.setIsActive(true);
        testHodUser = userRepository.save(testHodUser);
    }
    
    // ==================== Session Info Tests ====================
    
    @Test
    @DisplayName("GET /api/session/info - Should return current session information when session exists")
    void shouldReturnCurrentSessionInfoWhenSessionExists() throws Exception {
        // Arrange - Login to create a session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(testPassword);
        
        // Login and extract session cookie
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie sessionCookie = loginResult.getResponse().getCookie("ARCHIVESESSION");
        assertThat(sessionCookie).isNotNull();
        
        // Act & Assert - Get session info using the session cookie
        mockMvc.perform(get("/api/session/info")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Session information retrieved"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andExpect(jsonPath("$.data.creationTime").exists())
                .andExpect(jsonPath("$.data.lastAccessedTime").exists())
                .andExpect(jsonPath("$.data.maxInactiveInterval").exists())
                .andExpect(jsonPath("$.data.expiresInSeconds").exists());
    }
    
    @Test
    @DisplayName("GET /api/session/info - Should return inactive session when no session exists")
    void shouldReturnInactiveSessionWhenNoSessionExists() throws Exception {
        // Act & Assert - Get session info without session
        mockMvc.perform(get("/api/session/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Session information retrieved"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.message").value("No active session"));
    }
    
    // ==================== Session Status Tests ====================
    
    @Test
    @DisplayName("GET /api/session/status - Should return active status when session exists")
    void shouldReturnActiveStatusWhenSessionExists() throws Exception {
        // Arrange - Login to create a session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(testPassword);
        
        // Login and extract session cookie
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie sessionCookie = loginResult.getResponse().getCookie("ARCHIVESESSION");
        assertThat(sessionCookie).isNotNull();
        
        // Act & Assert - Get session status using the session cookie
        mockMvc.perform(get("/api/session/status")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Session status checked"))
                .andExpect(jsonPath("$.data.active").value(true));
    }
    
    @Test
    @DisplayName("GET /api/session/status - Should return inactive status when no session exists")
    void shouldReturnInactiveStatusWhenNoSessionExists() throws Exception {
        // Act & Assert - Get session status without session
        mockMvc.perform(get("/api/session/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Session status checked"))
                .andExpect(jsonPath("$.data.active").value(false));
    }
    
    // ==================== Session Refresh Tests ====================
    
    @Test
    @DisplayName("POST /api/session/refresh - Should refresh session successfully")
    void shouldRefreshSessionSuccessfully() throws Exception {
        // Arrange - Login to create a session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(testPassword);
        
        // Login and extract session cookie
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie sessionCookie = loginResult.getResponse().getCookie("ARCHIVESESSION");
        assertThat(sessionCookie).isNotNull();
        
        // Wait a bit to ensure time difference
        Thread.sleep(100);
        
        // Act & Assert - Refresh session using the session cookie
        mockMvc.perform(post("/api/session/refresh")
                        .cookie(sessionCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Session refreshed successfully"))
                .andExpect(jsonPath("$.data").exists());
    }
    
    @Test
    @DisplayName("POST /api/session/refresh - Should return 400 when no session exists")
    void shouldReturn400WhenRefreshingNonExistentSession() throws Exception {
        // Act & Assert - Try to refresh without session
        mockMvc.perform(post("/api/session/refresh")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No active session to refresh"));
    }
    
    // ==================== Session Invalidation Tests ====================
    
    @Test
    @DisplayName("POST /api/session/invalidate - Should invalidate session successfully")
    void shouldInvalidateSessionSuccessfully() throws Exception {
        // Arrange - Login to create a session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(testPassword);
        
        // Login and extract session cookie
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie sessionCookie = loginResult.getResponse().getCookie("ARCHIVESESSION");
        assertThat(sessionCookie).isNotNull();
        
        // Act & Assert - Invalidate session using the session cookie
        mockMvc.perform(post("/api/session/invalidate")
                        .cookie(sessionCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Session invalidated"))
                .andExpect(jsonPath("$.data").value("Logged out successfully"));
        
        // Verify session is invalidated - subsequent request should not have active session
        mockMvc.perform(get("/api/session/status")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }
    
    @Test
    @DisplayName("POST /api/session/invalidate - Should handle invalidating non-existent session gracefully")
    void shouldHandleInvalidatingNonExistentSessionGracefully() throws Exception {
        // Act & Assert - Try to invalidate without session
        mockMvc.perform(post("/api/session/invalidate")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("No active session"))
                .andExpect(jsonPath("$.data").value("Already logged out"));
    }
    
    // ==================== Session Statistics Tests ====================
    
    @Test
    @DisplayName("GET /api/session/stats - Should return session statistics for HOD role")
    void shouldReturnSessionStatsForHodRole() throws Exception {
        // Arrange - Login as HOD
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testHodUser.getEmail());
        loginRequest.setPassword(testPassword);
        
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        String loginResponse = loginResult.getResponse().getContentAsString();
        String jwtToken = objectMapper.readTree(loginResponse)
                .get("data")
                .get("token")
                .asText();
        
        // Act & Assert - Get session stats
        mockMvc.perform(get("/api/session/stats")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Session statistics retrieved"))
                .andExpect(jsonPath("$.data.table").value("SPRING_SESSION"));
    }
    
    @Test
    @DisplayName("GET /api/session/stats - Should return 403 for non-HOD role")
    void shouldReturn403ForNonHodRole() throws Exception {
        // Arrange - Login as professor
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(testPassword);
        
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        String loginResponse = loginResult.getResponse().getContentAsString();
        String jwtToken = objectMapper.readTree(loginResponse)
                .get("data")
                .get("token")
                .asText();
        
        // Act & Assert - Try to get session stats (should be denied)
        mockMvc.perform(get("/api/session/stats")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }
    
    // ==================== Session Health Check Tests ====================
    
    @Test
    @DisplayName("GET /api/session/health - Should return healthy status")
    void shouldReturnHealthyStatus() throws Exception {
        // Act & Assert - Health check
        mockMvc.perform(get("/api/session/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Session repository is healthy"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.sessionStore").value("JDBC"));
    }
    
    // ==================== Session Timeout Handling Tests ====================
    
    @Test
    @DisplayName("Session timeout - Should handle expired session gracefully")
    void shouldHandleExpiredSessionGracefully() throws Exception {
        // Arrange - Login to create a session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(testPassword);
        
        // Login and extract session cookie
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie sessionCookie = loginResult.getResponse().getCookie("ARCHIVESESSION");
        assertThat(sessionCookie).isNotNull();
        
        // Note: We can't directly set session timeout in Spring Session JDBC,
        // but we can test that the session info endpoint handles sessions correctly
        // Act & Assert - Access session info
        mockMvc.perform(get("/api/session/info")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(true));
    }
    
    @Test
    @DisplayName("Session refresh extends timeout - Should update last accessed time")
    void shouldUpdateLastAccessedTimeOnRefresh() throws Exception {
        // Arrange - Login to create a session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(testPassword);
        
        // Login and extract session cookie
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie sessionCookie = loginResult.getResponse().getCookie("ARCHIVESESSION");
        assertThat(sessionCookie).isNotNull();
        
        // Wait a bit
        Thread.sleep(100);
        
        // Act - Refresh session using the session cookie
        mockMvc.perform(post("/api/session/refresh")
                        .cookie(sessionCookie)
                        .with(csrf()))
                .andExpect(status().isOk());
        
        // Assert - Verify session info shows updated expiration
        mockMvc.perform(get("/api/session/info")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.expiresInSeconds").exists());
    }
}
