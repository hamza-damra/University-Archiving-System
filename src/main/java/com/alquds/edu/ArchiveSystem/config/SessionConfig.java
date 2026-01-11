package com.alquds.edu.ArchiveSystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import javax.sql.DataSource;

/**
 * Configuration for Spring Session JDBC.
 * 
 * Provides database-backed HTTP session management with the following features:
 * - Sessions persisted to MySQL database
 * - Survives application restarts
 * - Supports horizontal scaling (multiple instances share session state)
 * - Secure cookie configuration
 * - Automatic cleanup of expired sessions
 * 
 * @author Archive System Team
 */
@Configuration
@EnableJdbcHttpSession(
    tableName = "SPRING_SESSION",
    maxInactiveIntervalInSeconds = 1800  // 30 minutes default
)
public class SessionConfig {

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${server.servlet.session.cookie.same-site:Lax}")
    private String sameSite;

    /**
     * Configures the session cookie serializer with secure settings.
     * 
     * Security features:
     * - Custom cookie name to avoid default JSESSIONID
     * - HttpOnly flag prevents XSS attacks
     * - Secure flag ensures HTTPS-only transmission (in production)
     * - SameSite attribute prevents CSRF attacks
     * - Root path makes cookie available to entire application
     * 
     * @return configured cookie serializer
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        
        // Custom cookie name (not the default JSESSIONID)
        serializer.setCookieName("ARCHIVESESSION");
        
        // Path where cookie is valid (root = entire application)
        serializer.setCookiePath("/");
        
        // HttpOnly prevents JavaScript access (XSS protection)
        serializer.setUseHttpOnlyCookie(true);
        
        // Secure flag - only transmit over HTTPS (false in dev, true in prod)
        serializer.setUseSecureCookie(secureCookie);
        
        // SameSite attribute prevents CSRF
        // Lax: allows some cross-site usage (e.g., top-level navigation)
        // Strict: no cross-site usage at all (more secure but may break some UX)
        serializer.setSameSite(sameSite);
        
        // Don't use Base64 encoding for cookie value
        serializer.setUseBase64Encoding(false);
        
        return serializer;
    }

    /**
     * Creates a JdbcTemplate bean for session repository operations.
     * This enables Spring Session to interact with the database.
     * 
     * @param dataSource the application's data source
     * @return configured JDBC template
     */
    @Bean
    public JdbcTemplate sessionJdbcTemplate(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource cannot be null");
        }
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setFetchSize(100);
        return jdbcTemplate;
    }
}
