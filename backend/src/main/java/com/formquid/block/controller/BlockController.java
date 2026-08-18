package com.formquid.block.controller;

import com.formquid.block.dto.BlockResponseDto;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.service.BlockService;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador de bloques de catálogo.
 *
 * Expone las operaciones para consultar bloques y permite crear 
 * nuevos bloques.
 */
@RestController
@RequestMapping("/api/v1/catalog/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    /**
     * Devuelve los bloques disponibles en el catálogo ordenados por nombre.
     */
    @GetMapping
    public List<BlockResponseDto> findCatalogBlocks() {
        return blockService.findCatalogBlocks();
    }

    /**
     * Recibe los datos de un bloque. Devuelve el bloque de catálogo creado.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public BlockResponseDto create(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateBlockRequestDto request
    ) {
        return blockService.createCatalogBlock(request, user);
    }
}
