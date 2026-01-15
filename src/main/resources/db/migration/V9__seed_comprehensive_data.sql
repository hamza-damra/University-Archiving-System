-- =====================================================
-- Comprehensive Data Seeding Migration
-- =====================================================
-- This migration seeds the database with comprehensive sample data:
-- 1. Departments
-- 2. Users (Admin, Deanship, HODs, Professors) with BCrypt passwords
-- 3. Academic Years
-- 4. Semesters
-- 5. Courses
-- 6. Course Assignments
-- 7. Required Document Types
-- 8. Folders (hierarchical structure)
-- 9. Document Submissions (sample data)
-- =====================================================
-- Note: This migration is idempotent - it checks for existing data
-- before inserting to avoid duplicates.
-- =====================================================
-- IMPORTANT: Before running this migration, generate BCrypt password hashes
-- using PasswordHashGenerator.java or an online BCrypt generator tool.
-- Replace the placeholder hashes ($2a$10$YourBCryptHashHereFor...) with actual hashes.
-- =====================================================

-- =====================================================
-- 0. ENSURE FOLDERS TABLE EXISTS
-- =====================================================
CREATE TABLE IF NOT EXISTS folders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    path VARCHAR(500) NOT NULL UNIQUE COMMENT 'Full folder path',
    name VARCHAR(255) NOT NULL COMMENT 'Display name',
    type VARCHAR(50) NOT NULL COMMENT 'YEAR_ROOT, SEMESTER_ROOT, PROFESSOR_ROOT, COURSE, SUBFOLDER',
    parent_id BIGINT NULL COMMENT 'Parent folder ID',
    owner_id BIGINT NULL COMMENT 'Owner user ID',
    academic_year_id BIGINT NULL COMMENT 'Academic year ID',
    semester_id BIGINT NULL COMMENT 'Semester ID',
    course_id BIGINT NULL COMMENT 'Course ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_folders_parent FOREIGN KEY (parent_id) REFERENCES folders(id) ON DELETE CASCADE,
    CONSTRAINT fk_folders_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_folders_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE CASCADE,
    CONSTRAINT fk_folders_semester FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE CASCADE,
    CONSTRAINT fk_folders_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    
    INDEX idx_folder_path (path),
    INDEX idx_folder_parent (parent_id),
    INDEX idx_folder_owner (owner_id),
    INDEX idx_folder_context (academic_year_id, semester_id, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Folder hierarchy for file organization';

-- =====================================================
-- 1. SEED DEPARTMENTS
-- =====================================================
INSERT IGNORE INTO departments (name, shortcut, description, created_at, updated_at) VALUES
('Computer Science', 'cs', 'Department of Computer Science', NOW(), NULL),
('Mathematics', 'math', 'Department of Mathematics', NOW(), NULL),
('Physics', 'physics', 'Department of Physics', NOW(), NULL),
('Chemistry', 'chemistry', 'Department of Chemistry', NOW(), NULL),
('Engineering', 'eng', 'Department of Engineering', NOW(), NULL);

-- =====================================================
-- 2. SEED USERS WITH BCRYPT PASSWORDS
-- =====================================================
-- IMPORTANT: BCrypt hashes need to be generated using BCryptPasswordEncoder
-- Run PasswordHashGenerator.java to generate proper hashes, or use online tool
-- Passwords: Admin@123, Deanship@123, Hod@123, Prof@123
-- These are placeholder hashes - REPLACE with actual BCrypt hashes before running!

-- Admin User (Password: Admin@123)
INSERT IGNORE INTO users (email, password, first_name, last_name, role, is_active, created_at, updated_at)
SELECT 'admin@alquds.edu', 
       '$2a$10$YourBCryptHashHereForAdmin123', -- TODO: Replace with actual BCrypt hash for Admin@123
       'System', 'Administrator', 'ROLE_ADMIN', TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@alquds.edu');

-- Deanship User (Password: Deanship@123)
INSERT IGNORE INTO users (email, password, first_name, last_name, role, is_active, created_at, updated_at)
SELECT 'deanship@alquds.edu',
       '$2a$10$YourBCryptHashHereForDeanship123', -- TODO: Replace with actual BCrypt hash for Deanship@123
       'Deanship', 'Administrator', 'ROLE_DEANSHIP', TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'deanship@alquds.edu');

