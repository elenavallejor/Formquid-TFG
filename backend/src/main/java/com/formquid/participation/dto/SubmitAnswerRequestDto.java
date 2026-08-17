package com.formquid.participation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de entrada para responder a una pregunta de encuesta.
 *
 * Solo debe rellenarse el campo correspondiente al tipo de pregunta.
 */
public record SubmitAnswerRequestDto(

    @NotBlank(message = "La clave de la pregunta es obligatoria")
    @Size(max = 80, message = "La clave de la pregunta no puede superar los 80 caracteres")
    String questionKey,

    @Size(max = 5000, message = "La respuesta textual no puede superar los 5000 caracteres")
    String textValue,

    BigDecimal numericValue,

    Boolean booleanValue,

    List<@Size(max = 255, message = "El valor seleccionado no puede superar los 255 caracteres") String> selectedValues,

    @Valid
    List<MatrixAnswerRequestDto> matrixValues,

    @Size(max = 3000, message = "El texto libre no puede superar los 3000 caracteres")
    String freeTextValue
) {}
