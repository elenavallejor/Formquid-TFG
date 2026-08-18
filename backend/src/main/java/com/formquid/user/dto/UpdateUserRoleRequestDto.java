package com.formquid.user.dto;

import com.formquid.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequestDto(
    @NotNull UserRole role
) {
}
