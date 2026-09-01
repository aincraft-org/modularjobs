package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import dev.mintychochip.domain.repository.PlayerJobStateRepository;
import dev.mintychochip.repository.ConnectionSource;
import dev.mintychochip.repository.DatabaseType;
import dev.mintychochip.repository.NonClosableConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * MySQL (real SQL path, fake job catalog only). Requires live MySQL (see {@link
 * dev.mintychochip.test.MysqlTestSupport}).
 */
class RelationalPlayerJobStateRepositoryTest {

  private static final String TABLE = "job_progressions_test";

  private Connection connection;
  private PlayerJobStateRepository repository;
  private JobRecord miner;
  private JobRecord fisher;
  private JobRecord prospector;

  @BeforeEach
  void setUp() throws Exception {
    dev.mintychochip.test.MysqlTestSupport.assumeAvailable();
    connection = NonClosableConnection.create(dev.mintychochip.test.MysqlTestSupport.open());
    try (Statement st = connection.createStatement()) {
      st.execute("DROP TABLE IF EXISTS " + TABLE);
      st.execute(
          """
          CREATE TABLE job_progressions_test (
            player_id       VARCHAR(191)    NOT NULL,
            job_key         VARCHAR(191)    NOT NULL,
            current_node_key VARCHAR(191)   NOT NULL,
            experience      DECIMAL(38, 10) NOT NULL,
            PRIMARY KEY (player_id, job_key)
          )
          """);
    }

    miner =
        new JobRecord("modularjobs:miner", "Miner", "Mines", 100, "level * 100", Map.of(), null);
    fisher =
        new JobRecord("modularjobs:fisherman", "Fisher", "Fish", 50, "level * 50", Map.of(), null);
    prospector =
        new JobRecord(
            "modularjobs:prospector",
            "Prospector",
            "Finds ore",
            100,
            "unused child curve",
            Map.of(),
            miner.jobKey());

    Map<String, JobRecord> jobs = new ConcurrentHashMap<>();
    jobs.put(miner.jobKey(), miner);
    jobs.put(fisher.jobKey(), fisher);
    jobs.put(prospector.jobKey(), prospector);

    repository =
        RelationalPlayerJobStateRepositoryImpl.create(
            new MemoryJobRepositoryImpl(jobs), new FixedConnectionSource(connection), TABLE);
  }

  @AfterEach
  void tearDown() throws SQLException {
    if (connection != null) {
      try (Statement st = connection.createStatement()) {
        st.execute("DROP TABLE IF EXISTS " + TABLE);
      } catch (SQLException ignored) {
        // best-effort
      }
      if (connection instanceof NonClosableConnection) {
        ((NonClosableConnection) connection).shutdown();
      }
    }
  }

  @Test
  void saveThenLoadReturnsExperience() {
    PlayerJobStateRecord record =
        new PlayerJobStateRecord(
            "player-1", miner.jobKey(), miner.jobKey(), new BigDecimal("1234.50"));
    assertTrue(repository.save(record));

    PlayerJobStateRecord loaded = repository.load("player-1", "modularjobs:miner");
    assertNotNull(loaded);
    assertEquals("player-1", loaded.playerId());
    assertEquals("modularjobs:miner", loaded.jobKey());
    assertEquals("modularjobs:miner", loaded.currentNodeKey());
    assertEquals(0, new BigDecimal("1234.50").compareTo(loaded.experience()));
  }

  @Test
  void saveThenLoadPreservesActiveChildWithinRootTree() {
    repository.save(
        new PlayerJobStateRecord(
            "player-1", miner.jobKey(), prospector.jobKey(), new BigDecimal("321")));

    PlayerJobStateRecord loaded = repository.load("player-1", miner.jobKey());

    assertNotNull(loaded);
    assertEquals(miner.jobKey(), loaded.jobKey());
    assertEquals(prospector.jobKey(), loaded.currentNodeKey());
  }

  @Test
  void saveUpsertsExperience() {
    repository.save(
        new PlayerJobStateRecord(
            "player-1", miner.jobKey(), miner.jobKey(), new BigDecimal("100")));
    repository.save(
        new PlayerJobStateRecord(
            "player-1", miner.jobKey(), miner.jobKey(), new BigDecimal("999")));

    PlayerJobStateRecord loaded = repository.load("player-1", "modularjobs:miner");
    assertNotNull(loaded);
    assertEquals(0, new BigDecimal("999").compareTo(loaded.experience()));
  }

  @Test
  void loadUnknownOrMissingJobReturnsNull() {
    assertNull(repository.load("nobody", "modularjobs:miner"));
    repository.save(
        new PlayerJobStateRecord("player-1", miner.jobKey(), miner.jobKey(), BigDecimal.TEN));
    // job catalog missing → load must not invent a record for unknown job key
    assertNull(repository.load("player-1", "modularjobs:missing"));
  }

  @Test
  void loadAllForPlayerRespectsLimit() {
    repository.save(
        new PlayerJobStateRecord("p1", miner.jobKey(), miner.jobKey(), new BigDecimal("10")));
    repository.save(
        new PlayerJobStateRecord("p1", fisher.jobKey(), fisher.jobKey(), new BigDecimal("20")));

    List<PlayerJobStateRecord> limited = repository.loadAllForPlayer("p1", 1);
    assertEquals(1, limited.size());

    List<PlayerJobStateRecord> all = repository.loadAllForPlayer("p1", 10);
    assertEquals(2, all.size());
  }

  @Test
  void loadAllForJobOrdersByExperienceDescending() {
    repository.save(
        new PlayerJobStateRecord("low", miner.jobKey(), miner.jobKey(), new BigDecimal("10")));
    repository.save(
        new PlayerJobStateRecord("high", miner.jobKey(), miner.jobKey(), new BigDecimal("500")));
    repository.save(
        new PlayerJobStateRecord("mid", miner.jobKey(), miner.jobKey(), new BigDecimal("100")));

    List<PlayerJobStateRecord> top = repository.loadAllForJob("modularjobs:miner", 10);
    assertEquals(3, top.size());
    assertEquals("high", top.get(0).playerId());
    assertEquals(0, new BigDecimal("500").compareTo(top.get(0).experience()));
  }

  @Test
  void deleteRemovesRowAndCache() {
    repository.save(
        new PlayerJobStateRecord("player-1", miner.jobKey(), miner.jobKey(), new BigDecimal("50")));
    assertNotNull(repository.load("player-1", "modularjobs:miner"));

    assertTrue(repository.delete("player-1", "modularjobs:miner"));
    assertNull(repository.load("player-1", "modularjobs:miner"));
    assertFalse(repository.delete("player-1", "modularjobs:miner"));
  }

  /** Reuses a single open JDBC connection (shared NonClosableConnection). */
  private static final class FixedConnectionSource implements ConnectionSource {
    private final Connection connection;

    FixedConnectionSource(@NotNull Connection connection) {
      this.connection = connection;
    }

    @Override
    public @NotNull Connection getConnection() {
      return connection;
    }

    @Override
    public void shutdown() {}

    @Override
    public boolean isClosed() {
      try {
        return connection.isClosed();
      } catch (SQLException e) {
        return true;
      }
    }

    @Override
    public @NotNull DatabaseType getType() {
      return DatabaseType.MYSQL;
    }

    @Override
    public boolean isSetup() {
      return true;
    }
  }
}
