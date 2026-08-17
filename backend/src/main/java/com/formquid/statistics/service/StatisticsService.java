package com.formquid.statistics.service;

import com.formquid.participation.repository.AnswerRepository;
import com.formquid.participation.repository.ParticipationRepository;
import com.formquid.question.entity.MatrixRowConfig;
import com.formquid.question.entity.OptionConfig;
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
import com.formquid.survey.model.SurveyQuestionSnapshot;
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
 * Las preguntas se leen desde el snapshot JSONB de la encuesta y las
 * agregaciones de respuestas se delegan en PostgreSQL por questionKey.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private static final int MAX_TEXT_ANSWERS = 20;
    private static final int DECIMAL_SCALE = 2;

    private final SurveyRepository surveyRepository;
    private final ParticipationRepository participationRepository;
    private final AnswerRepository answerRepository;
    private final SurveyService surveyService;
    private final UserRepository userRepository;

    /**
     * Obtiene estadísticas globales de una encuesta.
     */
    public SurveyStatisticsResponseDto getSurveyStatistics(
        Long surveyId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        surveyService.assertCanViewResults(
            survey,
            user.getId(),
            user.getRole()
        );

        long totalParticipations =
            participationRepository.countBySurveyId(surveyId);

        List<QuestionStatisticsResponseDto> questionStatistics =
            getSurveyQuestions(survey)
                .stream()
                .map(question -> buildQuestionStatistics(survey.getId(), question))
                .toList();

        return new SurveyStatisticsResponseDto(
            survey.getId(),
            survey.getTitle(),
            totalParticipations,
            questionStatistics
        );
    }

    /**
     * Obtiene estadísticas de una pregunta concreta.
     */
    public QuestionStatisticsResponseDto getQuestionStatistics(
        Long surveyId,
        String questionKey,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        surveyService.assertCanViewResults(
            survey,
            user.getId(),
            user.getRole()
        );

        return buildQuestionStatistics(
            survey.getId(),
            getQuestion(survey, questionKey)
        );
    }

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

    private Survey getSurvey(Long surveyId) {
        return surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la encuesta indicada"
            ));
    }

    private List<SurveyQuestionSnapshot> getSurveyQuestions(Survey survey) {
        if (survey.getStructure() == null) {
            return List.of();
        }

        return survey.getStructure().getQuestions();
    }

    private SurveyQuestionSnapshot getQuestion(
        Survey survey,
        String questionKey
    ) {
        if (survey.getStructure() == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la pregunta indicada en esta encuesta"
            );
        }

        return survey.getStructure()
            .findQuestion(questionKey)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la pregunta indicada en esta encuesta"
            ));
    }

    private QuestionStatisticsResponseDto buildQuestionStatistics(
        Long surveyId,
        SurveyQuestionSnapshot question
    ) {
        return switch (question.getType()) {
            case SHORT_TEXT, LONG_TEXT -> buildTextQuestionStatistics(surveyId, question);
            case NUMBER, SCALE, BIPOLAR_SCALE -> buildNumericQuestionStatistics(surveyId, question);
            case BOOLEAN -> buildBooleanQuestionStatistics(surveyId, question);
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> buildSelectionQuestionStatistics(surveyId, question);
            case MATRIX_SCALE -> buildMatrixQuestionStatistics(surveyId, question);
        };
    }

    private QuestionStatisticsResponseDto buildTextQuestionStatistics(
        Long surveyId,
        SurveyQuestionSnapshot question
    ) {
        long totalAnswers =
            answerRepository.countTextAnswers(surveyId, question.getQuestionKey());

        List<String> textAnswers =
            answerRepository.findTextAnswerSample(
                surveyId,
                question.getQuestionKey(),
                MAX_TEXT_ANSWERS
            );

        return baseResponse(
            question,
            totalAnswers,
            null,
            List.of(),
            List.of(),
            textAnswers != null ? textAnswers : List.of()
        );
    }

    private QuestionStatisticsResponseDto buildNumericQuestionStatistics(
        Long surveyId,
        SurveyQuestionSnapshot question
    ) {
        NumericStatisticsDto statistics =
            toNumericStatistics(
                answerRepository.calculateNumericStatistics(
                    surveyId,
                    question.getQuestionKey()
                )
            );

        return baseResponse(
            question,
            statistics.count(),
            statistics,
            List.of(),
            List.of(),
            List.of()
        );
    }

    private QuestionStatisticsResponseDto buildBooleanQuestionStatistics(
        Long surveyId,
        SurveyQuestionSnapshot question
    ) {
        BooleanStatisticsProjection projection =
            answerRepository.calculateBooleanStatistics(
                surveyId,
                question.getQuestionKey()
            );

        long yesCount = projection != null ? valueOrZero(projection.getYesCount()) : 0;
        long noCount = projection != null ? valueOrZero(projection.getNoCount()) : 0;
        long totalAnswers = yesCount + noCount;

        List<ChoiceOptionStatisticsDto> choiceStatistics = List.of(
            new ChoiceOptionStatisticsDto(
                "true",
                "Sí",
                yesCount,
                percentage(yesCount, totalAnswers)
            ),
            new ChoiceOptionStatisticsDto(
                "false",
                "No",
                noCount,
                percentage(noCount, totalAnswers)
            )
        );

        return baseResponse(
            question,
            totalAnswers,
            null,
            choiceStatistics,
            List.of(),
            List.of()
        );
    }

    private QuestionStatisticsResponseDto buildSelectionQuestionStatistics(
        Long surveyId,
        SurveyQuestionSnapshot question
    ) {
        long totalAnswers =
            answerRepository.countSelectionAnswers(surveyId, question.getQuestionKey());

        Map<String, Long> countsByValue =
            getSelectionCounts(surveyId, question.getQuestionKey());

        List<ChoiceOptionStatisticsDto> choiceStatistics =
            getOptions(question)
                .stream()
                .map(option -> buildOptionStatistics(
                    option,
                    countsByValue,
                    totalAnswers
                ))
                .toList();

        return baseResponse(
            question,
            totalAnswers,
            null,
            choiceStatistics,
            List.of(),
            List.of()
        );
    }

    private QuestionStatisticsResponseDto buildMatrixQuestionStatistics(
        Long surveyId,
        SurveyQuestionSnapshot question
    ) {
        long totalAnswers =
            answerRepository.countMatrixAnswers(surveyId, question.getQuestionKey());

        Map<String, MatrixRowStatisticsProjection> statisticsByRow =
            getMatrixStatisticsByRow(surveyId, question.getQuestionKey());

        List<MatrixRowStatisticsDto> matrixStatistics =
            getMatrixRows(question)
                .stream()
                .map(row -> buildMatrixRowStatistics(
                    row,
                    statisticsByRow.get(row.getKey())
                ))
                .toList();

        return baseResponse(
            question,
            totalAnswers,
            null,
            List.of(),
            matrixStatistics,
            List.of()
        );
    }

    private QuestionStatisticsResponseDto baseResponse(
        SurveyQuestionSnapshot question,
        long totalAnswers,
        NumericStatisticsDto numericStatistics,
        List<ChoiceOptionStatisticsDto> choiceStatistics,
        List<MatrixRowStatisticsDto> matrixStatistics,
        List<String> textAnswers
    ) {
        return new QuestionStatisticsResponseDto(
            question.getQuestionKey(),
            question.getText(),
            question.getType(),
            question.isRequired(),
            totalAnswers,
            numericStatistics,
            choiceStatistics,
            matrixStatistics,
            textAnswers
        );
    }

    private NumericStatisticsDto toNumericStatistics(
        NumericStatisticsProjection projection
    ) {
        if (projection == null || valueOrZero(projection.getAnswerCount()) == 0) {
            return new NumericStatisticsDto(0, null, null, null);
        }

        return new NumericStatisticsDto(
            valueOrZero(projection.getAnswerCount()),
            scale(projection.getAverage()),
            scale(projection.getMinimum()),
            scale(projection.getMaximum())
        );
    }

    private ChoiceOptionStatisticsDto buildOptionStatistics(
        OptionConfig option,
        Map<String, Long> countsByValue,
        long totalAnswers
    ) {
        long count = countsByValue.getOrDefault(option.getStoredValue(), 0L);

        return new ChoiceOptionStatisticsDto(
            option.getStoredValue(),
            option.getLabel(),
            count,
            percentage(count, totalAnswers)
        );
    }

    private Map<String, Long> getSelectionCounts(
        Long surveyId,
        String questionKey
    ) {
        List<ChoiceCountProjection> projections =
            answerRepository.countSelectedValues(surveyId, questionKey);

        if (projections == null || projections.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> countsByValue = new HashMap<>();

        for (ChoiceCountProjection projection : projections) {
            countsByValue.put(
                projection.getStoredValue(),
                valueOrZero(projection.getAnswerCount())
            );
        }

        return countsByValue;
    }

    private Map<String, MatrixRowStatisticsProjection> getMatrixStatisticsByRow(
        Long surveyId,
        String questionKey
    ) {
        List<MatrixRowStatisticsProjection> projections =
            answerRepository.calculateMatrixStatistics(surveyId, questionKey);

        if (projections == null || projections.isEmpty()) {
            return Map.of();
        }

        Map<String, MatrixRowStatisticsProjection> statisticsByRow = new HashMap<>();

        for (MatrixRowStatisticsProjection projection : projections) {
            statisticsByRow.put(projection.getRowKey(), projection);
        }

        return statisticsByRow;
    }

    private MatrixRowStatisticsDto buildMatrixRowStatistics(
        MatrixRowConfig row,
        MatrixRowStatisticsProjection projection
    ) {
        if (projection == null || valueOrZero(projection.getAnswerCount()) == 0) {
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
            valueOrZero(projection.getAnswerCount()),
            scale(projection.getAverage()),
            scale(projection.getMinimum()),
            scale(projection.getMaximum())
        );
    }

    private List<OptionConfig> getOptions(SurveyQuestionSnapshot question) {
        return question.getOptions() != null ? question.getOptions() : List.of();
    }

    private List<MatrixRowConfig> getMatrixRows(SurveyQuestionSnapshot question) {
        return question.getMatrixRows() != null ? question.getMatrixRows() : List.of();
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0;
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return value.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(long count, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(DECIMAL_SCALE);
        }

        return BigDecimal.valueOf(count)
            .multiply(BigDecimal.valueOf(100))
            .divide(
                BigDecimal.valueOf(total),
                DECIMAL_SCALE,
                RoundingMode.HALF_UP
            );
    }
}
