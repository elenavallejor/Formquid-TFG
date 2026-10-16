package com.formquid.question.service;

import com.formquid.common.Category;
import com.formquid.common.service.CategoryCatalogService;
import com.formquid.content.entity.ContentItem;
import com.formquid.question.dto.CreateFixedQuestionRequestDto;
import com.formquid.question.dto.QuestionDraftDto;
import com.formquid.question.dto.QuestionOptionDto;
import com.formquid.question.dto.QuestionSettingsDto;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionOption;
import com.formquid.question.entity.QuestionScope;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.survey.entity.Survey;
import com.formquid.template.entity.Template;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private CategoryCatalogService categoryCatalogService;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void rejectsSelectionLimitGreaterThanAvailableOptions() {
        QuestionSettingsDto settings = settings(null, 3, null, null);

        assertThatThrownBy(() -> questionService.validateQuestionDraft(choiceQuestion(settings, validOptions())))
            .hasMessage("Los límites de selección no pueden superar el número de opciones");
    }

    @Test
    void rejectsEvaluationFactorOutsideZeroToOneRange() {
        List<QuestionOptionDto> options = List.of(
            new QuestionOptionDto(null, "Uno", 1, false, new BigDecimal("1.1")),
            new QuestionOptionDto(null, "Dos", 2, false, BigDecimal.ZERO)
        );

        assertThatThrownBy(() -> questionService.validateQuestionDraft(choiceQuestion(settings(null, null, null, null), options)))
            .hasMessage("El factor de evaluación de una opción debe estar entre 0 y 1");
    }

    @Test
    void rejectsNonConsecutiveOptionPositions() {
        List<QuestionOptionDto> options = List.of(
            new QuestionOptionDto(null, "Uno", 1, false, null),
            new QuestionOptionDto(null, "Dos", 3, false, null)
        );

        assertThatThrownBy(() -> questionService.validateQuestionDraft(choiceQuestion(settings(null, null, null, null), options)))
            .hasMessage("Las posiciones de las opciones deben ser consecutivas");
    }

    @Test
    void fixedCatalogQuestionDiscardsEvaluationConfiguration() {
        when(questionRepository.save(any(Question.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = questionService.createFixedQuestion(
            new CreateFixedQuestionRequestDto(evaluableChoiceQuestion(), Category.DATOS_PERSONALES),
            null
        );

        assertThat(response.evaluationDirection()).isEqualTo(EvaluationDirection.NEUTRAL);
        assertThat(response.evaluationWeight()).isNull();
        assertThat(response.category()).isEqualTo(Category.DATOS_PERSONALES);
        assertThat(response.options())
            .extracting(QuestionOptionDto::evaluationFactor)
            .containsOnlyNulls();
    }

    @Test
    void localQuestionKeepsEvaluationOnlyInsideSurvey() {
        when(questionRepository.save(any(Question.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        ContentItem templateItem = ContentItem.builder()
            .template(Template.builder().id(1L).build())
            .build();
        ContentItem surveyItem = ContentItem.builder()
            .survey(Survey.builder().id(2L).build())
            .build();

        Question catalogQuestion = questionService.createLocalQuestion(
            evaluableBooleanQuestion(),
            templateItem,
            null
        );
        Question surveyQuestion = questionService.createLocalQuestion(
            evaluableBooleanQuestion(),
            surveyItem,
            null
        );

        assertThat(catalogQuestion.getEvaluationDirection()).isEqualTo(EvaluationDirection.NEUTRAL);
        assertThat(catalogQuestion.getEvaluationWeight()).isNull();
        assertThat(catalogQuestion.getSettings().getTrueEvaluationFactor()).isNull();
        assertThat(catalogQuestion.getSettings().getFalseEvaluationFactor()).isNull();
        assertThat(surveyQuestion.getEvaluationDirection()).isEqualTo(EvaluationDirection.POSITIVE);
        assertThat(surveyQuestion.getEvaluationWeight()).isEqualByComparingTo("2");
        assertThat(surveyQuestion.getSettings().getTrueEvaluationFactor())
            .isEqualByComparingTo(BigDecimal.ONE);
        assertThat(surveyQuestion.getSettings().getFalseEvaluationFactor())
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void copiedFixedQuestionDiscardsLegacyEvaluationOnlyInCatalogContext() {
        when(questionRepository.save(any(Question.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        Question source = Question.builder()
            .scope(QuestionScope.CATALOG_FIXED)
            .category(Category.DATOS_PERSONALES)
            .statement("Pregunta evaluable antigua")
            .questionType(QuestionType.SINGLE_CHOICE)
            .evaluationDirection(EvaluationDirection.POSITIVE)
            .evaluationWeight(new BigDecimal("3"))
            .options(new ArrayList<>())
            .matrixRows(new ArrayList<>())
            .build();
        source.addOption(QuestionOption.builder()
            .label("Sí")
            .position(1)
            .evaluationFactor(BigDecimal.ONE)
            .build());
        ContentItem templateItem = ContentItem.builder()
            .template(Template.builder().id(1L).build())
            .build();
        ContentItem surveyItem = ContentItem.builder()
            .survey(Survey.builder().id(2L).build())
            .build();

        Question catalogCopy = questionService.createFixedSnapshot(source, templateItem, null);
        Question surveyCopy = questionService.createFixedSnapshot(source, surveyItem, null);

        assertThat(catalogCopy.getEvaluationDirection()).isEqualTo(EvaluationDirection.NEUTRAL);
        assertThat(catalogCopy.getCategory()).isEqualTo(Category.DATOS_PERSONALES);
        assertThat(surveyCopy.getCategory()).isEqualTo(Category.DATOS_PERSONALES);
        assertThat(catalogCopy.getEvaluationWeight()).isNull();
        assertThat(catalogCopy.getOptions().getFirst().getEvaluationFactor()).isNull();
        assertThat(surveyCopy.getEvaluationDirection()).isEqualTo(EvaluationDirection.POSITIVE);
        assertThat(surveyCopy.getEvaluationWeight()).isEqualByComparingTo("3");
        assertThat(surveyCopy.getOptions().getFirst().getEvaluationFactor())
            .isEqualByComparingTo(BigDecimal.ONE);
    }

    private QuestionDraftDto choiceQuestion(QuestionSettingsDto settings, List<QuestionOptionDto> options) {
        return new QuestionDraftDto(
            "Pregunta",
            null,
            QuestionType.MULTIPLE_CHOICE,
            EvaluationDirection.NEUTRAL,
            null,
            settings,
            options,
            List.of()
        );
    }

    private List<QuestionOptionDto> validOptions() {
        return List.of(
            new QuestionOptionDto(null, "Uno", 1, false, null),
            new QuestionOptionDto(null, "Dos", 2, false, null)
        );
    }

    private QuestionDraftDto evaluableChoiceQuestion() {
        return new QuestionDraftDto(
            "Pregunta evaluable",
            null,
            QuestionType.MULTIPLE_CHOICE,
            EvaluationDirection.POSITIVE,
            new BigDecimal("2"),
            settings(1, 2, null, null),
            List.of(
                new QuestionOptionDto(null, "Uno", 1, false, BigDecimal.ONE),
                new QuestionOptionDto(null, "Dos", 2, false, BigDecimal.ZERO)
            ),
            List.of()
        );
    }

    private QuestionDraftDto evaluableBooleanQuestion() {
        return new QuestionDraftDto(
            "Pregunta evaluable",
            null,
            QuestionType.BOOLEAN,
            EvaluationDirection.POSITIVE,
            new BigDecimal("2"),
            new QuestionSettingsDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Sí",
                "No",
                BigDecimal.ONE,
                BigDecimal.ZERO,
                null
            ),
            List.of(),
            List.of()
        );
    }

    private QuestionSettingsDto settings(
        Integer minSelections,
        Integer maxSelections,
        BigDecimal trueFactor,
        BigDecimal falseFactor
    ) {
        return new QuestionSettingsDto(
            null,
            null,
            null,
            null,
            minSelections,
            maxSelections,
            null,
            null,
            null,
            null,
            trueFactor,
            falseFactor,
            null
        );
    }
}
