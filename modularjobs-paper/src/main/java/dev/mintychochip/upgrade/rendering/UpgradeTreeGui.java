package dev.mintychochip.upgrade.rendering;

import dev.mintychochip.Job;
import dev.mintychochip.gui.PaperItemFactory;
import dev.mintychochip.gui.PaperUiHost;
import dev.mintychochip.upgrade.NodeEffect;
import dev.mintychochip.upgrade.PlayerUpgradeData;
import dev.mintychochip.upgrade.SkillNode;
import dev.mintychochip.upgrade.SkillTree;
import dev.mintychochip.upgrade.SkillTreeState;
import dev.mintychochip.upgrade.UpgradeEffect;
import dev.mintychochip.upgrade.UpgradeNode;
import dev.mintychochip.upgrade.UpgradeService;
import dev.mintychochip.upgrade.UpgradeService.PurchaseResult;
import dev.mintychochip.upgrade.UpgradeService.UnlockResult;
import dev.mintychochip.upgrade.UpgradeTree;
import dev.mintychochip.util.Messages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Job upgrade tree GUI rendered through the native Paper inventory host.
 *
 * <p>Presentation is rebuilt into a {@link PaperUiHost.ScreenView} on each open/refresh.
 */
public final class UpgradeTreeGui {

  private static final int GUI_SIZE = 54;
  private static final int GUI_ROWS = 5;
  private static final int GUI_COLS = 9;
  private static final int CONTROL_ROW_START = 45;
  private static final int CONFIRM_SLOT = 52;
  private static final String MENU_ID = "upgrade_tree";
  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();

  private final PaperUiHost host;
  private final UpgradeService upgradeService;
  private final Map<UUID, GuiSession> openGuis = new HashMap<>();

  private static final class GuiSession {
    final Job job;
    final UpgradeTree tree;
    final SkillTree skillTree;
    String pendingMajorKey;
    int scrollOffset;

    Map<Integer, String> slotNodes = Map.of();

    GuiSession(@NotNull Job job, @Nullable UpgradeTree tree, @Nullable SkillTree skillTree) {
      this.job = job;
      this.tree = tree;
      this.skillTree = skillTree;
      this.pendingMajorKey = null;
      this.scrollOffset = 0;
    }

    boolean isV2() {
      return skillTree != null;
    }
  }

  /** Builds the tree presenter over the shared native Paper inventory host. */
  public UpgradeTreeGui(@NotNull PaperUiHost host, @NotNull UpgradeService upgradeService) {
    this.host = host;
    this.upgradeService = upgradeService;
  }

  /**
   * Opens (or replaces) the upgrade-tree view for {@code player}, resolving the active skill tree
   * and resetting scroll/pending state. Bukkit thread.
   */
  public void open(@NotNull Player player, @NotNull Job job, @Nullable UpgradeTree tree) {
    String jobKey = job.key().value();
    SkillTree skillTree = upgradeService.getSkillTree(jobKey).orElse(null);
    if (tree == null && skillTree == null) {
      return;
    }

    UUID playerUuid = player.getUniqueId();
    GuiSession session = new GuiSession(job, tree, skillTree);
    host.open(player, buildView(player, session));
    openGuis.put(playerUuid, session);
  }

  /** Re-renders the caller's open tree view in place (Bukkit thread). */
  public void refresh(@NotNull Player player) {
    UUID playerId = player.getUniqueId();
    GuiSession session = openGuis.get(playerId);
    if (session == null) {
      return;
    }
    host.refresh(player, buildView(player, session));
  }

  /**
   * Host action handler for an upgrade node click. Unlocks/purchases the clicked node, staging
   * permanent "major" choices for confirm.
   */
  public void onNodeClick(@NotNull Player player, @NotNull InventoryClickEvent event) {
    UUID playerId = player.getUniqueId();
    GuiSession session = openGuis.get(playerId);
    if (session == null) {
      return;
    }
    String nodeKey = session.slotNodes.get(event.getRawSlot());
    if (nodeKey == null) {
      return;
    }
    String playerIdString = playerId.toString();
    String jobKey = session.job.key().value();
    if (session.isV2()) {
      handleV2NodeClick(player, session, nodeKey, playerIdString, jobKey);
    } else {
      handleLegacyUnlock(player, nodeKey, playerIdString, jobKey);
    }
  }

