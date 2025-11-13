package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.common.NotificationResponse;
import com.alqude.edu.ArchiveSystem.entity.Notification;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthService authService;

    public List<NotificationResponse> getCurrentUserNotifications() {
        User currentUser = authService.getCurrentUser();
        log.debug("Fetching notifications for user {}", currentUser.getId());

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
            .map(NotificationResponse::fromEntity)
            .toList();
    }

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
}