-- HOD Users (Password: Hod@123 for all)
INSERT IGNORE INTO users (email, password, first_name, last_name, professor_id, role, department_id, is_active, created_at, updated_at)
SELECT 'hod.cs@alquds.edu',
       '$2a$10$YourBCryptHashHereForHod123', -- TODO: Replace with actual BCrypt hash for Hod@123
       'Ahmed', 'Ali', 'CS001', 'ROLE_HOD',
       (SELECT id FROM departments WHERE shortcut = 'cs' LIMIT 1),
       TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'hod.cs@alquds.edu');

INSERT IGNORE INTO users (email, password, first_name, last_name, professor_id, role, department_id, is_active, created_at, updated_at)
SELECT 'hod.math@alquds.edu',
       '$2a$10$YourBCryptHashHereForHod123', -- TODO: Replace with actual BCrypt hash for Hod@123
       'Fatima', 'Hassan', 'MATH001', 'ROLE_HOD',
       (SELECT id FROM departments WHERE shortcut = 'math' LIMIT 1),
       TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'hod.math@alquds.edu');

INSERT IGNORE INTO users (email, password, first_name, last_name, professor_id, role, department_id, is_active, created_at, updated_at)
SELECT 'hod.physics@alquds.edu',
       '$2a$10$YourBCryptHashHereForHod123', -- TODO: Replace with actual BCrypt hash for Hod@123
       'Mohammed', 'Ibrahim', 'PHYS001', 'ROLE_HOD',
       (SELECT id FROM departments WHERE shortcut = 'physics' LIMIT 1),
       TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'hod.physics@alquds.edu');

-- Professor Users (Password: Prof@123 for all)
INSERT IGNORE INTO users (email, password, first_name, last_name, professor_id, role, department_id, is_active, created_at, updated_at)
SELECT 'prof1@alquds.edu',
       '$2a$10$YourBCryptHashHereForProf123', -- TODO: Replace with actual BCrypt hash for Prof@123
       'Omar', 'Khalil', 'PROF001', 'ROLE_PROFESSOR',
       (SELECT id FROM departments WHERE shortcut = 'cs' LIMIT 1),
       TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof1@alquds.edu');

INSERT IGNORE INTO users (email, password, first_name, last_name, professor_id, role, department_id, is_active, created_at, updated_at)
SELECT 'prof2@alquds.edu',
       '$2a$10$YourBCryptHashHereForProf123', -- TODO: Replace with actual BCrypt hash for Prof@123
       'Layla', 'Mahmoud', 'PROF002', 'ROLE_PROFESSOR',
       (SELECT id FROM departments WHERE shortcut = 'cs' LIMIT 1),
       TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof2@alquds.edu');

INSERT IGNORE INTO users (email, password, first_name, last_name, professor_id, role, department_id, is_active, created_at, updated_at)
SELECT 'prof3@alquds.edu',
       '$2a$10$YourBCryptHashHereForProf123', -- TODO: Replace with actual BCrypt hash for Prof@123
       'Youssef', 'Nasser', 'PROF003', 'ROLE_PROFESSOR',
       (SELECT id FROM departments WHERE shortcut = 'math' LIMIT 1),
       TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof3@alquds.edu');

INSERT IGNORE INTO users (email, password, first_name, last_name, professor_id, role, department_id, is_active, created_at, updated_at)
SELECT 'prof4@alquds.edu',
       '$2a$10$YourBCryptHashHereForProf123', -- TODO: Replace with actual BCrypt hash for Prof@123
       'Nour', 'Salem', 'PROF004', 'ROLE_PROFESSOR',
       (SELECT id FROM departments WHERE shortcut = 'math' LIMIT 1),
       TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof4@alquds.edu');

