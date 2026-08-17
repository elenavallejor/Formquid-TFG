package com.formquid.collaboration.service;

import com.formquid.collaboration.dto.CollaborationRequestResponseDto;
import com.formquid.collaboration.dto.CreateCollaborationRequestDto;
import com.formquid.collaboration.dto.SurveyCollaboratorResponseDto;
import com.formquid.collaboration.entity.SurveyCollaboration;
import com.formquid.collaboration.entity.SurveyCollaborationStatus;
import com.formquid.collaboration.repository.SurveyCollaborationRepository;
import com.formquid.notification.service.NotificationService;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de negocio para colaboraciones de encuestas.
 *
 * Las invitaciones pendientes, aceptadas, rechazadas y retiradas se almacenan
 * en una única tabla con estado. No se borran solicitudes resueltas porque la
 * plataforma necesita trazabilidad de uso.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollaborationService {

    private final SurveyCollaborationRepository surveyCollaborationRepository;
    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;
    private final SurveyService surveyService;
    private final NotificationService notificationService;

    /**
     * Crea o reabre una invitación pendiente de colaboración.
     */
    @Transactional
    public CollaborationRequestResponseDto createRequest(
        Long surveyId,
        CreateCollaborationRequestDto request,
        String requesterEmail
    ) {
        User requester = getAuthenticatedUser(requesterEmail);
        Survey survey = getSurvey(surveyId);

        surveyService.assertCanManageSurvey(
            survey,
            requester.getId(),
            requester.getRole()
        );

        User invitedEditor = resolveInvitedEditor(request);
        validateInvitedEditor(survey, invitedEditor, requester.getId());

        SurveyCollaboration collaboration =
            findExistingCollaboration(survey.getId(), invitedEditor.getId())
                .map(existing -> reopenExistingRequest(
                    existing,
                    requester,
                    cleanOptional(request.message())
                ))
                .orElseGet(() -> SurveyCollaboration.builder()
                    .survey(survey)
                    .requestedBy(requester)
                    .editor(invitedEditor)
                    .message(cleanOptional(request.message()))
                    .status(SurveyCollaborationStatus.PENDING)
                    .build());

        SurveyCollaboration saved =
            surveyCollaborationRepository.save(collaboration);

        notificationService.notifyCollaborationInvite(
            invitedEditor.getId(),
            saved.getId(),
            survey.getTitle()
        );

        return toRequestResponse(saved);
    }

    /**
     * Lista las invitaciones pendientes de una encuesta.
     */
    public List<CollaborationRequestResponseDto> getPendingRequests(
        Long surveyId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        surveyService.assertCanManageSurvey(
            survey,
            user.getId(),
            user.getRole()
        );

        return surveyCollaborationRepository
            .findBySurveyIdAndStatusOrderByCreatedAtDesc(
                surveyId,
                SurveyCollaborationStatus.PENDING
            )
            .stream()
            .map(this::toRequestResponse)
            .toList();
    }

    /**
     * Lista las invitaciones pendientes recibidas por el usuario autenticado.
     */
    public List<CollaborationRequestResponseDto> getReceivedRequests(
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);

        return surveyCollaborationRepository
            .findByEditorIdAndStatusOrderByCreatedAtDesc(
                user.getId(),
                SurveyCollaborationStatus.PENDING
            )
            .stream()
            .map(this::toRequestResponse)
            .toList();
    }

    /**
     * Acepta una invitación pendiente.
     */
    @Transactional
    public SurveyCollaboratorResponseDto accept(
        Long requestId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        SurveyCollaboration collaboration = getCollaboration(requestId);

        validatePending(collaboration);
        validateInvitedUser(collaboration, user.getId());

        collaboration.accept();

        notificationService.notifyCollaborationAccepted(
            collaboration.getRequestedBy().getId(),
            collaboration.getSurvey().getId(),
            collaboration.getEditor().getName(),
            collaboration.getSurvey().getTitle()
        );

        return toCollaboratorResponse(collaboration);
    }

    /**
     * Rechaza una invitación pendiente.
     */
    @Transactional
    public CollaborationRequestResponseDto reject(
        Long requestId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        SurveyCollaboration collaboration = getCollaboration(requestId);

        validatePending(collaboration);
        validateInvitedUser(collaboration, user.getId());

        collaboration.reject();

        notificationService.notifyCollaborationRejected(
            collaboration.getRequestedBy().getId(),
            collaboration.getSurvey().getId(),
            collaboration.getEditor().getName(),
            collaboration.getSurvey().getTitle()
        );

        return toRequestResponse(collaboration);
    }

    /**
     * Lista colaboradores aceptados de una encuesta.
     */
    public List<SurveyCollaboratorResponseDto> getCollaborators(
        Long surveyId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        surveyService.assertCanViewSurvey(
            survey,
            user.getId(),
            user.getRole()
        );

        return surveyCollaborationRepository
            .findBySurveyIdAndStatusOrderByCreatedAtDesc(
                surveyId,
                SurveyCollaborationStatus.ACCEPTED
            )
            .stream()
            .map(this::toCollaboratorResponse)
            .toList();
    }

    /**
     * Marca una colaboración como retirada sin borrar su historial.
     */
    @Transactional
    public void removeCollaborator(
        Long surveyId,
        Long editorId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        surveyService.assertCanManageSurvey(
            survey,
            user.getId(),
            user.getRole()
        );

        validateOwnerCannotBeRemoved(survey, editorId);

        SurveyCollaboration collaboration =
            surveyCollaborationRepository
                .findBySurveyIdAndEditorId(surveyId, editorId)
                .filter(item -> item.getStatus() == SurveyCollaborationStatus.ACCEPTED)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El editor indicado no es colaborador de esta encuesta"
                ));

        collaboration.remove();

        notificationService.notifyCollaboratorRemoved(
            collaboration.getEditor().getId(),
            survey.getId(),
            survey.getTitle()
        );
    }

    private SurveyCollaboration reopenExistingRequest(
        SurveyCollaboration existing,
        User requester,
        String message
    ) {
        if (existing.getStatus() == SurveyCollaborationStatus.PENDING) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ya existe una invitación pendiente para este editor"
            );
        }

        if (existing.getStatus() == SurveyCollaborationStatus.ACCEPTED) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El editor ya es colaborador de esta encuesta"
            );
        }

        existing.reopen(requester, message);
        return existing;
    }

    private java.util.Optional<SurveyCollaboration> findExistingCollaboration(
        Long surveyId,
        Long editorId
    ) {
        return surveyCollaborationRepository.findBySurveyIdAndEditorId(
            surveyId,
            editorId
        );
    }

    private Survey getSurvey(Long surveyId) {
        return surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la encuesta indicada"
            ));
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

    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe el usuario indicado"
            ));
    }

    private SurveyCollaboration getCollaboration(Long requestId) {
        return surveyCollaborationRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la invitación de colaboración indicada"
            ));
    }

    private User resolveInvitedEditor(CreateCollaborationRequestDto request) {
        boolean hasId = request.invitedEditorId() != null;
        boolean hasEmail = StringUtils.hasText(request.invitedEditorEmail());

        if (hasId == hasEmail) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debes indicar el id del editor o su correo electrónico, pero no ambos"
            );
        }

        if (hasId) {
            return getUser(request.invitedEditorId());
        }

        return userRepository
            .findByEmailIgnoreCase(request.invitedEditorEmail().trim())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe un editor registrado con ese correo electrónico"
            ));
    }

    private void validateInvitedEditor(
        Survey survey,
        User invitedEditor,
        Long requesterId
    ) {
        if (invitedEditor.getRole() != UserRole.EDITOR) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se puede invitar a usuarios con rol editor"
            );
        }

        if (!invitedEditor.isEnabled()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se puede invitar a un editor deshabilitado"
            );
        }

        if (invitedEditor.getId().equals(requesterId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No puedes invitarte a ti mismo como colaborador"
            );
        }

        if (survey.getOwner().getId().equals(invitedEditor.getId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El propietario de la encuesta no puede añadirse como colaborador"
            );
        }
    }

    private void validatePending(SurveyCollaboration collaboration) {
        if (collaboration.getStatus() != SurveyCollaborationStatus.PENDING) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La invitación de colaboración ya fue resuelta"
            );
        }
    }

    private void validateInvitedUser(
        SurveyCollaboration collaboration,
        Long userId
    ) {
        if (collaboration.getEditor() == null
            || collaboration.getEditor().getId() == null
            || !collaboration.getEditor().getId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo el editor invitado puede resolver esta invitación"
            );
        }
    }

    private void validateOwnerCannotBeRemoved(Survey survey, Long editorId) {
        if (survey.getOwner() != null
            && survey.getOwner().getId() != null
            && survey.getOwner().getId().equals(editorId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El propietario de la encuesta no puede eliminarse como colaborador"
            );
        }
    }

    private CollaborationRequestResponseDto toRequestResponse(
        SurveyCollaboration collaboration
    ) {
        return new CollaborationRequestResponseDto(
            collaboration.getId(),
            collaboration.getSurvey().getId(),
            collaboration.getSurvey().getTitle(),
            collaboration.getRequestedBy().getId(),
            collaboration.getRequestedBy().getName(),
            collaboration.getEditor().getId(),
            collaboration.getEditor().getName(),
            collaboration.getEditor().getEmail(),
            collaboration.getMessage(),
            collaboration.getStatus(),
            collaboration.getCreatedAt(),
            collaboration.getResolvedAt()
        );
    }

    private SurveyCollaboratorResponseDto toCollaboratorResponse(
        SurveyCollaboration collaboration
    ) {
        LocalDateTime addedAt = collaboration.getResolvedAt() != null
            ? collaboration.getResolvedAt()
            : collaboration.getCreatedAt();

        return new SurveyCollaboratorResponseDto(
            collaboration.getId(),
            collaboration.getSurvey().getId(),
            collaboration.getSurvey().getTitle(),
            collaboration.getEditor().getId(),
            collaboration.getEditor().getName(),
            collaboration.getEditor().getEmail(),
            collaboration.getRequestedBy().getId(),
            collaboration.getRequestedBy().getName(),
            addedAt
        );
    }

    private String cleanOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
