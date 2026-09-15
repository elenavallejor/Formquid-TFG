package com.formquid.question.service;

import com.formquid.question.dto.QuestionDraftDto;
import com.formquid.question.dto.QuestionOptionDto;
import com.formquid.question.dto.QuestionSettingsDto;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void rejectsSelectionLimitGreaterThanAvailableOptions() {
        QuestionSettingsDto settings = settings(null, 3, null, null, null);

        assertThatThrownBy(() -> questionService.validateQuestionDraft(choiceQuestion(settings, validOptions())))
            .hasMessage("Los límites de selección no pueden superar el número de opciones");
    }

    @Test
    void rejectsEvaluationFactorOutsideZeroToOneRange() {
        List<QuestionOptionDto> options = List.of(
            new QuestionOptionDto(null, "Uno", 1, false, new BigDecimal("1.1")),
            new QuestionOptionDto(null, "Dos", 2, false, BigDecimal.ZERO)
        );

        assertThatThrownBy(() -> questionService.validateQuestionDraft(choiceQuestion(settings(null, null, null, null, null), options)))
            .hasMessage("El factor de evaluación de una opción debe estar entre 0 y 1");
    }

    @Test
    void rejectsNonConsecutiveOptionPositions() {
        List<QuestionOptionDto> options = List.of(
            new QuestionOptionDto(null, "Uno", 1, false, null),
            new QuestionOptionDto(null, "Dos", 3, false, null)
        );

        assertThatThrownBy(() -> questionService.validateQuestionDraft(choiceQuestion(settings(null, null, null, null, null), options)))
            .hasMessage("Las posiciones de las opciones deben ser consecutivas");
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

    private QuestionSettingsDto settings(
        Integer minSelections,
        Integer maxSelections,
        BigDecimal trueFactor,
        BigDecimal falseFactor,
        Short fontSize
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
            null,
            fontSize,
            null,
            null,
            null
        );
    }
}
