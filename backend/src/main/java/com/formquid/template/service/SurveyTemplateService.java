package com.formquid.template.service;

import com.formquid.question.dto.MatrixRowResponseDto;
import com.formquid.question.dto.OptionConfigResponseDto;
import com.formquid.question.entity.MatrixRowConfig;
import com.formquid.question.entity.OptionConfig;
import com.formquid.survey.model.SurveyBlockSnapshot;
import com.formquid.survey.model.SurveyQuestionSnapshot;
import com.formquid.survey.model.SurveyStructure;
import com.formquid.survey.service.SurveyStructureFactory;
import com.formquid.template.dto.CreateSurveyTemplateRequestDto;
import com.formquid.template.dto.SurveyTemplateResponseDto;
import com.formquid.template.dto.TemplateBlockResponseDto;
import com.formquid.template.dto.TemplateQuestionResponseDto;
import com.formquid.template.dto.UpdateSurveyTemplateRequestDto;
import com.formquid.template.entity.SurveyTemplate;
import com.formquid.template.entity.TemplateStatus;
import com.formquid.template.repository.SurveyTemplateRepository;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Servicio de negocio para la gestión de plantillas reutilizables.
 *
 * Una plantilla guarda su estructura como snapshot JSONB. Al crear una
 * encuesta desde plantilla se copia ese snapshot completo, evitando tablas
 * intermedias y dependencias contra preguntas vivas del catálogo.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyTemplateService {

    private final SurveyTemplateRepository surveyTemplateRepository;
    private final SurveyStructureFactory surveyStructureFactory;
    private final UserRepository userRepository;

    /**
     * Crea una plantilla activa.
     */
    @Transactional
    public SurveyTemplateResponseDto create(
        CreateSurveyTemplateRequestDto request,
        String creatorEmail
    ) {
        String name = request.name().trim();
        String nameNormalized = normalize(name);

        if (surveyTemplateRepository.existsByNameNormalized(nameNormalized)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ya existe una plantilla con ese nombre"
            );
        }

        SurveyTemplate template = SurveyTemplate.builder()
            .name(name)
            .nameNormalized(nameNormalized)
            .description(cleanOptional(request.description()))
            .templateStructure(surveyStructureFactory.buildTemplateStructure(request.blocks()))
            .status(TemplateStatus.ACTIVE)
            .createdBy(findCreatorByEmail(creatorEmail))
            .build();

        return toResponse(surveyTemplateRepository.save(template));
    }

    /**
     * Lista todas las plantillas.
     */
    public List<SurveyTemplateResponseDto> findAll() {
        return surveyTemplateRepository
            .findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Lista plantillas activas.
     */
    public List<SurveyTemplateResponseDto> findActive() {
        return surveyTemplateRepository
            .findByStatusOrderByNameAsc(TemplateStatus.ACTIVE)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Obtiene una plantilla por id.
     */
    public SurveyTemplateResponseDto findById(Long id) {
        return toResponse(getTemplate(id));
    }

    /**
     * Actualiza metadatos y, si se informa, sustituye la estructura completa.
     */
    @Transactional
    public SurveyTemplateResponseDto update(
        Long id,
        UpdateSurveyTemplateRequestDto request
    ) {
        SurveyTemplate template = getTemplate(id);
        String name = request.name().trim();
        String nameNormalized = normalize(name);

        surveyTemplateRepository.findByNameNormalized(nameNormalized)
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ya existe otra plantilla con ese nombre"
                );
            });

        template.setName(name);
        template.setNameNormalized(nameNormalized);
        template.setDescription(cleanOptional(request.description()));

        if (request.blocks() != null) {
            template.setTemplateStructure(
                surveyStructureFactory.buildTemplateStructure(request.blocks())
            );
        }

        return toResponse(template);
    }

    @Transactional
    public SurveyTemplateResponseDto activate(Long id) {
        return changeStatus(id, TemplateStatus.ACTIVE);
    }

    @Transactional
    public SurveyTemplateResponseDto disable(Long id) {
        return changeStatus(id, TemplateStatus.DISABLED);
    }

    @Transactional
    public SurveyTemplateResponseDto archive(Long id) {
        return changeStatus(id, TemplateStatus.ARCHIVED);
    }

    private SurveyTemplateResponseDto changeStatus(Long id, TemplateStatus status) {
        SurveyTemplate template = getTemplate(id);

        if (status == TemplateStatus.ACTIVE && !template.hasQuestions()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La plantilla debe contener al menos una pregunta para activarse"
            );
        }

        template.setStatus(status);

        return toResponse(template);
    }

    private SurveyTemplate getTemplate(Long id) {
        return surveyTemplateRepository.findDetailedById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la plantilla indicada"
            ));
    }

    private User findCreatorByEmail(String email) {
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

    private SurveyTemplateResponseDto toResponse(SurveyTemplate template) {
        return new SurveyTemplateResponseDto(
            template.getId(),
            template.getName(),
            template.getDescription(),
            template.getStatus(),
            template.getCreatedBy() != null ? template.getCreatedBy().getId() : null,
            toBlockResponses(template.getTemplateStructure()),
            template.getCreatedAt(),
            template.getUpdatedAt()
        );
    }

    private List<TemplateBlockResponseDto> toBlockResponses(SurveyStructure structure) {
        if (structure == null || structure.getBlocks() == null) {
            return List.of();
        }

        return structure.getBlocks()
            .stream()
            .sorted(Comparator.comparing(SurveyBlockSnapshot::getPosition))
            .map(this::toBlockResponse)
            .toList();
    }

    private TemplateBlockResponseDto toBlockResponse(SurveyBlockSnapshot block) {
        return new TemplateBlockResponseDto(
            block.getBlockKey(),
            block.getSourceBlockId(),
            block.getTitle(),
            toQuestionResponses(block.getQuestions())
        );
    }

    private List<TemplateQuestionResponseDto> toQuestionResponses(
        List<SurveyQuestionSnapshot> questions
    ) {
        if (questions == null) {
            return List.of();
        }

        return questions.stream()
            .sorted(Comparator.comparing(SurveyQuestionSnapshot::getPosition))
            .map(this::toQuestionResponse)
            .toList();
    }

    private TemplateQuestionResponseDto toQuestionResponse(
        SurveyQuestionSnapshot question
    ) {
        return new TemplateQuestionResponseDto(
            question.getQuestionKey(),
            question.getSource(),
            question.getSourceQuestionId(),
            question.getText(),
            question.getDescription(),
            question.getType(),
            question.getEvaluationDirection(),
            question.isRequired(),
            question.getMinValue(),
            question.getMaxValue(),
            question.getScaleDisplayType(),
            question.getLeftLabel(),
            question.getRightLabel(),
            toOptionResponses(question.getOptions()),
            toMatrixRowResponses(question.getMatrixRows())
        );
    }

    private List<OptionConfigResponseDto> toOptionResponses(List<OptionConfig> options) {
        if (options == null) {
            return List.of();
        }

        return options.stream()
            .map(option -> new OptionConfigResponseDto(
                option.getLabel(),
                option.getStoredValue(),
                option.getScoreValue(),
                option.isNonResponseOption(),
                option.isAllowsFreeText()
            ))
            .toList();
    }

    private List<MatrixRowResponseDto> toMatrixRowResponses(List<MatrixRowConfig> rows) {
        if (rows == null) {
            return List.of();
        }

        return rows.stream()
            .map(row -> new MatrixRowResponseDto(row.getKey(), row.getLabel()))
            .toList();
    }

    private String normalize(String value) {
        String normalized = Normalizer
            .normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .trim()
            .replaceAll("\\s+", " ");

        if (!StringUtils.hasText(normalized)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El nombre de la plantilla es obligatorio"
            );
        }

        return normalized;
    }

    private String cleanOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
