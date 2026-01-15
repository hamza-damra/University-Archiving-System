package com.alquds.edu.ArchiveSystem.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.http.SessionRepositoryFilter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Test configuration for unit and integration tests.
 * Provides test-specific beans and configurations.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {
    
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Provides an in-memory session repository for tests instead of JDBC.
     * This avoids the need for Spring Session JDBC tables in test database.
     */
    @Bean
    @Primary
    public MapSessionRepository sessionRepository() {
        return new MapSessionRepository(new ConcurrentHashMap<>());
    }
}
