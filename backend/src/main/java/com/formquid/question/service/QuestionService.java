package com.formquid.question.service;

import com.formquid.block.entity.BlockScope;
import com.formquid.common.Category;
import com.formquid.common.service.CategoryCatalogService;
import com.formquid.common.dto.CatalogSearchField;
import com.formquid.common.dto.CatalogSortOrder;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.common.exception.ApiException;
import com.formquid.content.entity.ContentItem;
import com.formquid.question.dto.CreateFixedQuestionRequestDto;
import com.formquid.question.dto.FixedQuestionSummaryDto;
import com.formquid.question.dto.MatrixRowDto;
import com.formquid.question.dto.QuestionDraftDto;
import com.formquid.question.dto.QuestionOptionDto;
import com.formquid.question.dto.QuestionResponseDto;
import com.formquid.question.dto.QuestionSettingsDto;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.MatrixRow;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionOption;
import com.formquid.question.entity.QuestionScope;
import com.formquid.question.entity.QuestionSettings;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.repository.QuestionRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Gestiona la creación, validación, consulta y transformación de preguntas.
 */
@Service
@RequiredArgsConstructor
public class QuestionService {

    private static final int MAX_SCALE_VALUE_COUNT = 30;

    private final QuestionRepository questionRepository;
    private final CategoryCatalogService categoryCatalogService;

    @Transactional(readOnly = true)
    public PageResponseDto<FixedQuestionSummaryDto> findFixedQuestions(
        int page,
        int size,
        User user,
        boolean mine,
        String search,
        Set<CatalogSearchField> searchFields,
        CatalogSortOrder sortOrder,
        Category category
    ) {
        Set<CatalogSearchField> fields = searchFields == null ? Set.of() : searchFields;
        Pageable pageable = catalogPageable(page, size, sortOrder);
        Page<FixedQuestionSummaryDto> questions = questionRepository.findCatalogSummaries(
            QuestionScope.CATALOG_FIXED,
            user.getId(),
            mine,
            normalizeSearch(search),
            fields.contains(CatalogSearchField.TITLE),
            fields.contains(CatalogSearchField.DESCRIPTION),
            fields.contains(CatalogSearchField.CREATOR),
            category,
            pageable
        );
        return toPageResponse(questions);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<QuestionResponseDto> findFixedQuestionsForBuilder(int page, int size, Category category) {
        Pageable pageable = builderPageable(page, size);
        Page<QuestionResponseDto> questions = questionRepository
            .findCatalogQuestionsWithDetails(QuestionScope.CATALOG_FIXED, category, pageable)
            .map(this::toResponse);
        return toPageResponse(questions);
    }

    private <T> PageResponseDto<T> toPageResponse(Page<T> page) {
        return new PageResponseDto<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public QuestionResponseDto findFixedQuestion(Long questionId) {
        Question question = getQuestion(questionId);
        if (question.getScope() != QuestionScope.CATALOG_FIXED) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Pregunta fija no encontrada");
        }
        return toResponse(question);
    }

    @Transactional
    public void deleteFixedQuestion(Long questionId, User user) {
        Question question = getQuestion(questionId);
        if (question.getScope() != QuestionScope.CATALOG_FIXED) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Pregunta fija no encontrada");
        }
        requireManagementPermission(question.getCreatedBy(), user);
        questionRepository.delete(question);
    }

    public Question getQuestion(Long questionId) {
        return questionRepository.findById(questionId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pregunta no encontrada"));
    }

    public void validateQuestionDraft(QuestionDraftDto dto) {
        validateQuestion(dto);
    }

    /**
     * Recibe una pregunta fija y devuelve la entidad de catálogo creada.
     */
    @Transactional
    public QuestionResponseDto createFixedQuestion(CreateFixedQuestionRequestDto request, User creator) {
        Question question = buildQuestion(
            withoutEvaluationConfiguration(request.question()),
            QuestionScope.CATALOG_FIXED,
            null,
            creator
        );
        question.setCategory(request.category() == null ? Category.OTROS : request.category());
        categoryCatalogService.requireExisting(question.getCategory());
        return toResponse(questionRepository.save(question));
    }

    @Transactional
    public QuestionResponseDto updateFixedQuestion(
        Long questionId,
        CreateFixedQuestionRequestDto request,
        User user
    ) {
        Question question = getQuestion(questionId);
        if (question.getScope() != QuestionScope.CATALOG_FIXED) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Pregunta fija no encontrada");
        }
        requireManagementPermission(question.getCreatedBy(), user);
        if (request.category() != null) {
            categoryCatalogService.requireExisting(request.category());
            question.setCategory(request.category());
        }
        return toResponse(applyQuestionDraft(
            question,
            withoutEvaluationConfiguration(request.question())
        ));
    }

