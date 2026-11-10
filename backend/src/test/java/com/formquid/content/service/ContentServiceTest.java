package com.formquid.content.service;

import com.formquid.block.dto.CreateBlockItemRequestDto;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockScope;
import com.formquid.block.repository.BlockRepository;
import com.formquid.common.Category;
import com.formquid.common.service.CategoryCatalogService;
import com.formquid.content.dto.BlockContentItemRowDto;
import com.formquid.content.dto.ContentItemRowDto;
import com.formquid.content.dto.ContentItemResponseDto;
import com.formquid.content.dto.CreateTextItemRequestDto;
import com.formquid.content.dto.ReorderContentItemRequestDto;
import com.formquid.content.dto.ReorderContentRequestDto;
import com.formquid.content.dto.SurveyContentItemRequestDto;
import com.formquid.content.dto.UpdateSurveyContentRequestDto;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.question.dto.QuestionDraftDto;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.question.service.QuestionService;
import com.formquid.survey.entity.Survey;
import com.formquid.template.entity.Template;
import com.formquid.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import com.formquid.content.dto.*;
import com.formquid.question.entity.QuestionScope;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {
    @Test
    void builderBlocks_loadsMixedContentInBatchesAndIncludesEmptyBlocks() {
        Block a=Block.builder().id(20L).build(), b=Block.builder().id(21L).build();
        var rows=List.of(new BlockContentItemRowDto(20L,30L,ContentItemType.TEXT,1,null,"Intro"),
            new BlockContentItemRowDto(20L,31L,ContentItemType.QUESTION,2,true,null));
        when(contentItemRepository.findBlockContentRows(List.of(20L,21L))).thenReturn(rows);
        when(questionRepository.findByParentItemIdsWithDetails(List.of(31L))).thenReturn(List.of(
            Question.builder().id(40L).parentItem(ContentItem.builder().id(31L).build()).build()));
        var result=contentService.findBlocksContent(List.of(a,b));
        assertThat(result.get(20L)).hasSize(2);
        assertThat(result.get(20L).getFirst().textBody()).isEqualTo("Intro");
        assertThat(result.get(21L)).isEmpty();
        assertThat(contentService.findBlocksContent(List.of())).isEmpty();
        verify(questionRepository).findByParentItemIdsWithDetails(List.of(31L));
    }

    @Test
    void replaceSurveyContent_rejectsInvalidStructureBeforeDeletingExistingContent() {
        Survey survey=Survey.builder().id(1L).build();
        assertThatThrownBy(() -> contentService.replaceSurveyContent(survey,null,null)).hasMessageContaining("obligatoria");
        var text=new SurveyContentItemRequestDto(ContentItemType.TEXT,1,null,null," ",null,null,null);
        assertThatThrownBy(() -> contentService.replaceSurveyContent(survey,new UpdateSurveyContentRequestDto(List.of(text)),null)).hasMessageContaining("no pueden estar vacíos");
        var question=new SurveyContentItemRequestDto(ContentItemType.QUESTION,1,true,null,null,null,null,null);
        assertThatThrownBy(() -> contentService.replaceSurveyContent(survey,new UpdateSurveyContentRequestDto(List.of(question)),null)).hasMessageContaining("único origen");
        var block=new SurveyContentItemRequestDto(ContentItemType.BLOCK,1,null,null,null,null,null,null);
        assertThatThrownBy(() -> contentService.replaceSurveyContent(survey,new UpdateSurveyContentRequestDto(List.of(block)),null)).hasMessageContaining("único origen");
        verify(contentItemRepository,never()).deleteAll(any());
    }
    private QuestionDraftDto draft() {
        return new QuestionDraftDto("Pregunta",null,QuestionType.TEXT,EvaluationDirection.NEUTRAL,null,null,List.of(),List.of());
    }

    @Test
    void addQuestionsToEveryContainer_preservesOriginPositionAndRequirement() {
        Survey survey=Survey.builder().id(1L).build();
        Template template=Template.builder().id(2L).build();
        Block block=Block.builder().id(3L).scope(BlockScope.CATALOG).build();
        User user=User.builder().id(4L).build();
        Question fixed=Question.builder().id(9L).scope(QuestionScope.CATALOG_FIXED).build();
        when(questionService.getQuestion(9L)).thenReturn(fixed);
        when(contentItemRepository.save(any())).thenAnswer(i -> { ContentItem v=i.getArgument(0); v.setId(10L); return v; });
        var local=new CreateQuestionItemRequestDto(1,true,draft());
        var catalog=new AddFixedQuestionItemRequestDto(1,false,9L);
        assertThat(contentService.addLocalQuestionToSurvey(survey,local,user).required()).isTrue();
        assertThat(contentService.addFixedQuestionToSurvey(survey,catalog,user).required()).isFalse();
        contentService.addLocalQuestionToTemplate(template,local,user);
        contentService.addFixedQuestionToTemplate(template,catalog,user);
        contentService.addLocalQuestionToBlock(block,local,user);
        contentService.addFixedQuestionToBlock(block,catalog,user);
        assertThat(contentService.addTextToTemplate(template,new CreateTextItemRequestDto(2," Texto ")).textBody()).isEqualTo("Texto");
        assertThat(contentService.addTextToBlock(block,new CreateTextItemRequestDto(2," Otro ")).textBody()).isEqualTo("Otro");
        verify(questionService,times(3)).createFixedSnapshot(eq(fixed),any(),eq(user));
        verify(questionService,times(3)).createLocalQuestion(eq(local.question()),any(),eq(user));
        block.setScope(BlockScope.SNAPSHOT);
        assertThatThrownBy(() -> contentService.addTextToBlock(block,new CreateTextItemRequestDto(1,"x"))).hasMessageContaining("bloque copiado");
        assertThatThrownBy(() -> contentService.addLocalQuestionToBlock(block,local,user)).hasMessageContaining("bloque copiado");
        assertThatThrownBy(() -> contentService.addFixedQuestionToBlock(block,catalog,user)).hasMessageContaining("bloque copiado");
    }

    @Test
    void localBlocks_inheritCategoryAndCreateMixedContent() {
        Survey survey=Survey.builder().id(1L).category(Category.OTROS).build();
        Template template=Template.builder().id(2L).category(Category.OTROS).build();
        var request=new CreateBlockRequestDto(" Bloque "," descripción ",List.of(
            new CreateBlockItemRequestDto(ContentItemType.TEXT,1,null,null,null," Texto "),
            new CreateBlockItemRequestDto(ContentItemType.QUESTION,2,true,draft(),null,null),
            new CreateBlockItemRequestDto(ContentItemType.QUESTION,3,false,null,9L,null)),null);
        when(contentItemRepository.save(any())).thenAnswer(i -> { ContentItem v=i.getArgument(0); v.setId(10L); return v; });
        when(blockRepository.save(any())).thenAnswer(i -> { Block b=i.getArgument(0); b.setId(20L); return b; });
        when(questionService.getQuestion(9L)).thenReturn(Question.builder().scope(QuestionScope.CATALOG_FIXED).build());
        contentService.addLocalBlockToSurvey(survey,1,request,null);
        contentService.addLocalBlockToTemplate(template,1,request,null);
        var blocks=ArgumentCaptor.forClass(Block.class);
        verify(blockRepository,times(2)).save(blocks.capture());
        assertThat(blocks.getAllValues()).allSatisfy(b -> {
            assertThat(b.getCategory()).isEqualTo(Category.OTROS);
            assertThat(b.getScope()).isEqualTo(BlockScope.SNAPSHOT);
            assertThat(b.getName()).isEqualTo("Bloque");
            assertThat(b.getDescription()).isEqualTo("descripción");
        });
        verify(questionService,times(2)).createFixedSnapshot(any(),any(),isNull());
        verify(questionService,times(2)).createLocalQuestion(any(),any(),isNull());
    }

    @Test
    void catalogBlockCopy_copiesQuestionsAndTextsAndCanBeDeleted() {
        Survey survey=Survey.builder().id(1L).build();
        Template template=Template.builder().id(2L).build();
        Block source=Block.builder().id(3L).scope(BlockScope.CATALOG).name("Original").build();
        ContentItem text=ContentItem.builder().id(30L).itemType(ContentItemType.TEXT).position(1).textBody("Intro").build();
        ContentItem question=ContentItem.builder().id(31L).itemType(ContentItemType.QUESTION).position(2).required(true).build();
        Question original=Question.builder().id(40L).build();
        when(blockRepository.findById(3L)).thenReturn(Optional.of(source));
        when(contentItemRepository.save(any())).thenAnswer(i -> { ContentItem v=i.getArgument(0); v.setId(10L); return v; });
        when(blockRepository.save(any())).thenAnswer(i -> { Block b=i.getArgument(0); b.setId(20L); return b; });
        when(contentItemRepository.findByParentBlockIdOrderByPositionAsc(3L)).thenReturn(List.of(text,question));
        when(questionRepository.findByParentItemId(31L)).thenReturn(Optional.of(original));
        contentService.addBlockToSurvey(survey,new AddBlockItemRequestDto(1,3L),null);
        contentService.addBlockToTemplate(template,new AddBlockItemRequestDto(1,3L),null);
        verify(questionService,times(2)).copyAsLocal(eq(original),argThat(v -> v.getRequired() && v.getPosition()==2),isNull());
        ContentItem parent=ContentItem.builder().id(50L).survey(survey).itemType(ContentItemType.BLOCK).position(1).build();
        when(contentItemRepository.findById(50L)).thenReturn(Optional.of(parent));
        when(blockRepository.findByParentItemId(50L)).thenReturn(Optional.of(source));
        contentService.deleteSurveyItem(survey,50L);
        verify(blockRepository).delete(source);
        verify(questionRepository).delete(original);
        assertThat(parent.getBlock()).isNull();
    }

    @Test
    void updateSurveyItems_checksOwnershipAndTypeBeforeChangingContent() {
        Survey survey=Survey.builder().id(1L).build();
        ContentItem item=surveyItem(10L,survey,1);
        when(contentItemRepository.findById(10L)).thenReturn(Optional.of(item));
        assertThat(contentService.updateSurveyText(survey,10L,new UpdateTextItemRequestDto(" Nuevo ")).textBody()).isEqualTo("Nuevo");
        assertThatThrownBy(() -> contentService.updateSurveyQuestionRequired(survey,10L,new UpdateRequiredRequestDto(true))).hasMessageContaining("Solo las preguntas");
        assertThatThrownBy(() -> contentService.updateSurveyQuestion(survey,10L,new UpdateQuestionItemRequestDto(false,draft()))).hasMessageContaining("no es una pregunta");
        item.setItemType(ContentItemType.QUESTION);
        Question question=Question.builder().id(20L).build();
        when(questionRepository.findByParentItemId(10L)).thenReturn(Optional.of(question));
        contentService.updateSurveyQuestion(survey,10L,new UpdateQuestionItemRequestDto(false,draft()));
        verify(questionService).updateSurveyQuestion(eq(question),any());
        assertThat(contentService.updateSurveyQuestionRequired(survey,10L,new UpdateRequiredRequestDto(true)).required()).isTrue();
        assertThatThrownBy(() -> contentService.updateSurveyText(survey,10L,new UpdateTextItemRequestDto("x"))).hasMessageContaining("no es texto");
        item.setSurvey(Survey.builder().id(99L).build());
        assertThatThrownBy(() -> contentService.updateSurveyText(survey,10L,new UpdateTextItemRequestDto("x"))).hasMessageContaining("no pertenece");
        assertThatThrownBy(() -> contentService.deleteSurveyItem(survey,99L)).hasMessageContaining("no encontrado");
    }

    @Test
    void reorder_changesPositionsAndRejectsIncompleteOrDuplicateLists() {
        Survey survey=Survey.builder().id(1L).build();
        ContentItem a=surveyItem(10L,survey,1), b=surveyItem(11L,survey,2);
        when(contentItemRepository.findBySurveyIdOrderByPositionAsc(1L)).thenReturn(List.of(a,b));
        contentService.reorderSurveyContent(survey,new ReorderContentRequestDto(List.of(new ReorderContentItemRequestDto(11L,1),new ReorderContentItemRequestDto(10L,2))));
        assertThat(a.getPosition()).isEqualTo(2); assertThat(b.getPosition()).isEqualTo(1);
        assertThatThrownBy(() -> contentService.reorderSurveyContent(survey,null)).hasMessageContaining("incluir sus elementos");
        assertThatThrownBy(() -> contentService.reorderSurveyContent(survey,new ReorderContentRequestDto(List.of(new ReorderContentItemRequestDto(10L,1))))).hasMessageContaining("todos los elementos");
        assertThatThrownBy(() -> contentService.reorderSurveyContent(survey,new ReorderContentRequestDto(List.of(new ReorderContentItemRequestDto(10L,1),new ReorderContentItemRequestDto(10L,2))))).hasMessageContaining("duplicadas");
    }

    @Test
    void hasQuestion_recognizesQuestionsInsideBlocksAndHandlesEmptyContent() {
        ContentItem item=ContentItem.builder().id(10L).position(1).itemType(ContentItemType.BLOCK).build();
        Block block=Block.builder().id(20L).build();
        assertThat(contentService.hasQuestion(List.of())).isFalse();
        assertThat(contentService.hasQuestion(List.of(item))).isFalse();
        when(blockRepository.findByParentItemId(10L)).thenReturn(Optional.of(block));
        when(contentItemRepository.existsByParentBlockIdAndItemType(20L,ContentItemType.QUESTION)).thenReturn(true);
        assertThat(contentService.hasQuestion(List.of(item))).isTrue();
    }

    @Mock
    private ContentItemRepository contentItemRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private QuestionService questionService;
    @Mock
    private CategoryCatalogService categoryCatalogService;

    @InjectMocks
    private ContentService contentService;

    @Test
    void copiedSnapshotKeepsContentWithoutSourceReference() {
        Block templateSnapshot = Block.builder()
            .id(2L)
            .scope(BlockScope.SNAPSHOT)
            .category(Category.DATOS_PERSONALES)
            .name("Copia")
            .build();
        ContentItem surveyItem = ContentItem.builder().id(10L).itemType(ContentItemType.BLOCK).build();

        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(contentItemRepository.findByParentBlockIdOrderByPositionAsc(2L)).thenReturn(List.of());

        Block copy = contentService.copyExistingBlock(templateSnapshot, surveyItem, null);

        assertThat(copy.getScope()).isEqualTo(BlockScope.SNAPSHOT);
        assertThat(copy.getName()).isEqualTo("Copia");
        assertThat(copy.getCategory()).isEqualTo(Category.DATOS_PERSONALES);
    }

    @Test
    void insertingSurveyItemCompactsExistingPositionsAndMakesRoom() {
        Survey survey = Survey.builder().id(1L).build();
        ContentItem first = surveyItem(10L, survey, 1);
        ContentItem second = surveyItem(11L, survey, 4);
        when(contentItemRepository.findBySurveyIdOrderByPositionAsc(1L)).thenReturn(List.of(first, second));
        when(contentItemRepository.save(any(ContentItem.class))).thenAnswer(invocation -> {
            ContentItem item = invocation.getArgument(0);
            item.setId(12L);
            return item;
        });

        ContentItemResponseDto response = contentService.addTextToSurvey(
            survey,
            new CreateTextItemRequestDto(2, "Texto")
        );

        assertThat(first.getPosition()).isEqualTo(1);
        assertThat(second.getPosition()).isEqualTo(3);
        assertThat(response.position()).isEqualTo(2);
    }

    @Test
    void reorderRejectsNonConsecutivePositions() {
        Survey survey = Survey.builder().id(1L).build();
        when(contentItemRepository.findBySurveyIdOrderByPositionAsc(1L)).thenReturn(List.of(
            surveyItem(10L, survey, 1),
            surveyItem(11L, survey, 2)
        ));

        ReorderContentRequestDto request = new ReorderContentRequestDto(List.of(
            new ReorderContentItemRequestDto(10L, 1),
            new ReorderContentItemRequestDto(11L, 3)
        ));

        assertThatThrownBy(() -> contentService.reorderSurveyContent(survey, request))
            .hasMessage("Las posiciones deben ser consecutivas desde 1");
    }

    @Test
    void templateContentUsesProjectionInsteadOfLoadingContentItemEntities() {
        Template template = Template.builder().id(3L).build();
        when(contentItemRepository.findTemplateContentRows(3L)).thenReturn(List.of(
            new ContentItemRowDto(10L, ContentItemType.TEXT, 1, null, "Texto")
        ));

        List<ContentItemResponseDto> content = contentService.findTemplateContent(template);

        assertThat(content).hasSize(1);
        assertThat(content.getFirst().textBody()).isEqualTo("Texto");
        verify(contentItemRepository).findTemplateContentRows(3L);
        verify(contentItemRepository, never()).findByTemplateIdOrderByPositionAsc(any());
    }

    @Test
    void surveyContentLoadsQuestionsAndBlocksInBatches() {
        ContentItem blockParent = ContentItem.builder().id(11L).build();
        Block block = Block.builder()
            .id(20L)
            .scope(BlockScope.SNAPSHOT)
            .parentItem(blockParent)
            .name("Bloque")
            .build();
        Question surveyQuestion = Question.builder()
            .id(30L)
            .parentItem(ContentItem.builder().id(10L).build())
            .build();
        Question blockQuestion = Question.builder()
            .id(31L)
            .parentItem(ContentItem.builder().id(12L).build())
            .build();

        when(contentItemRepository.findSurveyContentRows(1L)).thenReturn(List.of(
            new ContentItemRowDto(10L, ContentItemType.QUESTION, 1, true, null),
            new ContentItemRowDto(11L, ContentItemType.BLOCK, 2, null, null)
        ));
        when(blockRepository.findByParentItemIdsWithDetails(List.of(11L))).thenReturn(List.of(block));
        when(contentItemRepository.findBlockContentRows(List.of(20L))).thenReturn(List.of(
            new BlockContentItemRowDto(20L, 12L, ContentItemType.QUESTION, 1, false, null)
        ));
        when(questionRepository.findByParentItemIdsWithDetails(List.of(10L, 12L)))
            .thenReturn(List.of(surveyQuestion, blockQuestion));

        List<ContentItemResponseDto> content = contentService.findSurveyContent(1L);

        assertThat(content).hasSize(2);
        assertThat(content.get(1).block()).isNotNull();
        assertThat(content.get(1).block().contentItems()).hasSize(1);
        verify(contentItemRepository).findSurveyContentRows(1L);
        verify(blockRepository).findByParentItemIdsWithDetails(List.of(11L));
        verify(contentItemRepository).findBlockContentRows(List.of(20L));
        verify(questionRepository).findByParentItemIdsWithDetails(List.of(10L, 12L));
        verify(contentItemRepository, never()).findBySurveyIdOrderByPositionAsc(any());
        verify(questionRepository, never()).findByParentItemId(any());
        verify(blockRepository, never()).findByParentItemId(any());
    }

    @Test
    void deleteSurveyContent_clearsDeletedQuestionBeforeNextLookup() {
        Survey survey = Survey.builder().id(1L).build();
        ContentItem first = ContentItem.builder()
            .id(10L).survey(survey).itemType(ContentItemType.QUESTION).position(1).required(true).build();
        ContentItem second = ContentItem.builder()
            .id(11L).survey(survey).itemType(ContentItemType.QUESTION).position(2).required(true).build();
        Question firstQuestion = Question.builder().id(20L).parentItem(first).build();
        Question secondQuestion = Question.builder().id(21L).parentItem(second).build();
        first.setQuestion(firstQuestion);
        second.setQuestion(secondQuestion);

        when(contentItemRepository.findBySurveyIdOrderByPositionAsc(1L)).thenReturn(List.of(first, second));
        when(questionRepository.findByParentItemId(10L)).thenReturn(Optional.of(firstQuestion));
        when(questionRepository.findByParentItemId(11L)).thenAnswer(invocation -> {
            assertThat(first.getQuestion()).isNull();
            return Optional.of(secondQuestion);
        });

        contentService.deleteSurveyContent(survey);

        assertThat(second.getQuestion()).isNull();
        verify(questionRepository).delete(firstQuestion);
        verify(questionRepository).delete(secondQuestion);
        verify(contentItemRepository).deleteAll(List.of(first, second));
    }

    @Test
    void replacingSurveyContentSavesFinalPositionsWithoutReorderingEachItem() {
        Survey survey = Survey.builder().id(1L).category(Category.OTROS).build();
        User creator = User.builder().id(2L).build();
        QuestionDraftDto question = new QuestionDraftDto(
            "Pregunta", null, QuestionType.TEXT, EvaluationDirection.NEUTRAL, null, null, List.of(), List.of()
        );
        CreateBlockRequestDto block = new CreateBlockRequestDto(
            "Bloque", null,
            List.of(new CreateBlockItemRequestDto(ContentItemType.TEXT, 1, null, null, null, "Dentro")),
            null
        );
        UpdateSurveyContentRequestDto request = new UpdateSurveyContentRequestDto(List.of(
            new SurveyContentItemRequestDto(ContentItemType.BLOCK, 3, null, null, null, null, null, block),
            new SurveyContentItemRequestDto(ContentItemType.TEXT, 1, null, null, "Introducción", null, null, null),
            new SurveyContentItemRequestDto(ContentItemType.QUESTION, 2, true, question, null, null, null, null)
        ));
        when(contentItemRepository.findBySurveyIdOrderByPositionAsc(1L)).thenReturn(List.of());
        when(contentItemRepository.save(any(ContentItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        contentService.replaceSurveyContent(survey, request, creator);

        ArgumentCaptor<ContentItem> savedItems = ArgumentCaptor.forClass(ContentItem.class);
        verify(contentItemRepository, times(4)).save(savedItems.capture());
        assertThat(savedItems.getAllValues().stream()
            .filter(item -> item.getSurvey() != null)
            .map(ContentItem::getPosition))
            .containsExactly(1, 2, 3);
        verify(contentItemRepository).findBySurveyIdOrderByPositionAsc(1L);
        verify(contentItemRepository).flush();
        verify(questionService).createLocalQuestion(question, savedItems.getAllValues().get(1), creator);
    }

    private ContentItem surveyItem(Long id, Survey survey, int position) {
        return ContentItem.builder()
            .id(id)
            .survey(survey)
            .itemType(ContentItemType.TEXT)
            .position(position)
            .textBody("Texto")
            .build();
    }
}
