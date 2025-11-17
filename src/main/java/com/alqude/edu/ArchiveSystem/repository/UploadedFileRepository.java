package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    
    List<UploadedFile> findByDocumentSubmissionId(Long documentSubmissionId);
    
    List<UploadedFile> findByDocumentSubmissionIdOrderByFileOrderAsc(Long documentSubmissionId);
}
