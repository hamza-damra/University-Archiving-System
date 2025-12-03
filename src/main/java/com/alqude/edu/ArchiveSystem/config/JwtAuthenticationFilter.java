package com.alqude.edu.ArchiveSystem.config;

import com.alqude.edu.ArchiveSystem.service.JwtService;
import com.alqude.edu.ArchiveSystem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        
        // Skip if no Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        jwt = authHeader.substring(7);
        
        try {
            userEmail = jwtService.extractUsername(jwt);
            
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.loadUserByUsername(userEmail);
                
                if (jwtService.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            
            filterChain.doFilter(request, response);
            
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired for user: {}", e.getClaims().getSubject());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "TOKEN_EXPIRED", "Access token has expired. Please refresh your token.");
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "TOKEN_MALFORMED", "Invalid token format.");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "TOKEN_UNSUPPORTED", "Token type is not supported.");
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "TOKEN_INVALID_SIGNATURE", "Token signature is invalid.");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "TOKEN_EMPTY", "Token is empty or invalid.");
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * Send a JSON error response with detailed error information.
     * This helps the frontend distinguish between different token errors.
     */
    private void sendErrorResponse(HttpServletResponse response, int status, 
                                   String errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", Map.of(
                "code", errorCode,
                "message", message
        ));
        body.put("timestamp", System.currentTimeMillis());
        
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
