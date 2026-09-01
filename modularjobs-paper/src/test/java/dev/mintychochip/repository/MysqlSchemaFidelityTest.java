package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.mintychochip.domain.RelationalJobTaskRepositoryImpl;
import dev.mintychochip.domain.model.JobTaskRecord;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Applies the shipped {@code sql/mysql.sql} DDL and verifies write→read fidelity for job-task /
 * payable / progression fields against a real MySQL instance.
 *
 * <p>Connection defaults to {@code jdbc:mysql://localhost:13306/modularjobs} (user/password {@code
 * test}). Override with env {@code MODULARJOBS_TEST_MYSQL_URL}, {@code
 * MODULARJOBS_TEST_MYSQL_USER}, {@code MODULARJOBS_TEST_MYSQL_PASSWORD}.
 */
class MysqlSchemaFidelityTest {

  private static final String DEFAULT_URL = "jdbc:mysql://localhost:13306/modularjobs";
  private static final String DEFAULT_USER = "test";
  private static final String DEFAULT_PASSWORD = "test";

  private static String jdbcUrl;
  private static String user;
  private static String password;
  private static boolean mysqlAvailable;

  private Connection connection;

  @BeforeAll
  static void detectMysql() {
    jdbcUrl = envOr("MODULARJOBS_TEST_MYSQL_URL", DEFAULT_URL);
    user = envOr("MODULARJOBS_TEST_MYSQL_USER", DEFAULT_USER);
    password = envOr("MODULARJOBS_TEST_MYSQL_PASSWORD", DEFAULT_PASSWORD);
    try {
      Class.forName(DatabaseType.MYSQL.getClassName());
      try (Connection c = DriverManager.getConnection(jdbcUrl, user, password);
          Statement st = c.createStatement()) {
        st.execute("SELECT 1");
        mysqlAvailable = true;
      }
    } catch (ClassNotFoundException | SQLException e) {
      mysqlAvailable = false;
    }
  }

  @BeforeEach
  void setUp() throws SQLException {
    // Connection opened only for live round-trip tests (see requireMysql()).
  }

  @AfterEach
  void tearDown() throws SQLException {
    if (connection != null && !connection.isClosed()) {
      cleanTables(connection);
      connection.close();
    }
  }

  private void requireMysql() throws SQLException {
    assumeTrue(mysqlAvailable, "MySQL must be reachable at " + jdbcUrl);
    if (connection == null || connection.isClosed()) {
      connection = DriverManager.getConnection(jdbcUrl, user, password);
      connection.setAutoCommit(true);
      applyShippedSchema(connection);
      cleanTables(connection);
    }
  }

  @Test
  void mysqlDriverClassIsConfigured() {
    assertEquals("com.mysql.cj.jdbc.Driver", DatabaseType.MYSQL.getClassName());
    assertEquals("mysql", DatabaseType.MYSQL.getIdentifier());
    assertEquals(DatabaseType.MYSQL, DatabaseType.fromIdentifier("mysql"));
  }

  @Test
  void shippedMysqlDdlUsesMysqlTypes() {
    String[] statements = DatabaseType.MYSQL.getSqlTables();
    assertTrue(statements.length > 0, "mysql.sql must produce statements");
    String joined = String.join("\n", statements).toUpperCase();
    assertFalse(joined.contains("SERIAL"), "MySQL DDL must not use SERIAL");
    assertTrue(joined.contains("AUTO_INCREMENT"), "task_id must use AUTO_INCREMENT");
    assertTrue(joined.contains("DECIMAL"), "amounts/experience must use DECIMAL");
    assertTrue(joined.contains("ENGINE=INNODB"), "tables must use InnoDB");
  }

  @Test
  void currentNodeMigrationHandlesLegacyAndCurrentSchemasAndIsIdempotent() throws Exception {
    requireMysql();
    String suffix = Long.toUnsignedString(System.nanoTime());
    String currentTable = "mj_current_state_" + suffix;
    String legacyTable = "mj_legacy_state_" + suffix;
    try {
      try (Statement st = connection.createStatement()) {
        st.execute(
            "CREATE TABLE "
                + currentTable
                + " (player_id VARCHAR(191) NOT NULL, job_key VARCHAR(191) NOT NULL,"
                + " current_node_key VARCHAR(191) NOT NULL, experience DECIMAL(38, 10) NOT NULL,"
                + " PRIMARY KEY (player_id, job_key))");
        st.execute(
            "INSERT INTO "
                + currentTable
                + " VALUES ('current-player', 'modularjobs:miner',"
                + " 'modularjobs:prospector', 25)");
        st.execute(
            "CREATE TABLE "
                + legacyTable
                + " (player_id VARCHAR(191) NOT NULL, job_key VARCHAR(191) NOT NULL,"
                + " experience DECIMAL(38, 10) NOT NULL,"
                + " PRIMARY KEY (player_id, job_key))");
        st.execute(
            "INSERT INTO " + legacyTable + " VALUES ('legacy-player', 'modularjobs:miner', 10)");
      }

      executeCurrentNodeMigration(currentTable, legacyTable);
      executeCurrentNodeMigration(currentTable, legacyTable);

      assertStateRow(currentTable, "current-player", "modularjobs:miner", "modularjobs:prospector");
      assertStateRow(legacyTable, "legacy-player", "modularjobs:miner", "modularjobs:miner");
      assertCurrentNodeRequired(currentTable);
      assertCurrentNodeRequired(legacyTable);
    } finally {
      try (Statement st = connection.createStatement()) {
        st.execute("DROP TABLE IF EXISTS " + currentTable);
        st.execute("DROP TABLE IF EXISTS " + legacyTable);
      }
    }
  }

