package dev.mintychochip.paper.event;

import dev.mintychochip.Job;
import dev.mintychochip.event.JobSwapRequestEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Bukkit dual-fire wrapper for {@link JobSwapRequestEvent}; cancel state is shared. */
public final class BukkitJobSwapRequestEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final JobSwapRequestEvent pure;

  /** Bukkit job swap request event. */
  public BukkitJobSwapRequestEvent(@NotNull Player player, @NotNull JobSwapRequestEvent pure) {
    this.player = player;
    this.pure = pure;
  }

  public @NotNull Player getPlayer() {
    return player;
  }

  /** Pure. */
  public @NotNull JobSwapRequestEvent pure() {
    return pure;
  }

  public @Nullable Job getFrom() {
    return pure.getFrom();
  }

  public @NotNull Job getTo() {
    return pure.getTo();
  }

  public boolean isReactivation() {
    return pure.isReactivation();
  }

  @Override
  public boolean isCancelled() {
    return pure.isCancelled();
  }

  @Override
  public void setCancelled(boolean cancel) {
    pure.setCancelled(cancel);
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static @NotNull HandlerList getHandlerList() {
    return HANDLERS;
  }
}
