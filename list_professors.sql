-- List all active professor accounts in the database
-- Run this to see what professor emails exist

USE archive_system;

SELECT 
    email,
    CONCAT(first_name, ' ', last_name) AS full_name,
    professor_id,
    d.name AS department,
    is_active
FROM users u
LEFT JOIN departments d ON u.department_id = d.id
WHERE u.role = 'ROLE_PROFESSOR'
ORDER BY d.name, professor_id;

-- Count professors by department
SELECT 
    d.name AS department,
    COUNT(*) AS professor_count
FROM users u
LEFT JOIN departments d ON u.department_id = d.id
WHERE u.role = 'ROLE_PROFESSOR'
GROUP BY d.name;
