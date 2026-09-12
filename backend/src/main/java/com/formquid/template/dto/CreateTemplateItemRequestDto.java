package com.formquid.template.dto;

import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.content.entity.ContentItemType;
import com.formquid.question.dto.QuestionDraftDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Representa un elemento ordenado dentro de una plantilla.
 */
public record CreateTemplateItemRequestDto(
    @NotNull ContentItemType itemType,
    @NotNull @Min(1) Integer position,
    Boolean required,
    @Valid QuestionDraftDto question,
    String textBody,
    Long fixedQuestionId,
    Long blockId,
    @Valid CreateBlockRequestDto block
) {
}
