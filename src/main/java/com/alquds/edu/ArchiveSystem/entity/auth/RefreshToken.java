package com.alquds.edu.ArchiveSystem.entity.auth;

import com.alquds.edu.ArchiveSystem.entity.user.User;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity for storing refresh tokens.
 * Refresh tokens are long-lived tokens used to obtain new access tokens
 * without requiring the user to re-authenticate.
 */
@Entity
@Table(name = "refresh_tokens",
       indexes = {
           @Index(name = "idx_refresh_token_user", columnList = "user_id"),
           @Index(name = "idx_refresh_token_expiry", columnList = "expiry_date")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Builder.Default
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;
    
    @Column(name = "device_info")
    private String deviceInfo;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    
    /**
     * Check if the refresh token is expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }
    
    /**
     * Check if the refresh token is valid (not expired and not revoked)
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }
}
