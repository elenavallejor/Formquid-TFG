package com.formquid.elementrequest.dto;

import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.elementrequest.entity.ElementRequestType;
import com.formquid.question.dto.CreateQuestionRequestDto;
import com.formquid.template.dto.CreateSurveyTemplateRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos necesarios para proponer un nuevo elemento reutilizable.
 *
 * Solo debe informarse el DTO específico correspondiente al valor de type:
 * - BLOCK -> block
 * - QUESTION -> question
 * - TEMPLATE -> template
 */
public record CreateElementRequestDto(

    @NotNull(message = "El tipo de elemento solicitado es obligatorio")
    ElementRequestType type,

    @NotBlank(message = "El título de la solicitud es obligatorio")
    @Size(max = 180, message = "El título no puede superar los 180 caracteres")
    String title,

    @NotBlank(message = "La descripción de la solicitud es obligatoria")
    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
    String description,

    @Size(max = 1500, message = "La justificación no puede superar los 1500 caracteres")
    String justification,

    @Valid
    CreateBlockRequestDto block,

    @Valid
    CreateQuestionRequestDto question,

    @Valid
    CreateSurveyTemplateRequestDto template

) {}
