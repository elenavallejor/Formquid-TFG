package com.formquid.collaboration.dto;

import com.formquid.collaboration.entity.SurveyCollaborationStatus;

import java.time.LocalDateTime;

/**
 * DTO de salida para invitaciones de colaboración.
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
    SurveyCollaborationStatus status,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt
) {}
