package com.alquds.edu.ArchiveSystem.repository.file;

import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;

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
    
    /**
     * Find a file by ID with uploader and folder eagerly fetched for permission checking.
     * 
     * @param id the ID of the file
     * @return optional uploaded file with uploader and folder loaded
     */
    @Query("SELECT f FROM UploadedFile f " +
           "LEFT JOIN FETCH f.uploader u " +
           "LEFT JOIN FETCH u.department " +
           "LEFT JOIN FETCH f.folder " +
           "WHERE f.id = :id")
    Optional<UploadedFile> findByIdWithUploaderAndFolder(@Param("id") Long id);
    
    List<UploadedFile> findByDocumentSubmissionId(Long documentSubmissionId);
    
    List<UploadedFile> findByDocumentSubmissionIdOrderByFileOrderAsc(Long documentSubmissionId);
    
    /**
     * Find all files for a document submission with uploader eagerly fetched.
     * 
     * @param documentSubmissionId the ID of the document submission
     * @return list of uploaded files with uploader data, ordered by file order
     */
    @Query("SELECT f FROM UploadedFile f LEFT JOIN FETCH f.uploader WHERE f.documentSubmission.id = :documentSubmissionId ORDER BY f.fileOrder ASC")
    List<UploadedFile> findByDocumentSubmissionIdWithUploaderOrderByFileOrderAsc(@Param("documentSubmissionId") Long documentSubmissionId);
    
    /**
     * Find all files in a specific folder.
     * 
     * @param folderId the ID of the folder
     * @return list of uploaded files in the folder
     */
    List<UploadedFile> findByFolderId(Long folderId);
    
    /**
     * Find all files in a specific folder with uploader eagerly fetched.
     * 
     * @param folderId the ID of the folder
     * @return list of uploaded files in the folder with uploader data
     */
    @Query("SELECT f FROM UploadedFile f LEFT JOIN FETCH f.uploader WHERE f.folder.id = :folderId")
    List<UploadedFile> findByFolderIdWithUploader(@Param("folderId") Long folderId);
    
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
    
    /**
     * Count the number of files uploaded by a specific user.
     * 
     * @param uploaderId the ID of the uploader
     * @return count of files uploaded by the user
     */
    long countByUploaderId(Long uploaderId);
    
    /**
     * Calculate total file size for files uploaded by a specific user.
     * 
     * @param uploaderId the ID of the uploader
     * @return total file size in bytes, or null if no files
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM UploadedFile f WHERE f.uploader.id = :uploaderId")
    Long sumFileSizeByUploaderId(@Param("uploaderId") Long uploaderId);
    
    /**
     * Find a file by its full file URL path.
     * Used for filesystem synchronization to match physical files with DB records.
     * 
     * @param fileUrl the full file URL/path
     * @return optional uploaded file if found
     */
    Optional<UploadedFile> findByFileUrl(String fileUrl);
    
    /**
     * Find a file by its stored filename (across all folders).
     * Used as fallback when file URL doesn't match.
     * 
     * @param storedFilename the stored filename
     * @return optional uploaded file if found
     */
    Optional<UploadedFile> findByStoredFilename(String storedFilename);
    
    /**
     * Find files by stored filename (may return multiple if same name in different folders).
     * 
     * @param storedFilename the stored filename
     * @return list of uploaded files with matching stored filename
     */
    List<UploadedFile> findAllByStoredFilename(String storedFilename);
}
