package dev.mintychochip.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.registry.Registry;
import dev.mintychochip.registry.SimpleRegistryImpl;
import dev.mintychochip.repository.ConnectionSource;
import dev.mintychochip.repository.DatabaseType;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.CloseResource")
class UpgradeServiceReadContractTest {

  @Test
  void v2StateIsReadableThroughSkillTreeStateAndImmutable() {
    Connection connection =
        scriptedConnection(
            Map.ofEntries(
                Map.entry("total_skill_points", 5),
                Map.entry("unlocked_nodes", ""),
                Map.entry("node_levels", "{\"far_gather\":2}")),
            Map.of("total_skill_points", 0, "unlocked_nodes", ""));
    UpgradeService service = service(v2Tree(), null, connection);

    assertTrue(service.getSkillTree("minecraft:miner").isPresent());
    SkillTreeState state = service.getSkillTreeState("player", "miner");

    assertEquals(2, state.nodeLevels().get("far_gather"));
    assertThrows(UnsupportedOperationException.class, () -> state.nodeLevels().put("other", 1));
  }

  @Test
  void legacyStateIsReadableThroughPlayerDataAndImmutable() {
    Connection connection =
        scriptedConnection(
            Map.of(
                "total_skill_points",
                2,
                "unlocked_nodes",
                "legacy_node",
                "node_levels",
                "{\"legacy_node\":1}"),
            Map.of(
                "total_skill_points",
                2,
                "unlocked_nodes",
                "legacy_node",
                "node_levels",
                "{\"legacy_node\":1}"));
    UpgradeService service = service(null, legacyTree(), connection);

    PlayerUpgradeData data = service.getPlayerData("player", "miner");

    assertEquals(1, data.nodeLevels().get("legacy_node"));
    assertThrows(UnsupportedOperationException.class, () -> data.nodeLevels().put("other", 1));
  }

  @Test
  void repositoryStateFailurePropagatesInsteadOfReturningEmptyState() {
    SQLException databaseFailure = new SQLException("database unavailable");
    UpgradeService service = service(v2Tree(), null, failingConnection(databaseFailure));

    RuntimeException failure =
        assertThrows(RuntimeException.class, () -> service.getSkillTreeState("player", "miner"));

    assertEquals("Failed to load player skill tree state for player/miner", failure.getMessage());
    assertEquals(databaseFailure, failure.getCause());
  }

  private static UpgradeService service(
      SkillTree skillTree, UpgradeTree upgradeTree, Connection connection) {
    Registry<SkillTree> skillTrees = new SimpleRegistryImpl<>();
    if (skillTree != null) {
      skillTrees.register(skillTree);
    }
    Registry<UpgradeTree> upgradeTrees = new SimpleRegistryImpl<>();
    if (upgradeTree != null) {
      upgradeTrees.register(upgradeTree);
    }
    return new UpgradeServiceImpl(
        upgradeTrees,
        skillTrees,
        new PlayerUpgradeRepository(connectionSource(connection)),
        null,
        new UpgradeEffectApplier(null));
  }

  private static SkillTree v2Tree() {
    SkillNode root = skillNode("root", SkillNodeKind.ROOT, List.of());
    SkillNode farGather =
        skillNode(
            "far_gather",
            SkillNodeKind.SKILL,
            List.of(new NodeLevel(1, List.of()), new NodeLevel(2, List.of())));
    return new SkillTree(
        Key.key("modularjobs", "upgrade_tree/miner"),
        "miner",
        null,
        1,
        "root",
        Map.of("root", root, "far_gather", farGather));
  }

  private static SkillNode skillNode(String key, SkillNodeKind kind, List<NodeLevel> levels) {
    return new SkillNode(
        Key.key("miner", key),
        key,
        null,
        "DIAMOND",
        "DIAMOND",
        null,
        null,
        kind,
        0,
        levels.size(),
        SkillNode.LevelEffectMode.REPLACE,
        levels,
        List.of(),
        Set.of(),
        Set.of(),
        List.of(),
        null,
        List.of(),
        List.of());
  }

  private static UpgradeTree legacyTree() {
    UpgradeNode legacyNode =
        new UpgradeNode(
            Key.key("modularjobs", "legacy_node"),
            "Legacy node",
            null,
            "STONE",
            "STONE",
            null,
            null,
            1,
            Set.of(),
            Set.of(),
            Set.of(),
            List.of(),
            List.of(),
            null,
            List.of(),
            "legacy_node",
            1);
    return new UpgradeTree(
        Key.key("modularjobs", "upgrade_tree/miner"),
        "miner",
        "root",
        1,
        Map.of("legacy_node", legacyNode));
  }

  private static ConnectionSource connectionSource(Connection connection) {
    return new ConnectionSource() {
      @Override
      public void shutdown() {}

      @Override
      public DatabaseType getType() {
        return null;
      }

      @Override
      public boolean isClosed() {
        return false;
      }

      @Override
      public Connection getConnection() {
        return connection;
      }

      @Override
      public boolean isSetup() {
        return true;
      }
    };
  }

  private static Connection failingConnection(SQLException failure) {
    return (Connection)
        Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
              if (method.getName().equals("prepareStatement")) {
                throw failure;
              }
              if (method.getName().equals("close")) {
                return null;
              }
              return defaultValue(method.getReturnType());
            });
  }

  private static Connection scriptedConnection(
      Map<String, Object> stateRow, Map<String, Object> legacyRow) {
    return (Connection)
        Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
              if (method.getName().equals("prepareStatement")) {
                String sql = (String) args[0];
                return scriptedStatement(sql.contains("node_levels") ? stateRow : legacyRow);
              }
              if (method.getName().equals("close")) {
                return null;
              }
              return defaultValue(method.getReturnType());
            });
  }

  private static PreparedStatement scriptedStatement(Map<String, Object> row) {
    return (PreparedStatement)
        Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {PreparedStatement.class},
            (proxy, method, args) -> {
              if (method.getName().equals("executeQuery")) {
                return scriptedResultSet(row);
              }
              if (method.getName().equals("close") || method.getName().startsWith("set")) {
                return null;
              }
              return defaultValue(method.getReturnType());
            });
  }

  private static ResultSet scriptedResultSet(Map<String, Object> row) {
    boolean[] first = {true};
    return (ResultSet)
        Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {ResultSet.class},
            (proxy, method, args) -> {
              if (method.getName().equals("next")) {
                boolean result = first[0];
                first[0] = false;
                return result;
              }
              if (method.getName().equals("getInt")) {
                return ((Number) row.get(args[0])).intValue();
              }
              if (method.getName().equals("getString")) {
                return row.get(args[0]);
              }
              if (method.getName().equals("close")) {
                return null;
              }
              return defaultValue(method.getReturnType());
            });
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) {
      return null;
    }
    if (type == boolean.class) {
      return false;
    }
    if (type == byte.class) {
      return (byte) 0;
    }
    if (type == short.class) {
      return (short) 0;
    }
    if (type == int.class) {
      return 0;
    }
    if (type == long.class) {
      return 0L;
    }
    if (type == float.class) {
      return 0F;
    }
    if (type == double.class) {
      return 0D;
    }
    if (type == char.class) {
      return '\0';
    }
    return null;
  }
}
