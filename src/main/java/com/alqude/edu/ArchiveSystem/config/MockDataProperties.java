package com.alqude.edu.ArchiveSystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for mock data generation.
 * Binds properties with prefix "mock.data" from application.properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "mock.data")
public class MockDataProperties {
    
    /**
     * Enable/disable mock data generation on application startup.
     */
    private boolean enabled = true;
    
    /**
     * Skip data generation if existing data is detected.
     */
    private boolean skipIfExists = true;
    
    /**
     * Number of academic years to generate.
     */
    private int academicYears = 3;
    
    /**
     * Number of departments to create.
     */
    private int departments = 5;
    
    /**
     * Number of courses per department.
     */
    private int coursesPerDepartment = 3;
    
    /**
     * Number of professors per department.
     */
    private int professorsPerDepartment = 5;
    
    /**
     * Number of course assignments per semester.
     */
    private int assignmentsPerSemester = 20;
    
    /**
     * Percentage of submissions to create (0-100).
     */
    private int submissionsPercentage = 70;
}
