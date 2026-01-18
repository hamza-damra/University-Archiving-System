-- =====================================================
-- Academic Structure Tables Migration
-- =====================================================
-- This migration creates the new semester-based academic structure tables
-- for the Document Archiving System refactoring.
--
-- Tables created:
-- - academic_years: Academic year periods (e.g., 2024-2025)
-- - semesters: Three semesters per academic year (FIRST, SECOND, SUMMER)
-- - courses: Course catalog with codes and names
-- - course_assignments: Links professors to courses for specific semesters
-- - required_document_types: Document types required for courses
-- - document_submissions: Replaces submitted_documents with semester context
-- - uploaded_files: Replaces file_attachments with enhanced metadata
-- =====================================================

-- =====================================================
-- 1. ACADEMIC_YEARS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS academic_years (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    year_code VARCHAR(20) NOT NULL UNIQUE COMMENT 'e.g., 2024-2025',
    start_year INT NOT NULL COMMENT 'e.g., 2024',
    end_year INT NOT NULL COMMENT 'e.g., 2025',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Only one year should be active at a time',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_academic_years_year_code (year_code),
    INDEX idx_academic_years_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Academic year periods spanning multiple semesters';

-- =====================================================
-- 2. SEMESTERS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS semesters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'FIRST, SECOND, or SUMMER',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_semesters_academic_year 
        FOREIGN KEY (academic_year_id) 
        REFERENCES academic_years(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT chk_semester_type 
        CHECK (type IN ('FIRST', 'SECOND', 'SUMMER')),
    
    CONSTRAINT chk_semester_dates 
        CHECK (end_date > start_date),
    
    UNIQUE KEY uk_semester_year_type (academic_year_id, type),
    INDEX idx_semesters_academic_year (academic_year_id),
    INDEX idx_semesters_type (type),
    INDEX idx_semesters_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Three semesters per academic year: First/Fall, Second/Spring, Summer';

-- =====================================================
-- 3. COURSES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'e.g., CS101',
    course_name VARCHAR(255) NOT NULL COMMENT 'e.g., Database Systems',
    department_id BIGINT NOT NULL,
    level VARCHAR(50) COMMENT 'e.g., Undergraduate, Graduate',
    description VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_courses_department 
        FOREIGN KEY (department_id) 
        REFERENCES departments(id) 
        ON DELETE RESTRICT,
    
    INDEX idx_courses_course_code (course_code),
    INDEX idx_courses_department (department_id),
    INDEX idx_courses_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Course catalog with codes, names, and department associations';

-- =====================================================
-- 4. COURSE_ASSIGNMENTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS course_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    semester_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    professor_id BIGINT NOT NULL COMMENT 'References users table',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_course_assignments_semester 
        FOREIGN KEY (semester_id) 
        REFERENCES semesters(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_course_assignments_course 
        FOREIGN KEY (course_id) 
        REFERENCES courses(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_course_assignments_professor 
        FOREIGN KEY (professor_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    UNIQUE KEY uk_course_assignment (semester_id, course_id, professor_id),
    INDEX idx_course_assignments_semester (semester_id),
    INDEX idx_course_assignments_course (course_id),
    INDEX idx_course_assignments_professor (professor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Links professors to courses for specific semesters';

-- =====================================================
-- 5. REQUIRED_DOCUMENT_TYPES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS required_document_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    semester_id BIGINT NULL COMMENT 'Optional: specific to a semester',
    document_type VARCHAR(50) NOT NULL COMMENT 'SYLLABUS, EXAM, ASSIGNMENT, PROJECT_DOCS, LECTURE_NOTES, OTHER',
    deadline TIMESTAMP NULL COMMENT 'Optional deadline for submission',
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    max_file_count INT DEFAULT 5 COMMENT 'Maximum number of files allowed',
    max_total_size_mb INT DEFAULT 50 COMMENT 'Maximum total size in MB',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_required_doc_types_course 
        FOREIGN KEY (course_id) 
        REFERENCES courses(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_required_doc_types_semester 
        FOREIGN KEY (semester_id) 
        REFERENCES semesters(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT chk_document_type 
        CHECK (document_type IN ('SYLLABUS', 'EXAM', 'ASSIGNMENT', 'PROJECT_DOCS', 'LECTURE_NOTES', 'OTHER')),
    
    INDEX idx_required_doc_types_course (course_id),
    INDEX idx_required_doc_types_semester (semester_id),
    INDEX idx_required_doc_types_document_type (document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Document types required for courses with optional deadlines';

-- =====================================================
-- 6. ALLOWED_FILE_EXTENSIONS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS allowed_file_extensions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    required_document_type_id BIGINT NOT NULL,
    extension VARCHAR(20) NOT NULL COMMENT 'e.g., pdf, zip',
    
    CONSTRAINT fk_allowed_extensions_required_doc_type 
        FOREIGN KEY (required_document_type_id) 
        REFERENCES required_document_types(id) 
        ON DELETE CASCADE,
    
    INDEX idx_allowed_extensions_required_doc_type (required_document_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Allowed file extensions for each required document type';

-- =====================================================
-- 7. DOCUMENT_SUBMISSIONS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS document_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_assignment_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL COMMENT 'SYLLABUS, EXAM, ASSIGNMENT, PROJECT_DOCS, LECTURE_NOTES, OTHER',
    professor_id BIGINT NOT NULL COMMENT 'References users table',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_late_submission BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_UPLOADED' COMMENT 'NOT_UPLOADED, UPLOADED, OVERDUE',
    notes VARCHAR(1000),
    file_count INT NOT NULL DEFAULT 0,
    total_file_size BIGINT NOT NULL DEFAULT 0 COMMENT 'Total size in bytes',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_document_submissions_course_assignment 
        FOREIGN KEY (course_assignment_id) 
        REFERENCES course_assignments(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_document_submissions_professor 
        FOREIGN KEY (professor_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT chk_submission_document_type 
        CHECK (document_type IN ('SYLLABUS', 'EXAM', 'ASSIGNMENT', 'PROJECT_DOCS', 'LECTURE_NOTES', 'OTHER')),
    
    CONSTRAINT chk_submission_status 
        CHECK (status IN ('NOT_UPLOADED', 'UPLOADED', 'OVERDUE')),
    
    INDEX idx_document_submissions_course_assignment (course_assignment_id),
    INDEX idx_document_submissions_professor (professor_id),
    INDEX idx_document_submissions_document_type (document_type),
    INDEX idx_document_submissions_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Document submissions replacing submitted_documents with semester context';

-- =====================================================
-- 8. ADD FOREIGN KEYS TO FOLDERS TABLE
-- =====================================================
-- The folders table was created in V0 without academic FKs
-- Now we add the foreign keys to academic_years, semesters, and courses

-- Add FK to academic_years (if not exists)
SET @fk_exists_folder_year = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'folders' 
    AND CONSTRAINT_NAME = 'fk_folders_academic_year'
);

SET @sql_add_fk_folder_year = IF(
    @fk_exists_folder_year = 0, 
    'ALTER TABLE folders ADD CONSTRAINT fk_folders_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE SET NULL', 
    'SELECT ''FK fk_folders_academic_year already exists'' AS message'
);

PREPARE stmt_add_fk_folder_year FROM @sql_add_fk_folder_year;
EXECUTE stmt_add_fk_folder_year;
DEALLOCATE PREPARE stmt_add_fk_folder_year;

-- Add FK to semesters (if not exists)
SET @fk_exists_folder_semester = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'folders' 
    AND CONSTRAINT_NAME = 'fk_folders_semester'
);

SET @sql_add_fk_folder_semester = IF(
    @fk_exists_folder_semester = 0, 
    'ALTER TABLE folders ADD CONSTRAINT fk_folders_semester FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE SET NULL', 
    'SELECT ''FK fk_folders_semester already exists'' AS message'
);

PREPARE stmt_add_fk_folder_semester FROM @sql_add_fk_folder_semester;
EXECUTE stmt_add_fk_folder_semester;
DEALLOCATE PREPARE stmt_add_fk_folder_semester;

-- Add FK to courses (if not exists)
SET @fk_exists_folder_course = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'folders' 
    AND CONSTRAINT_NAME = 'fk_folders_course'
);

SET @sql_add_fk_folder_course = IF(
    @fk_exists_folder_course = 0, 
    'ALTER TABLE folders ADD CONSTRAINT fk_folders_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL', 
    'SELECT ''FK fk_folders_course already exists'' AS message'
);

PREPARE stmt_add_fk_folder_course FROM @sql_add_fk_folder_course;
EXECUTE stmt_add_fk_folder_course;
DEALLOCATE PREPARE stmt_add_fk_folder_course;

-- =====================================================
-- 9. ADD FK TO UPLOADED_FILES FOR DOCUMENT_SUBMISSION
-- =====================================================
-- Add document_submission_id FK to existing uploaded_files table

SET @fk_exists_uf_submission = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'uploaded_files' 
    AND CONSTRAINT_NAME = 'fk_uploaded_files_document_submission'
);

SET @sql_add_fk_uf_submission = IF(
    @fk_exists_uf_submission = 0, 
    'ALTER TABLE uploaded_files ADD CONSTRAINT fk_uploaded_files_document_submission FOREIGN KEY (document_submission_id) REFERENCES document_submissions(id) ON DELETE SET NULL', 
    'SELECT ''FK fk_uploaded_files_document_submission already exists'' AS message'
);

PREPARE stmt_add_fk_uf_submission FROM @sql_add_fk_uf_submission;
EXECUTE stmt_add_fk_uf_submission;
DEALLOCATE PREPARE stmt_add_fk_uf_submission;

-- =====================================================
-- MIGRATION NOTES
-- =====================================================
-- 1. Old tables (document_requests, submitted_documents, file_attachments) 
--    are kept for backward compatibility and data migration
-- 2. The new structure supports hierarchical organization:
--    Year → Semester → Professor → Course → Document Type → Files
-- 3. All foreign keys use CASCADE delete for data integrity
-- 4. Indexes are created for frequently queried columns
-- 5. Check constraints ensure data validity
-- 6. UTF8MB4 charset supports full Unicode including emojis
-- =====================================================