INSERT IGNORE INTO users (email, password, first_name, last_name, professor_id, role, department_id, is_active, created_at, updated_at)
SELECT 'prof5@alquds.edu',
       '$2a$10$YourBCryptHashHereForProf123', -- TODO: Replace with actual BCrypt hash for Prof@123
       'Khalid', 'Omar', 'PROF005', 'ROLE_PROFESSOR',
       (SELECT id FROM departments WHERE shortcut = 'physics' LIMIT 1),
       TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof5@alquds.edu');

INSERT IGNORE INTO users (email, password, first_name, last_name, professor_id, role, department_id, is_active, created_at, updated_at)
SELECT 'prof6@alquds.edu',
       '$2a$10$YourBCryptHashHereForProf123', -- TODO: Replace with actual BCrypt hash for Prof@123
       'Sara', 'Ahmed', 'PROF006', 'ROLE_PROFESSOR',
       (SELECT id FROM departments WHERE shortcut = 'cs' LIMIT 1),
       TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof6@alquds.edu');

INSERT IGNORE INTO users (email, password, first_name, last_name, professor_id, role, department_id, is_active, created_at, updated_at)
SELECT 'prof7@alquds.edu',
       '$2a$10$YourBCryptHashHereForProf123', -- TODO: Replace with actual BCrypt hash for Prof@123
       'Hassan', 'Mohammed', 'PROF007', 'ROLE_PROFESSOR',
       (SELECT id FROM departments WHERE shortcut = 'chemistry' LIMIT 1),
       TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof7@alquds.edu');

INSERT IGNORE INTO users (email, password, first_name, last_name, professor_id, role, department_id, is_active, created_at, updated_at)
SELECT 'prof8@alquds.edu',
       '$2a$10$YourBCryptHashHereForProf123', -- TODO: Replace with actual BCrypt hash for Prof@123
       'Mariam', 'Ali', 'PROF008', 'ROLE_PROFESSOR',
       (SELECT id FROM departments WHERE shortcut = 'eng' LIMIT 1),
       TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof8@alquds.edu');

-- =====================================================
-- 3. SEED ACADEMIC YEARS
-- =====================================================
-- Create academic years: 2 years ago, 1 year ago, current, 1 year ahead, 2 years ahead
SET @current_year = YEAR(CURDATE());

INSERT IGNORE INTO academic_years (year_code, start_year, end_year, is_active, created_at, updated_at)
SELECT CONCAT(@current_year - 2, '-', @current_year - 1), @current_year - 2, @current_year - 1, FALSE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM academic_years WHERE year_code = CONCAT(@current_year - 2, '-', @current_year - 1));

INSERT IGNORE INTO academic_years (year_code, start_year, end_year, is_active, created_at, updated_at)
SELECT CONCAT(@current_year - 1, '-', @current_year), @current_year - 1, @current_year, FALSE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM academic_years WHERE year_code = CONCAT(@current_year - 1, '-', @current_year));

INSERT IGNORE INTO academic_years (year_code, start_year, end_year, is_active, created_at, updated_at)
SELECT CONCAT(@current_year, '-', @current_year + 1), @current_year, @current_year + 1, TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM academic_years WHERE year_code = CONCAT(@current_year, '-', @current_year + 1));

INSERT IGNORE INTO academic_years (year_code, start_year, end_year, is_active, created_at, updated_at)
SELECT CONCAT(@current_year + 1, '-', @current_year + 2), @current_year + 1, @current_year + 2, FALSE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM academic_years WHERE year_code = CONCAT(@current_year + 1, '-', @current_year + 2));

INSERT IGNORE INTO academic_years (year_code, start_year, end_year, is_active, created_at, updated_at)
SELECT CONCAT(@current_year + 2, '-', @current_year + 3), @current_year + 2, @current_year + 3, FALSE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM academic_years WHERE year_code = CONCAT(@current_year + 2, '-', @current_year + 3));

