package com.formquid.notification.dto;

import com.formquid.notification.entity.NotificationChannel;
import com.formquid.notification.entity.NotificationTargetType;
import com.formquid.notification.entity.NotificationType;

import java.time.LocalDateTime;

/**
 * DTO de salida de una notificación.
 *
 * El campo read se deriva de la existencia de readAt y no se almacena
 * como una segunda bandera en la entidad, evitando estados inconsistentes.
 */
public record NotificationResponseDto(
    Long id,
    NotificationType type,
    NotificationChannel channel,
    String title,
    String message,
    NotificationTargetType targetType,
    Long targetId,
    boolean read,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {}
