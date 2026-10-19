package com.formquid.common.service;

import com.formquid.common.Category;
import com.formquid.common.CategoryDefinition;
import com.formquid.common.CategoryDefinitionRepository;
import com.formquid.common.InitialCategory;
import com.formquid.common.dto.CategoryResponseDto;
import com.formquid.common.dto.CreateCategoryRequestDto;
import com.formquid.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryCatalogService {
    private static final Set<String> AVAILABLE_ICONS = Set.of(
        "BsPcDisplay", "BsLaptop", "BsTablet", "BsPhone", "BsSmartwatch", "BsEyeglasses", "BsTv",
        "BsController", "BsBookHalf", "BsEasel", "BsProjector", "BsMortarboard", "BsApp",
        "BsBookshelf", "BsRobot", "BsBoxSeam", "BsCpu", "BsStars", "BsChatDots",
        "BsMic", "BsHeadsetVr", "BsBadgeAr", "BsPeople", "BsChatText", "BsCameraVideo",
        "BsJoystick", "BsPlayCircle", "BsThreeDots", "BsPersonVcard", CategoryDefinition.DEFAULT_ICON
    );
    private static final Set<String> INITIAL_CATEGORY_CODES = Arrays.stream(InitialCategory.values())
        .map(Enum::name)
        .collect(Collectors.toUnmodifiableSet());

    private final CategoryDefinitionRepository repository;

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> findAll(boolean surveyOnly) {
        return repository.findAllByOrderByNameAsc().stream()
            .filter(category -> !surveyOnly || !category.isPersonalData())
            .sorted(Comparator.comparing((CategoryDefinition category) ->
                Category.OTROS.code().equals(category.getCode())))
            .map(CategoryResponseDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public void requireExisting(Category category) {
        if (category == null || !repository.existsById(category.code())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La categoría indicada no existe");
        }
    }

    @Transactional(readOnly = true)
    public void requireSurveyCategory(Category category) {
        requireExisting(category);
        if (Category.DATOS_PERSONALES.equals(category)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Una encuesta no puede ser de datos personales");
        }
    }

    @Transactional
    public CategoryResponseDto create(CreateCategoryRequestDto request) {
        String name = request.name().trim().replaceAll("\\s+", " ");
        String icon = request.icon() == null ? CategoryDefinition.DEFAULT_ICON : requireValidIcon(request.icon());
        String code = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
        if (!code.isEmpty() && Character.isDigit(code.charAt(0))) {
            code = "CATEGORIA_" + code;
        }
        if (!code.matches("[A-Z][A-Z0-9_]{1,63}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El nombre no genera un código de categoría válido");
        }
        if (repository.existsById(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre");
        }
        try {
            return CategoryResponseDto.from(repository.saveAndFlush(new CategoryDefinition(code, name, icon)));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre");
        }
    }

    @Transactional
    public CategoryResponseDto updateIcon(String code, String icon) {
        String validIcon = requireValidIcon(icon);
        CategoryDefinition category = repository.findById(code)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La categoría indicada no existe"));
        if (INITIAL_CATEGORY_CODES.contains(category.getCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede modificar el icono de una categoría inicial");
        }
        category.changeIcon(validIcon);
        return CategoryResponseDto.from(repository.saveAndFlush(category));
    }

    private String requireValidIcon(String icon) {
        if (icon == null || !AVAILABLE_ICONS.contains(icon)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El icono indicado no es válido");
        }
        return icon;
    }
}
