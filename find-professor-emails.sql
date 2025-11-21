-- Query to find professor email addresses
-- Run this in MySQL Workbench, phpMyAdmin, or command line

SELECT 
    email,
    first_name,
    last_name,
    professor_id,
    department_id,
    is_active
FROM users
WHERE role = 'ROLE_PROFESSOR'
AND is_active = true
ORDER BY department_id, professor_id
LIMIT 10;

-- To run in MySQL command line:
-- mysql -u root -p
-- (press Enter when prompted for password since it's blank)
-- USE your_database_name;
-- (paste the query above)