  /** Host action handler for scrolling up. */
  public void onScrollUp(@NotNull Player player, @NotNull InventoryClickEvent event) {
    GuiSession session = openGuis.get(player.getUniqueId());
    if (session == null) {
      return;
    }
    handleScroll(player, session, "up");
  }

  /** Host action handler for scrolling down. */
  public void onScrollDown(@NotNull Player player, @NotNull InventoryClickEvent event) {
    GuiSession session = openGuis.get(player.getUniqueId());
    if (session == null) {
      return;
    }
    handleScroll(player, session, "down");
  }

  /** Host action handler for confirming a pending major choice. */
  public void onConfirm(@NotNull Player player, @NotNull InventoryClickEvent event) {
    GuiSession session = openGuis.get(player.getUniqueId());
    if (session == null || session.pendingMajorKey == null) {
      return;
    }
    purchaseMajor(player, session, session.pendingMajorKey);
  }

  @NotNull
  PaperUiHost.ScreenView buildView(@NotNull Player player, @NotNull GuiSession session) {
    String playerId = player.getUniqueId().toString();
    String jobKey = session.job.key().value();
    PlayerUpgradeData data = loadData(playerId, jobKey, session);
    SkillTreeState state = loadState(playerId, jobKey, session);

    Map<Integer, ItemStack> items = new HashMap<>();
    Map<Integer, PaperUiHost.SlotAction> actions = new HashMap<>();
    Map<Integer, String> slotNodes = new HashMap<>();

    for (int i = 0; i < CONTROL_ROW_START; i++) {
      items.put(i, PaperItemFactory.pane(Material.BLACK_STAINED_GLASS_PANE));
    }

    if (!session.isV2()) {
      renderConnections(items, session, data.unlockedNodes());
      for (UpgradeNode node : session.tree.allNodes()) {
        int slot = calculateSlotWithScroll(node.position(), session.scrollOffset);
        if (slot < 0 || slot >= CONTROL_ROW_START) {
          continue;
        }
        NodeStatus status =
            getStatus(
                node,
                data.unlockedNodes(),
                session.tree.getAvailableNodes(data.unlockedNodes(), data));
        String key = getShortKey(node);
        items.put(slot, nodeItem(node, status, data, session.tree));
        actions.put(slot, (clicked, event) -> onNodeClick(clicked, event));
        slotNodes.put(slot, key);
      }
    } else {
      for (SkillNode node : session.skillTree.nodes()) {
        int slot = calculateSlotWithScroll(node.position(), session.scrollOffset);
        if (slot < 0 || slot >= CONTROL_ROW_START) {
          continue;
        }
        NodeStatus status = v2Status(session.skillTree, state, node);
        String key = node.key().value();
        items.put(slot, v2NodeItem(node, status, state, session.skillTree));
        actions.put(slot, (clicked, event) -> onNodeClick(clicked, event));
        slotNodes.put(slot, key);
      }
    }

    session.slotNodes = Map.copyOf(slotNodes);
    placeControls(items, actions, session, data, state);

    String title = PLAIN.serialize(session.job.displayName()) + " Upgrades";
    if (title.length() > 128) {
      title = title.substring(0, 128);
    }
    return new PaperUiHost.ScreenView(
        MENU_ID,
        6,
        Component.text(title),
        items,
        actions,
        closedPlayer -> {
          session.pendingMajorKey = null;
          openGuis.remove(closedPlayer.getUniqueId(), session);
        });
  }

