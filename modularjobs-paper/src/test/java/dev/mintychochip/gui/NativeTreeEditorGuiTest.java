package dev.mintychochip.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.boost.BoostFactoryImpl;
import dev.mintychochip.config.YamlConfigTestPlugin;
import dev.mintychochip.editor.json.GsonProvider;
import dev.mintychochip.registry.SimpleRegistryImpl;
import dev.mintychochip.test.MockBukkitSupport;
import dev.mintychochip.upgrade.SkillTree;
import dev.mintychochip.upgrade.UpgradeTree;
import dev.mintychochip.upgrade.config.UpgradeTreeLoader;
import dev.mintychochip.upgrade.rendering.editor.EditorNode;
import dev.mintychochip.upgrade.rendering.editor.EditorSession;
import dev.mintychochip.upgrade.rendering.editor.TreeEditorExporter;
import dev.mintychochip.upgrade.rendering.editor.TreeEditorGui;
import dev.mintychochip.upgrade.rendering.editor.TreeEditorNodeGui;
import dev.mintychochip.upgrade.rendering.editor.TreeEditorSettingsGui;
import java.io.File;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class NativeTreeEditorGuiTest {
  private static ServerMock server;
  private static JavaPlugin plugin;

  @BeforeAll
  static void setUp() {
    server = MockBukkitSupport.mockServer();
    plugin = org.mockbukkit.mockbukkit.MockBukkit.loadSimple(YamlConfigTestPlugin.class);
  }

  @AfterAll
  static void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void mainCanvasAndToolbarActionsUseNativeSlots() {
    Fixture fixture = fixture("canvas");
    fixture.gui.openNew(fixture.player, "miner");

    Inventory top = fixture.player.getOpenInventory().getTopInventory();
    assertEquals(54, top.getSize());
    assertEquals(Material.BOOK, top.getItem(4).getType());
    assertEquals(Material.EMERALD, top.getItem(47).getType());

    fixture.click(4, ClickType.LEFT);
    EditorSession session = fixture.gui.getSession(fixture.player).orElseThrow();
    assertEquals("miner_basics", session.selectedNodeId());

    fixture.click(47, ClickType.LEFT);
    fixture.click(0, ClickType.LEFT);
    assertEquals(2, session.tree().nodes().size());
    String exported = new TreeEditorExporter().exportSingle(session.tree());
    assertTrue(exported.contains("\"layout\""));
    assertTrue(exported.contains("\"miner_basics\""));
  }

  @Test
  void nodeActionsEditPropertiesAndBackReturnsToMainWithoutLosingSession() {
    Fixture fixture = fixture("node");
    fixture.gui.openNew(fixture.player, "miner");
    fixture.click(4, ClickType.RIGHT);

    assertTrue(fixture.player.getOpenInventory().getTitle().startsWith("Edit Node:"));
    assertEquals(
        Material.NAME_TAG,
        fixture.player.getOpenInventory().getTopInventory().getItem(10).getType());
    fixture.click(12, ClickType.LEFT);
    EditorNode root =
        fixture
            .gui
            .getSession(fixture.player)
            .orElseThrow()
            .tree()
            .getNode("miner_basics")
            .orElseThrow();
    assertEquals(Material.BLAZE_POWDER, root.icon());

    fixture.click(0, ClickType.LEFT);
    assertTrue(fixture.player.getOpenInventory().getTitle().startsWith("Tree Editor:"));
    assertNotNull(fixture.gui.getSession(fixture.player).orElse(null));
  }

  @Test
  void settingsActionsMutateTreeAndBackReturnsToMain() {
    Fixture fixture = fixture("settings");
    fixture.gui.openNew(fixture.player, "miner");
    fixture.click(52, ClickType.LEFT);

    assertTrue(fixture.player.getOpenInventory().getTitle().startsWith("Tree Settings:"));
    fixture.click(12, ClickType.RIGHT);
    EditorSession session = fixture.gui.getSession(fixture.player).orElseThrow();
    assertEquals(0, session.tree().skillPointsPerLevel());

    fixture.click(0, ClickType.LEFT);
    assertTrue(fixture.player.getOpenInventory().getTitle().startsWith("Tree Editor:"));
    assertNotNull(fixture.gui.getSession(fixture.player).orElse(null));
  }

  @Test
  void nativeCloseClearsSessionAndRestoresPlayerInventoryExactlyOnce() {
    Fixture fixture = fixture("close");
    fixture.player.getInventory().setItem(0, new org.bukkit.inventory.ItemStack(Material.DIAMOND));
    fixture.gui.openNew(fixture.player, "miner");
    fixture.host.close(fixture.player);

    assertNull(fixture.gui.getSession(fixture.player).orElse(null));
    assertEquals(Material.DIAMOND, fixture.player.getInventory().getItem(0).getType());
    fixture.host.close(fixture.player);
    assertEquals(Material.DIAMOND, fixture.player.getInventory().getItem(0).getType());
  }

  @Test
  void toolbarPreservesBoundsUndoRedoAndPathMode() {
    Fixture fixture = fixture("toolbar");
    fixture.gui.openNew(fixture.player, "miner");
    EditorSession session = fixture.gui.getSession(fixture.player).orElseThrow();

    fixture.click(45, ClickType.LEFT);
    assertEquals(0, session.scrollOffsetY());
    fixture.click(46, ClickType.LEFT);
    assertEquals(5, session.scrollOffsetY());
    fixture.click(45, ClickType.LEFT);
    assertEquals(0, session.scrollOffsetY());

    fixture.click(51, ClickType.LEFT);
    fixture.click(0, ClickType.LEFT);
    assertEquals(1, session.tree().paths().size());
    fixture.click(0, ClickType.LEFT);
    assertEquals(0, session.tree().paths().size());

    fixture.click(47, ClickType.LEFT);
    fixture.click(0, ClickType.LEFT);
    assertEquals(2, session.tree().nodes().size());
    session.saveSnapshot();
    fixture.click(48, ClickType.LEFT);
    assertEquals(1, session.tree().nodes().size());
    fixture.click(49, ClickType.LEFT);
    assertEquals(2, session.tree().nodes().size());
  }

  @Test
  void unsupportedClickTypesDoNotMutateMainOrNodeEditors() {
    Fixture fixture = fixture("clicks");
    fixture.gui.openNew(fixture.player, "miner");
    fixture.click(47, ClickType.MIDDLE);
    fixture.click(0, ClickType.LEFT);
    EditorSession session = fixture.gui.getSession(fixture.player).orElseThrow();
    assertEquals(1, session.tree().nodes().size());

    fixture.click(4, ClickType.RIGHT);
    fixture.click(13, ClickType.DROP);
    EditorNode root = session.tree().getNode("miner_basics").orElseThrow();
    assertEquals(0, root.cost());
  }

  @Test
  void saveActionExportsAndWritesTreeConfig() {
    Fixture fixture = fixture("save");
    fixture.gui.openNew(fixture.player, "miner_save");
    fixture.click(50, ClickType.LEFT);
    File saved = new File(plugin.getDataFolder(), "upgrade_trees/miner_save_v1.json");
    assertTrue(saved.isFile());
    assertTrue(!fixture.loader.saveTree("../missing/tree", "{}"));
    fixture.host.close(fixture.player);
  }

  @Test
  void nodePromptReopensAndAppliesChatEdit() {
    Fixture fixture = fixture("node-chat");
    fixture.gui.openNew(fixture.player, "miner");
    fixture.click(4, ClickType.RIGHT);
    fixture.click(10, ClickType.LEFT);
    AsyncPlayerChatEvent chat =
        new AsyncPlayerChatEvent(
            false, fixture.player, "Renamed", java.util.Set.of(fixture.player));
    fixture.node.onChat(chat);
    server.getScheduler().performOneTick();
    EditorNode root =
        fixture
            .gui
            .getSession(fixture.player)
            .orElseThrow()
            .tree()
            .getNode("miner_basics")
            .orElseThrow();
    assertEquals("Renamed", root.name());
    assertTrue(fixture.player.getOpenInventory().getTitle().startsWith("Edit Node:"));
  }

  @Test
  void settingsPromptReopensAndAppliesChatEdit() {
    Fixture fixture = fixture("settings-chat");
    fixture.gui.openNew(fixture.player, "miner");
    fixture.click(52, ClickType.LEFT);
    fixture.click(10, ClickType.LEFT);
    AsyncPlayerChatEvent chat =
        new AsyncPlayerChatEvent(
            false, fixture.player, "Renamed Tree", java.util.Set.of(fixture.player));
    fixture.settings.onChat(chat);
    server.getScheduler().performOneTick();
    assertEquals(
        "Renamed Tree", fixture.gui.getSession(fixture.player).orElseThrow().tree().displayName());
    assertTrue(fixture.player.getOpenInventory().getTitle().startsWith("Tree Settings:"));
  }

  @Test
  void waitingNodeOrSettingsPromptQuitCleansMainSessionAndRestoresInventory() {
    Fixture nodeFixture = fixture("node-prompt-quit");
    nodeFixture
        .player
        .getInventory()
        .setItem(0, new org.bukkit.inventory.ItemStack(Material.DIAMOND));
    nodeFixture.gui.openNew(nodeFixture.player, "miner");
    nodeFixture.click(4, ClickType.RIGHT);
    nodeFixture.click(10, ClickType.LEFT);
    nodeFixture.node.onQuit(
        new PlayerQuitEvent(nodeFixture.player, net.kyori.adventure.text.Component.empty()));
    assertNull(nodeFixture.gui.getSession(nodeFixture.player).orElse(null));
    assertEquals(Material.DIAMOND, nodeFixture.player.getInventory().getItem(0).getType());

    Fixture settingsFixture = fixture("settings-prompt-quit");
    settingsFixture
        .player
        .getInventory()
        .setItem(0, new org.bukkit.inventory.ItemStack(Material.DIAMOND));
    settingsFixture.gui.openNew(settingsFixture.player, "miner");
    settingsFixture.click(52, ClickType.LEFT);
    settingsFixture.click(10, ClickType.LEFT);
    settingsFixture.settings.onQuit(
        new PlayerQuitEvent(settingsFixture.player, net.kyori.adventure.text.Component.empty()));
    assertNull(settingsFixture.gui.getSession(settingsFixture.player).orElse(null));
    assertEquals(Material.DIAMOND, settingsFixture.player.getInventory().getItem(0).getType());
  }

  @Test
  void subEditorCloseAndQuitRestoreInventoryAndClearMainSession() {
    Fixture closeFixture = fixture("sub-close");
    closeFixture
        .player
        .getInventory()
        .setItem(0, new org.bukkit.inventory.ItemStack(Material.DIAMOND));
    closeFixture.gui.openNew(closeFixture.player, "miner");
    closeFixture.click(4, ClickType.RIGHT);
    closeFixture.host.close(closeFixture.player);
    assertNull(closeFixture.gui.getSession(closeFixture.player).orElse(null));
    assertEquals(Material.DIAMOND, closeFixture.player.getInventory().getItem(0).getType());

    Fixture quitFixture = fixture("sub-quit");
    quitFixture
        .player
        .getInventory()
        .setItem(0, new org.bukkit.inventory.ItemStack(Material.DIAMOND));
    quitFixture.gui.openNew(quitFixture.player, "miner");
    quitFixture.click(52, ClickType.LEFT);
    quitFixture.host.onPlayerQuit(
        new PlayerQuitEvent(quitFixture.player, net.kyori.adventure.text.Component.empty()));
    assertNull(quitFixture.gui.getSession(quitFixture.player).orElse(null));
    assertEquals(Material.DIAMOND, quitFixture.player.getInventory().getItem(0).getType());
  }

  private static @NotNull Fixture fixture(@NotNull String name) {
    PlayerMock player = server.addPlayer("tree-" + name + "-" + UUID.randomUUID());
    PaperUiHost host = new PaperUiHost();
    TreeEditorNodeGui node = new TreeEditorNodeGui(plugin, host);
    TreeEditorSettingsGui settings = new TreeEditorSettingsGui(plugin, host);
    UpgradeTreeLoader loader =
        new UpgradeTreeLoader(
            plugin,
            GsonProvider.create(),
            new SimpleRegistryImpl<UpgradeTree>(),
            new SimpleRegistryImpl<SkillTree>(),
            BoostFactoryImpl.INSTANCE,
            BoostFactoryImpl.INSTANCE);
    TreeEditorExporter exporter = new TreeEditorExporter();
    TreeEditorGui gui = new TreeEditorGui(plugin, host, exporter, loader, node, settings);
    return new Fixture(player, host, gui, node, settings, loader);
  }

  private record Fixture(
      @NotNull PlayerMock player,
      @NotNull PaperUiHost host,
      @NotNull TreeEditorGui gui,
      @NotNull TreeEditorNodeGui node,
      @NotNull TreeEditorSettingsGui settings,
      @NotNull UpgradeTreeLoader loader) {
    private void click(int rawSlot, @NotNull ClickType click) {
      InventoryClickEvent event =
          new InventoryClickEvent(
              player.getOpenInventory(),
              SlotType.CONTAINER,
              rawSlot,
              click,
              InventoryAction.PICKUP_ALL);
      host.onInventoryClick(event);
    }
  }
}
