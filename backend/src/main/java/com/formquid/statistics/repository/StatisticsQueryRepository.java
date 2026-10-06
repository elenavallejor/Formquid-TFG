package com.formquid.statistics.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Consultas agregadas de estadísticas. Devuelve únicamente filas escalares y evita
 * materializar respuestas y asociaciones JPA completas.
 */
@Repository
@RequiredArgsConstructor
public class StatisticsQueryRepository {

    private final JdbcClient jdbcClient;

    public ParticipationSummary summarizeParticipations(Long surveyId) {
        return jdbcClient.sql("""
                select count(*) as participation_count,
                       min(submitted_at) as first_participation,
                       max(submitted_at) as last_participation
                from participations
                where survey_id = :surveyId
                """)
            .param("surveyId", surveyId)
            .query((resultSet, rowNum) -> new ParticipationSummary(
                resultSet.getLong("participation_count"),
                nullableInstant(resultSet, "first_participation"),
                nullableInstant(resultSet, "last_participation")
            ))
            .single();
    }

    public List<QuestionAggregate> summarizeQuestions(Collection<Long> questionIds) {
        if (questionIds.isEmpty()) return List.of();
        return jdbcClient.sql("""
                select q.id as question_id,
                       count(a.id) as answer_count,
                       avg(a.numeric_value)::double precision as numeric_average,
                       min(a.numeric_value) as numeric_minimum,
                       max(a.numeric_value) as numeric_maximum,
                       percentile_cont(0.5) within group (
                           order by a.numeric_value::double precision
                       ) filter (where a.numeric_value is not null) as numeric_median,
                       stddev_pop(a.numeric_value)::double precision as numeric_standard_deviation,
                       count(a.id) filter (where a.boolean_value is true) as true_count,
                       count(a.id) filter (where a.boolean_value is false) as false_count,
                       case
                           when q.question_type in ('NUMBER', 'SCALE')
                                and qs.min_value is not null
                                and qs.max_value is not null
                                and qs.min_value < qs.max_value
                           then (
                               avg(greatest(0::numeric, least(100::numeric,
                                   round((a.numeric_value - qs.min_value)
                                       / nullif(qs.max_value - qs.min_value, 0), 6) * 100
                               ))) filter (where a.numeric_value is not null)
                           )::double precision
                           when q.question_type = 'BOOLEAN'
                           then avg(case
                               when a.boolean_value is true then qs.true_evaluation_factor * 100
                               when a.boolean_value is false then qs.false_evaluation_factor * 100
                           end)::double precision
                       end as scalar_normalized_score
                from questions q
                left join question_settings qs on qs.question_id = q.id
                left join answers a on a.question_id = q.id
                where q.id in (:questionIds)
                group by q.id, q.question_type, qs.min_value, qs.max_value,
                         qs.true_evaluation_factor, qs.false_evaluation_factor
                """)
            .param("questionIds", questionIds)
            .query(StatisticsQueryRepository::mapQuestionAggregate)
            .list();
    }

    public List<ValueCount> findScaleValueCounts(Collection<Long> questionIds) {
        if (questionIds.isEmpty()) return List.of();
        return jdbcClient.sql("""
                select question_id, numeric_value, count(*) as value_count
                from answers
                where question_id in (:questionIds) and numeric_value is not null
                group by question_id, numeric_value
                order by question_id, numeric_value
                """)
            .param("questionIds", questionIds)
            .query((rs, rowNum) -> new ValueCount(
                rs.getLong("question_id"), rs.getBigDecimal("numeric_value"), rs.getLong("value_count")
            ))
            .list();
    }

    public List<ChoiceOptionAggregate> summarizeChoiceOptions(Collection<Long> questionIds) {
        if (questionIds.isEmpty()) return List.of();
        return jdbcClient.sql("""
                select qo.question_id, qo.id as option_id, qo.label, qo.position,
                       count(ao.answer_id) as selection_count
                from question_options qo
                left join answer_options ao on ao.option_id = qo.id
                where qo.question_id in (:questionIds)
                group by qo.question_id, qo.id, qo.label, qo.position
                order by qo.question_id, qo.position
                """)
            .param("questionIds", questionIds)
            .query((rs, rowNum) -> new ChoiceOptionAggregate(
                rs.getLong("question_id"), rs.getLong("option_id"),
                rs.getString("label"), rs.getLong("selection_count")
            ))
            .list();
    }

    public List<QuestionScore> summarizeChoiceScores(Collection<Long> questionIds) {
        if (questionIds.isEmpty()) return List.of();
        return jdbcClient.sql("""
                select scores.question_id,
                       avg(scores.answer_score)::double precision as normalized_score
                from (
                    select a.question_id, a.id,
                           avg(qo.evaluation_factor * 100)::double precision as answer_score
                    from answers a
                    join answer_options ao on ao.answer_id = a.id
                    join question_options qo on qo.id = ao.option_id
                    where a.question_id in (:questionIds)
                    group by a.question_id, a.id
                ) scores
                where scores.answer_score is not null
                group by scores.question_id
                """)
            .param("questionIds", questionIds)
            .query((rs, rowNum) -> new QuestionScore(
                rs.getLong("question_id"), nullableDouble(rs, "normalized_score")
            ))
            .list();
    }

