package com.formquid.statistics.service;

import com.formquid.fixedquestion.entity.MatrixRowConfig;
import com.formquid.fixedquestion.entity.OptionConfig;
import com.formquid.participation.repository.AnswerRepository;
import com.formquid.participation.repository.ParticipationRepository;
import com.formquid.statistics.dto.ChoiceOptionStatisticsDto;
import com.formquid.statistics.dto.MatrixRowStatisticsDto;
import com.formquid.statistics.dto.NumericStatisticsDto;
import com.formquid.statistics.dto.QuestionStatisticsResponseDto;
import com.formquid.statistics.dto.SurveyStatisticsResponseDto;
import com.formquid.statistics.projection.BooleanStatisticsProjection;
import com.formquid.statistics.projection.ChoiceCountProjection;
import com.formquid.statistics.projection.MatrixRowStatisticsProjection;
import com.formquid.statistics.projection.NumericStatisticsProjection;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyQuestion;
import com.formquid.survey.repository.SurveyQuestionRepository;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de negocio encargado de calcular estadísticas de encuestas.
 *
 * Las estadísticas se calculan dinámicamente a partir de las respuestas.
 * Las operaciones pesadas de agregación (COUNT, AVG, MIN y MAX) se delegan
 * en PostgreSQL mediante consultas del repositorio, evitando cargar grandes
 * colecciones de Answer en memoria.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    /**
     * Número máximo de respuestas textuales devueltas como muestra.
     *
     * Se limita el contenido textual para impedir respuestas HTTP
     * innecesariamente grandes cuando una encuesta acumula muchas respuestas.
     */
    private static final int MAX_TEXT_ANSWERS = 20;

    /**
     * Número de decimales utilizado para medias y porcentajes.
     */
    private static final int DECIMAL_SCALE = 2;

    private final SurveyRepository surveyRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final ParticipationRepository participationRepository;
    private final AnswerRepository answerRepository;
    private final SurveyService surveyService;
    private final UserRepository userRepository;

    /**
     * Obtiene las estadísticas globales de una encuesta.
     *
     * Antes de realizar los cálculos se comprueba que el usuario autenticado
     * tenga permiso para consultar resultados. Pueden acceder el propietario,
     * los colaboradores y los usuarios con permisos administrativos.
     *
     * @param surveyId identificador de la encuesta.
     * @param userEmail correo del usuario autenticado.
     * @return estadísticas generales y desglose de todas las preguntas.
     */
    public SurveyStatisticsResponseDto getSurveyStatistics(
        Long surveyId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        validateResultsAccess(
            survey,
            user
        );

        /*
         * El total de participaciones se calcula directamente mediante COUNT
         * en base de datos.
         */
        long totalParticipations =
            participationRepository.countBySurveyId(surveyId);

        /*
         * Solo se cargan las preguntas de la encuesta. Los valores agregados
         * de sus respuestas se calculan posteriormente en PostgreSQL.
         */
        List<SurveyQuestion> questions =
            surveyQuestionRepository.findBySurvey(surveyId);

        List<QuestionStatisticsResponseDto> questionStatistics =
            questions.stream()
                .map(this::buildQuestionStatistics)
                .toList();

        return new SurveyStatisticsResponseDto(
            survey.getId(),
            survey.getTitle(),
            totalParticipations,
            questionStatistics
        );
    }

    /**
     * Obtiene las estadísticas de una pregunta concreta.
     *
     * Además de comprobar los permisos sobre la encuesta, la consulta del
     * repositorio garantiza que la pregunta indicada pertenezca realmente
     * a esa encuesta.
     *
     * @param surveyId identificador de la encuesta.
     * @param questionId identificador de la pregunta.
     * @param userEmail correo del usuario autenticado.
     * @return estadísticas de la pregunta.
     */
    public QuestionStatisticsResponseDto getQuestionStatistics(
        Long surveyId,
        Long questionId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        validateResultsAccess(
            survey,
            user
        );

        SurveyQuestion question =
            getQuestion(
                surveyId,
                questionId
            );

        return buildQuestionStatistics(question);
    }

    /**
     * Centraliza la comprobación de acceso a resultados.
     *
     * La regla de autorización se mantiene en SurveyService para evitar
     * duplicarla en cada módulo que necesita consultar datos de una encuesta.
     */
    private void validateResultsAccess(
        Survey survey,
        User user
    ) {
        surveyService.assertCanViewResults(
            survey,
            user.getId(),
            user.getRole()
        );
    }

    /**
     * Recupera el usuario autenticado mediante el correo almacenado
     * en java.security.Principal.
     */
    private User getAuthenticatedUser(
        String email
    ) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return userRepository
            .findByEmailIgnoreCase(email.trim())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario autenticado no encontrado"
                )
            );
    }

    /**
     * Obtiene una encuesta o devuelve error 404 si no existe.
     */
    private Survey getSurvey(
        Long surveyId
    ) {
        return surveyRepository
            .findById(surveyId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe la encuesta indicada"
                )
            );
    }

    /**
     * Obtiene una pregunta comprobando simultáneamente que pertenezca
     * a la encuesta indicada.
     */
    private SurveyQuestion getQuestion(
        Long surveyId,
        Long questionId
    ) {
        return surveyQuestionRepository
            .findByIdAndSurveyId(
                questionId,
                surveyId
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe la pregunta indicada en esta encuesta"
                )
            );
    }

    /**
     * Selecciona el cálculo estadístico adecuado según el tipo de pregunta.
     *
     * El flujo principal permanece compacto y cada tipo se procesa
     * en un método especializado.
     */
    private QuestionStatisticsResponseDto buildQuestionStatistics(
        SurveyQuestion question
    ) {
        return switch (question.getType()) {
            case SHORT_TEXT, LONG_TEXT ->
                buildTextQuestionStatistics(question);

            case NUMBER, SCALE, BIPOLAR_SCALE ->
                buildNumericQuestionStatistics(question);

            case BOOLEAN ->
                buildBooleanQuestionStatistics(question);

            case SINGLE_CHOICE, MULTIPLE_CHOICE ->
                buildSelectionQuestionStatistics(question);

            case MATRIX_SCALE ->
                buildMatrixQuestionStatistics(question);
        };
    }

    /**
     * Construye estadísticas para preguntas textuales.
     *
     * Solo se devuelve una muestra limitada de respuestas para evitar
     * transferir todo el contenido textual almacenado.
     */
    private QuestionStatisticsResponseDto buildTextQuestionStatistics(
        SurveyQuestion question
    ) {
        long totalAnswers =
            answerRepository.countTextAnswers(
                question.getId()
            );

        List<String> textAnswers =
            answerRepository.findTextAnswerSample(
                question.getId(),
                MAX_TEXT_ANSWERS
            );

        return new QuestionStatisticsResponseDto(
            question.getId(),
            question.getText(),
            question.getType(),
            question.isRequired(),
            totalAnswers,
            null,
            List.of(),
            List.of(),
            textAnswers != null
                ? textAnswers
                : List.of()
        );
    }

    /**
     * Construye estadísticas para preguntas numéricas y escalas.
     *
     * AVG, MIN, MAX y COUNT se calculan directamente en PostgreSQL.
     */
    private QuestionStatisticsResponseDto buildNumericQuestionStatistics(
        SurveyQuestion question
    ) {
        NumericStatisticsDto statistics =
            toNumericStatistics(
                answerRepository.calculateNumericStatistics(
                    question.getId()
                )
            );

        return new QuestionStatisticsResponseDto(
            question.getId(),
            question.getText(),
            question.getType(),
            question.isRequired(),
            statistics.count(),
            statistics,
            List.of(),
            List.of(),
            List.of()
        );
    }

    /**
     * Construye estadísticas para preguntas booleanas.
     */
    private QuestionStatisticsResponseDto buildBooleanQuestionStatistics(
        SurveyQuestion question
    ) {
        BooleanStatisticsProjection projection =
            answerRepository.calculateBooleanStatistics(
                question.getId()
            );

        long yesCount = projection != null
            ? valueOrZero(projection.getYesCount())
            : 0;

        long noCount = projection != null
            ? valueOrZero(projection.getNoCount())
            : 0;

        long totalAnswers =
            yesCount + noCount;

        List<ChoiceOptionStatisticsDto> choiceStatistics =
            List.of(
                new ChoiceOptionStatisticsDto(
                    "true",
                    "Sí",
                    yesCount,
                    percentage(
                        yesCount,
                        totalAnswers
                    )
                ),
                new ChoiceOptionStatisticsDto(
                    "false",
                    "No",
                    noCount,
                    percentage(
                        noCount,
                        totalAnswers
                    )
                )
            );

        return new QuestionStatisticsResponseDto(
            question.getId(),
            question.getText(),
            question.getType(),
            question.isRequired(),
            totalAnswers,
            null,
            choiceStatistics,
            List.of(),
            List.of()
        );
    }

    /**
     * Construye estadísticas para preguntas de selección simple o múltiple.
     *
     * Los recuentos de opciones se obtienen mediante JSONB directamente
     * en PostgreSQL.
     */
    private QuestionStatisticsResponseDto buildSelectionQuestionStatistics(
        SurveyQuestion question
    ) {
        long totalAnswers =
            answerRepository.countSelectionAnswers(
                question.getId()
            );

        Map<String, Long> countsByValue =
            getSelectionCounts(
                question.getId()
            );

        List<ChoiceOptionStatisticsDto> choiceStatistics =
            getOptions(question)
                .stream()
                .map(option ->
                    buildOptionStatistics(
                        option,
                        countsByValue,
                        totalAnswers
                    )
                )
                .toList();

        return new QuestionStatisticsResponseDto(
            question.getId(),
            question.getText(),
            question.getType(),
            question.isRequired(),
            totalAnswers,
            null,
            choiceStatistics,
            List.of(),
            List.of()
        );
    }

    /**
     * Construye estadísticas para preguntas de matriz.
     *
     * Las medias y extremos de cada fila se calculan mediante JSONB
     * directamente en la base de datos.
     */
    private QuestionStatisticsResponseDto buildMatrixQuestionStatistics(
        SurveyQuestion question
    ) {
        long totalAnswers =
            answerRepository.countMatrixAnswers(
                question.getId()
            );

        Map<String, MatrixRowStatisticsProjection> statisticsByRow =
            getMatrixStatisticsByRow(
                question.getId()
            );

        List<MatrixRowStatisticsDto> matrixStatistics =
            getMatrixRows(question)
                .stream()
                .map(row ->
                    buildMatrixRowStatistics(
                        row,
                        statisticsByRow.get(
                            row.getKey()
                        )
                    )
                )
                .toList();

        return new QuestionStatisticsResponseDto(
            question.getId(),
            question.getText(),
            question.getType(),
            question.isRequired(),
            totalAnswers,
            null,
            List.of(),
            matrixStatistics,
            List.of()
        );
    }

    /**
     * Convierte una proyección SQL numérica en DTO.
     *
     * Cuando no existen respuestas se devuelven valores neutros
     * en lugar de intentar operar con agregados nulos.
     */
    private NumericStatisticsDto toNumericStatistics(
        NumericStatisticsProjection projection
    ) {
        if (
            projection == null
            || valueOrZero(
                projection.getAnswerCount()
            ) == 0
        ) {
            return new NumericStatisticsDto(
                0,
                null,
                null,
                null
            );
        }

        return new NumericStatisticsDto(
            valueOrZero(
                projection.getAnswerCount()
            ),
            scale(
                projection.getAverage()
            ),
            scale(
                projection.getMinimum()
            ),
            scale(
                projection.getMaximum()
            )
        );
    }

    /**
     * Construye la estadística de una opción concreta.
     */
    private ChoiceOptionStatisticsDto buildOptionStatistics(
        OptionConfig option,
        Map<String, Long> countsByValue,
        long totalAnswers
    ) {
        long count =
            countsByValue.getOrDefault(
                option.getStoredValue(),
                0L
            );

        return new ChoiceOptionStatisticsDto(
            option.getStoredValue(),
            option.getLabel(),
            count,
            percentage(
                count,
                totalAnswers
            )
        );
    }

    /**
     * Recupera los recuentos de selección y los organiza por storedValue.
     */
    private Map<String, Long> getSelectionCounts(
        Long questionId
    ) {
        List<ChoiceCountProjection> projections =
            answerRepository.countSelectedValues(
                questionId
            );

        if (projections == null || projections.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> countsByValue =
            new HashMap<>();

        for (ChoiceCountProjection projection : projections) {
            countsByValue.put(
                projection.getStoredValue(),
                valueOrZero(
                    projection.getAnswerCount()
                )
            );
        }

        return countsByValue;
    }

    /**
     * Recupera las estadísticas de matriz y las organiza por rowKey.
     */
    private Map<String, MatrixRowStatisticsProjection>
    getMatrixStatisticsByRow(
        Long questionId
    ) {
        List<MatrixRowStatisticsProjection> projections =
            answerRepository.calculateMatrixStatistics(
                questionId
            );

        if (projections == null || projections.isEmpty()) {
            return Map.of();
        }

        Map<String, MatrixRowStatisticsProjection> statisticsByRow =
            new HashMap<>();

        for (MatrixRowStatisticsProjection projection : projections) {
            statisticsByRow.put(
                projection.getRowKey(),
                projection
            );
        }

        return statisticsByRow;
    }

    /**
     * Construye las estadísticas de una fila de matriz.
     *
     * Si la fila todavía no tiene respuestas se devuelven recuento cero
     * y valores agregados nulos.
     */
    private MatrixRowStatisticsDto buildMatrixRowStatistics(
        MatrixRowConfig row,
        MatrixRowStatisticsProjection projection
    ) {
        if (
            projection == null
            || valueOrZero(
                projection.getAnswerCount()
            ) == 0
        ) {
            return new MatrixRowStatisticsDto(
                row.getKey(),
                row.getLabel(),
                0,
                null,
                null,
                null
            );
        }

        return new MatrixRowStatisticsDto(
            row.getKey(),
            row.getLabel(),
            valueOrZero(
                projection.getAnswerCount()
            ),
            scale(
                projection.getAverage()
            ),
            scale(
                projection.getMinimum()
            ),
            scale(
                projection.getMaximum()
            )
        );
    }

    /**
     * Devuelve las opciones configuradas de una pregunta.
     */
    private List<OptionConfig> getOptions(
        SurveyQuestion question
    ) {
        return question.getOptions() != null
            ? question.getOptions()
            : List.of();
    }

    /**
     * Devuelve las filas configuradas de una pregunta de matriz.
     */
    private List<MatrixRowConfig> getMatrixRows(
        SurveyQuestion question
    ) {
        return question.getMatrixRows() != null
            ? question.getMatrixRows()
            : List.of();
    }

    /**
     * Convierte posibles valores agregados nulos en cero.
     */
    private long valueOrZero(
        Long value
    ) {
        return value != null
            ? value
            : 0;
    }

    /**
     * Redondea los valores numéricos a dos decimales.
     */
    private BigDecimal scale(
        BigDecimal value
    ) {
        if (value == null) {
            return null;
        }

        return value.setScale(
            DECIMAL_SCALE,
            RoundingMode.HALF_UP
        );
    }

    /**
     * Calcula un porcentaje con dos decimales.
     *
     * Si no existen respuestas se devuelve cero en lugar de dividir
     * entre cero.
     */
    private BigDecimal percentage(
        long count,
        long total
    ) {
        if (total == 0) {
            return BigDecimal.ZERO
                .setScale(DECIMAL_SCALE);
        }

        return BigDecimal.valueOf(count)
            .multiply(
                BigDecimal.valueOf(100)
            )
            .divide(
                BigDecimal.valueOf(total),
                DECIMAL_SCALE,
                RoundingMode.HALF_UP
            );
    }
}
