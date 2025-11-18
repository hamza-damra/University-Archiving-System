package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.service.DataMigrationService;
import com.alqude.edu.ArchiveSystem.service.DataMigrationService.MigrationAnalysis;
import com.alqude.edu.ArchiveSystem.service.DataMigrationService.MigrationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for administrative operations including data migration
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final DataMigrationService dataMigrationService;

    /**
     * Analyzes existing data to determine migration scope
     * GET /api/admin/migration/analyze
     */
    @GetMapping("/migration/analyze")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<MigrationAnalysis> analyzeMigrationData() {
        log.info("Analyzing migration data...");
        MigrationAnalysis analysis = dataMigrationService.analyzeExistingData();
        return ResponseEntity.ok(analysis);
    }

    /**
     * Executes the full data migration from old schema to new schema
     * POST /api/admin/migration/execute
     */
    @PostMapping("/migration/execute")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<MigrationResult> executeMigration() {
        log.info("Starting data migration...");
        MigrationResult result = dataMigrationService.executeFullMigration();
        
        if (result.isSuccess()) {
            log.info("Migration completed successfully");
            return ResponseEntity.ok(result);
        } else {
            log.error("Migration failed: {}", result.getErrorMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Health check endpoint for admin operations
     * GET /api/admin/health
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('DEANSHIP')")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Admin API is operational");
    }
}