  @Test
  void jobTaskPayableRoundTripPreservesCurrencyMetadataAndPreciseAmount() throws SQLException {
    requireMysql();
    int taskId;
    try (PreparedStatement ps =
        connection.prepareStatement(
            "INSERT INTO job_tasks (job_key, action_type_key, context_key) VALUES (?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, "modularjobs:miner");
      ps.setString(2, "modularjobs:block_break");
      ps.setString(3, "minecraft:diamond_ore");
      assertEquals(1, ps.executeUpdate());
      try (ResultSet keys = ps.getGeneratedKeys()) {
        boolean hasKey = keys.next();
        assertTrue(hasKey);
        taskId = keys.getInt(1);
      }
    }
    assertTrue(taskId > 0);

    BigDecimal amount = new BigDecimal("1234.5678901234");
    try (PreparedStatement ps =
        connection.prepareStatement(
            "INSERT INTO job_task_payables (job_task_id, payable_type_key, amount,"
                + " currency_identifier, currency_symbol) VALUES (?, ?, ?, ?, ?)")) {
      ps.setInt(1, taskId);
      ps.setString(2, "modularjobs:economy");
      ps.setBigDecimal(3, amount);
      ps.setString(4, "TOKENS");
      ps.setString(5, "✦");
      assertEquals(1, ps.executeUpdate());
    }

    try (PreparedStatement ps =
        connection.prepareStatement(
            """
            SELECT t.job_key, t.action_type_key, t.context_key,
                   p.payable_type_key, p.amount, p.currency_identifier, p.currency_symbol
            FROM job_tasks t
            JOIN job_task_payables p ON p.job_task_id = t.task_id
            WHERE t.task_id = ?
            """)) {
      ps.setInt(1, taskId);
      try (ResultSet rs = ps.executeQuery()) {
        boolean hasRow = rs.next();
        assertTrue(hasRow);
        assertEquals("modularjobs:miner", rs.getString("job_key"));
        assertEquals("modularjobs:block_break", rs.getString("action_type_key"));
        assertEquals("minecraft:diamond_ore", rs.getString("context_key"));
        assertEquals("modularjobs:economy", rs.getString("payable_type_key"));
        BigDecimal readAmount = rs.getBigDecimal("amount");
        assertNotNull(readAmount);
        assertEquals(
            0,
            amount.compareTo(readAmount),
            "DECIMAL amount must round-trip without loss; expected "
                + amount
                + " got "
                + readAmount);
        assertEquals("TOKENS", rs.getString("currency_identifier"));
        assertEquals("✦", rs.getString("currency_symbol"));
        boolean hasExtraRow = rs.next();
        assertFalse(hasExtraRow);
      }
    }
  }

  @Test
  void taskRepositoryLoadsExplicitTaskWithNoPayables() throws SQLException {
    requireMysql();
    try (Connection shared = NonClosableConnection.create(connection)) {
      ConnectionSource source = new FixedConnectionSource(shared);
      JobTaskRecord emptyOverride =
          new JobTaskRecord(
              "modularjobs:prospector",
              "modularjobs:block_break",
              "minecraft:diamond_ore",
              List.of());

      assertTrue(new RelationalJobTaskRepositoryImpl(source).save(emptyOverride));
      JobTaskRecord restored =
          new RelationalJobTaskRepositoryImpl(source)
              .load(
                  emptyOverride.nodeKey(),
                  emptyOverride.actionTypeKey(),
                  emptyOverride.contextKey());

      assertNotNull(restored);
      assertEquals(emptyOverride, restored);
      assertTrue(restored.payables().isEmpty());
    }
  }

  @Test
  void payableRecordsCompositeKeyAndCurrencyRoundTrip() throws SQLException {
    requireMysql();
    BigDecimal amount = new BigDecimal("99.1250000000");
    try (PreparedStatement ps =
        connection.prepareStatement(
            """
            INSERT INTO payable_records
              (job_key, action_type_key, context_key, payable_type_key, amount, currency)
            VALUES (?, ?, ?, ?, ?, ?)
            """)) {
      ps.setString(1, "modularjobs:fisherman");
      ps.setString(2, "modularjobs:fish");
      ps.setString(3, "minecraft:cod");
      ps.setString(4, "modularjobs:economy");
      ps.setBigDecimal(5, amount);
      ps.setString(6, "test:default");
      assertEquals(1, ps.executeUpdate());
    }

    try (PreparedStatement ps =
        connection.prepareStatement(
            """
            SELECT amount, currency FROM payable_records
            WHERE job_key = ? AND action_type_key = ? AND context_key = ? AND payable_type_key = ?
            """)) {
      ps.setString(1, "modularjobs:fisherman");
      ps.setString(2, "modularjobs:fish");
      ps.setString(3, "minecraft:cod");
      ps.setString(4, "modularjobs:economy");
      try (ResultSet rs = ps.executeQuery()) {
        boolean hasRow = rs.next();
        assertTrue(hasRow);
        assertEquals(0, amount.compareTo(rs.getBigDecimal("amount")));
        assertEquals("test:default", rs.getString("currency"));
      }
    }
  }

  @Test
  void playerJobStateIdentityAndExperienceRoundTrip() throws SQLException {
    requireMysql();
    BigDecimal exp = new BigDecimal("5000.2500000000");
    try (PreparedStatement ps =
        connection.prepareStatement(
            "INSERT INTO job_progression"
                + " (player_id, job_key, current_node_key, experience) VALUES (?, ?, ?, ?)")) {
      ps.setString(1, "11111111-2222-3333-4444-555555555555");
      ps.setString(2, "modularjobs:miner");
      ps.setString(3, "modularjobs:prospector");
      ps.setBigDecimal(4, exp);
      assertEquals(1, ps.executeUpdate());
    }

    try (PreparedStatement ps =
        connection.prepareStatement(
            "SELECT current_node_key, experience FROM job_progression"
                + " WHERE player_id = ? AND job_key = ?")) {
      ps.setString(1, "11111111-2222-3333-4444-555555555555");
      ps.setString(2, "modularjobs:miner");
      try (ResultSet rs = ps.executeQuery()) {
        boolean hasRow = rs.next();
        assertTrue(hasRow);
        assertEquals("modularjobs:prospector", rs.getString("current_node_key"));
        assertEquals(0, exp.compareTo(rs.getBigDecimal("experience")));
      }
    }
  }

  private void executeCurrentNodeMigration(
      @NotNull String currentTable, @NotNull String legacyTable) throws Exception {
    String migration =
        Files.readString(locateCurrentNodeMigration())
            .replace("archive_job_progression", legacyTable)
            .replace("job_progression", currentTable);
    try (Statement st = connection.createStatement()) {
      for (String statement : DatabaseType.splitStatements(migration)) {
        st.execute(statement);
      }
    }
  }

  private static @NotNull Path locateCurrentNodeMigration() {
    Path directory = Path.of("").toAbsolutePath();
    while (directory != null) {
      Path candidate = directory.resolve("scripts/migrate-add-current-node-key.sql");
      if (Files.isRegularFile(candidate)) {
        return candidate;
      }
      directory = directory.getParent();
    }
    return fail("Cannot locate scripts/migrate-add-current-node-key.sql");
  }

  private void assertStateRow(
      @NotNull String table,
      @NotNull String playerId,
      @NotNull String expectedJobKey,
      @NotNull String expectedNodeKey)
      throws SQLException {
    try (PreparedStatement ps =
        connection.prepareStatement(
            "SELECT job_key, current_node_key FROM " + table + " WHERE player_id = ?")) {
      ps.setString(1, playerId);
      try (ResultSet rs = ps.executeQuery()) {
        boolean found = rs.next();
        assertTrue(found);
        assertEquals(expectedJobKey, rs.getString("job_key"));
        assertEquals(expectedNodeKey, rs.getString("current_node_key"));
        boolean hasExtraRow = rs.next();
        assertFalse(hasExtraRow);
      }
    }
  }

  private void assertCurrentNodeRequired(@NotNull String table) throws SQLException {
    try (ResultSet columns =
        connection
            .getMetaData()
            .getColumns(connection.getCatalog(), null, table, "current_node_key")) {
      boolean found = columns.next();
      assertTrue(found);
      assertEquals(DatabaseMetaData.columnNoNulls, columns.getInt("NULLABLE"));
    }
  }

  private static void applyShippedSchema(@NotNull Connection connection) throws SQLException {
    String[] tables = DatabaseType.MYSQL.getSqlTables();
    assertNotNull(tables);
    assertTrue(tables.length > 0);
    try (Statement st = connection.createStatement()) {
      for (String sql : tables) {
        st.execute(sql);
      }
    }
  }

  private static void cleanTables(@NotNull Connection connection) throws SQLException {
    try (Statement st = connection.createStatement()) {
      // order matters for FKs
      for (String table :
          Arrays.asList(
              "job_task_payables",
              "job_tasks",
              "payable_records",
              "job_progression",
              "archive_job_progression",
              "time_boosts",
              "time_boost_identity",
              "player_upgrades",
              "editor_sessions")) {
        st.execute("DELETE FROM " + table);
      }
    }
  }

  /** Reuses one physical connection while repository try-with-resources closes wrappers. */
  private record FixedConnectionSource(@NotNull Connection connection) implements ConnectionSource {

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

  private static @NotNull String envOr(@NotNull String key, @NotNull String defaultValue) {
    String v = System.getenv(key);
    return v == null || v.isBlank() ? defaultValue : v;
  }
}
