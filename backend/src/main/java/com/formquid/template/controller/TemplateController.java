package com.formquid.template.controller;

import com.formquid.common.Category;
import com.formquid.common.dto.CatalogSearchField;
import com.formquid.common.dto.CatalogSortOrder;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.template.dto.CreateTemplateRequestDto;
import com.formquid.template.dto.TemplateResponseDto;
import com.formquid.template.dto.TemplateSummaryDto;
import com.formquid.template.service.TemplateService;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;


/**
 * Expone las operaciones de consulta y creación de plantillas del catálogo.
 */
@RestController
@RequestMapping("/api/v1/catalog/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public PageResponseDto<TemplateSummaryDto> findAll(
        @AuthenticationPrincipal User user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "false") boolean mine,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "TITLE,DESCRIPTION,CREATOR") Set<CatalogSearchField> searchFields,
        @RequestParam(defaultValue = "NEWEST") CatalogSortOrder sortOrder,
        @RequestParam(required = false) Category category
    ) {
        return templateService.findAll(page, size, user, mine, search, searchFields, sortOrder, category);
    }

    @GetMapping("/{templateId}")
    public TemplateResponseDto findById(@PathVariable Long templateId) {
        return templateService.findById(templateId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public TemplateResponseDto create(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateTemplateRequestDto request
    ) {
        return templateService.create(request, user);
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public TemplateResponseDto update(
        @PathVariable Long templateId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateTemplateRequestDto request
    ) {
        return templateService.update(templateId, request, user);
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public void delete(
        @PathVariable Long templateId,
        @AuthenticationPrincipal User user
    ) {
        templateService.delete(templateId, user);
    }
}
