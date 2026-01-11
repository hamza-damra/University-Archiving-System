package com.alquds.edu.ArchiveSystem.controller.api;

import com.alquds.edu.ArchiveSystem.entity.user.User;

import com.alquds.edu.ArchiveSystem.dto.auth.JwtResponse;
import com.alquds.edu.ArchiveSystem.dto.auth.LoginRequest;
import com.alquds.edu.ArchiveSystem.dto.auth.TokenRefreshRequest;
import com.alquds.edu.ArchiveSystem.dto.auth.TokenRefreshResponse;
import com.alquds.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alquds.edu.ArchiveSystem.dto.common.ErrorResponse;
import com.alquds.edu.ArchiveSystem.service.auth.AuthService;
import com.alquds.edu.ArchiveSystem.service.auth.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
// SECURITY: Removed @CrossOrigin(origins = "*") - using centralized CORS configuration instead
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            jakarta.servlet.http.HttpServletRequest request) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());
        
        try {
            JwtResponse jwtResponse = authService.login(loginRequest, request);
            return ResponseEntity.ok(ApiResponse.success("Login successful", jwtResponse));
        } catch (DisabledException e) {
            log.warn("Login attempt for disabled account: {}", loginRequest.getEmail());
            ErrorResponse errorResponse = ErrorResponse.of("ACCOUNT_DISABLED", "Your account has been deactivated. Please contact support.")
                    .withStatus(HttpStatus.FORBIDDEN.value())
                    .withSuggestions(List.of("Contact your administrator to reactivate your account", "Your account may have been disabled by an administrator"));
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(errorResponse));
        } catch (LockedException e) {
            log.warn("Login attempt for locked account: {}", loginRequest.getEmail());
            ErrorResponse errorResponse = ErrorResponse.of("ACCOUNT_LOCKED", "Your account has been locked. Please contact support.")
                    .withStatus(HttpStatus.FORBIDDEN.value())
                    .withSuggestions(List.of("Contact your administrator to unlock your account", "Your account may have been locked due to security reasons"));
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(errorResponse));
        } catch (Exception e) {
            log.error("Login failed for email: {}", loginRequest.getEmail(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid email or password"));
        }
    }
    
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        log.info("Token refresh attempt");
        
        try {
            TokenRefreshResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Failed to refresh token: " + e.getMessage()));
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.debug("Token validation request");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, Object> result = new HashMap<>();
            result.put("valid", false);
            result.put("status", "NO_TOKEN");
            result.put("message", "No token provided");
            return ResponseEntity.ok(ApiResponse.success("Token validation result", result));
        }
        
        String token = authHeader.substring(7);
        JwtService.TokenValidationResult validationResult = authService.validateToken(token);
        
        Map<String, Object> result = new HashMap<>();
        result.put("valid", validationResult.valid());
        result.put("status", validationResult.status());
        result.put("username", validationResult.username());
        
        if (validationResult.expiration() != null) {
            result.put("expiration", validationResult.expiration().getTime());
            result.put("remainingMs", validationResult.expiration().getTime() - System.currentTimeMillis());
        }
        
        return ResponseEntity.ok(ApiResponse.success("Token validation result", result));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestBody(required = false) TokenRefreshRequest refreshTokenRequest,
            jakarta.servlet.http.HttpServletRequest request) {
        log.info("Logout request");
        
        try {
            // If refresh token is provided, revoke just that token
            if (refreshTokenRequest != null && refreshTokenRequest.getRefreshToken() != null) {
                authService.logoutWithToken(refreshTokenRequest.getRefreshToken(), request);
            } else {
                // Try to get current user and revoke all their tokens
                try {
                    User currentUser = authService.getCurrentUser();
                    authService.logout(currentUser.getId(), request);
                } catch (Exception e) {
                    // If no user authenticated, just invalidate session
                    jakarta.servlet.http.HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    org.springframework.security.core.context.SecurityContextHolder.clearContext();
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success("Logout successful", "Session invalidated"));
        } catch (Exception e) {
            log.error("Logout error", e);
            // Still return success as the user wants to logout
            return ResponseEntity.ok(ApiResponse.success("Logout completed", "Session cleared"));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<JwtResponse>> getCurrentUser() {
        try {
            var currentUser = authService.getCurrentUser();
            JwtResponse response = new JwtResponse(
                    null, // No new token needed
                    null, // No refresh token needed
                    currentUser.getId(),
                    currentUser.getEmail(),
                    currentUser.getFirstName(),
                    currentUser.getLastName(),
                    currentUser.getRole(),
                    currentUser.getDepartment() != null ? currentUser.getDepartment().getId() : null,
                    currentUser.getDepartment() != null ? currentUser.getDepartment().getName() : null
            );
            return ResponseEntity.ok(ApiResponse.success("User information retrieved", response));
        } catch (Exception e) {
            log.error("Error retrieving current user", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unable to retrieve user information"));
        }
    }
}
