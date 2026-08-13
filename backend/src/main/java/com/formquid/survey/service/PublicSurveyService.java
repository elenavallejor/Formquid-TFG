package com.formquid.survey.service;

import com.formquid.fixedquestion.entity.MatrixRowConfig;
import com.formquid.fixedquestion.entity.OptionConfig;
import com.formquid.survey.dto.AccessSurveyRequestDto;
import com.formquid.survey.dto.PublicMatrixRowDto;
import com.formquid.survey.dto.PublicOptionDto;
import com.formquid.survey.dto.PublicSurveyBlockDto;
import com.formquid.survey.dto.PublicSurveyQuestionDto;
import com.formquid.survey.dto.PublicSurveyResponseDto;
import com.formquid.survey.dto.PublicSurveySummaryDto;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyBlock;
import com.formquid.survey.entity.SurveyQuestion;
import com.formquid.survey.entity.SurveyStatus;
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
 * Atiende a participantes anónimos y devuelve únicamente DTO públicos,
 * evitando exponer información administrativa o datos internos de evaluación.
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
     *
     * Las encuestas privadas deben accederse mediante accessByCode,
     * ya que requieren validar la contraseña.
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
     * Permite acceder a una encuesta mediante su código público.
     *
     * PUBLIC: acceso directo.
     * PRIVATE: requiere contraseña válida.
     * DRAFT/HIDDEN: no se exponen a participantes.
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

    /**
     * Recupera una encuesta con la estructura necesaria para el participante.
     */
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

    /**
     * Valida la contraseña de una encuesta privada.
     */
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

        if (!StringUtils.hasText(survey.getAccessPassword())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La encuesta privada no tiene contraseña configurada"
            );
        }

        if (!passwordEncoder.matches(
            accessPassword.trim(),
            survey.getAccessPassword()
        )) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La contraseña de acceso no es correcta"
            );
        }
    }

    /**
     * Construye el resumen mostrado en el listado público.
     */
    private PublicSurveySummaryDto toPublicSummary(Survey survey) {
        return new PublicSurveySummaryDto(
            survey.getPublicCode(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getImageUrl(),
            toTagNames(survey)
        );
    }

    /**
     * Construye la representación pública completa de una encuesta.
     */
    private PublicSurveyResponseDto toPublicResponse(Survey survey) {
        return new PublicSurveyResponseDto(
            survey.getPublicCode(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getImageUrl(),
            toTagNames(survey),
            toBlockResponses(survey.getBlocks())
        );
    }

    /**
     * Convierte las etiquetas a una lista ordenada de nombres.
     */
    private List<String> toTagNames(Survey survey) {
        if (survey.getTags() == null) {
            return List.of();
        }

        return survey.getTags()
            .stream()
            .map(tag -> tag.getName())
            .sorted()
            .toList();
    }

    /**
     * Convierte los bloques sin exponer referencias al catálogo interno.
     */
    private List<PublicSurveyBlockDto> toBlockResponses(
        List<SurveyBlock> blocks
    ) {
        if (blocks == null) {
            return List.of();
        }

        return blocks.stream()
            .sorted(Comparator.comparing(SurveyBlock::getPosition))
            .map(this::toBlockResponse)
            .toList();
    }

    /**
     * Convierte un bloque a su representación pública.
     */
    private PublicSurveyBlockDto toBlockResponse(SurveyBlock block) {
        return new PublicSurveyBlockDto(
            block.getTitle(),
            block.getPosition(),
            toQuestionResponses(block.getQuestions())
        );
    }

    /**
     * Convierte las preguntas de un bloque a DTO públicos.
     */
    private List<PublicSurveyQuestionDto> toQuestionResponses(
        List<SurveyQuestion> questions
    ) {
        if (questions == null) {
            return List.of();
        }

        return questions.stream()
            .sorted(Comparator.comparing(SurveyQuestion::getPosition))
            .map(this::toQuestionResponse)
            .toList();
    }

    /**
     * Convierte una pregunta sin exponer procedencia, puntuación
     * ni dirección de evaluación.
     */
    private PublicSurveyQuestionDto toQuestionResponse(
        SurveyQuestion question
    ) {
        return new PublicSurveyQuestionDto(
            question.getId(),
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

    /**
     * Convierte las opciones ocultando scoreValue.
     */
    private List<PublicOptionDto> toOptionResponses(
        List<OptionConfig> options
    ) {
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

    /**
     * Convierte las filas de matriz.
     */
    private List<PublicMatrixRowDto> toMatrixRowResponses(
        List<MatrixRowConfig> rows
    ) {
        if (rows == null) {
            return List.of();
        }

        return rows.stream()
            .map(row -> new PublicMatrixRowDto(
                row.getKey(),
                row.getLabel()
            ))
            .toList();
    }
}
