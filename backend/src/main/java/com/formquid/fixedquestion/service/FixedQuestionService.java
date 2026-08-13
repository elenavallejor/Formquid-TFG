package com.formquid.fixedquestion.service;

import com.formquid.block.entity.Block;
import com.formquid.block.repository.BlockRepository;
import com.formquid.fixedquestion.dto.*;
import com.formquid.fixedquestion.entity.*;
import com.formquid.fixedquestion.repository.FixedQuestionRepository;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Servicio encargado de gestionar preguntas fijas reutilizables.
 */
@Service
@RequiredArgsConstructor
public class FixedQuestionService {

    private final FixedQuestionRepository fixedQuestionRepository;
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    /**
     * Crea una nueva pregunta fija.
     */
    @Transactional
    public FixedQuestionResponseDto create(
        CreateFixedQuestionRequestDto request,
        String creatorEmail
    ) {
        validateQuestionConfiguration(
            request.type(),
            request.options(),
            request.matrixRows(),
            request.minValue(),
            request.maxValue(),
            request.leftLabel(),
            request.rightLabel()
        );

        User creator = findCreatorByEmail(creatorEmail);
        Block block = resolveBlock(request.blockId());

        FixedQuestion question = FixedQuestion.builder()
            .text(request.text().trim())
            .description(normalizeText(request.description()))
            .type(request.type())
            .evaluationDirection(
                resolveEvaluationDirection(
                    request.evaluationDirection()
                )
            )
            .status(FixedQuestionStatus.ACTIVE)
            .block(block)
            .createdBy(creator)
            .requiredByDefault(
                Boolean.TRUE.equals(request.requiredByDefault())
            )
            .minValue(
                resolveMinValue(
                    request.type(),
                    request.minValue()
                )
            )
            .maxValue(
                resolveMaxValue(
                    request.type(),
                    request.maxValue()
                )
            )
            .scaleDisplayType(
                resolveScaleDisplayType(
                    request.type(),
                    request.scaleDisplayType()
                )
            )
            .leftLabel(
                resolveLeftLabel(
                    request.type(),
                    request.leftLabel()
                )
            )
            .rightLabel(
                resolveRightLabel(
                    request.type(),
                    request.rightLabel()
                )
            )
            .options(
                buildOptions(
                    request.type(),
                    request.options()
                )
            )
            .matrixRows(
                buildMatrixRows(
                    request.type(),
                    request.matrixRows()
                )
            )
            .build();

        return mapToResponseDto(
            fixedQuestionRepository.save(question)
        );
    }

