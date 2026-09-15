package com.formquid.participation.service;

import com.formquid.block.entity.Block;
import com.formquid.block.repository.BlockRepository;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.participation.dto.ParticipationResponseDto;
import com.formquid.participation.dto.SubmitAnswerRequestDto;
import com.formquid.participation.dto.SubmitParticipationRequestDto;
import com.formquid.participation.entity.Answer;
import com.formquid.participation.entity.Participation;
import com.formquid.participation.repository.AnswerRepository;
import com.formquid.participation.repository.ParticipationRepository;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.repository.MatrixRowRepository;
import com.formquid.question.repository.QuestionOptionRepository;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.repository.SurveyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
    private PasswordEncoder passwordEncoder;

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
        ContentItem textItem = ContentItem.builder()
            .id(30L)
            .parentBlock(block)
            .itemType(ContentItemType.TEXT)
            .position(1)
            .textBody("Introducción")
            .build();
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
        AtomicReference<Answer> savedAnswer = new AtomicReference<>();

        when(surveyRepository.findByPublicCode("ABCDEF234567")).thenReturn(Optional.of(survey));
        when(contentItemRepository.findBySurveyIdOrderByPositionAsc(1L)).thenReturn(List.of(blockItem));
        when(blockRepository.findByParentItemId(10L)).thenReturn(Optional.of(block));
        when(contentItemRepository.findByParentBlockIdOrderByPositionAsc(20L)).thenReturn(List.of(textItem, questionItem));
        when(questionRepository.findByParentItemId(31L)).thenReturn(Optional.of(question));
        when(questionRepository.findById(40L)).thenReturn(Optional.of(question));
        when(participationRepository.save(org.mockito.ArgumentMatchers.any(Participation.class))).thenAnswer(invocation -> {
            Participation participation = invocation.getArgument(0);
            participation.setId(50L);
            participation.setSubmittedAt(Instant.parse("2026-09-15T10:00:00Z"));
            return participation;
        });
        when(participationRepository.findById(50L)).thenReturn(Optional.empty());
        when(answerRepository.save(org.mockito.ArgumentMatchers.any(Answer.class))).thenAnswer(invocation -> {
            Answer answer = invocation.getArgument(0);
            answer.setId(60L);
            savedAnswer.set(answer);
            return answer;
        });
        when(answerRepository.findByParticipationId(50L)).thenAnswer(invocation -> List.of(savedAnswer.get()));

        ParticipationResponseDto response = participationService.submit(
            "abcdef234567",
            new SubmitParticipationRequestDto(null, List.of(
                new SubmitAnswerRequestDto(40L, "Respuesta", null, null, null, null)
            )),
            null
        );

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.answers()).hasSize(1);
        assertThat(response.answers().getFirst().questionId()).isEqualTo(40L);
    }
}
