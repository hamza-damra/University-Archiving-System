package com.alquds.edu.ArchiveSystem.service.auth;

import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.RefreshToken;
import com.alquds.edu.ArchiveSystem.repository.auth.RefreshTokenRepository;

import com.alquds.edu.ArchiveSystem.exception.auth.TokenRefreshException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing refresh tokens.
 * Handles creation, validation, and revocation of refresh tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class RefreshTokenService {
    
    @Value("${jwt.refresh-expiration:604800000}") // Default 7 days in milliseconds
    private long refreshTokenDurationMs;
    
    @Value("${jwt.max-refresh-tokens-per-user:5}") // Maximum active refresh tokens per user
    private int maxRefreshTokensPerUser;
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    
    /**
     * Create a new refresh token for a user
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId, String deviceInfo) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Check if user has too many active tokens
        long activeTokens = refreshTokenRepository.countActiveTokensByUserId(userId, Instant.now());
        if (activeTokens >= maxRefreshTokensPerUser) {
            // Revoke oldest tokens to make room
            log.info("User {} has {} active tokens, revoking all to create new one", userId, activeTokens);
            refreshTokenRepository.revokeAllUserTokens(userId);
        }
        
        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .deviceInfo(deviceInfo)
                .revoked(false)
                .build();
        
        RefreshToken savedToken = refreshTokenRepository.save(newToken);
        log.info("Created refresh token for user: {}", user.getEmail());
        
        return savedToken;
    }
    
    /**
     * Find a refresh token by its token string
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    /**
     * Verify if a refresh token is valid
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }
        
        if (token.isRevoked()) {
            throw new TokenRefreshException(token.getToken(), "Refresh token was revoked. Please make a new signin request");
        }
        
        return token;
    }
    
    /**
     * Revoke a refresh token
     */
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.revokeToken(token);
        log.info("Revoked refresh token");
    }
    
    /**
     * Revoke all refresh tokens for a user
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId);
        log.info("Revoked all refresh tokens for user: {}", userId);
    }
    
    /**
     * Check if a token is valid
     */
    public boolean isTokenValid(String token) {
        return refreshTokenRepository.isTokenValid(token, Instant.now());
    }
    
    /**
     * Scheduled cleanup of expired tokens - runs daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        int deletedExpired = refreshTokenRepository.deleteExpiredTokens(Instant.now());
        int deletedRevoked = refreshTokenRepository.deleteRevokedTokens();
        log.info("Cleaned up {} expired and {} revoked refresh tokens", deletedExpired, deletedRevoked);
    }
}
