-- Create a specific professor account: prof.rania.hassan@alquds.edu
-- This will create the exact user you're trying to log in with

USE archive_system;

-- First, get a department ID (using Computer Science as example)
SET @dept_id = (SELECT id FROM departments WHERE name = 'Computer Science' LIMIT 1);

-- The password 'password123' encoded with BCrypt
-- This is the same password used by all mock accounts
-- BCrypt hash for 'password123': $2a$10$N9qo8uLOickgx2ZMRZoMyeIXfkcbQq3h9J3RQrY3p5XoQV5gY9oDa

INSERT INTO users (
    email,
    password,
    first_name,
    last_name,
    role,
    department_id,
    professor_id,
    is_active,
    created_at,
    updated_at
) VALUES (
    'prof.rania.hassan@alquds.edu',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIXfkcbQq3h9J3RQrY3p5XoQV5gY9oDa',
    'Rania',
    'Hassan',
    'ROLE_PROFESSOR',
    @dept_id,
    'PCS999',
    1,
    NOW(),
    NOW()
);

-- Verify the user was created
SELECT 
    email,
    CONCAT(first_name, ' ', last_name) AS full_name,
    professor_id,
    role,
    is_active
FROM users 
WHERE email = 'prof.rania.hassan@alquds.edu';
