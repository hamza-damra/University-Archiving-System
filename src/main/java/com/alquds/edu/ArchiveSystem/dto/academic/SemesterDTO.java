package com.alquds.edu.ArchiveSystem.dto.academic;

import com.alquds.edu.ArchiveSystem.entity.academic.SemesterType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SemesterDTO {
    
    @NotNull(message = "Semester type is required")
    private SemesterType type;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    private Boolean isActive;
}
