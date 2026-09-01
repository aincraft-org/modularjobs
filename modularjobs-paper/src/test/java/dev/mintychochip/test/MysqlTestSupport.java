package dev.mintychochip.test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.mintychochip.repository.DatabaseType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.jetbrains.annotations.NotNull;

/** Shared helper for tests that need a live MySQL 8 instance. */
public final class MysqlTestSupport {

  public static final String DEFAULT_URL = "jdbc:mysql://localhost:13306/modularjobs";
  public static final String DEFAULT_USER = "test";
  public static final String DEFAULT_PASSWORD = "test";

  private MysqlTestSupport() {}

  /** Jdbc url. */
  public static @NotNull String jdbcUrl() {
    return envOr("MODULARJOBS_TEST_MYSQL_URL", DEFAULT_URL);
  }

  /** User. */
  public static @NotNull String user() {
    return envOr("MODULARJOBS_TEST_MYSQL_USER", DEFAULT_USER);
  }

  /** Password. */
  public static @NotNull String password() {
    return envOr("MODULARJOBS_TEST_MYSQL_PASSWORD", DEFAULT_PASSWORD);
  }

  /** Returns whether available. */
  public static boolean isAvailable() {
    try {
      Class.forName(DatabaseType.MYSQL.getClassName());
      try (Connection c = DriverManager.getConnection(jdbcUrl(), user(), password());
          Statement st = c.createStatement()) {
        st.execute("SELECT 1");
        return true;
      }
    } catch (ClassNotFoundException | SQLException e) {
      return false;
    }
  }

  /** Assume available. */
  public static void assumeAvailable() {
    assumeTrue(isAvailable(), "MySQL must be reachable at " + jdbcUrl());
  }

  /** Open. */
  public static @NotNull Connection open() throws SQLException {
    return DriverManager.getConnection(jdbcUrl(), user(), password());
  }

  /** Apply shipped schema. */
  public static void applyShippedSchema(@NotNull Connection connection) throws SQLException {
    for (String sql : DatabaseType.MYSQL.getSqlTables()) {
      try (Statement st = connection.createStatement()) {
        st.execute(sql);
      }
    }
  }

  private static @NotNull String envOr(@NotNull String key, @NotNull String defaultValue) {
    String value = System.getenv(key);
    return value == null || value.isBlank() ? defaultValue : value;
  }
}
