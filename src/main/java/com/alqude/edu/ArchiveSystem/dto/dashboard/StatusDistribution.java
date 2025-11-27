package com.alqude.edu.ArchiveSystem.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for status distribution chart data.
 * Contains counts for each submission status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusDistribution {
    
    /**
     * Count of submissions with NOT_UPLOADED status
     */
    private Long pending;
    
    /**
     * Count of submissions with UPLOADED status
     */
    private Long uploaded;
    
    /**
     * Count of submissions with OVERDUE status
     */
    private Long overdue;
    
    /**
     * Total count of all submissions
     */
    private Long total;
}
