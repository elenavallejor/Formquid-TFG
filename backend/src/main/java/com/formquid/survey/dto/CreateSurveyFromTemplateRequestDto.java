package com.formquid.survey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de entrada para crear una encuesta a partir de una plantilla.
 */
public record CreateSurveyFromTemplateRequestDto(

    @NotNull(message = "La plantilla es obligatoria")
    Long templateId,

    @NotBlank(message = "El título de la encuesta es obligatorio")
    @Size(max = 180, message = "El título no puede superar los 180 caracteres")
    String title,

    @Size(max = 1500, message = "La descripción no puede superar los 1500 caracteres")
    String description,

    @Size(max = 1000, message = "La URL de la imagen no puede superar los 1000 caracteres")
    String imageUrl,

    List<@Size(max = 60, message = "La etiqueta no puede superar los 60 caracteres") String> tagNames
) {}