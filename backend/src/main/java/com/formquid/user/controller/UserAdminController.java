package com.formquid.user.controller;

import com.formquid.user.dto.UpdateUserRoleRequestDto;
import com.formquid.user.dto.UserResponseDto;
import com.formquid.user.entity.UserRole;
import com.formquid.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Controlador REST para la administración de usuarios.
 *
 * Expone las operaciones necesarias para gestionar cuentas y roles
 * desde el panel de administración.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(
    name = "Administración de usuarios",
    description = "Gestión de cuentas, roles y estado de usuarios"
)
public class UserAdminController {

    private final UserService userService;

    /**
     * Lista todos los usuarios registrados.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar usuarios",
        description = "Devuelve todos los usuarios registrados en la plataforma."
    )
    public List<UserResponseDto> findAll() {
        return userService.findAll();
    }

    /**
     * Lista usuarios filtrados por rol.
     */
    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar usuarios por rol",
        description = "Devuelve los usuarios que tienen el rol indicado."
    )
    public List<UserResponseDto> findByRole(
        @PathVariable UserRole role
    ) {
        return userService.findByRole(role);
    }

    /**
     * Activa una cuenta de usuario.
     */
    @PatchMapping("/{userId}/enable")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Activar usuario",
        description = "Activa una cuenta de usuario deshabilitada."
    )
    public UserResponseDto enableUser(
        @PathVariable Long userId,
        Principal principal
    ) {
        return userService.enableUser(
            userId,
            principal.getName()
        );
    }

    /**
     * Desactiva una cuenta de usuario.
     */
    @PatchMapping("/{userId}/disable")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Desactivar usuario",
        description = "Desactiva una cuenta de usuario sin eliminar sus datos."
    )
    public UserResponseDto disableUser(
        @PathVariable Long userId,
        Principal principal
    ) {
        return userService.disableUser(
            userId,
            principal.getName()
        );
    }

    /**
     * Cambia el rol de un usuario.
     *
     * Solo un SUPERADMIN puede realizar esta operación.
     */
    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(
        summary = "Cambiar rol de usuario",
        description = "Permite a un superadministrador cambiar el rol de una cuenta."
    )
    public UserResponseDto updateRole(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateUserRoleRequestDto request,
        Principal principal
    ) {
        return userService.updateRole(
            userId,
            request,
            principal.getName()
        );
    }
}