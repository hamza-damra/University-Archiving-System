package com.alqude.edu.ArchiveSystem.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * ONE-TIME repair utility - DELETE THIS FILE after successful startup
 * Only runs in dev profile, not in production.
 * 
 * This runner safely handles the case where the flyway_schema_history table
 * doesn't exist (e.g., fresh database).
 */
@Component
@Order(1)
@Profile("!prod")
public class FlywayCleanupRunner implements CommandLineRunner {

    private final DataSource dataSource;

    public FlywayCleanupRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n=== CHECKING FLYWAY MIGRATION STATUS ===\n");
        
        try (Connection conn = dataSource.getConnection()) {
            
            // Check if flyway_schema_history table exists
            if (!tableExists(conn, "flyway_schema_history")) {
                System.out.println("✓ Fresh database detected (no flyway_schema_history table).");
                System.out.println("  No migration cleanup needed.\n");
                return;
            }
            
            // Table exists, try to clean up failed migration
            try (Statement stmt = conn.createStatement()) {
                int deleted = stmt.executeUpdate(
                    "DELETE FROM flyway_schema_history WHERE version = '2' AND success = 0"
                );
                
                if (deleted > 0) {
                    System.out.println("✓ Removed " + deleted + " failed migration record(s) for version 2");
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
                } else {
                    System.out.println("✓ No failed migrations to clean up.\n");
                }
            }
            
        } catch (Exception e) {
            // Log error but don't throw - allow application to continue
            System.err.println("⚠ Warning during Flyway cleanup check: " + e.getMessage());
            System.err.println("  This is not critical - continuing startup...\n");
        }
    }
    
    /**
     * Checks if a table exists in the database.
     */
    private boolean tableExists(Connection conn, String tableName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            // Try both lowercase and uppercase table names for compatibility
            try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = meta.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
