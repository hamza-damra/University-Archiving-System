-- Clear existing data to regenerate with dean account
-- Run this in phpMyAdmin or MySQL client
-- MySQL credentials: username=root, password=(blank)

USE archive_system;

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- Clear all data
TRUNCATE TABLE uploaded_files;
TRUNCATE TABLE document_submissions;
TRUNCATE TABLE required_document_types;
TRUNCATE TABLE course_assignments;
TRUNCATE TABLE courses;
TRUNCATE TABLE semesters;
TRUNCATE TABLE academic_years;
TRUNCATE TABLE notifications;
TRUNCATE TABLE submitted_documents;
TRUNCATE TABLE document_requests;
TRUNCATE TABLE users;
TRUNCATE TABLE departments;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Verify tables are empty
SELECT 'users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'departments', COUNT(*) FROM departments
UNION ALL
SELECT 'academic_years', COUNT(*) FROM academic_years;
