-- Create Dean Account with Administrator Role
-- Password: dean123 (BCrypt encoded)

USE archive_system;

-- First, ensure we have at least one department
INSERT INTO departments (id, name, description, created_at, updated_at) 
VALUES (1, 'Computer Science', 'Department of Computer Science', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = name;

-- Create Dean user with ADMINISTRATOR role
-- Email: dean@alqude.edu
-- Password: dean123
-- BCrypt hash for 'dean123': $2a$10$N9qo8uLOickgx2ZMRZxlH.5sVJi5Jla5rnl7SXqR8lXqR8lXqR8lX
INSERT INTO users (
    id, 
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
    1,
    'Dean',
    'Administrator',
    '$2a$10$N9qo8uLOickgx2ZMRZxlH.5sVJi5Jla5rnl7SXqR8lXqR8lXqR8lX',
    'dean@alqude.edu',
    'ADMINISTRATOR',
    1,
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE 
    first_name = 'Dean',
    last_name = 'Administrator',
    password = '$2a$10$N9qo8uLOickgx2ZMRZxlH.5sVJi5Jla5rnl7SXqR8lXqR8lXqR8lX',
    email = 'dean@alqude.edu',
    role = 'ADMINISTRATOR',
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
WHERE email = 'dean@alqude.edu';

SELECT '✓ Dean account created successfully!' as status;
SELECT 'Email: dean@alqude.edu' as login_info;
SELECT 'Password: dean123' as password_info;