-- =====================================================
-- 4. SEED SEMESTERS
-- =====================================================
-- Create semesters for each academic year: FIRST, SECOND, SUMMER
INSERT IGNORE INTO semesters (academic_year_id, type, start_date, end_date, is_active, created_at, updated_at)
SELECT ay.id, 'FIRST', 
       DATE(CONCAT(ay.start_year, '-09-01')), 
       DATE(CONCAT(ay.start_year, '-12-31')),
       ay.is_active, NOW(), NULL
FROM academic_years ay
WHERE NOT EXISTS (
    SELECT 1 FROM semesters s 
    WHERE s.academic_year_id = ay.id AND s.type = 'FIRST'
);

INSERT IGNORE INTO semesters (academic_year_id, type, start_date, end_date, is_active, created_at, updated_at)
SELECT ay.id, 'SECOND',
       DATE(CONCAT(ay.end_year, '-02-01')),
       DATE(CONCAT(ay.end_year, '-05-31')),
       ay.is_active, NOW(), NULL
FROM academic_years ay
WHERE NOT EXISTS (
    SELECT 1 FROM semesters s 
    WHERE s.academic_year_id = ay.id AND s.type = 'SECOND'
);

INSERT IGNORE INTO semesters (academic_year_id, type, start_date, end_date, is_active, created_at, updated_at)
SELECT ay.id, 'SUMMER',
       DATE(CONCAT(ay.end_year, '-06-01')),
       DATE(CONCAT(ay.end_year, '-08-31')),
       FALSE, NOW(), NULL
FROM academic_years ay
WHERE NOT EXISTS (
    SELECT 1 FROM semesters s 
    WHERE s.academic_year_id = ay.id AND s.type = 'SUMMER'
);

-- =====================================================
-- 5. SEED COURSES
-- =====================================================
-- Computer Science Courses
INSERT IGNORE INTO courses (course_code, course_name, department_id, level, description, is_active, created_at, updated_at)
SELECT 'CS101', 'Introduction to Programming',
       (SELECT id FROM departments WHERE shortcut = 'cs' LIMIT 1),
       'Undergraduate', 'Basic programming concepts', TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE course_code = 'CS101');

INSERT IGNORE INTO courses (course_code, course_name, department_id, level, description, is_active, created_at, updated_at)
SELECT 'CS201', 'Data Structures',
       (SELECT id FROM departments WHERE shortcut = 'cs' LIMIT 1),
       'Undergraduate', 'Fundamental data structures', TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE course_code = 'CS201');

INSERT IGNORE INTO courses (course_code, course_name, department_id, level, description, is_active, created_at, updated_at)
SELECT 'CS301', 'Database Systems',
       (SELECT id FROM departments WHERE shortcut = 'cs' LIMIT 1),
       'Undergraduate', 'Database design and SQL', TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE course_code = 'CS301');

INSERT IGNORE INTO courses (course_code, course_name, department_id, level, description, is_active, created_at, updated_at)
SELECT 'CS401', 'Software Engineering',
       (SELECT id FROM departments WHERE shortcut = 'cs' LIMIT 1),
       'Undergraduate', 'Software development lifecycle', TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE course_code = 'CS401');

INSERT IGNORE INTO courses (course_code, course_name, department_id, level, description, is_active, created_at, updated_at)
SELECT 'CS501', 'Advanced Algorithms',
       (SELECT id FROM departments WHERE shortcut = 'cs' LIMIT 1),
       'Graduate', 'Advanced algorithmic techniques', TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE course_code = 'CS501');

-- Mathematics Courses
INSERT IGNORE INTO courses (course_code, course_name, department_id, level, description, is_active, created_at, updated_at)
SELECT 'MATH101', 'Calculus I',
       (SELECT id FROM departments WHERE shortcut = 'math' LIMIT 1),
       'Undergraduate', 'Differential and integral calculus', TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE course_code = 'MATH101');

INSERT IGNORE INTO courses (course_code, course_name, department_id, level, description, is_active, created_at, updated_at)
SELECT 'MATH201', 'Linear Algebra',
       (SELECT id FROM departments WHERE shortcut = 'math' LIMIT 1),
       'Undergraduate', 'Vector spaces and matrices', TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE course_code = 'MATH201');

