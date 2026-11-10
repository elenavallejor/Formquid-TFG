package com.formquid.template.service;

import com.formquid.block.repository.BlockRepository;
import com.formquid.common.service.CategoryCatalogService;
import com.formquid.block.service.BlockService;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.content.service.ContentService;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.question.service.QuestionService;
import com.formquid.template.dto.CreateTemplateItemRequestDto;
import com.formquid.template.dto.CreateTemplateRequestDto;
import com.formquid.template.repository.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.formquid.common.Category;
import com.formquid.common.dto.CatalogSearchField;
import com.formquid.template.entity.Template;
import com.formquid.user.entity.*;
import com.formquid.content.entity.ContentItem;
import com.formquid.survey.entity.Survey;
import com.formquid.question.entity.Question;
import com.formquid.block.entity.Block;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.dto.CreateBlockItemRequestDto;
import com.formquid.question.dto.QuestionDraftDto;
import com.formquid.question.entity.QuestionType;
import org.springframework.data.domain.Page;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {
    private final User owner = User.builder().id(1L).name("Autora").role(UserRole.EDITOR).build();

    @Test
    void createAndUpdate_preserveMetadataAndDispatchEveryContentType() {
        var draft = new QuestionDraftDto("Pregunta", null, QuestionType.TEXT, null, null, null, List.of(), List.of());
        var block = new CreateBlockRequestDto("Bloque", null, List.of(
            new CreateBlockItemRequestDto(ContentItemType.QUESTION, 1, true, draft, null, null)), Category.OTROS);
        var items = List.of(
            new CreateTemplateItemRequestDto(ContentItemType.TEXT, 1, null, null, " Intro ", null, null, null),
            new CreateTemplateItemRequestDto(ContentItemType.QUESTION, 2, true, null, null, 9L, null, null),
            new CreateTemplateItemRequestDto(ContentItemType.QUESTION, 3, false, draft, null, null, null, null),
            new CreateTemplateItemRequestDto(ContentItemType.BLOCK, 4, null, null, null, null, 8L, null),
            new CreateTemplateItemRequestDto(ContentItemType.BLOCK, 5, null, null, null, null, null, block));
        var request = new CreateTemplateRequestDto(" Plantilla ", " Descripción ", " imagen ", items, Category.OTROS);
        when(templateRepository.save(any())).thenAnswer(i -> { Template t=i.getArgument(0); t.setId(3L); return t; });
        when(contentService.hasQuestion(anyList())).thenReturn(true);
        var result = templateService.create(request, owner);
        assertThat(result.title()).isEqualTo("Plantilla");
        assertThat(result.description()).isEqualTo("Descripción");
        assertThat(result.imageKey()).isEqualTo("imagen");
        verify(contentService).addFixedQuestionToTemplate(any(), any(), eq(owner));
        verify(contentService).addLocalQuestionToTemplate(any(), any(), eq(owner));
        verify(contentService).addBlockToTemplate(any(), any(), eq(owner));
        verify(contentService).addLocalBlockToTemplate(any(), eq(5), eq(block), eq(owner));
        Template saved=Template.builder().id(3L).createdBy(owner).build();
        when(templateRepository.findById(3L)).thenReturn(Optional.of(saved));
        assertThat(templateService.update(3L, request, owner).title()).isEqualTo("Plantilla");
        assertThat(saved.getUpdatedAt()).isNotNull();
        verify(contentService).deleteTemplateContent(saved);
    }

    @Test
    void findAndDelete_requireExistingTemplateAndOwnerOrSuperadmin() {
        Template template=Template.builder().id(3L).title("Título").createdBy(owner).build();
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        assertThat(templateService.findById(3L).title()).isEqualTo("Título");
        assertThatThrownBy(() -> templateService.delete(3L, User.builder().id(2L).role(UserRole.EDITOR).build()))
            .hasMessageContaining("propias plantillas");
        templateService.delete(3L, owner);
        templateService.delete(3L, User.builder().role(UserRole.SUPERADMIN).build());
        verify(templateRepository, times(2)).delete(template);
        assertThatThrownBy(() -> templateService.findById(99L)).hasMessage("Plantilla no encontrada");
    }

    @Test
    void findAll_normalizesSearchAndSupportsDefaultFields() {
        when(templateRepository.findCatalogSummaries(any(), anyBoolean(), anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any()))
            .thenReturn(Page.empty());
        assertThat(templateService.findAll(0, 10, owner, true, " Prueba ", Set.of(CatalogSearchField.TITLE), null, null).content()).isEmpty();
        templateService.findAll(0, 10, owner, false, null, null, null, null);
        verify(templateRepository).findCatalogSummaries(eq(1L), eq(true), eq("prueba"), eq(true), eq(false), eq(false), isNull(), any());
    }

    @Test
    void copyTemplateIntoSurvey_copiesTextQuestionsAndBlocks() {
        Template template=Template.builder().id(3L).build();
        Survey survey=Survey.builder().id(4L).build();
        var text=ContentItem.builder().id(10L).position(1).itemType(ContentItemType.TEXT).textBody("Texto").build();
        var question=ContentItem.builder().id(11L).position(2).required(true).itemType(ContentItemType.QUESTION).build();
        var block=ContentItem.builder().id(12L).position(3).itemType(ContentItemType.BLOCK).build();
        Question source=Question.builder().id(20L).build();
        Block sourceBlock=Block.builder().id(21L).build();
        when(contentItemRepository.findByTemplateIdOrderByPositionAsc(3L)).thenReturn(List.of(text,question,block));
        when(questionRepository.findByParentItemId(11L)).thenReturn(Optional.of(source));
        when(blockRepository.findByParentItemId(12L)).thenReturn(Optional.of(sourceBlock));
        when(contentItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        templateService.copyTemplateIntoSurvey(template,survey,owner);
        verify(questionService).copyAsLocal(eq(source), argThat(i -> i.getSurvey()==survey && i.getRequired()), eq(owner));
        verify(contentService).copyExistingBlock(eq(sourceBlock), argThat(i -> i.getPosition()==3), eq(owner));
        when(questionRepository.findByParentItemId(11L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> templateService.copyTemplateIntoSurvey(template,survey,owner)).hasMessageContaining("pregunta incompleta");
    }

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private ContentItemRepository contentItemRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private BlockService blockService;
    @Mock
    private ContentService contentService;
    @Mock
    private QuestionService questionService;
    @Mock
    private CategoryCatalogService categoryCatalogService;

    @InjectMocks
    private TemplateService templateService;

    @Test
    void createRequiresCategory() {
        var question = new CreateTemplateItemRequestDto(
            ContentItemType.QUESTION, 1, true, null, null, 11L, null, null
        );
        var request = new CreateTemplateRequestDto("Plantilla", null, null,
            List.of(question), null);

        assertThatThrownBy(() -> templateService.create(request, null))
            .hasMessageContaining("categoría de la plantilla es obligatoria");
    }

    @Test
    void rejectsTitleLongerThanDatabaseLimit() {
        CreateTemplateRequestDto request = new CreateTemplateRequestDto(
            "a".repeat(201), null, null, List.of(), null
        );

        assertThatThrownBy(() -> templateService.validateDraft(request))
            .hasMessage("El título de la plantilla no puede superar 200 caracteres");
    }

    @Test
    void rejectsTemplateContainingOnlyText() {
        CreateTemplateItemRequestDto text = new CreateTemplateItemRequestDto(
            ContentItemType.TEXT,
            1,
            null,
            null,
            "Introducción",
            null,
            null,
            null
        );

        CreateTemplateRequestDto request = new CreateTemplateRequestDto(
            "Plantilla", null, null, List.of(text), null
        );

        assertThatThrownBy(() -> templateService.validateDraft(request))
            .hasMessage("Una plantilla debe contener preguntas o bloques");
    }
}
