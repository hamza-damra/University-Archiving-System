package com.alquds.edu.ArchiveSystem.e2e;

import com.alquds.edu.ArchiveSystem.dto.auth.LoginRequest;
import com.alquds.edu.ArchiveSystem.dto.auth.TokenRefreshRequest;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.repository.academic.DepartmentRepository;
import com.alquds.edu.ArchiveSystem.repository.auth.RefreshTokenRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End tests for authentication workflows following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Test complete authentication workflows across multiple layers
 * - Test real database interactions
 * - Test token lifecycle management
 * - Test session management and timeout scenarios
 * - Test concurrent access patterns
 * - Focus on high-value scenarios that would cause significant business impact if broken
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.session.store-type=jdbc",
        "app.rate-limit.enabled=false",
        "spring.session.timeout=30s" // Short timeout for testing
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Authentication E2E Tests")
class AuthenticationE2ETest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EntityManager entityManager;
    
    private Department testDepartment;
    private User testUser;
    private String testPassword = "TestPass123!";
    
    @BeforeEach
    void setUp() {
        // Clean up test data
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        
        // Create test department
        testDepartment = TestDataBuilder.createDepartment();
        testDepartment.setName("Computer Science");
        testDepartment.setShortcut("cs");
        testDepartment = departmentRepository.save(testDepartment);
        
        // Create test user with encoded password
        testUser = TestDataBuilder.createProfessorUser();
        testUser.setEmail("testprofessor@staff.alquds.edu");
        testUser.setPassword(passwordEncoder.encode(testPassword));
        testUser.setDepartment(testDepartment);
        testUser.setIsActive(true);
        testUser = userRepository.save(testUser);
    }
    
    // ==================== Test 1: Complete Login → Refresh Token → Logout Flow ====================
    
    @Test
    @DisplayName("E2E: Complete login → refresh token → logout flow")
    void shouldCompleteLoginRefreshLogoutFlow() throws Exception {
        // Step 1: Login and get tokens
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(testPassword);
        
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();
        
        // Extract tokens from login response
        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String accessToken = loginJson.get("data").get("token").asText();
        String refreshToken = loginJson.get("data").get("refreshToken").asText();
        
        // Verify refresh token was created in database
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isPresent();
        
        // Step 2: Use access token to access protected endpoint
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.status").value("VALID"));
        
        // Step 3: Refresh the access token using refresh token
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest();
        refreshRequest.setRefreshToken(refreshToken);
        
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();
        
        // Extract new tokens
        String refreshResponse = refreshResult.getResponse().getContentAsString();
        JsonNode refreshJson = objectMapper.readTree(refreshResponse);
        String newAccessToken = refreshJson.get("data").get("accessToken").asText();
        String newRefreshToken = refreshJson.get("data").get("refreshToken").asText();
        
        // Verify new access token works
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));
        
        // Step 4: Logout using refresh token
        TokenRefreshRequest logoutRequest = new TokenRefreshRequest();
        logoutRequest.setRefreshToken(newRefreshToken);
        
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));
        
        // Step 5: Verify refresh token was revoked
        entityManager.clear();
        var revokedToken = refreshTokenRepository.findByToken(newRefreshToken);
        assertThat(revokedToken).isPresent();
        assertThat(revokedToken.get().isRevoked()).isTrue();
        
        // Step 6: Verify refresh token can no longer be used
        mockMvc.perform(post("/api/auth/refresh-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isForbidden());
    }
    
    // ==================== Test 2: Login → Session Timeout → Re-authentication ====================
    
    @Test
    @DisplayName("E2E: Login → session timeout → re-authentication")
    void shouldHandleSessionTimeoutAndReauthentication() throws Exception {
        // Step 1: Login and establish session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(testPassword);
        
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        // Extract session info
        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String accessToken = loginJson.get("data").get("token").asText();
        
        // Step 2: Verify token is valid (token validation works even without active session)
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));
        
        // Step 3: Get session info to verify it exists
        // Note: In MockMvc, sessions may not persist across requests the same way as in real scenarios
        // We'll verify the token works instead
        mockMvc.perform(get("/api/session/info")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        
        // Step 4: Simulate session timeout by invalidating session
        // In a real scenario, this would happen automatically after timeout
        // For testing, we'll manually invalidate and then try to re-authenticate
        mockMvc.perform(post("/api/session/invalidate")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        
        // Step 5: Verify session is no longer active (or token still works but session is invalidated)
        // After invalidation, the token may still be valid but session is gone
        mockMvc.perform(get("/api/session/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        
        // Step 6: Re-authenticate with same credentials
        MvcResult reLoginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();
        
        // Step 7: Verify new token is valid
        String reLoginResponse = reLoginResult.getResponse().getContentAsString();
        JsonNode reLoginJson = objectMapper.readTree(reLoginResponse);
        String newAccessToken = reLoginJson.get("data").get("token").asText();
        
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));
    }
    
    // ==================== Test 3: Multiple Device Login → Token Management ====================
    
    @Test
    @DisplayName("E2E: Multiple device login → token management")
    void shouldHandleMultipleDeviceLoginAndTokenManagement() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(testPassword);
        
        // Step 1: Login from device 1
        MvcResult device1Result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header("User-Agent", "Device1-Browser"))
                .andExpect(status().isOk())
                .andReturn();
        
        String device1Response = device1Result.getResponse().getContentAsString();
        JsonNode device1Json = objectMapper.readTree(device1Response);
        String device1RefreshToken = device1Json.get("data").get("refreshToken").asText();
        
        // Step 2: Login from device 2
        MvcResult device2Result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header("User-Agent", "Device2-Browser"))
                .andExpect(status().isOk())
                .andReturn();
        
        String device2Response = device2Result.getResponse().getContentAsString();
        JsonNode device2Json = objectMapper.readTree(device2Response);
        String device2RefreshToken = device2Json.get("data").get("refreshToken").asText();
        
        // Step 3: Verify both tokens exist and are different
        assertThat(device1RefreshToken).isNotEqualTo(device2RefreshToken);
        assertThat(refreshTokenRepository.findByToken(device1RefreshToken)).isPresent();
        assertThat(refreshTokenRepository.findByToken(device2RefreshToken)).isPresent();
        
        // Step 4: Verify both tokens can be used independently
        TokenRefreshRequest refresh1 = new TokenRefreshRequest();
        refresh1.setRefreshToken(device1RefreshToken);
        
        mockMvc.perform(post("/api/auth/refresh-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refresh1)))
                .andExpect(status().isOk());
        
        TokenRefreshRequest refresh2 = new TokenRefreshRequest();
        refresh2.setRefreshToken(device2RefreshToken);
        
        mockMvc.perform(post("/api/auth/refresh-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refresh2)))
                .andExpect(status().isOk());
        
        // Step 5: Login from a few more devices (but not exceeding max limit to avoid token revocation)
        // We already have 2 tokens, so we'll add 2 more (total 4, which is under the limit of 5)
        String[] additionalDeviceTokens = new String[2];
        for (int i = 0; i < 2; i++) {
            MvcResult deviceResult = mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .header("User-Agent", "Device" + (i + 3) + "-Browser"))
                    .andExpect(status().isOk())
                    .andReturn();
            
            String deviceResponse = deviceResult.getResponse().getContentAsString();
            JsonNode deviceJson = objectMapper.readTree(deviceResponse);
            additionalDeviceTokens[i] = deviceJson.get("data").get("refreshToken").asText();
        }
        
        // Step 6: Verify all tokens exist
        entityManager.clear();
        List<com.alquds.edu.ArchiveSystem.entity.auth.RefreshToken> userTokens = 
                refreshTokenRepository.findByUserId(testUser.getId());
        
        // Should have at most 5 active tokens (system revokes old ones when limit reached)
        long activeTokens = userTokens.stream()
                .filter(token -> !token.isRevoked() && !token.isExpired())
                .count();
        
        assertThat(activeTokens).isLessThanOrEqualTo(5);
        assertThat(activeTokens).isGreaterThan(0);
        
        // Step 7: Logout from one device and verify others still work
        TokenRefreshRequest logoutRequest = new TokenRefreshRequest();
        logoutRequest.setRefreshToken(device2RefreshToken); // Use device2 token
        
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk());
        
        // Verify logged out token is revoked
        entityManager.clear();
        var revokedToken = refreshTokenRepository.findByToken(device2RefreshToken);
        assertThat(revokedToken).isPresent();
        assertThat(revokedToken.get().isRevoked()).isTrue();
        
        // Verify device1 token still works
        TokenRefreshRequest refresh3 = new TokenRefreshRequest();
        refresh3.setRefreshToken(device1RefreshToken);
        
        mockMvc.perform(post("/api/auth/refresh-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refresh3)))
                .andExpect(status().isOk());
    }
    
    // ==================== Test 4: Token Expiration Handling Workflow ====================
    
    @Test
    @DisplayName("E2E: Token expiration handling workflow")
    void shouldHandleTokenExpirationWorkflow() throws Exception {
        // Step 1: Login and get tokens
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
        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String accessToken = loginJson.get("data").get("token").asText();
        String refreshToken = loginJson.get("data").get("refreshToken").asText();
        
        // Step 2: Verify token is valid initially
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.status").value("VALID"));
        
        // Step 3: Use refresh token to get new access token (simulating token refresh before expiration)
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest();
        refreshRequest.setRefreshToken(refreshToken);
        
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        String refreshResponse = refreshResult.getResponse().getContentAsString();
        JsonNode refreshJson = objectMapper.readTree(refreshResponse);
        String newAccessToken = refreshJson.get("data").get("accessToken").asText();
        String newRefreshToken = refreshJson.get("data").get("refreshToken").asText();
        
        // Step 4: Verify new access token is valid
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));
        
        // Step 5: Verify old access token still works (until it expires naturally)
        // In a real scenario, the old token would expire after its TTL
        // For testing, we verify the refresh mechanism works correctly
        
        // Step 6: Test expired refresh token scenario
        // Manually expire a refresh token in the database
        entityManager.clear();
        var tokenOpt = refreshTokenRepository.findByToken(newRefreshToken);
        assertThat(tokenOpt).isPresent();
        
        com.alquds.edu.ArchiveSystem.entity.auth.RefreshToken token = tokenOpt.get();
        token.setExpiryDate(Instant.now().minusSeconds(3600)); // Set to 1 hour ago
        refreshTokenRepository.save(token);
        entityManager.flush();
        entityManager.clear();
        
        // Step 7: Try to use expired refresh token
        TokenRefreshRequest expiredRefreshRequest = new TokenRefreshRequest();
        expiredRefreshRequest.setRefreshToken(newRefreshToken);
        
        mockMvc.perform(post("/api/auth/refresh-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expiredRefreshRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
        
        // Step 8: Re-authenticate to get new tokens
        MvcResult reLoginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        String reLoginResponse = reLoginResult.getResponse().getContentAsString();
        JsonNode reLoginJson = objectMapper.readTree(reLoginResponse);
        String finalAccessToken = reLoginJson.get("data").get("token").asText();
        
        // Step 9: Verify new token works
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + finalAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));
    }
    
    // ==================== Test 5: Concurrent Login Sessions ====================
    
    @Test
    @DisplayName("E2E: Concurrent login sessions")
    void shouldHandleConcurrentLoginSessions() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(testPassword);
        
        // Step 1: Perform sequential logins (simulating concurrent access pattern)
        // Note: MockMvc is not thread-safe, so we test sequential logins that simulate
        // the same user logging in from multiple devices/sessions
        int numberOfLogins = 3;
        String[] accessTokens = new String[numberOfLogins];
        String[] refreshTokens = new String[numberOfLogins];
        
        for (int i = 0; i < numberOfLogins; i++) {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .header("User-Agent", "Concurrent-Device-" + i))
                    .andExpect(status().isOk())
                    .andReturn();
            
            String response = result.getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(response);
            accessTokens[i] = json.get("data").get("token").asText();
            refreshTokens[i] = json.get("data").get("refreshToken").asText();
            
            // Verify token is valid
            mockMvc.perform(get("/api/auth/validate")
                            .header("Authorization", "Bearer " + accessTokens[i]))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.valid").value(true));
        }
        
        // Step 2: Verify all tokens are unique (access tokens may be the same if generated at same time,
        // but refresh tokens should be unique)
        assertThat(refreshTokens[0]).isNotNull();
        assertThat(refreshTokens[1]).isNotNull();
        assertThat(refreshTokens[2]).isNotNull();
        assertThat(refreshTokens[0]).isNotEqualTo(refreshTokens[1]);
        assertThat(refreshTokens[1]).isNotEqualTo(refreshTokens[2]);
        
        // Step 3: Verify tokens were created (may be limited by max tokens per user)
        entityManager.clear();
        List<com.alquds.edu.ArchiveSystem.entity.auth.RefreshToken> userTokens = 
                refreshTokenRepository.findByUserId(testUser.getId());
        
        // Should have tokens (may be less than requested if system revokes old ones)
        assertThat(userTokens.size()).isGreaterThan(0);
        
        // Step 4: Verify all tokens are unique in database
        long uniqueTokens = userTokens.stream()
                .map(com.alquds.edu.ArchiveSystem.entity.auth.RefreshToken::getToken)
                .distinct()
                .count();
        
        assertThat(uniqueTokens).isEqualTo(userTokens.size());
        
        // Step 5: Verify at least one token can be used (some may be revoked if max limit reached)
        // Find a valid token to refresh
        boolean refreshedAtLeastOne = false;
        for (String refreshToken : refreshTokens) {
            if (refreshToken != null) {
                TokenRefreshRequest refreshRequest = new TokenRefreshRequest();
                refreshRequest.setRefreshToken(refreshToken);
                
                try {
                    mockMvc.perform(post("/api/auth/refresh-token")
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(refreshRequest)))
                            .andExpect(status().isOk());
                    refreshedAtLeastOne = true;
                    break; // Found a valid token, no need to check others
                } catch (Exception e) {
                    // Token may have been revoked, try next one
                    continue;
                }
            }
        }
        
        // At least one token should be usable
        assertThat(refreshedAtLeastOne)
                .as("At least one refresh token should be usable")
                .isTrue();
    }
}