INSERT IGNORE INTO courses (course_code, course_name, department_id, level, description, is_active, created_at, updated_at)
SELECT 'MATH301', 'Differential Equations',
       (SELECT id FROM departments WHERE shortcut = 'math' LIMIT 1),
       'Undergraduate', 'Ordinary and partial differential equations', TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE course_code = 'MATH301');

-- Physics Courses
INSERT IGNORE INTO courses (course_code, course_name, department_id, level, description, is_active, created_at, updated_at)
SELECT 'PHYS101', 'General Physics I',
       (SELECT id FROM departments WHERE shortcut = 'physics' LIMIT 1),
       'Undergraduate', 'Mechanics and thermodynamics', TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE course_code = 'PHYS101');

INSERT IGNORE INTO courses (course_code, course_name, department_id, level, description, is_active, created_at, updated_at)
SELECT 'PHYS201', 'General Physics II',
       (SELECT id FROM departments WHERE shortcut = 'physics' LIMIT 1),
       'Undergraduate', 'Electricity and magnetism', TRUE, NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE course_code = 'PHYS201');

-- =====================================================
-- 6. SEED COURSE ASSIGNMENTS
-- =====================================================
-- Assign professors to courses for active semesters
-- Get current academic year's semesters
SET @current_academic_year_id = (SELECT id FROM academic_years WHERE is_active = TRUE LIMIT 1);
SET @first_semester_id = (SELECT id FROM semesters WHERE academic_year_id = @current_academic_year_id AND type = 'FIRST' LIMIT 1);
SET @second_semester_id = (SELECT id FROM semesters WHERE academic_year_id = @current_academic_year_id AND type = 'SECOND' LIMIT 1);

-- CS Courses Assignments
INSERT IGNORE INTO course_assignments (semester_id, course_id, professor_id, is_active, created_at, updated_at)
SELECT @first_semester_id, c.id, u.id, TRUE, NOW(), NULL
FROM courses c
CROSS JOIN users u
WHERE c.course_code IN ('CS101', 'CS201', 'CS301', 'CS401', 'CS501')
  AND u.email IN ('prof1@alquds.edu', 'prof2@alquds.edu', 'prof6@alquds.edu')
  AND (c.course_code = 'CS101' AND u.email = 'prof1@alquds.edu' OR
       c.course_code = 'CS201' AND u.email = 'prof2@alquds.edu' OR
       c.course_code = 'CS301' AND u.email = 'prof1@alquds.edu' OR
       c.course_code = 'CS401' AND u.email = 'prof6@alquds.edu' OR
       c.course_code = 'CS501' AND u.email = 'prof2@alquds.edu')
  AND NOT EXISTS (
      SELECT 1 FROM course_assignments ca 
      WHERE ca.semester_id = @first_semester_id 
        AND ca.course_id = c.id 
        AND ca.professor_id = u.id
  );

-- Math Courses Assignments
INSERT IGNORE INTO course_assignments (semester_id, course_id, professor_id, is_active, created_at, updated_at)
SELECT @first_semester_id, c.id, u.id, TRUE, NOW(), NULL
FROM courses c
CROSS JOIN users u
WHERE c.course_code IN ('MATH101', 'MATH201', 'MATH301')
  AND u.email IN ('prof3@alquds.edu', 'prof4@alquds.edu')
  AND (c.course_code = 'MATH101' AND u.email = 'prof3@alquds.edu' OR
       c.course_code = 'MATH201' AND u.email = 'prof4@alquds.edu' OR
       c.course_code = 'MATH301' AND u.email = 'prof3@alquds.edu')
  AND NOT EXISTS (
      SELECT 1 FROM course_assignments ca 
      WHERE ca.semester_id = @first_semester_id 
        AND ca.course_id = c.id 
        AND ca.professor_id = u.id
  );

