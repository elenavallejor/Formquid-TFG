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
import com.formquid.collaboration.dto.CreateCollaborationRequestDto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {
    @ParameterizedTest
    @EnumSource(CollaborationRequestType.class)
    void requestAndResolve_routesInvitationsAndAccessRequests(CollaborationRequestType type) {
        User owner=User.builder().id(9L).name("Propietaria").email("owner@example.com").build();
        User editor=User.builder().id(10L).name("Editor").email("editor@example.com").build();
        Survey survey=Survey.builder().id(1L).owner(owner).title("Encuesta").build();
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        if(type==CollaborationRequestType.INVITATION)
            when(userRepository.findByEmail("editor@example.com")).thenReturn(Optional.of(editor));
        else when(userRepository.findById(10L)).thenReturn(Optional.of(editor));
        when(requestRepository.save(any())).thenAnswer(i -> { CollaborationRequest r=i.getArgument(0); r.setId(7L); return r; });
        long actor=type==CollaborationRequestType.INVITATION?9L:10L;
        var result=collaborationService.request(1L,actor,new CreateCollaborationRequestDto(10L," EDITOR@example.com ",null,type," Mensaje "));
        assertThat(result.message()).isEqualTo("Mensaje");
        assertThat(result.permission()).isEqualTo(CollaborationPermission.EDIT);
        var captor=org.mockito.ArgumentCaptor.forClass(CollaborationRequest.class);
        verify(requestRepository).save(captor.capture());
        CollaborationRequest saved=captor.getValue();
        when(requestRepository.findById(7L)).thenReturn(Optional.of(saved));
        long resolver=type==CollaborationRequestType.INVITATION?10L:9L;
        assertThatThrownBy(() -> collaborationService.accept(7L,99L)).hasMessageContaining(type==CollaborationRequestType.INVITATION?"invitado":"propietario");
        when(collaboratorRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(collaborationService.accept(7L,resolver).permission()).isEqualTo(CollaborationPermission.EDIT);
        collaborationService.reject(7L,resolver);
        verify(requestRepository,times(2)).delete(saved);
        verify(notificationService).createSurveyNotification(eq(type==CollaborationRequestType.INVITATION?owner:editor),eq(NotificationType.COLLABORATION_ACCEPTED),anyString(),anyString(),eq(survey));
    }

    @Test
    void publicationRequest_checksEditingPermissionAndQuestionsAndNotifiesOwner() {
        User owner=User.builder().id(9L).build();
        User editor=User.builder().id(10L).name("Editor").build();
        Survey survey=Survey.builder().id(1L).owner(owner).title("Encuesta").build();
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        assertThatThrownBy(() -> collaborationService.requestPublication(1L,9L,null)).hasMessageContaining("directamente");
        assertThatThrownBy(() -> collaborationService.requestPublication(1L,10L,null)).hasMessageContaining("permiso de edición");
        when(collaboratorRepository.existsByIdSurveyIdAndIdUserIdAndPermission(1L,10L,CollaborationPermission.EDIT)).thenReturn(true);
        assertThatThrownBy(() -> collaborationService.requestPublication(1L,10L,null)).hasMessageContaining("al menos una pregunta");
        when(contentService.hasQuestion(anyList())).thenReturn(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(editor));
        when(publicationRequestRepository.save(any())).thenAnswer(i -> { PublicationRequest r=i.getArgument(0); r.setId(7L); return r; });
        assertThat(collaborationService.requestPublication(1L,10L," Publicar ")).isEqualTo(7L);
        verify(publicationRequestRepository).save(argThat(r -> r.getMessage().equals("Publicar") && r.getSurvey()==survey));
        when(publicationRequestRepository.existsBySurveyId(1L)).thenReturn(true);
        assertThatThrownBy(() -> collaborationService.requestPublication(1L,10L,null)).hasMessageContaining("pendiente");
        PublicationRequest request=PublicationRequest.builder().id(7L).survey(survey).requestedBy(editor).build();
        when(publicationRequestRepository.findById(7L)).thenReturn(Optional.of(request));
        collaborationService.rejectPublication(7L,9L);
        verify(publicationRequestRepository).delete(request);
    }

    @Test
    void findLists_mapsPendingRequestsAndCollaborators() {
        User owner=User.builder().id(9L).build(); User editor=User.builder().id(10L).build();
        Survey survey=Survey.builder().id(1L).owner(owner).build();
        var c=SurveyCollaborator.builder().survey(survey).user(editor).permission(CollaborationPermission.EDIT).build();
        when(collaboratorRepository.findByIdSurveyId(1L)).thenReturn(List.of(c));
        when(requestRepository.findBySurveyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(request(7L,survey,editor,CollaborationRequestType.INVITATION)));
        when(publicationRequestRepository.findBySurveyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(PublicationRequest.builder().id(8L).survey(survey).requestedBy(editor).build()));
        assertThat(collaborationService.findCollaborators(1L)).hasSize(1);
        assertThat(collaborationService.findRequests(1L)).singleElement().satisfies(r -> assertThat(r.id()).isEqualTo(7L));
        assertThat(collaborationService.findPublicationRequests(1L)).singleElement().satisfies(r -> assertThat(r.id()).isEqualTo(8L));
    }

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
