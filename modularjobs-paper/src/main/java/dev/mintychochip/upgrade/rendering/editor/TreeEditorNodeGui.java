package dev.mintychochip.upgrade.rendering.editor;

import dev.mintychochip.gui.PaperItemFactory;
import dev.mintychochip.gui.PaperUiHost;
import dev.mintychochip.gui.PaperUiHost.ScreenView;
import dev.mintychochip.util.Messages;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

/** Node property editor as a native Paper inventory view. */
public final class TreeEditorNodeGui implements Listener {

  private static final int GUI_SIZE = 54;
  private static final String MENU_ID = "tree_editor_node";

  private final Plugin plugin;
  private final PaperUiHost uiHost;

  private TreeEditorGui mainEditor;

  private final Map<UUID, NodeEditSession> editSessions = new HashMap<>();
  private final Map<UUID, Map<Integer, String>> slotActions = new HashMap<>();
  private final Map<UUID, ChatInputHandler> chatInputHandlers = new HashMap<>();
  private final Set<UUID> preservingForChat = new HashSet<>();
  private boolean chatListenerRegistered;

  private record NodeEditSession(@NotNull EditorSession editorSession, @NotNull EditorNode node) {}

  @FunctionalInterface
  private interface ChatInputHandler {
    void handle(@NotNull String input);
  }

  /** Tree editor node gui. */
  public TreeEditorNodeGui(@NotNull Plugin plugin, @NotNull PaperUiHost uiHost) {
    this.plugin = plugin;
    this.uiHost = uiHost;
  }

  public void setMainEditor(@Nullable TreeEditorGui mainEditor) {
    this.mainEditor = mainEditor;
  }

  /** Open. */
  public void open(
      @NotNull Player player, @NotNull EditorSession session, @NotNull EditorNode node) {
    UUID playerId = player.getUniqueId();
    editSessions.put(playerId, new NodeEditSession(session, node));
    ensureChatListener();
    uiHost.open(player, buildView(player, session, node));
  }

