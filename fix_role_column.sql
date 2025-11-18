-- Fix the role column size to accommodate ROLE_DEANSHIP
-- Run this in phpMyAdmin or MySQL client
-- MySQL credentials: username=root, password=(blank)

USE archive_system;

-- Check current column definition
DESCRIBE users;

-- Alter the role column to increase its size
ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL;

-- Verify the change
DESCRIBE users;

-- Show current users
SELECT id, email, role, first_name, last_name FROM users;
