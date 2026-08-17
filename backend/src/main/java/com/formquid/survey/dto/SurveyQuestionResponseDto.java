package com.formquid.survey.dto;

import com.formquid.question.dto.MatrixRowResponseDto;
import com.formquid.question.dto.OptionConfigResponseDto;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.entity.ScaleDisplayType;
import com.formquid.survey.model.SurveyQuestionSource;

import java.util.List;

/**
 * DTO de salida para una pregunta de encuesta.
 */
public record SurveyQuestionResponseDto(
    String questionKey,
    SurveyQuestionSource source,
    Long sourceQuestionId,
    String text,
    String description,
    QuestionType type,
    EvaluationDirection evaluationDirection,
    boolean required,
    Integer minValue,
    Integer maxValue,
    ScaleDisplayType scaleDisplayType,
    String leftLabel,
    String rightLabel,
    List<OptionConfigResponseDto> options,
    List<MatrixRowResponseDto> matrixRows,
    Integer position
) {}