-- Physics Courses Assignments
INSERT IGNORE INTO course_assignments (semester_id, course_id, professor_id, is_active, created_at, updated_at)
SELECT @first_semester_id, c.id, u.id, TRUE, NOW(), NULL
FROM courses c
CROSS JOIN users u
WHERE c.course_code IN ('PHYS101', 'PHYS201')
  AND u.email = 'prof5@alquds.edu'
  AND NOT EXISTS (
      SELECT 1 FROM course_assignments ca 
      WHERE ca.semester_id = @first_semester_id 
        AND ca.course_id = c.id 
        AND ca.professor_id = u.id
  );

-- =====================================================
-- 7. SEED REQUIRED DOCUMENT TYPES
-- =====================================================
-- Create required document types for courses in active semesters
INSERT IGNORE INTO required_document_types (course_id, semester_id, document_type, deadline, is_required, max_file_count, max_total_size_mb, created_at, updated_at)
SELECT c.id, s.id, 'SYLLABUS',
       DATE_ADD(s.start_date, INTERVAL 14 DAY),
       TRUE, 5, 50, NOW(), NULL
FROM courses c
CROSS JOIN semesters s
WHERE s.academic_year_id = @current_academic_year_id
  AND s.type IN ('FIRST', 'SECOND')
  AND NOT EXISTS (
      SELECT 1 FROM required_document_types rdt
      WHERE rdt.course_id = c.id 
        AND rdt.semester_id = s.id 
        AND rdt.document_type = 'SYLLABUS'
  );

INSERT IGNORE INTO required_document_types (course_id, semester_id, document_type, deadline, is_required, max_file_count, max_total_size_mb, created_at, updated_at)
SELECT c.id, s.id, 'EXAM',
       DATE_ADD(s.start_date, INTERVAL 30 DAY),
       TRUE, 5, 50, NOW(), NULL
FROM courses c
CROSS JOIN semesters s
WHERE s.academic_year_id = @current_academic_year_id
  AND s.type IN ('FIRST', 'SECOND')
  AND NOT EXISTS (
      SELECT 1 FROM required_document_types rdt
      WHERE rdt.course_id = c.id 
        AND rdt.semester_id = s.id 
        AND rdt.document_type = 'EXAM'
  );

INSERT IGNORE INTO required_document_types (course_id, semester_id, document_type, deadline, is_required, max_file_count, max_total_size_mb, created_at, updated_at)
SELECT c.id, s.id, 'ASSIGNMENT',
       DATE_ADD(s.start_date, INTERVAL 30 DAY),
       TRUE, 5, 50, NOW(), NULL
FROM courses c
CROSS JOIN semesters s
WHERE s.academic_year_id = @current_academic_year_id
  AND s.type IN ('FIRST', 'SECOND')
  AND NOT EXISTS (
      SELECT 1 FROM required_document_types rdt
      WHERE rdt.course_id = c.id 
        AND rdt.semester_id = s.id 
        AND rdt.document_type = 'ASSIGNMENT'
  );

INSERT IGNORE INTO required_document_types (course_id, semester_id, document_type, deadline, is_required, max_file_count, max_total_size_mb, created_at, updated_at)
SELECT c.id, s.id, 'LECTURE_NOTES',
       DATE_ADD(s.start_date, INTERVAL 30 DAY),
       TRUE, 5, 50, NOW(), NULL
FROM courses c
CROSS JOIN semesters s
WHERE s.academic_year_id = @current_academic_year_id
  AND s.type IN ('FIRST', 'SECOND')
  AND NOT EXISTS (
      SELECT 1 FROM required_document_types rdt
      WHERE rdt.course_id = c.id 
        AND rdt.semester_id = s.id 
        AND rdt.document_type = 'LECTURE_NOTES'
  );

-- =====================================================
-- 8. SEED FOLDERS (Hierarchical Structure)
-- =====================================================
-- Note: Folders table must exist. This creates the folder hierarchy:
-- {yearCode}/{semesterType}/{professorName}/{courseCode - courseName}/{subfolder}

-- Create year root folders
INSERT IGNORE INTO folders (path, name, type, parent_id, owner_id, academic_year_id, semester_id, course_id, created_at)
SELECT CONCAT(ay.year_code, '/'), ay.year_code, 'YEAR_ROOT', NULL, NULL, ay.id, NULL, NULL, NOW()
FROM academic_years ay
WHERE NOT EXISTS (SELECT 1 FROM folders WHERE path = CONCAT(ay.year_code, '/'));