    /**
     * Devuelve todas las preguntas fijas.
     */
    @Transactional(readOnly = true)
    public List<FixedQuestionResponseDto> findAll() {
        return fixedQuestionRepository
            .findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::mapToResponseDto)
            .toList();
    }

    /**
     * Devuelve las preguntas fijas activas.
     */
    @Transactional(readOnly = true)
    public List<FixedQuestionResponseDto> findActive() {
        return fixedQuestionRepository
            .findByStatusOrderByTextAsc(
                FixedQuestionStatus.ACTIVE
            )
            .stream()
            .map(this::mapToResponseDto)
            .toList();
    }

    /**
     * Devuelve las preguntas activas de un bloque.
     */
    @Transactional(readOnly = true)
    public List<FixedQuestionResponseDto> findActiveByBlock(
        Long blockId
    ) {
        if (!blockRepository.existsById(blockId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Bloque no encontrado"
            );
        }

        return fixedQuestionRepository
            .findByBlockIdAndStatusOrderByTextAsc(
                blockId,
                FixedQuestionStatus.ACTIVE
            )
            .stream()
            .map(this::mapToResponseDto)
            .toList();
    }

    /**
     * Devuelve una pregunta fija por id.
     */
    @Transactional(readOnly = true)
    public FixedQuestionResponseDto findById(Long id) {
        return mapToResponseDto(findEntityById(id));
    }

    /**
     * Actualiza una pregunta fija.
     */
    @Transactional
    public FixedQuestionResponseDto update(
        Long id,
        UpdateFixedQuestionRequestDto request
    ) {
        validateQuestionConfiguration(
            request.type(),
            request.options(),
            request.matrixRows(),
            request.minValue(),
            request.maxValue(),
            request.leftLabel(),
            request.rightLabel()
        );

        FixedQuestion question = findEntityById(id);

        question.setText(request.text().trim());
        question.setDescription(
            normalizeText(request.description())
        );
        question.setType(request.type());
        question.setEvaluationDirection(
            resolveEvaluationDirection(
                request.evaluationDirection()
            )
        );
        question.setBlock(
            resolveBlock(request.blockId())
        );
        question.setRequiredByDefault(
            Boolean.TRUE.equals(
                request.requiredByDefault()
            )
        );
        question.setMinValue(
            resolveMinValue(
                request.type(),
                request.minValue()
            )
        );
        question.setMaxValue(
            resolveMaxValue(
                request.type(),
                request.maxValue()
            )
        );
        question.setScaleDisplayType(
            resolveScaleDisplayType(
                request.type(),
                request.scaleDisplayType()
            )
        );
        question.setLeftLabel(
            resolveLeftLabel(
                request.type(),
                request.leftLabel()
            )
        );
        question.setRightLabel(
            resolveRightLabel(
                request.type(),
                request.rightLabel()
            )
        );
        question.setOptions(
            buildOptions(
                request.type(),
                request.options()
            )
        );
        question.setMatrixRows(
            buildMatrixRows(
                request.type(),
                request.matrixRows()
            )
        );

        return mapToResponseDto(
            fixedQuestionRepository.save(question)
        );
    }

    @Transactional
    public FixedQuestionResponseDto activate(Long id) {
        return changeStatus(
            id,
            FixedQuestionStatus.ACTIVE
        );
    }

    @Transactional
    public FixedQuestionResponseDto disable(Long id) {
        return changeStatus(
            id,
            FixedQuestionStatus.DISABLED
        );
    }

    @Transactional
    public FixedQuestionResponseDto archive(Long id) {
        return changeStatus(
            id,
            FixedQuestionStatus.ARCHIVED
        );
    }

    private FixedQuestionResponseDto changeStatus(
        Long id,
        FixedQuestionStatus status
    ) {
        FixedQuestion question = findEntityById(id);
        question.setStatus(status);

        return mapToResponseDto(
            fixedQuestionRepository.save(question)
        );
    }

    private FixedQuestion findEntityById(Long id) {
        return fixedQuestionRepository.findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Pregunta fija no encontrada"
                )
            );
    }

    /**
     * Obtiene el usuario creador a partir del correo almacenado
     * como principal de Spring Security.
     */
    private User findCreatorByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario autenticado no encontrado"
                )
            );
    }

    /**
     * Obtiene el bloque asociado, si se ha indicado uno.
     */
    private Block resolveBlock(Long blockId) {
        if (blockId == null) {
            return null;
        }

        return blockRepository.findById(blockId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Bloque no encontrado"
                )
            );
    }

    /**
     * Valida la configuración específica de cada tipo
     * de pregunta.
     */
    private void validateQuestionConfiguration(
        QuestionType type,
        List<OptionConfigRequestDto> options,
        List<MatrixRowRequestDto> matrixRows,
        Integer minValue,
        Integer maxValue,
        String leftLabel,
        String rightLabel
    ) {
        if (
            requiresOptions(type)
            && (options == null || options.size() < 2)
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas de selección deben tener al menos dos opciones"
            );
        }

        if (
            !requiresOptions(type)
            && options != null
            && !options.isEmpty()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Este tipo de pregunta no admite opciones"
            );
        }

        if (
            type == QuestionType.MATRIX_SCALE
            && (matrixRows == null || matrixRows.isEmpty())
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas de matriz deben tener al menos una fila"
            );
        }

        if (
            type != QuestionType.MATRIX_SCALE
            && matrixRows != null
            && !matrixRows.isEmpty()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Este tipo de pregunta no admite filas de matriz"
            );
        }

        if (
            isScaleType(type)
            && minValue != null
            && maxValue != null
            && minValue >= maxValue
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El valor mínimo de la escala debe ser menor que el valor máximo"
            );
        }

        if (
            type == QuestionType.BIPOLAR_SCALE
            && (isBlank(leftLabel) || isBlank(rightLabel))
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las escalas bipolares deben tener etiqueta izquierda y derecha"
            );
        }
    }

    private boolean requiresOptions(QuestionType type) {
        return type == QuestionType.SINGLE_CHOICE
            || type == QuestionType.MULTIPLE_CHOICE;
    }

    private boolean isScaleType(QuestionType type) {
        return type == QuestionType.SCALE
            || type == QuestionType.MATRIX_SCALE
            || type == QuestionType.BIPOLAR_SCALE;
    }

    private Integer resolveMinValue(
        QuestionType type,
        Integer minValue
    ) {
        return isScaleType(type)
            ? minValue != null ? minValue : 1
            : null;
    }

    private Integer resolveMaxValue(
        QuestionType type,
        Integer maxValue
    ) {
        return isScaleType(type)
            ? maxValue != null ? maxValue : 5
            : null;
    }

    private ScaleDisplayType resolveScaleDisplayType(
        QuestionType type,
        ScaleDisplayType scaleDisplayType
    ) {
        return isScaleType(type)
            ? scaleDisplayType != null
                ? scaleDisplayType
                : ScaleDisplayType.NUMERIC
            : null;
    }

    private EvaluationDirection resolveEvaluationDirection(
        EvaluationDirection evaluationDirection
    ) {
        return evaluationDirection != null
            ? evaluationDirection
            : EvaluationDirection.NEUTRAL;
    }

    private String resolveLeftLabel(
        QuestionType type,
        String leftLabel
    ) {
        return type == QuestionType.BIPOLAR_SCALE
            ? normalizeText(leftLabel)
            : null;
    }

    private String resolveRightLabel(
        QuestionType type,
        String rightLabel
    ) {
        return type == QuestionType.BIPOLAR_SCALE
            ? normalizeText(rightLabel)
            : null;
    }

    /**
     * Construye las opciones únicamente para los tipos
     * de pregunta que las admiten.
     */
    private List<OptionConfig> buildOptions(
        QuestionType type,
        List<OptionConfigRequestDto> options
    ) {
        if (!requiresOptions(type)) {
            return new ArrayList<>();
        }

        return options.stream()
            .map(option ->
                OptionConfig.builder()
                    .label(option.label().trim())
                    .storedValue(
                        resolveStoredValue(option)
                    )
                    .scoreValue(option.scoreValue())
                    .nonResponseOption(
                        Boolean.TRUE.equals(
                            option.nonResponseOption()
                        )
                    )
                    .allowsFreeText(
                        Boolean.TRUE.equals(
                            option.allowsFreeText()
                        )
                    )
                    .build()
            )
            .toList();
    }

    /**
     * Construye las filas únicamente para preguntas
     * de tipo matriz.
     */
    private List<MatrixRowConfig> buildMatrixRows(
        QuestionType type,
        List<MatrixRowRequestDto> rows
    ) {
        if (type != QuestionType.MATRIX_SCALE) {
            return new ArrayList<>();
        }

        return rows.stream()
            .map(row ->
                MatrixRowConfig.builder()
                    .key(
                        !isBlank(row.key())
                            ? row.key().trim()
                            : UUID.randomUUID().toString()
                    )
                    .label(row.label().trim())
                    .build()
            )
            .toList();
    }

    private String resolveStoredValue(
        OptionConfigRequestDto option
    ) {
        return !isBlank(option.storedValue())
            ? option.storedValue().trim()
            : option.label().trim();
    }

    private String normalizeText(String text) {
        return text != null && !text.isBlank()
            ? text.trim()
            : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private FixedQuestionResponseDto mapToResponseDto(
        FixedQuestion question
    ) {
        Long blockId = question.getBlock() != null
            ? question.getBlock().getId()
            : null;

        String blockName = question.getBlock() != null
            ? question.getBlock().getName()
            : null;

        Long createdByUserId =
            question.getCreatedBy() != null
                ? question.getCreatedBy().getId()
                : null;

        return new FixedQuestionResponseDto(
            question.getId(),
            question.getText(),
            question.getDescription(),
            question.getType(),
            question.getEvaluationDirection(),
            question.getStatus(),
            blockId,
            blockName,
            createdByUserId,
            question.isRequiredByDefault(),
            question.getMinValue(),
            question.getMaxValue(),
            question.getScaleDisplayType(),
            question.getLeftLabel(),
            question.getRightLabel(),
            mapOptions(question.getOptions()),
            mapMatrixRows(question.getMatrixRows()),
            question.getCreatedAt(),
            question.getUpdatedAt()
        );
    }

    private List<OptionConfigResponseDto> mapOptions(
        List<OptionConfig> options
    ) {
        if (options == null) {
            return List.of();
        }

        return options.stream()
            .map(option ->
                new OptionConfigResponseDto(
                    option.getLabel(),
                    option.getStoredValue(),
                    option.getScoreValue(),
                    option.isNonResponseOption(),
                    option.isAllowsFreeText()
                )
            )
            .toList();
    }

    private List<MatrixRowResponseDto> mapMatrixRows(
        List<MatrixRowConfig> matrixRows
    ) {
        if (matrixRows == null) {
            return List.of();
        }

        return matrixRows.stream()
            .map(row ->
                new MatrixRowResponseDto(
                    row.getKey(),
                    row.getLabel()
                )
            )
            .toList();
    }
}