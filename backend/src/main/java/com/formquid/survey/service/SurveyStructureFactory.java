package com.formquid.survey.service;

import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockStatus;
import com.formquid.block.repository.BlockRepository;
import com.formquid.question.dto.MatrixRowRequestDto;
import com.formquid.question.dto.OptionConfigRequestDto;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.MatrixRowConfig;
import com.formquid.question.entity.OptionConfig;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionStatus;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.entity.ScaleDisplayType;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.survey.dto.SurveyBlockRequestDto;
import com.formquid.survey.dto.SurveyQuestionRequestDto;
import com.formquid.survey.model.SurveyBlockSnapshot;
import com.formquid.survey.model.SurveyQuestionSnapshot;
import com.formquid.survey.model.SurveyQuestionSource;
import com.formquid.survey.model.SurveyStructure;
import com.formquid.template.dto.TemplateBlockRequestDto;
import com.formquid.template.dto.TemplateQuestionRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Construye snapshots de estructura para encuestas y plantillas.
 *
 * Esta clase concentra la lógica de copy-on-write sin introducir tablas de
 * versionado: cada uso de un elemento del catálogo se guarda como copia JSONB.
 */
@Component
@RequiredArgsConstructor
public class SurveyStructureFactory {

    private final BlockRepository blockRepository;
    private final QuestionRepository questionRepository;

    /**
     * Construye la estructura propia de una encuesta.
     */
    public SurveyStructure buildSurveyStructure(
        List<SurveyBlockRequestDto> blocks,
        boolean allowEmpty
    ) {
        return buildStructure(
            toSurveyBlockInputs(blocks),
            allowEmpty
        );
    }

    /**
     * Construye la estructura propia de una plantilla.
     */
    public SurveyStructure buildTemplateStructure(
        List<TemplateBlockRequestDto> blocks
    ) {
        return buildStructure(
            toTemplateBlockInputs(blocks),
            false
        );
    }

    /**
     * Copia una estructura existente para aislarla del objeto origen.
     */
    public SurveyStructure copyStructure(SurveyStructure source) {
        if (source == null || source.getBlocks() == null) {
            return new SurveyStructure();
        }

        List<SurveyBlockSnapshot> blocks = source.getBlocks()
            .stream()
            .map(this::copyBlock)
            .toList();

        return SurveyStructure.builder()
            .blocks(blocks)
            .build();
    }

    private SurveyStructure buildStructure(
        List<BlockInput> blockInputs,
        boolean allowEmpty
    ) {
        if (blockInputs.isEmpty()) {
            if (allowEmpty) {
                return new SurveyStructure();
            }

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La estructura debe contener al menos un grupo de preguntas"
            );
        }

        StructureContext context = buildContext(blockInputs);
        Set<String> usedQuestionKeys = new HashSet<>();
        List<SurveyBlockSnapshot> blocks = new ArrayList<>();
        int blockPosition = 1;

        for (BlockInput blockInput : blockInputs) {
            blocks.add(
                buildBlock(
                    blockInput,
                    blockPosition++,
                    context,
                    usedQuestionKeys
                )
            );
        }

