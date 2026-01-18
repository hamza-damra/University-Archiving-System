package com.alquds.edu.ArchiveSystem.dto.task;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for HOD approval or rejection of tasks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskApprovalRequest {
    
    @Size(max = 500, message = "Feedback must not exceed 500 characters")
    private String feedback; // Optional feedback/reason for approval or rejection
}
