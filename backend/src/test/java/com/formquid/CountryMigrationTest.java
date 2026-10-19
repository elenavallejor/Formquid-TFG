package com.formquid;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class CountryMigrationTest {

    @Test
    void normalizesKnownNamesAndPreservesUnknownLegacyValues() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            String jdbcUrl = postgres.getJdbcUrl();
            String username = postgres.getUsername();
            String password = postgres.getPassword();

            Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration", "classpath:db/testmigration")
                .target("31")
                .load()
                .migrate();

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                    UPDATE users SET country = ' España '
                    WHERE email = 'test-superadmin@formquid.local'
                    """);
                statement.executeUpdate("""
                    INSERT INTO registration_requests
                        (email, name, institution, country, motivation, password_hash, created_at)
                    VALUES
                        ('known@example.com', 'Conocida', 'Centro', 'Kosovo', 'Registro', 'hash', now()),
                        ('unknown@example.com', 'Desconocida', 'Centro', 'País anterior', 'Registro', 'hash', now())
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
                     SELECT
                         (SELECT country FROM users WHERE email = 'test-superadmin@formquid.local'),
                         (SELECT country FROM registration_requests WHERE email = 'known@example.com'),
                         (SELECT country FROM registration_requests WHERE email = 'unknown@example.com')
                     """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("ES");
                assertThat(rows.getString(2)).isEqualTo("XK");
                assertThat(rows.getString(3)).isEqualTo("País anterior");
            }
        }
    }
}
