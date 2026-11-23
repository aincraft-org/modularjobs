package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.boost.BoostDataCodec;
import dev.mintychochip.boost.BoostFactoryImpl;
import dev.mintychochip.boost.MultiplicativeBoostImpl;
import dev.mintychochip.boost.RuledBoostSourceImpl;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.boost.RuledBoostSource.Rule;
import dev.mintychochip.container.boost.TimedBoostDataService.ActiveBoostData;
import dev.mintychochip.databag.gson.GsonConditionSerializer;
import dev.mintychochip.domain.RelationalJobTaskRepositoryImpl;
import dev.mintychochip.domain.model.JobTaskRecord;
import dev.mintychochip.domain.model.PayableRecord;
import dev.mintychochip.test.MockBukkitSupport;
import dev.mintychochip.upgrade.PlayerUpgradeDataImpl;
import dev.mintychochip.upgrade.PlayerUpgradeRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Exercises the production connection and repository paths against a real SQLite file. */
class SqlitePersistenceIntegrationTest {

  private static final String PLAYER_ID = "sqlite-player";
  private static final String JOB_KEY = "modularjobs:miner";

  @TempDir Path tempDir;

  private Plugin plugin;
  private ConnectionSource source;
  private String jdbcUrl;

  @BeforeEach
  void setUp() throws Exception {
    MockBukkitSupport.mockServer();
    plugin = MockBukkit.createMockPlugin("ModularJobsSqliteTest");
    jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("modularjobs.db").toAbsolutePath();
    provision(jdbcUrl);
    source = new ConnectionSourceFactory(plugin, sqliteConfig(jdbcUrl)).create();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (source != null && !source.isClosed()) {
      source.shutdown();
    }
    MockBukkitSupport.unmockServer();
  }

  @Test
  void factoryOpensProvisionedSchemaWithForeignKeysEnabled() throws Exception {
    assertEquals(DatabaseType.SQLITE, source.getType());
    try (Connection connection = source.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("PRAGMA foreign_keys")) {
      boolean hasPragma = result.next();
      assertTrue(hasPragma);
      assertEquals(1, result.getInt(1));
      SchemaPresence.requireTables(connection, DatabaseType.SQLITE, SchemaPresence.REQUIRED_TABLES);
      SchemaPresence.requireColumns(
          connection, DatabaseType.SQLITE, SchemaPresence.REQUIRED_COLUMNS);
    }
  }

  @Test
  void repositoriesRoundTripAndUpsertWithSqliteDialect() throws Exception {
    PlayerUpgradeRepository upgrades = new PlayerUpgradeRepository(source);
    upgrades.savePlayerData(new PlayerUpgradeDataImpl(PLAYER_ID, JOB_KEY, 3, Set.of("efficiency")));
    upgrades.savePlayerData(new PlayerUpgradeDataImpl(PLAYER_ID, JOB_KEY, 7, Set.of("fortune")));

    PlayerUpgradeDataImpl loadedUpgrade = upgrades.loadPlayerData(PLAYER_ID, JOB_KEY);
    assertNotNull(loadedUpgrade);
    assertEquals(7, loadedUpgrade.totalSkillPoints());
    assertEquals(Set.of("fortune"), loadedUpgrade.unlockedNodes());

    BigDecimal amount = new BigDecimal("1234.5678901234");
    JobTaskRecord task =
        new JobTaskRecord(
            JOB_KEY,
            "modularjobs:block_break",
            "minecraft:diamond_ore",
            List.of(new PayableRecord("modularjobs:economy", amount, "TOKENS", "✦")));
    RelationalJobTaskRepositoryImpl tasks = new RelationalJobTaskRepositoryImpl(source);
    assertTrue(tasks.save(task));
    JobTaskRecord loadedTask = tasks.load(task.nodeKey(), task.actionTypeKey(), task.contextKey());
    assertNotNull(loadedTask);
    assertEquals(1, loadedTask.payables().size());
    assertEquals(0, amount.compareTo(loadedTask.payables().getFirst().amount()));
    assertEquals("✦", loadedTask.payables().getFirst().currencySymbol());

    BoostDataCodec codec =
        new BoostDataCodec(GsonConditionSerializer.gson(), BoostFactoryImpl.INSTANCE);
    RelationalTimedBoostRepositoryImpl boosts =
        RelationalTimedBoostRepositoryImpl.createSynchronous(source, codec);
    BoostSource boostSource =
        new RuledBoostSourceImpl(
            List.of(
                new Rule(
                    BoostFactoryImpl.INSTANCE.sneaking(false),
                    1,
                    new MultiplicativeBoostImpl(new BigDecimal("2.0")))),
            Key.key("modularjobs", "sqlite_test"),
            "test");
    ActiveBoostData original =
        new ActiveBoostData(
            PLAYER_ID,
            "modularjobs:sqlite_test",
            Instant.ofEpochMilli(1_000),
            Duration.ofMinutes(5),
            boostSource);
    ActiveBoostData updated =
        new ActiveBoostData(
            PLAYER_ID,
            "modularjobs:sqlite_test",
            Instant.ofEpochMilli(2_000),
            Duration.ofMinutes(10),
            boostSource);
    boosts.addBoost(original);
    boosts.addBoost(updated);
    ActiveBoostData loadedBoost = boosts.findBoost(PLAYER_ID, "modularjobs:sqlite_test");
    assertNotNull(loadedBoost);
    assertEquals(updated.started(), loadedBoost.started());
    assertEquals(updated.duration(), loadedBoost.duration());
  }

