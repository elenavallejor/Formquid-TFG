package com.formquid.survey.dto;

import com.formquid.survey.entity.SurveyStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para cambiar el estado de una encuesta.
 */
public record ChangeSurveyStatusRequestDto(

    @NotNull(message = "El estado de la encuesta es obligatorio")
    SurveyStatus status,

    /**
     * Contraseña de acceso.
     *
     * Solo será obligatoria cuando el nuevo estado sea PRIVATE.
     */
    @Size(max = 255, message = "La contraseña de acceso no puede superar los 255 caracteres")
    String accessPassword
) {}