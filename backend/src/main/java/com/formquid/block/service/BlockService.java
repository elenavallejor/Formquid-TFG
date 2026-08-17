package com.formquid.block.service;

import com.formquid.block.dto.BlockResponseDto;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.dto.UpdateBlockRequestDto;
import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockStatus;
import com.formquid.block.repository.BlockRepository;
import com.formquid.question.entity.QuestionStatus;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Servicio de negocio para bloques reutilizables.
 *
 * Los bloques agrupan preguntas del catálogo y pueden reutilizarse al crear
 * plantillas o encuestas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

    private final BlockRepository blockRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    /**
     * Devuelve todos los bloques del sistema.
     */
    public List<BlockResponseDto> findAll() {
        return blockRepository.findAllByOrderByNameAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Devuelve solo los bloques activos.
     */
    public List<BlockResponseDto> findActive() {
        return blockRepository.findByStatusOrderByNameAsc(
                BlockStatus.ACTIVE
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Busca un bloque por su identificador.
     */
    public BlockResponseDto findById(Long blockId) {
        return toResponse(findBlock(blockId));
    }

    /**
     * Crea un nuevo bloque reutilizable.
     */
    @Transactional
    public BlockResponseDto create(
        CreateBlockRequestDto request,
        String creatorEmail
    ) {
        String name = cleanName(request.name());

        checkAvailableName(name, null);

        User creator = findCreatorByEmail(creatorEmail);

        Block block = Block.builder()
            .name(name)
            .description(cleanDescription(request.description()))
            .status(BlockStatus.ACTIVE)
            .createdBy(creator)
            .build();

        return toResponse(
            blockRepository.save(block)
        );
    }

    /**
     * Actualiza los datos básicos de un bloque.
     */
    @Transactional
    public BlockResponseDto update(
        Long blockId,
        UpdateBlockRequestDto request
    ) {
        Block block = findBlock(blockId);
        String name = cleanName(request.name());

        checkAvailableName(name, blockId);

        block.setName(name);
        block.setDescription(
            cleanDescription(request.description())
        );

        return toResponse(block);
    }

    /**
     * Activa un bloque.
     */
    @Transactional
    public BlockResponseDto activate(Long blockId) {
        return changeStatus(
            blockId,
            BlockStatus.ACTIVE
        );
    }

    /**
     * Desactiva un bloque.
     *
     * El bloque deja de estar disponible para nuevos usos,
     * pero se mantiene almacenado.
     */
    @Transactional
    public BlockResponseDto disable(Long blockId) {
        return changeStatus(
            blockId,
            BlockStatus.DISABLED
        );
    }

    /**
     * Archiva un bloque.
     */
    @Transactional
    public BlockResponseDto archive(Long blockId) {
        return changeStatus(
            blockId,
            BlockStatus.ARCHIVED
        );
    }

    /**
     * Devuelve un bloque que puede utilizarse al construir
     * una plantilla o encuesta.
     *
     * Debe estar activo y contener al menos una pregunta activa.
     */
    public Block getUsableBlock(Long blockId) {
        Block block = findBlock(blockId);

        if (block.getStatus() != BlockStatus.ACTIVE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El bloque indicado no está activo"
            );
        }

        boolean hasActiveQuestions =
            questionRepository.existsByBlockIdAndStatus(
                blockId,
                QuestionStatus.ACTIVE
            );

        if (!hasActiveQuestions) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El bloque indicado debe contener al menos una pregunta activa"
            );
        }

        return block;
    }

    /**
     * Cambia el estado de un bloque.
     */
    private BlockResponseDto changeStatus(
        Long blockId,
        BlockStatus status
    ) {
        Block block = findBlock(blockId);

        block.setStatus(status);

        return toResponse(block);
    }

    /**
     * Busca un bloque o lanza un error si no existe.
     */
    private Block findBlock(Long blockId) {
        return blockRepository.findById(blockId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Bloque no encontrado"
                )
            );
    }

    /**
     * Obtiene el usuario creador a partir del correo
     * del usuario autenticado.
     */
    private User findCreatorByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return userRepository.findByEmailIgnoreCase(email.trim())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario autenticado no encontrado"
                )
            );
    }

    /**
     * Comprueba que no exista otro bloque con el mismo nombre.
     */
    private void checkAvailableName(
        String name,
        Long currentBlockId
    ) {
        blockRepository.findByNameIgnoreCase(name)
            .filter(existingBlock ->
                currentBlockId == null
                    || !existingBlock.getId().equals(currentBlockId)
            )
            .ifPresent(existingBlock -> {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un bloque con ese nombre"
                );
            });
    }

    /**
     * Limpia el nombre antes de guardarlo.
     */
    private String cleanName(String name) {
        if (name == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El nombre del bloque es obligatorio"
            );
        }

        String cleanName = name.trim();

        if (cleanName.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El nombre del bloque es obligatorio"
            );
        }

        return cleanName;
    }

    /**
     * Limpia descripción.
     */
    private String cleanDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    /**
     * Convierte un bloque a DTO de respuesta.
     */
    private BlockResponseDto toResponse(Block block) {
        User creator = block.getCreatedBy();

        return new BlockResponseDto(
            block.getId(),
            block.getName(),
            block.getDescription(),
            block.getStatus(),
            creator != null ? creator.getId() : null,
            block.getCreatedAt(),
            block.getUpdatedAt()
        );
    }
}
