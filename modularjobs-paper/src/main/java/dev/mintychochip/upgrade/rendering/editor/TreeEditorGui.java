package dev.mintychochip.upgrade.rendering.editor;

import dev.mintychochip.gui.PaperItemFactory;
import dev.mintychochip.gui.PaperUiHost;
import dev.mintychochip.gui.PaperUiHost.ScreenView;
import dev.mintychochip.upgrade.UpgradeTree;
import dev.mintychochip.upgrade.config.UpgradeTreeLoader;
import dev.mintychochip.upgrade.rendering.Position;
import dev.mintychochip.util.Messages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Upgrade tree visual editor using the native Paper inventory host.
 *
 * <p>Toolbar controls live in the bottom inventory row. Sub-editors (node/settings) are separate
 * native views opened by host actions.
 */
public final class TreeEditorGui {

  private static final int GUI_ROWS = 6;
  private static final int GUI_COLS = 9;
  private static final int GUI_SIZE = 54;
  private static final int CANVAS_SLOTS = 45; // rows 0-4; row 5 = toolbar
  private static final int TOOLBAR_START = 45;
  private static final String MENU_ID = "tree_editor";

  private final PaperUiHost uiHost;
  private final TreeEditorExporter exporter;
  private final UpgradeTreeLoader treeLoader;
  private final TreeEditorNodeGui nodeEditorGui;
  private final TreeEditorSettingsGui settingsGui;
  private final Map<UUID, EditorSession> sessions = new HashMap<>();
  private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
  private final Set<UUID> transitioningToSubGui = new HashSet<>();
  private final Set<UUID> transitioningToMainGui = new HashSet<>();
  private final Map<UUID, Map<Integer, String>> slotNodes = new HashMap<>();
  private final Map<UUID, Map<Integer, String>> slotControls = new HashMap<>();

  /** Tree editor gui. */
  public TreeEditorGui(
      @NotNull Plugin plugin,
      @NotNull PaperUiHost uiHost,
      @NotNull TreeEditorExporter exporter,
      @NotNull UpgradeTreeLoader treeLoader,
      @NotNull TreeEditorNodeGui nodeEditorGui,
      @NotNull TreeEditorSettingsGui settingsGui) {
    // plugin reserved for future editor messaging/scheduling hooks
    if (plugin == null) {
      throw new IllegalArgumentException("plugin must not be null");
    }
    this.uiHost = uiHost;
    this.exporter = exporter;
    this.treeLoader = treeLoader;
    this.nodeEditorGui = nodeEditorGui;
    this.settingsGui = settingsGui;
    nodeEditorGui.setMainEditor(this);
    settingsGui.setMainEditor(this);
  }

  /** Open. */
  public void open(@NotNull Player player, @NotNull UpgradeTree tree) {
    openEditor(player, EditorTree.fromUpgradeTree(tree));
  }

  /** Open new. */
  public void openNew(@NotNull Player player, @NotNull String jobKey) {
    openEditor(player, EditorTree.createBlank(jobKey));
  }

  private void openEditor(@NotNull Player player, @NotNull EditorTree tree) {
    // Host.open closes any existing view first. Let its callback restore the previous editor
    // before replacing the session and saved inventory.
    uiHost.close(player);
    UUID playerId = player.getUniqueId();
    EditorSession session = new EditorSession(playerId, tree);
    sessions.put(playerId, session);
    savedInventories.put(playerId, player.getInventory().getContents().clone());
    player.getInventory().clear();
    uiHost.open(player, buildView(player, session));
  }

  /** Refresh. */
  public void refresh(@NotNull Player player) {
    UUID playerId = player.getUniqueId();
    EditorSession session = sessions.get(playerId);
    if (session == null) {
      return;
    }
    uiHost.refresh(player, buildView(player, session));
  }

  public @NotNull Optional<EditorSession> getSession(@NotNull Player player) {
    return Optional.ofNullable(sessions.get(player.getUniqueId()));
  }

  /** Reopen for. */
  public void reopenFor(@NotNull Player player) {
    UUID playerId = player.getUniqueId();
    EditorSession session = sessions.get(playerId);
    if (session == null) {
      return;
    }
    uiHost.open(player, buildView(player, session));
  }

  /** Marks the next main-view close as a transition into a sub-editor. */
  public void transitionToSubGui(@NotNull Player player) {
    transitioningToSubGui.add(player.getUniqueId());
  }

  /** Marks the sub-editor close as a return to the main editor. */
  public void transitionToMainGui(@NotNull Player player) {
    transitioningToMainGui.add(player.getUniqueId());
  }

