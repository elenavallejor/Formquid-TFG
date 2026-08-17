package com.formquid.user.controller;

import com.formquid.user.dto.UserSummaryResponseDto;
import com.formquid.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Controlador REST para consultas generales sobre usuarios.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(
    name = "Usuarios",
    description = "Consultas generales sobre usuarios"
)
public class UserController {

    private final UserService userService;

    /**
     * Busca editores activos por nombre, correo o institución.
     *
     * Si no se proporciona query, devuelve todos los editores activos
     * excepto el usuario autenticado.
     */
    @GetMapping("/editors")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Buscar editores",
        description = "Busca editores activos por nombre, correo o institución."
    )
    public List<UserSummaryResponseDto> searchEditors(
        @RequestParam(required = false) String query,
        Principal principal
    ) {
        return userService.searchEditors(
            query,
            principal.getName()
        );
    }
}
