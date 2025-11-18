-- =====================================================
-- Additional Performance Indexes Migration
-- =====================================================
-- This migration adds additional performance indexes beyond the basic
-- foreign key indexes created in V3 and V4. These indexes optimize
-- common query patterns in the semester-based file system.
--
-- Indexes added:
-- - Composite indexes for common multi-column queries
-- - Indexes for filtering and sorting operations
-- - Indexes for report generation queries
-- =====================================================

-- =====================================================
-- 1. COURSE_ASSIGNMENTS COMPOSITE INDEXES
-- =====================================================
-- Composite index for finding active assignments by semester
-- Used in: HOD dashboard, Professor dashboard, Deanship reports
CREATE INDEX IF NOT EXISTS idx_course_assignments_semester_active 
ON course_assignments(semester_id, is_active);

-- Composite index for finding professor's active assignments in a semester
-- Used in: Professor dashboard to show current courses
CREATE INDEX IF NOT EXISTS idx_course_assignments_professor_semester_active 
ON course_assignments(professor_id, semester_id, is_active);

-- Composite index for finding course assignments by department (via course)
-- Used in: HOD department-scoped queries
CREATE INDEX IF NOT EXISTS idx_course_assignments_course_semester 
ON course_assignments(course_id, semester_id);

-- =====================================================
-- 2. DOCUMENT_SUBMISSIONS COMPOSITE INDEXES
-- =====================================================
-- Composite index for finding submissions by status and semester
-- Used in: HOD reports showing overdue/missing submissions
CREATE INDEX IF NOT EXISTS idx_document_submissions_status_course_assignment 
ON document_submissions(status, course_assignment_id);

-- Composite index for professor's submissions in a semester
-- Used in: Professor dashboard showing submission status
CREATE INDEX IF NOT EXISTS idx_document_submissions_professor_status 
ON document_submissions(professor_id, status);

-- Composite index for finding submissions by document type and status
-- Used in: Reports filtering by document type
CREATE INDEX IF NOT EXISTS idx_document_submissions_document_type_status 
ON document_submissions(document_type, status);

-- Composite index for finding late submissions
-- Used in: Reports showing late submissions
CREATE INDEX IF NOT EXISTS idx_document_submissions_late_submission 
ON document_submissions(is_late_submission, submitted_at);

-- =====================================================
-- 3. UPLOADED_FILES COMPOSITE INDEXES
-- =====================================================
-- Composite index for retrieving files in order for a submission
-- Used in: File explorer and file download operations
-- Note: This is already partially covered by idx_uploaded_files_file_order in V3
-- Adding a covering index with additional metadata
CREATE INDEX IF NOT EXISTS idx_uploaded_files_submission_order_metadata 
ON uploaded_files(document_submission_id, file_order, created_at);

-- =====================================================
-- 4. SEMESTERS COMPOSITE INDEXES
-- =====================================================
-- Composite index for finding active semesters by year
-- Used in: Dashboard semester selectors
CREATE INDEX IF NOT EXISTS idx_semesters_year_active 
ON semesters(academic_year_id, is_active);

-- Composite index for finding semesters by type and year
-- Used in: Filtering by semester type (FIRST, SECOND, SUMMER)
CREATE INDEX IF NOT EXISTS idx_semesters_year_type_active 
ON semesters(academic_year_id, type, is_active);

-- Index for date range queries (finding current semester)
-- Used in: Determining active semester based on current date
CREATE INDEX IF NOT EXISTS idx_semesters_date_range 
ON semesters(start_date, end_date);

-- =====================================================
-- 5. COURSES COMPOSITE INDEXES
-- =====================================================
-- Composite index for finding active courses by department
-- Used in: HOD viewing department courses, Deanship course management
CREATE INDEX IF NOT EXISTS idx_courses_department_active 
ON courses(department_id, is_active);

-- Index for course code lookups (case-insensitive searches)
-- Note: course_code already has an index in V3, but adding for completeness
-- This is useful for autocomplete and search features

-- =====================================================
-- 6. REQUIRED_DOCUMENT_TYPES COMPOSITE INDEXES
-- =====================================================
-- Composite index for finding required documents by course and semester
-- Used in: Professor dashboard showing required documents for a course
CREATE INDEX IF NOT EXISTS idx_required_doc_types_course_semester 
ON required_document_types(course_id, semester_id);

-- Composite index for finding documents with upcoming deadlines
-- Used in: Notification system and deadline reminders
CREATE INDEX IF NOT EXISTS idx_required_doc_types_deadline 
ON required_document_types(deadline, is_required);

-- Composite index for filtering by document type and course
-- Used in: Reports filtering by specific document types
CREATE INDEX IF NOT EXISTS idx_required_doc_types_type_course 
ON required_document_types(document_type, course_id);

-- =====================================================
-- 7. USERS COMPOSITE INDEXES
-- =====================================================
-- Composite index for finding active professors by department
-- Used in: HOD viewing department professors, Deanship professor management
CREATE INDEX IF NOT EXISTS idx_users_department_role_active 
ON users(department_id, role, is_active);

-- Composite index for finding active users by role
-- Used in: Role-based user listings
CREATE INDEX IF NOT EXISTS idx_users_role_active 
ON users(role, is_active);

-- =====================================================
-- 8. ACADEMIC_YEARS ADDITIONAL INDEXES
-- =====================================================
-- Index for finding years by start/end year range
-- Used in: Year selection and filtering
CREATE INDEX IF NOT EXISTS idx_academic_years_year_range 
ON academic_years(start_year, end_year);

-- =====================================================
-- PERFORMANCE OPTIMIZATION NOTES
-- =====================================================
-- 1. Composite indexes are ordered by selectivity (most selective first)
-- 2. Indexes support common query patterns in the application:
--    - Department-scoped filtering (HOD queries)
--    - Semester-based filtering (all dashboards)
--    - Status-based filtering (reports)
--    - Date range queries (deadline tracking)
-- 3. All indexes use IF NOT EXISTS for idempotency
-- 4. Indexes are created on InnoDB tables for optimal performance
-- 5. Consider monitoring index usage and removing unused indexes
-- 6. Regular ANALYZE TABLE operations recommended for optimal query planning
-- =====================================================

-- =====================================================
-- INDEX USAGE RECOMMENDATIONS
-- =====================================================
-- To monitor index usage, run:
-- SELECT * FROM sys.schema_unused_indexes WHERE object_schema = 'your_database';
--
-- To analyze table statistics, run:
-- ANALYZE TABLE course_assignments, document_submissions, uploaded_files;
--
-- To check index cardinality, run:
-- SHOW INDEX FROM course_assignments;
-- =====================================================
