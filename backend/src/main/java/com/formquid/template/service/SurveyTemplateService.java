package com.formquid.template.service;

import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockStatus;
import com.formquid.block.repository.BlockRepository;
import com.formquid.fixedquestion.dto.MatrixRowRequestDto;
import com.formquid.fixedquestion.dto.MatrixRowResponseDto;
import com.formquid.fixedquestion.dto.OptionConfigRequestDto;
import com.formquid.fixedquestion.dto.OptionConfigResponseDto;
import com.formquid.fixedquestion.entity.EvaluationDirection;
import com.formquid.fixedquestion.entity.FixedQuestion;
import com.formquid.fixedquestion.entity.FixedQuestionStatus;
import com.formquid.fixedquestion.entity.MatrixRowConfig;
import com.formquid.fixedquestion.entity.OptionConfig;
import com.formquid.fixedquestion.entity.QuestionType;
import com.formquid.fixedquestion.entity.ScaleDisplayType;
import com.formquid.fixedquestion.repository.FixedQuestionRepository;
import com.formquid.template.dto.CreateSurveyTemplateRequestDto;
import com.formquid.template.dto.SurveyTemplateResponseDto;
import com.formquid.template.dto.TemplateBlockRequestDto;
import com.formquid.template.dto.TemplateBlockResponseDto;
import com.formquid.template.dto.TemplateQuestionRequestDto;
import com.formquid.template.dto.TemplateQuestionResponseDto;
import com.formquid.template.dto.UpdateSurveyTemplateRequestDto;
import com.formquid.template.entity.SurveyTemplate;
import com.formquid.template.entity.TemplateBlock;
import com.formquid.template.entity.TemplateQuestion;
import com.formquid.template.entity.TemplateQuestionSource;
import com.formquid.template.entity.TemplateStatus;
import com.formquid.template.repository.SurveyTemplateRepository;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión de plantillas reutilizables.
 *
 * Una plantilla define una estructura base que podrá copiarse posteriormente
 * al crear una encuesta real. Las preguntas fijas se guardan como referencia
 * y las preguntas propias se almacenan dentro de la plantilla.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyTemplateService {

    private final SurveyTemplateRepository surveyTemplateRepository;
    private final FixedQuestionRepository fixedQuestionRepository;
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    /**
     * Crea una plantilla reutilizable.
     *
     * Los bloques y preguntas fijas referenciados se resuelven en lote para
     * evitar consultas repetidas dentro de bucles.
     */
    @Transactional
    public SurveyTemplateResponseDto create(
        CreateSurveyTemplateRequestDto request,
        String creatorEmail
    ) {
        String name = request.name().trim();
        String nameNormalized = normalize(name);

        if (surveyTemplateRepository.existsByNameNormalized(nameNormalized)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ya existe una plantilla con ese nombre"
            );
        }

        SurveyTemplate template = SurveyTemplate.builder()
            .name(name)
            .nameNormalized(nameNormalized)
            .description(cleanOptional(request.description()))
            .status(TemplateStatus.ACTIVE)
            .createdBy(findCreatorByEmail(creatorEmail))
            .build();

        TemplateBuildContext context = buildContext(request.blocks());

        buildBlocks(
            template,
            request.blocks(),
            context
        );

        return toResponse(
            surveyTemplateRepository.save(template)
        );
    }

    /**
     * Lista todas las plantillas.
     */
    public List<SurveyTemplateResponseDto> findAll() {
        return surveyTemplateRepository
            .findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Lista únicamente las plantillas activas.
     */
    public List<SurveyTemplateResponseDto> findActive() {
        return surveyTemplateRepository
            .findByStatusOrderByNameAsc(
                TemplateStatus.ACTIVE
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Obtiene una plantilla por su identificador.
     */
    public SurveyTemplateResponseDto findById(Long id) {
        return toResponse(getTemplate(id));
    }

    /**
     * Actualiza una plantilla reutilizable.
     *
     * Los datos generales se actualizan siempre. La estructura de bloques
     * y preguntas solo se reemplaza cuando blocks viene informado.
     */
    @Transactional
    public SurveyTemplateResponseDto update(
        Long id,
        UpdateSurveyTemplateRequestDto request
    ) {
        SurveyTemplate template = getTemplate(id);

        String name = request.name().trim();
        String nameNormalized = normalize(name);

        surveyTemplateRepository
            .findByNameNormalized(nameNormalized)
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ya existe otra plantilla con ese nombre"
                );
            });

        template.setName(name);
        template.setNameNormalized(nameNormalized);
        template.setDescription(
            cleanOptional(request.description())
        );

        if (request.blocks() != null) {
            replaceBlocks(
                template,
                request.blocks()
            );
        }

        return toResponse(template);
    }

    /**
     * Activa una plantilla.
     */
    @Transactional
    public SurveyTemplateResponseDto activate(Long id) {
        return changeStatus(
            id,
            TemplateStatus.ACTIVE
        );
    }

    /**
     * Desactiva una plantilla sin eliminarla.
     */
    @Transactional
    public SurveyTemplateResponseDto disable(Long id) {
        return changeStatus(
            id,
            TemplateStatus.DISABLED
        );
    }

    /**
     * Archiva una plantilla.
     */
    @Transactional
    public SurveyTemplateResponseDto archive(Long id) {
        return changeStatus(
            id,
            TemplateStatus.ARCHIVED
        );
    }

    /**
     * Centraliza los cambios de estado.
     */
    private SurveyTemplateResponseDto changeStatus(
        Long id,
        TemplateStatus status
    ) {
        SurveyTemplate template = getTemplate(id);

        if (status == TemplateStatus.ACTIVE) {
            validateTemplateHasContent(template);
        }

        template.setStatus(status);

        return toResponse(template);
    }

    /**
     * Reemplaza la estructura completa de bloques y preguntas.
     */
    private void replaceBlocks(
        SurveyTemplate template,
        List<TemplateBlockRequestDto> blockRequests
    ) {
        if (blockRequests == null || blockRequests.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La plantilla debe contener al menos un grupo de preguntas"
            );
        }

        TemplateBuildContext context =
            buildContext(blockRequests);

        template.getBlocks().clear();

        /*
         * Fuerza la eliminación de los elementos anteriores antes de
         * insertar la nueva estructura para evitar conflictos con las
         * restricciones únicas de posición y bloque.
         */
        surveyTemplateRepository.flush();

        buildBlocks(
            template,
            blockRequests,
            context
        );
    }

    /**
     * Construye un contexto interno resolviendo bloques
     * y preguntas fijas en lote.
     */
    private TemplateBuildContext buildContext(
        List<TemplateBlockRequestDto> blockRequests
    ) {
        Set<Long> blockIds =
            collectBlockIds(blockRequests);

        Set<Long> fixedQuestionIds =
            collectFixedQuestionIds(blockRequests);

        return new TemplateBuildContext(
            resolveUsableBlocks(blockIds),
            resolveActiveFixedQuestions(fixedQuestionIds)
        );
    }

    /**
     * Extrae los ids de bloques reutilizables usados en el DTO.
     */
    private Set<Long> collectBlockIds(
        List<TemplateBlockRequestDto> blockRequests
    ) {
        Set<Long> blockIds =
            new LinkedHashSet<>();

        if (blockRequests == null) {
            return blockIds;
        }

        for (TemplateBlockRequestDto blockRequest : blockRequests) {
            if (blockRequest.blockId() != null) {
                blockIds.add(
                    blockRequest.blockId()
                );
            }
        }

        return blockIds;
    }

    /**
     * Extrae los ids de preguntas fijas usadas en el DTO.
     */
    private Set<Long> collectFixedQuestionIds(
        List<TemplateBlockRequestDto> blockRequests
    ) {
        Set<Long> questionIds =
            new LinkedHashSet<>();

        if (blockRequests == null) {
            return questionIds;
        }

        for (TemplateBlockRequestDto blockRequest : blockRequests) {

            if (blockRequest.questions() == null) {
                continue;
            }

            for (
                TemplateQuestionRequestDto questionRequest
                    : blockRequest.questions()
            ) {
                if (
                    questionRequest.source()
                        == TemplateQuestionSource.FIXED
                    && questionRequest.fixedQuestionId() != null
                ) {
                    questionIds.add(
                        questionRequest.fixedQuestionId()
                    );
                }
            }
        }

        return questionIds;
    }

    /**
     * Recupera y valida en lote los bloques reutilizables.
     */
    private Map<Long, Block> resolveUsableBlocks(
        Collection<Long> blockIds
    ) {
        Map<Long, Block> blocksById =
            new HashMap<>();

        if (blockIds.isEmpty()) {
            return blocksById;
        }

        for (
            Block block
                : blockRepository.findByIdIn(blockIds)
        ) {
            blocksById.put(
                block.getId(),
                block
            );
        }

        Set<Long> blockIdsWithActiveQuestions =
            fixedQuestionRepository.findBlockIdsWithActiveQuestions(
                blockIds,
                FixedQuestionStatus.ACTIVE
            );

        for (Long blockId : blockIds) {
            Block block = blocksById.get(blockId);

            if (block == null) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe el bloque indicado"
                );
            }

            if (block.getStatus() != BlockStatus.ACTIVE) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se pueden usar bloques activos"
                );
            }

            if (
                !blockIdsWithActiveQuestions.contains(blockId)
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El bloque indicado debe contener al menos una pregunta fija activa"
                );
            }
        }

        return blocksById;
    }

    /**
     * Recupera y valida en lote las preguntas fijas reutilizadas.
     */
    private Map<Long, FixedQuestion> resolveActiveFixedQuestions(
        Collection<Long> questionIds
    ) {
        Map<Long, FixedQuestion> questionsById =
            new HashMap<>();

        if (questionIds.isEmpty()) {
            return questionsById;
        }

        for (
            FixedQuestion question
                : fixedQuestionRepository.findByIdIn(questionIds)
        ) {
            questionsById.put(
                question.getId(),
                question
            );
        }

        for (Long questionId : questionIds) {

            FixedQuestion question =
                questionsById.get(questionId);

            if (question == null) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe la pregunta fija indicada"
                );
            }

            if (
                question.getStatus()
                    != FixedQuestionStatus.ACTIVE
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se pueden usar preguntas fijas activas"
                );
            }
        }

        return questionsById;
    }

    /**
     * Construye los grupos de la plantilla.
     */
    private void buildBlocks(
        SurveyTemplate template,
        List<TemplateBlockRequestDto> blockRequests,
        TemplateBuildContext context
    ) {
        if (
            blockRequests == null
            || blockRequests.isEmpty()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La plantilla debe contener al menos un grupo de preguntas"
            );
        }

        Set<Long> usedBlockIds =
            new HashSet<>();

        boolean hasUnassignedGroup = false;

        for (int i = 0; i < blockRequests.size(); i++) {

            TemplateBlockRequestDto request =
                blockRequests.get(i);

            Long blockId =
                request.blockId();

            if (blockId == null) {

                if (hasUnassignedGroup) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Solo puede existir un grupo de preguntas sin bloque"
                    );
                }

                hasUnassignedGroup = true;

            } else if (!usedBlockIds.add(blockId)) {

                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La plantilla no puede contener bloques repetidos"
                );
            }

            Block block =
                blockId != null
                    ? context.blocksById().get(blockId)
                    : null;

            TemplateBlock templateBlock =
                TemplateBlock.builder()
                    .block(block)
                    .position(i + 1)
                    .build();

            buildQuestions(
                templateBlock,
                block,
                request.questions(),
                context
            );

            template.addBlock(templateBlock);
        }
    }

    /**
     * Construye las preguntas de un grupo.
     */
    private void buildQuestions(
        TemplateBlock templateBlock,
        Block block,
        List<TemplateQuestionRequestDto> questionRequests,
        TemplateBuildContext context
    ) {
        if (
            questionRequests == null
            || questionRequests.isEmpty()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cada grupo de la plantilla debe contener al menos una pregunta"
            );
        }

        Set<Long> usedFixedQuestionIds =
            new HashSet<>();

        for (int i = 0; i < questionRequests.size(); i++) {

            TemplateQuestionRequestDto request =
                questionRequests.get(i);

            if (request.source() == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El origen de la pregunta es obligatorio"
                );
            }

            TemplateQuestion question =
                switch (request.source()) {
                    case FIXED -> buildFixedQuestion(
                        request,
                        block,
                        usedFixedQuestionIds,
                        context
                    );

                    case CUSTOM ->
                        buildCustomQuestion(request);
                };

            question.setPosition(i + 1);

            templateBlock.addQuestion(question);
        }
    }

    /**
     * Construye una pregunta basada en una pregunta fija.
     */
    private TemplateQuestion buildFixedQuestion(
        TemplateQuestionRequestDto request,
        Block block,
        Set<Long> usedFixedQuestionIds,
        TemplateBuildContext context
    ) {
        Long fixedQuestionId =
            request.fixedQuestionId();

        if (fixedQuestionId == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas fijas deben indicar fixedQuestionId"
            );
        }

        if (
            !usedFixedQuestionIds.add(fixedQuestionId)
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Un grupo no puede contener preguntas fijas repetidas"
            );
        }

        FixedQuestion fixedQuestion =
            context
                .fixedQuestionsById()
                .get(fixedQuestionId);

        validateFixedQuestionBlock(
            fixedQuestion,
            block
        );

        return TemplateQuestion.builder()
            .source(TemplateQuestionSource.FIXED)
            .fixedQuestion(fixedQuestion)
            .required(
                Boolean.TRUE.equals(
                    request.required()
                )
            )
            .build();
    }

    /**
     * Construye una pregunta propia.
     */
    private TemplateQuestion buildCustomQuestion(
        TemplateQuestionRequestDto request
    ) {
        if (request.fixedQuestionId() != null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Una pregunta propia no debe indicar fixedQuestionId"
            );
        }

        if (!StringUtils.hasText(request.text())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El enunciado de la pregunta propia es obligatorio"
            );
        }

        if (request.type() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El tipo de la pregunta propia es obligatorio"
            );
        }

        List<OptionConfig> options =
            buildOptions(request.options());

        List<MatrixRowConfig> matrixRows =
            buildMatrixRows(request.matrixRows());

        Integer minValue =
            resolveMin(
                request.type(),
                request.minValue()
            );

        Integer maxValue =
            resolveMax(
                request.type(),
                request.maxValue()
            );

        ScaleDisplayType scaleDisplayType =
            resolveScaleDisplay(
                request.type(),
                request.scaleDisplayType()
            );

        validateQuestionConfig(
            request.type(),
            options,
            matrixRows,
            minValue,
            maxValue,
            request.leftLabel(),
            request.rightLabel()
        );

        return TemplateQuestion.builder()
            .source(TemplateQuestionSource.CUSTOM)
            .text(request.text().trim())
            .description(
                cleanOptional(request.description())
            )
            .type(request.type())
            .evaluationDirection(
                resolveEvaluationDirection(
                    request.evaluationDirection()
                )
            )
            .required(
                Boolean.TRUE.equals(
                    request.required()
                )
            )
            .minValue(minValue)
            .maxValue(maxValue)
            .scaleDisplayType(scaleDisplayType)
            .leftLabel(
                cleanOptional(request.leftLabel())
            )
            .rightLabel(
                cleanOptional(request.rightLabel())
            )
            .options(options)
            .matrixRows(matrixRows)
            .build();
    }

    /**
     * Comprueba que una pregunta fija se use en el bloque correcto.
     */
    private void validateFixedQuestionBlock(
        FixedQuestion fixedQuestion,
        Block block
    ) {
        Block questionBlock =
            fixedQuestion.getBlock();

        if (
            block == null
            && questionBlock != null
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas fijas asociadas a un bloque deben usarse dentro de su bloque"
            );
        }

        if (
            block != null
            && questionBlock == null
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Una pregunta fija sin bloque no puede añadirse dentro de un bloque"
            );
        }

        if (
            block != null
            && questionBlock != null
            && !questionBlock
                .getId()
                .equals(block.getId())
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La pregunta fija no pertenece al bloque indicado"
            );
        }
    }

    /**
     * Valida la configuración de una pregunta propia.
     */
    private void validateQuestionConfig(
        QuestionType type,
        List<OptionConfig> options,
        List<MatrixRowConfig> matrixRows,
        Integer minValue,
        Integer maxValue,
        String leftLabel,
        String rightLabel
    ) {
        boolean selectionType =
            type == QuestionType.SINGLE_CHOICE
            || type == QuestionType.MULTIPLE_CHOICE;

        boolean matrixType =
            type == QuestionType.MATRIX_SCALE;

        if (
            selectionType
            && options.size() < 2
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas de selección deben tener al menos dos opciones"
            );
        }

        if (
            !selectionType
            && !options.isEmpty()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo las preguntas de selección pueden tener opciones"
            );
        }

        if (
            matrixType
            && matrixRows.isEmpty()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas de matriz deben tener al menos una fila"
            );
        }

        if (
            !matrixType
            && !matrixRows.isEmpty()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo las preguntas de matriz pueden tener filas"
            );
        }

        if (
            minValue != null
            && maxValue != null
            && minValue >= maxValue
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El valor mínimo debe ser menor que el valor máximo"
            );
        }

        if (
            type == QuestionType.BIPOLAR_SCALE
            && (
                !StringUtils.hasText(leftLabel)
                || !StringUtils.hasText(rightLabel)
            )
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las escalas bipolares deben tener etiqueta izquierda y derecha"
            );
        }
    }

    /**
     * Construye y valida las opciones.
     */
    private List<OptionConfig> buildOptions(
        List<OptionConfigRequestDto> optionRequests
    ) {
        if (
            optionRequests == null
            || optionRequests.isEmpty()
        ) {
            return new ArrayList<>();
        }

        List<OptionConfig> options =
            new ArrayList<>();

        Set<String> usedValues =
            new HashSet<>();

        for (
            OptionConfigRequestDto optionRequest
                : optionRequests
        ) {
            if (
                !StringUtils.hasText(
                    optionRequest.label()
                )
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La etiqueta de la opción es obligatoria"
                );
            }

            String label =
                optionRequest.label().trim();

            String storedValue =
                resolveStoredValue(
                    optionRequest.storedValue(),
                    label
                );

            if (!usedValues.add(storedValue)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La pregunta contiene opciones repetidas"
                );
            }

            options.add(
                OptionConfig.builder()
                    .label(label)
                    .storedValue(storedValue)
                    .scoreValue(
                        optionRequest.scoreValue()
                    )
                    .nonResponseOption(
                        Boolean.TRUE.equals(
                            optionRequest.nonResponseOption()
                        )
                    )
                    .allowsFreeText(
                        Boolean.TRUE.equals(
                            optionRequest.allowsFreeText()
                        )
                    )
                    .build()
            );
        }

        return options;
    }

    /**
     * Construye y valida las filas de matriz.
     */
    private List<MatrixRowConfig> buildMatrixRows(
        List<MatrixRowRequestDto> rowRequests
    ) {
        if (
            rowRequests == null
            || rowRequests.isEmpty()
        ) {
            return new ArrayList<>();
        }

        List<MatrixRowConfig> rows =
            new ArrayList<>();

        Set<String> usedKeys =
            new HashSet<>();

        for (
            MatrixRowRequestDto rowRequest
                : rowRequests
        ) {
            if (
                !StringUtils.hasText(
                    rowRequest.label()
                )
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La etiqueta de la fila de matriz es obligatoria"
                );
            }

            String label =
                rowRequest.label().trim();

            String key =
                StringUtils.hasText(rowRequest.key())
                    ? rowRequest.key().trim()
                    : UUID.randomUUID().toString();

            if (!usedKeys.add(key)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La pregunta contiene filas de matriz repetidas"
                );
            }

            rows.add(
                MatrixRowConfig.builder()
                    .key(key)
                    .label(label)
                    .build()
            );
        }

        return rows;
    }

    /**
     * Obtiene una plantilla o devuelve 404.
     */
    private SurveyTemplate getTemplate(Long id) {
        return surveyTemplateRepository
            .findDetailedById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe la plantilla indicada"
                )
            );
    }

    /**
     * Obtiene el usuario creador a partir del correo autenticado.
     */
    private User findCreatorByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return userRepository
            .findByEmail(email)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario autenticado no encontrado"
                )
            );
    }

    /**
     * Evita activar una plantilla sin contenido.
     */
    private void validateTemplateHasContent(
        SurveyTemplate template
    ) {
        boolean hasContent =
            template.getBlocks() != null
            && template.getBlocks()
                .stream()
                .anyMatch(block ->
                    block.getQuestions() != null
                    && !block
                        .getQuestions()
                        .isEmpty()
                );

        if (!hasContent) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La plantilla debe contener al menos una pregunta"
            );
        }
    }

    /**
     * Convierte una plantilla a DTO.
     */
    private SurveyTemplateResponseDto toResponse(
        SurveyTemplate template
    ) {
        return new SurveyTemplateResponseDto(
            template.getId(),
            template.getName(),
            template.getDescription(),
            template.getStatus(),
            template.getCreatedBy() != null
                ? template.getCreatedBy().getId()
                : null,
            template.getBlocks()
                .stream()
                .map(this::toBlockResponse)
                .toList(),
            template.getCreatedAt(),
            template.getUpdatedAt()
        );
    }

    /**
     * Convierte un bloque de plantilla a DTO.
     */
    private TemplateBlockResponseDto toBlockResponse(
        TemplateBlock templateBlock
    ) {
        Block block =
            templateBlock.getBlock();

        return new TemplateBlockResponseDto(
            templateBlock.getId(),
            block != null
                ? block.getId()
                : null,
            block != null
                ? block.getName()
                : "Preguntas sin bloque",
            templateBlock.getQuestions()
                .stream()
                .map(this::toQuestionResponse)
                .toList()
        );
    }

    /**
     * Convierte una pregunta de plantilla a DTO.
     */
    private TemplateQuestionResponseDto toQuestionResponse(
        TemplateQuestion templateQuestion
    ) {
        if (
            templateQuestion.getSource()
                == TemplateQuestionSource.FIXED
        ) {
            return toFixedQuestionResponse(
                templateQuestion
            );
        }

        return toCustomQuestionResponse(
            templateQuestion
        );
    }

    /**
     * Convierte una pregunta fija a DTO.
     */
    private TemplateQuestionResponseDto toFixedQuestionResponse(
        TemplateQuestion templateQuestion
    ) {
        FixedQuestion fixedQuestion =
            templateQuestion.getFixedQuestion();

        return new TemplateQuestionResponseDto(
            templateQuestion.getId(),
            TemplateQuestionSource.FIXED,
            fixedQuestion.getId(),
            fixedQuestion.getText(),
            fixedQuestion.getDescription(),
            fixedQuestion.getType(),
            fixedQuestion.getEvaluationDirection(),
            templateQuestion.isRequired(),
            fixedQuestion.getMinValue(),
            fixedQuestion.getMaxValue(),
            fixedQuestion.getScaleDisplayType(),
            fixedQuestion.getLeftLabel(),
            fixedQuestion.getRightLabel(),
            toOptionResponses(
                fixedQuestion.getOptions()
            ),
            toMatrixRowResponses(
                fixedQuestion.getMatrixRows()
            )
        );
    }

    /**
     * Convierte una pregunta propia a DTO.
     */
    private TemplateQuestionResponseDto toCustomQuestionResponse(
        TemplateQuestion templateQuestion
    ) {
        return new TemplateQuestionResponseDto(
            templateQuestion.getId(),
            TemplateQuestionSource.CUSTOM,
            null,
            templateQuestion.getText(),
            templateQuestion.getDescription(),
            templateQuestion.getType(),
            templateQuestion.getEvaluationDirection(),
            templateQuestion.isRequired(),
            templateQuestion.getMinValue(),
            templateQuestion.getMaxValue(),
            templateQuestion.getScaleDisplayType(),
            templateQuestion.getLeftLabel(),
            templateQuestion.getRightLabel(),
            toOptionResponses(
                templateQuestion.getOptions()
            ),
            toMatrixRowResponses(
                templateQuestion.getMatrixRows()
            )
        );
    }

    private List<OptionConfigResponseDto> toOptionResponses(
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

    private List<MatrixRowResponseDto> toMatrixRowResponses(
        List<MatrixRowConfig> rows
    ) {
        if (rows == null) {
            return List.of();
        }

        return rows.stream()
            .map(row ->
                new MatrixRowResponseDto(
                    row.getKey(),
                    row.getLabel()
                )
            )
            .toList();
    }

    private Integer resolveMin(
        QuestionType type,
        Integer value
    ) {
        if (type == QuestionType.NUMBER) {
            return value;
        }

        if (isScaleType(type)) {
            return value != null
                ? value
                : 1;
        }

        return null;
    }

    private Integer resolveMax(
        QuestionType type,
        Integer value
    ) {
        if (type == QuestionType.NUMBER) {
            return value;
        }

        if (isScaleType(type)) {
            return value != null
                ? value
                : 5;
        }

        return null;
    }

    private ScaleDisplayType resolveScaleDisplay(
        QuestionType type,
        ScaleDisplayType scaleDisplayType
    ) {
        if (isScaleType(type)) {
            return scaleDisplayType != null
                ? scaleDisplayType
                : ScaleDisplayType.NUMERIC;
        }

        return null;
    }

    private boolean isScaleType(QuestionType type) {
        return type == QuestionType.SCALE
            || type == QuestionType.MATRIX_SCALE
            || type == QuestionType.BIPOLAR_SCALE;
    }

    private EvaluationDirection resolveEvaluationDirection(
        EvaluationDirection direction
    ) {
        return direction != null
            ? direction
            : EvaluationDirection.NEUTRAL;
    }

    private String resolveStoredValue(
        String storedValue,
        String label
    ) {
        if (StringUtils.hasText(storedValue)) {
            return storedValue.trim();
        }

        return normalize(label);
    }

    private String normalize(String value) {
        String normalized =
            Normalizer.normalize(
                value.trim(),
                Normalizer.Form.NFD
            )
            .replaceAll("\\p{M}", "");

        return normalized.toLowerCase(
            Locale.ROOT
        );
    }

    private String cleanOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    /**
     * Contexto interno con entidades resueltas en lote.
     */
    private record TemplateBuildContext(
        Map<Long, Block> blocksById,
        Map<Long, FixedQuestion> fixedQuestionsById
    ) {}
}