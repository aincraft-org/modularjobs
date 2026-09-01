package dev.mintychochip.paper.event;

import dev.mintychochip.event.JobExperienceGainEvent;
import dev.mintychochip.event.JobJoinEvent;
import dev.mintychochip.event.JobLeaveEvent;
import dev.mintychochip.event.JobLevelEvent;
import dev.mintychochip.event.JobsPaymentEvent;
import dev.mintychochip.event.JobsPrePaymentEvent;
import java.util.Objects;
import org.aincraft.api.event.EventBus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dual-fires pure domain events on {@link EventBus} and thin Bukkit wrappers for third parties.
 * Cancellable wrappers share cancel/mutate state with the pure event.
 */
public final class PaperEventBridge {

  private final EventBus bus;

  /** Paper event bridge. */
  public PaperEventBridge(@NotNull EventBus bus) {
    this.bus = Objects.requireNonNull(bus, "bus");
  }

  /** Publish level. */
  public @NotNull JobLevelEvent publishLevel(
      @NotNull JobLevelEvent pure, @Nullable Player playerForBukkit) {
    bus.post(pure);
    if (playerForBukkit != null) {
      Bukkit.getPluginManager().callEvent(new BukkitJobLevelEvent(playerForBukkit, pure));
    }
    return pure;
  }

  /** Publish join. */
  public @NotNull JobJoinEvent publishJoin(
      @NotNull JobJoinEvent pure, @Nullable Player playerForBukkit) {
    bus.post(pure);
    if (playerForBukkit != null) {
      Bukkit.getPluginManager().callEvent(new BukkitJobJoinEvent(playerForBukkit, pure));
    }
    return pure;
  }

  /** Publish leave. */
  public @NotNull JobLeaveEvent publishLeave(
      @NotNull JobLeaveEvent pure, @Nullable Player playerForBukkit) {
    bus.post(pure);
    if (playerForBukkit != null) {
      Bukkit.getPluginManager().callEvent(new BukkitJobLeaveEvent(playerForBukkit, pure));
    }
    return pure;
  }

  /** Publish experience gain. */
  public @NotNull JobExperienceGainEvent publishExperienceGain(
      @NotNull JobExperienceGainEvent pure, @Nullable Player playerForBukkit) {
    bus.post(pure);
    if (playerForBukkit != null) {
      Bukkit.getPluginManager().callEvent(new BukkitJobExperienceGainEvent(playerForBukkit, pure));
    }
    return pure;
  }

  /** Publish payment. */
  public @NotNull JobsPaymentEvent publishPayment(
      @NotNull JobsPaymentEvent pure, @Nullable OfflinePlayer playerForBukkit) {
    bus.post(pure);
    if (playerForBukkit != null) {
      Bukkit.getPluginManager().callEvent(new BukkitJobsPaymentEvent(playerForBukkit, pure));
    }
    return pure;
  }

  /** Publish pre payment. */
  public @NotNull JobsPrePaymentEvent publishPrePayment(
      @NotNull JobsPrePaymentEvent pure, @Nullable OfflinePlayer playerForBukkit) {
    bus.post(pure);
    if (playerForBukkit != null) {
      Bukkit.getPluginManager().callEvent(new BukkitJobsPrePaymentEvent(playerForBukkit, pure));
    }
    return pure;
  }
}
