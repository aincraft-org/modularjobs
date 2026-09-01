package dev.mintychochip.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.JobTask;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.config.ProgressionLimitsConfig;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.PayableRenderer;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.JoinGate;
import dev.mintychochip.service.PreferencesService;
import dev.mintychochip.upgrade.UpgradeService;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class NativePaperGuiTest {
  private static ServerMock server;
  private static final PayableRenderer UNUSED_RENDERER = ignored -> Component.empty();

  @BeforeAll
  static void setUp() {
    server = dev.mintychochip.test.MockBukkitSupport.mockServer();
  }

  @AfterAll
  static void tearDown() {
    dev.mintychochip.test.MockBukkitSupport.unmockServer();
  }

  @Test
  void browseOpensNativeInventoryFillsPanesAndDispatchesJoinCallback() {
    PlayerMock player = server.addPlayer("browse-" + UUID.randomUUID());
    Job job = job("miner");
    AtomicInteger progressionLookups = new AtomicInteger();
    JobService jobs =
        proxy(
            JobService.class,
            (method, args) -> {
              if (method.getName().equals("getJobs") || method.getName().equals("getJob"))
                return method.getName().equals("getJobs") ? List.of(job) : job;
              if (method.getName().equals("getPlayerJobStates")) return List.of();
              if (method.getName().equals("getAllTasks")) return Map.of();
              if (method.getName().equals("getPlayerJobState")) {
                progressionLookups.incrementAndGet();
                return state("miner", player.getUniqueId());
              }
              return defaultValue(method.getReturnType());
            });
    UpgradeService upgrades =
        proxy(
            UpgradeService.class,
            (method, args) -> {
              if (method.getName().equals("getTree")) return java.util.Optional.empty();
              return defaultValue(method.getReturnType());
            });
    JoinGate gate = new JoinGate(new ProgressionLimitsConfig(0, List.of(), false), Set.of());
    PaperUiHost host = new PaperUiHost();
    JobBrowseGui gui = new JobBrowseGui(host, jobs, upgrades, gate);

    gui.open(player);

    Inventory top = player.getOpenInventory().getTopInventory();
    assertEquals(54, top.getSize());
    assertEquals(Material.GRAY_STAINED_GLASS_PANE, top.getItem(0).getType());
    assertEquals(Material.BOOK, top.getItem(10).getType());
    assertNotNull(top.getItem(53));

    PaperUiHost.ScreenView browseView = gui.buildView(player);
    assertNotNull(browseView.actions().get(10));
    browseView.actions().get(10).execute(player, click(player.getOpenInventory(), 10));
    assertEquals(1, progressionLookups.get());
  }

  @Test
  void infoRejectsInvalidPagesAndOnlyRegistersExistingNavigationActions() {
    PlayerMock player = server.addPlayer("info-" + UUID.randomUUID());
    PreferencesService preferences =
        proxy(
            PreferencesService.class,
            (method, args) -> {
              if (method.getName().equals("getEntriesPerPage")) return 2;
              return defaultValue(method.getReturnType());
            });
    JobInfoGui gui = new JobInfoGui(new PaperUiHost(), preferences, UNUSED_RENDERER);
    Map<ActionType, List<JobTask>> tasks =
        new LinkedHashMapBuilder<ActionType, List<JobTask>>()
            .put(action("mine"), List.of())
            .put(action("craft"), List.of())
            .put(action("fish"), List.of())
            .build();
    Job job = job("miner");

    assertFalse(gui.open(player, job, tasks, 0));
    assertFalse(gui.open(player, job, tasks, 3));
    assertTrue(gui.open(player, job, tasks, 1));
    Inventory first = player.getOpenInventory().getTopInventory();
    assertEquals(54, first.getSize());
    assertEquals(Material.BOOK, first.getItem(4).getType());
    assertEquals(Material.GRAY_STAINED_GLASS_PANE, first.getItem(45).getType());
    assertEquals(Material.ARROW, first.getItem(53).getType());

    assertTrue(gui.open(player, job, tasks, 2));
    Inventory last = player.getOpenInventory().getTopInventory();
    assertEquals(Material.ARROW, last.getItem(45).getType());
    assertEquals(Material.GRAY_STAINED_GLASS_PANE, last.getItem(53).getType());
  }

  @Test
  void infoNavigationDispatchesOnlyWhenPageExistsAndCloseClearsSession() {
    PlayerMock player = server.addPlayer("info-nav-" + UUID.randomUUID());
    PreferencesService preferences =
        proxy(
            PreferencesService.class,
            (method, args) -> {
              if (method.getName().equals("getEntriesPerPage")) return 1;
              return defaultValue(method.getReturnType());
            });
    PaperUiHost host = new PaperUiHost();
    JobInfoGui gui = new JobInfoGui(host, preferences, UNUSED_RENDERER);
    Map<ActionType, List<JobTask>> tasks =
        new LinkedHashMapBuilder<ActionType, List<JobTask>>()
            .put(action("mine"), List.of())
            .put(action("craft"), List.of())
            .build();
    assertTrue(gui.open(player, job("miner"), tasks, 1));

    host.onInventoryClick(click(player.getOpenInventory(), 53));
    assertEquals("Info: Miner (2/2)", player.getOpenInventory().getTitle());
    host.onInventoryClick(click(player.getOpenInventory(), 53));
    assertEquals("Info: Miner (2/2)", player.getOpenInventory().getTitle());

    host.close(player);
    assertEquals(Component.text("Job Info"), gui.buildView(player.getUniqueId()).title());
  }

  @Test
  void statsClampsPagesRendersNativeLayoutAndOnlyRegistersNavigationActions() {
    PlayerMock viewer = server.addPlayer("stats-" + UUID.randomUUID());
    OfflinePlayer target = server.getOfflinePlayer("target");
    List<PlayerJobState> states = new ArrayList<>();
    for (int i = 0; i < 6; i++) states.add(state("job" + i, target.getUniqueId()));
    PaperUiHost host = new PaperUiHost();
    StatsGui gui = new StatsGui(host);

    gui.open(viewer, target, states, 999);
    Inventory last = viewer.getOpenInventory().getTopInventory();
    assertEquals(54, last.getSize());
    assertEquals(Material.BOOK, last.getItem(4).getType());
    assertEquals(Material.EMERALD, last.getItem(19).getType());
    assertEquals(Material.ARROW, last.getItem(45).getType());
    assertEquals(Material.GRAY_STAINED_GLASS_PANE, last.getItem(53).getType());

    gui.open(viewer, target, states, -4);
    Inventory first = viewer.getOpenInventory().getTopInventory();
    assertEquals(Material.GRAY_STAINED_GLASS_PANE, first.getItem(45).getType());
    assertEquals(Material.ARROW, first.getItem(53).getType());

    host.close(viewer);
    assertEquals(Component.text("Job Statistics"), gui.buildView(viewer.getUniqueId()).title());
  }

  private static @NotNull InventoryClickEvent click(
      @NotNull org.bukkit.inventory.InventoryView view, int rawSlot) {
    return new InventoryClickEvent(
        view, SlotType.CONTAINER, rawSlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
  }

  private static @NotNull Job job(@NotNull String key) {
    JobKey jobKey = new JobKey(Key.key("modularjobs", key));
    JobNodeKey nodeKey = new JobNodeKey(jobKey.key());
    Component displayName = Component.text(key.substring(0, 1).toUpperCase() + key.substring(1));
    JobNode root =
        new JobNode() {
          @Override
          public @NotNull JobKey jobKey() {
            return jobKey;
          }

          @Override
          public @NotNull JobNodeKey nodeKey() {
            return nodeKey;
          }

          @Override
          public JobNodeKey parentKey() {
            return null;
          }

          @Override
          public @NotNull Component displayName() {
            return displayName;
          }

          @Override
          public @NotNull String getPlainName() {
            return key.substring(0, 1).toUpperCase() + key.substring(1);
          }

          @Override
          public @NotNull Component description() {
            return Component.text("A useful job");
          }
        };
    return new Job() {
      private final LevelingCurve curve =
          parameters -> BigDecimal.valueOf(parameters.level() * 100L);

      @Override
      public @NotNull JobKey jobKey() {
        return jobKey;
      }

      @Override
      public @NotNull JobNode rootNode() {
        return root;
      }

      @Override
      public @NotNull Map<JobNodeKey, JobNode> nodes() {
        return Map.of(nodeKey, root);
      }

      @Override
      public @NotNull LevelingCurve levelingCurve() {
        return curve;
      }

      @Override
      public @NotNull Map<Key, dev.mintychochip.PayableCurve> payableCurves() {
        return Map.of();
      }

      @Override
      public int maxLevel() {
        return 10;
      }
    };
  }

  private static @NotNull PlayerJobState state(@NotNull String key, @NotNull UUID playerId) {
    Job job = job(key);
    return proxy(
        PlayerJobState.class,
        (method, args) -> {
          if (method.getName().equals("job")) return job;
          if (method.getName().equals("playerId")) return playerId;
          if (method.getName().equals("experience")) return BigDecimal.valueOf(25);
          if (method.getName().equals("level")) return 1;
          if (method.getName().equals("experienceForLevel")) return BigDecimal.valueOf(100);
          return defaultValue(method.getReturnType());
        });
  }

  private static @NotNull ActionType action(@NotNull String key) {
    Key actionKey = Key.key("modularjobs", key);
    return new ActionType() {
      @Override
      public @NotNull String name() {
        return key.toUpperCase();
      }

      @Override
      public @NotNull Key key() {
        return actionKey;
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static <T> @NotNull T proxy(@NotNull Class<T> type, @NotNull CheckedHandler handler) {
    InvocationHandler invocation = (object, method, args) -> handler.invoke(method, args);
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, invocation);
  }

  private static @Nullable Object defaultValue(@NotNull Class<?> type) {
    if (!type.isPrimitive()) return null;
    if (type == boolean.class) return false;
    if (type == byte.class) return (byte) 0;
    if (type == short.class) return (short) 0;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == float.class) return 0F;
    if (type == double.class) return 0D;
    if (type == char.class) return '\0';
    return null;
  }

  @FunctionalInterface
  private interface CheckedHandler {
    @Nullable
    Object invoke(@NotNull Method method, @Nullable Object[] args) throws Throwable;
  }

  private static final class LinkedHashMapBuilder<K, V> {
    private final Map<K, V> map = new java.util.LinkedHashMap<>();

    @NotNull
    LinkedHashMapBuilder<K, V> put(@NotNull K key, @NotNull V value) {
      map.put(key, value);
      return this;
    }

    @NotNull
    Map<K, V> build() {
      return map;
    }
  }
}
