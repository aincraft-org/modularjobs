package dev.mintychochip.event;

import dev.mintychochip.Job;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired before a slot change that is not a free first join: a swap of one active job for another,
 * the re-activation of an on-hold (archived) job, or a join into a slot group where the player has
 * a job on hold (leave then join is a swap).
 *
 * <p>Nothing has changed when this fires. Cancelling it refuses the change; the canceller tells the
 * player why. Listeners use it to charge a swap price or hold a cooldown and confirm the charge on
 * {@link JobSwapEvent}.
 */
public final class JobSwapRequestEvent implements Cancellable, org.aincraft.api.event.Event {

  private final UUID playerId;
  private final Job from;
  private final Job to;
  private final boolean reactivation;
  private boolean cancelled;

  /**
   * Creates a swap request.
   *
   * @param playerId the player changing jobs
   * @param from the active job going on hold, or {@code null} when a free slot is filled
   * @param to the job becoming active
   * @param reactivation whether {@code to} comes back from hold with its kept progress
   */
  public JobSwapRequestEvent(
      @NotNull UUID playerId, @Nullable Job from, @NotNull Job to, boolean reactivation) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.from = from;
    this.to = Objects.requireNonNull(to, "to");
    this.reactivation = reactivation;
  }

  @Contract(pure = true)
  public @NotNull UUID getPlayerId() {
    return playerId;
  }

  /** The active job that goes on hold, or {@code null} when the player fills a free slot. */
  @Contract(pure = true)
  public @Nullable Job getFrom() {
    return from;
  }

  @Contract(pure = true)
  public @NotNull Job getTo() {
    return to;
  }

  /** Whether {@link #getTo()} returns from hold with its kept level and experience. */
  @Contract(pure = true)
  public boolean isReactivation() {
    return reactivation;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
