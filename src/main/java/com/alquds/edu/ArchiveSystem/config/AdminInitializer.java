package com.alquds.edu.ArchiveSystem.config;

import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializes a default admin account on application startup if none exists.
 * This ensures the system always has at least one admin user for management.
 * 
 * Runs after FlywayCleanupRunner (Order=1) to ensure database is ready.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Default admin credentials - CHANGE THESE IN PRODUCTION!
    private static final String DEFAULT_ADMIN_EMAIL = "admin@alquds.edu";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";
    private static final String DEFAULT_ADMIN_FIRST_NAME = "System";
    private static final String DEFAULT_ADMIN_LAST_NAME = "Administrator";

    @Override
    public void run(String... args) {
        initializeAdminUser();
    }

    @Transactional
    private void initializeAdminUser() {
        try {
            // Check if any admin user exists
            long adminCount = userRepository.countByRole(Role.ROLE_ADMIN);
            
            if (adminCount == 0) {
                log.info("No admin user found. Creating default admin account...");
                
                // Also check if the email already exists (edge case)
                if (userRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
                    log.warn("User with email {} already exists but is not an admin. Skipping admin creation.", 
                            DEFAULT_ADMIN_EMAIL);
                    return;
                }
                
                User admin = new User();
                admin.setEmail(DEFAULT_ADMIN_EMAIL);
                admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
                admin.setFirstName(DEFAULT_ADMIN_FIRST_NAME);
                admin.setLastName(DEFAULT_ADMIN_LAST_NAME);
                admin.setRole(Role.ROLE_ADMIN);
                admin.setIsActive(true);
                
                userRepository.save(admin);
                
                log.info("============================================");
                log.info("DEFAULT ADMIN ACCOUNT CREATED");
                log.info("Email: {}", DEFAULT_ADMIN_EMAIL);
                log.info("Password: {}", DEFAULT_ADMIN_PASSWORD);
                log.info("============================================");
                log.warn("IMPORTANT: Please change the default admin password after first login!");
                
            } else {
                log.info("Admin user(s) already exist. Skipping admin initialization.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize admin user: {}", e.getMessage(), e);
            // Don't throw - allow application to continue starting
            // The admin can be created manually later if needed
        }
    }
}
