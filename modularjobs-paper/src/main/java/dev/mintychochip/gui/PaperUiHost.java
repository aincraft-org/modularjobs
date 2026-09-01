package dev.mintychochip.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Native Paper inventory host for ModularJobs screens. */
public final class PaperUiHost implements Listener {
  private final Map<UUID, Session> sessions = new HashMap<>();

  /** Opens a screen for a player. */
  public void open(@NotNull Player player, @NotNull ScreenView view) {
    close(player);
    Holder holder = new Holder(player.getUniqueId(), view.id(), view);
    Inventory inventory = createInventory(holder, view);
    Session session = new Session(player, holder, inventory);
    sessions.put(player.getUniqueId(), session);
    player.openInventory(inventory);
  }

  /** Replaces a player's screen while retaining the screen holder identity. */
  public void refresh(@NotNull Player player, @NotNull ScreenView view) {
    Session session = sessions.get(player.getUniqueId());
    if (session == null) {
      open(player, view);
      return;
    }

    Holder holder = session.holder;
    Inventory inventory = createInventory(holder, view);
    holder.update(view);
    session.inventory = inventory;
    player.openInventory(inventory);
  }

  /** Closes a player's hosted screen and invokes its callback once. */
  public void close(@NotNull Player player) {
    Session session = sessions.remove(player.getUniqueId());
    if (session == null) {
      return;
    }
    player.closeInventory();
    session.holder.view().onClose().accept(player);
  }

  /** Closes all hosted screens and invokes each callback once. */
  public void closeAll() {
    for (Session session : new ArrayList<>(sessions.values())) {
      if (sessions.remove(session.player.getUniqueId(), session)) {
        session.player.closeInventory();
        session.holder.view().onClose().accept(session.player);
      }
    }
  }

  @EventHandler
  public void onInventoryClick(@NotNull InventoryClickEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) {
      return;
    }
    Session session = sessions.get(holder.viewerId());
    if (session == null
        || session.holder != holder
        || session.inventory != event.getView().getTopInventory()
        || !session.player.getUniqueId().equals(event.getWhoClicked().getUniqueId())) {
      return;
    }

    event.setCancelled(true);
    int rawSlot = event.getRawSlot();
    if (rawSlot < 0 || rawSlot >= session.inventory.getSize()) {
      return;
    }
    SlotAction action = holder.view().actions().get(rawSlot);
    if (action != null && event.getWhoClicked() instanceof Player player) {
      action.execute(player, event);
    }
  }

  @EventHandler
  public void onInventoryDrag(@NotNull InventoryDragEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) {
      return;
    }
    Session session = sessions.get(holder.viewerId());
    if (session == null
        || session.holder != holder
        || session.inventory != event.getView().getTopInventory()
        || !session.player.getUniqueId().equals(event.getWhoClicked().getUniqueId())) {
      return;
    }
    event.setCancelled(true);
  }

  @EventHandler
  public void onInventoryClose(@NotNull InventoryCloseEvent event) {
    if (!(event.getInventory().getHolder() instanceof Holder holder)) {
      return;
    }
    Session session = sessions.get(holder.viewerId());
    if (session == null
        || session.holder != holder
        || session.inventory != event.getInventory()
        || !session.player.getUniqueId().equals(event.getPlayer().getUniqueId())) {
      return;
    }
    sessions.remove(holder.viewerId(), session);
    if (event.getPlayer() instanceof Player player) {
      holder.view().onClose().accept(player);
    }
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    Session session = sessions.remove(event.getPlayer().getUniqueId());
    if (session != null) {
      session.holder.view().onClose().accept(event.getPlayer());
    }
  }

  private static @NotNull Inventory createInventory(
      @NotNull Holder holder, @NotNull ScreenView view) {
    Inventory inventory = Bukkit.createInventory(holder, view.rows() * 9, view.title());
    for (Map.Entry<Integer, ItemStack> entry : view.items().entrySet()) {
      inventory.setItem(entry.getKey(), entry.getValue());
    }
    holder.setInventory(inventory);
    return inventory;
  }

  private static final class Session {
    private final Player player;
    private final Holder holder;
    private Inventory inventory;

    private Session(@NotNull Player player, @NotNull Holder holder, @NotNull Inventory inventory) {
      this.player = player;
      this.holder = holder;
      this.inventory = inventory;
    }
  }

  private static final class Holder implements InventoryHolder {
    private final UUID viewerId;
    private String screenId;
    private ScreenView view;
    private Inventory inventory;

    private Holder(@NotNull UUID viewerId, @NotNull String screenId, @NotNull ScreenView view) {
      this.viewerId = viewerId;
      this.screenId = screenId;
      this.view = view;
    }

    private @NotNull UUID viewerId() {
      return viewerId;
    }

    private @NotNull ScreenView view() {
      return view;
    }

    private void update(@NotNull ScreenView next) {
      screenId = next.id();
      view = next;
    }

    private void setInventory(@NotNull Inventory next) {
      inventory = next;
    }

    @Override
    public @NotNull Inventory getInventory() {
      return inventory;
    }
  }

  /** Immutable description of a hosted screen. */
  public record ScreenView(
      @NotNull String id,
      int rows,
      @NotNull Component title,
      @NotNull Map<Integer, ItemStack> items,
      @NotNull Map<Integer, SlotAction> actions,
      @NotNull Consumer<Player> onClose) {
    public ScreenView {
      if (id == null || title == null || onClose == null) {
        throw new NullPointerException("screen id, title, and close callback are required");
      }
      if (rows < 1 || rows > 6) {
        throw new IllegalArgumentException("rows must be between 1 and 6");
      }
      items = copySlots(items, rows, "items");
      actions = copySlots(actions, rows, "actions");
    }

    private static <T> @NotNull Map<Integer, T> copySlots(
        @NotNull Map<Integer, T> source, int rows, @NotNull String name) {
      if (source == null) {
        throw new NullPointerException(name + " are required");
      }
      int slotCount = rows * 9;
      Map<Integer, T> copy = new HashMap<>(source.size());
      for (Map.Entry<Integer, T> entry : source.entrySet()) {
        Integer slot = entry.getKey();
        if (slot == null || slot < 0 || slot >= slotCount) {
          throw new IllegalArgumentException(name + " contain an invalid slot: " + slot);
        }
        if (entry.getValue() == null) {
          throw new NullPointerException(name + " contain a null value");
        }
        copy.put(slot, entry.getValue());
      }
      return Map.copyOf(copy);
    }
  }

  /** Action invoked for an owned top-inventory slot click. */
  @FunctionalInterface
  public interface SlotAction {
    void execute(@NotNull Player player, @NotNull InventoryClickEvent event);
  }
}
