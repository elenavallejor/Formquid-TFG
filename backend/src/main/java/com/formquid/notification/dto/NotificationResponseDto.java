package com.formquid.notification.dto;

import com.formquid.notification.entity.NotificationChannel;
import com.formquid.notification.entity.NotificationTargetType;
import com.formquid.notification.entity.NotificationType;

import java.time.Instant;

public record NotificationResponseDto(
    Long id,
    NotificationType type,
    NotificationChannel channel,
    String title,
    String message,
    NotificationTargetType targetType,
    Long targetId,
    Instant createdAt,
    Instant readAt
) {
}