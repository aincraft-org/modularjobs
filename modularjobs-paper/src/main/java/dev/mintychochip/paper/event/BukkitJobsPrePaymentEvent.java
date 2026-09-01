package dev.mintychochip.paper.event;

import dev.mintychochip.Job;
import dev.mintychochip.JobTask;
import dev.mintychochip.container.Payable;
import dev.mintychochip.event.JobsPrePaymentEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Bukkit dual-fire wrapper for {@link JobsPrePaymentEvent}. */
public final class BukkitJobsPrePaymentEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();

  private final OfflinePlayer player;
  private final JobsPrePaymentEvent pure;

  /** Bukkit jobs pre payment event. */
  public BukkitJobsPrePaymentEvent(
      @NotNull OfflinePlayer player, @NotNull JobsPrePaymentEvent pure) {
    this.player = player;
    this.pure = pure;
  }

  public @NotNull OfflinePlayer getPlayer() {
    return player;
  }

  /** Pure. */
  public @NotNull JobsPrePaymentEvent pure() {
    return pure;
  }

  public @NotNull Payable getPayable() {
    return pure.getPayable();
  }

  public @NotNull Job getJob() {
    return pure.getJob();
  }

  public @NotNull JobTask getJobTask() {
    return pure.getJobTask();
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
