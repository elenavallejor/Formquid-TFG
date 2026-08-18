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
import com.formquid.common.entity.RequestStatus;
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

    public List<SurveyCollaboratorResponseDto> findCollaborators(String surveyId) {
        return collaboratorRepository.findByIdSurveyId(surveyId).stream().map(this::toCollaboratorResponse).toList();
    }

    public List<CollaborationRequestResponseDto> findRequests(String surveyId) {
        return requestRepository.findBySurveyIdAndStatusOrderByCreatedAtDesc(surveyId, RequestStatus.PENDING)
            .stream()
            .map(this::toRequestResponse)
            .toList();
    }

    /**
     * Recibe una encuesta y usuario, devuelve solicitud pendiente.
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
            .message(dto.message())
            .build();
        CollaborationRequest saved = requestRepository.save(request);
        notificationService.createCollaborationRequestNotification(
            dto.requestType() == CollaborationRequestType.INVITATION ? targetUser : survey.getOwner(),
            dto.requestType() == CollaborationRequestType.INVITATION
                ? NotificationType.COLLABORATION_INVITATION
                : NotificationType.COLLABORATION_REQUEST,
            "Solicitud de colaboración",
            "Hay una solicitud de colaboración para la encuesta " + survey.getTitle(),
            saved
        );
        return toRequestResponse(saved);
    }

    /**
     * Recibe solicitud pendiente. Devuelve colaborador creado.
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
        request.approve();
        User recipient = request.getRequestType() == CollaborationRequestType.INVITATION
            ? request.getSurvey().getOwner()
            : request.getUser();
        notificationService.createCollaborationRequestNotification(
            recipient,
            NotificationType.COLLABORATION_ACCEPTED,
            "Colaboración aceptada",
            "La colaboración en " + request.getSurvey().getTitle() + " ha sido aceptada",
            request
        );
        return toCollaboratorResponse(saved);
    }

    @Transactional
    public void reject(UUID requestId, UUID actorId) {
        CollaborationRequest request = getRequest(requestId);
        ensureCollaborationResolver(request, actorId);
        request.reject();
        User recipient = request.getRequestType() == CollaborationRequestType.INVITATION
            ? request.getSurvey().getOwner()
            : request.getUser();
        notificationService.createCollaborationRequestNotification(
            recipient,
            NotificationType.COLLABORATION_REJECTED,
            "Colaboración rechazada",
            "La colaboración en " + request.getSurvey().getTitle() + " ha sido rechazada",
            request
        );
    }

    /**
     * Recibe encuesta y usuario colaborador. No devuelve contenido.
     */
    @Transactional
    public void removeCollaborator(String surveyId, UUID collaboratorId, UUID ownerId) {
        Survey survey = getSurvey(surveyId);
        ensureOwner(survey, ownerId);
        collaboratorRepository.deleteByIdSurveyIdAndIdUserId(surveyId, collaboratorId);
    }

    /**
     * Recibe encuesta y colaborador, devuelve la solicitud de publicación.
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
        if (!contentService.hasQuestion(contentItemRepository.findBySurveyIdOrderByPositionAsc(surveyId))) {
            throw new ApiException(HttpStatus.CONFLICT, "La encuesta debe contener al menos una pregunta");
        }
        if (publicationRequestRepository.existsBySurveyIdAndStatus(surveyId, RequestStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una solicitud de publicación pendiente");
        }
        PublicationRequest request = publicationRequestRepository.save(PublicationRequest.builder()
            .survey(survey)
            .requestedBy(getUser(requesterId))
            .message(message)
            .build());
        notificationService.createPublicationRequestNotification(
            survey.getOwner(),
            NotificationType.PUBLICATION_REQUEST,
            "Solicitud de publicación",
            "Un colaborador ha solicitado publicar la encuesta " + survey.getTitle(),
            request
        );
        return request.getId();
    }

    @Transactional
    public void acceptPublication(UUID requestId, UUID ownerId) {
        PublicationRequest request = getPublicationRequest(requestId);
        ensureOwner(request.getSurvey(), ownerId);
        if (!contentService.hasQuestion(contentItemRepository.findBySurveyIdOrderByPositionAsc(request.getSurvey().getId()))) {
            throw new ApiException(HttpStatus.CONFLICT, "La encuesta debe contener al menos una pregunta");
        }
        request.getSurvey().setLifecycleStatus(SurveyLifecycle.OPEN);
        request.approve();
        notificationService.createPublicationRequestNotification(
            request.getRequestedBy(),
            NotificationType.PUBLICATION_ACCEPTED,
            "Publicación aprobada",
            "La encuesta " + request.getSurvey().getTitle() + " ha sido publicada",
            request
        );
    }

    @Transactional
    public void rejectPublication(UUID requestId, UUID ownerId) {
        PublicationRequest request = getPublicationRequest(requestId);
        ensureOwner(request.getSurvey(), ownerId);
        request.reject();
        notificationService.createPublicationRequestNotification(
            request.getRequestedBy(),
            NotificationType.PUBLICATION_REJECTED,
            "Publicación rechazada",
            "La publicación de la encuesta " + request.getSurvey().getTitle() + " ha sido rechazada",
            request
        );
    }

    private void validateCollaborationRequest(
        Survey survey,
        UUID actorId,
        UUID targetUserId,
        CollaborationRequestType type
    ) {
        if (survey.getOwner().getId().equals(targetUserId)) {
            throw new ApiException(HttpStatus.CONFLICT, "El propietario no puede ser colaborador");
        }
        if (collaboratorRepository.existsByIdSurveyIdAndIdUserId(survey.getId(), targetUserId)) {
            throw new ApiException(HttpStatus.CONFLICT, "El usuario ya colabora en la encuesta");
        }
        if (requestRepository.existsBySurveyIdAndUserIdAndStatus(survey.getId(), targetUserId, RequestStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una solicitud pendiente");
        }
        if (type == CollaborationRequestType.INVITATION && !survey.getOwner().getId().equals(actorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el propietario puede invitar colaboradores");
        }
        if (type == CollaborationRequestType.ACCESS_REQUEST && !actorId.equals(targetUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "La solicitud de acceso debe crearla el propio usuario");
        }
    }

    private void ensureOwner(Survey survey, UUID userId) {
        if (!survey.getOwner().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el propietario puede resolver esta operación");
        }
    }

    private void ensureCollaborationResolver(CollaborationRequest request, UUID actorId) {
        if (request.getRequestType() == CollaborationRequestType.INVITATION) {
            if (!request.getUser().getId().equals(actorId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Solo el usuario invitado puede resolver la invitación");
            }
            return;
        }
        ensureOwner(request.getSurvey(), actorId);
    }

    private Survey getSurvey(String surveyId) {
        return surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
    }

    private CollaborationRequest getRequest(UUID requestId) {
        CollaborationRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de colaboración no encontrada"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "La solicitud de colaboración ya está resuelta");
        }
        return request;
    }

    private PublicationRequest getPublicationRequest(UUID requestId) {
        PublicationRequest request = publicationRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de publicación no encontrada"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "La solicitud de publicación ya está resuelta");
        }
        return request;
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

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
