package dev.mintychochip.event;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired after a slot change that {@link JobSwapRequestEvent} allowed has completed: {@code from}
 * (if any) is on hold and {@code to} is active. Read-only.
 */
public final class JobSwapEvent implements org.aincraft.api.event.Event {

  private final Job from;
  private final Job to;
  private final PlayerJobState state;
  private final boolean reactivation;

  /**
   * Creates a completed swap.
   *
   * @param from the job now on hold, or {@code null} when a free slot was filled
   * @param to the job now active
   * @param state the active state of {@code to}
   * @param reactivation whether {@code to} came back from hold
   */
  public JobSwapEvent(
      @Nullable Job from, @NotNull Job to, @NotNull PlayerJobState state, boolean reactivation) {
    this.from = from;
    this.to = Objects.requireNonNull(to, "to");
    this.state = Objects.requireNonNull(state, "state");
    if (!state.belongsTo(to)) {
      throw new IllegalArgumentException("Player state does not belong to the supplied job");
    }
    this.reactivation = reactivation;
  }

  @Contract(pure = true)
  public @NotNull UUID getPlayerId() {
    return state.playerId();
  }

  /** The job now on hold, or {@code null} when the player filled a free slot. */
  @Contract(pure = true)
  public @Nullable Job getFrom() {
    return from;
  }

  @Contract(pure = true)
  public @NotNull Job getTo() {
    return to;
  }

  /** The active state of {@link #getTo()}. */
  @Contract(pure = true)
  public @NotNull PlayerJobState getPlayerJobState() {
    return state;
  }

  /** Whether {@link #getTo()} came back from hold with its kept level and experience. */
  @Contract(pure = true)
  public boolean isReactivation() {
    return reactivation;
  }
}
