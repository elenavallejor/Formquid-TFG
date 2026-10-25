package com.formquid;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvaluationWeightMigrationTest {

    @Test
    void convertsExistingNegativeQuestionsToSignedWeights() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            String jdbcUrl = postgres.getJdbcUrl();
            String username = postgres.getUsername();
            String password = postgres.getPassword();

            Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration", "classpath:db/testmigration")
                .target("36")
                .load()
                .migrate();

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                    INSERT INTO questions (scope, statement, question_type, evaluation_direction,
                                           evaluation_weight, category, created_at, updated_at)
                    VALUES ('CATALOG_FIXED', 'Pregunta negativa existente', 'BOOLEAN', 'NEGATIVE',
                            3.5, 'OTROS', now(), now())
                    """);
            }

            Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration", "classpath:db/testmigration")
                .load()
                .migrate();

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("""
                     SELECT evaluation_weight FROM questions WHERE statement = 'Pregunta negativa existente'
                     """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getBigDecimal(1)).isEqualByComparingTo("-3.5");
            }

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement()) {
                assertThatThrownBy(() -> statement.executeUpdate("""
                    UPDATE questions SET evaluation_weight = 3.5
                    WHERE statement = 'Pregunta negativa existente'
                    """))
                    .hasMessageContaining("ck_questions_values");
            }
        }
    }
}
