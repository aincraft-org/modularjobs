package dev.mintychochip.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PayableCurve;
import dev.mintychochip.upgrade.NodeLevel;
import dev.mintychochip.upgrade.PlayerUpgradeData;
import dev.mintychochip.upgrade.SkillNode;
import dev.mintychochip.upgrade.SkillNode.LevelEffectMode;
import dev.mintychochip.upgrade.SkillNodeKind;
import dev.mintychochip.upgrade.SkillTree;
import dev.mintychochip.upgrade.SkillTreeState;
import dev.mintychochip.upgrade.UpgradeEffect;
import dev.mintychochip.upgrade.UpgradeNode;
import dev.mintychochip.upgrade.UpgradeService;
import dev.mintychochip.upgrade.UpgradeService.PurchaseResult;
import dev.mintychochip.upgrade.UpgradeService.UnlockResult;
import dev.mintychochip.upgrade.UpgradeTree;
import dev.mintychochip.upgrade.rendering.Position;
import dev.mintychochip.upgrade.rendering.UpgradeTreeGui;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
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

class NativeUpgradeGuiTest {
  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();
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
  void legacyAndV2NodesKeepComputedSlotsAndStatusMaterialLorePrecedence() {
    PlayerMock legacyPlayer = server.addPlayer("upgrade-legacy-" + UUID.randomUUID());
    UpgradeTree legacyTree = legacyTree();
    PlayerUpgradeData legacyData = legacyData(legacyPlayer, "miner");
    AtomicInteger unlocks = new AtomicInteger();
    UpgradeService legacyService =
        service(legacyTree, null, legacyData, null, null, null, unlocks, null, null);
    PaperUiHost legacyHost = new PaperUiHost();
    UpgradeTreeGui legacyGui = new UpgradeTreeGui(legacyHost, legacyService);

    legacyGui.open(legacyPlayer, job("miner"), legacyTree);
    Inventory legacy = legacyPlayer.getOpenInventory().getTopInventory();
    assertEquals(Material.DIAMOND, legacy.getItem(0).getType());
    assertEquals(Material.GOLD_INGOT, legacy.getItem(1).getType());
    assertEquals(Material.LIGHT_GRAY_STAINED_GLASS_PANE, legacy.getItem(2).getType());
    assertEquals(Material.RED_STAINED_GLASS_PANE, legacy.getItem(3).getType());
    legacyHost.onInventoryClick(click(legacyPlayer.getOpenInventory(), 1));
    assertEquals(1, unlocks.get());
    assertTrue(lore(legacy, 1).contains("Click to unlock"));
    assertTrue(lore(legacy, 2).contains("Requires:"));
    assertTrue(lore(legacy, 3).contains("Blocked by: Root"));

    PlayerMock v2Player = server.addPlayer("upgrade-v2-status-" + UUID.randomUUID());
    SkillTree v2Tree = v2Tree();
    SkillTreeState v2State = state(v2Player, "miner", 10, Map.of("root", 1));
    UpgradeService v2Service = service(null, v2Tree, null, v2State, null, null, null, null, null);
    UpgradeTreeGui v2Gui = new UpgradeTreeGui(new PaperUiHost(), v2Service);

    v2Gui.open(v2Player, job("miner"), null);
    Inventory v2 = v2Player.getOpenInventory().getTopInventory();
    assertEquals(Material.EMERALD, v2.getItem(0).getType());
    assertEquals(Material.GOLD_INGOT, v2.getItem(1).getType());
    assertEquals(Material.LIGHT_GRAY_STAINED_GLASS_PANE, v2.getItem(2).getType());
    assertEquals(Material.RED_STAINED_GLASS_PANE, v2.getItem(3).getType());
    assertEquals(Material.GOLD_INGOT, v2.getItem(4).getType());
    assertTrue(lore(v2, 1).contains("Click to unlock"));
    assertTrue(lore(v2, 2).contains("Requires:"));
    assertTrue(lore(v2, 3).contains("Path Locked"));
    assertTrue(lore(v2, 4).contains("Click to confirm - permanent choice"));
  }

