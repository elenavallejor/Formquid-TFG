package com.formquid.collaboration.dto;

import java.time.LocalDateTime;

/**
 * DTO de salida para colaboradores de una encuesta.
 */
public record SurveyCollaboratorResponseDto(
    Long id,
    Long surveyId,
    String surveyTitle,
    Long editorUserId,
    String editorName,
    String editorEmail,
    Long addedByUserId,
    String addedByName,
    LocalDateTime addedAt
) {}