package com.formquid.fixedquestion.dto;

import com.formquid.fixedquestion.entity.EvaluationDirection;
import com.formquid.fixedquestion.entity.QuestionType;
import com.formquid.fixedquestion.entity.ScaleDisplayType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Datos necesarios para crear una pregunta fija.
 */
public record CreateFixedQuestionRequestDto(

    @NotBlank(message = "El enunciado de la pregunta es obligatorio")
    @Size(max = 1000, message = "El enunciado no puede superar los 1000 caracteres")
    String text,

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    String description,

    @NotNull(message = "El tipo de pregunta es obligatorio")
    QuestionType type,

    EvaluationDirection evaluationDirection,

    Long blockId,

    Boolean requiredByDefault,

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

) {
}