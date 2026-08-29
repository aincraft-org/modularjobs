package dev.mintychochip.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
class PaperUiHostTest {
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
  void openCreatesOwnedSixRowInventoryAndCopiesMaps() {
    PaperUiHost host = new PaperUiHost();
    PlayerMock player = server.addPlayer("host-open-" + UUID.randomUUID());
    ItemStack item = new ItemStack(Material.BOOK);
    AtomicInteger calls = new AtomicInteger();
    PaperUiHost.SlotAction action = (p, event) -> calls.incrementAndGet();
    Map<Integer, ItemStack> items = new HashMap<>();
    Map<Integer, PaperUiHost.SlotAction> actions = new HashMap<>();
    items.put(10, item);
    actions.put(10, action);

    PaperUiHost.ScreenView view =
        new PaperUiHost.ScreenView(
            "open", 6, Component.text("Open"), items, actions, ignored -> {});
    host.open(player, view);

    Inventory top = player.getOpenInventory().getTopInventory();
    assertTrue(top.getHolder() != null);
    assertEquals(PaperUiHost.class, top.getHolder().getClass().getEnclosingClass());
    assertEquals(item, top.getItem(10));
    items.clear();
    actions.clear();
    InventoryClickEvent copiedMapEvent = click(player.getOpenInventory(), 10);
    host.onInventoryClick(copiedMapEvent);
    assertTrue(copiedMapEvent.isCancelled());
    assertEquals(1, calls.get());
  }

  @Test
  void owningTopClickCancelsAndDispatchesOnlyMappedAction() {
    PaperUiHost host = new PaperUiHost();
    PlayerMock player = server.addPlayer("host-click-" + UUID.randomUUID());
    AtomicInteger calls = new AtomicInteger();
    PaperUiHost.ScreenView view =
        new PaperUiHost.ScreenView(
            "click",
            1,
            Component.text("Click"),
            Map.of(0, new ItemStack(Material.BOOK)),
            Map.of(0, (p, event) -> calls.incrementAndGet()),
            ignored -> {});
    host.open(player, view);

    InventoryClickEvent event = click(player.getOpenInventory(), 0);
    host.onInventoryClick(event);

    assertTrue(event.isCancelled());
    assertEquals(1, calls.get());
  }

  @Test
  void unrelatedAndBottomClicksDoNotDispatchAction() {
    PaperUiHost host = new PaperUiHost();
    PlayerMock owner = server.addPlayer("host-owner-" + UUID.randomUUID());
    PlayerMock unrelated = server.addPlayer("host-unrelated-" + UUID.randomUUID());
    AtomicInteger calls = new AtomicInteger();
    PaperUiHost.ScreenView view =
        new PaperUiHost.ScreenView(
            "routing",
            1,
            Component.text("Routing"),
            Map.of(0, new ItemStack(Material.BOOK)),
            Map.of(0, (p, event) -> calls.incrementAndGet()),
            ignored -> {});
    host.open(owner, view);

    unrelated.openInventory(owner.getOpenInventory().getTopInventory());
    InventoryClickEvent unrelatedEvent = click(unrelated.getOpenInventory(), 0);
    host.onInventoryClick(unrelatedEvent);
    InventoryClickEvent bottomEvent = click(owner.getOpenInventory(), 9);
    host.onInventoryClick(bottomEvent);

    assertFalse(unrelatedEvent.isCancelled());
    assertTrue(bottomEvent.isCancelled());
    assertEquals(0, calls.get());
  }

  @Test
  void topDragIsCancelled() {
    PaperUiHost host = new PaperUiHost();
    PlayerMock player = server.addPlayer("host-drag-" + UUID.randomUUID());
    host.open(
        player,
        new PaperUiHost.ScreenView(
            "drag", 1, Component.text("Drag"), Map.of(), Map.of(), ignored -> {}));

    InventoryDragEvent event =
        new InventoryDragEvent(
            player.getOpenInventory(),
            new ItemStack(Material.DIRT),
            new ItemStack(Material.DIRT),
            false,
            Map.of(0, new ItemStack(Material.DIRT)));
    host.onInventoryDrag(event);

    assertTrue(event.isCancelled());
  }

