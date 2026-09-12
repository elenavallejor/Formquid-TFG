package com.formquid.block.dto;

import com.formquid.content.entity.ContentItemType;
import com.formquid.question.dto.QuestionDraftDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Representa una pregunta o un texto dentro de la estructura ordenada de un bloque.
 */
public record CreateBlockItemRequestDto(
    @NotNull ContentItemType itemType,
    @NotNull @Min(1) Integer position,
    Boolean required,
    @Valid QuestionDraftDto question,
    String textBody
) {
}