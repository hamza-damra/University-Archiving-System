-- SIMPLE FIX: Copy password from dean account to Rania's account
-- Both accounts will use password: password123

USE archive_system;

-- Show current password hash for reference
SELECT 'Current Rania password:' as info, LEFT(password, 60) as password_hash 
FROM users WHERE email = 'prof.rania.hassan@alquds.edu';

-- Show dean password hash for reference  
SELECT 'Dean password (correct):' as info, LEFT(password, 60) as password_hash
FROM users WHERE email = 'dean@alquds.edu';

-- Copy the correct password hash from dean to Rania
UPDATE users u1
SET u1.password = (
    SELECT u2.password 
    FROM users u2 
    WHERE u2.email = 'dean@alquds.edu'
    LIMIT 1
)
WHERE u1.email = 'prof.rania.hassan@alquds.edu';

-- Verify the update
SELECT 'UPDATED Rania password:' as info, LEFT(password, 60) as password_hash
FROM users WHERE email = 'prof.rania.hassan@alquds.edu';

SELECT '✓ Password updated! Both dean and Rania now use: password123' as result;