  /** On action. */
  public void onAction(@NotNull Player player, @NotNull InventoryClickEvent event) {
    UUID audience = player.getUniqueId();
    if (!isSupportedClick(event.getClick())) {
      return;
    }
    NodeEditSession edit = editSessions.get(audience);
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
  ScreenView buildView(
      @NotNull Player player, @NotNull EditorSession session, @NotNull EditorNode node) {
    final UUID audience = player.getUniqueId();
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
    Material icon = node.icon() != null ? node.icon() : Material.PAPER;
    items.put(4, PaperItemFactory.of(icon, node.name(), List.of("ID: " + node.id())));

    put(
        items,
        actions,
        actionIds,
        10,
        Material.NAME_TAG,
        "name",
        "Name",
        List.of("Current: " + node.name(), "Click to edit"));
    put(
        items,
        actions,
        actionIds,
        11,
        Material.WRITABLE_BOOK,
        "description",
        "Description",
        List.of(
            "Current: " + (node.description() != null ? node.description() : "(none)"),
            "Click to edit"));
    put(
        items,
        actions,
        actionIds,
        12,
        icon,
        "icon",
        "Icon",
        List.of("Current: " + icon.name(), "Click to cycle materials"));
    put(
        items,
        actions,
        actionIds,
        13,
        Material.DIAMOND,
        "cost",
        "Cost (SP)",
        List.of("Current: " + node.cost(), "Left +1 | Right -1 | Shift ±5"));
    put(
        items,
        actions,
        actionIds,
        19,
        Material.ENCHANTED_BOOK,
        "perk_id",
        "Perk ID",
        List.of(
            "Current: " + (node.perkId().isEmpty() ? "(none)" : node.perkId()), "Click to edit"));
    put(
        items,
        actions,
        actionIds,
        20,
        Material.EXPERIENCE_BOTTLE,
        "level",
        "Perk Level",
        List.of("Current: " + node.level(), "Left +1 | Right -1"));
    put(
        items,
        actions,
        actionIds,
        21,
        Material.PURPLE_DYE,
        "archetype",
        "Archetype",
        List.of(
            "Current: " + (node.archetypeRef() != null ? node.archetypeRef() : "(none)"),
            "Click to cycle"));
    put(
        items,
        actions,
        actionIds,
        28,
        Material.BREWING_STAND,
        "add_effect",
        "Add Effect",
        List.of("Add a boost effect stub"));
    List<EditorEffect> effects = node.effects();
    for (int i = 0; i < Math.min(effects.size(), 7); i++) {
      EditorEffect effect = effects.get(i);
      put(
          items,
          actions,
          actionIds,
          29 + i,
          Material.GOLDEN_APPLE,
          "effect_" + i,
          "Effect: " + effect.type().name(),
          List.of(effect.getDisplayDescription(), "Shift+click to remove"));
    }
    put(
        items,
        actions,
        actionIds,
        37,
        Material.COMPASS,
        "position",
        "Position",
        List.of(
            "X: "
                + (node.position() != null ? node.position().x() : 0)
                + ", Y: "
                + (node.position() != null ? node.position().y() : 0)));
    put(
        items,
        actions,
        actionIds,
        49,
        Material.BARRIER,
        "delete",
        "Delete Node",
        List.of("Permanently remove this node"));

    slotActions.put(audience, Map.copyOf(actionIds));
    return new ScreenView(
        MENU_ID,
        6,
        net.kyori.adventure.text.Component.text(trim("Edit Node: " + node.name())),
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
      @NotNull NodeEditSession edit,
      @NotNull String action,
      @NotNull ClickType kind) {
    EditorNode node = edit.node();
    EditorSession session = edit.editorSession();
    session.saveSnapshot();

    switch (action) {
      case "back" -> {
        editSessions.remove(player.getUniqueId());
        if (mainEditor != null) {
          mainEditor.transitionToMainGui(player);
          mainEditor.reopenFor(player);
        }
      }
      case "name" ->
          prompt(
              player,
              "Enter new name:",
              input -> {
                node.setName(input);
                Messages.send(player, "<success>Name set to: <secondary>" + input);
                reopen(player, edit);
              });
      case "description" ->
          prompt(
              player,
              "Enter description:",
              input -> {
                node.setDescription(input);
                Messages.send(player, "<success>Description updated");
                reopen(player, edit);
              });
      case "icon" -> {
        Material[] cycle = {
          Material.PAPER, Material.EMERALD, Material.DIAMOND, Material.GOLD_INGOT,
          Material.IRON_INGOT, Material.BOOK, Material.BLAZE_POWDER, Material.ENCHANTED_BOOK
        };
        Material current = node.icon() != null ? node.icon() : Material.PAPER;
        int idx = 0;
        for (int i = 0; i < cycle.length; i++) {
          if (cycle[i] == current) {
            idx = i;
            break;
          }
        }
        node.setIcon(cycle[(idx + 1) % cycle.length]);
        reopen(player, edit);
      }
      case "cost" -> {
        int delta = kind == ClickType.SHIFT_LEFT || kind == ClickType.SHIFT_RIGHT ? 5 : 1;
        if (kind == ClickType.RIGHT || kind == ClickType.SHIFT_RIGHT) {
          delta = -delta;
        }
        node.setCost(Math.max(0, node.cost() + delta));
        reopen(player, edit);
      }
      case "perk_id" ->
          prompt(
              player,
              "Enter perk id (or blank to clear):",
              input -> {
                node.setPerkId(input == null ? "" : input.trim());
                reopen(player, edit);
              });
      case "level" -> {
        int delta = kind == ClickType.RIGHT ? -1 : 1;
        node.setLevel(Math.max(0, node.level() + delta));
        reopen(player, edit);
      }
      case "archetype" -> {
        // Cycle among free-form: clear / warrior / mage / assassin
        String[] cycle = {null, "warrior", "mage", "assassin"};
        String current = node.archetypeRef();
        int idx = 0;
        for (int i = 0; i < cycle.length; i++) {
          if ((cycle[i] == null && current == null)
              || (cycle[i] != null && cycle[i].equals(current))) {
            idx = i;
            break;
          }
        }
        node.setArchetypeRef(cycle[(idx + 1) % cycle.length]);
        reopen(player, edit);
      }
      case "add_effect" -> {
        EditorEffect effect = new EditorEffect();
        effect.setType(EditorEffect.EffectType.BOOST);
        effect.setTarget("xp");
        effect.setAmount(1.1);
        node.effects().add(effect);
        Messages.send(player, "<success>Added boost effect stub");
        reopen(player, edit);
      }
      case "position" ->
          prompt(
              player,
              "Enter position as x,y:",
              input -> {
                try {
                  String[] parts = input.split(",");
                  int x = Integer.parseInt(parts[0].trim());
                  int y = Integer.parseInt(parts[1].trim());
                  node.setPosition(new dev.mintychochip.upgrade.rendering.Position(x, y));
                  Messages.send(player, "<success>Position set to " + x + "," + y);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                  Messages.send(player, "<error>Invalid position. Use x,y");
                }
                reopen(player, edit);
              });
      case "delete" -> {
        if (node.id().equals(session.tree().rootNodeId())) {
          Messages.send(player, "<error>Cannot delete root node!");
          return;
        }
        session.tree().removeNode(node.id());
        editSessions.remove(player.getUniqueId());
        if (mainEditor != null) {
          mainEditor.transitionToMainGui(player);
          mainEditor.reopenFor(player);
        }
      }
      default -> {
        if (action.startsWith("effect_")) {
          int index = Integer.parseInt(action.substring("effect_".length()));
          if (kind == ClickType.SHIFT_LEFT || kind == ClickType.SHIFT_RIGHT) {
            if (index >= 0 && index < node.effects().size()) {
              node.effects().remove(index);
              Messages.send(player, "<accent>Removed effect");
              reopen(player, edit);
            }
          }
        }
      }
    }
  }

  private void reopen(@NotNull Player player, @NotNull NodeEditSession edit) {
    UUID playerId = player.getUniqueId();
    editSessions.put(playerId, edit);
    uiHost.refresh(player, buildView(player, edit.editorSession(), edit.node()));
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
