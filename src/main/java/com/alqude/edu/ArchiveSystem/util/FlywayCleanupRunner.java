package com.alqude.edu.ArchiveSystem.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * ONE-TIME repair utility - DELETE THIS FILE after successful startup
 */
@Component
@Order(1)
public class FlywayCleanupRunner implements CommandLineRunner {

    private final DataSource dataSource;

    public FlywayCleanupRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n=== CLEANING FAILED FLYWAY MIGRATION ===\n");
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            int deleted = stmt.executeUpdate(
                "DELETE FROM flyway_schema_history WHERE version = '2'"
            );
            
            System.out.println("✓ Removed " + deleted + " migration record(s) for version 2");
            System.out.println("\n=== NEXT STEPS ===");
            System.out.println("1. Stop the application");
            System.out.println("2. In application.properties, change:");
            System.out.println("   spring.flyway.enabled=false  →  spring.flyway.enabled=true");
            System.out.println("   Add these lines:");
            System.out.println("   spring.flyway.baseline-on-migrate=true");
            System.out.println("   spring.flyway.locations=classpath:db/migration");
            System.out.println("   spring.flyway.baseline-version=0");
            System.out.println("   spring.flyway.validate-on-migrate=false");
            System.out.println("3. DELETE this file: FlywayCleanupRunner.java");
            System.out.println("4. Restart the application");
            System.out.println("==================\n");
            
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            throw e;
        }
    }
}
