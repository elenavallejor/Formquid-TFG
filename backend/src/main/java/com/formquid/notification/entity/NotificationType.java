package com.formquid.notification.entity;

/**
 * Tipos de notificación soportados por la plataforma.
 *
 * Se utilizan para clasificar los avisos internos y futuros avisos por correo.
 */
public enum NotificationType {

    GENERAL,
    REGISTRATION_APPROVED,
    ACCOUNT_UPDATED,
    COLLABORATION_REQUEST,
    COLLABORATION_ACCEPTED,
    COLLABORATION_REJECTED,
    ELEMENT_REQUEST_RESOLVED, // El detalle de si fue aprobada o rechazada se indica en el título y mensaje de la notificación.
    SURVEY_STATUS_CHANGED
}