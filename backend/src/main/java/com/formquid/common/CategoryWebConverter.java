package com.formquid.common;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CategoryWebConverter implements Converter<String, Category> {
    @Override
    public Category convert(String source) {
        return Category.of(source);
    }
}
