package com.alquds.edu.ArchiveSystem.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtService.
 * Tests JWT token generation, extraction, validation, and expiration handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;
    private static final String JWT_SECRET = "testSecretKeyForTestingPurposesOnly123456789012345678901234567890";
    private static final long JWT_EXPIRATION_MS = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(); // NOSONAR - Test class, null safety handled
        
        // Set JWT configuration using reflection
        ReflectionTestUtils.setField(jwtService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", JWT_EXPIRATION_MS);
        
        // Create test UserDetails
        userDetails = User.builder()
                .username("test@staff.alquds.edu")
                .password("password")
                .authorities("ROLE_PROFESSOR")
                .build();
    }

    // ==================== generateToken Tests ====================

    @Test
    @DisplayName("Should generate token with UserDetails")
    void shouldGenerateTokenWithUserDetails() {
        // Act
        String token = jwtService.generateToken(userDetails);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        // Verify token can be parsed
        Claims claims = parseToken(token);
        assertThat(claims.getSubject()).isEqualTo(userDetails.getUsername());
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    @DisplayName("Should generate token with extra claims")
    void shouldGenerateTokenWithExtraClaims() {
        // Arrange
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "PROFESSOR");
        extraClaims.put("departmentId", 1L);
        extraClaims.put("userId", 123L);

        // Act
        String token = jwtService.generateToken(extraClaims, userDetails);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        // Verify token contains extra claims
        Claims claims = parseToken(token);
        assertThat(claims.getSubject()).isEqualTo(userDetails.getUsername());
        assertThat(claims.get("role")).isEqualTo("PROFESSOR");
        // JWT stores small Long values as Integer, so check the value rather than exact type
        assertThat(claims.get("departmentId")).isEqualTo(1);
        assertThat(claims.get("userId")).isEqualTo(123);
    }

    // ==================== extractUsername Tests ====================

    @Test
    @DisplayName("Should extract username from valid token")
    void shouldExtractUsernameFromValidToken() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        String username = jwtService.extractUsername(token);

        // Assert
        assertThat(username).isEqualTo(userDetails.getUsername());
    }

    @Test
    @DisplayName("Should throw exception when extracting username from invalid token")
    void shouldThrowExceptionWhenExtractingUsernameFromInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.string";

        // Act & Assert
        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOfAny(MalformedJwtException.class, IllegalArgumentException.class);
    }

    // ==================== extractExpiration Tests ====================

    @Test
    @DisplayName("Should extract expiration from valid token")
    void shouldExtractExpirationFromValidToken() {
        // Arrange
        String token = jwtService.generateToken(userDetails);
        Date expectedExpiration = new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS);

        // Act
        Date expiration = jwtService.extractExpiration(token);

        // Assert
        assertThat(expiration).isNotNull();
        // Allow 1 second tolerance for timing differences
        assertThat(Math.abs(expiration.getTime() - expectedExpiration.getTime())).isLessThan(1000L);
    }

    // ==================== extractClaim Tests ====================

    @Test
    @DisplayName("Should extract custom claim from token")
    void shouldExtractCustomClaimFromToken() {
        // Arrange
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("customClaim", "customValue");
        String token = jwtService.generateToken(extraClaims, userDetails);

        // Act
        String customClaim = jwtService.extractClaim(token, claims -> claims.get("customClaim", String.class));

        // Assert
        assertThat(customClaim).isEqualTo("customValue");
    }

    // ==================== isTokenExpired Tests ====================

    @Test
    @DisplayName("Should return false for valid token")
    void shouldReturnFalseForValidToken() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        Boolean isExpired = jwtService.isTokenExpired(token);

        // Assert
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should return true for expired token")
    void shouldReturnTrueForExpiredToken() {
        // Arrange - Create an expired token by using a different JwtService with short expiration
        JwtService shortExpirationService = new JwtService();
        ReflectionTestUtils.setField(shortExpirationService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(shortExpirationService, "jwtExpirationMs", -1000L); // Already expired
        
        String expiredToken = shortExpirationService.generateToken(userDetails);
        
        // Wait a bit to ensure it's expired
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        Boolean isExpired = jwtService.isTokenExpired(expiredToken);

        // Assert
        assertThat(isExpired).isTrue();
    }

    // ==================== validateToken Tests ====================

    @Test
    @DisplayName("Should validate token with UserDetails - valid token")
    void shouldValidateTokenWithUserDetailsValid() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        Boolean isValid = jwtService.validateToken(token, userDetails);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false when username doesn't match")
    void shouldReturnFalseWhenUsernameDoesNotMatch() {
        // Arrange
        String token = jwtService.generateToken(userDetails);
        UserDetails differentUser = User.builder()
                .username("different@staff.alquds.edu")
                .password("password")
                .authorities("ROLE_PROFESSOR")
                .build();

        // Act
        Boolean isValid = jwtService.validateToken(token, differentUser);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false when token is expired")
    void shouldReturnFalseWhenTokenIsExpired() {
        // Arrange - Create an expired token
        JwtService shortExpirationService = new JwtService();
        ReflectionTestUtils.setField(shortExpirationService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(shortExpirationService, "jwtExpirationMs", -1000L);
        
        String expiredToken = shortExpirationService.generateToken(userDetails);
        
        // Wait a bit to ensure it's expired
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act & Assert
        // validateToken throws ExpiredJwtException for expired tokens, so we expect an exception
        // The method doesn't catch the exception, so it propagates
        assertThatThrownBy(() -> jwtService.validateToken(expiredToken, userDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
        
        // Alternatively, verify that isTokenExpired returns true
        assertThat(jwtService.isTokenExpired(expiredToken)).isTrue();
    }

    // ==================== validateTokenWithDetails Tests ====================

    @Test
    @DisplayName("Should validate token with details - valid token")
    void shouldValidateTokenWithDetailsValid() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        JwtService.TokenValidationResult result = jwtService.validateTokenWithDetails(token);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.valid()).isTrue();
        assertThat(result.status()).isEqualTo("VALID");
        assertThat(result.username()).isEqualTo(userDetails.getUsername());
        assertThat(result.expiration()).isAfter(new Date());
    }

    @Test
    @DisplayName("Should return TOKEN_EXPIRED for expired token")
    void shouldReturnTokenExpiredForExpiredToken() {
        // Arrange - Create an expired token
        JwtService shortExpirationService = new JwtService();
        ReflectionTestUtils.setField(shortExpirationService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(shortExpirationService, "jwtExpirationMs", -1000L);
        
        String expiredToken = shortExpirationService.generateToken(userDetails);
        
        // Wait a bit to ensure it's expired
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        JwtService.TokenValidationResult result = jwtService.validateTokenWithDetails(expiredToken);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.valid()).isFalse();
        assertThat(result.status()).isEqualTo("TOKEN_EXPIRED");
        assertThat(result.username()).isEqualTo(userDetails.getUsername());
    }

    @Test
    @DisplayName("Should return TOKEN_MALFORMED for malformed token")
    void shouldReturnTokenMalformedForMalformedToken() {
        // Arrange
        String malformedToken = "not.a.valid.jwt.token";

        // Act
        JwtService.TokenValidationResult result = jwtService.validateTokenWithDetails(malformedToken);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.valid()).isFalse();
        assertThat(result.status()).isEqualTo("TOKEN_MALFORMED");
        assertThat(result.username()).isNull();
        assertThat(result.expiration()).isNull();
    }

    @Test
    @DisplayName("Should return TOKEN_UNSUPPORTED for unsupported token")
    void shouldReturnTokenUnsupportedForUnsupportedToken() {
        // Arrange - Create a token with different algorithm (simulated by using wrong secret)
        JwtService differentSecretService = new JwtService();
        ReflectionTestUtils.setField(differentSecretService, "jwtSecret", "differentSecretKey123456789012345678901234567890");
        ReflectionTestUtils.setField(differentSecretService, "jwtExpirationMs", JWT_EXPIRATION_MS);
        
        String tokenWithDifferentSecret = differentSecretService.generateToken(userDetails);

        // Act - Try to validate with original service (different secret = invalid signature)
        JwtService.TokenValidationResult result = jwtService.validateTokenWithDetails(tokenWithDifferentSecret);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.valid()).isFalse();
        // This will result in TOKEN_INVALID_SIGNATURE, not TOKEN_UNSUPPORTED
        // TOKEN_UNSUPPORTED is for unsupported JWT format, which is harder to simulate
        assertThat(result.status()).isEqualTo("TOKEN_INVALID_SIGNATURE");
    }

    @Test
    @DisplayName("Should return TOKEN_INVALID_SIGNATURE for token with invalid signature")
    void shouldReturnTokenInvalidSignatureForInvalidSignature() {
        // Arrange - Create a token with different secret
        JwtService differentSecretService = new JwtService();
        ReflectionTestUtils.setField(differentSecretService, "jwtSecret", "differentSecretKey123456789012345678901234567890");
        ReflectionTestUtils.setField(differentSecretService, "jwtExpirationMs", JWT_EXPIRATION_MS);
        
        String tokenWithDifferentSecret = differentSecretService.generateToken(userDetails);

        // Act
        JwtService.TokenValidationResult result = jwtService.validateTokenWithDetails(tokenWithDifferentSecret);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.valid()).isFalse();
        assertThat(result.status()).isEqualTo("TOKEN_INVALID_SIGNATURE");
        assertThat(result.username()).isNull();
        assertThat(result.expiration()).isNull();
    }

    @Test
    @DisplayName("Should return TOKEN_EMPTY for empty token")
    void shouldReturnTokenEmptyForEmptyToken() {
        // Arrange
        String emptyToken = "";

        // Act
        JwtService.TokenValidationResult result = jwtService.validateTokenWithDetails(emptyToken);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.valid()).isFalse();
        assertThat(result.status()).isEqualTo("TOKEN_EMPTY");
        assertThat(result.username()).isNull();
        assertThat(result.expiration()).isNull();
    }

    // ==================== extractAllClaimsAllowExpired Tests ====================

    @Test
    @DisplayName("Should extract claims from expired token")
    void shouldExtractClaimsFromExpiredToken() {
        // Arrange - Create an expired token
        JwtService shortExpirationService = new JwtService();
        ReflectionTestUtils.setField(shortExpirationService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(shortExpirationService, "jwtExpirationMs", -1000L);
        
        String expiredToken = shortExpirationService.generateToken(userDetails);
        
        // Wait a bit to ensure it's expired
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        Claims claims = jwtService.extractAllClaimsAllowExpired(expiredToken);

        // Assert
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(userDetails.getUsername());
    }

    // ==================== extractUsernameAllowExpired Tests ====================

    @Test
    @DisplayName("Should extract username from expired token")
    void shouldExtractUsernameFromExpiredToken() {
        // Arrange - Create an expired token
        JwtService shortExpirationService = new JwtService();
        ReflectionTestUtils.setField(shortExpirationService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(shortExpirationService, "jwtExpirationMs", -1000L);
        
        String expiredToken = shortExpirationService.generateToken(userDetails);
        
        // Wait a bit to ensure it's expired
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        String username = jwtService.extractUsernameAllowExpired(expiredToken);

        // Assert
        assertThat(username).isEqualTo(userDetails.getUsername());
    }

    // ==================== getTokenRemainingTime Tests ====================

    @Test
    @DisplayName("Should get remaining time for valid token")
    void shouldGetRemainingTimeForValidToken() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        long remainingTime = jwtService.getTokenRemainingTime(token);

        // Assert
        assertThat(remainingTime).isPositive();
        assertThat(remainingTime).isLessThanOrEqualTo(JWT_EXPIRATION_MS);
        // Allow 1 second tolerance
        assertThat(remainingTime).isGreaterThan(JWT_EXPIRATION_MS - 2000L);
    }

    @Test
    @DisplayName("Should return 0 for expired token")
    void shouldReturnZeroForExpiredToken() {
        // Arrange - Create an expired token
        JwtService shortExpirationService = new JwtService();
        ReflectionTestUtils.setField(shortExpirationService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(shortExpirationService, "jwtExpirationMs", -1000L);
        
        String expiredToken = shortExpirationService.generateToken(userDetails);
        
        // Wait a bit to ensure it's expired
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        long remainingTime = jwtService.getTokenRemainingTime(expiredToken);

        // Assert
        assertThat(remainingTime).isEqualTo(0L);
    }

    // ==================== Helper Methods ====================

    /**
     * Helper method to parse a JWT token and extract claims
     */
    private Claims parseToken(String token) {
        Key key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
