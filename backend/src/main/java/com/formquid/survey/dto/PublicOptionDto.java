package com.formquid.survey.dto;

/**
 * Opción visible para el participante.
 *
 * scoreValue se omite porque pertenece a la evaluación interna.
 */
public record PublicOptionDto(
    String label,
    String storedValue,
    boolean nonResponseOption,
    boolean allowsFreeText
) {}
