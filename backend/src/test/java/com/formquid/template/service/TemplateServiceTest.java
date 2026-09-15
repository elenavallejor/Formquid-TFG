package com.formquid.template.service;

import com.formquid.block.repository.BlockRepository;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

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

    @InjectMocks
    private TemplateService templateService;

    @Test
    void rejectsTitleLongerThanDatabaseLimit() {
        CreateTemplateRequestDto request = new CreateTemplateRequestDto(
            "a".repeat(201), null, null, List.of()
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
            "Plantilla", null, null, List.of(text)
        );

        assertThatThrownBy(() -> templateService.validateDraft(request))
            .hasMessage("Una plantilla debe contener preguntas o bloques");
    }
}
