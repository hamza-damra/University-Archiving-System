package com.alquds.edu.ArchiveSystem.service.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for automatic task status updates.
 * Runs daily at midnight to check and update overdue tasks.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueTaskScheduler {
    
    private final TaskService taskService;
    
    /**
     * Check and update overdue tasks daily at midnight.
     * Cron expression: "0 0 0 * * ?" = Every day at 00:00:00
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkOverdueTasks() {
        log.info("Running scheduled job to check for overdue tasks");
        try {
            int updated = taskService.checkAndUpdateOverdueTasks();
            log.info("Scheduled job completed: {} tasks marked as OVERDUE", updated);
        } catch (Exception e) {
            log.error("Error in scheduled job for overdue tasks", e);
        }
    }
}
