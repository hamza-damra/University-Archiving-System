package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * LEGACY REPOSITORY - ARCHIVED
 * 
 * This repository is part of the old request-based document system.
 * 
 * Replacement repository:
 * - UploadedFileRepository: For uploaded files in semester-based system
 * 
 * This repository is kept for:
 * 1. Historical data access
 * 2. Data migration operations
 * 3. Rollback capability
 * 
 * DO NOT USE FOR NEW DEVELOPMENT
 * 
 * @deprecated Replaced by UploadedFileRepository
 * @see com.alqude.edu.ArchiveSystem.repository.UploadedFileRepository
 */
@Deprecated(since = "2.0", forRemoval = false)
@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
    
    List<FileAttachment> findBySubmittedDocumentId(Long submittedDocumentId);
    
    List<FileAttachment> findBySubmittedDocumentIdOrderByFileOrderAsc(Long submittedDocumentId);
    
    @Query("SELECT fa FROM FileAttachment fa WHERE fa.submittedDocument.documentRequest.id = :requestId ORDER BY fa.fileOrder ASC")
    List<FileAttachment> findByDocumentRequestId(@Param("requestId") Long requestId);
    
    @Query("SELECT COUNT(fa) FROM FileAttachment fa WHERE fa.submittedDocument.id = :submittedDocumentId")
    long countBySubmittedDocumentId(@Param("submittedDocumentId") Long submittedDocumentId);
    
    void deleteBySubmittedDocumentId(Long submittedDocumentId);
}
