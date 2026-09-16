package com.formquid.survey.service;

import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.content.service.ContentService;
import com.formquid.notification.service.NotificationService;
import com.formquid.participation.repository.ParticipationRepository;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyFontFamily;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.template.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private SurveyCollaboratorRepository collaboratorRepository;
    @Mock
    private ContentItemRepository contentItemRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private ContentService contentService;
    @Mock
    private TemplateService templateService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SurveyService surveyService;

    @Test
    void surveyUsesTimesNewRomanByDefault() {
        Survey survey = Survey.builder().build();

        survey.normalize();

        assertThat(survey.getFontFamily()).isEqualTo(SurveyFontFamily.TIMES_NEW_ROMAN);
    }

    @Test
    void openAfterPublicationApprovalGeneratesPublicCode() {
        Survey survey = Survey.builder()
            .id(1L)
            .lifecycleStatus(SurveyLifecycle.DRAFT)
            .build();
        List<ContentItem> content = List.of(ContentItem.builder()
            .id(10L)
            .itemType(ContentItemType.QUESTION)
            .position(1)
            .required(false)
            .build());

        when(contentItemRepository.findBySurveyIdOrderByPositionAsc(1L)).thenReturn(content);
        when(contentService.hasQuestion(content)).thenReturn(true);
        when(surveyRepository.existsByPublicCode(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        surveyService.openAfterPublicationApproval(survey);

        assertThat(survey.getLifecycleStatus()).isEqualTo(SurveyLifecycle.OPEN);
        assertThat(survey.getPublicCode()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ2-9]{12}");
    }
}
