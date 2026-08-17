package com.formquid.survey.dto;

import com.formquid.question.dto.MatrixRowRequestDto;
import com.formquid.question.dto.OptionConfigRequestDto;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.entity.ScaleDisplayType;
import com.formquid.survey.model.SurveyQuestionSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de entrada para una pregunta incluida en una encuesta.
 */
public record SurveyQuestionRequestDto(

    /**
     * Clave estable de la pregunta dentro de la encuesta.
     *
     * Puede omitirse al crear una pregunta nueva; se conserva al editar para
     * no romper respuestas ya asociadas por questionKey.
     */
    @Size(max = 80, message = "La clave de la pregunta no puede superar los 80 caracteres")
    String questionKey,

    @NotNull(message = "El origen de la pregunta es obligatorio")
    SurveyQuestionSource source,

    /**
     * Identificador de la pregunta de catálogo de origen.
     *
     * Se usa cuando source = CATALOG.
     */
    Long sourceQuestionId,

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
