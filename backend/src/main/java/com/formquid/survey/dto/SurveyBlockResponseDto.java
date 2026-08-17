package com.formquid.survey.dto;

import java.util.List;

/**
 * DTO de salida para un bloque o grupo de preguntas de una encuesta.
 */
public record SurveyBlockResponseDto(
    String blockKey,
    Long sourceBlockId,
    String title,
    Integer position,
    List<SurveyQuestionResponseDto> questions
) {}
