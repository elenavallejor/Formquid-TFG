package com.formquid.participation.service;

import com.formquid.participation.dto.AnswerResponseDto;
import com.formquid.participation.dto.MatrixAnswerRequestDto;
import com.formquid.participation.dto.MatrixAnswerResponseDto;
import com.formquid.participation.dto.ParticipationResponseDto;
import com.formquid.participation.dto.SubmitAnswerRequestDto;
import com.formquid.participation.dto.SubmitParticipationRequestDto;
import com.formquid.participation.entity.Answer;
import com.formquid.participation.entity.MatrixAnswerValue;
import com.formquid.participation.entity.Participation;
import com.formquid.participation.repository.ParticipationRepository;
import com.formquid.question.entity.MatrixRowConfig;
import com.formquid.question.entity.OptionConfig;
import com.formquid.question.entity.QuestionType;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyStatus;
import com.formquid.survey.model.SurveyQuestionSnapshot;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servicio de negocio para el envío y consulta de participaciones.
 *
 * Las respuestas se validan contra el snapshot JSONB de la encuesta. No existe
 * una tabla survey_questions: cada respuesta queda asociada a questionKey.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationService {

    private static final int DECIMAL_SCALE = 2;

    private final ParticipationRepository participationRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyService surveyService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra una participación anónima en una encuesta pública o privada.
     */
    @Transactional
    public ParticipationResponseDto submit(
        String publicCode,
        SubmitParticipationRequestDto request
    ) {
        validateRequest(request);

        Survey survey = getSurveyForAnswer(publicCode);
        validateSurveyCanBeAnswered(survey, request.accessPassword());

        List<SurveyQuestionSnapshot> questions = getSurveyQuestions(survey);
        Map<String, SurveyQuestionSnapshot> questionsByKey = toQuestionMap(questions);
        Map<String, SubmitAnswerRequestDto> answersByQuestionKey = toAnswerMap(request.answers());

        validateRequiredQuestions(questions, answersByQuestionKey);

        Participation participation = Participation.builder()
            .survey(survey)
            .build();

        for (SubmitAnswerRequestDto answerRequest : request.answers()) {
            SurveyQuestionSnapshot question =
                questionsByKey.get(answerRequest.questionKey().trim());

            if (question == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La respuesta contiene una pregunta que no pertenece a la encuesta"
                );
            }

            if (!isEmptyOptionalAnswer(answerRequest, question)) {
                participation.addAnswer(buildAnswer(answerRequest, question));
            }
        }

        if (participation.getAnswers().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La participación debe contener al menos una respuesta válida"
            );
        }

        return toResponse(participationRepository.save(participation));
    }

    /**
     * Obtiene las participaciones de una encuesta aplicando permisos.
     */
    public List<ParticipationResponseDto> getBySurveyId(
        Long surveyId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurveyById(surveyId);

        surveyService.assertCanViewResults(
            survey,
            user.getId(),
            user.getRole()
        );

        return participationRepository
            .findBySurveyIdOrderBySubmittedAtDesc(surveyId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private List<SurveyQuestionSnapshot> getSurveyQuestions(Survey survey) {
        if (survey.getStructure() == null || !survey.getStructure().hasQuestions()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La encuesta no contiene preguntas"
            );
        }

        return survey.getStructure().getQuestions();
    }

    private User getAuthenticatedUser(String email) {
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

    private void validateRequest(SubmitParticipationRequestDto request) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La participación debe contener al menos una respuesta"
            );
        }
    }

    private Survey getSurveyForAnswer(String publicCode) {
        return surveyRepository.findByPublicCodeForAnswer(publicCode)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la encuesta indicada"
            ));
    }

    private Survey getSurveyById(Long surveyId) {
        return surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la encuesta indicada"
            ));
    }

    private void validateSurveyCanBeAnswered(
        Survey survey,
        String accessPassword
    ) {
        if (survey.getStatus() == SurveyStatus.PUBLIC) {
            return;
        }

        if (survey.getStatus() == SurveyStatus.PRIVATE) {
            validatePrivateSurveyPassword(survey, accessPassword);
            return;
        }

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La encuesta no está disponible para ser respondida"
        );
    }

    private void validatePrivateSurveyPassword(
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

    private Map<String, SurveyQuestionSnapshot> toQuestionMap(
        List<SurveyQuestionSnapshot> questions
    ) {
        Map<String, SurveyQuestionSnapshot> result = new HashMap<>();

        for (SurveyQuestionSnapshot question : questions) {
            result.put(question.getQuestionKey(), question);
        }

        return result;
    }

    private Map<String, SubmitAnswerRequestDto> toAnswerMap(
        List<SubmitAnswerRequestDto> answers
    ) {
        Map<String, SubmitAnswerRequestDto> result = new HashMap<>();

        for (SubmitAnswerRequestDto answer : answers) {
            if (!StringUtils.hasText(answer.questionKey())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La clave de la pregunta es obligatoria"
                );
            }

            if (result.put(answer.questionKey().trim(), answer) != null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La participación contiene respuestas repetidas"
                );
            }
        }

        return result;
    }

    private void validateRequiredQuestions(
        List<SurveyQuestionSnapshot> questions,
        Map<String, SubmitAnswerRequestDto> answersByQuestionKey
    ) {
        for (SurveyQuestionSnapshot question : questions) {
            if (!question.isRequired()) {
                continue;
            }

            SubmitAnswerRequestDto answer =
                answersByQuestionKey.get(question.getQuestionKey());

            if (answer == null || !hasSubmittedValue(answer, question.getType())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Faltan respuestas obligatorias"
                );
            }
        }
    }

    private boolean isEmptyOptionalAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestionSnapshot question
    ) {
        return !question.isRequired()
            && !hasSubmittedValue(request, question.getType());
    }

    private boolean hasSubmittedValue(
        SubmitAnswerRequestDto request,
        QuestionType type
    ) {
        return switch (type) {
            case SHORT_TEXT, LONG_TEXT -> StringUtils.hasText(request.textValue());
            case NUMBER, SCALE, BIPOLAR_SCALE -> request.numericValue() != null;
            case BOOLEAN -> request.booleanValue() != null;
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> hasSelectedValues(request.selectedValues());
            case MATRIX_SCALE -> hasMatrixValues(request.matrixValues());
        };
    }

    private Answer buildAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestionSnapshot question
    ) {
        return switch (question.getType()) {
            case SHORT_TEXT, LONG_TEXT -> buildTextAnswer(request, question);
            case NUMBER -> buildNumberAnswer(request, question);
            case BOOLEAN -> buildBooleanAnswer(request, question);
            case SCALE, BIPOLAR_SCALE -> buildScaleAnswer(request, question);
            case SINGLE_CHOICE -> buildChoiceAnswer(request, question, false);
            case MULTIPLE_CHOICE -> buildChoiceAnswer(request, question, true);
            case MATRIX_SCALE -> buildMatrixAnswer(request, question);
        };
    }

    private Answer buildTextAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestionSnapshot question
    ) {
        if (!StringUtils.hasText(request.textValue())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La respuesta textual es obligatoria"
            );
        }

        validateOnlyText(request);

        return baseAnswer(question)
            .textValue(request.textValue().trim())
            .build();
    }

    private Answer buildNumberAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestionSnapshot question
    ) {
        if (request.numericValue() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La respuesta numérica es obligatoria"
            );
        }

        validateOnlyNumber(request);
        validateRange(request.numericValue(), question);

        return baseAnswer(question)
            .numericValue(request.numericValue())
            .scoreValue(request.numericValue())
            .build();
    }

    private Answer buildBooleanAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestionSnapshot question
    ) {
        if (request.booleanValue() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La respuesta booleana es obligatoria"
            );
        }

        validateOnlyBoolean(request);

        return baseAnswer(question)
            .booleanValue(request.booleanValue())
            .build();
    }

    private Answer buildScaleAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestionSnapshot question
    ) {
        if (request.numericValue() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La respuesta de escala es obligatoria"
            );
        }

        validateOnlyNumber(request);
        validateRange(request.numericValue(), question);

        return baseAnswer(question)
            .numericValue(request.numericValue())
            .scoreValue(request.numericValue())
            .build();
    }

    private Answer buildChoiceAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestionSnapshot question,
        boolean multiple
    ) {
        List<String> selectedValues = cleanSelectedValues(request.selectedValues());

        if (selectedValues.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La pregunta requiere al menos una opción seleccionada"
            );
        }

        if (!multiple && selectedValues.size() != 1) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La pregunta solo permite seleccionar una opción"
            );
        }

        validateOnlyChoice(request);
        validateNoDuplicatedValues(selectedValues);

        Map<String, OptionConfig> optionsByValue = toOptionsByValue(question);
        validateSelectedValues(selectedValues, optionsByValue);

        String freeText = resolveChoiceFreeText(
            request.freeTextValue(),
            selectedValues,
            optionsByValue
        );

        return baseAnswer(question)
            .selectedValues(selectedValues)
            .freeTextValue(freeText)
            .scoreValue(calculateChoiceScore(selectedValues, optionsByValue))
            .build();
    }

    private Answer buildMatrixAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestionSnapshot question
    ) {
        List<MatrixAnswerValue> values = toMatrixValues(request.matrixValues(), question);

        if (values.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La respuesta de matriz es obligatoria"
            );
        }

        validateOnlyMatrix(request);

        return baseAnswer(question)
            .matrixValues(values)
            .scoreValue(calculateMatrixScore(values))
            .build();
    }

    private Answer.AnswerBuilder baseAnswer(SurveyQuestionSnapshot question) {
        return Answer.builder()
            .questionKey(question.getQuestionKey())
            .questionText(question.getText());
    }

    private void validateOnlyText(SubmitAnswerRequestDto request) {
        if (request.numericValue() != null
            || request.booleanValue() != null
            || hasSelectedValues(request.selectedValues())
            || hasMatrixValues(request.matrixValues())
            || StringUtils.hasText(request.freeTextValue())) {
            throw invalidAnswerFormat();
        }
    }

    private void validateOnlyNumber(SubmitAnswerRequestDto request) {
        if (StringUtils.hasText(request.textValue())
            || request.booleanValue() != null
            || hasSelectedValues(request.selectedValues())
            || hasMatrixValues(request.matrixValues())
            || StringUtils.hasText(request.freeTextValue())) {
            throw invalidAnswerFormat();
        }
    }

    private void validateOnlyBoolean(SubmitAnswerRequestDto request) {
        if (StringUtils.hasText(request.textValue())
            || request.numericValue() != null
            || hasSelectedValues(request.selectedValues())
            || hasMatrixValues(request.matrixValues())
            || StringUtils.hasText(request.freeTextValue())) {
            throw invalidAnswerFormat();
        }
    }

    private void validateOnlyChoice(SubmitAnswerRequestDto request) {
        if (StringUtils.hasText(request.textValue())
            || request.numericValue() != null
            || request.booleanValue() != null
            || hasMatrixValues(request.matrixValues())) {
            throw invalidAnswerFormat();
        }
    }

    private void validateOnlyMatrix(SubmitAnswerRequestDto request) {
        if (StringUtils.hasText(request.textValue())
            || request.numericValue() != null
            || request.booleanValue() != null
            || hasSelectedValues(request.selectedValues())
            || StringUtils.hasText(request.freeTextValue())) {
            throw invalidAnswerFormat();
        }
    }

    private ResponseStatusException invalidAnswerFormat() {
        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El formato de respuesta no coincide con el tipo de pregunta"
        );
    }

    private void validateRange(
        BigDecimal value,
        SurveyQuestionSnapshot question
    ) {
        Integer min = question.getMinValue();
        Integer max = question.getMaxValue();

        if (min != null && value.compareTo(BigDecimal.valueOf(min)) < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El valor está por debajo del mínimo permitido"
            );
        }

        if (max != null && value.compareTo(BigDecimal.valueOf(max)) > 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El valor supera el máximo permitido"
            );
        }
    }

    private Map<String, OptionConfig> toOptionsByValue(
        SurveyQuestionSnapshot question
    ) {
        Map<String, OptionConfig> result = new HashMap<>();

        if (question.getOptions() == null) {
            return result;
        }

        for (OptionConfig option : question.getOptions()) {
            if (StringUtils.hasText(option.getStoredValue())) {
                result.put(option.getStoredValue(), option);
            }
        }

        return result;
    }

    private void validateSelectedValues(
        List<String> selectedValues,
        Map<String, OptionConfig> optionsByValue
    ) {
        for (String value : selectedValues) {
            if (!optionsByValue.containsKey(value)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La respuesta contiene una opción no válida"
                );
            }
        }
    }

    private void validateNoDuplicatedValues(List<String> values) {
        Set<String> usedValues = new HashSet<>();

        for (String value : values) {
            if (!usedValues.add(value)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La respuesta contiene opciones repetidas"
                );
            }
        }
    }

    private String resolveChoiceFreeText(
        String freeTextValue,
        List<String> selectedValues,
        Map<String, OptionConfig> optionsByValue
    ) {
        if (!StringUtils.hasText(freeTextValue)) {
            return null;
        }

        for (String value : selectedValues) {
            OptionConfig option = optionsByValue.get(value);

            if (option != null && option.isAllowsFreeText()) {
                return freeTextValue.trim();
            }
        }

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El texto libre solo está permitido para opciones configuradas para ello"
        );
    }

    private BigDecimal calculateChoiceScore(
        List<String> values,
        Map<String, OptionConfig> optionsByValue
    ) {
        BigDecimal total = BigDecimal.ZERO;
        boolean hasScore = false;

        for (String value : values) {
            OptionConfig option = optionsByValue.get(value);

            if (option != null && option.getScoreValue() != null) {
                total = total.add(BigDecimal.valueOf(option.getScoreValue()));
                hasScore = true;
            }
        }

        return hasScore ? total : null;
    }

    private List<MatrixAnswerValue> toMatrixValues(
        List<MatrixAnswerRequestDto> requestValues,
        SurveyQuestionSnapshot question
    ) {
        if (requestValues == null || requestValues.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> allowedRows = getAllowedMatrixRows(question);
        Set<String> usedRows = new HashSet<>();
        List<MatrixAnswerValue> result = new ArrayList<>();

        for (MatrixAnswerRequestDto value : requestValues) {
            if (value == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La respuesta de matriz contiene una fila inválida"
                );
            }

            result.add(buildMatrixValue(value, question, allowedRows, usedRows));
        }

        if (question.isRequired() && result.size() != allowedRows.size()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La pregunta de matriz obligatoria debe responder todas sus filas"
            );
        }

        return result;
    }

    private Set<String> getAllowedMatrixRows(SurveyQuestionSnapshot question) {
        Set<String> allowedRows = new HashSet<>();

        if (question.getMatrixRows() == null) {
            return allowedRows;
        }

        for (MatrixRowConfig row : question.getMatrixRows()) {
            if (StringUtils.hasText(row.getKey())) {
                allowedRows.add(row.getKey());
            }
        }

        return allowedRows;
    }

    private MatrixAnswerValue buildMatrixValue(
        MatrixAnswerRequestDto request,
        SurveyQuestionSnapshot question,
        Set<String> allowedRows,
        Set<String> usedRows
    ) {
        if (!StringUtils.hasText(request.rowKey())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La fila de matriz es obligatoria"
            );
        }

        String rowKey = request.rowKey().trim();

        if (!allowedRows.contains(rowKey)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La respuesta contiene una fila de matriz no válida"
            );
        }

        if (!usedRows.add(rowKey)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La respuesta contiene filas de matriz repetidas"
            );
        }

        BigDecimal numericValue = parseMatrixValue(request.value());
        validateRange(numericValue, question);

        return MatrixAnswerValue.builder()
            .rowKey(rowKey)
            .value(request.value().trim())
            .scoreValue(numericValue.intValue())
            .build();
    }

    private BigDecimal calculateMatrixScore(List<MatrixAnswerValue> values) {
        if (values.isEmpty()) {
            return null;
        }

        BigDecimal total = BigDecimal.ZERO;

        for (MatrixAnswerValue value : values) {
            if (value.getScoreValue() != null) {
                total = total.add(BigDecimal.valueOf(value.getScoreValue()));
            }
        }

        return total.divide(
            BigDecimal.valueOf(values.size()),
            DECIMAL_SCALE,
            RoundingMode.HALF_UP
        );
    }

    private BigDecimal parseMatrixValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El valor de matriz es obligatorio"
            );
        }

        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El valor de matriz debe ser numérico"
            );
        }
    }

    private ParticipationResponseDto toResponse(Participation participation) {
        return new ParticipationResponseDto(
            participation.getId(),
            participation.getSurvey().getId(),
            participation.getSubmittedAt(),
            participation.getAnswers()
                .stream()
                .map(this::toAnswerResponse)
                .toList()
        );
    }

    private AnswerResponseDto toAnswerResponse(Answer answer) {
        return new AnswerResponseDto(
            answer.getId(),
            answer.getQuestionKey(),
            answer.getQuestionText(),
            answer.getTextValue(),
            answer.getNumericValue(),
            answer.getBooleanValue(),
            answer.getSelectedValues(),
            toMatrixResponses(answer.getMatrixValues()),
            answer.getFreeTextValue(),
            answer.getScoreValue()
        );
    }

    private List<MatrixAnswerResponseDto> toMatrixResponses(
        List<MatrixAnswerValue> values
    ) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
            .map(value -> new MatrixAnswerResponseDto(
                value.getRowKey(),
                value.getValue(),
                value.getScoreValue()
            ))
            .toList();
    }

    private List<String> cleanSelectedValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }

        return values.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();
    }

    private boolean hasSelectedValues(List<String> values) {
        return values != null && values.stream().anyMatch(StringUtils::hasText);
    }

    private boolean hasMatrixValues(List<MatrixAnswerRequestDto> values) {
        return values != null && !values.isEmpty();
    }
}
