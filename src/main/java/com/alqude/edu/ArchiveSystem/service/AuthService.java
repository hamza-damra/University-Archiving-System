package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.auth.JwtResponse;
import com.alqude.edu.ArchiveSystem.dto.auth.LoginRequest;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    
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
     * @return JWT response with user details
     */
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
        String jwt = jwtService.generateToken(user);
        
        log.info("User logged in successfully: {} with new session: {}", user.getEmail(), newSessionId);
        
        return new JwtResponse(
                jwt,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getDepartment() != null ? user.getDepartment().getName() : null
        );
    }
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        
        if (authentication == null) {
            throw new RuntimeException("No authentication found");
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }
}
