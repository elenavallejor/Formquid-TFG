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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDestinationServiceTest {

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
