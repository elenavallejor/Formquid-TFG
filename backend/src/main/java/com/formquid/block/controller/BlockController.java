package com.formquid.block.controller;

import com.formquid.auth.security.CustomUserDetails;
import com.formquid.block.dto.BlockResponseDto;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.dto.UpdateBlockRequestDto;
import com.formquid.block.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestionar bloques reutilizables.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(
    name = "Bloques",
    description = "Gestión de bloques reutilizables"
)
public class BlockController {

    private final BlockService blockService;

    /**
     * Devuelve los bloques activos disponibles.
     *
     * Pueden consultarlos usuarios autenticados para construir encuestas,
     * plantillas o preguntas.
     *
     * @return lista de bloques activos.
     */
    @GetMapping("/blocks/active")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar bloques activos",
        description = "Devuelve los bloques reutilizables activos."
    )
    public List<BlockResponseDto> findActive() {
        return blockService.findActive();
    }

    /**
     * Devuelve todos los bloques.
     *
     * @return lista de bloques.
     */
    @GetMapping("/admin/blocks")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar bloques",
        description = "Devuelve todos los bloques registrados."
    )
    public List<BlockResponseDto> findAll() {
        return blockService.findAll();
    }

    /**
     * Devuelve un bloque concreto por su identificador.
     *
     * @param id identificador del bloque.
     * @return bloque encontrado.
     */
    @GetMapping("/admin/blocks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Obtener bloque",
        description = "Devuelve la información de un bloque concreto."
    )
    public BlockResponseDto findById(@PathVariable Long id) {
        return blockService.findById(id);
    }

    /**
     * Crea un nuevo bloque.
     *
     * @param request datos de creación.
     * @param userDetails usuario autenticado.
     * @return bloque creado.
     */
    @PostMapping("/admin/blocks")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Crear bloque",
        description = "Crea un nuevo bloque reutilizable."
    )
    public BlockResponseDto create(
        @Valid @RequestBody CreateBlockRequestDto request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return blockService.create(request, userDetails.getId());
    }

    /**
     * Actualiza un bloque existente.
     *
     * @param id identificador del bloque.
     * @param request nuevos datos.
     * @return bloque actualizado.
     */
    @PutMapping("/admin/blocks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Actualizar bloque",
        description = "Modifica los datos básicos de un bloque existente."
    )
    public BlockResponseDto update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateBlockRequestDto request
    ) {
        return blockService.update(id, request);
    }

    /**
     * Activa un bloque.
     *
     * @param id identificador del bloque.
     * @return bloque activado.
     */
    @PatchMapping("/admin/blocks/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Activar bloque",
        description = "Marca un bloque como activo."
    )
    public BlockResponseDto activate(@PathVariable Long id) {
        return blockService.activate(id);
    }

    /**
     * Desactiva un bloque.
     *
     * @param id identificador del bloque.
     * @return bloque desactivado.
     */
    @PatchMapping("/admin/blocks/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Desactivar bloque",
        description = "Marca un bloque como desactivado."
    )
    public BlockResponseDto disable(@PathVariable Long id) {
        return blockService.disable(id);
    }

    /**
     * Archiva un bloque.
     *
     * @param id identificador del bloque.
     * @return bloque archivado.
     */
    @PatchMapping("/admin/blocks/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Archivar bloque",
        description = "Marca un bloque como archivado."
    )
    public BlockResponseDto archive(@PathVariable Long id) {
        return blockService.archive(id);
    }
}