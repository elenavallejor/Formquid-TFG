package com.formquid.content.service;

import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockScope;
import com.formquid.block.repository.BlockRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.content.dto.AddBlockItemRequestDto;
import com.formquid.content.dto.AddFixedQuestionItemRequestDto;
import com.formquid.content.dto.ContentItemResponseDto;
import com.formquid.content.dto.CreateQuestionItemRequestDto;
import com.formquid.content.dto.CreateTextItemRequestDto;
import com.formquid.content.dto.ReorderContentRequestDto;
import com.formquid.content.dto.UpdateQuestionItemRequestDto;
import com.formquid.content.dto.UpdateRequiredRequestDto;
import com.formquid.content.dto.UpdateTextItemRequestDto;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionScope;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.question.service.QuestionService;
import com.formquid.survey.entity.Survey;
import com.formquid.template.entity.Template;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentItemRepository contentItemRepository;
    private final QuestionRepository questionRepository;
    private final BlockRepository blockRepository;
    private final QuestionService questionService;

    public List<ContentItemResponseDto> findSurveyContent(Long surveyId) {
        return contentItemRepository.findBySurveyIdOrderByPositionAsc(surveyId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ContentItemResponseDto> findTemplateContent(Template template) {
        return contentItemRepository.findByTemplateIdOrderByPositionAsc(template.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ContentItemResponseDto> findBlockContent(Block block) {
        return contentItemRepository.findByParentBlockIdOrderByPositionAsc(block.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Recibe texto y encuesta, devuelve el elemento creado.
     */
    @Transactional
    public ContentItemResponseDto addTextToSurvey(Survey survey, CreateTextItemRequestDto dto) {
        ContentItem item = baseSurveyItem(survey, ContentItemType.TEXT, dto.position());
        item.setTextBody(dto.textBody().trim());
        return toResponse(contentItemRepository.save(item));
    }

    @Transactional
    public ContentItemResponseDto addLocalQuestionToSurvey(
        Survey survey,
        CreateQuestionItemRequestDto dto,
        User creator
    ) {
        ContentItem item = baseSurveyItem(survey, ContentItemType.QUESTION, dto.position());
        item.setRequired(dto.required());
        ContentItem savedItem = contentItemRepository.save(item);
        questionService.createLocalQuestion(dto.question(), savedItem, creator);
        return toResponse(savedItem);
    }

    @Transactional
    public ContentItemResponseDto addFixedQuestionToSurvey(
        Survey survey,
        AddFixedQuestionItemRequestDto dto,
        User creator
    ) {
        ContentItem item = baseSurveyItem(survey, ContentItemType.QUESTION, dto.position());
        item.setRequired(dto.required());
        ContentItem savedItem = contentItemRepository.save(item);
        Question fixed = getCatalogFixedQuestion(dto.fixedQuestionId());
        questionService.createFixedSnapshot(fixed, savedItem, creator);
        return toResponse(savedItem);
    }

    @Transactional
    public ContentItemResponseDto addBlockToSurvey(Survey survey, AddBlockItemRequestDto dto, User creator) {
        ContentItem item = baseSurveyItem(survey, ContentItemType.BLOCK, dto.position());
        ContentItem savedItem = contentItemRepository.save(item);
        Block catalogBlock = getCatalogBlock(dto.blockId());
        copyExistingBlock(catalogBlock, savedItem, creator);
        return toResponse(savedItem);
    }

    @Transactional
    public ContentItemResponseDto updateSurveyText(Survey survey, Long itemId, UpdateTextItemRequestDto dto) {
        ContentItem item = getSurveyItem(survey, itemId);
        if (item.getItemType() != ContentItemType.TEXT) {
            throw new ApiException(HttpStatus.CONFLICT, "El elemento indicado no es texto");
        }
        item.setTextBody(dto.textBody().trim());
        return toResponse(item);
    }

    @Transactional
    public ContentItemResponseDto updateSurveyQuestion(Survey survey, Long itemId, UpdateQuestionItemRequestDto dto) {
        ContentItem item = getSurveyItem(survey, itemId);
        if (item.getItemType() != ContentItemType.QUESTION) {
            throw new ApiException(HttpStatus.CONFLICT, "El elemento indicado no es una pregunta");
        }
        Question question = questionRepository.findByParentItemId(item.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "El elemento no tiene pregunta asociada"));
        item.setRequired(dto.required());
        questionService.updateSurveyQuestion(question, dto.question());
        return toResponse(item);
    }

    @Transactional
    public ContentItemResponseDto updateSurveyQuestionRequired(Survey survey, Long itemId, UpdateRequiredRequestDto dto) {
        ContentItem item = getSurveyItem(survey, itemId);
        if (item.getItemType() != ContentItemType.QUESTION) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo las preguntas tienen campo required");
        }
        item.setRequired(dto.required());
        return toResponse(item);
    }

    @Transactional
    public void deleteSurveyItem(Survey survey, Long itemId) {
        ContentItem item = getSurveyItem(survey, itemId);
        contentItemRepository.delete(item);
    }

    @Transactional
    public List<ContentItemResponseDto> reorderSurveyContent(Survey survey, ReorderContentRequestDto dto) {
        List<ContentItem> currentItems = contentItemRepository.findBySurveyIdOrderByPositionAsc(survey.getId());
        Set<Long> currentIds = currentItems.stream()
            .map(ContentItem::getId)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<Long> requestedIds = dto.items().stream()
            .map(item -> item.itemId())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<Integer> requestedPositions = dto.items().stream()
            .map(item -> item.position())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        if (requestedIds.size() != dto.items().size() || requestedPositions.size() != dto.items().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El reordenado contiene elementos o posiciones duplicadas");
        }
        if (!currentIds.equals(requestedIds)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El reordenado debe incluir todos los elementos principales de la encuesta");
        }

        int temporaryPosition = currentItems.size() + dto.items().size() + 1000;
        for (Long itemId : requestedIds) {
            contentItemRepository.updatePosition(itemId, temporaryPosition++);
        }
        dto.items().forEach(item -> contentItemRepository.updatePosition(item.itemId(), item.position()));
        return findSurveyContent(survey.getId());
    }

    @Transactional
    public ContentItemResponseDto addTextToTemplate(Template template, CreateTextItemRequestDto dto) {
        ContentItem item = baseTemplateItem(template, ContentItemType.TEXT, dto.position());
        item.setTextBody(dto.textBody().trim());
        return toResponse(contentItemRepository.save(item));
    }

    @Transactional
    public ContentItemResponseDto addLocalQuestionToTemplate(Template template, CreateQuestionItemRequestDto dto, User creator) {
        ContentItem item = baseTemplateItem(template, ContentItemType.QUESTION, dto.position());
        item.setRequired(dto.required());
        ContentItem savedItem = contentItemRepository.save(item);
        questionService.createLocalQuestion(dto.question(), savedItem, creator);
        return toResponse(savedItem);
    }

    @Transactional
    public ContentItemResponseDto addFixedQuestionToTemplate(Template template, AddFixedQuestionItemRequestDto dto, User creator) {
        ContentItem item = baseTemplateItem(template, ContentItemType.QUESTION, dto.position());
        item.setRequired(dto.required());
        ContentItem savedItem = contentItemRepository.save(item);
        questionService.createFixedSnapshot(getCatalogFixedQuestion(dto.fixedQuestionId()), savedItem, creator);
        return toResponse(savedItem);
    }

    @Transactional
    public ContentItemResponseDto addBlockToTemplate(Template template, AddBlockItemRequestDto dto, User creator) {
        ContentItem item = baseTemplateItem(template, ContentItemType.BLOCK, dto.position());
        ContentItem savedItem = contentItemRepository.save(item);
        copyExistingBlock(getCatalogBlock(dto.blockId()), savedItem, creator);
        return toResponse(savedItem);
    }

    @Transactional
    public ContentItemResponseDto addLocalQuestionToBlock(Block block, CreateQuestionItemRequestDto dto, User creator) {
        if (block.getScope() == BlockScope.SNAPSHOT) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede modificar el contenido interno de un bloque copiado");
        }
        ContentItem item = ContentItem.builder()
            .parentBlock(block)
            .itemType(ContentItemType.QUESTION)
            .position(dto.position())
            .required(dto.required())
            .build();
        ContentItem savedItem = contentItemRepository.save(item);
        questionService.createLocalQuestion(dto.question(), savedItem, creator);
        return toResponse(savedItem);
    }

    public Block copyExistingBlock(Block source, ContentItem parentItem, User creator) {
        Block copy = Block.builder()
            .parentItem(parentItem)
            .scope(BlockScope.SNAPSHOT)
            .sourceBlock(source)
            .name(source.getName())
            .description(source.getDescription())
            .createdBy(creator)
            .build();
        Block savedBlock = blockRepository.save(copy);

        List<ContentItem> sourceItems = contentItemRepository.findByParentBlockIdOrderByPositionAsc(source.getId());
        for (ContentItem sourceItem : sourceItems) {
            if (sourceItem.getItemType() != ContentItemType.QUESTION) {
                throw new ApiException(HttpStatus.CONFLICT, "Los bloques solo pueden contener preguntas");
            }
            Question sourceQuestion = questionRepository.findByParentItemId(sourceItem.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "El bloque contiene un elemento sin pregunta asociada"));
            ContentItem copiedItem = ContentItem.builder()
                .parentBlock(savedBlock)
                .itemType(ContentItemType.QUESTION)
                .position(sourceItem.getPosition())
                .required(sourceItem.getRequired())
                .build();
            ContentItem savedItem = contentItemRepository.save(copiedItem);
            questionService.copyAsLocal(sourceQuestion, savedItem, creator);
        }
        return savedBlock;
    }

    @Transactional
    public void deleteBlockContent(Block block) {
        List<ContentItem> items = contentItemRepository.findByParentBlockIdOrderByPositionAsc(block.getId());
        for (ContentItem item : items) {
            questionRepository.findByParentItemId(item.getId()).ifPresent(questionRepository::delete);
        }
        questionRepository.flush();
        contentItemRepository.deleteAll(items);
        contentItemRepository.flush();
    }

    @Transactional
    public void deleteTemplateContent(Template template) {
        List<ContentItem> items = contentItemRepository.findByTemplateIdOrderByPositionAsc(template.getId());
        for (ContentItem item : items) {
            if (item.getItemType() == ContentItemType.QUESTION) {
                questionRepository.findByParentItemId(item.getId()).ifPresent(questionRepository::delete);
            } else if (item.getItemType() == ContentItemType.BLOCK) {
                blockRepository.findByParentItemId(item.getId()).ifPresent(block -> {
                    deleteBlockContent(block);
                    blockRepository.delete(block);
                    blockRepository.flush();
                });
            }
        }
        questionRepository.flush();
        contentItemRepository.deleteAll(items);
        contentItemRepository.flush();
    }

    private ContentItem baseSurveyItem(Survey survey, ContentItemType type, Integer position) {
        return ContentItem.builder()
            .survey(survey)
            .itemType(type)
            .position(position)
            .build();
    }

    private ContentItem baseTemplateItem(Template template, ContentItemType type, Integer position) {
        return ContentItem.builder()
            .template(template)
            .itemType(type)
            .position(position)
            .build();
    }

    private Question getCatalogFixedQuestion(Long questionId) {
        Question question = questionService.getQuestion(questionId);
        if (question.getScope() != QuestionScope.CATALOG_FIXED) {
            throw new ApiException(HttpStatus.CONFLICT, "La pregunta indicada no pertenece al catálogo fijo");
        }
        return question;
    }

    private Block getCatalogBlock(Long blockId) {
        Block block = blockRepository.findById(blockId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bloque no encontrado"));
        if (block.getScope() != BlockScope.CATALOG) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo pueden reutilizarse bloques de catálogo");
        }
        return block;
    }

    private ContentItem getSurveyItem(Survey survey, Long itemId) {
        ContentItem item = contentItemRepository.findById(itemId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Elemento de contenido no encontrado"));
        if (item.getSurvey() == null || !item.getSurvey().getId().equals(survey.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El elemento no pertenece a la encuesta");
        }
        return item;
    }

    public ContentItemResponseDto toResponse(ContentItem item) {
        Question question = questionRepository.findByParentItemId(item.getId()).orElse(null);
        Block block = blockRepository.findByParentItemId(item.getId()).orElse(null);
        return new ContentItemResponseDto(
            item.getId(),
            item.getItemType(),
            item.getPosition(),
            item.getRequired(),
            item.getTextBody(),
            question == null ? null : questionService.toResponse(question),
            block == null ? null : block.getId()
        );
    }

    public boolean hasQuestion(List<ContentItem> items) {
        return items.stream()
            .sorted(Comparator.comparing(ContentItem::getPosition))
            .anyMatch(item -> item.getItemType() == ContentItemType.QUESTION
                || (item.getItemType() == ContentItemType.BLOCK && blockContainsQuestion(item)));
    }

    private boolean blockContainsQuestion(ContentItem item) {
        return blockRepository.findByParentItemId(item.getId())
            .map(block -> contentItemRepository.existsByParentBlockIdAndItemType(block.getId(), ContentItemType.QUESTION))
            .orElse(false);
    }
}