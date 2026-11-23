package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import dev.mintychochip.domain.repository.PlayerJobStateRepository;
import dev.mintychochip.repository.ConnectionSource;
import dev.mintychochip.repository.ConnectionSourceFactory;
import dev.mintychochip.repository.DatabaseType;
import dev.mintychochip.test.MockBukkitSupport;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Verifies the production player-state repository selects SQLite upsert SQL. */
class SqlitePlayerJobStateRepositoryTest {

  @TempDir Path tempDir;

  private ConnectionSource source;
  private MemoryJobRepositoryImpl jobs;

  @BeforeEach
  void setUp() throws Exception {
    MockBukkitSupport.mockServer();
    String url = "jdbc:sqlite:" + tempDir.resolve("progression.db").toAbsolutePath();
    Class.forName(DatabaseType.SQLITE.getClassName());
    try (Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement()) {
      for (String sql : DatabaseType.SQLITE.getSqlTables()) {
        statement.execute(sql);
      }
    }

    YamlConfiguration config = new YamlConfiguration();
    config.set("type", "sqlite");
    config.set("jdbc-url", url);
    Plugin plugin = MockBukkit.createMockPlugin("ModularJobsSqliteProgressionTest");
    source = new ConnectionSourceFactory(plugin, config).create();

    JobRecord miner =
        new JobRecord("modularjobs:miner", "Miner", "Mines", 100, "level * 100", Map.of(), null);
    JobRecord prospector =
        new JobRecord(
            "modularjobs:prospector",
            "Prospector",
            "Finds ore",
            100,
            "unused child curve",
            Map.of(),
            miner.jobKey());
    jobs =
        new MemoryJobRepositoryImpl(Map.of(miner.jobKey(), miner, prospector.jobKey(), prospector));
  }

  @AfterEach
  void tearDown() throws Exception {
    if (source != null && !source.isClosed()) {
      source.shutdown();
    }
    MockBukkitSupport.unmockServer();
  }

  @Test
  void saveAtomicallyUpsertsCurrentNodeAndExactExperience() {
    PlayerJobStateRepository repository =
        RelationalPlayerJobStateRepositoryImpl.create(jobs, source, "job_progression");
    assertTrue(
        repository.save(
            new PlayerJobStateRecord(
                "sqlite-player",
                "modularjobs:miner",
                "modularjobs:miner",
                new BigDecimal("10.0000000001"))));
    assertTrue(
        repository.save(
            new PlayerJobStateRecord(
                "sqlite-player",
                "modularjobs:miner",
                "modularjobs:prospector",
                new BigDecimal("99.1234567890"))));

    PlayerJobStateRepository uncached =
        RelationalPlayerJobStateRepositoryImpl.create(jobs, source, "job_progression");
    PlayerJobStateRecord loaded = uncached.load("sqlite-player", "modularjobs:miner");
    assertNotNull(loaded);
    assertEquals("modularjobs:prospector", loaded.currentNodeKey());
    assertEquals(0, new BigDecimal("99.1234567890").compareTo(loaded.experience()));
  }
}
