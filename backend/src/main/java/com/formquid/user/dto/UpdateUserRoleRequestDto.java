package com.formquid.user.dto;

import com.formquid.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para cambiar el rol de un usuario.
 *
 * Esta operación solo debe estar disponible para SUPERADMIN.
 */
public record UpdateUserRoleRequestDto(

    @NotNull(message = "El nuevo rol es obligatorio")
    UserRole role
) {}
