package com.formquid.content.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Recibe la estructura ordenada que sustituye al contenido actual de una encuesta.
 */
public record UpdateSurveyContentRequestDto(
    @NotNull List<@NotNull @Valid SurveyContentItemRequestDto> items
) {
}