  private void placeControls(
      @NotNull Map<Integer, ItemStack> items,
      @NotNull Map<Integer, PaperUiHost.SlotAction> actions,
      @NotNull GuiSession session,
      @NotNull PlayerUpgradeData data,
      @NotNull SkillTreeState state) {
    int maxY =
        session.isV2()
            ? session.skillTree.nodes().stream()
                .map(SkillNode::position)
                .filter(pos -> pos != null)
                .mapToInt(Position::y)
                .max()
                .orElse(0)
            : session.tree.allNodes().stream()
                .map(UpgradeNode::position)
                .filter(pos -> pos != null)
                .mapToInt(Position::y)
                .max()
                .orElse(0);
    int maxScroll = Math.max(0, maxY - GUI_ROWS + 1);
    boolean canScrollUp = session.scrollOffset > 0;
    boolean canScrollDown = session.scrollOffset < maxScroll;

    for (int i = CONTROL_ROW_START; i < GUI_SIZE; i++) {
      items.put(i, PaperItemFactory.pane(Material.GRAY_STAINED_GLASS_PANE));
    }

    Material upMat =
        canScrollUp ? Material.CYAN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
    String upLabel = canScrollUp ? "Scroll Up" : "Scroll Up (At Top)";
    items.put(CONTROL_ROW_START, PaperItemFactory.of(upMat, upLabel, List.of()));
    if (canScrollUp) {
      actions.put(CONTROL_ROW_START, (player, event) -> onScrollUp(player, event));
    }

    Material downMat =
        canScrollDown ? Material.CYAN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
    String downLabel = canScrollDown ? "Scroll Down" : "Scroll Down (At Bottom)";
    items.put(CONTROL_ROW_START + 8, PaperItemFactory.of(downMat, downLabel, List.of()));
    if (canScrollDown) {
      actions.put(CONTROL_ROW_START + 8, (player, event) -> onScrollDown(player, event));
    }

    items.put(
        CONTROL_ROW_START + 4,
        session.isV2() ? v2InfoItem(session.skillTree, state) : infoItem(session.tree, data));

    if (session.pendingMajorKey != null) {
      String pendingName =
          session
              .skillTree
              .node(session.pendingMajorKey)
              .map(SkillNode::name)
              .orElse(session.pendingMajorKey);
      List<String> lore =
          List.of(pendingName, "Permanent choice - cannot be refunded", "Click to confirm");
      items.put(CONFIRM_SLOT, PaperItemFactory.of(Material.GOLD_INGOT, "Confirm Major?", lore));
      actions.put(CONFIRM_SLOT, (player, event) -> onConfirm(player, event));
    }
  }

  private void renderConnections(
      @NotNull Map<Integer, ItemStack> items,
      @NotNull GuiSession session,
      @NotNull Set<String> unlocked) {
    Set<GridPoint> allPathPoints = new HashSet<>();
    for (Position p : session.tree.paths()) {
      allPathPoints.add(new GridPoint(p.x(), p.y()));
    }

    Set<GridPoint> unlockedNodePositions = new HashSet<>();
    for (UpgradeNode node : session.tree.allNodes()) {
      if (node.position() == null) {
        continue;
      }
      GridPoint point = new GridPoint(node.position().x(), node.position().y());
      if (unlocked.contains(getShortKey(node))) {
        unlockedNodePositions.add(point);
      }
    }

    Set<GridPoint> litPathPoints = new HashSet<>();
    int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
    for (GridPoint startNode : unlockedNodePositions) {
      Map<GridPoint, GridPoint> parent = new HashMap<>();
      java.util.Queue<GridPoint> queue = new java.util.LinkedList<>();
      Set<GridPoint> visited = new HashSet<>();
      queue.add(startNode);
      visited.add(startNode);
      while (!queue.isEmpty()) {
        GridPoint current = queue.poll();
        for (int[] dir : directions) {
          GridPoint neighbor = new GridPoint(current.column + dir[0], current.row + dir[1]);
          if (visited.contains(neighbor)) {
            continue;
          }
          visited.add(neighbor);
          parent.put(neighbor, current);
          if (unlockedNodePositions.contains(neighbor)) {
            GridPoint trace = neighbor;
            while (trace != null && parent.containsKey(trace)) {
              GridPoint prev = parent.get(trace);
              if (allPathPoints.contains(trace)) {
                litPathPoints.add(trace);
              }
              trace = prev;
            }
            queue.add(neighbor);
          } else if (allPathPoints.contains(neighbor)) {
            queue.add(neighbor);
          }
        }
      }
    }

    for (GridPoint pathPoint : allPathPoints) {
      int screenY = pathPoint.row - session.scrollOffset;
      if (screenY < 0
          || screenY >= GUI_ROWS
          || pathPoint.column < 0
          || pathPoint.column >= GUI_COLS) {
        continue;
      }
      boolean isLit = litPathPoints.contains(pathPoint);
      Material material =
          isLit ? Material.CYAN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
      int slot = screenY * GUI_COLS + pathPoint.column;
      if (slot >= 0 && slot < CONTROL_ROW_START) {
        items.put(slot, PaperItemFactory.pane(material));
      }
    }
  }

  private @Nullable PlayerUpgradeData loadData(
      @NotNull String playerId, @NotNull String jobKey, @NotNull GuiSession session) {
    return session.isV2() ? null : upgradeService.getPlayerData(playerId, jobKey);
  }

