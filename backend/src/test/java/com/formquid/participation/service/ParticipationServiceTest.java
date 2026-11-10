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
import java.math.BigDecimal;
import com.formquid.participation.dto.*;
import com.formquid.question.entity.QuestionOption;
import com.formquid.question.entity.MatrixRow;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {
    private Question prepareQuestion(QuestionType type) {
        Survey survey=Survey.builder().id(1L).publicCode("ABCDEF234567").lifecycleStatus(SurveyLifecycle.OPEN).accessMode(SurveyAccessMode.PUBLIC).build();
        ContentItem item=ContentItem.builder().id(11L).itemType(ContentItemType.QUESTION).required(true).build();
        Question question=Question.builder().id(21L).parentItem(item).questionType(type).build();
        when(surveyRepository.findByPublicCode("ABCDEF234567")).thenReturn(Optional.of(survey));
        when(contentItemRepository.findSurveyContentRows(1L)).thenReturn(List.of(new ContentItemRowDto(11L,ContentItemType.QUESTION,1,true,null)));
        when(questionRepository.findByParentItemIdsForStatistics(Set.of(11L))).thenReturn(List.of(question));
        when(participationRepository.saveAndFlush(any())).thenReturn(Participation.builder().id(50L).survey(survey).build());
        return question;
    }

    private ParticipationResponseDto submitAnswer(SubmitAnswerRequestDto answer) {
        return participationService.submit("ABCDEF234567",new SubmitParticipationRequestDto(null,List.of(answer)),null,"token");
    }

    @ParameterizedTest
    @EnumSource(QuestionType.class)
    void submit_allQuestionTypes_persistsTypedValuesAndAssociations(QuestionType type) {
        Question q=prepareQuestion(type);
        q.setSettings(QuestionSettings.builder().minValue(BigDecimal.ONE).maxValue(BigDecimal.TEN).step(BigDecimal.ONE).maxLength(20).minSelections(1).maxSelections(2).build());
        when(answerRepository.save(any())).thenAnswer(i -> { Answer a=i.getArgument(0); a.setId(60L); return a; });
        String text=type==QuestionType.TEXT?"Respuesta":null;
        BigDecimal number=type==QuestionType.NUMBER||type==QuestionType.SCALE?new BigDecimal("3"):null;
        Boolean bool=type==QuestionType.BOOLEAN?false:null;
        List<OptionAnswerRequestDto> options=null;
        List<MatrixAnswerRequestDto> matrix=null;
        if(type==QuestionType.SINGLE_CHOICE||type==QuestionType.MULTIPLE_CHOICE) {
            QuestionOption option=QuestionOption.builder().id(70L).question(q).other(true).build();
            when(optionRepository.findById(70L)).thenReturn(Optional.of(option));
            options=List.of(new OptionAnswerRequestDto(70L,"Otro"));
        }
        if(type==QuestionType.MATRIX_SCALE) {
            MatrixRow row=MatrixRow.builder().id(80L).question(q).position(1).label("Fila").build();
            when(matrixRowRepository.findByQuestionIdOrderByPositionAsc(21L)).thenReturn(List.of(row));
            when(matrixRowRepository.findById(80L)).thenReturn(Optional.of(row));
            matrix=List.of(new MatrixAnswerRequestDto(80L,new BigDecimal("3")));
        }
        var response=submitAnswer(new SubmitAnswerRequestDto(21L,text,number,bool,options,matrix));
        assertThat(response.answers()).singleElement().satisfies(a -> {
            assertThat(a.questionId()).isEqualTo(21L);
            assertThat(a.textValue()).isEqualTo(text);
            assertThat(a.numericValue()).isEqualTo(number);
            assertThat(a.booleanValue()).isEqualTo(bool);
        });
        verify(answerRepository).save(argThat(a -> a.getQuestion()==q));
    }

    @Test
    void submitNumeric_rejectsOutOfRangeWrongStepAndIncompatibleFields() {
        Question q=prepareQuestion(QuestionType.SCALE);
        q.setSettings(QuestionSettings.builder().minValue(BigDecimal.ONE).maxValue(new BigDecimal("5")).step(new BigDecimal("2")).build());
        for(var entry:List.of(new String[]{"0","mínimo"},new String[]{"6","máximo"},new String[]{"2","step"})) {
            assertThatThrownBy(() -> submitAnswer(new SubmitAnswerRequestDto(21L,null,new BigDecimal(entry[0]),null,null,null)))
                .hasMessageContaining(entry[1]);
        }
        assertThatThrownBy(() -> submitAnswer(new SubmitAnswerRequestDto(21L,"texto",BigDecimal.ONE,null,null,null))).hasMessageContaining("incompatibles");
    }

    @Test
    void submitOptions_rejectsForeignDuplicateAndInvalidOtherText() {
        Question q=prepareQuestion(QuestionType.MULTIPLE_CHOICE);
        QuestionOption option=QuestionOption.builder().id(70L).question(q).other(true).build();
        when(optionRepository.findById(70L)).thenReturn(Optional.of(option));
        when(answerRepository.save(any())).thenAnswer(i -> { Answer a=i.getArgument(0); a.setId(60L); return a; });
        assertThatThrownBy(() -> submitAnswer(new SubmitAnswerRequestDto(21L,null,null,null,List.of(new OptionAnswerRequestDto(70L,null)),null))).hasMessageContaining("requiere texto");
        assertThatThrownBy(() -> submitAnswer(new SubmitAnswerRequestDto(21L,null,null,null,List.of(new OptionAnswerRequestDto(70L,"x".repeat(501))),null))).hasMessageContaining("longitud máxima");
        option.setOther(false);
        assertThatThrownBy(() -> submitAnswer(new SubmitAnswerRequestDto(21L,null,null,null,List.of(new OptionAnswerRequestDto(70L,"Otro")),null))).hasMessageContaining("Solo las opciones OTHER");
        assertThatThrownBy(() -> submitAnswer(new SubmitAnswerRequestDto(21L,null,null,null,List.of(new OptionAnswerRequestDto(70L,null),new OptionAnswerRequestDto(70L,null)),null))).hasMessageContaining("duplicadas");
        option.setQuestion(Question.builder().id(99L).build());
        assertThatThrownBy(() -> submitAnswer(new SubmitAnswerRequestDto(21L,null,null,null,List.of(new OptionAnswerRequestDto(70L,null)),null))).hasMessageContaining("no pertenece");
    }

    @Test
    void submitAccess_rejectsClosedUnauthenticatedAndIncorrectPassword() {
        Survey survey=Survey.builder().id(1L).lifecycleStatus(SurveyLifecycle.CLOSED).build();
        when(surveyRepository.findByPublicCode("ABCDEF234567")).thenReturn(Optional.of(survey));
        var request=new SubmitParticipationRequestDto(null,List.of());
        assertThatThrownBy(() -> participationService.submit("ABCDEF234567",request,null,"token")).hasMessageContaining("no admite respuestas");
        survey.setLifecycleStatus(SurveyLifecycle.OPEN); survey.setAccessMode(SurveyAccessMode.AUTHENTICATED);
        assertThatThrownBy(() -> participationService.submit("ABCDEF234567",request,null,"token")).hasMessageContaining("cuenta registrada");
        survey.setAccessMode(SurveyAccessMode.PASSWORD_PROTECTED);
        assertThatThrownBy(() -> participationService.submit("ABCDEF234567",request,null,"token")).hasMessageContaining("Contraseña");
        survey.setAccessMode(SurveyAccessMode.PUBLIC);
        assertThatThrownBy(() -> participationService.submit("ABCDEF234567",request,null,null)).hasMessageContaining("identidad asociada");
        User user=User.builder().id(9L).build();
        when(participationRepository.existsBySurveyIdAndRespondentUserId(1L,9L)).thenReturn(true);
        assertThatThrownBy(() -> participationService.submit("ABCDEF234567",request,user,null)).hasMessageContaining("Ya respondiste");
    }

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
