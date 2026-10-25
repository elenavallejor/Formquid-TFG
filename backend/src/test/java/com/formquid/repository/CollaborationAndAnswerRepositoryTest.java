package com.formquid.repository;

import com.formquid.collaboration.entity.CollaborationPermission;
import com.formquid.collaboration.entity.SurveyCollaborator;
import com.formquid.collaboration.entity.SurveyCollaboratorId;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.common.Category;
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
import com.formquid.participation.repository.ParticipationRepository;
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
import com.formquid.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
    @Autowired private ParticipationRepository participations;
    @Autowired private EntityManager entityManager;

    @Test
    void collaboratorQueries_exposePermissionAndEditableSurvey() {
        User owner = persistUser("collaboration-owner@example.com");
        User collaborator = persistUser("collaboration-editor@example.com");
        User unrelatedOwner = persistUser("collaboration-unrelated@example.com");
        Survey survey = persistSurvey(owner, "COLLAB000001");
        Survey ownedSurvey = persistSurvey(collaborator, "OWNED0000001");
        survey.setCategory(Category.TELEFONO_INTELIGENTE);
        survey.setAccessMode(SurveyAccessMode.AUTHENTICATED);
        ownedSurvey.setCategory(Category.ORDENADORES);
        persistSurvey(unrelatedOwner, "OTHER0000001");
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
            collaborator.getId(), "", null, List.of(SurveyAccessMode.PUBLIC, SurveyAccessMode.AUTHENTICATED),
            false, List.of(Category.ORDENADORES, Category.TELEFONO_INTELIGENTE), PageRequest.of(0, 10)
        )).extracting(Survey::getId).containsExactlyInAnyOrder(survey.getId(), ownedSurvey.getId());
        assertThat(surveys.findEditableByUser(
            collaborator.getId(), "", null, List.of(SurveyAccessMode.AUTHENTICATED),
            false, List.of(Category.ORDENADORES, Category.TELEFONO_INTELIGENTE), PageRequest.of(0, 10)
        )).extracting(Survey::getId).containsExactly(survey.getId());
    }

    @Test
    void participationResultQueries_returnStablePagesAndOrderedDetails() {
        User owner = persistUser("answer-owner@example.com");
        Survey survey = persistSurvey(owner, "ANSWERS00001");
        Question choiceQuestion = persistQuestion(survey, 1, QuestionType.SINGLE_CHOICE, "Opción");
        Question matrixQuestion = persistQuestion(survey, 2, QuestionType.MATRIX_SCALE, "Matriz");

        QuestionOption option = persist(QuestionOption.builder()
            .question(choiceQuestion).label("Sí").position(1).build());
        MatrixRow secondRow = persist(MatrixRow.builder()
            .question(matrixQuestion).label("Segunda").position(2).build());
        MatrixRow firstRow = persist(MatrixRow.builder()
            .question(matrixQuestion).label("Primera").position(1).build());

        Participation firstParticipation = persist(Participation.builder()
            .survey(survey).respondentTokenHash("c".repeat(64)).build());
        Participation secondParticipation = persist(Participation.builder()
            .survey(survey).respondentTokenHash("d".repeat(64)).build());
        entityManager.flush();

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
        entityManager.createNativeQuery("""
                update participations
                set submitted_at = timestamp with time zone '2026-10-06 10:00:00+00'
                where survey_id = :surveyId
                """)
            .setParameter("surveyId", survey.getId())
            .executeUpdate();
        entityManager.clear();

        Sort stableSort = Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id"));
        var firstPage = participations.findSummariesBySurveyId(
            survey.getId(), PageRequest.of(0, 1, stableSort)
        );
        var secondPage = participations.findSummariesBySurveyId(
            survey.getId(), PageRequest.of(1, 1, stableSort)
        );
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting("id").containsExactly(secondParticipation.getId());
        assertThat(secondPage.getContent()).extracting("id").containsExactly(firstParticipation.getId());

        assertThat(participations.findDetailByIdAndSurveyId(firstParticipation.getId(), survey.getId()))
            .isPresent();
        assertThat(answers.findDetailsByParticipationId(firstParticipation.getId()))
            .extracting(answer -> answer.getQuestion().getStatement())
            .containsExactly("Opción", "Matriz");
        assertThat(answerOptions.findByAnswerIdsWithOption(List.of(selected.getId(), matrix.getId())))
            .extracting(value -> value.getOption().getLabel())
            .containsExactly("Sí");
        assertThat(matrixValues.findByAnswerIdsWithRow(List.of(selected.getId(), matrix.getId())))
            .extracting(value -> value.getMatrixRow().getLabel())
            .containsExactly("Primera", "Segunda");
    }

    @Test
    void savedAnswers_cascadeNewOptionsAndMatrixValuesAtFlush() {
        User owner = persistUser("cascade-answer-owner@example.com");
        Survey survey = persistSurvey(owner, "CASCADE00001");
        Question choiceQuestion = persistQuestion(survey, 1, QuestionType.SINGLE_CHOICE, "Opción");
        Question matrixQuestion = persistQuestion(survey, 2, QuestionType.MATRIX_SCALE, "Matriz");
        QuestionOption option = persist(QuestionOption.builder()
            .question(choiceQuestion).label("Sí").position(1).build());
        MatrixRow row = persist(MatrixRow.builder()
            .question(matrixQuestion).label("Fila").position(1).build());
        Participation participation = persist(Participation.builder()
            .survey(survey).respondentTokenHash("i".repeat(64)).build());

        Answer choiceAnswer = answers.save(Answer.builder()
            .participation(participation).question(choiceQuestion).build());
        choiceAnswer.getSelectedOptions().add(AnswerOption.builder()
            .id(new AnswerOptionId(choiceAnswer.getId(), option.getId()))
            .answer(choiceAnswer).option(option).build());
        Answer matrixAnswer = answers.save(Answer.builder()
            .participation(participation).question(matrixQuestion).build());
        matrixAnswer.getMatrixValues().add(MatrixAnswerValue.builder()
            .id(new MatrixAnswerValueId(matrixAnswer.getId(), row.getId()))
            .answer(matrixAnswer).matrixRow(row).numericValue(new BigDecimal("3")).build());

        entityManager.flush();
        entityManager.clear();

        assertThat(answerOptions.findByAnswerIdsWithOption(List.of(choiceAnswer.getId(), matrixAnswer.getId())))
            .extracting(value -> value.getOption().getId()).containsExactly(option.getId());
        assertThat(matrixValues.findByAnswerIdsWithRow(List.of(choiceAnswer.getId(), matrixAnswer.getId())))
            .extracting(value -> value.getMatrixRow().getId()).containsExactly(row.getId());
    }

    @Test
    void resultSurveyQuery_excludesSurveysWithoutResponsesAndUnauthorizedSurveys() {
        User owner = persistUser("results-owner@example.com");
        User collaborator = persistUser("results-collaborator@example.com");
        User unrelatedOwner = persistUser("results-unrelated@example.com");
        Survey collaboratingSurvey = persistSurvey(owner, "RESULT000001");
        Survey ownedSurvey = persistSurvey(collaborator, "RESULT000002");
        Survey emptyOwnedSurvey = persistSurvey(collaborator, "RESULT000003");
        Survey unrelatedSurvey = persistSurvey(unrelatedOwner, "RESULT000004");
        persist(SurveyCollaborator.builder()
            .id(new SurveyCollaboratorId(collaboratingSurvey.getId(), collaborator.getId()))
            .survey(collaboratingSurvey)
            .user(collaborator)
            .permission(CollaborationPermission.VIEW)
            .build());
        persist(Participation.builder().survey(collaboratingSurvey).respondentTokenHash("e".repeat(64)).build());
        persist(Participation.builder().survey(ownedSurvey).respondentTokenHash("f".repeat(64)).build());
        persist(Participation.builder().survey(unrelatedSurvey).respondentTokenHash("g".repeat(64)).build());
        entityManager.flush();

        var page = surveys.findResultSummaries(
            collaborator.getId(),
            false,
            "",
            true,
            true,
            true,
            java.util.List.of(SurveyLifecycle.DRAFT),
            PageRequest.of(0, 10)
        );

        assertThat(page.getContent())
            .extracting("id")
            .containsExactlyInAnyOrder(collaboratingSurvey.getId(), ownedSurvey.getId())
            .doesNotContain(emptyOwnedSurvey.getId(), unrelatedSurvey.getId());
        assertThat(page.getContent()).allSatisfy(summary -> assertThat(summary.responses()).isEqualTo(1));

        var newestPage = surveys.findResultSummaries(
            collaborator.getId(), false, "", true, true, true, java.util.List.of(SurveyLifecycle.DRAFT),
            PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "updatedAt", "id"))
        );
        var oldestPage = surveys.findResultSummaries(
            collaborator.getId(), false, "", true, true, true, java.util.List.of(SurveyLifecycle.DRAFT),
            PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "updatedAt", "id"))
        );
        assertThat(newestPage.getTotalElements()).isEqualTo(2);
        assertThat(oldestPage.getTotalElements()).isEqualTo(2);
        assertThat(newestPage.getContent()).hasSize(1);
        assertThat(oldestPage.getContent()).hasSize(1);
    }

    @Test
    void deletingOpenSurvey_removesItsParticipationsAndContent() {
        User owner = persistUser("delete-open-survey@example.com");
        Survey survey = persistSurvey(owner, "DELETEOPEN01");
        Question question = persistQuestion(survey, 1, QuestionType.TEXT, "Pregunta de prueba");
        Participation participation = persist(Participation.builder()
            .survey(survey).respondentTokenHash("h".repeat(64)).build());
        entityManager.flush();
        Answer answer = persist(Answer.builder()
            .participation(participation).question(question).textValue("Respuesta").build());
        entityManager.flush();
        entityManager.clear();

        participations.deleteBySurveyId(survey.getId());
        surveys.deleteSurveyById(survey.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(surveys.existsById(survey.getId())).isFalse();
        assertThat(participations.existsById(participation.getId())).isFalse();
        assertThat(answers.existsById(answer.getId())).isFalse();
        assertThat(entityManager.find(Question.class, question.getId())).isNull();
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
            .institution("FormQuid")
            .passwordHash("not-used-in-tests")
            .build());
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
