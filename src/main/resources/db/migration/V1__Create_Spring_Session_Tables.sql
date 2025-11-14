-- =====================================================
-- Spring Session JDBC Schema for MySQL
-- =====================================================
-- This migration creates the tables required by Spring Session JDBC
-- to persist HTTP sessions in the database.
-- 
-- Tables:
-- - SPRING_SESSION: Stores session metadata
-- - SPRING_SESSION_ATTRIBUTES: Stores session attributes
-- =====================================================

-- Create SPRING_SESSION table
CREATE TABLE IF NOT EXISTS SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for efficient session lookup by SESSION_ID
CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);

-- Index for efficient session cleanup by expiry time
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);

-- Index for finding sessions by principal (username)
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

-- Create SPRING_SESSION_ATTRIBUTES table
CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BLOB NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) 
        REFERENCES SPRING_SESSION(PRIMARY_ID) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Notes:
-- 1. SESSION_ID is indexed for fast lookups
-- 2. EXPIRY_TIME index enables efficient cleanup of expired sessions
-- 3. PRINCIPAL_NAME index allows finding all sessions for a user
-- 4. Foreign key with CASCADE ensures attributes are deleted with session
-- 5. UTF8MB4 charset supports full Unicode including emojis
-- =====================================================