    /**
     * Recibe definición local y elemento padre, devuelve pregunta creada.
     */
    public Question createLocalQuestion(QuestionDraftDto dto, ContentItem parentItem, User creator) {
        QuestionDraftDto effectiveDraft = isCatalogContext(parentItem)
            ? withoutEvaluationConfiguration(dto)
            : dto;
        return questionRepository.save(buildQuestion(
            effectiveDraft,
            QuestionScope.LOCAL,
            parentItem,
            creator
        ));
    }

    /**
     * Recibe pregunta fija y elemento padre, devuelve copia bloqueada.
     */
    public Question createFixedSnapshot(Question fixedQuestion, ContentItem parentItem, User creator) {
        if (fixedQuestion.getScope() != QuestionScope.CATALOG_FIXED) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo pueden copiarse preguntas fijas de catálogo");
        }
        Question copy = copyQuestion(
            fixedQuestion,
            QuestionScope.FIXED_SNAPSHOT,
            parentItem,
            creator,
            !isCatalogContext(parentItem)
        );
        return questionRepository.save(copy);
    }

    /**
     * Recibe una pregunta origen y devuelve una copia local.
     */
    public Question copyAsLocal(Question source, ContentItem parentItem, User creator) {
        QuestionScope scope = source.getScope() == QuestionScope.LOCAL ? QuestionScope.LOCAL : QuestionScope.FIXED_SNAPSHOT;
        return questionRepository.save(copyQuestion(
            source,
            scope,
            parentItem,
            creator,
            !isCatalogContext(parentItem)
        ));
    }

    /**
     * Recibe una pregunta de encuesta y nuevos datos. Devuelve la pregunta actualizada.
     */
    @Transactional
    public Question updateSurveyQuestion(Question question, QuestionDraftDto dto) {
        if (question.getScope() == QuestionScope.CATALOG_FIXED) {
            throw new ApiException(HttpStatus.CONFLICT, "Las preguntas de catálogo no se editan desde una encuesta");
        }
        return applyQuestionDraft(question, dto);
    }

    private Question applyQuestionDraft(Question question, QuestionDraftDto dto) {
        validateQuestion(dto);
        question.setStatement(dto.statement().trim());
        question.setAnnotation(trimToNull(dto.annotation()));
        question.setQuestionType(dto.questionType());
        question.setEvaluationDirection(dto.evaluationDirection());
        question.setEvaluationWeight(weightFor(dto.evaluationDirection(), dto.evaluationWeight()));
        question.setUpdatedAt(Instant.now());
        if (question.getSettings() == null) {
            question.setSettings(toSettings(dto.settings()));
        } else {
            applySettings(question.getSettings(), dto.settings());
        }
        question.getOptions().clear();
        safeOptions(dto.options()).stream().map(this::toOption).forEach(question::addOption);
        question.getMatrixRows().clear();
        safeRows(dto.matrixRows()).stream().map(this::toRow).forEach(question::addMatrixRow);
        return questionRepository.save(question);
    }

    private void requireManagementPermission(User creator, User user) {
        if (user.getRole() == UserRole.SUPERADMIN) return;
        if (creator == null || !creator.getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo puedes gestionar tus propias preguntas");
        }
    }

    private Question buildQuestion(QuestionDraftDto dto, QuestionScope scope, ContentItem parentItem, User creator) {
        validateQuestion(dto);
        Question question = Question.builder()
            .parentItem(parentItem)
            .scope(scope)
            .category(categoryFrom(parentItem))
            .statement(dto.statement().trim())
            .annotation(trimToNull(dto.annotation()))
            .questionType(dto.questionType())
            .evaluationDirection(dto.evaluationDirection())
            .evaluationWeight(weightFor(dto.evaluationDirection(), dto.evaluationWeight()))
            .createdBy(creator)
            .options(new ArrayList<>())
            .matrixRows(new ArrayList<>())
            .build();
        question.setSettings(toSettings(dto.settings()));
        for (QuestionOptionDto option : safeOptions(dto.options())) {
            question.addOption(toOption(option));
        }
        for (MatrixRowDto row : safeRows(dto.matrixRows())) {
            question.addMatrixRow(toRow(row));
        }
        return question;
    }

    private Question copyQuestion(
        Question source,
        QuestionScope scope,
        ContentItem parentItem,
        User creator,
        boolean includeEvaluation
    ) {
        Question copy = Question.builder()
            .parentItem(parentItem)
            .scope(scope)
            .category(source.getCategory())
            .statement(source.getStatement())
            .annotation(source.getAnnotation())
            .questionType(source.getQuestionType())
            .evaluationDirection(includeEvaluation ? source.getEvaluationDirection() : EvaluationDirection.NEUTRAL)
            .evaluationWeight(includeEvaluation ? source.getEvaluationWeight() : null)
            .createdBy(creator)
            .options(new ArrayList<>())
            .matrixRows(new ArrayList<>())
            .build();
        copy.setSettings(copySettings(source.getSettings(), includeEvaluation));
        source.getOptions().stream()
            .sorted(Comparator.comparing(QuestionOption::getPosition))
            .map(option -> copyOption(option, includeEvaluation))
            .forEach(copy::addOption);
        source.getMatrixRows().stream()
            .sorted(Comparator.comparing(MatrixRow::getPosition))
            .map(this::copyRow)
            .forEach(copy::addMatrixRow);
        return copy;
    }

    private void validateQuestion(QuestionDraftDto dto) {
        if (dto == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La pregunta es obligatoria");
        }
        if (dto.statement() == null || dto.statement().isBlank() || dto.questionType() == null || dto.evaluationDirection() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La pregunta debe tener enunciado, tipo y dirección de evaluación");
        }
        validatePositions(dto);
        validateCommonSettings(dto.settings());
        validateEvaluationFactors(dto);
        if (dto.evaluationDirection() == EvaluationDirection.NEUTRAL && dto.evaluationWeight() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las preguntas neutrales no deben tener peso estadístico");
        }
        if (dto.evaluationDirection() != EvaluationDirection.NEUTRAL
            && (dto.evaluationWeight() == null || dto.evaluationWeight().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las preguntas evaluables necesitan peso mayor que cero");
        }
        if ((dto.questionType() == QuestionType.TEXT) && dto.evaluationDirection() != EvaluationDirection.NEUTRAL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las preguntas de texto deben ser neutrales");
        }
        if (dto.questionType() != QuestionType.SINGLE_CHOICE && dto.questionType() != QuestionType.MULTIPLE_CHOICE
            && !safeOptions(dto.options()).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Este tipo de pregunta no admite opciones");
        }
        if (dto.questionType() != QuestionType.MATRIX_SCALE && !safeRows(dto.matrixRows()).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solo las matrices admiten filas");
        }

        if (dto.questionType() == QuestionType.TEXT) {
            validateTextQuestion(dto.settings());
        } else if (dto.questionType() == QuestionType.NUMBER) {
            validateNumberQuestion(dto);
        } else if (dto.questionType() == QuestionType.SCALE) {
            validateScaleQuestion(dto, false);
        } else if (dto.questionType() == QuestionType.MATRIX_SCALE) {
            if (safeRows(dto.matrixRows()).isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las matrices necesitan al menos una fila");
            }
            validateScaleQuestion(dto, false);
        } else if (dto.questionType() == QuestionType.SINGLE_CHOICE || dto.questionType() == QuestionType.MULTIPLE_CHOICE) {
            validateChoiceQuestion(dto);
        } else if (dto.questionType() == QuestionType.BOOLEAN) {
            validateBooleanQuestion(dto);
        }
    }

    private void validatePositions(QuestionDraftDto dto) {
        if (dto.options() != null && dto.options().size() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Una pregunta no puede contener más de 50 opciones");
        }
        if (dto.matrixRows() != null && dto.matrixRows().size() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Una matriz no puede contener más de 50 filas");
        }
        if (dto.options() != null && dto.options().stream().anyMatch(Objects::isNull)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La pregunta contiene una opción nula");
        }
        if (dto.matrixRows() != null && dto.matrixRows().stream().anyMatch(Objects::isNull)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La pregunta contiene una fila de matriz nula");
        }
        Set<Integer> optionPositions = new HashSet<>();
        for (QuestionOptionDto option : safeOptions(dto.options())) {
            if (option.label() == null || option.label().isBlank() || option.position() == null || option.position() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las opciones necesitan etiqueta y posición válida");
            }
            if (!optionPositions.add(option.position())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las opciones no pueden repetir posición");
            }
        }
        for (int position = 1; position <= optionPositions.size(); position++) {
            if (!optionPositions.contains(position)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las posiciones de las opciones deben ser consecutivas");
            }
        }
        Set<Integer> rowPositions = new HashSet<>();
        for (MatrixRowDto row : safeRows(dto.matrixRows())) {
            if (row.label() == null || row.label().isBlank() || row.position() == null || row.position() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las filas de matriz necesitan etiqueta y posición válida");
            }
            if (!rowPositions.add(row.position())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las filas de matriz no pueden repetir posición");
            }
        }
        for (int position = 1; position <= rowPositions.size(); position++) {
            if (!rowPositions.contains(position)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las posiciones de las filas deben ser consecutivas");
            }
        }
    }

    private void validateCommonSettings(QuestionSettingsDto settings) {
        if (settings == null) {
            return;
        }
        if (settings.maxLength() != null && settings.maxLength() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La longitud máxima debe ser mayor que cero");
        }
        if (settings.step() != null && settings.step().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El step debe ser mayor que cero");
        }
        if ((settings.minSelections() != null && settings.minSelections() <= 0)
            || (settings.maxSelections() != null && settings.maxSelections() <= 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Los límites de selección deben ser mayores que cero");
        }
        if (settings.minSelections() != null && settings.maxSelections() != null
            && settings.minSelections() > settings.maxSelections()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El mínimo de selección no puede superar el máximo");
        }
        validateFactor(settings.trueEvaluationFactor(), "El factor de evaluación verdadero");
        validateFactor(settings.falseEvaluationFactor(), "El factor de evaluación falso");
        if ((settings.trueLabel() != null && settings.trueLabel().length() > 100)
            || (settings.falseLabel() != null && settings.falseLabel().length() > 100)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las etiquetas booleanas no pueden superar 100 caracteres");
        }
    }

    private void validateEvaluationFactors(QuestionDraftDto dto) {
        for (QuestionOptionDto option : safeOptions(dto.options())) {
            validateFactor(option.evaluationFactor(), "El factor de evaluación de una opción");
        }
    }

    private void validateFactor(BigDecimal factor, String fieldName) {
        if (factor != null && (factor.compareTo(BigDecimal.ZERO) < 0 || factor.compareTo(BigDecimal.ONE) > 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, fieldName + " debe estar entre 0 y 1");
        }
    }

    private void validateTextQuestion(QuestionSettingsDto settings) {
        if (settings == null) {
            return;
        }
        if (settings.minValue() != null || settings.maxValue() != null || settings.step() != null
            || settings.minSelections() != null || settings.maxSelections() != null
            || hasText(settings.leftLabel()) || hasText(settings.rightLabel())
            || hasText(settings.trueLabel()) || hasText(settings.falseLabel())
            || settings.trueEvaluationFactor() != null || settings.falseEvaluationFactor() != null
            || settings.scaleDisplayType() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las preguntas de texto solo admiten configuración de texto y estilo");
        }
    }

    private void validateNumberQuestion(QuestionDraftDto dto) {
        QuestionSettingsDto settings = dto.settings();
        if (settings == null) {
            if (dto.evaluationDirection() != EvaluationDirection.NEUTRAL) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las preguntas numéricas evaluables necesitan mínimo y máximo");
            }
            return;
        }
        if (settings.minSelections() != null || settings.maxSelections() != null
            || hasText(settings.leftLabel()) || hasText(settings.rightLabel())
            || hasText(settings.trueLabel()) || hasText(settings.falseLabel())
            || settings.trueEvaluationFactor() != null || settings.falseEvaluationFactor() != null
            || settings.scaleDisplayType() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La pregunta numérica contiene configuración incompatible");
        }
        validateNumericRange(settings);
        if (dto.evaluationDirection() != EvaluationDirection.NEUTRAL
            && (settings.minValue() == null || settings.maxValue() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las preguntas numéricas evaluables necesitan mínimo y máximo");
        }
    }

    private void validateScaleQuestion(QuestionDraftDto dto, boolean bipolar) {
        QuestionSettingsDto settings = dto.settings();
        if (settings == null || settings.minValue() == null || settings.maxValue() == null || settings.step() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las escalas necesitan mínimo, máximo y step");
        }
        if (settings.minSelections() != null || settings.maxSelections() != null
            || hasText(settings.trueLabel()) || hasText(settings.falseLabel())
            || settings.trueEvaluationFactor() != null || settings.falseEvaluationFactor() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La escala contiene configuración incompatible");
        }
        validateNumericRange(settings);
        BigDecimal valueCount = settings.maxValue()
            .subtract(settings.minValue())
            .divideToIntegralValue(settings.step())
            .add(BigDecimal.ONE);
        if (valueCount.compareTo(BigDecimal.valueOf(MAX_SCALE_VALUE_COUNT)) > 0) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Una escala puede contener como máximo " + MAX_SCALE_VALUE_COUNT + " valores"
            );
        }
        if (bipolar && (!hasText(settings.leftLabel()) || !hasText(settings.rightLabel()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La escala bipolar necesita etiquetas izquierda y derecha");
        }
        validateScaleDisplay(settings);
    }

    private void validateChoiceQuestion(QuestionDraftDto dto) {
        List<QuestionOptionDto> options = safeOptions(dto.options());
        if (options.size() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las preguntas de selección necesitan al menos dos opciones");
        }
        if (options.stream().filter(QuestionOptionDto::other).count() > 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solo puede existir una opción OTHER");
        }
        QuestionSettingsDto settings = dto.settings();
        if (settings != null
            && (settings.minValue() != null || settings.maxValue() != null || settings.step() != null
            || hasText(settings.leftLabel()) || hasText(settings.rightLabel())
            || hasText(settings.trueLabel()) || hasText(settings.falseLabel())
            || settings.trueEvaluationFactor() != null || settings.falseEvaluationFactor() != null
            || settings.scaleDisplayType() != null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La selección contiene configuración incompatible");
        }
        if (dto.questionType() == QuestionType.SINGLE_CHOICE
            && settings != null
            && (settings.minSelections() != null || settings.maxSelections() != null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La selección única no admite mínimo ni máximo de selección");
        }
        if (settings != null
            && settings.minSelections() != null
            && settings.maxSelections() != null
            && settings.minSelections() > settings.maxSelections()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El mínimo de selección no puede superar el máximo");
        }
        if (settings != null
            && ((settings.minSelections() != null && settings.minSelections() > options.size())
            || (settings.maxSelections() != null && settings.maxSelections() > options.size()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Los límites de selección no pueden superar el número de opciones");
        }
        if (dto.evaluationDirection() != EvaluationDirection.NEUTRAL
            && options.stream().anyMatch(option -> option.evaluationFactor() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las opciones evaluables necesitan evaluationFactor");
        }
    }

    private void validateBooleanQuestion(QuestionDraftDto dto) {
        QuestionSettingsDto settings = dto.settings();
        if (settings == null || !hasText(settings.trueLabel()) || !hasText(settings.falseLabel())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las preguntas booleanas necesitan etiquetas true y false");
        }
        if (settings.maxLength() != null || settings.minValue() != null || settings.maxValue() != null || settings.step() != null
            || settings.minSelections() != null || settings.maxSelections() != null
            || hasText(settings.leftLabel()) || hasText(settings.rightLabel())
            || settings.scaleDisplayType() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La pregunta booleana contiene configuración incompatible");
        }
        if (dto.evaluationDirection() != EvaluationDirection.NEUTRAL
            && (settings.trueEvaluationFactor() == null || settings.falseEvaluationFactor() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las preguntas booleanas evaluables necesitan factores true y false");
        }
    }

    private void validateNumericRange(QuestionSettingsDto settings) {
        if (settings.minValue() != null && settings.maxValue() != null
            && settings.minValue().compareTo(settings.maxValue()) >= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El mínimo debe ser menor que el máximo");
        }
        if (settings.step() != null && settings.step().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El step debe ser mayor que cero");
        }
    }

    private void validateScaleDisplay(QuestionSettingsDto settings) {
        if (settings.scaleDisplayType() == null) {
            return;
        }
        if (settings.scaleDisplayType().name().equals("STARS")) {
            requireIntegerScale(settings, BigDecimal.ONE, new BigDecimal("10"));
        }
        if (settings.scaleDisplayType().name().equals("FACES")) {
            requireIntegerScale(settings, BigDecimal.ONE, new BigDecimal("5"));
        }
    }

    private void requireIntegerScale(QuestionSettingsDto settings, BigDecimal minAllowed, BigDecimal maxAllowed) {
        if (settings.minValue().compareTo(minAllowed) != 0
            || settings.maxValue().compareTo(maxAllowed) > 0
            || settings.step().compareTo(BigDecimal.ONE) != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "STARS y FACES deben usar escalas enteras compatibles");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private BigDecimal weightFor(EvaluationDirection direction, BigDecimal weight) {
        return direction == EvaluationDirection.NEUTRAL ? null : weight;
    }

    private boolean isCatalogContext(ContentItem parentItem) {
        if (parentItem == null) {
            return false;
        }
        if (parentItem.getTemplate() != null) {
            return true;
        }
        if (parentItem.getSurvey() != null || parentItem.getParentBlock() == null) {
            return false;
        }
        if (parentItem.getParentBlock().getScope() == BlockScope.CATALOG) {
            return true;
        }
        ContentItem blockOwner = parentItem.getParentBlock().getParentItem();
        return blockOwner != null && blockOwner.getTemplate() != null;
    }

    private QuestionDraftDto withoutEvaluationConfiguration(QuestionDraftDto dto) {
        if (dto == null) {
            return null;
        }
        List<QuestionOptionDto> options = dto.options() == null
            ? null
            : dto.options().stream()
                .map(option -> option == null
                    ? null
                    : new QuestionOptionDto(
                        option.id(),
                        option.label(),
                        option.position(),
                        option.other(),
                        null
                    ))
                .toList();
        return new QuestionDraftDto(
            dto.statement(),
            dto.annotation(),
            dto.questionType(),
            EvaluationDirection.NEUTRAL,
            null,
            withoutEvaluationFactors(dto.settings()),
            options,
            dto.matrixRows()
        );
    }

    private QuestionSettingsDto withoutEvaluationFactors(QuestionSettingsDto settings) {
        if (settings == null) {
            return null;
        }
        return new QuestionSettingsDto(
            settings.maxLength(),
            settings.minValue(),
            settings.maxValue(),
            settings.step(),
            settings.minSelections(),
            settings.maxSelections(),
            settings.leftLabel(),
            settings.rightLabel(),
            settings.trueLabel(),
            settings.falseLabel(),
            null,
            null,
            settings.scaleDisplayType()
        );
    }

    private QuestionSettings toSettings(QuestionSettingsDto dto) {
        if (dto == null) {
            return QuestionSettings.builder().build();
        }
        return QuestionSettings.builder()
            .maxLength(dto.maxLength())
            .minValue(dto.minValue())
            .maxValue(dto.maxValue())
            .step(dto.step())
            .minSelections(dto.minSelections())
            .maxSelections(dto.maxSelections())
            .leftLabel(trimToNull(dto.leftLabel()))
            .rightLabel(trimToNull(dto.rightLabel()))
            .trueLabel(trimToNull(dto.trueLabel()))
            .falseLabel(trimToNull(dto.falseLabel()))
            .trueEvaluationFactor(dto.trueEvaluationFactor())
            .falseEvaluationFactor(dto.falseEvaluationFactor())
            .scaleDisplayType(dto.scaleDisplayType())
            .build();
    }

    private void applySettings(QuestionSettings settings, QuestionSettingsDto dto) {
        QuestionSettings source = toSettings(dto);
        settings.setMaxLength(source.getMaxLength());
        settings.setMinValue(source.getMinValue());
        settings.setMaxValue(source.getMaxValue());
        settings.setStep(source.getStep());
        settings.setMinSelections(source.getMinSelections());
        settings.setMaxSelections(source.getMaxSelections());
        settings.setLeftLabel(source.getLeftLabel());
        settings.setRightLabel(source.getRightLabel());
        settings.setTrueLabel(source.getTrueLabel());
        settings.setFalseLabel(source.getFalseLabel());
        settings.setTrueEvaluationFactor(source.getTrueEvaluationFactor());
        settings.setFalseEvaluationFactor(source.getFalseEvaluationFactor());
        settings.setScaleDisplayType(source.getScaleDisplayType());
    }

    private QuestionSettings copySettings(QuestionSettings settings, boolean includeEvaluation) {
        if (settings == null) {
            return QuestionSettings.builder().build();
        }
        return QuestionSettings.builder()
            .maxLength(settings.getMaxLength())
            .minValue(settings.getMinValue())
            .maxValue(settings.getMaxValue())
            .step(settings.getStep())
            .minSelections(settings.getMinSelections())
            .maxSelections(settings.getMaxSelections())
            .leftLabel(settings.getLeftLabel())
            .rightLabel(settings.getRightLabel())
            .trueLabel(settings.getTrueLabel())
            .falseLabel(settings.getFalseLabel())
            .trueEvaluationFactor(includeEvaluation ? settings.getTrueEvaluationFactor() : null)
            .falseEvaluationFactor(includeEvaluation ? settings.getFalseEvaluationFactor() : null)
            .scaleDisplayType(settings.getScaleDisplayType())
            .build();
    }

    private QuestionOption toOption(QuestionOptionDto dto) {
        return QuestionOption.builder()
            .label(dto.label().trim())
            .position(dto.position())
            .other(dto.other())
            .evaluationFactor(dto.evaluationFactor())
            .build();
    }

    private QuestionOption copyOption(QuestionOption option, boolean includeEvaluation) {
        return QuestionOption.builder()
            .label(option.getLabel())
            .position(option.getPosition())
            .other(option.isOther())
            .evaluationFactor(includeEvaluation ? option.getEvaluationFactor() : null)
            .build();
    }

    private MatrixRow toRow(MatrixRowDto dto) {
        return MatrixRow.builder()
            .label(dto.label().trim())
            .position(dto.position())
            .build();
    }

    private MatrixRow copyRow(MatrixRow row) {
        return MatrixRow.builder()
            .label(row.getLabel())
            .position(row.getPosition())
            .build();
    }

    public QuestionResponseDto toResponse(Question question) {
        QuestionSettings settings = question.getSettings();
        User creator = question.getCreatedBy();
        return new QuestionResponseDto(
            question.getId(),
            question.getScope(),
            question.getStatement(),
            question.getAnnotation(),
            question.getQuestionType(),
            question.getEvaluationDirection(),
            question.getEvaluationWeight(),
            settings == null ? null : new QuestionSettingsDto(
                settings.getMaxLength(),
                settings.getMinValue(),
                settings.getMaxValue(),
                settings.getStep(),
                settings.getMinSelections(),
                settings.getMaxSelections(),
                settings.getLeftLabel(),
                settings.getRightLabel(),
                settings.getTrueLabel(),
                settings.getFalseLabel(),
                settings.getTrueEvaluationFactor(),
                settings.getFalseEvaluationFactor(),
                settings.getScaleDisplayType()
            ),
            question.getOptions().stream()
                .sorted(Comparator.comparing(QuestionOption::getPosition))
                .map(option -> new QuestionOptionDto(
                    option.getId(),
                    option.getLabel(),
                    option.getPosition(),
                    option.isOther(),
                    option.getEvaluationFactor()
                ))
                .toList(),
            question.getMatrixRows().stream()
                .sorted(Comparator.comparing(MatrixRow::getPosition))
                .map(row -> new MatrixRowDto(row.getId(), row.getLabel(), row.getPosition()))
                .toList(),
            creator == null ? null : creator.getId(),
            creator == null ? null : creator.getName(),
            question.getCreatedAt(),
            question.getUpdatedAt(),
            question.getCategory()
        );
    }

    private Category categoryFrom(ContentItem parentItem) {
        if (parentItem == null) return Category.OTROS;
        if (parentItem.getParentBlock() != null) return parentItem.getParentBlock().getCategory();
        if (parentItem.getTemplate() != null) return parentItem.getTemplate().getCategory();
        if (parentItem.getSurvey() != null) return parentItem.getSurvey().getCategory();
        return Category.OTROS;
    }

    private List<QuestionOptionDto> safeOptions(List<QuestionOptionDto> options) {
        return options == null ? List.of() : options.stream().filter(Objects::nonNull).toList();
    }

    private List<MatrixRowDto> safeRows(List<MatrixRowDto> rows) {
        return rows == null ? List.of() : rows.stream().filter(Objects::nonNull).toList();
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
}
