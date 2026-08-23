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

    /**
     * Devuelve todas las solicitudes pendientes.
     */
    @Transactional(readOnly = true)
    public List<ElementRequestResponseDto> findPending() {
        return elementRequestRepository
            .findAllByOrderByCreatedAtAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Crea una nueva solicitud de elemento.
     */
    @Transactional
    public ElementRequestResponseDto create(
        CreateElementRequestDto request,
        User requester
    ) {
        validatePayload(request);

        ElementRequest entity = ElementRequest.builder()
            .requestedBy(requester)
            .elementType(request.elementType())
            .message(request.message().trim())
            .requestPayload(request.requestPayload())
            .build();

        ElementRequest saved =
            elementRequestRepository.save(entity);

        notificationService
            .notifyAdminsAboutElementRequest(saved);

        return toResponse(saved);
    }

    /**
     * Aprueba una solicitud, crea el elemento solicitado
     * y elimina la solicitud.
     */
    @Transactional
    public ApproveElementRequestResponseDto approve(
        UUID id,
        User admin
    ) {
        ElementRequest request = getRequest(id);

        String createdId = createRequestedElement(
            request,
            admin
        );

        /*
         * Elimina las notificaciones de administradores
         * que apuntaban a esta solicitud.
         */
        notificationService
            .deleteElementRequestNotifications(request.getId());

        /*
         * Notificación permanente al solicitante.
         */
        notificationService.createInternal(
            request.getRequestedBy(),
            NotificationType.ELEMENT_REQUEST_RESOLVED,
            "Solicitud de elemento aprobada",
            "Tu propuesta de catálogo ha sido aprobada"
        );

        /*
         * Una solicitud resuelta deja de existir.
         */
        elementRequestRepository.delete(request);

        return new ApproveElementRequestResponseDto(
            createdId
        );
    }

    /**
     * Rechaza una solicitud y la elimina.
     */
    @Transactional
    public void reject(UUID id) {
        ElementRequest request = getRequest(id);

        /*
         * Elimina las notificaciones administrativas
         * asociadas a la solicitud.
         */
        notificationService
            .deleteElementRequestNotifications(request.getId());

        /*
         * Notifica al editor del resultado.
         *
         * Esta notificación no apunta a la solicitud,
         * porque esta será eliminada.
         */
        notificationService.createInternal(
            request.getRequestedBy(),
            NotificationType.ELEMENT_REQUEST_RESOLVED,
            "Solicitud de elemento rechazada",
            "Tu propuesta de catálogo ha sido rechazada"
        );

        elementRequestRepository.delete(request);
    }

    /**
     * Obtiene una solicitud pendiente.
     */
    private ElementRequest getRequest(UUID id) {
        return elementRequestRepository
            .findById(id)
            .orElseThrow(() ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Solicitud de elemento no encontrada"
                )
            );
    }

    /**
     * Crea el elemento de catálogo solicitado.
     */
    private String createRequestedElement(
        ElementRequest request,
        User admin
    ) {
        if (
            request.getElementType()
                == RequestedElementType.FIXED_QUESTION
        ) {
            CreateFixedQuestionRequestDto dto =
                objectMapper.convertValue(
                    request.getRequestPayload(),
                    CreateFixedQuestionRequestDto.class
                );

            return questionService
                .createFixedQuestion(dto, admin)
                .id();
        }

        if (
            request.getElementType()
                == RequestedElementType.BLOCK
        ) {
            CreateBlockRequestDto dto =
                objectMapper.convertValue(
                    request.getRequestPayload(),
                    CreateBlockRequestDto.class
                );

            return blockService
                .createCatalogBlock(dto, admin)
                .id();
        }

        if (
            request.getElementType()
                == RequestedElementType.TEMPLATE
        ) {
            CreateTemplateRequestDto dto =
                objectMapper.convertValue(
                    request.getRequestPayload(),
                    CreateTemplateRequestDto.class
                );

            return templateService
                .create(dto, admin)
                .id()
                .toString();
        }

        throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "Tipo de elemento no soportado"
        );
    }

    /**
     * Valida el contenido propuesto según su tipo.
     */
    private void validatePayload(
        CreateElementRequestDto request
    ) {
        try {
            if (
                request.elementType()
                    == RequestedElementType.FIXED_QUESTION
            ) {
                validateFixedQuestion(request);
                return;
            }

            if (
                request.elementType()
                    == RequestedElementType.BLOCK
            ) {
                validateBlock(request);
                return;
            }

            if (
                request.elementType()
                    == RequestedElementType.TEMPLATE
            ) {
                validateTemplate(request);
                return;
            }

            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Tipo de elemento no soportado"
            );

        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "El payload no coincide con el tipo solicitado"
            );
        }
    }

    /**
     * Valida una propuesta de pregunta fija.
     */
    private void validateFixedQuestion(
        CreateElementRequestDto request
    ) {
        CreateFixedQuestionRequestDto dto =
            objectMapper.convertValue(
                request.requestPayload(),
                CreateFixedQuestionRequestDto.class
            );

        if (
            dto.catalogName() == null
                || dto.catalogName().isBlank()
        ) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "La pregunta fija necesita nombre de catálogo"
            );
        }

        questionService.validateQuestionDraft(
            dto.question()
        );
    }

    /**
     * Valida una propuesta de bloque.
     */
    private void validateBlock(
        CreateElementRequestDto request
    ) {
        CreateBlockRequestDto dto =
            objectMapper.convertValue(
                request.requestPayload(),
                CreateBlockRequestDto.class
            );

        if (
            dto.name() == null
                || dto.name().isBlank()
                || dto.questions() == null
                || dto.questions().isEmpty()
        ) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Un bloque solicitado necesita nombre "
                    + "y al menos una pregunta"
            );
        }

        dto.questions().forEach(question -> {
            if (question == null) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El bloque contiene una pregunta nula"
                );
            }

            questionService.validateQuestionDraft(
                question.question()
            );
        });
    }

    /**
     * Valida una propuesta de plantilla.
     */
    private void validateTemplate(
        CreateElementRequestDto request
    ) {
        CreateTemplateRequestDto dto =
            objectMapper.convertValue(
                request.requestPayload(),
                CreateTemplateRequestDto.class
            );

        if (
            dto.title() == null
                || dto.title().isBlank()
        ) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "La plantilla solicitada necesita título"
            );
        }

        long questionCount = 0;

        if (dto.localQuestions() != null) {
            dto.localQuestions().forEach(question -> {
                if (question == null) {
                    throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "La plantilla contiene "
                            + "una pregunta local nula"
                    );
                }

                questionService.validateQuestionDraft(
                    question.question()
                );
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
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Una plantilla solicitada debe contener "
                    + "preguntas o bloques"
            );
        }
    }

    /**
     * Convierte una solicitud a su DTO.
     */
    private ElementRequestResponseDto toResponse(
        ElementRequest request
    ) {
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