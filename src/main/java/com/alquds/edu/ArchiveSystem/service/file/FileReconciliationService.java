package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.entity.file.Folder;
import com.alquds.edu.ArchiveSystem.entity.file.FolderType;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.repository.file.FolderRepository;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.util.SafePathResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for reconciling database records with the filesystem.
 * Runs periodically to ensure database metadata stays in sync with actual files.
 * 
 * Features:
 * - Removes DB records for files/folders that no longer exist on disk
 * - Can optionally add DB records for orphaned files (files on disk without DB record)
 * - Logs discrepancies for monitoring
 * - Thread-safe operations
 * 
 * Note: This service treats the filesystem as the source of truth.
 * Files that exist only in the database (orphaned records) are cleaned up.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileReconciliationService {

    private final UploadedFileRepository uploadedFileRepository;
    private final FolderRepository folderRepository;
    private final SafePathResolver pathResolver;
    private final FilesystemScanService filesystemScanService;

    @Value("${app.reconciliation.enabled:true}")
    private boolean reconciliationEnabled;

    @Value("${app.reconciliation.remove-orphaned-records:true}")
    private boolean removeOrphanedRecords;

    // Statistics for last reconciliation run
    private volatile LocalDateTime lastRunTime;
    private volatile int lastRunFilesScanned;
    private volatile int lastRunOrphanedFilesRemoved;
    private volatile int lastRunOrphanedFoldersRemoved;
    private volatile int lastRunFoldersRecreated;

    /**
     * Scheduled reconciliation job.
     * Runs every 15 minutes by default (configurable via cron expression).
     * 
     * Scans the filesystem and reconciles with database records.
     */
    @Scheduled(cron = "${app.reconciliation.cron:0 */15 * * * *}")
    public void scheduledReconciliation() {
        if (!reconciliationEnabled) {
            log.debug("Reconciliation is disabled, skipping scheduled run");
            return;
        }

        log.info("Starting scheduled filesystem reconciliation");
        ReconciliationResult result = reconcile();
        log.info("Reconciliation completed: {} files scanned, {} orphaned files removed, {} orphaned folders removed, {} primary folders recreated",
                result.filesScanned, result.orphanedFilesRemoved, result.orphanedFoldersRemoved, result.foldersRecreated);
    }

    /**
     * Manually trigger reconciliation.
     * 
     * @return ReconciliationResult with statistics
     */
    @Transactional
    public ReconciliationResult reconcile() {
        lastRunTime = LocalDateTime.now();
        
        ReconciliationResult result = new ReconciliationResult();
        result.startTime = LocalDateTime.now();

        try {
            // Step 1: Check all file records in DB against filesystem
            result.filesScanned = reconcileFiles(result);
            
            // Step 2: Check all folder records in DB against filesystem
            // This will also recreate missing primary folders
            result.foldersScanned = reconcileFolders(result);
            
            // Step 3: Invalidate caches if any changes were made
            if (result.orphanedFilesRemoved > 0 || result.orphanedFoldersRemoved > 0 || result.foldersRecreated > 0) {
                log.info("Invalidating all caches after reconciliation changes");
                filesystemScanService.invalidateCache("");
            }
            
        } catch (Exception e) {
            log.error("Error during reconciliation", e);
            result.errors.add("Reconciliation error: " + e.getMessage());
        }

        result.endTime = LocalDateTime.now();
        
        // Update statistics
        lastRunFilesScanned = result.filesScanned;
        lastRunOrphanedFilesRemoved = result.orphanedFilesRemoved;
        lastRunOrphanedFoldersRemoved = result.orphanedFoldersRemoved;
        lastRunFoldersRecreated = result.foldersRecreated;

        return result;
    }

    /**
     * Reconcile file records with filesystem.
     * Removes DB records for files that don't exist on disk.
     */
    private int reconcileFiles(ReconciliationResult result) {
        List<UploadedFile> allFiles = uploadedFileRepository.findAll();
        int scanned = 0;

        for (UploadedFile file : allFiles) {
            scanned++;
            String fileUrl = file.getFileUrl();
            
            if (fileUrl == null || fileUrl.isEmpty()) {
                log.warn("File ID {} has null or empty fileUrl", file.getId());
                result.warnings.add("File ID " + file.getId() + " has null fileUrl");
                continue;
            }

            try {
                // Check if file exists on disk
                Path filePath = pathResolver.resolve(fileUrl);
                
                if (!Files.exists(filePath)) {
                    log.info("File no longer exists on disk: {} (DB ID: {})", fileUrl, file.getId());
                    result.orphanedFiles.add(fileUrl);
                    
                    if (removeOrphanedRecords) {
                        uploadedFileRepository.delete(file);
                        result.orphanedFilesRemoved++;
                        log.info("Removed orphaned file record: {}", file.getId());
                    }
                } else if (!Files.isRegularFile(filePath)) {
                    log.warn("File URL points to non-file: {} (DB ID: {})", fileUrl, file.getId());
                    result.warnings.add("File ID " + file.getId() + " points to non-file: " + fileUrl);
                }
            } catch (Exception e) {
                log.warn("Error checking file {}: {}", fileUrl, e.getMessage());
                result.warnings.add("Error checking file " + fileUrl + ": " + e.getMessage());
            }
        }

        return scanned;
    }

    /**
     * Reconcile folder records with filesystem.
     * For primary folders (YEAR_ROOT, SEMESTER_ROOT, PROFESSOR_ROOT, COURSE, SUBFOLDER):
     *   - Recreates the physical folder if it's missing
     * For custom folders:
     *   - Removes DB records for folders that don't exist on disk
     */
    private int reconcileFolders(ReconciliationResult result) {
        List<Folder> allFolders = folderRepository.findAll();
        int scanned = 0;

        // Sort by path length ascending to process parents first (for recreation)
        allFolders.sort((a, b) -> {
            String pathA = a.getPath() != null ? a.getPath() : "";
            String pathB = b.getPath() != null ? b.getPath() : "";
            return Integer.compare(pathA.length(), pathB.length());
        });

        for (Folder folder : allFolders) {
            scanned++;
            String folderPath = folder.getPath();
            
            if (folderPath == null || folderPath.isEmpty()) {
                log.debug("Folder ID {} has null or empty path (may be root folder)", folder.getId());
                continue;
            }

            try {
                // Check if folder exists on disk
                Path dirPath = pathResolver.resolve(folderPath);
                
                if (!Files.exists(dirPath)) {
                    // Check if this is a primary folder that should be recreated
                    if (isPrimaryFolder(folder)) {
                        // Recreate the folder immediately
                        log.info("Primary folder missing on disk, recreating: {} (type: {}, DB ID: {})", 
                                folderPath, folder.getType(), folder.getId());
                        
                        try {
                            Files.createDirectories(dirPath);
                            result.foldersRecreated++;
                            result.recreatedFolders.add(folderPath);
                            log.info("Successfully recreated primary folder: {}", folderPath);
                        } catch (IOException e) {
                            log.error("Failed to recreate primary folder: {}", folderPath, e);
                            result.errors.add("Failed to recreate folder " + folderPath + ": " + e.getMessage());
                        }
                    } else {
                        // Custom folder - can be safely removed from DB
                        log.info("Custom folder no longer exists on disk: {} (DB ID: {})", folderPath, folder.getId());
                        result.orphanedFolders.add(folderPath);
                        
                        if (removeOrphanedRecords) {
                            folderRepository.delete(folder);
                            result.orphanedFoldersRemoved++;
                            log.info("Removed orphaned custom folder record: {}", folder.getId());
                        }
                    }
                } else if (!Files.isDirectory(dirPath)) {
                    log.warn("Folder path points to non-directory: {} (DB ID: {})", folderPath, folder.getId());
                    result.warnings.add("Folder ID " + folder.getId() + " points to non-directory: " + folderPath);
                }
            } catch (Exception e) {
                log.warn("Error checking folder {}: {}", folderPath, e.getMessage());
                result.warnings.add("Error checking folder " + folderPath + ": " + e.getMessage());
            }
        }

        return scanned;
    }
    
    /**
     * Determines if a folder is a primary/essential folder that should be recreated
     * if it's missing from the filesystem.
     * 
     * Primary folders include:
     * - YEAR_ROOT: Academic year folders (e.g., "2024-2025")
     * - SEMESTER_ROOT: Semester folders (e.g., "first", "second")
     * - PROFESSOR_ROOT: Professor's root folder
     * - COURSE: Course folders assigned to professors
     * - SUBFOLDER: Standard subfolders within courses (Syllabus, Exams, etc.)
     * 
     * Custom folders are NOT recreated as they are user-created and may be
     * intentionally deleted by an admin.
     * 
     * @param folder The folder to check
     * @return true if the folder is primary and should be recreated
     */
    private boolean isPrimaryFolder(Folder folder) {
        if (folder == null || folder.getType() == null) {
            return false;
        }
        
        FolderType type = folder.getType();
        
        // Primary folders that should always exist
        return type == FolderType.YEAR_ROOT ||
               type == FolderType.SEMESTER_ROOT ||
               type == FolderType.PROFESSOR_ROOT ||
               type == FolderType.COURSE ||
               type == FolderType.SUBFOLDER;
    }

    /**
     * Scan filesystem for files that don't have DB records.
     * This is a heavier operation and should be run less frequently.
     * 
     * @param basePath The path to scan (relative to uploads root)
     * @return List of orphaned file paths (files on disk without DB records)
     */
    public List<String> findFilesWithoutDatabaseRecords(String basePath) {
        List<String> orphanedFiles = new ArrayList<>();
        
        try {
            Path dirPath = basePath.isEmpty() 
                    ? pathResolver.getUploadsRoot()
                    : pathResolver.resolveExistingDirectory(basePath);
            
            Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String relativePath = pathResolver.toRelativePath(file);
                    
                    // Check if file exists in database
                    Optional<UploadedFile> dbFile = uploadedFileRepository.findByFileUrl(relativePath);
                    if (!dbFile.isPresent()) {
                        // Try alternative lookup
                        dbFile = uploadedFileRepository.findByStoredFilename(file.getFileName().toString());
                    }
                    
                    if (!dbFile.isPresent()) {
                        orphanedFiles.add(relativePath);
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("Failed to visit file: {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
            
        } catch (Exception e) {
            log.error("Error scanning for orphaned files", e);
        }
        
        return orphanedFiles;
    }

    /**
     * Get statistics from the last reconciliation run.
     */
    public ReconciliationStats getLastRunStats() {
        return new ReconciliationStats(
                lastRunTime,
                lastRunFilesScanned,
                lastRunOrphanedFilesRemoved,
                lastRunOrphanedFoldersRemoved,
                lastRunFoldersRecreated
        );
    }

    /**
     * Result of a reconciliation run.
     */
    public static class ReconciliationResult {
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public int filesScanned;
        public int foldersScanned;
        public int orphanedFilesRemoved;
        public int orphanedFoldersRemoved;
        public int foldersRecreated;
        public List<String> orphanedFiles = new ArrayList<>();
        public List<String> orphanedFolders = new ArrayList<>();
        public List<String> recreatedFolders = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
    }

    /**
     * Statistics from last reconciliation run.
     */
    public record ReconciliationStats(
            LocalDateTime lastRunTime,
            int filesScanned,
            int orphanedFilesRemoved,
            int orphanedFoldersRemoved,
            int foldersRecreated
    ) {}
}
