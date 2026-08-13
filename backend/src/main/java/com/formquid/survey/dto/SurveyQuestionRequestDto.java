package com.formquid.survey.dto;

import com.formquid.fixedquestion.dto.MatrixRowRequestDto;
import com.formquid.fixedquestion.dto.OptionConfigRequestDto;
import com.formquid.fixedquestion.entity.EvaluationDirection;
import com.formquid.fixedquestion.entity.QuestionType;
import com.formquid.fixedquestion.entity.ScaleDisplayType;
import com.formquid.survey.entity.SurveyQuestionSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de entrada para una pregunta incluida en una encuesta.
 */
public record SurveyQuestionRequestDto(

    @NotNull(message = "El origen de la pregunta es obligatorio")
    SurveyQuestionSource source,

    /**
     * Identificador de la pregunta fija de origen.
     *
     * Se usa cuando source = FIXED.
     */
    Long sourceFixedQuestionId,

    /**
     * Identificador de la pregunta de plantilla de origen.
     *
     * Se usa internamente cuando source = TEMPLATE.
     */
    Long sourceTemplateQuestionId,

    @Size(max = 1000, message = "El enunciado no puede superar los 1000 caracteres")
    String text,

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    String description,

    QuestionType type,

    EvaluationDirection evaluationDirection,

    Boolean required,

    Integer minValue,

    Integer maxValue,

    ScaleDisplayType scaleDisplayType,

    @Size(max = 255, message = "La etiqueta izquierda no puede superar los 255 caracteres")
    String leftLabel,

    @Size(max = 255, message = "La etiqueta derecha no puede superar los 255 caracteres")
    String rightLabel,

    @Valid
    List<OptionConfigRequestDto> options,

    @Valid
    List<MatrixRowRequestDto> matrixRows
) {}