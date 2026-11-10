package com.formquid.notification.service;

import com.formquid.block.repository.BlockRepository;
import com.formquid.collaboration.repository.CollaborationRequestRepository;
import com.formquid.collaboration.repository.PublicationRequestRepository;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.elementrequest.repository.ElementRequestRepository;
import com.formquid.notification.entity.Notification;
import com.formquid.notification.entity.NotificationTargetType;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.registration.repository.RegistrationRequestRepository;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.template.repository.TemplateRepository;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.formquid.collaboration.entity.*;
import com.formquid.block.entity.*;
import com.formquid.question.entity.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDestinationServiceTest {
    @ParameterizedTest
    @EnumSource(NotificationTargetType.class)
    void resolve_currentTargets_returnTheirDestination(NotificationTargetType type) {
        User user=User.builder().id(3L).role(UserRole.SUPERADMIN).build();
        Survey survey=Survey.builder().id(8L).owner(user).build();
        Notification notification=Notification.builder().targetType(type).targetId(9L).build();
        when(notifications.getOwnedNotification(21L,3L)).thenReturn(notification);
        String expected=switch(type) {
            case SURVEY -> {
                when(surveys.findById(9L)).thenReturn(Optional.of(survey));
                yield "/encuestas/9/editar?vista=detalles";
            }
            case COLLABORATION_REQUEST -> {
                user.setRole(UserRole.EDITOR);
                when(collaborationRequests.findById(9L)).thenReturn(Optional.of(CollaborationRequest.builder().survey(survey).user(user).build()));
                yield "/colaboraciones";
            }
            case PUBLICATION_REQUEST -> {
                when(publicationRequests.findById(9L)).thenReturn(Optional.of(PublicationRequest.builder().survey(survey).build()));
                yield "/encuestas/8/editar?seccion=colaboraciones";
            }
            case REGISTRATION_REQUEST -> {
                when(registrationRequests.existsById(9L)).thenReturn(true);
                yield "/admin/solicitudes-registro";
            }
            case ELEMENT_REQUEST -> {
                when(elementRequests.existsById(9L)).thenReturn(true);
                yield "/admin/solicitudes-catalogo";
            }
            case USER -> {
                when(users.existsById(9L)).thenReturn(true);
                yield "/superadmin/usuarios";
            }
            case QUESTION -> {
                when(questions.findById(9L)).thenReturn(Optional.of(Question.builder().scope(QuestionScope.CATALOG_FIXED).build()));
                yield "/catalogo/detalles/pregunta/9";
            }
            case BLOCK -> {
                when(blocks.findById(9L)).thenReturn(Optional.of(Block.builder().scope(BlockScope.CATALOG).build()));
                yield "/catalogo/detalles/bloque/9";
            }
            case TEMPLATE -> {
                when(templates.existsById(9L)).thenReturn(true);
                yield "/catalogo/detalles/plantilla/9";
            }
            case NONE -> null;
        };
        assertThat(destinationService.resolve(21L,user)).isEqualTo(expected);
        notification.setTargetId(null);
        assertThat(destinationService.resolve(21L,user)).isNull();
    }

    @Mock private NotificationService notifications;
    @Mock private SurveyRepository surveys;
    @Mock private SurveyCollaboratorRepository collaborators;
    @Mock private CollaborationRequestRepository collaborationRequests;
    @Mock private PublicationRequestRepository publicationRequests;
    @Mock private RegistrationRequestRepository registrationRequests;
    @Mock private ElementRequestRepository elementRequests;
    @Mock private UserRepository users;
    @Mock private QuestionRepository questions;
    @Mock private BlockRepository blocks;
    @Mock private TemplateRepository templates;

    @InjectMocks
    private NotificationDestinationService destinationService;

    @Test
    void resolve_hidesSurveyThatEditorCannotAccess() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        User editor = User.builder().id(2L).role(UserRole.EDITOR).build();
        Notification notification = Notification.builder()
            .targetType(NotificationTargetType.SURVEY)
            .targetId(8L)
            .build();
        Survey survey = Survey.builder().id(8L).owner(owner).build();
        when(notifications.getOwnedNotification(20L, 2L)).thenReturn(notification);
        when(surveys.findById(8L)).thenReturn(Optional.of(survey));

        assertThat(destinationService.resolve(20L, editor)).isNull();
    }

    @Test
    void resolve_returnsRegistrationQueueOnlyToAdministrators() {
        User admin = User.builder().id(3L).role(UserRole.ADMIN).build();
        Notification notification = Notification.builder()
            .targetType(NotificationTargetType.REGISTRATION_REQUEST)
            .targetId(9L)
            .build();
        when(notifications.getOwnedNotification(21L, 3L)).thenReturn(notification);
        when(registrationRequests.existsById(9L)).thenReturn(true);

        assertThat(destinationService.resolve(21L, admin)).isEqualTo("/admin/solicitudes-registro");
    }
}
