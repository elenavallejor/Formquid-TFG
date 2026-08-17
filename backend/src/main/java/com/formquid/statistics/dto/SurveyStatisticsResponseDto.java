package com.formquid.statistics.dto;

import java.util.List;

/**
 * Estadísticas generales de una encuesta.
 */
public record SurveyStatisticsResponseDto(
    Long surveyId,
    String surveyTitle,
    long totalParticipations,
    List<QuestionStatisticsResponseDto> questions
) {}
