package com.formquid.collaboration.service;

import com.formquid.collaboration.entity.CollaborationPermission;
import com.formquid.collaboration.entity.CollaborationRequest;
import com.formquid.collaboration.entity.CollaborationRequestType;
import com.formquid.collaboration.entity.PublicationRequest;
import com.formquid.collaboration.entity.SurveyCollaborator;
import com.formquid.collaboration.entity.SurveyCollaboratorId;
import com.formquid.collaboration.repository.CollaborationRequestRepository;
import com.formquid.collaboration.repository.PublicationRequestRepository;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.content.service.ContentService;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.service.NotificationService;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SurveyCollaboratorRepository collaboratorRepository;
    @Mock
    private CollaborationRequestRepository requestRepository;
    @Mock
    private PublicationRequestRepository publicationRequestRepository;
    @Mock
    private ContentItemRepository contentItemRepository;
    @Mock
    private ContentService contentService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SurveyService surveyService;

    @InjectMocks
    private CollaborationService collaborationService;

    @Test
    void acceptPublicationUsesValidatedSurveyTransition() {
        User owner = User.builder().id(9L).build();
        User requester = User.builder().id(10L).build();
        Survey survey = Survey.builder().id(1L).owner(owner).title("Encuesta").build();
        PublicationRequest request = PublicationRequest.builder()
            .id(7L)
            .survey(survey)
            .requestedBy(requester)
            .build();
        List<ContentItem> content = List.of();

        when(publicationRequestRepository.findById(7L)).thenReturn(Optional.of(request));
        when(contentItemRepository.findBySurveyIdOrderByPositionAsc(1L)).thenReturn(content);
        when(contentService.hasQuestion(content)).thenReturn(true);

        collaborationService.acceptPublication(7L, 9L);

        verify(surveyService).openAfterPublicationApproval(survey);
        verify(publicationRequestRepository).delete(request);
    }

    @Test
    void findRequestsForUserReturnsAllFourManagedCategories() {
        User currentUser = User.builder().id(9L).name("Elena").email("elena@example.com").build();
        User otherUser = User.builder().id(10L).name("Ana").email("ana@example.com").build();
        Survey ownedSurvey = Survey.builder().id(1L).owner(currentUser).title("Encuesta propia").build();
        Survey externalSurvey = Survey.builder().id(2L).owner(otherUser).title("Encuesta externa").build();

        CollaborationRequest sentRequest = request(1L, externalSurvey, currentUser, CollaborationRequestType.ACCESS_REQUEST);
        CollaborationRequest receivedRequest = request(2L, ownedSurvey, otherUser, CollaborationRequestType.ACCESS_REQUEST);
        CollaborationRequest sentInvitation = request(3L, ownedSurvey, otherUser, CollaborationRequestType.INVITATION);
        CollaborationRequest receivedInvitation = request(4L, externalSurvey, currentUser, CollaborationRequestType.INVITATION);

        when(requestRepository.findRelatedToUser(9L)).thenReturn(List.of(
            sentRequest,
            receivedRequest,
            sentInvitation,
            receivedInvitation
        ));

        var result = collaborationService.findRequestsForUser(9L);

        assertEquals(List.of(1L, 2L, 3L, 4L), result.stream().map(request -> request.id()).toList());
        assertEquals("Encuesta externa", result.getFirst().surveyTitle());
        assertEquals("elena@example.com", result.getFirst().userEmail());
    }

    @Test
    void updateCollaboratorPermissionChangesTheStoredPermission() {
        User owner = User.builder().id(9L).build();
        User editor = User.builder().id(10L).build();
        Survey survey = Survey.builder().id(1L).owner(owner).build();
        SurveyCollaboratorId collaboratorId = new SurveyCollaboratorId(1L, 10L);
        SurveyCollaborator collaborator = SurveyCollaborator.builder()
            .id(collaboratorId)
            .survey(survey)
            .user(editor)
            .permission(CollaborationPermission.VIEW)
            .build();

        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(collaboratorRepository.findById(collaboratorId)).thenReturn(Optional.of(collaborator));
        when(collaboratorRepository.save(collaborator)).thenReturn(collaborator);

        var result = collaborationService.updateCollaboratorPermission(
            1L,
            10L,
            9L,
            CollaborationPermission.EDIT
        );

        assertEquals(CollaborationPermission.EDIT, result.permission());
        verify(collaboratorRepository).save(collaborator);
    }

    @Test
    void removeCollaboratorNotifiesTheRemovedEditor() {
        User owner = User.builder().id(9L).build();
        User editor = User.builder().id(10L).build();
        Survey survey = Survey.builder().id(1L).owner(owner).title("Encuesta").build();
        SurveyCollaboratorId collaboratorId = new SurveyCollaboratorId(1L, 10L);
        SurveyCollaborator collaborator = SurveyCollaborator.builder()
            .id(collaboratorId)
            .survey(survey)
            .user(editor)
            .build();

        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(collaboratorRepository.findById(collaboratorId)).thenReturn(Optional.of(collaborator));

        collaborationService.removeCollaborator(1L, 10L, 9L);

        verify(collaboratorRepository).delete(collaborator);
        verify(notificationService).createSurveyNotification(
            editor,
            NotificationType.COLLABORATION_REVOKED,
            "Colaboración revocada",
            "Tu colaboración en Encuesta ha sido revocada",
            survey
        );
    }

    private CollaborationRequest request(
        Long id,
        Survey survey,
        User user,
        CollaborationRequestType type
    ) {
        return CollaborationRequest.builder()
            .id(id)
            .survey(survey)
            .user(user)
            .requestType(type)
            .permission(CollaborationPermission.EDIT)
            .build();
    }
}
