package dev.mintychochip.paper.event;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.event.JobLeaveEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Bukkit dual-fire wrapper for {@link JobLeaveEvent}. */
public final class BukkitJobLeaveEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final JobLeaveEvent pure;

  /** Bukkit job leave event. */
  public BukkitJobLeaveEvent(@NotNull Player player, @NotNull JobLeaveEvent pure) {
    this.player = player;
    this.pure = pure;
  }

  public @NotNull Player getPlayer() {
    return player;
  }

  /** Pure. */
  public @NotNull JobLeaveEvent pure() {
    return pure;
  }

  public @NotNull Job getJob() {
    return pure.getJob();
  }

  public @NotNull PlayerJobState getPlayerJobState() {
    return pure.getPlayerJobState();
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static @NotNull HandlerList getHandlerList() {
    return HANDLERS;
  }
}