  @Test
  void scrollControlsMoveWithinBoundsAndDoNotRefreshAtBounds() {
    PlayerMock player = server.addPlayer("upgrade-scroll-" + UUID.randomUUID());
    SkillTree tree = scrollingTree();
    SkillTreeState state = state(player, "miner", 10, Map.of("root", 1));
    UpgradeService service = service(null, tree, null, state, null, null, null, null, null);
    PaperUiHost host = new PaperUiHost();
    UpgradeTreeGui gui = new UpgradeTreeGui(host, service);

    gui.open(player, job("miner"), null);
    Inventory initial = player.getOpenInventory().getTopInventory();
    Object holder = initial.getHolder();
    assertEquals(Material.BLACK_STAINED_GLASS_PANE, initial.getItem(36).getType());
    assertEquals(Material.CYAN_STAINED_GLASS_PANE, initial.getItem(53).getType());

    host.onInventoryClick(click(player.getOpenInventory(), 53));
    Inventory atBottom = player.getOpenInventory().getTopInventory();
    assertNotSame(initial, atBottom);
    assertSame(holder, atBottom.getHolder());
    assertEquals(Material.GOLD_INGOT, atBottom.getItem(36).getType());
    assertEquals(Material.GRAY_STAINED_GLASS_PANE, atBottom.getItem(53).getType());

    host.onInventoryClick(click(player.getOpenInventory(), 53));
    assertSame(atBottom, player.getOpenInventory().getTopInventory());

    host.onInventoryClick(click(player.getOpenInventory(), 45));
    Inventory atTop = player.getOpenInventory().getTopInventory();
    assertNotSame(atBottom, atTop);
    assertSame(holder, atTop.getHolder());
    assertEquals(Material.BLACK_STAINED_GLASS_PANE, atTop.getItem(36).getType());
    assertEquals(Material.GRAY_STAINED_GLASS_PANE, atTop.getItem(45).getType());

    host.onInventoryClick(click(player.getOpenInventory(), 45));
    assertSame(atTop, player.getOpenInventory().getTopInventory());
  }

  @Test
  void majorConfirmationAndNonMajorPurchaseDispatchExactlyOnce() {
    PlayerMock player = server.addPlayer("upgrade-purchase-" + UUID.randomUUID());
    SkillTree tree = v2Tree();
    SkillTreeState state = state(player, "miner", 10, Map.of("root", 1));
    AtomicInteger majorPurchases = new AtomicInteger();
    AtomicInteger skillPurchases = new AtomicInteger();
    UpgradeService service =
        service(
            null,
            tree,
            null,
            state,
            majorPurchases,
            skillPurchases,
            null,
            new PurchaseResult.Success(tree.node("major").orElseThrow(), 5),
            new PurchaseResult.Success(tree.node("skill").orElseThrow(), 8));
    PaperUiHost host = new PaperUiHost();
    UpgradeTreeGui gui = new UpgradeTreeGui(host, service);

    gui.open(player, job("miner"), null);
    host.onInventoryClick(click(player.getOpenInventory(), 4));
    Inventory pending = player.getOpenInventory().getTopInventory();
    assertEquals(Material.GOLD_INGOT, pending.getItem(52).getType());
    assertEquals("Confirm Major?", displayName(pending, 52));
    assertTrue(lore(pending, 52).contains("Permanent choice - cannot be refunded"));

    host.onInventoryClick(click(player.getOpenInventory(), 52));
    assertEquals(1, majorPurchases.get());
    assertEquals(
        Material.GRAY_STAINED_GLASS_PANE,
        player.getOpenInventory().getTopInventory().getItem(52).getType());

    host.onInventoryClick(click(player.getOpenInventory(), 1));
    assertEquals(1, skillPurchases.get());
  }

