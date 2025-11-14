package com.alqude.edu.ArchiveSystem.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequestCreateRequest {
    
    @NotBlank(message = "Course name is required")
    private String courseName;
    
    @NotBlank(message = "Document type is required")
    private String documentType;
    
    @NotEmpty(message = "At least one file extension is required")
    private List<String> requiredFileExtensions;
    
    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    private LocalDateTime deadline;
    
    @NotNull(message = "Professor ID is required")
    private Long professorId;
    
    private String description;
    
    private Integer maxFileCount = 5; // Default maximum 5 files
    
    private Integer maxTotalSizeMb = 50; // Default max 50MB total
}
