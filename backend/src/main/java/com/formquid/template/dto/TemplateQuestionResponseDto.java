package com.formquid.template.dto;

import com.formquid.fixedquestion.dto.MatrixRowResponseDto;
import com.formquid.fixedquestion.dto.OptionConfigResponseDto;
import com.formquid.fixedquestion.entity.EvaluationDirection;
import com.formquid.fixedquestion.entity.QuestionType;
import com.formquid.fixedquestion.entity.ScaleDisplayType;
import com.formquid.template.entity.TemplateQuestionSource;

import java.util.List;

/**
 * DTO de salida para una pregunta incluida en una plantilla.
 */
public record TemplateQuestionResponseDto(
    Long id,
    TemplateQuestionSource source,
    Long fixedQuestionId,
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
    List<MatrixRowResponseDto> matrixRows
) {}