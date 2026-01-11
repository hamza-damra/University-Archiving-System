package com.alquds.edu.ArchiveSystem.config;

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
     * Supports common university document and archive/image types.
     */
    private List<String> allowedExtensions = List.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "txt", "zip", "rar", "jpg", "jpeg", "png", "gif", "csv");
    
    /**
     * Maximum number of files per upload.
     */
    private Integer maxFileCount = 10;
    
    /**
     * Maximum total size in MB for all files in an upload.
     * Increased to support larger documents (e.g., up to 100MB per file).
     */
    private Integer maxTotalSizeMb = 1000;
    
    /**
     * Whether to automatically create upload directories if they don't exist.
     */
    private Boolean createDirectories = true;
}
