package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyLifecycle;

import java.time.Instant;
import java.util.List;

/**
 * Contiene únicamente los datos necesarios para descubrir encuestas y solicitar colaboración.
 */
public record SurveyDirectorySummaryDto(
    Long id,
    String title,
    String imageKey,
    String editorName,
    SurveyLifecycle lifecycleStatus,
    List<String> tags,
    Instant updatedAt,
    boolean ownedByCurrentUser,
    boolean collaborating,
    boolean collaborationRequestPending
) {
}
