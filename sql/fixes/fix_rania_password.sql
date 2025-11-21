-- Fix the password for prof.rania.hassan@alquds.edu
-- This updates the password to match the correct BCrypt hash for 'password123'

USE archive_system;

-- Update the password using the same hash as other mock accounts
-- We'll copy the password hash from the dean account which uses 'password123'
UPDATE users 
SET password = (SELECT password FROM (SELECT password FROM users WHERE email = 'dean@alquds.edu' LIMIT 1) AS temp)
WHERE email = 'prof.rania.hassan@alquds.edu';

-- Verify the update
SELECT 
    email,
    CONCAT(first_name, ' ', last_name) AS name,
    professor_id,
    is_active,
    LEFT(password, 20) AS password_hash_preview
FROM users 
WHERE email = 'prof.rania.hassan@alquds.edu';
