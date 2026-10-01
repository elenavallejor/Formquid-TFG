package com.formquid.collaboration.service;

import com.formquid.collaboration.dto.CollaborationRequestResponseDto;
import com.formquid.collaboration.dto.CreateCollaborationRequestDto;
import com.formquid.collaboration.dto.PublicationRequestResponseDto;
import com.formquid.collaboration.dto.SurveyCollaboratorResponseDto;
import com.formquid.collaboration.entity.CollaborationRequest;
import com.formquid.collaboration.entity.CollaborationPermission;
import com.formquid.collaboration.entity.CollaborationRequestType;
import com.formquid.collaboration.entity.PublicationRequest;
import com.formquid.collaboration.entity.SurveyCollaborator;
import com.formquid.collaboration.entity.SurveyCollaboratorId;
import com.formquid.collaboration.repository.CollaborationRequestRepository;
import com.formquid.collaboration.repository.PublicationRequestRepository;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.common.service.EmailNormalizer;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.content.service.ContentService;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.service.NotificationService;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final SurveyService surveyService;

    /**
     * Devuelve los colaboradores de una encuesta.
     */
    @Transactional(readOnly = true)
    public List<SurveyCollaboratorResponseDto> findCollaborators(Long surveyId) {
        return collaboratorRepository.findByIdSurveyId(surveyId)
            .stream()
            .map(this::toCollaboratorResponse)
            .toList();
    }

    /**
     * Devuelve las solicitudes pendientes de una encuesta.
     */
    @Transactional(readOnly = true)
    public List<CollaborationRequestResponseDto> findRequests(Long surveyId) {
        return requestRepository.findBySurveyIdOrderByCreatedAtDesc(surveyId)
            .stream()
            .map(this::toRequestResponse)
            .toList();
    }

    /**
     * Devuelve las solicitudes e invitaciones pendientes que el usuario debe gestionar.
     */
    @Transactional(readOnly = true)
    public List<CollaborationRequestResponseDto> findRequestsForUser(Long userId) {
        return requestRepository.findRelatedToUser(userId)
            .stream()
            .map(this::toRequestResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicationRequestResponseDto> findPublicationRequests(Long surveyId) {
        return publicationRequestRepository.findBySurveyIdOrderByCreatedAtDesc(surveyId)
            .stream()
            .map(request -> new PublicationRequestResponseDto(
                request.getId(),
                request.getSurvey().getId(),
                request.getRequestedBy().getId(),
                request.getRequestedBy().getName(),
                request.getRequestedBy().getEmail(),
                request.getMessage(),
                request.getCreatedAt()
            ))
            .toList();
    }

    /**
     * Crea una solicitud de colaboración.
     */
    @Transactional
    public CollaborationRequestResponseDto request(Long surveyId, Long actorId, CreateCollaborationRequestDto dto) {
        Survey survey = getSurvey(surveyId);
        User targetUser = resolveCollaborationTarget(dto);

        validateCollaborationRequest(survey, actorId, targetUser.getId(), dto.requestType());

        CollaborationRequest request = CollaborationRequest.builder()
            .survey(survey)
            .user(targetUser)
            .requestType(dto.requestType())
            .permission(dto.permission() == null ? CollaborationPermission.EDIT : dto.permission())
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
            (dto.requestType() == CollaborationRequestType.INVITATION
                ? survey.getOwner().getName() + " te ha invitado a colaborar en "
                : targetUser.getName() + " ha solicitado colaborar en ") + survey.getTitle()
                + ". Permiso: " + (saved.getPermission() == CollaborationPermission.VIEW ? "solo lectura." : "edición."),
            saved
        );

        return toRequestResponse(saved);
    }

    /**
     * Acepta una solicitud de colaboración.
     */
    @Transactional
    public SurveyCollaboratorResponseDto accept(Long requestId, Long actorId) {
        CollaborationRequest request = getRequest(requestId);
        ensureCollaborationResolver(request, actorId);

        SurveyCollaborator collaborator = SurveyCollaborator.builder()
            .id(new SurveyCollaboratorId(request.getSurvey().getId(), request.getUser().getId()))
            .survey(request.getSurvey())
            .user(request.getUser())
            .permission(request.getPermission())
            .build();

        SurveyCollaborator saved = collaboratorRepository.save(collaborator);
        User recipient = getResolutionRecipient(request);

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
    public void reject(Long requestId, Long actorId) {
        CollaborationRequest request = getRequest(requestId);
        ensureCollaborationResolver(request, actorId);

        User recipient = getResolutionRecipient(request);

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
    public void removeCollaborator(Long surveyId, Long collaboratorId, Long ownerId) {
        Survey survey = getSurvey(surveyId);
        ensureOwner(survey, ownerId);
        SurveyCollaborator collaborator = getCollaborator(surveyId, collaboratorId);

        collaboratorRepository.delete(collaborator);
        notificationService.createSurveyNotification(
            collaborator.getUser(),
            NotificationType.COLLABORATION_REVOKED,
            "Colaboración revocada",
            "Tu colaboración en " + survey.getTitle() + " ha sido revocada",
            survey
        );
    }

    /**
     * Actualiza el permiso de un colaborador.
     */
    @Transactional
    public SurveyCollaboratorResponseDto updateCollaboratorPermission(
        Long surveyId,
        Long collaboratorId,
        Long ownerId,
        CollaborationPermission permission
    ) {
        Survey survey = getSurvey(surveyId);
        ensureOwner(survey, ownerId);
        SurveyCollaborator collaborator = getCollaborator(surveyId, collaboratorId);

        if (collaborator.getPermission() == CollaborationPermission.EDIT && permission == CollaborationPermission.VIEW) {
            notificationService.createSurveyNotification(collaborator.getUser(),
                NotificationType.COLLABORATION_PERMISSION_REDUCED,
                "Permiso de edición revocado",
                "Tu permiso en " + survey.getTitle() + " ha cambiado a solo lectura. Ya no puedes editar la encuesta.", survey);
        }
        collaborator.setPermission(permission);
        return toCollaboratorResponse(collaboratorRepository.save(collaborator));
    }

    /**
     * Crea una solicitud de publicación.
     */
    @Transactional
    public Long requestPublication(Long surveyId, Long requesterId, String message) {
        Survey survey = getSurvey(surveyId);

        if (survey.getOwner().getId().equals(requesterId)) {
            throw new ApiException(HttpStatus.CONFLICT, "El propietario puede publicar directamente");
        }

        if (!collaboratorRepository.existsByIdSurveyIdAndIdUserIdAndPermission(
            surveyId,
            requesterId,
            CollaborationPermission.EDIT
        )) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo un colaborador con permiso de edición puede solicitar publicación");
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
            request.getRequestedBy().getName() + " ha solicitado publicar la encuesta " + survey.getTitle(),
            request
        );

        return request.getId();
    }

    /**
     * Acepta una solicitud de publicación.
     */
    @Transactional
    public void acceptPublication(Long requestId, Long ownerId) {
        PublicationRequest request = getPublicationRequest(requestId);
        ensureOwner(request.getSurvey(), ownerId);

        if (!surveyHasQuestions(request.getSurvey().getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "La encuesta debe contener al menos una pregunta");
        }

        surveyService.openAfterPublicationApproval(request.getSurvey());

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
    public void rejectPublication(Long requestId, Long ownerId) {
        PublicationRequest request = getPublicationRequest(requestId);
        ensureOwner(request.getSurvey(), ownerId);

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
    private void validateCollaborationRequest(Survey survey, Long actorId, Long targetUserId, CollaborationRequestType type) {
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
    private void ensureOwner(Survey survey, Long userId) {
        if (!survey.getOwner().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el propietario puede resolver esta operación");
        }
    }

    /**
     * Comprueba quién puede resolver una solicitud.
     */
    private void ensureCollaborationResolver(CollaborationRequest request, Long actorId) {
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
    private boolean surveyHasQuestions(Long surveyId) {
        return contentService.hasQuestion(contentItemRepository.findBySurveyIdOrderByPositionAsc(surveyId));
    }

    /**
     * Obtiene una encuesta.
     */
    private Survey getSurvey(Long surveyId) {
        return surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
    }

    /**
     * Obtiene una solicitud de colaboración.
     */
    private CollaborationRequest getRequest(Long requestId) {
        return requestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de colaboración no encontrada"));
    }

    private SurveyCollaborator getCollaborator(Long surveyId, Long collaboratorId) {
        return collaboratorRepository.findById(new SurveyCollaboratorId(surveyId, collaboratorId))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Colaborador no encontrado"));
    }

    /**
     * Obtiene una solicitud de publicación.
     */
    private PublicationRequest getPublicationRequest(Long requestId) {
        return publicationRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de publicación no encontrada"));
    }

    /**
     * Obtiene un usuario.
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private User resolveCollaborationTarget(CreateCollaborationRequestDto dto) {
        if (dto.requestType() == CollaborationRequestType.INVITATION) {
            if (dto.email() == null || dto.email().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Indica el correo electrónico del usuario invitado");
            }
            String normalizedEmail = EmailNormalizer.normalize(dto.email());
            return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe ningún usuario con ese correo electrónico"));
        }
        if (dto.userId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La solicitud de acceso necesita un usuario");
        }
        return getUser(dto.userId());
    }

    /**
     * Convierte una solicitud a DTO.
     */
    private CollaborationRequestResponseDto toRequestResponse(CollaborationRequest request) {
        return new CollaborationRequestResponseDto(
            request.getId(),
            request.getSurvey().getId(),
            request.getSurvey().getTitle(),
            request.getUser().getId(),
            request.getUser().getName(),
            request.getUser().getEmail(),
            request.getSurvey().getOwner().getId(),
            request.getSurvey().getOwner().getName(),
            request.getSurvey().getOwner().getEmail(),
            request.getRequestType(),
            request.getPermission(),
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
            collaborator.getPermission(),
            collaborator.getJoinedAt()
        );
    }
}
