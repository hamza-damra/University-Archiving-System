package com.alqude.edu.ArchiveSystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for file upload settings.
 * Binds properties with prefix "app.upload" from application.properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    
    /**
     * Base path for file uploads.
     */
    private String basePath = "uploads/";
    
    /**
     * Allowed file extensions for uploads.
     */
    private List<String> allowedExtensions = List.of("pdf", "zip");
    
    /**
     * Maximum number of files per upload.
     */
    private Integer maxFileCount = 10;
    
    /**
     * Maximum total size in MB for all files in an upload.
     */
    private Integer maxTotalSizeMb = 50;
    
    /**
     * Whether to automatically create upload directories if they don't exist.
     */
    private Boolean createDirectories = true;
}
