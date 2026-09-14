package com.formquid.content.dto;

import com.formquid.content.entity.ContentItemType;

public record BlockContentItemRowDto(
    Long parentBlockId,
    Long id,
    ContentItemType itemType,
    Integer position,
    Boolean required,
    String textBody
) {
}
