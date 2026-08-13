package com.formquid.collaboration.dto;

import java.time.LocalDateTime;

/**
 * DTO de salida para solicitudes pendientes de colaboración.
 */
public record CollaborationRequestResponseDto(
    Long id,
    Long surveyId,
    String surveyTitle,
    Long requestedByUserId,
    String requestedByName,
    Long invitedEditorUserId,
    String invitedEditorName,
    String invitedEditorEmail,
    String message,
    LocalDateTime createdAt
) {}