  private @Nullable SkillTreeState loadState(
      @NotNull String playerId, @NotNull String jobKey, @NotNull GuiSession session) {
    return session.isV2() ? upgradeService.getSkillTreeState(playerId, jobKey) : null;
  }

  private @NotNull NodeStatus v2Status(
      @NotNull SkillTree tree, @NotNull SkillTreeState state, @NotNull SkillNode node) {
    String key = node.key().value();
    if (state.levelOf(key) > 0) {
      return NodeStatus.UNLOCKED;
    }
    if (tree.canPurchase(state, key)) {
      return NodeStatus.AVAILABLE;
    }
    boolean excluded = tree.symmetricExcludes(key).stream().anyMatch(state::hasUnlocked);
    return excluded ? NodeStatus.EXCLUDED : NodeStatus.LOCKED;
  }

  private @NotNull NodeStatus getStatus(
      @NotNull UpgradeNode node,
      @NotNull Set<String> unlocked,
      @NotNull Set<UpgradeNode> available) {
    String shortKey = getShortKey(node);
    if (unlocked.contains(shortKey)) {
      return NodeStatus.UNLOCKED;
    }
    for (String exclusiveKey : node.exclusive()) {
      if (unlocked.contains(exclusiveKey)) {
        return NodeStatus.EXCLUDED;
      }
    }
    if (available.contains(node)) {
      return NodeStatus.AVAILABLE;
    }
    return NodeStatus.LOCKED;
  }

  private int calculateSlotWithScroll(@NotNull Position position, int scrollOffset) {
    if (position == null) {
      return -1;
    }
    int x = position.x();
    int y = position.y() - scrollOffset;
    if (x < 0 || x >= GUI_COLS || y < 0 || y >= GUI_ROWS) {
      return -1;
    }
    return y * GUI_COLS + x;
  }

  private @NotNull ItemStack nodeItem(
      @NotNull UpgradeNode node,
      @NotNull NodeStatus status,
      @NotNull PlayerUpgradeData data,
      @NotNull UpgradeTree tree) {
    List<String> lore = new ArrayList<>();
    if (node.description() != null && !node.description().isEmpty()) {
      for (String line : node.description().split("\n")) {
        lore.add(line);
      }
    }
    lore.add("");
    lore.add("Cost: " + node.cost() + " SP");
    if (!node.effects().isEmpty()) {
      lore.add("");
      lore.add("Effects:");
      for (UpgradeEffect effect : node.effects()) {
        lore.add("  • " + formatEffect(effect));
      }
    }
    if (status == NodeStatus.LOCKED && !node.prerequisites().isEmpty()) {
      lore.add("");
      lore.add("Requires:");
      for (String prereq : node.prerequisites()) {
        lore.add("  • " + prereq);
      }
    }
    lore.add("");
    lore.add(
        switch (status) {
          case UNLOCKED -> "✔ Unlocked!";
          case AVAILABLE ->
              data.availableSkillPoints() >= node.cost() ? "Click to unlock" : "Not enough SP!";
          case LOCKED -> "Locked";
          case EXCLUDED -> "✘ Path Locked (Exclusive Choice)";
        });
    if (status == NodeStatus.EXCLUDED && !node.exclusive().isEmpty()) {
      List<String> exclusiveNames =
          node.exclusive().stream()
              .map(tree::getNode)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .map(UpgradeNode::name)
              .toList();
      lore.add("Blocked by: " + String.join(", ", exclusiveNames));
    }
    Material material =
        switch (status) {
          case UNLOCKED -> materialFromName(node.unlockedIcon());
          case AVAILABLE -> materialFromName(node.icon());
          case LOCKED -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
          case EXCLUDED -> Material.RED_STAINED_GLASS_PANE;
        };
    NamedTextColor nameColor =
        switch (status) {
          case UNLOCKED -> NamedTextColor.GREEN;
          case AVAILABLE -> NamedTextColor.YELLOW;
          case LOCKED -> NamedTextColor.GRAY;
          case EXCLUDED -> NamedTextColor.RED;
        };
    return PaperItemFactory.of(
        material,
        plain(Component.text(node.name(), nameColor).decoration(TextDecoration.ITALIC, false)),
        lore);
  }

