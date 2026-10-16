package com.formquid.common.service;

import com.formquid.common.Category;
import com.formquid.common.CategoryDefinition;
import com.formquid.common.CategoryDefinitionRepository;
import com.formquid.common.dto.CategoryResponseDto;
import com.formquid.common.dto.CreateCategoryRequestDto;
import com.formquid.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CategoryCatalogService {
    private final CategoryDefinitionRepository repository;

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> findAll(boolean surveyOnly) {
        return repository.findAllByOrderByNameAsc().stream()
            .filter(category -> !surveyOnly || !category.isPersonalData())
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
            return CategoryResponseDto.from(repository.saveAndFlush(new CategoryDefinition(code, name)));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre");
        }
    }
}
