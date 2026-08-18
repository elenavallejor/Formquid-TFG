package com.formquid.elementrequest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.service.BlockService;
import com.formquid.common.entity.RequestStatus;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ElementRequestService {

    private final ElementRequestRepository elementRequestRepository;
    private final QuestionService questionService;
    private final BlockService blockService;
    private final TemplateService templateService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public List<ElementRequestResponseDto> findPending() {
        return elementRequestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING).stream().map(this::toResponse).toList();
    }

    /**
     * Recibe propuesta de elemento y devuelve la solicitud creada.
     */
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
        notifyAdmins(saved);
        return toResponse(saved);
    }

    /**
     * Recibe solicitud pendiente y devuelve el identificador creado.
     */
    @Transactional
    public ApproveElementRequestResponseDto approve(UUID id, User admin) {
        ElementRequest request = getRequest(id);
        String createdId;
        if (request.getElementType() == RequestedElementType.FIXED_QUESTION) {
            CreateFixedQuestionRequestDto dto = objectMapper.convertValue(request.getRequestPayload(), CreateFixedQuestionRequestDto.class);
            createdId = questionService.createFixedQuestion(dto, admin).id();
        } else if (request.getElementType() == RequestedElementType.BLOCK) {
            CreateBlockRequestDto dto = objectMapper.convertValue(request.getRequestPayload(), CreateBlockRequestDto.class);
            createdId = blockService.createCatalogBlock(dto, admin).id();
        } else if (request.getElementType() == RequestedElementType.TEMPLATE) {
            CreateTemplateRequestDto dto = objectMapper.convertValue(request.getRequestPayload(), CreateTemplateRequestDto.class);
            createdId = templateService.create(dto, admin).id().toString();
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tipo de elemento no soportado");
        }
        request.approve();
        notificationService.createElementRequestNotification(
            request.getRequestedBy(),
            NotificationType.ELEMENT_REQUEST_RESOLVED,
            "Solicitud de elemento aprobada",
            "Tu propuesta de catálogo ha sido aprobada",
            request
        );
        return new ApproveElementRequestResponseDto(createdId);
    }

    /**
     * Recibe solicitud pendiente. No devuelve contenido.
     */
    @Transactional
    public void reject(UUID id) {
        ElementRequest request = getRequest(id);
        request.reject();
        notificationService.createElementRequestNotification(
            request.getRequestedBy(),
            NotificationType.ELEMENT_REQUEST_RESOLVED,
            "Solicitud de elemento rechazada",
            "Tu propuesta de catálogo ha sido rechazada",
            request
        );
    }

    private ElementRequest getRequest(UUID id) {
        ElementRequest request = elementRequestRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de elemento no encontrada"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "La solicitud de elemento ya está resuelta");
        }
        return request;
    }

    private void validatePayload(CreateElementRequestDto request) {
        try {
            if (request.elementType() == RequestedElementType.FIXED_QUESTION) {
                CreateFixedQuestionRequestDto dto = objectMapper.convertValue(request.requestPayload(), CreateFixedQuestionRequestDto.class);
                if (dto.catalogName() == null || dto.catalogName().isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "La pregunta fija necesita nombre de catálogo");
                }
                questionService.validateQuestionDraft(dto.question());
                return;
            }
            if (request.elementType() == RequestedElementType.BLOCK) {
                CreateBlockRequestDto dto = objectMapper.convertValue(request.requestPayload(), CreateBlockRequestDto.class);
                if (dto.name() == null || dto.name().isBlank() || dto.questions() == null || dto.questions().isEmpty()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Un bloque solicitado necesita nombre y al menos una pregunta");
                }
                dto.questions().forEach(question -> {
                    if (question == null) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "El bloque contiene una pregunta nula");
                    }
                    questionService.validateQuestionDraft(question.question());
                });
                return;
            }
            if (request.elementType() == RequestedElementType.TEMPLATE) {
                CreateTemplateRequestDto dto = objectMapper.convertValue(request.requestPayload(), CreateTemplateRequestDto.class);
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
                return;
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tipo de elemento no soportado");
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El payload no coincide con el tipo solicitado");
        }
    }

    private void notifyAdmins(ElementRequest request) {
        notificationService.notifyAdminsAboutElementRequest(request);
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
