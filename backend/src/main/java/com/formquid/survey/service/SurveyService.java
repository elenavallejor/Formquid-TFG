package com.formquid.survey.service;

import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.collaboration.repository.CollaborationRequestRepository;
import com.formquid.collaboration.entity.CollaborationPermission;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.common.exception.ApiException;
import com.formquid.common.service.TagNormalizer;
import com.formquid.content.dto.ContentItemResponseDto;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.content.service.ContentService;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.service.NotificationService;
import com.formquid.participation.repository.ParticipationRepository;
import com.formquid.survey.dto.AdminSurveySummaryDto;
import com.formquid.survey.dto.CreateSurveyRequestDto;
import com.formquid.survey.dto.PublicSurveySummaryDto;
import com.formquid.survey.dto.SurveyPageResponseDto;
import com.formquid.survey.dto.SurveyDirectorySummaryDto;
import com.formquid.survey.dto.SurveyResponseDto;
import com.formquid.survey.dto.SurveySummaryDto;
import com.formquid.survey.dto.UpdateSurveyRequestDto;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyFontFamily;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.template.entity.Template;
import com.formquid.template.service.TemplateService;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Gestiona la creación, consulta, permisos y ciclo de vida de las encuestas.
 */
@Service
@RequiredArgsConstructor
public class SurveyService {

    private static final String PUBLIC_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int PUBLIC_CODE_LENGTH = 12;
    private static final int PUBLIC_CODE_ATTEMPTS = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final SurveyAccess NO_ACCESS = new SurveyAccess(false, false);
    private static final SurveyAccess OWNER_ACCESS = new SurveyAccess(true, true);

