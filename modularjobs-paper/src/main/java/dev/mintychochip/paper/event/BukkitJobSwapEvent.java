package dev.mintychochip.paper.event;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.event.JobSwapEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Bukkit dual-fire wrapper for {@link JobSwapEvent}. */
public final class BukkitJobSwapEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final JobSwapEvent pure;

  /** Bukkit job swap event. */
  public BukkitJobSwapEvent(@NotNull Player player, @NotNull JobSwapEvent pure) {
    this.player = player;
    this.pure = pure;
  }

  public @NotNull Player getPlayer() {
    return player;
  }

  /** Pure. */
  public @NotNull JobSwapEvent pure() {
    return pure;
  }

  public @Nullable Job getFrom() {
    return pure.getFrom();
  }

  public @NotNull Job getTo() {
    return pure.getTo();
  }

  public @NotNull PlayerJobState getPlayerJobState() {
    return pure.getPlayerJobState();
  }

  public boolean isReactivation() {
    return pure.isReactivation();
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static @NotNull HandlerList getHandlerList() {
    return HANDLERS;
  }
}
