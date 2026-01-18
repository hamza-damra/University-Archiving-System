-- =====================================================
-- Task Tracking System Migration
-- =====================================================
-- This migration creates the Academic Task Tracking System tables:
-- - tasks: Main task entity with weighted distribution and progress tracking
-- - task_audit_log: Audit trail for status changes and approvals
-- =====================================================

-- =====================================================
-- 1. TASKS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT 'Task title (e.g., Midterm Exam, Syllabus)',
    description VARCHAR(2000) COMMENT 'Task description',
    weight_percentage INT NOT NULL COMMENT 'Weight percentage (must sum to 100% per course+semester+professor)',
    progress_percentage INT NOT NULL DEFAULT 0 COMMENT 'Progress percentage (0-100)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, IN_PROGRESS, COMPLETED, OVERDUE, APPROVED, REJECTED',
    deadline DATE NULL COMMENT 'Optional deadline date',
    file_reference_id BIGINT NULL COMMENT 'Reference to uploaded_files.id for linked file',
    professor_id BIGINT NOT NULL COMMENT 'References users.id (professor who created the task)',
    course_id BIGINT NOT NULL COMMENT 'References courses.id',
    semester_id BIGINT NOT NULL COMMENT 'References semesters.id',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_tasks_professor 
        FOREIGN KEY (professor_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_tasks_course 
        FOREIGN KEY (course_id) 
        REFERENCES courses(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_tasks_semester 
        FOREIGN KEY (semester_id) 
        REFERENCES semesters(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_tasks_file_reference 
        FOREIGN KEY (file_reference_id) 
        REFERENCES uploaded_files(id) 
        ON DELETE SET NULL,
    
    CONSTRAINT chk_task_weight_percentage 
        CHECK (weight_percentage >= 0 AND weight_percentage <= 100),
    
    CONSTRAINT chk_task_progress_percentage 
        CHECK (progress_percentage >= 0 AND progress_percentage <= 100),
    
    CONSTRAINT chk_task_status 
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'APPROVED', 'REJECTED')),
    
    INDEX idx_tasks_professor_id (professor_id),
    INDEX idx_tasks_course_id (course_id),
    INDEX idx_tasks_semester_id (semester_id),
    INDEX idx_tasks_status (status),
    INDEX idx_tasks_professor_course_semester (professor_id, course_id, semester_id),
    INDEX idx_tasks_deadline (deadline),
    INDEX idx_tasks_file_reference (file_reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Academic tasks with weighted distribution and progress tracking';

-- =====================================================
-- 2. TASK_AUDIT_LOG TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS task_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT 'References tasks.id',
    old_status VARCHAR(20) NULL COMMENT 'Previous status',
    new_status VARCHAR(20) NOT NULL COMMENT 'New status',
    changed_by_id BIGINT NOT NULL COMMENT 'References users.id (who made the change)',
    change_reason VARCHAR(500) NULL COMMENT 'Reason for status change or feedback',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_task_audit_log_task 
        FOREIGN KEY (task_id) 
        REFERENCES tasks(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_task_audit_log_changed_by 
        FOREIGN KEY (changed_by_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT chk_audit_log_old_status 
        CHECK (old_status IS NULL OR old_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'APPROVED', 'REJECTED')),
    
    CONSTRAINT chk_audit_log_new_status 
        CHECK (new_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'APPROVED', 'REJECTED')),
    
    INDEX idx_task_audit_log_task_id (task_id),
    INDEX idx_task_audit_log_changed_by (changed_by_id),
    INDEX idx_task_audit_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit trail for task status changes and approvals';

-- =====================================================
-- MIGRATION NOTES
-- =====================================================
-- 1. Weight percentage validation is enforced at application level
--    (SUM must equal 100% per professor+course+semester combination)
-- 2. Progress percentage is independent from status
-- 3. Status workflow: PENDING -> IN_PROGRESS -> COMPLETED -> APPROVED/REJECTED
-- 4. OVERDUE status is set automatically by scheduled job when deadline passes
-- 5. File reference is optional - tasks can exist without linked files
-- 6. Audit log tracks all status changes for compliance and accountability
-- =====================================================
