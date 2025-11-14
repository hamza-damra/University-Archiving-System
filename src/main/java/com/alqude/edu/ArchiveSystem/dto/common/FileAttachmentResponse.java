package com.alqude.edu.ArchiveSystem.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for individual file attachment responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAttachmentResponse {
    
    private Long id;
    private String originalFilename;
    private String fileName; // Alias for frontend compatibility
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private Integer fileOrder;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
