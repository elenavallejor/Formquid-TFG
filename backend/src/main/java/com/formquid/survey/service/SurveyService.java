package com.formquid.survey.service;

import com.formquid.collaboration.entity.SurveyCollaboration;
import com.formquid.collaboration.entity.SurveyCollaborationStatus;
import com.formquid.collaboration.repository.SurveyCollaborationRepository;
import com.formquid.question.dto.MatrixRowResponseDto;
import com.formquid.question.dto.OptionConfigResponseDto;
import com.formquid.question.entity.MatrixRowConfig;
import com.formquid.question.entity.OptionConfig;
import com.formquid.survey.dto.ChangeSurveyStatusRequestDto;
import com.formquid.survey.dto.CreateSurveyFromTemplateRequestDto;
import com.formquid.survey.dto.CreateSurveyRequestDto;
import com.formquid.survey.dto.SurveyBlockResponseDto;
import com.formquid.survey.dto.SurveyQuestionResponseDto;
import com.formquid.survey.dto.SurveyResponseDto;
import com.formquid.survey.dto.SurveySummaryResponseDto;
import com.formquid.survey.dto.UpdateSurveyRequestDto;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyStatus;
import com.formquid.survey.model.SurveyBlockSnapshot;
import com.formquid.survey.model.SurveyQuestionSnapshot;
import com.formquid.survey.model.SurveyStructure;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.template.entity.SurveyTemplate;
import com.formquid.template.entity.TemplateStatus;
import com.formquid.template.repository.SurveyTemplateRepository;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio principal de gestión autenticada de encuestas.
 *
 * La encuesta almacena su estructura como snapshot JSONB. De esta forma se
 * reducen tablas intermedias y cada encuesta conserva la copia exacta de los
 * bloques y preguntas que tenía al crearse o editarse en borrador.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyService {

    private static final int MAX_TAGS = 12;

    private final SurveyRepository surveyRepository;
    private final SurveyTemplateRepository surveyTemplateRepository;
    private final SurveyCollaborationRepository surveyCollaborationRepository;
    private final SurveyStructureFactory surveyStructureFactory;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crea una encuesta desde cero en estado borrador.
     */
    @Transactional
    public SurveyResponseDto create(
        CreateSurveyRequestDto request,
        String ownerEmail
    ) {
        User owner = getAuthenticatedUser(ownerEmail);

        Survey survey = Survey.builder()
            .publicCode(generatePublicCode())
            .title(request.title().trim())
            .description(cleanOptional(request.description()))
            .imageUrl(cleanOptional(request.imageUrl()))
            .tagNames(normalizeTags(request.tagNames()))
            .structure(surveyStructureFactory.buildSurveyStructure(request.blocks(), true))
            .status(SurveyStatus.DRAFT)
            .owner(owner)
            .build();

        return toResponse(surveyRepository.save(survey));
    }

    /**
     * Crea una encuesta copiando el snapshot de una plantilla activa.
     */
    @Transactional
    public SurveyResponseDto createFromTemplate(
        CreateSurveyFromTemplateRequestDto request,
        String ownerEmail
    ) {
        User owner = getAuthenticatedUser(ownerEmail);
        SurveyTemplate template = getActiveTemplate(request.templateId());

        if (!template.hasQuestions()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La plantilla no contiene preguntas"
            );
        }

        Survey survey = Survey.builder()
            .publicCode(generatePublicCode())
            .title(request.title().trim())
            .description(cleanOptional(request.description()))
            .imageUrl(cleanOptional(request.imageUrl()))
            .tagNames(normalizeTags(request.tagNames()))
            .structure(surveyStructureFactory.copyStructure(template.getTemplateStructure()))
            .status(SurveyStatus.DRAFT)
            .owner(owner)
            .build();

        return toResponse(surveyRepository.save(survey));
    }

    /**
     * Lista las encuestas creadas por el usuario autenticado.
     */
    public List<SurveyResponseDto> findMine(String ownerEmail) {
        User owner = getAuthenticatedUser(ownerEmail);

        return surveyRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Lista las encuestas en las que el usuario es colaborador aceptado.
     */
    public List<SurveySummaryResponseDto> findCollaboratingSurveys(
        String editorEmail
    ) {
        User editor = getAuthenticatedUser(editorEmail);

        return surveyCollaborationRepository
            .findByEditorIdAndStatusOrderByCreatedAtDesc(
                editor.getId(),
                SurveyCollaborationStatus.ACCEPTED
            )
            .stream()
            .map(SurveyCollaboration::getSurvey)
            .map(this::toSummary)
            .toList();
    }

    /**
     * Lista todas las encuestas del sistema para administración.
     */
    public List<SurveySummaryResponseDto> findAllForAdmin(SurveyStatus status) {
        List<Survey> surveys = status == null
            ? surveyRepository.findAllByOrderByCreatedAtDesc()
            : surveyRepository.findByStatusOrderByCreatedAtDesc(status);

        return surveys.stream()
            .map(this::toSummary)
            .toList();
    }

    /**
     * Cuenta encuestas del sistema.
     */
    public long count(SurveyStatus status) {
        return status == null
            ? surveyRepository.count()
            : surveyRepository.countByStatus(status);
    }

    /**
     * Obtiene una encuesta aplicando permisos internos.
     */
    public SurveyResponseDto findById(
        Long surveyId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        assertCanViewSurvey(survey, user.getId(), user.getRole());

        return toResponse(survey);
    }

    /**
     * Actualiza datos y estructura de una encuesta en borrador.
     */
    @Transactional
    public SurveyResponseDto update(
        Long surveyId,
        UpdateSurveyRequestDto request,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        assertCanEditSurvey(survey, user.getId(), user.getRole());
        validateDraft(survey);

        survey.setTitle(request.title().trim());
        survey.setDescription(cleanOptional(request.description()));
        survey.setImageUrl(cleanOptional(request.imageUrl()));
        survey.setTagNames(normalizeTags(request.tagNames()));

        if (request.blocks() != null) {
            survey.setStructure(
                surveyStructureFactory.buildSurveyStructure(request.blocks(), true)
            );
        }

        return toResponse(survey);
    }

    /**
     * Cambia estado, visibilidad y contraseña de acceso si procede.
     */
    @Transactional
    public SurveyResponseDto changeStatus(
        Long surveyId,
        ChangeSurveyStatusRequestDto request,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        assertCanManageSurvey(survey, user.getId(), user.getRole());

        SurveyStatus newStatus = request.status();

        if (newStatus == SurveyStatus.PUBLIC || newStatus == SurveyStatus.PRIVATE) {
            validateReadyToPublish(survey);
        }

        applyStatus(survey, newStatus, request.accessPassword());

        return toResponse(survey);
    }

    /**
     * Comprueba si el usuario puede consultar una encuesta.
     */
    public void assertCanViewSurvey(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        validateAuthenticatedUser(userId, role);

        if (!canViewSurvey(survey, userId, role)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permisos para consultar esta encuesta"
            );
        }
    }

    /**
     * Comprueba si el usuario puede modificar una encuesta.
     */
    public void assertCanEditSurvey(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        assertCanViewSurvey(survey, userId, role);
    }

    /**
     * Comprueba si el usuario puede gestionar estado y colaboradores.
     */
    public void assertCanManageSurvey(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        validateAuthenticatedUser(userId, role);

        if (!canManageSurvey(survey, userId, role)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permisos para gestionar esta encuesta"
            );
        }
    }

    /**
     * Comprueba si el usuario puede consultar resultados.
     */
    public void assertCanViewResults(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        assertCanViewSurvey(survey, userId, role);
    }

    public boolean canViewSurvey(Survey survey, Long userId, UserRole role) {
        if (!isValidAccessCheck(survey, userId, role)) {
            return false;
        }

        return isPlatformManager(role)
            || isOwner(survey, userId)
            || isAcceptedCollaborator(survey.getId(), userId);
    }

    public boolean canManageSurvey(Survey survey, Long userId, UserRole role) {
        if (!isValidAccessCheck(survey, userId, role)) {
            return false;
        }

        return isPlatformManager(role) || isOwner(survey, userId);
    }

    public boolean isPlatformManager(UserRole role) {
        return role == UserRole.ADMIN || role == UserRole.SUPERADMIN;
    }

    private User getAuthenticatedUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return userRepository.findByEmailIgnoreCase(email.trim())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario autenticado no encontrado"
            ));
    }

    private Survey getSurvey(Long surveyId) {
        return surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la encuesta indicada"
            ));
    }

    private SurveyTemplate getActiveTemplate(Long templateId) {
        SurveyTemplate template = surveyTemplateRepository.findById(templateId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la plantilla indicada"
            ));

        if (template.getStatus() != TemplateStatus.ACTIVE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se pueden usar plantillas activas"
            );
        }

        return template;
    }

    private void validateDraft(Survey survey) {
        if (!survey.isEditable()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se pueden modificar encuestas en borrador"
            );
        }
    }

    private void validateReadyToPublish(Survey survey) {
        if (!survey.hasQuestions()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La encuesta debe contener al menos una pregunta antes de publicarse"
            );
        }
    }

    private void applyStatus(
        Survey survey,
        SurveyStatus newStatus,
        String accessPassword
    ) {
        if (newStatus == SurveyStatus.PRIVATE) {
            applyPrivateStatus(survey, accessPassword);
        } else {
            survey.setAccessPasswordHash(null);
        }

        survey.setStatus(newStatus);

        if ((newStatus == SurveyStatus.PUBLIC || newStatus == SurveyStatus.PRIVATE)
            && survey.getPublishedAt() == null) {
            survey.setPublishedAt(LocalDateTime.now());
        }

        if (newStatus == SurveyStatus.DRAFT) {
            survey.setPublishedAt(null);
        }
    }

    private void applyPrivateStatus(
        Survey survey,
        String accessPassword
    ) {
        if (StringUtils.hasText(accessPassword)) {
            survey.setAccessPasswordHash(
                passwordEncoder.encode(accessPassword.trim())
            );
            return;
        }

        if (!StringUtils.hasText(survey.getAccessPasswordHash())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las encuestas privadas requieren contraseña de acceso"
            );
        }
    }

    private boolean isOwner(Survey survey, Long userId) {
        return survey.getOwner() != null
            && survey.getOwner().getId() != null
            && survey.getOwner().getId().equals(userId);
    }

    private boolean isAcceptedCollaborator(Long surveyId, Long userId) {
        if (surveyId == null || userId == null) {
            return false;
        }

        return surveyCollaborationRepository.existsBySurveyIdAndEditorIdAndStatus(
            surveyId,
            userId,
            SurveyCollaborationStatus.ACCEPTED
        );
    }

    private void validateAuthenticatedUser(Long userId, UserRole role) {
        if (userId == null || role == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }
    }

    private boolean isValidAccessCheck(Survey survey, Long userId, UserRole role) {
        return survey != null && userId != null && role != null;
    }

    private List<String> normalizeTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> normalizedTags = new LinkedHashSet<>();

        for (String tagName : tagNames) {
            if (!StringUtils.hasText(tagName)) {
                continue;
            }

            String cleanTag = tagName.trim();

            if (cleanTag.length() > 60) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Las etiquetas no pueden superar los 60 caracteres"
                );
            }

            normalizedTags.add(cleanTag);
        }

        if (normalizedTags.size() > MAX_TAGS) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Una encuesta no puede tener más de " + MAX_TAGS + " etiquetas"
            );
        }

        return new ArrayList<>(normalizedTags);
    }

    private SurveyResponseDto toResponse(Survey survey) {
        return new SurveyResponseDto(
            survey.getId(),
            survey.getPublicCode(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getImageUrl(),
            survey.getStatus(),
            survey.getOwner().getId(),
            survey.getOwner().getName(),
            sortedTags(survey.getTagNames()),
            toBlockResponses(survey.getStructure()),
            survey.getCreatedAt(),
            survey.getUpdatedAt(),
            survey.getPublishedAt()
        );
    }

    private SurveySummaryResponseDto toSummary(Survey survey) {
        return new SurveySummaryResponseDto(
            survey.getId(),
            survey.getPublicCode(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getImageUrl(),
            survey.getStatus(),
            survey.getOwner().getId(),
            survey.getOwner().getName(),
            sortedTags(survey.getTagNames()),
            survey.getCreatedAt(),
            survey.getUpdatedAt(),
            survey.getPublishedAt()
        );
    }

    private List<SurveyBlockResponseDto> toBlockResponses(SurveyStructure structure) {
        if (structure == null || structure.getBlocks() == null) {
            return List.of();
        }

        return structure.getBlocks()
            .stream()
            .sorted(Comparator.comparing(SurveyBlockSnapshot::getPosition))
            .map(this::toBlockResponse)
            .toList();
    }

    private SurveyBlockResponseDto toBlockResponse(SurveyBlockSnapshot block) {
        return new SurveyBlockResponseDto(
            block.getBlockKey(),
            block.getSourceBlockId(),
            block.getTitle(),
            block.getPosition(),
            toQuestionResponses(block.getQuestions())
        );
    }

    private List<SurveyQuestionResponseDto> toQuestionResponses(
        List<SurveyQuestionSnapshot> questions
    ) {
        if (questions == null) {
            return List.of();
        }

        return questions.stream()
            .sorted(Comparator.comparing(SurveyQuestionSnapshot::getPosition))
            .map(this::toQuestionResponse)
            .toList();
    }

    private SurveyQuestionResponseDto toQuestionResponse(SurveyQuestionSnapshot question) {
        return new SurveyQuestionResponseDto(
            question.getQuestionKey(),
            question.getSource(),
            question.getSourceQuestionId(),
            question.getText(),
            question.getDescription(),
            question.getType(),
            question.getEvaluationDirection(),
            question.isRequired(),
            question.getMinValue(),
            question.getMaxValue(),
            question.getScaleDisplayType(),
            question.getLeftLabel(),
            question.getRightLabel(),
            toOptionResponses(question.getOptions()),
            toMatrixRowResponses(question.getMatrixRows()),
            question.getPosition()
        );
    }

    private List<OptionConfigResponseDto> toOptionResponses(List<OptionConfig> options) {
        if (options == null) {
            return List.of();
        }

        return options.stream()
            .map(option -> new OptionConfigResponseDto(
                option.getLabel(),
                option.getStoredValue(),
                option.getScoreValue(),
                option.isNonResponseOption(),
                option.isAllowsFreeText()
            ))
            .toList();
    }

    private List<MatrixRowResponseDto> toMatrixRowResponses(List<MatrixRowConfig> rows) {
        if (rows == null) {
            return List.of();
        }

        return rows.stream()
            .map(row -> new MatrixRowResponseDto(row.getKey(), row.getLabel()))
            .toList();
    }

    private List<String> sortedTags(List<String> tagNames) {
        if (tagNames == null) {
            return List.of();
        }

        return tagNames.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private String cleanOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generatePublicCode() {
        String code;

        do {
            code = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toLowerCase(Locale.ROOT);
        } while (surveyRepository.findPublicStructureByPublicCode(code).isPresent());

        return code;
    }
}
