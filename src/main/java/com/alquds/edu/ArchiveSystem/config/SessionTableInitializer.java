package com.alquds.edu.ArchiveSystem.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Ensures Spring Session tables are created before the application handles requests.
 * Uses @PostConstruct to ensure tables are created during bean initialization,
 * before any HTTP requests are processed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionTableInitializer {

    private final DataSource dataSource;

    @PostConstruct
    public void init() {
        log.info("Checking Spring Session tables...");
        
        try (Connection conn = dataSource.getConnection()) {
            if (!tableExists(conn, "SPRING_SESSION")) {
                log.info("Creating Spring Session tables...");
                createSessionTables(conn);
                log.info("ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Spring Session tables created successfully.");
            } else {
                log.info("ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Spring Session tables already exist.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Spring Session tables: {}", e.getMessage());
            // Don't throw - let Spring Session's own initialization try as well
        }
    }

    private boolean tableExists(Connection conn, String tableName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            // Try both cases for compatibility
            try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = meta.getTables(null, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void createSessionTables(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // Create SPRING_SESSION table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS SPRING_SESSION (
                    PRIMARY_ID CHAR(36) NOT NULL,
                    SESSION_ID CHAR(36) NOT NULL,
                    CREATION_TIME BIGINT NOT NULL,
                    LAST_ACCESS_TIME BIGINT NOT NULL,
                    MAX_INACTIVE_INTERVAL INT NOT NULL,
                    EXPIRY_TIME BIGINT NOT NULL,
                    PRINCIPAL_NAME VARCHAR(100),
                    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
                ) ENGINE=InnoDB ROW_FORMAT=DYNAMIC
                """);

            // Create indexes (ignore errors if they already exist)
            try {
                stmt.execute("CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID)");
            } catch (Exception ignored) {}
            
            try {
                stmt.execute("CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME)");
            } catch (Exception ignored) {}
            
            try {
                stmt.execute("CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME)");
            } catch (Exception ignored) {}

            // Create SPRING_SESSION_ATTRIBUTES table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
                    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
                    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
                    ATTRIBUTE_BYTES BLOB NOT NULL,
                    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
                    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) 
                        REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
                ) ENGINE=InnoDB ROW_FORMAT=DYNAMIC
                """);
        }
    }
}
