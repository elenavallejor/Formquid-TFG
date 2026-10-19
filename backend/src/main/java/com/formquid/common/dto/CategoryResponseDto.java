package com.formquid.common.dto;

import com.formquid.common.CategoryDefinition;

public record CategoryResponseDto(String code, String name, boolean personalData, String icon) {
    public static CategoryResponseDto from(CategoryDefinition category) {
        return new CategoryResponseDto(category.getCode(), category.getName(), category.isPersonalData(), category.getIcon());
    }
}
