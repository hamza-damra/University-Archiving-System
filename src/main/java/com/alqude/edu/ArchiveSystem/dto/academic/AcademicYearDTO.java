package com.alqude.edu.ArchiveSystem.dto.academic;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearDTO {
    
    @NotNull(message = "Year code is required")
    private String yearCode; // e.g., "2024-2025"
    
    @NotNull(message = "Start year is required")
    private Integer startYear; // e.g., 2024
    
    @NotNull(message = "End year is required")
    private Integer endYear; // e.g., 2025
    
    private Boolean isActive;
}