    public List<MatrixRowAggregate> summarizeMatrixRows(Collection<Long> questionIds) {
        if (questionIds.isEmpty()) return List.of();
        return jdbcClient.sql("""
                select mr.question_id, mr.id as matrix_row_id, mr.label, mr.position,
                       count(mav.answer_id) as answer_count,
                       avg(mav.numeric_value)::double precision as numeric_average,
                       min(mav.numeric_value) as numeric_minimum,
                       max(mav.numeric_value) as numeric_maximum,
                       percentile_cont(0.5) within group (
                           order by mav.numeric_value::double precision
                       ) filter (where mav.numeric_value is not null) as numeric_median,
                       stddev_pop(mav.numeric_value)::double precision as numeric_standard_deviation,
                       (
                           avg(greatest(0::numeric, least(100::numeric,
                               round((mav.numeric_value - qs.min_value)
                                   / nullif(qs.max_value - qs.min_value, 0), 6) * 100
                           ))) filter (
                               where mav.numeric_value is not null
                                 and qs.min_value is not null
                                 and qs.max_value is not null
                                 and qs.min_value < qs.max_value
                           )
                       )::double precision as normalized_average
                from matrix_rows mr
                left join question_settings qs on qs.question_id = mr.question_id
                left join matrix_answer_values mav on mav.matrix_row_id = mr.id
                where mr.question_id in (:questionIds)
                group by mr.question_id, mr.id, mr.label, mr.position,
                         qs.min_value, qs.max_value
                order by mr.question_id, mr.position
                """)
            .param("questionIds", questionIds)
            .query(StatisticsQueryRepository::mapMatrixRowAggregate)
            .list();
    }

    public List<MatrixValueCount> findMatrixValueCounts(Collection<Long> questionIds) {
        if (questionIds.isEmpty()) return List.of();
        return jdbcClient.sql("""
                select mr.question_id, mav.matrix_row_id, mav.numeric_value,
                       count(*) as value_count
                from matrix_answer_values mav
                join matrix_rows mr on mr.id = mav.matrix_row_id
                where mr.question_id in (:questionIds)
                group by mr.question_id, mav.matrix_row_id, mav.numeric_value
                order by mr.question_id, mav.matrix_row_id, mav.numeric_value
                """)
            .param("questionIds", questionIds)
            .query((rs, rowNum) -> new MatrixValueCount(
                rs.getLong("question_id"), rs.getLong("matrix_row_id"),
                rs.getBigDecimal("numeric_value"), rs.getLong("value_count")
            ))
            .list();
    }

    public List<QuestionScore> summarizeMatrixScores(Collection<Long> questionIds) {
        if (questionIds.isEmpty()) return List.of();
        return jdbcClient.sql("""
                select scores.question_id,
                       avg(scores.answer_score)::double precision as normalized_score
                from (
                    select a.question_id, a.id,
                           avg(greatest(0::numeric, least(100::numeric,
                               round((mav.numeric_value - qs.min_value)
                                   / nullif(qs.max_value - qs.min_value, 0), 6) * 100
                           )))::double precision as answer_score
                    from answers a
                    join matrix_answer_values mav on mav.answer_id = a.id
                    join question_settings qs on qs.question_id = a.question_id
                    where a.question_id in (:questionIds)
                      and qs.min_value is not null
                      and qs.max_value is not null
                      and qs.min_value < qs.max_value
                    group by a.question_id, a.id
                ) scores
                group by scores.question_id
                """)
            .param("questionIds", questionIds)
            .query((rs, rowNum) -> new QuestionScore(
                rs.getLong("question_id"), nullableDouble(rs, "normalized_score")
            ))
            .list();
    }

    private static QuestionAggregate mapQuestionAggregate(ResultSet rs, int rowNum) throws SQLException {
        return new QuestionAggregate(
            rs.getLong("question_id"), rs.getLong("answer_count"),
            nullableDouble(rs, "numeric_average"), rs.getBigDecimal("numeric_minimum"),
            rs.getBigDecimal("numeric_maximum"), nullableDouble(rs, "numeric_median"),
            nullableDouble(rs, "numeric_standard_deviation"), rs.getLong("true_count"),
            rs.getLong("false_count"), nullableDouble(rs, "scalar_normalized_score")
        );
    }

    private static MatrixRowAggregate mapMatrixRowAggregate(ResultSet rs, int rowNum) throws SQLException {
        return new MatrixRowAggregate(
            rs.getLong("question_id"), rs.getLong("matrix_row_id"), rs.getString("label"),
            rs.getInt("position"), rs.getLong("answer_count"),
            nullableDouble(rs, "numeric_average"), rs.getBigDecimal("numeric_minimum"),
            rs.getBigDecimal("numeric_maximum"), nullableDouble(rs, "numeric_median"),
            nullableDouble(rs, "numeric_standard_deviation"), nullableDouble(rs, "normalized_average")
        );
    }

    private static Double nullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    public record ParticipationSummary(long participationCount, Instant firstParticipation, Instant lastParticipation) {}

    public record QuestionAggregate(
        long questionId, long answerCount, Double numericAverage,
        BigDecimal numericMinimum, BigDecimal numericMaximum, Double numericMedian,
        Double numericStandardDeviation, long trueCount, long falseCount,
        Double scalarNormalizedScore
    ) {
        public static QuestionAggregate empty(long questionId) {
            return new QuestionAggregate(questionId, 0, null, null, null, null, null, 0, 0, null);
        }
    }

    public record ValueCount(long questionId, BigDecimal value, long count) {}
    public record ChoiceOptionAggregate(long questionId, long optionId, String label, long count) {}
    public record QuestionScore(long questionId, Double normalizedScore) {}
    public record MatrixRowAggregate(
        long questionId, long matrixRowId, String label, int position, long answerCount,
        Double numericAverage, BigDecimal numericMinimum, BigDecimal numericMaximum,
        Double numericMedian, Double numericStandardDeviation, Double normalizedAverage
    ) {}
    public record MatrixValueCount(long questionId, long matrixRowId, BigDecimal value, long count) {}
}
