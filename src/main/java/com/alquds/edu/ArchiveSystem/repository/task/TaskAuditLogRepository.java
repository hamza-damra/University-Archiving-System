package com.alquds.edu.ArchiveSystem.repository.task;

import com.alquds.edu.ArchiveSystem.entity.task.TaskAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAuditLogRepository extends JpaRepository<TaskAuditLog, Long> {
    
    /**
     * Find all audit log entries for a task, ordered by creation date.
     */
    @Query("SELECT tal FROM TaskAuditLog tal " +
           "JOIN FETCH tal.changedBy " +
           "WHERE tal.task.id = :taskId " +
           "ORDER BY tal.createdAt DESC")
    List<TaskAuditLog> findByTaskId(@Param("taskId") Long taskId);
}
