package com.formquid.survey.service;

import com.formquid.question.entity.MatrixRowConfig;
import com.formquid.question.entity.OptionConfig;
import com.formquid.survey.dto.AccessSurveyRequestDto;
import com.formquid.survey.dto.PublicMatrixRowDto;
import com.formquid.survey.dto.PublicOptionDto;
import com.formquid.survey.dto.PublicSurveyBlockDto;
import com.formquid.survey.dto.PublicSurveyQuestionDto;
import com.formquid.survey.dto.PublicSurveyResponseDto;
import com.formquid.survey.dto.PublicSurveySummaryDto;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyStatus;
import com.formquid.survey.model.SurveyBlockSnapshot;
import com.formquid.survey.model.SurveyQuestionSnapshot;
import com.formquid.survey.model.SurveyStructure;
import com.formquid.survey.repository.SurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/**
 * Servicio encargado exclusivamente del acceso público a encuestas.
 *
 * Devuelve únicamente el snapshot necesario para mostrar y responder la
 * encuesta, sin exponer datos administrativos ni puntuaciones internas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicSurveyService {

    private final SurveyRepository surveyRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Lista las encuestas públicas visibles en la página de inicio.
     */
    public List<PublicSurveySummaryDto> findPublic() {
        return surveyRepository
            .findPublicByStatusOrderByCreatedAtDesc(SurveyStatus.PUBLIC)
            .stream()
            .map(this::toPublicSummary)
            .toList();
    }

    /**
     * Obtiene directamente una encuesta con estado PUBLIC.
     */
    public PublicSurveyResponseDto findPublicByCode(String publicCode) {
        Survey survey = getSurveyByPublicCode(publicCode);

        if (survey.getStatus() != SurveyStatus.PUBLIC) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe una encuesta pública con el código indicado"
            );
        }

        return toPublicResponse(survey);
    }

    /**
     * Permite acceder a una encuesta pública o privada mediante código.
     */
    public PublicSurveyResponseDto accessByCode(
        String publicCode,
        AccessSurveyRequestDto request
    ) {
        Survey survey = getSurveyByPublicCode(publicCode);

        if (survey.getStatus() == SurveyStatus.PUBLIC) {
            return toPublicResponse(survey);
        }

        if (survey.getStatus() == SurveyStatus.PRIVATE) {
            validatePrivateAccess(
                survey,
                request != null ? request.accessPassword() : null
            );

            return toPublicResponse(survey);
        }

        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No existe una encuesta disponible con el código indicado"
        );
    }

    private Survey getSurveyByPublicCode(String publicCode) {
        if (!StringUtils.hasText(publicCode)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El código público de la encuesta es obligatorio"
            );
        }

        return surveyRepository
            .findPublicStructureByPublicCode(publicCode.trim())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe una encuesta con el código indicado"
            ));
    }

    private void validatePrivateAccess(
        Survey survey,
        String accessPassword
    ) {
        if (!StringUtils.hasText(accessPassword)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La encuesta privada requiere contraseña de acceso"
            );
        }

        if (!StringUtils.hasText(survey.getAccessPasswordHash())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La encuesta privada no tiene contraseña configurada"
            );
        }

        if (!passwordEncoder.matches(accessPassword.trim(), survey.getAccessPasswordHash())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La contraseña de acceso no es correcta"
            );
        }
    }

    private PublicSurveySummaryDto toPublicSummary(Survey survey) {
        return new PublicSurveySummaryDto(
            survey.getPublicCode(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getImageUrl(),
            toTagNames(survey)
        );
    }

    private PublicSurveyResponseDto toPublicResponse(Survey survey) {
        return new PublicSurveyResponseDto(
            survey.getPublicCode(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getImageUrl(),
            toTagNames(survey),
            toBlockResponses(survey.getStructure())
        );
    }

    private List<String> toTagNames(Survey survey) {
        if (survey.getTagNames() == null) {
            return List.of();
        }

        return survey.getTagNames()
            .stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private List<PublicSurveyBlockDto> toBlockResponses(SurveyStructure structure) {
        if (structure == null || structure.getBlocks() == null) {
            return List.of();
        }

        return structure.getBlocks()
            .stream()
            .sorted(Comparator.comparing(SurveyBlockSnapshot::getPosition))
            .map(this::toBlockResponse)
            .toList();
    }

    private PublicSurveyBlockDto toBlockResponse(SurveyBlockSnapshot block) {
        return new PublicSurveyBlockDto(
            block.getTitle(),
            block.getPosition(),
            toQuestionResponses(block.getQuestions())
        );
    }

    private List<PublicSurveyQuestionDto> toQuestionResponses(
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

    private PublicSurveyQuestionDto toQuestionResponse(
        SurveyQuestionSnapshot question
    ) {
        return new PublicSurveyQuestionDto(
            question.getQuestionKey(),
            question.getText(),
            question.getDescription(),
            question.getType(),
            question.isRequired(),
            question.getMinValue(),
            question.getMaxValue(),
            question.getScaleDisplayType(),
            question.getLeftLabel(),
            question.getRightLabel(),
            toOptionResponses(question.getOptions()),
            toMatrixRowResponses(question.getMatrixRows()),
            question.getPosition()
        );
    }

    private List<PublicOptionDto> toOptionResponses(List<OptionConfig> options) {
        if (options == null) {
            return List.of();
        }

        return options.stream()
            .map(option -> new PublicOptionDto(
                option.getLabel(),
                option.getStoredValue(),
                option.isNonResponseOption(),
                option.isAllowsFreeText()
            ))
            .toList();
    }

    private List<PublicMatrixRowDto> toMatrixRowResponses(List<MatrixRowConfig> rows) {
        if (rows == null) {
            return List.of();
        }

        return rows.stream()
            .map(row -> new PublicMatrixRowDto(row.getKey(), row.getLabel()))
            .toList();
    }
}
