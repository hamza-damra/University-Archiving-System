package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.dto.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for session management operations.
 * 
 * Provides endpoints to:
 * - Check session status
 * - Get session information
 * - Invalidate sessions
 * - View session statistics (admin only)
 * 
 * @author Archive System Team
 */
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
@Slf4j
// SECURITY: Removed @CrossOrigin(origins = "*") - using centralized CORS configuration instead
public class SessionController {

    private final JdbcIndexedSessionRepository sessionRepository;

    /**
     * Get current session information.
     * 
     * @param request HTTP request
     * @return session details
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionInfo(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        
        Map<String, Object> sessionInfo = new HashMap<>();
        
        if (session == null) {
            sessionInfo.put("active", false);
            sessionInfo.put("message", "No active session");
            return ResponseEntity.ok(ApiResponse.success("Session information retrieved", sessionInfo));
        }
        
        sessionInfo.put("active", true);
        sessionInfo.put("sessionId", session.getId());
        sessionInfo.put("creationTime", Instant.ofEpochMilli(session.getCreationTime()));
        sessionInfo.put("lastAccessedTime", Instant.ofEpochMilli(session.getLastAccessedTime()));
        sessionInfo.put("maxInactiveInterval", session.getMaxInactiveInterval());
        sessionInfo.put("isNew", session.isNew());
        
        // Calculate time until expiration
        long currentTime = System.currentTimeMillis();
        long lastAccessed = session.getLastAccessedTime();
        int maxInactive = session.getMaxInactiveInterval();
        long expiresIn = (lastAccessed + (maxInactive * 1000L)) - currentTime;
        sessionInfo.put("expiresInSeconds", expiresIn / 1000);
        
        log.debug("Session info retrieved for session: {}", session.getId());
        
        return ResponseEntity.ok(ApiResponse.success("Session information retrieved", sessionInfo));
    }

    /**
     * Check if session is active.
     * 
     * @param request HTTP request
     * @return session status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getSessionStatus(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        
        Map<String, Boolean> status = new HashMap<>();
        status.put("active", session != null);
        
        return ResponseEntity.ok(ApiResponse.success("Session status checked", status));
    }

    /**
     * Refresh/extend the current session.
     * 
     * @param request HTTP request
     * @return confirmation
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        
        if (session == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No active session to refresh"));
        }
        
        // Accessing the session automatically updates the last accessed time
        session.setAttribute("lastRefresh", System.currentTimeMillis());
        
        log.info("Session refreshed: {}", session.getId());
        
        return ResponseEntity.ok(ApiResponse.success(
                "Session refreshed successfully", 
                "Session expires in " + session.getMaxInactiveInterval() + " seconds"
        ));
    }

    /**
     * Invalidate current session (logout).
     * 
     * @param request HTTP request
     * @return confirmation
     */
    @PostMapping("/invalidate")
    public ResponseEntity<ApiResponse<String>> invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        
        if (session == null) {
            return ResponseEntity.ok(ApiResponse.success("No active session", "Already logged out"));
        }
        
        String sessionId = session.getId();
        session.invalidate();
        
        log.info("Session manually invalidated: {}", sessionId);
        
        return ResponseEntity.ok(ApiResponse.success("Session invalidated", "Logged out successfully"));
    }

    /**
     * Get session statistics (HOD only).
     * 
     * @return session statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Note: Spring Session JDBC doesn't provide easy count methods
            // This is a simplified version - you might want to add custom queries
            stats.put("message", "Session statistics available in database");
            stats.put("table", "SPRING_SESSION");
            stats.put("hint", "Query SPRING_SESSION table for detailed statistics");
            
            return ResponseEntity.ok(ApiResponse.success("Session statistics retrieved", stats));
        } catch (Exception e) {
            log.error("Error retrieving session statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve session statistics"));
        }
    }

    /**
     * Health check endpoint for session repository.
     * 
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        
        try {
            // Simple check - if repository is injected, it's healthy
            health.put("status", "UP");
            health.put("sessionStore", "JDBC");
            health.put("repository", sessionRepository.getClass().getSimpleName());
            
            return ResponseEntity.ok(ApiResponse.success("Session repository is healthy", health));
        } catch (Exception e) {
            log.error("Session repository health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Session repository health check failed"));
        }
    }
}
