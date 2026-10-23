package com.formquid.elementrequest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formquid.block.dto.BlockResponseDto;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.service.BlockService;
import com.formquid.common.Category;
import com.formquid.common.exception.ApiException;
import com.formquid.common.service.CategoryCatalogService;
import com.formquid.elementrequest.dto.CreateElementRequestDto;
import com.formquid.elementrequest.entity.ElementRequest;
import com.formquid.elementrequest.entity.RequestedElementType;
import com.formquid.elementrequest.repository.ElementRequestRepository;
import com.formquid.notification.service.NotificationService;
import com.formquid.question.dto.CreateFixedQuestionRequestDto;
import com.formquid.question.service.QuestionService;
import com.formquid.template.service.TemplateService;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElementRequestServiceTest {

    @Mock
    private ElementRequestRepository elementRequestRepository;
    @Mock
    private QuestionService questionService;
    @Mock
    private BlockService blockService;
    @Mock
    private TemplateService templateService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CategoryCatalogService categoryCatalogService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ElementRequestService elementRequestService;

    @Test
    void create_rejectsPayloadThatDoesNotMatchRequestedType() {
        var request = new CreateElementRequestDto(RequestedElementType.BLOCK, "Propuesta", Map.of());
        when(objectMapper.convertValue(request.requestPayload(), CreateBlockRequestDto.class))
            .thenThrow(new IllegalArgumentException("invalid"));

        assertThatThrownBy(() -> elementRequestService.create(request, User.builder().id(1L).build()))
            .isInstanceOf(ApiException.class)
            .hasMessage("El payload no coincide con el tipo solicitado");
        verify(elementRequestRepository, never()).save(any());
    }

    @Test
    void create_requiresCategoryInSuggestedElement() {
        var payload = Map.<String, Object>of("question", Map.of("statement", "Pregunta"));
        var request = new CreateElementRequestDto(RequestedElementType.FIXED_QUESTION, "Propuesta", payload);
        when(objectMapper.convertValue(payload, CreateFixedQuestionRequestDto.class))
            .thenReturn(new CreateFixedQuestionRequestDto(null, null));

        assertThatThrownBy(() -> elementRequestService.create(request, User.builder().id(1L).build()))
            .isInstanceOf(ApiException.class)
            .hasMessage("Selecciona una categoría para el elemento sugerido");
        verify(elementRequestRepository, never()).save(any());
    }

    @Test
    void create_checksSelectedCategoryExists() {
        var payload = Map.<String, Object>of("category", "SALUD");
        var request = new CreateElementRequestDto(RequestedElementType.FIXED_QUESTION, "Propuesta", payload);
        Category category = new Category("SALUD");
        when(objectMapper.convertValue(payload, CreateFixedQuestionRequestDto.class))
            .thenReturn(new CreateFixedQuestionRequestDto(null, category));
        org.mockito.Mockito.doThrow(new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "La categoría indicada no existe"))
            .when(categoryCatalogService).requireExisting(category);

        assertThatThrownBy(() -> elementRequestService.create(request, User.builder().id(1L).build()))
            .isInstanceOf(ApiException.class)
            .hasMessage("La categoría indicada no existe");
        verify(elementRequestRepository, never()).save(any());
    }

    @Test
    void approve_createsCatalogElementAndRemovesPendingRequest() {
        User requester = User.builder().id(4L).build();
        User admin = User.builder().id(9L).role(UserRole.ADMIN).build();
        var pending = ElementRequest.builder()
            .id(12L)
            .requestedBy(requester)
            .elementType(RequestedElementType.BLOCK)
            .requestPayload(Map.of("name", "Bloque"))
            .build();
        var draft = new CreateBlockRequestDto("Bloque", null, java.util.List.of(), null);
        var created = new BlockResponseDto(
            33L, null, "Bloque", null, java.util.List.of(), 9L, "Admin", null, null, null
        );
        when(elementRequestRepository.findById(12L)).thenReturn(Optional.of(pending));
        when(objectMapper.convertValue(pending.getRequestPayload(), CreateBlockRequestDto.class)).thenReturn(draft);
        when(blockService.createCatalogBlock(draft, admin)).thenReturn(created);

        var response = elementRequestService.approve(12L, admin);

        assertThat(response.createdElementId()).isEqualTo(33L);
        verify(notificationService).createInternal(
            org.mockito.ArgumentMatchers.eq(requester),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.contains("aprobada")
        );
        verify(elementRequestRepository).delete(pending);
    }
}
