package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.auth.JwtResponse;
import com.alqude.edu.ArchiveSystem.dto.auth.LoginRequest;
import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());
        
        try {
            JwtResponse jwtResponse = authService.login(loginRequest);
            return ResponseEntity.ok(ApiResponse.success("Login successful", jwtResponse));
        } catch (Exception e) {
            log.error("Login failed for email: {}", loginRequest.getEmail(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid email or password"));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        // In a stateless JWT implementation, logout is typically handled client-side
        // by removing the token from storage
        return ResponseEntity.ok(ApiResponse.success("Logout successful", "Token invalidated"));
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<JwtResponse>> getCurrentUser() {
        try {
            var currentUser = authService.getCurrentUser();
            JwtResponse response = new JwtResponse(
                    null, // No new token needed
                    currentUser.getId(),
                    currentUser.getEmail(),
                    currentUser.getFirstName(),
                    currentUser.getLastName(),
                    currentUser.getRole(),
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
