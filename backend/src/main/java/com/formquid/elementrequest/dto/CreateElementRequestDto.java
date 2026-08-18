package com.formquid.elementrequest.dto;

import com.formquid.elementrequest.entity.RequestedElementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateElementRequestDto(
    @NotNull RequestedElementType elementType,
    @NotBlank String message,
    @NotNull Map<String, Object> requestPayload
) {
}
