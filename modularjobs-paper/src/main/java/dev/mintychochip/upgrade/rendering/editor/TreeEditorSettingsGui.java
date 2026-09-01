package dev.mintychochip.upgrade.rendering.editor;

import dev.mintychochip.gui.PaperItemFactory;
import dev.mintychochip.gui.PaperUiHost;
import dev.mintychochip.gui.PaperUiHost.ScreenView;
import dev.mintychochip.util.Messages;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Tree-level settings editor via a native Paper inventory. */
public final class TreeEditorSettingsGui implements Listener {

  private static final int GUI_SIZE = 54;
  private static final String MENU_ID = "tree_editor_settings";

  private final Plugin plugin;
  private final PaperUiHost uiHost;

  private TreeEditorGui mainEditor;

  private final Map<UUID, SettingsEditSession> editSessions = new HashMap<>();
  private final Map<UUID, Map<Integer, String>> slotActions = new HashMap<>();
  private final java.util.Set<UUID> preservingForChat = new java.util.HashSet<>();
  private final Map<UUID, ChatInputHandler> chatInputHandlers = new HashMap<>();
  private boolean chatListenerRegistered;

  private record SettingsEditSession(@NotNull EditorSession editorSession) {}

  @FunctionalInterface
  private interface ChatInputHandler {
    void handle(@NotNull String input);
  }

  /** Tree editor settings gui. */
  public TreeEditorSettingsGui(@NotNull Plugin plugin, @NotNull PaperUiHost uiHost) {
    this.plugin = plugin;
    this.uiHost = uiHost;
  }

  public void setMainEditor(@Nullable TreeEditorGui mainEditor) {
    this.mainEditor = mainEditor;
  }

  /** Open. */
  public void open(@NotNull Player player, @NotNull EditorSession session) {
    UUID playerId = player.getUniqueId();
    editSessions.put(playerId, new SettingsEditSession(session));
    ensureChatListener();
    uiHost.open(player, buildView(player, session));
  }

  /** On action. */
  public void onAction(@NotNull Player player, @NotNull InventoryClickEvent event) {
    UUID audience = player.getUniqueId();
    if (!isSupportedClick(event.getClick())) {
      return;
    }
    SettingsEditSession edit = editSessions.get(audience);
    if (edit == null) {
      return;
    }
    Map<Integer, String> actions = slotActions.get(audience);
    if (actions == null) {
      return;
    }
    String action = actions.get(event.getRawSlot());
    if (action != null) {
      handleAction(player, edit, action, event.getClick());
    }
  }

  @NotNull
  ScreenView buildView(@NotNull Player player, @NotNull EditorSession session) {
    final UUID audience = player.getUniqueId();
    EditorTree tree = session.tree();
    Map<Integer, String> actionIds = new HashMap<>();
    Map<Integer, ItemStack> items = new HashMap<>();
    Map<Integer, PaperUiHost.SlotAction> actions = new HashMap<>();
    for (int i = 0; i < GUI_SIZE; i++) {
      items.put(i, PaperItemFactory.pane(Material.GRAY_STAINED_GLASS_PANE));
    }

    put(
        items,
        actions,
        actionIds,
        0,
        Material.ARROW,
        "back",
        "Back",
        List.of("Return to tree editor"));
    items.put(
        4,
        PaperItemFactory.of(
            Material.OAK_SIGN,
            tree.treeId(),
            List.of("Job: " + tree.jobKey(), "Nodes: " + tree.nodes().size())));
    put(
        items,
        actions,
        actionIds,
        10,
        Material.NAME_TAG,
        "display_name",
        "Display Name",
        List.of("Current: " + tree.displayName(), "Click to edit"));
    put(
        items,
        actions,
        actionIds,
        11,
        Material.PAPER,
        "tree_id",
        "Tree ID",
        List.of("Current: " + tree.treeId(), "Click to edit"));
    put(
        items,
        actions,
        actionIds,
        12,
        Material.EXPERIENCE_BOTTLE,
        "sp_per_level",
        "SP per Level",
        List.of("Current: " + tree.skillPointsPerLevel(), "Left +1 | Right -1"));
    put(
        items,
        actions,
        actionIds,
        19,
        Material.BOOK,
        "job_key",
        "Job Key",
        List.of("Current: " + tree.jobKey(), "Click to edit"));

    slotActions.put(audience, Map.copyOf(actionIds));
    return new ScreenView(
        MENU_ID,
        6,
        net.kyori.adventure.text.Component.text(trim("Tree Settings: " + tree.displayName())),
        items,
        actions,
        this::onSessionClosed);
  }

