package com.formquid.participation.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de salida para una participación anónima.
 */
public record ParticipationResponseDto(
    Long id,
    Long surveyId,
    LocalDateTime submittedAt,
    List<AnswerResponseDto> answers
) {}