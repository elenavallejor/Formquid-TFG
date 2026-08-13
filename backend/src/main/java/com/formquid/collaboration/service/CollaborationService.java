package com.formquid.collaboration.service;

import com.formquid.collaboration.dto.CollaborationRequestResponseDto;
import com.formquid.collaboration.dto.CreateCollaborationRequestDto;
import com.formquid.collaboration.dto.SurveyCollaboratorResponseDto;
import com.formquid.collaboration.entity.CollaborationRequest;
import com.formquid.collaboration.entity.SurveyCollaborator;
import com.formquid.collaboration.repository.CollaborationRequestRepository;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
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

import java.util.List;

/**
 * Servicio de negocio para gestionar colaboraciones entre editores.
 *
 * Centraliza:
 * - creación de solicitudes de colaboración;
 * - aceptación y rechazo de invitaciones;
 * - consulta de solicitudes pendientes;
 * - consulta de colaboradores;
 * - eliminación de colaboradores.
 *
 * Las solicitudes aceptadas o rechazadas se eliminan del sistema,
 * por lo que CollaborationRequest representa siempre una solicitud pendiente.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollaborationService {

    private final CollaborationRequestRepository collaborationRequestRepository;
    private final SurveyCollaboratorRepository surveyCollaboratorRepository;
    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;
    private final SurveyService surveyService;
    private final NotificationService notificationService;

    /**
     * Crea una nueva solicitud de colaboración.
     *
     * Puede realizar la operación el propietario de la encuesta,
     * un administrador o un superadministrador.
     */
    @Transactional
    public CollaborationRequestResponseDto createRequest(
        Long surveyId,
        CreateCollaborationRequestDto request,
        String requesterEmail
    ) {
        User requester = getAuthenticatedUser(requesterEmail);
        Survey survey = getSurvey(surveyId);

        /*
         * SurveyService centraliza las reglas de autorización interna.
         * Los colaboradores normales no pueden administrar colaboradores.
         */
        surveyService.assertCanManageSurvey(
            survey,
            requester.getId(),
            requester.getRole()
        );

        User invitedEditor = resolveInvitedEditor(request);

        validateInvitedEditor(
            survey,
            invitedEditor,
            requester.getId()
        );

        validateRequestDoesNotExist(
            survey.getId(),
            invitedEditor.getId()
        );

        CollaborationRequest collaborationRequest =
            CollaborationRequest.builder()
                .survey(survey)
                .requestedBy(requester)
                .invitedEditor(invitedEditor)
                .message(cleanOptional(request.message()))
                .build();

        CollaborationRequest savedRequest =
            collaborationRequestRepository.save(
                collaborationRequest
            );

        /*
         * CollaborationService no construye notificaciones.
         * Delega ese detalle al servicio específico de notificaciones.
         */
        notificationService.notifyCollaborationInvite(
            invitedEditor.getId(),
            savedRequest.getId(),
            survey.getTitle()
        );

        return toRequestResponse(savedRequest);
    }

    /**
     * Obtiene las solicitudes pendientes de una encuesta.
     *
     * Solo pueden consultar esta información quienes puedan
     * administrar la encuesta.
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

        return collaborationRequestRepository
            .findBySurveyIdOrderByCreatedAtDesc(surveyId)
            .stream()
            .map(this::toRequestResponse)
            .toList();
    }

    /**
     * Obtiene las solicitudes recibidas por el usuario autenticado.
     */
    public List<CollaborationRequestResponseDto> getReceivedRequests(
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);

        return collaborationRequestRepository
            .findByInvitedEditorIdOrderByCreatedAtDesc(
                user.getId()
            )
            .stream()
            .map(this::toRequestResponse)
            .toList();
    }

    /**
     * Acepta una solicitud de colaboración.
     *
     * Solo el editor invitado puede aceptar su propia solicitud.
     * Al aceptarla se crea la colaboración y se elimina la solicitud pendiente.
     */
    @Transactional
    public SurveyCollaboratorResponseDto accept(
        Long requestId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        CollaborationRequest request = getRequest(requestId);

        validateInvitedUser(
            request,
            user.getId()
        );

        /*
         * La comprobación evita crear una segunda colaboración
         * si el usuario ya fue añadido por otra operación.
         */
        if (
            surveyCollaboratorRepository
                .existsBySurveyIdAndEditorId(
                    request.getSurvey().getId(),
                    user.getId()
                )
        ) {
            /*
             * La solicitud ya no tiene sentido si la colaboración existe.
             */
            collaborationRequestRepository.delete(request);

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El editor ya es colaborador de esta encuesta"
            );
        }

        SurveyCollaborator collaborator =
            SurveyCollaborator.builder()
                .survey(request.getSurvey())
                .editor(request.getInvitedEditor())
                .addedBy(request.getRequestedBy())
                .build();

        SurveyCollaborator savedCollaborator =
            surveyCollaboratorRepository.save(
                collaborator
            );

        /*
         * Las solicitudes resueltas no se conservan.
         */
        collaborationRequestRepository.delete(request);

        notificationService.notifyCollaborationAccepted(
            request.getRequestedBy().getId(),
            request.getSurvey().getId(),
            request.getInvitedEditor().getName(),
            request.getSurvey().getTitle()
        );

        return toCollaboratorResponse(
            savedCollaborator
        );
    }

    /**
     * Rechaza una solicitud de colaboración.
     *
     * Solo el editor invitado puede rechazarla.
     * Al resolverse, la solicitud se elimina.
     */
    @Transactional
    public CollaborationRequestResponseDto reject(
        Long requestId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        CollaborationRequest request = getRequest(requestId);

        validateInvitedUser(
            request,
            user.getId()
        );

        /*
         * Se construye la respuesta antes de eliminar la entidad.
         */
        CollaborationRequestResponseDto response =
            toRequestResponse(request);

        collaborationRequestRepository.delete(request);

        notificationService.notifyCollaborationRejected(
            request.getRequestedBy().getId(),
            request.getSurvey().getId(),
            request.getInvitedEditor().getName(),
            request.getSurvey().getTitle()
        );

        return response;
    }

    /**
     * Obtiene los colaboradores actuales de una encuesta.
     *
     * Puede consultar esta lista cualquier usuario con acceso interno
     * a la encuesta.
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

        /*
         * El repositorio utiliza EntityGraph para cargar survey,
         * editor y addedBy evitando consultas secundarias N+1.
         */
        return surveyCollaboratorRepository
            .findBySurveyId(surveyId)
            .stream()
            .map(this::toCollaboratorResponse)
            .toList();
    }

    /**
     * Elimina un colaborador de una encuesta.
     *
     * Puede hacerlo el propietario, un administrador o un superadministrador.
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

        validateOwnerCannotBeRemoved(
            survey,
            editorId
        );

        SurveyCollaborator collaborator =
            surveyCollaboratorRepository
                .findBySurveyIdAndEditorId(
                    surveyId,
                    editorId
                )
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El editor indicado no es colaborador de esta encuesta"
                    )
                );

        surveyCollaboratorRepository.delete(
            collaborator
        );

        notificationService.notifyCollaboratorRemoved(
            collaborator.getEditor().getId(),
            survey.getId(),
            survey.getTitle()
        );
    }

    /**
     * Recupera una encuesta o devuelve 404.
     */
    private Survey getSurvey(Long surveyId) {
        return surveyRepository
            .findById(surveyId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe la encuesta indicada"
                )
            );
    }

    /**
     * Recupera al usuario autenticado a partir del correo utilizado
     * como identidad en Spring Security.
     */
    private User getAuthenticatedUser(
        String email
    ) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return userRepository
            .findByEmailIgnoreCase(email.trim())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario autenticado no encontrado"
                )
            );
    }

    /**
     * Obtiene un usuario por identificador.
     */
    private User getUser(Long userId) {
        return userRepository
            .findById(userId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe el usuario indicado"
                )
            );
    }

    /**
     * Obtiene una solicitud de colaboración o devuelve 404.
     */
    private CollaborationRequest getRequest(
        Long requestId
    ) {
        return collaborationRequestRepository
            .findById(requestId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe la solicitud de colaboración indicada"
                )
            );
    }

    /**
     * Resuelve el editor invitado mediante id o correo electrónico.
     *
     * Debe proporcionarse exactamente uno de los dos identificadores.
     */
    private User resolveInvitedEditor(
        CreateCollaborationRequestDto request
    ) {
        boolean hasId =
            request.invitedEditorId() != null;

        boolean hasEmail =
            StringUtils.hasText(
                request.invitedEditorEmail()
            );

        if (hasId == hasEmail) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debes indicar el id del editor o su correo electrónico, pero no ambos"
            );
        }

        if (hasId) {
            return getUser(
                request.invitedEditorId()
            );
        }

        return userRepository
            .findByEmailIgnoreCase(
                request.invitedEditorEmail().trim()
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe un editor registrado con ese correo electrónico"
                )
            );
    }

    /**
     * Valida que el usuario seleccionado pueda incorporarse
     * como colaborador de la encuesta.
     */
    private void validateInvitedEditor(
        Survey survey,
        User invitedEditor,
        Long requesterId
    ) {
        if (
            invitedEditor.getRole()
                != UserRole.EDITOR
        ) {
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

        if (
            invitedEditor.getId()
                .equals(requesterId)
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No puedes invitarte a ti mismo como colaborador"
            );
        }

        if (
            survey.getOwner().getId()
                .equals(invitedEditor.getId())
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El propietario de la encuesta no puede añadirse como colaborador"
            );
        }

        /*
         * Se comprueba la existencia antes de persistir para proporcionar
         * un error de dominio claro y evitar depender de una restricción SQL.
         */
        if (
            surveyCollaboratorRepository
                .existsBySurveyIdAndEditorId(
                    survey.getId(),
                    invitedEditor.getId()
                )
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El editor ya es colaborador de esta encuesta"
            );
        }
    }

    /**
     * Evita crear más de una solicitud pendiente
     * para la misma encuesta y editor.
     */
    private void validateRequestDoesNotExist(
        Long surveyId,
        Long invitedEditorId
    ) {
        if (
            collaborationRequestRepository
                .existsBySurveyIdAndInvitedEditorId(
                    surveyId,
                    invitedEditorId
                )
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ya existe una solicitud pendiente para este editor"
            );
        }
    }

    /**
     * Comprueba que quien resuelve una solicitud sea
     * exactamente el editor invitado.
     */
    private void validateInvitedUser(
        CollaborationRequest request,
        Long userId
    ) {
        if (
            request.getInvitedEditor() == null
            || request.getInvitedEditor().getId() == null
            || !request.getInvitedEditor()
                .getId()
                .equals(userId)
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo el editor invitado puede resolver esta solicitud"
            );
        }
    }

    /**
     * Impide intentar eliminar al propietario como colaborador.
     */
    private void validateOwnerCannotBeRemoved(
        Survey survey,
        Long editorId
    ) {
        if (
            survey.getOwner() != null
            && survey.getOwner().getId() != null
            && survey.getOwner()
                .getId()
                .equals(editorId)
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El propietario de la encuesta no puede eliminarse como colaborador"
            );
        }
    }

    /**
     * Convierte una solicitud pendiente a DTO.
     */
    private CollaborationRequestResponseDto toRequestResponse(
        CollaborationRequest request
    ) {
        return new CollaborationRequestResponseDto(
            request.getId(),
            request.getSurvey().getId(),
            request.getSurvey().getTitle(),
            request.getRequestedBy().getId(),
            request.getRequestedBy().getName(),
            request.getInvitedEditor().getId(),
            request.getInvitedEditor().getName(),
            request.getInvitedEditor().getEmail(),
            request.getMessage(),
            request.getCreatedAt()
        );
    }

    /**
     * Convierte una colaboración activa a DTO.
     */
    private SurveyCollaboratorResponseDto toCollaboratorResponse(
        SurveyCollaborator collaborator
    ) {
        User addedBy =
            collaborator.getAddedBy();

        return new SurveyCollaboratorResponseDto(
            collaborator.getId(),
            collaborator.getSurvey().getId(),
            collaborator.getSurvey().getTitle(),
            collaborator.getEditor().getId(),
            collaborator.getEditor().getName(),
            collaborator.getEditor().getEmail(),
            addedBy != null
                ? addedBy.getId()
                : null,
            addedBy != null
                ? addedBy.getName()
                : null,
            collaborator.getAddedAt()
        );
    }

    /**
     * Limpia un texto opcional.
     */
    private String cleanOptional(
        String value
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
