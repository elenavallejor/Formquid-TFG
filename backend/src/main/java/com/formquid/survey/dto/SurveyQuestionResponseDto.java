package com.formquid.survey.dto;

import com.formquid.fixedquestion.dto.MatrixRowResponseDto;
import com.formquid.fixedquestion.dto.OptionConfigResponseDto;
import com.formquid.fixedquestion.entity.EvaluationDirection;
import com.formquid.fixedquestion.entity.QuestionType;
import com.formquid.fixedquestion.entity.ScaleDisplayType;
import com.formquid.survey.entity.SurveyQuestionSource;

import java.util.List;

/**
 * DTO de salida para una pregunta de encuesta.
 */
public record SurveyQuestionResponseDto(
    Long id,
    SurveyQuestionSource source,
    Long sourceFixedQuestionId,
    Long sourceTemplateQuestionId,
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