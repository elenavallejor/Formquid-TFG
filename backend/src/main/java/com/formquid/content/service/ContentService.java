package com.formquid.content.service;

import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockScope;
import com.formquid.block.dto.BlockResponseDto;
import com.formquid.block.dto.CreateBlockItemRequestDto;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.repository.BlockRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.common.service.CategoryCatalogService;
import com.formquid.content.dto.AddBlockItemRequestDto;
import com.formquid.content.dto.AddFixedQuestionItemRequestDto;
import com.formquid.content.dto.BlockContentItemRowDto;
import com.formquid.content.dto.ContentItemRowDto;
import com.formquid.content.dto.ContentItemResponseDto;
import com.formquid.content.dto.CreateQuestionItemRequestDto;
import com.formquid.content.dto.CreateTextItemRequestDto;
import com.formquid.content.dto.ReorderContentRequestDto;
import com.formquid.content.dto.UpdateQuestionItemRequestDto;
import com.formquid.content.dto.UpdateRequiredRequestDto;
import com.formquid.content.dto.SurveyContentItemRequestDto;
import com.formquid.content.dto.UpdateSurveyContentRequestDto;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentItemRepository contentItemRepository;
    private final QuestionRepository questionRepository;
    private final BlockRepository blockRepository;
    private final QuestionService questionService;
    private final CategoryCatalogService categoryCatalogService;

    @Transactional(readOnly = true)
    public List<ContentItemResponseDto> findSurveyContent(Long surveyId) {
        return findStructuredContent(contentItemRepository.findSurveyContentRows(surveyId));
    }

    @Transactional(readOnly = true)
    public List<ContentItemResponseDto> findTemplateContent(Template template) {
        return findStructuredContent(contentItemRepository.findTemplateContentRows(template.getId()));
    }

    private List<ContentItemResponseDto> findStructuredContent(List<ContentItemRowDto> items) {
        List<Long> blockItemIds = items.stream()
            .filter(item -> item.itemType() == ContentItemType.BLOCK)
            .map(ContentItemRowDto::id)
            .toList();
        Map<Long, Block> blocksByItem = new LinkedHashMap<>();
        if (!blockItemIds.isEmpty()) {
            blockRepository.findByParentItemIdsWithDetails(blockItemIds)
                .forEach(block -> blocksByItem.put(block.getParentItem().getId(), block));
        }

        List<BlockContentItemRowDto> blockItems = blocksByItem.isEmpty()
            ? List.of()
            : contentItemRepository.findBlockContentRows(
                blocksByItem.values().stream().map(Block::getId).toList()
            );
        List<Long> questionItemIds = new ArrayList<>();
        items.stream()
            .filter(item -> item.itemType() == ContentItemType.QUESTION)
            .map(ContentItemRowDto::id)
            .forEach(questionItemIds::add);
        blockItems.stream()
            .filter(item -> item.itemType() == ContentItemType.QUESTION)
            .map(BlockContentItemRowDto::id)
            .forEach(questionItemIds::add);

        Map<Long, Question> questionsByItem = findQuestionsByItem(questionItemIds);
        Map<Long, List<ContentItemResponseDto>> contentByBlock = new LinkedHashMap<>();
        blocksByItem.values().forEach(block -> contentByBlock.put(block.getId(), new ArrayList<>()));
        blockItems.forEach(item -> contentByBlock.get(item.parentBlockId())
            .add(toResponse(item, questionsByItem.get(item.id()))));

        return items.stream()
            .map(item -> toResponse(
                item,
                questionsByItem.get(item.id()),
                blocksByItem.get(item.id()),
                contentByBlock
            ))
            .toList();
    }

    /**
     * Construye la respuesta completa de un único elemento recién añadido a una encuesta.
     * La lectura completa de encuestas utiliza findStructuredContent para evitar consultas N+1.
     */
    private ContentItemResponseDto toSurveyResponse(ContentItem item) {
        Question question = questionRepository.findByParentItemId(item.getId()).orElse(null);
        Block block = blockRepository.findByParentItemId(item.getId()).orElse(null);
        BlockResponseDto blockResponse = block == null ? null : new BlockResponseDto(
            block.getId(),
            block.getScope(),
            block.getName(),
            block.getDescription(),
            findBlockContent(block),
            block.getCreatedBy() == null ? null : block.getCreatedBy().getId(),
            block.getCreatedBy() == null ? null : block.getCreatedBy().getName(),
            block.getCreatedAt(),
            block.getUpdatedAt(),
            block.getCategory()
        );
        return new ContentItemResponseDto(
            item.getId(),
            item.getItemType(),
            item.getPosition(),
            item.getRequired(),
            item.getTextBody(),
            question == null ? null : questionService.toResponse(question),
            block == null ? null : block.getId(),
            blockResponse
        );
    }

    public List<ContentItemResponseDto> findBlockContent(Block block) {
        return contentItemRepository.findByParentBlockIdOrderByPositionAsc(block.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<ContentItemResponseDto>> findBlocksContent(List<Block> blocks) {
        Map<Long, List<ContentItemResponseDto>> contentByBlock = new LinkedHashMap<>();
        blocks.forEach(block -> contentByBlock.put(block.getId(), new ArrayList<>()));
        if (blocks.isEmpty()) return contentByBlock;

        List<BlockContentItemRowDto> items = contentItemRepository.findBlockContentRows(
            blocks.stream().map(Block::getId).toList()
        );
        List<Long> questionItemIds = items.stream()
            .filter(item -> item.itemType() == ContentItemType.QUESTION)
            .map(BlockContentItemRowDto::id)
            .toList();

        Map<Long, Question> questionsByItem = findQuestionsByItem(questionItemIds);

        for (BlockContentItemRowDto item : items) {
            contentByBlock.get(item.parentBlockId()).add(toResponse(item, questionsByItem.get(item.id())));
        }
        return contentByBlock;
    }

    private Map<Long, Question> findQuestionsByItem(List<Long> itemIds) {
        Map<Long, Question> questionsByItem = new LinkedHashMap<>();
        if (!itemIds.isEmpty()) {
            questionRepository.findByParentItemIdsWithDetails(itemIds)
                .forEach(question -> questionsByItem.put(question.getParentItem().getId(), question));
        }
        return questionsByItem;
    }

    private ContentItemResponseDto toResponse(BlockContentItemRowDto item, Question question) {
        return new ContentItemResponseDto(
            item.id(),
            item.itemType(),
            item.position(),
            item.required(),
            item.textBody(),
            question == null ? null : questionService.toResponse(question),
            null,
            null
        );
    }

    /**
     * Recibe texto y encuesta, devuelve el elemento creado.
     */
    @Transactional
    public ContentItemResponseDto addTextToSurvey(Survey survey, CreateTextItemRequestDto dto) {
        makeRoomForSurveyItem(survey, dto.position());
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
        makeRoomForSurveyItem(survey, dto.position());
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
        makeRoomForSurveyItem(survey, dto.position());
        ContentItem item = baseSurveyItem(survey, ContentItemType.QUESTION, dto.position());
        item.setRequired(dto.required());
        ContentItem savedItem = contentItemRepository.save(item);
        Question fixed = getCatalogFixedQuestion(dto.fixedQuestionId());
        questionService.createFixedSnapshot(fixed, savedItem, creator);
        return toResponse(savedItem);
    }

    @Transactional
    public ContentItemResponseDto addBlockToSurvey(Survey survey, AddBlockItemRequestDto dto, User creator) {
        makeRoomForSurveyItem(survey, dto.position());
        ContentItem item = baseSurveyItem(survey, ContentItemType.BLOCK, dto.position());
        ContentItem savedItem = contentItemRepository.save(item);
        Block catalogBlock = getCatalogBlock(dto.blockId());
        copyExistingBlock(catalogBlock, savedItem, creator);
        return toResponse(savedItem);
    }

    @Transactional
    public ContentItemResponseDto addLocalBlockToSurvey(
        Survey survey,
        Integer position,
        CreateBlockRequestDto dto,
        User creator
    ) {
        if (dto.category() != null) categoryCatalogService.requireExisting(dto.category());
        makeRoomForSurveyItem(survey, position);
        return toSurveyResponse(saveLocalSurveyBlock(survey, position, dto, creator));
    }

    private ContentItem saveLocalSurveyBlock(
        Survey survey,
        Integer position,
        CreateBlockRequestDto dto,
        User creator
    ) {
        ContentItem parentItem = contentItemRepository.save(
            baseSurveyItem(survey, ContentItemType.BLOCK, position)
        );
        Block block = blockRepository.save(Block.builder()
            .parentItem(parentItem)
            .scope(BlockScope.SNAPSHOT)
            .name(dto.name().trim())
            .category(dto.category() == null ? survey.getCategory() : dto.category())
            .description(dto.description() == null || dto.description().isBlank() ? null : dto.description().trim())
            .createdBy(creator)
            .build());

        dto.items().stream()
            .sorted(Comparator.comparing(CreateBlockItemRequestDto::position))
            .forEach(item -> {
                if (item.itemType() == ContentItemType.TEXT) {
                    contentItemRepository.save(ContentItem.builder()
                        .parentBlock(block)
                        .itemType(ContentItemType.TEXT)
                        .position(item.position())
                        .textBody(item.textBody().trim())
                        .build());
                    return;
                }

                ContentItem questionItem = contentItemRepository.save(ContentItem.builder()
                    .parentBlock(block)
                    .itemType(ContentItemType.QUESTION)
                    .position(item.position())
                    .required(item.required())
                    .build());
                if (item.fixedQuestionId() != null) {
                    questionService.createFixedSnapshot(
                        getCatalogFixedQuestion(item.fixedQuestionId()),
                        questionItem,
                        creator
                    );
                } else {
                    questionService.createLocalQuestion(item.question(), questionItem, creator);
                }
            });
        return parentItem;
    }

    @Transactional
    public List<ContentItemResponseDto> replaceSurveyContent(
        Survey survey,
        UpdateSurveyContentRequestDto request,
        User creator
    ) {
        validateSurveyContent(request);
        deleteSurveyContent(survey);

        request.items().stream()
            .sorted(Comparator.comparing(SurveyContentItemRequestDto::position))
            .forEach(item -> addSurveyItem(survey, item, creator));

        return findSurveyContent(survey.getId());
    }

    private void addSurveyItem(Survey survey, SurveyContentItemRequestDto item, User creator) {
        if (item.itemType() == ContentItemType.TEXT) {
            ContentItem textItem = baseSurveyItem(survey, ContentItemType.TEXT, item.position());
            textItem.setTextBody(item.textBody().trim());
            contentItemRepository.save(textItem);
            return;
        }
        if (item.itemType() == ContentItemType.QUESTION) {
            ContentItem questionItem = baseSurveyItem(survey, ContentItemType.QUESTION, item.position());
            questionItem.setRequired(item.required());
            ContentItem savedItem = contentItemRepository.save(questionItem);
            if (item.fixedQuestionId() != null) {
                questionService.createFixedSnapshot(getCatalogFixedQuestion(item.fixedQuestionId()), savedItem, creator);
            } else {
                questionService.createLocalQuestion(item.question(), savedItem, creator);
            }
            return;
        }
        if (item.blockId() != null) {
            ContentItem blockItem = contentItemRepository.save(
                baseSurveyItem(survey, ContentItemType.BLOCK, item.position())
            );
            copyExistingBlock(getCatalogBlock(item.blockId()), blockItem, creator);
        } else {
            if (item.block().category() != null) categoryCatalogService.requireExisting(item.block().category());
            saveLocalSurveyBlock(survey, item.position(), item.block(), creator);
        }
    }

    private void validateSurveyContent(UpdateSurveyContentRequestDto request) {
        if (request == null || request.items() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La estructura de la encuesta es obligatoria");
        }
        Set<Integer> positions = new HashSet<>();
        for (SurveyContentItemRequestDto item : request.items()) {
            if (item == null || item.itemType() == null || item.position() == null || item.position() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La encuesta contiene un elemento inválido");
            }
            if (!positions.add(item.position())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La encuesta contiene posiciones duplicadas");
            }
            if (item.itemType() == ContentItemType.TEXT
                && (item.textBody() == null || item.textBody().isBlank())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Los elementos de texto no pueden estar vacíos");
            }
            if (item.itemType() == ContentItemType.QUESTION
                && (item.required() == null || (item.question() == null) == (item.fixedQuestionId() == null))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Cada pregunta debe tener un único origen");
            }
            if (item.itemType() == ContentItemType.BLOCK
                && ((item.block() == null) == (item.blockId() == null))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Cada bloque debe tener un único origen");
            }
        }
        for (int position = 1; position <= request.items().size(); position++) {
            if (!positions.contains(position)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las posiciones de la encuesta deben ser consecutivas");
            }
        }
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
        deleteParentContentItems(List.of(item));
        normalizeSurveyPositions(survey.getId());
    }

    @Transactional
    public List<ContentItemResponseDto> reorderSurveyContent(Survey survey, ReorderContentRequestDto dto) {
        List<ContentItem> currentItems = contentItemRepository.findBySurveyIdOrderByPositionAsc(survey.getId());
        if (dto == null || dto.items() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El reordenado debe incluir sus elementos");
        }
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
        for (int position = 1; position <= currentItems.size(); position++) {
            if (!requestedPositions.contains(position)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Las posiciones deben ser consecutivas desde 1");
            }
        }

        Map<Long, Integer> positionsById = dto.items().stream()
            .collect(java.util.stream.Collectors.toMap(
                item -> item.itemId(),
                item -> item.position()
            ));
        rewritePositions(currentItems, positionsById);
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
    public ContentItemResponseDto addLocalBlockToTemplate(
        Template template,
        Integer position,
        CreateBlockRequestDto dto,
        User creator
    ) {
        if (dto.category() != null) categoryCatalogService.requireExisting(dto.category());
        ContentItem parentItem = contentItemRepository.save(
            baseTemplateItem(template, ContentItemType.BLOCK, position)
        );
        Block block = blockRepository.save(Block.builder()
            .parentItem(parentItem)
            .scope(BlockScope.SNAPSHOT)
            .name(dto.name().trim())
            .category(dto.category() == null ? template.getCategory() : dto.category())
            .description(dto.description() == null || dto.description().isBlank() ? null : dto.description().trim())
            .createdBy(creator)
            .build());

        dto.items().stream()
            .sorted(Comparator.comparing(CreateBlockItemRequestDto::position))
            .forEach(item -> {
                if (item.itemType() == ContentItemType.TEXT) {
                    contentItemRepository.save(ContentItem.builder()
                        .parentBlock(block)
                        .itemType(ContentItemType.TEXT)
                        .position(item.position())
                        .textBody(item.textBody().trim())
                        .build());
                    return;
                }

                ContentItem questionItem = contentItemRepository.save(ContentItem.builder()
                    .parentBlock(block)
                    .itemType(ContentItemType.QUESTION)
                    .position(item.position())
                    .required(item.required())
                    .build());
                if (item.fixedQuestionId() != null) {
                    questionService.createFixedSnapshot(
                        getCatalogFixedQuestion(item.fixedQuestionId()),
                        questionItem,
                        creator
                    );
                } else {
                    questionService.createLocalQuestion(item.question(), questionItem, creator);
                }
            });

        return toResponse(parentItem);
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

    @Transactional
    public ContentItemResponseDto addFixedQuestionToBlock(
        Block block,
        AddFixedQuestionItemRequestDto dto,
        User creator
    ) {
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
        questionService.createFixedSnapshot(getCatalogFixedQuestion(dto.fixedQuestionId()), savedItem, creator);
        return toResponse(savedItem);
    }

    @Transactional
    public ContentItemResponseDto addTextToBlock(Block block, CreateTextItemRequestDto dto) {
        if (block.getScope() == BlockScope.SNAPSHOT) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede modificar el contenido interno de un bloque copiado");
        }
        ContentItem item = ContentItem.builder()
            .parentBlock(block)
            .itemType(ContentItemType.TEXT)
            .position(dto.position())
            .textBody(dto.textBody().trim())
            .build();
        return toResponse(contentItemRepository.save(item));
    }

    public Block copyExistingBlock(Block source, ContentItem parentItem, User creator) {
        Block copy = Block.builder()
            .parentItem(parentItem)
            .scope(BlockScope.SNAPSHOT)
            .name(source.getName())
            .category(source.getCategory())
            .description(source.getDescription())
            .createdBy(creator)
            .build();
        Block savedBlock = blockRepository.save(copy);

        List<ContentItem> sourceItems = contentItemRepository.findByParentBlockIdOrderByPositionAsc(source.getId());
        for (ContentItem sourceItem : sourceItems) {
            if (sourceItem.getItemType() == ContentItemType.TEXT) {
                contentItemRepository.save(ContentItem.builder()
                    .parentBlock(savedBlock)
                    .itemType(ContentItemType.TEXT)
                    .position(sourceItem.getPosition())
                    .textBody(sourceItem.getTextBody())
                    .build());
                continue;
            }
            if (sourceItem.getItemType() != ContentItemType.QUESTION) {
                throw new ApiException(HttpStatus.CONFLICT, "El bloque contiene un tipo de elemento no permitido");
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
            if (item.getItemType() == ContentItemType.QUESTION) {
                deleteQuestionForItem(item);
            }
        }
        questionRepository.flush();
        contentItemRepository.deleteAll(items);
        contentItemRepository.flush();
    }

    @Transactional
    public void deleteTemplateContent(Template template) {
        deleteParentContentItems(contentItemRepository.findByTemplateIdOrderByPositionAsc(template.getId()));
    }

    @Transactional
    public void deleteSurveyContent(Survey survey) {
        deleteParentContentItems(contentItemRepository.findBySurveyIdOrderByPositionAsc(survey.getId()));
    }

    private void deleteParentContentItems(List<ContentItem> items) {
        for (ContentItem item : items) {
            if (item.getItemType() == ContentItemType.QUESTION) {
                deleteQuestionForItem(item);
            } else if (item.getItemType() == ContentItemType.BLOCK) {
                blockRepository.findByParentItemId(item.getId()).ifPresent(block -> {
                    deleteBlockContent(block);
                    item.setBlock(null);
                    blockRepository.delete(block);
                    blockRepository.flush();
                });
            }
        }
        questionRepository.flush();
        contentItemRepository.deleteAll(items);
        contentItemRepository.flush();
    }

    private void deleteQuestionForItem(ContentItem item) {
        questionRepository.findByParentItemId(item.getId()).ifPresent(question -> {
            item.setQuestion(null);
            questionRepository.delete(question);
        });
    }

    private ContentItem baseSurveyItem(Survey survey, ContentItemType type, Integer position) {
        return ContentItem.builder()
            .survey(survey)
            .itemType(type)
            .position(position)
            .build();
    }

    private void makeRoomForSurveyItem(Survey survey, Integer requestedPosition) {
        List<ContentItem> items = contentItemRepository.findBySurveyIdOrderByPositionAsc(survey.getId());
        if (requestedPosition == null || requestedPosition < 1 || requestedPosition > items.size() + 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La posición debe estar entre 1 y " + (items.size() + 1));
        }

        Map<Long, Integer> positionsById = new LinkedHashMap<>();
        for (int index = 0; index < items.size(); index++) {
            int normalizedPosition = index + 1;
            positionsById.put(
                items.get(index).getId(),
                normalizedPosition >= requestedPosition ? normalizedPosition + 1 : normalizedPosition
            );
        }
        rewritePositions(items, positionsById);
    }

    private void normalizeSurveyPositions(Long surveyId) {
        List<ContentItem> items = contentItemRepository.findBySurveyIdOrderByPositionAsc(surveyId);
        Map<Long, Integer> positionsById = new LinkedHashMap<>();
        for (int index = 0; index < items.size(); index++) {
            positionsById.put(items.get(index).getId(), index + 1);
        }
        rewritePositions(items, positionsById);
    }

    private void rewritePositions(List<ContentItem> items, Map<Long, Integer> positionsById) {
        if (items.isEmpty()) {
            return;
        }
        int temporaryPosition = items.stream()
            .map(ContentItem::getPosition)
            .filter(java.util.Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0) + items.size() + 1000;
        for (ContentItem item : items) {
            item.setPosition(temporaryPosition++);
        }
        contentItemRepository.flush();
        for (ContentItem item : items) {
            item.setPosition(positionsById.get(item.getId()));
        }
        contentItemRepository.flush();
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
            block == null ? null : block.getId(),
            null
        );
    }

    private ContentItemResponseDto toResponse(
        ContentItemRowDto item,
        Question question,
        Block block,
        Map<Long, List<ContentItemResponseDto>> contentByBlock
    ) {
        BlockResponseDto blockResponse = block == null || contentByBlock == null ? null : new BlockResponseDto(
            block.getId(),
            block.getScope(),
            block.getName(),
            block.getDescription(),
            contentByBlock.getOrDefault(block.getId(), List.of()),
            block.getCreatedBy() == null ? null : block.getCreatedBy().getId(),
            block.getCreatedBy() == null ? null : block.getCreatedBy().getName(),
            block.getCreatedAt(),
            block.getUpdatedAt(),
            block.getCategory()
        );
        return new ContentItemResponseDto(
            item.id(),
            item.itemType(),
            item.position(),
            item.required(),
            item.textBody(),
            question == null ? null : questionService.toResponse(question),
            block == null ? null : block.getId(),
            blockResponse
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
