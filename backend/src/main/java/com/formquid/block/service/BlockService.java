package com.formquid.block.service;

import com.formquid.block.dto.BlockResponseDto;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.dto.UpdateBlockRequestDto;
import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockStatus;
import com.formquid.block.repository.BlockRepository;
import com.formquid.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Servicio encargado de gestionar bloques.
 */
@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final EntityManager entityManager;

    /**
     * Crea un nuevo bloque.
     *
     * @param request datos de creación.
     * @param creatorId identificador del usuario creador.
     * @return bloque creado.
     */
    @Transactional
    public BlockResponseDto create(CreateBlockRequestDto request, Long creatorId) {
        String normalizedName = normalizeName(request.name());

        if (blockRepository.existsByNameNormalized(normalizedName)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ya existe un bloque con ese nombre"
            );
        }

        User creatorReference = entityManager.getReference(User.class, creatorId);

        Block block = Block.builder()
            .name(request.name().trim())
            .nameNormalized(normalizedName)
            .description(normalizeDescription(request.description()))
            .status(BlockStatus.ACTIVE)
            .createdBy(creatorReference)
            .build();

        return mapToResponseDto(blockRepository.save(block));
    }

    /**
     * Devuelve todos los bloques existentes.
     *
     * @return lista de bloques.
     */
    @Transactional(readOnly = true)
    public List<BlockResponseDto> findAll() {
        return blockRepository.findAllByOrderByNameAsc()
            .stream()
            .map(this::mapToResponseDto)
            .toList();
    }

    /**
     * Devuelve los bloques activos.
     *
     * @return lista de bloques activos.
     */
    @Transactional(readOnly = true)
    public List<BlockResponseDto> findActive() {
        return blockRepository.findByStatusOrderByNameAsc(BlockStatus.ACTIVE)
            .stream()
            .map(this::mapToResponseDto)
            .toList();
    }

    /**
     * Devuelve un bloque por su identificador.
     *
     * @param id identificador del bloque.
     * @return bloque encontrado.
     */
    @Transactional(readOnly = true)
    public BlockResponseDto findById(Long id) {
        return mapToResponseDto(findEntityById(id));
    }

    /**
     * Actualiza los datos básicos de un bloque.
     *
     * @param id identificador del bloque.
     * @param request nuevos datos.
     * @return bloque actualizado.
     */
    @Transactional
    public BlockResponseDto update(Long id, UpdateBlockRequestDto request) {
        Block block = findEntityById(id);
        String normalizedName = normalizeName(request.name());

        blockRepository.findByNameNormalized(normalizedName)
            .filter(existingBlock -> !existingBlock.getId().equals(id))
            .ifPresent(existingBlock -> {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ya existe otro bloque con ese nombre"
                );
            });

        block.setName(request.name().trim());
        block.setNameNormalized(normalizedName);
        block.setDescription(normalizeDescription(request.description()));

        return mapToResponseDto(blockRepository.save(block));
    }

    /**
     * Activa un bloque.
     *
     * @param id identificador del bloque.
     * @return bloque activado.
     */
    @Transactional
    public BlockResponseDto activate(Long id) {
        return changeStatus(id, BlockStatus.ACTIVE);
    }

    /**
     * Desactiva un bloque.
     *
     * @param id identificador del bloque.
     * @return bloque desactivado.
     */
    @Transactional
    public BlockResponseDto disable(Long id) {
        return changeStatus(id, BlockStatus.DISABLED);
    }

    /**
     * Archiva un bloque.
     *
     * @param id identificador del bloque.
     * @return bloque archivado.
     */
    @Transactional
    public BlockResponseDto archive(Long id) {
        return changeStatus(id, BlockStatus.ARCHIVED);
    }

    private BlockResponseDto changeStatus(Long id, BlockStatus status) {
        Block block = findEntityById(id);
        block.setStatus(status);

        return mapToResponseDto(blockRepository.save(block));
    }

    private Block findEntityById(Long id) {
        return blockRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Bloque no encontrado"
            ));
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        return description != null && !description.isBlank()
            ? description.trim()
            : null;
    }

    private BlockResponseDto mapToResponseDto(Block block) {
        Long createdByUserId = block.getCreatedBy() != null
            ? block.getCreatedBy().getId()
            : null;

        return new BlockResponseDto(
            block.getId(),
            block.getName(),
            block.getDescription(),
            block.getStatus(),
            createdByUserId,
            block.getCreatedAt(),
            block.getUpdatedAt()
        );
    }
}