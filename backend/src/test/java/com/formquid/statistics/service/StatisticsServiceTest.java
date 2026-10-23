package com.formquid.statistics.service;

import com.formquid.block.repository.BlockRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.content.dto.ContentItemRowDto;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionSettings;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.statistics.repository.StatisticsQueryRepository;
import com.formquid.statistics.repository.StatisticsQueryRepository.ParticipationSummary;
import com.formquid.statistics.repository.StatisticsQueryRepository.QuestionAggregate;
import com.formquid.statistics.repository.StatisticsQueryRepository.ValueCount;
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
    @Mock private ContentItemRepository contentItemRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private StatisticsQueryRepository statisticsQueryRepository;
    @Mock private SurveyService surveyService;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    void getSurveyStatistics_calculatesNumericAndEvaluationScoresFromAggregates() {
        User user = User.builder().id(4L).build();
        Survey survey = Survey.builder().id(1L).build();
        ContentItem item = ContentItem.builder()
            .id(10L).itemType(ContentItemType.QUESTION).position(1).required(false).build();
        Question question = Question.builder()
            .id(20L)
            .parentItem(item)
            .statement("Valoración")
            .questionType(QuestionType.NUMBER)
            .evaluationDirection(EvaluationDirection.POSITIVE)
            .evaluationWeight(BigDecimal.ONE)
            .build();
        question.setSettings(QuestionSettings.builder()
            .minValue(BigDecimal.ZERO)
            .maxValue(BigDecimal.TEN)
            .build());
        Instant first = Instant.parse("2026-01-01T10:00:00Z");
        Instant last = Instant.parse("2026-01-02T10:00:00Z");

        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(statisticsQueryRepository.summarizeParticipations(1L))
            .thenReturn(new ParticipationSummary(2, first, last));
        when(contentItemRepository.findSurveyContentRows(1L)).thenReturn(List.of(
            new ContentItemRowDto(10L, ContentItemType.QUESTION, 1, false, null)
        ));
        when(questionRepository.findByParentItemIdsForStatistics(List.of(10L))).thenReturn(List.of(question));
        when(statisticsQueryRepository.summarizeQuestions(List.of(20L))).thenReturn(List.of(
            new QuestionAggregate(
                20L, 2L, 5.0, new BigDecimal("2"), new BigDecimal("8"),
                5.0, 3.0, 0L, 0L, 50.0
            )
        ));

        var response = statisticsService.getSurveyStatistics(1L, user);

        assertThat(response.participations()).isEqualTo(2);
        assertThat(response.firstParticipation()).isEqualTo(first);
        assertThat(response.lastParticipation()).isEqualTo(last);
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
    void getSurveyStatistics_usesNegativeWeightMagnitudeWithoutReversingScoreTwice() {
        User user = User.builder().id(4L).build();
        Survey survey = Survey.builder().id(1L).build();
        ContentItem item = ContentItem.builder()
            .id(10L).itemType(ContentItemType.QUESTION).position(1).required(false).build();
        Question question = Question.builder()
            .id(20L)
            .parentItem(item)
            .statement("Uso excesivo")
            .questionType(QuestionType.NUMBER)
            .evaluationDirection(EvaluationDirection.NEGATIVE)
            .evaluationWeight(new BigDecimal("-3"))
            .build();
        question.setSettings(QuestionSettings.builder()
            .minValue(BigDecimal.ZERO)
            .maxValue(BigDecimal.TEN)
            .build());

        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(statisticsQueryRepository.summarizeParticipations(1L))
            .thenReturn(new ParticipationSummary(1, null, null));
        when(contentItemRepository.findSurveyContentRows(1L)).thenReturn(List.of(
            new ContentItemRowDto(10L, ContentItemType.QUESTION, 1, false, null)
        ));
        when(questionRepository.findByParentItemIdsForStatistics(List.of(10L))).thenReturn(List.of(question));
        when(statisticsQueryRepository.summarizeQuestions(List.of(20L))).thenReturn(List.of(
            new QuestionAggregate(20L, 1L, 10.0, BigDecimal.TEN, BigDecimal.TEN,
                10.0, 0.0, 0L, 0L, 100.0)
        ));

        var response = statisticsService.getSurveyStatistics(1L, user);

        assertThat(response.questions()).singleElement().satisfies(statistics ->
            assertThat(statistics.evaluationScore()).isEqualTo(-100.0)
        );
        assertThat(response.globalEvaluationScore()).isEqualTo(-100.0);
    }

    @Test
    void getSurveyStatistics_calculatesScaleValuePercentagesFromGroupedCounts() {
        User user = User.builder().id(4L).build();
        Survey survey = Survey.builder().id(1L).build();
        ContentItem item = ContentItem.builder()
            .id(10L).itemType(ContentItemType.QUESTION).position(1).required(false).build();
        Question question = Question.builder()
            .id(20L)
            .parentItem(item)
            .statement("Satisfacción")
            .questionType(QuestionType.SCALE)
            .evaluationDirection(EvaluationDirection.NEUTRAL)
            .build();
        question.setSettings(QuestionSettings.builder()
            .minValue(BigDecimal.ONE)
            .maxValue(new BigDecimal("5"))
            .build());

        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(statisticsQueryRepository.summarizeParticipations(1L))
            .thenReturn(new ParticipationSummary(3, null, null));
        when(contentItemRepository.findSurveyContentRows(1L)).thenReturn(List.of(
            new ContentItemRowDto(10L, ContentItemType.QUESTION, 1, false, null)
        ));
        when(questionRepository.findByParentItemIdsForStatistics(List.of(10L))).thenReturn(List.of(question));
        when(statisticsQueryRepository.summarizeQuestions(List.of(20L))).thenReturn(List.of(
            new QuestionAggregate(
                20L, 3L, 7.0 / 3.0, BigDecimal.ONE, new BigDecimal("5"),
                1.0, null, 0L, 0L, 100.0 / 3.0
            )
        ));
        when(statisticsQueryRepository.findScaleValueCounts(List.of(20L))).thenReturn(List.of(
            new ValueCount(20L, BigDecimal.ONE, 2L),
            new ValueCount(20L, new BigDecimal("5"), 1L)
        ));

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
    void getQuestionStatistics_rejectsQuestionOutsideSurveyBeforeQueryingAggregates() {
        User user = User.builder().id(4L).build();
        Survey survey = Survey.builder().id(1L).build();
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(questionRepository.findForStatistics(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statisticsService.getQuestionStatistics(1L, 99L, user))
            .isInstanceOf(ApiException.class)
            .hasMessage("La pregunta no pertenece a la encuesta");
        verify(statisticsQueryRepository, never()).summarizeParticipations(1L);
    }
}
