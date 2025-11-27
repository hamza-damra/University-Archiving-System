package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.dashboard.ChartDataPoint;
import com.alqude.edu.ArchiveSystem.dto.dashboard.DashboardStatistics;
import com.alqude.edu.ArchiveSystem.dto.dashboard.DepartmentChartData;
import com.alqude.edu.ArchiveSystem.dto.dashboard.RecentActivity;
import com.alqude.edu.ArchiveSystem.dto.dashboard.StatusDistribution;
import com.alqude.edu.ArchiveSystem.dto.dashboard.TimeGrouping;
import com.alqude.edu.ArchiveSystem.entity.Department;
import com.alqude.edu.ArchiveSystem.entity.DocumentSubmission;
import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.SubmissionStatus;
import com.alqude.edu.ArchiveSystem.repository.CourseRepository;
import com.alqude.edu.ArchiveSystem.repository.DepartmentRepository;
import com.alqude.edu.ArchiveSystem.repository.DocumentSubmissionRepository;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DashboardWidgetService.
 * Provides statistics and chart data for Admin and Dean dashboards.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class DashboardWidgetServiceImpl implements DashboardWidgetService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;

    @Override
    public DashboardStatistics getStatistics(Long academicYearId, Long semesterId) {
        log.debug("Getting dashboard statistics for academicYearId={}, semesterId={}", academicYearId, semesterId);
        
        // Count professors (active)
        long totalProfessors = userRepository.countActiveProfessors();
        log.info("Total active professors: {}", totalProfessors);
        
        // Count HODs
        long totalHods = userRepository.countByRole(Role.ROLE_HOD);
        log.info("Total HODs: {}", totalHods);
        
        // Count departments
        long totalDepartments = departmentRepository.count();
        log.info("Total departments: {}", totalDepartments);
        
        // Count active courses
        long totalCourses = courseRepository.countByIsActiveTrue();
        log.info("Total active courses: {}", totalCourses);
        
        // Get status distribution for submissions
        StatusDistribution statusDist = getStatusDistribution(semesterId);
        
        // Count recent submissions (within last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<DocumentSubmission> recentSubmissionsList = documentSubmissionRepository.findRecentSubmissions(thirtyDaysAgo);
        long recentSubmissions = recentSubmissionsList.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.UPLOADED)
                .count();
        
        DashboardStatistics stats = DashboardStatistics.builder()
                .totalProfessors(totalProfessors)
                .totalHods(totalHods)
                .totalDepartments(totalDepartments)
                .totalCourses(totalCourses)
                .totalSubmissions(statusDist.getTotal())
                .recentSubmissions(recentSubmissions)
                .pendingSubmissions(statusDist.getPending())
                .generatedAt(LocalDateTime.now())
                .build();
        
        log.info("Dashboard statistics: professors={}, courses={}, pending={}", 
                totalProfessors, totalCourses, statusDist.getPending());
        
        return stats;
    }

    @Override
    public List<ChartDataPoint> getSubmissionsOverTime(LocalDate startDate, LocalDate endDate, TimeGrouping groupBy) {
        log.debug("Getting submissions over time from {} to {} grouped by {}", startDate, endDate, groupBy);
        
        if (startDate == null || endDate == null) {
            endDate = LocalDate.now();
            startDate = endDate.minusDays(30);
        }
        
        if (groupBy == null) {
            groupBy = TimeGrouping.DAY;
        }
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        
        // Fetch submissions within the date range
        List<DocumentSubmission> submissions = documentSubmissionRepository.findBySubmittedAtBetween(startDateTime, endDateTime);
        
        // Group submissions by time period
        Map<String, Long> groupedCounts = new LinkedHashMap<>();
        
        // Initialize all periods with 0
        initializePeriods(groupedCounts, startDate, endDate, groupBy);
        
        // Count submissions per period
        for (DocumentSubmission submission : submissions) {
            if (submission.getSubmittedAt() != null) {
                String periodKey = getPeriodKey(submission.getSubmittedAt().toLocalDate(), groupBy);
                groupedCounts.merge(periodKey, 1L, Long::sum);
            }
        }
        
        // Convert to ChartDataPoint list
        return groupedCounts.entrySet().stream()
                .map(entry -> ChartDataPoint.builder()
                        .label(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<DepartmentChartData> getDepartmentDistribution(Long semesterId) {
        log.info("Getting department distribution for semesterId={}", semesterId);
        
        List<Department> departments = departmentRepository.findAll();
        log.info("Found {} departments", departments.size());
        
        // Get submission counts by department using optimized query
        List<Object[]> submissionCounts;
        if (semesterId != null) {
            submissionCounts = documentSubmissionRepository.countByDepartmentAndSemesterId(semesterId);
        } else {
            submissionCounts = documentSubmissionRepository.countByDepartment();
        }
        log.info("Submission counts result: {} rows", submissionCounts.size());
        
        // Convert to map for easy lookup
        Map<Long, Long> deptSubmissionMap = new HashMap<>();
        for (Object[] row : submissionCounts) {
            Long deptId = (Long) row[0];
            Long count = (Long) row[2];
            deptSubmissionMap.put(deptId, count);
            log.debug("Dept {} has {} submissions", deptId, count);
        }
        
        // Get professor counts by department
        List<Object[]> professorCounts = userRepository.countActiveProfessorsByDepartment();
        log.info("Professor counts result: {} rows", professorCounts.size());
        Map<Long, Long> deptProfessorMap = new HashMap<>();
        for (Object[] row : professorCounts) {
            Long deptId = (Long) row[0];
            Long count = (Long) row[2];
            deptProfessorMap.put(deptId, count);
            log.debug("Dept {} has {} professors", deptId, count);
        }
        
        // Get course counts by department
        List<Object[]> courseCounts = courseRepository.countActiveCoursesByDepartment();
        log.info("Course counts result: {} rows", courseCounts.size());
        Map<Long, Long> deptCourseMap = new HashMap<>();
        for (Object[] row : courseCounts) {
            Long deptId = (Long) row[0];
            Long count = (Long) row[2];
            deptCourseMap.put(deptId, count);
            log.debug("Dept {} has {} courses", deptId, count);
        }
        
        List<DepartmentChartData> result = new ArrayList<>();
        
        for (Department department : departments) {
            DepartmentChartData data = DepartmentChartData.builder()
                    .departmentId(department.getId())
                    .departmentName(department.getName())
                    .departmentShortcut(department.getShortcut())
                    .submissionCount(deptSubmissionMap.getOrDefault(department.getId(), 0L))
                    .professorCount(deptProfessorMap.getOrDefault(department.getId(), 0L))
                    .courseCount(deptCourseMap.getOrDefault(department.getId(), 0L))
                    .build();
            result.add(data);
            log.info("Department {}: submissions={}, professors={}, courses={}", 
                    department.getShortcut(), data.getSubmissionCount(), data.getProfessorCount(), data.getCourseCount());
        }
        
        return result;
    }
    
    @Override
    public StatusDistribution getStatusDistribution(Long semesterId) {
        log.debug("Getting status distribution for semesterId={}", semesterId);
        
        List<Object[]> statusCounts;
        if (semesterId != null) {
            statusCounts = documentSubmissionRepository.countByStatusAndSemesterId(semesterId);
        } else {
            statusCounts = documentSubmissionRepository.countByStatus();
        }
        
        long pending = 0;
        long uploaded = 0;
        long overdue = 0;
        
        for (Object[] row : statusCounts) {
            SubmissionStatus status = (SubmissionStatus) row[0];
            Long count = (Long) row[1];
            
            switch (status) {
                case NOT_UPLOADED:
                    pending = count;
                    break;
                case UPLOADED:
                    uploaded = count;
                    break;
                case OVERDUE:
                    overdue = count;
                    break;
            }
        }
        
        return StatusDistribution.builder()
                .pending(pending)
                .uploaded(uploaded)
                .overdue(overdue)
                .total(pending + uploaded + overdue)
                .build();
    }
    
    @Override
    public List<RecentActivity> getRecentActivity(int limit) {
        log.debug("Getting recent activity with limit={}", limit);
        
        List<RecentActivity> activities = new ArrayList<>();
        
        // Get recent submissions (last 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<DocumentSubmission> recentSubmissions = documentSubmissionRepository.findRecentSubmissions(sevenDaysAgo);
        
        for (DocumentSubmission submission : recentSubmissions) {
            if (activities.size() >= limit) break;
            
            String professorName = submission.getProfessor() != null ? 
                    submission.getProfessor().getName() : "Unknown";
            String courseName = submission.getCourseAssignment() != null && 
                    submission.getCourseAssignment().getCourse() != null ?
                    submission.getCourseAssignment().getCourse().getCourseCode() : "Unknown";
            
            String icon = "upload";
            String message = String.format("Prof. %s uploaded %s for %s", 
                    professorName, 
                    submission.getDocumentType() != null ? submission.getDocumentType().name() : "document",
                    courseName);
            
            activities.add(RecentActivity.builder()
                    .type("SUBMISSION")
                    .icon(icon)
                    .message(message)
                    .details(courseName)
                    .timestamp(submission.getSubmittedAt())
                    .timeAgo(getTimeAgo(submission.getSubmittedAt()))
                    .entityId(submission.getId())
                    .entityType("submission")
                    .build());
        }
        
        // Sort by timestamp descending
        activities.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        
        return activities.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * Calculate human-readable time ago string
     */
    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";
        
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();
        
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minutes ago";
        if (hours < 24) return hours + " hours ago";
        if (days < 7) return days + " days ago";
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
    
    /**
     * Initialize all periods with 0 count to ensure continuous data.
     */
    private void initializePeriods(Map<String, Long> groupedCounts, LocalDate startDate, LocalDate endDate, TimeGrouping groupBy) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String key = getPeriodKey(current, groupBy);
            groupedCounts.putIfAbsent(key, 0L);
            
            switch (groupBy) {
                case DAY:
                    current = current.plusDays(1);
                    break;
                case WEEK:
                    current = current.plusWeeks(1);
                    break;
                case MONTH:
                    current = current.plusMonths(1);
                    break;
            }
        }
    }
    
    /**
     * Get the period key for a date based on grouping.
     */
    private String getPeriodKey(LocalDate date, TimeGrouping groupBy) {
        switch (groupBy) {
            case DAY:
                return date.format(DateTimeFormatter.ofPattern("MMM dd"));
            case WEEK:
                int weekOfYear = date.get(WeekFields.ISO.weekOfWeekBasedYear());
                return String.format("Week %d", weekOfYear);
            case MONTH:
                return date.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            default:
                return date.format(DateTimeFormatter.ofPattern("MMM dd"));
        }
    }
}