-- Create semester root folders
INSERT IGNORE INTO folders (path, name, type, parent_id, owner_id, academic_year_id, semester_id, course_id, created_at)
SELECT CONCAT(ay.year_code, '/', LOWER(s.type), '/'),
       LOWER(s.type), 'SEMESTER_ROOT',
       (SELECT id FROM folders WHERE path = CONCAT(ay.year_code, '/') LIMIT 1),
       NULL, ay.id, s.id, NULL, NOW()
FROM semesters s
JOIN academic_years ay ON s.academic_year_id = ay.id
WHERE NOT EXISTS (
    SELECT 1 FROM folders 
    WHERE path = CONCAT(ay.year_code, '/', LOWER(s.type), '/')
);

-- Create professor root folders for active semesters
INSERT IGNORE INTO folders (path, name, type, parent_id, owner_id, academic_year_id, semester_id, course_id, created_at)
SELECT CONCAT(ay.year_code, '/', LOWER(s.type), '/', CONCAT(u.first_name, ' ', u.last_name), '/'),
       CONCAT(u.first_name, ' ', u.last_name), 'PROFESSOR_ROOT',
       (SELECT id FROM folders WHERE path = CONCAT(ay.year_code, '/', LOWER(s.type), '/') LIMIT 1),
       u.id, ay.id, s.id, NULL, NOW()
FROM users u
CROSS JOIN semesters s
JOIN academic_years ay ON s.academic_year_id = ay.id
WHERE u.role = 'ROLE_PROFESSOR'
  AND s.academic_year_id = @current_academic_year_id
  AND s.type IN ('FIRST', 'SECOND')
  AND NOT EXISTS (
      SELECT 1 FROM folders f
      WHERE f.path = CONCAT(ay.year_code, '/', LOWER(s.type), '/', CONCAT(u.first_name, ' ', u.last_name), '/')
        AND f.owner_id = u.id
        AND f.academic_year_id = ay.id
        AND f.semester_id = s.id
  );

-- Create course folders
INSERT IGNORE INTO folders (path, name, type, parent_id, owner_id, academic_year_id, semester_id, course_id, created_at)
SELECT CONCAT(pf.path, c.course_code, ' - ', c.course_name, '/'),
       CONCAT(c.course_code, ' - ', c.course_name), 'COURSE',
       pf.id, ca.professor_id, ay.id, s.id, c.id, NOW()
FROM course_assignments ca
JOIN courses c ON ca.course_id = c.id
JOIN semesters s ON ca.semester_id = s.id
JOIN academic_years ay ON s.academic_year_id = ay.id
JOIN folders pf ON pf.owner_id = ca.professor_id 
                AND pf.academic_year_id = ay.id 
                AND pf.semester_id = s.id 
                AND pf.type = 'PROFESSOR_ROOT'
WHERE NOT EXISTS (
    SELECT 1 FROM folders f
    WHERE f.course_id = c.id
      AND f.owner_id = ca.professor_id
      AND f.academic_year_id = ay.id
      AND f.semester_id = s.id
      AND f.type = 'COURSE'
);

-- Create standard subfolders (Syllabus, Exams, Course Notes, Assignments)
INSERT IGNORE INTO folders (path, name, type, parent_id, owner_id, academic_year_id, semester_id, course_id, created_at)
SELECT CONCAT(cf.path, 'Syllabus/'), 'Syllabus', 'SUBFOLDER',
       cf.id, cf.owner_id, cf.academic_year_id, cf.semester_id, cf.course_id, NOW()
FROM folders cf
WHERE cf.type = 'COURSE'
  AND NOT EXISTS (
      SELECT 1 FROM folders f
      WHERE f.parent_id = cf.id AND f.name = 'Syllabus'
  );

