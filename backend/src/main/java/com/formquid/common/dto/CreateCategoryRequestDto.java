package com.formquid.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequestDto(
    @NotBlank @Size(max = 100) String name,
    @Pattern(regexp = "Bs[A-Za-z0-9]{2,62}") String icon
) {
    public CreateCategoryRequestDto(String name) {
        this(name, null);
    }
}
