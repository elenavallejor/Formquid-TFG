package com.formquid.content.service;

import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockScope;
import com.formquid.block.repository.BlockRepository;
import com.formquid.content.dto.BlockContentItemRowDto;
import com.formquid.content.dto.ContentItemRowDto;
import com.formquid.content.dto.ContentItemResponseDto;
import com.formquid.content.dto.CreateTextItemRequestDto;
import com.formquid.content.dto.ReorderContentItemRequestDto;
import com.formquid.content.dto.ReorderContentRequestDto;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.question.entity.Question;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.question.service.QuestionService;
import com.formquid.survey.entity.Survey;
import com.formquid.template.entity.Template;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentItemRepository contentItemRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private QuestionService questionService;

    @InjectMocks
    private ContentService contentService;

    @Test
    void copiedSnapshotReferencesCatalogRootInsteadOfIntermediateSnapshot() {
        Block catalog = Block.builder().id(1L).scope(BlockScope.CATALOG).name("Catálogo").build();
        Block templateSnapshot = Block.builder()
            .id(2L)
            .scope(BlockScope.SNAPSHOT)
            .sourceBlock(catalog)
            .name("Copia")
            .build();
        ContentItem surveyItem = ContentItem.builder().id(10L).itemType(ContentItemType.BLOCK).build();

        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(contentItemRepository.findByParentBlockIdOrderByPositionAsc(2L)).thenReturn(List.of());

        Block copy = contentService.copyExistingBlock(templateSnapshot, surveyItem, null);

        assertThat(copy.getSourceBlock()).isSameAs(catalog);
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
