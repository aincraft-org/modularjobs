package dev.mintychochip.repository;

import java.sql.Connection;
import java.sql.SQLException;
import org.jetbrains.annotations.NotNull;

/**
 * Supplies database connections and reports the configured database lifecycle.
 *
 * <p>The application connects to an existing schema; schema creation and migration are performed
 * externally rather than by this abstraction.
 */
public interface ConnectionSource {

  /** Shutdown. */
  void shutdown() throws SQLException;

  /** Returns the type. */
  @NotNull
  DatabaseType getType();

  /** Returns whether closed. */
  boolean isClosed();

  /** Returns the connection. */
  @NotNull
  Connection getConnection() throws SQLException;

  /** Returns whether setup. */
  boolean isSetup();
}
