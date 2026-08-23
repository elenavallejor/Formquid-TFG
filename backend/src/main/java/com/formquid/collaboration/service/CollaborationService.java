package com.formquid.collaboration.service;

import com.formquid.collaboration.dto.CollaborationRequestResponseDto;
import com.formquid.collaboration.dto.CreateCollaborationRequestDto;
import com.formquid.collaboration.dto.SurveyCollaboratorResponseDto;
import com.formquid.collaboration.entity.CollaborationRequest;
import com.formquid.collaboration.entity.CollaborationRequestType;
import com.formquid.collaboration.entity.PublicationRequest;
import com.formquid.collaboration.entity.SurveyCollaborator;
import com.formquid.collaboration.entity.SurveyCollaboratorId;
import com.formquid.collaboration.repository.CollaborationRequestRepository;
import com.formquid.collaboration.repository.PublicationRequestRepository;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.content.service.ContentService;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.service.NotificationService;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollaborationService {

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;
    private final SurveyCollaboratorRepository collaboratorRepository;
    private final CollaborationRequestRepository requestRepository;
    private final PublicationRequestRepository publicationRequestRepository;
    private final ContentItemRepository contentItemRepository;
    private final ContentService contentService;
    private final NotificationService notificationService;

    /**
     * Devuelve los colaboradores de una encuesta.
     */
    @Transactional(readOnly = true)
    public List<SurveyCollaboratorResponseDto> findCollaborators(String surveyId) {
        return collaboratorRepository.findByIdSurveyId(surveyId)
            .stream()
            .map(this::toCollaboratorResponse)
            .toList();
    }

    /**
     * Devuelve las solicitudes pendientes de una encuesta.
     */
    @Transactional(readOnly = true)
    public List<CollaborationRequestResponseDto> findRequests(String surveyId) {
        return requestRepository.findBySurveyIdOrderByCreatedAtDesc(surveyId)
            .stream()
            .map(this::toRequestResponse)
            .toList();
    }

    /**
     * Crea una solicitud de colaboración.
     */
    @Transactional
    public CollaborationRequestResponseDto request(String surveyId, UUID actorId, CreateCollaborationRequestDto dto) {
        Survey survey = getSurvey(surveyId);
        User targetUser = getUser(dto.userId());

        validateCollaborationRequest(survey, actorId, targetUser.getId(), dto.requestType());

        CollaborationRequest request = CollaborationRequest.builder()
            .survey(survey)
            .user(targetUser)
            .requestType(dto.requestType())
            .message(dto.message() == null ? null : dto.message().trim())
            .build();

        CollaborationRequest saved = requestRepository.save(request);

        User recipient = dto.requestType() == CollaborationRequestType.INVITATION
            ? targetUser
            : survey.getOwner();

        NotificationType type = dto.requestType() == CollaborationRequestType.INVITATION
            ? NotificationType.COLLABORATION_INVITATION
            : NotificationType.COLLABORATION_REQUEST;

        notificationService.createCollaborationRequestNotification(
            recipient,
            type,
            "Solicitud de colaboración",
            "Hay una solicitud de colaboración para la encuesta " + survey.getTitle(),
            saved
        );

        return toRequestResponse(saved);
    }

    /**
     * Acepta una solicitud de colaboración.
     */
    @Transactional
    public SurveyCollaboratorResponseDto accept(UUID requestId, UUID actorId) {
        CollaborationRequest request = getRequest(requestId);
        ensureCollaborationResolver(request, actorId);

        SurveyCollaborator collaborator = SurveyCollaborator.builder()
            .id(new SurveyCollaboratorId(request.getSurvey().getId(), request.getUser().getId()))
            .survey(request.getSurvey())
            .user(request.getUser())
            .build();

        SurveyCollaborator saved = collaboratorRepository.save(collaborator);
        User recipient = getResolutionRecipient(request);

        notificationService.deleteCollaborationRequestNotifications(request.getId());

        notificationService.createSurveyNotification(
            recipient,
            NotificationType.COLLABORATION_ACCEPTED,
            "Colaboración aceptada",
            "La colaboración en " + request.getSurvey().getTitle() + " ha sido aceptada",
            request.getSurvey()
        );

        requestRepository.delete(request);
        return toCollaboratorResponse(saved);
    }

    /**
     * Rechaza una solicitud de colaboración.
     */
    @Transactional
    public void reject(UUID requestId, UUID actorId) {
        CollaborationRequest request = getRequest(requestId);
        ensureCollaborationResolver(request, actorId);

        User recipient = getResolutionRecipient(request);

        notificationService.deleteCollaborationRequestNotifications(request.getId());

        notificationService.createSurveyNotification(
            recipient,
            NotificationType.COLLABORATION_REJECTED,
            "Colaboración rechazada",
            "La colaboración en " + request.getSurvey().getTitle() + " ha sido rechazada",
            request.getSurvey()
        );

        requestRepository.delete(request);
    }

    /**
     * Elimina un colaborador.
     */
    @Transactional
    public void removeCollaborator(String surveyId, UUID collaboratorId, UUID ownerId) {
        Survey survey = getSurvey(surveyId);
        ensureOwner(survey, ownerId);

        collaboratorRepository.deleteByIdSurveyIdAndIdUserId(surveyId, collaboratorId);
    }

    /**
     * Crea una solicitud de publicación.
     */
    @Transactional
    public UUID requestPublication(String surveyId, UUID requesterId, String message) {
        Survey survey = getSurvey(surveyId);

        if (survey.getOwner().getId().equals(requesterId)) {
            throw new ApiException(HttpStatus.CONFLICT, "El propietario puede publicar directamente");
        }

        if (!collaboratorRepository.existsByIdSurveyIdAndIdUserId(surveyId, requesterId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo un colaborador puede solicitar publicación");
        }

        if (!surveyHasQuestions(surveyId)) {
            throw new ApiException(HttpStatus.CONFLICT, "La encuesta debe contener al menos una pregunta");
        }

        if (publicationRequestRepository.existsBySurveyId(surveyId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una solicitud de publicación pendiente");
        }

        PublicationRequest request = publicationRequestRepository.save(
            PublicationRequest.builder()
                .survey(survey)
                .requestedBy(getUser(requesterId))
                .message(message == null ? null : message.trim())
                .build()
        );

        notificationService.createPublicationRequestNotification(
            survey.getOwner(),
            NotificationType.PUBLICATION_REQUEST,
            "Solicitud de publicación",
            "Un colaborador ha solicitado publicar la encuesta " + survey.getTitle(),
            request
        );

        return request.getId();
    }

    /**
     * Acepta una solicitud de publicación.
     */
    @Transactional
    public void acceptPublication(UUID requestId, UUID ownerId) {
        PublicationRequest request = getPublicationRequest(requestId);
        ensureOwner(request.getSurvey(), ownerId);

        if (!surveyHasQuestions(request.getSurvey().getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "La encuesta debe contener al menos una pregunta");
        }

        request.getSurvey().setLifecycleStatus(SurveyLifecycle.OPEN);

        notificationService.deletePublicationRequestNotifications(request.getId());

        notificationService.createSurveyNotification(
            request.getRequestedBy(),
            NotificationType.PUBLICATION_ACCEPTED,
            "Publicación aprobada",
            "La encuesta " + request.getSurvey().getTitle() + " ha sido publicada",
            request.getSurvey()
        );

        publicationRequestRepository.delete(request);
    }

    /**
     * Rechaza una solicitud de publicación.
     */
    @Transactional
    public void rejectPublication(UUID requestId, UUID ownerId) {
        PublicationRequest request = getPublicationRequest(requestId);
        ensureOwner(request.getSurvey(), ownerId);

        notificationService.deletePublicationRequestNotifications(request.getId());

        notificationService.createSurveyNotification(
            request.getRequestedBy(),
            NotificationType.PUBLICATION_REJECTED,
            "Publicación rechazada",
            "La publicación de la encuesta " + request.getSurvey().getTitle() + " ha sido rechazada",
            request.getSurvey()
        );

        publicationRequestRepository.delete(request);
    }

    /**
     * Valida una solicitud de colaboración.
     */
    private void validateCollaborationRequest(Survey survey, UUID actorId, UUID targetUserId, CollaborationRequestType type) {
        if (survey.getOwner().getId().equals(targetUserId)) {
            throw new ApiException(HttpStatus.CONFLICT, "El propietario no puede ser colaborador");
        }

        if (collaboratorRepository.existsByIdSurveyIdAndIdUserId(survey.getId(), targetUserId)) {
            throw new ApiException(HttpStatus.CONFLICT, "El usuario ya colabora en la encuesta");
        }

        if (requestRepository.existsBySurveyIdAndUserId(survey.getId(), targetUserId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una solicitud pendiente");
        }

        if (type == CollaborationRequestType.INVITATION && !survey.getOwner().getId().equals(actorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el propietario puede invitar colaboradores");
        }

        if (type == CollaborationRequestType.ACCESS_REQUEST && !actorId.equals(targetUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "La solicitud de acceso debe crearla el propio usuario");
        }
    }

    /**
     * Comprueba que el usuario es propietario.
     */
    private void ensureOwner(Survey survey, UUID userId) {
        if (!survey.getOwner().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el propietario puede resolver esta operación");
        }
    }

    /**
     * Comprueba quién puede resolver una solicitud.
     */
    private void ensureCollaborationResolver(CollaborationRequest request, UUID actorId) {
        if (request.getRequestType() == CollaborationRequestType.INVITATION) {
            if (!request.getUser().getId().equals(actorId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Solo el usuario invitado puede resolver la invitación");
            }
            return;
        }

        ensureOwner(request.getSurvey(), actorId);
    }

    /**
     * Obtiene el destinatario de la resolución.
     */
    private User getResolutionRecipient(CollaborationRequest request) {
        return request.getRequestType() == CollaborationRequestType.INVITATION
            ? request.getSurvey().getOwner()
            : request.getUser();
    }

    /**
     * Comprueba si la encuesta contiene preguntas.
     */
    private boolean surveyHasQuestions(String surveyId) {
        return contentService.hasQuestion(contentItemRepository.findBySurveyIdOrderByPositionAsc(surveyId));
    }

    /**
     * Obtiene una encuesta.
     */
    private Survey getSurvey(String surveyId) {
        return surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
    }

    /**
     * Obtiene una solicitud de colaboración.
     */
    private CollaborationRequest getRequest(UUID requestId) {
        return requestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de colaboración no encontrada"));
    }

    /**
     * Obtiene una solicitud de publicación.
     */
    private PublicationRequest getPublicationRequest(UUID requestId) {
        return publicationRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de publicación no encontrada"));
    }

    /**
     * Obtiene un usuario.
     */
    private User getUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    /**
     * Convierte una solicitud a DTO.
     */
    private CollaborationRequestResponseDto toRequestResponse(CollaborationRequest request) {
        return new CollaborationRequestResponseDto(
            request.getId(),
            request.getSurvey().getId(),
            request.getUser().getId(),
            request.getRequestType(),
            request.getMessage(),
            request.getCreatedAt()
        );
    }

    /**
     * Convierte un colaborador a DTO.
     */
    private SurveyCollaboratorResponseDto toCollaboratorResponse(SurveyCollaborator collaborator) {
        return new SurveyCollaboratorResponseDto(
            collaborator.getSurvey().getId(),
            collaborator.getUser().getId(),
            collaborator.getUser().getEmail(),
            collaborator.getUser().getName(),
            collaborator.getJoinedAt()
        );
    }
}