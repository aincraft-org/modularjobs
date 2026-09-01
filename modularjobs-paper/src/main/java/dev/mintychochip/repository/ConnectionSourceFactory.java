package dev.mintychochip.repository;

import com.google.common.base.Preconditions;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Builds a MySQL {@link ConnectionSource} through the Utilities SQL lifecycle. Schema is never
 * applied here — ops must run {@code scripts/apply-mysql-schema.sh} first.
 */
public final class ConnectionSourceFactory {

  @NotNull private final Plugin plugin;

  @NotNull private final ConfigurationSection configuration;

  /** Connection source factory. */
  public ConnectionSourceFactory(
      @NotNull Plugin plugin, @NotNull ConfigurationSection configuration) {
    this.plugin = plugin;
    this.configuration = configuration;
  }

  /** API member. */
  @NotNull
  public ConnectionSource create() {
    Preconditions.checkState(
        configuration.contains("type"), "database section missing type (must be mysql)");
    DatabaseType type = DatabaseType.fromIdentifier(configuration.getString("type"));

    Logger log = plugin.getLogger();
    if (SchemaPolicy.hasIgnoredRemoteAutoSchema(type, configuration)) {
      log.warning(
          "database.yml sets auto-schema but ModularJobs never runs DDL in-process. "
              + "Ignore this key and provision with scripts/apply-mysql-schema.sh "
              + "(sql/mysql.sql).");
    }

    ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(plugin.getClass().getClassLoader());
    ConnectionSource source = null;
    try {
      // SqlDatabase is deliberately given a non-existent migration location. ModularJobs owns an
      // operator-applied SQL file rather than Flyway migrations, so utility startup must not scan
      // or replay another plugin's classpath migrations.
      source = new HikariSourceImpl(new HikariConfigProvider(configuration, type).create(), type);

      verifySchemaOrClose(source, type);
      return source;
    } finally {
      Thread.currentThread().setContextClassLoader(previousClassLoader);
    }
  }

  static void verifySchemaOrClose(@NotNull ConnectionSource source, @NotNull DatabaseType type) {
    try (SourceOwnership ownership = new SourceOwnership(source)) {
      if (SchemaPolicy.shouldVerifySchemaPresent(type)) {
        try (Connection connection = source.getConnection()) {
          SchemaPresence.requireTables(connection, type, SchemaPresence.REQUIRED_TABLES);
          SchemaPresence.requireColumns(connection, type, SchemaPresence.REQUIRED_COLUMNS);
        }
      }
      ownership.transfer();
    } catch (SQLException failure) {
      throw new RuntimeException(
          "Failed to verify MySQL schema (is the database up and schema applied?)", failure);
    }
  }

  /** Holds shutdown responsibility until schema verification succeeds. */
  private static final class SourceOwnership implements AutoCloseable {
    private final ConnectionSource source;
    private boolean transferred;

    private SourceOwnership(@NotNull ConnectionSource source) {
      this.source = source;
    }

    private void transfer() {
      transferred = true;
    }

    @Override
    public void close() throws SQLException {
      if (!transferred) {
        source.shutdown();
      }
    }
  }
}
