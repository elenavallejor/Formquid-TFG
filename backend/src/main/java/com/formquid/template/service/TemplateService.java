package com.formquid.template.service;

import com.formquid.block.entity.Block;
import com.formquid.block.repository.BlockRepository;
import com.formquid.block.service.BlockService;
import com.formquid.common.Category;
import com.formquid.common.service.CategoryCatalogService;
import com.formquid.common.dto.CatalogSearchField;
import com.formquid.common.dto.CatalogSortOrder;
import com.formquid.common.dto.PageResponseDto;
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
import com.formquid.template.dto.CreateTemplateItemRequestDto;
import com.formquid.template.dto.TemplateResponseDto;
import com.formquid.template.dto.TemplateSummaryDto;
import com.formquid.template.entity.Template;
import com.formquid.template.repository.TemplateRepository;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final ContentItemRepository contentItemRepository;
    private final QuestionRepository questionRepository;
    private final BlockRepository blockRepository;
    private final BlockService blockService;
    private final ContentService contentService;
    private final QuestionService questionService;
    private final CategoryCatalogService categoryCatalogService;

    @Transactional(readOnly = true)
    public PageResponseDto<TemplateSummaryDto> findAll(
        int page,
        int size,
        User user,
        boolean mine,
        String search,
        Set<CatalogSearchField> searchFields,
        CatalogSortOrder sortOrder,
        Category category
    ) {
        Set<CatalogSearchField> fields = searchFields == null ? Set.of() : searchFields;
        Pageable pageable = catalogPageable(page, size, sortOrder);
        Page<TemplateSummaryDto> templates = templateRepository.findCatalogSummaries(
            user.getId(),
            mine,
            normalizeSearch(search),
            fields.contains(CatalogSearchField.TITLE),
            fields.contains(CatalogSearchField.DESCRIPTION),
            fields.contains(CatalogSearchField.CREATOR),
            category,
            pageable
        );
        return new PageResponseDto<>(
            templates.getContent(),
            templates.getNumber(),
            templates.getSize(),
            templates.getTotalElements(),
            templates.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public TemplateResponseDto findById(Long id) {
        return toResponse(getTemplate(id));
    }

    @Transactional
    public void delete(Long id, User user) {
        Template template = getTemplate(id);
        requireManagementPermission(template.getCreatedBy(), user);
        contentService.deleteTemplateContent(template);
        templateRepository.delete(template);
    }

    public Template getTemplate(Long id) {
        return templateRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Plantilla no encontrada"));
    }

    @Transactional
    public TemplateResponseDto create(CreateTemplateRequestDto request, User creator) {
        validateDraft(request);
        categoryCatalogService.requireExisting(request.category() == null ? Category.OTROS : request.category());
        Template template = templateRepository.save(Template.builder()
            .title(request.title().trim())
            .category(request.category() == null ? Category.OTROS : request.category())
            .description(trimToNull(request.description()))
            .imageKey(trimToNull(request.imageKey()))
            .createdBy(creator)
            .build());

        request.items().stream()
            .sorted(Comparator.comparing(CreateTemplateItemRequestDto::position))
            .forEach(item -> addItem(template, item, creator));

        List<ContentItem> items = contentItemRepository.findByTemplateIdOrderByPositionAsc(template.getId());
        if (!contentService.hasQuestion(items)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Una plantilla debe contener al menos una pregunta");
        }
        return toResponse(template);
    }

    @Transactional
    public TemplateResponseDto update(Long id, CreateTemplateRequestDto request, User user) {
        Template template = getTemplate(id);
        requireManagementPermission(template.getCreatedBy(), user);
        validateDraft(request);
        if (request.category() != null) categoryCatalogService.requireExisting(request.category());
        contentService.deleteTemplateContent(template);
        template.setTitle(request.title().trim());
        if (request.category() != null) template.setCategory(request.category());
        template.setDescription(trimToNull(request.description()));
        template.setImageKey(trimToNull(request.imageKey()));
        template.setUpdatedAt(Instant.now());
        request.items().stream()
            .sorted(Comparator.comparing(CreateTemplateItemRequestDto::position))
            .forEach(item -> addItem(template, item, user));

        List<ContentItem> items = contentItemRepository.findByTemplateIdOrderByPositionAsc(template.getId());
        if (!contentService.hasQuestion(items)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Una plantilla debe contener al menos una pregunta");
        }
        return toResponse(templateRepository.save(template));
    }

    private void requireManagementPermission(User creator, User user) {
        if (user.getRole() == UserRole.SUPERADMIN) return;
        if (creator == null || !creator.getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo puedes gestionar tus propias plantillas");
        }
    }

    private void addItem(Template template, CreateTemplateItemRequestDto item, User creator) {
        if (item.itemType() == ContentItemType.TEXT) {
            contentService.addTextToTemplate(
                template,
                new com.formquid.content.dto.CreateTextItemRequestDto(item.position(), item.textBody())
            );
            return;
        }
        if (item.itemType() == ContentItemType.QUESTION) {
            if (item.fixedQuestionId() != null) {
                contentService.addFixedQuestionToTemplate(
                    template,
                    new com.formquid.content.dto.AddFixedQuestionItemRequestDto(
                        item.position(), item.required(), item.fixedQuestionId()
                    ),
                    creator
                );
            } else {
                contentService.addLocalQuestionToTemplate(
                    template,
                    new com.formquid.content.dto.CreateQuestionItemRequestDto(
                        item.position(), item.required(), item.question()
                    ),
                    creator
                );
            }
            return;
        }
        if (item.blockId() != null) {
            contentService.addBlockToTemplate(
                template,
                new com.formquid.content.dto.AddBlockItemRequestDto(item.position(), item.blockId()),
                creator
            );
        } else {
            contentService.addLocalBlockToTemplate(template, item.position(), item.block(), creator);
        }
    }

    public void validateDraft(CreateTemplateRequestDto request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La plantilla necesita un título");
        }
        if (request.title().trim().length() > 200) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El título de la plantilla no puede superar 200 caracteres");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La plantilla debe contener al menos un elemento");
        }
        validateItems(request.items());
        if (request.items().stream().noneMatch(item ->
            item.itemType() == ContentItemType.QUESTION || item.itemType() == ContentItemType.BLOCK
        )) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Una plantilla debe contener preguntas o bloques");
        }
    }

    private void validateItems(List<CreateTemplateItemRequestDto> items) {
        Set<Integer> positions = new HashSet<>();
        for (CreateTemplateItemRequestDto item : items) {
            if (item == null || item.itemType() == null || item.position() == null || item.position() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La plantilla contiene un elemento inválido");
            }
            if (!positions.add(item.position())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La plantilla contiene posiciones duplicadas");
            }
            if (item.itemType() == ContentItemType.TEXT) {
                if (item.textBody() == null || item.textBody().isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Los elementos de texto no pueden estar vacíos");
                }
                if (item.required() != null || item.question() != null || item.fixedQuestionId() != null
                    || item.blockId() != null || item.block() != null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Un texto no puede incluir configuración de pregunta o bloque");
                }
            }
            if (item.itemType() == ContentItemType.QUESTION) {
                if (item.required() == null || (item.question() == null) == (item.fixedQuestionId() == null)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Cada pregunta debe tener un único origen");
                }
                if (item.textBody() != null || item.blockId() != null || item.block() != null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Una pregunta no puede incluir configuración de texto o bloque");
                }
                if (item.question() != null) {
                    questionService.validateQuestionDraft(item.question());
                }
            }
            if (item.itemType() == ContentItemType.BLOCK) {
                if ((item.block() == null) == (item.blockId() == null)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Cada bloque debe tener un único origen");
                }
                if (item.required() != null || item.question() != null || item.fixedQuestionId() != null
                    || item.textBody() != null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Un bloque no puede incluir configuración de texto o pregunta");
                }
                if (item.block() != null) {
                    blockService.validateItems(item.block().items());
                    item.block().items().stream()
                        .filter(blockItem -> blockItem.itemType() == ContentItemType.QUESTION
                            && blockItem.fixedQuestionId() == null)
                        .forEach(blockItem -> questionService.validateQuestionDraft(blockItem.question()));
                }
            }
        }
        for (int position = 1; position <= items.size(); position++) {
            if (!positions.contains(position)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las posiciones de la plantilla deben ser consecutivas");
            }
        }
    }

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
        List<ContentItemResponseDto> items = contentService.findTemplateContent(template).stream()
            .sorted(Comparator.comparing(ContentItemResponseDto::position))
            .toList();
        User creator = template.getCreatedBy();
        return new TemplateResponseDto(
            template.getId(),
            template.getTitle(),
            template.getDescription(),
            template.getImageKey(),
            items,
            creator == null ? null : creator.getId(),
            creator == null ? null : creator.getName(),
            template.getCreatedAt(),
            template.getUpdatedAt(),
            template.getCategory()
        );
    }

    private Pageable catalogPageable(int page, int size, CatalogSortOrder sortOrder) {
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La página no puede ser negativa");
        }
        if (size < 1 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tamaño de página debe estar entre 1 y 100");
        }
        Sort.Direction direction = sortOrder == CatalogSortOrder.OLDEST
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, "updatedAt").and(Sort.by(direction, "id")));
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
