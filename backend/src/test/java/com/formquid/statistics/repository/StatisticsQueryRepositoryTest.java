package com.formquid.statistics.repository;

import com.formquid.config.PostgresDataJpaTest;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.participation.entity.Answer;
import com.formquid.participation.entity.AnswerOption;
import com.formquid.participation.entity.AnswerOptionId;
import com.formquid.participation.entity.MatrixAnswerValue;
import com.formquid.participation.entity.MatrixAnswerValueId;
import com.formquid.participation.entity.Participation;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.MatrixRow;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionOption;
import com.formquid.question.entity.QuestionScope;
import com.formquid.question.entity.QuestionSettings;
import com.formquid.question.entity.QuestionType;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserGender;
import com.formquid.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresDataJpaTest
@Import(StatisticsQueryRepository.class)
class StatisticsQueryRepositoryTest {

    @Autowired private StatisticsQueryRepository statistics;
    @Autowired private EntityManager entityManager;

    @Test
    void aggregateQueriesCalculateAllSupportedStatisticsInBatches() {
        User owner = persistUser();
        Survey survey = persistSurvey(owner);
        Question numeric = persistQuestion(survey, 1, QuestionType.NUMBER, "Número", "0", "10");
        Question bool = persistQuestion(survey, 2, QuestionType.BOOLEAN, "Booleano", null, null);
        Question choice = persistQuestion(survey, 3, QuestionType.SINGLE_CHOICE, "Opción", null, null);
        Question matrix = persistQuestion(survey, 4, QuestionType.MATRIX_SCALE, "Matriz", "1", "5");
        persist(QuestionSettings.builder()
            .question(bool)
            .trueEvaluationFactor(new BigDecimal("0.8"))
            .falseEvaluationFactor(new BigDecimal("0.2"))
            .build());

        QuestionOption low = persist(QuestionOption.builder()
            .question(choice).label("Baja").position(1).evaluationFactor(new BigDecimal("0.25")).build());
        QuestionOption high = persist(QuestionOption.builder()
            .question(choice).label("Alta").position(2).evaluationFactor(new BigDecimal("0.75")).build());
        MatrixRow firstRow = persist(MatrixRow.builder()
            .question(matrix).label("Primera").position(1).build());
        MatrixRow secondRow = persist(MatrixRow.builder()
            .question(matrix).label("Segunda").position(2).build());
        Participation first = persist(Participation.builder()
            .survey(survey).respondentTokenHash("a".repeat(64)).build());
        Participation second = persist(Participation.builder()
            .survey(survey).respondentTokenHash("b".repeat(64)).build());
        entityManager.flush();

        persistAnswer(first, numeric, new BigDecimal("2"), null);
        persistAnswer(second, numeric, new BigDecimal("8"), null);
        persistAnswer(first, bool, null, true);
        persistAnswer(second, bool, null, false);
        Answer choiceFirst = persistAnswer(first, choice, null, null);
        Answer choiceSecond = persistAnswer(second, choice, null, null);
        Answer matrixFirst = persistAnswer(first, matrix, null, null);
        Answer matrixSecond = persistAnswer(second, matrix, null, null);
        entityManager.flush();

        persistSelection(choiceFirst, low);
        persistSelection(choiceSecond, high);
        persistMatrixValue(matrixFirst, firstRow, "1");
        persistMatrixValue(matrixFirst, secondRow, "5");
        persistMatrixValue(matrixSecond, firstRow, "3");
        persistMatrixValue(matrixSecond, secondRow, "3");
        entityManager.flush();
        entityManager.clear();

        var participationSummary = statistics.summarizeParticipations(survey.getId());
        assertThat(participationSummary.participationCount()).isEqualTo(2);
        assertThat(participationSummary.firstParticipation()).isNotNull();
        assertThat(participationSummary.lastParticipation()).isNotNull();

        var questionAggregates = statistics.summarizeQuestions(List.of(
            numeric.getId(), bool.getId(), choice.getId(), matrix.getId()
        ));
        assertThat(questionAggregates).filteredOn(row -> row.questionId() == numeric.getId())
            .singleElement().satisfies(row -> {
                assertThat(row.answerCount()).isEqualTo(2);
                assertThat(row.numericAverage()).isEqualTo(5.0);
                assertThat(row.numericMinimum()).isEqualByComparingTo("2");
                assertThat(row.numericMaximum()).isEqualByComparingTo("8");
                assertThat(row.numericMedian()).isEqualTo(5.0);
                assertThat(row.numericStandardDeviation()).isEqualTo(3.0);
                assertThat(row.scalarNormalizedScore()).isEqualTo(50.0);
            });
        assertThat(questionAggregates).filteredOn(row -> row.questionId() == bool.getId())
            .singleElement().satisfies(row -> {
                assertThat(row.trueCount()).isEqualTo(1);
                assertThat(row.falseCount()).isEqualTo(1);
                assertThat(row.scalarNormalizedScore()).isEqualTo(50.0);
            });

        assertThat(statistics.findScaleValueCounts(List.of(numeric.getId())))
            .extracting("value", "count")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(new BigDecimal("2"), 1L),
                org.assertj.core.groups.Tuple.tuple(new BigDecimal("8"), 1L)
            );
        assertThat(statistics.summarizeChoiceOptions(List.of(choice.getId())))
            .extracting("label", "count")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("Baja", 1L),
                org.assertj.core.groups.Tuple.tuple("Alta", 1L)
            );
        assertThat(statistics.summarizeChoiceScores(List.of(choice.getId())))
            .singleElement().extracting("normalizedScore").isEqualTo(50.0);

        assertThat(statistics.summarizeMatrixRows(List.of(matrix.getId())))
            .extracting("label", "answerCount", "numericAverage", "normalizedAverage")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("Primera", 2L, 2.0, 25.0),
                org.assertj.core.groups.Tuple.tuple("Segunda", 2L, 4.0, 75.0)
            );
        assertThat(statistics.findMatrixValueCounts(List.of(matrix.getId()))).hasSize(4);
        assertThat(statistics.summarizeMatrixScores(List.of(matrix.getId())))
            .singleElement().extracting("normalizedScore").isEqualTo(50.0);
    }

    private Question persistQuestion(
        Survey survey,
        int position,
        QuestionType type,
        String statement,
        String minimum,
        String maximum
    ) {
        ContentItem item = persist(ContentItem.builder()
            .survey(survey).itemType(ContentItemType.QUESTION).position(position).required(false).build());
        Question question = persist(Question.builder()
            .parentItem(item)
            .scope(QuestionScope.LOCAL)
            .statement(statement)
            .questionType(type)
            .evaluationDirection(EvaluationDirection.NEUTRAL)
            .build());
        if (minimum != null) {
            persist(QuestionSettings.builder()
                .question(question)
                .minValue(new BigDecimal(minimum))
                .maxValue(new BigDecimal(maximum))
                .step(BigDecimal.ONE)
                .build());
        }
        return question;
    }

    private Answer persistAnswer(
        Participation participation,
        Question question,
        BigDecimal numericValue,
        Boolean booleanValue
    ) {
        return persist(Answer.builder()
            .participation(participation)
            .question(question)
            .numericValue(numericValue)
            .booleanValue(booleanValue)
            .build());
    }

    private void persistSelection(Answer answer, QuestionOption option) {
        persist(AnswerOption.builder()
            .id(new AnswerOptionId(answer.getId(), option.getId()))
            .answer(answer)
            .option(option)
            .build());
    }

    private void persistMatrixValue(Answer answer, MatrixRow row, String value) {
        persist(MatrixAnswerValue.builder()
            .id(new MatrixAnswerValueId(answer.getId(), row.getId()))
            .answer(answer)
            .matrixRow(row)
            .numericValue(new BigDecimal(value))
            .build());
    }

    private Survey persistSurvey(User owner) {
        return persist(Survey.builder()
            .publicCode("STATSQL00001")
            .owner(owner)
            .title("Encuesta de estadísticas")
            .lifecycleStatus(SurveyLifecycle.OPEN)
            .accessMode(SurveyAccessMode.PUBLIC)
            .visibility(SurveyVisibility.LISTED)
            .build());
    }

    private User persistUser() {
        return persist(User.builder()
            .role(UserRole.EDITOR)
            .email("statistics-query@example.com")
            .name("Usuario")
            .gender(UserGender.OTHER)
            .institution("FormQuid")
            .passwordHash("not-used-in-tests")
            .build());
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
