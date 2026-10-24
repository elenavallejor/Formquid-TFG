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
                statement.execute("SET formquid.allow_dev_seed = 'yes'");
                statement.execute(sql);

                try (ResultSet rows = statement.executeQuery("""
                    SELECT (SELECT count(*) FROM categories) AS categories,
                           (SELECT count(*) FROM questions WHERE scope = 'CATALOG_FIXED') AS fixed_questions,
                           (SELECT count(*) FROM surveys) AS surveys,
                           (SELECT count(*) FROM surveys WHERE cardinality(tags) <> 2) AS surveys_without_tags,
                           (SELECT count(*) FROM surveys WHERE category = 'ORDENADORES') AS computer_surveys,
                           (SELECT count(*) FROM surveys WHERE category = 'INTELIGENCIA_ARTIFICIAL') AS ai_surveys
                    """)) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getLong("categories")).isEqualTo(19);
                    assertThat(rows.getLong("fixed_questions")).isEqualTo(136);
                    assertThat(rows.getLong("surveys")).isEqualTo(30);
                    assertThat(rows.getLong("surveys_without_tags")).isZero();
                    assertThat(rows.getLong("computer_surveys")).isGreaterThanOrEqualTo(2);
                    assertThat(rows.getLong("ai_surveys")).isGreaterThanOrEqualTo(2);
                }
            }
        }
    }
}
