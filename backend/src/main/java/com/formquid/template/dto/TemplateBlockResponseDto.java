package com.formquid.template.dto;

import java.util.List;

/**
 * DTO de salida para un bloque incluido en una plantilla.
 */
public record TemplateBlockResponseDto(
    String blockKey,
    Long blockId,
    String blockName,
    List<TemplateQuestionResponseDto> questions
) {}
