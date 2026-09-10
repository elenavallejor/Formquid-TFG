package com.formquid.content.dto;

import com.formquid.content.entity.ContentItemType;
import com.formquid.question.dto.QuestionResponseDto;


public record ContentItemResponseDto(
    Long id,
    ContentItemType itemType,
    Integer position,
    Boolean required,
    String textBody,
    QuestionResponseDto question,
    Long blockId
) {
}
