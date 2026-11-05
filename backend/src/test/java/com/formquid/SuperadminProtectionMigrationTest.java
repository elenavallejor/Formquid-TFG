package com.formquid;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuperadminProtectionMigrationTest {

    private static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:16-alpine").withDatabaseName("superadmin_template");
    private static int databaseNumber;
    private String url;

    @BeforeAll
    static void startPostgres() {
        POSTGRES.start();
    }

    @AfterAll
    static void stopPostgres() {
        POSTGRES.stop();
    }

    @BeforeEach
    void migrateDatabase() throws SQLException {
        url = createDatabase();
        flyway().load().migrate();
    }

    @Test
    void upgrade_whenGuardExists_shouldPreserveUsersAndRemoveObsoleteObjects() throws SQLException {
        url = createDatabase();
        flyway().target("41").load().migrate();
        long admin = insertUser("SUPERADMIN", "original");
        long editor = insertUser("EDITOR", "editor");
        execute("UPDATE users SET profile_image_key = 'profile.png' WHERE id = " + admin);
        assertThat(count("SELECT count(*) FROM superadmin_guard")).isEqualTo(1);

        flyway().load().migrate();

        assertThat(count("SELECT count(*) FROM users")).isEqualTo(2);
        assertThat(count("""
            SELECT count(*) FROM users WHERE email = 'original@formquid.local'
            AND role = 'SUPERADMIN' AND password_hash = 'hash' AND profile_image_key = 'profile.png'
            """)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM users WHERE id = " + editor)).isEqualTo(1);
        assertThat(count("""
            SELECT count(*) FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = 'superadmin_guard'
            """)).isZero();
        assertThat(count("""
            SELECT count(*) FROM pg_proc WHERE pronamespace = 'public'::regnamespace
            AND proname IN ('register_first_superadmin', 'reassign_superadmin_guard',
                            'prevent_superadmin_guard_removal', 'prevent_no_superadmin')
            """)).isZero();
        assertThat(count("""
            SELECT count(*) FROM pg_constraint WHERE conrelid = 'users'::regclass
            AND conname IN ('uk_users_role_id_guard', 'uk_users_role_id')
            """)).isZero();
        assertRejected("DELETE FROM users WHERE id = " + admin);
    }

    @Test
    void bootstrap_whenDatabaseIsEmpty_shouldRequireSuperadminAndAllowNormalAccountsAfterwards() throws SQLException {
        assertRejected(insertSql("EDITOR", "first-editor"));
        assertRejected(insertSql("ADMIN", "first-admin"));
        assertThat(count("SELECT count(*) FROM users")).isZero();

        insertUser("SUPERADMIN", "first-superadmin");
        long editor = insertUser("EDITOR", "editor");
        insertUser("ADMIN", "admin");
        execute("UPDATE users SET name = 'Nombre nuevo' WHERE id = " + editor);
        execute("UPDATE users SET role = 'ADMIN' WHERE id = " + editor);
        execute("DELETE FROM users WHERE id = " + editor);
        assertThat(count("SELECT count(*) FROM users")).isEqualTo(2);
    }

    @Test
    void mutations_whenOnlyOneSuperadminRemains_shouldRejectDeletionDemotionAndTruncate() throws SQLException {
        long admin = insertUser("SUPERADMIN", "only-admin");
        insertUser("EDITOR", "editor");

        assertRejected("DELETE FROM users WHERE id = " + admin);
        assertRejected("UPDATE users SET role = 'ADMIN' WHERE id = " + admin);
        assertRejected("TRUNCATE users CASCADE");
        execute("UPDATE users SET role = role WHERE id = " + admin);
        assertThat(count("SELECT count(*) FROM users")).isEqualTo(2);
        assertThat(superadmins()).isEqualTo(1);
    }

    @Test
    void bulkMutations_whenTheyRemoveAllSuperadmins_shouldRollbackTheWholeStatement() throws SQLException {
        insertUser("SUPERADMIN", "first");
        insertUser("SUPERADMIN", "second");
        insertUser("EDITOR", "editor");

        assertRejected("DELETE FROM users");
        assertRejected("UPDATE users SET role = 'EDITOR' WHERE role = 'SUPERADMIN'");
        assertThat(count("SELECT count(*) FROM users")).isEqualTo(3);
        assertThat(superadmins()).isEqualTo(2);
    }

    @Test
    void roleTransfer_whenOneStatementReplacesTheSuperadmin_shouldSucceed() throws SQLException {
        long first = insertUser("SUPERADMIN", "first");
        long second = insertUser("EDITOR", "replacement");

        execute("UPDATE users SET role = CASE WHEN id = " + first
            + " THEN 'ADMIN' ELSE 'SUPERADMIN' END WHERE id IN (" + first + ", " + second + ")");

        assertThat(superadmins()).isEqualTo(1);
        execute("DELETE FROM users WHERE id = " + first);
        assertRejected("DELETE FROM users WHERE id = " + second);
    }

    static Stream<Arguments> concurrentMutations() {
        return Stream.of(Connection.TRANSACTION_READ_COMMITTED,
                         Connection.TRANSACTION_REPEATABLE_READ,
                         Connection.TRANSACTION_SERIALIZABLE)
            .flatMap(isolation -> Stream.of(
                Arguments.of(isolation, "DELETE FROM users WHERE id = ", "DELETE FROM users WHERE id = "),
                Arguments.of(isolation, "UPDATE users SET role = 'ADMIN' WHERE id = ", "UPDATE users SET role = 'EDITOR' WHERE id = "),
                Arguments.of(isolation, "DELETE FROM users WHERE id = ", "UPDATE users SET role = 'ADMIN' WHERE id = "),
                Arguments.of(isolation, "UPDATE users SET role = 'ADMIN' WHERE id = ", "DELETE FROM users WHERE id = ")
            ));
    }

    @ParameterizedTest
    @MethodSource("concurrentMutations")
    void concurrentMutations_whenBothRemoveASuperadmin_shouldCommitOnlyOne(
        int isolation, String firstSql, String secondSql
    ) throws Exception {
        long first = insertUser("SUPERADMIN", "first");
        long second = insertUser("SUPERADMIN", "second");

        try (Connection firstConnection = connect(); Connection secondConnection = connect();
             var executor = Executors.newSingleThreadExecutor()) {
            firstConnection.setTransactionIsolation(isolation);
            secondConnection.setTransactionIsolation(isolation);
            firstConnection.setAutoCommit(false);
            secondConnection.setAutoCommit(false);
            // Fija también una instantánea antigua en REPEATABLE READ y SERIALIZABLE.
            queryCount(secondConnection, "SELECT count(*) FROM users");
            long secondPid = queryCount(secondConnection, "SELECT pg_backend_pid()");
            try {
                try (Statement statement = firstConnection.createStatement()) {
                    statement.executeUpdate(firstSql + first);
                }
                var pending = executor.submit(() -> {
                    try (Statement statement = secondConnection.createStatement()) {
                        statement.executeUpdate(secondSql + second);
                        secondConnection.commit();
                        return (SQLException) null;
                    } catch (SQLException error) {
                        secondConnection.rollback();
                        return error;
                    }
                });

                awaitAdvisoryLock(secondPid);
                assertThat(pending.isDone()).isFalse();
                firstConnection.commit();

                SQLException failure = pending.get(10, TimeUnit.SECONDS);
                assertThat((Exception) failure).isNotNull();
                assertThat(failure.getSQLState()).isEqualTo(
                    isolation == Connection.TRANSACTION_READ_COMMITTED ? "23514" : "40001");
            } finally {
                firstConnection.rollback();
                secondConnection.rollback();
            }
        }
        assertThat(superadmins()).isEqualTo(1);
    }

    @Test
    void deletion_whenTransactionRollsBack_shouldReleaseProtectionForTheNextTransaction() throws Exception {
        long first = insertUser("SUPERADMIN", "first");
        long second = insertUser("SUPERADMIN", "second");
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM users WHERE id = " + first);
            }
            connection.rollback();
        }
        execute("DELETE FROM users WHERE id = " + second);
        assertThat(superadmins()).isEqualTo(1);
    }

    private void awaitAdvisoryLock(long pid) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (count("SELECT count(*) FROM pg_stat_activity WHERE pid = " + pid
                + " AND wait_event_type = 'Lock' AND wait_event = 'advisory'") == 1) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("La segunda operación no esperó al bloqueo de administración");
    }

    private String createDatabase() throws SQLException {
        String name = "superadmin_test_" + ++databaseNumber;
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        }
        return POSTGRES.getJdbcUrl().replace("/superadmin_template", "/" + name);
    }

    private org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
        return Flyway.configure().dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("classpath:db/migration");
    }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET statement_timeout = '10s'");
        }
        return connection;
    }

    private long insertUser(String role, String name) throws SQLException {
        return count(insertSql(role, name) + " RETURNING id");
    }

    private String insertSql(String role, String name) {
        return "INSERT INTO users (role, email, name, institution, password_hash, created_at, updated_at)"
            + " VALUES ('" + role + "', '" + name + "@formquid.local', '" + name
            + "', 'FormQuid', 'hash', now(), now())";
    }

    private void assertRejected(String sql) {
        assertThatThrownBy(() -> execute(sql)).isInstanceOfSatisfying(SQLException.class, error -> {
            assertThat(error.getSQLState()).isEqualTo("23514");
            assertThat(error.getMessage()).contains("Debe existir siempre un superadministrador");
        });
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long superadmins() throws SQLException {
        return count("SELECT count(*) FROM users WHERE role = 'SUPERADMIN'");
    }

    private long count(String sql) throws SQLException {
        try (Connection connection = connect()) {
            return queryCount(connection, sql);
        }
    }

    private long queryCount(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }
}