  private @NotNull ItemStack v2NodeItem(
      @NotNull SkillNode node,
      @NotNull NodeStatus status,
      @NotNull SkillTreeState state,
      @NotNull SkillTree tree) {
    String key = node.key().value();
    int owned = state.levelOf(key);
    List<String> lore = new ArrayList<>();
    if (node.description() != null && !node.description().isEmpty()) {
      for (String line : node.description().split("\n")) {
        lore.add(line);
      }
      lore.add("");
    }
    if (node.isSkill()) {
      lore.add("Level: " + owned + "/" + node.maxLevel());
    } else {
      lore.add("Permanent choice");
    }
    int cost = node.isSkill() ? node.levelCost(owned + 1) : node.cost();
    lore.add("Cost: " + cost + " SP");
    if (status == NodeStatus.UNLOCKED && !node.activeEffects(owned).isEmpty()) {
      lore.add("");
      lore.add("Active Effects:");
      for (NodeEffect effect : node.activeEffects(owned)) {
        lore.add("  • " + formatV2Effect(effect));
      }
    }
    if (status == NodeStatus.LOCKED && !node.prerequisites().isEmpty()) {
      lore.add("");
      lore.add("Requires:");
      for (String prereq : node.prerequisites()) {
        lore.add("  • " + prereq);
      }
    }
    lore.add("");
    if (status == NodeStatus.UNLOCKED) {
      lore.add("✔ Unlocked!");
    } else if (status == NodeStatus.AVAILABLE) {
      if (node.isMajor()) {
        lore.add("Click to confirm - permanent choice");
      } else if (cost > tree.availablePoints(state)) {
        lore.add("Not enough SP!");
      } else {
        lore.add("Click to unlock");
      }
    } else if (status == NodeStatus.EXCLUDED) {
      lore.add("✘ Path Locked (Exclusive Choice)");
    } else {
      lore.add("Locked");
    }
    Material material =
        switch (status) {
          case UNLOCKED -> materialFromName(node.unlockedIcon());
          case AVAILABLE -> materialFromName(node.lockedIcon());
          case LOCKED -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
          case EXCLUDED -> Material.RED_STAINED_GLASS_PANE;
        };
    NamedTextColor nameColor =
        switch (status) {
          case UNLOCKED -> NamedTextColor.GREEN;
          case AVAILABLE -> NamedTextColor.YELLOW;
          case LOCKED -> NamedTextColor.GRAY;
          case EXCLUDED -> NamedTextColor.RED;
        };
    return PaperItemFactory.of(
        material,
        plain(Component.text(node.name(), nameColor).decoration(TextDecoration.ITALIC, false)),
        lore);
  }

  private @NotNull ItemStack infoItem(@NotNull UpgradeTree tree, @NotNull PlayerUpgradeData data) {
    List<String> lore =
        List.of(
            "Available SP: " + data.availableSkillPoints(),
            "Total SP: " + data.totalSkillPoints(),
            "Unlocked: " + data.unlockedNodes().size() + "/" + tree.allNodes().size(),
            "",
            "SP per level: " + tree.skillPointsPerLevel());
    return PaperItemFactory.of(Material.BOOK, "Skill Tree Info", lore);
  }

  private @NotNull ItemStack v2InfoItem(@NotNull SkillTree tree, @NotNull SkillTreeState state) {
    long unlockedCount = state.nodeLevels().values().stream().filter(l -> l > 0).count();
    List<String> lore =
        List.of(
            "Available SP: " + tree.availablePoints(state),
            "Total SP: " + state.totalSkillPoints(),
            "Unlocked: " + unlockedCount + "/" + tree.nodes().size(),
            "",
            "Job Level: " + state.jobLevel(),
            "SP per level: " + tree.skillPointsPerLevel());
    return PaperItemFactory.of(Material.BOOK, "Skill Tree Info", lore);
  }

  private @NotNull String formatV2Effect(@NotNull NodeEffect effect) {
    return switch (effect) {
      case NodeEffect.BoostEffect boost ->
          String.format("+%.0f%% %s", (boost.multiplier().doubleValue() - 1) * 100, boost.target());
      case NodeEffect.RuledBoostEffect ruled -> {
        String desc = ruled.boostSource().description();
        yield desc != null ? desc : String.format("Conditional %s boost", ruled.target());
      }
      case NodeEffect.PermissionEffect perm ->
          String.format("Permission: %s", String.join(", ", perm.permissions()));
      case NodeEffect.RecipeUnlockEffect recipe ->
          String.format("Recipe: %s", recipe.recipeKey().asString());
      case NodeEffect.CapabilityEffect capability ->
          String.format(
              "Capability: %s (schema %d)", capability.key().asString(), capability.schema());
      case NodeEffect.StateSetEffect stateSet ->
          stateSet.remove()
              ? String.format("Removes %s", stateSet.key().asString())
              : String.format("Sets %s = %s", stateSet.key().asString(), stateSet.value());
    };
  }

