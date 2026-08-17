package com.formquid.participation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de entrada para enviar una participación completa.
 *
 * La contraseña de acceso solo se utiliza cuando la encuesta es privada.
 * En encuestas públicas puede omitirse.
 */
public record SubmitParticipationRequestDto(

    @Size(max = 255, message = "La contraseña de acceso no puede superar los 255 caracteres")
    String accessPassword,

    @Valid
    @NotEmpty(message = "La participación debe contener al menos una respuesta")
    List<SubmitAnswerRequestDto> answers
) {}
