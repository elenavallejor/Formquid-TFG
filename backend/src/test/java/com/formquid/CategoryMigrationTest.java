package com.formquid;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryMigrationTest {

    @Test
    void upgradesExistingCatalogDataFromVersion27() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            String jdbcUrl = postgres.getJdbcUrl();
            String username = postgres.getUsername();
            String password = postgres.getPassword();

            Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration", "classpath:db/testmigration")
                .target("27")
                .load()
                .migrate();

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                    INSERT INTO templates (title, category, created_at, updated_at)
                    VALUES ('Plantilla existente', 'ARTIFICIAL_INTELLIGENCE', now(), now())
                    """);
                statement.executeUpdate("""
                    INSERT INTO surveys (owner_id, title, lifecycle_status, access_mode, visibility,
                                         category, created_at, updated_at)
                    SELECT id, 'Encuesta existente', 'DRAFT', 'PUBLIC', 'UNLISTED',
                           'LAPTOP', now(), now()
                    FROM users WHERE role = 'SUPERADMIN' LIMIT 1
                    """);
                statement.executeUpdate("""
                    INSERT INTO blocks (scope, name, category, created_at, updated_at)
                    VALUES ('CATALOG', 'Bloque existente', 'SMARTPHONE', now(), now())
                    """);
                statement.executeUpdate("""
                    INSERT INTO questions (scope, statement, question_type, evaluation_direction,
                                           category, created_at, updated_at)
                    VALUES ('CATALOG_FIXED', 'Pregunta existente', 'TEXT', 'NEUTRAL',
                            'PERSONAL_DATA', now(), now())
                    """);
            }

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement()) {
                connection.setAutoCommit(false);
                statement.executeUpdate("""
                    INSERT INTO content_items (survey_id, item_type, position)
                    SELECT id, 'BLOCK', 1 FROM surveys WHERE title = 'Encuesta existente'
                    """);
                statement.executeUpdate("""
                    INSERT INTO blocks (parent_item_id, scope, name, category, created_at, updated_at)
                    SELECT id, 'SNAPSHOT', 'Bloque respondido', 'SMARTPHONE', now(), now()
                    FROM content_items WHERE item_type = 'BLOCK'
                    """);
                statement.executeUpdate("""
                    INSERT INTO content_items (parent_block_id, item_type, position, is_required)
                    SELECT id, 'QUESTION', 1, true FROM blocks WHERE name = 'Bloque respondido'
                    """);
                statement.executeUpdate("""
                    INSERT INTO questions (parent_item_id, scope, statement, question_type,
                                           evaluation_direction, category, created_at, updated_at)
                    SELECT id, 'LOCAL', 'Pregunta respondida', 'TEXT', 'NEUTRAL',
                           'SMARTPHONE', now(), now()
                    FROM content_items WHERE item_type = 'QUESTION'
                    """);
                connection.commit();
                statement.executeUpdate("""
                    INSERT INTO participations (survey_id, submitted_at)
                    SELECT id, now() FROM surveys WHERE title = 'Encuesta existente'
                    """);
                statement.executeUpdate("""
                    INSERT INTO answers (participation_id, question_id, text_value)
                    SELECT participation.id, question.id, 'Respuesta existente'
                    FROM participations participation
                    CROSS JOIN questions question
                    WHERE question.statement = 'Pregunta respondida'
                    """);
                connection.commit();
            }

            Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration", "classpath:db/testmigration")
                .load()
                .migrate();

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("""
                     SELECT (SELECT category FROM surveys WHERE title = 'Encuesta existente'),
                            (SELECT category FROM templates WHERE title = 'Plantilla existente'),
                            (SELECT category FROM blocks WHERE name = 'Bloque existente'),
                            (SELECT category FROM questions WHERE statement = 'Pregunta existente'),
                            (SELECT category FROM blocks WHERE name = 'Bloque respondido'),
                            (SELECT category FROM questions WHERE statement = 'Pregunta respondida')
                     """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("PORTATIL");
                assertThat(rows.getString(2)).isEqualTo("INTELIGENCIA_ARTIFICIAL");
                assertThat(rows.getString(3)).isEqualTo("TELEFONO_INTELIGENTE");
                assertThat(rows.getString(4)).isEqualTo("DATOS_PERSONALES");
                assertThat(rows.getString(5)).isEqualTo("TELEFONO_INTELIGENTE");
                assertThat(rows.getString(6)).isEqualTo("TELEFONO_INTELIGENTE");
            }

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement()) {
                assertThat(statement.executeUpdate("""
                    UPDATE blocks SET category = 'PORTATIL', updated_at = now()
                    WHERE name = 'Bloque respondido'
                    """)).isEqualTo(1);
                assertThat(statement.executeUpdate("""
                    UPDATE questions SET category = 'PORTATIL', updated_at = now()
                    WHERE statement = 'Pregunta respondida'
                    """)).isEqualTo(1);
            }

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement()) {
                assertThatThrownBy(() -> statement.executeUpdate("""
                    UPDATE blocks SET name = 'Estructura modificada'
                    WHERE name = 'Bloque respondido'
                    """)).isInstanceOf(SQLException.class)
                    .hasMessageContaining("No se puede modificar una pregunta que ya tiene respuestas");
            }

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement()) {
                assertThatThrownBy(() -> statement.executeUpdate("""
                    UPDATE questions SET statement = 'Enunciado modificado'
                    WHERE statement = 'Pregunta respondida'
                    """)).isInstanceOf(SQLException.class)
                    .hasMessageContaining("No se puede modificar una pregunta que ya tiene respuestas");
            }

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement()) {
                assertThatThrownBy(() -> statement.executeUpdate("""
                    UPDATE surveys SET category = 'DATOS_PERSONALES'
                    WHERE title = 'Encuesta existente'
                    """)).isInstanceOf(SQLException.class);
            }
        }
    }
}
