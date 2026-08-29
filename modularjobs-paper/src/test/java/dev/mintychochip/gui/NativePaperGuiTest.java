package dev.mintychochip.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.JobTask;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.JoinGate;
import dev.mintychochip.service.PreferencesService;
import dev.mintychochip.upgrade.UpgradeService;
import dev.mintychochip.config.ProgressionLimitsConfig;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class NativePaperGuiTest {
  private static ServerMock server;

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
    JobService jobs = proxy(JobService.class, (method, args) -> {
      if (method.getName().equals("getJobs") || method.getName().equals("getJob")) return method.getName().equals("getJobs") ? List.of(job) : job;
      if (method.getName().equals("getProgressions")) return List.of();
      if (method.getName().equals("getAllTasks")) return Map.of();
      if (method.getName().equals("getProgression")) {
        progressionLookups.incrementAndGet();
        return progression("miner", player.getUniqueId());
      }
      return defaultValue(method.getReturnType());
    });
    UpgradeService upgrades = proxy(UpgradeService.class, (method, args) -> {
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
    PreferencesService preferences = proxy(PreferencesService.class, (method, args) -> {
      if (method.getName().equals("getEntriesPerPage")) return 2;
      return defaultValue(method.getReturnType());
    });
    JobInfoGui gui = new JobInfoGui(new PaperUiHost(), preferences);
    Map<ActionType, List<JobTask>> tasks = new LinkedHashMapBuilder<ActionType, List<JobTask>>()
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
    PreferencesService preferences = proxy(PreferencesService.class, (method, args) -> {
      if (method.getName().equals("getEntriesPerPage")) return 1;
      return defaultValue(method.getReturnType());
    });
    PaperUiHost host = new PaperUiHost();
    JobInfoGui gui = new JobInfoGui(host, preferences);
    Map<ActionType, List<JobTask>> tasks = new LinkedHashMapBuilder<ActionType, List<JobTask>>()
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
    List<JobProgression> progressions = new ArrayList<>();
    for (int i = 0; i < 6; i++) progressions.add(progression("job" + i, target.getUniqueId()));
    PaperUiHost host = new PaperUiHost();
    StatsGui gui = new StatsGui(host);

    gui.open(viewer, target, progressions, 999);
    Inventory last = viewer.getOpenInventory().getTopInventory();
    assertEquals(54, last.getSize());
    assertEquals(Material.BOOK, last.getItem(4).getType());
    assertEquals(Material.EMERALD, last.getItem(19).getType());
    assertEquals(Material.ARROW, last.getItem(45).getType());
    assertEquals(Material.GRAY_STAINED_GLASS_PANE, last.getItem(53).getType());

    gui.open(viewer, target, progressions, -4);
    Inventory first = viewer.getOpenInventory().getTopInventory();
    assertEquals(Material.GRAY_STAINED_GLASS_PANE, first.getItem(45).getType());
    assertEquals(Material.ARROW, first.getItem(53).getType());

    host.close(viewer);
    assertEquals(Component.text("Job Statistics"), gui.buildView(viewer.getUniqueId()).title());
  }

  private static InventoryClickEvent click(org.bukkit.inventory.InventoryView view, int rawSlot) {
    return new InventoryClickEvent(view, SlotType.CONTAINER, rawSlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
  }

  private static Job job(String key) {
    Key jobKey = Key.key("modularjobs", key);
    return new Job() {
      private final LevelingCurve curve = parameters -> BigDecimal.valueOf(parameters.level() * 100L);

      @Override public Component displayName() { return Component.text(key.substring(0, 1).toUpperCase() + key.substring(1)); }
      @Override public String getPlainName() { return key.substring(0, 1).toUpperCase() + key.substring(1); }
      @Override public Component description() { return Component.text("A useful job"); }
      @Override public LevelingCurve levelingCurve() { return curve; }
      @Override public Map<Key, dev.mintychochip.PayableCurve> payableCurves() { return Map.of(); }
      @Override public int maxLevel() { return 10; }
      @Override public int upgradeLevel() { return 0; }
      @Override public Map<Integer, List<String>> perkUnlocks() { return Map.of(); }
      @Override public Key key() { return jobKey; }
    };
  }

  private static JobProgression progression(String key, UUID playerId) {
    Job job = job(key);
    return proxy(JobProgression.class, (method, args) -> {
      if (method.getName().equals("job")) return job;
      if (method.getName().equals("playerId")) return playerId;
      if (method.getName().equals("experience")) return BigDecimal.valueOf(25);
      if (method.getName().equals("level")) return 1;
      if (method.getName().equals("experienceForLevel")) return BigDecimal.valueOf(100);
      return defaultValue(method.getReturnType());
    });
  }

  private static ActionType action(String key) {
    Key actionKey = Key.key("modularjobs", key);
    return new ActionType() {
      @Override public String name() { return key.toUpperCase(); }
      @Override public Key key() { return actionKey; }
    };
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, CheckedHandler handler) {
    InvocationHandler invocation = (object, method, args) -> handler.invoke(method, args);
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, invocation);
  }

  private static Object defaultValue(Class<?> type) {
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
    Object invoke(Method method, Object[] args) throws Throwable;
  }

  private static final class LinkedHashMapBuilder<K, V> {
    private final Map<K, V> map = new java.util.LinkedHashMap<>();
    LinkedHashMapBuilder<K, V> put(K key, V value) { map.put(key, value); return this; }
    Map<K, V> build() { return map; }
  }
}
