package com.formquid.participation.service;

import com.formquid.fixedquestion.entity.MatrixRowConfig;
import com.formquid.fixedquestion.entity.OptionConfig;
import com.formquid.fixedquestion.entity.QuestionType;
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
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyQuestion;
import com.formquid.survey.entity.SurveyStatus;
import com.formquid.survey.repository.SurveyQuestionRepository;
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
 * Las participaciones son anónimas: no se almacena información identificativa
 * del encuestado. Cada envío se valida contra la estructura actual de la
 * encuesta antes de guardarse.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationService {

    private static final int DECIMAL_SCALE = 2;

    private final ParticipationRepository participationRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final SurveyService surveyService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra una participación anónima en una encuesta pública o privada.
     *
     * Las encuestas públicas pueden responderse directamente. Las privadas
     * requieren una contraseña de acceso correcta.
     */
    @Transactional
    public ParticipationResponseDto submit(
        String publicCode,
        SubmitParticipationRequestDto request
    ) {
        validateRequest(request);

        Survey survey = getSurveyForAnswer(publicCode);
        validateSurveyCanBeAnswered(survey, request.accessPassword());

        List<SurveyQuestion> questions = surveyQuestionRepository.findBySurvey(survey.getId());

        if (questions.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La encuesta no contiene preguntas"
            );
        }

        Map<Long, SurveyQuestion> questionsById = toQuestionMap(questions);
        Map<Long, SubmitAnswerRequestDto> answersByQuestionId = toAnswerMap(request.answers());

        validateRequiredQuestions(questions, answersByQuestionId);

        Participation participation = Participation.builder()
            .survey(survey)
            .build();

        for (SubmitAnswerRequestDto answerRequest : request.answers()) {
            SurveyQuestion question = questionsById.get(answerRequest.surveyQuestionId());

            if (question == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La respuesta contiene una pregunta que no pertenece a la encuesta"
                );
            }

            if (!isEmptyOptionalAnswer(answerRequest, question)) {
                Answer answer = buildAnswer(answerRequest, question);
                participation.addAnswer(answer);
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
     *
     * Pueden consultarlas el propietario, colaboradores, administradores
     * y superadministradores.
     */
    public List<ParticipationResponseDto> getBySurveyId(
        Long surveyId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurveyById(surveyId);

        /*
         * SurveyService centraliza las reglas de acceso interno a encuestas,
         * por lo que ParticipationService no duplica lógica de permisos.
         */
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

    /**
     * Recupera el usuario autenticado a partir del correo utilizado
     * como identidad en Spring Security.
     */
    private User getAuthenticatedUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return userRepository
            .findByEmailIgnoreCase(email.trim())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario autenticado no encontrado"
            ));
    }

    /**
     * Valida que el DTO principal contenga respuestas.
     */
    private void validateRequest(SubmitParticipationRequestDto request) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La participación debe contener al menos una respuesta"
            );
        }
    }

    /**
     * Obtiene una encuesta por su código público aplicando bloqueo de lectura.
     *
     * Esto evita que la encuesta cambie de estado mientras se validan y
     * almacenan las respuestas.
     */
    private Survey getSurveyForAnswer(String publicCode) {
        return surveyRepository.findByPublicCodeForAnswer(publicCode)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la encuesta indicada"
            ));
    }

    /**
     * Obtiene una encuesta por id.
     */
    private Survey getSurveyById(Long surveyId) {
        return surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la encuesta indicada"
            ));
    }

    /**
     * Comprueba que una encuesta pueda recibir respuestas.
     */
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

    /**
     * Valida la contraseña de una encuesta privada.
     */
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

        if (!StringUtils.hasText(survey.getAccessPassword())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La encuesta privada no tiene contraseña configurada"
            );
        }

        if (!passwordEncoder.matches(accessPassword.trim(), survey.getAccessPassword())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La contraseña de acceso no es correcta"
            );
        }
    }

    /**
     * Construye un mapa de preguntas para acceder por id de forma eficiente.
     */
    private Map<Long, SurveyQuestion> toQuestionMap(List<SurveyQuestion> questions) {
        Map<Long, SurveyQuestion> result = new HashMap<>();

        for (SurveyQuestion question : questions) {
            result.put(question.getId(), question);
        }

        return result;
    }

    /**
     * Construye un mapa de respuestas e impide responder dos veces
     * a la misma pregunta en una participación.
     */
    private Map<Long, SubmitAnswerRequestDto> toAnswerMap(
        List<SubmitAnswerRequestDto> answers
    ) {
        Map<Long, SubmitAnswerRequestDto> result = new HashMap<>();

        for (SubmitAnswerRequestDto answer : answers) {
            if (answer.surveyQuestionId() == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador de la pregunta es obligatorio"
                );
            }

            if (result.put(answer.surveyQuestionId(), answer) != null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La participación contiene respuestas repetidas"
                );
            }
        }

        return result;
    }

    /**
     * Comprueba que todas las preguntas obligatorias tengan respuesta real.
     */
    private void validateRequiredQuestions(
        List<SurveyQuestion> questions,
        Map<Long, SubmitAnswerRequestDto> answersByQuestionId
    ) {
        for (SurveyQuestion question : questions) {
            if (!question.isRequired()) {
                continue;
            }

            SubmitAnswerRequestDto answer = answersByQuestionId.get(question.getId());

            if (answer == null || !hasSubmittedValue(answer, question.getType())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Faltan respuestas obligatorias"
                );
            }
        }
    }

    /**
     * Indica si una respuesta opcional viene vacía.
     */
    private boolean isEmptyOptionalAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestion question
    ) {
        return !question.isRequired()
            && !hasSubmittedValue(request, question.getType());
    }

    /**
     * Comprueba si el DTO contiene un valor real según el tipo de pregunta.
     */
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

    /**
     * Construye una respuesta validando su tipo.
     */
    private Answer buildAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestion question
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

    /**
     * Construye una respuesta textual.
     */
    private Answer buildTextAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestion question
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

    /**
     * Construye una respuesta numérica.
     */
    private Answer buildNumberAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestion question
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

    /**
     * Construye una respuesta booleana.
     */
    private Answer buildBooleanAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestion question
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

    /**
     * Construye una respuesta de escala.
     */
    private Answer buildScaleAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestion question
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

    /**
     * Construye una respuesta de selección simple o múltiple.
     */
    private Answer buildChoiceAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestion question,
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

    /**
     * Construye una respuesta de matriz.
     */
    private Answer buildMatrixAnswer(
        SubmitAnswerRequestDto request,
        SurveyQuestion question
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

    /**
     * Crea el builder común de una respuesta.
     */
    private Answer.AnswerBuilder baseAnswer(SurveyQuestion question) {
        return Answer.builder()
            .surveyQuestion(question);
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

    /**
     * Crea una excepción homogénea para formatos incompatibles.
     */
    private ResponseStatusException invalidAnswerFormat() {
        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El formato de respuesta no coincide con el tipo de pregunta"
        );
    }

    /**
     * Comprueba que una respuesta numérica esté dentro del rango permitido.
     */
    private void validateRange(BigDecimal value, SurveyQuestion question) {
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

    /**
     * Crea un mapa de opciones usando storedValue como clave.
     */
    private Map<String, OptionConfig> toOptionsByValue(SurveyQuestion question) {
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

    /**
     * Comprueba que las opciones seleccionadas existan.
     */
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

    /**
     * Impide enviar opciones repetidas.
     */
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

    /**
     * Resuelve texto libre para opciones que lo permiten.
     */
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

    /**
     * Calcula puntuación total de selección.
     */
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

    /**
     * Convierte y valida respuestas de matriz.
     */
    private List<MatrixAnswerValue> toMatrixValues(
        List<MatrixAnswerRequestDto> requestValues,
        SurveyQuestion question
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

    /**
     * Obtiene las filas válidas de una pregunta de matriz.
     */
    private Set<String> getAllowedMatrixRows(SurveyQuestion question) {
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

    /**
     * Construye y valida el valor de una fila de matriz.
     */
    private MatrixAnswerValue buildMatrixValue(
        MatrixAnswerRequestDto request,
        SurveyQuestion question,
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

    /**
     * Calcula la media de puntuación de una respuesta de matriz.
     */
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

    /**
     * Convierte el valor recibido para una fila de matriz a número.
     */
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

    /**
     * Convierte una participación a DTO.
     */
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

    /**
     * Convierte una respuesta a DTO.
     */
    private AnswerResponseDto toAnswerResponse(Answer answer) {
        return new AnswerResponseDto(
            answer.getId(),
            answer.getSurveyQuestion().getId(),
            answer.getSurveyQuestion().getText(),
            answer.getTextValue(),
            answer.getNumericValue(),
            answer.getBooleanValue(),
            answer.getSelectedValues(),
            toMatrixResponses(answer.getMatrixValues()),
            answer.getFreeTextValue(),
            answer.getScoreValue()
        );
    }

    /**
     * Convierte valores de matriz a DTO.
     */
    private List<MatrixAnswerResponseDto> toMatrixResponses(List<MatrixAnswerValue> values) {
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

    /**
     * Limpia las opciones seleccionadas eliminando valores vacíos.
     */
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