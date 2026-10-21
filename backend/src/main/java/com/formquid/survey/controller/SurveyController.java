package com.formquid.survey.controller;

import com.formquid.common.Category;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.survey.dto.ChangeSurveyLifecycleRequestDto;
import com.formquid.survey.dto.CreateImportedSurveyRequestDto;
import com.formquid.survey.dto.CreateSurveyRequestDto;
import com.formquid.survey.dto.PublicSurveySummaryDto;
import com.formquid.survey.dto.SurveyPageResponseDto;
import com.formquid.survey.dto.SurveyDirectorySummaryDto;
import com.formquid.survey.dto.SurveyImportPreviewDto;
import com.formquid.survey.dto.SurveyResponseDto;
import com.formquid.survey.dto.SurveyResultsSummaryDto;
import com.formquid.survey.dto.SurveyResultsSearchField;
import com.formquid.survey.dto.SurveyResultsSortOrder;
import com.formquid.survey.dto.UpdateSurveyRequestDto;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.service.SurveyService;
import com.formquid.survey.service.SurveyOdtImportService;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Expone las operaciones autenticadas de creación, consulta y gestión de encuestas.
 */
@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;
    private final SurveyOdtImportService surveyOdtImportService;

    @GetMapping
    public SurveyPageResponseDto findMine(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) SurveyLifecycle lifecycleStatus,
        @PageableDefault(
            size = 8,
            sort = "updatedAt",
            direction = Sort.Direction.DESC
        ) Pageable pageable
    ) {
        return surveyService.findEditableByUser(
            user,
            search,
            lifecycleStatus,
            pageable
        );
    }

    @GetMapping("/results")
    public PageResponseDto<SurveyResultsSummaryDto> findResults(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "TITLE,OWNER") Set<SurveyResultsSearchField> searchFields,
        @RequestParam(required = false) SurveyLifecycle lifecycleStatus,
        @RequestParam(defaultValue = "NEWEST") SurveyResultsSortOrder sortOrder,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "9") int size
    ) {
        return surveyService.findResultSurveys(
            user,
            search,
            searchFields,
            lifecycleStatus,
            sortOrder,
            page,
            size
        );
    }

    @GetMapping("/directory")
    public PageResponseDto<SurveyDirectorySummaryDto> findDirectory(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) SurveyLifecycle lifecycleStatus,
        @PageableDefault(
            size = 9,
            sort = "updatedAt",
            direction = Sort.Direction.DESC
        ) Pageable pageable
    ) {
        return surveyService.findDirectory(user, search, lifecycleStatus, pageable);
    }

    @GetMapping("/registered/page")
    public PageResponseDto<PublicSurveySummaryDto> findVisibleForRegisteredPage(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) SurveyAccessMode accessMode,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "9") int size,
        @RequestParam(required = false) Category category
    ) {
        return surveyService.findListedOpenForRegisteredPage(user, search, tag, accessMode, page, size, category);
    }

    @GetMapping("/by-code/{publicCode}")
    public SurveyResponseDto getOpenForRespondent(
        @PathVariable String publicCode,
        @AuthenticationPrincipal User user
    ) {
        return surveyService.getOpenSurveyForRespondent(publicCode, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SurveyResponseDto create(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateSurveyRequestDto request
    ) {
        return surveyService.create(request, user);
    }

    @PostMapping("/from-template/{templateId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SurveyResponseDto createFromTemplate(
        @PathVariable Long templateId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateSurveyRequestDto request
    ) {
        return surveyService.createFromTemplate(templateId, request, user);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EDITOR')")
    public SurveyImportPreviewDto previewImport(@RequestPart("file") MultipartFile file) {
        return surveyOdtImportService.preview(file);
    }

    @PostMapping("/from-import")
    @PreAuthorize("hasRole('EDITOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public SurveyResponseDto createFromImport(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateImportedSurveyRequestDto request
    ) {
        return surveyOdtImportService.createImported(request, user);
    }

    @GetMapping("/{surveyId}")
    public SurveyResponseDto get(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user
    ) {
        return surveyService.getSurveyForUser(surveyId, user);
    }

    @GetMapping("/{surveyId}/results")
    public SurveyResponseDto getForResults(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user
    ) {
        return surveyService.getSurveyForResults(surveyId, user);
    }

    @PutMapping("/{surveyId}")
    public SurveyResponseDto update(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody UpdateSurveyRequestDto request
    ) {
        return surveyService.update(surveyId, request, user);
    }

    @PostMapping(value = "/{surveyId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SurveyResponseDto uploadCover(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user,
        @RequestPart("file") MultipartFile file
    ) {
        return surveyService.uploadCover(surveyId, user, file);
    }

    @PatchMapping("/{surveyId}/lifecycle")
    public SurveyResponseDto changeLifecycle(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody ChangeSurveyLifecycleRequestDto request
    ) {
        return surveyService.changeLifecycle(surveyId, request.lifecycleStatus(), user);
    }

    @DeleteMapping("/{surveyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user
    ) {
        surveyService.delete(surveyId, user);
    }
}
