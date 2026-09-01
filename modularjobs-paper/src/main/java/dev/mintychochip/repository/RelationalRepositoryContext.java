package dev.mintychochip.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adapter decoupling a temporary SQL-backed simple key/value repository from the database dialect
 * and schema, defining the query strings the repository executes and the null/parameter/result
 * conversions needed for each operation.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface RelationalRepositoryContext<K, V> {

  /** Returns the select query. */
  @NotNull
  String getSelectQuery();

  /** Returns the save query. */
  @NotNull
  String getSaveQuery();

  /** Returns the delete query. */
  @NotNull
  String getDeleteQuery();

  /** Sets the key. */
  void setKey(@NotNull PreparedStatement ps, @NotNull K key) throws SQLException;

  /** Sets the save values. */
  void setSaveValues(@NotNull PreparedStatement ps, @NotNull K key, @NotNull V value)
      throws SQLException;

  /** Map result. */
  @Nullable
  V mapResult(@NotNull ResultSet rs, @NotNull K key) throws SQLException;
}
