-- =====================================================
-- Update Users Table for Semester-Based System
-- =====================================================
-- This migration updates the users table to support the new
-- semester-based academic structure:
-- 1. Adds professor_id column for unique professor identification
-- 2. Ensures role enum includes ROLE_DEANSHIP
-- 3. Adds indexes for performance optimization
-- =====================================================

-- =====================================================
-- 1. ADD PROFESSOR_ID COLUMN
-- =====================================================
-- Add professor_id column if it doesn't exist
-- This column stores a unique identifier for professors (e.g., "prof_12345")
SET @col_exists_professor_id = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'professor_id'
);

SET @sql_add_professor_id = IF(
    @col_exists_professor_id = 0, 
    'ALTER TABLE users ADD COLUMN professor_id VARCHAR(50) UNIQUE COMMENT ''Unique professor identifier (e.g., prof_12345)''', 
    'SELECT ''Column professor_id already exists'' AS message'
);

PREPARE stmt_add_professor_id FROM @sql_add_professor_id;
EXECUTE stmt_add_professor_id;
DEALLOCATE PREPARE stmt_add_professor_id;

-- =====================================================
-- 2. ADD IS_ACTIVE COLUMN (if not exists)
-- =====================================================
-- Add is_active column if it doesn't exist
-- This column tracks whether a user account is active
SET @col_exists_is_active = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'is_active'
);

SET @sql_add_is_active = IF(
    @col_exists_is_active = 0, 
    'ALTER TABLE users ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT ''Whether the user account is active''', 
    'SELECT ''Column is_active already exists'' AS message'
);

PREPARE stmt_add_is_active FROM @sql_add_is_active;
EXECUTE stmt_add_is_active;
DEALLOCATE PREPARE stmt_add_is_active;

-- =====================================================
-- 3. ENSURE ROLE ENUM INCLUDES ROLE_DEANSHIP
-- =====================================================
-- Note: MySQL doesn't support ALTER ENUM directly in a safe way
-- The role column should already support VARCHAR/ENUM with ROLE_DEANSHIP
-- from the JPA entity definition. This is a verification step.

-- Verify role column can store ROLE_DEANSHIP
-- If role is VARCHAR, no action needed
-- If role is ENUM, it should already include ROLE_DEANSHIP from entity definition

-- Check if role column is VARCHAR or ENUM
SET @role_column_type = (
    SELECT DATA_TYPE 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'role'
);

-- If role is VARCHAR, ensure it's long enough for ROLE_DEANSHIP
SET @sql_ensure_role_varchar = IF(
    @role_column_type = 'varchar',
    'ALTER TABLE users MODIFY COLUMN role VARCHAR(50) NOT NULL COMMENT ''User role: ROLE_DEANSHIP, ROLE_HOD, or ROLE_PROFESSOR''',
    'SELECT ''Role column type is not VARCHAR, skipping modification'' AS message'
);

PREPARE stmt_ensure_role_varchar FROM @sql_ensure_role_varchar;
EXECUTE stmt_ensure_role_varchar;
DEALLOCATE PREPARE stmt_ensure_role_varchar;

-- =====================================================
-- 4. ADD INDEXES FOR PERFORMANCE (if not exist)
-- =====================================================
-- Index on department_id for department-scoped queries
SET @index_exists_department = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND INDEX_NAME = 'idx_users_department'
);

SET @sql_add_index_department = IF(
    @index_exists_department = 0, 
    'CREATE INDEX idx_users_department ON users(department_id)', 
    'SELECT ''Index idx_users_department already exists'' AS message'
);

PREPARE stmt_add_index_department FROM @sql_add_index_department;
EXECUTE stmt_add_index_department;
DEALLOCATE PREPARE stmt_add_index_department;

-- Index on role for role-based queries
SET @index_exists_role = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND INDEX_NAME = 'idx_users_role'
);

SET @sql_add_index_role = IF(
    @index_exists_role = 0, 
    'CREATE INDEX idx_users_role ON users(role)', 
    'SELECT ''Index idx_users_role already exists'' AS message'
);

PREPARE stmt_add_index_role FROM @sql_add_index_role;
EXECUTE stmt_add_index_role;
DEALLOCATE PREPARE stmt_add_index_role;

-- Index on professor_id for professor lookups
SET @index_exists_professor_id = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND INDEX_NAME = 'idx_users_professor_id'
);

SET @sql_add_index_professor_id = IF(
    @index_exists_professor_id = 0, 
    'CREATE INDEX idx_users_professor_id ON users(professor_id)', 
    'SELECT ''Index idx_users_professor_id already exists'' AS message'
);

PREPARE stmt_add_index_professor_id FROM @sql_add_index_professor_id;
EXECUTE stmt_add_index_professor_id;
DEALLOCATE PREPARE stmt_add_index_professor_id;

-- Index on is_active for filtering active users
SET @index_exists_is_active = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND INDEX_NAME = 'idx_users_is_active'
);

SET @sql_add_index_is_active = IF(
    @index_exists_is_active = 0, 
    'CREATE INDEX idx_users_is_active ON users(is_active)', 
    'SELECT ''Index idx_users_is_active already exists'' AS message'
);

PREPARE stmt_add_index_is_active FROM @sql_add_index_is_active;
EXECUTE stmt_add_index_is_active;
DEALLOCATE PREPARE stmt_add_index_is_active;

-- =====================================================
-- 5. ENSURE DEPARTMENTS TABLE EXISTS
-- =====================================================
-- Create departments table if it doesn't exist
-- This is needed for the foreign key constraint
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_departments_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='University departments';

-- =====================================================
-- MIGRATION NOTES
-- =====================================================
-- 1. professor_id is added as a unique identifier for professors
-- 2. is_active tracks whether user accounts are active
-- 3. Role enum should support ROLE_DEANSHIP, ROLE_HOD, ROLE_PROFESSOR
-- 4. Indexes are added for performance on frequently queried columns
-- 5. All operations are idempotent (safe to run multiple times)
-- 6. Existing data is preserved
-- =====================================================
