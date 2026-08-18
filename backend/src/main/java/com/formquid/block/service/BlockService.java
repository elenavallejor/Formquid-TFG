package com.formquid.block.service;

import com.formquid.block.dto.BlockResponseDto;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockScope;
import com.formquid.block.repository.BlockRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.common.service.IdSequenceService;
import com.formquid.content.service.ContentService;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final ContentService contentService;
    private final IdSequenceService idSequenceService;

    public List<BlockResponseDto> findCatalogBlocks() {
        return blockRepository.findByScopeOrderByNameAsc(BlockScope.CATALOG)
            .stream()
            .map(this::toResponse)
            .toList();
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

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