INSERT IGNORE INTO folders (path, name, type, parent_id, owner_id, academic_year_id, semester_id, course_id, created_at)
SELECT CONCAT(cf.path, 'Exams/'), 'Exams', 'SUBFOLDER',
       cf.id, cf.owner_id, cf.academic_year_id, cf.semester_id, cf.course_id, NOW()
FROM folders cf
WHERE cf.type = 'COURSE'
  AND NOT EXISTS (
      SELECT 1 FROM folders f
      WHERE f.parent_id = cf.id AND f.name = 'Exams'
  );

INSERT IGNORE INTO folders (path, name, type, parent_id, owner_id, academic_year_id, semester_id, course_id, created_at)
SELECT CONCAT(cf.path, 'Course Notes/'), 'Course Notes', 'SUBFOLDER',
       cf.id, cf.owner_id, cf.academic_year_id, cf.semester_id, cf.course_id, NOW()
FROM folders cf
WHERE cf.type = 'COURSE'
  AND NOT EXISTS (
      SELECT 1 FROM folders f
      WHERE f.parent_id = cf.id AND f.name = 'Course Notes'
  );

INSERT IGNORE INTO folders (path, name, type, parent_id, owner_id, academic_year_id, semester_id, course_id, created_at)
SELECT CONCAT(cf.path, 'Assignments/'), 'Assignments', 'SUBFOLDER',
       cf.id, cf.owner_id, cf.academic_year_id, cf.semester_id, cf.course_id, NOW()
FROM folders cf
WHERE cf.type = 'COURSE'
  AND NOT EXISTS (
      SELECT 1 FROM folders f
      WHERE f.parent_id = cf.id AND f.name = 'Assignments'
  );

-- =====================================================
-- 9. SEED SAMPLE DOCUMENT SUBMISSIONS
-- =====================================================
-- Create sample document submissions for course assignments
INSERT IGNORE INTO document_submissions (course_assignment_id, document_type, professor_id, submitted_at, is_late_submission, status, notes, file_count, total_file_size, created_at, updated_at)
SELECT ca.id, 'SYLLABUS', ca.professor_id,
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
       FALSE, 'UPLOADED', 'Sample syllabus submission', 1, 5242880, NOW(), NULL
FROM course_assignments ca
WHERE NOT EXISTS (
    SELECT 1 FROM document_submissions ds
    WHERE ds.course_assignment_id = ca.id
      AND ds.document_type = 'SYLLABUS'
)
LIMIT 5;

INSERT IGNORE INTO document_submissions (course_assignment_id, document_type, professor_id, submitted_at, is_late_submission, status, notes, file_count, total_file_size, created_at, updated_at)
SELECT ca.id, 'EXAM', ca.professor_id,
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 20) DAY),
       FALSE, 'UPLOADED', 'Sample exam submission', 2, 10485760, NOW(), NULL
FROM course_assignments ca
WHERE NOT EXISTS (
    SELECT 1 FROM document_submissions ds
    WHERE ds.course_assignment_id = ca.id
      AND ds.document_type = 'EXAM'
)
LIMIT 5;

INSERT IGNORE INTO document_submissions (course_assignment_id, document_type, professor_id, submitted_at, is_late_submission, status, notes, file_count, total_file_size, created_at, updated_at)
SELECT ca.id, 'ASSIGNMENT', ca.professor_id,
       NULL, FALSE, 'NOT_UPLOADED', NULL, 0, 0, NOW(), NULL
FROM course_assignments ca
WHERE NOT EXISTS (
    SELECT 1 FROM document_submissions ds
    WHERE ds.course_assignment_id = ca.id
      AND ds.document_type = 'ASSIGNMENT'
)
LIMIT 5;

-- =====================================================
-- MIGRATION COMPLETE
-- =====================================================
-- This migration has seeded:
-- - 5 Departments
-- - 13 Users (1 Admin, 1 Deanship, 3 HODs, 8 Professors)
-- - 5 Academic Years
-- - 15 Semesters (3 per year)
-- - 10 Courses
-- - Multiple Course Assignments
-- - Required Document Types for all courses
-- - Complete folder hierarchy
-- - Sample document submissions
-- =====================================================
