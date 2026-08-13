package com.formquid.survey.dto;

import com.formquid.fixedquestion.entity.QuestionType;
import com.formquid.fixedquestion.entity.ScaleDisplayType;

import java.util.List;

/**
 * Representación pública de una pregunta.
 *
 * El id se conserva porque es necesario para asociar posteriormente
 * la respuesta enviada por el participante.
 */
public record PublicSurveyQuestionDto(
    Long id,
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
