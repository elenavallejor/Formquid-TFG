package com.formquid;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuperadminGuardMigrationTest {

    @Test
    void concurrentDeletesLeaveOneSuperadmin() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            String url = postgres.getJdbcUrl();
            String username = postgres.getUsername();
            String password = postgres.getPassword();

            Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration", "classpath:db/testmigration")
                .load().migrate();

            long firstId;
            long secondId;
            try (Connection connection = DriverManager.getConnection(url, username, password);
                 Statement statement = connection.createStatement()) {
                try (ResultSet result = statement.executeQuery(
                    "select id from users where role = 'SUPERADMIN'")) {
                    assertThat(result.next()).isTrue();
                    firstId = result.getLong(1);
                }
                try (ResultSet result = statement.executeQuery("""
                    insert into users (role, email, name, institution, password_hash, created_at, updated_at)
                    values ('SUPERADMIN', 'second-admin@formquid.local', 'Segundo', 'FormQuid', 'hash', now(), now())
                    returning id
                    """)) {
                    assertThat(result.next()).isTrue();
                    secondId = result.getLong(1);
                }
            }

            CountDownLatch start = new CountDownLatch(1);
            try (var executor = Executors.newFixedThreadPool(2)) {
                Future<Boolean> first = executor.submit(() -> mutateUser(url, username, password,
                    "delete from users where id = ?", firstId, start));
                Future<Boolean> second = executor.submit(() -> mutateUser(url, username, password,
                    "delete from users where id = ?", secondId, start));
                start.countDown();
                assertThat((first.get() ? 1 : 0) + (second.get() ? 1 : 0)).isEqualTo(1);
            }

            try (Connection connection = DriverManager.getConnection(url, username, password);
                 Statement statement = connection.createStatement()) {
                try (ResultSet result = statement.executeQuery(
                    "select count(*) from users where role = 'SUPERADMIN'")) {
                    result.next();
                    assertThat(result.getLong(1)).isEqualTo(1);
                }
                assertThatThrownBy(() -> statement.executeUpdate(
                    "delete from users where role = 'SUPERADMIN'"))
                    .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    void emptyDatabaseRegistersFirstSuperadminAndRejectsLastDemotion() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            String url = postgres.getJdbcUrl();
            String username = postgres.getUsername();
            String password = postgres.getPassword();

            Flyway.configure().dataSource(url, username, password)
                .locations("classpath:db/migration").load().migrate();

            try (Connection connection = DriverManager.getConnection(url, username, password);
                 Statement statement = connection.createStatement()) {
                assertThatThrownBy(() -> statement.executeUpdate("""
                    insert into users (role, email, name, institution, password_hash, created_at, updated_at)
                    values ('EDITOR', 'first-editor@formquid.local', 'Editor', 'FormQuid', 'hash', now(), now())
                    """)).isInstanceOf(SQLException.class);

                statement.executeUpdate("""
                    insert into users (role, email, name, institution, password_hash, created_at, updated_at)
                    values ('SUPERADMIN', 'first-admin@formquid.local', 'Admin', 'FormQuid', 'hash', now(), now())
                    """);
                try (ResultSet result = statement.executeQuery("select count(*) from superadmin_guard")) {
                    result.next();
                    assertThat(result.getLong(1)).isEqualTo(1);
                }
                assertThatThrownBy(() -> statement.executeUpdate(
                    "update users set role = 'ADMIN' where role = 'SUPERADMIN'"))
                    .isInstanceOf(SQLException.class);

                statement.executeUpdate("""
                    insert into users (role, email, name, institution, password_hash, created_at, updated_at)
                    values ('SUPERADMIN', 'replacement@formquid.local', 'Relevo', 'FormQuid', 'hash', now(), now())
                    """);
                statement.executeUpdate("""
                    update users set role = 'ADMIN' where email = 'first-admin@formquid.local'
                    """);
                try (ResultSet result = statement.executeQuery("""
                    select users.email from superadmin_guard
                    join users on users.id = superadmin_guard.user_id
                    """)) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isEqualTo("replacement@formquid.local");
                }
            }
        }
    }

    private boolean mutateUser(String url, String username, String password, String sql,
                               long id, CountDownLatch start)
        throws Exception {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            start.await();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                statement.executeUpdate();
                connection.commit();
                return true;
            } catch (SQLException error) {
                connection.rollback();
                return false;
            }
        }
    }
}
