package com.alqude.edu.ArchiveSystem.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for chart data points used in dashboard visualizations.
 * Represents a single data point with label, value, and optional category.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataPoint {
    
    /**
     * Label for the data point (e.g., date/period label)
     */
    private String label;
    
    /**
     * Numeric value for the data point
     */
    private Long value;
    
    /**
     * Optional category for grouping data points
     */
    private String category;
}
