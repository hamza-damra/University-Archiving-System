-- Migration script for multi-file upload support
-- Version: V2__Add_Multi_File_Support.sql
-- MySQL compatible version (idempotent)

-- Add multi-file support columns to document_requests table (if they don't exist)
SET @col_exists_1 = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'document_requests' AND COLUMN_NAME = 'max_file_count');
SET @sql_1 = IF(@col_exists_1 = 0, 
    'ALTER TABLE document_requests ADD COLUMN max_file_count INTEGER DEFAULT 5', 
    'SELECT "Column max_file_count already exists" AS message');
PREPARE stmt_1 FROM @sql_1;
EXECUTE stmt_1;
DEALLOCATE PREPARE stmt_1;

SET @col_exists_2 = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'document_requests' AND COLUMN_NAME = 'max_total_size_mb');
SET @sql_2 = IF(@col_exists_2 = 0, 
    'ALTER TABLE document_requests ADD COLUMN max_total_size_mb INTEGER DEFAULT 50', 
    'SELECT "Column max_total_size_mb already exists" AS message');
PREPARE stmt_2 FROM @sql_2;
EXECUTE stmt_2;
DEALLOCATE PREPARE stmt_2;

-- Add multi-file support columns to submitted_documents table (if they don't exist)
SET @col_exists_3 = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'submitted_documents' AND COLUMN_NAME = 'total_file_size');
SET @sql_3 = IF(@col_exists_3 = 0, 
    'ALTER TABLE submitted_documents ADD COLUMN total_file_size BIGINT', 
    'SELECT "Column total_file_size already exists" AS message');
PREPARE stmt_3 FROM @sql_3;
EXECUTE stmt_3;
DEALLOCATE PREPARE stmt_3;

SET @col_exists_4 = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'submitted_documents' AND COLUMN_NAME = 'file_count');
SET @sql_4 = IF(@col_exists_4 = 0, 
    'ALTER TABLE submitted_documents ADD COLUMN file_count INTEGER DEFAULT 0', 
    'SELECT "Column file_count already exists" AS message');
PREPARE stmt_4 FROM @sql_4;
EXECUTE stmt_4;
DEALLOCATE PREPARE stmt_4;

SET @col_exists_5 = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'submitted_documents' AND COLUMN_NAME = 'notes');
SET @sql_5 = IF(@col_exists_5 = 0, 
    'ALTER TABLE submitted_documents ADD COLUMN notes VARCHAR(1000)', 
    'SELECT "Column notes already exists" AS message');
PREPARE stmt_5 FROM @sql_5;
EXECUTE stmt_5;
DEALLOCATE PREPARE stmt_5;

-- Make single file columns nullable for backward compatibility
ALTER TABLE submitted_documents
MODIFY COLUMN file_url VARCHAR(500) NULL,
MODIFY COLUMN original_filename VARCHAR(255) NULL;

-- Create file_attachments table for multi-file support
CREATE TABLE IF NOT EXISTS file_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submitted_document_id BIGINT NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(100),
    file_order INTEGER DEFAULT 0,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_file_attachment_submitted_document 
        FOREIGN KEY (submitted_document_id) 
        REFERENCES submitted_documents(id) 
        ON DELETE CASCADE
);

-- Create indexes for better query performance (if they don't exist)
SET @index_exists_1 = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'file_attachments' 
    AND INDEX_NAME = 'idx_file_attachments_submitted_document_id');
SET @sql_idx_1 = IF(@index_exists_1 = 0, 
    'CREATE INDEX idx_file_attachments_submitted_document_id ON file_attachments(submitted_document_id)', 
    'SELECT "Index idx_file_attachments_submitted_document_id already exists" AS message');
PREPARE stmt_idx_1 FROM @sql_idx_1;
EXECUTE stmt_idx_1;
DEALLOCATE PREPARE stmt_idx_1;

SET @index_exists_2 = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'file_attachments' 
    AND INDEX_NAME = 'idx_file_attachments_file_order');
SET @sql_idx_2 = IF(@index_exists_2 = 0, 
    'CREATE INDEX idx_file_attachments_file_order ON file_attachments(submitted_document_id, file_order)', 
    'SELECT "Index idx_file_attachments_file_order already exists" AS message');
PREPARE stmt_idx_2 FROM @sql_idx_2;
EXECUTE stmt_idx_2;
DEALLOCATE PREPARE stmt_idx_2;

-- Update existing submitted_documents to have file_count = 1 if they have files
UPDATE submitted_documents 
SET file_count = 1, 
    total_file_size = file_size
WHERE file_url IS NOT NULL AND (file_count = 0 OR file_count IS NULL);
