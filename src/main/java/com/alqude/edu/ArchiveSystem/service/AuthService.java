package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.auth.JwtResponse;
import com.alqude.edu.ArchiveSystem.dto.auth.LoginRequest;
import com.alqude.edu.ArchiveSystem.dto.auth.TokenRefreshRequest;
import com.alqude.edu.ArchiveSystem.dto.auth.TokenRefreshResponse;
import com.alqude.edu.ArchiveSystem.entity.RefreshToken;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.exception.TokenRefreshException;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    
    /**
     * Authenticates a user and performs session rotation for security.
     * 
     * Session Fixation Protection:
     * 1. Invalidates the old session (if exists)
     * 2. Creates a new session with a new session ID
     * 3. Stores authentication in the new session
     * 
     * This prevents session fixation attacks where an attacker tries to
     * use a pre-authenticated session ID.
     * 
     * @param loginRequest login credentials
     * @param request HTTP request to access session
     * @return JWT response with user details and refresh token
     */
    @Transactional
    public JwtResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        log.info("Attempting login for user: {}", loginRequest.getEmail());
        
        // Get old session before authentication (if exists)
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            String oldSessionId = oldSession.getId();
            log.debug("Invalidating old session: {}", oldSessionId);
            oldSession.invalidate();
        }
        
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Create new session (session rotation)
        HttpSession newSession = request.getSession(true);
        String newSessionId = newSession.getId();
        log.info("Created new session after authentication: {}", newSessionId);
        
        // Store authentication in new session
        newSession.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        
        User user = (User) authentication.getPrincipal();
        
        // Generate access token
        String jwt = jwtService.generateToken(user);
        
        // Generate refresh token
        String deviceInfo = extractDeviceInfo(request);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), deviceInfo);
        
        log.info("User logged in successfully: {} with new session: {}", user.getEmail(), newSessionId);
        
        return new JwtResponse(
                jwt,
                refreshToken.getToken(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getDepartment() != null ? user.getDepartment().getName() : null
        );
    }
    
    /**
     * Refresh the access token using a valid refresh token.
     * 
     * @param request TokenRefreshRequest containing the refresh token
     * @return TokenRefreshResponse with new access token
     */
    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtService.generateToken(user);
                    log.info("Access token refreshed for user: {}", user.getEmail());
                    return new TokenRefreshResponse(accessToken, requestRefreshToken);
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, 
                        "Refresh token is not in database!"));
    }
    
    /**
     * Validate the current access token and return validation status.
     * 
     * @param token JWT token to validate
     * @return TokenValidationResult with validation details
     */
    public JwtService.TokenValidationResult validateToken(String token) {
        return jwtService.validateTokenWithDetails(token);
    }
    
    /**
     * Logout user and revoke all their refresh tokens.
     * 
     * @param userId User ID
     * @param request HTTP request
     */
    @Transactional
    public void logout(Long userId, HttpServletRequest request) {
        // Revoke all refresh tokens for the user
        refreshTokenService.revokeAllUserTokens(userId);
        
        // Invalidate session
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("Invalidating session on logout: {}", session.getId());
            session.invalidate();
        }
        
        // Clear security context
        SecurityContextHolder.clearContext();
        
        log.info("User {} logged out successfully", userId);
    }
    
    /**
     * Logout user by revoking a specific refresh token.
     * 
     * @param refreshToken Refresh token to revoke
     * @param request HTTP request
     */
    @Transactional
    public void logoutWithToken(String refreshToken, HttpServletRequest request) {
        // Revoke the specific refresh token
        refreshTokenService.revokeToken(refreshToken);
        
        // Invalidate session
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("Invalidating session on logout");
            session.invalidate();
        }
        
        // Clear security context
        SecurityContextHolder.clearContext();
        
        log.info("User logged out successfully (token revoked)");
    }
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authentication found");
        }
        
        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = authentication.getName();
        }
        
        return userRepository.findByEmailWithDepartment(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }
    
    /**
     * Extract device information from request for refresh token tracking.
     */
    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = request.getRemoteAddr();
        return String.format("%s | %s", 
                userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 200)) : "Unknown",
                remoteAddr);
    }
}
