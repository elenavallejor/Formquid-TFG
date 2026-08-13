package com.formquid.template.dto;

import com.formquid.fixedquestion.dto.MatrixRowRequestDto;
import com.formquid.fixedquestion.dto.OptionConfigRequestDto;
import com.formquid.fixedquestion.entity.EvaluationDirection;
import com.formquid.fixedquestion.entity.QuestionType;
import com.formquid.fixedquestion.entity.ScaleDisplayType;
import com.formquid.template.entity.TemplateQuestionSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de entrada para una pregunta incluida en una plantilla.
 */
public record TemplateQuestionRequestDto(

    @NotNull(message = "El origen de la pregunta es obligatorio")
    TemplateQuestionSource source,

    Long fixedQuestionId,

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