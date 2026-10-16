package com.formquid.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CategoryConverter implements AttributeConverter<Category, String> {
    @Override
    public String convertToDatabaseColumn(Category category) {
        return category == null ? null : category.code();
    }

    @Override
    public Category convertToEntityAttribute(String code) {
        return code == null ? null : Category.of(code);
    }
}