  @Test
  void refreshUpdatesContentsWithoutReplacingHolder() {
    PaperUiHost host = new PaperUiHost();
    PlayerMock player = server.addPlayer("host-refresh-" + UUID.randomUUID());
    host.open(
        player,
        new PaperUiHost.ScreenView(
            "before", 1, Component.text("Before"), Map.of(0, new ItemStack(Material.BOOK)), Map.of(), ignored -> {}));
    Inventory before = player.getOpenInventory().getTopInventory();
    Object holder = before.getHolder();
    ItemStack refreshed = new ItemStack(Material.DIAMOND);
    AtomicInteger calls = new AtomicInteger();
    PaperUiHost.SlotAction action = (p, event) -> calls.incrementAndGet();

    host.refresh(
        player,
        new PaperUiHost.ScreenView(
            "after", 2, Component.text("After"), Map.of(5, refreshed), Map.of(5, action), ignored -> {}));

    Inventory after = player.getOpenInventory().getTopInventory();
    assertSame(holder, after.getHolder());
    assertEquals(18, after.getSize());
    assertEquals(refreshed, after.getItem(5));
    assertEquals(Component.text("After"), player.getOpenInventory().title());
    host.onInventoryClick(click(player.getOpenInventory(), 5));
    assertEquals(1, calls.get());
  }

  @Test
  void unrelatedCloseCannotRemoveOwnerSession() {
    PaperUiHost host = new PaperUiHost();
    PlayerMock owner = server.addPlayer("host-close-owner-" + UUID.randomUUID());
    PlayerMock unrelated = server.addPlayer("host-close-unrelated-" + UUID.randomUUID());
    AtomicInteger calls = new AtomicInteger();
    host.open(
        owner,
        new PaperUiHost.ScreenView(
            "close-isolation",
            1,
            Component.text("Close isolation"),
            Map.of(),
            Map.of(),
            ignored -> calls.incrementAndGet()));

    unrelated.openInventory(owner.getOpenInventory().getTopInventory());
    host.onInventoryClose(new InventoryCloseEvent(unrelated.getOpenInventory()));
    assertEquals(0, calls.get());

    host.onInventoryClose(new InventoryCloseEvent(owner.getOpenInventory()));
    assertEquals(1, calls.get());
    host.close(owner);
    assertEquals(1, calls.get());
  }

  @Test
  void playerQuitRemovesSessionAndInvokesCallbackOnce() {
    PaperUiHost host = new PaperUiHost();
    PlayerMock player = server.addPlayer("host-quit-" + UUID.randomUUID());
    AtomicInteger calls = new AtomicInteger();
    host.open(
        player,
        new PaperUiHost.ScreenView(
            "quit",
            1,
            Component.text("Quit"),
            Map.of(),
            Map.of(),
            ignored -> calls.incrementAndGet()));

    host.onPlayerQuit(new PlayerQuitEvent(player, Component.text("quit")));
    assertEquals(1, calls.get());
    host.close(player);
    assertEquals(1, calls.get());
  }

  @Test
  void closeInvokesCallbackOnceAndRemovesSession() {
    PaperUiHost host = new PaperUiHost();
    PlayerMock player = server.addPlayer("host-close-" + UUID.randomUUID());
    AtomicInteger calls = new AtomicInteger();
    host.open(
        player,
        new PaperUiHost.ScreenView(
            "close", 1, Component.text("Close"), Map.of(), Map.of(), ignored -> calls.incrementAndGet()));

    host.close(player);
    host.close(player);

    assertEquals(1, calls.get());
  }

  @Test
  void closeAllClosesEveryOwnedView() {
    PaperUiHost host = new PaperUiHost();
    PlayerMock first = server.addPlayer("host-close-all-a-" + UUID.randomUUID());
    PlayerMock second = server.addPlayer("host-close-all-b-" + UUID.randomUUID());
    AtomicInteger calls = new AtomicInteger();
    PaperUiHost.ScreenView view =
        new PaperUiHost.ScreenView(
            "close-all", 1, Component.text("Close all"), Map.of(), Map.of(), ignored -> calls.incrementAndGet());
    host.open(first, view);
    host.open(second, view);

    host.closeAll();

    assertEquals(2, calls.get());
    host.close(first);
    host.close(second);
    assertEquals(2, calls.get());
  }

  private static InventoryClickEvent click(org.bukkit.inventory.InventoryView view, int rawSlot) {
    return new InventoryClickEvent(
        view, SlotType.CONTAINER, rawSlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
  }
}
