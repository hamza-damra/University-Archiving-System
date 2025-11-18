package com.alqude.edu.ArchiveSystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for academic settings.
 * Binds to app.academic.* properties in application.properties
 */
@Configuration
@ConfigurationProperties(prefix = "app.academic")
public class AcademicProperties {
    
    /**
     * Default academic year (e.g., "2024-2025")
     */
    private String defaultYear = "2024-2025";
    
    /**
     * Whether to automatically create three semesters when creating an academic year
     */
    private Boolean autoCreateSemesters = true;
    
    /**
     * Start month for first semester (1-12)
     */
    private Integer semesterFirstStartMonth = 9;
    
    /**
     * End month for first semester (1-12)
     */
    private Integer semesterFirstEndMonth = 12;
    
    /**
     * Start month for second semester (1-12)
     */
    private Integer semesterSecondStartMonth = 1;
    
    /**
     * End month for second semester (1-12)
     */
    private Integer semesterSecondEndMonth = 5;
    
    /**
     * Start month for summer semester (1-12)
     */
    private Integer semesterSummerStartMonth = 6;
    
    /**
     * End month for summer semester (1-12)
     */
    private Integer semesterSummerEndMonth = 8;
    
    // Getters and Setters
    
    public String getDefaultYear() {
        return defaultYear;
    }
    
    public void setDefaultYear(String defaultYear) {
        this.defaultYear = defaultYear;
    }
    
    public Boolean getAutoCreateSemesters() {
        return autoCreateSemesters;
    }
    
    public void setAutoCreateSemesters(Boolean autoCreateSemesters) {
        this.autoCreateSemesters = autoCreateSemesters;
    }
    
    public Integer getSemesterFirstStartMonth() {
        return semesterFirstStartMonth;
    }
    
    public void setSemesterFirstStartMonth(Integer semesterFirstStartMonth) {
        this.semesterFirstStartMonth = semesterFirstStartMonth;
    }
    
    public Integer getSemesterFirstEndMonth() {
        return semesterFirstEndMonth;
    }
    
    public void setSemesterFirstEndMonth(Integer semesterFirstEndMonth) {
        this.semesterFirstEndMonth = semesterFirstEndMonth;
    }
    
    public Integer getSemesterSecondStartMonth() {
        return semesterSecondStartMonth;
    }
    
    public void setSemesterSecondStartMonth(Integer semesterSecondStartMonth) {
        this.semesterSecondStartMonth = semesterSecondStartMonth;
    }
    
    public Integer getSemesterSecondEndMonth() {
        return semesterSecondEndMonth;
    }
    
    public void setSemesterSecondEndMonth(Integer semesterSecondEndMonth) {
        this.semesterSecondEndMonth = semesterSecondEndMonth;
    }
    
    public Integer getSemesterSummerStartMonth() {
        return semesterSummerStartMonth;
    }
    
    public void setSemesterSummerStartMonth(Integer semesterSummerStartMonth) {
        this.semesterSummerStartMonth = semesterSummerStartMonth;
    }
    
    public Integer getSemesterSummerEndMonth() {
        return semesterSummerEndMonth;
    }
    
    public void setSemesterSummerEndMonth(Integer semesterSummerEndMonth) {
        this.semesterSummerEndMonth = semesterSummerEndMonth;
    }
}