  @Test
  void progressionUpsertResourcePreservesExactDecimal() throws Exception {
    String save =
        SqlStatements.load(DatabaseType.SQLITE, "job_progression/save.sql")
            .replace("{table}", "job_progression");
    try (Connection connection = source.getConnection()) {
      saveProgression(connection, save, "modularjobs:miner", "10.0000000001");
      saveProgression(connection, save, "modularjobs:prospector", "99.1234567890");
      try (PreparedStatement statement =
          connection.prepareStatement(
              "SELECT current_node_key, experience FROM job_progression"
                  + " WHERE player_id = ? AND job_key = ?")) {
        statement.setString(1, PLAYER_ID);
        statement.setString(2, JOB_KEY);
        try (ResultSet result = statement.executeQuery()) {
          boolean hasRow = result.next();
          assertTrue(hasRow);
          assertEquals("modularjobs:prospector", result.getString("current_node_key"));
          assertEquals(
              0, new BigDecimal("99.1234567890").compareTo(result.getBigDecimal("experience")));
          boolean hasExtraRow = result.next();
          assertFalse(hasExtraRow);
        }
      }
    }
  }

  @Test
  void factoryRejectsUnprovisionedSqliteFile() {
    String emptyUrl = "jdbc:sqlite:" + tempDir.resolve("empty.db").toAbsolutePath();
    SchemaPresence.SchemaMissingException failure =
        assertThrows(
            SchemaPresence.SchemaMissingException.class,
            () -> new ConnectionSourceFactory(plugin, sqliteConfig(emptyUrl)).create());
    assertEquals(DatabaseType.SQLITE, failure.getType());
    assertTrue(failure.getMessage().contains("sql/sqlite.sql"), failure.getMessage());
  }

  private static void provision(String url) throws Exception {
    Class.forName("org.sqlite.JDBC");
    try (Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement()) {
      for (String sql : DatabaseType.SQLITE.getSqlTables()) {
        statement.execute(sql);
      }
    }
  }

  private static YamlConfiguration sqliteConfig(String url) {
    YamlConfiguration config = new YamlConfiguration();
    config.set("type", "sqlite");
    config.set("jdbc-url", url);
    return config;
  }

  private static void saveProgression(
      Connection connection, String sql, String currentNode, String experience)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, PLAYER_ID);
      statement.setString(2, JOB_KEY);
      statement.setString(3, currentNode);
      statement.setBigDecimal(4, new BigDecimal(experience));
      assertTrue(statement.executeUpdate() > 0);
    }
  }
}
