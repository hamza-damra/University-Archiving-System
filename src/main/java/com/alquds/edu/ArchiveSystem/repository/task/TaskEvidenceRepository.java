package com.alquds.edu.ArchiveSystem.repository.task;

import com.alquds.edu.ArchiveSystem.entity.task.TaskEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing TaskEvidence entities.
 */
@Repository
public interface TaskEvidenceRepository extends JpaRepository<TaskEvidence, Long> {
    
    /**
     * Find all evidence files for a task, ordered by display order.
     */
    @Query("SELECT te FROM TaskEvidence te " +
           "LEFT JOIN FETCH te.file f " +
           "WHERE te.task.id = :taskId " +
           "ORDER BY te.displayOrder ASC, te.attachedAt ASC")
    List<TaskEvidence> findByTaskIdWithFile(@Param("taskId") Long taskId);
    
    /**
     * Find all evidence files for a task (basic query).
     */
    List<TaskEvidence> findByTaskIdOrderByDisplayOrderAsc(Long taskId);
    
    /**
     * Count evidence files for a task.
     */
    long countByTaskId(Long taskId);
    
    /**
     * Check if a file is already attached to a task.
     */
    boolean existsByTaskIdAndFileId(Long taskId, Long fileId);
    
    /**
     * Find specific evidence by task and file.
     */
    Optional<TaskEvidence> findByTaskIdAndFileId(Long taskId, Long fileId);
    
    /**
     * Get the maximum display order for a task.
     */
    @Query("SELECT COALESCE(MAX(te.displayOrder), -1) FROM TaskEvidence te WHERE te.task.id = :taskId")
    Integer findMaxDisplayOrderByTaskId(@Param("taskId") Long taskId);
    
    /**
     * Delete all evidence for a task.
     */
    @Modifying
    @Query("DELETE FROM TaskEvidence te WHERE te.task.id = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);
    
    /**
     * Delete specific evidence by ID.
     */
    @Modifying
    @Query("DELETE FROM TaskEvidence te WHERE te.id = :evidenceId AND te.task.id = :taskId")
    int deleteByIdAndTaskId(@Param("evidenceId") Long evidenceId, @Param("taskId") Long taskId);
    
    /**
     * Find all evidence files for multiple tasks (batch fetch for list views).
     */
    @Query("SELECT te FROM TaskEvidence te " +
           "LEFT JOIN FETCH te.file f " +
           "WHERE te.task.id IN :taskIds " +
           "ORDER BY te.task.id, te.displayOrder ASC")
    List<TaskEvidence> findByTaskIdInWithFile(@Param("taskIds") List<Long> taskIds);
    
    /**
     * Find evidence by file ID (to check if file is used as evidence).
     */
    List<TaskEvidence> findByFileId(Long fileId);
    
    /**
     * Count how many tasks a file is attached to as evidence.
     */
    long countByFileId(Long fileId);
}