  private void put(
      @NotNull Map<Integer, ItemStack> items,
      @NotNull Map<Integer, PaperUiHost.SlotAction> actions,
      @NotNull Map<Integer, String> actionIds,
      int index,
      @NotNull Material material,
      @NotNull String action,
      @NotNull String label,
      @NotNull List<String> lore) {
    items.put(index, PaperItemFactory.of(material, label, lore));
    actionIds.put(index, action);
    actions.put(index, this::onAction);
  }

  private void handleAction(
      @NotNull Player player,
      @NotNull SettingsEditSession edit,
      @NotNull String action,
      @NotNull ClickType kind) {
    EditorTree tree = edit.editorSession().tree();
    edit.editorSession().saveSnapshot();

    switch (action) {
      case "back" -> {
        editSessions.remove(player.getUniqueId());
        if (mainEditor != null) {
          mainEditor.transitionToMainGui(player);
          mainEditor.reopenFor(player);
        }
      }
      case "display_name" ->
          prompt(
              player,
              "Enter display name:",
              input -> {
                tree.setDisplayName(input);
                reopen(player, edit);
              });
      case "tree_id" ->
          prompt(
              player,
              "Enter tree id:",
              input -> {
                tree.setTreeId(input.trim());
                reopen(player, edit);
              });
      case "job_key" ->
          prompt(
              player,
              "Enter job key:",
              input -> {
                tree.setJobKey(input.trim());
                reopen(player, edit);
              });
      case "sp_per_level" -> {
        int delta = kind == ClickType.RIGHT || kind == ClickType.SHIFT_RIGHT ? -1 : 1;
        tree.setSkillPointsPerLevel(Math.max(0, tree.skillPointsPerLevel() + delta));
        reopen(player, edit);
      }
      default -> {}
    }
  }

  private void reopen(@NotNull Player player, @NotNull SettingsEditSession edit) {
    UUID playerId = player.getUniqueId();
    editSessions.put(playerId, edit);
    uiHost.refresh(player, buildView(player, edit.editorSession()));
  }

  private void prompt(
      @NotNull Player player, @NotNull String message, @NotNull ChatInputHandler handler) {
    Messages.send(player, "<accent>" + message);
    chatInputHandlers.put(player.getUniqueId(), handler);
    preservingForChat.add(player.getUniqueId());
    uiHost.close(player);
  }

  private void onSessionClosed(@NotNull Player player) {
    UUID playerId = player.getUniqueId();
    editSessions.remove(playerId);
    slotActions.remove(playerId);
    if (preservingForChat.contains(playerId)) {
      return;
    }
    if (mainEditor != null) {
      mainEditor.onSubEditorClosed(player);
    }
  }

  private void ensureChatListener() {
    if (chatListenerRegistered) {
      return;
    }
    chatListenerRegistered = true;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  /** API member. */
  @EventHandler
  public void onChat(@NotNull AsyncPlayerChatEvent event) {
    ChatInputHandler handler = chatInputHandlers.remove(event.getPlayer().getUniqueId());
    preservingForChat.remove(event.getPlayer().getUniqueId());
    if (handler == null) {
      return;
    }
    event.setCancelled(true);
    String input = event.getMessage();
    Bukkit.getScheduler().runTask(plugin, () -> handler.handle(input));
  }

  @EventHandler
  public void onQuit(@NotNull PlayerQuitEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    if (editSessions.containsKey(playerId)) {
      onSessionClosed(event.getPlayer());
    } else if (preservingForChat.remove(playerId)) {
      chatInputHandlers.remove(playerId);
      slotActions.remove(playerId);
      if (mainEditor != null) {
        mainEditor.onSubEditorClosed(event.getPlayer());
      }
    }
  }

  private static @NotNull String trim(@NotNull String title) {
    return title.length() > 128 ? title.substring(0, 128) : title;
  }

  private static boolean isSupportedClick(@NotNull ClickType click) {
    return click == ClickType.LEFT
        || click == ClickType.RIGHT
        || click == ClickType.SHIFT_LEFT
        || click == ClickType.SHIFT_RIGHT;
  }
}
