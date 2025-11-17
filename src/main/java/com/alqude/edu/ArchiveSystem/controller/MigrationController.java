package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.service.DataMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for data migration operations
 * Only accessible by DEANSHIP role
 */
@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('DEANSHIP')")
public class MigrationController {

    private final DataMigrationService migrationService;

    /**
     * Analyzes existing data to determine migration scope
     */
    @GetMapping("/analyze")
    public ResponseEntity<DataMigrationService.MigrationAnalysis> analyzeData() {
        log.info("Analyzing data for migration");
        DataMigrationService.MigrationAnalysis analysis = migrationService.analyzeExistingData();
        return ResponseEntity.ok(analysis);
    }

    /**
     * Executes the full migration process
     */
    @PostMapping("/execute")
    public ResponseEntity<DataMigrationService.MigrationResult> executeMigration() {
        log.info("Starting full migration");
        DataMigrationService.MigrationResult result = migrationService.executeFullMigration();
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Creates academic years from existing data
     */
    @PostMapping("/academic-years")
    public ResponseEntity<String> createAcademicYears() {
        log.info("Creating academic years");
        var academicYears = migrationService.createAcademicYearsFromData();
        return ResponseEntity.ok("Created " + academicYears.size() + " academic years");
    }

    /**
     * Migrates professors
     */
    @PostMapping("/professors")
    public ResponseEntity<String> migrateProfessors() {
        log.info("Migrating professors");
        int count = migrationService.migrateProfessors();
        return ResponseEntity.ok("Migrated " + count + " professors");
    }

    /**
     * Extracts and creates courses
     */
    @PostMapping("/courses")
    public ResponseEntity<String> extractCourses() {
        log.info("Extracting courses");
        var courses = migrationService.extractAndCreateCourses();
        return ResponseEntity.ok("Created " + courses.size() + " courses");
    }
}
