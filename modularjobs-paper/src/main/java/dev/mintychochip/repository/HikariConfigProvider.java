package dev.mintychochip.repository;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * Builds a {@link HikariConfig} for a supported database section.
 *
 * <p>MySQL requires {@code jdbc-url}, {@code username}, and {@code password}. SQLite requires only
 * a {@code jdbc:sqlite:} URL, always uses a single-connection pool, and enables foreign-key
 * enforcement for every pooled connection.
 */
final class HikariConfigProvider {

  @NotNull private final ConfigurationSection configuration;

  @NotNull private final DatabaseType databaseType;

  HikariConfigProvider(
      @NotNull ConfigurationSection configuration, @NotNull DatabaseType databaseType) {
    this.configuration = configuration;
    this.databaseType = databaseType;
  }

  /**
   * Creates a Hikari configuration from the configured database settings.
   *
   * @return populated {@link HikariConfig}
   */
  @NotNull
  HikariConfig create() {
    final HikariConfig hikariConfig = new HikariConfig();

    String jdbcUrl = configuration.getString("jdbc-url");
    Preconditions.checkNotNull(jdbcUrl, "missing required field: database.jdbc-url");
    hikariConfig.setJdbcUrl(jdbcUrl);

    if (databaseType == DatabaseType.MYSQL) {
      String username = configuration.getString("username");
      String password = configuration.getString("password");
      Preconditions.checkNotNull(username, "missing required field: database.username");
      Preconditions.checkNotNull(password, "missing required field: database.password");
      hikariConfig.setUsername(username);
      hikariConfig.setPassword(password);
      hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
      hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
    } else {
      Preconditions.checkArgument(
          jdbcUrl.startsWith("jdbc:sqlite:"), "SQLite jdbc-url must start with jdbc:sqlite:");
      hikariConfig.setMaximumPoolSize(1);
      hikariConfig.setConnectionInitSql("PRAGMA foreign_keys = ON");
    }

    hikariConfig.setDriverClassName(databaseType.getClassName());

    int maxPoolSize = configuration.getInt("maximum-pool-size", -1);
    if (databaseType == DatabaseType.SQLITE) {
      Preconditions.checkArgument(
          maxPoolSize <= 1, "SQLite maximum-pool-size must be 1 when configured");
    } else if (maxPoolSize > 0) {
      hikariConfig.setMaximumPoolSize(maxPoolSize);
    }

    int minIdle = configuration.getInt("minimum-idle", -1);
    if (databaseType == DatabaseType.SQLITE) {
      Preconditions.checkArgument(
          minIdle <= 1, "SQLite minimum-idle must be 0 or 1 when configured");
    }
    if (minIdle >= 0) {
      hikariConfig.setMinimumIdle(minIdle);
    }

    long connectionTimeout = configuration.getLong("connection-timeout", -1);
    if (connectionTimeout > 0) {
      hikariConfig.setConnectionTimeout(connectionTimeout);
    }

    long idleTimeout = configuration.getLong("idle-timeout", -1);
    if (idleTimeout > 0) {
      hikariConfig.setIdleTimeout(idleTimeout);
    }

    long maxLifetime = configuration.getLong("max-lifetime", -1);
    if (maxLifetime > 0) {
      hikariConfig.setMaxLifetime(maxLifetime);
    }
    return hikariConfig;
  }
}
