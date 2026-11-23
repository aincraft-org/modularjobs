package dev.mintychochip.event;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Event fired when a player leaves a job. */
public final class JobLeaveEvent implements org.aincraft.api.event.Event {

  private final Job job;
  private final PlayerJobState state;

  /** Creates a leave transition for the aggregate and player's final state. */
  public JobLeaveEvent(@NotNull Job job, @NotNull PlayerJobState state) {
    this.job = Objects.requireNonNull(job, "job");
    this.state = Objects.requireNonNull(state, "state");
    if (!state.belongsTo(job)) {
      throw new IllegalArgumentException("Player state does not belong to the supplied job");
    }
  }

  @Contract(pure = true)
  public @NotNull UUID getPlayerId() {
    return state.playerId();
  }

  @Contract(pure = true)
  public @NotNull Job getJob() {
    return job;
  }

  /** Returns the state being archived, including its active specialization node. */
  @Contract(pure = true)
  public @NotNull PlayerJobState getPlayerJobState() {
    return state;
  }
}
