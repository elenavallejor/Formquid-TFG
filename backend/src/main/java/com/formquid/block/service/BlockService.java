package com.formquid.block.service;

import com.formquid.block.dto.BlockResponseDto;
import com.formquid.block.dto.BlockSummaryDto;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockScope;
import com.formquid.block.repository.BlockRepository;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.common.exception.ApiException;
import com.formquid.common.service.IdSequenceService;
import com.formquid.content.service.ContentService;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final ContentService contentService;
    private final IdSequenceService idSequenceService;

    @Transactional(readOnly = true)
    public PageResponseDto<BlockSummaryDto> findCatalogBlocks(int page, int size) {
        Pageable pageable = catalogPageable(page, size);
        Page<BlockSummaryDto> blocks = blockRepository.findCatalogSummaries(BlockScope.CATALOG, pageable);
        return new PageResponseDto<>(
            blocks.getContent(),
            blocks.getNumber(),
            blocks.getSize(),
            blocks.getTotalElements(),
            blocks.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public BlockResponseDto findCatalogBlock(String blockId) {
        Block block = getBlock(blockId);
        if (block.getScope() != BlockScope.CATALOG) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bloque de catálogo no encontrado");
        }
        return toResponse(block);
    }

    @Transactional
    public void deleteCatalogBlock(String blockId) {
        Block block = getBlock(blockId);
        if (block.getScope() != BlockScope.CATALOG) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bloque de catálogo no encontrado");
        }
        if (blockRepository.countBySourceBlockId(blockId) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar un bloque que está siendo utilizado");
        }
        contentService.deleteBlockContent(block);
        blockRepository.delete(block);
    }

    public Block getBlock(String blockId) {
        return blockRepository.findById(blockId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bloque no encontrado"));
    }

    /**
     * Recibe un bloque de catálogo y devuelve el bloque creado.
     */
    @Transactional
    public BlockResponseDto createCatalogBlock(CreateBlockRequestDto request, User creator) {
        if (request.questions() == null || request.questions().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Un bloque debe contener al menos una pregunta");
        }
        Block block = blockRepository.save(Block.builder()
            .id(idSequenceService.nextBlockId())
            .scope(BlockScope.CATALOG)
            .name(request.name().trim())
            .description(trimToNull(request.description()))
            .createdBy(creator)
            .build());
        request.questions().forEach(question -> contentService.addLocalQuestionToBlock(block, question, creator));
        return toResponse(block);
    }

    public BlockResponseDto toResponse(Block block) {
        return new BlockResponseDto(
            block.getId(),
            block.getScope(),
            block.getSourceBlock() == null ? null : block.getSourceBlock().getId(),
            block.getName(),
            block.getDescription(),
            contentService.findBlockContent(block)
        );
    }

    private Pageable catalogPageable(int page, int size) {
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La página no puede ser negativa");
        }
        if (size < 1 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tamaño de página debe estar entre 1 y 100");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}