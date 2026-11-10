package com.formquid.survey.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SurveyCoverRepository {
    private final JdbcTemplate jdbcTemplate;

    public SurveyCoverRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Long surveyId, String filename, String contentType, byte[] bytes) {
        jdbcTemplate.update(
            "INSERT INTO survey_cover_files (filename, survey_id, content_type, image_bytes) VALUES (?, ?, ?, ?)",
            filename, surveyId, contentType, bytes
        );
    }

    public Optional<byte[]> read(String filename) {
        return jdbcTemplate.query(
            "SELECT image_bytes FROM survey_cover_files WHERE filename = ?",
            result -> result.next() ? Optional.of(result.getBytes(1)) : Optional.empty(),
            filename
        );
    }

    public void delete(String filename) {
        jdbcTemplate.update("DELETE FROM survey_cover_files WHERE filename = ?", filename);
    }
}
