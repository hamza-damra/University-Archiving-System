package com.alquds.edu.ArchiveSystem.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security Headers Configuration.
 * Adds essential security headers to all HTTP responses to protect against:
 * - XSS attacks (X-XSS-Protection, Content-Security-Policy)
 * - Clickjacking (X-Frame-Options)
 * - MIME type sniffing (X-Content-Type-Options)
 * - Referrer leakage (Referrer-Policy)
 * @author Security Team
 * @since 2024
 */
@Configuration
public class SecurityHeadersConfig implements WebMvcConfigurer {

    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String X_FRAME_OPTIONS = "X-Frame-Options";
    private static final String X_XSS_PROTECTION = "X-XSS-Protection";
    private static final String REFERRER_POLICY = "Referrer-Policy";
    private static final String PERMISSIONS_POLICY = "Permissions-Policy";
    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new SecurityHeadersInterceptor());
    }

    /**
     * Interceptor that adds security headers to all responses.
     */
    private static class SecurityHeadersInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                 @NonNull Object handler) {
            // Prevent MIME type sniffing
            response.setHeader(X_CONTENT_TYPE_OPTIONS, "nosniff");

            // Prevent clickjacking - page cannot be embedded in iframes
            response.setHeader(X_FRAME_OPTIONS, "DENY");

            // Enable XSS filter in browser (legacy browsers)
            response.setHeader(X_XSS_PROTECTION, "1; mode=block");

            // Control referrer information sent with requests
            response.setHeader(REFERRER_POLICY, "strict-origin-when-cross-origin");

            // Content Security Policy - adjust as needed for your frontend
            // This is a relatively permissive policy - tighten for production
            response.setHeader(CONTENT_SECURITY_POLICY,
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net https://cdn.tailwindcss.com; " +
                "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://fonts.googleapis.com https://cdn.tailwindcss.com; " +
                "font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com; " +
                "img-src 'self' data: blob:; " +
                "connect-src 'self'; " +
                "frame-src 'self' blob:; " +
                "object-src 'self' blob:; " +
                "frame-ancestors 'none';"
            );

            // Permissions Policy (formerly Feature-Policy)
            response.setHeader(PERMISSIONS_POLICY,
                "geolocation=(), microphone=(), camera=(), payment=()");

            // Prevent caching of sensitive data
            if (request.getRequestURI().contains("/api/")) {
                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
                response.setHeader(HttpHeaders.PRAGMA, "no-cache");
            }

            return true;
        }
    }
}
