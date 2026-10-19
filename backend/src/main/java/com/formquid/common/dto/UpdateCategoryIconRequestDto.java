package com.formquid.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateCategoryIconRequestDto(
    @NotBlank @Pattern(regexp = "Bs[A-Za-z0-9]{2,62}") String icon
) {
}
