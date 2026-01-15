package com.alquds.edu.ArchiveSystem.service.auth;

import com.alquds.edu.ArchiveSystem.dto.auth.JwtResponse;
import com.alquds.edu.ArchiveSystem.dto.auth.LoginRequest;
import com.alquds.edu.ArchiveSystem.dto.auth.TokenRefreshRequest;
import com.alquds.edu.ArchiveSystem.dto.auth.TokenRefreshResponse;
import com.alquds.edu.ArchiveSystem.entity.auth.RefreshToken;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.exception.auth.TokenRefreshException;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Unit tests with mocked dependencies
 * - Test authentication, token refresh, and logout flows
 * - Follow AAA pattern
 * - Test security scenarios and edge cases
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RefreshTokenService refreshTokenService;
    
    @Mock
    private HttpServletRequest httpServletRequest;
    
    @Mock
    private HttpSession httpSession;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private SecurityContext securityContext;
    
    @InjectMocks
    private AuthService authService;
    
    private User testUser;
    private LoginRequest loginRequest;
    private RefreshToken refreshToken;
    
    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.createProfessorUser();
        testUser.setId(1L);
        testUser.setEmail("test@staff.alquds.edu");
        
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@staff.alquds.edu");
        loginRequest.setPassword("TestPass123!");
        
        refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setToken("refresh-token-123");
        refreshToken.setUser(testUser);
        refreshToken.setExpiryDate(java.time.Instant.now().plusSeconds(604800)); // 7 days
        
        // Setup SecurityContext - AuthService uses SecurityContextHolder directly
        // We'll set it up in each test as needed
    }
    
    // ==================== Login Tests ====================
    
    @Test
    @DisplayName("Should login successfully and return JWT response")
    void shouldLoginSuccessfully() {
        // Arrange
        String accessToken = "access-token-123";
        String deviceInfo = "Mozilla/5.0 | 127.0.0.1";
        
        when(httpServletRequest.getSession(false)).thenReturn(null);
        when(httpServletRequest.getSession(true)).thenReturn(httpSession);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        
        // AuthService uses SecurityContextHolder.getContext() directly, which returns a real context
        // We need to set it up properly
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        realContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(realContext);
        
        when(jwtService.generateToken(testUser)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(testUser.getId(), deviceInfo))
                .thenReturn(refreshToken);
        
        // Act
        JwtResponse result = authService.login(loginRequest, httpServletRequest);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(accessToken);
        assertThat(result.getRefreshToken()).isEqualTo(refreshToken.getToken());
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(result.getId()).isEqualTo(testUser.getId());
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(testUser);
        verify(refreshTokenService).createRefreshToken(testUser.getId(), deviceInfo);
        verify(httpSession).setAttribute(eq("SPRING_SECURITY_CONTEXT"), any(SecurityContext.class));
    }
    
    @Test
    @DisplayName("Should invalidate old session before creating new one")
    void shouldInvalidateOldSessionBeforeCreatingNewOne() {
        // Arrange
        HttpSession oldSession = mock(HttpSession.class);
        String oldSessionId = "old-session-id";
        
        when(httpServletRequest.getSession(false)).thenReturn(oldSession);
        when(oldSession.getId()).thenReturn(oldSessionId);
        when(httpServletRequest.getSession(true)).thenReturn(httpSession);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        
        // AuthService uses SecurityContextHolder.getContext() directly
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        realContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(realContext);
        
        when(jwtService.generateToken(testUser)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(anyLong(), anyString()))
                .thenReturn(refreshToken);
        
        // Act
        authService.login(loginRequest, httpServletRequest);
        
        // Assert
        verify(oldSession).invalidate();
        verify(httpServletRequest).getSession(true);
    }
    
    @Test
    @DisplayName("Should throw exception when authentication fails")
    void shouldThrowExceptionWhenAuthenticationFails() {
        // Arrange
        when(httpServletRequest.getSession(false)).thenReturn(null);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        
        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest, httpServletRequest))
                .isInstanceOf(BadCredentialsException.class);
        
        verify(refreshTokenService, never()).createRefreshToken(anyLong(), anyString());
    }
    
    @Test
    @DisplayName("Should create new session with SecurityContext")
    void shouldCreateNewSessionWithSecurityContext() {
        // Arrange
        String accessToken = "access-token-123";
        String deviceInfo = "Mozilla/5.0 | 127.0.0.1";
        
        when(httpServletRequest.getSession(false)).thenReturn(null);
        when(httpServletRequest.getSession(true)).thenReturn(httpSession);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        realContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(realContext);
        
        when(jwtService.generateToken(testUser)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(testUser.getId(), deviceInfo))
                .thenReturn(refreshToken);
        
        // Act
        authService.login(loginRequest, httpServletRequest);
        
        // Assert
        verify(httpServletRequest).getSession(true);
        verify(httpSession).setAttribute(eq("SPRING_SECURITY_CONTEXT"), any(SecurityContext.class));
        // Verify that SecurityContext contains the authentication
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }
    
    @Test
    @DisplayName("Should extract device info from request")
    void shouldExtractDeviceInfoFromRequest() {
        // Arrange
        String accessToken = "access-token-123";
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        String remoteAddr = "192.168.1.100";
        String expectedDeviceInfo = userAgent.substring(0, Math.min(userAgent.length(), 200)) + " | " + remoteAddr;
        
        when(httpServletRequest.getSession(false)).thenReturn(null);
        when(httpServletRequest.getSession(true)).thenReturn(httpSession);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn(userAgent);
        when(httpServletRequest.getRemoteAddr()).thenReturn(remoteAddr);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        realContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(realContext);
        
        when(jwtService.generateToken(testUser)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(testUser.getId(), expectedDeviceInfo))
                .thenReturn(refreshToken);
        
        // Act
        authService.login(loginRequest, httpServletRequest);
        
        // Assert
        verify(httpServletRequest).getHeader("User-Agent");
        verify(httpServletRequest).getRemoteAddr();
        verify(refreshTokenService).createRefreshToken(testUser.getId(), expectedDeviceInfo);
    }
    
    @Test
    @DisplayName("Should extract device info with null User-Agent")
    void shouldExtractDeviceInfoWithNullUserAgent() {
        // Arrange
        String accessToken = "access-token-123";
        String remoteAddr = "192.168.1.100";
        String expectedDeviceInfo = "Unknown | " + remoteAddr;
        
        when(httpServletRequest.getSession(false)).thenReturn(null);
        when(httpServletRequest.getSession(true)).thenReturn(httpSession);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn(remoteAddr);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        realContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(realContext);
        
        when(jwtService.generateToken(testUser)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(testUser.getId(), expectedDeviceInfo))
                .thenReturn(refreshToken);
        
        // Act
        authService.login(loginRequest, httpServletRequest);
        
        // Assert
        verify(refreshTokenService).createRefreshToken(testUser.getId(), expectedDeviceInfo);
    }
    
    // ==================== Token Refresh Tests ====================
    
    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() {
        // Arrange
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("refresh-token-123");
        String newAccessToken = "new-access-token-456";
        
        RefreshToken verifiedToken = new RefreshToken();
        verifiedToken.setToken("refresh-token-123");
        verifiedToken.setUser(testUser);
        
        when(refreshTokenService.findByToken("refresh-token-123"))
                .thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(verifiedToken);
        when(jwtService.generateToken(testUser)).thenReturn(newAccessToken);
        
        // Act
        TokenRefreshResponse result = authService.refreshToken(request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(newAccessToken);
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token-123");
        
        verify(refreshTokenService).findByToken("refresh-token-123");
        verify(refreshTokenService).verifyExpiration(refreshToken);
        verify(jwtService).generateToken(testUser);
    }
    
    @Test
    @DisplayName("Should throw exception when refresh token not found")
    void shouldThrowExceptionWhenRefreshTokenNotFound() {
        // Arrange
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("invalid-token");
        
        when(refreshTokenService.findByToken("invalid-token")).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("not in database");
        
        verify(jwtService, never()).generateToken(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when refresh token is expired")
    void shouldThrowExceptionWhenRefreshTokenExpired() {
        // Arrange
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("expired-token");
        
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken("expired-token");
        
        when(refreshTokenService.findByToken("expired-token"))
                .thenReturn(Optional.of(expiredToken));
        when(refreshTokenService.verifyExpiration(expiredToken))
                .thenThrow(new TokenRefreshException("expired-token", "Refresh token was expired"));
        
        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(TokenRefreshException.class);
        
        verify(jwtService, never()).generateToken(any(User.class));
    }
    
    // ==================== Logout Tests ====================
    
    @Test
    @DisplayName("Should logout successfully and revoke all tokens")
    void shouldLogoutSuccessfully() {
        // Arrange
        Long userId = 1L;
        
        when(httpServletRequest.getSession(false)).thenReturn(httpSession);
        when(httpSession.getId()).thenReturn("session-id-123");
        
        doNothing().when(refreshTokenService).revokeAllUserTokens(userId);
        
        // Act
        authService.logout(userId, httpServletRequest);
        
        // Assert
        verify(refreshTokenService).revokeAllUserTokens(userId);
        verify(httpSession).invalidate();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
    
    @Test
    @DisplayName("Should logout successfully even when no session exists")
    void shouldLogoutSuccessfullyWhenNoSession() {
        // Arrange
        Long userId = 1L;
        
        when(httpServletRequest.getSession(false)).thenReturn(null);
        doNothing().when(refreshTokenService).revokeAllUserTokens(userId);
        
        // Act
        authService.logout(userId, httpServletRequest);
        
        // Assert
        verify(refreshTokenService).revokeAllUserTokens(userId);
        verify(httpSession, never()).invalidate();
    }
    
    @Test
    @DisplayName("Should logout with specific token successfully")
    void shouldLogoutWithTokenSuccessfully() {
        // Arrange
        String refreshTokenValue = "refresh-token-123";
        
        when(httpServletRequest.getSession(false)).thenReturn(httpSession);
        doNothing().when(refreshTokenService).revokeToken(refreshTokenValue);
        
        // Act
        authService.logoutWithToken(refreshTokenValue, httpServletRequest);
        
        // Assert
        verify(refreshTokenService).revokeToken(refreshTokenValue);
        verify(httpSession).invalidate();
    }
    
    @Test
    @DisplayName("Should clear SecurityContext properly on logout")
    void shouldClearSecurityContextProperlyOnLogout() {
        // Arrange
        Long userId = 1L;
        
        // Set up SecurityContext with authentication
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        realContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(realContext);
        
        // Verify context is set before logout
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        
        when(httpServletRequest.getSession(false)).thenReturn(httpSession);
        doNothing().when(refreshTokenService).revokeAllUserTokens(userId);
        
        // Act
        authService.logout(userId, httpServletRequest);
        
        // Assert
        verify(refreshTokenService).revokeAllUserTokens(userId);
        verify(httpSession).invalidate();
        // Verify SecurityContext is cleared
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
    
    @Test
    @DisplayName("Should clear SecurityContext properly on logout with token")
    void shouldClearSecurityContextProperlyOnLogoutWithToken() {
        // Arrange
        String refreshTokenValue = "refresh-token-123";
        
        // Set up SecurityContext with authentication
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        realContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(realContext);
        
        // Verify context is set before logout
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        
        when(httpServletRequest.getSession(false)).thenReturn(httpSession);
        doNothing().when(refreshTokenService).revokeToken(refreshTokenValue);
        
        // Act
        authService.logoutWithToken(refreshTokenValue, httpServletRequest);
        
        // Assert
        verify(refreshTokenService).revokeToken(refreshTokenValue);
        verify(httpSession).invalidate();
        // Verify SecurityContext is cleared
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
    
    // ==================== Get Current User Tests ====================
    
    @Test
    @DisplayName("Should get current user successfully")
    void shouldGetCurrentUserSuccessfully() {
        // Arrange
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        realContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(realContext);
        
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@staff.alquds.edu");
        when(userRepository.findByEmailWithDepartment("test@staff.alquds.edu"))
                .thenReturn(Optional.of(testUser));
        
        // Act
        User result = authService.getCurrentUser();
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@staff.alquds.edu");
        verify(userRepository).findByEmailWithDepartment("test@staff.alquds.edu");
    }
    
    @Test
    @DisplayName("Should throw exception when no authentication found")
    void shouldThrowExceptionWhenNoAuthenticationFound() {
        // Arrange
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        realContext.setAuthentication(null);
        SecurityContextHolder.setContext(realContext);
        
        // Act & Assert
        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No authentication found");
    }
    
    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        realContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(realContext);
        
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("nonexistent@staff.alquds.edu");
        when(userRepository.findByEmailWithDepartment("nonexistent@staff.alquds.edu"))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }
    
    @Test
    @DisplayName("Should get current user when principal is UserDetails")
    void shouldGetCurrentUserWhenPrincipalIsUserDetails() {
        // Arrange
        org.springframework.security.core.userdetails.UserDetails userDetails = 
                org.springframework.security.core.userdetails.User.builder()
                        .username("test@staff.alquds.edu")
                        .password("password")
                        .authorities("ROLE_PROFESSOR")
                        .build();
        
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        Authentication authWithUserDetails = mock(Authentication.class);
        when(authWithUserDetails.isAuthenticated()).thenReturn(true);
        when(authWithUserDetails.getPrincipal()).thenReturn(userDetails);
        realContext.setAuthentication(authWithUserDetails);
        SecurityContextHolder.setContext(realContext);
        
        when(userRepository.findByEmailWithDepartment("test@staff.alquds.edu"))
                .thenReturn(Optional.of(testUser));
        
        // Act
        User result = authService.getCurrentUser();
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@staff.alquds.edu");
        verify(userRepository).findByEmailWithDepartment("test@staff.alquds.edu");
    }
    
    @Test
    @DisplayName("Should get current user when principal is String email")
    void shouldGetCurrentUserWhenPrincipalIsStringEmail() {
        // Arrange
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        Authentication authWithString = mock(Authentication.class);
        when(authWithString.isAuthenticated()).thenReturn(true);
        when(authWithString.getPrincipal()).thenReturn("test@staff.alquds.edu");
        when(authWithString.getName()).thenReturn("test@staff.alquds.edu");
        realContext.setAuthentication(authWithString);
        SecurityContextHolder.setContext(realContext);
        
        when(userRepository.findByEmailWithDepartment("test@staff.alquds.edu"))
                .thenReturn(Optional.of(testUser));
        
        // Act
        User result = authService.getCurrentUser();
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@staff.alquds.edu");
        verify(userRepository).findByEmailWithDepartment("test@staff.alquds.edu");
    }
    
    @Test
    @DisplayName("Should throw exception when authentication is not authenticated")
    void shouldThrowExceptionWhenAuthenticationIsNotAuthenticated() {
        // Arrange
        SecurityContext realContext = SecurityContextHolder.createEmptyContext();
        when(authentication.isAuthenticated()).thenReturn(false);
        realContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(realContext);
        
        // Act & Assert
        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No authentication found");
    }
    
    // ==================== Token Validation Tests ====================
    
    @Test
    @DisplayName("Should validate token successfully")
    void shouldValidateTokenSuccessfully() {
        // Arrange
        String token = "valid-token";
        JwtService.TokenValidationResult validationResult = 
                new JwtService.TokenValidationResult(true, "VALID", "test@staff.alquds.edu", new java.util.Date());
        
        when(jwtService.validateTokenWithDetails(token)).thenReturn(validationResult);
        
        // Act
        JwtService.TokenValidationResult result = authService.validateToken(token);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.valid()).isTrue();
        assertThat(result.status()).isEqualTo("VALID");
        verify(jwtService).validateTokenWithDetails(token);
    }
}
