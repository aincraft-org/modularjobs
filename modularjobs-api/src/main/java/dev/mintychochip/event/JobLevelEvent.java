package dev.mintychochip.event;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fired when a player's job level changes. */
public final class JobLevelEvent implements org.aincraft.api.event.Event {

  /** Reason. */
  public enum Reason {
    EXPERIENCE,
    ADMIN_COMMAND,
    OTHER
  }

  private final Job job;
  private final PlayerJobState state;
  private final int oldLevel;
  private final int newLevel;
  private final Reason reason;

  /** Creates an experience-driven level transition. */
  public JobLevelEvent(
      @NotNull Job job, @NotNull PlayerJobState state, int oldLevel, int newLevel) {
    this(job, state, oldLevel, newLevel, Reason.EXPERIENCE);
  }

  /** Creates a level transition for the supplied aggregate and player state. */
  public JobLevelEvent(
      @NotNull Job job,
      @NotNull PlayerJobState state,
      int oldLevel,
      int newLevel,
      @Nullable Reason reason) {
    this.job = Objects.requireNonNull(job, "job");
    this.state = Objects.requireNonNull(state, "state");
    if (!state.belongsTo(job)) {
      throw new IllegalArgumentException("Player state does not belong to the supplied job");
    }
    this.oldLevel = oldLevel;
    this.newLevel = newLevel;
    this.reason = reason == null ? Reason.OTHER : reason;
  }

  @Contract(pure = true)
  public @NotNull UUID getPlayerId() {
    return state.playerId();
  }

  @Contract(pure = true)
  public @NotNull Job getJob() {
    return job;
  }

  public int getOldLevel() {
    return oldLevel;
  }

  public int getNewLevel() {
    return newLevel;
  }

  @Contract(pure = true)
  public @NotNull PlayerJobState getPlayerJobState() {
    return state;
  }

  @Contract(pure = true)
  public @NotNull Reason getReason() {
    return reason;
  }
}
