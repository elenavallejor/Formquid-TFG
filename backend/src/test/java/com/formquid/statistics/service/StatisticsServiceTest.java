package com.formquid.statistics.service;

import com.formquid.block.repository.BlockRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.participation.entity.Answer;
import com.formquid.participation.entity.Participation;
import com.formquid.participation.repository.AnswerOptionRepository;
import com.formquid.participation.repository.AnswerRepository;
import com.formquid.participation.repository.MatrixAnswerValueRepository;
import com.formquid.participation.repository.ParticipationRepository;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionSettings;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock private SurveyRepository surveyRepository;
    @Mock private ParticipationRepository participationRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private AnswerOptionRepository answerOptionRepository;
    @Mock private MatrixAnswerValueRepository matrixAnswerValueRepository;
    @Mock private ContentItemRepository contentItemRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private SurveyService surveyService;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    void getSurveyStatistics_calculatesNumericAndEvaluationScores() {
        User user = User.builder().id(4L).build();
        Survey survey = Survey.builder().id(1L).build();
        ContentItem item = ContentItem.builder()
            .id(10L).itemType(ContentItemType.QUESTION).position(1).required(false).build();
        Question question = Question.builder()
            .id(20L)
            .statement("Valoración")
            .questionType(QuestionType.NUMBER)
            .evaluationDirection(EvaluationDirection.POSITIVE)
            .evaluationWeight(BigDecimal.ONE)
            .build();
        question.setSettings(QuestionSettings.builder()
            .minValue(BigDecimal.ZERO)
            .maxValue(BigDecimal.TEN)
            .build());
        var first = Participation.builder().submittedAt(Instant.parse("2026-01-01T10:00:00Z")).build();
        var last = Participation.builder().submittedAt(Instant.parse("2026-01-02T10:00:00Z")).build();
        var answers = List.of(
            Answer.builder().numericValue(new BigDecimal("2")).build(),
            Answer.builder().numericValue(new BigDecimal("8")).build()
        );
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(participationRepository.findBySurveyIdOrderBySubmittedAtAsc(1L)).thenReturn(List.of(first, last));
        when(contentItemRepository.findBySurveyIdOrderByPositionAsc(1L)).thenReturn(List.of(item));
        when(questionRepository.findByParentItemId(10L)).thenReturn(Optional.of(question));
        when(answerRepository.findByQuestionId(20L)).thenReturn(answers);
        when(answerOptionRepository.countOptionsByQuestion(20L)).thenReturn(List.of());

        var response = statisticsService.getSurveyStatistics(1L, user);

        assertThat(response.participations()).isEqualTo(2);
        assertThat(response.firstParticipation()).isEqualTo(first.getSubmittedAt());
        assertThat(response.lastParticipation()).isEqualTo(last.getSubmittedAt());
        assertThat(response.globalEvaluationScore()).isEqualTo(0.0);
        assertThat(response.questions()).singleElement().satisfies(statistics -> {
            assertThat(statistics.answers()).isEqualTo(2);
            assertThat(statistics.omissions()).isZero();
            assertThat(statistics.responseRate()).isEqualTo(100.0);
            assertThat(statistics.numericStatistics().average()).isEqualTo(5.0);
            assertThat(statistics.numericStatistics().median()).isEqualTo(5.0);
            assertThat(statistics.numericStatistics().standardDeviation()).isEqualTo(3.0);
            assertThat(statistics.normalizedScore()).isEqualTo(50.0);
            assertThat(statistics.evaluationScore()).isEqualTo(0.0);
        });
        verify(surveyService).ensureCanViewResults(survey, user);
    }

    @Test
    void getSurveyStatistics_calculatesScaleValuePercentages() {
        User user = User.builder().id(4L).build();
        Survey survey = Survey.builder().id(1L).build();
        ContentItem item = ContentItem.builder()
            .id(10L).itemType(ContentItemType.QUESTION).position(1).required(false).build();
        Question question = Question.builder()
            .id(20L)
            .statement("Satisfacción")
            .questionType(QuestionType.SCALE)
            .evaluationDirection(EvaluationDirection.NEUTRAL)
            .build();
        question.setSettings(QuestionSettings.builder()
            .minValue(BigDecimal.ONE)
            .maxValue(new BigDecimal("5"))
            .build());
        var participations = List.of(
            Participation.builder().submittedAt(Instant.parse("2026-01-01T10:00:00Z")).build(),
            Participation.builder().submittedAt(Instant.parse("2026-01-02T10:00:00Z")).build(),
            Participation.builder().submittedAt(Instant.parse("2026-01-03T10:00:00Z")).build()
        );
        var answers = List.of(
            Answer.builder().numericValue(BigDecimal.ONE).build(),
            Answer.builder().numericValue(BigDecimal.ONE).build(),
            Answer.builder().numericValue(new BigDecimal("5")).build()
        );
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(participationRepository.findBySurveyIdOrderBySubmittedAtAsc(1L)).thenReturn(participations);
        when(contentItemRepository.findBySurveyIdOrderByPositionAsc(1L)).thenReturn(List.of(item));
        when(questionRepository.findByParentItemId(10L)).thenReturn(Optional.of(question));
        when(answerRepository.findByQuestionId(20L)).thenReturn(answers);
        when(answerOptionRepository.countOptionsByQuestion(20L)).thenReturn(List.of());

        var response = statisticsService.getSurveyStatistics(1L, user);

        assertThat(response.questions()).singleElement().satisfies(statistics ->
            assertThat(statistics.valueDistribution())
                .extracting("value", "count", "percentage")
                .containsExactly(
                    org.assertj.core.groups.Tuple.tuple(BigDecimal.ONE, 2L, 200.0 / 3.0),
                    org.assertj.core.groups.Tuple.tuple(new BigDecimal("2"), 0L, 0.0),
                    org.assertj.core.groups.Tuple.tuple(new BigDecimal("3"), 0L, 0.0),
                    org.assertj.core.groups.Tuple.tuple(new BigDecimal("4"), 0L, 0.0),
                    org.assertj.core.groups.Tuple.tuple(new BigDecimal("5"), 1L, 100.0 / 3.0)
                )
        );
    }

    @Test
    void getQuestionStatistics_rejectsQuestionOutsideSurvey() {
        User user = User.builder().id(4L).build();
        Survey survey = Survey.builder().id(1L).build();
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(contentItemRepository.findBySurveyIdOrderByPositionAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> statisticsService.getQuestionStatistics(1L, 99L, user))
            .isInstanceOf(ApiException.class)
            .hasMessage("La pregunta no pertenece a la encuesta");
        verify(questionRepository, never()).findById(99L);
    }
}
