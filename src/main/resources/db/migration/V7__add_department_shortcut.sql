-- =====================================================
-- Add Department Shortcut Field Migration
-- =====================================================
-- This migration adds a unique shortcut field to the departments table
-- for HOD email validation purposes.
--
-- The shortcut is used in HOD email patterns: hod.<shortcut>@dean.alquds.edu
-- =====================================================

-- Add shortcut column to departments table
ALTER TABLE departments 
ADD COLUMN shortcut VARCHAR(20) NULL AFTER name;

-- Create unique index for shortcut
CREATE UNIQUE INDEX idx_departments_shortcut ON departments(shortcut);

-- Update existing departments with default shortcuts based on name
-- This generates a lowercase shortcut from the department name
UPDATE departments 
SET shortcut = LOWER(REPLACE(REPLACE(REPLACE(name, ' ', ''), '-', ''), '_', ''))
WHERE shortcut IS NULL;

-- Make shortcut column NOT NULL after populating existing records
ALTER TABLE departments 
MODIFY COLUMN shortcut VARCHAR(20) NOT NULL;

-- Add check constraint for shortcut format (lowercase alphanumeric only)
-- Note: MySQL 8.0+ supports CHECK constraints
ALTER TABLE departments 
ADD CONSTRAINT chk_department_shortcut 
CHECK (shortcut REGEXP '^[a-z0-9]+$');

-- =====================================================
-- MIGRATION NOTES
-- =====================================================
-- 1. The shortcut field is required for HOD email validation
-- 2. Format: lowercase letters and numbers only (e.g., "cs", "math", "ee")
-- 3. Must be unique across all departments
-- 4. Used in HOD email pattern: hod.<shortcut>@dean.alquds.edu
-- =====================================================