  /** Handles a sub-editor close that is not a transition back to the main editor. */
  public void onSubEditorClosed(@NotNull Player player) {
    if (transitioningToMainGui.remove(player.getUniqueId())) {
      return;
    }
    onSessionClosed(player);
  }

  /** On canvas click. */
  public void onCanvasClick(@NotNull Player player, @NotNull InventoryClickEvent event) {
    if (!isSupportedClick(event.getClick())) {
      return;
    }
    UUID audience = player.getUniqueId();
    EditorSession session = sessions.get(audience);
    if (session == null) {
      return;
    }
    handleEmptySlotClick(player, session, event.getRawSlot());
  }

  /** On node click. */
  public void onNodeClick(@NotNull Player player, @NotNull InventoryClickEvent event) {
    UUID audience = player.getUniqueId();
    if (!isSupportedClick(event.getClick())) {
      return;
    }
    EditorSession session = sessions.get(audience);
    if (session == null) {
      return;
    }
    Map<Integer, String> nodes = slotNodes.get(audience);
    if (nodes == null) {
      return;
    }
    String nodeId = nodes.get(event.getRawSlot());
    if (nodeId == null) {
      return;
    }
    Optional<EditorNode> nodeOpt = session.tree().getNode(nodeId);
    if (nodeOpt.isEmpty()) {
      return;
    }
    handleNodeClick(player, session, nodeOpt.get(), event);
  }

  /** On control click. */
  public void onControlClick(@NotNull Player player, @NotNull InventoryClickEvent event) {
    UUID audience = player.getUniqueId();
    if (!isSupportedClick(event.getClick())) {
      return;
    }
    EditorSession session = sessions.get(audience);
    if (session == null) {
      return;
    }
    Map<Integer, String> controls = slotControls.get(audience);
    if (controls == null) {
      return;
    }
    String action = controls.get(event.getRawSlot());
    if (action != null) {
      handleControlAction(player, session, action);
    }
  }

  @NotNull
  ScreenView buildView(@NotNull Player player, @NotNull EditorSession session) {
    final UUID audience = player.getUniqueId();
    Map<Integer, ItemStack> items = new HashMap<>();
    Map<Integer, PaperUiHost.SlotAction> actions = new HashMap<>();
    Map<Integer, String> nodes = new HashMap<>();
    final Map<Integer, String> controls = new HashMap<>();

    EditorTree tree = session.tree();
    int scrollX = session.scrollOffsetX();
    int scrollY = session.scrollOffsetY();

    for (int i = 0; i < GUI_SIZE; i++) {
      items.put(i, PaperItemFactory.pane(Material.GRAY_STAINED_GLASS_PANE));
    }
    for (int i = 0; i < CANVAS_SLOTS; i++) {
      items.put(i, PaperItemFactory.pane(Material.BLACK_STAINED_GLASS_PANE));
      actions.put(i, this::onCanvasClick);
    }

    // Path points
    for (Position path : tree.paths()) {
      int sx = path.x() - scrollX;
      int sy = path.y() - scrollY;
      if (sx < 0 || sx >= GUI_COLS || sy < 0 || sy >= 5) {
        continue;
      }
      int slot = sy * GUI_COLS + sx;
      items.put(slot, PaperItemFactory.pane(Material.GRAY_STAINED_GLASS_PANE));
      actions.put(slot, this::onCanvasClick);
    }

    // Nodes
    for (EditorNode node : tree.nodes().values()) {
      Position pos = node.position();
      if (pos == null) {
        continue;
      }
      int sx = pos.x() - scrollX;
      int sy = pos.y() - scrollY;
      if (sx < 0 || sx >= GUI_COLS || sy < 0 || sy >= 5) {
        continue;
      }
      int slot = sy * GUI_COLS + sx;
      boolean selected = node.id().equals(session.selectedNodeId());
      Material mat = node.icon() != null ? node.icon() : Material.PAPER;
      String label = (selected ? "★ " : "") + node.name();
      List<String> lore = new ArrayList<>();
      lore.add("ID: " + node.id());
      lore.add("Left: select | Right: edit | Shift: link | Q: delete");
      items.put(slot, PaperItemFactory.of(mat, label, lore));
      nodes.put(slot, node.id());
      actions.put(slot, this::onNodeClick);
    }

    // Toolbar row
    putControl(items, actions, controls, TOOLBAR_START, Material.ARROW, "scroll_up", "Scroll Up");
    putControl(
        items, actions, controls, TOOLBAR_START + 1, Material.ARROW, "scroll_down", "Scroll Down");
    putControl(
        items, actions, controls, TOOLBAR_START + 2, Material.EMERALD, "add_node", "Add Node");
    putControl(items, actions, controls, TOOLBAR_START + 3, Material.IRON_AXE, "undo", "Undo");
    putControl(items, actions, controls, TOOLBAR_START + 4, Material.GOLDEN_AXE, "redo", "Redo");
    putControl(
        items, actions, controls, TOOLBAR_START + 5, Material.WRITABLE_BOOK, "save", "Save Tree");
    Material pathMat = session.isPathEditMode() ? Material.LEAD : Material.STRING;
    String pathLabel = session.isPathEditMode() ? "PATH MODE (Active)" : "Edit Paths";
    putControl(items, actions, controls, TOOLBAR_START + 6, pathMat, "path_edit", pathLabel);
    putControl(
        items,
        actions,
        controls,
        TOOLBAR_START + 7,
        Material.REDSTONE_TORCH,
        "settings",
        "Tree Settings");

    slotNodes.put(audience, Map.copyOf(nodes));
    slotControls.put(audience, Map.copyOf(controls));

    return new ScreenView(
        MENU_ID,
        GUI_ROWS,
        net.kyori.adventure.text.Component.text(
            trim("Tree Editor: " + session.tree().displayName())),
        items,
        actions,
        this::onSessionClosed);
  }

