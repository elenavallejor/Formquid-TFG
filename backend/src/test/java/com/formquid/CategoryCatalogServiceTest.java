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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryCatalogServiceTest {
    @Mock private CategoryDefinitionRepository repository;
    @InjectMocks private CategoryCatalogService service;

    @Test
    void listsOtrosAfterEveryOtherCategory() {
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(
            new CategoryDefinition("OTROS", "Otros"),
            new CategoryDefinition("ORDENADORES", "Ordenadores"),
            new CategoryDefinition("TECNOLOGIA_ASISTIVA", "Tecnología asistiva")
        ));

        assertThat(service.findAll(false))
            .extracting(category -> category.code())
            .containsExactly("ORDENADORES", "TECNOLOGIA_ASISTIVA", "OTROS");
    }

    @Test
    void createsSpanishCodeAndRejectsDuplicate() {
        when(repository.saveAndFlush(any(CategoryDefinition.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(new CreateCategoryRequestDto(" Tecnología   asistiva ", "BsRobot"));
        assertThat(created.code()).isEqualTo("TECNOLOGIA_ASISTIVA");
        assertThat(created.name()).isEqualTo("Tecnología asistiva");
        assertThat(created.personalData()).isFalse();
        assertThat(created.icon()).isEqualTo("BsRobot");

        when(repository.existsById("TECNOLOGIA_ASISTIVA")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new CreateCategoryRequestDto("Tecnología asistiva")))
            .isInstanceOf(ApiException.class)
            .hasMessage("Ya existe una categoría con ese nombre");
    }

    @Test
    void acceptsTheTwoNewReactIcons() {
        when(repository.saveAndFlush(any(CategoryDefinition.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create(new CreateCategoryRequestDto("Internet escolar", "BsGlobe2")).icon())
            .isEqualTo("BsGlobe2");
        assertThat(service.create(new CreateCategoryRequestDto("Ideas", "BsLightbulb")).icon())
            .isEqualTo("BsLightbulb");
    }

    @Test
    void rejectsNameThatCannotBecomeValidCode() {
        assertThatThrownBy(() -> service.create(new CreateCategoryRequestDto("!!!")))
            .isInstanceOf(ApiException.class)
            .hasMessage("El nombre no genera un código de categoría válido");
    }

    @Test
    void updatesExistingCategoryIconAndRejectsInvalidIcon() {
        var category = new CategoryDefinition("TECNOLOGIA_ASISTIVA", "Tecnología asistiva");
        when(repository.findById(category.getCode())).thenReturn(Optional.of(category));
        when(repository.saveAndFlush(category)).thenReturn(category);

        var updated = service.updateIcon(category.getCode(), "BsEyeglasses");
        assertThat(updated.icon()).isEqualTo("BsEyeglasses");

        assertThatThrownBy(() -> service.updateIcon(category.getCode(), "invalid"))
            .isInstanceOf(ApiException.class)
            .hasMessage("El icono indicado no es válido");
    }

    @Test
    void rejectsIconUpdateForMissingCategory() {
        assertThatThrownBy(() -> service.updateIcon("INEXISTENTE", "BsTag"))
            .isInstanceOf(ApiException.class)
            .hasMessage("La categoría indicada no existe");
    }

    @Test
    void rejectsIconUpdateForInitialCategory() {
        var category = new CategoryDefinition("GAFAS_INTELIGENTES", "Gafas inteligentes", "BsEyeglasses");
        when(repository.findById(category.getCode())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.updateIcon(category.getCode(), "BsCpu"))
            .isInstanceOf(ApiException.class)
            .hasMessage("No se puede modificar el icono de una categoría inicial");
        assertThat(category.getIcon()).isEqualTo("BsEyeglasses");
    }
}
