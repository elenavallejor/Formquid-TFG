package com.formquid.block.controller;

import com.formquid.block.dto.BlockResponseDto;
import com.formquid.block.dto.BlockSummaryDto;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.service.BlockService;
import com.formquid.common.dto.CatalogSearchField;
import com.formquid.common.dto.CatalogSortOrder;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

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
     * Devuelve una página de bloques disponibles en el catálogo.
     */
    @GetMapping
    public PageResponseDto<BlockSummaryDto> findCatalogBlocks(
        @AuthenticationPrincipal User user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "false") boolean mine,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "TITLE,DESCRIPTION,CREATOR") Set<CatalogSearchField> searchFields,
        @RequestParam(defaultValue = "NEWEST") CatalogSortOrder sortOrder
    ) {
        return blockService.findCatalogBlocks(page, size, user, mine, search, searchFields, sortOrder);
    }

    @GetMapping("/builder")
    public PageResponseDto<BlockResponseDto> findCatalogBlocksForBuilder(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return blockService.findCatalogBlocksForBuilder(page, size);
    }

    @GetMapping("/{blockId}")
    public BlockResponseDto findCatalogBlock(@PathVariable Long blockId) {
        return blockService.findCatalogBlock(blockId);
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

    @PutMapping("/{blockId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public BlockResponseDto update(
        @PathVariable Long blockId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateBlockRequestDto request
    ) {
        return blockService.updateCatalogBlock(blockId, request, user);
    }

    @DeleteMapping("/{blockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public void deleteCatalogBlock(
        @PathVariable Long blockId,
        @AuthenticationPrincipal User user
    ) {
        blockService.deleteCatalogBlock(blockId, user);
    }
}
