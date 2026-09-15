package com.formquid.collaboration.service;

import com.formquid.collaboration.entity.PublicationRequest;
import com.formquid.collaboration.repository.CollaborationRequestRepository;
import com.formquid.collaboration.repository.PublicationRequestRepository;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.content.service.ContentService;
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
}
