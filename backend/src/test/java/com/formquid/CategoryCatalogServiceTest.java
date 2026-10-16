package com.formquid;

import com.formquid.common.CategoryDefinition;
import com.formquid.common.CategoryDefinitionRepository;
import com.formquid.common.dto.CreateCategoryRequestDto;
import com.formquid.common.exception.ApiException;
import com.formquid.common.service.CategoryCatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryCatalogServiceTest {
    @Mock private CategoryDefinitionRepository repository;
    @InjectMocks private CategoryCatalogService service;

    @Test
    void createsSpanishCodeAndRejectsDuplicate() {
        when(repository.saveAndFlush(any(CategoryDefinition.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(new CreateCategoryRequestDto(" Tecnología   asistiva "));
        assertThat(created.code()).isEqualTo("TECNOLOGIA_ASISTIVA");
        assertThat(created.name()).isEqualTo("Tecnología asistiva");
        assertThat(created.personalData()).isFalse();

        when(repository.existsById("TECNOLOGIA_ASISTIVA")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new CreateCategoryRequestDto("Tecnología asistiva")))
            .isInstanceOf(ApiException.class)
            .hasMessage("Ya existe una categoría con ese nombre");
    }

    @Test
    void rejectsNameThatCannotBecomeValidCode() {
        assertThatThrownBy(() -> service.create(new CreateCategoryRequestDto("!!!")))
            .isInstanceOf(ApiException.class)
            .hasMessage("El nombre no genera un código de categoría válido");
    }
}
