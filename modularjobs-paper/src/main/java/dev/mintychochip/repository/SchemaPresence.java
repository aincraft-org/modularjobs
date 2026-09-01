package dev.mintychochip.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/** Fail-fast check that required tables exist. Does not create them. */
public final class SchemaPresence {

  /** Core tables the plugin depends on. */
  public static final List<String> REQUIRED_TABLES =
      List.of(
          "job_progression",
          "archive_job_progression",
          "job_tasks",
          "job_task_payables",
          "payable_records",
          "time_boosts",
          "player_upgrades");

  /** Columns introduced by operator-run migrations that runtime queries require. */
  public static final Map<String, List<String>> REQUIRED_COLUMNS =
      Map.of(
          "job_progression", List.of("current_node_key"),
          "archive_job_progression", List.of("current_node_key"));

  /** Session API table (same MySQL store). Optional for pure game-only pools. */
  public static final String EDITOR_SESSIONS = "editor_sessions";

  private static final String TABLE_EXISTS = SqlStatements.load("schema/table-exists.sql");
  private static final String COLUMN_EXISTS = SqlStatements.load("schema/column-exists.sql");

  private SchemaPresence() {}

  /**
   * Ensures every name in {@code required} exists in the connection's default schema.
   *
   * @throws SchemaMissingException if any table is absent
   */
  public static void requireTables(
      @NotNull Connection connection, @NotNull DatabaseType type, @NotNull List<String> required)
      throws SQLException {
    List<String> missing = new ArrayList<>();
    for (String table : required) {
      if (!tableExists(connection, type, table)) {
        missing.add(table);
      }
    }
    if (!missing.isEmpty()) {
      throw new SchemaMissingException(type, missing);
    }
  }

  /**
   * Ensures each required table contains every named column.
   *
   * @throws SchemaOutdatedException if any column is absent
   */
  public static void requireColumns(
      @NotNull Connection connection,
      @NotNull DatabaseType type,
      @NotNull Map<String, List<String>> required)
      throws SQLException {
    List<String> missing = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : required.entrySet()) {
      for (String column : entry.getValue()) {
        if (!columnExists(connection, type, entry.getKey(), column)) {
          missing.add(entry.getKey() + "." + column);
        }
      }
    }
    if (!missing.isEmpty()) {
      throw new SchemaOutdatedException(type, missing);
    }
  }

  /** Table exists. */
  public static boolean tableExists(
      @NotNull Connection connection, @NotNull DatabaseType type, @NotNull String table)
      throws SQLException {
    if (type != DatabaseType.MYSQL) {
      throw new IllegalArgumentException("Only MySQL is supported, got " + type);
    }
    try (PreparedStatement ps = connection.prepareStatement(TABLE_EXISTS)) {
      ps.setString(1, table);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  /** Column exists. */
  public static boolean columnExists(
      @NotNull Connection connection,
      @NotNull DatabaseType type,
      @NotNull String table,
      @NotNull String column)
      throws SQLException {
    if (type != DatabaseType.MYSQL) {
      throw new IllegalArgumentException("Only MySQL is supported, got " + type);
    }
    try (PreparedStatement ps = connection.prepareStatement(COLUMN_EXISTS)) {
      ps.setString(1, table);
      ps.setString(2, column);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  /** Thrown when tables exist but required migration columns are absent. */
  public static final class SchemaOutdatedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final List<String> missingColumns;

    SchemaOutdatedException(@NotNull DatabaseType type, @NotNull List<String> missingColumns) {
      super(
          "Database schema is outdated for "
              + type
              + ". Missing columns: "
              + missingColumns
              + ". Stop every plugin instance and apply the operator-run migrations, including "
              + "scripts/migrate-add-current-node-key.sql.");
      this.missingColumns = List.copyOf(missingColumns);
    }

    public @NotNull List<String> getMissingColumns() {
      return missingColumns;
    }
  }

  /** Thrown when schema was not provisioned. Message points operators at the SQL script. */
  public static final class SchemaMissingException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final DatabaseType type;
    private final List<String> missingTables;

    /** Schema missing exception. */
    public SchemaMissingException(@NotNull DatabaseType type, @NotNull List<String> missingTables) {
      super(buildMessage(type, missingTables));
      this.type = type;
      this.missingTables = List.copyOf(missingTables);
    }

    public @NotNull DatabaseType getType() {
      return type;
    }

    public @NotNull List<String> getMissingTables() {
      return missingTables;
    }

    private static @NotNull String buildMessage(
        @NotNull DatabaseType type, @NotNull List<String> missing) {
      return "Database schema not provisioned for "
          + type
          + ". Missing tables: "
          + missing
          + ". The plugin does not create tables. Apply "
          + "modularjobs-paper/src/main/resources/sql/mysql.sql out-of-band "
          + "(see scripts/apply-mysql-schema.sh).";
    }
  }
}
