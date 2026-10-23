package com.formquid.elementrequest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.service.BlockService;
import com.formquid.common.Category;
import com.formquid.common.exception.ApiException;
import com.formquid.common.service.CategoryCatalogService;
import com.formquid.content.entity.ContentItemType;
import com.formquid.elementrequest.dto.ApproveElementRequestResponseDto;
import com.formquid.elementrequest.dto.CreateElementRequestDto;
import com.formquid.elementrequest.dto.ElementRequestResponseDto;
import com.formquid.elementrequest.entity.ElementRequest;
import com.formquid.elementrequest.entity.RequestedElementType;
import com.formquid.elementrequest.repository.ElementRequestRepository;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.service.NotificationService;
import com.formquid.question.dto.CreateFixedQuestionRequestDto;
import com.formquid.question.service.QuestionService;
import com.formquid.template.dto.CreateTemplateRequestDto;
import com.formquid.template.service.TemplateService;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestiona las solicitudes de creación de elementos del catálogo.
 */
@Service
@RequiredArgsConstructor
public class ElementRequestService {

    private final ElementRequestRepository elementRequestRepository;
    private final QuestionService questionService;
    private final BlockService blockService;
    private final TemplateService templateService;
    private final NotificationService notificationService;
    private final CategoryCatalogService categoryCatalogService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ElementRequestResponseDto> findPending() {
        return elementRequestRepository.findAllResponses();
    }

    @Transactional
    public ElementRequestResponseDto create(CreateElementRequestDto request, User requester) {
        validatePayload(request);
        ElementRequest entity = ElementRequest.builder()
            .requestedBy(requester)
            .elementType(request.elementType())
            .message(request.message().trim())
            .requestPayload(request.requestPayload())
            .build();
        ElementRequest saved = elementRequestRepository.save(entity);
        notificationService.notifyAdminsAboutElementRequest(saved);
        return toResponse(saved);
    }

    @Transactional
    public ApproveElementRequestResponseDto approve(Long id, User admin) {
        ElementRequest request = getRequest(id);
        Long createdId = createRequestedElement(request, admin);
        notificationService.createInternal(
            request.getRequestedBy(),
            NotificationType.ELEMENT_REQUEST_RESOLVED,
            "Solicitud de elemento aprobada",
            "Tu propuesta #" + request.getId() + " de " + request.getElementType() + " para el catálogo ha sido aprobada."
        );
        elementRequestRepository.delete(request);
        return new ApproveElementRequestResponseDto(createdId);
    }

    @Transactional
    public void reject(Long id) {
        ElementRequest request = getRequest(id);
        notificationService.createInternal(
            request.getRequestedBy(),
            NotificationType.ELEMENT_REQUEST_RESOLVED,
            "Solicitud de elemento rechazada",
            "Tu propuesta #" + request.getId() + " de " + request.getElementType() + " para el catálogo ha sido rechazada."
        );
        elementRequestRepository.delete(request);
    }

    private ElementRequest getRequest(Long id) {
        return elementRequestRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de elemento no encontrada"));
    }

    private Long createRequestedElement(ElementRequest request, User admin) {
        if (request.getElementType() == RequestedElementType.FIXED_QUESTION) {
            CreateFixedQuestionRequestDto dto = objectMapper.convertValue(
                request.getRequestPayload(),
                CreateFixedQuestionRequestDto.class
            );
            return questionService.createFixedQuestion(dto, admin).id();
        }
        if (request.getElementType() == RequestedElementType.BLOCK) {
            CreateBlockRequestDto dto = objectMapper.convertValue(
                request.getRequestPayload(),
                CreateBlockRequestDto.class
            );
            return blockService.createCatalogBlock(dto, admin).id();
        }
        if (request.getElementType() == RequestedElementType.TEMPLATE) {
            CreateTemplateRequestDto dto = objectMapper.convertValue(
                request.getRequestPayload(),
                CreateTemplateRequestDto.class
            );
            return templateService.create(dto, admin).id();
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Tipo de elemento no soportado");
    }

    private void validatePayload(CreateElementRequestDto request) {
        try {
            if (request.elementType() == RequestedElementType.FIXED_QUESTION) {
                validateFixedQuestion(request);
                return;
            }
            if (request.elementType() == RequestedElementType.BLOCK) {
                validateBlock(request);
                return;
            }
            if (request.elementType() == RequestedElementType.TEMPLATE) {
                validateTemplate(request);
                return;
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tipo de elemento no soportado");
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El payload no coincide con el tipo solicitado");
        }
    }

    private void validateFixedQuestion(CreateElementRequestDto request) {
        CreateFixedQuestionRequestDto dto = objectMapper.convertValue(
            request.requestPayload(),
            CreateFixedQuestionRequestDto.class
        );
        requireCategory(dto.category());
        questionService.validateQuestionDraft(dto.question());
    }

    private void validateBlock(CreateElementRequestDto request) {
        CreateBlockRequestDto dto = objectMapper.convertValue(request.requestPayload(), CreateBlockRequestDto.class);
        validateBlock(dto);
    }

    private void validateBlock(CreateBlockRequestDto dto) {
        requireCategory(dto.category());
        blockService.validateDraft(dto);
        dto.items().stream()
            .filter(item -> item.itemType() == ContentItemType.QUESTION)
            .forEach(item -> questionService.validateQuestionDraft(item.question()));
    }

    private void validateTemplate(CreateElementRequestDto request) {
        CreateTemplateRequestDto dto = objectMapper.convertValue(request.requestPayload(), CreateTemplateRequestDto.class);
        requireCategory(dto.category());
        templateService.validateDraft(dto);
    }

    private void requireCategory(Category category) {
        if (category == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona una categoría para el elemento sugerido");
        }
        categoryCatalogService.requireExisting(category);
    }

    private ElementRequestResponseDto toResponse(ElementRequest request) {
        return new ElementRequestResponseDto(
            request.getId(),
            request.getRequestedBy().getId(),
            request.getRequestedBy().getName(),
            request.getRequestedBy().getEmail(),
            request.getRequestedBy().getInstitution(),
            request.getRequestedBy().getCountry(),
            request.getRequestedBy().getRole(),
            request.getElementType(),
            request.getMessage(),
            request.getRequestPayload(),
            request.getCreatedAt()
        );
    }
}
