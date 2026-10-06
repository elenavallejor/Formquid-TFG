package com.formquid.participation.dto;

import java.time.Instant;
import java.util.List;

/**
 * Detalle de solo lectura de una participación, deliberadamente anonimizado.
 */
public record ParticipationDetailResponseDto(
    Long id,
    Long surveyId,
    String surveyTitle,
    Instant submittedAt,
    List<ParticipationAnswerResponseDto> answers
) {
}
