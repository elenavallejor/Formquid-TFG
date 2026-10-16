package com.formquid.common;

import com.formquid.common.dto.CategoryResponseDto;
import com.formquid.common.dto.CreateCategoryRequestDto;
import com.formquid.common.service.CategoryCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryCatalogService service;

    @GetMapping
    public List<CategoryResponseDto> findAll(@RequestParam(defaultValue = "false") boolean surveyOnly) {
        return service.findAll(surveyOnly);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPERADMIN')")
    public CategoryResponseDto create(@Valid @RequestBody CreateCategoryRequestDto request) {
        return service.create(request);
    }
}