  @Test
  void lockedPurchaseUsesFailureMessagePathWithoutRefresh() {
    PlayerMock player = server.addPlayer("upgrade-failure-" + UUID.randomUUID());
    SkillTree tree = v2Tree();
    AtomicInteger stateReads = new AtomicInteger();
    SkillTreeState state =
        new SkillTreeState(
            player.getUniqueId().toString(),
            "miner",
            0,
            Map.of(),
            Map.of(),
            () -> stateReads.incrementAndGet(),
            permission -> false);
    AtomicInteger skillPurchases = new AtomicInteger();
    UpgradeService service =
        service(
            null,
            tree,
            null,
            state,
            null,
            skillPurchases,
            null,
            null,
            new PurchaseResult.InsufficientPoints(2, 0));
    PaperUiHost host = new PaperUiHost();
    UpgradeTreeGui gui = new UpgradeTreeGui(host, service);

    gui.open(player, job("miner"), null);
    Inventory before = player.getOpenInventory().getTopInventory();
    assertEquals(Material.LIGHT_GRAY_STAINED_GLASS_PANE, before.getItem(1).getType());

    host.onInventoryClick(click(player.getOpenInventory(), 1));
    assertEquals("Not enough SP! Need 2, have 0", PLAIN.serialize(player.nextComponentMessage()));
    var sounds = player.getHeardSounds();
    assertEquals(1, sounds.size());
    assertEquals("entity.villager.no", sounds.get(0).getSound());
    assertEquals(1.0f, sounds.get(0).getVolume());
    assertEquals(1.0f, sounds.get(0).getPitch());
    Inventory after = player.getOpenInventory().getTopInventory();
    assertEquals(1, skillPurchases.get());
    assertEquals(1, stateReads.get());
    assertSame(before, after);
  }

  @Test
  void invalidLegacyUnlockUsesMessageAndItemBreakFeedbackWithoutRefresh() {
    PlayerMock player = server.addPlayer("upgrade-invalid-" + UUID.randomUUID());
    UpgradeTree tree = legacyTree();
    PlayerUpgradeData data = legacyData(player, "miner");
    AtomicInteger unlocks = new AtomicInteger();
    UpgradeService service =
        service(
            tree,
            null,
            data,
            null,
            null,
            null,
            unlocks,
            null,
            null,
            new UnlockResult.NodeNotFound("missing"));
    PaperUiHost host = new PaperUiHost();
    UpgradeTreeGui gui = new UpgradeTreeGui(host, service);

    gui.open(player, job("miner"), tree);
    Inventory before = player.getOpenInventory().getTopInventory();
    host.onInventoryClick(click(player.getOpenInventory(), 1));

    assertEquals(1, unlocks.get());
    assertEquals("Node not found: missing", PLAIN.serialize(player.nextComponentMessage()));
    var sounds = player.getHeardSounds();
    assertEquals(1, sounds.size());
    assertEquals("entity.item.break", sounds.get(0).getSound());
    assertEquals(0.8f, sounds.get(0).getVolume());
    assertEquals(1.0f, sounds.get(0).getPitch());
    assertSame(before, player.getOpenInventory().getTopInventory());
  }

