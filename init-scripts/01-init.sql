-- ============================================
-- MySQL Initialization Script
-- University Archive System - Al-Quds University
-- ============================================
-- This script runs automatically on first container startup
-- when the mysql_data volume is empty

-- Set character encoding
SET NAMES 'utf8mb4';
SET CHARACTER SET utf8mb4;

-- Grant privileges to the application user
GRANT ALL PRIVILEGES ON archive_system.* TO 'archive_user'@'%';
FLUSH PRIVILEGES;

-- ============================================
-- Create Spring Session Tables
-- ============================================
CREATE TABLE IF NOT EXISTS SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BLOB NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;

-- Create indexes (ignore errors if they already exist)
-- Using ALTER TABLE instead of CREATE INDEX IF NOT EXISTS for compatibility
ALTER TABLE SPRING_SESSION ADD INDEX SPRING_SESSION_IX1 (SESSION_ID);
ALTER TABLE SPRING_SESSION ADD INDEX SPRING_SESSION_IX2 (EXPIRY_TIME);
ALTER TABLE SPRING_SESSION ADD INDEX SPRING_SESSION_IX3 (PRINCIPAL_NAME);

-- ============================================
-- Create Admin Account Procedure
-- ============================================
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS create_admin_user()
BEGIN
    DECLARE user_exists INT DEFAULT 0;
    
    -- Check if users table exists
    SELECT COUNT(*) INTO @table_exists 
    FROM information_schema.tables 
    WHERE table_schema = 'archive_system' AND table_name = 'users';
    
    IF @table_exists > 0 THEN
        -- Check if admin user already exists
        SELECT COUNT(*) INTO user_exists FROM users WHERE email = 'admin@alquds.edu';
        
        IF user_exists = 0 THEN
            -- Insert admin user
            -- Password: Admin@123 (BCrypt encoded)
            INSERT INTO users (email, password, first_name, last_name, role, is_active, created_at, updated_at)
            VALUES (
                'admin@alquds.edu',
                '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.RSVELzTQlHqXtQ/yS2',
                'System',
                'Administrator',
                'ROLE_ADMIN',
                TRUE,
                NOW(),
                NOW()
            );
            SELECT 'Admin user created successfully!' AS Status;
        ELSE
            SELECT 'Admin user already exists!' AS Status;
        END IF;
    ELSE
        SELECT 'Users table does not exist yet. Admin will be created by application.' AS Status;
    END IF;
END //

DELIMITER ;

-- Log successful initialization
SELECT 'Database initialized with Spring Session tables!' AS Status;
