-- Clear ALL mock data from archive_system database
-- This will delete all uploaded files, submissions, courses, semesters, and user data
-- Only departments table will remain for structure

USE archive_system;

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- Clear all uploaded files and submissions
TRUNCATE TABLE uploaded_files;
TRUNCATE TABLE document_submissions;
TRUNCATE TABLE submitted_documents;

-- Clear course-related data
TRUNCATE TABLE required_document_types;
TRUNCATE TABLE course_assignments;
TRUNCATE TABLE courses;

-- Clear semester and academic year data
TRUNCATE TABLE semesters;
TRUNCATE TABLE academic_years;

-- Clear notifications
TRUNCATE TABLE notifications;

-- Clear document requests
TRUNCATE TABLE document_requests;

-- Clear all users (professors, HODs, deans, etc.)
TRUNCATE TABLE users;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Verify all tables are empty
SELECT 'uploaded_files' as table_name, COUNT(*) as count FROM uploaded_files
UNION ALL SELECT 'document_submissions', COUNT(*) FROM document_submissions
UNION ALL SELECT 'submitted_documents', COUNT(*) FROM submitted_documents
UNION ALL SELECT 'required_document_types', COUNT(*) FROM required_document_types
UNION ALL SELECT 'course_assignments', COUNT(*) FROM course_assignments
UNION ALL SELECT 'courses', COUNT(*) FROM courses
UNION ALL SELECT 'semesters', COUNT(*) FROM semesters
UNION ALL SELECT 'academic_years', COUNT(*) FROM academic_years
UNION ALL SELECT 'notifications', COUNT(*) FROM notifications
UNION ALL SELECT 'document_requests', COUNT(*) FROM document_requests
UNION ALL SELECT 'users', COUNT(*) FROM users;

SELECT '✓ All mock data has been deleted successfully!' as status;
