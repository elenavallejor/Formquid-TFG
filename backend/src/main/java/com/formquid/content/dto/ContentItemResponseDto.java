package com.formquid.content.dto;

import com.formquid.content.entity.ContentItemType;
import com.formquid.question.dto.QuestionResponseDto;

import java.util.UUID;

public record ContentItemResponseDto(
    UUID id,
    ContentItemType itemType,
    Integer position,
    Boolean required,
    String textBody,
    QuestionResponseDto question,
    String blockId
) {
}
