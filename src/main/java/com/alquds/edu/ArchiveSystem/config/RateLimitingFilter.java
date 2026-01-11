package com.alquds.edu.ArchiveSystem.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiting filter.
 *
 * Protects against:
 * - Brute force login attacks
 * - API abuse
 * - Resource exhaustion (DoS)
 *
 * For production environments with multiple instances, consider using:
 * - Redis-based rate limiting
 * - Bucket4j with Redis
 * - API Gateway rate limiting
 *
 * @author Security Team
 * @since 2024
 */
@Component
@Order(1)
@Slf4j
public class RateLimitingFilter implements Filter {

    // Rate limit configurations
    @Value("${app.rate-limit.login.requests-per-minute:5}")
    private int loginRequestsPerMinute;

    @Value("${app.rate-limit.api.requests-per-minute:100}")
    private int apiRequestsPerMinute;

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    // In-memory storage for rate limiting (use Redis in production clusters)
    private final Map<String, RateLimitBucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, RateLimitBucket> apiBuckets = new ConcurrentHashMap<>();

    // Cleanup old entries every 5 minutes
    private long lastCleanup = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL = 5 * 60 * 1000; // 5 minutes

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!rateLimitEnabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIP(httpRequest);
        String path = httpRequest.getRequestURI();

        // Periodic cleanup of old entries
        cleanupIfNeeded();

        // Apply stricter rate limiting for login endpoints
        if (path.contains("/api/auth/login") || path.contains("/api/auth/refresh")) {
            RateLimitResult result = checkRateLimit(loginBuckets, clientIp, loginRequestsPerMinute);
            if (!result.allowed) {
                log.warn("Rate limit exceeded for login from IP: {}", clientIp);
                sendRateLimitResponse(httpResponse, "Too many login attempts. Please wait before trying again.", result.retryAfterSeconds);
                return;
            }
        }
        // Apply general rate limiting for all API endpoints
        else if (path.startsWith("/api/")) {
            RateLimitResult result = checkRateLimit(apiBuckets, clientIp, apiRequestsPerMinute);
            if (!result.allowed) {
                log.warn("Rate limit exceeded for API from IP: {}", clientIp);
                sendRateLimitResponse(httpResponse, "Too many requests. Please slow down.", result.retryAfterSeconds);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Check if the request is within rate limits.
     * Returns a result object containing whether the request is allowed and the retry-after time.
     */
    private RateLimitResult checkRateLimit(Map<String, RateLimitBucket> buckets, String clientIp, int maxRequests) {
        long currentMinute = System.currentTimeMillis() / 60000;
        long currentTimeMs = System.currentTimeMillis();

        RateLimitBucket bucket = buckets.compute(clientIp, (key, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                return new RateLimitBucket(currentMinute);
            }
            return existing;
        });

        boolean allowed = bucket.incrementAndCheck(maxRequests);
        
        // Calculate seconds until the next minute (when the rate limit resets)
        int retryAfterSeconds = 0;
        if (!allowed) {
            long nextMinuteMs = (currentMinute + 1) * 60000;
            retryAfterSeconds = (int) Math.ceil((nextMinuteMs - currentTimeMs) / 1000.0);
            // Ensure at least 1 second
            retryAfterSeconds = Math.max(1, retryAfterSeconds);
        }
        
        return new RateLimitResult(allowed, retryAfterSeconds);
    }

    /**
     * Get client IP address, considering proxies.
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    /**
     * Send rate limit exceeded response.
     */
    private void sendRateLimitResponse(HttpServletResponse response, String message, int retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        String jsonResponse = String.format(
            "{\"success\":false,\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"%s\",\"retryAfterSeconds\":%d},\"timestamp\":%d}",
            message, retryAfterSeconds, System.currentTimeMillis()
        );

        response.getWriter().write(jsonResponse);
    }

    /**
     * Cleanup old bucket entries to prevent memory leaks.
     */
    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL) {
            lastCleanup = now;
            long currentMinute = now / 60000;

            loginBuckets.entrySet().removeIf(entry -> entry.getValue().minute < currentMinute - 1);
            apiBuckets.entrySet().removeIf(entry -> entry.getValue().minute < currentMinute - 1);

            log.debug("Rate limit buckets cleaned up. Login: {}, API: {}",
                loginBuckets.size(), apiBuckets.size());
        }
    }

    /**
     * Simple rate limit bucket for counting requests per minute.
     */
    private static class RateLimitBucket {
        final long minute;
        final AtomicInteger count = new AtomicInteger(0);

        RateLimitBucket(long minute) {
            this.minute = minute;
        }

        boolean incrementAndCheck(int maxRequests) {
            return count.incrementAndGet() <= maxRequests;
        }
    }
    
    /**
     * Result object for rate limit check.
     */
    private static class RateLimitResult {
        final boolean allowed;
        final int retryAfterSeconds;
        
        RateLimitResult(boolean allowed, int retryAfterSeconds) {
            this.allowed = allowed;
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }
}

