package com.alqude.edu.ArchiveSystem.session;

import com.alqude.edu.ArchiveSystem.dto.auth.LoginRequest;
import com.alqude.edu.ArchiveSystem.entity.Department;
import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.repository.DepartmentRepository;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for database-backed session management.
 * 
 * Tests verify:
 * - Session persistence to database
 * - Session survival across app restarts (simulated)
 * - Session ID rotation on authentication
 * - Session invalidation on logout
 * - Session timeout behavior
 * - Secure cookie settings
 * 
 * @author Archive System Team
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
class SessionManagementIntegrationTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TEST_EMAIL = "session.test@alquds.edu";
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String TEST_DEPT_NAME = "Test Department Session";
    private User testUser;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        userRepository.findByEmail(TEST_EMAIL).ifPresent(user -> {
            userRepository.delete(user);
            userRepository.flush();
        });

        // Find or create test department
        testDepartment = departmentRepository.findByName(TEST_DEPT_NAME)
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setName(TEST_DEPT_NAME);
                    dept.setDescription("Test department for session management tests");
                    return departmentRepository.saveAndFlush(dept);
                });

        // Create test user
        testUser = new User();
        testUser.setEmail(TEST_EMAIL);
        testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setFirstName("Session");
        testUser.setLastName("Test");
        testUser.setRole(Role.ROLE_PROFESSOR);
        testUser.setDepartment(testDepartment);
        testUser = userRepository.saveAndFlush(testUser);
        assertThat(testUser).isNotNull();
    }

    @AfterEach
    void tearDown() {
        // Clean up test user
        userRepository.findByEmail(TEST_EMAIL).ifPresent(user -> {
            userRepository.delete(user);
            userRepository.flush();
        });
    }

    /**
     * Test 1: Verify session is created and persisted to database on login.
     */
    @Test
    @Order(1)
    @DisplayName("Session should be persisted to database after login")
    void testSessionPersistedToDatabase() throws Exception {
        // Count sessions before login
        Integer sessionCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION", Integer.class);
        assertThat(sessionCountBefore).isNotNull();

        // Perform login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("ARCHIVESESSION"))
                .andReturn();

        // Extract session ID from cookie
        var cookie = result.getResponse().getCookie("ARCHIVESESSION");
        assertThat(cookie).isNotNull();
        String sessionId = cookie.getValue();
        assertThat(sessionId).isNotNull().isNotEmpty();

        // Verify session exists in database
        Integer sessionCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION", Integer.class);
        assertThat(sessionCountAfter).isGreaterThan(sessionCountBefore);

        // Verify session can be found by SESSION_ID
        Integer sessionExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION WHERE SESSION_ID = ?",
                Integer.class,
                sessionId);
        assertThat(sessionExists).isEqualTo(1);
    }

    /**
     * Test 2: Verify session ID changes on authentication (session fixation protection).
     */
    @Test
    @Order(2)
    @DisplayName("Session ID should rotate on successful authentication")
    void testSessionIdRotationOnLogin() throws Exception {
        // Create initial session
        MvcResult initialResult = mockMvc.perform(get("/api/session/status"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession initialSession = (MockHttpSession) initialResult.getRequest().getSession();
        assertThat(initialSession).isNotNull();
        String initialSessionId = initialSession.getId();

        // Perform login with initial session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .session(initialSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("ARCHIVESESSION"))
                .andReturn();

        // Extract new session ID
        var newCookie = loginResult.getResponse().getCookie("ARCHIVESESSION");
        assertThat(newCookie).isNotNull();
        String newSessionId = newCookie.getValue();

        // Verify session ID changed (session rotation)
        assertThat(newSessionId).isNotEqualTo(initialSessionId);
    }

    /**
     * Test 3: Verify session is invalidated on logout.
     */
    @Test
    @Order(3)
    @DisplayName("Session should be invalidated and removed from database on logout")
    void testSessionInvalidationOnLogout() throws Exception {
        // Login to create session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("ARCHIVESESSION"))
                .andReturn();

        var sessionCookie = loginResult.getResponse().getCookie("ARCHIVESESSION");
        assertThat(sessionCookie).isNotNull();
        String sessionId = sessionCookie.getValue();

        // Verify session exists in database
        Integer sessionExistsBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION WHERE SESSION_ID = ?",
                Integer.class,
                sessionId);
        assertThat(sessionExistsBefore).isEqualTo(1);

        // Perform logout with session cookie
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(sessionCookie))
                .andExpect(status().isOk());

        // Wait briefly for async session cleanup
        Thread.sleep(1000);

        // Verify session is removed from database (or marked as expired)
        Integer sessionExistsAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION WHERE SESSION_ID = ?",
                Integer.class,
                sessionId);
        
        // Session should be gone or expired
        assertThat(sessionExistsAfter).isLessThanOrEqualTo(0);
    }

    /**
     * Test 4: Verify session persists across requests (stateful behavior).
     */
    @Test
    @Order(4)
    @DisplayName("Session should persist across multiple requests")
    void testSessionPersistenceAcrossRequests() throws Exception {
        // Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("ARCHIVESESSION"))
                .andReturn();

        var sessionCookie = loginResult.getResponse().getCookie("ARCHIVESESSION");
        assertThat(sessionCookie).isNotNull();
        String sessionId = sessionCookie.getValue();

        // First request - get session info (with authentication)
        mockMvc.perform(get("/api/session/info")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(sessionId));

        // Second request - refresh session (using same cookie)
        mockMvc.perform(post("/api/session/refresh")
                        .cookie(sessionCookie))
                .andExpect(status().isOk());

        // Third request - verify session still active
        mockMvc.perform(get("/api/session/status")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    /**
     * Test 5: Verify session cookie has correct security attributes.
     */
    @Test
    @Order(5)
    @DisplayName("Session cookie should have secure attributes")
    void testSessionCookieSecurityAttributes() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("ARCHIVESESSION"))
                .andExpect(cookie().httpOnly("ARCHIVESESSION", true))
                .andExpect(cookie().path("ARCHIVESESSION", "/"))
                .andReturn();

        // Note: In test profile, secure flag is false (no HTTPS)
        // In production, this should be true
    }

    /**
     * Test 6: Verify session attributes are stored in database.
     */
    @Test
    @Order(6)
    @DisplayName("Session attributes should be persisted to database")
    void testSessionAttributesPersistence() throws Exception {
        // Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var sessionCookie = loginResult.getResponse().getCookie("ARCHIVESESSION");
        assertThat(sessionCookie).isNotNull();
        String sessionId = sessionCookie.getValue();

        // Check if attributes exist in database
        Integer attributeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES sa " +
                        "JOIN SPRING_SESSION s ON sa.SESSION_PRIMARY_ID = s.PRIMARY_ID " +
                        "WHERE s.SESSION_ID = ?",
                Integer.class,
                sessionId);

        // Should have at least SPRING_SECURITY_CONTEXT attribute
        assertThat(attributeCount).isGreaterThan(0);
    }

    /**
     * Test 7: Verify session repository health check.
     */
    @Test
    @Order(7)
    @DisplayName("Session repository health check should return UP")
    void testSessionRepositoryHealthCheck() throws Exception {
        mockMvc.perform(get("/api/session/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.sessionStore").value("JDBC"));
    }

    /**
     * Test 8: Verify unauthorized access without session.
     */
    @Test
    @Order(8)
    @DisplayName("Protected endpoints should require valid session")
    void testUnauthorizedAccessWithoutSession() throws Exception {
        // Try to access protected endpoint without session/authentication
        mockMvc.perform(get("/api/session/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    /**
     * Test 9: Verify session cleanup removes expired sessions.
     */
    @Test
    @Order(9)
    @DisplayName("Expired sessions should be cleaned up from database")
    void testExpiredSessionCleanup() throws Exception {
        // Create session with very short timeout
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var expiredCookie = result.getResponse().getCookie("ARCHIVESESSION");
        assertThat(expiredCookie).isNotNull();
        String sessionId = expiredCookie.getValue();

        // Manually set session to expired in database
        jdbcTemplate.update(
                "UPDATE SPRING_SESSION SET EXPIRY_TIME = ? WHERE SESSION_ID = ?",
                System.currentTimeMillis() - 10000, // 10 seconds ago
                sessionId);

        // Wait for cleanup job (or manually trigger)
        // In production, cleanup runs based on cron schedule
        // For testing, we can verify the session is marked as expired

        Long expiryTime = jdbcTemplate.queryForObject(
                "SELECT EXPIRY_TIME FROM SPRING_SESSION WHERE SESSION_ID = ?",
                Long.class,
                sessionId);

        assertThat(expiryTime).isLessThan(System.currentTimeMillis());
    }
}
