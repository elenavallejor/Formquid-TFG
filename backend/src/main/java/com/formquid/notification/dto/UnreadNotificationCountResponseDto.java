package com.formquid.notification.dto;

/**
 * DTO utilizado para devolver el número de notificaciones
 * pendientes de lectura del usuario autenticado.
 */
public record UnreadNotificationCountResponseDto(
    long count
) {}
