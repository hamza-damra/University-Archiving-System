package com.alquds.edu.ArchiveSystem.repository.file;

import com.alquds.edu.ArchiveSystem.entity.file.FileAttachment;

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
 * @see com.alquds.edu.ArchiveSystem.repository.UploadedFileRepository
 */
@Deprecated(since = "2.0", forRemoval = false)
@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {

    @Deprecated
    List<FileAttachment> findBySubmittedDocumentId(Long submittedDocumentId);

    @Deprecated
    List<FileAttachment> findBySubmittedDocumentIdOrderByFileOrderAsc(Long submittedDocumentId);

    @Deprecated
    @Query("SELECT fa FROM FileAttachment fa WHERE fa.submittedDocument.documentRequest.id = :requestId ORDER BY fa.fileOrder ASC")
    List<FileAttachment> findByDocumentRequestId(@Param("requestId") Long requestId);

    @Deprecated
    @Query("SELECT COUNT(fa) FROM FileAttachment fa WHERE fa.submittedDocument.id = :submittedDocumentId")
    long countBySubmittedDocumentId(@Param("submittedDocumentId") Long submittedDocumentId);

    @Deprecated
    void deleteBySubmittedDocumentId(Long submittedDocumentId);
}
