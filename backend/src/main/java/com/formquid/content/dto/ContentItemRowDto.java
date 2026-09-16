package com.formquid.content.dto;

import com.formquid.content.entity.ContentItemType;

public record ContentItemRowDto(
    Long id,
    ContentItemType itemType,
    Integer position,
    Boolean required,
    String textBody
) {
}
