package com.formquid.statistics.service;

import com.formquid.block.repository.BlockRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.participation.entity.Answer;
import com.formquid.participation.entity.AnswerOption;
import com.formquid.participation.entity.Participation;
import com.formquid.participation.repository.AnswerOptionRepository;
import com.formquid.participation.repository.AnswerRepository;
import com.formquid.participation.repository.MatrixAnswerValueRepository;
import com.formquid.participation.repository.ParticipationRepository;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.MatrixRow;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionSettings;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.statistics.dto.BooleanStatisticsDto;
import com.formquid.statistics.dto.ChoiceOptionStatisticsDto;
import com.formquid.statistics.dto.MatrixRowStatisticsDto;
import com.formquid.statistics.dto.NumericStatisticsDto;
import com.formquid.statistics.dto.QuestionStatisticsResponseDto;
import com.formquid.statistics.dto.SurveyStatisticsResponseDto;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final SurveyRepository surveyRepository;
    private final ParticipationRepository participationRepository;
    private final AnswerRepository answerRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final MatrixAnswerValueRepository matrixAnswerValueRepository;
    private final ContentItemRepository contentItemRepository;
    private final QuestionRepository questionRepository;
    private final BlockRepository blockRepository;
    private final SurveyService surveyService;

    @Transactional(readOnly = true)
    public SurveyStatisticsResponseDto getSurveyStatistics(Long surveyId, User user) {
        Survey survey = getSurveyWithResultsAccess(surveyId, user);
        List<Participation> participations = participationRepository.findBySurveyIdOrderBySubmittedAtAsc(surveyId);
        long participationCount = participations.size();
        Instant first = participations.isEmpty() ? null : participations.getFirst().getSubmittedAt();
        Instant last = participations.isEmpty() ? null : participations.getLast().getSubmittedAt();
        List<Question> questions = loadSurveyQuestions(surveyId);
        List<QuestionStatisticsResponseDto> questionStats = questions.stream()
            .map(question -> getQuestionStatistics(question, participationCount))
            .toList();
        return new SurveyStatisticsResponseDto(
            survey.getId(),
            participationCount,
            first,
            last,
            globalTechnologyScore(questionStats, questions),
            questionStats
        );
    }

    @Transactional(readOnly = true)
    public QuestionStatisticsResponseDto getQuestionStatistics(Long surveyId, Long questionId, User user) {
        getSurveyWithResultsAccess(surveyId, user);
        if (loadSurveyQuestions(surveyId).stream().noneMatch(question -> question.getId().equals(questionId))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La pregunta no pertenece a la encuesta");
        }
        long participationCount = participationRepository.countBySurveyId(surveyId);
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pregunta no encontrada"));
        return getQuestionStatistics(question, participationCount);
    }

    private Survey getSurveyWithResultsAccess(Long surveyId, User user) {
        Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
        surveyService.ensureCanViewResults(survey, user);
        return survey;
    }

    private QuestionStatisticsResponseDto getQuestionStatistics(Question question, long participationCount) {
        List<Answer> answers = answerRepository.findByQuestionId(question.getId());
        long answerCount = answers.size();
        long omissions = Math.max(0, participationCount - answerCount);
        double responseRate = participationCount == 0 ? 0 : (answerCount * 100.0) / participationCount;
        NumericStatisticsDto numeric = numericStatistics(answers.stream()
            .map(Answer::getNumericValue)
            .filter(value -> value != null)
            .toList());
        Double normalizedScore = average(answers.stream()
            .map(answer -> rawScore(question, answer))
            .filter(value -> value != null)
            .toList());
        Double evaluationScore = normalizedScore == null ? null : directionalScore(question, normalizedScore);

        return new QuestionStatisticsResponseDto(
            question.getId(),
            question.getStatement(),
            question.getQuestionType(),
            answerCount,
            omissions,
            responseRate,
            numeric,
            choiceStatistics(question.getId(), answerCount),
            booleanStatistics(answers),
            matrixStatistics(question),
            normalizedScore,
            evaluationScore
        );
    }

    private List<ChoiceOptionStatisticsDto> choiceStatistics(Long questionId, long answerCount) {
        return answerOptionRepository.countOptionsByQuestion(questionId).stream()
            .map(row -> {
                Long optionId = (Long) row[0];
                String label = (String) row[1];
                long count = ((Number) row[2]).longValue();
                double percentage = answerCount == 0 ? 0 : (count * 100.0) / answerCount;
                return new ChoiceOptionStatisticsDto(optionId, label, count, percentage);
            })
            .toList();
    }

    private BooleanStatisticsDto booleanStatistics(List<Answer> answers) {
        long trueCount = answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getBooleanValue())).count();
        long falseCount = answers.stream().filter(answer -> Boolean.FALSE.equals(answer.getBooleanValue())).count();
        long total = trueCount + falseCount;
        if (total == 0) {
            return null;
        }
        return new BooleanStatisticsDto(
            trueCount,
            falseCount,
            (trueCount * 100.0) / total,
            (falseCount * 100.0) / total
        );
    }

    private List<MatrixRowStatisticsDto> matrixStatistics(Question question) {
        if (question.getQuestionType() != QuestionType.MATRIX_SCALE) {
            return List.of();
        }
        Map<Long, List<BigDecimal>> valuesByRow = new LinkedHashMap<>();
        matrixAnswerValueRepository.findByQuestionId(question.getId()).forEach(value ->
            valuesByRow.computeIfAbsent(value.getMatrixRow().getId(), id -> new ArrayList<>()).add(value.getNumericValue())
        );
        return question.getMatrixRows().stream()
            .sorted(Comparator.comparing(MatrixRow::getPosition))
            .map(row -> {
                List<BigDecimal> values = valuesByRow.getOrDefault(row.getId(), List.of());
                return new MatrixRowStatisticsDto(
                    row.getId(),
                    row.getLabel(),
                    values.size(),
                    numericStatistics(values),
                    average(values.stream().map(value -> normalize(question, value)).filter(value -> value != null).toList())
                );
            })
            .toList();
    }

    private NumericStatisticsDto numericStatistics(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return new NumericStatisticsDto(null, null, null, null, null);
        }
        List<Double> sorted = values.stream().map(BigDecimal::doubleValue).sorted().toList();
        DoubleSummaryStatistics stats = sorted.stream().mapToDouble(Double::doubleValue).summaryStatistics();
        double average = stats.getAverage();
        double variance = sorted.stream()
            .mapToDouble(value -> Math.pow(value - average, 2))
            .average()
            .orElse(0);
        return new NumericStatisticsDto(
            average,
            values.stream().min(BigDecimal::compareTo).orElse(null),
            values.stream().max(BigDecimal::compareTo).orElse(null),
            median(sorted),
            Math.sqrt(variance)
        );
    }

    private Double median(List<Double> sorted) {
        if (sorted.isEmpty()) {
            return null;
        }
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private Double rawScore(Question question, Answer answer) {
        return switch (question.getQuestionType()) {
            case NUMBER, SCALE -> normalize(question, answer.getNumericValue());
            case BOOLEAN -> booleanScore(question, answer.getBooleanValue());
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> choiceScore(answer);
            case MATRIX_SCALE -> average(answer.getMatrixValues().stream()
                .map(value -> normalize(question, value.getNumericValue()))
                .filter(value -> value != null)
                .toList());
            case TEXT -> null;
        };
    }

    private Double normalize(Question question, BigDecimal value) {
        if (value == null || question.getSettings() == null) {
            return null;
        }
        QuestionSettings settings = question.getSettings();
        if (settings.getMinValue() == null || settings.getMaxValue() == null
            || settings.getMinValue().compareTo(settings.getMaxValue()) >= 0) {
            return null;
        }
        BigDecimal range = settings.getMaxValue().subtract(settings.getMinValue());
        BigDecimal normalized = value.subtract(settings.getMinValue())
            .divide(range, 6, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        return Math.max(0, Math.min(100, normalized.doubleValue()));
    }

    private Double booleanScore(Question question, Boolean value) {
        if (value == null || question.getSettings() == null) {
            return null;
        }
        BigDecimal factor = value
            ? question.getSettings().getTrueEvaluationFactor()
            : question.getSettings().getFalseEvaluationFactor();
        return factor == null ? null : factor.multiply(new BigDecimal("100")).doubleValue();
    }

    private Double choiceScore(Answer answer) {
        return average(answer.getSelectedOptions().stream()
            .map(AnswerOption::getOption)
            .map(option -> option.getEvaluationFactor() == null ? null : option.getEvaluationFactor().multiply(new BigDecimal("100")).doubleValue())
            .filter(value -> value != null)
            .toList());
    }

    private Double directionalScore(Question question, Double normalizedScore) {
        if (question.getEvaluationDirection() == EvaluationDirection.NEUTRAL) {
            return null;
        }
        double centered = (normalizedScore * 2) - 100;
        return question.getEvaluationDirection() == EvaluationDirection.NEGATIVE ? -centered : centered;
    }

    private Double globalTechnologyScore(List<QuestionStatisticsResponseDto> stats, List<Question> questions) {
        Map<Long, Question> questionsById = questions.stream()
            .collect(java.util.stream.Collectors.toMap(Question::getId, question -> question));
        double weighted = 0;
        double totalWeight = 0;
        for (QuestionStatisticsResponseDto stat : stats) {
            Question question = questionsById.get(stat.questionId());
            if (question == null || stat.evaluationScore() == null || question.getEvaluationWeight() == null) {
                continue;
            }
            double weight = question.getEvaluationWeight().doubleValue();
            weighted += stat.evaluationScore() * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? null : weighted / totalWeight;
    }

    private Double average(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private List<Question> loadSurveyQuestions(Long surveyId) {
        List<Question> questions = new ArrayList<>();
        for (ContentItem item : contentItemRepository.findBySurveyIdOrderByPositionAsc(surveyId)) {
            if (item.getItemType() == ContentItemType.QUESTION) {
                questionRepository.findByParentItemId(item.getId()).ifPresent(questions::add);
            } else if (item.getItemType() == ContentItemType.BLOCK) {
                blockRepository.findByParentItemId(item.getId()).ifPresent(block ->
                    contentItemRepository.findByParentBlockIdOrderByPositionAsc(block.getId()).stream()
                        .sorted(Comparator.comparing(ContentItem::getPosition))
                        .forEach(blockItem -> questionRepository.findByParentItemId(blockItem.getId()).ifPresent(questions::add))
                );
            }
        }
        return questions;
    }
}