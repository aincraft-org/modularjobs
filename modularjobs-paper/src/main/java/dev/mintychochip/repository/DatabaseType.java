package dev.mintychochip.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Supported ModularJobs stores. Schemas are applied out-of-band from {@code sql/mysql.sql} or
 * {@code sql/sqlite.sql}.
 */
public enum DatabaseType {
  MYSQL("mysql", "com.mysql.cj.jdbc.Driver"),
  SQLITE("sqlite", "org.sqlite.JDBC");

  @NotNull private final String identifier;
  private final String className;

  DatabaseType(@NotNull String identifier, @NotNull String className) {
    this.identifier = identifier;
    this.className = className;
  }

  public @NotNull String getIdentifier() {
    return identifier;
  }

  public @NotNull String getClassName() {
    return className;
  }

  /**
   * Returns the shipped DDL statements for this database type.
   *
   * <p>The file is split on semicolons that are not inside SQL comments or string literals, so
   * comment lines such as {@code -- Apply out-of-band; the plugin never runs DDL.} do not create
   * partial statements.
   */
  public @NotNull String[] getSqlTables() {
    try (InputStream resourceStream =
            ResourceExtractor.getResourceStream(String.format("sql/%s.sql", identifier));
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
      String sql = reader.lines().collect(Collectors.joining("\n"));
      return splitStatements(sql);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Splits SQL on statement terminators ({@code ;}) while respecting single-line comments ({@code
   * --}), block comments ({@code /* * /}), and single/double-quoted string literals.
   */
  @NotNull
  static String[] splitStatements(@NotNull String sql) {
    List<String> statements = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    boolean inLineComment = false;
    boolean inBlockComment = false;
    boolean escapeNext = false;

    int i = 0;
    while (i < sql.length()) {
      char c = sql.charAt(i);

      if (inLineComment) {
        if (c == '\n') {
          inLineComment = false;
        }
        i++;
        continue;
      }

      if (inBlockComment) {
        if (c == '*' && i + 1 < sql.length() && sql.charAt(i + 1) == '/') {
          inBlockComment = false;
          i += 2;
        } else {
          i++;
        }
        continue;
      }

      if (inSingleQuote || inDoubleQuote) {
        current.append(c);
        if (escapeNext) {
          escapeNext = false;
        } else if (c == '\\') {
          escapeNext = true;
        } else if (c == '\'' && inSingleQuote) {
          inSingleQuote = false;
        } else if (c == '"' && inDoubleQuote) {
          inDoubleQuote = false;
        }
        i++;
        continue;
      }

      if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
        inLineComment = true;
        i += 2;
        continue;
      }

      if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
        inBlockComment = true;
        i += 2;
        continue;
      }

      if (c == '\'') {
        inSingleQuote = true;
        current.append(c);
        i++;
        continue;
      }

      if (c == '"') {
        inDoubleQuote = true;
        current.append(c);
        i++;
        continue;
      }

      if (c == ';') {
        String stmt = current.toString().trim();
        if (!stmt.isEmpty()) {
          statements.add(stmt + ";");
        }
        current.setLength(0);
      } else {
        current.append(c);
      }
      i++;
    }

    String remaining = current.toString().trim();
    if (!remaining.isEmpty()) {
      statements.add(remaining + ";");
    }

    return statements.toArray(new String[0]);
  }

  /**
   * Resolves a configured database identifier, defaulting blank values to MySQL.
   *
   * @throws IllegalArgumentException if the identifier is unsupported
   */
  @Contract(value = "null -> !null", pure = true)
  public static @NotNull DatabaseType fromIdentifier(@Nullable String identifier) {
    if (identifier == null || identifier.isBlank()) {
      return getDefault();
    }
    String id = identifier.trim().toLowerCase(Locale.ROOT);
    for (DatabaseType type : values()) {
      if (type.identifier.equals(id)) {
        return type;
      }
    }
    throw new IllegalArgumentException(
        "Unsupported database type '"
            + identifier
            + "'. ModularJobs supports mysql and sqlite. "
            + "Provision the selected schema externally before startup.");
  }

  @Contract(pure = true)
  public static @NotNull DatabaseType getDefault() {
    return MYSQL;
  }
}
