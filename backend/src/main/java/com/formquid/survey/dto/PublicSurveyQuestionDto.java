package com.formquid.survey.dto;

import com.formquid.question.entity.QuestionType;
import com.formquid.question.entity.ScaleDisplayType;

import java.util.List;

/**
 * Representación pública de una pregunta.
 *
 * questionKey se conserva porque es necesario para asociar posteriormente
 * la respuesta enviada por el participante.
 */
public record PublicSurveyQuestionDto(
    String questionKey,
    String text,
    String description,
    QuestionType type,
    boolean required,
    Integer minValue,
    Integer maxValue,
    ScaleDisplayType scaleDisplayType,
    String leftLabel,
    String rightLabel,
    List<PublicOptionDto> options,
    List<PublicMatrixRowDto> matrixRows,
    Integer position
) {}
