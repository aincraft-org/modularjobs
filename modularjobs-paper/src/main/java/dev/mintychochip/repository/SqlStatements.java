package dev.mintychochip.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Loads packaged SQL statement text from {@code /sql/} classpath resources. */
public final class SqlStatements {

  private SqlStatements() {}

  /**
   * Reads a database-independent SQL resource. Relative names resolve under {@code /sql/}.
   *
   * @param name resource name such as {@code job_tasks/select-task-id.sql}
   * @return trimmed statement text
   * @throws IllegalStateException if the resource is missing
   */
  @Contract(pure = true)
  public static @NotNull String load(@NotNull String name) {
    String path = name.startsWith("/") ? name : "/sql/" + name;
    return loadPath(path);
  }

  /**
   * Reads a dialect-specific SQL resource from {@code /sql/<type>/}.
   *
   * @param type selected database dialect
   * @param name dialect-relative resource name
   * @return trimmed statement text
   * @throws IllegalStateException if the dialect resource is missing
   */
  @Contract(pure = true)
  public static @NotNull String load(@NotNull DatabaseType type, @NotNull String name) {
    Objects.requireNonNull(type, "type");
    String relative = name.startsWith("/") ? name.substring(1) : name;
    return loadPath("/sql/" + type.getIdentifier() + "/" + relative);
  }

  private static @NotNull String loadPath(@NotNull String path) {
    try (InputStream in = SqlStatements.class.getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalStateException("missing SQL resource: " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
