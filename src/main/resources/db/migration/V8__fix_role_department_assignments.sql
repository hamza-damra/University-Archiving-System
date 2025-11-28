-- =====================================================
-- V8: Fix Role-Based Department Assignments
-- =====================================================
-- This migration enforces the correct role-department relationship:
-- - ADMIN: Should NOT have a department (system-wide access)
-- - DEANSHIP (Dean): Should NOT have a department (faculty/college-wide access)
-- - HOD: MUST have a department (manages a specific department)
-- - PROFESSOR: MUST have a department (belongs to a specific department)
-- =====================================================

-- Step 1: Remove department assignments from Admin users
-- Admin users have system-wide access and should not be tied to any department
UPDATE users 
SET department_id = NULL, updated_at = NOW()
WHERE role = 'ROLE_ADMIN' AND department_id IS NOT NULL;

-- Step 2: Remove department assignments from Dean (DEANSHIP) users
-- Dean users have faculty/college-wide access and should not be tied to a single department
UPDATE users 
SET department_id = NULL, updated_at = NOW()
WHERE role = 'ROLE_DEANSHIP' AND department_id IS NOT NULL;

-- Step 3: Validation check - Report any HOD users without a department
-- These should be manually fixed by assigning them to appropriate departments
SELECT 
    id, 
    email, 
    first_name, 
    last_name, 
    role,
    'WARNING: HOD user missing department assignment' as issue
FROM users 
WHERE role = 'ROLE_HOD' AND department_id IS NULL;

-- Step 4: Validation check - Report any Professor users without a department
-- These should be manually fixed by assigning them to appropriate departments
SELECT 
    id, 
    email, 
    first_name, 
    last_name, 
    role,
    'WARNING: Professor user missing department assignment' as issue
FROM users 
WHERE role = 'ROLE_PROFESSOR' AND department_id IS NULL;

-- Log migration summary
SELECT 
    'Migration V8 completed: Role-based department assignments fixed' as status,
    (SELECT COUNT(*) FROM users WHERE role = 'ROLE_ADMIN') as total_admins,
    (SELECT COUNT(*) FROM users WHERE role = 'ROLE_DEANSHIP') as total_deans,
    (SELECT COUNT(*) FROM users WHERE role = 'ROLE_HOD') as total_hods,
    (SELECT COUNT(*) FROM users WHERE role = 'ROLE_PROFESSOR') as total_professors,
    (SELECT COUNT(*) FROM users WHERE role IN ('ROLE_HOD', 'ROLE_PROFESSOR') AND department_id IS NULL) as users_needing_department_assignment;
