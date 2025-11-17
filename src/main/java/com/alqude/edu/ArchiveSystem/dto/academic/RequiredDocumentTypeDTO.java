package com.alqude.edu.ArchiveSystem.dto.academic;

import com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocumentTypeDTO {
    
    @NotNull(message = "Document type is required")
    private DocumentTypeEnum documentType;
    
    private Long semesterId; // Optional: specific to semester
    
    private LocalDateTime deadline;
    
    private Boolean isRequired;
    
    private Integer maxFileCount;
    
    private Integer maxTotalSizeMb;
    
    private List<String> allowedFileExtensions; // e.g., ["pdf", "zip"]
}
