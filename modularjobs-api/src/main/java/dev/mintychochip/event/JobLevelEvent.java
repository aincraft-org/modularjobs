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

  private final PlayerJobState state;
  private final int oldLevel;
  private final int newLevel;
  private final Reason reason;

  /** Creates an experience-driven level transition. */
  public JobLevelEvent(@NotNull PlayerJobState state, int oldLevel, int newLevel) {
    this(state, oldLevel, newLevel, Reason.EXPERIENCE);
  }

  /** Creates a level transition for the supplied player state. */
  public JobLevelEvent(
      @NotNull PlayerJobState state, int oldLevel, int newLevel, @Nullable Reason reason) {
    this.state = Objects.requireNonNull(state, "state");
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
    return state.job();
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
