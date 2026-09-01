package dev.mintychochip.event;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Event fired when a player leaves a job. */
public final class JobLeaveEvent implements org.aincraft.api.event.Event {

  private final PlayerJobState state;

  /** Creates a leave transition for the player's final job-tree state. */
  public JobLeaveEvent(@NotNull PlayerJobState state) {
    this.state = Objects.requireNonNull(state, "state");
  }

  @Contract(pure = true)
  public @NotNull UUID getPlayerId() {
    return state.playerId();
  }

  @Contract(pure = true)
  public @NotNull Job getJob() {
    return state.job();
  }

  /** Returns the state being archived, including its active specialization node. */
  @Contract(pure = true)
  public @NotNull PlayerJobState getPlayerJobState() {
    return state;
  }
}
