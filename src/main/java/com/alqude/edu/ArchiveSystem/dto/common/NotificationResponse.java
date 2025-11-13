package com.alqude.edu.ArchiveSystem.dto.common;

import com.alqude.edu.ArchiveSystem.entity.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private boolean seen;
    private Notification.NotificationType type;
    private Long relatedEntityId;
    private String relatedEntityType;
    private LocalDateTime createdAt;

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .seen(Boolean.TRUE.equals(notification.getIsRead()))
                .type(notification.getType())
                .relatedEntityId(notification.getRelatedEntityId())
                .relatedEntityType(notification.getRelatedEntityType())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
