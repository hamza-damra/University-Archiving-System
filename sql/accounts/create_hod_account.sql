-- Create HOD (Head of Department) Account
-- Password: hod123 (BCrypt encoded)

USE archive_system;

-- First, ensure we have at least one department
INSERT INTO departments (id, name, description, created_at, updated_at) 
VALUES (1, 'Computer Science', 'Department of Computer Science', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = name;

-- Create HOD user with HOD role
-- Email: hod@alqude.edu
-- Password: hod123
-- BCrypt hash for 'hod123': $2a$10$N9qo8uLOickgx2ZMRZxlH.5sVJi5Jla5rnl7SXqR8lXqR8lXqR8lX
INSERT INTO users (
    first_name,
    last_name,
    password, 
    email, 
    role, 
    department_id, 
    is_active, 
    created_at, 
    updated_at
) VALUES (
    'Head',
    'Of Department',
    '$2a$10$N9qo8uLOickgx2ZMRZxlH.5sVJi5Jla5rnl7SXqR8lXqR8lXqR8lX',
    'hod@alqude.edu',
    'HOD',
    1,
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE 
    first_name = 'Head',
    last_name = 'Of Department',
    password = '$2a$10$N9qo8uLOickgx2ZMRZxlH.5sVJi5Jla5rnl7SXqR8lXqR8lXqR8lX',
    email = 'hod@alqude.edu',
    role = 'HOD',
    is_active = 1;

-- Verify the account was created
SELECT 
    id,
    first_name,
    last_name,
    email,
    role,
    is_active,
    department_id
FROM users 
WHERE email = 'hod@alqude.edu';

SELECT '✓ HOD account created successfully!' as status;
SELECT 'Email: hod@alqude.edu' as login_info;
SELECT 'Password: hod123' as password_info;
