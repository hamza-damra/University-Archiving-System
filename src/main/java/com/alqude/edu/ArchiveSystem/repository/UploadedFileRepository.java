package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    
    /**
     * Find a file by ID with uploader eagerly fetched.
     * 
     * @param id the ID of the file
     * @return optional uploaded file with uploader loaded
     */
    @Query("SELECT f FROM UploadedFile f LEFT JOIN FETCH f.uploader WHERE f.id = :id")
    Optional<UploadedFile> findByIdWithUploader(@Param("id") Long id);
    
    List<UploadedFile> findByDocumentSubmissionId(Long documentSubmissionId);
    
    List<UploadedFile> findByDocumentSubmissionIdOrderByFileOrderAsc(Long documentSubmissionId);
    
    /**
     * Find all files in a specific folder.
     * 
     * @param folderId the ID of the folder
     * @return list of uploaded files in the folder
     */
    List<UploadedFile> findByFolderId(Long folderId);
    
    /**
     * Find all files uploaded by a specific user.
     * 
     * @param uploaderId the ID of the uploader
     * @return list of uploaded files by the user
     */
    List<UploadedFile> findByUploaderId(Long uploaderId);
    
    /**
     * Find a file by folder ID and stored filename for duplicate checking.
     * 
     * @param folderId the ID of the folder
     * @param storedFilename the stored filename
     * @return optional uploaded file if found
     */
    Optional<UploadedFile> findByFolderIdAndStoredFilename(Long folderId, String storedFilename);
    
    /**
     * Count the number of files in a specific folder.
     * 
     * @param folderId the ID of the folder
     * @return count of files in the folder
     */
    long countByFolderId(Long folderId);
}
