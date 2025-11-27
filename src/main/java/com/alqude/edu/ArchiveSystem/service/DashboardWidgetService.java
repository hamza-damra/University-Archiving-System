package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.dashboard.ChartDataPoint;
import com.alqude.edu.ArchiveSystem.dto.dashboard.DashboardStatistics;
import com.alqude.edu.ArchiveSystem.dto.dashboard.DepartmentChartData;
import com.alqude.edu.ArchiveSystem.dto.dashboard.StatusDistribution;
import com.alqude.edu.ArchiveSystem.dto.dashboard.RecentActivity;
import com.alqude.edu.ArchiveSystem.dto.dashboard.TimeGrouping;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service interface for dashboard widget operations.
 * Provides statistics and chart data for Admin and Dean dashboards.
 */
public interface DashboardWidgetService {
    
    /**
     * Gets dashboard statistics for Admin/Dean.
     * Returns counts for professors, HODs, departments, courses, and submissions.
     * 
     * @param academicYearId Optional filter by academic year (null for all)
     * @param semesterId Optional filter by semester (null for all)
     * @return Dashboard statistics DTO with all counts
     */
    DashboardStatistics getStatistics(Long academicYearId, Long semesterId);
    
    /**
     * Gets chart data for submissions over time.
     * Groups submission counts by the specified time period.
     * 
     * @param startDate Start of the period
     * @param endDate End of the period
     * @param groupBy Time grouping (DAY, WEEK, MONTH)
     * @return List of chart data points with date labels and submission counts
     */
    List<ChartDataPoint> getSubmissionsOverTime(LocalDate startDate, LocalDate endDate, TimeGrouping groupBy);
    
    /**
     * Gets chart data for department distribution.
     * Returns submission counts per department for the specified semester.
     * 
     * @param semesterId Optional semester filter (null for all semesters)
     * @return List of department chart data with submission counts
     */
    List<DepartmentChartData> getDepartmentDistribution(Long semesterId);
    
    /**
     * Gets status distribution for submissions.
     * Returns counts for Pending, Uploaded, and Overdue submissions.
     * 
     * @param semesterId Optional semester filter (null for all semesters)
     * @return Status distribution with counts for each status
     */
    StatusDistribution getStatusDistribution(Long semesterId);
    
    /**
     * Gets recent activity feed.
     * Returns recent submissions, professor additions, and course updates.
     * 
     * @param limit Maximum number of activities to return
     * @return List of recent activities
     */
    List<RecentActivity> getRecentActivity(int limit);
}