  private static @NotNull InventoryClickEvent click(
      @NotNull org.bukkit.inventory.InventoryView view, int rawSlot) {
    return new InventoryClickEvent(
        view, SlotType.CONTAINER, rawSlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
  }

  private static @NotNull String displayName(@NotNull Inventory inventory, int slot) {
    return PLAIN.serialize(inventory.getItem(slot).getItemMeta().displayName());
  }

  private static @NotNull String lore(@NotNull Inventory inventory, int slot) {
    return inventory.getItem(slot).getItemMeta().lore().stream()
        .map(PLAIN::serialize)
        .reduce((a, b) -> a + "\n" + b)
        .orElse("");
  }

  private static @NotNull UpgradeTree legacyTree() {
    UpgradeNode root =
        legacyNode(
            "root",
            "Root",
            "STONE",
            "DIAMOND",
            Set.of(),
            Set.of(),
            new Position(0, 0),
            List.of("available"));
    UpgradeNode available =
        legacyNode(
            "available",
            "Available",
            "GOLD_INGOT",
            "EMERALD",
            Set.of("root"),
            Set.of(),
            new Position(1, 0),
            List.of());
    UpgradeNode locked =
        legacyNode(
            "locked",
            "Locked",
            "GOLD_INGOT",
            "EMERALD",
            Set.of("missing"),
            Set.of(),
            new Position(2, 0),
            List.of());
    UpgradeNode excluded =
        legacyNode(
            "excluded",
            "Excluded",
            "GOLD_INGOT",
            "EMERALD",
            Set.of(),
            Set.of("root"),
            new Position(3, 0),
            List.of());
    return new UpgradeTree(
        Key.key("modularjobs", "upgrade_tree/miner"),
        "miner",
        null,
        "root",
        1,
        Map.of("root", root, "available", available, "locked", locked, "excluded", excluded));
  }

  private static @NotNull UpgradeNode legacyNode(
      @NotNull String key,
      @NotNull String name,
      @NotNull String icon,
      @NotNull String unlockedIcon,
      @NotNull Set<String> prerequisites,
      @NotNull Set<String> exclusive,
      @NotNull Position position,
      @NotNull List<String> children) {
    return new UpgradeNode(
        Key.key("miner", key),
        name,
        "Description",
        icon,
        unlockedIcon,
        null,
        null,
        2,
        prerequisites,
        Set.of(),
        exclusive,
        children,
        List.<UpgradeEffect>of(),
        position,
        List.of(),
        key + "_perk",
        1);
  }

  private static @NotNull SkillTree v2Tree() {
    SkillNode root =
        skillNode(
            "root",
            "Root",
            SkillNodeKind.ROOT,
            "STONE",
            "EMERALD",
            Set.of(),
            Set.of(),
            List.of(),
            new Position(0, 0),
            List.of());
    SkillNode skill =
        skillNode(
            "skill",
            "Skill",
            SkillNodeKind.SKILL,
            "GOLD_INGOT",
            "EMERALD",
            Set.of("root"),
            Set.of(),
            List.of(new NodeLevel(2, List.of())),
            new Position(1, 0),
            List.of());
    SkillNode locked =
        skillNode(
            "locked",
            "Locked",
            SkillNodeKind.SKILL,
            "DIAMOND",
            "EMERALD",
            Set.of("missing"),
            Set.of(),
            List.of(new NodeLevel(2, List.of())),
            new Position(2, 0),
            List.of());
    SkillNode excluded =
        skillNode(
            "excluded",
            "Excluded",
            SkillNodeKind.MAJOR,
            "IRON_INGOT",
            "EMERALD",
            Set.of(),
            Set.of("root"),
            List.of(),
            new Position(3, 0),
            List.of());
    SkillNode major =
        skillNode(
            "major",
            "Major",
            SkillNodeKind.MAJOR,
            "GOLD_INGOT",
            "EMERALD",
            Set.of("root"),
            Set.of(),
            List.of(),
            new Position(4, 0),
            List.of());
    return new SkillTree(
        Key.key("modularjobs", "upgrade_tree/miner"),
        "miner",
        null,
        1,
        "root",
        Map.of(
            "root", root, "skill", skill, "locked", locked, "excluded", excluded, "major", major));
  }

  private static @NotNull SkillTree scrollingTree() {
    SkillNode root =
        skillNode(
            "root",
            "Root",
            SkillNodeKind.ROOT,
            "STONE",
            "DIAMOND",
            Set.of(),
            Set.of(),
            List.of(),
            new Position(0, 0),
            List.of());
    SkillNode deep =
        skillNode(
            "deep",
            "Deep",
            SkillNodeKind.SKILL,
            "GOLD_INGOT",
            "DIAMOND",
            Set.of("root"),
            Set.of(),
            List.of(new NodeLevel(1, List.of())),
            new Position(0, 8),
            List.of());
    return new SkillTree(
        Key.key("modularjobs", "upgrade_tree/miner"),
        "miner",
        null,
        1,
        "root",
        Map.of("root", root, "deep", deep));
  }

  private static @NotNull SkillNode skillNode(
      @NotNull String key,
      @NotNull String name,
      @NotNull SkillNodeKind kind,
      @NotNull String lockedIcon,
      @NotNull String unlockedIcon,
      @NotNull Set<String> prerequisites,
      @NotNull Set<String> excludes,
      @NotNull List<NodeLevel> levels,
      @NotNull Position position,
      @NotNull List<dev.mintychochip.upgrade.NodeEffect> effects) {
    return new SkillNode(
        Key.key("miner", key),
        name,
        "Description",
        lockedIcon,
        unlockedIcon,
        null,
        null,
        kind,
        kind == SkillNodeKind.MAJOR ? 5 : 0,
        levels.size(),
        LevelEffectMode.REPLACE,
        levels,
        List.of(),
        prerequisites,
        excludes,
        effects,
        position,
        List.of(),
        List.of());
  }

  private static @NotNull SkillTreeState state(
      @NotNull PlayerMock player,
      @NotNull String jobKey,
      int totalPoints,
      @NotNull Map<String, Integer> levels) {
    return new SkillTreeState(
        player.getUniqueId().toString(), jobKey, totalPoints, levels, Map.of());
  }

  private static @NotNull PlayerUpgradeData legacyData(
      @NotNull PlayerMock player, @NotNull String jobKey) {
    return proxy(
        PlayerUpgradeData.class,
        (method, args) ->
            switch (method.getName()) {
              case "playerId" -> player.getUniqueId().toString();
              case "jobKey" -> jobKey;
              case "totalSkillPoints", "availableSkillPoints" -> 10;
              case "spentSkillPoints" -> 0;
              case "unlockedNodes" -> Set.of("root");
              case "hasUnlocked" -> Set.of("root").contains(args[0]);
              case "perkLevels" -> Map.of();
              case "getMaxLevel" -> 1;
              case "state" -> SkillTreeState.empty(player.getUniqueId().toString(), jobKey);
              default -> defaultValue(method.getReturnType());
            });
  }

  private static @NotNull UpgradeService service(
      @Nullable UpgradeTree legacyTree,
      @Nullable SkillTree skillTree,
      @Nullable PlayerUpgradeData data,
      @Nullable SkillTreeState state,
      @Nullable AtomicInteger majorPurchases,
      @Nullable AtomicInteger skillPurchases,
      @Nullable AtomicInteger unlocks,
      @Nullable PurchaseResult majorResult,
      @Nullable PurchaseResult skillResult) {
    return service(
        legacyTree,
        skillTree,
        data,
        state,
        majorPurchases,
        skillPurchases,
        unlocks,
        majorResult,
        skillResult,
        new UnlockResult.InsufficientPoints(2, 0));
  }

  private static @NotNull UpgradeService service(
      @Nullable UpgradeTree legacyTree,
      @Nullable SkillTree skillTree,
      @Nullable PlayerUpgradeData data,
      @Nullable SkillTreeState state,
      @Nullable AtomicInteger majorPurchases,
      @Nullable AtomicInteger skillPurchases,
      @Nullable AtomicInteger unlocks,
      @Nullable PurchaseResult majorResult,
      @Nullable PurchaseResult skillResult,
      @Nullable UnlockResult unlockResult) {
    return proxy(
        UpgradeService.class,
        (method, args) ->
            switch (method.getName()) {
              case "getTree" -> Optional.ofNullable(legacyTree);
              case "getSkillTree" -> Optional.ofNullable(skillTree);
              case "getPlayerData" -> data;
              case "getSkillTreeState" -> state;
              case "purchaseMajor" -> {
                if (majorPurchases != null) majorPurchases.incrementAndGet();
                yield majorResult;
              }
              case "purchaseSkillLevel" -> {
                if (skillPurchases != null) skillPurchases.incrementAndGet();
                yield skillResult;
              }
              case "unlock" -> {
                if (unlocks != null) unlocks.incrementAndGet();
                yield unlockResult;
              }
              default -> defaultValue(method.getReturnType());
            });
  }

  private static @NotNull Job job(@NotNull String key) {
    JobKey jobKey = new JobKey(Key.key("modularjobs", key));
    JobNodeKey nodeKey = new JobNodeKey(jobKey.key());
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
            return Component.text("Miner");
          }

          @Override
          public @NotNull String getPlainName() {
            return "Miner";
          }

          @Override
          public @NotNull Component description() {
            return Component.text("A useful job");
          }
        };
    LevelingCurve curve = parameters -> BigDecimal.valueOf(parameters.level() * 100L);
    return new Job() {
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
      public @NotNull Map<Key, PayableCurve> payableCurves() {
        return Map.of();
      }

      @Override
      public int maxLevel() {
        return 10;
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
}
