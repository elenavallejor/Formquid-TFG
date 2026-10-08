package com.formquid.block.service;

import com.formquid.block.dto.BlockResponseDto;
import com.formquid.block.dto.BlockSummaryDto;
import com.formquid.block.dto.CreateBlockItemRequestDto;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockScope;
import com.formquid.block.repository.BlockRepository;
import com.formquid.common.dto.CatalogSearchField;
import com.formquid.common.dto.CatalogSortOrder;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.common.exception.ApiException;
import com.formquid.content.dto.AddFixedQuestionItemRequestDto;
import com.formquid.content.dto.ContentItemResponseDto;
import com.formquid.content.dto.CreateQuestionItemRequestDto;
import com.formquid.content.dto.CreateTextItemRequestDto;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.service.ContentService;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final ContentService contentService;

    @Transactional(readOnly = true)
    public PageResponseDto<BlockSummaryDto> findCatalogBlocks(
        int page,
        int size,
        User user,
        boolean mine,
        String search,
        Set<CatalogSearchField> searchFields,
        CatalogSortOrder sortOrder
    ) {
        Set<CatalogSearchField> fields = searchFields == null ? Set.of() : searchFields;
        Pageable pageable = catalogPageable(page, size, sortOrder);
        Page<BlockSummaryDto> blocks = blockRepository.findCatalogSummaries(
            BlockScope.CATALOG,
            user.getId(),
            mine,
            normalizeSearch(search),
            fields.contains(CatalogSearchField.TITLE),
            fields.contains(CatalogSearchField.DESCRIPTION),
            fields.contains(CatalogSearchField.CREATOR),
            pageable
        );
        return new PageResponseDto<>(
            blocks.getContent(),
            blocks.getNumber(),
            blocks.getSize(),
            blocks.getTotalElements(),
            blocks.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDto<BlockResponseDto> findCatalogBlocksForBuilder(int page, int size) {
        Pageable pageable = builderPageable(page, size);
        Page<Block> blocks = blockRepository.findCatalogBlocksWithDetails(BlockScope.CATALOG, pageable);
        Map<Long, List<ContentItemResponseDto>> contentByBlock = contentService
            .findBlocksContent(blocks.getContent());
        List<BlockResponseDto> responses = blocks.getContent().stream()
            .map(block -> toResponse(block, contentByBlock.getOrDefault(block.getId(), List.of())))
            .toList();
        return new PageResponseDto<>(
            responses,
            blocks.getNumber(),
            blocks.getSize(),
            blocks.getTotalElements(),
            blocks.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public BlockResponseDto findCatalogBlock(Long blockId) {
        Block block = getBlock(blockId);
        if (block.getScope() != BlockScope.CATALOG) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bloque de catálogo no encontrado");
        }
        return toResponse(
            block,
            contentService.findBlockContent(block),
            blockRepository.countBySourceBlockId(blockId)
        );
    }

    @Transactional
    public void deleteCatalogBlock(Long blockId, User user) {
        Block block = getBlock(blockId);
        if (block.getScope() != BlockScope.CATALOG) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bloque de catálogo no encontrado");
        }
        requireManagementPermission(block.getCreatedBy(), user);
        if (blockRepository.countBySourceBlockId(blockId) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar un bloque que está siendo utilizado");
        }
        contentService.deleteBlockContent(block);
        blockRepository.delete(block);
    }

    public Block getBlock(Long blockId) {
        return blockRepository.findById(blockId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bloque no encontrado"));
    }

    /**
     * Recibe un bloque de catálogo y devuelve el bloque creado.
     */
    @Transactional
    public BlockResponseDto createCatalogBlock(CreateBlockRequestDto request, User creator) {
        validateDraft(request);
        Block block = blockRepository.save(Block.builder()
            .scope(BlockScope.CATALOG)
            .name(request.name().trim())
            .description(trimToNull(request.description()))
            .createdBy(creator)
            .build());
        request.items().stream()
            .sorted(Comparator.comparing(CreateBlockItemRequestDto::position))
            .forEach(item -> addItem(block, item, creator));
        return toResponse(block);
    }

    @Transactional
    public BlockResponseDto updateCatalogBlock(Long blockId, CreateBlockRequestDto request, User user) {
        Block block = getBlock(blockId);
        if (block.getScope() != BlockScope.CATALOG) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bloque de catálogo no encontrado");
        }
        requireManagementPermission(block.getCreatedBy(), user);
        validateDraft(request);
        contentService.deleteBlockContent(block);
        block.setName(request.name().trim());
        block.setDescription(trimToNull(request.description()));
        block.setUpdatedAt(Instant.now());
        request.items().stream()
            .sorted(Comparator.comparing(CreateBlockItemRequestDto::position))
            .forEach(item -> addItem(block, item, user));
        return toResponse(blockRepository.save(block));
    }

    private void requireManagementPermission(User creator, User user) {
        if (user.getRole() == UserRole.SUPERADMIN) return;
        if (creator == null || !creator.getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo puedes gestionar tus propios bloques");
        }
    }

    public BlockResponseDto toResponse(Block block) {
        return toResponse(block, contentService.findBlockContent(block), null);
    }

    private BlockResponseDto toResponse(Block block, List<ContentItemResponseDto> contentItems) {
        return toResponse(block, contentItems, null);
    }

    private BlockResponseDto toResponse(
        Block block,
        List<ContentItemResponseDto> contentItems,
        Long usageCount
    ) {
        User creator = block.getCreatedBy();
        return new BlockResponseDto(
            block.getId(),
            block.getScope(),
            block.getSourceBlock() == null ? null : block.getSourceBlock().getId(),
            block.getName(),
            block.getDescription(),
            contentItems,
            creator == null ? null : creator.getId(),
            creator == null ? null : creator.getName(),
            block.getCreatedAt(),
            block.getUpdatedAt(),
            usageCount
        );
    }

    private Pageable catalogPageable(int page, int size, CatalogSortOrder sortOrder) {
        validatePage(page, size);
        Sort.Direction direction = sortOrder == CatalogSortOrder.OLDEST
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, "updatedAt").and(Sort.by(direction, "id")));
    }

    private Pageable builderPageable(int page, int size) {
        validatePage(page, size);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La página no puede ser negativa");
        }
        if (size < 1 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tamaño de página debe estar entre 1 y 100");
        }
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public void validateItems(List<CreateBlockItemRequestDto> items) {
        if (items == null || items.stream().noneMatch(item ->
            item != null && item.itemType() == ContentItemType.QUESTION
        )) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Un bloque debe contener al menos una pregunta");
        }
        Set<Integer> positions = new HashSet<>();
        for (CreateBlockItemRequestDto item : items) {
            if (item == null || item.itemType() == null || item.position() == null || item.position() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La estructura del bloque contiene un elemento inválido");
            }
            if (!positions.add(item.position())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La estructura del bloque contiene posiciones duplicadas");
            }
            if (item.itemType() == ContentItemType.QUESTION
                && (item.required() == null || (item.question() == null) == (item.fixedQuestionId() == null))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Cada pregunta debe tener un único origen");
            }
            if (item.itemType() == ContentItemType.QUESTION && item.textBody() != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Una pregunta no puede incluir contenido de texto independiente");
            }
            if (item.itemType() == ContentItemType.TEXT && (item.textBody() == null || item.textBody().isBlank())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Los elementos de texto no pueden estar vacíos");
            }
            if (item.itemType() == ContentItemType.TEXT
                && (item.required() != null || item.question() != null || item.fixedQuestionId() != null)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Un texto no puede incluir configuración de pregunta");
            }
            if (item.itemType() == ContentItemType.BLOCK) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Un bloque no puede contener otros bloques");
            }
        }
        for (int position = 1; position <= items.size(); position++) {
            if (!positions.contains(position)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las posiciones del bloque deben ser consecutivas");
            }
        }
    }

    public void validateDraft(CreateBlockRequestDto request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El bloque necesita un nombre");
        }
        if (request.name().trim().length() > 150) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El nombre del bloque no puede superar 150 caracteres");
        }
        validateItems(request.items());
    }

    private void addItem(Block block, CreateBlockItemRequestDto item, User creator) {
        if (item.itemType() == ContentItemType.TEXT) {
            contentService.addTextToBlock(block, new CreateTextItemRequestDto(item.position(), item.textBody()));
            return;
        }
        if (item.fixedQuestionId() != null) {
            contentService.addFixedQuestionToBlock(
                block,
                new AddFixedQuestionItemRequestDto(item.position(), item.required(), item.fixedQuestionId()),
                creator
            );
        } else {
            contentService.addLocalQuestionToBlock(
                block,
                new CreateQuestionItemRequestDto(item.position(), item.required(), item.question()),
                creator
            );
        }
    }
}
