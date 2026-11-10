package com.formquid;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class DevSeedScriptTest {

    @Test
    void resetScriptCreatesSurveysWithTheConsolidatedCategories() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration", "classpath:db/testmigration")
                .load()
                .migrate();

            String sql = Files.readString(Path.of("../scripts/reset-initizalice-DB.sql"));
            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 Statement statement = connection.createStatement()) {
                // El superadministrador conservado puede no tener el ID 1.
                statement.executeUpdate("""
                    INSERT INTO users (role, email, name, institution, password_hash, created_at, updated_at)
                    VALUES ('SUPERADMIN', 'replacement@formquid.local', 'Relevo', 'FormQuid', 'preserved-hash', now(), now())
                    """);
                statement.executeUpdate("DELETE FROM users WHERE email = 'test-superadmin@formquid.local'");
                statement.execute("SET formquid.allow_dev_seed = 'yes'");
                statement.execute(sql);

                try (ResultSet admin = statement.executeQuery("""
                    SELECT email, password_hash FROM users WHERE id = 1 AND role = 'SUPERADMIN'
                    """)) {
                    assertThat(admin.next()).isTrue();
                    assertThat(admin.getString("email")).isEqualTo("replacement@formquid.local");
                    assertThat(admin.getString("password_hash")).isEqualTo("preserved-hash");
                }
                // Verifica también el reinicio cuando ya se conserva el ID 1.
                statement.executeUpdate("""
                    INSERT INTO survey_cover_files (filename, survey_id, content_type, image_bytes)
                    SELECT '123e4567-e89b-12d3-a456-426614174000.png', id, 'image/png', decode('010203', 'hex')
                    FROM surveys ORDER BY id LIMIT 1
                    """);
                statement.execute(sql);

                try (ResultSet rows = statement.executeQuery("""
                    SELECT (SELECT count(*) FROM categories) AS categories,
                           (SELECT count(*) FROM questions WHERE scope = 'CATALOG_FIXED') AS fixed_questions,
                           (SELECT count(*) FROM surveys) AS surveys,
                           (SELECT count(*) FROM survey_cover_files) AS covers,
                           (SELECT count(*) FROM surveys WHERE cardinality(tags) <> 2) AS surveys_without_tags,
                           (SELECT count(*) FROM surveys WHERE category = 'ORDENADORES') AS computer_surveys,
                           (SELECT count(*) FROM surveys WHERE category = 'INTELIGENCIA_ARTIFICIAL') AS ai_surveys
                    """)) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getLong("categories")).isEqualTo(19);
                    assertThat(rows.getLong("fixed_questions")).isEqualTo(136);
                    assertThat(rows.getLong("surveys")).isEqualTo(30);
                    assertThat(rows.getLong("covers")).isZero();
                    assertThat(rows.getLong("surveys_without_tags")).isZero();
                    assertThat(rows.getLong("computer_surveys")).isGreaterThanOrEqualTo(2);
                    assertThat(rows.getLong("ai_surveys")).isGreaterThanOrEqualTo(2);
                }
            }
        }
    }
}
