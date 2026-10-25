package com.formquid.participation.service;

import com.formquid.block.dto.BlockItemIdDto;
import com.formquid.block.entity.Block;
import com.formquid.block.repository.BlockRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.content.dto.BlockContentItemRowDto;
import com.formquid.content.dto.ContentItemRowDto;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.participation.dto.ParticipationResponseDto;
import com.formquid.participation.dto.ParticipationSummaryResponseDto;
import com.formquid.participation.dto.SubmitAnswerRequestDto;
import com.formquid.participation.dto.SubmitParticipationRequestDto;
import com.formquid.participation.entity.Answer;
import com.formquid.participation.entity.Participation;
import com.formquid.participation.repository.AnswerRepository;
import com.formquid.participation.repository.AnswerOptionRepository;
import com.formquid.participation.repository.MatrixAnswerValueRepository;
import com.formquid.participation.repository.ParticipationRepository;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionSettings;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.repository.MatrixRowRepository;
import com.formquid.question.repository.QuestionOptionRepository;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private ContentItemRepository contentItemRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionOptionRepository optionRepository;
    @Mock
    private MatrixRowRepository matrixRowRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private AnswerOptionRepository answerOptionRepository;
    @Mock
    private MatrixAnswerValueRepository matrixAnswerValueRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SurveyService surveyService;

    @InjectMocks
    private ParticipationService participationService;

    @Test
    void submitAcceptsTextAndQuestionInsideSameBlock() {
        Survey survey = Survey.builder()
            .id(1L)
            .publicCode("ABCDEF234567")
            .lifecycleStatus(SurveyLifecycle.OPEN)
            .accessMode(SurveyAccessMode.PUBLIC)
            .build();
        ContentItem blockItem = ContentItem.builder()
            .id(10L)
            .itemType(ContentItemType.BLOCK)
            .position(1)
            .build();
        Block block = Block.builder().id(20L).parentItem(blockItem).build();
        ContentItem questionItem = ContentItem.builder()
            .id(31L)
            .parentBlock(block)
            .itemType(ContentItemType.QUESTION)
            .position(2)
            .required(true)
            .build();
        Question question = Question.builder()
            .id(40L)
            .parentItem(questionItem)
            .questionType(QuestionType.TEXT)
            .statement("Pregunta")
            .build();
        when(surveyRepository.findByPublicCode("ABCDEF234567")).thenReturn(Optional.of(survey));
        when(contentItemRepository.findSurveyContentRows(1L))
            .thenReturn(List.of(new ContentItemRowDto(10L, ContentItemType.BLOCK, 1, null, null)));
        when(blockRepository.findIdsByParentItemIds(Set.of(10L)))
            .thenReturn(List.of(new BlockItemIdDto(10L, 20L)));
        when(contentItemRepository.findBlockContentRows(Set.of(20L))).thenReturn(List.of(
            new BlockContentItemRowDto(20L, 30L, ContentItemType.TEXT, 1, null, "Introducción"),
            new BlockContentItemRowDto(20L, 31L, ContentItemType.QUESTION, 2, true, null)
        ));
        when(questionRepository.findByParentItemIdsForStatistics(Set.of(31L)))
            .thenReturn(List.of(question));
        when(participationRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Participation.class))).thenAnswer(invocation -> {
            Participation participation = invocation.getArgument(0);
            participation.setId(50L);
            participation.setSubmittedAt(Instant.parse("2026-09-15T10:00:00Z"));
            return participation;
        });
        when(answerRepository.save(org.mockito.ArgumentMatchers.any(Answer.class))).thenAnswer(invocation -> {
            Answer answer = invocation.getArgument(0);
            answer.setId(60L);
            return answer;
        });

        ParticipationResponseDto response = participationService.submit(
            "abcdef234567",
            new SubmitParticipationRequestDto(null, List.of(
                new SubmitAnswerRequestDto(40L, "Respuesta", null, null, null, null)
            )),
            null,
            "respondent-token-hash"
        );

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.answers()).hasSize(1);
        assertThat(response.answers().getFirst().questionId()).isEqualTo(40L);
        verify(answerRepository, times(1)).save(org.mockito.ArgumentMatchers.any(Answer.class));
    }

    @Test
    void submit_withMultipleQuestions_loadsThemInOneQuery() {
        Survey survey = Survey.builder()
            .id(1L)
            .publicCode("ABCDEF234567")
            .lifecycleStatus(SurveyLifecycle.OPEN)
            .accessMode(SurveyAccessMode.PUBLIC)
            .build();
        ContentItem firstItem = ContentItem.builder()
            .id(11L).itemType(ContentItemType.QUESTION).required(false).build();
        ContentItem secondItem = ContentItem.builder()
            .id(12L).itemType(ContentItemType.QUESTION).required(false).build();
        Question firstQuestion = Question.builder().id(21L).parentItem(firstItem).build();
        Question secondQuestion = Question.builder().id(22L).parentItem(secondItem).build();
        when(surveyRepository.findByPublicCode("ABCDEF234567")).thenReturn(Optional.of(survey));
        when(contentItemRepository.findSurveyContentRows(1L))
            .thenReturn(List.of(
                new ContentItemRowDto(11L, ContentItemType.QUESTION, 1, false, null),
                new ContentItemRowDto(12L, ContentItemType.QUESTION, 2, false, null)
            ));
        when(questionRepository.findByParentItemIdsForStatistics(Set.of(11L, 12L)))
            .thenReturn(List.of(firstQuestion, secondQuestion));
        when(participationRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Participation.class)))
            .thenReturn(Participation.builder().id(50L).survey(survey)
                .submittedAt(Instant.parse("2026-09-15T10:00:00Z")).build());

        ParticipationResponseDto response = participationService.submit(
            "ABCDEF234567",
            new SubmitParticipationRequestDto(null, List.of()),
            null,
            "respondent-token-hash"
        );

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.answers()).isEmpty();
        verify(questionRepository).findByParentItemIdsForStatistics(Set.of(11L, 12L));
    }

    @Test
    void submitRejectsASecondAnonymousParticipation() {
        Survey survey = Survey.builder()
            .id(1L)
            .publicCode("ABCDEF234567")
            .lifecycleStatus(SurveyLifecycle.OPEN)
            .accessMode(SurveyAccessMode.PUBLIC)
            .build();
        when(surveyRepository.findByPublicCode("ABCDEF234567")).thenReturn(Optional.of(survey));
        when(participationRepository.existsBySurveyIdAndRespondentTokenHash(1L, "respondent-token-hash"))
            .thenReturn(true);

        assertThatThrownBy(() -> participationService.submit(
            "ABCDEF234567",
            new SubmitParticipationRequestDto(null, List.of()),
            null,
            "respondent-token-hash"
        ))
            .isInstanceOf(ApiException.class)
            .hasMessage("Ya respondiste esta encuesta. Solo se puede enviar una vez.");

        verifyNoInteractions(contentItemRepository, answerRepository);
    }

    @Test
    void findParticipationPage_returnsStableAnonymousPageAfterCheckingAccess() {
        User actor = User.builder().id(8L).build();
        Survey survey = Survey.builder().id(3L).title("Encuesta").build();
        PageRequest pageable = PageRequest.of(
            1,
            10,
            Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id"))
        );
        var summary = new ParticipationSummaryResponseDto(
            21L,
            Instant.parse("2026-09-15T10:00:00Z")
        );

        when(surveyRepository.findById(3L)).thenReturn(Optional.of(survey));
        when(participationRepository.findSummariesBySurveyId(3L, pageable))
            .thenReturn(new PageImpl<>(List.of(summary), pageable, 11));

        var response = participationService.findParticipationPage(3L, actor, 1, 10);

        assertThat(response.content()).containsExactly(summary);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(11);
        verify(surveyService).ensureCanViewResults(survey, actor);
    }

    @Test
    void getParticipationDetail_returnsAnswersWithoutRespondentIdentity() {
        User actor = User.builder().id(8L).build();
        Survey survey = Survey.builder().id(3L).title("Encuesta").build();
        Participation participation = Participation.builder()
            .id(21L)
            .survey(survey)
            .submittedAt(Instant.parse("2026-09-15T10:00:00Z"))
            .respondentUser(User.builder().id(99L).build())
            .respondentTokenHash("secret-token-hash")
            .build();
        Question question = Question.builder()
            .id(40L)
            .statement("Respuesta libre")
            .questionType(QuestionType.TEXT)
            .build();
        Answer answer = Answer.builder()
            .id(60L)
            .participation(participation)
            .question(question)
            .textValue("Contenido anónimo")
            .build();
        Question booleanQuestion = Question.builder()
            .id(41L)
            .statement("¿Está de acuerdo?")
            .questionType(QuestionType.BOOLEAN)
            .build();
        booleanQuestion.setSettings(QuestionSettings.builder()
            .trueLabel("De acuerdo")
            .falseLabel("En desacuerdo")
            .build());
        Answer booleanAnswer = Answer.builder()
            .id(61L)
            .participation(participation)
            .question(booleanQuestion)
            .booleanValue(true)
            .build();

        when(surveyRepository.findById(3L)).thenReturn(Optional.of(survey));
        when(participationRepository.findDetailByIdAndSurveyId(21L, 3L))
            .thenReturn(Optional.of(participation));
        when(answerRepository.findDetailsByParticipationId(21L)).thenReturn(List.of(answer, booleanAnswer));
        when(answerOptionRepository.findByAnswerIdsWithOption(List.of(60L, 61L))).thenReturn(List.of());
        when(matrixAnswerValueRepository.findByAnswerIdsWithRow(List.of(60L, 61L))).thenReturn(List.of());

        var response = participationService.getParticipationDetail(3L, 21L, actor);

        assertThat(response.id()).isEqualTo(21L);
        assertThat(response.surveyTitle()).isEqualTo("Encuesta");
        assertThat(response.answers()).hasSize(2);
        assertThat(response.answers().getFirst()).satisfies(result -> {
            assertThat(result.statement()).isEqualTo("Respuesta libre");
            assertThat(result.textValue()).isEqualTo("Contenido anónimo");
        });
        assertThat(response.answers().get(1)).satisfies(result -> {
            assertThat(result.booleanValue()).isTrue();
            assertThat(result.trueLabel()).isEqualTo("De acuerdo");
            assertThat(result.falseLabel()).isEqualTo("En desacuerdo");
        });
        verify(surveyService).ensureCanViewResults(survey, actor);
    }
}
