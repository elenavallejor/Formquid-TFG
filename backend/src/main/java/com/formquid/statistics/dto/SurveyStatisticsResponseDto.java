package com.formquid.statistics.dto;

import java.time.Instant;
import java.util.List;

public record SurveyStatisticsResponseDto(
    String surveyId,
    long participations,
    Instant firstParticipation,
    Instant lastParticipation,
    Double globalTechnologyScore,
    List<QuestionStatisticsResponseDto> questions
) {
}
