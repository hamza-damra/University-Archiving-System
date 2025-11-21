-- Fix the dean's email domain typo from 'alqude.edu' to 'alquds.edu'

USE archive_system;

-- Show current dean email
SELECT 'BEFORE:' as status, email FROM users WHERE role = 'ROLE_DEANSHIP';

-- Fix the typo
UPDATE users 
SET email = 'dean@alquds.edu' 
WHERE email = 'dean@alqude.edu';

-- Verify the fix
SELECT 'AFTER:' as status, email FROM users WHERE role = 'ROLE_DEANSHIP';

SELECT '✓ Email fixed! You can now login with: dean@alquds.edu' as result;
