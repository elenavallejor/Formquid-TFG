package com.formquid.survey.dto;

import java.util.List;

/**
 * Representa una página de encuestas resumidas junto con sus datos de paginación.
 */
public record SurveyPageResponseDto(
    List<SurveySummaryDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
