package com.alqude.edu.ArchiveSystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for file upload settings.
 * Binds to app.upload.* properties in application.properties
 */
@Configuration
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    
    /**
     * Base path for file uploads (e.g., "uploads/")
     */
    private String basePath = "uploads/";
    
    /**
     * Comma-separated list of allowed file extensions (e.g., "pdf,zip")
     */
    private String allowedExtensions = "pdf,zip";
    
    /**
     * Maximum number of files per upload
     */
    private Integer maxFileCount = 10;
    
    /**
     * Maximum total size in MB for all files in a single upload
     */
    private Integer maxTotalSizeMb = 50;
    
    /**
     * Whether to automatically create directories if they don't exist
     */
    private Boolean createDirectories = true;
    
    // Getters and Setters
    
    public String getBasePath() {
        return basePath;
    }
    
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    
    public String getAllowedExtensions() {
        return allowedExtensions;
    }
    
    public void setAllowedExtensions(String allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }
    
    public List<String> getAllowedExtensionsList() {
        return List.of(allowedExtensions.split(","));
    }
    
    public Integer getMaxFileCount() {
        return maxFileCount;
    }
    
    public void setMaxFileCount(Integer maxFileCount) {
        this.maxFileCount = maxFileCount;
    }
    
    public Integer getMaxTotalSizeMb() {
        return maxTotalSizeMb;
    }
    
    public void setMaxTotalSizeMb(Integer maxTotalSizeMb) {
        this.maxTotalSizeMb = maxTotalSizeMb;
    }
    
    public Boolean getCreateDirectories() {
        return createDirectories;
    }
    
    public void setCreateDirectories(Boolean createDirectories) {
        this.createDirectories = createDirectories;
    }
}
