package com.alquds.edu.ArchiveSystem.service.auth;

import com.alquds.edu.ArchiveSystem.entity.auth.RefreshToken;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.exception.auth.TokenRefreshException;
import com.alquds.edu.ArchiveSystem.repository.auth.RefreshTokenRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefreshTokenService.
 * Tests token creation, validation, revocation, and cleanup operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Unit Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken testToken;
    private static final long REFRESH_TOKEN_DURATION_MS = 604800000L; // 7 days
    private static final int MAX_REFRESH_TOKENS_PER_USER = 5;

    @BeforeEach
    void setUp() {
        // Set up configuration values using reflection
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", REFRESH_TOKEN_DURATION_MS);
        ReflectionTestUtils.setField(refreshTokenService, "maxRefreshTokensPerUser", MAX_REFRESH_TOKENS_PER_USER);

        // Set up test data
        testUser = TestDataBuilder.createProfessorUser();
        testUser.setId(1L);
        testUser.setEmail("test@staff.alquds.edu");

        testToken = RefreshToken.builder()
                .id(1L)
                .token("test-refresh-token-123")
                .user(testUser)
                .expiryDate(Instant.now().plusMillis(REFRESH_TOKEN_DURATION_MS))
                .deviceInfo("Test Device")
                .revoked(false)
                .build();
    }

    // ==================== createRefreshToken Tests ====================

    @Test
    @DisplayName("Should create refresh token successfully")
    void shouldCreateRefreshTokenSuccessfully() {
        // Arrange
        Long userId = 1L;
        String deviceInfo = "Test Device";
        Instant beforeCreation = Instant.now();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.countActiveTokensByUserId(eq(userId), any(Instant.class))).thenReturn(0L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        RefreshToken result = refreshTokenService.createRefreshToken(userId, deviceInfo);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getDeviceInfo()).isEqualTo(deviceInfo);
        assertThat(result.isRevoked()).isFalse();
        assertThat(result.getExpiryDate()).isAfter(beforeCreation);
        assertThat(result.getToken()).isNotNull();

        verify(userRepository).findById(userId);
        verify(refreshTokenRepository).countActiveTokensByUserId(eq(userId), any(Instant.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).revokeAllUserTokens(anyLong());
    }

    @Test
    @DisplayName("Should revoke oldest tokens when user has max tokens")
    void shouldRevokeOldestTokensWhenUserHasMaxTokens() {
        // Arrange
        Long userId = 1L;
        String deviceInfo = "Test Device";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.countActiveTokensByUserId(eq(userId), any(Instant.class)))
                .thenReturn((long) MAX_REFRESH_TOKENS_PER_USER);
        doNothing().when(refreshTokenRepository).revokeAllUserTokens(userId);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        RefreshToken result = refreshTokenService.createRefreshToken(userId, deviceInfo);

        // Assert
        assertThat(result).isNotNull();
        verify(refreshTokenRepository).revokeAllUserTokens(userId);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        Long userId = 999L;
        String deviceInfo = "Test Device";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.createRefreshToken(userId, deviceInfo))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: " + userId);

        verify(userRepository).findById(userId);
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw exception when userId is null")
    void shouldThrowExceptionWhenUserIdIsNull() {
        // Arrange
        String deviceInfo = "Test Device";

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.createRefreshToken(null, deviceInfo))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("User ID cannot be null");

        verify(userRepository, never()).findById(anyLong());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    // ==================== findByToken Tests ====================

    @Test
    @DisplayName("Should find token by token string")
    void shouldFindTokenByTokenString() {
        // Arrange
        String tokenString = "test-refresh-token-123";
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(testToken));

        // Act
        Optional<RefreshToken> result = refreshTokenService.findByToken(tokenString);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testToken);
        verify(refreshTokenRepository).findByToken(tokenString);
    }

    @Test
    @DisplayName("Should return empty when token not found")
    void shouldReturnEmptyWhenTokenNotFound() {
        // Arrange
        String tokenString = "non-existent-token";
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

        // Act
        Optional<RefreshToken> result = refreshTokenService.findByToken(tokenString);

        // Assert
        assertThat(result).isEmpty();
        verify(refreshTokenRepository).findByToken(tokenString);
    }

    // ==================== verifyExpiration Tests ====================

    @Test
    @DisplayName("Should verify valid token successfully")
    void shouldVerifyValidTokenSuccessfully() {
        // Arrange
        RefreshToken validToken = RefreshToken.builder()
                .id(1L)
                .token("valid-token")
                .user(testUser)
                .expiryDate(Instant.now().plusMillis(REFRESH_TOKEN_DURATION_MS))
                .revoked(false)
                .build();

        // Act
        RefreshToken result = refreshTokenService.verifyExpiration(validToken);

        // Assert
        assertThat(result).isEqualTo(validToken);
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw exception when token is expired")
    void shouldThrowExceptionWhenTokenIsExpired() {
        // Arrange
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .token("expired-token")
                .user(testUser)
                .expiryDate(Instant.now().minusSeconds(3600)) // Expired 1 hour ago
                .revoked(false)
                .build();

        doNothing().when(refreshTokenRepository).delete(expiredToken);

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(expiredToken))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("Refresh token was expired");

        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    @DisplayName("Should throw exception when token is revoked")
    void shouldThrowExceptionWhenTokenIsRevoked() {
        // Arrange
        RefreshToken revokedToken = RefreshToken.builder()
                .id(1L)
                .token("revoked-token")
                .user(testUser)
                .expiryDate(Instant.now().plusMillis(REFRESH_TOKEN_DURATION_MS))
                .revoked(true)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(revokedToken))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("Refresh token was revoked");

        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    // ==================== revokeToken Tests ====================

    @Test
    @DisplayName("Should revoke token successfully")
    void shouldRevokeTokenSuccessfully() {
        // Arrange
        String tokenString = "test-refresh-token-123";
        doNothing().when(refreshTokenRepository).revokeToken(tokenString);

        // Act
        refreshTokenService.revokeToken(tokenString);

        // Assert
        verify(refreshTokenRepository).revokeToken(tokenString);
    }

    // ==================== revokeAllUserTokens Tests ====================

    @Test
    @DisplayName("Should revoke all user tokens successfully")
    void shouldRevokeAllUserTokensSuccessfully() {
        // Arrange
        Long userId = 1L;
        doNothing().when(refreshTokenRepository).revokeAllUserTokens(userId);

        // Act
        refreshTokenService.revokeAllUserTokens(userId);

        // Assert
        verify(refreshTokenRepository).revokeAllUserTokens(userId);
    }

    // ==================== isTokenValid Tests ====================

    @Test
    @DisplayName("Should return true for valid token")
    void shouldReturnTrueForValidToken() {
        // Arrange
        String tokenString = "valid-token";
        when(refreshTokenRepository.isTokenValid(eq(tokenString), any(Instant.class))).thenReturn(true);

        // Act
        boolean result = refreshTokenService.isTokenValid(tokenString);

        // Assert
        assertThat(result).isTrue();
        verify(refreshTokenRepository).isTokenValid(eq(tokenString), any(Instant.class));
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void shouldReturnFalseForInvalidToken() {
        // Arrange
        String tokenString = "invalid-token";
        when(refreshTokenRepository.isTokenValid(eq(tokenString), any(Instant.class))).thenReturn(false);

        // Act
        boolean result = refreshTokenService.isTokenValid(tokenString);

        // Assert
        assertThat(result).isFalse();
        verify(refreshTokenRepository).isTokenValid(eq(tokenString), any(Instant.class));
    }

    // ==================== cleanupExpiredTokens Tests ====================

    @Test
    @DisplayName("Should cleanup expired and revoked tokens")
    void shouldCleanupExpiredAndRevokedTokens() {
        // Arrange
        int deletedExpired = 5;
        int deletedRevoked = 3;
        when(refreshTokenRepository.deleteExpiredTokens(any(Instant.class))).thenReturn(deletedExpired);
        when(refreshTokenRepository.deleteRevokedTokens()).thenReturn(deletedRevoked);

        // Act
        refreshTokenService.cleanupExpiredTokens();

        // Assert
        verify(refreshTokenRepository).deleteExpiredTokens(any(Instant.class));
        verify(refreshTokenRepository).deleteRevokedTokens();
    }
}
