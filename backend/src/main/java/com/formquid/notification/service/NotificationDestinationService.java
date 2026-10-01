package com.formquid.notification.service;

import com.formquid.block.entity.BlockScope;
import com.formquid.block.repository.BlockRepository;
import com.formquid.collaboration.repository.CollaborationRequestRepository;
import com.formquid.collaboration.repository.PublicationRequestRepository;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.elementrequest.repository.ElementRequestRepository;
import com.formquid.question.entity.QuestionScope;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.registration.repository.RegistrationRequestRepository;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.template.repository.TemplateRepository;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resuelve enlaces usando la existencia y los permisos actuales, nunca el texto histórico. */
@Service
@RequiredArgsConstructor
public class NotificationDestinationService {
    private final NotificationService notifications;
    private final SurveyRepository surveys;
    private final SurveyCollaboratorRepository collaborators;
    private final CollaborationRequestRepository collaborationRequests;
    private final PublicationRequestRepository publicationRequests;
    private final RegistrationRequestRepository registrationRequests;
    private final ElementRequestRepository elementRequests;
    private final UserRepository users;
    private final QuestionRepository questions;
    private final BlockRepository blocks;
    private final TemplateRepository templates;

    @Transactional(readOnly = true)
    public String resolve(Long notificationId, User user) {
        var notification = notifications.getOwnedNotification(notificationId, user.getId());
        Long id = notification.getTargetId();
        if (id == null) return null;
        boolean admin = user.getRole() != UserRole.EDITOR;
        return switch (notification.getTargetType()) {
            case SURVEY -> surveys.findById(id).filter(s -> canAccess(s, user))
                .map(s -> "/encuestas/" + id + "/editar?vista=detalles").orElse(null);
            case COLLABORATION_REQUEST -> collaborationRequests.findById(id)
                .filter(r -> user.getRole() == UserRole.EDITOR
                    && (r.getUser().getId().equals(user.getId()) || r.getSurvey().getOwner().getId().equals(user.getId())))
                .map(r -> "/colaboraciones").orElse(null);
            case PUBLICATION_REQUEST -> publicationRequests.findById(id)
                .filter(r -> r.getSurvey().getOwner().getId().equals(user.getId()))
                .map(r -> "/encuestas/" + r.getSurvey().getId() + "/editar?seccion=colaboraciones").orElse(null);
            case REGISTRATION_REQUEST -> admin && registrationRequests.existsById(id) ? "/admin/solicitudes-registro" : null;
            case ELEMENT_REQUEST -> admin && elementRequests.existsById(id) ? "/admin/solicitudes-catalogo" : null;
            case USER -> !users.existsById(id) ? null : id.equals(user.getId()) ? "/perfil"
                : user.getRole() == UserRole.SUPERADMIN ? "/superadmin/usuarios" : null;
            case QUESTION -> questions.findById(id).filter(q -> q.getScope() == QuestionScope.CATALOG_FIXED)
                .map(q -> "/catalogo/detalles/pregunta/" + id).orElse(null);
            case BLOCK -> blocks.findById(id).filter(b -> b.getScope() == BlockScope.CATALOG)
                .map(b -> "/catalogo/detalles/bloque/" + id).orElse(null);
            case TEMPLATE -> templates.existsById(id) ? "/catalogo/detalles/plantilla/" + id : null;
            case NONE -> null;
        };
    }

    private boolean canAccess(Survey survey, User user) {
        return user.getRole() != UserRole.EDITOR || survey.getOwner().getId().equals(user.getId())
            || collaborators.existsByIdSurveyIdAndIdUserId(survey.getId(), user.getId());
    }
}