    private final SurveyRepository surveyRepository;
    private final SurveyCollaboratorRepository collaboratorRepository;
    private final CollaborationRequestRepository collaborationRequestRepository;
    private final ContentItemRepository contentItemRepository;
    private final ParticipationRepository participationRepository;
    private final ContentService contentService;
    private final TemplateService templateService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public SurveyPageResponseDto findEditableByUser(User user, String search, SurveyLifecycle lifecycleStatus, Pageable pageable) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paginación de encuestas no válida");
        }
        Sort.Order updatedAtOrder = pageable.getSort().getOrderFor("updatedAt");
        if (updatedAtOrder == null || pageable.getSort().stream().count() != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ordenación de encuestas no válida");
        }
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        PageRequest safePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(updatedAtOrder.getDirection(), "updatedAt"));
        Page<Survey> surveys = surveyRepository.findEditableByUser(user.getId(), normalizedSearch, lifecycleStatus, safePageable);
        return new SurveyPageResponseDto(
            surveys.getContent().stream().map(this::toSummary).toList(),
            surveys.getNumber(),
            surveys.getSize(),
            surveys.getTotalElements(),
            surveys.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDto<SurveyDirectorySummaryDto> findDirectory(
        User user,
        String search,
        SurveyLifecycle lifecycleStatus,
        Pageable pageable
    ) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        if (user.getRole() != UserRole.EDITOR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo los editores pueden consultar las encuestas disponibles");
        }
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paginación de encuestas no válida");
        }

        Sort.Order updatedAtOrder = pageable.getSort().getOrderFor("updatedAt");
        if (updatedAtOrder == null || pageable.getSort().stream().count() != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ordenación de encuestas no válida");
        }

        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        PageRequest safePageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(updatedAtOrder.getDirection(), "updatedAt")
        );
        Page<Survey> surveys = surveyRepository.findDirectory(normalizedSearch, lifecycleStatus, safePageable);
        List<Long> surveyIds = surveys.getContent().stream().map(Survey::getId).toList();
        Set<Long> collaboratingIds = surveyIds.isEmpty()
            ? Set.of()
            : new HashSet<>(collaboratorRepository.findSurveyIdsByUserIdAndSurveyIds(user.getId(), surveyIds));
        Set<Long> pendingRequestIds = surveyIds.isEmpty()
            ? Set.of()
            : new HashSet<>(collaborationRequestRepository.findSurveyIdsByUserIdAndSurveyIds(user.getId(), surveyIds));

        List<SurveyDirectorySummaryDto> content = surveys.getContent().stream()
            .map(survey -> new SurveyDirectorySummaryDto(
                survey.getId(),
                survey.getTitle(),
                survey.getImageKey(),
                survey.getOwner().getName(),
                survey.getLifecycleStatus(),
                tagsFrom(survey),
                survey.getUpdatedAt(),
                survey.getOwner().getId().equals(user.getId()),
                collaboratingIds.contains(survey.getId()),
                pendingRequestIds.contains(survey.getId())
            ))
            .toList();

        return new PageResponseDto<>(
            content,
            surveys.getNumber(),
            surveys.getSize(),
            surveys.getTotalElements(),
            surveys.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDto<AdminSurveySummaryDto> findAdminSummaries(
        String search,
        SurveyLifecycle lifecycleStatus,
        SurveyVisibility visibility,
        int page,
        int size
    ) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paginación de encuestas no válida");
        }

        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Page<AdminSurveySummaryDto> surveys = surveyRepository.findAdminSummaries(
            normalizedSearch,
            lifecycleStatus,
            visibility,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );

        return new PageResponseDto<>(
            surveys.getContent(),
            surveys.getNumber(),
            surveys.getSize(),
            surveys.getTotalElements(),
            surveys.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<PublicSurveySummaryDto> findListedOpen(List<String> tags) {
        List<String> normalizedTags = normalizeTags(tags);
        return surveyRepository.findByLifecycleStatusAndVisibilityOrderByCreatedAtDesc(SurveyLifecycle.OPEN, SurveyVisibility.LISTED).stream()
            .filter(survey -> survey.getAccessMode() != SurveyAccessMode.AUTHENTICATED)
            .filter(survey -> matchesTags(survey, normalizedTags))
            .map(this::toPublicSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicSurveySummaryDto> findListedOpenForRegistered(User user, List<String> tags) {
        List<String> normalizedTags = normalizeTags(tags);
        return surveyRepository.findByLifecycleStatusAndVisibilityOrderByCreatedAtDesc(SurveyLifecycle.OPEN, SurveyVisibility.LISTED).stream()
            .filter(survey -> canViewOpenSurvey(survey, user))
            .filter(survey -> matchesTags(survey, normalizedTags))
            .map(this::toPublicSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<PublicSurveySummaryDto> findListedOpenPage(
        String search,
        String tag,
        SurveyAccessMode accessMode,
        int page,
        int size
    ) {
        return findListedOpenPage(search, tag, accessMode, page, size, false);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<PublicSurveySummaryDto> findListedOpenForRegisteredPage(
        User user,
        String search,
        String tag,
        SurveyAccessMode accessMode,
        int page,
        int size
    ) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return findListedOpenPage(search, tag, accessMode, page, size, true);
    }

    private PageResponseDto<PublicSurveySummaryDto> findListedOpenPage(
        String search,
        String tag,
        SurveyAccessMode accessMode,
        int page,
        int size,
        boolean includeAuthenticated
    ) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paginación de encuestas no válida");
        }

        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String normalizedTag = tag == null ? "" : Normalizer.normalize(
            tag.trim().toLowerCase(Locale.ROOT).replaceFirst("^#", ""),
            Normalizer.Form.NFKC
        ).replaceAll("\\s+", "-");
        Page<Survey> surveys;
        if (normalizedTag.isEmpty()) {
            surveys = surveyRepository.findListedOpenPage(
                SurveyLifecycle.OPEN,
                SurveyVisibility.LISTED,
                includeAuthenticated,
                SurveyAccessMode.AUTHENTICATED,
                accessMode,
                normalizedSearch,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        } else {
            surveys = surveyRepository.findListedOpenPageByTag(
                includeAuthenticated,
                accessMode == null ? "" : accessMode.name(),
                normalizedSearch,
                normalizedTag,
                PageRequest.of(page, size)
            );
        }

        return new PageResponseDto<>(
            surveys.getContent().stream().map(this::toPublicSummary).toList(),
            surveys.getNumber(),
            surveys.getSize(),
            surveys.getTotalElements(),
            surveys.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public SurveyResponseDto getSurveyForUser(Long surveyId, User user) {
        Survey survey = getSurvey(surveyId);
        SurveyAccess access = resolveAccess(survey, user);
        ensureCanViewSurvey(survey, user, access);
        return toResponse(survey, access);
    }

    @Transactional(readOnly = true)
    public SurveyResponseDto getPublicSurvey(String publicCode) {
        Survey survey = surveyRepository.findByPublicCode(normalizePublicCode(publicCode))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
        if (!canViewPublicSurvey(survey)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Encuesta no disponible públicamente");
        }
        if (survey.getAccessMode() == SurveyAccessMode.PASSWORD_PROTECTED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta encuesta requiere contraseña");
        }
        return toResponse(survey);
    }

    @Transactional(readOnly = true)
    public SurveyResponseDto getOpenSurveyForRespondent(String publicCode, User user) {
        Survey survey = surveyRepository.findByPublicCode(normalizePublicCode(publicCode))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
        if (!canViewOpenSurvey(survey, user)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Encuesta no disponible");
        }
        if (survey.getAccessMode() == SurveyAccessMode.PASSWORD_PROTECTED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta encuesta requiere contraseña");
        }
        return toResponse(survey);
    }

    @Transactional(readOnly = true)
    public SurveyResponseDto getPasswordProtectedPublicSurvey(String publicCode, String accessPassword) {
        Survey survey = surveyRepository.findByPublicCode(normalizePublicCode(publicCode))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
        if (!canViewPublicSurvey(survey)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Encuesta no disponible públicamente");
        }
        if (survey.getAccessMode() != SurveyAccessMode.PASSWORD_PROTECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Esta encuesta no requiere contraseña");
        }
        if (accessPassword == null || !passwordEncoder.matches(accessPassword, survey.getAccessPasswordHash())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "La contraseña no es correcta");
        }
        return toResponse(survey);
    }

    public Survey getSurvey(Long surveyId) {
        return surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
    }

    @Transactional
    public SurveyResponseDto create(CreateSurveyRequestDto request, User owner) {
        Survey saved = createDraft(request, owner);
        return toResponse(saved, owner);
    }

    @Transactional
    public SurveyResponseDto createFromTemplate(Long templateId, CreateSurveyRequestDto request, User owner) {
        Template template = templateService.getTemplate(templateId);
        Survey saved = createDraft(request, owner);
        templateService.copyTemplateIntoSurvey(template, saved, owner);
        return toResponse(saved, owner);
    }

    @Transactional
    public SurveyResponseDto update(Long surveyId, UpdateSurveyRequestDto request, User actor) {
        Survey survey = getSurvey(surveyId);
        SurveyAccess access = resolveAccess(survey, actor);
        ensureCanEdit(access);
        survey.setTitle(request.title().trim());
        survey.setDescription(trimToNull(request.description()));
        survey.setImageKey(trimToNull(request.imageKey()));
        survey.setFontFamily(fontFamilyOrDefault(request.fontFamily()));
        saveTags(survey, request.tags());
        if (isOwner(survey, actor)) {
            SurveyAccessMode previousMode = survey.getAccessMode();
            SurveyAccessMode requestedMode = request.accessMode() == null ? SurveyAccessMode.PUBLIC : request.accessMode();
            survey.setAccessMode(requestedMode);
            survey.setVisibility(survey.getLifecycleStatus() == SurveyLifecycle.DRAFT
                ? SurveyVisibility.UNLISTED
                : request.visibility() == null ? SurveyVisibility.LISTED : request.visibility());
            survey.setAccessPasswordHash(updatedPasswordHash(previousMode, requestedMode, survey.getAccessPasswordHash(), request.accessPassword()));
        }
        return toResponse(survey, access);
    }

    @Transactional
    public SurveyResponseDto changeLifecycle(Long surveyId, SurveyLifecycle lifecycle, User actor) {
        Survey survey = getSurvey(surveyId);
        ensureOwner(survey, actor);
        prepareLifecycleChange(survey, lifecycle);
        notifySurveyStatusChanged(
            survey,
            "Estado de encuesta actualizado",
            "La encuesta " + survey.getTitle() + " ha pasado a estado " + lifecycle.name()
        );
        return toResponse(survey, actor);
    }

    /**
     * Aplica la misma transición a OPEN para una solicitud de publicación ya autorizada.
     */
    @Transactional
    public void openAfterPublicationApproval(Survey survey) {
        prepareLifecycleChange(survey, SurveyLifecycle.OPEN);
    }

    @Transactional
    public void delete(Long surveyId, User actor) {
        Survey survey = getSurvey(surveyId);
        ensureOwner(survey, actor);
        ensureClosedForDelete(survey);
        surveyRepository.delete(survey);
    }

    @Transactional
    public SurveyResponseDto adminChangeLifecycle(Long surveyId, SurveyLifecycle lifecycle, User admin) {
        if (!isAdmin(admin)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administración puede intervenir encuestas");
        }
        Survey survey = getSurvey(surveyId);
        prepareLifecycleChange(survey, lifecycle);
        notifySurveyStatusChanged(
            survey,
            "Intervención administrativa sobre encuesta",
            "Administración ha cambiado la encuesta " + survey.getTitle() + " a estado " + lifecycle.name()
        );
        return toResponse(survey, admin);
    }

    @Transactional
    public SurveyResponseDto adminChangeVisibility(Long surveyId, SurveyVisibility visibility, User admin) {
        if (!isAdmin(admin)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administración puede intervenir encuestas");
        }
        if (visibility != SurveyVisibility.UNLISTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Administración solo puede ocultar encuestas");
        }
        Survey survey = getSurvey(surveyId);
        survey.setVisibility(SurveyVisibility.UNLISTED);
        notifySurveyStatusChanged(
            survey,
            "Encuesta ocultada por administración",
            "Administración ha ocultado la encuesta " + survey.getTitle() + " de los listados públicos"
        );
        return toResponse(survey, admin);
    }

    @Transactional
    public void adminDelete(Long surveyId, User admin) {
        if (!isAdmin(admin)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administración puede eliminar encuestas");
        }
        Survey survey = getSurvey(surveyId);
        ensureClosedForDelete(survey);
        surveyRepository.delete(survey);
    }

    public void ensureCanViewSurvey(Survey survey, User user) {
        ensureCanViewSurvey(survey, user, resolveAccess(survey, user));
    }

    private void ensureCanViewSurvey(Survey survey, User user, SurveyAccess access) {
        if (access.canPreview() || isAdmin(user) || canViewOpenSurvey(survey, user)) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "No puedes consultar esta encuesta");
    }

    public void ensureCanViewResults(Survey survey, User user) {
        if (resolveAccess(survey, user).canPreview() || isAdmin(user)) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "No puedes consultar las estadísticas de esta encuesta");
    }

    public boolean canEditSurvey(Survey survey, User user) {
        return resolveAccess(survey, user).canEdit();
    }

    public boolean canPreviewSurvey(Survey survey, User user) {
        return resolveAccess(survey, user).canPreview();
    }

    public void ensureStructureEditable(Survey survey) {
        if (participationRepository.existsBySurveyId(survey.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "No puede modificarse la estructura de una encuesta con respuestas");
        }
    }

    public SurveyResponseDto toResponse(Survey survey) {
        return toResponse(survey, NO_ACCESS);
    }

    private SurveyResponseDto toResponse(Survey survey, User actor) {
        return toResponse(survey, resolveAccess(survey, actor));
    }

    private SurveyResponseDto toResponse(Survey survey, SurveyAccess access) {
        List<ContentItemResponseDto> content = contentService.findSurveyContent(survey.getId()).stream()
            .sorted(Comparator.comparing(ContentItemResponseDto::position))
            .toList();
        return new SurveyResponseDto(
            survey.getId(),
            survey.getPublicCode(),
            survey.getOwner().getId(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getImageKey(),
            survey.getFontFamily(),
            survey.getLifecycleStatus(),
            survey.getAccessMode(),
            survey.getVisibility(),
            tagsFrom(survey),
            content,
            access.canEdit(),
            access.canPreview(),
            survey.getCreatedAt(),
            survey.getUpdatedAt()
        );
    }

    private Survey createDraft(CreateSurveyRequestDto request, User owner) {
        Survey survey = Survey.builder()
            .owner(owner)
            .title(request.title().trim())
            .description(trimToNull(request.description()))
            .imageKey(trimToNull(request.imageKey()))
            .fontFamily(fontFamilyOrDefault(request.fontFamily()))
            .lifecycleStatus(SurveyLifecycle.DRAFT)
            .accessMode(request.accessMode() == null ? SurveyAccessMode.PUBLIC : request.accessMode())
            .visibility(SurveyVisibility.UNLISTED)
            .accessPasswordHash(passwordHash(request.accessMode(), request.accessPassword()))
            .build();
        saveTags(survey, request.tags());
        return surveyRepository.save(survey);
    }

    private void prepareLifecycleChange(Survey survey, SurveyLifecycle lifecycle) {
        if (lifecycle == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El estado de la encuesta es obligatorio");
        }
        SurveyLifecycle currentLifecycle = survey.getLifecycleStatus();
        if (currentLifecycle == SurveyLifecycle.DRAFT && lifecycle == SurveyLifecycle.CLOSED) {
            throw new ApiException(HttpStatus.CONFLICT, "Una encuesta en borrador debe publicarse antes de poder cerrarse");
        }
        if (currentLifecycle != SurveyLifecycle.DRAFT && lifecycle == SurveyLifecycle.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "Una encuesta publicada no puede volver al estado de borrador");
        }
        if (lifecycle == SurveyLifecycle.OPEN) {
            if (!contentService.hasQuestion(contentItemRepository.findBySurveyIdOrderByPositionAsc(survey.getId()))) {
                throw new ApiException(HttpStatus.CONFLICT, "La encuesta no puede abrirse sin preguntas");
            }
            if (survey.getPublicCode() == null) {
                survey.setPublicCode(generatePublicCode());
            }
            if (currentLifecycle == SurveyLifecycle.DRAFT) {
                survey.setVisibility(SurveyVisibility.LISTED);
            }
        }
        survey.setLifecycleStatus(lifecycle);
    }

    private String generatePublicCode() {
        for (int attempt = 0; attempt < PUBLIC_CODE_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(PUBLIC_CODE_LENGTH);
            for (int index = 0; index < PUBLIC_CODE_LENGTH; index++) {
                code.append(PUBLIC_CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(PUBLIC_CODE_ALPHABET.length())));
            }
            String candidate = code.toString();
            if (!surveyRepository.existsByPublicCode(candidate)) {
                return candidate;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el código público de la encuesta");
    }

    private String normalizePublicCode(String publicCode) {
        if (publicCode == null || publicCode.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada");
        }
        return publicCode.trim().toUpperCase(Locale.ROOT);
    }

    private SurveyFontFamily fontFamilyOrDefault(SurveyFontFamily fontFamily) {
        return fontFamily == null ? SurveyFontFamily.TIMES_NEW_ROMAN : fontFamily;
    }

    private boolean canViewPublicSurvey(Survey survey) {
        return survey.getLifecycleStatus() == SurveyLifecycle.OPEN && survey.getAccessMode() != SurveyAccessMode.AUTHENTICATED;
    }

    private boolean canViewOpenSurvey(Survey survey, User user) {
        return survey.getLifecycleStatus() == SurveyLifecycle.OPEN
            && (survey.getAccessMode() != SurveyAccessMode.AUTHENTICATED || user != null);
    }

    private SurveySummaryDto toSummary(Survey survey) {
        return new SurveySummaryDto(
            survey.getId(),
            survey.getTitle(),
            survey.getImageKey(),
            survey.getLifecycleStatus(),
            tagsFrom(survey),
            survey.getUpdatedAt()
        );
    }

    private PublicSurveySummaryDto toPublicSummary(Survey survey) {
        return new PublicSurveySummaryDto(
            survey.getPublicCode(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getImageKey(),
            survey.getAccessMode(),
            tagsFrom(survey)
        );
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        try {
            return tags.stream().map(TagNormalizer::normalize).distinct().toList();
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    private boolean matchesTags(Survey survey, List<String> tags) {
        return tags.isEmpty() || tagsFrom(survey).containsAll(tags);
    }

    private void saveTags(Survey survey, List<String> tags) {
        List<String> normalized = normalizeTags(tags).stream().limit(4).toList();
        survey.setTags(normalized.toArray(String[]::new));
    }

    private List<String> tagsFrom(Survey survey) {
        return survey.getTags() == null ? List.of() : Arrays.asList(survey.getTags());
    }

    private String passwordHash(SurveyAccessMode mode, String rawPassword) {
        SurveyAccessMode resolvedMode = mode == null ? SurveyAccessMode.PUBLIC : mode;
        if (resolvedMode != SurveyAccessMode.PASSWORD_PROTECTED) {
            return null;
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Las encuestas con contraseña necesitan contraseña de acceso");
        }
        return passwordEncoder.encode(rawPassword);
    }

    private String updatedPasswordHash(SurveyAccessMode previousMode, SurveyAccessMode requestedMode, String currentHash, String rawPassword) {
        if (requestedMode != SurveyAccessMode.PASSWORD_PROTECTED) {
            return null;
        }
        if (rawPassword != null && !rawPassword.isBlank()) {
            return passwordEncoder.encode(rawPassword);
        }
        if (previousMode == SurveyAccessMode.PASSWORD_PROTECTED && currentHash != null) {
            return currentHash;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Las encuestas con contraseña necesitan contraseña de acceso");
    }

    private void ensureCanEdit(SurveyAccess access) {
        if (!access.canEdit()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puedes editar esta encuesta");
        }
    }

    private void ensureOwner(Survey survey, User user) {
        if (!isOwner(survey, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el propietario puede realizar esta operación");
        }
    }

    private void ensureClosedForDelete(Survey survey) {
        if (survey.getLifecycleStatus() != SurveyLifecycle.CLOSED) {
            throw new ApiException(HttpStatus.CONFLICT, "La encuesta debe estar cerrada antes de eliminarse");
        }
    }

    private boolean isOwner(Survey survey, User user) {
        return user != null && survey.getOwner().getId().equals(user.getId());
    }

    private SurveyAccess resolveAccess(Survey survey, User user) {
        if (user == null) {
            return NO_ACCESS;
        }
        if (isOwner(survey, user)) {
            return OWNER_ACCESS;
        }
        return collaboratorRepository.findPermission(survey.getId(), user.getId())
            .map(permission -> new SurveyAccess(permission == CollaborationPermission.EDIT, true))
            .orElse(NO_ACCESS);
    }

    private boolean isAdmin(User user) {
        return user != null && (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPERADMIN);
    }

    private void notifySurveyStatusChanged(Survey survey, String title, String message) {
        notificationService.createSurveyNotification(
            survey.getOwner(),
            NotificationType.SURVEY_STATUS_CHANGED,
            title,
            message,
            survey
        );
        collaboratorRepository.findByIdSurveyId(survey.getId()).forEach(collaborator ->
            notificationService.createSurveyNotification(
                collaborator.getUser(),
                NotificationType.SURVEY_STATUS_CHANGED,
                title,
                message,
                survey
            )
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record SurveyAccess(boolean canEdit, boolean canPreview) {
    }
}
