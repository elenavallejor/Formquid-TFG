package com.formquid.statistics.service;

import com.formquid.block.entity.Block;
import com.formquid.block.repository.BlockRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.content.dto.BlockContentItemRowDto;
import com.formquid.content.dto.ContentItemRowDto;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.question.entity.EvaluationDirection;
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
import com.formquid.statistics.dto.ValueDistributionDto;
import com.formquid.statistics.repository.StatisticsQueryRepository;
import com.formquid.statistics.repository.StatisticsQueryRepository.ChoiceOptionAggregate;
import com.formquid.statistics.repository.StatisticsQueryRepository.MatrixRowAggregate;
import com.formquid.statistics.repository.StatisticsQueryRepository.MatrixValueCount;
import com.formquid.statistics.repository.StatisticsQueryRepository.ParticipationSummary;
import com.formquid.statistics.repository.StatisticsQueryRepository.QuestionAggregate;
import com.formquid.statistics.repository.StatisticsQueryRepository.QuestionScore;
import com.formquid.statistics.repository.StatisticsQueryRepository.ValueCount;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final SurveyRepository surveyRepository;
    private final ContentItemRepository contentItemRepository;
    private final QuestionRepository questionRepository;
    private final BlockRepository blockRepository;
    private final StatisticsQueryRepository statisticsQueryRepository;
    private final SurveyService surveyService;

    @Transactional(readOnly = true)
    public SurveyStatisticsResponseDto getSurveyStatistics(Long surveyId, User user) {
        Survey survey = getSurveyWithResultsAccess(surveyId, user);
        ParticipationSummary participationSummary = statisticsQueryRepository.summarizeParticipations(surveyId);
        List<Question> questions = loadSurveyQuestions(surveyId);
        StatisticsData data = loadStatisticsData(questions);
        List<QuestionStatisticsResponseDto> questionStats = questions.stream()
            .map(question -> getQuestionStatistics(question, participationSummary.participationCount(), data))
            .toList();

        return new SurveyStatisticsResponseDto(
            survey.getId(),
            participationSummary.participationCount(),
            participationSummary.firstParticipation(),
            participationSummary.lastParticipation(),
            globalEvaluationScore(questionStats, questions),
            questionStats
        );
    }

    private Survey getSurveyWithResultsAccess(Long surveyId, User user) {
        Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
        surveyService.ensureCanViewResults(survey, user);
        return survey;
    }

    private StatisticsData loadStatisticsData(List<Question> questions) {
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        List<Long> scaleQuestionIds = questionIdsByType(questions, QuestionType.SCALE);
        List<Long> choiceQuestionIds = questionIdsByType(
            questions, QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE
        );
        List<Long> matrixQuestionIds = questionIdsByType(questions, QuestionType.MATRIX_SCALE);

        Map<Long, QuestionAggregate> questionAggregates = statisticsQueryRepository
            .summarizeQuestions(questionIds).stream()
            .collect(Collectors.toMap(QuestionAggregate::questionId, Function.identity()));
        Map<Long, List<ValueCount>> scaleValueCounts = queryScaleValues(scaleQuestionIds).stream()
            .collect(Collectors.groupingBy(ValueCount::questionId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<ChoiceOptionAggregate>> choiceOptions = queryChoiceOptions(choiceQuestionIds).stream()
            .collect(Collectors.groupingBy(
                ChoiceOptionAggregate::questionId, LinkedHashMap::new, Collectors.toList()
            ));
        Map<Long, Double> choiceScores = queryChoiceScores(choiceQuestionIds).stream()
            .collect(Collectors.toMap(QuestionScore::questionId, QuestionScore::normalizedScore));
        Map<Long, List<MatrixRowAggregate>> matrixRows = queryMatrixRows(matrixQuestionIds).stream()
            .collect(Collectors.groupingBy(
                MatrixRowAggregate::questionId, LinkedHashMap::new, Collectors.toList()
            ));
        Map<Long, List<MatrixValueCount>> matrixValueCounts = queryMatrixValues(matrixQuestionIds).stream()
            .collect(Collectors.groupingBy(
                MatrixValueCount::matrixRowId, LinkedHashMap::new, Collectors.toList()
            ));
        Map<Long, Double> matrixScores = queryMatrixScores(matrixQuestionIds).stream()
            .collect(Collectors.toMap(QuestionScore::questionId, QuestionScore::normalizedScore));

        return new StatisticsData(
            questionAggregates,
            scaleValueCounts,
            choiceOptions,
            choiceScores,
            matrixRows,
            matrixValueCounts,
            matrixScores
        );
    }

    private List<Long> questionIdsByType(List<Question> questions, QuestionType... types) {
        List<QuestionType> acceptedTypes = List.of(types);
        return questions.stream()
            .filter(question -> acceptedTypes.contains(question.getQuestionType()))
            .map(Question::getId)
            .toList();
    }

    private List<ValueCount> queryScaleValues(List<Long> questionIds) {
        return questionIds.isEmpty() ? List.of() : statisticsQueryRepository.findScaleValueCounts(questionIds);
    }

    private List<ChoiceOptionAggregate> queryChoiceOptions(List<Long> questionIds) {
        return questionIds.isEmpty() ? List.of() : statisticsQueryRepository.summarizeChoiceOptions(questionIds);
    }

    private List<QuestionScore> queryChoiceScores(List<Long> questionIds) {
        return questionIds.isEmpty() ? List.of() : statisticsQueryRepository.summarizeChoiceScores(questionIds);
    }

    private List<MatrixRowAggregate> queryMatrixRows(List<Long> questionIds) {
        return questionIds.isEmpty() ? List.of() : statisticsQueryRepository.summarizeMatrixRows(questionIds);
    }

    private List<MatrixValueCount> queryMatrixValues(List<Long> questionIds) {
        return questionIds.isEmpty() ? List.of() : statisticsQueryRepository.findMatrixValueCounts(questionIds);
    }

    private List<QuestionScore> queryMatrixScores(List<Long> questionIds) {
        return questionIds.isEmpty() ? List.of() : statisticsQueryRepository.summarizeMatrixScores(questionIds);
    }

    private QuestionStatisticsResponseDto getQuestionStatistics(
        Question question,
        long participationCount,
        StatisticsData data
    ) {
        QuestionAggregate aggregate = data.questionAggregates().getOrDefault(
            question.getId(), QuestionAggregate.empty(question.getId())
        );
        long answerCount = aggregate.answerCount();
        long omissions = Math.max(0, participationCount - answerCount);
        double responseRate = participationCount == 0 ? 0 : (answerCount * 100.0) / participationCount;
        Double normalizedScore = normalizedScore(question, aggregate, data);
        Double evaluationScore = normalizedScore == null ? null : directionalScore(question, normalizedScore);

        return new QuestionStatisticsResponseDto(
            question.getId(),
            question.getStatement(),
            question.getQuestionType(),
            answerCount,
            omissions,
            responseRate,
            numericStatistics(aggregate),
            scaleValueDistribution(question, data.scaleValueCounts().getOrDefault(question.getId(), List.of())),
            choiceStatistics(question.getId(), answerCount, data),
            booleanStatistics(aggregate),
            matrixStatistics(question, data),
            normalizedScore,
            evaluationScore
        );
    }

    private NumericStatisticsDto numericStatistics(QuestionAggregate aggregate) {
        return new NumericStatisticsDto(
            aggregate.numericAverage(),
            aggregate.numericMinimum(),
            aggregate.numericMaximum(),
            aggregate.numericMedian(),
            aggregate.numericStandardDeviation()
        );
    }

    private NumericStatisticsDto numericStatistics(MatrixRowAggregate aggregate) {
        return new NumericStatisticsDto(
            aggregate.numericAverage(),
            aggregate.numericMinimum(),
            aggregate.numericMaximum(),
            aggregate.numericMedian(),
            aggregate.numericStandardDeviation()
        );
    }

    private List<ValueDistributionDto> scaleValueDistribution(
        Question question,
        List<ValueCount> valueCounts
    ) {
        if (question.getQuestionType() != QuestionType.SCALE) {
            return List.of();
        }
        List<CountedValue> values = valueCounts.stream()
            .map(value -> new CountedValue(value.value(), value.count()))
            .toList();
        return valueDistribution(question, values);
    }

    private List<ValueDistributionDto> valueDistribution(Question question, List<CountedValue> valueCounts) {
        Map<BigDecimal, Long> counts = new TreeMap<>();
        valueCounts.forEach(valueCount -> counts.put(valueCount.value().stripTrailingZeros(), valueCount.count()));
        long total = valueCounts.stream().mapToLong(CountedValue::count).sum();
        addConfiguredScaleValues(question, counts);
        if (counts.isEmpty()) {
            return List.of();
        }
        return counts.entrySet().stream()
            .map(entry -> new ValueDistributionDto(
                entry.getKey(),
                entry.getValue(),
                total == 0 ? 0 : (entry.getValue() * 100.0) / total
            ))
            .toList();
    }

    private void addConfiguredScaleValues(Question question, Map<BigDecimal, Long> counts) {
        QuestionSettings settings = question.getSettings();
        if (settings == null || settings.getMinValue() == null || settings.getMaxValue() == null) {
            return;
        }
        BigDecimal step = settings.getStep() == null ? BigDecimal.ONE : settings.getStep();
        if (step.signum() <= 0 || settings.getMinValue().compareTo(settings.getMaxValue()) > 0) {
            return;
        }
        BigDecimal configuredValueCount = settings.getMaxValue()
            .subtract(settings.getMinValue())
            .divide(step, 0, RoundingMode.FLOOR)
            .add(BigDecimal.ONE);
        if (configuredValueCount.compareTo(new BigDecimal("100")) > 0) {
            return;
        }
        BigDecimal current = settings.getMinValue();
        while (current.compareTo(settings.getMaxValue()) <= 0) {
            counts.putIfAbsent(current.stripTrailingZeros(), 0L);
            current = current.add(step);
        }
    }

    private List<ChoiceOptionStatisticsDto> choiceStatistics(
        Long questionId,
        long answerCount,
        StatisticsData data
    ) {
        return data.choiceOptions().getOrDefault(questionId, List.of()).stream()
            .map(option -> new ChoiceOptionStatisticsDto(
                option.optionId(),
                option.label(),
                option.count(),
                answerCount == 0 ? 0 : (option.count() * 100.0) / answerCount
            ))
            .toList();
    }

    private BooleanStatisticsDto booleanStatistics(QuestionAggregate aggregate) {
        long total = aggregate.trueCount() + aggregate.falseCount();
        if (total == 0) {
            return null;
        }
        return new BooleanStatisticsDto(
            aggregate.trueCount(),
            aggregate.falseCount(),
            (aggregate.trueCount() * 100.0) / total,
            (aggregate.falseCount() * 100.0) / total
        );
    }

    private List<MatrixRowStatisticsDto> matrixStatistics(Question question, StatisticsData data) {
        if (question.getQuestionType() != QuestionType.MATRIX_SCALE) {
            return List.of();
        }
        return data.matrixRows().getOrDefault(question.getId(), List.of()).stream()
            .map(row -> new MatrixRowStatisticsDto(
                row.matrixRowId(),
                row.label(),
                row.answerCount(),
                numericStatistics(row),
                matrixValueDistribution(question, row.matrixRowId(), data),
                row.normalizedAverage()
            ))
            .toList();
    }

    private List<ValueDistributionDto> matrixValueDistribution(
        Question question,
        Long matrixRowId,
        StatisticsData data
    ) {
        List<CountedValue> values = data.matrixValueCounts().getOrDefault(matrixRowId, List.of()).stream()
            .map(value -> new CountedValue(value.value(), value.count()))
            .toList();
        return valueDistribution(question, values);
    }

    private Double normalizedScore(
        Question question,
        QuestionAggregate aggregate,
        StatisticsData data
    ) {
        return switch (question.getQuestionType()) {
            case NUMBER, SCALE, BOOLEAN -> aggregate.scalarNormalizedScore();
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> data.choiceScores().get(question.getId());
            case MATRIX_SCALE -> data.matrixScores().get(question.getId());
            case TEXT -> null;
        };
    }

    private Double directionalScore(Question question, Double normalizedScore) {
        if (question.getEvaluationDirection() == EvaluationDirection.NEUTRAL) {
            return null;
        }
        double centered = (normalizedScore * 2) - 100;
        return question.getEvaluationDirection() == EvaluationDirection.NEGATIVE ? -centered : centered;
    }

    private Double globalEvaluationScore(List<QuestionStatisticsResponseDto> stats, List<Question> questions) {
        Map<Long, Question> questionsById = questions.stream()
            .collect(Collectors.toMap(Question::getId, Function.identity()));
        double weighted = 0;
        double totalWeight = 0;
        for (QuestionStatisticsResponseDto stat : stats) {
            Question question = questionsById.get(stat.questionId());
            if (question == null || stat.evaluationScore() == null || question.getEvaluationWeight() == null) {
                continue;
            }
            double weight = question.getEvaluationWeight().abs().doubleValue();
            weighted += stat.evaluationScore() * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? null : weighted / totalWeight;
    }

    private List<Question> loadSurveyQuestions(Long surveyId) {
        List<ContentItemRowDto> surveyContent = contentItemRepository.findSurveyContentRows(surveyId);
        List<Long> blockParentItemIds = surveyContent.stream()
            .filter(item -> item.itemType() == ContentItemType.BLOCK)
            .map(ContentItemRowDto::id)
            .toList();
        List<Block> blocks = blockParentItemIds.isEmpty()
            ? List.of()
            : blockRepository.findByParentItemIdsWithDetails(blockParentItemIds);
        Map<Long, Long> blockIdsByParentItem = blocks.stream()
            .collect(Collectors.toMap(block -> block.getParentItem().getId(), Block::getId));
        List<Long> blockIds = blocks.stream().map(Block::getId).toList();
        List<BlockContentItemRowDto> blockContent = blockIds.isEmpty()
            ? List.of()
            : contentItemRepository.findBlockContentRows(blockIds);

        List<Long> questionParentItemIds = new ArrayList<>();
        surveyContent.stream()
            .filter(item -> item.itemType() == ContentItemType.QUESTION)
            .map(ContentItemRowDto::id)
            .forEach(questionParentItemIds::add);
        blockContent.stream()
            .filter(item -> item.itemType() == ContentItemType.QUESTION)
            .map(BlockContentItemRowDto::id)
            .forEach(questionParentItemIds::add);
        if (questionParentItemIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Question> questionsByParentItem = questionRepository
            .findByParentItemIdsForStatistics(questionParentItemIds).stream()
            .collect(Collectors.toMap(question -> question.getParentItem().getId(), Function.identity()));
        Map<Long, List<BlockContentItemRowDto>> contentByBlock = blockContent.stream()
            .collect(Collectors.groupingBy(
                BlockContentItemRowDto::parentBlockId, LinkedHashMap::new, Collectors.toList()
            ));

        List<Question> orderedQuestions = new ArrayList<>();
        for (ContentItemRowDto item : surveyContent) {
            if (item.itemType() == ContentItemType.QUESTION) {
                addQuestion(item.id(), questionsByParentItem, orderedQuestions);
                continue;
            }
            if (item.itemType() != ContentItemType.BLOCK) {
                continue;
            }
            Long blockId = blockIdsByParentItem.get(item.id());
            contentByBlock.getOrDefault(blockId, List.of()).stream()
                .filter(blockItem -> blockItem.itemType() == ContentItemType.QUESTION)
                .forEach(blockItem -> addQuestion(blockItem.id(), questionsByParentItem, orderedQuestions));
        }
        return orderedQuestions;
    }

    private void addQuestion(
        Long parentItemId,
        Map<Long, Question> questionsByParentItem,
        List<Question> target
    ) {
        Question question = questionsByParentItem.get(parentItemId);
        if (question != null) {
            target.add(question);
        }
    }

    private record CountedValue(BigDecimal value, long count) {}

    private record StatisticsData(
        Map<Long, QuestionAggregate> questionAggregates,
        Map<Long, List<ValueCount>> scaleValueCounts,
        Map<Long, List<ChoiceOptionAggregate>> choiceOptions,
        Map<Long, Double> choiceScores,
        Map<Long, List<MatrixRowAggregate>> matrixRows,
        Map<Long, List<MatrixValueCount>> matrixValueCounts,
        Map<Long, Double> matrixScores
    ) {}
}
