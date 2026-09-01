package dev.mintychochip.paper.event;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.event.JobJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Bukkit dual-fire wrapper for {@link JobJoinEvent}. */
public final class BukkitJobJoinEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final JobJoinEvent pure;

  /** Bukkit job join event. */
  public BukkitJobJoinEvent(@NotNull Player player, @NotNull JobJoinEvent pure) {
    this.player = player;
    this.pure = pure;
  }

  public @NotNull Player getPlayer() {
    return player;
  }

  /** Pure. */
  public @NotNull JobJoinEvent pure() {
    return pure;
  }

  public @NotNull Job getJob() {
    return pure.getJob();
  }

  public @NotNull PlayerJobState getPlayerJobState() {
    return pure.getPlayerJobState();
  }

  public boolean isRejoin() {
    return pure.isRejoin();
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static @NotNull HandlerList getHandlerList() {
    return HANDLERS;
  }
}
