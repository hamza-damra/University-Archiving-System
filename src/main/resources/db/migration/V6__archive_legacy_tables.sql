-- Migration script to archive legacy tables
-- Version: V6__archive_legacy_tables.sql
-- This script adds comments to legacy tables to indicate they are archived
-- and should not be used for new development

-- Add comments to legacy tables indicating they are archived
-- These tables are kept for rollback capability and historical data

-- document_requests table (legacy request-based system)
-- Replaced by: course_assignments + required_document_types
ALTER TABLE document_requests COMMENT = 
'LEGACY TABLE - Archived. Replaced by course_assignments and required_document_types. 
Data migrated to new semester-based structure. Keep for rollback capability.';

-- submitted_documents table (legacy submission system)
-- Replaced by: document_submissions
ALTER TABLE submitted_documents COMMENT = 
'LEGACY TABLE - Archived. Replaced by document_submissions. 
Data migrated to new semester-based structure. Keep for rollback capability.';

-- file_attachments table (legacy file storage)
-- Replaced by: uploaded_files
ALTER TABLE file_attachments COMMENT = 
'LEGACY TABLE - Archived. Replaced by uploaded_files. 
Data migrated to new semester-based structure. Keep for rollback capability.';

-- Add archived_at column to track when tables were archived
ALTER TABLE document_requests 
ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when this table was archived';

ALTER TABLE submitted_documents 
ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when this table was archived';

ALTER TABLE file_attachments 
ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when this table was archived';

-- Create a view for legacy data access if needed
-- This view can be used to query old data without directly accessing archived tables
CREATE OR REPLACE VIEW v_legacy_document_requests AS
SELECT 
    dr.id,
    dr.course_name,
    dr.document_type,
    dr.deadline,
    dr.professor_id,
    u.first_name AS professor_first_name,
    u.last_name AS professor_last_name,
    u.email AS professor_email,
    dr.hod_id,
    dr.status,
    dr.created_at,
    dr.updated_at,
    'LEGACY' AS data_source
FROM document_requests dr
LEFT JOIN users u ON dr.professor_id = u.id;

CREATE OR REPLACE VIEW v_legacy_submitted_documents AS
SELECT 
    sd.id,
    sd.document_request_id,
    sd.professor_id,
    u.first_name AS professor_first_name,
    u.last_name AS professor_last_name,
    sd.file_url,
    sd.original_filename,
    sd.file_size,
    sd.file_type,
    sd.file_count,
    sd.total_file_size,
    sd.notes,
    sd.submitted_at,
    sd.is_late_submission,
    sd.created_at,
    sd.updated_at,
    'LEGACY' AS data_source
FROM submitted_documents sd
LEFT JOIN users u ON sd.professor_id = u.id;

-- Add indexes to improve performance of legacy data queries
CREATE INDEX IF NOT EXISTS idx_document_requests_archived 
ON document_requests(archived_at);

CREATE INDEX IF NOT EXISTS idx_submitted_documents_archived 
ON submitted_documents(archived_at);

CREATE INDEX IF NOT EXISTS idx_file_attachments_archived 
ON file_attachments(archived_at);

-- Insert a record in a metadata table to track the migration
CREATE TABLE IF NOT EXISTS system_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metadata_key VARCHAR(255) NOT NULL UNIQUE,
    metadata_value TEXT,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO system_metadata (metadata_key, metadata_value, description)
VALUES (
    'legacy_tables_archived',
    'true',
    'Legacy tables (document_requests, submitted_documents, file_attachments) have been archived. Data migrated to semester-based structure.'
)
ON DUPLICATE KEY UPDATE 
    metadata_value = 'true',
    description = 'Legacy tables (document_requests, submitted_documents, file_attachments) have been archived. Data migrated to semester-based structure.',
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO system_metadata (metadata_key, metadata_value, description)
VALUES (
    'migration_to_semester_based_system',
    CURRENT_TIMESTAMP,
    'Timestamp when migration from request-based to semester-based system was completed.'
)
ON DUPLICATE KEY UPDATE 
    metadata_value = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP;

-- Add a comment to the database itself
-- Note: This is MySQL-specific syntax
-- ALTER DATABASE archive_system COMMENT = 'Document Archiving System - Migrated to semester-based structure on 2025-11-18';