  private @NotNull String formatEffect(@NotNull UpgradeEffect effect) {
    return switch (effect) {
      case UpgradeEffect.BoostEffect boost ->
          String.format("+%.0f%% %s", (boost.multiplier().doubleValue() - 1) * 100, boost.target());
      case UpgradeEffect.RuledBoostEffect ruled -> {
        String desc = ruled.boostSource().description();
        yield desc != null ? desc : String.format("Conditional %s boost", ruled.target());
      }
      case UpgradeEffect.PermissionEffect perm ->
          String.format("Permission: %s", perm.permission());
    };
  }

  private @NotNull String getShortKey(@NotNull UpgradeNode node) {
    String full = node.key().asString();
    int colonIndex = full.indexOf(':');
    return colonIndex >= 0 ? full.substring(colonIndex + 1) : full;
  }

  private void handleV2NodeClick(
      @NotNull Player player,
      @NotNull GuiSession session,
      @NotNull String nodeKey,
      @NotNull String playerId,
      @NotNull String jobKey) {
    SkillNode node = session.skillTree.node(nodeKey).orElse(null);
    if (node != null && node.isMajor()) {
      if (session.pendingMajorKey != null && !session.pendingMajorKey.equals(nodeKey)) {
        session.pendingMajorKey = null;
        refresh(player);
      }
      if (session.pendingMajorKey == null) {
        SkillTreeState state = upgradeService.getSkillTreeState(playerId, jobKey);
        if (session.skillTree.canPurchase(state, nodeKey)) {
          session.pendingMajorKey = nodeKey;
          refresh(player);
        } else if (state.levelOf(nodeKey) > 0) {
          player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
        } else {
          player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
      }
      return;
    }

    if (session.pendingMajorKey != null) {
      session.pendingMajorKey = null;
      refresh(player);
    }

    PurchaseResult result = upgradeService.purchaseSkillLevel(playerId, jobKey, nodeKey);
    handleSkillPurchase(player, result);
  }

  private void purchaseMajor(
      @NotNull Player player, @NotNull GuiSession session, @NotNull String nodeKey) {
    String playerId = player.getUniqueId().toString();
    String jobKey = session.job.key().value();
    PurchaseResult result = upgradeService.purchaseMajor(playerId, jobKey, nodeKey);
    session.pendingMajorKey = null;

    switch (result) {
      case PurchaseResult.Success success -> {
        Messages.send(
            player,
            "<accent>Chosen: <primary>"
                + success.node().name()
                + " <neutral>(<secondary>"
                + success.remainingPoints()
                + " SP remaining<neutral>)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        refresh(player);
      }
      case PurchaseResult.AlreadyOwned _ ->
          player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
      case PurchaseResult.ExcludedByChoice ec -> {
        Messages.send(
            player, "<error>Blocked by: <secondary>" + String.join(", ", ec.conflicting()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.RequirementsNotMet _ -> {
        Messages.send(player, "<error>Requirements not met.");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.PrerequisitesNotMet pn -> {
        Messages.send(
            player, "<error>Missing prerequisites: <secondary>" + String.join(", ", pn.missing()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.InsufficientPoints ip -> {
        Messages.send(
            player,
            "<error>Not enough SP! Need <secondary>"
                + ip.required()
                + "<error>, have <secondary>"
                + ip.available());
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.NodeNotFound nf -> {
        Messages.send(player, "<error>Node not found: " + nf.nodeKey());
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
      case PurchaseResult.TreeNotFound _ -> {
        Messages.send(player, "<error>No upgrade tree for this job.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
    }
    if (!(result instanceof PurchaseResult.Success)) {
      refresh(player);
    }
  }

  private void handleSkillPurchase(@NotNull Player player, @NotNull PurchaseResult result) {
    switch (result) {
      case PurchaseResult.Success success -> {
        Messages.send(
            player,
            "<accent>Unlocked: <primary>"
                + success.node().name()
                + " <neutral>(<secondary>"
                + success.remainingPoints()
                + " SP remaining<neutral>)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        refresh(player);
      }
      case PurchaseResult.AlreadyOwned _ ->
          player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
      case PurchaseResult.ExcludedByChoice ec -> {
        Messages.send(
            player, "<error>Blocked by: <secondary>" + String.join(", ", ec.conflicting()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.RequirementsNotMet _ -> {
        Messages.send(player, "<error>Requirements not met.");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.PrerequisitesNotMet pn -> {
        Messages.send(
            player, "<error>Missing prerequisites: <secondary>" + String.join(", ", pn.missing()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.InsufficientPoints ip -> {
        Messages.send(
            player,
            "<error>Not enough SP! Need <secondary>"
                + ip.required()
                + "<error>, have <secondary>"
                + ip.available());
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.NodeNotFound nf -> {
        Messages.send(player, "<error>Node not found: " + nf.nodeKey());
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
      case PurchaseResult.TreeNotFound _ -> {
        Messages.send(player, "<error>No upgrade tree for this job.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
    }
  }

  private void handleLegacyUnlock(
      @NotNull Player player,
      @NotNull String nodeKey,
      @NotNull String playerId,
      @NotNull String jobKey) {
    UnlockResult result = upgradeService.unlock(playerId, jobKey, nodeKey);
    switch (result) {
      case UnlockResult.Success success -> {
        Messages.send(
            player,
            "<accent>Unlocked: <primary>"
                + success.node().name()
                + " <neutral>(<secondary>"
                + success.remainingPoints()
                + " SP remaining<neutral>)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        refresh(player);
      }
      case UnlockResult.InsufficientPoints ip -> {
        Messages.send(
            player,
            "<error>Not enough SP! Need <secondary>"
                + ip.required()
                + "<error>, have <secondary>"
                + ip.available());
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.PrerequisitesNotMet pm -> {
        Messages.send(
            player, "<error>Missing prerequisites: <secondary>" + String.join(", ", pm.missing()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.ExcludedByChoice ec -> {
        Messages.send(
            player, "<error>Blocked by: <secondary>" + String.join(", ", ec.conflicting()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.AlreadyUnlocked _ ->
          player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
      case UnlockResult.NodeNotFound nf -> {
        Messages.send(player, "<error>Node not found: " + nf.nodeKey());
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
      case UnlockResult.TreeNotFound _ -> {
        Messages.send(player, "<error>No upgrade tree for this job.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
    }
  }

  private void handleScroll(
      @NotNull Player player, @NotNull GuiSession session, @NotNull String action) {
    int maxY =
        session.isV2()
            ? session.skillTree.nodes().stream()
                .map(SkillNode::position)
                .filter(pos -> pos != null)
                .mapToInt(Position::y)
                .max()
                .orElse(0)
            : session.tree.allNodes().stream()
                .map(UpgradeNode::position)
                .filter(pos -> pos != null)
                .mapToInt(Position::y)
                .max()
                .orElse(0);
    int maxScroll = Math.max(0, maxY - GUI_ROWS + 1);

    if ("up".equals(action) && session.scrollOffset > 0) {
      session.scrollOffset = Math.max(0, session.scrollOffset - GUI_ROWS);
    } else if ("down".equals(action) && session.scrollOffset < maxScroll) {
      session.scrollOffset = Math.min(maxScroll, session.scrollOffset + GUI_ROWS);
    } else {
      return;
    }
    refresh(player);
  }

  private static @NotNull String sanitize(@NotNull String key) {
    return key.replace(':', '.').replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
  }

  private static @NotNull String plain(@NotNull Component component) {
    return PLAIN.serialize(component);
  }

  private enum NodeStatus {
    UNLOCKED,
    AVAILABLE,
    LOCKED,
    EXCLUDED
  }

  private static @NotNull Material materialFromName(@NotNull String name) {
    if (name == null || name.isBlank()) {
      return Material.BARRIER;
    }
    String bare = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
    try {
      return Material.valueOf(bare.toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return Material.BARRIER;
    }
  }

  private static final class GridPoint {
    final int column;
    final int row;

    GridPoint(int column, int row) {
      this.column = column;
      this.row = row;
    }

    @Override
    public boolean equals(@NotNull Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof GridPoint that)) {
        return false;
      }
      return column == that.column && row == that.row;
    }

    @Override
    public int hashCode() {
      return 31 * column + row;
    }
  }
}
