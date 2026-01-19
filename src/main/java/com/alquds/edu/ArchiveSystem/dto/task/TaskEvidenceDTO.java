package com.alquds.edu.ArchiveSystem.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing an evidence file attached to a task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEvidenceDTO {
    
    private Long id;
    
    // File information
    private Long fileId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    
    // Display metadata
    private Integer displayOrder;
    private String note;
    
    // Snapshot data (original values at attachment time)
    private String originalFilenameSnapshot;
    private String fileUrlSnapshot;
    
    // Timestamps
    private LocalDateTime attachedAt;
    private LocalDateTime fileUploadedAt;
    
    // Uploader info
    private Long uploaderId;
    private String uploaderName;
    
    // Status flags
    private Boolean fileExists;  // true if file reference is still valid
    private Boolean fileMoved;   // true if file URL differs from snapshot
}
