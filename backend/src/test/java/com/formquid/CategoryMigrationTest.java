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
                            (SELECT category FROM questions WHERE statement = 'Pregunta existente')
                     """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("PORTATIL");
                assertThat(rows.getString(2)).isEqualTo("INTELIGENCIA_ARTIFICIAL");
                assertThat(rows.getString(3)).isEqualTo("TELEFONO_INTELIGENTE");
                assertThat(rows.getString(4)).isEqualTo("DATOS_PERSONALES");
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
