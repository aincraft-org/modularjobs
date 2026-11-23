package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zaxxer.hikari.HikariConfig;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

class HikariConfigProviderTest {

  @Test
  void mysqlConfigEnablesPrepStmtCache() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("jdbc-url", "jdbc:mysql://127.0.0.1:3306/modularjobs");
    section.set("username", "modularjobs");
    section.set("password", "secret");
    section.set("maximum-pool-size", 10);

    HikariConfig config = new HikariConfigProvider(section, DatabaseType.MYSQL).create();

    assertEquals("true", config.getDataSourceProperties().getProperty("cachePrepStmts"));
    assertEquals("250", config.getDataSourceProperties().getProperty("prepStmtCacheSize"));
  }

  @Test
  void sqliteConfigUsesOneConnectionAndNoCredentials() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("jdbc-url", "jdbc:sqlite:plugins/ModularJobs/modularjobs.db");

    HikariConfig config = new HikariConfigProvider(section, DatabaseType.SQLITE).create();

    assertEquals("org.sqlite.JDBC", config.getDriverClassName());
    assertEquals(1, config.getMaximumPoolSize());
    assertEquals("PRAGMA foreign_keys = ON", config.getConnectionInitSql());
    assertNull(config.getUsername());
    assertNull(config.getPassword());
    assertNull(config.getDataSourceProperties().getProperty("cachePrepStmts"));
  }

  @Test
  void sqliteRejectsMultipleConnections() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("jdbc-url", "jdbc:sqlite:plugins/ModularJobs/modularjobs.db");
    section.set("maximum-pool-size", 2);

    assertThrows(
        IllegalArgumentException.class,
        () -> new HikariConfigProvider(section, DatabaseType.SQLITE).create());
  }
}
