package com.formquid.survey.service;

import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.common.service.IdSequenceService;
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
import com.formquid.survey.dto.SurveyResponseDto;
import com.formquid.survey.dto.SurveySummaryDto;
import com.formquid.survey.dto.UpdateSurveyRequestDto;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.template.entity.Template;
import com.formquid.template.repository.TemplateRepository;
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Centraliza la lógica de creación, consulta, permisos y gestión de las encuestas.
 */
@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyCollaboratorRepository collaboratorRepository;
    private final TemplateRepository templateRepository;
    private final ContentItemRepository contentItemRepository;
    private final ParticipationRepository participationRepository;
    private final ContentService contentService;
    private final TemplateService templateService;
    private final IdSequenceService idSequenceService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public SurveyPageResponseDto findEditableByUser(
        User user,
        String search,
        SurveyLifecycle lifecycleStatus,
        Pageable pageable
    ) {
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

        String normalizedSearch = search == null
            ? ""
            : search.trim().toLowerCase(Locale.ROOT);

        PageRequest safePageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(updatedAtOrder.getDirection(), "updatedAt")
        );

        Page<Survey> surveys = surveyRepository.findEditableByUser(
            user.getId(),
            normalizedSearch,
            lifecycleStatus,
            safePageable
        );

        return new SurveyPageResponseDto(
            surveys.getContent().stream().map(this::toSummary).toList(),
            surveys.getNumber(),
            surveys.getSize(),
            surveys.getTotalElements(),
            surveys.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminSurveySummaryDto> findAdminSummaries() {
        return surveyRepository.findAdminSummaries();
    }

    @Transactional(readOnly = true)
    public List<PublicSurveySummaryDto> findListedOpen(List<String> tags) {
        List<String> normalizedTags = normalizeTags(tags);
        return surveyRepository.findByLifecycleStatusAndVisibilityOrderByCreatedAtDesc(
                SurveyLifecycle.OPEN,
                SurveyVisibility.LISTED
            ).stream()
            .filter(survey -> survey.getAccessMode() != SurveyAccessMode.AUTHENTICATED)
            .filter(survey -> matchesTags(survey, normalizedTags))
            .map(this::toPublicSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicSurveySummaryDto> findListedOpenForRegistered(User user, List<String> tags) {
        List<String> normalizedTags = normalizeTags(tags);
        return surveyRepository.findByLifecycleStatusAndVisibilityOrderByCreatedAtDesc(
                SurveyLifecycle.OPEN,
                SurveyVisibility.LISTED
            ).stream()
            .filter(survey -> canViewOpenSurvey(survey, user))
            .filter(survey -> matchesTags(survey, normalizedTags))
            .map(this::toPublicSummary)
            .toList();
    }

    private PublicSurveySummaryDto toPublicSummary(Survey survey) {
        return new PublicSurveySummaryDto(
            survey.getId(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getImageKey(),
            survey.getAccessMode()
        );
    }

    @Transactional(readOnly = true)
    public SurveyResponseDto getSurveyForUser(String surveyId, User user) {
        Survey survey = getSurvey(surveyId);
        ensureCanViewSurvey(survey, user);
        return toResponse(survey);
    }

    @Transactional(readOnly = true)
    public SurveyResponseDto getPublicSurvey(String surveyId) {
        Survey survey = getSurvey(surveyId);
        if (!canViewPublicSurvey(survey)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Encuesta no disponible públicamente");
        }
        return toResponse(survey);
    }

    private boolean canViewPublicSurvey(Survey survey) {
        return survey.getLifecycleStatus() == SurveyLifecycle.OPEN
            && survey.getAccessMode() != SurveyAccessMode.AUTHENTICATED;
    }

    private boolean canViewOpenSurvey(Survey survey, User user) {
        return survey.getLifecycleStatus() == SurveyLifecycle.OPEN
            && (survey.getAccessMode() != SurveyAccessMode.AUTHENTICATED || user != null);
    }

    public void ensureCanViewSurvey(Survey survey, User user) {
        if (isOwner(survey, user)
            || isCollaborator(survey, user)
            || isAdmin(user)
            || canViewOpenSurvey(survey, user)) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "No puedes consultar esta encuesta");
    }

    public void ensureCanViewResults(Survey survey, User user) {
        if (isOwner(survey, user)
            || isCollaborator(survey, user)
            || isAdmin(user)) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "No puedes consultar las estadísticas de esta encuesta");
    }

    public boolean canEditSurvey(Survey survey, User user) {
        return isOwner(survey, user) || isCollaborator(survey, user) || isSuperadmin(user);
    }

    public boolean canResolveSurveyAsAdmin(User user) {
        return isAdmin(user);
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
        if (tags.isEmpty()) {
            return true;
        }
        return tagsFrom(survey).containsAll(tags);
    }

    public Survey getSurvey(String surveyId) {
        return surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
    }
    @Transactional
    public SurveyResponseDto create(CreateSurveyRequestDto request, User owner) {
        Template sourceTemplate = null;
        if (request.sourceTemplateId() != null) {
            sourceTemplate = templateRepository.findById(request.sourceTemplateId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Plantilla no encontrada"));
        }
        Survey survey = Survey.builder()
            .id(idSequenceService.nextSurveyId())
            .owner(owner)
            .sourceTemplate(sourceTemplate)
            .title(request.title().trim())
            .description(trimToNull(request.description()))
            .imageKey(trimToNull(request.imageKey()))
            .lifecycleStatus(SurveyLifecycle.DRAFT)
            .accessMode(request.accessMode() == null ? SurveyAccessMode.PUBLIC : request.accessMode())
            .visibility(request.visibility() == null ? SurveyVisibility.LISTED : request.visibility())
            .accessPasswordHash(passwordHash(request.accessMode(), request.accessPassword()))
            .build();
        Survey saved = surveyRepository.save(survey);
        saveTags(saved, request.tags());
        if (sourceTemplate != null) {
            templateService.copyTemplateIntoSurvey(sourceTemplate, saved, owner);
        }
        return toResponse(saved);
    }
    @Transactional
    public SurveyResponseDto update(String surveyId, UpdateSurveyRequestDto request, User actor) {
        Survey survey = getSurvey(surveyId);
        ensureCanEdit(survey, actor);
        survey.setTitle(request.title().trim());
        survey.setDescription(trimToNull(request.description()));
        survey.setImageKey(trimToNull(request.imageKey()));
        survey.setAccessMode(request.accessMode() == null ? SurveyAccessMode.PUBLIC : request.accessMode());
        survey.setVisibility(request.visibility() == null ? SurveyVisibility.LISTED : request.visibility());
        survey.setAccessPasswordHash(passwordHash(request.accessMode(), request.accessPassword()));
        saveTags(survey, request.tags());
        return toResponse(survey);
    }
    @Transactional
    public SurveyResponseDto changeLifecycle(String surveyId, SurveyLifecycle lifecycle, User actor) {
        Survey survey = getSurvey(surveyId);
        ensureOwnerOrSuperadmin(survey, actor);
        if (lifecycle == SurveyLifecycle.OPEN
            && !contentService.hasQuestion(contentItemRepository.findBySurveyIdOrderByPositionAsc(surveyId))) {
            throw new ApiException(HttpStatus.CONFLICT, "La encuesta no puede abrirse sin preguntas");
        }
        survey.setLifecycleStatus(lifecycle);
        notifySurveyStatusChanged(survey, "Estado de encuesta actualizado", "La encuesta " + survey.getTitle() + " ha pasado a estado " + lifecycle.name());
        return toResponse(survey);
    }

    @Transactional
    public void delete(String surveyId, User actor) {
        Survey survey = getSurvey(surveyId);
        ensureOwner(survey, actor);
        ensureClosedForDelete(survey);
        surveyRepository.delete(survey);
    }

    @Transactional
    public SurveyResponseDto adminChangeLifecycle(String surveyId, SurveyLifecycle lifecycle, User admin) {
        if (!isAdmin(admin)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administración puede intervenir encuestas");
        }
        Survey survey = getSurvey(surveyId);
        if (lifecycle == SurveyLifecycle.OPEN
            && !contentService.hasQuestion(contentItemRepository.findBySurveyIdOrderByPositionAsc(surveyId))) {
            throw new ApiException(HttpStatus.CONFLICT, "La encuesta no puede abrirse sin preguntas");
        }
        survey.setLifecycleStatus(lifecycle);
        notifySurveyStatusChanged(survey, "Intervención administrativa sobre encuesta", "Administración ha cambiado la encuesta " + survey.getTitle() + " a estado " + lifecycle.name());
        return toResponse(survey);
    }

    @Transactional
    public void adminDelete(String surveyId, User admin) {
        if (!isAdmin(admin)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administración puede eliminar encuestas");
        }
        Survey survey = getSurvey(surveyId);
        ensureClosedForDelete(survey);
        surveyRepository.delete(survey);
    }

    public void ensureStructureEditable(Survey survey) {
        if (participationRepository.existsBySurveyId(survey.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "No puede modificarse la estructura de una encuesta con respuestas");
        }
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

    public SurveyResponseDto toResponse(Survey survey) {
        List<ContentItemResponseDto> content = contentService.findSurveyContent(survey.getId())
            .stream()
            .sorted(Comparator.comparing(ContentItemResponseDto::position))
            .toList();
        return new SurveyResponseDto(
            survey.getId(),
            survey.getOwner().getId(),
            survey.getSourceTemplate() == null ? null : survey.getSourceTemplate().getId(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getImageKey(),
            survey.getLifecycleStatus(),
            survey.getAccessMode(),
            survey.getVisibility(),
            tagsFrom(survey),
            content,
            survey.getCreatedAt(),
            survey.getUpdatedAt()
        );
    }

    private void saveTags(Survey survey, List<String> tags) {
        List<String> normalized = normalizeTags(tags).stream().limit(4).toList();
        survey.setTags(normalized.toArray(String[]::new));
    }

    private List<String> tagsFrom(Survey survey) {
        return survey.getTags() == null ? List.of() : Arrays.asList(survey.getTags());
    }

    private String passwordHash(SurveyAccessMode mode, String rawPassword) {
        if (mode == SurveyAccessMode.PASSWORD_PROTECTED) {
            if (rawPassword == null || rawPassword.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las encuestas con contraseña necesitan contraseña de acceso");
            }
            return passwordEncoder.encode(rawPassword);
        }
        return null;
    }


    private void ensureCanEdit(Survey survey, User user) {
        if (!canEditSurvey(survey, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puedes editar esta encuesta");
        }
    }

    private void ensureOwner(Survey survey, User user) {
        if (!survey.getOwner().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el propietario puede realizar esta operación");
        }
    }

    private void ensureOwnerOrSuperadmin(Survey survey, User user) {
        if (!isOwner(survey, user) && !isSuperadmin(user)) {
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

    private boolean isCollaborator(Survey survey, User user) {
        return user != null && collaboratorRepository.existsByIdSurveyIdAndIdUserId(survey.getId(), user.getId());
    }

    private boolean isAdmin(User user) {
        return user != null && (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPERADMIN);
    }

    private boolean isSuperadmin(User user) {
        return user != null && user.getRole() == UserRole.SUPERADMIN;
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
}