  private void putControl(
      @NotNull Map<Integer, ItemStack> items,
      @NotNull Map<Integer, PaperUiHost.SlotAction> actions,
      @NotNull Map<Integer, String> controls,
      int index,
      @NotNull Material material,
      @NotNull String action,
      @NotNull String label) {
    items.put(index, PaperItemFactory.of(material, label, List.of()));
    controls.put(index, action);
    actions.put(index, this::onControlClick);
  }

  private void handleNodeClick(
      @NotNull Player player,
      @NotNull EditorSession session,
      @NotNull EditorNode node,
      @NotNull InventoryClickEvent event) {
    EditorTree tree = session.tree();
    ClickType kind = event.getClick();

    if (kind == ClickType.RIGHT) {
      transitionToSubGui(player);
      nodeEditorGui.open(player, session, node);
      return;
    }

    if (kind == ClickType.DROP) {
      if (node.id().equals(tree.rootNodeId())) {
        Messages.send(player, "<error>Cannot delete root node!");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        return;
      }
      session.saveSnapshot();
      tree.removeNode(node.id());
      if (node.id().equals(session.selectedNodeId())) {
        session.selectNode(null);
      }
      Messages.send(player, "<accent>Deleted node: <secondary>" + node.id());
      refresh(player);
      player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
      return;
    }

    if (kind == ClickType.SHIFT_LEFT || kind == ClickType.SHIFT_RIGHT) {
      String selectedId = session.selectedNodeId();
      if (selectedId == null || selectedId.equals(node.id())) {
        Messages.send(player, "<error>Select a different node first!");
        return;
      }
      Optional<EditorNode> selectedOpt = tree.getNode(selectedId);
      if (selectedOpt.isEmpty()) {
        return;
      }
      EditorNode selected = selectedOpt.get();
      session.saveSnapshot();
      if (selected.children().contains(node.id())) {
        selected.children().remove(node.id());
        node.prerequisites().remove(selectedId);
        Messages.send(
            player, "<accent>Removed link: <secondary>" + selectedId + " -> " + node.id());
      } else {
        selected.children().add(node.id());
        node.prerequisites().add(selectedId);
        Messages.send(player, "<success>Added link: <secondary>" + selectedId + " -> " + node.id());
      }
      refresh(player);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
      return;
    }

    if (node.id().equals(session.selectedNodeId())) {
      session.selectNode(null);
      Messages.send(player, "<accent>Deselected node");
    } else {
      session.selectNode(node.id());
      Messages.send(player, "<accent>Selected: <secondary>" + node.id());
    }
    refresh(player);
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.2f);
  }

  private void handleControlAction(
      @NotNull Player player, @NotNull EditorSession session, @NotNull String action) {
    switch (action) {
      case "scroll_up" -> {
        if (session.scrollOffsetY() > 0) {
          session.setScrollOffsetY(Math.max(0, session.scrollOffsetY() - 5));
          refresh(player);
        }
      }
      case "scroll_down" -> {
        session.setScrollOffsetY(session.scrollOffsetY() + 5);
        refresh(player);
      }
      case "add_node" -> {
        Messages.send(player, "<accent>Click an empty slot to place a new node");
        session.setDragging(true);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
      }
      case "undo" -> {
        if (session.undo()) {
          Messages.send(player, "<accent>Undone");
          refresh(player);
        }
      }
      case "redo" -> {
        if (session.redo()) {
          Messages.send(player, "<accent>Redone");
          refresh(player);
        }
      }
      case "save" -> {
        EditorTree tree = session.tree();
        String json = exporter.exportSingle(tree);
        String treeId = tree.treeId();
        if (treeLoader.saveTree(treeId, json)) {
          Messages.send(
              player,
              "<success>Saved tree '<secondary>"
                  + treeId
                  + "<success>' to <primary>upgrade_trees/"
                  + treeId
                  + ".json");
          player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.5f, 1.0f);
        } else {
          Messages.send(player, "<error>Failed to save tree. Check server logs.");
          player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        }
      }
      case "settings" -> {
        transitionToSubGui(player);
        settingsGui.open(player, session);
      }
      case "path_edit" -> {
        session.setPathEditMode(!session.isPathEditMode());
        Messages.send(
            player,
            session.isPathEditMode()
                ? "<accent>Path edit mode <success>enabled"
                : "<accent>Path edit mode <error>disabled");
        refresh(player);
      }
      default -> {}
    }
  }

  private void handleEmptySlotClick(
      @NotNull Player player, @NotNull EditorSession session, int slot) {
    if (slot >= CANVAS_SLOTS) {
      return;
    }
    EditorTree tree = session.tree();
    int scrollX = session.scrollOffsetX();
    int scrollY = session.scrollOffsetY();
    int canvasX = slot % GUI_COLS;
    int canvasY = slot / GUI_COLS;
    int worldX = canvasX + scrollX;
    int worldY = canvasY + scrollY;
    Position newPos = new Position(worldX, worldY);

    if (session.isDragging()) {
      session.setDragging(false);
      session.saveSnapshot();
      String nodeId = "node_" + System.currentTimeMillis();
      EditorNode newNode = new EditorNode();
      newNode.setId(nodeId);
      newNode.setPosition(newPos);
      tree.addNode(newNode);
      Messages.send(player, "<success>Created node: <secondary>" + nodeId);
      refresh(player);
      return;
    }

    if (session.isPathEditMode()) {
      boolean pathExists = tree.paths().stream().anyMatch(p -> p.x() == worldX && p.y() == worldY);
      if (pathExists) {
        session.saveSnapshot();
        tree.paths().removeIf(p -> p.x() == worldX && p.y() == worldY);
        Messages.send(player, "<accent>Removed path point at (" + worldX + ", " + worldY + ")");
      } else {
        session.saveSnapshot();
        tree.paths().add(newPos);
        Messages.send(player, "<accent>Added path point at (" + worldX + ", " + worldY + ")");
      }
      refresh(player);
      return;
    }

    String selectedId = session.selectedNodeId();
    if (selectedId != null) {
      Optional<EditorNode> selectedOpt = tree.getNode(selectedId);
      if (selectedOpt.isPresent()) {
        session.saveSnapshot();
        selectedOpt.get().setPosition(newPos);
        Messages.send(
            player,
            "<accent>Moved <secondary>"
                + selectedId
                + " <accent>to ("
                + worldX
                + ", "
                + worldY
                + ")");
        refresh(player);
      }
    }
  }

  /** Called when the native host closes the main editor view. */
  public void onSessionClosed(@NotNull Player player) {
    UUID audience = player.getUniqueId();
    slotNodes.remove(audience);
    slotControls.remove(audience);
    if (transitioningToSubGui.remove(audience)) {
      return;
    }
    EditorSession session = sessions.remove(audience);
    ItemStack[] saved = savedInventories.remove(audience);
    if (session == null || saved == null) {
      return;
    }
    player.getInventory().clear();
    player.getInventory().setContents(saved);
    player.updateInventory();
    Messages.send(
        player, "<accent>Closed tree editor. Use <secondary>/jobs treeeditor<accent> to reopen.");
  }

  private static @NotNull String trim(@NotNull String title) {
    return title.length() > 128 ? title.substring(0, 128) : title;
  }

  private static boolean isSupportedClick(@NotNull ClickType click) {
    return click == ClickType.LEFT
        || click == ClickType.RIGHT
        || click == ClickType.SHIFT_LEFT
        || click == ClickType.SHIFT_RIGHT
        || click == ClickType.DROP;
  }
}
