package com.formquid.template.service;

import com.formquid.block.entity.Block;
import com.formquid.block.repository.BlockRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.content.dto.ContentItemResponseDto;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.content.service.ContentService;
import com.formquid.question.entity.Question;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.question.service.QuestionService;
import com.formquid.template.dto.CreateTemplateRequestDto;
import com.formquid.template.dto.TemplateResponseDto;
import com.formquid.template.entity.Template;
import com.formquid.template.repository.TemplateRepository;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final ContentItemRepository contentItemRepository;
    private final QuestionRepository questionRepository;
    private final BlockRepository blockRepository;
    private final ContentService contentService;
    private final QuestionService questionService;

    public List<TemplateResponseDto> findAll() {
        return templateRepository.findAllByOrderByTitleAsc().stream().map(this::toResponse).toList();
    }

    public Template getTemplate(UUID id) {
        return templateRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Plantilla no encontrada"));
    }

    /**
     * Recibe una plantilla completa y devuelve la plantilla creada.
     */
    @Transactional
    public TemplateResponseDto create(CreateTemplateRequestDto request, User creator) {
        Template template = templateRepository.save(Template.builder()
            .title(request.title().trim())
            .description(trimToNull(request.description()))
            .imageKey(trimToNull(request.imageKey()))
            .createdBy(creator)
            .build());

        if (request.texts() != null) {
            request.texts().forEach(text -> contentService.addTextToTemplate(template, text));
        }
        if (request.localQuestions() != null) {
            request.localQuestions().forEach(question -> contentService.addLocalQuestionToTemplate(template, question, creator));
        }
        if (request.fixedQuestions() != null) {
            request.fixedQuestions().forEach(question -> contentService.addFixedQuestionToTemplate(template, question, creator));
        }
        if (request.blocks() != null) {
            request.blocks().forEach(block -> contentService.addBlockToTemplate(template, block, creator));
        }

        List<ContentItem> items = contentItemRepository.findByTemplateIdOrderByPositionAsc(template.getId());
        if (!contentService.hasQuestion(items)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Una plantilla debe contener al menos una pregunta");
        }
        return toResponse(template);
    }

    /**
     * Recibe plantilla y encuesta destino. No devuelve contenido.
     */
    @Transactional
    public void copyTemplateIntoSurvey(Template template, com.formquid.survey.entity.Survey survey, User creator) {
        List<ContentItem> items = contentItemRepository.findByTemplateIdOrderByPositionAsc(template.getId());
        for (ContentItem item : items) {
            if (item.getItemType() == ContentItemType.TEXT) {
                contentService.addTextToSurvey(survey, new com.formquid.content.dto.CreateTextItemRequestDto(item.getPosition(), item.getTextBody()));
            } else if (item.getItemType() == ContentItemType.QUESTION) {
                Question sourceQuestion = questionRepository.findByParentItemId(item.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "La plantilla contiene una pregunta incompleta"));
                ContentItem newItem = contentItemRepository.save(ContentItem.builder()
                    .survey(survey)
                    .itemType(ContentItemType.QUESTION)
                    .position(item.getPosition())
                    .required(item.getRequired())
                    .build());
                questionService.copyAsLocal(sourceQuestion, newItem, creator);
            } else if (item.getItemType() == ContentItemType.BLOCK) {
                Block sourceBlock = blockRepository.findByParentItemId(item.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "La plantilla contiene un bloque incompleto"));
                ContentItem newItem = contentItemRepository.save(ContentItem.builder()
                    .survey(survey)
                    .itemType(ContentItemType.BLOCK)
                    .position(item.getPosition())
                    .build());
                contentService.copyExistingBlock(sourceBlock, newItem, creator);
            }
        }
    }

    public TemplateResponseDto toResponse(Template template) {
        List<ContentItemResponseDto> items = contentService.findTemplateContent(template)
            .stream()
            .sorted(Comparator.comparing(ContentItemResponseDto::position))
            .toList();
        return new TemplateResponseDto(
            template.getId(),
            template.getTitle(),
            template.getDescription(),
            template.getImageKey(),
            items,
            template.getCreatedAt(),
            template.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
