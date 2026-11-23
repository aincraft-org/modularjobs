package dev.mintychochip.repository;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Schema ownership for every ModularJobs database backend.
 *
 * <p>The plugin process never runs DDL. Operators provision the selected schema from {@code
 * sql/mysql.sql} or {@code sql/sqlite.sql} before startup. On connect, the plugin verifies required
 * tables and migration-sensitive columns.
 *
 * <p>Config key {@code auto-schema} is ignored and logged as a misconfiguration.
 */
public final class SchemaPolicy {

  private SchemaPolicy() {}

  /**
   * Determines whether schema DDL should run during connection startup.
   *
   * @return always false — never CREATE TABLE inside the game process
   */
  public static boolean shouldApplySchemaOnConnect(
      @NotNull DatabaseType type, @Nullable ConfigurationSection configuration) {
    return false;
  }

  /**
   * Reports whether required tables must exist before the plugin uses the database.
   *
   * @return true for every supported externally provisioned backend
   */
  public static boolean shouldVerifySchemaPresent(@NotNull DatabaseType type) {
    return true;
  }

  /**
   * Detects legacy {@code auto-schema} configuration that the plugin ignores.
   *
   * @return true if config still sets {@code auto-schema}
   */
  public static boolean hasIgnoredRemoteAutoSchema(
      @NotNull DatabaseType type, @Nullable ConfigurationSection configuration) {
    return configuration != null && configuration.contains("auto-schema");
  }
}
