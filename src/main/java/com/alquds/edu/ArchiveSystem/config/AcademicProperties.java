package com.alquds.edu.ArchiveSystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for academic settings.
 * Binds properties with prefix "app.academic" from application.properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.academic")
public class AcademicProperties {
    
    /**
     * Default academic year (e.g., "2024-2025").
     */
    private String defaultYear = "2024-2025";
    
    /**
     * Whether to automatically create semesters for new academic years.
     */
    private Boolean autoCreateSemesters = true;
    
    /**
     * Start month for first semester (1-12).
     */
    private Integer semesterFirstStartMonth = 9;
    
    /**
     * End month for first semester (1-12).
     */
    private Integer semesterFirstEndMonth = 12;
    
    /**
     * Start month for second semester (1-12).
     */
    private Integer semesterSecondStartMonth = 1;
    
    /**
     * End month for second semester (1-12).
     */
    private Integer semesterSecondEndMonth = 5;
    
    /**
     * Start month for summer semester (1-12).
     */
    private Integer semesterSummerStartMonth = 6;
    
    /**
     * End month for summer semester (1-12).
     */
    private Integer semesterSummerEndMonth = 8;
}
