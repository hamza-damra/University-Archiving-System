package com.alquds.edu.ArchiveSystem.service.user;

import com.alquds.edu.ArchiveSystem.service.auth.AuthService;

import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.user.Notification;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.repository.user.NotificationRepository;

import com.alquds.edu.ArchiveSystem.dto.common.NotificationResponse;
import com.alquds.edu.ArchiveSystem.entity.user.Notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Service for managing notifications in the Archive System.
 * Handles notification creation, retrieval, and status management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    /**
     * Get all notifications for the current authenticated user.
     * 
     * @return List of notification responses ordered by creation date descending
     */
    public List<NotificationResponse> getCurrentUserNotifications() {
        User currentUser = authService.getCurrentUser();
        log.debug("Fetching notifications for user {}", currentUser.getId());

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
            .map(NotificationResponse::fromEntity)
            .toList();
    }

    /**
     * Mark a notification as read.
     * Only the owner of the notification can mark it as read.
     * 
     * @param notificationId The ID of the notification to mark as read
     * @throws ResponseStatusException if notification not found or user doesn't own it
     */
    @Transactional
    public void markNotificationAsRead(Long notificationId) {
        User currentUser = authService.getCurrentUser();
        if (notificationId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Notification id is required");
        }
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Notification not found"));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "You cannot modify this notification");
        }

        if (Boolean.TRUE.equals(notification.getIsRead())) {
            log.debug("Notification {} already marked as read", notificationId);
            return;
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Creates notifications for HOD and Dean users when a professor submits a document.
     * - Creates one notification for the HOD of the professor's department
     * - Creates one notification for each Dean user
     * 
     * The notification includes professor name, course, department, and submission timestamp.
     * 
     * @param submission The document submission that triggered the notification
     * @throws IllegalArgumentException if submission is null or missing required data
     */
    @Transactional
    public void notifySubmission(DocumentSubmission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("Submission cannot be null");
        }
        
        User professor = submission.getProfessor();
        if (professor == null) {
            throw new IllegalArgumentException("Submission must have a professor");
        }
        
        Department department = professor.getDepartment();
        String courseName = submission.getCourseAssignment() != null && 
                           submission.getCourseAssignment().getCourse() != null
                ? submission.getCourseAssignment().getCourse().getCourseName()
                : "Unknown Course";
        String departmentName = department != null ? department.getName() : "Unknown Department";
        String professorName = professor.getName();
        LocalDateTime submittedAt = submission.getSubmittedAt();
        
        String notificationTitle = "New Document Submission";
        String notificationMessage = String.format(
            "Professor %s submitted a document for %s (%s) at %s",
            professorName,
            courseName,
            departmentName,
            submittedAt != null ? submittedAt.toString() : "N/A"
        );
        
        log.info("Creating submission notifications for submission ID: {}, professor: {}, course: {}, department: {}",
                submission.getId(), professorName, courseName, departmentName);
        
        // Create notification for HOD of the professor's department
        if (department != null) {
            List<User> hodUsers = userRepository.findByDepartmentIdAndRole(department.getId(), Role.ROLE_HOD);
            for (User hod : hodUsers) {
                createNotification(hod, notificationTitle, notificationMessage, 
                        NotificationType.DOCUMENT_SUBMITTED, submission.getId(), "DocumentSubmission");
                log.debug("Created notification for HOD: {} (department: {})", hod.getEmail(), departmentName);
            }
        }
        
        // Create notification for all Dean users
        List<User> deanUsers = userRepository.findByRole(Role.ROLE_DEANSHIP);
        for (User dean : deanUsers) {
            createNotification(dean, notificationTitle, notificationMessage,
                    NotificationType.DOCUMENT_SUBMITTED, submission.getId(), "DocumentSubmission");
            log.debug("Created notification for Dean: {}", dean.getEmail());
        }
        
        log.info("Created {} notifications for submission ID: {}", 
                (department != null ? userRepository.findByDepartmentIdAndRole(department.getId(), Role.ROLE_HOD).size() : 0) 
                + deanUsers.size(), 
                submission.getId());
    }

    /**
     * Gets the count of unread notifications for a user.
     * Used for displaying notification badge count.
     * 
     * @param userId The ID of the user
     * @return The count of unread notifications
     */
    public long getUnreadCount(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return notificationRepository.countUnreadNotificationsByUser(userId);
    }

    /**
     * Gets notifications for a user with role-based filtering.
     * - HOD users only see notifications for submissions from their department
     * - Dean users see notifications from all departments
     * 
     * @param user The user to get notifications for
     * @return List of notifications filtered by role
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        log.debug("Getting notifications for user: {} with role: {}", user.getEmail(), user.getRole());
        
        // Get all notifications for the user
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        // For HOD users, filter to only show notifications from their department
        // This is already handled by the fact that HOD notifications are only created
        // for submissions in their department, but we add this as an extra safety check
        if (user.getRole() == Role.ROLE_HOD) {
            Department userDepartment = user.getDepartment();
            if (userDepartment == null) {
                log.warn("HOD user {} has no department assigned", user.getEmail());
                return notifications;
            }
            // HOD notifications are already scoped to their department during creation
            // No additional filtering needed here
        }
        
        // Dean users see all notifications (no filtering needed)
        return notifications;
    }

    /**
     * Helper method to create and save a notification.
     * 
     * @param user The user to receive the notification
     * @param title The notification title
     * @param message The notification message
     * @param type The notification type
     * @param relatedEntityId The ID of the related entity (e.g., submission ID)
     * @param relatedEntityType The type of the related entity
     * @return The created notification
     */
    private Notification createNotification(User user, String title, String message,
                                           NotificationType type, Long relatedEntityId, 
                                           String relatedEntityType) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRelatedEntityType(relatedEntityType);
        
        return notificationRepository.save(notification);
    }
}
