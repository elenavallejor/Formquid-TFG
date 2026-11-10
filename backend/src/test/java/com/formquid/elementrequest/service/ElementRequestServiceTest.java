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
import java.util.List;
import com.formquid.question.dto.*;
import com.formquid.question.entity.*;
import com.formquid.block.dto.CreateBlockItemRequestDto;
import com.formquid.content.entity.ContentItemType;
import com.formquid.template.dto.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElementRequestServiceTest {
    @Mock QuestionResponseDto createdQuestion;
    @Mock TemplateResponseDto createdTemplate;

    @ParameterizedTest
    @EnumSource(RequestedElementType.class)
    void createAndResolve_validatesAndCreatesRequestedCatalogType(RequestedElementType type) {
        User user=User.builder().id(4L).name("Autora").build();
        var payload=Map.<String,Object>of("category","OTROS");
        var draft=new QuestionDraftDto("Pregunta",null,QuestionType.TEXT,EvaluationDirection.NEUTRAL,null,null,List.of(),List.of());
        var fixed=new CreateFixedQuestionRequestDto(draft,Category.OTROS);
        var block=new CreateBlockRequestDto("Bloque",null,List.of(new CreateBlockItemRequestDto(ContentItemType.QUESTION,1,true,draft,null,null)),Category.OTROS);
        var template=new CreateTemplateRequestDto("Plantilla",null,null,List.of(),Category.OTROS);
        switch(type) {
            case FIXED_QUESTION -> {
                when(objectMapper.convertValue(payload,CreateFixedQuestionRequestDto.class)).thenReturn(fixed);
                when(questionService.createFixedQuestion(fixed,user)).thenReturn(createdQuestion);
                when(createdQuestion.id()).thenReturn(33L);
            }
            case BLOCK -> {
                when(objectMapper.convertValue(payload,CreateBlockRequestDto.class)).thenReturn(block);
                when(blockService.createCatalogBlock(block,user)).thenReturn(new BlockResponseDto(33L,null,"Bloque",null,List.of(),4L,null,null,null,Category.OTROS));
            }
            case TEMPLATE -> {
                when(objectMapper.convertValue(payload,CreateTemplateRequestDto.class)).thenReturn(template);
                when(templateService.create(template,user)).thenReturn(createdTemplate);
                when(createdTemplate.id()).thenReturn(33L);
            }
        }
        when(elementRequestRepository.save(any())).thenAnswer(i -> { ElementRequest e=i.getArgument(0); e.setId(12L); return e; });
        var response=elementRequestService.create(new CreateElementRequestDto(type," Propuesta ",payload),user);
        assertThat(response.message()).isEqualTo("Propuesta");
        verify(categoryCatalogService).requireExisting(Category.OTROS);
        var captor=org.mockito.ArgumentCaptor.forClass(ElementRequest.class);
        verify(elementRequestRepository).save(captor.capture());
        ElementRequest pending=captor.getValue();
        when(elementRequestRepository.findById(12L)).thenReturn(Optional.of(pending));
        assertThat(elementRequestService.approve(12L,user).createdElementId()).isEqualTo(33L);
        elementRequestService.reject(12L);
        verify(elementRequestRepository,times(2)).delete(pending);
        verify(notificationService).notifyAdminsAboutElementRequest(pending);
    }

    @Test
    void pendingAndResolution_handleEmptyListAndMissingRequest() {
        assertThat(elementRequestService.findPending()).isEmpty();
        verify(elementRequestRepository).findAllResponses();
        assertThatThrownBy(() -> elementRequestService.reject(99L)).hasMessage("Solicitud de elemento no encontrada");
    }

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
