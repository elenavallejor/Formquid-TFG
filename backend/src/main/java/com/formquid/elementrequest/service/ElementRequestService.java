package com.formquid.elementrequest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.service.BlockService;
import com.formquid.elementrequest.dto.ApproveElementRequestResponseDto;
import com.formquid.elementrequest.dto.CreateElementRequestDto;
import com.formquid.elementrequest.dto.ElementRequestResponseDto;
import com.formquid.elementrequest.dto.ResolveElementRequestDto;
import com.formquid.elementrequest.entity.ElementRequest;
import com.formquid.elementrequest.entity.ElementRequestStatus;
import com.formquid.elementrequest.repository.ElementRequestRepository;
import com.formquid.notification.entity.NotificationTargetType;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.service.NotificationService;
import com.formquid.question.dto.CreateQuestionRequestDto;
import com.formquid.question.service.QuestionService;
import com.formquid.template.dto.CreateSurveyTemplateRequestDto;
import com.formquid.template.service.SurveyTemplateService;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Servicio de negocio para gestionar solicitudes de elementos del catálogo.
 *
 * La solicitud se conserva tras aprobarse o rechazarse; el estado y los campos
 * de resolución aportan trazabilidad sin tablas adicionales.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ElementRequestService {

    private final ElementRequestRepository elementRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BlockService blockService;
    private final QuestionService questionService;
    private final SurveyTemplateService surveyTemplateService;
    private final ObjectMapper objectMapper;

    /**
     * Crea una nueva solicitud pendiente.
     */
    @Transactional
    public ElementRequestResponseDto create(
        CreateElementRequestDto request,
        String requesterEmail
    ) {
        User requester = getAuthenticatedUser(requesterEmail);
        JsonNode payload = buildPayload(request);

        ElementRequest elementRequest = ElementRequest.builder()
            .type(request.type())
            .title(request.title().trim())
            .description(request.description().trim())
            .justification(cleanOptional(request.justification()))
            .requestPayload(payload)
            .requestedBy(requester)
            .status(ElementRequestStatus.PENDING)
            .build();

        return toResponse(elementRequestRepository.save(elementRequest));
    }

    /**
     * Devuelve todas las solicitudes, incluidas las resueltas.
     */
    public List<ElementRequestResponseDto> getAll() {
        return elementRequestRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public ElementRequestResponseDto getById(Long requestId) {
        return toResponse(getRequest(requestId));
    }

    /**
     * Devuelve solicitudes pendientes.
     */
    public List<ElementRequestResponseDto> getPending() {
        return elementRequestRepository
            .findByStatusOrderByCreatedAtDesc(ElementRequestStatus.PENDING)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ElementRequestResponseDto> getMyRequests(String userEmail) {
        User user = getAuthenticatedUser(userEmail);

        return elementRequestRepository
            .findByRequestedByIdOrderByCreatedAtDesc(user.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public long countPending() {
        return elementRequestRepository.countByStatus(ElementRequestStatus.PENDING);
    }

    /**
     * Aprueba una solicitud y crea el elemento definitivo del catálogo.
     */
    @Transactional
    public ApproveElementRequestResponseDto approve(
        Long requestId,
        ResolveElementRequestDto resolution,
        String adminEmail
    ) {
        User admin = getAdministrator(adminEmail);
        ElementRequest request = getPendingRequest(requestId);
        Long createdElementId = createRequestedElement(request, adminEmail);

        request.approve(admin, getResolutionMessage(resolution));
        notifyRequester(request, true);

        return new ApproveElementRequestResponseDto(
            request.getType(),
            createdElementId
        );
    }

    /**
     * Rechaza una solicitud pendiente conservando el historial.
     */
    @Transactional
    public void reject(
        Long requestId,
        ResolveElementRequestDto resolution,
        String adminEmail
    ) {
        User admin = getAdministrator(adminEmail);
        ElementRequest request = getPendingRequest(requestId);

        request.reject(admin, getResolutionMessage(resolution));
        notifyRequester(request, false);
    }

    private JsonNode buildPayload(CreateElementRequestDto request) {
        validatePayload(request);

        return switch (request.type()) {
            case BLOCK -> objectMapper.valueToTree(request.block());
            case QUESTION -> objectMapper.valueToTree(request.question());
            case TEMPLATE -> objectMapper.valueToTree(request.template());
        };
    }

    private void validatePayload(CreateElementRequestDto request) {
        if (request.type() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debe indicarse el tipo de elemento solicitado"
            );
        }

        int informedPayloads = 0;

        if (request.block() != null) informedPayloads++;
        if (request.question() != null) informedPayloads++;
        if (request.template() != null) informedPayloads++;

        if (informedPayloads != 1) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debe informarse exactamente la definición del elemento solicitado"
            );
        }

        boolean matchesType = switch (request.type()) {
            case BLOCK -> request.block() != null;
            case QUESTION -> request.question() != null;
            case TEMPLATE -> request.template() != null;
        };

        if (!matchesType) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La definición enviada no corresponde con el tipo de solicitud"
            );
        }
    }

    private Long createRequestedElement(
        ElementRequest request,
        String adminEmail
    ) {
        try {
            return switch (request.getType()) {
                case BLOCK -> {
                    CreateBlockRequestDto dto = objectMapper.treeToValue(
                        request.getRequestPayload(),
                        CreateBlockRequestDto.class
                    );

                    yield blockService.create(dto, adminEmail).id();
                }

                case QUESTION -> {
                    CreateQuestionRequestDto dto = objectMapper.treeToValue(
                        request.getRequestPayload(),
                        CreateQuestionRequestDto.class
                    );

                    yield questionService.create(dto, adminEmail).id();
                }

                case TEMPLATE -> {
                    CreateSurveyTemplateRequestDto dto = objectMapper.treeToValue(
                        request.getRequestPayload(),
                        CreateSurveyTemplateRequestDto.class
                    );

                    yield surveyTemplateService.create(dto, adminEmail).id();
                }
            };
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La definición almacenada para la solicitud no es válida",
                ex
            );
        }
    }

    private ElementRequest getRequest(Long requestId) {
        return elementRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la solicitud indicada"
            ));
    }

    private ElementRequest getPendingRequest(Long requestId) {
        ElementRequest request = getRequest(requestId);

        if (request.getStatus() != ElementRequestStatus.PENDING) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud indicada ya ha sido resuelta"
            );
        }

        return request;
    }

    private User getAuthenticatedUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return userRepository.findByEmailIgnoreCase(email.trim())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario autenticado no encontrado"
            ));
    }

    private User getAdministrator(String email) {
        User user = getAuthenticatedUser(email);

        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPERADMIN) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo un administrador puede resolver solicitudes de elementos"
            );
        }

        return user;
    }

    private void notifyRequester(
        ElementRequest request,
        boolean approved
    ) {
        String title = approved
            ? "Solicitud de nuevo elemento aprobada"
            : "Solicitud de nuevo elemento rechazada";

        String message = approved
            ? "Tu solicitud " + request.getTitle() + " ha sido aprobada y el elemento ya está disponible en el catálogo."
            : "Tu solicitud " + request.getTitle() + " ha sido rechazada.";

        if (StringUtils.hasText(request.getResolutionMessage())) {
            message += " Comentario: " + request.getResolutionMessage();
        }

        notificationService.notifyInternal(
            request.getRequestedBy().getId(),
            NotificationType.ELEMENT_REQUEST_RESOLVED,
            title,
            message,
            NotificationTargetType.ELEMENT_REQUEST,
            request.getId()
        );
    }

    private String getResolutionMessage(ResolveElementRequestDto resolution) {
        return resolution == null ? null : resolution.resolutionMessage();
    }

    private ElementRequestResponseDto toResponse(ElementRequest request) {
        User requestedBy = request.getRequestedBy();
        User resolvedBy = request.getResolvedBy();

        return new ElementRequestResponseDto(
            request.getId(),
            request.getType(),
            request.getStatus(),
            request.getTitle(),
            request.getDescription(),
            request.getJustification(),
            request.getRequestPayload(),
            requestedBy.getId(),
            requestedBy.getName(),
            resolvedBy != null ? resolvedBy.getId() : null,
            resolvedBy != null ? resolvedBy.getName() : null,
            request.getResolutionMessage(),
            request.getCreatedAt(),
            request.getResolvedAt()
        );
    }

    private String cleanOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