        return SurveyStructure.builder()
            .blocks(blocks)
            .build();
    }

    private SurveyBlockSnapshot buildBlock(
        BlockInput blockInput,
        int position,
        StructureContext context,
        Set<String> usedQuestionKeys
    ) {
        if (blockInput.questions().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cada grupo debe contener al menos una pregunta"
            );
        }

        Block sourceBlock = resolveSourceBlock(
            blockInput.sourceBlockId(),
            context.blocksById()
        );

        List<SurveyQuestionSnapshot> questions = new ArrayList<>();
        int questionPosition = 1;

        for (QuestionInput questionInput : blockInput.questions()) {
            SurveyQuestionSnapshot question = buildQuestion(
                questionInput,
                sourceBlock,
                questionPosition++,
                context.questionsById()
            );

            if (!usedQuestionKeys.add(question.getQuestionKey())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La estructura contiene claves de pregunta repetidas"
                );
            }

            questions.add(question);
        }

        return SurveyBlockSnapshot.builder()
            .blockKey(resolveKey(blockInput.blockKey()))
            .sourceBlockId(sourceBlock != null ? sourceBlock.getId() : null)
            .title(resolveBlockTitle(blockInput.title(), sourceBlock, position))
            .position(position)
            .questions(questions)
            .build();
    }

    private SurveyQuestionSnapshot buildQuestion(
        QuestionInput input,
        Block sourceBlock,
        int position,
        Map<Long, Question> questionsById
    ) {
        if (input.source() == SurveyQuestionSource.CATALOG) {
            return buildCatalogQuestion(
                input,
                sourceBlock,
                position,
                questionsById
            );
        }

        if (input.source() == SurveyQuestionSource.CUSTOM) {
            return buildCustomQuestion(
                input,
                position
            );
        }

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El origen de la pregunta es obligatorio"
        );
    }

    private SurveyQuestionSnapshot buildCatalogQuestion(
        QuestionInput input,
        Block sourceBlock,
        int position,
        Map<Long, Question> questionsById
    ) {
        if (input.sourceQuestionId() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas de catálogo deben indicar sourceQuestionId"
            );
        }

        Question question = questionsById.get(input.sourceQuestionId());

        if (question == null || question.getStatus() != QuestionStatus.ACTIVE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La pregunta de catálogo indicada no está activa o no existe"
            );
        }

        validateQuestionBelongsToBlock(sourceBlock, question);

        return SurveyQuestionSnapshot.builder()
            .questionKey(resolveKey(input.questionKey()))
            .sourceQuestionId(question.getId())
            .source(SurveyQuestionSource.CATALOG)
            .text(question.getText())
            .description(question.getDescription())
            .type(question.getType())
            .evaluationDirection(question.getEvaluationDirection())
            .required(resolveRequired(input.required(), question.isRequiredByDefault()))
            .minValue(question.getMinValue())
            .maxValue(question.getMaxValue())
            .scaleDisplayType(question.getScaleDisplayType())
            .leftLabel(question.getLeftLabel())
            .rightLabel(question.getRightLabel())
            .options(copyOptions(question.getOptions()))
            .matrixRows(copyMatrixRows(question.getMatrixRows()))
            .position(position)
            .build();
    }

    private SurveyQuestionSnapshot buildCustomQuestion(
        QuestionInput input,
        int position
    ) {
        if (input.sourceQuestionId() != null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Una pregunta propia no debe indicar sourceQuestionId"
            );
        }

        validateQuestionConfiguration(
            input.text(),
            input.type(),
            input.options(),
            input.matrixRows(),
            input.minValue(),
            input.maxValue(),
            input.leftLabel(),
            input.rightLabel()
        );

        return SurveyQuestionSnapshot.builder()
            .questionKey(resolveKey(input.questionKey()))
            .source(SurveyQuestionSource.CUSTOM)
            .text(input.text().trim())
            .description(cleanOptional(input.description()))
            .type(input.type())
            .evaluationDirection(resolveEvaluationDirection(input.evaluationDirection()))
            .required(Boolean.TRUE.equals(input.required()))
            .minValue(resolveMinValue(input.type(), input.minValue()))
            .maxValue(resolveMaxValue(input.type(), input.maxValue()))
            .scaleDisplayType(resolveScaleDisplayType(input.type(), input.scaleDisplayType()))
            .leftLabel(resolveLeftLabel(input.type(), input.leftLabel()))
            .rightLabel(resolveRightLabel(input.type(), input.rightLabel()))
            .options(buildOptions(input.type(), input.options()))
            .matrixRows(buildMatrixRows(input.type(), input.matrixRows()))
            .position(position)
            .build();
    }

    private StructureContext buildContext(List<BlockInput> blockInputs) {
        Set<Long> blockIds = new LinkedHashSet<>();
        Set<Long> questionIds = new LinkedHashSet<>();

        for (BlockInput blockInput : blockInputs) {
            if (blockInput.sourceBlockId() != null) {
                blockIds.add(blockInput.sourceBlockId());
            }

            for (QuestionInput questionInput : blockInput.questions()) {
                if (questionInput.source() == SurveyQuestionSource.CATALOG
                    && questionInput.sourceQuestionId() != null) {
                    questionIds.add(questionInput.sourceQuestionId());
                }
            }
        }

        return new StructureContext(
            resolveBlocks(blockIds),
            resolveQuestions(questionIds)
        );
    }

    private Map<Long, Block> resolveBlocks(Collection<Long> blockIds) {
        Map<Long, Block> blocksById = new HashMap<>();

        if (blockIds.isEmpty()) {
            return blocksById;
        }

        for (Block block : blockRepository.findByIdIn(blockIds)) {
            blocksById.put(block.getId(), block);
        }

        for (Long blockId : blockIds) {
            Block block = blocksById.get(blockId);

            if (block == null || block.getStatus() != BlockStatus.ACTIVE) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El bloque de catálogo indicado no está activo o no existe"
                );
            }
        }

        return blocksById;
    }

    private Map<Long, Question> resolveQuestions(Collection<Long> questionIds) {
        Map<Long, Question> questionsById = new HashMap<>();

        if (questionIds.isEmpty()) {
            return questionsById;
        }

        for (Question question : questionRepository.findByIdIn(questionIds)) {
            questionsById.put(question.getId(), question);
        }

        return questionsById;
    }

    private Block resolveSourceBlock(Long blockId, Map<Long, Block> blocksById) {
        return blockId != null ? blocksById.get(blockId) : null;
    }

    private void validateQuestionBelongsToBlock(Block sourceBlock, Question question) {
        if (sourceBlock == null || question.getBlock() == null) {
            return;
        }

        if (!sourceBlock.getId().equals(question.getBlock().getId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La pregunta de catálogo no pertenece al bloque indicado"
            );
        }
    }

    private void validateQuestionConfiguration(
        String text,
        QuestionType type,
        List<OptionConfigRequestDto> options,
        List<MatrixRowRequestDto> matrixRows,
        Integer minValue,
        Integer maxValue,
        String leftLabel,
        String rightLabel
    ) {
        if (!StringUtils.hasText(text)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El enunciado de la pregunta es obligatorio"
            );
        }

        if (type == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El tipo de pregunta es obligatorio"
            );
        }

        if (requiresOptions(type) && (options == null || options.size() < 2)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas de selección deben tener al menos dos opciones"
            );
        }

        if (!requiresOptions(type) && options != null && !options.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Este tipo de pregunta no admite opciones"
            );
        }

        if (type == QuestionType.MATRIX_SCALE
            && (matrixRows == null || matrixRows.isEmpty())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas de matriz deben tener al menos una fila"
            );
        }

        if (type != QuestionType.MATRIX_SCALE
            && matrixRows != null
            && !matrixRows.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Este tipo de pregunta no admite filas de matriz"
            );
        }

        if (isScaleType(type)
            && minValue != null
            && maxValue != null
            && minValue >= maxValue) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El valor mínimo debe ser menor que el valor máximo"
            );
        }

        if (type == QuestionType.BIPOLAR_SCALE
            && (!StringUtils.hasText(leftLabel) || !StringUtils.hasText(rightLabel))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las escalas bipolares deben tener etiqueta izquierda y derecha"
            );
        }
    }

    private List<OptionConfig> buildOptions(
        QuestionType type,
        List<OptionConfigRequestDto> options
    ) {
        if (!requiresOptions(type)) {
            return new ArrayList<>();
        }

        return options.stream()
            .map(option -> OptionConfig.builder()
                .label(option.label().trim())
                .storedValue(resolveStoredValue(option))
                .scoreValue(option.scoreValue())
                .nonResponseOption(Boolean.TRUE.equals(option.nonResponseOption()))
                .allowsFreeText(Boolean.TRUE.equals(option.allowsFreeText()))
                .build())
            .toList();
    }

    private List<MatrixRowConfig> buildMatrixRows(
        QuestionType type,
        List<MatrixRowRequestDto> rows
    ) {
        if (type != QuestionType.MATRIX_SCALE) {
            return new ArrayList<>();
        }

        return rows.stream()
            .map(row -> MatrixRowConfig.builder()
                .key(StringUtils.hasText(row.key()) ? row.key().trim() : UUID.randomUUID().toString())
                .label(row.label().trim())
                .build())
            .toList();
    }

    private List<OptionConfig> copyOptions(List<OptionConfig> options) {
        if (options == null) {
            return new ArrayList<>();
        }

        return options.stream()
            .map(option -> OptionConfig.builder()
                .label(option.getLabel())
                .storedValue(option.getStoredValue())
                .scoreValue(option.getScoreValue())
                .nonResponseOption(option.isNonResponseOption())
                .allowsFreeText(option.isAllowsFreeText())
                .build())
            .toList();
    }

    private List<MatrixRowConfig> copyMatrixRows(List<MatrixRowConfig> rows) {
        if (rows == null) {
            return new ArrayList<>();
        }

        return rows.stream()
            .map(row -> MatrixRowConfig.builder()
                .key(row.getKey())
                .label(row.getLabel())
                .build())
            .toList();
    }

    private SurveyBlockSnapshot copyBlock(SurveyBlockSnapshot source) {
        return SurveyBlockSnapshot.builder()
            .blockKey(source.getBlockKey())
            .sourceBlockId(source.getSourceBlockId())
            .title(source.getTitle())
            .position(source.getPosition())
            .questions(source.getQuestions() == null
                ? new ArrayList<>()
                : source.getQuestions().stream().map(this::copyQuestion).toList())
            .build();
    }

    private SurveyQuestionSnapshot copyQuestion(SurveyQuestionSnapshot source) {
        return SurveyQuestionSnapshot.builder()
            .questionKey(source.getQuestionKey())
            .sourceQuestionId(source.getSourceQuestionId())
            .source(source.getSource())
            .text(source.getText())
            .description(source.getDescription())
            .type(source.getType())
            .evaluationDirection(source.getEvaluationDirection())
            .required(source.isRequired())
            .minValue(source.getMinValue())
            .maxValue(source.getMaxValue())
            .scaleDisplayType(source.getScaleDisplayType())
            .leftLabel(source.getLeftLabel())
            .rightLabel(source.getRightLabel())
            .options(copyOptions(source.getOptions()))
            .matrixRows(copyMatrixRows(source.getMatrixRows()))
            .position(source.getPosition())
            .build();
    }

    private List<BlockInput> toSurveyBlockInputs(List<SurveyBlockRequestDto> requests) {
        if (requests == null) {
            return List.of();
        }

        return requests.stream()
            .map(request -> new BlockInput(
                request.blockKey(),
                request.sourceBlockId(),
                request.title(),
                toSurveyQuestionInputs(request.questions())
            ))
            .toList();
    }

    private List<BlockInput> toTemplateBlockInputs(List<TemplateBlockRequestDto> requests) {
        if (requests == null) {
            return List.of();
        }

        return requests.stream()
            .map(request -> new BlockInput(
                request.blockKey(),
                request.blockId(),
                null,
                toTemplateQuestionInputs(request.questions())
            ))
            .toList();
    }

    private List<QuestionInput> toSurveyQuestionInputs(List<SurveyQuestionRequestDto> requests) {
        if (requests == null) {
            return List.of();
        }

        return requests.stream()
            .map(request -> new QuestionInput(
                request.questionKey(),
                request.source(),
                request.sourceQuestionId(),
                request.text(),
                request.description(),
                request.type(),
                request.evaluationDirection(),
                request.required(),
                request.minValue(),
                request.maxValue(),
                request.scaleDisplayType(),
                request.leftLabel(),
                request.rightLabel(),
                request.options(),
                request.matrixRows()
            ))
            .toList();
    }

    private List<QuestionInput> toTemplateQuestionInputs(List<TemplateQuestionRequestDto> requests) {
        if (requests == null) {
            return List.of();
        }

        return requests.stream()
            .map(request -> new QuestionInput(
                request.questionKey(),
                request.source(),
                request.sourceQuestionId(),
                request.text(),
                request.description(),
                request.type(),
                request.evaluationDirection(),
                request.required(),
                request.minValue(),
                request.maxValue(),
                request.scaleDisplayType(),
                request.leftLabel(),
                request.rightLabel(),
                request.options(),
                request.matrixRows()
            ))
            .toList();
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

    private Boolean resolveRequired(Boolean requestedRequired, boolean defaultRequired) {
        return requestedRequired != null ? requestedRequired : defaultRequired;
    }

    private Integer resolveMinValue(QuestionType type, Integer minValue) {
        return isScaleType(type)
            ? minValue != null ? minValue : 1
            : null;
    }

    private Integer resolveMaxValue(QuestionType type, Integer maxValue) {
        return isScaleType(type)
            ? maxValue != null ? maxValue : 5
            : null;
    }

    private ScaleDisplayType resolveScaleDisplayType(
        QuestionType type,
        ScaleDisplayType scaleDisplayType
    ) {
        return isScaleType(type)
            ? scaleDisplayType != null ? scaleDisplayType : ScaleDisplayType.NUMERIC
            : null;
    }

    private EvaluationDirection resolveEvaluationDirection(
        EvaluationDirection evaluationDirection
    ) {
        return evaluationDirection != null
            ? evaluationDirection
            : EvaluationDirection.NEUTRAL;
    }

    private String resolveLeftLabel(QuestionType type, String leftLabel) {
        return type == QuestionType.BIPOLAR_SCALE ? cleanOptional(leftLabel) : null;
    }

    private String resolveRightLabel(QuestionType type, String rightLabel) {
        return type == QuestionType.BIPOLAR_SCALE ? cleanOptional(rightLabel) : null;
    }

    private String resolveStoredValue(OptionConfigRequestDto option) {
        return StringUtils.hasText(option.storedValue())
            ? option.storedValue().trim()
            : option.label().trim();
    }

    private String resolveBlockTitle(
        String requestedTitle,
        Block sourceBlock,
        int position
    ) {
        if (StringUtils.hasText(requestedTitle)) {
            return requestedTitle.trim();
        }

        if (sourceBlock != null) {
            return sourceBlock.getName();
        }

        return "Grupo " + position;
    }

    private String resolveKey(String requestedKey) {
        return StringUtils.hasText(requestedKey)
            ? requestedKey.trim()
            : UUID.randomUUID().toString();
    }

    private String cleanOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record StructureContext(
        Map<Long, Block> blocksById,
        Map<Long, Question> questionsById
    ) {}

    private record BlockInput(
        String blockKey,
        Long sourceBlockId,
        String title,
        List<QuestionInput> questions
    ) {}

    private record QuestionInput(
        String questionKey,
        SurveyQuestionSource source,
        Long sourceQuestionId,
        String text,
        String description,
        QuestionType type,
        EvaluationDirection evaluationDirection,
        Boolean required,
        Integer minValue,
        Integer maxValue,
        ScaleDisplayType scaleDisplayType,
        String leftLabel,
        String rightLabel,
        List<OptionConfigRequestDto> options,
        List<MatrixRowRequestDto> matrixRows
    ) {}
}
