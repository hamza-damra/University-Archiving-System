package com.alquds.edu.ArchiveSystem.exception.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when token refresh fails.
 * This can happen when the refresh token is expired, revoked, or invalid.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenRefreshException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final String token;
    
    public TokenRefreshException(String token, String message) {
        super(String.format("Failed for [%s]: %s", token, message));
        this.token = token;
    }
    
    public TokenRefreshException(String message) {
        super(message);
        this.token = null;
    }
    
    public String getToken() {
        return token;
    }
}
