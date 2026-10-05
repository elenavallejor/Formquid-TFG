package com.formquid.repository;

import com.formquid.collaboration.entity.CollaborationPermission;
import com.formquid.collaboration.entity.SurveyCollaborator;
import com.formquid.collaboration.entity.SurveyCollaboratorId;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.config.PostgresDataJpaTest;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.participation.entity.Answer;
import com.formquid.participation.entity.AnswerOption;
import com.formquid.participation.entity.AnswerOptionId;
import com.formquid.participation.entity.MatrixAnswerValue;
import com.formquid.participation.entity.MatrixAnswerValueId;
import com.formquid.participation.entity.Participation;
import com.formquid.participation.repository.AnswerOptionRepository;
import com.formquid.participation.repository.AnswerRepository;
import com.formquid.participation.repository.MatrixAnswerValueRepository;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.MatrixRow;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionOption;
import com.formquid.question.entity.QuestionScope;
import com.formquid.question.entity.QuestionType;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserGender;
import com.formquid.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresDataJpaTest
class CollaborationAndAnswerRepositoryTest {

    @Autowired private SurveyCollaboratorRepository collaborators;
    @Autowired private SurveyRepository surveys;
    @Autowired private AnswerRepository answers;
    @Autowired private AnswerOptionRepository answerOptions;
    @Autowired private MatrixAnswerValueRepository matrixValues;
    @Autowired private EntityManager entityManager;

    @Test
    void collaboratorQueries_exposePermissionAndEditableSurvey() {
        User owner = persistUser("collaboration-owner@example.com");
        User collaborator = persistUser("collaboration-editor@example.com");
        Survey survey = persistSurvey(owner, "COLLAB000001");
        persist(SurveyCollaborator.builder()
            .id(new SurveyCollaboratorId(survey.getId(), collaborator.getId()))
            .survey(survey)
            .user(collaborator)
            .permission(CollaborationPermission.VIEW)
            .build());
        entityManager.flush();

        assertThat(collaborators.findPermission(survey.getId(), collaborator.getId()))
            .contains(CollaborationPermission.VIEW);
        assertThat(collaborators.findSurveyIdsByUserIdAndSurveyIds(
            collaborator.getId(), List.of(survey.getId())
        )).containsExactly(survey.getId());
        assertThat(surveys.findEditableByUser(
            collaborator.getId(), "", null, PageRequest.of(0, 10)
        )).extracting(Survey::getId).containsExactly(survey.getId());
    }

    @Test
    void answerRepositories_calculateAggregatesAndOrderedSelections() {
        User owner = persistUser("answer-owner@example.com");
        Survey survey = persistSurvey(owner, "ANSWERS00001");
        Question numericQuestion = persistQuestion(survey, 1, QuestionType.NUMBER, "Número");
        Question choiceQuestion = persistQuestion(survey, 2, QuestionType.SINGLE_CHOICE, "Opción");
        Question matrixQuestion = persistQuestion(survey, 3, QuestionType.MATRIX_SCALE, "Matriz");

        QuestionOption option = persist(QuestionOption.builder()
            .question(choiceQuestion).label("Sí").position(1).build());
        QuestionOption unselectedOption = persist(QuestionOption.builder()
            .question(choiceQuestion).label("No").position(2).build());
        MatrixRow secondRow = persist(MatrixRow.builder()
            .question(matrixQuestion).label("Segunda").position(2).build());
        MatrixRow firstRow = persist(MatrixRow.builder()
            .question(matrixQuestion).label("Primera").position(1).build());

        Participation firstParticipation = persist(Participation.builder()
            .survey(survey).respondentTokenHash("c".repeat(64)).build());
        Participation secondParticipation = persist(Participation.builder()
            .survey(survey).respondentTokenHash("d".repeat(64)).build());
        entityManager.flush();

        persist(Answer.builder().participation(firstParticipation).question(numericQuestion)
            .numericValue(new BigDecimal("2")).build());
        persist(Answer.builder().participation(secondParticipation).question(numericQuestion)
            .numericValue(new BigDecimal("8")).build());
        Answer selected = persist(Answer.builder()
            .participation(firstParticipation).question(choiceQuestion).build());
        Answer matrix = persist(Answer.builder()
            .participation(firstParticipation).question(matrixQuestion).build());
        entityManager.flush();

        persist(AnswerOption.builder()
            .id(new AnswerOptionId(selected.getId(), option.getId()))
            .answer(selected).option(option).build());
        persist(MatrixAnswerValue.builder()
            .id(new MatrixAnswerValueId(matrix.getId(), secondRow.getId()))
            .answer(matrix).matrixRow(secondRow).numericValue(new BigDecimal("7")).build());
        persist(MatrixAnswerValue.builder()
            .id(new MatrixAnswerValueId(matrix.getId(), firstRow.getId()))
            .answer(matrix).matrixRow(firstRow).numericValue(new BigDecimal("3")).build());
        entityManager.flush();

        assertThat(answers.countByQuestionId(numericQuestion.getId())).isEqualTo(2);
        assertThat(answers.averageNumeric(numericQuestion.getId())).isEqualTo(5.0);
        assertThat(answers.minNumeric(numericQuestion.getId())).isEqualByComparingTo("2");
        assertThat(answers.maxNumeric(numericQuestion.getId())).isEqualByComparingTo("8");

        assertThat(answerOptions.countOptionsByQuestion(choiceQuestion.getId()))
            .hasSize(2)
            .first().satisfies(row -> {
                assertThat(row[0]).isEqualTo(option.getId());
                assertThat(row[1]).isEqualTo("Sí");
                assertThat(((Number) row[2]).longValue()).isEqualTo(1);
            });
        assertThat(answerOptions.countOptionsByQuestion(choiceQuestion.getId()))
            .element(1).satisfies(row -> {
                assertThat(row[0]).isEqualTo(unselectedOption.getId());
                assertThat(row[1]).isEqualTo("No");
                assertThat(((Number) row[2]).longValue()).isZero();
            });
        assertThat(matrixValues.findByQuestionId(matrixQuestion.getId()))
            .extracting(value -> value.getMatrixRow().getLabel())
            .containsExactly("Primera", "Segunda");
    }

    private Question persistQuestion(Survey survey, int position, QuestionType type, String statement) {
        ContentItem item = persist(ContentItem.builder()
            .survey(survey).itemType(ContentItemType.QUESTION).position(position).required(false).build());
        return persist(Question.builder()
            .parentItem(item)
            .scope(QuestionScope.LOCAL)
            .statement(statement)
            .questionType(type)
            .evaluationDirection(EvaluationDirection.NEUTRAL)
            .build());
    }

    private Survey persistSurvey(User owner, String publicCode) {
        return persist(Survey.builder()
            .publicCode(publicCode)
            .owner(owner)
            .title("Encuesta de repositorio")
            .lifecycleStatus(SurveyLifecycle.OPEN)
            .accessMode(SurveyAccessMode.PUBLIC)
            .visibility(SurveyVisibility.LISTED)
            .build());
    }

    private User persistUser(String email) {
        return persist(User.builder()
            .role(UserRole.EDITOR)
            .email(email)
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
