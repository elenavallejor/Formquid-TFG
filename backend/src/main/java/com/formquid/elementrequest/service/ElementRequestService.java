package com.formquid.elementrequest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.service.BlockService;
import com.formquid.common.exception.ApiException;
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
 * Gestiona la creación, validación y resolución de solicitudes de elementos del catálogo.
 */
@Service
@RequiredArgsConstructor
public class ElementRequestService {

    private final ElementRequestRepository elementRequestRepository;
    private final QuestionService questionService;
    private final BlockService blockService;
    private final TemplateService templateService;
    private final NotificationService notificationService;
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
        notificationService.deleteElementRequestNotifications(request.getId());
        notificationService.createInternal(
            request.getRequestedBy(),
            NotificationType.ELEMENT_REQUEST_RESOLVED,
            "Solicitud de elemento aprobada",
            "Tu propuesta de catálogo ha sido aprobada"
        );
        elementRequestRepository.delete(request);
        return new ApproveElementRequestResponseDto(createdId);
    }

    @Transactional
    public void reject(Long id) {
        ElementRequest request = getRequest(id);
        notificationService.deleteElementRequestNotifications(request.getId());
        notificationService.createInternal(
            request.getRequestedBy(),
            NotificationType.ELEMENT_REQUEST_RESOLVED,
            "Solicitud de elemento rechazada",
            "Tu propuesta de catálogo ha sido rechazada"
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
        questionService.validateQuestionDraft(dto.question());
    }

    private void validateBlock(CreateElementRequestDto request) {
        CreateBlockRequestDto dto = objectMapper.convertValue(
            request.requestPayload(),
            CreateBlockRequestDto.class
        );
        if (dto.name() == null || dto.name().isBlank() || dto.questions() == null || dto.questions().isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Un bloque solicitado necesita nombre y al menos una pregunta"
            );
        }
        dto.questions().forEach(question -> {
            if (question == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El bloque contiene una pregunta nula");
            }
            questionService.validateQuestionDraft(question.question());
        });
    }

    private void validateTemplate(CreateElementRequestDto request) {
        CreateTemplateRequestDto dto = objectMapper.convertValue(
            request.requestPayload(),
            CreateTemplateRequestDto.class
        );
        if (dto.title() == null || dto.title().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La plantilla solicitada necesita título");
        }
        long questionCount = 0;
        if (dto.localQuestions() != null) {
            dto.localQuestions().forEach(question -> {
                if (question == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "La plantilla contiene una pregunta local nula");
                }
                questionService.validateQuestionDraft(question.question());
            });
            questionCount += dto.localQuestions().size();
        }
        if (dto.fixedQuestions() != null) {
            questionCount += dto.fixedQuestions().size();
        }
        if (dto.blocks() != null) {
            questionCount += dto.blocks().size();
        }
        if (questionCount == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Una plantilla solicitada debe contener preguntas o bloques");
        }
    }

    private ElementRequestResponseDto toResponse(ElementRequest request) {
        return new ElementRequestResponseDto(
            request.getId(),
            request.getRequestedBy().getId(),
            request.getElementType(),
            request.getMessage(),
            request.getRequestPayload(),
            request.getCreatedAt()
        );
    }
}